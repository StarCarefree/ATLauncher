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

import java.text.SimpleDateFormat;

import com.atlauncher.App;
import com.atlauncher.graphql.GetNewsQuery.GeneralNew;

/**
 * Because there are two types of news in ATLauncher, but are used for the same purpose, this is a combing class.
 */
public class AbstractNews {

    /**
     * HTML row entry
     */
    public final String htmlEntry;

    /**
     * The headline, on its own.
     */
    public final String title;

    /**
     * When it was published, already formatted to the user's date preference.
     */
    public final String date;

    /**
     * The body, as HTML. Excludes the title and date, which the view presents itself.
     */
    public final String content;

    /**
     * Source object, just in case you want to use it.
     */
    public final Object source;

    /**
     * Create from {@link News}
     *
     * @param fileNews Old file json news
     */
    public AbstractNews(News fileNews) {
        source = fileNews;
        htmlEntry = fileNews.getHTML();
        title = fileNews.getTitle();
        date = fileNews.getFormattedDate();
        content = fileNews.getContent();
    }

    /**
     * Create from {@link GeneralNew}
     *
     * @param networkNews GraphQL network result
     */
    public AbstractNews(GeneralNew networkNews) {
        final SimpleDateFormat formatter = new SimpleDateFormat(App.settings.dateFormat + " HH:mm:ss a");
        source = networkNews;
        title = networkNews.title();
        date = formatter.format(networkNews.createdAt());
        content = networkNews.content();
        htmlEntry = "<h2>" + title + " (" + date + ")</h2>" + "<p>" + content + "</p>";
    }
}
