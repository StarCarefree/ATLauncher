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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Data;
import com.atlauncher.Gsons;
import com.atlauncher.Launcher;
import com.atlauncher.data.Instance;
import com.atlauncher.data.QuickPlayOption;
import com.atlauncher.data.Settings;
import com.atlauncher.data.json.QuickPlay;
import com.atlauncher.data.minecraft.JavaRuntimes;
import com.atlauncher.gui.dialogs.InstanceSettingsDialog;
import com.atlauncher.gui.dialogs.instancesettings.CommandsInstanceSettingsTab;
import com.atlauncher.gui.dialogs.instancesettings.GeneralInstanceSettingsTab;
import com.atlauncher.gui.dialogs.instancesettings.JavaInstanceSettingsTab;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The instance settings, which are now the same list of rows the settings page is built from.
 *
 * <p>
 * These pages are almost entirely conditional: which Quick Play input is shown depends on the type
 * chosen, and whether a Java path can be set at all depends on whether the version of Minecraft
 * brings its own. That used to be done by toggling a label and a control separately - two calls per
 * setting, easy to get half right. It is now one row each, so what these check is that a row is
 * shown exactly when the setting behind it applies.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@link MD3GalleryRenderTest} for what offscreen
 * rendering can and cannot tell you about text.
 */
public class InstanceSettingsRenderTest {
    private static final int PAGE_WIDTH = 780;
    private static final int PAGE_HEIGHT = 520;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();

