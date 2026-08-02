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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.json.Mod;
import com.atlauncher.gui.components.ModsJCheckBox;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Utils;
import com.atlauncher.workers.InstanceInstaller;
import com.formdev.flatlaf.util.UIScale;

public class ModsChooser extends JDialog {
    private static final long serialVersionUID = -5309108183485463434L;

    /** Unscaled; two columns of mod names need this much before they start truncating. */
    private static final int MIN_WIDTH = 640;
    private static final int DIALOG_HEIGHT = 480;

    private final InstanceInstaller installer;
    private final MD3Button selectAllButton;
    private final MD3Button clearAllButton;
    private final MD3Button installButton;
    private List<ModsJCheckBox> modCheckboxes;
    private List<ModsJCheckBox> sortedOut;

    private boolean wasClosed = false;

    public ModsChooser(InstanceInstaller installerr) {
        super(App.launcher.getParent(), GetText.tr("Select Mods To Install"), ModalityType.DOCUMENT_MODAL);
        this.installer = installerr;

        Analytics.sendScreenView("Mods Chooser Dialog");

        setIconImage(Utils.getImage("/assets/image/icon.png"));
        setLocationRelativeTo(App.launcher.getParent());
        setLayout(new BorderLayout());
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                wasClosed = true;
                dispose();
            }
        });

        // Two columns of mods. This used to be four JSplitPanes with their dividers disabled and
        // sized to zero - one of which held nothing at all and existed only to push the headings
        // down - which is a layout written as something the user could have dragged. A grid says
        // the same thing and can be read.
        JPanel optionalMods = modList();
        JPanel requiredMods = modList();

        JPanel columns = new JPanel(new GridLayout(1, 2, MD3Spacing.scale(MD3Spacing.L), 0));
        columns.setOpaque(false);
        columns.setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, 0, MD3Spacing.L));
        columns.add(buildColumn(GetText.tr("Required Mods"), requiredMods));
        columns.add(buildColumn(GetText.tr("Optional Mods"), optionalMods));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(MD3Color.surface());
        content.add(columns, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        bottomPanel.setOpaque(true);
        bottomPanel.setBackground(MD3Color.surface());
        bottomPanel.setBorder(MD3Spacing.border(MD3Spacing.L));
        add(bottomPanel, BorderLayout.SOUTH);

        // its label depends on what the pack offers, so it is set below rather than at construction
        selectAllButton = MD3Button.outlined("");

        if (installer.hasRecommendedMods()) {
            selectAllButton.setText(GetText.tr("Select Recommended"));
        } else {
            selectAllButton.setText(GetText.tr("Select All"));
        }

        selectAllButton.addActionListener(e -> {
            for (ModsJCheckBox check : modCheckboxes) {
                if ((installer.isServer ? check.getMod().isServerOptional() : check.getMod().isOptional())) {
                    if (check.getMod().isRecommended()) {
                        if (check.getMod().hasGroup()) {
                            if (check.getMod().isRecommended() && installer.isOnlyRecommendedInGroup(check.getMod())) {
                                check.setSelected(true);
                                check.setEnabled(true);
                                sortOutMods(check);
                            } else if (installer.hasRecommendedMods()) {
                                check.setSelected(false);
                            }
                        } else {
                            check.setSelected(true);
                            check.setEnabled(true);
                            sortOutMods(check);
                        }
                    } else {
                        check.setSelected(false);
                    }
                }
            }
        });
        bottomPanel.add(selectAllButton);

        clearAllButton = MD3Button.outlined(GetText.tr("Clear All"));
        clearAllButton.addActionListener(e -> {
            for (ModsJCheckBox check : modCheckboxes) {
                if ((installer.isServer ? check.getMod().isServerOptional() : check.getMod().isOptional())) {
                    check.setSelected(false);
                    List<Mod> linkedMods = modsToChange(check.getMod());
                    for (Mod mod : linkedMods) {
                        for (ModsJCheckBox check1 : modCheckboxes) {
                            if (check1.getMod() == mod) {
                                check1.setEnabled(false);
                            }
                        }
                    }
                }
            }
        });
        bottomPanel.add(clearAllButton);

        // the one thing this dialog is for: it closes it and the install proceeds
        installButton = MD3Button.filled(GetText.tr("Install"));
        installButton.addActionListener(e -> dispose());
        bottomPanel.add(installButton);

        modCheckboxes = new ArrayList<>();

        List<Mod> orderedMods = installer.allMods.stream().sorted(Comparator.comparing(Mod::getName))
                .collect(Collectors.toList());

        for (Mod mod : orderedMods) {
            // this was a bare `continue` inside a for loop whose counter was incremented at the
            // bottom of the body: installing a server with any mod that does not go on one spun
            // forever on that mod. A for-each cannot have that bug.
            if (installer.isServer && !mod.installOnServer()) {
                continue;
            }

            ModsJCheckBox checkBox;
            if ((installer.isServer ? mod.isServerOptional() : mod.isOptional())) {
                checkBox = new ModsJCheckBox(mod);
                checkBox.setEnabled(true);
                if (mod.hasLinked()) {
                    Mod linkedMod = installer.getModByName(mod.getLinked());
                    if (linkedMod == null) {
                        LogManager.error("The mod " + mod.getName() + " tried to reference a linked mod "
                                + mod.getLinked() + " which doesn't exist!");
                        installer.cancel(true);
                        return;
                    }
                    if ((installer.isServer ? linkedMod.isServerOptional() : linkedMod.isOptional())) {
                        checkBox.setEnabled(false);
                        // a mod that depends on another is indented under it, which the hand-placed
                        // bounds used to do with a 20px x offset
                        checkBox.setBorder(MD3Spacing.border(0, MD3Spacing.XL, 0, 0));
                    }
                    if (mod.isSelected()) {
                        checkBox.setEnabled(true);
                        checkBox.setSelected(true);
                        if (!linkedMod.isSelected()) {
                            boolean needToEnableChildren = false;
                            for (ModsJCheckBox checkbox : modCheckboxes) {
                                if (checkbox.getMod().getName().equalsIgnoreCase(mod.getLinked())) {
                                    checkbox.setSelected(true); // Select the checkbox
                                    needToEnableChildren = true;
                                    break;
                                }
                            }
                            if (needToEnableChildren) {
                                for (ModsJCheckBox checkbox : modCheckboxes) {
                                    if (checkbox.getMod().getLinked().equalsIgnoreCase(mod.getLinked())) {
                                        checkbox.setEnabled(true);
                                    }
                                }
                            }
                        }
                    } else {
                        if (linkedMod.isSelected()) {
                            checkBox.setEnabled(true);
                        }
                    }
                }
                if (mod.isHidden() || mod.isLibrary()) {
                    checkBox.setVisible(false);
                }

                if (mod.hasWarning()) {
                    final ModsJCheckBox finalCheckBox = checkBox;
                    checkBox.addActionListener(e -> {
                        if (finalCheckBox.isSelected() && installer.packVersion.hasWarningMessage(mod.getWarning())) {
                            String message = installer.packVersion.getWarningMessage(mod.getWarning());

                            if (message != null) {
                                int ret = DialogManager
                                        .optionDialog().setTitle(GetText.tr("Warning")).setContent("<html>"
                                                // #. {0} is a warning for a given mod
                                                + GetText.tr(
                                                        "{0}<br/><br/>Are you sure that you want to enable this mod?",
                                                        message)
                                                + "</html>")
                                        .setType(DialogManager.WARNING).addOption(GetText.tr("Yes"))
                                        .addOption(GetText.tr("No"), true).show();

                                if (ret != 0) {
                                    finalCheckBox.setSelected(false);
                                }
                            }
                        }
                    });
                }
            } else {
                checkBox = new ModsJCheckBox(mod);
                checkBox.setSelected(true);
                checkBox.setEnabled(false);

                if (mod.isHidden() || mod.isLibrary()) {
                    checkBox.setVisible(false);
                }
            }

            if (installer.isReinstall) {
                if (!installer.wasModSelected(mod.getName())) {
                    if ((installer.isServer ? mod.isServerOptional() : mod.isOptional())) {
                        checkBox.setSelected(false);
                        checkBox.setEnabled(true);
                    }
                } else if (installer.wasModInstalled(mod.getName())) {
                    if ((installer.isServer ? mod.isServerOptional() : mod.isOptional())) {
                        checkBox.setSelected(true);
                        checkBox.setEnabled(true);
                    }
                }
            } else {
                if ((installer.isServer ? mod.isServerOptional() : mod.isOptional()) && mod.isSelected()) {
                    checkBox.setSelected(true);
                    checkBox.setEnabled(true);
                }
            }
            checkBox.addActionListener(e -> {
                ModsJCheckBox a = (ModsJCheckBox) e.getSource();
                sortOutMods(a, true);
            });
            modCheckboxes.add(checkBox);
        }

        for (ModsJCheckBox checkBox : modCheckboxes) {
            // a box layout stretches its children to the widest of them, and a checkbox stretched
            // across the column puts its label in the middle of nowhere
            checkBox.setAlignmentX(LEFT_ALIGNMENT);
            checkBox.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, checkBox.getPreferredSize().height));

            if ((installer.isServer ? checkBox.getMod().isServerOptional() : checkBox.getMod().isOptional())) {
                optionalMods.add(checkBox);
            } else {
                requiredMods.add(checkBox);
            }
        }

        sortedOut = new ArrayList<>();
        for (ModsJCheckBox cb : this.modCheckboxes) {
            if ((installer.isServer ? cb.getMod().isServerOptional() : cb.getMod().isOptional()) && cb.isSelected()) {
                sortOutMods(cb);
            }
        }

        setSize(UIScale.scale(calculateWidth()), UIScale.scale(DIALOG_HEIGHT));
        setLocationRelativeTo(App.launcher.getParent());
    }

    /**
     * A list of mods, stacked. The rows used to be positioned by hand at a hardcoded 20 pixels
     * apart on a panel with no layout manager at all, which pinned every checkbox to a height it
     * has not been since they became Material ones - and was never scaled for the display either.
     */
    private static JPanel modList() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(true);
        list.setBackground(MD3Color.surfaceContainerLow());
        list.setBorder(MD3Spacing.border(MD3Spacing.S, 0));

        return list;
    }

    /**
     * One column: what it holds, and the mods themselves.
     */
    private static JComponent buildColumn(String title, JPanel mods) {
        JLabel label = new JLabel(title);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, title));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.primary());
        label.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));

        JScrollPane scroller = new JScrollPane(mods, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(MD3Spacing.scale(MD3Spacing.XL));
        scroller.getViewport().setBackground(MD3Color.surfaceContainerLow());

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(label, BorderLayout.NORTH);
        column.add(scroller, BorderLayout.CENTER);

        return column;
    }

    /**
     * Wide enough for the action bar, and never narrower than two readable columns of mod names.
     *
     * <p>
     * Unscaled: the buttons answer in device pixels already, so it is divided back out before the
     * caller scales the whole figure.
     */
    private int calculateWidth() {
        int buttons = selectAllButton.getPreferredSize().width + clearAllButton.getPreferredSize().width
                + installButton.getPreferredSize().width;

        return Math.max(MIN_WIDTH, MD3Spacing.XXL + (int) (buttons / UIScale.getUserScaleFactor()));
    }

    private List<Mod> modsToChange(Mod mod) {
        return installer.getLinkedMods(mod);
    }

    private List<Mod> modsInGroup(Mod mod) {
        return installer.getGroupedMods(mod);
    }

    private List<Mod> modsDependancies(Mod mod) {
        return installer.getModsDependancies(mod);
    }

    private List<Mod> dependedMods(Mod mod) {
        return installer.dependedMods(mod);
    }

    private boolean hasADependancy(Mod mod) {
        return installer.hasADependancy(mod);
    }

    public void sortOutMods(ModsJCheckBox a) {
        this.sortOutMods(a, false);
    }

    public void sortOutMods(ModsJCheckBox a, boolean firstGo) {
        if (firstGo) {
            sortedOut = new ArrayList<>();
        }

        if (a.isSelected()) {
            List<Mod> linkedMods = modsToChange(a.getMod());
            for (Mod mod : linkedMods) {
                for (ModsJCheckBox check : modCheckboxes) {
                    if (check.getMod() == mod) {
                        LogManager.debug("Selected " + a.getMod().getName() + " which is auto selecting "
                                + check.getMod().getName() + " because it's a linked mod.");
                        check.setEnabled(true);
                    }
                }
            }
            if (a.getMod().hasGroup()) {
                List<Mod> groupMods = modsInGroup(a.getMod());
                for (Mod mod : groupMods) {
                    for (ModsJCheckBox check : modCheckboxes) {
                        if (check.getMod() == mod) {
                            LogManager.debug("Selected " + a.getMod().getName() + " which is auto deselecting "
                                    + check.getMod().getName() + " because it's in the same group.");
                            check.setSelected(false);
                        }
                    }
                }
            }
            if (a.getMod().hasDepends()) {
                List<Mod> dependsMods = modsDependancies(a.getMod());
                for (Mod mod : dependsMods) {
                    for (ModsJCheckBox check : modCheckboxes) {
                        if (check.getMod() == mod && !sortedOut.contains(check)) {
                            LogManager.debug("Selected " + a.getMod().getName() + " which is auto selecting "
                                    + check.getMod().getName() + " because it's a dependency.");
                            sortedOut.add(check);
                            check.setSelected(true);
                            sortOutMods(check);
                        }
                    }
                }
            }
        } else {
            List<Mod> linkedMods = modsToChange(a.getMod());
            for (Mod mod : linkedMods) {
                for (ModsJCheckBox check : modCheckboxes) {
                    if (check.getMod() == mod) {
                        LogManager.debug("Deselected " + a.getMod().getName() + " which is auto deselecting "
                                + check.getMod().getName() + " because it's a linked mod.");
                        check.setEnabled(false);
                        check.setSelected(false);
                    }
                }
            }
            if (hasADependancy(a.getMod())) {
                List<Mod> dependedMods = dependedMods(a.getMod());
                for (Mod mod : dependedMods) {
                    for (ModsJCheckBox check : modCheckboxes) {
                        if (check.getMod() == mod) {
                            LogManager.debug("Deselected " + a.getMod().getName() + " which is auto deselecting "
                                    + check.getMod().getName() + " because it's a dependant mod.");
                            check.setSelected(false);
                        }
                    }
                }
            } else if (a.getMod().hasDepends()) {
                List<Mod> dependsMods = modsDependancies(a.getMod());
                for (Mod mod : dependsMods) {
                    for (ModsJCheckBox check : modCheckboxes) {
                        if (check.getMod() == mod) {
                            if (check.getMod().isLibrary()) {
                                LogManager.debug("Deselected " + a.getMod().getName() + " which is auto deselecting "
                                        + check.getMod().getName() + " because it's a dependant library mod.");
                                check.setSelected(false);
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Mod> getSelectedMods() {
        if (wasClosed) {
            return null;
        }
        List<Mod> mods = new ArrayList<>();
        for (ModsJCheckBox check : modCheckboxes) {
            if (check.isSelected()) {
                mods.add(check.getMod());
            }
        }
        return mods;
    }

    public List<Mod> getUnselectedMods() {
        if (wasClosed) {
            return null;
        }
        List<Mod> mods = new ArrayList<>();
        for (ModsJCheckBox check : modCheckboxes) {
            if (!check.isSelected()) {
                mods.add(check.getMod());
            }
        }
        return mods;
    }

    public boolean wasClosed() {
        return this.wasClosed;
    }

}
