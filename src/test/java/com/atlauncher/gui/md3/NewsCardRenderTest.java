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
package com.atlauncher.gui.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.data.AbstractNews;
import com.atlauncher.data.News;
import com.atlauncher.data.Settings;
import com.atlauncher.gui.card.NewsCard;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints real news articles as cards.
 *
 * <p>
 * The page was one {@link JEditorPane} of every article concatenated; it is now a card each, which
 * means each article's HTML has to be measured on its own against a width nobody knows until the
 * window has one. An HTML pane that is never told its width reports the height of a single very long
 * line, so the failure mode is a page of one-line cards - which is what these assertions are for.
 */
public class NewsCardRenderTest {
    private static final int PAGE_WIDTH = 1000;
    private static final int PAGE_HEIGHT = 620;

    private static final String LONG_ARTICLE = "Previously we had to remove FTB packs from the launcher at the "
            + "request of the old FTB CEO. Recently FTB have changed CEO and the new CEO has once again allowed "
            + "us to list FTB packs on the launcher. This is great news and we have released an update to add FTB "
            + "packs back again through the FTB Packs tab. If you had previous instances from before we had to "
            + "remove the functionality, they should show updates and allow reinstallation again.";

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        // the date on a card is formatted to the user's preference, which has to exist
        App.settings = new Settings();
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static AbstractNews article(String title, String content) {
        String json = Gsons.DEFAULT.toJson(new String[0]);
        News news = Gsons.DEFAULT.fromJson("{\"title\":" + Gsons.DEFAULT.toJson(title) + ",\"content\":"
                + Gsons.DEFAULT.toJson(content) + ",\"created_at\":\"2026-07-24T20:55:33.000000Z\"}", News.class);

        assertNotNull(news, "the fixture did not deserialise" + json);

        return new AbstractNews(news);
    }

    private static JEditorPane findBody(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JEditorPane) {
                return (JEditorPane) c;
            }

            if (c instanceof Container) {
                JEditorPane found = findBody((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private JPanel buildPage(int width) {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setOpaque(true);
        page.setBackground(MD3Color.surface());
        page.setBorder(MD3Spacing.border(MD3Spacing.L));

        NewsCard first = new NewsCard(article("FTB Packs Available Again", "<p>" + LONG_ARTICLE + "</p>"));
        first.setAlignmentX(Component.CENTER_ALIGNMENT);
        page.add(first);
        page.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.M)));

        NewsCard second = new NewsCard(article("Serialization Is Bad",
                "<p>There is an exploit going around. See <a href=\"https://atlauncher.com\">the details</a>.</p>"));
        second.setAlignmentX(Component.CENTER_ALIGNMENT);
        page.add(second);

        page.setSize(new Dimension(width, PAGE_HEIGHT));
        layoutTree(page);

        // a card only learns its width by being laid out, and only then can it measure its article,
        // which changes the height it asks for. In the running launcher the card's revalidate
        // schedules the second pass; laying it out by hand, the column has to be invalidated so its
        // BoxLayout drops the sizes it cached before the articles had been measured
        page.invalidate();
        layoutTree(page);

        return page;
    }

    @Test
    public void testNewsRenders() throws Exception {
        JPanel page = buildPage(PAGE_WIDTH);

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        page.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/news-dark.png"));
    }

    @Test
    public void testAnArticleIsMeasuredAgainstTheWidthItGets() {
        JPanel page = buildPage(PAGE_WIDTH);
        NewsCard card = (NewsCard) page.getComponent(0);
        JEditorPane body = findBody(card);

        assertNotNull(body, "the card lost its body");
        assertEquals(card.getWidth() - card.getInsets().left - card.getInsets().right, body.getWidth(),
                "the body was not given the card's width, so its text runs past the edge");

        // a paragraph of that length cannot fit on one line at this width; if it reports as one, it
        // was measured before anything told it how wide it is
        assertTrue(body.getHeight() > body.getFontMetrics(body.getFont()).getHeight() * 2,
                "the article collapsed to a single line, so the card has swallowed most of it");
    }

    @Test
    public void testTheReadingColumnIsCapped() {
        JPanel wide = buildPage(2400);
        NewsCard card = (NewsCard) wide.getComponent(0);

        assertTrue(card.getWidth() < 2400 - UIScale.scale(MD3Spacing.L) * 2,
                "the article runs the full width of the window, which is a line nobody can track back");

        JPanel narrow = buildPage(700);
        NewsCard narrowCard = (NewsCard) narrow.getComponent(0);

        assertTrue(narrowCard.getWidth() > card.getWidth() / 2,
                "a narrow window does not use the width it has");
        assertTrue(narrowCard.getHeight() > card.getHeight(),
                "the same article is no taller in a narrower column, so it is not re-wrapping");
    }
}
