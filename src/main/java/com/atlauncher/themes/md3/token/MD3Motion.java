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
package com.atlauncher.themes.md3.token;

import javax.swing.UIManager;

import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;

/**
 * The Material 3 easing curves and duration tokens.
 *
 * <p>
 * Emphasised curves are the expressive ones - they overshoot slightly at the start and settle
 * gently, and belong on anything the user initiated and is watching, like a navigation transition.
 * Standard curves are for everything else: state layer fades, small property changes, anything that
 * should read as responsive rather than as an animation.
 *
 * <p>
 * Durations are the spec's. Pick by distance travelled, not by importance - a state layer crossing
 * a few pixels wants {@link #SHORT2}, a panel sliding the width of the window wants
 * {@link #LONG2}.
 */
public final class MD3Motion {
    /** Set to true in UIDefaults to collapse every animation to an instant change. */
    public static final String REDUCED_MOTION_KEY = "md.sys.motion.reduced";

    public static final Animator.Interpolator EMPHASIZED = new CubicBezierEasing(0.2f, 0f, 0f, 1f);
    public static final Animator.Interpolator EMPHASIZED_DECELERATE = new CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f);
    public static final Animator.Interpolator EMPHASIZED_ACCELERATE = new CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f);

    public static final Animator.Interpolator STANDARD = new CubicBezierEasing(0.2f, 0f, 0f, 1f);
    public static final Animator.Interpolator STANDARD_DECELERATE = new CubicBezierEasing(0f, 0f, 0f, 1f);
    public static final Animator.Interpolator STANDARD_ACCELERATE = new CubicBezierEasing(0.3f, 0f, 1f, 1f);

    public static final Animator.Interpolator LINEAR = new Animator.Interpolator() {
        @Override
        public float interpolate(float fraction) {
            return fraction;
        }
    };

    /**
     * Settles a little past its target and comes back - the spring Material 3 Expressive uses for
     * something arriving where the user asked it to go.
     *
     * <p>
     * A cubic bezier cannot overshoot the way a spring does, so this is the "back out" curve instead,
     * softened to about four percent. Enough that a navigation indicator reads as having travelled
     * and stopped rather than having been redrawn; not so much that it looks loose. Only for movement
     * - an overshooting <em>colour</em> or opacity clips at the end of its range and simply stalls.
     */
    public static final Animator.Interpolator EMPHASIZED_OVERSHOOT = new Animator.Interpolator() {
        @Override
        public float interpolate(float fraction) {
            float tension = 1.02f;
            float past = fraction - 1f;

            return 1f + (tension + 1f) * past * past * past + tension * past * past;
        }
    };

    public static final int SHORT1 = 50;
    public static final int SHORT2 = 100;
    public static final int SHORT3 = 150;
    public static final int SHORT4 = 200;
    public static final int MEDIUM1 = 250;
    public static final int MEDIUM2 = 300;
    public static final int MEDIUM3 = 350;
    public static final int MEDIUM4 = 400;
    public static final int LONG1 = 450;
    public static final int LONG2 = 500;
    public static final int LONG3 = 550;
    public static final int LONG4 = 600;

    /** State layer hover and press fades. */
    public static final int STATE_LAYER = SHORT3;
    /** Switching the selected destination. */
    public static final int NAVIGATION = MEDIUM2;
    /** Dialogs and menus opening. */
    public static final int CONTAINER_ENTER = MEDIUM1;
    /** Dialogs and menus closing - exits are faster than entrances. */
    public static final int CONTAINER_EXIT = SHORT4;
    /**
     * A control changing shape under the pointer. The shortest token there is: a press has to look
     * like the button reacting to the finger, and anything slower reads as lag.
     */
    public static final int SHAPE_MORPH = SHORT1;
    /** A component lifting toward the pointer, or settling back. */
    public static final int ELEVATION = SHORT4;
    /** One whole page replacing another. */
    public static final int PAGE_TRANSITION = MEDIUM2;

    /**
     * How long a snackbar stays before dismissing itself, and how long one carrying an action stays.
     *
     * <p>
     * Longer than any of the duration tokens above by an order of magnitude, because it is not a
     * transition - it is how long a message the user did not ask for is allowed to sit on their
     * window. An action gets more, since reading it and deciding to take it is two things rather
     * than one.
     */
    public static final int SNACKBAR_DWELL = 4000;
    public static final int SNACKBAR_DWELL_WITH_ACTION = 6000;

    private MD3Motion() {
    }

    public static boolean isReduced() {
        return UIManager.getBoolean(REDUCED_MOTION_KEY);
    }

    /**
     * Turns motion off, or back on.
     *
     * <p>
     * Every animation in the launcher already asked {@link #isReduced()}, but nothing outside the
     * tests ever answered yes - the switch existed and was unreachable. Java has no portable way to
     * read the platform's own "reduce motion" preference, so this is driven by a launcher setting
     * instead, and applies without a restart: each animation reads the flag when it starts.
     */
    public static void setReduced(boolean reduced) {
        UIManager.put(REDUCED_MOTION_KEY, reduced);
    }

    /**
     * Builds an animator wired to a curve and duration, or one that completes in a single frame if
     * motion has been reduced.
     */
    public static Animator animator(int durationMs, Animator.Interpolator interpolator, Animator.TimingTarget target) {
        Animator animator = new Animator(isReduced() ? 1 : Math.max(1, durationMs), target);
        animator.setInterpolator(isReduced() ? LINEAR : interpolator);

        return animator;
    }

    /**
     * @return the duration to actually use, honouring reduced motion
     */
    public static int duration(int durationMs) {
        return isReduced() ? 1 : durationMs;
    }
}
