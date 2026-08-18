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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A list of settings, grouped into cards, one full-width row each.
 *
 * <p>
 * A setting used to be two cells of a {@link java.awt.GridBagLayout}: a right-aligned
 * "{@code Something?}" carrying a help icon, and the control beside it. That put the labels in a
 * ragged column down the middle with the explanation of each hidden in a tooltip on a 16px glyph.
 *
 * <p>
 * Each section is now a heading plus a filled card of rows. A row is what the setting is called,
 * what it does underneath, and the control on the trailing edge. Related settings share a surface,
 * so the page reads as groups rather than a long stack of floating labels.
 *
 * <p>
 * Both the settings page and the instance settings dialog are built from this.
 */
public class MD3SettingsList extends JPanel implements Scrollable {
    /**
     * How much of a row the description may take before the rest goes to the tooltip. Some of these
     * run to a paragraph, and a settings list where every row is four lines tall is one nobody can
     * scan.
     */
    private static final int SUPPORTING_LINES = 2;

    /** Room kept for the control, so a long description cannot squeeze it out. */
    private static final int CONTROL_WIDTH = 320;

    /** What a description is wrapped against before the row has been given a real width. */
    private static final int NOMINAL_TEXT_WIDTH = 520;

    private final JPanel rows = new JPanel();
    private MD3Card currentGroup;

