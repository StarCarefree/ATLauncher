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

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * A popup on a container surface, so overflow menus sit on the same elevation as the rest of the
 * Material chrome rather than on the page colour.
 */
public class MD3PopupMenu extends JPopupMenu {
    public MD3PopupMenu() {
        applyTokens();
    }

    private void applyTokens() {
        setBackground(MD3Color.surfaceContainerHigh());
        setForeground(MD3Color.onSurface());
        setBorder(MD3Spacing.border(MD3Spacing.XS, 0));
        setLightWeightPopupEnabled(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyTokens();
    }

    @Override
    public JMenuItem add(String s) {
        MD3MenuItem item = new MD3MenuItem(s);
        add(item);

        return item;
    }
}
