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

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Elevation;
import com.atlauncher.themes.md3.token.MD3Motion;
import com.atlauncher.themes.md3.token.MD3Shape;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 snackbar - a brief message at the bottom of the window, with at most one action.
 *
 * <p>
 * The replacement for {@code io.github.asyncronous.toast}, which popped a separate always-on-top
 * window per message. This lives in the frame's layered pane instead, so it cannot steal focus,
 * cannot outlive the window it belongs to, and cannot end up stranded on another monitor.
 *
 * <p>
 * Snackbars are for things the user does not have to act on. Anything that needs a decision, or
 * that they must not miss, is a dialog - a message that dismisses itself after four seconds is not
 * a way to report a failed install.
 *
 * <p>
 * Only one shows at a time; the rest queue.
 */
public final class MD3Snackbar {
    private static final int DEFAULT_DURATION = 4000;
    private static final int DURATION_WITH_ACTION = 6000;
    private static final int MAX_WIDTH = 560;
    private static final int MIN_HEIGHT = 48;

    private static final Deque<Pending> QUEUE = new ArrayDeque<>();
    private static SnackbarPanel visible;

    private MD3Snackbar() {
    }

    public static void show(Window owner, String message) {
        show(owner, message, null, null);
    }

    /**
     * @param actionLabel the single action offered, or null for a message with none. Never make it
     *                    "Dismiss" - the snackbar already dismisses itself.
     */
    public static void show(Window owner, String message, String actionLabel, ActionListener action) {
        JRootPane root = rootPaneOf(owner);

        if (root == null || message == null || message.isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            QUEUE.add(new Pending(root, message, actionLabel, action));
            pump();
        });
    }

    private static JRootPane rootPaneOf(Window owner) {
        if (owner instanceof RootPaneContainer) {
            return ((RootPaneContainer) owner).getRootPane();
        }

        return null;
    }

    private static void pump() {
        if (visible != null || QUEUE.isEmpty()) {
            return;
        }

        Pending next = QUEUE.poll();

        if (!next.root.isShowing()) {
            // the window went away while this was queued; drop it and carry on
            pump();

            return;
        }

        visible = new SnackbarPanel(next);
        visible.attach();
    }

    private static void finished() {
        visible = null;
        pump();
    }

    private static final class Pending {
        final JRootPane root;
        final String message;
        final String actionLabel;
        final ActionListener action;

        Pending(JRootPane root, String message, String actionLabel, ActionListener action) {
            this.root = root;
            this.message = message;
            this.actionLabel = actionLabel;
            this.action = action;
        }
    }

    /**
     * The visible bar. Positions itself against the bottom-left of its layered pane and slides in
     * from below.
     */
    private static final class SnackbarPanel extends JPanel {
        private final JLayeredPane layeredPane;
        private final ComponentListener resizeListener;
        private final Timer dismissTimer;

        /** 0 fully off-screen and transparent, 1 fully in place. */
        private float entry;
        private Animator animator;
        private boolean dismissing;

        SnackbarPanel(Pending pending) {
            super(new BorderLayout(UIScale.scale(MD3Spacing.S), 0));

            this.layeredPane = pending.root.getLayeredPane();

            setOpaque(false);
            setBorder(MD3Spacing.border(MD3Spacing.S, MD3Spacing.L));

            JLabel label = new JLabel(pending.message);
            label.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
            label.putClientProperty(MD3Type.TYPE_ROLE_KEY, MD3Type.BODY_MEDIUM);
            label.setForeground(MD3Color.inverseOnSurface());
            add(label, BorderLayout.CENTER);

            if (pending.actionLabel != null && !pending.actionLabel.isEmpty()) {
                MD3Button action = MD3Button.text(pending.actionLabel);
                action.setForeground(MD3Color.get(MD3Color.INVERSE_PRIMARY));
                action.addActionListener(e -> {
                    if (pending.action != null) {
                        pending.action.actionPerformed(e);
                    }

                    dismiss();
                });

                add(action, BorderLayout.EAST);
            }

            resizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    reposition();
                }
            };

            dismissTimer = new Timer(
                    pending.actionLabel != null ? DURATION_WITH_ACTION : DEFAULT_DURATION,
                    e -> dismiss());
            dismissTimer.setRepeats(false);
        }

        void attach() {
            layeredPane.add(this, JLayeredPane.POPUP_LAYER);
            layeredPane.addComponentListener(resizeListener);

            reposition();
            animateTo(1f, MD3Motion.CONTAINER_ENTER, MD3Motion.EMPHASIZED_DECELERATE, null);

            dismissTimer.start();
        }

        private void dismiss() {
            if (dismissing) {
                return;
            }

            dismissing = true;
            dismissTimer.stop();

            animateTo(0f, MD3Motion.CONTAINER_EXIT, MD3Motion.EMPHASIZED_ACCELERATE, this::detach);
        }

        private void detach() {
            layeredPane.removeComponentListener(resizeListener);
            layeredPane.remove(this);
            layeredPane.repaint();

            finished();
        }

        private void animateTo(float target, int duration, Animator.Interpolator easing, Runnable onEnd) {
            if (animator != null) {
                animator.stop();
            }

            if (!Animator.useAnimation() || MD3Motion.isReduced()) {
                entry = target;
                reposition();
                repaint();

                if (onEnd != null) {
                    onEnd.run();
                }

                return;
            }

            float start = entry;

            Animator.TimingTarget step = fraction -> {
                entry = start + (target - start) * fraction;
                reposition();
                repaint();
            };

            animator = onEnd != null ? new Animator(duration, step, onEnd) : new Animator(duration, step);
            animator.setInterpolator(easing);
            animator.start();
        }

        private void reposition() {
            Dimension size = getPreferredSize();
            int margin = UIScale.scale(MD3Spacing.L);
            int width = Math.min(size.width, Math.min(UIScale.scale(MAX_WIDTH), layeredPane.getWidth() - margin * 2));
            int height = Math.max(size.height, UIScale.scale(MIN_HEIGHT));

            // slides up into place, and drops back out the way it came
            int restingY = layeredPane.getHeight() - height - margin;
            int y = Math.round(restingY + height * (1f - entry));

            setBounds(margin, y, width, height);

            // a layered pane uses a null layout, so nothing lays this panel's own children out
            // unless it is asked to - without this the bar shows up as an empty slab
            validate();
        }

        /**
         * Fades the whole bar, contents included.
         *
         * <p>
         * The opacity used to be set in {@code paintComponent}, which only reaches what this panel
         * draws itself - so the container arrived over four hundred milliseconds while the message
         * and its action were fully opaque from the first frame, and the text appeared to float in
         * ahead of the thing it is written on. Applying it here puts the children under the same
         * composite.
         */
        @Override
        public void paint(Graphics g) {
            float alpha = Math.max(0f, Math.min(1f, entry));

            if (alpha >= 1f) {
                super.paint(g);

                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = MD3Paint.setup(g);

            try {
                MD3Paint.shadow(g2, MD3Paint.shapeOf(this, MD3Shape.SNACKBAR), MD3Elevation.LEVEL3);
                MD3Paint.fill(g2, MD3Paint.shapeOf(this, MD3Shape.SNACKBAR), MD3Color.inverseSurface());
            } finally {
                g2.dispose();
            }

            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.height = Math.max(size.height, UIScale.scale(MIN_HEIGHT));

            return size;
        }
    }
}
