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
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.CheckState;
import com.atlauncher.data.ScreenResolution;
import com.atlauncher.gui.components.JLabelWithHover;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3Spinner;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.listener.DelayedSavingKeyListener;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;
import com.atlauncher.viewmodel.impl.settings.JavaSettingsViewModel;
import com.formdev.flatlaf.ui.FlatScrollPaneBorder;

public class JavaSettingsTab extends AbstractSettingsTab {
    private final JavaSettingsViewModel viewModel;

    private MD3TextField javaPath;
    private JLabelWithHover javaPathChecker;
    private JTextArea javaParameters;
    private JLabelWithHover javaParamChecker;
    private MD3TextField javaInstallLocation;
    private JLabelWithHover javaInstallLocationChecker;

    public JavaSettingsTab(JavaSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected void onShow() {
        Integer systemRam = viewModel.getSystemRam();
        Integer maximumSystemRamForSpinnerModels = systemRam == null || systemRam == 0 ? null : systemRam;

        addSection(GetText.tr("Memory"));

        // Maximum Memory Settings
        SpinnerNumberModel maximumMemoryModel = new SpinnerNumberModel(App.settings.maximumMemory, null, null, 512);
        maximumMemoryModel.setMinimum(512);
        maximumMemoryModel.setMaximum(maximumSystemRamForSpinnerModels);
        MD3Spinner maximumMemory = new MD3Spinner(maximumMemoryModel);
        ((JSpinner.DefaultEditor) maximumMemory.getEditor()).getTextField().setColumns(5);
        maximumMemory.addChangeListener(e -> {
            viewModel.setMaxRam((Integer) maximumMemory.getValue());
        });
        addDisposable(viewModel.getMaxRam().subscribe(maximumMemory::setValue));

        if (viewModel.isJava32Bit()) {
            addRow(GetText.tr("Maximum Memory/Ram"),
                GetText.tr("The maximum amount of memory/ram to allocate when starting Minecraft."),
                group(new JLabelWithHover(warningIcon(), new HTMLBuilder().center().split(100).text(GetText
                        .tr("You're running a 32 bit Java and therefore cannot use more than 1GB of Ram. Please see http://atl.pw/32bit for help."))
                    .build(), RESTART_BORDER), maximumMemory));
        } else {
            addRow(GetText.tr("Maximum Memory/Ram"),
                GetText.tr("The maximum amount of memory/ram to allocate when starting Minecraft."), maximumMemory);
        }

        // Perm Gen Settings
        SpinnerNumberModel permGenModel = new SpinnerNumberModel(App.settings.metaspace, null, null, 32);
        permGenModel.setMinimum(32);
        permGenModel.setMaximum(maximumSystemRamForSpinnerModels);
        MD3Spinner permGen = new MD3Spinner(permGenModel);
        ((JSpinner.DefaultEditor) permGen.getEditor()).getTextField().setColumns(3);
        permGen.addChangeListener(e -> {
            boolean result = viewModel.setPermGen((Integer) permGen.getValue());

            if (result) {
                viewModel.setPermgenWarningShown();
                int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Warning"))
                    .setType(DialogManager.WARNING)
                    .setContent(GetText.tr(
                        "Setting PermGen size above {0}MB is not recommended and can cause issues. Are you sure you want to do this?",
                        viewModel.getPermGenMaxRecommendSize()))
                    .show();

                if (ret != 0) {
                    permGen.setValue(viewModel.getPermGenMaxRecommendSize());
                }
            }
        });
        addDisposable(viewModel.getMetaspace().subscribe(permGen::setValue));

        addRow(GetText.tr("PermGen Size"),
            GetText.tr("The PermGen Size for java to use when launching Minecraft in MB."), permGen);

        addSection(GetText.tr("Minecraft Window"));

        // Window Size
        SpinnerNumberModel widthModel = new SpinnerNumberModel(App.settings.windowWidth, 1, OS.getMaximumWindowWidth(),
            1);
        MD3Spinner widthField = new MD3Spinner(widthModel);
        widthField.setEditor(new JSpinner.NumberEditor(widthField, "#"));
        widthField.addChangeListener(e -> viewModel.setWidth((Integer) widthModel.getValue()));
        addDisposable(viewModel.getWidth().subscribe(widthModel::setValue));

        SpinnerNumberModel heightModel = new SpinnerNumberModel(App.settings.windowHeight, 1,
            OS.getMaximumWindowHeight(), 1);
        MD3Spinner heightField = new MD3Spinner(heightModel);
        heightField.setEditor(new JSpinner.NumberEditor(heightField, "#"));
        heightField.addChangeListener(e -> viewModel.setHeight((Integer) heightField.getValue()));
        addDisposable(viewModel.getHeight().subscribe(heightField::setValue));

        MD3ComboBox<ComboItem<ScreenResolution>> commonScreenSizes = new MD3ComboBox<>();
        commonScreenSizes.addItem(new ComboItem<>(null, "Select An Option"));

        for (ScreenResolution resolution : viewModel.getScreenResolutions()) {
            commonScreenSizes.addItem(new ComboItem<>(resolution, resolution.toString()));
        }
        commonScreenSizes.addActionListener(e -> {
            Object selectedItem = commonScreenSizes.getSelectedItem();
            if (selectedItem == null)
                return;

            @SuppressWarnings("unchecked")
            ComboItem<ScreenResolution> selected = (ComboItem<ScreenResolution>) selectedItem;

            ScreenResolution screenResolution = selected.getValue();

            if (screenResolution != null)
                viewModel.setScreenResolution(screenResolution);
        });
        commonScreenSizes.setPreferredSize(new Dimension(commonScreenSizes.getPreferredSize().width + 10,
            commonScreenSizes.getPreferredSize().height));

        addRow(GetText.tr("Window Size"),
            GetText.tr("The size that the Minecraft window should open as, Width x Height, in pixels."),
            group(widthField, new JLabel("x"), heightField, commonScreenSizes));

        // Start Minecraft Maximised

        MD3Switch startMinecraftMaximised = new MD3Switch();
        startMinecraftMaximised.addItemListener(
            itemEvent -> viewModel.setStartMinecraftMax(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getMaximizeMinecraft().subscribe(startMinecraftMaximised::setSelected));

        addRow(GetText.tr("Start Minecraft Maximised"),
            GetText.tr("Enabling this will start Minecraft maximised so that it takes up the full size of your screen."),
            startMinecraftMaximised);

        addSection(GetText.tr("Java"));

        // Java Path

        // no size of its own: it goes into a wide row, which gives it the width, and a control that
        // paints its own container knows its own height. The 24px it used to be pinned to was
        // shorter than the container and clipped the value through the middle.
        MD3ComboBox<ComboItem<String>> installedJavasComboBox = new MD3ComboBox<>();

        installedJavasComboBox.addItem(new ComboItem<String>(null, GetText.tr("Select Java Path To Autofill")));

        for (String javaInfo : viewModel.getJavaPaths()) {
            installedJavasComboBox.addItem(new ComboItem<>(javaInfo, javaInfo));
        }

        boolean hasInstalledJavas = installedJavasComboBox.getItemCount() != 1;

        if (hasInstalledJavas) {
            installedJavasComboBox.addActionListener(e -> {
                ComboItem<String> path = ((ComboItem<String>) installedJavasComboBox.getSelectedItem());
                String value = path.getValue();
                if (value != null)
                    viewModel.setJavaPath(value);
            });
        }

        javaPath = new MD3TextField(32);
        javaPathChecker = new JLabelWithHover("", null, null);
        javaPath.addKeyListener(new DelayedSavingKeyListener(
            500,
            () -> viewModel.setJavaPath(javaPath.getText()),
            viewModel::setJavaPathPending));

        addDisposable(viewModel.getJavaPathObservable().subscribe(path -> {
            if (!javaPath.getText().equals(path))
                javaPath.setText(path);
        }));
        javaPath.setText(App.settings.javaPath);
        addDisposable(viewModel.getJavaPathChecker().subscribe(this::setJavaPathCheckState));

        MD3Button javaPathResetButton = MD3Button.outlined(GetText.tr("Reset"));
        javaPathResetButton.addActionListener(e -> {
            viewModel.resetJavaPath();
            resetJavaPathCheckLabel();
        });
        MD3Button javaBrowseButton = MD3Button.outlined(GetText.tr("Browse"));
        javaBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(javaPath.getText()));
            chooser.setDialogTitle(GetText.tr("Select"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedPath = chooser.getSelectedFile();
                File jPath = new File(selectedPath, "bin");
                File javaExe = new File(selectedPath, "java.exe");
                File javaExecutable = new File(selectedPath, "java");

                // user selected the bin dir
                if (!jPath.exists() && (javaExe.exists() || javaExecutable.exists())) {
                    viewModel.setJavaPath(selectedPath.getParent());
                } else {
                    viewModel.setJavaPath(selectedPath.getAbsolutePath());
                }
                viewModel.setJavaPathPending();
            }
        });

        if (hasInstalledJavas) {
            addWideRow(GetText.tr("Detected Java Installs"), GetText.tr("Select Java Path To Autofill"),
                installedJavasComboBox);
        }

        addWideRow(GetText.tr("Java Path"), GetText.tr(
                "This setting allows you to specify where your Java Path is. Where possible the launcher will use a version of Java provided by Minecraft to launch the instance, but in cases where one isn't available, this path will be used."),
            group(javaPath, javaPathChecker, javaPathResetButton, javaBrowseButton));

        // Java Paramaters

        JScrollPane javaParametersScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        javaParametersScrollPane.setBorder(new FlatScrollPaneBorder());
        javaParametersScrollPane.setMaximumSize(new Dimension(1000, 200));

        javaParameters = new JTextArea(6, 40);
        ((AbstractDocument) javaParameters.getDocument()).setDocumentFilter(
            new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                    fb.insertString(offset, string.replaceAll("[\n\r]", ""), attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                    fb.replace(offset, length, text.replaceAll("[\n\r]", ""), attrs);
                }
            });
        javaParamChecker = new JLabelWithHover("", null, null);
        javaParameters.setLineWrap(true);
        javaParameters.setWrapStyleWord(true);
        javaParameters.addKeyListener(new DelayedSavingKeyListener(
            500,
            () -> viewModel.setJavaParams(javaParameters.getText()),
            viewModel::setJavaParamsPending));
        addDisposable(viewModel.getJavaParams().subscribe(params -> {
            if (!javaParameters.getText().equals(params)) {
                javaParameters.setText(params);
            }
        }));
        addDisposable(viewModel.getJavaParamsChecker().subscribe(this::setJavaParamCheckState));

