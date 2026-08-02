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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Launcher;
import com.atlauncher.data.Pack;
import com.atlauncher.data.PackVersion;
import com.atlauncher.data.Settings;
import com.atlauncher.data.json.Mod;
import com.atlauncher.data.json.Version;
import com.atlauncher.gui.components.ModsJCheckBox;
import com.atlauncher.gui.dialogs.ModsChooser;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.workers.InstanceInstaller;

/**
 * The dialog that asks which optional mods to install.
 *
 * <p>
 * It was four {@link javax.swing.JSplitPane}s - one of them empty, there only to push the headings
 * down - over two panels with no layout manager, whose rows were positioned by hand twenty pixels
 * apart. That number had not been the height of a checkbox since they became Material ones, and was
 * never scaled for the display either.
 */
public class ModsChooserRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);

        // no frame, so the dialog is built with no owner - which is what lets it be built at all
        // without starting the launcher
        App.launcher = new Launcher();
    }

    private static Mod mod(String name, boolean optional, boolean onServer) {
        Mod mod = new Mod();
        mod.name = name;
        mod.optional = optional;
        mod.serverOptional = optional;
        mod.server = onServer;
        mod.file = name + ".jar";

        return mod;
    }

    /**
     * An installer carrying a mod list and nothing else. Its constructor only assigns fields and
     * resolves two paths, so it can be built without any of the install machinery being reachable.
     */
    private static InstanceInstaller installer(boolean isServer, List<Mod> mods) {
        Pack pack = new Pack();
        pack.name = "All the Mods 9";

        PackVersion version = new PackVersion();
        version.version = "0.3.2";

        InstanceInstaller installer = new InstanceInstaller("Test", pack, version, false, isServer, false, false,
                true, null, null, null, null, null, null, null, null, null, null, null);

        installer.allMods = mods;
        installer.packVersion = new Version();

        return installer;
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static List<ModsJCheckBox> checkboxesIn(Component c) {
        List<ModsJCheckBox> found = new ArrayList<>();

        if (c instanceof ModsJCheckBox) {
            found.add((ModsJCheckBox) c);
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                found.addAll(checkboxesIn(child));
            }
        }

        return found;
    }

    private static List<Mod> sampleMods() {
        return new ArrayList<>(Arrays.asList(
                mod("Journeymap", true, true),
                mod("JEI", true, true),
                mod("Iron Chests", true, true),
                mod("Fabric API", false, true),
                mod("Architectury", false, true)));
    }

    /**
     * The bug this rewrite found. The loop over the mods was a {@code for} with its counter
     * incremented at the bottom of the body and a bare {@code continue} partway up - so installing a
     * server, with any mod that does not go on one, span forever on that mod and the launcher hung
     * with no dialog and no error.
     */
    @Test
    public void testAServerInstallWithAClientOnlyModDoesNotHang() {
        List<Mod> mods = sampleMods();
        mods.add(mod("OptiFine", true, false));

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            ModsChooser chooser = installerChooser(true, mods);

            assertTrue(checkboxesIn(chooser.getContentPane()).size() > 0,
                    "the dialog came up with no mods in it");
        }, "the mods chooser hung - a client-only mod in a server install is the loop that never advances");
    }

    private static ModsChooser installerChooser(boolean isServer, List<Mod> mods) {
        return new ModsChooser(installer(isServer, mods));
    }

    /**
     * Every row is as tall as the checkbox asks to be. The hand-placed bounds pinned them to 20
     * pixels, which squashed a Material box drawn at 24dp - and at any display scale above 100% the
     * label was clipped through the middle.
     */
    @Test
    public void testEveryRowIsAsTallAsItsCheckbox() {
        ModsChooser chooser = installerChooser(false, sampleMods());
        chooser.pack();
        layoutTree(chooser.getContentPane());

        List<ModsJCheckBox> boxes = checkboxesIn(chooser.getContentPane());

        assertTrue(boxes.size() >= 5, "not every mod got a checkbox - found " + boxes.size());

        for (ModsJCheckBox box : boxes) {
            if (!box.isVisible()) {
                continue;
            }

            assertEquals(box.getPreferredSize().height, box.getHeight(),
                    "the row for " + box.getText() + " was laid out at " + box.getHeight()
                            + " but asks for " + box.getPreferredSize().height);
        }
    }

    /**
     * Optional mods on one side, required on the other. That split is the whole purpose of the
     * dialog, and it used to be expressed by which half of a split pane a panel went into.
     */
    @Test
    public void testOptionalAndRequiredModsAreInSeparateColumns() {
        ModsChooser chooser = installerChooser(false, sampleMods());
        chooser.pack();
        layoutTree(chooser.getContentPane());

        // grouped by the scroller each one landed in, which is what a column is here
        Set<Component> optionalScrollers = new HashSet<>();
        Set<Component> requiredScrollers = new HashSet<>();

        for (ModsJCheckBox box : checkboxesIn(chooser.getContentPane())) {
            Component scroller = SwingUtilities.getAncestorOfClass(JScrollPane.class, box);

            assertNotNull(scroller, "a mod checkbox is not in a scroller at all");
            (box.getMod().isOptional() ? optionalScrollers : requiredScrollers).add(scroller);
        }

        assertEquals(1, optionalScrollers.size(), "the optional mods are spread over more than one column");
        assertEquals(1, requiredScrollers.size(), "the required mods are spread over more than one column");
        assertTrue(Collections.disjoint(optionalScrollers, requiredScrollers),
                "the optional and required mods share a column, so the dialog says nothing");
    }

    @Test
    public void testRenderTheSheet() throws Exception {
        ModsChooser chooser = installerChooser(false, sampleMods());
        chooser.pack();

        Component content = chooser.getContentPane();
        layoutTree(content);

        BufferedImage image = new BufferedImage(Math.max(1, content.getWidth()), Math.max(1, content.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        content.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/mods-chooser-dark.png"));

        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    painted++;
                }
            }
        }

        assertTrue(painted > 0, "the dialog came out blank");
    }
}
