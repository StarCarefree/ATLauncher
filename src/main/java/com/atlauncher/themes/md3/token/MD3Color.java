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

/**
 * The Material 3 colour roles, and how to read them back out of {@link UIManager}.
 *
 * <p>
 * Every theme - including the sixteen that predate the Material work - publishes the full set of
 * roles, so MD3 components can be written against roles alone and never need to know which theme is
 * active. See {@link MD3Scheme} for where the values come from.
 *
 * <p>
 * Use the named accessors rather than {@link #get(String)} where one exists; they read better at
 * the call site and survive a rename of the underlying key.
 */
public final class MD3Color {
    public static final String PREFIX = "md.sys.color.";

    public static final String PRIMARY = PREFIX + "primary";
    public static final String ON_PRIMARY = PREFIX + "onPrimary";
    public static final String PRIMARY_CONTAINER = PREFIX + "primaryContainer";
    public static final String ON_PRIMARY_CONTAINER = PREFIX + "onPrimaryContainer";
    public static final String INVERSE_PRIMARY = PREFIX + "inversePrimary";

    public static final String SECONDARY = PREFIX + "secondary";
    public static final String ON_SECONDARY = PREFIX + "onSecondary";
    public static final String SECONDARY_CONTAINER = PREFIX + "secondaryContainer";
    public static final String ON_SECONDARY_CONTAINER = PREFIX + "onSecondaryContainer";

    public static final String TERTIARY = PREFIX + "tertiary";
    public static final String ON_TERTIARY = PREFIX + "onTertiary";
    public static final String TERTIARY_CONTAINER = PREFIX + "tertiaryContainer";
    public static final String ON_TERTIARY_CONTAINER = PREFIX + "onTertiaryContainer";

    public static final String ERROR = PREFIX + "error";
    public static final String ON_ERROR = PREFIX + "onError";
    public static final String ERROR_CONTAINER = PREFIX + "errorContainer";
    public static final String ON_ERROR_CONTAINER = PREFIX + "onErrorContainer";

    public static final String BACKGROUND = PREFIX + "background";
    public static final String ON_BACKGROUND = PREFIX + "onBackground";

    public static final String SURFACE = PREFIX + "surface";
    public static final String ON_SURFACE = PREFIX + "onSurface";
    public static final String SURFACE_VARIANT = PREFIX + "surfaceVariant";
    public static final String ON_SURFACE_VARIANT = PREFIX + "onSurfaceVariant";
    public static final String SURFACE_DIM = PREFIX + "surfaceDim";
    public static final String SURFACE_BRIGHT = PREFIX + "surfaceBright";

    public static final String SURFACE_CONTAINER_LOWEST = PREFIX + "surfaceContainerLowest";
    public static final String SURFACE_CONTAINER_LOW = PREFIX + "surfaceContainerLow";
    public static final String SURFACE_CONTAINER = PREFIX + "surfaceContainer";
    public static final String SURFACE_CONTAINER_HIGH = PREFIX + "surfaceContainerHigh";
    public static final String SURFACE_CONTAINER_HIGHEST = PREFIX + "surfaceContainerHighest";

    public static final String INVERSE_SURFACE = PREFIX + "inverseSurface";
    public static final String INVERSE_ON_SURFACE = PREFIX + "inverseOnSurface";

    public static final String OUTLINE = PREFIX + "outline";
    public static final String OUTLINE_VARIANT = PREFIX + "outlineVariant";

    public static final String SHADOW = PREFIX + "shadow";
    public static final String SCRIM = PREFIX + "scrim";
    public static final String SURFACE_TINT = PREFIX + "surfaceTint";

    /**
     * Fixed roles keep the same value in light and dark, for elements that must stay recognisable
     * when placed on either.
     */
    public static final String PRIMARY_FIXED = PREFIX + "primaryFixed";
    public static final String PRIMARY_FIXED_DIM = PREFIX + "primaryFixedDim";
    public static final String ON_PRIMARY_FIXED = PREFIX + "onPrimaryFixed";
    public static final String ON_PRIMARY_FIXED_VARIANT = PREFIX + "onPrimaryFixedVariant";
    public static final String SECONDARY_FIXED = PREFIX + "secondaryFixed";
    public static final String SECONDARY_FIXED_DIM = PREFIX + "secondaryFixedDim";
    public static final String ON_SECONDARY_FIXED = PREFIX + "onSecondaryFixed";
    public static final String ON_SECONDARY_FIXED_VARIANT = PREFIX + "onSecondaryFixedVariant";
    public static final String TERTIARY_FIXED = PREFIX + "tertiaryFixed";
    public static final String TERTIARY_FIXED_DIM = PREFIX + "tertiaryFixedDim";
    public static final String ON_TERTIARY_FIXED = PREFIX + "onTertiaryFixed";
    public static final String ON_TERTIARY_FIXED_VARIANT = PREFIX + "onTertiaryFixedVariant";

