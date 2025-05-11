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
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final char[] a, final char valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final char[] a, final char valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final short[] a, final short valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final short[] a, final short valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final int[] a, final int valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final int[] a, final int valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final long[] a, final long valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final long[] a, final long valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final float[] a, final float valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final float[] a, final float valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a
     * @param valueToFind
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final double[] a, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final double[] a, final int fromIndex, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array, within a given tolerance.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which to find the value.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object)
     * @see N#indexOf(double[], double, double)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance) {
        return of(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the index of the specified value in the array, within a given tolerance and starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which to find the value.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #of(Object[], Object, int)
     * @see N#indexOf(double[], double, double, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, tolerance, fromIndex));
    }

    /**
     * Returns the index of the specified value in the array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the specified value in the array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the collection.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @return An OptionalInt containing the index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.indexOf(c, valueToFind));
    }

    /**
     * Returns the index of the specified value in the collection, starting from the specified index.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(c, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified value in the iterator.
     *
     * @param iter The iterator to be searched.
     * @param valueToFind The value to find in the iterator.
     * @return An OptionalInt containing the index of the value in the iterator, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind) {
        return toOptionalInt(N.indexOf(iter, valueToFind));
    }

    /**
     * Returns the index of the specified value in the iterator, starting from the specified index.
     *
     * @param iter The iterator to be searched.
     * @param valueToFind The value to find in the iterator.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the value in the iterator, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(iter, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified character in the string.
     *
     * @param str The string to be searched.
     * @param charValueToFind The character to find in the string.
     * @return An OptionalInt containing the index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#indexOf(String, int)
     * @see String#indexOf(int)
     * @see String#indexOf(int, int)
     */
    public static OptionalInt of(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind));
    }

    /**
     * Returns the index of the specified character in the string, starting from the specified index.
     *
     * @param str The string to be searched.
     * @param charValueToFind The character to find in the string.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#indexOf(String, int, int)
     * @see String#indexOf(int)
     * @see String#indexOf(int, int)
     */
    public static OptionalInt of(final String str, final int charValueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified string in the given string.
     *
     * @param str The string to be searched.
     * @param valueToFind The string to find in the main string.
     * @return An OptionalInt containing the index of the string in the main string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#indexOf(String, String)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt of(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(str, valueToFind));
    }

    /**
     * Returns the index of the specified string in the given string, starting from the specified index.
     *
     * @param str The string to be searched.
     * @param valueToFind The string to find in the main string.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the string in the main string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#indexOf(String, String, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt of(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified string in the given string, ignoring case considerations.
     *
     * @param str The string to be searched.
     * @param valueToFind The string to find in the main string, ignoring case considerations.
     * @return An OptionalInt containing the index of the string in the main string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#indexOfIgnoreCase(String, String)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the index of the specified string in the given string, ignoring case considerations and starting from the specified index.
     *
     * @param str The string to be searched.
     * @param valueToFind The string to find in the main string, ignoring case considerations.
     * @param fromIndex The index to start the search from.
     * @return An OptionalInt containing the index of the string in the main string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#indexOfIgnoreCase(String, String, int)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     *
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param fromIndex The index to start the search from.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceList The list to be searched.
     * @param subListToFind The sub-list to find in the source array.
     * @return An OptionalInt containing the index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
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
     * @param sourceList The list to be searched.
     * @param fromIndex The index to start the search from.
     * @param subListToFind The sub-list to find in the source array.
     * @return An OptionalInt containing the index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind) {
        return ofSubList(sourceList, fromIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the specified sub-list in the given source list, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the sub-list starting from <i>startIndexOfSubList</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceList The list to be searched.
     * @param fromIndex The index to start the search from.
     * @param subListToFind The sub-list to find in the source list.
     * @param startIndexOfSubList The starting index of the sub-list to be found.
     * @param sizeToMatch The number of elements to match from the sub-list.
     * @return An OptionalInt containing the index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubList</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subListToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
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

            for (int i = fromIndex, maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
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
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final char[] a, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final char[] a, final char valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final short[] a, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final short[] a, final short valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final int[] a, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final int[] a, final int valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final long[] a, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final long[] a, final long valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final float[] a, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final float[] a, final float valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final double[] a, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array within a specified tolerance.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which to find the value.
     * @return An OptionalInt containing the last index of the value in the array within the specified tolerance, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance) {
        return last(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the last index of the specified value in the given array within a specified tolerance, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which to find the value.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array within the specified tolerance, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @return An OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given collection.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @return An OptionalInt containing the last index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given collection, starting from the specified index from the end.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @param startIndexFromBack The index to start the search from the end of the collection.
     * @return An OptionalInt containing the last index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified character in the given string.
     *
     * @param str The string to be searched.
     * @param charValueToFind The character value to find in the string.
     * @return An OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind));
    }

    /**
     * Returns the last index of the specified character in the given string, starting from the specified index from the end.
     *
     * @param str The string to be searched.
     * @param charValueToFind The character value to find in the string.
     * @param startIndexFromBack The index to start the search from the end of the string.
     * @return An OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified string in the given string.
     *
     * @param str The string to be searched.
     * @param valueToFind The string value to find in the string.
     * @return An OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
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
     * @param str The string to be searched.
     * @param valueToFind The string value to find in the string.
     * @param startIndexFromBack The index to start the search from the end of the string.
     * @return An OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOf(String, String, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt last(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified string in the given string, ignoring case considerations.
     *
     * @param str The string to be searched.
     * @param valueToFind The string value to find in the string.
     * @return An OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOfIgnoreCase(String, String)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the last index of the specified string in the given string, ignoring case considerations, starting from the specified index from the end.
     *
     * @param str The string to be searched.
     * @param valueToFind The string value to find in the string.
     * @param startIndexFromBack The index to start the search from the end of the string.
     * @return An OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceArray The array to be searched.
     * @param startIndexFromBack The index to start the search from the end of the array.
     * @param subArrayToFind The subarray to find in the source array.
     * @param startIndexOfSubArray The starting index of the subarray to be found.
     * @param sizeToMatch The number of elements to match from the subarray.
     * @return An OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
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
     * @param sourceList The list to be searched.
     * @param subListToFind The sub-list to find in the source list.
     * @return An OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        return lastOfSubList(sourceList, N.size(sourceList), subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the last index of the specified sub-list in the given source list, starting from the specified index from the end.
     *
     * @param sourceList The list to be searched.
     * @param startIndexFromBack The index to start the search from the end of the list.
     * @param subListToFind The sub-list to find in the source list.
     * @return An OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
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
     * @param sourceList The list to be searched.
     * @param startIndexFromBack The index to start the search from the end of the list.
     * @param subListToFind The sub-list to find in the source list.
     * @param startIndexOfSubList The starting index of the sub-list to be found.
     * @param sizeToMatch The number of elements to match from the sub-list.
     * @return An OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final char[] a, final char valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final short[] a, final short valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final int[] a, final int valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final long[] a, final long valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final float[] a, final float valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final double[] a, final double valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array within a specified tolerance.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which matches will be found.
     * @return A BitSet containing the indices of all occurrences of the value in the array within the specified tolerance, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance) {
        return allOf(a, valueToFind, tolerance, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array within a specified tolerance, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param tolerance The tolerance within which matches will be found.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array within the specified tolerance, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a The array to be searched.
     * @param valueToFind The value to find in the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences of the specified value in the given collection.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @return A BitSet containing the indices of all occurrences of the value in the collection, or an empty BitSet if the value is not found or the input collection is {@code null}.
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind) {
        return allOf(c, valueToFind, 0);
    }

    /**
     * Finds and returns the indices of all occurrences of the specified value in the given collection, starting from the specified index.
     *
     * @param c The collection to be searched.
     * @param valueToFind The value to find in the collection.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all occurrences of the value in the collection starting from the specified index, or an empty BitSet if the value is not found or the input collection is {@code null}.
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

            while (idx < fromIndex) {
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
     * Finds and returns the indices of all occurrences in the given array for which the provided predicate returns {@code true}.
     *
     * @param <T> The type of the elements in the array.
     * @param a The array to be searched.
     * @param predicate The predicate to use to test the elements of the array.
     * @return A BitSet containing the indices of all elements in the array for which the predicate returns {@code true}, or an empty BitSet if no elements match or the input array is {@code null}.
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate) {
        return allOf(a, predicate, 0);
    }

    /**
     * Finds and returns the indices of all occurrences in the given array for which the provided predicate returns {@code true}, starting from the specified index.
     *
     * @param <T> The type of the elements in the array.
     * @param a The array to be searched.
     * @param predicate The predicate to use to test the elements of the array.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all elements in the array for which the predicate returns {@code true} starting from the specified index, or an empty BitSet if no elements match or the input array is {@code null}.
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
     * Finds and returns the indices of all occurrences in the given collection for which the provided predicate returns {@code true}.
     *
     * @param <T> The type of the elements in the collection.
     * @param c The collection to be searched.
     * @param predicate The predicate to use to test the elements of the collection.
     * @return A BitSet containing the indices of all elements in the collection for which the predicate returns {@code true}, or an empty BitSet if no elements match or the input collection is {@code null}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return allOf(c, predicate, 0);
    }

    /**
     * Finds and returns the indices of all occurrences in the given collection for which the provided predicate returns {@code true}, starting from the specified index.
     *
     * @param <T> The type of the elements in the collection.
     * @param c The collection to be searched.
     * @param predicate The predicate to use to test the elements of the collection.
     * @param fromIndex The index to start the search from.
     * @return A BitSet containing the indices of all elements in the collection for which the predicate returns {@code true} starting from the specified index, or an empty BitSet if no elements match or the input collection is {@code null}.
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

            while (idx < fromIndex) {
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
     * To optional int.
     *
     * @param index
     * @return
     */
    private static OptionalInt toOptionalInt(final int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }
}
