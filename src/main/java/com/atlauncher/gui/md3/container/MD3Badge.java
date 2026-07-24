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
package com.atlauncher.gui.md3.container;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A small rounded label for read-only metadata - a version number, a loader name, a count.
 *
 * <p>
 * Looks like a chip and is deliberately not one. {@link com.atlauncher.gui.md3.input.MD3Chip} is a
 * control: it takes focus, responds to hover, and invites a click. Facts about an instance do
 * none of those things, and dressing them as chips teaches users that half the chips on the screen
 * are inert.
 */
public class MD3Badge extends JLabel {
    private final String containerRole;
    private final String contentRole;
    private final boolean outlined;

    private MD3Badge(String text, String containerRole, String contentRole, boolean outlined) {
        super(text);

        this.containerRole = containerRole;
        this.contentRole = contentRole;
        this.outlined = outlined;

        setOpaque(false);
        setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        setForeground(MD3Color.get(contentRole));
        setBorder(MD3Spacing.border(MD3Spacing.XS, MD3Spacing.S));
    }

    /**
     * Neutral metadata - versions, counts, loaders.
     *
     * <p>
     * Outlined rather than filled, because there is no container colour that reliably contrasts
     * with every surface a badge might sit on. A filled card is already surfaceContainerHighest, so
     * a badge filled with the same role vanishes into it. An outline reads on all of them.
     */
    public static MD3Badge neutral(String text) {
        return new MD3Badge(text, MD3Color.OUTLINE_VARIANT, MD3Color.ON_SURFACE_VARIANT, true);
    }

    /** Something the user may want to act on, such as an available update. */
    public static MD3Badge notable(String text) {
        return new MD3Badge(text, MD3Color.TERTIARY_CONTAINER, MD3Color.ON_TERTIARY_CONTAINER, false);
    }

    /** Something wrong - a corrupted instance, a failed install. */
    public static MD3Badge problem(String text) {
        return new MD3Badge(text, MD3Color.ERROR_CONTAINER, MD3Color.ON_ERROR_CONTAINER, false);
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // roles are resolved from the active theme, so they have to be re-read when it changes;
        // the fields do not exist yet during the superclass constructor's own call
        if (contentRole != null) {
            setForeground(MD3Color.get(contentRole));
            setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Color container = MD3Color.get(containerRole);

            if (outlined) {
                MD3Paint.outline(g2, MD3Paint.shapeOf(this, MD3Shape.SMALL), container, 1f);
            } else {
                MD3Paint.fill(g2, MD3Paint.shapeOf(this, MD3Shape.SMALL), container);
            }
        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }

    /**
     * A row height that lines badges up with each other regardless of their text.
     */
    public static int height() {
        return UIScale.scale(24);
    }
}
