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
package com.atlauncher.gui.card.packbrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.components.BackgroundImageLabel;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.MD3Menus;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * The shape every modpack card takes, whichever platform it came from.
 *
 * <p>
 * The six platform cards were near-identical: a titled border, a {@link javax.swing.JSplitPane}
 * faking a two-column layout, a scrollable description, and a row of between two and seven buttons.
 * They now share this base and differ only in the data they hand it - which is the only thing that
 * ever actually differed.
 *
 * <p>
 * Subclasses keep their own buttons and listeners. Pass the one that matters as {@code primary} and
 * the rest as overflow; see {@link MD3Menus} for why they are not rebuilt as menu items.
 */
public abstract class MD3PackCard extends MD3Card {
    protected static final int CARD_WIDTH = 280;
    /** 16:9 against the card width. */
    protected static final int COVER_HEIGHT = 158;
    protected static final int MAX_BADGES = 3;

    /** Roughly two lines at body-small in a 280dp card. */
    private static final int DESCRIPTION_LIMIT = 84;

    /** Written as an escape because the build sets no source encoding. */
    private static final String ELLIPSIS = "\u2026";

    protected MD3PackCard() {
        super(Variant.FILLED, new BorderLayout());

        // the cover runs to the card's edges, so padding belongs to the body
        setBorder(null);
    }

    /**
     * @param cover       the artwork, or null for packs that ship none
     * @param badges      up to {@link #MAX_BADGES}; anything beyond is dropped rather than wrapped
     *                    onto a row the card was not measured for
     * @param primary     the action the card exists for - installing
     * @param overflow    everything else, in the order it should appear in the menu; nulls and
     *                    hidden buttons are skipped
     */
    protected void build(String title, JComponent cover, String description, List<MD3Badge> badges,
            AbstractButton primary, AbstractButton... overflow) {
        add(buildCover(cover), BorderLayout.NORTH);
        add(buildBody(title, description, badges, primary, overflow), BorderLayout.CENTER);
    }

    /**
     * Artwork from a URL, fetched in the background. Most platforms give a square icon rather than
     * a banner, so it is centred rather than stretched.
     */
    protected static JComponent coverFromUrl(String url) {
        BackgroundImageLabel label = new BackgroundImageLabel(url, UIScale.scale(CARD_WIDTH),
                UIScale.scale(COVER_HEIGHT));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        return label;
    }

    private JComponent buildCover(JComponent cover) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = MD3Paint.setup(g);

