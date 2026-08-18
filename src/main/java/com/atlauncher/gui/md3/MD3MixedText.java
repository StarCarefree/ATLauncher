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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

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

    private static final Pattern CSS_WIDTH = Pattern.compile("(?i)width\\s*:\\s*(\\d+)px");

    /** Last split, so a hover-repaint of the same label does not walk the string again. */
    private static Font cachedRunFont;
    private static String cachedRunText;
    private static List<Run> cachedRuns;

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
        if (text == null || text.isEmpty() || base == null) {
            return new ArrayList<Run>();
        }

        if (cachedRuns != null && text.equals(cachedRunText) && sameFace(base, cachedRunFont)) {
            return cachedRuns;
        }

        List<Run> runs = new ArrayList<Run>();

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

        cachedRunFont = base;
        cachedRunText = text;
        cachedRuns = runs;

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

    public static boolean isHtml(String text) {
        return text != null && text.regionMatches(true, 0, "<html>", 0, 6);
    }

    /**
     * Markup we can draw ourselves. Tables, links and images stay with Swing's HTML view.
     */
    public static boolean isSimpleHtml(String text) {
        if (!isHtml(text)) {
            return false;
        }

        String lower = text.toLowerCase(Locale.ROOT);

        return lower.indexOf("<a ") < 0 && lower.indexOf("<a>") < 0 && lower.indexOf("<table") < 0
                && lower.indexOf("<img") < 0 && lower.indexOf("<ul") < 0 && lower.indexOf("<ol") < 0
                && lower.indexOf("<object") < 0 && lower.indexOf("<iframe") < 0;
    }

    /**
     * Visible lines of a plain string or of the simple HTML wrap the launcher emits.
     */
    public static List<String> plainLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isHtml(text)) {
            List<String> lines = new ArrayList<String>();
            String[] parts = text.split("\n", -1);

            for (int i = 0; i < parts.length; i++) {
                lines.add(parts[i]);
            }

            return lines;
        }

        String normalized = text.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n").replaceAll("(?i)<[^>]+>", "");
        normalized = unescapeHtml(normalized);

        String[] parts = normalized.split("\n", -1);
        List<String> lines = new ArrayList<String>();

        for (int i = 0; i < parts.length; i++) {
            String line = parts[i].replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
            lines.add(line);
        }

        while (!lines.isEmpty() && lines.get(0).isEmpty()) {
            lines.remove(0);
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return lines;
    }

    public static int cssPixelWidth(String html) {
        if (html == null) {
            return 0;
        }

        Matcher matcher = CSS_WIDTH.matcher(html);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public static List<String> displayLines(Font font, String text, int wrapWidth) {
        List<String> lines = plainLines(text);

        if (lines.size() != 1 || wrapWidth <= 0 || width(font, lines.get(0)) <= wrapWidth) {
            return lines;
        }

        String wrapped = MD3Text.wrapToPlainLines(METRICS.getFontMetrics(font), lines.get(0), wrapWidth, 40);

        return plainLines(wrapped);
    }

    public static Dimension blockSize(Font font, List<String> lines) {
        FontMetrics metrics = METRICS.getFontMetrics(font);
        int width = 0;

        for (int i = 0; i < lines.size(); i++) {
            width = Math.max(width, width(font, lines.get(i)));
        }

        int height = Math.max(1, lines.size()) * metrics.getHeight();

        return new Dimension(width, height);
    }

    /**
     * @return the advance of the widest line
     */
    public static int drawLines(Graphics2D g, Font font, List<String> lines, int x, int y, int width,
            int horizontalAlignment, boolean leftToRight) {
        if (lines == null || lines.isEmpty()) {
            return 0;
        }

        FontMetrics metrics = g.getFontMetrics(font);
        int cursor = y + metrics.getAscent();
        int widest = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineWidth = width(font, line);
            widest = Math.max(widest, lineWidth);
            int lineX = alignedX(x, width, lineWidth, horizontalAlignment, leftToRight);

            draw(g, line, lineX, cursor, font);
            cursor += metrics.getHeight();
        }

        return widest;
    }

    public static int alignedX(int x, int width, int lineWidth, int alignment, boolean leftToRight) {
        boolean trailing = alignment == SwingConstants.TRAILING
                || alignment == SwingConstants.EAST
                || alignment == SwingConstants.RIGHT;
        boolean leading = alignment == SwingConstants.LEADING
                || alignment == SwingConstants.WEST
                || alignment == SwingConstants.LEFT;

        if (alignment == SwingConstants.CENTER) {
            return x + Math.max(0, width - lineWidth) / 2;
        }

        if (trailing || (leading && !leftToRight)) {
            return x + Math.max(0, width - lineWidth);
        }

        return x;
    }

    public static String unescapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&nbsp;", " ").replace("&middot;", "·").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
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
