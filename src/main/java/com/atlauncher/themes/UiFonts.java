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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import com.atlauncher.App;
import com.atlauncher.utils.Resources;

/**
 * How the launcher picks faces from the ones the operating system already has.
 *
 * <p>
 * English and Chinese are separate settings. The English face is the UI's own type; the Chinese
 * face is what labels fall back to when the English one cannot draw them. A mixed string - a pack
 * name with a Chinese title and a Latin version number - therefore keeps both.
 *
 * <p>
 * {@code uiFontFamily} is the previous single setting and is still read as the English face so a
 * config written before the split does not forget what it picked.
 */
public final class UiFonts {
    /** Follow the theme, or the platform when the language needs it. */
    public static final String AUTO = "";

    /** The JRE's logical sans, which is the OS UI face on every platform we ship. */
    public static final String SYSTEM = Font.SANS_SERIF;

    /**
     * Bundled English face used when Settings left English on automatic. The same
     * file the console uses, so Latin UI and the log share one family.
     */
    public static final String THEME_LATIN_FONT = "JetBrainsMono-Medium";

    public static final String LATIN_SAMPLE = "Aa";
    public static final String CJK_SAMPLE = "汉字";

    /**
     * Latin / CJK pair derived from a component font. Entries drop when the base
     * font is collected; {@link #invalidate()} drops them when Settings change.
     */
    private static final Map<Font, ScriptFaces> FACES = Collections
            .synchronizedMap(new WeakHashMap<Font, ScriptFaces>());

    private static List<String> englishFamilies;
    private static List<String> chineseFamilies;
    private static String cachedThemeLatinFamily;
    private static Boolean cachedThemeLatinDisableCustomFonts;

    private UiFonts() {
    }

    /**
     * Settings or the theme face changed. Mixed painting keeps derived faces and
     * measured runs; without this a Chinese pick would keep drawing the previous
     * one until the English face object itself was replaced.
     */
    public static void invalidate() {
        FACES.clear();
        cachedThemeLatinFamily = null;
        cachedThemeLatinDisableCustomFonts = null;
    }

    /**
     * The English family Settings asked for, or null to leave the theme / locale fallback in
     * charge.
     */
    public static String explicitEnglishFamily() {
        if (App.settings == null) {
            return null;
        }

        String family = firstSet(App.settings.uiEnglishFontFamily, App.settings.uiFontFamily);

        if (family != null) {
            return family;
        }

        if (App.settings.disableCustomFonts) {
            return SYSTEM;
        }

        return null;
    }

    /**
     * The Chinese family Settings asked for, or null to use the platform face that has CJK.
     */
    public static String explicitChineseFamily() {
        if (App.settings == null) {
            return null;
        }

        return firstSet(App.settings.uiChineseFontFamily, null);
    }

    /**
     * @deprecated use {@link #explicitEnglishFamily()}
     */
    public static String explicitFamily() {
        return explicitEnglishFamily();
    }

    public static Font englishFace(int style, float size) {
        String family = explicitEnglishFamily();

        if (family != null) {
            return face(family, style, size);
        }

        return null;
    }

    public static Font chineseFace(int style, float size) {
        String family = explicitChineseFamily();

        if (family != null) {
            return face(family, style, size);
        }

        return new Font(SYSTEM, style, Math.round(size)).deriveFont(style, size);
    }

    /**
     * A face the same size and weight as {@code preferred} that can draw {@code text}. Used when
     * the English UI face runs out of glyphs.
     */
    public static Font fallbackFor(Font preferred, String text) {
        if (text != null && !text.isEmpty()) {
            return faceFor(preferred, text.codePointAt(0));
        }

        return cjkFace(preferred);
    }

    /**
     * The English face at {@code base}'s size, weight and tracking.
     *
     * <p>
     * Automatic English is the theme's Latin face (JetBrains Mono), never the component's own font. A
     * Chinese locale still swaps the UI default onto a face that can draw CJK so unmigrated Swing
     * controls are not empty boxes - but that default must not steal Latin glyphs from the English
     * setting. Mixed painting asks here for each character.
     */
    public static Font latinFace(Font base) {
        return faces(base).latin;
    }

