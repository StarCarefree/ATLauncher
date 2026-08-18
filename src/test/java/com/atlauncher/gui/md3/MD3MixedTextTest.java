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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.themes.UiFonts;

/**
 * English and Chinese settings have to land on different runs of the same string. Swapping the
 * whole label onto the Chinese face was how "100" in "僵尸入侵 100 天" used to follow 微软雅黑.
 */
public class MD3MixedTextTest {
    @BeforeEach
    public void install() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
        App.settings = new Settings();
    }

    @Test
    public void aMixedStringKeepsEachScriptOnItsOwnFace() {
        List<String> english = UiFonts.familiesForEnglish();
        List<String> chinese = UiFonts.familiesForChinese();
        String latin = null;

        for (int i = 0; i < english.size(); i++) {
            if (new Font(english.get(i), Font.PLAIN, 12).canDisplayUpTo("汉") >= 0) {
                latin = english.get(i);

                break;
            }
        }

        assertTrue(latin != null && !chinese.isEmpty(), "no pair of faces to split a mixed string with");

        App.settings.uiEnglishFontFamily = latin;
        App.settings.uiChineseFontFamily = chinese.get(0);

        Font base = new Font(latin, Font.PLAIN, 12);
        List<MD3MixedText.Run> runs = MD3MixedText.runs(base, "ATM 僵尸 100");

        assertTrue(runs.size() >= 3, "the mixed string was not split into script runs: " + runs.size());

        boolean sawLatin = false;
        boolean sawCjk = false;

        for (int i = 0; i < runs.size(); i++) {
            MD3MixedText.Run run = runs.get(i);

            if (run.text.contains("ATM") || run.text.contains("100")) {
                assertEquals(latin, run.font.getFamily(), "Latin ran on the Chinese face: " + run.text);
                sawLatin = true;
            }

            if (run.text.contains("尸")) {
                assertEquals(chinese.get(0), run.font.getFamily(), "CJK ran on the English face: " + run.text);
                sawCjk = true;
            }
        }

        assertTrue(sawLatin && sawCjk, "a script was missing from the runs");
    }

    @Test
    public void automaticEnglishStillSplitsOffTheChineseUiFace() {
        List<String> chinese = UiFonts.familiesForChinese();

        assertTrue(!chinese.isEmpty(), "no Chinese face to split a mixed string with");

        App.settings.uiEnglishFontFamily = "";
        App.settings.uiChineseFontFamily = chinese.get(0);

        Font ui = new Font(chinese.get(0), Font.PLAIN, 12);
        List<MD3MixedText.Run> runs = MD3MixedText.runs(ui, "ATM 僵尸 100");

        assertTrue(runs.size() >= 3, "the mixed string was not split into script runs: " + runs.size());

        for (int i = 0; i < runs.size(); i++) {
            MD3MixedText.Run run = runs.get(i);

            if (run.text.contains("ATM") || run.text.contains("100")) {
                assertEquals(UiFonts.themeLatinFamily(), run.font.getFamily(),
                        "Latin stayed on the Chinese UI face: " + run.text);
            }

            if (run.text.contains("尸")) {
                assertEquals(chinese.get(0), run.font.getFamily(),
                        "CJK ran on the English face: " + run.text);
            }
        }
    }

    @Test
    public void wrappedHtmlIsPaintedAsMixedLinesNotOneHtmlFace() {
        List<String> english = UiFonts.familiesForEnglish();
        List<String> chinese = UiFonts.familiesForChinese();
        String latin = null;

        for (int i = 0; i < english.size(); i++) {
            if (new Font(english.get(i), Font.PLAIN, 12).canDisplayUpTo("汉") >= 0) {
                latin = english.get(i);

                break;
            }
        }

        assertTrue(latin != null && !chinese.isEmpty(), "no pair of faces to split wrapped HTML with");

        App.settings.uiEnglishFontFamily = latin;
        App.settings.uiChineseFontFamily = chinese.get(0);

        Font base = new Font(latin, Font.PLAIN, 12);
        String html = MD3Text.wrapToLines(new javax.swing.JLabel().getFontMetrics(base),
                "ATM 僵尸 100 是一个很长的说明，用来强迫折行。", 80, 2);

        assertTrue(MD3MixedText.isSimpleHtml(html), "the wrap is no longer the simple HTML the label paints");

        List<String> lines = MD3MixedText.plainLines(html);
        StringBuilder visible = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            visible.append(lines.get(i));
        }

        List<MD3MixedText.Run> runs = MD3MixedText.runs(base, visible.toString());
        boolean sawLatin = false;
        boolean sawCjk = false;

        for (int i = 0; i < runs.size(); i++) {
            MD3MixedText.Run run = runs.get(i);

            if (run.text.contains("ATM") || run.text.contains("100")) {
                assertEquals(latin, run.font.getFamily(), "wrapped Latin ran on the Chinese face: " + run.text);
                sawLatin = true;
            }

            if (run.text.contains("尸")) {
                assertEquals(chinese.get(0), run.font.getFamily(),
                        "wrapped CJK ran on the English face: " + run.text);
                sawCjk = true;
            }
        }

        assertTrue(sawLatin && sawCjk, "wrapped HTML lost a script: " + visible);
    }
}
