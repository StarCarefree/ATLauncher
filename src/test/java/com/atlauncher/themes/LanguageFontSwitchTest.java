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

import java.awt.Font;
import java.util.List;
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
        App.settings.uiFontFamily = "";
        App.settings.uiEnglishFontFamily = "";
        App.settings.uiChineseFontFamily = "";
        App.settings.disableCustomFonts = false;

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

    /**
     * The log is a column of Latin timestamps. Switching the UI to Chinese must
     * not drag the console off JetBrains Mono onto the proportional fallback
     * the rest of the window uses.
     */
    @Test
    public void theConsoleKeepsJetBrainsMonoWhenTheUiIsChinese() {
        startUpIn(ZH_CN);

        String name = (App.THEME.getConsoleFont().getFamily() + " "
                + App.THEME.getConsoleFont().getFontName()).toLowerCase(Locale.ROOT);

        assertTrue(name.contains("jetbrains"),
                "the console followed the UI onto the locale fallback, so the log is no longer monospaced");
    }

    @Test
    public void pickingASystemFontUsesThatFace() {
        startUpIn(Locale.ENGLISH);

        App.settings.uiEnglishFontFamily = Font.SANS_SERIF;
        App.THEME.updateUIFonts();

        assertEquals(Font.SANS_SERIF, App.THEME.getNormalFont().getFamily(),
                "picking the system face left the theme's face in place");

        List<String> families = UiFonts.familiesForEnglish();

        assertTrue(!families.isEmpty(), "the machine has no usable UI font, so there is nothing to switch to");

        App.settings.uiEnglishFontFamily = families.get(0);
        App.THEME.updateUIFonts();

        assertEquals(families.get(0), App.THEME.getNormalFont().getFamily(),
                "picking an installed family did not change the UI face");
    }

    /**
     * The two settings are independent. Picking an English face must not steal the Chinese
     * fallback, and picking a Chinese face must not move Latin off the English one.
     */
    @Test
    public void englishAndChineseFacesAreAppliedSeparately() {
        startUpIn(Locale.ENGLISH);

        List<String> english = UiFonts.familiesForEnglish();
        List<String> chinese = UiFonts.familiesForChinese();

        assertTrue(!english.isEmpty() && !chinese.isEmpty(),
                "this machine has no pair of faces to prove the split with");

        String latin = firstThatCannotDraw(english, CHINESE);
        String cjk = chinese.get(0);

        assertTrue(latin != null, "every English face on this machine also draws Chinese, so the split is untestable");

        App.settings.uiEnglishFontFamily = latin;
        App.settings.uiChineseFontFamily = cjk;
        App.THEME.updateUIFonts();

        assertEquals(latin, App.THEME.getNormalFont().getFamily(),
                "the English setting did not become the UI face");
        assertEquals(cjk, MD3Type.font(MD3Type.BODY_LARGE, CHINESE).getFamily(),
                "Chinese text did not take the Chinese setting");
        assertEquals(latin, MD3Type.font(MD3Type.BODY_LARGE, "Launcher").getFamily(),
                "a Latin string was pushed onto the Chinese face");
    }

    private static String firstThatCannotDraw(List<String> families, String sample) {
        for (int i = 0; i < families.size(); i++) {
            Font font = new Font(families.get(i), Font.PLAIN, 12);

            if (font.canDisplayUpTo(sample) >= 0) {
                return families.get(i);
            }
        }

        return null;
    }

    /**
     * A Chinese UI must still offer Latin faces. Filtering the list to glyphs the language
     * needs hid Arial and Segoe UI the moment someone picked 简体中文.
     */
    @Test
    public void englishFacesStayOnTheListWhenTheUiIsChinese() {
        Language.selectedLocale = ZH_CN;

        List<String> families = UiFonts.familiesForEnglish();
        String[] latin = { "Arial", "Segoe UI", "Calibri", "Tahoma", "Verdana", "Times New Roman",
                "Georgia", "Calibri Light" };
        boolean installed = false;
        boolean listed = false;

        for (int i = 0; i < latin.length; i++) {
            Font font = new Font(latin[i], Font.PLAIN, 12);

            if (!latin[i].equals(font.getFamily())) {
                continue;
            }

            installed = true;

            if (families.contains(latin[i])) {
                listed = true;

                break;
            }
        }

        assertTrue(installed, "this machine has no ordinary English face to prove the list with");
        assertTrue(listed, "the font list dropped every Latin-only face once the UI language was Chinese");
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
