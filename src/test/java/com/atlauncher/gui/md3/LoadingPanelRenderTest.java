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

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.themes.md3.token.MD3Color;
import com.formdev.flatlaf.util.UIScale;

/**
 * What the news tab, the pack browser and the mod browser show while they are fetching.
 *
 * <p>
 * It was {@code loading-bars.gif}: a fixed set of pixels in one colour, so it stayed that colour
 * under all eighteen themes and stayed the same physical size at every display scale. The point of
 * these tests is that it now comes out of the theme - the same panel painted under two schemes has
 * to differ, which a GIF could never do.
 */
public class LoadingPanelRenderTest {
    private static final int WIDTH = 420;
    private static final int HEIGHT = 200;

    @BeforeEach
    public void installTheme() throws Exception {
        install("MaterialDark");

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /** This one swaps to the light theme, and the rest of the suite expects the dark one. */
    @AfterEach
    public void restoreDarkTheme() throws Exception {
        install("MaterialDark");
    }

    private static void install(String theme) throws Exception {
        Class.forName("com.atlauncher.themes." + theme).getMethod("install").invoke(null);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static MD3CircularProgress spinnerWithin(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3CircularProgress) {
                return (MD3CircularProgress) c;
            }

            if (c instanceof Container) {
                MD3CircularProgress found = spinnerWithin((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static BufferedImage render(String text) {
        LoadingPanel panel = new LoadingPanel(text);
        panel.setSize(WIDTH, HEIGHT);
        layoutTree(panel);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, WIDTH, HEIGHT);
            panel.paint(g);
        } finally {
            g.dispose();
        }

        return image;
    }

    @Test
    public void testTheSpinnerIsAMaterialIndicator() {
        LoadingPanel panel = new LoadingPanel("Loading news...");

        MD3CircularProgress spinner = spinnerWithin(panel);

        assertNotNull(spinner, "the loading state has no Material indicator in it");
        assertTrue(spinner.isIndeterminate(), "a wait of unknown length is showing a determinate bar");
    }

    /**
     * The inline size exists so a page that is already showing its contents can say it is refreshing
     * them on a toolbar line, beside label-sized text, rather than with a 48dp disc.
     */
    @Test
    public void testTheSpinnerCanBeSizedForAToolbar() {
        MD3CircularProgress inline = MD3CircularProgress.inline();
        MD3CircularProgress full = MD3CircularProgress.indeterminate();

        assertTrue(inline.getPreferredSize().height < full.getPreferredSize().height,
                "the inline spinner is no smaller than the one meant to fill a panel");
        assertTrue(inline.getPreferredSize().height >= UIScale.scale(MD3CircularProgress.INLINE_DIAMETER) - 1,
                "the inline spinner ignored the size it was given");
    }

    @Test
    public void testTheLoadingStateTakesItsThemesColours() throws Exception {
        BufferedImage dark = render("Loading news...");

        install("MaterialLight");

        BufferedImage light = render("Loading news...");

        new File("build/md3-preview").mkdirs();
        ImageIO.write(dark, "png", new File("build/md3-preview/loading-dark.png"));
        ImageIO.write(light, "png", new File("build/md3-preview/loading-light.png"));

        boolean differs = false;

        for (int y = 0; y < HEIGHT && !differs; y += 4) {
            for (int x = 0; x < WIDTH && !differs; x += 4) {
                differs = dark.getRGB(x, y) != light.getRGB(x, y);
            }
        }

        assertTrue(differs, "the loading state paints identically under both themes, so it is not themed at all");
    }
}
