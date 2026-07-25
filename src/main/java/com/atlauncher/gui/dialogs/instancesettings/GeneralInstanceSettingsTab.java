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
package com.atlauncher.gui.dialogs.instancesettings;

import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.Instance;
import com.atlauncher.data.QuickPlayOption;
import com.atlauncher.data.json.QuickPlay;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.ValidationUtils;

public class GeneralInstanceSettingsTab extends MD3SettingsList {
    /** Wide enough for a server address, without taking the whole row. */
    private static final int FIELD_COLUMNS = 20;

    private final Instance instance;

    private JComboBox<ComboItem<String>> account;

    private JComboBox<ComboItem<QuickPlayOption>> quickPlayType;

    private JTextField quickPlayServerAddress;
    private JComboBox<ComboItem<String>> quickPlaySinglePlayerWorld;
    private JTextField quickPlayRealmId;

    /**
     * The Quick Play settings, kept so the one the selected type needs can be the only one shown.
     */
    private JComponent quickPlayServerAddressRow;
    private JComponent quickPlaySinglePlayerWorldRow;
    private JComponent quickPlayRealmIdRow;

    public GeneralInstanceSettingsTab(Instance instance) {
        this.instance = instance;

        setupComponents();
    }

    private void setupComponents() {
        // Account

        account = new JComboBox<>();
        account.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
        AccountManager.getAccounts().stream()
                .forEach(a -> account.addItem(new ComboItem<>(a.username, a.minecraftUsername)));

        for (int i = 0; i < account.getItemCount(); i++) {
            ComboItem<String> item = account.getItemAt(i);

            if ((item.getValue() == null && instance.launcher.account == null)
                    || (item.getValue() != null && item.getValue().equalsIgnoreCase(instance.launcher.account))) {
                account.setSelectedIndex(i);
                break;
            }
        }

        addRow(GetText.tr("Account Override"), GetText.tr(
                "Which account to use when launching this instance. Use Launcher Default will use whichever account is selected in the launcher."),
                account);

        // Quick Play

        QuickPlay quickPlay = instance.launcher.quickPlay;

        quickPlayType = new JComboBox<>();
        Arrays.stream(QuickPlayOption.compatibleValues(instance))
                .forEach(option -> quickPlayType.addItem(new ComboItem<>(option, option.label)));
        quickPlayType.setSelectedIndex(
                Arrays.asList(QuickPlayOption.compatibleValues(instance))
                        .indexOf(quickPlay.getSelectedQuickPlayOption()));

        // Code that is responsible for changing the input
        quickPlayType.addActionListener(e -> showInputForTheSelectedQuickPlayOption());

        addRow(GetText.tr("Quick Play Type"),
                GetText.tr("Select the type of the Quick Play feature, default to disabled."), quickPlayType);

        // TODO: Allow to select the list of the servers in the game using dropdown as a
        // value for this text input
        quickPlayServerAddress = new JTextField(FIELD_COLUMNS);
        quickPlayServerAddress.putClientProperty("JTextField.showClearButton", true);
        quickPlayServerAddress.putClientProperty("JTextField.clearCallback",
                (Runnable) () -> quickPlayServerAddress.setText(""));
        quickPlayServerAddress.setText(quickPlay.serverAddress);

        quickPlayServerAddressRow = addRow(GetText.tr("Server address"),
                GetText.tr(
                        "The server address that is used to connect to Minecraft server in multiplayer after" +
                                " launching the game."),
                quickPlayServerAddress);

        quickPlaySinglePlayerWorld = new JComboBox<>();
        List<String> worldNames = instance.getSinglePlayerWorldNamesFromFilesystem();
        worldNames.forEach(saveName -> quickPlaySinglePlayerWorld.addItem(new ComboItem<>(saveName, saveName)));

        if (!worldNames.isEmpty()) {
            final int selectedWorldFolderNameIndex = worldNames.indexOf(quickPlay.worldName);
            quickPlaySinglePlayerWorld.setSelectedIndex(
                    selectedWorldFolderNameIndex != -1 ? selectedWorldFolderNameIndex : 0);
        }

        quickPlaySinglePlayerWorldRow = addRow(GetText.tr("Single Player World"),
                GetText.tr("Select the single player world to load after launching the game."),
                quickPlaySinglePlayerWorld);

        // TODO: We might want to make this as dropdown to all the realms
        quickPlayRealmId = new JTextField(FIELD_COLUMNS);
        quickPlayRealmId.putClientProperty("JTextField.showClearButton", true);
        quickPlayRealmId.putClientProperty("JTextField.clearCallback", (Runnable) () -> quickPlayRealmId.setText(""));
        quickPlayRealmId.setText(quickPlay.realmId);

        quickPlayRealmIdRow = addRow(GetText.tr("Minecraft Realm"),
                GetText.tr("Type the id of the realm to join after launching the game."), quickPlayRealmId);

        // Show only the input for the selected quick play type
        showInputForTheSelectedQuickPlayOption();
    }

