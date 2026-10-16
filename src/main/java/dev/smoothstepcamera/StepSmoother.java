package dev.smoothstepcamera;

/** Tick-driven, critically damped correction of short changes in foot height. */
public final class StepSmoother {
    public static final double MAX_STEP_HEIGHT = 0.6;
    private static final double EPSILON = 1.0e-5;
    private static final double TICK_SECONDS = 0.05;

    private boolean initialized;
    private boolean descending;
    private double descentStartY;
    private double lastY;
    private double error;
    private double velocity;
    private double tickError;
    private double tickVelocity;
    private double compensatedDelta;
    private double omega;

    public void reset(double y) {
        initialized = true;
        lastY = y;
        descending = false;
        error = velocity = tickError = tickVelocity = compensatedDelta = 0.0;
    }

    public boolean isDescending() {
        return descending;
    }

    public boolean matchesPosition(double y) {
        return initialized && Math.abs(lastY - y) <= EPSILON;
    }

    /**
     * floorGap is the collision-tested space below the current feet. A small
     * descent is smoothed only while its total drop, including that gap, fits
     * within a normal step. Jump arcs and long falls do not start smoothing.
     */
    public void tick(double fromY, double toY, boolean groundedBefore, boolean groundedAfter,
                     boolean canSmoothSteps, double floorGap, double durationSeconds) {
        if (!Double.isFinite(fromY) || !Double.isFinite(toY)
                || !Double.isFinite(durationSeconds) || durationSeconds <= 0.0) {
            reset(toY);
            return;
        }
        if (!initialized) {
            reset(fromY);
        } else if (!matchesPosition(fromY) || Math.abs(toY - fromY) > MAX_STEP_HEIGHT + EPSILON) {
            // Position corrections and teleports must never become camera travel.
            reset(toY);
            return;
        }

        double delta = toY - fromY;
        boolean step = false;
        if (canSmoothSteps) {
            if (groundedBefore && groundedAfter && delta > EPSILON) {
                step = delta <= MAX_STEP_HEIGHT + EPSILON;
                descending = false;
            } else if (delta <= EPSILON && (groundedBefore || descending)) {
                if (groundedBefore) {
                    descentStartY = fromY;
                }
                double totalDrop = descentStartY - toY;
                step = Double.isFinite(floorGap) && floorGap >= 0.0
                        && totalDrop >= -EPSILON
                        && totalDrop + floorGap <= MAX_STEP_HEIGHT + EPSILON;
                descending = step && !groundedAfter;
            } else {
                descending = false;
            }
        } else {
            descending = false;
        }

        compensatedDelta = step ? delta : 0.0;
        tickError = error - compensatedDelta;
        tickVelocity = velocity;
        // At durationSeconds a single step has settled by approximately 98%.
        omega = 6.0 / durationSeconds;
        double coefficient = tickVelocity + omega * tickError;
        double decay = Math.exp(-omega * TICK_SECONDS);
        error = (tickError + coefficient * TICK_SECONDS) * decay;
        velocity = (tickVelocity - omega * coefficient * TICK_SECONDS) * decay;
        lastY = toY;
    }

    /** Pure sampling: rendering more frames cannot advance the spring. */
    public double offset(float partialTick) {
        if (!initialized || !Float.isFinite(partialTick)) {
            return 0.0;
        }
        double fraction = Math.clamp((double) partialTick, 0.0, 1.0);
        double seconds = fraction * TICK_SECONDS;
        double springError = (tickError + (tickVelocity + omega * tickError) * seconds)
                * Math.exp(-omega * seconds);
        // Cancel only the new step's vanilla linear interpolation; keep all
        // other movement, eye-height changes, and rotation in vanilla's hands.
        return springError + (1.0 - fraction) * compensatedDelta;
    }
}
