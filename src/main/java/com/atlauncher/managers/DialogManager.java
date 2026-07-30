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
package com.atlauncher.managers;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.gui.md3.feedback.MD3Dialog;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Type;

public final class DialogManager {
    /** Wide enough for a pack name or a URL, which is what these ask for. */
    private static final int INPUT_COLUMNS = 28;

    public static final int OPTION_TYPE = 0;
    public static final int CONFIRM_TYPE = 1;
    public static final int OK_TYPE = 1;

    public static final int ERROR = JOptionPane.ERROR_MESSAGE;
    public static final int INFO = JOptionPane.INFORMATION_MESSAGE;
    public static final int WARNING = JOptionPane.WARNING_MESSAGE;
    public static final int QUESTION = JOptionPane.QUESTION_MESSAGE;

    public static final int DEFAULT_OPTION = JOptionPane.DEFAULT_OPTION;
    public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;

    public static final int YES_OPTION = JOptionPane.YES_OPTION;
    public static final int NO_OPTION = JOptionPane.NO_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

    public int dialogType;
    public Window parent;
    public String title;
    public Object content;
    public List<String> options = new ArrayList<>();
    public String defaultOption = null;
    public int type = DialogManager.QUESTION;

    private DialogManager(int dialogType) {
        this.dialogType = dialogType;
    }

    public static DialogManager optionDialog() {
        return new DialogManager(DialogManager.OPTION_TYPE);
    }

    public static DialogManager confirmDialog() {
        return new DialogManager(DialogManager.CONFIRM_TYPE);
    }

    public static DialogManager okDialog() {
        DialogManager dialog = new DialogManager(DialogManager.CONFIRM_TYPE);

        dialog.addOption(GetText.tr("Ok"), true);

        return dialog;
    }

    public static DialogManager okCancelDialog() {
        DialogManager dialog = new DialogManager(DialogManager.CONFIRM_TYPE);

        dialog.addOption(GetText.tr("Ok"), true);
        dialog.addOption(GetText.tr("Cancel"));

        return dialog;
    }

    public static DialogManager yesNoDialog() {
        return yesNoDialog(true);
    }

    public static DialogManager yesNoDialog(boolean yesDefault) {
        DialogManager dialog = new DialogManager(DialogManager.CONFIRM_TYPE);

        dialog.addOption(GetText.tr("Yes"), yesDefault);
        dialog.addOption(GetText.tr("No"), !yesDefault);

        return dialog;
    }

    public static DialogManager yesNoCancelDialog() {
        DialogManager dialog = new DialogManager(DialogManager.CONFIRM_TYPE);

        dialog.addOption(GetText.tr("Yes"), true);
        dialog.addOption(GetText.tr("No"));
        dialog.addOption(GetText.tr("Cancel"));

        return dialog;
    }

    public DialogManager setParent(Window parent) {
        this.parent = parent;
        return this;
    }

    public DialogManager setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogManager setContent(Object content) {
        this.content = content;
        return this;
    }

    public DialogManager setType(int type) {
        this.type = type;
        return this;
    }

    public DialogManager setDefaultOption(String defaultOption) {
        this.defaultOption = defaultOption;
        return this;
    }

    public DialogManager addOption(String option, boolean isDefault) {
        this.options.add(option);

        if (isDefault) {
            this.defaultOption = option;
        }

        return this;
    }

    public DialogManager addOption(String option) {
        return this.addOption(option, false);
    }

    public Object[] getOptions() {
        if (this.options.isEmpty()) {
            return null;
        }

        return this.options.toArray();
    }

    public Window getParent() {
        if (this.parent != null) {
            return this.parent;
        }

        return parentWindow();
    }

    /**
     * The window a dialog should belong to when the caller has not said.
     *
     * <p>
     * Static because most callers are not building a {@link DialogManager} - anything showing its
     * own dialog needs the same answer, and there should be one place that knows it.
     */
    public static Window parentWindow() {
        if (App.settings != null && App.launcher != null && App.launcher.getParent() != null) {
            return App.launcher.getParent();
        }

        return JOptionPane.getRootFrame();
    }

    /**
     * The order the actions are shown in, which is not the order they were added in.
     *
     * <p>
     * Material puts the action that proceeds on the trailing edge and everything that backs out to
     * its left, whereas these are declared with the default first - {@code yesNoDialog} adds Yes
     * then No. So the default is moved last and the rest keep their relative order, and the array
     * maps each position back to the index the caller is expecting to be told.
     */
    private int[] displayOrder() {
        int[] order = new int[this.options.size()];
        int at = 0;

        for (int i = 0; i < this.options.size(); i++) {
            if (!this.options.get(i).equals(this.defaultOption)) {
                order[at++] = i;
            }
        }

        for (int i = 0; i < this.options.size(); i++) {
            if (this.options.get(i).equals(this.defaultOption)) {
                order[at++] = i;
            }
        }

        return order;
    }

