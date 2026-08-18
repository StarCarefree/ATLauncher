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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 small top app bar: the title of the current destination, plus its actions.
 *
 * <p>
 * Gives the launcher a consistent place for the things currently scattered across each tab - the
 * search box, the sort control, the account picker - instead of each page inventing its own header
 * row.
 *
 * <p>
 * Keep it to three actions and an overflow. The bar competes with the content for attention, and
 * anything that is not used on most visits belongs in the menu.
 */
public class MD3TopAppBar extends JPanel {
    /**
     * Set to {@link Boolean#TRUE} on a page's own toolbar to have it raise with the bar.
     *
     * <p>
     * Several pages put a search and a row of chips directly under the app bar, which makes the
     * header two bands deep - and the band the content actually passes beneath is the lower one. If
     * only the bar raised, the header would go two-tone the moment the page scrolled, which reads as
     * a mistake rather than as a boundary. A toolbar that opts in is treated as part of the bar.
     */
    public static final String COMPANION_KEY = "md3.appBar.companion";

    private final JLabel titleLabel = new JLabel();
    private final JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    // no gap of its own: the actions are icon buttons, and each already carries the padding that
    // brings its container up to a full touch target. A gap here would be added to theirs
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
    private final JPanel centre = new JPanel(new BorderLayout());

    /** 0 while the page beneath is at the top, 1 once it has scrolled under the bar. */
    private final MD3Animated raise = new MD3Animated(this, 0f, MD3Motion.ELEVATION, MD3Motion.STANDARD);

    private MD3IconButton navigationButton;
    private boolean scrolled;

    private JScrollPane tracked;
    private AdjustmentListener trackedListener;

    /** The current page's own toolbars, which raise along with the bar. */
    private final List<JComponent> companions = new ArrayList<>();

    public MD3TopAppBar() {
        this(null);
    }

    public MD3TopAppBar(String title) {
        super(new BorderLayout());

        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(0, MD3Spacing.XS));

        raise.setListener(this::tintCompanions);

        titleLabel.setName("appBarTitle");
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setBorder(MD3Spacing.border(0, MD3Spacing.M));

        leading.setOpaque(false);
        trailing.setOpaque(false);
        centre.setOpaque(false);

        leading.add(titleLabel);

        // FlowLayout stacks its row against the top of whatever height BorderLayout hands it, so
        // the title and actions would sit 12dp high in a 64dp bar. Wrapping centres them without
        // pinning either side to a fixed height.
        add(centred(leading), BorderLayout.WEST);
        add(centre, BorderLayout.CENTER);
        add(centred(trailing), BorderLayout.EAST);

