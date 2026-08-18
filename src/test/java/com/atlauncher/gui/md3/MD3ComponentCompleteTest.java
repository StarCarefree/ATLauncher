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
package com.atlauncher.gui.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.accessibility.AccessibleRole;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3Radio;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The design gaps the audit asked to close: hit targets, variant paint, search anatomy,
 * inverse-surface content, and the radio that was missing from the set.
 */
public class MD3ComponentCompleteTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static BufferedImage paint(java.awt.Component c) {
        c.setSize(c.getPreferredSize());

        BufferedImage image = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();

        return image;
    }

    private static Color fillSample(BufferedImage image) {
        return new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void testAChipIsAFullTouchTarget() {
        MD3Chip chip = MD3Chip.filter("Fabric");

        assertTrue(chip.getPreferredSize().height >= UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET),
                "a chip is shorter than the 48dp target");
    }

    @Test
    public void testAssistAndInputChipsDoNotFillWhenSelected() {
        MD3Chip assist = MD3Chip.assist("Open folder", MD3Icons.FOLDER);
        assist.setSelected(true);

        MD3Chip input = MD3Chip.input("sodium");
        input.setSelected(true);

        assertNotEquals(MD3Color.secondaryContainer().getRGB(), fillSample(paint(assist)).getRGB(),
                "an assist chip filled like a selected filter");
        assertNotEquals(MD3Color.secondaryContainer().getRGB(), fillSample(paint(input)).getRGB(),
                "an input chip filled like a selected filter");
    }

    @Test
    public void testAnInputChipCloseRemovesIt() {
        AtomicInteger removed = new AtomicInteger();
        MD3Chip chip = MD3Chip.input("sodium");
        chip.addRemoveListener(e -> removed.incrementAndGet());
        chip.setSize(chip.getPreferredSize());

        java.awt.Rectangle close = com.atlauncher.gui.md3.input.MD3ChipUI.closeBounds(chip);
        java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(chip,
                java.awt.event.MouseEvent.MOUSE_RELEASED, 0L, 0, close.x + close.width / 2,
                close.y + close.height / 2, 1, false);

        for (java.awt.event.MouseListener listener : chip.getMouseListeners()) {
            listener.mouseReleased(click);
        }

        assertEquals(1, removed.get(), "clicking the close on an input chip did not remove it");
    }

    @Test
    public void testSearchCarriesItsMagnifier() {
        assertNotNull(MD3TextField.search("Search packs").getLeadingIcon(),
                "the search factory did not install the magnifier it documents");
    }

    @Test
    public void testASwitchReportsAToggleRoleAndAFullTarget() {
        MD3Switch toggle = new MD3Switch("Reduce animations");

        assertEquals(AccessibleRole.TOGGLE_BUTTON, toggle.getAccessibleContext().getAccessibleRole());
        assertTrue(toggle.getIcon().getIconHeight() >= UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET),
                "the switch icon is shorter than the halo it draws");
    }

    @Test
    public void testAStandardSelectedIconButtonFillsItsContainer() {
        MD3IconButton button = new MD3IconButton(MD3Icons.MORE_VERT, "More");
        button.setSelected(true);

        BufferedImage image = paint(button);
        Color sample = new Color(image.getRGB(Math.max(1, image.getHeight() / 4), image.getHeight() / 2));

        assertEquals(MD3Color.secondaryContainer().getRGB(), sample.getRGB(),
                "a selected standard icon button has no container");
    }

    @Test
    public void testAContentOverrideBeatsTheVariantColour() {
        MD3Button button = MD3Button.text("Open folder")
                .withContentOverride(MD3Color.get(MD3Color.INVERSE_PRIMARY));
        button.setSize(button.getPreferredSize());

        BufferedImage image = paint(button);
        int inverse = MD3Color.get(MD3Color.INVERSE_PRIMARY).getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == inverse) {
                    painted++;
                }
            }
        }

        assertTrue(painted > 0, "a snackbar action still painted in primary");
    }

    @Test
    public void testAnInsetDividerLinesUpWithListText() {
        MD3Divider divider = MD3Divider.inset();
        divider.setSize(200, Math.max(1, UIScale.scale(MD3Spacing.DIVIDER_THICKNESS)));

        BufferedImage image = new BufferedImage(200, Math.max(1, divider.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        divider.paint(g);
        g.dispose();

        int textColumn = UIScale.scale(MD3Spacing.L + MD3Spacing.LIST_LEADING_COLUMN);

        assertEquals(MD3Color.surface().getRGB(), image.getRGB(0, 0),
                "the inset divider still starts at the container edge");
        assertEquals(MD3Color.outlineVariant().getRGB(), image.getRGB(textColumn, 0),
                "the inset divider does not start at the list text column");
    }

    @Test
    public void testAMenuItemIsAFullTouchTarget() {
        MD3MenuItem item = new MD3MenuItem("Edit mods");

        assertTrue(item.getPreferredSize().height >= UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET),
                "a menu item is shorter than 48dp");
    }

    @Test
    public void testARadioPaintsItsSelectedDot() {
        MD3Radio off = new MD3Radio("Forge");
        MD3Radio on = new MD3Radio("Fabric");
        on.setSelected(true);

        assertNotEquals(paintIcon(off).getRGB(paintIcon(off).getWidth() / 2, paintIcon(off).getHeight() / 2),
                paintIcon(on).getRGB(paintIcon(on).getWidth() / 2, paintIcon(on).getHeight() / 2),
                "a selected radio paints the same as a clear one");
    }

    private static BufferedImage paintIcon(MD3Radio radio) {
        int width = radio.getIcon().getIconWidth();
        int height = radio.getIcon().getIconHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, width, height);
        radio.getIcon().paintIcon(radio, g, 0, 0);
        g.dispose();

        return image;
    }
}
