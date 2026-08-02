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
package com.atlauncher.gui.md3.input;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;
import javax.swing.plaf.SpinnerUI;

/**
 * A number field with steppers - the launcher's memory allocation, window size, timeouts and ports.
 *
 * <p>
 * <b>Material 3 has no spinner</b>, and the two things it does have that could stand in for one are
 * both wrong here. A slider says "somewhere around here", and every one of these wants an exact
 * figure - 4096MB, port 25565, not approximately. A plain number field would do, but it would throw
 * away the step the model already defines, which is what makes 512MB at a time a single click
 * rather than a thing to type. So this is what Material builds a quantity control out of: its text
 * field, with icon buttons beside it.
 *
 * <p>
 * Extends {@link JSpinner}, so {@link javax.swing.SpinnerNumberModel}, {@code addChangeListener},
 * {@code getValue} and the {@code NumberEditor} the call sites install all carry over untouched.
 */
public class MD3Spinner extends JSpinner {
    public static final String UI_CLASS_ID = "MD3SpinnerUI";

    public MD3Spinner() {
        updateUI();
    }

    public MD3Spinner(SpinnerModel model) {
        super(model);

        updateUI();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3SpinnerUI.class.getName());
        }

        setUI((SpinnerUI) UIManager.getUI(this));
    }
}
