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
package com.atlauncher.gui.md3.input;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What a switch looks like partway through being flipped.
 *
 * <p>
 * The handle's position and size were interpolated, but every colour on the control - the track,
 * the handle, the outline - was chosen by {@code position > 0.5f}, and the tick was drawn at full
 * size and full opacity on the same test. So the slide had a jump cut in the middle of it: the
 * handle glided across while everything else changed in one frame halfway.
 *
 * <p>
 * Held at a point rather than raced, since what it looks like mid-travel is the entire question.
 */
public class SwitchMotionTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static MD3Switch switchAt(float position) {
        MD3Switch toggle = new MD3Switch("Reduce Animations?");
        toggle.setSelected(position >= 0.5f);
        ((MD3Switch.SwitchIcon) toggle.getIcon()).position.set(position);

        return toggle;
    }

    private static BufferedImage paintIcon(MD3Switch toggle) {
        int width = toggle.getIcon().getIconWidth();
        int height = toggle.getIcon().getIconHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        toggle.getIcon().paintIcon(toggle, g, 0, 0);
        g.dispose();

        return image;
    }

    /**
     * A point on the track the handle cannot reach at any point in its travel, so it reads the
     * track and not whatever is sliding over it.
     *
     * <p>
     * Above the handle rather than beside it: the handle covers every horizontal position on the
     * centre line at one end of the travel or the other, but it is a circle inside a taller
     * stadium, so the band along the top is always track. Far enough down not to catch the outline.
     */
    private static Color trackColourOf(MD3Switch toggle) {
        BufferedImage image = paintIcon(toggle);

        // the track is centred in a 48dp target; sample the band above the handle
        return new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2 - 12));
    }

    private static int differingPixels(BufferedImage a, BufferedImage b) {
        int differing = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        return differing;
    }

    private static boolean isBetween(int from, int to, int value) {
        return value >= Math.min(from, to) && value <= Math.max(from, to);
    }

    @Test
    public void testTheTrackIsPartwayBetweenItsTwoColoursPartwayAcross() {
        Color off = trackColourOf(switchAt(0f));
        Color half = trackColourOf(switchAt(0.5f));
        Color on = trackColourOf(switchAt(1f));

        assertNotEquals(off.getRGB(), on.getRGB(),
                "the track is the same colour on and off, so this test cannot tell them apart");

        assertNotEquals(off.getRGB(), half.getRGB(),
                "the track was still fully in its off colour halfway across, so it changes in one frame");
        assertNotEquals(on.getRGB(), half.getRGB(),
                "the track was already fully in its on colour halfway across, so it changed in one frame");

        assertTrue(isBetween(off.getRed(), on.getRed(), half.getRed())
                && isBetween(off.getGreen(), on.getGreen(), half.getGreen())
                && isBetween(off.getBlue(), on.getBlue(), half.getBlue()),
                "the track colour halfway across is not between the two ends - it is " + half + ", where off is "
                        + off + " and on is " + on);
    }

    /**
     * And the whole control, not only the track. This is what catches the tick, which used to be
     * switched on at full opacity partway through the handle's travel.
     */
    @Test
    public void testEveryPointAlongTheTravelLooksDifferent() {
        BufferedImage quarter = paintIcon(switchAt(0.25f));
        BufferedImage half = paintIcon(switchAt(0.5f));
        BufferedImage threeQuarters = paintIcon(switchAt(0.75f));

        assertTrue(differingPixels(quarter, half) > 0, "a switch a quarter across paints the same as at half");
        assertTrue(differingPixels(half, threeQuarters) > 0,
                "a switch half across paints the same as at three quarters");
    }

    /**
     * The ends still have to be the ends. A colour interpolated the wrong way round, or one that
     * never quite arrives, leaves a switch that never looks properly on.
     */
    @Test
    public void testTheEndsOfTheTravelAreTheRestingStates() {
        MD3Switch resting = new MD3Switch("Reduce Animations?");
        resting.setSelected(true);

        assertEquals(paintIcon(switchAt(1f)), paintIcon(resting),
                "a switch driven to the end of its travel does not look like one that was simply switched on");
    }

    private static void assertEquals(BufferedImage expected, BufferedImage actual, String message) {
        assertTrue(differingPixels(expected, actual) == 0, message);
    }
}
