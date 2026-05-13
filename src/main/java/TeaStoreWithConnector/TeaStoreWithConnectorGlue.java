////package TeaStoreWithConnector;
////
////import org.javabip.glue.TwoSynchronGlueBuilder;
////
////public class TeaStoreWithConnectorGlue extends TwoSynchronGlueBuilder {
////    @Override
////    public void configure() {
////        synchron(Looper1.class, "loop")       .to(CollectConnector3.class, "input1");
////        synchron(Looper2.class, "loop")       .to(CollectConnector3.class, "input2");
////        synchron(Client3.class, "sendRequest").to(CollectConnector3.class, "input3");
////
////        synchron(CollectConnector3.class, "sendUp").to(CollectConnector2.class, "input1");
////        synchron(Looper2.class, "loop")            .to(CollectConnector2.class, "input2");
////        synchron(Client2.class, "sendRequest")     .to(CollectConnector2.class, "input3");
////
////        synchron(CollectConnector2.class, "sendUp").to(CollectConnector1.class, "input1");
////        synchron(Looper2.class, "loop")            .to(CollectConnector1.class, "input2");
////        synchron(Client1.class, "sendRequest")     .to(CollectConnector1.class, "input3");
////
////        synchron(CollectConnector1.class, "sendUp").to(Server.class, "receiveRequests");
////
////        // ---
////        synchron(Server.class, "sendToClients")   .to(SpreadConnector1.class, "input");
////
////        synchron(SpreadConnector1.class, "sendUp").to(Client1.class, "receiveResp");
////        synchron(SpreadConnector1.class, "sendUp").to(SpreadConnector2.class, "input");
////
////        synchron(SpreadConnector2.class, "sendUp").to(Client2.class, "receiveResp");
////        synchron(SpreadConnector2.class, "sendUp").to(SpreadConnector3.class, "input");
////
////        synchron(SpreadConnector3.class, "sendUp").to(Client3.class, "receiveResp");
////
////
////        // ==================
////        // === DATA WIRES ===
////        // ==================
////
////        // Cln -> CCn
////        data(Client1.class, "requests").to(CollectConnector1.class, "additionalRequests");
////        data(Client2.class, "requests").to(CollectConnector2.class, "additionalRequests");
////        data(Client3.class, "requests").to(CollectConnector3.class, "additionalRequests");
////
////        // CCn -> CCn-1
////        data(CollectConnector3.class, "getRequests").to(CollectConnector2.class, "previousRequests");
////        data(CollectConnector2.class, "getRequests").to(CollectConnector1.class, "previousRequests");
////        data(Looper1.class, "getNothing").to(CollectConnector3.class, "previousRequests");
////
////        // CC1 -> Server
////        data(CollectConnector1.class, "getRequests").to(Server.class, "requests");
////
////        // Server -> SP1
////        data(Server.class, "getResponses").to(SpreadConnector1.class, "responses");
////
////        // SPn -> SPn+1
////        data(SpreadConnector1.class, "getResponses").to(SpreadConnector2.class, "responses");
////        data(SpreadConnector2.class, "getResponses").to(SpreadConnector3.class, "responses");
////
////        // SPn -> Cln
////        data(SpreadConnector1.class, "getResponses").to(Client1.class, "responses");
////        data(SpreadConnector2.class, "getResponses").to(Client2.class, "responses");
////        data(SpreadConnector3.class, "getResponses").to(Client3.class, "responses");
////    }
////}
//
//package TeaStoreWithConnector;
//
//import org.javabip.glue.TwoSynchronGlueBuilder;
//
//public class TeaStoreWithConnectorGlue extends TwoSynchronGlueBuilder {
//    @Override
//    public void configure() {
//        // C3 -> CC3
//        synchron(Looper1.class, "loop")       .to(CollectConnector3.class, "input1");
//        synchron(Looper2.class, "loop")       .to(CollectConnector3.class, "input2");
//        synchron(Client3.class, "sendRequest").to(CollectConnector3.class, "input3");
//
//        // CC3 & C2 -> CC2
//        synchron(CollectConnector3.class, "sendUp").to(CollectConnector2.class, "input1");
//        synchron(Looper2.class, "loop")            .to(CollectConnector2.class, "input2");
//        synchron(Client2.class, "sendRequest")     .to(CollectConnector2.class, "input3");
//
//        // CC2 & C1 -> CC1
//        synchron(CollectConnector2.class, "sendUp").to(CollectConnector1.class, "input1");
//        synchron(Looper2.class, "loop")            .to(CollectConnector1.class, "input2");
//        synchron(Client1.class, "sendRequest")     .to(CollectConnector1.class, "input3");
//
//        // CC1 -> S
//        synchron(CollectConnector1.class, "sendUp").to(Server.class, "receiveRequests");
//
//        // S -> DP
//        synchron(Server.class, "sendToProvider").to(DataProvider.class, "receiveRequest");
//
//        // DP -> PID
//        synchron(DataProvider.class, "sendResponseTime").to(PIDController.class, "receiveResponseTime");
//
//        // PID -> DP
//        synchron(PIDController.class, "sendCacheSize").to(DataProvider.class, "receiveNewCacheSize");
//
//        // DP -> S
//        synchron(DataProvider.class, "notifyServer").to(Server.class, "receiveResponse");
//
//        // S -> SP1
//        synchron(Server.class, "sendToClients").to(SpreadConnector1.class, "input");
//
//        // SP1 -> C1 & SP2
//        synchron(SpreadConnector1.class, "sendToClient").to(Client1.class, "receiveResp");
//        synchron(SpreadConnector1.class, "sendToNext")  .to(SpreadConnector2.class, "input");
//
//        // SP2 -> C2 & SP3
//        synchron(SpreadConnector2.class, "sendToClient").to(Client2.class, "receiveResp");
//        synchron(SpreadConnector2.class, "sendToNext")  .to(SpreadConnector3.class, "input");
//
//        // SP3 -> C3
//        synchron(SpreadConnector3.class, "sendToClient").to(Client3.class, "receiveResp");
//
//        // ==================
//        // === DATA WIRES ===
//        // ==================
//
//        // Client_n -> CC_n
//        data(Client1.class, "requests").to(CollectConnector1.class, "additionalRequests");
//        data(Client2.class, "requests").to(CollectConnector2.class, "additionalRequests");
//        data(Client3.class, "requests").to(CollectConnector3.class, "additionalRequests");
//
//        // CC_n -> CC_(n-1)
//        data(CollectConnector3.class, "getRequests").to(CollectConnector2.class, "previousRequests");
//        data(CollectConnector2.class, "getRequests").to(CollectConnector1.class, "previousRequests");
//        data(Looper1.class, "getNothing").to(CollectConnector3.class, "previousRequests");
//
//        // CC1 -> Server
//        data(CollectConnector1.class, "getRequests").to(Server.class, "requests");
//
//        // Server -> SP1
//        data(Server.class, "getResponses").to(SpreadConnector1.class, "responses");
//
//        // SP_n -> SP_(n+1)
//        data(SpreadConnector1.class, "getResponses").to(SpreadConnector2.class, "responses");
//        data(SpreadConnector2.class, "getResponses").to(SpreadConnector3.class, "responses");
//
//        // SP_n -> Client_n
//        data(SpreadConnector1.class, "getResponses").to(Client1.class, "responses");
//        data(SpreadConnector2.class, "getResponses").to(Client2.class, "responses");
//        data(SpreadConnector3.class, "getResponses").to(Client3.class, "responses");
//
//        // S -> DP
//        data(Server.class, "request").to(DataProvider.class, "request");
//
//        // DP -> PID
//        data(DataProvider.class, "responseTime").to(PIDController.class, "responseTime");
//
//        // PID -> DP
//        data(PIDController.class, "newCacheSize").to(DataProvider.class, "newCacheSize");
//
//        // DP -> S
//        data(DataProvider.class, "response").to(Server.class, "response");
//    }
//}

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