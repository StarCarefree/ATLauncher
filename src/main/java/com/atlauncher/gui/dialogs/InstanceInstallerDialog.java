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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Pack;
import com.atlauncher.data.PackVersion;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.curseforge.pack.CurseForgeManifest;
import com.atlauncher.data.ftb.FTBPackLink;
import com.atlauncher.data.ftb.FTBPackLinkType;
import com.atlauncher.data.ftb.FTBPackManifest;
import com.atlauncher.data.ftb.FTBPackVersion;
import com.atlauncher.data.installables.ATLauncherInstallable;
import com.atlauncher.data.installables.CurseForgeInstallable;
import com.atlauncher.data.installables.CurseForgeManifestInstallable;
import com.atlauncher.data.installables.FTBInstallable;
import com.atlauncher.data.installables.Installable;
import com.atlauncher.data.installables.ModrinthInstallable;
import com.atlauncher.data.installables.ModrinthManifestInstallable;
import com.atlauncher.data.installables.MultiMCInstallable;
import com.atlauncher.data.installables.TechnicModpackInstallable;
import com.atlauncher.data.installables.VanillaInstallable;
import com.atlauncher.data.json.Version;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.VersionManifestVersionType;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.minecraft.loaders.fabric.FabricLoader;
import com.atlauncher.data.minecraft.loaders.forge.ForgeLoader;
import com.atlauncher.data.minecraft.loaders.legacyfabric.LegacyFabricLoader;
import com.atlauncher.data.minecraft.loaders.neoforge.NeoForgeLoader;
import com.atlauncher.data.minecraft.loaders.paper.PaperLoader;
import com.atlauncher.data.minecraft.loaders.purpur.PurpurLoader;
import com.atlauncher.data.minecraft.loaders.quilt.QuiltLoader;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.data.modrinth.pack.ModrinthModpackManifest;
import com.atlauncher.data.multimc.MultiMCComponent;
import com.atlauncher.data.multimc.MultiMCManifest;
import com.atlauncher.data.technic.TechnicModpack;
import com.atlauncher.data.technic.TechnicModpackSlim;
import com.atlauncher.data.technic.TechnicSolderModpack;
import com.atlauncher.exceptions.InvalidMinecraftVersion;
import com.atlauncher.exceptions.InvalidPack;
import com.atlauncher.graphql.fragment.UnifiedModPackResultsFragment;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.CurseForgeUpdateManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.MinecraftManager;
import com.atlauncher.managers.PackManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.NetworkClient;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.FTBApi;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.Pair;
import com.atlauncher.utils.TechnicApi;
import com.atlauncher.utils.WindowUtils;

import okhttp3.CacheControl;

public class InstanceInstallerDialog extends JDialog {
    private static final long serialVersionUID = -6984886874482721558L;
    private int versionLength = 0;
    private int loaderVersionLength = 0;
    private boolean isReinstall = false;
    private boolean isServer = false;
    private Pack pack;
    private Instance instance = null;
    private CurseForgeManifest curseForgeManifest = null;
    private ModrinthModpackManifest modrinthManifest = null;
    private CurseForgeProject curseForgeProject = null;
    private ModrinthProject modrinthProject = null;
    private ModrinthVersion preselectedModrinthVersion = null;
    private FTBPackManifest ftbPackManifest = null;
    private MultiMCManifest multiMCManifest = null;
    private TechnicModpack technicModpack = null;
    private UnifiedModPackResultsFragment unifiedModpackResult = null;

    private final MD3SettingsList middle;
    private final MD3Button install;
    private final JTextField nameField;
    private JComboBox<PackVersion> versionsDropDown;
    private final JComboBox<ComboItem<LoaderVersion>> loaderVersionsDropDown = new JComboBox<>();
    private final List<LoaderVersion> loaderVersions = new ArrayList<>();

    private final JCheckBox showAllMinecraftVersionsCheckbox = new JCheckBox(GetText.tr("Show All"));

    /**
     * The rows, kept because each of them is shown only when it applies: there is no version to
     * choose when importing a MultiMC pack, no loader version unless the pack has a choosable one,
     * and nothing to save unless the update changes the Minecraft version.
     */
    private MD3SettingsList.Row versionRow;
    private MD3SettingsList.Row loaderVersionRow;
    private MD3SettingsList.Row saveModsRow;

    private JCheckBox saveModsCheckbox;
    private final boolean isUpdate;
    private final PackVersion autoInstallVersion;
    private final Path extractedPath;

