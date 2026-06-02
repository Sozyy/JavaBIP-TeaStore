package TeaStoreWithConnector;

import org.javabip.glue.TwoSynchronGlueBuilder;

public class TeaStoreWithConnectorGlue extends TwoSynchronGlueBuilder {

    private static final int N = 3; // Number of Spread + Client + Collect blocs

    private static final Class<?>[] CLIENTS = {
            Client1.class,
            Client2.class,
            Client3.class
    };
    private static final Class<?>[] COLLECTORS = {
            CollectConnector1.class,
            CollectConnector2.class,
            CollectConnector3.class
    };
    private static final Class<?>[] SPREADERS = {
            SpreadConnector1.class,
            SpreadConnector2.class,
            SpreadConnector3.class
    };

    @Override
    public void configure() {

        // =====================
        // === SYNCHRON WIRES ==
        // =====================

        // CC_i -> CC_(i-1) -> ... -> CC1
        for (int i = 0; i < N; i++) {
            Class<?> cc = COLLECTORS[i];

            if (i == N - 1) {
                synchron(Looper1.class, "loop").to(cc, "input1");
            } else {
                synchron(COLLECTORS[i + 1], "sendUp").to(cc, "input1");
            }

            synchron(Looper2.class, "loop").to(cc, "input2");

            synchron(CLIENTS[i], "sendRequest").to(cc, "input3");
        }

        // CC1 -> Server
        synchron(COLLECTORS[0], "sendUp").to(Server.class, "receiveRequests");

        // Server <-> DataProvider <-> PIDController
        synchron(Server.class, "sendToProvider").to(DataProvider.class, "receiveRequest");
        synchron(DataProvider.class, "sendResponseTime").to(PIDController.class, "receiveResponseTime");
        synchron(PIDController.class, "sendCacheSize").to(DataProvider.class, "receiveNewCacheSize");
        synchron(DataProvider.class, "notifyServer").to(Server.class, "receiveResponse");

        // Server -> SP1
        synchron(Server.class, "sendToClients").to(SPREADERS[0], "input");

        // SP_i -> SP_(i+1) and Client_i
        for (int i = 0; i < N; i++) {
            synchron(SPREADERS[i], "sendToClient").to(CLIENTS[i], "receiveResp");

            if (i < N - 1) {
                synchron(SPREADERS[i], "sendToNext").to(SPREADERS[i + 1], "input");
            }
        }

        // ==================
        // === DATA WIRES ===
        // ==================

        // Client_i -> CC_i (requests)
        for (int i = 0; i < N; i++) {
            data(CLIENTS[i], "requests").to(COLLECTORS[i], "additionalRequests");
        }

        // CC_(i+1) -> CC_i (previousRequests)
        for (int i = 0; i < N; i++) {
            if (i == N - 1) {
                data(Looper1.class, "getNothing").to(COLLECTORS[i], "previousRequests");
            } else {
                data(COLLECTORS[i + 1], "getRequests").to(COLLECTORS[i], "previousRequests");
            }
        }

        // CC1 -> Server
        data(COLLECTORS[0], "getRequests").to(Server.class, "requests");

        // Server -> SP1
        data(Server.class, "getResponses").to(SPREADERS[0], "responses");

        // SP_i -> SP_(i+1) et SP_i -> Client_i
        for (int i = 0; i < N; i++) {
            if (i < N - 1) {
                data(SPREADERS[i], "getResponses").to(SPREADERS[i + 1], "responses");
            }
            data(SPREADERS[i], "getResponses").to(CLIENTS[i], "responses");
        }

        // Server <-> DataProvider <-> PIDController (data, inchangé)
        data(Server.class, "request").to(DataProvider.class, "request");
        data(DataProvider.class, "responseTime").to(PIDController.class, "responseTime");
        data(PIDController.class, "newCacheSize").to(DataProvider.class, "newCacheSize");
        data(DataProvider.class, "response").to(Server.class, "response");
    }
}