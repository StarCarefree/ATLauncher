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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlauncher.gui.md3.input.MD3FilterChip;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.utils.ComboItem;

/**
 * The dropdown chip the pack browser and the mod browser both filter with.
 *
 * <p>
 * It replaced a labelled combo box, and the thing a combo box did for free was hold a selection
 * across a rebuild of its items. Switching platform rebuilds every chip, so what these pin down is
 * that putting a selection back is not mistaken for the user having made one - the mod browser
 * reloads its grid on every change, and a chip that announced its own repopulation would reload it
 * once per chip.
 */
public class MD3FilterChipTest {
    @BeforeEach
    public void installTheme() throws Exception {
        Class.forName("com.atlauncher.themes.MaterialDark").getMethod("install").invoke(null);

        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("md.sys.motion.reduced", Boolean.TRUE);
    }

    private static List<ComboItem<String>> sections() {
        return Arrays.asList(new ComboItem<>("Mods", "Mods"), new ComboItem<>("Shaders", "Shaders"),
                new ComboItem<>("Worlds", "Worlds"));
    }

    @Test
    public void testTheFirstOptionIsTheOneInEffect() {
        MD3FilterChip<String> chip = new MD3FilterChip<>("Type", true, () -> {
        });
        chip.setOptions(sections());

        assertEquals("Mods", chip.getValue(), "the chip is filtering by nothing after being given values");
    }

    @Test
    public void testASelectionSurvivesTheOptionsBeingRebuilt() {
        AtomicInteger changes = new AtomicInteger();
        MD3FilterChip<String> chip = new MD3FilterChip<>("Type", true, changes::incrementAndGet);

        chip.setOptions(sections());
        assertTrue(chip.selectValue("Shaders"), "the chip has no option for a value it was given");

        chip.setOptions(sections());
        assertTrue(chip.selectValue("Shaders"), "the chip lost an option it had before");

        assertEquals("Shaders", chip.getValue(), "the selection did not survive the rebuild");
        assertEquals(0, changes.get(),
                "putting a selection back counted as the user changing it, which reloads the grid for nothing");
    }

    @Test
    public void testAValueThatIsNoLongerOfferedLeavesTheSelectionAlone() {
        MD3FilterChip<String> chip = new MD3FilterChip<>("Type", true, () -> {
        });

        chip.setOptions(sections());

        assertFalse(chip.selectValue("Plugins"), "the chip claimed to have an option it was never given");
        assertEquals("Mods", chip.getValue(), "a value that is not offered moved the selection anyway");
    }

    @Test
    public void testAnEmptyChipHasNoValue() {
        MD3FilterChip<String> chip = new MD3FilterChip<>("Category", true, () -> {
        });

        assertTrue(chip.isEmpty(), "a chip that was given nothing does not report itself empty");
        assertNull(chip.getValue(), "a chip with no options is filtering by something");

        chip.setOptions(Collections.<ComboItem<String>>emptyList());

        assertNull(chip.getValue(), "an emptied chip kept a value");
    }

    /**
     * A filter says what it is filtering by; a sort has to keep its name, or "Popularity" sits in a
     * row of filters looking like one of them.
     */
    @Test
    public void testTheFaceSaysWhatItIsFilteringBy() {
        MD3FilterChip<String> filter = new MD3FilterChip<>("Type", true, () -> {
        });
        filter.setOptions(sections());

        assertEquals("Mods", filter.getChip().getText(), "a filter chip is not showing the value in effect");

        MD3FilterChip<String> sort = new MD3FilterChip<>("Sort", false, () -> {
        });
        sort.setOptions(Collections.singletonList(new ComboItem<>("Popularity", "Popularity")));

        assertEquals("Sort: Popularity", sort.getChip().getText(),
                "a sort chip dropped its name, so its value reads as a filter");
    }

    /**
     * An empty chip used to return no menu, so clicking Category before the network came back -
     * or after a failed fetch - did nothing. The chip has to open and say so.
     */
    @Test
    public void testAnEmptyChipStillOpensAMenu() {
        MD3FilterChip<String> chip = new MD3FilterChip<>("Category", true, () -> {
        });

        JPopupMenu menu = chip.createMenu();

        assertNotNull(menu, "an empty chip built no menu, so clicking it does nothing");
        assertTrue(menu.getComponentCount() > 0, "the menu has nothing to show");
    }

    /**
     * CurseForge has dozens of categories and Minecraft has hundreds of versions. Without a search
     * box the chip can be opened and still not used.
     */
    @Test
    public void testALongListCanBeSearched() {
        AtomicInteger changes = new AtomicInteger();
        MD3FilterChip<String> chip = new MD3FilterChip<>("Category", true, changes::incrementAndGet);
        chip.setOptions(manyCategories());

        JPopupMenu menu = chip.createMenu();
        MD3TextField search = find(menu, MD3TextField.class);

        assertNotNull(search, "a long list has no search box");

        search.setText("tech");

        JList<?> list = find(menu, JList.class);

        assertNotNull(list, "the menu has no list of values");
        assertEquals(1, list.getModel().getSize(), "search did not narrow the list");
        assertEquals("Technology", list.getModel().getElementAt(0).toString());

        list.setSelectedIndex(0);
        list.getActionMap().get("md3.choose").actionPerformed(new ActionEvent(list, 0, "md3.choose"));

        assertEquals("tech", chip.getValue(), "picking the filtered row did not choose that value");
        assertEquals(1, changes.get(), "choosing from a filtered list did not count as a change");
    }

    private static List<ComboItem<String>> manyCategories() {
        List<ComboItem<String>> values = new ArrayList<>();
        values.add(new ComboItem<>(null, "All Categories"));
        values.add(new ComboItem<>("adv", "Adventure and RPG"));
        values.add(new ComboItem<>("magic", "Magic"));
        values.add(new ComboItem<>("map", "Map and Information"));
        values.add(new ComboItem<>("redstone", "Redstone"));
        values.add(new ComboItem<>("storage", "Storage"));
        values.add(new ComboItem<>("tech", "Technology"));
        values.add(new ComboItem<>("world", "World Generation"));
        values.add(new ComboItem<>("lib", "Library / APIs"));

        return values;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return (T) child;
            }

            if (child instanceof Container) {
                T nested = find((Container) child, type);

                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }
}
