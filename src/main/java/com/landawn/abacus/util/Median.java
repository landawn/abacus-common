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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Finds the median element (or the two middle elements) of an array, a collection, or a range of either, without
 * modifying the input. Ranges of four or more elements are copied and the copy is sorted, costing O(n log n) time
 * and O(n) auxiliary space; shorter ranges are resolved directly (see <b>Algorithm</b> below). The caller's data is
 * never touched either way.
 *
 * <p><b>What is returned.</b> A {@link Pair}. For an odd number of elements, {@code left} is the single median and
 * {@code right} is empty. For an even number, {@code left} is the lower median and {@code right} the upper - which may
 * compare equal to it. Primitive overloads pair a boxed {@code left} with a primitive optional {@code right}
 * ({@link OptionalInt}, {@link OptionalDouble}, ...), so only the lower median is ever boxed.
 *
 * <p><b>This is not the statistical median of a numeric input.</b> This class reports the two middle <i>elements</i>;
 * for the conventional arithmetic mean of them, as a {@code double}, use {@link N#median(int[])} and its siblings
 * rather than averaging the pair by hand. For the lower median alone, unwrapped, use {@link N#lowerMedian(int[])}.
 *
 * <p><b>An empty {@code right} means odd length, including inputs with {@code null} elements.</b> The
 * {@code left} component is a bare value and is {@code null} when the lower median element is; a {@code null}
 * <i>upper</i> median is represented by a present {@link Nullable}, even when its value is null.
 * The right component is empty exactly when the selected input has odd size, so parity remains unambiguous.
 *
 * <p><b>Empty and null inputs are rejected</b> with {@link IllegalArgumentException}; a range outside the input raises
 * {@link IndexOutOfBoundsException}, which is checked before the emptiness check. A {@code null} comparator is always
 * rejected - ahead of every other check except in {@link #of(Object[], Comparator)}, which validates the array first
 * and so reports the empty-input failure for a {@code null} or empty array. How {@code null} <i>elements</i> are
 * ordered is entirely the comparator's business - the natural-order overloads use a null-first comparator, so a
 * {@code null} element sorts below every other value.
 *
 * <p><b>Floating point.</b> {@code float} and {@code double} inputs are ordered by {@link Float#compare(float, float)}
 * / {@link Double#compare(double, double)}: {@code NaN} sorts above every other value including positive infinity, and
 * {@code 0.0} sorts above {@code -0.0}. No arithmetic is performed on the input, so nothing overflows and no precision
 * is lost.
 *
 * <p><b>Algorithm.</b> Ranges of four or more elements are copied and sorted, then read at the middle position(s).
 * Shorter ranges are resolved without sorting: the primitive overloads compare directly and allocate nothing, while
 * the object overloads delegate their three-element case to {@link N#lowerMedian}, which selects through a small
 * bounded heap (falling back to copy-and-sort when the range contains {@code null}) and so does allocate. Collection
 * overloads always copy first, so the result never depends on a {@code size()} the iterator disagrees with. The
 * selected range or collection is read exactly once.
 *
 * <p><b>Tie handling.</b> When several elements compare equal at a median position, <i>which</i> of those equal
 * elements is returned is unspecified. Ranges of one, two, and four-or-more elements all return the element a stable
 * sort would place there. Only the <i>three</i>-element range can differ: it delegates to {@link N#lowerMedian}, whose
 * tie-break is an original-position heuristic rather than a stable sort. This is observable only for
 * equal-but-distinct instances.
 *
 * <p>All methods are static and hold no shared state, so they are safe to call concurrently - provided the caller does
 * not mutate the input meanwhile and any shared comparator is itself thread-safe.
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Even length: both middle elements
 * int[] numbers = {5, 2, 8, 1, 9, 3};
 * Pair<Integer, OptionalInt> result = Median.of(numbers);
 * result.left();          // 3
 * result.right().get();   // 5
 *
 * // Odd length: right is empty
 * Median.of(numbers, 1, 4);   // (2, OptionalInt.empty) - considers indices 1..3
 *
 * // Collections, natural order
 * List<String> words = Arrays.asList("zebra", "apple", "banana", "cherry");
 * Median.of(words).left();   // "banana"
 *
 * // Custom ordering
 * Median.of(words, Comparator.comparing(String::length));
 *
 * // The conventional statistical median of a numeric input
 * double[] data = {23.1, 45.7, 12.3, 67.8};
 * double middle = N.median(data);
 * }</pre>
 *
 * @see Pair
 * @see OptionalInt
 * @see OptionalLong
 * @see OptionalDouble
 * @see OptionalChar
 * @see OptionalByte
 * @see OptionalShort
 * @see OptionalFloat
 * @see Nullable
 * @see Comparator
 * @see Collection
 * @see N#median(int[])
 * @see N#lowerMedian(int[])
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
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Character, OptionalChar> median = Median.of('z', 'a', 'm');
     * char medianValue = median.left();  // returns 'm'
     * }</pre>
     *
     * @param source the array of characters to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(char[], int, int)
     * @see Pair
     * @see OptionalChar
     * @see N#lowerMedian(char[])
     */
    public static Pair<Character, OptionalChar> of(final char... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty"); //NOSONAR

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of characters defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'z', 'a', 'm', 'x'};
     * Pair<Character, OptionalChar> median = Median.of(chars, 1, 3);
     * // Considers only 'a' and 'm', returns ['a', OptionalChar.of('m')]
     * }</pre>
     *
     * @param source the array of characters to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(char...)
     * @see N#lowerMedian(char[], int, int)
     */
    public static Pair<Character, OptionalChar> of(final char[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1"); //NOSONAR
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalChar.empty());
        } else if (len == 2) {
            return source[fromIndex] <= source[fromIndex + 1] ? Pair.of(source[fromIndex], OptionalChar.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalChar.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalChar.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final char[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalChar.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalChar.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of bytes using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Byte, OptionalByte> median = Median.of((byte)10, (byte)5, (byte)15);
     * byte medianValue = median.left();  // returns 10
     * }</pre>
     *
     * @param source the array of bytes to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(byte[], int, int)
     * @see Pair
     * @see OptionalByte
     * @see N#lowerMedian(byte[])
     */
    public static Pair<Byte, OptionalByte> of(final byte... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of bytes defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = {30, 10, 20, 40};
     * Pair<Byte, OptionalByte> median = Median.of(bytes, 1, 3);
     * // Considers only bytes[1] and bytes[2]: 10, 20. Returns [10, OptionalByte.of(20)]
     * }</pre>
     *
     * @param source the array of bytes to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(byte...)
     * @see N#lowerMedian(byte[], int, int)
     */
    public static Pair<Byte, OptionalByte> of(final byte[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalByte.empty());
        } else if (len == 2) {
            return source[fromIndex] <= source[fromIndex + 1] ? Pair.of(source[fromIndex], OptionalByte.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalByte.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalByte.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final byte[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalByte.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalByte.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of short integers using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Short, OptionalShort> median = Median.of((short)100, (short)50, (short)200);
     * short medianValue = median.left();  // returns 100
     * }</pre>
     *
     * @param source the array of short integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(short[], int, int)
     * @see Pair
     * @see OptionalShort
     * @see N#lowerMedian(short[])
     */
    public static Pair<Short, OptionalShort> of(final short... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of short integers defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {300, 100, 200, 400};
     * Pair<Short, OptionalShort> median = Median.of(values, 1, 3);
     * // Considers only values[1] and values[2]: 100, 200. Returns [100, OptionalShort.of(200)]
     * }</pre>
     *
     * @param source the array of short integers to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(short...)
     * @see N#lowerMedian(short[], int, int)
     */
    public static Pair<Short, OptionalShort> of(final short[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalShort.empty());
        } else if (len == 2) {
            return source[fromIndex] <= source[fromIndex + 1] ? Pair.of(source[fromIndex], OptionalShort.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalShort.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalShort.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final short[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalShort.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalShort.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of integers using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Integer, OptionalInt> median = Median.of(10, 5, 20, 15);
     * int lowerMedian = median.left();          // returns 10
     * int upperMedian = median.right().get();   // returns 15
     * }</pre>
     *
     * @param source the array of integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(int[], int, int)
     * @see Pair
     * @see OptionalInt
     * @see N#lowerMedian(int[])
     */
    public static Pair<Integer, OptionalInt> of(final int... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of integers defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {100, 50, 75, 25, 90};
     * Pair<Integer, OptionalInt> median = Median.of(numbers, 1, 4);
     * // Considers only numbers[1], numbers[2], numbers[3]: 50, 75, 25. Returns [50, OptionalInt.empty()]
     * }</pre>
     *
     * @param source the array of integers to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(int...)
     * @see N#lowerMedian(int[], int, int)
     */
    public static Pair<Integer, OptionalInt> of(final int[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalInt.empty());
        } else if (len == 2) {
            return source[fromIndex] <= source[fromIndex + 1] ? Pair.of(source[fromIndex], OptionalInt.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalInt.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalInt.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final int[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalInt.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalInt.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of long integers using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Long, OptionalLong> median = Median.of(1000L, 500L, 1500L, 750L);
     * long lowerMedian = median.left();          // returns 750
     * long upperMedian = median.right().get();   // returns 1000
     * }</pre>
     *
     * @param source the array of long integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(long[], int, int)
     * @see Pair
     * @see OptionalLong
     * @see N#lowerMedian(long[])
     */
    public static Pair<Long, OptionalLong> of(final long... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of long integers defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] values = {3000L, 1000L, 2000L, 4000L, 1500L};
     * Pair<Long, OptionalLong> median = Median.of(values, 1, 4);
     * // Considers values[1], values[2], values[3]: 1000, 2000, 4000. Returns [2000, OptionalLong.empty()]
     * }</pre>
     *
     * @param source the array of long integers to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(long...)
     * @see N#lowerMedian(long[], int, int)
     */
    public static Pair<Long, OptionalLong> of(final long[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalLong.empty());
        } else if (len == 2) {
            return source[fromIndex] <= source[fromIndex + 1] ? Pair.of(source[fromIndex], OptionalLong.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalLong.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalLong.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final long[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalLong.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalLong.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of float values using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.
     * Elements are compared using {@link Float#compare(float, float)}, which imposes a total ordering in which
     * {@code NaN} is considered greater than all other values and {@code 0.0f} is considered greater than {@code -0.0f}.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Float, OptionalFloat> median = Median.of(10.5f, 5.2f, 20.8f);
     * float medianValue = median.left();  // returns 10.5f
     * }</pre>
     *
     * @param source the array of float values to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(float[], int, int)
     * @see Pair
     * @see OptionalFloat
     * @see N#lowerMedian(float[])
     */
    public static Pair<Float, OptionalFloat> of(final float... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of float values defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.
     * Elements are compared using {@link Float#compare(float, float)}, which imposes a total ordering in which
     * {@code NaN} is considered greater than all other values and {@code 0.0f} is considered greater than {@code -0.0f}.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] values = {30.5f, 10.2f, 20.8f, 40.1f};
     * Pair<Float, OptionalFloat> median = Median.of(values, 1, 3);
     * // Considers values[1] and values[2]: 10.2f, 20.8f. Returns [10.2f, OptionalFloat.of(20.8f)]
     * }</pre>
     *
     * @param source the array of float values to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(float...)
     * @see N#lowerMedian(float[], int, int)
     */
    public static Pair<Float, OptionalFloat> of(final float[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalFloat.empty());
        } else if (len == 2) {
            return Float.compare(source[fromIndex], source[fromIndex + 1]) <= 0 ? Pair.of(source[fromIndex], OptionalFloat.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalFloat.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalFloat.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final float[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalFloat.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalFloat.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of double values using natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method copies the array and sorts the
     * copy, running in O(n log n) time without modifying the input.
     * Elements are compared using {@link Double#compare(double, double)}, which imposes a total ordering in which
     * {@code NaN} is considered greater than all other values (including positive infinity) and {@code 0.0d} is
     * considered greater than {@code -0.0d}.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Double, OptionalDouble> median = Median.of(10.5, 5.2, 20.8, 15.1);
     * double lowerMedian = median.left();          // returns 10.5
     * double upperMedian = median.right().get();   // returns 15.1
     * }</pre>
     *
     * @param source the array of double values to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(double[], int, int)
     * @see Pair
     * @see OptionalDouble
     * @see N#lowerMedian(double[])
     */
    public static Pair<Double, OptionalDouble> of(final double... source) throws IllegalArgumentException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
    }

    /**
     * Finds the median value(s) from a subarray of double values defined by the specified range using natural ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order.
     * The input array does not need to be pre-sorted. This method operates on a contiguous subarray defined
     * by the range [fromIndex, toIndex); the range is copied and the copy is sorted, leaving the input unmodified.
     * Elements are compared using {@link Double#compare(double, double)}, which imposes a total ordering in which
     * {@code NaN} is considered greater than all other values (including positive infinity) and {@code 0.0d} is
     * considered greater than {@code -0.0d}.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {30.5, 10.2, 20.8, 40.1, 15.3};
     * Pair<Double, OptionalDouble> median = Median.of(values, 1, 4);
     * // Considers values[1], values[2], values[3]: 10.2, 20.8, 40.1. Returns [20.8, OptionalDouble.empty()]
     * }</pre>
     *
     * @param source the array of double values to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @see #of(double...)
     * @see N#lowerMedian(double[], int, int)
     */
    public static Pair<Double, OptionalDouble> of(final double[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return Pair.of(source[fromIndex], OptionalDouble.empty());
        } else if (len == 2) {
            return Double.compare(source[fromIndex], source[fromIndex + 1]) <= 0 ? Pair.of(source[fromIndex], OptionalDouble.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], OptionalDouble.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex), OptionalDouble.empty());
        } else {
            // Copy and sort rather than retain a bounded min-heap of len/2+1 *boxed* values: sorting a
            // primitive copy allocates less, runs faster, and orders by the same total ordering.
            final double[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], OptionalDouble.of(copy[len / 2])) : Pair.of(copy[len / 2], OptionalDouble.empty());
        }
    }

    /**
     * Finds the median value(s) from an array of Comparable objects using their natural ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input array does not need to be pre-sorted.
     * The selected range is copied and the copy is sorted, so the input array is not modified.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"apple", "banana", "cherry"};
     * Pair<String, Nullable<String>> median = Median.of(words);
     * String medianWord = median.left();  // returns "banana"
     * }</pre>
     *
     * @param <T> the type of elements in the array, which must implement {@code Comparable}.
     * @param source the array of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Comparable[], int, int)
     * @see #of(Object[], Comparator)
     * @see N#lowerMedian(Comparable[])
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final T[] source) throws IllegalArgumentException, ClassCastException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");

        return of(source, 0, source.length);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"zebra", "apple", "banana", "cherry"};
     * Pair<String, Nullable<String>> median = Median.of(words, 1, 4);
     * // Considers words[1], words[2], words[3]: "apple", "banana", "cherry". Returns ["banana", Nullable.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the array, which must implement {@code Comparable}.
     * @param source the array of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Comparable[])
     * @see #of(Object[], int, int, Comparator)
     * @see N#lowerMedian(Comparable[], int, int)
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final T[] source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        return of(source, fromIndex, toIndex, Comparators.naturalOrder());
    }

    /**
     * Finds the median value(s) from an array of objects using a custom comparator for ordering.
     *
     * <p>The median represents the middle value(s) when the array elements are arranged in sorted order
     * according to the provided comparator. The input array does not need to be pre-sorted. This method
     * copies the array and sorts the copy with the supplied comparator, allowing median calculation on objects
     * that may not implement Comparable or when a different ordering than natural order is desired.</p>
     *
     * <p><strong>Note:</strong> {@code cmp} must not be {@code null}; an {@code IllegalArgumentException} is thrown if it is.
     * A custom comparator that does not handle nulls may throw {@code NullPointerException} when the input
     * contains {@code null} elements.</p>
     *
     * <p>For arrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For arrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"apple", "pie", "banana"};
     * Pair<String, Nullable<String>> median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left();  // returns "apple" (middle length)
     * }</pre>
     *
     * @param <T> the type of elements in the array.
     * @param source the array of objects to find the median from. Must not be {@code null} or empty.
     * @param cmp the comparator used for ordering elements; must not be {@code null}.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, or if {@code cmp} is
     *         {@code null}.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Object[], int, int, Comparator)
     * @see #of(Comparable[])
     * @see N#lowerMedian(Object[], Comparator)
     */
    public static <T> Pair<T, Nullable<T>> of(final T[] source, final Comparator<? super T> cmp) throws IllegalArgumentException, ClassCastException {
        N.checkArgNotEmpty(source, "The specified array 'source' cannot be null or empty");
        N.checkArgNotNull(cmp, cs.cmp);

        return of(source, 0, source.length, cmp);
    }

    /**
     * Finds the median value(s) from a subarray of objects defined by the specified range using a custom comparator for ordering.
     *
     * <p>The median represents the middle value(s) when the subarray elements are arranged in sorted order
     * according to the provided comparator. The input array does not need to be pre-sorted. This method
     * operates on a contiguous subarray defined by the range [fromIndex, toIndex); the range is copied and the
     * copy is sorted, leaving the input array unmodified.</p>
     *
     * <p>The selected range is copied and the copy is sorted; the input array is not modified.</p>
     *
     * <p><strong>Note:</strong> {@code cmp} must not be {@code null}; an {@code IllegalArgumentException} is thrown if it is.
     * A custom comparator that does not handle nulls may throw {@code NullPointerException} when the input
     * contains {@code null} elements.</p>
     *
     * <p>For subarrays with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subarrays with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"elephant", "ant", "bee", "tiger"};
     * Pair<String, Nullable<String>> median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words[1], words[2], words[3]: "ant", "bee", "tiger". Returns ["bee", Nullable.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the array.
     * @param source the array of objects to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @param cmp the comparator used for ordering elements; must not be {@code null}.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty, if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1, or if {@code cmp} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   length of the array, or {@code fromIndex > toIndex}. The range is
     *                                   validated before the emptiness check, so an out-of-range index raises
     *                                   this rather than the empty-input {@code IllegalArgumentException}; a
     *                                   {@code null} {@code cmp} is still rejected first.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Object[], Comparator)
     * @see #of(Comparable[], int, int)
     * @see N#lowerMedian(Object[], int, int, Comparator)
     */
    public static <T> Pair<T, Nullable<T>> of(final T[] source, final int fromIndex, final int toIndex, final Comparator<? super T> cmp)
            throws IllegalArgumentException, IndexOutOfBoundsException, ClassCastException {
        N.checkArgNotNull(cmp, cs.cmp);
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source array is null/empty, or the range is empty: toIndex - fromIndex must be >= 1");
        }

        final int len = toIndex - fromIndex;

        if (len == 1) {
            // Nullable preserves a present null upper median separately from an absent odd-size counterpart.
            return Pair.of(source[fromIndex], Nullable.empty());
        } else if (len == 2) {
            return cmp.compare(source[fromIndex], source[fromIndex + 1]) <= 0 ? Pair.of(source[fromIndex], Nullable.of(source[fromIndex + 1]))
                    : Pair.of(source[fromIndex + 1], Nullable.of(source[fromIndex]));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(source, fromIndex, toIndex, cmp), Nullable.empty());
        } else {
            // One copy-and-sort for every range. The previous bounded min-heap could not hold null, so it had
            // to pre-scan for nulls, reading the range twice and then tie-breaking differently from its own
            // sort fallback. Sorting always is a single pass, is stable, and lets a null-tolerant comparator
            // order nulls like any other value.
            final T[] copy = N.copyOfRange(source, fromIndex, toIndex);
            N.sort(copy, cmp);

            return len % 2 == 0 ? Pair.of(copy[len / 2 - 1], Nullable.of(copy[len / 2])) : Pair.of(copy[len / 2], Nullable.empty());
        }
    }

    /**
     * Finds the median value(s) from a collection of Comparable objects using their natural ordering.
     *
     * <p>The median represents the middle value(s) when the collection elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input collection does not need to be sorted.
     * The collection is traversed once into a copy, which is then sorted; the input collection is not modified.</p>
     *
     * <p>For collections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For collections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(10, 5, 20, 15, 25);
     * Pair<Integer, Nullable<Integer>> median = Median.of(numbers);
     * int medianValue = median.left();  // returns 15
     * }</pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable.
     * @param source the collection of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Collection, Comparator)
     * @see #of(Collection, int, int)
     * @see N#lowerMedian(Collection)
     * @see Iterables#lowerMedian(Collection)
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final Collection<? extends T> source)
            throws IllegalArgumentException, ClassCastException {
        return of(source, Comparators.naturalOrder());
    }

    /**
     * Finds the median value(s) from a collection of objects using a custom comparator for ordering.
     *
     * <p>The median represents the middle value(s) when the collection elements are arranged in sorted order
     * according to the provided comparator. The input collection does not need to be sorted. This method
     * copies the collection and sorts the copy with the supplied comparator, providing flexibility for objects
     * that may not implement Comparable or when a different ordering than natural order is desired.</p>
     *
     * <p>The collection is traversed exactly once into a copy, which is then sorted; the input collection
     * is not modified.</p>
     *
     * <p><strong>Note:</strong> {@code cmp} must not be {@code null}; an {@code IllegalArgumentException} is thrown if it is.
     * A custom comparator that does not handle nulls may throw {@code NullPointerException} when the input
     * contains {@code null} elements.</p>
     *
     * <p>For collections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For collections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> words = new HashSet<>(Arrays.asList("apple", "pie", "banana"));
     * Pair<String, Nullable<String>> median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left();  // the word with the median length
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param source the collection of objects to find the median from. Must not be {@code null} or empty.
     * @param cmp the comparator used for ordering elements; must not be {@code null}.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty, or if {@code cmp} is
     *         {@code null}.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Collection)
     * @see #of(Collection, int, int, Comparator)
     * @see N#lowerMedian(Collection, Comparator)
     * @see Iterables#lowerMedian(Collection, Comparator)
     */
    public static <T> Pair<T, Nullable<T>> of(final Collection<? extends T> source, final Comparator<? super T> cmp)
            throws IllegalArgumentException, ClassCastException {
        N.checkArgNotNull(cmp, cs.cmp);

        if (N.isEmpty(source)) {
            throw new IllegalArgumentException("Source collection is null or empty");
        }

        final List<T> copy = new ArrayList<>(source);
        final int len = copy.size();

        if (len == 0) {
            // Reached only when the collection reported a non-zero size above but yielded no elements while
            // being copied - a concurrently shrinking collection, or one whose size() disagrees with its iterator.
            throw new IllegalArgumentException("Source collection yielded no elements while being copied");
        } else if (len == 1) {
            return Pair.of(copy.get(0), Nullable.empty());
        } else if (len == 2) {
            final T first = copy.get(0);
            final T second = copy.get(1);
            return cmp.compare(first, second) <= 0 ? Pair.of(first, Nullable.of(second)) : Pair.of(second, Nullable.of(first));
        } else if (len == 3) {
            return Pair.of(N.lowerMedian(copy, cmp), Nullable.empty());
        } else {
            copy.sort(cmp);

            return len % 2 == 0 ? Pair.of(copy.get(len / 2 - 1), Nullable.of(copy.get(len / 2))) : Pair.of(copy.get(len / 2), Nullable.empty());
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
     * Pair<Integer, Nullable<Integer>> median = Median.of(numbers, 1, 4);
     * // Considers elements at indices 1, 2, 3: 50, 75, 25. Returns [50, Nullable.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable.
     * @param source the collection of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   size of the collection, or {@code fromIndex > toIndex}. The range is
     *                                   validated first, so an out-of-range index always raises this rather
     *                                   than {@code IllegalArgumentException}.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty, or if the
     *         (in-range) {@code toIndex - fromIndex} is less than 1.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Collection)
     * @see #of(Collection, int, int, Comparator)
     * @see N#lowerMedian(Collection, int, int)
     */
    public static <T extends Comparable<? super T>> Pair<T, Nullable<T>> of(final Collection<? extends T> source, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        return of(source, fromIndex, toIndex, Comparators.naturalOrder());
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
     * <p><strong>Note:</strong> {@code cmp} must not be {@code null}; an {@code IllegalArgumentException} is thrown if it is.
     * A custom comparator that does not handle nulls may throw {@code NullPointerException} when the input
     * contains {@code null} elements.</p>
     *
     * <p>For subcollections with an odd number of elements, returns the single median value in the {@code left}
     * component of the pair, with the {@code right} component empty.</p>
     *
     * <p>For subcollections with an even number of elements, returns the two median values, with the smaller
     * value in the {@code left} component and the larger value in the {@code right} component.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
     * Pair<String, Nullable<String>> median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words at indices 1, 2, 3: "ant", "bee", "tiger". Returns ["bee", Nullable.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param source the collection of objects to find the median from. Must not be {@code null} or empty.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @param cmp the comparator used for ordering elements; must not be {@code null}.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty, if the (in-range)
     *         {@code toIndex - fromIndex} is less than 1, or if {@code cmp} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than the
     *                                   size of the collection, or {@code fromIndex > toIndex}. The range is
     *                                   validated before the emptiness check, so an out-of-range index raises
     *                                   this rather than the empty-input {@code IllegalArgumentException}; a
     *                                   {@code null} {@code cmp} is still rejected first.
     * @throws ClassCastException if the selected elements cannot be compared with each other by the chosen ordering
     * @see #of(Collection, Comparator)
     * @see #of(Collection, int, int)
     * @see N#lowerMedian(Collection, int, int, Comparator)
     */
    public static <T> Pair<T, Nullable<T>> of(final Collection<? extends T> source, final int fromIndex, final int toIndex, final Comparator<? super T> cmp)
            throws IllegalArgumentException, IndexOutOfBoundsException, ClassCastException {
        N.checkArgNotNull(cmp, cs.cmp);
        N.checkFromToIndex(fromIndex, toIndex, N.size(source));

        if (N.isEmpty(source) || fromIndex >= toIndex) {
            throw new IllegalArgumentException("Source collection is null/empty, or the range is empty: toIndex - fromIndex must be >= 1"); //NOSONAR
        }

        return of(N.slice(source, fromIndex, toIndex), cmp);
    }
}
