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
import com.atlauncher.data.curseforge.CurseForgeAttachment;
import com.atlauncher.data.curseforge.CurseForgeFileDependency;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.gui.dialogs.CurseForgeProjectFileSelectorDialog;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;
import com.atlauncher.workers.BackgroundImageWorker;
import com.formdev.flatlaf.util.UIScale;

public final class CurseForgeFileDependencyCard extends MD3Card {
    private final CurseForgeProjectFileSelectorDialog parent;
    private final CurseForgeFileDependency dependency;
    private final ModManagement instanceOrServer;

    public CurseForgeFileDependencyCard(CurseForgeProjectFileSelectorDialog parent, CurseForgeFileDependency dependency,
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
        CurseForgeProject mod = CurseForgeApi.getProjectById(dependency.modId);

        JLabel icon = new JLabel(Utils.getIconImage("/assets/image/no-icon.png"));
        icon.setVisible(false);

        JLabel title = new JLabel(mod.name);
        title.setFont(MD3Type.font(MD3Type.TITLE_SMALL, mod.name));
        title.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        title.setForeground(MD3Color.onSurface());

        JLabel summary = new JLabel();
        summary.setFont(MD3Type.font(MD3Type.BODY_SMALL, mod.summary));
        summary.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_SMALL);
        summary.setForeground(MD3Color.onSurfaceVariant());
        summary.setText(MD3Text.wrapToLines(summary.getFontMetrics(summary.getFont()), mod.summary,
                UIScale.scale(160), 3));

        JPanel text = new JPanel(new BorderLayout(0, MD3Spacing.scale(MD3Spacing.XS)));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(summary, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(MD3Spacing.scale(MD3Spacing.S), 0));
        body.setOpaque(false);
        body.add(icon, BorderLayout.WEST);
        body.add(text, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, MD3Spacing.scale(MD3Spacing.S), 0));
        buttons.setOpaque(false);
        MD3Button addButton = MD3Button.filled(GetText.tr("Add"));
        MD3Button viewButton = MD3Button.outlined(GetText.tr("View"));
        buttons.add(addButton);
        buttons.add(viewButton);

        addButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.forAddMod(mod));
            new CurseForgeProjectFileSelectorDialog(parent, mod, instanceOrServer).setVisible(true);
            parent.reloadDependenciesPanel();
        });

        viewButton.addActionListener(e -> OS.openWebBrowser(mod.getWebsiteUrl()));

        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        Optional<CurseForgeAttachment> attachment = mod.getLogo();
        attachment.filter(item -> item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty())
                .ifPresent(item -> new BackgroundImageWorker(icon, item.thumbnailUrl, 60, 60).execute());
    }
}
