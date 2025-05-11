/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Random;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.NullSafe;

/**
 * It is a utility class that provides various methods for array manipulation.
 *
 * @see java.lang.reflect.Array
 * @see java.util.Arrays
 * @see com.landawn.abacus.util.CommonUtil
 * @see com.landawn.abacus.util.N
 */
@SuppressWarnings({ "java:S1168" })
public abstract sealed class Array permits Array.ArrayUtil {
    private Array() {
        // Utility class.
    }

    /**
     * Creates a new instance of an array with the specified component type and length.
     *
     * <p>This method uses {@link java.lang.reflect.Array#newInstance(Class, int)} to create a new instance of the specified array.
     *
     * @param <T> The type of the array.
     * @param componentType The Class object representing the component type of the new array.
     * @param length The length of the new array.
     * @return The new array.
     * @throws NegativeArraySizeException if the specified length is negative.
     */
    public static <T> T newInstance(final Class<?> componentType, final int length) throws NegativeArraySizeException {
        if (length == 0) {
            final Object result = N.CLASS_EMPTY_ARRAY.computeIfAbsent(componentType, k -> java.lang.reflect.Array.newInstance(componentType, length));

            return (T) result;
        }

        return (T) java.lang.reflect.Array.newInstance(componentType, length);
    }

    /**
     * Creates a new instance of an array with the specified component type and dimensions.
     *
     * <p>This method uses {@link java.lang.reflect.Array#newInstance(Class, int...)} to create a new instance of the specified array.
     * The dimensions should be a valid int array representing the dimensions of the new array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Array.newInstance(Integer.class, 5, 5); // returns a 5x5 Integer array
     * Array.newInstance(String.class, 3, 3, 3); // returns a 3x3x3 String array
     * </code>
     * </pre>
     *
     * @param <T> The type of the array.
     * @param componentType The Class object representing the component type of the new array.
     * @param dimensions The dimensions of the new array.
     * @return The new array.
     * @throws IllegalArgumentException if the componentType is {@code null}.
     * @throws NegativeArraySizeException if any of the specified dimensions is negative.
     * @see java.lang.reflect.Array#newInstance(Class, int...)
     */
    public static <T> T newInstance(final Class<?> componentType, final int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        return (T) java.lang.reflect.Array.newInstance(componentType, dimensions);
    }

    /**
     * Retrieves the length of the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getLength(Object)} to determine the length of the array.
     * The array can be an object array or a primitive array.
     *
     * @param array The array whose length is to be determined.
     * @return The length of the array.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @see java.lang.reflect.Array#getLength(Object)
     */
    public static int getLength(final Object array) throws IllegalArgumentException {
        return array == null ? 0 : java.lang.reflect.Array.getLength(array);
    }

    /**
     * Retrieves the element at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#get(Object, int)} to retrieve the element.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Integer[] array = {1, 2, 3};
     * Integer element = Array.get(array, 1); // returns 2
     * </code>
     * </pre>
     *
     * @param <T> The type of the array.
     * @param array The array from which to retrieve the element.
     * @param index The index of the element to be retrieved.
     * @return The element at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#get(Object, int)
     */
    public static <T> T get(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return (T) java.lang.reflect.Array.get(array, index);
    }

    /**
     * Retrieves the boolean value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getBoolean(Object, int)} to retrieve the boolean value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * boolean[] array = {true, false, true};
     * boolean element = Array.getBoolean(array, 1); // returns false
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the boolean value.
     * @param index The index of the boolean value to be retrieved.
     * @return The boolean value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getBoolean(Object, int)
     */
    public static boolean getBoolean(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getBoolean(array, index);
    }

    /**
     * Retrieves the byte value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getByte(Object, int)} to retrieve the byte value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * byte[] array = {1, 2, 3};
     * byte element = Array.getByte(array, 1); // returns 2
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the byte value.
     * @param index The index of the byte value to be retrieved.
     * @return The byte value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getByte(Object, int)
     */
    public static byte getByte(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getByte(array, index);
    }

    /**
     * Retrieves the char value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getChar(Object, int)} to retrieve the char value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * char[] array = {'a', 'b', 'c'};
     * char element = Array.getChar(array, 1); // returns 'b'
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the char value.
     * @param index The index of the char value to be retrieved.
     * @return The char value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getChar(Object, int)
     */
    public static char getChar(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getChar(array, index);
    }

    /**
     * Retrieves the short value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getShort(Object, int)} to retrieve the short value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * short[] array = {1, 2, 3};
     * short element = Array.getShort(array, 1); // returns 2
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the short value.
     * @param index The index of the short value to be retrieved.
     * @return The short value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getShort(Object, int)
     */
    public static short getShort(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getShort(array, index);
    }

    /**
     * Retrieves the integer value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getInt(Object, int)} to retrieve the integer value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * int[] array = {1, 2, 3};
     * int element = Array.getInt(array, 1); // returns 2
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the integer value.
     * @param index The index of the integer value to be retrieved.
     * @return The integer value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getInt(Object, int)
     */
    public static int getInt(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getInt(array, index);
    }

    /**
     * Retrieves the long value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getLong(Object, int)} to retrieve the long value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * long[] array = {1L, 2L, 3L};
     * long element = Array.getLong(array, 1); // returns 2L
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the long value.
     * @param index The index of the long value to be retrieved.
     * @return The long value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getLong(Object, int)
     */
    public static long getLong(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getLong(array, index);
    }

    /**
     * Retrieves the float value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getFloat(Object, int)} to retrieve the float value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * float[] array = {1.1f, 2.2f, 3.3f};
     * float element = Array.getFloat(array, 1); // returns 2.2f
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the float value.
     * @param index The index of the float value to be retrieved.
     * @return The float value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getFloat(Object, int)
     */
    public static float getFloat(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getFloat(array, index);
    }

    /**
     * Retrieves the double value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getDouble(Object, int)} to retrieve the double value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * double[] array = {1.1, 2.2, 3.3};
     * double element = Array.getDouble(array, 1); // returns 2.2
     * </code>
     * </pre>
     *
     * @param array The array from which to retrieve the double value.
     * @param index The index of the double value to be retrieved.
     * @return The double value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getDouble(Object, int)
     */
    public static double getDouble(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getDouble(array, index);
    }

