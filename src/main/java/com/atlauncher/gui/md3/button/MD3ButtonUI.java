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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3Button}.
 *
 * <p>
 * Never shared between components - each instance owns a {@link MD3StateLayer} holding that
 * button's own animation state, and a {@link MD3Animated} for how far into being selected it is.
 *
 * <p>
 * Elevated buttons carry no drop shadow. Swing clips a component's painting to its own bounds, so a
 * shadow would be sliced off at the edge it is supposed to fall past; and Material 3 expresses
 * height through surface colour first anyway, which works here and costs nothing.
 *
 * <p>
 * Pressing a button rounds its corners in and lets them back out - Material 3's shape morph, and the
 * one piece of press feedback that survives having no ripple. It is the shape that moves rather than
 * the size, so nothing around the button shifts and the whole thing costs one interpolated number.
 */
public class MD3ButtonUI extends BasicButtonUI {
    private MD3StateLayer stateLayer;

    /**
     * How far into being selected the button is, so a toggle can fade its container in rather than
     * swapping colours on a click.
     */
    protected MD3Animated selection;
    private ChangeListener selectionListener;

    /** The button this UI is installed on, for the no-argument metrics ChipUI still overrides. */
    protected JComponent host;

    public static ComponentUI createUI(JComponent c) {
        return new MD3ButtonUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        host = c;

        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorder(new MD3ButtonBorder());
        b.setIconTextGap(UIScale.scale(MD3Spacing.S));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MD3Type.Role role = typeRole(b);
        b.setFont(MD3Type.font(role));
        b.putClientProperty(MD3Type.TYPE_ROLE_KEY, role);

        stateLayer = MD3StateLayer.attach(b, b.getModel());
        selection = new MD3Animated(c, isSelected(b) ? 1f : 0f, MD3Motion.SHORT4, MD3Motion.STANDARD);
        selectionListener = e -> selection.setTarget(isSelected(b) ? 1f : 0f);
        b.getModel().addChangeListener(selectionListener);
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (selectionListener != null) {
            ((AbstractButton) c).getModel().removeChangeListener(selectionListener);
            selectionListener = null;
        }

        if (selection != null) {
            selection.stop();
            selection = null;
        }

        if (stateLayer != null) {
            stateLayer.uninstall();
            stateLayer = null;
        }

        c.setBorder(null);
        host = null;

        super.uninstallUI(c);
    }

    protected MD3Type.Role typeRole() {
        return MD3Type.LABEL_LARGE;
    }

    protected MD3Type.Role typeRole(AbstractButton b) {
        if (buttonSizeOf(b) == MD3Button.Size.SMALL) {
            return MD3Type.LABEL_MEDIUM;
        }

        return typeRole();
    }

    protected int shapeRadius() {
        return MD3Shape.BUTTON;
    }

    /**
     * The corner the button reaches at the bottom of a press. A stadium squaring off a little is the
     * whole gesture; going further makes a 40dp control look like it changed into a different one.
     */
    protected int pressedRadius() {
        switch (buttonSizeOf(host)) {
            case SMALL:
                return MD3Shape.EXTRA_SMALL;
            case LARGE:
                return MD3Shape.LARGE;
            case MEDIUM:
            default:
                return MD3Shape.MEDIUM;
        }
    }

    protected int minimumHeight() {
        switch (buttonSizeOf(host)) {
            case SMALL:
                return MD3Spacing.BUTTON_HEIGHT_SMALL;
            case LARGE:
                return MD3Spacing.BUTTON_HEIGHT_LARGE;
            case MEDIUM:
            default:
                return MD3Spacing.BUTTON_HEIGHT;
        }
    }

    /**
     * @return how far into a press the button is, 0 to 1
     */
    protected float pressProgress() {
        return stateLayer == null ? 0f : stateLayer.pressProgress();
    }

