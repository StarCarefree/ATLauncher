/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.gui.tabs.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * One tool, as a card: what it is called, what it does, and the button that does it.
 *
 * <p>
 * These were titled-border panels in a fixed three by two grid, each holding a centred paragraph
 * hard-wrapped at seventy characters. The wrapping was fixed while the panels were not, so the text
 * broke wherever the character count said rather than wherever the panel ended.
 *
 * <p>
 * Subclasses pass their description as plain text and it is measured against the width the card
 * actually gets. They keep {@link #LAUNCH_BUTTON} - which is still a {@link javax.swing.JButton}, so
 * their listeners and their enabled-state bindings are untouched.
 */
public abstract class AbstractToolPanel extends MD3Card implements CardGridLayout.WidthAware {
    public static final int CARD_WIDTH = 300;
    public static final int MAX_CARD_WIDTH = 420;

    /** Enough for the longest of these, which runs to about forty words. */
    private static final int DESCRIPTION_LINES = 5;

    protected final JPanel MIDDLE_PANEL = new JPanel();
    protected final JPanel BOTTOM_PANEL = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    protected final MD3Button LAUNCH_BUTTON = MD3Button.outlined(GetText.tr("Launch"));

    private final JLabel description = new JLabel();
    private final String descriptionText;

    /** Scaled; what the description was last wrapped against. */
    private int descriptionWidth = -1;

    /** Scaled; -1 until the grid has said how wide this card is. */
    private int layoutWidth = -1;

    protected AbstractToolPanel(String title, String descriptionText) {
        super(Variant.FILLED, new BorderLayout());

        setHoverElevation(true);

        this.descriptionText = descriptionText;

        setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, MD3Spacing.M, MD3Spacing.L));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, title));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(titleLabel);

        description.setFont(MD3Type.font(MD3Type.BODY_SMALL, descriptionText));
        description.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        description.setForeground(MD3Color.onSurfaceVariant());
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setVerticalAlignment(SwingConstants.TOP);
        description.setToolTipText(descriptionText);
        wrapDescription(UIScale.scale(CARD_WIDTH));

        body.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        body.add(description);

        MIDDLE_PANEL.setOpaque(false);
        MIDDLE_PANEL.setAlignmentX(LEFT_ALIGNMENT);

        BOTTOM_PANEL.setOpaque(false);
        BOTTOM_PANEL.setAlignmentX(LEFT_ALIGNMENT);
        BOTTOM_PANEL.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));
        BOTTOM_PANEL.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                UIScale.scale(MD3Spacing.BUTTON_HEIGHT + MD3Spacing.M)));

        body.add(MIDDLE_PANEL);
        body.add(Box.createVerticalGlue());
        body.add(BOTTOM_PANEL);

        add(body, BorderLayout.CENTER);
    }

    private void wrapDescription(int width) {
        descriptionWidth = width;

        FontMetrics metrics = description.getFontMetrics(description.getFont());
        description.setText(MD3Text.wrapToLines(metrics, descriptionText, width, DESCRIPTION_LINES));
    }

    @Override
    public void setLayoutWidth(int width) {
        if (width <= 0 || width == layoutWidth) {
            return;
        }

        layoutWidth = width;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return size;
    }

    @Override
    public void doLayout() {
        int available = getWidth() - getInsets().left - getInsets().right;

        if (available > 0 && available != descriptionWidth) {
            wrapDescription(available);
            revalidate();
        }

        super.doLayout();
    }
}
