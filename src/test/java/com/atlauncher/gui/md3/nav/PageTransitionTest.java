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
package com.atlauncher.gui.md3.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Motion;

/**
 * How a page arrives when the launcher is navigated.
 *
 * <p>
 * The transition used to paint both halves from images taken at the moment the card layout
 * switched. For the page being left that is the only option - it is hidden by then - but for the
 * page arriving it was a picture of something that did not exist yet: the launcher's pages are
 * {@link com.atlauncher.gui.panels.HierarchyPanel}s that assemble themselves when shown, several of
 * them finishing on a later pass of the event queue. So navigating to the instances or the pack
 * browser faded up an empty window and then snapped to the real page, and any spinner on a page
 * still loading stood still for the length of the transition.
 *
 * <p>
 * These hold the transition at a point rather than racing it, since which half is on screen is
 * otherwise a question of when you looked.
 */
public class PageTransitionTest {
    /** Comfortably into the arrival: the outgoing half is over by 0.3. */
    private static final float ARRIVING = 0.98f;

    /** Early in the exit, while the page being left is still almost fully opaque. */
    private static final float LEAVING = 0.02f;

    private JFrame frame;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        MD3Motion.setReduced(false);
    }

    @AfterEach
    public void putMotionBack() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }

        // the render tests share this JVM and all of them want motion off
        MD3Motion.setReduced(true);
    }

    private static JPanel page(Color colour) {
        JPanel page = new JPanel();
        page.setOpaque(true);
        page.setBackground(colour);

        return page;
    }

    /**
     * A host inside a realized window, which is what the transition needs to run at all - a value on
     * a component nothing could see arrives instantly by design.
     */
    private MD3PageHost realizedHost() {
        MD3PageHost host = new MD3PageHost();

        frame = new JFrame();
        frame.setUndecorated(true);
        frame.getContentPane().add(host);
        frame.setSize(120, 80);
        // realized without ever being shown: displayable is all the transition asks for
        frame.pack();
        frame.setSize(120, 80);
        frame.validate();

        host.setSize(120, 80);
        layoutTree(host);

        return host;
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static BufferedImage paintAt(MD3PageHost host, float fraction) {
        BufferedImage image = new BufferedImage(host.getWidth(), host.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, host.getWidth(), host.getHeight());

            host.paintTransition(g, fraction);
        } finally {
            g.dispose();
        }

        return image;
    }

    private static Color centreOf(BufferedImage image) {
        return new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
    }

    /**
     * The page arriving is painted as it is now, not as it was when the swap happened. This is the
     * whole point: a page that builds itself after being shown was otherwise faded up empty.
     */
    @Test
    public void testTheArrivingPageIsPaintedAsItIsNow() {
        JPanel first = page(Color.RED);
        JPanel second = page(Color.RED);

        MD3PageHost host = realizedHost();
        host.addPage(first, "first");
        host.addPage(second, "second");
        layoutTree(host);

        host.showPage("second");

        // what every page in the launcher does the moment it is shown: fill itself in
        second.setBackground(Color.GREEN);

        Color centre = centreOf(paintAt(host, ARRIVING));

        assertTrue(centre.getGreen() > 200 && centre.getRed() < 60,
                "the arriving page was painted from a picture taken before it had built itself, and came "
                        + "up as " + centre);
    }

    /**
     * The other half. The page being left has already been hidden by the card layout, so an image is
     * the only way to show it leaving at all.
     */
    @Test
    public void testThePageBeingLeftIsStillPaintedOnTheWayOut() {
        JPanel first = page(Color.RED);
        JPanel second = page(Color.GREEN);

        MD3PageHost host = realizedHost();
        host.addPage(first, "first");
        host.addPage(second, "second");
        layoutTree(host);

        host.showPage("second");

        Color centre = centreOf(paintAt(host, LEAVING));

        assertTrue(centre.getRed() > 200 && centre.getGreen() < 60,
                "the page being navigated away from vanished on the frame of the swap rather than fading, "
                        + "and the exit showed " + centre);
    }

    /**
     * Painting the live tree under a transform only works if Swing paints none of it on its own. A
     * child repainting itself - a spinner on a page still loading, a button under the pointer - goes
     * straight to that child otherwise, which would draw it at the position it will have once the
     * transition is over rather than where it is currently being shown.
     */
    @Test
    public void testAChildRepaintingMidTransitionRedrawsTheWholeHost() {
        MD3PageHost host = realizedHost();
        host.addPage(page(Color.RED), "first");
        host.addPage(page(Color.GREEN), "second");
        layoutTree(host);

        host.showPage("second");

        assertEquals(new Rectangle(0, 0, host.getWidth(), host.getHeight()), repaintAsked(host, 10, 10, 4, 4),
                "a repaint during a transition stayed where it was asked for, so whatever caused it will be "
                        + "drawn outside the transform the rest of the page is under");
    }

    /**
     * And once it is over, a repaint is the small one it asked for again - the widening above is for
     * the three hundred milliseconds it is needed, not for the life of the window.
     */
    @Test
    public void testRepaintsGoBackToNormalOnceTheTransitionIsOver() {
        MD3PageHost host = realizedHost();
        host.addPage(page(Color.RED), "first");
        layoutTree(host);

        MD3Motion.setReduced(true);
        host.showPage("first");

        assertEquals(new Rectangle(10, 10, 4, 4), repaintAsked(host, 10, 10, 4, 4),
                "the host is still widening every repaint to the whole page after the transition finished");
    }

    /**
     * @return the region the host actually asked Swing to redraw, which is not the one passed in
     *         while a transition is running
     */
    private static Rectangle repaintAsked(MD3PageHost host, int x, int y, int width, int height) {
        Recorder recorder = new Recorder();
        RepaintManager previous = RepaintManager.currentManager(host);

        // a manager of our own rather than reading the dirty region back: the real one drops
        // everything for a component whose window was never shown, which is every window in a test
        RepaintManager.setCurrentManager(recorder);

        try {
            host.repaint(0, x, y, width, height);
        } finally {
            RepaintManager.setCurrentManager(previous);
        }

        return recorder.asked;
    }

    private static final class Recorder extends RepaintManager {
        private Rectangle asked;

        @Override
        public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            asked = new Rectangle(x, y, w, h);
        }
    }
}
