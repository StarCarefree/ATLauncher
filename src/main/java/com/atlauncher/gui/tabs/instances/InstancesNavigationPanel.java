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
package com.atlauncher.gui.tabs.instances;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.dialogs.ImportInstanceDialog;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.gui.tabs.InstancesTab;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.sort.InstanceSortingStrategies;
import com.atlauncher.viewmodel.base.IInstancesTabViewModel;
import com.formdev.flatlaf.util.UIScale;
import com.gitlab.doomsdayrs.lib.rxswing.schedulers.SwingSchedulers;

/**
 * The instances toolbar: search, sort, import.
 *
 * <p>
 * Sorting moved from a combo box to a row of filter chips. There are four orders and exactly one is
 * active - a combo hides all four behind a click and gives no hint that changing it is cheap, while
 * chips show the whole choice and which one is on. The count of what the grid is showing sits with
 * Import, so a search that has hidden most of the library is visible without scrolling the cards.
 */
public final class InstancesNavigationPanel extends JPanel implements RelocalizationListener {
    private final IInstancesTabViewModel viewModel;

    private final MD3Button importButton = MD3Button.outlined(GetText.tr("Import"),
            MD3Icon.of(MD3Icons.DOWNLOAD));
    private final InstancesSearchField searchField;
    private final List<MD3Chip> sortChips = new ArrayList<>();
    private final JLabel countLabel = new JLabel();
    private final JLabel loadingLabel = new JLabel();
    private final JPanel loadingIndicator = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));

    /** What {@link #countLabel} last showed, so a language change can rewrite it. */
    private int shownCount;

    /** Stops a chip's own deselection from cascading while the row is being re-synchronised. */
    private boolean syncingChips;

    public InstancesNavigationPanel(final InstancesTab tab, final IInstancesTabViewModel viewModel) {
        super(new BorderLayout());

        this.viewModel = viewModel;
        this.searchField = new InstancesSearchField(viewModel);

        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        // this sits between the app bar and the content, so it is the app bar's lower half as far
        // as the user is concerned and raises with it once the grid scrolls underneath
        putClientProperty(MD3TopAppBar.COMPANION_KEY, true);

        // the search box is 40dp and the sort chips are 32dp, so without this they sit on two
        // different centre lines - a flow layout centres within its tallest child, not its container
        add(MD3TopAppBar.centred(buildLeading()), BorderLayout.WEST);
        add(MD3TopAppBar.centred(buildSortChips()), BorderLayout.CENTER);
        add(MD3TopAppBar.centred(buildTrailing(tab)), BorderLayout.EAST);

        this.importButton.addActionListener(e -> new ImportInstanceDialog());

        tab.addDisposable(viewModel.getInstancesList().observeOn(SwingSchedulers.edt())
                .subscribe(list -> setShownCount(list.instances.size())));

        RelocalizationManager.addListener(this);
    }

    private JPanel buildLeading() {
        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leading.setOpaque(false);
        leading.add(searchField);

        return leading;
    }

    private JPanel buildSortChips() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));
        row.setOpaque(false);
        row.setBorder(MD3Spacing.border(0, MD3Spacing.M, 0, 0));

        for (InstanceSortingStrategies strategy : InstanceSortingStrategies.values()) {
            MD3Chip chip = MD3Chip.filter(strategy.toString());
            chip.setSelected(strategy == viewModel.getSort());
            chip.addActionListener(e -> selectSort(strategy, chip));

            sortChips.add(chip);
            row.add(chip);
        }

        return row;
    }

    /**
     * Sorting is a single choice, so selecting one chip clears the rest - and re-clicking the
     * active one leaves it active rather than leaving the list with no order at all.
     */
    private void selectSort(InstanceSortingStrategies strategy, MD3Chip chosen) {
        if (syncingChips) {
            return;
        }

        syncingChips = true;

        try {
            for (MD3Chip chip : sortChips) {
                chip.setSelected(chip == chosen);
            }
        } finally {
            syncingChips = false;
        }

        viewModel.setSort(strategy);
    }

    private JPanel buildTrailing(InstancesTab tab) {
        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(MD3Spacing.S), 0));
        trailing.setOpaque(false);

        loadingLabel.setText(GetText.tr("Loading..."));
        loadingLabel.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, loadingLabel.getText()));
        loadingLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        loadingLabel.setForeground(MD3Color.onSurfaceVariant());

        // the GIF this replaces was a fixed set of pixels in one colour, so it stayed that colour
        // under all eighteen themes and the same physical size at every display scale
        loadingIndicator.setOpaque(false);
        loadingIndicator.add(MD3CircularProgress.inline());
        loadingIndicator.add(loadingLabel);

        tab.addDisposable(viewModel.getIsLoading().subscribe(loadingIndicator::setVisible));

        countLabel.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        countLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        countLabel.setForeground(MD3Color.onSurfaceVariant());
        setShownCount(0);

        trailing.add(loadingIndicator);
        trailing.add(countLabel);
        trailing.add(importButton);

        return trailing;
    }

    private void setShownCount(int count) {
        shownCount = count;
        // #. {0} is the number of instances currently shown in the grid
        countLabel.setText(GetText.tr("{0} instances", count));
    }

    @Override
    public void onRelocalization() {
        importButton.setText(GetText.tr("Import"));
        loadingLabel.setText(GetText.tr("Loading..."));
        searchField.setLabel(GetText.tr("Search"));
        setShownCount(shownCount);

        InstanceSortingStrategies[] strategies = InstanceSortingStrategies.values();

        for (int i = 0; i < sortChips.size() && i < strategies.length; i++) {
            sortChips.get(i).setText(strategies[i].toString());
        }
    }
}
