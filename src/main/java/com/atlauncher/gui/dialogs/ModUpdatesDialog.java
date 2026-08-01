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
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.ModManagement;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.ModUpdate;
import com.atlauncher.data.modrinth.ModrinthDownloadMetadata;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListItem;
import com.atlauncher.gui.md3.feedback.MD3Dialog;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.ModUpdateManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Utils;
import com.atlauncher.workers.BackgroundImageWorker;

import com.formdev.flatlaf.util.UIScale;

/**
 * Everything that has a newer file, in one list, updated in one go.
 *
 * <p>
 * "Check For Updates" used to run {@code DisableableMod.checkForUpdate} in a loop, and that opens a
 * progress dialog and then a version selector <em>per mod</em>. Ticking fifty mods meant clicking
 * through fifty pairs of dialogs, with no way to see beforehand which of them even had an update -
 * and the summary at the end said "the selected mods have been checked" whatever had happened.
 *
 * <p>
 * So the check is now one pass ({@link ModUpdateManager}) and the result is this: a row per update
 * saying what it is going from and to, everything ticked, and a single download afterwards. The per
 * mod flow is still on the right click menu, where picking a specific version is the point.
 */
public final class ModUpdatesDialog {
    /** Wide enough for a mod name, both version numbers and the badges without wrapping. */
    private static final int DIALOG_WIDTH = 640;

    /** Past this the list scrolls rather than growing the window off the screen. */
    private static final int MAX_LIST_HEIGHT = 360;

    private static final int ICON_SIZE = 32;

    private ModUpdatesDialog() {
    }

    /**
     * @param updates what the check found; showing this with an empty list is a caller error, since
     *                "no updates" is a sentence rather than an empty list
     * @return how many mods were actually updated, so the caller knows whether to reload
     */
    public static int show(Window parent, ModManagement instanceOrServer, List<ModUpdate> updates) {
        if (updates.isEmpty()) {
            return 0;
        }

        Analytics.sendScreenView("Mod Updates Dialog");

        Map<ModUpdate, JCheckBox> boxes = new LinkedHashMap<>();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        for (ModUpdate update : updates) {
            JCheckBox box = new JCheckBox();
            box.setOpaque(false);
            box.setSelected(true);
            box.setToolTipText(update.getName());

            boxes.put(update, box);
            list.add(buildRow(update, box));
        }

        JCheckBox selectAll = new JCheckBox(GetText.tr("Select All"));
        selectAll.setOpaque(false);
        selectAll.setSelected(true);
        selectAll.addActionListener(e -> boxes.values().forEach(b -> b.setSelected(selectAll.isSelected())));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(buildHeader(selectAll), BorderLayout.NORTH);
        content.add(buildScroller(list), BorderLayout.CENTER);

        int chosen = MD3Dialog.builder(parent == null ? DialogManager.parentWindow() : parent)
                .title(GetText.tr("Mod Updates"))
                .headline(GetText.tr("Mod Updates"))
                // #. {0} is the number of mods that have an update available
                .supportingText(GetText.tr("{0} of the mods in this instance have a newer version.", updates.size()))
                .maxWidth(DIALOG_WIDTH)
                .content(content)
                .dismiss(GetText.tr("Cancel"))
                .confirm(GetText.tr("Update Selected"))
                .build()
                .showAndWait();

        if (chosen != 1) {
            return 0;
        }

        List<ModUpdate> selected = new ArrayList<>();

        for (Map.Entry<ModUpdate, JCheckBox> entry : boxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }

        return apply(parent, instanceOrServer, selected);
    }

