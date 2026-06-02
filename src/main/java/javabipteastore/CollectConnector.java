package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendUp", type = PortType.enforceable),
        @Port(name = "reset",  type = PortType.enforceable),
        @Port(name = "input1", type = PortType.enforceable),
        @Port(name = "input2", type = PortType.enforceable),
        @Port(name = "input3", type = PortType.enforceable)
})
@ComponentType(name = "Connector", initial = "IDLE")
public class CollectConnector {
    private final int id;
    private final int maxClient;

    private boolean rec1 = false;
    private boolean rec2 = false;
    private boolean rec3 = false;

    private int[] requests;
    private String whereWeAt = "";

    public CollectConnector(int id, int maxClient) {
        this.id = id;
        this.maxClient = maxClient;
        requests = new int[maxClient];
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "input1",
                source = "IDLE",
                target = "IDLE",
                guard  = "!rec1")
    public void input1(@Data(name = "previousRequests") int[] previousRequests) {
        rec1 = true;
        whereWeAt += "1";
        System.out.printf("[CollectConnector %d] : input1 - %s - receive from previous connector : %s%n", id, whereWeAt, java.util.Arrays.toString(previousRequests));
        mergeRequests(previousRequests);
    }

    // input2 is reserved for future potential contribution but has no use for the moment, as well as Looper2
    @Transition(name   = "input2",
                source = "IDLE",
                target = "IDLE",
                guard  = "!rec2")
    public void input2() {
        rec2 = true;
        whereWeAt += "2";
    }

    @Transition(name   = "input3",
                source = "IDLE",
                target = "IDLE",
                guard  = "!rec3")
    public void input3(@Data(name = "additionalRequests") int[] additionalRequests) {
        rec3 = true;
        whereWeAt += "3";
        System.out.printf("[CollectConnector %d] : input3 - %s - receive from client %d : %s%n", id, whereWeAt, id, java.util.Arrays.toString(additionalRequests));
        mergeRequests(additionalRequests);
    }

    @Transition(name   = "sendUp",
                source = "IDLE",
                target = "SEND",
                guard  = "rec1 & rec2 & rec3")
    public void sendUp() {
        whereWeAt += "Up";
        if (id == 1) {
            System.out.printf("[CollectConnector %d] : sendUp to server %s%n", id, java.util.Arrays.toString(requests));
        } else {
            System.out.printf("[CollectConnector %d] : sendUp %s%n", id, java.util.Arrays.toString(requests));
        }
    }

    @Transition(name   = "reset",
                source = "SEND",
                target = "IDLE")
    public void reset() {
        System.out.printf("[CollectConnector %d] : reset%n", id);
        requests = new int[maxClient];
        rec1 = false;
        rec2 = false;
        rec3 = false;
        whereWeAt = "";
    }

    // ==============
    // === GUARDS ===
    // ==============

    @Guard(name = "rec1")
    public boolean isRec1() { return rec1; }

    @Guard(name = "rec2")
    public boolean isRec2() { return rec2; }

    @Guard(name = "rec3")
    public boolean isRec3() { return rec3; }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "getRequests", accessTypePort = DataOut.AccessType.any)
    public int[] getRequests() { return requests; }

    // ===============
    // === HELPERS ===
    // ===============

    public void mergeRequests(int[] additionalRequests) {
        for (int i = 0; i < requests.length; i++) {
            requests[i] += additionalRequests[i];
        }
    }
}