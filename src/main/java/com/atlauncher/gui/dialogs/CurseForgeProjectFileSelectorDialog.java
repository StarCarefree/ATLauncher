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
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.AddModRestriction;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeFileDependency;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.gui.card.CurseForgeFileDependencyCard;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.ModCompatibility;
import com.atlauncher.utils.ModDependencyResolver;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Pair;
import com.formdev.flatlaf.util.UIScale;

public class CurseForgeProjectFileSelectorDialog extends JDialog {
    private int filesLength = 0;
    private final CurseForgeProject mod;
    private final ModManagement instanceOrServer;
    private Integer installedFileId = null;
    private boolean selectNewest = true;

    private final JPanel dependenciesPanel = new JPanel(new FlowLayout());

    /** What the panel is showing, so "Install All Required" acts on the same list the user sees. */
    private List<CurseForgeFileDependency> lastDependenciesNeeded;

    private MD3Button installAllDependencies;
    private MD3ListContainer dependenciesContainer;
    /** The whole dependency block - heading, list and button - shown only when there is one. */
    private JPanel dependenciesSection;
    private MD3Button addButton;
    private MD3Button viewModButton;
    private MD3Button viewFileButton;
    private JLabel versionsLabel;
    private JLabel installedJLabel;
    private MD3ComboBox<CurseForgeFile> filesDropdown;
    private final List<CurseForgeFile> files = new ArrayList<>();

    public CurseForgeProjectFileSelectorDialog(CurseForgeProject mod, ModManagement instanceOrServer) {
        this(App.launcher.getParent(), mod, instanceOrServer);
    }

    public CurseForgeProjectFileSelectorDialog(Window parent, CurseForgeProject mod, ModManagement instanceOrServer) {
        super(parent, ModalityType.DOCUMENT_MODAL);

        this.mod = mod;
        this.instanceOrServer = instanceOrServer;

        setupComponents();
    }

    public CurseForgeProjectFileSelectorDialog(Window parent, CurseForgeProject mod, ModManagement instanceOrServer,
        int installedFileId) {
        super(parent, ModalityType.DOCUMENT_MODAL);

        this.mod = mod;
        this.instanceOrServer = instanceOrServer;
        this.installedFileId = installedFileId;

        setupComponents();
    }

    public CurseForgeProjectFileSelectorDialog(Window parent, CurseForgeProject mod, ModManagement instanceOrServer,
        int installedFileId, boolean selectNewest) {
        super(parent, ModalityType.DOCUMENT_MODAL);

        this.mod = mod;
        this.instanceOrServer = instanceOrServer;
        this.installedFileId = installedFileId;
        this.selectNewest = selectNewest;

        setupComponents();
    }

