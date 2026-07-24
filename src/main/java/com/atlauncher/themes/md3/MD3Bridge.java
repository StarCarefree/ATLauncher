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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.UIDefaults;

import com.atlauncher.themes.md3.hct.Hct;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Scheme;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3State;

/**
 * Translates Material 3 roles into the UI defaults keys Swing and FlatLaf actually read.
 *
 * <p>
 * Two halves, applied independently:
 *
 * <ul>
 * <li>{@link #applyShapeTokens} is colour neutral - corner radii, focus ring geometry, scrollbar
 * metrics. Every theme gets it, so the launcher picks up Material's shape language without any
 * theme losing its palette.
 * <li>{@link #applyColorTokens} rewrites the whole colour set from a generated scheme. Only the
 * Material themes opt into this today; flipping the remaining themes over is a one-line change once
 * the component migration is finished.
 * </ul>
 */
public final class MD3Bridge {
    /** ATLauncher's brand green, used when a theme offers nothing better to seed from. */
    public static final int DEFAULT_SEED = 0xFF89C236;

    /**
     * Keys examined, in order, when working out what colour a theme is "about". The first candidate
     * that holds a usable accent wins.
     *
     * <p>
     * Order matters more than it looks. The list deliberately excludes FlatLaf's own
     * {@code Component.accentColor}: every theme inherits it, so probing it first would hand all
     * sixteen themes the same generic blue. {@code TabbedPane.underlineColor} is the opposite - each
     * theme sets it to its own accent, and the ones that don't inherit ATLauncher's brand green,
     * which is the right answer for them anyway. Plain {@code accentColor} is a theme-only key
     * (FlatLaf declares its own as a variable, which never reaches UIDefaults) so it is safe to
     * keep, just lower down.
     */
    private static final String[] SEED_CANDIDATES = {
            "md.sys.seed.override",
            "TabbedPane.underlineColor",
            "Component.focusedBorderColor",
            "ProgressBar.foreground",
            "accentColor",
            "primary.500" };

    /**
     * Suffixes rewritten wholesale before the per-component overrides run, mirroring the {@code *.}
     * wildcard rules FlatLaf applies while loading properties. Without this pass, any UI class the
     * explicit list below forgets would keep the pre-Material background and show up as a hole.
     */
    private static final String[] BACKGROUND_SUFFIXES = { ".background", ".disabledBackground",
            ".inactiveBackground" };
    private static final String[] FOREGROUND_SUFFIXES = { ".foreground", ".caretForeground" };

    private MD3Bridge() {
    }

    /**
     * Works out the seed colour for a theme by looking at what it already uses as its accent.
     *
     * <p>
     * Themes are checked for a usable accent rather than carrying a hand written seed table, so a
     * new theme dropped into the resources folder gets a correct Material palette for free.
     * Candidates that are too grey or too close to black or white are skipped - a near black
     * underline colour is a decoration, not an accent, and seeding from it would produce a
     * colourless scheme.
     */
    public static int detectSeed(UIDefaults defaults, int fallback) {
        for (String key : SEED_CANDIDATES) {
            Object value = defaults.get(key);

            if (!(value instanceof Color)) {
                continue;
            }

            int argb = ((Color) value).getRGB();

            if (isUsableSeed(argb)) {
                return argb;
            }
        }

        return fallback;
    }

    private static boolean isUsableSeed(int argb) {
        Hct hct = Hct.fromInt(argb);

        return hct.getChroma() >= 8.0 && hct.getTone() >= 8.0 && hct.getTone() <= 95.0;
    }

