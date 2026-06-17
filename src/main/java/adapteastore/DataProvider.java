package adapteastore;

import adapteastore.cache.ICache;
import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import java.util.Random;


/**
 * DataProvider get a request from the Bridge and process the way it worked for the CollectiveTeaStore :
 * Loads a number of random integers, this number is the request.
 * Calculates a loading time depending on if the random integers were present or not in the cache.
 * Send the processingTime to the PIDController which will give a new cache to communicate to the Bridge.
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
    private final Random random = new Random();
    private final int imageUniverseSize;

    private final float hitWeight;
    private final float missWeight;

    private float responseTime;
    private int loadedImages;

    public DataProvider(ICache cache, int imageUniverseSize, float hitWeight, float missWeight) {
        this.cache             = cache;
        this.imageUniverseSize = imageUniverseSize;
        this.hitWeight         = hitWeight;
        this.missWeight        = missWeight;
    }

    // === Transitions ===

    @Transition(name   = "receiveRequest",
                source = "IDLE",
                target = "PROCESSING")
    public void receiveRequest(@Data(name = "request") int nbImagesToLoad) {
        processRequest(nbImagesToLoad);
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

    public void processRequest(int nbImages) {
        int hits   = 0;
        int misses = 0;

        for (int i = 0; i < nbImages; i++) {
            int imageId = random.nextInt(imageUniverseSize);
            if (cache.contains(imageId)) {
                hits++;
            } else {
                misses++;
                cache.addItem(imageId);
            }
        }

        responseTime = (float) (missWeight * misses + hitWeight * hits);
        loadedImages = nbImages;
        cache.recordStep(cache.getCapacity(), responseTime, misses, hits, nbImages);
    }
}
