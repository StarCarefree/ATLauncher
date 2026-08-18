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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3ButtonGroup;
import com.atlauncher.gui.md3.button.MD3Fab;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.button.MD3MenuButton;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.util.UIScale;

/**
 * The button scale that the rest of the launcher is built on.
 *
 * <p>
 * These pin down the things a size, a tone or a selected state can get wrong without throwing:
 * a small button that is the same height as a medium one, a Delete that is still painted in
 * primary, a selected outlined segment that never fills in.
 *
 * <p>
 * Sheets land in {@code build/md3-preview}.
 */
public class MD3ButtonRenderTest {
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
        BufferedImage image = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();

        return image;
    }

    private static void sizeToPreferred(Component c) {
        c.setSize(c.getPreferredSize());
    }

    @Test
    public void testTheThreeSizesAreActuallyDifferentHeights() {
        MD3Button small = MD3Button.filled("Play").withButtonSize(MD3Button.Size.SMALL);
        MD3Button medium = MD3Button.filled("Play");
        MD3Button large = MD3Button.filled("Play").withButtonSize(MD3Button.Size.LARGE);

        assertTrue(small.getPreferredSize().height >= UIScale.scale(MD3Spacing.BUTTON_HEIGHT_SMALL));
        assertTrue(medium.getPreferredSize().height >= UIScale.scale(MD3Spacing.BUTTON_HEIGHT));
        assertTrue(large.getPreferredSize().height >= UIScale.scale(MD3Spacing.BUTTON_HEIGHT_LARGE));
        assertTrue(small.getPreferredSize().height < medium.getPreferredSize().height);
        assertTrue(medium.getPreferredSize().height < large.getPreferredSize().height);
    }

    @Test
    public void testAFilledErrorButtonIsPaintedInTheErrorRole() throws Exception {
        MD3Button button = MD3Button.filledError("Delete");
        sizeToPreferred(button);

        BufferedImage image = paint(button);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/button-error-dark.png"));

        assertEquals(MD3Color.error().getRGB(), fillSample(image).getRGB(),
                "a filled error button is not painted in the error role");
    }

    @Test
    public void testASelectedOutlinedButtonFillsIn() throws Exception {
        MD3Button selected = MD3Button.outlined("Grid");
        selected.setSelected(true);

        sizeToPreferred(selected);

        BufferedImage selectedImage = paint(selected);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(selectedImage, "png", new File("build/md3-preview/button-selected-dark.png"));

        assertEquals(MD3Color.secondaryContainer().getRGB(), fillSample(selectedImage).getRGB(),
                "a selected outlined button did not fill with its container");
    }

    @Test
    public void testATrailingIconMakesTheButtonWider() {
        MD3Button plain = MD3Button.outlined("Open");
        MD3Button withChevron = MD3Button.outlined("Open")
                .withTrailingIcon(MD3Icon.of(MD3Icons.CHEVRON_DOWN));

        assertTrue(withChevron.getPreferredSize().width > plain.getPreferredSize().width,
                "a trailing icon did not change the button's width, so it is being drawn on top of the label");
    }

    @Test
    public void testAButtonGroupSelectsOneSegment() throws Exception {
        MD3ButtonGroup group = new MD3ButtonGroup();
        group.addOption("Name");
        group.addOption("Date");
        group.addOption("Size");
        group.setSelectedIndex(1);
        group.setSize(group.getPreferredSize());
        layoutTree(group);

        assertEquals(1, group.getSelectedIndex());
        assertTrue(group.getOption(1).isSelected());
        assertTrue(!group.getOption(0).isSelected());
        assertEquals(MD3Button.Segment.START, group.getOption(0).getSegment());
        assertEquals(MD3Button.Segment.MIDDLE, group.getOption(1).getSegment());
        assertEquals(MD3Button.Segment.END, group.getOption(2).getSegment());

        BufferedImage image = paint(group);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/button-group-dark.png"));
    }

    @Test
    public void testAMenuButtonCarriesAChevron() {
        MD3MenuButton menu = MD3MenuButton.outlined("Play", new JPopupMenu());
        MD3Button plain = MD3Button.outlined("Play");

        assertTrue(menu.getTrailingIcon() != null, "a menu button has no trailing chevron");
        assertTrue(menu.getPreferredSize().width > plain.getPreferredSize().width,
                "a menu button is no wider than the same label without a chevron");
    }

    /**
     * An icon button is square, and big enough to hit.
     *
     * <p>
     * The size it occupies is not the size of the circle it draws. Material's medium container is
     * 40dp and its floor for a pointer target is 48, so the component is the target and the container
     * is centred in it - which is why this asks about the minimum rather than about
     * {@code ICON_BUTTON_SIZE}. The small one is deliberately exempt: it is the dense variant, for
     * somewhere the row or card around it is a target of its own.
     */
    @Test
    public void testIconButtonSizesAreSquareAndBigEnoughToHit() {
        MD3IconButton small = new MD3IconButton(MD3Icons.MORE_VERT, "More", MD3IconButton.Variant.STANDARD,
                MD3IconButton.Size.SMALL);
        MD3IconButton medium = new MD3IconButton(MD3Icons.MORE_VERT, "More");
        MD3IconButton large = new MD3IconButton(MD3Icons.MORE_VERT, "More", MD3IconButton.Variant.STANDARD,
                MD3IconButton.Size.LARGE);

        for (MD3IconButton button : new MD3IconButton[] { small, medium, large }) {
            assertEquals(button.getPreferredSize().width, button.getPreferredSize().height,
                    "an icon button is not square");
        }

        assertEquals(UIScale.scale(MD3Spacing.ICON_BUTTON_SIZE_SMALL), small.getPreferredSize().width,
                "the dense icon button no longer takes a dense amount of room");
        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), medium.getPreferredSize().width,
                "the default icon button is smaller than the minimum a pointer has to hit");
        assertEquals(UIScale.scale(MD3Spacing.MIN_TOUCH_TARGET), large.getPreferredSize().width,
                "the large icon button is smaller than the minimum a pointer has to hit");
    }

    @Test
    public void testASelectedStandardIconButtonChangesColour() throws Exception {
        MD3IconButton resting = new MD3IconButton(MD3Icons.GRID_VIEW, "Grid");
        MD3IconButton selected = new MD3IconButton(MD3Icons.GRID_VIEW, "Grid");
        selected.setSelected(true);

        sizeToPreferred(resting);
        sizeToPreferred(selected);

        BufferedImage restingImage = paint(resting);
        BufferedImage selectedImage = paint(selected);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(selectedImage, "png", new File("build/md3-preview/icon-button-selected-dark.png"));

        assertTrue(!imagesEqual(restingImage, selectedImage),
                "selecting a standard icon button did not change what is painted");
    }

    @Test
    public void testSmallAndExtendedFabsDifferFromTheRegularOne() throws Exception {
        MD3Fab regular = new MD3Fab(MD3Icons.ADD, "Create");
        MD3Fab small = MD3Fab.small(MD3Icons.ADD, "Create");
        MD3Fab extended = MD3Fab.extended(MD3Icons.ADD, "Create instance");

        assertTrue(small.getPreferredSize().width < regular.getPreferredSize().width);
        assertTrue(extended.getPreferredSize().width > extended.getPreferredSize().height,
                "an extended FAB is still square, so its label has nowhere to go");

        sizeToPreferred(extended);
        BufferedImage image = paint(extended);

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/fab-extended-dark.png"));
    }

    /**
     * A pixel of the container, not of the label. Taken a quarter of the height in from the leading
     * edge, which is inside a stadium's fill and well clear of centred text.
     */
    private static Color fillSample(BufferedImage image) {
        return new Color(image.getRGB(Math.max(1, image.getHeight() / 4), image.getHeight() / 2));
    }

    private static boolean imagesEqual(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }
}
