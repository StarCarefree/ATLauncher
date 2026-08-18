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
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Instance;
import com.atlauncher.gui.dialogs.instancesettings.CommandsInstanceSettingsTab;
import com.atlauncher.gui.dialogs.instancesettings.GeneralInstanceSettingsTab;
import com.atlauncher.gui.dialogs.instancesettings.JavaInstanceSettingsTab;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.feedback.MD3WindowDialog;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

public class InstanceSettingsDialog extends MD3WindowDialog {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private final Instance instance;

    private final MD3Tabs tabs = new MD3Tabs();
    private final CardLayout sectionLayout = new CardLayout();
    private final JPanel sections = new JPanel(sectionLayout);

    private final GeneralInstanceSettingsTab generalInstanceSettingsTab;
    private final JavaInstanceSettingsTab javaInstanceSettingsTab;
    private final CommandsInstanceSettingsTab commandsInstanceSettingsTab;

    public InstanceSettingsDialog(Instance instance) {
        // #. {0} is the name of the instance
        super(App.launcher.getParent(), GetText.tr("{0} Settings", instance.launcher.name),
                ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        this.generalInstanceSettingsTab = new GeneralInstanceSettingsTab(instance);
        this.javaInstanceSettingsTab = new JavaInstanceSettingsTab(instance);
        this.commandsInstanceSettingsTab = new CommandsInstanceSettingsTab(instance);

        setupComponents();
    }

    private void setupComponents() {
        setDialogSize(WIDTH, HEIGHT);

        sections.setOpaque(false);

        addSection(GetText.tr("General"), generalInstanceSettingsTab);
        addSection(GetText.tr("Java/Minecraft"), javaInstanceSettingsTab);
        addSection(GetText.tr("Commands"), commandsInstanceSettingsTab);

        tabs.setSelectedIndex(0);
        sectionLayout.show(sections, "0");
        tabs.addChangeListener(e -> sectionLayout.show(sections, String.valueOf(tabs.getSelectedIndex())));

        // the tabs name the dialog, so there is no headline over them - three words of section are
        // more use than a fourth repeat of the instance's name
        add(tabs, BorderLayout.NORTH);
        setBody(sections);
        buildActions();
    }

    /**
     * Each section scrolls on its own: they are a list of full-width rows rather than a grid sized
     * to whatever fitted, and the Java one does not fit a dialog.
     */
    private void addSection(String title, JPanel section) {
        JScrollPane scrollPane = new JScrollPane(section, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIScale.scale(MD3Spacing.L));

        sections.add(scrollPane, String.valueOf(tabs.getTabCount()));
        tabs.addTab(title);
    }

    /**
     * Settings apply on save rather than as they are changed, so the buttons that do it stay visible
     * whichever section is open and wherever it has been scrolled to.
     */
    private void buildActions() {
        MD3Button saveButton = MD3Button.filled(GetText.tr("Save"));
        saveButton.addActionListener(arg0 -> {
            if (javaInstanceSettingsTab.isValidJavaPath() && javaInstanceSettingsTab.isValidJavaParamaters()
                    && generalInstanceSettingsTab.isValidQuickPlayOptionValue()) {
                saveSettings();
                NotificationManager.show("Instance Settings Saved");
                close();
            }
        });

        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(arg0 -> close());

        setActions(cancelButton, saveButton);
        setDefaultAction(saveButton);
    }

    private void saveSettings() {
        generalInstanceSettingsTab.saveSettings();
        javaInstanceSettingsTab.saveSettings();
        commandsInstanceSettingsTab.saveSettings();

        this.instance.save();
    }
}
