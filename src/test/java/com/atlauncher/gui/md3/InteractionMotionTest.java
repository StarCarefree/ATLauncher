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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.nav.MD3PageHost;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * What the launcher's controls look like at the ends of the interactions that move them.
 *
 * <p>
 * The motion between those ends is timing, and a test that waits on a timer is a test that fails on
 * a loaded machine. So these put a component into a state directly - a rollover on a button model,
 * a mouse-entered on a card - and check that the state is <em>drawn</em>. With reduced motion on,
 * which the render tests all run with, a value sent somewhere arrives at once, so the painted result
 * is exactly the end of the animation.
 *
 * <p>
 * What that catches is the failure that matters: an interaction state wired up but never painted,
 * which looks identical to a component that is simply not responding.
 */
public class InteractionMotionTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static BufferedImage paint(Component c) {
        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();

        return image;
    }

    private static int differingPixels(BufferedImage a, BufferedImage b) {
        int differing = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        return differing;
    }

    /** How much of the component is painted at all, rather than left as the page behind it. */
    private static int coveredPixels(BufferedImage image) {
        int surface = MD3Color.surface().getRGB();
        int covered = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    covered++;
                }
            }
        }

        return covered;
    }

    /**
     * The pointer arriving, delivered to the listeners rather than through the event queue.
     *
     * <p>
     * An exit is reported at a point outside the component, which is what tells it apart from the
     * exit Swing sends when the pointer has merely reached something <em>inside</em> - see
     * {@link #exitAt(Component, int, int)}.
     */
    private static void hover(Component c, boolean hovered) {
        if (hovered) {
            MouseEvent event = new MouseEvent(c, MouseEvent.MOUSE_ENTERED, 0L, 0, 1, 1, 0, false);

            for (MouseListener listener : c.getMouseListeners()) {
                listener.mouseEntered(event);
            }

            return;
        }

        exitAt(c, -1, -1);
    }

    /**
     * An exit reported at a given point in the component's own coordinates.
     *
     * <p>
     * Swing sends one of these whenever the pointer reaches a descendant that takes mouse events of
     * its own - a button on a card - and the point it carries is still on the component. That is the
     * only thing telling it apart from the pointer having actually gone.
     */
    private static void exitAt(Component c, int x, int y) {
        MouseEvent event = new MouseEvent(c, MouseEvent.MOUSE_EXITED, 0L, 0, x, y, 0, false);

        for (MouseListener listener : c.getMouseListeners()) {
            listener.mouseExited(event);
        }
    }

    private static MD3Card card(boolean hoverElevation) {
        MD3Card card = new MD3Card(MD3Card.Variant.FILLED);
        card.setHoverElevation(hoverElevation);
        card.setSize(new Dimension(200, 120));

        return card;
    }

    /**
     * Every card in the launcher's grids stands for one instance, one pack, one account. A grid of
     * them that does not move under the pointer reads as a picture of a grid.
     */
    @Test
    public void testACardAnswersThePointer() throws Exception {
        MD3Card card = card(true);

        BufferedImage resting = paint(card);

        hover(card, true);
        BufferedImage hovered = paint(card);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(hovered, "png", new File("build/md3-preview/card-hovered-dark.png"));

        assertTrue(differingPixels(resting, hovered) > 0,
                "a card with hover elevation paints the same under the pointer as away from it");
    }

    /**
     * The other half of it. A card that only groups controls - the launcher details on the about
     * page - has nothing to say about the pointer being over it, and lighting up would claim it does.
     */
    @Test
    public void testACardThatOnlyGroupsThingsIgnoresThePointer() {
        MD3Card card = card(false);

        BufferedImage resting = paint(card);

        hover(card, true);
        BufferedImage hovered = paint(card);

        assertEquals(0, differingPixels(resting, hovered),
                "a plain card lit up under the pointer, so it looks like something you can act on");
    }

    @Test
    public void testTheLiftComesBackDownWhenThePointerLeaves() {
        MD3Card card = card(true);

        BufferedImage resting = paint(card);

        hover(card, true);
        hover(card, false);

        assertEquals(0, differingPixels(resting, paint(card)),
                "a card stayed lifted after the pointer left it");
    }

    /**
     * The pointer reaching a button on a card is still the pointer on the card.
     *
     * <p>
     * Swing says otherwise: it reports the component as exited the moment anything inside it takes
     * the mouse events, and reports nothing at all when the pointer then leaves for good. Taken at
     * face value, every card in the launcher's grids dropped its hover as the pointer crossed the
     * controls on it and took it back in the gaps between them - a flicker, on the pages the
     * launcher spends most of its time showing.
     */
    @Test
    public void testACardKeepsItsLiftWhileThePointerIsOnAControlInsideIt() {
        MD3Card card = card(true);
        card.add(MD3Button.filled("Play"));
        layoutTree(card);

        hover(card, true);
        BufferedImage lifted = paint(card);

        // the exit Swing sends when the pointer arrives on the button: reported by the card, at a
        // point that is on the card
        exitAt(card, card.getWidth() / 2, card.getHeight() / 2);

        assertEquals(0, differingPixels(lifted, paint(card)),
                "a card dropped its hover when the pointer reached a control inside it");
    }

    /**
     * With no ripple, the shape morph is the whole of a button's press feedback. Rounding the corners
     * in means more of the button's box is filled, which is the same measurement whatever scale the
     * display is at - unlike the corner radius itself.
     */
    @Test
    public void testPressingAButtonRoundsItsCornersIn() throws Exception {
        MD3Button resting = MD3Button.filled("Play");
        MD3Button pressed = MD3Button.filled("Play");

        for (MD3Button button : new MD3Button[] { resting, pressed }) {
            button.setSize(button.getPreferredSize());
        }

        pressed.getModel().setArmed(true);
        pressed.getModel().setPressed(true);

        BufferedImage restingImage = paint(resting);
        BufferedImage pressedImage = paint(pressed);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(pressedImage, "png", new File("build/md3-preview/button-pressed-dark.png"));

        assertTrue(coveredPixels(pressedImage) > coveredPixels(restingImage),
                "a pressed button is the same stadium it was, so pressing it shows nothing at all");
    }

    /**
     * The container fades in behind the label as the outline fades out. It has to finish opaque - a
     * chip left at a fraction of its colour is a chip that never quite looks chosen.
     */
    @Test
    public void testASelectedChipEndsUpWithItsFullContainer() {
        MD3Chip chip = MD3Chip.filter("Name");
        chip.setSize(chip.getPreferredSize());
        chip.setSelected(true);

        BufferedImage image = paint(chip);
        Color centre = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));

        assertEquals(MD3Color.secondaryContainer().getRGB(), centre.getRGB(),
                "a selected chip did not reach its container colour, so the selection reads as half applied");
    }

    /**
     * The page host paints a snapshot while a page is arriving and the live page once it has. The
     * failure it has to be held to is the obvious one - a transition that never finishes leaves the
     * window empty, and nothing else in the launcher would say so.
     */
    @Test
    public void testThePageHostEndsUpShowingThePageItWasAskedFor() {
        JPanel first = new JPanel();
        first.setOpaque(true);
        first.setBackground(Color.RED);

        JPanel second = new JPanel();
        second.setOpaque(true);
        second.setBackground(Color.GREEN);

        MD3PageHost host = new MD3PageHost();
        host.addPage(first, "first");
        host.addPage(second, "second");
        host.setSize(new Dimension(120, 80));

        host.showPage("second");
        layoutTree(host);

        BufferedImage image = paint(host);

        assertEquals(Color.GREEN.getRGB(), image.getRGB(host.getWidth() / 2, host.getHeight() / 2),
                "the page host is not showing the page it was asked for");
    }
}
