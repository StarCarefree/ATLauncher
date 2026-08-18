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
package com.atlauncher.gui.md3.feedback;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3CircularProgress}.
 *
 * <p>
 * Determinate is a track with an active arc growing clockwise from twelve o'clock. Indeterminate
 * is Material's expand-and-collapse spinner, timed in milliseconds so it does not change speed
 * with the frame rate. Reduced motion parks the arc at 270° - the shape of a wait - instead of
 * freezing the first 20° frame, which read as a dash.
 */
public class MD3CircularProgressUI extends BasicProgressBarUI {
    private static final int SIZE = 48;
    private static final int STROKE = 4;
    private static final int FRAME_MS = 1000 / 60;
    /** Gap, in degrees, kept between the active arc and the track so they never fuse. */
    private static final float GAP_DEGREES = 12f;

    private Timer timer;
    private long startedAtNanos;
    private final float[] startSweep = new float[2];

    public static ComponentUI createUI(JComponent c) {
        return new MD3CircularProgressUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        progressBar.setOpaque(false);
        progressBar.setBorderPainted(false);
        progressBar.setBorder(null);
    }

    @Override
    public void uninstallUI(JComponent c) {
        stopAnimationTimer();
        super.uninstallUI(c);
    }

    @Override
    protected void startAnimationTimer() {
        if (MD3Motion.isReduced()) {
            if (progressBar != null) {
                progressBar.repaint();
            }

            return;
        }

        startedAtNanos = System.nanoTime();

        if (timer == null) {
            timer = new Timer(FRAME_MS, e -> {
                if (progressBar != null) {
                    progressBar.repaint();
                }
            });
            timer.setRepeats(true);
        }

        if (!timer.isRunning()) {
            timer.start();
        }
    }

    @Override
    protected void stopAnimationTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private float elapsedMs() {
        if (MD3Motion.isReduced() || startedAtNanos == 0L) {
            return 0f;
        }

        return (System.nanoTime() - startedAtNanos) / 1_000_000f;
    }

    private static int diameterOf(JComponent c) {
        Object requested = c.getClientProperty(MD3CircularProgress.DIAMETER_KEY);

        if (requested instanceof Integer && (Integer) requested > 0) {
            return (Integer) requested;
        }

        return SIZE;
    }

    private static float strokeOf(JComponent c) {
        return Math.max(UIScale.scale(1.5f), UIScale.scale(STROKE * diameterOf(c) / (float) SIZE));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int size = UIScale.scale(diameterOf(c));

        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    private Arc2D.Float arcOf(JComponent c, float start, float extent) {
        float stroke = strokeOf(c);
        float size = Math.min(c.getWidth(), c.getHeight()) - stroke;
        float x = (c.getWidth() - size) / 2f;
        float y = (c.getHeight() - size) / 2f;

        return new Arc2D.Float(x, y, size, size, start, extent, Arc2D.OPEN);
    }

    private void stroke(Graphics2D g, JComponent c) {
        g.setStroke(new BasicStroke(strokeOf(c), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            stroke(g2, c);

            int span = progressBar.getMaximum() - progressBar.getMinimum();
            float fraction = span <= 0 ? 0f
                    : Math.max(0f, Math.min(1f, (progressBar.getValue() - progressBar.getMinimum()) / (float) span));

            if (fraction <= 0f) {
                g2.setColor(MD3Color.secondaryContainer());
                g2.draw(arcOf(c, 90, 360));

                return;
            }

            if (fraction >= 1f) {
                g2.setColor(MD3Color.primary());
                g2.draw(arcOf(c, 90, -360));

                return;
            }

            float active = 360f * fraction;
            float gap = Math.min(GAP_DEGREES, (360f - active) / 2f);
            float rest = 360f - active - gap * 2f;

            g2.setColor(MD3Color.secondaryContainer());
            g2.draw(arcOf(c, 90f - active - gap, -rest));

            g2.setColor(MD3Color.primary());
            g2.draw(arcOf(c, 90f, -active));
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            stroke(g2, c);
            MD3ProgressMotion.circularArc(elapsedMs(), startSweep);
            g2.setColor(MD3Color.primary());
            g2.draw(arcOf(c, startSweep[0], startSweep[1]));
        } finally {
            g2.dispose();
        }
    }
}
