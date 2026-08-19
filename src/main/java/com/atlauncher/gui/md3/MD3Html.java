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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.atlauncher.themes.UiFonts;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Html;
import com.atlauncher.utils.OS;

/**
 * A short HTML fragment with working links, styled in the theme.
 *
 * <p>
 * The accounts empty state and the Microsoft login prompt both tell the user to open a page, and
 * both used to put that instruction through {@link com.atlauncher.builders.HTMLBuilder} into a
 * bare {@link JEditorPane}. The builder escaped the {@code <a href>}, so the link rendered as
 * source, and the pane used the kit's black-on-white defaults on a dark surface.
 */
public final class MD3Html {
    private MD3Html() {
    }

    public static JEditorPane pane(String html) {
        JEditorPane pane = new JEditorPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public Dimension getPreferredSize() {
                int width = getWidth();

                if (width <= 0) {
                    return super.getPreferredSize();
                }

                // an HTML view reports one long line until it is given a width to wrap against
                setSize(width, Short.MAX_VALUE);

                return new Dimension(width, super.getPreferredSize().height);
            }
        };

        pane.setEditorKit(snippetKit());
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setBorder(null);
        pane.setHighlighter(null);
        pane.setText(html == null ? "" : html);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }

            String href = Html.hrefOf(e.getURL(), e.getDescription());

            if (href != null) {
                OS.openWebBrowser(href);
            }
        });

        return pane;
    }

    /**
     * Pins the fragment to a reading width so it wraps instead of running off the page.
     */
    public static void wrapTo(JEditorPane pane, int width) {
        if (width <= 0) {
            return;
        }

        pane.setSize(width, Short.MAX_VALUE);

        Dimension size = new Dimension(width, pane.getPreferredSize().height);
        pane.setPreferredSize(size);
        pane.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }

    private static HTMLEditorKit snippetKit() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styles = new StyleSheet();
        styles.addStyleSheet(kit.getStyleSheet());

        Font body = MD3Type.font(MD3Type.BODY_MEDIUM);
        Font heading = MD3Type.font(MD3Type.TITLE_SMALL);
        String bodyStack = cssFontStack(body);
        String headingStack = cssFontStack(heading);

        styles.addRule("body { font-family: " + bodyStack + "; font-size: " + body.getSize()
                + "pt; color: " + hex(MD3Color.onSurfaceVariant()) + "; background: transparent; margin: 0; }");
        styles.addRule("p { margin: 0; }");
        styles.addRule("a { color: " + hex(MD3Color.primary()) + "; text-decoration: underline; }");
        styles.addRule("b, strong { font-weight: bold; color: " + hex(MD3Color.onSurface()) + "; }");
        styles.addRule("h1, h2, h3 { font-family: " + headingStack + "; font-size: " + heading.getSize()
                + "pt; font-weight: bold; color: " + hex(MD3Color.onSurface()) + "; margin: " + MD3Spacing.M
                + "px 0 " + MD3Spacing.S + "px 0; }");

        kit.setStyleSheet(styles);

        return kit;
    }

    /**
     * English face first, Chinese face next. Swing's HTML kit still picks one family for a
     * block, but a stack keeps CJK from falling through to a face that has no glyphs when the
     * fragment was not already marked up by {@link MD3MixedText#toHtml}.
     */
    private static String cssFontStack(Font font) {
        String latin = quoteFamily(font.getFamily());
        String cjk = quoteFamily(UiFonts.cjkFace(font).getFamily());

        if (latin.equals(cjk)) {
            return latin;
        }

        return latin + ", " + cjk;
    }

    private static String quoteFamily(String family) {
        return "'" + family.replace("'", "") + "'";
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
