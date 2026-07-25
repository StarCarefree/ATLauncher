/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.themes;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import com.atlauncher.App;
import com.atlauncher.data.Language;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.md3.MD3Bridge;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Scheme;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Resources;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class ATLauncherLaf extends FlatLaf {
    public static ATLauncherLaf instance;

    private final String defaultFontName = "OpenSans-Regular";
    private final String defaultBoldFontName = "OpenSans-Bold";
    private final String consoleFontName = "OpenSans-Regular";
    private final String tabFontName = "Oswald-Regular";

    public static boolean install() {
        instance = new ATLauncherLaf();

        return setup(instance);
    }

    public static ATLauncherLaf getInstance() {
        return instance;
    }

    /**
     * If user has disabled custom fonts or is using a language without a font, then
     * we should use the base "sansserif" font to let the OS font take over.
     */
    private static boolean useBaseFont() {
        return App.settings.disableCustomFonts || Language.localesWithoutFont.contains(Language.selectedLocale);
    }

    /**
     * If user has disabled custom fonts or is using a language without a tab font,
     * then we should use the base "sansserif" font to let the OS font take over.
     */
    private static boolean useTabFont() {
        return App.settings.disableCustomFonts || Language.localesWithoutTabFont.contains(Language.selectedLocale);
    }

    public Font getNormalFont() {
        if (useBaseFont()) {
            return Resources.makeFont("sansserif").deriveFont(Font.PLAIN, 12f);
        } else {
            return Resources.makeFont(defaultFontName).deriveFont(Font.PLAIN, 12f);
        }
    }

    public Font getBoldFont() {
        if (useBaseFont()) {
            return Resources.makeFont("sansserif").deriveFont(Font.BOLD, 12f);
        } else {
            return Resources.makeFont(defaultFontName).deriveFont(Font.BOLD, 12f);
        }
    }

    public Font getTitleFont() {
        if (useBaseFont()) {
            return Resources.makeFont("sansserif").deriveFont(Font.BOLD, 18f);
        } else {
            return Resources.makeFont(defaultFontName).deriveFont(Font.BOLD, 18f);
        }
    }

    public Font getConsoleFont() {
        if (useBaseFont()) {
            return Resources.makeFont("sansserif").deriveFont(Font.PLAIN, 12f);
        } else {
            return Resources.makeFont(consoleFontName).deriveFont(Font.PLAIN, 12f);
        }
    }

    public Font getTabFont() {
        if (useTabFont()) {
            return Resources.makeFont("sansserif").deriveFont(Font.PLAIN, 32f);
        } else {
            return Resources.makeFont(tabFontName).deriveFont(Font.PLAIN, 32f);
        }
    }

    public void registerFonts() {
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Resources.makeFont(defaultFontName));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Resources.makeFont(defaultBoldFontName));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Resources.makeFont(consoleFontName));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Resources.makeFont(tabFontName));
        } catch (Throwable t) {
            LogManager.logStackTrace("Error registering fonts", t);
        }
    }

    @Override
    public String getName() {
        return "ATLauncher";
    }

    @Override
    public String getDescription() {
        return "Default theme of ATLauncher";
    }

    @Override
    public boolean isDark() {
        return true;
    }

    public boolean isIntelliJTheme() {
        return false;
    }

    /**
     * Whether this theme's colours come from a generated Material 3 scheme rather than from its own
     * properties.
     *
     * <p>
     * Every theme publishes the Material token set either way - see {@link #getDefaults()} - so MD3
     * components can be written against colour roles alone and still look right under a theme that
     * has never heard of Material. What this flag controls is the reverse direction: whether the
     * generated scheme is also pushed back onto the stock Swing and FlatLaf keys, which is what
     * restyles the components that have not been migrated yet.
     *
     * <p>
     * Only the Material themes opt in for now. Once the component migration is finished, returning
     * true here is what switches the remaining sixteen themes over to Material colour in one move -
     * each keeping its own hue, because each seeds from its own accent.
     */
    public boolean isMaterialColors() {
        return false;
    }

    /**
     * The colour this theme's Material scheme is generated from.
     *
     * <p>
     * Detected from the theme's own accent rather than hard coded, so a theme dropped into the
     * resources folder gets a coherent palette without anyone having to maintain a lookup table.
     * Override, or set {@code md.sys.seed.override} in the theme's properties, to pin it.
     */
    public int getSeedColor(UIDefaults defaults) {
        return MD3Bridge.detectSeed(defaults, MD3Bridge.DEFAULT_SEED);
    }

    /**
     * Layers the Material 3 design tokens on top of whatever the theme's properties produced.
     *
     * <p>
     * Three things happen here, in order:
     *
     * <ol>
     * <li>the full colour role set is generated from the theme's seed and published, so MD3
     * components have roles to read under any theme;
     * <li>the shape and focus tokens are applied to every theme - these are colour neutral and do
     * not change any component's size, so no existing layout shifts;
     * <li>for themes that opt in via {@link #isMaterialColors()}, the generated scheme is mapped
     * back onto the stock Swing keys, restyling components that have not been migrated yet.
     * </ol>
     *
     * <p>
     * Wrapped in a catch-all: a theme that renders in the wrong colours is a bad afternoon, but a
     * theme that throws while loading leaves the launcher with no look and feel at all.
     */
    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();

        try {
            MD3Scheme scheme = MD3Scheme.from(getSeedColor(defaults), isDark());
            scheme.applyTo(defaults);

            MD3Bridge.applyShapeTokens(defaults, scheme.get(MD3Color.PRIMARY));

            if (isMaterialColors()) {
                MD3Bridge.applyColorTokens(defaults, scheme);
            }
        } catch (Throwable t) {
            LogManager.logStackTrace("Error applying Material 3 design tokens", t);
        }

        return defaults;
    }

    /**
     * Publishes the type scale. Separate from {@link #getDefaults()} because the fonts depend on
     * the user's settings and locale, which are only safe to read once the look and feel is
     * installed - see {@code App.modifyLAF}.
     */
    public void installTypeScale() {
        try {
            MD3Type.install(javax.swing.UIManager.getDefaults(), getNormalFont(), getBoldFont());
        } catch (Throwable t) {
            LogManager.logStackTrace("Error installing Material 3 type scale", t);
        }
    }

    @Override
    public List<Class<?>> getLafClassesForDefaultsLoading() {
        List<Class<?>> classes = new ArrayList<>();

        classes.add(FlatLaf.class); // FlatLaf class

        // Add the themes base dark/light class
        if (isDark()) {
            classes.add(FlatDarkLaf.class);

            if (isIntelliJTheme()) {
                classes.add(FlatDarculaLaf.class);
            }
        } else {
            classes.add(FlatLightLaf.class);

            if (isIntelliJTheme()) {
                classes.add(FlatIntelliJLaf.class);
            }
        }

        classes.add(ATLauncherLaf.class); // ATLauncher base class

        if (getClass().getSuperclass() != ATLauncherLaf.class) {
            classes.add(getClass().getSuperclass()); // Dark/Light ATLauncher base class
        }

        classes.add(getClass()); // Theme's class

        return classes;
    }

    public String getIconPath(String icon) {
        // check for a theme specific icon first
        String themeSpecificPath = "/assets/icon/" + (isDark() ? "dark" : "light") + "/" + icon + ".png";
        if (App.class.getResource(themeSpecificPath) != null) {
            return themeSpecificPath;
        }

        // if no theme specific icon, then return path to where a general one should be
        return "/assets/icon/" + icon + ".png";
    }

    public String getResourcePath(String path, String icon) {
        // check for a theme specific icon first
        String themeSpecificPath = "/assets/" + path + "/" + (isDark() ? "dark" : "light") + "/" + icon + ".png";
        if (App.class.getResource(themeSpecificPath) != null) {
            return themeSpecificPath;
        }

        // if no theme specific icon, then return path to where a general one should be
        return "/assets/" + path + "/" + icon + ".png";
    }

    public void updateUIFonts() {
        // The base fonts may have changed with the language, so the defaults have to be put back
        // before the type scale is rebuilt from them. The tree walk below repairs the windows that
        // are already open; these are what everything opened afterwards is built from, and without
        // them the next dialog comes up in the outgoing language's face.
        UIManager.put("defaultFont", getNormalFont());
        UIManager.put("Button.font", getNormalFont());
        UIManager.put("ToolTip.font", getNormalFont());

        installTypeScale();

        EventQueue.invokeLater(() -> {
            for (Window w : Window.getWindows()) {
                updateFontInComponentTree(w);
            }
        });
    }

    private void updateFontInComponentTree(Component c) {
        if (c == null) {
            return;
        }

        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            JPopupMenu jpm = jc.getComponentPopupMenu();
            if (jpm != null) {
                updateFontInComponentTree(jpm);
            }
        }
        Component[] children = null;
        if (c instanceof JMenu) {
            children = ((JMenu) c).getMenuComponents();
        } else if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }
        if (children != null) {
            for (Component child : children) {
                updateFontInComponentTree(child);
            }
        }

        // a component styled from the type scale knows which role it is, so restore that rather
        // than guessing from the point size the way the fallback below has to
        if (c instanceof JComponent) {
            Object role = ((JComponent) c).getClientProperty(MD3Type.TYPE_ROLE_KEY);

            if (role instanceof MD3Type.Role) {
                c.setFont(MD3Type.font((MD3Type.Role) role));
                return;
            }
        }

        Font f = c.getFont();
        if (f != null) {
            Font newFont = App.THEME.getNormalFont();

            if (f.isBold()) {
                newFont = App.THEME.getBoldFont();
            }

            if (f.getSize() == 32f) {
                newFont = App.THEME.getTabFont();
            } else if (f.getSize() == 17f) {
                newFont = App.THEME.getNormalFont().deriveFont(17.0F);
            } else {
                newFont = newFont.deriveFont(f.getSize());
            }

            c.setFont(newFont);
        }
    }
}
