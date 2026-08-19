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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.gui.dialogs.PackDescriptionDialog;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.MD3Menus;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3ButtonBar;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Html;
import com.atlauncher.workers.BackgroundImageWorker;
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
public abstract class MD3PackCard extends MD3Card implements CardGridLayout.WidthAware {
    /** The narrowest a card is laid out at, and what everything below is measured against. */
    public static final int CARD_WIDTH = 280;

    /** The widest the grid may stretch one to. */
    public static final int MAX_CARD_WIDTH = 400;

    /** 16:9 against the card width, whatever that turns out to be. */
    protected static final int COVER_HEIGHT = coverHeight(CARD_WIDTH);

    protected static final int MAX_BADGES = 3;

    /** Roughly two lines at body-small in a 280dp card. */
    private static final int DESCRIPTION_LIMIT = 84;

    /** About a paragraph - enough to decide from, short of being a document in a tooltip. */
    private static final int TOOLTIP_LIMIT = 320;

    /** Characters per line in the tooltip, so it cannot come out as wide as its longest sentence. */
    private static final int TOOLTIP_WRAP = 64;

    /** Marks a summary that was cut short of its character limit before it was even wrapped. */
    private static final String ELLIPSIS = "\u2026";

    private JPanel coverWrapper;
    private JPanel actionsHolder;
    private MD3FittingLabel titleLabel;
    private JLabel summary;
    private String title;
    private String description;
    private Supplier<String> descriptionLoader;

    /** Scaled; -1 until the grid has said how wide this card is. */
    private int layoutWidth = -1;

    protected MD3PackCard() {
        super(Variant.FILLED, new BorderLayout());

        setHoverElevation(true);

        // the cover runs to the card's edges, so padding belongs to the body
        setBorder(null);
    }

    private static int coverHeight(int width) {
        return Math.round(width * 9f / 16f);
    }

    /**
     * Takes the width the grid worked out and re-measures everything that depends on it: the cover
     * keeps its aspect, and the summary re-wraps rather than leaving the extra width blank.
     */
    @Override
    public void setLayoutWidth(int width) {
        if (width <= 0 || width == layoutWidth) {
            return;
        }

        layoutWidth = width;

        if (coverWrapper != null) {
            coverWrapper.setPreferredSize(new Dimension(width, coverHeight(width)));
        }

        refreshSummary();
        refreshTitle();
    }

    private int contentWidth() {
        int width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return width - UIScale.scale(MD3Spacing.L) - UIScale.scale(MD3Spacing.S);
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
     * Artwork from a URL, fetched in the background.
     *
     * <p>
     * Fetched at the widest a card can be and scaled down to whatever it actually got, rather than
     * re-fetched every time the window is dragged. Scaled to fit, not to fill: most platforms give
     * a square icon rather than a banner, and cropping one to the cover's shape cuts the middle out
     * of the logo.
     */
    protected static JComponent coverFromUrl(String url) {
        return new Cover(url);
    }

    private static final class Cover extends JLabel {
        Cover(String url) {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVisible(url == null || url.isEmpty());

            if (url != null && !url.isEmpty()) {
                new BackgroundImageWorker(this, url, UIScale.scale(MAX_CARD_WIDTH),
                        UIScale.scale(coverHeight(MAX_CARD_WIDTH))).execute();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Icon icon = getIcon();

            if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                super.paintComponent(g);

                return;
            }

            double scale = Math.min(getWidth() / (double) icon.getIconWidth(),
                    getHeight() / (double) icon.getIconHeight());

            Graphics2D g2 = MD3Paint.setup(g);

            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.translate((getWidth() - icon.getIconWidth() * scale) / 2,
                        (getHeight() - icon.getIconHeight() * scale) / 2);
                g2.scale(scale, scale);
                icon.paintIcon(this, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }
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

        coverWrapper = wrapper;

        return wrapper;
    }

    private JComponent buildBody(String title, String description, List<MD3Badge> badges, AbstractButton primary,
            AbstractButton... overflow) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.M, MD3Spacing.S));

