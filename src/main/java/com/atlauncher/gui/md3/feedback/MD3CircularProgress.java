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
 * A Material 3 circular progress indicator.
 *
 * <p>
 * For waits that have no natural place to put a bar - a panel loading its contents, a search in
 * flight. Where there is a container whose width the progress can span, prefer
 * {@link MD3LinearProgress}: a bar communicates how far along something is at a glance, and a
 * spinner does not.
 */
public class MD3CircularProgress extends JProgressBar {
    public static final String UI_CLASS_ID = "MD3CircularProgressUI";

    /** Client property holding an unscaled diameter, as an {@link Integer}. */
    public static final String DIAMETER_KEY = "md3.progress.diameter";

    /** The size of a spinner meant to sit inline on a toolbar line, beside label-sized text. */
    public static final int INLINE_DIAMETER = 18;

    public MD3CircularProgress() {
        super();

        updateUI();
    }

    /**
     * A spinner for a wait of unknown length.
     */
    public static MD3CircularProgress indeterminate() {
        MD3CircularProgress progress = new MD3CircularProgress();
        progress.setIndeterminate(true);

        return progress;
    }

    /**
     * A spinner small enough to stand beside a line of text rather than to fill a panel - for a
     * page that is already showing its contents and is refreshing them.
     */
    public static MD3CircularProgress inline() {
        MD3CircularProgress progress = indeterminate();
        progress.setDiameter(INLINE_DIAMETER);

        return progress;
    }

    /**
     * @param diameter unscaled, as the tokens are; the ring keeps its proportion of it
     */
    public void setDiameter(int diameter) {
        putClientProperty(DIAMETER_KEY, diameter);

        revalidate();
        repaint();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3CircularProgressUI.class.getName());
        }

        setUI((ProgressBarUI) UIManager.getUI(this));
    }
}