    /**
     * The Chinese face at {@code base}'s size, weight and tracking.
     */
    public static Font cjkFace(Font base) {
        return faces(base).cjk;
    }

    /**
     * The bundled English family the theme uses when Settings left English on automatic.
     */
    public static String themeLatinFamily() {
        boolean disableCustom = App.settings != null && App.settings.disableCustomFonts;

        if (cachedThemeLatinFamily != null
                && cachedThemeLatinDisableCustomFonts != null
                && cachedThemeLatinDisableCustomFonts.booleanValue() == disableCustom) {
            return cachedThemeLatinFamily;
        }

        cachedThemeLatinFamily = themeLatinFace(Font.PLAIN, 12f).getFamily();
        cachedThemeLatinDisableCustomFonts = Boolean.valueOf(disableCustom);

        return cachedThemeLatinFamily;
    }

    /**
     * CJK code points always use the Chinese setting, even when the English face could draw
     * them (微软雅黑 as the English pick would otherwise swallow the Chinese pick). Everything
     * else uses the English face, falling back to the Chinese one only for a glyph it cannot
     * draw - Arabic on a Latin theme face, for example.
     */
    public static Font faceFor(Font base, int codePoint) {
        ScriptFaces faces = faces(base);

        if (isCjk(codePoint)) {
            return faces.cjk;
        }

        // the English picker only lists faces that can draw Latin; ASCII never needs
        // canDisplay, which is the expensive part of walking a mixed string
        if (codePoint < 128 || faces.latin.canDisplay(codePoint)) {
            return faces.latin;
        }

        if (faces.cjk.canDisplay(codePoint)) {
            return faces.cjk;
        }

        return faces.latin;
    }

    public static boolean isCjk(int codePoint) {
        return (codePoint >= 0x3000 && codePoint <= 0x303F)
                || (codePoint >= 0x3040 && codePoint <= 0x30FF)
                || (codePoint >= 0x31C0 && codePoint <= 0x31EF)
                || (codePoint >= 0x3200 && codePoint <= 0x33FF)
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                || (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7AF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFF00 && codePoint <= 0xFFEF)
                || (codePoint >= 0x20000 && codePoint <= 0x2FA1F);
    }

    public static boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int i = 0;
        int length = text.length();

        while (i < length) {
            int codePoint = text.codePointAt(i);

            if (isCjk(codePoint)) {
                return true;
            }

            i += Character.charCount(codePoint);
        }

