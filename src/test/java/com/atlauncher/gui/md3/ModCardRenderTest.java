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
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.atlauncher.App;
import com.atlauncher.Launcher;
import com.atlauncher.data.Settings;
import com.atlauncher.data.curseforge.CurseForgeAuthor;
import com.atlauncher.data.curseforge.CurseForgeCategory;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.gui.card.CurseForgeProjectCard;
import com.atlauncher.gui.card.ModrinthSearchHitCard;
import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.token.MD3Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The mod browser's grid.
 *
 * <p>
 * These two cards were the last pre-Material holdouts in the launcher: a titled border in a
 * hardcoded 12pt bold face around a fixed 250x180 box, duplicated line for line between the
 * platforms. They now share {@link MD3PackCard} with the pack browser, and show the author,
 * download count and category that the search response was already carrying and both cards threw
 * away.
 *
 * <p>
 * Icon URLs are left null so nothing reaches for the network.
 */
public class ModCardRenderTest {
    private static final int GRID_WIDTH = 1000;
    private static final int GRID_HEIGHT = 460;
    private static final int GAP = 16;

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
     * Both platforms have to come out the same size, or the grid loses its baseline - which is the
     * thing a shared base class is for.
     */
    @Test
    public void testBothPlatformsProduceTheSameCard() {
        JPanel grid = buildGrid();

        Dimension first = grid.getComponent(0).getSize();

        for (Component card : grid.getComponents()) {
            assertEquals(first.width, card.getSize().width, "cards came out different widths");
            assertEquals(first.height, card.getSize().height, "cards came out different heights");
        }
    }

    @Test
    public void testTheGridReachesBothEdges() {
        JPanel grid = buildGrid();

        Component leftmost = grid.getComponent(0);
        Component rightmost = grid.getComponent(0);

        for (Component card : grid.getComponents()) {
            if (card.getX() < leftmost.getX()) {
                leftmost = card;
            }

            if (card.getX() + card.getWidth() > rightmost.getX() + rightmost.getWidth()) {
                rightmost = card;
            }
        }

        assertTrue(leftmost.getX() <= GAP + 1, "the grid starts at " + leftmost.getX() + "px, leaving a gutter");
        assertTrue(rightmost.getX() + rightmost.getWidth() >= GRID_WIDTH - GAP - 1,
                "the grid stops at " + (rightmost.getX() + rightmost.getWidth()) + "px of " + GRID_WIDTH);
    }

    @Test
    public void testModCardsRender() throws Exception {
        JPanel grid = buildGrid();

        BufferedImage image = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        grid.paint(g);
        g.dispose();

        File out = new File("build/md3-preview/mod-cards-dark.png");
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);

        assertTrue(out.exists(), "nothing was written");
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new CardGridLayout(MD3PackCard.CARD_WIDTH, MD3PackCard.MAX_CARD_WIDTH, GAP));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        grid.add(new CurseForgeProjectCard(curseForge("Sodium",
                "A modern rendering engine for Minecraft which greatly improves frame rates.", 18_400_000,
                "jellysquid3", "Performance"), null, e -> {
                }, e -> {
                }));
        grid.add(new ModrinthSearchHitCard(modrinth("Iris Shaders",
                "A modern shaders mod compatible with OptiFine shaderpacks.", 4_200_000, "IMS", "optimization"), null,
                e -> {
                }, e -> {
                }));
        grid.add(new CurseForgeProjectCard(curseForge("Tiny Mod", null, 850, null, null), null, e -> {
        }, e -> {
        }));

        grid.setSize(new Dimension(GRID_WIDTH, GRID_HEIGHT));
        layoutTree(grid);

        return grid;
    }

    private static CurseForgeProject curseForge(String name, String summary, int downloads, String author,
            String category) {
        CurseForgeProject project = new CurseForgeProject();
        project.name = name;
        project.summary = summary;
        project.downloadCount = downloads;
        project.latestFiles = new ArrayList<>();

        if (author != null) {
            CurseForgeAuthor curseForgeAuthor = new CurseForgeAuthor();
            curseForgeAuthor.name = author;
            project.authors = new ArrayList<>(Arrays.asList(curseForgeAuthor));
        }

        if (category != null) {
            CurseForgeCategory curseForgeCategory = new CurseForgeCategory();
            curseForgeCategory.name = category;
            project.categories = new ArrayList<>(Arrays.asList(curseForgeCategory));
        }

        return project;
    }

    private static ModrinthSearchHit modrinth(String title, String description, int downloads, String author,
            String category) {
        ModrinthSearchHit hit = new ModrinthSearchHit();
        hit.title = title;
        hit.description = description;
        hit.downloads = downloads;
        hit.slug = "test";
        hit.projectId = "abc";
        hit.author = author;
        hit.categories = new ArrayList<>(Arrays.asList(category));

        return hit;
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }
}
