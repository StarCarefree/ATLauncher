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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3ComboBox}.
 *
 * <p>
 * The chevron is drawn rather than being a button. {@link BasicComboBoxUI} wants an arrow button and
 * lays the value out beside it, which means a component inside the control with its own background,
 * border and rollover - three things to talk out of looking like a button. It is given one that
 * occupies nothing, and the space for the glyph comes out of the border's trailing inset instead.
 *
 * <p>
 * The popup is a plain menu of the values in Material's list metrics. No animation on it: a menu
 * that has to be waited for is a menu that gets clicked through, and Material's own guidance puts
 * menu entry at the shortest duration there is.
 */
public class MD3ComboBoxUI extends BasicComboBoxUI {
    /** Same as a search field - the other label-less control that shares a line with chips. */
    private static final int HEIGHT = MD3Spacing.FIELD_HEIGHT_COMPACT;

    private static final int CHEVRON_SIZE = MD3Spacing.ICON_SIZE;

    /** Rows past this and the menu scrolls, rather than running off the screen. */
    private static final int VISIBLE_ROWS = 12;

    private MD3StateLayer stateLayer;

    public static ComponentUI createUI(JComponent c) {
        return new MD3ComboBoxUI();
    }

    private static MD3ComboBox.Variant variantOf(Component c) {
        return c instanceof MD3ComboBox ? ((MD3ComboBox<?>) c).getVariant() : MD3ComboBox.Variant.OUTLINED;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        comboBox.setOpaque(false);
        comboBox.setBorder(new MD3ComboBoxBorder());
        comboBox.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        comboBox.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        comboBox.setForeground(MD3Color.onSurface());
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        stateLayer = MD3StateLayer.install(comboBox);
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

    /**
     * A button that takes up no room. The glyph is painted; this exists because
     * {@link BasicComboBoxUI} dereferences it.
     */
    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton();

        button.setBorder(null);
        button.setFocusable(false);
        button.setVisible(false);

        return button;
    }

    /**
     * The whole inside of the border belongs to the value, since the chevron's room is already
     * reserved by the border itself.
     */
    @Override
    protected Rectangle rectangleForCurrentValue() {
        Insets insets = comboBox.getInsets();

        return new Rectangle(insets.left, insets.top, comboBox.getWidth() - insets.left - insets.right,
                comboBox.getHeight() - insets.top - insets.bottom);
    }

