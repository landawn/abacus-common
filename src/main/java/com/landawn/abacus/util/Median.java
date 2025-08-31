/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * <p>A utility class that provides efficient methods to find the median value(s) of non-sorted arrays and collections
 * using a priority queue-based algorithm that avoids full sorting operations.</p>
 * 
 * <p>The median represents the middle value in a sorted sequence. For sequences with an odd number of elements, 
 * there's exactly one median value. For sequences with an even number of elements, there are two median values
 * (the two middle elements of the sorted sequence).</p>
 * 
 * <p>All methods in this class return the median as a {@code Pair} object:</p>
 * <ul>
 *   <li>For sequences with an odd number of elements, the {@code left} component of the pair contains the 
 *       median value, and the {@code right} component is empty.</li>
 *   <li>For sequences with an even number of elements, the {@code left} component contains the smaller of 
 *       the two median values, and the {@code right} component contains the larger one.</li>
 * </ul>
 * 
 * <p>For primitive arrays, specialized Optional classes ({@code OptionalInt}, {@code OptionalLong}, etc.) 
 * are used to represent the second median value. For object arrays and collections, {@code Optional<T>} is used.</p>
 * 
 * <p>The input arrays or collections do not need to be sorted beforehand. The implementation efficiently
 * finds the median without fully sorting the input data using a min-heap of size k = (length / 2) + 1.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // With array of odd length
 * Pair&lt;Integer, OptionalInt&gt; result1 = Median.of(1, 3, 5);
 * // result1: [3, OptionalInt.empty()]
 * 
 * // With array of even length
 * Pair&lt;Integer, OptionalInt&gt; result2 = Median.of(1, 3, 5, 7);
 * // result2: [3, OptionalInt.of(5)]
 * 
 * // With repeated values
 * Pair&lt;Integer, OptionalInt&gt; result3 = Median.of(1, 1, 3, 5);
 * // result3: [1, OptionalInt.of(3)]
 * 
 * // With collections and custom comparators
 * List&lt;String&gt; words = Arrays.asList("apple", "banana", "cherry");
 * Pair&lt;String, Optional&lt;String&gt;&gt; result4 = Median.of(words);
 * // result4: ["banana", Optional.empty()]
 * </pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe as it contains only static methods and 
 * maintains no state. Multiple threads can safely call these methods concurrently.</p>
 * 
 * <p><strong>Performance:</strong> Time complexity is O(n) where n is the number of elements, 
 * using a bounded priority queue instead of full sorting which would be O(n log n).</p>
 * 
 * @see Pair
 * @see OptionalInt
 * @see OptionalLong
 * @see OptionalDouble
 * @see OptionalChar
 * @see OptionalByte
 * @see OptionalShort
 * @see OptionalFloat
 * @see Optional
 */
public final class Median {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * This class is designed to be used only through its static methods.
     */
    private Median() {
        // no instance.
    }

