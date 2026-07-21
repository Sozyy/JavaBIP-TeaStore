package tools.spirals.cerberus237.siphonix.strategies.javabip.component;

import org.javabip.annotations.*;
import org.javabip.api.DataOut;
import org.javabip.api.PortType;

/**
 * PID Controller that receives the DataProvider responseTime and returns a new cache size
 * comparing the responseTime and targetResponseTime.
 *
 * Reasons in item counts, not bytes: the shadow cache tracks how many images it holds, not
 * their size. CacheManagementStrategy converts the returned item count into a byte target
 * for AdaptableTeaStore's real cache using the average measured image byte size.
 */
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

    // Anti-windup bound on |integral|: without it, a sustained one-signed error (e.g. a
    // mis-scaled target vs. real measurements) makes the integral term grow unbounded,
    // producing an increasingly aggressive control signal cycle after cycle and a slow,
    // "stuck" recovery once the error eventually flips sign. Clamped so ki*integral alone
    // can never exceed the full output span (maxCacheSize - minCacheSize).
    private final float integralClampBound;

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

        this.integral      = 0.0F;
        this.derivative    = 0.0F;
        this.previousError = 0.0F;

        this.integralClampBound = (ki != 0.0F)
                ? Math.abs((maxCacheSize - minCacheSize) / ki)
                : Float.MAX_VALUE;
    }

    // === TRANSITIONS ===

    @Transition(name = "receiveResponseTime",
                source = "IDLE",
                target = "COMPUTING")
    public void receiveResponseTime(@Data(name = "responseTime") float responseTime) {
        currentCacheSize = computeNewCacheSize(responseTime);
    }

    @Transition(name = "sendCacheSize",
                source = "COMPUTING",
                target = "IDLE")
    public void sendCacheSize() {}

    // === DATA WIRES ===

    @Data(name = "newCacheSize", accessTypePort = DataOut.AccessType.any)
    public int newCacheSize() { return currentCacheSize; }

    // === HELPERS ===

    /**
     * PID Controller getting the DataProvider time and comparing it to the target time.
     *
     * @param responseTime The processingTime received from the DataProvider
     *
     * @return The new cache size (item instead of bytes).
     */
    public int computeNewCacheSize(float responseTime) {
        float error = responseTime - targetResponseTime;

        integral  += error;
        integral   = clampFloat(integral, -integralClampBound, integralClampBound);
        derivative = error - previousError;

        float control = kp * error
                      + ki * integral
                      + kd * derivative;

        int newCacheSize = clamp(currentCacheSize + (int) Math.round(control), minCacheSize, maxCacheSize);

        previousError = error;
        return newCacheSize;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
