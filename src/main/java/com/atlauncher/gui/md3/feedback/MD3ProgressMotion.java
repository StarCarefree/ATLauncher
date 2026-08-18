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
package com.atlauncher.gui.md3.feedback;

import com.atlauncher.themes.md3.token.MD3Motion;

/**
 * Time-based poses for the Material 3 indeterminate indicators.
 *
 * <p>
 * {@link javax.swing.plaf.basic.BasicProgressBarUI} animates by counting frames, so the same bar
 * crawls on a busy machine and races on an idle one, and with reduced motion it freezes on the
 * first frame - a 20° speck that reads as a dash rather than as a wait. These poses are functions
 * of elapsed milliseconds, and reduced motion gets a still, fully readable shape instead of a
 * paused birth frame.
 */
final class MD3ProgressMotion {
    /** One pass of the two-segment linear animation. From Material Components. */
    static final int LINEAR_PERIOD_MS = 1800;

    /** Expand plus collapse of the circular sweep. */
    static final int CIRCULAR_CYCLE_MS = 1334;

    /** How long a full spin of the circular indicator takes. */
    static final int CIRCULAR_TURN_MS = 2000;

    static final float CIRCULAR_SWEEP_MIN = 20f;
    static final float CIRCULAR_SWEEP_MAX = 270f;

    /**
     * When to start moving each of the four ends, and how long they take to cross. Order is
     * segment 1 start, segment 1 end, segment 2 start, segment 2 end - Material's disjoint
     * sequence.
     */
    private static final int[] LINEAR_DELAY_MS = { 1267, 1000, 333, 0 };
    private static final int[] LINEAR_DURATION_MS = { 533, 567, 850, 750 };

    private MD3ProgressMotion() {
    }

    static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    static float ease(float fraction) {
        return MD3Motion.EMPHASIZED.interpolate(clamp01(fraction));
    }

    /**
     * Where one end of a linear segment sits, 0 at the start of the track and 1 at the finish.
     *
     * @param end 0..3 as documented on {@link #LINEAR_DELAY_MS}
     */
    static float linearEnd(int end, float cycle01) {
        float time = clamp01(cycle01) * LINEAR_PERIOD_MS;
        float into = time - LINEAR_DELAY_MS[end];

        if (into <= 0f) {
            return 0f;
        }

        if (into >= LINEAR_DURATION_MS[end]) {
            return 1f;
        }

        return ease(into / LINEAR_DURATION_MS[end]);
    }

    /**
     * Writes two [start, end] pairs into {@code out} as fractions along the track.
     */
    static void linearSegments(float cycle01, float[] out) {
        out[0] = linearEnd(0, cycle01);
        out[1] = linearEnd(1, cycle01);
        out[2] = linearEnd(2, cycle01);
        out[3] = linearEnd(3, cycle01);
    }

    /**
     * Writes start angle (degrees, Arc2D) and clockwise sweep (negative) for the circular
     * indicator.
     *
     * <p>
     * Reduced motion parks the indicator at a 270° arc from twelve o'clock - the shape of a wait,
     * not of a wait that has just been born.
     */
    static void circularArc(float elapsedMs, float[] outStartSweep) {
        if (MD3Motion.isReduced()) {
            outStartSweep[0] = 90f;
            outStartSweep[1] = -CIRCULAR_SWEEP_MAX;

            return;
        }

        float rotation = (elapsedMs / CIRCULAR_TURN_MS) * 360f;
        float cycle = elapsedMs / CIRCULAR_CYCLE_MS;
        int passed = (int) Math.floor(cycle);
        float local = cycle - passed;
        float start = 90f - rotation - passed * 90f;
        float sweep;

        if (local < 0.5f) {
            sweep = CIRCULAR_SWEEP_MIN
                    + (CIRCULAR_SWEEP_MAX - CIRCULAR_SWEEP_MIN) * ease(local * 2f);
        } else {
            sweep = CIRCULAR_SWEEP_MAX
                    - (CIRCULAR_SWEEP_MAX - CIRCULAR_SWEEP_MIN) * ease((local - 0.5f) * 2f);
            start -= CIRCULAR_SWEEP_MAX - sweep;
        }

        outStartSweep[0] = start;
        outStartSweep[1] = -sweep;
    }
}
