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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.Contributor;
import com.atlauncher.gui.components.BackgroundImageLabel;
import com.atlauncher.gui.components.SocialLinks;
import com.atlauncher.gui.md3.MD3Html;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;
import com.atlauncher.viewmodel.base.IAboutTabViewModel;
import com.atlauncher.viewmodel.impl.AboutTabViewModel;
import com.formdev.flatlaf.util.UIScale;

/**
 * The about tab displays to the user some basic information in regard to the current state of ATLauncher, and some
 * other basic diagnostic information to let users more easily report errors.
 */
public class AboutTab extends HierarchyPanel implements Tab {
    private static final int AVATAR_SIZE = 64;
    private static final int CONTRIBUTORS_HEIGHT = 116;
    private static final int DOCUMENT_HEIGHT = 240;

    private JScrollPane contributorsScrollPane;
    private JPanel authorsList;
    private JPanel socialLinks;

    private IAboutTabViewModel viewModel;

    public AboutTab() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.XL));
    }

    @Override
    public String getTitle() {
        return GetText.tr("About");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "About";
    }

    @Override
    protected void createViewModel() {
        viewModel = new AboutTabViewModel();
    }

    /**
     * A heading over a section, in the launcher's own type scale rather than a separate title font
     * and a rule under it.
     */
    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.TITLE_SMALL, text));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        label.setForeground(MD3Color.primary());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(MD3Spacing.border(MD3Spacing.XL, 0, MD3Spacing.S, 0));

        return label;
    }

    @Override
    protected void onShow() {
        JLabel name = new JLabel(Constants.LAUNCHER_NAME);
        name.setFont(MD3Type.font(MD3Type.HEADLINE_SMALL, Constants.LAUNCHER_NAME));
        name.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.HEADLINE_SMALL);
        name.setForeground(MD3Color.onSurface());
        name.setAlignmentX(LEFT_ALIGNMENT);
        add(name);

        add(buildInfoCard());

        // Contributors
        add(sectionLabel(GetText.tr("Contributors")));

        authorsList = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.L), 0));
        authorsList.setOpaque(false);

        addDisposable(viewModel.getContributors().subscribe(this::renderAuthors));

        contributorsScrollPane = new JScrollPane(authorsList, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contributorsScrollPane.setBorder(null);
        contributorsScrollPane.setOpaque(false);
        contributorsScrollPane.getViewport().setOpaque(false);
        contributorsScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        contributorsScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        contributorsScrollPane.setPreferredSize(new Dimension(0, UIScale.scale(CONTRIBUTORS_HEIGHT)));
        contributorsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(CONTRIBUTORS_HEIGHT)));
        add(contributorsScrollPane);

        // Where to find us. These used to sit in a bar along the bottom of every screen; this
        // is where someone goes when they are actually looking for them.
        socialLinks = SocialLinks.panel();
        socialLinks.setAlignmentX(LEFT_ALIGNMENT);
        socialLinks.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(48)));
        add(socialLinks);

        add(buildDocuments());
    }

    /**
     * What the user is asked for when they report a problem, and a button that puts it on the
     * clipboard - the view model has offered the text for it all along, and nothing ever called it.
     *
     * <p>
     * Monospaced, because the lines are tab separated and a proportional face puts the values
     * wherever the tab stops happen to land.
     */
    private InfoCard buildInfoCard() {
        InfoCard card = new InfoCard();

        JTextPane textInfo = new JTextPane();
        textInfo.setText(viewModel.getInfo());
        textInfo.setEditable(false);
        textInfo.setFocusable(false);
        textInfo.setOpaque(false);
        textInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, MD3Type.font(MD3Type.BODY_MEDIUM).getSize()));
        textInfo.setForeground(MD3Color.onSurfaceVariant());

        MD3Button copy = MD3Button.text(GetText.tr("Copy"));
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(viewModel.getCopyInfo()), null));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);

        actions.add(copy);

        card.add(textInfo, BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);

        return card;
    }

    /**
     * The licence and the third party notices, one tab each.
     */
    private JPanel buildDocuments() {
        MD3Tabs tabs = new MD3Tabs();
        CardLayout layout = new CardLayout();
        JPanel documents = new JPanel(layout);
        documents.setOpaque(false);

        documents.add(documentPane("/LICENSE"), "license");
        documents.add(documentPane("/THIRDPARTYLIBRARIES"), "libraries");

        tabs.addTab(GetText.tr("License"));
        tabs.addTab(GetText.tr("Third Party Libraries"));
        tabs.addChangeListener(e -> layout.show(documents, tabs.getSelectedIndex() == 0 ? "license" : "libraries"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(MD3Spacing.border(MD3Spacing.XL, 0, 0, 0));
        panel.add(tabs, BorderLayout.NORTH);
        panel.add(documents, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane documentPane(String resource) {
        String html = "";

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(App.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            html = new HTMLBuilder()
                    .text(reader.lines().collect(Collectors.joining("<br/>"))
                        .replace("%YEAR%", new SimpleDateFormat("yyyy").format(new Date())))
                    .build();
        } catch (Exception e) {
            LogManager.logStackTrace(e);
        }

        JEditorPane document = MD3Html.pane(html);

        JScrollPane scrollPane = new JScrollPane(document);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(0, UIScale.scale(DOCUMENT_HEIGHT)));
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

        return scrollPane;
    }

    @Override
    protected void onDestroy() {
        removeAll();

        authorsList = null;
        contributorsScrollPane = null;
        socialLinks = null;
    }

    /**
     * Accepts contributors to render onto the screen.
     *
     * @param contributors contributors to render
     */
    private void renderAuthors(List<Contributor> contributors) {
        authorsList.removeAll();

        for (Contributor contributor : contributors) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setOpaque(false);

            BackgroundImageLabel icon = new BackgroundImageLabel(contributor.avatarUrl, UIScale.scale(AVATAR_SIZE),
                    UIScale.scale(AVATAR_SIZE));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            icon.setCursor(new Cursor(Cursor.HAND_CURSOR));
            icon.setToolTipText(contributor.name);
            icon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        OS.openWebBrowser(contributor.url);
                    }
                }
            });
            panel.add(icon);
            panel.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XS)));

            JLabel contributorName = new JLabel(contributor.name);
            contributorName.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, contributor.name));
            contributorName.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
            contributorName.setForeground(MD3Color.primary());
            contributorName.setAlignmentX(Component.CENTER_ALIGNMENT);
            contributorName.setCursor(new Cursor(Cursor.HAND_CURSOR));
            contributorName.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        OS.openWebBrowser(contributor.url);
                    }
                }
            });
            panel.add(contributorName);

            authorsList.add(panel);
        }

        authorsList.revalidate();
        authorsList.repaint();

        SwingUtilities.invokeLater(() -> {
            if (contributorsScrollPane != null) {
                contributorsScrollPane.getHorizontalScrollBar().setValue(0);
            }
        });
    }

    /**
     * A card that lays its contents out and stops growing once it has what it needs, so a page
     * stacking it in a box layout does not stretch it to fill.
     */
    private static final class InfoCard extends MD3Card {
        InfoCard() {
            super(Variant.FILLED, new BorderLayout(UIScale.scale(MD3Spacing.L), 0));

            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(MD3Spacing.border(MD3Spacing.L));
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = getPreferredSize();
            size.width = Integer.MAX_VALUE;

            return size;
        }
    }
}
