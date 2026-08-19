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

    @Test
    public void aLatinStringIsOneRunOnTheEnglishFace() {
        Pair pair = pair();
        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);
        List<MD3MixedText.Run> runs = MD3MixedText.runs(base, "All the Mods 10");

        assertEquals(1, runs.size(), "a Latin-only string was still split: " + runs.size());
        assertEquals(pair.latin, runs.get(0).font.getFamily(), "Latin left the English face");
    }

    @Test
    public void aChineseStringIsOneRunOnTheChineseFace() {
        Pair pair = pair();
        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);
        List<MD3MixedText.Run> runs = MD3MixedText.runs(base, "僵尸入侵");

        assertEquals(1, runs.size(), "a Chinese-only string was still split: " + runs.size());
        assertEquals(pair.cjk, runs.get(0).font.getFamily(), "CJK left the Chinese face");
    }

    @Test
    public void rangeWidthMatchesTheSubstring() {
        Pair pair = pair();
        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);
        String text = "ATM 僵尸 100";
        MD3MixedText.Layout layout = MD3MixedText.layout(base, text);

        assertEquals(MD3MixedText.width(base, text.substring(0, 5)), layout.width(0, 5),
                "a prefix of a mixed string did not match measuring the substring");
        assertEquals(layout.width(), layout.width(0, text.length()),
                "the full-range width drifted from the layout total");
    }

    @Test
    public void changingTheChineseFaceIsVisibleOnTheSameString() {
        Pair pair = pair();
        String otherCjk = otherChinese(pair.cjk);

        if (otherCjk == null) {
            return;
        }

        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);
        String text = "ATM 僵尸 100";

        assertEquals(pair.cjk, cjkFamilyOf(MD3MixedText.runs(base, text)),
                "the first Chinese pick did not land");

        App.settings.uiChineseFontFamily = otherCjk;

        assertEquals(otherCjk, cjkFamilyOf(MD3MixedText.runs(base, text)),
                "cached runs kept the previous Chinese face after Settings changed");
    }

    @Test
    public void ellipsisStillFitsAndMarksTheCut() {
        Pair pair = pair();
        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);
        String text = "僵尸入侵 100 天 All the Mods";
        int full = MD3MixedText.width(base, text);
        String cut = MD3MixedText.ellipsisToWidth(base, text, Math.max(12, full / 3));

        assertTrue(cut.endsWith("…"), "a line that did not fit was not marked: " + cut);
        assertTrue(cut.length() < text.length() + 1, "the ellipsis did not shorten the line: " + cut);
        assertTrue(MD3MixedText.width(base, cut) <= full,
                "the ellipsised line is wider than the original");
    }

    @Test
    public void latinAndCjkFacesAreReusedForTheSameBase() {
        Pair pair = pair();
        App.settings.uiEnglishFontFamily = pair.latin;
        App.settings.uiChineseFontFamily = pair.cjk;

        Font base = new Font(pair.latin, Font.PLAIN, 12);

        assertTrue(UiFonts.latinFace(base) == UiFonts.latinFace(base),
                "the English face was rebuilt for every character");
        assertTrue(UiFonts.cjkFace(base) == UiFonts.cjkFace(base),
                "the Chinese face was rebuilt for every character");
    }

    private static String cjkFamilyOf(List<MD3MixedText.Run> runs) {
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).text.contains("尸")) {
                return runs.get(i).font.getFamily();
            }
        }

        return null;
    }

    private static String otherChinese(String used) {
        List<String> chinese = UiFonts.familiesForChinese();

        for (int i = 0; i < chinese.size(); i++) {
            if (!chinese.get(i).equals(used)) {
                return chinese.get(i);
            }
        }

        return null;
    }

    private static Pair pair() {
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

        return new Pair(latin, chinese.get(0));
    }

    private static final class Pair {
        final String latin;
        final String cjk;

        Pair(String latin, String cjk) {
            this.latin = latin;
            this.cjk = cjk;
        }
    }
}
