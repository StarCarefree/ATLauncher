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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.Settings;
import com.atlauncher.data.Type;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthDownloadMetadata;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.gui.dialogs.AddModsDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The add-mods dialog chrome, assembled without talking to CurseForge or Modrinth.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class AddModsRenderTest {
    private static final int PAGE_WIDTH = 1000;
    private static final int PAGE_HEIGHT = 640;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
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

    private static MD3Chip findChip(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3Chip && text.equals(((MD3Chip) c).getText())) {
                return (MD3Chip) c;
            }

            if (c instanceof Container) {
                MD3Chip nested = findChip((Container) c, text);

                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    private static boolean findType(Container root, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                return true;
            }

            if (c instanceof Container && findType((Container) c, type)) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void testTheChromeFitsAGridAndASearchRow() throws Exception {
        AddModsDialog dialog = new AddModsDialog(null, new FakeMods(), false);
        dialog.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(dialog);

        assertTrue(findType(dialog, MD3Tabs.class), "the platform tabs are missing");
        assertTrue(findType(dialog, MD3TextField.class), "there is no search field");
        assertTrue(findType(dialog, MD3Chip.class), "the filter chips are missing");
        assertTrue(dialog.getWidth() >= UIScale.scale(880),
                "the dialog is still the 800px box that wrapped the toolbar");

        MD3Chip category = findChip(dialog, "All Categories");

        assertNotNull(category, "the category chip is not seeded, so it cannot be opened");
        assertTrue(category.isEnabled(), "the category chip is disabled, so it cannot be used");

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        dialog.getContentPane().paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/add-mods-dark.png"));

        dialog.dispose();
    }

    private static final class FakeMods implements ModManagement {
        @Override
        public Path getRoot() {
            return Paths.get("target", "fake-mods");
        }

        @Override
        public String getName() {
            return "All the Mods 9";
        }

        @Override
        public String getMinecraftVersion() {
            return "1.20.1";
        }

        @Override
        public LoaderVersion getLoaderVersion() {
            return new LoaderVersion("47.1.106", false, "NeoForge");
        }

        @Override
        public boolean supportsPlugins() {
            return false;
        }

        @Override
        public boolean isForgeLikeAndHasInstalledSinytraConnector() {
            return false;
        }

        @Override
        public List<DisableableMod> getMods() {
            return new ArrayList<>();
        }

        @Override
        public void addMod(DisableableMod mod) {
        }

        @Override
        public void addMods(List<DisableableMod> modsToAdd) {
        }

        @Override
        public void removeMod(DisableableMod mod) {
        }

        @Override
        public void addFileFromCurseForge(CurseForgeProject mod, CurseForgeFile file, ProgressDialog<Void> dialog) {
        }

        @Override
        public void addFileFromModrinth(ModrinthProject project, ModrinthVersion version, ModrinthFile file,
                Type installType, ModrinthDownloadMetadata.Reason downloadReason, String dependentVersionId,
                ProgressDialog<Void> dialog) {
        }

        @Override
        public void scanMissingMods(Window parent) {
        }

        @Override
        public void save() {
        }
    }
}