    public MD3SettingsList() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, MD3Spacing.XL, MD3Spacing.L));

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        // NORTH rather than CENTER: the rows keep their own heights, so a section heading does not
        // stretch to absorb whatever is left over below the last row
        add(rows, BorderLayout.NORTH);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return UIScale.scale(MD3Spacing.L);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
    }

    /**
     * Takes the width of the scroll pane it is in rather than of its widest row.
     *
     * <p>
     * Without this the list is as wide as the longest thing in it - a Java path field and its two
     * buttons - and every row is laid out against that width, which puts the controls of the shorter
     * ones past the right edge of a window that cannot scroll sideways.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Drops the rows, keeping the list itself.
     */
    public void clear() {
        rows.removeAll();
        currentGroup = null;
    }

    /**
     * A heading over the rows that follow, for a list with more than one idea in it.
     *
     * @return the heading, for a caller that has to show or hide the group it introduces
     */
    public JComponent addSection(String title) {
        JLabel label = new JLabel(title);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, title));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(rows.getComponentCount() == 0 ? MD3Spacing.S : MD3Spacing.XL,
                MD3Spacing.XS, MD3Spacing.S, MD3Spacing.XS));
        stretch(label);

        rows.add(label);
        currentGroup = newGroup();
        rows.add(currentGroup);

        return label;
    }

    /**
     * A setting.
     *
     * @param label   what it is called
     * @param help    what it does, in plain text - not the HTML the tooltips used to be built from,
     *                since this is now laid out as text rather than handed to an HTML renderer. May
     *                be null for a setting whose name is the whole story
     * @param control the control, or a panel of them for a setting that takes more than one
     * @return the row, for a caller that shows or hides a setting depending on another
     */
    public Row addRow(String label, String help, JComponent control) {
        return addRow(label, help, control, false);
    }

    /**
     * A row whose control spans the width instead of sitting on the trailing edge - a table, a text
     * area, anything that would be unusable at a control's width.
     */
    public Row addWideRow(String label, String help, JComponent control) {
        return addRow(label, help, control, true);
    }

    private Row addRow(String label, String help, JComponent control, boolean wide) {
        if (currentGroup == null) {
            currentGroup = newGroup();
            rows.add(currentGroup);
        }

        Row row = new Row(label, help, control, wide);
        currentGroup.add(row);

        return row;
    }

    /**
     * Puts controls in a row, for the settings that take a field and a button to go with it.
     */
    public static JPanel group(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIScale.scale(MD3Spacing.S), 0));
        panel.setOpaque(false);

        for (Component component : components) {
            panel.add(component);
        }

        return panel;
    }

    private static MD3Card newGroup() {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(MD3Spacing.border(MD3Spacing.XS, 0));
        card.setAlignmentX(LEFT_ALIGNMENT);
        stretch(card);

        return card;
    }

    private static void stretch(JComponent component) {
        // width only: a card's height is the sum of its rows, and pinning the height at the
        // moment the empty card is built would clip every setting added after
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * One setting: its name, what it does, and the control.
     */
    public static final class Row extends JPanel {
        private final JLabel headline;
        private final JLabel supporting;
        private final String help;
        private final boolean wide;

        /** Scaled; what the description was last wrapped against. */
        private int supportingWidth = -1;

        Row(String label, String help, JComponent control, boolean wide) {
            super(new BorderLayout(UIScale.scale(MD3Spacing.L), 0));

            this.help = help;
            this.wide = wide;

            setOpaque(false);
            setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
            setAlignmentX(LEFT_ALIGNMENT);

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);

            headline = new JLabel();
            headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
            headline.setForeground(MD3Color.onSurface());
            headline.setAlignmentX(LEFT_ALIGNMENT);
            setLabel(label);
            text.add(headline);

            supporting = new JLabel();
            supporting.setOpaque(false);
            supporting.setFont(MD3Type.font(MD3Type.BODY_SMALL));
            supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
            supporting.setForeground(MD3Color.onSurfaceVariant());
            supporting.setAlignmentX(LEFT_ALIGNMENT);

            if (help != null && !help.isEmpty()) {
                // the whole explanation stays reachable even when it is longer than the row shows
                supporting.setToolTipText(help);
                headline.setToolTipText(help);

                // wrapped once against a nominal width so the row has its text - and so asks for
                // the right height - before anything has told it how wide it really is. doLayout
                // re-wraps it to the width it actually gets
                wrapSupporting(UIScale.scale(NOMINAL_TEXT_WIDTH));

                text.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
                text.add(supporting);
            }

            add(text, BorderLayout.CENTER);

            // settings carry a lot of translated text the theme's face may have no glyphs for -
            // language names most of all, since those are shown in their own language
            MD3Type.ensureCanDisplay(control);

            control.setAlignmentX(LEFT_ALIGNMENT);

            if (wide) {
                text.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));
                add(control, BorderLayout.SOUTH);
            } else {
                add(trailing(control), BorderLayout.LINE_END);
            }
        }

        /**
         * Renames the setting, for the few whose name is not known until something else has been
         * chosen - the loader version, which is Forge's or Fabric's depending on the pack.
         */
        public void setLabel(String label) {
            headline.setText(label);
            headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, label));
        }

        /**
         * Controls line up on the trailing edge, so the eye runs down one column of them rather
         * than following each label to wherever its control happened to start.
         */
        private static JComponent trailing(JComponent control) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            panel.add(control, BorderLayout.EAST);

            return panel;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            int min = UIScale.scale(help != null && !help.isEmpty()
                    ? MD3Spacing.LIST_ITEM_HEIGHT_TWO_LINE
                    : MD3Spacing.LIST_ITEM_HEIGHT_ONE_LINE);

            if (size != null) {
                size.height = Math.max(size.height, min);
            }

            return size;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = getPreferredSize();
            size.width = Integer.MAX_VALUE;

            return size;
        }

        private void wrapSupporting(int width) {
            supportingWidth = width;

            FontMetrics metrics = supporting.getFontMetrics(supporting.getFont());
            supporting.setText(MD3Text.wrapToLines(metrics, help, width, SUPPORTING_LINES));
        }

        /**
         * The description is wrapped to whatever is left after the control has taken what it needs,
         * so it re-flows with the window rather than staying at the width it was guessed at.
         */
        @Override
        public void doLayout() {
            if (help != null && !help.isEmpty()) {
                int available = getWidth() - getInsets().left - getInsets().right
                        - (wide ? 0 : UIScale.scale(CONTROL_WIDTH));

                if (available > 0 && available != supportingWidth) {
                    wrapSupporting(available);
                    revalidate();
                }
            }

            super.doLayout();
        }

        /**
         * A hairline between this row and the one above it, skipped for the first visible row so
         * the card's top edge is clean. Drawn here rather than as a sibling so hiding a row hides
         * its rule with it.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!hasVisiblePredecessor()) {
                return;
            }

            int inset = UIScale.scale(MD3Spacing.L);
            int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);

            g.setColor(MD3Color.outlineVariant());
            g.fillRect(inset, 0, Math.max(0, getWidth() - inset * 2), thickness);
        }

        private boolean hasVisiblePredecessor() {
            Container parent = getParent();

            if (parent == null) {
                return false;
            }

            Component[] children = parent.getComponents();

            for (int i = 0; i < children.length; i++) {
                if (children[i] == this) {
                    return false;
                }

                if (children[i].isVisible() && children[i] instanceof Row) {
                    return true;
                }
            }

            return false;
        }
    }
}
