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
package com.atlauncher.gui.md3.container;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Focus;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.gui.md3.paint.MD3StateLayer;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 list item: an optional leading slot, one to three lines of text, and an optional
 * trailing slot.
 *
 * <p>
 * This is the workhorse of the settings pages. Each setting becomes a headline, a line of
 * supporting text explaining it, and a trailing control - which is both more readable and far less
 * code than the {@link java.awt.GridBagLayout} forms it replaces, where every row is six lines of
 * constraint fiddling.
 *
 * <p>
 * Text is laid out by hand rather than with nested panels because the vertical rhythm has to be
 * exact: Material specifies the gap between headline and supporting text, and letting a layout
 * manager derive it from font metrics gives a different answer per locale.
 */
public class MD3ListItem extends JPanel {
    private final JLabel leadingIcon = new JLabel();
    private final JLabel headline = new JLabel();
    private final JLabel supporting = new JLabel();
    private final JLabel overline = new JLabel();

    private Component leading;
    private Component trailing;

    private boolean clickable;
    private MD3StateLayer stateLayer;
    private MouseListener activationListener;
    private final List<ActionListener> actionListeners = new ArrayList<>();

    public MD3ListItem() {
        super(null);

        setLayout(new ListItemLayout());
        setOpaque(false);
        setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        overline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_SMALL);
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);

        for (JLabel label : new JLabel[] { overline, headline, supporting }) {
            label.setVisible(false);
            add(label);
        }

        applyTypography();
    }

    public static MD3ListItem of(String headline) {
        MD3ListItem item = new MD3ListItem();
        item.setHeadline(headline);

        return item;
    }

    public static MD3ListItem of(String headline, String supporting) {
        MD3ListItem item = of(headline);
        item.setSupportingText(supporting);

        return item;
    }

    private void applyTypography() {
        overline.setFont(MD3Type.font(MD3Type.LABEL_SMALL));
        headline.setFont(MD3Type.font(MD3Type.BODY_LARGE));
        supporting.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));

        overline.setForeground(MD3Color.onSurfaceVariant());
        headline.setForeground(MD3Color.onSurface());
        supporting.setForeground(MD3Color.onSurfaceVariant());
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // fonts and colours are resolved from the theme, so they have to be re-resolved when it
        // changes; the labels are final fields, which may not exist yet on the first call
        if (headline != null) {
            applyTypography();
        }
    }

    public void setHeadline(String text) {
        setLabelText(headline, text);
    }

    public void setSupportingText(String text) {
        setLabelText(supporting, text);
    }

    private void setLabelText(JLabel label, String text) {
        label.setText(text);
        label.setVisible(text != null && !text.isEmpty());

        revalidate();
        repaint();
    }

    /**
     * A short label above the headline - a category, a status. Use sparingly; it competes with the
     * headline for the eye.
     */
    public void setOverline(String text) {
        setLabelText(overline, text);
    }

    public void setLeadingIcon(MD3Icon.Painter painter) {
        setLeadingIcon(MD3Icon.of(painter, MD3Spacing.ICON_SIZE_LARGE).withRole(MD3Color.ON_SURFACE_VARIANT));
    }

    public void setLeadingIcon(Icon icon) {
        leadingIcon.setIcon(icon);
        setLeading(leadingIcon);
    }

    /**
     * An arbitrary component in the leading slot - an avatar, a thumbnail, a checkbox.
     */
    public void setLeading(Component component) {
        if (leading != null) {
            remove(leading);
        }

        leading = component;

        if (component != null) {
            add(component);
        }

        revalidate();
        repaint();
    }

    /**
     * An arbitrary component in the trailing slot - a switch, a dropdown, an icon button.
     */
    public void setTrailing(Component component) {
        if (trailing != null) {
            remove(trailing);
        }

        trailing = component;

        if (component != null) {
            add(component);
        }

        revalidate();
        repaint();
    }

    public void setClickable(boolean clickable) {
        if (this.clickable == clickable) {
            return;
        }

        this.clickable = clickable;

        if (clickable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            stateLayer = MD3StateLayer.install(this);

            activationListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        requestFocusInWindow();
                        fireActionPerformed();
                    }
                }
            };

            addMouseListener(activationListener);

            AbstractAction activate = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireActionPerformed();
                }
            };

            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "md3.activate");
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "md3.activate");
            getActionMap().put("md3.activate", activate);
        } else {
            setCursor(Cursor.getDefaultCursor());
            setFocusable(false);

            if (activationListener != null) {
                removeMouseListener(activationListener);
                activationListener = null;
            }

            getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            getActionMap().remove("md3.activate");

            if (stateLayer != null) {
                stateLayer.uninstall();
                stateLayer = null;
            }
        }
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click");

        for (ActionListener listener : new ArrayList<>(actionListeners)) {
            listener.actionPerformed(event);
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPanel() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return clickable ? AccessibleRole.PUSH_BUTTON : super.getAccessibleRole();
                }
            };
        }

        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (stateLayer != null) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                Shape shape = MD3Paint.shapeOf(this, MD3Shape.NONE);
                stateLayer.paint(g2, shape, MD3Color.onSurface());

                if (clickable && MD3Focus.isVisible(this)) {
                    MD3Paint.focusRingInside(g2, shape, null);
                }
            } finally {
                g2.dispose();
            }
        }

        super.paintComponent(g);
    }

    /**
     * Lays out the three slots: leading at its preferred width, trailing at its preferred width,
     * and the text column taking whatever is left.
     */
    private final class ListItemLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets insets = getInsets();
            int gap = UIScale.scale(MD3Spacing.L);

            int textWidth = 0;
            int textHeight = 0;

            for (JLabel label : visibleLabels()) {
                textWidth = Math.max(textWidth, label.getPreferredSize().width);
                textHeight += label.getPreferredSize().height;
            }

            int width = insets.left + insets.right + textWidth;
            int height = Math.max(textHeight, 0);

            if (leading != null) {
                width += leading.getPreferredSize().width + gap;
                height = Math.max(height, leading.getPreferredSize().height);
            }

            if (trailing != null) {
                width += trailing.getPreferredSize().width + gap;
                height = Math.max(height, trailing.getPreferredSize().height);
            }

            height += insets.top + insets.bottom;

            return new Dimension(width, Math.max(height, UIScale.scale(minimumHeight())));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = getInsets();
            int gap = UIScale.scale(MD3Spacing.L);

            boolean ltr = MD3Paint.isLeftToRight(parent);
            int left = insets.left;
            int right = getWidth() - insets.right;
            int centreY = (insets.top + getHeight() - insets.bottom) / 2;

            if (leading != null) {
                Dimension size = leading.getPreferredSize();
                int leadX = ltr ? left : right - size.width;
                leading.setBounds(leadX, centreY - size.height / 2, size.width, size.height);
                if (ltr) {
                    left += size.width + gap;
                } else {
                    right -= size.width + gap;
                }
            }

            if (trailing != null) {
                Dimension size = trailing.getPreferredSize();
                int trailX = ltr ? right - size.width : left;
                trailing.setBounds(trailX, centreY - size.height / 2, size.width, size.height);
                if (ltr) {
                    right -= size.width + gap;
                } else {
                    left += size.width + gap;
                }
            }

            int x = left;

            List<JLabel> labels = visibleLabels();
            int textHeight = 0;

            for (JLabel label : labels) {
                textHeight += label.getPreferredSize().height;
            }

            int y = centreY - textHeight / 2;
            int width = Math.max(0, right - x);

            for (JLabel label : labels) {
                int height = label.getPreferredSize().height;
                label.setBounds(x, y, width, height);
                y += height;
            }
        }

        private int minimumHeight() {
            int lines = visibleLabels().size();

            if (lines >= 3) {
                return MD3Spacing.LIST_ITEM_HEIGHT_THREE_LINE;
            }

            return lines == 2 ? MD3Spacing.LIST_ITEM_HEIGHT_TWO_LINE : MD3Spacing.LIST_ITEM_HEIGHT_ONE_LINE;
        }
    }

    private List<JLabel> visibleLabels() {
        List<JLabel> labels = new ArrayList<>(3);

        addIfPresent(labels, overline);
        addIfPresent(labels, headline);
        addIfPresent(labels, supporting);

        return labels;
    }

    private static void addIfPresent(List<JLabel> labels, JLabel label) {
        String text = label.getText();

        if (text != null && !text.isEmpty()) {
            labels.add(label);
        }
    }

    @Override
    public Component add(Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setOpaque(false);
        }

        return super.add(comp);
    }
}
