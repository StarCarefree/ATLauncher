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
package com.atlauncher.themes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Language;
import com.atlauncher.data.Settings;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * Pins the face the launcher draws in following the language it is set to.
 *
 * <p>
 * A theme's own face covers Latin and little else, so a language it cannot draw has to move the
 * whole UI onto the platform's face. That choice is made once, where the fonts are installed, which
 * left changing the language at runtime turning every string Chinese while the face stayed Latin -
 * and a plain Swing label draws a glyph it does not have as an empty box. Picking Chinese made the
 * launcher unreadable, and picking it back was the only way out.
 */
public class LanguageFontSwitchTest {
    private static final Locale ZH_CN = new Locale("zh", "CN");
    private static final String CHINESE = "中文";

    private Locale before;

    @BeforeEach
    public void installTheme() throws Exception {
        before = Language.selectedLocale;

        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);
    }

    @AfterEach
    public void restoreLanguage() {
        Language.selectedLocale = before;

        // the fonts live in UIManager, which outlives this test - put them back for the next one
        App.THEME.updateUIFonts();
    }

    /**
     * The premise the rest of this rests on. If a face with Chinese coverage is ever bundled this
     * fails, and the switching below stops being load-bearing - which is worth being told about.
     */
    @Test
    public void theThemesOwnFaceCannotDrawChinese() {
        Language.selectedLocale = Locale.ENGLISH;

        assertTrue(App.THEME.getNormalFont().canDisplayUpTo(CHINESE) >= 0,
                "the theme's face draws Chinese now, so nothing below is proving anything");
    }

    /**
     * Puts UIManager into the state the launcher starts in, which is what {@code App.modifyLAF}
     * leaves behind. Without this the defaults hold whatever face FlatLaf picked off the platform -
     * one that draws Chinese perfectly well, so every assertion below would pass on a launcher that
     * was still broken.
     */
    private static void startUpIn(Locale locale) {
        Language.selectedLocale = locale;

        UIManager.put("defaultFont", App.THEME.getNormalFont());
        UIManager.put("Button.font", App.THEME.getNormalFont());
        UIManager.put("ToolTip.font", App.THEME.getNormalFont());
        App.THEME.installTypeScale();
    }

    @Test
    public void changingToChineseInstallsAFaceThatCanDrawIt() {
        startUpIn(Locale.ENGLISH);

        assertTrue(UIManager.getFont("defaultFont").canDisplayUpTo(CHINESE) >= 0,
                "this is meant to start on a face that cannot draw Chinese");

        Language.selectedLocale = ZH_CN;
        App.THEME.updateUIFonts();

        assertTrue(UIManager.getFont("defaultFont").canDisplayUpTo(CHINESE) < 0,
                "everything built after the change inherits this font, so it would draw boxes");
        assertTrue(UIManager.getFont("Button.font").canDisplayUpTo(CHINESE) < 0,
                "buttons take their font from this one");
        assertTrue(MD3Type.font(MD3Type.BODY_LARGE).canDisplayUpTo(CHINESE) < 0,
                "the type scale is derived from the base face, so it has to be rebuilt too");
    }

    @Test
    public void changingBackToEnglishReturnsToTheThemesFace() {
        startUpIn(ZH_CN);

        Language.selectedLocale = Locale.ENGLISH;
        App.THEME.updateUIFonts();

        assertEquals(App.THEME.getNormalFont(), UIManager.getFont("defaultFont"),
                "the launcher stayed on the fallback face after the language stopped needing it");
    }
}
