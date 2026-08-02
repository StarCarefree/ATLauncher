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
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The checkbox, in the states the dialogs put it in.
 *
 * <p>
 * Held at its two ends rather than raced. What these catch is the failure that looks like nothing:
 * a box that paints no container, a ticked one indistinguishable from a clear one, or a target too
 * small to hit.
 */
public class CheckboxRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
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

    private static MD3Checkbox box(boolean selected) {
        MD3Checkbox box = new MD3Checkbox();
        box.setSelected(selected);

        return box;
    }

    @Test
    public void testAClearBoxStillPaintsItsOutline() {
        assertTrue(paintedPixels(paint(box(false))) > 0,
                "a clear checkbox painted nothing, so there is no box to tick");
    }

    @Test
    public void testATickedBoxLooksNothingLikeAClearOne() {
        assertTrue(differingPixels(paint(box(false)), paint(box(true))) > 0,
                "ticking the box changed nothing on screen");
        assertTrue(paintedPixels(paint(box(true))) > paintedPixels(paint(box(false))),
                "a ticked box is not more filled in than a clear one");
    }

    @Test
    public void testADisabledBoxLooksDisabled() {
        MD3Checkbox disabled = box(true);
        disabled.setEnabled(false);

        assertTrue(differingPixels(paint(box(true)), paint(disabled)) > 0,
                "a disabled checkbox paints the same as one you can tick");
    }

    /**
     * Material's box is 18dp, but the thing you have to hit is the 40dp target around it - and Swing
     * clips an icon to the size it declares, so the state layer needs that room too.
     */
    @Test
    public void testTheTargetIsBigEnoughToHit() {
        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), box(false).getIcon().getIconWidth(),
                "the checkbox target is not the minimum touch size");
        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), box(false).getIcon().getIconHeight(),
                "the checkbox target is not the minimum touch size");
    }

    /**
     * A box in a list of eighty mods cannot have 40dp a row, or the list is twice as long. The box
     * itself does not change - it is the same control, with less room around it - and the gap the
     * target used to provide is asked for instead, or the label runs into the box.
     */
    @Test
    public void testACompactBoxIsSmallerAroundTheSameBox() {
        MD3Checkbox standalone = new MD3Checkbox("Sodium");
        MD3Checkbox inList = new MD3Checkbox("Sodium");
        inList.setCompact(true);

        assertTrue(inList.getIcon().getIconHeight() < standalone.getIcon().getIconHeight(),
                "a compact checkbox takes as much room as a standalone one");
        assertTrue(inList.getIconTextGap() > standalone.getIconTextGap(),
                "a compact checkbox does not make up the gap its smaller target stopped providing");

        // and it is still a Material box, not a smaller drawing of one
        assertTrue(paintedPixels(paint(inList)) > 0, "a compact checkbox painted nothing");
    }

    /**
     * It is a {@code JCheckBox} and stays one - the call sites drive it through that API.
     */
    @Test
    public void testItStillBehavesLikeACheckBox() {
        MD3Checkbox box = new MD3Checkbox("Select All");
        int[] fired = { 0 };

        box.addActionListener(e -> fired[0]++);

        assertEquals(false, box.isSelected(), "a new checkbox started out ticked");

        box.setSelected(true);
        assertEquals(true, box.isSelected(), "setSelected did not take");

        box.doClick();
        assertEquals(false, box.isSelected(), "clicking a ticked box did not clear it");
        assertEquals(1, fired[0], "clicking did not reach the action listener");
        assertEquals("Select All", box.getText(), "the label did not survive construction");
    }

    @Test
    public void testRenderTheSheet() throws Exception {
        JPanel sheet = new JPanel(new GridBagLayout());
        sheet.setOpaque(true);
        sheet.setBackground(MD3Color.surface());

        GridBagConstraints labels = new GridBagConstraints();
        labels.gridx = 0;
        labels.anchor = GridBagConstraints.WEST;
        labels.insets = new Insets(4, 16, 4, 16);

        GridBagConstraints controls = new GridBagConstraints();
        controls.gridx = 1;
        controls.anchor = GridBagConstraints.WEST;
        controls.insets = new Insets(4, 0, 4, 16);

        String[] names = { "clear", "ticked", "disabled clear", "disabled ticked", "with a label" };
        MD3Checkbox[] boxes = { box(false), box(true), box(false), box(true), new MD3Checkbox("Select All") };

        boxes[2].setEnabled(false);
        boxes[3].setEnabled(false);

        for (int i = 0; i < names.length; i++) {
            JLabel label = new JLabel(names[i]);
            label.setForeground(MD3Color.onSurfaceVariant());

            labels.gridy = i;
            controls.gridy = i;

            sheet.add(label, labels);
            sheet.add(boxes[i], controls);
        }

        Dimension size = sheet.getPreferredSize();
        sheet.setPreferredSize(new Dimension(size.width + 32, size.height + 16));

        BufferedImage image = paint(sheet);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/checkbox-dark.png"));

        assertTrue(paintedPixels(image) > 0, "the sheet came out blank");
    }
}
