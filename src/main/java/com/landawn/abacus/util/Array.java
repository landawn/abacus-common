/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.NullSafe;

/**
 *
 * @author Haiyang Li
 * @see java.lang.reflect.Array
 * @since 0.8
 *
 * @see com.landawn.abacus.util.N
 */
public class Array {
    static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    static final Executor parallelSortExecutor = new ThreadPoolExecutor(Math.min(8, CPU_CORES), CPU_CORES, 180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    static final int MIN_ARRAY_SORT_GRAN = 8192;

    static final int BINARYSEARCH_THRESHOLD = 64;

    private Array() {
        // Utility class.
    }

    /**
     *
     * @param <T>
     * @param componentType
     * @param length
     * @return
     * @throws NegativeArraySizeException the negative array size exception
     */
    public static <T> T newInstance(final Class<?> componentType, final int length) throws NegativeArraySizeException {
        if (length == 0) {
            Object result = N.CLASS_EMPTY_ARRAY.get(componentType);

            if (result == null) {
                result = java.lang.reflect.Array.newInstance(componentType, length);
                N.CLASS_EMPTY_ARRAY.put(componentType, result);
            }

            return (T) result;
        }

        return (T) java.lang.reflect.Array.newInstance(componentType, length);
    }

    /**
     *
     * @param <T>
     * @param componentType
     * @param dimensions
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws NegativeArraySizeException the negative array size exception
     */
    @SafeVarargs
    public static <T> T newInstance(final Class<?> componentType, final int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        return (T) java.lang.reflect.Array.newInstance(componentType, dimensions);
    }

    /**
     * Gets the length.
     *
     * @param array
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static int getLength(final Object array) throws IllegalArgumentException {
        return java.lang.reflect.Array.getLength(array);
    }

    /**
     *
     * @param <T>
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static <T> T get(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return (T) java.lang.reflect.Array.get(array, index);
    }

    /**
     * Gets the boolean.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static boolean getBoolean(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getBoolean(array, index);
    }

    /**
     * Gets the byte.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static byte getByte(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getByte(array, index);
    }

    /**
     * Gets the char.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static char getChar(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getChar(array, index);
    }

    /**
     * Gets the short.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static short getShort(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getShort(array, index);
    }

    /**
     * Gets the int.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static int getInt(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getInt(array, index);
    }

    /**
     * Gets the long.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static long getLong(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getLong(array, index);
    }

    /**
     * Gets the float.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static float getFloat(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getFloat(array, index);
    }

    /**
     * Gets the double.
     *
     * @param array
     * @param index
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static double getDouble(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getDouble(array, index);
    }

    /**
     *
     * @param array
     * @param index
     * @param value
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void set(final Object array, final int index, final Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.set(array, index, value);
    }

    /**
     * Sets the boolean.
     *
     * @param array
     * @param index
     * @param z
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setBoolean(final Object array, final int index, final boolean z) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setBoolean(array, index, z);
    }

    /**
     * Sets the byte.
     *
     * @param array
     * @param index
     * @param b
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setByte(final Object array, final int index, final byte b) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setByte(array, index, b);
    }

    /**
     * Sets the char.
     *
     * @param array
     * @param index
     * @param c
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setChar(final Object array, final int index, final char c) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setChar(array, index, c);
    }

    /**
     * Sets the short.
     *
     * @param array
     * @param index
     * @param s
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setShort(final Object array, final int index, final short s) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setShort(array, index, s);
    }

    /**
     * Sets the int.
     *
     * @param array
     * @param index
     * @param i
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setInt(final Object array, final int index, final int i) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setInt(array, index, i);
    }

    /**
     * Sets the long.
     *
     * @param array
     * @param index
     * @param l
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setLong(final Object array, final int index, final long l) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setLong(array, index, l);
    }

    /**
     * Sets the float.
     *
     * @param array
     * @param index
     * @param f
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setFloat(final Object array, final int index, final float f) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setFloat(array, index, f);
    }

    /**
     * Sets the double.
     *
     * @param array
     * @param index
     * @param d
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
     */
    public static void setDouble(final Object array, final int index, final double d) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setDouble(array, index, d);
    }

    /**
     * Returns a fixed-size list backed by the specified array if it's not null or empty, otherwise an immutable empty list is returned.
     *
     * @param <T>
     * @param a
     * @return
     * @see Arrays#asList(Object...)
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(T... a) {
        return N.isNullOrEmpty(a) ? N.<T> emptyList() : Arrays.asList(a);
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static boolean[] of(final boolean... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static char[] of(final char... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static byte[] of(final byte... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static short[] of(final short... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static int[] of(final int... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static long[] of(final long... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static float[] of(final float... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static double[] of(final double... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static String[] of(final String... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> 
     * @param a 
     * @return 
     */
    @SafeVarargs
    public static <T extends java.util.Date> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> 
     * @param a 
     * @return 
     */
    @SafeVarargs
    public static <T extends java.util.Calendar> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> 
     * @param a 
     * @return 
     */
    @SafeVarargs
    public static <T extends java.time.temporal.Temporal> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T>
     * @param a
     * @return
     * @deprecated please use {@code N.asArray(Object...)}.
     */
    @Deprecated
    @SafeVarargs
    public static <T> T[] oF(final T... a) { //NOSONAR
        return a;
    }

    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    @SafeVarargs
    //    public static BigInteger[] of(final BigInteger... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    @SafeVarargs
    //    public static BigDecimal[] of(final BigDecimal... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Enum<T>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends java.util.Date> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Calendar> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Runnable> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Callable<?>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    @SuppressWarnings("rawtypes")
    //    public static Class[] of(final Class... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends EntityId> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends DirtyMarker> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Condition> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Type<?>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends List<?>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Set<?>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Queue<?>> T[] of(final T... a) {
    //        return a;
    //    }
    //
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T extends Map<?, ?>> T[] of(final T... a) {
    //        return a;
    //    }

    //    // Only for Java 8. it's ambiguous in the Java version before 8.
    //    /**
    //     * Returns the input array
    //     *
    //     * @param a
    //     * @return
    //     */
    //    public static <T> T[] of(final T... a) {
    //        return a;
    //    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static char[] range(char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] a = new char[endExclusive * 1 - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static byte[] range(byte startInclusive, final byte endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] a = new byte[endExclusive * 1 - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static short[] range(short startInclusive, final short endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] a = new short[endExclusive * 1 - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static int[] range(int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_INT_ARRAY;
        }

