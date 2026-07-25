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
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
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
 * button's own animation state.
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
    /** Icons sit at 18dp inside a button, smaller than the 24dp standalone size. */
    private static final int ICON_SIZE = 18;

    private MD3StateLayer stateLayer;

    public static ComponentUI createUI(JComponent c) {
        return new MD3ButtonUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

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
        b.setFont(MD3Type.font(typeRole()));
        b.putClientProperty(MD3Type.TYPE_ROLE_KEY, typeRole());

        stateLayer = MD3StateLayer.attach(b, b.getModel());
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (stateLayer != null) {
            stateLayer.uninstall();
            stateLayer = null;
        }

        c.setBorder(null);

        super.uninstallUI(c);
    }

    protected MD3Type.Role typeRole() {
        return MD3Type.LABEL_LARGE;
    }

    protected int shapeRadius() {
        return MD3Shape.BUTTON;
    }

    /**
     * The corner the button reaches at the bottom of a press. A stadium squaring off a little is the
     * whole gesture; going further makes a 40dp control look like it changed into a different one.
     */
    protected int pressedRadius() {
        return MD3Shape.MEDIUM;
    }

    protected int minimumHeight() {
        return MD3Spacing.BUTTON_HEIGHT;
    }

    /**
     * @return how far into a press the button is, 0 to 1
     */
    protected float pressProgress() {
        return stateLayer == null ? 0f : stateLayer.pressProgress();
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

        float radius = MD3Animated.lerp(MD3Shape.resolve(shapeRadius(), width, height),
                MD3Shape.resolve(pressedRadius(), width, height), pressProgress());

        return new RoundRectangle2D.Float(inset, inset, width, height, radius * 2f, radius * 2f);
    }

    static MD3Button.Variant variantOf(Component c) {
        return c instanceof MD3Button ? ((MD3Button) c).getVariant() : MD3Button.Variant.TONAL;
    }

    /**
     * The colour the button fills itself with, or null for the variants that draw no container.
     */
    protected Color containerColor(AbstractButton b) {
        if (!b.isEnabled()) {
            switch (variantOf(b)) {
                case FILLED:
                case TONAL:
                case ELEVATED:
                    return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
                default:
                    return null;
            }
        }

        switch (variantOf(b)) {
            case FILLED:
                return MD3Color.primary();
            case TONAL:
                return MD3Color.secondaryContainer();
            case ELEVATED:
                return MD3Color.surfaceContainerLow();
            case OUTLINED:
            case TEXT:
            default:
                return null;
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

        switch (variantOf(b)) {
            case FILLED:
                return MD3Color.get(MD3Color.ON_PRIMARY);
            case TONAL:
                return MD3Color.onSecondaryContainer();
            case ELEVATED:
            case OUTLINED:
            case TEXT:
            default:
                return MD3Color.primary();
        }
    }

    protected Color outlineColor(AbstractButton b) {
        if (variantOf(b) != MD3Button.Variant.OUTLINED) {
            return null;
        }

        if (!b.isEnabled()) {
            return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        // the outline takes the accent while focused, so focus is legible even for users who
        // cannot pick out the state layer
        return b.isFocusOwner() ? MD3Color.primary() : MD3Color.outline();
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

        g.setColor(MD3Color.get(MD3Color.SECONDARY));
        g.setStroke(new BasicStroke(width));
        g.draw(shapeOf(c, width / 2f));
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

    protected int iconSize() {
        return ICON_SIZE;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);

        if (size == null) {
            return null;
        }

        size.height = Math.max(size.height, UIScale.scale(minimumHeight()));

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
            boolean hasIcon = c instanceof AbstractButton && ((AbstractButton) c).getIcon() != null;
            boolean lowEmphasis = variantOf(c) == MD3Button.Variant.TEXT;

            int leading;
            int trailing;

            if (lowEmphasis) {
                leading = MD3Spacing.M;
                trailing = hasIcon ? MD3Spacing.L : MD3Spacing.M;
            } else {
                leading = hasIcon ? MD3Spacing.L : MD3Spacing.BUTTON_PADDING_H;
                trailing = MD3Spacing.BUTTON_PADDING_H;
            }

            insets.set(UIScale.scale(MD3Spacing.S), UIScale.scale(leading), UIScale.scale(MD3Spacing.S),
                    UIScale.scale(trailing));

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
