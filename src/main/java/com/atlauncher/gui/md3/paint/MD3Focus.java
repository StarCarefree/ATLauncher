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
package com.atlauncher.gui.md3.paint;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Whether the focus indicator should be showing - the desktop equivalent of the web's
 * {@code :focus-visible}.
 *
 * <p>
 * Focus and <em>showing</em> focus are not the same question, and Swing only answers the first.
 * {@code isFocusOwner} is true after a click as much as after a tab, so a ring drawn on it alone
 * left a 3dp accent outline around every button a mouse user had touched - Material asks for the
 * indicator where the keyboard is, and for nothing where the pointer already said where it was.
 *
 * <p>
 * Which it is comes from the last thing the user did rather than from the focus event, because the
 * focus event cannot tell: a click and a tab both arrive as {@code focusGained}. So this watches the
 * event queue for the one bit that distinguishes them.
 *
 * <p>
 * It starts out true. A window that has just opened has been navigated to by no pointer, and
 * whatever it focuses first is somewhere the user has to be able to find - and it keeps the
 * offscreen render tests, which post no events at all, testing the indicator rather than this.
 */
public final class MD3Focus {
    private static volatile boolean keyboard = true;
    private static boolean listening;

    private MD3Focus() {
    }

    /**
     * @return whether this component should be drawing its focus indicator
     */
    public static boolean isVisible(Component c) {
        return c != null && c.isFocusOwner() && isKeyboardModality();
    }

    /**
     * @return whether the user is currently working with the keyboard rather than the pointer, for a
     *         component deciding its focus state before it has one to read
     */
    public static boolean isKeyboardModality() {
        listen();

        return keyboard;
    }

    private static synchronized void listen() {
        if (listening) {
            return;
        }

        listening = true;

        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event.getID() == KeyEvent.KEY_PRESSED) {
                    keyboard = true;
                } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                    keyboard = false;
                }
            }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
        } catch (Throwable t) {
            // not allowed to watch the queue. Then every focus is a visible one, which is the safe
            // direction to be wrong in - a ring nobody needed beats no ring for somebody who did
        }
    }
}
