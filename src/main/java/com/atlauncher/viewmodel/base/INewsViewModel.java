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
package com.atlauncher.viewmodel.base;

import java.util.List;

import com.atlauncher.data.AbstractNews;

import io.reactivex.rxjava3.core.Observable;

/**
 * View model for NewsTab
 */
public interface INewsViewModel {

    /**
     * Observable of the news items to display, most recent first.
     *
     * <p>
     * The articles themselves rather than one HTML document of all of them: the view presents each
     * as its own card, so it needs the title and date apart from the body.
     */
    Observable<List<AbstractNews>> getNews();
}
