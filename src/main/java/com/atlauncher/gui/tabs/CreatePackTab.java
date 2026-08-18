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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.MCVersionRow;
import com.atlauncher.data.minecraft.loaders.LoaderType;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.gui.components.LockingPreservingCaretTextSetter;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.container.MD3Table;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3TextArea;
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
import com.formdev.flatlaf.util.UIScale;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Create a vanilla instance or server.
 *
 * <p>
 * Two columns, because a 1200×700 window cannot stack an identity card, eight hundred Minecraft
 * versions and a loader picker and leave any of them usable. The form (name, description, loader)
 * sits on the leading edge at a fixed width; the version table takes every leftover pixel, which
 * is the only list on the page long enough to need them.
 *
 * <p>
 * Loader versions used to sit in a 23px combo capped at 400px. They now fill the form column and
 * size themselves to the longest value, so {@code 14.23.5.2860 (Recommended)} keeps its suffix.
 */
public class CreatePackTab extends HierarchyPanel implements Tab {
    /** Wide enough for the loader chips to wrap once, not so wide the table starves. */
    private static final int FORM_WIDTH = 360;

    private static final int DESCRIPTION_HEIGHT = 72;

    private MD3TextField nameField;
    private MD3TextArea descriptionField;
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
    private JPanel loaderVersionRow;
    private MD3Card loaderCard;
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
    private MD3Table minecraftVersionTable = null;
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
     * A heading over a group, in the same role the settings page uses for its sections.
     */
    private static JLabel sectionLabel(String text) {
        return sectionLabel(text, MD3Spacing.M);
    }

