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
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;

import com.atlauncher.gui.md3.button.MD3ButtonUI;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
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
 *
 * <p>
 * Selecting one fades its container up and its outline away rather than swapping both at once. A row
 * of filter chips is usually exclusive, so two of them change at the same moment, and switching them
 * instantly makes the row read as having been rebuilt rather than as one choice having moved.
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

    /**
     * A chip is already only 8dp round, so it squares off almost completely under the finger.
     */
    @Override
    protected int pressedRadius() {
        return MD3Shape.EXTRA_SMALL;
    }

    @Override
    protected int minimumHeight() {
        return MD3Spacing.MIN_TOUCH_TARGET;
    }

    @Override
    protected float containerInset(JComponent c) {
        return Math.max(0f, (c.getHeight() - UIScale.scale(MD3Spacing.CHIP_HEIGHT)) / 2f);
    }

    @Override
    protected int iconSize() {
        return 18;
    }

    private static MD3Chip.Variant variantOf(Component c) {
        return c instanceof MD3Chip ? ((MD3Chip) c).getVariant() : MD3Chip.Variant.FILTER;
    }

    private static boolean hasMenu(Component c) {
        return c instanceof MD3Chip && ((MD3Chip) c).hasMenu();
    }

    private static boolean isRemovable(Component c) {
        return c instanceof MD3Chip && ((MD3Chip) c).isRemovable();
    }

    /**
     * Where the trailing close sits, or an empty box for a chip that has none.
     */
    public static Rectangle closeBounds(JComponent c) {
        if (!isRemovable(c)) {
            return new Rectangle();
        }

        int size = UIScale.scale(TRAILING_ICON_SIZE);

        return new Rectangle(MD3Paint.mirrorX(c, c.getWidth() - UIScale.scale(MD3Spacing.M) - size, size),
                (c.getHeight() - size) / 2, size, size);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        AbstractButton b = (AbstractButton) c;
        int size = UIScale.scale(TRAILING_ICON_SIZE);
        Color color = contentColor(b);

        if (hasMenu(c)) {
            MD3Icon.of(MD3Icons.CHEVRON_DOWN, TRAILING_ICON_SIZE).withColor(color).paintIcon(c, g,
                    MD3Paint.mirrorX(c, c.getWidth() - UIScale.scale(MD3Spacing.M) - size, size),
                    (c.getHeight() - size) / 2);
        } else if (isRemovable(c)) {
            Rectangle bounds = closeBounds(c);
            MD3Icon.of(MD3Icons.CLOSE, TRAILING_ICON_SIZE).withColor(color).paintIcon(c, g, bounds.x, bounds.y);
        }
    }

    @Override
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        if (variantOf(c) == MD3Chip.Variant.ASSIST && c instanceof AbstractButton
                && ((AbstractButton) c).isEnabled() && ((AbstractButton) c).getIcon() instanceof MD3Icon) {
            MD3Icon icon = ((MD3Icon) ((AbstractButton) c).getIcon()).withSize(iconSize())
                    .withColor(MD3Color.primary());
            icon.paintIcon(c, g, iconRect.x, iconRect.y);

            return;
        }

        super.paintIcon(g, c, iconRect);
    }

    @Override
    protected Color containerColor(AbstractButton b) {
        MD3Chip.Variant variant = variantOf(b);

        // only a filter chip fills when selected. Assist, input and suggestion stay outlined so
        // they do not read as the same control as a facet that is on
        if (variant != MD3Chip.Variant.FILTER) {
            return null;
        }

        float selected = selectedProgress(b);

        if (selected <= 0f) {
            return null;
        }

        Color container = b.isEnabled() ? MD3Color.secondaryContainer()
                : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());

        // there is nothing behind an unselected chip but the page, so the container arrives by
        // becoming opaque rather than by being blended against something
        return MD3Color.withAlpha(container, selected);
    }

    @Override
    protected Color contentColor(AbstractButton b) {
        if (!b.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        MD3Chip.Variant variant = variantOf(b);

        if (variant == MD3Chip.Variant.ASSIST || variant == MD3Chip.Variant.SUGGESTION) {
            return MD3Color.onSurface();
        }

        return MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.onSecondaryContainer(),
                selectedProgress(b));
    }

    @Override
    protected Color outlineColor(AbstractButton b) {
        MD3Chip.Variant variant = variantOf(b);
        float remaining = variant == MD3Chip.Variant.FILTER ? 1f - selectedProgress(b) : 1f;

        if (remaining <= 0f) {
            return null;
        }

        if (!b.isEnabled()) {
            return MD3Color.withAlpha(MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()),
                    remaining);
        }

        // focus is the ring from the button UI, not a primary stroke on top of it
        return MD3Color.withAlpha(MD3Color.outline(), remaining);
    }

    private static class ChipBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            boolean hasIcon = c instanceof AbstractButton && ((AbstractButton) c).getIcon() != null;
            int leading = hasIcon ? MD3Spacing.S : MD3Spacing.L;

            // a menu chevron or a close is painted rather than laid out, so the label has to be
            // kept out of the strip it occupies
            int trailing = hasMenu(c) || isRemovable(c) ? MD3Spacing.M + TRAILING_ICON_SIZE + MD3Spacing.XS
                    : MD3Spacing.L;

            int top = MD3Spacing.XS + Math.max(0, (MD3Spacing.MIN_TOUCH_TARGET - MD3Spacing.CHIP_HEIGHT) / 2);

            MD3Paint.setLeadingTrailing(insets, c, UIScale.scale(top), UIScale.scale(leading), UIScale.scale(top),
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
