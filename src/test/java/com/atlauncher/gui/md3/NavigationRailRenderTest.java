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
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The launcher's primary navigation as it is worn: eight destinations, a header action, and a
 * break between the places you go and the places you configure.
 */
public class NavigationRailRenderTest {
    private static final int RAIL_HEIGHT = 700;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static MD3NavigationRail launcherRail() {
        MD3NavigationRail rail = new MD3NavigationRail();
        rail.setHeader(new MD3Fab(MD3Icons.ADD, "Create"));
        rail.addDestination(MD3Icons.ARTICLE, "News");
        rail.addDestination(MD3Icons.SEARCH, "Packs");
        rail.addDestination(MD3Icons.PACKAGE, "Instances");
        rail.addDestination(MD3Icons.DNS, "Servers");
        rail.addDestination(MD3Icons.PERSON, "Accounts");
        rail.addSeparator();
        rail.addDestination(MD3Icons.TUNE, "Tools");
        rail.addDestination(MD3Icons.SETTINGS, "Settings");
        rail.addDestination(MD3Icons.INFO, "About");
        rail.setSelectedIndex(2);

        return rail;
    }

    @Test
    public void testTheLauncherRailRenders() throws Exception {
        MD3NavigationRail rail = launcherRail();
        int width = rail.getPreferredSize().width;
        rail.setSize(width, RAIL_HEIGHT);
        layoutTree(rail);

        BufferedImage image = new BufferedImage(width, RAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, RAIL_HEIGHT);
        rail.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/rail-launcher-dark.png"));

        assertTrue(width >= UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH), "the rail is narrower than a rail");
        assertTrue(rail.getPreferredSize().height <= RAIL_HEIGHT,
                "eight destinations plus the FAB do not fit the launcher's minimum height");
    }

    /**
     * 80dp is not enough for "Einstellungen" or "Instances" at the type scale. Drawing them at
     * full width walks them off both sides of the rail, which is how the sidebar used to look.
     */
    @Test
    public void testALongLabelStaysInsideTheRail() throws Exception {
        MD3NavigationRail rail = new MD3NavigationRail();
        rail.addDestination(MD3Icons.SETTINGS, "Einstellungen");
        rail.setSelectedIndex(0);

        int width = rail.getPreferredSize().width;
        int height = rail.getPreferredSize().height;
        rail.setSize(width, height);
        layoutTree(rail);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        rail.paint(g);
        g.dispose();

        // the trailing edge is a hairline divider, so only the leading column is a fair
        // overflow check - an untruncated "Einstellungen" starts at a negative x
        int edge = image.getRGB(0, 0);
        boolean overflowed = false;

        for (int y = 0; y < height; y++) {
            if (image.getRGB(0, y) != edge) {
                overflowed = true;

                break;
            }
        }

        assertTrue(!overflowed, "a long destination label is drawn past the rail's edge");
    }

    @Test
    public void testAChineseLabelIsDrawnWithAFaceThatHasTheGlyphs() {
        MD3NavigationRail rail = new MD3NavigationRail();
        rail.addDestination(MD3Icons.PACKAGE, "实例");

        Component destination = rail.getComponentCount() > 0 ? findDestination(rail) : null;

        assertTrue(destination != null, "the destination was not added");
        assertTrue(destination.getFont().canDisplayUpTo("实例") < 0,
                "the rail label is still on a face that cannot draw Chinese");
    }

    private static Component findDestination(Container c) {
        for (Component child : c.getComponents()) {
            if (child.getClass().getName().endsWith("Destination")) {
                return child;
            }

            if (child instanceof Container) {
                Component found = findDestination((Container) child);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
