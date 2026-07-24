package tools.spirals.cerberus237.siphonix.strategies.javabip;

import java.io.IOException;

import akka.actor.ActorSystem;

import tools.spirals.cerberus237.siphonix.strategies.javabip.component.Bridge;
import tools.spirals.cerberus237.siphonix.strategies.javabip.component.DataProvider;
import tools.spirals.cerberus237.siphonix.strategies.javabip.component.Glue;
import tools.spirals.cerberus237.siphonix.strategies.javabip.component.PIDController;
import tools.spirals.cerberus237.siphonix.strategies.javabip.http.CacheEntriesCollector;
import tools.spirals.cerberus237.siphonix.strategies.javabip.http.CacheHitMissCollector;
import tools.spirals.cerberus237.siphonix.strategies.javabip.http.CacheMetricsCollector;
import tools.spirals.cerberus237.siphonix.strategies.javabip.http.CacheUpdater;
import tools.spirals.cerberus237.siphonix.strategies.javabip.cache.SwitchableCache;

import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheHitMissMetrics;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheMetrics;

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
 * Real metrics: the hit/miss timing weights (HIT_WEIGHT/MISS_WEIGHT below) are only fallback
 * defaults, overridden at runtime from the image service's real GET /rest/metrics/cache-performance
 * (see CacheHitMissCollector). The shadow cache/PID still reason in item counts internally; the
 * real, byte-based measurements (GET /rest/metrics/cache-entries byteSize per image, and
 * GET /rest/metrics/cache-metrics maxCacheSize) are only used at the very end of each cycle, to
 * convert the PID's item-count decision into a byte target meaningful to TeaStore's real cache
 * before it is POSTed via CacheUpdater — see the sizeForTeaStore computation below.
 */
