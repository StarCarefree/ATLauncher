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
package com.atlauncher.themes.md3.hct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the colour engine to the reference CAM16/HCT values.
 *
 * <p>
 * These are the numbers Material's own implementation produces. Every colour in every theme is
 * derived through this code, so a regression here would quietly shift the entire launcher's palette
 * rather than failing anywhere visible.
 */
public class HctTest {
    private static final double TOLERANCE = 0.05;

    @Test
    public void testRedMatchesReferenceHct() {
        Hct red = Hct.fromInt(0xffff0000);

        assertEquals(27.408, red.getHue(), TOLERANCE);
        assertEquals(113.357, red.getChroma(), TOLERANCE);
        assertEquals(53.237, red.getTone(), TOLERANCE);
    }

    @Test
    public void testGreenMatchesReferenceHct() {
        Hct green = Hct.fromInt(0xff00ff00);

        assertEquals(142.139, green.getHue(), TOLERANCE);
        assertEquals(108.410, green.getChroma(), TOLERANCE);
        assertEquals(87.737, green.getTone(), TOLERANCE);
    }

    @Test
    public void testBlueMatchesReferenceHct() {
        Hct blue = Hct.fromInt(0xff0000ff);

        assertEquals(282.788, blue.getHue(), TOLERANCE);
        assertEquals(87.230, blue.getChroma(), TOLERANCE);
        assertEquals(32.302, blue.getTone(), TOLERANCE);
    }

    @Test
    public void testBlackAndWhiteSitAtTheEndsOfTheToneRange() {
        assertEquals(0.0, Hct.fromInt(0xff000000).getTone(), 0.01);
        assertEquals(100.0, Hct.fromInt(0xffffffff).getTone(), 0.01);

        assertEquals(0xff000000, Hct.from(0, 0, 0).toInt());
        assertEquals(0xffffffff, Hct.from(0, 0, 100).toInt());
    }

    @Test
    public void testColoursSurviveARoundTripThroughHct() {
        int[] samples = { 0xff89c236, 0xff6750a4, 0xffffd866, 0xff2d2a2e, 0xff1a202c, 0xffc53030, 0xff718096,
                0xfff57900, 0xffff79c5, 0xff7aa2f7 };

        for (int sample : samples) {
            Hct original = Hct.fromInt(sample);
            Hct roundTripped = Hct
                    .fromInt(Hct.from(original.getHue(), original.getChroma(), original.getTone()).toInt());

            assertEquals(original.getTone(), roundTripped.getTone(), 0.5,
                    () -> String.format("tone drifted for #%06X", sample & 0xffffff));
            assertEquals(original.getHue(), roundTripped.getHue(), 2.0,
                    () -> String.format("hue drifted for #%06X", sample & 0xffffff));
        }
    }

    @Test
    public void testToneIsMonotonicAcrossAPalette() {
        TonalPalette palette = TonalPalette.fromInt(0xff89c236);
        double previous = -1.0;

        for (int tone = 0; tone <= 100; tone += 5) {
            double actual = Hct.fromInt(palette.tone(tone)).getTone();

            assertTrue(actual >= previous - 0.5,
                    "tone " + tone + " came back darker than the tone below it");
            previous = actual;
        }
    }

    @Test
    public void testRequestedToneIsHonouredEvenWhenChromaIsNot() {
        // A highly saturated yellow at a dark tone does not exist in sRGB. Chroma has to give way;
        // tone must not, since every contrast guarantee in Material rests on it.
        for (int tone = 5; tone <= 95; tone += 10) {
            Hct clamped = Hct.from(100.0, 200.0, tone);

            assertEquals(tone, clamped.getTone(), 1.0, "tone was sacrificed at tone " + tone);
        }
    }

    @Test
    public void testPaletteToneCorrespondsToRelativeLightness() {
        TonalPalette palette = TonalPalette.fromInt(0xff6750a4);

        assertTrue(MdColorUtils.relativeLuminance(palette.tone(90)) > MdColorUtils
                .relativeLuminance(palette.tone(40)));
        assertTrue(MdColorUtils.contrastRatio(palette.tone(10), palette.tone(90)) > 10.0);
    }
}
