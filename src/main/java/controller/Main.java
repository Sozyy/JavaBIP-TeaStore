package controller;
import akka.actor.ActorSystem;
import controller.cache.SwitchableCache;

import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;

/**
 * Loop of monitoring :
 *   1. GET /image/rest/metrics/requests -> delta of requests since last poll
 *   2. delta > 0 : getting into the JavaBIP pipeline, PID calculates a new cache size
 *   3. delta = 0 : no traffic or no TeaStore connection, this cycle is skipped
 *   4. After each JavaBIP cycle, the new cache size is POSTed to /image/rest/image/setCacheSize.
 */
public class Main {
    /**
     * Ports have been changed in experimentation-platform/Sources/examples/docker/docker-compose_default.yaml
     * image:                                               
     *     image: cerberus237/adaptable-teastore-image      
     *     expose:                                          
     *       - "8080"                                       
     *     ports:                                           
     *       - "8083:8080"                                  
     */
    private static final String IMAGE_BASE_URL = "http://localhost:8083/tools.descartes.teastore.image";

    // Supported strategies: LRU, LFU, FIFO, LIFO, MRU, RR
    private static final String DEFAULT_CACHE_STRATEGY = "LRU";

    private static final int   CACHE_CAPACITY      = 80;    // initial cache size
    private static final int   MIN_CACHE_CAPACITY  = 2;     // minimum cache size
    private static final int   MAX_CACHE_CAPACITY  = 500;   // maximum cache size
    private static final int   IMAGE_UNIVERSE_SIZE = 100;   // may not be use in new DP logic
    private static final float TARGET_TIME         = 10.0F; // should change

    private static final float KP = 0.9F;                   //
    private static final float KI = 0.05F;                  // PID controller parameters
    private static final float KD = 0.15F;                  //

    private static final float HIT_WEIGHT  = 0.2F;          // loading time of an already cached item
    private static final float MISS_WEIGHT = 2.0F;          // loading time of a non-cached item

    private static final long SCALE_FACTOR     = 1L;        // factor multiplied to new cache size to be in bytes
    private static final long POLL_INTERVAL_MS = 2_000L;    // timeout between two TeaStore polls
    private static final long CYCLE_TIMEOUT_MS = 10_000L;   // timeout per BIP cycle, has to be superior then DataProvider processing time

    public static void main(String[] args) throws Exception {
        ActorSystem   system            = ActorSystem.create("system");
        EngineFactory engineFactory     = new EngineFactory(system);
        BIPGlue       glue              = new Glue().build();
        BIPEngine     engine            = engineFactory.create("glue", glue);

        SwitchableCache cache           = new SwitchableCache(DEFAULT_CACHE_STRATEGY, CACHE_CAPACITY);
        DataProvider    dataProvider    = new DataProvider(cache, IMAGE_UNIVERSE_SIZE, HIT_WEIGHT, MISS_WEIGHT);
        PIDController   pid             = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY, MAX_CACHE_CAPACITY, TARGET_TIME, KP, KI, KD);
        Bridge          bridge          = new Bridge(cache);

        CacheUpdater     imageUpdater   = new CacheUpdater(IMAGE_BASE_URL);
        ServiceCollector imageCollector = new ServiceCollector("image", IMAGE_BASE_URL, "/rest/metrics/requests");

        engine.register(dataProvider, "dataProvider",  true);
        engine.register(pid,          "pidController", true);
        engine.register(bridge,       "bridge",        true);

        System.out.println("=== START ===");
        System.out.println("Reading : " + IMAGE_BASE_URL + "/rest/metrics/requests");

        engine.specifyGlue(glue);
        engine.start();
        engine.execute();

        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

        int iter = 0;
        try {
            while (true) {
                int delta = imageCollector.get();
                if (delta > 0) {
                    bridge.update(delta, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);

                    if (newCacheSize >= 0) {
                        long sizeForTeaStore = (long) newCacheSize * SCALE_FACTOR;
                        CacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);

                        System.out.printf("[Monitor] iter=%4d  delta=%4d  -> PID_cache=%d  strategy=%s  -> image=%s%n", iter, delta, newCacheSize, cache.getActiveStrategy(), result);
                    } else {
                        System.out.printf("[Monitor] iter=%4d  delta=%4d  -> timeout (BIP cycle)%n", iter, delta);
                    }

                } else {
                    System.out.printf("[Monitor] iter=%4d  delta=  0   (no traffic or unreachable)%n", iter);
                }

                iter++;
                Thread.sleep(POLL_INTERVAL_MS);     // periodicity
            }

        } catch (InterruptedException ignored) {
            System.out.println("[Monitor] interrupted");
        } finally {
            System.out.printf("%n=== STOP (iter=%d) ===%n", iter);
            try { engine.stop();                 } catch (Exception ignored3) {}
            try { engineFactory.destroy(engine); } catch (Exception ignored3) {}
            try { system.terminate();            } catch (Exception ignored3) {}
        }
    }
}