    /**
     * Finds the median value(s) from an array of characters using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that avoids full sorting.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Character, OptionalChar&gt; median = Median.of('z', 'a', 'm');
     * char medianValue = median.left; // 'm'
     * </pre>
     * 
     * <pre>
     * <code>
     * Median.of('a'); // -> ['a', OptionalChar.empty()]
     * Median.of('a', 'c'); // -> ['a', OptionalChar.of('c')]
     * Median.of('a', 'c', 'b'); // -> ['b', OptionalChar.empty()]
     * Median.of('a', 'c', 'b', 'd'); // -> ['b', OptionalChar.of('c')]
     * </code>
     * </pre>
     *
     * @param a the array of characters to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(char[], int, int)
     * @see N#median(char[])
     */
    public static Pair<Character, OptionalChar> of(final char... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty"); //NOSONAR

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of characters defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and uses an efficient priority queue-based algorithm.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * char[] chars = {'z', 'a', 'm', 'x'};
     * Pair&lt;Character, OptionalChar&gt; median = Median.of(chars, 1, 3);
     * // Considers only 'a' and 'm', returns ['a', OptionalChar.of('m')]
     * </pre>
     * 
     * <pre>
     * <code>
     * Median.of(new char[]{'a', 'b', 'c'}, 0, 1); // -> ['a', OptionalChar.empty()]
     * Median.of(new char[]{'a', 'b', 'c'}, 0, 2); // -> ['a', OptionalChar.of('b')]
     * Median.of(new char[]{'a', 'b', 'c'}, 0, 3); // -> ['b', OptionalChar.empty()]
     * Median.of(new char[]{'a', 'b', 'c', 'd'}, 0, 4); // -> ['b', OptionalChar.of('c')]
     * </code>
     * </pre>
     *
     * @param a the array of characters to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(char...)
     * @see N#median(char[], int, int)
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
     * Finds the median value(s) from an array of bytes using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that processes the array in O(n) time complexity.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Byte, OptionalByte&gt; median = Median.of((byte)10, (byte)5, (byte)15);
     * byte medianValue = median.left; // 10
     * </pre>
     *
     * @param a the array of bytes to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(byte[], int, int)
     */
    public static Pair<Byte, OptionalByte> of(final byte... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of bytes defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and uses an efficient priority queue-based algorithm that maintains
     * only the necessary elements for median calculation.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * byte[] bytes = {30, 10, 20, 40};
     * Pair&lt;Byte, OptionalByte&gt; median = Median.of(bytes, 1, 3);
     * // Considers only bytes[1] and bytes[2]: 10, 20. Returns [10, OptionalByte.of(20)]
     * </pre>
     *
     * @param a the array of bytes to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(byte...)
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
     * Finds the median value(s) from an array of short integers using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that processes the array in linear time while maintaining only the necessary elements
     * for median calculation.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Short, OptionalShort&gt; median = Median.of((short)100, (short)50, (short)200);
     * short medianValue = median.left; // 100
     * </pre>
     *
     * @param a the array of short integers to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(short[], int, int)
     */
    public static Pair<Short, OptionalShort> of(final short... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of short integers defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and efficiently computes the median using a bounded priority queue
     * that avoids the overhead of full array sorting.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * short[] values = {300, 100, 200, 400};
     * Pair&lt;Short, OptionalShort&gt; median = Median.of(values, 1, 3);
     * // Considers only values[1] and values[2]: 100, 200. Returns [100, OptionalShort.of(200)]
     * </pre>
     *
     * @param a the array of short integers to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(short...)
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
     * Finds the median value(s) from an array of integers using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that achieves O(n) time complexity by maintaining a bounded heap of size (length/2 + 1)
     * instead of performing a full O(n log n) sort operation.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Integer, OptionalInt&gt; median = Median.of(10, 5, 20, 15);
     * int lowerMedian = median.left; // 10
     * int upperMedian = median.right.get(); // 15
     * </pre>
     *
     * @param a the array of integers to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(int[], int, int)
     */
    public static Pair<Integer, OptionalInt> of(final int... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of integers defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and uses an efficient selection algorithm based on a priority queue
     * that maintains only the smallest (length/2 + 1) elements during processing.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * int[] numbers = {100, 50, 75, 25, 90};
     * Pair&lt;Integer, OptionalInt&gt; median = Median.of(numbers, 1, 4);
     * // Considers only numbers[1], numbers[2], numbers[3]: 50, 75, 25. Returns [50, OptionalInt.empty()]
     * </pre>
     *
     * @param a the array of integers to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(int...)
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
     * Finds the median value(s) from an array of long integers using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that processes large integer values while maintaining optimal performance characteristics.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Long, OptionalLong&gt; median = Median.of(1000L, 500L, 1500L, 750L);
     * long lowerMedian = median.left; // 750
     * long upperMedian = median.right.get(); // 1000
     * </pre>
     *
     * @param a the array of long integers to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(long[], int, int)
     */
    public static Pair<Long, OptionalLong> of(final long... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of long integers defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and uses an efficient selection algorithm that maintains a min-heap
     * of the k smallest elements where k = (length/2 + 1).</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * long[] values = {3000L, 1000L, 2000L, 4000L, 1500L};
     * Pair&lt;Long, OptionalLong&gt; median = Median.of(values, 1, 4);
     * // Considers values[1], values[2], values[3]: 1000, 2000, 4000. Returns [2000, OptionalLong.empty()]
     * </pre>
     *
     * @param a the array of long integers to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(long...)
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
     * Finds the median value(s) from an array of float values using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that handles floating-point values correctly, including proper handling of NaN values
     * according to IEEE 754 floating-point standards.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Float, OptionalFloat&gt; median = Median.of(10.5f, 5.2f, 20.8f);
     * float medianValue = median.left; // 10.5f
     * </pre>
     *
     * @param a the array of float values to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(float[], int, int)
     */
    public static Pair<Float, OptionalFloat> of(final float... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of float values defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and efficiently computes the median using a bounded priority queue
     * while properly handling floating-point comparison semantics.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * float[] values = {30.5f, 10.2f, 20.8f, 40.1f};
     * Pair&lt;Float, OptionalFloat&gt; median = Median.of(values, 1, 3);
     * // Considers values[1] and values[2]: 10.2f, 20.8f. Returns [10.2f, OptionalFloat.of(20.8f)]
     * </pre>
     *
     * @param a the array of float values to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(float...)
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
     * Finds the median value(s) from an array of double values using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that handles double-precision floating-point values with full IEEE 754 compliance,
     * including proper treatment of special values like NaN and infinity.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Pair&lt;Double, OptionalDouble&gt; median = Median.of(10.5, 5.2, 20.8, 15.1);
     * double lowerMedian = median.left; // 10.5
     * double upperMedian = median.right.get(); // 15.1
     * </pre>
     *
     * @param a the array of double values to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(double[], int, int)
     */
    public static Pair<Double, OptionalDouble> of(final double... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of double values defined by the specified range using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex) and uses an efficient selection algorithm that maintains optimal
     * precision for double-precision floating-point arithmetic operations.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * double[] values = {30.5, 10.2, 20.8, 40.1, 15.3};
     * Pair&lt;Double, OptionalDouble&gt; median = Median.of(values, 1, 4);
     * // Considers values[1], values[2], values[3]: 10.2, 20.8, 40.1. Returns [20.8, OptionalDouble.empty()]
     * </pre>
     *
     * @param a the array of double values to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(double...)
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
     * Finds the median value(s) from an array of Comparable objects using their natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input array does not need to be pre-sorted.
     * This method uses an efficient priority queue-based algorithm that works with any type implementing
     * the Comparable interface.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * String[] words = {"apple", "banana", "cherry"};
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words);
     * String medianWord = median.left; // "banana"
     * </pre>
     *
     * @param <T> the type of elements in the array, which must implement Comparable
     * @param a the array of Comparable objects to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(Object[], int, int)
     * @see #of(Object[], Comparator)
     */
    public static <T extends Comparable<? super T>> Pair<T, Optional<T>> of(final T[] a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length);
    }

    /**
     * Finds the median value(s) from a subarray of Comparable objects defined by the specified range using their natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input array does not need to be pre-sorted.
     * This method operates on a contiguous subarray defined by the range [fromIndex, toIndex) and delegates
     * to the comparator-based version using the natural order comparator.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * String[] words = {"zebra", "apple", "banana", "cherry"};
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words, 1, 4);
     * // Considers words[1], words[2], words[3]: "apple", "banana", "cherry". Returns ["banana", Optional.empty()]
     * </pre>
     *
     * @param <T> the type of elements in the array, which must implement Comparable
     * @param a the array of Comparable objects to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Object[])
     * @see #of(Object[], int, int, Comparator)
     */
    public static <T extends Comparable<? super T>> Pair<T, Optional<T>> of(final T[] a, final int fromIndex, final int toIndex) {
        return of(a, fromIndex, toIndex, Comparators.naturalOrder());
    }

    /**
     * Finds the median value(s) from an array of objects using a custom comparator for ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order
     * according to the provided comparator. The input array does not need to be pre-sorted. This method
     * uses an efficient priority queue-based algorithm that works with any custom comparison logic,
     * allowing for flexible median calculation on objects that may not implement Comparable or when
     * a different ordering than natural order is desired.</p>
     * 
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * String[] words = {"apple", "pie", "banana"};
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left; // "pie" (middle length)
     * </pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array of objects to find the median from. Must not be null or empty.
     * @param cmp the comparator to use for ordering the elements. If null, natural ordering is used.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or empty
     * @see #of(Object[], int, int, Comparator)
     * @see #of(Object[])
     */
    public static <T> Pair<T, Optional<T>> of(final T[] a, final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");

        return of(a, 0, a.length, cmp);
    }

    /**
     * Finds the median value(s) from a subarray of objects defined by the specified range using a custom comparator for ordering.
     * 
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order
     * according to the provided comparator. The input array does not need to be pre-sorted. This method
     * operates on a contiguous subarray defined by the range [fromIndex, toIndex) and uses an efficient
     * selection algorithm based on a bounded priority queue that maintains only the necessary elements.</p>
     * 
     * <p>The algorithm optimizes performance by using a min-heap of size k = (length/2 + 1) to track
     * only the smaller half of the elements plus one, avoiding the need to sort the entire subarray.</p>
     * 
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * String[] words = {"elephant", "ant", "bee", "tiger"};
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words[1], words[2], words[3]: "ant", "bee", "tiger". Returns ["bee", Optional.empty()]
     * </pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array of objects to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @param cmp the comparator to use for ordering the elements. If null, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Object[], Comparator)
     * @see #of(Object[], int, int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Pair<T, Optional<T>> of(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");
        }

        N.checkFromToIndex(fromIndex, toIndex, a.length);

        cmp = cmp == null ? (Comparator) Comparators.naturalOrder() : cmp;

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(a[fromIndex], Optional.empty());
        } else if (len == 2) {
            return cmp.compare(a[fromIndex], a[fromIndex + 1]) <= 0 ? Pair.of(a[fromIndex], Optional.of(a[fromIndex + 1]))
                    : Pair.of(a[fromIndex + 1], Optional.of(a[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.median(a, fromIndex, toIndex, cmp), Optional.empty());
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

            return len % 2 == 0 ? Pair.of(queue.poll(), Optional.of(queue.poll())) : Pair.of(queue.peek(), Optional.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of characters using natural ordering.
     * 
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method uses an efficient priority queue-based
     * algorithm that avoids full sorting.</p>
     * 
     * <p>For collections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * List&lt;Integer&gt; numbers = Arrays.asList(10, 5, 20, 15, 25);
     * Pair&lt;Integer, Optional&lt;Integer&gt;&gt; median = Median.of(numbers);
     * int medianValue = median.left; // 15
     * </pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable
     * @param c the collection of Comparable objects to find the median from. Must not be null or empty.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is null or empty
     * @see #of(Collection, Comparator)
     * @see #of(Collection, int, int)
     */
    public static <T extends Comparable<? super T>> Pair<T, Optional<T>> of(final Collection<? extends T> c) {
        return of(c, Comparators.naturalOrder());
    }

    /**
     * Finds the median value(s) from a collection of objects using a custom comparator for ordering.
     * 
     * <p>The median represents the middle value(s) when the collection elements are arranged in sorted order
     * according to the provided comparator. The input collection does not need to be sorted. This method
     * uses an efficient priority queue-based algorithm that works with any custom comparison logic,
     * providing flexibility for objects that may not implement Comparable or when a different ordering
     * than natural order is desired.</p>
     * 
     * <p>The algorithm maintains optimal performance by using a min-heap of size k = (size/2 + 1) to track
     * only the smaller half of the elements plus one, iterating through the collection exactly once.</p>
     * 
     * <p>For collections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For collections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * Set&lt;String&gt; words = new HashSet&lt;&gt;(Arrays.asList("apple", "pie", "banana"));
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left; // Word with median length
     * </pre>
     *
     * @param <T> the type of elements in the collection
     * @param c the collection of objects to find the median from. Must not be null or empty.
     * @param cmp the comparator to use for ordering the elements. If null, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is null or empty
     * @see #of(Collection)
     * @see #of(Collection, int, int, Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Pair<T, Optional<T>> of(final Collection<? extends T> c, Comparator<? super T> cmp) {
        if (N.isEmpty(c)) {
            throw new IllegalArgumentException("The size of collection can't be null or empty");
        }

        cmp = cmp == null ? (Comparator) Comparators.naturalOrder() : cmp;

        final int len = c.size();

        if (len == 1) {
            return Pair.of(c.iterator().next(), Optional.empty());
        } else if (len == 2) {
            final Iterator<? extends T> iter = c.iterator();
            final T first = iter.next();
            final T second = iter.next();
            return cmp.compare(first, second) <= 0 ? Pair.of(first, Optional.of(second)) : Pair.of(second, Optional.of(first));
        } else if (len == 3) {
            return Pair.of(N.median(c, cmp), Optional.empty());
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

            return len % 2 == 0 ? Pair.of(queue.poll(), Optional.of(queue.poll())) : Pair.of(queue.peek(), Optional.empty());
        }
    }

    /**
     * Finds the median value(s) from a subcollection of Comparable objects defined by the specified range using their natural ordering.
     * 
     * <p>The median represents the middle value(s) when the subcollection elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input collection does not need to be sorted.
     * This method extracts a slice of the collection defined by the range [fromIndex, toIndex) and delegates
     * to the comparator-based version using the natural order comparator.</p>
     * 
     * <p>The method efficiently handles the range extraction by using a slice operation that creates a view
     * of the specified portion of the collection without copying all elements unnecessarily.</p>
     * 
     * <p>For subcollections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subcollections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * List&lt;Integer&gt; numbers = Arrays.asList(100, 50, 75, 25, 90);
     * Pair&lt;Integer, Optional&lt;Integer&gt;&gt; median = Median.of(numbers, 1, 4);
     * // Considers elements at indices 1, 2, 3: 50, 75, 25. Returns [50, Optional.empty()]
     * </pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable
     * @param c the collection of Comparable objects to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the size of the collection, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Collection)
     * @see #of(Collection, int, int, Comparator)
     */
    public static <T extends Comparable<? super T>> Pair<T, Optional<T>> of(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return of(c, fromIndex, toIndex, Comparators.naturalOrder());
    }

    /**
     * Finds the median value(s) from a subcollection of objects defined by the specified range using a custom comparator for ordering.
     * 
     * <p>The median represents the middle value(s) when the subcollection elements are arranged in sorted order
     * according to the provided comparator. The input collection does not need to be sorted. This method
     * extracts a slice of the collection defined by the range [fromIndex, toIndex) and then applies the
     * median-finding algorithm to the resulting subcollection.</p>
     * 
     * <p>The method first validates the input parameters and range bounds, then creates a slice view of the
     * collection using the specified indices. This approach is memory-efficient as it avoids copying
     * elements that are not part of the target range.</p>
     * 
     * <p>For subcollections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     * 
     * <p>For subcollections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     * 
     * <p>Usage example:</p>
     * <pre>
     * List&lt;String&gt; words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
     * Pair&lt;String, Optional&lt;String&gt;&gt; median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words at indices 1, 2, 3: "ant", "bee", "tiger". Returns ["bee", Optional.empty()]
     * </pre>
     *
     * @param <T> the type of elements in the collection
     * @param c the collection of objects to find the median from. Must not be null.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @param cmp the comparator to use for ordering the elements. If null, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is null or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the size of the collection, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Collection, Comparator)
     * @see #of(Collection, int, int)
     */
    public static <T> Pair<T, Optional<T>> of(final Collection<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        if (N.isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection cannot be null or empty"); //NOSONAR
        }

        N.checkFromToIndex(fromIndex, toIndex, c.size());

        return of(N.slice(c, fromIndex, toIndex), cmp);
    }
}
