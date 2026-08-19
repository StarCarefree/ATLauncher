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
package com.atlauncher.gui.tabs.accounts;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.card.AccountCard;
import com.atlauncher.gui.dialogs.LoginWithMicrosoftDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.MD3Html;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Utils;
import com.atlauncher.viewmodel.base.IAccountsViewModel;
import com.atlauncher.viewmodel.impl.AccountsViewModel;
import com.formdev.flatlaf.util.UIScale;

/**
 * The accounts you have signed in with, one card each.
 */
public class AccountsTab extends HierarchyPanel implements Tab {
    private static final long serialVersionUID = 2493791137600123223L;

    /** The onboarding text is a paragraph, so it is capped rather than run across the window. */
    private static final int READING_WIDTH = 560;

    private IAccountsViewModel viewModel;

    private JPanel accounts;
    private JScrollPane scrollPane;
    private JButton loginWithMicrosoftButton;

    public AccountsTab() {
        super(new BorderLayout());
    }

    @Override
    protected void onShow() {
        // the grid and its scroller are both transparent, so the page's own colour is what shows
        // behind them - and left to itself that is FlatLaf's panel grey rather than the Material
        // surface every other page sits on
        setOpaque(true);
        setBackground(MD3Color.surface());

        accounts = new JPanel(new CardGridLayout(AccountCard.CARD_WIDTH, AccountCard.MAX_CARD_WIDTH, MD3Spacing.L));
        accounts.setOpaque(false);
        accounts.setBorder(MD3Spacing.border(MD3Spacing.L));

        scrollPane = new JScrollPane(accounts, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(buildToolbar(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        observe();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(true);
        toolbar.setBackground(MD3Color.surface());
        // reads as the app bar's lower half, so it raises with it once the grid scrolls underneath
        toolbar.putClientProperty(MD3TopAppBar.COMPANION_KEY, true);
        toolbar.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(MD3Spacing.S), 0));
        trailing.setOpaque(false);

        // Microsoft's own sign-in artwork, which their brand terms require be used as supplied -
        // so this is the one button on the page that is not a Material one
        loginWithMicrosoftButton = new JButton();
        loginWithMicrosoftButton.setName("loginWithMicrosoft");
        loginWithMicrosoftButton.setBorderPainted(false);
        loginWithMicrosoftButton.setContentAreaFilled(false);
        loginWithMicrosoftButton.setToolTipText(GetText.tr("Sign In with Microsoft"));
        loginWithMicrosoftButton.setIcon(Utils.getIconImage(
                App.THEME.getResourcePath("image/providers", "sign-in-with-microsoft")));
        loginWithMicrosoftButton.addActionListener(e -> addAccount());

        trailing.add(loginWithMicrosoftButton);
        toolbar.add(trailing, BorderLayout.EAST);

        return toolbar;
    }

    private void addAccount() {
        // TODO This should be handled by some reaction via listener
        int numberOfAccountsBefore = viewModel.accountCount();

        LoginWithMicrosoftDialog loginWithMicrosoftDialog = new LoginWithMicrosoftDialog();
        loginWithMicrosoftDialog.setVisible(true);

        if (numberOfAccountsBefore != viewModel.accountCount()) {
            // account was added, so get the skin
            if (loginWithMicrosoftDialog.account != null) {
                loginWithMicrosoftDialog.account.updateSkin();
            }

            viewModel.pushNewAccounts();
        }
    }

    /**
     * What to show before there is an account to show. The launcher is unusable without one, so
     * this is where a new user is told what they need and where to get it.
     */
    private JPanel buildEmptyState() {
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setOpaque(false);
        empty.setBorder(MD3Spacing.border(MD3Spacing.XXL, MD3Spacing.L));

        JLabel heading = new JLabel(GetText.tr("No accounts yet"));
        heading.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        heading.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        heading.setForeground(MD3Color.onSurface());
        heading.setAlignmentX(CENTER_ALIGNMENT);
        empty.add(heading);
        empty.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));

        JEditorPane info = MD3Html.pane(new HTMLBuilder().center().text(GetText.tr(
                "In order to login and use ATLauncher modpacks, " +
                    "you must authenticate with your existing " +
                    "Minecraft/Mojang account. You must own and have paid " +
                    "for the Minecraft Java edition " +
                    "(not the Windows 10 edition) and use the same " +
                    "login here.<br><br>If you don't have an existing " +
                    "account, you can get one " +
                    "<a href=\"https://atl.pw/create-account\">by buying " +
                    "Minecraft here</a>. ATLauncher doesn't work with cracked" +
                    " accounts."))
            .build());
        info.setAlignmentX(CENTER_ALIGNMENT);
        MD3Html.wrapTo(info, UIScale.scale(READING_WIDTH));

