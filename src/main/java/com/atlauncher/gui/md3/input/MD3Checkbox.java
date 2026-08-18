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
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 checkbox.
 *
 * <p>
 * Use a checkbox where more than one thing can be picked from a set, or where the choice takes
 * effect on save. Use {@link MD3Switch} where a single thing is on or off and acts at once. The
 * launcher's remaining checkboxes are all the first kind - "select all" over a list of mods, which
 * files to include in an export - which is exactly what a switch would misdescribe.
 *
 * <p>
 * Extends {@link JCheckBox} deliberately, as {@link MD3Switch} does: the call sites drive it through
 * {@code addActionListener}, {@code isSelected} and {@code setSelected}, and none of that changes.
 *
 * <p>
 * Painted through a per-instance {@link Icon} rather than a component UI - the checkbox UI already
 * handles label layout, mnemonics and focus traversal, and there is nothing to gain from doing it
 * again.
 *
 * <p>
 * <b>No indeterminate state.</b> Material defines one, and a "select all" over a partly-selected
 * list is exactly what it is for - but nothing here tracks that; the two select-all boxes set every
 * child and never read them back. Adding a third state to the control without the call sites
 * feeding it would be a state nothing can reach.
 */
public class MD3Checkbox extends JCheckBox {
    /** Material's box. The touch target around it is much larger. */
    private static final int BOX_SIZE = MD3Spacing.CHECKBOX_BOX_SIZE;

    private static final int OUTLINE_WIDTH = 2;

    /** The state layer, and the target the pointer actually has to hit. */
    private static final int TARGET = MD3Spacing.MIN_TOUCH_TARGET;

    /** The target for a box in a dense list, where a full one a row would double the list's height. */
    private static final int COMPACT_TARGET = 24;

    private boolean compact;

    public MD3Checkbox() {
        this(null);
    }

    public MD3Checkbox(String text) {
        super(text);

        setIcon(new CheckboxIcon(this));
        setOpaque(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setBorderPainted(false);
        setIconTextGap(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(MD3Type.font(MD3Type.BODY_LARGE));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        setForeground(MD3Color.onSurface());
    }

    public boolean isCompact() {
        return compact;
    }

    /**
     * Shrinks the target around the box, for a checkbox that is one row of many.
     *
     * <p>
     * The box itself does not change - it is 18dp either way, and still reads as the same control.
     * What goes is most of the space around it, which a standalone checkbox needs to be an easy
     * thing to hit and a list of eighty mods cannot afford: at 40dp a row that list is twice as
     * long, and the pointer has the whole row to aim at anyway.
     */
    public void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }

        this.compact = compact;

        // the full target leaves 11dp of its own between the box and the label; a compact one leaves
        // three, so it has to ask for the rest rather than run the text into the box
        setIconTextGap(compact ? MD3Spacing.scale(MD3Spacing.S) : 0);

        revalidate();
        repaint();
    }

    private int target() {
        return compact ? COMPACT_TARGET : TARGET;
    }

    /**
     * Draws the box and its tick, and animates between the two states.
     *
     * <p>
     * One per checkbox, since it holds that checkbox's animation position. The icon is the whole
     * 40dp target rather than the 18dp box, so the state layer has somewhere to be drawn - Swing
     * clips an icon to the size it declares.
     */
    private static final class CheckboxIcon implements Icon {
        private final MD3Checkbox button;

        /** 0 while clear, 1 while ticked, anywhere between mid-animation. */
        private final MD3Animated selection;

        CheckboxIcon(MD3Checkbox button) {
            this.button = button;
            this.selection = new MD3Animated(button, button.isSelected() ? 1f : 0f, MD3Motion.SHORT3,
                    MD3Motion.EMPHASIZED_DECELERATE);

            button.addChangeListener(e -> {
                selection.setTarget(button.isSelected() ? 1f : 0f);

                // rollover, press and focus all change what is drawn without moving the tick
                button.repaint();
            });
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(button.target());
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(button.target());
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = button.getModel();
            boolean enabled = button.isEnabled();
            float ticked = selection.value();

            Graphics2D g2 = MD3Paint.setup(g);

            try {
                g2.translate(x, y);

                float centreX = getIconWidth() / 2f;
                float centreY = getIconHeight() / 2f;
                float box = UIScale.scale((float) BOX_SIZE);

                float stateAlpha = enabled
                        ? MD3State.opacityFor(model.isRollover(), MD3Focus.isVisible(button),
                                model.isPressed() && model.isArmed(), false)
                        : 0f;

                if (stateAlpha > 0f) {
                    // the halo fills the target, so a compact box gets a compact one rather than
                    // one clipped square by the icon's bounds
                    float halo = UIScale.scale((float) button.target());
                    MD3Paint.stateLayer(g2, new Ellipse2D.Float(centreX - halo / 2f, centreY - halo / 2f, halo, halo),
                            contentColor(enabled, ticked), stateAlpha);
                }

                Shape container = MD3Shape.rounded(centreX - box / 2f, centreY - box / 2f, box, box,
                        MD3Shape.CHECKBOX);

                // the container arrives by becoming opaque - there is nothing behind an unticked box
                // but the page, so blending it against a colour it is not on would be wrong
                MD3Paint.fill(g2, container, MD3Color.withAlpha(containerColor(enabled), ticked));

                // and the outline goes as the container comes, so the two never both read as an edge
                if (ticked < 1f) {
                    MD3Paint.outline(g2, container, MD3Color.withAlpha(outlineColor(enabled), 1f - ticked),
                            OUTLINE_WIDTH);
                }

                paintTick(g2, enabled, ticked, centreX, centreY, box);
            } finally {
                g2.dispose();
            }
        }

        /**
         * The tick, fading up inside the box as it fills. Sized to the box rather than to the
         * target, or it would spill past the corners.
         */
        private void paintTick(Graphics2D g, boolean enabled, float ticked, float centreX, float centreY, float box) {
            if (ticked <= 0f) {
                return;
            }

            int glyph = Math.round(box * 0.8f);

            if (glyph <= 0) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        Math.min(1f, ticked)));

                MD3Icon.of(MD3Icons.CHECK, glyph).withColor(tickColor(enabled)).paintIcon(button, g2,
                        Math.round(centreX - glyph / 2f), Math.round(centreY - glyph / 2f));
            } finally {
                g2.dispose();
            }
        }

        private Color containerColor(boolean enabled) {
            return enabled ? MD3Color.primary()
                    : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        private Color outlineColor(boolean enabled) {
            return enabled ? MD3Color.onSurfaceVariant()
                    : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        private Color tickColor(boolean enabled) {
            return enabled ? MD3Color.get(MD3Color.ON_PRIMARY) : MD3Color.surface();
        }

        /** What the state layer is tinted with, which follows whichever colour the box is showing. */
        private Color contentColor(boolean enabled, float ticked) {
            return MD3Animated.lerp(outlineColor(enabled), containerColor(enabled), ticked);
        }
    }
}
