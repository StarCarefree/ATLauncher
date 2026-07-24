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
package com.atlauncher.gui.md3.icon;

import java.awt.Color;

import javax.swing.Icon;

import com.atlauncher.App;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Loads a Material Symbols glyph from {@code /assets/icon/md3/} and tints it to a colour role.
 *
 * <p>
 * The extension point for glyphs too pictorial to draw in {@link MD3Icons} - drop the SVG export in
 * and reference it by name. Rendering goes through jsvg, which arrives with flatlaf-extras, so
 * there is nothing new to add to the build.
 *
 * <p>
 * A missing file falls back to a drawn placeholder rather than to an empty space, so an icon that
 * failed to ship is obvious during development instead of invisible.
 */
public final class MD3SvgIcon {
    private static final String BASE_PATH = "/assets/icon/md3/";

    private MD3SvgIcon() {
    }

    public static Icon of(String name) {
        return of(name, MD3Spacing.ICON_SIZE, null);
    }

    public static Icon of(String name, int sizeDp) {
        return of(name, sizeDp, null);
    }

    /**
     * @param name   file name without the {@code .svg} extension
     * @param sizeDp unscaled size; the icon is rendered at the display's scale
     * @param role   an {@link MD3Color} role to tint every path to, or null to leave the artwork's
     *               own colours alone
     */
    public static Icon of(String name, int sizeDp, String role) {
        String path = BASE_PATH + name + ".svg";

        if (App.class.getResource(path) == null) {
            LogManager.warn("Missing Material icon " + path + ", falling back to a placeholder");

            return MD3Icon.of(MD3Icons.ERROR, sizeDp).withRole(MD3Color.ERROR);
        }

        FlatSVGIcon icon = new FlatSVGIcon(path.substring(1), sizeDp, sizeDp);

        if (role != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(source -> resolve(role)));
        }

        return icon;
    }

    private static Color resolve(String role) {
        return MD3Color.get(role);
    }
}
