/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2026 ATLauncher
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
package com.atlauncher.gui.components;

import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.JComboBox;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.FileSystem;
import com.atlauncher.data.ConsoleState;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.ConsoleStateManager;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.AccountsDropDownRenderer;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Pair;

import io.reactivex.rxjava3.core.Observable;

/**
 * The main window's top app bar.
 *
 * <p>
 * Carries what the bottom bar used to: the account picker and the three launcher-wide actions.
 * Moving them here reclaims the fifty pixel strip that ran the width of the window to hold six
 * social icons and three buttons, and puts the account - the one control a user changes mid-session -
 * where it is visible from every page rather than in the corner below the content.
 *
 * <p>
 * The action buttons keep the names the bottom bar's did, so anything looking them up by name still
 * finds them.
 */
public class LauncherAppBar extends MD3TopAppBar implements RelocalizationListener {
    private final Observable<Pair<List<MicrosoftAccount>, Optional<MicrosoftAccount>>> accountState = Observable
            .combineLatest(
                    AccountManager.getAccountsObservable(),
                    AccountManager.getSelectedAccountObservable(),
                    Pair::new);

    private final MD3IconButton toggleConsole;
    private final MD3IconButton openFolder;
    private final MD3IconButton checkForUpdates;
    private final JComboBox<MicrosoftAccount> accountSelector = new JComboBox<>();

    /** Guards against writing the account back while the list is being rebuilt. */
    private boolean rebuilding;

    public LauncherAppBar() {
        toggleConsole = addAction(MD3Icons.TERMINAL, consoleActionText(),
                e -> App.console.setVisible(!App.console.isVisible()));
        toggleConsole.setName("toggleConsole");

        openFolder = addAction(MD3Icons.FOLDER, GetText.tr("Open Folder"),
                e -> OS.openFileExplorer(FileSystem.BASE_DIR));
        openFolder.setName("openFolder");

        checkForUpdates = addAction(MD3Icons.REFRESH, GetText.tr("Check For Updates"), e -> checkForUpdates());
        checkForUpdates.setName("checkForUpdates");

        accountSelector.setName("accountSelector");
        accountSelector.setRenderer(new AccountsDropDownRenderer());
        accountSelector.setToolTipText(GetText.tr("Account"));

        for (MicrosoftAccount account : AccountManager.getAccounts()) {
            accountSelector.addItem(account);
        }

        MicrosoftAccount active = AccountManager.getSelectedAccount();

        if (active != null) {
            accountSelector.setSelectedItem(active);
        }

        accountSelector.setVisible(!AccountManager.getAccounts().isEmpty());
        accountSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !rebuilding) {
                Analytics.trackEvent(AnalyticsEvent.simpleEvent("switch_account"));
                AccountManager.switchAccount((MicrosoftAccount) accountSelector.getSelectedItem());
            }
        });

        addAction(accountSelector);

        ConsoleStateManager.getObservable().subscribe(state -> toggleConsole
                .setToolTipText(state == ConsoleState.OPEN ? GetText.tr("Hide Console") : GetText.tr("Show Console")));
        accountState.subscribe(state -> reloadAccounts(state.left(), state.right()));

        RelocalizationManager.addListener(this);
    }

    private static String consoleActionText() {
        return App.console != null && App.console.isVisible() ? GetText.tr("Hide Console") : GetText.tr("Show Console");
    }

    private void checkForUpdates() {
        ProgressDialog<Object> dialog = new ProgressDialog<>(GetText.tr("Checking For Updates"), 0,
                GetText.tr("Checking For Updates"), "Aborting Update Check!");

        dialog.addThread(new Thread(() -> {
            Analytics.trackEvent(AnalyticsEvent.simpleEvent("update_data"));
            App.launcher.updateData(true);
            dialog.close();
        }));
        dialog.start();
    }

    private void reloadAccounts(List<MicrosoftAccount> accounts, Optional<MicrosoftAccount> selected) {
        rebuilding = true;

        accountSelector.removeAllItems();

        for (MicrosoftAccount account : accounts) {
            accountSelector.addItem(account);
        }

        selected.ifPresent(accountSelector::setSelectedItem);
        accountSelector.setVisible(!accounts.isEmpty());

        rebuilding = false;
    }

    @Override
    public void onRelocalization() {
        toggleConsole.setToolTipText(consoleActionText());
        openFolder.setToolTipText(GetText.tr("Open Folder"));
        checkForUpdates.setToolTipText(GetText.tr("Check For Updates"));
        accountSelector.setToolTipText(GetText.tr("Account"));
    }
}
