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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.feedback.MD3Dialog;
import com.atlauncher.gui.md3.feedback.MD3Snackbar;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * Renders the two components that live in a window rather than in a panel.
 *
 * <p>
 * Dialogs and snackbars cannot go in {@link MD3Gallery} - one is a separate window, the other
 * attaches itself to a layered pane - so they are exercised here against a real frame instead.
 * Skipped where there is no display; the rest of the Material suite runs headless.
 *
 * <p>
 * Animation is pinned off so a capture lands on the finished state rather than partway through a
 * slide.
 */
public class MD3OverlayRenderTest {
    private static final String OUT = "build/md3-preview";

    @BeforeEach
    public void installTheme() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "no display available");

        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void write(BufferedImage image, String name) throws Exception {
        new File(OUT).mkdirs();

        ImageIO.write(image, "png", new File(OUT, name));
    }

    private static BufferedImage capture(java.awt.Component c, int width, int height) {
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

    @Test
    public void testDialogLaysOutAndPaints() throws Exception {
        MD3Dialog dialog = MD3Dialog.builder(null)
                .title("Delete instance")
                .icon(MD3Icons.WARNING)
                .headline("Delete All the Mods 9?")
                .supportingText("This removes the instance and everything in its folder, including "
                        + "worlds and screenshots. This cannot be undone.")
                .dismiss("Keep it")
                .action("Back up first", MD3Button.Variant.OUTLINED)
                .destructive("Delete")
                .build();

        java.awt.Container content = dialog.getContentPane();

        assertTrue(content.getWidth() > 0 && content.getHeight() > 0,
                "the dialog did not lay itself out during pack");

        write(capture(content, content.getWidth(), content.getHeight()), "dialog-dark.png");

        dialog.dispose();
    }

    @Test
    public void testSnackbarAttachesToTheWindowItWasGiven() throws Exception {
        JFrame frame = new JFrame("snackbar host");
        frame.setSize(640, 240);

        JPanel background = new JPanel();
        background.setBackground(MD3Color.surfaceContainerLow());
        frame.setContentPane(background);

        frame.setLocation(-2000, -2000); // off to one side, so the test does not flash on screen
        frame.setVisible(true);

        try {
            MD3Snackbar.show(frame, "Instance exported to Downloads", "Open folder", e -> {
            });

            // the snackbar posts itself onto the event queue, so wait for that to drain
            SwingUtilities.invokeAndWait(() -> {
            });
            SwingUtilities.invokeAndWait(() -> {
            });

            java.awt.Component[] overlay = frame.getLayeredPane().getComponents();
            java.awt.Component snackbar = null;

            for (java.awt.Component c : overlay) {
                if (c.getClass().getName().contains("SnackbarPanel")) {
                    snackbar = c;
                }
            }

            assertNotNull(snackbar, "the snackbar did not attach to the layered pane");
            assertTrue(snackbar.getWidth() > 0 && snackbar.getHeight() > 0, "the snackbar has no size");

            BufferedImage image = new BufferedImage(frame.getLayeredPane().getWidth(),
                    frame.getLayeredPane().getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(new Color(MD3Color.surfaceContainerLow().getRGB()));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            frame.getLayeredPane().paint(g);
            g.dispose();

            write(image, "snackbar-dark.png");
        } finally {
            frame.dispose();
        }
    }
}
