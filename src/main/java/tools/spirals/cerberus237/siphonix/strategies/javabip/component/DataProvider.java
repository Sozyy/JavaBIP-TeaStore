package tools.spirals.cerberus237.siphonix.strategies.javabip.component;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.strategies.javabip.CacheManagementStrategy;
import tools.spirals.cerberus237.siphonix.strategies.javabip.cache.ICache;

/**
 * DataProvider gets the real IDs of the images loaded by AdaptableTeaStore since the last poll,
 * replays them against the local shadow cache to determine which would have been hits/misses,
 * and derives a response time estimate from that.
 * Sends the responseTime to the PIDController which will give a new cache size to communicate to the Bridge.
 *
 * hitWeight/missWeight are the per-access time costs used to turn a hit/miss count into a
 * responseTime estimate. They used to be fixed constants guessed at startup; they are now
 * updated at runtime from the real measurements exposed by AdaptableTeaStore's image service
 * (GET /rest/metrics/cache-performance, see CacheHitMissCollector), via updateTimings().
 * Sensible defaults are still accepted at construction time in case the endpoint is briefly
 * unreachable (e.g. before the first successful poll).
 */
@Ports({
        @Port(name = "receiveRequest",      type = PortType.enforceable),
        @Port(name = "sendResponseTime",    type = PortType.enforceable),
        @Port(name = "receiveNewCacheSize", type = PortType.enforceable),
        @Port(name = "notifyServer",        type = PortType.enforceable)
})
@ComponentType(name = "DataProvider", initial = "IDLE")
public class DataProvider {
    private final ICache cache;

    private volatile float hitWeight;
    private volatile float missWeight;

    private float responseTime;
    private int loadedImages;

    public DataProvider(ICache cache, float hitWeight, float missWeight) {
        this.cache      = cache;
        this.hitWeight  = hitWeight;
        this.missWeight = missWeight;
    }

    /**
     * Updates the hit/miss timing weights from a freshly polled CacheHitMissMetrics.
     * Called by CacheManagementStrategy before each cycle, right after CacheHitMissCollector.get().
     * Values that are NaN, negative, or otherwise not usable are ignored so a
     * transient collector error doesn't zero out the PID's process variable.
     */
    public void updateTimings(double meanHitTime, double meanMissTime) {
        if (meanHitTime >= 0 && !Double.isNaN(meanHitTime)) {
            this.hitWeight = (float) meanHitTime;
        }
        if (meanMissTime >= 0 && !Double.isNaN(meanMissTime)) {
            this.missWeight = (float) meanMissTime;
        }
    }

    // === Transitions ===

    @Transition(name   = "receiveRequest",
                source = "IDLE",
                target = "PROCESSING")
    public void receiveRequest(@Data(name = "request") int[] loadedImageIds) {
        processRequest(loadedImageIds);
    }

    @Transition(name   = "sendResponseTime",
                source = "PROCESSING",
                target = "WAITING_FOR_CACHE_SIZE")
    public void sendResponseTime() {}

    @Transition(name   = "receiveNewCacheSize",
                source = "WAITING_FOR_CACHE_SIZE",
                target = "READY")
    public void receiveNewCacheSize(@Data(name = "newCacheSize") int newCacheSize) {
        cache.setCapacity(newCacheSize);
    }

    @Transition(name   = "notifyServer",
                source = "READY",
                target = "IDLE")
    public void notifyServer() {}

    // === DATA WIRES ===

    @Data(name = "responseTime", accessTypePort = DataOut.AccessType.any)
    public float getResponseTime() { return responseTime; }

    @Data(name = "response", accessTypePort = DataOut.AccessType.any)
    public int response() { return loadedImages; }


    // === HELPERS ===

    public void processRequest(int[] loadedImageIds) {
        int hits   = 0;
        int misses = 0;

        for (int imageId : loadedImageIds) {
            if (cache.contains(imageId)) {
                hits++;
            } else {
                misses++;
                cache.addItem(imageId);
            }
        }

        responseTime = (float) (missWeight * misses + hitWeight * hits);
        loadedImages = loadedImageIds.length;
        cache.recordStep(cache.getCapacity(), responseTime, misses, hits, loadedImages);
    }
}