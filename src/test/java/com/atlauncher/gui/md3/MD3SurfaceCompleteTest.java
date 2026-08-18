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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JToolTip;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.data.json.Mod;
import com.atlauncher.gui.card.ModCard;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.feedback.MD3TooltipUI;
import com.atlauncher.gui.md3.input.MD3Slider;
import com.atlauncher.gui.md3.input.MD3TextArea;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The second wave of component work: tooltip chrome, multiline fields, sliders, and the cards
 * that were still painting themselves by hand.
 */
public class MD3SurfaceCompleteTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static BufferedImage paint(java.awt.Component c) {
        c.setSize(Math.max(1, c.getPreferredSize().width), Math.max(1, c.getPreferredSize().height));

        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();

        return image;
    }

    @Test
    public void testTooltipsUseTheMaterialDelegate() {
        assertEquals(MD3TooltipUI.class.getName(), UIManager.get("ToolTipUI"));

        JToolTip tip = new JToolTip();
        tip.setTipText("Saved");
        tip.updateUI();

        BufferedImage image = paint(tip);

        assertEquals(MD3Color.inverseSurface().getRGB(),
                new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2)).getRGB(),
                "a tooltip did not fill with the inverse surface");
    }

    @Test
    public void testATextAreaContainerPaintsAField() {
        MD3TextArea area = new MD3TextArea(3, 20);
        area.setText("notes");

        BufferedImage image = paint(area.contained(80));
        int surface = MD3Color.surface().getRGB();
        int painted = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != surface) {
                    painted++;
                }
            }
        }

        assertTrue(painted > 0, "a contained text area painted nothing");
    }

    @Test
    public void testASliderIsAFullTouchTarget() {
        MD3Slider slider = new MD3Slider(0, 100, 40);

        assertTrue(slider.getPreferredSize().height >= UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET),
                "a slider is shorter than 48dp");
    }

    @Test
    public void testAnElevatedCardRestsOnTheLowContainer() {
        MD3Card card = new MD3Card(MD3Card.Variant.ELEVATED);
        card.setSize(120, 80);

        BufferedImage image = paint(card);

        assertEquals(MD3Elevation.surface(MD3Elevation.LEVEL1).getRGB(),
                new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2)).getRGB(),
                "an elevated card is not sitting on surface-container-low");
    }

    @Test
    public void testAModCardIsAMaterialCard() {
        Mod mod = new Mod();
        mod.name = "Sodium";
        mod.optional = true;

        ModCard card = new ModCard(mod);

        assertTrue(card instanceof MD3Card, "the view-mods card is still a raw panel");
        assertTrue(card.getPreferredSize().height >= UIScale.scale(MD3Spacing.LIST_ITEM_HEIGHT_ONE_LINE) / 2);
    }

    @Test
    public void testAConnectedGroupUsesTheLargerInnerCorner() {
        assertEquals(MD3Shape.SMALL, MD3Shape.BUTTON_GROUP_INNER);
    }

    @Test
    public void testASliderThumbMovesWithTheValue() {
        MD3Slider low = new MD3Slider(0, 100, 0);
        MD3Slider high = new MD3Slider(0, 100, 100);
        low.setSize(200, UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET));
        high.setSize(200, UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET));

        assertNotEquals(paint(low).getRGB(20, low.getHeight() / 2), paint(high).getRGB(20, high.getHeight() / 2),
                "a slider at 0 paints the same leading track as one at 100");
    }
}
