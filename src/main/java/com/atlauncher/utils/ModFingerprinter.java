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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.atlauncher.App;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.curseforge.CurseForgeFingerprint;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.FabricMod;
import com.atlauncher.data.minecraft.MCMod;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.managers.LogManager;

/**
 * Works out which project a mod file on disk belongs to, by hashing it and asking the platforms.
 *
 * <p>
 * This is what gives a hand-dropped jar a name, a description and - the part that matters - the
 * project and file ids that make updating it possible at all. CurseForge matches on murmur2 of the
 * file with whitespace stripped, Modrinth on sha1, and both answer in bulk.
 *
 * <p>
 * There were three copies of this: scanning an instance for mods added outside the launcher,
 * dropping files onto the mod manager, and the mod manager's "Refresh Metadata" button - which is
 * where the author left {@code // TODO: Generalise this, cause fuck me I've copy pasted this like
 * 10 times now}. They had drifted in ways nobody would have chosen:
 *
 * <ul>
 * <li>Refresh Metadata hashed the <em>enabled</em> path for every mod, so refreshing a disabled mod
 * hashed a file that is not there and matched nothing.
 * <li>The drag and drop handler did the same for its murmur hash but not for its sha1, so a file
 * dropped onto the disabled column was found on Modrinth and not on CurseForge.
 * <li>Refresh Metadata overwrote the name with Modrinth's whatever {@code defaultModPlatform} said,
 * while the other two honoured it.
 * </ul>
 *
 * <p>
 * All three now behave the way the majority did, which is also the way that is right.
 */
public final class ModFingerprinter {
    /** CurseForge's "approved and publicly listed". Anything else is not offered to users. */
    private static final int CURSEFORGE_APPROVED = 4;

    private ModFingerprinter() {
    }

    /**
     * @param onlyUnidentified skip mods that already carry the platform's ids. True when the mods
     *                         have just been discovered, false when the user has asked for the
     *                         metadata to be looked up again
     */
    public static void identify(List<DisableableMod> mods, ModManagement instanceOrServer,
            boolean onlyUnidentified) {
        if (mods == null || mods.isEmpty()) {
            return;
        }

        if (!App.settings.dontCheckModsOnCurseForge) {
            identifyOnCurseForge(mods, instanceOrServer, onlyUnidentified);
        }

        if (!App.settings.dontCheckModsOnModrinth) {
            identifyOnModrinth(mods, instanceOrServer, onlyUnidentified);
        }
    }

    private static void identifyOnCurseForge(List<DisableableMod> mods, ModManagement instanceOrServer,
            boolean onlyUnidentified) {
        Map<Long, DisableableMod> murmurHashes = new HashMap<>();

        for (DisableableMod mod : mods) {
            if (onlyUnidentified && (mod.curseForgeProject != null || mod.curseForgeFile != null)) {
                continue;
            }

            File file = fileOf(mod, instanceOrServer);

            if (file == null) {
                continue;
            }

            try {
                murmurHashes.put(Hashing.murmur(file.toPath()), mod);
            } catch (Exception e) {
                LogManager.logStackTrace(e);
            }
        }

        if (murmurHashes.isEmpty()) {
            return;
        }

        CurseForgeFingerprint response = CurseForgeApi.checkFingerprints(murmurHashes.keySet().toArray(new Long[0]));

        if (response == null || response.exactMatches == null) {
            return;
        }

        int[] projectIds = response.exactMatches.stream().mapToInt(em -> em.id).toArray();

        if (projectIds.length == 0) {
            return;
        }

        Map<Integer, CurseForgeProject> projects = CurseForgeApi.getProjectsAsMap(projectIds);

        if (projects == null) {
            return;
        }

        response.exactMatches.stream()
                .filter(em -> em != null && em.file != null && murmurHashes.containsKey(em.file.packageFingerprint))
                .forEach(match -> {
                    DisableableMod mod = murmurHashes.get(match.file.packageFingerprint);
                    CurseForgeProject project = projects.get(match.id);

                    if (project == null) {
                        return;
                    }

                    if (project.status == CURSEFORGE_APPROVED) {
                        mod.curseForgeProjectId = match.id;
                        mod.curseForgeFile = match.file;
                        mod.curseForgeFileId = match.file.id;
                        mod.curseForgeProject = project;
                        mod.name = project.name;
                        mod.description = project.summary;

                        LogManager.debug("Found matching mod from CurseForge called " + mod.curseForgeFile.displayName);

                        return;
                    }

                    // not approved, so it is not something to point a user at - drop what we know
                    // and fall back to whatever the jar says about itself
                    mod.curseForgeProjectId = null;
                    mod.curseForgeFile = null;
                    mod.curseForgeFileId = null;
                    mod.curseForgeProject = null;

                    nameFromJar(mod, instanceOrServer);
                });
    }

