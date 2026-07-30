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
package com.atlauncher.gui.panels;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * What a page shows while it is fetching what it is going to show.
 *
 * <p>
 * This was an animated GIF and a bare {@link JLabel}, and it was the last piece of the launcher's
 * everyday chrome that could not take a theme: {@code loading-bars.gif} is a fixed set of pixels in
 * one colour, so it stayed that colour under all eighteen themes and stayed the same physical size
 * whatever the display scale. It is now {@link MD3CircularProgress}, painted from the theme's own
 * primary colour at whatever size the display asks for.
 *
 * <p>
 * The column is centred rather than stacked into the top left, so a loading state handed the whole
 * of a page - the news tab, the pack browser, the mod browser - reads as the page being busy rather
 * than as a page that has loaded one small thing in its corner.
 */
public class LoadingPanel extends JPanel {
    public LoadingPanel() {
        this(GetText.tr("Loading..."));
    }

    public LoadingPanel(String text) {
        super(new GridBagLayout());

        setOpaque(false);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        MD3CircularProgress spinner = MD3CircularProgress.indeterminate();
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        column.add(spinner);
        column.add(label);

        add(column);
    }
}
