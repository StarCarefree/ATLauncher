/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.gui;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * Draws an account in the app bar's account picker: the player's head, then their name.
 *
 * <p>
 * The dropdown paints its own container, so this must not paint a background over the closed
 * control - Swing distinguishes that case by passing an index of -1. In the menu it is an opaque
 * row, because that is what the selection highlight fills.
 */
public class AccountsDropDownRenderer extends JLabel implements ListCellRenderer<MicrosoftAccount> {
    public AccountsDropDownRenderer() {
        setVerticalAlignment(CENTER);
        setIconTextGap(MD3Spacing.scale(MD3Spacing.M));
    }

    /**
     * This finds the image and text corresponding to the selected value and returns
     * the label to be displayed in the accounts selection dropdown.
     *
     * @param list The JList we're painting
     * @param account the account we're rendering
     * @param index The cell's index, or -1 for the value shown in the closed control
     * @param isSelected True if the specified cell was selected
     * @param cellHasFocus True if the specified cell has the focus
     * @return A component whose paint() method will render the specified value
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends MicrosoftAccount> list, MicrosoftAccount account,
            int index, boolean isSelected, boolean cellHasFocus) {
        boolean inMenu = index >= 0;

        setOpaque(inMenu);
        setBorder(inMenu ? MD3Spacing.border(MD3Spacing.S, MD3Spacing.L) : null);

        if (account == null) {
            setIcon(null);
            setText("");

            return this;
        }

        ImageIcon icon = account.getMinecraftHead();
        String username = account.minecraftUsername;

        setIcon(icon);
        setText(username);
        // a Minecraft username can be anything the theme's face may not cover
        setFont(MD3Type.font(MD3Type.BODY_LARGE, username));

        if (inMenu && isSelected) {
            setBackground(MD3Color.secondaryContainer());
            setForeground(MD3Color.onSecondaryContainer());
        } else if (inMenu) {
            setBackground(MD3Color.surfaceContainer());
            setForeground(MD3Color.onSurface());
        } else {
            setForeground(MD3Color.onSurface());
        }

        return this;
    }
}
