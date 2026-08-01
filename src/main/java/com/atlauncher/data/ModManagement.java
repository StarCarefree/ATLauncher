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
package com.atlauncher.data;

import java.awt.Window;
import java.nio.file.Path;
import java.util.List;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthDownloadMetadata;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.utils.FileUtils;

/**
 * Interface for mod management. Used by Instances as well as Servers to manage the mods/plugins for them.
 */
public interface ModManagement {
    public abstract Path getRoot();

    public abstract String getName();

    public abstract String getMinecraftVersion();

    public abstract LoaderVersion getLoaderVersion();

    public abstract boolean supportsPlugins();

    public abstract boolean isForgeLikeAndHasInstalledSinytraConnector();

    public abstract List<DisableableMod> getMods();

    public abstract void addMod(DisableableMod mod);

    public abstract void addMods(List<DisableableMod> modsToAdd);

    public abstract void removeMod(DisableableMod mod);

    /**
     * Removes several mods at once.
     *
     * <p>
     * Not a loop over {@link #removeMod}: that saves and posts a notification per mod, and only one
     * snackbar shows at a time with the rest queued behind it - so deleting twenty mods would leave
     * the user watching twenty messages go past. This saves once and says so once.
     *
     * <p>
     * It exists at all because the two places that delete in bulk had each grown their own copy of
     * the remove-then-delete-the-file dance, one of them using a different delete helper, and
     * neither going anywhere near {@link #removeMod}.
     */
    public default void removeMods(List<DisableableMod> modsToRemove) {
        if (modsToRemove == null || modsToRemove.isEmpty()) {
            return;
        }

        if (modsToRemove.size() == 1) {
            removeMod(modsToRemove.get(0));

            return;
        }

        for (DisableableMod mod : modsToRemove) {
            getMods().remove(mod);
            FileUtils.delete((mod.isDisabled() ? mod.getDisabledFile(this) : mod.getFile(this)).toPath(), true);
        }

        save();

        // #. {0} is the number of mods that were removed
        NotificationManager.show(GetText.tr("{0} Mods Removed", modsToRemove.size()));
    }

    public abstract void addFileFromCurseForge(CurseForgeProject mod, CurseForgeFile file, ProgressDialog<Void> dialog);

    public default void addFileFromModrinth(ModrinthProject project, ModrinthVersion version, ModrinthFile file,
            Type installType, ProgressDialog<Void> dialog) {
        addFileFromModrinth(project, version, file, installType, ModrinthDownloadMetadata.Reason.STANDALONE, null,
            dialog);
    }

    public default void addFileFromModrinth(ModrinthProject project, ModrinthVersion version, ModrinthFile file,
            Type installType, ModrinthDownloadMetadata.Reason downloadReason, ProgressDialog<Void> dialog) {
        addFileFromModrinth(project, version, file, installType, downloadReason, null, dialog);
    }

    public abstract void addFileFromModrinth(ModrinthProject project, ModrinthVersion version, ModrinthFile file,
            Type installType, ModrinthDownloadMetadata.Reason downloadReason, String dependentVersionId,
            ProgressDialog<Void> dialog);

    public abstract void scanMissingMods(Window parent);

    public abstract void save();
}
