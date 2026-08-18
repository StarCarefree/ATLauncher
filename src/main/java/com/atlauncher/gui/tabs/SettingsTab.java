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
package com.atlauncher.gui.tabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.gui.tabs.settings.BackupsSettingsTab;
import com.atlauncher.gui.tabs.settings.CommandsSettingsTab;
import com.atlauncher.gui.tabs.settings.EnvironmentVariablesTab;
import com.atlauncher.gui.tabs.settings.GeneralSettingsTab;
import com.atlauncher.gui.tabs.settings.JavaSettingsTab;
import com.atlauncher.gui.tabs.settings.LoggingSettingsTab;
import com.atlauncher.gui.tabs.settings.ModsSettingsTab;
import com.atlauncher.gui.tabs.settings.NetworkSettingsTab;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.viewmodel.impl.settings.BackupsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.CommandsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.EnvironmentVariablesViewModel;
import com.atlauncher.viewmodel.impl.settings.GeneralSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.JavaSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.LoggingSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.ModsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.NetworkSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.SettingsViewModel;

public class SettingsTab extends HierarchyPanel implements Tab {
    @Nullable
    private MD3Tabs tabs;
    @Nullable
    private JPanel sections;
    @Nullable
    private CardLayout sectionLayout;
    @Nullable
    private MD3Button saveButton;

    private SettingsViewModel viewModel;

    // We maintain the state at the top level for all tabs

    private BackupsSettingsViewModel backupSettingsViewModel;
    private CommandsSettingsViewModel commandsSettingsViewModel;
    private GeneralSettingsViewModel generalSettingsViewModel;
    private JavaSettingsViewModel javaSettingsViewModel;
    private EnvironmentVariablesViewModel environmentVariablesViewModel;
    private LoggingSettingsViewModel loggingSettingsViewModel;
    private ModsSettingsViewModel modsSettingsViewModel;
    private NetworkSettingsViewModel networkSettingsViewModel;

    @Nullable
    private GeneralSettingsTab generalSettingsTab;
    @Nullable
    private ModsSettingsTab modsSettingsTab;
    @Nullable
    private JavaSettingsTab javaSettingsTab;
    @Nullable
    private EnvironmentVariablesTab environmentVariablesTab;
    @Nullable
    private NetworkSettingsTab networkSettingsTab;
    @Nullable
    private LoggingSettingsTab loggingSettingsTab;
    @Nullable
    private BackupsSettingsTab backupsSettingsTab;
    @Nullable
    private CommandsSettingsTab commandSettingsTab;
    @Nullable
    private List<Tab> sectionTabs;

    private int selectedTabIndex = 0;

    public SettingsTab() {
        setLayout(new BorderLayout());
    }

    @Override
    protected void createViewModel() {
        viewModel = new SettingsViewModel();

        backupSettingsViewModel = new BackupsSettingsViewModel();
        commandsSettingsViewModel = new CommandsSettingsViewModel();
        generalSettingsViewModel = new GeneralSettingsViewModel();
        javaSettingsViewModel = new JavaSettingsViewModel();
        environmentVariablesViewModel = new EnvironmentVariablesViewModel();
        loggingSettingsViewModel = new LoggingSettingsViewModel();
        modsSettingsViewModel = new ModsSettingsViewModel();
        networkSettingsViewModel = new NetworkSettingsViewModel();
    }

    @SuppressWarnings("null")
    @Override
    protected void onShow() {
        saveButton = MD3Button.filled(GetText.tr("Save"));
        tabs = new MD3Tabs();
        tabs.setBackground(MD3Color.surfaceContainer());
        sectionLayout = new CardLayout();
        sections = new JPanel(sectionLayout);
        sections.setOpaque(false);
        sections.setBackground(MD3Color.surface());

        generalSettingsTab = new GeneralSettingsTab(generalSettingsViewModel);
        modsSettingsTab = new ModsSettingsTab(modsSettingsViewModel);
        javaSettingsTab = new JavaSettingsTab(javaSettingsViewModel);
        networkSettingsTab = new NetworkSettingsTab(networkSettingsViewModel);
        loggingSettingsTab = new LoggingSettingsTab(loggingSettingsViewModel);
        backupsSettingsTab = new BackupsSettingsTab(backupSettingsViewModel);
        commandSettingsTab = new CommandsSettingsTab(commandsSettingsViewModel);
        environmentVariablesTab = new EnvironmentVariablesTab(environmentVariablesViewModel);
        sectionTabs = Arrays.asList(
                new Tab[] { this.generalSettingsTab, this.modsSettingsTab, this.javaSettingsTab,
                        this.networkSettingsTab, this.loggingSettingsTab, this.backupsSettingsTab,
                        this.commandSettingsTab, this.environmentVariablesTab });

        for (int i = 0; i < sectionTabs.size(); i++) {
            Tab tab = sectionTabs.get(i);

            // each section scrolls on its own: they are now a list of full-width rows rather than a
            // grid sized to whatever fitted, and the longer ones do not fit a window
            JScrollPane scrollPane = new JScrollPane((JPanel) tab, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(null);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.getViewport().setBackground(MD3Color.surface());
            scrollPane.getVerticalScrollBar().setUnitIncrement(MD3Spacing.scale(MD3Spacing.L));

            sections.add(scrollPane, String.valueOf(i));
            tabs.addTab(tab.getTitle()).setName("settingsSection." + tab.getAnalyticsScreenViewName());
        }

        tabs.setSelectedIndex(selectedTabIndex);
        sectionLayout.show(sections, String.valueOf(selectedTabIndex));

        add(tabs, BorderLayout.NORTH);
        add(sections, BorderLayout.CENTER);
        add(buildSaveBar(), BorderLayout.SOUTH);

        addDisposable(viewModel.getSaveEnabled().subscribe(saveButton::setEnabled));
        saveButton.addActionListener(arg0 -> viewModel.save());

        tabs.addChangeListener(e -> {
            selectedTabIndex = tabs.getSelectedIndex();
            sectionLayout.show(sections, String.valueOf(selectedTabIndex));
            Analytics.sendScreenView(
                    sectionTabs.get(selectedTabIndex).getAnalyticsScreenViewName() + " Settings");
        });
    }

    /**
     * Settings apply on save rather than as they are changed, so the button that does it stays
     * visible whichever section is open and wherever it has been scrolled to.
     */
    private JPanel buildSaveBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(true);
        bar.setBackground(MD3Color.surfaceContainer());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
        actions.add(saveButton);

        bar.add(new MD3Divider(), BorderLayout.NORTH);
        bar.add(actions, BorderLayout.CENTER);

        return bar;
    }

    @Override
    protected void onDestroy() {
        removeAll();
        tabs = null;
        sections = null;
        sectionLayout = null;
        saveButton = null;

        generalSettingsTab = null;
        modsSettingsTab = null;
        javaSettingsTab = null;
        environmentVariablesTab = null;
        networkSettingsTab = null;
        loggingSettingsTab = null;
        backupsSettingsTab = null;
        commandSettingsTab = null;
        sectionTabs = null;
    }

    @Override
    public String getTitle() {
        return GetText.tr("Settings");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        // since this is the default, this is the main view name
        return "General Settings";
    }
}