    public InstanceInstallerDialog(CurseForgeManifest manifest, Path curseExtractedPath) {
        this(manifest, false, false, null, false, curseExtractedPath, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(ModrinthModpackManifest manifest, Path modrinthExtractedPath) {
        this(manifest, false, false, null, false, modrinthExtractedPath, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(MultiMCManifest manifest, Path multiMCExtractedPath) {
        this(manifest, false, false, null, false, multiMCExtractedPath, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object) {
        this(object, false, false, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(ModrinthProject modrinthProject, ModrinthVersion preselectedModrinthVersion) {
        this(modrinthProject, false, false, null, true, null, App.launcher.getParent(),
            preselectedModrinthVersion);
    }

    public InstanceInstallerDialog(Object object, boolean isServer) {
        this(object, false, isServer, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(UnifiedModPackResultsFragment resultsFragment, boolean isServer) {
        this(resultsFragment, false, isServer, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Window parent, Object object) {
        this(object, false, false, null, true, null, parent, null);
    }

    public InstanceInstallerDialog(Pack pack, PackVersion version, boolean showModsChooser) {
        this(pack, false, false, version, showModsChooser, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Pack pack, boolean isServer) {
        this(pack, false, isServer, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object, boolean isUpdate, boolean isServer, PackVersion autoInstallVersion,
        boolean showModsChooser, Path extractedPath) {
        this(object, isUpdate, isServer, autoInstallVersion, showModsChooser, extractedPath,
            App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object, final boolean isUpdate, final boolean isServer,
        final PackVersion autoInstallVersion, final boolean showModsChooser,
        Path extractedPathCon, Window parent, ModrinthVersion preselectedModrinthVersion) {
        super(parent, ModalityType.DOCUMENT_MODAL);

        setName("instanceInstallerDialog");
        this.isUpdate = isUpdate;
        this.autoInstallVersion = autoInstallVersion;
        this.extractedPath = extractedPathCon;
        this.preselectedModrinthVersion = preselectedModrinthVersion;
        this.isServer = isServer;

        Analytics.sendScreenView("Instance Installer Dialog");

        if (object instanceof Pack) {
            handlePackInstall(object);
        } else if (object instanceof CurseForgeProject) {
            handleCurseForgeInstall(object);
        } else if (object instanceof FTBPackManifest) {
            handleFTBInstall(object);
        } else if (object instanceof ModrinthSearchHit || object instanceof ModrinthProject) {
            handleModrinthInstall(object);
        } else if (object instanceof TechnicModpackSlim || object instanceof TechnicModpack) {
            handleTechnicInstall(object);
        } else if (object instanceof CurseForgeManifest) {
            handleCurseForgeImport(object);
        } else if (object instanceof ModrinthModpackManifest) {
            handleModrinthImport(object);
        } else if (object instanceof MultiMCManifest) {
            handleMultiMcImport(object);
        } else if (object instanceof UnifiedModPackResultsFragment) {
            handleUnifiedModPackInstall(object);
        } else {
            handleInstanceInstall(object);
        }

        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        install = MD3Button.filled(
            ((isReinstall) ? (isUpdate ? GetText.tr("Update") : GetText.tr("Reinstall")) : GetText.tr("Install")));

        // Headline
        String packName = Optional.ofNullable(pack).map(Pack::getName).orElse("Pack");
        String headlineText = ((isReinstall) ? (isUpdate ? GetText.tr("Updating") : GetText.tr("Reinstalling"))
            : GetText.tr("Installing")) + " " + packName
            + (isReinstall ? GetText.tr(" (Current Version: {0})", instance.getVersionOfPack()) : "");

        JLabel headline = new JLabel(headlineText);
        // a pack's name is not the launcher's own text, so the theme's face may have no glyphs for it
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE, headlineText));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        top.add(headline, BorderLayout.CENTER);

        // The form
        middle = new MD3SettingsList();

        nameField = new JTextField(17);
        nameField.setText(((isReinstall) ? instance.launcher.name : packName));
        if (isReinstall) {
            nameField.setEnabled(false);
        }
        nameField.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent ce) {
                nameField.requestFocusInWindow();
            }
        });
        nameField.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent pE) {
            }

            @Override
            public void focusGained(final FocusEvent pE) {
                nameField.selectAll();
            }
        });
        middle.addRow(GetText.tr("Name"), null, nameField);

        this.setupVersionsDropdown();

        if (isReinstall && instance.launcher.vanillaInstance) {
            showAllMinecraftVersionsCheckbox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                    setVanillaPackVersions(e.getStateChange() == ItemEvent.SELECTED);
                    setVersionsDropdown();
                }
            });
        } else {
            showAllMinecraftVersionsCheckbox.setVisible(false);
        }

        this.setupLoaderVersionsDropdown();

        if (!this.isServer && isReinstall) {
            saveModsCheckbox = new JCheckBox();

            saveModsRow = middle.addRow(GetText.tr("Save Mods"), MD3Text.plain(GetText.tr(
                "Since this update changes the Minecraft version, your custom mods may no longer work.<br/><br/>Checking this box will keep your custom mods, otherwise they'll be removed.")),
                saveModsCheckbox);

            PackVersion packVersion = ((PackVersion) versionsDropDown.getSelectedItem());
            Optional<VersionManifestVersion> minecraftVersion = Optional.ofNullable(packVersion)
                .map(pv -> pv.minecraftVersion);

            saveModsRow.setVisible(
                minecraftVersion.isPresent() && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));
        }

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.CENTER);

        install.addActionListener(e -> {
            Installable installable;

            PackVersion packVersion = ((PackVersion) versionsDropDown.getSelectedItem());
            if (packVersion == null) {
                LogManager.error("No version selected");
                setVisible(false);
                dispose();
                return;
            }

            LoaderVersion loaderVersion = (packVersion.hasLoader() && packVersion.hasChoosableLoader())
                ? ((ComboItem<LoaderVersion>) loaderVersionsDropDown.getSelectedItem()).getValue()
                : null;

            if (curseForgeManifest != null) {
                installable = new CurseForgeManifestInstallable(pack, packVersion, loaderVersion);

                installable.curseForgeManifest = curseForgeManifest;
                installable.curseExtractedPath = extractedPath;
            } else if (curseForgeProject != null) {
                installable = new CurseForgeInstallable(pack, packVersion, loaderVersion);

                installable.curseForgeManifest = curseForgeManifest;
                installable.curseExtractedPath = extractedPath;
            } else if (modrinthProject != null) {
                installable = new ModrinthInstallable(pack, packVersion, loaderVersion);

                installable.modrinthProject = modrinthProject;
                installable.modrinthVersion = packVersion._modrinthVersion;
            } else if (modrinthManifest != null) {
                installable = new ModrinthManifestInstallable(pack, packVersion, loaderVersion);

                installable.modrinthManifest = modrinthManifest;
                installable.modrinthExtractedPath = extractedPath;
            } else if (ftbPackManifest != null) {
                installable = new FTBInstallable(pack, packVersion, loaderVersion);

                installable.ftbPackManifest = ftbPackManifest;
            } else if (multiMCManifest != null) {
                installable = new MultiMCInstallable(pack, packVersion, loaderVersion);

                installable.multiMCManifest = multiMCManifest;
                installable.multiMCExtractedPath = extractedPath;
            } else if (technicModpack != null) {
                installable = new TechnicModpackInstallable(pack, packVersion, loaderVersion);

                installable.technicModpack = technicModpack;
            } else if (instance != null && instance.launcher.vanillaInstance) {
                installable = new VanillaInstallable(packVersion.minecraftVersion, loaderVersion,
                    instance.launcher.description);
            } else {
                installable = new ATLauncherInstallable(pack, packVersion, loaderVersion);
            }

            if (instance != null) {
                installable.instance = instance;
            }

            installable.instanceName = nameField.getText();
            installable.isReinstall = isReinstall;
            installable.isUpdate = isUpdate;
            installable.isServer = isServer;
            installable.saveMods = !isServer && isReinstall && saveModsCheckbox != null
                && saveModsCheckbox.isSelected();

            setVisible(false);

            boolean success = installable.startInstall();

            if (success) {
                System.gc();
                dispose();
            }
        });
        MD3Button cancel = MD3Button.text(GetText.tr("Cancel"));
        cancel.addActionListener(e -> dispose());

        // the confirming action goes on the trailing edge
        actions.add(cancel);
        actions.add(install);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        WindowUtils.resizeForContent(this);
    }

