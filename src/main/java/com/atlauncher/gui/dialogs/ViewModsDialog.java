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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.Pack;
import com.atlauncher.data.json.Mod;
import com.atlauncher.gui.card.ModCard;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.network.Analytics;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;

import com.formdev.flatlaf.util.UIScale;

public final class ViewModsDialog extends JDialog {
    private final JPanel contentPanel = new JPanel(new GridBagLayout());
    private final MD3TextField searchField;
    private final List<ModCard> cards = new ArrayList<>();

    public ViewModsDialog(Pack pack) {
        // #. {0} is the name of the pack
        super(App.launcher.getParent(), GetText.tr("Mods in {0}", pack.getName()), ModalityType.DOCUMENT_MODAL);

        Analytics.sendScreenView("View Mods Dialog");

        this.setPreferredSize(UIScale.scale(new Dimension(550, 450)));
        this.setMinimumSize(UIScale.scale(new Dimension(550, 450)));
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        // the label is the placeholder: it says what is being searched until typing replaces it
        searchField = MD3TextField.search(GetText.tr("Search"));
        searchField.setLeadingIcon(MD3Icons.SEARCH);
        searchField.setColumns(20);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));
        topPanel.add(this.searchField);

        contentPanel.setOpaque(false);

        JScrollPane scroller = new JScrollPane(this.contentPanel);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        this.add(topPanel, BorderLayout.NORTH);
        this.add(scroller, BorderLayout.CENTER);

        GridBagConstraints gbc = cardConstraints();

        this.searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                reload();
            }
        });

        List<Mod> mods = pack.getJsonVersion(pack.getLatestVersion().version).getMods();
        mods.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        for (Mod mod : mods) {
            ModCard card = new ModCard(mod);
            cards.add(card);
            contentPanel.add(card, gbc);
            gbc.gridy++;
        }

        this.pack();
        this.setLocationRelativeTo(App.launcher.getParent());
    }

    /**
     * One card per row, on the 4dp grid rather than the two unscaled pixels the cards used to be
     * packed with - which HiDPI displays never saw at all.
     */
    private static GridBagConstraints cardConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = MD3Spacing.insets(MD3Spacing.XS, MD3Spacing.S, 0, MD3Spacing.S);

        return gbc;
    }

    private void reload() {
        GridBagConstraints gbc = cardConstraints();

        this.contentPanel.removeAll();
        for (ModCard card : this.cards) {
            boolean show = true;

            if (!this.searchField.getText().isEmpty()) {
                if (!Pattern.compile(Pattern.quote(this.searchField.getText()), Pattern.CASE_INSENSITIVE)
                        .matcher(card.mod.getName()).find()) {

                    show = false;
                }
            }

            if (show) {
                this.contentPanel.add(card, gbc);
                gbc.gridy++;
            }
        }

        revalidate();
        repaint();
    }
}
