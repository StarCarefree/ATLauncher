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
package com.atlauncher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.SystemTray;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Pack;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.evnt.manager.TabChangeManager;
import com.atlauncher.gui.components.LauncherAppBar;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.gui.md3.button.MD3Fab;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3NavigationRail;
import com.atlauncher.gui.md3.nav.MD3PageHost;
import com.atlauncher.gui.tabs.AboutTab;
import com.atlauncher.gui.tabs.CreatePackTab;
import com.atlauncher.gui.tabs.InstancesTab;
import com.atlauncher.gui.tabs.PacksBrowserTab;
import com.atlauncher.gui.tabs.ServersTab;
import com.atlauncher.gui.tabs.SettingsTab;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.gui.tabs.accounts.AccountsTab;
import com.atlauncher.gui.tabs.news.NewsTab;
import com.atlauncher.gui.tabs.tools.ToolsTab;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.PackManager;
import com.atlauncher.managers.PerformanceManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.utils.Utils;

import com.formdev.flatlaf.util.UIScale;

/**
 * The launcher's main window: a navigation rail, a top app bar, and one page at a time.
 *
 * <p>
 * Replaces the right-hand tab strip, whose 32pt vertical labels spent about a tenth of the window's
 * width naming nine places. The rail says the same things in 80dp, and the app bar picks up the
 * account picker and launcher-wide actions that used to sit in a fifty pixel bar along the bottom.
 *
 * <p>
 * Creating a pack is the header action rather than a rail destination: it is something you do, not
 * somewhere you go, and Material puts exactly that on the rail's header.
 *
 * <p>
 * Destinations are identified throughout by their {@link UIConstants} constant, not by their
 * position on the rail. Those constants are persisted in settings and passed to
 * {@link TabChangeManager}, so they have to stay stable however the navigation is arranged.
 */
public final class LauncherFrame extends JFrame implements RelocalizationListener {
    /** Rail order, top to bottom. -1 marks a visual break between groups. */
    private static final int[] RAIL_DESTINATIONS = {
            UIConstants.LAUNCHER_NEWS_TAB,
            UIConstants.LAUNCHER_PACKS_TAB,
            UIConstants.LAUNCHER_INSTANCES_TAB,
            UIConstants.LAUNCHER_SERVERS_TAB,
            UIConstants.LAUNCHER_ACCOUNTS_TAB,
            -1,
            UIConstants.LAUNCHER_TOOLS_TAB,
            UIConstants.LAUNCHER_SETTINGS_TAB,
            UIConstants.LAUNCHER_ABOUT_TAB };

    private final Map<Integer, Tab> tabs = new LinkedHashMap<>();
    /** Rail position to destination constant, skipping the separators. */
    private final List<Integer> railOrder = new ArrayList<>();

    private final MD3NavigationRail rail = new MD3NavigationRail();
    private final LauncherAppBar appBar = new LauncherAppBar();
    private final MD3PageHost content = new MD3PageHost();

    private int selectedDestination = -1;
    /** Stops the rail's own change event from re-entering the navigation it just caused. */
    private boolean navigating;

