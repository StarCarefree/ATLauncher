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
 * CAM16, a colour appearance model.
 *
 * <p>
 * Used as the engine behind {@link Hct}. A colour is described by its hue, chroma (colourfulness)
 * and lightness as a human actually perceives them, rather than by how much of each phosphor a
 * monitor lights up. That is what makes it possible to hold a hue steady while walking a tone from
 * 0 to 100, which is the whole basis of the Material 3 tonal palettes.
 */
public final class Cam16 {
    static final double[][] XYZ_TO_CAM16RGB = {
            { 0.401288, 0.650173, -0.051461 },
            { -0.250268, 1.204414, 0.045854 },
            { -0.002079, 0.048952, 0.953127 } };

    private final double hue;
    private final double chroma;
    private final double j;
    private final double q;
    private final double m;
    private final double s;
    private final double jstar;
    private final double astar;
    private final double bstar;

    private Cam16(double hue, double chroma, double j, double q, double m, double s, double jstar, double astar,
            double bstar) {
        this.hue = hue;
        this.chroma = chroma;
        this.j = j;
        this.q = q;
        this.m = m;
        this.s = s;
        this.jstar = jstar;
        this.astar = astar;
        this.bstar = bstar;
    }

    public static Cam16 fromInt(int argb) {
        return fromIntInViewingConditions(argb, ViewingConditions.DEFAULT);
    }

    public static Cam16 fromIntInViewingConditions(int argb, ViewingConditions vc) {
        double redL = MdColorUtils.linearized(MdColorUtils.redFromArgb(argb));
        double greenL = MdColorUtils.linearized(MdColorUtils.greenFromArgb(argb));
        double blueL = MdColorUtils.linearized(MdColorUtils.blueFromArgb(argb));

        double x = 0.41233895 * redL + 0.35762064 * greenL + 0.18051042 * blueL;
        double y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL;
        double z = 0.01932141 * redL + 0.11916382 * greenL + 0.95034478 * blueL;

        return fromXyzInViewingConditions(x, y, z, vc);
    }

    static Cam16 fromXyzInViewingConditions(double x, double y, double z, ViewingConditions vc) {
        double rC = 0.401288 * x + 0.650173 * y - 0.051461 * z;
        double gC = -0.250268 * x + 1.204414 * y + 0.045854 * z;
        double bC = -0.002079 * x + 0.048952 * y + 0.953127 * z;

        double rD = vc.getRgbD()[0] * rC;
        double gD = vc.getRgbD()[1] * gC;
        double bD = vc.getRgbD()[2] * bC;

        double rAF = Math.pow(vc.getFl() * Math.abs(rD) / 100.0, 0.42);
        double gAF = Math.pow(vc.getFl() * Math.abs(gD) / 100.0, 0.42);
        double bAF = Math.pow(vc.getFl() * Math.abs(bD) / 100.0, 0.42);

        double rA = MdMathUtils.signum(rD) * 400.0 * rAF / (rAF + 27.13);
        double gA = MdMathUtils.signum(gD) * 400.0 * gAF / (gAF + 27.13);
        double bA = MdMathUtils.signum(bD) * 400.0 * bAF / (bAF + 27.13);

        double a = (11.0 * rA + -12.0 * gA + bA) / 11.0;
        double b = (rA + gA - 2.0 * bA) / 9.0;
        double u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0;
        double p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0;

        double atanDegrees = Math.atan2(b, a) * 180.0 / Math.PI;
        double hue = MdMathUtils.sanitizeDegreesDouble(atanDegrees);
        double hueRadians = hue * Math.PI / 180.0;

        double ac = p2 * vc.getNbb();
        double j = 100.0 * Math.pow(ac / vc.getAw(), vc.getC() * vc.getZ());
        double q = (4.0 / vc.getC()) * Math.sqrt(j / 100.0) * (vc.getAw() + 4.0) * vc.getFlRoot();

        double huePrime = (hue < 20.14) ? hue + 360 : hue;
        double eHue = 0.25 * (Math.cos(huePrime * Math.PI / 180.0 + 2.0) + 3.8);
        double p1 = 50000.0 / 13.0 * eHue * vc.getNc() * vc.getNcb();
        double t = p1 * Math.hypot(a, b) / (u + 0.305);

        double alpha = Math.pow(1.64 - Math.pow(0.29, vc.getN()), 0.73) * Math.pow(t, 0.9);
        double c = alpha * Math.sqrt(j / 100.0);
        double m = c * vc.getFlRoot();
        double s = 50.0 * Math.sqrt((alpha * vc.getC()) / (vc.getAw() + 4.0));

        double jstar = (1.0 + 100.0 * 0.007) * j / (1.0 + 0.007 * j);
        double mstar = 1.0 / 0.0228 * Math.log1p(0.0228 * m);

        return new Cam16(hue, c, j, q, m, s, jstar, mstar * Math.cos(hueRadians), mstar * Math.sin(hueRadians));
    }

