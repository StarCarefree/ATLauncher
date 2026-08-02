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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 data table.
 *
 * <p>
 * The differences from a stock {@link JTable} are all Material's: rows separated by a rule rather
 * than boxed in a grid, no vertical lines at all, a header on its own surface, and a row that
 * answers the pointer. Swing's default is a spreadsheet; Material's is a list with columns, which is
 * what both of the launcher's tables actually are.
 *
 * <p>
 * <b>36dp rows, not the 52dp of Material's standard density.</b> The create-pack table lists every
 * Minecraft version there has ever been - some eight hundred - and at 52dp that is a scrollbar the
 * height of a pixel. Material allows 36 through 64 for exactly this reason.
 *
 * <p>
 * Extends {@link JTable}, so the models, selection listeners, column widths and cell editors at the
 * call sites are untouched.
 */
public class MD3Table extends JTable {
    private static final int ROW_HEIGHT = 36;
    private static final int HEADER_HEIGHT = 44;

    /** The row the pointer is on, or -1. Selection wins over it. */
    private int hoveredRow = -1;

    public MD3Table(TableModel model) {
        super(model);

        setRowHeight(UIScale.scale(ROW_HEIGHT));
        setShowGrid(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setBackground(MD3Color.surface());
        setForeground(MD3Color.onSurface());
        setSelectionBackground(MD3Color.secondaryContainer());
        setSelectionForeground(MD3Color.onSecondaryContainer());
        setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);

        setDefaultRenderer(Object.class, new CellRenderer());
        setDefaultEditor(Object.class, new CellEditor());

        installHeader();
        installHoverTracking();
    }

    private void installHeader() {
        JTableHeader header = getTableHeader();

        if (header == null) {
            return;
        }

        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setBackground(MD3Color.surfaceContainerLow());
        header.setForeground(MD3Color.onSurfaceVariant());
        header.setBorder(null);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, UIScale.scale(HEADER_HEIGHT)));
    }

    /**
     * A row lights under the pointer, which is what tells a list of eight hundred versions that the
     * one your eye is on is the one a click would take.
     */
    private void installHoverTracking() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoveredRow(rowAtPoint(e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                setHoveredRow(rowAtPoint(e.getPoint()));
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredRow(-1);
            }
        });
    }

    private void setHoveredRow(int row) {
        if (hoveredRow == row) {
            return;
        }

        int previous = hoveredRow;
        hoveredRow = row;

        repaintRow(previous);
        repaintRow(row);
    }

    private void repaintRow(int row) {
        if (row >= 0 && row < getRowCount()) {
            repaint(0, row * getRowHeight(), getWidth(), getRowHeight());
        }
    }

    /**
     * Lays the hover state over whatever the renderer decided, so a custom renderer at a call site
     * still gets the behaviour without knowing about it.
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);

        if (row == hoveredRow && !isRowSelected(row) && component != null) {
            component.setBackground(MD3Color.blend(MD3Color.surface(), MD3Color.onSurface(), MD3State.HOVER));
        }

        return component;
    }

    /** The rule under each row, which is the only line a Material table draws. */
    private static Border rowBorder(boolean last) {
        int padding = MD3Spacing.scale(MD3Spacing.L);
        Border inner = BorderFactory.createEmptyBorder(0, padding, 0, padding);

        if (last) {
            return inner;
        }

        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, UIScale.scale(1), 0, MD3Color.outlineVariant()), inner);
    }

    /**
     * A cell: the value, on the type scale, padded to the same gutter the header uses.
     */
    private final class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);

            String text = value == null ? "" : value.toString();

            // version strings, environment variable values - none of it written by the launcher
            setFont(MD3Type.font(MD3Type.BODY_MEDIUM, text));
            setBorder(rowBorder(row == table.getRowCount() - 1));

            if (!selected) {
                setBackground(MD3Color.surface());
                setForeground(MD3Color.onSurface());
            }

            return this;
        }
    }

    /**
     * A column heading. Its own surface and its own weight, so the table reads as headed rather
     * than as having one bold row.
     */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);

            String text = value == null ? "" : value.toString();

            setFont(MD3Type.font(MD3Type.LABEL_LARGE, text));
            setForeground(MD3Color.onSurfaceVariant());
            setBackground(MD3Color.surfaceContainerLow());
            setOpaque(true);
            setHorizontalAlignment(JLabel.LEADING);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, UIScale.scale(1), 0, MD3Color.outlineVariant()),
                    BorderFactory.createEmptyBorder(0, MD3Spacing.scale(MD3Spacing.L), 0,
                            MD3Spacing.scale(MD3Spacing.L))));

            return this;
        }
    }

    /**
     * The editor for an editable cell - the environment variables table, which is edited in place.
     *
     * <p>
     * Not an {@code MD3TextField}: that paints its own container, and inside a cell that already has
     * one there would be two. This is the plain field with the table's own colours and the same
     * gutter, so editing looks like the row it replaces rather than like a box dropped onto it.
     */
    private static final class CellEditor extends DefaultCellEditor {
        CellEditor() {
            super(new JTextField());

            JTextField field = (JTextField) getComponent();

            field.setBorder(BorderFactory.createEmptyBorder(0, MD3Spacing.scale(MD3Spacing.L), 0,
                    MD3Spacing.scale(MD3Spacing.L)));
            field.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
            field.setBackground(MD3Color.surfaceContainerHighest());
            field.setForeground(MD3Color.onSurface());
            field.setCaretColor(MD3Color.primary());
            field.setSelectionColor(MD3Color.secondaryContainer());
            field.setSelectedTextColor(MD3Color.onSecondaryContainer());
        }
    }

    /**
     * @return the colour a caller should give the scroll pane's viewport, so the space below the
     *         last row is the table's surface and not the look and feel's panel colour
     */
    public static Color viewportColor() {
        return MD3Color.surface();
    }
}
