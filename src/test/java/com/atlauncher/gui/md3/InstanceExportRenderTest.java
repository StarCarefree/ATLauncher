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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.awt.Point;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mini2Dx.gettext.GetText;
import org.mini2Dx.gettext.PoFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.dialogs.InstanceExportDialog;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The export dialog, rebuilt as the same list of settings rows everything else uses.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class InstanceExportRenderTest {
    private static final int PAGE_WIDTH = 960;
    private static final int PAGE_HEIGHT = 640;
    /** The form column is 420dp plus the page padding. */
    private static final int FORM_COLUMN_RIGHT = 460;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
    }

    private static Instance load(String name) throws Exception {
        URL resource = InstanceExportRenderTest.class.getResource("/instances/" + name + "/instance.json");

        assertNotNull(resource, "missing fixture for " + name);

        Path file = Paths.get(resource.toURI());

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Instance instance = Gsons.DEFAULT.fromJson(reader, Instance.class);
            // the resource fixture is only instance.json. Real instances have mods and
            // config, and the dialog skips empty directories - so a preview built from the
            // fixture alone always said there was nothing to export
            Path root = Files.createTempDirectory("export-" + name);
            Files.copy(file, root.resolve("instance.json"));
            seedFolder(root, "mods", "sodium.jar");
            seedFolder(root, "config", "sodium.properties");
            seedFolder(root, "resourcepacks", "faithful.zip");
            seedFolder(root, "saves", "level.dat");
            instance.ROOT = root;

            return instance;
        }
    }

    private static void seedFolder(Path root, String folder, String file) throws Exception {
        Path dir = root.resolve(folder);
        Files.createDirectories(dir);
        Files.write(dir.resolve(file), new byte[] { 1 });
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

    /**
     * Fitting labels wrap through HTML, so the visible string is not always {@code getText()}.
     */
    private static String labelText(JLabel label) {
        if (label instanceof MD3FittingLabel) {
            return ((MD3FittingLabel) label).getFullText();
        }

        String text = label.getText();

        return text == null ? "" : text.replaceAll("<[^>]+>", "");
    }

    @Test
    public void testTheExportFormIsASettingsList() throws Exception {
        InstanceExportDialog dialog = new InstanceExportDialog(load("AllTheMods9"));
        dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(dialog);

        boolean foundCard = false;
        boolean foundSwitch = false;
        MD3Button export = null;
        MD3Button cancel = null;
        MD3Button browse = null;
        MD3Button reset = null;

        for (Component c : findAll(dialog)) {
            if (c instanceof MD3Card) {
                foundCard = true;
            }

            if (c instanceof MD3Switch) {
                foundSwitch = true;
            }

            if (c instanceof MD3Button) {
                MD3Button button = (MD3Button) c;
                String text = button.getText();

                if ("Export".equals(text)) {
                    export = button;
                } else if ("Cancel".equals(text)) {
                    cancel = button;
                } else if ("Browse".equals(text)) {
                    browse = button;
                } else if ("Reset".equals(text)) {
                    reset = button;
                }
            }
        }

        assertTrue(foundCard, "the export form is not grouped into cards");
        assertTrue(foundSwitch, "joint packaging and hash verification are not switches");
        assertNotNull(export, "no Export button");
        assertNotNull(cancel, "no Cancel button");
        assertNotNull(browse, "no Browse button");
        assertNotNull(reset, "no Reset button");

        Point browseOnDialog = SwingUtilities.convertPoint(browse.getParent(), browse.getLocation(), dialog);
        assertTrue(browseOnDialog.x + browse.getWidth() <= PAGE_WIDTH,
                "Browse is clipped at x=" + browseOnDialog.x);
        assertEquals(MD3Button.Variant.FILLED, export.getVariant(), "Export is not the confirming action");
        assertEquals(MD3Button.Variant.TEXT, cancel.getVariant(), "Cancel is not the lowest-emphasis dismiss");
        assertEquals(MD3Button.Variant.OUTLINED, browse.getVariant(), "Browse should be outlined");
        assertEquals(MD3Button.Variant.TEXT, reset.getVariant(),
                "Reset is outlined, so it competes with Browse");

        boolean foundPackHeading = false;

        for (Component c : findAll(dialog)) {
            if (c instanceof JLabel && "Pack".equals(((JLabel) c).getText())) {
                foundPackHeading = true;
            }
        }

        assertTrue(foundPackHeading, "the form has no Pack section");

        boolean foundFolders = false;
        boolean foundFolderList = false;

        for (Component c : findAll(dialog)) {
            if (c instanceof JLabel && "Folders To Export".equals(((JLabel) c).getText())) {
                foundFolders = true;
            }

            if (c instanceof MD3ListContainer) {
                foundFolderList = true;
            }
        }

        assertTrue(foundFolders, "the folders column has no heading");
        assertTrue(foundFolderList, "the folders are not in a list container of their own");

        boolean foundModsFolder = false;
        boolean foundDestination = false;

        for (Component c : findAll(dialog)) {
            if (c instanceof JLabel) {
                String text = ((JLabel) c).getText();

                if ("mods".equals(text)) {
                    foundModsFolder = true;
                }

                if ("Destination".equals(text)) {
                    foundDestination = true;
                    Point onDialog = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), dialog);
                    assertTrue(onDialog.y + c.getHeight() < PAGE_HEIGHT - 80,
                            "Destination is still under the fold at " + onDialog.y);
                }

                if ("Author".equals(text)) {
                    Point onDialog = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), dialog);
                    assertTrue(onDialog.x + 8 < FORM_COLUMN_RIGHT,
                            "Author was clipped off the form column at x=" + onDialog.x);
                }
            }
        }

        assertTrue(foundModsFolder, "the folder list is still empty");
        assertTrue(foundDestination, "the destination section is missing");

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        dialog.getContentPane().paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/instance-export-dark.png"));

        dialog.dispose();
    }

    /**
     * The first rewrite coined shorter English labels that were not in the po file. Gettext
     * answers a missing msgid with the source, so a Chinese session kept showing "Joint
     * packaging" and "Folders to export" next to the strings that did still match.
     */
    @Test
    public void testTheFormComesBackInChinese() throws Exception {
        try (java.io.InputStream in = InstanceExportRenderTest.class.getResourceAsStream("/assets/lang/zh-CN.po")) {
            assertNotNull(in, "zh-CN.po is not on the classpath");
            GetText.add(new PoFile(new Locale("zh", "CN"), in));
            GetText.setLocale(new Locale("zh", "CN"));

            InstanceExportDialog dialog = new InstanceExportDialog(load("AllTheMods9"));
            dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
            layoutTree(dialog);

            boolean foundFolders = false;
            boolean foundJoint = false;
            boolean foundExport = false;
            boolean foundSaveTo = false;

            for (Component c : findAll(dialog)) {
                if (c instanceof JLabel) {
                    String text = labelText((JLabel) c);

                    if (text.contains("要导出的文件夹")) {
                        foundFolders = true;
                    }

                    if (text.contains("联合打包")) {
                        foundJoint = true;
                    }

                    if (text.contains("导出位置")) {
                        foundSaveTo = true;
                    }
                }

                if (c instanceof MD3Button && "导出".equals(((MD3Button) c).getText())) {
                    foundExport = true;
                }
            }

            assertTrue(foundFolders, "Folders To Export did not resolve to its Chinese msgid");
            assertTrue(foundJoint, "Joint Packaging did not resolve to its Chinese msgid");
            assertTrue(foundSaveTo, "Destination did not resolve to its Chinese msgid");
            assertTrue(foundExport, "Export did not resolve to its Chinese msgid");

            BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            dialog.getContentPane().paint(g);
            g.dispose();

            new File("build/md3-preview").mkdirs();
            ImageIO.write(image, "png", new File("build/md3-preview/instance-export-zh.png"));

            dialog.dispose();
        } finally {
            GetText.setLocale(Locale.ENGLISH);
        }
    }
}
