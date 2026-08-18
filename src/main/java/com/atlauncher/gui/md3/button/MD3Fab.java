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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A floating action button - the one thing a screen is most for, sitting above everything else.
 *
 * <p>
 * Distinct from {@link MD3IconButton} in every dimension that matters: 56dp rather than 40, a 16dp
 * corner rather than a circle, the primary <em>container</em> rather than primary itself, and a
 * shadow. Those differences are the point - a FAB is meant to read as floating over the surface
 * rather than sitting in it, and a filled icon button at 40dp reads as one control among the rest.
 *
 * <p>
 * One of the few things in Material 3 that still casts a shadow; everything else expresses height
 * with a lighter surface. So this reserves room inside its own bounds for the shadow to fall in -
 * Swing clips painting to the component, and a shadow drawn at the edges of the container would
 * have nowhere to go.
 *
 * <pre>
 * {@link #MD3Fab(MD3Icon.Painter, String) regular}   56dp, icon only - the default
 * {@link #small(MD3Icon.Painter, String) small}      40dp, icon only - a FAB that has to share
 * {@link #extended(MD3Icon.Painter, String) extended} 56dp, icon plus a label
 * </pre>
 */
public class MD3Fab extends JButton {
    public enum Size {
        SMALL, REGULAR
    }

    /** Level 3 at rest, and a level higher under the pointer, which is Material's lift. */
    private static final int RESTING = MD3Elevation.LEVEL3;
    private static final int HOVERED = MD3Elevation.LEVEL4;

    private final MD3Icon.Painter painter;
    private final Size fabSize;
    private final boolean extended;
    private final String label;

    private MD3StateLayer stateLayer;

    /**
     * A regular 56dp icon-only FAB.
     *
     * @param label what the action is, as the tooltip and the accessible name - a button drawn as a
     *              plus sign is one a screen reader can otherwise only call "button"
     */
    public MD3Fab(MD3Icon.Painter painter, String label) {
        this(painter, label, Size.REGULAR, false);
    }

    public static MD3Fab small(MD3Icon.Painter painter, String label) {
        return new MD3Fab(painter, label, Size.SMALL, false);
    }

    /**
     * A FAB that carries its label next to the icon, for a screen that has the room and a first-time
     * user who should not have to guess what the plus sign does.
     */
    public static MD3Fab extended(MD3Icon.Painter painter, String label) {
        return new MD3Fab(painter, label, Size.REGULAR, true);
    }

    private MD3Fab(MD3Icon.Painter painter, String label, Size fabSize, boolean extended) {
        this.painter = painter;
        this.fabSize = fabSize;
        this.extended = extended;
        this.label = label;

        setToolTipText(label);
        getAccessibleContext().setAccessibleName(label);

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(null);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(MD3Type.font(MD3Type.LABEL_LARGE));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_LARGE);

        // from the model rather than from mouse listeners: it is the only source that reports a
        // space bar press as a press
        stateLayer = MD3StateLayer.attach(this, getModel());
    }

    public Size getFabSize() {
        return fabSize;
    }

    public boolean isExtended() {
        return extended;
    }

    /**
     * A FAB paints itself rather than going through a {@code ComponentUI}, so there is no
     * {@code uninstallUI} for its state layer to be released from. Leaving the hierarchy is the
     * equivalent moment: the layer listens to this button's model and animates it, and neither is
     * any use to a FAB that is no longer on screen.
     */
    @Override
    public void addNotify() {
        super.addNotify();

        if (stateLayer == null) {
            stateLayer = MD3StateLayer.attach(this, getModel());
        }
    }

    @Override
    public void removeNotify() {
        if (stateLayer != null) {
            stateLayer.uninstall();
            stateLayer = null;
        }

        super.removeNotify();
    }

    private float hoverLift() {
        return stateLayer == null ? 0f : Math.max(0f, stateLayer.hoverProgress() - stateLayer.pressProgress());
    }

    private float pressProgress() {
        return stateLayer == null ? 0f : stateLayer.pressProgress();
    }

    /**
     * The room kept around the container for the shadow. Enough for the blur at the height this
     * reaches under the pointer; the few pixels of downward offset below that are allowed to clip,
     * being the faintest part of it.
     */
    private static int shadowRoom() {
        return UIScale.scale(MD3Paint.shadowRoom(HOVERED));
    }

    /**
     * @return the height it has reached, between {@link #RESTING} and {@link #HOVERED} - a press
     *         puts it back down, so pushing the button looks like pushing it rather than like the
     *         pointer having left
     */
    private float elevation() {
        if (!isEnabled()) {
            return MD3Elevation.LEVEL0;
        }

        return MD3Animated.lerp(RESTING, HOVERED, hoverLift());
    }

    /**
     * The corner it has reached. A FAB is the one control in the launcher that rounds <em>out</em>
     * under the finger rather than in: it is already a squircle, and Material 3 morphs it the rest of
     * the way to a circle. An extended FAB is already a stadium, so the morph has nothing to do.
     */
    private Shape shapeOf(float inset) {
        float width = getWidth() - inset * 2f;
        float height = getHeight() - inset * 2f;
        int restToken = extended ? MD3Shape.FULL
                : (fabSize == Size.SMALL ? MD3Shape.MEDIUM : MD3Shape.FAB);
        float radius = MD3Animated.lerp(MD3Shape.resolve(restToken, width, height),
                MD3Shape.resolve(MD3Shape.FULL, width, height), pressProgress());

        return new RoundRectangle2D.Float(inset, inset, width, height, radius * 2f, radius * 2f);
    }

    private Color containerColor() {
        if (!isEnabled()) {
            return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        return MD3Color.get(MD3Color.PRIMARY_CONTAINER);
    }

    private Color contentColor() {
        if (!isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        return MD3Color.get(MD3Color.ON_PRIMARY_CONTAINER);
    }

    private int containerHeight() {
        return fabSize == Size.SMALL ? MD3Spacing.FAB_SIZE_SMALL : MD3Spacing.FAB_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            float inset = shadowRoom();
            Shape shape = shapeOf(inset);
            Color content = contentColor();

            MD3Paint.shadow(g2, shape, elevation());
            MD3Paint.fill(g2, shape, containerColor());

            if (stateLayer != null) {
                stateLayer.paint(g2, shape, content);
            }

            if (isEnabled() && MD3Focus.isVisible(this)) {
                MD3Paint.focusRing(g2, inset, inset, getWidth() - inset * 2f, getHeight() - inset * 2f,
                        extended ? MD3Shape.FULL : MD3Shape.FAB);
            }

            int box = UIScale.scale(MD3Spacing.ICON_SIZE_LARGE);
            int iconX;
            int iconY = (getHeight() - box) / 2;

            if (extended) {
                iconX = Math.round(inset) + UIScale.scale(MD3Spacing.L);
            } else {
                iconX = (getWidth() - box) / 2;
            }

            MD3Icon.of(painter, MD3Spacing.ICON_SIZE_LARGE).withColor(content).paintIcon(this, g2, iconX, iconY);

            if (extended && label != null && !label.isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(content);

                FontMetrics metrics = g2.getFontMetrics();
                int textX = iconX + box + UIScale.scale(MD3Spacing.M);
                int textY = (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;

                g2.drawString(label, textX, textY);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int room = shadowRoom();
        int height = UIScale.scale(containerHeight()) + room * 2;

        if (!extended) {
            return new Dimension(height, height);
        }

        FontMetrics metrics = getFontMetrics(getFont());
        int text = label == null ? 0 : metrics.stringWidth(label);
        int width = room * 2 + UIScale.scale(MD3Spacing.L) + UIScale.scale(MD3Spacing.ICON_SIZE_LARGE)
                + UIScale.scale(MD3Spacing.M) + text + UIScale.scale(20);

        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