                try {
                    // rounding a box twice this tall leaves only the top two corners curved
                    g2.clip(MD3Shape.rounded(0, 0, getWidth(), getHeight() * 2f, MD3Shape.CARD));
                    super.paintChildren(g2);
                } finally {
                    g2.dispose();
                }
            }
        };

        wrapper.setOpaque(true);
        wrapper.setBackground(MD3Color.surfaceContainerHigh());
        wrapper.setPreferredSize(new Dimension(UIScale.scale(CARD_WIDTH), UIScale.scale(COVER_HEIGHT)));

        if (cover != null) {
            wrapper.add(cover, BorderLayout.CENTER);
        }

        return wrapper;
    }

    private JComponent buildBody(String title, String description, List<MD3Badge> badges, AbstractButton primary,
            AbstractButton... overflow) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.M, MD3Spacing.S));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setToolTipText(title);
        body.add(titleLabel);

        body.add(buildSummary(description));

        if (badges != null && !badges.isEmpty()) {
            JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.XS), 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);

            for (int i = 0; i < badges.size() && i < MAX_BADGES; i++) {
                row.add(badges.get(i));
            }

            body.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
            body.add(row);
        }

        body.add(buildActions(primary, overflow));

        return body;
    }

    /**
     * The summary, wrapped to two lines.
     *
     * <p>
     * A plain {@link JLabel} does not wrap - it clips at the card's edge and the sentence simply
     * stops - so the text goes through an HTML block with an explicit width, which Swing does wrap.
     * The block is always added, at a fixed two-line height, even for the platforms that return no
     * summary at all: without it those cards come out shorter than their neighbours and the grid
     * loses its baseline.
     */
    private JComponent buildSummary(String description) {
        int contentWidth = UIScale.scale(CARD_WIDTH - MD3Spacing.L - MD3Spacing.S);
        Font font = MD3Type.font(MD3Type.BODY_SMALL);

        JLabel summary = new JLabel();
        summary.setFont(font);
        summary.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        summary.setForeground(MD3Color.onSurfaceVariant());
        summary.setAlignmentX(LEFT_ALIGNMENT);
        summary.setVerticalAlignment(SwingConstants.TOP);
        summary.setText(wrapToTwoLines(summary.getFontMetrics(font), description, contentWidth));

        if (description != null && !description.trim().isEmpty()) {
            summary.setToolTipText(description);
        }

        Dimension fixed = new Dimension(contentWidth, summary.getFontMetrics(font).getHeight() * 2);
        summary.setPreferredSize(fixed);
        summary.setMinimumSize(fixed);
        summary.setMaximumSize(fixed);

        return summary;
    }

    /**
     * Breaks a summary across two lines that are guaranteed to fit.
     *
     * <p>
     * Measured with {@link FontMetrics} and emitted as an explicit {@code <br>} rather than left to
     * Swing's HTML engine. A {@code width} on an HTML block is honoured inconsistently - the label
     * lays the text out at its natural width and then clips, which loses whole words from the
     * middle of the sentence rather than trimming the end.
     */
    private static String wrapToTwoLines(FontMetrics metrics, String description, int width) {
        if (description == null || description.trim().isEmpty()) {
            return " ";
        }

        String[] words = shorten(description).split(" ");
        StringBuilder line = new StringBuilder();
        List<String> lines = new ArrayList<>();

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (metrics.stringWidth(candidate) <= width || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);

                continue;
            }

            lines.add(line.toString());
            line.setLength(0);
            line.append(word);

            if (lines.size() == 2) {
                break;
            }
        }

        if (lines.size() < 2 && line.length() > 0) {
            lines.add(line.toString());
        }

        // anything that did not fit is signalled on the last line rather than silently dropped
        int used = 0;

        for (String rendered : lines) {
            used += rendered.length() + 1;
        }

        if (used < shorten(description).length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, truncateToWidth(metrics, lines.get(last), width));
        }

        StringBuilder html = new StringBuilder("<html>");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                html.append("<br>");
            }

            html.append(escapeHtml(lines.get(i)));
        }

        return html.append("</html>").toString();
    }

    private static String truncateToWidth(FontMetrics metrics, String text, int width) {
        String candidate = text + ELLIPSIS;

        while (candidate.length() > ELLIPSIS.length() + 1 && metrics.stringWidth(candidate) > width) {
            text = text.substring(0, text.length() - 1);
            candidate = text + ELLIPSIS;
        }

        return candidate;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JComponent buildActions(AbstractButton primary, AbstractButton... overflow) {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        MD3Button install = MD3Button.filled(primary.getText(), null);
        install.setEnabled(primary.isEnabled());
        install.addActionListener(e -> primary.doClick());
        actions.add(install, BorderLayout.WEST);

        List<AbstractButton> rest = new ArrayList<>();

        for (AbstractButton button : overflow) {
            if (button != null && button.isVisible()) {
                rest.add(button);
            }
        }

        if (!rest.isEmpty()) {
            MD3IconButton more = new MD3IconButton(MD3Icons.MORE_VERT, GetText.tr("More options"));
            more.addActionListener(e -> {
                JPopupMenu menu = new JPopupMenu();

                for (AbstractButton button : rest) {
                    MD3Menus.addAction(menu, button);
                }

                menu.show(more, 0, more.getHeight());
            });

            actions.add(more, BorderLayout.EAST);
        }

        return actions;
    }

    /**
     * A download count as a badge, in the shortened form people actually read.
     */
    protected static void addDownloads(List<MD3Badge> badges, long downloads) {
        if (downloads <= 0) {
            return;
        }

        // #. {0} is a download count, already shortened - "1.2M", "45K"
        badges.add(MD3Badge.neutral(GetText.tr("{0} downloads", shortenCount(downloads))));
    }

    private static String shortenCount(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000d);
        }

        if (count >= 1_000) {
            return String.format(Locale.ROOT, "%.0fK", count / 1_000d);
        }

        return Long.toString(count);
    }

    /**
     * Trims a summary to about two lines. Platforms return anything from a sentence to a full
     * README, and a card that grows to fit one breaks the grid for all of them.
     *
     * <p>
     * Some summaries arrive as Markdown. Rendering it properly would mean an HTML view per card,
     * which is what the old cards did and what made them 155px tall regardless of content; at this
     * length, dropping the syntax reads better than honouring it.
     */
    private static String shorten(String description) {
        String flat = description
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("[*_`#>]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (flat.length() <= DESCRIPTION_LIMIT) {
            return flat;
        }

        int cut = flat.lastIndexOf(' ', DESCRIPTION_LIMIT);

        // written as an escape: the build sets no source encoding, so a literal ellipsis fails to
        // compile wherever the platform default is not UTF-8
        return flat.substring(0, cut < 0 ? DESCRIPTION_LIMIT : cut) + ELLIPSIS;
    }

    /**
     * Every card is the same width so the grid stays regular; height follows the content.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = UIScale.scale(CARD_WIDTH);

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
