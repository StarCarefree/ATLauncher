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
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;

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
    public void testHtmlDescriptionsAreLeftAlone() {
        String html = "<p>Already <strong>html</strong>.</p>";

        assertEquals(html, PackDescriptionDialog.asHtml(html));
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
}
