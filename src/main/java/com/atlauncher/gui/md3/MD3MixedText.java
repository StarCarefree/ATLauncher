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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.atlauncher.App;
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

    private static final Pattern HTML_BR = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_CLOSE_P = Pattern.compile("(?i)</p>");
    private static final Pattern HTML_CLOSE_DIV = Pattern.compile("(?i)</div>");
    private static final Pattern HTML_TAG = Pattern.compile("(?i)<[^>]+>");
    private static final Pattern HTML_SPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");

    /**
     * How many measured strings to keep. A page of cards re-paints the same titles; wrapping a
     * Chinese paragraph asks for many prefixes of one string. One last-hit slot was not enough
     * for either.
     */
    private static final int LAYOUT_CACHE_SIZE = 256;

    private static final Map<LayoutKey, Layout> LAYOUTS = new LinkedHashMap<LayoutKey, Layout>(64,
            0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LayoutKey, Layout> eldest) {
            return size() > LAYOUT_CACHE_SIZE;
        }
    };

    private MD3MixedText() {
    }

    /**
     * Settings changed the English or Chinese face. Cached runs still name the old fonts.
     */
    public static void invalidate() {
        synchronized (LAYOUTS) {
            LAYOUTS.clear();
        }
    }

    public static final class Run {
        public final String text;
        public final Font font;

        Run(String text, Font font) {
            this.text = text;
            this.font = font;
        }
    }

    /**
     * A string already split into script runs and measured. Wrapping and ellipsis ask for many
     * prefixes of the same line; they share this rather than walking the string again.
     */
    public static final class Layout {
        public final String text;
        public final List<Run> runs;
        private final int width;

        Layout(String text, List<Run> runs, int width) {
            this.text = text;
            this.runs = runs;
            this.width = width;
        }

        public int width() {
            return width;
        }

        public int width(int start, int end) {
            if (start < 0) {
                start = 0;
            }

            if (end > text.length()) {
                end = text.length();
            }

            if (start >= end) {
                return 0;
            }

            if (start == 0 && end == text.length()) {
                return width;
            }

            int measured = 0;
            int pos = 0;

            for (int i = 0; i < runs.size(); i++) {
                Run run = runs.get(i);
                int runEnd = pos + run.text.length();
                int from = Math.max(start, pos);
                int to = Math.min(end, runEnd);

                if (from < to) {
                    measured += metrics(run.font).stringWidth(text.substring(from, to));
                }

                pos = runEnd;

                if (pos >= end) {
                    break;
                }
            }

            return measured;
        }
    }

    public static List<Run> runs(Font base, String text) {
        return layout(base, text).runs;
    }

    public static Layout layout(Font base, String text) {
        if (text == null || text.isEmpty() || base == null) {
            return new Layout(text == null ? "" : text, Collections.<Run>emptyList(), 0);
        }

        LayoutKey key = new LayoutKey(base, text);
        Layout cached;

        synchronized (LAYOUTS) {
            cached = LAYOUTS.get(key);
        }

        if (cached != null) {
            return cached;
        }

        Layout layout = split(base, text);

        synchronized (LAYOUTS) {
            LAYOUTS.put(key, layout);
        }

        return layout;
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
        List<Run> runs = layout(base, text).runs;

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

        return layout(base, text).width();
    }

    public static int width(Font base, String text, int start, int end) {
        if (text == null || text.isEmpty() || base == null) {
            return 0;
        }

        return layout(base, text).width(start, end);
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

        int ellipsis = width(base, "…");

        if (ellipsis > width) {
            return "…";
        }

        Layout layout = layout(base, text);

        if (layout.width() + ellipsis <= width) {
            return text + "…";
        }

        int lo = 0;
        int hi = text.length();

        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;

            if (mid > 0 && mid < text.length() && Character.isLowSurrogate(text.charAt(mid))) {
                mid--;
            }

            if (mid <= lo) {
                hi = lo;
                break;
            }

            if (layout.width(0, mid) + ellipsis <= width) {
                lo = mid;
            } else {
                hi = mid - 1;

                if (hi > 0 && hi < text.length() && Character.isLowSurrogate(text.charAt(hi))) {
                    hi--;
                }
            }
        }

        if (lo <= 0) {
            return "…";
        }

        if (lo < text.length() && Character.isLowSurrogate(text.charAt(lo))) {
            lo--;
        }

        return text.substring(0, lo) + "…";
    }

    /**
     * Marks each script with the face Settings named for it, so an HTML label does not have to
     * pick one family for the whole line.
     */
    public static String toHtml(Font base, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        List<Run> scriptRuns = layout(base, text).runs;

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

        return !containsIgnoreCase(text, "<a ") && !containsIgnoreCase(text, "<a>")
                && !containsIgnoreCase(text, "<table") && !containsIgnoreCase(text, "<img")
                && !containsIgnoreCase(text, "<ul") && !containsIgnoreCase(text, "<ol")
                && !containsIgnoreCase(text, "<object") && !containsIgnoreCase(text, "<iframe");
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

        String normalized = HTML_BR.matcher(text).replaceAll("\n");
        normalized = HTML_CLOSE_P.matcher(normalized).replaceAll("\n");
        normalized = HTML_CLOSE_DIV.matcher(normalized).replaceAll("\n");
        normalized = HTML_TAG.matcher(normalized).replaceAll("");
        normalized = unescapeHtml(normalized);

        String[] parts = normalized.split("\n", -1);
        List<String> lines = new ArrayList<String>();

        for (int i = 0; i < parts.length; i++) {
            String line = HTML_SPACE.matcher(parts[i]).replaceAll(" ").trim();
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
        FontMetrics metrics = metrics(font);
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

    static FontMetrics metrics(Font font) {
        return METRICS.getFontMetrics(font);
    }

    private static Layout split(Font base, String text) {
        Font latin = UiFonts.latinFace(base);
        Font cjk = UiFonts.cjkFace(base);

        if (!UiFonts.containsCjk(text) && latin.canDisplayUpTo(text) < 0) {
            return single(text, latin);
        }

        if (isAllCjk(text)) {
            return single(text, cjk);
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
                Font nextFont = UiFonts.faceFor(base, next);

                if (!sameFace(nextFont, font)) {
                    break;
                }

                j += Character.charCount(next);
            }

            runs.add(new Run(text.substring(i, j), font));
            i = j;
        }

        return new Layout(text, Collections.unmodifiableList(runs), measure(runs));
    }

    private static Layout single(String text, Font font) {
        List<Run> runs = Collections.singletonList(new Run(text, font));

        return new Layout(text, runs, metrics(font).stringWidth(text));
    }

    private static int measure(List<Run> runs) {
        int width = 0;

        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            width += metrics(run.font).stringWidth(run.text);
        }

        return width;
    }

    private static boolean isAllCjk(String text) {
        int i = 0;
        int length = text.length();

        while (i < length) {
            int codePoint = text.codePointAt(i);

            if (!UiFonts.isCjk(codePoint)) {
                return false;
            }

            i += Character.charCount(codePoint);
        }

        return true;
    }

    private static boolean containsIgnoreCase(String text, String needle) {
        int length = text.length();
        int needleLength = needle.length();

        outer:
        for (int i = 0; i <= length - needleLength; i++) {
            for (int j = 0; j < needleLength; j++) {
                char have = text.charAt(i + j);
                char want = needle.charAt(j);

                if (have != want && Character.toLowerCase(have) != want) {
                    continue outer;
                }
            }

            return true;
        }

        return false;
    }

    private static boolean sameFace(Font a, Font b) {
        if (a.getStyle() != b.getStyle() || a.getSize2D() != b.getSize2D()) {
            return false;
        }

        return a.getFamily().equals(b.getFamily());
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static final class LayoutKey {
        private final String text;
        private final String family;
        private final int style;
        private final float size;
        private final String english;
        private final String chinese;
        private final boolean disableCustom;
        private final int hash;

        LayoutKey(Font base, String text) {
            this.text = text;
            this.family = base.getFamily();
            this.style = base.getStyle();
            this.size = base.getSize2D();
            this.english = nz(UiFonts.explicitEnglishFamily());
            this.chinese = nz(UiFonts.explicitChineseFamily());
            this.disableCustom = App.settings != null && App.settings.disableCustomFonts;
            this.hash = computeHash();
        }

        private int computeHash() {
            int value = text.hashCode();
            value = 31 * value + family.hashCode();
            value = 31 * value + style;
            value = 31 * value + Float.floatToIntBits(size);
            value = 31 * value + english.hashCode();
            value = 31 * value + chinese.hashCode();
            value = 31 * value + (disableCustom ? 1 : 0);

            return value;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof LayoutKey)) {
                return false;
            }

            LayoutKey key = (LayoutKey) other;

            return style == key.style && disableCustom == key.disableCustom && size == key.size
                    && text.equals(key.text) && family.equals(key.family)
                    && english.equals(key.english) && chinese.equals(key.chinese);
        }
    }
}
