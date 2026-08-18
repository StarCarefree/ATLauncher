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

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.atlauncher.gui.md3.MD3MixedText;
import com.formdev.flatlaf.ui.FlatCheckBoxUI;

/**
 * A checkbox whose label keeps the English face on Latin and the Chinese face on CJK.
 */
public class MD3MixedCheckBoxUI extends FlatCheckBoxUI {
    public MD3MixedCheckBoxUI() {
        super(false);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MD3MixedCheckBoxUI();
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        MD3MixedText.paintButtonText(g, b, textRect, text, disabledText);
    }
}
