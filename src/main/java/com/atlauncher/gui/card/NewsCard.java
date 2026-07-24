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
package com.atlauncher.gui.card;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.AbstractNews;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;
import com.formdev.flatlaf.util.UIScale;

/**
 * One news article.
 *
 * <p>
 * The page used to be a single {@link JEditorPane} holding every article concatenated, separated by
 * horizontal rules and styled by two {@code News.*} colour keys. Nothing about an article was
 * addressable - it could not be given a card, a scroll position, or a heading in the launcher's own
 * type scale, because to Swing the whole page was one blob of text.
 *
 * <p>
 * The body stays HTML: news is written as HTML upstream and contains links people follow. Only the
 * title and date are lifted out, which is what lets them use the type scale and the article get a
 * container.
 */
public final class NewsCard extends MD3Card {
    /**
     * Long lines of prose are hard to track back to the start of. News is read rather than scanned,
     * so the column stops widening well before a maximised window does and is centred instead.
     */
    private static final int MAX_COLUMN_WIDTH = 900;

    private final JEditorPane body;

    /** Scaled; what the body was last measured against. */
    private int bodyWidth = -1;

    public NewsCard(AbstractNews news) {
        super(Variant.FILLED, new BorderLayout());

        setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, MD3Spacing.M, MD3Spacing.L));

        this.body = buildBody(news.content);

        add(buildHeader(news), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private JPanel buildHeader(AbstractNews news) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));

        JLabel title = new JLabel(news.title);
        title.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, news.title));
        title.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        title.setForeground(MD3Color.onSurface());
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);

        if (news.date != null && !news.date.isEmpty()) {
            JLabel date = new JLabel(news.date);
            date.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, news.date));
            date.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
            date.setForeground(MD3Color.onSurfaceVariant());
            date.setAlignmentX(LEFT_ALIGNMENT);
            header.add(date);
        }

        return header;
    }

    private JEditorPane buildBody(String content) {
        JEditorPane pane = new JEditorPane("text/html;charset=UTF-8", "");
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setEditorKit(newsKit());

        // a text component drags its scroll pane around to keep its caret in view, and every card
        // on the page has one. Left alone, the last article to finish laying itself out scrolls the
        // page to itself, so the news opens part way down instead of at the newest item
        if (pane.getCaret() instanceof DefaultCaret) {
            ((DefaultCaret) pane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }

        pane.setText(content == null ? "" : content);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OS.openWebBrowser(e.getURL());
            }
        });

        // selecting text needs the caret, and the caret needs focus, so the pane is focusable only
        // once there is a selection to copy
        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && pane.getSelectedText() != null) {
                    contextMenu(pane).show(pane, e.getX(), e.getY());
                }
            }
        });

        return pane;
    }

    private static JPopupMenu contextMenu(JEditorPane pane) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copy = new JMenuItem(GetText.tr("Copy"));

        // the old menu built this item and never added it, so copying from the news silently did
        // nothing for as long as the page has existed
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(pane.getSelectedText()), null));
        menu.add(copy);

        return menu;
    }

    /**
     * Restyles the article's own HTML in the launcher's tokens, so news written elsewhere still
     * reads as part of the theme rather than as a browser window pasted into it.
     */
    private static HTMLEditorKit newsKit() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = new StyleSheet();
        Font font = MD3Type.font(MD3Type.BODY_MEDIUM);

        styleSheet.addRule(String.format("body { font-family: %s; font-size: %dpt; color: %s; margin: 0; }",
                font.getFamily(), font.getSize(), hex(MD3Color.onSurfaceVariant())));
        styleSheet.addRule(String.format("a { color: %s; }", hex(MD3Color.primary())));
        styleSheet.addRule("p { margin: 0 0 8px 0; }");
        styleSheet.addRule(String.format("h1, h2, h3 { font-size: %dpt; color: %s; }",
                MD3Type.font(MD3Type.TITLE_SMALL).getSize(), hex(MD3Color.onSurface())));

        kit.setStyleSheet(styleSheet);

        return kit;
    }

    private static String hex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    /**
     * Caps the reading column. The height is pinned to what the content needs, so a short article
     * in a tall window keeps its size instead of being stretched to fill the space.
     */
    @Override
    public Dimension getMaximumSize() {
        Dimension size = getPreferredSize();
        size.width = UIScale.scale(MAX_COLUMN_WIDTH);

        return size;
    }

    /**
     * An HTML pane reports the height of one long line until it is told how wide it is, and the
     * card is only told that by its parent. So the body is re-measured whenever the card's width
     * changes - guarded on the width actually having changed, since setting a preferred size from
     * inside a layout pass otherwise loops.
     */
    @Override
    public void doLayout() {
        int width = getWidth() - getInsets().left - getInsets().right;

        if (width > 0 && width != bodyWidth) {
            bodyWidth = width;

            body.setSize(width, Short.MAX_VALUE);

            Dimension size = new Dimension(width, body.getPreferredSize().height);
            body.setPreferredSize(size);
            body.setMinimumSize(size);
            body.setMaximumSize(size);

            // the height this card asks for has just changed, so the column stacking it has to lay
            // out again. Costs one extra pass per resize, which is why it is guarded on the width
            // having actually changed rather than run on every layout
            revalidate();
        }

        super.doLayout();
    }
}
