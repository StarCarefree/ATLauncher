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
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.gui.md3.feedback.MD3LinearProgress;
import com.atlauncher.interfaces.NetworkProgressable;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.Utils;
import com.formdev.flatlaf.util.UIScale;

/**
 * What the launcher shows while it is busy.
 *
 * <p>
 * A Material linear indicator carries no text inside it, so the counts that used to be painted into
 * the bars - "3/10 Tasks Done", "4.20 MB / 9.00 MB" - are labels beneath them now. That is also why
 * the window keeps its title bar: closing it is how a long install is cancelled, and an undecorated
 * dialog would take that away.
 */
public class ProgressDialog<T> extends JDialog implements NetworkProgressable {
    /** Wide enough for "Downloading some-very-long-mod-file.jar" without the dialog resizing. */
    private static final int WIDTH = 420;

    private final String labelText; // The text to add to the JLabel
    private final MD3LinearProgress progressBar; // The Progress Bar
    private final MD3LinearProgress subProgressBar; // The Progress Bar
    private Thread thread = null; // The Thread were optionally running
    private final String closedLogMessage; // The message to log to the console when dialog closed
    private T returnValue = null; // The value returned
    public boolean wasClosed = false; // If the dialog was closed by the user
    private final JLabel label = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final JLabel subProgressLabel = new JLabel();
    private final int tasksToDo;
    private int tasksDone;
    private double totalBytes = 0; // Total number of bytes to download
    private double downloadedBytes = 0; // Total number of bytes downloaded

    public ProgressDialog(String title, int initMax, String initLabelText, String initClosedLogMessage,
            boolean showProgressBar, Window parent) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        this.labelText = initLabelText;
        this.closedLogMessage = initClosedLogMessage;

        // the count the bar is measured against. It was never stored, so every dialog counted its
        // way up towards "N/0 Tasks Done"
        this.tasksToDo = initMax;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setIconImage(Utils.getImage("/assets/image/icon.png"));
        setTitle(title);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(true);
        content.setBackground(MD3Color.surface());
        content.setBorder(MD3Spacing.border(MD3Spacing.XL));

