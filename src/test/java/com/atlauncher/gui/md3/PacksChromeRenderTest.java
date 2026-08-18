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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.tabs.packbrowser.PacksNavigationPanel;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.ComboItem;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints the pack browser's chrome - the platform tabs and the filter toolbar that replaced the
 * left-hand tabbed pane and its row of combo boxes.
 *
 * <p>
 * The toolbar only works if its parts agree on a height: a search box, three chips and a button on
 * one line, none of them setting the row taller than the rest. That is the thing most likely to be
 * broken from a distance - by a token changing, or by the search field quietly falling back to the
 * 56dp form variant - so it is asserted rather than left to the eye.
 */
public class PacksChromeRenderTest {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 140;

    /** The six platforms the browser ships with, in the order the tab row shows them. */
    private static final String[] PLATFORMS = { "Search", "ATLauncher", "CurseForge", "FTB", "Modrinth", "Technic" };

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static Component findByName(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }

            if (c instanceof Container) {
                Component found = findByName((Container) c, name);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * The real brand marks, loaded the way the tab row loads them - they are 50px squares and have
     * to come down to Material's 24dp icon box without going ragged.
     */
    private static Icon platformIcon(String platform) {
        if ("Search".equals(platform)) {
            return null;
        }

        URL resource = PacksChromeRenderTest.class
                .getResource("/assets/image/modpack-platform/" + platform.toLowerCase(Locale.ENGLISH) + ".png");

        assertNotNull(resource, "missing platform icon for " + platform);

        return new ImageIcon(resource);
    }

    private static MD3Tabs buildTabs() {
        MD3Tabs tabs = new MD3Tabs();

        for (String platform : PLATFORMS) {
            Icon icon = platformIcon(platform);

            if (icon == null) {
                tabs.addTab(platform, MD3Icons.SEARCH);
            } else {
                tabs.addTab(platform, icon);
            }
        }

        return tabs;
    }

    private static PacksNavigationPanel buildToolbar(AtomicInteger reloads) {
        PacksNavigationPanel panel = new PacksNavigationPanel(new PacksNavigationPanel.Listener() {
            @Override
            public void onFiltersChanged() {
                reloads.incrementAndGet();
            }

            @Override
            public void onSortFieldChanged() {
                reloads.incrementAndGet();
            }

            @Override
            public void onSearch() {
                reloads.incrementAndGet();
            }

            @Override
            public void onAddManually() {
            }
        });

        List<ComboItem<String>> versions = new ArrayList<>();
        versions.add(new ComboItem<>(null, "All Versions"));
        versions.add(new ComboItem<>("1.21.4", "1.21.4"));
        versions.add(new ComboItem<>("1.20.1", "1.20.1"));
        panel.setMinecraftVersions(versions);

        panel.addCategory(new ComboItem<>(null, "All Categories"));
        panel.addCategory(new ComboItem<>("adventure", "Adventure and RPG"));

        List<ComboItem<String>> sorts = new ArrayList<>();
        sorts.add(new ComboItem<>("popularity", "Popularity"));
        sorts.add(new ComboItem<>("name", "Name"));
        panel.setSortFields(sorts);

        return panel;
    }

    private JPanel buildChrome(AtomicInteger reloads) {
        JPanel chrome = new JPanel();
        chrome.setLayout(new BoxLayout(chrome, BoxLayout.Y_AXIS));
        chrome.setOpaque(true);
        chrome.setBackground(MD3Color.surface());

        chrome.add(buildTabs());
        chrome.add(buildToolbar(reloads));

        chrome.setSize(new Dimension(WIDTH, HEIGHT));
        layoutTree(chrome);

        return chrome;
    }

    @Test
    public void testChromeRenders() throws Exception {
        JPanel chrome = buildChrome(new AtomicInteger());

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, WIDTH, HEIGHT);
        chrome.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/packs-chrome-dark.png"));
    }

    @Test
    public void testTabsShareTheWidthEqually() {
        MD3Tabs tabs = buildTabs();
        tabs.setSize(new Dimension(WIDTH, tabs.getPreferredSize().height));
        tabs.doLayout();

        int width = -1;
        int covered = 0;

        for (int i = 0; i < tabs.getTabCount() - 1; i++) {
            Component tab = tabs.getComponent(i);

            if (width < 0) {
                width = tab.getWidth();
            }

            assertEquals(width, tab.getWidth(), "the platform tabs are not sharing the row equally");
            covered += tab.getWidth();
        }

        covered += tabs.getComponent(tabs.getTabCount() - 1).getWidth();

        assertEquals(WIDTH, covered, "the tab row leaves a gap at its end");
    }

    @Test
    public void testSelectingATabReportsIt() {
        MD3Tabs tabs = buildTabs();
        AtomicInteger changes = new AtomicInteger();

        tabs.addChangeListener(e -> changes.incrementAndGet());

        assertEquals(0, tabs.getSelectedIndex(), "the first platform is not selected to start with");

        tabs.setSelectedIndex(3);

        assertEquals(3, tabs.getSelectedIndex());
        assertEquals(1, changes.get(), "selecting a platform did not report the change");

        tabs.setSelectedIndex(3);

        assertEquals(1, changes.get(), "re-selecting the same platform reported a change");
    }

    @Test
    public void testToolbarSitsOnOneLine() {
        AtomicInteger reloads = new AtomicInteger();
        JPanel chrome = buildChrome(reloads);
        PacksNavigationPanel toolbar = (PacksNavigationPanel) chrome.getComponent(1);

        Component search = findByName(toolbar, "packsSearchField");

        assertNotNull(search, "the search field is missing, or was renamed out from under the ui test");
        assertEquals(UIScale.scale(MD3Spacing.FIELD_HEIGHT_COMPACT), search.getPreferredSize().height,
                "the search box is not the compact variant, so it sets the toolbar's height on its own");

        // The row has to clear the tallest control in it, plus its own padding. That is the icon
        // buttons rather than the labelled ones: their container is 40dp like everything else here,
        // but the component is the touch target the container is centred in, and Material puts the
        // floor for that at 48. Eight pixels once per page header, for a target a pointer can hit.
        int padding = UIScale.scale(MD3Spacing.M) + UIScale.scale(MD3Spacing.S);
        int tallest = Math.max(MD3Spacing.BUTTON_HEIGHT, MD3Spacing.MIN_TOUCH_TARGET);
        int expected = UIScale.scale(tallest) + padding;

        assertEquals(expected, toolbar.getPreferredSize().height,
                "the toolbar is taller than the controls it holds, so something inside it is out of scale");
    }

    @Test
    public void testFiltersStartUnapplied() {
        AtomicInteger reloads = new AtomicInteger();
        PacksNavigationPanel toolbar = buildToolbar(reloads);

        assertNull(toolbar.getMinecraftVersion(), "the version filter starts applied, so the grid opens narrowed");
        assertNull(toolbar.getCategory(), "the category filter starts applied");
        assertEquals("popularity", toolbar.getSort(), "the first sort field is not the one in effect");
        assertTrue(toolbar.isSortDescending(), "packs open sorted ascending, which puts the least popular first");
        assertEquals(0, reloads.get(), "populating the filters reloaded the grid before the user touched anything");
    }
}
