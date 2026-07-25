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
package com.atlauncher.managers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * The dialogs moved from {@code JOptionPane} to Material, which puts the action that proceeds on the
 * trailing edge - the opposite end from where these are declared. Every call site reads the answer
 * as an index into the options it added, so the two orders have to be reconciled.
 *
 * <p>
 * This is the part worth pinning down: getting it wrong does not look broken, it just quietly
 * returns "No" when the user pressed "Yes", and the caller deletes something it should not have.
 */
public class DialogManagerOrderTest {
    private static int[] displayOrder(DialogManager dialog) throws Exception {
        Method method = DialogManager.class.getDeclaredMethod("displayOrder");
        method.setAccessible(true);

        return (int[]) method.invoke(dialog);
    }

    private static int resultOf(DialogManager dialog, int chosen, int[] order) throws Exception {
        Method method = DialogManager.class.getDeclaredMethod("resultOf", int.class, int[].class);
        method.setAccessible(true);

        return (int) method.invoke(dialog, chosen, order);
    }

    /**
     * Walks the options as the user would: presses the button in the given position and reports the
     * index the caller is told.
     */
    private static int press(DialogManager dialog, int position) throws Exception {
        int[] order = displayOrder(dialog);

        return resultOf(dialog, position, order);
    }

    @Test
    public void testYesIsShownLastAndStillReportsAsTheFirstOption() throws Exception {
        DialogManager dialog = DialogManager.yesNoDialog();

        assertArrayEquals(new int[] { 1, 0 }, displayOrder(dialog),
                "Yes is not on the trailing edge, so the dialog reads back to front");

        assertEquals(DialogManager.YES_OPTION, press(dialog, 1), "pressing Yes did not report Yes");
        assertEquals(DialogManager.NO_OPTION, press(dialog, 0), "pressing No did not report No");
    }

    /**
     * {@code yesNoDialog(false)} makes No the default, which is used before destructive things.
     */
    @Test
    public void testTheDefaultIsWhicheverWasDeclaredAsSuch() throws Exception {
        DialogManager dialog = DialogManager.yesNoDialog(false);

        assertArrayEquals(new int[] { 0, 1 }, displayOrder(dialog),
                "No was declared the default, so it should be the one on the trailing edge");

        assertEquals(DialogManager.NO_OPTION, press(dialog, 1), "pressing No did not report No");
        assertEquals(DialogManager.YES_OPTION, press(dialog, 0), "pressing Yes did not report Yes");
    }

    @Test
    public void testTheOtherOptionsKeepTheirRelativeOrder() throws Exception {
        DialogManager dialog = DialogManager.yesNoCancelDialog();

        assertArrayEquals(new int[] { 1, 2, 0 }, displayOrder(dialog),
                "No and Cancel did not keep the order they were added in");

        assertEquals(DialogManager.YES_OPTION, press(dialog, 2), "pressing Yes did not report Yes");
        assertEquals(DialogManager.NO_OPTION, press(dialog, 0), "pressing No did not report No");
        assertEquals(DialogManager.CANCEL_OPTION, press(dialog, 1), "pressing Cancel did not report Cancel");
    }

    @Test
    public void testASingleOptionIsUnchanged() throws Exception {
        DialogManager dialog = DialogManager.okDialog();

        assertArrayEquals(new int[] { 0 }, displayOrder(dialog), "the only option moved");
        assertEquals(DialogManager.OK_OPTION, press(dialog, 0), "pressing Ok did not report Ok");
    }

    /**
     * Closing a dialog without answering it has always reported {@code CLOSED_OPTION}, and a good
     * deal of the launcher treats that as "do nothing".
     */
    @Test
    public void testDismissingReportsClosed() throws Exception {
        DialogManager dialog = DialogManager.yesNoDialog();
        int[] order = displayOrder(dialog);

        assertEquals(DialogManager.CLOSED_OPTION, resultOf(dialog, -1, order),
                "a dismissed dialog did not report as closed");
        assertEquals(DialogManager.CLOSED_OPTION, resultOf(dialog, 5, order),
                "an answer that is not one of the options did not report as closed");
    }
}
