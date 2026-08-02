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
package com.atlauncher.gui.md3.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.formdev.flatlaf.util.UIScale;

/**
 * The data table, in the shape the create-pack version list gives it.
 *
 * <p>
 * What separates a Material table from Swing's default is what it does <em>not</em> draw - no grid,
 * no vertical lines, no box around each cell - so most of these check that something is absent, and
 * the sheet is there for the parts that are easier to see than to assert.
 */
public class TableRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static MD3Table versions() {
        DefaultTableModel model = new DefaultTableModel(
                new String[][] {
                        { "1.21.1", "2024-08-08", "release" },
                        { "1.21", "2024-06-13", "release" },
                        { "24w14potato", "2024-04-01", "snapshot" },
                        { "b1.7.3", "2011-07-08", "old_beta" },
                },
                new String[] { "Version", "Released", "Type" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        MD3Table table = new MD3Table(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return table;
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    /** How much of the image is something other than the page behind it. */
    private static int paintedPixels(BufferedImage image) {
        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    painted++;
                }
            }
        }

        return painted;
    }

    private static BufferedImage paint(Component c, int width, int height) {
        c.setSize(new Dimension(width, height));

        // a scroll pane's viewport and view are laid out by their own parents, so one doLayout on
        // the outside leaves the table at no size and the image comes out empty
        layoutTree(c);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, height);
        c.paint(g);
        g.dispose();

        return image;
    }

    /**
     * Swing's default is a spreadsheet - every cell boxed in on all four sides. Material's is a list
     * with columns, ruled between the rows and nowhere else.
     */
    @Test
    public void testThereIsNoGrid() {
        MD3Table table = versions();

        assertEquals(false, table.getShowVerticalLines(), "the table draws vertical rules");
        assertEquals(new Dimension(0, 0), table.getIntercellSpacing(),
                "the table leaves a gap between cells, which is where the grid used to be drawn");
    }

    /**
     * 36dp, not Material's standard 52 - the create-pack table lists every Minecraft version there
     * has ever been.
     */
    @Test
    public void testTheRowsAreDenseEnoughForEightHundredVersions() {
        int height = versions().getRowHeight();

        assertEquals(UIScale.scale(36), height, "the row height is not the density this table needs");
    }

    @Test
    public void testTheHeaderIsOnItsOwnSurface() {
        MD3Table table = versions();

        assertNotEquals(table.getBackground().getRGB(), table.getTableHeader().getBackground().getRGB(),
                "the header is the same colour as the rows, so the table reads as headless");
        assertEquals(false, table.getTableHeader().getReorderingAllowed(),
                "the columns can be dragged around, which these two tables have no use for");
    }

    /**
     * The selection has to be the Material one, since it is how you pick a version.
     */
    @Test
    public void testTheSelectionUsesTheThemesContainer() {
        MD3Table table = versions();

        assertEquals(MD3Color.secondaryContainer().getRGB(), table.getSelectionBackground().getRGB(),
                "a selected row is not on the theme's selection container");
        assertEquals(MD3Color.onSecondaryContainer().getRGB(), table.getSelectionForeground().getRGB(),
                "a selected row's text is not the colour that container calls for");
    }

    /**
     * A selected row has to look different from an unselected one - the whole point of the version
     * table is showing which version a click landed on.
     */
    @Test
    public void testASelectedRowIsDrawnDifferently() {
        MD3Table plain = versions();
        MD3Table selected = versions();
        selected.setRowSelectionInterval(1, 1);

        BufferedImage a = paint(plain, 420, 200);
        BufferedImage b = paint(selected, 420, 200);
        int differing = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        assertTrue(differing > 0, "selecting a row changed nothing on screen");
    }

    /**
     * The header is a separate component the scroll pane carries above the view, so it has to be
     * put somewhere for a test to see it - a scroll pane that was never realized never lays it out.
     */
    @Test
    public void testTheHeaderIsDrawnAboveTheRows() {
        MD3Table table = versions();
        JTableHeader header = table.getTableHeader();

        header.setColumnModel(table.getColumnModel());
        header.setSize(new Dimension(420, header.getPreferredSize().height));
        layoutTree(header);

        BufferedImage image = paint(header, 420, header.getHeight());

        assertTrue(paintedPixels(image) > 0, "the table header drew nothing, so the columns are unnamed");

        // its own surface, not the rows'
        assertNotEquals(MD3Color.surface().getRGB(), image.getRGB(image.getWidth() / 2, image.getHeight() / 2),
                "the header is drawn on the same surface as the rows");
    }

    @Test
    public void testRenderTheSheet() throws Exception {
        MD3Table table = versions();
        table.setRowSelectionInterval(1, 1);

        JScrollPane scroller = new JScrollPane(table);
        scroller.setBorder(null);
        scroller.getViewport().setBackground(MD3Table.viewportColor());

        BufferedImage image = paint(scroller, 460, 240);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/table-dark.png"));

        assertTrue(paintedPixels(image) > 0, "the sheet came out blank");
    }
}