        MD3Button javaParametersResetButton = MD3Button.outlined(GetText.tr("Reset"));
        javaParametersResetButton.addActionListener(e -> viewModel.resetJavaParams());

        javaParametersScrollPane.setViewportView(javaParameters);

        addWideRow(GetText.tr("Java Parameters"),
            GetText.tr("Extra Java command line paramaters can be added here."),
            group(javaParametersScrollPane, javaParamChecker, javaParametersResetButton));

        // Jave Install Location

        javaInstallLocation = new MD3TextField(32);
        javaInstallLocationChecker = new JLabelWithHover("", null, null);
        javaInstallLocation.addKeyListener(new DelayedSavingKeyListener(
            500,
            () -> viewModel.setJavaInstallLocation(javaInstallLocation.getText()),
            viewModel::setJavaInstallLocationPending));

        addDisposable(viewModel.getJavaInstallLocationObservable().subscribe(folder -> {
            if (!javaInstallLocation.getText().equals(folder))
                javaInstallLocation.setText(folder);
        }));
        javaInstallLocation.setText(App.settings.javaInstallLocation);
        addDisposable(viewModel.getJavaInstallLocationChecker().subscribe(this::setJavaInstallLocationState));

        MD3Button javaInstallLocationBrowseButton = MD3Button.outlined(GetText.tr("Browse"));
        javaInstallLocationBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(javaInstallLocation.getText()));
            chooser.setDialogTitle(GetText.tr("Select"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                viewModel.setJavaInstallLocation(chooser.getSelectedFile().getAbsolutePath());

                if (!chooser.getSelectedFile().getAbsolutePath().isEmpty()) {
                    viewModel.setJavaInstallLocationPending();
                }
            }
        });

        addWideRow(GetText.tr("Java Install Location"), GetText.tr(
                "This setting allows you to specify a common location that you install all your Java installs to. This helps find your installed Java installs easier if you install them all within 1 folder."),
            group(javaInstallLocation, javaInstallLocationChecker, javaInstallLocationBrowseButton));

        // Ignore Java checks On Launch

        MD3Switch ignoreJavaOnInstanceLaunch = new MD3Switch();
        ignoreJavaOnInstanceLaunch.addItemListener(
            itemEvent -> viewModel.setIgnoreJavaChecks(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getIgnoreJavaOnInstanceLaunch().subscribe(ignoreJavaOnInstanceLaunch::setSelected));

        addRow(GetText.tr("Ignore Java Checks On Launch"), GetText.tr(
                "This enables ignoring errors when launching a pack that you don't have a compatible Java version for."),
            ignoreJavaOnInstanceLaunch);

        // Use Java Provided By Minecraft

        MD3Switch useJavaProvidedByMinecraft = new MD3Switch();
        addDisposable(viewModel.getUseJavaProvidedByMinecraft().subscribe(useJavaProvidedByMinecraft::setSelected));
        useJavaProvidedByMinecraft.setEnabled(viewModel.getUseJavaFromMinecraftEnabled());
        useJavaProvidedByMinecraft.addItemListener(e -> {
            boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
            viewModel.setJavaFromMinecraft(enabled);

            if (!enabled) {
                SwingUtilities.invokeLater(() -> {
                    int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Warning"))
                        .setType(DialogManager.WARNING)
                        .setContent(GetText.tr(
                            "Unchecking this is not recommended and may cause Minecraft to no longer run. Are you sure you want to do this?"))
                        .show();

                    if (ret != 0) {
                        useJavaProvidedByMinecraft.setSelected(true);
                    }
                });
            }
        });

        addRow(GetText.tr("Use Java Provided By Minecraft"), GetText.tr(
                "This allows you to enable/disable using the version of Java provided by the version of Minecraft you're running. It's highly recommended to not disable this, unless you know what you're doing."),
            useJavaProvidedByMinecraft);

        addSection(GetText.tr("Advanced"));

        // Disable Legacy Launching

        MD3Switch disableLegacyLaunching = new MD3Switch();
        disableLegacyLaunching.addItemListener(
            itemEvent -> viewModel.setDisableLegacyLaunching(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(
            viewModel.getDisableLegacyLaunching().subscribe(disableLegacyLaunching::setSelected));

        addRow(GetText.tr("Disable Legacy Launching"), GetText.tr(
                "This allows you to disable legacy launching for Minecraft < 1.6. It's highly recommended to not disable this, unless you're having issues launching older Minecraft versions."),
            disableLegacyLaunching);

        // Use System GLFW

        MD3Switch useSystemGlfw = new MD3Switch();
        useSystemGlfw.addItemListener(
            itemEvent -> viewModel.setSystemGLFW(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getSystemGLFW().subscribe(useSystemGlfw::setSelected));

        addRow(GetText.tr("Use System GLFW"), GetText.tr("Use the systems install for GLFW native library."),
            useSystemGlfw);

        // Use System OpenAL

        MD3Switch useSystemOpenAl = new MD3Switch();
        useSystemOpenAl.addItemListener(
            itemEvent -> viewModel.setSystemOpenAL(itemEvent.getStateChange() == ItemEvent.SELECTED));
        addDisposable(viewModel.getSystemOpenAL().subscribe(useSystemOpenAl::setSelected));

        addRow(GetText.tr("Use System OpenAL"), GetText.tr("Use the systems install for OpenAL native library."),
            useSystemOpenAl);
    }

    private void showJavaPathWarning() {
        DialogManager.okDialog()
            .setTitle(GetText.tr("Help"))
            .setContent(
                new HTMLBuilder()
                    .center()
                    .text(
                        GetText.tr(
                            "The Java Path you set is incorrect.<br/><br/>Please verify it points to the folder where the bin folder is and try again."))
                    .build())
            .setType(DialogManager.ERROR)
            .show();
    }

    private void showJavaParamWarning() {
        DialogManager.okDialog()
            .setTitle(GetText.tr("Help"))
            .setContent(
                new HTMLBuilder()
                    .center()
                    .text(
                        GetText.tr(
                            "The entered Java Parameters were incorrect.<br/><br/>Please remove any references to Xmx or XX:PermSize."))
                    .build())
            .setType(DialogManager.ERROR)
            .show();
    }

    private void showJavaInstallLocationWarning() {
        DialogManager.okDialog()
            .setTitle(GetText.tr("Help"))
            .setContent(
                new HTMLBuilder()
                    .center()
                    .text(
                        GetText.tr(
                            "The Java Install Location Path you set is incorrect.<br/><br/>Please verify it points to a folder and try again."))
                    .build())
            .setType(DialogManager.ERROR)
            .show();
    }

    @Override
    public String getTitle() {
        return GetText.tr("Java/Minecraft");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Java/Minecraft";
    }

    @Override
    protected void createViewModel() {
    }

    private void setLabelState(JLabelWithHover label, String tooltip, String path) {
        try {
            label.setToolTipText(tooltip);
            ImageIcon icon = Utils.getIconImage(path);
            if (icon != null) {
                label.setIcon(icon);
                icon.setImageObserver(label);
            }
        } catch (NullPointerException ignored) {
            // ignored
        }
    }

    private void resetJavaPathCheckLabel() {
        javaPathChecker.setText("");
        javaPathChecker.setIcon(null);
        javaPathChecker.setToolTipText(null);
    }

    private void setJavaPathCheckState(CheckState state) {
        if (state == CheckState.NotChecking) {
            resetJavaPathCheckLabel();
        } else if (state == CheckState.CheckPending) {
            setLabelState(javaPathChecker, GetText.tr("Java path change pending"), "/assets/icon/warning.png");
        } else if (state == CheckState.Checking) {
            setLabelState(javaPathChecker, GetText.tr("Checking java path"), "/assets/image/loading-bars-small.gif");

            javaPath.setEnabled(false);
        } else if (state instanceof CheckState.Checked) {
            if (((CheckState.Checked) state).valid) {
                resetJavaPathCheckLabel();
            } else {
                setLabelState(javaPathChecker, GetText.tr("Invalid!"), "/assets/icon/error.png");
                showJavaPathWarning();
            }
            javaPath.setEnabled(true);
        }
    }

    private void resetJavaParamCheckLabel() {
        javaParamChecker.setText("");
        javaParamChecker.setIcon(null);
        javaParamChecker.setToolTipText(null);
    }

    private void setJavaParamCheckState(CheckState state) {
        if (state == CheckState.NotChecking) {
            resetJavaParamCheckLabel();
        } else if (state == CheckState.CheckPending) {
            setLabelState(javaParamChecker, GetText.tr("Java params change pending"), "/assets/icon/warning.png");
        } else if (state == CheckState.Checking) {
            setLabelState(javaParamChecker, GetText.tr("Checking java params"), "/assets/image/loading-bars-small.gif");

            javaParameters.setEnabled(false);
        } else if (state instanceof CheckState.Checked) {
            if (((CheckState.Checked) state).valid) {
                resetJavaParamCheckLabel();
            } else {
                setLabelState(javaParamChecker, GetText.tr("Invalid!"), "/assets/icon/error.png");
                showJavaParamWarning();
            }
            javaParameters.setEnabled(true);
        }
    }

    private void resetJavaInstallLocationCheckLabel() {
        javaInstallLocationChecker.setText("");
        javaInstallLocationChecker.setIcon(null);
        javaInstallLocationChecker.setToolTipText(null);
    }

    private void setJavaInstallLocationState(CheckState state) {
        if (state == CheckState.NotChecking) {
            resetJavaInstallLocationCheckLabel();
        } else if (state == CheckState.CheckPending) {
            setLabelState(javaInstallLocationChecker, GetText.tr("Java install location change pending"),
                "/assets/icon/warning.png");
        } else if (state == CheckState.Checking) {
            setLabelState(javaInstallLocationChecker, GetText.tr("Checking java install location path"),
                "/assets/image/loading-bars-small.gif");

            javaInstallLocation.setEnabled(false);
        } else if (state instanceof CheckState.Checked) {
            if (((CheckState.Checked) state).valid) {
                resetJavaInstallLocationCheckLabel();
            } else {
                setLabelState(javaInstallLocationChecker, GetText.tr("Invalid!"), "/assets/icon/error.png");
                showJavaInstallLocationWarning();
            }
            javaInstallLocation.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        removeAll();
        javaPath = null;
        javaPathChecker = null;
        javaParamChecker = null;
        javaInstallLocationChecker = null;
        javaParameters = null;
    }
}
