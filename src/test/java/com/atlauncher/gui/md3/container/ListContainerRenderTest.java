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
package com.atlauncher.gui.md3.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.input.MD3Checkbox;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * The rounded, low-emphasis container the launcher's lists sit in.
 *
 * <p>
 * The container is the only thing allowed to draw an edge: the scroll pane inside it is borderless
 * and transparent, so nothing squares the list off against the page the way the look and feel's
 * default scroll pane border did.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@code MD3GalleryRenderTest} for what offscreen
 * rendering is for.
 */
public class ListContainerRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static JPanel rows() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(MD3Spacing.border(MD3Spacing.S, 0));

        String[] names = { "Iris Shaders", "Mod Menu", "Sodium", "Lithium", "FerriteCore" };

        for (String name : names) {
            MD3Checkbox row = new MD3Checkbox(name);
            row.setOpaque(false);
            list.add(row);
        }

        return list;
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static BufferedImage paintOnSurface(Component c) {
        layoutTree(c);

        BufferedImage image = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        c.paint(g);
        g.dispose();

        return image;
    }

    /**
     * The boundary is the container's to draw, not the scroll pane's: no border, no fill, no
     * viewport colour - anything else would square the list off inside the rounded outline.
     */
    @Test
    public void testTheScrollPaneDrawsNoEdgeOfItsOwn() {
        MD3ListContainer container = MD3ListContainer.wrapping(rows());

        assertNull(container.getScrollPane().getBorder(), "the scroll pane still draws a border");
        assertFalse(container.getScrollPane().isOpaque(), "the scroll pane still paints a background");
        assertFalse(container.getScrollPane().getViewport().isOpaque(), "the viewport still paints a background");
    }

    /**
     * A rounded container does not paint its corners - what shows there is the page behind it -
     * and fills the middle at {@code surfaceContainerLow}.
     */
    @Test
    public void testTheCornersAreCutAndTheBodyIsFilled() {
        MD3ListContainer container = MD3ListContainer.wrapping(rows());
        container.setSize(360, 240);

        BufferedImage image = paintOnSurface(container);

        Color corner = new Color(image.getRGB(0, 0));
        assertEquals(MD3Color.surface().getRGB(), corner.getRGB(),
                "the top left corner is painted - the container is not rounded");

        Color middle = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
        assertEquals(MD3Color.surfaceContainerLow().getRGB(), middle.getRGB(),
                "the body is not the low container tone");
    }

    @Test
    public void testRenderTheSheet() throws Exception {
        MD3ListContainer container = MD3ListContainer.wrapping(rows());

        JPanel page = new JPanel(null);
        page.setOpaque(false);
        page.setSize(400, 280);
        container.setBounds(MD3Spacing.scale(MD3Spacing.L), MD3Spacing.scale(MD3Spacing.L),
                page.getWidth() - MD3Spacing.scale(MD3Spacing.L) * 2, page.getHeight() - MD3Spacing.scale(MD3Spacing.L) * 2);
        page.add(container);

        BufferedImage image = paintOnSurface(page);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/list-container-dark.png"));

        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    painted++;
                }
            }
        }

        assertTrue(painted > 0, "the container came out blank");
    }
}
