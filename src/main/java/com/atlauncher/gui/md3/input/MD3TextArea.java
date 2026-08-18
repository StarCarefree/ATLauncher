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
package com.atlauncher.gui.md3.input;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 multiline field.
 *
 * <p>
 * Still a {@link JTextArea}, so document filters, carets and the existing
 * {@code getText}/{@code setText} wiring keep working. The decoration lives on
 * {@link #contained(int)} so a scroller can sit inside the outline rather than drawing FlatLaf's
 * square border around it.
 */
public class MD3TextArea extends JTextArea {
    public MD3TextArea(int rows, int columns) {
        super(rows, columns);

        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.M));
        setFont(MD3Type.font(MD3Type.BODY_LARGE));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        setForeground(MD3Color.onSurface());
        setCaretColor(MD3Color.primary());
        setSelectionColor(MD3Color.secondaryContainer());
        setSelectedTextColor(MD3Color.onSecondaryContainer());
        setDisabledTextColor(MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface()));
    }

    @Override
    public void updateUI() {
        setUI(new MD3TextAreaUI());
    }

    /**
     * The field inside a rounded container of a given height, ready to drop into a settings row.
     *
     * @param heightDp unscaled height of the whole control, including the outline
     */
    public JComponent contained(int heightDp) {
        JScrollPane scroller = new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(MD3Spacing.scale(MD3Spacing.L));

        JPanel wrap = new Container(this);
        wrap.setLayout(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(getPreferredSize().width, UIScale.scale(heightDp)));
        wrap.add(scroller, BorderLayout.CENTER);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrap.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrap.repaint();
            }
        });

        return wrap;
    }

    private static final class Container extends JPanel {
        private final MD3TextArea area;

        Container(MD3TextArea area) {
            this.area = area;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                Shape shape = MD3Paint.shapeOf(this, MD3Shape.TEXT_FIELD);
                Color fill = area.isEnabled() ? MD3Color.surfaceContainerHighest()
                        : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
                MD3Paint.fill(g2, shape, fill);

                Color line;
                float width;

                if (!area.isEnabled()) {
                    line = MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
                    width = 1f;
                } else if (area.isFocusOwner()) {
                    line = MD3Color.primary();
                    width = 2f;
                } else {
                    line = MD3Color.outline();
                    width = 1f;
                }

                MD3Paint.outline(g2, shape, line, width);
            } finally {
                g2.dispose();
            }

            super.paintComponent(g);
        }
    }
}