    /**
     * A helper method to set the correct visibility for all the quick play inputs
     */
    private void showInputForTheSelectedQuickPlayOption() {
        QuickPlayOption quickPlayOption = ((ComboItem<QuickPlayOption>) quickPlayType.getSelectedItem()).getValue();
        switch (quickPlayOption) {
            case disabled:
                // Hide all
                setQuickPlayInputsVisibility(false, false, false);
                break;
            case multiPlayer:
                // Only show the server address input field
                setQuickPlayInputsVisibility(true, false, false);
                break;
            case singlePlayer:
                // Only show the select world dropdown
                setQuickPlayInputsVisibility(false, true, false);
                break;
            case realm:
                setQuickPlayInputsVisibility(false, false, true);
                break;
        }
    }

    /**
     * A helper method to set the visibility for different quick play inputs
     * (multiplayer, single player, realms)
     */
    private void setQuickPlayInputsVisibility(
            boolean serverAddress,
            boolean singlePlayerWorld,
            boolean realmId) {
        quickPlayServerAddressRow.setVisible(serverAddress);
        quickPlaySinglePlayerWorldRow.setVisible(singlePlayerWorld);
        quickPlayRealmIdRow.setVisible(realmId);

        // the rows are in a box layout, which lays out what it has rather than watching for a child
        // becoming visible - without this the list keeps the gap the hidden row used to fill
        revalidate();
        repaint();
    }

    public boolean isValidQuickPlayOptionValue() {
        QuickPlayOption quickPlayOption = ((ComboItem<QuickPlayOption>) quickPlayType.getSelectedItem()).getValue();
        switch (quickPlayOption) {
            case disabled:
                return true;
            case singlePlayer:
                if (quickPlaySinglePlayerWorld.getSelectedItem() == null) {
                    DialogManager.okDialog().setTitle(GetText.tr("Invalid Input"))
                            .setContent(new HTMLBuilder().center()
                                    .text(GetText.tr("You don't have any single player worlds yet on this instance."))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return false;
                }
                return true;
            case multiPlayer:
                if (quickPlayServerAddress.getText().isEmpty()) {
                    DialogManager.okDialog().setTitle(GetText.tr("Invalid Input"))
                            .setContent(new HTMLBuilder().center()
                                    .text(GetText.tr("The server address is empty."))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return false;
                }
                if (!ValidationUtils.isValidMinecraftServerAddress(quickPlayServerAddress.getText())) {
                    DialogManager.okDialog().setTitle(GetText.tr("Invalid Input"))
                            .setContent(new HTMLBuilder().center()
                                    .text(GetText.tr("The entered server address is invalid."))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return false;
                }
                return true;
            case realm:
                if (quickPlayRealmId.getText().isEmpty()) {
                    DialogManager.okDialog().setTitle(GetText.tr("Invalid Input"))
                            .setContent(new HTMLBuilder().center()
                                    .text(GetText.tr("The realm id is empty."))
                                    .build())
                            .setType(DialogManager.ERROR).show();
                    return false;
                }
                return true;
        }
        return true;
    }

    public void saveSettings() {
        this.instance.launcher.account = ((ComboItem<String>) account.getSelectedItem()).getValue();
        QuickPlayOption quickPlayOption = ((ComboItem<QuickPlayOption>) quickPlayType.getSelectedItem()).getValue();
        this.instance.launcher.quickPlay = new QuickPlay(
                quickPlayOption == QuickPlayOption.multiPlayer ? quickPlayServerAddress.getText() : null,
                quickPlayOption == QuickPlayOption.singlePlayer
                        ? ((ComboItem<String>) quickPlaySinglePlayerWorld.getSelectedItem()).getValue()
                        : null,
                quickPlayOption == QuickPlayOption.realm ? quickPlayRealmId.getText() : null);
    }

}
