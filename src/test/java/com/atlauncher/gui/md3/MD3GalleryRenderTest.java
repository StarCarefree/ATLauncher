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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Paints every Material component under every shipped theme.
 *
 * <p>
 * A smoke test rather than a pixel comparison: it asserts only that nothing throws and that a sheet
 * comes out the other side. That is worth more than it sounds - the components paint themselves,
 * and a colour role a theme fails to publish, or a shape resolved against a zero-size component,
 * surfaces here as an exception rather than as a hole somebody notices after release.
 *
 * <p>
 * The sheets land in {@code build/md3-preview} and are the visual baseline for reviewing a change.
 *
 * <p>
 * One caveat when reading them. A component measures its text with the font rendering context the
 * desktop implies - on Windows that means LCD subpixel advances - while a {@link
 * java.awt.image.BufferedImage} can only render greyscale antialiased glyphs, which are marginally
 * wider. A long label can therefore appear a character short in a sheet and be perfectly intact on
 * screen. Trust the sheets for layout, colour and state; check text extents in a running window.
 */
public class MD3GalleryRenderTest {
    private static final String[] THEMES = {
            "MaterialDark", "MaterialLight", "Dark", "Light", "MonokaiPro", "DraculaContrast", "HiberbeeDark",
            "Vuesion", "MaterialPalenightContrast", "ArcOrange", "CyanLight", "HighTechDarkness", "OneDark",
            "Tokyonight", "CatppuccinLatte", "CatppuccinFrappe", "CatppuccinMacchiato", "CatppuccinMocha" };

    @Test
    public void testEveryThemeRendersEveryComponent() throws Exception {
        for (String theme : THEMES) {
            File sheet = new File("build/md3-preview/gallery-" + theme + ".png");

            MD3Gallery.renderTo(sheet, "com.atlauncher.themes." + theme);

            assertTrue(sheet.isFile() && sheet.length() > 0, "no sheet produced for " + theme);
        }
    }
}
