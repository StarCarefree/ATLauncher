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
package com.atlauncher.gui.card.packbrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import java.util.ArrayList;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Pack;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.components.PackImagePanel;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.gui.dialogs.ViewModsDialog;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.OS;

public class ATLauncherPackCard extends MD3PackCard implements RelocalizationListener {
    private final JButton installButton = new JButton(GetText.tr("Install"));
    private final JButton createServerButton = new JButton(GetText.tr("Create Server"));
    private final JButton discordInviteButton = new JButton("Discord");
    private final JButton supportButton = new JButton(GetText.tr("Support"));
    private final JButton websiteButton = new JButton(GetText.tr("Website"));
    private final JButton serversButton = new JButton(GetText.tr("Servers"));
    private final JButton modsButton = new JButton(GetText.tr("View Mods"));
    private final Pack pack;

    public ATLauncherPackCard(final Pack pack) {
        super();
        this.pack = pack;

        RelocalizationManager.addListener(this);

        // the visibility rules the two button rows used to encode; the overflow menu reads them
        this.discordInviteButton.setVisible(pack.getDiscordInviteURL() != null);
        this.supportButton.setVisible(pack.getSupportURL() != null);
        this.websiteButton.setVisible(pack.getWebsiteURL() != null);
        this.serversButton.setVisible(!pack.isSystem());
        this.modsButton.setVisible(!pack.isSystem() && pack.getVersionCount() != 0);
        this.createServerButton.setVisible(pack.canCreateServer());

        this.addActionListeners();

        build(pack.name, new PackImagePanel(pack), pack.getDescription(), new ArrayList<>(), installButton,
                createServerButton, modsButton, serversButton, websiteButton, supportButton, discordInviteButton);
    }

    public Pack getPack() {
        return this.pack;
    }

    private void addActionListeners() {
        this.installButton.addActionListener(e -> {
            if (AccountManager.getSelectedAccount() == null) {
                DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                        .setContent(GetText.tr("Cannot create instance as you have no account selected."))
                        .setType(DialogManager.ERROR).show();

                if (AccountManager.getAccounts().isEmpty()) {
                    App.navigate(UIConstants.LAUNCHER_ACCOUNTS_TAB);
                }
            } else {
                Analytics.trackEvent(AnalyticsEvent.forPackInstall(pack));
                InstanceInstallerDialog instanceInstallerDialog = new InstanceInstallerDialog(pack);
                instanceInstallerDialog.setVisible(true);
            }
        });

        this.createServerButton.addActionListener(e -> {
            // user has no instances, they may not be aware this is not how to play
            if (InstanceManager.getInstances().isEmpty()) {
                int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Are you sure you want to create a server?"))
                        .setContent(new HTMLBuilder().center().text(GetText.tr(
                                "Creating a server won't allow you play Minecraft, it's for letting others play together.<br/><br/>If you just want to play Minecraft, you don't want to create a server, and instead will want to create an instance.<br/><br/>Are you sure you want to create a server?"))
                                .build())
                        .setType(DialogManager.QUESTION).show();

                if (ret != 0) {
                    return;
                }
            }

            if (AccountManager.getSelectedAccount() == null) {
                DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                        .setContent(GetText.tr("Cannot create server as you have no account selected."))
                        .setType(DialogManager.ERROR).show();

                if (AccountManager.getAccounts().isEmpty()) {
                    App.navigate(UIConstants.LAUNCHER_ACCOUNTS_TAB);
                }
            } else {
                Analytics.trackEvent(AnalyticsEvent.forPackInstall(pack, true));
                InstanceInstallerDialog instanceInstallerDialog = new InstanceInstallerDialog(pack, true);
                instanceInstallerDialog.setVisible(true);
            }
        });

        this.discordInviteButton.addActionListener(e -> OS.openWebBrowser(pack.getDiscordInviteURL()));

        this.supportButton.addActionListener(e -> OS.openWebBrowser(pack.getSupportURL()));

        this.websiteButton.addActionListener(e -> OS.openWebBrowser(pack.getWebsiteURL()));

        this.serversButton.addActionListener(e -> OS
                .openWebBrowser(String.format("%s/%s?utm_source=launcher&utm_medium=button&utm_campaign=pack_button",
                        Constants.SERVERS_LIST_PACK, pack.getSafeName())));

        this.modsButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forPackEvent("pack_view_mods", pack.getName(), "ATLauncher"));
            ViewModsDialog viewModsDialog = new ViewModsDialog(pack);
            viewModsDialog.setVisible(false);
        });
    }

    @Override
    public void onRelocalization() {
        this.installButton.setText(GetText.tr("New Instance"));
        this.createServerButton.setText(GetText.tr("Create Server"));
        this.supportButton.setText(GetText.tr("Support"));
        this.websiteButton.setText(GetText.tr("Website"));
        this.serversButton.setText(GetText.tr("Servers"));
        this.modsButton.setText(GetText.tr("View Mods"));
    }
}
