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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.utils.Html;

/**
 * How a modpack's description reaches the user.
 *
 * <p>
 * The card shows two lines, and everything past them used to be a tooltip carrying the description
 * exactly as the platform sent it. For a pack whose author wrote a README that is a plain-text
 * tooltip several thousand characters long, laid out at the width of its longest line - which is how
 * a hover over the pack grid ended up covering the whole window - and it showed the Markdown as
 * typed rather than as text.
 */
public class PackDescriptionTest {
    private static final String MARKDOWN = "## All The Forge 10\n\n"
            + "A **big** pack with [a link](https://example.com) and a list:\n\n"
            + "- Applied Energistics 2\n"
            + "- Mekanism\n"
            + "- Create\n\n"
            + "### Setup\n\n"
            + "Allocate at least 5GB of RAM.\n";

    /** Long enough that showing it whole in a tooltip is the bug this is about. */
    private static String readme() {
        StringBuilder builder = new StringBuilder(MARKDOWN);

        while (builder.length() < 6000) {
            builder.append("Another paragraph about what this pack contains and how to play it.\n\n");
        }

        return builder.toString();
    }

    /** A card with nothing platform-specific about it, so the shared behaviour can be built. */
    private static final class TestPackCard extends MD3PackCard {
        TestPackCard(String description) {
            super();

            build("All The Forge 10", null, description, new ArrayList<>(), new JButton("Install"));
        }
    }

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static JLabel summaryOf(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getToolTipText() != null
                    && !((JLabel) c).getToolTipText().equals("All The Forge 10")) {
                return (JLabel) c;
            }

