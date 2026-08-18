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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3TextField}: the container, the floating label, and the supporting text.
 *
 * <p>
 * The label's travel is a single animated fraction - 0 resting inside the field, 1 floated onto its
 * edge - which drives its position, size and colour together. Interpolating one number rather than
 * three keeps the motion coherent and makes the reduced-motion path a matter of pinning the
 * fraction rather than of special-casing every property.
 */
public class MD3TextFieldUI extends BasicTextFieldUI {
    /** Height of the field box itself, excluding any supporting text. */
    private static final int BOX_HEIGHT = MD3Spacing.TEXT_FIELD_HEIGHT;
    /** A search box has no label to make room for, so it takes the compact height instead. */
    private static final int SEARCH_HEIGHT = MD3Spacing.FIELD_HEIGHT_COMPACT;
    /** Room above an outlined box for the half of the floated label that sits outside it. */
    private static final int LABEL_OVERFLOW = 8;
    /** Height reserved for the supporting text line. */
    private static final int SUPPORTING_HEIGHT = 20;

    private JTextComponent component;
    private FocusListener focusListener;
    private DocumentListener documentListener;
    private PropertyChangeListener documentPropertyListener;
    private MouseListener clearListener;

    private float floatFraction;
    private float from;
    private float to;
    private Animator animator;

    public static ComponentUI createUI(JComponent c) {
        return new MD3TextFieldUI();
    }

    private static MD3TextField field(Component c) {
        return c instanceof MD3TextField ? (MD3TextField) c : null;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        component = getComponent();
        component.setOpaque(false);
        component.setBorder(new MD3TextFieldBorder());
        component.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        component.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        component.setForeground(MD3Color.onSurface());
        component.setCaretColor(MD3Color.primary());
        component.setSelectionColor(MD3Color.secondaryContainer());
        component.setSelectedTextColor(MD3Color.onSecondaryContainer());

        floatFraction = shouldFloat() ? 1f : 0f;
        to = floatFraction;
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                retarget();
            }

