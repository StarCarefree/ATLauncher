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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
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
 * A Material 3 switch.
 *
 * <p>
 * Extends {@link JCheckBox} deliberately: the settings pages are full of checkboxes wired to view
 * models through {@code addActionListener} and {@code isSelected}, and this drops straight into
 * those call sites with nothing but the constructor changing.
 *
 * <p>
 * Material reserves switches for changes that take effect immediately and asks for a checkbox where
 * they take effect on save. <b>The launcher's settings page is the second kind and uses switches
 * anyway</b>, deliberately: all 27 of its boolean settings are things that are simply on or off,
 * a column of switches says that at a glance where a column of tick boxes does not, and the pinned
 * save bar is what tells the user nothing has been committed yet. Somewhere with no save step - a
 * dialog that acts on the spot - a switch is the unambiguous choice.
 *
 * <p>
 * Painted through a per-instance {@link Icon} rather than a full component UI - the checkbox UI
 * already handles label layout, mnemonics and focus traversal correctly, and there is nothing to
 * gain from reimplementing it.
 */
public class MD3Switch extends JCheckBox {
    private static final int TRACK_WIDTH = MD3Spacing.SWITCH_TRACK_WIDTH;
    private static final int TRACK_HEIGHT = MD3Spacing.SWITCH_TRACK_HEIGHT;
    private static final int HANDLE_OFF = MD3Spacing.SWITCH_HANDLE_OFF;
    private static final int HANDLE_ON = MD3Spacing.SWITCH_HANDLE_ON;
    private static final int HANDLE_PRESSED = MD3Spacing.SWITCH_HANDLE_PRESSED;

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

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJCheckBox() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.TOGGLE_BUTTON;
                }
            };
        }

        return accessibleContext;
    }

    /**
     * Draws the track and handle, and animates the handle between the two ends.
     *
     * <p>
     * One per switch, since it holds that switch's animation position.
     */
    static final class SwitchIcon implements Icon {
        private final AbstractButton button;

        /**
         * 0 while off, 1 while on, anywhere between mid-animation.
         *
         * <p>
         * Reachable from the package so a test can hold the handle partway across - what it looks
         * like there is the whole question, and racing a two hundred millisecond animation to find
         * out is not a test.
         */
        final MD3Animated position;

        /** 0 at rest, 1 while held down - Material grows the handle under the finger. */
        private final MD3Animated pressure;

        SwitchIcon(AbstractButton button) {
            this.button = button;
            this.position = new MD3Animated(button, button.isSelected() ? 1f : 0f, MD3Motion.SHORT4,
                    MD3Motion.EMPHASIZED);
            this.pressure = new MD3Animated(button, 0f, MD3Motion.SHORT3, MD3Motion.STANDARD);

            button.addChangeListener(e -> {
                ButtonModel model = button.getModel();

                position.setTarget(button.isSelected() ? 1f : 0f);
                pressure.setTarget(model.isPressed() && model.isArmed() ? 1f : 0f);

                // rollover, press and focus all change what is drawn without moving the handle, and
                // the model reports all of them through here
                button.repaint();
            });
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(TRACK_WIDTH);
        }

        @Override
        public int getIconHeight() {
            // the track is 32dp; the halo and the pointer target are 40 and 48. Swing clips an
            // icon to the size it declares, so reporting the track height sliced the focus ring
            return UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = button.getModel();
            boolean enabled = button.isEnabled();

            // how far into being on it is, not whether it has passed halfway. Every colour here used
            // to be picked by a boolean on that test, so the track, the handle and the outline all
            // changed at once in the middle of the handle's travel and the tick appeared whole out
            // of nothing - a two hundred millisecond slide with a jump cut in it.
            float on = position.value();

            Graphics2D g2 = MD3Paint.setup(g);

            try {
                float width = UIScale.scale((float) TRACK_WIDTH);
                float height = UIScale.scale((float) TRACK_HEIGHT);

                g2.translate(x, y + (getIconHeight() - height) / 2f);

                Shape track = MD3Shape.rounded(0, 0, width, height, MD3Shape.FULL);

                MD3Paint.fill(g2, track, trackColor(enabled, on));

                // the outline belongs to the off state, and goes as the track fills in. A selected
                // disabled switch is fill only - keeping the line draws two edges on a 12% track
                if (on < 1f) {
                    MD3Paint.outline(g2, track, MD3Color.withAlpha(outlineColor(enabled), 1f - on), 2f);
                }

                // the handle grows as it travels, which is what makes the state change read as a
                // physical movement rather than as a colour swap - and grows again while it is held,
                // which is the whole of a switch's press feedback. It stops 2dp short of the track at
                // either end, so the larger handle still has somewhere to be
                float settled = HANDLE_OFF + (HANDLE_ON - HANDLE_OFF) * on;
                float diameter = UIScale.scale(MD3Animated.lerp(settled, HANDLE_PRESSED, pressure.value()));
                float inset = UIScale.scale(4f);
                float travel = width - inset * 2f - UIScale.scale((float) HANDLE_ON);
                float centreX = inset + UIScale.scale(HANDLE_ON) / 2f + travel * on;
                float centreY = height / 2f;

                Shape handle = new Ellipse2D.Float(centreX - diameter / 2f, centreY - diameter / 2f, diameter,
                        diameter);

                float stateAlpha = enabled
                        ? MD3State.opacityFor(model.isRollover(), MD3Focus.isVisible(button),
                                model.isPressed() && model.isArmed(), false)
                        : 0f;

                if (stateAlpha > 0f) {
                    float haloDiameter = UIScale.scale(40f);
                    Shape halo = new Ellipse2D.Float(centreX - haloDiameter / 2f, centreY - haloDiameter / 2f,
                            haloDiameter, haloDiameter);
                    MD3Paint.stateLayer(g2, halo, handleColor(enabled, on), stateAlpha);
                }

                MD3Paint.fill(g2, handle, handleColor(enabled, on));

                paintTick(g2, enabled, on, diameter, centreX, centreY);
            } finally {
                g2.dispose();
            }
        }

        /**
         * The tick inside the handle, fading up as the handle arrives rather than being switched on
         * partway across.
         */
        private void paintTick(Graphics2D g, boolean enabled, float on, float diameter, float centreX,
                float centreY) {
            if (!enabled || on <= 0f) {
                return;
            }

            int glyphSize = Math.round(diameter * 0.66f);

            if (glyphSize <= 0) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, on)));

                MD3Icon.of(MD3Icons.CHECK, glyphSize).withColor(MD3Color.get(MD3Color.ON_PRIMARY_CONTAINER))
                        .paintIcon(button, g2, Math.round(centreX - glyphSize / 2f),
                                Math.round(centreY - glyphSize / 2f));
            } finally {
                g2.dispose();
            }
        }

        private Color trackColor(boolean enabled, float on) {
            if (!enabled) {
                return MD3Animated.lerp(MD3State.disabledContainer(MD3Color.surfaceVariant(), MD3Color.surface()),
                        MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()), on);
            }

            return MD3Animated.lerp(MD3Color.surfaceContainerHighest(), MD3Color.primary(), on);
        }

        private Color handleColor(boolean enabled, float on) {
            if (!enabled) {
                return MD3State.disabledContent(
                        MD3Animated.lerp(MD3Color.onSurface(), MD3Color.surface(), on), MD3Color.surface());
            }

            return MD3Animated.lerp(MD3Color.outline(), MD3Color.get(MD3Color.ON_PRIMARY), on);
        }

        private Color outlineColor(boolean enabled) {
            return enabled ? MD3Color.outline()
                    : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }
    }

}
