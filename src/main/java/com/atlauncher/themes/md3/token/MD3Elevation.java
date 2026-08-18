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

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

/**
 * The six elevation levels, expressed the way Material 3 actually expresses them.
 *
 * <p>
 * Height is carried primarily by <em>surface colour</em>, not by shadow: each level maps to a
 * surface container role that is a little lighter (dark theme) or a little darker (light theme)
 * than the one below. Shadows are a secondary cue and are deliberately weak - in a dark theme they
 * are nearly invisible against a dark background anyway, and painting soft shadows under every card
 * is the fastest way to make a Swing scroll pane stutter.
 *
 * <p>
 * So: always call {@link #surfaceRole(int)}. Only reach for the shadow values on components that
 * genuinely float over content - dialogs, menus, snackbars, a dragged card.
 */
public final class MD3Elevation {
    public static final int LEVEL0 = 0;
    public static final int LEVEL1 = 1;
    public static final int LEVEL2 = 2;
    public static final int LEVEL3 = 3;
    public static final int LEVEL4 = 4;
    public static final int LEVEL5 = 5;

    private static final String[] SURFACE_ROLES = {
            MD3Color.SURFACE,
            MD3Color.SURFACE_CONTAINER_LOW,
            MD3Color.SURFACE_CONTAINER,
            MD3Color.SURFACE_CONTAINER_HIGH,
            MD3Color.SURFACE_CONTAINER_HIGH,
            MD3Color.SURFACE_CONTAINER_HIGHEST };

    /** Unscaled vertical offset of the shadow, per level. */
    private static final int[] SHADOW_OFFSET_Y = { 0, 1, 2, 4, 6, 8 };
    /** Unscaled blur radius of the shadow, per level. */
    private static final int[] SHADOW_BLUR = { 0, 3, 6, 8, 10, 12 };
    /** Shadow opacity in a light theme, per level. */
    private static final float[] SHADOW_ALPHA_LIGHT = { 0f, 0.15f, 0.18f, 0.20f, 0.22f, 0.24f };
    /** Shadow opacity in a dark theme - lower, since a dark background swallows most of it. */
    private static final float[] SHADOW_ALPHA_DARK = { 0f, 0.28f, 0.32f, 0.36f, 0.40f, 0.44f };

    private MD3Elevation() {
    }

    private static int clamp(int level) {
        return Math.max(LEVEL0, Math.min(LEVEL5, level));
    }

    /**
     * @return the {@link MD3Color} role a component at this level should fill itself with
     */
    public static String surfaceRole(int level) {
        return SURFACE_ROLES[clamp(level)];
    }

    public static Color surface(int level) {
        return MD3Color.get(surfaceRole(level));
    }

    public static int shadowOffsetY(int level) {
        return SHADOW_OFFSET_Y[clamp(level)];
    }

    public static int shadowBlur(int level) {
        return SHADOW_BLUR[clamp(level)];
    }

    public static float shadowAlpha(int level) {
        return isDark() ? SHADOW_ALPHA_DARK[clamp(level)] : SHADOW_ALPHA_LIGHT[clamp(level)];
    }

    /**
     * @return the shadow colour including its per-level alpha, or a fully transparent colour at
     *         level 0
     */
    public static Color shadowColor(int level) {
        return MD3Color.get(MD3Color.SHADOW, shadowAlpha(level));
    }

    /**
     * Whether the active theme is dark. Published by {@code MD3Scheme} when the theme is built.
     *
     * <p>
     * Falls back to asking the look and feel rather than to a guess. The two shadow ramps differ by
     * nearly a factor of two, so defaulting the wrong way puts visibly heavy shadows under every
     * light-theme dialog for the whole time the token is missing.
     */
    public static boolean isDark() {
        Object dark = UIManager.get("md.sys.dark");

        return dark instanceof Boolean ? (Boolean) dark : FlatLaf.isLafDark();
    }
}
