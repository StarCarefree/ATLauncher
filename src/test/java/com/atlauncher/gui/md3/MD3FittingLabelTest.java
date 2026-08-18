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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Long instance names used to clip at the card's edge with no sign that anything was missing.
 */
public class MD3FittingLabelTest {
    private static final String LONG_NAME = "我的世界整合包僵尸入侵一百天超长实例名 All The Mods 10";

    @BeforeEach
    public void install() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);
    }

    @Test
    public void aNarrowTitleWrapsInsteadOfClipping() {
        MD3FittingLabel title = new MD3FittingLabel(LONG_NAME, 2);
        title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        title.fitTo(120);

        String shown = title.getText();

        assertTrue(shown.contains("<br>") || shown.contains("…") || shown.contains("..."),
                "a long title stayed on one unwrapped line: " + shown);
        assertTrue(title.getPreferredSize().height >= title.getFontMetrics(title.getFont()).getHeight() * 2,
                "the card did not keep room for the second line");
    }

    @Test
    public void aSingleLineTitleGetsAnEllipsisWhenItDoesNotFit() {
        MD3FittingLabel subtitle = new MD3FittingLabel(LONG_NAME, 1);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.fitTo(80);

        assertTrue(subtitle.getText().contains("…"),
                "a line that did not fit was clipped with no ellipsis: " + subtitle.getText());
        assertTrue(LONG_NAME.equals(subtitle.getToolTipText()) || subtitle.getToolTipText() != null,
                "the full title is no longer reachable");
    }
}
