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
package com.atlauncher.gui.md3.button;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * A row of actions that keeps each button the size it asked for.
 *
 * <p>
 * Card action rows used to be a {@link java.awt.BorderLayout}: Play on the west, overflow on the
 * east. That layout stretches those two slots to the row's height, and the row's height is the
 * tallest child - so a 40dp Play beside a 48dp icon button became a 48dp Play.
 *
 * <p>
 * Leading actions sit on the start edge, trailing ones on the end. Leftover width is a gap
 * between the two groups, not extra size on the buttons.
 */
public final class MD3ButtonBar extends JPanel {
    private boolean trailingStarted;

    public MD3ButtonBar() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public MD3ButtonBar leading(JComponent... components) {
        for (JComponent component : components) {
            addSpaced(component);
        }

        revalidate();
        repaint();

        return this;
    }

    public MD3ButtonBar trailing(JComponent... components) {
        if (!trailingStarted) {
            add(Box.createHorizontalGlue());
            trailingStarted = true;
        }

        for (JComponent component : components) {
            addSpaced(component);
        }

        revalidate();
        repaint();

        return this;
    }

    private void addSpaced(JComponent component) {
        if (getComponentCount() > 0 && !lastIsGlue()) {
            add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        }

        add(component);
    }

    private boolean lastIsGlue() {
        if (getComponentCount() == 0) {
            return false;
        }

        Component last = getComponent(getComponentCount() - 1);

        return last instanceof Box.Filler && last.getPreferredSize().width == 0;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();

        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}
