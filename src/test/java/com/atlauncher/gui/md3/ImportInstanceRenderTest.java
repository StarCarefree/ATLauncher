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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mini2Dx.gettext.GetText;
import org.mini2Dx.gettext.PoFile;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.dialogs.ImportInstanceDialog;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The import dialog chrome, assembled without showing the window or talking to a pack host.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class ImportInstanceRenderTest {
    private static final int PAGE_WIDTH = 600;
    private static final int PAGE_HEIGHT = 400;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
        GetText.setLocale(Locale.ENGLISH);
    }

    private static void layoutTree(Component c) {
        if (c instanceof Container) {
            c.invalidate();
        }

        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static List<Component> findAll(Container root) {
        List<Component> all = new ArrayList<>();

        for (Component c : root.getComponents()) {
            all.add(c);

            if (c instanceof Container) {
                all.addAll(findAll((Container) c));
            }
        }

        return all;
    }

    private static String labelText(JLabel label) {
        String text = label.getText();

        return text == null ? "" : text.replaceAll("<[^>]+>", "");
    }

    @Test
    public void testTheChromeHasBothSourcesAndADisabledImport() throws Exception {
        ImportInstanceDialog dialog = new ImportInstanceDialog(null);
        dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(dialog);

        boolean foundCard = false;
        boolean foundUrl = false;
        boolean foundFile = false;
        MD3Button importButton = null;
        MD3Button cancel = null;
        MD3Button browse = null;
        int fields = 0;

        for (Component c : findAll(dialog)) {
            if (c instanceof MD3Card) {
                foundCard = true;
            }

            if (c instanceof MD3TextField) {
                fields++;
            }

            if (c instanceof JLabel) {
                String text = labelText((JLabel) c);

                if ("URL".equals(text)) {
                    foundUrl = true;
                }

                if ("File".equals(text)) {
                    foundFile = true;
                }
            }

            if (c instanceof MD3Button) {
                MD3Button button = (MD3Button) c;
                String text = button.getText();

                if ("Import".equals(text)) {
                    importButton = button;
                } else if ("Cancel".equals(text)) {
                    cancel = button;
                } else if ("Browse".equals(text)) {
                    browse = button;
                }
            }
        }

        assertTrue(foundCard, "the sources are not grouped into cards");
        assertTrue(foundUrl, "the URL source is missing");
        assertTrue(foundFile, "the file source is missing");
        assertTrue(fields >= 2, "a source field is missing");
        assertNotNull(importButton, "no Import button");
        assertNotNull(cancel, "no Cancel button");
        assertNotNull(browse, "no Browse button");
        assertEquals(MD3Button.Variant.FILLED, importButton.getVariant(), "Import is not the confirming action");
        assertEquals(MD3Button.Variant.TEXT, cancel.getVariant(), "Cancel is not the lowest-emphasis dismiss");
        assertEquals(MD3Button.Variant.OUTLINED, browse.getVariant(), "Browse should be outlined");
        assertFalse(importButton.isEnabled(), "Import is on before anything has been given");

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        dialog.getContentPane().paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/import-instance-dark.png"));

        dialog.dispose();
    }

    @Test
    public void testTheFormComesBackInChinese() throws Exception {
        try (java.io.InputStream in = ImportInstanceRenderTest.class.getResourceAsStream("/assets/lang/zh-CN.po")) {
            assertNotNull(in, "zh-CN.po is not on the classpath");
            GetText.add(new PoFile(new Locale("zh", "CN"), in));
            GetText.setLocale(new Locale("zh", "CN"));

            ImportInstanceDialog dialog = new ImportInstanceDialog(null);
            dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
            layoutTree(dialog);

            boolean foundHeadline = false;
            boolean foundImport = false;
            boolean foundBrowse = false;

            for (Component c : findAll(dialog)) {
                if (c instanceof JLabel && labelText((JLabel) c).contains("导入实例")) {
                    foundHeadline = true;
                }

                if (c instanceof MD3Button) {
                    String text = ((MD3Button) c).getText();

                    if ("导入".equals(text)) {
                        foundImport = true;
                    }

                    if ("浏览".equals(text)) {
                        foundBrowse = true;
                    }
                }
            }

            assertTrue(foundHeadline, "Import Instance did not resolve to its Chinese msgid");
            assertTrue(foundImport, "Import did not resolve to its Chinese msgid");
            assertTrue(foundBrowse, "Browse did not resolve to its Chinese msgid");

            BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            dialog.getContentPane().paint(g);
            g.dispose();

            new File("build/md3-preview").mkdirs();
            ImageIO.write(image, "png", new File("build/md3-preview/import-instance-zh.png"));

            dialog.dispose();
        } finally {
            GetText.setLocale(Locale.ENGLISH);
        }
    }
}
