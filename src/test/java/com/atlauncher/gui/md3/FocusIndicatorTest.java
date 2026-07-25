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

import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3NavigationRail;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * That a keyboard user can see where they are.
 *
 * <p>
 * Focus is drawn, not queried, so what these do is paint a component twice - once claiming focus,
 * once not - and require the two to differ. A component that shows nothing for focus produces two
 * identical images, which is exactly the failure: the navigation rail was focusable and had arrow
 * keys, and drew nothing at all to say which destination they would move away from.
 *
 * <p>
 * {@code isFocusOwner} is overridden rather than a window being realized and focused, because a test
 * that depends on a real focus owner depends on the window manager.
 */
public class FocusIndicatorTest {
    private static final int RAIL_HEIGHT = 320;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /** A rail that says whether it holds focus, so the test decides rather than the desktop. */
    private static final class TestRail extends MD3NavigationRail {
        private boolean focused;

        @Override
        public boolean isFocusOwner() {
            return focused;
        }
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static BufferedImage paint(Component c, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, height);
        c.paint(g);
        g.dispose();

        return image;
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

    @Test
    public void testTheNavigationRailMarksTheDestinationTheArrowKeysAreOn() throws Exception {
        TestRail rail = new TestRail();
        rail.addDestination(MD3Icons.HOME, "Instances");
        rail.addDestination(MD3Icons.PACKAGE, "Packs");
        rail.addDestination(MD3Icons.SETTINGS, "Settings");
        rail.setSelectedIndex(1);

        int width = rail.getPreferredSize().width;
        rail.setSize(width, RAIL_HEIGHT);
        layoutTree(rail);

        BufferedImage unfocused = paint(rail, width, RAIL_HEIGHT);

        rail.focused = true;
        BufferedImage focused = paint(rail, width, RAIL_HEIGHT);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(focused, "png", new File("build/md3-preview/focus-rail-dark.png"));

        assertTrue(differingPixels(unfocused, focused) > 0,
                "the rail paints the same focused as not, so a keyboard user cannot see where they are");
    }

    /**
     * A standard icon button draws no container and no outline, so there is nothing for a focus
     * state to tint - it needs the ring the other buttons get.
     */
    @Test
    public void testAStandardIconButtonShowsItsFocus() throws Exception {
        final boolean[] holdsFocus = new boolean[1];

        MD3IconButton button = new MD3IconButton(MD3Icons.CHEVRON_RIGHT, "") {
            @Override
            public boolean isFocusOwner() {
                return holdsFocus[0];
            }
        };

        int size = button.getPreferredSize().width;
        button.setSize(size, size);

        BufferedImage unfocused = paint(button, size, size);

        holdsFocus[0] = true;
        BufferedImage focused = paint(button, size, size);

        assertTrue(differingPixels(unfocused, focused) > 0,
                "a standard icon button paints the same focused as not");
    }
}
