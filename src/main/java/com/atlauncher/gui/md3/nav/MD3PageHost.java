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
 * The outgoing half is the half that used to be missing. The old page vanished on the frame the
 * card layout switched and the new one faded up out of the background, which reads as the window
 * having been cleared and repainted rather than as one page replacing another - and it gave the eye
 * nothing to follow across the gap.
 *
 * <p>
 * <b>Each page is painted once into an image and the images are what animate.</b> Fading a live
 * component tree means every child repainting on every frame, and the instances page can hold a
 * hundred cards - the cost of the transition would scale with how much is on the page, which is
 * exactly backwards. This way it is one image blit per frame whatever the page is.
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

    private BufferedImage snapshot;
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

    private void beginEntrance(BufferedImage previous) {
        enter.stop();
        snapshot = null;
        leaving = null;

        if (!animates()) {
            enter.set(1f);

            return;
        }

        snapshot = capture();

        if (snapshot == null) {
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
     * Paints the page that was just switched to, once, into an image.
     */
    private BufferedImage capture() {
        // the card layout has changed which child is visible but the container has not been laid out
        // since, so an un-validated page would be captured at whatever size it last held
        validate();

        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        try {
            // super, so this goes to the real implementation rather than back into the override
            // below, which would find the half-built snapshot and paint nothing at all
            super.paintChildren(g);
        } catch (RuntimeException e) {
            return null;
        } finally {
            g.dispose();
        }

        return image;
    }

    @Override
    protected void paintChildren(Graphics g) {
        float fraction = enter.value();

        // the last frame an animation delivers is exactly 1, which is where the images are done
        // with and the live page takes over - so there is no completion callback to wire up
        if (snapshot == null || fraction >= 1f || snapshot.getWidth() != getWidth()
                || snapshot.getHeight() != getHeight()) {
            snapshot = null;
            leaving = null;

            super.paintChildren(g);

            return;
        }

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            if (fraction < HANDOVER) {
                paintLeaving(g2, fraction / HANDOVER);
            } else {
                paintArriving(g2, (fraction - HANDOVER) / (1f - HANDOVER));
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * The page that was there, on its way out. Accelerating, so it commits to leaving rather than
     * lingering half visible over the whole exit.
     */
    private void paintLeaving(Graphics2D g2, float progress) {
        if (leaving == null || leaving.getWidth() != getWidth() || leaving.getHeight() != getHeight()) {
            return;
        }

        float alpha = 1f - MD3Motion.EMPHASIZED_ACCELERATE.interpolate(Math.min(1f, progress));

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        g2.drawImage(leaving, 0, 0, null);
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

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(snapshot, 0, 0, null);
    }
}
