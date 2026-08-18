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
package com.atlauncher.gui.card;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.MD3MenuItem;
import com.atlauncher.gui.md3.MD3PopupMenu;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3ButtonBar;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * One Minecraft account.
 *
 * <p>
 * The page used to be a combo box of usernames beside a single large skin, with every action on an
 * account - change the skin, refresh its token, delete it - hidden behind a right-click on that
 * image. Nothing said the menu was there, and nothing showed you which accounts you had without
 * opening the box.
 *
 * <p>
 * A card each says both: the accounts are visible at a glance, and their actions are where actions
 * live on every other card in the launcher.
 */
public final class AccountCard extends MD3Card implements CardGridLayout.WidthAware {
    public static final int CARD_WIDTH = 240;
    public static final int MAX_CARD_WIDTH = 300;

    /** Room for the 128x256 skin the launcher renders, at a size that keeps its pixels crisp. */
    private static final int SKIN_HEIGHT = 200;

    private final Skin skin;
    private MD3FittingLabel username;

    /** Scaled; -1 until the grid has said how wide this card is. */
    private int layoutWidth = -1;

    /**
     * @param account  the account this card stands for
     * @param onAction run before any action, so the view model is pointed at this account - it
     *                 works on "the selected one", and a grid has no selection of its own
     */
    public AccountCard(MicrosoftAccount account, Runnable onAction, Actions actions) {
        super(Variant.FILLED, new BorderLayout());

        setHoverElevation(true);
        setBorder(null);
        setName("accountCard." + account.minecraftUsername);

        this.skin = new Skin(account.getMinecraftSkin());

        add(buildSkin(), BorderLayout.NORTH);
        add(buildBody(account, onAction, actions), BorderLayout.CENTER);
    }

    /**
     * What can be done to an account. The tab supplies these because each one needs a dialog, a
     * progress window or a confirmation that does not belong on a card.
     */
    public interface Actions {
        void changeSkin();

        void reloadSkin();

        void updateUsername();

        void refreshAccessToken();

        void delete();
    }

    private JComponent buildSkin() {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = MD3Paint.setup(g);

                try {
                    // rounding a box twice this tall leaves only the top two corners curved
                    g2.clip(MD3Shape.rounded(0, 0, getWidth(), getHeight() * 2f, MD3Shape.CARD));
                    super.paintChildren(g2);
                } finally {
                    g2.dispose();
                }
            }
        };

        wrapper.setOpaque(true);
        wrapper.setBackground(MD3Color.surfaceContainerHigh());
        wrapper.setPreferredSize(new Dimension(UIScale.scale(CARD_WIDTH), UIScale.scale(SKIN_HEIGHT)));
        wrapper.add(skin, BorderLayout.CENTER);

        return wrapper;
    }

    private JComponent buildBody(MicrosoftAccount account, Runnable onAction, Actions actions) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.M, MD3Spacing.S));

        username = new MD3FittingLabel(account.minecraftUsername, 2);
        username.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, account.minecraftUsername));
        username.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        username.setForeground(MD3Color.onSurface());
        username.setOverflowTip(account.minecraftUsername);
        body.add(username);

        // an account whose token has expired cannot launch anything, and until now the only sign of
        // that was the launch failing
        if (account.mustLogin) {
            MD3Badge badge = MD3Badge.problem(GetText.tr("Login required"));
            badge.setAlignmentX(LEFT_ALIGNMENT);

            body.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
            body.add(badge);
        }

        body.add(buildActions(onAction, actions));

        return body;
    }

    private JComponent buildActions(Runnable onAction, Actions actions) {
        MD3Button primary = MD3Button.outlined(GetText.tr("Change Skin"));
        primary.addActionListener(e -> {
            onAction.run();
            actions.changeSkin();
        });

        MD3IconButton overflow = new MD3IconButton(MD3Icons.MORE_VERT, GetText.tr("More options"),
                MD3IconButton.Variant.STANDARD, MD3IconButton.Size.SMALL);
        overflow.addActionListener(e -> {
            onAction.run();

            JPopupMenu menu = new MD3PopupMenu();
            menu.add(item(GetText.tr("Reload Skin"), actions::reloadSkin));
            menu.add(item(GetText.tr("Update Username"), actions::updateUsername));
            menu.add(item(GetText.tr("Refresh Access Token"), actions::refreshAccessToken));
            menu.addSeparator();
            menu.add(item(GetText.tr("Delete"), actions::delete));

            menu.show(overflow, 0, overflow.getHeight());
        });

        MD3ButtonBar row = new MD3ButtonBar();
        row.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));
        row.leading(primary);
        row.trailing(overflow);

        return row;
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem menuItem = new MD3MenuItem(text);
        menuItem.addActionListener(e -> action.run());

        return menuItem;
    }

    @Override
    public void setLayoutWidth(int width) {
        if (width <= 0 || width == layoutWidth) {
            return;
        }

        layoutWidth = width;

        if (username != null) {
            username.fitTo(width - UIScale.scale(MD3Spacing.L) - UIScale.scale(MD3Spacing.S));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return size;
    }

    /**
     * The skin, drawn to fit whatever height the card has.
     *
     * <p>
     * Nearest neighbour rather than smooth: a Minecraft skin is 64 pixels of deliberate pixel art,
     * and interpolating it turns the face into a smudge.
     */
    private static final class Skin extends JLabel {
        Skin(Icon icon) {
            setIcon(icon);
            setHorizontalAlignment(CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Icon icon = getIcon();

            if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                super.paintComponent(g);

                return;
            }

            double scale = Math.min(getWidth() / (double) icon.getIconWidth(),
                    getHeight() / (double) icon.getIconHeight());

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.translate((getWidth() - icon.getIconWidth() * scale) / 2,
                        (getHeight() - icon.getIconHeight() * scale) / 2);
                g2.scale(scale, scale);
                icon.paintIcon(this, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }
    }
}
