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
package com.atlauncher.managers;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.joda.time.format.ISODateTimeFormat;

import com.atlauncher.App;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.ModUpdate;
import com.atlauncher.data.Type;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeGameVersionLatestFiles;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.Hashing;
import com.atlauncher.utils.ModCompatibility;
import com.atlauncher.utils.ModrinthApi;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Which of an instance's mods have a newer file waiting, worked out in a handful of requests rather
 * than one per mod.
 *
 * <p>
 * The launcher used to answer this one mod at a time: {@code DisableableMod.checkForUpdate} fetches
 * a project's whole file list, and {@code EditModsDialog} called it in a loop, opening a progress
 * dialog and then a version selector for every mod that had one. On a large modpack that is
 * hundreds of round trips and hundreds of dialogs. Both platforms can answer for a whole instance
 * at once, so that is what this does:
 *
 * <ul>
 * <li>Modrinth resolves sha1 hashes to their projects and hands back the latest matching version
 * for each, in one POST.
 * <li>CurseForge's project payload carries {@code latestFilesIndexes} - the newest file per game
 * version and loader - so one POST for the projects and one for the winning files is enough.
 * </ul>
 *
 * <p>
 * Results are cached per instance in a {@link BehaviorSubject}, the same shape
 * {@link ModrinthModpackUpdateManager} uses for modpacks, so a row can show an "update available"
 * badge without asking again. Checking is only ever started by the user; nothing here runs at
 * startup.
 */
public class ModUpdateManager {
    /**
     * Keyed by the instance or server's directory. {@code ModManagement} has no id of its own -
     * {@code getUUID} is on {@code Instance} only - and the root is unique and stable.
     */
    private static final Map<Path, BehaviorSubject<List<ModUpdate>>> UPDATES = new ConcurrentHashMap<>();

    /**
     * How many ids to put in one bulk CurseForge request. Their API has a ceiling on this and a
     * modpack can easily carry several hundred mods.
     */
    private static final int CURSEFORGE_BATCH_SIZE = 200;

    private static BehaviorSubject<List<ModUpdate>> getSubject(ModManagement instanceOrServer) {
        UPDATES.putIfAbsent(instanceOrServer.getRoot(),
                BehaviorSubject.createDefault(Collections.<ModUpdate>emptyList()));

        return UPDATES.get(instanceOrServer.getRoot());
    }

    /**
     * The updates found the last time this instance was checked.
     *
     * <p>
     * Please do not cast to a behavior subject.
     */
    public static Observable<List<ModUpdate>> getObservable(ModManagement instanceOrServer) {
        return getSubject(instanceOrServer);
    }

    public static List<ModUpdate> getUpdates(ModManagement instanceOrServer) {
        return getSubject(instanceOrServer).getValue();
    }

    /** Whether this exact mod has an update waiting, for a row that wants to badge itself. */
    public static boolean hasUpdate(ModManagement instanceOrServer, DisableableMod mod) {
        return getUpdates(instanceOrServer).stream().anyMatch(u -> u.mod == mod);
    }

    /**
     * Drops one mod from the cached results, for when it has just been updated.
     *
     * <p>
     * Cheaper and less surprising than re-checking: the file on disk is now the one the update
     * pointed at, so the row should stop offering it immediately rather than at the next check.
     */
    public static void markUpdated(ModManagement instanceOrServer, DisableableMod mod) {
        BehaviorSubject<List<ModUpdate>> subject = getSubject(instanceOrServer);

        subject.onNext(subject.getValue().stream().filter(u -> u.mod != mod).collect(Collectors.toList()));
    }

    public static void invalidate(ModManagement instanceOrServer) {
        UPDATES.remove(instanceOrServer.getRoot());
    }

