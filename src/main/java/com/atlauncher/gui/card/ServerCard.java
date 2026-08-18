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

import static com.atlauncher.gui.md3.MD3Menus.addAction;
import static com.atlauncher.gui.md3.MD3Menus.addSubmenu;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Server;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.components.DropDownButton;
import com.atlauncher.gui.components.ImagePanel;
import com.atlauncher.gui.dialogs.AddModsDialog;
import com.atlauncher.gui.dialogs.EditModsDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.MD3FittingLabel;
import com.atlauncher.gui.md3.MD3PopupMenu;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3ButtonBar;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.managers.ServerManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;
import com.formdev.flatlaf.util.UIScale;

/**
 * One server, as a Material 3 card.
 *
 * <p>
 * The last of the launcher's collapsible titled panels: a split pane with the image on one side and
 * nine buttons stacked under a description on the other, all of it as loud as everything else. It
 * now matches {@link InstanceCard}, which is the point - a server and an instance are the same kind
 * of thing to the person looking at them, and they were drawn nothing alike.
 *
 * <p>
 * As there, nothing was dropped. The original buttons still exist and still hold every action's
 * wiring, including the rules about what a Paper or Purpur server can do; they are simply no longer
 * laid out, and the overflow menu is built from them each time it opens.
 */
public class ServerCard extends MD3Card implements RelocalizationListener, CardGridLayout.WidthAware {
    public static final int CARD_WIDTH = 280;
    public static final int MAX_CARD_WIDTH = 400;

    private static final int MAX_BADGES = 3;

    /** 16:9 against the card width. */
    private static final int COVER_HEIGHT = 158;

    private final Server server;
    private final ImagePanel image;

    private MD3FittingLabel titleLabel;
    private MD3FittingLabel subtitleLabel;
    private MD3Button launchAction;
    private MD3IconButton overflowAction;
    private JPanel coverWrapper;

    /** Scaled; -1 until the grid has said how wide this card is. */
    private int layoutWidth = -1;

    private final JButton launchButton = new JButton(GetText.tr("Launch"));
    private final JButton launchAndCloseButton = new JButton(GetText.tr("Launch & Close"));
    private final JButton launchWithGui = new JButton(GetText.tr("Launch With GUI"));
    private final JButton launchWithGuiAndClose = new JButton(GetText.tr("Launch With GUI & Close"));
    private final JButton addButton = new JButton(GetText.tr("Add Mods"));
    private final JButton editButton = new JButton(GetText.tr("Edit Mods"));
    private final JButton backupButton = new JButton(GetText.tr("Backup"));
    private final JButton deleteButton = new JButton(GetText.tr("Delete"));
    private final JButton openButton = new JButton(GetText.tr("Open Folder"));

    private final JPopupMenu getHelpPopupMenu = new JPopupMenu();
    private final JMenuItem discordLinkMenuItem = new JMenuItem(GetText.tr("Discord"));
    private final JMenuItem supportLinkMenuItem = new JMenuItem(GetText.tr("Support"));
    private final JMenuItem websiteLinkMenuItem = new JMenuItem(GetText.tr("Website"));
    private final JMenuItem wikiLinkMenuItem = new JMenuItem(GetText.tr("Wiki"));
    private final JMenuItem sourceLinkMenuItem = new JMenuItem(GetText.tr("Source"));
    private final DropDownButton getHelpButton = new DropDownButton(GetText.tr("Get Help"), getHelpPopupMenu);

    public ServerCard(Server server) {
        super(Variant.FILLED, new BorderLayout());

        setHoverElevation(true);

        this.server = server;
        this.image = new ImagePanel(() -> server.getImage().getImage());

        // the cover runs to the card's edges, so the padding belongs to the body instead
        setBorder(null);
        setName("serverCard." + server.name);

        applyAvailabilityRules();
        setupButtonPopupMenus();

        add(buildCover(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        addActionListeners();
        addMouseListeners();

        RelocalizationManager.addListener(this);
    }

    /**
     * Hides the actions that do not apply to this server. They stay constructed - the overflow menu
     * reads their visibility rather than repeating the conditions.
     */
    private void applyAvailabilityRules() {
        getHelpButton.setVisible(server.showGetHelpButton());

        // Paper/Purpur only supports plugins and not mods, so update the button text to be clear
        boolean mods = server.loaderVersion != null;

        addButton.setVisible(mods);
        editButton.setVisible(mods);

        if (mods && (server.loaderVersion.isPaper() || server.loaderVersion.isPurpur())) {
            addButton.setText(GetText.tr("Add Plugins"));
            editButton.setText(GetText.tr("Edit Plugins"));
        }

        // unfortunately OSX doesn't allow us to pass arguments with open and Terminal
        if (OS.isMac()) {
            launchButton.setVisible(false);
            launchAndCloseButton.setVisible(false);
        }
    }

    /**
     * The cover art, clipped to the card's top corners.
     */
    private JComponent buildCover() {
        JPanel cover = new JPanel(new BorderLayout()) {
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

        cover.setOpaque(false);
        cover.add(image, BorderLayout.CENTER);
        cover.setPreferredSize(new Dimension(UIScale.scale(CARD_WIDTH), UIScale.scale(COVER_HEIGHT)));

        coverWrapper = cover;

        return cover;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.M, MD3Spacing.S));

        titleLabel = new MD3FittingLabel(server.name, 2);
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, server.name));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setOverflowTip(server.name);

