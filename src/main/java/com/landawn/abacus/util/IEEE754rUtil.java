/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * <p>
 * Provides IEEE-754r variants of NumberUtils methods.
 * </p>
 *
 * <p>
 * See: <a href="http://en.wikipedia.org/wiki/IEEE_754r">http://en.wikipedia.org/wiki/IEEE_754r</a>
 * </p>
 *
 * @version $Id: IEEE754rUtils.java 1436768 2013-01-22 07:07:42Z ggregory $
 */
public final class IEEE754rUtil {

    private IEEE754rUtil() {
        // singleton.
    }

    /**
     * <p>
     * Gets the minimum of two {@code float} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @return
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
     * <p>
     * Gets the minimum of three {@code float} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @param c
     *            value 3
     * @return
     */
    public static float min(final float a, final float b, final float c) {
        return min(min(a, b), c);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param array an array, must not be {@code null} or empty
     * @return
     * @throws IllegalArgumentException             if {@code array} is empty
     */
    @SafeVarargs
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
     * <p>
     * Gets the minimum of two {@code double} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @return
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
     * <p>
     * Gets the minimum of three {@code double} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @param c
     *            value 3
     * @return
     */
    public static double min(final double a, final double b, final double c) {
        return min(min(a, b), c);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param array an array, must not be {@code null} or empty
     * @return
     * @throws IllegalArgumentException             if {@code array} is empty
     */
    @SafeVarargs
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
     * <p>
     * Gets the maximum of two {@code float} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @return
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
     * <p>
     * Gets the maximum of three {@code float} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @param c
     *            value 3
     * @return
     */
    public static float max(final float a, final float b, final float c) {
        return max(max(a, b), c);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param array an array, must not be {@code null} or empty
     * @return
     * @throws IllegalArgumentException             if {@code array} is empty
     */
    @SafeVarargs
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
     * <p>
     * Gets the maximum of two {@code double} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @return
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
     * <p>
     * Gets the maximum of three {@code double} values.
     * </p>
     *
     * <p>
     * NaN is only returned if all numbers are NaN as per IEEE-754r.
     * </p>
     *
     * @param a
     *            value 1
     * @param b
     *            value 2
     * @param c
     *            value 3
     * @return
     */
    public static double max(final double a, final double b, final double c) {
        return max(max(a, b), c);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param array an array, must not be {@code null} or empty
     * @return
     * @throws IllegalArgumentException             if {@code array} is empty
     */
    @SafeVarargs
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