    public LauncherFrame(boolean show) {
        LogManager.info("Launcher opening");
        LogManager.info("Made By Bob*");
        LogManager.info("*(Not Actually)");

        App.launcher.setParentFrame(this);
        setTitle(Constants.LAUNCHER_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());
        setIconImage(Utils.getImage("/assets/image/icon.png"));

        setMinimumSize(UIScale.scale(new Dimension(1200, 700)));
        setLocationRelativeTo(null);

        restoreWindowBounds();

        LogManager.info("Setting up Tabs");
        createTabs();
        buildNavigation();
        LogManager.info("Finished Setting up Tabs");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(appBar, BorderLayout.NORTH);
        body.add(content, BorderLayout.CENTER);

        add(rail, BorderLayout.WEST);
        add(body, BorderLayout.CENTER);

        navigateTo(App.settings.selectedTabOnStartup);

        if (show) {
            LogManager.info("Showing Launcher");
            setVisible(true);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent windowEvent) {
                    try {
                        if (SystemTray.isSupported()) {
                            SystemTray.getSystemTray().remove(App.trayIcon);
                        }
                    } catch (Exception ignored) {
                        // ignored
                    }
                }
            });
        }

        RelocalizationManager.addListener(this);

        installRequestedPack();
        rememberWindowBounds();
    }

    private void restoreWindowBounds() {
        try {
            if (App.settings.rememberWindowSizePosition && App.settings.launcherSize != null) {
                setSize(App.settings.launcherSize);
            }

            if (App.settings.rememberWindowSizePosition && App.settings.launcherPosition != null) {
                setLocation(App.settings.launcherPosition);
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error setting custom remembered window size settings", e);
        }
    }

    private void rememberWindowBounds() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.launcherSize = c.getSize();
                    App.settings.save();
                }
            }

            @Override
            public void componentMoved(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.launcherPosition = c.getLocation();
                    App.settings.save();
                }
            }
        });
    }

    private void installRequestedPack() {
        if (App.packToInstall == null) {
            return;
        }

        Pack pack = PackManager.getPackBySafeName(App.packToInstall);

        if (pack != null && pack.isSemiPublic() && !PackManager.canViewSemiPublicPackByCode(pack.getCode())) {
            LogManager.error("Error automatically installing " + pack.getName() + " as you don't have the "
                    + "pack added to the launcher!");

            return;
        }

        if (AccountManager.getSelectedAccount() == null || pack == null) {
            LogManager.error("Error automatically installing " + (pack == null ? "pack" : pack.getName()) + "!");

            return;
        }

        InstanceInstallerDialog instanceInstallerDialog = new InstanceInstallerDialog(pack);
        instanceInstallerDialog.setVisible(true);
    }

    private void createTabs() {
        addTab(UIConstants.LAUNCHER_NEWS_TAB, "newsTab", new NewsTab());
        addTab(UIConstants.LAUNCHER_CREATE_PACK_TAB, "createPackTab", new CreatePackTab());

        PerformanceManager.start("packsBrowserTab");
        PacksBrowserTab packsBrowserTab = new PacksBrowserTab();
        tabs.put(UIConstants.LAUNCHER_PACKS_TAB, packsBrowserTab);
        App.launcher.setPacksBrowserPanel(packsBrowserTab);
        PerformanceManager.end("packsBrowserTab");

        addTab(UIConstants.LAUNCHER_INSTANCES_TAB, "instancesTab", new InstancesTab());
        addTab(UIConstants.LAUNCHER_SERVERS_TAB, "serversTab", new ServersTab());
        addTab(UIConstants.LAUNCHER_ACCOUNTS_TAB, "accountsTab", new AccountsTab());
        addTab(UIConstants.LAUNCHER_TOOLS_TAB, "toolsTab", new ToolsTab());
        addTab(UIConstants.LAUNCHER_SETTINGS_TAB, "settingsTab", new SettingsTab());
        addTab(UIConstants.LAUNCHER_ABOUT_TAB, "aboutTab", new AboutTab());
    }

    private void addTab(int destination, String timing, Tab tab) {
        PerformanceManager.start(timing);
        tabs.put(destination, tab);
        PerformanceManager.end(timing);
    }

    private void buildNavigation() {
        for (Map.Entry<Integer, Tab> entry : tabs.entrySet()) {
            content.addPage((JPanel) entry.getValue(), String.valueOf(entry.getKey()));
        }

        MD3Fab create = new MD3Fab(MD3Icons.ADD, tabs.get(UIConstants.LAUNCHER_CREATE_PACK_TAB).getTitle());
        create.setName("createPackAction");
        create.addActionListener(e -> navigateTo(UIConstants.LAUNCHER_CREATE_PACK_TAB));
        rail.setHeader(create);

        for (int destination : RAIL_DESTINATIONS) {
            if (destination < 0) {
                rail.addSeparator();

                continue;
            }

            rail.addDestination(iconFor(destination), tabs.get(destination).getTitle())
                    .setName("nav." + destination);
            railOrder.add(destination);
        }

        rail.setName("mainNavigation");
        rail.addChangeListener(e -> {
            if (!navigating) {
                navigateTo(railOrder.get(rail.getSelectedIndex()));
            }
        });
    }

    private static MD3Icon.Painter iconFor(int destination) {
        switch (destination) {
            case UIConstants.LAUNCHER_NEWS_TAB:
                return MD3Icons.HOME;
            case UIConstants.LAUNCHER_PACKS_TAB:
                return MD3Icons.SEARCH;
            case UIConstants.LAUNCHER_INSTANCES_TAB:
                return MD3Icons.PACKAGE;
            case UIConstants.LAUNCHER_SERVERS_TAB:
                return MD3Icons.LIST_VIEW;
            case UIConstants.LAUNCHER_ACCOUNTS_TAB:
                return MD3Icons.PERSON;
            case UIConstants.LAUNCHER_TOOLS_TAB:
                return MD3Icons.TUNE;
            case UIConstants.LAUNCHER_SETTINGS_TAB:
                return MD3Icons.SETTINGS;
            case UIConstants.LAUNCHER_ABOUT_TAB:
            default:
                return MD3Icons.INFO;
        }
    }

    /**
     * Shows a destination, identified by its {@link UIConstants} constant.
     */
    public void navigateTo(int destination) {
        Tab tab = tabs.get(destination);

        if (tab == null || destination == selectedDestination) {
            return;
        }

        selectedDestination = destination;

        navigating = true;

        try {
            content.showPage(String.valueOf(destination));
            // create-pack is reached from the header, so it has no rail position to light up
            rail.setSelectedIndex(railOrder.indexOf(destination));
            appBar.setTitle(tab.getTitle());
        } finally {
            navigating = false;
        }

        Analytics.sendScreenView(tab.getAnalyticsScreenViewName());
        TabChangeManager.post(destination);
    }

    public int getSelectedDestination() {
        return selectedDestination;
    }

    @Override
    public void onRelocalization() {
        for (int i = 0; i < railOrder.size(); i++) {
            rail.setLabelAt(i, tabs.get(railOrder.get(i)).getTitle());
        }

        Tab selected = tabs.get(selectedDestination);

        if (selected != null) {
            appBar.setTitle(selected.getTitle());
        }
    }
}
