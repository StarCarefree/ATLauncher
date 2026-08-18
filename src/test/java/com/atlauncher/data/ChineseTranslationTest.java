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
package com.atlauncher.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mini2Dx.gettext.GetText;
import org.mini2Dx.gettext.PoFile;

/**
 * Checks that the Chinese translation is picked up and read the way the launcher reads it.
 *
 * <p>
 * {@link PoFile} takes an {@link InputStream} and no charset, so what a translation decodes to
 * depends on where it is read. Nothing about a mis-decoded file throws - the launcher starts, the
 * menus fill with mojibake, and the first sign of it is a screenshot. The same goes for a msgid
 * that no longer matches its source: gettext answers with the English, silently.
 */
public class ChineseTranslationTest {
    private static final Locale ZH_CN = new Locale("zh", "CN");

    @BeforeAll
    public static void loadTranslation() throws IOException {
        try (InputStream in = ChineseTranslationTest.class.getResourceAsStream("/assets/lang/zh-CN.po")) {
            assertNotNull(in, "zh-CN.po is not on the classpath");

            GetText.add(new PoFile(ZH_CN, in));
        }
    }

    @Test
    public void theLauncherOffersChineseInTheLanguageList() throws IOException {
        Language.init();

        assertTrue(Language.languages.containsValue(ZH_CN),
                "a language is only offered when its po file is found: " + Language.languages.keySet());
    }

    @Test
    public void navigationComesBackInChinese() {
        assertEquals("实例", GetText.tr(ZH_CN, "Instances"));
        assertEquals("整合包", GetText.tr(ZH_CN, "Packs"));
        assertEquals("服务器", GetText.tr(ZH_CN, "Servers"));
        assertEquals("设置", GetText.tr(ZH_CN, "Settings"));
        assertEquals("字体", GetText.tr(ZH_CN, "Font"));
        assertEquals("英文字体", GetText.tr(ZH_CN, "English Font"));
        assertEquals("中文字体", GetText.tr(ZH_CN, "Chinese Font"));
        assertEquals("自动（主题）", GetText.tr(ZH_CN, "Auto (theme)"));
        assertEquals("自动（系统）", GetText.tr(ZH_CN, "Auto (system)"));
        assertEquals("系统默认", GetText.tr(ZH_CN, "System default"));
        assertEquals("可选", GetText.tr(ZH_CN, "Optional"));
        assertEquals("必需", GetText.tr(ZH_CN, "Required"));
    }

    @Test
    public void charactersOutsideLatin1SurviveTheRead() {
        // catches the file being decoded as the platform charset rather than UTF-8, which leaves
        // every one of these as a pair of replacement characters
        assertEquals("正在加载…", GetText.tr(ZH_CN, "Loading..."));
        assertEquals("无效！", GetText.tr(ZH_CN, "Invalid!"));
        assertEquals("未发现可用更新。", GetText.tr(ZH_CN, "No updates were found."));
    }

    @Test
    public void placeholdersStillTakeTheirArgument() {
        assertEquals("正在安装 AllTheMods 9", GetText.tr(ZH_CN, "Installing {0}", "AllTheMods 9"));
        assertEquals("正在获取 CurseForge 信息", GetText.tr(ZH_CN, "Getting {0} Information", "CurseForge"));
    }

    @Test
    public void anythingNotTranslatedYetFallsBackToEnglish() {
        String untranslated = "Reinstalling Mods";

        assertEquals(untranslated, GetText.tr(ZH_CN, untranslated),
                "an untranslated string must come back as its English source, not blank");
    }

    /**
     * Reads the entries back out of the file rather than through gettext, because what is being
     * checked is what a translator wrote, and gettext has already thrown some of that away by the
     * time a string comes back out of it.
     */
    private static List<String[]> entries() throws IOException {
        List<String[]> entries = new ArrayList<>();
        Pattern pattern = Pattern.compile("^msgid (\".*\")\\R^msgstr (\".*\")$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(read("/assets/lang/zh-CN.po"));

        while (matcher.find()) {
            entries.add(new String[] { matcher.group(1), matcher.group(2) });
        }

        assertTrue(entries.size() > 100, "expected the whole file, matched " + entries.size() + " entries");

        return entries;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = ChineseTranslationTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is not on the classpath");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];

            for (int read = in.read(chunk); read != -1; read = in.read(chunk)) {
                buffer.write(chunk, 0, read);
            }

            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> matches(String pattern, String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(text);

        while (matcher.find()) {
            found.add(matcher.group());
        }

        Collections.sort(found);

        return found;
    }

    @Test
    public void noTranslationLosesAPlaceholder() throws IOException {
        for (String[] entry : entries()) {
            assertEquals(matches("\\{\\d+\\}", entry[0]), matches("\\{\\d+\\}", entry[1]),
                    "a dropped or renumbered placeholder formats as literal text or throws: " + entry[0]);
        }
    }

    @Test
    public void noTranslationLosesItsMarkup() throws IOException {
        for (String[] entry : entries()) {
            assertEquals(matches("<[^>]+>", entry[0]), matches("<[^>]+>", entry[1]),
                    "these strings are rendered by a label, so a dropped tag changes the layout: " + entry[0]);
        }
    }
}
