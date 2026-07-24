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

import java.awt.Component;

import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Builds overflow menus out of the buttons that already hold the actions.
 *
 * <p>
 * A Material card shows one or two actions and hides the rest behind an overflow. The launcher's
 * cards were built the other way round - every action was a button - and the behaviour, the enabled
 * rules and the translated labels all live on those buttons. Rather than move any of that, the
 * buttons stay constructed and simply stop being laid out, and the menu forwards to them.
 *
 * <p>
 * Build the menu each time it opens. Then it always reflects the current state of what it points
 * at, including labels that changed with the language.
 */
public final class MD3Menus {
    private MD3Menus() {
    }

    /**
     * A menu item that clicks the control holding the real action, so behaviour has one home no
     * matter how many places surface it.
     */
    public static JMenuItem delegateTo(AbstractButton source) {
        JMenuItem item = new JMenuItem(source.getText());
        item.setEnabled(source.isEnabled());
        item.addActionListener(e -> source.doClick());

        return item;
    }

    /**
     * Adds a button to the menu, unless the button has been hidden because it does not apply.
     */
    public static void addAction(JPopupMenu menu, AbstractButton source) {
        if (source == null || !source.isVisible()) {
            return;
        }

        menu.add(delegateTo(source));
    }

    /**
     * Adds an entry with its own label and behaviour, gated on a button's visibility and enabled
     * state. For the split buttons whose main half does something the dropdown does not list.
     */
    public static void addAction(JPopupMenu menu, AbstractButton source, String text, Runnable action) {
        if (source == null || !source.isVisible()) {
            return;
        }

        JMenuItem item = new JMenuItem(text);
        item.setEnabled(source.isEnabled());
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    /**
     * Folds a dropdown button's menu in as a submenu, or straight into the parent when there is no
     * button to name it after. Empty submenus are dropped rather than shown as dead ends.
     */
    public static void addSubmenu(JPopupMenu menu, AbstractButton source, JPopupMenu contents) {
        if (source != null && !source.isVisible()) {
            return;
        }

        if (source == null) {
            for (Component c : contents.getComponents()) {
                copyInto(menu, c);
            }

            return;
        }

        JMenu submenu = new JMenu(source.getText());
        submenu.setEnabled(source.isEnabled());

        for (Component c : contents.getComponents()) {
            copyInto(submenu.getPopupMenu(), c);
        }

        if (submenu.getMenuComponentCount() > 0) {
            menu.add(submenu);
        }
    }

    private static void copyInto(JPopupMenu target, Component source) {
        if (source instanceof JMenuItem && source.isVisible()) {
            target.add(delegateTo((JMenuItem) source));
        } else if (source instanceof JSeparator) {
            target.addSeparator();
        }
    }
}
