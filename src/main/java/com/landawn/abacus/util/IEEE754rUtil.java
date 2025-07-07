/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * Utility class providing IEEE-754r compliant operations for floating-point numbers.
 * 
 * <p>This class provides methods for finding minimum and maximum values among floating-point
 * numbers following the IEEE-754r standard. The key difference from standard Math.min/max
 * methods is the special handling of NaN (Not-a-Number) values.</p>
 * 
 * <p>According to IEEE-754r:</p>
 * <ul>
 *   <li>When comparing a NaN with a non-NaN value, the non-NaN value is returned</li>
 *   <li>Only when all values are NaN is NaN returned</li>
 * </ul>
 * 
 * <p>This behavior differs from {@link Math#min} and {@link Math#max}, which return NaN
 * if any argument is NaN.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Standard Math.min returns NaN if any argument is NaN
 * double result1 = Math.min(5.0, Double.NaN); // returns NaN
 * 
 * // IEEE754rUtil.min returns the non-NaN value
 * double result2 = IEEE754rUtil.min(5.0, Double.NaN); // returns 5.0
 * 
 * // Array operations
 * float[] values = {3.0f, Float.NaN, 1.0f, 2.0f};
 * float min = IEEE754rUtil.min(values); // returns 1.0f
 * }</pre>
 * 
 * <p>Note: This class is copied from Apache Commons Lang and may have been modified.</p>
 * 
 * @see Math#min(double, double)
 * @see Math#max(double, double)
 * @see <a href="http://en.wikipedia.org/wiki/IEEE_754r">IEEE 754r Wikipedia Article</a>
 */
public final class IEEE754rUtil {

    private IEEE754rUtil() {
        // singleton.
    }

    /**
     * Returns the smaller of two float values according to IEEE-754r standard.
     * 
     * <p>If one value is NaN and the other is not, the non-NaN value is returned.
     * If both values are NaN, NaN is returned. Otherwise, behaves like {@link Math#min}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.min(3.0f, 5.0f);        // returns 3.0f
     * IEEE754rUtil.min(Float.NaN, 5.0f);   // returns 5.0f
     * IEEE754rUtil.min(3.0f, Float.NaN);   // returns 3.0f
     * IEEE754rUtil.min(Float.NaN, Float.NaN); // returns Float.NaN
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @return the smaller of {@code a} and {@code b} according to IEEE-754r
     */
    public static float min(final float a, final float b) {
        if (Float.isNaN(a)) {
            return b;
        } else if (Float.isNaN(b)) {
            return a;
        } else {
            return Math.min(a, b);
        }
    }

    /**
     * Returns the smallest of three float values according to IEEE-754r standard.
     * 
     * <p>NaN values are ignored unless all three values are NaN, in which case NaN is returned.
     * This method is equivalent to calling {@code min(min(a, b), c)}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.min(3.0f, 5.0f, 1.0f);  // returns 1.0f
     * IEEE754rUtil.min(Float.NaN, 5.0f, 1.0f); // returns 1.0f
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @return the smallest of {@code a}, {@code b}, and {@code c} according to IEEE-754r
     */
    public static float min(final float a, final float b, final float c) {
        return min(min(a, b), c);
    }

    /**
     * Returns the minimum value in a float array according to IEEE-754r standard.
     * 
     * <p>NaN values in the array are ignored unless all values are NaN.
     * The array must not be null or empty.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * float[] values = {3.0f, Float.NaN, 1.0f, 5.0f};
     * float min = IEEE754rUtil.min(values); // returns 1.0f
     * }</pre>
     *
     * @param array the array of values, must not be null or empty
     * @return the minimum value in the array according to IEEE-754r
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static float min(final float... array) {
        // Validates input
        if (N.isEmpty(array)) {
            throw new IllegalArgumentException("Array cannot be null or empty."); //NOSONAR
        }

        // Finds and returns min
        float min = array[0];
        for (int i = 1; i < array.length; i++) {
            min = min(array[i], min);
        }

        return min;
    }

    /**
     * Returns the smaller of two double values according to IEEE-754r standard.
     * 
     * <p>If one value is NaN and the other is not, the non-NaN value is returned.
     * If both values are NaN, NaN is returned. Otherwise, behaves like {@link Math#min}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.min(3.0, 5.0);          // returns 3.0
     * IEEE754rUtil.min(Double.NaN, 5.0);   // returns 5.0
     * IEEE754rUtil.min(3.0, Double.NaN);   // returns 3.0
     * IEEE754rUtil.min(Double.NaN, Double.NaN); // returns Double.NaN
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @return the smaller of {@code a} and {@code b} according to IEEE-754r
     */
    public static double min(final double a, final double b) {
        if (Double.isNaN(a)) {
            return b;
        } else if (Double.isNaN(b)) {
            return a;
        } else {
            return Math.min(a, b);
        }
    }

    /**
     * Returns the smallest of three double values according to IEEE-754r standard.
     * 
     * <p>NaN values are ignored unless all three values are NaN, in which case NaN is returned.
     * This method is equivalent to calling {@code min(min(a, b), c)}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.min(3.0, 5.0, 1.0);  // returns 1.0
     * IEEE754rUtil.min(Double.NaN, 5.0, 1.0); // returns 1.0
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @return the smallest of {@code a}, {@code b}, and {@code c} according to IEEE-754r
     */
    public static double min(final double a, final double b, final double c) {
        return min(min(a, b), c);
    }