    /**
     * Corner radii, focus ring and scrollbar metrics. Safe for every theme - nothing here changes a
     * colour or a component's preferred size, so existing layouts are unaffected.
     *
     * @param accent the colour to draw focus rings in, normally the theme's own accent
     */
    public static void applyShapeTokens(UIDefaults defaults, Color accent) {
        defaults.put("Button.arc", 999);
        defaults.put("Component.arc", MD3Shape.SMALL);
        defaults.put("TextComponent.arc", MD3Shape.EXTRA_SMALL);
        defaults.put("ProgressBar.arc", MD3Shape.EXTRA_SMALL);
        defaults.put("ScrollPane.arc", MD3Shape.EXTRA_SMALL);
        defaults.put("ScrollPane.TextComponent.arc", MD3Shape.EXTRA_SMALL);
        defaults.put("CheckBox.arc", MD3Shape.EXTRA_SMALL);
        defaults.put("PopupMenu.borderCornerRadius", MD3Shape.EXTRA_SMALL);
        defaults.put("Popup.borderCornerRadius", MD3Shape.EXTRA_SMALL);
        defaults.put("ToolTip.borderCornerRadius", MD3Shape.EXTRA_SMALL);

        // Material's focus indicator is a visible ring, not a subtle tint. The base theme had
        // switched this off entirely, which left keyboard users with nothing to follow.
        defaults.put("Component.focusWidth", 2);
        defaults.put("Component.innerFocusWidth", 0);
        defaults.put("Component.innerOutlineWidth", 0);
        defaults.put("Component.focusColor", MD3Color.withAlpha(accent, 0.45f));
        defaults.put("Button.default.focusColor", MD3Color.withAlpha(accent, 0.45f));

        defaults.put("ScrollBar.width", 12);
        defaults.put("ScrollBar.thumbArc", 999);
        defaults.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        defaults.put("ScrollBar.showButtons", false);

        defaults.put("TabbedPane.tabSelectionHeight", 3);
        defaults.put("TabbedPane.tabSelectionArc", MD3Shape.EXTRA_SMALL);

        defaults.put("Separator.height", 1);
        defaults.put("Separator.stripeWidth", 1);
    }

