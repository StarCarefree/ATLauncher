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
package com.atlauncher.gui.md3.nav;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.formdev.flatlaf.util.Animator;

/**
 * Holds the launcher's pages and shows one at a time, arriving rather than appearing.
 *
 * <p>
 * A {@link CardLayout} swap is instantaneous, which leaves the window looking like it was redrawn
 * with different contents. Material's fade-through says the same thing as a movement, in two parts
 * that do not overlap: the page that was there goes, and only then does the one you asked for grow
 * the last few percent into its place and fade up.
 *
 * <p>
 * <b>The page arriving is painted live; only the one leaving is an image.</b> The page you asked
 * for is still assembling itself while the transition runs - the launcher's pages are
 * {@link com.atlauncher.gui.panels.HierarchyPanel}s, which build their contents when they are shown
 * and several of which finish on a later pass of the event queue - so a picture taken at the moment
 * of the swap is a picture of a page that is not there yet. That is what made navigating to the
 * instances or the pack browser fade up an empty window and then snap to the real one. Anything
 * inside the page that moves - a spinner on a page still loading - was frozen for the same reason.
 * The page being left has no such problem and no alternative either: the card layout has already
 * hidden it, so an image is the only way to show it at all.
 *
 * <p>
 * Painting the live tree under a transform means Swing must not repaint any of it on its own, or a
 * child would draw itself at the position it will have when the transition is over. Hence
 * {@link #isPaintingOrigin()} and the widened {@link #repaint(long, int, int, int, int)}: while the
 * transition runs, every repaint that starts anywhere inside becomes a repaint of the whole host.
 */
public class MD3PageHost extends JPanel {
    /**
     * Where the outgoing page has finished leaving and the incoming one starts arriving. Material's
     * fade-through spends the first ninety of its three hundred milliseconds on the exit.
     */
    private static final float HANDOVER = 0.3f;

    /** How small the incoming page starts, as a fraction of its size. */
    private static final float ENTER_SCALE = 0.94f;

    private final CardLayout cards = new CardLayout();

    /**
     * Runs the whole transition, and linearly: each half eases itself, so a curve here would make
     * one of them travel most of its distance before the other had begun.
     */
    private final MD3Animated enter = new MD3Animated(this, 1f, MD3Motion.PAGE_TRANSITION, MD3Motion.LINEAR);

    private BufferedImage leaving;

    public MD3PageHost() {
        setLayout(cards);
        setOpaque(true);
        setBackground(MD3Color.surface());
    }

    /**
     * @param name the key {@link #showPage(String)} will ask for
     */
    public void addPage(Component page, String name) {
        add(page, name);
    }

    /**
     * Shows a page, with the transition if there is anything to animate and instantly if there is
     * not.
     */
    public void showPage(String name) {
        BufferedImage previous = animates() ? capture() : null;

        cards.show(this, name);

        beginEntrance(previous);
    }

    private boolean animates() {
        return Animator.useAnimation() && !MD3Motion.isReduced() && isDisplayable() && getWidth() > 0
                && getHeight() > 0;
    }

    /**
     * @return whether a transition is running, which is the whole time the children are being
     *         painted somewhere other than where they think they are
     */
    private boolean isTransitioning() {
        // null while the superclass constructor is still running: installing a UI repaints, and
        // both overrides below ask this before the field initialisers have run
        return enter != null && enter.value() < 1f;
    }

    private void beginEntrance(BufferedImage previous) {
        enter.stop();
        leaving = null;

        if (!animates()) {
            enter.set(1f);

            return;
        }

        leaving = previous;

        // there is no outgoing page on the first navigation - it happens before the window is on
        // screen - so that one starts at the handover and is the arrival on its own, rather than
        // spending its first ninety milliseconds fading out a page nobody ever saw
        enter.set(leaving == null ? HANDOVER : 0f);
        enter.setTarget(1f);
    }

