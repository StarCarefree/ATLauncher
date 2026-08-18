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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * Material 3 primary tabs - the horizontal row that switches between views <em>within</em> one
 * screen.
 *
 * <p>
 * Distinct from {@link MD3NavigationRail}, which moves between the launcher's top level
 * destinations. Anything below that level - the pack browser's platforms, the settings sections -
 * is a tab, and putting it in a rail as well would leave the window with two competing navigations.
 *
 * <p>
 * Tabs share the width equally when they fit, which is Material's fixed tab layout, and fall back
 * to their natural widths when they do not, so a narrow window clips rather than illegibly crushes
 * them. The active indicator slides between tabs instead of cutting, because it is the only thing
 * on screen that says which view you just moved to.
 *
 * <p>
 * The row is a single tab stop with left and right arrows moving between tabs, so keyboard users
 * step past six platforms in one press.
 */
public class MD3Tabs extends JPanel {
    /** A label-only tab; Material's height for one line of text. */
    private static final int HEIGHT_TEXT = MD3Spacing.TAB_HEIGHT;

    /** With an icon above the label. */
    private static final int HEIGHT_WITH_ICON = MD3Spacing.TAB_HEIGHT_WITH_ICON;

    private static final int INDICATOR_HEIGHT = MD3Spacing.TAB_INDICATOR_HEIGHT;

    /** Keeps the indicator from shrinking to a stub under a one or two character label. */
    private static final int INDICATOR_MIN_WIDTH = MD3Spacing.XL;

    private static final int TAB_MIN_WIDTH = MD3Spacing.TAB_MIN_WIDTH;
    private static final int TAB_PADDING_H = MD3Spacing.L;

    /** How far one notch of the wheel moves a row of scrolling tabs. */
    private static final int SCROLL_STEP = MD3Spacing.L;

    private final List<TabItem> tabs = new ArrayList<>();
    private final List<ChangeListener> changeListeners = new ArrayList<>();

    private int selectedIndex = -1;

    /** How far a row too wide to fit has been scrolled. Always 0 for one that fits. */
    private int scrollOffset;

    /** Where the indicator is being painted right now, which is mid-slide during a change. */
    private Rectangle indicator;
    private Rectangle indicatorFrom;
    /** Where it is heading. A field rather than a captured local, so a re-layout can re-aim it. */
    private Rectangle indicatorTo;
    private Animator indicatorAnimator;

    public MD3Tabs() {
        setLayout(null);
        setOpaque(true);
        setBackground(MD3Color.surface());
        setFocusable(true);

        installKeyBindings();
        installWheelScrolling();
    }

