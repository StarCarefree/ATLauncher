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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceExportFormat;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.components.JLabelWithHover;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.input.MD3Checkbox;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Pair;
import com.atlauncher.utils.Utils;
import com.atlauncher.utils.WindowUtils;

import com.formdev.flatlaf.util.UIScale;

public class InstanceExportDialog extends JDialog {
    private final Instance instance;
    private final List<String> overrides = new ArrayList<>();

    private final JPanel topPanel = new JPanel();
    private final JPanel bottomPanel = new JPanel();

    final ImageIcon HELP_ICON = Utils.getIconImage(App.THEME.getIconPath("question"));

    final GridBagConstraints gbc = new GridBagConstraints();

    public InstanceExportDialog(Instance instance) {
        // #. {0} is the name of the instance we're exporting
        super(App.launcher.getParent(), GetText.tr("Export {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        setupComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                close();
            }
        });

        WindowUtils.resizeForContent(this);
    }

    private void setupComponents() {
        setLocationRelativeTo(App.launcher.getParent());
        setLayout(new BorderLayout());
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        topPanel.setOpaque(false);
        topPanel.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        topPanel.setLayout(new GridBagLayout());

        // Name
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        JLabelWithHover nameLabel = new JLabelWithHover(GetText.tr("Name") + ":", HELP_ICON,
            GetText.tr("The name of the instance"));
        topPanel.add(nameLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        final MD3TextField name = new MD3TextField(30);
        name.setText(Optional.ofNullable(instance.launcher.lastExportName).orElse(instance.launcher.name));
        topPanel.add(name, gbc);

        // Version
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        JLabelWithHover versionLabel = new JLabelWithHover(GetText.tr("Version") + ":", HELP_ICON,
            GetText.tr("The version of this instance"));
        topPanel.add(versionLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        final MD3TextField version = new MD3TextField(30);
        version.setText(Optional.ofNullable(instance.launcher.lastExportVersion).orElse(instance.launcher.version));
        topPanel.add(version, gbc);

        // Author
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        JLabelWithHover authorLabel = new JLabelWithHover(GetText.tr("Author") + ":", HELP_ICON,
            GetText.tr("Your name"));
        topPanel.add(authorLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        final MD3TextField author = new MD3TextField(30);
        final MicrosoftAccount selectedAccount = AccountManager.getSelectedAccount();
        author.setText(Optional.ofNullable(instance.launcher.lastExportAuthor)
            .orElse(selectedAccount == null ? "" : selectedAccount.minecraftUsername));
        topPanel.add(author, gbc);

        // Format
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        JLabelWithHover formatLabel = new JLabelWithHover(GetText.tr("Format") + ":", HELP_ICON,
            GetText.tr("Which format to export this instance as"));
        topPanel.add(formatLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        final MD3ComboBox<ComboItem<InstanceExportFormat>> format = new MD3ComboBox<>();
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE, "CurseForge"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MODRINTH, "Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE_AND_MODRINTH, "CurseForge & Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MULTIMC, "MultiMC"));
        topPanel.add(format, gbc);

        for (int i = 0; i < format.getItemCount(); i++) {
            ComboItem<InstanceExportFormat> item = format.getItemAt(i);

            if (item.getValue() == App.settings.defaultExportFormat) {
                format.setSelectedIndex(i);
                break;
            }
        }

        // Joint Packaging
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        JLabelWithHover jointPackagingLabel = new JLabelWithHover(GetText.tr("Joint Packaging") + ":", HELP_ICON,
            GetText.tr(
                "Also include mods that are only published on the other platform. For Modrinth exports they are added as external download entries, for CurseForge exports they are kept in overrides and listed in modlist.html. You may need distribution permission from the mod authors to publish the exported pack."));
        topPanel.add(jointPackagingLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        final MD3Checkbox jointPackaging = new MD3Checkbox(GetText.tr("Include single-platform mods"));
        topPanel.add(jointPackaging, gbc);

        Runnable updateJointPackagingState = () -> {
            boolean supported = ((ComboItem<InstanceExportFormat>) format.getSelectedItem())
                .getValue() != InstanceExportFormat.MULTIMC;
            jointPackaging.setEnabled(supported);
            if (!supported) {
                jointPackaging.setSelected(false);
            }
        };
        format.addActionListener(e -> updateJointPackagingState.run());
        updateJointPackagingState.run();

        // Export File
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
        JLabelWithHover saveToLabel = new JLabelWithHover(GetText.tr("Save To") + ":", HELP_ICON,
            GetText.tr("Select the folder you wish to export the instance to"));
        topPanel.add(saveToLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;

        JPanel saveToPanel = new JPanel();
        saveToPanel.setOpaque(false);
        saveToPanel.setLayout(new BoxLayout(saveToPanel, BoxLayout.X_AXIS));

        final MD3TextField saveTo = new MD3TextField(25);
        saveTo.setText(Optional.ofNullable(instance.launcher.lastExportSaveTo)
            .orElse(instance.getRoot().toAbsolutePath().toString()));

        // Disable manual input on flatpak (require proper xdg selection)
        saveTo.setEnabled(!OS.isUsingFlatpak());

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"));
        browseButton.addActionListener(e -> {
            FileChooserDialog fcd = new FileChooserDialog(this,
                GetText.tr("Select export directory"),
                GetText.tr("Directory"),
                GetText.tr("Select"));
            fcd.setVisible(true);

            if (fcd.wasClosed()) {
                return;
            }

            List<File> files = fcd.getChosenFiles();

            if (files != null && !files.isEmpty()) {
                File dir = files.get(0);
                saveTo.setText(dir.getAbsolutePath());
            }
        });

        MD3Button resetButton = MD3Button.outlined(GetText.tr("Reset"));
        resetButton.addActionListener(e -> saveTo.setText(instance.getRoot().toAbsolutePath().toString()));

        saveToPanel.add(saveTo);
        saveToPanel.add(Box.createHorizontalStrut(5));
        saveToPanel.add(browseButton);
        saveToPanel.add(resetButton);

        topPanel.add(saveToPanel, gbc);

        // Overrides
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        JLabelWithHover overridesLabel = new JLabelWithHover(GetText.tr("Folders To Export") + ":", HELP_ICON,
            GetText.tr("Select the folders you wish to include for this export"));
        topPanel.add(overridesLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;

        JPanel overridesPanel = new JPanel();
        overridesPanel.setOpaque(false);
        overridesPanel.setLayout(new BoxLayout(overridesPanel, BoxLayout.Y_AXIS));

        // get all files ignoring ATLauncher specific things as well as naughtys
        File[] files = Optional.ofNullable(instance.getRoot().toFile()
            .listFiles(pathname -> !pathname.getName().equalsIgnoreCase("disabledmods")
                && !pathname.getName().equalsIgnoreCase("instance.json")
                && !pathname.getName().equalsIgnoreCase(".fabric")
                && !pathname.getName().equalsIgnoreCase(".quilt"))).orElse(new File[0]);

        for (File filename : files) {
            // skip any folders with no files inside
            if (filename.isDirectory() && Optional.ofNullable(filename.listFiles()).orElse(new File[0]).length == 0) {
                continue;
            }

            MD3Checkbox checkBox = new MD3Checkbox(filename.getName());

            checkBox.addItemListener(e -> {
                if (checkBox.isSelected()) {
                    overrides.add(checkBox.getText());
                } else {
                    overrides.remove(checkBox.getText());
                }
            });

            if (filename.getName().equalsIgnoreCase("config") || filename.getName().equalsIgnoreCase("mods")
                || filename.getName().equalsIgnoreCase("oresources")
                || filename.getName().equalsIgnoreCase("resourcepacks")
                || filename.getName().equalsIgnoreCase("shaderpacks")
                || filename.getName().equalsIgnoreCase("datapacks")
                || filename.getName().equalsIgnoreCase("resources")
                || filename.getName().equalsIgnoreCase("scripts")) {
                checkBox.setSelected(true);
            }

            overridesPanel.add(checkBox);
        }

        MD3ListContainer overridesContainer = MD3ListContainer.wrapping(overridesPanel);
        overridesContainer.setPreferredSize(UIScale.scale(new Dimension(350, 200)));

        topPanel.add(overridesContainer, gbc);

        // bottom panel
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        bottomPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        MD3Button exportButton = MD3Button.filled(GetText.tr("Export"));
        exportButton.addActionListener(arg0 -> {
            instance.scanMissingMods(this);

            final ProgressDialog<Object> dialog = new ProgressDialog<>(GetText.tr("Exporting Instance"), 0,
                GetText.tr("Exporting Instance. Please wait..."), null, this);

            dialog.addThread(new Thread(() -> {
                InstanceExportFormat exportFormat = ((ComboItem<InstanceExportFormat>) format.getSelectedItem())
                    .getValue();

                Pair<Path, String> exportResult = instance.export(name.getText(), version.getText(),
                    author.getText(),
                    exportFormat, saveTo.getText(), overrides, jointPackaging.isSelected());

                if (exportResult.left() != null) {
                    instance.launcher.lastExportName = name.getText();
                    instance.launcher.lastExportVersion = version.getText();
                    instance.launcher.lastExportAuthor = author.getText();
                    instance.launcher.lastExportSaveTo = saveTo.getText();
                    instance.save();

                    if ((exportFormat == InstanceExportFormat.MODRINTH
                        || exportFormat == InstanceExportFormat.CURSEFORGE_AND_MODRINTH)
                        && exportResult.right() != null && !exportResult.right().isEmpty()) {
                        ModrinthExportOverridesDialog modrinthExportOverridesDialog = new ModrinthExportOverridesDialog(
                            dialog, exportResult.right());
                        modrinthExportOverridesDialog.setVisible(true);
                    }

                    NotificationManager.show(GetText.tr("Exported Instance Successfully"));
                    if (exportFormat == InstanceExportFormat.CURSEFORGE_AND_MODRINTH) {
                        OS.openFileExplorer(Paths.get(saveTo.getText()));
                    } else {
                        OS.openFileExplorer(exportResult.left(), true);
                    }
                } else {
                    DialogManager.okDialog().setType(DialogManager.ERROR).setTitle(GetText.tr("Export Failed"))
                            .setContent(GetText.tr("Failed to export instance. Check the console for details"))
                            .show();
                }
                dialog.close();
                close();
            }));

            dialog.start();
        });
        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(arg0 -> close());

        // confirm goes rightmost: cancel first, export last
        bottomPanel.add(cancelButton);
        bottomPanel.add(exportButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottom.add(bottomPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void close() {
        setVisible(false);
        dispose();
    }
}
