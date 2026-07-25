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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.AddModRestriction;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.curseforge.CurseForgeCategoryForGame;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthCategory;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.data.modrinth.ModrinthSearchResult;
import com.atlauncher.exceptions.InvalidMinecraftVersion;
import com.atlauncher.gui.card.CurseForgeProjectCard;
import com.atlauncher.gui.card.ModrinthSearchHitCard;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3FilterChip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.gui.panels.NoCurseModsPanel;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.MinecraftManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.Utils;

import com.formdev.flatlaf.util.UIScale;

public final class AddModsDialog extends JDialog {
    /** Wide enough for a mod name, and no wider - the grid needs the rest. */
    private static final int SEARCH_COLUMNS = 18;

    private final ModManagement instanceOrServer;

    private boolean updating = false;

    /** Holds {@link #platformMessageLabel}, so the padding around it goes when the message does. */
    private JPanel platformMessagePanel;

    private final JPanel contentPanel = new JPanel(new WrapLayout());
    private final JPanel topPanel = new JPanel();
    private final JPanel warningPanel = new JPanel();
    private final MD3TextField searchField = MD3TextField.search(GetText.tr("Search"));
    private final JLabel platformMessageLabel = new JLabel();

    /**
     * Which platform is being browsed. A destination rather than a filter - the two hold different
     * mods and offer different sorts - so it is tabs, as it is on the pack browser. Hidden when
     * there is only one of them enabled, since a tab bar of one says nothing.
     */
    private final MD3Tabs hostTabs = new MD3Tabs();
    private final List<ModPlatform> hosts = new ArrayList<>();

    private final MD3FilterChip<String> sectionChip = new MD3FilterChip<>(GetText.tr("Type of Mod"), true,
            this::onSectionChanged);
    private final MD3FilterChip<String> sortChip = new MD3FilterChip<>(GetText.tr("Sort"), false, this::onFilterChanged);
    private final MD3FilterChip<String> categoryChip = new MD3FilterChip<>(GetText.tr("Category"), true,
            this::onFilterChanged);

    // #. {0} is the loader api (Fabric API/QSL)
    private final MD3Button installFabricApiButton = MD3Button.tonal(GetText.tr("Install {0}", "Fabric API"));

    // #. {0} is the loader (Fabric/Quilt), {1} is the loader api (Fabric API/QSL)
    private final JPanel fabricApiWarning = buildWarning(GetText.tr(
            "Before installing {0} mods, you should install {1} first!", "Fabric", "Fabric API"),
            installFabricApiButton);

    // #. {0} is the loader api (Fabric API/QSL)
    private final MD3Button installLegacyFabricApiButton = MD3Button
            .tonal(GetText.tr("Install {0}", "Legacy Fabric API"));

    // #. {0} is the loader (Fabric/Quilt), {1} is the loader api (Fabric API/QSL)
    private final JPanel legacyFabricApiWarning = buildWarning(GetText.tr(
            "Before installing {0} mods, you should install {1} first!", "Legacy Fabric", "Legacy Fabric API"),
            installLegacyFabricApiButton);

    private final MD3Button installQuiltStandardLibrariesButton = MD3Button.tonal(
            // #. {0} is the loader api (Fabric API/QSL)
            GetText.tr("Install {0}", "Quilt Standard Libraries"));

    // #. {0} is the loader (Fabric/Quilt), {1} is the loader api (Fabric API/QSL)
    private final JPanel quiltStandardLibrariesWarning = buildWarning(GetText.tr(
            "Before installing {0} mods, you should install {1} first!", "Quilt", "Quilt Standard Libraries"),
            installQuiltStandardLibrariesButton);

    // #. {0} is the loader api (Fabric API/QSL)
    private final MD3Button installForgifiedFabricApiButton = MD3Button
            .tonal(GetText.tr("Install {0}", "Forgified Fabric API"));

    // #. {0} is the loader (Fabric/Quilt), {1} is the loader api (Fabric API/QSL)
    private final JPanel forgifiedFabricApiWarning = buildWarning(GetText.tr(
            "Before installing {0} mods, you should install {1} first!", "Fabric", "Forgified Fabric API"),
            installForgifiedFabricApiButton);

    private JScrollPane jscrollPane;
    private MD3IconButton nextButton;
    private MD3IconButton prevButton;
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private int page = 0;

    public AddModsDialog(ModManagement instanceOrServer) {
        this(App.launcher.getParent(), instanceOrServer);
    }