        label.setText(initLabelText);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(MD3Type.font(MD3Type.BODY_LARGE, initLabelText));
        label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_LARGE);
        label.setForeground(MD3Color.onSurface());
        label.setAlignmentX(CENTER_ALIGNMENT);
        content.add(label);
        content.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)));

        progressBar = new MD3LinearProgress(0, initMax);
        if (initMax <= 0) {
            progressBar.setIndeterminate(true);
        }
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        content.add(progressBar);

        styleProgressLabel(progressLabel);
        progressLabel.setText(" ");
        content.add(progressLabel);

        subProgressBar = new MD3LinearProgress(0, 10000);
        subProgressBar.setAlignmentX(CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        content.add(subProgressBar);

        styleProgressLabel(subProgressLabel);
        subProgressLabel.setText(" ");
        content.add(subProgressLabel);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        // packed while everything is still showing, so the window has room for the download bar
        // before it appears - the dialog cannot be resized, and a box layout drops what is hidden
        pack();
        setSize(new Dimension(UIScale.scale(WIDTH), getHeight()));
        setLocationRelativeTo(parent);

        progressBar.setVisible(showProgressBar);
        progressLabel.setVisible(false);
        subProgressBar.setVisible(false);
        subProgressLabel.setVisible(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                wasClosed = true;
                if (closedLogMessage != null) {
                    LogManager.error(closedLogMessage);
                }
                if (thread != null) {
                    if (thread.isAlive()) {
                        thread.interrupt();
                    }
                }
                close(); // Close the dialog
            }
        });
    }

    /**
     * The counts that used to be painted inside the bars.
     */
    private void styleProgressLabel(JLabel toStyle) {
        toStyle.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        toStyle.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.LABEL_MEDIUM);
        toStyle.setForeground(MD3Color.onSurfaceVariant());
        toStyle.setHorizontalAlignment(SwingConstants.CENTER);
        toStyle.setAlignmentX(CENTER_ALIGNMENT);
        toStyle.setBorder(MD3Spacing.border(MD3Spacing.XS, 0, 0, 0));
    }

    public ProgressDialog(String title, int initMax, String initLabelText, String initClosedLogMessage,
            boolean showProgressBar) {
        this(title, initMax, initLabelText, initClosedLogMessage, showProgressBar, App.launcher.getParent());
    }

    public ProgressDialog(String title, int initMax, String initLabelText, String initClosedLogMessage, Window parent) {
        this(title, initMax, initLabelText, initClosedLogMessage, true, parent);
    }

    public ProgressDialog(String title, int initMax, String initLabelText, String initClosedLogMessage) {
        this(title, initMax, initLabelText, initClosedLogMessage, true);
    }

    public ProgressDialog(String title, int initMax, String initLabelText, Window parent) {
        this(title, initMax, initLabelText, null, true, parent);
    }

    public ProgressDialog(String title, int initMax, String initLabelText) {
        this(title, initMax, initLabelText, null, true);
    }

    public ProgressDialog(String title) {
        this(title, 0, title, null, true);
    }

    public ProgressDialog(String title, boolean showProgressBar, Window parent) {
        this(title, 0, title, null, showProgressBar, parent);
    }

    public void addThread(Thread thread) {
        this.thread = thread;
    }

    public void start() {
        if (this.thread != null) {
            thread.start();
        }
        setVisible(true);
    }

    public void doneTask() {
        this.tasksDone++;

        if (this.tasksToDo > 0) {
            // built the way it always was, so the existing translation of "Tasks Done" still applies
            this.progressLabel.setText(this.tasksDone + "/" + this.tasksToDo + " " + GetText.tr("Tasks Done"));
            this.progressLabel.setVisible(this.progressBar.isVisible());
        }

        this.progressBar.setValue(this.tasksDone);
        this.clearDownloadedBytes();
        this.label.setText(this.labelText);
    }

    public void setReturnValue(T returnValue) {
        this.returnValue = returnValue;
    }

    @Nullable
    public T getReturnValue() {
        return this.returnValue;
    }

    public void close() {
        setVisible(false); // Remove the dialog
        dispose(); // Dispose the dialog
    }

    public void setLabel(String text) {
        this.label.setText(text);
    }

    private void updateProgressBar() {
        double progress;
        if (this.totalBytes > 0) {
            progress = (this.downloadedBytes / this.totalBytes) * 100.0;
        } else {
            progress = 0.0;
        }
        double done = this.downloadedBytes / 1024.0 / 1024.0;
        double toDo = this.totalBytes / 1024.0 / 1024.0;
        if (done > toDo) {
            setSubProgress(100.0, String.format(Locale.ENGLISH, "%.2f MB", done));
        } else {
            setSubProgress(progress, String.format(Locale.ENGLISH, "%.2f MB / %.2f MB", done, toDo));
        }
    }

    public void setSubProgress(double percent, String label) {
        if (!subProgressBar.isVisible()) {
            subProgressBar.setVisible(true);
        }

        if (subProgressBar.isIndeterminate()) {
            subProgressBar.setIndeterminate(false);
        }

        if (percent < 0.0) {
            subProgressLabel.setVisible(false);
            subProgressBar.setVisible(false);
        } else {
            subProgressLabel.setVisible(true);

            if (label != null) {
                subProgressLabel.setText(label);
            }
        }

        if (label == null && percent > 0.0) {
            subProgressLabel.setText(String.format(Locale.ENGLISH, "%.2f%%", percent));
        }

        subProgressBar.setValue((int) Math.round(percent * 100.0));
    }

    public void setIndeterminate() {
        subProgressLabel.setVisible(false);

        if (!subProgressBar.isVisible()) {
            subProgressBar.setVisible(true);
        }
        if (!subProgressBar.isIndeterminate()) {
            subProgressBar.setIndeterminate(true);
        }
    }

    @Override
    public void setTotalBytes(long bytes) {
        this.downloadedBytes = 0L;
        this.totalBytes = bytes;

        subProgressBar.setVisible(bytes > 0L);
        subProgressLabel.setVisible(bytes > 0L);

        if (bytes > 0L) {
            this.updateProgressBar();
        }
    }

    @Override
    public void addDownloadedBytes(long bytes) {
        this.downloadedBytes += bytes;
        this.updateProgressBar();
    }

    public void clearDownloadedBytes() {
        this.downloadedBytes = 0L;
        subProgressBar.setVisible(false);
        subProgressLabel.setVisible(false);
    }

    @Override
    public void addBytesToDownload(long bytes) {
        this.totalBytes += bytes;
        this.updateProgressBar();
    }
}
