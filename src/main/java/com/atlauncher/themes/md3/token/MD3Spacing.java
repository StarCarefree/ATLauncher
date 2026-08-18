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

    /**
     * The smallest a thing the user has to hit with a pointer may be, per Material 3 and WCAG 2.5.5.
     *
     * <p>
     * A <em>target</em>, not a size: the control it belongs to is usually smaller and centred in it,
     * with the difference made up by padding that draws nothing. This used to be 40 and to double as
     * the height of a compact field and the width of a list's leading column, which meant correcting
     * it to what the spec asks would silently have resized three unrelated things. Those have
     * {@link #FIELD_HEIGHT_COMPACT} and {@link #LIST_LEADING_COLUMN} of their own now.
     */
    public static final int MIN_TOUCH_TARGET = 48;

    /**
     * Height of a field that shares a row with other controls - a search box, a dropdown, a spinner.
     *
     * <p>
     * Shorter than the 56dp of a standalone text field, because a toolbar of 56dp controls is a
     * toolbar that takes a sixth of a short window. The hit target is brought back up to
     * {@link #MIN_TOUCH_TARGET} by padding rather than by growing what is drawn.
     */
    public static final int FIELD_HEIGHT_COMPACT = 40;

    /**
     * Width of a list item's leading column - the icon, checkbox or avatar, plus the room around it.
     * Also what an inset divider is indented by, so the rule lines up with the text.
     */
    public static final int LIST_LEADING_COLUMN = 40;

    public static final int BUTTON_HEIGHT_SMALL = 32;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_HEIGHT_LARGE = 48;
    public static final int BUTTON_PADDING_H_SMALL = 16;
    public static final int BUTTON_PADDING_H = 24;
    /** Short labels like "Add" still need to look like a button, not a lozenge. */
    public static final int BUTTON_MIN_WIDTH = 48;
    public static final int BUTTON_ICON_SIZE_SMALL = 16;
    public static final int BUTTON_ICON_SIZE = 18;
    public static final int BUTTON_ICON_SIZE_LARGE = 20;
    /** Gap between segments of a connected button group. */
    public static final int BUTTON_GROUP_GAP = 2;
    public static final int ICON_BUTTON_SIZE_SMALL = 32;
    public static final int ICON_BUTTON_SIZE = 40;
    public static final int ICON_BUTTON_SIZE_LARGE = 48;
    public static final int ICON_SIZE = 20;
    public static final int ICON_SIZE_LARGE = 24;
    public static final int FAB_SIZE_SMALL = 40;
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

    public static final int CHECKBOX_BOX_SIZE = 18;
    public static final int SWITCH_TRACK_WIDTH = 52;
    public static final int SWITCH_TRACK_HEIGHT = 32;
    public static final int SWITCH_HANDLE_OFF = 16;
    public static final int SWITCH_HANDLE_ON = 24;
    /** Material grows the handle again while it is held down, past the size it settles at. */
    public static final int SWITCH_HANDLE_PRESSED = 28;

    /** A label-only tab; Material's height for one line of text. And with an icon above it. */
    public static final int TAB_HEIGHT = 48;
    public static final int TAB_HEIGHT_WITH_ICON = 64;
    public static final int TAB_INDICATOR_HEIGHT = 3;
    public static final int TAB_MIN_WIDTH = 72;

    /** Held to a line the eye can take in without tracking back; a dialog showing a document says so. */
    public static final int DIALOG_MIN_WIDTH = 280;
    public static final int DIALOG_MAX_WIDTH = 560;

    public static final int SNACKBAR_MAX_WIDTH = 560;
    public static final int SNACKBAR_MIN_HEIGHT = 48;

    public static final int PROGRESS_TRACK_HEIGHT = 4;

    /**
     * Material's standard density is 52dp. The create-pack table lists every Minecraft version there
     * has ever been, and the spec allows 36 through 64 for exactly that.
     */
    public static final int TABLE_ROW_HEIGHT = 36;
    public static final int TABLE_HEADER_HEIGHT = 44;

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
