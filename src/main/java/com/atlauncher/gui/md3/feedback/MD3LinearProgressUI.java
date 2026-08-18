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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3LinearProgress}.
 *
 * <p>
 * The Material 3 shape: a 4dp rounded track, a rounded active indicator, a gap between the two, and
 * a stop dot at the far end. The gap and the dot are what make a nearly-full bar readable - without
 * them the indicator merges into the track and "98%" looks identical to "done".
 *
 * <p>
 * A 4dp bar has no room for a caption, so a bar with {@code stringPainted} set grows to make space
 * above itself rather than trying to print inside the track.
 */
public class MD3LinearProgressUI extends BasicProgressBarUI {
    private static final int TRACK_HEIGHT = MD3Spacing.PROGRESS_TRACK_HEIGHT;
    private static final int GAP = MD3Spacing.XS;
    private static final int STOP_DIAMETER = MD3Spacing.XS;

    /** Fraction of the track the travelling segment covers while indeterminate. */
    private static final float SWEEP_WIDTH = 0.35f;

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

    private void paintCaption(Graphics2D g) {
        if (!hasCaption()) {
            return;
        }

        FontMetrics metrics = progressBar.getFontMetrics(MD3Type.font(MD3Type.BODY_SMALL));

        g.setFont(MD3Type.font(MD3Type.BODY_SMALL));
        g.setColor(MD3Color.onSurfaceVariant());
        g.drawString(progressBar.getString(), 0, metrics.getAscent());
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintCaption(g2);

            float height = UIScale.scale((float) TRACK_HEIGHT);
            float y = captionHeight();
            float width = c.getWidth();
            float gap = UIScale.scale((float) GAP);
            float stop = UIScale.scale((float) STOP_DIAMETER);

            float fraction = fraction();
            float indicatorWidth = width * fraction;

            Color track = MD3Color.surfaceContainerHighest();
            Color indicator = MD3Color.primary();

            // the track resumes after a gap rather than running under the indicator, so the two
            // never blur into one another at high values
            float trackStart = Math.min(width, indicatorWidth + gap);

            if (trackStart < width) {
                MD3Paint.fill(g2, MD3Shape.rounded(trackStart, y, width - trackStart, height, MD3Shape.FULL), track);
            }

            if (indicatorWidth > 0f) {
                MD3Paint.fill(g2, MD3Shape.rounded(0, y, indicatorWidth, height, MD3Shape.FULL), indicator);
            }

            // the stop dot marks where "complete" is, so a bar at 98% still reads as unfinished
            if (fraction < 1f) {
                MD3Paint.fill(g2, new Ellipse2D.Float(width - stop, y + (height - stop) / 2f, stop, stop), indicator);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintCaption(g2);

            float height = UIScale.scale((float) TRACK_HEIGHT);
            float y = captionHeight();
            float width = c.getWidth();

            MD3Paint.fill(g2, MD3Shape.rounded(0, y, width, height, MD3Shape.FULL),
                    MD3Color.surfaceContainerHighest());

            int frameCount = Math.max(1, getFrameCount());
            float cycle = (getAnimationIndex() % frameCount) / (float) frameCount;

            // travel the full width plus the segment's own length, so it enters and leaves cleanly
            // instead of appearing and vanishing at the edges
            float segment = width * SWEEP_WIDTH;
            float x = -segment + (width + segment) * cycle;
            float start = Math.max(0f, x);
            float end = Math.min(width, x + segment);

            if (end > start) {
                MD3Paint.fill(g2, MD3Shape.rounded(start, y, end - start, height, MD3Shape.FULL),
                        MD3Color.primary());
            }
        } finally {
            g2.dispose();
        }
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
