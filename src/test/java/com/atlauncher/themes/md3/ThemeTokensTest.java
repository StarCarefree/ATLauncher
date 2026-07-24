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
package com.atlauncher.themes.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.themes.md3.hct.MdColorUtils;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * Installs every shipped theme for real and checks what lands in {@code UIManager}.
 *
 * <p>
 * The unit tests above prove the colour maths; this proves the wiring. A theme whose properties
 * publish no usable accent, or that somehow fails to reach the token layer at all, would pass every
 * other test in this package and still ship a launcher with grey holes in it.
 */
public class ThemeTokensTest {
    private static final String[] THEMES = {
            "com.atlauncher.themes.Dark",
            "com.atlauncher.themes.Light",
            "com.atlauncher.themes.MaterialDark",
            "com.atlauncher.themes.MaterialLight",
            "com.atlauncher.themes.MonokaiPro",
            "com.atlauncher.themes.DraculaContrast",
            "com.atlauncher.themes.HiberbeeDark",
            "com.atlauncher.themes.Vuesion",
            "com.atlauncher.themes.MaterialPalenightContrast",
            "com.atlauncher.themes.ArcOrange",
            "com.atlauncher.themes.CyanLight",
            "com.atlauncher.themes.HighTechDarkness",
            "com.atlauncher.themes.OneDark",
            "com.atlauncher.themes.Tokyonight",
            "com.atlauncher.themes.CatppuccinLatte",
            "com.atlauncher.themes.CatppuccinFrappe",
            "com.atlauncher.themes.CatppuccinMacchiato",
            "com.atlauncher.themes.CatppuccinMocha" };

    private static ATLauncherLaf install(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        clazz.getMethod("install").invoke(null);

        return (ATLauncherLaf) clazz.getMethod("getInstance").invoke(null);
    }

    @Test
    public void testEveryThemePublishesTheFullTokenSet() throws Exception {
        for (String className : THEMES) {
            install(className);

            for (String role : MD3Color.ALL_ROLES) {
                assertNotNull(UIManager.getColor(role), role + " missing under " + className);
            }

            assertNotNull(UIManager.get("md.sys.seed"), "seed missing under " + className);
            assertNotNull(UIManager.get("md.sys.dark"), "brightness missing under " + className);
        }
    }

    @Test
    public void testEveryThemeSeedsFromAColourfulAccent() throws Exception {
        for (String className : THEMES) {
            install(className);
            Color primary = UIManager.getColor(MD3Color.PRIMARY);

            // a scheme generated from a grey or near black seed collapses into something
            // indistinguishable from the surface, which is exactly what the seed guard prevents
            double contrast = MdColorUtils.contrastRatio(primary.getRGB(),
                    UIManager.getColor(MD3Color.SURFACE).getRGB());

            assertTrue(contrast >= 3.0, String.format("%s: primary only reaches %.2f:1 against its surface",
                    className, contrast));
        }
    }

    @Test
    public void testEveryThemeGetsTheShapeAndFocusTokens() throws Exception {
        for (String className : THEMES) {
            install(className);

            assertEquals(999, UIManager.getInt("Button.arc"), className + " did not get the button shape");
            assertEquals(2, UIManager.getInt("Component.focusWidth"), className + " did not get a focus ring");

            Color focus = UIManager.getColor("Component.focusColor");
            assertNotNull(focus, className + " has no focus colour");
            assertTrue(focus.getAlpha() > 0, className + " has an invisible focus ring");
        }
    }

    @Test
    public void testOnlyMaterialThemesTakeTheGeneratedColours() throws Exception {
        ATLauncherLaf material = install("com.atlauncher.themes.MaterialDark");
        assertTrue(material.isMaterialColors());
        assertEquals(UIManager.getColor(MD3Color.SURFACE), UIManager.getColor("Panel.background"),
                "Material Dark should paint panels with its generated surface");
        assertEquals(UIManager.getColor(MD3Color.OUTLINE_VARIANT), UIManager.getColor("Separator.foreground"),
                "Material Dark should draw dividers with its generated outline");
        assertEquals(UIManager.getColor(MD3Color.SECONDARY_CONTAINER), UIManager.getColor("Button.background"),
                "ordinary buttons should read as Material tonal buttons");
        assertEquals(UIManager.getColor(MD3Color.PRIMARY), UIManager.getColor("Button.default.background"),
                "the default button should read as a Material filled button");
        assertEquals(UIManager.getColor(MD3Color.INVERSE_SURFACE), UIManager.getColor("ToolTip.background"),
                "tooltips should sit on an inverse surface");

        ATLauncherLaf classic = install("com.atlauncher.themes.MonokaiPro");
        assertTrue(!classic.isMaterialColors());
        assertEquals(new Color(0x2D2A2E), UIManager.getColor("Panel.background"),
                "Monokai Pro should keep its own background");
    }

    @Test
    public void testMaterialThemesSeedFromTheBrandGreen() throws Exception {
        for (String className : new String[] { "com.atlauncher.themes.MaterialDark",
                "com.atlauncher.themes.MaterialLight" }) {
            install(className);

            assertEquals(new Color(MD3Bridge.DEFAULT_SEED, false), UIManager.getColor("md.sys.seed"),
                    className + " is not seeded from the brand green");
        }
    }
}
