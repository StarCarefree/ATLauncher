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
package com.atlauncher.gui.md3.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The rewritten indicators: determinate anatomy, and an indeterminate wait that is still a wait
 * when motion has been reduced.
 */
public class MD3ProgressRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static BufferedImage paint(java.awt.Component c, int width, int height) {
        c.setSize(width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, height);
        c.paint(g);
        g.dispose();

        return image;
    }

    private static int pixelsOf(BufferedImage image, int rgb) {
        int count = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) {
                    count++;
                }
            }
        }

        return count;
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        int count = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }

        return count;
    }

    @Test
    public void testADeterminateBarChangesAlongItsTravel() {
        MD3LinearProgress empty = new MD3LinearProgress(0, 100);
        MD3LinearProgress mid = new MD3LinearProgress(0, 100);
        mid.setValue(50);
        MD3LinearProgress full = new MD3LinearProgress(0, 100);
        full.setValue(100);

        int width = 200;
        int height = UIScale.scale(MD3Spacing.PROGRESS_TRACK_HEIGHT);

        BufferedImage emptyImage = paint(empty, width, height);
        BufferedImage midImage = paint(mid, width, height);
        BufferedImage fullImage = paint(full, width, height);

        assertTrue(differing(emptyImage, midImage) > 0, "50% paints the same as 0%");
        assertTrue(differing(midImage, fullImage) > 0, "100% paints the same as 50%");
        assertTrue(pixelsOf(fullImage, MD3Color.primary().getRGB()) > pixelsOf(midImage, MD3Color.primary().getRGB()),
                "a finished bar has no more active indicator than one halfway along");
    }

    @Test
    public void testAHalfwayBarKeepsAStopAtTheEnd() {
        MD3LinearProgress bar = new MD3LinearProgress(0, 100);
        bar.setValue(40);

        int height = UIScale.scale(MD3Spacing.PROGRESS_TRACK_HEIGHT);
        BufferedImage image = paint(bar, 200, height);
        int primary = MD3Color.primary().getRGB();

        assertTrue(image.getRGB(198, height / 2) == primary,
                "the stop indicator is missing from the trailing end");
        assertTrue(image.getRGB(20, height / 2) == primary,
                "the active indicator did not start at the leading edge");
    }

    @Test
    public void testReducedMotionStillShowsAWaitingLinearBar() {
        MD3LinearProgress bar = new MD3LinearProgress();
        bar.setIndeterminate(true);

        int height = UIScale.scale(MD3Spacing.PROGRESS_TRACK_HEIGHT);
        BufferedImage image = paint(bar, 200, height);

        assertTrue(pixelsOf(image, MD3Color.primary().getRGB()) > 80,
                "a reduced-motion wait painted almost no active indicator");
    }

    @Test
    public void testReducedMotionStillShowsAWaitingSpinner() {
        MD3CircularProgress spinner = MD3CircularProgress.indeterminate();
        int size = spinner.getPreferredSize().width;
        BufferedImage image = paint(spinner, size, size);

        assertTrue(pixelsOf(image, MD3Color.primary().getRGB()) > 40,
                "a reduced-motion spinner collapsed to a speck");
    }

    @Test
    public void testADeterminateRingFillsAsTheValueDoes() {
        MD3CircularProgress empty = new MD3CircularProgress();
        empty.setValue(0);
        MD3CircularProgress mid = new MD3CircularProgress();
        mid.setValue(50);
        MD3CircularProgress full = new MD3CircularProgress();
        full.setValue(100);

        int size = empty.getPreferredSize().width;

        assertTrue(pixelsOf(paint(full, size, size), MD3Color.primary().getRGB()) > pixelsOf(paint(mid, size, size),
                MD3Color.primary().getRGB()), "a full ring has no more active arc than a half one");
        assertTrue(pixelsOf(paint(empty, size, size), MD3Color.primary().getRGB()) == 0,
                "an empty ring is already drawing an active arc");
    }

    @Test
    public void testLinearSegmentEndsTravelAcrossTheTrack() {
        float[] early = new float[4];
        float[] late = new float[4];
        MD3ProgressMotion.linearSegments(0.1f, early);
        MD3ProgressMotion.linearSegments(0.8f, late);

        assertTrue(late[1] > early[1] || late[3] > early[3],
                "the disjoint segments do not move along the track");

        for (float value : late) {
            assertTrue(value >= 0f && value <= 1f, "a segment end left the track: " + value);
        }
    }

    @Test
    public void testTheCircularWaitParksAtAReadableSweep() {
        float[] pose = new float[2];
        MD3ProgressMotion.circularArc(0f, pose);

        assertEquals(90f, pose[0], 0.01f);
        assertEquals(-MD3ProgressMotion.CIRCULAR_SWEEP_MAX, pose[1], 0.01f);
    }
}
