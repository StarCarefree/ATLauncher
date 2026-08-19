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
package com.atlauncher.gui.tabs.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.themes.UiFonts;
import com.atlauncher.utils.ComboItem;

/**
 * Switching the English face used to leave the font picker's shared renderer pinned to that
 * face. "自动（系统）" then drew as empty boxes, and only the rows whose preview face still had
 * CJK stayed readable.
 */
public class FontPickerRendererTest {
    @BeforeEach
    public void install() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
        App.settings = new Settings();
    }

    @Test
    public void autoStaysReadableAfterPreviewingALatinFace() {
        String latin = latinOnlyFamily();

        assertTrue(latin != null, "no Latin-only face to pin the renderer to");

        App.settings.uiEnglishFontFamily = latin;

        GeneralSettingsTab.FontPickerRenderer renderer = new GeneralSettingsTab.FontPickerRenderer();
        JList<Object> list = new JList<Object>();
        list.setFont(new Font(latin, Font.PLAIN, 12));

        ComboItem<String> preview = new ComboItem<String>(latin, latin);
        ComboItem<String> auto = new ComboItem<String>(UiFonts.AUTO, "自动（系统）");

        renderer.getListCellRendererComponent(list, preview, 0, false, false);
        Component shown = renderer.getListCellRendererComponent(list, auto, -1, false, false);

        assertFalse(Boolean.TRUE.equals(((JLabel) shown).getClientProperty(MD3MixedText.KEEP_FACE_KEY)),
                "Auto was left on KEEP_FACE after a family row");

        Font ui = ((JLabel) shown).getFont();
        List<MD3MixedText.Run> runs = MD3MixedText.runs(ui, "自动（系统）");
        boolean sawCjk = false;

        for (int i = 0; i < runs.size(); i++) {
            MD3MixedText.Run run = runs.get(i);

            if (run.text.contains("自")) {
                sawCjk = true;
                assertTrue(run.font.canDisplayUpTo("自动") < 0,
                        "Auto still draws Chinese with the English face: " + run.font.getFamily());
            }
        }

        assertTrue(sawCjk, "the Auto label lost its Chinese run");
    }

    @Test
    public void aFamilyThatCannotDrawItsNameFallsBackToMixedPaint() {
        String latin = latinOnlyFamily();

        assertTrue(latin != null, "no Latin-only face to prove the fallback with");

        App.settings.uiEnglishFontFamily = latin;

        GeneralSettingsTab.FontPickerRenderer renderer = new GeneralSettingsTab.FontPickerRenderer();
        JList<Object> list = new JList<Object>();
        list.setFont(new Font(latin, Font.PLAIN, 12));

        ComboItem<String> item = new ComboItem<String>(latin, "微软雅黑");
        JLabel shown = (JLabel) renderer.getListCellRendererComponent(list, item, 0, false, false);

        assertFalse(Boolean.TRUE.equals(shown.getClientProperty(MD3MixedText.KEEP_FACE_KEY)),
                "a Latin face was asked to preview a Chinese name");

        boolean cjkDrawn = false;
        List<MD3MixedText.Run> runs = MD3MixedText.runs(shown.getFont(), shown.getText());

        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).text.contains("微") && runs.get(i).font.canDisplayUpTo("微") < 0) {
                cjkDrawn = true;
            }
        }

        assertTrue(cjkDrawn, "the Chinese name was left on a face that cannot draw it");
    }

    private static String latinOnlyFamily() {
        List<String> english = UiFonts.familiesForEnglish();

        for (int i = 0; i < english.size(); i++) {
            Font font = new Font(english.get(i), Font.PLAIN, 12);

            if (font.canDisplayUpTo("汉") >= 0) {
                return english.get(i);
            }
        }

        return null;
    }
}