    private void handlePackInstall(Object object) {
        pack = (Pack) object;
        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", pack.getName()));
        if (this.isServer) {
            // #. {0} is the name of the pack the user is installing
            setTitle(GetText.tr("Installing {0} Server", pack.getName()));
        }
    }

    private void handleCurseForgeInstall(Object object) {
        curseForgeProject = (CurseForgeProject) object;

        pack = new Pack();
        pack.name = curseForgeProject.name;

        pack.externalId = curseForgeProject.id;
        pack.description = curseForgeProject.summary;
        pack.websiteURL = curseForgeProject.getWebsiteUrl();
        pack.curseForgeProject = curseForgeProject;

        final ProgressDialog<Pair<List<CurseForgeFile>, String>> dialog = new ProgressDialog<>(
            GetText.tr("Getting Versions"), 0,
            GetText.tr("Getting Versions"), "Aborting Getting Versions");

        dialog.addThread(new Thread(() -> {
            List<CurseForgeFile> files = CurseForgeApi.getFilesForProject(curseForgeProject.id);
            String description = CurseForgeApi.getProjectDescription(curseForgeProject.id);

            if (isServer) {
                int[] serverFileIds = files.stream().filter(file -> file.serverPackFileId != null)
                    .mapToInt(file -> file.serverPackFileId).toArray();
                List<CurseForgeFile> serverFiles = CurseForgeApi.getFiles(serverFileIds);

                if (serverFiles == null) {
                    serverFiles = new ArrayList<>();
                }

                dialog.setReturnValue(
                    new Pair<>(serverFiles.stream().map(f -> {
                            if (f.getGameVersion() == null) {
                                Optional<CurseForgeFile> matchingFile = files.stream()
                                    .filter(sf -> sf.serverPackFileId != null)
                                    .filter(sf -> sf.serverPackFileId == f.id).findFirst();

                                matchingFile.ifPresent(curseForgeFile -> f.gameVersions = curseForgeFile.gameVersions);
                            }

                            return f;
                        }).filter(f -> f.isAvailable && f.isServerPack && f.getGameVersion() != null)
                        .collect(Collectors.toList()), description));
            } else {
                dialog.setReturnValue(new Pair<>(files, description));
            }

            dialog.close();
        }));

        dialog.start();

        Pair<List<CurseForgeFile>, String> returnValue = dialog.getReturnValue();
        if (returnValue == null) {
            DialogManager.okDialog().setTitle(GetText.tr("Error"))
                .setContent(new HTMLBuilder().text(GetText.tr(
                        "Failed to get project files from CurseForge."))
                    .center().build())
                .show();
            return;
        }

        List<CurseForgeFile> files = Optional.ofNullable(returnValue.left()).orElse(new ArrayList<>());
        pack.curseForgeProjectDescription = returnValue.right();

        if (files.isEmpty()) {
            if (isServer) {
                DialogManager.okDialog().setTitle(GetText.tr("No Server Files Available"))
                    .setContent(new HTMLBuilder().text(GetText.tr(
                            "No server files are available for this pack, so a server cannot be created."))
                        .center().build())
                    .setType(DialogManager.ERROR)
                    .show();
            } else {
                DialogManager.okDialog().setTitle(GetText.tr("No Files Available"))
                    .setContent(new HTMLBuilder().text(GetText.tr(
                            "No files are available for this pack, so it cannot be installed.<br/>CurseForge may be down or having issues. Please wait and try again in a few minutes."))
                        .center().build())
                    .setType(DialogManager.ERROR)
                    .show();
            }
            return;
        }

        pack.versions = files.stream().sorted(Comparator.comparingInt((CurseForgeFile file) -> file.id).reversed())
            .map(f -> {
                PackVersion packVersion = new PackVersion();
                packVersion.version = f.getDisplayName();
                packVersion.hasLoader = true;
                packVersion._curseForgeFile = f;

                if (!App.settings.allowCurseForgeAlphaBetaFiles) {
                    packVersion.isRecommended = f.isReleaseType();
                }

                try {
                    packVersion.minecraftVersion = MinecraftManager.getMinecraftVersion(f.getGameVersion());
                } catch (InvalidMinecraftVersion e) {
                    LogManager.warn(String.format("Failed to find Minecraft version for %s", f.getGameVersion()));
                    // somewhat valid, can happen, so grab version from the manifest
                    packVersion.minecraftVersion = null;
                }

                return packVersion;
            }).filter(Objects::nonNull).collect(Collectors.toList());

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", curseForgeProject.name));
    }

