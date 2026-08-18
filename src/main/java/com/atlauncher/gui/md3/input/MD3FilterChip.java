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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.MD3MenuItem;
import com.atlauncher.gui.md3.MD3PopupMenu;
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
 * not scroll, and Minecraft has over eight hundred versions. Once there are more values than fit
 * on a glance, the menu grows a search box - CurseForge's category list is the same problem at a
 * smaller scale, and a chip that only scrolled was one people opened and then could not use.
 *
 * @param <T> what the chosen option stands for - a version string, a category id, a platform
 */
public final class MD3FilterChip<T> {
    private static final int VISIBLE_ROWS = 14;

    /**
     * Above this, scanning the list is slower than typing. Minecraft versions and CurseForge
     * categories both clear it; a sort field of four values does not, and a search box above four
     * rows would be louder than the list.
     */
    private static final int SEARCHABLE_AT = 8;

    private final MD3Chip chip;
    private final List<ComboItem<T>> options = new ArrayList<>();
    private final Runnable onChange;
    private final boolean narrows;

    private String name;
    private int selected = -1;
    private boolean loading;

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

    /**
     * The chip stays clickable while its values are in flight, so a user who opens it is told it
     * is loading rather than meeting a chip that does nothing. Once the values arrive they replace
     * this; an empty list with loading still true is the only state that shows the placeholder.
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isLoading() {
        return loading;
    }

    /**
     * The menu the chip would open. Built fresh, so a test can ask what it contains without
     * showing it - and so a caller that needs to inspect the list (search field, current values)
     * does not have to click.
     */
    public JPopupMenu createMenu() {
        return buildMenu();
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
            return statusMenu(loading ? GetText.tr("Loading...") : GetText.tr("Nothing to choose"));
        }

        DefaultListModel<ComboItem<T>> model = new DefaultListModel<>();
        fillModel(model, "");

        final MD3PopupMenu menu = new MD3PopupMenu();
        final JList<ComboItem<T>> list = new JList<>(model);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectCurrentIn(list);
        list.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        list.setBackground(MD3Color.surfaceContainerHigh());
        list.setForeground(MD3Color.onSurface());
        list.setSelectionBackground(MD3Color.secondaryContainer());
        list.setSelectionForeground(MD3Color.onSecondaryContainer());
        list.setCellRenderer(new OptionRenderer());
        list.setVisibleRowCount(Math.min(VISIBLE_ROWS, Math.max(1, options.size())));

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    menu.setVisible(false);
                    choose(list.getModel().getElementAt(index));
                }
            }
        });

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "md3.choose");
        list.getActionMap().put("md3.choose", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ComboItem<T> option = list.getSelectedValue();

                if (option == null) {
                    return;
                }

                menu.setVisible(false);
                choose(option);
            }
        });

        JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(list.getFixedCellHeight() > 0 ? list.getFixedCellHeight()
                : MD3Spacing.scale(MD3Spacing.XL));

        // never narrower than the chip, and never so thin a search box has nowhere to sit
        Dimension size = scrollPane.getPreferredSize();
        size.width = Math.max(size.width, Math.max(chip.getWidth(), MD3Spacing.scale(240)));
        scrollPane.setPreferredSize(size);

        menu.setLayout(new BorderLayout());
        menu.add(scrollPane, BorderLayout.CENTER);

        if (options.size() >= SEARCHABLE_AT) {
            MD3TextField search = MD3TextField.search(GetText.tr("Search"));
            search.setColumns(16);
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    applyFilter();
                }

                private void applyFilter() {
                    fillModel(model, search.getText());
                    selectCurrentIn(list);
                    list.ensureIndexIsVisible(Math.max(0, list.getSelectedIndex()));
                }
            });

            search.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER && model.getSize() == 1) {
                        menu.setVisible(false);
                        choose(model.getElementAt(0));
                        e.consume();
                        return;
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (model.getSize() == 0) {
                            return;
                        }

                        list.requestFocusInWindow();

                        if (list.getSelectedIndex() < 0) {
                            list.setSelectedIndex(0);
                        }

                        e.consume();
                    }
                }
            });

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(true);
            header.setBackground(MD3Color.surfaceContainerHigh());
            header.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.S, 0, MD3Spacing.S));
            header.add(search, BorderLayout.CENTER);
            menu.add(header, BorderLayout.NORTH);
            SwingUtilities.invokeLater(search::requestFocusInWindow);
        } else {
            // arrow keys have to reach the list, and it is not focused just by being shown
            SwingUtilities.invokeLater(list::requestFocusInWindow);
        }

        return menu;
    }

    private JPopupMenu statusMenu(String message) {
        MD3PopupMenu menu = new MD3PopupMenu();
        MD3MenuItem item = new MD3MenuItem(message);
        item.setEnabled(false);
        menu.add(item);

        return menu;
    }

    private void fillModel(DefaultListModel<ComboItem<T>> model, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        model.clear();

        for (ComboItem<T> option : options) {
            String label = option.getLabel();

            if (needle.isEmpty() || (label != null && label.toLowerCase(Locale.ROOT).contains(needle))) {
                model.addElement(option);
            }
        }
    }

    /**
     * Highlight the value already in effect, if the current filter still contains it. Falling
     * back to the first row is what a search that has narrowed the list away from the selection
     * should do - Enter then takes the remaining match rather than nothing.
     */
    private void selectCurrentIn(JList<ComboItem<T>> list) {
        ComboItem<T> current = selected < 0 || selected >= options.size() ? null : options.get(selected);

        if (current != null) {
            for (int i = 0; i < list.getModel().getSize(); i++) {
                if (list.getModel().getElementAt(i) == current) {
                    list.setSelectedIndex(i);

                    return;
                }
            }
        }

        if (list.getModel().getSize() > 0) {
            list.setSelectedIndex(0);
        } else {
            list.clearSelection();
        }
    }

    private void choose(ComboItem<T> option) {
        int index = options.indexOf(option);

        if (index >= 0) {
            select(index);
        }
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
