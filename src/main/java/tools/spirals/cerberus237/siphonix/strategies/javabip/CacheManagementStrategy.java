package tools.spirals.cerberus237.siphonix.strategies.javabip;

import akka.actor.ActorSystem;
import controller.Bridge;
import controller.CacheUpdater;
import controller.DataProvider;
import controller.Glue;
import controller.PIDController;
import controller.ServiceCollector;
import controller.cache.SwitchableCache;

import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CacheManagementStrategy is the SiphoniX strategy wrapping the adapteastore JavaBIP controller.
 *
 * It runs as a long-lived thread alongside SiphoniX's other AdaptiFlow-based strategies
 * (BeninTrafficObservation, DatabaseAvailabilityObservation).
 *
 * Architecture:
 *   SiphoniX.main()
 *     ├── BeninTrafficObservation     (AdaptiFlow / traffic surge)
 *     ├── DatabaseAvailabilityObservation (AdaptiFlow / DB restart)
 *     └── CacheManagementStrategy     (JavaBIP / PID cache control)  <-- this class
 *
 * The TARGET_URL env variable controls which image service to target.
 * Defaults to http://localhost:8083/tools.descartes.teastore.image for local dev.
 *
 * Alternatively, you can run adapteastore.Main directly as a standalone process.
 */
public class CacheManagementStrategy implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(CacheManagementStrategy.class);
    private final String imageBaseUrl;

    private static final String DEFAULT_CACHE_STRATEGY = "LRU";
    private static final int   CACHE_CAPACITY      = 80;
    private static final int   MIN_CACHE_CAPACITY  = 2;
    private static final int   MAX_CACHE_CAPACITY  = 500;
    private static final int   IMAGE_UNIVERSE_SIZE = 100;
    private static final float TARGET_TIME         = 10.0F;
    private static final float KP = 0.9F;
    private static final float KI = 0.05F;
    private static final float KD = 0.15F;
    private static final float HIT_WEIGHT  = 0.2F;
    private static final float MISS_WEIGHT = 2.0F;
    private static final long  SCALE_FACTOR      = 1L;
    private static final long  POLL_INTERVAL_MS  = 2_000L;
    private static final long  CYCLE_TIMEOUT_MS  = 10_000L;

    /**
     * @param imageBaseUrl e.g. "http://localhost:8083/tools.descartes.teastore.image"
     */
    public CacheManagementStrategy(String imageBaseUrl) {
        this.imageBaseUrl = imageBaseUrl;
    }

    @Override
    public void run() {
        LOG.info("[CacheManagement] Starting JavaBIP cache controller targeting {}", imageBaseUrl);

        ActorSystem   system        = null;
        EngineFactory engineFactory = null;
        BIPEngine     engine        = null;

        try {
            system        = ActorSystem.create("system");
            engineFactory = new EngineFactory(system);
            BIPGlue glue  = new Glue().build();
            engine        = engineFactory.create("glue", glue);

            SwitchableCache cache        = new SwitchableCache(DEFAULT_CACHE_STRATEGY, CACHE_CAPACITY);
            DataProvider    dataProvider = new DataProvider(cache, IMAGE_UNIVERSE_SIZE, HIT_WEIGHT, MISS_WEIGHT);
            PIDController   pid          = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY, MAX_CACHE_CAPACITY, TARGET_TIME, KP, KI, KD);
            Bridge          bridge       = new Bridge(cache);

            CacheUpdater     imageUpdater   = new CacheUpdater(imageBaseUrl);
            ServiceCollector imageCollector = new ServiceCollector("image", imageBaseUrl, "/rest/metrics/requests");

            engine.register(dataProvider, "dataProvider",  true);
            engine.register(pid,          "pidController", true);
            engine.register(bridge,       "bridge",        true);

            engine.specifyGlue(glue);
            engine.start();
            engine.execute();

            int iter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                int delta = imageCollector.get();
                if (delta > 0) {
                    bridge.update(delta, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);
                    if (newCacheSize >= 0) {
                        long sizeForTeaStore = (long) newCacheSize * SCALE_FACTOR;
                        CacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);
                        LOG.info("[CacheManagement] iter={} delta={} -> PID_cache={} strategy={} -> image={}",
                                iter, delta, newCacheSize, cache.getActiveStrategy(), result);
                    } else {
                        LOG.warn("[CacheManagement] iter={} delta={} -> BIP cycle timeout", iter, delta);
                    }
                } else {
                    LOG.debug("[CacheManagement] iter={} delta=0 (no traffic or unreachable)", iter);
                }
                iter++;
                Thread.sleep(POLL_INTERVAL_MS);
            }

        } catch (InterruptedException e) {
            LOG.info("[CacheManagement] Interrupted, shutting down.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("[CacheManagement] Fatal error", e);
        } finally {
            if (engine        != null) try { engine.stop();                 } catch (Exception ignored) {}
            if (engineFactory != null) try { engineFactory.destroy(engine); } catch (Exception ignored) {}
            if (system        != null) try { system.terminate();            } catch (Exception ignored) {}
            LOG.info("[CacheManagement] Stopped.");
        }
    }
}