            @Override
            public void focusLost(FocusEvent e) {
                retarget();
            }
        };

        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                retarget();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                retarget();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                retarget();
            }
        };

        // JTextComponent installs its UI from its own constructor, before it has a document, and
        // swaps documents freely afterwards - so the listener follows the document rather than
        // being attached once
        documentPropertyListener = e -> {
            if (e.getOldValue() instanceof Document) {
                ((Document) e.getOldValue()).removeDocumentListener(documentListener);
            }

            if (e.getNewValue() instanceof Document) {
                ((Document) e.getNewValue()).addDocumentListener(documentListener);
            }

            retarget();
        };

        clearListener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                JTextComponent c = getComponent();
                Rectangle bounds = clearBounds(c);

                if (bounds == null || !c.isEnabled() || !bounds.contains(e.getPoint())) {
                    return;
                }

                c.setText("");

                Runnable callback = clearCallback(c);

                if (callback != null) {
                    callback.run();
                }
            }
        };

        getComponent().addFocusListener(focusListener);
        getComponent().addMouseListener(clearListener);
        getComponent().addPropertyChangeListener("document", documentPropertyListener);

        Document document = getComponent().getDocument();

        if (document != null) {
            document.addDocumentListener(documentListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (focusListener != null) {
            getComponent().removeFocusListener(focusListener);
            focusListener = null;
        }

        if (clearListener != null) {
            getComponent().removeMouseListener(clearListener);
            clearListener = null;
        }

        if (documentPropertyListener != null) {
            getComponent().removePropertyChangeListener("document", documentPropertyListener);
            documentPropertyListener = null;
        }

        if (documentListener != null) {
            Document document = getComponent().getDocument();

            if (document != null) {
                document.removeDocumentListener(documentListener);
            }

            documentListener = null;
        }

        if (animator != null) {
            animator.stop();
            animator = null;
        }

        super.uninstallListeners();
    }

    private static boolean isSearch(Component c) {
        MD3TextField f = field(c);

        return f != null && f.getVariant() == MD3TextField.Variant.SEARCH;
    }

    /**
     * Whether there is a label to float at all.
     *
     * <p>
     * A field given none is the common case in this launcher: every field in a settings row is
     * named by the row's own headline, and repeating it inside the box would say it twice. The
     * 56dp box exists to hold a label above the text and the 8dp overflow to let it sit on the
     * outline - neither is needed when there is nothing to put there.
     */
    private static boolean hasLabel(Component c) {
        MD3TextField f = field(c);

        return f != null && f.getLabel() != null && !f.getLabel().isEmpty();
    }

    private boolean hasText() {
        JTextComponent c = getComponent();

        // getText goes through the document, which does not exist yet while the component is
        // still inside its own constructor
        Document document = c == null ? null : c.getDocument();

        return document != null && document.getLength() > 0;
    }

    private boolean shouldFloat() {
        JTextComponent c = getComponent();

        if (c == null || isSearch(c)) {
            // a search box's label is a placeholder: it stays where it is and then disappears
            return false;
        }

        return c.isFocusOwner() || hasText();
    }

    private void retarget() {
        float target = shouldFloat() ? 1f : 0f;

        if (Math.abs(target - to) < 0.001f) {
            return;
        }

        if (animator != null) {
            animator.stop();
        }

        if (!Animator.useAnimation() || MD3Motion.isReduced()) {
            floatFraction = target;
            to = target;
            getComponent().repaint();

            return;
        }

        from = floatFraction;
        to = target;

        animator = MD3Motion.animator(MD3Motion.SHORT4, MD3Motion.EMPHASIZED_DECELERATE, fraction -> {
            floatFraction = from + (to - from) * fraction;
            getComponent().repaint();
        });
        animator.start();
    }

    private static int labelOverflow(Component c) {
        MD3TextField f = field(c);

        return f != null && f.getVariant() == MD3TextField.Variant.OUTLINED && hasLabel(c) ? LABEL_OVERFLOW : 0;
    }

    private static int supportingHeight(Component c) {
        MD3TextField f = field(c);

        return f != null && f.getSupportingText() != null && !f.getSupportingText().isEmpty() ? SUPPORTING_HEIGHT : 0;
    }

    private static int boxHeight(Component c) {
        return isSearch(c) || !hasLabel(c) ? SEARCH_HEIGHT : BOX_HEIGHT;
    }

    /** The box, in component coordinates. */
    private static Rectangle boxBounds(JComponent c) {
        int top = UIScale.scale(labelOverflow(c));

        return new Rectangle(0, top, c.getWidth(), UIScale.scale(boxHeight(c)));
    }

    private Color accentColor(MD3TextField f) {
        if (!f.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        if (f.isError()) {
            return MD3Color.error();
        }

        return f.isFocusOwner() ? MD3Color.primary() : MD3Color.outline();
    }

    private Color labelColor(MD3TextField f) {
        if (!f.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        if (f.isError()) {
            return MD3Color.error();
        }

        return f.isFocusOwner() ? MD3Color.primary() : MD3Color.onSurfaceVariant();
    }

    /**
     * Draws the container, label and supporting text before the text itself.
     *
     * <p>
     * Done from {@code update} rather than by overriding {@code paintBackground}, which
     * {@link javax.swing.plaf.basic.BasicTextUI} only calls for opaque components - and this one
     * cannot be opaque, since Swing would then flood the whole bounds with a single colour and bury
     * the label overflow and supporting text areas that sit outside the box.
     */
    @Override
    public void update(Graphics g, JComponent c) {
        paintDecoration(g);

        paint(g, c);
    }

    private void paintDecoration(Graphics g) {
        JComponent c = getComponent();
        MD3TextField f = field(c);

        if (f == null) {
            return;
        }

        Graphics2D g2 = MD3Paint.setup(g);

        try {
            Rectangle box = boxBounds(c);
            Color accent = accentColor(f);
            float lineWidth = f.isFocusOwner() ? 2f : 1f;

            if (f.getVariant() == MD3TextField.Variant.SEARCH) {
                paintSearchContainer(g2, f, box);
            } else if (f.getVariant() == MD3TextField.Variant.FILLED) {
                paintFilledContainer(g2, f, box, accent, lineWidth);
            } else {
                paintOutlinedContainer(g2, f, box, accent, lineWidth);
            }

            paintLeadingIcon(g2, f, box);
            paintClearIcon(g2, f);
            paintLabel(g2, f, box);
            paintSupportingText(g2, f, box);
        } finally {
            g2.dispose();
        }
    }

    /**
     * A stadium with no indicator line. The container carries the whole shape, which is what lets a
     * search box sit in a toolbar without a form field's structure around it.
     */
    private void paintSearchContainer(Graphics2D g, MD3TextField f, Rectangle box) {
        Shape container = MD3Shape.rounded(box.x, box.y, box.width, box.height, MD3Shape.FULL);

        MD3Paint.fill(g, container, f.isEnabled() ? MD3Color.surfaceContainerHigh()
                : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()));

        if (f.isEnabled() && f.isFocusOwner()) {
            MD3Paint.outline(g, container, f.isError() ? MD3Color.error() : MD3Color.primary(), 1f);
        }
    }

    private void paintFilledContainer(Graphics2D g, MD3TextField f, Rectangle box, Color accent, float lineWidth) {
        Shape container = MD3Shape.rounded(box.x, box.y, box.width, box.height, MD3Shape.EXTRA_SMALL,
                MD3Shape.EXTRA_SMALL, MD3Shape.NONE, MD3Shape.NONE);

        MD3Paint.fill(g, container, f.isEnabled() ? MD3Color.surfaceContainerHighest()
                : MD3State.disabledContainer(MD3Color.onSurface(), MD3Color.surface()));

        // the indicator sits on the bottom edge and thickens on focus - the filled variant's only
        // structural cue, which is why it is never lighter than the outlined variant's border
        float thickness = UIScale.scale(lineWidth);
        g.setColor(accent);
        g.fill(new Rectangle2D.Float(box.x, box.y + box.height - thickness, box.width, thickness));
    }

    private void paintOutlinedContainer(Graphics2D g, MD3TextField f, Rectangle box, Color accent, float lineWidth) {
        float width = UIScale.scale(lineWidth);
        Shape outline = MD3Shape.rounded(box.x + width / 2f, box.y + width / 2f, box.width - width,
                box.height - width, MD3Shape.EXTRA_SMALL);

        g.setColor(accent);
        g.setStroke(new BasicStroke(width));
        g.draw(outline);

        // punch a gap in the top edge for the floated label to sit in, so the two never overlap
        if (floatFraction > 0.01f && f.getLabel() != null && !f.getLabel().isEmpty()) {
            Rectangle notch = labelBounds(f, box);
            int padding = UIScale.scale(4);

            g.setColor(backgroundBehind(f));
            g.fillRect(notch.x - padding, box.y - 1, notch.width + padding * 2, Math.round(width) + 2);
        }
    }

    /**
     * What is behind the field, so a notch in its outline can be filled with it. Falls back to the
     * surface role, which is what a page background is under every Material theme.
     */
    private Color backgroundBehind(MD3TextField f) {
        Component parent = f.getParent();

        if (parent != null && parent.isOpaque() && parent.getBackground() != null) {
            return parent.getBackground();
        }

        return MD3Color.surface();
    }

    /**
     * Whether the field offers to clear itself.
     *
     * <p>
     * Read from the same two client properties FlatLaf's own field UI uses, since that is what the
     * call sites were already written against - three optional settings where emptying the box is
     * how you say "do not use this". A Material field puts that on a trailing icon.
     */
    private static boolean isClearable(Component c) {
        return c instanceof JComponent
                && Boolean.TRUE.equals(((JComponent) c).getClientProperty("JTextField.showClearButton"));
    }

    private static Runnable clearCallback(JComponent c) {
        Object callback = c.getClientProperty("JTextField.clearCallback");

        return callback instanceof Runnable ? (Runnable) callback : null;
    }

    /** Where the clear icon is drawn, or null for a field that has none to draw. */
    private static Rectangle clearBounds(JComponent c) {
        if (!isClearable(c)) {
            return null;
        }

        int size = UIScale.scale(MD3Spacing.ICON_SIZE);
        Rectangle box = boxBounds(c);

        return new Rectangle(c.getWidth() - UIScale.scale(MD3Spacing.M) - size,
                box.y + (box.height - size) / 2, size, size);
    }

    private void paintClearIcon(Graphics2D g, MD3TextField f) {
        Rectangle bounds = clearBounds(f);

        // nothing to clear is nothing to offer
        if (bounds == null || !f.isEnabled() || !hasText()) {
            return;
        }

        MD3Icon.of(MD3Icons.CLOSE, MD3Spacing.ICON_SIZE).withColor(MD3Color.onSurfaceVariant())
                .paintIcon(f, g, bounds.x, bounds.y);
    }

    private int textLeftEdge(MD3TextField f) {
        int left = UIScale.scale(MD3Spacing.L);

        if (f.getLeadingIcon() != null) {
            left += f.getLeadingIcon().getIconWidth() + UIScale.scale(MD3Spacing.M);
        }

        return left;
    }

    private void paintLeadingIcon(Graphics2D g, MD3TextField f, Rectangle box) {
        MD3Icon icon = f.getLeadingIcon();

        if (icon == null) {
            return;
        }

        int x = UIScale.scale(MD3Spacing.L);
        int y = box.y + (box.height - icon.getIconHeight()) / 2;

        icon.withColor(f.isEnabled() ? MD3Color.onSurfaceVariant()
                : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface())).paintIcon(f, g, x, y);
    }

    /**
     * Where the label sits at the current point in its travel.
     */
    private Rectangle labelBounds(MD3TextField f, Rectangle box) {
        Font floated = MD3Type.font(MD3Type.BODY_SMALL);
        Font resting = MD3Type.font(MD3Type.BODY_LARGE);
        Font current = floatFraction > 0.5f ? floated : resting;

        FontMetrics metrics = f.getFontMetrics(current);
        int width = metrics.stringWidth(f.getLabel() == null ? "" : f.getLabel());
        int height = metrics.getHeight();

        int restingY = box.y + (box.height - height) / 2;
        int floatedY = f.getVariant() == MD3TextField.Variant.OUTLINED ? box.y - height / 2
                : box.y + UIScale.scale(MD3Spacing.S);

        int y = Math.round(restingY + (floatedY - restingY) * floatFraction);

        return new Rectangle(textLeftEdge(f), y, width, height);
    }

    private void paintLabel(Graphics2D g, MD3TextField f, Rectangle box) {
        String label = f.getLabel();

        if (label == null || label.isEmpty()) {
            return;
        }

        // a placeholder is replaced by what the user types, rather than moving aside for it
        if (isSearch(f) && hasText()) {
            return;
        }

        Font font = MD3Type.font(floatFraction > 0.5f ? MD3Type.BODY_SMALL : MD3Type.BODY_LARGE);
        Rectangle bounds = labelBounds(f, box);
        FontMetrics metrics = f.getFontMetrics(font);

        g.setFont(font);
        g.setColor(labelColor(f));
        g.drawString(label, bounds.x, bounds.y + metrics.getAscent());
    }

    private void paintSupportingText(Graphics2D g, MD3TextField f, Rectangle box) {
        String text = f.getSupportingText();

        if (text == null || text.isEmpty()) {
            return;
        }

        Font font = MD3Type.font(MD3Type.BODY_SMALL);
        FontMetrics metrics = f.getFontMetrics(font);

        g.setFont(font);
        g.setColor(f.isError() ? MD3Color.error() : MD3Color.onSurfaceVariant());
        g.drawString(text, UIScale.scale(MD3Spacing.L),
                box.y + box.height + UIScale.scale(MD3Spacing.XS) + metrics.getAscent());
    }

    /**
     * The box's own height, or the content's where that is taller - the same rule
     * {@link MD3ComboBoxUI} follows, so a field and a dropdown in one row stay within a pixel or
     * two of each other however the UI font is set.
     */
    private static Dimension sized(JComponent c, Dimension size) {
        if (size == null) {
            return null;
        }

        size.height = Math.max(size.height,
                UIScale.scale(labelOverflow(c) + boxHeight(c) + supportingHeight(c)));

        return size;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return sized(c, super.getPreferredSize(c));
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return sized(c, super.getMinimumSize(c));
    }

    /**
     * Reserves the space the container, label and supporting text occupy, so the editor lands in
     * the middle of the box rather than over the decoration.
     */
    private static class MD3TextFieldBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            MD3TextField f = field(c);
            boolean filled = f != null && f.getVariant() == MD3TextField.Variant.FILLED;

            // nothing above the text means it is padded evenly and lands on the box's centre line -
            // true of a search box, and of any field that was given no label
            int top = isSearch(c) || !hasLabel(c) ? MD3Spacing.S
                    : (filled ? MD3Spacing.XL : labelOverflow(c) + MD3Spacing.S);
            int bottom = MD3Spacing.S + supportingHeight(c);

            // the icon's width is already in device pixels, so it is added after scaling the
            // spacing rather than before
            int left = UIScale.scale(MD3Spacing.L);

            if (f != null && f.getLeadingIcon() != null) {
                left += f.getLeadingIcon().getIconWidth() + UIScale.scale(MD3Spacing.M);
            }

            // the clear icon is painted over the trailing edge, so the text stops short of it
            int right = isClearable(c) ? MD3Spacing.M + MD3Spacing.ICON_SIZE + MD3Spacing.S : MD3Spacing.L;

            insets.set(UIScale.scale(top), left, UIScale.scale(bottom), UIScale.scale(right));

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
