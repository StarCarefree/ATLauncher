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
package com.atlauncher.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.AddModRestriction;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeGameVersionLatestFiles;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.exceptions.InvalidMinecraftVersion;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.MinecraftManager;

/**
 * Whether a mod file will run in a given instance or server.
 *
 * <p>
 * The rules - which Minecraft versions count under {@link AddModRestriction}, which loader tags a
 * file may carry, and when NeoForge accepts Forge's files - had three near identical copies: the
 * update check in {@code DisableableMod}, and the file list in each of the two version selector
 * dialogs. They had already drifted: only the CurseForge selector knew about Sinytra Connector, so
 * a Forge instance with Connector installed was offered Fabric mods when browsing and then told
 * there was no update for them.
 *
 * <p>
 * Nothing here is user facing, which is just as well - {@code utils} is excluded from the gettext
 * extraction, so a string in this package would never reach a translator.
 */
public final class ModCompatibility {
    /** CurseForge puts loader names in the same list as the game versions. */
    public static final String FABRIC_TAG = "Fabric";
    public static final String FORGE_TAG = "Forge";
    public static final String NEOFORGE_TAG = "NeoForge";
    public static final String QUILT_TAG = "Quilt";

    /**
     * What {@code latestFilesIndexes} uses for a file that is not tied to a loader. The same entries
     * carry the loader as a number rather than as one of the tags above.
     */
    public static final int MOD_LOADER_ANY = 0;

    private ModCompatibility() {
    }

    /**
     * The Minecraft versions a file may declare and still be offered, or null when the setting puts
     * no restriction on it at all.
     *
     * <p>
     * A LAX lookup that throws is deliberately treated as no restriction rather than as no matches:
     * that is what the three call sites this replaces did, and refusing every file because the
     * version manifest could not be read would look exactly like the mod having no updates.
     */
    @Nullable
    public static List<String> minecraftVersionsToMatch(ModManagement instanceOrServer) {
        String minecraftVersion = instanceOrServer.getMinecraftVersion();

        if (App.settings.addModRestriction == AddModRestriction.STRICT) {
            return Collections.singletonList(minecraftVersion);
        }

        if (App.settings.addModRestriction == AddModRestriction.LAX) {
            try {
                return MinecraftManager.getMajorMinecraftVersions(minecraftVersion).stream().map(mv -> mv.id)
                        .collect(Collectors.toList());
            } catch (InvalidMinecraftVersion e) {
                LogManager.logStackTrace(e);
            }
        }

        return null;
    }

    /**
     * @param versionsToMatch what {@link #minecraftVersionsToMatch} answered; null means anything goes
     */
    public static boolean matchesMinecraftVersion(@Nullable List<String> gameVersions,
            @Nullable List<String> versionsToMatch) {
        if (versionsToMatch == null) {
            return true;
        }

        if (gameVersions == null) {
            return false;
        }

        return gameVersions.stream().anyMatch(versionsToMatch::contains);
    }

    /**
     * Whether NeoForge is close enough to Forge on this Minecraft version to run its files, which is
     * a remote config list rather than anything derivable.
     */
    public static boolean neoForgeUsesForgeFiles(ModManagement instanceOrServer) {
        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        if (loaderVersion == null || !loaderVersion.isNeoForge()) {
            return false;
        }

        List<String> compatibleVersions = ConfigManager
                .getConfigItem("loaders.neoforge.forgeCompatibleMinecraftVersions", new ArrayList<String>());

        return compatibleVersions.contains(instanceOrServer.getMinecraftVersion());
    }

    /**
     * Whether the loader tags CurseForge stamped on a file allow it to run here.
     *
     * @param projectHasFileForOwnLoader whether the project publishes anything for this instance's own
     *                                   loader. Sinytra Connector lets a Forge-like instance run Fabric
     *                                   mods, but only where the author has not shipped a Forge-like
     *                                   build of their own - that one is always the better choice.
     *                                   Ignored unless Connector is installed.
     */
    public static boolean matchesCurseForgeLoaderTags(@Nullable List<String> gameVersions,
            ModManagement instanceOrServer, boolean projectHasFileForOwnLoader) {
        if (gameVersions == null) {
            return true;
        }

        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        if (gameVersions.contains(FABRIC_TAG) && loaderVersion != null
                && (loaderVersion.isFabric() || loaderVersion.isLegacyFabric() || loaderVersion.isQuilt()
                        || acceptsFabricViaSinytra(instanceOrServer, projectHasFileForOwnLoader))) {
            return true;
        }

        if (gameVersions.contains(NEOFORGE_TAG) && loaderVersion != null && loaderVersion.isNeoForge()) {
            return true;
        }

        if (gameVersions.contains(FORGE_TAG) && loaderVersion != null
                && (loaderVersion.isForge() || neoForgeUsesForgeFiles(instanceOrServer))) {
            return true;
        }

        if (gameVersions.contains(QUILT_TAG) && loaderVersion != null && loaderVersion.isQuilt()) {
            return true;
        }

        // if there's no loaders, assume the mod is untagged so we should show it
        return !gameVersions.contains(FABRIC_TAG) && !gameVersions.contains(NEOFORGE_TAG)
                && !gameVersions.contains(FORGE_TAG) && !gameVersions.contains(QUILT_TAG);
    }

