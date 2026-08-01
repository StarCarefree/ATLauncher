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
package com.atlauncher.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Type;
import com.atlauncher.gui.md3.container.MD3Badge;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

import com.formdev.flatlaf.util.UIScale;

/**
 * One line of the mod manager: what kind of thing it is, its tick, and what version of it is
 * installed from where.
 *
 * <p>
 * The list used to be bare {@link javax.swing.JCheckBox}es carrying nothing but a name. Everything
 * else the launcher knows about a mod - the version on disk, which platform it came from, whether
 * it is a mod at all or a shader pack sitting in the same list - was either in a tooltip or nowhere.
 *
 * <p>
 * The tick is still a {@link ModsJCheckBox} rather than something new: it carries the pack author's
 * colour, the description tooltip and the whole right click menu, and it is what
 * {@link com.atlauncher.gui.handlers.ModsJCheckBoxTransferHandler} and the render tests look for.
 *
 * <p>
 * The glyph is drawn rather than fetched. A project icon per row would be prettier and would also
 * mean a few hundred image requests the moment a large modpack's mod list is opened; the type is
 * the thing the list could not tell you before, and it costs nothing to paint.
 */
public final class ModRow extends JPanel {
    private static final int GLYPH_SIZE = 18;

    private final ModsJCheckBox checkBox;

    public ModRow(ModsJCheckBox checkBox, boolean hasUpdate) {
        super(new BorderLayout(UIScale.scale(MD3Spacing.S), 0));

        this.checkBox = checkBox;

        setOpaque(false);
        setBorder(MD3Spacing.border(0, MD3Spacing.XS, 0, MD3Spacing.S));

        add(buildGlyph(checkBox.getDisableableMod()), BorderLayout.WEST);
        add(checkBox, BorderLayout.CENTER);
        add(buildTrailing(checkBox.getDisableableMod(), hasUpdate), BorderLayout.EAST);
    }

    public ModsJCheckBox getCheckBox() {
        return checkBox;
    }

    /**
     * As wide as the list and no taller than it needs.
     *
     * <p>
     * The lists are {@link javax.swing.BoxLayout} panels inside a scroll pane, and
     * {@code ViewportLayout} grows a view smaller than its viewport to fill it - so with a handful
     * of mods in a tall window the box layout has surplus height to hand out, and hands it to
     * whichever children will take it. Without this cap four mods came out spread over the whole
     * column, each row a hundred pixels tall with its version stranded at the top.
     */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static JLabel buildGlyph(DisableableMod mod) {
        JLabel glyph = new JLabel(MD3Icon.of(glyphFor(mod.type), GLYPH_SIZE).withRole(MD3Color.ON_SURFACE_VARIANT));
        glyph.setToolTipText(nameFor(mod.type));

        return glyph;
    }

    private static MD3Icon.Painter glyphFor(Type type) {
        if (type == null) {
            return MD3Icons.PACKAGE;
        }

        switch (type) {
            case resourcepack:
            case texturepack:
                return MD3Icons.GRID_VIEW;
            case shaderpack:
                return MD3Icons.VISIBILITY;
            case datapack:
                return MD3Icons.FOLDER;
            default:
                return MD3Icons.PACKAGE;
        }
    }

    /** Only the kinds a user can meaningfully tell apart; everything else is just a mod. */
    public static String nameFor(Type type) {
        if (type == null) {
            return GetText.tr("Mods");
        }

        switch (type) {
            case resourcepack:
            case texturepack:
                return GetText.tr("Resource Packs");
            case shaderpack:
                return GetText.tr("Shaders");
            case datapack:
                return GetText.tr("Data Packs");
            case plugins:
                return GetText.tr("Plugins");
            default:
                return GetText.tr("Mods");
        }
    }

    /**
     * The version on disk and where it came from, on one muted line, plus a badge for anything with
     * an update waiting.
     */
    private static JPanel buildTrailing(DisableableMod mod, boolean hasUpdate) {
        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(MD3Spacing.XS), 0));
        trailing.setOpaque(false);

        String provenance = provenanceOf(mod);

        if (!provenance.isEmpty()) {
            JLabel label = new JLabel(provenance, SwingConstants.RIGHT);
            label.setFont(MD3Type.font(MD3Type.LABEL_SMALL, provenance));
            label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_SMALL);
            label.setForeground(MD3Color.onSurfaceVariant());

            trailing.add(label);
        }

        if (hasUpdate) {
            trailing.add(MD3Badge.notable(GetText.tr("Update")));
        }

        return trailing;
    }

    private static String provenanceOf(DisableableMod mod) {
        String version = mod.version == null || mod.version.isEmpty() || "Unknown".equals(mod.version) ? ""
                : mod.version;
        String source = sourceOf(mod);

        if (version.isEmpty()) {
            return source;
        }

        return source.isEmpty() ? version : version + " · " + source;
    }

    private static String sourceOf(DisableableMod mod) {
        if (mod.isFromCurseForge() && mod.isFromModrinth()) {
            return "CurseForge · Modrinth";
        }

        if (mod.isFromCurseForge()) {
            return "CurseForge";
        }

        if (mod.isFromModrinth()) {
            return "Modrinth";
        }

        return "";
    }
}
