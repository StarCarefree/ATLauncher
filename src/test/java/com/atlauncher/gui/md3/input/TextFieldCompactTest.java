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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * A text field with no label.
 *
 * <p>
 * Material's outlined field is 56dp with 8dp of overflow above it, and both of those exist to hold
 * a floating label. Every field in this launcher's settings is named by the row it sits in, so it
 * has no label - and at 64dp it would have made each of those rows half again as tall to reserve
 * room for something that is never drawn.
 */
public class TextFieldCompactTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static BufferedImage paint(MD3TextField field) {
        field.setSize(field.getPreferredSize());

        BufferedImage image = new BufferedImage(Math.max(1, field.getWidth()), Math.max(1, field.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        field.paint(g);
        g.dispose();

        return image;
    }

    private static int paintedPixels(BufferedImage image) {
        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    painted++;
                }
            }
        }

        return painted;
    }

    /**
     * The height a settings row was built for, and near enough to a dropdown's that a row holding
     * one of each reads as one line.
     *
     * <p>
     * Not exactly equal on purpose: both take the larger of the token height and their content's,
     * and the two measure their content differently enough to land a pixel or two apart. They are
     * centred within the row, so that does not show - what would show is one of them at 56dp.
     */
    @Test
    public void testAFieldWithNoLabelIsTheHeightOfASettingsRowControl() {
        int field = new MD3TextField(16).getPreferredSize().height;
        int dropdown = new MD3ComboBox<String>().getPreferredSize().height;

        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), field,
                "a label-less field is not the height the settings rows were built for");

        assertTrue(Math.abs(field - dropdown) <= UIScale.scale(4),
                "a field and a dropdown in the same row are " + Math.abs(field - dropdown) + "px apart");
    }

    /**
     * The other half: a field that does have a label still gets the room to float it into, or the
     * label would be drawn over the outline.
     */
    @Test
    public void testAFieldWithALabelKeepsItsRoom() {
        assertTrue(new MD3TextField("Instance name").getPreferredSize().height
                > new MD3TextField(16).getPreferredSize().height,
                "a labelled field is no taller than one with no label, so the label has nowhere to float");
    }

    @Test
    public void testItStillPaintsItsContainer() {
        assertTrue(paintedPixels(paint(new MD3TextField(16))) > 0,
                "a label-less field painted no container, so it is a caret on an empty page");
    }

    /**
     * Three optional settings offer to empty themselves, through the two client properties FlatLaf's
     * own field UI reads. Swapping the component out would have silently dropped that - the
     * properties are strings, so nothing would have failed to compile and nothing would have failed
     * a test either; the little cross would just have stopped being drawn.
     */
    @Test
    public void testAClearableFieldDrawsItsCrossOnceThereIsSomethingToClear() {
        MD3TextField empty = new MD3TextField(16);
        empty.putClientProperty("JTextField.showClearButton", true);

        MD3TextField filled = new MD3TextField(16);
        filled.putClientProperty("JTextField.showClearButton", true);
        filled.setText("an api key");

        assertTrue(paintedInClearSlot(filled) > 0,
                "a clearable field with something in it drew no cross to clear it with");
        assertEquals(0, paintedInClearSlot(empty),
                "an empty field offered to clear itself, which is an offer to do nothing");
    }

    /**
     * How much is drawn where the clear icon goes - against the trailing edge, on the centre line.
     */
    private static int paintedInClearSlot(MD3TextField field) {
        BufferedImage image = paint(field);

        int size = UIScale.scale(MD3Spacing.ICON_SIZE);
        int x = image.getWidth() - UIScale.scale(MD3Spacing.M) - size;
        int y = (image.getHeight() - size) / 2;
        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int row = y; row < Math.min(image.getHeight(), y + size); row++) {
            for (int column = x; column < Math.min(image.getWidth(), x + size); column++) {
                if (image.getRGB(column, row) != surface) {
                    painted++;
                }
            }
        }

        return painted;
    }

    @Test
    public void testClearingRunsTheCallbackTheCallSitesRegister() {
        MD3TextField field = new MD3TextField(16);
        boolean[] ran = { false };

        field.putClientProperty("JTextField.showClearButton", true);
        field.putClientProperty("JTextField.clearCallback", (Runnable) () -> ran[0] = true);
        field.setText("something");
        field.setSize(field.getPreferredSize());

        // the cross sits against the trailing edge, on the centre line
        int x = field.getWidth() - UIScale.scale(MD3Spacing.M) - UIScale.scale(MD3Spacing.ICON_SIZE) / 2;
        java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(field,
                java.awt.event.MouseEvent.MOUSE_RELEASED, 0L, 0, x, field.getHeight() / 2, 1, false);

        for (java.awt.event.MouseListener listener : field.getMouseListeners()) {
            listener.mouseReleased(click);
        }

        assertEquals("", field.getText(), "clicking the cross did not empty the field");
        assertTrue(ran[0], "clicking the cross did not run the callback the setting registered");
    }

    /**
     * The columns constructor is the whole reason the swap was a one-liner at 24 call sites.
     */
    @Test
    public void testTheColumnsConstructorSetsColumnsAndNotTheLabel() {
        MD3TextField field = new MD3TextField(16);

        assertEquals(16, field.getColumns(), "the columns constructor did not set the columns");
        assertEquals(null, field.getLabel(), "the columns constructor set a label");
        assertEquals("", field.getText(), "the columns constructor put something in the field");
    }
}