    /**
     * Rewrites the colour of every component from a generated Material scheme.
     *
     * <p>
     * Runs a wildcard pass first, then per-component overrides, in that order - the same order
     * FlatLaf resolves its own properties in, so a component's specific rule always beats the
     * blanket one.
     */
    public static void applyColorTokens(UIDefaults defaults, MD3Scheme scheme) {
        Color surface = scheme.get(MD3Color.SURFACE);
        Color onSurface = scheme.get(MD3Color.ON_SURFACE);
        Color onSurfaceVariant = scheme.get(MD3Color.ON_SURFACE_VARIANT);
        Color surfaceContainer = scheme.get(MD3Color.SURFACE_CONTAINER);
        Color surfaceContainerLow = scheme.get(MD3Color.SURFACE_CONTAINER_LOW);
        Color surfaceContainerLowest = scheme.get(MD3Color.SURFACE_CONTAINER_LOWEST);
        Color surfaceContainerHigh = scheme.get(MD3Color.SURFACE_CONTAINER_HIGH);
        Color surfaceContainerHighest = scheme.get(MD3Color.SURFACE_CONTAINER_HIGHEST);
        Color primary = scheme.get(MD3Color.PRIMARY);
        Color onPrimary = scheme.get(MD3Color.ON_PRIMARY);
        Color primaryContainer = scheme.get(MD3Color.PRIMARY_CONTAINER);
        Color onPrimaryContainer = scheme.get(MD3Color.ON_PRIMARY_CONTAINER);
        Color secondaryContainer = scheme.get(MD3Color.SECONDARY_CONTAINER);
        Color onSecondaryContainer = scheme.get(MD3Color.ON_SECONDARY_CONTAINER);
        Color tertiary = scheme.get(MD3Color.TERTIARY);
        Color error = scheme.get(MD3Color.ERROR);
        Color outline = scheme.get(MD3Color.OUTLINE);
        Color outlineVariant = scheme.get(MD3Color.OUTLINE_VARIANT);
        Color inverseSurface = scheme.get(MD3Color.INVERSE_SURFACE);
        Color inverseOnSurface = scheme.get(MD3Color.INVERSE_ON_SURFACE);

        Color disabledText = MD3State.disabledContent(onSurface, surface);
        Color hoverOnSurface = MD3State.applyTo(surface, onSurface, MD3State.HOVER);
        Color pressedOnSurface = MD3State.applyTo(surface, onSurface, MD3State.PRESSED);

        // Wildcard pass. FlatLaf resolves its own "*." rules while loading properties, which is
        // long finished by the time we get here, so the equivalent sweep has to be done by hand -
        // otherwise any UI class the explicit list below forgets keeps its pre-Material colour and
        // shows up as a hole. Specific keys are set afterwards and win, same as in FlatLaf.
        replaceBySuffix(defaults, BACKGROUND_SUFFIXES, surface);
        replaceBySuffix(defaults, FOREGROUND_SUFFIXES, onSurface);
        replaceBySuffix(defaults, new String[] { ".errorForeground" }, error);
        replaceBySuffix(defaults, new String[] { ".disabledForeground", ".disabledText", ".inactiveForeground" },
                disabledText);
        replaceBySuffix(defaults, new String[] { ".selectionBackground" }, secondaryContainer);
        replaceBySuffix(defaults, new String[] { ".selectionForeground" }, onSecondaryContainer);
        replaceBySuffix(defaults, new String[] { ".selectionInactiveBackground", ".selectionBackgroundInactive" },
                surfaceContainerHigh);
        replaceBySuffix(defaults, new String[] { ".selectionInactiveForeground" }, onSurface);
        replaceBySuffix(defaults, new String[] { ".borderColor", ".separatorColor" }, outline);
        replaceBySuffix(defaults, new String[] { ".infoForeground" }, onSurfaceVariant);

        defaults.put("background", surface);
        defaults.put("foreground", onSurface);
        defaults.put("control", surfaceContainer);
        defaults.put("controlText", onSurface);
        defaults.put("text", onSurface);
        defaults.put("textText", onSurface);
        defaults.put("window", surface);
        defaults.put("windowText", onSurface);

        defaults.put("Panel.background", surface);
        defaults.put("Viewport.background", surface);
        defaults.put("ScrollPane.background", surface);
        defaults.put("SplitPane.background", surface);
        defaults.put("SplitPaneDivider.draggingColor", outlineVariant);
        defaults.put("RootPane.background", surface);

        defaults.put("Label.foreground", onSurface);
        defaults.put("Label.disabledForeground", disabledText);

        defaults.put("Separator.foreground", outlineVariant);
        defaults.put("Separator.background", surface);
        defaults.put("BottomBar.dividerColor", outlineVariant);

        defaults.put("Component.borderColor", outline);
        defaults.put("Component.disabledBorderColor", MD3State.disabledContainer(outline, surface));
        defaults.put("Component.focusedBorderColor", primary);
        defaults.put("Component.accentColor", primary);
        defaults.put("Component.linkColor", primary);
        defaults.put("Component.error.borderColor", error);
        defaults.put("Component.error.focusedBorderColor", error);
        defaults.put("Component.warning.borderColor", tertiary);
        defaults.put("Component.warning.focusedBorderColor", tertiary);
        defaults.put("Component.custom.borderColor", tertiary);

        // Buttons stay tonal here. The filled/outlined/text variants arrive with MD3Button; until
        // then a secondary container reads as a Material button without needing a custom UI.
        defaults.put("Button.background", secondaryContainer);
        defaults.put("Button.foreground", onSecondaryContainer);
        defaults.put("Button.focusedBackground", MD3State.applyTo(secondaryContainer, onSecondaryContainer,
                MD3State.FOCUS));
        defaults.put("Button.hoverBackground", MD3State.applyTo(secondaryContainer, onSecondaryContainer,
                MD3State.HOVER));
        defaults.put("Button.pressedBackground", MD3State.applyTo(secondaryContainer, onSecondaryContainer,
                MD3State.PRESSED));
        defaults.put("Button.borderColor", secondaryContainer);
        defaults.put("Button.focusedBorderColor", primary);
        defaults.put("Button.hoverBorderColor", primary);
        defaults.put("Button.disabledBackground", MD3State.disabledContainer(onSurface, surface));
        defaults.put("Button.disabledText", disabledText);
        defaults.put("Button.disabledBorderColor", MD3State.disabledContainer(outline, surface));

        defaults.put("Button.default.background", primary);
        defaults.put("Button.default.foreground", onPrimary);
        defaults.put("Button.default.focusedBackground", MD3State.applyTo(primary, onPrimary, MD3State.FOCUS));
        defaults.put("Button.default.hoverBackground", MD3State.applyTo(primary, onPrimary, MD3State.HOVER));
        defaults.put("Button.default.pressedBackground", MD3State.applyTo(primary, onPrimary, MD3State.PRESSED));
        defaults.put("Button.default.borderColor", primary);
        defaults.put("Button.default.focusedBorderColor", primary);
        defaults.put("Button.default.hoverBorderColor", primary);
        defaults.put("Button.default.shadow", scheme.get(MD3Color.SHADOW));

        defaults.put("ToggleButton.background", surfaceContainerHigh);
        defaults.put("ToggleButton.foreground", onSurface);
        defaults.put("ToggleButton.selectedBackground", secondaryContainer);
        defaults.put("ToggleButton.selectedForeground", onSecondaryContainer);
        defaults.put("ToggleButton.disabledText", disabledText);

        defaults.put("CheckBox.background", surface);
        defaults.put("CheckBox.foreground", onSurface);
        defaults.put("CheckBox.disabledText", disabledText);
        defaults.put("CheckBox.icon.background", surface);
        defaults.put("CheckBox.icon.borderColor", onSurfaceVariant);
        defaults.put("CheckBox.icon.disabledBorderColor", MD3State.disabledContainer(onSurface, surface));
        defaults.put("CheckBox.icon.selectedBackground", primary);
        defaults.put("CheckBox.icon.selectedBorderColor", primary);
        defaults.put("CheckBox.icon.checkmarkColor", onPrimary);
        defaults.put("CheckBox.icon.disabledCheckmarkColor", disabledText);
        defaults.put("CheckBox.icon.focusedBorderColor", primary);
        defaults.put("CheckBox.icon.focusedSelectedBorderColor", primary);
        defaults.put("CheckBox.icon.hoverBorderColor", onSurface);

        defaults.put("RadioButton.background", surface);
        defaults.put("RadioButton.foreground", onSurface);
        defaults.put("RadioButton.disabledText", disabledText);
        defaults.put("RadioButton.icon.centerDiameter", 10);

        defaults.put("TextField.background", surfaceContainerHighest);
        defaults.put("TextField.foreground", onSurface);
        defaults.put("TextField.caretForeground", primary);
        defaults.put("TextField.placeholderForeground", onSurfaceVariant);
        defaults.put("TextField.disabledBackground", MD3State.disabledContainer(onSurface, surface));
        defaults.put("TextField.inactiveBackground", surfaceContainer);
        defaults.put("TextField.inactiveForeground", disabledText);
        defaults.put("TextField.iconColor", onSurfaceVariant);

        copyPrefix(defaults, "TextField", "PasswordField");
        copyPrefix(defaults, "TextField", "FormattedTextField");
        copyPrefix(defaults, "TextField", "Spinner");

        defaults.put("TextArea.background", surfaceContainerLowest);
        defaults.put("TextArea.foreground", onSurface);
        defaults.put("TextArea.caretForeground", primary);
        defaults.put("TextArea.disabledBackground", surface);
        defaults.put("TextArea.inactiveBackground", surface);
        defaults.put("TextArea.inactiveForeground", onSurfaceVariant);

        defaults.put("TextPane.background", surface);
        defaults.put("TextPane.foreground", onSurface);
        defaults.put("TextPane.disabledBackground", surface);
        defaults.put("TextPane.inactiveBackground", surface);

        defaults.put("EditorPane.background", surface);
        defaults.put("EditorPane.foreground", onSurface);
        defaults.put("EditorPane.disabledBackground", surface);
        defaults.put("EditorPane.inactiveBackground", surface);

        defaults.put("ComboBox.background", surfaceContainerHighest);
        defaults.put("ComboBox.foreground", onSurface);
        defaults.put("ComboBox.buttonBackground", surfaceContainerHighest);
        defaults.put("ComboBox.buttonArrowColor", onSurfaceVariant);
        defaults.put("ComboBox.buttonHoverArrowColor", onSurface);
        defaults.put("ComboBox.buttonPressedArrowColor", onSurface);
        defaults.put("ComboBox.buttonDisabledArrowColor", disabledText);
        defaults.put("ComboBox.disabledBackground", MD3State.disabledContainer(onSurface, surface));
        defaults.put("ComboBox.disabledForeground", disabledText);
        defaults.put("ComboBox.popupBackground", surfaceContainerHigh);
        defaults.put("ComboBox.selectionBackground", secondaryContainer);
        defaults.put("ComboBox.selectionForeground", onSecondaryContainer);

        defaults.put("List.background", surface);
        defaults.put("List.foreground", onSurface);
        defaults.put("List.selectionBackground", secondaryContainer);
        defaults.put("List.selectionForeground", onSecondaryContainer);
        defaults.put("List.selectionInactiveBackground", surfaceContainerHigh);
        defaults.put("List.selectionInactiveForeground", onSurface);
        defaults.put("List.hoverBackground", hoverOnSurface);

        defaults.put("Table.background", surface);
        defaults.put("Table.foreground", onSurface);
        defaults.put("Table.gridColor", outlineVariant);
        defaults.put("Table.selectionBackground", secondaryContainer);
        defaults.put("Table.selectionForeground", onSecondaryContainer);
        defaults.put("Table.selectionInactiveBackground", surfaceContainerHigh);
        defaults.put("Table.selectionInactiveForeground", onSurface);
        defaults.put("TableHeader.background", surfaceContainer);
        defaults.put("TableHeader.foreground", onSurfaceVariant);
        defaults.put("TableHeader.separatorColor", outlineVariant);
        defaults.put("TableHeader.bottomSeparatorColor", outlineVariant);

        defaults.put("Tree.background", surface);
        defaults.put("Tree.foreground", onSurface);
        defaults.put("Tree.selectionBackground", secondaryContainer);
        defaults.put("Tree.selectionForeground", onSecondaryContainer);
        defaults.put("Tree.selectionInactiveBackground", surfaceContainerHigh);
        defaults.put("Tree.selectionInactiveForeground", onSurface);
        defaults.put("Tree.hash", outlineVariant);
        defaults.put("Tree.icon.expandedColor", onSurfaceVariant);
        defaults.put("Tree.icon.collapsedColor", onSurfaceVariant);

        defaults.put("TabbedPane.background", surface);
        defaults.put("TabbedPane.foreground", onSurfaceVariant);
        defaults.put("TabbedPane.selectedForeground", onSurface);
        defaults.put("TabbedPane.underlineColor", primary);
        defaults.put("TabbedPane.inactiveUnderlineColor", MD3Color.withAlpha(primary, 0.5f));
        defaults.put("TabbedPane.disabledForeground", disabledText);
        defaults.put("TabbedPane.hoverColor", hoverOnSurface);
        defaults.put("TabbedPane.focusColor", MD3State.applyTo(surface, onSurface, MD3State.FOCUS));
        defaults.put("TabbedPane.contentAreaColor", outlineVariant);
        defaults.put("TabbedPane.tabSeparatorColor", outlineVariant);

        defaults.put("MenuBar.background", surfaceContainer);
        defaults.put("MenuBar.foreground", onSurface);
        defaults.put("MenuBar.borderColor", outlineVariant);
        defaults.put("PopupMenu.background", surfaceContainerHigh);
        defaults.put("PopupMenu.foreground", onSurface);
        defaults.put("PopupMenu.borderColor", outlineVariant);
        defaults.put("PopupMenu.borderInsets", new java.awt.Insets(4, 0, 4, 0));

        for (String menu : new String[] { "Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem" }) {
            defaults.put(menu + ".background", surfaceContainerHigh);
            defaults.put(menu + ".foreground", onSurface);
            defaults.put(menu + ".selectionBackground", secondaryContainer);
            defaults.put(menu + ".selectionForeground", onSecondaryContainer);
            defaults.put(menu + ".disabledForeground", disabledText);
            defaults.put(menu + ".acceleratorForeground", onSurfaceVariant);
            defaults.put(menu + ".acceleratorSelectionForeground", onSecondaryContainer);
        }

        defaults.put("MenuItem.underlineSelectionColor", primary);
        defaults.put("MenuItem.underlineSelectionBackground", secondaryContainer);

        defaults.put("ScrollBar.track", surface);
        defaults.put("ScrollBar.thumb", MD3Color.withAlpha(onSurfaceVariant, 0.38f));
        defaults.put("ScrollBar.hoverThumbColor", MD3Color.withAlpha(onSurfaceVariant, 0.6f));
        defaults.put("ScrollBar.pressedThumbColor", MD3Color.withAlpha(onSurfaceVariant, 0.8f));
        defaults.put("ScrollBar.hoverTrackColor", surfaceContainerLow);

        defaults.put("ProgressBar.background", surfaceContainerHighest);
        defaults.put("ProgressBar.foreground", primary);
        defaults.put("ProgressBar.selectionForeground", onPrimary);
        defaults.put("ProgressBar.selectionBackground", onSurface);

        defaults.put("Slider.background", surface);
        defaults.put("Slider.trackColor", surfaceContainerHighest);
        defaults.put("Slider.thumbColor", primary);
        defaults.put("Slider.trackValueColor", primary);
        defaults.put("Slider.tickColor", onSurfaceVariant);
        defaults.put("Slider.focusedColor", MD3Color.withAlpha(primary, 0.45f));
        defaults.put("Slider.hoverThumbColor", MD3State.applyTo(primary, onPrimary, MD3State.HOVER));
        defaults.put("Slider.pressedThumbColor", MD3State.applyTo(primary, onPrimary, MD3State.PRESSED));
        defaults.put("Slider.disabledTrackColor", MD3State.disabledContainer(onSurface, surface));
        defaults.put("Slider.disabledThumbColor", disabledText);

        // Tooltips read as an inverted surface in Material, the same treatment snackbars get.
        defaults.put("ToolTip.background", inverseSurface);
        defaults.put("ToolTip.foreground", inverseOnSurface);
        defaults.put("ToolTip.borderColor", inverseSurface);

        defaults.put("OptionPane.background", surfaceContainerHigh);
        defaults.put("OptionPane.foreground", onSurface);
        defaults.put("OptionPane.messageForeground", onSurfaceVariant);

        defaults.put("TitlePane.background", surfaceContainer);
        defaults.put("TitlePane.foreground", onSurface);
        defaults.put("TitlePane.inactiveBackground", surface);
        defaults.put("TitlePane.inactiveForeground", onSurfaceVariant);
        defaults.put("TitlePane.buttonHoverBackground", hoverOnSurface);
        defaults.put("TitlePane.buttonPressedBackground", pressedOnSurface);
        defaults.put("TitlePane.closeHoverBackground", error);
        defaults.put("TitlePane.closeHoverForeground", scheme.get(MD3Color.ON_ERROR));

        defaults.put("FileChooser.background", surface);
        defaults.put("FileChooser.foreground", onSurface);
        defaults.put("FileChooser.icon.disabledColor", disabledText);

        defaults.put("HelpButton.background", surfaceContainerHighest);
        defaults.put("HelpButton.borderColor", outline);
        defaults.put("HelpButton.questionMarkColor", onSurfaceVariant);

        applyLauncherColorTokens(defaults, scheme);
    }

