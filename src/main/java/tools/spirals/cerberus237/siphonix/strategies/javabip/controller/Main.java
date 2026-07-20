package tools.spirals.cerberus237.siphonix.strategies.javabip.controller;

import akka.actor.ActorSystem;
import tools.spirals.cerberus237.siphonix.strategies.javabip.controller.cache.SwitchableCache;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheHitMissMetrics;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheMetrics;

import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;

/**
 * Loop of monitoring :
 *   1. GET /image/rest/metrics/cache-entries      -> real IDs of images currently in the cache
 *   2. GET /image/rest/metrics/cache-performance   -> real meanHitTime/meanMissTime, pushed into
 *      DataProvider so its responseTime estimate reflects what the TeaStore actually measures,
 *      instead of the fixed HIT_WEIGHT/MISS_WEIGHT guesses used previously
 *   3. GET /image/rest/metrics/cache-metrics       -> real cache occupation in bytes, logged
 *      alongside the PID's own item-based capacity (see NOTE on units below)
 *   4. any cache-entries IDs received : getting into the JavaBIP pipeline, DataProvider replays
 *      them against the shadow cache to derive hits/misses, PID calculates a new cache size
 *   5. no IDs : no traffic or no TeaStore connection, this cycle is skipped
 *   6. After each JavaBIP cycle, the new cache size is POSTed to /image/rest/image/setCacheSize.
 *
 * NOTE on units: the shadow cache (SwitchableCache/ICache) and the PIDController still work in
 * item counts (inherited from the CollectiveTeaStore/CHIPS port), while AdaptableTeaStore's real
 * cache-metrics are in bytes. CacheMetricsCollector is used here only to log the real byte-based
 * occupation for comparison/diagnostics; it is NOT used to bound the PID (that would silently
 * mix units). Migrating the shadow cache itself to a byte-aware capacity is a separate, larger
 * change touching every ICache implementation and is left for a dedicated follow-up.
 */
public class Main {
    private static final String IMAGE_BASE_URL = "http://localhost:8080/tools.descartes.teastore.image";

    // Supported strategies: LRU, LFU, FIFO, LIFO, MRU, RR
    private static final String DEFAULT_CACHE_STRATEGY = "LRU";

    private static final int   CACHE_CAPACITY      = 80;    // initial cache size
    private static final int   MIN_CACHE_CAPACITY  = 2;     // minimum cache size
    private static final int   MAX_CACHE_CAPACITY  = 500;   // maximum cache size
    private static final float TARGET_TIME         = 10.0F; // should change

    private static final float KP = 0.9F;                   //
    private static final float KI = 0.05F;                  // PID controller parameters
    private static final float KD = 0.15F;                  //

    // Fallback values only, used until the first successful cache-performance poll (or if it
    // becomes unreachable). Overridden at runtime by DataProvider.updateTimings() with the real
    // meanHitTime/meanMissTime measured by AdaptableTeaStore (see CacheHitMissCollector).
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
        DataProvider    dataProvider    = new DataProvider(cache, HIT_WEIGHT, MISS_WEIGHT);
        PIDController   pid             = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY, MAX_CACHE_CAPACITY, TARGET_TIME, KP, KI, KD);
        Bridge          bridge          = new Bridge(cache);

        CacheUpdater           imageUpdater        = new CacheUpdater(IMAGE_BASE_URL);
        CacheEntriesCollector  imageCollector      = new CacheEntriesCollector("image", IMAGE_BASE_URL);
        CacheHitMissCollector  hitMissCollector    = new CacheHitMissCollector("image", IMAGE_BASE_URL);
        CacheMetricsCollector  cacheMetricsCollector = new CacheMetricsCollector("image", IMAGE_BASE_URL);

        engine.register(dataProvider, "dataProvider",  true);
        engine.register(pid,          "pidController", true);
        engine.register(bridge,       "bridge",        true);

        System.out.println("=== START ===");
        System.out.println("Reading : " + imageCollector);

        engine.specifyGlue(glue);
        engine.start();
        engine.execute();

        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

        int iter = 0;
        try {
            while (true) {
                // Pull the real hit/miss timings first so this cycle's responseTime estimate
                // (computed by DataProvider from the images below) uses up-to-date weights.
                CacheHitMissMetrics hitMiss = hitMissCollector.get();
                if (hitMiss != null) {
                    dataProvider.updateTimings(hitMiss.getMeanHitTime(), hitMiss.getMeanMissTime());
                }

                // Real byte-based occupation, logged for diagnostics only (see NOTE on units above).
                CacheMetrics realOccupation = cacheMetricsCollector.get();

                int[] loadedImageIds = imageCollector.get();
                if (loadedImageIds.length > 0) {
                    bridge.update(loadedImageIds, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);

                    if (newCacheSize >= 0) {
                        long sizeForTeaStore = (long) newCacheSize * SCALE_FACTOR;
                        CacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);

                        System.out.printf(
                            "[Monitor] iter=%4d  images=%4d  hit=%.2f miss=%.2f  -> PID_cache=%d  strategy=%s  real_bytes=%s  -> image=%s%n",
                            iter, loadedImageIds.length, hitMiss != null ? hitMiss.getMeanHitTime() : Double.NaN,
                            hitMiss != null ? hitMiss.getMeanMissTime() : Double.NaN, newCacheSize,
                            cache.getActiveStrategy(), realOccupation, result);
                    } else {
                        System.out.printf("[Monitor] iter=%4d  images=%4d  -> timeout (BIP cycle)%n", iter, loadedImageIds.length);
                    }

                } else {
                    System.out.printf("[Monitor] iter=%4d  images=  0   (no traffic or unreachable)  real_bytes=%s%n", iter, realOccupation);
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