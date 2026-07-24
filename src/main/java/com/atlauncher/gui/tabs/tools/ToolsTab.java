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
package com.atlauncher.gui.tabs.tools;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.themes.md3.token.MD3Spacing;

public class ToolsTab extends HierarchyPanel implements Tab {

    private ToolsViewModel viewModel;

    public ToolsTab() {
        super(new BorderLayout());
    }

    @Override
    protected void onShow() {
        // the tools reflow with the window rather than being pinned to a three by two grid, which
        // stretched six cards to whatever shape the window happened to be
        JPanel mainPanel = new JPanel(
                new CardGridLayout(AbstractToolPanel.CARD_WIDTH, AbstractToolPanel.MAX_CARD_WIDTH, MD3Spacing.L));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(MD3Spacing.border(MD3Spacing.L));

        mainPanel.add(new NetworkCheckerToolPanel(viewModel));
        mainPanel.add(new LogClearerToolPanel(viewModel));
        mainPanel.add(new DebugModePanel(viewModel));
        mainPanel.add(new DownloadClearerToolPanel(viewModel));
        mainPanel.add(new SkinUpdaterToolPanel(viewModel));
        mainPanel.add(new LibrariesDeleterToolPanel(viewModel));

        JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Tools");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Tools";
    }

    @Override
    protected void createViewModel() {
        viewModel = new ToolsViewModel();
    }

    @Override
    protected void onDestroy() {
        removeAll();
    }
}
