package Adapteastore;

import akka.actor.ActorSystem;
import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;

import TeaStoreWithConnector.adaptiflow.TeaStorePersistenceCollector;
import TeaStoreWithConnector.adaptiflow.TeaStoreImageCacheUpdater;

/**
 * Phase 2 : integration avec le vrai AdaptableTeaStore (Docker Compose).
 *
 * Boucle de monitoring + ACTUATION :
 *   1. GET /persistence/rest/metrics/requests -> delta de requetes depuis le dernier poll
 *   2. delta > 0 : injecte dans le pipeline BIP -> PID calcule un nouveau cache
 *   3. delta = 0 : pas de trafic ou TeaStore injoignable -> on skip le cycle BIP
 *   4. Apres chaque cycle BIP reussi : POST /image/rest/image/setCacheSize avec la
 *      nouvelle taille calculee par le PID. Le service Image a deja un endpoint
 *      natif qui accepte ce contrat (long en bytes), donc l'actuation se fait
 *      SANS modifier AdaptableTeaStore.
 *
 * Note sur le contrat de la taille :
 *   Le PID raisonne en "nombre d'items" (typiquement 2..500). Le endpoint image
 *   attend des bytes. Pour l'instant on transmet la valeur telle quelle - une
 *   conversion items -> bytes pourra etre ajoutee plus tard (cf. SCALE_FACTOR
 *   ci-dessous, mis a 1 par defaut).
 *
 * Demarrage : docker compose up, puis mvn exec:java -Dexec.mainClass=...MainTeaStoreDemo
 * Arret propre : Ctrl+C -> CSV exporte.
 */
public class Main {

    // === Endpoints AdaptableTeaStore ===
    // Persistence : on lit ici le compteur de requetes (entree du PID).
    private static final String PERSISTENCE_BASE_URL =
            "http://localhost:8082/tools.descartes.teastore.persistence";

    // Image : on ecrit ici la nouvelle taille de cache (sortie du PID).
    // Ajuste le port selon le mapping de ton docker-compose. Avec le compose par
    // defaut du projet, seul webui (8080) est expose sur l'hote ; pour atteindre
    // 'image' depuis le hote, ajoute dans ton docker-compose :
    //     image:
    //       ports:
    //         - "8083:8080"
    // (ou n'importe quel autre port libre cote hote) et mets ce port ci-dessous.
    private static final String IMAGE_BASE_URL =
            "http://localhost:8083/tools.descartes.teastore.image";

    // === Parametres cache + PID ===
    private static final int    CACHE_CAPACITY      = 80;
    private static final int    MIN_CACHE_CAPACITY  = 2;
    private static final int    MAX_CACHE_CAPACITY  = 500;
    private static final int    IMAGE_UNIVERSE_SIZE = 100;
    private static final float  TARGET_TIME         = 10.0F;
    private static final float  KP = 0.9F, KI = 0.05F, KD = 0.15F;

    /** Facteur items -> bytes. Mis a 1 pour l'instant (cf. note en tete). */
    private static final long   SCALE_FACTOR   = 1L;

    /** Intervalle entre deux polls (augmenter si TeaStore est lent). */
    private static final long POLL_INTERVAL_MS = 2_000L;
    /** Timeout par cycle BIP ; doit etre superieur au temps de traitement DataProvider. */
    private static final long CYCLE_TIMEOUT_MS = 10_000L;

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("system");
        EngineFactory engineFactory = new EngineFactory(system);

        BIPGlue glue = new SimpleBridgeGlue().build();
        BIPEngine engine = engineFactory.create("glue", glue);

        LRUCache cache            = new LRUCache(CACHE_CAPACITY);
        DataProvider dataProvider = new DataProvider(cache, IMAGE_UNIVERSE_SIZE);
        PIDController pid         = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY,
                MAX_CACHE_CAPACITY, TARGET_TIME,
                KP, KI, KD);

        Bridge bridge = new Bridge(cache);
        TeaStorePersistenceCollector collector = new TeaStorePersistenceCollector(PERSISTENCE_BASE_URL);
        TeaStoreImageCacheUpdater imageUpdater = new TeaStoreImageCacheUpdater(IMAGE_BASE_URL);

        engine.register(dataProvider, "dataProvider", true);
        engine.register(pid,          "pidController", true);
        engine.register(bridge,       "bridge",        true);

        System.out.println("=== START (TeaStore integration, with actuation) ===");
        System.out.println("Reading  : " + PERSISTENCE_BASE_URL + "/rest/metrics/requests");

        engine.specifyGlue(glue);
        engine.start();
        engine.execute();

        // Ctrl+C -> interrompt le thread principal -> finally exporte le CSV
        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

        int iter = 0;
        try {
            while (true) {
                int delta = collector.get();

                if (delta > 0) {
                    bridge.update(delta, "TeaStore poll");
                    int newCacheSize = bridge.waitForCycleAndGetCacheSize(CYCLE_TIMEOUT_MS);

                    if (newCacheSize >= 0) {
                        // === ACTUATION : pousser la nouvelle taille vers TeaStore (service Image) ===
                        long sizeForTeaStore = (long) newCacheSize * SCALE_FACTOR;
                        TeaStoreImageCacheUpdater.UpdateResult result = imageUpdater.update(sizeForTeaStore);

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
                System.err.println("[MainTeaStoreDemo] CSV export failed: " + e.getMessage());
            }
            try { engine.stop();              } catch (Exception ignored2) {}
            try { engineFactory.destroy(engine); } catch (Exception ignored2) {}
            try { system.terminate();         } catch (Exception ignored2) {}
        }
    }
}
