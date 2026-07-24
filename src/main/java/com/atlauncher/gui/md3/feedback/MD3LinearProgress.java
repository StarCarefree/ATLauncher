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
package com.atlauncher.gui.md3.feedback;

import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.plaf.ProgressBarUI;

/**
 * A Material 3 linear progress indicator.
 *
 * <p>
 * Extends {@link JProgressBar} so it drops straight into the launcher's existing progress plumbing -
 * {@code ProgressDialog} drives two of these through {@code setValue}, {@code setIndeterminate} and
 * {@code setString}, and none of that has to change.
 *
 * <p>
 * Use determinate whenever the total is known, even roughly. An indeterminate bar tells the user
 * only that something is happening, which after a few seconds is indistinguishable from a hang.
 */
public class MD3LinearProgress extends JProgressBar {
    public static final String UI_CLASS_ID = "MD3LinearProgressUI";

    public MD3LinearProgress() {
        super();

        updateUI();
    }

    public MD3LinearProgress(int min, int max) {
        super(min, max);

        updateUI();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3LinearProgressUI.class.getName());
        }

        setUI((ProgressBarUI) UIManager.getUI(this));
    }
}