    public AddModsDialog(Window parent, ModManagement instanceOrServer) {
        // #. {0} is the name of the mod we're installing
        super(parent, GetText.tr("Adding Mods For {0}", instanceOrServer.getName()), ModalityType.DOCUMENT_MODAL);
        this.instanceOrServer = instanceOrServer;

        this.setPreferredSize(UIScale.scale(new Dimension(800, 500)));
        this.setMinimumSize(UIScale.scale(new Dimension(800, 500)));
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        int selectedHostIndex = 0;
        String platformMessage = null;

        if (ConfigManager.getConfigItem("platforms.curseforge.modsEnabled", true)) {
            hosts.add(ModPlatform.CURSEFORGE);
            hostTabs.addTab("CurseForge");

            if (App.settings.defaultModPlatform == ModPlatform.CURSEFORGE) {
                selectedHostIndex = hosts.size() - 1;
                platformMessage = ConfigManager.getConfigItem("platforms.curseforge.message", null);
            }
        }

        if (ConfigManager.getConfigItem("platforms.modrinth.modsEnabled", true)) {
            hosts.add(ModPlatform.MODRINTH);
            hostTabs.addTab("Modrinth");

            if (App.settings.defaultModPlatform == ModPlatform.MODRINTH) {
                selectedHostIndex = hosts.size() - 1;
                platformMessage = ConfigManager.getConfigItem("platforms.modrinth.message", null);
            }
        }

        if (!hosts.isEmpty()) {
            hostTabs.setSelectedIndex(selectedHostIndex);
        }

        hostTabs.setVisible(hosts.size() > 1);

        searchField.setColumns(SEARCH_COLUMNS);
        searchField.setLeadingIcon(MD3Icons.SEARCH);
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.clearCallback", (Runnable) () -> {
            searchField.setText("");
            searchForMods();
        });

        setPlatformMessage(platformMessage);

        addSectionAndSortOptions(true);

        addCategories();

        setupComponents();

        this.loadDefaultMods();

        this.pack();
        this.setLocationRelativeTo(parent);
    }

    private void setupComponents() {
        Analytics.sendScreenView("Add Mods Dialog");

        getContentPane().setBackground(MD3Color.surface());

        this.topPanel.setLayout(new BoxLayout(this.topPanel, BoxLayout.Y_AXIS));
        this.topPanel.setOpaque(false);

        // a stack rather than a row: these used to be appended to one X_AXIS box, so a second
        // warning would have landed beside the first one's button
        this.warningPanel.setLayout(new BoxLayout(this.warningPanel, BoxLayout.Y_AXIS));
        this.warningPanel.setOpaque(false);
        this.warningPanel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S), 0));
        filters.setOpaque(false);
        filters.setBorder(MD3Spacing.border(0, MD3Spacing.M, 0, 0));
        filters.add(sectionChip.getChip());
        filters.add(sortChip.getChip());
        filters.add(categoryChip.getChip());

        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leading.setOpaque(false);
        leading.add(this.searchField);

        JPanel searchButtonsPanel = new JPanel(new BorderLayout());
        searchButtonsPanel.setOpaque(false);
        searchButtonsPanel.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        // the stack is aligned on its leading edge; a child left at the default centres itself in it
        searchButtonsPanel.setAlignmentX(LEFT_ALIGNMENT);
        searchButtonsPanel.add(leading, BorderLayout.WEST);
        searchButtonsPanel.add(filters, BorderLayout.CENTER);

        this.installFabricApiButton.addActionListener(e -> {
            ModPlatform selectedHost = selectedPlatform();
            boolean isCurseForge = selectedHost == ModPlatform.CURSEFORGE;
            if (isCurseForge) {
                final ProgressDialog<CurseForgeProject> curseForgeProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"),
                        "Aborting Getting Fabric API Information");

                curseForgeProjectLookupDialog.addThread(new Thread(() -> {
                    curseForgeProjectLookupDialog
                            .setReturnValue(CurseForgeApi.getProjectById(Constants.CURSEFORGE_FABRIC_MOD_ID));

                    curseForgeProjectLookupDialog.close();
                }));

                curseForgeProjectLookupDialog.start();

                CurseForgeProject mod = curseForgeProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog().setTitle(GetText.tr("Error Getting {0} Information", "Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Fabric API", "CurseForge"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Fabric API", "CurseForge", "mod"));
                CurseForgeProjectFileSelectorDialog curseForgeProjectFileSelectorDialog = new CurseForgeProjectFileSelectorDialog(
                        this, mod, instanceOrServer);
                curseForgeProjectFileSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge() && m.getCurseForgeModId() == Constants.CURSEFORGE_FABRIC_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_FABRIC_MOD_ID)))) {
                    fabricApiWarning.setVisible(false);
                }
            } else {
                final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"),
                        "Aborting Getting Fabric API Information");

                modrinthProjectLookupDialog.addThread(new Thread(() -> {
                    modrinthProjectLookupDialog
                            .setReturnValue(ModrinthApi.getProject(Constants.MODRINTH_FABRIC_MOD_ID));

                    modrinthProjectLookupDialog.close();
                }));

                modrinthProjectLookupDialog.start();

                ModrinthProject mod = modrinthProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog().setTitle(GetText.tr("Error Getting {0} Information", "Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Fabric API",
                                    "Modrinth"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Fabric API", "Modrinth", "mod"));
                ModrinthVersionSelectorDialog modrinthVersionSelectorDialog = new ModrinthVersionSelectorDialog(this,
                        mod, instanceOrServer);
                modrinthVersionSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge() && m.getCurseForgeModId() == Constants.CURSEFORGE_FABRIC_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_FABRIC_MOD_ID)))) {
                    fabricApiWarning.setVisible(false);
                }
            }

            if (searchField.getText().isEmpty()) {
                loadDefaultMods();
            } else {
                searchForMods();
            }
        });

        this.installLegacyFabricApiButton.addActionListener(e -> {
            boolean isCurseForge = selectedPlatform() == ModPlatform.CURSEFORGE;
            if (isCurseForge) {
                final ProgressDialog<CurseForgeProject> curseForgeProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Legacy Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Legacy Fabric API"),
                        "Aborting Getting Legacy Fabric API Information");

                curseForgeProjectLookupDialog.addThread(new Thread(() -> {
                    curseForgeProjectLookupDialog
                            .setReturnValue(CurseForgeApi.getProjectById(Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID));

                    curseForgeProjectLookupDialog.close();
                }));

                curseForgeProjectLookupDialog.start();

                CurseForgeProject mod = curseForgeProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog().setTitle(GetText.tr("Error Getting {0} Information", "Legacy Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Legacy Fabric API",
                                    "CurseForge"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Legacy Fabric API", "CurseForge", "mod"));
                CurseForgeProjectFileSelectorDialog curseForgeProjectFileSelectorDialog = new CurseForgeProjectFileSelectorDialog(
                        this, mod, instanceOrServer);
                curseForgeProjectFileSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge()
                                && m.getCurseForgeModId() == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id
                                                .equalsIgnoreCase(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID)))) {
                    legacyFabricApiWarning.setVisible(false);
                }
            } else {
                final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Legacy Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Legacy Fabric API"),
                        "Aborting Getting Legacy Fabric API Information");

                modrinthProjectLookupDialog.addThread(new Thread(() -> {
                    modrinthProjectLookupDialog
                            .setReturnValue(ModrinthApi.getProject(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID));

                    modrinthProjectLookupDialog.close();
                }));

                modrinthProjectLookupDialog.start();

                ModrinthProject mod = modrinthProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog().setTitle(GetText.tr("Error Getting {0} Information", "Legacy Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Legacy Fabric API",
                                    "Modrinth"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Legacy Fabric API", "Modrinth", "mod"));
                ModrinthVersionSelectorDialog modrinthVersionSelectorDialog = new ModrinthVersionSelectorDialog(this,
                        mod, instanceOrServer);
                modrinthVersionSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge()
                                && m.getCurseForgeModId() == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id
                                                .equalsIgnoreCase(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID)))) {
                    legacyFabricApiWarning.setVisible(false);
                }
            }

            if (searchField.getText().isEmpty()) {
                loadDefaultMods();
            } else {
                searchForMods();
            }
        });

        this.installQuiltStandardLibrariesButton.addActionListener(e -> {
            final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    GetText.tr("Getting {0} Information", "Quilt Standard Libaries"), 0,
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    GetText.tr("Getting {0} Information", "Quilt Standard Libaries"),
                    "Aborting Getting Quilt Standard Libaries Information");

            modrinthProjectLookupDialog.addThread(new Thread(() -> {
                modrinthProjectLookupDialog
                        .setReturnValue(ModrinthApi.getProject(Constants.MODRINTH_QSL_MOD_ID));

                modrinthProjectLookupDialog.close();
            }));

            modrinthProjectLookupDialog.start();

            ModrinthProject mod = modrinthProjectLookupDialog.getReturnValue();

            if (mod == null) {
                DialogManager.okDialog()
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        .setTitle(GetText.tr("Error Getting {0} Information", "Quilt Standard Libaries"))
                        // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                        .setContent(new HTMLBuilder().center().text(GetText.tr(
                                "There was an error getting {0} information from {1}. Please try again later.",
                                "Quilt Standard Libaries", "Modrinth"))
                                .build())
                        .setType(DialogManager.ERROR).show();
                return;
            }

            Analytics.trackEvent(AnalyticsEvent.forAddMod("Quilt Standard Libraries", "Modrinth", "mod"));
            ModrinthVersionSelectorDialog modrinthVersionSelectorDialog = new ModrinthVersionSelectorDialog(this, mod,
                    instanceOrServer);
            modrinthVersionSelectorDialog.setVisible(true);

            if (instanceOrServer.getMods().stream().anyMatch(
                    m -> m.isFromModrinth()
                            && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_QSL_MOD_ID))) {
                quiltStandardLibrariesWarning.setVisible(false);
            }
        });

        this.installForgifiedFabricApiButton.addActionListener(e -> {
            boolean isCurseForge = selectedPlatform() == ModPlatform.CURSEFORGE;
            if (isCurseForge) {
                final ProgressDialog<CurseForgeProject> curseForgeProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Forgified Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Forgified Fabric API"),
                        "Aborting Getting Forgified Fabric API Information");

                curseForgeProjectLookupDialog.addThread(new Thread(() -> {
                    curseForgeProjectLookupDialog
                            .setReturnValue(
                                    CurseForgeApi.getProjectById(Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID));

                    curseForgeProjectLookupDialog.close();
                }));

                curseForgeProjectLookupDialog.start();

                CurseForgeProject mod = curseForgeProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog()
                            .setTitle(GetText.tr("Error Getting {0} Information", "Forgified Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Forgified Fabric API", "CurseForge"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Forgified Fabric API", "CurseForge", "mod"));
                CurseForgeProjectFileSelectorDialog curseForgeProjectFileSelectorDialog = new CurseForgeProjectFileSelectorDialog(
                        this, mod, instanceOrServer);
                curseForgeProjectFileSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge()
                                && m.getCurseForgeModId() == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id
                                                .equalsIgnoreCase(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID)))) {
                    forgifiedFabricApiWarning.setVisible(false);
                }
            } else {
                final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"), 0,
                        // #. {0} is the loader api were getting info from (Fabric/Quilt)
                        GetText.tr("Getting {0} Information", "Fabric API"),
                        "Aborting Getting Fabric API Information");

                modrinthProjectLookupDialog.addThread(new Thread(() -> {
                    modrinthProjectLookupDialog
                            .setReturnValue(ModrinthApi.getProject(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID));

                    modrinthProjectLookupDialog.close();
                }));

                modrinthProjectLookupDialog.start();

                ModrinthProject mod = modrinthProjectLookupDialog.getReturnValue();

                if (mod == null) {
                    // #. {0} is the loader api were getting info from (Fabric/Quilt)
                    DialogManager.okDialog()
                            .setTitle(GetText.tr("Error Getting {0} Information", "Forgified Fabric API"))
                            // #. {0} is the loader (Fabric/Quilt) {1} is the platform (CurseForge/Modrinth)
                            .setContent(new HTMLBuilder().center().text(GetText.tr(
                                    "There was an error getting {0} information from {1}. Please try again later.",
                                    "Forgified Fabric API", "Modrinth"))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return;
                }

                Analytics.trackEvent(AnalyticsEvent.forAddMod("Forgified Fabric API", "Modrinth", "mod"));
                ModrinthVersionSelectorDialog modrinthVersionSelectorDialog = new ModrinthVersionSelectorDialog(this,
                        mod, instanceOrServer);
                modrinthVersionSelectorDialog.setVisible(true);

                if (instanceOrServer.getMods().stream().anyMatch(
                        m -> (m.isFromCurseForge()
                                && m.getCurseForgeModId() == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID)
                                || (m.isFromModrinth()
                                        && m.modrinthProject.id
                                                .equalsIgnoreCase(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID)))) {
                    forgifiedFabricApiWarning.setVisible(false);
                }
            }

            if (searchField.getText().isEmpty()) {
                loadDefaultMods();
            } else {
                searchForMods();
            }
        });

        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        if (loaderVersion != null && loaderVersion.isFabric() && instanceOrServer.getMods().stream()
                .noneMatch(m -> (m.isFromCurseForge() && m.getCurseForgeModId() == Constants.CURSEFORGE_FABRIC_MOD_ID)
                        || (m.isFromModrinth()
                                && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_FABRIC_MOD_ID)))) {
            this.warningPanel.add(fabricApiWarning);
        }

        if (loaderVersion != null && loaderVersion.isLegacyFabric() && instanceOrServer.getMods().stream()
                .noneMatch(m -> (m.isFromCurseForge()
                        && m.getCurseForgeModId() == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID)
                        || (m.isFromModrinth()
                                && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID)))) {
            this.warningPanel.add(legacyFabricApiWarning);
        }

        if (loaderVersion != null && loaderVersion.isQuilt() && instanceOrServer.getMods().stream()
                .noneMatch(m -> m.isFromModrinth()
                        && m.modrinthProject.id.equalsIgnoreCase(Constants.MODRINTH_QSL_MOD_ID))) {
            this.warningPanel.add(quiltStandardLibrariesWarning);
        }

        // If on Forge/NeoForge and has Sinytra Connector installed, then show Forgified
        // Fabric API things
        if (instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector() && instanceOrServer.getMods().stream()
                .noneMatch(m -> (m.isFromCurseForge()
                        && m.getCurseForgeModId() == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID)
                        || (m.isFromModrinth()
                                && m.modrinthProject.id
                                        .equalsIgnoreCase(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID)))) {
            this.warningPanel.add(forgifiedFabricApiWarning);
        }

        this.topPanel.add(searchButtonsPanel);
        this.topPanel.add(buildPlatformMessage());
        this.topPanel.add(warningPanel);

        this.jscrollPane = new JScrollPane(this.contentPanel) {
            {
                this.getVerticalScrollBar().setUnitIncrement(16);
            }
        };

        this.jscrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.jscrollPane.setBorder(null);
        this.jscrollPane.setOpaque(false);
        this.jscrollPane.getViewport().setOpaque(false);

        this.contentPanel.setOpaque(false);
        this.mainPanel.setOpaque(false);

        mainPanel.add(this.topPanel, BorderLayout.NORTH);
        mainPanel.add(this.jscrollPane, BorderLayout.CENTER);

        // named, not just drawn: MD3IconButton takes the tooltip as its accessible name, and an
        // icon-only button without one is a button a screen reader can only call "button"
        prevButton = new MD3IconButton(MD3Icons.CHEVRON_LEFT, GetText.tr("Previous page"));
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> goToPreviousPage());

        nextButton = new MD3IconButton(MD3Icons.CHEVRON_RIGHT, GetText.tr("Next page"));
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> goToNextPage());

        JPanel bottomButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, MD3Spacing.scale(MD3Spacing.S), 0));
        bottomButtonsPanel.setOpaque(false);
        bottomButtonsPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));
        bottomButtonsPanel.add(prevButton);
        bottomButtonsPanel.add(nextButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottomPanel.add(bottomButtonsPanel, BorderLayout.CENTER);

        this.add(hostTabs, BorderLayout.NORTH);
        this.add(mainPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        this.hostTabs.addChangeListener(e -> {
            updating = true;
            page = 0;
            ModPlatform selectedModPlatform = selectedPlatform();

            boolean isCurseForge = selectedModPlatform == ModPlatform.CURSEFORGE;
            boolean isModrinth = selectedModPlatform == ModPlatform.MODRINTH;

            addSectionAndSortOptions(false);

            String platformMessage = null;

            if (isCurseForge) {
                platformMessage = ConfigManager.getConfigItem("platforms.curseforge.message", null);
            } else if (isModrinth) {
                platformMessage = ConfigManager.getConfigItem("platforms.modrinth.message", null);
            }

            addCategories();

            setPlatformMessage(platformMessage);

            reload();
            updating = false;
        });

        this.searchField.addActionListener(e -> searchForMods());
    }

    /**
     * A row of advice with the button that acts on it - the loader APIs a mod is likely to need
     * before any of the mods here will run.
     *
     * <p>
     * These were labels of hand written HTML holding a hex colour read out of the theme at class
     * load. The text is now laid out as text, and the colours come from the scheme.
     */
    private static JPanel buildWarning(String message, MD3Button action) {
        JLabel label = new JLabel(message,
                MD3Icon.of(MD3Icons.WARNING, MD3Spacing.ICON_SIZE).withRole(MD3Color.TERTIARY),
                SwingConstants.LEADING);
        label.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, message));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        label.setForeground(MD3Color.onSurface());
        label.setIconTextGap(MD3Spacing.scale(MD3Spacing.S));

        JPanel banner = new JPanel(new BorderLayout(MD3Spacing.scale(MD3Spacing.L), 0));
        banner.setOpaque(true);
        banner.setBackground(MD3Color.get(MD3Color.SURFACE_CONTAINER_HIGH));
        banner.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));
        banner.setAlignmentX(LEFT_ALIGNMENT);
        banner.add(label, BorderLayout.CENTER);
        banner.add(action, BorderLayout.EAST);

        return banner;
    }

    /**
     * Whatever the launcher has been told to say about the platform being browsed. Above the grid
     * rather than under it, where it used to sit - a message about what is being shown is of no use
     * once the user has scrolled past everything.
     */
    private JPanel buildPlatformMessage() {
        platformMessageLabel.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        platformMessageLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        platformMessageLabel.setForeground(MD3Color.onSurfaceVariant());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(MD3Spacing.border(0, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(platformMessageLabel, BorderLayout.CENTER);

        panel.setVisible(platformMessageLabel.isVisible());
        platformMessagePanel = panel;

        return panel;
    }

    private void setPlatformMessage(String message) {
        if (message != null) {
            platformMessageLabel.setText(new HTMLBuilder().text(message).build());
        }

        platformMessageLabel.setVisible(message != null);

        if (platformMessagePanel != null) {
            platformMessagePanel.setVisible(message != null);
        }
    }

    /**
     * @return the platform being browsed, or null when the config has disabled both of them
     */
    private ModPlatform selectedPlatform() {
        if (hosts.isEmpty()) {
            return null;
        }

        return hosts.get(Math.max(0, Math.min(hostTabs.getSelectedIndex(), hosts.size() - 1)));
    }

    /**
     * The section decides which categories there are, so it reloads those as well.
     */
    private void onSectionChanged() {
        if (updating) {
            return;
        }

        page = 0;

        addCategories();
        reload();
    }

    private void onFilterChanged() {
        if (updating) {
            return;
        }

        page = 0;

        reload();
    }

    private void reload() {
        if (searchField.getText().isEmpty()) {
            loadDefaultMods();
        } else {
            searchForMods();
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            contentPanel.removeAll();
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new LoadingPanel(), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private void goToPreviousPage() {
        if (page > 0) {
            page -= 1;
        }

        ModPlatform selectedModPlatform = selectedPlatform();
        Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_mods", searchField.getText(), page + 1,
                        selectedModPlatform.toString()));

        getMods();
    }

    private void goToNextPage() {
        if (contentPanel.getComponentCount() != 0) {
            page += 1;
        }

        ModPlatform selectedModPlatform = selectedPlatform();
        Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_mods", searchField.getText(), page + 1,
                        selectedModPlatform.toString()));

        getMods();
    }

    @SuppressWarnings("unchecked")
    private void getMods() {
        setLoading(true);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        String query = searchField.getText();
        ModPlatform selectedModPlatform = selectedPlatform();
        String sectionValue = Optional.ofNullable(sectionChip.getValue()).orElse("Mods");
        String sortValue = Optional.ofNullable(sortChip.getValue())
                .orElse(selectedModPlatform == ModPlatform.CURSEFORGE ? "Popularity" : "relevance");

        new Thread(() -> {
            if (selectedModPlatform == ModPlatform.CURSEFORGE) {
                String versionToSearchFor = App.settings.addModRestriction == AddModRestriction.STRICT
                        ? instanceOrServer.getMinecraftVersion()
                        : null;

                if (sectionValue.equals("Data Packs")) {
                    setCurseForgeMods(CurseForgeApi.searchDataPacks(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Resource Packs")) {
                    setCurseForgeMods(CurseForgeApi.searchResourcePacks(query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Shaders")) {
                    setCurseForgeMods(CurseForgeApi.searchShaderPacks(query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Worlds")) {
                    setCurseForgeMods(CurseForgeApi.searchWorlds(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Plugins")) {
                    setCurseForgeMods(CurseForgeApi.searchPlugins(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else {
                    if (instanceOrServer.getLoaderVersion().isFabric()
                            || instanceOrServer.getLoaderVersion().isLegacyFabric()) {
                        setCurseForgeMods(CurseForgeApi.searchModsForFabric(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isQuilt()) {
                        setCurseForgeMods(CurseForgeApi.searchModsForQuilt(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
                        if (instanceOrServer.getLoaderVersion().isForge()) {
                            setCurseForgeMods(CurseForgeApi.searchModsForForgeOrFabric(versionToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        } else {
                            setCurseForgeMods(CurseForgeApi.searchModsForNeoForgeOrFabric(versionToSearchFor, query,
                                    page,
                                    sortValue,
                                    categoryChip.getValue()));
                        }
                    } else if (instanceOrServer.getLoaderVersion().isForge()) {
                        setCurseForgeMods(CurseForgeApi.searchModsForForge(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isNeoForge()) {
                        setCurseForgeMods(CurseForgeApi.searchModsForNeoForge(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else {
                        setCurseForgeMods(CurseForgeApi.searchMods(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    }
                }
            } else if (selectedModPlatform == ModPlatform.MODRINTH) {
                List<String> versionsToSearchFor = new ArrayList<>();

                if (App.settings.addModRestriction == AddModRestriction.STRICT) {
                    versionsToSearchFor.add(instanceOrServer.getMinecraftVersion());
                } else if (App.settings.addModRestriction == AddModRestriction.LAX) {
                    try {
                        versionsToSearchFor.addAll(MinecraftManager
                                .getMajorMinecraftVersions(instanceOrServer.getMinecraftVersion()).stream()
                                .map(mv -> mv.id).collect(Collectors.toList()));
                    } catch (InvalidMinecraftVersion e) {
                        LogManager.logStackTrace(e);
                        versionsToSearchFor = null;
                    }
                } else if (App.settings.addModRestriction == AddModRestriction.NONE) {
                    versionsToSearchFor = null;
                }

                if (sectionValue.equals("Data Packs")) {
                    setModrinthMods(ModrinthApi.searchDataPacks(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Resource Packs")) {
                    setModrinthMods(ModrinthApi.searchResourcePacks(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Shaders")) {
                    setModrinthMods(ModrinthApi.searchShaders(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Plugins")) {
                    if (instanceOrServer.getLoaderVersion().isPaper()) {
                        setModrinthMods(ModrinthApi.searchPluginsForPaper(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isPurpur()) {
                        setModrinthMods(ModrinthApi.searchPluginsForPurpur(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    }
                } else {
                    if (instanceOrServer.getLoaderVersion().isFabric()) {
                        setModrinthMods(ModrinthApi.searchModsForFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isLegacyFabric()) {
                        setModrinthMods(ModrinthApi.searchModsForLegacyFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isQuilt()) {
                        setModrinthMods(ModrinthApi.searchModsForQuiltOrFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
                        if (instanceOrServer.getLoaderVersion().isForge()) {
                            setModrinthMods(ModrinthApi.searchModsForForgeOrFabric(versionsToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        } else {
                            setModrinthMods(ModrinthApi.searchModsForNeoForgeOrFabric(versionsToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        }
                    } else if (instanceOrServer.getLoaderVersion().isForge()) {
                        setModrinthMods(ModrinthApi.searchModsForForge(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (instanceOrServer.getLoaderVersion().isNeoForge()) {
                        setModrinthMods(ModrinthApi.searchModsForNeoForge(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    }
                }
            }

            setLoading(false);
        }).start();
    }

    private void loadDefaultMods() {
        getMods();
    }

    private void searchForMods() {
        String query = searchField.getText();

        page = 0;

        ModPlatform selectedModPlatform = selectedPlatform();
        Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_mods", query, page + 1,
                        selectedModPlatform.toString()));

        getMods();
    }

    private void setCurseForgeMods(List<CurseForgeProject> mods) {
        contentPanel.removeAll();

        if (mods == null || mods.isEmpty()) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
        } else {
            prevButton.setEnabled(page > 0);
            nextButton.setEnabled(mods.size() == Constants.CURSEFORGE_PAGINATION_SIZE);

            contentPanel.setLayout(new WrapLayout());

            mods.forEach(mod -> {
                CurseForgeProject castMod = mod;
                String sectionValue = Optional.ofNullable(sectionChip.getValue()).orElse("Mods");

                contentPanel.add(new CurseForgeProjectCard(castMod, instanceOrServer, e -> {
                    if (sectionValue.equals("Plugins")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddPlugin(castMod));
                    } else if (sectionValue.equals("Data Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddDataPack(castMod));
                    } else if (sectionValue.equals("Resource Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddResourcePack(castMod));
                    } else if (sectionValue.equals("Shaders")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddShaders(castMod));
                    } else {
                        Analytics.trackEvent(AnalyticsEvent.forAddMod(castMod));
                    }

                    CurseForgeProjectFileSelectorDialog curseForgeProjectFileSelectorDialog = new CurseForgeProjectFileSelectorDialog(
                            this, castMod, instanceOrServer);
                    curseForgeProjectFileSelectorDialog.setVisible(true);
                }, e -> {
                    if (sectionValue.equals("Plugins")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemovePlugin(castMod));
                    } else if (sectionValue.equals("Data Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveDataPack(castMod));
                    } else if (sectionValue.equals("Resource Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveResourcePack(castMod));
                    } else if (sectionValue.equals("Shaders")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveShaders(castMod));
                    } else {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveMod(castMod));
                    }

                    Optional<DisableableMod> foundMod = instanceOrServer.getMods().stream()
                            .filter(dm -> dm.isFromCurseForge() && dm.curseForgeProjectId == castMod.id)
                            .findFirst();

                    if (foundMod.isPresent()) {
                        instanceOrServer.removeMod(foundMod.get());

                        if (castMod.id == Constants.CURSEFORGE_FABRIC_MOD_ID) {
                            fabricApiWarning.setVisible(true);
                        }

                        if (castMod.id == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID) {
                            legacyFabricApiWarning.setVisible(true);
                        }

                        if (castMod.id == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID) {
                            forgifiedFabricApiWarning.setVisible(true);
                        }
                    }
                }));

            });
        }

        SwingUtilities.invokeLater(() -> jscrollPane.getVerticalScrollBar().setValue(0));

        revalidate();
        repaint();
    }

    private void setModrinthMods(ModrinthSearchResult searchResult) {
        contentPanel.removeAll();

        if (searchResult == null || searchResult.hits.isEmpty()) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
        } else {
            prevButton.setEnabled(page > 0);
            nextButton.setEnabled((searchResult.offset + searchResult.limit) < searchResult.totalHits);

            contentPanel.setLayout(new WrapLayout());

            searchResult.hits.forEach(mod -> {
                ModrinthSearchHit castMod = mod;
                String sectionValue = Optional.ofNullable(sectionChip.getValue()).orElse("Mods");

                contentPanel.add(new ModrinthSearchHitCard(castMod, instanceOrServer, e -> {
                    final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                            GetText.tr("Getting Mod Information"), 0, GetText.tr("Getting Mod Information"),
                            "Aborting Getting Mod Information");

                    modrinthProjectLookupDialog.addThread(new Thread(() -> {
                        modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getProject(castMod.projectId));

                        modrinthProjectLookupDialog.close();
                    }));

                    modrinthProjectLookupDialog.start();

                    ModrinthProject modrinthMod = modrinthProjectLookupDialog.getReturnValue();

                    if (modrinthMod == null) {
                        DialogManager.okDialog().setTitle(GetText.tr("Error Getting Mod Information"))
                                .setContent(new HTMLBuilder().center().text(GetText.tr(
                                        "There was an error getting mod information from Modrinth. Please try again later."))
                                        .build())
                                .setType(DialogManager.ERROR).show();
                        return;
                    }

                    if (sectionValue.equals("Plugins")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddPlugin(castMod));
                    } else if (sectionValue.equals("Data Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddDataPack(castMod));
                    } else if (sectionValue.equals("Resource Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddResourcePack(castMod));
                    } else if (sectionValue.equals("Shaders")) {
                        Analytics.trackEvent(AnalyticsEvent.forAddShaders(castMod));
                    } else {
                        Analytics.trackEvent(AnalyticsEvent.forAddMod(castMod));
                    }

                    ModrinthVersionSelectorDialog modrinthVersionSelectorDialog = sectionValue.equals("Data Packs")
                            ? new ModrinthVersionSelectorDialog(this, modrinthMod, instanceOrServer,
                                    com.atlauncher.data.Type.datapack)
                            : new ModrinthVersionSelectorDialog(this, modrinthMod, instanceOrServer);
                    modrinthVersionSelectorDialog.setVisible(true);
                }, e -> {
                    if (sectionValue.equals("Plugins")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemovePlugin(castMod));
                    } else if (sectionValue.equals("Data Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveDataPack(castMod));
                    } else if (sectionValue.equals("Resource Packs")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveResourcePack(castMod));
                    } else if (sectionValue.equals("Shaders")) {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveShaders(castMod));
                    } else {
                        Analytics.trackEvent(AnalyticsEvent.forRemoveMod(castMod));
                    }

                    Optional<DisableableMod> foundMod = instanceOrServer.getMods().stream()
                            .filter(dm -> dm.isFromModrinth() && dm.modrinthProject.id.equals(castMod.projectId))
                            .findFirst();

                    if (foundMod.isPresent()) {
                        instanceOrServer.removeMod(foundMod.get());

                        if (castMod.projectId.equals(Constants.MODRINTH_FABRIC_MOD_ID)) {
                            fabricApiWarning.setVisible(true);
                        }

                        if (castMod.projectId.equals(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID)) {
                            legacyFabricApiWarning.setVisible(true);
                        }

                        if (castMod.projectId.equals(Constants.MODRINTH_QSL_MOD_ID)) {
                            quiltStandardLibrariesWarning.setVisible(true);
                        }

                        if (castMod.projectId.equals(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID)) {
                            forgifiedFabricApiWarning.setVisible(true);
                        }
                    }
                }));

            });
        }

        SwingUtilities.invokeLater(() -> jscrollPane.getVerticalScrollBar().setValue(0));

        revalidate();
        repaint();
    }

    /**
     * Rebuilds what can be browsed and how it can be sorted, both of which depend on the platform.
     *
     * <p>
     * The section the user was on is kept if the new platform also has it, which used to be four
     * near-identical blocks of "if this one is selected, re-select it at its new index" - a chip
     * picks by value, so the whole dance is one call.
     */
    private void addSectionAndSortOptions(boolean firstTime) {
        String previousSection = firstTime ? null : sectionChip.getValue();

        List<ComboItem<String>> sections = new ArrayList<>();
        List<ComboItem<String>> sorts = new ArrayList<>();

        if (instanceOrServer.supportsPlugins()) {
            sections.add(new ComboItem<>("Plugins", GetText.tr("Plugins")));
        }
        if (instanceOrServer.getLoaderVersion() != null && !instanceOrServer.getLoaderVersion().isPaper()
                && !instanceOrServer.getLoaderVersion().isPurpur()) {
            sections.add(new ComboItem<>("Mods", GetText.tr("Mods")));
        }
        if (instanceOrServer instanceof Instance) {
            sections.add(new ComboItem<>("Data Packs", GetText.tr("Data Packs")));
            sections.add(new ComboItem<>("Resource Packs", GetText.tr("Resource Packs")));
            sections.add(new ComboItem<>("Shaders", GetText.tr("Shaders")));
        }

        boolean isCurseForgeSelected = (firstTime
                && (instanceOrServer.getLoaderVersion() == null || (!instanceOrServer.getLoaderVersion().isPaper()
                        && !instanceOrServer.getLoaderVersion().isPurpur())))
                                ? App.settings.defaultModPlatform == ModPlatform.CURSEFORGE
                                : selectedPlatform() == ModPlatform.CURSEFORGE;

        if (isCurseForgeSelected) {
            if (instanceOrServer instanceof Instance) {
                sections.add(new ComboItem<>("Worlds", GetText.tr("Worlds")));
            }

            sorts.add(new ComboItem<>("Popularity", GetText.tr("Popularity")));
            sorts.add(new ComboItem<>("Last Updated", GetText.tr("Last Updated")));
            sorts.add(new ComboItem<>("Total Downloads", GetText.tr("Total Downloads")));
        } else {
            sorts.add(new ComboItem<>("relevance", GetText.tr("Relevance")));
            sorts.add(new ComboItem<>("newest", GetText.tr("Newest")));
            sorts.add(new ComboItem<>("updated", GetText.tr("Last Updated")));
            sorts.add(new ComboItem<>("downloads", GetText.tr("Total Downloads")));
        }

        sectionChip.setOptions(sections);
        sortChip.setOptions(sorts);

        if (previousSection != null) {
            sectionChip.selectValue(previousSection);
        }
    }

    private void addCategories() {
        updating = true;

        List<ComboItem<String>> options = new ArrayList<>();
        options.add(new ComboItem<>(null, GetText.tr("All Categories")));

        String section = sectionChip.getValue();

        if (selectedPlatform() == ModPlatform.CURSEFORGE) {
            List<CurseForgeCategoryForGame> categories = new ArrayList<>();

            if ("Data Packs".equals(section)) {
                categories.addAll(CurseForgeApi.getCategoriesForDataPacks());
            } else if ("Resource Packs".equals(section)) {
                categories.addAll(CurseForgeApi.getCategoriesForResourcePacks());
            } else if ("Shaders".equals(section)) {
                categories.addAll(CurseForgeApi.getCategoriesForShaderPacks());
            } else if ("Worlds".equals(section)) {
                categories.addAll(CurseForgeApi.getCategoriesForWorlds());
            } else if ("Plugins".equals(section)) {
                categories.addAll(CurseForgeApi.getCategoriesForPlugins());
            } else {
                categories.addAll(CurseForgeApi.getCategoriesForMods());
            }

            categories.forEach(c -> options.add(new ComboItem<>(String.valueOf(c.id), c.name)));
        } else {
            List<ModrinthCategory> categories = new ArrayList<>();

            if ("Data Packs".equals(section)) {
                categories.addAll(ModrinthApi.getCategoriesForDataPacks());
            } else if ("Resource Packs".equals(section)) {
                categories.addAll(ModrinthApi.getCategoriesForResourcePacks());
            } else if ("Shaders".equals(section)) {
                categories.addAll(ModrinthApi.getCategoriesForShaders());
            } else if ("Plugins".equals(section)) {
                categories.addAll(ModrinthApi.getCategoriesForPlugins());
            } else {
                categories.addAll(ModrinthApi.getCategoriesForMods());
            }

            categories.forEach(c -> options.add(new ComboItem<>(c.name, Utils.capitalize(c.name))));
        }

        categoryChip.setOptions(options);
        updating = false;
    }
}
