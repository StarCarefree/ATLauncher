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

import java.awt.event.ItemEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.CheckState;
import com.atlauncher.data.LauncherTheme;
import com.atlauncher.gui.components.JLabelWithHover;
import com.atlauncher.listener.DelayedSavingKeyListener;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;
import com.atlauncher.utils.sort.InstanceSortingStrategies;
import com.atlauncher.viewmodel.impl.settings.GeneralSettingsViewModel;

public class GeneralSettingsTab extends AbstractSettingsTab {
    private final GeneralSettingsViewModel viewModel;
    private JLabelWithHover customDownloadsPathChecker;

    public GeneralSettingsTab(GeneralSettingsViewModel generalSettingsViewModel) {
        this.viewModel = generalSettingsViewModel;
    }

    @Override
    protected void onShow() {
        addSection(GetText.tr("Appearance"));

        // Language

        JComboBox<String> language = new JComboBox<>(viewModel.getLanguages());
        language.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                viewModel.setSelectedLanguage((String) itemEvent.getItem());
        });
        addDisposable(viewModel.getSelectedLanguage().subscribe(language::setSelectedItem));

        JButton translateButton = new JButton(GetText.tr("Help Translate"));
        translateButton.addActionListener(e -> OS.openWebBrowser(Constants.CROWDIN_URL));

        addRow(GetText.tr("Language"), GetText.tr("This specifies the language used by the Launcher."),
            group(language, translateButton));

        // Theme

        JComboBox<ComboItem<String>> theme = new JComboBox<>();

        for (LauncherTheme launcherTheme : viewModel.getThemes()) {
            theme.addItem(new ComboItem<>(launcherTheme.id, launcherTheme.label));
        }
        addDisposable(viewModel.getSelectedTheme().subscribe(theme::setSelectedIndex));

        theme.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                viewModel.setSelectedTheme(((ComboItem<String>) itemEvent.getItem()).getValue());
            }
        });

        addRow(GetText.tr("Theme"), GetText.tr("This sets the theme that the launcher will use."), theme);

        // Date Format

        JComboBox<ComboItem<String>> dateFormat = new JComboBox<>();

        Date exampleDate = viewModel.getDate();

        for (String format : viewModel.getDateFormats()) {
            dateFormat.addItem(new ComboItem<>(format, new SimpleDateFormat(format).format(exampleDate)));
        }

        addDisposable(viewModel.getDateFormat().subscribe(dateFormat::setSelectedIndex));

        dateFormat.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                viewModel.setDateFormat(((ComboItem<String>) itemEvent.getItem()).getValue());
        });

        addRow(GetText.tr("Date Format"),
            GetText.tr("This controls the format that dates are displayed in the launcher."), dateFormat);

        // Instance Title Format

        JComboBox<ComboItem<String>> instanceTitleFormat = new JComboBox<>();

        for (String format : viewModel.getInstanceTitleFormats()) {
            instanceTitleFormat.addItem(new ComboItem<>(format, String.format(format, GetText.tr("Instance Name"),
                    GetText.tr("Pack Name"), GetText.tr("Pack Version"), GetText.tr("Minecraft Version"))));
        }

        addDisposable(viewModel.getInstanceFormat().subscribe(instanceTitleFormat::setSelectedIndex));

        instanceTitleFormat.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                viewModel.setInstanceTitleFormat(((ComboItem<String>) itemEvent.getItem()).getValue());
        });

        addRow(GetText.tr("Instance Title Format"),
            GetText.tr("This controls the format that instances titles are shown as."), instanceTitleFormat);

        // Disable custom fonts

        JCheckBox disableCustomFonts = new JCheckBox();
        disableCustomFonts.addItemListener(itemEvent -> {
            viewModel.setDisableCustomFonts(itemEvent.getStateChange() == ItemEvent.SELECTED);
        });
        addDisposable(viewModel.getDisableCustomFonts().subscribe(disableCustomFonts::setSelected));

        addRow(GetText.tr("Disable Custom Fonts?"), GetText.tr(
                "This will disable custom fonts used by themes. If your system has issues with font display not looking right, you can disable this to switch to a default compatible font."),
            disableCustomFonts);

        // Reduce animations

        JCheckBox reduceAnimations = new JCheckBox();
        reduceAnimations.addItemListener(itemEvent -> {
            viewModel.setReduceAnimations(itemEvent.getStateChange() == ItemEvent.SELECTED);
        });
        addDisposable(viewModel.getReduceAnimations().subscribe(reduceAnimations::setSelected));

        addRow(GetText.tr("Reduce Animations?"), GetText.tr(
                "This turns off the launcher's animations, so views change instantly rather than sliding or fading. Turn this on if motion makes you uncomfortable, or if the launcher feels sluggish."),
            reduceAnimations);

        addSection(GetText.tr("Startup"));

        // Selected tab on startup

        JComboBox<ComboItem<Integer>> selectedTabOnStartup = new JComboBox<>();
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_NEWS_TAB, GetText.tr("News")));
        selectedTabOnStartup
                .addItem(new ComboItem<>(UIConstants.LAUNCHER_CREATE_PACK_TAB, GetText.tr("Create Pack")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_PACKS_TAB, GetText.tr("Packs")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_INSTANCES_TAB, GetText.tr("Instances")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_SERVERS_TAB, GetText.tr("Servers")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_ACCOUNTS_TAB, GetText.tr("Accounts")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_TOOLS_TAB, GetText.tr("Tools")));
        selectedTabOnStartup.addItem(new ComboItem<>(UIConstants.LAUNCHER_SETTINGS_TAB, GetText.tr("Settings")));

        addDisposable(viewModel.getSelectedTabOnStartup().subscribe(selectedTabOnStartup::setSelectedIndex));

        selectedTabOnStartup.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                viewModel.setSelectedTabOnStartup(((ComboItem<Integer>) itemEvent.getItem()).getValue());
        });

        addRow(GetText.tr("Default Tab"),
            GetText.tr("Which tab to have selected by default when opening the launcher."), selectedTabOnStartup);

        // Default instance sorting

        JComboBox<InstanceSortingStrategies> defaultInstanceSorting = new JComboBox<>(
                InstanceSortingStrategies.values());
        addDisposable(viewModel.getInstanceSortingObservable().subscribe(defaultInstanceSorting::setSelectedIndex));
        defaultInstanceSorting.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                viewModel.setInstanceSorting((InstanceSortingStrategies) itemEvent.getItem());
        });

        addRow(GetText.tr("Default Instance Sort"),
            GetText.tr("Default sorting of instances under the Instances tab."), defaultInstanceSorting);

        // Enable Console

        JCheckBox enableConsole = new JCheckBox();
        enableConsole.addItemListener(e -> viewModel.setEnableConsole(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getEnableConsole().subscribe(enableConsole::setSelected));

        addRow(GetText.tr("Enable Console"),
            GetText.tr("If you want the console to be visible when opening the Launcher."), enableConsole);

        // Enable Tray Icon

        JCheckBox enableTrayIcon = new JCheckBox();
        enableTrayIcon.addItemListener(e -> viewModel.setEnableTrayMenuOpen(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getEnableTrayMenu().subscribe(enableTrayIcon::setSelected));

        addRow(GetText.tr("Enable Tray Menu"), GetText.tr(
                "The Tray Menu is a little icon that shows in your system taskbar which allows you to perform different functions to do various things with the launcher such as hiding or showing the console, killing Minecraft or closing ATLauncher."),
            enableTrayIcon);

        // Remember gui sizes and positions

        JCheckBox rememberWindowSizePosition = new JCheckBox();
        rememberWindowSizePosition
                .addItemListener(e -> viewModel.setRememberWindowStuff(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getRememberWindowSizePosition().subscribe(rememberWindowSizePosition::setSelected));

        addRow(GetText.tr("Remember Window Size & Positions?"), GetText.tr(
                "This will remember the windows positions and size so they keep the same size and position when you restart the launcher."),
            rememberWindowSizePosition);

        addSection(GetText.tr("Launching"));

        // Keep Launcher Open

        JCheckBox keepLauncherOpen = new JCheckBox();
        keepLauncherOpen.addItemListener(e -> viewModel.setKeepLauncherOpen(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getKeepLauncherOpen().subscribe(keepLauncherOpen::setSelected));

        addRow(GetText.tr("Keep Launcher Open"),
            GetText.tr("This determines if ATLauncher should stay open or exit after Minecraft has exited"),
            keepLauncherOpen);

        // Enable Feral Gamemode

        if (viewModel.showFeralGameMode()) {
            boolean gameModeExistsInPath = viewModel.hasFeralGameMode();

            JCheckBox enableFeralGamemode = new JCheckBox();
            enableFeralGamemode
                    .addItemListener(e -> viewModel.setEnableFeralGameMode(e.getStateChange() == ItemEvent.SELECTED));
            addDisposable(viewModel.getEnableFeralGameMode().subscribe(enableFeralGamemode::setSelected));

            String help = GetText.tr("This will enable Feral Gamemode for packs launched.");

            if (!gameModeExistsInPath) {
                help = GetText.tr(
                        "This will enable Feral Gamemode for packs launched (disabled because gamemoderun not found in PATH, please install Feral Gamemode or add it to your PATH).");

                enableFeralGamemode.setEnabled(false);
                enableFeralGamemode.setSelected(false);
            }

            addRow(GetText.tr("Enable Feral Gamemode"), help, enableFeralGamemode);
        }

        if (viewModel.showArmSupport()) {
            // Enable ARM Support

            JCheckBox enableArmSupport = new JCheckBox();

            // this listened on, and was bound to, the recycle bin checkbox above - so the ARM
            // setting could not be changed and toggling the recycle bin changed it instead
            enableArmSupport
                    .addItemListener(e -> viewModel.setEnableArmSupport(e.getStateChange() == ItemEvent.SELECTED));
            addDisposable(viewModel.getEnableArmSupport().subscribe(enableArmSupport::setSelected));

            addRow(GetText.tr("Enable ARM Support?"), GetText.tr(
                    "Support for ARM devices is still experimental. If you experience issues on an ARM based device, please turn this off."),
                enableArmSupport);
        }

        addSection(GetText.tr("Files"));

        // Custom Downloads Path

        JTextField customDownloadsPath = new JTextField(16);
        customDownloadsPathChecker = new JLabelWithHover("", null, null);
        addDisposable(viewModel.getCustomsDownloadPath().subscribe(customDownloadsPath::setText));
        customDownloadsPath.addKeyListener(
                new DelayedSavingKeyListener(
                        500,
                        () -> viewModel.setCustomsDownloadPath(customDownloadsPath.getText()),
                        viewModel::setCustomsDownloadPathPending));
        addDisposable(viewModel.getCustomDownloadsPathChecker().subscribe(this::setCustomDownloadsPathCheckState));

        JButton customDownloadsPathResetButton = new JButton(GetText.tr("Reset"));

        customDownloadsPathResetButton.addActionListener(e -> {
            viewModel.resetCustomDownloadPath();
            resetCustomDownloadsPathCheckLabel();
        });

        JButton customDownloadsPathBrowseButton = new JButton(GetText.tr("Browse"));
        customDownloadsPathBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(customDownloadsPath.getText()));
            chooser.setDialogTitle(GetText.tr("Select"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedPath = chooser.getSelectedFile();
                customDownloadsPath.setText(selectedPath.getAbsolutePath());
                viewModel.setCustomsDownloadPath(selectedPath.getAbsolutePath());
                viewModel.setCustomsDownloadPathPending();
            }
        });

        addWideRow(GetText.tr("Downloads Folder"), GetText.tr(
                "This setting allows you to change the Downloads folder that the launcher looks in when downloading browser mods."),
            group(customDownloadsPath, customDownloadsPathChecker, customDownloadsPathResetButton,
                customDownloadsPathBrowseButton));

        // Use native file picker

        if (viewModel.getShowNativeFilePickerOption()) {
            JCheckBox useNativeFilePicker = new JCheckBox();
            useNativeFilePicker
                    .addItemListener(e -> viewModel.setUseNativeFilePicker(e.getStateChange() == ItemEvent.SELECTED));
            addDisposable(viewModel.getUseNativeFilePicker().subscribe(useNativeFilePicker::setSelected));

            addRow(GetText.tr("Use Native File Picker?"),
                GetText.tr("This will use your operating systems native file picker when selecting files."),
                useNativeFilePicker);
        }

        // Use recycle bin

        JCheckBox useRecycleBin = new JCheckBox();
        useRecycleBin.addItemListener(e -> viewModel.setUseRecycleBin(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getUseRecycleBin().subscribe(useRecycleBin::setSelected));

        addRow(GetText.tr("Use Recycle Bin/Trash?"), GetText.tr(
                "This will use your operating systems recycle bin/trash where possible when deleting files/instances/servers instead of just deleting them entirely, allowing you to recover files if you make a mistake or want to get them back."),
            useRecycleBin);
    }

    @Override
    public String getTitle() {
        return GetText.tr("General");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "General";
    }

    private void showCustomDownloadsPathWarning() {
        DialogManager.okDialog()
                .setTitle(GetText.tr("Help"))
                .setContent(
                        new HTMLBuilder()
                                .center()
                                .text(
                                        GetText.tr(
                                                "The Downloads Folder Path you set is incorrect.<br/><br/>Please verify it points to a folder and try again."))
                                .build())
                .setType(DialogManager.ERROR)
                .show();
    }

    private void setLabelState(String tooltip, String path) {
        try {
            customDownloadsPathChecker.setToolTipText(tooltip);
            ImageIcon icon = Utils.getIconImage(path);
            if (icon != null) {
                customDownloadsPathChecker.setIcon(icon);
                icon.setImageObserver(customDownloadsPathChecker);
            }
        } catch (NullPointerException ignored) {
            // ignored
        }
    }

    private void resetCustomDownloadsPathCheckLabel() {
        customDownloadsPathChecker.setText("");
        customDownloadsPathChecker.setIcon(null);
        customDownloadsPathChecker.setToolTipText(null);
    }

    private void setCustomDownloadsPathCheckState(CheckState state) {
        if (state == CheckState.NotChecking) {
            resetCustomDownloadsPathCheckLabel();
        } else if (state == CheckState.CheckPending) {
            setLabelState(GetText.tr("Downloads folder path change pending"),
                    "/assets/icon/warning.png");
        } else if (state == CheckState.Checking) {
            setLabelState(GetText.tr("Checking downloads folder path"),
                    "/assets/image/loading-bars-small.gif");
        } else if (state instanceof CheckState.Checked) {
            if (((CheckState.Checked) state).valid) {
                resetCustomDownloadsPathCheckLabel();
            } else {
                setLabelState(GetText.tr("Invalid!"), "/assets/icon/error.png");
                showCustomDownloadsPathWarning();
            }
        }
    }

    @Override
    protected void onDestroy() {
        removeAll();
        customDownloadsPathChecker = null;
    }

    @Override
    protected void createViewModel() {
    }
}
