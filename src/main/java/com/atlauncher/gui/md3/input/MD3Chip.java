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

import java.awt.event.ItemEvent;

import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;

/**
 * A Material 3 chip - a compact, selectable token.
 *
 * <pre>
 * FILTER      toggles a facet on or off; shows a tick when on   Minecraft 1.21.4, Fabric
 * ASSIST      triggers an action related to nearby content      Open folder
 * INPUT       represents something the user entered             a search term, a chosen mod
 * SUGGESTION  offers a value the user might want                a recommended loader version
 * </pre>
 *
 * <p>
 * The intended replacement for the rows of small buttons and combo boxes the pack browser and
 * instance list currently use for filtering.
 */
public class MD3Chip extends JToggleButton {
    public static final String UI_CLASS_ID = "MD3ChipUI";

    public enum Variant {
        ASSIST, FILTER, INPUT, SUGGESTION
    }

    private Variant variant;
    private Icon userIcon;

    public MD3Chip(String text) {
        this(text, Variant.FILTER);
    }

    public MD3Chip(String text, Variant variant) {
        super(text);

        this.variant = variant;

        // a filter chip announces its own state with a tick, so the icon is managed here rather
        // than left to the caller to remember
        addItemListener(e -> {
            if (getVariant() == Variant.FILTER) {
                refreshSelectionIcon(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        updateUI();
    }

    public static MD3Chip filter(String text) {
        return new MD3Chip(text, Variant.FILTER);
    }

    public static MD3Chip assist(String text, MD3Icon.Painter painter) {
        MD3Chip chip = new MD3Chip(text, Variant.ASSIST);
        chip.setIcon(MD3Icon.of(painter));

        return chip;
    }

    public static MD3Chip suggestion(String text) {
        return new MD3Chip(text, Variant.SUGGESTION);
    }

    private void refreshSelectionIcon(boolean selected) {
        if (selected) {
            super.setIcon(MD3Icon.of(MD3Icons.CHECK));
        } else {
            super.setIcon(userIcon);
        }

        revalidate();
        repaint();
    }

    @Override
    public void setIcon(Icon icon) {
        userIcon = icon;

        super.setIcon(icon);
    }

    public Variant getVariant() {
        return variant != null ? variant : Variant.FILTER;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;

        revalidate();
        repaint();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3ChipUI.class.getName());
        }

        setUI((ButtonUI) UIManager.getUI(this));
    }
}
