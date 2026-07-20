package tools.spirals.cerberus237.siphonix.strategies.javabip.controller;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import tools.spirals.cerberus237.siphonix.strategies.javabip.controller.cache.ICache;
import tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.operators.TrueEvaluator;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bridge Adaptiflow <-> JavaBIP
 *
 * Adaptiflow side : Observer<int[]> notified by Event.observe() when the collector pushes the real
 * IDs of the images loaded by AdaptableTeaStore since the last poll.
 *
 * JavaBIP side : two ports connected on the DataProvider.
 * sendRequest has no guard because it blocks on dataAvailable until Adaptiflow calls update(),
 * it could be replaced by a spontaneous transition.
 *
 * Complete cycle :
 *   1. event.observe() -> bridge.update(imageIds) -> pendingImageIds=imageIds, signal dataAvailable
 *   2. sendRequest() unlocks, expose pendingImageIds via @Data "request"
 *   3. DataProvider and PID work, DataProvider get the new cache
 *   4. DataProvider.notifyServer -> bridge.receiveResp() -> cycleCompleted=true
 *   5. waitForCycleAndGetCacheSize() unlocks and sends the new capacity
 */
@Ports({
        @Port(name = "sendRequest", type = PortType.enforceable),
        @Port(name = "receiveResp", type = PortType.enforceable)
})
@ComponentType(name = "AdaptableTeaStoreBridge", initial = "IDLE")
public class Bridge implements Observer<int[]> {

    private volatile int[] pendingImageIds = new int[0];
    private final ICache cache;
    private final ConditionEvaluator<int[]> conditionEvaluator;

    // Adaptiflow and JavaBIP coordination
    private final Lock lock = new ReentrantLock();
    private final Condition cycleDone     = lock.newCondition();
    private final Condition dataAvailable = lock.newCondition();

    private volatile boolean hasNewData     = false;
    private volatile boolean cycleCompleted = false;
    private volatile int     lastCacheSize  = -1;

    public Bridge(ICache cache) {
        this.cache = cache;
        this.conditionEvaluator = new TrueEvaluator<>();
    }

    // === Observer<int[]> (AdaptiFlow) ===

    @Override
    public void update(int[] imageIds, String message) {
        if (imageIds == null) return;

        lock.lock();
        try {
            if (hasNewData) {
                System.out.printf("[Bridge] update(%d images) DROPPED: previous data not consumed yet%n",
                        imageIds.length);
                return;
            }
            System.out.printf("[Bridge] update(%d images, msg=%s)%n", imageIds.length, message);
            this.pendingImageIds = imageIds;
            this.cycleCompleted = false;
            this.hasNewData = true;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ConditionEvaluator<int[]> getConditionEvaluator() {
        return conditionEvaluator;
    }

    // === Transitions ===

    @Transition(name = "sendRequest", source = "IDLE", target = "SENT")
    public void sendRequest() throws InterruptedException {
        lock.lock();
        try {
            while (!hasNewData) {
                dataAvailable.await();
            }
            System.out.printf("[Bridge] IDLE -> SENT (injecting %d images)%n", pendingImageIds.length);
            hasNewData = false;
        } finally {
            lock.unlock();
        }
    }

    @Transition(name = "receiveResp", source = "SENT", target = "IDLE")
    public void receiveResp() {
        lock.lock();
        try {
            lastCacheSize = cache.getCapacity();
            cycleCompleted = true;
            System.out.printf("[Bridge] SENT -> IDLE (cycle done, cache size = %d)%n", lastCacheSize);
            cycleDone.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // === Data wires ===

    @Data(name = "request", accessTypePort = DataOut.AccessType.any)
    public int[] getRequest() {
        return pendingImageIds;
    }

    // === Helpers ===

    public int waitForCycleAndGetCacheSize(long timeoutMs) throws InterruptedException {
        lock.lock();
        try {
            long remainingNanos = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (!cycleCompleted) {
                if (remainingNanos <= 0) {
                    System.err.println("[Bridge] waitForCycleAndGetCacheSize: timeout");
                    return -1;
                }
                remainingNanos = cycleDone.awaitNanos(remainingNanos);
            }
            int size = lastCacheSize;
            cycleCompleted = false;
            return size;
        } finally {
            lock.unlock();
        }
    }
}

