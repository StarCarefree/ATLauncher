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
package com.atlauncher.builders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code HTMLBuilder} has to keep the markup the launcher already put in the string.
 *
 * <p>
 * Mixing English and Chinese fonts used to run over the whole body, which escaped
 * {@code <a href="...">} into visible source. The accounts empty state then told people to buy
 * Minecraft by showing them the tag.
 */
public class HTMLBuilderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    @Test
    public void anInlineLinkSurvives() {
        String html = new HTMLBuilder().center().text(
                "you can get one <a href=\"https://atl.pw/create-account\">by buying Minecraft here</a>.")
                .build();

        assertTrue(html.contains("href=\"https://atl.pw/create-account\""),
                "the href was dropped, so the link has nowhere to go");
        assertTrue(html.contains(">by buying Minecraft here</a>"),
                "the link text was not kept inside the tag");
        assertFalse(html.contains("&lt;a"),
                "the anchor was escaped, so it renders as source instead of as a link");
    }

    @Test
    public void emphasisAndBreaksSurvive() {
        String html = new HTMLBuilder().center()
                .text("enter the code <b>ABCD</b>.<br><br>Then continue.").build();

        assertTrue(html.contains("<b>ABCD</b>"), "bold was escaped");
        assertTrue(html.contains("<br>"), "the author's line breaks were escaped");
        assertFalse(html.contains("&lt;b"), "emphasis rendered as source");
    }

    @Test
    public void headingsAndLicenseLinksSurvive() {
        String html = new HTMLBuilder().text(
                "<h2>Build Dependencies</h2><br/>See <a href=\"https://www.gnu.org/licenses/\">the GPL</a>.")
                .build();

        assertTrue(html.contains("<h2>Build Dependencies</h2>"),
                "a heading was escaped, so the About libraries list shows the tag");
        assertTrue(html.contains("href=\"https://www.gnu.org/licenses/\""),
                "the licence link was not kept as a link");
        assertFalse(html.contains("&lt;h2"), "the heading rendered as source");
        assertFalse(html.contains("&lt;a"), "the licence link rendered as source");
    }

    @Test
    public void aLessThanInProseIsStillEscaped() {
        String html = new HTMLBuilder().text("Minecraft < 1.6 needs this.").build();

        assertTrue(html.contains("&lt;"), "a stray less-than was left as a broken tag");
        assertFalse(html.contains("< 1.6"), "the comparison was treated as markup");
    }
}
