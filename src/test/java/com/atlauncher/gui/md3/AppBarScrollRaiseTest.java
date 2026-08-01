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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;

/**
 * The top app bar raising itself once a page has scrolled underneath it.
 *
 * <p>
 * {@code setScrolled} had been written and was reachable from nothing - the launcher never called
 * it, so the header sat on the plain surface however far down a page you were, and there was no
 * boundary at all between the chrome and the content passing behind it.
 *
 * <p>
 * The tricky half is not the colour but finding what to follow. A page builds its own scroll pane
 * when it is shown and throws it away when it is not, several pages put their own toolbar between
 * the bar and the content, and one page attaches its scroller only once it has been told the
 * launcher has arrived on its tab.
 */
public class AppBarScrollRaiseTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /**
     * A page shaped like the launcher's: a toolbar of its own between the bar and a scrolling grid.
     */
    private static JPanel page(JPanel toolbar, JScrollPane scroller) {
        JPanel page = new JPanel(new BorderLayout());

        if (toolbar != null) {
            page.add(toolbar, BorderLayout.NORTH);
        }

        page.add(scroller, BorderLayout.CENTER);

        return page;
    }

    private static JScrollPane scroller() {
        JScrollPane scroller = new JScrollPane(Box.createRigidArea(new Dimension(200, 4000)),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setSize(200, 200);
        scroller.doLayout();

        return scroller;
    }

    @Test
    public void testTheBarFollowsThePageItIsGiven() {
        MD3TopAppBar bar = new MD3TopAppBar("Instances");
        JScrollPane scroller = scroller();

        bar.trackScroll(page(null, scroller));

        assertFalse(bar.isScrolled(), "the bar raised itself with the page still at the top");

        scroller.getVerticalScrollBar().setValue(240);
        assertTrue(bar.isScrolled(), "the page scrolled and the bar did not answer");

        scroller.getVerticalScrollBar().setValue(0);
        assertFalse(bar.isScrolled(), "the page came back to the top and the bar stayed raised");
    }

    /**
     * Pages are rebuilt every time they are shown, so the bar has to let go of the previous one -
     * otherwise a page that is no longer on screen keeps deciding what the header looks like.
     */
    @Test
    public void testTheBarLetsGoOfThePageItLeft() {
        MD3TopAppBar bar = new MD3TopAppBar("Instances");
        JScrollPane left = scroller();

        bar.trackScroll(page(null, left));
        left.getVerticalScrollBar().setValue(240);
        assertTrue(bar.isScrolled());

        // a page with nothing to scroll - the settings save bar, the tools grid
        bar.trackScroll(new JPanel());
        assertFalse(bar.isScrolled(), "arriving on a page with no scroller left the bar raised");

        left.getVerticalScrollBar().setValue(600);
        assertFalse(bar.isScrolled(), "the page the bar had left was still driving the header");
    }

    /**
     * Several pages put a search and a row of chips directly under the bar, and the band the content
     * actually passes beneath is that lower one. Raising only the bar would split the header into
     * two tones, which reads as a mistake rather than as a boundary.
     */
    @Test
    public void testAPagesOwnToolbarRaisesWithTheBar() {
        MD3TopAppBar bar = new MD3TopAppBar("Packs");
        bar.setSize(600, 64);

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(true);
        toolbar.setBackground(MD3Color.surface());
        toolbar.putClientProperty(MD3TopAppBar.COMPANION_KEY, true);

        JScrollPane scroller = scroller();
        bar.trackScroll(page(toolbar, scroller));

        paint(bar);
        assertEquals(MD3Color.surface(), toolbar.getBackground(),
                "the toolbar was raised before anything had scrolled");

        scroller.getVerticalScrollBar().setValue(240);
        paint(bar);

        assertEquals(MD3Elevation.surface(MD3Elevation.LEVEL2), toolbar.getBackground(),
                "the page scrolled and the toolbar stayed on the resting surface while the bar rose");
    }

    /**
     * The point of the whole thing: the header has to actually look different once content has gone
     * behind it.
     */
    @Test
    public void testTheRaiseIsVisible() {
        MD3TopAppBar bar = new MD3TopAppBar("Instances");
        bar.setSize(600, 64);

        JScrollPane scroller = scroller();
        bar.trackScroll(page(null, scroller));

        BufferedImage resting = paint(bar);

        scroller.getVerticalScrollBar().setValue(240);

        BufferedImage raised = paint(bar);

        assertNotEquals(resting.getRGB(300, 32), raised.getRGB(300, 32),
                "the bar reports itself as scrolled but paints exactly the same");
    }

    private static BufferedImage paint(MD3TopAppBar bar) {
        BufferedImage image = new BufferedImage(bar.getWidth() > 0 ? bar.getWidth() : 600,
                bar.getHeight() > 0 ? bar.getHeight() : 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            bar.paint(g);
        } finally {
            g.dispose();
        }

        return image;
    }
}
