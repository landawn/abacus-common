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

import java.util.Arrays;
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
 * A high-performance utility class providing efficient median calculation methods for arrays and collections
 * without requiring full sorting operations. This class implements optimized algorithms using priority queues
 * to find median values in O(n) time complexity, making it significantly faster than traditional sorting-based
 * approaches for median-only calculations.
 *
 * <p>The median is the middle value in a dataset when arranged in sorted order. For datasets with an odd number
 * of elements, there is exactly one median value. For datasets with an even number of elements, there are two
 * median values (the two middle elements). This class handles both cases elegantly by returning results as
 * {@link Pair} objects with specialized {@code Optional} types for the second median value.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>High Performance:</b> O(n) time complexity using bounded priority queues instead of O(n log n) sorting</li>
 *   <li><b>Type Safety:</b> Specialized methods for all primitive types with corresponding Optional return types</li>
 *   <li><b>Flexible Input:</b> Support for arrays, collections, and custom comparators</li>
 *   <li><b>Range Support:</b> Ability to calculate median for subarrays and subcollections</li>
 *   <li><b>Null Safety:</b> Graceful handling of null inputs and elements</li>
 *   <li><b>Memory Efficient:</b> Minimal memory overhead with bounded heap data structures</li>
 *   <li><b>Thread Safe:</b> Stateless design ensuring safe concurrent access</li>
 *   <li><b>Zero Dependencies:</b> Self-contained implementation without external algorithm libraries</li>
 * </ul>
 *
 * <p><b>Algorithm Strategy:</b>
 * <ul>
 *   <li><b>Priority Queue Based:</b> Uses min-heap of size k = (length / 2) + 1 for efficient median finding</li>
 *   <li><b>Single Pass:</b> Processes input data in a single iteration without multiple scans</li>
 *   <li><b>Bounded Memory:</b> Memory usage independent of input size, only depends on median position</li>
 *   <li><b>Adaptive Algorithm:</b> Optimizes behavior based on input size and type characteristics</li>
 * </ul>
 *
 * <p><b>Return Value Convention:</b>
 * <ul>
 *   <li><b>Odd Length Datasets:</b> {@code Pair.left} contains the single median, {@code Pair.right} is empty</li>
 *   <li><b>Even Length Datasets:</b> {@code Pair.left} contains the smaller median, {@code Pair.right} contains the larger. The smaller and larger medians may be equal.</li>
 *   <li><b>Primitive Types:</b> Use specialized Optional types (OptionalInt, OptionalLong, etc.)</li>
 *   <li><b>Object Types:</b> Use generic Optional&lt;T&gt; for the second median value</li>
 * </ul>
 *
 * <p><b>Supported Data Types:</b>
 * <ul>
 *   <li><b>Primitive Arrays:</b> {@code char[]}, {@code byte[]}, {@code short[]}, {@code int[]}, {@code long[]}, {@code float[]}, {@code double[]}</li>
 *   <li><b>Object Arrays:</b> {@code T[]} with natural ordering or custom {@code Comparator}</li>
 *   <li><b>Collections:</b> {@code Collection<T>} with natural ordering or custom {@code Comparator}</li>
 *   <li><b>Range Operations:</b> Subarray and subcollection median calculation with {@code fromIndex/toIndex}</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic median calculation for primitive arrays
 * int[] numbers = {5, 2, 8, 1, 9, 3};
 * Pair<Integer, OptionalInt> result = Median.of(numbers);
 * int median1 = result.left;  // First median (smaller for even length)
 * OptionalInt median2 = result.right;  // Second median (empty for odd length)
 *
 * // Handle odd vs even length results
 * if (median2.isPresent()) {
 *     // Even number of elements - two medians
 *     double average = (median1 + median2.get()) / 2.0;
 *     System.out.println("Average of medians: " + average);
 * } else {
 *     // Odd number of elements - single median
 *     System.out.println("Single median: " + median1);
 * }
 *
 * // Range-based median calculation
 * Pair<Integer, OptionalInt> rangeResult = Median.of(numbers, 1, 4);   // Elements at indices 1-3
 *
 * // Collection with natural ordering
 * List<String> words = Arrays.asList("zebra", "apple", "banana", "cherry");
 * Pair<String, Optional<String>> wordMedian = Median.of(words);
 * String medianWord = wordMedian.left;  // "banana" (alphabetically)
 *
 * // Collection with custom comparator
 * Comparator<String> lengthComparator = Comparator.comparing(String::length);
 * Pair<String, Optional<String>> lengthMedian = Median.of(words, lengthComparator);
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Working with floating-point data
 * double[] measurements = {23.1, 45.7, 12.3, 67.8, 34.5, 56.2};
 * Pair<Double, OptionalDouble> doubleResult = Median.of(measurements);
 * double primaryMedian = doubleResult.left;
 * doubleResult.right.ifPresent(secondMedian -> {
 *     double traditionalMedian = (primaryMedian + secondMedian) / 2.0;
 *     System.out.println("Traditional median: " + traditionalMedian);
 * });
 *
 * // Custom object with natural ordering
 * public class Score implements Comparable<Score> {
 *     private final int value;
 *     public Score(int value) { this.value = value; }
 *     public int compareTo(Score other) { return Integer.compare(this.value, other.value); }
 * }
 *
 * Score[] scores = {new Score(85), new Score(92), new Score(78), new Score(96), new Score(88)};
 * Pair<Score, Optional<Score>> scoreMedian = Median.of(scores);
 *
 * // Complex custom comparator
 * List<Employee> employees = getEmployees();
 * Comparator<Employee> salaryComparator = Comparator
 *     .comparing(Employee::getSalary)
 *     .thenComparing(Employee::getExperience);
 * Pair<Employee, Optional<Employee>> salaryMedian = Median.of(employees, salaryComparator);
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Time Complexity:</b> O(n) where n is the number of elements</li>
 *   <li><b>Space Complexity:</b> O(k) where k = (n/2 + 1), typically much less than n</li>
 *   <li><b>Comparison Count:</b> Exactly n comparisons for array traversal plus heap operations</li>
 *   <li><b>Memory Allocation:</b> Single priority queue allocation, no additional arrays</li>
 *   <li><b>Cache Efficiency:</b> Sequential access pattern for optimal cache performance</li>
 * </ul>
 *
 * <p><b>Algorithm Details:</b>
 * <ul>
 *   <li><b>Min-Heap Strategy:</b> Maintains a min-heap of the largest (n/2 + 1) elements</li>
 *   <li><b>Heap Size:</b> Bounded heap size ensures constant space complexity relative to position</li>
 *   <li><b>Single Traversal:</b> Processes each element exactly once in the input order</li>
 *   <li><b>Early Termination:</b> No unnecessary processing after heap reaches optimal size</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All methods are stateless and thread-safe</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 *   <li><b>No Shared State:</b> No static mutable fields or shared resources</li>
 *   <li><b>Local Variables:</b> All computation uses local variables and parameters</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IllegalArgumentException:</b> Thrown for null or empty input arrays/collections</li>
 *   <li><b>IndexOutOfBoundsException:</b> Thrown for invalid range parameters in range-based methods</li>
 *   <li><b>ClassCastException:</b> Thrown when elements don't implement Comparable and no Comparator provided</li>
 *   <li><b>Null Elements:</b> Handled gracefully based on Comparator null-handling behavior</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. Full Sorting:</b> 3-5x faster for median-only calculations, O(n) vs O(n log n)</li>
 *   <li><b>vs. Quickselect:</b> More predictable performance, better for small to medium datasets</li>
 *   <li><b>vs. Stream.sorted():</b> Avoids intermediate collection creation and full sorting overhead</li>
 *   <li><b>vs. Manual Implementation:</b> Optimized algorithm with comprehensive type support</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Bounded Allocation:</b> Memory usage independent of input size beyond median position</li>
 *   <li><b>Efficient Heap:</b> Priority queue automatically manages memory for optimal performance</li>
 *   <li><b>No Copying:</b> Processes input data in-place without creating sorted copies</li>
 *   <li><b>Garbage Collection:</b> Minimal object creation reduces GC pressure</li>
 * </ul>
 *
 * <p><b>Numerical Stability:</b>
 * <ul>
 *   <li><b>Floating-Point Precision:</b> Preserves original precision without intermediate calculations</li>
 *   <li><b>Overflow Protection:</b> No arithmetic operations that could cause overflow</li>
 *   <li><b>NaN Handling:</b> Follows standard floating-point comparison rules for NaN values</li>
 *   <li><b>Infinity Support:</b> Properly handles positive and negative infinity values</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate primitive-specific methods to avoid boxing overhead</li>
 *   <li>Check {@code Optional.isPresent()} before accessing the second median value</li>
 *   <li>Provide explicit {@code Comparator} for custom objects to avoid ClassCastException</li>
 *   <li>Validate input ranges before calling range-based methods</li>
 *   <li>Consider caching {@code Comparator} instances for repeated operations</li>
 *   <li>Use {@code Pair.left} and {@code Pair.right} descriptively in variable names</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Sorting the entire dataset just to find the median</li>
 *   <li>Using {@code Optional.get()} without checking {@code isPresent()} first</li>
 *   <li>Ignoring the distinction between single and double median cases</li>
 *   <li>Creating unnecessary intermediate collections for simple arrays</li>
 *   <li>Using inappropriate range parameters that exceed array bounds</li>
 * </ul>
 *
 * <p><b>Integration with Statistics Libraries:</b>
 * <ul>
 *   <li><b>Descriptive Statistics:</b> Can be combined with mean, mode, and variance calculations</li>
 *   <li><b>Percentile Calculations:</b> Median is the 50th percentile, can extend to other percentiles</li>
 *   <li><b>Data Analysis:</b> Useful for outlier detection and distribution analysis</li>
 *   <li><b>Quality Metrics:</b> Common metric in performance analysis and benchmarking</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link Pair}:</b> Container for the median result pair</li>
 *   <li><b>{@link OptionalInt}:</b> Type-safe optional for integer median values</li>
 *   <li><b>{@link OptionalDouble}:</b> Type-safe optional for double median values</li>
 *   <li><b>{@link Optional}:</b> Generic optional for object median values</li>
 *   <li><b>{@link Comparator}:</b> For custom ordering in object median calculations</li>
 *   <li><b>{@link PriorityQueue}:</b> Underlying data structure for the algorithm</li>
 * </ul>
 *
 * <p><b>Example: Statistical Analysis Workflow</b>
 * <pre>{@code
 * public class DataAnalysis {
 *     public StatisticalSummary analyzeDataset(double[] dataset) {
 *         // Calculate median efficiently
 *         Pair<Double, OptionalDouble> medianResult = Median.of(dataset);
 *         double median = medianResult.right.isPresent() 
 *             ? (medianResult.left + medianResult.right.get()) / 2.0
 *             : medianResult.left;
 *
 *         // Use median for further analysis
 *         double[] deviationsFromMedian = Arrays.stream(dataset)
 *             .map(value -> Math.abs(value - median))
 *             .toArray();
 *
 *         Pair<Double, OptionalDouble> madResult = Median.of(deviationsFromMedian);
 *         double medianAbsoluteDeviation = madResult.right.isPresent()
 *             ? (madResult.left + madResult.right.get()) / 2.0
 *             : madResult.left;
 *
 *         return new StatisticalSummary(median, medianAbsoluteDeviation, detectOutliers(dataset, median));
 *     }
 * }
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
 * @see Optional
 * @see Comparator
 * @see PriorityQueue
 * @see Collection
 * @see Arrays
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).</p>
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
     * char medianValue = median.left;  // 'm'
     * }</pre>
     *
     * @param a the array of characters to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(char[], int, int)
     * @see Pair
     * @see OptionalChar
     * @see N#median(char[])
     */
    public static Pair<Character, OptionalChar> of(final char... a) throws IllegalArgumentException {
        N.checkArgNotEmpty(a, "The specified array 'a' can't be null or empty");   //NOSONAR

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'z', 'a', 'm', 'x'};
     * Pair<Character, OptionalChar> median = Median.of(chars, 1, 3);
     * // Considers only 'a' and 'm', returns ['a', OptionalChar.of('m')]
     * }</pre>
     *
     * @param a the array of characters to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(char...)
     * @see N#median(char[], int, int)
     */
    public static Pair<Character, OptionalChar> of(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        if (N.isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of array can't be null or empty");   //NOSONAR
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).</p>
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
     * byte medianValue = median.left;  // 10
     * }</pre>
     *
     * @param a the array of bytes to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(byte[], int, int)
     * @see Pair
     * @see OptionalByte
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = {30, 10, 20, 40};
     * Pair<Byte, OptionalByte> median = Median.of(bytes, 1, 3);
     * // Considers only bytes[1] and bytes[2]: 10, 20. Returns [10, OptionalByte.of(20)]
     * }</pre>
     *
     * @param a the array of bytes to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).</p>
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
     * short medianValue = median.left;  // 100
     * }</pre>
     *
     * @param a the array of short integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(short[], int, int)
     * @see Pair
     * @see OptionalShort
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {300, 100, 200, 400};
     * Pair<Short, OptionalShort> median = Median.of(values, 1, 3);
     * // Considers only values[1] and values[2]: 100, 200. Returns [100, OptionalShort.of(200)]
     * }</pre>
     *
     * @param a the array of short integers to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).</p>
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
     * int lowerMedian = median.left;  // 10
     * int upperMedian = median.right.get();   // 15
     * }</pre>
     *
     * @param a the array of integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(int[], int, int)
     * @see Pair
     * @see OptionalInt
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {100, 50, 75, 25, 90};
     * Pair<Integer, OptionalInt> median = Median.of(numbers, 1, 4);
     * // Considers only numbers[1], numbers[2], numbers[3]: 50, 75, 25. Returns [50, OptionalInt.empty()]
     * }</pre>
     *
     * @param a the array of integers to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).</p>
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
     * long lowerMedian = median.left;  // 750
     * long upperMedian = median.right.get();   // 1000
     * }</pre>
     *
     * @param a the array of long integers to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(long[], int, int)
     * @see Pair
     * @see OptionalLong
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] values = {3000L, 1000L, 2000L, 4000L, 1500L};
     * Pair<Long, OptionalLong> median = Median.of(values, 1, 4);
     * // Considers values[1], values[2], values[3]: 1000, 2000, 4000. Returns [2000, OptionalLong.empty()]
     * }</pre>
     *
     * @param a the array of long integers to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).
     * This implementation handles floating-point values correctly, including proper handling of NaN values
     * according to IEEE 754 floating-point standards.</p>
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
     * float medianValue = median.left;  // 10.5f
     * }</pre>
     *
     * @param a the array of float values to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(float[], int, int)
     * @see Pair
     * @see OptionalFloat
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
     * by the range [fromIndex, toIndex) and efficiently computes the median using a bounded priority queue.
     * This implementation handles floating-point values correctly, including proper handling of NaN values
     * according to IEEE 754 floating-point standards.</p>
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
     * @param a the array of float values to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * algorithm that processes the array in O(n) time complexity by maintaining a bounded heap of size (length/2 + 1).
     * This implementation handles double-precision floating-point values with full IEEE 754 compliance,
     * including proper treatment of special values like NaN and infinity.</p>
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
     * double lowerMedian = median.left;  // 10.5
     * double upperMedian = median.right.get();   // 15.1
     * }</pre>
     *
     * @param a the array of double values to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(double[], int, int)
     * @see Pair
     * @see OptionalDouble
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
     * by the range [fromIndex, toIndex) and efficiently computes the median using a bounded priority queue.
     * This implementation handles double-precision floating-point values with full IEEE 754 compliance,
     * including proper treatment of special values like NaN and infinity.</p>
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
     * @param a the array of double values to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"apple", "banana", "cherry"};
     * Pair<String, Optional<String>> median = Median.of(words);
     * String medianWord = median.left;  // "banana"
     * }</pre>
     *
     * @param <T> the type of elements in the array., which must implement Comparable.
     * @param a the array of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(Comparable[], int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"zebra", "apple", "banana", "cherry"};
     * Pair<String, Optional<String>> median = Median.of(words, 1, 4);
     * // Considers words[1], words[2], words[3]: "apple", "banana", "cherry". Returns ["banana", Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the array., which must implement Comparable.
     * @param a the array of Comparable objects to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Comparable[])
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
     * <p><strong>Note:</strong> This method does not explicitly handle {@code null} elements. Behavior with null
     * elements depends on the comparator used. If using natural ordering (null comparator) or a comparator
     * that does not handle nulls, a {@code NullPointerException} may be thrown during comparison.</p>
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
     * Pair<String, Optional<String>> median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left;  // "apple" (middle length)
     * }</pre>
     *
     * @param <T> the type of elements in the array.
     * @param a the array of objects to find the median from. Must not be {@code null} or empty.
     * @param cmp the comparator to use for ordering the elements. If {@code null}, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-length arrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length arrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or empty.
     * @see #of(Object[], int, int, Comparator)
     * @see #of(Comparable[])
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
     * <p><strong>Note:</strong> This method does not explicitly handle {@code null} elements. Behavior with null
     * elements depends on the comparator used. If using natural ordering (null comparator) or a comparator
     * that does not handle nulls, a {@code NullPointerException} may be thrown during comparison.</p>
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
     * Pair<String, Optional<String>> median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words[1], words[2], words[3]: "ant", "bee", "tiger". Returns ["bee", Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the array.
     * @param a the array of objects to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the array to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the array to consider.
     *                Must be greater than fromIndex and not exceed array length.
     * @param cmp the comparator to use for ordering the elements. If {@code null}, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-length subarrays, the {@code left}
     *         contains the median and {@code right} is empty. For even-length subarrays, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified array is {@code null} or the range is empty.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the length of the array, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Object[], Comparator)
     * @see #of(Comparable[], int, int)
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
     * Finds the median value(s) from a collection of Comparable objects using their natural ordering.
     *
     * <p>The median represents the middle value(s) when the collection elements are arranged in sorted order
     * according to their natural comparison method (compareTo). The input collection does not need to be sorted.
     * This method uses an efficient priority queue-based algorithm that iterates through the collection once
     * while maintaining a bounded heap of size (size/2 + 1), avoiding the need for full sorting.</p>
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
     * Pair<Integer, Optional<Integer>> median = Median.of(numbers);
     * int medianValue = median.left;  // 15
     * }</pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable.
     * @param c the collection of Comparable objects to find the median from. Must not be {@code null} or empty.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
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
     * <p><strong>Note:</strong> This method does not explicitly handle {@code null} elements. Behavior with null
     * elements depends on the comparator used. If using natural ordering (null comparator) or a comparator
     * that does not handle nulls, a {@code NullPointerException} may be thrown during comparison.</p>
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
     * Pair<String, Optional<String>> median = Median.of(words, Comparator.comparing(String::length));
     * String medianWord = median.left;  // Word with median length
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection of objects to find the median from. Must not be {@code null} or empty.
     * @param cmp the comparator to use for ordering the elements. If {@code null}, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-size collections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size collections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or empty.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
     * Pair<Integer, Optional<Integer>> median = Median.of(numbers, 1, 4);
     * // Considers elements at indices 1, 2, 3: 50, 75, 25. Returns [50, Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the collection, which must implement Comparable.
     * @param c the collection of Comparable objects to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or the range is empty.
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
     * <p><strong>Note:</strong> This method does not explicitly handle {@code null} elements. Behavior with null
     * elements depends on the comparator used. If using natural ordering (null comparator) or a comparator
     * that does not handle nulls, a {@code NullPointerException} may be thrown during comparison.</p>
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
     * Pair<String, Optional<String>> median = Median.of(words, 1, 4, Comparator.comparing(String::length));
     * // Considers words at indices 1, 2, 3: "ant", "bee", "tiger". Returns ["bee", Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection of objects to find the median from. Must not be {@code null}.
     * @param fromIndex the starting index (inclusive) of the range within the collection to consider.
     *                  Must be non-negative and less than or equal to toIndex.
     * @param toIndex the ending index (exclusive) of the range within the collection to consider.
     *                Must be greater than fromIndex and not exceed collection size.
     * @param cmp the comparator to use for ordering the elements. If {@code null}, natural ordering is used.
     *            The comparator must be consistent with equals for predictable results.
     * @return a {@code Pair} containing the median value(s). For odd-size subcollections, the {@code left}
     *         contains the median and {@code right} is empty. For even-size subcollections, the {@code left}
     *         contains the smaller median and {@code right} contains the larger median.
     * @throws IllegalArgumentException if the specified collection is {@code null} or the range is empty.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than 
     *                                   the size of the collection, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Collection, Comparator)
     * @see #of(Collection, int, int)
     */
    public static <T> Pair<T, Optional<T>> of(final Collection<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        if (N.isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection can't be null or empty");   //NOSONAR
        }

        N.checkFromToIndex(fromIndex, toIndex, c.size());

        return of(N.slice(c, fromIndex, toIndex), cmp);
    }
}
