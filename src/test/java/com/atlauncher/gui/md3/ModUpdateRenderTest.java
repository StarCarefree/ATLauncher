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
package com.atlauncher.gui.md3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.util.Collections;

import javax.swing.UIManager;

import com.atlauncher.App;
import com.atlauncher.Launcher;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.ModUpdate;
import com.atlauncher.data.Settings;
import com.atlauncher.data.Type;
import com.atlauncher.data.curseforge.CurseForgeFile;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.modrinth.ModrinthChannel;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.themes.ATLauncherLaf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What the update panel puts in front of the user.
 *
 * <p>
 * A row has to say what a mod is going from and to - "the selected mods have been checked for
 * updates", which is what the old flow said whatever had happened, is not something anyone can act
 * on. The pre-release badge matters for the same reason: moving from a stable release onto an alpha
 * is the one thing about an update a user may want to decline.
 */
public class ModUpdateRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.settings = new Settings();
        App.THEME = (ATLauncherLaf) Class.forName("com.atlauncher.themes.MaterialDark")
                .getMethod("getInstance").invoke(null);
        App.launcher = new Launcher();
    }

    @Test
    public void testACurseForgeUpdateSaysWhatItIsGoingFromAndTo() {
        CurseForgeProject project = new CurseForgeProject();
        project.id = 1234;
        project.name = "Sodium";

        CurseForgeFile installed = new CurseForgeFile();
        installed.id = 100;
        installed.displayName = "sodium-0.6.0";
        installed.releaseType = 1;

        CurseForgeFile newer = new CurseForgeFile();
        newer.id = 200;
        newer.displayName = "sodium-0.6.3";
        newer.releaseType = 1;

        DisableableMod mod = new DisableableMod("Sodium", "0.6.0", false, "sodium-0.6.0.jar", Type.mods, null, "", false,
                true, true, false, project.id, installed.id, project, installed);

        ModUpdate update = ModUpdate.forCurseForge(mod, project, newer);

        assertEquals(ModPlatform.CURSEFORGE, update.platform);
        assertEquals("Sodium", update.getName());
        assertEquals("sodium-0.6.0", update.getCurrentVersion());
        assertEquals("sodium-0.6.3", update.getNewVersion());
        assertEquals("CurseForge", update.getPlatformName());
        assertNull(update.getPrereleaseChannel(), "a stable release should not be badged");
    }

    @Test
    public void testAModrinthUpdateReadsItsVersionNumbers() {
        ModrinthProject project = new ModrinthProject();
        project.id = "AANobbMI";
        project.title = "Sodium";
        project.iconUrl = "https://example.invalid/sodium.png";

        DisableableMod mod = new DisableableMod("Sodium", "0.6.0", false, "sodium-0.6.0.jar", Type.mods, null, "", false,
                true, true, false, project, version("0.6.0", ModrinthChannel.RELEASE));

        ModUpdate update = ModUpdate.forModrinth(mod, project, version("0.6.3", ModrinthChannel.RELEASE));

        assertEquals("0.6.0", update.getCurrentVersion());
        assertEquals("0.6.3", update.getNewVersion());
        assertEquals("Modrinth", update.getPlatformName());
        assertEquals("https://example.invalid/sodium.png", update.getIconUrl());
    }

    @Test
    public void testAPreReleaseIsBadged() {
        ModrinthProject project = new ModrinthProject();
        project.id = "AANobbMI";
        project.title = "Sodium";

        DisableableMod mod = new DisableableMod("Sodium", "0.6.0", false, "sodium-0.6.0.jar", Type.mods, null, "", false,
                true, true, false, project, version("0.6.0", ModrinthChannel.RELEASE));

        assertNotNull(ModUpdate.forModrinth(mod, project, version("0.7.0", ModrinthChannel.BETA))
                .getPrereleaseChannel());
        assertNotNull(ModUpdate.forModrinth(mod, project, version("0.7.0", ModrinthChannel.ALPHA))
                .getPrereleaseChannel());
        assertNull(ModUpdate.forModrinth(mod, project, version("0.7.0", ModrinthChannel.RELEASE))
                .getPrereleaseChannel());
    }

    /**
     * A mod dropped in by hand and then matched by fingerprint has platform ids but no file object
     * behind them, so the row falls back to what the launcher recorded rather than showing nothing.
     */
    @Test
    public void testAModWithNoRecordedFileStillSaysWhatIsInstalled() {
        CurseForgeProject project = new CurseForgeProject();
        project.id = 1234;
        project.name = "Sodium";

        CurseForgeFile newer = new CurseForgeFile();
        newer.id = 200;
        newer.displayName = "sodium-0.6.3";
        newer.releaseType = 1;

        DisableableMod mod = new DisableableMod("Sodium", "0.6.0", false, "sodium-0.6.0.jar", Type.mods, null, "", false,
                true, true, false, project.id, 100, null, null);

        ModUpdate update = ModUpdate.forCurseForge(mod, project, newer);

        assertEquals("0.6.0", update.getCurrentVersion());
        assertFalse(update.getNewVersion().isEmpty());
        assertTrue(update.getIconUrl() == null || !update.getIconUrl().isEmpty());
    }

    private static ModrinthVersion version(String number, ModrinthChannel channel) {
        ModrinthVersion version = new ModrinthVersion();
        version.id = "v" + number;
        version.name = "Sodium " + number;
        version.versionNumber = number;
        version.versionType = channel;
        version.datePublished = "2024-01-01T00:00:00Z";

        ModrinthFile file = new ModrinthFile();
        file.primary = true;
        file.filename = "sodium-" + number + ".jar";
        file.hashes = Collections.singletonMap("sha1", "0123456789abcdef0123456789abcdef01234567");
        version.files = Collections.singletonList(file);

        return version;
    }
}
