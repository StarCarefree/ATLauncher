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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

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
     * <p>
     * By pulling the shape in half a line width and stroking that, rather than by stroking at double
     * width and clipping the outer half away. A clip in Java2D is a hard-edged region and takes no
     * part in antialiasing, so the clipped version handed every outlined button, card, badge and
     * switch track a visibly stepped outer edge around each corner - and did it while
     * {@link #setup} was asking for pure stroke control two lines earlier.
     *
     * @param widthDp unscaled line width
     */
    public static void outline(Graphics2D g, Shape shape, Color color, float widthDp) {
        if (color == null || widthDp <= 0f) {
            return;
        }

        float width = UIScale.scale(widthDp);

        stroke(g, insetBy(shape, width / 2f), color, width);
    }

    /**
     * Strokes a shape centred on its own outline, for a caller that has already pulled the geometry
     * in by however much it wants clear.
     *
     * @param width line width in pixels, already scaled
     */
    public static void stroke(Graphics2D g, Shape shape, Color color, float width) {
        if (color == null || width <= 0f) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(width));
        g2.draw(shape);
        g2.dispose();
    }

    /**
     * A shape pulled in by the given number of pixels on every side, corners included.
     *
     * <p>
     * Exact for the rounded boxes every Material component is built from. Anything else - a path with
     * independently rounded corners, which is what a segmented button's end caps are - is scaled
     * about its centre instead, which shrinks its radii in proportion rather than by a constant. At
     * the half-pixel insets an outline asks for, on a control tens of pixels across, the difference
     * is below what a pixel can show; a caller that cannot accept even that builds its own inset
     * shape and calls {@link #stroke} directly, as the buttons do for their focus ring.
     */
    public static Shape insetBy(Shape shape, float inset) {
        if (shape == null || inset <= 0f) {
            return shape;
        }

        if (shape instanceof RoundRectangle2D) {
            RoundRectangle2D box = (RoundRectangle2D) shape;
            double width = box.getWidth() - inset * 2d;
            double height = box.getHeight() - inset * 2d;

            if (width <= 0d || height <= 0d) {
                return shape;
            }

            return new RoundRectangle2D.Double(box.getX() + inset, box.getY() + inset, width, height,
                    Math.max(0d, box.getArcWidth() - inset * 2d), Math.max(0d, box.getArcHeight() - inset * 2d));
        }

        if (shape instanceof Ellipse2D) {
            Ellipse2D oval = (Ellipse2D) shape;
            double width = oval.getWidth() - inset * 2d;
            double height = oval.getHeight() - inset * 2d;

            if (width <= 0d || height <= 0d) {
                return shape;
            }

            return new Ellipse2D.Double(oval.getX() + inset, oval.getY() + inset, width, height);
        }

        Rectangle2D bounds = shape.getBounds2D();

        if (bounds.getWidth() <= inset * 2d || bounds.getHeight() <= inset * 2d) {
            return shape;
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(bounds.getCenterX(), bounds.getCenterY());
        transform.scale((bounds.getWidth() - inset * 2d) / bounds.getWidth(),
                (bounds.getHeight() - inset * 2d) / bounds.getHeight());
        transform.translate(-bounds.getCenterX(), -bounds.getCenterY());

        return transform.createTransformedShape(shape);
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
     * How far past a shape its shadow reaches at a given level, unscaled, so a component can keep
     * that much of its own bounds clear for it.
     *
     * <p>
     * Swing clips painting to the component, so a shadow drawn at the very edge of the container has
     * nowhere to fall and is spent entirely on pixels that are then covered by the container itself.
     * Anything calling {@link #shadow} needs room for it or should not be calling it at all.
     */
    public static int shadowRoom(int level) {
        return MD3Elevation.shadowBlur(level);
    }

    /**
     * The same, below the shape, where the shadow's own downward offset lands as well.
     */
    public static int shadowRoomBelow(int level) {
        return MD3Elevation.shadowBlur(level) + MD3Elevation.shadowOffsetY(level);
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

    /** Unscaled width of the focus indicator, and the gap it keeps from the outline it marks. */
    public static final int FOCUS_RING_WIDTH = 3;
    private static final int FOCUS_RING_GAP = 2;

    /**
     * The one colour a focus indicator is ever drawn in.
     *
     * <p>
     * Shared because it was not: the buttons drew a 2dp secondary ring, the rail and the tabs a 3dp
     * one, and {@code MD3Bridge} gave every component still painted by FlatLaf a 2dp primary ring at
     * 45% alpha. One pass of the tab key showed three different answers to the same question.
     */
    public static Color focusRingColor() {
        return MD3Color.get(MD3Color.SECONDARY);
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
        float stroke = UIScale.scale((float) FOCUS_RING_WIDTH);
        float offset = UIScale.scale((float) FOCUS_RING_GAP) + stroke / 2f;

        stroke(g, MD3Shape.rounded(x - offset, y - offset, width + offset * 2f, height + offset * 2f, radius),
                focusRingColor(), stroke);
    }

    /**
     * The same ring, drawn just inside a shape instead of around it.
     *
     * <p>
     * For a component that fills its own bounds - a button, a card - where Material's ring would fall
     * entirely in the pixels Swing clips away. Same width and same colour as the outside form, so the
     * two read as one indicator wherever the tab key lands.
     *
     * @param color the ring's colour, for the error tone that wants its own; null for the default
     */
    public static void focusRingInside(Graphics2D g, Shape shape, Color color) {
        float width = UIScale.scale((float) FOCUS_RING_WIDTH);

        stroke(g, insetBy(shape, width / 2f), color != null ? color : focusRingColor(), width);
    }

    /**
     * Whether a component reads left to right, and so whether its leading edge is its left one.
     *
     * <p>
     * Null and unrealised components read left to right, which is the launcher's own default and what
     * an offscreen render test gets.
     */
    public static boolean isLeftToRight(Component c) {
        return c == null || c.getComponentOrientation().isLeftToRight();
    }

    /**
     * An x position measured from the leading edge, turned into one measured from the left.
     *
     * <p>
     * Everything Material calls leading or trailing is a side of the component rather than a
     * direction, and only the first of those survives being written as {@code getWidth() - pad}.
     *
     * @param itemWidth the width of the thing being placed, so it lands inside the component rather
     *                  than ending where it should have started
     */
    public static int mirrorX(Component c, int x, int itemWidth) {
        return isLeftToRight(c) ? x : c.getWidth() - x - itemWidth;
    }

    /**
     * Scales an unscaled inset set for the current display.
     */
    public static Insets scale(Insets insets) {
        return new Insets(UIScale.scale(insets.top), UIScale.scale(insets.left), UIScale.scale(insets.bottom),
                UIScale.scale(insets.right));
    }

    /**
     * Writes leading/trailing padding into left/right, honouring the component's writing direction.
     *
     * @param leading  already-scaled padding on the start edge
     * @param trailing already-scaled padding on the end edge
     */
    public static void setLeadingTrailing(Insets insets, Component c, int top, int leading, int bottom,
            int trailing) {
        if (isLeftToRight(c)) {
            insets.set(top, leading, bottom, trailing);
        } else {
            insets.set(top, trailing, bottom, leading);
        }
    }
}