    private void setupComponents() {
        // #. {0} is the name of the mod we're installing
        setTitle(GetText.tr("Installing {0}", mod.name));

        setSize(UIScale.scale(550), UIScale.scale(200));
        setMinimumSize(UIScale.scale(new Dimension(550, 200)));
        setLocationRelativeTo(App.launcher.getParent());
        setLayout(new BorderLayout());
        setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        addButton = MD3Button.filled(GetText.tr("Add"));
        addButton.setEnabled(false);

        viewModButton = MD3Button.outlined(GetText.tr("View Mod"));
        viewModButton.setEnabled(false);

        viewFileButton = MD3Button.outlined(GetText.tr("View File"));
        viewFileButton.setEnabled(false);

        getContentPane().setBackground(MD3Color.surface());

        dependenciesPanel.setOpaque(false);

        // the whole block appears and disappears as one: there is nothing to say about
        // dependencies until a file that has some is picked
        dependenciesSection = new JPanel(new BorderLayout(0, MD3Spacing.scale(MD3Spacing.S)));
        dependenciesSection.setOpaque(false);
        dependenciesSection.setVisible(false);

        // Top Panel Stuff
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        // #. {0} is the name of the mod we're installing
        String headlineText = GetText.tr("Installing {0}", mod.name);
        JLabel headline = new JLabel(headlineText);
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE, headlineText));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());
        top.add(headline, BorderLayout.NORTH);

        installedJLabel = new JLabel("");
        installedJLabel.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        installedJLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        installedJLabel.setForeground(MD3Color.onSurfaceVariant());
        top.add(installedJLabel, BorderLayout.SOUTH);

        // Middle Panel Stuff
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.setBorder(MD3Spacing.border(0, MD3Spacing.L, 0, MD3Spacing.L));

        JPanel filesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S), 0));
        filesPanel.setOpaque(false);
        filesPanel.setBorder(MD3Spacing.border(MD3Spacing.S, 0, MD3Spacing.M, 0));

        versionsLabel = new JLabel(GetText.tr("Version To Install") + ": ");
        versionsLabel.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        versionsLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        versionsLabel.setForeground(MD3Color.onSurface());
        filesPanel.add(versionsLabel);

        filesDropdown = new MD3ComboBox<>();
        CurseForgeFile loadingProject = new CurseForgeFile();
        loadingProject.displayName = GetText.tr("Loading");
        filesDropdown.addItem(loadingProject);
        filesDropdown.setEnabled(false);
        filesPanel.add(filesDropdown);

        // one click for the lot, and it follows the chain - a dependency's own dependencies were
        // never mentioned, since this panel is built from this mod's list and nothing else
        installAllDependencies = MD3Button.outlined(GetText.tr("Install All Required"));
        installAllDependencies.setVisible(false);
        installAllDependencies.addActionListener(e -> installAllDependencies());

        String dependenciesTitle = GetText.tr("The below mods need to be installed");
        JLabel dependenciesLabel = new JLabel(dependenciesTitle);
        dependenciesLabel.setFont(MD3Type.font(MD3Type.TITLE_SMALL, dependenciesTitle));
        dependenciesLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        dependenciesLabel.setForeground(MD3Color.primary());

        JPanel dependencyHeader = new JPanel(new BorderLayout());
        dependencyHeader.setOpaque(false);
        dependencyHeader.add(dependenciesLabel, BorderLayout.WEST);
        dependencyHeader.add(installAllDependencies, BorderLayout.EAST);

        dependenciesContainer = MD3ListContainer.wrapping(dependenciesPanel);
        dependenciesContainer.setPreferredSize(UIScale.scale(new Dimension(550, 250)));

        dependenciesSection.add(dependencyHeader, BorderLayout.NORTH);
        dependenciesSection.add(dependenciesContainer, BorderLayout.CENTER);

        middle.add(filesPanel, BorderLayout.NORTH);
        middle.add(dependenciesSection, BorderLayout.SOUTH);

        this.getFiles();

        // Bottom Panel Stuff
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        buttons.setOpaque(false);
        buttons.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        addButton.addActionListener(e -> {
            CurseForgeFile file = (CurseForgeFile) filesDropdown.getSelectedItem();

            ProgressDialog<Void> progressDialog = new ProgressDialog<>(
                // #. {0} is the name of the mod we're installing
                GetText.tr("Installing {0}", file.displayName), false, this);
            progressDialog.addThread(new Thread(() -> {
                Analytics.trackEvent(mod.getAnalyticsEventForAdded(file));
                instanceOrServer.addFileFromCurseForge(mod, file, progressDialog);

                progressDialog.close();
            }));
            progressDialog.start();
            dispose();
        });

        viewModButton.addActionListener(e -> OS.openWebBrowser(mod.getWebsiteUrl()));

        viewFileButton.addActionListener(e -> {
            CurseForgeFile file = (CurseForgeFile) filesDropdown.getSelectedItem();

            OS.openWebBrowser(String.format(Locale.ENGLISH, "%s/files/%d", mod.getWebsiteUrl(), file.id));
        });

        filesDropdown.addActionListener(e -> reloadDependenciesPanel());

        MD3Button cancel = MD3Button.text(GetText.tr("Cancel"));
        cancel.addActionListener(e -> dispose());

        // confirm goes rightmost: cancel first, Add last
        buttons.add(cancel);
        buttons.add(viewFileButton);
        buttons.add(viewModButton);
        buttons.add(addButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void reloadDependenciesPanel() {
        if (filesDropdown.getSelectedItem() == null) {
            return;
        }

        CurseForgeFile selectedFile = (CurseForgeFile) filesDropdown.getSelectedItem();

        dependenciesPanel.setVisible(false);

        // this file has dependencies
        if (!selectedFile.dependencies.isEmpty()) {
            // check to see which required ones we don't already have
            List<CurseForgeFileDependency> dependencies = selectedFile.dependencies.stream()
                .filter(dependency -> dependency.isRequired())
                .filter(dependency -> {
                    if (dependency.modId != Constants.CURSEFORGE_FABRIC_MOD_ID) {
                        return true;
                    }

                    // We shouldn't install Fabric API when using Sinytra Connector
                    return !instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector();
                })
                .filter(dependency -> instanceOrServer.getMods().stream()
                    .noneMatch(installedMod -> {
                        if (instanceOrServer.getLoaderVersion() != null && instanceOrServer.getLoaderVersion().isQuilt()
                            && dependency.modId == Constants.CURSEFORGE_FABRIC_MOD_ID) {
                            // if on Quilt and the dependency is Fabric API, then don't show it if user
                            // already has QSL installed
                            return instanceOrServer.getMods().parallelStream().anyMatch(m -> m.isFromModrinth()
                                && m.modrinthProject.id.equals(Constants.MODRINTH_QSL_MOD_ID));
                        }

                        // don't show CurseForge dependency when grabbed from Modrinth
                        if (dependency.modId == Constants.CURSEFORGE_FABRIC_MOD_ID
                            && installedMod.isFromModrinth()
                            && installedMod.modrinthProject.id.equals(Constants.MODRINTH_FABRIC_MOD_ID)) {
                            return true;
                        }

                        // don't show CurseForge dependency when grabbed from Modrinth
                        if (dependency.modId == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID
                            && installedMod.isFromModrinth()
                            && installedMod.modrinthProject.id
                            .equals(Constants.MODRINTH_LEGACY_FABRIC_MOD_ID)) {
                            return true;
                        }

                        // don't show CurseForge dependency when grabbed from Modrinth
                        if (dependency.modId == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID
                            && installedMod.isFromModrinth()
                            && installedMod.modrinthProject.id
                            .equals(Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID)) {
                            return true;
                        }

                        return installedMod.isFromCurseForge()
                            && installedMod.getCurseForgeModId() == dependency.modId;
                    }))
                .collect(Collectors.toList());

            lastDependenciesNeeded = dependencies;

            if (!dependencies.isEmpty()) {
                dependenciesPanel.removeAll();

                installAllDependencies.setVisible(true);
                installAllDependencies.setEnabled(true);

                dependencies.forEach(dependency -> dependenciesPanel
                    .add(new CurseForgeFileDependencyCard(this, dependency, instanceOrServer)));

                dependenciesPanel.setLayout(new GridLayout(dependencies.size() < 2 ? 1 : dependencies.size() / 2,
                    (dependencies.size() / 2) + 1));

                setSize(UIScale.scale(550), UIScale.scale(450));
                setLocationRelativeTo(App.launcher.getParent());

                dependenciesSection.setVisible(true);

                dependenciesContainer.repaint();
                dependenciesContainer.validate();
            } else {
                installAllDependencies.setVisible(false);
                setSize(UIScale.scale(550), UIScale.scale(200));
            }
        } else {
            installAllDependencies.setVisible(false);
            setSize(UIScale.scale(550), UIScale.scale(200));
        }
    }

    /**
     * Installs every required dependency, and everything those need in turn, behind one progress
     * dialog rather than a version selector each.
     */
    private void installAllDependencies() {
        installAllDependencies.setEnabled(false);

        List<CurseForgeFileDependency> required = lastDependenciesNeeded;

        if (required == null || required.isEmpty()) {
            return;
        }

        final ProgressDialog<Void> dialog = new ProgressDialog<>(GetText.tr("Installing Dependencies"), 0,
            GetText.tr("Installing Dependencies"), this);

        dialog.addThread(new Thread(() -> {
            List<Pair<CurseForgeProject, CurseForgeFile>> toInstall = ModDependencyResolver
                .resolveCurseForge(instanceOrServer, required);

            for (int i = 0; i < toInstall.size(); i++) {
                Pair<CurseForgeProject, CurseForgeFile> pair = toInstall.get(i);

                // #. {0} is the mod being installed, {1} is which one it is, {2} is how many there are
                dialog.setLabel(GetText.tr("Installing {0} ({1} of {2})", pair.left().name, i + 1, toInstall.size()));

                try {
                    Analytics.trackEvent(pair.left().getAnalyticsEventForAdded(pair.right()));
                    instanceOrServer.addFileFromCurseForge(pair.left(), pair.right(), dialog);
                } catch (Exception e) {
                    LogManager.logStackTrace("Failed to install dependency " + pair.left().name, e);
                }
            }

            dialog.close();
        }));

        dialog.start();

        reloadDependenciesPanel();
    }

    protected void getFiles() {
        versionsLabel.setVisible(true);
        filesDropdown.setVisible(true);

        Runnable r = () -> {
            LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

            List<CurseForgeFile> projectFiles = CurseForgeApi.getFilesForProject(mod.id);

            if (projectFiles == null) {
                DialogManager.okDialog().setParent(CurseForgeProjectFileSelectorDialog.this)
                    .setTitle(GetText.tr("No files found"))
                    .setContent(new HTMLBuilder().text(GetText.tr(
                            "No files found for this mod. CurseForge may be down or having issues.<br/>Please wait and try again in a few minutes."))
                        .center().build()).setType(DialogManager.ERROR).show();
                dispose();
                return;
            }

            Stream<CurseForgeFile> curseForgeFilesStream = projectFiles.stream()
                .sorted(Comparator.comparingInt((CurseForgeFile file) -> file.id).reversed());

            // resource packs and plugins declare version ranges the launcher cannot reason about, so
            // an exact match on the instance's version leaves nothing at all. Only STRICT exempts
            // them, which is what this has always done - LAX is loose enough to keep them anyway
            boolean exemptFromStrict = App.settings.addModRestriction == AddModRestriction.STRICT
                && (mod.getRootCategoryId() == Constants.CURSEFORGE_RESOURCE_PACKS_SECTION_ID
                    || mod.getRootCategoryId() == Constants.CURSEFORGE_PLUGINS_SECTION_ID);

            if (!exemptFromStrict) {
                List<String> versionsToMatch = ModCompatibility.minecraftVersionsToMatch(instanceOrServer);

                curseForgeFilesStream = curseForgeFilesStream
                    .filter(file -> ModCompatibility.matchesMinecraftVersion(file.gameVersions, versionsToMatch));
            }

            // filter out files not for our loader (if browsing mods)
            if (mod.getRootCategoryId() == Constants.CURSEFORGE_MODS_SECTION_ID) {
                boolean hasOwnLoaderFile = ModCompatibility.hasFileForOwnLoader(instanceOrServer, projectFiles);

                curseForgeFilesStream = curseForgeFilesStream.filter(cf -> ModCompatibility
                    .matchesCurseForgeLoaderTags(cf.gameVersions, instanceOrServer, hasOwnLoaderFile));
            }

            files.addAll(curseForgeFilesStream.collect(Collectors.toList()));

            // ensures that font width is taken into account
            for (CurseForgeFile file : files) {
                filesLength = Math.max(filesLength,
                    getFontMetrics(App.THEME.getNormalFont()).stringWidth(file.displayName) + 100);
            }

            filesDropdown.removeAllItems();

            // try to filter out non compatible mods (Forge on Fabric and vice versa) if no
            // loader gameVersions are set
            if (App.settings.addModRestriction == AddModRestriction.NONE) {
                files.forEach(version -> filesDropdown.addItem(version));
            } else {
                files.stream().filter(version -> {
                    if (!version.gameVersions.contains("Forge") && !version.gameVersions.contains("Fabric")) {
                        String fileName = version.fileName.toLowerCase(Locale.ENGLISH);
                        String displayName = version.displayName.toLowerCase(Locale.ENGLISH);

                        if (loaderVersion != null && loaderVersion.isFabric()) {
                            return !displayName.contains("-forge-") && !displayName.contains("(forge)")
                                && !displayName.contains("[forge") && !fileName.contains("forgemod");
                        }

                        if (loaderVersion != null && !loaderVersion.isFabric()) {
                            // if it's Forge, and the gameVersion has "Fabric" then exclude it
                            return version.gameVersions.contains("Fabric")
                                || (!displayName.toLowerCase(Locale.ENGLISH).contains("-fabric-")
                                && !displayName.contains("(fabric)")
                                && !displayName.contains("[fabric") && !fileName.contains("fabricmod"));
                        }
                    }

                    return true;
                }).forEach(version -> filesDropdown.addItem(version));
            }

            if (filesDropdown.getItemCount() == 0) {
                DialogManager.okDialog().setParent(CurseForgeProjectFileSelectorDialog.this)
                    .setTitle(GetText.tr("No files found"))
                    .setContent(new HTMLBuilder().text(GetText.tr(
                            "No files found for this mod. CurseForge may be down or having issues.<br/>Please wait and try again in a few minutes."))
                        .center().build()).setType(DialogManager.ERROR).show();
                dispose();
            }

            if (this.installedFileId != null) {
                CurseForgeFile installedFile = files.stream().filter(f -> f.id == this.installedFileId).findFirst()
                    .orElse(null);

                if (installedFile != null) {
                    if (!selectNewest) {
                        filesDropdown.setSelectedItem(installedFile);
                    }

                    // #. {0} is the name of the file that the user already has installed
                    installedJLabel.setText(GetText.tr("The version currently installed is {0}", installedFile));
                    installedJLabel.setVisible(true);
                }
            }

            // ensures that the dropdown is at least 200 px wide and has a maximum width of
            // 350 px to prevent overflow. The height is the control's own - it paints a container
            // that does not fit in the 25px this used to pin it to.
            filesDropdown.setPreferredSize(new Dimension(UIScale.scale(Math.min(350, Math.max(200, filesLength))),
                    filesDropdown.getPreferredSize().height));

            filesDropdown.setEnabled(true);
            versionsLabel.setVisible(true);
            filesDropdown.setVisible(true);
            addButton.setEnabled(true);
            viewModButton.setEnabled(true);
            viewFileButton.setEnabled(true);
        };

        new Thread(r).start();
    }
}