    private static JLabel sectionLabel(String text, int top) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(top, MD3Spacing.XS, MD3Spacing.S, MD3Spacing.XS));

        return label;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.BODY_LARGE, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        label.setForeground(MD3Color.onSurface());
        label.setAlignmentX(LEFT_ALIGNMENT);

        return label;
    }

    private static void stretchWidth(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
    }

    private static JPanel chipRow() {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S),
                MD3Spacing.scale(MD3Spacing.S)));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        return row;
    }

    private void setupMainPanel() {
        setOpaque(true);
        setBackground(MD3Color.surface());

        JPanel mainPanel = new JPanel(new BorderLayout(UIScale.scale(MD3Spacing.L), 0));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, 0, MD3Spacing.L));

        mainPanel.add(buildFormColumn(), BorderLayout.WEST);
        mainPanel.add(buildMinecraftColumn(), BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Name, description and loader. Fixed width so the version table's column never depends on how
     * long a Forge build string is.
     */
    private JPanel buildFormColumn() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(UIScale.scale(FORM_WIDTH), 0));
        form.setMinimumSize(new Dimension(UIScale.scale(FORM_WIDTH), 0));

        form.add(buildIdentityCard());
        form.add(buildLoaderSection());
        form.add(Box.createVerticalGlue());

        return form;
    }

    /**
     * The version list, and the chips that decide what is in it. This is the page; everything else
     * is there to describe the row you pick.
     */
    private JPanel buildMinecraftColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(sectionLabel(GetText.tr("Minecraft Version"), 0));
        header.add(buildMinecraftFilters());
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));

        column.add(header, BorderLayout.NORTH);
        column.add(buildVersionTable(), BorderLayout.CENTER);

        return column;
    }

    private MD3Card buildIdentityCard() {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        nameField.setLabel(GetText.tr("Instance Name"));
        stretchWidth(nameField);
        LockingPreservingCaretTextSetter nameFieldSetter = new LockingPreservingCaretTextSetter(nameField);
        addDisposable(viewModel.name().subscribe(it -> nameFieldSetter.setText(it.orElse(null))));
        nameField.addKeyListener(new StatefulTextKeyAdapter(
                e -> viewModel.setName(nameField.getText()),
                e -> nameFieldSetter.setLocked(true),
                e -> SwingUtilities.invokeLater(() -> nameFieldSetter.setLocked(false))));
        card.add(nameField);

        card.add(Box.createVerticalStrut(MD3Spacing.scale(MD3Spacing.M)));

        JLabel descriptionLabel = fieldLabel(GetText.tr("Description"));
        descriptionLabel.setBorder(MD3Spacing.border(0, 0, MD3Spacing.XS, 0));
        card.add(descriptionLabel);

        JComponent description = descriptionField.contained(DESCRIPTION_HEIGHT);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(DESCRIPTION_HEIGHT)));
        LockingPreservingCaretTextSetter descriptionFieldSetter = new LockingPreservingCaretTextSetter(
                descriptionField);
        addDisposable(viewModel.description().subscribe(it -> descriptionFieldSetter.setText(it.orElse(null))));
        descriptionField.addKeyListener(new StatefulTextKeyAdapter(
                e -> viewModel.setDescription(descriptionField.getText()),
                e -> descriptionFieldSetter.setLocked(true),
                e -> SwingUtilities.invokeLater(() -> descriptionFieldSetter.setLocked(false))));
        card.add(description);
        stretchWidth(card);

        return card;
    }

    private JPanel buildMinecraftFilters() {
        JPanel filters = chipRow();
        filters.setAlignmentX(LEFT_ALIGNMENT);

        setupReleaseCheckbox(filters);
        setupExperimentsCheckbox(filters);
        setupSnapshotsCheckbox(filters);
        setupOldBetasCheckbox(filters);
        setupOldAlphasCheckbox(filters);

        return filters;
    }

    private JComponent buildVersionTable() {
        setupMinecraftVersionsTable();

        MD3ListContainer versions = MD3ListContainer.wrapping(minecraftVersionTable);
        versions.setMinimumSize(new Dimension(0, UIScale.scale(160)));

        return versions;
    }

    private JPanel buildLoaderSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setBorder(MD3Spacing.border(MD3Spacing.S, 0, MD3Spacing.S, 0));

        section.add(sectionLabel(GetText.tr("Loader")));

        loaderCard = new MD3Card(MD3Card.Variant.FILLED);
        loaderCard.setLayout(new BoxLayout(loaderCard, BoxLayout.Y_AXIS));

        JPanel loaderTypePanel = chipRow();
        loaderTypePanel.setAlignmentX(LEFT_ALIGNMENT);

        setupLoaderNoneButton(loaderTypePanel);
        setupLoaderFabricButton(loaderTypePanel);
        setupLoaderForgeButton(loaderTypePanel);
        setupLoaderLegacyFabricButton(loaderTypePanel);
        setupLoaderNeoForgeButton(loaderTypePanel);
        setupLoaderPaperButton(loaderTypePanel);
        setupLoaderPurpurButton(loaderTypePanel);
        setupLoaderQuiltButton(loaderTypePanel);

        loaderCard.add(loaderTypePanel);
        loaderCard.add(buildLoaderVersionRow());

        // WrapLayout sizes itself as one long row until it has a real width. Give it the
        // form column's inner width so the card's height includes the wrapped chips and
        // the version dropdown is not clipped off.
        loaderTypePanel.setSize(UIScale.scale(FORM_WIDTH - MD3Spacing.L * 2), 1);
        stretchWidth(loaderCard);

        section.add(loaderCard);

        return section;
    }

    /**
     * Label above the combo, not beside it. The form column is only {@value #FORM_WIDTH}dp; a
     * recommended Forge build and a label cannot share that line without clipping.
     */
    private JPanel buildLoaderVersionRow() {
        loaderVersionRow = new JPanel();
        loaderVersionRow.setLayout(new BoxLayout(loaderVersionRow, BoxLayout.Y_AXIS));
        loaderVersionRow.setOpaque(false);
        loaderVersionRow.setAlignmentX(LEFT_ALIGNMENT);
        loaderVersionRow.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        JLabel loaderVersionLabel = fieldLabel(GetText.tr("Loader version"));
        loaderVersionLabel.setBorder(MD3Spacing.border(0, 0, MD3Spacing.XS, 0));
        loaderVersionRow.add(loaderVersionLabel);

        stretchWidth(loaderVersionsDropDown);
        loaderVersionRow.add(loaderVersionsDropDown);

        addDisposable(viewModel.loaderVersionsDropDownEnabled().subscribe(loaderVersionsDropDown::setEnabled));

        addDisposable(viewModel.loaderVersions().subscribe(loaderVersionsOptional -> {
            loaderVersionsDropDown.removeAllItems();

            if (!loaderVersionsOptional.isPresent()) {
                setEmpty();
            } else {
                for (LoaderVersion version : loaderVersionsOptional.get()) {
                    loaderVersionsDropDown.addItem(new ComboItem<>(version, version.toString()));
                }

                loaderVersionsDropDown.sizeToItems();
            }
        }));

        addDisposable(viewModel.selectedLoaderVersionIndex().subscribe(index -> {
            if (loaderVersionsDropDown.getItemAt(index) != null) {
                loaderVersionLastChange = System.currentTimeMillis();
                loaderVersionsDropDown.setSelectedIndex(index);
            }
        }));

        loaderVersionsDropDown.addActionListener(e -> {
            // A user cannot change the loader version in under 100 ms. It is physically
            // impossible.
            if (e.getWhen() > loaderVersionLastChange + 100) {
                @SuppressWarnings("unchecked")
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

        addDisposable(viewModel.loaderLoading().subscribe(loading -> {
            if (loading) {
                loaderVersionsDropDown.removeAllItems();
                loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("Getting Loader Versions")));
            }
        }));

        return loaderVersionRow;
    }

    private void setEmpty() {
        loaderVersionsDropDown.addItem(new ComboItem<>(null, GetText.tr("Select Loader First")));
        loaderVersionsDropDown.sizeToItems();
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

        minecraftVersionTable = new MD3Table(minecraftVersionTableModel);
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
        cm.getColumn(1).setMinWidth(UIScale.scale(112));
        cm.getColumn(1).setPreferredWidth(UIScale.scale(140));
        cm.getColumn(1).setMaxWidth(UIScale.scale(168));
        cm.getColumn(2).setResizable(false);
        cm.getColumn(2).setMinWidth(UIScale.scale(88));
        cm.getColumn(2).setPreferredWidth(UIScale.scale(104));
        cm.getColumn(2).setMaxWidth(UIScale.scale(128));
        minecraftVersionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        descriptionField = new MD3TextArea(2, 40);
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
        loaderVersionRow = null;
        loaderCard = null;
        createServerButton = null;
        createInstanceButton = null;
    }
}