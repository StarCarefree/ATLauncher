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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 navigation rail - the vertical strip of top level destinations.
 *
 * <p>
 * Replaces the launcher's right-hand {@link javax.swing.JTabbedPane}, whose 32pt Oswald labels ran
 * down the side of the window and cost roughly a tenth of the horizontal space to say nine words.
 * The rail says the same nine things in 80dp using icons, and marks the active one with a pill
 * behind its glyph rather than by underlining a word.
 *
 * <p>
 * Material recommends three to seven destinations. More than that and the rail stops being
 * scannable, which is the whole point of it - put the overflow in the app bar's menu instead.
 *
 * <p>
 * Arrow keys move between destinations, and the rail is a single tab stop, so keyboard users step
 * past the navigation in one press rather than nine.
 *
 * <p>
 * There is one indicator pill and the rail owns it, rather than a pill per destination that switches
 * on and off. It travels to the destination you picked, which is the only thing on screen that says
 * the window moved rather than redrew - and because the destinations are transparent, a pill painted
 * by the rail lands underneath their glyphs on the way past.
 */
public class MD3NavigationRail extends JPanel {
    private static final int INDICATOR_WIDTH = 56;

    /** Room above the pill inside a destination. Kept short so eight destinations still fit. */
    private static final int INDICATOR_TOP = MD3Spacing.XS;

    private static final int LABEL_GAP = MD3Spacing.XS;
    private static final int ITEM_BOTTOM = MD3Spacing.S;
    private static final int LABEL_INSET = MD3Spacing.XS;

    private final List<Destination> destinations = new ArrayList<>();
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private final JPanel items = new JPanel();
    private final JScrollPane scroller;

    /** How far along the pill is between the destination it left and the one it is heading to. */
    private final MD3Animated slide = new MD3Animated(this, 1f, MD3Motion.NAVIGATION,
            MD3Motion.EMPHASIZED_OVERSHOOT);

    /** Whether there is a pill at all - there is not, on a destination the rail does not list. */
    private final MD3Animated presence = new MD3Animated(this, 0f, MD3Motion.SHORT4, MD3Motion.STANDARD);

    private JComponent header;
    private int selectedIndex = -1;
    /** Where the pill was before the current journey, in rail coordinates. */
    private float slideFrom = Float.NaN;
    /** The last destination the pill actually rested on, so it can fade out where it stopped. */
    private int restingIndex = -1;

    public MD3NavigationRail() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        applyChrome();
        setFocusable(true);

        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setOpaque(false);

        scroller = new JScrollPane(items, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setAlignmentX(CENTER_ALIGNMENT);
        scroller.setMaximumSize(new Dimension(UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH), Integer.MAX_VALUE));
        scroller.getVerticalScrollBar().setUnitIncrement(UIScale.scale(MD3Spacing.S));
        scroller.setWheelScrollingEnabled(true);

        add(scroller);

