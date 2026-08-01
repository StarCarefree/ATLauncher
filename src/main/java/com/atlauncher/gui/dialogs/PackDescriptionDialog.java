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
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.gui.md3.feedback.MD3Dialog;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Markdown;
import com.atlauncher.utils.OS;
import com.formdev.flatlaf.util.UIScale;

/**
 * A modpack's description, in full and laid out to be read.
 *
 * <p>
 * There was no way to read one. The card shows two lines and the rest was a <em>tooltip</em> holding
 * the raw description - which for a pack whose author wrote a README meant a plain-text tooltip
 * several thousand characters long, laid out at the width of its longest line, covering the whole
 * window until the pointer moved. It also showed the Markdown as typed: literal {@code ##} headings,
 * {@code **bold**} and {@code [text](url)} links. The only real way to find out what a pack was
 * about was to open its website in a browser.
 *
 * <p>
 * {@link Markdown} had been in the tree, with commonmark as a shipped dependency, and was called
 * from nowhere. This is what it was for.
 *
 * <p>
 * CurseForge and Modrinth only put a one-line summary in their search results, so for those the
 * dialog opens on the summary and fetches the real description behind it - which is the difference
 * between this and a bigger copy of what the card already said.
 */
public final class PackDescriptionDialog {
    /** Wider than a basic dialog: this holds something to read, not something to answer. */
    private static final int DIALOG_WIDTH = 720;

    /** Tall enough for a long description to be worth scrolling, short of filling the screen. */
    private static final int DOCUMENT_HEIGHT = 420;

    /** Below this a dialog stops reading as a document and starts reading as a cut-off one. */
    private static final int MIN_DOCUMENT_HEIGHT = 140;

    /**
     * A bare URL in prose. Excludes the brackets Markdown uses for its own links, so one that is
     * already written as a link is left alone, and stops short of the punctuation that ends the
     * sentence rather than swallowing it into the address.
     */
    private static final String BARE_URL = "(?<![(<\\w])(https?://[^\\s<>()\\[\\]\"']*[^\\s<>()\\[\\]\"'.,;:!?])";

    private PackDescriptionDialog() {
    }

    public static boolean hasSomethingToShow(String description) {
        return description != null && !description.trim().isEmpty();
    }

    public static void show(String name, String description) {
        show(name, description, null);
    }

    /**
     * @param name        the pack, which becomes the headline - a description with no idea what it
     *                    describes is no use once the card behind it is covered
     * @param description what the card had: Markdown, HTML or plain prose, since the platforms each
     *                    hand over something different
     * @param full        fetches the whole description, or null where the platform already gave it.
     *                    Called off the event thread; may return null, which leaves the summary up
     */
    public static void show(String name, String description, Supplier<String> full) {
        if (!hasSomethingToShow(description) && full == null) {
            return;
        }

        JEditorPane pane = buildPane(description);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(buildScroller(pane, full != null), BorderLayout.CENTER);

        JPanel loading = buildLoadingRow();

        if (full != null) {
            // added before the dialog is packed, so the window has room for it; hiding it later
            // gives the space back to the document rather than resizing a window already on screen
            content.add(loading, BorderLayout.SOUTH);
        }

        MD3Dialog dialog = MD3Dialog.builder(DialogManager.parentWindow())
                .title(name)
                .headline(name)
                .maxWidth(DIALOG_WIDTH)
                .content(content)
                .dismiss(GetText.tr("Close"))
                .build();

        if (full != null) {
            fetchInto(dialog, pane, loading, full);
        }

        dialog.showAndWait();
    }

