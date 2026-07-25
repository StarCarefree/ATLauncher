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
package com.atlauncher.gui.tabs.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.Border;

import com.atlauncher.App;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.utils.Utils;

/**
 * A section of the settings, laid out as a list of rows.
 *
 * <p>
 * The rows themselves are {@link MD3SettingsList}'s, which the instance settings dialog builds from
 * too. This adds what a section of the settings page needs on top of them: the tab lifecycle, and
 * the scrolling behaviour of the pane each section sits in.
 */
public abstract class AbstractSettingsTab extends HierarchyPanel implements Tab, Scrollable {
    final Border RESTART_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 5);

    private final MD3SettingsList settings = new MD3SettingsList();

    public AbstractSettingsTab() {
        setLayout(new BorderLayout());
        setOpaque(false);

        add(settings, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return settings.getScrollableUnitIncrement(visible, orientation, direction);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return settings.getScrollableBlockIncrement(visible, orientation, direction);
    }

    /**
     * Takes the width of the scroll pane it is in rather than of its widest row. See
     * {@link MD3SettingsList#getScrollableTracksViewportWidth()} for why that matters.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * The warning glyph, loaded when something actually has to be warned about.
     *
     * <p>
     * Not a field: a section is constructed before it is shown, which can be before a theme has
     * been installed, and there is no icon to load until there is. The help and error icons that
     * used to sit beside it are gone - a setting's explanation is now part of its row rather than a
     * tooltip on a glyph.
     */
    static ImageIcon warningIcon() {
        return Utils.getIconImage(App.THEME.getIconPath("warning"));
    }

    /**
     * Clears the settings, not the tab.
     *
     * <p>
     * Every section's {@code onDestroy} drops what it built this way. The list they are built into
     * is part of the tab's own structure, so it has to survive being torn down and shown again.
     */
    @Override
    public void removeAll() {
        settings.clear();
    }

    /**
     * A heading over the rows that follow, for a section with more than one idea in it.
     */
    protected JComponent addSection(String title) {
        return settings.addSection(title);
    }

    /**
     * A setting. See {@link MD3SettingsList#addRow}.
     */
    protected MD3SettingsList.Row addRow(String label, String help, JComponent control) {
        return settings.addRow(label, help, control);
    }

    /**
     * A row whose control spans the width instead of sitting on the trailing edge - a table, a text
     * area, anything that would be unusable at a control's width.
     */
    protected MD3SettingsList.Row addWideRow(String label, String help, JComponent control) {
        return settings.addWideRow(label, help, control);
    }

    /**
     * Puts controls in a row, for the settings that take a field and a button to go with it.
     */
    protected static JPanel group(Component... components) {
        return MD3SettingsList.group(components);
    }
}
