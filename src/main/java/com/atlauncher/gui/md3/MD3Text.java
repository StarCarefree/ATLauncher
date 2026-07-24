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
     * @param width    the space available, in device pixels
     * @param maxLines how many lines the caller has measured room for
     * @return an HTML block for a {@link javax.swing.JLabel}, or a single space when there is
     *         nothing to say - never an empty string, which would collapse the label's height and
     *         take the row's alignment with it
     */
    public static String wrapToLines(FontMetrics metrics, String text, int width, int maxLines) {
        if (text == null || text.trim().isEmpty()) {
            return " ";
        }

        String flat = text.replaceAll("\\s+", " ").trim();

        if (width <= 0 || maxLines <= 0) {
            return "<html>" + escapeHtml(flat) + "</html>";
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : flat.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            // a word wider than the box still goes on its own line; it is truncated below rather
            // than left to push the layout out
            if (metrics.stringWidth(candidate) <= width || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);

                continue;
            }

            lines.add(line.toString());
            line.setLength(0);
            line.append(word);

            if (lines.size() == maxLines) {
                break;
            }
        }

        if (lines.size() < maxLines && line.length() > 0) {
            lines.add(line.toString());
        }

        int used = 0;

        for (String rendered : lines) {
            used += rendered.length() + 1;
        }

        if (used < flat.length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, truncateToWidth(metrics, lines.get(last), width));
        }

        StringBuilder html = new StringBuilder("<html>");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                html.append("<br>");
            }

            html.append(escapeHtml(lines.get(i)));
        }

        return html.append("</html>").toString();
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
