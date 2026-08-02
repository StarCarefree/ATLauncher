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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The number field, in the shapes the settings pages give it.
 *
 * <p>
 * The steppers are the point of the control - a plain number field would have been less work and
 * would have thrown away the step the model already defines - so the tests that matter are that
 * they are there, that they step by what the model says, and that they survive the call sites
 * replacing the editor after construction.
 */
public class SpinnerRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /** The launcher's memory setting: 512MB at a time, which is what makes the step worth having. */
    private static MD3Spinner memory() {
        SpinnerNumberModel model = new SpinnerNumberModel(4096, null, null, 512);
        model.setMinimum(512);
        model.setMaximum(16384);

        return new MD3Spinner(model);
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static BufferedImage paint(Component c) {
        c.setSize(c.getPreferredSize());
        layoutTree(c);

        BufferedImage image = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        c.paint(g);
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

    private static AbstractButton stepper(MD3Spinner spinner, String name) {
        for (Component child : spinner.getComponents()) {
            if (name.equals(child.getName())) {
                return (AbstractButton) child;
            }
        }

        return null;
    }

    @Test
    public void testItPaintsAContainer() {
        assertTrue(paintedPixels(paint(memory())) > 0, "the spinner painted nothing at all");
    }

    /**
     * The height a settings row was built for, matching the field and the dropdown beside it.
     */
    @Test
    public void testItIsTheHeightOfTheOtherRowControls() {
        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), memory().getPreferredSize().height,
                "the spinner is not the height the settings rows were built for");
    }

    /**
     * Both steppers exist and are laid out where the container leaves room for them - against the
     * trailing edge, one above the other.
     */
    @Test
    public void testBothSteppersAreLaidOutAgainstTheTrailingEdge() {
        MD3Spinner spinner = memory();
        spinner.setSize(spinner.getPreferredSize());
        layoutTree(spinner);

        AbstractButton up = stepper(spinner, "Spinner.nextButton");
        AbstractButton down = stepper(spinner, "Spinner.previousButton");

        assertNotNull(up, "the spinner has no increment stepper");
        assertNotNull(down, "the spinner has no decrement stepper");

        assertTrue(up.getWidth() > 0 && up.getHeight() > 0, "the increment stepper was laid out at no size");
        assertTrue(down.getY() >= up.getY() + up.getHeight() - 1,
                "the steppers are not stacked - decrement is at " + down.getY() + ", increment ends at "
                        + (up.getY() + up.getHeight()));
        assertTrue(up.getX() + up.getWidth() <= spinner.getWidth(), "a stepper hangs off the end of the spinner");
    }

    /**
     * What the steppers are for. Pressing one moves the value by the model's step, not by one -
     * which is the whole reason this is a spinner rather than a number field.
     */
    @Test
    public void testASteppersMovesTheValueByTheModelsStep() {
        MD3Spinner spinner = memory();
        spinner.setSize(spinner.getPreferredSize());
        layoutTree(spinner);

        stepper(spinner, "Spinner.nextButton").doClick();
        assertEquals(4608, spinner.getValue(), "the increment stepper did not step by the model's 512");

        stepper(spinner, "Spinner.previousButton").doClick();
        stepper(spinner, "Spinner.previousButton").doClick();
        assertEquals(3584, spinner.getValue(), "the decrement stepper did not step by the model's 512");
    }

    /**
     * Three call sites install a {@code NumberEditor} after construction, to drop the thousands
     * separator. That replaces the field the UI had styled and the one it watches for focus, so it
     * has to be caught and done again - otherwise those three spinners keep a bordered, opaque box
     * inside the Material container.
     */
    @Test
    public void testReplacingTheEditorRestylesIt() {
        MD3Spinner spinner = memory();

        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();

        assertEquals(null, editor.getTextField().getBorder(), "a replaced editor kept its own border");
        assertEquals(false, editor.getTextField().isOpaque(),
                "a replaced editor is opaque and will paint over the container");
        assertEquals(MD3Color.onSurface(), editor.getTextField().getForeground(),
                "a replaced editor did not take the theme's text colour");
    }

    @Test
    public void testADisabledSpinnerLooksDisabled() {
        MD3Spinner enabled = memory();
        MD3Spinner disabled = memory();
        disabled.setEnabled(false);

        BufferedImage a = paint(enabled);
        BufferedImage b = paint(disabled);
        int differing = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        assertTrue(differing > 0, "a disabled spinner paints the same as one you can use");
    }

    @Test
    public void testRenderTheSheet() throws Exception {
        JPanel sheet = new JPanel(new GridBagLayout());
        sheet.setOpaque(true);
        sheet.setBackground(MD3Color.surface());

        GridBagConstraints labels = new GridBagConstraints();
        labels.gridx = 0;
        labels.anchor = GridBagConstraints.WEST;
        labels.insets = new Insets(8, 16, 8, 16);

        GridBagConstraints controls = new GridBagConstraints();
        controls.gridx = 1;
        controls.anchor = GridBagConstraints.WEST;
        controls.insets = new Insets(8, 0, 8, 16);

        MD3Spinner port = new MD3Spinner(new SpinnerNumberModel(8080, 1, 65535, 1));
        port.setEditor(new JSpinner.NumberEditor(port, "#"));

        MD3Spinner disabled = memory();
        disabled.setEnabled(false);

        String[] names = { "memory (512 step)", "port (no separator)", "disabled" };
        MD3Spinner[] spinners = { memory(), port, disabled };

        for (int i = 0; i < names.length; i++) {
            JLabel label = new JLabel(names[i]);
            label.setForeground(MD3Color.onSurfaceVariant());

            labels.gridy = i;
            controls.gridy = i;

            sheet.add(label, labels);
            sheet.add(spinners[i], controls);
        }

        Dimension size = sheet.getPreferredSize();
        sheet.setPreferredSize(new Dimension(size.width + 32, size.height + 16));

        BufferedImage image = paint(sheet);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/spinner-dark.png"));

        assertTrue(paintedPixels(image) > 0, "the sheet came out blank");
    }
}
