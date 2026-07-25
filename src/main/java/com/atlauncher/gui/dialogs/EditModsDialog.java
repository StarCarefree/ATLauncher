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
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.Server;
import com.atlauncher.data.curseforge.CurseForgeFingerprint;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.FabricMod;
import com.atlauncher.data.minecraft.MCMod;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.gui.WheelScrollLayerUI;
import com.atlauncher.gui.components.ModsJCheckBox;
import com.atlauncher.gui.handlers.ModsJCheckBoxTransferHandler;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.FileUtils;
import com.atlauncher.utils.Hashing;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.Utils;

import com.formdev.flatlaf.util.UIScale;

public class EditModsDialog extends JDialog {
    private static final long serialVersionUID = 7004414192679481818L;

    public final ModManagement instanceOrServer;

    private JPanel disabledModsPanel, enabledModsPanel;
    private MD3Button checkForUpdatesButton;
    private MD3Button reinstallButton;
    private MD3Button enableButton;
    private MD3Button disableButton;
    private MD3Button removeButton;
    private MD3Button refreshMetadataButton;
    private JCheckBox selectAllEnabledModsCheckbox, selectAllDisabledModsCheckbox;
    private ArrayList<ModsJCheckBox> enabledMods, disabledMods;

    public EditModsDialog(Instance instance) {
        super(App.launcher.getParent(),
            // #. {0} is the name of the instance
            GetText.tr("Editing Mods For {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instanceOrServer = instance;

        setup();
    }

    public EditModsDialog(Server server) {
        super(App.launcher.getParent(),
            // #. {0} is the name of the instance
            GetText.tr("Editing Mods For {0}", server.name), ModalityType.DOCUMENT_MODAL);
        this.instanceOrServer = server;

        setup();
    }

    private void setup() {
        // wide enough that the row of actions is one row: at the old 550 the last of them wrapped
        // onto a line of its own
        setSize(UIScale.scale(780), UIScale.scale(520));
        setMinimumSize(UIScale.scale(new Dimension(550, 400)));
        setLocationRelativeTo(App.launcher.getParent());
        setLayout(new BorderLayout());
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                dispose();
            }
        });

        setupComponents();

        instanceOrServer.scanMissingMods(this);

