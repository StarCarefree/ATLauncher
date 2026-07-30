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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.BackupMode;
import com.atlauncher.data.Instance;
import com.atlauncher.data.minecraft.loaders.LoaderType;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.components.DropDownButton;
import com.atlauncher.gui.components.ImagePanel;
import com.atlauncher.gui.dialogs.AddModsDialog;
import com.atlauncher.gui.dialogs.EditModsDialog;
import com.atlauncher.gui.dialogs.InstanceExportDialog;
import com.atlauncher.gui.dialogs.InstanceSettingsDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.layouts.CardGridLayout;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;
import com.formdev.flatlaf.util.UIScale;

/**
 * One instance, as a Material 3 card.
 *
 * <p>
 * The old card was a collapsible titled panel carrying fourteen visible controls - eight buttons and
 * six dropdowns holding twenty more items - which made every instance look equally urgent and left
 * no room for the cover art. Here the cover leads, the metadata is a row of badges, playing is the
 * one filled button, and everything else lives behind the overflow.
 *
 * <p>
 * Nothing was dropped. The original buttons and menus are still constructed and still hold all the
 * wiring; they are simply no longer laid out, and the overflow menu is built from them on each
 * open. That keeps one source of truth for what each action does and for when it applies -
 * {@code setEditInstanceMenuItemVisbility} still decides which loader entries appear, and the menu
 * follows without knowing anything about loaders.
 */
public class InstanceCard extends MD3Card implements RelocalizationListener, CardGridLayout.WidthAware {
    public static final int CARD_WIDTH = 280;
    public static final int MAX_CARD_WIDTH = 400;

    private static final int MAX_BADGES = 3;
    /** 16:9 against the card width. */
    private static final int COVER_HEIGHT = 158;

    private final String titleFormat;

    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private MD3Button playAction;
    private MD3IconButton overflowAction;
    private JPanel coverWrapper;

    /** Scaled; -1 until the grid has said how wide this card is. */
    private int layoutWidth = -1;

    private final Instance instance;
    private final ImagePanel image;
    private final JButton updateButton = new JButton(GetText.tr("Update"));
    private final JButton deleteButton = new JButton(GetText.tr("Delete"));
    private final JButton exportButton = new JButton(GetText.tr("Export"));
    private final JButton addButton = new JButton(GetText.tr("Add Mods"));
    private final JButton editButton = new JButton(GetText.tr("Edit Mods"));
    private final JButton serversButton = new JButton(GetText.tr("Servers"));
    private final JButton openWebsite = new JButton(GetText.tr("Open Website"));
    private final JButton settingsButton = new JButton(GetText.tr("Settings"));

