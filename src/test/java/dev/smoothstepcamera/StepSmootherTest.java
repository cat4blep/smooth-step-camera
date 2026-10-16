package dev.smoothstepcamera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepSmootherTest {
    private static final double DURATION = 0.18;
    private static final double EPSILON = 1.0e-8;

    @Test
    void slabAscentStartsAtOldHeightAndMovesGradually() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);

        assertEquals(64.0, renderedY(smoother, 64.0, 64.5, 0.0f), EPSILON);
        double previous = 64.0;
        for (int frame = 1; frame <= 20; frame++) {
            double y = renderedY(smoother, 64.0, 64.5, frame / 20.0f);
            assertTrue(y >= previous && y < 64.5);
            assertTrue(y - previous < 0.02, "A step must not appear as one large camera jump");
            previous = y;
        }
        assertTrue(previous < 64.3);
    }

    @Test
    void ascentSettlesWithoutOvershooting() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        double previous = renderedY(smoother, 64.0, 64.5, 1.0f);
        for (int tick = 0; tick < 12; tick++) {
            smoother.tick(64.5, 64.5, true, true, true, 0.0, DURATION);
            double y = 64.5 + smoother.offset(1.0f);
            assertTrue(y >= previous && y <= 64.5);
            previous = y;
        }
        assertEquals(64.5, previous, 1.0e-6);
    }

    @Test
    void descendingHalfBlockIsSmoothThroughoutTheShortFall() {
        StepSmoother smoother = new StepSmoother();
        double[] feet = {64.5, 64.4216, 64.188, 64.0};
        double previousRender = feet[0];
        for (int tick = 1; tick < feet.length; tick++) {
            boolean landed = tick == feet.length - 1;
            smoother.tick(feet[tick - 1], feet[tick], tick == 1, landed,
                    true, feet[tick] - 64.0, DURATION);
            assertEquals(previousRender,
                    renderedY(smoother, feet[tick - 1], feet[tick], 0.0f), EPSILON);
            for (int frame = 1; frame <= 10; frame++) {
                double y = renderedY(smoother, feet[tick - 1], feet[tick], frame / 10.0f);
                assertTrue(y <= previousRender + EPSILON && y >= 64.0);
                assertTrue(previousRender - y < 0.05);
                previousRender = y;
            }
        }
        assertFalse(smoother.isDescending());
        assertTrue(previousRender > 64.0, "The camera should continue easing after the feet land");
    }

    @Test
    void zeroVerticalMovementOnTheEdgeDoesNotLoseTheDescent() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.5, 64.5, true, false, true, 0.5, DURATION);
        assertTrue(smoother.isDescending());
        smoother.tick(64.5, 64.4216, false, false, true, 0.4216, DURATION);
        assertTrue(smoother.offset(1.0f) > 0.0);
    }

    @Test
    void repeatedSlabToBlockStepsRemainContinuous() {
        StepSmoother smoother = new StepSmoother();
        double from = 64.0;
        double previousRender = from;
        for (int tick = 0; tick < 12; tick++) {
            double to = from + (tick % 2 == 0 ? 0.5 : 0.0);
            smoother.tick(from, to, true, true, true, 0.0, DURATION);
            assertEquals(previousRender, renderedY(smoother, from, to, 0.0f), EPSILON);
            double y = renderedY(smoother, from, to, 1.0f);
            assertTrue(y >= previousRender && y <= to);
            previousRender = y;
            from = to;
        }
    }

    @Test
    void ordinaryJumpAndLandingKeepVanillaInterpolation() {
        StepSmoother smoother = new StepSmoother();
        double[] arc = {64.0, 64.42, 64.75, 65.0, 64.8, 64.4, 64.0};
        for (int tick = 1; tick < arc.length; tick++) {
            smoother.tick(arc[tick - 1], arc[tick], tick == 1,
                    tick == arc.length - 1, tick > 3, Double.POSITIVE_INFINITY, DURATION);
            assertEquals(0.0, smoother.offset(0.5f), EPSILON);
        }
    }

    @Test
    void longFallIsNotTreatedAsAStepEvenWhenTheFirstDeltaIsSmall() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(70.0, 69.9216, true, false, true, 4.9216, DURATION);
        assertEquals(0.0, smoother.offset(0.5f), EPSILON);
        assertFalse(smoother.isDescending());
        smoother.tick(69.9216, 69.766, false, false, true, 4.766, DURATION);
        assertEquals(0.0, smoother.offset(1.0f), EPSILON);
    }

    @Test
    void descentStopsBeingCompensatedIfTheFloorDisappears() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.5, 64.4216, true, false, true, 0.4216, DURATION);
        double before = smoother.offset(1.0f);
        smoother.tick(64.4216, 64.188, false, false, true, 3.0, DURATION);
        assertFalse(smoother.isDescending());
        assertTrue(smoother.offset(1.0f) < before);
    }

    @Test
    void jumpingAfterAStepPreservesContinuityWhileTheOldCorrectionSettles() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        double before = smoother.offset(1.0f);
        smoother.tick(64.5, 64.92, true, false, false, 0.0, DURATION);
        assertEquals(before, smoother.offset(0.0f), EPSILON);
        assertTrue(Math.abs(smoother.offset(1.0f)) < Math.abs(before));
    }

    @Test
    void teleportBetweenTicksResetsTheCorrection() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        smoother.tick(100.0, 100.0, true, true, true, 0.0, DURATION);
        assertEquals(0.0, smoother.offset(0.5f), EPSILON);
        assertTrue(smoother.matchesPosition(100.0));
    }

    @Test
    void largePositionCorrectionDuringTickResetsTheCorrection() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        smoother.tick(64.5, 80.0, true, true, true, 0.0, DURATION);
        assertEquals(0.0, smoother.offset(1.0f), EPSILON);
    }

    @Test
    void respawnOrDimensionResetClearsAllInterpolationState() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        smoother.reset(-20.0);
        assertEquals(0.0, smoother.offset(0.0f), EPSILON);
        assertEquals(0.0, smoother.offset(1.0f), EPSILON);
        assertTrue(smoother.matchesPosition(-20.0));
    }

    @Test
    void renderFrequencyAndRepeatedRenderPassesDoNotChangeTheResult() {
        StepSmoother lowFps = new StepSmoother();
        StepSmoother highFps = new StepSmoother();
        lowFps.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        highFps.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        for (int frame = 0; frame < 1000; frame++) {
            highFps.offset(frame / 1000.0f);
        }
        assertEquals(lowFps.offset(0.75f), highFps.offset(0.75f), EPSILON);
        for (int tick = 0; tick < 5; tick++) {
            lowFps.tick(64.5, 64.5, true, true, true, 0.0, DURATION);
            highFps.tick(64.5, 64.5, true, true, true, 0.0, DURATION);
            highFps.offset(0.2f);
            highFps.offset(0.8f);
            assertEquals(lowFps.offset(0.75f), highFps.offset(0.75f), EPSILON);
        }
    }

    @Test
    void pausedGameSamplesRemainIdentical() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(64.0, 64.5, true, true, true, 0.0, DURATION);
        double paused = smoother.offset(0.37f);
        for (int render = 0; render < 100; render++) {
            assertEquals(paused, smoother.offset(0.37f), EPSILON);
        }
    }

    @Test
    void invalidInputsCannotCreateANonFiniteCameraOffset() {
        StepSmoother smoother = new StepSmoother();
        smoother.tick(0.0, Double.NaN, true, true, true, 0.0, DURATION);
        assertEquals(0.0, smoother.offset(0.5f));
        assertEquals(0.0, smoother.offset(Float.NaN));
        smoother.tick(0.0, 0.5, true, true, true, 0.0, 0.0);
        assertEquals(0.0, smoother.offset(1.0f));
    }

    private static double renderedY(StepSmoother smoother, double from, double to, float partialTick) {
        return from + (to - from) * partialTick + smoother.offset(partialTick);
    }
}
