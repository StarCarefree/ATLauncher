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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

/**
 * Field and area views that paint each script with the face Settings named for it.
 *
 * <p>
 * The {@code int} draw methods are Java 8; the {@code Graphics2D} ones are what a modern JRE
 * actually calls. Both have to paint mixed, or a search box on Java 11+ would go back to one
 * face. The field also reports mixed width so the caret lands on the glyph that was drawn.
 */
public final class MD3MixedPlainView {
    private MD3MixedPlainView() {
    }

    public static View field(Element elem) {
        return new MixedFieldView(elem);
    }

    public static View wrapped(Element elem) {
        return new MixedWrappedView(elem);
    }

    private static int drawRange(Graphics g, View view, int x, int y, int p0, int p1, Color color)
            throws BadLocationException {
        String text = view.getDocument().getText(p0, p1 - p0);
        JTextComponent host = (JTextComponent) view.getContainer();
        Font font = host == null ? g.getFont() : host.getFont();

        g.setColor(color);

        return x + MD3MixedText.draw((Graphics2D) g, text, x, y, font);
    }

    private static Font hostFont(View view) {
        JTextComponent host = (JTextComponent) view.getContainer();

        if (host != null && host.getFont() != null) {
            return host.getFont();
        }

        return view.getContainer() != null ? view.getContainer().getFont() : null;
    }

    private static int mixedWidth(View view, int p0, int p1) throws BadLocationException {
        Font font = hostFont(view);

        if (font == null || p1 <= p0) {
            return 0;
        }

        return MD3MixedText.width(font, view.getDocument().getText(p0, p1 - p0));
    }

    private static final class MixedFieldView extends FieldView {
        MixedFieldView(Element elem) {
            super(elem);
        }

        @Override
        public float getPreferredSpan(int axis) {
            if (axis == View.X_AXIS) {
                try {
                    return mixedWidth(this, getStartOffset(), getEndOffset());
                } catch (BadLocationException e) {
                    return 0;
                }
            }

            return super.getPreferredSpan(axis);
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            int start = getStartOffset();
            int end = getEndOffset();

            if (pos < start || pos > end) {
                throw new BadLocationException("modelToView", pos);
            }

            Shape allocated = adjustAllocation(a);
            Rectangle bounds = allocated instanceof Rectangle ? (Rectangle) allocated : allocated.getBounds();
            int x = mixedWidth(this, start, pos);

            return new Rectangle(bounds.x + x, bounds.y, 1, bounds.height);
        }

        @Override
        public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
            Shape allocated = adjustAllocation(a);
            Rectangle bounds = allocated instanceof Rectangle ? (Rectangle) allocated : allocated.getBounds();
            int x = Math.round(fx) - bounds.x;
            int start = getStartOffset();
            int end = getEndOffset();

            if (bias != null) {
                bias[0] = Position.Bias.Forward;
            }

            if (x <= 0) {
                return start;
            }

            try {
                String text = getDocument().getText(start, end - start);
                Font font = hostFont(this);

                if (font == null || text.isEmpty()) {
                    return start;
                }

                MD3MixedText.Layout layout = MD3MixedText.layout(font, text);

                if (x >= layout.width()) {
                    return end;
                }

                int lo = 0;
                int hi = text.length();

                while (lo < hi) {
                    int mid = (lo + hi + 1) >>> 1;

                    if (mid > 0 && mid < text.length() && Character.isLowSurrogate(text.charAt(mid))) {
                        mid--;
                    }

                    if (mid <= lo) {
                        break;
                    }

                    if (layout.width(0, mid) <= x) {
                        lo = mid;
                    } else {
                        hi = mid - 1;

                        if (hi > 0 && hi < text.length() && Character.isLowSurrogate(text.charAt(hi))) {
                            hi--;
                        }
                    }
                }

                return start + lo;
            } catch (BadLocationException e) {
                return start;
            }
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, unselected(this, g));
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, selected(this, g));
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return drawRange(g, this, Math.round(x), Math.round(y), p0, p1, unselected(this, g));
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return drawRange(g, this, Math.round(x), Math.round(y), p0, p1, selected(this, g));
        }
    }

    private static final class MixedWrappedView extends WrappedPlainView {
        MixedWrappedView(Element elem) {
            super(elem, true);
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, unselected(this, g));
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, selected(this, g));
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return drawRange(g, this, Math.round(x), Math.round(y), p0, p1, unselected(this, g));
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return drawRange(g, this, Math.round(x), Math.round(y), p0, p1, selected(this, g));
        }
    }

    private static Color unselected(View view, Graphics g) {
        JTextComponent host = (JTextComponent) view.getContainer();

        if (host == null) {
            return g.getColor();
        }

        return host.isEnabled() ? host.getForeground() : host.getDisabledTextColor();
    }

    private static Color selected(View view, Graphics g) {
        JTextComponent host = (JTextComponent) view.getContainer();

        return host == null ? g.getColor() : host.getSelectedTextColor();
    }
}