        titleLabel = new MD3FittingLabel(title, 2);
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, title));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setOverflowTip(title);
        body.add(titleLabel);

        body.add(buildSummary(title, description));

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

        // leftover height is the grid matching this card to a taller neighbour. Without somewhere
        // to put it, BoxLayout hands it to the action row, and Install comes out a different size
        // on every card that sat next to a longer summary
        body.add(Box.createVerticalGlue());

        // cap the holder's height so a BorderLayout cannot stretch the bar the way the old west/east
        // row stretched Install to the overflow's 48dp target
        actionsHolder = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();

                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }
        };
        actionsHolder.setOpaque(false);
        actionsHolder.setAlignmentX(LEFT_ALIGNMENT);
        actionsHolder.add(buildActions(primary, overflow), BorderLayout.CENTER);

        body.add(actionsHolder);

        return body;
    }

    /**
     * Rebuilds the action row against state that has just changed.
     *
     * <p>
     * The mod browser needs this: a card offers "Add" until the mod is installed and "Remove" once
     * it is, and the install happens in a dialog the card opened. Without it the card would go on
     * offering to add something the user has just added, until the whole grid was reloaded - which
     * means another search request for a change the launcher already knows about.
     */
    protected void refreshActions(AbstractButton primary, AbstractButton... overflow) {
        if (actionsHolder == null) {
            return;
        }

        actionsHolder.removeAll();
        actionsHolder.add(buildActions(primary, overflow), BorderLayout.CENTER);
        actionsHolder.revalidate();
        actionsHolder.repaint();
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
     *
     * <p>
     * Where there is more description than two lines, the summary opens
     * {@link PackDescriptionDialog}. A tooltip cannot be the answer to "what is this pack" - it was
     * the answer here, carrying the whole raw description, and for a pack whose author wrote a
     * README that meant a plain-text tooltip laid out at the width of its longest line, covering the
     * window until the pointer moved.
     */
    private JComponent buildSummary(String title, String description) {
        Font font = MD3Type.font(MD3Type.BODY_SMALL);

        this.title = title;
        this.description = description;
        this.summary = new JLabel();

        summary.setFont(font);
        summary.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        summary.setForeground(MD3Color.onSurfaceVariant());
        summary.setAlignmentX(LEFT_ALIGNMENT);
        summary.setVerticalAlignment(SwingConstants.TOP);

        if (PackDescriptionDialog.hasSomethingToShow(description)) {
            // a hint, at a width that reads - not the document, which has somewhere of its own now
            summary.setToolTipText(previewOf(description));
            summary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            summary.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        showDescription();
                    }
                }
            });
        }

        refreshSummary();

        return summary;
    }

    /**
     * Points the description dialog at the whole description, for the platforms whose search results
     * only carry a summary.
     *
     * <p>
     * Call it before {@link #build}. Not a {@code build} parameter because four of the six platforms
     * have nothing to fetch - they hand over everything they have in the search response - and the
     * signature is long enough.
     *
     * @param loader called off the event thread, once, when the dialog is opened
     */
    protected void setDescriptionLoader(Supplier<String> loader) {
        this.descriptionLoader = loader;
    }

    private void showDescription() {
        PackDescriptionDialog.show(title, description, descriptionLoader);
    }

    /**
     * The first couple of sentences, flattened and wrapped, for the tooltip.
     *
     * <p>
     * Bounded on both axes on purpose: Swing lays a tooltip out at whatever size its content asks
     * for and will happily make one larger than the screen.
     */
    private static String previewOf(String description) {
        String flat = shorten(description, TOOLTIP_LIMIT);

        return new HTMLBuilder().text(MD3Text.escapeHtml(flat)).split(TOOLTIP_WRAP).build();
    }

    private void refreshTitle() {
        if (titleLabel != null) {
            titleLabel.fitTo(contentWidth());
        }
    }

    /**
     * Re-wraps the summary to the card's current width. Pinned to two lines at every width - a card
     * that grew a third line as the window widened would break its row's baseline.
     */
    private void refreshSummary() {
        if (summary == null) {
            return;
        }

        int width = contentWidth();
        FontMetrics metrics = summary.getFontMetrics(summary.getFont());

        summary.setText(wrapToTwoLines(metrics, description, width));

        Dimension fixed = new Dimension(width, metrics.getHeight() * 2);
        summary.setPreferredSize(fixed);
        summary.setMinimumSize(fixed);
        summary.setMaximumSize(fixed);
    }

    /**
     * Breaks a summary across two lines that are guaranteed to fit.
     */
    private static String wrapToTwoLines(FontMetrics metrics, String description, int width) {
        if (description == null || description.trim().isEmpty()) {
            return " ";
        }

        // the character limit is measured against the narrowest card, so a stretched one is allowed
        // proportionally more of the summary rather than being trimmed to fit a card it is not
        int limit = DESCRIPTION_LIMIT * width / UIScale.scale(CARD_WIDTH - MD3Spacing.L - MD3Spacing.S);

        return MD3Text.wrapToLines(metrics, shorten(description, limit), width, 2);
    }

    /**
     * The same row the instance and server cards use. A {@link BorderLayout} here used to stretch
     * Install to the overflow's 48dp target, so the one labelled button on the card sat at a
     * different size from the overflow it shared the row with, and from every other control on
     * the page.
     */
    private JComponent buildActions(AbstractButton primary, AbstractButton... overflow) {
        MD3ButtonBar actions = new MD3ButtonBar();
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        // SMALL: a card action, and the size the overflow already is. Medium made a short label
        // like "Install" or "Add" into a 40dp stadium next to a 32dp icon
        MD3Button install = MD3Button.filled(primary.getText(), null).withButtonSize(MD3Button.Size.SMALL);
        install.setEnabled(primary.isEnabled());
        install.addActionListener(e -> primary.doClick());
        actions.leading(install);

        List<AbstractButton> rest = new ArrayList<>();

        for (AbstractButton button : overflow) {
            if (button != null && button.isVisible()) {
                rest.add(button);
            }
        }

        boolean describable = PackDescriptionDialog.hasSomethingToShow(description) || descriptionLoader != null;

        if (!rest.isEmpty() || describable) {
            MD3IconButton more = new MD3IconButton(MD3Icons.MORE_VERT, GetText.tr("More options"),
                    MD3IconButton.Variant.STANDARD, MD3IconButton.Size.SMALL);
            more.addActionListener(e -> {
                JPopupMenu menu = new JPopupMenu();

                // first, and separated from the actions: reading about a pack is what you do before
                // deciding to do any of them, and it is the only one that changes nothing
                if (describable) {
                    JMenuItem describe = new JMenuItem(GetText.tr("Description"));
                    describe.addActionListener(chosen -> showDescription());
                    menu.add(describe);

                    if (!rest.isEmpty()) {
                        menu.addSeparator();
                    }
                }

                for (AbstractButton button : rest) {
                    MD3Menus.addAction(menu, button);
                }

                menu.show(more, 0, more.getHeight());
            });

            actions.trailing(more);
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
     * Some summaries arrive as Markdown, others as HTML. Rendering either properly would mean an
     * HTML view per card, which is what the old cards did and what made them 155px tall regardless
     * of content; at this length, dropping the markup reads better than honouring it.
     */
    private static String shorten(String description, int limit) {
        String flat = Html.toPlain(description);

        if (flat.length() <= limit) {
            return flat;
        }

        int cut = flat.lastIndexOf(' ', limit);

        return flat.substring(0, cut < 0 ? limit : cut) + ELLIPSIS;
    }

    /**
     * The width the grid gave this card, or its natural one until it has been asked. Height follows
     * the content, and every card in a row is given the same width, so the row stays regular.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return size;
    }
}
