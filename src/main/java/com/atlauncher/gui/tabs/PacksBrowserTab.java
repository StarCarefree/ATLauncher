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
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.listener.TabChangeListener;
import com.atlauncher.evnt.listener.ThemeListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.evnt.manager.TabChangeManager;
import com.atlauncher.evnt.manager.ThemeManager;
import com.atlauncher.gui.WheelScrollLayerUI;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.panels.packbrowser.ATLauncherPacksPanel;
import com.atlauncher.gui.panels.packbrowser.CurseForgePacksPanel;
import com.atlauncher.gui.panels.packbrowser.FTBPacksPanel;
import com.atlauncher.gui.panels.packbrowser.ModrinthPacksPanel;
import com.atlauncher.gui.panels.packbrowser.PackBrowserPlatformPanel;
import com.atlauncher.gui.panels.packbrowser.TechnicPacksPanel;
import com.atlauncher.gui.panels.packbrowser.UnifiedPacksPanel;
import com.atlauncher.gui.tabs.packbrowser.PacksNavigationPanel;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.MinecraftManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.Utils;
import com.formdev.flatlaf.util.UIScale;

public final class PacksBrowserTab extends JPanel
    implements Tab, RelocalizationListener, ThemeListener, TabChangeListener, PacksNavigationPanel.Listener {
    private final PacksNavigationPanel navigationPanel = new PacksNavigationPanel(this);

    private final JPanel platformMessageJPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
        MD3Spacing.scale(MD3Spacing.S), MD3Spacing.scale(MD3Spacing.S)));
    private final JLabel platformMessageJLabel = new JLabel();

    /**
     * Which platform is being browsed. A tab rather than a rail: the rail already carries the
     * launcher's nine destinations, and a second vertical strip beside it read as a second
     * navigation rather than as a filter on this one.
     */
    private final MD3Tabs platformTabs = new MD3Tabs();
    private final CardLayout platformLayout = new CardLayout();
    private final JPanel platformHost = new JPanel(platformLayout);
    private final List<PackBrowserPlatformPanel> platforms = new ArrayList<>();

    private final PackBrowserPlatformPanel unifiedPacksPanel = new UnifiedPacksPanel();
    private final PackBrowserPlatformPanel atlauncherPacksPanel = new ATLauncherPacksPanel();
    private final PackBrowserPlatformPanel curseForgePacksPanel = new CurseForgePacksPanel();
    private final PackBrowserPlatformPanel ftbPacksPanel = new FTBPacksPanel();
    private final PackBrowserPlatformPanel modrinthPacksPanel = new ModrinthPacksPanel();
    private final PackBrowserPlatformPanel technicPacksPanel = new TechnicPacksPanel();

    private JScrollPane scrollPane;
    private JLayer<JScrollPane> layerForScrollPane;
    private final JPanel contentPanel = new JPanel();

    private boolean loaded = false;
    private boolean loading = false;
    private int page = 1;

    private Timer tabsEnabledTimer = null;

    public PacksBrowserTab() {
        super(new BorderLayout());
        setName("packsBrowserPanel");
        RelocalizationManager.addListener(this);
        ThemeManager.addListener(this);

        initComponents();
    }

    private void initComponents() {
        // content panel

        // cards form a grid that reflows with the window rather than one full-width card per row
        contentPanel.setLayout(new WrapLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.L),
                UIScale.scale(MD3Spacing.L)));
        contentPanel.setBorder(MD3Spacing.border(MD3Spacing.L));

        // platform message panel

        platformMessageJPanel.setOpaque(true);
        platformMessageJPanel.setBackground(MD3Color.tertiaryContainer());
        platformMessageJLabel.setForeground(MD3Color.onTertiaryContainer());
        platformMessageJLabel.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        platformMessageJLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        platformMessageJLabel.setIcon(MD3Icon.of(MD3Icons.INFO).withColor(MD3Color.onTertiaryContainer()));
        platformMessageJLabel.setIconTextGap(MD3Spacing.scale(MD3Spacing.S));
        platformMessageJPanel.add(platformMessageJLabel);

        // scrollpane

        scrollPane = new JScrollPane(contentPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

            if (!loading && selectedPanel != null && selectedPanel.hasPagination() && selectedPanel.hasMorePages()) {
                int maxValue = scrollPane.getVerticalScrollBar().getMaximum()
                    - scrollPane.getVerticalScrollBar().getVisibleAmount();
                int currentValue = scrollPane.getVerticalScrollBar().getValue();

                if ((float) currentValue / (float) maxValue > 0.9f) {
                    loadMorePacks();
                }
            }
        });

        layerForScrollPane = new JLayer<>(scrollPane, new WheelScrollLayerUI());

        // platforms

        addPlatform(unifiedPacksPanel, GetText.tr("Search"), null);
        addPlatform(atlauncherPacksPanel, "ATLauncher", "atlauncher");

        if (ConfigManager.getConfigItem("platforms.curseforge.modpacksEnabled", true)) {
            addPlatform(curseForgePacksPanel, "CurseForge", "curseforge");
        }

        if (ConfigManager.getConfigItem("platforms.ftb.modpacksEnabled", true)) {
            addPlatform(ftbPacksPanel, "FTB", "ftb");
        }

        if (ConfigManager.getConfigItem("platforms.modrinth.modpacksEnabled", true)) {
            addPlatform(modrinthPacksPanel, "Modrinth", "modrinth");
        }

        if (ConfigManager.getConfigItem("platforms.technic.modpacksEnabled", true)) {
            addPlatform(technicPacksPanel, "Technic", "technic");
        }

        platformTabs.addChangeListener(e -> {
            PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

            if (selectedPanel == null) {
                return;
            }

            platformLayout.show(platformHost, selectedPanel.getPlatformName());

            // send analytics page view
            if (selectedPanel.getPlatformName().equals("UnifiedModPackSearch")) {
                Analytics.sendScreenView("Unified ModPack Search");
            } else {
                Analytics.sendScreenView(selectedPanel.getPlatformName() + " Platform Packs");
            }

            afterTabChange();
            System.gc();
        });

        TabChangeManager.addListener(this);

        // the header stacks what is being browsed above how it is being filtered, since the
        // filters only mean anything once a platform has been chosen
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(platformTabs);
        header.add(navigationPanel);

        add(header, BorderLayout.NORTH);
        add(platformHost, BorderLayout.CENTER);
    }

    private void addPlatform(PackBrowserPlatformPanel panel, String label, String iconName) {
        platforms.add(panel);
        platformHost.add(panel, panel.getPlatformName());
        platformTabs.addTab(label, platformIcon(iconName)).setName("packsPlatform." + label);
    }

    /**
     * A platform's own mark, which is how it is recognised - so unlike the launcher's own icons
     * these are loaded rather than drawn, and are never recoloured.
     */
    private Icon platformIcon(String iconName) {
        if (iconName == null) {
            return MD3Icon.of(MD3Icons.SEARCH, MD3Spacing.ICON_SIZE_LARGE);
        }

        return Utils.getIconImage(App.THEME.getResourcePath("image/modpack-platform", iconName));
    }

    private PackBrowserPlatformPanel getSelectedPanel() {
        int index = platformTabs.getSelectedIndex();

        return index < 0 || index >= platforms.size() ? null : platforms.get(index);
    }

    private void disableTabsWhileLoading() {
        if (tabsEnabledTimer != null && tabsEnabledTimer.isRunning()) {
            tabsEnabledTimer.stop();
        }

        setBrowsingEnabled(false);
        navigationPanel.setLoading(true);

        tabsEnabledTimer = new Timer(30000, e2 -> {
            setBrowsingEnabled(true);
            navigationPanel.setLoading(false);
        });
        tabsEnabledTimer.setRepeats(false);
        tabsEnabledTimer.start();
    }

    private void enableTabsAfterLoading() {
        if (tabsEnabledTimer != null && tabsEnabledTimer.isRunning()) {
            tabsEnabledTimer.stop();
        }

        setBrowsingEnabled(true);
        navigationPanel.setLoading(false);
    }

    /**
     * The filters go with the tabs: a request is already in flight, and changing its terms
     * mid-flight only means the results that arrive do not match what the toolbar says.
     */
    private void setBrowsingEnabled(boolean enabled) {
        platformTabs.setEnabled(enabled);
        navigationPanel.setFiltersEnabled(enabled);
    }

    private void afterTabChange() {
        // add the scrollPane to the newly selected panel
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel == null) {
            return;
        }

        selectedPanel.add(platformMessageJPanel, BorderLayout.NORTH);
        selectedPanel.add(layerForScrollPane, BorderLayout.CENTER);

        // clear search
        navigationPanel.setSearch("");

        // reset page
        loading = true;
        page = 1;

        // disable the tabs
        disableTabsWhileLoading();

        // remove minecraft version, category and sort values
        navigationPanel.setMinecraftVersions(new ArrayList<>());
        navigationPanel.clearCategories();
        navigationPanel.setSortFields(new ArrayList<>());

        // add in minecraft versions if the platform supports it
        if (selectedPanel.supportsMinecraftVersionFiltering()) {
            List<ComboItem<String>> versions = new ArrayList<>();
            versions.add(new ComboItem<>(null, GetText.tr("All Versions")));

            List<VersionManifestVersion> versionsToShow = !selectedPanel
                .getSupportedMinecraftVersionsForFiltering().isEmpty()
                ? selectedPanel.getSupportedMinecraftVersionsForFiltering()
                : MinecraftManager
                .getFilteredMinecraftVersions(
                    selectedPanel.getSupportedMinecraftVersionTypesForFiltering());

            for (VersionManifestVersion mv : versionsToShow) {
                if (mv != null) {
                    versions.add(new ComboItem<>(mv.id, mv.id));
                }
            }

            navigationPanel.setMinecraftVersions(versions);
        }

        // add in categories if the platform supports it
        if (selectedPanel.hasCategories()) {
            new Thread(() -> {
                navigationPanel.addCategory(new ComboItem<>(null, GetText.tr("All Categories")));

                for (Map.Entry<String, String> entry : selectedPanel.getCategoryFields().entrySet()) {
                    navigationPanel.addCategory(new ComboItem<>(entry.getKey(), entry.getValue()));
                }
            }).start();
        }

        // add in sort fields if the platform supports it
        if (selectedPanel.hasSort()) {
            List<ComboItem<String>> sorts = new ArrayList<>();

            for (Map.Entry<String, String> entry : selectedPanel.getSortFields().entrySet()) {
                sorts.add(new ComboItem<>(entry.getKey(), entry.getValue()));
            }

            navigationPanel.setSortFields(sorts);
        }

        if (selectedPanel.supportsSortOrder()) {
            navigationPanel.setSortDescending(
                selectedPanel.getSortFieldsDefaultOrder().getOrDefault(navigationPanel.getSort(), true));
        } else {
            navigationPanel.setSortDescending(true);
        }

        // hide minecraft version/sort/category if not needed
        navigationPanel.setSearchVisible(selectedPanel.supportsSearch());
        navigationPanel.setMinecraftVersionVisible(selectedPanel.supportsMinecraftVersionFiltering());
        navigationPanel.setCategoriesVisible(selectedPanel.hasCategories());
        navigationPanel.setSortVisible(selectedPanel.hasSort());
        navigationPanel.setSortOrderVisible(selectedPanel.supportsSortOrder());
        navigationPanel.setAddManuallyVisible(selectedPanel.supportsManualAdding());

        String platformMessage = selectedPanel.getPlatformMessage();
        platformMessageJPanel.setVisible(platformMessage != null);
        platformMessageJLabel.setText(new HTMLBuilder().center().text(platformMessage).build());

        // load in the content for the platform
        load(true);
    }

    private void loadMorePacks() {
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel != null && selectedPanel.hasPagination()) {
            loading = true;
            disableTabsWhileLoading();
            page += 1;

            Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_pack", navigationPanel.getSearch(), page,
                    selectedPanel.getPlatformName()));

            // load in the content for the platform
            new Thread(() -> {
                // the first page filters on the Minecraft version, so the later ones have to as
                // well - this asked about categories instead, and scrolling silently widened the
                // results back out to every version
                String minecraftVersion = null;
                if (selectedPanel.supportsMinecraftVersionFiltering()) {
                    minecraftVersion = navigationPanel.getMinecraftVersion();
                }

                String category = null;
                if (selectedPanel.hasCategories()) {
                    category = navigationPanel.getCategory();
                }

                String sort = null;
                if (selectedPanel.hasSort()) {
                    sort = navigationPanel.getSort();
                }

                // load in the content for the platform
                selectedPanel.loadMorePacks(contentPanel, minecraftVersion, category, sort,
                    navigationPanel.isSortDescending(), navigationPanel.getSearch(), page);

                SwingUtilities.invokeLater(() -> {
                    loading = false;
                    enableTabsAfterLoading();
                });

                revalidate();
                repaint();
            }).start();
        }
    }

    /**
     * Restarts the listing from its first page under whatever the toolbar now says.
     */
    private void reloadFromFilters() {
        if (loading) {
            return;
        }

        loading = true;
        page = 1;

        // disable the tabs
        disableTabsWhileLoading();

        load(true);
    }

    @Override
    public void onFiltersChanged() {
        reloadFromFilters();
    }

    @Override
    public void onSortFieldChanged() {
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel != null && selectedPanel.supportsSortOrder()) {
            navigationPanel.setSortDescending(
                selectedPanel.getSortFieldsDefaultOrder().getOrDefault(navigationPanel.getSort(), true));
        }

        reloadFromFilters();
    }

    @Override
    public void onSearch() {
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel == null) {
            return;
        }

        loading = true;
        page = 1;

        // disable the tabs
        disableTabsWhileLoading();

        if (!navigationPanel.getSearch().isEmpty()) {
            Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_pack", navigationPanel.getSearch(), page,
                    selectedPanel.getPlatformName()));
        }

        // load in the content for the platform
        load(true);
    }

    @Override
    public void onAddManually() {
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel == null) {
            return;
        }

        String id = DialogManager.okDialog().setTitle(GetText.tr("Add Pack By ID/Slug/URL"))
            .setContent(GetText.tr("Enter an ID/slug/url for a pack to add manually:")).showInput();

        if (id != null && !id.isEmpty()) {
            selectedPanel.addById(id);
        }
    }

    private void load(boolean scrollToTop) {
        loaded = true;
        PackBrowserPlatformPanel selectedPanel = getSelectedPanel();

        if (selectedPanel == null) {
            return;
        }

        new Thread(() -> {
            String minecraftVersion = null;
            if (selectedPanel.supportsMinecraftVersionFiltering()) {
                minecraftVersion = navigationPanel.getMinecraftVersion();
            }

            String category = null;
            if (selectedPanel.hasCategories()) {
                category = navigationPanel.getCategory();
            }

            String sort = null;
            if (selectedPanel.hasSort()) {
                sort = navigationPanel.getSort();
            }

            // load in the content for the platform
            selectedPanel.load(contentPanel, minecraftVersion, category, sort, navigationPanel.isSortDescending(),
                navigationPanel.getSearch(), page);

            SwingUtilities.invokeLater(() -> {
                if (scrollToTop) {
                    scrollPane.getVerticalScrollBar().setValue(0);
                }

                loading = false;
                enableTabsAfterLoading();
            });

            revalidate();
            repaint();
        }).start();
    }

    public void reload() {
        platformTabs.setSelectedIndex(0);
    }

    public void refresh() {
    }

    @Override
    public String getTitle() {
        return GetText.tr("Packs");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Unified ModPack Search";
    }

    @Override
    public void onRelocalization() {
        platformTabs.setLabelAt(0, GetText.tr("Search"));
    }

    @Override
    public void onThemeChange() {
        // the platform marks ship in light and dark pairs, so they are reloaded rather than retinted
        for (int i = 1; i < platformTabs.getTabCount(); i++) {
            String label = platformTabs.getLabelAt(i);

            if (label != null) {
                platformTabs.setIconAt(i, platformIcon(label.toLowerCase(Locale.ENGLISH)));
            }
        }

        platformMessageJPanel.setBackground(MD3Color.tertiaryContainer());
        platformMessageJLabel.setForeground(MD3Color.onTertiaryContainer());
        platformMessageJLabel.setIcon(MD3Icon.of(MD3Icons.INFO).withColor(MD3Color.onTertiaryContainer()));
    }

    @Override
    public void onTabChange(int tabIndex) {
        if (tabIndex == UIConstants.LAUNCHER_PACKS_TAB && !loaded) {
            afterTabChange();
        }
    }
}
