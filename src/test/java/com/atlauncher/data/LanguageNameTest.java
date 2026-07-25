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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Pins what the languages are called and that an older settings file still finds one.
 *
 * <p>
 * The name is not only a label: it is the value {@link Settings#language} stores, so renaming the
 * languages renames the setting, and a name that no longer resolves drops the launcher back to
 * English without saying anything.
 */
public class LanguageNameTest {
    private static final Locale ZH_CN = new Locale("zh", "CN");

    private final Locale before = Locale.getDefault();

    @AfterEach
    public void restoreDefaultLocale() {
        Locale.setDefault(before);
    }

    @Test
    public void chineseIsNamedForItsScriptRatherThanACountry() {
        assertEquals("简体中文", Language.displayName(ZH_CN));
        assertEquals("繁體中文", Language.displayName(new Locale("zh", "TW")));
    }

    /**
     * Java keeps the country in the name of a locale that has one, which is what tells the two
     * Portuguese apart, so it is left in rather than stripped for the sake of the shorter ones.
     */
    @Test
    public void everyLanguageIsNamedInItsOwnLanguage() {
        assertEquals("English", Language.displayName(Locale.ENGLISH));
        assertEquals("Deutsch (Deutschland)", Language.displayName(new Locale("de", "DE")));
        assertNotEquals(Language.displayName(new Locale("pt", "PT")), Language.displayName(new Locale("pt", "BR")));
    }

    @Test
    public void aNameDoesNotChangeWithTheJvmsOwnLocale() {
        Locale.setDefault(Locale.FRANCE);

        assertEquals("English", Language.displayName(Locale.ENGLISH));
        assertEquals("简体中文", Language.displayName(ZH_CN));

        Locale.setDefault(ZH_CN);

        assertEquals("English", Language.displayName(Locale.ENGLISH));
        assertEquals("简体中文", Language.displayName(ZH_CN));
    }

    /**
     * The upgrade path: what a settings file written before this holds is whatever the JVM's own
     * locale called the language at the time.
     */
    @Test
    public void aSettingUnderTheOldNamesIsBroughtForward() throws IOException {
        Language.init();
        Locale.setDefault(ZH_CN);

        String oldEnglish = Locale.ENGLISH.getDisplayName();
        String oldChinese = ZH_CN.getDisplayName();

        assertNotEquals("English", oldEnglish, "this test only means something while the names differ");

        assertEquals("English", Language.migrateName(oldEnglish));
        assertEquals("简体中文", Language.migrateName(oldChinese));
    }

    @Test
    public void aNameAlreadyInTodaysTermsIsLeftAlone() throws IOException {
        Language.init();

        assertEquals("English", Language.migrateName("English"));
        assertEquals("简体中文", Language.migrateName("简体中文"));
    }

    @Test
    public void theDefaultLanguageIsOneThePickerOffers() throws IOException {
        Language.init();

        assertTrue(Language.isLanguageByName(new Settings().language),
                "the launcher would start in English instead: " + Language.languages.keySet());
    }
}
