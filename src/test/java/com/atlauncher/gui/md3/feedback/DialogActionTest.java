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
package com.atlauncher.gui.md3.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Which of a dialog's actions the enter key takes.
 *
 * <p>
 * Asked of the builder rather than of a built dialog, so this needs no display and runs with the rest
 * of the Material suite.
 */
public class DialogActionTest {
    /**
     * The action that proceeds is added last so it lands rightmost, and the default button is the
     * rightmost one - which quietly made enter mean "delete the instance" in every confirmation built
     * with {@code destructive}. Those are left with no default at all.
     */
    @Test
    public void testADestructiveDialogHasNoDefaultButton() {
        assertEquals(-1, MD3Dialog.builder(null).headline("Delete instance?").dismiss("Cancel")
                .destructive("Delete").defaultActionIndex(),
                "enter would take the destructive action");
    }

    @Test
    public void testAnOrdinaryDialogDefaultsToTheActionThatProceeds() {
        assertEquals(1, MD3Dialog.builder(null).headline("Save changes?").dismiss("Cancel").confirm("Save")
                .defaultActionIndex(),
                "enter does not take the confirming action of an ordinary dialog");
    }
}
