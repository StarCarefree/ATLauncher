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
package com.atlauncher.gui.md3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.atlauncher.gui.md3.button.MD3Button;
import com.atlauncher.gui.md3.button.MD3ButtonBar;
import com.atlauncher.gui.md3.button.MD3ButtonGroup;
import com.atlauncher.gui.md3.button.MD3Fab;
import com.atlauncher.gui.md3.button.MD3IconButton;
import com.atlauncher.gui.md3.button.MD3MenuButton;
import com.atlauncher.gui.md3.container.MD3Card;
import com.atlauncher.gui.md3.container.MD3Divider;
import com.atlauncher.gui.md3.container.MD3ListItem;
import com.atlauncher.gui.md3.feedback.MD3CircularProgress;
import com.atlauncher.gui.md3.feedback.MD3LinearProgress;
import com.atlauncher.gui.md3.icon.MD3Icon;
import com.atlauncher.gui.md3.icon.MD3Icons;
import com.atlauncher.gui.md3.input.MD3Chip;
import com.atlauncher.gui.md3.input.MD3Radio;
import com.atlauncher.gui.md3.input.MD3Slider;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.gui.md3.input.MD3TextArea;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.gui.md3.nav.MD3NavigationRail;
import com.atlauncher.gui.md3.nav.MD3TopAppBar;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Spacing;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * Every Material component in every state, on one page.
 *
 * <p>
 * The visual baseline for the migration. Swing components are painted, not declared, so the only
 * way to know a state layer or a disabled colour is right is to look at it - and the only way to
 * know a theme change did not break one of the eighteen themes is to look at all of them. Rendering
 * this offscreen makes that a diff rather than a memory.
 *
 * <p>
 * Run it directly to browse interactively, or call {@link #renderTo} from a test to capture a
 * sheet.
 */
public final class MD3Gallery {
    private MD3Gallery() {
    }

    public static void main(String[] args) throws Exception {
        String theme = args.length > 0 ? args[0] : "com.atlauncher.themes.MaterialDark";
        installTheme(theme);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Material 3 gallery - " + theme);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JScrollPane scroller = new JScrollPane(build());
            scroller.getVerticalScrollBar().setUnitIncrement(16);
            scroller.setBorder(null);

            frame.add(scroller, BorderLayout.CENTER);
            frame.setSize(1100, 900);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void installTheme(String themeClass) throws Exception {
        Class.forName(themeClass).getMethod("install").invoke(null);
    }

    /**
     * Paints the gallery to a PNG without needing a display, so a build can keep a rendered record
     * of every theme.
     */
    public static void renderTo(File file, String themeClass) throws Exception {
        installTheme(themeClass);

        JPanel root = build();
        Dimension size = root.getPreferredSize();
        root.setSize(size);
        layoutTree(root);

        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        applyDesktopFontHints(g);
        g.setColor(MD3Color.surface());
        g.fillRect(0, 0, size.width, size.height);
        root.paint(g);
        g.dispose();

        File parent = file.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        ImageIO.write(image, "png", file);
    }

    /**
     * Copies the desktop's own font rendering hints onto an offscreen canvas.
     *
     * <p>
     * Matters more than it looks. A component measures itself with the font rendering context the
     * desktop hints imply, and forcing a different antialiasing mode at paint time gives the glyphs
     * different advances than the widths the layout was computed from - which shows up as the last
     * character of a long label being shaved off in the capture but not in the running launcher.
     */
    public static void applyDesktopFontHints(Graphics2D g) {
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

        if (hints instanceof Map) {
            g.addRenderingHints((Map<?, ?>) hints);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    private static void layoutTree(Component component) {
        component.doLayout();

        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                layoutTree(child);
            }
        }
    }

    public static JPanel build() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(MD3Color.surface());
        root.setBorder(MD3Spacing.border(MD3Spacing.XL));

        root.add(section("Typography", typography()));
        root.add(section("Colour roles", colourRoles()));
        root.add(section("Buttons", buttons()));
        root.add(section("Icon buttons", iconButtons()));
        root.add(section("Icons", icons()));
        root.add(section("Text fields", textFields()));
        root.add(section("Switches and chips", switchesAndChips()));
        root.add(section("Sliders", sliders()));
        root.add(section("Progress", progress()));
        root.add(section("Navigation", navigation()));
        root.add(section("Cards", cards()));
        root.add(section("List items", listItems()));

        return root;
    }

    private static JComponent textFields() {
        JPanel row = flow();

        MD3TextField outlined = new MD3TextField("Instance name");
        outlined.setColumns(14);

        MD3TextField filled = MD3TextField.filled("Search");
        filled.setLeadingIcon(MD3Icons.SEARCH);
        filled.setColumns(14);

        MD3TextField withText = new MD3TextField("Memory (MB)");
        withText.setText("4096");
        withText.setSupportingText("Between 1024 and 16384");
        withText.setColumns(14);

        MD3TextField errored = new MD3TextField("Server address");
        errored.setText("not a host");
        errored.setSupportingText("That is not a valid address");
        errored.setError(true);
        errored.setColumns(14);

        MD3TextField disabled = new MD3TextField("Locked");
        disabled.setText("read only");
        disabled.setEnabled(false);
        disabled.setColumns(10);

        for (MD3TextField field : new MD3TextField[] { outlined, filled, withText, errored, disabled }) {
            row.add(field);
        }

        MD3TextArea area = new MD3TextArea(3, 24);
        area.setText("A description that runs to more than one line.");
        row.add(area.contained(88));

        return row;
    }

    private static JComponent switchesAndChips() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JPanel switches = flow();
        switches.add(tag("SWITCH"));

        MD3Switch on = new MD3Switch("Enabled");
        on.setSelected(true);

        MD3Switch off = new MD3Switch("Disabled");

        MD3Switch disabledOn = new MD3Switch("Off limits");
        disabledOn.setSelected(true);
        disabledOn.setEnabled(false);

        MD3Switch disabledOff = new MD3Switch("Off limits");
        disabledOff.setEnabled(false);

        switches.add(on);
        switches.add(off);
        switches.add(disabledOn);
        switches.add(disabledOff);

        JPanel chips = flow();
        chips.add(tag("CHIP"));

        MD3Chip selected = MD3Chip.filter("Fabric");
        selected.setSelected(true);

        MD3Chip unselected = MD3Chip.filter("Forge");
        MD3Chip suggestion = MD3Chip.suggestion("Minecraft 1.21.4");
        MD3Chip assist = MD3Chip.assist("Open folder", MD3Icons.FOLDER);
        MD3Chip input = MD3Chip.input("sodium");

        MD3Chip disabledChip = MD3Chip.filter("Unavailable");
        disabledChip.setEnabled(false);

        chips.add(selected);
        chips.add(unselected);
        chips.add(suggestion);
        chips.add(assist);
        chips.add(input);
        chips.add(disabledChip);

        JPanel radios = flow();
        radios.add(tag("RADIO"));

        MD3Radio fabric = new MD3Radio("Fabric");
        fabric.setSelected(true);
        MD3Radio forge = new MD3Radio("Forge");
        MD3Radio quilt = new MD3Radio("Quilt");
        quilt.setEnabled(false);

        radios.add(fabric);
        radios.add(forge);
        radios.add(quilt);

        panel.add(switches);
        panel.add(chips);
        panel.add(radios);

        return panel;
    }

    private static JComponent sliders() {
        JPanel row = flow();

        MD3Slider memory = new MD3Slider(1024, 16384, 4096);
        memory.setPreferredSize(new java.awt.Dimension(240, memory.getPreferredSize().height));

        MD3Slider disabled = new MD3Slider(0, 100, 40);
        disabled.setEnabled(false);
        disabled.setPreferredSize(new java.awt.Dimension(160, disabled.getPreferredSize().height));

        row.add(tag("RANGE"));
        row.add(memory);
        row.add(tag("DISABLED"));
        row.add(disabled);

        return row;
    }

    private static JComponent section(String title, JComponent content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel(title);
        heading.setFont(MD3Type.font(MD3Type.TITLE_LARGE));
        heading.setForeground(MD3Color.onSurface());
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setBorder(MD3Spacing.border(MD3Spacing.XL, 0, MD3Spacing.M, 0));

        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(heading);
        panel.add(content);
        panel.add(new MD3Divider());

        return panel;
    }

    private static JPanel flow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, MD3Spacing.M, MD3Spacing.S));
        panel.setOpaque(false);

        return panel;
    }

    private static JComponent typography() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        MD3Type.Role[] roles = { MD3Type.HEADLINE_SMALL, MD3Type.TITLE_LARGE, MD3Type.TITLE_MEDIUM,
                MD3Type.TITLE_SMALL, MD3Type.BODY_LARGE, MD3Type.BODY_MEDIUM, MD3Type.BODY_SMALL,
                MD3Type.LABEL_LARGE, MD3Type.LABEL_MEDIUM, MD3Type.LABEL_SMALL };

        for (MD3Type.Role role : roles) {
            JLabel label = new JLabel(role.name + "  -  All the Mods 9  -  Minecraft 1.21.4");
            label.setFont(MD3Type.font(role));
            label.setForeground(role.name.startsWith("body") ? MD3Color.onSurfaceVariant() : MD3Color.onSurface());
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
        }

        return panel;
    }

    private static JComponent colourRoles() {
        String[][] groups = {
                { MD3Color.PRIMARY, MD3Color.ON_PRIMARY, MD3Color.PRIMARY_CONTAINER, MD3Color.ON_PRIMARY_CONTAINER },
                { MD3Color.SECONDARY, MD3Color.ON_SECONDARY, MD3Color.SECONDARY_CONTAINER,
                        MD3Color.ON_SECONDARY_CONTAINER },
                { MD3Color.TERTIARY, MD3Color.ON_TERTIARY, MD3Color.TERTIARY_CONTAINER,
                        MD3Color.ON_TERTIARY_CONTAINER },
                { MD3Color.ERROR, MD3Color.ON_ERROR, MD3Color.ERROR_CONTAINER, MD3Color.ON_ERROR_CONTAINER },
                { MD3Color.SURFACE_CONTAINER_LOWEST, MD3Color.SURFACE_CONTAINER_LOW, MD3Color.SURFACE_CONTAINER,
                        MD3Color.SURFACE_CONTAINER_HIGH },
                { MD3Color.SURFACE_CONTAINER_HIGHEST, MD3Color.SURFACE_VARIANT, MD3Color.OUTLINE,
                        MD3Color.OUTLINE_VARIANT } };

        JPanel panel = new JPanel(new GridLayout(groups.length, 4, 8, 8));
        panel.setOpaque(false);

        for (String[] group : groups) {
            for (String role : group) {
                panel.add(swatch(role));
            }
        }

        panel.setMaximumSize(new Dimension(880, groups.length * 56));

        return panel;
    }

    private static JComponent swatch(String role) {
        Color background = MD3Color.get(role);
        String shortName = role.substring(MD3Color.PREFIX.length());

        JLabel label = new JLabel(shortName);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(readableOn(background));
        label.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        label.setBorder(MD3Spacing.border(MD3Spacing.S));
        label.setPreferredSize(new Dimension(200, 48));

        return label;
    }

    /**
     * Picks whichever of the on-surface pair actually reads against a swatch, so the gallery's own
     * labels never become the thing that is unreadable.
     */
    private static Color readableOn(Color background) {
        double luminance = (0.2126 * background.getRed() + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue()) / 255.0;

        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private static JComponent buttons() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        for (MD3Button.Variant variant : MD3Button.Variant.values()) {
            JPanel row = flow();
            row.add(tag(variant.name()));

            MD3Button plain = new MD3Button("Play", variant);
            MD3Button withIcon = new MD3Button("Play", MD3Icon.of(MD3Icons.PLAY), variant);
            MD3Button disabled = new MD3Button("Disabled", variant);
            disabled.setEnabled(false);

            row.add(plain);
            row.add(withIcon);
            row.add(disabled);
            panel.add(row);
        }

        JPanel sizes = flow();
        sizes.add(tag("SIZE"));
        sizes.add(MD3Button.filled("Small").withButtonSize(MD3Button.Size.SMALL));
        sizes.add(MD3Button.filled("Medium"));
        sizes.add(MD3Button.filled("Large").withButtonSize(MD3Button.Size.LARGE));
        panel.add(sizes);

        JPanel tones = flow();
        tones.add(tag("ERROR"));
        tones.add(MD3Button.filledError("Delete"));
        tones.add(MD3Button.tonal("Remove").withTone(MD3Button.Tone.ERROR));
        tones.add(MD3Button.outlined("Cancel").withTone(MD3Button.Tone.ERROR));
        tones.add(MD3Button.text("Dismiss").withTone(MD3Button.Tone.ERROR));
        panel.add(tones);

        JPanel selected = flow();
        selected.add(tag("SELECTED"));

        MD3Button selectedOutlined = MD3Button.outlined("Grid");
        selectedOutlined.setSelected(true);

        selected.add(selectedOutlined);
        selected.add(MD3Button.outlined("List"));
        panel.add(selected);

        JPanel menus = flow();
        menus.add(tag("MENU"));
        menus.add(MD3MenuButton.filled("Play", MD3Icon.of(MD3Icons.PLAY), demoMenu()).withSplit(true));
        menus.add(MD3MenuButton.tonal("Open", demoMenu()).withSplit(true));
        panel.add(menus);

        JPanel bar = flow();
        bar.add(tag("BAR"));
        MD3ButtonBar actions = new MD3ButtonBar();
        actions.leading(MD3Button.filled("Play", MD3Icon.of(MD3Icons.PLAY)));
        actions.leading(MD3Button.tonal("Update").withButtonSize(MD3Button.Size.SMALL));
        actions.trailing(new MD3IconButton(MD3Icons.MORE_VERT, "More", MD3IconButton.Variant.STANDARD,
                MD3IconButton.Size.SMALL));
        actions.setPreferredSize(new Dimension(280, actions.getPreferredSize().height));
        bar.add(actions);
        panel.add(bar);

        JPanel groups = flow();
        groups.add(tag("GROUP"));

        MD3ButtonGroup group = new MD3ButtonGroup();
        group.addOption("Name");
        group.addOption("Date");
        group.addOption("Size");
        groups.add(group);
        panel.add(groups);

        JPanel fabs = flow();
        fabs.add(tag("FAB"));
        fabs.add(MD3Fab.small(MD3Icons.ADD, "Create"));
        fabs.add(new MD3Fab(MD3Icons.ADD, "Create instance"));
        fabs.add(MD3Fab.extended(MD3Icons.ADD, "Create instance"));
        panel.add(fabs);

        return panel;
    }

    private static JPopupMenu demoMenu() {
        MD3PopupMenu menu = new MD3PopupMenu();
        menu.add("Play Online");
        menu.add("Play Offline");

        return menu;
    }

    private static JComponent iconButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JPanel row = flow();

        for (MD3IconButton.Variant variant : MD3IconButton.Variant.values()) {
            row.add(tag(variant.name()));
            row.add(new MD3IconButton(MD3Icons.MORE_VERT, "More options", variant));

            MD3IconButton disabled = new MD3IconButton(MD3Icons.MORE_VERT, "More options", variant);
            disabled.setEnabled(false);
            row.add(disabled);
        }

        panel.add(row);

        JPanel selected = flow();
        selected.add(tag("SELECTED"));

        for (MD3IconButton.Variant variant : MD3IconButton.Variant.values()) {
            MD3IconButton on = new MD3IconButton(MD3Icons.GRID_VIEW, "Grid view", variant);
            on.setSelected(true);
            selected.add(on);
        }

        panel.add(selected);

        JPanel sizes = flow();
        sizes.add(tag("SIZE"));
        sizes.add(new MD3IconButton(MD3Icons.MORE_VERT, "More options", MD3IconButton.Variant.STANDARD,
                MD3IconButton.Size.SMALL));
        sizes.add(new MD3IconButton(MD3Icons.MORE_VERT, "More options"));
        sizes.add(new MD3IconButton(MD3Icons.MORE_VERT, "More options", MD3IconButton.Variant.STANDARD,
                MD3IconButton.Size.LARGE));
        panel.add(sizes);

        return panel;
    }

    private static JComponent icons() {
        JPanel row = flow();

        for (Field field : MD3Icons.class.getDeclaredFields()) {
            if (field.getType() != MD3Icon.Painter.class) {
                continue;
            }

            try {
                JLabel label = new JLabel(MD3Icon.of((MD3Icon.Painter) field.get(null),
                        MD3Spacing.ICON_SIZE_LARGE));
                label.setForeground(MD3Color.onSurfaceVariant());
                label.setToolTipText(field.getName());
                row.add(label);
            } catch (IllegalAccessException ignored) {
                // a painter we cannot read is a painter we cannot show; nothing useful to do
            }
        }

        return row;
    }

    private static JComponent progress() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JPanel linear = flow();
        linear.add(tag("LINEAR"));

        for (int value : new int[] { 0, 35, 72, 100 }) {
            MD3LinearProgress bar = new MD3LinearProgress(0, 100);
            bar.setValue(value);
            bar.setPreferredSize(new Dimension(160, bar.getPreferredSize().height));
            linear.add(bar);
        }

        MD3LinearProgress captioned = new MD3LinearProgress(0, 100);
        captioned.setValue(48);
        captioned.setString("12/25 Tasks Done");
        captioned.setStringPainted(true);
        captioned.setPreferredSize(new Dimension(180, captioned.getPreferredSize().height));
        linear.add(captioned);

        MD3LinearProgress waiting = new MD3LinearProgress();
        waiting.setIndeterminate(true);
        waiting.setPreferredSize(new Dimension(160, waiting.getPreferredSize().height));
        linear.add(waiting);

        JPanel circular = flow();
        circular.add(tag("CIRCULAR"));

        for (int value : new int[] { 25, 60, 90 }) {
            MD3CircularProgress ring = new MD3CircularProgress();
            ring.setValue(value);
            circular.add(ring);
        }

        circular.add(MD3CircularProgress.indeterminate());
        circular.add(MD3CircularProgress.inline());

        panel.add(linear);
        panel.add(circular);

        return panel;
    }

    private static JComponent navigation() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(880, 300));
        panel.setMaximumSize(new Dimension(880, 300));

        MD3NavigationRail rail = new MD3NavigationRail();
        // the rail is 80dp wide, so its header has to be an icon-only target - a FAB fits, an
        // extended one with a label beside the icon would not
        rail.setHeader(new MD3Fab(MD3Icons.ADD, "Create instance"));
        rail.addDestination(MD3Icons.ARTICLE, "News");
        rail.addDestination(MD3Icons.SEARCH, "Discover");
        rail.addDestination(MD3Icons.PACKAGE, "Instances");
        rail.addDestination(MD3Icons.DNS, "Servers");
        rail.addSeparator();
        rail.addDestination(MD3Icons.SETTINGS, "Settings");
        rail.setSelectedIndex(2);

        MD3TopAppBar bar = new MD3TopAppBar("Instances");
        MD3TextField search = MD3TextField.filled("Search");
        search.setLeadingIcon(MD3Icons.SEARCH);
        search.setColumns(18);
        bar.setCentreComponent(search);
        bar.addAction(MD3Icons.SORT, "Sort", e -> {
        });
        bar.addAction(MD3Icons.GRID_VIEW, "View", e -> {
        });
        bar.addAction(MD3Icons.PERSON, "Account", e -> {
        });

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(MD3Color.surfaceContainerLow());
        content.add(bar, BorderLayout.NORTH);

        JLabel placeholder = new JLabel("page content", JLabel.CENTER);
        placeholder.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
        placeholder.setForeground(MD3Color.onSurfaceVariant());
        content.add(placeholder, BorderLayout.CENTER);

        panel.add(rail, BorderLayout.WEST);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private static JComponent cards() {
        JPanel row = flow();

        for (MD3Card.Variant variant : MD3Card.Variant.values()) {
            MD3Card card = new MD3Card(variant, new BorderLayout());
            card.setPreferredSize(new Dimension(240, 132));

            JLabel title = new JLabel(
                    variant.name().charAt(0) + variant.name().substring(1).toLowerCase(Locale.ROOT) + " card");
            title.setFont(MD3Type.font(MD3Type.TITLE_MEDIUM));
            title.setForeground(MD3Color.onSurface());

            JLabel body = new JLabel("<html>Forge 47.2.0 &middot; 214 mods installed</html>");
            body.setFont(MD3Type.font(MD3Type.BODY_MEDIUM));
            body.setForeground(MD3Color.onSurfaceVariant());

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(title);
            text.add(body);

            JPanel actions = flow();
            actions.add(MD3Button.filled("Play", MD3Icon.of(MD3Icons.PLAY)));
            actions.add(new MD3IconButton(MD3Icons.MORE_VERT, "More options"));

            card.add(text, BorderLayout.NORTH);
            card.add(actions, BorderLayout.SOUTH);
            card.setClickable(true);

            row.add(card);
        }

        return row;
    }

    private static JComponent listItems() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(720, Integer.MAX_VALUE));

        List<MD3ListItem> items = new ArrayList<>();

        MD3ListItem one = MD3ListItem.of("One line");
        one.setLeadingIcon(MD3Icons.FOLDER);
        items.add(one);

        MD3ListItem two = MD3ListItem.of("Concurrent downloads",
                "How many files to fetch at once. Higher is faster on a good connection.");
        two.setLeadingIcon(MD3Icons.DOWNLOAD);
        two.setTrailing(new MD3IconButton(MD3Icons.CHEVRON_RIGHT, "Change"));
        items.add(two);

        MD3ListItem three = MD3ListItem.of("Enable console",
                "Opens the console window when the launcher starts.");
        three.setOverline("REQUIRES RESTART");
        three.setLeadingIcon(MD3Icons.SETTINGS);
        three.setTrailing(MD3Button.text("Change"));
        items.add(three);

        for (int i = 0; i < items.size(); i++) {
            MD3ListItem item = items.get(i);
            item.setClickable(true);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(item);

            if (i < items.size() - 1) {
                MD3Divider divider = MD3Divider.inset();
                divider.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(divider);
            }
        }

        return panel;
    }

    private static JLabel tag(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MD3Type.font(MD3Type.LABEL_MEDIUM));
        label.setForeground(MD3Color.onSurfaceVariant());
        label.setPreferredSize(new Dimension(88, 24));

        return label;
    }

    static {
        // the gallery is often the first thing to touch the type scale in a bare JVM, where no
        // default font has been published yet
        if (UIManager.getFont("defaultFont") == null) {
            UIManager.put("defaultFont", new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
        }
    }
}