    public static Cam16 fromJch(double j, double c, double h) {
        return fromJchInViewingConditions(j, c, h, ViewingConditions.DEFAULT);
    }

    static Cam16 fromJchInViewingConditions(double j, double c, double h, ViewingConditions vc) {
        double q = (4.0 / vc.getC()) * Math.sqrt(j / 100.0) * (vc.getAw() + 4.0) * vc.getFlRoot();
        double m = c * vc.getFlRoot();
        double alpha = c / Math.sqrt(j / 100.0);
        double s = 50.0 * Math.sqrt((alpha * vc.getC()) / (vc.getAw() + 4.0));

        double hueRadians = h * Math.PI / 180.0;
        double jstar = (1.0 + 100.0 * 0.007) * j / (1.0 + 0.007 * j);
        double mstar = 1.0 / 0.0228 * Math.log1p(0.0228 * m);

        return new Cam16(h, c, j, q, m, s, jstar, mstar * Math.cos(hueRadians), mstar * Math.sin(hueRadians));
    }

    /**
     * Perceptual distance in CAM16-UCS. A value at or below 1 is effectively indistinguishable.
     */
    public double distance(Cam16 other) {
        double dJ = getJstar() - other.getJstar();
        double dA = getAstar() - other.getAstar();
        double dB = getBstar() - other.getBstar();
        double dEPrime = Math.sqrt(dJ * dJ + dA * dA + dB * dB);

        return 1.41 * Math.pow(dEPrime, 0.63);
    }

    public int toInt() {
        return viewed(ViewingConditions.DEFAULT);
    }

    public int viewed(ViewingConditions vc) {
        double[] xyz = xyzInViewingConditions(vc);

        return MdColorUtils.argbFromXyz(xyz[0], xyz[1], xyz[2]);
    }

    double[] xyzInViewingConditions(ViewingConditions vc) {
        double alpha = (getChroma() == 0.0 || getJ() == 0.0) ? 0.0 : getChroma() / Math.sqrt(getJ() / 100.0);

        double t = Math.pow(alpha / Math.pow(1.64 - Math.pow(0.29, vc.getN()), 0.73), 1.0 / 0.9);
        double hRad = getHue() * Math.PI / 180.0;

        double eHue = 0.25 * (Math.cos(hRad + 2.0) + 3.8);
        double ac = vc.getAw() * Math.pow(getJ() / 100.0, 1.0 / vc.getC() / vc.getZ());
        double p1 = eHue * (50000.0 / 13.0) * vc.getNc() * vc.getNcb();
        double p2 = ac / vc.getNbb();

        double hSin = Math.sin(hRad);
        double hCos = Math.cos(hRad);

        double gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin);
        double a = gamma * hCos;
        double b = gamma * hSin;

        double rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0;
        double gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0;
        double bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0;

        double rCBase = Math.max(0, (27.13 * Math.abs(rA)) / (400.0 - Math.abs(rA)));
        double rC = MdMathUtils.signum(rA) * (100.0 / vc.getFl()) * Math.pow(rCBase, 1.0 / 0.42);
        double gCBase = Math.max(0, (27.13 * Math.abs(gA)) / (400.0 - Math.abs(gA)));
        double gC = MdMathUtils.signum(gA) * (100.0 / vc.getFl()) * Math.pow(gCBase, 1.0 / 0.42);
        double bCBase = Math.max(0, (27.13 * Math.abs(bA)) / (400.0 - Math.abs(bA)));
        double bC = MdMathUtils.signum(bA) * (100.0 / vc.getFl()) * Math.pow(bCBase, 1.0 / 0.42);

        double rF = rC / vc.getRgbD()[0];
        double gF = gC / vc.getRgbD()[1];
        double bF = bC / vc.getRgbD()[2];

        double x = 1.86206786 * rF - 1.01125463 * gF + 0.14918677 * bF;
        double y = 0.38752654 * rF + 0.62144744 * gF - 0.00897398 * bF;
        double z = -0.01584150 * rF - 0.03412294 * gF + 1.04996444 * bF;

        return new double[] { x, y, z };
    }

    public double getHue() {
        return hue;
    }

    public double getChroma() {
        return chroma;
    }

    public double getJ() {
        return j;
    }

    public double getQ() {
        return q;
    }

    public double getM() {
        return m;
    }

    public double getS() {
        return s;
    }

    public double getJstar() {
        return jstar;
    }

    public double getAstar() {
        return astar;
    }

    public double getBstar() {
        return bstar;
    }
}