    /**
     * Replaces the summary with the whole description once it arrives.
     */
    private static void fetchInto(MD3Dialog dialog, JEditorPane pane, JPanel loading, Supplier<String> full) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return full.get();
            }

            @Override
            protected void done() {
                loading.setVisible(false);

                if (isCancelled()) {
                    return;
                }

                try {
                    String fetched = get();

                    if (hasSomethingToShow(fetched)) {
                        pane.setText(asHtml(fetched));
                        pane.setCaretPosition(0);
                    }
                } catch (Exception e) {
                    // the summary is already up, which is better than an error where a description
                    // should be - the log is where a failed fetch belongs
                    LogManager.logStackTrace("Failed to fetch the full pack description", e);
                }
            }
        };

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                worker.cancel(true);
            }
        });

        worker.execute();
    }

    /**
     * The document itself, without the dialog around it. Package-private so a test can render one.
     */
    static JEditorPane buildPane(String description) {
        JEditorPane pane = new JEditorPane();

        pane.setEditorKit(buildEditorKit());
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(null);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                OS.openWebBrowser(e.getURL());
            }
        });

        pane.setText(asHtml(description));
        // without this the pane keeps the caret where the last link landed, and a document with any
        // link in it opens scrolled to the first one rather than to the top
        pane.setCaretPosition(0);

        return pane;
    }

    /**
     * @param expectMore whether a fetch is on its way. The dialog is packed once, so it can only be
     *                   sized to what it has - and a two-sentence summary about to be replaced by a
     *                   README would leave that README in a box a few lines tall
     */
    private static JScrollPane buildScroller(JEditorPane pane, boolean expectMore) {
        JScrollPane scroller = new JScrollPane(pane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(UIScale.scale(16));
        scroller.setPreferredSize(new Dimension(UIScale.scale(DIALOG_WIDTH - MD3Spacing.XL * 2),
                heightFor(pane, expectMore)));

        SwingUtilities.invokeLater(() -> scroller.getVerticalScrollBar().setValue(0));

        return scroller;
    }

    /**
     * As tall as the description needs, up to the point where scrolling is the better answer. A
     * fixed height leaves a three paragraph description sitting in half a window of empty surface.
     */
    private static int heightFor(JEditorPane pane, boolean expectMore) {
        int width = UIScale.scale(DIALOG_WIDTH - MD3Spacing.XL * 2);
        int max = UIScale.scale(DOCUMENT_HEIGHT);

        if (expectMore) {
            return max;
        }

        // an HTML view only knows its height once it has been given a width to wrap against
        pane.setSize(width, Short.MAX_VALUE);

        return Math.max(UIScale.scale(MIN_DOCUMENT_HEIGHT), Math.min(max, pane.getPreferredSize().height));
    }

    private static JPanel buildLoadingRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));
        row.setOpaque(false);
        row.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        JLabel label = new JLabel(GetText.tr("Loading..."));
        label.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, label.getText()));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        label.setForeground(MD3Color.onSurfaceVariant());

        row.add(MD3CircularProgress.inline());
        row.add(label);

        return row;
    }

    /**
     * Most descriptions are Markdown, a few arrive as HTML already, and ATLauncher's own are plain
     * prose - which Markdown renders correctly as a paragraph, so only the HTML needs telling apart.
     *
     * <p>
     * Running HTML through the Markdown renderer would escape it and show the tags; running Markdown
     * through as HTML would show the syntax. Either way round the reader gets the source instead of
     * the text.
     */
    static String asHtml(String description) {
        if (!hasSomethingToShow(description)) {
            return "";
        }

        if (looksLikeHtml(description)) {
            return description;
        }

        // prose keeps the shape its author gave it; Markdown follows Markdown's rules, where a
        // single newline is a space and paragraphs are wrapped in the source on purpose
        return Markdown.render(linkify(description), !looksLikeMarkdown(description));
    }

    /**
     * Whether the author was writing Markdown or just typing.
     *
     * <p>
     * Deliberately looks for block syntax at the start of a line and for link syntax rather than for
     * emphasis: a description saying "the *best* pack" is prose with a stray asterisk in it, not a
     * document, and treating it as one loses every line break the author put in.
     */
    private static boolean looksLikeMarkdown(String description) {
        return description.matches("(?s).*\\]\\(.*") || description.matches("(?m)(?s).*^\\s{0,3}(#{1,6} |[-*+] |> |```).*");
    }

    /**
     * Wraps bare URLs so they come out clickable.
     *
     * <p>
     * Pack authors write "Website: https://example.com" rather than a Markdown link, and CommonMark
     * only autolinks what is in angle brackets - so the address most descriptions end on rendered as
     * text you had to retype into a browser. The brackets are the parser's own syntax, so this needs
     * no extension to it.
     */
    static String linkify(String markdown) {
        return markdown.replaceAll(BARE_URL, "<$1>");
    }

    private static boolean looksLikeHtml(String description) {
        return description.matches("(?is).*<(p|br|div|ul|ol|li|h[1-6]|strong|em|a\\s)[^>]*>.*");
    }

    /**
     * The document takes the theme's own type and colours rather than the editor kit's defaults,
     * which are a black-on-white serif face that looks like a different application.
     */
    private static HTMLEditorKit buildEditorKit() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styles = new StyleSheet();

        Font body = MD3Type.font(MD3Type.BODY_MEDIUM);
        // the type scale puts titleSmall and bodyMedium at the same size, so a document styled with
        // the two next to each other has headings that are only a shade of grey apart from the
        // paragraphs under them - two steps up the scale is what makes it read as a document
        Font major = MD3Type.font(MD3Type.TITLE_LARGE);
        Font minor = MD3Type.font(MD3Type.TITLE_MEDIUM);

        // this is what the reader came for, so it takes onSurface rather than the muted variant
        styles.addRule("body { font-family: " + body.getFamily() + "; font-size: " + body.getSize() + "pt; color: "
                + hex(MD3Color.onSurface()) + "; background: " + hex(MD3Elevation.surface(MD3Elevation.LEVEL3))
                + "; margin: 0; }");
        styles.addRule("p { margin: 0 0 " + MD3Spacing.M + "px 0; }");
        styles.addRule("h1, h2 { font-family: " + major.getFamily() + "; font-size: " + major.getSize()
                + "pt; font-weight: bold; color: " + hex(MD3Color.onSurface()) + "; margin: " + MD3Spacing.L + "px 0 "
                + MD3Spacing.S + "px 0; }");
        styles.addRule("h3, h4, h5, h6 { font-family: " + minor.getFamily() + "; font-size: " + minor.getSize()
                + "pt; font-weight: bold; color: " + hex(MD3Color.onSurface()) + "; margin: " + MD3Spacing.M + "px 0 "
                + MD3Spacing.XS + "px 0; }");
        styles.addRule("a { color: " + hex(MD3Color.primary()) + "; }");
        styles.addRule("ul, ol { margin: 0 0 " + MD3Spacing.M + "px " + MD3Spacing.L + "px; }");
        styles.addRule("li { margin: 0 0 " + MD3Spacing.XS + "px 0; }");
        styles.addRule("code, pre { font-family: monospaced; color: " + hex(MD3Color.onSurfaceVariant()) + "; }");
        styles.addRule("blockquote { margin: 0 0 " + MD3Spacing.M + "px " + MD3Spacing.M + "px; color: "
                + hex(MD3Color.onSurfaceVariant()) + "; }");
        styles.addRule("hr { border-color: " + hex(MD3Color.outlineVariant()) + "; }");

        kit.setStyleSheet(styles);

        return kit;
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
