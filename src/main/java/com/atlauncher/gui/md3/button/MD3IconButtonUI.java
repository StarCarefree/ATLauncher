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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3IconButton}.
 *
 * <p>
 * Reuses the button painting wholesale and only re-answers the colour questions, since an icon
 * button's variants map onto different roles than a labelled button's do.
 *
 * <p>
 * A selected icon button is a toggle, not a louder version of the same action. Standard and
 * outlined pick up a container; tonal steps up to the primary container; filled is already as loud
 * as it gets, so selecting it does not change the fill.
 */
public class MD3IconButtonUI extends MD3ButtonUI {
    public static ComponentUI createUI(JComponent c) {
        return new MD3IconButtonUI();
    }

    private static MD3IconButton.Variant variant(Component c) {
        return c instanceof MD3IconButton ? ((MD3IconButton) c).getVariant() : MD3IconButton.Variant.STANDARD;
    }

    private static MD3IconButton.Size sizeOf(Component c) {
        return c instanceof MD3IconButton ? ((MD3IconButton) c).getButtonSize() : MD3IconButton.Size.MEDIUM;
    }

    private static int targetSize(Component c) {
        switch (sizeOf(c)) {
            case SMALL:
                return MD3Spacing.ICON_BUTTON_SIZE_SMALL;
            case LARGE:
                return MD3Spacing.ICON_BUTTON_SIZE_LARGE;
            case MEDIUM:
            default:
                return MD3Spacing.ICON_BUTTON_SIZE;
        }
    }

    @Override
    protected int shapeRadius() {
        return MD3Shape.ICON_BUTTON;
    }

    @Override
    protected int minimumHeight() {
        return targetSize(host);
    }

    @Override
    protected int iconSize() {
        return sizeOf(host) == MD3IconButton.Size.SMALL ? MD3Spacing.ICON_SIZE : MD3Spacing.ICON_SIZE_LARGE;
    }

    @Override
    protected Color containerColor(AbstractButton b) {
        MD3IconButton.Variant variant = variant(b);
        float selected = selectedProgress(b);

        if (variant == MD3IconButton.Variant.STANDARD) {
            return null;
        }

        if (!b.isEnabled()) {
            if (variant == MD3IconButton.Variant.OUTLINED && selected <= 0f) {
                return null;
            }

            return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        switch (variant) {
            case FILLED:
                return MD3Color.primary();
            case TONAL:
                return MD3Animated.lerp(MD3Color.secondaryContainer(), MD3Color.primaryContainer(), selected);
            case OUTLINED:
                if (selected <= 0f) {
                    return null;
                }

                return MD3Color.withAlpha(MD3Color.inverseSurface(), selected);
            case STANDARD:
            default:
                return null;
        }
    }

    @Override
    protected Color contentColor(AbstractButton b) {
        if (!b.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        float selected = selectedProgress(b);

        switch (variant(b)) {
            case FILLED:
                return MD3Color.get(MD3Color.ON_PRIMARY);
            case TONAL:
                return MD3Animated.lerp(MD3Color.onSecondaryContainer(), MD3Color.onPrimaryContainer(), selected);
            case OUTLINED:
                return MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.inverseOnSurface(), selected);
            case STANDARD:
            default:
                return MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.primary(), selected);
        }
    }

    @Override
    protected Color outlineColor(AbstractButton b) {
        if (variant(b) != MD3IconButton.Variant.OUTLINED) {
            return null;
        }

        float remaining = 1f - selectedProgress(b);

        if (remaining <= 0f) {
            return null;
        }

        if (!b.isEnabled()) {
            return MD3Color.withAlpha(MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()),
                    remaining);
        }

        Color line = b.isFocusOwner() ? MD3Color.primary() : MD3Color.outline();

        return MD3Color.withAlpha(line, remaining);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int size = UIScale.scale(targetSize(c));

        return new Dimension(size, size);
    }
}
