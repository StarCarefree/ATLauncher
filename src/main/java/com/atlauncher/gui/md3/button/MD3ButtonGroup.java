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
package com.atlauncher.gui.md3.button;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * A connected row of buttons that share a single selection.
 *
 * <p>
 * Material's segmented button: one control, several mutually exclusive choices. The launcher wants
 * this for view mode, sort order, and anything else that used to be a row of unrelated outlined
 * buttons pretending to be a group.
 *
 * <p>
 * Segments share a visual family - outer corners stay a stadium, inner corners shrink to
 * {@link com.atlauncher.themes.md3.token.MD3Shape#BUTTON_GROUP_INNER} - and the selected segment
 * fills in the way an outlined button does when {@link MD3Button#setSelected(boolean) selected}.
 * Clicking an already-selected segment leaves it selected; this is a radio group, not a set of
 * toggles.
 */
public class MD3ButtonGroup extends JPanel {
    private final List<MD3Button> buttons = new ArrayList<>();
    private final List<ActionListener> listeners = new ArrayList<>();

    private MD3Button.Variant variant = MD3Button.Variant.OUTLINED;
    private MD3Button.Size buttonSize = MD3Button.Size.MEDIUM;
    private int selectedIndex = -1;

    public MD3ButtonGroup() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    }

    public MD3Button addOption(String label) {
        return addOption(label, null);
    }

    public MD3Button addOption(String label, MD3Icon icon) {
        MD3Button button = new MD3Button(label, icon, variant);
        button.setButtonSize(buttonSize);
        button.addActionListener(e -> select(buttons.indexOf(button), true));

        buttons.add(button);
        rebuild();

        if (selectedIndex < 0) {
            select(0, false);
        }

        return button;
    }

    public int getOptionCount() {
        return buttons.size();
    }

    public MD3Button getOption(int index) {
        return buttons.get(index);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        select(index, false);
    }

    public void addChangeListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void setVariant(MD3Button.Variant variant) {
        this.variant = variant;

        for (MD3Button button : buttons) {
            button.setVariant(variant);
        }
    }

    public void setButtonSize(MD3Button.Size buttonSize) {
        this.buttonSize = buttonSize;

        for (MD3Button button : buttons) {
            button.setButtonSize(buttonSize);
        }
    }

    private void select(int index, boolean announce) {
        if (index < 0 || index >= buttons.size()) {
            return;
        }

        boolean changed = selectedIndex != index;
        selectedIndex = index;

        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setSelected(i == index);
        }

        if (announce && changed) {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selected");

            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }
    }

    private void rebuild() {
        removeAll();

        int n = buttons.size();

        for (int i = 0; i < n; i++) {
            MD3Button button = buttons.get(i);

            if (n == 1) {
                button.setSegment(MD3Button.Segment.SOLO);
            } else if (i == 0) {
                button.setSegment(MD3Button.Segment.START);
            } else if (i == n - 1) {
                button.setSegment(MD3Button.Segment.END);
            } else {
                button.setSegment(MD3Button.Segment.MIDDLE);
            }

            if (i > 0) {
                add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.BUTTON_GROUP_GAP)));
            }

            add(button);
        }

        revalidate();
        repaint();
    }
}