    /**
     * A hero icon for the dialogs that need the weight of one. A question is routine and an
     * announcement is not an alarm, so neither gets one.
     */
    private MD3Icon.Painter icon() {
        if (this.type == ERROR) {
            return MD3Icons.ERROR;
        }

        if (this.type == WARNING) {
            return MD3Icons.WARNING;
        }

        return null;
    }

    /**
     * Applies whatever the caller passed as content.
     *
     * <p>
     * Most of these are strings the caller has already built into HTML, usually through
     * {@link com.atlauncher.builders.HTMLBuilder}. Those go in as their own component rather than as
     * supporting text, which escapes what it is given - it has no way to know whether a string is
     * markup or a sentence that happens to contain a bracket.
     */
    private void applyContent(MD3Dialog.Builder builder) {
        if (this.content == null) {
            return;
        }

        if (this.content instanceof JComponent) {
            builder.content((JComponent) this.content);

            return;
        }

        String text = String.valueOf(this.content);

        if (text.trim().toLowerCase(Locale.ENGLISH).startsWith("<html")) {
            JLabel label = new JLabel(text);
            label.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, text));
            label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
            label.setForeground(MD3Color.onSurfaceVariant());

            builder.content(label);

            return;
        }

        builder.supportingText(text);
    }

    /**
     * Builds the dialog, ready to be shown.
     *
     * <p>
     * The title becomes the headline as well as the window title: a Material dialog is undecorated
     * wherever the platform can round its corners, and a window title nobody can see is no title.
     */
    private MD3Dialog buildDialog(int[] order) {
        MD3Dialog.Builder builder = MD3Dialog.builder(this.getParent())
                .title(this.title == null ? "" : this.title)
                .headline(this.title)
                .icon(icon());

        applyContent(builder);

        for (int i = 0; i < order.length; i++) {
            String option = this.options.get(order[i]);

            if (i == order.length - 1) {
                builder.confirm(option);
            } else {
                builder.dismiss(option);
            }
        }

        return builder.build();
    }

    /**
     * Translates what the dialog reported back into the index of the option the caller added.
     */
    private int resultOf(int chosen, int[] order) {
        if (chosen < 0 || chosen >= order.length) {
            return CLOSED_OPTION;
        }

        return order[chosen];
    }

    public int show() {
        try {
            int[] order = displayOrder();

            return resultOf(buildDialog(order).showAndWait(), order);
        } catch (Exception e) {
            LogManager.logStackTrace(e, false);
        }

        return CLOSED_OPTION;
    }

    public int showWithFileMonitoring(File firstFile, File secondFile, int size, int returnValue) {
        if (secondFile != null) {
            return showWithFileMonitoring(size, returnValue, firstFile, secondFile);
        }

        return showWithFileMonitoring(size, returnValue, firstFile);
    }

    /**
     * Shows the dialog, and answers it on the user's behalf if the file it is waiting for turns up.
     */
    public int showWithFileMonitoring(int size, int returnValue, File... files) {
        try {
            int[] order = displayOrder();
            MD3Dialog dialog = buildDialog(order);

            List<File> filesForMonitoring = Arrays.asList(files);
            int chooses = position(order, returnValue);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (filesForMonitoring.stream().anyMatch(f -> f.exists() && f.length() == size)) {
                        timer.cancel();
                        SwingUtilities.invokeLater(() -> dialog.choose(chooses));
                    }
                }
            }, 1000, 1000);

            int result = resultOf(dialog.showAndWait(), order);

            // make sure this timer gets killed
            timer.cancel();

            return result;
        } catch (Exception e) {
            LogManager.logStackTrace(e, false);
        }

        return -1;
    }

    /**
     * Where an option ended up once the actions were put in Material's order.
     */
    private static int position(int[] order, int option) {
        for (int i = 0; i < order.length; i++) {
            if (order[i] == option) {
                return i;
            }
        }

        return order.length - 1;
    }

    public String showInput() {
        return showInput("");
    }

    /**
     * Asks for a line of text.
     *
     * @return what was typed, or null if the dialog was cancelled or dismissed - as
     *         {@code JOptionPane} did, so callers that null-check keep working
     */
    public String showInput(String defaultValue) {
        try {
            MD3TextField field = new MD3TextField(this.content == null ? null : String.valueOf(this.content));
            field.setText(defaultValue);
            field.setColumns(INPUT_COLUMNS);

            MD3Dialog dialog = MD3Dialog.builder(this.getParent())
                    .title(this.title == null ? "" : this.title)
                    .headline(this.title)
                    .content(field)
                    .dismiss(GetText.tr("Cancel"))
                    .confirm(GetText.tr("Ok"))
                    .build();

            // the field is what the dialog is for, so it starts focused with the default selected -
            // typing replaces it rather than appending to it
            SwingUtilities.invokeLater(() -> {
                field.requestFocusInWindow();
                field.selectAll();
            });

            return dialog.showAndWait() == 1 ? field.getText() : null;
        } catch (Exception e) {
            LogManager.logStackTrace(e, false);
        }

        return null;
    }
}
