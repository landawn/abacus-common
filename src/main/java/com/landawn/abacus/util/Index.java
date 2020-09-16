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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import com.landawn.abacus.util.u.OptionalInt;

public final class Index {

    private static final OptionalInt NOT_FOUND = OptionalInt.empty();

    private Index() {
        // singleton.
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final boolean[] a, final boolean objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final boolean[] a, final int startIndex, final boolean objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final char[] a, final char objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final char[] a, final int startIndex, final char objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final byte[] a, final byte objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final byte[] a, final int startIndex, final byte objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final short[] a, final short objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final short[] a, final int startIndex, final short objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final int[] a, final int objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final int[] a, final int startIndex, final int objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final long[] a, final long objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final long[] a, final int startIndex, final long objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final float[] a, final float objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final float[] a, final int startIndex, final float objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final double[] a, final double objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final double[] a, final int startIndex, final double objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final Object[] a, final Object objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final Object[] a, final int startIndex, final Object objToFind) {
        return toOptionalInt(N.indexOf(a, startIndex, objToFind));
    }

    /**
     *
     * @param c
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final Collection<?> c, final Object objToFind) {
        return toOptionalInt(N.indexOf(c, objToFind));
    }

    /**
     *
     * @param c
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final Collection<?> c, final int startIndex, final Object objToFind) {
        return toOptionalInt(N.indexOf(c, startIndex, objToFind));
    }

    /**
     * 
     * @param iter
     * @param objToFind
     * @return
     */
    public static OptionalInt indexOf(final Iterator<?> iter, final Object objToFind) {
        return toOptionalInt(N.indexOf(iter, objToFind));
    }

