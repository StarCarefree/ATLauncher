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
package com.atlauncher.gui.handlers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Type;
import com.atlauncher.data.minecraft.FabricMod;
import com.atlauncher.data.minecraft.MCMod;
import com.atlauncher.gui.dialogs.EditModsDialog;
import com.atlauncher.gui.dialogs.FileTypeDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ModFingerprinter;
import com.atlauncher.utils.Utils;

public class ModsJCheckBoxTransferHandler extends TransferHandler {
    private final EditModsDialog dialog;
    private final boolean disabled;

    public ModsJCheckBoxTransferHandler(EditModsDialog dialog, boolean disabled) {
        this.dialog = dialog;
        this.disabled = disabled;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferSupport ts) {
        return ts.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport ts) {
        try {
            @SuppressWarnings("unchecked")
            final List<File> data = (List<File>) ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (data.isEmpty()) {
                return false;
            }

            Type type;
            File instanceFile;

            String[] modTypes;

            if (dialog.instanceOrServer instanceof Instance) {
                modTypes = new String[] { "Mods Folder", "Data Pack", "Resource Pack", "Shader Pack",
                        "Inside Minecraft.jar" };
            } else if (dialog.instanceOrServer.getLoaderVersion() != null
                    && (dialog.instanceOrServer.getLoaderVersion().isPaper()
                            || dialog.instanceOrServer.getLoaderVersion().isPurpur())) {
                modTypes = new String[] { "Plugins Folder" };
            } else {
                modTypes = new String[] { "Mods Folder" };
            }

            FileTypeDialog ftd = new FileTypeDialog(GetText.tr("Add Mod"), GetText.tr("Adding Mods"), GetText.tr("Add"),
                    GetText.tr("Type"), modTypes);
            ftd.setVisible(true);

            if (ftd.wasClosed()) {
                return false;
            }

            String typeTemp = ftd.getSelectorValue();

            if (typeTemp.equalsIgnoreCase("Inside Minecraft.jar")) {
                int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Add As Mod?"))
                        .setContent(new HTMLBuilder().text(GetText.tr(
                                "Adding as Inside Minecraft.jar is usually not what you want and will likely cause issues.<br/><br/>If you're adding mods this is usually not correct. Do you want to add this as a mod instead?"))
                                .build())
                        .setType(DialogManager.WARNING).show();

                if (ret != 0) {
                    type = Type.jar;
                    instanceFile = dialog.instanceOrServer.getRoot().resolve("jarmods").toFile();
                } else {
                    type = Type.mods;
                    instanceFile = dialog.instanceOrServer.getRoot().resolve("mods").toFile();
                }
            } else if (typeTemp.equalsIgnoreCase("CoreMods Mod")) {
                type = Type.coremods;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("coremods").toFile();
            } else if (typeTemp.equalsIgnoreCase("Texture Pack")) {
                type = Type.texturepack;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("texturepacks").toFile();
            } else if (typeTemp.equalsIgnoreCase("Data Pack")) {
                type = Type.datapack;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("datapacks").toFile();
            } else if (typeTemp.equalsIgnoreCase("Resource Pack")) {
                type = Type.resourcepack;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("resourcepacks").toFile();
            } else if (typeTemp.equalsIgnoreCase("Shader Pack")) {
                type = Type.shaderpack;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("shaderpacks").toFile();
            } else if (typeTemp.equalsIgnoreCase("Plugins Folder")) {
                type = Type.plugins;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("plugins").toFile();
            } else {
                type = Type.mods;
                instanceFile = dialog.instanceOrServer.getRoot().resolve("mods").toFile();
            }

            final ProgressDialog<Object> progressDialog = new ProgressDialog<>(GetText.tr("Copying Mods"), 0,
                    GetText.tr("Copying Mods"), dialog);

            progressDialog.addThread(new Thread(() -> {
                List<DisableableMod> modsAdded = new ArrayList<>();

                for (File item : data) {
                    File copyTo = instanceFile;

                    if (!Utils.isAcceptedModFile(item)) {
                        DialogManager.okDialog().setTitle(GetText.tr("Invalid File")).setContent(GetText
                                .tr("Skipping file {0}. Only zip, jar and litemod files can be added.", item.getName()))
                                .setType(DialogManager.ERROR).show();
                        continue;
                    }

                    if (this.disabled) {
                        copyTo = dialog.instanceOrServer.getRoot().resolve("disabledmods").toFile();
                    }

                    DisableableMod mod = new DisableableMod();
                    mod.disabled = this.disabled;
                    mod.userAdded = true;
                    mod.wasSelected = true;
                    mod.file = item.getName();
                    mod.type = type;
                    mod.optional = true;
                    mod.name = item.getName();
                    mod.version = "Unknown";
                    mod.description = null;

                    MCMod mcMod = Utils.getMCModForFile(item);
                    if (mcMod != null) {
                        mod.name = Optional.ofNullable(mcMod.name).orElse(item.getName());
                        mod.version = Optional.ofNullable(mcMod.version).orElse("Unknown");
                        mod.description = mcMod.description;
                    } else {
                        FabricMod fabricMod = Utils.getFabricModForFile(item);
                        if (fabricMod != null) {
                            mod.name = Optional.ofNullable(fabricMod.name).orElse(item.getName());
                            mod.version = Optional.ofNullable(fabricMod.version).orElse("Unknown");
                            mod.description = fabricMod.description;
                        }
                    }

                    if (!copyTo.exists()) {
                        copyTo.mkdirs();
                    }

                    if (Utils.copyFile(item, copyTo)) {
                        modsAdded.add(mod);
                    }
                }

                // was two 70 line blocks of hashing and lookups copy pasted from the instance
                // scan, one of which hashed the enabled path even for a mod dropped onto the
                // disabled column - so those were found on Modrinth and never on CurseForge
                ModFingerprinter.identify(modsAdded, dialog.instanceOrServer, true);

                dialog.instanceOrServer.addMods(modsAdded);

                progressDialog.close();
            }));
            progressDialog.start();

            dialog.reloadPanels();
            return true;

        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }
}
