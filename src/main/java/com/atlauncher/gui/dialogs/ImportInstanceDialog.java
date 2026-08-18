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
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
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
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ImportPackUtils;
import com.atlauncher.utils.OS;

import com.formdev.flatlaf.util.UIScale;

/**
 * Import a pack from a CurseForge / Modrinth / MultiMC file or URL.
 *
 * <p>
 * This was a 500×250 GridBag of {@code URL:} / {@code File:} labels under a centred HTML pane,
 * with Import as the only action and no way to dismiss it but the window chrome. The form is now
 * the same stacked cards the exporter uses: a headline, the supported sources, and Cancel / Import
 * on an action bar. Import stays off until there is something to import.
 *
 * <p>
 * The no-argument constructor still shows the dialog, because the instances toolbar constructs
 * it for its side effect. Tests pass a parent and keep it hidden.
 */
public class ImportInstanceDialog extends JDialog {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private static final int HELP_LINES = 4;

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

        setupComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        if (show) {
            setVisible(true);
        }
    }

    private static Window ownerWindow() {
        return App.launcher == null ? null : App.launcher.getParent();
    }

    private void setupComponents() {
        setSize(UIScale.scale(new Dimension(WIDTH, HEIGHT)));
        setMinimumSize(UIScale.scale(new Dimension(520, 360)));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());
        setTransferHandler(new PackFileTransferHandler());

        Analytics.sendScreenView("Import Instance Dialog");

        add(buildHeadline(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeadline() {
        String title = GetText.tr("Import Instance");

        JLabel headline = new JLabel(title);
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE, title));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setAlignmentX(LEFT_ALIGNMENT);

        String help = stripBreaks(GetText.tr(
                "Select a zip/mrpack file to import it.<br/>We currently support CurseForge, Modrinth and MultiMC exported files/urls, as well as CurseForge.com links."));

        JLabel supporting = new JLabel();
        supporting.setOpaque(false);
        supporting.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, help));
        supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        supporting.setForeground(MD3Color.onSurfaceVariant());
        supporting.setAlignmentX(LEFT_ALIGNMENT);
        FontMetrics metrics = supporting.getFontMetrics(supporting.getFont());
        supporting.setText(MD3Text.wrapToLines(metrics, help, UIScale.scale(WIDTH - MD3Spacing.L * 2), HELP_LINES));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        top.add(headline);
        top.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        top.add(supporting);

        return top;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        listen(url, this::onUrlChanged);
        listen(filePath, this::onFileChanged);

        MD3Button browseButton = MD3Button.outlined(GetText.tr("Browse"), MD3Icon.of(MD3Icons.FOLDER));
        browseButton.addActionListener(e -> browseForFile());

        form.add(sectionCard(stackedField(GetText.tr("URL"), url)));
        form.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));
        form.add(sectionCard(stackedField(GetText.tr("File"), fileRow(filePath, browseButton))));
        form.add(Box.createVerticalGlue());

        return form;
    }

    private static MD3Card sectionCard(JComponent row) {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setLayout(new BorderLayout());
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(row, BorderLayout.CENTER);
        stretch(card);

        return card;
    }

    private static JPanel stackedField(String title, JComponent control) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);

        JLabel headline = new JLabel(title);
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE, title));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setAlignmentX(LEFT_ALIGNMENT);
        block.add(headline);
        block.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));

        stretch(control);
        block.add(control);

        return block;
    }

    private static JPanel fileRow(MD3TextField filePath, MD3Button browse) {
        Dimension pref = filePath.getPreferredSize();
        filePath.setPreferredSize(new Dimension(UIScale.scale(80), pref.height));
        stretch(filePath);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(filePath);
        row.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
        row.add(browse);
        stretch(row);

        return row;
    }

    private static void stretch(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
    }

    private JPanel buildActionBar() {
        importButton.addActionListener(e -> startImport());
        importButton.setEnabled(false);

        MD3Button cancelButton = MD3Button.text(GetText.tr("Cancel"));
        cancelButton.addActionListener(e -> close());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
        actions.add(cancelButton);
        actions.add(importButton);

        getRootPane().setDefaultButton(importButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(MD3Divider.inset(), BorderLayout.NORTH);
        bar.add(actions, BorderLayout.CENTER);

        return bar;
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

    private void close() {
        setVisible(false);
        dispose();
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
