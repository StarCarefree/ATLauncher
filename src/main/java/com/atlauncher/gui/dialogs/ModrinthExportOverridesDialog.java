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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListContainer;
import com.atlauncher.gui.md3.input.MD3TextArea;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.atlauncher.utils.OS;
import com.formdev.flatlaf.util.UIScale;

/**
 * The list of files that went into overrides on a Modrinth export.
 *
 * <p>
 * Those files are not on Modrinth, and uploading the pack without permission to distribute them
 * is why Modrinth rejects packs. The dialog used to be a raw text area under a centred HTML
 * label; it is now the same surface and action bar as the rest of the exporter.
 */
public class ModrinthExportOverridesDialog extends JDialog {
    public ModrinthExportOverridesDialog(Dialog parent, String text) {
        super(parent, GetText.tr("Overrides Included"), true);

        setMinimumSize(UIScale.scale(new Dimension(560, 420)));
        setSize(UIScale.scale(new Dimension(640, 480)));
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                close();
            }
        });

        String supporting = GetText.tr(
                "Your exported instance contains mods not on Modrinth and were included in the overrides folder. If you're uploading this to Modrinth, you will need to make sure you have permissions to distribute the below mods, else your modpack will get denied on Modrinth.");

        JLabel headline = new JLabel(GetText.tr("Overrides included"));
        headline.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        headline.setForeground(MD3Color.onSurface());

        JLabel body = new JLabel("<html><body style='width:480px'>" + supporting + "</body></html>");
        body.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        body.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        body.setForeground(MD3Color.onSurfaceVariant());

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.M, MD3Spacing.L));
        headline.setAlignmentX(LEFT_ALIGNMENT);
        body.setAlignmentX(LEFT_ALIGNMENT);
        header.add(headline);
        header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
        header.add(body);

        MD3TextArea area = new MD3TextArea(12, 48);
        area.setText(text);
        area.setEditable(false);
        area.setCaretPosition(0);

        JPanel list = new JPanel(new BorderLayout());
        list.setOpaque(false);
        list.setBorder(MD3Spacing.border(0, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        list.add(MD3ListContainer.wrapping(area), BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        add(buildActionBar(text), BorderLayout.SOUTH);
    }

    private JPanel buildActionBar(String text) {
        MD3Button copyButton = MD3Button.outlined(GetText.tr("Copy"));
        copyButton.addActionListener(e -> OS.copyToClipboard(text));

        MD3Button closeButton = MD3Button.text(GetText.tr("Close"));
        closeButton.addActionListener(e -> close());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        actions.setOpaque(false);
        actions.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));
        actions.add(copyButton);
        actions.add(closeButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(MD3Divider.inset(), BorderLayout.NORTH);
        bar.add(actions, BorderLayout.CENTER);

        return bar;
    }

    private void close() {
        setVisible(false);
        dispose();
    }
}
