package TeaStoreWithConnector;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

import java.util.Random;

@Ports({
        @Port(name = "sendRequest", type = PortType.enforceable),
        @Port(name = "receiveResp", type = PortType.enforceable)
})
@ComponentType(name = "Client", initial = "IDLE")
public class Client {
    Random random = new Random();

    private final int id;
    private final int objective;
    private final int maxRequestSize;
    private final int[] requests;

    private int[] responses;
    private int totalImages = 0;
    private boolean isDone;

    public Client(int id, int objective, int maxRequestSize, int maxClient) {
        this.id = id;
        this.objective = objective;
        this.maxRequestSize = maxRequestSize;

        isDone = false;
        requests  = new int[maxClient];
        responses = new int[maxClient];
        drawNextRequest();
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name   = "sendRequest",
                source = "IDLE",
                target = "SENT")
    public void sendRequest() {
        float percent = (float) (totalImages * 100) / objective;
        if (id == 1) {
            System.out.printf("%d/%d -> %.2f%% %n", totalImages, objective, percent);
        }
        //System.out.printf("Client %d - (%d/%d : %.1f%%) - requests %d images%n", id, totalImages, objective, percent, requests[id]);
    }

    @Transition(name   = "receiveResp",
                source = "SENT",
                target = "IDLE")
    public void receiveResp(@Data(name = "responses") int[] responses) {
        this.responses = responses;
        if (!isDone) {
            totalImages += responses[id];
//            System.out.printf("Client %d - SENT -> IDLE (%d/%d)%n", id, totalImages, objective);
            if (objective <= totalImages) {
                isDone = true;
//                System.out.printf("Client %d has completed its objective!%n", id);
            }
        } else {
//            System.out.printf("Client %d - SENT -> IDLE (done, ignoring response)%n", id);
        }
        drawNextRequest();
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "requests", accessTypePort = DataOut.AccessType.any)
    public int[] getRequests() {
        return requests;
    }

    @Data(name = "originId", accessTypePort = DataOut.AccessType.any)
    public int getOriginId() {
        return id;
    }

    // ===============
    // === HELPERS ===
    // ===============

    public boolean isDone() { return isDone; }

    private void drawNextRequest() {
        if (isDone) {
            requests[id] = 0;
        } else {
            boolean enableRandomization = false;

            if (enableRandomization) {
                requests[id] = (int) (0.9 * maxRequestSize + random.nextInt((int) (0.1 * maxRequestSize)) * (random.nextBoolean() ? 1 : -1));
            } else {
                requests[id] = maxRequestSize;
            }
            // 90% of maxRequestSize + a random value between 1 and 10% of maxRequestSize, sign is randomize too
        }
    }
}
