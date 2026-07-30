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
package com.atlauncher.gui.tabs.instances;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.util.List;
import java.util.stream.Collectors;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.gui.card.InstanceCard;
import com.atlauncher.gui.card.NilCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.InstancesTab;
import com.atlauncher.managers.PerformanceManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.viewmodel.base.IInstancesTabViewModel;

/**
 * The instances, as a grid of cards that reflows with the window.
 *
 * <p>
 * Previously one full-width card per row, which meant a 1200px window showed three instances and
 * spent most of its area on empty card. The same window now shows twelve.
 *
 * <p>
 * On {@link CardGridLayout} rather than {@link WrapLayout}, as the servers and accounts grids
 * already were. A flow layout places cards at their preferred width and leaves whatever does not
 * divide evenly as a gutter down the trailing edge - on a maximised window that came to most of a
 * card's width, which read as the page having failed to load its last column.
 */
public final class InstancesListPanel extends HierarchyPanel {
    private final InstancesTab instancesTab;
    private final IInstancesTabViewModel viewModel;

    private final LayoutManager grid = new CardGridLayout(InstanceCard.CARD_WIDTH, InstanceCard.MAX_CARD_WIDTH,
            MD3Spacing.L);

    private final NilCard nilCard = new NilCard(
            getNilMessage(),
            new NilCard.Action[] {
                    NilCard.Action.createCreatePackAction(),
                    NilCard.Action.createDownloadPackAction()
            });

    public InstancesListPanel(InstancesTab instancesTab, final IInstancesTabViewModel viewModel) {
        super(new CardGridLayout(InstanceCard.CARD_WIDTH, InstanceCard.MAX_CARD_WIDTH, MD3Spacing.L));

        this.instancesTab = instancesTab;
        this.viewModel = viewModel;

        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(MD3Spacing.L));

        PerformanceManager.start("Displaying Instances");
    }

    private static String getNilMessage() {
        return new HTMLBuilder()
                .text(GetText.tr("There are no instances to display.<br/><br/>Install one from the Packs tab."))
                .build();
    }

    @Override
    protected void onShow() {
        addDisposable(viewModel.getInstancesList()
                .map(instancesList -> {
                    viewModel.setIsLoading(true);

                    return instancesList.instances.stream().map(instance -> new InstanceCard(
                            instance.instance,
                            instance.hasUpdate,
                            instancesList.instanceTitleFormat)).collect(Collectors.toList());
                }).subscribe(this::render));
    }

    private void render(List<InstanceCard> instances) {
        removeAll();

        if (instances.isEmpty()) {
            // the grid hands every child a column's width, which is not a shape the nil card was
            // built for - so the empty state is laid out on its own terms instead
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            add(nilCard);
        } else {
            PerformanceManager.start("Render cards");

            setLayout(grid);

            for (InstanceCard instance : instances) {
                add(instance);
            }

            PerformanceManager.end("Render cards");
        }

        revalidate();
        repaint();

        viewModel.setIsLoading(false);

        // once the cards are laid out there is somewhere to scroll back to
        invokeLater(() -> instancesTab.setScroll(viewModel.getScroll()));
        PerformanceManager.end("Displaying Instances");
    }

    @Override
    protected void createViewModel() {
    }

    @Override
    protected void onDestroy() {
        removeAll();
    }
}
