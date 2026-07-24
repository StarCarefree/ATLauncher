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
package com.atlauncher.gui.md3.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.ButtonModel;
import javax.swing.JComponent;

import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3State;
import com.formdev.flatlaf.util.Animator;

/**
 * Tracks a component's interaction state and fades its state layer between the levels Material 3
 * defines for it.
 *
 * <p>
 * Two ways to drive it. Buttons already have a {@link ButtonModel} that knows about rollover and
 * armed - and, importantly, knows a button triggered from the keyboard is pressed, which no mouse
 * listener can tell you - so those {@link #attach(JComponent, ButtonModel) attach} to the model.
 * Everything else {@link #install(JComponent) installs} its own listeners.
 *
 * <p>
 * Deliberately no ripple. Material's own desktop and web implementations dropped it, and in Swing
 * an expanding clipped circle means repainting the whole component every frame for the duration of
 * a click - a real cost on a view showing a hundred cards, for an effect the platform's users do
 * not expect anyway.
 */
public final class MD3StateLayer {
    private final JComponent component;

    private ButtonModel model;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private FocusListener focusListener;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean selected;

    private float alpha;
    private float from;
    private float to;
    private Animator animator;

    private MD3StateLayer(JComponent component) {
        this.component = component;
    }

    /**
     * Drives the layer from a button's model. Preferred for anything with one - the model is the
     * only source that reports a space-bar press as a press.
     */
    public static MD3StateLayer attach(JComponent component, ButtonModel model) {
        MD3StateLayer layer = new MD3StateLayer(component);
        layer.model = model;
        model.addChangeListener(e -> layer.syncFromModel());
        layer.installFocusListener();
        layer.syncFromModel();

        return layer;
    }

    /**
     * Drives the layer from its own mouse and focus listeners, for components with no button model.
     */
    public static MD3StateLayer install(JComponent component) {
        MD3StateLayer layer = new MD3StateLayer(component);
        layer.installMouseListeners();
        layer.installFocusListener();

        return layer;
    }

    private void installMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(false);
                setPressed(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setPressed(true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setPressed(false);
            }
        };

        mouseListener = adapter;
        mouseMotionListener = adapter;
        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseMotionListener);
    }

    private void installFocusListener() {
        focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                setFocused(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                setFocused(false);
            }
        };

        component.addFocusListener(focusListener);
    }

    /**
     * Removes every listener this layer added. Call from a component's {@code uninstallUI} so a
     * discarded card does not keep its model alive.
     */
    public void uninstall() {
        if (animator != null) {
            animator.stop();
            animator = null;
        }

        if (mouseListener != null) {
            component.removeMouseListener(mouseListener);
            mouseListener = null;
        }

        if (mouseMotionListener != null) {
            component.removeMouseMotionListener(mouseMotionListener);
            mouseMotionListener = null;
        }

        if (focusListener != null) {
            component.removeFocusListener(focusListener);
            focusListener = null;
        }

        model = null;
    }

    private void syncFromModel() {
        if (model == null) {
            return;
        }

        boolean changed = hovered != model.isRollover();
        changed |= pressed != (model.isPressed() && model.isArmed());
        changed |= selected != model.isSelected();

        hovered = model.isRollover();
        pressed = model.isPressed() && model.isArmed();
        selected = model.isSelected();

        if (changed) {
            retarget();
        }
    }

    public void setHovered(boolean value) {
        if (hovered != value) {
            hovered = value;
            retarget();
        }
    }

    public void setPressed(boolean value) {
        if (pressed != value) {
            pressed = value;
            retarget();
        }
    }

    public void setFocused(boolean value) {
        if (focused != value) {
            focused = value;
            retarget();
        }
    }

    public void setSelected(boolean value) {
        if (selected != value) {
            selected = value;
            retarget();
        }
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * @return the current opacity, mid-fade if an animation is running
     */
    public float alpha() {
        return component.isEnabled() ? alpha : 0f;
    }

    private float targetAlpha() {
        if (!component.isEnabled()) {
            return 0f;
        }

        return MD3State.opacityFor(hovered, focused, pressed, false);
    }

    private void retarget() {
        float target = targetAlpha();

        if (Math.abs(target - to) < 0.001f && animator != null && animator.isRunning()) {
            return;
        }

        if (animator != null) {
            animator.stop();
        }

        if (!Animator.useAnimation() || MD3Motion.isReduced()) {
            alpha = target;
            to = target;
            component.repaint();

            return;
        }

        from = alpha;
        to = target;

        animator = MD3Motion.animator(MD3Motion.STATE_LAYER, MD3Motion.STANDARD, fraction -> {
            alpha = from + (to - from) * fraction;
            component.repaint();
        });
        animator.start();
    }

    /**
     * Paints the layer, if there is one to paint.
     *
     * @param content the component's content colour - the colour its text and icons are drawn in,
     *                which is what Material lays over the container
     */
    public void paint(Graphics2D g, Shape shape, Color content) {
        MD3Paint.stateLayer(g, shape, content, alpha());
    }
}
