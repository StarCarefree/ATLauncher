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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.tabs.tools.AbstractToolPanel;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * Paints the tools grid.
 *
 * <p>
 * Each tool used to be a titled-border panel holding a centred paragraph hard-wrapped at seventy
 * characters, in a fixed three by two grid. The wrapping was fixed while the panels were not, so
 * what these assert is the opposite: that a description is measured against the width its card
 * actually has, and that the cards in a row still line up.
 */
public class ToolCardRenderTest {
    private static final int GRID_WIDTH = 1000;
    private static final int GRID_HEIGHT = 420;

    private static final String LONG_DESCRIPTION = "This tool clears out all the downloads done by the launcher. "
            + "This will not affect any instances, but means new pack installs may take longer as it needs to "
            + "redownload mods.";

    /** A tool with no view model behind it - what is under test is the card, not what it runs. */
    private static final class TestTool extends AbstractToolPanel {
        TestTool(String title, String description) {
            super(title, description);

            BOTTOM_PANEL.add(LAUNCH_BUTTON);
        }
    }

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

    private JPanel buildGrid(int width) {
        JPanel grid = new JPanel(
                new CardGridLayout(AbstractToolPanel.CARD_WIDTH, AbstractToolPanel.MAX_CARD_WIDTH, MD3Spacing.L));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(MD3Spacing.border(MD3Spacing.L));

        grid.add(new TestTool("Network Checker", "This tool does various tests on your network and determines any "
                + "issues that may pop up with connecting to our file servers and to other servers."));
        grid.add(new TestTool("Log Clearer", "This tool clears out all logs created by the launcher to free up "
                + "space and old junk."));
        grid.add(new TestTool("Skin Updater", "This tool will update all your accounts skins on the launcher."));
        grid.add(new TestTool("Download Clearer", LONG_DESCRIPTION));

        grid.setSize(new Dimension(width, GRID_HEIGHT));
        layoutTree(grid);
        layoutTree(grid);

        return grid;
    }

    private static JLabel findDescription(Container card) {
        JLabel found = null;

        for (Component c : card.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null
                    && ((JLabel) c).getText().startsWith("<html>")) {
                found = (JLabel) c;
            } else if (c instanceof Container) {
                JLabel nested = findDescription((Container) c);

                if (nested != null) {
                    found = nested;
                }
            }
        }

        return found;
    }

    private static MD3Button findAction(Container card) {
        for (Component c : card.getComponents()) {
            if (c instanceof MD3Button) {
                return (MD3Button) c;
            }

            if (c instanceof Container) {
                MD3Button found = findAction((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Test
    public void testToolsRender() throws Exception {
        JPanel grid = buildGrid(GRID_WIDTH);

        BufferedImage image = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, GRID_WIDTH, GRID_HEIGHT);
        grid.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/tools-dark.png"));
    }

    /**
     * Cards sharing a row share a height, however much or little each has to say. Rows are measured
     * separately, so only the first one is compared.
     */
    @Test
    public void testTheCardsInARowLineUp() {
        JPanel grid = buildGrid(GRID_WIDTH);

        int width = grid.getComponent(0).getWidth();
        int height = grid.getComponent(0).getHeight();
        int top = grid.getComponent(0).getY();
        int inRow = 0;

        for (Component card : grid.getComponents()) {
            assertEquals(width, card.getWidth(), "a card broke the grid's rhythm");

            if (card.getY() == top) {
                inRow++;

                assertEquals(height, card.getHeight(),
                        "cards differ in height, so a short description leaves its neighbour out of line");
            }
        }

        assertTrue(inRow > 1, "the grid put every card on its own row, so nothing was actually compared");
    }

    /**
     * The old wrapping was a character count, so it broke at the same place whatever width the panel
     * had. This is the thing that had to stop being true.
     *
     * <p>
     * Measured by laying one card out at two widths rather than through the grid: the grid adds a
     * column instead of letting cards grow much, so a card is close to the same width in a narrow
     * window as in a wide one - which is the point of it, and no test of the wrapping.
     */
    @Test
    public void testADescriptionIsWrappedToTheWidthItGets() {
        assertTrue(linesAtWidth(280) > linesAtWidth(600),
                "the description broke into the same number of lines at 280px and 600px, so it is being wrapped to "
                        + "a character count rather than to the card");
    }

    private int linesAtWidth(int width) {
        AbstractToolPanel card = new TestTool("Download Clearer", LONG_DESCRIPTION);
        card.setSize(new Dimension(width, GRID_HEIGHT));
        layoutTree(card);

        JLabel description = findDescription(card);

        assertNotNull(description, "the card lost its description");

        return description.getText().split("<br>").length;
    }

    @Test
    public void testEveryToolKeepsItsAction() {
        JPanel grid = buildGrid(GRID_WIDTH);

        for (Component card : grid.getComponents()) {
            MD3Button action = findAction((Container) card);

            assertNotNull(action, "a tool lost the button that runs it");
            assertEquals("Launch", action.getText(), "a tool's action was renamed out from under its translation");
        }
    }
}
