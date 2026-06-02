package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "receiveResponseTime", type = PortType.enforceable),
        @Port(name = "sendCacheSize",       type = PortType.enforceable)
})
@ComponentType(name = "PIDController", initial = "IDLE")
public class PIDController {
    private final int minCacheSize;
    private final int maxCacheSize;
    private final float targetResponseTime;
    private final float kp;
    private final float ki;
    private final float kd;

    private int currentCacheSize;

    private float integral;
    private float derivative;
    private float previousError;

    public PIDController(int initialCacheSize,
                         int minCacheSize,
                         int maxCacheSize,
                         float targetResponseTime,
                         float kp,
                         float ki,
                         float kd) {
        this.currentCacheSize = initialCacheSize;
        this.minCacheSize = minCacheSize;
        this.maxCacheSize = maxCacheSize;
        this.targetResponseTime = targetResponseTime;

        this.kp = kp;
        this.ki = ki;
        this.kd = kd;

        this.integral = 0.0F;
        this.derivative = 0.0F;
        this.previousError = 0.0F;
    }

    // ===================
    // === TRANSITIONS ===
    // ===================

    @Transition(name = "receiveResponseTime",
                source = "IDLE",
                target = "COMPUTING")
    public void receiveResponseTime(@Data(name = "responseTime") float responseTime) {
//        System.out.printf("[PIDController] IDLE -> COMPUTING - Received from DataProvider : response time = %.2f ms%n", responseTime);
        currentCacheSize = computeNewCacheSize(responseTime);
//        System.out.printf("[PIDController] Computed new cache size = %d%n", currentCacheSize);
    }

    @Transition(name = "sendCacheSize",
                source = "COMPUTING",
                target = "IDLE")
    public void sendCacheSize() {
//        System.out.printf("[PIDController] COMPUTING -> IDLE - Sending to DataProvider : new cache size = %d%n", currentCacheSize);
    }

    // ==================
    // === DATA WIRES ===
    // ==================

    @Data(name = "newCacheSize", accessTypePort = DataOut.AccessType.any)
    public int newCacheSize() { return currentCacheSize; }

    // ===============
    // === HELPERS ===
    // ===============

    public int computeNewCacheSize(float responseTime) {
        float error = responseTime - targetResponseTime;

        integral += error;
        derivative = error - previousError;

        float control = kp * error + ki * integral + kd * derivative;

        int newCacheSize = clamp(currentCacheSize + (int) Math.round(control), minCacheSize, maxCacheSize);

        // Log de debug
//        System.out.printf("[PID] error=%.2f integral=%.2f derivative=%.2f control=%.2f cache: %d -> %d%n", error,     integral,     derivative,     control,     currentCacheSize, newCacheSize);

        previousError = error;
        return newCacheSize;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}