    /**
     * Asks both platforms what is newer, for every mod that came from one of them.
     *
     * <p>
     * Always checks the whole instance rather than a selection: a bulk request costs the same for
     * ten mods as for two hundred, and a cache holding "the mods you happened to have ticked" is
     * one nothing else can read. Callers wanting a subset filter the result.
     *
     * <p>
     * Blocking - run it on a worker thread. Never throws; a platform that fails is logged and
     * contributes nothing, so one being down does not hide the other's updates.
     */
    public static List<ModUpdate> checkForUpdates(ModManagement instanceOrServer) {
        Analytics.trackEvent(AnalyticsEvent.simpleEvent("mod_update_check"));
        PerformanceManager.start();

        List<ModUpdate> updates = new ArrayList<>();

        try {
            List<DisableableMod> curseForgeMods = new ArrayList<>();
            List<DisableableMod> modrinthMods = new ArrayList<>();

            for (DisableableMod mod : new ArrayList<>(instanceOrServer.getMods())) {
                if (!mod.isUpdatable()) {
                    continue;
                }

                if (platformFor(mod) == ModPlatform.CURSEFORGE) {
                    curseForgeMods.add(mod);
                } else {
                    modrinthMods.add(mod);
                }
            }

            LogManager.info(String.format("Checking %d mod(s) for updates in %s", curseForgeMods.size()
                    + modrinthMods.size(), instanceOrServer.getName()));

            updates.addAll(checkCurseForge(instanceOrServer, curseForgeMods));
            updates.addAll(checkModrinth(instanceOrServer, modrinthMods));
        } catch (Exception e) {
            LogManager.logStackTrace("Error checking mods for updates", e);
        }

        // the order mods appear in the list, so the panel reads the way the mod list does
        updates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        getSubject(instanceOrServer).onNext(updates);
        PerformanceManager.end();

        return updates;
    }

    /**
     * Which platform to ask about a mod that exists on both, matching what the single mod check has
     * always done: whichever one it came from, and the default when it came from both.
     */
    private static ModPlatform platformFor(DisableableMod mod) {
        if (mod.isFromCurseForge() && (!mod.isFromModrinth()
                || App.settings.defaultModPlatform == ModPlatform.CURSEFORGE)) {
            return ModPlatform.CURSEFORGE;
        }

        return ModPlatform.MODRINTH;
    }

    // ---------------------------------------------------------------- CurseForge