    /**
     * @return how far into being selected the button is, falling back to the model for a paint that
     *         arrives before the UI has finished installing
     */
    protected float selectedProgress(AbstractButton b) {
        return selection != null ? selection.value() : (isSelected(b) ? 1f : 0f);
    }

    protected static boolean isSelected(AbstractButton b) {
        return b.getModel().isSelected();
    }

    /**
     * The button's outline at the corner it has reached, which is somewhere between its resting
     * shape and {@link #pressedRadius()} while a press is going in or coming back out.
     *
     * @param inset how far inside its own bounds to draw, for the focus ring that would otherwise be
     *              clipped in half
     */
    protected Shape shapeOf(JComponent c, float inset) {
        float width = c.getWidth() - inset * 2f;
        float height = c.getHeight() - inset * 2f;
        float rest = MD3Shape.resolve(shapeRadius(), width, height);
        float pressed = MD3Shape.resolve(pressedRadius(), width, height);
        float outer = MD3Animated.lerp(rest, pressed, pressProgress());
        MD3Button.Segment segment = segmentOf(c);

        if (segment == MD3Button.Segment.SOLO) {
            return new RoundRectangle2D.Float(inset, inset, width, height, outer * 2f, outer * 2f);
        }

        float innerRest = MD3Shape.resolve(MD3Shape.BUTTON_GROUP_INNER, width, height);
        float innerPress = MD3Shape.resolve(MD3Shape.NONE, width, height);
        float inner = MD3Animated.lerp(innerRest, innerPress, pressProgress());

        float leading = segment == MD3Button.Segment.START ? outer : inner;
        float trailing = segment == MD3Button.Segment.END ? outer : inner;

        return MD3Shape.roundedRadii(inset, inset, width, height, leading, trailing, trailing, leading);
    }