        return false;
    }

    public static List<String> familiesForEnglish() {
        List<String> cached = englishFamilies;

        if (cached != null) {
            return cached;
        }

        synchronized (UiFonts.class) {
            if (englishFamilies == null) {
                englishFamilies = Collections.unmodifiableList(familiesThatCanDraw(LATIN_SAMPLE));
            }

            return englishFamilies;
        }
    }

    public static List<String> familiesForChinese() {
        List<String> cached = chineseFamilies;

        if (cached != null) {
            return cached;
        }

        synchronized (UiFonts.class) {
            if (chineseFamilies == null) {
                chineseFamilies = Collections.unmodifiableList(familiesThatCanDraw(CJK_SAMPLE));
            }

            return chineseFamilies;
        }
    }

    /**
     * @deprecated use {@link #familiesForEnglish()} or {@link #familiesForChinese()}
     */
    public static List<String> familiesForUi() {
        return familiesForEnglish();
    }

    private static List<String> familiesThatCanDraw(String sample) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        List<String> families = new ArrayList<String>();

        for (int i = 0; i < installed.length; i++) {
            String family = installed[i];

            if (unusable(family)) {
                continue;
            }

            Font font = new Font(family, Font.PLAIN, 12);

            if (font.canDisplayUpTo(sample) < 0) {
                families.add(family);
            }
        }

        Collections.sort(families, String.CASE_INSENSITIVE_ORDER);

        return families;
    }

    private static Font face(String family, int style, float size) {
        return new Font(family, style, Math.round(size)).deriveFont(style, size);
    }

    private static Font themeLatinFace(int style, float size) {
        if (App.settings != null && App.settings.disableCustomFonts) {
            return face(SYSTEM, style, size);
        }

        return Resources.makeFont(THEME_LATIN_FONT).deriveFont(style, size);
    }

    /**
     * Same size, weight and tracking as {@code base}, but on {@code family}.
     *
     * <p>
     * {@code Font.deriveFont} with a new {@code FAMILY} does not actually switch a physical face
     * onto another - it stays put or falls through to Dialog. The English bundled file and a
     * {@code new Font(family, ...)} for a system face are what really change the glyphs.
     */
    private static Font sameCut(Font base, String family) {
        if (base == null) {
            return namedFace(family, Font.PLAIN, 12f);
        }

        if (family.equals(base.getFamily())) {
            return base;
        }

        Font cut = namedFace(family, base.getStyle(), base.getSize2D());
        Object tracking = base.getAttributes().get(TextAttribute.TRACKING);

        if (tracking instanceof Number && Math.abs(((Number) tracking).floatValue()) >= 0.005f) {
            Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
            attributes.put(TextAttribute.TRACKING, tracking);
            cut = cut.deriveFont(attributes);
        }

        return cut;
    }

    private static Font namedFace(String family, int style, float size) {
        if (family.equals(themeLatinFamily())) {
            return themeLatinFace(style, size);
        }

        return face(family, style, size);
    }

    /**
     * One latin / CJK pair per component font, so walking a mixed string does not
     * build a new face for every code point.
     */
    private static ScriptFaces faces(Font base) {
        if (base == null) {
            return new ScriptFaces(explicitEnglishFamily(), explicitChineseFamily(),
                    disableCustomFonts(), sameCut(null, latinFamily()), sameCut(null, cjkFamily()));
        }

        String english = explicitEnglishFamily();
        String chinese = explicitChineseFamily();
        boolean disableCustom = disableCustomFonts();
        ScriptFaces cached = FACES.get(base);

        if (cached != null && cached.matches(english, chinese, disableCustom)) {
            return cached;
        }

        ScriptFaces faces = new ScriptFaces(english, chinese, disableCustom, sameCut(base, latinFamily()),
                sameCut(base, cjkFamily()));
        FACES.put(base, faces);

        return faces;
    }

    private static String latinFamily() {
        String family = explicitEnglishFamily();

        return family != null ? family : themeLatinFamily();
    }

    private static String cjkFamily() {
        String family = explicitChineseFamily();

        return family != null ? family : SYSTEM;
    }

    private static boolean disableCustomFonts() {
        return App.settings != null && App.settings.disableCustomFonts;
    }

    private static boolean sameNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class ScriptFaces {
        private final String english;
        private final String chinese;
        private final boolean disableCustom;
        final Font latin;
        final Font cjk;

        ScriptFaces(String english, String chinese, boolean disableCustom, Font latin, Font cjk) {
            this.english = english;
            this.chinese = chinese;
            this.disableCustom = disableCustom;
            this.latin = latin;
            this.cjk = cjk;
        }

        boolean matches(String english, String chinese, boolean disableCustom) {
            return this.disableCustom == disableCustom && sameNullable(this.english, english)
                    && sameNullable(this.chinese, chinese);
        }
    }

    private static String firstSet(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary.trim();
        }

        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim();
        }

        return null;
    }

    private static boolean unusable(String family) {
        if (family == null || family.isEmpty() || family.charAt(0) == '@') {
            return true;
        }

        String name = family.toLowerCase(Locale.ROOT);

        return name.contains("emoji") || name.contains("symbol") || name.contains("wingding")
                || name.contains("webding") || name.contains("marlett") || name.contains("awesome")
                || name.contains("icon") || name.contains("extra");
    }
}
