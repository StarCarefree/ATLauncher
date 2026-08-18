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

import java.awt.FileDialog;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.FileSystem;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.dbus.DBusUtils;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Form;
import com.atlauncher.gui.md3.feedback.MD3WindowDialog;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.ImportPackUtils;
import com.atlauncher.utils.OS;

/**
 * Import a pack from a CurseForge / Modrinth / MultiMC file or URL.
 *
 * <p>
 * This was a 500×250 GridBag of {@code URL:} / {@code File:} labels under a centred HTML pane,
 * with Import as the only action and no way to dismiss it but the window chrome. The form is now
 * the same stacked fields the exporter uses, and Import stays off until there is something to
 * import.
 *
 * <p>
 * The two sources share one card rather than having one each. Typing in either clears the other -
 * they are alternatives, not a pair to fill in - and two separate cards said the opposite, so the
 * URL you had pasted vanished from a card that still looked like it was waiting for input.
 *
 * <p>
 * The no-argument constructor still shows the dialog, because the instances toolbar constructs
 * it for its side effect. Tests pass a parent and keep it hidden.
 */
public class ImportInstanceDialog extends MD3WindowDialog {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private final MD3TextField url;
    private final MD3TextField filePath;
    private final MD3Button importButton;

    /** Stops clearing one field from clearing the other in a loop. */
    private boolean updating;

    public ImportInstanceDialog() {
        this(ownerWindow(), true);
    }

    public ImportInstanceDialog(Window parent) {
        this(parent, false);
    }

    public ImportInstanceDialog(Window parent, boolean show) {
        super(parent, GetText.tr("Import Instance"), ModalityType.DOCUMENT_MODAL);

        url = new MD3TextField(24);
        filePath = new MD3TextField(16);
        importButton = MD3Button.filled(GetText.tr("Import"));

        setDialogSize(WIDTH, HEIGHT, 520, 360);
        setTransferHandler(new PackFileTransferHandler());

        Analytics.sendScreenView("Import Instance Dialog");

        setHeadline(GetText.tr("Import Instance"), stripBreaks(GetText.tr(
                "Select a zip/mrpack file to import it.<br/>We currently support CurseForge, Modrinth and MultiMC exported files/urls, as well as CurseForge.com links.")));
        setBody(buildBody());
        buildActions();

        if (show) {
            setVisible(true);
        }
    }

    private static Window ownerWindow() {
        return App.launcher == null ? null : App.launcher.getParent();
    }

    private JPanel buildBody() {
        listen(url, this::onUrlChanged);
        listen(filePath, this::onFileChanged);

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"), MD3Icon.of(MD3Icons.FOLDER));
        browseButton.addActionListener(e -> browseForFile());

        MD3Form form = new MD3Form(WIDTH - MD3Spacing.L * 2);
        form.addField(GetText.tr("URL"), null, url);
        form.addDivider();
        form.addField(GetText.tr("File"), GetText.tr("Modpack Export (.zip, .mrpack)"),
                MD3Form.row(filePath, browseButton));

        JPanel body = form.atTop();
        body.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        return body;
    }

    private void buildActions() {
        importButton.addActionListener(e -> startImport());
        importButton.setEnabled(false);

        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(e -> close());

        setActions(cancelButton, importButton);
        setDefaultAction(importButton);
    }

    private void onUrlChanged() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            if (!url.getText().isEmpty()) {
                filePath.setText("");
            }

            changeAddButtonStatus();
        } finally {
            updating = false;
        }
    }

    private void onFileChanged() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            if (!filePath.getText().isEmpty()) {
                url.setText("");
            }

            changeAddButtonStatus();
        } finally {
            updating = false;
        }
    }

    private void browseForFile() {
        if (OS.isUsingFlatpak()) {
            File[] filesChosen = DBusUtils.selectFiles();

            if (filesChosen.length != 0) {
                setChosenFile(filesChosen[0]);
            }

            return;
        }

        if (App.settings != null && App.settings.useNativeFilePicker) {
            FileDialog fileDialog = new FileDialog(this, GetText.tr("Select file/s"), FileDialog.LOAD);
            fileDialog.setFilenameFilter((dir, name) -> isPackFileName(name));
            fileDialog.setVisible(true);

            if (fileDialog.getFiles().length != 0) {
                setChosenFile(fileDialog.getFiles()[0]);
            }

            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(FileSystem.getUserDownloadsPath().toFile());
        chooser.setDialogTitle(GetText.tr("Select"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return GetText.tr("Modpack Export (.zip, .mrpack)");
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || isPackFileName(f.getName());
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setChosenFile(chooser.getSelectedFile());
        }
    }

    private void setChosenFile(File file) {
        if (file == null) {
            return;
        }

        filePath.setText(file.getAbsolutePath());
        changeAddButtonStatus();
    }

    private static boolean isPackFileName(String name) {
        if (name == null) {
            return false;
        }

        String lower = name.toLowerCase(Locale.ROOT);

        return lower.endsWith(".zip") || lower.endsWith(".mrpack");
    }

    private void startImport() {
        if (AccountManager.getSelectedAccount() == null) {
            DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                    .setContent(GetText.tr("Cannot create instance as you have no account selected."))
                    .setType(DialogManager.ERROR).show();

            if (AccountManager.getAccounts().isEmpty()) {
                App.navigate(UIConstants.LAUNCHER_ACCOUNTS_TAB);
            }

            return;
        }

        setVisible(false);

        final ProgressDialog<Boolean> dialog = new ProgressDialog<>(GetText.tr("Import Instance"), 0,
                GetText.tr("Import Instance"), this);

        dialog.addThread(new Thread(() -> {
            if (!url.getText().isEmpty()) {
                Analytics.trackEvent(AnalyticsEvent.forImportInstance("Url", url.getText()));
                dialog.setReturnValue(ImportPackUtils.loadFromUrl(url.getText()));
            } else if (!filePath.getText().isEmpty()) {
                Analytics.trackEvent(
                        AnalyticsEvent.forImportInstance("Archive", new File(filePath.getText()).getName()));
                dialog.setReturnValue(ImportPackUtils.loadFromFile(new File(filePath.getText())));
            } else {
                dialog.setReturnValue(false);
            }

            dialog.close();
        }));

        dialog.start();

        if (!Boolean.TRUE.equals(dialog.getReturnValue())) {
            DialogManager.okDialog().setTitle(GetText.tr("Failed To Import Instance"))
                    .setContent(new HTMLBuilder().center().text(GetText.tr(
                            "An error occured when trying to import an instance.<br/><br/>Check the console for more information."))
                            .build())
                    .setType(DialogManager.ERROR).show();
            setVisible(true);
        } else {
            close();
        }
    }

    private void changeAddButtonStatus() {
        importButton.setEnabled(!url.getText().isEmpty() || !filePath.getText().isEmpty());
    }

    private static void listen(MD3TextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.run();
            }
        });
    }

    /**
     * The supporting copy is stored with {@code <br/>} for the old HTML pane. The label now wraps
     * itself, so those tags would show as text.
     */
    private static String stripBreaks(String html) {
        return html.replace("<br/>", " ").replace("<br>", " ").replace("  ", " ").trim();
    }

    /**
     * A zip or mrpack dropped on the window is the file source. URLs still have to be pasted; a
     * dropped link is a file path on most desktops, not the CurseForge page.
     */
    private final class PackFileTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                for (File file : files) {
                    if (file != null && isPackFileName(file.getName())) {
                        setChosenFile(file);

                        return true;
                    }
                }
            } catch (Exception ignored) {
                return false;
            }

            return false;
        }
    }
}
