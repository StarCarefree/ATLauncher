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
package com.atlauncher.gui.tabs;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.MCVersionRow;
import com.atlauncher.data.minecraft.loaders.LoaderType;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.gui.components.LockingPreservingCaretTextSetter;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.listener.StatefulTextKeyAdapter;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.viewmodel.base.ICreatePackViewModel;
import com.atlauncher.viewmodel.impl.CreatePackViewModel;
import com.formdev.flatlaf.ui.FlatScrollPaneBorder;
import com.formdev.flatlaf.util.UIScale;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class CreatePackTab extends HierarchyPanel implements Tab {
    /** Wide enough for a long instance name without the form running the width of the window. */
    private static final int FIELD_WIDTH = 520;

    private static final int DESCRIPTION_HEIGHT = 80;

    private MD3TextField nameField;
    private JTextArea descriptionField;
    private MD3Chip minecraftVersionReleasesFilterCheckbox;
    private MD3Chip minecraftVersionExperimentsFilterCheckbox;
    private MD3Chip minecraftVersionSnapshotsFilterCheckbox;
    private MD3Chip minecraftVersionBetasFilterCheckbox;
    private MD3Chip minecraftVersionAlphasFilterCheckbox;
    private MD3Chip loaderTypeNoneRadioButton;
    private MD3Chip loaderTypeFabricRadioButton;
    private MD3Chip loaderTypeForgeRadioButton;
    private MD3Chip loaderTypeLegacyFabricRadioButton;
    private MD3Chip loaderTypeNeoForgeRadioButton;
    private MD3Chip loaderTypePaperRadioButton;
    private MD3Chip loaderTypePurpurRadioButton;
    private MD3Chip loaderTypeQuiltRadioButton;
    private MD3ComboBox<ComboItem<LoaderVersion>> loaderVersionsDropDown;
    private MD3Button createServerButton;
    private MD3Button createInstanceButton;
    private ICreatePackViewModel viewModel;
    /**
     * Last time the loaderVersion has been changed.
     * <p>
     * Used to prevent an infinite changes from occuring.
     */
    private long loaderVersionLastChange = System.currentTimeMillis();
    @Nullable
    private JTable minecraftVersionTable = null;
    @Nullable
    private DefaultTableModel minecraftVersionTableModel = null;
    private boolean hasScrolledToSelection = false;
    // Guard to prevent infinite selection loop
    private boolean isUpdatingSelection = false;

    public CreatePackTab() {
        super(new BorderLayout());
        setName("createPackPanel");
    }

    private String getReleasesText() {
        return GetText.tr("Releases");
    }

    private String getExperimentsText() {
        return GetText.tr("Experiments");
    }

    private String getSnapshotsText() {
        return GetText.tr("Snapshots");
    }

    private String getBetasText() {
        return GetText.tr("Betas");
    }

    private String getAlphasText() {
        return GetText.tr("Alphas");
    }

    private String getNoneText() {
        return GetText.tr("None");
    }

    private String getCreateServerText() {
        return GetText.tr("Create Server");
    }

    private String getCreateInstanceText() {
        return GetText.tr("Create Instance");
    }

    /**
     * A label over a section of the form, in the launcher's own type scale.
     */
    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.primary());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(MD3Spacing.L, 0, MD3Spacing.S, 0));

        return label;
    }

    private static JPanel chipRow() {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S),
                MD3Spacing.scale(MD3Spacing.S)));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        return row;
    }

    private void setupMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, 0, MD3Spacing.L));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        // Name
        nameField.setLabel(GetText.tr("Instance Name"));
        nameField.setAlignmentX(LEFT_ALIGNMENT);
        nameField.setMaximumSize(new Dimension(UIScale.scale(FIELD_WIDTH), nameField.getPreferredSize().height));
        LockingPreservingCaretTextSetter nameFieldSetter = new LockingPreservingCaretTextSetter(nameField);
        addDisposable(viewModel.name().subscribe((it) -> nameFieldSetter.setText(it.orElse(null))));
        nameField.addKeyListener(new StatefulTextKeyAdapter(
            (e) -> viewModel.setName(nameField.getText()),
            (e) -> nameFieldSetter.setLocked(true),
            (e) -> SwingUtilities.invokeLater(() -> nameFieldSetter.setLocked(false))));
        header.add(nameField);

        // Description
        header.add(sectionLabel(GetText.tr("Description")));

        JScrollPane descriptionScrollPane = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionScrollPane.setBorder(new FlatScrollPaneBorder());
        descriptionScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        descriptionScrollPane.setPreferredSize(new Dimension(UIScale.scale(FIELD_WIDTH),
                UIScale.scale(DESCRIPTION_HEIGHT)));
        descriptionScrollPane.setMaximumSize(descriptionScrollPane.getPreferredSize());
        descriptionScrollPane.setViewportView(descriptionField);

        descriptionField.setLineWrap(true);
        LockingPreservingCaretTextSetter descriptionFieldSetter = new LockingPreservingCaretTextSetter(
            descriptionField);
        addDisposable(viewModel.description().subscribe((it) -> descriptionFieldSetter.setText(it.orElse(null))));
        descriptionField.addKeyListener(new StatefulTextKeyAdapter(
            (e) -> viewModel.setDescription(descriptionField.getText()),
            (e) -> descriptionFieldSetter.setLocked(true),
            (e) -> SwingUtilities.invokeLater(() -> descriptionFieldSetter.setLocked(false))));
        header.add(descriptionScrollPane);

        // Minecraft Version, and which kinds of it to list
        header.add(sectionLabel(GetText.tr("Minecraft Version")));

        JPanel minecraftVersionFilterPanel = chipRow();

        setupReleaseCheckbox(minecraftVersionFilterPanel);
        setupExperimentsCheckbox(minecraftVersionFilterPanel);
        setupSnapshotsCheckbox(minecraftVersionFilterPanel);
        setupOldBetasCheckbox(minecraftVersionFilterPanel);
        setupOldAlphasCheckbox(minecraftVersionFilterPanel);

        header.add(minecraftVersionFilterPanel);
        header.add(Box.createVerticalStrut(MD3Spacing.scale(MD3Spacing.M)));

        mainPanel.add(header, BorderLayout.NORTH);

        // the version list is what this page is mostly about, so it takes the space left over
        // rather than sitting in a 450x300 box with empty window around it
        JScrollPane minecraftVersionScrollPane = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        minecraftVersionScrollPane.setBorder(new FlatScrollPaneBorder());
        setupMinecraftVersionsTable();
        minecraftVersionScrollPane.setViewportView(minecraftVersionTable);
        mainPanel.add(minecraftVersionScrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);

        // Loader Type
        footer.add(sectionLabel(GetText.tr("Loader")));

        JPanel loaderTypePanel = chipRow();

        setupLoaderNoneButton(loaderTypePanel);
        setupLoaderFabricButton(loaderTypePanel);
        setupLoaderForgeButton(loaderTypePanel);
        setupLoaderLegacyFabricButton(loaderTypePanel);
        setupLoaderNeoForgeButton(loaderTypePanel);
        setupLoaderPaperButton(loaderTypePanel);
        setupLoaderPurpurButton(loaderTypePanel);
        setupLoaderQuiltButton(loaderTypePanel);

        footer.add(loaderTypePanel);

        // Loader Version
        JPanel loaderVersionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S), 0));
        loaderVersionPanel.setOpaque(false);
        loaderVersionPanel.setAlignmentX(LEFT_ALIGNMENT);
        loaderVersionPanel.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        JLabel loaderVersionLabel = new JLabel(GetText.tr("Loader Version") + ":");
        loaderVersionLabel.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        loaderVersionLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        loaderVersionLabel.setForeground(MD3Color.onSurfaceVariant());
        loaderVersionPanel.add(loaderVersionLabel);

        addDisposable(viewModel.loaderVersionsDropDownEnabled().subscribe(loaderVersionsDropDown::setEnabled));

        addDisposable(viewModel.loaderVersions().subscribe((loaderVersionsOptional) -> {
            loaderVersionsDropDown.removeAllItems();
            if (!loaderVersionsOptional.isPresent()) {
                setEmpty();
            } else {
                int loaderVersionLength = 0;

                List<LoaderVersion> loaderVersions = loaderVersionsOptional.get();

                for (LoaderVersion version : loaderVersions) {
                    // ensures that font width is taken into account
                    loaderVersionLength = max(
                        loaderVersionLength,
                        getFontMetrics(App.THEME.getNormalFont())
                            .stringWidth(version.toString()) + 25);

                    loaderVersionsDropDown.addItem(new ComboItem<>(version, version.toString()));
                }

                // ensures that the dropdown is at least 200 px wide
                loaderVersionLength = max(200, loaderVersionLength);

                // ensures that there is a maximum width of 400 px to prevent overflow
                loaderVersionLength = min(400, loaderVersionLength);
                loaderVersionsDropDown.setPreferredSize(new Dimension(loaderVersionLength, 23));

            }
        }));
        addDisposable(viewModel.selectedLoaderVersionIndex().subscribe((index) -> {
            if (loaderVersionsDropDown.getItemAt(index) != null) {
                loaderVersionLastChange = System.currentTimeMillis();
                loaderVersionsDropDown.setSelectedIndex(index);
            }
        }));
        loaderVersionsDropDown.addActionListener((e) -> {
            // A user cannot change the loader version in under 100 ms. It is physically
            // impossible.
            if (e.getWhen() > (loaderVersionLastChange + 100)) {
                ComboItem<LoaderVersion> comboItem = (ComboItem<LoaderVersion>) loaderVersionsDropDown
                    .getSelectedItem();

                if (comboItem != null) {
                    LoaderVersion version = comboItem.getValue();
                    if (version != null) {
                        viewModel.setLoaderVersion(version);
                    }
                }
            }
        });

        addDisposable(viewModel.loaderLoading().subscribe((it) -> {
            loaderVersionsDropDown.removeAllItems();
            if (it) {
                loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("Getting Loader Versions")));
            } else {
                setEmpty();
            }
        }));

        loaderVersionPanel.add(loaderVersionsDropDown);
        footer.add(loaderVersionPanel);

        mainPanel.add(footer, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setEmpty() {
        loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("Select Loader First")));
    }

    private void setupLoaderQuiltButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypeQuiltSelected().subscribe(loaderTypeQuiltRadioButton::setSelected));
        addDisposable(viewModel.loaderTypeQuiltEnabled().subscribe(loaderTypeQuiltRadioButton::setEnabled));
        addDisposable(viewModel.isQuiltVisible().subscribe(loaderTypeQuiltRadioButton::setVisible));
        loaderTypeQuiltRadioButton.addActionListener((e) -> viewModel.setLoaderType(LoaderType.QUILT));
        if (viewModel.showQuiltOption()) {
            loaderTypePanel.add(loaderTypeQuiltRadioButton);
        }
    }

    private void setupLoaderForgeButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypeForgeSelected().subscribe(loaderTypeForgeRadioButton::setSelected));
        addDisposable(viewModel.loaderTypeForgeEnabled().subscribe(loaderTypeForgeRadioButton::setEnabled));
        addDisposable(viewModel.isForgeVisible().subscribe(loaderTypeForgeRadioButton::setVisible));
        loaderTypeForgeRadioButton.addActionListener((e) -> viewModel.setLoaderType(LoaderType.FORGE));
        if (viewModel.showForgeOption()) {
            loaderTypePanel.add(loaderTypeForgeRadioButton);
        }
    }

    private void setupLoaderLegacyFabricButton(JPanel loaderTypePanel) {
        addDisposable(
            viewModel.loaderTypeLegacyFabricSelected().subscribe(loaderTypeLegacyFabricRadioButton::setSelected));
        addDisposable(
            viewModel.loaderTypeLegacyFabricEnabled().subscribe(loaderTypeLegacyFabricRadioButton::setEnabled));
        addDisposable(viewModel.isLegacyFabricVisible().subscribe(loaderTypeLegacyFabricRadioButton::setVisible));
        loaderTypeLegacyFabricRadioButton.addActionListener(
            e -> viewModel.setLoaderType(
                LoaderType.LEGACY_FABRIC));
        if (viewModel.showLegacyFabricOption()) {
            loaderTypePanel.add(loaderTypeLegacyFabricRadioButton);
        }
    }

    private void setupLoaderNeoForgeButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypeNeoForgeSelected().subscribe(loaderTypeNeoForgeRadioButton::setSelected));
        addDisposable(viewModel.loaderTypeNeoForgeEnabled().subscribe(loaderTypeNeoForgeRadioButton::setEnabled));
        addDisposable(viewModel.isNeoForgeVisible().subscribe(loaderTypeNeoForgeRadioButton::setVisible));
        loaderTypeNeoForgeRadioButton.addActionListener(
            e -> viewModel.setLoaderType(
                LoaderType.NEOFORGE));
        if (viewModel.showNeoForgeOption()) {
            loaderTypePanel.add(loaderTypeNeoForgeRadioButton);
        }
    }

    private void setupLoaderPaperButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypePaperSelected().subscribe(loaderTypePaperRadioButton::setSelected));
        addDisposable(viewModel.loaderTypePaperEnabled().subscribe(loaderTypePaperRadioButton::setEnabled));
        addDisposable(viewModel.isPaperVisible().subscribe(loaderTypePaperRadioButton::setVisible));
        loaderTypePaperRadioButton.addActionListener(
            e -> viewModel.setLoaderType(LoaderType.PAPER));
        // #. {0} is the name of the loader
        loaderTypePaperRadioButton.setToolTipText(new HTMLBuilder().text(GetText.tr(
            "{0} is a loader for servers that allow you to install and run plugins.<br/>You can't run mods with the {0} loader and can only be used on servers.",
            "Paper")).center().build());
        if (viewModel.showPaperOption()) {
            loaderTypePanel.add(loaderTypePaperRadioButton);
        }
    }

    private void setupLoaderPurpurButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypePurpurSelected().subscribe(loaderTypePurpurRadioButton::setSelected));
        addDisposable(viewModel.loaderTypePurpurEnabled().subscribe(loaderTypePurpurRadioButton::setEnabled));
        addDisposable(viewModel.isPurpurVisible().subscribe(loaderTypePurpurRadioButton::setVisible));
        loaderTypePurpurRadioButton.addActionListener(
            e -> viewModel.setLoaderType(LoaderType.PURPUR));
        // #. {0} is the name of the loader
        loaderTypePurpurRadioButton.setToolTipText(new HTMLBuilder().text(GetText.tr(
            "{0} is a loader for servers that allow you to install and run plugins.<br/>You can't run mods with the {0} loader and can only be used on servers.",
            "Purpur")).center().build());
        if (viewModel.showPurpurOption()) {
            loaderTypePanel.add(loaderTypePurpurRadioButton);
        }
    }

    private void setupLoaderFabricButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypeFabricSelected().subscribe(loaderTypeFabricRadioButton::setSelected));
        addDisposable(viewModel.loaderTypeFabricEnabled().subscribe(loaderTypeFabricRadioButton::setEnabled));
        addDisposable(viewModel.isFabricVisible().subscribe(loaderTypeFabricRadioButton::setVisible));
        loaderTypeFabricRadioButton.addActionListener(
            e -> viewModel.setLoaderType(
                LoaderType.FABRIC));
        if (viewModel.showFabricOption()) {
            loaderTypePanel.add(loaderTypeFabricRadioButton);
        }
    }

    private void setupLoaderNoneButton(JPanel loaderTypePanel) {
        addDisposable(viewModel.loaderTypeNoneSelected().subscribe(loaderTypeNoneRadioButton::setSelected));
        addDisposable(viewModel.loaderTypeNoneEnabled().subscribe(loaderTypeNoneRadioButton::setEnabled));
        loaderTypeNoneRadioButton.addActionListener((e) -> viewModel.setLoaderType(null));
        loaderTypePanel.add(loaderTypeNoneRadioButton);
    }

    private void setupOldAlphasCheckbox(JPanel minecraftVersionFilterPanel) {
        addDisposable(viewModel.oldAlphaSelected().subscribe(minecraftVersionAlphasFilterCheckbox::setSelected));
        addDisposable(viewModel.oldAlphaEnabled().subscribe(minecraftVersionAlphasFilterCheckbox::setEnabled));
        minecraftVersionAlphasFilterCheckbox.addActionListener(
            it -> viewModel.setOldAlphaSelected(minecraftVersionAlphasFilterCheckbox.isSelected()));
        if (viewModel.showOldAlphaOption()) {
            minecraftVersionFilterPanel.add(minecraftVersionAlphasFilterCheckbox);
        }
    }

    private void setupOldBetasCheckbox(JPanel minecraftVersionFilterPanel) {
        addDisposable(viewModel.oldBetaSelected().subscribe(minecraftVersionBetasFilterCheckbox::setSelected));
        addDisposable(viewModel.oldBetaEnabled().subscribe(minecraftVersionBetasFilterCheckbox::setEnabled));
        minecraftVersionBetasFilterCheckbox.addActionListener(
            it -> viewModel.setOldBetaSelected(minecraftVersionBetasFilterCheckbox.isSelected()));
        if (viewModel.showOldBetaOption()) {
            minecraftVersionFilterPanel.add(minecraftVersionBetasFilterCheckbox);
        }
    }

    private void setupSnapshotsCheckbox(JPanel minecraftVersionFilterPanel) {
        addDisposable(viewModel.snapshotSelected().subscribe(minecraftVersionSnapshotsFilterCheckbox::setSelected));
        addDisposable(viewModel.snapshotEnabled().subscribe(minecraftVersionSnapshotsFilterCheckbox::setEnabled));
        minecraftVersionSnapshotsFilterCheckbox.addActionListener(
            it -> viewModel.setSnapshotSelected(minecraftVersionSnapshotsFilterCheckbox.isSelected()));
        if (viewModel.showSnapshotOption()) {
            minecraftVersionFilterPanel.add(minecraftVersionSnapshotsFilterCheckbox);
        }
    }

    private void setupExperimentsCheckbox(JPanel minecraftVersionFilterPanel) {
        addDisposable(viewModel.experimentSelected().subscribe(minecraftVersionExperimentsFilterCheckbox::setSelected));
        addDisposable(viewModel.experimentEnabled().subscribe(minecraftVersionExperimentsFilterCheckbox::setEnabled));
        minecraftVersionExperimentsFilterCheckbox.addActionListener(
            it -> viewModel.setExperimentSelected(minecraftVersionExperimentsFilterCheckbox.isSelected()));
        if (viewModel.showExperimentOption()) {
            minecraftVersionFilterPanel.add(minecraftVersionExperimentsFilterCheckbox);
        }
    }

    private void setupReleaseCheckbox(JPanel minecraftVersionFilterPanel) {
        addDisposable(viewModel.releaseSelected().subscribe(minecraftVersionReleasesFilterCheckbox::setSelected));
        addDisposable(viewModel.releaseEnabled().subscribe(minecraftVersionReleasesFilterCheckbox::setEnabled));
        minecraftVersionReleasesFilterCheckbox.setSelected(true);
        minecraftVersionReleasesFilterCheckbox.addActionListener(
            it -> viewModel.setReleaseSelected(minecraftVersionReleasesFilterCheckbox.isSelected()));
        if (viewModel.showReleaseOption()) {
            minecraftVersionFilterPanel.add(minecraftVersionReleasesFilterCheckbox);
        }
    }

    @SuppressWarnings("null")
    private void setupMinecraftVersionsTable() {
        minecraftVersionTableModel = new DefaultTableModel(
            new String[][] {},
            new String[] { GetText.tr("Version"), GetText.tr("Released"), GetText.tr("Type") }) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        minecraftVersionTable = new JTable(minecraftVersionTableModel);
        minecraftVersionTable.getTableHeader().setReorderingAllowed(false);
        ListSelectionModel sm = minecraftVersionTable.getSelectionModel();
        sm.addListSelectionListener((e) -> {
            if (isUpdatingSelection || e.getValueIsAdjusting()) {
                return;
            }
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();

            int minIndex = e.getFirstIndex();
            int maxIndex = e.getLastIndex();
            for (int i = minIndex; i <= maxIndex; i++) {
                if (lsm.isSelectedIndex(i)) {
                    viewModel.setSelectedMinecraftVersion(
                        (String) minecraftVersionTableModel.getValueAt(i, 0));
                }
            }
        });

        BehaviorSubject<Boolean> isTableSetup = BehaviorSubject.createDefault(false);

        addDisposable(viewModel.minecraftVersions().subscribe((minecraftVersions) -> {
            isUpdatingSelection = true;
            // remove all rows
            int rowCount = 0;
            if (minecraftVersionTableModel != null) {
                rowCount = minecraftVersionTableModel.getRowCount();
            }

            if (rowCount > 0) {
                for (int i = rowCount - 1; i >= 0; i--) {
                    if (minecraftVersionTableModel != null) {
                        minecraftVersionTableModel.removeRow(i);
                    }
                }
            }

            for (MCVersionRow row : minecraftVersions) {
                if (minecraftVersionTableModel != null) {
                    minecraftVersionTableModel.addRow(
                        new Object[] {
                            row.id,
                            row.date,
                            row.type
                        });
                }
            }

            // refresh the table
            if (minecraftVersionTable != null) {
                minecraftVersionTable.revalidate();
            }
            isTableSetup.onNext(true);
            isUpdatingSelection = false;
        }));

        addDisposable(Observable.combineLatest(
            isTableSetup.filter(setup -> setup),
            viewModel.selectedMinecraftVersionIndex(),
            (setup, index) -> index).subscribe(it -> {
            if (minecraftVersionTable != null) {
                int rowCount = minecraftVersionTable.getRowCount();

                if (it < rowCount) {
                    minecraftVersionTable.setRowSelectionInterval(it, it);
                    minecraftVersionTable.revalidate();

                    if (!hasScrolledToSelection) {
                        Rectangle rect = minecraftVersionTable.getCellRect(it, 0, true);
                        minecraftVersionTable.scrollRectToVisible(rect);
                        hasScrolledToSelection = true;
                    }
                }
            }
        }));

        TableColumnModel cm = minecraftVersionTable.getColumnModel();
        cm.getColumn(0).setResizable(false);
        cm.getColumn(1).setResizable(false);
        cm.getColumn(1).setMaxWidth(200);
        cm.getColumn(2).setResizable(false);
        cm.getColumn(2).setMaxWidth(200);
        minecraftVersionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        minecraftVersionTable.setShowVerticalLines(false);
    }

    private void setupBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));

        bottomPanel.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottomPanel.add(actions, BorderLayout.CENTER);

        actions.add(createServerButton);
        createServerButton.addActionListener((event) -> { // user has no instances, they may not be aware this is not
            // how to play
            if (viewModel.warnUserAboutServer()) {
                int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Are you sure you want to create a server?"))
                    .setContent(
                        new HTMLBuilder().center().text(
                                GetText.tr(
                                    "Creating a server won't allow you play Minecraft, it's for letting others play together.<br/><br/>If you just want to play Minecraft, you don't want to create a server, and instead will want to create an instance.<br/><br/>Are you sure you want to create a server?"))
                            .build())
                    .setType(DialogManager.QUESTION).show();
                if (ret != 0) {
                    return;
                }
            }
            viewModel.createServer();
        });
        addDisposable(viewModel.createInstanceDisabledReason()
            .subscribe((reason) -> createInstanceButton.setToolTipText(reason.orElse(null))));
        addDisposable(viewModel.createInstanceEnabled().subscribe(createInstanceButton::setEnabled));
        addDisposable(viewModel.createServerEnabled().subscribe(createServerButton::setEnabled));
        actions.add(createInstanceButton);
        createInstanceButton.addActionListener((event) -> viewModel.createInstance());
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Create Pack");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Create Pack";
    }

    @Override
    protected void createViewModel() {
        viewModel = new CreatePackViewModel();
    }

    @Override
    protected void onShow() {
        hasScrolledToSelection = false;
        nameField = new MD3TextField(GetText.tr("Instance Name"));
        descriptionField = new JTextArea(2, 40);
        minecraftVersionReleasesFilterCheckbox = MD3Chip.filter(getReleasesText());
        minecraftVersionExperimentsFilterCheckbox = MD3Chip.filter(getExperimentsText());
        minecraftVersionSnapshotsFilterCheckbox = MD3Chip.filter(getSnapshotsText());
        minecraftVersionBetasFilterCheckbox = MD3Chip.filter(getBetasText());
        minecraftVersionAlphasFilterCheckbox = MD3Chip.filter(getAlphasText());
        loaderTypeNoneRadioButton = MD3Chip.filter(getNoneText());
        loaderTypeFabricRadioButton = MD3Chip.filter("Fabric");
        loaderTypeForgeRadioButton = MD3Chip.filter("Forge");
        loaderTypeLegacyFabricRadioButton = MD3Chip.filter("Legacy Fabric");
        loaderTypeNeoForgeRadioButton = MD3Chip.filter("NeoForge");
        loaderTypePaperRadioButton = MD3Chip.filter("Paper");
        loaderTypePurpurRadioButton = MD3Chip.filter("Purpur");
        loaderTypeQuiltRadioButton = MD3Chip.filter("Quilt");
        loaderVersionsDropDown = new MD3ComboBox<>();
        createServerButton = MD3Button.outlined(getCreateServerText());
        createInstanceButton = MD3Button.filled(getCreateInstanceText());

        setupMainPanel();
        setupBottomPanel();
    }

    @Override
    protected void onDestroy() {
        removeAll();
        nameField = null;
        descriptionField = null;
        minecraftVersionReleasesFilterCheckbox = null;
        minecraftVersionExperimentsFilterCheckbox = null;
        minecraftVersionSnapshotsFilterCheckbox = null;
        minecraftVersionBetasFilterCheckbox = null;
        minecraftVersionAlphasFilterCheckbox = null;
        loaderTypeNoneRadioButton = null;
        loaderTypeFabricRadioButton = null;
        loaderTypeForgeRadioButton = null;
        loaderTypeLegacyFabricRadioButton = null;
        loaderTypeNeoForgeRadioButton = null;
        loaderTypePaperRadioButton = null;
        loaderTypePurpurRadioButton = null;
        loaderTypeQuiltRadioButton = null;
        loaderVersionsDropDown = null;
        createServerButton = null;
        createInstanceButton = null;
    }
}