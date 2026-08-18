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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * A scrolling list in a Material 3 container - the rounded, low-emphasis surface that a group of
 * rows sits in, rather than a bare {@link JScrollPane} squared off against the page.
 *
 * <p>
 * The lists this replaces announced their boundaries with the look and feel's default scroll pane
 * border, a hard-edged one pixel outline drawn for a desktop twenty years ago. Material draws the
 * same boundary with tone and a corner radius: the container fills at
 * {@code surfaceContainerLow} and carries a hairline of {@code outlineVariant} for the themes
 * where tone alone is not enough.
 *
 * <p>
 * The wrapped component keeps everything that was attached to it - transfer handlers, listeners,
 * layout - because it is only ever re-parented, never replaced. The scroll pane around it is
 * borderless and transparent, so the container is the only thing drawing an edge.
 */
public class MD3ListContainer extends JPanel {
    private final JScrollPane scroller;

    public MD3ListContainer(JComponent content) {
        super(new BorderLayout());

        setOpaque(false);
        // at least the corner radius, so a row cannot paint over the curve and look clipped
        setBorder(MD3Spacing.border(MD3Shape.MEDIUM));

        scroller = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(MD3Spacing.scale(MD3Spacing.L));

        add(scroller, BorderLayout.CENTER);
    }

    public static MD3ListContainer wrapping(JComponent content) {
        return new MD3ListContainer(content);
    }

    public JScrollPane getScrollPane() {
        return scroller;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Shape shape = MD3Paint.shapeOf(this, MD3Shape.MEDIUM);

            MD3Paint.fill(g2, shape, MD3Color.surfaceContainerLow());
            MD3Paint.outline(g2, shape, MD3Color.outlineVariant(), 1f);
        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }
}
