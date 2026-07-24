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
package com.atlauncher.themes.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Type;

/**
 * The launcher shows a great deal of text it did not write - pack names and summaries from six
 * platforms, instance names from the user, dates carrying a localised marker. A theme's face covers
 * Latin and little else, and a plain Swing label draws what it cannot find as an empty box rather
 * than substituting a face that has it.
 */
public class MD3TypeFallbackTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
    }

    @Test
    public void testTextTheThemeCanDrawKeepsTheThemeFont() {
        Font plain = MD3Type.font(MD3Type.TITLE_MEDIUM);

        assertEquals(plain, MD3Type.font(MD3Type.TITLE_MEDIUM, "All the Mods 10"),
                "a Latin string was pushed off the theme's own face");
        assertEquals(plain, MD3Type.font(MD3Type.TITLE_MEDIUM, null), "a null string changed the font");
    }

    @Test
    public void testTextTheThemeCannotDrawFallsBack() {
        String chinese = "僵尸入侵 100 天";
        Font font = MD3Type.font(MD3Type.TITLE_MEDIUM, chinese);

        assertTrue(font.canDisplayUpTo(chinese) < 0,
                "the font chosen for this text still cannot draw it, so the card shows empty boxes");
    }

    @Test
    public void testFallbackKeepsTheRolesMetrics() {
        Font plain = MD3Type.font(MD3Type.LABEL_MEDIUM);
        Font fallback = MD3Type.font(MD3Type.LABEL_MEDIUM, "下午");

        assertEquals(plain.getSize2D(), fallback.getSize2D(), 0.01f,
                "the fallback changed size, so a date in one language sits taller than in another");
        assertEquals(plain.getStyle(), fallback.getStyle(), "the fallback lost the role's weight");
    }
}
