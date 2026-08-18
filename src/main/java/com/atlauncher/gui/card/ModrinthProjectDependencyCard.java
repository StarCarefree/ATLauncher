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
import java.awt.FlowLayout;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.ModManagement;
import com.atlauncher.data.modrinth.ModrinthDependency;
import com.atlauncher.data.modrinth.ModrinthDownloadMetadata;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.gui.dialogs.ModrinthVersionSelectorDialog;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;
import com.atlauncher.workers.BackgroundImageWorker;
import com.formdev.flatlaf.util.UIScale;

public final class ModrinthProjectDependencyCard extends MD3Card {
    private final ModrinthVersionSelectorDialog parent;
    private final ModrinthDependency dependency;
    private final ModManagement instanceOrServer;

    public ModrinthProjectDependencyCard(ModrinthVersionSelectorDialog parent, ModrinthDependency dependency,
            ModManagement instanceOrServer) {
        super(Variant.FILLED, new BorderLayout(0, MD3Spacing.scale(MD3Spacing.S)));

        this.parent = parent;
        this.dependency = dependency;
        this.instanceOrServer = instanceOrServer;

        setHoverElevation(true);
        setPreferredSize(UIScale.scale(new Dimension(250, 180)));

        setupComponents();
    }

    private void setupComponents() {
        ModrinthProject mod = ModrinthApi.getProject(dependency.projectId);
        String titleText = Optional.ofNullable(mod).map(m -> m.title).orElse(GetText.tr("Unknown Project"));
        String description = Optional.ofNullable(mod).map(m -> m.description).orElse(GetText.tr("Unknown Project"));

        JLabel icon = new JLabel(Utils.getIconImage("/assets/image/no-icon.png"));
        icon.setVisible(false);

        JLabel title = new JLabel(titleText);
        title.setFont(MD3Type.font(MD3Type.TITLE_SMALL, titleText));
        title.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        title.setForeground(MD3Color.onSurface());

        JLabel summary = new JLabel();
        summary.setFont(MD3Type.font(MD3Type.BODY_SMALL, description));
        summary.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        summary.setForeground(MD3Color.onSurfaceVariant());
        summary.setText(MD3Text.wrapToLines(summary.getFontMetrics(summary.getFont()), description,
                UIScale.scale(160), 3));

        JPanel text = new JPanel(new BorderLayout(0, MD3Spacing.scale(MD3Spacing.XS)));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(summary, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(MD3Spacing.scale(MD3Spacing.S), 0));
        body.setOpaque(false);
        body.add(icon, BorderLayout.WEST);
        body.add(text, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        if (mod == null) {
            return;
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, MD3Spacing.scale(MD3Spacing.S), 0));
        buttons.setOpaque(false);
        MD3Button addButton = MD3Button.filled(GetText.tr("Add"));
        MD3Button viewButton = MD3Button.outlined(GetText.tr("View"));
        buttons.add(addButton);
        buttons.add(viewButton);

        addButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forAddMod(mod));
            new ModrinthVersionSelectorDialog(parent, mod, instanceOrServer, ModrinthDownloadMetadata.Reason.DEPENDENCY,
                    parent.getSelectedVersionId()).setVisible(true);
            parent.reloadDependenciesPanel();
        });

        viewButton.addActionListener(e -> OS.openWebBrowser(String.format("https://modrinth.com/mod/%s", mod.slug)));

        add(buttons, BorderLayout.SOUTH);

        if (mod.iconUrl != null && !mod.iconUrl.isEmpty()) {
            new BackgroundImageWorker(icon, mod.iconUrl, 60, 60).execute();
        }
    }
}