    private void handleVanillaInstall() {
        pack = new Pack();
        pack.vanillaInstance = true;
        pack.name = instance.launcher.pack;
        pack.description = instance.launcher.description;

        setVanillaPackVersions(false);
    }

    private void setVanillaPackVersions(boolean showAll) {
        pack.versions = MinecraftManager.getMinecraftVersions().stream()
            .filter(mv -> showAll || mv.type == instance.type).filter(mv -> {
                if (mv.type == VersionManifestVersionType.EXPERIMENT
                    && !ConfigManager.getConfigItem("minecraft.experiment.enabled", true)) {
                    return false;
                }

                if (mv.type == VersionManifestVersionType.SNAPSHOT
                    && !ConfigManager.getConfigItem("minecraft.snapshot.enabled", true)) {
                    return false;
                }

                if (mv.type == VersionManifestVersionType.RELEASE
                    && !ConfigManager.getConfigItem("minecraft.release.enabled", true)) {
                    return false;
                }

                if (mv.type == VersionManifestVersionType.OLD_BETA
                    && !ConfigManager.getConfigItem("minecraft.old_beta.enabled", true)) {
                    return false;
                }

                return mv.type != VersionManifestVersionType.OLD_ALPHA
                    || ConfigManager.getConfigItem("minecraft.old_alpha.enabled", true);
            }).map(v -> {
                PackVersion packVersion = new PackVersion();
                packVersion.version = v.id;
                packVersion.minecraftVersion = v;

                if (instance.launcher.loaderVersion != null) {
                    packVersion.hasLoader = true;
                    packVersion.hasChoosableLoader = true;
                    packVersion.loaderType = instance.launcher.loaderVersion.type;
                }

                return packVersion;
            }).collect(Collectors.toList());
    }

    private void handleFTBInstall(Object object) {
        ftbPackManifest = (FTBPackManifest) object;

        pack = new Pack();
        pack.externalId = ftbPackManifest.id;
        pack.name = ftbPackManifest.name;
        pack.description = ftbPackManifest.description;

        if (ftbPackManifest.links != null) {
            FTBPackLink link = ftbPackManifest.links.stream()
                .filter(l -> l.type == FTBPackLinkType.WEBSITE).findFirst().orElse(null);

            if (link != null) {
                pack.websiteURL = link.link;
            }
        }

        pack.ftbPack = ftbPackManifest;

        pack.versions = ftbPackManifest.versions.stream()
            .sorted(Comparator.comparingInt((FTBPackVersion version) -> version.updated).reversed())
            .map(v -> {
                PackVersion packVersion = new PackVersion();
                packVersion.version = v.name;
                packVersion.hasLoader = true;
                packVersion._ftbId = v.id;
                packVersion._ftbType = v.type;
                return packVersion;
            }).filter(pv -> pv != null).collect(Collectors.toList());

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", ftbPackManifest.name));
    }

    private void handleModrinthInstall(Object object) {
        if (object instanceof ModrinthSearchHit) {
            ModrinthSearchHit modrinthSearchHit = (ModrinthSearchHit) object;

            final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                "Aborting Getting Modpack Details");

            modrinthProjectLookupDialog.addThread(new Thread(() -> {
                modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getProject(modrinthSearchHit.projectId));

                modrinthProjectLookupDialog.close();
            }));