    private static void identifyOnModrinth(List<DisableableMod> mods, ModManagement instanceOrServer,
            boolean onlyUnidentified) {
        Map<String, DisableableMod> sha1Hashes = new HashMap<>();

        for (DisableableMod mod : mods) {
            if (onlyUnidentified && (mod.modrinthProject != null || mod.modrinthVersion != null)) {
                continue;
            }

            File file = fileOf(mod, instanceOrServer);

            if (file == null) {
                continue;
            }

            try {
                sha1Hashes.put(Hashing.sha1(file.toPath()).toString(), mod);
            } catch (Exception e) {
                LogManager.logStackTrace(e);
            }
        }

        if (sha1Hashes.isEmpty()) {
            return;
        }

        Map<String, ModrinthVersion> versions = ModrinthApi
                .getVersionsFromSha1Hashes(sha1Hashes.keySet().toArray(new String[0]));

        if (versions == null || versions.isEmpty()) {
            return;
        }

        String[] projectIds = versions.values().stream().map(mv -> mv.projectId).toArray(String[]::new);

        if (projectIds.length == 0) {
            return;
        }

        Map<String, ModrinthProject> projects = ModrinthApi.getProjectsAsMap(projectIds);

        if (projects == null) {
            return;
        }

        for (Map.Entry<String, ModrinthVersion> entry : versions.entrySet()) {
            ModrinthVersion version = entry.getValue();
            ModrinthProject project = projects.get(version.projectId);
            DisableableMod mod = sha1Hashes.get(entry.getKey());

            if (project == null || mod == null) {
                continue;
            }

            mod.modrinthProject = project;
            mod.modrinthVersion = version;

            // a mod on both platforms keeps the name of whichever one the user browses by, rather
            // than of whichever lookup happened to run last
            if (!mod.isFromCurseForge() || App.settings.defaultModPlatform == ModPlatform.MODRINTH) {
                mod.name = project.title;
                mod.description = project.description;
            }

            LogManager.debug(String.format("Found matching mod from Modrinth called %s with file %s", project.title,
                    version.name));
        }
    }

    /**
     * The jar's own idea of what it is - {@code mcmod.info} or {@code fabric.mod.json} - for a mod
     * no platform will claim.
     */
    private static void nameFromJar(DisableableMod mod, ModManagement instanceOrServer) {
        File file = fileOf(mod, instanceOrServer);

        if (file == null) {
            return;
        }

        MCMod mcMod = Utils.getMCModForFile(file);

        if (mcMod != null) {
            mod.name = Optional.ofNullable(mcMod.name).orElse(file.getName());
            mod.description = mcMod.description;

            return;
        }

        FabricMod fabricMod = Utils.getFabricModForFile(file);

        if (fabricMod != null) {
            mod.name = Optional.ofNullable(fabricMod.name).orElse(file.getName());
            mod.description = fabricMod.description;
        }
    }

    /**
     * Where this mod's file actually is - which for a disabled mod is {@code disabledmods}, not the
     * folder its type would put it in. Two of the three call sites this replaces forgot that.
     */
    @Nullable
    private static File fileOf(DisableableMod mod, ModManagement instanceOrServer) {
        File file = mod.isDisabled() ? mod.getDisabledFile(instanceOrServer)
                : mod.getFile(instanceOrServer.getRoot(), instanceOrServer.getMinecraftVersion());

        return file != null && file.exists() ? file : null;
    }
}
