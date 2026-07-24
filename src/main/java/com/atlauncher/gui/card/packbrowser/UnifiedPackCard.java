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
import java.awt.Font;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;

import java.util.ArrayList;
import java.util.List;

import com.atlauncher.gui.md3.container.MD3Badge;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.exceptions.InvalidPack;
import com.atlauncher.graphql.fragment.UnifiedModPackResultsFragment;
import com.atlauncher.graphql.type.ModPackPlatformType;
import com.atlauncher.gui.borders.IconTitledBorder;
import com.atlauncher.gui.components.BackgroundImageLabel;
import com.atlauncher.gui.components.PackImagePanel;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.managers.PackManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.Markdown;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;

public class UnifiedPackCard extends MD3PackCard implements RelocalizationListener {
    private final JButton installButton = new JButton(GetText.tr("Install"));
    private final JButton createServerButton = new JButton(GetText.tr("Create Server"));
    private final JButton websiteButton = new JButton(GetText.tr("Website"));

    public UnifiedPackCard(final UnifiedModPackResultsFragment result) {
        super();

        RelocalizationManager.addListener(this);

        // ATLauncher's own packs have their artwork on disk already; every other platform's arrives
        // over the network
        JComponent cover = null;

        if (result.platform() == ModPackPlatformType.ATLAUNCHER) {
            try {
                cover = new PackImagePanel(PackManager.getPackByID(Integer.parseInt(result.id())));
            } catch (InvalidPack | NumberFormatException e) {
                // no artwork; the card falls back to a plain container
            }
        } else {
            cover = coverFromUrl(result.iconUrl());
        }

        installButton.addActionListener(e -> {
            if (AccountManager.getSelectedAccount() == null) {
                DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                        .setContent(GetText.tr("Cannot create instance as you have no account selected."))
                        .setType(DialogManager.ERROR).show();

                if (AccountManager.getAccounts().isEmpty()) {
                    App.navigate(UIConstants.LAUNCHER_ACCOUNTS_TAB);
                }
            } else {
                Analytics.trackEvent(AnalyticsEvent.forPackInstall(result));
                InstanceInstallerDialog instanceInstallerDialog = new InstanceInstallerDialog(result, false);
                instanceInstallerDialog.setVisible(true);
            }
        });

        createServerButton.addActionListener(e -> {
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
                Analytics.trackEvent(AnalyticsEvent.forPackInstall(result, true));
                InstanceInstallerDialog instanceInstallerDialog = new InstanceInstallerDialog(result, true);
                instanceInstallerDialog.setVisible(true);
            }
        });

        boolean showCreateServerButton = result.platform() == ModPackPlatformType.MODRINTH
                || result.platform() == ModPackPlatformType.CURSEFORGE;
        if (result.platform() == ModPackPlatformType.ATLAUNCHER) {
            try {
                showCreateServerButton = PackManager.getPackByID(Integer.parseInt(result.id())).createServer;
            } catch (InvalidPack | NumberFormatException e) {
                // ignored
            }
        }
        createServerButton.setVisible(showCreateServerButton);

        websiteButton.addActionListener(e -> OS.openWebBrowser(result.url()));

        build(result.name(), cover, result.summary(), new ArrayList<>(), installButton,
                createServerButton, websiteButton);
    }

    @Override
    public void onRelocalization() {
        installButton.setText(GetText.tr("New Instance"));
        websiteButton.setText(GetText.tr("Website"));
    }
}