        installKeyBindings();
    }

    private void applyChrome() {
        // a shade off the content it sits beside; the rail and the page are both surface in the
        // spec, which leaves the launcher's primary navigation with no edge at all
        setBackground(MD3Color.surfaceContainer());
        setBorder(MD3Spacing.border(MD3Spacing.S, 0));
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (scroller != null) {
            applyChrome();
        }
    }

    private void installKeyBindings() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "md3.next");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "md3.previous");

        getActionMap().put("md3.next", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(1);
            }
        });

        getActionMap().put("md3.previous", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(-1);
            }
        });
    }

    private void move(int delta) {
        if (destinations.isEmpty()) {
            return;
        }

        int next = Math.floorMod(Math.max(0, selectedIndex) + delta, destinations.size());
        setSelectedIndex(next);
    }

    /**
     * A component pinned above the destinations - normally the primary action for the whole
     * window, such as "create instance".
     */
    public void setHeader(JComponent header) {
        if (this.header != null) {
            remove(this.header);
        }

        this.header = header;

        if (header != null) {
            header.setAlignmentX(CENTER_ALIGNMENT);
            add(header, 0);
            add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)), 1);
        }

        revalidate();
        repaint();
    }

    /**
     * @return the destination component, so a caller can name it for lookup in tests or tooling
     */
    public JComponent addDestination(MD3Icon.Painter icon, String label) {
        Destination destination = new Destination(icon, label, destinations.size());

        destinations.add(destination);
        destination.setAlignmentX(CENTER_ALIGNMENT);
        items.add(destination);

        if (selectedIndex < 0) {
            setSelectedIndex(0);
        }

        revalidate();
        repaint();

        return destination;
    }

    /**
     * A visual break between groups of destinations - primary above, utility below.
     */
    public void addSeparator() {
        MD3Divider divider = new MD3Divider(SwingConstants.HORIZONTAL);
        divider.setInsets(MD3Spacing.L, MD3Spacing.L);

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.setAlignmentX(CENTER_ALIGNMENT);
        wrap.setBorder(MD3Spacing.border(MD3Spacing.S, 0));
        wrap.add(divider);
        wrap.setMaximumSize(new Dimension(UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH),
                wrap.getPreferredSize().height));

        items.add(wrap);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Relabels a destination in place, for when the language changes.
     */
    public void setLabelAt(int index, String label) {
        if (index < 0 || index >= destinations.size()) {
            return;
        }

        destinations.get(index).setLabel(label);
    }

    /**
     * @param index the destination to mark active, or -1 for none - which is what a window shows
     *              while it is on a destination the rail does not list, such as one reached from the
     *              header action
     */
    public void setSelectedIndex(int index) {
        if (index == selectedIndex || index < -1 || index >= destinations.size()) {
            return;
        }

        int previous = selectedIndex;
        selectedIndex = index;

        travel(previous, index);

        for (Destination destination : destinations) {
            destination.onSelectionChanged();
        }

        repaint();
        fireStateChanged();
    }

    /**
     * Starts the pill on its way. It slides when it has somewhere to slide from and somewhere to
     * slide to; a first selection, or one that arrives while nothing was selected, simply appears
     * where it belongs and fades up.
     */
    private void travel(int from, int to) {
        presence.setTarget(to < 0 ? 0f : 1f);

        if (to < 0) {
            return;
        }

        // where the pill actually is, not where it was last sent: a second destination picked
        // before the first had been reached would otherwise start the new journey from a place the
        // pill had never got to, and it would jump there before setting off
        slideFrom = currentIndicatorY();
        restingIndex = to;

        if (Float.isNaN(slideFrom) || from < 0) {
            slide.set(1f);

            return;
        }

        slide.set(0f);
        slide.setTarget(1f);
    }

    /**
     * @return where the pill is being painted at this moment, which is between two destinations
     *         while it is travelling
     */
    private float currentIndicatorY() {
        float resting = indicatorY(restingIndex);

        if (Float.isNaN(slideFrom) || Float.isNaN(resting)) {
            return resting;
        }

        return MD3Animated.lerp(slideFrom, resting, slide.value());
    }

    /**
     * @return where a destination's pill sits in the rail's own coordinates, or NaN for one that has
     *         no pill or has not been laid out yet
     */
    private float indicatorY(int index) {
        if (index < 0 || index >= destinations.size()) {
            return Float.NaN;
        }

        Destination destination = destinations.get(index);

        if (destination.getParent() == null) {
            return Float.NaN;
        }

        // convert rather than add the nest of Ys by hand: destinations live in a scroll pane, and
        // a resize or a scroll would otherwise leave the pill where the item used to be
        Point onRail = SwingUtilities.convertPoint(destination, 0, UIScale.scale(INDICATOR_TOP), this);

        return onRail.y;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        float alpha = presence.value();

        // read from the live layout rather than from a remembered rectangle, so a resize mid-slide
        // lands the pill where the destination actually ended up
        float y = currentIndicatorY();

        if (alpha <= 0f || Float.isNaN(y)) {
            return;
        }

        int width = UIScale.scale(INDICATOR_WIDTH);
        int height = UIScale.scale(MD3Spacing.NAV_ITEM_INDICATOR_HEIGHT);

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            paintTrailingEdge(g2);

            // the pill is painted on the rail, the destinations scroll inside it - clip so a
            // destination that has been scrolled off does not leave its indicator on the FAB
            g2.clipRect(scroller.getX(), scroller.getY(), scroller.getWidth(), scroller.getHeight());

            MD3Paint.fill(g2, MD3Shape.rounded((getWidth() - width) / 2f, y, width, height,
                    MD3Shape.NAV_INDICATOR), MD3Color.get(MD3Color.SECONDARY_CONTAINER, alpha));
        } finally {
            g2.dispose();
        }
    }

    /**
     * A hairline on the trailing edge, so the rail is a column rather than a stain on the page.
     */
    private void paintTrailingEdge(Graphics2D g2) {
        int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);
        int x = MD3Paint.mirrorX(this, getWidth() - thickness, thickness);

        g2.setColor(MD3Color.outlineVariant());
        g2.fillRect(x, 0, thickness, getHeight());
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    private void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);

        for (ChangeListener listener : new ArrayList<>(changeListeners)) {
            listener.stateChanged(event);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH);

        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();
        size.width = UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH);

        return size;
    }

    /**
     * The launcher's primary navigation, named as such rather than announced as a panel of panels.
     * The rail is the tab stop and the arrow keys move within it, so it is the list and its
     * destinations are the items - the same shape as {@link MD3Tabs}.
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleMD3NavigationRail();
        }

        return accessibleContext;
    }

    protected class AccessibleMD3NavigationRail extends AccessibleJPanel {
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PAGE_TAB_LIST;
        }
    }

    /**
     * One destination: a pill indicator, a glyph, and a label beneath it.
     */
    private final class Destination extends JPanel {
        private final MD3Icon.Painter painter;
        private final int index;
        private final MD3StateLayer stateLayer;
        /** How far into being the active destination this one is, so its glyph crossfades. */
        private final MD3Animated activeness;

        private String label;

        Destination(MD3Icon.Painter painter, String label, int index) {
            this.painter = painter;
            this.label = label;
            this.index = index;
            this.activeness = new MD3Animated(this, 0f, MD3Motion.NAVIGATION, MD3Motion.STANDARD);

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(label);
            putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
            applyLabelFont();

            stateLayer = MD3StateLayer.install(this);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        MD3NavigationRail.this.requestFocusInWindow();
                        setSelectedIndex(Destination.this.index);
                    }
                }
            });
        }

        void setLabel(String label) {
            this.label = label;

            setToolTipText(label);
            applyLabelFont();
            revalidate();
            repaint();
        }

        private void applyLabelFont() {
            setFont(MD3Type.font(MD3Type.LABEL_MEDIUM, this.label));
        }

        @Override
        public void updateUI() {
            super.updateUI();

            if (label != null) {
                applyLabelFont();
            }
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleDestination();
            }

            return accessibleContext;
        }

        private final class AccessibleDestination extends AccessibleJPanel {
            @Override
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.PAGE_TAB;
            }

            @Override
            public String getAccessibleName() {
                return label;
            }

            @Override
            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleStateSet states = super.getAccessibleStateSet();

                if (isActive()) {
                    states.add(AccessibleState.SELECTED);
                }

                return states;
            }
        }

        private boolean isActive() {
            return index == selectedIndex;
        }

        void onSelectionChanged() {
            activeness.setTarget(isActive() ? 1f : 0f);

            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());

            return new Dimension(UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH),
                    UIScale.scale(INDICATOR_TOP + MD3Spacing.NAV_ITEM_INDICATOR_HEIGHT + LABEL_GAP
                            + ITEM_BOTTOM) + metrics.getHeight());
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                float active = activeness.value();

                int indicatorWidth = UIScale.scale(INDICATOR_WIDTH);
                int indicatorHeight = UIScale.scale(MD3Spacing.NAV_ITEM_INDICATOR_HEIGHT);
                int indicatorX = (getWidth() - indicatorWidth) / 2;
                int indicatorY = UIScale.scale(INDICATOR_TOP);

                Shape indicator = MD3Shape.rounded(indicatorX, indicatorY, indicatorWidth, indicatorHeight,
                        MD3Shape.NAV_INDICATOR);

                // the pill itself belongs to the rail, which paints it underneath all of this so it
                // can travel between destinations rather than switching on and off

                Color content = contentColor(active);

                // the state layer follows the indicator's pill, not the whole item, so hovering an
                // inactive destination previews exactly the shape selecting it would produce
                stateLayer.paint(g2, indicator, content);

                int iconSize = UIScale.scale(MD3Spacing.ICON_SIZE_LARGE);
                MD3Icon.of(painter, MD3Spacing.ICON_SIZE_LARGE).withColor(content).paintIcon(this, g2,
                        (getWidth() - iconSize) / 2, indicatorY + (indicatorHeight - iconSize) / 2);

                FontMetrics metrics = getFontMetrics(getFont());
                int textPad = UIScale.scale(LABEL_INSET);
                int maxTextWidth = Math.max(0, getWidth() - textPad * 2);
                String shown = MD3MixedText.fitToWidth(getFont(), label, maxTextWidth);
                int textWidth = MD3MixedText.width(getFont(), shown);

                g2.setColor(MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.onSurface(), active));
                MD3MixedText.draw(g2, shown, (getWidth() - textWidth) / 2,
                        indicatorY + indicatorHeight + UIScale.scale(LABEL_GAP) + metrics.getAscent(),
                        getFont());

                // the rail is one tab stop and the arrow keys move the selection within it, so the
                // ring goes on the destination that is selected - the same thing MD3Tabs does.
                // Without it, tabbing into the launcher's primary navigation showed nothing at all
                if (isActive() && MD3Focus.isVisible(MD3NavigationRail.this)) {
                    MD3Paint.focusRing(g2, indicatorX, indicatorY, indicatorWidth, indicatorHeight,
                            MD3Shape.NAV_INDICATOR);
                }
            } finally {
                g2.dispose();
            }
        }

        private Color contentColor(float active) {
            return MD3Animated.lerp(MD3Color.onSurfaceVariant(), MD3Color.onSecondaryContainer(), active);
        }
    }

}