    private static List<ModUpdate> checkCurseForge(ModManagement instanceOrServer, List<DisableableMod> mods) {
        List<ModUpdate> updates = new ArrayList<>();

        if (mods.isEmpty()) {
            return updates;
        }

        try {
            Map<Integer, CurseForgeProject> projects = getProjects(mods.stream()
                    .map(m -> m.curseForgeProjectId).distinct().collect(Collectors.toList()));

            // fileId -> the mods waiting on it, so one bulk file lookup serves them all
            Map<Integer, List<DisableableMod>> wanted = new LinkedHashMap<>();
            Map<DisableableMod, CurseForgeProject> resolved = new HashMap<>();

            for (DisableableMod mod : mods) {
                CurseForgeProject project = projects.get(mod.curseForgeProjectId);

                if (project == null) {
                    continue;
                }

                Integer fileId = newestCurseForgeFileId(instanceOrServer, mod, project);

                if (fileId == null) {
                    continue;
                }

                resolved.put(mod, project);

                List<DisableableMod> waiting = wanted.get(fileId);

                if (waiting == null) {
                    waiting = new ArrayList<>();
                    wanted.put(fileId, waiting);
                }

                waiting.add(mod);
            }

            if (wanted.isEmpty()) {
                return updates;
            }

            Map<Integer, CurseForgeFile> files = getFiles(new ArrayList<>(wanted.keySet()));

            for (Map.Entry<Integer, List<DisableableMod>> entry : wanted.entrySet()) {
                CurseForgeFile file = files.get(entry.getKey());

                if (file == null) {
                    continue;
                }

                for (DisableableMod mod : entry.getValue()) {
                    updates.add(ModUpdate.forCurseForge(mod, resolved.get(mod), file));
                }
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error checking CurseForge for mod updates", e);
        }

        return updates;
    }

    /**
     * The newest file id this project offers that runs here and is newer than what is installed, or
     * null when there is nothing to offer.
     *
     * <p>
     * {@code latestFilesIndexes} is the newest file per game version and loader, which is exactly
     * the question, and it arrives with the project rather than needing the project's whole file
     * list. Older responses have carried nothing there, so {@code latestFiles} is a fallback - it
     * is a much shorter list and may not reach back to this instance's Minecraft version, but a
     * missed update reads better than a crash.
     */
    @Nullable
    private static Integer newestCurseForgeFileId(ModManagement instanceOrServer, DisableableMod mod,
            CurseForgeProject project) {
        List<String> versionsToMatch = ModCompatibility.minecraftVersionsToMatch(instanceOrServer);
        boolean skipVersionCheck = isVersionAgnostic(mod);
        Integer installed = mod.curseForgeFileId;

        if (project.latestFilesIndexes != null && !project.latestFilesIndexes.isEmpty()) {
            boolean hasOwnLoaderFile = ModCompatibility.hasIndexForOwnLoader(instanceOrServer,
                    project.latestFilesIndexes);
            Integer newest = null;

            for (CurseForgeGameVersionLatestFiles index : project.latestFilesIndexes) {
                if (!skipVersionCheck && !ModCompatibility.matchesMinecraftVersion(
                        Collections.singletonList(index.gameVersion), versionsToMatch)) {
                    continue;
                }

                if (!ModCompatibility.matchesCurseForgeModLoaderId(index.modLoader, instanceOrServer,
                        hasOwnLoaderFile)) {
                    continue;
                }

                if (!allowsReleaseType(mod, index.releaseType)) {
                    continue;
                }

                if (index.fileId > installed && (newest == null || index.fileId > newest)) {
                    newest = index.fileId;
                }
            }

            return newest;
        }

        if (project.latestFiles == null) {
            return null;
        }

        boolean hasOwnLoaderFile = ModCompatibility.hasFileForOwnLoader(instanceOrServer, project.latestFiles);
        Integer newest = null;

        for (CurseForgeFile file : project.latestFiles) {
            if (!skipVersionCheck && !ModCompatibility.matchesMinecraftVersion(file.gameVersions, versionsToMatch)) {
                continue;
            }

            if (!ModCompatibility.matchesCurseForgeLoaderTags(file.gameVersions, instanceOrServer, hasOwnLoaderFile)) {
                continue;
            }

            if (file.releaseType != null && !allowsReleaseType(mod, file.releaseType)) {
                continue;
            }

            if (file.id > installed && (newest == null || file.id > newest)) {
                newest = file.id;
            }
        }

        return newest;
    }

    /**
     * The alpha/beta rule the modpack update check already uses: a mod sitting on a stable release
     * is not moved onto a pre-release behind the user's back, but one already on a pre-release
     * keeps getting them.
     */
    private static boolean allowsReleaseType(DisableableMod mod, int releaseType) {
        if (App.settings.allowCurseForgeAlphaBetaFiles || mod.curseForgeFile == null) {
            return true;
        }

        if (mod.curseForgeFile.isReleaseType()) {
            return releaseType == 1;
        }

        if (mod.curseForgeFile.isBetaType()) {
            return releaseType == 1 || releaseType == 2;
        }

        return true;
    }

    private static Map<Integer, CurseForgeProject> getProjects(List<Integer> projectIds) {
        Map<Integer, CurseForgeProject> projects = new HashMap<>();

        for (List<Integer> batch : batches(projectIds)) {
            int[] ids = new int[batch.size()];

            for (int i = 0; i < batch.size(); i++) {
                ids[i] = batch.get(i);
            }

            Map<Integer, CurseForgeProject> found = CurseForgeApi.getProjectsAsMap(ids);

            if (found != null) {
                projects.putAll(found);
            }
        }

        return projects;
    }

    private static Map<Integer, CurseForgeFile> getFiles(List<Integer> fileIds) {
        Map<Integer, CurseForgeFile> files = new HashMap<>();

        for (List<Integer> batch : batches(fileIds)) {
            int[] ids = new int[batch.size()];

            for (int i = 0; i < batch.size(); i++) {
                ids[i] = batch.get(i);
            }

            List<CurseForgeFile> found = CurseForgeApi.getFiles(ids);

            if (found != null) {
                found.forEach(f -> files.put(f.id, f));
            }
        }

        return files;
    }

    private static List<List<Integer>> batches(List<Integer> ids) {
        List<List<Integer>> batches = new ArrayList<>();

        for (int i = 0; i < ids.size(); i += CURSEFORGE_BATCH_SIZE) {
            batches.add(ids.subList(i, Math.min(ids.size(), i + CURSEFORGE_BATCH_SIZE)));
        }

        return batches;
    }

    // ------------------------------------------------------------------ Modrinth

    private static List<ModUpdate> checkModrinth(ModManagement instanceOrServer, List<DisableableMod> mods) {
        List<ModUpdate> updates = new ArrayList<>();

        if (mods.isEmpty()) {
            return updates;
        }

        // one request per distinct filter, which in practice is one for the mods and, if the
        // instance has any, one for the data packs - they are matched on a loader of their own
        Map<String, List<DisableableMod>> byFilter = new LinkedHashMap<>();

        for (DisableableMod mod : mods) {
            String key = String.valueOf(modrinthLoadersFor(instanceOrServer, mod))
                    + String.valueOf(modrinthGameVersionsFor(instanceOrServer, mod));

            List<DisableableMod> group = byFilter.get(key);

            if (group == null) {
                group = new ArrayList<>();
                byFilter.put(key, group);
            }

            group.add(mod);
        }

        for (List<DisableableMod> group : byFilter.values()) {
            try {
                updates.addAll(checkModrinthGroup(instanceOrServer, group));
            } catch (Exception e) {
                LogManager.logStackTrace("Error checking Modrinth for mod updates", e);
            }
        }

        return updates;
    }

    private static List<ModUpdate> checkModrinthGroup(ModManagement instanceOrServer, List<DisableableMod> mods) {
        List<ModUpdate> updates = new ArrayList<>();
        Map<String, DisableableMod> byHash = new LinkedHashMap<>();

        for (DisableableMod mod : mods) {
            String sha1 = sha1For(instanceOrServer, mod);

            if (sha1 != null) {
                byHash.put(sha1.toLowerCase(Locale.ENGLISH), mod);
            }
        }

        if (byHash.isEmpty()) {
            return updates;
        }

        DisableableMod first = mods.get(0);
        Map<String, ModrinthVersion> latest = ModrinthApi.getLatestVersionsFromHashes(
                byHash.keySet().toArray(new String[0]), modrinthLoadersFor(instanceOrServer, first),
                modrinthGameVersionsFor(instanceOrServer, first));

        for (Map.Entry<String, ModrinthVersion> entry : latest.entrySet()) {
            DisableableMod mod = byHash.get(entry.getKey().toLowerCase(Locale.ENGLISH));
            ModrinthVersion version = entry.getValue();

            if (mod == null || version == null || !isNewer(version, mod.modrinthVersion)) {
                continue;
            }

            updates.add(ModUpdate.forModrinth(mod, mod.modrinthProject, version));
        }

        return updates;
    }

    /**
     * Modrinth answers with the latest version matching the filter, which is the installed one when
     * there is nothing newer. The id check catches that; the date is a second opinion for the case
     * where a project has republished under a new id.
     */
    private static boolean isNewer(ModrinthVersion candidate, @Nullable ModrinthVersion installed) {
        if (installed == null) {
            return true;
        }

        if (candidate.id != null && candidate.id.equals(installed.id)) {
            return false;
        }

        if (candidate.datePublished == null || installed.datePublished == null) {
            return true;
        }

        return ISODateTimeFormat.dateTimeParser().parseDateTime(candidate.datePublished).minusSeconds(1)
                .isAfter(ISODateTimeFormat.dateTimeParser().parseDateTime(installed.datePublished));
    }

    /**
     * The sha1 of what is installed. Taken from the recorded version where possible - the launcher
     * already stores the hashes Modrinth gave it, so a whole instance can be checked without
     * reading a single file back off disk.
     */
    @Nullable
    private static String sha1For(ModManagement instanceOrServer, DisableableMod mod) {
        if (mod.modrinthVersion != null && mod.modrinthVersion.files != null
                && !mod.modrinthVersion.files.isEmpty()) {
            for (ModrinthFile file : mod.modrinthVersion.files) {
                if (file.hashes != null && file.hashes.containsKey("sha1")) {
                    return file.hashes.get("sha1");
                }
            }
        }

        try {
            File onDisk = mod.getFile(instanceOrServer.getRoot(), instanceOrServer.getMinecraftVersion());

            if (onDisk != null && onDisk.exists()) {
                return Hashing.sha1(onDisk.toPath()).toString();
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error hashing " + mod.getFilename(), e);
        }

        return null;
    }

    /**
     * Which loaders a file for this mod may declare.
     *
     * <p>
     * A data pack is matched on the {@code datapack} loader rather than the instance's, and things
     * that are not loader specific at all - resource packs, shaders - are matched on nothing, since
     * asking Modrinth for a Fabric resource pack returns nothing at all.
     */
    private static List<String> modrinthLoadersFor(ModManagement instanceOrServer, DisableableMod mod) {
        if (mod.type == Type.datapack) {
            return Collections.singletonList("datapack");
        }

        if (mod.type == Type.resourcepack || mod.type == Type.shaderpack || mod.type == Type.texturepack) {
            return Collections.emptyList();
        }

        return ModCompatibility.modrinthLoaders(instanceOrServer);
    }

    /**
     * Resource packs and shaders are left unfiltered by Minecraft version for the same reason the
     * CurseForge file list skips them: they carry version ranges the launcher cannot reason about,
     * and filtering strictly leaves the user with nothing.
     */
    @Nullable
    private static List<String> modrinthGameVersionsFor(ModManagement instanceOrServer, DisableableMod mod) {
        if (mod.type == Type.resourcepack || mod.type == Type.shaderpack || mod.type == Type.texturepack) {
            return null;
        }

        return ModCompatibility.minecraftVersionsToMatch(instanceOrServer);
    }

    /** Whether the Minecraft version filter should be skipped for this kind of file. */
    private static boolean isVersionAgnostic(DisableableMod mod) {
        return mod.type == Type.resourcepack || mod.type == Type.texturepack || mod.type == Type.plugins;
    }
}
