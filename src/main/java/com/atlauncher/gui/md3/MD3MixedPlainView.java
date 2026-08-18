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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

/**
 * Field and area views that paint each script with the face Settings named for it.
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

    private static final class MixedFieldView extends FieldView {
        MixedFieldView(Element elem) {
            super(elem);
        }

        @Override
        protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, unselected(this, g));
        }

        @Override
        protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
            return drawRange(g, this, x, y, p0, p1, selected(this, g));
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
