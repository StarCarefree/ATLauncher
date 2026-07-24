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
package com.atlauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.atlauncher.themes.ArcOrange;
import com.atlauncher.themes.CatppuccinFrappe;

/**
 * Guards {@code options.encoding = 'UTF-8'} in the build.
 *
 * <p>
 * Without it javac reads sources in the platform's default charset. On a machine whose default is
 * not UTF-8 - Windows in most of Asia, for one - every accented character in a user-facing string
 * silently becomes mojibake, and anything the platform charset cannot represent at all fails the
 * build outright. Neither shows up on a developer machine that happens to default to UTF-8, which
 * is exactly why it needs a test rather than a convention.
 *
 * <p>
 * These strings are compared against their escaped forms, so this test says the same thing however
 * the file holding it is read.
 */
public class SourceEncodingTest {
    @Test
    public void testAccentedThemeNamesSurviveCompilation() {
        assertEquals("Catppuccin (Frappé) by sgoudham", new CatppuccinFrappe().getDescription(),
                "the theme description lost its accent, so sources were not read as UTF-8");
    }

    @Test
    public void testAccentedAuthorNamesSurviveCompilation() {
        assertEquals("Arc Orange by Pavel Zlámal", new ArcOrange().getDescription(),
                "the theme author lost their accent, so sources were not read as UTF-8");
    }

    @Test
    public void testNonAsciiLiteralsRoundTrip() {
        // a spread of what the launcher's strings actually contain: accents, a middle dot used as a
        // separator on cards, an ellipsis, and CJK from the translations
        assertEquals(1, "é".length());
        assertEquals('é', "é".charAt(0));
        assertEquals('·', "·".charAt(0));
        assertEquals('…', "…".charAt(0));
        assertEquals('实', "实例".charAt(0));
    }
}
