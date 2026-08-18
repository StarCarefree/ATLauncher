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
package com.atlauncher.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.themes.MaterialDark;

/**
 * The two faces the console draws with, without standing up the window.
 */
public class ConsoleFontsTest {
    @BeforeEach
    public void installTheme() throws Exception {
        MaterialDark.install();
        App.THEME = MaterialDark.getInstance();
        App.settings = new Settings();
    }

    @Test
    public void theBundledFaceIsOnTheClasspath() {
        assertNotNull(getClass().getResource("/assets/font/JetBrainsMono-Medium.ttf"),
                "JetBrains Mono Medium was not packaged");
    }

    @Test
    public void latinIsJetBrainsMonoAndMonospaced() {
        Font font = ConsoleFonts.latin();
        String name = (font.getFamily() + " " + font.getFontName()).toLowerCase(Locale.ROOT);

        assertTrue(name.contains("jetbrains"), "the Latin face is " + font.getFontName() + ", not JetBrains Mono");
        assertTrue(new JLabel().getFontMetrics(font).charWidth('i')
                == new JLabel().getFontMetrics(font).charWidth('W'),
                "the Latin face is not monospaced");
        assertTrue(font.canDisplayUpTo("Launcher opening [INFO]") < 0,
                "JetBrains Mono cannot draw the ASCII the log is made of");
    }

    @Test
    public void fallbackCanDrawChinese() {
        String chinese = "启动器正在打开";
        Font font = ConsoleFonts.fallback();

        assertTrue(font.canDisplayUpTo(chinese) < 0,
                "the fallback face still cannot draw Chinese, so mixed lines will show boxes");
        assertEquals(ConsoleFonts.latin().getSize(), font.getSize(),
                "the fallback changed size, so a mixed line jumps where the script does");
    }

    @Test
    public void aMixedLineIsSplitSoEachRunCanDrawItself() throws Exception {
        StyledDocument document = new DefaultStyledDocument();
        String text = "Loading 僵尸入侵 100 天";

        ConsoleFonts.insert(document, text, new SimpleAttributeSet());

        assertEquals(text, document.getText(0, document.getLength()),
                "splitting the line into font runs dropped or reordered characters");

        int chinese = text.indexOf("尸");
        String family = StyleConstants.getFontFamily(document.getCharacterElement(chinese).getAttributes());
        Font face = new Font(family, Font.PLAIN, 12);

        assertTrue(face.canDisplay('尸'),
                "the CJK run was left on a face that cannot draw it (" + family + ")");
    }
}
