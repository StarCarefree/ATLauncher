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

import java.awt.FontMetrics;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Fitting text into a box a label was measured for.
 *
 * <p>
 * A plain {@link javax.swing.JLabel} does not wrap - it draws one line and clips it at the
 * component's edge, losing the end of the sentence with no sign that anything is missing. Swing's
 * HTML block does wrap, but honours a {@code width} inconsistently: it lays the text out at its
 * natural width and then clips, which drops whole words from the middle.
 *
 * <p>
 * So the breaks are measured here and emitted as explicit {@code <br>}s, and anything past the last
 * line it will fit is marked with an ellipsis rather than silently dropped.
 */
public final class MD3Text {
    private static final String ELLIPSIS = "…";

    private MD3Text() {
    }

    /**
     * Opens an HTML block whose body does not paint a slab of {@code Label.background}.
     *
     * <p>
     * Swing's HTML view fills the default stylesheet colour - the surface role - even when the
     * label itself is not opaque. On a card that is already a step up the container ramp, that
     * shows as a darker rectangle behind the wrapped lines.
     */
    public static final String HTML_OPEN = "<html><body style='background:none;background-color:transparent;margin:0;padding:0'>";

    public static final String HTML_CLOSE = "</body></html>";

    /**
     * @param width    the space available, in device pixels
     * @param maxLines how many lines the caller has measured room for
     * @return an HTML block for a {@link javax.swing.JLabel}, or a single space when there is
     *         nothing to say - never an empty string, which would collapse the label's height and
     *         take the row's alignment with it
     */
    public static String wrapToLines(FontMetrics metrics, String text, int width, int maxLines) {
        List<String> lines = breakLines(metrics, text, width, maxLines);

        if (lines.isEmpty()) {
            return " ";
        }

        StringBuilder html = new StringBuilder(HTML_OPEN);

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                html.append("<br>");
            }

            html.append(escapeHtml(lines.get(i)));
        }

        return html.append(HTML_CLOSE).toString();
    }

    /**
     * The same wrap as {@link #wrapToLines}, as plain text with {@code \n}s. For a
     * {@code JTextArea}, which wraps without painting the HTML slab a {@code JLabel} does.
     */
    public static String wrapToPlainLines(FontMetrics metrics, String text, int width, int maxLines) {
        List<String> lines = breakLines(metrics, text, width, maxLines);

        if (lines.isEmpty()) {
            return " ";
        }

        StringBuilder plain = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                plain.append('\n');
            }

            plain.append(lines.get(i));
        }

        return plain.toString();
    }

    private static List<String> breakLines(FontMetrics metrics, String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return lines;
        }

        String flat = text.replaceAll("\\s+", " ").trim();

        if (width <= 0 || maxLines <= 0) {
            lines.add(flat);

            return lines;
        }

        // Splitting on spaces would take a Chinese sentence - which has none - for one long word,
        // put the whole of it on the first line and truncate the rest away. BreakIterator gives the
        // break opportunities the script actually has: between words in English, between characters
        // in Chinese, and never before a mark like "。" that may not open a line.
        BreakIterator breaks = BreakIterator.getLineInstance();
        breaks.setText(flat);

        int lineStart = 0;
        int lineEnd = 0;
        int consumed = 0;

        for (int end = breaks.following(0); end != BreakIterator.DONE; end = breaks.following(end)) {
            // a run with no break opportunity inside it is wider than the box on its own; it still
            // gets a line to itself and is truncated below rather than left to push the layout out
            if (lineEnd == lineStart || metrics.stringWidth(flat.substring(lineStart, end).trim()) <= width) {
                lineEnd = end;

                continue;
            }

            lines.add(flat.substring(lineStart, lineEnd).trim());
            consumed = lineEnd;

            if (lines.size() == maxLines) {
                break;
            }

            lineStart = lineEnd;
            lineEnd = end;
        }

        if (lines.size() < maxLines && lineEnd > lineStart) {
            lines.add(flat.substring(lineStart, lineEnd).trim());
            consumed = lineEnd;
        }

        if (consumed < flat.length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, truncateToWidth(metrics, lines.get(last), width));
        }

        return lines;
    }

    /**
     * Drops the line breaks a string was written with as HTML.
     *
     * <p>
     * Much of the launcher's explanatory text was written to be a tooltip and carries {@code <br/>}s
     * for the width it was expected to be shown at. Laying that text out here means measuring it, so
     * the markup has to go - but rewriting the string in the source would change its msgid and drop
     * every translation of it. So the {@code GetText.tr} call keeps the string it has always had and
     * the breaks come out on the way to the label.
     *
     * <p>
     * Only {@code <br>} is handled, because that is the only tag these strings carry. Stripping tags
     * in general would take "{@code Minecraft < 1.6}" for one.
     */
    public static String plain(String html) {
        if (html == null) {
            return null;
        }

        return html.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Trims text until it and an ellipsis fit.
     */
    public static String truncateToWidth(FontMetrics metrics, String text, int width) {
        String candidate = text + ELLIPSIS;

        while (candidate.length() > ELLIPSIS.length() + 1 && metrics.stringWidth(candidate) > width) {
            text = text.substring(0, text.length() - 1);
            candidate = text + ELLIPSIS;
        }

        return candidate;
    }

    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
