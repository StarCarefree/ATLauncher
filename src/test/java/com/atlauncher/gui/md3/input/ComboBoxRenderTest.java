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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.ComboItem;
import com.formdev.flatlaf.util.UIScale;

/**
 * The dropdown, in the states a settings page puts it in.
 *
 * <p>
 * Rendered offscreen rather than asserted on: what matters about a control that draws its own
 * container is whether the container is there, distinguishable between variants and states, and the
 * right height for the row it sits in. The sheet is for a person to look at; the assertions are the
 * failures that look like nothing at all - a container that never got painted, a disabled control
 * that looks enabled, a height that would make every settings row taller.
 */
public class ComboBoxRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static MD3ComboBox<ComboItem<String>> combo(MD3ComboBox.Variant variant) {
        MD3ComboBox<ComboItem<String>> box = new MD3ComboBox<>(variant);

        box.addItem(new ComboItem<>("en", "English"));
        box.addItem(new ComboItem<>("zh", "简体中文"));
        box.addItem(new ComboItem<>("de", "Deutsch"));

        return box;
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

    private static BufferedImage paintAtPreferredSize(Component c) {
        c.setSize(c.getPreferredSize());
        layoutTree(c);

        return paint(c);
    }

    private static int differingPixels(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return Integer.MAX_VALUE;
        }

        int differing = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        return differing;
    }

    /** How much of the control is something other than the page behind it. */
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
     * The whole point of the control: it draws a container, so it reads as something to click rather
     * than as a word sitting on the page.
     */
    @Test
    public void testTheControlPaintsAContainer() {
        assertTrue(paintedPixels(paintAtPreferredSize(combo(MD3ComboBox.Variant.OUTLINED))) > 0,
                "an outlined dropdown painted nothing at all");
        assertTrue(paintedPixels(paintAtPreferredSize(combo(MD3ComboBox.Variant.FILLED))) > 0,
                "a filled dropdown painted nothing at all");
    }

    @Test
    public void testTheTwoVariantsAreTellableApart() {
        assertTrue(differingPixels(paintAtPreferredSize(combo(MD3ComboBox.Variant.OUTLINED)),
                paintAtPreferredSize(combo(MD3ComboBox.Variant.FILLED))) > 0,
                "the filled and outlined dropdowns paint identically, so the variant does nothing");
    }

    @Test
    public void testADisabledDropdownLooksDisabled() {
        MD3ComboBox<ComboItem<String>> enabled = combo(MD3ComboBox.Variant.OUTLINED);
        MD3ComboBox<ComboItem<String>> disabled = combo(MD3ComboBox.Variant.OUTLINED);
        disabled.setEnabled(false);

        assertTrue(differingPixels(paintAtPreferredSize(enabled), paintAtPreferredSize(disabled)) > 0,
                "a disabled dropdown paints the same as one you can use");
    }

    /**
     * 40dp, the same as a search field. A settings row is two lines of text tall, so anything up to
     * that costs nothing; Material's own 56dp exposed dropdown would make every row in the launcher's
     * settings taller for a label the row already has on its other side.
     */
    @Test
    public void testItIsTheHeightASettingsRowCanAbsorb() {
        assertEquals(UIScale.scale(MD3Spacing.FIELD_HEIGHT_COMPACT),
                combo(MD3ComboBox.Variant.OUTLINED).getPreferredSize().height,
                "the dropdown is not the height a settings row was built for");
    }

    /**
     * It is a {@code JComboBox} and has to keep behaving like one - forty call sites drive it through
     * that API and were not touched when they were swapped over.
     */
    @Test
    public void testItStillBehavesLikeAComboBox() {
        MD3ComboBox<ComboItem<String>> box = combo(MD3ComboBox.Variant.OUTLINED);
        List<Object> selections = new ArrayList<>();

        box.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selections.add(e.getItem());
            }
        });

        box.setSelectedIndex(2);

        assertEquals(3, box.getItemCount(), "items added through addItem did not land in the model");
        assertEquals(2, box.getSelectedIndex(), "the selection did not take");
        assertEquals("Deutsch", box.getSelectedItem().toString(), "the wrong item is selected");
        assertEquals(1, selections.size(), "selecting an item did not reach the item listener");

        box.removeAllItems();

        assertEquals(0, box.getItemCount(), "removeAllItems left something behind");
    }

    /**
     * A sheet of the states, for looking at.
     */
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

        String[] names = { "outlined", "filled", "disabled", "long value" };
        MD3ComboBox<?>[] boxes = new MD3ComboBox<?>[names.length];

        boxes[0] = combo(MD3ComboBox.Variant.OUTLINED);
        boxes[1] = combo(MD3ComboBox.Variant.FILLED);
        boxes[2] = combo(MD3ComboBox.Variant.OUTLINED);
        boxes[2].setEnabled(false);

        MD3ComboBox<ComboItem<String>> wide = new MD3ComboBox<>(MD3ComboBox.Variant.OUTLINED);
        wide.addItem(new ComboItem<>("x", "Instance Name (Pack Name Pack Version)"));
        boxes[3] = wide;

        for (int i = 0; i < names.length; i++) {
            JLabel label = new JLabel(names[i]);
            label.setForeground(MD3Color.onSurfaceVariant());

            labels.gridy = i;
            controls.gridy = i;

            sheet.add(label, labels);
            sheet.add(boxes[i], controls);
        }

        Dimension size = sheet.getPreferredSize();
        sheet.setSize(new Dimension(size.width + 32, size.height + 16));
        layoutTree(sheet);

        BufferedImage image = paint(sheet);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/combobox-dark.png"));

        assertTrue(paintedPixels(image) > 0, "the sheet came out blank");
    }
}