    static MD3Button.Variant variantOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getVariant() : MD3Button.Variant.TONAL;
    }

    static MD3Button.Size buttonSizeOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getButtonSize() : MD3Button.Size.MEDIUM;
    }

    static MD3Button.Tone toneOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getTone() : MD3Button.Tone.DEFAULT;
    }

    static MD3Button.Segment segmentOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getSegment() : MD3Button.Segment.SOLO;
    }

    static Icon trailingOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getTrailingIcon() : null;
    }

    static boolean isError(Component c) {
        return toneOf(c) == MD3Button.Tone.ERROR;
    }

    /**
     * The colour the button fills itself with, or null for the variants that draw no container.
     */
    protected Color containerColor(AbstractButton b) {
        float selected = selectedProgress(b);
        boolean error = isError(b);

        if (!b.isEnabled()) {
            switch (variantOf(b)) {
                case FILLED:
                case TONAL:
                case ELEVATED:
                    return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
                case OUTLINED:
                case TEXT:
                default:
                    return selected > 0f
                            ? MD3Color.withAlpha(
                                    MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()), selected)
                            : null;
            }
        }

        switch (variantOf(b)) {
            case FILLED:
                return error ? MD3Color.error() : MD3Color.primary();
            case TONAL:
                if (error) {
                    return MD3Color.errorContainer();
                }

                return MD3Animated.lerp(MD3Color.secondaryContainer(), MD3Color.primaryContainer(), selected);
            case ELEVATED:
                if (error) {
                    return MD3Color.errorContainer();
                }

                return MD3Animated.lerp(MD3Color.surfaceContainerLow(), MD3Color.surfaceContainerHigh(), selected);
            case OUTLINED:
            case TEXT:
            default:
                if (selected <= 0f) {
                    return null;
                }

                Color fill = error ? MD3Color.errorContainer() : MD3Color.secondaryContainer();

                return MD3Color.withAlpha(fill, selected);
        }
    }

    /**
     * The colour of the button's text and icon. Also the colour its state layer is drawn in, which
     * is what makes hover feel like the same gesture across every variant.
     */
    protected Color contentColor(AbstractButton b) {
        if (!b.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        float selected = selectedProgress(b);
        boolean error = isError(b);

        switch (variantOf(b)) {
            case FILLED:
                return error ? MD3Color.onError() : MD3Color.get(MD3Color.ON_PRIMARY);
            case TONAL:
                if (error) {
                    return MD3Color.onErrorContainer();
                }

                return MD3Animated.lerp(MD3Color.onSecondaryContainer(), MD3Color.onPrimaryContainer(), selected);
            case ELEVATED:
            case OUTLINED:
            case TEXT:
            default:
                if (error) {
                    return MD3Animated.lerp(MD3Color.error(), MD3Color.onErrorContainer(), selected);
                }

                return MD3Animated.lerp(MD3Color.primary(), MD3Color.onSecondaryContainer(), selected);
        }
    }

    protected Color outlineColor(AbstractButton b) {
        if (variantOf(b) != MD3Button.Variant.OUTLINED) {
            return null;
        }

        float remaining = 1f - selectedProgress(b);

        if (remaining <= 0f) {
            return null;
        }

        if (!b.isEnabled()) {
            return MD3Color.withAlpha(MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()),
                    remaining);
        }

        Color line = isError(b) ? MD3Color.error() : (b.isFocusOwner() ? MD3Color.primary() : MD3Color.outline());

        return MD3Color.withAlpha(line, remaining);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Shape shape = shapeOf(c, 0f);

            MD3Paint.fill(g2, shape, containerColor(b));
            MD3Paint.outline(g2, shape, outlineColor(b), 1f);

            if (stateLayer != null) {
                stateLayer.paint(g2, shape, contentColor(b));
            }

            if (b.isEnabled() && b.isFocusOwner()) {
                paintFocusIndicator(g2, c);
            }
        } finally {
            g2.dispose();
        }

        paint(g, c);
    }

    /**
     * Material puts the focus ring outside the component; Swing clips painting to the component's
     * bounds, so it is drawn just inside the edge instead. Same job, and it cannot be sliced off.
     */
    protected void paintFocusIndicator(Graphics2D g, JComponent c) {
        float width = UIScale.scale(2f);

        g.setColor(isError(c) ? MD3Color.error() : MD3Color.get(MD3Color.SECONDARY));
        g.setStroke(new BasicStroke(width));
        g.draw(shapeOf(c, width / 2f));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        paintTrailingIcon(g, c);
        paintSplitDivider(g, c);
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        FontMetrics metrics = b.getFontMetrics(b.getFont());

        g.setColor(contentColor(b));
        FlatUIUtils.drawStringUnderlineCharAt(b, g, text, b.getDisplayedMnemonicIndex(), textRect.x,
                textRect.y + metrics.getAscent());
    }

    @Override
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        AbstractButton b = (AbstractButton) c;
        Icon icon = b.getIcon();

        if (icon == null) {
            return;
        }

        // a Material icon follows its host's foreground by default, but a button's content colour
        // is not its foreground - it depends on the variant - so it is passed explicitly
        if (icon instanceof MD3Icon) {
            icon = ((MD3Icon) icon).withSize(iconSize()).withColor(contentColor(b));
        }

        icon.paintIcon(c, g, iconRect.x, iconRect.y);
    }

    protected void paintTrailingIcon(Graphics g, JComponent c) {
        Icon icon = trailingOf(c);

        if (icon == null) {
            return;
        }

        AbstractButton b = (AbstractButton) c;

        if (icon instanceof MD3Icon) {
            icon = ((MD3Icon) icon).withSize(iconSize()).withColor(contentColor(b));
        }

        int size = icon.getIconWidth();
        int pad = UIScale.scale(trailingIconPad(c));

        icon.paintIcon(c, g, c.getWidth() - pad - size, (c.getHeight() - icon.getIconHeight()) / 2);
    }

    /**
     * The hairline that splits a menu button into a primary action and a chevron. Only drawn when
     * the button has said it is split - a menu-only button has no primary half to mark off.
     */
    protected void paintSplitDivider(Graphics g, JComponent c) {
        if (!(c instanceof MD3MenuButton) || !((MD3MenuButton) c).isSplit() || !c.isEnabled()) {
            return;
        }

        AbstractButton b = (AbstractButton) c;
        int x = c.getWidth() - UIScale.scale(trailingZone(c));
        int inset = UIScale.scale(MD3Spacing.S);
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            g2.setColor(MD3Color.withAlpha(contentColor(b), 0.24f));
            g2.drawLine(x, inset, x, c.getHeight() - inset);
        } finally {
            g2.dispose();
        }
    }

    protected int iconSize() {
        switch (buttonSizeOf(host)) {
            case SMALL:
                return MD3Spacing.BUTTON_ICON_SIZE_SMALL;
            case LARGE:
                return MD3Spacing.BUTTON_ICON_SIZE_LARGE;
            case MEDIUM:
            default:
                return MD3Spacing.BUTTON_ICON_SIZE;
        }
    }

    static int trailingIconPad(Component c) {
        return buttonSizeOf(c) == MD3Button.Size.SMALL ? MD3Spacing.M : MD3Spacing.L;
    }

    static int trailingZone(Component c) {
        return trailingIconPad(c) + iconSizeFor(c) + MD3Spacing.XS;
    }

    static int iconSizeFor(Component c) {
        switch (buttonSizeOf(c)) {
            case SMALL:
                return MD3Spacing.BUTTON_ICON_SIZE_SMALL;
            case LARGE:
                return MD3Spacing.BUTTON_ICON_SIZE_LARGE;
            case MEDIUM:
            default:
                return MD3Spacing.BUTTON_ICON_SIZE;
        }
    }

    static int horizontalPadding(Component c) {
        return buttonSizeOf(c) == MD3Button.Size.SMALL ? MD3Spacing.BUTTON_PADDING_H_SMALL
                : MD3Spacing.BUTTON_PADDING_H;
    }

    static int verticalPadding(Component c) {
        return buttonSizeOf(c) == MD3Button.Size.SMALL ? MD3Spacing.XS : MD3Spacing.S;
    }

    static int leadingIconPadding(Component c) {
        return buttonSizeOf(c) == MD3Button.Size.SMALL ? MD3Spacing.M : MD3Spacing.L;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);

        if (size == null) {
            return null;
        }

        size.height = Math.max(size.height, UIScale.scale(minimumHeight()));

        if (c instanceof MD3Button) {
            size.width = Math.max(size.width, UIScale.scale(MD3Spacing.BUTTON_MIN_WIDTH));
        }

        return size;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /**
     * Supplies the padding, recomputed on demand rather than fixed at install time - a button that
     * gains an icon needs less padding on its leading edge, and that can happen at any point.
     */
    private static class MD3ButtonBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            if (c instanceof MD3IconButton) {
                // the 40dp (or 32, or 48) is the whole target; padding here would shove the glyph
                // off centre and, on a 40dp box with 16/24 insets, leave no room for it at all
                insets.set(0, 0, 0, 0);

                return insets;
            }

            boolean hasIcon = c instanceof AbstractButton && ((AbstractButton) c).getIcon() != null;
            boolean hasTrailing = trailingOf(c) != null;
            boolean lowEmphasis = variantOf(c) == MD3Button.Variant.TEXT;

            int padH = horizontalPadding(c);
            int padV = verticalPadding(c);
            int iconPad = leadingIconPadding(c);

            int leading;
            int trailing;

            if (lowEmphasis) {
                leading = MD3Spacing.M;
                trailing = hasIcon || hasTrailing ? MD3Spacing.L : MD3Spacing.M;
            } else {
                leading = hasIcon ? iconPad : padH;
                trailing = padH;
            }

            if (hasTrailing) {
                trailing = trailingZone(c);
            }

            insets.set(UIScale.scale(padV), UIScale.scale(leading), UIScale.scale(padV), UIScale.scale(trailing));

            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0, 0, 0, 0));
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
