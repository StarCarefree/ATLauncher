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
package com.atlauncher.gui.md3.feedback;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.View;

import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * Paints tooltips as an inverted surface, the same treatment snackbars get.
 *
 * <p>
 * FlatLaf's default tooltip is a hard rectangle with a one-pixel border. Material's is a rounded
 * slab in {@code inverseSurface}, so a tip over a dark window does not disappear into it.
 */
public class MD3TooltipUI extends BasicToolTipUI {
    public static ComponentUI createUI(JComponent c) {
        return new MD3TooltipUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
        c.setBorder(MD3Spacing.border(MD3Spacing.XS, MD3Spacing.S));
        c.setFont(MD3Type.font(MD3Type.BODY_SMALL));
        c.setForeground(MD3Color.inverseOnSurface());
        c.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Shape shape = MD3Paint.shapeOf(c, MD3Shape.TOOLTIP);
            MD3Paint.fill(g2, shape, MD3Color.inverseSurface());

            String text = c instanceof JToolTip ? ((JToolTip) c).getTipText() : null;

            if (text == null || text.isEmpty()) {
                return;
            }

            Insets insets = c.getInsets();
            Rectangle area = new Rectangle(insets.left, insets.top,
                    c.getWidth() - insets.left - insets.right, c.getHeight() - insets.top - insets.bottom);

            g2.setColor(MD3Color.inverseOnSurface());

            View view = (View) c.getClientProperty(BasicHTML.propertyKey);

            if (view != null) {
                view.paint(g2, area);
            } else {
                FontMetrics metrics = c.getFontMetrics(c.getFont());
                MD3MixedText.draw(g2, text, area.x, area.y + metrics.getAscent(), c.getFont());
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);

        if (size != null) {
            size.width = Math.max(size.width, 1);
            size.height = Math.max(size.height, 1);
        }

        if (!(c instanceof JToolTip) || size == null) {
            return size;
        }

        String text = ((JToolTip) c).getTipText();

        if (text == null || text.regionMatches(true, 0, "<html>", 0, 6)) {
            return size;
        }

        FontMetrics metrics = c.getFontMetrics(c.getFont());
        int mixed = MD3MixedText.width(c.getFont(), text);
        int single = metrics.stringWidth(text);

        size.width += Math.max(0, mixed - single);

        return size;
    }
}
