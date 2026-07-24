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
 * A colour expressed as Hue, Chroma and Tone - the colour space Material 3 is built on.
 *
 * <p>
 * Hue and chroma come from CAM16, tone is CIE L*. The useful property is that tone maps directly
 * onto perceived lightness, so two colours with the same tone contrast identically against a given
 * background no matter what their hue is. Material 3 leans on that to guarantee its "on" colours
 * are always readable.
 *
 * <p>
 * Not every hue/chroma/tone triple exists in sRGB. When one doesn't, the requested chroma is
 * lowered until the colour is displayable - hue and tone are always preserved.
 */
public final class Hct {
    private double hue;
    private double chroma;
    private double tone;
    private int argb;

    private Hct(int argb) {
        setInternalState(argb);
    }

    /**
     * @param hue    0 to 360, in degrees
     * @param chroma informally, colourfulness. The actual chroma may be lower if the requested one
     *               is not displayable in sRGB at this hue and tone
     * @param tone   0 to 100, CIE L*
     */
    public static Hct from(double hue, double chroma, double tone) {
        return new Hct(solveToInt(hue, chroma, tone));
    }

    public static Hct fromInt(int argb) {
        return new Hct(argb);
    }

    public int toInt() {
        return argb;
    }

    public double getHue() {
        return hue;
    }

    public double getChroma() {
        return chroma;
    }

    public double getTone() {
        return tone;
    }

    public Hct withTone(double newTone) {
        return from(hue, chroma, newTone);
    }

    private void setInternalState(int argb) {
        this.argb = argb;

        Cam16 cam = Cam16.fromInt(argb);
        this.hue = cam.getHue();
        this.chroma = cam.getChroma();
        this.tone = MdColorUtils.lstarFromArgb(argb);
    }

    /**
     * Finds the sRGB colour closest to the requested HCT triple.
     *
     * <p>
     * Walks chroma down by bisection until a displayable colour is found. The first probe uses the
     * requested chroma directly, so fully in-gamut colours - the common case - cost a single inner
     * search.
     */
    static int solveToInt(double hue, double chroma, double tone) {
        if (chroma < 1.0 || Math.round(tone) <= 0.0 || Math.round(tone) >= 100.0) {
            return MdColorUtils.argbFromLstar(tone);
        }

        hue = MdMathUtils.sanitizeDegreesDouble(hue);

        double high = chroma;
        double mid = chroma;
        double low = 0.0;
        boolean isFirstLoop = true;
        Cam16 answer = null;

        while (Math.abs(low - high) >= 0.4) {
            Cam16 possibleAnswer = findCamByJ(hue, mid, tone);

            if (isFirstLoop) {
                if (possibleAnswer != null) {
                    return possibleAnswer.toInt();
                }

                isFirstLoop = false;
                mid = low + (high - low) / 2.0;
                continue;
            }

            if (possibleAnswer == null) {
                high = mid;
            } else {
                answer = possibleAnswer;
                low = mid;
            }

            mid = low + (high - low) / 2.0;
        }

        if (answer == null) {
            return MdColorUtils.argbFromLstar(tone);
        }

        return answer.toInt();
    }

    /**
     * Searches CAM16 lightness (J) for a colour that lands on the requested tone at the requested
     * hue and chroma, or returns null if no such sRGB colour exists.
     */
    private static Cam16 findCamByJ(double hue, double chroma, double tone) {
        double low = 0.0;
        double high = 100.0;
        double bestdL = 1000.0;
        double bestdE = 1000.0;
        Cam16 bestCam = null;

        while (Math.abs(low - high) > 0.01) {
            double mid = low + (high - low) / 2.0;

            int clipped = Cam16.fromJch(mid, chroma, hue).toInt();
            double clippedLstar = MdColorUtils.lstarFromArgb(clipped);
            double dL = Math.abs(tone - clippedLstar);

            if (dL < 0.2) {
                Cam16 camClipped = Cam16.fromInt(clipped);
                double dE = camClipped.distance(Cam16.fromJch(camClipped.getJ(), camClipped.getChroma(), hue));

                if (dE <= 1.0 && dE <= bestdE) {
                    bestdL = dL;
                    bestdE = dE;
                    bestCam = camClipped;
                }
            }

            if (bestdL == 0 && bestdE == 0) {
                break;
            }

            if (clippedLstar < tone) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return bestCam;
    }
}