        empty.add(info);

        return empty;
    }

    /**
     * Rebuilds the grid.
     *
     * <p>
     * The view model reports accounts as a list of names and works on whichever one is "selected",
     * a shape left from the combo box this page used to be. A card knows its own position instead,
     * and points the view model at it before doing anything - which keeps the whole contract
     * untouched while the page stops having a selection at all.
     */
    private void show(List<String> names) {
        accounts.removeAll();

        if (names.isEmpty()) {
            scrollPane.setViewportView(buildEmptyState());
            revalidate();
            repaint();

            return;
        }

        for (int i = 0; i < names.size(); i++) {
            final int index = i;

            viewModel.setSelectedAccount(index + 1);
            MicrosoftAccount account = viewModel.getSelectedAccount();

            if (account == null) {
                continue;
            }

            accounts.add(new AccountCard(account, () -> viewModel.setSelectedAccount(index + 1), actionsFor()));
        }

        scrollPane.setViewportView(accounts);
        revalidate();
        repaint();
    }

    /**
     * The actions a card offers. They act on whatever the card pointed the view model at, so one
     * instance serves every card.
     */
    private AccountCard.Actions actionsFor() {
        return new AccountCard.Actions() {
            @Override
            public void changeSkin() {
                viewModel.changeSkin();
                viewModel.pushNewAccounts();
            }

            @Override
            public void reloadSkin() {
                viewModel.updateSkin();
                viewModel.pushNewAccounts();
            }

            @Override
            public void updateUsername() {
                viewModel.updateUsername();
            }

            @Override
            public void refreshAccessToken() {
                AccountsTab.this.refreshAccessToken();
            }

            @Override
            public void delete() {
                int ret = DialogManager
                    .yesNoDialog()
                    .setTitle(GetText.tr("Delete"))
                    .setContent(GetText.tr("Are you sure you want " +
                        "to delete this account?"))
                    .setType(DialogManager.WARNING).show();

                if (ret == DialogManager.YES_OPTION) {
                    viewModel.deleteAccount();
                }
            }
        };
    }

    /**
     * Refresh the access token, and react to result
     */
    private void refreshAccessToken() {
        MicrosoftAccount account = viewModel.getSelectedAccount();
        if (account == null) {
            return;
        }

        final ProgressDialog<Boolean> dialog = new ProgressDialog<>(
            GetText.tr("Refreshing Access Token For {0}", account.minecraftUsername),
            0,
            GetText.tr("Refreshing Access Token For {0}", account.minecraftUsername),
            "Aborting refreshing access token for " + account.minecraftUsername);

        dialog.addThread(new Thread(() -> {
            boolean success = viewModel.refreshAccessToken();
            dialog.setReturnValue(success);
            dialog.close();
        }));
        dialog.start();

        boolean success = Boolean.TRUE.equals(dialog.getReturnValue());

        if (success) {
            DialogManager
                .okDialog()
                .setTitle(GetText.tr("Access Token Refreshed"))
                .setContent(
                    GetText.tr("Access token refreshed successfully"))
                .setType(DialogManager.INFO)
                .show();
        } else {
            DialogManager
                .okDialog()
                .setTitle(GetText.tr("Failed To Refresh Access Token"))
                .setContent(GetText.tr("Failed to refresh accessToken. Please login again."))
                .setType(DialogManager.ERROR)
                .show();

            LoginWithMicrosoftDialog loginWithMicrosoftDialog = new LoginWithMicrosoftDialog(account);
            loginWithMicrosoftDialog.setVisible(true);
        }

        viewModel.pushNewAccounts();
    }

    /**
     * Start observing state changes from view model
     */
    private void observe() {
        // the page no longer has a selected account of its own, but the view model still reports
        // one; taking it keeps its contract satisfied
        viewModel.onAccountSelected(account -> {
        });
        viewModel.onAccountsNamesChanged(this::show);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Accounts");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Accounts";
    }

    @Override
    protected void createViewModel() {
        viewModel = new AccountsViewModel();
    }

    @Override
    protected void onDestroy() {
        removeAll();
        accounts = null;
        scrollPane = null;
        loginWithMicrosoftButton = null;
    }
}
