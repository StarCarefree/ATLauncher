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
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A form of stacked fields, grouped into cards.
 *
 * <p>
 * The companion to {@link MD3SettingsList}, and deliberately not the same thing. A settings row is a
 * name on the leading edge and a control on the trailing one, and reserves 320dp for that control so
 * a long description cannot squeeze it out. That is right for a full-width settings page and wrong
 * for a form in a column beside something else: the export dialog's form column is 420dp all in, so
 * a settings row there leaves a sliver for the field and wraps "The name of the instance" into "The
 * n". Here the label sits <em>above</em> its control and the control takes the column's full width.
 *
 * <p>
 * Both form dialogs had grown their own copy of this - a {@code sectionCard}, a {@code stackedField},
 * a {@code switchRow}, a {@code supportingLabel} and a {@code stretch}, near enough line for line.
 */
public class MD3Form extends JPanel implements Scrollable {
    /** How many lines of explanation a row may show before the rest lives in the tooltip. */
    private static final int SUPPORTING_LINES = 2;

    /** Unscaled; what the form's controls are laid out against. */
    private final int width;

    private MD3Card currentCard;

    /**
     * @param width unscaled width of the column the form sits in, which its cards are capped to. A
     *              long path in a field would otherwise set the form's preferred width and push
     *              everything to the right of it out of the viewport
     */
    public MD3Form(int width) {
        this.width = width;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setMaximumSize(new Dimension(UIScale.scale(width), Integer.MAX_VALUE));
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
     * Takes the width of the viewport rather than of its widest card.
     *
     * <p>
     * The scroll pane is the column's full width, and the viewport inside it is that less the
     * scrollbar. Without this the cards are laid out against the outer width and the last few pixels
     * of every one of them - and of the Reset button on the trailing edge of the destination row -
     * sat behind the scrollbar or past it, in a column that cannot scroll sideways to reach them.
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
     * A heading over the card that follows.
     *
     * @return the heading, for a caller that has to show or hide the group it introduces
     */
    public JLabel addSection(String title) {
        JLabel label = new JLabel(title);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, title));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(getComponentCount() == 0 ? MD3Spacing.XS : MD3Spacing.M,
                MD3Spacing.XS, MD3Spacing.XS, MD3Spacing.XS));

        add(label);

        currentCard = newCard();
        add(currentCard);

        return label;
    }

    /**
     * A labelled control, the label stacked above it.
     *
     * @param help what the field is for; always the tooltip, and shown as a line under the label
     *             only when there is nothing else to explain it
     * @return the block, for a caller that has to enable or hide the field
     */
    public JComponent addField(String label, String help, JComponent control) {
        return addRow(field(label, help, control));
    }

    /**
     * Two labelled controls sharing a row, for the short ones that would waste a line each.
     */
    public JComponent addFields(String labelA, String helpA, JComponent a, String labelB, String helpB,
            JComponent b) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(field(labelA, helpA, a));
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        row.add(field(labelB, helpB, b));

        return addRow(row);
    }

    /** A control whose section heading is the whole label it needs. */
    public JComponent addControl(JComponent control) {
        return addRow(control);
    }

    /**
     * A label, a line of explanation, and a toggle on the trailing edge.
     *
     * @param summary one line under the label, and a msgid of its own. Null where the label says it
     *                all
     * @param help    the long version; always the tooltip, never a third line on the row
     */
    public JComponent addToggle(String label, String summary, String help, JComponent toggle) {
        int textWidth = UIScale.scale(width - MD3Spacing.XL * 3);

        MD3FittingLabel headline = new MD3FittingLabel(label, 2);
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, label));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setOverflowTip(help);
        headline.fitTo(textWidth);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(headline);

        if (summary != null && !summary.isEmpty()) {
            MD3FittingLabel supporting = MD3FittingLabel.supporting(MD3Type.BODY_SMALL, summary, SUPPORTING_LINES);
            supporting.setOverflowTip(help);
            supporting.fitTo(textWidth);

            text.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
            text.add(supporting);
        }

        JPanel row = new JPanel(new BorderLayout(UIScale.scale(MD3Spacing.M), 0));
        row.setOpaque(false);
        row.add(text, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.LINE_END);

        return addRow(row);
    }

    /** A rule between two groups of fields sharing one card. */
    public void addDivider() {
        if (currentCard == null) {
            return;
        }

        currentCard.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));

        MD3Divider divider = new MD3Divider();
        divider.setAlignmentX(LEFT_ALIGNMENT);
        currentCard.add(divider);
    }

    /**
     * Puts controls on one line, the first of them taking whatever the others do not.
     *
     * <p>
     * The first control is given a token preferred width on purpose. A text field holding an absolute
     * path asks for the width of that path, and a {@link BoxLayout} honours it - so the Browse and
     * Reset buttons beside it were pushed off the edge of a column that does not scroll sideways.
     */
    public static JPanel row(JComponent first, JComponent... rest) {
        first.setPreferredSize(new Dimension(UIScale.scale(80), first.getPreferredSize().height));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(first);

        for (JComponent component : rest) {
            row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.XS)));
            row.add(component);
        }

        return row;
    }

    /**
     * The form pinned to the top of whatever it is put in.
     *
     * <p>
     * A card takes the height it is offered, and a form dropped straight into a
     * {@link BorderLayout#CENTER} is offered all of it - so a two-field form in a tall dialog came
     * out as two cards stretched over the whole body. Needed only where the form is not already in a
     * scroll pane, which measures it at its preferred height.
     */
    public JPanel atTop() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(this, BorderLayout.NORTH);

        return panel;
    }

    /**
     * The form in a scroll pane of its own width, for a column beside something that scrolls
     * separately.
     */
    public JScrollPane inScrollPane() {
        JScrollPane scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(MD3Spacing.L));
        scroll.setPreferredSize(new Dimension(UIScale.scale(width), 0));
        scroll.setMinimumSize(new Dimension(UIScale.scale(width), 0));

        return scroll;
    }

    private JComponent addRow(JComponent row) {
        if (currentCard == null) {
            currentCard = newCard();
            add(currentCard);
        }

        if (currentCard.getComponentCount() > 0) {
            currentCard.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));
        }

        stretch(row);
        currentCard.add(row);

        return row;
    }

    private JPanel field(String label, String help, JComponent control) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);

        // plain rather than fitting: a field's label is two words, and the render tests read it back
        // through getText() to check which column it landed in
        JLabel headline = new JLabel(label);
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, label));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setAlignmentX(LEFT_ALIGNMENT);
        headline.setToolTipText(help);
        block.add(headline);

        block.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));

        control.setAlignmentX(LEFT_ALIGNMENT);
        control.setToolTipText(help);
        stretch(control);
        block.add(control);

        stretch(block);

        return block;
    }

    private MD3Card newCard() {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(UIScale.scale(width), Integer.MAX_VALUE));

        return card;
    }

    /** Full width, natural height - a row must not absorb the slack a box layout has to hand out. */
    private static void stretch(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
    }
}
