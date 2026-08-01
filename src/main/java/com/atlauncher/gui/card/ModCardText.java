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
package com.atlauncher.gui.card;

import java.util.Locale;

import org.mini2Dx.gettext.GetText;

/**
 * The small pieces of text a mod card puts on its badges, shared by the two platforms so they read
 * the same way.
 */
final class ModCardText {
    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;

    private ModCardText() {
    }

    /**
     * A download count as a badge.
     *
     * <p>
     * Rounded, because the exact figure is both meaningless and too wide for a badge - what the
     * reader wants from it is the order of magnitude. The unit letters are translatable: not every
     * language shortens a million to M.
     */
    static String downloads(long count) {
        if (count >= MILLION) {
            // #. {0} is a rounded number of downloads, in millions - eg 1.2M
            return GetText.tr("{0}M downloads", String.format(Locale.ENGLISH, "%.1f", count / (double) MILLION));
        }

        if (count >= THOUSAND) {
            // #. {0} is a rounded number of downloads, in thousands - eg 345K
            return GetText.tr("{0}K downloads", String.valueOf(count / THOUSAND));
        }

        // #. {0} is a number of downloads
        return GetText.tr("{0} downloads", String.valueOf(count));
    }
}
