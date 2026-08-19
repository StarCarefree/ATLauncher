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
package com.atlauncher.gui.tabs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.gui.md3.MD3Gallery;
import com.atlauncher.gui.md3.MD3Html;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The About page's licence and third-party list are HTML documents run through
 * {@code HTMLBuilder}. Headings and licence links used to be escaped into visible source.
 */
public class AboutDocumentsRenderTest {
    private static final int WIDTH = 720;
    private static final int HEIGHT = 360;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static String documentHtml(String resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                AboutDocumentsRenderTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            return new HTMLBuilder().text(reader.lines().collect(Collectors.joining("<br/>"))).build();
        }
    }

    private static void paint(String html, String name) throws Exception {
        JEditorPane document = MD3Html.pane(html);
        JScrollPane scroll = new JScrollPane(document);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        scroll.setSize(new Dimension(WIDTH, HEIGHT));
        scroll.doLayout();
        scroll.getViewport().doLayout();
        document.setSize(WIDTH - 16, HEIGHT);

        JPanel host = new JPanel();
        host.setOpaque(true);
        host.setBackground(MD3Color.surface());
        host.setSize(new Dimension(WIDTH, HEIGHT));
        host.setLayout(null);
        scroll.setBounds(0, 0, WIDTH, HEIGHT);
        host.add(scroll);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, WIDTH, HEIGHT);
        host.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/" + name));
    }

    @Test
    public void testThirdPartyHeadingsStayHeadings() throws Exception {
        String html = documentHtml("/THIRDPARTYLIBRARIES");

        assertTrue(html.contains("<h2>Build Dependencies</h2>"),
                "the libraries heading was escaped");
        assertTrue(html.contains("<h2>Application Dependencies</h2>"),
                "the application heading was escaped");
        assertFalse(html.contains("&lt;h2"), "the libraries list is still showing the heading tags");

        paint(html, "about-libraries-dark.png");
    }

    @Test
    public void testTheLicenceKeepsItsLinks() throws Exception {
        String html = documentHtml("/LICENSE");

        assertTrue(html.contains("href=\"https://github.com/ATLauncher/ATLauncher\""),
                "the project link is not a link");
        assertTrue(html.contains("href=\"https://www.gnu.org/licenses/\""),
                "the GPL link is not a link");
        assertFalse(html.contains("&lt;a"), "the licence is still showing the tags instead of the links");

        paint(html, "about-license-dark.png");

        boolean themedLink = false;
        BufferedImage image = ImageIO.read(new File("build/md3-preview/about-license-dark.png"));

        for (int y = 0; y < image.getHeight() && !themedLink; y += 2) {
            for (int x = 0; x < image.getWidth() && !themedLink; x += 2) {
                themedLink = image.getRGB(x, y) == MD3Color.primary().getRGB();
            }
        }

        assertTrue(themedLink, "nothing in the licence is the theme's link colour");
    }
}