    /**
     * The same rule as {@link #matchesCurseForgeLoaderTags}, against the numeric loader that
     * {@code latestFilesIndexes} carries instead of the tags.
     */
    public static boolean matchesCurseForgeModLoaderId(@Nullable Integer modLoader,
            ModManagement instanceOrServer, boolean projectHasFileForOwnLoader) {
        if (modLoader == null || modLoader == MOD_LOADER_ANY) {
            // untagged, same as a file carrying none of the four tags
            return true;
        }

        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        if (loaderVersion == null) {
            return false;
        }

        if (modLoader == Constants.CURSEFORGE_FABRIC_MODLOADER_ID) {
            return loaderVersion.isFabric() || loaderVersion.isLegacyFabric() || loaderVersion.isQuilt()
                    || acceptsFabricViaSinytra(instanceOrServer, projectHasFileForOwnLoader);
        }

        if (modLoader == Constants.CURSEFORGE_NEOFORGE_MODLOADER_ID) {
            return loaderVersion.isNeoForge();
        }

        if (modLoader == Constants.CURSEFORGE_FORGE_MODLOADER_ID) {
            return loaderVersion.isForge() || neoForgeUsesForgeFiles(instanceOrServer);
        }

        if (modLoader == Constants.CURSEFORGE_QUILT_MODLOADER_ID) {
            return loaderVersion.isQuilt();
        }

        // Cauldron, LiteLoader and anything added since - nothing the launcher installs runs these
        return false;
    }

    private static boolean acceptsFabricViaSinytra(ModManagement instanceOrServer,
            boolean projectHasFileForOwnLoader) {
        return instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector() && !projectHasFileForOwnLoader;
    }

    /**
     * Whether the project publishes anything for this instance's own Forge-like loader - the answer
     * the Sinytra rule needs, worked out once for a project's whole file list.
     *
     * <p>
     * Answers false immediately when Connector is not installed, since that is the only thing that
     * asks, and false is the answer that changes nothing.
     */
    public static boolean hasFileForOwnLoader(ModManagement instanceOrServer,
            @Nullable List<CurseForgeFile> files) {
        if (files == null || !instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
            return false;
        }

        return files.stream().anyMatch(f -> declaresOwnLoader(instanceOrServer, f.gameVersions));
    }

    /** The same question against {@code latestFilesIndexes}, which names the loader by number. */
    public static boolean hasIndexForOwnLoader(ModManagement instanceOrServer,
            @Nullable List<CurseForgeGameVersionLatestFiles> indexes) {
        if (indexes == null || !instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
            return false;
        }

        boolean neoForge = isNeoForge(instanceOrServer);
        boolean acceptsForge = !neoForge || neoForgeUsesForgeFiles(instanceOrServer);

        return indexes.stream().anyMatch(index -> {
            if (neoForge && index.modLoader == Constants.CURSEFORGE_NEOFORGE_MODLOADER_ID) {
                return true;
            }

            return acceptsForge && index.modLoader == Constants.CURSEFORGE_FORGE_MODLOADER_ID;
        });
    }

    private static boolean declaresOwnLoader(ModManagement instanceOrServer, @Nullable List<String> gameVersions) {
        if (gameVersions == null) {
            return false;
        }

        if (isNeoForge(instanceOrServer)) {
            return gameVersions.contains(NEOFORGE_TAG)
                    || (neoForgeUsesForgeFiles(instanceOrServer) && gameVersions.contains(FORGE_TAG));
        }

        return gameVersions.contains(FORGE_TAG);
    }

    private static boolean isNeoForge(ModManagement instanceOrServer) {
        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        return loaderVersion != null && loaderVersion.isNeoForge();
    }

    /**
     * The loader names Modrinth knows this instance by, for the {@code loaders} facet.
     *
     * <p>
     * Quilt asks for Fabric as well because it runs Fabric mods, and NeoForge asks for Forge on the
     * versions the config says are compatible. An empty list means the instance has no loader and
     * the caller should send no facet at all.
     */
    public static List<String> modrinthLoaders(ModManagement instanceOrServer) {
        List<String> loaders = new ArrayList<>();
        LoaderVersion loaderVersion = instanceOrServer.getLoaderVersion();

        if (loaderVersion == null) {
            return loaders;
        }

        if (loaderVersion.isForge()) {
            loaders.add("forge");
        } else if (loaderVersion.isNeoForge()) {
            if (neoForgeUsesForgeFiles(instanceOrServer)) {
                loaders.add("forge");
            }

            loaders.add("neoforge");
        } else if (loaderVersion.isFabric()) {
            loaders.add("fabric");
        } else if (loaderVersion.isLegacyFabric()) {
            loaders.add("fabric");
            loaders.add("legacy-fabric");
        } else if (loaderVersion.isQuilt()) {
            loaders.add("fabric");
            loaders.add("quilt");
        } else if (loaderVersion.isPaper()) {
            loaders.add("paper");
        } else if (loaderVersion.isPurpur()) {
            loaders.add("purpur");
        }

        if (instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector() && !loaders.contains("fabric")) {
            loaders.add("fabric");
        }

        return loaders;
    }
}
