/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Predicate;

import com.landawn.abacus.util.u.OptionalInt;

public final class Index {

    private static final OptionalInt NOT_FOUND = OptionalInt.empty();

    private Index() {
        // singleton.
    }

    /**
     * Returns the index of the first occurrence of the specified boolean value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given boolean array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(boolean[], boolean, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified boolean value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given boolean array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified char value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given char array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     *
     * @param a the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(char[], char, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final char[] a, final char valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified char value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given char array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(char[], char)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final char[] a, final char valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given byte array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * <p>
     * Example:
     * <pre>{@code
     * byte[] arr = {1, 2, 3, 2, 1};
     * Index.of(arr, (byte) 2).get();  // returns 1
     * Index.of(arr, (byte) 5).isPresent();  // returns false
     * }</pre>
     *
     * @param a the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(byte[], byte, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given byte array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * byte[] arr = {1, 2, 3, 2, 1};
     * Index.of(arr, (byte) 2, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(byte[], byte)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given short array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * <p>
     * Example:
     * <pre>{@code
     * short[] arr = {10, 20, 30, 20, 10};
     * Index.of(arr, (short) 20).get();  // returns 1
     * }</pre>
     *
     * @param a the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(short[], short, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final short[] a, final short valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given short array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * short[] arr = {10, 20, 30, 20, 10};
     * Index.of(arr, (short) 20, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(short[], short)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final short[] a, final short valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given int array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * <p>
     * Example:
     * <pre>{@code
     * int[] arr = {100, 200, 300, 200, 100};
     * Index.of(arr, 200).get();  // returns 1
     * }</pre>
     *
     * @param a the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(int[], int, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final int[] a, final int valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given int array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * int[] arr = {100, 200, 300, 200, 100};
     * Index.of(arr, 200, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(int[], int)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final int[] a, final int valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given long array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * <p>
     * Example:
     * <pre>{@code
     * long[] arr = {1000L, 2000L, 3000L, 2000L, 1000L};
     * Index.of(arr, 2000L).get();  // returns 1
     * }</pre>
     *
     * @param a the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(long[], long, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final long[] a, final long valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given long array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * long[] arr = {1000L, 2000L, 3000L, 2000L, 1000L};
     * Index.of(arr, 2000L, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(long[], long)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final long[] a, final long valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given float array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned. Comparison is performed
     * using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     * <p>
     * Example:
     * <pre>{@code
     * float[] arr = {1.5f, 2.5f, 3.5f, 2.5f, 1.5f};
     * Index.of(arr, 2.5f).get();  // returns 1
     * }</pre>
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(float[], float, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final float[] a, final float valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given float array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * Comparison is performed using {@link Float#compare(float, float)}.
     * <p>
     * Example:
     * <pre>{@code
     * float[] arr = {1.5f, 2.5f, 3.5f, 2.5f, 1.5f};
     * Index.of(arr, 2.5f, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(float[], float)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final float[] a, final float valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given double array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned. Comparison is performed
     * using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     * <p>
     * Example:
     * <pre>{@code
     * double[] arr = {1.5, 2.5, 3.5, 2.5, 1.5};
     * Index.of(arr, 2.5).get();  // returns 1
     * }</pre>
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(double[], double, int)
     * @see #of(double[], double, double)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final double[] a, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given double array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * Comparison is performed using {@link Double#compare(double, double)}.
     * <p>
     * Example:
     * <pre>{@code
     * double[] arr = {1.5, 2.5, 3.5, 2.5, 1.5};
     * Index.of(arr, 2.5, 2).get();  // returns 3
     * }</pre>
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(double[], double)
     * @see #of(double[], double, double, int)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, within a given tolerance.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given double array.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance,
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @see #of(double[], double, double, int)
     * @see #of(double[], double)
     * @see N#indexOf(double[], double, double)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance) {
        return of(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, within a given tolerance and starting from the specified index.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given double array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance at or after {@code fromIndex},
     *         or an empty OptionalInt if no value is found within tolerance, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(double[], double, double)
     * @see #of(double[], double, int)
     * @see N#indexOf(double[], double, double, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, tolerance, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given object array
     * using {@code equals()} for comparison. {@code null} values are handled correctly - a {@code null}
     * valueToFind will match the first {@code null} element in the array.
     * <p>
     * Example:
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b"};
     * Index.of(arr, "b").get();        // returns 1
     * Index.of(arr, "d").isPresent();  // returns false
     * }</pre>
     *
     * @param a the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given object array,
     * beginning at the specified {@code fromIndex}. Uses {@code equals()} for comparison.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the collection.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given collection
     * using {@code equals()} for comparison. The index represents the position in iteration order.
     * {@code null} values are handled correctly.
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #of(Collection, Object, int)
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.indexOf(c, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the collection, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given collection,
     * beginning at the specified {@code fromIndex}. Uses {@code equals()} for comparison.
     * Negative {@code fromIndex} values are treated as 0. The index represents the position in iteration order.
     * <p>
     * Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b", "a");
     * Index.of(list, "b", 2).get();  // returns 3
     * }</pre>
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the collection is {@code null}, or {@code fromIndex >= collection.size()}
     * @see #of(Collection, Object)
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(c, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} by iterating through the iterator
     * and using {@code equals()} for comparison. The iterator will be consumed up to and including the matching element.
     * Note that the iterator cannot be reset, so this operation is destructive.
     *
     * @param iter the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @see #of(Iterator, Object, int)
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind) {
        return toOptionalInt(N.indexOf(iter, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator, starting from the specified index.
     * <p>
     * This method skips the first {@code fromIndex} elements, then searches for {@code valueToFind} using {@code equals()}
     * for comparison. The iterator will be consumed up to and including the matching element (or exhausted if not found).
     * Note that the iterator cannot be reset, so this operation is destructive. Negative {@code fromIndex} values are treated as 0.
     *
     * @param iter the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @see #of(Iterator, Object)
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(iter, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified character in the string.
     * <p>
     * This method searches for the first occurrence of the character (represented as an int Unicode code point)
     * in the given string. If the string is {@code null} or empty, an empty OptionalInt is returned.
     *
     * @param str the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character,
     *         or an empty OptionalInt if the character is not found or the string is {@code null}
     * @see #of(String, int, int)
     * @see Strings#indexOf(String, int)
     * @see String#indexOf(int)
     */
    public static OptionalInt of(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified character in the string, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of the character (represented as an int Unicode code point)
     * in the given string, beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * Index.of("hello world", 'o', 5).get();  // returns 7
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character at or after {@code fromIndex},
     *         or an empty OptionalInt if the character is not found, the string is {@code null}, or {@code fromIndex >= str.length()}
     * @see #of(String, int)
     * @see Strings#indexOf(String, int, int)
     * @see String#indexOf(int, int)
     */
    public static OptionalInt of(final String str, final int charValueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code str}.
     * If the string is {@code null} or empty, an empty OptionalInt is returned.
     * <p>
     * Example:
     * <pre>{@code
     * Index.of("hello world", "world").get();  // returns 6
     * Index.of("hello world", "bye").isPresent();  // returns false
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring,
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #of(String, String, int)
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOf(String, String)
     * @see String#indexOf(String)
     */
    public static OptionalInt of(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(str, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code str},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * Example:
     * <pre>{@code
     * Index.of("hello world hello", "hello", 1).get();  // returns 12
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring at or after {@code fromIndex},
     *         or an empty OptionalInt if the substring is not found, either parameter is {@code null}, or {@code fromIndex >= str.length()}
     * @see #of(String, String)
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOf(String, String, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt of(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code str}.
     * Both ASCII and Unicode characters are compared case-insensitively.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case),
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOfIgnoreCase(String, String)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case and starting from the specified index.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code str},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case) at or after {@code fromIndex},
     *         or an empty OptionalInt if the substring is not found, either parameter is {@code null}, or {@code fromIndex >= str.length()}
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOfIgnoreCase(String, String, int)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray}.
     * It's similar to {@link String#indexOf(String)} but for boolean arrays.
     * <p>
     * Example:
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false};
     * boolean[] sub = {true, true};
     * Index.ofSubArray(source, sub).get();  // returns 2
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are non-null, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceArray.length}, returns empty OptionalInt</li>
     * </ul>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified sub-list in the given source list.
     *
     * @param sourceList the list to be searched.
     * @param subListToFind the sub-list to find in the source array.
     * @return an OptionalInt containing the index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final List<?> subListToFind) {
        return ofSubList(sourceList, 0, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the specified sub-list in the given source list, starting from the specified index.
     *
     * @param sourceList the list to be searched.
     * @param fromIndex the index to start the search from.
     * @param subListToFind the sub-list to find in the source array.
     * @return an OptionalInt containing the index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind) {
        return ofSubList(sourceList, fromIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified sub-list in the given source list.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subListToFind} within {@code sourceList},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subListToFind}
     * starting at {@code startIndexOfSubList}. Uses {@code equals()} for element comparison.
     * <p>
     * The implementation is optimized for {@code RandomAccess} lists. For non-RandomAccess lists,
     * it converts sublists to arrays for comparison.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both lists are non-null, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either list is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceList.size()}, returns empty OptionalInt</li>
     * </ul>
     *
     * @param sourceList the list to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subListToFind the sub-list to search for, may be {@code null}
     * @param startIndexOfSubList the starting index within {@code subListToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subListToFind}
     * @return an OptionalInt containing the zero-based index where the sub-list is found,
     *         or an empty OptionalInt if the sub-list is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubList} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subListToFind}
     * @see #ofSubList(List, List)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(sourceList);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceList == null || subListToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceList == null || subListToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (!N.equals(sourceList.get(k), subListToFind.get(j))) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return ofSubArray(sourceList.subList(fromIndex, sourceList.size()).toArray(), 0,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
        }
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final char[] a, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final char[] a, final char valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final short[] a, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final short[] a, final short valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final int[] a, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final int[] a, final int valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final long[] a, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final long[] a, final long valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final float[] a, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final float[] a, final float valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final double[] a, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified double value in the given array, within a specified tolerance.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of a value
     * that falls within the range {@code [valueToFind - tolerance, valueToFind + tolerance]}.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return an OptionalInt containing the zero-based index of the last occurrence of a value within tolerance,
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @see #last(double[], double, double, int)
     * @see #last(double[], double)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance));
    }

    /**
     * Returns the last index of the specified value in the given array within a specified tolerance, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param tolerance the tolerance within which to find the value.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array within the specified tolerance, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given collection.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @return an OptionalInt containing the last index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given collection, starting from the specified index from the end.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @param startIndexFromBack the index to start the search from the end of the collection.
     * @return an OptionalInt containing the last index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified character in the given string.
     *
     * @param str the string to be searched.
     * @param charValueToFind the character value to find in the string.
     * @return an OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind));
    }

    /**
     * Returns the last index of the specified character in the given string, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param charValueToFind the character value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified string in the given string.
     *
     * @param str the string to be searched.
     * @param valueToFind the string value to find in the string.
     * @return an OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt last(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind));
    }

    /**
     * Returns the last index of the specified string in the given string, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param valueToFind the string value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOf(String, String, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt last(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive backwards search for {@code valueToFind} within {@code str},
     * starting from the end of the string. Both ASCII and Unicode characters are compared case-insensitively.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring (ignoring case),
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #lastOfIgnoreCase(String, String, int)
     * @see Strings#lastIndexOfIgnoreCase(String, String)
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the last index of the specified string in the given string, ignoring case considerations, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param valueToFind the string value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOfIgnoreCase(String, String, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, searching backwards from a specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are non-null,
     *       returns {@code min(startIndexFromBack, sourceArray.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code sourceArray.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the position to start the backwards search from; the search includes this position
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".lastIndexOf("") = 3
        // "aaa".lastIndexOf("", 0) = 0
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to find in the source array.
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified sub-list in the given source list.
     *
     * @param sourceList the list to be searched.
     * @param subListToFind the sub-list to find in the source list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        return lastOfSubList(sourceList, N.size(sourceList), subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the last index of the specified sub-list in the given source list, starting from the specified index from the end.
     *
     * @param sourceList the list to be searched.
     * @param startIndexFromBack the index to start the search from the end of the list.
     * @param subListToFind the sub-list to find in the source list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind) {
        return lastOfSubList(sourceList, startIndexFromBack, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the last index of the specified sub-list in the given source list, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the sub-list starting from <i>startIndexOfSubList</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceList the list to be searched.
     * @param startIndexFromBack the index to start the search from the end of the list.
     * @param subListToFind the sub-list to find in the source list.
     * @param startIndexOfSubList the starting index of the sub-list to be found.
     * @param sizeToMatch the number of elements to match from the sub-list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubList</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(sourceList);

        if (sizeToMatch == 0) {
            if (sourceList == null || subListToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceList == null || subListToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++) {
                    if (!N.equals(sourceList.get(k), subListToFind.get(j++))) {
                        break;
                    } else if (j == endIndexOfTargetSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(startIndexFromBack, len - sizeToMatch) + sizeToMatch).toArray(), startIndexFromBack,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
        }
    }

    /**
     * Returns the indices of all occurrences of the specified boolean value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit in the BitSet corresponds to an index where the value was found.
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value in the array;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(boolean[], boolean, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final char[] a, final char valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final char[] a, final char valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final short[] a, final short valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final short[] a, final short valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final int[] a, final int valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final int[] a, final int valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final long[] a, final long valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final long[] a, final long valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final float[] a, final float valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final float[] a, final float valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Float.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final double[] a, final double valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Double.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified double value in the given array, within a specified tolerance.
     * <p>
     * This method finds all positions where a value falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} and returns them as a BitSet.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return a BitSet containing the zero-based indices of all occurrences of values within tolerance;
     *         returns an empty BitSet if no values are found within tolerance or the array is {@code null} or empty
     * @see #allOf(double[], double, double, int)
     * @see #allOf(double[], double)
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance) {
        return allOf(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array within a specified tolerance, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param tolerance the tolerance within which matches will be found.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array within the specified tolerance, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        final double min = valueToFind - tolerance;
        final double max = valueToFind + tolerance;

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] >= min && a[i] <= max) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (N.equals(a[i], valueToFind)) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given collection.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @return a BitSet containing the indices of all occurrences of the value in the collection, or an empty BitSet if the value is not found or the input collection is {@code null}.
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind) {
        return allOf(c, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given collection, starting from the specified index.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the collection starting from the specified index, or an empty BitSet if the value is not found or the input collection is {@code null}.
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List<?> list && c instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (N.equals(list.get(idx), valueToFind)) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<?> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    bitSet.set(idx);
                }

                idx++;
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all elements in the given array that match the provided predicate.
     * <p>
     * This method tests each element in the array against the predicate and returns a BitSet
     * containing the indices of all elements for which the predicate returns {@code true}.
     * The predicate is not invoked for elements beyond a {@code null} array boundary.
     * <p>
     * Example:
     * <pre>{@code
     * String[] arr = {"apple", "banana", "avocado", "cherry"};
     * BitSet indices = Index.allOf(arr, s -> s.startsWith("a"));
     * // indices contains {0, 2} (positions of "apple" and "avocado")
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @return a BitSet containing the zero-based indices of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the array is {@code null} or empty
     * @see #allOf(Object[], Predicate, int)
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate) {
        return allOf(a, predicate, 0);
    }

    /**
     * Returns the indices of all elements in the given array that match the provided predicate, starting from the specified index.
     * <p>
     * This method tests each element in the array (starting from {@code fromIndex}) against the predicate
     * and returns a BitSet containing the indices of all elements for which the predicate returns {@code true}.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * @param <T> the type of elements in the array
     * @param a the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all elements at or after {@code fromIndex} matching the predicate;
     *         returns an empty BitSet if no elements match, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #allOf(Object[], Predicate)
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int idx = N.max(fromIndex, 0); idx < len; idx++) {
            if (predicate.test(a[idx])) {
                bitSet.set(idx);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all elements in the given collection that match the provided predicate.
     * <p>
     * This method tests each element in the collection (in iteration order) against the predicate
     * and returns a BitSet containing the indices of all elements for which the predicate returns {@code true}.
     * The implementation is optimized for {@code RandomAccess} lists.
     *
     * <p><b>Null Handling:</b></p>
     * <ul>
     *   <li>If {@code c} is {@code null}, returns empty BitSet</li>
     *   <li>If {@code predicate} is {@code null}, throws {@code NullPointerException}</li>
     *   <li>Null elements in the collection are passed to the predicate</li>
     * </ul>
     *
     * <p><b>Common Mistakes:</b></p>
     * <pre>{@code
     * // DON'T: Pass null predicate
     * Index.allOf(collection, null);  // NullPointerException!
     *
     * // DO: Provide valid predicate
     * Index.allOf(collection, Objects::nonNull);
     *
     * // DON'T: Assume predicate won't receive nulls
     * Index.allOf(Arrays.asList(1, null, 3), x -> x > 0);  // NPE inside predicate!
     *
     * // DO: Handle nulls in predicate
     * Index.allOf(Arrays.asList(1, null, 3), x -> x != null && x > 0);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param c the collection to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @return a BitSet containing the zero-based indices (in iteration order) of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the collection is {@code null} or empty
     * @see #allOf(Collection, Predicate, int)
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return allOf(c, predicate, 0);
    }

    /**
     * Returns the indices of all occurrences in the given collection for which the provided predicate returns {@code true}, starting from the specified index.
     *
     * @param <T> the type of the elements in the collection.
     * @param c the collection to be searched.
     * @param predicate the predicate to use to test the elements of the collection.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all elements in the collection for which the predicate returns {@code true} starting from the specified index, or an empty BitSet if no elements match or the input collection is {@code null}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List<? extends T> list && c instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (predicate.test(list.get(idx))) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (predicate.test(iter.next())) {
                    bitSet.set(idx);
                }

                idx++;
            }
        }

        return bitSet;
    }

    /**
     * Converts an integer index to an OptionalInt, treating negative values as "not found".
     * <p>
     * This is a helper method used internally by all index search methods. It converts the convention
     * of returning negative values (typically -1) for "not found" into an empty OptionalInt.
     *
     * @param index the index value; negative values indicate "not found"
     * @return an OptionalInt containing the index if non-negative, or an empty OptionalInt if negative
     */
    private static OptionalInt toOptionalInt(final int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }
}
