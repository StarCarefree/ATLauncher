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
package com.atlauncher.gui.md3.input;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 slider.
 *
 * <p>
 * A continuous value on a track. The launcher's memory and timeout settings stay as
 * {@link MD3Spinner}s - those have to land on a step - and this is for anything that is a range
 * rather than a count.
 */
public class MD3Slider extends JSlider {
    private static final int TRACK = MD3Spacing.PROGRESS_TRACK_HEIGHT;
    private static final int THUMB = 20;
    private static final int HALO = 40;

    public MD3Slider(int min, int max, int value) {
        super(min, max, value);

        setOpaque(false);
        setFocusable(true);
        setUI(new SliderUI(this));
    }

    @Override
    public void updateUI() {
        setUI(new SliderUI(this));
    }

    private static final class SliderUI extends BasicSliderUI {
        SliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void installUI(javax.swing.JComponent c) {
            super.installUI(c);
            c.setOpaque(false);
        }

        @Override
        protected Dimension getThumbSize() {
            int size = UIScale.scale(HALO);

            return new Dimension(size, size);
        }

        @Override
        public Dimension getPreferredHorizontalSize() {
            return new Dimension(UIScale.scale(200), UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET));
        }

        @Override
        public Dimension getPreferredSize(javax.swing.JComponent c) {
            Dimension size = super.getPreferredSize(c);
            size.height = Math.max(size.height, UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET));

            return size;
        }

        @Override
        public void paintFocus(Graphics g) {
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                float height = UIScale.scale((float) TRACK);
                float y = trackRect.y + (trackRect.height - height) / 2f;
                float x = trackRect.x;
                float width = trackRect.width;
                boolean ltr = MD3Paint.isLeftToRight(slider);
                float fraction = slider.getMaximum() == slider.getMinimum() ? 0f
                        : (slider.getValue() - slider.getMinimum())
                                / (float) (slider.getMaximum() - slider.getMinimum());

                if (!ltr) {
                    fraction = 1f - fraction;
                }

                Color track = slider.isEnabled() ? MD3Color.surfaceContainerHighest()
                        : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
                Color active = slider.isEnabled() ? MD3Color.primary()
                        : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());

                MD3Paint.fill(g2, MD3Shape.rounded(x, y, width, height, MD3Shape.FULL), track);

                float filled = width * fraction;

                if (filled > 0f) {
                    MD3Paint.fill(g2, MD3Shape.rounded(x, y, filled, height, MD3Shape.FULL), active);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                float cx = thumbRect.x + thumbRect.width / 2f;
                float cy = thumbRect.y + thumbRect.height / 2f;
                float thumb = UIScale.scale((float) THUMB);
                boolean enabled = slider.isEnabled();
                Color fill = enabled ? MD3Color.primary()
                        : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());

                float state = enabled
                        ? MD3State.opacityFor(false, MD3Focus.isVisible(slider), isDragging(), false)
                        : 0f;

                if (state > 0f) {
                    float halo = UIScale.scale((float) HALO);
                    MD3Paint.stateLayer(g2, new Ellipse2D.Float(cx - halo / 2f, cy - halo / 2f, halo, halo), fill,
                            state);
                }

                Shape knob = new Ellipse2D.Float(cx - thumb / 2f, cy - thumb / 2f, thumb, thumb);
                MD3Paint.fill(g2, knob, fill);

                if (enabled && MD3Focus.isVisible(slider)) {
                    MD3Paint.focusRing(g2, cx - thumb / 2f, cy - thumb / 2f, thumb, thumb, MD3Shape.FULL);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
