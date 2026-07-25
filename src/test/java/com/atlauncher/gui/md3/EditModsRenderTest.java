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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.Launcher;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Settings;
import com.atlauncher.data.Type;
import com.atlauncher.gui.components.ModsJCheckBox;
import com.atlauncher.gui.dialogs.EditModsDialog;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.token.MD3Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The mod manager.
 *
 * <p>
 * Its two lists were four nested {@link javax.swing.JSplitPane}s with their dividers disabled and
 * sized to zero, and their headings were a label beside an unlabelled tick box. They are now two
 * columns with a heading and a named "Select All" each.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@link MD3GalleryRenderTest} for what offscreen
 * rendering can and cannot tell you about text.
 */
public class EditModsRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);
        App.launcher = new Launcher();
    }

    /**
     * An instance with four mods in a directory of its own.
     *
     * <p>
     * Not the fixture directory: opening this dialog scans the instance, which drops any mod whose
     * file is not on disk and then saves - so a mod listed in a checked in {@code instance.json}
     * with no jar beside it both fails to show up and rewrites the fixture on the way past. The
     * files are empty; nothing here reads their contents, and a mod already recorded is not one the
     * scan will look up.
     */
    private static Instance instance() throws Exception {
        URL resource = EditModsRenderTest.class.getResource("/instances/VanillaTest/instance.json");

        assertNotNull(resource, "missing fixture");

        Instance instance;

        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(Paths.get(resource.toURI())), StandardCharsets.UTF_8)) {
            instance = Gsons.DEFAULT.fromJson(reader, Instance.class);
        }

        Path root = Files.createTempDirectory("atlauncher-edit-mods");
        root.toFile().deleteOnExit();

        instance.ROOT = root;

        instance.launcher.mods.add(mod(instance, "Sodium", "sodium-0.6.0.jar", false));
        instance.launcher.mods.add(mod(instance, "Iris Shaders", "iris-1.8.0.jar", false));
        instance.launcher.mods.add(mod(instance, "Mod Menu", "modmenu-11.0.0.jar", false));
        instance.launcher.mods.add(mod(instance, "OptiFine", "optifine.jar", true));

        return instance;
    }

    private static DisableableMod mod(Instance instance, String name, String file, boolean disabled) throws Exception {
        DisableableMod mod = new DisableableMod(name, "1.0.0", false, file, Type.mods, null, "A mod.", disabled, true,
                true, false, null, null, null, null);

        Path path = (disabled ? mod.getDisabledFile(instance) : mod.getFile(instance)).toPath();

        Files.createDirectories(path.getParent());
        Files.createFile(path);
        path.toFile().deleteOnExit();
        path.getParent().toFile().deleteOnExit();

        return mod;
    }

    private static void onEdt(EdtBody body) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    body.run();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof IllegalStateException && cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause instanceof Error) {
                throw (Error) cause;
            }

            throw (Exception) cause;
        }
    }

    private interface EdtBody {
        void run() throws Exception;
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

    private static List<String> visibleLabels(Container root) {
        List<String> found = new ArrayList<>();

        collectVisibleLabels(root, found);

        return found;
    }

    private static void collectVisibleLabels(Container root, List<String> found) {
        for (Component c : root.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }

            if (c instanceof JLabel) {
                String text = ((JLabel) c).getText();

                if (text != null && !text.trim().isEmpty() && !text.startsWith("<html>")) {
                    found.add(text);
                }
            }

            if (c instanceof Container) {
                collectVisibleLabels((Container) c, found);
            }
        }
    }

    private static EditModsDialog build() throws Exception {
        EditModsDialog dialog = new EditModsDialog(instance());

        Container content = dialog.getContentPane();
        content.setSize(dialog.getSize());

        layoutTree(content);
        content.invalidate();
        layoutTree(content);

        return dialog;
    }

    @Test
    public void testBothListsAreHeaded() throws Exception {
        onEdt(() -> {
            List<String> shown = visibleLabels(build().getContentPane());

            assertTrue(shown.contains("Enabled Mods"), "the enabled list has no heading: " + shown);
            assertTrue(shown.contains("Disabled Mods"), "the disabled list has no heading: " + shown);
        });
    }

    /**
     * A mod goes in the list it is in the state of, and is drawn there - the panels are sized by
     * hand, so a change to how they are laid out can leave them measuring zero and showing nothing.
     */
    @Test
    public void testEachModIsInTheListForItsState() throws Exception {
        onEdt(() -> {
            EditModsDialog dialog = build();

            List<ModsJCheckBox> boxes = new ArrayList<>();
            collect(dialog.getContentPane(), boxes);

            assertEquals(4, boxes.size(), "the dialog is not showing all four mods");

            for (ModsJCheckBox box : boxes) {
                assertTrue(box.getWidth() > 0 && box.getHeight() > 0,
                        "\"" + box.getText() + "\" was laid out at " + box.getWidth() + "x" + box.getHeight()
                                + ", so nothing of it is drawn");
            }
        });
    }

    /**
     * The panels holding the mods are sized by hand, because they sit in a scroll pane that has to
     * be told how far it can scroll. That height used to be the number of mods times a hardcoded 20
     * - a check box's height at 100% and at no other scale or font size - so the last mods in a long
     * list could not be scrolled to.
     */
    @Test
    public void testTheListIsAsTallAsTheModsInIt() throws Exception {
        onEdt(() -> {
            EditModsDialog dialog = build();

            List<ModsJCheckBox> boxes = new ArrayList<>();
            collect(dialog.getContentPane(), boxes);

            for (ModsJCheckBox box : boxes) {
                Container list = box.getParent();
                int needed = 0;

                for (Component sibling : list.getComponents()) {
                    needed += sibling.getPreferredSize().height;
                }

                assertTrue(list.getPreferredSize().height >= needed,
                        "the list asks for " + list.getPreferredSize().height + "px to show " + needed
                                + "px of mods, so the ones at the bottom cannot be scrolled to");
            }
        });
    }

    private static void collect(Container root, List<ModsJCheckBox> found) {
        for (Component c : root.getComponents()) {
            if (c instanceof ModsJCheckBox) {
                found.add((ModsJCheckBox) c);
            } else if (c instanceof Container) {
                collect((Container) c, found);
            }
        }
    }

    @Test
    public void testEditModsRenders() throws Exception {
        onEdt(() -> {
            EditModsDialog dialog = build();

            BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            dialog.getContentPane().paint(g);
            g.dispose();

            new File("build/md3-preview").mkdirs();
            ImageIO.write(image, "png", new File("build/md3-preview/edit-mods-dark.png"));
        });
    }
}
