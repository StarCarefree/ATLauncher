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

import com.atlauncher.themes.md3.token.MD3Type;

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

    /**
     * How large the button is. Medium is Material's default, and what every existing call site gets.
     *
     * <pre>
     * SMALL   32dp - a card action, a table row, anything that has to share a line
     * MEDIUM  40dp - the default, and the size a dialog action should stay
     * LARGE   48dp - an empty state, a hero action that is the page
     * </pre>
     *
     * <p>
     * The extra-large sizes Material 3 Expressive added for phones do not appear here. At the
     * launcher's 1200x700 minimum they would be a third of the window.
     */
    public enum Size {
        SMALL, MEDIUM, LARGE
    }

    /**
     * The colour role the variant is painted in.
     *
     * <p>
     * {@link Tone#ERROR} is for an action that destroys something - Delete, Remove, Kill Minecraft.
     * It is not a sixth variant: the hierarchy of filled / tonal / outlined still applies, and a
     * dialog that asks before deleting still wants a filled confirm and a text dismiss.
     */
    public enum Tone {
        DEFAULT, ERROR
    }

    /**
     * Where the button sits in a connected group. {@link Segment#SOLO} is the default, and the only
     * value a button that is not inside an {@link MD3ButtonGroup} should ever have.
     */
    public enum Segment {
        SOLO, START, MIDDLE, END
    }

    private Variant variant;
    private Size buttonSize;
    private Tone tone;
    private Segment segment;
    private Icon trailingIcon;

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
        this.buttonSize = Size.MEDIUM;
        this.tone = Tone.DEFAULT;
        this.segment = Segment.SOLO;

        // the superclass constructor already ran updateUI, before the fields above existed, so the
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

    public static MD3Button filledError(String text) {
        return filled(text).withTone(Tone.ERROR);
    }

    public static MD3Button filledError(String text, Icon icon) {
        return filled(text, icon).withTone(Tone.ERROR);
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

    /**
     * Named {@code getButtonSize} rather than {@code getSize} so it does not hide
     * {@link java.awt.Component#getSize()}, which is the pixel box the layout already asked for.
     */
    public Size getButtonSize() {
        return buttonSize != null ? buttonSize : Size.MEDIUM;
    }

    public void setButtonSize(Size buttonSize) {
        Size old = this.buttonSize;
        this.buttonSize = buttonSize;

        firePropertyChange("buttonSize", old, buttonSize);
        applyTypeRole();
        revalidate();
        repaint();
    }

    public MD3Button withButtonSize(Size buttonSize) {
        setButtonSize(buttonSize);

        return this;
    }

    public Tone getTone() {
        return tone != null ? tone : Tone.DEFAULT;
    }

    public void setTone(Tone tone) {
        Tone old = this.tone;
        this.tone = tone;

        firePropertyChange("tone", old, tone);
        repaint();
    }

    public MD3Button withTone(Tone tone) {
        setTone(tone);

        return this;
    }

    public Segment getSegment() {
        return segment != null ? segment : Segment.SOLO;
    }

    public void setSegment(Segment segment) {
        Segment old = this.segment;
        this.segment = segment;

        firePropertyChange("segment", old, segment);
        repaint();
    }

    /**
     * An icon on the trailing edge, after the label. Used for a menu chevron or a "opens something"
     * affordance; the leading icon stays the one {@link #setIcon(Icon)} holds.
     */
    public Icon getTrailingIcon() {
        return trailingIcon;
    }

    public void setTrailingIcon(Icon trailingIcon) {
        Icon old = this.trailingIcon;
        this.trailingIcon = trailingIcon;

        firePropertyChange("trailingIcon", old, trailingIcon);
        revalidate();
        repaint();
    }

    public MD3Button withTrailingIcon(Icon trailingIcon) {
        setTrailingIcon(trailingIcon);

        return this;
    }

    /**
     * Restores the type role for the current size. The UI sets this at install time; calling it
     * again is what lets {@link #setButtonSize(Size)} change the face without rebuilding the UI.
     */
    void applyTypeRole() {
        MD3Type.Role role = getButtonSize() == Size.SMALL ? MD3Type.LABEL_MEDIUM : MD3Type.LABEL_LARGE;

        setFont(MD3Type.font(role));
        putClientProperty(MD3Type.TYPE_ROLE_KEY, role);
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