    /**
     * Returns the minimum value in a double array according to IEEE-754r standard.
     * 
     * <p>NaN values in the array are ignored unless all values are NaN.
     * The array must not be null or empty.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * double[] values = {3.0, Double.NaN, 1.0, 5.0};
     * double min = IEEE754rUtil.min(values); // returns 1.0
     * }</pre>
     *
     * @param array the array of values, must not be null or empty
     * @return the minimum value in the array according to IEEE-754r
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static double min(final double... array) {
        // Validates input
        if (N.isEmpty(array)) {
            throw new IllegalArgumentException("Array cannot be null or empty.");
        }

        // Finds and returns min
        double min = array[0];
        for (int i = 1; i < array.length; i++) {
            min = min(array[i], min);
        }

        return min;
    }

    /**
     * Returns the larger of two float values according to IEEE-754r standard.
     * 
     * <p>If one value is NaN and the other is not, the non-NaN value is returned.
     * If both values are NaN, NaN is returned. Otherwise, behaves like {@link Math#max}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.max(3.0f, 5.0f);        // returns 5.0f
     * IEEE754rUtil.max(Float.NaN, 5.0f);   // returns 5.0f
     * IEEE754rUtil.max(3.0f, Float.NaN);   // returns 3.0f
     * IEEE754rUtil.max(Float.NaN, Float.NaN); // returns Float.NaN
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @return the larger of {@code a} and {@code b} according to IEEE-754r
     */
    public static float max(final float a, final float b) {
        if (Float.isNaN(a)) {
            return b;
        } else if (Float.isNaN(b)) {
            return a;
        } else {
            return Math.max(a, b);
        }
    }

    /**
     * Returns the largest of three float values according to IEEE-754r standard.
     * 
     * <p>NaN values are ignored unless all three values are NaN, in which case NaN is returned.
     * This method is equivalent to calling {@code max(max(a, b), c)}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.max(3.0f, 5.0f, 1.0f);  // returns 5.0f
     * IEEE754rUtil.max(Float.NaN, 5.0f, 1.0f); // returns 5.0f
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @return the largest of {@code a}, {@code b}, and {@code c} according to IEEE-754r
     */
    public static float max(final float a, final float b, final float c) {
        return max(max(a, b), c);
    }

    /**
     * Returns the maximum value in a float array according to IEEE-754r standard.
     * 
     * <p>NaN values in the array are ignored unless all values are NaN.
     * The array must not be null or empty.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * float[] values = {3.0f, Float.NaN, 1.0f, 5.0f};
     * float max = IEEE754rUtil.max(values); // returns 5.0f
     * }</pre>
     *
     * @param array the array of values, must not be null or empty
     * @return the maximum value in the array according to IEEE-754r
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static float max(final float... array) {
        // Validates input
        if (N.isEmpty(array)) {
            throw new IllegalArgumentException("Array cannot be null or empty.");
        }

        // Finds and returns max
        float max = array[0];
        for (int j = 1; j < array.length; j++) {
            max = max(array[j], max);
        }

        return max;
    }

    /**
     * Returns the larger of two double values according to IEEE-754r standard.
     * 
     * <p>If one value is NaN and the other is not, the non-NaN value is returned.
     * If both values are NaN, NaN is returned. Otherwise, behaves like {@link Math#max}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.max(3.0, 5.0);          // returns 5.0
     * IEEE754rUtil.max(Double.NaN, 5.0);   // returns 5.0
     * IEEE754rUtil.max(3.0, Double.NaN);   // returns 3.0
     * IEEE754rUtil.max(Double.NaN, Double.NaN); // returns Double.NaN
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @return the larger of {@code a} and {@code b} according to IEEE-754r
     */
    public static double max(final double a, final double b) {
        if (Double.isNaN(a)) {
            return b;
        } else if (Double.isNaN(b)) {
            return a;
        } else {
            return Math.max(a, b);
        }
    }

    /**
     * Returns the largest of three double values according to IEEE-754r standard.
     * 
     * <p>NaN values are ignored unless all three values are NaN, in which case NaN is returned.
     * This method is equivalent to calling {@code max(max(a, b), c)}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IEEE754rUtil.max(3.0, 5.0, 1.0);  // returns 5.0
     * IEEE754rUtil.max(Double.NaN, 5.0, 1.0); // returns 5.0
     * }</pre>
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @return the largest of {@code a}, {@code b}, and {@code c} according to IEEE-754r
     */
    public static double max(final double a, final double b, final double c) {
        return max(max(a, b), c);
    }

    /**
     * Returns the maximum value in a double array according to IEEE-754r standard.
     * 
     * <p>NaN values in the array are ignored unless all values are NaN.
     * The array must not be null or empty.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * double[] values = {3.0, Double.NaN, 1.0, 5.0};
     * double max = IEEE754rUtil.max(values); // returns 5.0
     * }</pre>
     *
     * @param array the array of values, must not be null or empty
     * @return the maximum value in the array according to IEEE-754r
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static double max(final double... array) {
        // Validates input
        if (N.isEmpty(array)) {
            throw new IllegalArgumentException("Array cannot be null or empty.");
        }

        // Finds and returns max
        double max = array[0];
        for (int j = 1; j < array.length; j++) {
            max = max(array[j], max);
        }

        return max;
    }
}