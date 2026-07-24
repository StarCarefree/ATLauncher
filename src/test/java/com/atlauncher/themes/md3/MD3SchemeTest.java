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
package com.atlauncher.themes.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.UIDefaults;

import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.hct.MdColorUtils;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Scheme;

/**
 * Checks that a generated scheme actually delivers what Material 3 promises.
 *
 * <p>
 * The contrast assertions are the important ones. Material's whole argument for generating colour
 * from tone rather than picking it by hand is that readability then holds for <em>any</em> seed - so
 * these run across a spread of seeds, including the awkward ones (near black, near white, fully
 * desaturated) that a hand tuned palette would have been quietly broken by.
 */
public class MD3SchemeTest {
    /** WCAG AA for body text. */
    private static final double TEXT_CONTRAST = 4.5;
    /** WCAG AA for user interface components and graphical objects. */
    private static final double NON_TEXT_CONTRAST = 3.0;

    private static final int[] SEEDS = {
            0xFF89C236, // ATLauncher green
            0xFF6750A4, // Material baseline purple
            0xFFFFD866, // Monokai Pro yellow
            0xFFFF79C5, // Dracula pink
            0xFF0AB0D1, // Cyan Light
            0xFF7AA2F7, // TokyoNight blue
            0xFFF57900, // Arc Orange
            0xFF000000, // degenerate: black
            0xFFFFFFFF, // degenerate: white
            0xFF808080 // degenerate: mid grey
    };

    private static final String[][] TEXT_PAIRS = {
            { MD3Color.ON_PRIMARY, MD3Color.PRIMARY },
            { MD3Color.ON_PRIMARY_CONTAINER, MD3Color.PRIMARY_CONTAINER },
            { MD3Color.ON_SECONDARY, MD3Color.SECONDARY },
            { MD3Color.ON_SECONDARY_CONTAINER, MD3Color.SECONDARY_CONTAINER },
            { MD3Color.ON_TERTIARY, MD3Color.TERTIARY },
            { MD3Color.ON_TERTIARY_CONTAINER, MD3Color.TERTIARY_CONTAINER },
            { MD3Color.ON_ERROR, MD3Color.ERROR },
            { MD3Color.ON_ERROR_CONTAINER, MD3Color.ERROR_CONTAINER },
            { MD3Color.ON_SURFACE, MD3Color.SURFACE },
            { MD3Color.ON_SURFACE_VARIANT, MD3Color.SURFACE_VARIANT },
            { MD3Color.ON_SURFACE, MD3Color.SURFACE_CONTAINER_HIGHEST },
            { MD3Color.ON_BACKGROUND, MD3Color.BACKGROUND },
            { MD3Color.INVERSE_ON_SURFACE, MD3Color.INVERSE_SURFACE } };

    @Test
    public void testEveryRoleIsPublished() {
        for (boolean dark : new boolean[] { true, false }) {
            MD3Scheme scheme = MD3Scheme.from(0xFF89C236, dark);
            UIDefaults defaults = new UIDefaults();
            scheme.applyTo(defaults);

            for (String role : MD3Color.roles()) {
                assertNotNull(defaults.get(role), role + " missing from the " + (dark ? "dark" : "light") + " scheme");
                assertTrue(defaults.get(role) instanceof Color, role + " is not a colour");
            }
        }
    }

    @Test
    public void testTextIsReadableOnItsContainerForEverySeed() {
        for (int seed : SEEDS) {
            for (boolean dark : new boolean[] { true, false }) {
                MD3Scheme scheme = MD3Scheme.from(seed, dark);

                for (String[] pair : TEXT_PAIRS) {
                    double contrast = MdColorUtils.contrastRatio(scheme.get(pair[0]).getRGB(),
                            scheme.get(pair[1]).getRGB());

                    assertTrue(contrast >= TEXT_CONTRAST, String.format(
                            "%s on %s is only %.2f:1 for seed #%06X (%s)",
                            pair[0], pair[1], contrast, seed & 0xffffff, dark ? "dark" : "light"));
                }
            }
        }
    }

