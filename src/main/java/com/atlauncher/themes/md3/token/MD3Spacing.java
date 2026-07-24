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

import java.awt.Insets;

import javax.swing.border.Border;

import com.formdev.flatlaf.util.ScaledEmptyBorder;
import com.formdev.flatlaf.util.UIScale;

/**
 * The 4dp spacing grid, and the standard component sizes measured on it.
 *
 * <p>
 * Replaces {@link com.atlauncher.constants.UIConstants}'s 3/5/10 values, which were not on any
 * grid and left components a pixel or two out of alignment with each other.
 *
 * <p>
 * All values are unscaled. The {@code insets} and {@code border} helpers scale for you; raw
 * constants need {@link #scale(int)} before they reach a layout.
 */
public final class MD3Spacing {
    public static final int NONE = 0;
    public static final int XS = 4;
    public static final int S = 8;
    public static final int M = 12;
    public static final int L = 16;
    public static final int XL = 24;
    public static final int XXL = 32;

    /** Minimum size of anything the user has to hit with a pointer. */
    public static final int MIN_TOUCH_TARGET = 40;

    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_PADDING_H = 24;
    public static final int ICON_BUTTON_SIZE = 40;
    public static final int ICON_SIZE = 20;
    public static final int ICON_SIZE_LARGE = 24;
    public static final int FAB_SIZE = 56;
    public static final int CHIP_HEIGHT = 32;
    public static final int TEXT_FIELD_HEIGHT = 56;
    public static final int LIST_ITEM_HEIGHT_ONE_LINE = 56;
    public static final int LIST_ITEM_HEIGHT_TWO_LINE = 72;
    public static final int LIST_ITEM_HEIGHT_THREE_LINE = 88;
    public static final int TOP_APP_BAR_HEIGHT = 64;
    public static final int NAV_RAIL_WIDTH = 80;
    public static final int NAV_DRAWER_WIDTH = 240;
    public static final int NAV_ITEM_INDICATOR_HEIGHT = 32;
    public static final int DIVIDER_THICKNESS = 1;

    private MD3Spacing() {
    }

    public static int scale(int value) {
        return UIScale.scale(value);
    }

    public static Insets insets(int all) {
        return insets(all, all, all, all);
    }

    public static Insets insets(int vertical, int horizontal) {
        return insets(vertical, horizontal, vertical, horizontal);
    }

    public static Insets insets(int top, int left, int bottom, int right) {
        return new Insets(UIScale.scale(top), UIScale.scale(left), UIScale.scale(bottom), UIScale.scale(right));
    }

    /**
     * An empty border that re-scales itself if the display's scaling factor changes, so a window
     * dragged between monitors keeps its padding proportional.
     */
    public static Border border(int all) {
        return border(all, all, all, all);
    }

    public static Border border(int vertical, int horizontal) {
        return border(vertical, horizontal, vertical, horizontal);
    }

    public static Border border(int top, int left, int bottom, int right) {
        return new ScaledEmptyBorder(top, left, bottom, right);
    }
}
