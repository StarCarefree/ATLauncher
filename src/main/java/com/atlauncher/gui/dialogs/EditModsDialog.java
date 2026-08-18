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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.ModUpdate;
import com.atlauncher.data.Server;
import com.atlauncher.gui.components.ModRow;
import com.atlauncher.gui.components.ModsJCheckBox;
import com.atlauncher.gui.handlers.ModsJCheckBoxTransferHandler;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.feedback.MD3WindowDialog;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3Checkbox;
import com.atlauncher.gui.md3.input.MD3FilterChip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.ModUpdateManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.ModFingerprinter;
import com.atlauncher.utils.Utils;

public class EditModsDialog extends MD3WindowDialog {
    private static final long serialVersionUID = 7004414192679481818L;

    /** How long typing has to stop for before the lists are filtered. */
    private static final int SETTLE_MS = 250;

    /** Wide enough for a mod name, and no wider - the chips need the rest of the row. */
    private static final int SEARCH_COLUMNS = 16;

    /**
     * Wide enough that the row of actions is one row: at the old 550 the last of them wrapped onto a
     * line of its own.
     */
    private static final int WIDTH = 780;
    private static final int HEIGHT = 560;

    private static final String SOURCE_CURSEFORGE = "curseforge";
    private static final String SOURCE_MODRINTH = "modrinth";
    private static final String SOURCE_MANUAL = "manual";
    private static final String UPDATABLE = "updatable";

    public final ModManagement instanceOrServer;

    private JPanel disabledModsPanel, enabledModsPanel;
    private MD3Button checkForUpdatesButton;
    private MD3Button reinstallButton;
    private MD3Button enableButton;
    private MD3Button disableButton;
    private MD3Button removeButton;
    private MD3Button refreshMetadataButton;
    private MD3Checkbox selectAllEnabledModsCheckbox, selectAllDisabledModsCheckbox;
    private ArrayList<ModsJCheckBox> enabledMods, disabledMods;

    /** How many mods each column is showing, which a filter changes under you. */
    private JLabel enabledCount, disabledCount;

    private MD3TextField searchField;

    /**
     * Keyed on the group's own name rather than on {@code com.atlauncher.data.Type}: this dialog is
     * a {@link java.awt.Window}, whose inherited {@code Window.Type} shadows the import, and the
     * grouping is coarser than the enum anyway - a texture pack and a resource pack are one row of
     * the filter.
     */
    private MD3FilterChip<String> typeChip;
    private MD3FilterChip<String> sourceChip;
    private MD3FilterChip<String> statusChip;
    private Timer settle;