    @Test
    public void testOutlinesAreVisibleAgainstTheirSurface() {
        for (int seed : SEEDS) {
            for (boolean dark : new boolean[] { true, false }) {
                MD3Scheme scheme = MD3Scheme.from(seed, dark);
                double contrast = MdColorUtils.contrastRatio(scheme.get(MD3Color.OUTLINE).getRGB(),
                        scheme.get(MD3Color.SURFACE).getRGB());

                assertTrue(contrast >= NON_TEXT_CONTRAST, String.format(
                        "outline on surface is only %.2f:1 for seed #%06X (%s)",
                        contrast, seed & 0xffffff, dark ? "dark" : "light"));
            }
        }
    }

    @Test
    public void testSurfaceContainersFormAnOrderedRamp() {
        for (boolean dark : new boolean[] { true, false }) {
            MD3Scheme scheme = MD3Scheme.from(0xFF89C236, dark);

            double lowest = MdColorUtils.relativeLuminance(scheme.get(MD3Color.SURFACE_CONTAINER_LOWEST).getRGB());
            double low = MdColorUtils.relativeLuminance(scheme.get(MD3Color.SURFACE_CONTAINER_LOW).getRGB());
            double base = MdColorUtils.relativeLuminance(scheme.get(MD3Color.SURFACE_CONTAINER).getRGB());
            double high = MdColorUtils.relativeLuminance(scheme.get(MD3Color.SURFACE_CONTAINER_HIGH).getRGB());
            double highest = MdColorUtils.relativeLuminance(scheme.get(MD3Color.SURFACE_CONTAINER_HIGHEST).getRGB());

            if (dark) {
                // raised surfaces get lighter as they come forward
                assertTrue(lowest < low && low < base && base < high && high < highest,
                        "dark surface containers are not in ascending lightness order");
            } else {
                assertTrue(lowest > low && low > base && base > high && high > highest,
                        "light surface containers are not in descending lightness order");
            }
        }
    }

    @Test
    public void testDarkAndLightDifferMeaningfully() {
        MD3Scheme dark = MD3Scheme.from(0xFF89C236, true);
        MD3Scheme light = MD3Scheme.from(0xFF89C236, false);

        assertNotEquals(dark.get(MD3Color.SURFACE), light.get(MD3Color.SURFACE));
        assertTrue(MdColorUtils.relativeLuminance(light.get(MD3Color.SURFACE).getRGB()) > MdColorUtils
                .relativeLuminance(dark.get(MD3Color.SURFACE).getRGB()));
    }

    @Test
    public void testSchemesAreCachedPerSeedAndBrightness() {
        assertEquals(MD3Scheme.from(0xFF123456, true), MD3Scheme.from(0xFF123456, true));
        assertNotEquals(MD3Scheme.from(0xFF123456, true), MD3Scheme.from(0xFF123456, false));
    }

    @Test
    public void testSeedDetectionPrefersAnExplicitOverride() {
        UIDefaults defaults = new UIDefaults();
        defaults.put("md.sys.seed.override", new Color(0xFF79C5));
        defaults.put("accentColor", new Color(0x89C236));

        assertEquals(0xFFFF79C5, MD3Bridge.detectSeed(defaults, MD3Bridge.DEFAULT_SEED));
    }

    @Test
    public void testSeedDetectionFallsBackThroughTheCandidateList() {
        UIDefaults defaults = new UIDefaults();
        defaults.put("TabbedPane.underlineColor", new Color(0xF57900));

        assertEquals(0xFFF57900, MD3Bridge.detectSeed(defaults, MD3Bridge.DEFAULT_SEED));
    }

    @Test
    public void testSeedDetectionSkipsColoursThatAreNotAccents() {
        UIDefaults defaults = new UIDefaults();
        // a near black decoration line, as used by the High Tech Darkness theme
        defaults.put("TabbedPane.underlineColor", new Color(0x00061F));
        // and a flat grey, which would generate a colourless scheme
        defaults.put("ProgressBar.foreground", new Color(0x808080));

        assertEquals(MD3Bridge.DEFAULT_SEED, MD3Bridge.detectSeed(defaults, MD3Bridge.DEFAULT_SEED));
    }

    @Test
    public void testSeedDetectionFallsBackWhenNothingIsPublished() {
        assertEquals(MD3Bridge.DEFAULT_SEED, MD3Bridge.detectSeed(new UIDefaults(), MD3Bridge.DEFAULT_SEED));
    }
}
