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
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3State;

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
 *
 * <p>
 * Besides the layer's own opacity it publishes {@link #hoverProgress()} and {@link #pressProgress()},
 * the same two states as smoothed numbers rather than as booleans. A component that expresses hover
 * or press as something other than a tint - a card lifting, a button rounding its corners in - reads
 * those instead of installing a second set of listeners that would have to agree with these about
 * what counts as pressed.
 */
public final class MD3StateLayer {
    /**
     * How often to look for the pointer once it has gone somewhere inside the component that takes
     * its own mouse events. Frequent enough that letting go of a card is not noticeably late, and
     * only ever one of these is running - there is one pointer.
     */
    private static final int DEPARTURE_POLL = 60;

    private final JComponent component;

    private ButtonModel model;
    private ChangeListener modelListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private FocusListener focusListener;
    private Timer departureWatch;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean selected;

    private final MD3Animated overlay;
    private final MD3Animated hoverProgress;
    private final MD3Animated pressProgress;

    private MD3StateLayer(JComponent component) {
        this.component = component;

        overlay = new MD3Animated(component, 0f, MD3Motion.STATE_LAYER, MD3Motion.STANDARD);
        hoverProgress = new MD3Animated(component, 0f, MD3Motion.ELEVATION, MD3Motion.STANDARD);
        pressProgress = new MD3Animated(component, 0f, MD3Motion.SHAPE_MORPH, MD3Motion.STANDARD_ACCELERATE);
    }

    /**
     * Drives the layer from a button's model. Preferred for anything with one - the model is the
     * only source that reports a space-bar press as a press.
     */
    public static MD3StateLayer attach(JComponent component, ButtonModel model) {
        MD3StateLayer layer = new MD3StateLayer(component);
        layer.model = model;
        layer.modelListener = e -> layer.syncFromModel();
        model.addChangeListener(layer.modelListener);
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
                stopWatchingForDeparture();
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setPressed(false);

                // Swing reports the pointer as having left the moment it reaches anything inside
                // that takes mouse events of its own - a button on a card, a label with a tooltip -
                // and reports nothing at all when it then leaves for good. Taken at face value that
                // made every card in the launcher's grids flicker its hover on and off as the
                // pointer crossed what is on it. So an exit that lands inside the component is not
                // an exit; it means watching for the real one instead.
                if (component.contains(e.getPoint())) {
                    watchForDeparture();

                    return;
                }

                setHovered(false);
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

    /**
     * Watches for the pointer leaving a component it is still over but no longer sending events
     * from.
     *
     * <p>
     * Polled rather than driven by an event because there is no event to drive it: once the pointer
     * is over a child, everything it does is reported to that child, and moving from there out of
     * the window tells this component nothing. Only ever one timer is alive across the whole
     * launcher, since the pointer can only be inside one thing at a time.
     */
    private void watchForDeparture() {
        if (departureWatch == null) {
            departureWatch = new Timer(DEPARTURE_POLL, e -> {
                if (stillUnderPointer()) {
                    return;
                }

                stopWatchingForDeparture();
                setHovered(false);
            });
        }

        departureWatch.start();
    }

    private void stopWatchingForDeparture() {
        if (departureWatch != null) {
            departureWatch.stop();
        }
    }

    private boolean stillUnderPointer() {
        try {
            // allowing children, which is the whole point - the pointer being on a card's button is
            // the pointer being on the card
            return component.isShowing() && component.getMousePosition(true) != null;
        } catch (RuntimeException e) {
            // no pointer to ask about, on a headless display or a platform that will not say
            return false;
        }
    }

    private void installFocusListener() {
        focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // whether this focus should be shown, not whether there is any. A click focuses as
                // much as a tab does, and Material's focus layer belongs to the second - the first
                // already has the pointer saying where it is
                setFocused(MD3Focus.isKeyboardModality());
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
        overlay.stop();
        hoverProgress.stop();
        pressProgress.stop();

        if (departureWatch != null) {
            departureWatch.stop();
            departureWatch = null;
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

        // the model outlives the UI delegate that attached to it - a look and feel change installs a
        // new delegate onto the same button - so a layer left listening here is a layer that keeps
        // animating and repainting a component it no longer paints
        if (modelListener != null && model != null) {
            model.removeChangeListener(modelListener);
        }

        modelListener = null;
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
        return component.isEnabled() ? overlay.value() : 0f;
    }

    /**
     * @return how far into being hovered the component is, 0 to 1 - for anything that expresses
     *         hover as more than a tint, such as a card lifting toward the pointer
     */
    public float hoverProgress() {
        return component.isEnabled() ? hoverProgress.value() : 0f;
    }

    /**
     * @return how far into being pressed the component is, 0 to 1 - for a control that changes shape
     *         under the finger
     */
    public float pressProgress() {
        return component.isEnabled() ? pressProgress.value() : 0f;
    }

    private void retarget() {
        boolean enabled = component.isEnabled();

        overlay.setTarget(enabled ? MD3State.opacityFor(hovered, focused, pressed, false) : 0f);
        hoverProgress.setTarget(enabled && hovered ? 1f : 0f);
        pressProgress.setTarget(enabled && pressed ? 1f : 0f);
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
