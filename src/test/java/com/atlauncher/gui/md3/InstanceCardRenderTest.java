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

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.Gsons;
import com.atlauncher.data.Instance;
import com.atlauncher.gui.card.InstanceCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3MenuButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * Builds real instance cards from real instance files and paints them.
 *
 * <p>
 * The card is the piece of this migration with the most behaviour hanging off it - eight buttons,
 * six menus and their visibility rules - so it is worth constructing from the same JSON the launcher
 * loads rather than from a mock. That catches the whole path: Gson to model, model to card, card to
 * pixels.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}. See {@link MD3GalleryRenderTest} for what offscreen
 * rendering can and cannot tell you about text.
 */
public class InstanceCardRenderTest {
    private static final int GRID_WIDTH = 1000;
    private static final int GRID_HEIGHT = 460;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static Instance load(String name) throws Exception {
        URL resource = InstanceCardRenderTest.class.getResource("/instances/" + name + "/instance.json");

        assertNotNull(resource, "missing fixture for " + name);

        Path file = Paths.get(resource.toURI());

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Instance instance = Gsons.DEFAULT.fromJson(reader, Instance.class);
            instance.ROOT = file.getParent();

            return instance;
        }
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static int countBadges(Container root) {
        int found = 0;

        for (Component c : root.getComponents()) {
            if (c instanceof MD3Badge) {
                found++;
            } else if (c instanceof Container) {
                found += countBadges((Container) c);
            }
        }

        return found;
    }

    private JPanel buildGrid() throws Exception {
        // the same layout the page uses - a flow layout places cards at their preferred width and
        // leaves the remainder as a gutter down the trailing edge, which is what this replaced
        JPanel grid = new JPanel(
                new CardGridLayout(InstanceCard.CARD_WIDTH, InstanceCard.MAX_CARD_WIDTH, MD3Spacing.L));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        grid.add(new InstanceCard(load("AllTheMods9"), true, "%1$s (%2$s %3$s)"));
        grid.add(new InstanceCard(load("VanillaTest"), false, "%1$s (%2$s %3$s)"));

        Instance corrupted = load("VanillaTest");
        corrupted.launcher.isPlayable = false;
        corrupted.launcher.name = "Broken Install";
        grid.add(new InstanceCard(corrupted, false, "%1$s (%2$s %3$s)"));

        grid.setSize(new Dimension(GRID_WIDTH, GRID_HEIGHT));
        layoutTree(grid);

        return grid;
    }

    @Test
    public void testCardsRenderForEveryState() throws Exception {
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
        ImageIO.write(image, "png", new File("build/md3-preview/instances-dark.png"));
    }

    @Test
    public void testCardsAreAUniformWidth() throws Exception {
        JPanel grid = buildGrid();

        int width = -1;

        for (Component card : grid.getComponents()) {
            if (width < 0) {
                width = card.getWidth();
            }

            assertEquals(width, card.getWidth(), "a card broke the grid's rhythm");
            assertTrue(card.getHeight() > 0, "a card has no height");
        }
    }

    /**
     * The reason the page left {@code WrapLayout}: a flow layout places each card at its preferred
     * width and leaves whatever does not divide evenly as a gutter down the right, which on a
     * maximised window came to most of a card's width and read as a column having failed to load.
     */
    @Test
    public void testTheGridReachesBothEdges() throws Exception {
        JPanel grid = buildGrid();

        int rightmost = 0;

        for (Component card : grid.getComponents()) {
            rightmost = Math.max(rightmost, card.getX() + card.getWidth());
        }

        int content = GRID_WIDTH - grid.getInsets().right;

        assertTrue(content - rightmost <= grid.getComponentCount(),
                "the grid left a " + (content - rightmost) + "px gutter down its trailing edge");
    }

    @Test
    public void testBadgesStayWithinOneRow() throws Exception {
        JPanel grid = buildGrid();

        for (Component card : grid.getComponents()) {
            int badges = countBadges((Container) card);

            assertTrue(badges <= 3,
                    "a card showed " + badges + " badges, which wraps onto a row the card is not tall enough for");
        }
    }

    @Test
    public void testPlayIsASplitMenuAndAnUpdateStepsUpBesideIt() throws Exception {
        InstanceCard card = new InstanceCard(load("AllTheMods9"), true, "%1$s");
        card.setSize(card.getPreferredSize());
        layoutTree(card);

        MD3MenuButton play = findPlay(card);

        assertNotNull(play, "the card has no Play button");
        assertTrue(play.isSplit(), "Play is no longer split, so offline is buried in the overflow");
        assertEquals(play.getPreferredSize().height, play.getHeight(),
                "Play was stretched to the overflow's height");

        MD3Button update = null;

        for (Component c : findAll(card)) {
            if (c instanceof MD3Button && !(c instanceof MD3MenuButton)
                    && "Update".equals(((MD3Button) c).getText())) {
                update = (MD3Button) c;
            }
        }

        assertNotNull(update, "an instance with an update has no Update button on the card");
        assertEquals(MD3Button.Variant.TONAL, update.getVariant(),
                "Update is not tonal, so it competes with Play");
    }

    @Test
    public void testACorruptedInstanceCannotBePlayed() throws Exception {
        Instance corrupted = load("VanillaTest");
        corrupted.launcher.isPlayable = false;

        InstanceCard card = new InstanceCard(corrupted, false, "%1$s");
        layoutTree(card);

        boolean foundDisabledPlay = false;

        for (Component c : findAll((Container) card)) {
            if (c instanceof com.atlauncher.gui.md3.button.MD3Button && !c.isEnabled()) {
                foundDisabledPlay = true;
            }
        }

        assertTrue(foundDisabledPlay, "a corrupted instance still offers a working play button");
    }

    private static MD3MenuButton findPlay(Container root) {
        for (Component c : findAll(root)) {
            if (c instanceof MD3MenuButton && "Play".equals(((MD3MenuButton) c).getText())) {
                return (MD3MenuButton) c;
            }
        }

        return null;
    }

    private static java.util.List<Component> findAll(Container root) {
        java.util.List<Component> all = new java.util.ArrayList<>();

        for (Component c : root.getComponents()) {
            all.add(c);

            if (c instanceof Container) {
                all.addAll(findAll((Container) c));
            }
        }

        return all;
    }
}
