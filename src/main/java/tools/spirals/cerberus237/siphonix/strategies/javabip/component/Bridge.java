package tools.spirals.cerberus237.siphonix.strategies.javabip.component;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import tools.spirals.cerberus237.siphonix.strategies.javabip.cache.ICache;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bridge between CacheManagementStrategy's polling thread and the JavaBIP engine.
 *
 * The polling thread runs outside the BIP engine and drives it synchronously: it pushes the
 * real image IDs loaded by AdaptableTeaStore since the last poll via update(), then blocks on
 * waitForCycleAndGetCacheSize() until the engine has run a full DataProvider/PID cycle.
 *
 * JavaBIP side : two ports connected to DataProvider (see Glue).
 * sendRequest has no guard because it blocks on dataAvailable until update() is called,
 * it could be replaced by a spontaneous transition.
 *
 * Complete cycle :
 *   1. update(imageIds) -> pendingImageIds=imageIds, signal dataAvailable
 *   2. sendRequest() unlocks, expose pendingImageIds via @Data "request"
 *   3. DataProvider and PID work, DataProvider get the new cache
 *   4. DataProvider.notifyServer -> bridge.receiveResp() -> cycleCompleted=true
 *   5. waitForCycleAndGetCacheSize() unlocks and sends the new capacity
 */
// portail entre le thread de polling de CacheManagementStrategy et le moteur JavaBIP
@Ports({
        @Port(name = "sendRequest", type = PortType.enforceable),
        @Port(name = "receiveResp", type = PortType.enforceable)
})
@ComponentType(name = "Bridge", initial = "IDLE")
public class Bridge {

    private volatile int[] pendingImageIds = new int[0];
    private final ICache cache;

    // Polling thread <-> JavaBIP engine coordination
    private final Lock lock = new ReentrantLock();
    private final Condition cycleDone     = lock.newCondition();
    private final Condition dataAvailable = lock.newCondition();

    private volatile boolean hasNewData     = false;
    private volatile boolean cycleCompleted = false;
    private volatile int     lastCacheSize  = -1;

    public Bridge(ICache cache) {
        this.cache = cache;
    }

    // === Called by CacheManagementStrategy's polling thread ===

    public void update(int[] imageIds, String message) {
        if (imageIds == null) return;

        lock.lock();
        try {
            if (hasNewData) {
                System.out.printf("[Bridge] update(%d images) DROPPED: previous data not consumed yet%n", imageIds.length);
                return;
            }
            this.pendingImageIds = imageIds;
            this.cycleCompleted = false;
            this.hasNewData = true;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // === Transitions ===

    @Transition(name = "sendRequest", source = "IDLE", target = "SENT")
    public void sendRequest() throws InterruptedException {
        lock.lock();
        try {
            while (!hasNewData) {
                dataAvailable.await();
            }
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

