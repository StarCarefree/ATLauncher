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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Pins how a description is broken into lines, in both scripts the launcher has to lay out.
 *
 * <p>
 * The first cut split on spaces, which reads a Chinese sentence - which has none - as one very long
 * word. Every card given Chinese text drew one line and ellipsised the rest of the paragraph away,
 * and it did so without throwing, so nothing but the eye caught it.
 */
public class MD3TextWrapTest {
    /** Ten han characters, the width the wrapping tests give themselves. */
    private static final String TEN_HAN = "一二三四五六七八九十";

    private static final String CHINESE_DESCRIPTION = "这是一个包含大量"
            + "科技模组的整合包，适合喜欢自动"
            + "化生产线的玩家。";

    private static FontMetrics metrics() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    private static String[] linesOf(String html) {
        return html.replaceAll("^<html>", "").replaceAll("</html>$", "").split("<br>", -1);
    }

    @Test
    public void chineseFillsEveryLineItWasGivenRoomFor() {
        FontMetrics metrics = metrics();
        String[] lines = linesOf(MD3Text.wrapToLines(metrics, CHINESE_DESCRIPTION,
                metrics.stringWidth(TEN_HAN), 3));

        assertEquals(3, lines.length, "Chinese should wrap rather than collapse onto one line");

        for (String line : lines) {
            assertFalse(line.isEmpty(), "no line should come out blank");
        }
    }

    @Test
    public void noLineOverflowsTheBoxItWasMeasuredFor() {
        FontMetrics metrics = metrics();
        int width = metrics.stringWidth(TEN_HAN);

        for (String line : linesOf(MD3Text.wrapToLines(metrics, CHINESE_DESCRIPTION, width, 3))) {
            assertTrue(metrics.stringWidth(line) <= width, "line wider than its box: " + line);
        }
    }

    @Test
    public void closingPunctuationStaysOffTheStartOfALine() {
        FontMetrics metrics = metrics();

        for (String line : linesOf(MD3Text.wrapToLines(metrics, CHINESE_DESCRIPTION,
                metrics.stringWidth(TEN_HAN), 3))) {
            assertFalse("，。）：；".indexOf(line.charAt(0)) >= 0,
                    "a line may not open with a closing mark: " + line);
        }
    }

    @Test
    public void chineseTooLongForItsLinesIsEllipsised() {
        FontMetrics metrics = metrics();
        String html = MD3Text.wrapToLines(metrics, CHINESE_DESCRIPTION, metrics.stringWidth(TEN_HAN), 1);

        assertTrue(html.endsWith("…</html>"), "expected an ellipsis, got: " + html);
    }

    @Test
    public void englishStillBreaksBetweenWords() {
        FontMetrics metrics = metrics();
        String text = "Install the Forge mod loader for this instance";
        String[] lines = linesOf(MD3Text.wrapToLines(metrics, text, metrics.stringWidth("Install the Forge"), 4));

        assertTrue(lines.length > 1, "expected the sentence to wrap");

        for (String line : lines) {
            for (String word : line.replace("…", "").trim().split(" ")) {
                assertTrue(word.isEmpty() || text.contains(word), "word was split mid-way: " + word);
            }
        }
    }

    @Test
    public void shortTextIsLeftOnOneLine() {
        FontMetrics metrics = metrics();
        String html = MD3Text.wrapToLines(metrics, "安装", metrics.stringWidth(TEN_HAN), 2);

        assertEquals("<html>安装</html>", html);
    }

    @Test
    public void blankTextKeepsTheRowsHeight() {
        assertEquals(" ", MD3Text.wrapToLines(metrics(), "   ", 100, 2));
    }
}
