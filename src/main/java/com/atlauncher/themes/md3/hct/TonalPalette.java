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

import java.util.HashMap;
import java.util.Map;

/**
 * A single hue and chroma sampled at every tone from 0 (black) to 100 (white).
 *
 * <p>
 * Material 3 builds a theme out of six of these. Every colour role in the scheme is just a
 * {@code palette.tone(n)} lookup, which is what keeps a theme coherent - nothing is hand picked.
 */
public final class TonalPalette {
    private final Map<Integer, Integer> cache = new HashMap<>();
    private final double hue;
    private final double chroma;

    private TonalPalette(double hue, double chroma) {
        this.hue = hue;
        this.chroma = chroma;
    }

    public static TonalPalette fromInt(int argb) {
        Hct hct = Hct.fromInt(argb);

        return fromHueAndChroma(hct.getHue(), hct.getChroma());
    }

    public static TonalPalette fromHueAndChroma(double hue, double chroma) {
        return new TonalPalette(hue, chroma);
    }

    /**
     * @param tone 0 to 100, where 0 is black and 100 is white
     * @return the ARGB of this palette at the given tone
     */
    public int tone(int tone) {
        Integer color = cache.get(tone);

        if (color == null) {
            color = Hct.from(hue, chroma, tone).toInt();
            cache.put(tone, color);
        }

        return color;
    }

    public double getHue() {
        return hue;
    }

    public double getChroma() {
        return chroma;
    }
}
