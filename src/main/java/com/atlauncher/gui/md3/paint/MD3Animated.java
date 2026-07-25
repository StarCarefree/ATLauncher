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
package com.atlauncher.gui.md3.paint;

import java.awt.Color;

import javax.swing.JComponent;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.formdev.flatlaf.util.Animator;

/**
 * One number on its way somewhere, and the component that should repaint while it travels.
 *
 * <p>
 * Every animation in the launcher is this: a fraction between two values, driving a colour, a corner
 * radius, an offset or an opacity in a paint method. Each component that wanted one used to carry its
 * own {@code from}/{@code to}/{@code animator} triple and its own copy of the reduced-motion and
 * animations-disabled checks, which is four fields and a dozen lines before it can move anything.
 *
 * <p>
 * A value that is not going anywhere holds no timer, so a page of a hundred cards costs nothing until
 * the pointer reaches one of them.
 *
 * <p>
 * <b>A value on a component that was never realised does not animate.</b> Nothing would ever see the
 * frames - there is no window to paint them into - and it keeps the offscreen render tests
 * deterministic whatever the reduced-motion flag happens to be set to.
 */
public final class MD3Animated {
    /** Below this, a retarget is the value it already has and is ignored. */
    private static final float EPSILON = 0.001f;

    private final JComponent component;
    private final int duration;
    private final Animator.Interpolator interpolator;

    private float value;
    private float from;
    private float to;
    private Animator animator;

    /**
     * @param duration     one of the {@link MD3Motion} duration tokens
     * @param interpolator one of the {@link MD3Motion} curves
     */
    public MD3Animated(JComponent component, float initial, int duration, Animator.Interpolator interpolator) {
        this.component = component;
        this.duration = duration;
        this.interpolator = interpolator;
        this.value = initial;
        this.to = initial;
    }

    /**
     * @return where the value is right now, which is mid-flight while an animation is running
     */
    public float value() {
        return value;
    }

    /**
     * @return where it is heading, which is where it already is when nothing is running
     */
    public float target() {
        return to;
    }

    public boolean isAnimating() {
        return animator != null && animator.isRunning();
    }

    /**
     * Sends the value somewhere, animating if animation is possible and jumping if it is not.
     */
    public void setTarget(float target) {
        if (Math.abs(target - to) < EPSILON) {
            return;
        }

        stop();

        if (!animates()) {
            set(target);

            return;
        }

        from = value;
        to = target;

        animator = MD3Motion.animator(duration, interpolator, new Animator.TimingTarget() {
            @Override
            public void timingEvent(float fraction) {
                value = from + (to - from) * fraction;

                component.repaint();
            }

            @Override
            public void end() {
                // an overshoot curve is only exactly on target at fraction 1, and a stopped
                // animator never delivers that frame
                value = to;

                component.repaint();
            }
        });

        animator.start();
    }

    /**
     * Puts the value somewhere with no animation - for setting up a component, or for a change the
     * user did not cause and should not see travel.
     */
    public void set(float value) {
        stop();

        this.value = value;
        this.to = value;

        component.repaint();
    }

    public void stop() {
        if (animator != null) {
            animator.stop();
            animator = null;
        }
    }

    private boolean animates() {
        return Animator.useAnimation() && !MD3Motion.isReduced() && component.isDisplayable();
    }

    public static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    /**
     * The colour a value has reached between two others. Both must be opaque; the result is too.
     */
    public static Color lerp(Color from, Color to, float fraction) {
        if (from == null) {
            return to;
        }

        if (to == null) {
            return from;
        }

        return MD3Color.blend(from, to, fraction);
    }
}
