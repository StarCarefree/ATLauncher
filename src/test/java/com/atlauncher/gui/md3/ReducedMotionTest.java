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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.data.Settings;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.viewmodel.impl.settings.GeneralSettingsViewModel;

/**
 * The switch that turns the launcher's animations off.
 *
 * <p>
 * Every animation in the launcher already asked {@link MD3Motion#isReduced()}, and nothing outside
 * the tests ever answered yes - the accessibility switch was there and unreachable. What these check
 * is the whole chain: the setting reaches the token, and the token reaches the animations.
 */
public class ReducedMotionTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        App.settings = new Settings();
    }

    /**
     * The render tests want motion off, and this one turns it on, so it is put back - these share a
     * JVM and a set of UI defaults.
     */
    @AfterEach
    public void restoreReducedMotion() {
        MD3Motion.setReduced(true);
    }

    @Test
    public void testTheTokenIsWhatEveryAnimationAsks() {
        MD3Motion.setReduced(false);

        assertFalse(MD3Motion.isReduced(), "motion is reported reduced when it was turned back on");
        assertEquals(MD3Motion.MEDIUM2, MD3Motion.duration(MD3Motion.MEDIUM2),
                "an animation was shortened while motion was on");

        MD3Motion.setReduced(true);

        assertTrue(MD3Motion.isReduced(), "turning motion off did not take");
        assertEquals(1, MD3Motion.duration(MD3Motion.MEDIUM2),
                "an animation still runs its full length with motion turned off");
    }

    /**
     * Applied as it is set rather than on save, so ticking the box in the settings takes effect in
     * the settings page itself.
     */
    @Test
    public void testTheSettingReachesTheToken() {
        MD3Motion.setReduced(false);

        GeneralSettingsViewModel viewModel = new GeneralSettingsViewModel();

        viewModel.setReduceAnimations(true);

        assertTrue(App.settings.reduceAnimations, "the setting was not recorded, so it will not be saved");
        assertTrue(MD3Motion.isReduced(),
                "the setting never reached the token, so the animations carry on regardless of it");

        viewModel.setReduceAnimations(false);

        assertFalse(App.settings.reduceAnimations, "the setting could not be turned back off");
        assertFalse(MD3Motion.isReduced(), "turning the setting back off left the animations off");
    }
}
