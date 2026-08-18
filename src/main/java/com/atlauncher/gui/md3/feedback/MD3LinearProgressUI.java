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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3LinearProgress}.
 *
 * <p>
 * Determinate is Material's current anatomy: a rounded active indicator, a gap, the remaining
 * track, and a stop at the trailing end so a bar at 98% cannot be mistaken for one that has
 * finished. Indeterminate is the two-segment disjoint travel Material ships, driven by elapsed
 * time rather than by Swing's frame counter.
 */
public class MD3LinearProgressUI extends BasicProgressBarUI {
    private static final int TRACK_HEIGHT = MD3Spacing.PROGRESS_TRACK_HEIGHT;
    private static final int GAP = MD3Spacing.XS;
    private static final int STOP_DIAMETER = MD3Spacing.XS;
    private static final int FRAME_MS = 1000 / 60;

    private Timer timer;
    private long startedAtNanos;
    private final float[] segments = new float[4];

    public static ComponentUI createUI(JComponent c) {
        return new MD3LinearProgressUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        progressBar.setOpaque(false);
        progressBar.setBorderPainted(false);
        progressBar.setBorder(null);
        progressBar.setFont(MD3Type.font(MD3Type.BODY_SMALL));
        progressBar.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
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

    private boolean hasCaption() {
        return progressBar.isStringPainted() && progressBar.getString() != null
                && !progressBar.getString().isEmpty();
    }

    private int captionHeight() {
        if (!hasCaption()) {
            return 0;
        }

        return progressBar.getFontMetrics(MD3Type.font(MD3Type.BODY_SMALL)).getHeight()
                + UIScale.scale(MD3Spacing.XS);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(UIScale.scale(120), UIScale.scale(TRACK_HEIGHT) + captionHeight());
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(UIScale.scale(24), UIScale.scale(TRACK_HEIGHT) + captionHeight());
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, UIScale.scale(TRACK_HEIGHT) + captionHeight());
    }

    private void paintCaption(Graphics2D g, JComponent c) {
        if (!hasCaption()) {
            return;
        }

        Font font = MD3Type.font(MD3Type.BODY_SMALL);
        FontMetrics metrics = progressBar.getFontMetrics(font);
        String text = progressBar.getString();
        int x = MD3Paint.isLeftToRight(c) ? 0 : c.getWidth() - MD3MixedText.width(font, text);

        g.setColor(MD3Color.onSurfaceVariant());
        MD3MixedText.draw(g, text, x, metrics.getAscent(), font);
    }

    private static Color trackColor() {
        return MD3Color.secondaryContainer();
    }

    private static Color indicatorColor() {
        return MD3Color.primary();
    }

    private static void fillBar(Graphics2D g, float x, float y, float width, float height, Color color) {
        if (width <= 0f || height <= 0f || color == null) {
            return;
        }

        MD3Paint.fill(g, new RoundRectangle2D.Float(x, y, width, height, height, height), color);
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintCaption(g2, c);
            paintDeterminateBar(g2, c, captionHeight());
        } finally {
            g2.dispose();
        }
    }

    private void paintDeterminateBar(Graphics2D g, JComponent c, float y) {
        float height = UIScale.scale((float) TRACK_HEIGHT);
        float width = c.getWidth();
        float gap = UIScale.scale((float) GAP);
        float stop = UIScale.scale((float) STOP_DIAMETER);
        boolean ltr = MD3Paint.isLeftToRight(c);
        float fraction = fraction();

        float stopRoom = fraction >= 1f ? 0f : stop + gap;
        float indicatorWidth = width * fraction;

        if (fraction > 0f && fraction < 1f) {
            indicatorWidth = Math.max(indicatorWidth, height);
        }

        indicatorWidth = Math.min(indicatorWidth, Math.max(0f, width - stopRoom));

        float indicatorStart = ltr ? 0f : width - indicatorWidth;
        float trackStart;
        float trackWidth;

        if (fraction >= 1f) {
            trackStart = 0f;
            trackWidth = 0f;
        } else if (ltr) {
            trackStart = Math.min(width, indicatorWidth + (indicatorWidth > 0f ? gap : 0f));
            trackWidth = Math.max(0f, width - stop - gap / 2f - trackStart);
        } else {
            trackWidth = Math.max(0f, width - indicatorWidth - (indicatorWidth > 0f ? gap : 0f) - stop - gap / 2f);
            trackStart = stop + gap / 2f;
        }

        fillBar(g, trackStart, y, trackWidth, height, trackColor());
        fillBar(g, indicatorStart, y, indicatorWidth, height, indicatorColor());

        if (fraction < 1f) {
            float stopX = ltr ? width - stop : 0f;
            MD3Paint.fill(g, new Ellipse2D.Float(stopX, y + (height - stop) / 2f, stop, stop), indicatorColor());
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintCaption(g2, c);

            float height = UIScale.scale((float) TRACK_HEIGHT);
            float y = captionHeight();
            float width = c.getWidth();
            boolean ltr = MD3Paint.isLeftToRight(c);

            fillBar(g2, 0, y, width, height, trackColor());

            if (MD3Motion.isReduced()) {
                float segment = width * 0.4f;
                float x = (width - segment) / 2f;
                fillBar(g2, x, y, segment, height, indicatorColor());

                return;
            }

            float cycle = (elapsedMs() / MD3ProgressMotion.LINEAR_PERIOD_MS)
                    - (float) Math.floor(elapsedMs() / MD3ProgressMotion.LINEAR_PERIOD_MS);
            MD3ProgressMotion.linearSegments(cycle, segments);

            drawSegment(g2, width, y, height, segments[0], segments[1], ltr);
            drawSegment(g2, width, y, height, segments[2], segments[3], ltr);
        } finally {
            g2.dispose();
        }
    }

    private static void drawSegment(Graphics2D g, float width, float y, float height, float start, float end,
            boolean ltr) {
        if (end - start <= 0.001f) {
            return;
        }

        float left = ltr ? start * width : (1f - end) * width;
        fillBar(g, left, y, (end - start) * width, height, indicatorColor());
    }

    private float fraction() {
        JProgressBar bar = progressBar;
        int span = bar.getMaximum() - bar.getMinimum();

        if (span <= 0) {
            return 0f;
        }

        return Math.max(0f, Math.min(1f, (bar.getValue() - bar.getMinimum()) / (float) span));
    }
}
