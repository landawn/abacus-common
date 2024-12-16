/*
 * Copyright (c) 2017, Haiyang Li.
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * The Median class is a utility class that provides methods for calculating the median of various types of data.
 * It includes methods for calculating the median of arrays and collections of various types, including integers, longs, doubles, and objects.
 * This class is final and cannot be instantiated.
 */
public final class Median {

    private Median() {
        // no instance.
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Character, OptionalChar> of(final char... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty"); //NOSONAR

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of characters to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Character, OptionalChar> of(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty"); //NOSONAR
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalChar.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalChar.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalChar.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalChar.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Character> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalChar.of(queue.poll())) : Pair.of(queue.peek(), OptionalChar.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Byte, OptionalByte> of(final byte... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of bytes to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Byte, OptionalByte> of(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalByte.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalByte.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalByte.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalByte.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Byte> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalByte.of(queue.poll())) : Pair.of(queue.peek(), OptionalByte.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Short, OptionalShort> of(final short... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of shorts to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Short, OptionalShort> of(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalShort.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalShort.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalShort.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalShort.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Short> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalShort.of(queue.poll())) : Pair.of(queue.peek(), OptionalShort.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     * <br />
     * The input array don't need to be sorted. in other words, there is no benefit even if the array is sorted.
     *
     * <pre>
     * <code>
     * Median.of(1); // -> [1, OptionalInt[]]
     * Median.of(1, 3); // -> [1, 3]
     * Median.of(1, 3, 5); // -> [3, OptionalInt[]]
     *
     * Median.of(1, 1); // -> [1, 1]
     * Median.of(1, 1, 3); // -> [1, OptionalInt[]]
     * Median.of(1, 1, 3, 5); // -> [1, 3]
     * Median.of(1, 1, 1, 3, 5); // -> [1, OptionalInt[]]
     * Median.of(1, 1, 1, 3, 3, 5); // -> [1, 3]
     * Median.of(1, 1, 1, 3, 3, 3, 5); // -> [3, OptionalInt[]]
     * </code>
     * </pre>
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see N#median(int[])
     */
    @SafeVarargs
    public static Pair<Integer, OptionalInt> of(final int... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of integers to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Integer, OptionalInt> of(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalInt.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalInt.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalInt.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalInt.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Integer> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalInt.of(queue.poll())) : Pair.of(queue.peek(), OptionalInt.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Long, OptionalLong> of(final long... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of longs to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Long, OptionalLong> of(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalLong.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalLong.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalLong.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalLong.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Long> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalLong.of(queue.poll())) : Pair.of(queue.peek(), OptionalLong.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Float, OptionalFloat> of(final float... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of floats to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Float, OptionalFloat> of(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalFloat.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalFloat.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalFloat.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalFloat.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Float> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalFloat.of(queue.poll())) : Pair.of(queue.peek(), OptionalFloat.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    @SafeVarargs
    public static Pair<Double, OptionalDouble> of(final double... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param a the array of doubles to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static Pair<Double, OptionalDouble> of(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], OptionalDouble.empty());
        } else if (len == 2) {
            return a[fromIndex] <= a[fromIndex + 1] ? Pair.of(a[fromIndex], OptionalDouble.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], OptionalDouble.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex), OptionalDouble.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<Double> queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    //noinspection DataFlowIssue
                    if (a[i] > queue.peek()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            //noinspection DataFlowIssue
            return len % 2 == 0 ? Pair.of(queue.poll(), OptionalDouble.of(queue.poll())) : Pair.of(queue.peek(), OptionalDouble.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param <T>
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final T[] a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param <T> the type of elements in the array.
     * @param a the array of elements to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @see #of(int[])
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final T[] a, final int fromIndex, final int toIndex) {
        return of(a, fromIndex, toIndex, Comparators.naturalOrder());
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (length / 2 + 1)} largest value,
     * and the {@code right} is the {@code (length / 2)} largest value if the length of array is even.
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array} is {@code null} or empty.
     * @see #of(int[])
     */
    public static <T> Pair<T, Nullable<T>> of(final T[] a, final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length, cmp);
    }

    /**
     * Returns a {@code Pair} with {@code left} as the {@code sizeOfRange / 2 + 1} largest value in the given range,
     * and the {@code right} as the {@code sizeOfRange / 2} largest value if the size of the given range is even.
     *
     * @param <T> the type of elements in the array.
     * @param a the array of elements to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @param cmp the comparator to determine the order of the elements.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(int[])
     */
    @SuppressWarnings("rawtypes")
    public static <T> Pair<T, Nullable<T>> of(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        cmp = cmp == null ? (Comparator) Comparators.naturalOrder() : cmp;

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], Nullable.empty());
        } else if (len == 2) {
            return cmp.compare(a[fromIndex], a[fromIndex + 1]) <= 0 ? Pair.of(a[fromIndex], Nullable.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], Nullable.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex, cmp), Nullable.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<T> queue = new PriorityQueue<>(k, cmp);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (cmp.compare(a[i], queue.peek()) > 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }

            return len % 2 == 0 ? Pair.of(queue.poll(), Nullable.of(queue.poll())) : Pair.of(queue.peek(), Nullable.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (size / 2 + 1)} largest value,
     * and the {@code right} is the {@code (size / 2)} largest value if the size of the collection is even.
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection of elements to find the median from.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the collection is even.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
     * @see #of(int[])
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final Collection<? extends T> c) {
        return of(c, Comparators.naturalOrder());
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (size / 2 + 1)} largest value,
     * and the {@code right} is the {@code (size / 2)} largest value if the size of the collection is even.
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection of elements to find the median from.
     * @param cmp the comparator to determine the order of the elements.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the collection is even.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
     * @see #of(int[])
     */
    @SuppressWarnings("rawtypes")
    public static <T> Pair<T, Nullable<T>> of(final Collection<? extends T> c, Comparator<? super T> cmp) {
        if (N.isEmpty(c)) {
            throw new IllegalArgumentException("The size of collection can't be null or empty");
        }

        cmp = cmp == null ? (Comparator) Comparators.naturalOrder() : cmp;

        final int len = c.size();

        if (len == 1) {
            return Pair.of(c.iterator().next(), Nullable.empty());
        } else if (len == 2) {
            final Iterator<? extends T> iter = c.iterator();
            final T first = iter.next();
            final T second = iter.next();
            return cmp.compare(first, second) <= 0 ? Pair.of(first, Nullable.of(second)) : Pair.of(second, Nullable.of(first));
        } else if (len == 3) {
            return Pair.of(N.median(c, cmp), Nullable.empty());
        } else {
            final int k = len / 2 + 1;
            final Queue<T> queue = new PriorityQueue<>(k, cmp);

            for (final T e : c) {
                if (queue.size() < k) {
                    queue.add(e);
                } else {
                    if (cmp.compare(e, queue.peek()) > 0) {
                        queue.remove();
                        queue.add(e);
                    }
                }
            }

            return len % 2 == 0 ? Pair.of(queue.poll(), Nullable.of(queue.poll())) : Pair.of(queue.peek(), Nullable.empty());
        }
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (size / 2 + 1)} largest value in the given range,
     * and the {@code right} is the {@code (size / 2)} largest value if the size of the given range is even.
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection of elements to find the median from.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the collection is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
     * @see #of(int[])
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return of(c, fromIndex, toIndex, Comparators.naturalOrder());
    }

    /**
     * Returns a {@code Pair} with {@code left} is the {@code (size / 2 + 1)} largest value in the given range,
     * and the {@code right} is the {@code (size / 2)} largest value if the size of the given range is even.
     *
     * @param <T> the type of elements in the collection.
     * @param fromIndex the starting index (inclusive) of the range to consider.
     * @param toIndex the ending index (exclusive) of the range to consider.
     * @param c the collection of elements to find the median from.
     * @param cmp the comparator to determine the order of the elements.
     * @return a {@code Pair} containing the median value and an optional second median value if the size of the given range is even.
     * @throws IndexOutOfBoundsException if the specified range is out of bounds.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
     * @see #of(int[])
     */
    public static <T> Pair<T, Nullable<T>> of(final Collection<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        if (N.isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty"); //NOSONAR
        }

        N.checkFromToIndex(fromIndex, toIndex, c.size());

        return of(N.slice(c, fromIndex, toIndex), cmp);
    }
}
