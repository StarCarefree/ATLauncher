/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mini2Dx.gettext.GetText;
import org.mini2Dx.gettext.PoFile;

import com.atlauncher.App;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.utils.Utils;

public class Language {
    public final static List<Locale> locales = new ArrayList<>();
    public final static Map<String, Locale> languages = new LinkedHashMap<>();
    public final static List<Locale> localesWithoutFont = new ArrayList<>();
    public final static List<Locale> localesWithoutTabFont = new ArrayList<>();

    /**
     * Names Java does not have a good one for. Java calls these 中文 (中国) and 中文 (台灣), naming
     * them for a country, when what tells them apart and what a reader looks for is the script.
     */
    private final static Map<Locale, String> displayNames = new HashMap<>();

    // filled here rather than in the block below, which runs after the field that reads it
    static {
        displayNames.put(new Locale("zh", "CN"), "简体中文");
        displayNames.put(new Locale("zh", "TW"), "繁體中文");
    }

    /**
     * The language in use, by the name {@link #displayName} gives it. Starts as English because
     * that is what the launcher reads as before any translation has been loaded - not because
     * English is the default, which is {@link Settings#language}'s to say.
     */
    public static String selected = displayName(Locale.ENGLISH);
    public static Locale selectedLocale = Locale.ENGLISH;

    // add in the languages we have support for
    static {
        locales.add(Locale.ENGLISH); // English
        locales.add(new Locale("af", "ZA")); // Afrikaans
        locales.add(new Locale("ar", "SA")); // Arabic
        locales.add(new Locale("ca", "ES")); // Catalan
        locales.add(new Locale("zh", "CN")); // Chinese Simplified
        locales.add(new Locale("zh", "TW")); // Chinese Traditional
        locales.add(new Locale("cs", "CZ")); // Czech
        locales.add(new Locale("da", "DK")); // Danish
        locales.add(new Locale("nl", "NL")); // Dutch
        locales.add(new Locale("fi", "FI")); // Finnish
        locales.add(new Locale("fr", "FR")); // French
        locales.add(new Locale("de", "DE")); // German
        locales.add(new Locale("el", "GR")); // Greek
        locales.add(new Locale("he", "IL")); // Hebrew
        locales.add(new Locale("hu", "HU")); // Hungarian
        locales.add(new Locale("it", "IT")); // Italian
        locales.add(new Locale("ja", "JP")); // Japanese
        locales.add(new Locale("ko", "KR")); // Korean
        locales.add(new Locale("no", "NO")); // Norwegian
        locales.add(new Locale("pl", "PL")); // Polish
        locales.add(new Locale("pt", "PT")); // Portuguese
        locales.add(new Locale("pt", "BR")); // Portuguese, Brazilian
        locales.add(new Locale("ro", "RO")); // Romanian
        locales.add(new Locale("ru", "RU")); // Russian
        locales.add(new Locale("sr", "SP")); // Serbian
        locales.add(new Locale("es", "ES")); // Spanish
        locales.add(new Locale("sv", "SE")); // Swedish
        locales.add(new Locale("tr", "TR")); // Turkish
        locales.add(new Locale("uk", "UA")); // Ukranian

        localesWithoutFont.add(new Locale("ar", "SA"));
        localesWithoutFont.add(new Locale("zh", "CN"));
        localesWithoutFont.add(new Locale("zh", "TW"));
        localesWithoutFont.add(new Locale("he", "IL"));
        localesWithoutFont.add(new Locale("ja", "JP"));
        localesWithoutFont.add(new Locale("ko", "KR"));

        localesWithoutTabFont.add(new Locale("ar", "SA"));
        localesWithoutTabFont.add(new Locale("zh", "CN"));
        localesWithoutTabFont.add(new Locale("zh", "TW"));
        localesWithoutTabFont.add(new Locale("he", "IL"));
        localesWithoutTabFont.add(new Locale("el", "GR"));
        localesWithoutTabFont.add(new Locale("ja", "JP"));
        localesWithoutTabFont.add(new Locale("ko", "KR"));
    }

    /**
     * What a language is called, in that language.
     *
     * <p>
     * Not {@link Locale#getDisplayName()}, which names a language in whichever locale the JVM
     * happens to be running in - so the picker read "英语" on one machine and "English" on another,
     * and since {@link Settings#language} stores this string, a settings file stopped matching the
     * moment the operating system's language changed, dropping the launcher back to English with
     * nothing said. A language named in its own language is the same string everywhere, and is also
     * what a reader scanning the list is looking for.
     */
    public static String displayName(Locale locale) {
        String own = displayNames.get(locale);

        return own != null ? own : locale.getDisplayName(locale);
    }

    /**
     * The name a stored setting refers to, in today's terms.
     *
     * <p>
     * Settings written before languages were named in their own language hold whatever
     * {@link Locale#getDisplayName()} produced on that machine, so the names are matched that way
     * too and handed back under the current name. Only the JVM's own locale is tried, which is the
     * one that wrote the file on every upgrade that is not also a change of operating system
     * language - that case was already broken and is what naming them this way puts a stop to.
     */
    public static String migrateName(String language) {
        if (language == null || isLanguageByName(language) || displayName(Locale.ENGLISH).equals(language)) {
            return language;
        }

        for (Locale locale : locales) {
            if (locale.getDisplayName().equals(language)) {
                LogManager.info("Language " + language + " is now known as " + displayName(locale));

                return displayName(locale);
            }
        }

        return language;
    }

    public static void init() throws IOException {
        for (Locale locale : locales) {
            if (Utils.getResourceInputStream(
                    "/assets/lang/" + locale.getLanguage() + "-" + locale.getCountry() + ".po") != null) {
                languages.put(displayName(locale), locale);
                LogManager.debug("Loaded language " + displayName(locale) + " with key of " + locale);
            }
        }
    }

    public static void setLanguage(String language) {
        language = migrateName(language);

        if (selected.equals(language)) {
            return;
        }

        Locale locale;

        if (isLanguageByName(language)) {
            LogManager.info("Language set to " + language);
            locale = languages.get(language);
            selected = language;
        } else {
            LogManager.info("Unknown language " + language + ". Defaulting to " + displayName(Locale.ENGLISH));
            locale = Locale.ENGLISH;
            selected = displayName(Locale.ENGLISH);
        }

        if (!locale.equals(Locale.ENGLISH)) {
            try {
                GetText.add(
                        new PoFile(locale, App.class.getResourceAsStream(
                                "/assets/lang/" + locale.getLanguage() + "-" + locale.getCountry() + ".po")));
            } catch (IOException e) {
                LogManager.logStackTrace("Failed loading language po file for " + language, e);
                locale = Locale.ENGLISH;
                selected = displayName(Locale.ENGLISH);
            }
        }

        selectedLocale = locale;

        GetText.setLocale(locale);
        RelocalizationManager.post();
    }

    public static boolean isLanguageByName(String language) {
        return languages.containsKey(language);
    }
}
