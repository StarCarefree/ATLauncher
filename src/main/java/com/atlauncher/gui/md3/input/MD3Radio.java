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
import java.awt.geom.Ellipse2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JRadioButton;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 radio button.
 *
 * <p>
 * Use where exactly one value can be picked from a short set. A longer set is a
 * {@link MD3ComboBox} or a row of {@link MD3FilterChip}s; a yes/no that acts at once is a
 * {@link MD3Switch}.
 *
 * <p>
 * Extends {@link JRadioButton} so an existing {@link javax.swing.ButtonGroup} keeps working.
 */
public class MD3Radio extends JRadioButton {
    private static final int RING_SIZE = MD3Spacing.CHECKBOX_BOX_SIZE;
    private static final int OUTLINE_WIDTH = 2;
    private static final int TARGET = MD3Spacing.MIN_TOUCH_TARGET;
    private static final int COMPACT_TARGET = 24;

    private boolean compact;

    public MD3Radio() {
        this(null);
    }

    public MD3Radio(String text) {
        super(text);

        setIcon(new RadioIcon(this));
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

    @Override
    public void updateUI() {
        setUI(new MD3MixedRadioUI());
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }

        this.compact = compact;
        setIconTextGap(compact ? MD3Spacing.scale(MD3Spacing.S) : 0);
        revalidate();
        repaint();
    }

    private int target() {
        return compact ? COMPACT_TARGET : TARGET;
    }

    private static final class RadioIcon implements Icon {
        private final MD3Radio button;
        private final MD3Animated selection;

        RadioIcon(MD3Radio button) {
            this.button = button;
            this.selection = new MD3Animated(button, button.isSelected() ? 1f : 0f, MD3Motion.SHORT3,
                    MD3Motion.EMPHASIZED_DECELERATE);

            button.addChangeListener(e -> {
                selection.setTarget(button.isSelected() ? 1f : 0f);
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
            float on = selection.value();

            Graphics2D g2 = MD3Paint.setup(g);

            try {
                g2.translate(x, y);

                float centreX = getIconWidth() / 2f;
                float centreY = getIconHeight() / 2f;
                float ring = UIScale.scale((float) RING_SIZE);

                float stateAlpha = enabled
                        ? MD3State.opacityFor(model.isRollover(), MD3Focus.isVisible(button),
                                model.isPressed() && model.isArmed(), false)
                        : 0f;

                if (stateAlpha > 0f) {
                    float halo = UIScale.scale((float) button.target());
                    MD3Paint.stateLayer(g2, new Ellipse2D.Float(centreX - halo / 2f, centreY - halo / 2f, halo,
                            halo), contentColor(enabled, on), stateAlpha);
                }

                Ellipse2D.Float outline = new Ellipse2D.Float(centreX - ring / 2f, centreY - ring / 2f, ring, ring);
                Color ringColor = enabled ? MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.primary(), on)
                        : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
                MD3Paint.outline(g2, outline, ringColor, OUTLINE_WIDTH);

                if (on > 0f) {
                    float dot = ring * 0.5f * on;
                    Color fill = enabled ? MD3Color.primary()
                            : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
                    MD3Paint.fill(g2, new Ellipse2D.Float(centreX - dot / 2f, centreY - dot / 2f, dot, dot),
                            MD3Color.withAlpha(fill, on));
                }
            } finally {
                g2.dispose();
            }
        }

        private Color contentColor(boolean enabled, float on) {
            Color outline = enabled ? MD3Color.onSurfaceVariant()
                    : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
            Color selected = enabled ? MD3Color.primary()
                    : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());

            return MD3Animated.lerp(outline, selected, on);
        }
    }
}