        setTitle(title);
    }

    /**
     * Holds a row against the vertical centre of whatever height it is given, without pinning it to
     * a height of its own.
     *
     * <p>
     * A {@link FlowLayout} centres its components within the tallest one in the row, not within the
     * container, so a row of 32dp chips beside a 40dp search field sits four pixels high in the
     * band and every toolbar in the launcher had its controls on two different centre lines. Public
     * because the pages' own toolbars share the bar's band and so have the same problem.
     */
    public static JPanel centred(JComponent inner) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;

        wrapper.add(inner, constraints);

        return wrapper;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
        titleLabel.setVisible(title != null && !title.isEmpty());

        revalidate();
        repaint();
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * The leading icon - a back arrow, or a menu button that opens the navigation drawer.
     */
    public void setNavigationAction(MD3Icon.Painter icon, String tooltip, ActionListener listener) {
        if (navigationButton != null) {
            leading.remove(navigationButton);
        }

        navigationButton = new MD3IconButton(icon, tooltip);
        navigationButton.addActionListener(listener);

        leading.add(navigationButton, 0);

        revalidate();
        repaint();
    }

    /**
     * A component filling the space between the title and the actions - most usefully a search
     * field.
     */
    public void setCentreComponent(JComponent component) {
        centre.removeAll();

        if (component != null) {
            centre.add(component, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    public void addAction(JComponent component) {
        trailing.add(component);

        revalidate();
        repaint();
    }

    public MD3IconButton addAction(MD3Icon.Painter icon, String tooltip, ActionListener listener) {
        MD3IconButton button = new MD3IconButton(icon, tooltip);
        button.addActionListener(listener);

        addAction(button);

        return button;
    }

    /**
     * Raises the bar onto a container surface once the content beneath it has scrolled, so it
     * separates from the page rather than floating over it unannounced.
     *
     * <p>
     * The two surfaces are a step apart on the elevation ramp, and the bar travels between them
     * rather than switching - a header that changes colour the instant the wheel turns reads as a
     * glitch, and the same change over two hundred milliseconds reads as the page moving underneath
     * something that was there all along.
     */
    public void setScrolled(boolean scrolled) {
        if (this.scrolled == scrolled) {
            return;
        }

        this.scrolled = scrolled;

        raise.setTarget(scrolled ? 1f : 0f);
    }

    public boolean isScrolled() {
        return scrolled;
    }

    /**
     * Follows a page's scrolling, so the bar answers the page currently on screen.
     *
     * <p>
     * The pages build their own scroll panes when they are shown and throw them away when they are
     * not, so this is re-established on every navigation rather than wired once. Passing a page with
     * nothing to scroll - or null - settles the bar back onto the plain surface.
     */
    public void trackScroll(Component page) {
        if (tracked != null) {
            tracked.getVerticalScrollBar().removeAdjustmentListener(trackedListener);

            tracked = null;
            trackedListener = null;
        }

        companions.clear();

        JScrollPane pane = page == null ? null : survey(page);

        if (pane == null) {
            setScrolled(false);
            tintCompanions();

            return;
        }

        tracked = pane;
        trackedListener = e -> setScrolled(e.getValue() > 0);
        tracked.getVerticalScrollBar().addAdjustmentListener(trackedListener);

        setScrolled(tracked.getVerticalScrollBar().getValue() > 0);

        // a page that arrived at the tone the bar is already at moves nothing, so its toolbars have
        // to be told the colour rather than waiting to be sent it by an animation that will not run
        tintCompanions();
    }

    /**
     * Walks a page for the two things the bar needs from it: the scroller it should follow - the
     * shallowest one that scrolls vertically, which is the one holding the page rather than one of
     * the lists inside it - and any toolbar that has asked to raise along with the bar.
     *
     * @return the page's scroller, or null if it has nothing to scroll
     */
    private JScrollPane survey(Component root) {
        JScrollPane scroller = null;

        Deque<Component> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Component component = queue.poll();

            if (component instanceof JComponent
                    && Boolean.TRUE.equals(((JComponent) component).getClientProperty(COMPANION_KEY))) {
                companions.add((JComponent) component);
            }

            if (scroller == null && component instanceof JScrollPane
                    && ((JScrollPane) component)
                            .getVerticalScrollBarPolicy() != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
                scroller = (JScrollPane) component;
            }

            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    queue.add(child);
                }
            }
        }

        return scroller;
    }

    /**
     * The tone the header is at, which is anywhere between the two ends while the raise is
     * travelling.
     */
    private Color surfaceTone() {
        return MD3Animated.lerp(MD3Color.surface(), MD3Elevation.surface(MD3Elevation.LEVEL2), raise.value());
    }

    /**
     * Carries the tone across to the page's own toolbars, which are ordinary opaque panels and so
     * follow by having their background set.
     *
     * <p>
     * Driven from the animation rather than from {@code paintComponent}. Setting another
     * component's background during a repaint is the one thing Swing asks painting not to do, and it
     * arrived a frame late as well - the toolbar was repainted after the bar that had just decided
     * its colour, so the header went briefly two-tone every time the page was scrolled.
     */
    private void tintCompanions() {
        Color surface = surfaceTone();

        for (JComponent companion : companions) {
            if (!surface.equals(companion.getBackground())) {
                companion.setBackground(surface);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // painted rather than set as the background, so the tone can be anywhere between the two
        // ends while the change is travelling
        g.setColor(surfaceTone());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }
}
