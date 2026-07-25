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
package com.atlauncher.gui.tabs.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Language;
import com.atlauncher.data.Settings;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.viewmodel.impl.settings.GeneralSettingsViewModel;

/**
 * Checks that the language picker opens on the language actually in use.
 *
 * <p>
 * It did not: the view model publishes an index, as it does for every other combo on the page, but
 * this one was handed to {@code setSelectedItem}, which went looking for an entry equal to a number
 * and found none. The picker therefore sat on whichever language was listed first and quietly
 * disagreed with the launcher around it - invisible for as long as that first entry happened to be
 * the default.
 */
public class LanguagePickerTest {
    @BeforeEach
    public void setUpLauncher() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);

        Language.init();
    }

    /**
     * The picker is the only combo on the page holding plain strings; the rest hold
     * {@code ComboItem}s.
     */
    private static JComboBox<?> findPicker(Component component, String expectedEntry) {
        if (component instanceof JComboBox) {
            ComboBoxModel<?> model = ((JComboBox<?>) component).getModel();

            for (int i = 0; i < model.getSize(); i++) {
                if (expectedEntry.equals(model.getElementAt(i))) {
                    return (JComboBox<?>) component;
                }
            }
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JComboBox<?> found = findPicker(child, expectedEntry);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static JComboBox<?> buildPageAndFindPicker(String expectedEntry, String settles) throws Exception {
        JComboBox<?>[] picker = new JComboBox<?>[1];

        // onShow rather than a window, which the panel would otherwise wait to be showing in - this
        // test is in the same package, and a visible frame in a test run is its own problem. On the
        // event thread all the same, since that is where the page expects to be built.
        SwingUtilities.invokeAndWait(() -> {
            GeneralSettingsTab tab = new GeneralSettingsTab(new GeneralSettingsViewModel());

            tab.onShow();
            picker[0] = findPicker(tab, expectedEntry);
        });

        if (picker[0] == null) {
            return null;
        }

        // The selection is published from an Rx scheduler onto the event thread, so it can be
        // queued after the page is built rather than before. Draining the queue once therefore
        // passes or fails depending on which thread got there first; wait for the value instead.
        // A page that never settles falls out of here and fails on the assertion, as it should.
        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {
            boolean[] settled = new boolean[1];

            SwingUtilities.invokeAndWait(() -> settled[0] = settles.equals(picker[0].getSelectedItem()));

            if (settled[0]) {
                break;
            }

            Thread.sleep(25);
        }

        return picker[0];
    }

    @Test
    public void thePickerOpensOnTheLanguageInUse() throws Exception {
        App.settings.language = "简体中文";

        JComboBox<?> picker = buildPageAndFindPicker("简体中文", "简体中文");

        assertNotNull(picker, "no combo on the page offers the languages");
        assertEquals("简体中文", picker.getSelectedItem(),
                "the picker disagrees with the language the launcher is running in");
    }

    @Test
    public void thePickerFollowsTheSettingRatherThanTheListOrder() throws Exception {
        App.settings.language = "English";

        JComboBox<?> picker = buildPageAndFindPicker("简体中文", "English");

        assertNotNull(picker, "no combo on the page offers the languages");
        assertEquals("English", picker.getSelectedItem());
    }

    @Test
    public void theDefaultSettingIsALanguageThePickerOffers() throws IOException {
        assertEquals(true, Language.isLanguageByName(new Settings().language),
                "the launcher would fall back to English: " + Language.languages.keySet());
    }
}
