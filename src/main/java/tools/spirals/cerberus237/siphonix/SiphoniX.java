package tools.spirals.cerberus237.siphonix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.CacheSizeAdaptationObservation;
import tools.spirals.cerberus237.siphonix.strategies.javabip.CacheManagementStrategy;

/**
 * Entry point of the SiphoniX autonomic manager sidecar.
 *
 * Starts two independent, co-located strategies targeting the same image service,
 * neither one depending on the other's code:
 *   - CacheSizeAdaptationObservation : AdaptiFlow-based observation scenario (Arléon)
 *   - CacheManagementStrategy        : JavaBIP-based PID cache controller (Julien)
 *
 * Note: this class only wires and starts the two strategies as threads — it does not itself
 * poll or process anything. The JavaBIP PID loop, the real-metrics polling (cache-entries,
 * cache-performance, cache-metrics) and all the controller logic live in CacheManagementStrategy;
 * that is the file to look at to see the actual behaviour.
 */
public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    // Convention partagée avec CacheSizeAdaptationObservation : TARGET_URL inclut le suffixe /rest.
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault(
            "TARGET_URL", "http://image:8080/tools.descartes.teastore.image/rest");

    // CacheManagementStrategy (comme CacheUpdater / CacheEntriesCollector) attend une base URL
    // SANS le suffixe /rest, puisque chaque appel REST l'ajoute lui-même à l'endpoint appelé.
    // On dérive donc IMAGE_BASE_URL depuis TARGET_SERVICE_URL pour n'avoir qu'une seule variable
    // d'env à configurer (TARGET_URL) pour les deux stratégies.
    private static final String IMAGE_BASE_URL = TARGET_SERVICE_URL.endsWith("/rest")
            ? TARGET_SERVICE_URL.substring(0, TARGET_SERVICE_URL.length() - "/rest".length())
            : TARGET_SERVICE_URL;

    public static void main(String[] args) {
        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);

        // --- Stratégie AdaptiFlow (Arléon) ---
        if (CacheSizeAdaptationObservation.cacheSizeAdaptationObservationScheduler == null)
            CacheSizeAdaptationObservation.getInstance();
        CacheSizeAdaptationObservation.cacheSizeAdaptationObservationScheduler.start();
        logger.info("[SiphoniX] Cache Size Adaptation Observation Start");

        // --- Stratégie JavaBIP (contrôleur PID) ---
        Thread javabipThread = new Thread(
                new CacheManagementStrategy(IMAGE_BASE_URL),
                "javabip-cache-management"
        );
        javabipThread.setDaemon(true);
        javabipThread.start();
        logger.info("[SiphoniX] JavaBIP Cache Management Strategy Start (target: {})", IMAGE_BASE_URL);
        logger.info("[SiphoniX] PID process variable now driven by real metrics: {}/rest/metrics/cache-performance " +
                "(meanHitTime/meanMissTime), occupation logged from {}/rest/metrics/cache-metrics",
                IMAGE_BASE_URL, IMAGE_BASE_URL);

        // javabipThread is a daemon thread: without joining it here, main() returns immediately
        // and the JVM shuts down right away since no non-daemon thread is left to keep it alive.
        try {
            javabipThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}



/*

dans siphonix ->

mvn clean install
cd samples/adaptable-teastore-image
sh run.debug.sh
docker ps -> récupérer l'id du conteneur image
docker logs -f l'id 
 */