package tools.spirals.cerberus237.siphonix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.BeninTrafficObservation;
import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.DatabaseAvailabilityObservation;
import tools.spirals.cerberus237.siphonix.strategies.javabip.CacheManagementStrategy;

/**
 * SiphoniX — Autonomic Manager Sidecar
 *
 * Launches three concurrent adaptation strategies:
 *   1. BeninTrafficObservation       (AdaptiFlow) — traffic surge detection
 *   2. DatabaseAvailabilityObservation (AdaptiFlow) — DB restart
 *   3. CacheManagementStrategy       (JavaBIP/PID) — adaptive cache control
 *
 * Environment variable:
 *   TARGET_URL   Base REST URL of the monitored service.
 *                Default (Docker): http://adaptable-teastore-image:8080/tools.descartes.teastore.image
 *                Default (local):  http://localhost:8083/tools.descartes.teastore.image
 *
 * Run options:
 *   Full sidecar (all strategies):
 *     java -cp target/io.github.brice10.siphonix-jar-with-dependencies.jar \
 *          tools.spirals.cerberus237.siphonix.SiphoniX
 *
 *   JavaBIP controller only (local dev):
 *     java -cp target/io.github.brice10.siphonix-jar-with-dependencies.jar \
 *          adapteastore.Main
 */
public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    // Docker default : service name in compose network
    // Local dev override : export TARGET_URL=http://localhost:8083/tools.descartes.teastore.image
    private static final String TARGET_SERVICE_URL = System.getenv()
            .getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image");

    public static void main(String[] args) {
        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);

        // Strategy 1 — AdaptiFlow : traffic surge
        if (BeninTrafficObservation.beninTrafficObservationScheduler == null)
            BeninTrafficObservation.getInstance();
        BeninTrafficObservation.beninTrafficObservationScheduler.start();
        logger.info("[SiphoniX] Traffic Surge Observation started.");

        // Strategy 2 — AdaptiFlow : database availability
        if (DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler == null)
            DatabaseAvailabilityObservation.getInstance();
        DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler.start();
        logger.info("[SiphoniX] Database Availability Observation started.");

        // Strategy 3 — JavaBIP / PID : adaptive cache control
        // Runs in its own thread to avoid blocking the two schedulers above
        String imageBaseUrl = TARGET_SERVICE_URL.endsWith("/rest")
                ? TARGET_SERVICE_URL.substring(0, TARGET_SERVICE_URL.length() - 5)
                : TARGET_SERVICE_URL;
        Thread cacheThread = new Thread(
                new CacheManagementStrategy(imageBaseUrl),
                "cache-management-javabip"
        );
        cacheThread.setDaemon(false);
        cacheThread.start();
        logger.info("[SiphoniX] Cache Management (JavaBIP) started.");

        // Keep main thread alive and handle graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[SiphoniX] Shutdown signal received. Stopping strategies...");
            if (BeninTrafficObservation.beninTrafficObservationScheduler != null)
                BeninTrafficObservation.beninTrafficObservationScheduler.stop();
            if (DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler != null)
                DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler.stop();
            cacheThread.interrupt();
            logger.info("[SiphoniX] All strategies stopped.");
        }));

        logger.info("[SiphoniX] All strategies running. Press Ctrl+C to stop.");
        try {
            cacheThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
