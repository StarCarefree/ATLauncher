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
package com.atlauncher.gui.dialogs.instancesettings;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.swing.JComponent;
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
import com.atlauncher.Data;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.Instance;
import com.atlauncher.data.minecraft.JavaRuntime;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3SettingsList;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3Spinner;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.Java;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.javafinder.JavaInfo;
import com.formdev.flatlaf.ui.FlatScrollPaneBorder;

public class JavaInstanceSettingsTab extends MD3SettingsList {
    private final Instance instance;

    private MD3Spinner maximumMemory;
    private MD3Spinner permGen;
    private MD3TextField javaPath;
    private JTextArea javaParameters;
    private MD3ComboBox<ComboItem<String>> javaRuntimeOverride;
    private MD3ComboBox<ComboItem<Boolean>> useJavaProvidedByMinecraft;
    private MD3ComboBox<ComboItem<Boolean>> disableLegacyLaunching;
    private MD3ComboBox<ComboItem<Boolean>> useSystemGlfw;
    private MD3ComboBox<ComboItem<Boolean>> useSystemOpenAl;

    /**
     * The Java rows that swap over: either Minecraft provides the runtime, in which case there is
     * nothing to set but which runtime, or it does not and the path is the instance's own.
     */
    private JComponent providedJavaRow;
    private JComponent runtimeOverrideRow;
    private JComponent javaPathRow;
    @Nullable
    private JComponent detectedJavasRow;

    private boolean permgenWarningShown = false;

    public JavaInstanceSettingsTab(Instance instance) {
        this.instance = instance;

        setupComponents();
    }

