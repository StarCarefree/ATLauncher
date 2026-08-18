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

import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.atlauncher.App;
import com.atlauncher.themes.ATLauncherLaf;

/**
 * The two faces the console draws with: JetBrains Mono Medium for everything it
 * covers, and a platform face for the rest - Chinese, Japanese, Korean, and
 * anything else a programming face does not ship.
 *
 * <p>
 * Swing will not substitute missing glyphs on a {@code JTextPane} the way a
 * browser does. Setting the pane to JetBrains Mono and leaving it at that
 * draws every CJK character as an empty box, which is how a log of a Chinese
 * instance used to look. Splitting each line into runs, and giving the runs
 * the Latin face cannot draw to a face that can, is what makes the same log
 * readable.
 */
public final class ConsoleFonts {
    /**
     * Marks the console so a language-driven font walk leaves its monospaced face
     * alone. The UI face follows the locale; the log face does not.
     */
    public static final String TYPE_ROLE_KEY = ATLauncherLaf.CONSOLE_FONT_KEY;

    /** Sample used to decide whether a candidate face can actually draw CJK. */
    private static final String CJK_SAMPLE = "汉字";

    /**
     * Families tried, in order, for a run JetBrains Mono cannot draw. The logical
     * faces at the end are Java composites and pick up whatever the OS installed.
     */
    private static final String[] FALLBACK_FAMILIES = {
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "PingFang SC",
            "Hiragino Sans GB",
            "Noto Sans CJK SC",
            "Noto Sans SC",
            "Source Han Sans SC",
            "WenQuanYi Micro Hei",
            "Noto Sans CJK JP",
            Font.SANS_SERIF,
            Font.DIALOG
    };

    private static Font latinCache;
    private static Font fallbackCache;
    private static String cacheKey;

    private ConsoleFonts() {
    }

    /**
     * @return the console point size from the theme, or 12 if a theme has not
     *         published one
     */
    public static float size() {
        Object value = UIManager.get("Console.fontSize");

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        return 12f;
    }

    /**
     * JetBrains Mono Medium, or the platform monospaced face if that file did not
     * load as a monospaced one.
     *
     * <p>
     * The columns in the log are held apart by padding the level tag to a fixed
     * number of characters, which only lines up if every Latin character is the
     * same width. A proportional face would put the messages at a different
     * column on every line.
     */
    public static Font latin() {
        resolve();
        return latinCache;
    }

    /**
     * A face that can draw Chinese. Same size and style as {@link #latin()}, so a
     * mixed line does not jump in height where the script changes.
     */
    public static Font fallback() {
        resolve();
        return fallbackCache;
    }

    /**
     * The face that should draw {@code text}: the Latin one when it can, the
     * fallback when it cannot.
     */
    public static Font forText(String text) {
        Font latin = latin();

        if (text == null || text.isEmpty() || latin.canDisplayUpTo(text) < 0) {
            return latin;
        }

        return fallback();
    }

    /**
     * Writes {@code text} into the document, splitting it wherever the Latin face
     * runs out of glyphs.
     *
     * @param style colour and weight for the whole stretch; the face is added per
     *              run
     */
    public static void insert(StyledDocument document, String text, SimpleAttributeSet style)
            throws BadLocationException {
        if (text == null || text.isEmpty()) {
            return;
        }

        Font latin = latin();
        int i = 0;
        int length = text.length();

        while (i < length) {
            int codePoint = text.codePointAt(i);
            boolean useLatin = latin.canDisplay(codePoint);
            int j = i + Character.charCount(codePoint);

            while (j < length) {
                int next = text.codePointAt(j);

                if (latin.canDisplay(next) != useLatin) {
                    break;
                }

                j += Character.charCount(next);
            }

            SimpleAttributeSet run = new SimpleAttributeSet(style);

            if (!useLatin) {
                Font fallback = fallback();
                StyleConstants.setFontFamily(run, fallback.getFamily());
                StyleConstants.setFontSize(run, fallback.getSize());
            }

            document.insertString(document.getLength(), text.substring(i, j), run);
            i = j;
        }
    }

    private static synchronized void resolve() {
        String key = cacheKey();

        if (key.equals(cacheKey) && latinCache != null && fallbackCache != null) {
            return;
        }

        Font font;

        if (App.THEME != null) {
            font = App.THEME.getConsoleFont().deriveFont(size());
        } else {
            font = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size()));
        }

        FontMetrics metrics = new JLabel().getFontMetrics(font);

        if (metrics.charWidth('i') != metrics.charWidth('W')) {
            font = new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
        }

        latinCache = font;
        fallbackCache = pickFallback(font.getStyle(), font.getSize());
        cacheKey = key;
    }

    private static String cacheKey() {
        boolean customDisabled = App.settings != null && App.settings.disableCustomFonts;
        Object theme = App.THEME;

        return String.valueOf(theme) + '|' + size() + '|' + customDisabled;
    }

    private static Font pickFallback(int style, int size) {
        for (String family : FALLBACK_FAMILIES) {
            Font font = new Font(family, style, size);

            if (font.canDisplayUpTo(CJK_SAMPLE) < 0) {
                return font;
            }
        }

        return new Font(Font.DIALOG, style, size);
    }
}
