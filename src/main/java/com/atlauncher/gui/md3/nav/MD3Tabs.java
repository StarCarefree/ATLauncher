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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.atlauncher.gui.md3.icon.MD3Icon;
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
    private static final int HEIGHT_TEXT = 48;

    /** With an icon above the label. */
    private static final int HEIGHT_WITH_ICON = 64;

    private static final int INDICATOR_HEIGHT = 3;

    /** Keeps the indicator from shrinking to a stub under a one or two character label. */
    private static final int INDICATOR_MIN_WIDTH = 24;

    private static final int TAB_MIN_WIDTH = 72;
    private static final int TAB_PADDING_H = MD3Spacing.L;

    private final List<TabItem> tabs = new ArrayList<>();
    private final List<ChangeListener> changeListeners = new ArrayList<>();

    private int selectedIndex = -1;

    /** Where the indicator is being painted right now, which is mid-slide during a change. */
    private Rectangle indicator;
    private Rectangle indicatorFrom;
    private Animator indicatorAnimator;

    public MD3Tabs() {
        setLayout(null);
        setOpaque(true);
        setBackground(MD3Color.surface());
        setFocusable(true);

        installKeyBindings();
    }

    private void installKeyBindings() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "md3.next");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "md3.previous");

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
            indicator = indicatorBounds(index);
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

        int x = 0;

        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);

            // the last fixed tab absorbs the rounding, so the row ends flush with the container
            int width = fits ? (i == tabs.size() - 1 ? getWidth() - x : share) : tab.getPreferredSize().width;

            tab.setBounds(x, 0, width, height);
            x += width;
        }

        indicator = indicatorBounds(selectedIndex);
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

    private void slideIndicatorTo(final Rectangle target) {
        if (indicatorAnimator != null) {
            indicatorAnimator.stop();
            indicatorAnimator = null;
        }

        if (target == null || indicator == null || !Animator.useAnimation() || MD3Motion.isReduced()) {
            indicator = target;

            return;
        }

        indicatorFrom = new Rectangle(indicator);

        // the same curve and length the rail's pill travels on, so the two navigations in the window
        // move alike rather than each having its own idea of how far away a tab is
        indicatorAnimator = MD3Motion.animator(MD3Motion.NAVIGATION, MD3Motion.EMPHASIZED_OVERSHOOT,
                new Animator.TimingTarget() {
            @Override
            public void timingEvent(float fraction) {
                indicator = new Rectangle(
                        Math.round(indicatorFrom.x + (target.x - indicatorFrom.x) * fraction),
                        target.y,
                        Math.round(indicatorFrom.width + (target.width - indicatorFrom.width) * fraction),
                        target.height);

                repaint();
            }

            @Override
            public void end() {
                indicator = target;

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

            g2.setColor(MD3Color.surfaceVariant());
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

                if (MD3Tabs.this.isFocusOwner() && isActive()) {
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
