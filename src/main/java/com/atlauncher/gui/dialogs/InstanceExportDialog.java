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
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceExportFormat;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Form;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.container.MD3ListItem;
import com.atlauncher.gui.md3.feedback.MD3WindowDialog;
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
 * Two columns, for the same reason create-pack is: a stacked settings list buried the folder
 * picker - the one control that grows with the instance - under a fold of fields. The form
 * (name, format, destination) sits on the leading edge; the folders take every leftover pixel.
 */
public class InstanceExportDialog extends MD3WindowDialog {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;
    private static final int FORM_WIDTH = 420;
    private static final int FIELD_COLUMNS = 16;
    /** How many lines of help a row may show before the rest lives in the tooltip. */
    private static final int SUPPORTING_LINES = 2;

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
    private JComponent jointPackagingRow;

    /** Ticked over total, so the column says how much of the instance is going. */
    private JLabel folderCount;
    private MD3Checkbox selectAllFolders;

    public InstanceExportDialog(Instance instance) {
        // #. {0} is the name of the instance we're exporting
        super(ownerWindow(), GetText.tr("Export {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        setDialogSize(WIDTH, HEIGHT);

        // #. {0} is the name of the instance we're exporting
        setHeadline(GetText.tr("Export {0}", instance.launcher.name));
        setBody(buildBody());
        buildActions();
    }

    private static Window ownerWindow() {
        return App.launcher == null ? null : App.launcher.getParent();
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(UIScale.scale(MD3Spacing.L), 0));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(0, MD3Spacing.L, 0, MD3Spacing.L));

        body.add(buildForm().inScrollPane(), BorderLayout.WEST);
        body.add(buildFoldersColumn(), BorderLayout.CENTER);

        return body;
    }

