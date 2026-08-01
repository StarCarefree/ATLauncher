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
import java.awt.LayoutManager;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3FilterChip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
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

    /**
     * How long typing has to stop for before a search is sent.
     *
     * <p>
     * Longer than the instances list's 250ms because this one goes over the network: at 250ms a
     * user typing "sodium" would send five requests and read the answer to the last of them.
     */
    private static final int SETTLE_MS = 400;

    private final ModManagement instanceOrServer;

    private boolean updating = false;

    /**
     * Which search the grid is showing. Every request carries the value it was started with and
     * drops itself if the counter has moved on - without it a slow response to an earlier query
     * lands on top of a later one, which is what a chip changed twice in quick succession does.
     */
    private final AtomicLong searchGeneration = new AtomicLong();

    private Timer settle;

    /** Holds {@link #platformMessageLabel}, so the padding around it goes when the message does. */
    private JPanel platformMessagePanel;

    private final JPanel contentPanel = new JPanel(
            new CardGridLayout(MD3PackCard.CARD_WIDTH, MD3PackCard.MAX_CARD_WIDTH, MD3Spacing.L));
    private final JPanel topPanel = new JPanel();
    private final JPanel warningPanel = new JPanel();
    private final MD3TextField searchField = MD3TextField.search(GetText.tr("Search"));
    private final JLabel platformMessageLabel = new JLabel();
    private final JLabel pageLabel = new JLabel();

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

        searchField.setName("addModsSearchField");
        searchField.setColumns(SEARCH_COLUMNS);
        searchField.setLeadingIcon(MD3Icons.SEARCH);
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.clearCallback", (Runnable) () -> {
            searchField.setText("");
            searchForMods();
        });

        setPlatformMessage(platformMessage);

        addSectionAndSortOptions(true);

        setupComponents();

        // the categories used to be fetched here, on the event thread, so the dialog did not appear
        // until two API calls had come back - and there was no way to open it offline at all, which
        // is why it is the one dialog in the launcher with no render test
        loadCategories();

        this.loadDefaultMods();

        this.pack();
        this.setLocationRelativeTo(parent);
    }

    /**
     * A pending settle would otherwise fire into a dialog that has been closed.
     */
    @Override
    public void dispose() {
        if (settle != null) {
            settle.stop();
        }

        super.dispose();
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

        // there was no way to tell where in a result set you were, or how much of one there was -
        // just two chevrons, the second of which CurseForge could only guess the enabled state of
        pageLabel.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        pageLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        pageLabel.setForeground(MD3Color.onSurfaceVariant());

        JPanel bottomButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, MD3Spacing.scale(MD3Spacing.S), 0));
        bottomButtonsPanel.setOpaque(false);
        bottomButtonsPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));
        bottomButtonsPanel.add(prevButton);
        bottomButtonsPanel.add(MD3TopAppBar.centred(pageLabel));
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

            loadCategories();

            setPlatformMessage(platformMessage);

            reload();
            updating = false;
        });

        // searches as you type rather than only on Enter, on a settle long enough that a word
        // typed at speed is one request rather than one per letter
        settle = new Timer(SETTLE_MS, e -> searchForMods());
        settle.setRepeats(false);

        this.searchField.getDocument().addDocumentListener(new DocumentListener() {
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

        this.searchField.addActionListener(e -> {
            settle.stop();
            searchForMods();
        });
    }

    private static LayoutManager grid() {
        return new CardGridLayout(MD3PackCard.CARD_WIDTH, MD3PackCard.MAX_CARD_WIDTH, MD3Spacing.L);
    }

    /**
     * @param shown what is on this page
     * @param total how many results there are, 0 for none and -1 where the platform does not say
     */
    private void setPageLabel(int shown, int total) {
        if (shown == 0) {
            pageLabel.setText("");

            return;
        }

        if (total > 0) {
            // #. {0} is the page number, {1} is the total number of search results
            pageLabel.setText(GetText.tr("Page {0} · {1} results", page + 1, total));
        } else {
            // #. {0} is the page number
            pageLabel.setText(GetText.tr("Page {0}", page + 1));
        }
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

        loadCategories();
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

        final long generation = searchGeneration.incrementAndGet();

        new Thread(() -> {
            if (generation != searchGeneration.get()) {
                return;
            }

            if (selectedModPlatform == ModPlatform.CURSEFORGE) {
                String versionToSearchFor = App.settings.addModRestriction == AddModRestriction.STRICT
                        ? instanceOrServer.getMinecraftVersion()
                        : null;

                if (sectionValue.equals("Data Packs")) {
                    setCurseForgeMods(generation, CurseForgeApi.searchDataPacks(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Resource Packs")) {
                    setCurseForgeMods(generation, CurseForgeApi.searchResourcePacks(query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Shaders")) {
                    setCurseForgeMods(generation, CurseForgeApi.searchShaderPacks(query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Worlds")) {
                    setCurseForgeMods(generation, CurseForgeApi.searchWorlds(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Plugins")) {
                    setCurseForgeMods(generation, CurseForgeApi.searchPlugins(versionToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else {
                    // read once and null checked: an instance with no loader at all reaches here if
                    // the section chip is left at its default, and every branch below used to
                    // dereference this without asking
                    LoaderVersion loader = instanceOrServer.getLoaderVersion();

                    if (loader != null && (loader.isFabric() || loader.isLegacyFabric())) {
                        setCurseForgeMods(generation, CurseForgeApi.searchModsForFabric(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isQuilt()) {
                        setCurseForgeMods(generation, CurseForgeApi.searchModsForQuilt(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
                        if (loader.isForge()) {
                            setCurseForgeMods(generation, CurseForgeApi.searchModsForForgeOrFabric(versionToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        } else {
                            setCurseForgeMods(generation, CurseForgeApi.searchModsForNeoForgeOrFabric(versionToSearchFor, query,
                                    page,
                                    sortValue,
                                    categoryChip.getValue()));
                        }
                    } else if (loader != null && loader.isForge()) {
                        setCurseForgeMods(generation, CurseForgeApi.searchModsForForge(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isNeoForge()) {
                        setCurseForgeMods(generation, CurseForgeApi.searchModsForNeoForge(versionToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else {
                        setCurseForgeMods(generation, CurseForgeApi.searchMods(versionToSearchFor, query, page,
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
                    setModrinthMods(generation, ModrinthApi.searchDataPacks(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Resource Packs")) {
                    setModrinthMods(generation, ModrinthApi.searchResourcePacks(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Shaders")) {
                    setModrinthMods(generation, ModrinthApi.searchShaders(versionsToSearchFor, query, page,
                            sortValue,
                            categoryChip.getValue()));
                } else if (sectionValue.equals("Plugins")) {
                    LoaderVersion loader = instanceOrServer.getLoaderVersion();

                    if (loader != null && loader.isPaper()) {
                        setModrinthMods(generation, ModrinthApi.searchPluginsForPaper(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isPurpur()) {
                        setModrinthMods(generation, ModrinthApi.searchPluginsForPurpur(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else {
                        setModrinthMods(generation, null);
                    }
                } else {
                    LoaderVersion loader = instanceOrServer.getLoaderVersion();

                    if (loader != null && loader.isFabric()) {
                        setModrinthMods(generation, ModrinthApi.searchModsForFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isLegacyFabric()) {
                        setModrinthMods(generation, ModrinthApi.searchModsForLegacyFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isQuilt()) {
                        setModrinthMods(generation, ModrinthApi.searchModsForQuiltOrFabric(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
                        if (loader.isForge()) {
                            setModrinthMods(generation, ModrinthApi.searchModsForForgeOrFabric(versionsToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        } else {
                            setModrinthMods(generation, ModrinthApi.searchModsForNeoForgeOrFabric(versionsToSearchFor, query, page,
                                    sortValue,
                                    categoryChip.getValue()));
                        }
                    } else if (loader != null && loader.isForge()) {
                        setModrinthMods(generation, ModrinthApi.searchModsForForge(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else if (loader != null && loader.isNeoForge()) {
                        setModrinthMods(generation, ModrinthApi.searchModsForNeoForge(versionsToSearchFor, query, page,
                                sortValue,
                                categoryChip.getValue()));
                    } else {
                        // no loader, or one Modrinth has no mods for - the grid says so rather than
                        // being left on the loading indicator for ever, which is what used to
                        // happen when none of these branches matched
                        setModrinthMods(generation, null);
                    }
                }
            }

            SwingUtilities.invokeLater(() -> setLoading(false));
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

    /**
     * @param generation which search this is the answer to; a stale one is dropped rather than
     *                   painted over a newer result
     */
    private void setCurseForgeMods(long generation, List<CurseForgeProject> mods) {
        // the whole grid used to be rebuilt from whichever thread the search ran on
        SwingUtilities.invokeLater(() -> applyCurseForgeMods(generation, mods));
    }

    private void applyCurseForgeMods(long generation, List<CurseForgeProject> mods) {
        if (generation != searchGeneration.get()) {
            return;
        }

        contentPanel.removeAll();

        if (mods == null || mods.isEmpty()) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
            setPageLabel(0, 0);
        } else {
            prevButton.setEnabled(page > 0);
            nextButton.setEnabled(mods.size() == Constants.CURSEFORGE_PAGINATION_SIZE);
            // CurseForge does return a total, in a pagination block that was being deserialized and
            // read nowhere; until that is threaded through, a full page means there is another one
            setPageLabel(mods.size(), -1);

            contentPanel.setLayout(grid());

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

    private void setModrinthMods(long generation, ModrinthSearchResult searchResult) {
        SwingUtilities.invokeLater(() -> applyModrinthMods(generation, searchResult));
    }

    private void applyModrinthMods(long generation, ModrinthSearchResult searchResult) {
        if (generation != searchGeneration.get()) {
            return;
        }

        contentPanel.removeAll();

        if (searchResult == null || searchResult.hits.isEmpty()) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
            setPageLabel(0, 0);
        } else {
            prevButton.setEnabled(page > 0);
            nextButton.setEnabled((searchResult.offset + searchResult.limit) < searchResult.totalHits);
            setPageLabel(searchResult.hits.size(), searchResult.totalHits);

            contentPanel.setLayout(grid());

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

    /**
     * Fetches the categories for the platform and section being browsed, off the event thread.
     *
     * <p>
     * {@link MD3FilterChip#setOptions} deliberately does not fire its change callback, so filling
     * the chip in later does not reload a grid that is already loading.
     */
    private void loadCategories() {
        final long generation = searchGeneration.get();

        new SwingWorker<List<ComboItem<String>>, Void>() {
            @Override
            protected List<ComboItem<String>> doInBackground() {
                return fetchCategories();
            }

            @Override
            protected void done() {
                if (isCancelled() || generation != searchGeneration.get()) {
                    return;
                }

                try {
                    updating = true;
                    categoryChip.setOptions(get());
                } catch (Exception e) {
                    // the chip stays on "All Categories", which is a working filter rather than an
                    // error - the grid below it is unaffected
                    LogManager.logStackTrace("Failed to fetch mod categories", e);
                } finally {
                    updating = false;
                }
            }
        }.execute();
    }

    private List<ComboItem<String>> fetchCategories() {
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

        return options;
    }
}
