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
package com.atlauncher.themes.md3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The keys that decide whether a menu gets rounded corners from the window manager.
 *
 * <p>
 * FlatLaf reads a different one depending on what opened the popup - a combo box's menu is not a
 * plain popup menu - and it only asks at all when the drop shadow is on. Setting one and leaving
 * the others is how a launcher ends up with rounded dialogs and square menus, which is the state
 * this pins against.
 */
public class PopupCornerTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
    }

    /**
     * All four, because FlatLaf picks by the popup's owner: a combo box reads
     * {@code ComboBox.borderCornerRadius}, a menu reads {@code PopupMenu}'s, a tooltip its own, and
     * anything else falls back to {@code Popup}'s.
     */
    @Test
    public void testEveryPopupKindHasACornerRadius() {
        for (String key : new String[] { "Popup.borderCornerRadius", "PopupMenu.borderCornerRadius",
                "ComboBox.borderCornerRadius", "ToolTip.borderCornerRadius" }) {
            int radius = UIManager.getInt(key);

            assertTrue(radius > 0, key + " is " + radius + ", so that kind of popup stays square");
        }
    }

    /**
     * The gate in front of all of them: FlatLaf only takes the rounded-border path when the popup
     * would carry a drop shadow, and skips straight past it otherwise.
     */
    @Test
    public void testTheDropShadowThatGatesTheRoundedPathIsOn() {
        assertTrue(UIManager.getBoolean("Popup.dropShadowPainted"),
                "Popup.dropShadowPainted is off, and FlatLaf never reaches the rounded-corner code without it");
    }

    /**
     * And the thing that reads all of the above. Every one of those defaults is inert unless
     * FlatLaf's own popup factory is the one Swing asks for popups - the corners are its work, not
     * the theme's.
     */
    @Test
    public void testFlatLafsPopupFactoryIsTheOneInstalled() {
        String factory = javax.swing.PopupFactory.getSharedInstance().getClass().getName();

        assertTrue(factory.startsWith("com.formdev.flatlaf"),
                "popups are made by " + factory + ", which does not round them - every borderCornerRadius "
                        + "in the theme is dead until FlatLaf's factory is installed");
    }
}
