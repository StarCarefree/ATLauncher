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
package com.atlauncher.gui.md3.button;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.paint.MD3Paint;
import com.formdev.flatlaf.util.UIScale;

/**
 * A Material 3 button that opens a menu.
 *
 * <p>
 * The replacement for {@link com.atlauncher.gui.components.DropDownButton}. That class painted a
 * chevron onto a FlatLaf button and hit-tested the last 24 pixels itself; this is an
 * {@link MD3Button} with a trailing chevron, so it picks up size, tone, state layers and the shape
 * morph for free.
 *
 * <p>
 * Two ways to use it. The default is a single target: clicking anywhere opens the menu. {@link
 * #setSplit(boolean) Split} divides it in two - the label fires the action listeners, the chevron
 * opens the menu - which is what Play-with-a-choice-of-offline used to be.
 *
 * <p>
 * A split button is two targets on one control, and where they are only means something to a
 * pointer. So the keyboard gets its own route to each: space and enter take the primary action, and
 * {@code Alt+Down} - what every platform's own split buttons use - opens the menu.
 *
 * <p>
 * Build the menu on each open when its contents can change. A supplier that is asked at show time
 * always reflects the current language and the current enabled state of whatever it points at.
 */
public class MD3MenuButton extends MD3Button {
    private static final String OPEN_MENU = "md3.openMenu";

    private Supplier<JPopupMenu> menu;
    private boolean split;

    /**
     * Set by the press that is about to become an action, and read once by it.
     *
     * <p>
     * One-shot rather than remembered: it can only ever be answered by a pointer, and a stale answer
     * left lying around is one the keyboard reads next. Holding it meant a split button whose chevron
     * had been clicked would open its menu again on the next space bar, and one that never had been
     * offered no way to open the menu at all.
     */
    private boolean pressInChevron;

    public MD3MenuButton(String text, Variant variant, JPopupMenu menu) {
        this(text, variant, () -> menu);
    }

    public MD3MenuButton(String text, Variant variant, Supplier<JPopupMenu> menu) {
        this(text, null, variant, menu);
    }

    public MD3MenuButton(String text, Icon icon, Variant variant, Supplier<JPopupMenu> menu) {
        super(text, icon, variant);

        this.menu = menu;

        setTrailingIcon(MD3Icon.of(MD3Icons.CHEVRON_DOWN));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressInChevron = isSplit() && isInChevron(e.getX());
            }
        });

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), OPEN_MENU);
        getActionMap().put(OPEN_MENU, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMenu();
            }
        });
    }

    public static MD3MenuButton filled(String text, JPopupMenu menu) {
        return new MD3MenuButton(text, Variant.FILLED, menu);
    }

    public static MD3MenuButton tonal(String text, JPopupMenu menu) {
        return new MD3MenuButton(text, Variant.TONAL, menu);
    }

    public static MD3MenuButton outlined(String text, JPopupMenu menu) {
        return new MD3MenuButton(text, Variant.OUTLINED, menu);
    }

    public static MD3MenuButton text(String text, JPopupMenu menu) {
        return new MD3MenuButton(text, Variant.TEXT, menu);
    }

    public boolean isSplit() {
        return split;
    }

    /**
     * Splits the button into a primary action and a menu, or joins them back together.
     *
     * <p>
     * A split button without an action listener is a button whose label does nothing, which is
     * worse than not splitting it.
     */
    public void setSplit(boolean split) {
        this.split = split;
        this.pressInChevron = false;

        repaint();
    }

    public MD3MenuButton withSplit(boolean split) {
        setSplit(split);

        return this;
    }

    public void setMenu(Supplier<JPopupMenu> menu) {
        this.menu = menu;
    }

    public void setMenu(JPopupMenu menu) {
        this.menu = () -> menu;
    }

    int chevronStart() {
        return getWidth() - UIScale.scale(MD3ButtonUI.trailingZone(this));
    }

    /**
     * Whether an x position is in the half of a split button that opens the menu.
     *
     * <p>
     * The chevron sits on the trailing edge, which is the left one where the interface reads right to
     * left - so the test is which side of the divider the press landed on, not whether it was far
     * enough right.
     */
    boolean isInChevron(int x) {
        return MD3Paint.isLeftToRight(this) ? x >= chevronStart()
                : x <= UIScale.scale(MD3ButtonUI.trailingZone(this));
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        boolean chevron = pressInChevron;
        pressInChevron = false;

        if (isSplit() && !chevron) {
            super.fireActionPerformed(event);

            return;
        }

        showMenu();
    }

    public void showMenu() {
        if (menu == null) {
            return;
        }

        JPopupMenu popup = menu.get();

        if (popup == null || popup.getComponentCount() == 0) {
            return;
        }

        popup.setPreferredSize(new Dimension(Math.max(getWidth(), popup.getPreferredSize().width),
                popup.getPreferredSize().height));
        popup.show(this, 0, getHeight());
    }
}
