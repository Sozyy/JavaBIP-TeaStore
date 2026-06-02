package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;

@Ports({
        @Port(name = "loop", type = org.javabip.api.PortType.enforceable)
})
@ComponentType(name = "Looper", initial = "IDLE")
public class Looper {
    private final int[] emptyRequests;

    public Looper(int maxClients) {
        this.emptyRequests = new int[maxClients];
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "loop",
                source = "IDLE",
                target = "IDLE")
    public void loop() throws InterruptedException {
        Thread.sleep(200);
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "getNothing", accessTypePort = DataOut.AccessType.any)
    public int[] getNothing() { return emptyRequests; }
}
