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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * Paints {@link MD3Spinner}: the same container a text field and a dropdown draw, with the two
 * steppers stacked against its trailing edge.
 *
 * <p>
 * The steppers are real buttons rather than painted glyphs, unlike the dropdown's chevron - they
 * have to be, because {@link BasicSpinnerUI#installNextButtonListeners} is what gives them
 * press-and-hold repeat, and reimplementing that to save two components would be a poor trade. They
 * are stripped of everything that makes a button look like one and draw only their arrow.
 *
 * <p>
 * <b>The focus that matters is the editor's, not the spinner's.</b> A spinner is not itself
 * focusable; the field inside it is, so the container watches that instead - otherwise the outline
 * never takes the accent and the control looks inert while it is being typed into.
 */
public class MD3SpinnerUI extends BasicSpinnerUI {
    /** Same as the field and the dropdown, so a row of mixed controls sits on one line. */
    private static final int HEIGHT = MD3Spacing.MIN_TOUCH_TARGET;

    private static final int ARROW_WIDTH = 28;
    private static final int ARROW_ICON = 16;

    private PropertyChangeListener editorListener;
    private FocusListener focusListener;
    private JFormattedTextField watched;

    public static ComponentUI createUI(JComponent c) {
        return new MD3SpinnerUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        spinner.setOpaque(false);
        spinner.setBorder(new MD3SpinnerBorder());

        styleEditor();
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        // the call sites install a NumberEditor after construction, which replaces the field this
        // has just styled and the one it is watching for focus
        editorListener = e -> {
            unwatchEditor();
            styleEditor();
        };

        spinner.addPropertyChangeListener("editor", editorListener);
    }

    @Override
    protected void uninstallListeners() {
        if (editorListener != null) {
            spinner.removePropertyChangeListener("editor", editorListener);
            editorListener = null;
        }

        unwatchEditor();

        super.uninstallListeners();
    }

    @Override
    public void uninstallUI(JComponent c) {
        c.setBorder(null);

        super.uninstallUI(c);
    }

    private void unwatchEditor() {
        if (watched != null && focusListener != null) {
            watched.removeFocusListener(focusListener);
        }

        watched = null;
    }

    /**
     * @return the field the number is typed into, or null for an editor that is not one
     */
    private JFormattedTextField editorField() {
        JComponent editor = spinner.getEditor();

        return editor instanceof JSpinner.DefaultEditor ? ((JSpinner.DefaultEditor) editor).getTextField() : null;
    }

    private void styleEditor() {
        JComponent editor = spinner.getEditor();

        if (editor == null) {
            return;
        }

        editor.setOpaque(false);
        editor.setBorder(null);

        JFormattedTextField field = editorField();

        if (field == null) {
            return;
        }

        field.setOpaque(false);
        field.setBorder(null);
        field.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        field.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        field.setForeground(MD3Color.onSurface());
        field.setCaretColor(MD3Color.primary());
        field.setSelectionColor(MD3Color.secondaryContainer());
        field.setSelectedTextColor(MD3Color.onSecondaryContainer());
        field.setHorizontalAlignment(SwingConstants.LEADING);

        if (focusListener == null) {
            focusListener = new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    spinner.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    spinner.repaint();
                }
            };
        }

        field.addFocusListener(focusListener);
        watched = field;
    }

    @Override
    protected Component createNextButton() {
        JButton button = stepper(MD3Icons.CHEVRON_UP);
        button.setName("Spinner.nextButton");

        // this is what gives the stepper press-and-hold repeat, and the reason these are real
        // buttons rather than glyphs painted onto the container
        installNextButtonListeners(button);

        return button;
    }

    @Override
    protected Component createPreviousButton() {
        JButton button = stepper(MD3Icons.CHEVRON_DOWN);
        button.setName("Spinner.previousButton");

        installPreviousButtonListeners(button);

        return button;
    }

    /**
     * A button stripped of everything that makes a button look like one, drawing its arrow and a
     * state layer over the container behind it.
     */
    private JButton stepper(final MD3Icon.Painter painter) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = MD3Paint.setup(g);

                try {
                    boolean enabled = spinner.isEnabled();
                    Color content = enabled ? MD3Color.onSurfaceVariant()
                            : MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());

                    float alpha = enabled
                            ? MD3State.opacityFor(getModel().isRollover(), false,
                                    getModel().isPressed() && getModel().isArmed(), false)
                            : 0f;

                    if (alpha > 0f) {
                        MD3Paint.stateLayer(g2, MD3Shape.rounded(0, 0, getWidth(), getHeight(), MD3Shape.NONE),
                                content, alpha);
                    }

                    int size = UIScale.scale(ARROW_ICON);

                    MD3Icon.of(painter, ARROW_ICON).withColor(content).paintIcon(this, g2,
                            (getWidth() - size) / 2, (getHeight() - size) / 2);
                } finally {
                    g2.dispose();
                }
            }
        };

        button.setOpaque(false);
        button.setBorder(null);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);

        return button;
    }

    /**
     * The field takes what is left after the steppers; the steppers split the height between them.
     */
    /**
     * The field takes what is left after the steppers; the steppers split the height between them.
     *
     * <p>
     * The three are picked up from the constraints {@link BasicSpinnerUI} adds them under, which is
     * how its own layout finds them - the components carry no names to look up.
     */
    @Override
    protected LayoutManager createLayout() {
        return new LayoutManager() {
            private Component editor;
            private Component next;
            private Component previous;

            @Override
            public void addLayoutComponent(String name, Component comp) {
                if ("Next".equals(name)) {
                    next = comp;
                } else if ("Previous".equals(name)) {
                    previous = comp;
                } else if ("Editor".equals(name)) {
                    editor = comp;
                }
            }

            @Override
            public void removeLayoutComponent(Component comp) {
                if (comp == next) {
                    next = null;
                } else if (comp == previous) {
                    previous = null;
                } else if (comp == editor) {
                    editor = null;
                }
            }

            @Override
            public void layoutContainer(Container parent) {
                Insets insets = spinner.getInsets();
                int x = insets.left;
                int y = insets.top;
                int width = spinner.getWidth() - insets.left - insets.right;
                int height = spinner.getHeight() - insets.top - insets.bottom;
                int arrows = UIScale.scale(ARROW_WIDTH);

                if (editor != null) {
                    editor.setBounds(x, y, Math.max(0, width - arrows), height);
                }

                if (next != null) {
                    next.setBounds(x + width - arrows, y, arrows, height / 2);
                }

                if (previous != null) {
                    previous.setBounds(x + width - arrows, y + height / 2, arrows, height - height / 2);
                }
            }

            /**
             * Measured here rather than deferred to the spinner.
             *
             * <p>
             * {@link BasicSpinnerUI} does not answer {@code getPreferredSize}, so the question comes
             * straight back to the layout - asking the parent for its size from in here is an
             * infinite recursion, not a delegation. (A combo box gets away with it because
             * {@code BasicComboBoxUI} measures its own display size and never reaches the layout.)
             */
            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return sizeFor(parent, editor == null ? new Dimension() : editor.getPreferredSize());
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return sizeFor(parent, editor == null ? new Dimension() : editor.getMinimumSize());
            }

            private Dimension sizeFor(Container parent, Dimension content) {
                Insets insets = parent.getInsets();

                return new Dimension(
                        insets.left + insets.right + content.width + UIScale.scale(ARROW_WIDTH),
                        Math.max(insets.top + insets.bottom + content.height, UIScale.scale(HEIGHT)));
            }
        };
    }

    private Color accentColor() {
        if (!spinner.isEnabled()) {
            return MD3State.disabledContent(MD3Color.onSurface(), MD3Color.surface());
        }

        JFormattedTextField field = editorField();

        return field != null && field.isFocusOwner() ? MD3Color.primary() : MD3Color.outline();
    }

    /**
     * Draws the container before the editor and steppers put themselves on top of it.
     */
    @Override
    public void update(Graphics g, JComponent c) {
        Graphics2D g2 = MD3Paint.setup(g);

        try {
            JFormattedTextField field = editorField();
            boolean focused = field != null && field.isFocusOwner();
            float line = UIScale.scale(focused ? 2f : 1f);

            Shape container = MD3Shape.rounded(line / 2f, line / 2f, c.getWidth() - line, c.getHeight() - line,
                    MD3Shape.EXTRA_SMALL);

            g2.setColor(accentColor());
            g2.setStroke(new BasicStroke(line));
            g2.draw(container);
        } finally {
            g2.dispose();
        }

        paint(g, c);
    }

    /**
     * Padding, plus the room the steppers are laid out in.
     */
    private static class MD3SpinnerBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(UIScale.scale(MD3Spacing.S), UIScale.scale(MD3Spacing.M), UIScale.scale(MD3Spacing.S),
                    UIScale.scale(MD3Spacing.XS));

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
