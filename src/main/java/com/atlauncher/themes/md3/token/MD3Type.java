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
package com.atlauncher.themes.md3.token;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.UIScale;

/**
 * The Material 3 type scale.
 *
 * <p>
 * Sizes are the spec's values scaled to 0.875. Material 3 is designed mobile first, and at the
 * launcher's 1200x700 minimum window the unscaled scale wastes a lot of room - body large at 16sp
 * pushes an instance card's supporting text onto a second line for no benefit. The ratios between
 * roles are what carry the hierarchy, and those are preserved.
 *
 * <p>
 * Display roles are defined for completeness but are not used anywhere in the launcher; headline
 * small is the largest text a desktop window has room for.
 *
 * <p>
 * Fonts are derived from whatever the active theme reports as its normal and bold faces, so the
 * "disable custom fonts" setting and the CJK fallbacks in {@code Language} keep working untouched.
 */
public final class MD3Type {
    /**
     * Client property naming the type role a component was styled with, so font refreshes after a
     * language change can restore the right role instead of guessing from the point size.
     */
    public static final String TYPE_ROLE_KEY = "MD3.typeRole";

    public static final String PREFIX = "md.sys.typescale.";

    /** One entry in the type scale. */
    public static final class Role {
        public final String name;
        /** UIDefaults key holding the resolved {@link Font}. */
        public final String key;
        /** Unscaled point size. Pass through {@link UIScale} before using for layout. */
        public final float size;
        /** True for the spec's "medium" weight, which maps to bold in a two-weight family. */
        public final boolean emphasised;
        /** Unscaled line height, for components that lay out multiple lines. */
        public final int lineHeight;
        /** Letter spacing in ems. */
        public final float tracking;

        private Role(String name, float size, boolean emphasised, int lineHeight, float tracking) {
            this.name = name;
            this.key = PREFIX + name + ".font";
            this.size = size;
            this.emphasised = emphasised;
            this.lineHeight = lineHeight;
            this.tracking = tracking;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Role DISPLAY_LARGE = new Role("displayLarge", 50f, false, 56, 0f);
    public static final Role DISPLAY_MEDIUM = new Role("displayMedium", 39f, false, 46, 0f);
    public static final Role DISPLAY_SMALL = new Role("displaySmall", 32f, false, 39, 0f);

    public static final Role HEADLINE_LARGE = new Role("headlineLarge", 28f, false, 35, 0f);
    public static final Role HEADLINE_MEDIUM = new Role("headlineMedium", 25f, false, 32, 0f);
    public static final Role HEADLINE_SMALL = new Role("headlineSmall", 21f, false, 28, 0f);

    public static final Role TITLE_LARGE = new Role("titleLarge", 19f, false, 25, 0f);
    public static final Role TITLE_MEDIUM = new Role("titleMedium", 14f, true, 21, 0.0094f);
    public static final Role TITLE_SMALL = new Role("titleSmall", 13f, true, 18, 0.0071f);

    public static final Role BODY_LARGE = new Role("bodyLarge", 14f, false, 21, 0.031f);
    public static final Role BODY_MEDIUM = new Role("bodyMedium", 13f, false, 18, 0.018f);
    public static final Role BODY_SMALL = new Role("bodySmall", 11f, false, 14, 0.033f);

    public static final Role LABEL_LARGE = new Role("labelLarge", 13f, true, 18, 0.0071f);
    public static final Role LABEL_MEDIUM = new Role("labelMedium", 12f, true, 14, 0.042f);
    public static final Role LABEL_SMALL = new Role("labelSmall", 11f, true, 14, 0.045f);

    private static final Role[] ALL = {
            DISPLAY_LARGE, DISPLAY_MEDIUM, DISPLAY_SMALL,
            HEADLINE_LARGE, HEADLINE_MEDIUM, HEADLINE_SMALL,
            TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
            BODY_LARGE, BODY_MEDIUM, BODY_SMALL,
            LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL };

    /**
     * @return every role in the scale, in display order
     */
    public static Role[] roles() {
        return ALL.clone();
    }

    /**
     * Derived fonts, keyed by the base face they came from. Entries fall out on their own once a
     * theme or language change replaces the base font.
     */
    private static final Map<Font, Map<String, Font>> DERIVED = Collections
            .synchronizedMap(new WeakHashMap<Font, Map<String, Font>>());

    /** Tracking below this is not worth the cost of an attributed font. */
    private static final float TRACKING_THRESHOLD = 0.005f;

    private MD3Type() {
    }

    /**
     * Publishes the whole scale into the given defaults. Called while a theme's defaults are being
     * built, and again whenever the base fonts change.
     */
    public static void install(UIDefaults defaults, Font regular, Font emphasised) {
        for (Role role : ALL) {
            defaults.put(role.key, derive(role.emphasised ? emphasised : regular, role));
            defaults.put(PREFIX + role.name + ".size", role.size);
            defaults.put(PREFIX + role.name + ".lineHeight", role.lineHeight);
        }
    }

    /**
     * @return the font for a role, resolving from the active theme's defaults and falling back to
     *         deriving one from {@code defaultFont} if a theme has not published the scale
     */
    public static Font font(Role role) {
        Font font = UIManager.getFont(role.key);

        if (font != null) {
            return font;
        }

        Font base = UIManager.getFont("defaultFont");

        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }

        return derive(base, role);
    }

    /**
     * The line height for a role, scaled for the current display.
     */
    public static int lineHeight(Role role) {
        return UIScale.scale(role.lineHeight);
    }

    private static Font derive(Font base, Role role) {
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }

        Map<String, Font> perRole = DERIVED.get(base);

        if (perRole == null) {
            perRole = new HashMap<>();
            DERIVED.put(base, perRole);
        }

        Font cached = perRole.get(role.name);

        if (cached != null) {
            return cached;
        }

        Font font = base.deriveFont(role.emphasised ? Font.BOLD : Font.PLAIN, UIScale.scale(role.size));

        // FontMetrics.stringWidth does account for TextAttribute.TRACKING, so a tracked font still
        // measures itself correctly and nothing downstream needs to compensate
        if (Math.abs(role.tracking) >= TRACKING_THRESHOLD) {
            Map<TextAttribute, Object> attributes = new HashMap<>();
            attributes.put(TextAttribute.TRACKING, role.tracking);
            font = font.deriveFont(attributes);
        }

        perRole.put(role.name, font);

        return font;
    }
}
