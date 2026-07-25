/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.ConsoleState;
import com.atlauncher.evnt.LogEvent.LogType;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.ConsoleStateManager;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.components.Console;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.NotificationManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.thread.PasteUpload;
import com.atlauncher.utils.Utils;

import com.formdev.flatlaf.util.UIScale;

/**
 * The console window: a toolbar to narrow the log down with, the log, and what you can do with it.
 *
 * <p>
 * It used to be the log and a fifty pixel bar carrying three buttons and six social network icons,
 * which hid themselves one at a time as the window narrowed - a console is where you go when
 * something has gone wrong, and half of that bar was links to Facebook. Those live on the About page
 * now, as they already did for the main window, so the two bottom bar classes went with them.
 *
 * <p>
 * What replaced them is the thing the window was actually missing: a search box and a chip per log
 * level, so a thousand lines of Minecraft debug output can be narrowed to the four that matter.
 */
public class LauncherConsole extends JFrame implements RelocalizationListener {
    private static final long serialVersionUID = -3538990021922025818L;

    /** Long enough that typing does not rebuild the document per keystroke. */
    private static final int SEARCH_DEBOUNCE = 200;

    public Console console;

    private final Map<LogType, MD3Chip> levelChips = new EnumMap<>(LogType.class);
    private final JLabel counts = new JLabel();
    private MD3TextField search;
    private MD3Button clearButton;
    private MD3Button copyLogButton;
    private MD3Button uploadLogButton;
    private MD3Button killMinecraftButton;

    private JPopupMenu contextMenu;
    private JMenuItem copy;

