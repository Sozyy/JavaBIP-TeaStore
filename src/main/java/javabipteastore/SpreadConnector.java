package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendToClient", type = PortType.enforceable),
        @Port(name = "sendToNext",   type = PortType.enforceable),
        @Port(name = "reset",        type = PortType.enforceable),
        @Port(name = "input",        type = PortType.enforceable)
})
@ComponentType(name = "SpreadConnector", initial = "IDLE")
public class SpreadConnector {
    private final int id;
    private final int maxClient;
    private final boolean hasNext;     // true, but false for last SP

    private int[] responses;
    private boolean inputReceived = false;
    private boolean sentToClient  = false;
    private boolean sentToNext    = false;

    public SpreadConnector(int id, int maxClient, boolean hasNext) {
        this.id = id;
        this.maxClient = maxClient;
        this.hasNext = hasNext;
        this.responses = new int[maxClient];
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "input",
                source = "IDLE",
                target = "IDLE",
                guard  = "!inputReceived")
    public void input(@Data(name = "responses") int[] responses) {
        this.responses = responses;
        this.inputReceived = true;
//        System.out.printf("[SpreadConnector %d] input received %s%n", id, java.util.Arrays.toString(responses));
    }

    @Transition(name   = "sendToClient",
                source = "IDLE",
                target = "SEND",
                guard  = "inputReceived & !sentToClient")
    public void sendToClient() {
        sentToClient = true;
//        System.out.printf("[SpreadConnector %d] IDLE -> SEND (sendToClient OK)%n", id);
    }

    @Transition(name   = "sendToClient",
                source = "SEND",
                target = "SEND",
                guard  = "!sentToClient")
    public void sendToClientFromSend() {
        sentToClient = true;
//        System.out.printf("[SpreadConnector %d] SEND -> SEND (sendToClient OK)%n", id);
    }

    @Transition(name   = "sendToNext",
                source = "IDLE",
                target = "SEND",
                guard  = "inputReceived & !sentToNext & hasNext")
    public void sendToNext() {
        sentToNext = true;
//        System.out.printf("[SpreadConnector %d] IDLE -> SEND (sendToNext OK)%n", id);
    }

    @Transition(name   = "sendToNext",
                source = "SEND",
                target = "SEND",
                guard  = "!sentToNext & hasNext")
    public void sendToNextFromSend() {
        sentToNext = true;
//        System.out.printf("[SpreadConnector %d] SEND -> SEND (sendToNext OK)%n", id);
    }

    @Transition(name   = "reset",
                source = "SEND",
                target = "IDLE",
                guard  = "allSent")
    public void reset() {
        responses = new int[maxClient];
        inputReceived = false;
        sentToClient = false;
        sentToNext = false;
//        System.out.printf("[SpreadConnector %d] SEND -> IDLE (reset)%n", id);
    }

    // ==============
    // === GUARDS ===
    // ==============

    @Guard(name = "inputReceived")
    public boolean inputReceived() { return inputReceived; }

    @Guard(name = "sentToClient")
    public boolean sentToClient() { return sentToClient; }

    @Guard(name = "sentToNext")
    public boolean sentToNext() { return sentToNext; }

    @Guard(name = "hasNext")
    public boolean hasNext() { return hasNext; }

    @Guard(name = "allSent")
    public boolean allSent() {
        return sentToClient && (sentToNext || !hasNext);
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "getResponses", accessTypePort = DataOut.AccessType.any)
    public int[] getResponses() { return responses; }
}