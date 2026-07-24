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
package com.atlauncher.gui.components;

import java.awt.FlowLayout;
import java.util.Date;

import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.OS;

/**
 * The launcher's outbound links, in one place.
 *
 * <p>
 * These used to live only on {@link BottomBar}, which meant the main window had to carry a bar
 * across its whole width to show six icons. Now the bar keeps its copy for the console window, and
 * the About tab - where someone actually goes looking for them - builds its own.
 */
public final class SocialLinks {
    /** Minutes after a crash during which the Discord button offers the modpack's own server. */
    private static final long CRASH_WINDOW_MILLIS = 300000;

    public enum Link {
        NODECRAFT("/assets/image/social/nodecraft.png",
                "Nodecraft - Setup a Minecraft server with an ATLauncher modpack in less than 60 seconds",
                "https://atl.pw/nodecraft-from-launcher"),
        DISCORD("/assets/image/social/discord.png", "Discord", "https://atl.pw/discord"),
        FACEBOOK("/assets/image/social/facebook.png", "Facebook", "https://atl.pw/facebook"),
        GITHUB("/assets/image/social/github.png", "GitHub", "https://atl.pw/github-launcher-3"),
        REDDIT("/assets/image/social/reddit.png", "Reddit", "https://atl.pw/reddit"),
        TWITTER("/assets/image/social/twitter.png", "Twitter", "https://atl.pw/twitter");

        private final String icon;
        private final String tooltip;
        private final String url;

        Link(String icon, String tooltip, String url) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.url = url;
        }
    }

    private SocialLinks() {
    }

    public static SMButton button(Link link) {
        SMButton button = new SMButton(link.icon, link.tooltip);

        button.addActionListener(e -> open(link));

        return button;
    }

    /**
     * A row of every link, for placing wherever there is room for it.
     */
    public static JPanel panel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setOpaque(false);

        for (Link link : Link.values()) {
            panel.add(button(link));
        }

        return panel;
    }

    private static void open(Link link) {
        if (link == Link.DISCORD && offerCrashedModpackDiscord()) {
            return;
        }

        LogManager.info("Opening Up ATLauncher " + link.tooltip);
        OS.openWebBrowser(link.url);
    }

    /**
     * Right after a crash, the modpack's own Discord is far more likely to be what the user wants
     * than ATLauncher's, so it is offered first.
     *
     * @return true if the modpack's server was opened and the general one should not be
     */
    private static boolean offerCrashedModpackDiscord() {
        if (App.launcher.lastInstanceCrashTime == null || App.launcher.lastInstanceCrash == null) {
            return false;
        }

        if (new Date().getTime() - App.launcher.lastInstanceCrashTime.getTime() >= CRASH_WINDOW_MILLIS) {
            return false;
        }

        if (App.launcher.lastInstanceCrash.getDiscordInviteUrl() == null) {
            return false;
        }

        int answer = DialogManager.yesNoDialog(false).setTitle(GetText.tr("Visit Modpack Discord?"))
                .setContent(new HTMLBuilder().center()
                        .text(GetText.tr(
                                "Would you like to open the Discord server for the last instance that crashed?"))
                        .build())
                .setType(DialogManager.QUESTION).show();

        if (answer != DialogManager.YES_OPTION) {
            return false;
        }

        Analytics.trackEvent(AnalyticsEvent.forInstanceEvent("instance_crashed_discord_button",
                App.launcher.lastInstanceCrash));
        LogManager.info("Opening Up Discord for Modpack");
        OS.openWebBrowser(App.launcher.lastInstanceCrash.getDiscordInviteUrl());

        return true;
    }
}