        // the Java tab lists the runtimes Mojang publishes for this machine; there are none here,
        // but the map has to exist or reading it throws before anything is laid out
        Data.JAVA_RUNTIMES = new JavaRuntimes();
        Data.JAVA_RUNTIMES.gamecore = new HashMap<>();
        Data.JAVA_RUNTIMES.linux = new HashMap<>();
        Data.JAVA_RUNTIMES.linuxI386 = new HashMap<>();
        Data.JAVA_RUNTIMES.linuxArm = new HashMap<>();
        Data.JAVA_RUNTIMES.linuxArm64 = new HashMap<>();
        Data.JAVA_RUNTIMES.macOs = new HashMap<>();
        Data.JAVA_RUNTIMES.macOsArm64 = new HashMap<>();
        Data.JAVA_RUNTIMES.windowsX64 = new HashMap<>();
        Data.JAVA_RUNTIMES.windowsX86 = new HashMap<>();
    }

    private static Instance load(String name) throws Exception {
        URL resource = InstanceSettingsRenderTest.class.getResource("/instances/" + name + "/instance.json");

        assertNotNull(resource, "missing fixture for " + name);

        Path file = Paths.get(resource.toURI());

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Instance instance = Gsons.DEFAULT.fromJson(reader, Instance.class);
            instance.ROOT = file.getParent();

            return instance;
        }
    }

    /**
     * A row re-wraps its description once it knows its width, which changes the height it asks for.
     * In the launcher its revalidate schedules another pass; laying it out by hand, each container
     * is invalidated first so its layout manager drops the sizes it cached beforehand.
     */
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

    private static void build(JPanel tab) {
        tab.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));

        layoutTree(tab);
        tab.invalidate();
        layoutTree(tab);
    }

    /** Every label on a row that is actually being shown, in the order they appear. */
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

    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> comboAfter(Container root, String label) {
        return (JComboBox<T>) controlAfter(root, label, JComboBox.class);
    }

    /**
     * The control on the row whose headline is {@code label}, found by walking to the row that
     * carries it. Rows are not named, and naming them only for a test would be worse than looking
     * them up by what the user reads.
     *
     * <p>
     * Hidden rows are walked past rather than into. A setting that does not apply is still in the
     * tree - the Java page carries both a path field and a "Minecraft provides one" note, and swaps
     * which is shown - so a search that ignored visibility would find either of them whichever was
     * on screen.
     */
    private static Component controlAfter(Container root, String label, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (c instanceof Container && c.isVisible()) {
                Container row = (Container) c;

                if (headlineOf(row) != null && headlineOf(row).equals(label)) {
                    Component control = firstOfType(row, type);

                    if (control != null) {
                        return control;
                    }
                }

                Component found = controlAfter(row, label, type);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static String headlineOf(Container row) {
        for (Component c : row.getComponents()) {
            if (c instanceof Container) {
                for (Component text : ((Container) c).getComponents()) {
                    if (text instanceof JLabel) {
                        return ((JLabel) text).getText();
                    }
                }
            }
        }

        return null;
    }

    private static Component firstOfType(Container root, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }

            if (type.isInstance(c)) {
                return c;
            }

            if (c instanceof Container) {
                Component found = firstOfType((Container) c, type);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Test
    public void testOnlyTheChosenQuickPlayInputIsShown() throws Exception {
        Instance instance = load("QuickPlayTest");
        instance.launcher.quickPlay = new QuickPlay("mc.example.com", null, null);

        GeneralInstanceSettingsTab tab = new GeneralInstanceSettingsTab(instance);
        build(tab);

        List<String> shown = visibleLabels(tab);

        assertTrue(shown.contains("Server address"),
                "an instance set to join a server does not offer its address: " + shown);
        assertFalse(shown.contains("Single Player World"),
                "the world picker is shown for an instance that is set to join a server: " + shown);
        assertFalse(shown.contains("Minecraft Realm"),
                "the realm id is shown for an instance that is set to join a server: " + shown);
    }

    @Test
    public void testTurningQuickPlayOffTakesItsInputsAway() throws Exception {
        Instance instance = load("QuickPlayTest");
        instance.launcher.quickPlay = new QuickPlay("mc.example.com", null, null);

        GeneralInstanceSettingsTab tab = new GeneralInstanceSettingsTab(instance);
        build(tab);

        JComboBox<Object> type = comboAfter(tab, "Quick Play Type");

        assertNotNull(type, "the Quick Play type is not on the page");

        for (int i = 0; i < type.getItemCount(); i++) {
            type.setSelectedIndex(i);

            if (QuickPlayOption.disabled.label.equals(type.getSelectedItem().toString())) {
                break;
            }
        }

        build(tab);

        List<String> shown = visibleLabels(tab);

        assertFalse(shown.contains("Server address"),
                "turning Quick Play off left the server address behind: " + shown);
        assertFalse(shown.contains("Single Player World"),
                "turning Quick Play off left the world picker behind: " + shown);
        assertFalse(shown.contains("Minecraft Realm"),
                "turning Quick Play off left the realm id behind: " + shown);
    }

    /**
     * There is a Java path row and a "you cannot set one" row, and exactly one of them applies.
     * Showing both is what the old two-calls-per-setting toggling made easy to do.
     */
    @Test
    public void testTheJavaPathIsOnlyOfferedWhenMinecraftDoesNotProvideOne() throws Exception {
        Instance provided = load("QuickPlayTest");
        provided.launcher.useJavaProvidedByMinecraft = true;

        JavaInstanceSettingsTab providedTab = new JavaInstanceSettingsTab(provided);
        build(providedTab);

        assertTrue(visibleLabels(providedTab).contains("Runtime Override"),
                "an instance using Minecraft's own Java cannot choose which runtime");
        assertNull(controlAfter(providedTab, "Java Path", JTextField.class),
                "a path field is offered for an instance whose Java comes from Minecraft");

        Instance own = load("QuickPlayTest");
        own.launcher.useJavaProvidedByMinecraft = false;

        JavaInstanceSettingsTab ownTab = new JavaInstanceSettingsTab(own);
        build(ownTab);

        assertNotNull(controlAfter(ownTab, "Java Path", JTextField.class),
                "an instance not using Minecraft's Java has nowhere to say where its own is");
        assertFalse(visibleLabels(ownTab).contains("Runtime Override"),
                "a runtime override is offered for an instance that is not using Minecraft's runtimes");
    }

    @Test
    public void testTheCommandFieldsFollowTheirSwitch() throws Exception {
        Instance instance = load("QuickPlayTest");
        instance.launcher.enableCommands = false;

        CommandsInstanceSettingsTab tab = new CommandsInstanceSettingsTab(instance);
        build(tab);

        Component preLaunch = controlAfter(tab, "Pre-launch command", JTextField.class);

        assertNotNull(preLaunch, "the pre-launch command has no field");
        assertFalse(preLaunch.isEnabled(), "a command can be typed into an instance that has commands turned off");
    }

    /**
     * The dialog itself: the sections on {@link com.atlauncher.gui.md3.nav.MD3Tabs} over a card
     * layout, and a pinned bar for Save and Cancel. It was the last {@code JTabbedPane} in the
     * launcher.
     *
     * <p>
     * {@code App.launcher} has no frame here, so the dialog is built with no owner - which is what
     * lets it be painted at all without starting the launcher.
     */
    @Test
    public void testTheDialogRenders() throws Exception {
        // on the event thread: a realized dialog's own layout runs there, and laying the same tree
        // out from the test thread deadlocks against it - see InstanceInstallerRenderTest.onEdt
        SwingUtilities.invokeAndWait(() -> {
            App.launcher = new Launcher();

            Instance instance;

            try {
                instance = load("QuickPlayTest");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            instance.launcher.quickPlay = new QuickPlay("mc.example.com", null, null);

            InstanceSettingsDialog dialog = new InstanceSettingsDialog(instance);

            Container content = dialog.getContentPane();
            content.setSize(dialog.getSize());

            layoutTree(content);
            content.invalidate();
            layoutTree(content);

            assertEquals(3, tabsOf(content).getTabCount(), "the dialog lost a section");

            BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            content.paint(g);
            g.dispose();

            try {
                new File("build/md3-preview").mkdirs();
                ImageIO.write(image, "png", new File("build/md3-preview/instance-settings-dialog-dark.png"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static MD3Tabs tabsOf(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3Tabs) {
                return (MD3Tabs) c;
            }

            if (c instanceof Container) {
                MD3Tabs found = tabsOf((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Test
    public void testInstanceSettingsRender() throws Exception {
        Instance instance = load("QuickPlayTest");
        instance.launcher.quickPlay = new QuickPlay("mc.example.com", null, null);

        JPanel[] tabs = new JPanel[] { new GeneralInstanceSettingsTab(instance),
                new JavaInstanceSettingsTab(instance), new CommandsInstanceSettingsTab(instance) };
        String[] names = new String[] { "general", "java", "commands" };

        new File("build/md3-preview").mkdirs();

        for (int i = 0; i < tabs.length; i++) {
            build(tabs[i]);

            BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            g.setColor(MD3Color.surface());
            g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            tabs[i].paint(g);
            g.dispose();

            ImageIO.write(image, "png", new File("build/md3-preview/instance-settings-" + names[i] + "-dark.png"));
        }
    }
}
