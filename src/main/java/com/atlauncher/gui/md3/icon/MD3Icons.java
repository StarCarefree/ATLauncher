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
package com.atlauncher.gui.md3.icon;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The icon set, drawn rather than loaded.
 *
 * <p>
 * These are geometric glyphs on Material's 24 by 24 grid with its 2dp stroke weight. Drawing them
 * keeps them resolution independent, recolourable per theme, and free of any asset pipeline - which
 * matters here because the launcher currently ships eleven PNGs in light and dark pairs and has no
 * way to tint anything.
 *
 * <p>
 * Richer pictorial glyphs do not belong in code. {@link MD3SvgIcon} loads those from
 * {@code /assets/icon/md3/}, so a Material Symbols export can be dropped in without touching this
 * class.
 */
public final class MD3Icons {
    private static final float STROKE = 2f;

    private MD3Icons() {
    }

    private static void stroke(Graphics2D g, Shape... shapes) {
        g.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (Shape shape : shapes) {
            g.draw(shape);
        }
    }

    private static Path2D.Float path(float... points) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(points[0], points[1]);

        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }

        return path;
    }

    private static Ellipse2D.Float dot(float cx, float cy, float r) {
        return new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f);
    }

    public static final MD3Icon.Painter CLOSE = g -> stroke(g,
            new Line2D.Float(6, 6, 18, 18), new Line2D.Float(18, 6, 6, 18));

    public static final MD3Icon.Painter CHECK = g -> stroke(g, path(5, 12.5f, 9.5f, 17, 19, 7));

    public static final MD3Icon.Painter CHEVRON_DOWN = g -> stroke(g, path(6.5f, 9.5f, 12, 15, 17.5f, 9.5f));

    public static final MD3Icon.Painter CHEVRON_UP = g -> stroke(g, path(6.5f, 14.5f, 12, 9, 17.5f, 14.5f));

    public static final MD3Icon.Painter CHEVRON_LEFT = (MD3Icon.Directional) g -> stroke(g,
            path(14.5f, 6.5f, 9, 12, 14.5f, 17.5f));

    public static final MD3Icon.Painter CHEVRON_RIGHT = (MD3Icon.Directional) g -> stroke(g,
            path(9.5f, 6.5f, 15, 12, 9.5f, 17.5f));

    public static final MD3Icon.Painter ARROW_BACK = (MD3Icon.Directional) g -> stroke(g,
            new Line2D.Float(19, 12, 5, 12), path(11, 6, 5, 12, 11, 18));

    public static final MD3Icon.Painter ARROW_FORWARD = (MD3Icon.Directional) g -> stroke(g,
            new Line2D.Float(5, 12, 19, 12), path(13, 6, 19, 12, 13, 18));

    public static final MD3Icon.Painter ARROW_UPWARD = g -> stroke(g,
            new Line2D.Float(12, 19, 12, 5), path(6, 11, 12, 5, 18, 11));

    public static final MD3Icon.Painter ARROW_DOWNWARD = g -> stroke(g,
            new Line2D.Float(12, 5, 12, 19), path(6, 13, 12, 19, 18, 13));

    public static final MD3Icon.Painter ADD = g -> stroke(g,
            new Line2D.Float(12, 5, 12, 19), new Line2D.Float(5, 12, 19, 12));

    public static final MD3Icon.Painter REMOVE = g -> stroke(g, new Line2D.Float(5, 12, 19, 12));

    public static final MD3Icon.Painter SEARCH = g -> stroke(g,
            dot(10.5f, 10.5f, 5.5f), new Line2D.Float(14.6f, 14.6f, 19, 19));

    public static final MD3Icon.Painter MENU = g -> stroke(g,
            new Line2D.Float(4, 6.5f, 20, 6.5f), new Line2D.Float(4, 12, 20, 12),
            new Line2D.Float(4, 17.5f, 20, 17.5f));

    public static final MD3Icon.Painter MORE_VERT = g -> {
        g.fill(dot(12, 5.5f, 1.75f));
        g.fill(dot(12, 12, 1.75f));
        g.fill(dot(12, 18.5f, 1.75f));
    };

    public static final MD3Icon.Painter MORE_HORIZ = g -> {
        g.fill(dot(5.5f, 12, 1.75f));
        g.fill(dot(12, 12, 1.75f));
        g.fill(dot(18.5f, 12, 1.75f));
    };

    public static final MD3Icon.Painter PLAY = g -> {
        Path2D.Float triangle = path(8.5f, 5.5f, 19, 12, 8.5f, 18.5f);
        triangle.closePath();
        g.fill(triangle);
    };

    // the tracks break either side of each handle, as they do in the Material original - drawing
    // them straight through makes the handles read as blobs rather than as controls
    public static final MD3Icon.Painter TUNE = g -> stroke(g,
            new Line2D.Float(4, 7, 6.4f, 7), new Line2D.Float(11.6f, 7, 20, 7),
            new Line2D.Float(4, 12, 12.4f, 12), new Line2D.Float(17.6f, 12, 20, 12),
            new Line2D.Float(4, 17, 5.4f, 17), new Line2D.Float(10.6f, 17, 20, 17),
            dot(9, 7, 2.2f), dot(15, 12, 2.2f), dot(8, 17, 2.2f));

    public static final MD3Icon.Painter SORT = g -> stroke(g,
            new Line2D.Float(4, 7, 20, 7), new Line2D.Float(4, 12, 15, 12), new Line2D.Float(4, 17, 10, 17));

    public static final MD3Icon.Painter GRID_VIEW = g -> stroke(g,
            new RoundRectangle2D.Float(4, 4, 7, 7, 2, 2), new RoundRectangle2D.Float(13, 4, 7, 7, 2, 2),
            new RoundRectangle2D.Float(4, 13, 7, 7, 2, 2), new RoundRectangle2D.Float(13, 13, 7, 7, 2, 2));

    public static final MD3Icon.Painter LIST_VIEW = g -> {
        stroke(g, new Line2D.Float(9, 6.5f, 20, 6.5f), new Line2D.Float(9, 12, 20, 12),
                new Line2D.Float(9, 17.5f, 20, 17.5f));
        g.fill(dot(5, 6.5f, 1.5f));
        g.fill(dot(5, 12, 1.5f));
        g.fill(dot(5, 17.5f, 1.5f));
    };

    public static final MD3Icon.Painter FOLDER = g -> {
        Path2D.Float folder = new Path2D.Float();
        folder.moveTo(3.5f, 7);
        folder.lineTo(9.5f, 7);
        folder.lineTo(11.5f, 9.5f);
        folder.lineTo(20.5f, 9.5f);
        folder.lineTo(20.5f, 18);
        folder.lineTo(3.5f, 18);
        folder.closePath();
        stroke(g, folder);
    };

    public static final MD3Icon.Painter DOWNLOAD = g -> stroke(g,
            new Line2D.Float(12, 4, 12, 15), path(7.5f, 10.5f, 12, 15, 16.5f, 10.5f),
            path(4.5f, 15.5f, 4.5f, 19.5f, 19.5f, 19.5f, 19.5f, 15.5f));

    public static final MD3Icon.Painter REFRESH = g -> {
        Path2D.Float arc = new Path2D.Float();
        arc.append(new Arc2D.Float(4.5f, 4.5f, 15, 15, 60, 285, Arc2D.OPEN), false);
        stroke(g, arc);

        Path2D.Float head = path(15.5f, 3.5f, 18.5f, 8, 13.5f, 8.5f);
        head.closePath();
        g.fill(head);
    };

    public static final MD3Icon.Painter SETTINGS = g -> {
        // a gear: an outer ring with eight teeth punched around it and the hub removed
        Area gear = new Area(dot(12, 12, 7.4f));

        for (int i = 0; i < 8; i++) {
            Area tooth = new Area(new RoundRectangle2D.Float(10.6f, 2.6f, 2.8f, 5f, 1.2f, 1.2f));
            tooth.transform(AffineTransform.getRotateInstance(Math.PI / 4 * i, 12, 12));
            gear.add(tooth);
        }

        gear.subtract(new Area(dot(12, 12, 4.6f)));
        g.fill(gear);
    };

    public static final MD3Icon.Painter PERSON = g -> {
        g.fill(dot(12, 8, 3.8f));

        Path2D.Float body = new Path2D.Float();
        body.append(new Arc2D.Float(4.5f, 13, 15, 15, 0, 180, Arc2D.CHORD), false);
        g.fill(body);
    };

    public static final MD3Icon.Painter HOME = g -> stroke(g,
            path(3.5f, 11.5f, 12, 4.5f, 20.5f, 11.5f), path(5.8f, 10.5f, 5.8f, 19.5f, 18.2f, 19.5f, 18.2f, 10.5f));

    /** A folded article - news, rather than a house that does not say what the page is. */
    public static final MD3Icon.Painter ARTICLE = g -> stroke(g,
            new RoundRectangle2D.Float(5, 3.5f, 14, 17, 2, 2),
            new Line2D.Float(8, 8, 16, 8), new Line2D.Float(8, 12, 16, 12),
            new Line2D.Float(8, 16, 13, 16));

    /** Two stacked machines - a server list, rather than a generic list view. */
    public static final MD3Icon.Painter DNS = g -> {
        stroke(g, new RoundRectangle2D.Float(4, 3.5f, 16, 7.5f, 2, 2),
                new RoundRectangle2D.Float(4, 13, 16, 7.5f, 2, 2));
        g.fill(dot(7.2f, 7.25f, 1.15f));
        g.fill(dot(7.2f, 16.75f, 1.15f));
    };

    public static final MD3Icon.Painter PACKAGE = g -> stroke(g,
            path(12, 3.5f, 20, 7.75f, 20, 16.25f, 12, 20.5f, 4, 16.25f, 4, 7.75f, 12, 3.5f),
            new Line2D.Float(4, 7.75f, 12, 12), new Line2D.Float(20, 7.75f, 12, 12),
            new Line2D.Float(12, 12, 12, 20.5f));

    public static final MD3Icon.Painter INFO = g -> {
        stroke(g, dot(12, 12, 8.5f));
        g.fill(dot(12, 7.8f, 1.3f));
        stroke(g, new Line2D.Float(12, 11, 12, 16.5f));
    };

    public static final MD3Icon.Painter WARNING = g -> {
        Path2D.Float triangle = path(12, 3.5f, 21.5f, 20, 2.5f, 20);
        triangle.closePath();
        stroke(g, triangle);
        stroke(g, new Line2D.Float(12, 10, 12, 14.5f));
        g.fill(dot(12, 17.3f, 1.2f));
    };

    public static final MD3Icon.Painter ERROR = g -> {
        stroke(g, dot(12, 12, 8.5f));
        stroke(g, new Line2D.Float(12, 7, 12, 13));
        g.fill(dot(12, 16.4f, 1.3f));
    };

    public static final MD3Icon.Painter TERMINAL = g -> {
        stroke(g, new RoundRectangle2D.Float(3.5f, 5, 17, 14, 2.5f, 2.5f),
                path(7, 10, 10, 12.5f, 7, 15), new Line2D.Float(12.5f, 15.5f, 17, 15.5f));
    };

    public static final MD3Icon.Painter VISIBILITY = g -> {
        Path2D.Float eye = new Path2D.Float();
        eye.moveTo(2.5f, 12);
        eye.quadTo(7, 5.5f, 12, 5.5f);
        eye.quadTo(17, 5.5f, 21.5f, 12);
        eye.quadTo(17, 18.5f, 12, 18.5f);
        eye.quadTo(7, 18.5f, 2.5f, 12);
        eye.closePath();
        stroke(g, eye, dot(12, 12, 3.2f));
    };
}
