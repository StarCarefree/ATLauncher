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

import javax.swing.JButton;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.ModManagement;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.gui.card.packbrowser.MD3PackCard;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.OS;

/**
 * One mod in the mod browser's grid, from Modrinth.
 *
 * <p>
 * The twin of {@link CurseForgeProjectCard}; see its notes for what these two were and why they are
 * now {@link MD3PackCard}s.
 */
public final class ModrinthSearchHitCard extends MD3PackCard {
    private final ModrinthSearchHit mod;
    private final ModManagement instanceOrServer;

    private final JButton addButton = new JButton(GetText.tr("Add"));
    private final JButton reinstallButton = new JButton(GetText.tr("Reinstall"));
    private final JButton removeButton = new JButton(GetText.tr("Remove"));
    private final JButton viewButton = new JButton(GetText.tr("View"));

    public ModrinthSearchHitCard(final ModrinthSearchHit mod, final ModManagement instanceOrServer,
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
        viewButton.addActionListener(e -> OS.openWebBrowser(String.format("https://modrinth.com/mod/%s", mod.slug)));

        setDescriptionLoader(() -> {
            ModrinthProject project = ModrinthApi.getProject(mod.projectId);

            return project == null ? null : project.body;
        });

        applyInstalledState();

        build(mod.title, coverFromUrl(mod.iconUrl == null || mod.iconUrl.isEmpty() ? null : mod.iconUrl),
                mod.description, badges(), primary(), overflow());
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
                .anyMatch(m -> m.isFromModrinth() && m.modrinthProject.id.equals(mod.projectId));
    }

    private JButton primary() {
        return isInstalled() ? reinstallButton : addButton;
    }

    private JButton[] overflow() {
        return isInstalled() ? new JButton[] { removeButton, viewButton } : new JButton[] { viewButton };
    }

    /** Two rather than three, for the reason given on {@link CurseForgeProjectCard}. */
    private List<MD3Badge> badges() {
        List<MD3Badge> badges = new ArrayList<>();

        if (mod.author != null && !mod.author.isEmpty()) {
            badges.add(MD3Badge.neutral(mod.author));
        }

        if (mod.downloads > 0) {
            badges.add(MD3Badge.neutral(ModCardText.downloads(mod.downloads)));
        }

        return badges;
    }
}
