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
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3CircularProgress}.
 *
 * <p>
 * Both the sweep and the rotation advance while indeterminate, at different rates - that is what
 * gives the Material spinner its characteristic stretch and recoil instead of a mechanical
 * constant-speed arc.
 */
public class MD3CircularProgressUI extends BasicProgressBarUI {
    private static final int SIZE = 48;
    private static final int STROKE = 4;

    /** Degrees the arc spans at its longest while indeterminate. */
    private static final float MAX_SWEEP = 270f;
    /** Degrees the arc spans at its shortest. */
    private static final float MIN_SWEEP = 20f;
    /** Full rotations the arc makes per animation cycle. */
    private static final float ROTATIONS_PER_CYCLE = 2f;
    /** Stretch-and-recoil cycles per animation cycle. */
    private static final float SWEEPS_PER_CYCLE = 3f;

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
    public Dimension getPreferredSize(JComponent c) {
        int size = UIScale.scale(SIZE);

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
        float stroke = UIScale.scale((float) STROKE);
        float size = Math.min(c.getWidth(), c.getHeight()) - stroke;
        float x = (c.getWidth() - size) / 2f;
        float y = (c.getHeight() - size) / 2f;

        return new Arc2D.Float(x, y, size, size, start, extent, Arc2D.OPEN);
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            float stroke = UIScale.scale((float) STROKE);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int span = progressBar.getMaximum() - progressBar.getMinimum();
            float fraction = span <= 0 ? 0f
                    : Math.max(0f, Math.min(1f, (progressBar.getValue() - progressBar.getMinimum()) / (float) span));

            g2.setColor(MD3Color.surfaceContainerHighest());
            g2.draw(arcOf(c, 90, 360));

            if (fraction > 0f) {
                g2.setColor(MD3Color.primary());
                // negative extent so progress runs clockwise from twelve o'clock
                g2.draw(arcOf(c, 90, -360f * fraction));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            float stroke = UIScale.scale((float) STROKE);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int frameCount = Math.max(1, getFrameCount());
            float cycle = (getAnimationIndex() % frameCount) / (float) frameCount;

            float rotation = 360f * ROTATIONS_PER_CYCLE * cycle;
            float breathe = (float) (1 - Math.cos(cycle * SWEEPS_PER_CYCLE * 2 * Math.PI)) / 2f;
            float sweep = MIN_SWEEP + (MAX_SWEEP - MIN_SWEEP) * breathe;

            g2.setColor(MD3Color.primary());
            g2.draw(arcOf(c, 90 - rotation, -sweep));
        } finally {
            g2.dispose();
        }
    }
}