            modrinthProjectLookupDialog.start();
            modrinthProject = modrinthProjectLookupDialog.getReturnValue();
        } else {
            modrinthProject = (ModrinthProject) object;
        }

        pack = new Pack();
        pack.name = modrinthProject.title;

        // pack.externalId = modrinthProject.id; // TODO: Fuck me we got a String here
        pack.description = modrinthProject.description;
        pack.websiteURL = String.format("https://modrinth.com/modpack/%s", modrinthProject.slug);
        pack.modrinthProject = modrinthProject;

        final ProgressDialog<List<ModrinthVersion>> modrinthProjectLookupDialog = new ProgressDialog<>(
            GetText.tr("Getting Modpack Versions"), 0, GetText.tr("Getting Modpack Versions"),
            "Aborting Getting Modpack Versions");

        modrinthProjectLookupDialog.addThread(new Thread(() -> {
            modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getVersions(modrinthProject.id));

            modrinthProjectLookupDialog.close();
        }));

        modrinthProjectLookupDialog.start();
        List<ModrinthVersion> versions = Optional.ofNullable(modrinthProjectLookupDialog.getReturnValue())
            .orElse(new ArrayList<>());

        pack.versions = versions.stream()
            .sorted(Comparator.comparing((ModrinthVersion version) -> version.datePublished).reversed())
            .map(version -> {
                PackVersion packVersion = new PackVersion();
                packVersion.version = String.format("%s (%s)", version.name, version.versionNumber);
                packVersion.hasLoader = !version.loaders.isEmpty();
                packVersion._modrinthVersion = version;

                try {
                    packVersion.minecraftVersion = MinecraftManager
                        .getMinecraftVersion(version.gameVersions.get(0));
                } catch (InvalidMinecraftVersion e) {
                    LogManager.error(e.getMessage());
                    packVersion.minecraftVersion = null;
                }

                return packVersion;
            }).filter(Objects::nonNull).collect(Collectors.toList());

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", modrinthProject.title));
    }

    private void handleTechnicInstall(Object object) {
        String slug;

        if (object instanceof TechnicModpack) {
            slug = ((TechnicModpack) object).name;
        } else if (object instanceof TechnicModpackSlim) {
            slug = ((TechnicModpackSlim) object).slug;
        } else {
            DialogManager.okDialog().setTitle(GetText.tr("Error"))
                .setContent(new HTMLBuilder().text(GetText.tr(
                        "Failed to get slug for modpack from Technic."))
                    .center().build())
                .show();
            return;
        }

        final ProgressDialog<TechnicModpack> technicModpackDialog = new ProgressDialog<>(
            GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
            "Aborting Getting Modpack Details");

        technicModpackDialog.addThread(new Thread(() -> {
            technicModpackDialog.setReturnValue(TechnicApi.getModpackBySlug(slug));

            technicModpackDialog.close();
        }));

        technicModpackDialog.start();
        technicModpack = technicModpackDialog.getReturnValue();

        if (technicModpack == null) {
            LogManager.error("Failed to get modpack from Technic, null response for slug " + slug + ".");
            DialogManager.okDialog().setTitle(GetText.tr("Error"))
                .setContent(new HTMLBuilder().text(GetText.tr(
                        "Failed to get information for modpack from Technic."))
                    .center().build())
                .show();
            return;
        }

        pack = new Pack();
        pack.externalId = technicModpack.id;
        pack.name = technicModpack.displayName;
        pack.description = technicModpack.description;
        pack.websiteURL = technicModpack.platformUrl;

        pack.technicModpack = technicModpack;

        if (technicModpack.solder != null) {
            final ProgressDialog<TechnicSolderModpack> dialog = new ProgressDialog<>(
                GetText.tr("Getting Modpack Builds"), 0, GetText.tr("Getting Modpack Builds"),
                "Aborting Getting Modpack Builds");

            dialog.addThread(new Thread(() -> {
                dialog.setReturnValue(TechnicApi.getSolderModpackBySlug(technicModpack.solder, technicModpack.name));

                dialog.close();
            }));

            dialog.start();

            TechnicSolderModpack technicSolderModpack = dialog.getReturnValue();
            if (technicSolderModpack == null) {
                DialogManager.okDialog().setTitle(GetText.tr("Error"))
                    .setContent(new HTMLBuilder().text(GetText.tr(
                            "Failed to get modpack builds from Technic."))
                        .center().build())
                    .show();
                return;
            }

            pack.versions = technicSolderModpack.builds.stream().map(v -> {
                PackVersion packVersion = new PackVersion();
                packVersion.version = v;
                packVersion.isRecommended = v.equalsIgnoreCase(technicSolderModpack.recommended);
                packVersion._technicRecommended = v.equalsIgnoreCase(technicSolderModpack.recommended);
                packVersion._technicLatest = v.equalsIgnoreCase(technicSolderModpack.latest);
                return packVersion;
            }).collect(Collectors.toList());

            Collections.reverse(pack.versions);
        } else {
            PackVersion packVersion = new PackVersion();
            packVersion.version = technicModpack.version;
            pack.versions = Collections.singletonList(packVersion);
        }

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", technicModpack.displayName));
    }

    private void handleCurseForgeImport(Object object) {
        curseForgeManifest = (CurseForgeManifest) object;

        pack = new Pack();
        pack.name = curseForgeManifest.name;

        if (curseForgeManifest.projectID != null && curseForgeManifest.projectID != 0) {
            CurseForgeProject curseForgeProject = CurseForgeApi.getProjectById(curseForgeManifest.projectID);

            curseForgeManifest.websiteUrl = curseForgeProject.getWebsiteUrl();

            pack.externalId = curseForgeManifest.projectID;
            pack.description = curseForgeProject.summary;
            pack.curseForgeProject = curseForgeProject;
        }

        PackVersion packVersion = new PackVersion();
        packVersion.version = Optional.ofNullable(curseForgeManifest.version).orElse("1.0.0");

        try {
            packVersion.minecraftVersion = MinecraftManager.getMinecraftVersion(curseForgeManifest.minecraft.version);
        } catch (InvalidMinecraftVersion e) {
            LogManager.error(e.getMessage());
            return;
        }

        packVersion.hasLoader = true;

        pack.versions = Collections.singletonList(packVersion);

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", curseForgeManifest.name));
    }

    private void handleModrinthImport(Object object) {
        modrinthManifest = (ModrinthModpackManifest) object;

        pack = new Pack();
        pack.name = modrinthManifest.name;
        pack.description = modrinthManifest.summary;

        PackVersion packVersion = new PackVersion();
        packVersion.version = Optional.ofNullable(modrinthManifest.versionId).orElse("1.0.0");

        try {
            packVersion.minecraftVersion = MinecraftManager
                .getMinecraftVersion(modrinthManifest.dependencies.get("minecraft"));
        } catch (InvalidMinecraftVersion e) {
            LogManager.error(e.getMessage());
            return;
        }

        packVersion.hasLoader = modrinthManifest.dependencies.containsKey("fabric-loader")
            || modrinthManifest.dependencies.containsKey("quilt-loader")
            || modrinthManifest.dependencies.containsKey("neoforge")
            || modrinthManifest.dependencies.containsKey("forge");

        pack.versions = Collections.singletonList(packVersion);

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", modrinthManifest.name));
    }

    private void handleMultiMcImport(Object object) {
        multiMCManifest = (MultiMCManifest) object;

        pack = new Pack();
        pack.name = multiMCManifest.config.name;

        PackVersion packVersion = new PackVersion();
        packVersion.version = "1";

        try {
            Optional<MultiMCComponent> minecraftVersionComponent = multiMCManifest.components.stream()
                .filter(c -> c.uid.equalsIgnoreCase("net.minecraft")).findFirst();

            if (!minecraftVersionComponent.isPresent()) {
                LogManager.error("No net.minecraft component present in manifest");
                return;
            }

            packVersion.minecraftVersion = MinecraftManager
                .getMinecraftVersion(minecraftVersionComponent.get().version);
        } catch (InvalidMinecraftVersion e) {
            LogManager.error(e.getMessage());
            return;
        }

        packVersion.hasLoader = multiMCManifest.components.stream()
            .anyMatch(c -> c.uid.equalsIgnoreCase("net.neoforged")
                || c.uid.equalsIgnoreCase("net.minecraftforge")
                || c.uid.equalsIgnoreCase("net.fabricmc.hashed"));

        pack.versions = Collections.singletonList(packVersion);

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", multiMCManifest.config.name));
    }

    private void handleUnifiedModPackInstall(Object object) {
        unifiedModpackResult = (UnifiedModPackResultsFragment) object;

        switch (unifiedModpackResult.platform()) {
            case ATLAUNCHER: {
                try {
                    Pack pack = PackManager.getPackByID(Integer.parseInt(unifiedModpackResult.id()));

                    handlePackInstall(pack);
                    return;
                } catch (NumberFormatException | InvalidPack e) {
                    LogManager.logStackTrace("Failed to get ATLauncher pack", e);
                    return;
                }
            }
            case MODRINTH: {
                final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                    GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                    "Aborting Getting Modpack Details");

                modrinthProjectLookupDialog.addThread(new Thread(() -> {
                    modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getProject(unifiedModpackResult.id()));

                    modrinthProjectLookupDialog.close();
                }));

                modrinthProjectLookupDialog.start();
                ModrinthProject modrinthProject = modrinthProjectLookupDialog.getReturnValue();

                if (modrinthProject == null) {
                    LogManager.error("Failed to get Modrinth project");
                    return;
                }

                handleModrinthInstall(modrinthProject);
                return;
            }
            case CURSEFORGE: {
                final ProgressDialog<CurseForgeProject> curseForgeProjectLookupDialog = new ProgressDialog<>(
                    GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                    "Aborting Getting Modpack Details");

                curseForgeProjectLookupDialog.addThread(new Thread(() -> {
                    curseForgeProjectLookupDialog
                        .setReturnValue(CurseForgeApi.getProjectById(unifiedModpackResult.id()));

                    curseForgeProjectLookupDialog.close();
                }));

                curseForgeProjectLookupDialog.start();
                CurseForgeProject curseForgeProject = curseForgeProjectLookupDialog.getReturnValue();

                if (curseForgeProject == null) {
                    LogManager.error("Failed to get CurseForge project");
                    return;
                }

                handleCurseForgeInstall(curseForgeProject);
                return;
            }
            case FTB: {
                final ProgressDialog<FTBPackManifest> ftbPackManifestLookupDialog = new ProgressDialog<>(
                    GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                    "Aborting Getting Modpack Details");

                ftbPackManifestLookupDialog.addThread(new Thread(() -> {
                    ftbPackManifestLookupDialog
                        .setReturnValue(FTBApi.getModpackManifest(unifiedModpackResult.id()));

                    ftbPackManifestLookupDialog.close();
                }));

                ftbPackManifestLookupDialog.start();
                FTBPackManifest ftbPackManifest = ftbPackManifestLookupDialog.getReturnValue();

                if (ftbPackManifest == null) {
                    LogManager.error("Failed to get FTB manifest");
                    return;
                }

                handleFTBInstall(ftbPackManifest);
                return;
            }
            case TECHNIC: {
                final ProgressDialog<TechnicModpack> technicModpackLookupDialog = new ProgressDialog<>(
                    GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                    "Aborting Getting Modpack Details");

                technicModpackLookupDialog.addThread(new Thread(() -> {
                    technicModpackLookupDialog
                        .setReturnValue(TechnicApi.getModpackBySlug(unifiedModpackResult.id()));

                    technicModpackLookupDialog.close();
                }));

                technicModpackLookupDialog.start();
                TechnicModpack technicModpack = technicModpackLookupDialog.getReturnValue();

                if (technicModpack == null) {
                    LogManager.error("Failed to get Technic modpack");
                    return;
                }

                handleTechnicInstall(technicModpack);
                return;
            }
            default: // fall out
        }
    }

    private void handleInstanceInstall(Object object) {
        instance = (Instance) object;

        if (instance.isFTBPack()) {
            final ProgressDialog<FTBPackManifest> dialog = new ProgressDialog<>(
                GetText.tr("Downloading Pack Manifest"), 0, GetText.tr("Downloading Pack Manifest"),
                "Cancelled downloading FTB pack manifest", this);
            dialog.addThread(new Thread(() -> {
                FTBPackManifest packManifest = NetworkClient.getCached(
                    String.format(Locale.ENGLISH, "%s/modpack/%d", Constants.FTB_API_URL,
                        instance.launcher.ftbPackManifest.id),
                    FTBPackManifest.class,
                    new CacheControl.Builder().maxStale(10, TimeUnit.MINUTES).build());
                dialog.setReturnValue(packManifest);
                dialog.close();
            }));
            dialog.start();

            if (dialog.wasClosed) {
                setVisible(false);
                dispose();
                return;
            }

            handleFTBInstall(dialog.getReturnValue());
        } else if (instance.isCurseForgePack()) {
            handleCurseForgeInstall(instance.launcher.curseForgeProject);
        } else if (instance.isTechnicPack()) {
            handleTechnicInstall(instance.launcher.technicModpack);
        } else if (instance.isModrinthPack()) {
            handleModrinthInstall(instance.launcher.modrinthProject);
        } else if (instance.launcher.vanillaInstance) {
            handleVanillaInstall();
        } else {
            pack = instance.getPack();
        }

        isReinstall = true; // We're reinstalling

        if (isUpdate) {
            // #. {0} is the name of the instance the user is updating
            setTitle(GetText.tr("Updating {0}", instance.launcher.name));
        } else {
            // #. {0} is the name of the instance the user is reinstalling
            setTitle(GetText.tr("Reinstalling {0}", instance.launcher.name));
        }
    }

    private void setupVersionsDropdown() {
        versionsDropDown = new JComboBox<>();
        setVersionsDropdown();

        // "Show All" only applies to the version beside it, so it belongs on that row rather than
        // on one of its own
        versionRow = middle.addRow(GetText.tr("Version To Install"), null,
            MD3SettingsList.group(versionsDropDown, showAllMinecraftVersionsCheckbox));

        versionsDropDown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                PackVersion packVersion = ((PackVersion) e.getItem());

                if (packVersion != null) {
                    updateLoaderVersions((PackVersion) e.getItem());

                    if (!isServer && isReinstall) {
                        Optional<VersionManifestVersion> minecraftVersion = Optional
                            .ofNullable(packVersion.minecraftVersion);

                        saveModsRow.setVisible(minecraftVersion.isPresent()
                            && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));

                        WindowUtils.resizeForContent(this);
                    }
                }
            }
        });

        if (autoInstallVersion != null) {
            versionsDropDown.setSelectedItem(autoInstallVersion);
            versionsDropDown.setEnabled(false);
        }

        if (preselectedModrinthVersion != null) {
            Optional<PackVersion> versionToSelect = this.pack.versions.stream()
                .filter(pv -> pv._modrinthVersion.id.equals(this.preselectedModrinthVersion.id)).findFirst();

            versionToSelect.ifPresent(packVersion -> versionsDropDown.setSelectedItem(packVersion));
        }

        if (multiMCManifest != null) {
            versionRow.setVisible(false);
        }
    }

    private void setVersionsDropdown() {
        List<PackVersion> versions = new ArrayList<>();
        versionsDropDown.removeAllItems();

        if (pack.isTester()) {
            for (PackVersion pv : pack.getDevVersions()) {
                if (!isServer || (isServer && pv.minecraftVersion != null && pv.minecraftVersion.hasServer())) {
                    versions.add(pv);
                }
            }
        }
        for (PackVersion pv : pack.getVersions()) {
            if (!isServer || (isServer && pv.minecraftVersion != null && pv.minecraftVersion.hasServer())) {
                versions.add(pv);
            }
        }
        PackVersion forUpdate = null;
        for (PackVersion version : versions) {
            if ((!version.isDev) && (forUpdate == null)) {
                forUpdate = version;
            }
            versionsDropDown.addItem(version);
        }

        if (isUpdate && instance != null && instance.isCurseForgePack()) {
            CurseForgeFile latestVersion = CurseForgeUpdateManager.getLatestVersion(instance);
            if (latestVersion != null) {
                for (PackVersion version : versions) {
                    if (version._curseForgeFile.id == latestVersion.id) {
                        forUpdate = version;
                    }
                }
            }
        }

        if (isUpdate && forUpdate != null) {
            versionsDropDown.setSelectedItem(forUpdate);
        } else if (isReinstall) {
            for (PackVersion version : versions) {
                if (version.versionMatches(instance)) {
                    versionsDropDown.setSelectedItem(version);
                }
            }
        } else {
            for (PackVersion version : versions) {
                if (!version.isRecommended || version.isDev) {
                    continue;
                }
                versionsDropDown.setSelectedItem(version);
                break;
            }
        }

        // ensures that font width is taken into account
        for (PackVersion version : versions) {
            versionLength = Math.max(versionLength,
                getFontMetrics(App.THEME.getNormalFont()).stringWidth(version.toString()) + 25);
        }

        // ensures that the dropdown is at least 200 px wide
        versionLength = Math.max(200, versionLength);

        // ensures that there is a maximum width of 250 px to prevent overflow
        versionLength = Math.min(250, versionLength);

        versionsDropDown.setPreferredSize(new Dimension(versionLength, 23));
    }

    protected void updateLoaderVersions(@Nonnull PackVersion item) {
        if (!item.hasLoader() || !item.hasChoosableLoader()) {
            loaderVersionRow.setVisible(false);
            return;
        }

        if (item.loaderType != null && item.loaderType.equalsIgnoreCase("fabric")) {
            if (!ConfigManager.getConfigItem("loaders.fabric.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Fabric"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("forge")) {
            if (!ConfigManager.getConfigItem("loaders.forge.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Forge"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("neoforge")) {
            if (!ConfigManager.getConfigItem("loaders.neoforge.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "NeoForge"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("paper")) {
            if (!ConfigManager.getConfigItem("loaders.paper.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Paper"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("purpur")) {
            if (!ConfigManager.getConfigItem("loaders.purpur.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Purpur"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("legacyfabric")) {
            if (!ConfigManager.getConfigItem("loaders.legacyfabric.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Legacy Fabric"));
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("quilt")) {
            if (!ConfigManager.getConfigItem("loaders.quilt.enabled", false)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionRow.setLabel(GetText.tr("{0} Version", "Quilt"));
        } else {
            loaderVersionRow.setLabel(GetText.tr("Loader Version"));
        }

        loaderVersionsDropDown.setEnabled(false);
        loaderVersions.clear();

        loaderVersionsDropDown.removeAllItems();
        loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("Getting Loader Versions")));

        loaderVersionRow.setVisible(true);

        install.setEnabled(false);
        versionsDropDown.setEnabled(false);

        Runnable r = () -> {
            loaderVersions.clear();

            if (this.instance != null && this.instance.launcher.vanillaInstance) {
                if (this.instance.launcher.loaderVersion.isFabric()) {
                    loaderVersions.addAll(FabricLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isForge()) {
                    loaderVersions.addAll(ForgeLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isNeoForge()) {
                    loaderVersions.addAll(NeoForgeLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isPaper()) {
                    loaderVersions.addAll(PaperLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isPurpur()) {
                    loaderVersions.addAll(PurpurLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isLegacyFabric()) {
                    loaderVersions.addAll(LegacyFabricLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isQuilt()) {
                    loaderVersions.addAll(QuiltLoader.getChoosableVersions(item.minecraftVersion.id));
                } else {
                    return;
                }
            } else {
                Version jsonVersion = Gsons.DEFAULT.fromJson(pack.getJSON(item.version), Version.class);

                if (jsonVersion == null) {
                    return;
                }

                loaderVersions.addAll(jsonVersion.getLoader().getChoosableVersions(jsonVersion.getMinecraft()));
            }

            if (loaderVersions.isEmpty()) {
                loaderVersionsDropDown.removeAllItems();
                loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("No Versions Found")));
                loaderVersionRow.setVisible(true);
                versionsDropDown.setEnabled(true);
                return;
            }

            // ensures that font width is taken into account
            for (LoaderVersion version : loaderVersions) {
                loaderVersionLength = Math.max(loaderVersionLength,
                    getFontMetrics(App.THEME.getNormalFont()).stringWidth(version.toString()) + 25);
            }

            loaderVersionsDropDown.removeAllItems();

            loaderVersions.forEach(version -> loaderVersionsDropDown
                .addItem(new ComboItem<>(version, version.toStringWithCurrent(instance))));

            if (isReinstall && instance.launcher.loaderVersion != null) {
                String loaderVersionString = instance.launcher.loaderVersion.version;

                for (int i = 0; i < loaderVersionsDropDown.getItemCount(); i++) {
                    LoaderVersion loaderVersion = loaderVersionsDropDown.getItemAt(i)
                        .getValue();

                    if (loaderVersion.version.equals(loaderVersionString)) {
                        loaderVersionsDropDown.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // ensures that the dropdown is at least 200 px wide
            loaderVersionLength = Math.max(200, loaderVersionLength);

            // ensures that there is a maximum width of 250 px to prevent overflow
            loaderVersionLength = Math.min(250, loaderVersionLength);

            loaderVersionsDropDown.setPreferredSize(new Dimension(loaderVersionLength, 23));

            loaderVersionsDropDown.setEnabled(true);
            loaderVersionRow.setVisible(true);
            install.setEnabled(true);
            versionsDropDown.setEnabled(true);
        };

        new Thread(r).start();
    }

    private void setupLoaderVersionsDropdown() {
        // the row exists before its name does: which loader this is - Forge's, Fabric's - is only
        // known once a version has been picked, and updateLoaderVersions is what renames it
        loaderVersionRow = middle.addRow("", null, loaderVersionsDropDown);

        if (this.versionsDropDown.getSelectedItem() != null) {
            this.updateLoaderVersions((PackVersion) this.versionsDropDown.getSelectedItem());
        }
    }
}