    /**
     * Paints the page currently showing, once, into an image.
     *
     * <p>
     * At the display's own resolution rather than at the layout's. On a scaled display the two are
     * not the same number, and an image made at the smaller one is stretched back up when it is
     * drawn - which put the entire window through a blur for the length of every navigation, and
     * then snapped it sharp on the last frame.
     */
    private BufferedImage capture() {
        double scale = deviceScale();

        int width = (int) Math.ceil(getWidth() * scale);
        int height = (int) Math.ceil(getHeight() * scale);

        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        try {
            g.scale(scale, scale);

            // super, so this goes to the real implementation rather than back into the override
            // below, which would find a transition already in progress and paint it twice over
            super.paintChildren(g);
        } catch (RuntimeException e) {
            return null;
        } finally {
            g.dispose();
        }

        return image;
    }

    private double deviceScale() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();

        if (configuration == null) {
            return 1;
        }

        double scale = configuration.getDefaultTransform().getScaleX();

        return scale > 0 ? scale : 1;
    }

    /**
     * A child repainting itself mid-transition has to become a repaint of the host, since the host
     * is what knows where the child is currently being drawn.
     */
    @Override
    protected boolean isPaintingOrigin() {
        return isTransitioning();
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        if (isTransitioning()) {
            super.repaint(tm, 0, 0, getWidth(), getHeight());

            return;
        }

        super.repaint(tm, x, y, width, height);
    }

    /**
     * The other way a partial redraw arrives - a viewport blitting a scroll, anything calling this
     * directly. Same answer: under a transform, part of the page cannot be redrawn on its own.
     */
    @Override
    public void paintImmediately(int x, int y, int width, int height) {
        if (isTransitioning()) {
            super.paintImmediately(0, 0, getWidth(), getHeight());

            return;
        }

        super.paintImmediately(x, y, width, height);
    }

    @Override
    protected void paintChildren(Graphics g) {
        float fraction = enter.value();

        if (fraction >= 1f) {
            leaving = null;

            super.paintChildren(g);

            return;
        }

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintTransition(g2, fraction);
        } finally {
            g2.dispose();
        }
    }

    /**
     * The transition at a point along it.
     *
     * <p>
     * Package private so a test can hold it at a moment rather than racing it - the two halves look
     * nothing alike, and which one is on screen is otherwise a question of when you looked.
     */
    void paintTransition(Graphics2D g2, float fraction) {
        if (fraction < HANDOVER) {
            paintLeaving(g2, fraction / HANDOVER);
        } else {
            paintArriving(g2, (fraction - HANDOVER) / (1f - HANDOVER));
        }
    }

    /**
     * The page that was there, on its way out. Accelerating, so it commits to leaving rather than
     * lingering half visible over the whole exit.
     */
    private void paintLeaving(Graphics2D g2, float progress) {
        if (leaving == null) {
            return;
        }

        float alpha = 1f - MD3Motion.EMPHASIZED_ACCELERATE.interpolate(Math.min(1f, progress));

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // drawn back down to the layout's size, which is where it was captured from
        g2.drawImage(leaving, 0, 0, getWidth(), getHeight(), null);
    }

    /**
     * The page you asked for, arriving. It grows the last few percent into place as it fades up -
     * scaled rather than slid, so nothing is clipped off the bottom of the window on the way.
     */
    private void paintArriving(Graphics2D g2, float progress) {
        // the outgoing image is finished with, and it is a window's worth of pixels
        leaving = null;

        float eased = MD3Motion.EMPHASIZED_DECELERATE.interpolate(Math.max(0f, Math.min(1f, progress)));
        float scale = ENTER_SCALE + (1f - ENTER_SCALE) * eased;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, eased))));

        double centreX = getWidth() / 2d;
        double centreY = getHeight() / 2d;

        g2.translate(centreX, centreY);
        g2.scale(scale, scale);
        g2.translate(-centreX, -centreY);

        super.paintChildren(g2);
    }
}
