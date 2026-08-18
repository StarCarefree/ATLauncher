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
import java.awt.FontMetrics;
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceExportFormat;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.container.MD3ListItem;
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
public class InstanceExportDialog extends JDialog {
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
    private JPanel jointPackagingRow;

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
        add(buildBody(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(UIScale.scale(MD3Spacing.L), 0));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(0, MD3Spacing.L, 0, MD3Spacing.L));

        JScrollPane form = wrapForm(buildForm());
        form.setPreferredSize(new Dimension(UIScale.scale(FORM_WIDTH), 0));
        form.setMinimumSize(new Dimension(UIScale.scale(FORM_WIDTH), 0));

        body.add(form, BorderLayout.WEST);
        body.add(buildFoldersColumn(), BorderLayout.CENTER);

        return body;
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
        top.setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        top.add(headline, BorderLayout.CENTER);

        return top;
    }

    private JScrollPane wrapForm(JComponent form) {
        JScrollPane scroll = new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    /**
     * Stacked fields, not settings rows. A settings row keeps 320dp for the control; in this
     * column that leaves a sliver for the name and wraps "The name of the instance" into "The n".
     */
    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(MD3Spacing.border(0, 0, MD3Spacing.L, MD3Spacing.S));
        // the scroll pane is FORM_WIDTH; without this a long path or a row of three fields
        // grows the form and the viewport clips the switches and Browse
        form.setMaximumSize(new Dimension(UIScale.scale(FORM_WIDTH), Integer.MAX_VALUE));

        name = field(Optional.ofNullable(instance.launcher.lastExportName).orElse(instance.launcher.name));
        version = field(Optional.ofNullable(instance.launcher.lastExportVersion).orElse(instance.launcher.version));
        MicrosoftAccount selectedAccount = AccountManager.getSelectedAccount();
        author = field(Optional.ofNullable(instance.launcher.lastExportAuthor)
                .orElse(selectedAccount == null ? "" : selectedAccount.minecraftUsername));

        form.add(sectionHeading(GetText.tr("Pack")));
        form.add(sectionCard(identityRow(name, version, author)));

        format = new MD3ComboBox<>();
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE, "CurseForge"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MODRINTH, "Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.CURSEFORGE_AND_MODRINTH, "CurseForge & Modrinth"));
        format.addItem(new ComboItem<>(InstanceExportFormat.MULTIMC, "MultiMC"));
        selectDefaultFormat();
        stretch(format);

        jointPackaging = new MD3Switch();
        // these titles have to be the msgids the po file already holds. The first rewrite
        // shortened them, gettext answered with the English, and a Chinese session showed a
        // half-translated form
        jointPackagingRow = switchRow(GetText.tr("Joint Packaging"),
                GetText.tr("Include single-platform mods"),
                GetText.tr(
                        "Also include mods that are only published on the other platform. For Modrinth exports they are added as external download entries, for CurseForge exports they are kept in overrides and listed in modlist.html. You may need distribution permission from the mod authors to publish the exported pack."),
                jointPackaging);
        skipHashVerification = new MD3Switch();
        boolean skipHashDefault = instance.launcher.lastExportSkipHashVerification != null
                ? instance.launcher.lastExportSkipHashVerification
                : App.settings != null && App.settings.skipExportHashVerification;
        skipHashVerification.setSelected(skipHashDefault);

        format.setToolTipText(GetText.tr("Which format to export this instance as"));

        form.add(sectionHeading(GetText.tr("Format")));
        form.add(sectionCard(format, jointPackagingRow,
                switchRow(GetText.tr("Skip hash check, use metadata only"), null,
                        GetText.tr(
                                "By default, export fingerprints every file against CurseForge and Modrinth so mods without stored IDs can still be listed. Skip that lookup to export using only the project and file metadata already on the instance. Use this when files have been changed or hash lookup fails."),
                        skipHashVerification)));

        format.addActionListener(e -> updateJointPackagingState());
        updateJointPackagingState();

        saveTo = field(Optional.ofNullable(instance.launcher.lastExportSaveTo)
                .orElse(instance.getRoot().toAbsolutePath().toString()));
        saveTo.setEnabled(!OS.isUsingFlatpak());

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"), MD3Icon.of(MD3Icons.FOLDER));
        browseButton.addActionListener(e -> browseForDirectory());

        MD3Button resetButton = MD3Button.text(GetText.tr("Reset"));
        resetButton.addActionListener(e -> saveTo.setText(instance.getRoot().toAbsolutePath().toString()));

        saveTo.setToolTipText(GetText.tr("Select the folder you wish to export the instance to"));

        form.add(sectionHeading(GetText.tr("Destination")));
        form.add(sectionCard(destinationControls(saveTo, browseButton, resetButton)));

        return form;
    }

    private static MD3TextField field(String value) {
        MD3TextField field = new MD3TextField(FIELD_COLUMNS);
        field.setText(value);
        stretch(field);

        return field;
    }

    private static JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.XS, MD3Spacing.XS, MD3Spacing.XS));

        return label;
    }

    private static MD3Card sectionCard(JComponent... rows) {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(UIScale.scale(FORM_WIDTH), Integer.MAX_VALUE));

        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                card.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
            }

            card.add(rows[i]);
        }

        return card;
    }

    /**
     * Name on its own row, version and author sharing the next. Three fields in a 420dp column
     * clipped the author and everything to the right of it - the switches, Browse, Reset.
     */
    private static JPanel identityRow(MD3TextField name, MD3TextField version, MD3TextField author) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setAlignmentX(LEFT_ALIGNMENT);

        JPanel nameBlock = stackedField(GetText.tr("Name"), GetText.tr("The name of the instance"), name, false);
        JPanel versionBlock = stackedField(GetText.tr("Version"), GetText.tr("The version of this instance"), version,
                false);
        JPanel authorBlock = stackedField(GetText.tr("Author"), GetText.tr("Your name"), author, false);
        stretch(nameBlock);
        stretch(versionBlock);
        stretch(authorBlock);

        JPanel second = new JPanel();
        second.setOpaque(false);
        second.setLayout(new BoxLayout(second, BoxLayout.X_AXIS));
        second.setAlignmentX(LEFT_ALIGNMENT);
        second.add(versionBlock);
        second.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        second.add(authorBlock);
        stretch(second);

        column.add(nameBlock);
        column.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        column.add(second);
        stretch(column);

        return column;
    }

    private static JPanel stackedField(String title, String help, JComponent control, boolean showHelp) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);

        JLabel headline = new JLabel(title);
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, title));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setAlignmentX(LEFT_ALIGNMENT);
        headline.setToolTipText(help);
        block.add(headline);

        if (showHelp && help != null && !help.isEmpty()) {
            block.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
            block.add(supportingLabel(help, UIScale.scale(FORM_WIDTH - MD3Spacing.XL * 2)));
        }

        block.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));

        control.setAlignmentX(LEFT_ALIGNMENT);
        block.add(control);

        return block;
    }

    /**
     * @param summary one line under the title, already a gettext msgid of its own. Null when the
     *                title is the whole story
     * @param help    the long explanation; always the tooltip, never a third line on the row
     */
    private JPanel switchRow(String title, String summary, String help, MD3Switch toggle) {
        int textWidth = UIScale.scale(FORM_WIDTH - MD3Spacing.XL * 3);

        MD3FittingLabel headline = new MD3FittingLabel(title, 2);
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, title));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setToolTipText(help);
        headline.fitTo(textWidth);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        headline.setAlignmentX(LEFT_ALIGNMENT);
        text.add(headline);

        if (summary != null && !summary.isEmpty()) {
            JLabel supporting = supportingLabel(summary, textWidth);
            supporting.setToolTipText(help);
            text.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
            text.add(supporting);
        }

        JPanel row = new JPanel(new BorderLayout(UIScale.scale(MD3Spacing.M), 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(text, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);

        return row;
    }

    /**
     * Wraps in both scripts. Splitting on spaces used to take a Chinese sentence for one word and
     * then hide it behind the 80-character cutoff the first layout used.
     */
    private static JLabel supportingLabel(String help, int width) {
        JLabel supporting = new JLabel();
        supporting.setOpaque(false);
        supporting.setFont(MD3Type.font(MD3Type.BODY_SMALL, help));
        supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        supporting.setForeground(MD3Color.onSurfaceVariant());
        supporting.setAlignmentX(LEFT_ALIGNMENT);
        supporting.setToolTipText(help);

        FontMetrics metrics = supporting.getFontMetrics(supporting.getFont());
        supporting.setText(MD3Text.wrapToLines(metrics, help, width, SUPPORTING_LINES));

        return supporting;
    }

    private static void stretch(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
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
        // a long path must not set the row's preferred width, or Browse and Reset are clipped
        Dimension fieldPref = saveTo.getPreferredSize();
        saveTo.setPreferredSize(new Dimension(UIScale.scale(80), fieldPref.height));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(saveTo);
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        row.add(browse);
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.XS)));
        row.add(reset);
        stretch(row);

        return row;
    }

    /**
     * The folders take the leftover width and height. They used to be a wide row at the bottom of
     * a scrolling form, so a long instance hid them under the fold.
     */
    private JPanel buildFoldersColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);

        JLabel heading = new JLabel(GetText.tr("Folders To Export"));
        heading.setFont(MD3Type.font(MD3Type.TITLE_SMALL, heading.getText()));
        heading.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        heading.setForeground(MD3Color.onSurfaceVariant());

        String foldersHelp = GetText.tr("Select the folders you wish to include for this export");
        JLabel supporting = supportingLabel(foldersHelp, UIScale.scale(400));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(MD3Spacing.S, 0, MD3Spacing.S, 0));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        supporting.setAlignmentX(LEFT_ALIGNMENT);
        header.add(heading);
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));
        header.add(supporting);

        JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEADING, UIScale.scale(MD3Spacing.S), 0));
        selection.setOpaque(false);
        selection.setAlignmentX(LEFT_ALIGNMENT);

        MD3Button selectAll = MD3Button.text(GetText.tr("Select All"));
        selectAll.addActionListener(e -> setAllFolders(true));

        MD3Button selectNone = MD3Button.text(GetText.tr("Select None"));
        selectNone.addActionListener(e -> setAllFolders(false));

        selection.add(selectAll);
        selection.add(selectNone);
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        header.add(selection);

        column.add(header, BorderLayout.NORTH);
        column.add(buildFoldersList(), BorderLayout.CENTER);

        return column;
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
