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

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;

import com.atlauncher.gui.md3.icon.MD3Icon;

/**
 * A Material 3 icon button - a 40dp circular target holding a single glyph.
 *
 * <p>
 * Always give one a tooltip. An icon alone is a guess for anyone who has not met it before, and the
 * tooltip is also what a screen reader reads out.
 */
public class MD3IconButton extends JButton {
    public static final String UI_CLASS_ID = "MD3IconButtonUI";

    public enum Variant {
        /** No container. The default, and the right choice inside a toolbar or app bar. */
        STANDARD,
        /** Filled with primary. For the one icon action that matters most in its context. */
        FILLED,
        /** Filled with a secondary container. A strong but not primary action. */
        TONAL,
        /** Outlined, no fill. Use where the target needs edges to be findable. */
        OUTLINED
    }

    private Variant variant;

    public MD3IconButton(MD3Icon.Painter painter, String tooltip) {
        this(MD3Icon.of(painter), tooltip, Variant.STANDARD);
    }

    public MD3IconButton(MD3Icon.Painter painter, String tooltip, Variant variant) {
        this(MD3Icon.of(painter), tooltip, variant);
    }

    public MD3IconButton(Icon icon, String tooltip, Variant variant) {
        super(icon);

        this.variant = variant;

        setToolTipText(tooltip);
        getAccessibleContext().setAccessibleName(tooltip);

        updateUI();
    }

    public Variant getVariant() {
        return variant != null ? variant : Variant.STANDARD;
    }

    public void setVariant(Variant variant) {
        Variant old = this.variant;
        this.variant = variant;

        firePropertyChange("variant", old, variant);
        repaint();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3IconButtonUI.class.getName());
        }

        setUI((ButtonUI) UIManager.getUI(this));
    }
}
