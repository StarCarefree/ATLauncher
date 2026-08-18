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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatLabelUI;

/**
 * A label that draws English and Chinese with the faces Settings named for each.
 *
 * <p>
 * Swing's HTML view is the reason mixed strings used to ignore the split: wrapping a description
 * installs {@code BasicHTML}, and that view paints the whole block with the label's one font.
 * {@code paintEnabledText} never runs. Simple HTML - the wrap the launcher emits - is painted here
 * as mixed lines instead.
 */
public class MD3LabelUI extends FlatLabelUI {
    public MD3LabelUI() {
        super(false);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MD3LabelUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JLabel label = (JLabel) c;
        String text = label.getText();

        if (keepFace(label) || label.getIcon() != null || !MD3MixedText.isSimpleHtml(text)) {
            super.paint(g, c);

            return;
        }

        paintMixedHtml(g, label, text);
    }

    @Override
    protected void paintEnabledText(JLabel label, Graphics g, String text, int textX, int textY) {
        if (keepFace(label)) {
            super.paintEnabledText(label, g, text, textX, textY);

            return;
        }

        g.setColor(label.getForeground());
        paintPlainOrMultiline((Graphics2D) g, label, text, textX, textY);
    }

    @Override
    protected void paintDisabledText(JLabel label, Graphics g, String text, int textX, int textY) {
        if (keepFace(label)) {
            super.paintDisabledText(label, g, text, textX, textY);

            return;
        }

        g.setColor(disabledColor(label));
        paintPlainOrMultiline((Graphics2D) g, label, text, textX, textY);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (!(c instanceof JLabel)) {
            return super.getPreferredSize(c);
        }

        JLabel label = (JLabel) c;
        String text = label.getText();

        if (keepFace(label) || label.getIcon() != null || text == null) {
            return super.getPreferredSize(c);
        }

        if (MD3MixedText.isSimpleHtml(text) || text.indexOf('\n') >= 0) {
            return mixedPreferredSize(label, text);
        }

        Dimension size = super.getPreferredSize(c);

        if (size == null) {
            return size;
        }

        FontMetrics metrics = label.getFontMetrics(label.getFont());
        int mixed = MD3MixedText.width(label.getFont(), text);
        int single = metrics.stringWidth(text);

        size.width += Math.max(0, mixed - single);

        return size;
    }

    private static void paintMixedHtml(Graphics g, JLabel label, String text) {
        if (label.isOpaque()) {
            g.setColor(label.getBackground());
            g.fillRect(0, 0, label.getWidth(), label.getHeight());
        }

        Insets insets = label.getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = Math.max(0, label.getWidth() - insets.left - insets.right);
        int height = Math.max(0, label.getHeight() - insets.top - insets.bottom);
        Font font = label.getFont();
        int wrap = MD3MixedText.cssPixelWidth(text);

        if (wrap <= 0) {
            wrap = width;
        }

        List<String> lines = MD3MixedText.displayLines(font, text, wrap);
        Dimension block = MD3MixedText.blockSize(font, lines);
        int top = y + verticalOffset(label.getVerticalAlignment(), height, block.height);

        g.setColor(label.isEnabled() ? label.getForeground() : disabledColor(label));
        MD3MixedText.drawLines((Graphics2D) g, font, lines, x, top, width, label.getHorizontalAlignment(),
                label.getComponentOrientation().isLeftToRight());
    }

    private static void paintPlainOrMultiline(Graphics2D g, JLabel label, String text, int textX, int textY) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (text.indexOf('\n') < 0) {
            MD3MixedText.draw(g, text, textX, textY, label.getFont());

            return;
        }

        List<String> lines = MD3MixedText.plainLines(text);
        FontMetrics metrics = label.getFontMetrics(label.getFont());
        int top = textY - metrics.getAscent();

        MD3MixedText.drawLines(g, label.getFont(), lines, textX, top, 0, label.getHorizontalAlignment(),
                label.getComponentOrientation().isLeftToRight());
    }

    private static Dimension mixedPreferredSize(JLabel label, String text) {
        Insets insets = label.getInsets();
        Font font = label.getFont();
        int wrap = MD3MixedText.cssPixelWidth(text);

        if (wrap <= 0 && label.getWidth() > insets.left + insets.right) {
            wrap = label.getWidth() - insets.left - insets.right;
        }

        List<String> lines = MD3MixedText.displayLines(font, text, wrap);
        Dimension block = MD3MixedText.blockSize(font, lines);

        if (wrap > 0) {
            block.width = Math.max(block.width, wrap);
        }

        block.width += insets.left + insets.right;
        block.height += insets.top + insets.bottom;

        return block;
    }

    private static int verticalOffset(int alignment, int available, int block) {
        if (alignment == javax.swing.SwingConstants.BOTTOM) {
            return Math.max(0, available - block);
        }

        if (alignment == javax.swing.SwingConstants.CENTER) {
            return Math.max(0, available - block) / 2;
        }

        return 0;
    }

    private static Color disabledColor(JLabel label) {
        Color color = UIManager.getColor("Label.disabledForeground");

        return color != null ? color : label.getBackground().brighter();
    }

    private static boolean keepFace(JLabel label) {
        return Boolean.TRUE.equals(label.getClientProperty(MD3MixedText.KEEP_FACE_KEY));
    }
}
