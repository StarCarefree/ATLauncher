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

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultButtonModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3MenuButton;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.nav.MD3Tabs;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3State;

/**
 * The behaviours an audit of the Material components found missing, each one a thing that was wrong
 * in a way no test would have noticed.
 *
 * <p>
 * They are here together rather than spread across the component suites because what they have in
 * common is why they were written: every one of these was a working component doing the wrong thing
 * in a case nobody had exercised - a card counting clicks twice, a split button with no keyboard
 * route to its menu, a tab that could be selected but not seen.
 */
public class MD3AuditRegressionTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void click(JComponent c, int x) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, 0L, 0, x, c.getHeight() / 2, 1, false,
                MouseEvent.BUTTON1));
    }

    private static void press(JComponent c, int x) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 0L, 0, x, c.getHeight() / 2, 1, false,
                MouseEvent.BUTTON1));
    }

    /**
     * A card's clickability follows what it holds, so it can be turned off and on any number of times.
     * Each pass used to leave another listener on it, and one click then started as many instances as
     * the card had been rebuilt.
     */
    @Test
    public void testTurningACardsClickabilityOffAndOnDoesNotDoubleItsClicks() {
        AtomicInteger clicks = new AtomicInteger();

        MD3Card card = new MD3Card();
        card.addActionListener(e -> clicks.incrementAndGet());
        card.setSize(200, 100);

        card.setClickable(true);
        card.setClickable(false);
        card.setClickable(true);

        click(card, 10);

        assertEquals(1, clicks.get(), "one click on the card fired its action " + clicks.get() + " times");
    }

    /** And a card that has been told to stop being clickable stops answering clicks at all. */
    @Test
    public void testACardThatIsNoLongerClickableIgnoresClicks() {
        AtomicInteger clicks = new AtomicInteger();

        MD3Card card = new MD3Card();
        card.addActionListener(e -> clicks.incrementAndGet());
        card.setSize(200, 100);

        card.setClickable(true);
        card.setClickable(false);

        click(card, 10);

        assertEquals(0, clicks.get(), "a card that is not clickable still fired its action");
    }

    /** A split button whose chevron opens a menu, counting the opens rather than showing one. */
    private static final class CountingMenuButton extends MD3MenuButton {
        private int menus;

        CountingMenuButton() {
            super("Play", MD3Button.Variant.OUTLINED, new JPopupMenu());
        }

        @Override
        public void showMenu() {
            menus++;
        }
    }

    /**
     * Where the press landed only means something to a pointer, so the keyboard has to be answered on
     * its own terms. It used to be answered with wherever the mouse had last been: a space bar after a
     * click on the chevron reopened the menu, and one on a button never clicked could not open it.
     */
    @Test
    public void testASplitButtonsKeyboardActivationTakesThePrimaryAction() {
        AtomicInteger actions = new AtomicInteger();

        CountingMenuButton button = new CountingMenuButton();
        button.setSplit(true);
        button.addActionListener(e -> actions.incrementAndGet());
        button.setSize(button.getPreferredSize());

        press(button, button.getWidth() - 1);
        button.doClick();

        assertEquals(1, button.menus, "clicking the chevron of a split button did not open its menu");
        assertEquals(0, actions.get(), "clicking the chevron also took the primary action");

        button.doClick();

        assertEquals(1, button.menus, "the keyboard reopened the menu the mouse had last been in");
        assertEquals(1, actions.get(), "the keyboard did not take the split button's primary action");
    }

    /**
     * A row of tabs too wide for the window scrolls. It used to clip, which left the tabs past the
     * edge selectable by the arrow keys and reachable by nothing at all.
     */
    @Test
    public void testATabTooFarAlongToFitIsScrolledIntoView() {
        MD3Tabs tabs = new MD3Tabs();
        JComponent last = null;

        for (String platform : new String[] { "ATLauncher", "CurseForge", "Modrinth", "FTB", "Technic",
                "Unified" }) {
            last = tabs.addTab(platform);
        }

        int narrow = tabs.getPreferredSize().width / 2;
        tabs.setSize(narrow, tabs.getPreferredSize().height);
        tabs.doLayout();

        assertTrue(last.getX() + last.getWidth() > narrow,
                "the row fits in half its own width, so there is nothing here to scroll");

        // the tabs were added before the row had a size, and adding the first one selects it
        assertEquals(0, ((JComponent) tabs.getComponent(0)).getX(),
                "the row opens scrolled, so the first tab is off the leading edge");

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        tabs.doLayout();

        assertTrue(last.getX() >= 0 && last.getX() + last.getWidth() <= narrow,
                "the selected tab is at " + last.getX() + " to " + (last.getX() + last.getWidth())
                        + " in a row " + narrow + " wide, so it cannot be seen or clicked");
    }

    /**
     * A button model outlives the UI delegate that attached to it - a look and feel change installs a
     * new one onto the same button - so a state layer that does not let go keeps animating and
     * repainting a component it no longer paints.
     */
    @Test
    public void testUninstallingAStateLayerLetsGoOfTheButtonModel() {
        DefaultButtonModel model = new DefaultButtonModel();
        JPanel host = new JPanel();

        int before = model.getChangeListeners().length;

        MD3StateLayer layer = MD3StateLayer.attach(host, model);

        assertEquals(before + 1, model.getChangeListeners().length,
                "the state layer is not listening to the model it was attached to");

        layer.uninstall();

        assertEquals(before, model.getChangeListeners().length,
                "an uninstalled state layer is still listening to the button's model");
    }

    /**
     * Material's order is dragged, pressed, focus, hover. Hover used to be asked first, so a focused
     * control the pointer happened to be resting on - which is most of them a moment after a click -
     * showed the weaker of the two layers.
     */
    @Test
    public void testFocusOutranksHoverInTheStateLayer() {
        assertEquals(MD3State.FOCUS, MD3State.opacityFor(true, true, false, false),
                "a control that is both hovered and focused shows the hover layer");
        assertEquals(MD3State.PRESSED, MD3State.opacityFor(true, true, true, false),
                "a press does not outrank focus and hover");
        assertEquals(MD3State.DRAGGED, MD3State.opacityFor(true, true, true, true),
                "a drag does not outrank everything else");
    }

    /**
     * An outline is stroked on a shape pulled in by half its width, rather than at double width and
     * clipped. A clip in Java2D takes no part in antialiasing, so the clipped version left every
     * outlined component with a stepped outer edge around each corner - which means the radius has to
     * come in with the box, or the corner it traces is not the corner it is tracing.
     */
    @Test
    public void testInsettingARoundedBoxBringsItsCornersInWithIt() {
        RoundRectangle2D inset = (RoundRectangle2D) MD3Paint
                .insetBy(new RoundRectangle2D.Float(0f, 0f, 40f, 20f, 20f, 20f), 2f);

        assertEquals(2d, inset.getX(), 0.001d);
        assertEquals(2d, inset.getY(), 0.001d);
        assertEquals(36d, inset.getWidth(), 0.001d);
        assertEquals(16d, inset.getHeight(), 0.001d);
        assertEquals(16d, inset.getArcWidth(), 0.001d,
                "the corner radius did not come in with the box, so the outline cuts across it");
        assertEquals(16d, inset.getArcHeight(), 0.001d);
    }
}
