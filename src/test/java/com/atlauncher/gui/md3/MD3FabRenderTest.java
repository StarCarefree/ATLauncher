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
package com.atlauncher.gui.md3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Fab;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3NavigationRail;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The create instance action at the top of the navigation rail.
 *
 * <p>
 * It was a filled icon button, which is 40dp, circular and filled with primary - one control among
 * the rest. A floating action button is 56dp on a 16dp corner in the primary container, and casts a
 * shadow, all of which is what makes it read as the thing the window is for.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@link MD3GalleryRenderTest} for what offscreen
 * rendering can and cannot tell you about text.
 */
public class MD3FabRenderTest {
    private static final int RAIL_HEIGHT = 380;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /** One of these swaps to the light theme, and the rest of the suite expects the dark one. */
    @org.junit.jupiter.api.AfterEach
    public void restoreDarkTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static void paintInto(BufferedImage image, Component c, int x, int y) {
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.translate(x, y);
        c.paint(g);
        g.dispose();
    }

    private static BufferedImage surface(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, height);
        g.dispose();

        return image;
    }

    private static MD3Fab fab() {
        MD3Fab fab = new MD3Fab(MD3Icons.ADD, "Create Instance");
        fab.setSize(fab.getPreferredSize());

        return fab;
    }

    /**
     * A FAB is one of the few things in Material 3 that still casts a shadow, and Swing clips
     * painting to the component - so it has to be bigger than its own container for the shadow to
     * have anywhere to fall.
     *
     * <p>
     * Checked on the light theme, because that is where a shadow is meant to be legible: Material's
     * shadow alphas on a dark surface are low enough that a broken one and a working one look much
     * the same.
     */
    @Test
    public void testTheFabKeepsRoomForItsShadow() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialLight").getMethod("install").invoke(null);

        MD3Fab fab = fab();
        int size = fab.getPreferredSize().width;

        assertTrue(size > UIScale.scale(MD3Spacing.FAB_SIZE),
                "the FAB is exactly its container's size, so its shadow is clipped away to nothing");

        BufferedImage image = surface(size, size);
        paintInto(image, fab, 0, 0);

        ImageIO.write(image, "png", new File("build/md3-preview/fab-light.png"));

        // halfway between the component's edge and the container's, level with the middle of it -
        // outside the shape, and near enough that the blur has not yet faded out
        int surface = MD3Color.surface().getRGB();
        int beside = image.getRGB(UIScale.scale(MD3Elevation.shadowBlur(MD3Elevation.LEVEL4)) / 2, size / 2);

        assertTrue(beside != surface,
                "nothing is painted beside the container, so the FAB is not casting a shadow at all");
    }

    @Test
    public void testFabRenders() throws Exception {
        MD3Fab resting = fab();
        MD3Fab hovered = fab();
        MD3Fab disabled = fab();

        hovered.getModel().setRollover(true);
        disabled.setEnabled(false);

        int size = resting.getPreferredSize().width;
        int gap = UIScale.scale(MD3Spacing.L);
        BufferedImage image = surface(size * 3 + gap * 4, size + gap * 2);

        paintInto(image, resting, gap, gap);
        paintInto(image, hovered, gap * 2 + size, gap);
        paintInto(image, disabled, gap * 3 + size * 2, gap);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/fab-dark.png"));
    }

    @Test
    public void testTheRailWearsItAsItsHeader() throws Exception {
        MD3NavigationRail rail = new MD3NavigationRail();
        rail.setHeader(fab());
        rail.addDestination(MD3Icons.HOME, "Instances");
        rail.addDestination(MD3Icons.PACKAGE, "Packs");
        rail.addDestination(MD3Icons.SETTINGS, "Settings");
        rail.setSelectedIndex(0);

        int width = rail.getPreferredSize().width;
        rail.setSize(width, RAIL_HEIGHT);
        layoutTree(rail);

        BufferedImage image = surface(width, RAIL_HEIGHT);
        paintInto(image, rail, 0, 0);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/rail-with-fab-dark.png"));

        assertTrue(rail.getPreferredSize().width >= UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH),
                "the FAB made the rail narrower than a rail");
    }
}
