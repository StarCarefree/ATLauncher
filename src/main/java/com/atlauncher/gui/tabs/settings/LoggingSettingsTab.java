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

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.viewmodel.impl.settings.LoggingSettingsViewModel;

public class LoggingSettingsTab extends AbstractSettingsTab {

    private final LoggingSettingsViewModel viewModel;

    public LoggingSettingsTab(LoggingSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected void onShow() {
        // Enable Logging

        MD3Switch enableLogs = new MD3Switch();
        enableLogs.addActionListener(e ->
            viewModel.setEnableLogging(enableLogs.isSelected())
        );
        addDisposable(viewModel.getEnableLogging().subscribe(enableLogs::setSelected));
        addRow(GetText.tr("Enable Logging"), GetText.tr(
                "The Launcher sends back anonymous usage and error logs to our servers in order to make the Launcher and Packs better. If you don't want this to happen then simply disable this option."),
            enableLogs);

        // Enable Analytics

        MD3Switch enableAnalytics = new MD3Switch();
        enableAnalytics.addActionListener(e ->
            viewModel.setEnableAnalytics(enableAnalytics.isSelected())
        );
        addDisposable(viewModel.getEnableAnalytics().subscribe(enableAnalytics::setSelected));
        addRow(GetText.tr("Enable Anonymous Analytics"), GetText.tr(
                "The Launcher sends back anonymous analytics to our own servers in a non identifying way in order to track what people do and don't use in the launcher. This helps determine what new features we implement in the future. All analytics are anonymous and contain no user/instance information in it at all. If you don't want to send anonymous analytics, you can disable this option."),
            enableAnalytics);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Logging");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Logging";
    }

    @Override
    protected void createViewModel() {
    }

    @Override
    protected void onDestroy() {
        removeAll();
    }
}
