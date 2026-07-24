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

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.Gsons;
import com.atlauncher.data.Server;
import com.atlauncher.gui.card.ServerCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * Paints the servers grid.
 *
 * <p>
 * A server was a collapsible titled panel: a split pane with the image on one side and nine buttons
 * under a description on the other. An instance is the same kind of thing to the person looking at
 * it and had already become a card, so the two pages showed the same idea in two unrelated shapes.
 * These assertions are mostly about them now agreeing.
 */
public class ServerCardRenderTest {
    private static final int GRID_WIDTH = 1000;
    private static final int GRID_HEIGHT = 420;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
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

    /**
     * Built from JSON, as the launcher loads them - a server carries enough state that constructing
     * one by hand would be inventing a shape the launcher never sees.
     *
     * <p>
     * {@code vanillaInstance} is set because a server that is neither vanilla nor from an external
     * pack is looked up by its numeric pack id, and there are no packs loaded here to find.
     */
    private static Server server(String name, String pack, String version, String loader) {
        String loaderJson = loader == null ? ""
                : ",\"loaderVersion\":{\"type\":\"" + loader + "\",\"version\":\"1.0.0\",\"rawVersion\":\"1.0.0\"}";

        return Gsons.DEFAULT.fromJson("{\"name\":\"" + name + "\",\"pack\":\"" + pack + "\",\"version\":\"" + version
                + "\",\"vanillaInstance\":true" + loaderJson + "}", Server.class);
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new CardGridLayout(ServerCard.CARD_WIDTH, ServerCard.MAX_CARD_WIDTH, MD3Spacing.L));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(MD3Spacing.border(MD3Spacing.L));

        grid.add(new ServerCard(server("All the Mods 9", "All the Mods 9", "1.0.5", "Forge")));
        grid.add(new ServerCard(server("Paper Survival", "Paper", "1.21.4", "Paper")));
        grid.add(new ServerCard(server("Vanilla", "Minecraft", "1.21.4", null)));

        grid.setSize(new Dimension(GRID_WIDTH, GRID_HEIGHT));
        layoutTree(grid);

        return grid;
    }

    private static MD3Button findPrimary(Container card) {
        for (Component c : card.getComponents()) {
            if (c instanceof MD3Button) {
                return (MD3Button) c;
            }

            if (c instanceof Container) {
                MD3Button found = findPrimary((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Test
    public void testServersRender() throws Exception {
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
        ImageIO.write(image, "png", new File("build/md3-preview/servers-dark.png"));
    }

    /**
     * Cards in a row share a height, and the one with the least to say has space left over. It has
     * to go somewhere that is not the button.
     */
    @Test
    public void testTheLaunchButtonIsTheSameSizeOnEveryCard() {
        JPanel grid = buildGrid();

        int height = -1;

        for (Component card : grid.getComponents()) {
            MD3Button launch = findPrimary((Container) card);

            assertNotNull(launch, "a card has no launch button");

            if (height < 0) {
                height = launch.getHeight();
            }

            assertEquals(height, launch.getHeight(),
                    "the launch buttons are different heights, so the row does not read as one set of cards");
        }
    }

    @Test
    public void testTheGridIsRegularAndFillsTheWidth() {
        JPanel grid = buildGrid();

        int width = grid.getComponent(0).getWidth();
        int height = grid.getComponent(0).getHeight();

        for (Component card : grid.getComponents()) {
            assertEquals(width, card.getWidth(), "a card broke the grid's rhythm");
            assertEquals(height, card.getHeight(),
                    "cards differ in height, so a server with no loader sits out of line");
        }

        Component last = grid.getComponent(grid.getComponentCount() - 1);
        int shortfall = GRID_WIDTH - MD3Spacing.L - (last.getX() + last.getWidth());

        assertTrue(shortfall >= 0 && shortfall < grid.getComponentCount(),
                "the grid leaves " + shortfall + "px unused down its trailing edge");
    }

    /**
     * Launching is the thing a server card exists for, so it is the one button on the face of it -
     * the other eight actions are behind the overflow.
     */
    @Test
    public void testLaunchingIsTheOnlyButtonOnTheCard() {
        JPanel grid = buildGrid();
        MD3Button primary = findPrimary((Container) grid.getComponent(0));

        assertNotNull(primary, "the card has no launch button");
        assertEquals("Launch", primary.getText(), "the card's primary action is not launching the server");
        assertEquals(1, countButtons((Container) grid.getComponent(0)),
                "the card grew a second button, so the actions are back to competing with each other");
    }

    private static int countButtons(Container root) {
        int found = 0;

        for (Component c : root.getComponents()) {
            if (c instanceof MD3Button) {
                found++;
            } else if (c instanceof Container) {
                found += countButtons((Container) c);
            }
        }

        return found;
    }
}
