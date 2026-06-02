package adapteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.operators.TrueEvaluator;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bridge Adaptiflow <-> JavaBIP
 *
 * Adaptiflow side : Observer<Integer> notified by Event.observe() when the collector pushes a new value.
 *
 * JavaBIP side : two ports connected on the DataProvider.
 * sendRequest has no guard because it blocks on dataAvailable until Adaptiflow calls update(),
 * it could be replaced by a spontaneous transition.
 *
 * Complete cycle :
 *   1. event.observe() -> bridge.update(value) -> pendingMetric=value, signal dataAvailable
 *   2. sendRequest() unlocks, expose pendingMetric via @Data "request"
 *   3. DataProvider and PID work, DataProvider get the new cache
 *   4. DataProvider.notifyServer -> bridge.receiveResp() -> cycleCompleted=true
 *   5. waitForCycleAndGetCacheSize() unlocks and sends the new capacity
 */
@Ports({
        @Port(name = "sendRequest", type = PortType.enforceable),
        @Port(name = "receiveResp", type = PortType.enforceable)
})
@ComponentType(name = "AdaptableTeaStoreBridge", initial = "IDLE")
public class Bridge implements Observer<Integer> {

    private volatile int pendingMetric = 0;
    private final LRUCache cache;
    private final ConditionEvaluator<Integer> conditionEvaluator;

    // Adaptiflow and JavaBIP coordination
    private final Lock lock = new ReentrantLock();
    private final Condition cycleDone     = lock.newCondition();
    private final Condition dataAvailable = lock.newCondition();

    private volatile boolean hasNewData     = false;
    private volatile boolean cycleCompleted = false;
    private volatile int     lastCacheSize  = -1;

    public Bridge(LRUCache cache) {
        this.cache = cache;
        this.conditionEvaluator = new TrueEvaluator<>();
    }

    // === Observer<Integer> (AdaptiFlow) ===

    @Override
    public void update(Integer metricValue, String message) {
        if (metricValue == null) return;

        lock.lock();
        try {
            if (hasNewData) {
                System.out.printf("[Bridge] update(metric=%d) DROPPED: previous data not consumed yet%n",
                        metricValue);
                return;
            }
            System.out.printf("[Bridge] update(metric=%d, msg=%s)%n", metricValue, message);
            this.pendingMetric = metricValue;
            this.cycleCompleted = false;
            this.hasNewData = true;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ConditionEvaluator<Integer> getConditionEvaluator() {
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
            System.out.printf("[Bridge] IDLE -> SENT (injecting %d images)%n", pendingMetric);
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
    public int getRequest() {
        return pendingMetric;
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
