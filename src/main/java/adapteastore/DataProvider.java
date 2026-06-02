package Adapteastore;

import TeaStoreWithConnector.LRUCache;
import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import java.util.Random;

@Ports({
        @Port(name = "receiveRequest",      type = PortType.enforceable),
        @Port(name = "sendResponseTime",    type = PortType.enforceable),
        @Port(name = "receiveNewCacheSize", type = PortType.enforceable),
        @Port(name = "notifyServer",        type = PortType.enforceable)
})
@ComponentType(name = "DataProvider", initial = "IDLE")
public class DataProvider {
    private final LRUCache cache;
    private final Random random = new Random();
    private final int imageUniverseSize;

    private float responseTime;
    private int loadedImages;

    public DataProvider(LRUCache cache, int  imageUniverseSize) {
        this.cache = cache;
        this.imageUniverseSize = imageUniverseSize;
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "receiveRequest",
            source = "IDLE",
            target = "PROCESSING")
    public void receiveRequest(@Data(name = "request") int nbImagesToLoad) {
//        System.out.println("[DataProvider] IDLE -> PROCESSING - Received from Server : " + nbImagesToLoad + " images to load");
        processRequest(nbImagesToLoad);
    }

    @Transition(name   = "sendResponseTime",
            source = "PROCESSING",
            target = "WAITING_FOR_CACHE_SIZE")
    public void sendResponseTime() {
//        System.out.println("[DataProvider] PROCESSING -> WAITING_FOR_CACHE_SIZE");
    }

    @Transition(name   = "receiveNewCacheSize",
            source = "WAITING_FOR_CACHE_SIZE",
            target = "READY")
    public void receiveNewCacheSize(@Data(name = "newCacheSize") int newCacheSize) {
        cache.setCapacity(newCacheSize);
//        System.out.println(" > DataProvider - Received from Controller : " + newCacheSize + " new cache size");
//        System.out.println("[DataProvider] WAITING_FOR_CACHE_SIZE -> READY - Received from Controller : " + newCacheSize + " new cache size");
    }

    @Transition(name   = "notifyServer",
            source = "READY",
            target = "IDLE")
    public void notifyServer() {
//        System.out.println("[DataProvider] READY -> IDLE");
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "responseTime", accessTypePort = DataOut.AccessType.any)
    public float getResponseTime() { return responseTime; }

    @Data(name = "response", accessTypePort = DataOut.AccessType.any)
    public int response() { return loadedImages; }

    // ===============
    // === HELPERS ===
    // ===============

    public void processRequest(int nbImages) {
        int hits = 0;
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

        responseTime = (float) (2 * misses + 0.2 * hits);
        loadedImages = nbImages;
        cache.recordStep(cache.getCapacity(), responseTime, misses, hits, nbImages);

//        System.out.println(cache.toString());
    }
}