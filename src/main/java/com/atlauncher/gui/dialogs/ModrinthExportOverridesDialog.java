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
import java.awt.Dialog;

import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.feedback.MD3WindowDialog;
import com.atlauncher.gui.md3.input.MD3TextArea;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.utils.OS;

/**
 * The list of files that went into overrides on a Modrinth export.
 *
 * <p>
 * Those files are not on Modrinth, and uploading the pack without permission to distribute them
 * is why Modrinth rejects packs. The dialog used to be a raw text area under a centred HTML
 * label; it is now the same surface and action bar as the rest of the exporter.
 */
public class ModrinthExportOverridesDialog extends MD3WindowDialog {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public ModrinthExportOverridesDialog(Dialog parent, String text) {
        super(parent, GetText.tr("Overrides Included"), ModalityType.APPLICATION_MODAL);

        setDialogSize(WIDTH, HEIGHT, 560, 420);

        setHeadline(GetText.tr("Overrides Included"), GetText.tr(
                "Your exported instance contains mods not on Modrinth and were included in the overrides folder. If you're uploading this to Modrinth, you will need to make sure you have permissions to distribute the below mods, else your modpack will get denied on Modrinth."));
        setBody(buildBody(text));
        buildActions(text);
    }

    private JPanel buildBody(String text) {
        MD3TextArea area = new MD3TextArea(12, 48);
        area.setText(text);
        area.setEditable(false);
        area.setCaretPosition(0);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(MD3Spacing.border(0, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        body.add(MD3ListContainer.wrapping(area), BorderLayout.CENTER);

        return body;
    }

    private void buildActions(String text) {
        MD3Button copyButton = MD3Button.outlined(GetText.tr("Copy"));
        copyButton.addActionListener(e -> OS.copyToClipboard(text));

        MD3Button closeButton = MD3Button.text(GetText.tr("Close"));
        closeButton.addActionListener(e -> close());

        // the dismiss goes last, because there is nothing here to confirm - the export has already
        // happened, and Copy is the only thing this dialog can do for you
        setActions(copyButton, closeButton);
        setDefaultAction(closeButton);
    }
}
