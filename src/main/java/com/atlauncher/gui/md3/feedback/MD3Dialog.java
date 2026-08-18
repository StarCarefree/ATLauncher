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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;

import com.atlauncher.gui.md3.MD3MixedText;
import com.atlauncher.gui.md3.MD3Text;
import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.paint.MD3Animated;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3State;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 basic dialog.
 *
 * <p>
 * Built through {@link #builder(Window)}; {@link Builder#show()} blocks and returns the index of
 * the action the user chose, or {@link #DISMISSED} if they closed it without choosing.
 *
 * <p>
 * Order the actions so the confirming one is last and rightmost, and label them with verbs - "Delete
 * instance", not "OK". A user who reads only the buttons should still know what is about to happen.
 *
 * <p>
 * The owner window is dimmed behind the dialog. Rounded corners need per-pixel window translucency;
 * where the platform cannot do it, the dialog falls back to square corners rather than to a black
 * box around itself.
 */
public class MD3Dialog extends JDialog {
    /** Returned by {@link Builder#show()} when the dialog was closed without choosing an action. */
    public static final int DISMISSED = -1;

    private static final int MIN_WIDTH = MD3Spacing.DIALOG_MIN_WIDTH;
    private static final int MAX_WIDTH = MD3Spacing.DIALOG_MAX_WIDTH;

    /**
     * Wraps supporting text to a pixel width, but only once it actually needs wrapping.
     *
     * <p>
     * Swing honours a {@code width} on an HTML block and breaks the text itself, which is the only
     * way to wrap to a real width - a character count cannot know how wide a character is, and gets
     * it badly wrong for CJK, where every glyph is twice the width the count assumes.
     *
     * <p>
     * Short text is left unwrapped so a two-word dialog does not stretch to the full 560dp.
     */
    private static String wrap(String text, int maxWidth, java.awt.Font font) {
        if (MD3MixedText.width(font, text) <= maxWidth) {
            return text;
        }

        return MD3Text.HTML_OPEN + "<div style='width:" + maxWidth + "px'>" + MD3MixedText.toHtml(font, text)
                + "</div>" + MD3Text.HTML_CLOSE;
    }

    private final DialogPanel panel;
    private JPanel scrim;

    private int result = DISMISSED;

    private MD3Dialog(Window owner, Builder builder) {
        super(owner, builder.title, ModalityType.APPLICATION_MODAL);

        panel = new DialogPanel(builder, this);

        boolean rounded = supportsRoundedCorners();

        setUndecorated(rounded);

        if (rounded) {
            setBackground(new Color(0, 0, 0, 0));
            getRootPane().setOpaque(false);
            ((JComponent) getContentPane()).setOpaque(false);
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        // an undecorated window has no title bar to read the title off, so the headline has to be
        // what names the dialog, and the supporting text what explains it
        if (builder.headline != null && !builder.headline.isEmpty()) {
            getAccessibleContext().setAccessibleName(builder.headline);
        }

        if (builder.supportingText != null && !builder.supportingText.isEmpty()) {
            getAccessibleContext().setAccessibleDescription(builder.supportingText);
        }

        installEscapeToDismiss();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                removeScrim();
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    public static Builder builder(Window owner) {
        return new Builder(owner);
    }

    /**
     * Shows the dialog and blocks until it is answered.
     *
     * @return the index of the action chosen, or {@link #DISMISSED}
     */
    public int showAndWait() {
        showScrim();
        fadeIn();
        setVisible(true);

        return result;
    }

    /**
     * Brings the window up from nothing rather than having it be there.
     *
     * <p>
     * Started before {@link #setVisible(boolean)}, which blocks - a modal dialog runs its own event
     * pump, and the timer driving this is dispatched through it like any other event, so the frames
     * arrive while the call above is still waiting for an answer.
     *
     * <p>
     * Every step is guarded. Window opacity is the one piece of Material motion here that the
     * platform can refuse outright, and a dialog that will not fade has to still be a dialog.
     */
    private void fadeIn() {
        if (!Animator.useAnimation() || MD3Motion.isReduced() || !supportsWindowFade()) {
            return;
        }

        try {
            setOpacity(0f);
        } catch (Exception e) {
            return;
        }

        MD3Motion.animator(MD3Motion.CONTAINER_ENTER, MD3Motion.EMPHASIZED_DECELERATE,
                new Animator.TimingTarget() {
                    @Override
                    public void timingEvent(float fraction) {
                        setWindowOpacity(fraction);
                    }

                    @Override
                    public void end() {
                        setWindowOpacity(1f);
                    }
                }).start();
    }

    private void setWindowOpacity(float opacity) {
        try {
            if (isDisplayable()) {
                setOpacity(Math.max(0f, Math.min(1f, opacity)));
            }
        } catch (Exception ignored) {
            // the platform changed its mind about translucency mid-fade; nothing to do but leave the
            // dialog at whatever opacity it last accepted, which is a visible dialog either way
        }
    }

    private static boolean supportsWindowFade() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Answers the dialog from outside it, for one that is waiting on something other than the user -
     * a file appearing on disk, say. Does nothing once the dialog has already been answered.
     */
    public void choose(int index) {
        finish(index);
    }

    /**
     * Whether the platform can draw a window with transparent corners. Linux without a compositor
     * cannot, and forcing it there produces a black rectangle behind every rounded edge.
     */
    private static boolean supportsRoundedCorners() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        } catch (Throwable t) {
            return false;
        }
    }

    private void installEscapeToDismiss() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "md3.dismiss");
        getRootPane().getActionMap().put("md3.dismiss", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                finish(DISMISSED);
            }
        });
    }

    void finish(int index) {
        result = index;

        dispose();
    }

    private void showScrim() {
        Window owner = getOwner();

        if (!(owner instanceof RootPaneContainer)) {
            return;
        }

        JRootPane root = ((RootPaneContainer) owner).getRootPane();
        JLayeredPane layered = root.getLayeredPane();

        scrim = new Scrim();
        scrim.setBounds(0, 0, layered.getWidth(), layered.getHeight());

        layered.add(scrim, JLayeredPane.MODAL_LAYER);
        layered.repaint();

        // added first, so it is on screen and its animation has somewhere to paint
        ((Scrim) scrim).dimIn();
    }

    /**
     * The dim over the window behind the dialog. It arrives rather than switching on, so the window
     * looks like it went behind something instead of having been repainted darker.
     */
    private static final class Scrim extends JPanel {
        private final MD3Animated dim = new MD3Animated(this, 0f, MD3Motion.CONTAINER_ENTER,
                MD3Motion.EMPHASIZED_DECELERATE);

        Scrim() {
            setOpaque(false);
        }

        void dimIn() {
            dim.setTarget(1f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setColor(MD3Color.get(MD3Color.SCRIM, MD3State.SCRIM * dim.value()));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    private void removeScrim() {
        if (scrim == null) {
            return;
        }

        Window owner = getOwner();

        if (owner instanceof RootPaneContainer) {
            JLayeredPane layered = ((RootPaneContainer) owner).getRootPane().getLayeredPane();
            layered.remove(scrim);
            layered.repaint();
        }

        scrim = null;
    }

    /**
     * Assembles and shows a dialog.
     */
    public static final class Builder {
        private final Window owner;
        private final List<ActionSpec> actions = new ArrayList<>();

        private String title = "";
        private MD3Icon.Painter icon;
        private String headline;
        private String supportingText;
        private JComponent content;
        private int maxWidth = MAX_WIDTH;

        private Builder(Window owner) {
            this.owner = owner;
        }

        /** The window title, used by the taskbar and by screen readers. */
        public Builder title(String title) {
            this.title = title;

            return this;
        }

        /**
         * A hero icon above the headline. Its presence centres the header, so use it for dialogs
         * that need weight - a destructive confirmation - and leave it off for routine ones.
         */
        public Builder icon(MD3Icon.Painter icon) {
            this.icon = icon;

            return this;
        }

        public Builder headline(String headline) {
            this.headline = headline;

            return this;
        }

        public Builder supportingText(String supportingText) {
            this.supportingText = supportingText;

            return this;
        }

        /** Arbitrary content between the supporting text and the actions - a form, a list. */
        public Builder content(JComponent content) {
            this.content = content;

            return this;
        }

        /**
         * Widens the dialog past the {@value #MAX_WIDTH}dp a basic dialog is held to.
         *
         * <p>
         * That limit is the right one for a question - a line of text the eye can take in without
         * tracking back - and the wrong one for a document. A dialog showing something to read
         * rather than something to answer says so with this.
         *
         * @param maxWidth unscaled, as the tokens are
         */
        public Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;

            return this;
        }

        public Builder action(String label, MD3Button.Variant variant) {
            return action(label, variant, MD3Button.Tone.DEFAULT);
        }

        public Builder action(String label, MD3Button.Variant variant, MD3Button.Tone tone) {
            actions.add(new ActionSpec(label, variant, tone));

            return this;
        }

        /** The action that proceeds. Added last so it lands rightmost. */
        public Builder confirm(String label) {
            return action(label, MD3Button.Variant.FILLED);
        }

        /**
         * A confirming action that destroys something. Filled, in the error role, so it cannot be
         * mistaken for the routine confirm next to it.
         */
        public Builder destructive(String label) {
            return action(label, MD3Button.Variant.FILLED, MD3Button.Tone.ERROR);
        }

        /** The action that backs out. */
        public Builder dismiss(String label) {
            return action(label, MD3Button.Variant.TEXT);
        }

        /**
         * Which action the enter key takes.
         *
         * <p>
         * The rightmost one, which is the one that proceeds - except where any of them is
         * {@link #destructive(String) destructive}, and a destructive action is added last like every
         * other confirming one. A dialog where enter deletes the instance is a dialog that deletes
         * instances by accident, so that one is left with no default at all rather than having it
         * handed to whatever is beside it, which would make enter mean "cancel" here and "proceed"
         * everywhere else.
         *
         * @return the index to make default, or -1 for none
         */
        int defaultActionIndex() {
            for (ActionSpec action : actions) {
                if (action.tone == MD3Button.Tone.ERROR) {
                    return -1;
                }
            }

            return actions.size() - 1;
        }

        public MD3Dialog build() {
            return new MD3Dialog(owner, this);
        }

        /**
         * @return the index of the action chosen, in the order they were added, or
         *         {@link MD3Dialog#DISMISSED}
         */
        public int show() {
            return build().showAndWait();
        }
    }

    private static final class ActionSpec {
        final String label;
        final MD3Button.Variant variant;
        final MD3Button.Tone tone;

        ActionSpec(String label, MD3Button.Variant variant, MD3Button.Tone tone) {
            this.label = label;
            this.variant = variant;
            this.tone = tone;
        }
    }

    /**
     * The dialog's surface. Paints the container itself so the corners can be rounded past what a
     * border would allow.
     */
    private static final class DialogPanel extends JPanel {
        private final boolean rounded;
        private final int maxWidth;

        DialogPanel(Builder builder, MD3Dialog dialog) {
            super(new BorderLayout());

            this.rounded = supportsRoundedCorners();
            this.maxWidth = builder.maxWidth;

            setOpaque(false);

            int room = rounded ? MD3Paint.shadowRoom(MD3Elevation.LEVEL3) : 0;
            int roomBelow = rounded ? MD3Paint.shadowRoomBelow(MD3Elevation.LEVEL3) : 0;
            setBorder(MD3Spacing.border(MD3Spacing.XL + room, MD3Spacing.XL + room,
                    MD3Spacing.XL + roomBelow, MD3Spacing.XL + room));

            boolean centred = builder.icon != null;

            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setOpaque(false);

            if (builder.icon != null) {
                JLabel iconLabel = new JLabel(
                        MD3Icon.of(builder.icon, MD3Spacing.ICON_SIZE_LARGE).withRole(MD3Color.SECONDARY));
                iconLabel.setAlignmentX(CENTER_ALIGNMENT);
                header.add(iconLabel);
                header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)));
            }

            if (builder.headline != null && !builder.headline.isEmpty()) {
                JLabel headline = new JLabel(builder.headline);
                headline.setFont(MD3Type.font(MD3Type.HEADLINE_SMALL));
                headline.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.HEADLINE_SMALL);
                headline.setForeground(MD3Color.onSurface());
                headline.setAlignmentX(centred ? CENTER_ALIGNMENT : LEFT_ALIGNMENT);
                headline.setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEADING);
                header.add(headline);
            }

            if (builder.supportingText != null && !builder.supportingText.isEmpty()) {
                header.add(Box.createVerticalStrut(UIScale.scale(MD3Spacing.L)));

                JLabel supporting = new JLabel(wrap(builder.supportingText,
                        UIScale.scale(MAX_WIDTH - MD3Spacing.XL * 2), MD3Type.font(MD3Type.BODY_MEDIUM)));
                supporting.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
                supporting.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
                supporting.setForeground(MD3Color.onSurfaceVariant());
                supporting.setAlignmentX(centred ? CENTER_ALIGNMENT : LEFT_ALIGNMENT);
                header.add(supporting);
            }

            add(header, BorderLayout.NORTH);

            if (builder.content != null) {
                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setOpaque(false);
                wrapper.setBorder(MD3Spacing.border(MD3Spacing.L, 0, 0, 0));
                wrapper.add(builder.content, BorderLayout.CENTER);

                add(wrapper, BorderLayout.CENTER);
            }

            if (!builder.actions.isEmpty()) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIScale.scale(MD3Spacing.S), 0));
                row.setOpaque(false);
                row.setBorder(MD3Spacing.border(MD3Spacing.XL, 0, 0, 0));

                int defaultIndex = builder.defaultActionIndex();

                for (int i = 0; i < builder.actions.size(); i++) {
                    ActionSpec spec = builder.actions.get(i);
                    int index = i;

                    MD3Button button = new MD3Button(spec.label, spec.variant);
                    button.setTone(spec.tone);
                    button.addActionListener(e -> dialog.finish(index));

                    row.add(button);

                    if (i == defaultIndex) {
                        dialog.getRootPane().setDefaultButton(button);
                    }
                }

                add(row, BorderLayout.SOUTH);
            }
        }


        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();

            size.width = Math.max(UIScale.scale(MIN_WIDTH), Math.min(size.width, UIScale.scale(maxWidth)));

            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                java.awt.Shape shape;

                if (rounded) {
                    float room = UIScale.scale((float) MD3Paint.shadowRoom(MD3Elevation.LEVEL3));
                    float below = UIScale.scale((float) MD3Paint.shadowRoomBelow(MD3Elevation.LEVEL3));
                    shape = MD3Shape.rounded(room, room, getWidth() - room * 2f, getHeight() - room - below,
                            MD3Shape.DIALOG);
                    MD3Paint.shadow(g2, shape, MD3Elevation.LEVEL3);
                } else {
                    shape = MD3Paint.shapeOf(this, MD3Shape.NONE);
                }

                MD3Paint.fill(g2, shape, MD3Elevation.surface(MD3Elevation.LEVEL3));
            } finally {
                g2.dispose();
            }

            super.paintComponent(g);
        }

        @Override
        public Component add(Component comp) {
            if (comp instanceof JComponent) {
                ((JComponent) comp).setOpaque(false);
            }

            return super.add(comp);
        }
    }
}