    /**
     * Downloads every chosen update behind one progress dialog.
     *
     * <p>
     * {@code addFileFromCurseForge} and {@code addFileFromModrinth} already delete the file being
     * replaced and drop the old entry for the same project, so nothing here has to uninstall first.
     */
    private static int apply(Window parent, ModManagement instanceOrServer, List<ModUpdate> selected) {
        if (selected.isEmpty()) {
            return 0;
        }

        Analytics.trackEvent(AnalyticsEvent.simpleEvent("mod_bulk_update"));

        final int[] updated = new int[1];

        ProgressDialog<Void> dialog = parent == null
                ? new ProgressDialog<Void>(GetText.tr("Updating Mods"), selected.size(), GetText.tr("Updating Mods"))
                : new ProgressDialog<Void>(GetText.tr("Updating Mods"), selected.size(), GetText.tr("Updating Mods"),
                        parent);

        dialog.addThread(new Thread(() -> {
            for (ModUpdate update : selected) {
                try {
                    if (update.platform == ModPlatform.CURSEFORGE) {
                        instanceOrServer.addFileFromCurseForge(update.curseForgeProject, update.curseForgeFile,
                                dialog);
                    } else {
                        instanceOrServer.addFileFromModrinth(update.modrinthProject, update.modrinthVersion, null,
                                update.mod.type, ModrinthDownloadMetadata.Reason.UPDATE, dialog);
                    }

                    ModUpdateManager.markUpdated(instanceOrServer, update.mod);
                    updated[0]++;
                } catch (Exception e) {
                    // one mod failing is not a reason to abandon the rest of the queue
                    LogManager.logStackTrace("Failed to update " + update.getName(), e);
                }

                dialog.doneTask();
            }

            dialog.close();
        }));

        dialog.start();

        return updated[0];
    }

    private static JPanel buildHeader(JCheckBox selectAll) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(MD3Spacing.border(0, 0, MD3Spacing.S, 0));
        row.add(selectAll, BorderLayout.WEST);
        row.add(MD3Divider.inset(), BorderLayout.SOUTH);

        return row;
    }

    private static JScrollPane buildScroller(JPanel list) {
        JScrollPane scroller = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(UIScale.scale(16));

        int width = UIScale.scale(DIALOG_WIDTH - MD3Spacing.XL * 2);
        int height = Math.min(UIScale.scale(MAX_LIST_HEIGHT), list.getPreferredSize().height);

        scroller.setPreferredSize(new Dimension(width, height));

        return scroller;
    }

    /**
     * A row: tick and icon leading, the mod's name over what it is going from and to, and the
     * platform it came from trailing.
     */
    private static Component buildRow(ModUpdate update, JCheckBox box) {
        MD3ListItem item = new MD3ListItem();

        item.setLeading(buildLeading(update, box));
        item.setHeadline(update.getName());
        // #. {0} is the currently installed version, {1} is the version being updated to
        item.setSupportingText(GetText.tr("{0} to {1}", update.getCurrentVersion(), update.getNewVersion()));
        item.setTrailing(buildBadges(update));

        return item;
    }

    private static JPanel buildLeading(ModUpdate update, JCheckBox box) {
        JLabel icon = new JLabel(Utils.getIconImage("/assets/image/no-icon.png"));
        icon.setPreferredSize(UIScale.scale(new Dimension(ICON_SIZE, ICON_SIZE)));

        String iconUrl = update.getIconUrl();

        if (iconUrl != null && !iconUrl.isEmpty()) {
            new BackgroundImageWorker(icon, iconUrl, UIScale.scale(ICON_SIZE), UIScale.scale(ICON_SIZE)).execute();
        }

        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));
        leading.setOpaque(false);
        leading.add(box);
        leading.add(icon);

        return leading;
    }

    /**
     * The platform, and the release channel where it is not a stable release - which is worth
     * flagging, since it is the one thing about an update a user may want to decline.
     */
    private static JPanel buildBadges(ModUpdate update) {
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(MD3Spacing.XS), 0));
        badges.setOpaque(false);

        String channel = update.getPrereleaseChannel();

        if (channel != null) {
            badges.add(MD3Badge.notable(channel));
        }

        badges.add(MD3Badge.neutral(update.getPlatformName()));

        return badges;
    }

    /** The message for a check that came back with nothing, so both callers say the same thing. */
    public static void showNoUpdates() {
        JLabel label = new JLabel(GetText.tr("Every mod that can be checked is already up to date."));
        label.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, label.getText()));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        label.setForeground(MD3Color.onSurfaceVariant());

        MD3Dialog.builder(DialogManager.parentWindow())
                .title(GetText.tr("No Updates Found"))
                .headline(GetText.tr("No Updates Found"))
                .content(label)
                .dismiss(GetText.tr("Close"))
                .show();
    }
}