        loadMods();
    }

    private void setupComponents() {
        Analytics.sendScreenView("Edit Mods Dialog");

        getContentPane().setBackground(MD3Color.surface());

        // the two lists used to be four nested JSplitPanes with their dividers disabled and sized to
        // zero - a layout, written as something the user could have dragged. Two equal columns is
        // what that was drawing, so that is what this is
        disabledModsPanel = new JPanel();
        disabledModsPanel.setLayout(new BoxLayout(disabledModsPanel, BoxLayout.Y_AXIS));
        disabledModsPanel.setBackground(UIManager.getColor("Mods.modSelectionColor"));
        disabledModsPanel.setTransferHandler(new ModsJCheckBoxTransferHandler(this, true));

        enabledModsPanel = new JPanel();
        enabledModsPanel.setLayout(new BoxLayout(enabledModsPanel, BoxLayout.Y_AXIS));
        enabledModsPanel.setBackground(UIManager.getColor("Mods.modSelectionColor"));
        enabledModsPanel.setTransferHandler(new ModsJCheckBoxTransferHandler(this, true));

        selectAllEnabledModsCheckbox = new JCheckBox(GetText.tr("Select All"));
        selectAllEnabledModsCheckbox.setOpaque(false);
        selectAllEnabledModsCheckbox.addActionListener(e -> {
            boolean selected = selectAllEnabledModsCheckbox.isSelected();

            enabledMods.forEach(em -> em.setSelected(selected));
        });

        selectAllDisabledModsCheckbox = new JCheckBox(GetText.tr("Select All"));
        selectAllDisabledModsCheckbox.setOpaque(false);
        selectAllDisabledModsCheckbox.addActionListener(e -> {
            boolean selected = selectAllDisabledModsCheckbox.isSelected();

            disabledMods.forEach(dm -> dm.setSelected(selected));
        });

        JPanel columns = new JPanel(new GridLayout(1, 2, MD3Spacing.scale(MD3Spacing.L), 0));
        columns.setOpaque(false);
        columns.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, 0, MD3Spacing.L));
        columns.add(buildColumn(GetText.tr("Enabled Mods"), selectAllEnabledModsCheckbox, enabledModsPanel));
        columns.add(buildColumn(GetText.tr("Disabled Mods"), selectAllDisabledModsCheckbox, disabledModsPanel));

        add(columns, BorderLayout.CENTER);

        // left aligned, because this is a toolbar rather than an action bar - there is nothing to
        // confirm here, the dialog is closed by its window control and every change is already made
        JPanel bottomPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S),
            MD3Spacing.scale(MD3Spacing.S)));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottom.add(bottomPanel, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);

        MD3Button addButton = MD3Button.filled(GetText.tr("Add Mod"));
        addButton.addActionListener(e -> {
            String[] modTypes;

            if (instanceOrServer instanceof Instance) {
                modTypes = new String[] { "Mods Folder", "Data Pack", "Resource Pack", "Shader Pack",
                    "Inside Minecraft.jar" };
            } else if (instanceOrServer.getLoaderVersion() != null && (instanceOrServer.getLoaderVersion().isPaper()
                || instanceOrServer.getLoaderVersion().isPurpur())) {
                modTypes = new String[] { "Plugins Folder" };
            } else {
                modTypes = new String[] { "Mods Folder" };
            }

            FileChooserDialog fcd = new FileChooserDialog(this, GetText.tr("Add Mod"), GetText.tr("Mod"),
                GetText.tr("Add"), GetText.tr("Type of Mod"), modTypes);
            fcd.setVisible(true);

            if (fcd.wasClosed()) {
                return;
            }

            final ProgressDialog<Object> progressDialog = new ProgressDialog<>(GetText.tr("Copying Mods"), 0,
                GetText.tr("Copying Mods"), this);

            progressDialog.addThread(new Thread(() -> {
                List<File> files = fcd.getChosenFiles();
                if (files != null && !files.isEmpty()) {
                    boolean reload = false;
                    for (File file : files) {
                        String typeTemp = fcd.getSelectorValue();
                        com.atlauncher.data.Type type = null;
                        if (typeTemp.equalsIgnoreCase("Mods Folder")) {
                            type = com.atlauncher.data.Type.mods;
                        } else if (typeTemp.equalsIgnoreCase("Inside Minecraft.jar")) {
                            int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Add As Mod?"))
                                .setContent(new HTMLBuilder().text(GetText.tr(
                                        "Adding as Inside Minecraft.jar is usually not what you want and will likely cause issues.<br/><br/>If you're adding mods this is usually not correct. Do you want to add this as a mod instead?"))
                                    .build())
                                .setType(DialogManager.WARNING).show();

                            if (ret != 0) {
                                type = com.atlauncher.data.Type.jar;
                            } else {
                                type = com.atlauncher.data.Type.mods;
                            }
                        } else if (typeTemp.equalsIgnoreCase("CoreMods Mod")) {
                            type = com.atlauncher.data.Type.coremods;
                        } else if (typeTemp.equalsIgnoreCase("Texture Pack")) {
                            type = com.atlauncher.data.Type.texturepack;
                        } else if (typeTemp.equalsIgnoreCase("Data Pack")) {
                            type = com.atlauncher.data.Type.datapack;
                        } else if (typeTemp.equalsIgnoreCase("Resource Pack")) {
                            type = com.atlauncher.data.Type.resourcepack;
                        } else if (typeTemp.equalsIgnoreCase("Shader Pack")) {
                            type = com.atlauncher.data.Type.shaderpack;
                        } else if (typeTemp.equalsIgnoreCase("Plugins Folder")) {
                            type = com.atlauncher.data.Type.plugins;
                        }
                        if (type != null) {
                            DisableableMod mod = DisableableMod.generateMod(file, type,
                                App.settings.enableAddedModsByDefault);
                            File copyTo = App.settings.enableAddedModsByDefault ? mod.getFile(instanceOrServer)
                                : mod.getDisabledFile(instanceOrServer);

                            if (copyTo.exists()) {
                                LogManager.warn("The file " + file.getName() + " already exists. Not adding!");
                                continue;
                            }

                            if (!copyTo.getParentFile().exists()) {
                                copyTo.getParentFile().mkdirs();
                            }

                            if (Utils.copyFile(file, copyTo, true)) {
                                instanceOrServer.addMod(mod);
                                reload = true;
                            }
                        }
                    }
                    if (reload) {
                        reloadPanels();
                    }
                }
                progressDialog.close();
            }));

            progressDialog.start();
        });
        bottomPanel.add(addButton);

        if (instanceOrServer instanceof Server || (instanceOrServer instanceof Instance
            && ((Instance) instanceOrServer).launcher.enableCurseForgeIntegration)) {
            if (ConfigManager.getConfigItem("platforms.curseforge.modsEnabled", true)
                || (ConfigManager.getConfigItem("platforms.modrinth.modsEnabled", true)
                && (instanceOrServer.getLoaderVersion() != null
                || instanceOrServer instanceof Instance))) {
                MD3Button browseMods = MD3Button.outlined(GetText.tr("Browse Mods"));
                browseMods.addActionListener(e -> {
                    AddModsDialog addModsDialog = new AddModsDialog(this, instanceOrServer);
                    addModsDialog.setVisible(true);

                    loadMods();

                    reloadPanels();
                });
                bottomPanel.add(browseMods);
            }

            checkForUpdatesButton = MD3Button.text(GetText.tr("Check For Updates"));
            checkForUpdatesButton.addActionListener(e -> checkForUpdates());
            checkForUpdatesButton.setEnabled(false);
            bottomPanel.add(checkForUpdatesButton);

            reinstallButton = MD3Button.text(GetText.tr("Reinstall"));
            reinstallButton.addActionListener(e -> reinstall());
            reinstallButton.setEnabled(false);
            bottomPanel.add(reinstallButton);
        }

        enableButton = MD3Button.text(GetText.tr("Enable Selected"));
        enableButton.addActionListener(e -> enableMods());
        enableButton.setEnabled(false);
        bottomPanel.add(enableButton);

        disableButton = MD3Button.text(GetText.tr("Disable Selected"));
        disableButton.addActionListener(e -> disableMods());
        disableButton.setEnabled(false);
        bottomPanel.add(disableButton);

        removeButton = MD3Button.text(GetText.tr("Remove Selected"));
        removeButton.addActionListener(e -> removeMods());
        removeButton.setEnabled(false);
        bottomPanel.add(removeButton);

        refreshMetadataButton = MD3Button.text(GetText.tr("Refresh Metadata"));
        refreshMetadataButton.addActionListener(e -> refreshMetadata());
        refreshMetadataButton.setEnabled(false);
        bottomPanel.add(refreshMetadataButton);
    }

    /**
     * One of the two lists: what it holds, a way to tick all of it, and the mods themselves.
     *
     * <p>
     * The select-all box used to be an unlabelled tick beside the heading, which said nothing about
     * what ticking it would do.
     */
    private JComponent buildColumn(String title, JCheckBox selectAll, JPanel mods) {
        JLabel label = new JLabel(title);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, title));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.primary());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));
        header.add(label, BorderLayout.WEST);
        header.add(selectAll, BorderLayout.EAST);

        JScrollPane scroller = new JScrollPane(mods, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(header, BorderLayout.NORTH);
        column.add(new JLayer<>(scroller, new WheelScrollLayerUI()), BorderLayout.CENTER);

        return column;
    }

    private void loadMods() {
        List<DisableableMod> mods = instanceOrServer.getMods().stream().filter(DisableableMod::wasSelected)
            .filter(m -> !m.skipped && m.type != com.atlauncher.data.Type.worlds)
            .sorted(Comparator.comparing(m -> m.name, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
        enabledMods = new ArrayList<>();
        disabledMods = new ArrayList<>();

        for (DisableableMod mod : mods) {
            // the bounds these used to be given here were overwritten by the box layout before
            // anything read them
            ModsJCheckBox checkBox = new ModsJCheckBox(mod, this);

            if (mod.isDisabled()) {
                disabledMods.add(checkBox);
            } else {
                enabledMods.add(checkBox);
            }
        }

        for (ModsJCheckBox checkBox : enabledMods) {
            checkBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                    checkBoxesChanged();
                }
            });
            enabledModsPanel.add(checkBox);
        }
        for (ModsJCheckBox checkBox : disabledMods) {
            checkBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                    checkBoxesChanged();
                }
            });
            disabledModsPanel.add(checkBox);
        }
        enabledModsPanel.setPreferredSize(new Dimension(0, heightOf(enabledMods)));
        disabledModsPanel.setPreferredSize(new Dimension(0, heightOf(disabledMods)));
    }

    /**
     * How tall a list of mods is, which is what the scroll pane needs to know.
     *
     * <p>
     * Was the number of them times a hardcoded 20 - the height of a check box at 100% and at no
     * other scale or font size, so the last few mods fell off the bottom of a list that would not
     * scroll far enough to reach them. Asking the rows how tall they are is right at any scale.
     */
    private static int heightOf(List<ModsJCheckBox> mods) {
        int height = 0;

        for (ModsJCheckBox mod : mods) {
            height += mod.getPreferredSize().height;
        }

        return height;
    }

    private void checkBoxesChanged() {
        if (instanceOrServer instanceof Server || (instanceOrServer instanceof Instance
            && ((Instance) instanceOrServer).launcher.enableCurseForgeIntegration)) {
            boolean hasSelectedACurseForgeOrModrinthMod = (enabledMods.stream().anyMatch(AbstractButton::isSelected)
                && enabledMods.stream().filter(AbstractButton::isSelected)
                .anyMatch(cb -> cb.getDisableableMod().isUpdatable()))
                || (disabledMods.stream().anyMatch(AbstractButton::isSelected) && disabledMods.stream()
                .filter(AbstractButton::isSelected).anyMatch(cb -> cb.getDisableableMod().isUpdatable()));

            checkForUpdatesButton.setEnabled(hasSelectedACurseForgeOrModrinthMod);
            reinstallButton.setEnabled(hasSelectedACurseForgeOrModrinthMod);
        }

        removeButton.setEnabled((!disabledMods.isEmpty() && disabledMods.stream().anyMatch(AbstractButton::isSelected))
            || (!enabledMods.isEmpty() && enabledMods.stream().anyMatch(AbstractButton::isSelected)));
        enableButton.setEnabled(!disabledMods.isEmpty() && disabledMods.stream().anyMatch(AbstractButton::isSelected));
        disableButton.setEnabled(!enabledMods.isEmpty() && enabledMods.stream().anyMatch(AbstractButton::isSelected));
        refreshMetadataButton
            .setEnabled(!enabledMods.isEmpty() && enabledMods.stream().anyMatch(AbstractButton::isSelected));

        selectAllEnabledModsCheckbox
            .setSelected(!enabledMods.isEmpty() && enabledMods.stream().allMatch(AbstractButton::isSelected));
        selectAllDisabledModsCheckbox
            .setSelected(!disabledMods.isEmpty() && disabledMods.stream().allMatch(AbstractButton::isSelected));
    }

    private void checkForUpdates() {
        ArrayList<ModsJCheckBox> mods = new ArrayList<>();
        mods.addAll(enabledMods);
        mods.addAll(disabledMods);

        ProgressDialog<Void> progressDialog = new ProgressDialog<>(GetText.tr("Checking For Updates"), mods.size(),
            GetText.tr("Checking For Updates"), this);
        progressDialog.addThread(new Thread(() -> {
            for (ModsJCheckBox mod : mods) {
                if (mod.isSelected() && mod.getDisableableMod().isUpdatable()) {
                    mod.getDisableableMod().checkForUpdate(progressDialog, instanceOrServer);
                }
                progressDialog.doneTask();
            }

            progressDialog.close();
        }));
        progressDialog.start();

        DialogManager.okDialog().setTitle(GetText.tr("Checking For Updates Complete"))
            .setContent(GetText.tr("The selected mods have been checked for updates.")).show();

        reloadPanels();
    }

    private void reinstall() {
        ArrayList<ModsJCheckBox> mods = new ArrayList<>();
        mods.addAll(enabledMods);
        mods.addAll(disabledMods);

        for (ModsJCheckBox mod : mods) {
            if (mod.isSelected() && mod.getDisableableMod().isUpdatable()) {
                mod.getDisableableMod().reinstall(this, instanceOrServer);
            }
        }
        reloadPanels();
    }

    private void enableMods() {
        ArrayList<ModsJCheckBox> mods = new ArrayList<>(disabledMods);
        for (ModsJCheckBox mod : mods) {
            if (mod.isSelected()) {
                mod.getDisableableMod().enable(instanceOrServer);
            }
        }
        reloadPanels();
    }

    private void disableMods() {
        ArrayList<ModsJCheckBox> mods = new ArrayList<>(enabledMods);
        for (ModsJCheckBox mod : mods) {
            if (mod.isSelected()) {
                mod.getDisableableMod().disable(instanceOrServer);
            }
        }
        reloadPanels();
    }

    private void removeMods() {
        int ret = DialogManager.yesNoDialog(false)
            .setTitle(GetText.tr("Delete Selected Mods?"))
            .setContent(new HTMLBuilder().center().text(GetText.tr(
                    "This will delete the selected mods from the instance.<br/><br/>Are you sure you want to do this?"))
                .build())
            .setType(DialogManager.WARNING).show();

        if (ret == 0) {
            ArrayList<ModsJCheckBox> mods = new ArrayList<>(enabledMods);
            for (ModsJCheckBox mod : mods) {
                if (mod.isSelected()) {
                    instanceOrServer.getMods().remove(mod.getDisableableMod());
                    FileUtils.delete(
                        (mod.getDisableableMod().isDisabled()
                            ? mod.getDisableableMod().getDisabledFile(instanceOrServer)
                            : mod.getDisableableMod().getFile(instanceOrServer)).toPath(),
                        true);
                    enabledMods.remove(mod);
                }
            }
            mods = new ArrayList<>(disabledMods);
            for (ModsJCheckBox mod : mods) {
                if (mod.isSelected()) {
                    instanceOrServer.getMods().remove(mod.getDisableableMod());
                    FileUtils.delete(
                        (mod.getDisableableMod().isDisabled()
                            ? mod.getDisableableMod().getDisabledFile(instanceOrServer)
                            : mod.getDisableableMod().getFile(instanceOrServer)).toPath(),
                        true);
                    disabledMods.remove(mod);
                }
            }
            reloadPanels();
        }
    }

    private void refreshMetadata() {
        final ProgressDialog<Boolean> dialog = new ProgressDialog<>(GetText.tr("Refreshing Metadata"), 0,
            GetText.tr("Refreshing Metadata"),
            "Aborting refreshing metadata");
        dialog.addThread(new Thread(() -> {

            List<ModsJCheckBox> modsToRefresh = new ArrayList<>();
            modsToRefresh
                .addAll(enabledMods.parallelStream().filter(ModsJCheckBox::isSelected)
                    .collect(Collectors.toList()));
            modsToRefresh
                .addAll(disabledMods.parallelStream().filter(ModsJCheckBox::isSelected)
                    .collect(Collectors.toList()));

            // TODO: Generalise this, cause fuck me I've copy pasted this like 10 times now
            if (!App.settings.dontCheckModsOnCurseForge) {
                Map<Long, ModsJCheckBox> murmurHashes = new HashMap<>();

                modsToRefresh.stream()
                    .filter(mjc -> mjc.getDisableableMod().getFile(instanceOrServer.getRoot(),
                        instanceOrServer.getMinecraftVersion()) != null)
                    .forEach(mjc -> {
                        try {
                            long hash = Hashing
                                .murmur(mjc.getDisableableMod().getFile(instanceOrServer.getRoot(),
                                    instanceOrServer.getMinecraftVersion()).toPath());
                            murmurHashes.put(hash, mjc);
                        } catch (IOException t) {
                            LogManager.logStackTrace(t);
                        }
                    });

                if (!murmurHashes.isEmpty()) {
                    CurseForgeFingerprint fingerprintResponse = CurseForgeApi
                        .checkFingerprints(murmurHashes.keySet().stream().toArray(Long[]::new));

                    if (fingerprintResponse != null && fingerprintResponse.exactMatches != null) {
                        int[] projectIdsFound = fingerprintResponse.exactMatches.stream().mapToInt(em -> em.id)
                            .toArray();

                        if (projectIdsFound.length != 0) {
                            Map<Integer, CurseForgeProject> foundProjects = CurseForgeApi
                                .getProjectsAsMap(projectIdsFound);

                            if (foundProjects != null) {
                                fingerprintResponse.exactMatches.stream().filter(em -> em != null && em.file != null
                                    && murmurHashes.containsKey(em.file.packageFingerprint)).forEach(foundMod -> {
                                    DisableableMod dm = murmurHashes.get(foundMod.file.packageFingerprint)
                                        .getDisableableMod();

                                    CurseForgeProject curseForgeProject = foundProjects.get(foundMod.id);

                                    if (curseForgeProject != null && curseForgeProject.status == 4) {
                                        dm.curseForgeProjectId = foundMod.id;
                                        dm.curseForgeFile = foundMod.file;
                                        dm.curseForgeFileId = foundMod.file.id;
                                        dm.curseForgeProject = curseForgeProject;
                                        dm.name = curseForgeProject.name;
                                        dm.description = curseForgeProject.summary;

                                        LogManager.debug("Found matching mod from CurseForge called "
                                            + dm.curseForgeFile.displayName);
                                    }

                                    // reset if the file is not approved
                                    if (curseForgeProject != null && curseForgeProject.status != 4) {
                                        dm.curseForgeProjectId = null;
                                        dm.curseForgeFile = null;
                                        dm.curseForgeFileId = null;
                                        dm.curseForgeProject = null;

                                        File path = dm.getFile(instanceOrServer);
                                        MCMod mcMod = Utils.getMCModForFile(path);
                                        if (mcMod != null) {
                                            dm.name = Optional.ofNullable(mcMod.name)
                                                .orElse(path.getName());
                                            dm.description = mcMod.description;
                                        } else {
                                            FabricMod fabricMod = Utils.getFabricModForFile(path);
                                            if (fabricMod != null) {
                                                dm.name = Optional.ofNullable(fabricMod.name)
                                                    .orElse(path.getName());
                                                dm.description = fabricMod.description;
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }

            if (!App.settings.dontCheckModsOnModrinth) {
                Map<String, ModsJCheckBox> sha1Hashes = new HashMap<>();

                modsToRefresh.stream()
                    .filter(mjc -> mjc.getDisableableMod().getFile(instanceOrServer.getRoot(),
                        instanceOrServer.getMinecraftVersion()) != null)
                    .forEach(mjc -> {
                        try {
                            sha1Hashes.put(
                                Hashing.sha1(
                                        mjc.getDisableableMod()
                                            .getFile(instanceOrServer.getRoot(),
                                                instanceOrServer.getMinecraftVersion())
                                            .toPath())
                                    .toString(),
                                mjc);
                        } catch (Throwable t) {
                            LogManager.logStackTrace(t);
                        }
                    });

                if (!sha1Hashes.isEmpty()) {
                    Set<String> keys = sha1Hashes.keySet();
                    Map<String, ModrinthVersion> modrinthVersions = ModrinthApi
                        .getVersionsFromSha1Hashes(keys.toArray(new String[0]));

                    if (modrinthVersions != null && !modrinthVersions.isEmpty()) {
                        String[] projectIdsFound = modrinthVersions.values().stream().map(mv -> mv.projectId)
                            .toArray(String[]::new);

                        if (projectIdsFound.length != 0) {
                            Map<String, ModrinthProject> foundProjects = ModrinthApi.getProjectsAsMap(projectIdsFound);

                            if (foundProjects != null) {
                                for (Map.Entry<String, ModrinthVersion> entry : modrinthVersions.entrySet()) {
                                    ModrinthVersion version = entry.getValue();
                                    ModrinthProject project = foundProjects.get(version.projectId);

                                    if (project != null) {
                                        DisableableMod dm = sha1Hashes.get(entry.getKey()).getDisableableMod();

                                        // add Modrinth information
                                        dm.modrinthProject = project;
                                        dm.modrinthVersion = version;
                                        dm.name = project.title;
                                        dm.description = project.description;

                                        LogManager
                                            .debug(String.format(
                                                "Found matching mod from Modrinth called %s with file %s",
                                                project.title, version.name));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            instanceOrServer.save();

            dialog.close();
        }));
        dialog.start();

        reloadPanels();
    }

    public void reloadPanels() {
        instanceOrServer.save();

        enabledModsPanel.removeAll();
        disabledModsPanel.removeAll();
        loadMods();
        checkBoxesChanged();
        enabledModsPanel.revalidate();
        enabledModsPanel.repaint();
        disabledModsPanel.revalidate();
        disabledModsPanel.repaint();
    }

}