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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.md3.feedback.MD3LinearProgress;
import com.atlauncher.themes.md3.token.MD3Color;

/**
 * The busy dialog.
 *
 * <p>
 * A Material linear indicator paints no text inside itself, so the counts that used to live in the
 * bars had to move to labels beneath them. If that had been done by simply dropping the
 * {@code setString} calls, the counts would have disappeared with nothing to show it - which is what
 * these check has not happened.
 */
public class ProgressDialogRenderTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static ProgressDialog<Object> dialog(int tasks) {
        return new ProgressDialog<>("Installing", tasks, "Downloading mods", null, new Frame());
    }

    private static List<JLabel> labels(Container root) {
        List<JLabel> found = new ArrayList<>();

        for (Component c : root.getComponents()) {
            if (c instanceof JLabel) {
                found.add((JLabel) c);
            } else if (c instanceof Container) {
                found.addAll(labels((Container) c));
            }
        }

        return found;
    }

    private static JLabel labelStartingWith(Container root, String prefix) {
        for (JLabel label : labels(root)) {
            if (label.getText() != null && label.getText().startsWith(prefix)) {
                return label;
            }
        }

        return null;
    }

    private static MD3LinearProgress firstBar(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof MD3LinearProgress) {
                return (MD3LinearProgress) c;
            }

            if (c instanceof Container) {
                MD3LinearProgress found = firstBar((Container) c);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * The count of tasks was never stored, so the denominator was always zero - every install
     * counted its way towards "N/0 Tasks Done".
     */
    @Test
    public void testTheTaskCountKnowsHowManyThereAre() {
        ProgressDialog<Object> dialog = dialog(10);

        dialog.doneTask();
        dialog.doneTask();
        dialog.doneTask();

        JLabel count = labelStartingWith(dialog.getContentPane(), "3/");

        assertNotNull(count, "the task count is not being shown at all");
        assertTrue(count.getText().startsWith("3/10"),
                "the task count reads \"" + count.getText() + "\" rather than counting towards 10");
    }

    @Test
    public void testTheBarTracksTheTasks() {
        ProgressDialog<Object> dialog = dialog(4);

        dialog.doneTask();
        dialog.doneTask();

        MD3LinearProgress bar = firstBar(dialog.getContentPane());

        assertNotNull(bar, "the dialog has no progress bar");
        assertEquals(2, bar.getValue(), "the bar did not follow the tasks");
        assertEquals(4, bar.getMaximum(), "the bar is not measured against the number of tasks");
    }

    /**
     * Download progress is reported in bytes and shown in megabytes under the second bar.
     */
    @Test
    public void testDownloadProgressIsShownUnderItsBar() {
        ProgressDialog<Object> dialog = dialog(1);

        dialog.setTotalBytes(4L * 1024 * 1024);
        dialog.addDownloadedBytes(1024 * 1024);

        JLabel bytes = labelStartingWith(dialog.getContentPane(), "1.00 MB");

        assertNotNull(bytes, "the download size is not being shown");
        assertTrue(bytes.getText().contains("4.00 MB"), "the download size does not say how much there is to go");
        assertTrue(bytes.isVisible(), "the download size is hidden while a download is in progress");
    }

    @Test
    public void testAnUncountedJobGetsAnIndeterminateBar() {
        MD3LinearProgress bar = firstBar(dialog(0).getContentPane());

        assertNotNull(bar, "the dialog has no progress bar");
        assertTrue(bar.isIndeterminate(),
                "a job with no known number of steps shows an empty bar rather than an indeterminate one");
    }

    @Test
    public void testProgressRenders() throws Exception {
        ProgressDialog<Object> dialog = dialog(10);
        dialog.doneTask();
        dialog.doneTask();
        dialog.doneTask();
        dialog.setTotalBytes(9L * 1024 * 1024);
        dialog.addDownloadedBytes(4L * 1024 * 1024);

        Container content = dialog.getContentPane();
        content.setSize(dialog.getSize());
        content.doLayout();

        for (Component c : content.getComponents()) {
            c.doLayout();
        }

        BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MD3Gallery.applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        content.paint(g);
        g.dispose();

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/progress-dark.png"));
    }
}
