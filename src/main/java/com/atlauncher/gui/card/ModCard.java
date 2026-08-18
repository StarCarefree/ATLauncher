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

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.json.Mod;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;

/**
 * One pack mod in the "view mods" list: the name, whether it is required, and a click through to
 * its site when it has one.
 */
public final class ModCard extends MD3Card {
    public final Mod mod;

    public ModCard(final Mod mod) {
        super(Variant.FILLED, new BorderLayout());

        this.mod = mod;

        setHoverElevation(true);
        setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.M));

        if (mod.hasWebsite()) {
            setClickable(true);
            addActionListener(e -> OS.openWebBrowser(mod.getWebsite()));
        }

        JLabel name = new JLabel(mod.getName());
        name.setFont(MD3Type.font(MD3Type.TITLE_SMALL, mod.getName()));
        name.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_SMALL);
        name.setForeground(MD3Color.onSurface());

        MD3Badge badge = mod.isOptional() ? MD3Badge.neutral(GetText.tr("Optional"))
                : MD3Badge.notable(GetText.tr("Required"));

        JPanel row = new JPanel(new BorderLayout(MD3Spacing.scale(MD3Spacing.S), 0));
        row.setOpaque(false);
        row.add(name, BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);

        add(row, BorderLayout.CENTER);
    }
}