    @Override
    protected LayoutManager createLayoutManager() {
        return new LayoutManager() {
            @Override
            public void layoutContainer(Container parent) {
                if (arrowButton != null) {
                    arrowButton.setBounds(0, 0, 0, 0);
                }

                if (comboBox.isEditable() && editor != null) {
                    editor.setBounds(rectangleForCurrentValue());
                }
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return parent.getPreferredSize();
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return parent.getMinimumSize();
            }

            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }
        };
    }

    @Override
    protected ComboPopup createPopup() {
        return new MD3ComboPopup(comboBox);
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new OptionRenderer();
    }

    private Color accentColor() {
        if (!comboBox.isEnabled()) {
            return MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface());
        }

        if (comboBox.isFocusOwner() || comboBox.isPopupVisible()) {
            return MD3Color.primary();
        }

        if (stateLayer != null && stateLayer.isHovered()) {
            return MD3Color.onSurface();
        }

        return MD3Color.outline();
    }

    private Color contentColor() {
        if (!comboBox.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        return MD3Color.onSurface();
    }

    /**
     * Draws the container and the chevron, then lets the superclass put the value on top.
     *
     * <p>
     * From {@code update} rather than {@code paint}, so it runs even though the component is not
     * opaque - it cannot be, or Swing would flood the corners the rounded container leaves.
     */
    @Override
    public void update(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Shape container = paintContainer(g2);

            if (stateLayer != null) {
                stateLayer.paint(g2, container, MD3Color.onSurface());
            }

            paintChevron(g2);
        } finally {
            g2.dispose();
        }

        paint(g, c);
    }

    /**
     * @return the container's shape, so the state layer can be clipped to the same outline
     */
    private Shape paintContainer(Graphics2D g) {
        float width = comboBox.getWidth();
        float height = comboBox.getHeight();
        float line = UIScale.scale(comboBox.isFocusOwner() || comboBox.isPopupVisible() ? 2f : 1f);

        if (variantOf(comboBox) == MD3ComboBox.Variant.FILLED) {
            Shape container = MD3Shape.rounded(0, 0, width, height, MD3Shape.EXTRA_SMALL, MD3Shape.EXTRA_SMALL,
                    MD3Shape.NONE, MD3Shape.NONE);

            MD3Paint.fill(g, container, comboBox.isEnabled() ? MD3Color.surfaceContainerHighest()
                    : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()));

            g.setColor(accentColor());
            g.fill(new Rectangle2D.Float(0, height - line, width, line));

            return container;
        }

        Shape container = MD3Shape.rounded(line / 2f, line / 2f, width - line, height - line,
                MD3Shape.EXTRA_SMALL);

        g.setColor(accentColor());
        g.setStroke(new BasicStroke(line));
        g.draw(container);

        return container;
    }

    private void paintChevron(Graphics2D g) {
        int size = UIScale.scale(CHEVRON_SIZE);
        int x = MD3Paint.mirrorX(comboBox, comboBox.getWidth() - UIScale.scale(MD3Spacing.M) - size, size);
        int y = (comboBox.getHeight() - size) / 2;

        MD3Icon.of(MD3Icons.CHEVRON_DOWN, CHEVRON_SIZE)
                .withColor(comboBox.isEnabled() ? MD3Color.onSurfaceVariant() : contentColor())
                .paintIcon(comboBox, g, x, y);
    }

    /**
     * The renderer draws the value; the container is already painted underneath it.
     */
    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
    }

    private Dimension sized(Dimension size) {
        if (size == null) {
            return null;
        }

        size.height = Math.max(size.height, UIScale.scale(HEIGHT));

        return size;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return sized(super.getPreferredSize(c));
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return sized(super.getMinimumSize(c));
    }

    /**
     * Padding, plus the room the chevron is painted in.
     */
    private static class MD3ComboBoxBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            int trailing = MD3Spacing.M + CHEVRON_SIZE + MD3Spacing.S;

            MD3Paint.setLeadingTrailing(insets, c, UIScale.scale(MD3Spacing.S), UIScale.scale(MD3Spacing.M),
                    UIScale.scale(MD3Spacing.S), UIScale.scale(trailing));

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

    /**
     * Material's menu item metrics. The default renderer's two pixels of padding leave the values
     * too tight to pick apart at a glance.
     *
     * <p>
     * <b>The same renderer draws the menu rows and the value shown in the closed control</b>, which
     * Swing distinguishes by passing an index of -1 for the latter. The two want opposite things:
     * a row is an opaque 12dp-padded target the selection highlight fills, while the value sits
     * inside a container this UI has already painted and has already been given its padding by the
     * border. Painting the row treatment there would put an opaque rectangle over the rounded
     * container and make the control 24dp taller than the row it lives in.
     */
    private static class OptionRenderer extends JLabel implements ListCellRenderer<Object> {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focused) {
            String text = value == null ? "" : value.toString();
            boolean inMenu = index >= 0;

            setText(text);
            // the launcher did not write most of these - pack names, language names, Java paths -
            // so the theme's face may have no glyphs for them
            setFont(MD3Type.font(MD3Type.BODY_LARGE, text));
            setOpaque(inMenu);
            setBorder(inMenu ? MD3Spacing.border(MD3Spacing.M, MD3Spacing.L) : null);
            setEnabled(list == null || list.isEnabled());

            if (inMenu) {
                setBackground(selected ? MD3Color.secondaryContainer() : MD3Color.surfaceContainer());
                setForeground(selected ? MD3Color.onSecondaryContainer() : MD3Color.onSurface());
            } else {
                setForeground(list != null && !list.isEnabled()
                        ? MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface())
                        : MD3Color.onSurface());
            }

            return this;
        }
    }

    /**
     * The menu. A container surface rather than the page's, so it reads as being above what it
     * covers, and never narrower than the control it dropped out of.
     */
    private static class MD3ComboPopup extends BasicComboPopup {
        MD3ComboPopup(javax.swing.JComboBox<Object> combo) {
            super(combo);

            // deliberately no setBorder: FlatLaf's popup border is what draws the rounded corner
            // the window manager gives the popup, and replacing it with padding of our own leaves
            // a square menu under a rounded dialog. The padding comes from PopupMenu.borderInsets,
            // which the theme already sets.
            setBackground(MD3Color.surfaceContainer());

            list.setBackground(MD3Color.surfaceContainer());
            list.setForeground(MD3Color.onSurface());
            list.setSelectionBackground(MD3Color.secondaryContainer());
            list.setSelectionForeground(MD3Color.onSecondaryContainer());
        }

        @Override
        protected void configureScroller() {
            super.configureScroller();

            scroller.setBorder(null);
            scroller.getViewport().setBackground(MD3Color.surfaceContainer());
        }

        @Override
        protected Rectangle computePopupBounds(int x, int y, int width, int height) {
            int rows = Math.min(VISIBLE_ROWS, Math.max(1, comboBox.getItemCount()));

            comboBox.setMaximumRowCount(rows);

            return super.computePopupBounds(x, y, Math.max(width, comboBox.getWidth()), height);
        }
    }
}
