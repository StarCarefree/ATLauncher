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
 * The environment a colour is being viewed in, as defined by CAM16.
 *
 * <p>
 * Only {@link #DEFAULT} is used by the launcher - it models an average sRGB display in a room with
 * typical lighting, which is what Material 3 assumes.
 */
public final class ViewingConditions {
    public static final ViewingConditions DEFAULT = defaultWithBackgroundLstar(50.0);

    private final double aw;
    private final double nbb;
    private final double ncb;
    private final double c;
    private final double nc;
    private final double n;
    private final double[] rgbD;
    private final double fl;
    private final double flRoot;
    private final double z;

    private ViewingConditions(double n, double aw, double nbb, double ncb, double c, double nc, double[] rgbD,
            double fl, double flRoot, double z) {
        this.n = n;
        this.aw = aw;
        this.nbb = nbb;
        this.ncb = ncb;
        this.c = c;
        this.nc = nc;
        this.rgbD = rgbD;
        this.fl = fl;
        this.flRoot = flRoot;
        this.z = z;
    }

    public static ViewingConditions defaultWithBackgroundLstar(double lstar) {
        return make(MdColorUtils.whitePointD65(), 200.0 / Math.PI * MdColorUtils.yFromLstar(50.0) / 100.0, lstar, 2.0,
                false);
    }

    public static ViewingConditions make(double[] whitePoint, double adaptingLuminance, double backgroundLstar,
            double surround, boolean discountingIlluminant) {
        // avoid divide by zero
        backgroundLstar = Math.max(0.1, backgroundLstar);

        double[] xyz = whitePoint;
        double rW = xyz[0] * Cam16.XYZ_TO_CAM16RGB[0][0] + xyz[1] * Cam16.XYZ_TO_CAM16RGB[0][1]
                + xyz[2] * Cam16.XYZ_TO_CAM16RGB[0][2];
        double gW = xyz[0] * Cam16.XYZ_TO_CAM16RGB[1][0] + xyz[1] * Cam16.XYZ_TO_CAM16RGB[1][1]
                + xyz[2] * Cam16.XYZ_TO_CAM16RGB[1][2];
        double bW = xyz[0] * Cam16.XYZ_TO_CAM16RGB[2][0] + xyz[1] * Cam16.XYZ_TO_CAM16RGB[2][1]
                + xyz[2] * Cam16.XYZ_TO_CAM16RGB[2][2];

        double f = 0.8 + surround / 10.0;
        double c = (f >= 0.9) ? MdMathUtils.lerp(0.59, 0.69, (f - 0.9) * 10.0)
                : MdMathUtils.lerp(0.525, 0.59, (f - 0.8) * 10.0);

        double d = discountingIlluminant ? 1.0
                : f * (1.0 - ((1.0 / 3.6) * Math.exp((-adaptingLuminance - 42.0) / 92.0)));
        d = MdMathUtils.clampDouble(0.0, 1.0, d);

        double nc = f;
        double[] rgbD = new double[] {
                d * (100.0 / rW) + 1.0 - d,
                d * (100.0 / gW) + 1.0 - d,
                d * (100.0 / bW) + 1.0 - d };

        double k = 1.0 / (5.0 * adaptingLuminance + 1.0);
        double k4 = k * k * k * k;
        double k4F = 1.0 - k4;
        double fl = (k4 * adaptingLuminance) + (0.1 * k4F * k4F * Math.cbrt(5.0 * adaptingLuminance));

        double n = MdColorUtils.yFromLstar(backgroundLstar) / whitePoint[1];
        double z = 1.48 + Math.sqrt(n);
        double nbb = 0.725 / Math.pow(n, 0.2);
        double ncb = nbb;

        double[] rgbAFactors = new double[] {
                Math.pow(fl * rgbD[0] * rW / 100.0, 0.42),
                Math.pow(fl * rgbD[1] * gW / 100.0, 0.42),
                Math.pow(fl * rgbD[2] * bW / 100.0, 0.42) };

        double[] rgbA = new double[] {
                (400.0 * rgbAFactors[0]) / (rgbAFactors[0] + 27.13),
                (400.0 * rgbAFactors[1]) / (rgbAFactors[1] + 27.13),
                (400.0 * rgbAFactors[2]) / (rgbAFactors[2] + 27.13) };

        double aw = ((2.0 * rgbA[0]) + rgbA[1] + (0.05 * rgbA[2])) * nbb;

        return new ViewingConditions(n, aw, nbb, ncb, c, nc, rgbD, fl, Math.pow(fl, 0.25), z);
    }

    public double getAw() {
        return aw;
    }

    public double getN() {
        return n;
    }

    public double getNbb() {
        return nbb;
    }

    public double getNcb() {
        return ncb;
    }

    public double getC() {
        return c;
    }

    public double getNc() {
        return nc;
    }

    public double[] getRgbD() {
        return rgbD;
    }

    public double getFl() {
        return fl;
    }

    public double getFlRoot() {
        return flRoot;
    }

    public double getZ() {
        return z;
    }
}
