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
package com.atlauncher.data;

import java.util.Optional;

import javax.annotation.Nullable;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.data.curseforge.CurseForgeAttachment;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthChannel;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;

/**
 * A newer file for an installed mod, and everything needed to both show it and install it.
 *
 * <p>
 * This is deliberately not a pair of ids: the update check already had to fetch the project and the
 * file to know there was an update at all, so carrying them means the install does not go back to
 * the API for what it was just told.
 */
public class ModUpdate {
    public final DisableableMod mod;
    public final ModPlatform platform;

    public final CurseForgeProject curseForgeProject;
    public final CurseForgeFile curseForgeFile;

    public final ModrinthProject modrinthProject;
    public final ModrinthVersion modrinthVersion;

    private ModUpdate(DisableableMod mod, ModPlatform platform, CurseForgeProject curseForgeProject,
            CurseForgeFile curseForgeFile, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion) {
        this.mod = mod;
        this.platform = platform;
        this.curseForgeProject = curseForgeProject;
        this.curseForgeFile = curseForgeFile;
        this.modrinthProject = modrinthProject;
        this.modrinthVersion = modrinthVersion;
    }

    public static ModUpdate forCurseForge(DisableableMod mod, CurseForgeProject project, CurseForgeFile file) {
        return new ModUpdate(mod, ModPlatform.CURSEFORGE, project, file, null, null);
    }

    public static ModUpdate forModrinth(DisableableMod mod, ModrinthProject project, ModrinthVersion version) {
        return new ModUpdate(mod, ModPlatform.MODRINTH, null, null, project, version);
    }

    public String getName() {
        if (platform == ModPlatform.CURSEFORGE && curseForgeProject != null) {
            return curseForgeProject.name;
        }

        if (platform == ModPlatform.MODRINTH && modrinthProject != null) {
            return modrinthProject.title;
        }

        return mod.getName();
    }

    /**
     * What is installed now.
     *
     * <p>
     * Falls back to the version the launcher recorded: a mod matched by fingerprint after being
     * dropped in by hand has platform ids but no file object behind them.
     */
    public String getCurrentVersion() {
        if (platform == ModPlatform.CURSEFORGE && mod.curseForgeFile != null) {
            return mod.curseForgeFile.displayName;
        }

        if (platform == ModPlatform.MODRINTH && mod.modrinthVersion != null) {
            return versionLabel(mod.modrinthVersion);
        }

        return mod.version == null || mod.version.isEmpty() ? mod.getFilename() : mod.version;
    }

    public String getNewVersion() {
        if (platform == ModPlatform.CURSEFORGE && curseForgeFile != null) {
            return curseForgeFile.displayName;
        }

        if (platform == ModPlatform.MODRINTH && modrinthVersion != null) {
            return versionLabel(modrinthVersion);
        }

        return "";
    }

    /**
     * The version number where the author gave one, since "0.6.0 to 0.6.3" says more than a display
     * name that repeats the mod's own title.
     */
    private static String versionLabel(ModrinthVersion version) {
        if (version.versionNumber != null && !version.versionNumber.isEmpty()) {
            return version.versionNumber;
        }

        return version.name;
    }

    @Nullable
    public String getIconUrl() {
        if (platform == ModPlatform.CURSEFORGE && curseForgeProject != null) {
            Optional<CurseForgeAttachment> logo = curseForgeProject.getLogo();

            return logo.isPresent() ? logo.get().thumbnailUrl : null;
        }

        if (platform == ModPlatform.MODRINTH && modrinthProject != null) {
            return modrinthProject.iconUrl;
        }

        return null;
    }

    /**
     * @return the release channel to badge the row with, or null for a stable release - which is
     *         most of them, and does not need saying
     */
    @Nullable
    public String getPrereleaseChannel() {
        if (platform == ModPlatform.CURSEFORGE && curseForgeFile != null) {
            if (curseForgeFile.isBetaType()) {
                return GetText.tr("Beta");
            }

            if (curseForgeFile.isAlphaType()) {
                return GetText.tr("Alpha");
            }

            return null;
        }

        if (platform == ModPlatform.MODRINTH && modrinthVersion != null) {
            if (modrinthVersion.versionType == ModrinthChannel.BETA) {
                return GetText.tr("Beta");
            }

            if (modrinthVersion.versionType == ModrinthChannel.ALPHA) {
                return GetText.tr("Alpha");
            }
        }

        return null;
    }

    public String getPlatformName() {
        return platform == ModPlatform.CURSEFORGE ? "CurseForge" : "Modrinth";
    }
}
