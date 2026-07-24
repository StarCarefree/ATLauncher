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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * Pins the app bar's layout.
 *
 * <p>
 * Worth a test of its own because the bar is nested three containers deep by the time it reaches
 * the window, and a mistake there does not throw - the actions simply end up somewhere the eye
 * skips, which is exactly how the first cut of this shipped with everything jammed against the top
 * edge of a 64dp bar.
 */
public class MD3TopAppBarLayoutTest {
    private static final int BAR_WIDTH = 1085;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.Dark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    /** Bounds of a component relative to the given ancestor. */
    private static Rectangle boundsWithin(Component c, Component ancestor) {
        Rectangle bounds = c.getBounds();

        for (Component p = c.getParent(); p != null && p != ancestor; p = p.getParent()) {
            bounds.translate(p.getX(), p.getY());
        }

        return bounds;
    }

    private static Component findFirst(Container root, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                return c;
            }

            if (c instanceof Container) {
                Component found = findFirst((Container) c, type);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private MD3TopAppBar buildBar() {
        MD3TopAppBar bar = new MD3TopAppBar("News");
        bar.addAction(MD3Icons.TERMINAL, "Show Console", e -> {
        });
        bar.addAction(MD3Icons.FOLDER, "Open Folder", e -> {
        });
        bar.addAction(MD3Icons.REFRESH, "Check For Updates", e -> {
        });

        JComboBox<String> combo = new JComboBox<>(new String[] { "Steve" });
        combo.setName("accountSelector");
        bar.addAction(combo);

        JPanel body = new JPanel(new BorderLayout());
        body.add(bar, BorderLayout.NORTH);
        body.add(new JPanel(), BorderLayout.CENTER);
        body.setSize(BAR_WIDTH, 620);
        layoutTree(body);

        return bar;
    }

    @Test
    public void testActionsAreLaidOutAgainstTheTrailingEdge() {
        MD3TopAppBar bar = buildBar();

        Component action = findFirst(bar, MD3IconButton.class);
        assertNotNull(action, "no action button was laid out");

        Rectangle bounds = boundsWithin(action, bar);

        assertTrue(bounds.width > 0 && bounds.height > 0, "the action has no size");
        assertTrue(bounds.x > BAR_WIDTH / 2, "the actions should sit on the trailing half of the bar, not at "
                + bounds.x);
    }

    @Test
    public void testActionsAreVerticallyCentred() {
        MD3TopAppBar bar = buildBar();

        Component action = findFirst(bar, MD3IconButton.class);
        Rectangle bounds = boundsWithin(action, bar);

        int barCentre = bar.getHeight() / 2;
        int actionCentre = bounds.y + bounds.height / 2;

        assertEquals(barCentre, actionCentre, 2,
                "actions sit at " + actionCentre + " in a bar centred on " + barCentre);
    }

    @Test
    public void testTheAccountSelectorSurvivesTheNesting() {
        MD3TopAppBar bar = buildBar();

        Component combo = findFirst(bar, JComboBox.class);

        assertNotNull(combo, "the account selector was not laid out");
        assertEquals("accountSelector", combo.getName(), "the selector lost the name tests look it up by");
        assertTrue(combo.getWidth() > 0, "the account selector has no width");
    }

    @Test
    public void testTheBarKeepsItsSpecifiedHeight() {
        assertEquals(MD3Spacing.scale(MD3Spacing.TOP_APP_BAR_HEIGHT), buildBar().getHeight());
    }
}
