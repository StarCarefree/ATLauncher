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

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.dialogs.InstanceExportDialog;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The export dialog, rebuilt as the same list of settings rows everything else uses.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class InstanceExportRenderTest {
    private static final int PAGE_WIDTH = 720;
    private static final int PAGE_HEIGHT = 680;

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
            instance.ROOT = file.getParent();

            return instance;
        }
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

    @Test
    public void testTheExportFormIsASettingsList() throws Exception {
        InstanceExportDialog dialog = new InstanceExportDialog(load("AllTheMods9"));
        dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(dialog);

        boolean foundList = false;
        boolean foundSwitch = false;
        MD3Button export = null;
        MD3Button cancel = null;
        MD3Button browse = null;
        MD3Button reset = null;

        for (Component c : findAll(dialog)) {
            if (c instanceof MD3SettingsList) {
                foundList = true;
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

        assertTrue(foundList, "the export dialog is not built from a settings list");
        assertTrue(foundSwitch, "joint packaging and hash verification are not switches");
        assertNotNull(export, "no Export button");
        assertNotNull(cancel, "no Cancel button");
        assertNotNull(browse, "no Browse button");
        assertNotNull(reset, "no Reset button");
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
}
