/*
 * Copyright (c) 2018, Haiyang Li.
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
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final boolean[] a, final int startIndex, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final char[] a, final char valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final char[] a, final int startIndex, final char valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final byte[] a, final int startIndex, final byte valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final short[] a, final short valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final short[] a, final int startIndex, final short valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final int[] a, final int valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final int[] a, final int startIndex, final int valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final long[] a, final long valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final long[] a, final int startIndex, final long valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final float[] a, final float valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final float[] a, final int startIndex, final float valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final double[] a, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final double[] a, final int startIndex, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @param tolerance
     * @return
     * @see N#indexOf(double[], double, double)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance) {
        return of(a, 0, valueToFind, tolerance);
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @param tolerance
     * @return
     * @see N#indexOf(double[], int, double, double)
     */
    public static OptionalInt of(final double[] a, int startIndex, final double valueToFind, final double tolerance) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind, tolerance));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Object[] a, final int startIndex, final Object valueToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, valueToFind));
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.indexOf(c, valueToFind));
    }

    /**
     *
     * @param c
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Collection<?> c, final int startIndex, final Object valueToFind) {
        return toOptionalInt(N.indexOf(c, startIndex, valueToFind));
    }

    /**
     *
     * @param iter
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind) {
        return toOptionalInt(N.indexOf(iter, valueToFind));
    }

    /**
     *
     * @param iter
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final Iterator<?> iter, final int startIndex, final Object valueToFind) {
        return toOptionalInt(N.indexOf(iter, startIndex, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final String str, final char valueToFind) {
        return toOptionalInt(Strings.indexOf(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final String str, final int startIndex, final char valueToFind) {
        return toOptionalInt(Strings.indexOf(str, startIndex, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final String str, final int startIndex, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(str, startIndex, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt ofIgnoreCase(final String str, final int startIndex, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, startIndex, valueToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int startIndex, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int startIndex, final boolean[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int startIndex, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int startIndex, final char[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int startIndex, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int startIndex, final byte[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int startIndex, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int startIndex, final short[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int startIndex, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int startIndex, final int[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int startIndex, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int startIndex, final long[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int startIndex, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int startIndex, final float[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int startIndex, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int startIndex, final double[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int startIndex, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, startIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int startIndex, final Object[] subArrayToFind, final int beginIndexOfTargetSubArray,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
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
     *
     *
     * @param sourceList
     * @param subListToFind
     * @return
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final List<?> subListToFind) {
        return ofSubList(sourceList, 0, subListToFind, 0, N.size(subListToFind));
    }

    /**
     *
     *
     * @param sourceList
     * @param startIndex
     * @param subListToFind
     * @return
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int startIndex, final List<?> subListToFind) {
        return ofSubList(sourceList, startIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Of sub list.
     *
     * @param sourceList
     * @param startIndex
     * @param subListToFind
     * @param beginIndexOfTargetSubList
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int startIndex, final List<?> subListToFind, final int beginIndexOfTargetSubList,
            final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(subListToFind));

        final int len = N.size(sourceList);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList + size;

            for (int i = startIndex, maxFromIndex = len - size; i <= maxFromIndex; i++) {
                for (int k = i, j = beginIndexOfTargetSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (!N.equals(sourceList.get(k), subListToFind.get(j))) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return ofSubArray(sourceList.subList(startIndex, sourceList.size()).toArray(), 0,
                    subListToFind.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final boolean[] a, final int startIndexFromBack, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final char[] a, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final char[] a, final int startIndexFromBack, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final byte[] a, final int startIndexFromBack, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final short[] a, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final short[] a, final int startIndexFromBack, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final int[] a, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final int[] a, final int startIndexFromBack, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final long[] a, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final long[] a, final int startIndexFromBack, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final float[] a, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final float[] a, final int startIndexFromBack, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final double[] a, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final double[] a, final int startIndexFromBack, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @param tolerance
     * @return
     * @see N#lastIndexOf(double[], double, double)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance) {
        return last(a, 0, valueToFind, tolerance);
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @param tolerance
     * @return
     * @see N#lastIndexOf(double[], int, double, double)
     */
    public static OptionalInt last(final double[] a, int startIndexFromBack, final double valueToFind, final double tolerance) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind, tolerance));
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final Object[] a, final int startIndexFromBack, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind));
    }

    /**
     *
     * @param c
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final Collection<?> c, final int startIndexFromBack, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(c, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final String str, final char valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final String str, final int startIndexFromBack, final char valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt last(final String str, final int startIndexFromBack, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, startIndexFromBack, valueToFind));
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind));
    }

    /**
     *
     * @param str
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final int startIndexFromBack, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, startIndexFromBack, valueToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceArray
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     *
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind,
            final int beginIndexOfTargetSubArray, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param sourceList
     * @param subListToFind
     * @return
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        return lastOfSubList(sourceList, 0, subListToFind, 0, N.size(subListToFind));
    }

    /**
     *
     *
     * @param sourceList
     * @param startIndexFromBack
     * @param subListToFind
     * @return
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind) {
        return lastOfSubList(sourceList, startIndexFromBack, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Last of sub list.
     *
     * @param sourceList
     * @param startIndexFromBack
     * @param subArrayToFind
     * @param beginIndexOfTargetSubList
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subArrayToFind,
            final int beginIndexOfTargetSubList, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(subArrayToFind));

        final int len = N.size(sourceList);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subArrayToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList - 1 + size;

            for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
                for (int k = i, j = endIndexOfTargetSubList; j >= beginIndexOfTargetSubList; k--, j--) {
                    if (!N.equals(sourceList.get(k), subArrayToFind.get(j))) {
                        break;
                    } else if (j == beginIndexOfTargetSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(startIndexFromBack, N.size(sourceList) - 1) + 1).toArray(), startIndexFromBack,
                    subArrayToFind.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet ({@code -1}).</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null}
     *  a input
     * @since 3.10
     */
    public static BitSet allOf(final boolean[] a, int startIndex, final boolean valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final byte[] a, int startIndex, final byte valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final char[] a, final char valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final char[] a, int startIndex, final char valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final short[] a, final short valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final short[] a, int startIndex, final short valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final int[] a, final int valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final int[] a, int startIndex, final int valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final long[] a, final long valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final long[] a, int startIndex, final long valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final float[] a, final float valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final float[] a, int startIndex, final float valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (Float.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value in the array.
     *
     * <p>This method returns empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, final double valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @return a BitSet of the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, int startIndex, final double valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (Double.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given value within a given tolerance in the array.
     *
     * <p>
     * This method will return all the indices of the value which fall between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance, each time between the nearest integers.
     * </p>
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance) {
        return allOf(a, 0, valueToFind, tolerance);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>
     * This method will return the indices of the values which fall between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance, between the nearest integers.
     * </p>
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @return a BitSet of the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, int startIndex, final double valueToFind, final double tolerance) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        final double min = valueToFind - tolerance;
        final double max = valueToFind + tolerance;

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] >= min && a[i] <= max) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given object in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind the object to find, may be {@code null}
     * @return a BitSet of all the indices of the object within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind) {
        return allOf(a, 0, valueToFind);
    }

    /**
     * Finds the indices of the given object in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind the object to find, may be {@code null}
     * @return a BitSet of all the indices of the object within the a starting at the index,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Object[] a, int startIndex, final Object valueToFind) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (N.equals(a[i], valueToFind)) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Finds the indices of the given object in the array.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * @param c
     * @param valueToFind the object to find, may be {@code null}
     * @return a BitSet of all the indices of the object within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind) {
        return allOf(c, 0, valueToFind);
    }

    /**
     * Finds the indices of the given object in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param c
     * @param startIndex the index to start searching at
     * @param valueToFind the object to find, may be {@code null}
     * @return a BitSet of all the indices of the object within the a starting at the index,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Collection<?> c, int startIndex, final Object valueToFind) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || startIndex >= size) {
            return bitSet;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<?> list = (List<?>) c;

            for (int idx = N.max(startIndex, 0); idx < size; idx++) {
                if (N.equals(list.get(idx), valueToFind)) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<?> iter = c.iterator();
            int idx = 0;

            while (idx < startIndex) {
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
     *
     * @param <T>
     * @param a
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate) {
        return allOf(a, 0, predicate);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param startIndex
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final T[] a, int startIndex, final Predicate<? super T> predicate) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return bitSet;
        }

        for (int idx = N.max(startIndex, 0); idx < len; idx++) {
            if (predicate.test(a[idx])) {
                bitSet.set(idx);
            }
        }

        return bitSet;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return allOf(c, 0, predicate);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param startIndex
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, int startIndex, final Predicate<? super T> predicate) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || startIndex >= size) {
            return bitSet;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends T> list = (List<? extends T>) c;

            for (int idx = N.max(startIndex, 0); idx < size; idx++) {
                if (predicate.test(list.get(idx))) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < startIndex) {
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
    private static OptionalInt toOptionalInt(int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }
}
