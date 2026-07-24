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
package com.atlauncher.gui.layouts;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import com.formdev.flatlaf.util.UIScale;

/**
 * A responsive grid of equal cards: as many columns as fit at the minimum card width, then the
 * leftover width is shared out between them so the row reaches both edges.
 *
 * <p>
 * {@link WrapLayout} cannot do this - it places components at their preferred width and leaves
 * whatever does not divide evenly as a gutter on the trailing edge. On a maximised window that
 * gutter came to most of a card's width, which read as the page having failed to load its last
 * column.
 *
 * <p>
 * Cards are capped at a maximum width so that a wide window stretches them only until another
 * column fits, rather than to the point where a card is mostly cover art. Past the cap the grid
 * centres itself instead.
 */
public class CardGridLayout implements LayoutManager {
    /**
     * Implemented by cards whose height depends on how wide they end up - artwork with a fixed
     * aspect, or text wrapped to the width it is given. The layout hands them their width before
     * asking what height they need.
     */
    public interface WidthAware {
        void setLayoutWidth(int width);
    }

    private final int minCardWidth;
    private final int maxCardWidth;
    private final int gap;

    /**
     * @param minCardWidth unscaled; the narrowest a card may be, and so what decides the column count
     * @param maxCardWidth unscaled; the widest a card may be stretched to
     * @param gap          unscaled; between columns and between rows
     */
    public CardGridLayout(int minCardWidth, int maxCardWidth, int gap) {
        this.minCardWidth = minCardWidth;
        this.maxCardWidth = maxCardWidth;
        this.gap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    private static List<Component> visible(Container parent) {
        List<Component> components = new ArrayList<>();

        for (Component component : parent.getComponents()) {
            if (component.isVisible()) {
                components.add(component);
            }
        }

        return components;
    }

    private int columnsFor(int available) {
        int min = UIScale.scale(minCardWidth);
        int scaledGap = UIScale.scale(gap);

        return Math.max(1, (available + scaledGap) / (min + scaledGap));
    }

    private int cardWidthFor(int available, int columns) {
        int scaledGap = UIScale.scale(gap);
        int share = (available - (columns - 1) * scaledGap) / columns;

        return Math.min(UIScale.scale(maxCardWidth), Math.max(UIScale.scale(minCardWidth), share));
    }

    /**
     * The width to lay out against. A card grid normally lives in a scroll pane, whose viewport has
     * no width of its own until the grid has one - so the question is passed up until something
     * answers it.
     */
    private static int availableWidth(Container parent) {
        Container container = parent;

        while (container.getWidth() == 0 && container.getParent() != null) {
            container = container.getParent();
        }

        Insets insets = parent.getInsets();

        return Math.max(0, container.getWidth() - insets.left - insets.right);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            List<Component> components = visible(parent);
            Insets insets = parent.getInsets();

            if (components.isEmpty()) {
                return new Dimension(insets.left + insets.right, insets.top + insets.bottom);
            }

            int available = availableWidth(parent);
            int columns = columnsFor(available);
            int width = cardWidthFor(available, columns);
            int scaledGap = UIScale.scale(gap);

            int height = insets.top + insets.bottom;
            int rows = 0;

            for (int i = 0; i < components.size(); i += columns) {
                height += rowHeight(components, i, columns, width);
                rows++;
            }

            height += Math.max(0, rows - 1) * scaledGap;

            return new Dimension(insets.left + insets.right + columns * width + (columns - 1) * scaledGap, height);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Insets insets = parent.getInsets();

        return new Dimension(insets.left + insets.right + UIScale.scale(minCardWidth), 0);
    }

    /**
     * Tallest card in a row, measured at the width the row will actually use.
     */
    private int rowHeight(List<Component> components, int from, int columns, int width) {
        int height = 0;

        for (int i = from; i < from + columns && i < components.size(); i++) {
            Component component = components.get(i);

            if (component instanceof WidthAware) {
                ((WidthAware) component).setLayoutWidth(width);
            }

            height = Math.max(height, component.getPreferredSize().height);
        }

        return height;
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            List<Component> components = visible(parent);

            if (components.isEmpty()) {
                return;
            }

            Insets insets = parent.getInsets();
            int available = availableWidth(parent);
            int columns = columnsFor(available);
            int width = cardWidthFor(available, columns);
            int scaledGap = UIScale.scale(gap);

            // only reachable once the cards have hit their maximum width, which is where stretching
            // them further would say less than the whitespace does
            int used = columns * width + (columns - 1) * scaledGap;
            int left = insets.left + Math.max(0, (available - used) / 2);

            int y = insets.top;

            for (int i = 0; i < components.size(); i += columns) {
                int height = rowHeight(components, i, columns, width);
                int x = left;

                for (int j = i; j < i + columns && j < components.size(); j++) {
                    components.get(j).setBounds(x, y, width, height);
                    x += width + scaledGap;
                }

                y += height + scaledGap;
            }
        }
    }
}
