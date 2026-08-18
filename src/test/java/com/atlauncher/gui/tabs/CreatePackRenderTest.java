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
package com.atlauncher.gui.tabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.gui.md3.MD3Gallery;
import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.formdev.flatlaf.util.UIScale;

/**
 * The create-pack page, after the loader version stopped being a 23px combo capped at 400px.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class CreatePackRenderTest {
    private static final int PAGE_WIDTH = 1000;
    private static final int PAGE_HEIGHT = 700;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);
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

    @SuppressWarnings("unchecked")
    private static MD3ComboBox<ComboItem<LoaderVersion>> findCombo(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3ComboBox) {
                return (MD3ComboBox<ComboItem<LoaderVersion>>) c;
            }

            if (c instanceof Container) {
                MD3ComboBox<ComboItem<LoaderVersion>> found = findCombo((Container) c);

                if (found != null) {
                    return found;
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

    private static MD3ListContainer findList(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3ListContainer) {
                return (MD3ListContainer) c;
            }

            if (c instanceof Container) {
                MD3ListContainer found = findList((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Test
    public void testThePageUsesGroupedSurfacesAndAFullHeightVersionTable() {
        CreatePackTab tab = new CreatePackTab();
        tab.createViewModel();
        tab.onShow();
        tab.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(tab);

        assertTrue(findType(tab, MD3Card.class), "the form is not grouped into cards");

        MD3ListContainer versions = findList(tab);

        assertTrue(versions != null, "the version table is not in a Material list container");
        assertTrue(versions.getHeight() > PAGE_HEIGHT / 2,
                "the version table is still a strip: it is " + versions.getHeight() + "px on a "
                        + PAGE_HEIGHT + "px page");

        MD3ComboBox<ComboItem<LoaderVersion>> combo = findCombo(tab);

        assertTrue(combo != null, "there is no loader version dropdown");
        assertEquals(UIScale.scale(MD3Spacing.FIELD_HEIGHT_COMPACT), combo.getPreferredSize().height,
                "the loader version dropdown is still the 23px box it used to be");
    }

    @Test
    public void testARecommendedForgeBuildFitsTheDropdown() {
        CreatePackTab tab = new CreatePackTab();
        tab.createViewModel();
        tab.onShow();

        MD3ComboBox<ComboItem<LoaderVersion>> combo = findCombo(tab);
        String label = "14.23.5.2860 (Recommended)";

        combo.removeAllItems();
        combo.addItem(new ComboItem<>(new LoaderVersion("14.23.5.2860", true, "Forge"), label));
        combo.addItem(new ComboItem<>(new LoaderVersion("14.23.5.2855", false, "Forge"), "14.23.5.2855"));
        combo.setSelectedIndex(1);
        combo.sizeToItems();

        int text = MD3MixedText.width(MD3Type.font(MD3Type.BODY_LARGE, label), label);
        int insets = combo.getInsets().left + combo.getInsets().right;

        assertTrue(combo.getPreferredSize().width >= text + insets,
                "the recommended Forge version is still clipped");

        tab.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(tab);

        assertTrue(combo.getWidth() >= text + insets,
                "the laid-out dropdown is narrower than the version it has to show");
    }

    @Test
    public void testThePageRenders() throws Exception {
        CreatePackTab tab = new CreatePackTab();
        tab.createViewModel();
        tab.onShow();
        tab.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
        layoutTree(tab);

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        tab.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/create-pack-dark.png"));
    }
}
