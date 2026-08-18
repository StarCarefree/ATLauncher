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
package com.atlauncher.workers;

import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

/**
 * A blank URL used to resolve to the {@code remote_image} cache folder. Opening that
 * directory as a file is AccessDeniedException on Windows.
 */
public class BackgroundImageWorkerTest {
    @Test
    public void aBlankUrlDoesNotOpenTheCacheDirectory() throws Exception {
        assertNull(BackgroundImageWorker.cacheFile(""));
        assertNull(BackgroundImageWorker.cacheFile("   "));
        assertNull(BackgroundImageWorker.cacheFile("://"));
        assertNull(new BackgroundImageWorker(new JLabel(), "", 32, 32).doInBackground());
    }
}
