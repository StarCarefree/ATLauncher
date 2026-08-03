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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.atlauncher.App;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

import com.formdev.flatlaf.util.UIScale;

public class FileTypeDialog extends JDialog {

    private final MD3ComboBox<String> selector;

    private boolean closed = false;

    public FileTypeDialog(String title, String labelName, String bottomText, String selectorText, String[] subOptions) {
        super(App.launcher.getParent(), title, ModalityType.DOCUMENT_MODAL);
        setSize(UIScale.scale(400), UIScale.scale(175));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        getContentPane().setBackground(MD3Color.surface());

        // Top Panel Stuff
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.XL, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        JLabel prompt = new JLabel(labelName);
        prompt.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM, labelName));
        prompt.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_MEDIUM);
        prompt.setForeground(MD3Color.onSurface());
        top.add(prompt, BorderLayout.CENTER);

        // Middle Panel Stuff
        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setBorder(MD3Spacing.border(MD3Spacing.L));
        middle.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        JLabel selectorLabel = new JLabel(selectorText + ": ");
        selectorLabel.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, selectorText));
        selectorLabel.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        selectorLabel.setForeground(MD3Color.onSurface());
        middle.add(selectorLabel, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        selector = new MD3ComboBox<>();
        for (String item : subOptions) {
            selector.addItem(item);
        }
        middle.add(selector, gbc);

        // Bottom Panel Stuff
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, MD3Spacing.scale(MD3Spacing.S), 0));
        buttons.setOpaque(false);
        buttons.setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

        MD3Button bottomButton = MD3Button.filled(bottomText);
        bottomButton.addActionListener(e -> close());
        buttons.add(bottomButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(MD3Divider.inset(), BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                closed = true;
                close();
            }
        });
    }

    private void close() {
        setVisible(false);
        dispose();
    }

    public boolean wasClosed() {
        return this.closed;
    }

    public String getSelectorValue() {
        return (String) this.selector.getSelectedItem();
    }

}
