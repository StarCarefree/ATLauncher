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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.App;
import com.atlauncher.Launcher;
import com.atlauncher.data.Settings;
import com.atlauncher.evnt.LogEvent.LogType;
import com.atlauncher.gui.LauncherConsole;
import com.atlauncher.gui.components.Console;
import com.atlauncher.themes.MaterialDark;

/**
 * The console window - the log, and the toolbar that narrows it down.
 *
 * <p>
 * The console is built before {@code LogManager.start()} runs, so anything it throws goes to
 * {@code ExceptionStrainer}, which hands it to a logger that has not been started yet: the launcher
 * exits with status 0, an empty log, and no window. There is no worse place in the launcher for an
 * exception to be, which is why this builds the real window rather than testing the pieces.
 */
public class ConsoleRenderTest {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 420;

    @BeforeEach
    public void installTheme() throws Exception {
        MaterialDark.install();

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);

        App.THEME = MaterialDark.getInstance();
        App.settings = new Settings();
        App.launcher = new Launcher();
    }

    /**
     * Laid out on the event thread: a realised window packs itself, which starts the event queue, and
     * laying the same tree out from the test thread then deadlocks against the AWT tree lock.
     */
    private static LauncherConsole console() throws Exception {
        final LauncherConsole[] built = new LauncherConsole[1];

        SwingUtilities.invokeAndWait(() -> {
            built[0] = new LauncherConsole();
            App.console = built[0];

            // laid out by hand rather than packed: realising the window starts the event queue, and
            // the tree then has to be laid out from it or the two deadlock over the AWT tree lock
            built[0].getContentPane().setSize(WIDTH, HEIGHT);
            layoutTree(built[0].getContentPane());
        });

        return built[0];
    }

    private static void layoutTree(Component c) {
        c.doLayout();

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static void log(Console console, LogType type, String body) throws Exception {
        console.append(type, "16:01:16", body + "\n");

        // append hands the drawing to the event thread, which is where the assertions want it done
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    @Test
    public void testTheConsoleBuildsAndRenders() throws Exception {
        LauncherConsole window = console();

        log(window.console, LogType.INFO, "Launcher opening");
        log(window.console, LogType.DEBUG, "Setting up language for console");
        log(window.console, LogType.WARN, "_JAVA_OPTIONS environment variable detected");
        log(window.console, LogType.ERROR, "Error organising filesystem");
        log(window.console, LogType.INFO,
                "GPU: NVIDIA GeForce RTX 3050 Laptop GPU (NVIDIA) 32.0.16.1062 4096MB VRAM, which is long "
                        + "enough that it has to wrap and hang under the message rather than starting back at "
                        + "the margin");

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        SwingUtilities.invokeAndWait(() -> {
            // the log has grown since the tree was last laid out, so the scroll pane's view has to
            // be re-measured or the lines land outside the viewport and nothing is drawn
            layoutTree(window.getContentPane());

            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MD3Gallery.applyDesktopFontHints(g);
            window.getContentPane().paint(g);
            g.dispose();
        });

        new File("build/md3-preview").mkdirs();
        ImageIO.write(image, "png", new File("build/md3-preview/console-dark.png"));

        assertEquals(5, window.console.getTotalCount(), "the console did not keep every line it was given");
        assertEquals(5, window.console.getShownCount(), "the console is hiding lines with no filter applied");
    }

    /**
     * The reason the entries are kept at all. A thousand lines of Minecraft debug output with four
     * that matter is the case the console exists for.
     */
    @Test
    public void testHidingALevelAndSearchingNarrowTheView() throws Exception {
        LauncherConsole window = console();
        Console console = window.console;

        log(console, LogType.INFO, "Launcher opening");
        log(console, LogType.DEBUG, "Loading instances");
        log(console, LogType.DEBUG, "Loading packs");
        log(console, LogType.ERROR, "Error organising filesystem");

        SwingUtilities.invokeAndWait(() -> console.setLevelVisible(LogType.DEBUG, false));

        assertEquals(2, console.getShownCount(), "turning debug off did not hide the debug lines");
        assertEquals(4, console.getTotalCount(), "turning debug off dropped the lines rather than hiding them");

        SwingUtilities.invokeAndWait(() -> console.setLevelVisible(LogType.DEBUG, true));
        SwingUtilities.invokeAndWait(() -> console.setQuery("packs"));

        assertEquals(1, console.getShownCount(), "the search matched something other than the one line with it in");
    }

    /**
     * Turning every level off leaves a blank rectangle, which looks exactly like a console that has
     * stopped working - so it has to say that a filter is the reason.
     */
    @Test
    public void testAConsoleFilteredToNothingSaysWhy() throws Exception {
        LauncherConsole window = console();
        Console console = window.console;

        log(console, LogType.INFO, "Launcher opening");

        SwingUtilities.invokeAndWait(() -> {
            for (LogType type : LogType.values()) {
                console.setLevelVisible(type, false);
            }

            console.setSize(WIDTH, HEIGHT);
        });

        assertEquals(0, console.getShownCount(), "a level that was turned off is still being shown");

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            console.paint(g);
            g.dispose();
        });

        int corner = image.getRGB(0, 0);
        boolean painted = false;

        for (int y = 0; y < HEIGHT && !painted; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (image.getRGB(x, y) != corner) {
                    painted = true;

                    break;
                }
            }
        }

        assertTrue(painted, "a console filtered down to nothing paints nothing, so it looks broken rather than empty");
    }

    /**
     * A log uploaded to a bug report has to be the whole log. One trimmed to whatever the reporter
     * happened to be filtering by is worse than none, because nothing in it says it was trimmed.
     */
    @Test
    public void testTheUploadedLogIgnoresTheFilter() throws Exception {
        LauncherConsole window = console();
        Console console = window.console;

        log(console, LogType.INFO, "Launcher opening");
        log(console, LogType.ERROR, "Error organising filesystem");

        SwingUtilities.invokeAndWait(() -> console.setLevelVisible(LogType.ERROR, false));

        String log = window.getLog();

        assertTrue(log.contains("Error organising filesystem"),
                "the log handed to Copy and Upload is missing the errors the view is hiding");
        assertTrue(log.contains("[ERROR]"), "the log handed to Copy and Upload does not say what level a line was");
    }
}
