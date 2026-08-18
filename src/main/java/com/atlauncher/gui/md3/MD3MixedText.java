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
package com.atlauncher.gui.md3;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JLabel;

import com.atlauncher.themes.UiFonts;

/**
 * Draws a string with the English face on Latin and the Chinese face on CJK, so a mixed label
 * does not have to pick one setting and abandon the other.
 */
public final class MD3MixedText {
    /**
     * Put {@code Boolean.TRUE} on a component that is already showing a chosen face - the font
     * picker, which would otherwise paint every row with the English / Chinese settings instead of
     * the family it is offering.
     */
    public static final String KEEP_FACE_KEY = "MD3.keepFace";

    private static final JLabel METRICS = new JLabel();

    private MD3MixedText() {
    }

    public static final class Run {
        public final String text;
        public final Font font;

        Run(String text, Font font) {
            this.text = text;
            this.font = font;
        }
    }

    public static List<Run> runs(Font base, String text) {
        List<Run> runs = new ArrayList<Run>();

        if (text == null || text.isEmpty() || base == null) {
            return runs;
        }

        int i = 0;
        int length = text.length();

        while (i < length) {
            int codePoint = text.codePointAt(i);
            Font font = UiFonts.faceFor(base, codePoint);
            int j = i + Character.charCount(codePoint);

            while (j < length) {
                int next = text.codePointAt(j);

                if (!sameFace(UiFonts.faceFor(base, next), font)) {
                    break;
                }

                j += Character.charCount(next);
            }

            runs.add(new Run(text.substring(i, j), font));
            i = j;
        }

        return runs;
    }

    /**
     * @return the advance after drawing, so a caller that is placing more than one string can
     *         continue from where this one ended
     */
    public static int draw(Graphics2D g, String text, int x, int y, Font base) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cursor = x;
        List<Run> runs = runs(base, text);

        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);

            g.setFont(run.font);
            g.drawString(run.text, cursor, y);
            cursor += g.getFontMetrics(run.font).stringWidth(run.text);
        }

        return cursor - x;
    }

    public static int width(Font base, String text) {
        if (text == null || text.isEmpty() || base == null) {
            return 0;
        }

        int width = 0;
        List<Run> runs = runs(base, text);

        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);

            width += METRICS.getFontMetrics(run.font).stringWidth(run.text);
        }

        return width;
    }

    public static int width(FontMetrics metrics, String text) {
        if (metrics == null) {
            return 0;
        }

        return width(metrics.getFont(), text);
    }

    public static String fitToWidth(Font base, String text, int width) {
        if (text == null || text.isEmpty() || width(base, text) <= width) {
            return text == null ? "" : text;
        }

        return ellipsisToWidth(base, text, width);
    }

    /**
     * Always ends with an ellipsis. For a line that has already been cut by a line cap, so the
     * reader can see there is more even when the remaining fragment would have fitted.
     */
    public static String ellipsisToWidth(Font base, String text, int width) {
        if (text == null || text.isEmpty()) {
            return "…";
        }

        String candidate = text + "…";

        while (candidate.length() > 2 && width(base, candidate) > width) {
            text = text.substring(0, text.length() - 1);
            candidate = text + "…";
        }

        return candidate;
    }

    /**
     * Marks each script with the face Settings named for it, so an HTML label does not have to
     * pick one family for the whole line.
     */
    public static String toHtml(Font base, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        List<Run> scriptRuns = runs(base, text);

        if (scriptRuns.size() <= 1) {
            return escapeHtml(text);
        }

        StringBuilder html = new StringBuilder();

        for (int i = 0; i < scriptRuns.size(); i++) {
            Run run = scriptRuns.get(i);
            String family = run.font.getFamily().replace("'", "");

            html.append("<span style=\"font-family:'").append(family).append("'\">")
                    .append(escapeHtml(run.text)).append("</span>");
        }

        return html.toString();
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * The same paint {@link MD3ButtonUI} uses, for a checkbox, radio or switch whose UI still
     * belongs to FlatLaf.
     */
    public static void paintButtonText(Graphics g, AbstractButton button, Rectangle textRect, String text,
            Color disabled) {
        if (text == null || text.isEmpty() || textRect == null) {
            return;
        }

        Color color = button.isEnabled() ? button.getForeground() : disabled;

        if (color == null) {
            color = button.getForeground();
        }

        g.setColor(color);
        draw((Graphics2D) g, text, textRect.x,
                textRect.y + button.getFontMetrics(button.getFont()).getAscent(), button.getFont());
    }

    private static boolean sameFace(Font a, Font b) {
        return a.getFamily().equals(b.getFamily()) && a.getStyle() == b.getStyle()
                && a.getSize2D() == b.getSize2D();
    }
}
