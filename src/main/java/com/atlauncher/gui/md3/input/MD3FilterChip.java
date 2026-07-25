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
package com.atlauncher.gui.md3.input;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ComboItem;

/**
 * A filter chip backed by a list of values, showing the chosen one on its face.
 *
 * <p>
 * What a labelled combo box was. A chip says what is filtering the grid without being opened -
 * "1.21.4" rather than a box the user has to look inside - and a row of them collapses several
 * labelled controls into as many tokens. The pack browser and the mod browser are both built from
 * these.
 *
 * <p>
 * The values usually arrive with the platform and are replaced on every switch, so the menu is
 * built each time it opens. It is a list rather than a stack of menu items because Swing menus do
 * not scroll, and Minecraft has over eight hundred versions.
 *
 * @param <T> what the chosen option stands for - a version string, a category id, a platform
 */
public final class MD3FilterChip<T> {
    private static final int VISIBLE_ROWS = 14;

    private final MD3Chip chip;
    private final List<ComboItem<T>> options = new ArrayList<>();
    private final Runnable onChange;
    private final boolean narrows;

    private String name;
    private int selected = -1;

    /**
     * @param name     the facet, shown while nothing is chosen
     * @param narrows  whether choosing a value here hides packs. True of a version or a category,
     *                 false of a sort order - a sort always has a value, so a chip that marked
     *                 itself applied for it would sit permanently lit next to filters where the
     *                 same styling means something
     * @param onChange run when the user picks a different value, and never when the values
     *                 themselves are replaced - a platform switch rebuilds all three of these and
     *                 would otherwise reload the grid once per chip
     */
    public MD3FilterChip(String name, boolean narrows, Runnable onChange) {
        this.name = name;
        this.narrows = narrows;
        this.onChange = onChange;
        this.chip = MD3Chip.filter(name);

        chip.setMenu(this::buildMenu);
        refresh();
    }

    public MD3Chip getChip() {
        return chip;
    }

    /**
     * Renames the facet, for when the language changes.
     */
    public void setName(String name) {
        this.name = name;

        refresh();
    }

    /**
     * Replaces the values. The first becomes the selection, which for each of these is the
     * unfiltered "All Versions" or "All Categories".
     */
    public void setOptions(List<ComboItem<T>> values) {
        options.clear();
        options.addAll(values);
        selected = options.isEmpty() ? -1 : 0;

        refresh();
    }

    public void addOption(ComboItem<T> option) {
        options.add(option);

        if (selected < 0) {
            selected = 0;
        }

        refresh();
    }

    public void clear() {
        options.clear();
        selected = -1;

        refresh();
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }

    public T getValue() {
        return selected < 0 || selected >= options.size() ? null : options.get(selected).getValue();
    }

    /**
     * Picks the option standing for {@code value}, quietly - this is for putting a selection back
     * after the values have been rebuilt, which is not the user choosing anything.
     *
     * @return whether an option for it was found; false leaves the selection where it was
     */
    public boolean selectValue(T value) {
        for (int i = 0; i < options.size(); i++) {
            T candidate = options.get(i).getValue();

            if (candidate == null ? value == null : candidate.equals(value)) {
                selected = i;

                refresh();

                return true;
            }
        }

        return false;
    }

    public void setVisible(boolean visible) {
        chip.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        chip.setEnabled(enabled);
    }

    private void select(int index) {
        if (index < 0 || index >= options.size() || index == selected) {
            return;
        }

        selected = index;

        refresh();
        onChange.run();
    }

    /**
     * The chosen value reads better on the chip than the facet's name does, so the label carries
     * it - and the chip only marks itself selected once that value actually narrows anything.
     *
     * <p>
     * A filter's values say what they are: "All Versions", "Adventure and RPG". A sort field's do
     * not - "Popularity" on its own sits in a row of filters looking like another one - so that
     * chip keeps its name in front of the value.
     */
    private void refresh() {
        ComboItem<T> option = selected < 0 || selected >= options.size() ? null : options.get(selected);

        if (option == null) {
            chip.setText(name);
        } else {
            chip.setText(narrows ? option.getLabel() : name + ": " + option.getLabel());
        }

        chip.setToolTipText(name);
        chip.setSelected(narrows && option != null && option.getValue() != null);
    }

    private JPopupMenu buildMenu() {
        if (options.isEmpty()) {
            return null;
        }

        DefaultListModel<ComboItem<T>> model = new DefaultListModel<>();

        for (ComboItem<T> option : options) {
            model.addElement(option);
        }

        final JPopupMenu menu = new JPopupMenu();
        final JList<ComboItem<T>> list = new JList<>(model);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(Math.max(0, selected));
        list.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        list.setBackground(MD3Color.surfaceContainer());
        list.setForeground(MD3Color.onSurface());
        list.setSelectionBackground(MD3Color.secondaryContainer());
        list.setSelectionForeground(MD3Color.onSecondaryContainer());
        list.setCellRenderer(new OptionRenderer());
        list.setVisibleRowCount(Math.min(VISIBLE_ROWS, model.getSize()));

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    menu.setVisible(false);
                    select(index);
                }
            }
        });

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "md3.choose");
        list.getActionMap().put("md3.choose", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                menu.setVisible(false);
                select(list.getSelectedIndex());
            }
        });

        JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(list.getFixedCellHeight() > 0 ? list.getFixedCellHeight()
                : MD3Spacing.scale(MD3Spacing.XL));

        // the popup is never narrower than the chip that opened it, so a short value does not
        // produce a menu the user has to aim at
        Dimension size = scrollPane.getPreferredSize();
        size.width = Math.max(size.width, chip.getWidth());
        scrollPane.setPreferredSize(size);

        menu.setLayout(new BorderLayout());
        menu.add(scrollPane, BorderLayout.CENTER);

        // arrow keys have to reach the list, and it is not focused just by being shown
        SwingUtilities.invokeLater(() -> list.requestFocusInWindow());

        return menu;
    }

    /**
     * Material's menu item metrics - the default renderer's two pixels of padding leave the values
     * too tight to pick apart at a glance.
     */
    private static final class OptionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);

            setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.M));
            setFont(MD3Type.font(MD3Type.BODY_LARGE));

            return this;
        }
    }
}