    public LauncherConsole() {
        // #. {0} is the name of the launcher (ATLauncher)
        setTitle(GetText.tr("{0} Console", Constants.LAUNCHER_NAME));
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setIconImage(Utils.getImage("/assets/image/icon.png"));
        setLayout(new BorderLayout());

        setMinimumSize(UIScale.scale(new Dimension(720, 320)));
        setSize(UIScale.scale(new Dimension(900, 560)));

        try {
            if (App.settings.rememberWindowSizePosition && App.settings.consoleSize != null
                    && App.settings.consolePosition != null) {
                setBounds(App.settings.consolePosition.x, App.settings.consolePosition.y,
                        App.settings.consoleSize.width, App.settings.consoleSize.height);
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error setting custom remembered window size settings", e);
        }

        console = new Console();
        console.setBackground(MD3Color.surface());
        console.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
        console.setOnContentChanged(this::updateCounts);

        setupContextMenu();

        JScrollPane scrollPane = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(MD3Color.surface());
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIScale.scale(24));

        add(buildToolbar(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        updateCounts();
        RelocalizationManager.addListener(this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.consoleSize = c.getSize();
                    App.settings.save();
                }
            }

            @Override
            public void componentMoved(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.consolePosition = c.getLocation();
                    App.settings.save();
                }
            }
        });
    }

    /**
     * Search on the left, a chip per level, and how much of the log is getting through them.
     */
    private JPanel buildToolbar() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));
        row.setOpaque(false);

        search = MD3TextField.search(GetText.tr("Search"));
        search.setPreferredSize(new Dimension(UIScale.scale(240), search.getPreferredSize().height));
        search.getDocument().addDocumentListener(new DocumentListener() {
            private final Timer debounce = newSearchDebounce();

            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        });

        row.add(search);

        for (LogType type : LogType.values()) {
            MD3Chip chip = MD3Chip.filter(labelFor(type));
            chip.setSelected(console.isLevelVisible(type));
            chip.addActionListener(e -> console.setLevelVisible(type, chip.isSelected()));

            levelChips.put(type, chip);
            row.add(chip);
        }

        counts.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        counts.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        counts.setForeground(MD3Color.onSurfaceVariant());
        counts.setToolTipText(GetText.tr("Lines shown of lines logged"));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(true);
        toolbar.setBackground(MD3Color.surfaceContainer());
        toolbar.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));
        toolbar.add(row, BorderLayout.CENTER);
        toolbar.add(counts, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(toolbar, BorderLayout.CENTER);
        header.add(new MD3Divider(), BorderLayout.SOUTH);

        return header;
    }

    /**
     * A debounce shared by every kind of document change - one timer, restarted, so a burst of
     * keystrokes rebuilds the view once at the end rather than once each.
     */
    private Timer newSearchDebounce() {
        Timer timer = new Timer(SEARCH_DEBOUNCE, e -> console.setQuery(search.getText()));
        timer.setRepeats(false);

        return timer;
    }

    private JPanel buildActionBar() {
        clearButton = MD3Button.text(GetText.tr("Clear"));
        copyLogButton = MD3Button.outlined(GetText.tr("Copy Log"));
        uploadLogButton = MD3Button.outlined(GetText.tr("Upload Log"));
        killMinecraftButton = MD3Button.filled(GetText.tr("Kill Minecraft"));
        killMinecraftButton.setVisible(false);

        addActionListeners();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.add(clearButton);
        actions.add(copyLogButton);
        actions.add(uploadLogButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(true);
        bar.setBackground(MD3Color.surfaceContainer());
        bar.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));
        bar.add(actions, BorderLayout.WEST);
        bar.add(killMinecraftButton, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(new MD3Divider(), BorderLayout.NORTH);
        footer.add(bar, BorderLayout.CENTER);

        return footer;
    }

    private void addActionListeners() {
        clearButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.simpleEvent("console_clear"));
            App.console.clearConsole();
            LogManager.info("Console Cleared");
        });

        copyLogButton.addActionListener(e -> {
            Analytics.trackEvent(AnalyticsEvent.simpleEvent("console_copy"));
            NotificationManager.show("Copied Log to clipboard");
            LogManager.info("Copied Log to clipboard");
            StringSelection text = new StringSelection(App.console.getLog());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(text, null);
        });

        uploadLogButton.addActionListener(e -> uploadLog());

        killMinecraftButton.addActionListener(e -> {
            int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Kill Minecraft") + "?")
                    .setContent(new HTMLBuilder().center().text(GetText.tr(
                            "Are you sure you want to kill the Minecraft process?<br/><br/>Doing so can cause corruption of your saves."))
                            .build())
                    .setType(DialogManager.QUESTION).show();

            if (ret == DialogManager.YES_OPTION) {
                Analytics.trackEvent(AnalyticsEvent.simpleEvent("console_kill_minecraft"));
                App.launcher.killMinecraft();
                killMinecraftButton.setVisible(false);
            }
        });
    }

    private void uploadLog() {
        final ProgressDialog<String> dialog = new ProgressDialog<>(GetText.tr("Uploading Logs"), 0,
                GetText.tr("Uploading Logs"), "Aborting Uploading Logs", App.console);

        dialog.addThread(new Thread(() -> {
            try {
                dialog.setReturnValue(App.TASKPOOL.submit(new PasteUpload()).get());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                dialog.setReturnValue(null);
            } catch (ExecutionException ex) {
                LogManager.logStackTrace("Exception while uploading paste", ex);
                dialog.setReturnValue(null);
            }

            dialog.close();
        }));

        dialog.start();

        String result = dialog.getReturnValue();

        if (result != null && result.contains(Constants.PASTE_CHECK_URL)) {
            Analytics.trackEvent(AnalyticsEvent.simpleEvent("console_upload"));
            NotificationManager.show("Log uploaded and link copied to clipboard");
            LogManager.info("Log uploaded and link copied to clipboard: " + result);
            StringSelection text = new StringSelection(result);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(text, null);
        } else {
            DialogManager.okDialog().setType(DialogManager.ERROR).setTitle(GetText.tr("Failed to upload log"))
                    .setContent("Log failed to upload!").show();
            LogManager.error("Log failed to upload: " + result);
        }
    }

    /**
     * Log levels are named the same way everywhere, so these are the level names rather than a
     * description of what each one means.
     */
    private static String labelFor(LogType type) {
        switch (type) {
            case WARN:
                return GetText.tr("Warn");
            case ERROR:
                return GetText.tr("Error");
            case DEBUG:
                return GetText.tr("Debug");
            case INFO:
            default:
                return GetText.tr("Info");
        }
    }

    private void updateCounts() {
        NumberFormat format = NumberFormat.getIntegerInstance();

        counts.setText(format.format(console.getShownCount()) + " / " + format.format(console.getTotalCount()));
    }

    @Override
    public void setVisible(boolean flag) {
        super.setVisible(flag);

        ConsoleStateManager.setState(flag ? ConsoleState.OPEN : ConsoleState.CLOSED);
    }

    private void setupContextMenu() {
        contextMenu = new JPopupMenu();

        copy = new JMenuItem(GetText.tr("Copy"));
        copy.addActionListener(e -> {
            StringSelection text = new StringSelection(console.getSelectedText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(text, null);
        });
        contextMenu.add(copy);

        console.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (console.getSelectedText() != null) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        contextMenu.show(console, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     * @return every line logged this session, whatever the console is currently filtered to
     */
    public String getLog() {
        return console.getLog();
    }

    public void showKillMinecraft() {
        killMinecraftButton.setVisible(true);
    }

    public void hideKillMinecraft() {
        killMinecraftButton.setVisible(false);
    }

    public void setupLanguage() {
        LogManager.debug("Setting up language for console");
        onRelocalization();
        LogManager.debug("Finished setting up language for console");
    }

    public void clearConsole() {
        console.clear();
    }

    @Override
    public void onRelocalization() {
        copy.setText(GetText.tr("Copy"));
        search.setLabel(GetText.tr("Search"));
        counts.setToolTipText(GetText.tr("Lines shown of lines logged"));

        clearButton.setText(GetText.tr("Clear"));
        copyLogButton.setText(GetText.tr("Copy Log"));
        uploadLogButton.setText(GetText.tr("Upload Log"));
        killMinecraftButton.setText(GetText.tr("Kill Minecraft"));

        for (Map.Entry<LogType, MD3Chip> entry : levelChips.entrySet()) {
            entry.getValue().setText(labelFor(entry.getKey()));
        }
    }
}
