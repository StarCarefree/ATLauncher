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
package com.atlauncher.gui.md3.input;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.plaf.ComboBoxUI;

/**
 * A Material 3 dropdown - the control for picking one value out of a list too long, or too dull, to
 * be a row of chips.
 *
 * <p>
 * Extends {@link JComboBox} deliberately, exactly as {@link MD3Switch} extends
 * {@code JCheckBox}: the launcher has some forty of these wired to view models through
 * {@code addItemListener}, {@code addItem}, {@code setSelectedIndex} and their own renderers, and
 * this drops into those call sites with nothing but the constructor changing.
 *
 * <p>
 * <b>Held to 40dp rather than the 56dp of Material's exposed dropdown menu.</b> That control is a
 * text field with a floating label, and it belongs on a form where it has to name itself. Every
 * dropdown in this launcher sits on the trailing edge of a settings row whose name is already on
 * the leading edge, so the label would be said twice and the row would be half as tall again for
 * saying it. Same height as {@link MD3TextField#search}, which is the other label-less control the
 * launcher puts on one line.
 *
 * <p>
 * Distinct from {@link MD3FilterChip}, which is also a menu behind a control: a chip says "the view
 * is narrowed to this" and is only meaningful next to the thing it narrows. A dropdown says "this
 * is the value", which is what a setting has.
 */
public class MD3ComboBox<E> extends JComboBox<E> {
    public static final String UI_CLASS_ID = "MD3ComboBoxUI";

    public enum Variant {
        /** A 1dp outline that takes the accent on focus. The default, and the quieter of the two. */
        OUTLINED,

        /** A tonal container under an indicator line, matching {@link MD3TextField.Variant#FILLED}. */
        FILLED
    }

    private Variant variant;

    public MD3ComboBox() {
        this(Variant.OUTLINED);
    }

    public MD3ComboBox(Variant variant) {
        this.variant = variant;

        // the superclass constructor already ran updateUI, before the field above existed
        updateUI();
        useHeavyweightPopup();
    }

    public MD3ComboBox(E[] items) {
        this(items, Variant.OUTLINED);
    }

    public MD3ComboBox(E[] items, Variant variant) {
        super(items);

        this.variant = variant;

        updateUI();
        useHeavyweightPopup();
    }

    public MD3ComboBox(ComboBoxModel<E> model) {
        super(model);

        this.variant = Variant.OUTLINED;

        updateUI();
        useHeavyweightPopup();
    }

    public Variant getVariant() {
        return variant != null ? variant : Variant.OUTLINED;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;

        repaint();
    }

    /**
     * The menu is a real window, so the window manager can round it.
     *
     * <p>
     * <b>A combo box keeps its own flag for this</b>, separate from
     * {@link javax.swing.JPopupMenu#setDefaultLightWeightPopupEnabled} - which is what the launcher
     * sets for every other menu - and it defaults to true. Left alone, the dropdown is a panel
     * inside the frame whenever it fits there, and a panel has no window corner to round.
     */
    private void useHeavyweightPopup() {
        setLightWeightPopupEnabled(false);
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3ComboBoxUI.class.getName());
        }

        setUI((ComboBoxUI) UIManager.getUI(this));
    }
}
