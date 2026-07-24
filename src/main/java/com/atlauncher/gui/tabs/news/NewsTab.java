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
package com.atlauncher.gui.tabs.news;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.AbstractNews;
import com.atlauncher.gui.card.NewsCard;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.viewmodel.base.INewsViewModel;
import com.atlauncher.viewmodel.impl.NewsViewModel;
import com.formdev.flatlaf.util.UIScale;

/**
 * The latest news, one card per article.
 */
public class NewsTab extends HierarchyPanel implements Tab {
    private INewsViewModel viewModel;
    private ArticleList articles;
    private JScrollPane scrollPane;

    public NewsTab() {
        super(new BorderLayout());
    }

    @Override
    protected void createViewModel() {
        viewModel = new NewsViewModel();
    }

    @Override
    protected void onShow() {
        articles = new ArticleList();

        scrollPane = new JScrollPane(new LoadingPanel(GetText.tr("Loading news...")),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(MD3Color.surface());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        addDisposable(viewModel.getNews().subscribe(this::show));
    }

    private void show(List<AbstractNews> news) {
        if (news == null || news.isEmpty()) {
            return;
        }

        articles.removeAll();

        for (AbstractNews item : news) {
            NewsCard card = new NewsCard(item);
            card.setAlignmentX(CENTER_ALIGNMENT);

            articles.add(card);
            articles.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));
        }

        scrollPane.setViewportView(articles);
        articles.revalidate();
        articles.repaint();

        // the cards only measure their articles once they have been laid out, and each one that
        // grows moves what is under it. Sent to the back of the queue so the page lands on the
        // newest item rather than wherever the last card to settle left it
        SwingUtilities.invokeLater(() -> {
            if (scrollPane != null) {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
            }
        });
    }

    @Override
    protected void onDestroy() {
        articles = null;
        scrollPane = null;
        removeAll();
    }

    @Override
    public String getTitle() {
        return GetText.tr("News");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "News";
    }

    /**
     * The column the articles are stacked in.
     *
     * <p>
     * Implements {@link Scrollable} to take the viewport's width, which is what makes the cards - and
     * so the HTML inside them - reflow with the window instead of being laid out at whatever width
     * their content happened to want.
     */
    private static final class ArticleList extends JPanel implements Scrollable {
        ArticleList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setBackground(MD3Color.surface());
            setBorder(MD3Spacing.border(MD3Spacing.L));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return UIScale.scale(MD3Spacing.L);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
