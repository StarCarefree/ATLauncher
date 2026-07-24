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
 * Small numeric helpers shared by the CAM16/HCT colour code.
 *
 * <p>
 * Kept separate from {@link com.atlauncher.utils.Utils} so the colour engine has no dependencies on
 * the rest of the launcher and can be unit tested in isolation.
 */
public final class MdMathUtils {
    private MdMathUtils() {
    }

    /**
     * The signum function, returning -1, 0 or 1 as a double so it can be used directly in the CAM16
     * formulas.
     */
    public static double signum(double num) {
        if (num < 0) {
            return -1;
        } else if (num == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    public static double lerp(double start, double stop, double amount) {
        return (1.0 - amount) * start + amount * stop;
    }

    public static int clampInt(int min, int max, int input) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    public static double clampDouble(double min, double max, double input) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    /**
     * Wraps an angle into the [0, 360) range.
     */
    public static double sanitizeDegreesDouble(double degrees) {
        degrees = degrees % 360.0;

        if (degrees < 0) {
            degrees = degrees + 360.0;
        }

        return degrees;
    }

    /**
     * Multiplies a 1x3 row vector with a 3x3 matrix.
     */
    public static double[] matrixMultiply(double[] row, double[][] matrix) {
        double a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2];
        double b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2];
        double c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2];

        return new double[] { a, b, c };
    }
}