    public EditModsDialog(Instance instance) {
        super(App.launcher.getParent(),
            // #. {0} is the name of the instance
            GetText.tr("Editing Mods For {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instanceOrServer = instance;

        // #. {0} is the name of the instance
        setup(GetText.tr("Editing Mods For {0}", instance.launcher.name));
    }

    public EditModsDialog(Server server) {
        super(App.launcher.getParent(),
            // #. {0} is the name of the instance
            GetText.tr("Editing Mods For {0}", server.name), ModalityType.DOCUMENT_MODAL);
        this.instanceOrServer = server;

        // #. {0} is the name of the instance
        setup(GetText.tr("Editing Mods For {0}", server.name));
    }

    private void setup(String headline) {
        setDialogSize(WIDTH, HEIGHT, 550, 420);
        setHeadline(headline);

        setupComponents();

        instanceOrServer.scanMissingMods(this);

        loadMods();
    }

    private void setupComponents() {
        Analytics.sendScreenView("Edit Mods Dialog");

        // the two lists used to be four nested JSplitPanes with their dividers disabled and sized to
        // zero - a layout, written as something the user could have dragged. Two equal columns is
        // what that was drawing, so that is what this is
        disabledModsPanel = new JPanel();
        disabledModsPanel.setLayout(new BoxLayout(disabledModsPanel, BoxLayout.Y_AXIS));
        // transparent: the container around the list draws the surface now
        disabledModsPanel.setOpaque(false);
        disabledModsPanel.setTransferHandler(new ModsJCheckBoxTransferHandler(this, true));

        enabledModsPanel = new JPanel();
        enabledModsPanel.setLayout(new BoxLayout(enabledModsPanel, BoxLayout.Y_AXIS));
        enabledModsPanel.setOpaque(false);
        enabledModsPanel.setTransferHandler(new ModsJCheckBoxTransferHandler(this, true));

        selectAllEnabledModsCheckbox = new MD3Checkbox(GetText.tr("Select All"));
        selectAllEnabledModsCheckbox.setOpaque(false);
        selectAllEnabledModsCheckbox.addActionListener(e -> {
            boolean selected = selectAllEnabledModsCheckbox.isSelected();

            enabledMods.forEach(em -> em.setSelected(selected));
        });

        selectAllDisabledModsCheckbox = new MD3Checkbox(GetText.tr("Select All"));
        selectAllDisabledModsCheckbox.setOpaque(false);
        selectAllDisabledModsCheckbox.addActionListener(e -> {
            boolean selected = selectAllDisabledModsCheckbox.isSelected();

            disabledMods.forEach(dm -> dm.setSelected(selected));
        });

        enabledCount = countLabel();
        disabledCount = countLabel();

        JPanel columns = new JPanel(new GridLayout(1, 2, MD3Spacing.scale(MD3Spacing.L), 0));
        columns.setOpaque(false);
        columns.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, 0, MD3Spacing.L));
        columns.add(buildColumn(GetText.tr("Enabled Mods"), enabledCount, selectAllEnabledModsCheckbox,
            enabledModsPanel));
        columns.add(buildColumn(GetText.tr("Disabled Mods"), disabledCount, selectAllDisabledModsCheckbox,
            disabledModsPanel));

        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.add(buildToolbar(), BorderLayout.NORTH);
        centre.add(columns, BorderLayout.CENTER);

        setBody(centre);

        // left aligned, because this is a toolbar rather than an action bar - there is nothing to
        // confirm here, the dialog is closed by its window control and every change is already made
        JPanel bottomPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S),
            MD3Spacing.scale(MD3Spacing.S)));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        setActionBar(bottomPanel);

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

        boolean platformsEnabled = instanceOrServer instanceof Server || (instanceOrServer instanceof Instance
            && ((Instance) instanceOrServer).launcher.enableCurseForgeIntegration);

        if (platformsEnabled) {
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
        }

        // the actions above act on the instance; the ones below act on whatever is ticked. They were
        // one undivided run of six text buttons, four of them greyed out until you ticked something,
        // which said nothing about why they were off
        bottomPanel.add(Box.createHorizontalStrut(MD3Spacing.scale(MD3Spacing.L)));

        enableButton = MD3Button.text(GetText.tr("Enable Selected"));
        enableButton.addActionListener(e -> enableMods());
        enableButton.setEnabled(false);
        bottomPanel.add(enableButton);

        disableButton = MD3Button.text(GetText.tr("Disable Selected"));
        disableButton.addActionListener(e -> disableMods());
        disableButton.setEnabled(false);
        bottomPanel.add(disableButton);

        if (platformsEnabled) {
            reinstallButton = MD3Button.text(GetText.tr("Reinstall"));
            reinstallButton.addActionListener(e -> reinstall());
            reinstallButton.setEnabled(false);
            bottomPanel.add(reinstallButton);
        }

        refreshMetadataButton = MD3Button.text(GetText.tr("Refresh Metadata"));
        refreshMetadataButton.addActionListener(e -> refreshMetadata());
        refreshMetadataButton.setEnabled(false);
        bottomPanel.add(refreshMetadataButton);

        // last, and in the error role: deleting mods off disk is not undoable, and it looked exactly
        // like disabling them, which is
        removeButton = MD3Button.text(GetText.tr("Remove Selected"));
        removeButton.setTone(MD3Button.Tone.ERROR);
        removeButton.addActionListener(e -> removeMods());
        removeButton.setEnabled(false);
        bottomPanel.add(removeButton);
    }

    /**
     * Search and the three facets, above both lists and filtering them together.
     *
     * <p>
     * There was nothing here at all: two lists of ticks, and a modpack with four hundred mods in it
     * gave you a scrollbar and your own eyes. Every other list in the launcher had picked up a
     * search box by now.
     *
     * <p>
     * Each slot is centred in its own wrapper because {@link FlowLayout} centres its children
     * within the tallest one in the row, not within the container - so 32dp chips beside a 40dp
     * search box sit four pixels high otherwise. Every other toolbar in the launcher hit this.
     */
    private JComponent buildToolbar() {
        searchField = MD3TextField.search(GetText.tr("Search"));
        searchField.setName("editModsSearchField");
        searchField.setColumns(SEARCH_COLUMNS);
        searchField.setLeadingIcon(MD3Icons.SEARCH);
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.clearCallback", (Runnable) () -> {
            searchField.setText("");
            applyFilters();
        });

        // filters as you type rather than on Enter: the mods are already in memory, so there was
        // never anything to wait for. The settle is what keeps a rebuild off every keystroke
        settle = new Timer(SETTLE_MS, e -> applyFilters());
        settle.setRepeats(false);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                settle.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                settle.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                settle.restart();
            }
        });

        searchField.addActionListener(e -> {
            settle.stop();
            applyFilters();
        });

        typeChip = new MD3FilterChip<>(GetText.tr("Type"), true, this::applyFilters);
        sourceChip = new MD3FilterChip<>(GetText.tr("Source"), true, this::applyFilters);
        statusChip = new MD3FilterChip<>(GetText.tr("Status"), true, this::applyFilters);

        populateFilterOptions();

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S), 0));
        filters.setOpaque(false);
        filters.setBorder(MD3Spacing.border(0, MD3Spacing.M, 0, 0));
        filters.add(typeChip.getChip());
        filters.add(sourceChip.getChip());
        filters.add(statusChip.getChip());

        // the whole slot is centred, not each chip inside it: a row of 32dp chips wrapped one by one
        // is still a 32dp row, and it was that row - not the chips in it - sitting four pixels above
        // the 40dp search box's centre line
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L, 0, MD3Spacing.L));
        toolbar.add(MD3TopAppBar.centred(searchField), BorderLayout.WEST);
        toolbar.add(MD3TopAppBar.centred(filters), BorderLayout.CENTER);

        return toolbar;
    }

    /**
     * The facets, built from what this instance actually holds - a pack with no shaders in it does
     * not need to be told it can filter by them.
     */
    private void populateFilterOptions() {
        List<ComboItem<String>> types = new ArrayList<>();
        types.add(new ComboItem<>(null, GetText.tr("All Types")));

        List<String> seen = new ArrayList<>();

        for (DisableableMod mod : instanceOrServer.getMods()) {
            if (!mod.wasSelected() || mod.skipped || mod.type == com.atlauncher.data.Type.worlds) {
                continue;
            }

            String name = ModRow.nameFor(mod.type);

            if (!seen.contains(name)) {
                seen.add(name);
                types.add(new ComboItem<>(name, name));
            }
        }

        typeChip.setOptions(types);
        typeChip.setVisible(types.size() > 2);

        List<ComboItem<String>> sources = new ArrayList<>();
        sources.add(new ComboItem<>(null, GetText.tr("All Sources")));
        sources.add(new ComboItem<>(SOURCE_CURSEFORGE, "CurseForge"));
        sources.add(new ComboItem<>(SOURCE_MODRINTH, "Modrinth"));
        sources.add(new ComboItem<>(SOURCE_MANUAL, GetText.tr("Added Manually")));

        sourceChip.setOptions(sources);

        List<ComboItem<String>> statuses = new ArrayList<>();
        statuses.add(new ComboItem<>(null, GetText.tr("Any Status")));
        statuses.add(new ComboItem<>(UPDATABLE, GetText.tr("Update Available")));

        statusChip.setOptions(statuses);
    }

    /**
     * Rebuilds both lists against the filters, keeping what was ticked.
     *
     * <p>
     * Not {@link #reloadPanels()}: that saves the instance, which a keystroke in a search box has
     * no business doing.
     */
    private void applyFilters() {
        Set<DisableableMod> ticked = selectedMods();

        enabledModsPanel.removeAll();
        disabledModsPanel.removeAll();
        loadMods();

        for (ModsJCheckBox mod : enabledMods) {
            mod.setSelected(ticked.contains(mod.getDisableableMod()));
        }

        for (ModsJCheckBox mod : disabledMods) {
            mod.setSelected(ticked.contains(mod.getDisableableMod()));
        }

        checkBoxesChanged();
        enabledModsPanel.revalidate();
        enabledModsPanel.repaint();
        disabledModsPanel.revalidate();
        disabledModsPanel.repaint();
    }

    /**
     * A pending settle would otherwise fire into a dialog that has been closed.
     */
    @Override
    public void dispose() {
        if (settle != null) {
            settle.stop();
        }

        super.dispose();
    }

    /**
     * One of the two lists: what it holds, a way to tick all of it, and the mods themselves.
     *
     * <p>
     * The select-all box used to be an unlabelled tick beside the heading, which said nothing about
     * what ticking it would do.
     *
     * <p>
     * The wheel-forwarding {@code JLayer} the scroller was once wrapped in is gone: it existed to
     * pass wheel events from a nested scroll pane to an outer one, and a list of {@code ModRow}s
     * has no nested scroll pane to forward from.
     */
    private JComponent buildColumn(String title, JLabel count, MD3Checkbox selectAll, JPanel mods) {
        JLabel label = new JLabel(title);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, title));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.primary());

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEADING, MD3Spacing.scale(MD3Spacing.S), 0));
        heading.setOpaque(false);
        heading.add(label);
        heading.add(count);

        // the heading is a line of text beside a 48dp tick box, so it needs centring in the band the
        // box sets - it sat on the box's top edge otherwise
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));
        header.add(MD3TopAppBar.centred(heading), BorderLayout.WEST);
        header.add(selectAll, BorderLayout.EAST);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(header, BorderLayout.NORTH);
        column.add(MD3ListContainer.wrapping(mods), BorderLayout.CENTER);

        return column;
    }

    /**
     * How many mods a column is showing. Numerals only - a search box that quietly drops half the
     * list needs to say so, and a bare count needs no translating to do it.
     */
    private static JLabel countLabel() {
        JLabel label = new JLabel();
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        label.setForeground(MD3Color.onSurfaceVariant());

        return label;
    }

    private void updateCounts() {
        if (enabledCount == null || disabledCount == null) {
            return;
        }

        enabledCount.setText(String.valueOf(enabledMods.size()));
        enabledCount.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, enabledCount.getText()));

        disabledCount.setText(String.valueOf(disabledMods.size()));
        disabledCount.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, disabledCount.getText()));
    }

    private void loadMods() {
        List<ModUpdate> updates = ModUpdateManager.getUpdates(instanceOrServer);

        List<DisableableMod> mods = instanceOrServer.getMods().stream().filter(DisableableMod::wasSelected)
            .filter(m -> !m.skipped && m.type != com.atlauncher.data.Type.worlds)
            .filter(m -> matchesFilters(m, updates))
            .sorted(Comparator.comparing(m -> m.name, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
        enabledMods = new ArrayList<>();
        disabledMods = new ArrayList<>();

        for (DisableableMod mod : mods) {
            // the bounds these used to be given here were overwritten by the box layout before
            // anything read them
            ModsJCheckBox checkBox = new ModsJCheckBox(mod, this);
            checkBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                    checkBoxesChanged();
                }
            });

            ModRow row = new ModRow(checkBox, updates.stream().anyMatch(u -> u.mod == mod));

            if (mod.isDisabled()) {
                disabledMods.add(checkBox);
                disabledModsPanel.add(row);
            } else {
                enabledMods.add(checkBox);
                enabledModsPanel.add(row);
            }
        }

        enabledModsPanel.setPreferredSize(new Dimension(0, heightOf(enabledModsPanel)));
        disabledModsPanel.setPreferredSize(new Dimension(0, heightOf(disabledModsPanel)));

        updateCounts();
    }

    /**
     * Whether a mod survives the search box and the three chips.
     *
     * <p>
     * The search reads the name, the filename and the description, because half of what is in a
     * mods folder is named for its jar rather than for itself, and a user looking for "the one that
     * does the minimap" has only the description to go on.
     */
    private boolean matchesFilters(DisableableMod mod, List<ModUpdate> updates) {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ENGLISH);

        if (!query.isEmpty()) {
            boolean matches = contains(mod.getName(), query) || contains(mod.getFilename(), query)
                || contains(mod.description, query);

            if (!matches) {
                return false;
            }
        }

        String type = typeChip == null ? null : typeChip.getValue();

        if (type != null && !type.equals(ModRow.nameFor(mod.type))) {
            return false;
        }

        String source = sourceChip == null ? null : sourceChip.getValue();

        if (source != null) {
            if (SOURCE_CURSEFORGE.equals(source) && !mod.isFromCurseForge()) {
                return false;
            }

            if (SOURCE_MODRINTH.equals(source) && !mod.isFromModrinth()) {
                return false;
            }

            if (SOURCE_MANUAL.equals(source) && mod.isUpdatable()) {
                return false;
            }
        }

        return !UPDATABLE.equals(statusChip == null ? null : statusChip.getValue())
            || updates.stream().anyMatch(u -> u.mod == mod);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    /**
     * How tall a list of mods is, which is what the scroll pane needs to know.
     *
     * <p>
     * Was the number of them times a hardcoded 20 - the height of a check box at 100% and at no
     * other scale or font size, so the last few mods fell off the bottom of a list that would not
     * scroll far enough to reach them. Asking the rows how tall they are is right at any scale, and
     * has to be asked of the rows rather than of the ticks inside them.
     */
    private static int heightOf(JPanel list) {
        int height = 0;

        for (Component row : list.getComponents()) {
            height += row.getPreferredSize().height;
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

            // checking is no longer scoped to the selection - it asks about the whole instance in
            // one request - so the button is offered whenever there is anything at all to ask about
            checkForUpdatesButton.setEnabled(
                enabledMods.stream().anyMatch(cb -> cb.getDisableableMod().isUpdatable())
                    || disabledMods.stream().anyMatch(cb -> cb.getDisableableMod().isUpdatable()));
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

    /**
     * Asks both platforms what is newer, once, and then offers the lot.
     *
     * <p>
     * This used to call {@link DisableableMod#checkForUpdate} in a loop, which opens a progress
     * dialog and a version selector for every mod that had one - so ticking fifty mods meant
     * clicking through a hundred dialogs. It also announced "the selected mods have been checked"
     * from the event thread <em>while</em> the worker was still running, and ignored every result,
     * so it said the same thing whether it had found anything or not.
     */
    private void checkForUpdates() {
        ProgressDialog<List<ModUpdate>> progressDialog = new ProgressDialog<>(GetText.tr("Checking For Updates"), 0,
            GetText.tr("Checking For Updates"), "Cancelled checking for mod updates", this);
        progressDialog.addThread(new Thread(() -> {
            progressDialog.setReturnValue(ModUpdateManager.checkForUpdates(instanceOrServer));
            progressDialog.close();
        }));
        progressDialog.start();

        List<ModUpdate> found = progressDialog.getReturnValue();

        if (found == null) {
            return;
        }

        // a selection narrows what is offered, but never what is checked - one bulk request costs
        // the same either way, and the answer is worth keeping for the rest of the session
        Set<DisableableMod> selected = selectedMods();

        List<ModUpdate> toOffer = selected.isEmpty() ? found
            : found.stream().filter(u -> selected.contains(u.mod)).collect(Collectors.toList());

        if (toOffer.isEmpty()) {
            ModUpdatesDialog.showNoUpdates();
            return;
        }

        if (ModUpdatesDialog.show(this, instanceOrServer, toOffer) > 0) {
            reloadPanels();
        }
    }

    /** The mods that are ticked in either list. */
    private Set<DisableableMod> selectedMods() {
        Set<DisableableMod> selected = new HashSet<>();

        for (ModsJCheckBox mod : enabledMods) {
            if (mod.isSelected()) {
                selected.add(mod.getDisableableMod());
            }
        }

        for (ModsJCheckBox mod : disabledMods) {
            if (mod.isSelected()) {
                selected.add(mod.getDisableableMod());
            }
        }

        return selected;
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
            List<DisableableMod> toRemove = new ArrayList<>();

            for (ModsJCheckBox mod : new ArrayList<>(enabledMods)) {
                if (mod.isSelected()) {
                    toRemove.add(mod.getDisableableMod());
                    enabledMods.remove(mod);
                }
            }

            for (ModsJCheckBox mod : new ArrayList<>(disabledMods)) {
                if (mod.isSelected()) {
                    toRemove.add(mod.getDisableableMod());
                    disabledMods.remove(mod);
                }
            }

            // was two copies of the remove-then-delete-the-file dance written out in place, neither
            // of which went through the interface method that exists for it
            instanceOrServer.removeMods(toRemove);

            reloadPanels();
        }
    }

    /**
     * Looks the selected mods up on both platforms again, by hashing what is on disk.
     *
     * <p>
     * This was 150 lines of fingerprinting written out in place - the third copy of it, and the one
     * carrying the author's own note about that. It also hashed the enabled path for every mod, so
     * refreshing a disabled one looked up a file that is not there.
     */
    private void refreshMetadata() {
        final ProgressDialog<Boolean> dialog = new ProgressDialog<>(GetText.tr("Refreshing Metadata"), 0,
            GetText.tr("Refreshing Metadata"),
            "Aborting refreshing metadata");
        dialog.addThread(new Thread(() -> {
            List<DisableableMod> modsToRefresh = new ArrayList<>();

            for (ModsJCheckBox mod : enabledMods) {
                if (mod.isSelected()) {
                    modsToRefresh.add(mod.getDisableableMod());
                }
            }

            for (ModsJCheckBox mod : disabledMods) {
                if (mod.isSelected()) {
                    modsToRefresh.add(mod.getDisableableMod());
                }
            }

            ModFingerprinter.identify(modsToRefresh, instanceOrServer, false);

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