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

import javax.swing.JMenuItem;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A menu item on Material's type scale and 48dp target, so overflow menus match the rest of the
 * launcher rather than FlatLaf's tighter default.
 */
public class MD3MenuItem extends JMenuItem {
    public MD3MenuItem() {
        this(null);
    }

    public MD3MenuItem(String text) {
        super(text);

        applyTokens();
    }

    private void applyTokens() {
        setFont(MD3Type.font(MD3Type.LABEL_LARGE, getText()));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_LARGE);
        setOpaque(true);
        setBackground(MD3Color.surfaceContainerHigh());
        setForeground(MD3Color.onSurface());
        setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.M));
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (MD3Type.LABEL_LARGE != null) {
            applyTokens();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        if (size != null) {
            size.height = Math.max(size.height, UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET));
        }

        return size;
    }
}