        subtitleLabel = new MD3FittingLabel(subtitleText(), 1);
        subtitleLabel.setFont(MD3Type.font(MD3Type.BODY_SMALL, subtitleText()));
        subtitleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        subtitleLabel.setForeground(MD3Color.onSurfaceVariant());

        JPanel badges = new JPanel(new WrapLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.XS), 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);

        for (MD3Badge badge : buildBadges()) {
            badges.add(badge);
        }

        launchAction = MD3Button.filled(GetText.tr("Launch"), MD3Icon.of(MD3Icons.PLAY));
        launchAction.addActionListener(e -> launch());

        overflowAction = new MD3IconButton(MD3Icons.MORE_VERT, GetText.tr("More options"),
                MD3IconButton.Variant.STANDARD, MD3IconButton.Size.SMALL);
        overflowAction.addActionListener(e -> {
            JPopupMenu menu = buildOverflowMenu();
            menu.show(overflowAction, 0, overflowAction.getHeight());
        });

        MD3ButtonBar actions = new MD3ButtonBar();
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));
        actions.leading(launchAction);
        actions.trailing(overflowAction);

        body.add(titleLabel);
        body.add(subtitleLabel);
        body.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        body.add(badges);

        // the grid gives every card in a row the same height, so a server with no loader badge has
        // space left over. Without somewhere to put it the box layout hands it to the actions and
        // the launch button comes out taller than its neighbours'
        body.add(Box.createVerticalGlue());
        body.add(actions);

        return body;
    }

    /**
     * What the server is, under its name. The pack it came from, and the version of it.
     */
    private String subtitleText() {
        String pack = server.pack == null ? "" : server.pack;
        String version = server.version;

        if (pack.isEmpty()) {
            return version == null ? "" : version;
        }

        return version == null || version.isEmpty() ? pack : pack + " · " + version;
    }

    private List<MD3Badge> buildBadges() {
        List<MD3Badge> badges = new ArrayList<>();

        if (server.loaderVersion != null && badges.size() < MAX_BADGES) {
            // the loader's name is what identifies a server; its patch version is detail, and the
            // full string is in the tooltip anyway
            badges.add(MD3Badge.neutral(server.loaderVersion.type));
        }

        return badges;
    }

    /**
     * Launching without a GUI is what a server is normally started as, and is what a double click on
     * the cover has always done. macOS cannot pass the argument, so there it launches with the GUI.
     */
    private void launch() {
        if (OS.isMac()) {
            server.launch(false);
        } else {
            server.launch("nogui", false);
        }
    }

    private JPopupMenu buildOverflowMenu() {
        JPopupMenu menu = new MD3PopupMenu();

        addAction(menu, launchAndCloseButton);
        addAction(menu, launchWithGui);
        addAction(menu, launchWithGuiAndClose);
        menu.addSeparator();

        addAction(menu, addButton);
        addAction(menu, editButton);
        menu.addSeparator();

        addAction(menu, backupButton);
        addAction(menu, openButton);
        addSubmenu(menu, getHelpButton, getHelpPopupMenu);
        menu.addSeparator();

        addAction(menu, deleteButton);

        return menu;
    }

    private void addActionListeners() {
        this.launchButton.addActionListener(e -> server.launch("nogui", false));
        this.launchAndCloseButton.addActionListener(e -> server.launch("nogui", true));
        this.launchWithGui.addActionListener(e -> server.launch(false));
        this.launchWithGuiAndClose.addActionListener(e -> server.launch(true));
        this.addButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forServerEvent("server_add_mods", server));
            AddModsDialog addModsDialog = new AddModsDialog(server);
            addModsDialog.setVisible(true);
        });
        this.editButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forServerEvent("server_edit_mods", server));
            EditModsDialog editModsDialog = new EditModsDialog(server);
            editModsDialog.setVisible(true);
        });
        this.backupButton.addActionListener(e -> server.backup());
        this.deleteButton.addActionListener(e -> {
            int ret = DialogManager.yesNoDialog(false).setTitle(GetText.tr("Delete Server"))
                .setContent(GetText.tr("Are you sure you want to delete this server?")).setType(DialogManager.ERROR)
                .show();

            if (ret == DialogManager.YES_OPTION) {
                Analytics.trackEvent(AnalyticsEvent.forServerEvent("server_delete", server));
                final ProgressDialog<Object> dialog = new ProgressDialog<>(GetText.tr("Deleting Server"), 0,
                    GetText.tr("Deleting Server. Please wait..."), null, App.launcher.getParent());
                dialog.addThread(new Thread(() -> {
                    ServerManager.removeServer(server);
                    dialog.close();
                    NotificationManager.show(GetText.tr("Deleted Server Successfully"));
                }));
                dialog.start();
            }
        });
        this.openButton.addActionListener(e -> OS.openFileExplorer(server.getRoot()));
    }

    private void addMouseListeners() {
        this.image.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    launch();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu rightClickMenu = new JPopupMenu();

                    JMenuItem changeDescriptionItem = new JMenuItem(GetText.tr("Change Description"));
                    rightClickMenu.add(changeDescriptionItem);

                    JMenuItem changeImageItem = new JMenuItem(GetText.tr("Change Image"));
                    rightClickMenu.add(changeImageItem);

                    rightClickMenu.show(image, e.getX(), e.getY());

                    changeDescriptionItem.addActionListener(e14 -> server.startChangeDescription());

                    changeImageItem.addActionListener(e13 -> {
                        server.startChangeImage();
                        image.setImage(server.getImage().getImage());
                    });
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private void setupButtonPopupMenus() {
        if (server.showGetHelpButton()) {
            if (server.getDiscordInviteUrl() != null) {
                discordLinkMenuItem.addActionListener(e -> OS.openWebBrowser(server.getDiscordInviteUrl()));
                getHelpPopupMenu.add(discordLinkMenuItem);
            }

            if (server.getSupportUrl() != null) {
                supportLinkMenuItem.addActionListener(e -> OS.openWebBrowser(server.getSupportUrl()));
                getHelpPopupMenu.add(supportLinkMenuItem);
            }

            if (server.getWebsiteUrl() != null) {
                websiteLinkMenuItem.addActionListener(e -> OS.openWebBrowser(server.getWebsiteUrl()));
                getHelpPopupMenu.add(websiteLinkMenuItem);
            }

            if (server.getWikiUrl() != null) {
                wikiLinkMenuItem.addActionListener(e -> OS.openWebBrowser(server.getWikiUrl()));
                getHelpPopupMenu.add(wikiLinkMenuItem);
            }

            if (server.getSourceUrl() != null) {
                sourceLinkMenuItem.addActionListener(e -> OS.openWebBrowser(server.getSourceUrl()));
                getHelpPopupMenu.add(sourceLinkMenuItem);
            }
        }
    }

    @Override
    public void setLayoutWidth(int width) {
        if (width <= 0 || width == layoutWidth) {
            return;
        }

        layoutWidth = width;

        if (coverWrapper != null) {
            coverWrapper.setPreferredSize(new Dimension(width, Math.round(width * 9f / 16f)));
        }

        int textWidth = width - UIScale.scale(MD3Spacing.L) - UIScale.scale(MD3Spacing.S);

        if (titleLabel != null) {
            titleLabel.fitTo(textWidth);
        }

        if (subtitleLabel != null) {
            subtitleLabel.fitTo(textWidth);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return size;
    }

    @Override
    public void onRelocalization() {
        this.launchButton.setText(GetText.tr("Launch"));
        this.launchAndCloseButton.setText(GetText.tr("Launch & Close"));
        this.launchWithGui.setText(GetText.tr("Launch With GUI"));
        this.launchWithGuiAndClose.setText(GetText.tr("Launch With GUI & Close"));
        this.backupButton.setText(GetText.tr("Backup"));
        this.deleteButton.setText(GetText.tr("Delete"));
        this.openButton.setText(GetText.tr("Open Folder"));

        if (launchAction != null) {
            launchAction.setText(GetText.tr("Launch"));
        }

        // Paper/Purpur only supports plugins and not mods, so update the button text to be clear
        if (server.loaderVersion != null && (server.loaderVersion.isPaper() || server.loaderVersion.isPurpur())) {
            this.addButton.setText(GetText.tr("Add Plugins"));
            this.editButton.setText(GetText.tr("Edit Plugins"));
        } else {
            this.addButton.setText(GetText.tr("Add Mods"));
            this.editButton.setText(GetText.tr("Edit Mods"));
        }
    }
}
