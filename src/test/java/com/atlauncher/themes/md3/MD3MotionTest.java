/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2026 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.themes.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.formdev.flatlaf.util.Animator;

/**
 * The curves, and the one number every animation in the launcher is built out of.
 *
 * <p>
 * These are pure functions and a value object, so they can be checked exactly rather than by
 * painting - which matters most for the overshoot curve, whose whole job is to leave the range it is
 * interpolating over and come back. A constant nudged the wrong way there does not break anything
 * visibly; it just quietly stops overshooting, and the motion goes flat.
 */
public class MD3MotionTest {
    /** A component that claims to be on screen, so a value on it is allowed to animate. */
    private static final class RealisedPanel extends JPanel {
        @Override
        public boolean isDisplayable() {
            return true;
        }
    }

    @BeforeEach
    public void allowAnimation() {
        MD3Motion.setReduced(false);
    }

    /** The render tests share this JVM and all of them want motion off. */
    @AfterEach
    public void restoreReducedMotion() {
        MD3Motion.setReduced(true);
    }

    @Test
    public void testEveryCurveStartsAndFinishesWhereItIsAsked() {
        Animator.Interpolator[] curves = { MD3Motion.EMPHASIZED, MD3Motion.EMPHASIZED_ACCELERATE,
                MD3Motion.EMPHASIZED_DECELERATE, MD3Motion.EMPHASIZED_OVERSHOOT, MD3Motion.STANDARD,
                MD3Motion.STANDARD_ACCELERATE, MD3Motion.STANDARD_DECELERATE, MD3Motion.LINEAR };

        for (Animator.Interpolator curve : curves) {
            assertEquals(0f, curve.interpolate(0f), 0.001f,
                    "a curve that does not start at its origin makes whatever it drives jump on the first frame");
            assertEquals(1f, curve.interpolate(1f), 0.001f,
                    "a curve that does not finish at 1 leaves whatever it drives short of its target");
        }
    }

    /**
     * The point of the expressive curve. Enough to read as having arrived somewhere, and not so much
     * that a navigation indicator looks like it came loose.
     */
    @Test
    public void testTheExpressiveCurveOvershootsAndSettles() {
        float peak = 0f;

        for (int step = 0; step <= 100; step++) {
            peak = Math.max(peak, MD3Motion.EMPHASIZED_OVERSHOOT.interpolate(step / 100f));
        }

        assertTrue(peak > 1.02f, "the expressive curve never passes its target, so nothing springs - peak " + peak);
        assertTrue(peak < 1.1f, "the expressive curve overshoots far enough to look loose - peak " + peak);
    }

    /**
     * Nothing would ever see the frames, and it is what keeps the offscreen render tests painting the
     * same thing however the reduced-motion flag happens to be left.
     */
    @Test
    public void testAValueOnAComponentThatIsNotOnScreenGoesStraightThere() {
        MD3Animated value = new MD3Animated(new JPanel(), 0f, MD3Motion.MEDIUM2, MD3Motion.STANDARD);

        value.setTarget(1f);

        assertEquals(1f, value.value(), 0.001f, "a value on an unrealised component animated to nowhere");
        assertEquals(false, value.isAnimating(), "an unrealised component left a timer running");
    }

    @Test
    public void testReducedMotionSkipsStraightToTheTarget() {
        MD3Animated value = new MD3Animated(new RealisedPanel(), 0f, MD3Motion.MEDIUM2, MD3Motion.STANDARD);

        MD3Motion.setReduced(true);
        value.setTarget(1f);

        assertEquals(1f, value.value(), 0.001f, "reduced motion still animated");
        assertEquals(false, value.isAnimating(), "reduced motion left a timer running");
    }

    /**
     * A realised component does animate, which is the other half of the check above - otherwise a
     * test that everything jumps would pass just as well with the animation removed entirely.
     */
    @Test
    public void testAValueOnScreenSetsOffRatherThanArriving() {
        Assumptions.assumeTrue(Animator.useAnimation(), "animation is turned off for this JVM");

        MD3Animated value = new MD3Animated(new RealisedPanel(), 0f, MD3Motion.LONG4, MD3Motion.STANDARD);

        value.setTarget(1f);

        try {
            assertTrue(value.isAnimating(), "a value on a realised component did not animate");
            assertTrue(value.value() < 1f, "a value on a realised component was already at its target");
            assertEquals(1f, value.target(), 0.001f, "the value is not heading where it was sent");
        } finally {
            value.stop();
        }
    }

    @Test
    public void testSettingAValueOutrightCancelsWhateverItWasDoing() {
        Assumptions.assumeTrue(Animator.useAnimation(), "animation is turned off for this JVM");

        MD3Animated value = new MD3Animated(new RealisedPanel(), 0f, MD3Motion.LONG4, MD3Motion.STANDARD);

        value.setTarget(1f);
        value.set(0.25f);

        assertEquals(false, value.isAnimating(), "setting a value outright left the animation running behind it");
        assertEquals(0.25f, value.value(), 0.001f, "setting a value outright did not take");
    }

    /**
     * Material picks a duration by how far the thing travels. A press crosses a couple of pixels of
     * corner, a card lifts, a page crosses the window - so they get progressively longer, and getting
     * that backwards is what makes an interface feel sluggish in the small and abrupt in the large.
     */
    @Test
    public void testTheDurationTokensGetLongerAsTheDistanceDoes() {
        assertTrue(MD3Motion.SHAPE_MORPH < MD3Motion.ELEVATION,
                "a press takes longer to register than the lift it happens inside");
        assertTrue(MD3Motion.ELEVATION <= MD3Motion.PAGE_TRANSITION,
                "a control lifting takes longer than a whole page arriving");
    }
}
