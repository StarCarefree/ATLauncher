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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatLabelUI;

/**
 * A label that draws English and Chinese with the faces Settings named for each, instead of
 * swapping the whole string onto whichever face can draw the rarer script.
 */
public class MD3LabelUI extends FlatLabelUI {
    public MD3LabelUI() {
        super(false);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MD3LabelUI();
    }

    @Override
    protected void paintEnabledText(JLabel label, Graphics g, String text, int textX, int textY) {
        if (isHtml(text) || keepFace(label)) {
            super.paintEnabledText(label, g, text, textX, textY);

            return;
        }

        g.setColor(label.getForeground());
        MD3MixedText.draw((Graphics2D) g, text, textX, textY, label.getFont());
    }

    @Override
    protected void paintDisabledText(JLabel label, Graphics g, String text, int textX, int textY) {
        if (isHtml(text) || keepFace(label)) {
            super.paintDisabledText(label, g, text, textX, textY);

            return;
        }

        g.setColor(label.getBackground().brighter());
        MD3MixedText.draw((Graphics2D) g, text, textX, textY, label.getFont());
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);

        if (!(c instanceof JLabel) || size == null) {
            return size;
        }

        JLabel label = (JLabel) c;
        String text = label.getText();

        if (text == null || isHtml(text) || label.getIcon() != null) {
            return size;
        }

        FontMetrics metrics = label.getFontMetrics(label.getFont());
        int mixed = MD3MixedText.width(label.getFont(), text);
        int single = metrics.stringWidth(text);

        size.width += Math.max(0, mixed - single);

        return size;
    }

    private static boolean isHtml(String text) {
        return text != null && text.regionMatches(true, 0, "<html>", 0, 6);
    }

    private static boolean keepFace(JLabel label) {
        return Boolean.TRUE.equals(label.getClientProperty(MD3MixedText.KEEP_FACE_KEY));
    }
}
