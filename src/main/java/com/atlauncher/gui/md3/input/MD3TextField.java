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

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.TextUI;

import com.atlauncher.gui.md3.icon.MD3Icon;

/**
 * A Material 3 text field.
 *
 * <p>
 * The label starts inside the field and rises to sit on its edge once there is something to say
 * about - which is what lets a form keep its labels without doubling its height, and means a filled
 * field never loses its label the way a placeholder does.
 *
 * <pre>
 * OUTLINED  the default; works on any surface, and reads clearly in a dense form
 * FILLED    stronger presence; use where a field is the focus of its area
 * SEARCH    a toolbar's search box: a short stadium, no floating label, sized to sit beside chips
 * </pre>
 *
 * <p>
 * Still a {@link JTextField}, so existing document listeners, {@code getText} calls and validation
 * carry over unchanged.
 */
public class MD3TextField extends JTextField {
    public static final String UI_CLASS_ID = "MD3TextFieldUI";

    public enum Variant {
        OUTLINED, FILLED, SEARCH
    }

    private Variant variant;
    private String label;
    private String supportingText;
    private boolean error;
    private MD3Icon leadingIcon;

    public MD3TextField() {
        this(null, Variant.OUTLINED);
    }

    /**
     * A field of a given width in characters, with no label.
     *
     * <p>
     * The shape a settings row wants: the row's headline names the setting, so a label inside the
     * box would say it twice, and a field with nothing to float is held to 40dp rather than the
     * 56dp a floating label needs room for.
     *
     * <p>
     * <b>There is deliberately no {@code MD3TextField(String, int)}.</b> {@link JTextField}'s
     * version of that takes the initial <em>text</em>, while this class's single-String constructor
     * takes the <em>label</em> - one silent mix-up away from a field that shows its contents as a
     * placeholder. Set the text with {@link #setText(String)}, where it says what it is.
     *
     * @param columns width in characters, as {@link JTextField#setColumns(int)} means it
     */
    public MD3TextField(int columns) {
        this(null, Variant.OUTLINED);

        setColumns(columns);
    }

    /**
     * @param label the floating label. Leave it off for a field whose name is already beside it.
     */
    public MD3TextField(String label) {
        this(label, Variant.OUTLINED);
    }

    public MD3TextField(String label, Variant variant) {
        this.label = label;
        this.variant = variant;

        updateUI();
    }

    public static MD3TextField filled(String label) {
        return new MD3TextField(label, Variant.FILLED);
    }

    /**
     * A toolbar search box, already carrying its magnifier.
     *
     * <p>
     * Shorter than a form field and fully rounded, so it sits on the same line as a row of chips
     * instead of setting the height of the whole toolbar. Its label behaves as a placeholder - it
     * never floats, it just gets out of the way once there is something typed.
     *
     * @param placeholder what the field searches, such as "Search packs"
     */
    public static MD3TextField search(String placeholder) {
        return new MD3TextField(placeholder, Variant.SEARCH);
    }

    public Variant getVariant() {
        return variant != null ? variant : Variant.OUTLINED;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;

        revalidate();
        repaint();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;

        revalidate();
        repaint();
    }

    /**
     * A line of guidance below the field - a format hint, a constraint, or, when
     * {@link #setError(boolean) in error}, what went wrong.
     */
    public String getSupportingText() {
        return supportingText;
    }

    public void setSupportingText(String supportingText) {
        this.supportingText = supportingText;

        revalidate();
        repaint();
    }

    public boolean isError() {
        return error;
    }

    /**
     * Marks the field as invalid. Always pair with {@link #setSupportingText(String)} saying why -
     * a red outline on its own tells the user they are wrong without telling them how to be right.
     */
    public void setError(boolean error) {
        this.error = error;

        repaint();
    }

    public MD3Icon getLeadingIcon() {
        return leadingIcon;
    }

    public void setLeadingIcon(MD3Icon.Painter painter) {
        setLeadingIcon(MD3Icon.of(painter));
    }

    public void setLeadingIcon(MD3Icon icon) {
        this.leadingIcon = icon;

        revalidate();
        repaint();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        if (UIManager.get(UI_CLASS_ID) == null) {
            UIManager.put(UI_CLASS_ID, MD3TextFieldUI.class.getName());
        }

        setUI((TextUI) UIManager.getUI(this));
    }
}
