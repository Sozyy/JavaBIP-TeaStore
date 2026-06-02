package TeaStoreWithConnector;

import akka.actor.ActorSystem;
import org.javabip.api.BIPEngine;
import org.javabip.api.BIPGlue;
import org.javabip.engine.factory.EngineFactory;

public class Main {
    private static final int MAX_CLIENT          = 3 + 1; // +1 is due to index 0 never being used
    private static final int CLIENT_OBJECTIVES   = 600;
    private static final int CACHE_CAPACITY      = 20;
    private static final int MIN_CACHE_CAPACITY  = 2;
    private static final int MAX_CACHE_CAPACITY  = 500;
    private static final int MAX_REQUEST_SIZE    = 30;
    private static final int IMAGE_UNIVERSE_SIZE = 100;
    private static final float TARGET_TIME = 20.0F;
    private static final float KP = 0.8F;   // 0.80
    private static final float KI = 0.05F;  // 0.05
    private static final float KD = 0.2F;   // 0.2

    private ActorSystem system;
    private EngineFactory engineFactory;

    private void initialize() {
        system = ActorSystem.create("system");
        engineFactory = new EngineFactory(system);
    }

    private void cleanup() {
        if (system != null) {
            system.terminate();
        }
    }

    public void runDemo(/*float KP, float KI, float KD*/) {
        BIPEngine engine = null;
        initialize();

        try {
            BIPGlue glue = new TeaStoreWithConnectorGlue().build();
            engine = engineFactory.create("glue", glue);

            Server server               = new Server(MAX_CLIENT);
            LRUCache cache              = new LRUCache(CACHE_CAPACITY);
            DataProvider dataProvider   = new DataProvider(cache, IMAGE_UNIVERSE_SIZE);
            PIDController pidController = new PIDController(CACHE_CAPACITY, MIN_CACHE_CAPACITY, MAX_CACHE_CAPACITY, TARGET_TIME, KP, KI, KD);

            Client1 client1 = new Client1(1, CLIENT_OBJECTIVES, MAX_REQUEST_SIZE, MAX_CLIENT);
            Client2 client2 = new Client2(2, CLIENT_OBJECTIVES, MAX_REQUEST_SIZE, MAX_CLIENT);
            Client3 client3 = new Client3(3, CLIENT_OBJECTIVES, MAX_REQUEST_SIZE, MAX_CLIENT);

            CollectConnector1 collectConnector1 = new CollectConnector1(MAX_CLIENT);
            CollectConnector2 collectConnector2 = new CollectConnector2(MAX_CLIENT);
            CollectConnector3 collectConnector3 = new CollectConnector3(MAX_CLIENT);

            SpreadConnector1 spreadConnector1 = new SpreadConnector1(MAX_CLIENT, true);
            SpreadConnector2 spreadConnector2 = new SpreadConnector2(MAX_CLIENT, true);
            SpreadConnector3 spreadConnector3 = new SpreadConnector3(MAX_CLIENT, false);

            Looper1 looper1 = new Looper1(MAX_CLIENT);
            Looper2 looper2 = new Looper2();

            engine.register(server, "server", true);

            engine.register(dataProvider, "dataProvider", true);
            engine.register(pidController, "pidController", true);

            engine.register(client1, "client1", true);
            engine.register(client2, "client2", true);
            engine.register(client3, "client3", true);

            engine.register(collectConnector1, "collectConnector1", true);
            engine.register(collectConnector2, "collectConnector2", true);
            engine.register(collectConnector3, "collectConnector3", true);

            engine.register(spreadConnector1, "spreadConnector1", true);
            engine.register(spreadConnector2, "spreadConnector2", true);
            engine.register(spreadConnector3, "spreadConnector3", true);

            engine.register(looper1, "looper1", true);
            engine.register(looper2, "looper2", true);

            System.out.println("=== START ===");
            engine.specifyGlue(glue);
            engine.start();
            engine.execute();

            while ((!client1.isDone() || !client2.isDone() || !client3.isDone()) && cache.getCapacity() < MAX_CACHE_CAPACITY) {
                //System.out.println("Client 1: " + c1.isDone() + ", Client 2: " + c2.isDone() + ", Client 3: " + c3.isDone());
                Thread.sleep(1);
            }

            // make the name depends on P, I and D
            cache.exportHistoryCsv("src/main/java/TeaStoreWithConnector/output/cache_history_KP" + KP + "_KI" + KI + "_KD" + KD + ".csv");

            engine.stop();
            engineFactory.destroy(engine);
            System.out.println("=== STOP ===");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            cleanup();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== MAIN ===");
        float maxKP     = 1.5F;
        float maxKI     = 1.0F;
        float maxKD     = 1.0F;
        float stepKP    = 0.1F;
        float stepKI    = 0.05F;
        float stepKD    = 0.05F;

        int totalIteration = (int) ((maxKP / stepKP) * (maxKI / stepKI) * (maxKD / stepKD));
        int cpt = 0;

        new Main().runDemo(); // run the demo with the initial values of KP, KI and KD

        // testing a lot of configurations to find a good one (very long to run)
        /*for (float d = 0.0F; d < maxKD; d += stepKD) {
            for (float i = 0.0F; i < maxKI; i += stepKI) {
                for (float p = stepKP; p < maxKP; p += stepKP) {
                    System.out.println(" >>> Running with KP = " + p + ", KI = " + i + ", KD = " + d);
                    new Main().runDemo(p, i, d);
                    cpt++;
                    System.out.printf("Progress: %d/%d (%.3f%%)%n", cpt, totalIteration, (float) cpt * 100 / totalIteration);
                }
            }
        }*/
    }
}

