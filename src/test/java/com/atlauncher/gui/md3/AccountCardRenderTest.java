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
package com.atlauncher.gui.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mini2Dx.gettext.GetText;

import com.atlauncher.Gsons;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.card.AccountCard;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

/**
 * Paints the accounts grid.
 *
 * <p>
 * The page was a combo box of usernames beside one large skin, with every action hidden behind a
 * right-click on that image. What matters now is that an account is visible without opening
 * anything, that one needing attention says so, and that acting on a card acts on <em>that</em>
 * account - the view model still works on "the selected one", and a grid has no selection.
 */
public class AccountCardRenderTest {
    private static final int GRID_WIDTH = 820;
    private static final int GRID_HEIGHT = 360;

    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static void layoutTree(Component c) {
        if (c instanceof Container) {
            c.invalidate();
        }

        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    /**
     * Built from JSON rather than a constructor: the only one an account has takes the responses
     * from a Microsoft sign-in, and this is the same shape the launcher saves them in anyway.
     */
    private static MicrosoftAccount account(String username, boolean mustLogin) {
        return Gsons.DEFAULT.fromJson("{\"username\":\"" + username + "\",\"minecraftUsername\":\"" + username
                + "\",\"uuid\":\"e50e5b562ca3c41f35631867a7cb14c5\",\"mustLogin\":" + mustLogin + "}",
                MicrosoftAccount.class);
    }

    private static AccountCard.Actions noActions() {
        return new AccountCard.Actions() {
            @Override
            public void changeSkin() {
            }

            @Override
            public void reloadSkin() {
            }

            @Override
            public void updateUsername() {
            }

            @Override
            public void refreshAccessToken() {
            }

            @Override
            public void delete() {
            }
        };
    }

    private static int countBadges(Container root) {
        int found = 0;

        for (Component c : root.getComponents()) {
            if (c instanceof MD3Badge) {
                found++;
            } else if (c instanceof Container) {
                found += countBadges((Container) c);
            }
        }

        return found;
    }

    private JPanel buildGrid(AtomicInteger pointedAt) {
        JPanel grid = new JPanel(
                new CardGridLayout(AccountCard.CARD_WIDTH, AccountCard.MAX_CARD_WIDTH, MD3Spacing.L));
        grid.setOpaque(true);
        grid.setBackground(MD3Color.surface());
        grid.setBorder(MD3Spacing.border(MD3Spacing.L));

        grid.add(new AccountCard(account("Example", false), () -> pointedAt.set(0), noActions()));
        grid.add(new AccountCard(account("SomeoneElse", true), () -> pointedAt.set(1), noActions()));
        grid.add(new AccountCard(account("Third", false), () -> pointedAt.set(2), noActions()));

        grid.setSize(new Dimension(GRID_WIDTH, GRID_HEIGHT));
        layoutTree(grid);

        return grid;
    }

    @Test
    public void testAccountsRender() throws Exception {
        JPanel grid = buildGrid(new AtomicInteger(-1));

        BufferedImage image = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, GRID_WIDTH, GRID_HEIGHT);
        grid.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/accounts-dark.png"));
    }

    @Test
    public void testEachAccountGetsItsOwnCard() {
        JPanel grid = buildGrid(new AtomicInteger(-1));

        assertEquals(3, grid.getComponentCount(), "an account went missing from the grid");
        assertEquals("accountCard.Example", grid.getComponent(0).getName(),
                "the card is not named after its account, so nothing can find it");

        int width = grid.getComponent(0).getWidth();

        for (Component card : grid.getComponents()) {
            assertEquals(width, card.getWidth(), "a card broke the grid's rhythm");
            assertTrue(card.getHeight() > 0, "a card has no size");
        }
    }

    /**
     * An expired token cannot launch anything, and the only sign of it used to be the launch
     * failing.
     */
    @Test
    public void testAnAccountNeedingAttentionSaysSo() {
        JPanel grid = buildGrid(new AtomicInteger(-1));

        assertEquals(0, countBadges((Container) grid.getComponent(0)),
                "a healthy account is flagged as needing a login");
        assertEquals(1, countBadges((Container) grid.getComponent(1)),
                "an account that must log in again shows nothing to say so");
    }

    /**
     * The view model acts on whichever account is selected, so a card has to point it at its own
     * before doing anything - otherwise every card would act on the same account.
     */
    @Test
    public void testACardPointsTheViewModelAtItsOwnAccount() {
        AtomicInteger pointedAt = new AtomicInteger(-1);
        JPanel grid = buildGrid(pointedAt);

        clickAction((Container) grid.getComponent(1));

        assertEquals(1, pointedAt.get(), "acting on the second card did not select the second account");

        clickAction((Container) grid.getComponent(2));

        assertEquals(2, pointedAt.get(),
                "the card acted on whichever account was selected last rather than on its own");
    }

    /**
     * The card's visible action. The overflow does the same thing first, but opening its menu needs
     * a component that is actually on screen.
     */
    private static boolean clickAction(Container card) {
        for (Component c : card.getComponents()) {
            if (c instanceof MD3Button) {
                ((MD3Button) c).doClick();

                return true;
            }

            if (c instanceof Container && clickAction((Container) c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * The empty state is how a new user is told to buy Minecraft. The link used to be escaped into
     * visible source by {@code HTMLBuilder}, so the sentence ended with the tag instead of with
     * somewhere to click.
     */
    @Test
    public void testTheEmptyStateKeepsTheBuyMinecraftLink() throws Exception {
        String html = new HTMLBuilder().center().text(GetText.tr(
                "In order to login and use ATLauncher modpacks, " +
                    "you must authenticate with your existing " +
                    "Minecraft/Mojang account. You must own and have paid " +
                    "for the Minecraft Java edition " +
                    "(not the Windows 10 edition) and use the same " +
                    "login here.<br><br>If you don't have an existing " +
                    "account, you can get one " +
                    "<a href=\"https://atl.pw/create-account\">by buying " +
                    "Minecraft here</a>. ATLauncher doesn't work with cracked" +
                    " accounts."))
            .build();

        assertTrue(html.contains("href=\"https://atl.pw/create-account\""),
                "the buy-Minecraft address is not a link");
        assertFalse(html.contains("&lt;a"), "the empty state is still showing the tag instead of the link");

        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setOpaque(true);
        empty.setBackground(MD3Color.surface());
        empty.setBorder(MD3Spacing.border(MD3Spacing.XXL, MD3Spacing.L));

        JEditorPane info = MD3Html.pane(html);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        MD3Html.wrapTo(info, 560);
        empty.add(info);
        empty.setSize(new Dimension(720, 320));
        layoutTree(empty);

        BufferedImage image = new BufferedImage(720, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, 720, 320);
        empty.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/accounts-empty-dark.png"));

        boolean themedLink = false;

        for (int y = 0; y < 320 && !themedLink; y += 2) {
            for (int x = 0; x < 720 && !themedLink; x += 2) {
                themedLink = image.getRGB(x, y) == MD3Color.primary().getRGB();
            }
        }

        assertTrue(themedLink, "nothing in the empty state is the theme's link colour");
    }
}