        if (endExclusive * 1L - startInclusive > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow"); //NOSONAR
        }

        final int[] a = new int[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static long[] range(long startInclusive, final long endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_LONG_ARRAY;
        }

        if (endExclusive - startInclusive < 0 || endExclusive - startInclusive > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) (endExclusive - startInclusive)];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static float[] range(float startInclusive, final float endExclusive) {
    //        if (endExclusive == startInclusive) {
    //            return N.EMPTY_FLOAT_ARRAY;
    //        }
    //
    //        int tmp = (int) (endExclusive - startInclusive);
    //        final float[] a = new float[(startInclusive + tmp == endExclusive) ? tmp : tmp + 1];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = startInclusive++;
    //        }
    //
    //        return a;
    //    }
    //
    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static double[] range(double startInclusive, final double endExclusive) {
    //        if (endExclusive == startInclusive) {
    //            return N.EMPTY_DOUBLE_ARRAY;
    //        }
    //
    //        int tmp = (int) (endExclusive - startInclusive);
    //        final double[] a = new double[(startInclusive + tmp == endExclusive) ? tmp : tmp + 1];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = startInclusive++;
    //        }
    //
    //        return a;
    //    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static char[] range(char startInclusive, final char endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero"); //NOSONAR
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive * 1 - startInclusive) / by + ((endExclusive * 1 - startInclusive) % by == 0 ? 0 : 1);
        final char[] a = new char[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static byte[] range(byte startInclusive, final byte endExclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive * 1 - startInclusive) / by + ((endExclusive * 1 - startInclusive) % by == 0 ? 0 : 1);
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static short[] range(short startInclusive, final short endExclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive * 1 - startInclusive) / by + ((endExclusive * 1 - startInclusive) % by == 0 ? 0 : 1);
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static int[] range(int startInclusive, final int endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_INT_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final long len = (endExclusive * 1L - startInclusive) / by + ((endExclusive * 1L - startInclusive) % by == 0 ? 0 : 1);

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static long[] range(long startInclusive, final long endExclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        long len = 0;

        if ((by > 0 && endExclusive - startInclusive < 0) || (by < 0 && startInclusive - endExclusive < 0)) {
            final BigInteger m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(by));

            if (m.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("overflow");
            }

            len = m.multiply(BigInteger.valueOf(by)).add(BigInteger.valueOf(startInclusive)).equals(BigInteger.valueOf(endExclusive)) ? m.longValue()
                    : m.longValue() + 1;
        } else {
            len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        }

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static float[] range(float startInclusive, final float endExclusive, final float by) {
    //        if (by == 0) {
    //            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
    //        }
    //
    //        if (endExclusive == startInclusive) {
    //            return N.EMPTY_FLOAT_ARRAY;
    //        }
    //
    //        if (endExclusive > startInclusive != by > 0) {
    //            throw new IllegalArgumentException(
    //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
    //        }
    //
    //        final int tmp = (int) ((endExclusive - startInclusive) / by);
    //        final int len = startInclusive + (tmp * by) == endExclusive ? tmp : tmp + 1;
    //        final float[] a = new float[len];
    //
    //        for (int i = 0; i < len; i++, startInclusive += by) {
    //            a[i] = startInclusive;
    //        }
    //
    //        return a;
    //    }
    //
    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static double[] range(double startInclusive, final double endExclusive, final double by) {
    //        if (by == 0) {
    //            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
    //        }
    //
    //        if (endExclusive == startInclusive) {
    //            return N.EMPTY_DOUBLE_ARRAY;
    //        }
    //
    //        if (endExclusive > startInclusive != by > 0) {
    //            throw new IllegalArgumentException(
    //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
    //        }
    //
    //        final int tmp = (int) ((endExclusive - startInclusive) / by);
    //        final int len = startInclusive + (tmp * by) == endExclusive ? tmp : tmp + 1;
    //        final double[] a = new double[len];
    //
    //        for (int i = 0; i < len; i++, startInclusive += by) {
    //            a[i] = startInclusive;
    //        }
    //
    //        return a;
    //    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static char[] rangeClosed(char startInclusive, final char endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final char[] a = new char[endInclusive * 1 - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static byte[] rangeClosed(byte startInclusive, final byte endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final byte[] a = new byte[endInclusive * 1 - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static short[] rangeClosed(short startInclusive, final short endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final short[] a = new short[endInclusive * 1 - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static int[] rangeClosed(int startInclusive, final int endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_INT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        if (endInclusive * 1L - startInclusive + 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static long[] rangeClosed(long startInclusive, final long endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_LONG_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        if (endInclusive - startInclusive + 1 <= 0 || endInclusive - startInclusive + 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) (endInclusive - startInclusive + 1)];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static float[] rangeClosed(float startInclusive, final float endInclusive) {
    //        final float[] a = new float[(int) (endInclusive - startInclusive) + 1];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = startInclusive++;
    //        }
    //
    //        return a;
    //    }
    //
    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static double[] rangeClosed(double startInclusive, final double endInclusive) {
    //        final double[] a = new double[(int) (endInclusive - startInclusive) + 1];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = startInclusive++;
    //        }
    //
    //        return a;
    //    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static char[] rangeClosed(char startInclusive, final char endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new char[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive * 1 - startInclusive) / by + 1;
        final char[] a = new char[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static byte[] rangeClosed(byte startInclusive, final byte endInclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new byte[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive * 1 - startInclusive) / by + 1;
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static short[] rangeClosed(short startInclusive, final short endInclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new short[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive * 1 - startInclusive) / by + 1;
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static int[] rangeClosed(int startInclusive, final int endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new int[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_INT_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final long len = (endInclusive * 1L - startInclusive) / by + 1;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static long[] rangeClosed(long startInclusive, final long endInclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new long[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        long len = 0;

        if ((by > 0 && endInclusive - startInclusive < 0) || (by < 0 && startInclusive - endInclusive < 0) || ((endInclusive - startInclusive) / by + 1 <= 0)) {
            final BigInteger m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(by));

            if (m.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("overflow");
            }

            len = m.longValue() + 1;
        } else {
            len = (endInclusive - startInclusive) / by + 1;
        }

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static float[] rangeClosed(float startInclusive, final float endExclusive, final float by) {
    //        if (by == 0) {
    //            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
    //        }
    //
    //        if (endExclusive == startInclusive) {
    //            return new float[] { startInclusive };
    //        }
    //
    //        if (endExclusive > startInclusive != by > 0) {
    //            throw new IllegalArgumentException(
    //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
    //        }
    //
    //        final int len = (int) (((double) endExclusive - (double) startInclusive) / by) + 1;
    //        final float[] a = new float[len];
    //
    //        for (int i = 0; i < len; i++, startInclusive += by) {
    //            a[i] = startInclusive;
    //        }
    //
    //        return a;
    //    }
    //
    //    // Doesn't work as expected due to precision issue. "3.3d - 1.1d != 2.2d". Refer to: https://en.wikipedia.org/wiki/IEEE_floating_point
    //    // http://stackoverflow.com/questions/15625556/java-adding-and-subtracting-doubles-are-giving-strange-results
    //    static double[] rangeClosed(double startInclusive, final double endExclusive, final double by) {
    //        if (by == 0) {
    //            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
    //        }
    //
    //        if (endExclusive == startInclusive) {
    //            return new double[] { startInclusive };
    //        }
    //
    //        if (endExclusive > startInclusive != by > 0) {
    //            throw new IllegalArgumentException(
    //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
    //        }
    //
    //        final int len = (int) ((endExclusive - startInclusive) / by) + 1;
    //        final double[] a = new double[len];
    //
    //        for (int i = 0; i < len; i++, startInclusive += by) {
    //            a[i] = startInclusive;
    //        }
    //
    //        return a;
    //    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static boolean[] repeat(final boolean element, final int n) {
        final boolean[] a = new boolean[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static char[] repeat(final char element, final int n) {
        final char[] a = new char[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static byte[] repeat(final byte element, final int n) {
        final byte[] a = new byte[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static short[] repeat(final short element, final int n) {
        final short[] a = new short[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static int[] repeat(final int element, final int n) {
        final int[] a = new int[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static long[] repeat(final long element, final int n) {
        final long[] a = new long[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static float[] repeat(final float element, final int n) {
        final float[] a = new float[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param element
     * @param n
     * @return
     */
    public static double[] repeat(final double element, final int n) {
        final double[] a = new double[n];
        N.fill(a, element);
        return a;
    }

    /**
     * 
     *
     * @param element 
     * @param n 
     * @return 
     */
    public static String[] repeat(final String element, final int n) {
        final String[] a = new String[n];
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param <T>
     * @param element
     * @param n
     * @return
     * @throws NullPointerException if the specified {@code element} is null.
     * @see Array#repeat(Object, int, Class)
     * @deprecated prefer to {@link Array#repeat(Object, int, Class)} because this method throws NullPointerException when element is {@code null}
     */
    @Deprecated
    public static <T> T[] repeat(final T element, final int n) throws NullPointerException {
        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param elementClass
    //     * @param element
    //     * @param n
    //     * @return
    //     * @throws NullPointerException
    //     * @deprecated {@link #repeat(Object, int, Class)}
    //     */
    //    @Deprecated
    //    public static <T> T[] repeat(final Class<? extends T> elementClass, final T element, final int n) throws NullPointerException {
    //        final T[] a = N.newArray(elementClass, n);
    //        N.fill(a, element);
    //        return a;
    //    }

    /**
     *
     * @param <T>
     * @param element
     * @param n
     * @param elementClass
     * @return
     * @throws NullPointerException
     */
    public static <T> T[] repeat(final T element, final int n, final Class<? extends T> elementClass) throws NullPointerException {
        final T[] a = N.newArray(elementClass, n);
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param <T>
     * @param element
     * @param n
     * @return
     * @throws IllegalArgumentException if the specified {@code element} is null.
     */
    public static <T> T[] repeatNonNull(final T element, final int n) throws IllegalArgumentException {
        N.checkArgNotNull(element, "element");

        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean[][] concat(final boolean[][] a, final boolean[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new boolean[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final boolean[][] result = new boolean[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean[][][] concat(final boolean[][][] a, final boolean[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new boolean[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final boolean[][][] result = new boolean[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static char[][] concat(final char[][] a, final char[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new char[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final char[][] result = new char[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static char[][][] concat(final char[][][] a, final char[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new char[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final char[][][] result = new char[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[][] concat(final byte[][] a, final byte[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new byte[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final byte[][] result = new byte[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[][][] concat(final byte[][][] a, final byte[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new byte[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final byte[][][] result = new byte[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static short[][] concat(final short[][] a, final short[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new short[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final short[][] result = new short[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static short[][][] concat(final short[][][] a, final short[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new short[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final short[][][] result = new short[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int[][] concat(final int[][] a, final int[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new int[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final int[][] result = new int[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int[][][] concat(final int[][][] a, final int[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new int[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final int[][][] result = new int[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static long[][] concat(final long[][] a, final long[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new long[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final long[][] result = new long[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static long[][][] concat(final long[][][] a, final long[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new long[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final long[][][] result = new long[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static float[][] concat(final float[][] a, final float[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new float[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final float[][] result = new float[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static float[][][] concat(final float[][][] a, final float[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new float[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final float[][][] result = new float[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static double[][] concat(final double[][] a, final double[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new double[0][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final double[][] result = new double[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static double[][][] concat(final double[][][] a, final double[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new double[0][][] : N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final double[][][] result = new double[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> T[][] concatt(final T[][] a, final T[][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final T[][] result = newInstance(a.getClass().getComponentType(), maxLen);

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> T[][][] concatt(final T[][][] a, final T[][][] b) {
        if (N.isNullOrEmpty(a)) {
            return N.clone(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final T[][][] result = newInstance(a.getClass().getComponentType(), maxLen);

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concatt(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive booleans to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code boolean} array
     * @return a {@code Boolean} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Boolean[] box(final boolean... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Boolean[] box(final boolean[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_OBJECT_ARRAY;
        }

        final Boolean[] result = new Boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive chars to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code char} array
     * @return a {@code Character} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Character[] box(final char... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Character[] box(final char[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHARACTER_OBJECT_ARRAY;
        }

        final Character[] result = new Character[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive bytes to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code byte} array
     * @return a {@code Byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Byte[] box(final byte... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Byte[] box(final byte[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_OBJECT_ARRAY;
        }

        final Byte[] result = new Byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive shorts to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code short} array
     * @return a {@code Short} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Short[] box(final short... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Short[] box(final short[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_OBJECT_ARRAY;
        }

        final Short[] result = new Short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive ints to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            an {@code int} array
     * @return an {@code Integer} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Integer[] box(final int... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Integer[] box(final int[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INTEGER_OBJECT_ARRAY;
        }

        final Integer[] result = new Integer[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive longs to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code long} array
     * @return a {@code Long} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Long[] box(final long... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Long[] box(final long[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_OBJECT_ARRAY;
        }

        final Long[] result = new Long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive floats to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code float} array
     * @return a {@code Float} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Float[] box(final float... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Float[] box(final float[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_OBJECT_ARRAY;
        }

        final Float[] result = new Float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive doubles to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code double} array
     * @return a {@code Double} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Double[] box(final double... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Double[] box(final double[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_OBJECT_ARRAY;
        }

        final Double[] result = new Double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Boolean[][] box(boolean[][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][] result = new Boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Character[][] box(char[][] a) {
        if (a == null) {
            return null;
        }

        final Character[][] result = new Character[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Byte[][] box(byte[][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][] result = new Byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Short[][] box(short[][] a) {
        if (a == null) {
            return null;
        }

        final Short[][] result = new Short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Integer[][] box(int[][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][] result = new Integer[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Long[][] box(long[][] a) {
        if (a == null) {
            return null;
        }

        final Long[][] result = new Long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Float[][] box(float[][] a) {
        if (a == null) {
            return null;
        }

        final Float[][] result = new Float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Double[][] box(double[][] a) {
        if (a == null) {
            return null;
        }

        final Double[][] result = new Double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Boolean[][][] box(boolean[][][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][][] result = new Boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Character[][][] box(char[][][] a) {
        if (a == null) {
            return null;
        }

        final Character[][][] result = new Character[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Byte[][][] box(byte[][][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][][] result = new Byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Short[][][] box(short[][][] a) {
        if (a == null) {
            return null;
        }

        final Short[][][] result = new Short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Integer[][][] box(int[][][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][][] result = new Integer[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Long[][][] box(long[][][] a) {
        if (a == null) {
            return null;
        }

        final Long[][][] result = new Long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Float[][][] box(float[][][] a) {
        if (a == null) {
            return null;
        }

        final Float[][][] result = new Float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Double[][][] box(double[][][] a) {
        if (a == null) {
            return null;
        }

        final Double[][][] result = new Double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    // Boolean array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Booleans to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Boolean} array, may be {@code null}
     * @return a {@code boolean} array, {@code null} if null array input
     */
    @SafeVarargs
    public static boolean[] unbox(final Boolean... a) {
        return unbox(a, false);
    }

    /**
     * <p>
     * Converts an array of object Booleans to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Boolean} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code boolean} array, {@code null} if null array input
     */
    public static boolean[] unbox(final Boolean[] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static boolean[] unbox(final Boolean[] a, final int fromIndex, final int toIndex, final boolean valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] result = new boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Character array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Characters to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Character} array, may be {@code null}
     * @return a {@code char} array, {@code null} if null array input
     */
    @SafeVarargs
    public static char[] unbox(final Character... a) {
        return unbox(a, (char) 0);
    }

    /**
     * <p>
     * Converts an array of object Character to primitives handling {@code null}
     * .
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Character} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code char} array, {@code null} if null array input
     */
    public static char[] unbox(final Character[] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static char[] unbox(final Character[] a, final int fromIndex, final int toIndex, final char valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] result = new char[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Byte array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Bytes to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Byte} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static byte[] unbox(final Byte... a) {
        return unbox(a, (byte) 0);
    }

    /**
     * <p>
     * Converts an array of object Bytes to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Byte} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static byte[] unbox(final Byte[] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static byte[] unbox(final Byte[] a, final int fromIndex, final int toIndex, final byte valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] result = new byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Short array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Shorts to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Short} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static short[] unbox(final Short... a) {
        return unbox(a, (short) 0);
    }

    /**
     * <p>
     * Converts an array of object Short to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Short} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static short[] unbox(final Short[] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static short[] unbox(final Short[] a, final int fromIndex, final int toIndex, final short valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] result = new short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Int array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Integers to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Integer} array, may be {@code null}
     * @return an {@code int} array, {@code null} if null array input
     */
    @SafeVarargs
    public static int[] unbox(final Integer... a) {
        return unbox(a, 0);
    }

    /**
     * <p>
     * Converts an array of object Integer to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Integer} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return an {@code int} array, {@code null} if null array input
     */
    public static int[] unbox(final Integer[] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static int[] unbox(final Integer[] a, final int fromIndex, final int toIndex, final int valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] result = new int[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Long array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Longs to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Long} array, may be {@code null}
     * @return a {@code long} array, {@code null} if null array input
     */
    @SafeVarargs
    public static long[] unbox(final Long... a) {
        return unbox(a, 0L);
    }

    /**
     * <p>
     * Converts an array of object Long to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Long} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code long} array, {@code null} if null array input
     */
    public static long[] unbox(final Long[] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static long[] unbox(final Long[] a, final int fromIndex, final int toIndex, final long valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long[] result = new long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Float array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Floats to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Float} array, may be {@code null}
     * @return a {@code float} array, {@code null} if null array input
     */
    @SafeVarargs
    public static float[] unbox(final Float... a) {
        return unbox(a, 0f);
    }

    /**
     * <p>
     * Converts an array of object Floats to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Float} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code float} array, {@code null} if null array input
     */
    public static float[] unbox(final Float[] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static float[] unbox(final Float[] a, final int fromIndex, final int toIndex, final float valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final float[] result = new float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    // Double array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Doubles to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Double} array, may be {@code null}
     * @return a {@code double} array, {@code null} if null array input
     */
    @SafeVarargs
    public static double[] unbox(final Double... a) {
        return unbox(a, 0d);
    }

    /**
     * <p>
     * Converts an array of object Doubles to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Double} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code double} array, {@code null} if null array input
     */
    public static double[] unbox(final Double[] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueForNull
     * @return
     */
    public static double[] unbox(final Double[] a, final int fromIndex, final int toIndex, final double valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final double[] result = new double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static boolean[][] unbox(Boolean[][] a) {
        return unbox(a, false);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static boolean[][] unbox(Boolean[][] a, boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][] result = new boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static char[][] unbox(Character[][] a) {
        return unbox(a, (char) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static char[][] unbox(Character[][] a, char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][] result = new char[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static byte[][] unbox(Byte[][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static byte[][] unbox(Byte[][] a, byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][] result = new byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static short[][] unbox(Short[][] a) {
        return unbox(a, (short) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static short[][] unbox(Short[][] a, short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][] result = new short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static int[][] unbox(Integer[][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static int[][] unbox(Integer[][] a, int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][] result = new int[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static long[][] unbox(Long[][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static long[][] unbox(Long[][] a, long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][] result = new long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static float[][] unbox(Float[][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static float[][] unbox(Float[][] a, float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][] result = new float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static double[][] unbox(Double[][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static double[][] unbox(Double[][] a, double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][] result = new double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static boolean[][][] unbox(Boolean[][][] a) {
        return unbox(a, false);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static boolean[][][] unbox(Boolean[][][] a, boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][][] result = new boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static char[][][] unbox(Character[][][] a) {
        return unbox(a, (char) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static char[][][] unbox(Character[][][] a, char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][][] result = new char[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static byte[][][] unbox(Byte[][][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static byte[][][] unbox(Byte[][][] a, byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][][] result = new byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static short[][][] unbox(Short[][][] a) {
        return unbox(a, (short) 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static short[][][] unbox(Short[][][] a, short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][][] result = new short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static int[][][] unbox(Integer[][][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static int[][][] unbox(Integer[][][] a, int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][][] result = new int[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static long[][][] unbox(Long[][][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static long[][][] unbox(Long[][][] a, long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][][] result = new long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static float[][][] unbox(Float[][][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static float[][][] unbox(Float[][][] a, float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][][] result = new float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static double[][][] unbox(Double[][][] a) {
        return unbox(a, 0);
    }

    /**
     *
     * @param a
     * @param valueForNull
     * @return
     */
    public static double[][][] unbox(Double[][][] a, double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][][] result = new double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    //    // TODO It seems there is some bug in  Arrays.parallelSort(a, fromIndex, toIndex). see below test:
    //
    //    @Test
    //    public void test_parallelSort_double() throws Exception {
    //        assertFalse(1d > Double.NaN);
    //        assertFalse(1d < Double.NaN);
    //        assertFalse(1d == Double.NaN);
    //        assertFalse(1d <= Double.NaN);
    //
    //        final Random rand = new Random();
    //        final int maxSize = 10000;
    //
    //        for (int c = 1; c < 17; c++) {
    //            changeCPUCoreNum(c);
    //
    //            for (int i = 0; i < 13; i++) {
    //                double[] a = new double[rand.nextInt(maxSize)];
    //
    //                for (int k = 0, len = a.length; k < len; k++) {
    //                    a[k] = k % 3 == 0 ? Double.NaN : rand.nextFloat();
    //                }
    //
    //                double[] b = a.clone();
    //                // N.println(a);
    //                N.parallelSort(a);
    //                // N.println(a);
    //                Arrays.sort(b);
    //                // N.println(b);
    //
    //                for (int k = 0, len = a.length; k < len; k++) {
    //                    assertEquals(b[k], a[k]);
    //                }
    //            }
    //        }
    //    }
    //
    //    private void changeCPUCoreNum(final int c) {
    //        Array.CPU_CORES = c;
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final char[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final char[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final char[] a, int fromIndexA, int toIndexA, final char[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (a[fromIndexA] <= b[fromIndexB]) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final byte[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final byte[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final byte[] a, int fromIndexA, int toIndexA, final byte[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (a[fromIndexA] <= b[fromIndexB]) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final short[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final short[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final short[] a, int fromIndexA, int toIndexA, final short[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (a[fromIndexA] <= b[fromIndexB]) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final int[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final int[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final int[] a, int fromIndexA, int toIndexA, final int[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (a[fromIndexA] <= b[fromIndexB]) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final long[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final long[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final long[] a, int fromIndexA, int toIndexA, final long[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (a[fromIndexA] <= b[fromIndexB]) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final float[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final float[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final float[] a, int fromIndexA, int toIndexA, final float[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        int numOfNaN = 0;
    //
    //        for (int i = toIndexA - 1; i >= fromIndexA && Float.isNaN(a[i]); i--) {
    //            toIndexA--;
    //            numOfNaN++;
    //        }
    //
    //        for (int i = toIndexB - 1; i >= fromIndexB && Float.isNaN(b[i]); i--) {
    //            toIndexB--;
    //            numOfNaN++;
    //        }
    //
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (Float.compare(a[fromIndexA], b[fromIndexB]) <= 0) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        } else if (fromIndexB < toIndexB && numOfNaN > 0) {
    //            N.copy(b, fromIndexB, b, fromIndex, toIndexB - fromIndexB);
    //            fromIndex += toIndexB - fromIndexB;
    //        }
    //
    //        if (numOfNaN > 0) {
    //            N.fill(b, fromIndex, fromIndex + numOfNaN, Float.NaN);
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param array
    //    */
    //    static void parallelSort(final double[] array) {
    //        if (N.isNullOrEmpty(array)) {
    //            return;
    //        }
    //
    //        parallelSort(array, 0, array.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final double[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    */
    //    static void merge(final double[] a, int fromIndexA, int toIndexA, final double[] b, int fromIndexB, int toIndexB, int fromIndex) {
    //        int numOfNaN = 0;
    //
    //        for (int i = toIndexA - 1; i >= fromIndexA && Double.isNaN(a[i]); i--) {
    //            toIndexA--;
    //            numOfNaN++;
    //        }
    //
    //        for (int i = toIndexB - 1; i >= fromIndexB && Double.isNaN(b[i]); i--) {
    //            toIndexB--;
    //            numOfNaN++;
    //        }
    //
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (Double.compare(a[fromIndexA], b[fromIndexB]) <= 0) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        } else if (fromIndexB < toIndexB && numOfNaN > 0) {
    //            N.copy(b, fromIndexB, b, fromIndex, toIndexB - fromIndexB);
    //            fromIndex += toIndexB - fromIndexB;
    //        }
    //
    //        if (numOfNaN > 0) {
    //            N.fill(b, fromIndex, fromIndex + numOfNaN, Double.NaN);
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    */
    //    static void parallelSort(final Object[] a) {
    //        if (N.isNullOrEmpty(a)) {
    //            return;
    //        }
    //
    //        parallelSort(a, 0, a.length);
    //    }
    //
    //    /**
    //    *
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static void parallelSort(final Object[] a, final int fromIndex, final int toIndex) {
    //        parallelSort(a, fromIndex, toIndex, Comparators.NATURAL_ORDER);
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param a
    //    * @param cmp
    //    */
    //    static <T> void parallelSort(final T[] a, final Comparator<? super T> cmp) {
    //        if (N.isNullOrEmpty(a)) {
    //            return;
    //        }
    //
    //        parallelSort(a, 0, a.length, cmp);
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param a
    //    * @param fromIndex
    //    * @param toIndex
    //    * @param cmp
    //    */
    //    static <T> void parallelSort(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        final Comparator<? super T> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
    //        final int len = toIndex - fromIndex;
    //
    //        if (len < MIN_ARRAY_SORT_GRAN || CPU_CORES == 1) {
    //            N.sort(a, fromIndex, toIndex, comparator);
    //            return;
    //        }
    //
    //        final Queue<Pair<Integer, Integer>> subArrayIndexQueue = new LinkedList<>();
    //        final AtomicInteger activeThreadNum = new AtomicInteger();
    //        final Holder<Throwable> errorHolder = new Holder<>();
    //        final int lenOfSubArray = len % CPU_CORES == 0 ? len / CPU_CORES : (len / CPU_CORES) + 1;
    //
    //        for (int i = 0; i < CPU_CORES; i++) {
    //            final int start = fromIndex + i * lenOfSubArray;
    //            final int end = toIndex - start < lenOfSubArray ? toIndex : start + lenOfSubArray;
    //            subArrayIndexQueue.add(Pair.of(start, end));
    //
    //            activeThreadNum.incrementAndGet();
    //
    //            parallelSortExecutor.execute(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        if (errorHolder.value() != null) {
    //                            return;
    //                        }
    //
    //                        Arrays.sort(a, start, end, comparator);
    //                    } catch (Exception e) {
    //                        setError(errorHolder, e);
    //                    } finally {
    //                        activeThreadNum.decrementAndGet();
    //                    }
    //                }
    //            });
    //        }
    //
    //        while (activeThreadNum.get() > 0) {
    //            N.sleep(1);
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //
    //        while (subArrayIndexQueue.size() > 1 && errorHolder.value() == null) {
    //            for (int i = 0, size = subArrayIndexQueue.size(); i < size;) {
    //                final Pair<Integer, Integer> pairA = subArrayIndexQueue.poll();
    //                if (++i == size) {
    //                    subArrayIndexQueue.add(pairA);
    //                } else {
    //                    i++;
    //                    final Pair<Integer, Integer> pairB = subArrayIndexQueue.poll();
    //                    subArrayIndexQueue.offer(Pair.of(pairA.left, pairB.right));
    //
    //                    activeThreadNum.incrementAndGet();
    //
    //                    parallelSortExecutor.execute(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            try {
    //                                if (errorHolder.value() != null) {
    //                                    return;
    //                                }
    //
    //                                merge(N.copyOfRange(a, pairA.left, pairA.right), 0, pairA.right - pairA.left, a, pairB.left, pairB.right, pairA.left,
    //                                        comparator);
    //
    //                            } catch (Exception e) {
    //                                setError(errorHolder, e);
    //                            } finally {
    //                                activeThreadNum.decrementAndGet();
    //                            }
    //                        }
    //                    });
    //                }
    //            }
    //
    //            while (activeThreadNum.get() > 0) {
    //                N.sleep(1);
    //            }
    //
    //            if (errorHolder.value() != null) {
    //                throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //            }
    //        }
    //
    //        if (errorHolder.value() != null) {
    //            throw ExceptionUtil.toRuntimeException(errorHolder.value());
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param a
    //    * @param fromIndexA
    //    * @param toIndexA
    //    * @param b
    //    * @param fromIndexB
    //    * @param toIndexB
    //    * @param fromIndex
    //    * @param cmp
    //    */
    //    static <T> void merge(final T[] a, int fromIndexA, int toIndexA, final T[] b, int fromIndexB, int toIndexB, int fromIndex, Comparator<? super T> cmp) {
    //        while (fromIndexA < toIndexA && fromIndexB < toIndexB) {
    //            if (cmp.compare(a[fromIndexA], b[fromIndexB]) <= 0) {
    //                b[fromIndex++] = a[fromIndexA++];
    //            } else {
    //                b[fromIndex++] = b[fromIndexB++];
    //            }
    //        }
    //
    //        if (fromIndexA < toIndexA) {
    //            N.copy(a, fromIndexA, b, fromIndex, toIndexA - fromIndexA);
    //            fromIndex += toIndexA - fromIndexA;
    //        }
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param c
    //    */
    //    static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c) {
    //        if (N.isNullOrEmpty(c)) {
    //            return;
    //        }
    //
    //        parallelSort(c, 0, c.size());
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param c
    //    * @param fromIndex
    //    * @param toIndex
    //    */
    //    static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex) {
    //        parallelSort(c, fromIndex, toIndex, Comparators.NATURAL_ORDER);
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param list
    //    * @param cmp
    //    */
    //    static <T> void parallelSort(final List<? extends T> list, final Comparator<? super T> cmp) {
    //        if (N.isNullOrEmpty(list)) {
    //            return;
    //        }
    //
    //        parallelSort(list, 0, list.size(), cmp);
    //    }
    //
    //    /**
    //    *
    //    * @param <T>
    //    * @param c
    //    * @param fromIndex
    //    * @param toIndex
    //    * @param cmp
    //    */
    //    @SuppressWarnings("rawtypes")
    //    static <T> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
    //        if ((N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        @SuppressWarnings("deprecation")
    //        final T[] a = (T[]) InternalUtil.getInternalArray(c);
    //
    //        if (a != null) {
    //            parallelSort(a, fromIndex, toIndex, cmp);
    //
    //            return;
    //        }
    //
    //        final T[] array = (T[]) c.toArray();
    //
    //        parallelSort(array, fromIndex, toIndex, cmp);
    //
    //        final ListIterator<Object> it = (ListIterator) c.listIterator();
    //
    //        for (int i = 0, len = array.length; i < len; i++) {
    //            it.next();
    //
    //            it.set(array[i]);
    //        }
    //    }
    //
    //    /**
    //    * Sets the error.
    //    *
    //    * @param errorHolder
    //    * @param e
    //    */
    //    private static void setError(final Holder<Throwable> errorHolder, Throwable e) {
    //        synchronized (errorHolder) {
    //            if (errorHolder.value() == null) {
    //                errorHolder.setValue(e);
    //            } else {
    //                errorHolder.value().addSuppressed(e);
    //            }
    //        }
    //    }

    //    static double medianOfTwoSortedArrays(final int[] a, final int[] b) {
    //        final int n = a.length;
    //        final int m = b.length;
    //
    //        if (n > m) {
    //            return medianOfTwoSortedArrays(b, a);
    //        }
    //
    //        int k = (n + m - 1) / 2;
    //        int l = 0, r = Math.min(k, n);
    //        while (l < r) {
    //            int mid1 = (l + r) / 2;
    //            int mid2 = k - mid1;
    //
    //            if (a[mid1] < b[mid2]) {
    //                l = mid1 + 1;
    //            } else {
    //                r = mid1;
    //            }
    //        }
    //
    //        int num1 = Math.max(l - 1 >= 0 ? a[l - 1] : Integer.MIN_VALUE, k - l >= 0 ? b[k - l] : Integer.MIN_VALUE);
    //
    //        if ((n + m) % 2 != 0) {
    //            return num1;
    //        }
    //
    //        int num2 = Math.min(l < n ? a[l] : Integer.MAX_VALUE, k - l + 1 < m ? b[k - l + 1] : Integer.MAX_VALUE);
    //
    //        return (num1 + num2) / (double) 2;
    //    }
    //
    //    static int theKthNumberOfTwoSortedArrays(final int[] a, final int[] b, final int k) {
    //        final int n = a.length;
    //        final int m = b.length;
    //
    //        if (n > m) {
    //            return theKthNumberOfTwoSortedArrays(b, a, k);
    //        }
    //
    //        int l = 0, r = Math.min(k, n);
    //        while (l < r) {
    //            int mid1 = (l + r) / 2;
    //            int mid2 = k - mid1;
    //
    //            if (a[mid1] < b[mid2]) {
    //                l = mid1 + 1;
    //            } else {
    //                r = mid1;
    //            }
    //        }
    //
    //        return Math.max(l - 1 >= 0 ? a[l - 1] : Integer.MIN_VALUE, k - l >= 0 ? b[k - l] : Integer.MIN_VALUE);
    //    }
    //
    //    static long theKthNumberOfTwoSortedArrays(final long[] a, final long[] b, final int k) {
    //        final int n = a.length;
    //        final int m = b.length;
    //
    //        if (n > m) {
    //            return theKthNumberOfTwoSortedArrays(b, a, k);
    //        }
    //
    //        int l = 0, r = Math.min(k, n);
    //        while (l < r) {
    //            int mid1 = (l + r) / 2;
    //            int mid2 = k - mid1;
    //
    //            if (a[mid1] < b[mid2]) {
    //                l = mid1 + 1;
    //            } else {
    //                r = mid1;
    //            }
    //        }
    //
    //        return Math.max(l - 1 >= 0 ? a[l - 1] : Integer.MIN_VALUE, k - l >= 0 ? b[k - l] : Integer.MIN_VALUE);
    //    }
    //
    //    static float theKthNumberOfTwoSortedArrays(final float[] a, final float[] b, final int k) {
    //        final int n = a.length;
    //        final int m = b.length;
    //
    //        if (n > m) {
    //            return theKthNumberOfTwoSortedArrays(b, a, k);
    //        }
    //
    //        int l = 0, r = Math.min(k, n);
    //        while (l < r) {
    //            int mid1 = (l + r) / 2;
    //            int mid2 = k - mid1;
    //
    //            if (a[mid1] < b[mid2]) {
    //                l = mid1 + 1;
    //            } else {
    //                r = mid1;
    //            }
    //        }
    //
    //        return Math.max(l - 1 >= 0 ? a[l - 1] : Integer.MIN_VALUE, k - l >= 0 ? b[k - l] : Integer.MIN_VALUE);
    //    }
    //
    //    static double theKthNumberOfTwoSortedArrays(final double[] a, final double[] b, final int k) {
    //        final int n = a.length;
    //        final int m = b.length;
    //
    //        if (n > m) {
    //            return theKthNumberOfTwoSortedArrays(b, a, k);
    //        }
    //
    //        int l = 0, r = Math.min(k, n);
    //        while (l < r) {
    //            int mid1 = (l + r) / 2;
    //            int mid2 = k - mid1;
    //
    //            if (a[mid1] < b[mid2]) {
    //                l = mid1 + 1;
    //            } else {
    //                r = mid1;
    //            }
    //        }
    //
    //        return Math.max(l - 1 >= 0 ? a[l - 1] : Integer.MIN_VALUE, k - l >= 0 ? b[k - l] : Integer.MIN_VALUE);
    //    }
    //
    //    static <E> Collection<List<E>> permutationsOf(final Collection<E> elements) {
    //        return Collections2.permutations(elements);
    //    }
    //
    //    static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutationsOf(final Collection<E> elements) {
    //        return Collections2.orderedPermutations(elements);
    //    }
    //
    //    static <E> Collection<List<E>> orderedPermutationsOf(final Collection<E> elements, final Comparator<? super E> comparator) {
    //        return Collections2.orderedPermutations(elements, comparator);
    //    }
    //
    //    private static final String[] tens = { "", "Ten", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };
    //    private static final String[] lessThan20 = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
    //            "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
    //    private static final String[] thousands = { "", "Thousand", "Million", "Billion" };
    //
    //    static String numberToWords(int num) {
    //        // https://leetcode.com/discuss/55462/my-clean-java-solution-very-easy-to-understand
    //        if (num == 0) {
    //            return "Zero";
    //        }
    //        int i = 0;
    //        String words = "";
    //
    //        while (num > 0) {
    //            if (num % 1000 != 0) {
    //                words = numberToWordsHelper(num % 1000) + thousands[i] + " " + words;
    //            }
    //            num /= 1000;
    //            i++;
    //        }
    //
    //        return words.trim();
    //    }
    //
    //    private static String numberToWordsHelper(int num) {
    //        if (num == 0)
    //            return "";
    //        else if (num < 20)
    //            return lessThan20[num] + " ";
    //        else if (num < 100)
    //            return tens[num / 10] + " " + numberToWordsHelper(num % 10);
    //        else
    //            return lessThan20[num / 100] + " Hundred " + numberToWordsHelper(num % 100);
    //    }
    //
    //    private static final String[] t = { "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz" };
    //
    //    static List<String> letterCombinationsOfPhoneNum(final String digits) {
    //        List<String> res = new ArrayList<String>();
    //
    //        if (digits == null || digits.length() == 0) {
    //            return res;
    //        }
    //
    //        res.add("");
    //        for (int i = 0, len = digits.length(); i < len; i++) {
    //            String str = t[digits.charAt(i) - '0'];
    //            if (str.length() == 0) {
    //                continue;
    //            }
    //            int size = res.size();
    //            for (int j = 0; j < size; j++) {
    //                for (int k = 0; k < str.length(); k++) {
    //                    res.add(res.get(j) + str.charAt(k));
    //                }
    //            }
    //
    //            res = res.subList(size, res.size());
    //        }
    //        return res;
    //    }
    //
    //    static boolean isPowerOfTwo(final int n) {
    //        return (n > 0 && (n & (n - 1)) == 0);
    //    }
    //
    //    static int reverse(int x) {
    //        long res = 0;
    //        while (x != 0) {
    //            res = res * 10 + x % 10;
    //            x = x / 10;
    //        }
    //
    //        return (res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) ? 0 : (int) res;
    //    }
    //
    //    static int reverse(long x) {
    //        long res = 0;
    //        while (x != 0) {
    //            res = res * 10 + x % 10;
    //            x = x / 10;
    //        }
    //
    //        return (res > Long.MAX_VALUE || res < Long.MIN_VALUE) ? 0 : (int) res;
    //    }
    //
    //    static boolean isPalindromeNumber(final int x) {
    //        if (x < 0) {
    //            return false;
    //        }
    //        if (x < 10) {
    //            return true;
    //        }
    //        int y = x;
    //        long z = 0;
    //        while (y != 0) {
    //            z = z * 10 + y % 10;
    //            y = y / 10;
    //        }
    //        return z == x;
    //    }
    //
    //    /**
    //     * Given a string containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.
    //     * The brackets must close in the correct order, "()" and "()[]{}" are all valid but "(]" and "([)]" are not.
    //     * @param str
    //     * @return
    //     */
    //    static boolean isValidParentheses(final String str) {
    //        if (str == null || str.length() == 0) {
    //            return true;
    //        }
    //
    //        final Map<Character, Character> m = new HashMap<Character, Character>();
    //        m.put('(', ')');
    //        m.put('{', '}');
    //        m.put('[', ']');
    //
    //        final Stack<Character> stack = new Stack<>();
    //        for (int i = 0, len = str.length(); i < len; i++) {
    //            char ch = str.charAt(i);
    //            Character p = m.get(ch);
    //
    //            if (p == null) {
    //                if (stack.size() == 0 || m.get(stack.pop()) != ch) {
    //                    return false;
    //                }
    //            } else {
    //                stack.push(ch);
    //            }
    //        }
    //
    //        return stack.size() == 0;
    //    }
    //
    //    static List<String> generateParenthesis(final int n) {
    //        final List<String> res = new ArrayList<>();
    //        generate(n, 0, 0, res, "");
    //        return res;
    //    }
    //
    //    private static void generate(int n, int open, int close, List<String> result, String current) {
    //        if (close == n && open == n) {
    //            result.add(current);
    //        } else {
    //            if (open < n) {
    //                generate(n, open + 1, close, result, current + "(");
    //            }
    //
    //            if (close < open) {
    //                generate(n, open, close + 1, result, current + ")");
    //            }
    //        }
    //    }
    //
    //    static void rotate90Degree(int[][] matrix) {
    //        int n = matrix.length;
    //
    //        for (int i = 0; i < n / 2; ++i) {
    //            for (int j = i; j < n - 1 - i; ++j) {
    //                int tmp = matrix[i][j];
    //                matrix[i][j] = matrix[n - j - 1][i];
    //                matrix[n - j - 1][i] = matrix[n - i - 1][n - j - 1];
    //                matrix[n - i - 1][n - j - 1] = matrix[j][n - i - 1];
    //                matrix[j][n - i - 1] = tmp;
    //            }
    //        }
    //    }

    @Beta
    public static final class ArrayEx extends Array {
        private ArrayEx() {
            // utility class
        }
    }
}
