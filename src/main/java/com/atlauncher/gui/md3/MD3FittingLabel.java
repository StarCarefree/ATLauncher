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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A label that keeps its whole string reachable: it wraps to a line cap and ellipsises the last
 * line rather than clipping mid-glyph when the card it sits on is narrower than the text.
 */
public class MD3FittingLabel extends JLabel {
    private String fullText = "";
    private String overflowTip;
    private int maxLines = 1;
    private int fittedWidth = -1;

    public MD3FittingLabel(String text, int maxLines) {
        this.maxLines = Math.max(1, maxLines);

        setAlignmentX(LEFT_ALIGNMENT);
        setVerticalAlignment(SwingConstants.TOP);
        setFullText(text);
    }

    public void setFullText(String text) {
        fullText = text == null ? "" : text;
        fittedWidth = -1;
        fitTo(availableWidth());
    }

    public String getFullText() {
        return fullText;
    }

    /**
     * Shown whenever the visible text is shorter than {@link #getFullText()}, or always when set
     * - instance cards keep a formatted title in the tip even when the name itself fitted.
     */
    public void setOverflowTip(String tip) {
        overflowTip = tip;
        applyTip();
    }

    public void fitTo(int width) {
        int inner = Math.max(0, width);

        if (inner <= 0 || inner == fittedWidth) {
            return;
        }

        fittedWidth = inner;

        String shown = maxLines == 1
                ? MD3MixedText.fitToWidth(getFont(), fullText, inner)
                : MD3Text.wrapToLines(getFontMetrics(getFont()), fullText, inner, maxLines);

        if (!shown.equals(getText())) {
            super.setText(shown);
        }

        applyTip();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        fittedWidth = -1;
        fitTo(availableWidth());
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        fitTo(w - horizontalInsets());
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        int width = fittedWidth > 0 ? fittedWidth : Math.max(0, getWidth() - horizontalInsets());

        if (width <= 0) {
            return super.getPreferredSize();
        }

        FontMetrics metrics = getFontMetrics(getFont());
        List<String> lines;

        if (maxLines == 1) {
            lines = MD3MixedText.plainLines(MD3MixedText.fitToWidth(getFont(), fullText, width));
        } else {
            lines = MD3MixedText.displayLines(getFont(),
                    MD3Text.wrapToLines(metrics, fullText, width, maxLines), width);
        }

        Dimension block = MD3MixedText.blockSize(getFont(), lines);

        return new Dimension(width + insets.left + insets.right, block.height + insets.top + insets.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();

        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private void applyTip() {
        if (overflowTip != null) {
            setToolTipText(overflowTip);

            return;
        }

        String shown = MD3Text.plain(getText());

        setToolTipText(fullText.equals(shown) ? null : fullText);
    }

    private int availableWidth() {
        int width = getWidth();

        return width > 0 ? width - horizontalInsets() : 0;
    }

    private int horizontalInsets() {
        Insets insets = getInsets();

        return insets.left + insets.right;
    }
}
