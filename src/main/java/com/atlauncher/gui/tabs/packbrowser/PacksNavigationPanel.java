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
package com.atlauncher.gui.tabs.packbrowser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3FilterChip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.ComboItem;
import com.formdev.flatlaf.util.UIScale;

/**
 * The pack browser's toolbar: search, the filters, and adding a pack by hand.
 *
 * <p>
 * Replaces a 34px strip of five labelled combo boxes. The labels are gone because a chip already
 * shows what it filters by, and the two sort arrows are gone because only ever one of them was
 * visible - a control that swaps itself for a different control when clicked is two ways of saying
 * one thing.
 *
 * <p>
 * Holds no loading logic of its own. Which platform is selected, what it supports and when to
 * reload all belong to {@link com.atlauncher.gui.tabs.PacksBrowserTab}; this reports what the user
 * did and answers what they chose.
 */
public final class PacksNavigationPanel extends JPanel implements RelocalizationListener {
    /** Wide enough for a pack name, and no wider - the grid needs the rest. */
    private static final int SEARCH_COLUMNS = 18;

    public interface Listener {
        /**
         * A filter or the sort direction changed.
         */
        void onFiltersChanged();

        /**
         * The sort field changed. Separate because the direction that makes sense goes with the
         * field - newest first, but A to Z - and only the platform knows which.
         */
        void onSortFieldChanged();

        /**
         * The user submitted the search box.
         */
        void onSearch();

        void onAddManually();
    }

    private final Listener listener;

    private final MD3TextField searchField = MD3TextField.search(GetText.tr("Search"));
    private final MD3FilterChip<String> minecraftVersionChip;
    private final MD3FilterChip<String> categoryChip;
    private final MD3FilterChip<String> sortChip;
    private final MD3IconButton sortOrderButton = new MD3IconButton(MD3Icons.ARROW_DOWNWARD, "");
    private final MD3Button addManuallyButton = MD3Button.outlined(GetText.tr("Add Manually"),
            MD3Icon.of(MD3Icons.ADD));
    private final MD3CircularProgress loadingIndicator = MD3CircularProgress.indeterminate();

    private final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.scale(MD3Spacing.S), 0));

    private boolean sortDescending = true;

    public PacksNavigationPanel(Listener listener) {
        super(new BorderLayout());

        this.listener = listener;
        this.minecraftVersionChip = new MD3FilterChip<>(GetText.tr("Minecraft"), true, listener::onFiltersChanged);
        this.categoryChip = new MD3FilterChip<>(GetText.tr("Category"), true, listener::onFiltersChanged);
        this.sortChip = new MD3FilterChip<>(GetText.tr("Sort"), false, listener::onSortFieldChanged);

        setName("packsNavigationPanel");
        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        // the search box is 40dp and the filter chips are 32dp, so without this they sit on two
        // different centre lines - a flow layout centres within its tallest child, not its container
        add(MD3TopAppBar.centred(buildLeading()), BorderLayout.WEST);
        add(MD3TopAppBar.centred(buildFilters()), BorderLayout.CENTER);
        add(MD3TopAppBar.centred(buildTrailing()), BorderLayout.EAST);

        RelocalizationManager.addListener(this);
    }

    private JPanel buildLeading() {
        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leading.setOpaque(false);

        searchField.setName("packsSearchField");
        searchField.setColumns(SEARCH_COLUMNS);
        searchField.setLeadingIcon(MD3Icons.SEARCH);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    listener.onSearch();
                }
            }
        });

        leading.add(searchField);

        return leading;
    }

    private JPanel buildFilters() {
        filters.setOpaque(false);
        filters.setBorder(MD3Spacing.border(0, MD3Spacing.M, 0, 0));

        filters.add(minecraftVersionChip.getChip());
        filters.add(categoryChip.getChip());
        filters.add(sortChip.getChip());

        sortOrderButton.addActionListener(e -> {
            setSortDescending(!sortDescending);
            listener.onFiltersChanged();
        });

        filters.add(sortOrderButton);
        refreshSortOrderButton();

        return filters;
    }

    private JPanel buildTrailing() {
        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        trailing.setOpaque(false);

        loadingIndicator.setDiameter(MD3Spacing.ICON_SIZE_LARGE);
        loadingIndicator.setVisible(false);
        loadingIndicator.setToolTipText(GetText.tr("Loading..."));

        addManuallyButton.addActionListener(e -> listener.onAddManually());

        trailing.add(loadingIndicator);
        trailing.add(addManuallyButton);

        return trailing;
    }

    public String getSearch() {
        return searchField.getText();
    }

    public void setSearch(String search) {
        searchField.setText(search == null ? "" : search);
    }

    public void setMinecraftVersions(List<ComboItem<String>> versions) {
        minecraftVersionChip.setOptions(versions);
    }

    public String getMinecraftVersion() {
        return minecraftVersionChip.getValue();
    }

    public void clearCategories() {
        categoryChip.clear();
    }

    public void addCategory(ComboItem<String> category) {
        categoryChip.addOption(category);
    }

    public String getCategory() {
        return categoryChip.getValue();
    }

    public void setSortFields(List<ComboItem<String>> fields) {
        sortChip.setOptions(fields);
    }

    public String getSort() {
        return sortChip.getValue();
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    /**
     * Sets the direction without reporting it, for when the platform - not the user - decided it.
     */
    public void setSortDescending(boolean sortDescending) {
        this.sortDescending = sortDescending;

        refreshSortOrderButton();
    }

    private void refreshSortOrderButton() {
        sortOrderButton.setIcon(MD3Icon.of(sortDescending ? MD3Icons.ARROW_DOWNWARD : MD3Icons.ARROW_UPWARD));

        // the tooltip says what clicking will do, not what the arrow already shows
        String tooltip = sortDescending ? GetText.tr("Sort ascending") : GetText.tr("Sort descending");
        sortOrderButton.setToolTipText(tooltip);
        sortOrderButton.getAccessibleContext().setAccessibleName(tooltip);
    }

    public void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
    }

    public void setSearchVisible(boolean visible) {
        searchField.setVisible(visible);
    }

    public void setMinecraftVersionVisible(boolean visible) {
        minecraftVersionChip.setVisible(visible);
    }

    public void setCategoriesVisible(boolean visible) {
        categoryChip.setVisible(visible);
    }

    public void setSortVisible(boolean visible) {
        sortChip.setVisible(visible);
    }

    public void setSortOrderVisible(boolean visible) {
        sortOrderButton.setVisible(visible);
    }

    public void setAddManuallyVisible(boolean visible) {
        addManuallyButton.setVisible(visible);
    }

    /**
     * Locks the filters while a load is in flight, so a second request cannot be queued behind the
     * first with different terms.
     */
    public void setFiltersEnabled(boolean enabled) {
        minecraftVersionChip.setEnabled(enabled);
        categoryChip.setEnabled(enabled);
        sortChip.setEnabled(enabled);
        sortOrderButton.setEnabled(enabled);
    }

    @Override
    public void onRelocalization() {
        searchField.setLabel(GetText.tr("Search"));
        addManuallyButton.setText(GetText.tr("Add Manually"));
        loadingIndicator.setToolTipText(GetText.tr("Loading..."));
        minecraftVersionChip.setName(GetText.tr("Minecraft"));
        categoryChip.setName(GetText.tr("Category"));
        sortChip.setName(GetText.tr("Sort"));

        refreshSortOrderButton();
    }
}
