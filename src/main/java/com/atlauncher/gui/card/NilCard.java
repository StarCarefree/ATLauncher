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
package com.atlauncher.gui.card;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * What a grid shows when it has nothing to show: a glyph, a headline, what happened, and the one or
 * two things you can do about it.
 *
 * <p>
 * This was the last pre-Material thing on the instances, servers and pack browser pages - a
 * {@link javax.swing.border.TitledBorder} in a 15pt bold face wrapped around a disabled
 * {@link javax.swing.JSplitPane}, with ATLauncher's default cover art on one side of a divider the
 * user could not move. It read as a broken card sitting in a window of finished ones, which on an
 * empty instances page is the entire first impression of the launcher.
 *
 * <p>
 * The four things it says are unchanged, and so are the strings, so no translation is lost.
 */
public class NilCard extends MD3Card implements RelocalizationListener {
    /** Wide enough for two actions side by side without the row wrapping. */
    private static final int MIN_WIDTH = 420;

    private final JLabel headline = new JLabel();
    private final JLabel message = new JLabel();
    private final JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    public NilCard(@Nonnull String message) {
        this(message, null);
    }

    public NilCard(@Nonnull String message, @Nullable Action[] actions) {
        super(Variant.OUTLINED);

        RelocalizationManager.addListener(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(MD3Spacing.border(MD3Spacing.XXL, MD3Spacing.XL));

        IconPlate plate = new IconPlate();
        plate.setAlignmentX(CENTER_ALIGNMENT);

        headline.setText(GetText.tr("Nothing To Show"));
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());
        headline.setAlignmentX(CENTER_ALIGNMENT);
        headline.setHorizontalAlignment(SwingConstants.CENTER);

        // the callers all hand over HTML, and the paragraph breaks in it are how these messages are
        // written - so it stays HTML and only the colour and face are ours
        this.message.setText(message);
        this.message.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        this.message.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        this.message.setForeground(MD3Color.onSurfaceVariant());
        this.message.setAlignmentX(CENTER_ALIGNMENT);
        this.message.setHorizontalAlignment(SwingConstants.CENTER);

        actionRow.setOpaque(false);
        actionRow.setAlignmentX(CENTER_ALIGNMENT);

        add(plate);
        add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)));
        add(headline);
        add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        add(this.message);
        add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.XL)));
        add(actionRow);

        setActions(actions, false);
    }

    public void setActions(@Nullable Action[] actions) {
        setActions(actions, true);
    }

    /**
     * The first action is the one to take, so it is the filled button and the rest are outlined -
     * "create a pack" and "download a pack" are not equally likely on an empty instances page.
     */
    private void setActions(@Nullable Action[] actions, boolean revalidate) {
        actionRow.removeAll();

        if (actions != null) {
            for (int i = 0; i < actions.length; i++) {
                MD3Button button = i == 0 ? MD3Button.filled(actions[i].name)
                        : MD3Button.outlined(actions[i].name);

                button.addActionListener(actions[i].onClicked);
                actionRow.add(button);

                if (i < actions.length - 1) {
                    actionRow.add(Box.createHorizontalStrut(UIScale.scale(MD3Spacing.S)));
                }
            }
        }

        actionRow.setVisible(actions != null && actions.length > 0);

        if (revalidate) {
            revalidate();
            repaint();
        }
    }

    public void setMessage(String message) {
        this.message.setText(message);

        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = Math.max(size.width, UIScale.scale(MIN_WIDTH));

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public void onRelocalization() {
        headline.setText(GetText.tr("Nothing To Show"));
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
    }

    /**
     * The glyph in a tonal circle. Material's empty states lead with one symbol rather than with a
     * picture - an illustration of a landscape says nothing about there being no instances.
     */
    private static final class IconPlate extends JPanel {
        private static final int PLATE = 72;
        private static final int GLYPH = 32;

        IconPlate() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            int size = UIScale.scale(PLATE);

            return new Dimension(size, size);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                MD3Paint.fill(g2, MD3Shape.rounded(0, 0, getWidth(), getHeight(), MD3Shape.FULL),
                        MD3Color.secondaryContainer());

                int glyph = UIScale.scale(GLYPH);
                MD3Icon.of(MD3Icons.PACKAGE, GLYPH).withColor(MD3Color.onSecondaryContainer())
                        .paintIcon(this, g2, (getWidth() - glyph) / 2, (getHeight() - glyph) / 2);
            } finally {
                g2.dispose();
            }
        }
    }

    public static class Action {
        public final String name;
        public final ActionListener onClicked;

        public Action(String name, ActionListener onClicked) {
            this.name = name;
            this.onClicked = onClicked;
        }

        public static Action createCreatePackAction() {
            return new NilCard.Action(
                    GetText.tr("Create Pack"),
                    e -> App.navigate(UIConstants.LAUNCHER_CREATE_PACK_TAB));
        }

        public static Action createDownloadPackAction() {
            return new NilCard.Action(
                    GetText.tr("Download Pack"),
                    e -> App.navigate(UIConstants.LAUNCHER_PACKS_TAB));
        }

        public static Action createCreateServerAction() {
            return new NilCard.Action(
                    GetText.tr("Create Server"),
                    e -> App.navigate(UIConstants.LAUNCHER_CREATE_PACK_TAB));
        }

        public static Action createDownloadServerAction() {
            return new NilCard.Action(
                    GetText.tr("Download Server"),
                    e -> App.navigate(UIConstants.LAUNCHER_PACKS_TAB));
        }
    }
}
