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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.UIDefaults;

import com.atlauncher.themes.md3.hct.CorePalette;
import com.atlauncher.themes.md3.hct.TonalPalette;

/**
 * A complete set of Material 3 colour roles, generated from a single seed colour.
 *
 * <p>
 * Roles are pure tone lookups against the six palettes in {@link CorePalette}. Because tone is
 * perceptual lightness, the tone pairs the spec picks - 40/100 for light, 80/20 for dark and so on -
 * guarantee readable contrast for any seed, which is why nothing here is hand tuned per theme.
 *
 * <p>
 * Generating a scheme costs a couple of milliseconds, so results are cached by seed and brightness;
 * switching themes back and forth is free after the first visit.
 */
public final class MD3Scheme {
    private static final Map<Long, MD3Scheme> CACHE = new LinkedHashMap<Long, MD3Scheme>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, MD3Scheme> eldest) {
            return size() > 32;
        }
    };

    private final Map<String, Color> roles = new LinkedHashMap<>();
    private final int seedArgb;
    private final boolean dark;

    private MD3Scheme(int seedArgb, boolean dark, boolean content) {
        this.seedArgb = seedArgb;
        this.dark = dark;

        CorePalette core = content ? CorePalette.contentOf(seedArgb) : CorePalette.of(seedArgb);

        if (dark) {
            buildDark(core);
        } else {
            buildLight(core);
        }

        buildFixed(core);
    }

    /**
     * @param seedArgb the colour the whole theme is derived from
     * @param dark     whether to build the dark or light mapping
     */
    public static MD3Scheme from(int seedArgb, boolean dark) {
        return from(seedArgb, dark, false);
    }

    /**
     * @param content when true the seed's own chroma is preserved rather than being pushed up to
     *                the vibrant minimum. Use for themes derived from artwork.
     */
    public static synchronized MD3Scheme from(int seedArgb, boolean dark, boolean content) {
        long key = ((long) seedArgb << 2) | (dark ? 2L : 0L) | (content ? 1L : 0L);
        MD3Scheme cached = CACHE.get(key);

        if (cached == null) {
            cached = new MD3Scheme(seedArgb, dark, content);
            CACHE.put(key, cached);
        }

        return cached;
    }

    private void buildLight(CorePalette core) {
        TonalPalette a1 = core.a1;
        TonalPalette a2 = core.a2;
        TonalPalette a3 = core.a3;
        TonalPalette n1 = core.n1;
        TonalPalette n2 = core.n2;
        TonalPalette err = core.error;

        put(MD3Color.PRIMARY, a1.tone(40));
        put(MD3Color.ON_PRIMARY, a1.tone(100));
        put(MD3Color.PRIMARY_CONTAINER, a1.tone(90));
        put(MD3Color.ON_PRIMARY_CONTAINER, a1.tone(10));
        put(MD3Color.INVERSE_PRIMARY, a1.tone(80));

        put(MD3Color.SECONDARY, a2.tone(40));
        put(MD3Color.ON_SECONDARY, a2.tone(100));
        put(MD3Color.SECONDARY_CONTAINER, a2.tone(90));
        put(MD3Color.ON_SECONDARY_CONTAINER, a2.tone(10));

        put(MD3Color.TERTIARY, a3.tone(40));
        put(MD3Color.ON_TERTIARY, a3.tone(100));
        put(MD3Color.TERTIARY_CONTAINER, a3.tone(90));
        put(MD3Color.ON_TERTIARY_CONTAINER, a3.tone(10));

        put(MD3Color.ERROR, err.tone(40));
        put(MD3Color.ON_ERROR, err.tone(100));
        put(MD3Color.ERROR_CONTAINER, err.tone(90));
        put(MD3Color.ON_ERROR_CONTAINER, err.tone(10));

        put(MD3Color.BACKGROUND, n1.tone(98));
        put(MD3Color.ON_BACKGROUND, n1.tone(10));

        put(MD3Color.SURFACE, n1.tone(98));
        put(MD3Color.ON_SURFACE, n1.tone(10));
        put(MD3Color.SURFACE_VARIANT, n2.tone(90));
        put(MD3Color.ON_SURFACE_VARIANT, n2.tone(30));
        put(MD3Color.SURFACE_DIM, n1.tone(87));
        put(MD3Color.SURFACE_BRIGHT, n1.tone(98));

        put(MD3Color.SURFACE_CONTAINER_LOWEST, n1.tone(100));
        put(MD3Color.SURFACE_CONTAINER_LOW, n1.tone(96));
        put(MD3Color.SURFACE_CONTAINER, n1.tone(94));
        put(MD3Color.SURFACE_CONTAINER_HIGH, n1.tone(92));
        put(MD3Color.SURFACE_CONTAINER_HIGHEST, n1.tone(90));

        put(MD3Color.INVERSE_SURFACE, n1.tone(20));
        put(MD3Color.INVERSE_ON_SURFACE, n1.tone(95));

        put(MD3Color.OUTLINE, n2.tone(50));
        put(MD3Color.OUTLINE_VARIANT, n2.tone(80));

        put(MD3Color.SHADOW, n1.tone(0));
        put(MD3Color.SCRIM, n1.tone(0));
        put(MD3Color.SURFACE_TINT, a1.tone(40));
    }

    private void buildDark(CorePalette core) {
        TonalPalette a1 = core.a1;
        TonalPalette a2 = core.a2;
        TonalPalette a3 = core.a3;
        TonalPalette n1 = core.n1;
        TonalPalette n2 = core.n2;
        TonalPalette err = core.error;

        put(MD3Color.PRIMARY, a1.tone(80));
        put(MD3Color.ON_PRIMARY, a1.tone(20));
        put(MD3Color.PRIMARY_CONTAINER, a1.tone(30));
        put(MD3Color.ON_PRIMARY_CONTAINER, a1.tone(90));
        put(MD3Color.INVERSE_PRIMARY, a1.tone(40));

        put(MD3Color.SECONDARY, a2.tone(80));
        put(MD3Color.ON_SECONDARY, a2.tone(20));
        put(MD3Color.SECONDARY_CONTAINER, a2.tone(30));
        put(MD3Color.ON_SECONDARY_CONTAINER, a2.tone(90));

        put(MD3Color.TERTIARY, a3.tone(80));
        put(MD3Color.ON_TERTIARY, a3.tone(20));
        put(MD3Color.TERTIARY_CONTAINER, a3.tone(30));
        put(MD3Color.ON_TERTIARY_CONTAINER, a3.tone(90));

        put(MD3Color.ERROR, err.tone(80));
        put(MD3Color.ON_ERROR, err.tone(20));
        put(MD3Color.ERROR_CONTAINER, err.tone(30));
        put(MD3Color.ON_ERROR_CONTAINER, err.tone(90));

        put(MD3Color.BACKGROUND, n1.tone(6));
        put(MD3Color.ON_BACKGROUND, n1.tone(90));

        put(MD3Color.SURFACE, n1.tone(6));
        put(MD3Color.ON_SURFACE, n1.tone(90));
        put(MD3Color.SURFACE_VARIANT, n2.tone(30));
        put(MD3Color.ON_SURFACE_VARIANT, n2.tone(80));
        put(MD3Color.SURFACE_DIM, n1.tone(6));
        put(MD3Color.SURFACE_BRIGHT, n1.tone(24));

        put(MD3Color.SURFACE_CONTAINER_LOWEST, n1.tone(4));
        put(MD3Color.SURFACE_CONTAINER_LOW, n1.tone(10));
        put(MD3Color.SURFACE_CONTAINER, n1.tone(12));
        put(MD3Color.SURFACE_CONTAINER_HIGH, n1.tone(17));
        put(MD3Color.SURFACE_CONTAINER_HIGHEST, n1.tone(22));

        put(MD3Color.INVERSE_SURFACE, n1.tone(90));
        put(MD3Color.INVERSE_ON_SURFACE, n1.tone(20));

        put(MD3Color.OUTLINE, n2.tone(60));
        put(MD3Color.OUTLINE_VARIANT, n2.tone(30));

        put(MD3Color.SHADOW, n1.tone(0));
        put(MD3Color.SCRIM, n1.tone(0));
        put(MD3Color.SURFACE_TINT, a1.tone(80));
    }

    /**
     * Fixed roles are identical in both brightnesses by design.
     */
    private void buildFixed(CorePalette core) {
        put(MD3Color.PRIMARY_FIXED, core.a1.tone(90));
        put(MD3Color.PRIMARY_FIXED_DIM, core.a1.tone(80));
        put(MD3Color.ON_PRIMARY_FIXED, core.a1.tone(10));
        put(MD3Color.ON_PRIMARY_FIXED_VARIANT, core.a1.tone(30));

        put(MD3Color.SECONDARY_FIXED, core.a2.tone(90));
        put(MD3Color.SECONDARY_FIXED_DIM, core.a2.tone(80));
        put(MD3Color.ON_SECONDARY_FIXED, core.a2.tone(10));
        put(MD3Color.ON_SECONDARY_FIXED_VARIANT, core.a2.tone(30));

        put(MD3Color.TERTIARY_FIXED, core.a3.tone(90));
        put(MD3Color.TERTIARY_FIXED_DIM, core.a3.tone(80));
        put(MD3Color.ON_TERTIARY_FIXED, core.a3.tone(10));
        put(MD3Color.ON_TERTIARY_FIXED_VARIANT, core.a3.tone(30));
    }

    private void put(String role, int argb) {
        roles.put(role, new Color(argb, false));
    }

    /**
     * Publishes every role into the given defaults so components can read them via
     * {@link MD3Color}.
     */
    public void applyTo(UIDefaults defaults) {
        for (Map.Entry<String, Color> entry : roles.entrySet()) {
            defaults.put(entry.getKey(), entry.getValue());
        }

        defaults.put("md.sys.seed", new Color(seedArgb, false));
        defaults.put("md.sys.dark", dark);
    }

    public Color get(String role) {
        Color color = roles.get(role);

        return color != null ? color : Color.GRAY;
    }

    public boolean isDark() {
        return dark;
    }

    public int getSeedArgb() {
        return seedArgb;
    }
}
