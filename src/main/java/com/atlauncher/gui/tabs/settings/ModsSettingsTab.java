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

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.AddModRestriction;
import com.atlauncher.data.InstanceExportFormat;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.viewmodel.impl.settings.ModsSettingsViewModel;

public class ModsSettingsTab extends AbstractSettingsTab {
    private final ModsSettingsViewModel viewModel;

    public ModsSettingsTab(ModsSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected void onShow() {
        // Default mod platform

        MD3ComboBox<ComboItem<ModPlatform>> defaultModPlatform = new MD3ComboBox<>();
        defaultModPlatform.addItem(new ComboItem<>(ModPlatform.CURSEFORGE, "CurseForge"));
        defaultModPlatform.addItem(new ComboItem<>(ModPlatform.MODRINTH, "Modrinth"));

        defaultModPlatform.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                @SuppressWarnings("unchecked")
                ComboItem<ModPlatform> item = (ComboItem<ModPlatform>) itemEvent.getItem();

                viewModel.setDefaultModPlatform(item.getValue());
            }
        });
        addDisposable(viewModel.getDefaultModPlatform().subscribe(defaultModPlatform::setSelectedIndex));

        addRow(GetText.tr("Default Mod Platform"), GetText.tr(
                "The default mod platform to use when adding mods to instances, as well as the platform to use when updating/reinstalling mods on multiple platforms."),
            defaultModPlatform);

        // Default export format

        MD3ComboBox<ComboItem<InstanceExportFormat>> defaultExportFormat = new MD3ComboBox<>();
        defaultExportFormat.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE, "CurseForge"));
        defaultExportFormat.addItem(new ComboItem<>(InstanceExportFormat.MODRINTH, "Modrinth"));
        defaultExportFormat
                .addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE_AND_MODRINTH, "CurseForge & Modrinth"));
        defaultExportFormat.addItem(new ComboItem<>(InstanceExportFormat.MULTIMC, "MultiMC"));

        defaultExportFormat.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                @SuppressWarnings("unchecked")
                ComboItem<InstanceExportFormat> item = (ComboItem<InstanceExportFormat>) itemEvent.getItem();

                viewModel.setDefaultExportFormat(item.getValue());
            }
        });
        addDisposable(viewModel.getDefaultExportFormat().subscribe(defaultExportFormat::setSelectedIndex));

        addRow(GetText.tr("Default Export Format"),
            GetText.tr("The default format to export instances to. Can also be changed at time of export."),
            defaultExportFormat);

        MD3Switch skipExportHashVerification = new MD3Switch();
        skipExportHashVerification.addItemListener(itemEvent -> viewModel
            .setSkipExportHashVerification(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getSkipExportHashVerification().subscribe(skipExportHashVerification::setSelected));

        addRow(GetText.tr("Skip Hash Verification When Exporting?"),
            GetText.tr(
                "When exporting, do not fingerprint files against CurseForge or Modrinth. Only the project and file metadata already on the instance is used. Can also be changed at time of export."),
            skipExportHashVerification);

        // Add Mod Restrictions

        MD3ComboBox<ComboItem<AddModRestriction>> addModRestriction = new MD3ComboBox<>();
        addModRestriction.addItem(
                new ComboItem<>(AddModRestriction.STRICT, GetText.tr("Only show mods for current Minecraft version")));
        addModRestriction.addItem(new ComboItem<>(AddModRestriction.LAX,
                GetText.tr("Show mods for the current major Minecraft version (eg: 1.16.x)")));
        addModRestriction
                .addItem(new ComboItem<>(AddModRestriction.NONE, GetText.tr("Show mods for all Minecraft versions")));

        addModRestriction.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                @SuppressWarnings("unchecked")
                ComboItem<AddModRestriction> item = (ComboItem<AddModRestriction>) itemEvent.getItem();

                viewModel.setAddModRestrictions(item.getValue());
            }
        });
        addDisposable(viewModel.getAddModRestriction().subscribe(addModRestriction::setSelectedIndex));

        addRow(GetText.tr("Add Mod Restrictions"),
            GetText.tr("What restrictions should be in place when adding mods from a mod platform."),
            addModRestriction);

        // Enable added mods by default

        MD3Switch enableAddedModsByDefault = new MD3Switch();
        enableAddedModsByDefault.addItemListener(
                itemEvent -> viewModel.setEnableAddedModsByDefault(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getEnableAddedModsByDefault().subscribe(enableAddedModsByDefault::setSelected));

        addRow(GetText.tr("Enable Added Mods By Default?"),
            GetText.tr("When adding mods manually, should they be enabled automatically?"),
            enableAddedModsByDefault);

        // Show Fabric Mods When Sinytra Installed

        MD3Switch showFabricModsWhenSinytraInstalled = new MD3Switch();
        showFabricModsWhenSinytraInstalled.addItemListener(itemEvent -> viewModel
                .setShowFabricModsWhenSinytraInstalled(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getShowFabricModsWhenSinytraInstalled()
                .subscribe(showFabricModsWhenSinytraInstalled::setSelected));

        addRow(GetText.tr("Show Fabric Mods When Sinytra Installed?"),
            GetText.tr("When Sinytra Connector is installed, should Fabric mods be shown?"),
            showFabricModsWhenSinytraInstalled);

        // Allow CurseForge Alpha/Beta CurseForge files

        MD3Switch allowCurseForgeAlphaBetaFiles = new MD3Switch();
        allowCurseForgeAlphaBetaFiles.setSelected(App.settings.allowCurseForgeAlphaBetaFiles);
        allowCurseForgeAlphaBetaFiles.addItemListener(itemEvent -> viewModel
                .setAllowCurseForgeAlphaBetaFiles(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(
                viewModel.getAllowCurseForgeAlphaBetaFiles().subscribe(allowCurseForgeAlphaBetaFiles::setSelected));

        addRow(GetText.tr("Allow CurseForge Alpha/Beta Files?"), GetText.tr(
                "This will enable using Alpha/Beta files from CurseForge by default when installing modpacks as well as updating to Alpha/Beta versions from stable release versions."),
            allowCurseForgeAlphaBetaFiles);

        // Dont check mods on CurseForge

        MD3Switch dontCheckModsOnCurseForge = new MD3Switch();
        dontCheckModsOnCurseForge.addItemListener(
                itemEvent -> viewModel.setDoNotCheckModsOnCurseForge(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getDoNotCheckModsOnCurseForge().subscribe(dontCheckModsOnCurseForge::setSelected));

        addRow(
            // #. {0} is the platform (e.g. CurseForge/Modrinth)
            GetText.tr("Don't Check Mods On {0}?", "CurseForge"),
            // #. {0} is the platform (e.g. CurseForge/Modrinth)
            GetText.tr(
                "When installing packs or adding mods manually to instances, we check for the file on {0} to show more information about the mod as well as make updating easier. Disabling this will mean you won't be able to update manually added mods from within the launcher.",
                "CurseForge"),
            dontCheckModsOnCurseForge);

        // Dont check mods on Modrinth

        MD3Switch dontCheckModsOnModrinth = new MD3Switch();
        dontCheckModsOnModrinth.addItemListener(
                itemEvent -> viewModel.setDoNotCheckModsOnModrinth(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getDoNotCheckModsOnModrinth().subscribe(dontCheckModsOnModrinth::setSelected));

        addRow(
            // #. {0} is the platform (e.g. CurseForge/Modrinth)
            GetText.tr("Don't Check Mods On {0}?", "Modrinth"),
            // #. {0} is the platform (e.g. CurseForge/Modrinth)
            GetText.tr(
                "When installing packs or adding mods manually to instances, we check for the file on {0} to show more information about the mod as well as make updating easier. Disabling this will mean you won't be able to update manually added mods from within the launcher.",
                "Modrinth"),
            dontCheckModsOnModrinth);

        // Enable scanning mods on launch

        MD3Switch scanModsOnLaunch = new MD3Switch();
        scanModsOnLaunch.setSelected(App.settings.scanModsOnLaunch);
        scanModsOnLaunch.addItemListener(e -> viewModel.setScanModsOnLaunch(e.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getScanModsOnLaunch().subscribe(scanModsOnLaunch::setSelected));

        addRow(GetText.tr("Scan Mods On Launch?"), GetText.tr(
                "This will scan the mods in instances before launching to ensure they do not contain malware or have been modified since installing."),
            scanModsOnLaunch);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Mods");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Mods";
    }

    @Override
    protected void createViewModel() {}

    @Override
    protected void onDestroy() {
        removeAll();
    }
}
