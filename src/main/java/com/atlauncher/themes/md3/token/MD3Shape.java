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

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import com.formdev.flatlaf.util.UIScale;

/**
 * The Material 3 shape scale, plus helpers for building the rounded outlines components paint.
 *
 * <p>
 * Corner radii are unscaled - run them through {@link #scaled(int)} or use the shape builders,
 * which scale for you.
 */
public final class MD3Shape {
    public static final int NONE = 0;
    public static final int EXTRA_SMALL = 4;
    public static final int SMALL = 8;
    public static final int MEDIUM = 12;
    public static final int LARGE = 16;
    public static final int EXTRA_LARGE = 28;

    /** Marker for a fully rounded (stadium) shape; resolved against the component's height. */
    public static final int FULL = -1;

    /** Per component-class defaults, so the same component is shaped alike everywhere. */
    public static final int BUTTON = FULL;
    public static final int ICON_BUTTON = FULL;
    public static final int CHIP = SMALL;
    public static final int CARD = MEDIUM;
    public static final int DIALOG = EXTRA_LARGE;
    public static final int MENU = EXTRA_SMALL;
    public static final int TEXT_FIELD = EXTRA_SMALL;
    public static final int FAB = LARGE;
    public static final int SNACKBAR = EXTRA_SMALL;
    public static final int NAV_INDICATOR = FULL;
    public static final int PROGRESS = EXTRA_SMALL;
    public static final int TOOLTIP = EXTRA_SMALL;

    private MD3Shape() {
    }

    public static int scaled(int radius) {
        return radius == FULL ? FULL : UIScale.scale(radius);
    }

    /**
     * Resolves a radius against a component's height, turning {@link #FULL} into half the height
     * and clamping anything else so it can never exceed what the box can actually round.
     */
    public static float resolve(int radius, float width, float height) {
        float limit = Math.min(width, height) / 2f;

        if (radius == FULL) {
            return limit;
        }

        return Math.min(UIScale.scale((float) radius), limit);
    }

    public static Shape rounded(float x, float y, float width, float height, int radius) {
        float r = resolve(radius, width, height);

        return new RoundRectangle2D.Float(x, y, width, height, r * 2f, r * 2f);
    }

    public static Shape rounded(float width, float height, int radius) {
        return rounded(0f, 0f, width, height, radius);
    }

    /**
     * A box with independently rounded corners, for shapes the scale does not cover - a filled text
     * field rounds its top two corners only, a segmented button rounds the outer corners of its end
     * segments.
     *
     * @param topLeft     unscaled radius, or {@link #FULL}
     * @param topRight    unscaled radius, or {@link #FULL}
     * @param bottomRight unscaled radius, or {@link #FULL}
     * @param bottomLeft  unscaled radius, or {@link #FULL}
     */
    public static Shape rounded(float x, float y, float width, float height, int topLeft, int topRight,
            int bottomRight, int bottomLeft) {
        float tl = resolve(topLeft, width, height);
        float tr = resolve(topRight, width, height);
        float br = resolve(bottomRight, width, height);
        float bl = resolve(bottomLeft, width, height);

        Path2D.Float path = new Path2D.Float();
        float right = x + width;
        float bottom = y + height;

        path.moveTo(x + tl, y);
        path.lineTo(right - tr, y);

        if (tr > 0) {
            path.quadTo(right, y, right, y + tr);
        }

        path.lineTo(right, bottom - br);

        if (br > 0) {
            path.quadTo(right, bottom, right - br, bottom);
        }

        path.lineTo(x + bl, bottom);

        if (bl > 0) {
            path.quadTo(x, bottom, x, bottom - bl);
        }

        path.lineTo(x, y + tl);

        if (tl > 0) {
            path.quadTo(x, y, x + tl, y);
        }

        path.closePath();

        return path;
    }
}