            if (c instanceof Container) {
                JLabel found = summaryOf((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * A tooltip is a hint. Swing lays one out at whatever size its content asks for and will happily
     * make one larger than the screen, so the bound has to be on the content.
     */
    @Test
    public void testTheTooltipIsAHintRatherThanTheWholeDocument() {
        String description = readme();
        JLabel summary = summaryOf(new TestPackCard(description));

        assertNotNull(summary, "the card has no summary to hover");

        String tooltip = summary.getToolTipText();

        assertTrue(tooltip.length() < description.length() / 4,
                "the tooltip is still carrying most of the description: " + tooltip.length() + " characters");
        assertTrue(tooltip.contains("<br/>"), "the tooltip has no line breaks, so it lays out as wide as it likes");
    }

    /**
     * A pack with a description gets somewhere to read it, whatever else the card offers.
     */
    @Test
    public void testTheCardOffersTheDescription() {
        assertTrue(PackDescriptionDialog.hasSomethingToShow("A pack."));
        assertFalse(PackDescriptionDialog.hasSomethingToShow("   "));
        assertFalse(PackDescriptionDialog.hasSomethingToShow(null));
    }

    @Test
    public void testMarkdownIsRenderedRatherThanShownAsTyped() {
        String html = PackDescriptionDialog.asHtml(MARKDOWN);

        assertTrue(html.contains("<h2>"), "a heading came through as text");
        assertTrue(html.contains("<strong>"), "bold came through as text");
        assertTrue(html.contains("<li>"), "a list came through as text");
        assertTrue(html.contains("href=\"https://example.com\""), "a link came through as text");

        assertFalse(html.contains("## All"), "the heading syntax is still in the output");
        assertFalse(html.contains("**big**"), "the emphasis syntax is still in the output");
    }

    /**
     * Pack authors write "Website: https://example.com" rather than a Markdown link, and the address
     * a description ends on is the one thing in it the reader might want to act on.
     */
    @Test
    public void testBareUrlsBecomeLinks() {
        String html = PackDescriptionDialog.asHtml("Website: https://pixelmonmod.com/ and that is all.");

        assertTrue(html.contains("href=\"https://pixelmonmod.com/\""), "a bare URL came through as text");

        // a URL already written as a link must not be wrapped again, which breaks both
        String linked = PackDescriptionDialog.linkify("See [the site](https://example.com) for more.");

        assertEquals("See [the site](https://example.com) for more.", linked);

        // and the full stop that ends the sentence is not part of the address
        assertEquals("Go to <https://example.com>.", PackDescriptionDialog.linkify("Go to https://example.com."));
    }

    /**
     * ATLauncher's own pack descriptions are typed as prose, with the links on their own lines.
     * Markdown joins lines like that, which ran the two addresses together into what looked like one
     * broken URL.
     */
    @Test
    public void testProseKeepsTheLineBreaksItWasWrittenWith() {
        String html = PackDescriptionDialog.asHtml("This pack needs 2gb of RAM.\n"
                + "Java 8 is required.\n\n"
                + "Website:\nhttps://example.com/\nhttps://example.org/");

        assertTrue(html.contains("<br />"), "the author's line breaks were thrown away");
        assertTrue(html.contains("href=\"https://example.com/\""), "the first address is not a link");
        assertTrue(html.contains("href=\"https://example.org/\""), "the second address is not a link");
    }

    /**
     * A real Markdown document wraps its paragraphs in the source and expects them joined - putting
     * a break at every newline there would leave every paragraph ragged.
     */
    @Test
    public void testMarkdownParagraphsAreStillJoined() {
        String html = PackDescriptionDialog.asHtml("## Heading\n\nA paragraph that happens\nto be wrapped.\n");

        assertFalse(html.contains("<br />"), "a wrapped Markdown paragraph was broken at its source line endings");
    }

    /**
     * A few platforms send HTML. Rendering that as Markdown escapes it and shows the tags, which is
     * the same failure as showing Markdown syntax, just the other way round.
     */
    @Test
    public void testHtmlDescriptionsAreKeptAsHtml() {
        String html = "<p>Already <strong>html</strong>.</p>";

        assertEquals(html, PackDescriptionDialog.asHtml(html));
    }

    /**
     * CurseForge authors paint with inline colour, drop in images, and embed a video. None of that
     * belongs in a themed dialog: the colours fight the surface, and JEditorPane fetching images
     * on the event thread is how opening a description used to freeze the launcher.
     */
    @Test
    public void testHtmlFromAPlatformIsSanitized() {
        String html = "<h2>All The Mods</h2>"
                + "<img src=\"https://example.com/banner.png\" alt=\"Banner\">"
                + "<p style=\"color:#ffffff\">White on CurseForge.</p>"
                + "<font color=\"red\">A warning.</font>"
                + "<iframe src=\"https://youtube.com/embed/x\"></iframe>"
                + "<ul><li>Quests</li></ul>";

        String shown = PackDescriptionDialog.asHtml(html);

        assertTrue(shown.contains("<h2>"), "the heading was thrown away with the junk");
        assertTrue(shown.contains("<li>Quests</li>"), "the list was thrown away with the junk");
        assertTrue(shown.contains("Banner"), "an image's alt text did not survive");
        assertTrue(shown.contains("White on CurseForge."), "the painted paragraph was dropped");
        assertTrue(shown.contains("A warning."), "font-wrapped text was dropped");
        assertFalse(shown.contains("<img"), "an image tag is still in the document");
        assertFalse(shown.contains("iframe"), "an embed is still in the document");
        assertFalse(shown.contains("color:#ffffff"), "inline colour is still in the document");
        assertFalse(shown.contains("<font"), "a font tag is still in the document");
    }

    /**
     * CurseForge wraps its banners and social buttons in {@code <a href>}. Stripping the picture
     * used to leave an empty tag, so the link was in the document and painted as nothing.
     */
    @Test
    public void testALinkedImageKeepsAClickableAddress() {
        String html = "<p><a href=\"https://example.com/pack\"><img src=\"x.png\" alt=\"Download\"></a></p>"
                + "<p><a href=\"/linkout?remoteUrl=https%3A%2F%2Fwiki.example.com\"><img src=\"y.png\"></a></p>";

        String shown = PackDescriptionDialog.asHtml(html);

        assertTrue(shown.contains("href=\"https://example.com/pack\""), "the banner's address was dropped");
        assertTrue(shown.contains("Download"), "the banner's alt text did not become the link's face");
        assertTrue(shown.contains("href=\"https://www.curseforge.com/linkout?remoteUrl="),
                "a relative linkout was not resolved against CurseForge");
        assertTrue(shown.contains("https://wiki.example.com"),
                "a picture-only linkout has no text, so the destination should be");
        assertFalse(shown.contains("<img"), "an image tag is still wrapping the link");
    }

    /**
     * A README that uses an HTML anchor rather than Markdown's {@code [text](url)} is still a
     * Markdown document. Treating it as HTML left the headings as hashes; treating it as Markdown
     * without rewriting the tag escaped the link into visible source.
     */
    @Test
    public void testAnHtmlAnchorInMarkdownStillBecomesALink() {
        String markdown = "## Setup\n\nSee <a href=\"https://wiki.example.com/start\">the wiki</a>.";

        String html = PackDescriptionDialog.asHtml(markdown);

        assertTrue(html.contains("<h2>"), "the heading was not rendered, so the file was treated as HTML");
        assertTrue(html.contains("href=\"https://wiki.example.com/start\""),
                "the HTML anchor came through as text");
        assertFalse(html.contains("&lt;a"), "the HTML anchor was escaped instead of rendered");
    }

    @Test
    public void testResolveHrefMakesRelativeAndBareAddressesOpenable() {
        assertEquals("https://example.com/a", Html.resolveHref("https://example.com/a"));
        assertEquals("http://192.168.1.8/wiki", Html.resolveHref("http://192.168.1.8/wiki"));
        assertEquals("https://cdn.example.com/x", Html.resolveHref("//cdn.example.com/x"));
        assertEquals("https://www.curseforge.com/minecraft/modpacks/foo",
                Html.resolveHref("/minecraft/modpacks/foo"));
        assertEquals("https://www.example.com/x", Html.resolveHref("www.example.com/x"));
        assertNull(Html.resolveHref("javascript:alert(1)"));
        assertNull(Html.resolveHref("#section"));
    }

    /**
     * An HTML description on a card used to show the tags: shorten escaped them instead of
     * dropping them, so a CurseForge summary read {@code <p>A large kitchen sink...}.
     */
    @Test
    public void testTheCardSummaryDoesNotShowHtmlTags() {
        TestPackCard card = new TestPackCard("<p>A <strong>large</strong> kitchen sink pack.</p>");
        JLabel summary = summaryOf(card);

        assertNotNull(summary, "the card has no summary to read");

        String visible = MD3Text.plain(summary.getText());

        assertTrue(visible.contains("large"), "the summary lost the words: " + visible);
        assertFalse(visible.contains("<p>"), "the summary is still showing tags: " + visible);
        assertFalse(visible.contains("&lt;"), "the tags were escaped rather than dropped: " + visible);
        assertEquals("A large kitchen sink pack.", Html.toPlain("<p>A <strong>large</strong> kitchen sink pack.</p>"));
    }

    @Test
    public void testTheDocumentRenders() throws Exception {
        JEditorPane pane = PackDescriptionDialog.buildPane(MARKDOWN);
        pane.setSize(640, 360);
        pane.doLayout();

        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(MD3Elevation.surface(MD3Elevation.LEVEL3));
            g.fillRect(0, 0, 640, 360);
            pane.paint(g);
        } finally {
            g.dispose();
        }

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/pack-description-dark.png"));

        // the editor kit's own defaults are black on white; the document has to have taken the
        // theme's instead, or the dialog looks like a different application
        boolean themed = false;

        for (int y = 0; y < 360 && !themed; y += 2) {
            for (int x = 0; x < 640 && !themed; x += 2) {
                themed = image.getRGB(x, y) == MD3Color.primary().getRGB();
            }
        }

        assertTrue(themed, "nothing in the document is the theme's link colour, so it kept the kit's defaults");
    }

    /**
     * What CurseForge actually sends: headings, a list, a table, a code block, and the junk
     * {@link #testHtmlFromAPlatformIsSanitized} already dropped.
     */
    @Test
    public void testHtmlFromAPlatformRenders() throws Exception {
        String html = "<h2>All The Mods 9</h2>"
                + "<p>A <b>kitchen sink</b> pack. See <a href=\"https://example.com\">the site</a>.</p>"
                + "<p><a href=\"/linkout?remoteUrl=https%3A%2F%2Fwiki.example.com\">"
                + "<img src=\"https://example.com/banner.png\" alt=\"Wiki\"></a></p>"
                + "<p style=\"color:#ffffff\">Quests, 400+ mods, and a custom progression.</p>"
                + "<ul><li>Applied Energistics 2</li><li>Mekanism</li><li>Create</li></ul>"
                + "<h3>Requirements</h3>"
                + "<table><tr><th>Loader</th><th>RAM</th></tr>"
                + "<tr><td>Forge 1.20.1</td><td>8 GB</td></tr></table>"
                + "<pre>allocate 8192M</pre>";

        JEditorPane pane = PackDescriptionDialog.buildPane(html);
        pane.setSize(640, 420);
        pane.doLayout();

        BufferedImage image = new BufferedImage(640, 420, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(MD3Elevation.surface(MD3Elevation.LEVEL3));
            g.fillRect(0, 0, 640, 420);
            pane.paint(g);
        } finally {
            g.dispose();
        }

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/pack-description-html-dark.png"));

        String shown = PackDescriptionDialog.asHtml(html);

        assertTrue(shown.contains("<table>"), "the table did not survive sanitizing");
        assertTrue(shown.contains("<pre>"), "the code block did not survive sanitizing");
        assertFalse(shown.contains("<img"), "an image tag is still in the rendered document");
    }
}
