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
package com.atlauncher.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.atlauncher.evnt.listener.ThemeListener;
import com.atlauncher.evnt.manager.ThemeManager;

public abstract class BottomBar extends JPanel implements ThemeListener {
    private static final long serialVersionUID = -7488195680365431776L;

    // still fields rather than a SocialLinks.panel, because ConsoleBottomBar hides them one at a
    // time as its window narrows
    public final JButton nodeCraftIcon = SocialLinks.button(SocialLinks.Link.NODECRAFT);
    public final JButton discordIcon = SocialLinks.button(SocialLinks.Link.DISCORD);
    public final JButton facebookIcon = SocialLinks.button(SocialLinks.Link.FACEBOOK);
    public final JButton githubIcon = SocialLinks.button(SocialLinks.Link.GITHUB);
    public final JButton twitterIcon = SocialLinks.button(SocialLinks.Link.TWITTER);
    public final JButton redditIcon = SocialLinks.button(SocialLinks.Link.REDDIT);

    protected final JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 8));

    public BottomBar() {
        super(new BorderLayout());
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("BottomBar.dividerColor")));
        this.setPreferredSize(new Dimension(0, 50));

        this.add(this.rightSide, BorderLayout.EAST);
        this.rightSide.add(this.nodeCraftIcon);
        this.rightSide.add(this.discordIcon);
        this.rightSide.add(this.facebookIcon);
        this.rightSide.add(this.githubIcon);
        this.rightSide.add(this.redditIcon);
        this.rightSide.add(this.twitterIcon);

        ThemeManager.addListener(this);
    }


    @Override
    public void onThemeChange() {
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("BottomBar.dividerColor")));
    }
}
