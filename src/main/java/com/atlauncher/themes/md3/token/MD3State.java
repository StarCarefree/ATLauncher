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

import java.awt.Color;

/**
 * State layer opacities.
 *
 * <p>
 * Material 3 expresses interaction feedback by painting the component's <em>content</em> colour
 * over its container at a low opacity, rather than by swapping in a different container colour.
 * That is why a hovered filled button and a hovered outlined button feel like the same gesture even
 * though they look nothing alike.
 *
 * <p>
 * Disabled is the exception: it reduces the opacity of the component's own colours instead of
 * layering anything on top.
 */
public final class MD3State {
    public static final float HOVER = 0.08f;
    public static final float FOCUS = 0.10f;
    public static final float PRESSED = 0.10f;
    public static final float DRAGGED = 0.16f;
    public static final float SELECTED = 0.12f;

    /** Opacity applied to a disabled component's container. */
    public static final float DISABLED_CONTAINER = 0.12f;
    /** Opacity applied to a disabled component's text and icons. */
    public static final float DISABLED_CONTENT = 0.38f;

    /** Opacity of the scrim behind a modal dialog. */
    public static final float SCRIM = 0.32f;

    private MD3State() {
    }

    /**
     * The opacity for the strongest state currently active. Material 3 does not stack state layers -
     * pressed wins over hover, not pressed plus hover.
     */
    public static float opacityFor(boolean hovered, boolean focused, boolean pressed, boolean dragged) {
        if (dragged) {
            return DRAGGED;
        }

        if (pressed) {
            return PRESSED;
        }

        if (hovered) {
            return HOVER;
        }

        if (focused) {
            return FOCUS;
        }

        return 0f;
    }

    /**
     * Flattens a state layer into an opaque colour.
     *
     * <p>
     * Use where a real colour is needed - a border, or a component that has to stay opaque. Where
     * the layer can simply be painted on top, prefer doing that so overlapping content stays
     * visible.
     */
    public static Color applyTo(Color container, Color content, float opacity) {
        if (opacity <= 0f) {
            return container;
        }

        return MD3Color.blend(container, content, opacity);
    }

    /**
     * A content colour dimmed to the disabled opacity, composited onto the surface it sits on so
     * the result stays opaque.
     */
    public static Color disabledContent(Color content, Color surface) {
        return MD3Color.blend(surface, content, DISABLED_CONTENT);
    }

    /**
     * A container colour at the disabled opacity, composited onto the surface it sits on.
     */
    public static Color disabledContainer(Color container, Color surface) {
        return MD3Color.blend(surface, container, DISABLED_CONTAINER);
    }
}
