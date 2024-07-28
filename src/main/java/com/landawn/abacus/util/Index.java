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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final char[] a, final char valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final short[] a, final short valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final int[] a, final int valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final long[] a, final long valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final float[] a, final float valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param fromIndex
     * @param valueToFind
     * @return
     */
    public static OptionalInt of(final double[] a, final int fromIndex, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
        return of(a, valueToFind, tolerance, 0);
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @param tolerance
     * @param fromIndex
     * @return
     * @see N#indexOf(double[], double, double, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance, int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, tolerance, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(c, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(iter, valueToFind, fromIndex));
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @return
     */
    public static OptionalInt of(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind));
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final String str, final int charValueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt of(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, valueToFind, fromIndex));
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
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind, fromIndex));
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subArrayToFind
     * @return
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param fromIndex
     * @param subArrayToFind
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param fromIndex
     * @param subListToFind
     * @return
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind) {
        return ofSubList(sourceList, fromIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Of sub list.
     *
     * @param sourceList
     * @param fromIndex
     * @param subListToFind
     * @param startIndexOfSubList
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(sourceList);

        if (len == 0 || fromIndex >= len || sizeToMatch == 0 || len - N.max(fromIndex, 0) < sizeToMatch) {
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final char[] a, final char valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final short[] a, final short valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final int[] a, final int valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final long[] a, final long valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final float[] a, final float valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
        return last(a, valueToFind, tolerance, 0);
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @param tolerance
     * @param startIndexFromBack
     * @return
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance, int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind, startIndexFromBack));
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @return
     */
    public static OptionalInt last(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind));
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final String str, final int charValueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt last(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind, startIndexFromBack));
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
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind, startIndexFromBack));
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubArray
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray - 1 + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= startIndexOfSubArray; k--, j--) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == startIndexOfSubArray) {
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
     * @param startIndexOfSubList
     * @param sizeToMatch
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subArrayToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subArrayToFind));

        final int len = N.size(sourceList);

        if (len == 0 || startIndexFromBack < 0 || sizeToMatch == 0 || sizeToMatch > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subArrayToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList - 1 + sizeToMatch;

            for (int i = N.min(startIndexFromBack, len - 1); i >= sizeToMatch - 1; i--) {
                for (int k = i, j = endIndexOfTargetSubList; j >= startIndexOfSubList; k--, j--) {
                    if (!N.equals(sourceList.get(k), subArrayToFind.get(j))) {
                        break;
                    } else if (j == startIndexOfSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(startIndexFromBack, N.size(sourceList) - 1) + 1).toArray(), startIndexFromBack,
                    subArrayToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet ({@code -1}).</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null}
     *  a input
     * @since 3.10
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final char[] a, final char valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final short[] a, final short valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final int[] a, final int valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final long[] a, final long valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final float[] a, final float valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given value in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param fromIndex the index to start searching at
     * @return a BitSet of the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, final double valueToFind, int fromIndex) {
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
        return allOf(a, valueToFind, tolerance, 0);
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
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @param fromIndex the index to start searching at
     * @return a BitSet of the indices of the value within the a,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance, int fromIndex) {
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
        return allOf(a, valueToFind, 0);
    }

    /**
     * Finds the indices of the given object in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param a the a to search through for the object, may be {@code null}
     * @param valueToFind the object to find, may be {@code null}
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the object within the a starting at the index,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind, int fromIndex) {
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
        return allOf(c, valueToFind, 0);
    }

    /**
     * Finds the indices of the given object in the a starting at the given index.
     *
     * <p>This method returns an empty BitSet for a {@code null} input array.</p>
     *
     * <p>A negative fromIndex is treated as zero. A fromIndex larger than the a
     * length will return an empty BitSet.</p>
     *
     * @param c
     * @param valueToFind the object to find, may be {@code null}
     * @param fromIndex the index to start searching at
     * @return a BitSet of all the indices of the object within the a starting at the index,
     *  an empty BitSet if not found or {@code null} a input
     * @since 3.10
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind, int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<?> list = (List<?>) c;

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
     *
     * @param <T>
     * @param a
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate) {
        return allOf(a, predicate, 0);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param predicate
     * @param fromIndex
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate, int fromIndex) {
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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return allOf(c, predicate, 0);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param predicate
     * @param fromIndex
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate, int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends T> list = (List<? extends T>) c;

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
    private static OptionalInt toOptionalInt(int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }
}