    private final JPopupMenu openPopupMenu = new JPopupMenu();
    private final JMenuItem openResourceMenuItem = new JMenuItem(GetText.tr("Open Resources"));
    private final JMenuItem openInstanceJsonMenuItem = new JMenuItem(GetText.tr("Open instance.json"));
    private final DropDownButton openButton = new DropDownButton(GetText.tr("Open Folder"), openPopupMenu, true,
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    OS.openFileExplorer(instance.getRoot());
                }
            });

    private final JPopupMenu playPopupMenu = new JPopupMenu();
    private final JMenuItem playOnlinePlayMenuItem = new JMenuItem(GetText.tr("Play Online"));
    private final JMenuItem playOfflinePlayMenuItem = new JMenuItem(GetText.tr("Play Offline"));
    private final DropDownButton playButton = new DropDownButton(GetText.tr("Play"), playPopupMenu, true,
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    play(false);
                }
            });

    private final JPopupMenu backupPopupMenu = new JPopupMenu();
    private final JMenuItem normalBackupMenuItem = new JMenuItem(GetText.tr("Normal Backup"));
    private final JMenuItem normalPlusModsBackupMenuItem = new JMenuItem(GetText.tr("Normal + Mods Backup"));
    private final JMenuItem fullBackupMenuItem = new JMenuItem(GetText.tr("Full Backup"));
    private final DropDownButton backupButton = new DropDownButton(GetText.tr("Backup"), backupPopupMenu);

    private final JPopupMenu getHelpPopupMenu = new JPopupMenu();
    private final JMenuItem discordLinkMenuItem = new JMenuItem(GetText.tr("Discord"));
    private final JMenuItem supportLinkMenuItem = new JMenuItem(GetText.tr("Support"));
    private final JMenuItem websiteLinkMenuItem = new JMenuItem(GetText.tr("Website"));
    private final JMenuItem wikiLinkMenuItem = new JMenuItem(GetText.tr("Wiki"));
    private final JMenuItem sourceLinkMenuItem = new JMenuItem(GetText.tr("Source"));
    private final DropDownButton getHelpButton = new DropDownButton(GetText.tr("Get Help"), getHelpPopupMenu);

    private final JPopupMenu editInstancePopupMenu = new JPopupMenu();
    private final JMenuItem reinstallMenuItem = new JMenuItem(GetText.tr("Reinstall"));
    private final JMenuItem cloneMenuItem = new JMenuItem(GetText.tr("Clone"));
    private final JMenuItem renameMenuItem = new JMenuItem(GetText.tr("Rename"));
    private final JMenuItem changeDescriptionMenuItem = new JMenuItem(GetText.tr("Change Description"));
    private final JMenuItem changeImageMenuItem = new JMenuItem(GetText.tr("Change Image"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem addFabricMenuItem = new JMenuItem(GetText.tr("Add {0}", "Fabric"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeFabricVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "Fabric"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem removeFabricMenuItem = new JMenuItem(GetText.tr("Remove {0}", "Fabric"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem addForgeMenuItem = new JMenuItem(GetText.tr("Add {0}", "Forge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeForgeVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "Forge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem removeForgeMenuItem = new JMenuItem(GetText.tr("Remove {0}", "Forge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem addLegacyFabricMenuItem = new JMenuItem(GetText.tr("Add {0}", "Legacy Fabric"));
    // #. {0} is the loader (Forge/LegacyFabric/Quilt)
    private final JMenuItem changeLegacyFabricVersionMenuItem = new JMenuItem(
            GetText.tr("Change {0} Version", "Legacy Fabric"));
    // #. {0} is the loader (Forge/LegacyFabric/Quilt)
    private final JMenuItem removeLegacyFabricMenuItem = new JMenuItem(GetText.tr("Remove {0}", "Legacy Fabric"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem addNeoForgeMenuItem = new JMenuItem(GetText.tr("Add {0}", "NeoForge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeNeoForgeVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "NeoForge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem removeNeoForgeMenuItem = new JMenuItem(GetText.tr("Remove {0}", "NeoForge"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem addQuiltMenuItem = new JMenuItem(GetText.tr("Add {0}", "Quilt"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeQuiltVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "Quilt"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem removeQuiltMenuItem = new JMenuItem(GetText.tr("Remove {0}", "Quilt"));
    private final DropDownButton editInstanceButton = new DropDownButton(GetText.tr("Edit Instance"),
            editInstancePopupMenu);

    private final boolean hasUpdate;

    public InstanceCard(Instance instance, boolean hasUpdate, String instanceTitleFormat) {
        super(Variant.FILLED, new BorderLayout());

        setHoverElevation(true);

        this.instance = instance;
        this.image = new ImagePanel(() -> instance.getImage().getImage());
        this.hasUpdate = hasUpdate;
        this.titleFormat = instanceTitleFormat;

        // the cover runs to the card's edges, so the padding belongs to the body instead
        setBorder(null);

        setupPlayPopupMenus();
        setupOpenPopupMenus();
        setupButtonPopupMenus();
        applyAvailabilityRules();

        add(buildCover(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        this.addActionListeners();
        this.addMouseListeners();

        RelocalizationManager.addListener(this);
    }

    /**
     * Hides the actions that do not apply to this instance. They stay constructed - the overflow
     * menu reads their visibility rather than repeating the conditions.
     */
    private void applyAvailabilityRules() {
        exportButton.setVisible(instance.canBeExported());
        getHelpButton.setVisible(instance.showGetHelpButton());
        updateButton.setVisible(instance.isUpdatable() && hasUpdate);
        openWebsite.setVisible(instance.hasWebsite());

        boolean serversApply = !instance.isExternalPack() && !instance.launcher.vanillaInstance
                && (instance.getPack() == null || !instance.getPack().system);
        serversButton.setVisible(serversApply);

        addButton.setVisible(instance.launcher.enableCurseForgeIntegration
                && (ConfigManager.getConfigItem("platforms.curseforge.modsEnabled", true)
                        || (ConfigManager.getConfigItem("platforms.modrinth.modsEnabled", true)
                                && instance.launcher.loaderVersion != null)));

        editButton.setVisible(instance.launcher.enableEditingMods);

        playButton.setEnabled(instance.launcher.isPlayable);
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

        titleLabel = new JLabel(instance.launcher.name);
        titleLabel.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM));
        titleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        titleLabel.setForeground(MD3Color.onSurface());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setToolTipText(formattedTitle());

        subtitleLabel = new JLabel(subtitleText());
        subtitleLabel.setFont(MD3Type.font(MD3Type.BODY_SMALL));
        subtitleLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        subtitleLabel.setForeground(MD3Color.onSurfaceVariant());
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel badges = new JPanel(new WrapLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.XS), 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);

        for (MD3Badge badge : buildBadges()) {
            badges.add(badge);
        }

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, 0, 0, 0));

        playAction = MD3Button.filled(GetText.tr("Play"), MD3Icon.of(MD3Icons.PLAY));
        playAction.setEnabled(instance.launcher.isPlayable);
        playAction.addActionListener(e -> play(false));

        overflowAction = new MD3IconButton(MD3Icons.MORE_VERT, GetText.tr("More options"));
        overflowAction.addActionListener(e -> {
            JPopupMenu menu = buildOverflowMenu();
            menu.show(overflowAction, 0, overflowAction.getHeight());
        });

        actions.add(playAction, BorderLayout.WEST);
        actions.add(overflowAction, BorderLayout.EAST);

        body.add(titleLabel);
        body.add(subtitleLabel);
        body.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        body.add(badges);
        body.add(actions);

        return body;
    }

    private String formattedTitle() {
        try {
            return String.format(titleFormat, instance.launcher.name, instance.launcher.pack,
                    instance.launcher.version, instance.id);
        } catch (Throwable t) {
            return instance.launcher.name;
        }
    }

    private String subtitleText() {
        String pack = instance.launcher.pack;
        String version = instance.launcher.version;

        // an instance usually keeps its pack's name, and repeating it under the title says nothing
        if (pack != null && pack.equals(instance.launcher.name)) {
            pack = null;
        }

        if (pack == null || pack.isEmpty()) {
            return version == null ? "" : version;
        }

        return version == null || version.isEmpty() ? pack : pack + " · " + version;
    }

    /**
     * The grid shares the leftover width out between the columns, so a card is told how wide it has
     * ended up before it is asked how tall it needs to be - the cover art has a fixed aspect and its
     * height follows from that.
     */
    @Override
    public void setLayoutWidth(int width) {
        if (width <= 0 || width == layoutWidth) {
            return;
        }

        layoutWidth = width;

        if (coverWrapper != null) {
            coverWrapper.setPreferredSize(new Dimension(width, Math.round(width * 9f / 16f)));
        }
    }

    /**
     * Every card in a row is the same width so the grid stays regular. Height follows the content,
     * since a card with four badges genuinely needs a line more than one with none.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = layoutWidth > 0 ? layoutWidth : UIScale.scale(CARD_WIDTH);

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * At most three badges, in descending order of what the user needs to know.
     *
     * <p>
     * The cap is a layout constraint as much as an editorial one - a fourth badge wraps onto a
     * second line that the card's height was not measured for, and the wrapped row is then clipped.
     * It is also the right call on its own: a card that says five things at once says nothing.
     */
    private List<MD3Badge> buildBadges() {
        List<MD3Badge> badges = new ArrayList<>();

        if (!instance.launcher.isPlayable) {
            badges.add(MD3Badge.problem(GetText.tr("Corrupted")));
        } else if (hasUpdate) {
            badges.add(MD3Badge.notable(GetText.tr("Update available")));
        }

        if (instance.id != null && !instance.id.isEmpty()) {
            badges.add(MD3Badge.neutral(instance.id));
        }

        if (instance.launcher.loaderVersion != null && badges.size() < MAX_BADGES) {
            // the loader's name is what identifies an instance; its patch version is detail, and
            // detail that is often longer than the badge it would sit in
            MD3Badge loader = MD3Badge.neutral(instance.launcher.loaderVersion.type);
            loader.setToolTipText(instance.launcher.loaderVersion.toString());
            badges.add(loader);
        }

        int mods = instance.launcher.mods == null ? 0 : instance.launcher.mods.size();

        if (mods > 0 && badges.size() < MAX_BADGES) {
            // #. {0} is the number of mods installed in an instance
            badges.add(MD3Badge.neutral(GetText.tr("{0} mods", mods)));
        }

        return badges;
    }

    /**
     * Built fresh on every open, so it always reflects the current state of the actions behind it -
     * including their translated labels after a language change.
     */
    private JPopupMenu buildOverflowMenu() {
        setEditInstanceMenuItemVisbility();

        JPopupMenu menu = new JPopupMenu();

        addAction(menu, playOfflinePlayMenuItem);
        addAction(menu, updateButton);
        menu.addSeparator();

        addSubmenu(menu, editInstanceButton, editInstancePopupMenu);
        addAction(menu, addButton);
        addAction(menu, editButton);
        addAction(menu, settingsButton);
        menu.addSeparator();

        addSubmenu(menu, backupButton, backupPopupMenu);
        addAction(menu, openButton, GetText.tr("Open Folder"), () -> OS.openFileExplorer(instance.getRoot()));
        addSubmenu(menu, null, openPopupMenu);
        addAction(menu, exportButton);
        menu.addSeparator();

        addAction(menu, serversButton);
        addAction(menu, openWebsite);
        addSubmenu(menu, getHelpButton, getHelpPopupMenu);
        menu.addSeparator();

        addAction(menu, deleteButton);

        return menu;
    }

    private static void addAction(JPopupMenu menu, AbstractButton source) {
        if (!source.isVisible()) {
            return;
        }

        menu.add(delegateTo(source));
    }

    private static void addAction(JPopupMenu menu, AbstractButton source, String text, Runnable action) {
        if (!source.isVisible()) {
            return;
        }

        JMenuItem item = new JMenuItem(text);
        item.setEnabled(source.isEnabled());
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    /**
     * Folds a dropdown button's menu in as a submenu, or straight into the parent when there is no
     * button to name it after.
     */
    private static void addSubmenu(JPopupMenu menu, AbstractButton source, JPopupMenu contents) {
        if (source != null && !source.isVisible()) {
            return;
        }

        if (source == null) {
            for (Component c : contents.getComponents()) {
                copyInto(menu, c);
            }

            return;
        }

        JMenu submenu = new JMenu(source.getText());
        submenu.setEnabled(source.isEnabled());

        for (Component c : contents.getComponents()) {
            copyInto(submenu.getPopupMenu(), c);
        }

        if (submenu.getMenuComponentCount() > 0) {
            menu.add(submenu);
        }
    }

    private static void copyInto(JPopupMenu target, Component source) {
        if (source instanceof JMenuItem && source.isVisible()) {
            target.add(delegateTo((JMenuItem) source));
        } else if (source instanceof JSeparator) {
            target.addSeparator();
        }
    }

    /**
     * A menu item that forwards to the control holding the real action, so behaviour lives in one
     * place no matter how it is surfaced.
     */
    private static JMenuItem delegateTo(AbstractButton source) {
        JMenuItem item = new JMenuItem(source.getText());
        item.setEnabled(source.isEnabled());
        item.addActionListener(e -> source.doClick());

        return item;
    }

    private void setupPlayPopupMenus() {
        playOnlinePlayMenuItem.addActionListener(e -> play(false));
        playPopupMenu.add(playOnlinePlayMenuItem);

        playOfflinePlayMenuItem.addActionListener(e -> play(true));
        playPopupMenu.add(playOfflinePlayMenuItem);
    }

    private void setupOpenPopupMenus() {
        openResourceMenuItem.addActionListener(e -> {
            DialogManager.okDialog().setTitle(GetText.tr("Reminder"))
                    .setContent(GetText.tr("You may not distribute ANY resources."))
                    .setType(DialogManager.WARNING).show();
            OS.openFileExplorer(instance.getMinecraftJarLibraryPath());
        });
        openPopupMenu.add(openResourceMenuItem);

        openInstanceJsonMenuItem.addActionListener(e -> {
            OS.openFile(instance.getRoot().resolve("instance.json"));
        });
        openPopupMenu.add(openInstanceJsonMenuItem);
    }

    private void setupButtonPopupMenus() {
        if (instance.showGetHelpButton()) {
            if (instance.getDiscordInviteUrl() != null) {
                discordLinkMenuItem.addActionListener(e -> OS.openWebBrowser(instance.getDiscordInviteUrl()));
                getHelpPopupMenu.add(discordLinkMenuItem);
            }

            if (instance.getSupportUrl() != null) {
                supportLinkMenuItem.addActionListener(e -> OS.openWebBrowser(instance.getSupportUrl()));
                getHelpPopupMenu.add(supportLinkMenuItem);
            }

            if (instance.getWebsiteUrl() != null) {
                websiteLinkMenuItem.addActionListener(e -> OS.openWebBrowser(instance.getWebsiteUrl()));
                getHelpPopupMenu.add(websiteLinkMenuItem);
            }

            if (instance.getWikiUrl() != null) {
                wikiLinkMenuItem.addActionListener(e -> OS.openWebBrowser(instance.getWikiUrl()));
                getHelpPopupMenu.add(wikiLinkMenuItem);
            }

            if (instance.getSourceUrl() != null) {
                sourceLinkMenuItem.addActionListener(e -> OS.openWebBrowser(instance.getSourceUrl()));
                getHelpPopupMenu.add(sourceLinkMenuItem);
            }
        }

        normalBackupMenuItem.addActionListener(e -> instance.backup(BackupMode.NORMAL));
        backupPopupMenu.add(normalBackupMenuItem);

        normalPlusModsBackupMenuItem.addActionListener(e -> instance.backup(BackupMode.NORMAL_PLUS_MODS));
        backupPopupMenu.add(normalPlusModsBackupMenuItem);

        fullBackupMenuItem.addActionListener(e -> instance.backup(BackupMode.FULL));
        backupPopupMenu.add(fullBackupMenuItem);

        setupEditInstanceButton();
    }

    private void setupEditInstanceButton() {
        editInstancePopupMenu.add(reinstallMenuItem);
        editInstancePopupMenu.add(cloneMenuItem);
        editInstancePopupMenu.add(renameMenuItem);
        editInstancePopupMenu.add(changeDescriptionMenuItem);
        editInstancePopupMenu.add(changeImageMenuItem);
        editInstancePopupMenu.addSeparator();

        if (ConfigManager.getConfigItem("loaders.fabric.enabled", true)
                && !ConfigManager.getConfigItem("loaders.fabric.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(addFabricMenuItem);
            editInstancePopupMenu.add(changeFabricVersionMenuItem);
        }
        editInstancePopupMenu.add(removeFabricMenuItem);

        if (ConfigManager.getConfigItem("loaders.forge.enabled", true)
                && !ConfigManager.getConfigItem("loaders.forge.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(addForgeMenuItem);
            editInstancePopupMenu.add(changeForgeVersionMenuItem);
        }
        editInstancePopupMenu.add(removeForgeMenuItem);

        if (ConfigManager.getConfigItem("loaders.legacyfabric.enabled", true)
                && !ConfigManager
                        .getConfigItem("loaders.legacyfabric.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(addLegacyFabricMenuItem);
            editInstancePopupMenu.add(changeLegacyFabricVersionMenuItem);
        }
        editInstancePopupMenu.add(removeLegacyFabricMenuItem);

        if (ConfigManager.getConfigItem("loaders.neoforge.enabled", true)
                && !ConfigManager
                        .getConfigItem("loaders.neoforge.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(addNeoForgeMenuItem);
            editInstancePopupMenu.add(changeNeoForgeVersionMenuItem);
        }
        editInstancePopupMenu.add(removeNeoForgeMenuItem);

        if (ConfigManager.getConfigItem("loaders.quilt.enabled", false)
                && !ConfigManager.getConfigItem("loaders.quilt.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(addQuiltMenuItem);
            editInstancePopupMenu.add(changeQuiltVersionMenuItem);
        }
        editInstancePopupMenu.add(removeQuiltMenuItem);

        setEditInstanceMenuItemVisbility();

        reinstallMenuItem.addActionListener(e -> instance.startReinstall());
        cloneMenuItem.addActionListener(e -> instance.startClone());
        renameMenuItem.addActionListener(e -> instance.startRename());
        changeDescriptionMenuItem.addActionListener(e -> instance.startChangeDescription());
        changeImageMenuItem.addActionListener(e -> {
            instance.startChangeImage();
            image.setImage(instance.getImage().getImage());
        });

        // loader things
        addFabricMenuItem.addActionListener(e -> {
            instance.addLoader(LoaderType.FABRIC);
            setEditInstanceMenuItemVisbility();
        });
        addForgeMenuItem.addActionListener(e -> {
            instance.addLoader(LoaderType.FORGE);
            setEditInstanceMenuItemVisbility();
        });
        addLegacyFabricMenuItem.addActionListener(e -> {
            instance.addLoader(LoaderType.LEGACY_FABRIC);
            setEditInstanceMenuItemVisbility();
        });
        addNeoForgeMenuItem.addActionListener(e -> {
            instance.addLoader(LoaderType.NEOFORGE);
            setEditInstanceMenuItemVisbility();
        });
        addQuiltMenuItem.addActionListener(e -> {
            instance.addLoader(LoaderType.QUILT);
            setEditInstanceMenuItemVisbility();
        });

        changeFabricVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeForgeVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeLegacyFabricVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeNeoForgeVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeQuiltVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });

        removeFabricMenuItem.addActionListener(e -> {
            instance.removeLoader();
            setEditInstanceMenuItemVisbility();
        });
        removeForgeMenuItem.addActionListener(e -> {
            instance.removeLoader();
            setEditInstanceMenuItemVisbility();
        });
        removeLegacyFabricMenuItem.addActionListener(e -> {
            instance.removeLoader();
            setEditInstanceMenuItemVisbility();
        });
        removeNeoForgeMenuItem.addActionListener(e -> {
            instance.removeLoader();
            setEditInstanceMenuItemVisbility();
        });
        removeQuiltMenuItem.addActionListener(e -> {
            instance.removeLoader();
            setEditInstanceMenuItemVisbility();
        });
    }

    private void setEditInstanceMenuItemVisbility() {
        reinstallMenuItem.setVisible(instance.isUpdatable());

        addFabricMenuItem.setVisible(instance.launcher.loaderVersion == null);
        addForgeMenuItem.setVisible(instance.launcher.loaderVersion == null);
        addLegacyFabricMenuItem.setVisible(instance.launcher.loaderVersion == null);
        addNeoForgeMenuItem.setVisible(instance.launcher.loaderVersion == null);
        addQuiltMenuItem.setVisible(instance.launcher.loaderVersion == null);

        changeFabricVersionMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isFabric());
        changeForgeVersionMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isForge());
        changeLegacyFabricVersionMenuItem
                .setVisible(
                        instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isLegacyFabric());
        changeNeoForgeVersionMenuItem
                .setVisible(
                        instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isNeoForge());
        changeQuiltVersionMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isQuilt());

        removeFabricMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isFabric());
        removeForgeMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isForge());
        removeLegacyFabricMenuItem
                .setVisible(
                        instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isLegacyFabric());
        removeNeoForgeMenuItem
                .setVisible(
                        instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isNeoForge());
        removeQuiltMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isQuilt());
    }

    private void addActionListeners() {
        this.updateButton.addActionListener(e -> {
            if (AccountManager.getSelectedAccount() == null) {
                DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                        .setContent(GetText.tr("Cannot update pack as you have no account selected."))
                        .setType(DialogManager.ERROR).show();
                return;
            }

            Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_update", instance));
            instance.update();
        });
        this.addButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_add_mods", instance));
            AddModsDialog addModsDialog = new AddModsDialog(instance);
            addModsDialog.setVisible(true);
            exportButton.setVisible(instance.canBeExported());
        });
        this.editButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_edit_mods", instance));
            EditModsDialog editModsDialog = new EditModsDialog(instance);
            editModsDialog.setVisible(true);
            exportButton.setVisible(instance.canBeExported());
        });
        this.serversButton.addActionListener(e -> OS.openWebBrowser(
                String.format("%s/%s?utm_source=launcher&utm_medium=button&utm_campaign=instance_v2_button",
                        Constants.SERVERS_LIST_PACK, instance.getSafePackName())));
        this.openWebsite.addActionListener(e -> OS.openWebBrowser(instance.getWebsiteUrl()));
        this.settingsButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_settings", instance));
            InstanceSettingsDialog instanceSettingsDialog = new InstanceSettingsDialog(instance);
            instanceSettingsDialog.setVisible(true);
        });
        this.deleteButton.addActionListener(e -> {
            int ret = DialogManager.yesNoDialog(false).setTitle(GetText.tr("Delete Instance"))
                    .setContent(
                            GetText.tr("Are you sure you want to delete the instance \"{0}\"?", instance.launcher.name))
                    .setType(DialogManager.ERROR).show();

            if (ret == DialogManager.YES_OPTION) {
                Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_delete", instance));
                final ProgressDialog<Object> dialog = new ProgressDialog<>(GetText.tr("Deleting Instance"), 0,
                        GetText.tr("Deleting Instance. Please wait..."), null, App.launcher.getParent());
                dialog.addThread(new Thread(() -> {
                    InstanceManager.removeInstance(instance);
                    dialog.close();
                    NotificationManager.show(GetText.tr("Deleted Instance Successfully"));
                }));
                dialog.start();
            }
        });
        this.exportButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_export", instance));
            InstanceExportDialog instanceExportDialog = new InstanceExportDialog(instance);
            instanceExportDialog.setVisible(true);
        });
    }

    private void play(boolean offline) {
        if (!instance.launcher.isPlayable) {
            DialogManager.okDialog().setTitle(GetText.tr("Instance Corrupt"))
                    .setContent(GetText
                            .tr("Cannot play instance as it's corrupted. Please reinstall, update or delete it."))
                    .setType(DialogManager.ERROR).show();
            return;
        }

        if (!App.settings.ignoreJavaOnInstanceLaunch && instance.shouldShowWrongJavaWarning()) {
            DialogManager.okDialog().setTitle(GetText.tr("Cannot launch instance due to your Java version"))
                    .setContent(new HTMLBuilder().center().text(GetText.tr(
                            "There was an issue launching this instance.<br/><br/>This version of the pack requires a Java version which you are not using.<br/><br/>Please install that version of Java and try again.<br/><br/>Java version needed: {0}",
                            instance.launcher.java.getVersionString())).build())
                    .setType(DialogManager.ERROR).show();
            return;
        }

        if (hasUpdate && !instance.hasLatestUpdateBeenIgnored()) {
            int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Update Available"))
                    .setContent(new HTMLBuilder().center()
                            .text(GetText.tr(
                                    "An update is available for this instance.<br/><br/>Do you want to update now?"))
                            .build())
                    .addOption(GetText.tr("Ignore This Update"))
                    .addOption(GetText.tr("Don't Remind Me Again")).setType(DialogManager.INFO)
                    .show();

            if (ret == 0) {
                if (AccountManager.getSelectedAccount() == null) {
                    DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                            .setContent(GetText.tr("Cannot update pack as you have no account selected."))
                            .setType(DialogManager.ERROR).show();
                } else {
                    Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_update", instance));
                    instance.update();
                }
            } else if (ret == 1 || ret == DialogManager.CLOSED_OPTION || ret == 2 || ret == 3) {
                if (ret == 2) {
                    instance.ignoreUpdate();
                } else if (ret == 3) {
                    instance.ignoreAllUpdates();
                }

                if (!App.launcher.minecraftLaunched) {
                    if (instance.launch()) {
                        App.launcher.setMinecraftLaunched(true);
                    }
                }
            }
        } else {
            if (!App.launcher.minecraftLaunched) {
                if (instance.launch(offline)) {
                    App.launcher.setMinecraftLaunched(true);
                }
            }
        }
    }

    private void addMouseListeners() {
        this.image.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    play(false);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu rightClickMenu = new JPopupMenu();

                    JMenuItem playOnlineButton = new JMenuItem(GetText.tr("Play Online"));
                    playOnlineButton.addActionListener(l -> play(false));
                    rightClickMenu.add(playOnlineButton);

                    JMenuItem playOfflineButton = new JMenuItem(GetText.tr("Play Offline"));
                    playOfflineButton.addActionListener(l -> play(true));
                    rightClickMenu.add(playOfflineButton);

                    if (instance.isUpdatable()) {
                        rightClickMenu.addSeparator();
                    }

                    JMenuItem reinstallItem = new JMenuItem(GetText.tr("Reinstall"));
                    reinstallItem.addActionListener(l -> instance.startReinstall());
                    reinstallItem.setVisible(instance.isUpdatable());
                    rightClickMenu.add(reinstallItem);

                    JMenuItem updateItem = new JMenuItem(GetText.tr("Update"));
                    updateItem.addActionListener(l -> instance.update());
                    updateItem.setVisible(instance.isUpdatable());
                    updateItem.setEnabled(hasUpdate && instance.launcher.isPlayable);
                    rightClickMenu.add(updateItem);

                    rightClickMenu.addSeparator();

                    JMenuItem supportPackItem = new JMenuItem(GetText.tr("Create Support Pack"));
                    supportPackItem.addActionListener(l -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle(GetText.tr("Choose location to save support pack"));
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        chooser.setAcceptAllFileFilterUsed(false);

                        if (chooser.showSaveDialog(App.launcher.getParent()) == JFileChooser.APPROVE_OPTION) {
                            Path finalPath = instance.createSupportPack(chooser.getSelectedFile().toPath());
                            if (finalPath != null) {
                                NotificationManager.show(GetText.tr("Support pack created"));
                                OS.openFileExplorer(finalPath);
                            }
                        }
                    });
                    rightClickMenu.add(supportPackItem);

                    JMenuItem renameItem = new JMenuItem(GetText.tr("Rename"));
                    renameItem.addActionListener(l -> instance.startRename());
                    rightClickMenu.add(renameItem);

                    JMenuItem changeDescriptionItem = new JMenuItem(GetText.tr("Change Description"));
                    changeDescriptionItem.addActionListener(l -> instance.startChangeDescription());
                    changeDescriptionItem.setVisible(instance.canChangeDescription());
                    rightClickMenu.add(changeDescriptionItem);

                    JMenuItem changeImageItem = new JMenuItem(GetText.tr("Change Image"));
                    changeImageItem.addActionListener(l -> {
                        instance.startChangeImage();
                        image.setImage(instance.getImage().getImage());
                    });
                    rightClickMenu.add(changeImageItem);

                    JMenuItem cloneItem = new JMenuItem(GetText.tr("Clone"));
                    cloneItem.addActionListener(l -> instance.startClone());
                    rightClickMenu.add(cloneItem);
                    rightClickMenu.show(image, e.getX(), e.getY());
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

    public Instance getInstance() {
        return instance;
    }

    @Override
    public void onRelocalization() {
        this.playButton.setText(GetText.tr("Play"));
        this.updateButton.setText(GetText.tr("Update"));
        this.backupButton.setText(GetText.tr("Backup"));
        this.deleteButton.setText(GetText.tr("Delete"));
        this.addButton.setText(GetText.tr("Add Mods"));
        this.editButton.setText(GetText.tr("Edit Mods"));
        this.serversButton.setText(GetText.tr("Servers"));
        this.openWebsite.setText(GetText.tr("Open Website"));
        this.openButton.setText(GetText.tr("Open Folder"));
        this.openResourceMenuItem.setText(GetText.tr("Open Resources"));
        this.openInstanceJsonMenuItem.setText(GetText.tr("Open instance.json"));
        this.settingsButton.setText(GetText.tr("Settings"));

        this.normalBackupMenuItem.setText(GetText.tr("Normal Backup"));
        this.normalPlusModsBackupMenuItem.setText(GetText.tr("Normal + Mods Backup"));
        this.fullBackupMenuItem.setText(GetText.tr("Full Backup"));
        this.backupButton.setText(GetText.tr("Backup"));

        this.discordLinkMenuItem.setText(GetText.tr("Discord"));
        this.supportLinkMenuItem.setText(GetText.tr("Support"));
        this.websiteLinkMenuItem.setText(GetText.tr("Website"));
        this.wikiLinkMenuItem.setText(GetText.tr("Wiki"));
        this.sourceLinkMenuItem.setText(GetText.tr("Source"));
        this.getHelpButton.setText(GetText.tr("Get Help"));
    }
}
