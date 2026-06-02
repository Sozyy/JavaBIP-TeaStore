package adapteastore;

import akka.actor.ActorSystem;
import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;

/**
 * Loop of monitoring :
 *   1. GET /persistence/rest/metrics/requests -> delta of requests since last poll
 *   2. delta > 0 : getting into the JavaBIP pipeline, PID calculates a new cache size
 *   3. delta = 0 : no traffic or no TeaStore connection, this cycle is skipped
 *   4. After each JavaBIP cycle, the new cache size is POSTed to /image/rest/image/setCacheSize.
 */
public class Main {
    /**
     * Ports have been changed in experimentation-platform/Sources/examples/docker/docker-compose_default.yaml
     * image:                                               * persistence:
     *     image: cerberus237/adaptable-teastore-image      *     image: cerberus237/adaptable-teastore-persistence
     *     expose:                                          *     expose:
     *       - "8080"                                       *       - "8080"
     *     ports:                                           *     ports:
     *       - "8083:8080"                                  *       - "8082:8080"
     */
    private static final String PERSISTENCE_BASE_URL = "http://localhost:8082/tools.descartes.teastore.persistence";
    private static final String IMAGE_BASE_URL       = "http://localhost:8083/tools.descartes.teastore.image";

    private static final int    CACHE_CAPACITY      = 80;
    private static final int    MIN_CACHE_CAPACITY  = 2;
    private static final int    MAX_CACHE_CAPACITY  = 500;      // rise up to be in bytes
    private static final int    IMAGE_UNIVERSE_SIZE = 100;      // may not be use in new DP logic
    private static final float  TARGET_TIME         = 10.0F;    // should change
    private static final float  KP = 0.9F, KI = 0.05F, KD = 0.15F;

    private static final long SCALE_FACTOR     = 1L;        // TODO : change in future version, factor to go from PID result to bytes
    private static final long POLL_INTERVAL_MS = 2_000L;    // timeout between two TeaStore polls
    private static final long CYCLE_TIMEOUT_MS = 10_000L;   // timeout per BIP cycle, has to be superior then DataProvider processing time

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("system");
        EngineFactory engineFactory = new EngineFactory(system);

        BIPGlue glue = new Glue().build();
        BIPEngine engine = engineFactory.create("glue", glue);

        LRUCache cache            = new LRUCache(CACHE_CAPACITY);
        DataProvider dataProvider = new DataProvider(cache, IMAGE_UNIVERSE_SIZE);
        PIDController pid         = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY,
                MAX_CACHE_CAPACITY, TARGET_TIME,
                KP, KI, KD);

        Bridge bridge = new Bridge(cache);
        PersistenceCollector collector = new PersistenceCollector(PERSISTENCE_BASE_URL);
        CacheUpdater imageUpdater = new CacheUpdater(IMAGE_BASE_URL);

        engine.register(dataProvider, "dataProvider",  true);
        engine.register(pid,          "pidController", true);
        engine.register(bridge,       "bridge",        true);

        System.out.println("=== START (TeaStore integration, with actuation) ===");
        System.out.println("Reading  : " + PERSISTENCE_BASE_URL + "/rest/metrics/requests");

        engine.specifyGlue(glue);
        engine.start();
        engine.execute();

        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

        int iter = 0;
        try {
            while (true) {  // not supposed to stop
                int delta = collector.get();

                if (delta > 0) {
                    bridge.update(delta, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);

                    if (newCacheSize >= 0) {
                        long sizeForTeaStore = (long) newCacheSize * SCALE_FACTOR;
                        CacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);

                        System.out.printf("[Monitor] iter=%4d  delta=%4d  -> PID_cache=%d  -> image=%s%n", iter, delta, newCacheSize, result);
                    } else {
                        System.out.printf("[Monitor] iter=%4d  delta=%4d  -> timeout (BIP cycle)%n", iter, delta);
                    }

                } else {
                    System.out.printf("[Monitor] iter=%4d  delta=  0   (no traffic or unreachable)%n", iter);
                }

                iter++;
                Thread.sleep(POLL_INTERVAL_MS);
            }

        } catch (InterruptedException ignored) {
            System.out.println("[Monitor] interrupted");
        } finally {
            System.out.printf("%n=== STOP (iter=%d) ===%n", iter);
            try {
                String csvPath = "src/main/java/TeaStoreWithConnector/output/teastore_history_KP"
                        + KP + "_KI" + KI + "_KD" + KD + ".csv";
                cache.exportHistoryCsv(csvPath);
            } catch (Exception e) {
                System.err.println("[Main] CSV export failed: " + e.getMessage());
            }
            try { engine.stop();                 } catch (Exception ignored2) {}
            try { engineFactory.destroy(engine); } catch (Exception ignored2) {}
            try { system.terminate();            } catch (Exception ignored2) {}
        }
    }
}
