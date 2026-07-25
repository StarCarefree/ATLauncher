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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.JComponent;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.formdev.flatlaf.util.UIScale;

/**
 * Shared drawing for the Material components.
 *
 * <p>
 * Everything here works in the component's own coordinate space and leaves the caller's
 * {@link Graphics} untouched - each method either takes an already-prepared {@link Graphics2D} or
 * disposes of the copy it made.
 */
public final class MD3Paint {
    private MD3Paint() {
    }

    /**
     * A private copy of the graphics with antialiasing on and stroke control set to pure, which is
     * what keeps a one pixel outline from drifting half a pixel at fractional scaling factors.
     *
     * <p>
     * The caller owns the result and must dispose it.
     */
    public static Graphics2D setup(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        return g2;
    }

    /**
     * The rounded outline of a component, inset by the given amount on every side.
     */
    public static Shape shapeOf(JComponent c, int radius, float inset) {
        return MD3Shape.rounded(inset, inset, c.getWidth() - inset * 2f, c.getHeight() - inset * 2f, radius);
    }

    public static Shape shapeOf(JComponent c, int radius) {
        return shapeOf(c, radius, 0f);
    }

    public static void fill(Graphics2D g, Shape shape, Color color) {
        if (color == null) {
            return;
        }

        g.setColor(color);
        g.fill(shape);
    }

    /**
     * Strokes a shape with the line drawn <em>inside</em> it, so an outlined component occupies
     * exactly the bounds it was given rather than bleeding half a line width past them.
     *
     * @param widthDp unscaled line width
     */
    public static void outline(Graphics2D g, Shape shape, Color color, float widthDp) {
        if (color == null || widthDp <= 0f) {
            return;
        }

        float width = UIScale.scale(widthDp);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(shape);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(width * 2f));
        g2.draw(shape);
        g2.dispose();
    }

    /**
     * Paints an interaction state layer - the component's content colour laid over its container at
     * a low opacity, which is how Material expresses hover, focus and press.
     */
    public static void stateLayer(Graphics2D g, Shape shape, Color content, float alpha) {
        if (content == null || alpha <= 0f) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
        g2.setColor(content);
        g2.fill(shape);
        g2.dispose();
    }

    /**
     * Paints a drop shadow under a shape.
     *
     * <p>
     * Approximated by stacking progressively wider strokes rather than by convolving a blur, which
     * at these radii is visually indistinguishable and costs a handful of draws instead of an
     * offscreen buffer per repaint. Still, this is the expensive call in this class - reserve it
     * for things that genuinely float, and let everything else express height through
     * {@link MD3Elevation#surfaceRole(int)}.
     */
    public static void shadow(Graphics2D g, Shape shape, int level) {
        shadow(g, shape, (float) level);
    }

    /**
     * The same shadow at a height between two levels, for a component on its way up or down.
     *
     * <p>
     * A whole number reproduces {@link #shadow(Graphics2D, Shape, int)} exactly - the interpolation
     * has nothing to do at zero - so a component that never animates its height is unaffected by
     * going through here.
     */
    public static void shadow(Graphics2D g, Shape shape, float level) {
        int lower = (int) Math.floor(level);
        float between = level - lower;

        int blur = UIScale.scale(Math.round(MD3Animated.lerp(MD3Elevation.shadowBlur(lower),
                MD3Elevation.shadowBlur(lower + 1), between)));
        float alpha = MD3Animated.lerp(MD3Elevation.shadowAlpha(lower), MD3Elevation.shadowAlpha(lower + 1),
                between);
        int offsetY = Math.round(MD3Animated.lerp(MD3Elevation.shadowOffsetY(lower),
                MD3Elevation.shadowOffsetY(lower + 1), between));

        if (blur <= 0 || alpha <= 0f) {
            return;
        }

        Color shadow = MD3Color.get(MD3Color.SHADOW);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(0, UIScale.scale(offsetY));
        g2.setColor(shadow);

        for (int i = blur; i >= 1; i--) {
            float ringAlpha = alpha * (1f - (float) i / (blur + 1f)) / blur;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, ringAlpha)));
            g2.setStroke(new BasicStroke(i * 2f));
            g2.draw(shape);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.4f)));
        g2.fill(shape);
        g2.dispose();
    }

    /**
     * Paints the focus indicator - a ring drawn outside the component's own outline, so it reads as
     * a separate marker rather than as a thicker border.
     */
    public static void focusRing(Graphics2D g, JComponent c, int radius) {
        focusRing(g, 0, 0, c.getWidth(), c.getHeight(), radius);
    }

    /**
     * The same ring around part of a component, for one that draws more than one thing.
     *
     * <p>
     * The navigation rail is a single tab stop whose arrow keys move between destinations, so the
     * ring belongs on the destination's indicator rather than around the whole item - it marks what
     * the arrow keys would move away from, not where the tab landed.
     */
    public static void focusRing(Graphics2D g, float x, float y, float width, float height, int radius) {
        float stroke = UIScale.scale(3f);
        float offset = UIScale.scale(2f) + stroke / 2f;

        Shape ring = MD3Shape.rounded(x - offset, y - offset, width + offset * 2f, height + offset * 2f, radius);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(MD3Color.get(MD3Color.SECONDARY));
        g2.setStroke(new BasicStroke(stroke));
        g2.draw(ring);
        g2.dispose();
    }

    /**
     * Scales an unscaled inset set for the current display.
     */
    public static Insets scale(Insets insets) {
        return new Insets(UIScale.scale(insets.top), UIScale.scale(insets.left), UIScale.scale(insets.bottom),
                UIScale.scale(insets.right));
    }
}