    /**
     * The launcher's own UI defaults keys - the ones read by hand out of {@code UIManager} by
     * bespoke components rather than by a Swing UI delegate.
     */
    private static void applyLauncherColorTokens(UIDefaults defaults, MD3Scheme scheme) {
        Color primary = scheme.get(MD3Color.PRIMARY);
        Color surface = scheme.get(MD3Color.SURFACE);
        Color onSurface = scheme.get(MD3Color.ON_SURFACE);
        Color error = scheme.get(MD3Color.ERROR);
        Color tertiary = scheme.get(MD3Color.TERTIARY);
        Color outlineVariant = scheme.get(MD3Color.OUTLINE_VARIANT);
        Color secondaryContainer = scheme.get(MD3Color.SECONDARY_CONTAINER);

        defaults.put("CollapsiblePanel.normal", onSurface);
        defaults.put("CollapsiblePanel.warning", tertiary);
        defaults.put("CollapsiblePanel.error", error);

        defaults.put("Console.LogType.debug", scheme.get(MD3Color.TERTIARY));
        defaults.put("Console.LogType.error", error);
        defaults.put("Console.LogType.info", primary);
        defaults.put("Console.LogType.warn", tertiary);
        defaults.put("Console.LogType.default", onSurface);

        defaults.put("News.headerColor", primary);
        defaults.put("News.linkColor", primary);

        defaults.put("Mods.modSelectionColor", secondaryContainer);
        defaults.put("ModsJCheckBox.hoverBorderColor", primary);
        defaults.put("SMButton.hoverBorderColor", primary);
        defaults.put("HoverLineBorder.borderColor", primary);

        defaults.put("SocialIcon.backgroundColor", scheme.get(MD3Color.SURFACE_CONTAINER_HIGH));
        defaults.put("ToolPanel.borderColor", outlineVariant);

        defaults.put("Toaster.bgColor", scheme.get(MD3Color.INVERSE_SURFACE));
        defaults.put("Toaster.msgColor", scheme.get(MD3Color.INVERSE_ON_SURFACE));
        defaults.put("Toaster.borderColor", scheme.get(MD3Color.INVERSE_SURFACE));

        // The base theme mapped everything onto its own nine-step ramps. Keeping the ramp anchors
        // pointed at something sensible means any straggler still reading them does not fall back
        // to a stale green.
        defaults.put("primary.500", primary);
        defaults.put("secondary.500", scheme.get(MD3Color.SECONDARY));
    }

    /**
     * Applies a colour to every key ending in one of the given suffixes, provided the key currently
     * holds a colour.
     */
    private static void replaceBySuffix(UIDefaults defaults, String[] suffixes, Color color) {
        List<Object> matched = new ArrayList<>();

        for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();

            if (!(key instanceof String)) {
                continue;
            }

            String name = (String) key;

            if (name.startsWith("*") || name.startsWith(MD3Color.PREFIX)) {
                continue;
            }

            for (String suffix : suffixes) {
                if (name.endsWith(suffix) && defaults.get(key) instanceof Color) {
                    matched.add(key);
                    break;
                }
            }
        }

        for (Object key : matched) {
            defaults.put(key, color);
        }
    }

    /**
     * Copies every {@code from.*} colour onto the matching {@code to.*} key, for component families
     * that should look identical.
     */
    private static void copyPrefix(UIDefaults defaults, String from, String to) {
        List<String> names = new ArrayList<>();

        for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();

            if (key instanceof String && ((String) key).startsWith(from + ".")) {
                names.add((String) key);
            }
        }

        for (String name : names) {
            Object value = defaults.get(name);

            if (value instanceof Color) {
                defaults.put(to + name.substring(from.length()), value);
            }
        }
    }
}
