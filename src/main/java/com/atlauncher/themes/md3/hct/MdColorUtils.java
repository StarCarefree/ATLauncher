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

import java.awt.Color;

/**
 * Conversions between sRGB, linear RGB, CIE XYZ and L*.
 *
 * <p>
 * All "argb" values in this package are packed ints in the same layout as {@link Color#getRGB()}.
 */
public final class MdColorUtils {
    private static final double[][] SRGB_TO_XYZ = {
            { 0.41233895, 0.35762064, 0.18051042 },
            { 0.2126, 0.7152, 0.0722 },
            { 0.01932141, 0.11916382, 0.95034478 } };

    private static final double[][] XYZ_TO_SRGB = {
            { 3.2413774792388685, -1.5376652402851851, -0.49885366846268053 },
            { -0.9691452513005321, 1.8758853451067872, 0.04156585616912061 },
            { 0.05562093689691305, -0.20395524564742123, 1.0571799111220335 } };

    private static final double[] WHITE_POINT_D65 = { 95.047, 100.0, 108.883 };

    private MdColorUtils() {
    }

    public static double[] whitePointD65() {
        return WHITE_POINT_D65.clone();
    }

    public static int argbFromRgb(int red, int green, int blue) {
        return (255 << 24) | ((red & 255) << 16) | ((green & 255) << 8) | (blue & 255);
    }

    public static int alphaFromArgb(int argb) {
        return (argb >> 24) & 255;
    }

    public static int redFromArgb(int argb) {
        return (argb >> 16) & 255;
    }

    public static int greenFromArgb(int argb) {
        return (argb >> 8) & 255;
    }

    public static int blueFromArgb(int argb) {
        return argb & 255;
    }

    public static Color colorFromArgb(int argb) {
        return new Color(argb, true);
    }

    public static int argbFromColor(Color color) {
        return color.getRGB();
    }

    public static double[] xyzFromArgb(int argb) {
        double r = linearized(redFromArgb(argb));
        double g = linearized(greenFromArgb(argb));
        double b = linearized(blueFromArgb(argb));

        return MdMathUtils.matrixMultiply(new double[] { r, g, b }, SRGB_TO_XYZ);
    }

    public static int argbFromXyz(double x, double y, double z) {
        double linearR = XYZ_TO_SRGB[0][0] * x + XYZ_TO_SRGB[0][1] * y + XYZ_TO_SRGB[0][2] * z;
        double linearG = XYZ_TO_SRGB[1][0] * x + XYZ_TO_SRGB[1][1] * y + XYZ_TO_SRGB[1][2] * z;
        double linearB = XYZ_TO_SRGB[2][0] * x + XYZ_TO_SRGB[2][1] * y + XYZ_TO_SRGB[2][2] * z;

        return argbFromRgb(delinearized(linearR), delinearized(linearG), delinearized(linearB));
    }

    /**
     * The grey with the given L*, which is what HCT falls back to when a hue/chroma pair cannot be
     * represented in sRGB.
     */
    public static int argbFromLstar(double lstar) {
        int component = delinearized(yFromLstar(lstar));

        return argbFromRgb(component, component, component);
    }

    public static double lstarFromArgb(int argb) {
        return 116.0 * labF(xyzFromArgb(argb)[1] / 100.0) - 16.0;
    }

    public static double yFromLstar(double lstar) {
        return 100.0 * labInvf((lstar + 16.0) / 116.0);
    }

    /**
     * sRGB component (0-255) to linear RGB (0-100).
     */
    public static double linearized(int rgbComponent) {
        double normalized = rgbComponent / 255.0;

        if (normalized <= 0.040449936) {
            return normalized / 12.92 * 100.0;
        }

        return Math.pow((normalized + 0.055) / 1.055, 2.4) * 100.0;
    }

    /**
     * Linear RGB (0-100) to sRGB component, clamped to 0-255.
     */
    public static int delinearized(double rgbComponent) {
        double normalized = rgbComponent / 100.0;
        double delinearized;

        if (normalized <= 0.0031308) {
            delinearized = normalized * 12.92;
        } else {
            delinearized = 1.055 * Math.pow(normalized, 1.0 / 2.4) - 0.055;
        }

        return MdMathUtils.clampInt(0, 255, (int) Math.round(delinearized * 255.0));
    }

    /**
     * Relative luminance as used by the WCAG contrast formula.
     */
    public static double relativeLuminance(int argb) {
        return xyzFromArgb(argb)[1] / 100.0;
    }

    /**
     * WCAG 2.x contrast ratio between two opaque colours, in the range [1, 21].
     */
    public static double contrastRatio(int argb1, int argb2) {
        double l1 = relativeLuminance(argb1);
        double l2 = relativeLuminance(argb2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double labF(double t) {
        double e = 216.0 / 24389.0;
        double kappa = 24389.0 / 27.0;

        if (t > e) {
            return Math.pow(t, 1.0 / 3.0);
        }

        return (kappa * t + 16) / 116;
    }

    private static double labInvf(double ft) {
        double e = 216.0 / 24389.0;
        double kappa = 24389.0 / 27.0;
        double ft3 = ft * ft * ft;

        if (ft3 > e) {
            return ft3;
        }

        return (116 * ft - 16) / kappa;
    }
}
