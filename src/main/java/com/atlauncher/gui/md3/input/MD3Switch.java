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
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 switch.
 *
 * <p>
 * Extends {@link JCheckBox} deliberately: the settings pages are full of checkboxes wired to view
 * models through {@code addActionListener} and {@code isSelected}, and this drops straight into
 * those call sites with nothing but the constructor changing.
 *
 * <p>
 * Use a switch when the change takes effect immediately, and a checkbox when it takes effect on
 * save. Most of the launcher's settings are the former.
 *
 * <p>
 * Painted through a per-instance {@link Icon} rather than a full component UI - the checkbox UI
 * already handles label layout, mnemonics and focus traversal correctly, and there is nothing to
 * gain from reimplementing it.
 */
public class MD3Switch extends JCheckBox {
    private static final int TRACK_WIDTH = 52;
    private static final int TRACK_HEIGHT = 32;
    private static final int HANDLE_OFF = 16;
    private static final int HANDLE_ON = 24;

    public MD3Switch() {
        this(null);
    }

    public MD3Switch(String text) {
        super(text);

        setIcon(new SwitchIcon(this));
        setOpaque(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setBorderPainted(false);
        setIconTextGap(UIScale.scale(MD3Spacing.M));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(MD3Type.font(MD3Type.BODY_LARGE));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        setForeground(MD3Color.onSurface());
    }

    /**
     * Draws the track and handle, and animates the handle between the two ends.
     *
     * <p>
     * One per switch, since it holds that switch's animation position.
     */
    private static final class SwitchIcon implements Icon {
        private final AbstractButton button;

        /** 0 while off, 1 while on, anywhere between mid-animation. */
        private float position;
        private Animator animator;
        private float from;
        private float to;

        SwitchIcon(AbstractButton button) {
            this.button = button;
            this.position = button.isSelected() ? 1f : 0f;
            this.to = position;

            button.addChangeListener(e -> retarget());
        }

        private void retarget() {
            float target = button.isSelected() ? 1f : 0f;

            if (Math.abs(target - to) < 0.001f) {
                button.repaint();

                return;
            }

            if (animator != null) {
                animator.stop();
            }

            if (!Animator.useAnimation() || MD3Motion.isReduced()) {
                position = target;
                to = target;
                button.repaint();

                return;
            }

            from = position;
            to = target;

            animator = MD3Motion.animator(MD3Motion.SHORT4, MD3Motion.EMPHASIZED, fraction -> {
                position = from + (to - from) * fraction;
                button.repaint();
            });
            animator.start();
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(TRACK_WIDTH);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(TRACK_HEIGHT);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = button.getModel();
            boolean enabled = button.isEnabled();
            boolean on = position > 0.5f;

            Graphics2D g2 = MD3Paint.setup(g);

            try {
                g2.translate(x, y);

                float width = getIconWidth();
                float height = getIconHeight();
                Shape track = MD3Shape.rounded(0, 0, width, height, MD3Shape.FULL);

                MD3Paint.fill(g2, track, trackColor(enabled, on));

                if (!on || !enabled) {
                    MD3Paint.outline(g2, track, outlineColor(enabled), 2f);
                }

                // the handle grows as it travels, which is what makes the state change read as a
                // physical movement rather than as a colour swap
                float diameter = UIScale.scale(HANDLE_OFF + (HANDLE_ON - HANDLE_OFF) * position);
                float inset = UIScale.scale(4f);
                float travel = width - inset * 2f - UIScale.scale((float) HANDLE_ON);
                float centreX = inset + UIScale.scale(HANDLE_ON) / 2f + travel * position;
                float centreY = height / 2f;

                Shape handle = new Ellipse2D.Float(centreX - diameter / 2f, centreY - diameter / 2f, diameter,
                        diameter);

                float stateAlpha = enabled
                        ? MD3State.opacityFor(model.isRollover(), button.isFocusOwner(),
                                model.isPressed() && model.isArmed(), false)
                        : 0f;

                if (stateAlpha > 0f) {
                    float haloDiameter = UIScale.scale(40f);
                    Shape halo = new Ellipse2D.Float(centreX - haloDiameter / 2f, centreY - haloDiameter / 2f,
                            haloDiameter, haloDiameter);
                    MD3Paint.stateLayer(g2, halo, handleColor(enabled, on), stateAlpha);
                }

                MD3Paint.fill(g2, handle, handleColor(enabled, on));

                if (on && enabled) {
                    int glyphSize = Math.round(diameter * 0.66f);
                    MD3Icon.of(MD3Icons.CHECK, glyphSize).withColor(MD3Color.get(MD3Color.ON_PRIMARY_CONTAINER))
                            .paintIcon(button, g2, Math.round(centreX - glyphSize / 2f),
                                    Math.round(centreY - glyphSize / 2f));
                }
            } finally {
                g2.dispose();
            }
        }

        private Color trackColor(boolean enabled, boolean on) {
            if (!enabled) {
                return on ? MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface())
                        : MD3State.disabledContainer(MD3Color.surfaceVariant(), MD3Color.surface());
            }

            return on ? MD3Color.primary() : MD3Color.surfaceContainerHighest();
        }

        private Color handleColor(boolean enabled, boolean on) {
            if (!enabled) {
                return MD3State.disabledContent(on ? MD3Color.surface() : MD3Color.onSurface(), MD3Color.surface());
            }

            return on ? MD3Color.get(MD3Color.ON_PRIMARY) : MD3Color.outline();
        }

        private Color outlineColor(boolean enabled) {
            return enabled ? MD3Color.outline()
                    : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }
    }

}
