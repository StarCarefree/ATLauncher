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

    /**
     * How large the target is. Medium is Material's 40dp default.
     *
     * <pre>
     * SMALL   32dp - a dense toolbar, or a trailing control on a list item
     * MEDIUM  40dp - the default, and the size an app-bar action should stay
     * LARGE   48dp - a sparse header, or an action that has to be easy to hit
     * </pre>
     */
    public enum Size {
        SMALL, MEDIUM, LARGE
    }

    private Variant variant;
    private Size buttonSize;

    public MD3IconButton(MD3Icon.Painter painter, String tooltip) {
        this(MD3Icon.of(painter), tooltip, Variant.STANDARD);
    }

    public MD3IconButton(MD3Icon.Painter painter, String tooltip, Variant variant) {
        this(MD3Icon.of(painter), tooltip, variant);
    }

    public MD3IconButton(Icon icon, String tooltip, Variant variant) {
        this(icon, tooltip, variant, Size.MEDIUM);
    }

    public MD3IconButton(MD3Icon.Painter painter, String tooltip, Variant variant, Size size) {
        this(MD3Icon.of(painter), tooltip, variant, size);
    }

    public MD3IconButton(Icon icon, String tooltip, Variant variant, Size size) {
        super(icon);

        this.variant = variant;
        this.buttonSize = size;

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

    /**
     * Named {@code getButtonSize} rather than {@code getSize} so it does not hide
     * {@link java.awt.Component#getSize()}.
     */
    public Size getButtonSize() {
        return buttonSize != null ? buttonSize : Size.MEDIUM;
    }

    public void setButtonSize(Size buttonSize) {
        Size old = this.buttonSize;
        this.buttonSize = buttonSize;

        firePropertyChange("buttonSize", old, buttonSize);
        revalidate();
        repaint();
    }

    public MD3IconButton withButtonSize(Size buttonSize) {
        setButtonSize(buttonSize);

        return this;
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
