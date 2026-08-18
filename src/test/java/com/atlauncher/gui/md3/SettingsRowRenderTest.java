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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.tabs.settings.AbstractSettingsTab;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * Paints a section of the settings.
 *
 * <p>
 * A setting used to be two cells of a {@link java.awt.GridBagLayout} - a right-aligned label with a
 * help icon, and a control - and what it actually did lived in a tooltip on that icon. It is now a
 * row: the name, what it does underneath, and the control on the trailing edge.
 *
 * <p>
 * The failure this guards against is the description eating the row: these run to a paragraph, and
 * a page where every setting is four lines tall cannot be scanned. So it is capped, with the whole
 * text still reachable as a tooltip.
 */
public class SettingsRowRenderTest {
    private static final int PAGE_WIDTH = 900;
    private static final int PAGE_HEIGHT = 360;

    private static final String LONG_HELP = "The Tray Menu is a little icon that shows in your system taskbar "
            + "which allows you to perform different functions to do various things with the launcher such as "
            + "hiding or showing the console, killing Minecraft or closing ATLauncher.";

    /** A section built by hand, so the test does not need a view model or the network. */
    private static final class Section extends AbstractSettingsTab {
        JComponent briefRow;
        JComponent verboseRow;
        JComponent unexplainedRow;

        @Override
        protected void onShow() {
            addSection("Appearance");
            briefRow = addRow("Theme", "This sets the theme that the launcher will use.", new JComboBox<String>());
            verboseRow = addRow("Enable Tray Menu", LONG_HELP, new JCheckBox());
            unexplainedRow = addRow("Keep Launcher Open", null, new JCheckBox());
            addWideRow("Java Parameters", "Extra Java command line paramaters can be added here.",
                    new JTextField(20));
        }

        @Override
        protected void createViewModel() {
        }

        @Override
        protected void onDestroy() {
            removeAll();
        }

        @Override
        public String getTitle() {
            return "Test";
        }

        @Override
        public String getAnalyticsScreenViewName() {
            return "Test";
        }

        void build() {
            onShow();
        }
    }

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    /**
     * A row re-wraps its description once it knows its width, which changes the height it asks for.
     * In the launcher its revalidate schedules another pass; laying it out by hand, each container
     * is invalidated first so its layout manager drops the sizes it cached beforehand.
     */
    private static void layoutTree(Component c) {
        if (c instanceof Container) {
            c.invalidate();
        }

        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private Section buildSection() {
        Section section = new Section();
        section.build();
        section.setSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));

        layoutTree(section);
        section.invalidate();
        layoutTree(section);

        return section;
    }

    private static JLabel supportingOf(Container row) {
        JLabel found = null;

        for (Component c : row.getComponents()) {
            if (c instanceof Container) {
                for (Component text : ((Container) c).getComponents()) {
                    // the headline comes first, so the last label in the text block is the
                    // description
                    if (text instanceof JLabel) {
                        found = (JLabel) text;
                    }
                }
            }
        }

        return found;
    }

    @Test
    public void testSectionRenders() throws Exception {
        Section section = buildSection();

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        section.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/settings-dark.png"));
    }

    @Test
    public void testALongDescriptionDoesNotTakeOverTheRow() {
        Section section = buildSection();

        Component brief = section.briefRow;
        Component verbose = section.verboseRow;

        assertTrue(verbose.getHeight() > 0, "the row was never laid out");
        assertTrue(verbose.getHeight() <= brief.getHeight() * 2,
                "a paragraph of help text made its row " + verbose.getHeight() + "px against a normal "
                        + brief.getHeight() + "px, so the page cannot be scanned");
    }

    @Test
    public void testTheWholeDescriptionStaysReachable() {
        Section section = buildSection();
        JLabel supporting = supportingOf(section.verboseRow);

        assertNotNull(supporting, "the row lost its description");
        assertTrue(supporting.getText().contains("…"),
                "the description was not shortened, so the cap is not being applied");
        assertEquals(LONG_HELP, supporting.getToolTipText(),
                "the full description is no longer reachable, so shortening it lost information");
    }

    @Test
    public void testASettingWithNothingToExplainGetsNoDescription() {
        Section section = buildSection();
        Container row = section.unexplainedRow;

        JPanel text = null;

        for (Component c : row.getComponents()) {
            if (c instanceof JPanel && ((Container) c).getComponentCount() > 0
                    && ((Container) c).getComponent(0) instanceof JLabel) {
                text = (JPanel) c;

                break;
            }
        }

        assertNotNull(text, "the row lost its label");
        assertEquals(1, text.getComponentCount(),
                "a setting with no help text still reserved room for a description");
    }
}
