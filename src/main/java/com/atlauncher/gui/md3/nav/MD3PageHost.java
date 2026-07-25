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
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * Holds the launcher's pages and shows one at a time, arriving rather than appearing.
 *
 * <p>
 * A {@link CardLayout} swap is instantaneous, which leaves the window looking like it was redrawn
 * with different contents. Material's fade-through says the same thing as a movement: the page that
 * was there goes, and the one you asked for rises a little and fades up into its place.
 *
 * <p>
 * <b>The page is painted once into an image and the image is what animates.</b> Fading a live
 * component tree means every child repainting on every frame, and the instances page can hold a
 * hundred cards - the cost of the transition would scale with how much is on the page, which is
 * exactly backwards. This way it is one image blit per frame whatever the page is.
 */
public class MD3PageHost extends JPanel {
    /** How far below its place the page starts, unscaled. Small enough to read as settling. */
    private static final int RISE = 12;

    private final CardLayout cards = new CardLayout();
    private final MD3Animated enter = new MD3Animated(this, 1f, MD3Motion.PAGE_TRANSITION,
            MD3Motion.EMPHASIZED_DECELERATE);

    private BufferedImage snapshot;

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
     * Shows a page, with the entrance if there is anything to animate and instantly if there is not.
     */
    public void showPage(String name) {
        cards.show(this, name);

        beginEntrance();
    }

    private void beginEntrance() {
        enter.stop();
        snapshot = null;

        if (!Animator.useAnimation() || MD3Motion.isReduced() || !isDisplayable() || getWidth() <= 0
                || getHeight() <= 0) {
            enter.set(1f);

            return;
        }

        snapshot = capture();

        if (snapshot == null) {
            enter.set(1f);

            return;
        }

        enter.set(0f);
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

        // the last frame an animation delivers is exactly 1, which is where the image is done with
        // and the live page takes over - so there is no completion callback to wire up
        if (snapshot == null || fraction >= 1f || snapshot.getWidth() != getWidth()
                || snapshot.getHeight() != getHeight()) {
            snapshot = null;

            super.paintChildren(g);

            return;
        }

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, fraction)));
            g2.drawImage(snapshot, 0, Math.round((1f - fraction) * UIScale.scale(RISE)), null);
        } finally {
            g2.dispose();
        }
    }
}
