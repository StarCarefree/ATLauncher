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
package com.atlauncher.gui.md3.icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.formdev.flatlaf.util.UIScale;

/**
 * A vector icon drawn on Material's 24 by 24 design grid.
 *
 * <p>
 * Icons are resolution independent and take their colour from a Material role rather than from a
 * baked-in value, which is why the launcher no longer needs a light and a dark copy of every glyph
 * the way {@code ATLauncherLaf.getIconPath} does for its PNGs.
 *
 * <p>
 * Immutable - {@code with*} returns a new icon, so a shared constant can be safely re-coloured or
 * re-sized at a call site.
 */
public final class MD3Icon implements Icon {
    /** The design grid every painter draws on, regardless of the icon's rendered size. */
    public static final float GRID = 24f;

    /** Draws an icon on the 24 by 24 grid. The colour is already set on the graphics. */
    public interface Painter {
        void paint(Graphics2D g);
    }

    /**
     * A glyph that points somewhere, and so is mirrored where the interface reads right to left.
     *
     * <p>
     * A back arrow points the way the text came from, not left; in Arabic or Hebrew that is the other
     * way round, and an unmirrored one sends the user forwards. Marked on the painter rather than
     * asked for at the call site, because whether a glyph has a direction is a fact about the glyph -
     * a call site that has to remember is a call site that will not.
     */
    public interface Directional extends Painter {
    }

    private final Painter painter;
    private final int sizeDp;
    private final String role;
    private final Color color;

    private MD3Icon(Painter painter, int sizeDp, String role, Color color) {
        this.painter = painter;
        this.sizeDp = sizeDp;
        this.role = role;
        this.color = color;
    }

    /**
     * An icon at the default size, coloured to match the text beside it.
     */
    public static MD3Icon of(Painter painter) {
        return new MD3Icon(painter, MD3Spacing.ICON_SIZE, null, null);
    }

    public static MD3Icon of(Painter painter, int sizeDp) {
        return new MD3Icon(painter, sizeDp, null, null);
    }

    /**
     * @param role an {@link MD3Color} role name, painted regardless of the host component's
     *             foreground
     */
    public MD3Icon withRole(String role) {
        return new MD3Icon(painter, sizeDp, role, null);
    }

    public MD3Icon withColor(Color color) {
        return new MD3Icon(painter, sizeDp, null, color);
    }

    public MD3Icon withSize(int sizeDp) {
        return new MD3Icon(painter, sizeDp, role, color);
    }

    /**
     * An icon that follows its host component's foreground colour. This is the default, and is what
     * you want inside a button - the icon then tracks the button's content colour through every
     * state without the call site having to know which variant it is.
     */
    public MD3Icon following() {
        return new MD3Icon(painter, sizeDp, null, null);
    }

    @Override
    public int getIconWidth() {
        return UIScale.scale(sizeDp);
    }

    @Override
    public int getIconHeight() {
        return UIScale.scale(sizeDp);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            g2.translate(x, y);

            float scale = UIScale.scale((float) sizeDp) / GRID;
            g2.scale(scale, scale);

            if (painter instanceof Directional && !MD3Paint.isLeftToRight(c)) {
                // flipped about the middle of the grid, so the glyph stays inside the box it was
                // given rather than being drawn to the left of it
                g2.translate(GRID, 0);
                g2.scale(-1, 1);
            }

            g2.setColor(resolveColor(c));

            painter.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    private Color resolveColor(Component c) {
        if (color != null) {
            return color;
        }

        if (role != null) {
            return MD3Color.get(role);
        }

        Color foreground = c != null ? c.getForeground() : null;

        if (foreground == null) {
            foreground = MD3Color.onSurface();
        }

        // a disabled host dims its own text through the look and feel, but an icon is painted by
        // us, so the same reduction has to be applied here
        if (c != null && !c.isEnabled()) {
            return MD3State.disabledContent(foreground, MD3Color.surface());
        }

        return foreground;
    }
}
