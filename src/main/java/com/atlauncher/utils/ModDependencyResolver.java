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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.atlauncher.constants.Constants;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeFileDependency;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthDependency;
import com.atlauncher.data.modrinth.ModrinthDependencyType;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;

/**
 * Works out everything a mod needs before it will run, including what those things need.
 *
 * <p>
 * The version selector dialogs already showed the required dependencies a mod was missing, as a
 * card each with an Add button on it - so installing a mod with three dependencies meant three more
 * dialogs, and a dependency of a dependency was never mentioned at all, because the panel is
 * rebuilt from the parent mod's own dependency list and nothing else.
 *
 * <p>
 * This walks the graph instead: breadth first, skipping anything already installed or already
 * queued, and stopping at {@link #MAX_DEPTH}. The depth cap is a backstop against a cycle the
 * visited set somehow misses, not a real limit - three levels is deeper than any mod goes.
 *
 * <p>
 * Nothing here is user facing; the dialogs do the installing, with their own progress and their own
 * strings. {@code utils} is excluded from the gettext extraction, so it could not say anything to
 * the user anyway.
 */
public final class ModDependencyResolver {
    private static final int MAX_DEPTH = 3;

    private ModDependencyResolver() {
    }

    /**
     * Everything to install for these Modrinth dependencies, in the order to install it.
     *
     * @param required the direct required dependencies, already filtered by the caller
     */
    public static List<Pair<ModrinthProject, ModrinthVersion>> resolveModrinth(ModManagement instanceOrServer,
            List<ModrinthDependency> required) {
        List<Pair<ModrinthProject, ModrinthVersion>> resolved = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> frontier = new LinkedHashSet<>();

        for (ModrinthDependency dependency : required) {
            if (dependency.projectId != null) {
                frontier.add(dependency.projectId);
            }
        }

        for (int depth = 0; depth < MAX_DEPTH && !frontier.isEmpty(); depth++) {
            Set<String> next = new LinkedHashSet<>();

            for (String projectId : frontier) {
                if (!visited.add(projectId) || isSatisfiedOnModrinth(instanceOrServer, projectId)) {
                    continue;
                }

                ModrinthProject project = ModrinthApi.getProject(projectId);
                ModrinthVersion version = newestModrinthVersion(instanceOrServer, projectId);

                if (project == null || version == null) {
                    continue;
                }

                resolved.add(new Pair<>(project, version));

                if (version.dependencies == null) {
                    continue;
                }

                for (ModrinthDependency dependency : version.dependencies) {
                    if (dependency.dependencyType == ModrinthDependencyType.REQUIRED && dependency.projectId != null
                            && !visited.contains(dependency.projectId)) {
                        next.add(dependency.projectId);
                    }
                }
            }

            frontier = next;
        }

        return resolved;
    }

    /** Everything to install for these CurseForge dependencies, in the order to install it. */
    public static List<Pair<CurseForgeProject, CurseForgeFile>> resolveCurseForge(ModManagement instanceOrServer,
            List<CurseForgeFileDependency> required) {
        List<Pair<CurseForgeProject, CurseForgeFile>> resolved = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Set<Integer> frontier = new LinkedHashSet<>();

        for (CurseForgeFileDependency dependency : required) {
            frontier.add(dependency.modId);
        }

        for (int depth = 0; depth < MAX_DEPTH && !frontier.isEmpty(); depth++) {
            Set<Integer> next = new LinkedHashSet<>();

            for (Integer projectId : frontier) {
                if (!visited.add(projectId) || isSatisfiedOnCurseForge(instanceOrServer, projectId)) {
                    continue;
                }

                CurseForgeProject project = CurseForgeApi.getProjectById(projectId);
                CurseForgeFile file = newestCurseForgeFile(instanceOrServer, projectId);

                if (project == null || file == null) {
                    continue;
                }

                resolved.add(new Pair<>(project, file));

                if (file.dependencies == null) {
                    continue;
                }

                for (CurseForgeFileDependency dependency : file.dependencies) {
                    if (dependency.isRequired() && !visited.contains(dependency.modId)) {
                        next.add(dependency.modId);
                    }
                }
            }

            frontier = next;
        }

        return resolved;
    }

