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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;

import com.atlauncher.gui.md3.button.MD3ButtonUI;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3Chip}.
 *
 * <p>
 * A chip is a button with different metrics and a selected state, so the button painting is reused
 * and only the colours, shape and padding are re-answered.
 */
public class MD3ChipUI extends MD3ButtonUI {
    /** The chevron on a menu chip, and the room the border keeps clear for it. */
    private static final int TRAILING_ICON_SIZE = 18;

    public static ComponentUI createUI(JComponent c) {
        return new MD3ChipUI();
    }

    public static void installBorder(AbstractButton b) {
        b.setBorder(new ChipBorder());
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        installBorder((AbstractButton) c);
    }

    @Override
    protected MD3Type.Role typeRole() {
        return MD3Type.LABEL_LARGE;
    }

    @Override
    protected int shapeRadius() {
        return MD3Shape.CHIP;
    }

    @Override
    protected int minimumHeight() {
        return MD3Spacing.CHIP_HEIGHT;
    }

    @Override
    protected int iconSize() {
        return 18;
    }

    private static boolean isSelected(AbstractButton b) {
        return b.getModel().isSelected();
    }

    private static boolean hasMenu(Component c) {
        return c instanceof MD3Chip && ((MD3Chip) c).hasMenu();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        if (!hasMenu(c)) {
            return;
        }

        AbstractButton b = (AbstractButton) c;
        int size = UIScale.scale(TRAILING_ICON_SIZE);

        MD3Icon.of(MD3Icons.CHEVRON_DOWN, TRAILING_ICON_SIZE).withColor(contentColor(b)).paintIcon(c, g,
                c.getWidth() - UIScale.scale(MD3Spacing.M) - size, (c.getHeight() - size) / 2);
    }

    @Override
    protected Color containerColor(AbstractButton b) {
        if (!b.isEnabled()) {
            return isSelected(b) ? MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()) : null;
        }

        return isSelected(b) ? MD3Color.secondaryContainer() : null;
    }

    @Override
    protected Color contentColor(AbstractButton b) {
        if (!b.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        return isSelected(b) ? MD3Color.onSecondaryContainer() : MD3Color.onSurfaceVariant();
    }

    @Override
    protected Color outlineColor(AbstractButton b) {
        // a selected chip is identified by its fill, and keeping the outline as well would make it
        // read as two nested shapes
        if (isSelected(b)) {
            return null;
        }

        if (!b.isEnabled()) {
            return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        return b.isFocusOwner() ? MD3Color.primary() : MD3Color.outline();
    }

    private static class ChipBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            boolean hasIcon = c instanceof AbstractButton && ((AbstractButton) c).getIcon() != null;
            int leading = hasIcon ? MD3Spacing.S : MD3Spacing.L;

            // a menu chip's chevron is painted rather than laid out, so the label has to be kept
            // out of the strip it occupies
            int trailing = hasMenu(c) ? MD3Spacing.M + TRAILING_ICON_SIZE + MD3Spacing.XS : MD3Spacing.L;

            insets.set(UIScale.scale(MD3Spacing.XS), UIScale.scale(leading), UIScale.scale(MD3Spacing.XS),
                    UIScale.scale(trailing));

            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0, 0, 0, 0));
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
