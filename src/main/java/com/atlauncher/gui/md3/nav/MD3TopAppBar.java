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
package com.atlauncher.gui.md3.nav;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 small top app bar: the title of the current destination, plus its actions.
 *
 * <p>
 * Gives the launcher a consistent place for the things currently scattered across each tab - the
 * search box, the sort control, the account picker - instead of each page inventing its own header
 * row.
 *
 * <p>
 * Keep it to three actions and an overflow. The bar competes with the content for attention, and
 * anything that is not used on most visits belongs in the menu.
 */
public class MD3TopAppBar extends JPanel {
    private final JLabel titleLabel = new JLabel();
    private final JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.XS, 0));
    private final JPanel centre = new JPanel(new BorderLayout());

    private MD3IconButton navigationButton;
    private boolean scrolled;

    public MD3TopAppBar() {
        this(null);
    }

    public MD3TopAppBar(String title) {
        super(new BorderLayout());

        setOpaque(true);
        setBackground(MD3Color.surface());
        setBorder(MD3Spacing.border(0, MD3Spacing.XS));

        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setBorder(MD3Spacing.border(0, MD3Spacing.M));

        leading.setOpaque(false);
        trailing.setOpaque(false);
        centre.setOpaque(false);

        leading.add(titleLabel);

        add(leading, BorderLayout.WEST);
        add(centre, BorderLayout.CENTER);
        add(trailing, BorderLayout.EAST);

        setTitle(title);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
        titleLabel.setVisible(title != null && !title.isEmpty());

        revalidate();
        repaint();
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * The leading icon - a back arrow, or a menu button that opens the navigation drawer.
     */
    public void setNavigationAction(MD3Icon.Painter icon, String tooltip, ActionListener listener) {
        if (navigationButton != null) {
            leading.remove(navigationButton);
        }

        navigationButton = new MD3IconButton(icon, tooltip);
        navigationButton.addActionListener(listener);

        leading.add(navigationButton, 0);

        revalidate();
        repaint();
    }

    /**
     * A component filling the space between the title and the actions - most usefully a search
     * field.
     */
    public void setCentreComponent(JComponent component) {
        centre.removeAll();

        if (component != null) {
            centre.add(component, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    public void addAction(JComponent component) {
        trailing.add(component);

        revalidate();
        repaint();
    }

    public MD3IconButton addAction(MD3Icon.Painter icon, String tooltip, ActionListener listener) {
        MD3IconButton button = new MD3IconButton(icon, tooltip);
        button.addActionListener(listener);

        addAction(button);

        return button;
    }

    /**
     * Raises the bar onto a container surface once the content beneath it has scrolled, so it
     * separates from the page rather than floating over it unannounced.
     */
    public void setScrolled(boolean scrolled) {
        if (this.scrolled == scrolled) {
            return;
        }

        this.scrolled = scrolled;

        setBackground(scrolled ? MD3Elevation.surface(MD3Elevation.LEVEL2) : MD3Color.surface());
        repaint();
    }

    public boolean isScrolled() {
        return scrolled;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = UIScale.scale(MD3Spacing.TOP_APP_BAR_HEIGHT);

        return size;
    }
}
