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

import java.awt.Dimension;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.plaf.ComboBoxUI;

import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.themes.md3.token.MD3Type;

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
     * Sizes the closed control to the longest value currently in it.
     *
     * <p>
     * {@link JComboBox}'s preferred width is the selected item's. A Forge build that reads
     * {@code 14.23.5.2860 (Recommended)} then clips as soon as any shorter version is chosen, and
     * even when it is selected the chevron's inset is not in a string width measured against the
     * wrong font. Setting the prototype to the longest value is what Swing already uses to reserve
     * that room.
     */
    public void sizeToItems() {
        Object longest = null;
        int width = 0;

        for (int i = 0; i < getItemCount(); i++) {
            E item = getItemAt(i);
            String text = item == null ? "" : item.toString();
            int next = MD3MixedText.width(MD3Type.font(MD3Type.BODY_LARGE, text), text);

            if (next >= width) {
                width = next;
                longest = item;
            }
        }

        @SuppressWarnings("unchecked")
        E prototype = (E) longest;

        setPrototypeDisplayValue(prototype);
        revalidate();
        repaint();
    }

    /**
     * Width may grow to fill a row; height may not. A combo dropped in {@code BorderLayout.CENTER}
     * would otherwise become a tall slab the row never asked for.
     */
    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return new Dimension(super.getMaximumSize());
        }

        Dimension preferred = getPreferredSize();

        return new Dimension(Integer.MAX_VALUE, preferred.height);
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
