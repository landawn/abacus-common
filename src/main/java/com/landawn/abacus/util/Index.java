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

import java.util.List;
import java.util.RandomAccess;

import com.landawn.abacus.util.u.OptionalInt;

public final class Index {
    private static final OptionalInt NOT_FOUND = OptionalInt.empty();

    private Index() {
        // singleton.
    }

    public static OptionalInt of(final boolean[] a, final boolean objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final boolean[] a, final int fromIndex, final boolean objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final char[] a, final char objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final char[] a, final int fromIndex, final char objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final byte[] a, final byte objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final byte[] a, final int fromIndex, final byte objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final short[] a, final short objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final short[] a, final int fromIndex, final short objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final int[] a, final int objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final int[] a, final int fromIndex, final int objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final long[] a, final long objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final long[] a, final int fromIndex, final long objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final float[] a, final float objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final float[] a, final int fromIndex, final float objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final double[] a, final double objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final double[] a, final int fromIndex, final double objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final Object[] a, final Object objToFind) {
        return toOptionalInt(N.indexOf(a, objToFind));
    }

    public static OptionalInt of(final Object[] a, final int fromIndex, final Object objToFind) {
        return toOptionalInt(N.indexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt of(final List<?> list, final Object objToFind) {
        return toOptionalInt(N.indexOf(list, objToFind));
    }

    public static OptionalInt of(final List<?> list, final int fromIndex, final Object objToFind) {
        return toOptionalInt(N.indexOf(list, fromIndex, objToFind));
    }

    public static OptionalInt of(final String str, final int objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, objToFind));
    }

    public static OptionalInt of(final String str, final int fromIndex, final int objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, fromIndex, objToFind));
    }

    public static OptionalInt of(final String str, final String objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, objToFind));
    }

    public static OptionalInt of(final String str, final int fromIndex, final String objToFind) {
        return toOptionalInt(StringUtil.indexOf(str, fromIndex, objToFind));
    }

    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkIndex(fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || N.len(sourceArray) - fromIndex < size) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray + size;

        for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
            for (int k = i, j = beginIndexOfTargetSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return OptionalInt.of(i);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> targetSubList, final int beginIndexOfTargetSubList,
            final int size) {
        N.checkIndex(fromIndex, N.size(sourceList));
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(targetSubList));

        if (size == 0 || N.size(sourceList) - fromIndex < size) {
            OptionalInt.empty();
        }

        if (sourceList instanceof RandomAccess && targetSubList instanceof RandomAccess) {
            final int aLen = N.size(sourceList);
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList + size;

            for (int i = fromIndex, maxFromIndex = aLen - size; i <= maxFromIndex; i++) {
                for (int k = i, j = beginIndexOfTargetSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (N.equals(sourceList.get(k), targetSubList.get(j)) == false) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else {
            return ofSubArray(sourceList.subList(fromIndex, sourceList.size()).toArray(), 0,
                    targetSubList.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
    }

    public static OptionalInt last(final boolean[] a, final boolean objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final boolean[] a, final int fromIndex, final boolean objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final char[] a, final char objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final char[] a, final int fromIndex, final char objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final byte[] a, final byte objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final byte[] a, final int fromIndex, final byte objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final short[] a, final short objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final short[] a, final int fromIndex, final short objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final int[] a, final int objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final int[] a, final int fromIndex, final int objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final long[] a, final long objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final long[] a, final int fromIndex, final long objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final float[] a, final float objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final float[] a, final int fromIndex, final float objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final double[] a, final double objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final double[] a, final int fromIndex, final double objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final Object[] a, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(a, objToFind));
    }

    public static OptionalInt last(final Object[] a, final int fromIndex, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(a, fromIndex, objToFind));
    }

    public static OptionalInt last(final List<?> list, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(list, objToFind));
    }

    public static OptionalInt last(final List<?> list, final int fromIndex, final Object objToFind) {
        return toOptionalInt(N.lastIndexOf(list, fromIndex, objToFind));
    }

    public static OptionalInt last(final String str, final int objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, objToFind));
    }

    public static OptionalInt last(final String str, final int fromIndex, final int objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, fromIndex, objToFind));
    }

    public static OptionalInt last(final String str, final String objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, objToFind));
    }

    public static OptionalInt last(final String str, final int fromIndex, final String objToFind) {
        return toOptionalInt(StringUtil.lastIndexOf(str, fromIndex, objToFind));
    }

    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] targetSubList,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int fromIndex, final char[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int fromIndex, final byte[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int fromIndex, final short[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int fromIndex, final int[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int fromIndex, final long[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (sourceArray[k] != targetSubList[j]) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int fromIndex, final float[] targetSubList, final int beginIndexOfTargetSubArray,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int fromIndex, final double[] targetSubList,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int fromIndex, final Object[] targetSubList,
            final int beginIndexOfTargetSubArray, final int size) {
        N.checkFromToIndex(0, fromIndex, N.len(sourceArray));
        N.checkFromIndexSize(beginIndexOfTargetSubArray, size, N.len(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        final int aLen = N.len(sourceArray);
        final int endIndexOfTargetSubArray = beginIndexOfTargetSubArray - 1 + size;

        for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
            for (int k = i, j = endIndexOfTargetSubArray; j >= beginIndexOfTargetSubArray; k--, j--) {
                if (N.equals(sourceArray[k], targetSubList[j]) == false) {
                    break;
                } else if (j == beginIndexOfTargetSubArray) {
                    return OptionalInt.of(k);
                }
            }
        }

        return OptionalInt.empty();
    }

    public static OptionalInt lastOfSubList(final List<?> sourceList, final int fromIndex, final List<?> targetSubList, final int beginIndexOfTargetSubList,
            final int size) {
        N.checkFromToIndex(0, fromIndex, N.size(sourceList));
        N.checkFromIndexSize(beginIndexOfTargetSubList, size, N.size(targetSubList));

        if (size == 0 || fromIndex < size - 1) {
            OptionalInt.empty();
        }

        if (sourceList instanceof RandomAccess && targetSubList instanceof RandomAccess) {
            final int aLen = N.size(sourceList);
            final int endIndexOfTargetSubList = beginIndexOfTargetSubList - 1 + size;

            for (int i = N.min(fromIndex, aLen - 1); i >= size - 1; i--) {
                for (int k = i, j = endIndexOfTargetSubList; j >= beginIndexOfTargetSubList; k--, j--) {
                    if (N.equals(sourceList.get(k), targetSubList.get(j)) == false) {
                        break;
                    } else if (j == beginIndexOfTargetSubList) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(fromIndex, N.size(sourceList) - 1) + 1).toArray(), fromIndex,
                    targetSubList.subList(beginIndexOfTargetSubList, beginIndexOfTargetSubList + size).toArray(), 0, size);
        }
    }

    private static OptionalInt toOptionalInt(int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }

}
