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
package com.atlauncher.gui.tabs.settings;

import java.awt.Dimension;
import java.awt.event.ItemEvent;

import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.listener.DelayedSavingKeyListener;
import com.atlauncher.viewmodel.impl.settings.CommandsSettingsViewModel;

public class CommandsSettingsTab extends AbstractSettingsTab {
    /** A command line is long, so its field spans the row rather than sitting beside the label. */
    private static final int COMMAND_WIDTH = 516;
    private static final int COMMAND_HEIGHT = 24;

    private final CommandsSettingsViewModel viewModel;
    private JTextField preLaunchCommand;
    private JTextField postExitCommand;
    private JTextField wrapperCommand;
    private MD3Switch enableCommands;

    public CommandsSettingsTab(CommandsSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected void onShow() {
        // region Enable Checkbox
        enableCommands = new MD3Switch();
        enableCommands.setSelected(App.settings.enableCommands);
        enableCommands.addItemListener(e -> viewModel.setEnableCommands(e.getStateChange() == ItemEvent.SELECTED));
        addRow(GetText.tr("Enable commands"),
            GetText.tr("This allows you to turn launch/exit commands on or off."), enableCommands);
        // endregion

        // region Pre-launch command
        preLaunchCommand = new JTextField(App.settings.preLaunchCommand, 32);
        preLaunchCommand.setPreferredSize(new Dimension(COMMAND_WIDTH, COMMAND_HEIGHT));
        preLaunchCommand.addKeyListener(
            new DelayedSavingKeyListener(
                100,
                () -> viewModel.setPreLaunchCommand(preLaunchCommand.getText()),
                viewModel::setPreLaunchCommandPending
            )
        );
        addDisposable(viewModel.getPreLaunchCommand().subscribe(preLaunchCommand::setText));
        addWideRow(GetText.tr("Pre-launch command"), GetText.tr(
                "This command will be run before the instance launches. The game will not run until the command has finished."),
            preLaunchCommand);
        // endregion

        // region Post-exit command
        postExitCommand = new JTextField(App.settings.postExitCommand, 32);
        postExitCommand.setPreferredSize(new Dimension(COMMAND_WIDTH, COMMAND_HEIGHT));
        postExitCommand.addKeyListener(
            new DelayedSavingKeyListener(
                100,
                () -> viewModel.setPostExitCommand(postExitCommand.getText()),
                viewModel::setPostExitCommandPending
            )
        );
        addDisposable(viewModel.getPostExitCommand().subscribe(postExitCommand::setText));
        addWideRow(GetText.tr("Post-exit command"), GetText.tr(
                "This command will be run after the instance exits. It will run even if the instance is killed or if it crashes and exits."),
            postExitCommand);
        // endregion

        // region Wrapper command
        wrapperCommand = new JTextField(App.settings.wrapperCommand, 32);
        wrapperCommand.setPreferredSize(new Dimension(COMMAND_WIDTH, COMMAND_HEIGHT));
        wrapperCommand.addKeyListener(
            new DelayedSavingKeyListener(
                100,
                () -> viewModel.setWrapperCommand(wrapperCommand.getText()),
                viewModel::setWrapperCommandPending
            )
        );
        addDisposable(viewModel.getWrapperCommand().subscribe(wrapperCommand::setText));
        addWideRow(GetText.tr("Wrapper command"), GetText.tr(
                "Wrapper command allow launcher using an extra wrapper program (like 'prime-run' on Linux)\nUse %command% to substitute launch command\n%\"command\"% to substitute launch as a whole string (like 'bash -c' on Linux)"),
            wrapperCommand);
        // endregion

        // region Information text pane
        JTextPane parameterInformation = new JTextPane();
        parameterInformation
            .setText(GetText.tr("Commands will be run in the directory of the instance that is launched/exited.")
                + System.lineSeparator() + GetText.tr("The following variables are available for each command")
                + ":" + System.lineSeparator() + "$INST_NAME: " + GetText.tr("The name of the instance")
                + System.lineSeparator() + "$INST_ID: "
                + GetText.tr("The name of the instance's root directory") + System.lineSeparator()
                + "$INST_DIR: " + GetText.tr("The absolute path to the instance directory")
                + System.lineSeparator() + "$INST_MC_DIR: " + GetText.tr("Alias for") + " $INST_DIR"
                + System.lineSeparator() + "$INST_JAVA: "
                + GetText.tr("The absolute path to the java executable used for launch")
                + System.lineSeparator() + "$INST_JAVA_ARGS: "
                + GetText.tr("The JVM parameters used for launch") + System.lineSeparator());
        parameterInformation.setEditable(false);
        parameterInformation.setOpaque(false);

        addWideRow(GetText.tr("Variables"), null, parameterInformation);
        // endregion

        addDisposable(
            viewModel.getEnableCommands().subscribe(it -> {
                if (it) {
                    enableCommands();
                } else {
                    disableCommands();
                }
            })
        );
    }

    @Override
    protected void onDestroy() {
        removeAll();
        preLaunchCommand = null;
        postExitCommand = null;
        wrapperCommand = null;
        enableCommands = null;
    }

    private void disableCommands() {
        preLaunchCommand.setEnabled(false);
        postExitCommand.setEnabled(false);
        wrapperCommand.setEnabled(false);
    }

    private void enableCommands() {
        preLaunchCommand.setEnabled(true);
        postExitCommand.setEnabled(true);
        wrapperCommand.setEnabled(true);
    }

    @Override
    public String getTitle() {
        return GetText.tr("Commands");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Commands";
    }

    @Override
    protected void createViewModel() {
    }
}