    /**
     * Stacked fields, not settings rows. A settings row keeps 320dp for the control; in this
     * column that leaves a sliver for the name and wraps "The name of the instance" into "The n".
     */
    private MD3Form buildForm() {
        MD3Form form = new MD3Form(FORM_WIDTH);

        name = field(Optional.ofNullable(instance.launcher.lastExportName).orElse(instance.launcher.name));
        version = field(Optional.ofNullable(instance.launcher.lastExportVersion).orElse(instance.launcher.version));
        MicrosoftAccount selectedAccount = AccountManager.getSelectedAccount();
        author = field(Optional.ofNullable(instance.launcher.lastExportAuthor)
                .orElse(selectedAccount == null ? "" : selectedAccount.minecraftUsername));

        form.addSection(GetText.tr("Pack"));
        form.addField(GetText.tr("Name"), GetText.tr("The name of the instance"), name);
        form.addFields(GetText.tr("Version"), GetText.tr("The version of this instance"), version,
                GetText.tr("Author"), GetText.tr("Your name"), author);

        format = new MD3ComboBox<>();
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE, "CurseForge"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MODRINTH, "Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE_AND_MODRINTH, "CurseForge & Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MULTIMC, "MultiMC"));
        format.setToolTipText(GetText.tr("Which format to export this instance as"));
        selectDefaultFormat();

        jointPackaging = new MD3Switch();

        skipHashVerification = new MD3Switch();
        skipHashVerification.setSelected(instance.launcher.lastExportSkipHashVerification != null
                ? instance.launcher.lastExportSkipHashVerification
                : App.settings != null && App.settings.skipExportHashVerification);

        form.addSection(GetText.tr("Format"));
        form.addControl(format);

        // these titles have to be the msgids the po file already holds. The first rewrite
        // shortened them, gettext answered with the English, and a Chinese session showed a
        // half-translated form
        jointPackagingRow = form.addToggle(GetText.tr("Joint Packaging"),
                GetText.tr("Include single-platform mods"),
                GetText.tr(
                        "Also include mods that are only published on the other platform. For Modrinth exports they are added as external download entries, for CurseForge exports they are kept in overrides and listed in modlist.html. You may need distribution permission from the mod authors to publish the exported pack."),
                jointPackaging);
        form.addToggle(GetText.tr("Skip hash check, use metadata only"), null,
                GetText.tr(
                        "By default, export fingerprints every file against CurseForge and Modrinth so mods without stored IDs can still be listed. Skip that lookup to export using only the project and file metadata already on the instance. Use this when files have been changed or hash lookup fails."),
                skipHashVerification);

        format.addActionListener(e -> updateJointPackagingState());
        updateJointPackagingState();

        saveTo = field(Optional.ofNullable(instance.launcher.lastExportSaveTo)
                .orElse(instance.getRoot().toAbsolutePath().toString()));
        saveTo.setEnabled(!OS.isUsingFlatpak());
        saveTo.setToolTipText(GetText.tr("Select the folder you wish to export the instance to"));

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"), MD3Icon.of(MD3Icons.FOLDER));
        browseButton.addActionListener(e -> browseForDirectory());

        MD3Button resetButton = MD3Button.text(GetText.tr("Reset"));
        resetButton.addActionListener(e -> saveTo.setText(instance.getRoot().toAbsolutePath().toString()));

        form.addSection(GetText.tr("Destination"));
        form.addControl(MD3Form.row(saveTo, browseButton, resetButton));

        return form;
    }

    private static MD3TextField field(String value) {
        MD3TextField field = new MD3TextField(FIELD_COLUMNS);
        field.setText(value);

        return field;
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

    /**
     * The folders take the leftover width and height. They used to be a wide row at the bottom of
     * a scrolling form, so a long instance hid them under the fold.
     */
    private JPanel buildFoldersColumn() {
        JLabel heading = new JLabel(GetText.tr("Folders To Export"));
        heading.setFont(MD3Type.font(MD3Type.TITLE_SMALL, heading.getText()));
        heading.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        heading.setForeground(MD3Color.onSurfaceVariant());
        heading.setAlignmentX(LEFT_ALIGNMENT);

        MD3FittingLabel supporting = MD3FittingLabel.supporting(MD3Type.BODY_SMALL,
                GetText.tr("Select the folders you wish to include for this export"), SUPPORTING_LINES);
        supporting.fitTo(UIScale.scale(WIDTH - FORM_WIDTH - MD3Spacing.L * 3));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(MD3Spacing.XS, 0, MD3Spacing.S, 0));
        header.add(heading);
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
        header.add(supporting);
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
        header.add(buildFoldersSelection());

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(header, BorderLayout.NORTH);
        column.add(buildFoldersList(), BorderLayout.CENTER);

        updateFolderCount();

        return column;
    }

    /**
     * One tick for the lot, and how many of them are going.
     *
     * <p>
     * This was a pair of "Select All" / "Select None" text buttons - two controls for one binary
     * choice, in the visual weight of the actions that do the export. The count is numerals on
     * purpose: it needs no translating, and there was previously no way to tell what was selected
     * without scrolling the list and counting.
     */
    private JPanel buildFoldersSelection() {
        selectAllFolders = new MD3Checkbox(GetText.tr("Select All"));
        selectAllFolders.setOpaque(false);
        selectAllFolders.addActionListener(e -> setAllFolders(selectAllFolders.isSelected()));

        folderCount = new JLabel();
        folderCount.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        folderCount.setForeground(MD3Color.onSurfaceVariant());

        JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEADING, UIScale.scale(MD3Spacing.S), 0));
        selection.setOpaque(false);
        selection.setAlignmentX(LEFT_ALIGNMENT);
        selection.add(selectAllFolders);
        selection.add(folderCount);

        return selection;
    }

    private JComponent buildFoldersList() {
        JPanel folders = new JPanel();
        folders.setOpaque(false);
        folders.setLayout(new BoxLayout(folders, BoxLayout.Y_AXIS));

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

            folders.add(folderRow(filename.getName()));
        }

        if (folderBoxes.isEmpty()) {
            JLabel empty = new JLabel(GetText.tr("No extra folders found in this instance."));
            empty.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, empty.getText()));
            empty.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
            empty.setForeground(MD3Color.onSurfaceVariant());
            empty.setAlignmentX(LEFT_ALIGNMENT);
            folders.add(empty);

            selectAllFolders.setEnabled(false);
        }

        return MD3ListContainer.wrapping(folders);
    }

    private MD3ListItem folderRow(String name) {
        MD3Checkbox checkBox = new MD3Checkbox();
        checkBox.setCompact(true);
        checkBox.addItemListener(e -> {
            if (checkBox.isSelected()) {
                if (!overrides.contains(name)) {
                    overrides.add(name);
                }
            } else {
                overrides.remove(name);
            }

            updateFolderCount();
        });

        if (isRecommendedFolder(name)) {
            checkBox.setSelected(true);
        }

        folderBoxes.add(checkBox);

        MD3ListItem item = MD3ListItem.of(name);
        item.setLeading(checkBox);
        item.setClickable(true);
        item.addActionListener(e -> checkBox.setSelected(!checkBox.isSelected()));

        return item;
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

    /**
     * Keeps the count and the select-all tick honest about the list below them. The tick has no
     * indeterminate state, so a partial selection reads as clear - the count is what distinguishes
     * "none" from "some".
     */
    private void updateFolderCount() {
        if (folderCount == null) {
            return;
        }

        folderCount.setText(overrides.size() + " / " + folderBoxes.size());
        folderCount.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, folderCount.getText()));

        if (selectAllFolders != null && !folderBoxes.isEmpty()) {
            selectAllFolders.setSelected(overrides.size() == folderBoxes.size());
        }
    }

    private void buildActions() {
        MD3Button exportButton = MD3Button.filled(GetText.tr("Export"));
        exportButton.addActionListener(e -> startExport());

        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(e -> close());

        setActions(cancelButton, exportButton);
        setDefaultAction(exportButton);
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
}
