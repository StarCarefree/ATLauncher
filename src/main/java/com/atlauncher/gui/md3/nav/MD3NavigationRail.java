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
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
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
 */
public class MD3NavigationRail extends JPanel {
    private final List<Destination> destinations = new ArrayList<>();
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private final JPanel items = new JPanel();

    private JComponent header;
    private int selectedIndex = -1;

    public MD3NavigationRail() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(MD3Spacing.S, 0));
        setFocusable(true);

        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setOpaque(false);
        items.setAlignmentX(CENTER_ALIGNMENT);

        add(items);
        add(Box.createVerticalGlue());

        installKeyBindings();
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
        items.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)));
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

        selectedIndex = index;

        for (Destination destination : destinations) {
            destination.repaint();
        }

        fireStateChanged();
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
     * One destination: a pill indicator, a glyph, and a label beneath it.
     */
    private final class Destination extends JPanel {
        private static final int INDICATOR_WIDTH = 56;
        private static final int ITEM_HEIGHT = 56;

        private final MD3Icon.Painter painter;
        private final int index;
        private final MD3StateLayer stateLayer;

        private String label;

        Destination(MD3Icon.Painter painter, String label, int index) {
            this.painter = painter;
            this.label = label;
            this.index = index;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(label);
            setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
            putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);

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
            revalidate();
            repaint();
        }

        private boolean isActive() {
            return index == selectedIndex;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());

            return new Dimension(UIScale.scale(MD3Spacing.NAV_RAIL_WIDTH),
                    UIScale.scale(ITEM_HEIGHT) + metrics.getHeight() + UIScale.scale(MD3Spacing.XS));
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
                boolean active = isActive();

                int indicatorWidth = UIScale.scale(INDICATOR_WIDTH);
                int indicatorHeight = UIScale.scale(MD3Spacing.NAV_ITEM_INDICATOR_HEIGHT);
                int indicatorX = (getWidth() - indicatorWidth) / 2;
                int indicatorY = UIScale.scale(MD3Spacing.S);

                Shape indicator = MD3Shape.rounded(indicatorX, indicatorY, indicatorWidth, indicatorHeight,
                        MD3Shape.NAV_INDICATOR);

                if (active) {
                    MD3Paint.fill(g2, indicator, MD3Color.secondaryContainer());
                }

                // the state layer follows the indicator's pill, not the whole item, so hovering an
                // inactive destination previews exactly the shape selecting it would produce
                stateLayer.paint(g2, indicator, contentColor(active));

                int iconSize = UIScale.scale(MD3Spacing.ICON_SIZE_LARGE);
                MD3Icon.of(painter, MD3Spacing.ICON_SIZE_LARGE).withColor(contentColor(active)).paintIcon(this, g2,
                        (getWidth() - iconSize) / 2, indicatorY + (indicatorHeight - iconSize) / 2);

                FontMetrics metrics = getFontMetrics(getFont());
                int textWidth = metrics.stringWidth(label);

                g2.setFont(getFont());
                g2.setColor(active ? MD3Color.onSurface() : MD3Color.onSurfaceVariant());
                g2.drawString(label, (getWidth() - textWidth) / 2,
                        indicatorY + indicatorHeight + UIScale.scale(MD3Spacing.XS) + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }

        private Color contentColor(boolean active) {
            return active ? MD3Color.onSecondaryContainer() : MD3Color.onSurfaceVariant();
        }
    }

}
