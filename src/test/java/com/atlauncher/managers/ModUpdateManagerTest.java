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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.AddModRestriction;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModManagement;
import com.atlauncher.data.Settings;
import com.atlauncher.data.Type;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthDownloadMetadata;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.utils.ModCompatibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The rules behind the bulk update check, without the network.
 *
 * <p>
 * The check itself needs both platforms to answer, but everything it decides with - which
 * Minecraft versions count, which loader tags a file may carry, whether a stable release is allowed
 * to become a pre-release - is {@link ModCompatibility} and a comparison, and those are worth
 * pinning: getting them wrong does not look broken, it just quietly stops offering updates.
 */
public class ModUpdateManagerTest {
    @BeforeEach
    public void settings() {
        App.settings = new Settings();
        App.settings.addModRestriction = AddModRestriction.STRICT;
    }

    @Test
    public void testAFileForAnotherMinecraftVersionIsNotAnUpdate() {
        FakeInstance instance = new FakeInstance("1.21.1", new LoaderVersion("0.16.0", false, "Fabric"));
        List<String> toMatch = ModCompatibility.minecraftVersionsToMatch(instance);

        assertTrue(ModCompatibility.matchesMinecraftVersion(Arrays.asList("1.21.1", "Fabric"), toMatch));
        assertFalse(ModCompatibility.matchesMinecraftVersion(Arrays.asList("1.20.1", "Fabric"), toMatch));
    }

    @Test
    public void testNoRestrictionAcceptsAnyMinecraftVersion() {
        App.settings.addModRestriction = AddModRestriction.NONE;

        FakeInstance instance = new FakeInstance("1.21.1", new LoaderVersion("0.16.0", false, "Fabric"));

        assertTrue(ModCompatibility.matchesMinecraftVersion(Collections.singletonList("1.7.10"),
                ModCompatibility.minecraftVersionsToMatch(instance)));
    }

    @Test
    public void testAFabricFileIsNotOfferedToAForgeInstance() {
        FakeInstance forge = new FakeInstance("1.20.1", new LoaderVersion("47.2.0", false, "Forge"));

        assertFalse(ModCompatibility.matchesCurseForgeLoaderTags(Arrays.asList("1.20.1", "Fabric"), forge, false));
        assertTrue(ModCompatibility.matchesCurseForgeLoaderTags(Arrays.asList("1.20.1", "Forge"), forge, false));
    }

    /**
     * CurseForge stuffs loader names into the game version list, and a file that names none of them
     * is a mod that runs anywhere - a resource pack, or an author who did not tag it. Dropping
     * those would silently stop half a modpack from ever being updated.
     */
    @Test
    public void testAnUntaggedFileIsOfferedToAnyLoader() {
        FakeInstance forge = new FakeInstance("1.20.1", new LoaderVersion("47.2.0", false, "Forge"));

        assertTrue(ModCompatibility.matchesCurseForgeLoaderTags(Collections.singletonList("1.20.1"), forge, false));
        assertTrue(ModCompatibility.matchesCurseForgeModLoaderId(ModCompatibility.MOD_LOADER_ANY, forge, false));
    }

    @Test
    public void testQuiltTakesFabricFiles() {
        FakeInstance quilt = new FakeInstance("1.21.1", new LoaderVersion("0.26.0", false, "Quilt"));

        assertTrue(ModCompatibility.matchesCurseForgeLoaderTags(Arrays.asList("1.21.1", "Fabric"), quilt, false));
        assertTrue(ModCompatibility.matchesCurseForgeModLoaderId(Constants.CURSEFORGE_FABRIC_MODLOADER_ID, quilt,
                false));
    }

    @Test
    public void testAnInstanceWithNoLoaderTakesNothingLoaderSpecific() {
        FakeInstance vanilla = new FakeInstance("1.21.1", null);

        assertFalse(ModCompatibility.matchesCurseForgeModLoaderId(Constants.CURSEFORGE_FABRIC_MODLOADER_ID, vanilla,
                false));
        assertTrue(ModCompatibility.matchesCurseForgeModLoaderId(ModCompatibility.MOD_LOADER_ANY, vanilla, false));
    }

