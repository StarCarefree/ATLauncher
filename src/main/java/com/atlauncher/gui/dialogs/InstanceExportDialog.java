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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceExportFormat;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3Checkbox;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Pair;
import com.formdev.flatlaf.util.UIScale;

/**
 * Export an instance as a shareable pack.
 *
 * <p>
 * Built as the same list of settings rows the installer and instance settings use, rather than a
 * {@link java.awt.GridBagLayout} of right-aligned labels with a help icon. The explanations used to
 * live in tooltips on a 16px glyph; they now sit under each setting's name.
 */
public class InstanceExportDialog extends JDialog {
    private static final int WIDTH = 720;
    private static final int HEIGHT = 680;
    private static final int FIELD_COLUMNS = 18;

    private final Instance instance;
    private final List<String> overrides = new ArrayList<>();
    private final List<MD3Checkbox> folderBoxes = new ArrayList<>();

    private MD3TextField name;
    private MD3TextField version;
    private MD3TextField author;
    private MD3ComboBox<ComboItem<InstanceExportFormat>> format;
    private MD3Switch jointPackaging;
    private MD3Switch skipHashVerification;
    private MD3TextField saveTo;
    private MD3SettingsList.Row jointPackagingRow;

    public InstanceExportDialog(Instance instance) {
        // #. {0} is the name of the instance we're exporting
        super(ownerWindow(), GetText.tr("Export {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        setupComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                close();
            }
        });
    }

    private static Window ownerWindow() {
        return App.launcher == null ? null : App.launcher.getParent();
    }

    private void setupComponents() {
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(WIDTH / 2, HEIGHT / 2));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        add(buildHeadline(), BorderLayout.NORTH);
        add(wrapForm(buildForm()), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeadline() {
        // #. {0} is the name of the instance we're exporting
        String text = GetText.tr("Export {0}", instance.launcher.name);

        JLabel headline = new JLabel(text);
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE, text));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        top.add(headline, BorderLayout.CENTER);

        return top;
    }

    private JScrollPane wrapForm(MD3SettingsList form) {
        JScrollPane scroll = new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    private MD3SettingsList buildForm() {
        MD3SettingsList form = new MD3SettingsList();

        form.addSection(GetText.tr("Pack"));

        name = new MD3TextField(FIELD_COLUMNS);
        name.setText(Optional.ofNullable(instance.launcher.lastExportName).orElse(instance.launcher.name));
        form.addRow(GetText.tr("Name"), GetText.tr("The name of the instance"), name);

        version = new MD3TextField(FIELD_COLUMNS);
        version.setText(Optional.ofNullable(instance.launcher.lastExportVersion).orElse(instance.launcher.version));
        form.addRow(GetText.tr("Version"), GetText.tr("The version of this instance"), version);

        author = new MD3TextField(FIELD_COLUMNS);
        MicrosoftAccount selectedAccount = AccountManager.getSelectedAccount();
        author.setText(Optional.ofNullable(instance.launcher.lastExportAuthor)
                .orElse(selectedAccount == null ? "" : selectedAccount.minecraftUsername));
        form.addRow(GetText.tr("Author"), GetText.tr("Your name"), author);

        form.addSection(GetText.tr("Format"));

        format = new MD3ComboBox<>();
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE, "CurseForge"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MODRINTH, "Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE_AND_MODRINTH, "CurseForge & Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MULTIMC, "MultiMC"));
        selectDefaultFormat();
        form.addRow(GetText.tr("Format"), GetText.tr("Which format to export this instance as"), format);

        jointPackaging = new MD3Switch();
        jointPackagingRow = form.addRow(GetText.tr("Joint packaging"), GetText.tr(
                "Also include mods that are only published on the other platform. For Modrinth exports they are added as external download entries, for CurseForge exports they are kept in overrides and listed in modlist.html. You may need distribution permission from the mod authors to publish the exported pack."),
                jointPackaging);

        skipHashVerification = new MD3Switch();
        boolean skipHashDefault = instance.launcher.lastExportSkipHashVerification != null
                ? instance.launcher.lastExportSkipHashVerification
                : App.settings != null && App.settings.skipExportHashVerification;
        skipHashVerification.setSelected(skipHashDefault);
        form.addRow(GetText.tr("Skip hash check"), GetText.tr(
                "By default, export fingerprints every file against CurseForge and Modrinth so mods without stored IDs can still be listed. Skip that lookup to export using only the project and file metadata already on the instance. Use this when files have been changed or hash lookup fails."),
                skipHashVerification);

        format.addActionListener(e -> updateJointPackagingState());
        updateJointPackagingState();

        form.addSection(GetText.tr("Destination"));

        saveTo = new MD3TextField(24);
        saveTo.setText(Optional.ofNullable(instance.launcher.lastExportSaveTo)
                .orElse(instance.getRoot().toAbsolutePath().toString()));
        saveTo.setEnabled(!OS.isUsingFlatpak());

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"), MD3Icon.of(MD3Icons.FOLDER));
        browseButton.addActionListener(e -> browseForDirectory());

        MD3Button resetButton = MD3Button.text(GetText.tr("Reset"));
        resetButton.addActionListener(e -> saveTo.setText(instance.getRoot().toAbsolutePath().toString()));

        form.addWideRow(GetText.tr("Save to"),
                GetText.tr("Select the folder you wish to export the instance to"),
                destinationControls(saveTo, browseButton, resetButton));

        form.addSection(GetText.tr("Folders to export"));
        form.addWideRow(GetText.tr("Include"),
                GetText.tr("Select the folders you wish to include for this export"),
                buildFoldersPanel());

        return form;
    }

    private void selectDefaultFormat() {
        InstanceExportFormat preferred = App.settings == null ? InstanceExportFormat.CURSEFORGE
                : App.settings.defaultExportFormat;

        for (int i = 0; i < format.getItemCount(); i++) {
            ComboItem<InstanceExportFormat> item = format.getItemAt(i);

            if (item.getValue() == preferred) {
                format.setSelectedIndex(i);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateJointPackagingState() {
        ComboItem<InstanceExportFormat> selected = (ComboItem<InstanceExportFormat>) format.getSelectedItem();
        boolean supported = selected != null && selected.getValue() != InstanceExportFormat.MULTIMC;

        jointPackaging.setEnabled(supported);
        jointPackagingRow.setEnabled(supported);

        if (!supported) {
            jointPackaging.setSelected(false);
        }
    }

    private void browseForDirectory() {
        FileChooserDialog chooser = new FileChooserDialog(this, GetText.tr("Select export directory"),
                GetText.tr("Directory"), GetText.tr("Select"));
        chooser.setVisible(true);

        if (chooser.wasClosed()) {
            return;
        }

        List<File> files = chooser.getChosenFiles();

        if (files != null && !files.isEmpty()) {
            saveTo.setText(files.get(0).getAbsolutePath());
        }
    }

    private static JPanel destinationControls(MD3TextField saveTo, MD3Button browse, MD3Button reset) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(saveTo);
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        row.add(browse);
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.XS)));
        row.add(reset);

        return row;
    }

    private JPanel buildFoldersPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        File[] files = Optional.ofNullable(instance.getRoot().toFile()
                .listFiles(pathname -> !pathname.getName().equalsIgnoreCase("disabledmods")
                        && !pathname.getName().equalsIgnoreCase("instance.json")
                        && !pathname.getName().equalsIgnoreCase(".fabric")
                        && !pathname.getName().equalsIgnoreCase(".quilt")))
                .orElse(new File[0]);

        for (File filename : files) {
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

            if (isRecommendedFolder(filename.getName())) {
                checkBox.setSelected(true);
            }

            folderBoxes.add(checkBox);
        }

        if (folderBoxes.isEmpty()) {
            JLabel empty = new JLabel(GetText.tr("No extra folders found in this instance."));
            empty.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
            empty.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
            empty.setForeground(MD3Color.onSurfaceVariant());
            empty.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(empty);

            return panel;
        }

        JPanel folders = new JPanel();
        folders.setOpaque(false);
        folders.setLayout(new BoxLayout(folders, BoxLayout.Y_AXIS));

        for (MD3Checkbox box : folderBoxes) {
            folders.add(box);
        }

        MD3ListContainer list = MD3ListContainer.wrapping(folders);
        list.setPreferredSize(UIScale.scale(new Dimension(0, 200)));
        list.setAlignmentX(LEFT_ALIGNMENT);

        JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEADING, UIScale.scale(MD3Spacing.S), 0));
        selection.setOpaque(false);
        selection.setAlignmentX(LEFT_ALIGNMENT);

        MD3Button selectAll = MD3Button.text(GetText.tr("Select all"));
        selectAll.addActionListener(e -> setAllFolders(true));

        MD3Button selectNone = MD3Button.text(GetText.tr("Select none"));
        selectNone.addActionListener(e -> setAllFolders(false));

        selection.add(selectAll);
        selection.add(selectNone);

        panel.add(selection);
        panel.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        panel.add(list);

        return panel;
    }

    private static boolean isRecommendedFolder(String name) {
        return name.equalsIgnoreCase("config") || name.equalsIgnoreCase("mods")
                || name.equalsIgnoreCase("oresources")
                || name.equalsIgnoreCase("resourcepacks")
                || name.equalsIgnoreCase("shaderpacks")
                || name.equalsIgnoreCase("datapacks")
                || name.equalsIgnoreCase("resources")
                || name.equalsIgnoreCase("scripts");
    }

    private void setAllFolders(boolean selected) {
        for (MD3Checkbox box : folderBoxes) {
            box.setSelected(selected);
        }
    }

    private JPanel buildActionBar() {
        MD3Button exportButton = MD3Button.filled(GetText.tr("Export"));
        exportButton.addActionListener(e -> startExport());

        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(e -> close());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
        actions.add(cancelButton);
        actions.add(exportButton);

        getRootPane().setDefaultButton(exportButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(MD3Divider.inset(), BorderLayout.NORTH);
        bar.add(actions, BorderLayout.CENTER);

        return bar;
    }

    @SuppressWarnings("unchecked")
    private void startExport() {
        instance.scanMissingMods(this);

        final ProgressDialog<Object> dialog = new ProgressDialog<>(GetText.tr("Exporting Instance"), 0,
                GetText.tr("Exporting Instance. Please wait..."), null, this);

        dialog.addThread(new Thread(() -> {
            InstanceExportFormat exportFormat = ((ComboItem<InstanceExportFormat>) format.getSelectedItem())
                    .getValue();

            Pair<Path, String> exportResult = instance.export(name.getText(), version.getText(),
                    author.getText(), exportFormat, saveTo.getText(), overrides, jointPackaging.isSelected(),
                    skipHashVerification.isSelected());

            if (exportResult.left() != null) {
                instance.launcher.lastExportName = name.getText();
                instance.launcher.lastExportVersion = version.getText();
                instance.launcher.lastExportAuthor = author.getText();
                instance.launcher.lastExportSaveTo = saveTo.getText();
                instance.launcher.lastExportSkipHashVerification = skipHashVerification.isSelected();
                instance.save();

                if ((exportFormat == InstanceExportFormat.MODRINTH
                        || exportFormat == InstanceExportFormat.CURSEFORGE_AND_MODRINTH)
                        && exportResult.right() != null && !exportResult.right().isEmpty()) {
                    ModrinthExportOverridesDialog overridesDialog = new ModrinthExportOverridesDialog(dialog,
                            exportResult.right());
                    overridesDialog.setVisible(true);
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
    }

    private void close() {
        setVisible(false);
        dispose();
    }
}
