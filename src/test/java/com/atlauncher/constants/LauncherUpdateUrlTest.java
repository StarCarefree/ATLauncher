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
package com.atlauncher.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.atlauncher.data.Settings;

/**
 * The address the launcher fetches a new build from, and that automatic updates start on.
 */
public class LauncherUpdateUrlTest {
    @Test
    public void theBinaryUrlIsOnTheCdn() {
        assertEquals("https://download.nodecdn.net/containers/atl/ATLauncher.exe",
                Constants.launcherBinaryUrl("exe"));
        assertEquals("https://download.nodecdn.net/containers/atl/ATLauncher.jar",
                Constants.launcherBinaryUrl("jar"));
        assertEquals("https://atlauncher.com/downloads", Constants.LAUNCHER_DOWNLOADS_URL);
    }

    @Test
    public void automaticUpdatesAreOnByDefault() {
        assertTrue(new Settings().enableLauncherAutoUpdate,
                "a new install would skip updates unless the user asked");
    }
}
