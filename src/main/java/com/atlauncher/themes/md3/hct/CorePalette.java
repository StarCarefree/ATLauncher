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
package com.atlauncher.themes.md3.hct;

/**
 * The six tonal palettes a Material 3 theme is generated from, derived from one seed colour.
 *
 * <p>
 * {@link #of(int)} produces the vibrant palettes used for a launcher-wide theme - chroma is forced
 * up to a minimum so a muted seed still yields a usable accent. {@link #contentOf(int)} preserves
 * the seed's own chroma, which is what you want when deriving a theme from artwork such as an
 * instance's cover image.
 */
public final class CorePalette {
    /** Primary accent. */
    public final TonalPalette a1;
    /** Secondary accent, a desaturated sibling of {@link #a1}. */
    public final TonalPalette a2;
    /** Tertiary accent, rotated 60 degrees from {@link #a1}. */
    public final TonalPalette a3;
    /** Neutral, used for surfaces and text. */
    public final TonalPalette n1;
    /** Neutral variant, used for outlines and secondary text. */
    public final TonalPalette n2;
    /** Error, fixed at the Material 3 red regardless of seed. */
    public final TonalPalette error;

    private CorePalette(int seedArgb, boolean isContent) {
        Hct hct = Hct.fromInt(seedArgb);
        double hue = hct.getHue();
        double chroma = hct.getChroma();

        if (isContent) {
            a1 = TonalPalette.fromHueAndChroma(hue, chroma);
            a2 = TonalPalette.fromHueAndChroma(hue, chroma / 3.0);
            a3 = TonalPalette.fromHueAndChroma(hue + 60.0, chroma / 2.0);
            n1 = TonalPalette.fromHueAndChroma(hue, Math.min(chroma / 12.0, 4.0));
            n2 = TonalPalette.fromHueAndChroma(hue, Math.min(chroma / 6.0, 8.0));
        } else {
            a1 = TonalPalette.fromHueAndChroma(hue, Math.max(48.0, chroma));
            a2 = TonalPalette.fromHueAndChroma(hue, 16.0);
            a3 = TonalPalette.fromHueAndChroma(hue + 60.0, 24.0);
            n1 = TonalPalette.fromHueAndChroma(hue, 4.0);
            n2 = TonalPalette.fromHueAndChroma(hue, 8.0);
        }

        error = TonalPalette.fromHueAndChroma(25.0, 84.0);
    }

    public static CorePalette of(int seedArgb) {
        return new CorePalette(seedArgb, false);
    }

    public static CorePalette contentOf(int seedArgb) {
        return new CorePalette(seedArgb, true);
    }
}
