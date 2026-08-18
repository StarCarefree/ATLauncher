/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2026 ATLauncher
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
package com.atlauncher.gui.md3.feedback;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.UIScale;

/**
 * The chrome every one of the launcher's large dialogs was writing out for itself: a headline, a
 * body, and a row of actions over a rule.
 *
 * <p>
 * There is already {@link MD3Dialog} for a Material basic dialog - a question, built through a
 * builder and answered by an index. This is the other kind: a resizable window holding a form or a
 * list, which the caller assembles and drives itself. A dozen of them each repeated the same four
 * incantations, and drifted while doing it - two of them sized themselves in raw pixels, so at 200%
 * scaling they came up half the size they meant to; their headline padding was {@code L} in some and
 * {@code XL} in others; their action rows were {@code M} in some and {@code S} in others; and not one
 * of them closed on Escape.
 *
 * <p>
 * A subclass calls {@link #setDialogSize} first - it is what the headline's supporting text is
 * wrapped against - then any of {@link #setHeadline}, {@link #setBody} and {@link #setActions}.
 * {@link #setBody} takes the component unpadded, since a body is as likely to be a two-column
 * layout that pads its own columns as a single panel.
 */
public abstract class MD3WindowDialog extends JDialog {
    /** How many lines of supporting text a headline may carry before the rest is a tooltip. */
    private static final int SUPPORTING_LINES = 4;

    /** What supporting text is wrapped against before {@link #setDialogSize} has been called. */
    private int contentWidth = MD3Spacing.DIALOG_MAX_WIDTH;

    protected MD3WindowDialog(Window owner, String title, ModalityType modality) {
        super(owner, title, modality);

        setLayout(new BorderLayout());
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        getContentPane().setBackground(MD3Color.surface());

        installEscapeToClose();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    /**
     * Sizes the dialog and centres it on its owner.
     *
     * <p>
     * Both numbers are unscaled, like the spacing tokens. Passing real pixels here is the bug this
     * replaces: a dialog that asks for 960 gets 960 physical pixels, which on a 200% display is half
     * the window it was drawn for, with everything in it laid out at twice the density.
     *
     * @param width  unscaled; also what the headline's supporting text wraps against
     * @param height unscaled
     */
    protected void setDialogSize(int width, int height) {
        setDialogSize(width, height, width / 2, height / 2);
    }

    protected void setDialogSize(int width, int height, int minWidth, int minHeight) {
        contentWidth = width;

        setSize(UIScale.scale(new Dimension(width, height)));
        setMinimumSize(UIScale.scale(new Dimension(minWidth, minHeight)));
        setLocationRelativeTo(getOwner());
    }

    protected void setHeadline(String headline) {
        setHeadline(headline, null);
    }

    /**
     * The title over the body, and optionally a paragraph under it.
     *
     * <p>
     * The window title says the same thing, and on every platform the launcher runs on it says it in
     * the title bar's own type at the top of a frame the eye has already skipped. A dialog needs to
     * name itself inside itself.
     *
     * @param supporting plain text; wrapped here rather than handed to an HTML renderer, so it
     *                   breaks correctly in Chinese as well as in English
     */
    protected void setHeadline(String headline, String supporting) {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(MD3Spacing.border(MD3Spacing.L, MD3Spacing.L, MD3Spacing.S, MD3Spacing.L));

        JLabel title = new JLabel(headline);
        title.setFont(MD3Type.font(MD3Type.TITLE_LARGE, headline));
        title.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.TITLE_LARGE);
        title.setForeground(MD3Color.onSurface());
        title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title);

        if (supporting != null && !supporting.isEmpty()) {
            top.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.S)));
            top.add(supportingLabel(supporting, UIScale.scale(contentWidth - MD3Spacing.L * 2), SUPPORTING_LINES));
        }

        add(top, BorderLayout.NORTH);
    }

    /**
     * A block of muted body copy, wrapped to a width and truncated into its own tooltip past
     * {@code maxLines}.
     */
    public static JLabel supportingLabel(String text, int width, int maxLines) {
        JLabel supporting = new JLabel();
        supporting.setOpaque(false);
        supporting.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, text));
        supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
        supporting.setForeground(MD3Color.onSurfaceVariant());
        supporting.setAlignmentX(LEFT_ALIGNMENT);
        supporting.setToolTipText(text);

        FontMetrics metrics = supporting.getFontMetrics(supporting.getFont());
        supporting.setText(MD3Text.wrapToLines(metrics, text, width, maxLines));

        return supporting;
    }

    /** The dialog's content, added without padding of its own. */
    protected void setBody(JComponent body) {
        add(body, BorderLayout.CENTER);
    }

    /**
     * The actions, on the trailing edge over a rule.
     *
     * <p>
     * Order them so the one that proceeds is last, and label them with verbs. A user who reads only
     * the buttons should still know what is about to happen.
     */
    protected void setActions(JComponent... actions) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIScale.scale(MD3Spacing.S), 0));
        row.setOpaque(false);
        row.setBorder(MD3Spacing.border(MD3Spacing.M, MD3Spacing.L));

        for (JComponent action : actions) {
            row.add(action);
        }

        setActionBar(row);
    }

    /**
     * An action bar that is not a row of confirmations - a toolbar over a list, say, where every
     * change has already been made and there is nothing to confirm.
     *
     * @param content added below the rule, and expected to carry its own padding
     */
    protected void setActionBar(JComponent content) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(MD3Divider.inset(), BorderLayout.NORTH);
        bar.add(content, BorderLayout.CENTER);

        add(bar, BorderLayout.SOUTH);
    }

    /** Which action the enter key takes. */
    protected void setDefaultAction(JButton action) {
        getRootPane().setDefaultButton(action);
    }

    /**
     * Escape backs out, as it does in every other dialog on the desktop and in {@link MD3Dialog}.
     *
     * <p>
     * Routed through {@link #close()} so a subclass that has something to put down on the way out
     * gets to do it, however the dialog was dismissed.
     */
    private void installEscapeToClose() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "md3.close");
        getRootPane().getActionMap().put("md3.close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    /**
     * Dismisses the dialog. Override to refuse - a dialog mid-download - or to tidy up, and call
     * {@code super.close()} to let it go.
     */
    protected void close() {
        setVisible(false);
        dispose();
    }
}