    private void setupComponents() {
        int systemRam = OS.getSystemRam();

        addSection(GetText.tr("Memory"));

        // Maximum Memory Settings
        SpinnerNumberModel maximumMemoryModel = new SpinnerNumberModel(
            getIfNotNull(this.instance.launcher.maximumMemory, App.settings.maximumMemory), null, null, 512);
        maximumMemoryModel.setMinimum(512);
        maximumMemoryModel.setMaximum((systemRam == 0 ? null : systemRam));
        maximumMemory = new MD3Spinner(maximumMemoryModel);
        ((JSpinner.DefaultEditor) maximumMemory.getEditor()).getTextField().setColumns(5);

        addRow(GetText.tr("Maximum Memory/Ram"),
            GetText.tr("The maximum amount of memory/ram to allocate when starting Minecraft."), maximumMemory);

        // Perm Gen Settings
        SpinnerNumberModel permGenModel = new SpinnerNumberModel(
            getIfNotNull(this.instance.launcher.permGen, App.settings.metaspace), null, null, 32);
        permGenModel.setMinimum(32);
        permGenModel.setMaximum((systemRam == 0 ? null : systemRam));
        permGen = new MD3Spinner(permGenModel);
        ((JSpinner.DefaultEditor) permGen.getEditor()).getTextField().setColumns(3);
        permGen.addChangeListener(e -> {
            JSpinner s = (JSpinner) e.getSource();
            int permGenMaxRecommendedSize = (OS.is64Bit() ? 256 : 128);

            if ((Integer) s.getValue() > permGenMaxRecommendedSize && !permgenWarningShown) {
                permgenWarningShown = true;
                int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Warning"))
                    .setType(DialogManager.WARNING)
                    .setContent(GetText.tr(
                        "Setting PermGen size above {0}MB is not recommended and can cause issues. Are you sure you want to do this?",
                        permGenMaxRecommendedSize))
                    .show();

                if (ret != 0) {
                    permGen.setValue(permGenMaxRecommendedSize);
                }
            }
        });

        addRow(GetText.tr("PermGen Size"),
            GetText.tr("The PermGen Size for java to use when launching Minecraft in MB."), permGen);

        addSection(GetText.tr("Java"));

        // Java Path, when Minecraft brings its own and there is nothing to set

        JLabel javaPathDummy = new JLabel("Uses Java provided by Minecraft");
        javaPathDummy.setEnabled(false);

        providedJavaRow = addRow(GetText.tr("Java Path"), MD3Text.plain(GetText.tr(
            "This version of Minecraft provides a specific version of Java to be used with it, so you cannot set a custom Java path.<br/><br/>In order to manually set a path, you must disable this option (highly not recommended).")),
            javaPathDummy);

        // Java Path

        javaPath = new MD3TextField(32);
        javaPath.setText(getIfNotNull(this.instance.launcher.javaPath, App.settings.javaPath));

        MD3Button javaPathResetButton = MD3Button.outlined(GetText.tr("Reset"));
        javaPathResetButton.addActionListener(e -> javaPath.setText(OS.getDefaultJavaPath()));
        MD3Button javaBrowseButton = MD3Button.outlined(GetText.tr("Browse"));
        javaBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(javaPath.getText()));
            chooser.setDialogTitle(GetText.tr("Select path to Java install"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedPath = chooser.getSelectedFile();
                File jPath = new File(selectedPath, "bin");
                File javaExe = new File(selectedPath, "java.exe");
                File javaExecutable = new File(selectedPath, "java");

                // user selected the bin dir
                if (!jPath.exists() && (javaExe.exists() || javaExecutable.exists())) {
                    javaPath.setText(selectedPath.getParent());
                } else {
                    javaPath.setText(selectedPath.getAbsolutePath());
                }
            }
        });

        // no size of its own: the wide row gives it the width, and a control that paints its own
        // container knows its own height
        MD3ComboBox<ComboItem<JavaInfo>> installedJavasComboBox = new MD3ComboBox<>();
        installedJavasComboBox.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
        List<JavaInfo> installedJavas = Java.getInstalledJavas();
        int selectedIndex = 0;

        for (JavaInfo javaInfo : installedJavas) {
            installedJavasComboBox.addItem(new ComboItem<>(javaInfo, javaInfo.toString()));

            if (javaInfo.rootPath
                .equalsIgnoreCase(getIfNotNull(this.instance.launcher.javaPath, App.settings.javaPath))) {
                selectedIndex = installedJavasComboBox.getItemCount() - 2;
            }
        }

        if (installedJavasComboBox.getItemCount() != 1) {
            installedJavasComboBox.setSelectedIndex(selectedIndex);
            installedJavasComboBox.addActionListener(e -> {
                JavaInfo selectedItem = ((ComboItem<JavaInfo>) installedJavasComboBox.getSelectedItem()).getValue();

                if (selectedItem == null) {
                    javaPath.setText("");
                } else {
                    javaPath.setText(selectedItem.rootPath);
                }
            });

            detectedJavasRow = addWideRow(GetText.tr("Detected Java Installs"),
                GetText.tr("Select Java Path To Autofill"), installedJavasComboBox);
        }

        javaPathRow = addWideRow(GetText.tr("Java Path"), MD3Text.plain(GetText.tr(
            "This setting allows you to specify where your Java Path is.<br/><br/>This should be left as default, but if you know what you're doing, just set<br/>this to the path where the bin folder is for the version of Java you want to use.<br/><br/>If you mess up, click the Reset button to go back to the default")),
            group(javaPath, javaPathResetButton, javaBrowseButton));

        boolean isUsingMinecraftProvidedJava = Optional.ofNullable(instance.launcher.useJavaProvidedByMinecraft)
            .orElse(App.settings.useJavaProvidedByMinecraft);

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
        javaParameters.setText(getIfNotNull(this.instance.launcher.javaArguments, App.settings.javaParameters));
        javaParameters.setLineWrap(true);
        javaParameters.setWrapStyleWord(true);
        javaParametersScrollPane.setViewportView(javaParameters);

        MD3Button javaParametersResetButton = MD3Button.outlined(GetText.tr("Reset"));
        javaParametersResetButton.addActionListener(e -> javaParameters.setText(App.settings.javaParameters));

        addWideRow(GetText.tr("Java Parameters"),
            GetText.tr("Extra Java command line paramaters can be added here."),
            group(javaParametersScrollPane, javaParametersResetButton));

        // Runtime Override

        javaRuntimeOverride = new MD3ComboBox<>();
        if (instance.javaVersion != null) {
            javaRuntimeOverride.addItem(new ComboItem<>(null, GetText.tr("Use Default (Recommended)")));
        } else {
            javaRuntimeOverride.addItem(new ComboItem<>(null, GetText.tr("Use System Java")));
        }

        int selectedIndexRuntime = 0;
        Map<String, List<JavaRuntime>> runtimes = Data.JAVA_RUNTIMES.getForSystem();
        for (String runtime : runtimes.keySet()) {
            List<JavaRuntime> runtimeObject = runtimes.get(runtime);

            if (runtimeObject != null && !runtimeObject.isEmpty()) {
                javaRuntimeOverride.addItem(
                    new ComboItem<>(runtime,
                        String.format("%s (Java %s)", runtime, runtimeObject.get(0).version.name)));

                if (this.instance.launcher.javaRuntimeOverride != null
                    && this.instance.launcher.javaRuntimeOverride.equals(runtime)) {
                    selectedIndexRuntime = javaRuntimeOverride.getItemCount() - 1;
                }
            }
        }

        javaRuntimeOverride.setSelectedIndex(selectedIndexRuntime);

        runtimeOverrideRow = addRow(GetText.tr("Runtime Override"), MD3Text.plain(GetText.tr(
            "This allows you to override which runtime is used to launch this instance.<br/><br/>Runtimes are provided by Mojang and used to launch the game and generally correspond to a particular Java version.<br/><br/>Changing this is usually not required or recommended.")),
            javaRuntimeOverride);

        // Use Java Provided By Minecraft

        useJavaProvidedByMinecraft = new MD3ComboBox<>();
        useJavaProvidedByMinecraft.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
        useJavaProvidedByMinecraft.addItem(new ComboItem<>(true, GetText.tr("Yes")));
        useJavaProvidedByMinecraft.addItem(new ComboItem<>(false, GetText.tr("No")));

        if (instance.launcher.useJavaProvidedByMinecraft == null) {
            useJavaProvidedByMinecraft.setSelectedIndex(0);
        } else if (instance.launcher.useJavaProvidedByMinecraft) {
            useJavaProvidedByMinecraft.setSelectedIndex(1);
        } else {
            useJavaProvidedByMinecraft.setSelectedIndex(2);
        }

        useJavaProvidedByMinecraft.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                SwingUtilities.invokeLater(() -> {
                    if (useJavaProvidedByMinecraft.getSelectedIndex() == 2) {
                        int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Warning"))
                            .setType(DialogManager.WARNING)
                            .setContent(GetText.tr(
                                "Unchecking this is not recommended and may cause Minecraft to no longer run. Are you sure you want to do this?"))
                            .show();

                        if (ret != 0) {
                            useJavaProvidedByMinecraft.setSelectedIndex(0);
                        } else {
                            showJavaRows(false);
                        }
                    } else if (useJavaProvidedByMinecraft.getSelectedIndex() == 1) {
                        showJavaRows(true);
                    } else {
                        showJavaRows(App.settings.useJavaProvidedByMinecraft);
                    }
                });
            }
        });

        JComponent useJavaProvidedByMinecraftRow = addRow(GetText.tr("Use Java Provided By Minecraft"),
            GetText.tr(
                "This allows you to enable/disable using the version of Java provided by the version of Minecraft you're running. It's highly recommended to not disable this, unless you know what you're doing."),
            useJavaProvidedByMinecraft);
        useJavaProvidedByMinecraftRow.setVisible(instance.javaVersion != null);

        addSection(GetText.tr("Advanced"));

        // Disable Legacy Launching (hidden for legacy fabric as it never uses legacy launching)
        if (instance.launcher.loaderVersion == null || !instance.launcher.loaderVersion.isLegacyFabric()) {
            disableLegacyLaunching = new MD3ComboBox<>();
            disableLegacyLaunching.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
            disableLegacyLaunching.addItem(new ComboItem<>(true, GetText.tr("Yes")));
            disableLegacyLaunching.addItem(new ComboItem<>(false, GetText.tr("No")));

            if (instance.launcher.disableLegacyLaunching == null) {
                disableLegacyLaunching.setSelectedIndex(0);
            } else if (instance.launcher.disableLegacyLaunching) {
                disableLegacyLaunching.setSelectedIndex(1);
            } else {
                disableLegacyLaunching.setSelectedIndex(2);
            }

            addRow(GetText.tr("Disable Legacy Launching"), GetText.tr(
                "This allows you to disable legacy launching for Minecraft < 1.6. It's highly recommended to not disable this, unless you're having issues launching older Minecraft versions."),
                disableLegacyLaunching);
        }

        // Use System GLFW
        useSystemGlfw = new MD3ComboBox<>();
        useSystemGlfw.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
        useSystemGlfw.addItem(new ComboItem<>(true, GetText.tr("Yes")));
        useSystemGlfw.addItem(new ComboItem<>(false, GetText.tr("No")));

        if (instance.launcher.useSystemGlfw == null) {
            useSystemGlfw.setSelectedIndex(0);
        } else if (instance.launcher.useSystemGlfw) {
            useSystemGlfw.setSelectedIndex(1);
        } else {
            useSystemGlfw.setSelectedIndex(2);
        }

        addRow(GetText.tr("Use System GLFW"), GetText.tr("Use the systems install for GLFW native library."),
            useSystemGlfw);

        // Use System OpenAL
        useSystemOpenAl = new MD3ComboBox<>();
        useSystemOpenAl.addItem(new ComboItem<>(null, GetText.tr("Use Launcher Default")));
        useSystemOpenAl.addItem(new ComboItem<>(true, GetText.tr("Yes")));
        useSystemOpenAl.addItem(new ComboItem<>(false, GetText.tr("No")));

        if (instance.launcher.useSystemOpenAl == null) {
            useSystemOpenAl.setSelectedIndex(0);
        } else if (instance.launcher.useSystemOpenAl) {
            useSystemOpenAl.setSelectedIndex(1);
        } else {
            useSystemOpenAl.setSelectedIndex(2);
        }

        addRow(GetText.tr("Use System OpenAL"), GetText.tr("Use the systems install for OpenAL native library."),
            useSystemOpenAl);

        // an instance on a version of Minecraft that brings no Java of its own has a path to set
        // whatever this is set to, so the swap only applies once there is a runtime to choose
        showJavaRows(instance.javaVersion != null && isUsingMinecraftProvidedJava);

        if (instance.javaVersion == null) {
            runtimeOverrideRow.setVisible(isUsingMinecraftProvidedJava);
        }
    }

    /**
     * @param provided whether Minecraft's own Java is being used, which decides whether the runtime
     *                 to use or the path to find it at is the thing to show
     */
    private void showJavaRows(boolean provided) {
        providedJavaRow.setVisible(provided);
        runtimeOverrideRow.setVisible(provided);

        javaPathRow.setVisible(!provided);

        if (detectedJavasRow != null) {
            detectedJavasRow.setVisible(!provided);
        }

        // a box layout lays out what it has rather than watching for a child becoming visible -
        // without this the list keeps the gap the hidden rows used to fill
        revalidate();
        repaint();
    }

    private Integer getIfNotNull(Integer value, Integer defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    private String getIfNotNull(String value, String defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    public boolean isValidJavaPath() {
        File jPath = new File(javaPath.getText(), "bin");
        if (!jPath.exists()) {
            DialogManager.okDialog().setTitle(GetText.tr("Help")).setContent(new HTMLBuilder().center().text(GetText.tr(
                    "The Java Path you set is incorrect.<br/><br/>Please verify it points to the folder where the bin folder is and try again."))
                .build()).setType(DialogManager.ERROR).show();
            return false;
        }
        return true;
    }

    public boolean isValidJavaParamaters() {
        if (javaParameters.getText().contains("-Xmx")
            || javaParameters.getText().contains("-XX:PermSize")
            || javaParameters.getText().contains("-XX:MetaspaceSize")) {
            DialogManager.okDialog().setTitle(GetText.tr("Help")).setContent(new HTMLBuilder().center().text(GetText.tr(
                    "The entered Java Parameters were incorrect.<br/><br/>Please remove any references to Xmx or XX:PermSize."))
                .build()).setType(DialogManager.ERROR).show();
            return false;
        }
        return true;
    }

    public void saveSettings() {
        Integer maximumMemory = (Integer) this.maximumMemory.getValue();
        Integer permGen = (Integer) this.permGen.getValue();
        String javaPath = this.javaPath.getText();
        String javaParameters = this.javaParameters.getText();
        String javaRuntimeOverrideVal = ((ComboItem<String>) javaRuntimeOverride.getSelectedItem())
            .getValue();
        Boolean useJavaProvidedByMinecraftVal = ((ComboItem<Boolean>) useJavaProvidedByMinecraft.getSelectedItem())
            .getValue();
        Boolean disableLegacyLaunchingVal = disableLegacyLaunching == null ? null
            : ((ComboItem<Boolean>) disableLegacyLaunching.getSelectedItem()).getValue();
        Boolean useSystemGlfwVal = ((ComboItem<Boolean>) useSystemGlfw.getSelectedItem()).getValue();
        Boolean useSystemOpenAlVal = ((ComboItem<Boolean>) useSystemOpenAl.getSelectedItem()).getValue();

        this.instance.launcher.maximumMemory = (maximumMemory == App.settings.maximumMemory ? null : maximumMemory);
        this.instance.launcher.permGen = (permGen == App.settings.metaspace ? null : permGen);

        boolean instanceWillUseMinecraftProvidedJava = Optional.ofNullable(useJavaProvidedByMinecraftVal)
            .orElse(App.settings.useJavaProvidedByMinecraft);

        if (!instanceWillUseMinecraftProvidedJava || instance.javaVersion == null) {
            this.instance.launcher.javaPath = (javaPath.equals(App.settings.javaPath) ? null : javaPath);
        }

        this.instance.launcher.javaArguments = (javaParameters.equals(App.settings.javaParameters) ? null
            : javaParameters);

        this.instance.launcher.useJavaProvidedByMinecraft = useJavaProvidedByMinecraftVal;
        this.instance.launcher.disableLegacyLaunching = disableLegacyLaunchingVal;
        this.instance.launcher.javaRuntimeOverride = javaRuntimeOverrideVal;
        this.instance.launcher.useSystemGlfw = useSystemGlfwVal;
        this.instance.launcher.useSystemOpenAl = useSystemOpenAlVal;
    }
}