    /**
     * 
     * @param iter
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt indexOf(final Iterator<?> iter, final int startIndex, final Object objToFind) {
        return toOptionalInt(N.indexOf(iter, startIndex, objToFind));
    }

    /**
     *
     * @param str
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final String str, final int objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, objToFind));
    }

    /**
     *
     * @param str
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final String str, final int startIndex, final int objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, startIndex, objToFind));
    }

    /**
     *
     * @param str
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final String str, final String objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, objToFind));
    }

    /**
     *
     * @param str
     * @param startIndex
     * @param objToFind
     * @return
     */
    public static OptionalInt of(final String str, final int startIndex, final String objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, startIndex, objToFind));
    }

    public static OptionalInt ofSubArray(final boolean[] sourceArray, final boolean[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int startIndex, final boolean[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int startIndex, final boolean[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final char[] sourceArray, final char[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final char[] sourceArray, final int startIndex, final char[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int startIndex, final char[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final byte[] sourceArray, final byte[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final byte[] sourceArray, final int startIndex, final byte[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int startIndex, final byte[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final short[] sourceArray, final short[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final short[] sourceArray, final int startIndex, final short[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int startIndex, final short[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final int[] sourceArray, final int[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final int[] sourceArray, final int startIndex, final int[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int startIndex, final int[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final long[] sourceArray, final long[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final long[] sourceArray, final int startIndex, final long[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int startIndex, final long[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final float[] sourceArray, final float[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final float[] sourceArray, final int startIndex, final float[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int startIndex, final float[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final double[] sourceArray, final double[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final double[] sourceArray, final int startIndex, final double[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int startIndex, final double[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubArray(final Object[] sourceArray, final Object[] targetSubArray) {
        return ofSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt ofSubArray(final Object[] sourceArray, final int startIndex, final Object[] targetSubArray) {
        return ofSubArray(sourceArray, startIndex, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Of sub array.
     *
     * @param sourceArray
     * @param startIndex
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int startIndex, final Object[] targetSubArray, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = N.max(startIndex, 0), maxFromIndex = len - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt ofSubList(final List<?> sourceList, final List<?> targetSubList) {
        return ofSubList(sourceList, 0, targetSubList, 0, N.size(targetSubList));
    }

    public static OptionalInt ofSubList(final List<?> sourceList, final int startIndex, final List<?> targetSubList) {
        return ofSubList(sourceList, startIndex, targetSubList, 0, N.size(targetSubList));
    }

    /**
     * Of sub list.
     *
     * @param sourceList
     * @param startIndex
     * @param targetSubList
     * @param beginIndexOfTargetSubList
     * @param size
     * @return
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int startIndex, final List<?> targetSubList, final int beginIndexOfTargetSubList,
            final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(targetSubList));

        final int len = N.size(sourceList);

        if (len == 0 || startIndex >= len || size == 0 || len - N.max(startIndex, 0) < size) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && targetSubList instanceof RandomAccess) {
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList + size;

            for (int i = startIndex, maxFromIndex = len - size; i <= maxFromIndex; i++) {
                for (int k = i, j = beginIndexOfTargetSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (N.equals(sourceList.get(k), targetSubList.get(j)) == false) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return ofSubArray(sourceList.subList(startIndex, sourceList.size()).toArray(), 0,
                    targetSubList.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final boolean[] a, final boolean objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final boolean[] a, final int startIndexFromBack, final boolean objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final char[] a, final char objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final char[] a, final int startIndexFromBack, final char objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final byte[] a, final byte objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final byte[] a, final int startIndexFromBack, final byte objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final short[] a, final short objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final short[] a, final int startIndexFromBack, final short objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final int[] a, final int objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final int[] a, final int startIndexFromBack, final int objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final long[] a, final long objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final long[] a, final int startIndexFromBack, final long objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final float[] a, final float objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final float[] a, final int startIndexFromBack, final float objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final double[] a, final double objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final double[] a, final int startIndexFromBack, final double objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param a
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final Object[] a, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    /**
     *
     * @param a
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final Object[] a, final int startIndexFromBack, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(a, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param list
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final List<?> list, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(list, objToFind));
    }

    /**
     *
     * @param list
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final List<?> list, final int startIndexFromBack, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(list, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param str
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final String str, final int objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, objToFind));
    }

    /**
     *
     * @param str
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final String str, final int startIndexFromBack, final int objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, startIndexFromBack, objToFind));
    }

    /**
     *
     * @param str
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final String str, final String objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, objToFind));
    }

    /**
     *
     * @param str
     * @param startIndexFromBack
     * @param objToFind
     * @return
     */
    public static OptionalInt last(final String str, final int startIndexFromBack, final String objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, startIndexFromBack, objToFind));
    }

    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final boolean[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final char[] sourceArray, final char[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final byte[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final short[] sourceArray, final short[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final long[] sourceArray, final long[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubArray[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final float[] sourceArray, final float[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final double[] sourceArray, final double[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final Object[] targetSubArray) {
        return lastOfSubArray(sourceArray, 0, targetSubArray, 0, N.len(targetSubArray));
    }

    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] targetSubArray) {
        return lastOfSubArray(sourceArray, startIndexFromBack, targetSubArray, 0, N.len(targetSubArray));
    }

    /**
     * Last of sub array.
     *
     * @param sourceArray
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubArray
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] targetSubArray,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubArray));

        final int len = N.len(sourceArray);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubArray[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return toOptionalInt(k);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    public static OptionalInt lastOfSubList(final List<?> sourceList, final List<?> targetSubList) {
        return lastOfSubList(sourceList, 0, targetSubList, 0, N.size(targetSubList));
    }

    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> targetSubList) {
        return lastOfSubList(sourceList, startIndexFromBack, targetSubList, 0, N.size(targetSubList));
    }

    /**
     * Last of sub list.
     *
     * @param sourceList
     * @param startIndexFromBack
     * @param targetSubArray
     * @param beginIndexOfTargetSubList
     * @param size
     * @return
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> targetSubArray,
            final int beginIndexOfTargetSubList, final int size) {
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(targetSubArray));

        final int len = N.size(sourceList);

        if (len == 0 || startIndexFromBack < 0 || size == 0 || size > startIndexFromBack) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && targetSubArray instanceof RandomAccess) {
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList - 1 + size;

            for (int i = N.min(startIndexFromBack, len - 1); i >= size - 1; i--) {
                for (int k = i, j = endIndexOfTargetSubList; j >= beginIndexOfTargetSubList; k--, j--) {
                    if (N.equals(sourceList.get(k), targetSubArray.get(j)) == false) {
                        break;
                    } else if (j == beginIndexOfTargetSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(startIndexFromBack, N.size(sourceList) - 1) + 1).toArray(), startIndexFromBack,
                    targetSubArray.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
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
