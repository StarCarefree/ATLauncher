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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Launcher;
import com.atlauncher.data.Pack;
import com.atlauncher.data.PackVersion;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The pack installer, which is the dialog the launcher shows most.
 *
 * <p>
 * It was a {@link java.awt.GridBagLayout} of right-aligned labels and the controls beside them, with
 * "Save Mods?" explained by a tooltip on a help glyph. It is now the same list of rows the settings
 * are built from - so the explanation is on the row, and a setting that does not apply is one call
 * to hide rather than a label and a control to hide separately.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@link MD3GalleryRenderTest} for what offscreen
 * rendering can and cannot tell you about text.
 */
public class InstanceInstallerRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();

        // the dialog measures its version list against the theme's font to size the dropdown, and
        // nothing outside the launcher's own startup puts the theme where it looks for it
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);

        // no frame, so the dialog is built with no owner - which is what lets it be painted at all
        // without starting the launcher
        App.launcher = new Launcher();
    }

    private static Pack pack() {
        Pack pack = new Pack();
        pack.name = "All the Mods 9";
        pack.description = "A kitchen sink modpack.";

        List<PackVersion> versions = new ArrayList<>();

        for (String version : Arrays.asList("0.3.2", "0.3.1", "0.3.0")) {
            PackVersion packVersion = new PackVersion();
            packVersion.version = version;
            packVersion.isRecommended = version.equals("0.3.2");

            versions.add(packVersion);
        }

        pack.versions = versions;

        return pack;
    }

    /**
     * Runs the test body on the event dispatch thread.
     *
     * <p>
     * This dialog is a real {@link javax.swing.JDialog} and packs itself as it is built, which
     * realizes it and starts the event queue. Laying the same tree out from the test thread then
     * deadlocks: the test holds a {@code BoxLayout}'s monitor and waits for the AWT tree lock while
     * the event thread holds the tree lock and waits for that monitor. It is timing dependent -
     * this class passed on its own and hung the moment it ran alongside the rest of the suite.
     */
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

    private static InstanceInstallerDialog build() {
        InstanceInstallerDialog dialog = new InstanceInstallerDialog(pack(), false);

        Container content = dialog.getContentPane();
        content.setSize(dialog.getSize());

        layoutTree(content);
        content.invalidate();
        layoutTree(content);

        return dialog;
    }

    /**
     * A pack with no choosable loader has no loader version to pick, and the row for it should not
     * be sitting there empty - which is what an unnamed row would look like, since its name is only
     * set once a loader is known.
     */
    @Test
    public void testAPackWithNoLoaderIsNotAskedWhichLoaderVersion() throws Exception {
        onEdt(() -> {
            InstanceInstallerDialog dialog = build();

            List<String> shown = visibleLabels(dialog.getContentPane());

            assertTrue(shown.contains("Name"), "the installer does not ask what to call the instance: " + shown);
            assertTrue(shown.contains("Version To Install"), "the installer does not ask which version: " + shown);
            assertTrue(shown.contains("Installing All the Mods 9"),
                    "the installer does not say what it is installing: " + shown);

            for (String label : shown) {
                assertTrue(!label.isEmpty(), "an unnamed row is being shown: " + shown);
            }
        });
    }

    @Test
    public void testTheInstallerRenders() throws Exception {
        onEdt(() -> {
            InstanceInstallerDialog dialog = build();

            assertNotNull(dialog.getSize(), "the dialog was never sized");
            assertTrue(dialog.getWidth() > 0 && dialog.getHeight() > 0,
                    "the dialog sized itself to " + dialog.getWidth() + "x" + dialog.getHeight());

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
            ImageIO.write(image, "png", new File("build/md3-preview/instance-installer-dark.png"));
        });
    }
}
