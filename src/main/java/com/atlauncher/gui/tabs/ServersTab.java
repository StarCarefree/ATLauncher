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
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.gui.WheelScrollLayerUI;
import com.atlauncher.gui.card.NilCard;
import com.atlauncher.gui.card.ServerCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.viewmodel.base.IServersTabViewModel;
import com.atlauncher.viewmodel.impl.ServersTabViewModel;

public class ServersTab extends HierarchyPanel implements Tab {
    /** Wide enough for a server name, and no wider - the grid needs the rest. */
    private static final int SEARCH_COLUMNS = 18;

    private MD3TextField searchBox;

    private JPanel panel;
    private JScrollPane scrollPane;

    private final NilCard nilCard = new NilCard(
            getNilMessage(),
            new NilCard.Action[] {
                    NilCard.Action.createCreateServerAction(),
                    NilCard.Action.createDownloadServerAction()
            });

    private IServersTabViewModel viewModel;

    public ServersTab() {
        super(new BorderLayout());
    }

    @Override
    protected void onShow() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(true);
        toolbar.setBackground(MD3Color.surface());
        toolbar.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leading.setOpaque(false);

        searchBox = MD3TextField.search(GetText.tr("Search"));
        searchBox.setName("serversSearchField");
        searchBox.setColumns(SEARCH_COLUMNS);
        searchBox.setLeadingIcon(MD3Icons.SEARCH);
        addDisposable(
                viewModel.getSearchObservable().subscribe(it -> searchBox.setText(it.orElse(null))));
        searchBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    String text = searchBox.getText();
                    Analytics.trackEvent(AnalyticsEvent.forSearchEvent("servers", text));
                    viewModel.setSearchSubject(text);
                }
            }
        });

        leading.add(searchBox);
        toolbar.add(leading, BorderLayout.WEST);

        add(toolbar, BorderLayout.NORTH);

        // servers reflow into a grid, as instances do - the two are the same kind of thing and were
        // laid out nothing alike, one full-width row per server against a wall of cards
        panel = new JPanel(new CardGridLayout(ServerCard.CARD_WIDTH, ServerCard.MAX_CARD_WIDTH, MD3Spacing.L));
        panel.setOpaque(false);
        panel.setBorder(MD3Spacing.border(MD3Spacing.L));

        scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(new JLayer<>(scrollPane, new WheelScrollLayerUI()), BorderLayout.CENTER);

        addDisposable(viewModel.getServersObservable().subscribe(servers -> {
            panel.removeAll();

            servers.forEach(server -> panel.add(new ServerCard(server)));

            scrollPane.setViewportView(panel.getComponentCount() == 0 ? emptyState() : panel);

            validate();
            repaint();
            searchBox.requestFocus();
        }));

        addDisposable(
                viewModel.getViewPosition().subscribe(scrollPane.getVerticalScrollBar()::setValue));
    }

    /**
     * The grid gives every child a column's width, which is not a shape the nil card was built for -
     * so it is shown on its own rather than as a card in the grid.
     */
    private JPanel emptyState() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(MD3Spacing.border(MD3Spacing.L));
        wrapper.add(nilCard);

        return wrapper;
    }

    @Override
    public String getTitle() {
        return GetText.tr("Servers");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Servers";
    }

    private static String getNilMessage() {
        return new HTMLBuilder()
                .text(GetText.tr("There are no servers to display.<br/><br/>Install one from the Packs tab."))
                .build();
    }

    @Override
    protected void createViewModel() {
        viewModel = new ServersTabViewModel();
    }

    @Override
    protected void onDestroy() {
        if (scrollPane != null) {
            viewModel.setViewPosition(scrollPane.getVerticalScrollBar().getValue());
        }
        removeAll();
        searchBox = null;
        panel = null;
        scrollPane = null;
    }
}