    @Test
    public void testModrinthLoadersCarryFabricForQuilt() {
        FakeInstance quilt = new FakeInstance("1.21.1", new LoaderVersion("0.26.0", false, "Quilt"));

        List<String> loaders = ModCompatibility.modrinthLoaders(quilt);

        assertTrue(loaders.contains("quilt"), "quilt was not asked for: " + loaders);
        assertTrue(loaders.contains("fabric"), "quilt runs fabric mods, but fabric was not asked for: " + loaders);
    }

    /**
     * The cached result is what a row's "Update" badge reads, so a mod that has just been updated
     * has to leave it without another round trip.
     */
    @Test
    public void testAnUpdatedModStopsBeingOffered() {
        FakeInstance instance = new FakeInstance("1.21.1", new LoaderVersion("0.16.0", false, "Fabric"));
        DisableableMod mod = modrinthMod("Sodium", "sodium-0.6.0.jar");

        instance.mods.add(mod);
        ModUpdateManager.invalidate(instance);

        assertFalse(ModUpdateManager.hasUpdate(instance, mod), "nothing has been checked yet");
        assertTrue(ModUpdateManager.getUpdates(instance).isEmpty());

        ModUpdateManager.markUpdated(instance, mod);

        assertFalse(ModUpdateManager.hasUpdate(instance, mod));
    }

    private static DisableableMod modrinthMod(String name, String file) {
        ModrinthProject project = new ModrinthProject();
        project.id = "AANobbMI";
        project.title = name;

        ModrinthVersion version = new ModrinthVersion();
        version.id = "abc123";
        version.versionNumber = "0.6.0";
        version.datePublished = "2024-01-01T00:00:00Z";

        ModrinthFile modrinthFile = new ModrinthFile();
        modrinthFile.primary = true;
        modrinthFile.filename = file;
        modrinthFile.hashes = Collections.singletonMap("sha1", "0123456789abcdef0123456789abcdef01234567");
        version.files = Collections.singletonList(modrinthFile);

        return new DisableableMod(name, "0.6.0", false, file, Type.mods, null, "A mod.", false, true, true, false,
                project, version);
    }

    /**
     * Enough of a {@link ModManagement} to answer the compatibility rules. Building a real
     * {@link com.atlauncher.data.Instance} would drag in the whole settings and manager stack for
     * three fields.
     */
    private static final class FakeInstance implements ModManagement {
        private final String minecraftVersion;
        private final LoaderVersion loaderVersion;
        private final List<DisableableMod> mods = new ArrayList<>();

        FakeInstance(String minecraftVersion, LoaderVersion loaderVersion) {
            this.minecraftVersion = minecraftVersion;
            this.loaderVersion = loaderVersion;
        }

        @Override
        public Path getRoot() {
            return Paths.get("target", "fake-instance", minecraftVersion);
        }

        @Override
        public String getName() {
            return "Fake";
        }

        @Override
        public String getMinecraftVersion() {
            return minecraftVersion;
        }

        @Override
        public LoaderVersion getLoaderVersion() {
            return loaderVersion;
        }

        @Override
        public boolean supportsPlugins() {
            return false;
        }

        @Override
        public boolean isForgeLikeAndHasInstalledSinytraConnector() {
            return false;
        }

        @Override
        public List<DisableableMod> getMods() {
            return mods;
        }

        @Override
        public void addMod(DisableableMod mod) {
            mods.add(mod);
        }

        @Override
        public void addMods(List<DisableableMod> modsToAdd) {
            mods.addAll(modsToAdd);
        }

        @Override
        public void removeMod(DisableableMod mod) {
            mods.remove(mod);
        }

        @Override
        public void addFileFromCurseForge(CurseForgeProject mod, CurseForgeFile file, ProgressDialog<Void> dialog) {
        }

        @Override
        public void addFileFromModrinth(ModrinthProject project, ModrinthVersion version, ModrinthFile file,
                Type installType, ModrinthDownloadMetadata.Reason downloadReason, String dependentVersionId,
                ProgressDialog<Void> dialog) {
        }

        @Override
        public void scanMissingMods(Window parent) {
        }

        @Override
        public void save() {
        }
    }
}