    private static ModrinthVersion newestModrinthVersion(ModManagement instanceOrServer, String projectId) {
        List<ModrinthVersion> versions = ModrinthApi.getVersions(projectId, instanceOrServer.getMinecraftVersion(),
                instanceOrServer.getLoaderVersion());

        if (versions == null || versions.isEmpty()) {
            return null;
        }

        List<String> versionsToMatch = ModCompatibility.minecraftVersionsToMatch(instanceOrServer);

        return versions.stream()
                .filter(v -> v.datePublished != null)
                .filter(v -> ModCompatibility.matchesMinecraftVersion(v.gameVersions, versionsToMatch))
                .sorted(Comparator.comparing((ModrinthVersion v) -> v.datePublished).reversed())
                .findFirst().orElse(null);
    }

    private static CurseForgeFile newestCurseForgeFile(ModManagement instanceOrServer, int projectId) {
        List<CurseForgeFile> files = CurseForgeApi.getFilesForProject(projectId);

        if (files == null || files.isEmpty()) {
            return null;
        }

        List<String> versionsToMatch = ModCompatibility.minecraftVersionsToMatch(instanceOrServer);
        boolean hasOwnLoaderFile = ModCompatibility.hasFileForOwnLoader(instanceOrServer, files);

        return files.stream()
                .filter(f -> ModCompatibility.matchesMinecraftVersion(f.gameVersions, versionsToMatch))
                .filter(f -> ModCompatibility.matchesCurseForgeLoaderTags(f.gameVersions, instanceOrServer,
                        hasOwnLoaderFile))
                .sorted(Comparator.comparingInt((CurseForgeFile f) -> f.id).reversed())
                .findFirst().orElse(null);
    }

    /**
     * Whether the instance already has this, counting the several ways the loader APIs stand in for
     * each other - the same equivalences the dependency panels use to decide what to show.
     */
    public static boolean isSatisfiedOnModrinth(ModManagement instanceOrServer, String projectId) {
        if (Constants.MODRINTH_FABRIC_MOD_ID.equals(projectId)
                && instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
            return true;
        }

        for (DisableableMod mod : instanceOrServer.getMods()) {
            if (mod.isFromModrinth() && projectId.equals(mod.modrinthProject.id)) {
                return true;
            }

            // Quilt Standard Libraries carries Fabric API
            if (Constants.MODRINTH_FABRIC_MOD_ID.equals(projectId) && mod.isFromModrinth()
                    && Constants.MODRINTH_QSL_MOD_ID.equals(mod.modrinthProject.id)) {
                return true;
            }

            if (mod.isFromCurseForge() && isTheSameLoaderApi(projectId, mod.getCurseForgeModId())) {
                return true;
            }
        }

        return false;
    }

    /** As {@link #isSatisfiedOnModrinth}, from the other side. */
    public static boolean isSatisfiedOnCurseForge(ModManagement instanceOrServer, int projectId) {
        if (projectId == Constants.CURSEFORGE_FABRIC_MOD_ID
                && instanceOrServer.isForgeLikeAndHasInstalledSinytraConnector()) {
            return true;
        }

        for (DisableableMod mod : instanceOrServer.getMods()) {
            if (mod.isFromCurseForge() && mod.getCurseForgeModId() == projectId) {
                return true;
            }

            if (projectId == Constants.CURSEFORGE_FABRIC_MOD_ID && mod.isFromModrinth()
                    && Constants.MODRINTH_QSL_MOD_ID.equals(mod.modrinthProject.id)) {
                return true;
            }

            if (mod.isFromModrinth() && isTheSameLoaderApi(mod.modrinthProject.id, projectId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * The three loader APIs that exist on both platforms, so one installed from CurseForge
     * satisfies a Modrinth dependency on it and the other way round.
     */
    private static boolean isTheSameLoaderApi(String modrinthProjectId, Integer curseForgeProjectId) {
        if (modrinthProjectId == null || curseForgeProjectId == null) {
            return false;
        }

        if (Constants.MODRINTH_FABRIC_MOD_ID.equals(modrinthProjectId)) {
            return curseForgeProjectId == Constants.CURSEFORGE_FABRIC_MOD_ID;
        }

        if (Constants.MODRINTH_LEGACY_FABRIC_MOD_ID.equals(modrinthProjectId)) {
            return curseForgeProjectId == Constants.CURSEFORGE_LEGACY_FABRIC_MOD_ID;
        }

        if (Constants.MODRINTH_FORGIFIED_FABRIC_API_MOD_ID.equals(modrinthProjectId)) {
            return curseForgeProjectId == Constants.CURSEFORGE_FORGIFIED_FABRIC_API_MOD_ID;
        }

        return false;
    }
}
