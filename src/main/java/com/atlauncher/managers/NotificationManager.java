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
package com.atlauncher.managers;

import java.awt.Window;

import com.atlauncher.App;
import com.atlauncher.gui.md3.feedback.MD3Snackbar;

/**
 * Brief messages about something that has just finished.
 *
 * <p>
 * Replaces the vendored toaster, which floated its own always-on-top windows in the corner of the
 * <em>screen</em> - so a message about the launcher appeared over whatever else the user had open,
 * and outlived the window it came from. A snackbar belongs to the launcher's own window.
 *
 * <p>
 * Only for things nobody has to act on. Anything that failed goes to {@link DialogManager}: a
 * message that takes itself away after four seconds is not a way to report that a backup did not
 * happen.
 *
 * <p>
 * Here rather than at the call sites because most of them are in data classes - an instance
 * finishing a backup should not have to know which window is on screen.
 */
public final class NotificationManager {
    private NotificationManager() {
    }

    /**
     * @param message what happened, in the past tense - "Backup is complete"
     */
    public static void show(String message) {
        Window parent = parent();

        if (parent == null) {
            // before the window exists, or after it has gone. The log already has this
            LogManager.info(message);

            return;
        }

        MD3Snackbar.show(parent, message);
    }

    private static Window parent() {
        if (App.launcher == null) {
            return null;
        }

        return App.launcher.getParent();
    }
}
