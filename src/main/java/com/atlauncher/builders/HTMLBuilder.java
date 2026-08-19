/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.builders;

import java.awt.Font;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlauncher.App;
import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.themes.ATLauncherLaf;

public final class HTMLBuilder {
    /**
     * Markup the launcher puts in these strings itself. Everything else is prose and is escaped,
     * including a stray {@code <} in "Minecraft < 1.6".
     *
     * <p>
     * {@link MD3MixedText#toHtml} used to run over the whole body, which escaped
     * {@code <a href="...">} into visible source - so the accounts empty state, the Microsoft
     * login prompt, and every other string that already contained a link rendered the tags
     * instead of the link.
     */
    private static final Pattern MARKUP = Pattern.compile(
            "(?is)</?(?:a|b|i|u|em|strong|br|p|div|span|ul|ol|li|font|h[1-6])\\b[^>]*>");

    public boolean center = false;
    public String text;
    public Integer split;

    public HTMLBuilder center() {
        center = true;

        return this;
    }

    public HTMLBuilder text(String text) {
        this.text = text;

        return this;
    }

    public HTMLBuilder split(int length) {
        split = length;

        return this;
    }

    private String getText() {
        if (split == null) {
            return text;
        }

        char[] chars = text.toCharArray();
        StringBuilder sb = new StringBuilder();
        char spaceChar = ' ';
        int count = 0;
        for (char character : chars) {
            if (count >= split && character == spaceChar) {
                sb.append("<br/>");
                count = 0;
            } else {
                count++;
                sb.append(character);
            }
        }
        return sb.toString();
    }

    public String build() {
        String start = "";
        String end = "";
        Font font = Optional.ofNullable(App.THEME)
                .map(ATLauncherLaf::getNormalFont)
                .orElse(new Font("Arial", Font.PLAIN, 12));

        if (center) {
            start += "<p style=\"padding: 0;font-size: " + font.getSize() + "pt;\" align=\"center\">";
            end += "</p>";
        }

        return String.format("<html>%s%s%s</html>", start, mixedBody(font), end);
    }

    private String mixedBody(Font font) {
        String body = getText();

        if (body == null) {
            return "";
        }

        Matcher matcher = MARKUP.matcher(body);
        StringBuilder html = new StringBuilder();
        int last = 0;

        while (matcher.find()) {
            html.append(MD3MixedText.toHtml(font, body.substring(last, matcher.start())));
            html.append(matcher.group());
            last = matcher.end();
        }

        html.append(MD3MixedText.toHtml(font, body.substring(last)));

        return html.toString();
    }
}
