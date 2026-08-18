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
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * A Material 3 card - a rounded surface grouping related content.
 *
 * <p>
 * Replaces the launcher's {@code CollapsiblePanel}, which drew its frame with a {@link
 * javax.swing.border.TitledBorder} and could only ever look like a titled box.
 *
 * <pre>
 * ELEVATED  separated from the content behind it; use over scrolling or patterned backgrounds
 * FILLED    the default; grouping without emphasis
 * OUTLINED  the strongest boundary, and the cheapest to paint
 * </pre>
 *
 * <p>
 * A card that represents a single thing the user can act on - an instance, a modpack - should be
 * {@link #setClickable(boolean) clickable}, which gives it a state layer, a hand cursor, and
 * keyboard activation. A card that merely groups controls should not be.
 *
 * <p>
 * A card that stands for one thing but is <em>acted on through its own buttons</em> - which is every
 * card in the launcher's grids - is not clickable but should still
 * {@link #setHoverElevation(boolean) lift under the pointer}, so a grid of them answers the mouse
 * instead of sitting there.
 */
public class MD3Card extends JPanel {
    public enum Variant {
        ELEVATED, FILLED, OUTLINED
    }

    /** How far an outlined card's edge moves toward the accent under the pointer. */
    private static final float HOVER_RING_ALPHA = 0.24f;

    /** How much a filled card warms under the pointer, where no state layer is doing it already. */
    private static final float HOVER_TONE_ALPHA = 0.08f;

    private Variant variant;
    private boolean clickable;
    private boolean hoverElevation;
    private MD3StateLayer stateLayer;
    private MouseListener activationListener;
    private final List<ActionListener> actionListeners = new ArrayList<>();

    public MD3Card() {
        this(Variant.FILLED, null);
    }

    public MD3Card(Variant variant) {
        this(variant, null);
    }

    public MD3Card(Variant variant, LayoutManager layout) {
        super(layout);

        this.variant = variant;

        setOpaque(false);
        setBorder(MD3Spacing.border(MD3Spacing.L));
    }

    public Variant getVariant() {
        return variant != null ? variant : Variant.FILLED;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;

        repaint();
    }

    public boolean isClickable() {
        return clickable;
    }

    /**
     * Makes the card behave as a single control: it gains a state layer, a hand cursor, focus, and
     * responds to Enter and Space as well as to a click.
     */
    public void setClickable(boolean clickable) {
        if (this.clickable == clickable) {
            return;
        }

        this.clickable = clickable;

        if (clickable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            installStateLayer();

            activationListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        requestFocusInWindow();
                        fireActionPerformed();
                    }
                }
            };

            addMouseListener(activationListener);

            AbstractAction activate = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireActionPerformed();
                }
            };

            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "md3.activate");
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "md3.activate");
            getActionMap().put("md3.activate", activate);
        } else {
            setCursor(Cursor.getDefaultCursor());
            setFocusable(false);

            // a card whose clickability follows its contents can go through here any number of
            // times; without the removal each pass left another listener behind, and one click then
            // fired the action once per pass
            if (activationListener != null) {
                removeMouseListener(activationListener);
                activationListener = null;
            }

            getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            getActionMap().remove("md3.activate");

            releaseStateLayer();
        }

        repaint();
    }

    public boolean hasHoverElevation() {
        return hoverElevation;
    }

    /**
     * Makes the card answer the pointer: it warms a shade and takes an accent ring while the pointer
     * is over it, both fading in and out rather than switching.
     *
     * <p>
     * Distinct from {@link #setClickable(boolean)} on purpose. A card in one of the launcher's grids
     * stands for one instance or one pack, but you act on it through the buttons it carries, not by
     * clicking the card - so it should look reachable without claiming to be a control, which is what
     * a state layer and a hand cursor would say.
     */
    public void setHoverElevation(boolean hoverElevation) {
        if (this.hoverElevation == hoverElevation) {
            return;
        }

        this.hoverElevation = hoverElevation;

        if (hoverElevation) {
            installStateLayer();
        } else {
            releaseStateLayer();
        }

        repaint();
    }

    private void installStateLayer() {
        if (stateLayer == null) {
            stateLayer = MD3StateLayer.install(this);
        }
    }

    private void releaseStateLayer() {
        if (stateLayer != null && !clickable && !hoverElevation) {
            stateLayer.uninstall();
            stateLayer = null;
        }
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click");

        for (ActionListener listener : new ArrayList<>(actionListeners)) {
            listener.actionPerformed(event);
        }
    }

    /**
     * @return how far into being hovered the card is, 0 to 1, or 0 for a card that does not answer
     *         the pointer at all
     */
    protected float lift() {
        return stateLayer == null ? 0f : stateLayer.hoverProgress();
    }

    /**
     * @return the level the card rests at, before anything the pointer does to it
     */
    protected int elevation() {
        return getVariant() == Variant.ELEVATED ? MD3Elevation.LEVEL1 : MD3Elevation.LEVEL0;
    }

    protected Color containerColor() {
        float lift = lift();

        switch (getVariant()) {
            case ELEVATED:
                // rest at L1 (surface-container-low); hover steps to L2
                return MD3Animated.lerp(MD3Elevation.surface(elevation()),
                        MD3Elevation.surface(elevation() + 1), lift);
            case OUTLINED:
                return MD3Animated.lerp(MD3Color.surface(), MD3Color.surfaceContainerLow(), lift);
            case FILLED:
            default:
                // the filled card already sits at the top of the ramp, so it warms instead
                return MD3Color.blend(MD3Color.surfaceContainerHighest(), MD3Color.onSurface(),
                        HOVER_TONE_ALPHA * toneLift());
        }
    }

    /**
     * A clickable card lifts through its state layer, which is already the whole card's worth of
     * tint; adding the container's own would count hover twice and land it somewhere no token
     * describes.
     */
    private float toneLift() {
        return clickable ? 0f : lift();
    }

    /**
     * The ring that comes up under the pointer. An outlined card already has a line to raise toward
     * the accent; the others grow one out of nothing.
     */
    private Color outlineColor() {
        float lift = lift();

        if (getVariant() == Variant.OUTLINED) {
            return MD3Animated.lerp(MD3Color.outlineVariant(), MD3Color.primary(), lift);
        }

        return lift <= 0f ? null : MD3Color.get(MD3Color.OUTLINE, HOVER_RING_ALPHA * lift);
    }

    /**
     * A clickable card is a button, and has to say so.
     *
     * <p>
     * It is a {@link JPanel} that took focus and bound enter and space, which is enough for a keyboard
     * user and nothing at all for a screen reader: the whole grid of instances was announced as a
     * stack of panels, with no indication that any of them could be activated.
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleMD3Card();
        }

        return accessibleContext;
    }

    protected class AccessibleMD3Card extends AccessibleJPanel {
        @Override
        public AccessibleRole getAccessibleRole() {
            return clickable ? AccessibleRole.PUSH_BUTTON : super.getAccessibleRole();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Shape shape = MD3Paint.shapeOf(this, MD3Shape.CARD);

            MD3Paint.fill(g2, shape, containerColor());
            MD3Paint.outline(g2, shape, outlineColor(), 1f);

            if (clickable && stateLayer != null) {
                stateLayer.paint(g2, shape, MD3Color.onSurface());
            }

            if (clickable && MD3Focus.isVisible(this)) {
                MD3Paint.focusRingInside(g2, shape, null);
            }
        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }
}