public class CacheManagementStrategy implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(CacheManagementStrategy.class);
    private final String imageBaseUrl;

    private static final String DEFAULT_CACHE_STRATEGY = "LRU";
    private static final int    CACHE_CAPACITY      = 80;
    private static final int    MIN_CACHE_CAPACITY  = 2;
    private static final int    MAX_CACHE_CAPACITY  = 500;
    // TARGET_TIME=10.0F (inherited from CollectiveTeaStore) was ~100x smaller than the real
    // meanHitTime/meanMissTime measured on AdaptableTeaStore (fractions of a ms), so the error
    // was always deeply negative and, combined with unbounded integral growth, collapsed the
    // cache to MIN_CACHE_CAPACITY within a handful of cycles regardless of actual traffic.
    // Retuned to a user-defined acceptable per-request time (2s = 2000ms, same unit as the real
    // hit/miss measurements). KP/KI/KD are scaled down by the same 200x factor (2000/10) so the
    // control loop reacts to a given *relative* error the same way it did at the old scale,
    // instead of saturating the output on the first cycle.
    private static final float TARGET_TIME         = 2000.0F;
    private static final float KP = 0.9F;
    private static final float KI = 0.05F;
    private static final float KD = 0.15F;
    private static final float HIT_WEIGHT  = 0.2F; 
    private static final float MISS_WEIGHT = 2.0F; 
    private static final long  POLL_INTERVAL_MS  = 2_000L;
    private static final long  CYCLE_TIMEOUT_MS  = 10_000L;

    // Written every iteration (not just on a graceful shutdown) because the "image" container
    // has no shutdown hook and is stopped by the daemon JVM just being killed (see javabipThread
    // in SiphoniX): waiting for a clean interrupt to flush the history would lose it entirely.
    // Mounted to the host via the "image" service's ./output volume (see docker-compose.yml) so
    // scripts/plot_cache_history.py can read it while or after the strategy runs.
    private static final String OUTPUT_DIR = System.getenv().getOrDefault("CACHE_HISTORY_DIR", "/opt/siphonix/output");
    private static final String CACHE_HISTORY_CSV = String.format("%s/cache_history_KP%s_KI%s_KD%s.csv", OUTPUT_DIR, KP, KI, KD);

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
            DataProvider    dataProvider = new DataProvider(cache, HIT_WEIGHT, MISS_WEIGHT);
            PIDController   pid          = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY, MAX_CACHE_CAPACITY, TARGET_TIME, KP, KI, KD);
            Bridge          bridge       = new Bridge(cache);

            CacheUpdater           imageUpdater          = new CacheUpdater(imageBaseUrl);
            CacheEntriesCollector  imageCollector        = new CacheEntriesCollector("image", imageBaseUrl);
            CacheHitMissCollector  hitMissCollector      = new CacheHitMissCollector("image", imageBaseUrl);
            CacheMetricsCollector  cacheMetricsCollector = new CacheMetricsCollector("image", imageBaseUrl);

            engine.register(dataProvider, "dataProvider",  true);
            engine.register(pid,          "pidController", true);
            engine.register(bridge,       "bridge",        true);

            engine.specifyGlue(glue);
            engine.start();
            engine.execute();

            // The JVM runs as root inside the "image" container, so files it creates under the
            // bind-mounted OUTPUT_DIR come out root-owned with the default 0755 dir permissions
            // on the host -- making the CSV impossible to delete/replace from the host side
            // without root (directory *write* permission, not file ownership, is what Linux
            // checks for unlink). Chmod'ing the dir world-writable lets any host user manage it.
            try {
                java.nio.file.Path outputDir = java.nio.file.Paths.get(OUTPUT_DIR);
                java.nio.file.Files.createDirectories(outputDir);
                java.nio.file.Files.setPosixFilePermissions(outputDir,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rwxrwxrwx"));
            } catch (Exception e) {
                LOG.warn("[CacheManagement] could not make {} world-writable: {}", OUTPUT_DIR, e.getMessage());
            }

            int iter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                CacheHitMissMetrics hitMiss = hitMissCollector.get();
                Double meanHit  = null;
                Double meanMiss = null;
                if (hitMiss != null) {
                    meanHit  = hitMiss.getMeanHitTime();
                    meanMiss = hitMiss.getMeanMissTime();
                    dataProvider.updateTimings(meanHit, meanMiss);
                }
                CacheMetrics realOccupation = cacheMetricsCollector.get();

                int[] loadedImageIds = imageCollector.get();
                if (loadedImageIds.length > 0) {
                    bridge.update(loadedImageIds, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);
                    if (newCacheSize >= 0) {
                        // Convert the PID's item-count decision into a byte target using the real,
                        // measured average image size (cache-entries.byteSize), rather than treating
                        // item count as if it were already bytes. Do NOT clamp against realOccupation's
                        // maxCacheSize: setCacheSize() below overwrites TeaStore's real maxCacheSize with
                        // whatever we send, so that value is our own previous output, not a fixed ceiling.
                        // Clamping against it created a one-way ratchet (max can only shrink cycle over
                        // cycle, never grow back) that collapsed the cache to MIN_CACHE_CAPACITY and stuck.
                        double avgBytesPerImage = imageCollector.getLastAverageByteSize();
                        long sizeForTeaStore = Math.round(newCacheSize * avgBytesPerImage) + 1; // +1 not to get to 0 bytes
                        
                        CacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);
                        LOG.info("[CacheManagement] iter={} images={} hit={} miss={} -> PID_cache={} items avg_bytes/img={} -> bytes_target={} strategy={} real_bytes={} -> image={}",
                                iter, loadedImageIds.length, meanHit, meanMiss,
                                newCacheSize, avgBytesPerImage, sizeForTeaStore, cache.getActiveStrategy(), realOccupation, result);
                    } else {
                        LOG.warn("[CacheManagement] iter={} images={} -> BIP cycle timeout", iter, loadedImageIds.length);
                    }
                } else {
                    LOG.debug("[CacheManagement] iter={} images=0 (no traffic or unreachable) real_bytes={}", iter, realOccupation);
                }

                try {
                    cache.exportHistoryCsv(CACHE_HISTORY_CSV);
                } catch (IOException e) {
                    LOG.warn("[CacheManagement] failed to export cache history to {}: {}", CACHE_HISTORY_CSV, e.getMessage());
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