    /**
     * Lets the wheel reach tabs that do not fit.
     *
     * <p>
     * Only when there are any. A row that fits has nothing to scroll and must not swallow the event -
     * the wheel over a header belongs to the page underneath it, and a listener that keeps it would
     * stop the pack browser scrolling whenever the pointer strayed onto the platform tabs.
     */
    private void installWheelScrolling() {
        addMouseWheelListener(e -> {
            if (maxScroll() > 0) {
                setScrollOffset(scrollOffset + e.getUnitsToScroll() * UIScale.scale(SCROLL_STEP));

                return;
            }

            Container parent = getParent();

            if (parent != null) {
                parent.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, parent));
            }
        });
    }

    private int contentWidth() {
        int width = 0;

        for (TabItem tab : tabs) {
            width += tab.getPreferredSize().width;
        }

        return width;
    }

    private int maxScroll() {
        return Math.max(0, contentWidth() - getWidth());
    }

    private void setScrollOffset(int offset) {
        int clamped = Math.max(0, Math.min(offset, maxScroll()));

        if (clamped == scrollOffset) {
            return;
        }

        scrollOffset = clamped;

        revalidate();
        repaint();
    }

    /**
     * Brings a tab fully into view.
     *
     * <p>
     * The row is one tab stop with the arrow keys moving inside it, so a keyboard user can select a
     * tab that is not on screen. Before this, the row simply clipped: the sixth platform in the pack
     * browser could be selected and then neither seen nor clicked.
     */
    private void revealTab(int index) {
        // nothing is on screen before the first layout, and asking then would measure every tab
        // against a width of zero - which reads as the whole row overflowing and scrolls it to the
        // end. Adding the tabs selects the first of them, so that is not a rare path
        if (index < 0 || index >= tabs.size() || getWidth() <= 0 || maxScroll() == 0) {
            return;
        }

        int x = 0;

        for (int i = 0; i < index; i++) {
            x += tabs.get(i).getPreferredSize().width;
        }

        int width = tabs.get(index).getPreferredSize().width;

        if (x < scrollOffset) {
            setScrollOffset(x);
        } else if (x + width > scrollOffset + getWidth()) {
            setScrollOffset(x + width - getWidth());
        }
    }

    private void installKeyBindings() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "md3.right");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "md3.left");

        getActionMap().put("md3.right", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(MD3Paint.isLeftToRight(MD3Tabs.this) ? 1 : -1);
            }
        });

        getActionMap().put("md3.left", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                move(MD3Paint.isLeftToRight(MD3Tabs.this) ? -1 : 1);
            }
        });
    }

    private void move(int delta) {
        if (tabs.isEmpty()) {
            return;
        }

        setSelectedIndex(Math.floorMod(Math.max(0, selectedIndex) + delta, tabs.size()));
    }

    /**
     * @return the tab component, so a caller can name it for lookup in tests or tooling
     */
    public JComponent addTab(String label) {
        return addTab(label, (Icon) null);
    }

    public JComponent addTab(String label, MD3Icon.Painter painter) {
        return addTab(label, painter == null ? null : MD3Icon.of(painter, MD3Spacing.ICON_SIZE_LARGE));
    }

    /**
     * @param icon any icon - a brand mark loaded from a PNG as readily as a drawn glyph. Non
     *             Material icons are scaled into the same 24dp box and left in their own colours,
     *             which is what a platform logo needs.
     */
    public JComponent addTab(String label, Icon icon) {
        TabItem tab = new TabItem(label, icon, tabs.size());

        tabs.add(tab);
        add(tab);

        if (selectedIndex < 0) {
            setSelectedIndex(0);
        }

        revalidate();
        repaint();

        return tab;
    }

    public int getTabCount() {
        return tabs.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * @param index the tab to activate, or -1 for none
     */
    public void setSelectedIndex(int index) {
        if (index == selectedIndex || index < -1 || index >= tabs.size()) {
            return;
        }

        selectedIndex = index;

        revealTab(index);
        slideIndicatorTo(indicatorBounds(index));
        repaint();
        fireStateChanged();
    }

    /**
     * Relabels a tab in place, for when the language changes.
     */
    public void setLabelAt(int index, String label) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }

        tabs.get(index).setLabel(label);

        if (index == selectedIndex) {
            // the indicator is as wide as the label, so a new one moves it - re-aimed rather than
            // reset, for the same reason a re-layout is
            if (isSliding()) {
                indicatorTo = indicatorBounds(index);
            } else {
                indicator = indicatorBounds(index);
            }
        }

        revalidate();
        repaint();
    }

    public void setIconAt(int index, Icon icon) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }

        tabs.get(index).setIcon(icon);
        repaint();
    }

    public String getLabelAt(int index) {
        return index < 0 || index >= tabs.size() ? null : tabs.get(index).label;
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * The launcher's pages are rebuilt every time they are shown, and a listener that cannot be taken
     * off is one the rebuilt page adds a second copy of.
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    private void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);

        for (ChangeListener listener : new ArrayList<>(changeListeners)) {
            listener.stateChanged(event);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        for (TabItem tab : tabs) {
            tab.setEnabled(enabled);
        }

        repaint();
    }

    /**
     * A tab row, and the tabs in it, named as such.
     *
     * <p>
     * This is a {@link JPanel} of {@link JPanel}s that paints an indicator, which told a screen reader
     * nothing: not that the row was a set of choices, not how many there were, and not which one was
     * current. The row is the tab stop, so it carries the list role and the items carry their own.
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleMD3Tabs();
        }

        return accessibleContext;
    }

    protected class AccessibleMD3Tabs extends AccessibleJPanel {
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PAGE_TAB_LIST;
        }
    }

    private boolean hasIcons() {
        for (TabItem tab : tabs) {
            if (tab.icon != null) {
                return true;
            }
        }

        return false;
    }

    private int rowHeight() {
        return UIScale.scale(hasIcons() ? HEIGHT_WITH_ICON : HEIGHT_TEXT);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 0;

        for (TabItem tab : tabs) {
            width += tab.getPreferredSize().width;
        }

        return new Dimension(width, rowHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(UIScale.scale(TAB_MIN_WIDTH) * Math.max(1, tabs.size()), rowHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, rowHeight());
    }

    /**
     * Fixed tabs: an equal share each, unless that would be narrower than a tab's own content, in
     * which case every tab takes what it needs and the row overflows.
     */
    @Override
    public void doLayout() {
        if (tabs.isEmpty()) {
            return;
        }

        int height = getHeight();
        int share = getWidth() / tabs.size();
        boolean fits = true;

        for (TabItem tab : tabs) {
            if (tab.getPreferredSize().width > share) {
                fits = false;

                break;
            }
        }

        // a row that fits is never scrolled, and one that no longer overflows as far as it did has to
        // give back the offset it cannot use, or it would be scrolled past its own last tab
        scrollOffset = fits ? 0 : Math.max(0, Math.min(scrollOffset, Math.max(0, contentWidth() - getWidth())));

        int x = fits ? 0 : -scrollOffset;

        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);

            // the last fixed tab absorbs the rounding, so the row ends flush with the container
            int width = fits ? (i == tabs.size() - 1 ? getWidth() - x : share) : tab.getPreferredSize().width;

            tab.setBounds(x, 0, width, height);
            x += width;
        }

        // a layout lands in the middle of a slide more often than not - selecting a tab is what
        // rebuilds the view under it, and that revalidates the window. Dropping the indicator on
        // its destination here would have the next frame pull it back to where it had got to, which
        // is the stutter the slide was there to avoid. What a layout genuinely changes is where the
        // tab ended up, so that is what it re-aims at.
        if (isSliding()) {
            indicatorTo = indicatorBounds(selectedIndex);
        } else {
            indicator = indicatorBounds(selectedIndex);
        }
    }

    private boolean isSliding() {
        return indicatorAnimator != null && indicatorAnimator.isRunning();
    }

    private Rectangle indicatorBounds(int index) {
        if (index < 0 || index >= tabs.size()) {
            return null;
        }

        TabItem tab = tabs.get(index);
        int width = Math.max(UIScale.scale(INDICATOR_MIN_WIDTH), tab.labelWidth());
        int height = UIScale.scale(INDICATOR_HEIGHT);

        return new Rectangle(tab.getX() + (tab.getWidth() - width) / 2, getHeight() - height, width, height);
    }

    private void slideIndicatorTo(Rectangle target) {
        // where it has actually got to. FlatLaf's animator delivers a final frame when it is
        // stopped, which would put the indicator on the tab it was still travelling towards - so
        // picking a third tab before the second had arrived jumped it there first
        Rectangle current = indicator == null ? null : new Rectangle(indicator);

        if (indicatorAnimator != null) {
            indicatorAnimator.stop();
            indicatorAnimator = null;
        }

        indicator = current;
        indicatorTo = target;

        if (target == null || current == null || !Animator.useAnimation() || MD3Motion.isReduced()) {
            indicator = target;

            return;
        }

        indicatorFrom = current;

        // the same curve and length the rail's pill travels on, so the two navigations in the window
        // move alike rather than each having its own idea of how far away a tab is
        indicatorAnimator = MD3Motion.animator(MD3Motion.NAVIGATION, MD3Motion.EMPHASIZED_OVERSHOOT,
                new Animator.TimingTarget() {
            @Override
            public void timingEvent(float fraction) {
                if (indicatorTo == null) {
                    return;
                }

                indicator = new Rectangle(
                        Math.round(indicatorFrom.x + (indicatorTo.x - indicatorFrom.x) * fraction),
                        indicatorTo.y,
                        Math.round(indicatorFrom.width + (indicatorTo.width - indicatorFrom.width) * fraction),
                        indicatorTo.height);

                repaint();
            }

            @Override
            public void end() {
                indicator = indicatorTo;

                repaint();
            }
        });

        indicatorAnimator.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            // the divider runs the full width, so the row reads as one surface with the content
            // below it rather than as six separate headers
            int thickness = UIScale.scale(MD3Spacing.DIVIDER_THICKNESS);

            g2.setColor(MD3Color.outlineVariant());
            g2.fillRect(0, getHeight() - thickness, getWidth(), thickness);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        if (indicator == null) {
            return;
        }

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            // rounded at the top only - it grows out of the divider it sits on
            MD3Paint.fill(g2, MD3Shape.rounded(indicator.x, indicator.y, indicator.width, indicator.height,
                    MD3Shape.EXTRA_SMALL, MD3Shape.EXTRA_SMALL, MD3Shape.NONE, MD3Shape.NONE),
                    isEnabled() ? MD3Color.primary()
                            : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface()));
        } finally {
            g2.dispose();
        }
    }

    /**
     * One tab: a state layer over the whole target, an optional icon, and a label.
     */
    private final class TabItem extends JPanel {
        private final MD3StateLayer stateLayer;
        private final int index;

        private String label;
        private Icon icon;

        TabItem(String label, Icon icon, int index) {
            this.label = label;
            this.icon = icon;
            this.index = index;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(MD3Type.font(MD3Type.TITLE_SMALL));
            putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);

            stateLayer = MD3StateLayer.install(this);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && MD3Tabs.this.isEnabled()) {
                        MD3Tabs.this.requestFocusInWindow();
                        setSelectedIndex(TabItem.this.index);
                    }
                }
            });
        }

        void setLabel(String label) {
            this.label = label;

            revalidate();
            repaint();
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleTabItem();
            }

            return accessibleContext;
        }

        private final class AccessibleTabItem extends AccessibleJPanel {
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

        void setIcon(Icon icon) {
            this.icon = icon;

            repaint();
        }

        int labelWidth() {
            return getFontMetrics(getFont()).stringWidth(label) + UIScale.scale(MD3Spacing.S);
        }

        private boolean isActive() {
            return index == selectedIndex;
        }

        private Color contentColor() {
            if (!MD3Tabs.this.isEnabled()) {
                return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
            }

            return isActive() ? MD3Color.primary() : MD3Color.onSurfaceVariant();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Math.max(UIScale.scale(TAB_MIN_WIDTH),
                    labelWidth() + UIScale.scale(TAB_PADDING_H) * 2), rowHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                Color content = contentColor();
                Shape bounds = new Rectangle(0, 0, getWidth(), getHeight());

                stateLayer.paint(g2, bounds, content);

                FontMetrics metrics = getFontMetrics(getFont());
                int textY;

                if (icon != null) {
                    int box = UIScale.scale(MD3Spacing.ICON_SIZE_LARGE);
                    int iconY = UIScale.scale(MD3Spacing.S);

                    paintIcon(g2, (getWidth() - box) / 2, iconY, box, content);
                    textY = iconY + box + UIScale.scale(MD3Spacing.XS) + metrics.getAscent();
                } else {
                    textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                }

                g2.setFont(getFont());
                g2.setColor(content);
                g2.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2, textY);

                if (isActive() && MD3Focus.isVisible(MD3Tabs.this)) {
                    MD3Paint.focusRing(g2, this, MD3Shape.NONE);
                }
            } finally {
                g2.dispose();
            }
        }

        /**
         * Material icons follow the tab's content colour; a brand mark keeps its own and is only
         * fitted to the box, since a recoloured logo is no longer the logo.
         */
        private void paintIcon(Graphics2D g, int x, int y, int box, Color content) {
            if (icon instanceof MD3Icon) {
                ((MD3Icon) icon).withColor(content).paintIcon(this, g, x, y);

                return;
            }

            int width = icon.getIconWidth();
            int height = icon.getIconHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                double scale = Math.min(box / (double) width, box / (double) height);

                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.translate(x + (box - width * scale) / 2, y + (box - height * scale) / 2);
                g2.scale(scale, scale);
                icon.paintIcon(this, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }
    }
}
