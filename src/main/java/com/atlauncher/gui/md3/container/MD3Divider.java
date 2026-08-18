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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * A one pixel rule in the outline variant colour.
 *
 * <p>
 * Material uses dividers sparingly - whitespace and surface colour separate most things perfectly
 * well, and a list where every row is ruled reads as a table. Reach for one when items belong to
 * genuinely different groups.
 *
 * <p>
 * The inset form aligns the rule with the text column rather than the container edge, so a list
 * with leading icons keeps a clean vertical edge down its text.
 */
public class MD3Divider extends JComponent {
    private final int orientation;
    private int leadingInset;
    private int trailingInset;

    public MD3Divider() {
        this(SwingConstants.HORIZONTAL);
    }

    public MD3Divider(int orientation) {
        this.orientation = orientation;

        setOpaque(false);
    }

    /**
     * A divider indented to line up with the text of a list item that has a leading icon.
     */
    public static MD3Divider inset() {
        MD3Divider divider = new MD3Divider(SwingConstants.HORIZONTAL);
        divider.setInsets(MD3Spacing.L + MD3Spacing.LIST_LEADING_COLUMN, 0);

        return divider;
    }

    /**
     * A divider inset equally at both ends, for separating sections within a card.
     */
    public static MD3Divider middle() {
        MD3Divider divider = new MD3Divider(SwingConstants.HORIZONTAL);
        divider.setInsets(MD3Spacing.L, MD3Spacing.L);

        return divider;
    }

    /**
     * @param leading  unscaled inset at the left, or top when vertical
     * @param trailing unscaled inset at the right, or bottom when vertical
     */
    public void setInsets(int leading, int trailing) {
        this.leadingInset = leading;
        this.trailingInset = trailing;

        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);

        return orientation == SwingConstants.HORIZONTAL ? new Dimension(0, thickness)
                : new Dimension(thickness, 0);
    }

    @Override
    public Dimension getMaximumSize() {
        int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);

        return orientation == SwingConstants.HORIZONTAL ? new Dimension(Integer.MAX_VALUE, thickness)
                : new Dimension(thickness, Integer.MAX_VALUE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);
        int leading = UIScale.scale(leadingInset);
        int trailing = UIScale.scale(trailingInset);

        g.setColor(MD3Color.outlineVariant());

        if (orientation == SwingConstants.HORIZONTAL) {
            g.fillRect(leading, 0, Math.max(0, getWidth() - leading - trailing), thickness);
        } else {
            g.fillRect(0, leading, thickness, Math.max(0, getHeight() - leading - trailing));
        }
    }
}
