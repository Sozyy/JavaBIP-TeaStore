package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "receiveRequests", type = PortType.enforceable),
        @Port(name = "sendToProvider",  type = PortType.enforceable),
        @Port(name = "receiveResponse", type = PortType.enforceable),
        @Port(name = "sendToClients",   type = PortType.enforceable)
})
@ComponentType(name = "Server", initial = "IDLE")
public class Server {
    private int currentRequestId = 0;
    private int[] requests;
    private int[] responses;

    public Server(int maxClient) {
        requests  = new int[maxClient];
        responses = new int[maxClient];
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "receiveRequests",
                source = "IDLE",
                target = "CALC")
    public void receiveClientRequest(@Data(name = "requests") int[] requests) {
        this.requests = requests;
        this.responses = new int[requests.length];
        currentRequestId = nextRequest();
//        System.out.printf("[Server] IDLE -> CALC - received : %s%n", java.util.Arrays.toString(requests));
    }

    @Transition(name   = "sendToProvider",
                source = "CALC",
                target = "SENT",
                guard  = "!allRequestsTreated")
    public void sendToProvider() {
//        System.out.printf("[Server] CALC -> SENT - sending C%d request with %d images%n", currentRequestId, requests[currentRequestId]);
    }

    @Transition(name   = "receiveResponse",
                source = "SENT",
                target = "CALC")
    public void receiveResponse(@Data(name = "response") int response) {
        this.responses[currentRequestId] = response;
//        System.out.printf("[Server] SENT -> CALC - received response for C%d : %b%n", currentRequestId, done);
        currentRequestId = nextRequest();
    }

    @Transition(name   = "sendToClients",
                source = "CALC",
                target = "IDLE",
                guard  = "allRequestsTreated")
    public void sendToClients() {
//        System.out.printf("[Server] CALC -> IDLE %n");
    }

    // ==============
    // === GUARDS ===
    // ==============

    @Guard(name = "allRequestsTreated")
    public boolean allRequestsTreated() {
        // true when the last client request index has been reached
        return requests != null && requests.length > 0 && currentRequestId == 0;
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "request", accessTypePort = DataOut.AccessType.any)
    public int getRequest() {
        return requests[currentRequestId];
    }

    @Data(name = "getResponses", accessTypePort = DataOut.AccessType.any)
    public int[] getResponses() {
        return responses;
    }

    // ===============
    // === HELPERS ===
    // ===============

    private int nextRequest() {
        return (currentRequestId + 1) % requests.length;
    }
}
