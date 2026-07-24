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
package com.atlauncher.gui.md3.button;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;

/**
 * A Material 3 button.
 *
 * <p>
 * The five variants are a hierarchy, not a palette. Exactly one {@link Variant#FILLED} button
 * belongs on a screen - the single action the user most likely came to perform. Everything else
 * steps down from there, and a screen full of filled buttons communicates nothing.
 *
 * <pre>
 * FILLED    the primary action              Play, Install, Save
 * TONAL     a strong secondary action       Update, Export
 * OUTLINED  a secondary action with edges   Cancel on a destructive dialog
 * ELEVATED  used only over patterned or scrolling content that needs separation
 * TEXT      the lowest emphasis             Learn more, dialog dismissals
 * </pre>
 *
 * <p>
 * Deliberately still a {@link JButton}: the launcher's UI tests find buttons by text through
 * {@code JButtonMatcher}, and every existing action listener keeps working unchanged.
 */
public class MD3Button extends JButton {
    public static final String UI_CLASS_ID = "MD3ButtonUI";

    public enum Variant {
        FILLED, TONAL, OUTLINED, ELEVATED, TEXT
    }

    private Variant variant;

    public MD3Button() {
        this(null, null, Variant.TONAL);
    }

    public MD3Button(String text) {
        this(text, null, Variant.TONAL);
    }

    public MD3Button(String text, Variant variant) {
        this(text, null, variant);
    }

    public MD3Button(String text, Icon icon, Variant variant) {
        super(text, icon);

        this.variant = variant;

        // the superclass constructor already ran updateUI, before the field above existed, so the
        // UI needs telling that the variant it read as null has since become real
        updateUI();
    }

    public static MD3Button filled(String text) {
        return new MD3Button(text, Variant.FILLED);
    }

    public static MD3Button filled(String text, Icon icon) {
        return new MD3Button(text, icon, Variant.FILLED);
    }

    public static MD3Button tonal(String text) {
        return new MD3Button(text, Variant.TONAL);
    }

    public static MD3Button tonal(String text, Icon icon) {
        return new MD3Button(text, icon, Variant.TONAL);
    }

    public static MD3Button outlined(String text) {
        return new MD3Button(text, Variant.OUTLINED);
    }

    public static MD3Button outlined(String text, Icon icon) {
        return new MD3Button(text, icon, Variant.OUTLINED);
    }

    public static MD3Button elevated(String text) {
        return new MD3Button(text, Variant.ELEVATED);
    }

    public static MD3Button text(String text) {
        return new MD3Button(text, Variant.TEXT);
    }

    public static MD3Button text(String text, Icon icon) {
        return new MD3Button(text, icon, Variant.TEXT);
    }

    /**
     * @return the variant, never null - a button constructed through the no-argument path reads as
     *         tonal until told otherwise
     */
    public Variant getVariant() {
        return variant != null ? variant : Variant.TONAL;
    }

    public void setVariant(Variant variant) {
        Variant old = this.variant;
        this.variant = variant;

        firePropertyChange("variant", old, variant);
        revalidate();
        repaint();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        // self registering, so an MD3Button works even under a look and feel that has never heard
        // of it - which is what lets pages migrate one at a time
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3ButtonUI.class.getName());
        }

        setUI((ButtonUI) UIManager.getUI(this));
    }
}
