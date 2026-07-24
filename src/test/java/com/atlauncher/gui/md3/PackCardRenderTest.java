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
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.gui.card.packbrowser.CurseForgePackCard;
import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.card.packbrowser.ModrinthPackCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * Paints modpack cards from each platform in one grid.
 *
 * <p>
 * The six platform cards now share {@code MD3PackCard}, so what is worth testing is that the shared
 * part copes with what the platforms actually return: summaries from one sentence to a paragraph,
 * and none at all. All three have to come out the same size, or the grid loses its baseline.
 *
 * <p>
 * Image URLs are left null so no card reaches for the network; the cover falls back to its
 * placeholder, which is what the layout has to accommodate anyway.
 */
public class PackCardRenderTest {
    private static final int GRID_WIDTH = 1000;
    private static final int GRID_HEIGHT = 480;
    private static final int GAP = 16;

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

    private static CurseForgeProject curseForge(String name, String summary, int downloads) {
        CurseForgeProject project = new CurseForgeProject();
        project.name = name;
        project.summary = summary;
        project.downloadCount = downloads;
        project.latestFiles = new ArrayList<>();

        return project;
    }

    private static ModrinthSearchHit modrinth(String title, String description, int downloads) {
        ModrinthSearchHit hit = new ModrinthSearchHit();
        hit.title = title;
        hit.description = description;
        hit.downloads = downloads;
        hit.slug = "test";

        return hit;
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new CardGridLayout(MD3PackCard.CARD_WIDTH, MD3PackCard.MAX_CARD_WIDTH, GAP));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        grid.add(new CurseForgePackCard(curseForge("All the Mods 9",
                "A large kitchen sink modpack with over 400 mods, quests and a custom progression system.",
                18_400_000)));
        grid.add(new ModrinthPackCard(modrinth("Fabulously Optimized",
                "A modpack focused on performance and vanilla aesthetics.", 4_200_000)));
        grid.add(new CurseForgePackCard(curseForge("Tiny Pack", null, 850)));

        grid.setSize(new Dimension(GRID_WIDTH, GRID_HEIGHT));
        layoutTree(grid);

        return grid;
    }

    @Test
    public void testCardsRenderAcrossPlatforms() throws Exception {
        JPanel grid = buildGrid();

        BufferedImage image = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, GRID_WIDTH, GRID_HEIGHT);
        grid.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/packs-dark.png"));
    }

    @Test
    public void testCardsAreAUniformSize() {
        JPanel grid = buildGrid();

        int width = -1;
        int height = -1;

        for (Component card : grid.getComponents()) {
            if (width < 0) {
                width = card.getWidth();
                height = card.getHeight();
            }

            assertEquals(width, card.getWidth(), "a card broke the grid's rhythm");
            assertEquals(height, card.getHeight(),
                    "cards differ in height, so a pack with no summary sits out of line");
            assertTrue(width > 0 && height > 0, "a card has no size");
        }
    }

    /**
     * The cards used to be pinned to 280dp, so whatever did not divide evenly into the page was
     * left as a gutter down the right - most of a card's width on a maximised window, which read as
     * the last column having failed to load.
     */
    @Test
    public void testTheGridFillsTheWidth() {
        JPanel grid = buildGrid();
        Component last = grid.getComponent(grid.getComponentCount() - 1);

        int columns = 0;

        for (Component card : grid.getComponents()) {
            if (card.getY() == last.getY()) {
                columns++;
            }
        }

        int right = grid.getComponent(columns - 1).getX() + grid.getComponent(columns - 1).getWidth();
        int shortfall = GRID_WIDTH - GAP - right;

        // integer division leaves at most a pixel per column unspent, which is not a gutter
        assertTrue(shortfall >= 0 && shortfall < columns,
                "the grid leaves " + shortfall + "px unused down its trailing edge");
    }

    /**
     * A card stretched by the grid has to re-measure what was built for 280dp, or it grows a blank
     * strip where its summary stops and its cover art does not reach.
     */
    @Test
    public void testAStretchedCardCarriesItsContentWithIt() {
        JPanel grid = buildGrid();
        MD3PackCard card = (MD3PackCard) grid.getComponent(0);

        assertTrue(card.getWidth() > 0, "the card was never laid out");

        Component cover = findCover(card);

        assertNotNull(cover, "the card lost its cover");
        assertEquals(card.getWidth(), cover.getWidth(),
                "the cover does not reach the card's edges, so a stretched card is framed in background");

        Component summary = findSummary(card);

        assertNotNull(summary, "the card lost its summary");
        assertTrue(summary.getWidth() >= card.getWidth() - 40,
                "the summary kept its 280dp wrapping width, so the card has a blank column beside it");
    }

    /** The cover is the only child of the card's north wrapper. */
    private static Component findCover(Container card) {
        for (Component c : card.getComponents()) {
            if (c instanceof JPanel && c.getY() == 0 && ((Container) c).getComponentCount() == 1) {
                return c;
            }
        }

        return null;
    }

    private static Component findSummary(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null
                    && ((JLabel) c).getText().startsWith("<html>")) {
                return c;
            }

            if (c instanceof Container) {
                Component found = findSummary((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