    /**
     * Sets the value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#set(Object, int, Object)} to set the value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Integer[] array = {1, 2, 3};
     * Array.set(array, 1, 4); // array now is {1, 4, 3}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the value.
     * @param index The index at which the value is to be set.
     * @param value The value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#set(Object, int, Object)
     */
    public static void set(final Object array, final int index, final Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.set(array, index, value);
    }

    /**
     * Sets the boolean value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setBoolean(Object, int, boolean)} to set the boolean value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * boolean[] array = {true, false, true};
     * Array.setBoolean(array, 1, true); // array now is {true, true, true}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the boolean value.
     * @param index The index at which the boolean value is to be set.
     * @param z The boolean value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setBoolean(Object, int, boolean)
     */
    public static void setBoolean(final Object array, final int index, final boolean z) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setBoolean(array, index, z);
    }

    /**
     * Sets the byte value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setByte(Object, int, byte)} to set the byte value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * byte[] array = {1, 2, 3};
     * Array.setByte(array, 1, (byte)4); // array now is {1, 4, 3}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the byte value.
     * @param index The index at which the byte value is to be set.
     * @param b The byte value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setByte(Object, int, byte)
     */
    public static void setByte(final Object array, final int index, final byte b) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setByte(array, index, b);
    }

    /**
     * Sets the char value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setChar(Object, int, char)} to set the char value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * char[] array = {'a', 'b', 'c'};
     * Array.setChar(array, 1, 'd'); // array now is {'a', 'd', 'c'}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the char value.
     * @param index The index at which the char value is to be set.
     * @param c The char value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setChar(Object, int, char)
     */
    public static void setChar(final Object array, final int index, final char c) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setChar(array, index, c);
    }

    /**
     * Sets the short value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setShort(Object, int, short)} to set the short value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * short[] array = {1, 2, 3};
     * Array.setShort(array, 1, (short)4); // array now is {1, 4, 3}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the short value.
     * @param index The index at which the short value is to be set.
     * @param s The short value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setShort(Object, int, short)
     */
    public static void setShort(final Object array, final int index, final short s) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setShort(array, index, s);
    }

    /**
     * Sets the integer value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setInt(Object, int, int)} to set the integer value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * int[] array = {1, 2, 3};
     * Array.setInt(array, 1, 4); // array now is {1, 4, 3}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the integer value.
     * @param index The index at which the integer value is to be set.
     * @param i The integer value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setInt(Object, int, int)
     */
    public static void setInt(final Object array, final int index, final int i) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setInt(array, index, i);
    }

    /**
     * Sets the long value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setLong(Object, int, long)} to set the long value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * long[] array = {1L, 2L, 3L};
     * Array.setLong(array, 1, 4L); // array now is {1L, 4L, 3L}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the long value.
     * @param index The index at which the long value is to be set.
     * @param l The long value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setLong(Object, int, long)
     */
    public static void setLong(final Object array, final int index, final long l) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setLong(array, index, l);
    }

    /**
     * Sets the float value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setFloat(Object, int, float)} to set the float value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * float[] array = {1.1f, 2.2f, 3.3f};
     * Array.setFloat(array, 1, 4.4f); // array now is {1.1f, 4.4f, 3.3f}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the float value.
     * @param index The index at which the float value is to be set.
     * @param f The float value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setFloat(Object, int, float)
     */
    public static void setFloat(final Object array, final int index, final float f) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setFloat(array, index, f);
    }

    /**
     * Sets the double value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setDouble(Object, int, double)} to set the double value.
     * The array can be an object array or a primitive array.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * double[] array = {1.1, 2.2, 3.3};
     * Array.setDouble(array, 1, 4.4); // array now is {1.1, 4.4, 3.3}
     * </code>
     * </pre>
     *
     * @param array The array in which to set the double value.
     * @param index The index at which the double value is to be set.
     * @param d The double value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setDouble(Object, int, double)
     */
    public static void setDouble(final Object array, final int index, final double d) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setDouble(array, index, d);
    }

    /**
     * Returns a fixed-size list backed by the specified array if it's not {@code null} or empty, otherwise an immutable/unmodifiable empty list is returned.
     *
     * @param <T>
     * @param a
     * @return
     * @see Arrays#asList(Object...)
     * @see N#asList(Object...)
     * @see List#of(Object...)
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(@NullSafe final T... a) {
        return N.isEmpty(a) ? N.emptyList() : Arrays.asList(a);
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of booleans
     * @return the same input array
     */
    public static boolean[] of(final boolean... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of characters
     * @return the same input array
     */
    public static char[] of(final char... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of bytes
     * @return the same input array
     */
    public static byte[] of(final byte... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of shorts
     * @return the same input array
     */
    public static short[] of(final short... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of integers
     * @return the same input array
     */
    public static int[] of(final int... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of longs
     * @return the same input array
     */
    public static long[] of(final long... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of floats
     * @return the same input array
     */
    public static float[] of(final float... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of doubles
     * @return the same input array
     */
    public static double[] of(final double... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param a the input array of strings
     * @return the same input array
     * @see N#asArray(Object...)
     */
    public static String[] of(final String... a) {
        return a;
    }

    //    /**
    //     * Returns the input array.
    //     *
    //     * @param <T>
    //     * @param a
    //     * @return
    //     */
    //    @SafeVarargs
    //    public static <T extends Number> T[] of(final T... a) {
    //        return a;
    //    }

    /**
     * Returns the input array.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.util.Date> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.util.Calendar> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.time.temporal.Temporal> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends Enum<?>> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @deprecated replaced by {@code N.asArray(Object...)}.
     * @see N#asArray(Object...)
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

    //    // Only for Java 8. It's ambiguous in the Java version before 8.
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
     * Generates a range of characters from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to, but not including, the <i>endExclusive</i> character.
     * The characters are generated in ascending order.
     *
     * @param startInclusive The first character (inclusive) in the char array.
     * @param endExclusive The upper bound (exclusive) of the char array.
     * @return A char array containing characters from <i>startInclusive</i> to <i>endExclusive</i>.
     */
    public static char[] range(char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] a = new char[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to, but not including, the <i>endExclusive</i> byte.
     * The bytes are generated in ascending order.
     *
     * @param startInclusive The first byte (inclusive) in the byte array.
     * @param endExclusive The upper bound (exclusive) of the byte array.
     * @return A byte array containing bytes from <i>startInclusive</i> to <i>endExclusive</i>.
     */
    public static byte[] range(byte startInclusive, final byte endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] a = new byte[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to, but not including, the <i>endExclusive</i> short integer.
     * The short integers are generated in ascending order.
     *
     * @param startInclusive The first short integer (inclusive) in the short array.
     * @param endExclusive The upper bound (exclusive) of the short array.
     * @return A short array containing short integers from <i>startInclusive</i> to <i>endExclusive</i>.
     */
    public static short[] range(short startInclusive, final short endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] a = new short[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to, but not including, the <i>endExclusive</i> integer.
     * The integers are generated in ascending order.
     *
     * @param startInclusive The first integer (inclusive) in the integer array.
     * @param endExclusive The upper bound (exclusive) of the integer array.
     * @return An integer array containing integers from <i>startInclusive</i> to <i>endExclusive</i>.
     */
    public static int[] range(int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_INT_ARRAY;
        }

        if ((long) endExclusive - startInclusive > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow"); //NOSONAR
        }

        final int[] a = new int[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to, but not including, the <i>endExclusive</i> long integer.
     * The long integers are generated in ascending order.
     *
     * @param startInclusive The first long integer (inclusive) in the long array.
     * @param endExclusive The upper bound (exclusive) of the long array.
     * @return A long array containing long integers from <i>startInclusive</i> to <i>endExclusive</i>.
     */
    public static long[] range(long startInclusive, final long endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long range = endExclusive - startInclusive;

        if (range < 0 || range > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) range];

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
     * Generates a range of characters from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to, but not including, the <i>endExclusive</i> character.
     * The characters are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first character (inclusive) in the char array.
     * @param endExclusive The upper bound (exclusive) of the char array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent character.
     * @return A char array containing characters from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final char[] a = new char[len];
        final char byChar = (char) by;

        for (int i = 0; i < len; i++, startInclusive += byChar) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to, but not including, the <i>endExclusive</i> byte.
     * The bytes are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first byte (inclusive) in the byte array.
     * @param endExclusive The upper bound (exclusive) of the byte array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent byte.
     * @return A byte array containing bytes from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to, but not including, the <i>endExclusive</i> short integer.
     * The short integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first short integer (inclusive) in the short array.
     * @param endExclusive The upper bound (exclusive) of the short array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent short integer.
     * @return A short array containing short integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to, but not including, the <i>endExclusive</i> integer.
     * The integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first integer (inclusive) in the integer array.
     * @param endExclusive The upper bound (exclusive) of the integer array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent integer.
     * @return An integer array containing integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final long len = ((long) endExclusive - startInclusive) / by + (((long) endExclusive - startInclusive) % by == 0 ? 0 : 1);

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
     * Generates a range of long integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to, but not including, the <i>endExclusive</i> long integer.
     * The long integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first long integer (inclusive) in the long array.
     * @param endExclusive The upper bound (exclusive) of the long array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent long integer.
     * @return A long array containing long integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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
                throw new IllegalArgumentException("Overflow. Array size is too large to allocate: " + m);
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
     * Generates a range of characters from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to and including the <i>endInclusive</i> character.
     * The characters are generated in ascending order.
     *
     * @param startInclusive The first character (inclusive) in the char array.
     * @param endInclusive The upper bound (inclusive) of the char array.
     * @return A char array containing characters from <i>startInclusive</i> to <i>endInclusive</i>.
     */
    public static char[] rangeClosed(char startInclusive, final char endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final char[] a = new char[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to and including the <i>endInclusive</i> byte.
     * The bytes are generated in ascending order.
     *
     * @param startInclusive The first byte (inclusive) in the byte array.
     * @param endInclusive The upper bound (inclusive) of the byte array.
     * @return A byte array containing bytes from <i>startInclusive</i> to <i>endInclusive</i>.
     */
    public static byte[] rangeClosed(byte startInclusive, final byte endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final byte[] a = new byte[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to and including the <i>endInclusive</i> short integer.
     * The short integers are generated in ascending order.
     *
     * @param startInclusive The first short integer (inclusive) in the short array.
     * @param endInclusive The upper bound (inclusive) of the short array.
     * @return A short array containing short integers from <i>startInclusive</i> to <i>endInclusive</i>.
     */
    public static short[] rangeClosed(short startInclusive, final short endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final short[] a = new short[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to and including the <i>endInclusive</i> integer.
     * The integers are generated in ascending order.
     *
     * @param startInclusive The first integer (inclusive) in the integer array.
     * @param endInclusive The upper bound (inclusive) of the integer array.
     * @return An integer array containing integers from <i>startInclusive</i> to <i>endInclusive</i>.
     */
    public static int[] rangeClosed(int startInclusive, final int endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_INT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        if ((long) endInclusive - startInclusive + 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to and including the <i>endInclusive</i> long integer.
     * The long integers are generated in ascending order.
     *
     * @param startInclusive The first long integer (inclusive) in the long array.
     * @param endInclusive The upper bound (inclusive) of the long array.
     * @return A long array containing long integers from <i>startInclusive</i> to <i>endInclusive</i>.
     */
    public static long[] rangeClosed(long startInclusive, final long endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_LONG_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final long range = endInclusive - startInclusive + 1;

        if (range <= 0 || range > Integer.MAX_VALUE) { // Check the final length
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) range];

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
     * Generates a range of characters from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to and including the <i>endInclusive</i> character.
     * The characters are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first character (inclusive) in the char array.
     * @param endInclusive The upper bound (inclusive) of the char array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent character.
     * @return A char array containing characters from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endInclusive - startInclusive) / by + 1;
        final char[] a = new char[len];
        final char byChar = (char) by;

        for (int i = 0; i < len; i++, startInclusive += byChar) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to and including the <i>endInclusive</i> byte.
     * The bytes are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first byte (inclusive) in the byte array.
     * @param endInclusive The upper bound (inclusive) of the byte array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent byte.
     * @return A byte array containing bytes from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endInclusive - startInclusive) / by + 1;
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to and including the <i>endInclusive</i> short integer.
     * The short integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first short integer (inclusive) in the short array.
     * @param endInclusive The upper bound (inclusive) of the short array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent short integer.
     * @return A short array containing short integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final int len = (endInclusive - startInclusive) / by + 1;
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to and including the <i>endInclusive</i> integer.
     * The integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first integer (inclusive) in the integer array.
     * @param endInclusive The upper bound (inclusive) of the integer array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent integer.
     * @return An integer array containing integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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

        final long len = ((long) endInclusive - startInclusive) / by + 1;

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
     * Generates a range of long integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to and including the <i>endInclusive</i> long integer.
     * The long integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     *
     * @param startInclusive The first long integer (inclusive) in the long array.
     * @param endInclusive The upper bound (inclusive) of the long array.
     * @param by The step to increment (if positive) or decrement (if negative) for each subsequent long integer.
     * @return A long array containing long integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by.
     * @throws IllegalArgumentException if <i>by</i> is zero.
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
                throw new IllegalArgumentException("Overflow. Array size is too large to allocate: " + m);
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
     * Generates a new boolean array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The boolean value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A boolean array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static boolean[] repeat(final boolean element, final int n) {
        final boolean[] a = new boolean[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new char array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The char value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A char array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static char[] repeat(final char element, final int n) {
        final char[] a = new char[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new byte array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The byte value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A byte array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static byte[] repeat(final byte element, final int n) {
        final byte[] a = new byte[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new short array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The short value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A short array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static short[] repeat(final short element, final int n) {
        final short[] a = new short[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new integer array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The integer value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return An integer array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static int[] repeat(final int element, final int n) {
        final int[] a = new int[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new long array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The long value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A long array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static long[] repeat(final long element, final int n) {
        final long[] a = new long[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new float array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The float value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A float array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static float[] repeat(final float element, final int n) {
        final float[] a = new float[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new double array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The double value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A double array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static double[] repeat(final double element, final int n) {
        final double[] a = new double[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new String array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element The String value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return A String array of length <i>n</i> with all elements set to <i>element</i>.
     */
    public static String[] repeat(final String element, final int n) {
        final String[] a = new String[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the type of <i>element</i>.
     *
     * @param <T> The type of the elements in the array.
     * @param element The value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return An array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if the specified <i>element</i> is {@code null}.
     * @deprecated prefer to {@link Array#repeatNonNull(Object, int)} or {@link Array#repeat(Object, int, Class)} because this method throws NullPointerException when element is {@code null}
     */
    @Deprecated
    public static <T> T[] repeat(final T element, final int n) throws IllegalArgumentException {
        N.checkArgNotNull(element, cs.element);

        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the <i>elementClass</i> parameter.
     *
     * @param <T> The type of the elements in the array.
     * @param element The value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @param elementClass The class of the elements in the array.
     * @return An array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     */
    public static <T> T[] repeat(final T element, final int n, final Class<? extends T> elementClass) {
        final T[] a = N.newArray(elementClass, n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the type of <i>element</i>.
     * Unlike the {@link Array#repeat(Object, int)} method, this method does not throw a NullPointerException when <i>element</i> is {@code null}.
     *
     * @param <T> The type of the elements in the array.
     * @param element The value to be repeated in the array.
     * @param n The length of the array to be generated.
     * @return An array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if the specified {@code element} is {@code null}.
     */
    public static <T> T[] repeatNonNull(final T element, final int n) throws IllegalArgumentException {
        N.checkArgNotNull(element, cs.element);

        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates an array of random integers of the specified length.
     *
     * @param len the length of the array to be generated
     * @return an array of random integers of the specified length
     * @see Random#nextInt()
     * @see IntList#random(int)
     */
    @Beta
    public static int[] random(final int len) {
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = N.RAND.nextInt();
        }

        return a;
    }

    /**
     * Generates an array of random integers within the specified range.
     *
     * @param startInclusive the lower bound (inclusive) of the random integers
     * @param endExclusive the upper bound (exclusive) of the random integers
     * @param len the length of the array to be generated
     * @return an array of random integers within the specified range
     * @throws IllegalArgumentException if startInclusive is not less than endExclusive
     * @see Random#nextInt(int)
     * @see IntList#random(int, int, int)
     */
    @Beta
    public static int[] random(final int startInclusive, final int endExclusive, final int len) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final int[] a = new int[len];
        final long mod = (long) endExclusive - (long) startInclusive;

        if (mod < Integer.MAX_VALUE) {
            final int n = (int) mod;

            for (int i = 0; i < len; i++) {
                a[i] = N.RAND.nextInt(n) + startInclusive;
            }
        } else {
            for (int i = 0; i < len; i++) {
                a[i] = (int) (Math.abs(N.RAND.nextLong() % mod) + startInclusive);
            }
        }

        return a;
    }

    /**
     * Concatenates two 2D boolean arrays.
     *
     * <p>This method takes two 2D boolean arrays as input and returns a new 2D boolean array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D boolean array to be concatenated.
     * @param b The second 2D boolean array to be concatenated.
     * @return A new 2D boolean array which is the concatenation of the input arrays.
     */
    public static boolean[][] concat(final boolean[][] a, final boolean[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new boolean[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D boolean arrays.
     *
     * <p>This method takes two 3D boolean arrays as input and returns a new 3D boolean array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D boolean array to be concatenated.
     * @param b The second 3D boolean array to be concatenated.
     * @return A new 3D boolean array which is the concatenation of the input arrays.
     */
    public static boolean[][][] concat(final boolean[][][] a, final boolean[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new boolean[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D char arrays.
     *
     * <p>This method takes two 2D char arrays as input and returns a new 2D char array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D char array to be concatenated.
     * @param b The second 2D char array to be concatenated.
     * @return A new 2D char array which is the concatenation of the input arrays.
     */
    public static char[][] concat(final char[][] a, final char[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new char[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D char arrays.
     *
     * <p>This method takes two 3D char arrays as input and returns a new 3D char array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D char array to be concatenated.
     * @param b The second 3D char array to be concatenated.
     * @return A new 3D char array which is the concatenation of the input arrays.
     */
    public static char[][][] concat(final char[][][] a, final char[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new char[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D byte arrays.
     *
     * <p>This method takes two 2D byte arrays as input and returns a new 2D byte array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D byte array to be concatenated.
     * @param b The second 2D byte array to be concatenated.
     * @return A new 2D byte array which is the concatenation of the input arrays.
     */
    public static byte[][] concat(final byte[][] a, final byte[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new byte[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D byte arrays.
     *
     * <p>This method takes two 3D byte arrays as input and returns a new 3D byte array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D byte array to be concatenated.
     * @param b The second 3D byte array to be concatenated.
     * @return A new 3D byte array which is the concatenation of the input arrays.
     */
    public static byte[][][] concat(final byte[][][] a, final byte[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new byte[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D short arrays.
     *
     * <p>This method takes two 2D short arrays as input and returns a new 2D short array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D short array to be concatenated.
     * @param b The second 2D short array to be concatenated.
     * @return A new 2D short array which is the concatenation of the input arrays.
     */
    public static short[][] concat(final short[][] a, final short[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new short[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D short arrays.
     *
     * <p>This method takes two 3D short arrays as input and returns a new 3D short array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D short array to be concatenated.
     * @param b The second 3D short array to be concatenated.
     * @return A new 3D short array which is the concatenation of the input arrays.
     */
    public static short[][][] concat(final short[][][] a, final short[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new short[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D integer arrays.
     *
     * <p>This method takes two 2D integer arrays as input and returns a new 2D integer array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D integer array to be concatenated.
     * @param b The second 2D integer array to be concatenated.
     * @return A new 2D integer array which is the concatenation of the input arrays.
     */
    public static int[][] concat(final int[][] a, final int[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new int[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D integer arrays.
     *
     * <p>This method takes two 3D integer arrays as input and returns a new 3D integer array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D integer array to be concatenated.
     * @param b The second 3D integer array to be concatenated.
     * @return A new 3D integer array which is the concatenation of the input arrays.
     */
    public static int[][][] concat(final int[][][] a, final int[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new int[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D long arrays.
     *
     * <p>This method takes two 2D long arrays as input and returns a new 2D long array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D long array to be concatenated.
     * @param b The second 2D long array to be concatenated.
     * @return A new 2D long array which is the concatenation of the input arrays.
     */
    public static long[][] concat(final long[][] a, final long[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new long[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D long arrays.
     *
     * <p>This method takes two 3D long arrays as input and returns a new 3D long array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D long array to be concatenated.
     * @param b The second 3D long array to be concatenated.
     * @return A new 3D long array which is the concatenation of the input arrays.
     */
    public static long[][][] concat(final long[][][] a, final long[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new long[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D float arrays.
     *
     * <p>This method takes two 2D float arrays as input and returns a new 2D float array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D float array to be concatenated.
     * @param b The second 2D float array to be concatenated.
     * @return A new 2D float array which is the concatenation of the input arrays.
     */
    public static float[][] concat(final float[][] a, final float[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new float[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D float arrays.
     *
     * <p>This method takes two 3D float arrays as input and returns a new 3D float array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D float array to be concatenated.
     * @param b The second 3D float array to be concatenated.
     * @return A new 3D float array which is the concatenation of the input arrays.
     */
    public static float[][][] concat(final float[][][] a, final float[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new float[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D double arrays.
     *
     * <p>This method takes two 2D double arrays as input and returns a new 2D double array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 2D double array to be concatenated.
     * @param b The second 2D double array to be concatenated.
     * @return A new 2D double array which is the concatenation of the input arrays.
     */
    public static double[][] concat(final double[][] a, final double[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new double[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D double arrays.
     *
     * <p>This method takes two 3D double arrays as input and returns a new 3D double array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param a The first 3D double array to be concatenated.
     * @param b The second 3D double array to be concatenated.
     * @return A new 3D double array which is the concatenation of the input arrays.
     */
    public static double[][][] concat(final double[][][] a, final double[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new double[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 2D arrays of generic type T.
     *
     * <p>This method takes two 2D arrays of type T as input and returns a new 2D array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param <T> The type of the elements in the arrays.
     * @param a The first 2D array to be concatenated.
     * @param b The second 2D array to be concatenated.
     * @return A new 2D array which is the concatenation of the input arrays.
     */
    public static <T> T[][] concatt(final T[][] a, final T[][] b) {
        if (N.isEmpty(a)) {
            return N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Concatenates two 3D arrays of generic type T.
     *
     * <p>This method takes two 3D arrays of type T as input and returns a new 3D array which is the concatenation of the two input arrays.
     * The input arrays are not modified during the operation.
     *
     * @param <T> The type of the elements in the arrays.
     * @param a The first 3D array to be concatenated.
     * @param b The second 3D array to be concatenated.
     * @return A new 3D array which is the concatenation of the input arrays.
     */
    public static <T> T[][][] concatt(final T[][][] a, final T[][][] b) {
        if (N.isEmpty(a)) {
            return N.clone(b);
        } else if (N.isEmpty(b)) {
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
     * Converts an array of primitive booleans to an array of Boolean objects.
     *
     * @param a The array of primitive booleans to be converted.
     * @return An array of Boolean objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Boolean[] box(final boolean... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive booleans to an array of Boolean objects.
     *
     * @param a The array of primitive booleans to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Boolean objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Boolean[] box(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_OBJ_ARRAY;
        }

        final Boolean[] result = new Boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive chars to an array of Character objects.
     *
     * @param a The array of primitive chars to be converted.
     * @return An array of Character objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Character[] box(final char... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive chars to an array of Character objects.
     *
     * @param a The array of primitive chars to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Character objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Character[] box(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHAR_OBJ_ARRAY;
        }

        final Character[] result = new Character[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Byte[] box(final byte... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Byte objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Byte[] box(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_OBJ_ARRAY;
        }

        final Byte[] result = new Byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Short[] box(final short... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive shorts to an array of Short objects.
     *
     * @param a The array of primitive shorts to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Short objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Short[] box(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_OBJ_ARRAY;
        }

        final Short[] result = new Short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Integer[] box(final int... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive integers to an array of Integer objects.
     *
     * @param a The array of primitive integers to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Integer objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Integer[] box(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INT_OBJ_ARRAY;
        }

        final Integer[] result = new Integer[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Long[] box(final long... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive longs to an array of Long objects.
     *
     * @param a The array of primitive longs to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Long objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Long[] box(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_OBJ_ARRAY;
        }

        final Long[] result = new Long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Float[] box(final float... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive floats to an array of Float objects.
     *
     * @param a The array of primitive floats to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Float objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Float[] box(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_OBJ_ARRAY;
        }

        final Float[] result = new Float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a The array of primitive bytes to be converted.
     * @return An array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Double[] box(final double... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive doubles to an array of Double objects.
     *
     * @param a The array of primitive doubles to be converted.
     * @param fromIndex The start index of the portion to be converted.
     * @param toIndex The end index of the portion to be converted.
     * @return An array of Double objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Double[] box(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_OBJ_ARRAY;
        }

        final Double[] result = new Double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts a 2D array of primitive booleans to a 2D array of Boolean objects.
     *
     * @param a The 2D array of primitive booleans to be converted.
     * @return A 2D array of Boolean objects, {@code null} if the input array is {@code null}.
     * @see #box(boolean[])
     */
    @MayReturnNull
    public static Boolean[][] box(final boolean[][] a) {
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
     * Converts a 2D array of primitive chars to a 2D array of Character objects.
     *
     * @param a The 2D array of primitive chars to be converted.
     * @return A 2D array of Character objects, {@code null} if the input array is {@code null}.
     * @see #box(char[])
     */
    @MayReturnNull
    public static Character[][] box(final char[][] a) {
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
     * Converts a 2D array of primitive bytes to a 2D array of Byte objects.
     *
     * @param a The 2D array of primitive bytes to be converted.
     * @return A 2D array of Byte objects, {@code null} if the input array is {@code null}.
     * @see #box(byte[])
     */
    @MayReturnNull
    public static Byte[][] box(final byte[][] a) {
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
     * Converts a 2D array of primitive shorts to a 2D array of Short objects.
     *
     * @param a The 2D array of primitive shorts to be converted.
     * @return A 2D array of Short objects, {@code null} if the input array is {@code null}.
     * @see #box(short[])
     */
    @MayReturnNull
    public static Short[][] box(final short[][] a) {
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
     * Converts a 2D array of primitive integers to a 2D array of Integer objects.
     *
     * @param a The 2D array of primitive integers to be converted.
     * @return A 2D array of Integer objects, {@code null} if the input array is {@code null}.
     * @see #box(int[])
     */
    @MayReturnNull
    public static Integer[][] box(final int[][] a) {
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
     * Converts a 2D array of primitive longs to a 2D array of Long objects.
     *
     * @param a The 2D array of primitive longs to be converted.
     * @return A 2D array of Long objects, {@code null} if the input array is {@code null}.
     * @see #box(long[])
     */
    @MayReturnNull
    public static Long[][] box(final long[][] a) {
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
     * Converts a 2D array of primitive floats to a 2D array of Float objects.
     *
     * @param a The 2D array of primitive floats to be converted.
     * @return A 2D array of Float objects, {@code null} if the input array is {@code null}.
     * @see #box(float[])
     */
    @MayReturnNull
    public static Float[][] box(final float[][] a) {
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
     * Converts a 2D array of primitive doubles to a 2D array of Double objects.
     *
     * @param a The 2D array of primitive doubles to be converted.
     * @return A 2D array of Double objects, {@code null} if the input array is {@code null}.
     * @see #box(double[])
     */
    @MayReturnNull
    public static Double[][] box(final double[][] a) {
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
     * Converts a 3D array of primitive booleans to a 3D array of Boolean objects.
     *
     * @param a The 3D array of primitive booleans to be converted.
     * @return A 3D array of Boolean objects, {@code null} if the input array is {@code null}.
     * @see #box(boolean[])
     * @see #box(boolean[][])
     */
    @MayReturnNull
    public static Boolean[][][] box(final boolean[][][] a) {
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
     * Converts a 3D array of primitive chars to a 3D array of Character objects.
     *
     * @param a The 3D array of primitive chars to be converted.
     * @return A 3D array of Character objects, {@code null} if the input array is {@code null}.
     * @see #box(char[])
     * @see #box(char[][])
     */
    @MayReturnNull
    public static Character[][][] box(final char[][][] a) {
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
     * Converts a 3D array of primitive bytes to a 3D array of Byte objects.
     *
     * @param a The 3D array of primitive bytes to be converted.
     * @return A 3D array of Byte objects, {@code null} if the input array is {@code null}.
     * @see #box(byte[])
     * @see #box(byte[][])
     */
    @MayReturnNull
    public static Byte[][][] box(final byte[][][] a) {
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
     * Converts a 3D array of primitive shorts to a 3D array of Short objects.
     *
     * @param a The 3D array of primitive shorts to be converted.
     * @return A 3D array of Short objects, {@code null} if the input array is {@code null}.
     * @see #box(short[])
     * @see #box(short[][])
     */
    @MayReturnNull
    public static Short[][][] box(final short[][][] a) {
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
     * Converts a 3D array of primitive integers to a 3D array of Integer objects.
     *
     * @param a The 3D array of primitive integers to be converted.
     * @return A 3D array of Integer objects, {@code null} if the input array is {@code null}.
     * @see #box(int[])
     * @see #box(int[][])
     */
    @MayReturnNull
    public static Integer[][][] box(final int[][][] a) {
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
     * Converts a 3D array of primitive longs to a 3D array of Long objects.
     *
     * @param a The 3D array of primitive longs to be converted.
     * @return A 3D array of Long objects, {@code null} if the input array is {@code null}.
     * @see #box(long[])
     * @see #box(long[][])
     */
    @MayReturnNull
    public static Long[][][] box(final long[][][] a) {
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
     * Converts a 3D array of primitive floats to a 3D array of Float objects.
     *
     * @param a The 3D array of primitive floats to be converted.
     * @return A 3D array of Float objects, {@code null} if the input array is {@code null}.
     * @see #box(float[])
     * @see #box(float[][])
     */
    @MayReturnNull
    public static Float[][][] box(final float[][][] a) {
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
     * Converts a 3D array of primitive doubles to a 3D array of Double objects.
     *
     * @param a The 3D array of primitive doubles to be converted.
     * @return A 3D array of Double objects, {@code null} if the input array is {@code null}.
     * @see #box(double[])
     * @see #box(double[][])
     */
    @MayReturnNull
    public static Double[][][] box(final double[][][] a) {
        if (a == null) {
            return null;
        }

        final Double[][][] result = new Double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    // ----------------------------------------------------------------------

    /**
     * Converts an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value {@code false}.
     *
     * @param a The array of Boolean objects to be converted.
     * @return An array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[], boolean)
     * @see #unbox(Boolean[], int, int, boolean)
     */
    public static boolean[] unbox(final Boolean... a) {
        return unbox(a, false);
    }

    /**
     * Converts an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Boolean objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive booleans, {@code null} if the input array is {@code null}.
     * See #unbox(Boolean[], int, int, boolean)
     */
    @MayReturnNull
    public static boolean[] unbox(final Boolean[] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Boolean objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive booleans, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Boolean[], boolean)
     */
    @MayReturnNull
    public static boolean[] unbox(final Boolean[] a, final int fromIndex, final int toIndex, final boolean valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a The array of Character objects to be converted.
     * @return An array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], char)
     * @see #unbox(Character[], int, int, char)
     */
    public static char[] unbox(final Character... a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Character objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], int, int, char)
     */
    @MayReturnNull
    public static char[] unbox(final Character[] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Character objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive chars, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Character[], char)
     * @see #unbox(Character[])
     */
    @MayReturnNull
    public static char[] unbox(final Character[] a, final int fromIndex, final int toIndex, final char valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a The array of Byte objects to be converted.
     * @return An array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], byte)
     * @see #unbox(Byte[], int, int, byte)
     */
    public static byte[] unbox(final Byte... a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Byte objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], int, int, byte)
     */
    @MayReturnNull
    public static byte[] unbox(final Byte[] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Byte objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive bytes, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Byte[], byte)
     * @see #unbox(Byte[])
     */
    @MayReturnNull
    public static byte[] unbox(final Byte[] a, final int fromIndex, final int toIndex, final byte valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a The array of Short objects to be converted.
     * @return An array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], short)
     * @see #unbox(Short[], int, int, short)
     */
    public static short[] unbox(final Short... a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Short objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], int, int, short)
     */
    @MayReturnNull
    public static short[] unbox(final Short[] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Short objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive shorts, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Short[], short)
     * @see #unbox(Short[])
     */
    @MayReturnNull
    public static short[] unbox(final Short[] a, final int fromIndex, final int toIndex, final short valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * @param a The array of Integer objects to be converted.
     * @return An array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[], int)
     * @see #unbox(Integer[], int, int, int)
     */
    public static int[] unbox(final Integer... a) {
        return unbox(a, 0);
    }

    /**
     * Converts an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Integer objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[], int, int, int)
     * @see #unbox(Integer...)
     */
    @MayReturnNull
    public static int[] unbox(final Integer[] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Integer objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive integers, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Integer[], int)
     * @see #unbox(Integer...)
     */
    @MayReturnNull
    public static int[] unbox(final Integer[] a, final int fromIndex, final int toIndex, final int valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a The array of Long objects to be converted.
     * @return An array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], long)
     * @see #unbox(Long[], int, int, long)
     */
    public static long[] unbox(final Long... a) {
        return unbox(a, 0L);
    }

    /**
     * Converts an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Long objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], int, int, long)
     * @see #unbox(Long...)
     */
    @MayReturnNull
    public static long[] unbox(final Long[] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Long objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive longs, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Long[], long)
     * @see #unbox(Long...)
     */
    @MayReturnNull
    public static long[] unbox(final Long[] a, final int fromIndex, final int toIndex, final long valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a The array of Float objects to be converted.
     * @return An array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], float)
     * @see #unbox(Float[], int, int, float)
     */
    public static float[] unbox(final Float... a) {
        return unbox(a, 0f);
    }

    /**
     * Converts an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Float objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], int, int, float)
     * @see #unbox(Float...)
     */
    @MayReturnNull
    public static float[] unbox(final Float[] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Float objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive floats, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Float[], float)
     * @see #unbox(Float...)
     */
    @MayReturnNull
    public static float[] unbox(final Float[] a, final int fromIndex, final int toIndex, final float valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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

    /**
     * Converts an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a The array of Double objects to be converted.
     * @return An array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], double)
     * @see #unbox(Double[], int, int, double)
     */
    public static double[] unbox(final Double... a) {
        return unbox(a, 0d);
    }

    /**
     * Converts an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Double objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], int, int, double)
     * @see #unbox(Double...)
     */
    @MayReturnNull
    public static double[] unbox(final Double[] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The array of Double objects to be converted.
     * @param fromIndex The starting index (inclusive) in the array to be converted.
     * @param toIndex The ending index (exclusive) in the array to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return An array of primitive doubles, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Double[], double)
     * @see #unbox(Double...)
     */
    @MayReturnNull
    public static double[] unbox(final Double[] a, final int fromIndex, final int toIndex, final double valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

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
     * Converts a 2D array of Boolean objects into a 2D array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (boolean) {@code false}.
     *
     * @param a The 2D array of Boolean objects to be converted.
     * @return A 2D array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][], boolean)
     * @see #unbox(Boolean[])
     */
    public static boolean[][] unbox(final Boolean[][] a) {
        return unbox(a, false);
    }

    /**
     * Converts a 2D array of Boolean objects into a 2D array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Boolean objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[], boolean)
     */
    @MayReturnNull
    public static boolean[][] unbox(final Boolean[][] a, final boolean valueForNull) {
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
     * Converts a 2D array of Character objects into a 2D array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a The 2D array of Character objects to be converted.
     * @return A 2D array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][], char)
     * @see #unbox(Character[])
     */
    public static char[][] unbox(final Character[][] a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts a 2D array of Character objects into a 2D array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Character objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], char)
     */
    @MayReturnNull
    public static char[][] unbox(final Character[][] a, final char valueForNull) {
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
     * Converts a 2D array of Byte objects into a 2D array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a The 2D array of Byte objects to be converted.
     * @return A 2D array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][], byte)
     * @see #unbox(Byte[])
     */
    public static byte[][] unbox(final Byte[][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts a 2D array of Byte objects into a 2D array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Byte objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], byte)
     */
    @MayReturnNull
    public static byte[][] unbox(final Byte[][] a, final byte valueForNull) {
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
     * Converts a 2D array of Short objects into a 2D array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a The 2D array of Short objects to be converted.
     * @return A 2D array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][], short)
     * @see #unbox(Short[])
     */
    public static short[][] unbox(final Short[][] a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts a 2D array of Short objects into a 2D array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Short objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], short)
     */
    @MayReturnNull
    public static short[][] unbox(final Short[][] a, final short valueForNull) {
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
     * Converts a 2D array of Integer objects into a 2D array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * @param a The 2D array of Integer objects to be converted.
     * @return A 2D array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][], int)
     * @see #unbox(Integer[])
     */
    public static int[][] unbox(final Integer[][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 2D array of Integer objects into a 2D array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Integer objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[], int)
     */
    @MayReturnNull
    public static int[][] unbox(final Integer[][] a, final int valueForNull) {
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
     * Converts a 2D array of Long objects into a 2D array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a The 2D array of Long objects to be converted.
     * @return A 2D array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][], long)
     * @see #unbox(Long[])
     */
    public static long[][] unbox(final Long[][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 2D array of Long objects into a 2D array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Long objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], long)
     */
    @MayReturnNull
    public static long[][] unbox(final Long[][] a, final long valueForNull) {
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
     * Converts a 2D array of Float objects into a 2D array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a The 2D array of Float objects to be converted.
     * @return A 2D array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][], float)
     * @see #unbox(Float[])
     */
    public static float[][] unbox(final Float[][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 2D array of Float objects into a 2D array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Float objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], float)
     */
    @MayReturnNull
    public static float[][] unbox(final Float[][] a, final float valueForNull) {
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
     * Converts a 2D array of Double objects into a 2D array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a The 2D array of Double objects to be converted.
     * @return A 2D array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][], double)
     * @see #unbox(Double[])
     */
    public static double[][] unbox(final Double[][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 2D array of Double objects into a 2D array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 2D array of Double objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 2D array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], double)
     */
    @MayReturnNull
    public static double[][] unbox(final Double[][] a, final double valueForNull) {
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
     * Converts a 3D array of Boolean objects into a 3D array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (boolean) {@code false}.
     *
     * @param a The 3D array of Boolean objects to be converted.
     * @return A 3D array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][][], boolean)
     */
    public static boolean[][][] unbox(final Boolean[][][] a) {
        return unbox(a, false);
    }

    /**
     * Converts a 3D array of Boolean objects into a 3D array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Boolean objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][], boolean)
     */
    @MayReturnNull
    public static boolean[][][] unbox(final Boolean[][][] a, final boolean valueForNull) {
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
     * Converts a 3D array of Character objects into a 3D array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a The 3D array of Character objects to be converted.
     * @return A 3D array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][][], char)
     */
    public static char[][][] unbox(final Character[][][] a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts a 3D array of Character objects into a 3D array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Character objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][], char)
     */
    @MayReturnNull
    public static char[][][] unbox(final Character[][][] a, final char valueForNull) {
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
     * Converts a 3D array of Byte objects into a 3D array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a The 3D array of Byte objects to be converted.
     * @return A 3D array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][][], byte)
     */
    public static byte[][][] unbox(final Byte[][][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts a 3D array of Byte objects into a 3D array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Byte objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][], byte)
     */
    @MayReturnNull
    public static byte[][][] unbox(final Byte[][][] a, final byte valueForNull) {
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
     * Converts a 3D array of Short objects into a 3D array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a The 3D array of Short objects to be converted.
     * @return A 3D array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][][], short)
     */
    public static short[][][] unbox(final Short[][][] a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts a 3D array of Short objects into a 3D array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Short objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][], short)
     */
    @MayReturnNull
    public static short[][][] unbox(final Short[][][] a, final short valueForNull) {
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
     * Converts a 3D array of Integer objects into a 3D array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * @param a The 3D array of Integer objects to be converted.
     * @return A 3D array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][][], int)
     */
    public static int[][][] unbox(final Integer[][][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 3D array of Integer objects into a 3D array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Integer objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][], int)
     */
    @MayReturnNull
    public static int[][][] unbox(final Integer[][][] a, final int valueForNull) {
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
     * Converts a 3D array of Long objects into a 3D array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a The 3D array of Long objects to be converted.
     * @return A 3D array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][][], long)
     */
    public static long[][][] unbox(final Long[][][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 3D array of Long objects into a 3D array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Long objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][], long)
     */
    @MayReturnNull
    public static long[][][] unbox(final Long[][][] a, final long valueForNull) {
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
     * Converts a 3D array of Float objects into a 3D array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a The 3D array of Float objects to be converted.
     * @return A 3D array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][][], float)
     */
    public static float[][][] unbox(final Float[][][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 3D array of Float objects into a 3D array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Float objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][], float)
     */
    @MayReturnNull
    public static float[][][] unbox(final Float[][][] a, final float valueForNull) {
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
     * Converts a 3D array of Double objects into a 3D array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a The 3D array of Double objects to be converted.
     * @return A 3D array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][][], double)
     */
    public static double[][][] unbox(final Double[][][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a 3D array of Double objects into a 3D array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a The 3D array of Double objects to be converted.
     * @param valueForNull The value to be used for {@code null} values in the input array.
     * @return A 3D array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][], double)
     */
    @MayReturnNull
    public static double[][][] unbox(final Double[][][] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][][] result = new double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static boolean[][] transpose(final boolean[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final boolean[][] c = new boolean[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static char[][] transpose(final char[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final char[][] c = new char[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static byte[][] transpose(final byte[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final byte[][] c = new byte[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static short[][] transpose(final short[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final short[][] c = new short[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static int[][] transpose(final int[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final int[][] c = new int[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static long[][] transpose(final long[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final long[][] c = new long[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static float[][] transpose(final float[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final float[][] c = new float[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input 2D array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the 2D array to be transposed
     * @return the transposed 2D array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static double[][] transpose(final double[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final double[][] c = new double[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the given 2D array.
     * The original 2D array is unchanged; a new 2D array representing the transposed matrix is returned.
     * This method can be used to interchange the rows and columns of the 2D array.
     *
     * @param <T> The type of the elements in the 2D array.
     * @param a The original 2D array to be transposed.
     * @return A new 2D array representing the transposed matrix, or {@code null} if the input array is {@code null}.
     * @throws IllegalArgumentException if the input array is not a matrix.
     */
    @MayReturnNull
    @Beta
    public static <T> T[][] transpose(final T[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final T[][] c = newInstance(a[0].getClass().getComponentType(), cols, rows);

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    private static void checkIfMatrixArray(final Object[] a) {
        if (a == null || a.length <= 1) {
            return;
        }

        final int cols = getLength(a[0]);

        for (int i = 1, len = a.length; i < len; i++) {
            if (getLength(a[i]) != cols) {
                throw new IllegalArgumentException("The length of sub arrays must be same");
            }
        }
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(array)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(a)) {
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
    //        if (N.isEmpty(a)) {
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
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
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
    //        if (N.isEmpty(c)) {
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
    //        if (N.isEmpty(list)) {
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
    //        if ((N.isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
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
    public static final class ArrayUtil extends Array {
        private ArrayUtil() {
            // utility class
        }
    }
}
