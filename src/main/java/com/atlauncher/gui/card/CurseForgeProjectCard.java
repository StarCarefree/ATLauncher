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

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.ModManagement;
import com.atlauncher.data.curseforge.CurseForgeAttachment;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.OS;

/**
 * One mod in the mod browser's grid, from CurseForge.
 *
 * <p>
 * This and {@link ModrinthSearchHitCard} were the last two pre-Material cards in the launcher and
 * were line for line the same as each other: a {@link javax.swing.JPanel} with a titled border in a
 * hardcoded 12pt bold face, a fixed 250x180, a {@link javax.swing.JTextArea} for the summary and
 * four plain buttons. They now share {@link MD3PackCard} with the six pack cards, which is what
 * that class was extracted to be - a cover, a title, a summary, some badges and the actions.
 *
 * <p>
 * They also threw away most of what the search response carries. The author and the download count
 * were both fetched and then dropped on the floor; they are the card's badges now, because "who
 * wrote this and do other people use it" is most of how a mod gets chosen.
 */
public final class CurseForgeProjectCard extends MD3PackCard {
    private final CurseForgeProject mod;
    private final ModManagement instanceOrServer;

    private final JButton addButton = new JButton(GetText.tr("Add"));
    private final JButton reinstallButton = new JButton(GetText.tr("Reinstall"));
    private final JButton removeButton = new JButton(GetText.tr("Remove"));
    private final JButton viewButton = new JButton(GetText.tr("View"));

    public CurseForgeProjectCard(final CurseForgeProject mod, final ModManagement instanceOrServer,
            ActionListener installAl, ActionListener removeAl) {
        this.mod = mod;
        this.instanceOrServer = instanceOrServer;

        addButton.addActionListener(e -> {
            installAl.actionPerformed(e);
            updateInstalledStatus();
        });
        reinstallButton.addActionListener(installAl);
        removeButton.addActionListener(e -> {
            removeAl.actionPerformed(e);
            updateInstalledStatus();
        });
        viewButton.addActionListener(e -> OS.openWebBrowser(mod.getWebsiteUrl()));

        // the search response only carries a one line summary, so the description dialog fetches
        // the real thing when it is opened
        setDescriptionLoader(() -> CurseForgeApi.getProjectDescription(mod.id));

        applyInstalledState();

        Optional<CurseForgeAttachment> logo = mod.getLogo();

        build(mod.name, coverFromUrl(logo.isPresent() ? logo.get().thumbnailUrl : null), mod.summary, badges(),
                primary(), overflow());
    }

    private void updateInstalledStatus() {
        applyInstalledState();

        refreshActions(primary(), overflow());
    }

    private void applyInstalledState() {
        boolean installed = isInstalled();

        addButton.setVisible(!installed);
        reinstallButton.setVisible(installed);
        removeButton.setVisible(installed);
    }

    private boolean isInstalled() {
        return instanceOrServer != null && instanceOrServer.getMods().stream()
                .anyMatch(m -> m.isFromCurseForge() && m.curseForgeProjectId == mod.id);
    }

    /** Installed mods lead with reinstalling, since adding one twice is not a thing. */
    private JButton primary() {
        return isInstalled() ? reinstallButton : addButton;
    }

    private JButton[] overflow() {
        return isInstalled() ? new JButton[] { removeButton, viewButton } : new JButton[] { viewButton };
    }

    /**
     * Two, not three: the card measures its badge row for one line, and a long author name plus a
     * download count plus a category wraps onto a second one that is simply clipped off the bottom.
     * Who wrote it and how many people use it are the two that decide a mod; its category is a chip
     * in the toolbar above.
     */
    private List<MD3Badge> badges() {
        List<MD3Badge> badges = new ArrayList<>();

        if (mod.authors != null && !mod.authors.isEmpty() && mod.authors.get(0).name != null) {
            badges.add(MD3Badge.neutral(mod.authors.get(0).name));
        }

        if (mod.downloadCount > 0) {
            badges.add(MD3Badge.neutral(ModCardText.downloads(mod.downloadCount)));
        }

        return badges;
    }
}