    /** Every role, in the order {@link MD3Scheme} writes them. Useful for diagnostics. */
    public static final String[] ALL_ROLES = {
            PRIMARY, ON_PRIMARY, PRIMARY_CONTAINER, ON_PRIMARY_CONTAINER, INVERSE_PRIMARY,
            SECONDARY, ON_SECONDARY, SECONDARY_CONTAINER, ON_SECONDARY_CONTAINER,
            TERTIARY, ON_TERTIARY, TERTIARY_CONTAINER, ON_TERTIARY_CONTAINER,
            ERROR, ON_ERROR, ERROR_CONTAINER, ON_ERROR_CONTAINER,
            BACKGROUND, ON_BACKGROUND,
            SURFACE, ON_SURFACE, SURFACE_VARIANT, ON_SURFACE_VARIANT, SURFACE_DIM, SURFACE_BRIGHT,
            SURFACE_CONTAINER_LOWEST, SURFACE_CONTAINER_LOW, SURFACE_CONTAINER, SURFACE_CONTAINER_HIGH,
            SURFACE_CONTAINER_HIGHEST,
            INVERSE_SURFACE, INVERSE_ON_SURFACE,
            OUTLINE, OUTLINE_VARIANT, SHADOW, SCRIM, SURFACE_TINT,
            PRIMARY_FIXED, PRIMARY_FIXED_DIM, ON_PRIMARY_FIXED, ON_PRIMARY_FIXED_VARIANT,
            SECONDARY_FIXED, SECONDARY_FIXED_DIM, ON_SECONDARY_FIXED, ON_SECONDARY_FIXED_VARIANT,
            TERTIARY_FIXED, TERTIARY_FIXED_DIM, ON_TERTIARY_FIXED, ON_TERTIARY_FIXED_VARIANT };

    private static final Color FALLBACK = Color.GRAY;

    private MD3Color() {
    }

    /**
     * @return the colour for a role, never null - falls back to grey if a theme somehow failed to
     *         publish the role, so a missing token degrades to something visible rather than an NPE
     *         deep inside a paint method
     */
    public static Color get(String role) {
        Color color = UIManager.getColor(role);

        return color != null ? color : FALLBACK;
    }

    /**
     * A role at a given alpha. Used for state layers, scrims and disabled content.
     */
    public static Color get(String role, float alpha) {
        return withAlpha(get(role), alpha);
    }

    public static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f));
    }

    /**
     * Composites {@code overlay} onto the opaque {@code base} at {@code alpha}, returning an opaque
     * colour.
     *
     * <p>
     * Preferred over painting a translucent layer where the result needs to be a real colour - for
     * example a border that must not let the component behind it show through.
     */
    public static Color blend(Color base, Color overlay, float alpha) {
        alpha = Math.max(0f, Math.min(1f, alpha));

        return new Color(
                Math.round(base.getRed() + (overlay.getRed() - base.getRed()) * alpha),
                Math.round(base.getGreen() + (overlay.getGreen() - base.getGreen()) * alpha),
                Math.round(base.getBlue() + (overlay.getBlue() - base.getBlue()) * alpha));
    }

    public static Color primary() {
        return get(PRIMARY);
    }

    public static Color onPrimary() {
        return get(ON_PRIMARY);
    }

    public static Color primaryContainer() {
        return get(PRIMARY_CONTAINER);
    }

    public static Color onPrimaryContainer() {
        return get(ON_PRIMARY_CONTAINER);
    }

    public static Color secondary() {
        return get(SECONDARY);
    }

    public static Color secondaryContainer() {
        return get(SECONDARY_CONTAINER);
    }

    public static Color onSecondaryContainer() {
        return get(ON_SECONDARY_CONTAINER);
    }

    public static Color tertiary() {
        return get(TERTIARY);
    }

    public static Color tertiaryContainer() {
        return get(TERTIARY_CONTAINER);
    }

    public static Color onTertiaryContainer() {
        return get(ON_TERTIARY_CONTAINER);
    }

    public static Color error() {
        return get(ERROR);
    }

    public static Color onError() {
        return get(ON_ERROR);
    }

    public static Color errorContainer() {
        return get(ERROR_CONTAINER);
    }

    public static Color onErrorContainer() {
        return get(ON_ERROR_CONTAINER);
    }

    public static Color surface() {
        return get(SURFACE);
    }

    public static Color onSurface() {
        return get(ON_SURFACE);
    }

    public static Color surfaceVariant() {
        return get(SURFACE_VARIANT);
    }

    public static Color onSurfaceVariant() {
        return get(ON_SURFACE_VARIANT);
    }

    public static Color surfaceContainerLowest() {
        return get(SURFACE_CONTAINER_LOWEST);
    }

    public static Color surfaceContainerLow() {
        return get(SURFACE_CONTAINER_LOW);
    }

    public static Color surfaceContainer() {
        return get(SURFACE_CONTAINER);
    }

    public static Color surfaceContainerHigh() {
        return get(SURFACE_CONTAINER_HIGH);
    }

    public static Color surfaceContainerHighest() {
        return get(SURFACE_CONTAINER_HIGHEST);
    }

    public static Color inverseSurface() {
        return get(INVERSE_SURFACE);
    }

    public static Color inverseOnSurface() {
        return get(INVERSE_ON_SURFACE);
    }

    public static Color outline() {
        return get(OUTLINE);
    }

    public static Color outlineVariant() {
        return get(OUTLINE_VARIANT);
    }

    public static Color scrim() {
        return get(SCRIM);
    }
}
