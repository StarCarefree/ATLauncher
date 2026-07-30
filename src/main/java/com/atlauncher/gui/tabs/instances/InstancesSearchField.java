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
package com.atlauncher.gui.tabs.instances;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.viewmodel.base.IInstancesTabViewModel;

/**
 * The instances toolbar's search box.
 *
 * <p>
 * Was a plain {@link javax.swing.JTextField} carrying FlatLaf's search decorations, which left the
 * one page users spend the most time on with a 23 pixel box beside 32dp chips - and 23 pixels at
 * every display scale, since the size was set in raw pixels rather than through the scale. The rest
 * of the launcher's toolbars already used {@link MD3TextField#search}.
 *
 * <p>
 * It also filters as you type rather than on Enter. The instances are already in memory and the
 * filter is a {@link java.util.regex.Pattern} over their names, so there was never anything to wait
 * for - the only reason to make the user press a key was that the field had no other way to know
 * they had finished. A short settling delay does that instead, and keeps a re-render off every
 * keystroke.
 */
public final class InstancesSearchField extends MD3TextField implements KeyListener {
    /** How long typing has to stop for before the list is filtered. */
    private static final int SETTLE_MS = 250;
    private static final int COLUMNS = 16;

    private final IInstancesTabViewModel viewModel;

    private final Timer settle = new Timer(SETTLE_MS, e -> applySearch());

    public InstancesSearchField(final IInstancesTabViewModel viewModel) {
        super(GetText.tr("Search"), Variant.SEARCH);

        this.viewModel = viewModel;

        setName("instancesSearchField");
        setColumns(COLUMNS);
        setLeadingIcon(MD3Icons.SEARCH);
        setText(viewModel.getSearch());

        settle.setRepeats(false);

        // attached after the initial text, so restoring the last search does not schedule one
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                settle.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                settle.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                settle.restart();
            }
        });

        addKeyListener(this);
    }

    /**
     * The page is torn down and rebuilt every time it is shown, so a settle left pending would fire
     * into a toolbar that is no longer on screen.
     */
    @Override
    public void removeNotify() {
        settle.stop();

        super.removeNotify();
    }

    /**
     * An empty box means no filter at all, rather than a filter that happens to match everything.
     */
    private void applySearch() {
        String text = getText();

        viewModel.setSearch(text == null || text.isEmpty() ? null : text);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyChar() != KeyEvent.VK_ENTER) {
            return;
        }

        // Enter still commits, so a user who expects to press it is not left waiting a quarter of a
        // second for something they think they have just asked for
        settle.stop();
        applySearch();

        Analytics.trackEvent(AnalyticsEvent.forSearchEvent("instances", getText()));
    }
}
