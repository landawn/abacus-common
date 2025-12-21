/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.io.Serial;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

/**
 * A high-performance, resizable array implementation for primitive double values that provides
 * specialized operations optimized for double-precision floating-point data types. This class extends
 * {@link PrimitiveList} to offer memory-efficient storage and operations that avoid the boxing overhead
 * associated with {@code List<Double>}, making it ideal for applications requiring intensive double array
 * manipulation with optimal performance characteristics.
 *
 * <p>DoubleList is specifically designed for scenarios involving large collections of high-precision
 * floating-point values such as scientific computing, financial calculations, statistical analysis,
 * numerical simulations, mathematical modeling, and performance-critical applications requiring
 * double-precision arithmetic. The implementation uses a compact double array as the underlying storage
 * mechanism, providing direct primitive access without wrapper object allocation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct double primitive storage without Double wrapper allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact double array storage with minimal memory overhead</li>
 *   <li><b>Double-Precision Arithmetic:</b> Full support for IEEE 754 double-precision operations</li>
 *   <li><b>High Performance:</b> Optimized algorithms for floating-point specific operations</li>
 *   <li><b>Rich Mathematical API:</b> Statistical operations like min, max, median, sum, average</li>
 *   <li><b>Set Operations:</b> Efficient intersection, union, and difference operations</li>
 *   <li><b>Range Generation:</b> Built-in support for arithmetic progressions and sequences</li>
 *   <li><b>Random Access:</b> O(1) element access and modification by index</li>
 *   <li><b>Dynamic Sizing:</b> Automatic capacity management with intelligent growth</li>
 *   <li><b>Stream Integration:</b> Full compatibility with DoubleStream for functional processing</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Scientific Computing:</b> Numerical simulations, mathematical modeling, research calculations</li>
 *   <li><b>Financial Analysis:</b> Price data, returns, risk calculations, portfolio optimization</li>
 *   <li><b>Statistical Analysis:</b> Data science, hypothesis testing, regression analysis</li>
 *   <li><b>Engineering Applications:</b> CAD calculations, finite element analysis, optimization problems</li>
 *   <li><b>Machine Learning:</b> Training data, feature vectors, model parameters, predictions</li>
 *   <li><b>Signal Processing:</b> Digital filters, Fourier transforms, time series analysis</li>
 *   <li><b>Geospatial Computing:</b> Coordinates, distances, geographic calculations</li>
 *   <li><b>Physics Simulations:</b> Particle systems, molecular dynamics, quantum calculations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and initializing double lists
 * DoubleList prices = DoubleList.of(100.50, 101.25, 99.75, 102.10);
 * DoubleList range = DoubleList.range(0.0, 10.0, 0.1);   // [0.0, 0.1, 0.2, ..., 9.9]
 * DoubleList measurements = new DoubleList(10000);   // Pre-sized for large dataset
 * DoubleList random = DoubleList.random(0.0, 1.0, 1000);   // 1000 random doubles [0.0, 1.0)
 *
 * // Basic operations
 * prices.add(103.45);   // Append double value
 * double firstPrice = prices.get(0);   // Access by index: 100.50
 * prices.set(1, 101.50);   // Modify existing value
 *
 * // Mathematical operations for high-precision data
 * OptionalDouble min = prices.min();   // Find minimum value
 * OptionalDouble max = prices.max();   // Find maximum value
 * OptionalDouble median = prices.median();   // Calculate median value
 * double sum = prices.stream().sum();   // Calculate sum
 * double average = prices.stream().average().orElse(0.0);   // Calculate average
 *
 * // Set operations for data analysis
 * DoubleList set1 = DoubleList.of(1.0, 2.5, 3.7, 4.2);
 * DoubleList set2 = DoubleList.of(3.7, 4.2, 5.1, 6.8);
 * DoubleList intersection = set1.intersection(set2);   // [3.7, 4.2]
 * DoubleList difference = set1.difference(set2);   // [1.0, 2.5]
 *
 * // High-performance sorting and searching
 * prices.sort();   // Sort in ascending order
 * prices.parallelSort();   // Parallel sort for large datasets
 * int index = prices.binarySearch(101.25);   // Fast lookup
 *
 * // Statistical and functional operations
 * DoubleList returns = prices.stream()                     // Calculate returns
 *     .skip(1)
 *     .map((i, current) -> (current - prices.get(i)) / prices.get(i))
 *     .collect(DoubleList::new, DoubleList::add, DoubleList::addAll);
 *
 * // Type conversions
 * FloatList floatValues = prices.stream()                  // Convert to float (precision loss)
 *     .mapToFloat(d -> (float) d)
 *     .collect(FloatList::new, FloatList::add, FloatList::addAll);
 * double[] primitiveArray = prices.toArray();   // To primitive array
 * List<Double> boxedList = prices.boxed();   // To boxed collection
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Element Access:</b> O(1) for get/set operations by index</li>
 *   <li><b>Insertion:</b> O(1) amortized for append, O(n) for middle insertion</li>
 *   <li><b>Deletion:</b> O(1) for last element, O(n) for arbitrary position</li>
 *   <li><b>Search:</b> O(n) for contains/indexOf, O(log n) for binary search on sorted data</li>
 *   <li><b>Sorting:</b> O(n log n) using optimized primitive sorting algorithms</li>
 *   <li><b>Parallel Sorting:</b> O(n log n) with improved constants on multi-core systems</li>
 *   <li><b>Set Operations:</b> O(n) to O(n²) depending on algorithm selection and data size</li>
 *   <li><b>Mathematical Operations:</b> O(n) for statistical calculations</li>
 * </ul>
 *
 * <p><b>Memory Efficiency:</b>
 * <ul>
 *   <li><b>Storage:</b> 8 bytes per element (64 bits) with no object overhead</li>
 *   <li><b>vs List&lt;Double&gt;:</b> ~3x less memory usage (no Double wrapper objects)</li>
 *   <li><b>Capacity Management:</b> 1.75x growth factor balances memory and performance</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} (typically Integer.MAX_VALUE - 8)</li>
 * </ul>
 *
 * <p><b>Floating-Point Considerations:</b>
 * <ul>
 *   <li><b>IEEE 754 Compliance:</b> Full support for double-precision floating-point standard</li>
 *   <li><b>Special Values:</b> Proper handling of NaN, positive/negative infinity</li>
 *   <li><b>Precision:</b> ~15-17 decimal digits of precision (53-bit mantissa)</li>
 *   <li><b>Range:</b> Approximately ±1.8 × 10^308 with subnormal support</li>
 *   <li><b>Comparison:</b> NaN-aware comparison operations</li>
 * </ul>
 *
 * <p><b>Double-Specific Operations:</b>
 * <ul>
 *   <li><b>Range Generation:</b> {@code range()}, {@code rangeClosed()} for arithmetic sequences</li>
 *   <li><b>Mathematical Functions:</b> {@code min()}, {@code max()}, {@code median()}</li>
 *   <li><b>Statistical Analysis:</b> Full integration with DoubleStream for advanced operations</li>
 *   <li><b>Random Generation:</b> {@code random()} methods for simulations and testing</li>
 *   <li><b>Parallel Operations:</b> {@code parallelSort()} for large dataset optimization</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li><b>{@code of(double...)}:</b> Create from varargs array</li>
 *   <li><b>{@code copyOf(double[])}:</b> Create defensive copy of array</li>
 *   <li><b>{@code range(double, double, double)}:</b> Create arithmetic sequence with step</li>
 *   <li><b>{@code repeat(double, int)}:</b> Create with repeated values</li>
 *   <li><b>{@code random(int)}:</b> Create with random double values</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toArray()}:</b> Convert to primitive double array</li>
 *   <li><b>{@code boxed()}:</b> Convert to {@code List<Double>}</li>
 *   <li><b>{@code stream()}:</b> Convert to DoubleStream for functional processing</li>
 * </ul>
 *
 * <p><b>Deque-like Operations:</b>
 * <ul>
 *   <li><b>{@code addFirst(double)}:</b> Insert at beginning (O(n) operation)</li>
 *   <li><b>{@code addLast(double)}:</b> Insert at end (O(1) amortized)</li>
 *   <li><b>{@code removeFirst()}:</b> Remove from beginning (O(n) operation)</li>
 *   <li><b>{@code removeLast()}:</b> Remove from end (O(1) operation)</li>
 *   <li><b>{@code getFirst()}:</b> Access first element (O(1) operation)</li>
 *   <li><b>{@code getLast()}:</b> Access last element (O(1) operation)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Not Thread-Safe:</b> This implementation is not synchronized</li>
 *   <li><b>External Synchronization:</b> Required for concurrent access</li>
 *   <li><b>Fail-Fast Iterators:</b> Detect concurrent modifications</li>
 *   <li><b>Read-Only Access:</b> Multiple threads can safely read simultaneously</li>
 * </ul>
 *
 * <p><b>Capacity Management:</b>
 * <ul>
 *   <li><b>Initial Capacity:</b> Default capacity of 10 elements</li>
 *   <li><b>Growth Strategy:</b> 1.75x expansion when capacity exceeded</li>
 *   <li><b>Manual Control:</b> {@code ensureCapacity()} for performance optimization</li>
 *   <li><b>Trimming:</b> {@code trimToSize()} to reduce memory footprint</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IndexOutOfBoundsException:</b> For invalid index access</li>
 *   <li><b>NoSuchElementException:</b> For operations on empty lists</li>
 *   <li><b>IllegalArgumentException:</b> For invalid method parameters</li>
 *   <li><b>OutOfMemoryError:</b> When capacity exceeds available memory</li>
 * </ul>
 *
 * <p><b>Serialization Support:</b>
 * <ul>
 *   <li><b>Serializable:</b> Implements {@link java.io.Serializable}</li>
 *   <li><b>Version Compatibility:</b> Stable serialVersionUID for version compatibility</li>
 *   <li><b>Efficient Format:</b> Optimized serialization of double arrays</li>
 *   <li><b>Cross-Platform:</b> Platform-independent serialized format</li>
 * </ul>
 *
 * <p><b>Integration with Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Collection Compatibility:</b> Seamless conversion to standard collections</li>
 *   <li><b>Utility Integration:</b> Works with Collections utility methods via boxed()</li>
 *   <li><b>Stream API:</b> Full integration with DoubleStream for functional processing</li>
 * </ul>
 *
 * <p><b>Mathematical and Statistical Operations:</b>
 * <ul>
 *   <li><b>Aggregation:</b> Sum, min, max, average operations via stream API</li>
 *   <li><b>Central Tendency:</b> Median calculation with efficient sorting</li>
 *   <li><b>Occurrence Counting:</b> {@code occurrencesOf()} for frequency analysis</li>
 *   <li><b>Duplicate Detection:</b> {@code hasDuplicates()}, {@code removeDuplicates()}</li>
 *   <li><b>Advanced Statistics:</b> Standard deviation, variance via stream operations</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs List&lt;Double&gt;:</b> 3x less memory, significantly faster operations</li>
 *   <li><b>vs double[]:</b> Dynamic sizing, rich API, set operations, statistical functions</li>
 *   <li><b>vs FloatList:</b> Higher precision, larger range, better for scientific computing</li>
 *   <li><b>vs ArrayList&lt;Double&gt;:</b> No boxing overhead, primitive-specific methods</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code DoubleList} when high precision is required for calculations</li>
 *   <li>Specify initial capacity for known data sizes to avoid resizing</li>
 *   <li>Use bulk operations ({@code addAll}, {@code removeAll}) instead of loops</li>
 *   <li>Leverage DoubleStream for complex mathematical transformations</li>
 *   <li>Consider parallel operations for large datasets (>10,000 elements)</li>
 *   <li>Be aware of floating-point precision limitations in equality comparisons</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Pre-size lists with known capacity using constructor or {@code ensureCapacity()}</li>
 *   <li>Use {@code addLast()} instead of {@code addFirst()} for better performance</li>
 *   <li>Sort data before using {@code binarySearch()} for O(log n) lookups</li>
 *   <li>Use {@code parallelSort()} for large datasets to leverage multi-core processors</li>
 *   <li>Consider {@code stream().parallel()} for CPU-intensive mathematical operations</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Financial Data:</b> {@code DoubleList prices = new DoubleList(tradingDays);}</li>
 *   <li><b>Scientific Data:</b> {@code DoubleList measurements = DoubleList.random(minVal, maxVal, count);}</li>
 *   <li><b>Statistical Analysis:</b> {@code DoubleList results = dataset.stream().mapToDouble(...).collect(...);}</li>
 *   <li><b>Mathematical Modeling:</b> {@code DoubleList coefficients = DoubleList.of(a, b, c, d);}</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link PrimitiveList}:</b> Abstract base class for all primitive list types</li>
 *   <li><b>{@link FloatList}:</b> Single-precision floating-point list implementation</li>
 *   <li><b>{@link LongList}:</b> 64-bit integer primitive list for discrete values</li>
 *   <li><b>{@link DoubleIterator}:</b> Specialized iterator for double primitives</li>
 *   <li><b>{@link DoubleStream}:</b> Functional processing of double sequences</li>
 * </ul>
 *
 * <p><b>Example: Financial Analysis</b>
 * <pre>{@code
 * // Analyze stock price data
 * DoubleList prices = DoubleList.of(100.0, 102.5, 101.8, 105.2, 103.7, 106.1);
 *
 * // Calculate daily returns
 * DoubleList returns = new DoubleList(prices.size() - 1);
 * for (int i = 1; i < prices.size(); i++) {
 *     double returnValue = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
 *     returns.add(returnValue);
 * }
 *
 * // Statistical analysis
 * double avgReturn = returns.stream().average().orElse(0.0);
 * double totalReturn = (prices.getLast() - prices.getFirst()) / prices.getFirst();
 * OptionalDouble maxReturn = returns.max();
 * OptionalDouble minReturn = returns.min();
 *
 * // Risk analysis using stream operations
 * double variance = returns.stream()
 *     .map(r -> Math.pow(r - avgReturn, 2))
 *     .average()
 *     .orElse(0.0);
 * double volatility = Math.sqrt(variance);
 *
 * // Portfolio optimization
 * DoubleList weights = DoubleList.of(0.3, 0.4, 0.3);
 * double portfolioReturn = returns.stream()
 *     .mapToDouble((i, r) -> r * weights.get(i % weights.size()))
 *     .sum();
 * }</pre>
 *
 * @see PrimitiveList
 * @see DoubleIterator
 * @see DoubleStream
 * @see FloatList
 * @see LongList
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see java.util.List
 * @see java.util.RandomAccess
 * @see java.io.Serializable
 */
public final class DoubleList extends PrimitiveList<Double, double[], DoubleList> {

    @Serial
    private static final long serialVersionUID = 766157472430159621L;

    static final Random RAND = new SecureRandom();

    /**
     * The array buffer into which the elements of the DoubleList are stored.
     */
    private double[] elementData = N.EMPTY_DOUBLE_ARRAY;

    /**
     * The size of the DoubleList (the number of elements it contains).
     */
    private int size = 0;

    /**
     * Constructs an empty DoubleList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     */
    public DoubleList() {
    }

    /**
     * Constructs an empty DoubleList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public DoubleList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_DOUBLE_ARRAY : new double[initialCapacity];
    }

    /**
     * Constructs a DoubleList using the specified array as the backing array.
     * The array is used directly without copying, making this constructor very efficient.
     * Modifications to the list will directly affect the provided array.
     * The size of the list will be equal to the length of the array.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     */
    public DoubleList(final double[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a DoubleList using the specified array as the backing array with a specific size.
     * The array is used directly without copying. Only the first <i>size</i> elements of the array
     * are considered part of the list. This constructor is useful when working with arrays
     * that are larger than the actual data they contain.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     * @param size the number of elements in the list. Must be between 0 and a.length (inclusive).
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public DoubleList(final double[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new DoubleList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new DoubleList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static DoubleList of(final double... a) {
        return new DoubleList(N.nullToEmpty(a));
    }

    /**
     * Creates a new DoubleList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * @param a the array of double values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new DoubleList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static DoubleList of(final double[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new DoubleList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new DoubleList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(double...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new DoubleList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static DoubleList copyOf(final double[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new DoubleList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new DoubleList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static DoubleList copyOf(final double[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a new DoubleList with the specified element repeated a given number of times.
     * This method is useful for initializing a list with a default value.
     *
     * @param element the double value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new DoubleList containing <i>len</i> copies of the specified element
     * @throws IllegalArgumentException if len is negative
     */
    public static DoubleList repeat(final double element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a new DoubleList filled with random double values.
     * Each element is a random double value between 0.0 (inclusive) and 1.0 (exclusive),
     * generated using a secure random number generator.
     *
     * @param len the number of random double values to generate. Must be non-negative.
     * @return a new DoubleList containing <i>len</i> random double values
     * @throws IllegalArgumentException if len is negative
     */
    public static DoubleList random(final int len) {
        final double[] a = new double[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextDouble();
        }

        return of(a);
    }

    /**
     * Returns the underlying double array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.</p>
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal double array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public double[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public double get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index the index of the element to replace
     * @param e the element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public double set(final int index, final double e) {
        rangeCheck(index);

        final double oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * The list will automatically grow if necessary to accommodate the new element.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the element to be appended to this list
     */
    public void add(final double e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final double e) {
        rangeCheckForAdd(index);

        ensureCapacity(size + 1);

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + 1, numMoved);
        }

        elementData[index] = e;

        size++;
    }

    /**
     * Appends all elements from the specified DoubleList to the end of this list.
     * The elements are appended in the order they appear in the specified list.
     *
     * @param c the DoubleList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty)
     */
    @Override
    public boolean addAll(final DoubleList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew);

        N.copy(c.array(), 0, elementData, size, numNew);

        size += numNew;

        return true;
    }

    /**
     * Inserts all elements from the specified DoubleList into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices). The new elements will appear in this list in the order they appear
     * in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the DoubleList containing elements to be inserted into this list
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final DoubleList c) {
        rangeCheckForAdd(index);

        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew); // Increments modCount

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(c.array(), 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    /**
     * Appends all elements from the specified array to the end of this list.
     * The elements are appended in the order they appear in the array.
     *
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty)
     */
    @Override
    public boolean addAll(final double[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices). The new elements will appear in this list in the order they appear
     * in the array.
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be inserted into this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final double[] a) {
        rangeCheckForAdd(index);

        if (N.isEmpty(a)) {
            return false;
        }

        final int numNew = a.length;

        ensureCapacity(size + numNew); // Increments modCount

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(a, 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If the list does not contain the element, it is unchanged. Elements are compared using
     * {@code Double.compare} for accurate floating-point comparison.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final double e) {
        for (int i = 0; i < size; i++) {
            if (N.equals(elementData[i], e)) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list.
     * The list is compacted after removal, and all remaining elements maintain their relative order.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param e the element to be removed from this list
     * @return {@code true} if at least one occurrence was removed from the list
     */
    public boolean removeAllOccurrences(final double e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (!N.equals(elementData[i], e)) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Removes the element at the specified index without bounds checking.
     *
     * @param index the index of the element to remove
     */
    private void fastRemove(final int index) {
        final int numMoved = size - index - 1;

        if (numMoved > 0) {
            N.copy(elementData, index + 1, elementData, index, numMoved);
        }

        elementData[--size] = 0; // clear to let GC do its work
    }

    /**
     * Removes from this list all of its elements that are contained in the specified DoubleList.
     * This operation effectively performs a set difference, removing elements based on their values
     * rather than their positions. If an element appears multiple times, all occurrences are removed.
     *
     * @param c the DoubleList containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of this operation
     */
    @Override
    public boolean removeAll(final DoubleList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * This operation effectively performs a set difference, removing elements based on their values
     * rather than their positions. If an element appears multiple times, all occurrences are removed.
     *
     * @param a the array containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of this operation
     */
    @Override
    public boolean removeAll(final double[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * The elements are tested in order, and those for which the predicate returns {@code true} are removed.
     * The relative order of retained elements is preserved.
     *
     * @param p the predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed
     */
    public boolean removeIf(final DoublePredicate p) {
        final DoubleList tmp = new DoubleList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, 0d);
        size = tmp.size;

        return true;
    }

    /**
     * Removes duplicate elements from this list, keeping only the first occurrence of each value.
     * The relative order of retained elements is preserved. This method is optimized to handle
     * both sorted and unsorted lists efficiently.
     *
     * @return {@code true} if any duplicates were removed from the list
     */
    @Override
    public boolean removeDuplicates() {
        if (size < 2) {
            return false;
        }

        final boolean isSorted = isSorted();
        int idx = 0;

        if (isSorted) {
            for (int i = 1; i < size; i++) {
                if (elementData[i] != elementData[idx]) { // NOSONAR
                    elementData[++idx] = elementData[i];
                }
            }

        } else {
            final Set<Double> set = N.newLinkedHashSet(size);
            set.add(elementData[0]);

            for (int i = 1; i < size; i++) {
                if (set.add(elementData[i])) {
                    elementData[++idx] = elementData[i];
                }
            }
        }

        if (idx == size - 1) {
            return false;
        } else {
            N.fill(elementData, idx + 1, size, 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified DoubleList.
     * In other words, removes from this list all elements that are not contained in the specified list.
     * This operation effectively performs a set intersection based on element values.
     *
     * @param c the DoubleList containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of this operation
     */
    @Override
    public boolean retainAll(final DoubleList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all elements that are not contained in the specified array.
     * This operation effectively performs a set intersection based on element values.
     *
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of this operation
     */
    @Override
    public boolean retainAll(final double[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(DoubleList.of(a));
    }

    /**
     * Performs a batch removal operation based on the specified collection and complement flag.
     *
     * @param c the collection of elements to check against
     * @param complement if {@code true}, retain elements in c; if {@code false}, remove elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final DoubleList c, final boolean complement) {
        final double[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Double> set = c.toSet();

            for (int i = 0; i < size; i++) {
                if (set.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (c.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list and returns it.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public double delete(final int index) {
        rangeCheck(index);

        final double oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list.
     * The indices array is processed to handle removal correctly even for duplicate or unordered indices.
     * After removal, remaining elements are shifted to fill gaps, maintaining their relative order.
     *
     * @param indices the indices of elements to be removed. Can be empty, unordered, or contain duplicates.
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final double[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, 0d);
        }

        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes a range of elements from this list.
     * The range to be removed extends from fromIndex (inclusive) to toIndex (exclusive).
     * Shifts any subsequent elements to the left to fill the gap.
     *
     * @param fromIndex the starting index of the range to be removed (inclusive)
     * @param toIndex the ending index of the range to be removed (exclusive)
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public void deleteRange(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (fromIndex == toIndex) {
            return;
        }

        final int size = size();//NOSONAR
        final int newSize = size - (toIndex - fromIndex);

        if (toIndex < size) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size, 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     * 
     * <p><b>Usage Examples:</b></p> 
     * <pre>{@code
     * DoubleList list = DoubleList.of(0, 1, 2, 3, 4, 5);
     * list.moveRange(1, 3, 3);   // Moves elements [1, 2] to position starting at index 3
     * // Result: [0, 3, 4, 1, 2, 5]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionAfterMove — the zero-based index where the first element of the range will be placed after the move; 
     *      must be between 0 and size() - lengthOfRange, inclusive.
     * @throws IndexOutOfBoundsException if any index is out of bounds or if
     *         newPositionAfterMove would cause elements to be moved outside the list
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionAfterMove);
    }

    /**
     * Replaces a range of elements in this list with elements from the specified DoubleList.
     * The range to be replaced extends from fromIndex (inclusive) to toIndex (exclusive).
     * The size of the list may change if the replacement has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the starting index of the range to be replaced (inclusive)
     * @param toIndex the ending index of the range to be replaced (exclusive)
     * @param replacement the DoubleList whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final DoubleList replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        final int newSize = size - (toIndex - fromIndex) + replacement.size();

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.size() && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.size(), size - toIndex);
        }

        N.copy(replacement.elementData, 0, elementData, fromIndex, replacement.size());

        if (newSize < size) {
            N.fill(elementData, newSize, size, 0D);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with elements from the specified array.
     * The range to be replaced extends from fromIndex (inclusive) to toIndex (exclusive).
     * The size of the list may change if the replacement array has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the starting index of the range to be replaced (inclusive)
     * @param toIndex the ending index of the range to be replaced (exclusive)
     * @param replacement the array whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final double[] replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        final int newSize = size - (toIndex - fromIndex) + replacement.length;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.length && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.length, size - toIndex);
        }

        N.copy(replacement, 0, elementData, fromIndex, replacement.length);

        if (newSize < size) {
            N.fill(elementData, newSize, size, 0D);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of a specified value with a new value throughout the list.
     * Values are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal
     * @return the number of elements that were replaced
     */
    public int replaceAll(final double oldVal, final double newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (Double.compare(elementData[i], oldVal) == 0) {
                elementData[i] = newVal;

                result++;
            }
        }

        return result;
    }

    /**
     * Replaces each element of this list with the result of applying the specified operator to that element.
     * The operator is applied to each element in order from index 0 to size-1.
     *
     * @param operator the operator to apply to each element
     */
    public void replaceAll(final DoubleUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsDouble(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * Elements are tested with the predicate in order, and those for which it returns true
     * are replaced with newValue.
     *
     * @param predicate the predicate to test elements
     * @param newValue the value to replace matching elements with
     * @return {@code true} if at least one element was replaced
     */
    public boolean replaceIf(final DoublePredicate predicate, final double newValue) {
        boolean result = false;

        for (int i = 0, len = size(); i < len; i++) {
            if (predicate.test(elementData[i])) {
                elementData[i] = newValue;

                result = true;
            }
        }

        return result;
    }

    /**
     * Replaces all elements in this list with the specified value.
     * After this operation, every element in the list will have the same value.
     *
     * @param val the value to fill the list with
     */
    public void fill(final double val) {
        fill(0, size(), val);
    }

    /**
     * Replaces elements in the specified range with the specified value.
     * The range extends from fromIndex (inclusive) to toIndex (exclusive).
     *
     * @param fromIndex the starting index of the range to fill (inclusive)
     * @param toIndex the ending index of the range to fill (exclusive)
     * @param val the value to fill the range with
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public void fill(final int fromIndex, final int toIndex, final double val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code Double.compare(e, valueToFind) == 0}.
     * This comparison method correctly handles NaN and signed zero values.
     *
     * <p>This method performs a linear search through the list.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element, {@code false} otherwise
     */
    public boolean contains(final double valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Checks if this list contains any element from the specified DoubleList.
     * Returns {@code true} if there is at least one element that appears in both lists.
     *
     * @param c the DoubleList to check for common elements
     * @return {@code true} if this list contains any element from the specified list
     */
    @Override
    public boolean containsAny(final DoubleList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Checks if this list contains any element from the specified array.
     * Returns {@code true} if there is at least one element that appears in both this list and the array.
     *
     * @param a the array to check for common elements
     * @return {@code true} if this list contains any element from the specified array
     */
    @Override
    public boolean containsAny(final double[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Checks if this list contains all elements from the specified DoubleList.
     * Returns {@code true} only if every element in the specified list is also present in this list.
     * The frequency of elements is not considered; only presence is checked.
     *
     * @param c the DoubleList to check for containment
     * @return {@code true} if this list contains all elements from the specified list
     */
    @Override
    public boolean containsAll(final DoubleList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Double> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (!set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (!contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if this list contains all elements from the specified array.
     * Returns {@code true} only if every element in the specified array is also present in this list.
     * The frequency of elements is not considered; only presence is checked.
     *
     * @param a the array to check for containment
     * @return {@code true} if this list contains all elements from the specified array
     */
    @Override
    public boolean containsAll(final double[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Checks if this list and the specified DoubleList have no elements in common.
     * Returns {@code true} if the two lists are disjoint (i.e., have no common elements).
     *
     * @param c the DoubleList to check for disjointness
     * @return {@code true} if this list and the specified list have no elements in common
     */
    @Override
    public boolean disjoint(final DoubleList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Double> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if this list and the specified array have no elements in common.
     * Returns {@code true} if the list and array are disjoint (i.e., have no common elements).
     *
     * @param b the array to check for disjointness
     * @return {@code true} if this list and the specified array have no elements in common
     */
    @Override
    public boolean disjoint(final double[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both lists. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * DoubleList list2 = DoubleList.of(1.0, 2.0, 2.0, 4.0);
     * DoubleList result = list1.intersection(list2);   // result will be [1.0, 2.0]
     * // One occurrence of '1.0' (minimum count in both lists) and one occurrence of '2.0'
     *
     * DoubleList list3 = DoubleList.of(5.0, 5.0, 6.0);
     * DoubleList list4 = DoubleList.of(5.0, 7.0);
     * DoubleList result2 = list3.intersection(list4);   // result will be [5.0]
     * // One occurrence of '5.0' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list
     * @return a new DoubleList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(double[])
     * @see #difference(DoubleList)
     * @see #symmetricDifference(DoubleList)
     * @see N#intersection(double[], double[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public DoubleList intersection(final DoubleList b) {
        if (N.isEmpty(b)) {
            return new DoubleList();
        }

        final Multiset<Double> bOccurrences = b.toMultiset();

        final DoubleList c = new DoubleList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both sources. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * double[] array = new double[] {1.0, 2.0, 2.0, 4.0};
     * DoubleList result = list1.intersection(array);   // result will be [1.0, 2.0]
     * // One occurrence of '1.0' (minimum count in both sources) and one occurrence of '2.0'
     *
     * DoubleList list2 = DoubleList.of(5.0, 5.0, 6.0);
     * double[] array2 = new double[] {5.0, 7.0};
     * DoubleList result2 = list2.intersection(array2);   // result will be [5.0]
     * // One occurrence of '5.0' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list
     * @return a new DoubleList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(DoubleList)
     * @see #difference(double[])
     * @see #symmetricDifference(double[])
     * @see N#intersection(double[], double[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public DoubleList intersection(final double[] b) {
        if (N.isEmpty(b)) {
            return new DoubleList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element. If an element appears n times in this list
     * and m times in the specified list, the result will contain (n - m) occurrences of that element
     * (or zero if m &gt;= n). The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * DoubleList list2 = DoubleList.of(1.0, 4.0);
     * DoubleList result = list1.difference(list2);   // result will be [1.0, 2.0, 3.0]
     * // One '1.0' remains because list1 has two occurrences and list2 has one
     *
     * DoubleList list3 = DoubleList.of(5.0, 6.0);
     * DoubleList list4 = DoubleList.of(5.0, 5.0, 6.0);
     * DoubleList result2 = list3.difference(list4);   // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list
     * @return a new DoubleList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(double[])
     * @see #symmetricDifference(DoubleList)
     * @see #intersection(DoubleList)
     * @see N#difference(double[], double[])
     * @see N#difference(int[], int[])
     */
    @Override
    public DoubleList difference(final DoubleList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Double> bOccurrences = b.toMultiset();

        final DoubleList c = new DoubleList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list with the elements in this list but not in the specified array {@code b},
     * considering the number of occurrences of each element. If an element appears n times in this list
     * and m times in the specified array, the result will contain (n - m) occurrences of that element
     * (or zero if m &gt;= n). The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * double[] array = new double[] {1.0, 4.0};
     * DoubleList result = list1.difference(array);   // result will be [1.0, 2.0, 3.0]
     * // One '1.0' remains because list1 has two occurrences and array has one
     *
     * DoubleList list2 = DoubleList.of(5.0, 6.0);
     * double[] array2 = new double[] {5.0, 5.0, 6.0};
     * DoubleList result2 = list2.difference(array2);   // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param b the array to compare against this list
     * @return a new DoubleList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(DoubleList)
     * @see #symmetricDifference(double[])
     * @see #intersection(double[])
     * @see N#difference(double[], double[])
     * @see N#difference(double[], double[])
     */
    @Override
    public DoubleList difference(final double[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new DoubleList containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains the absolute difference
     * in occurrences between the two lists.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list that are not in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * DoubleList list2 = DoubleList.of(2.0, 3.0, 3.0, 4.0);
     * DoubleList result = list1.symmetricDifference(list2);
     * // result will contain: [1.0, 1.0, 3.0, 4.0]
     * // Elements explanation:
     * // - 1.0 appears twice in list1 and zero times in list2, so both occurrences remain
     * // - 2.0 appears once in each list, so it's removed from the result
     * // - 3.0 appears once in list1 and twice in list2, so one occurrence remains
     * // - 4.0 appears only in list2, so it remains in the result
     * }</pre>
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new DoubleList containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(double[])
     * @see #difference(DoubleList)
     * @see #intersection(DoubleList)
     * @see N#symmetricDifference(double[], double[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public DoubleList symmetricDifference(final DoubleList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Double> bOccurrences = b.toMultiset();
        final DoubleList c = new DoubleList(N.max(9, Math.abs(size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        for (int i = 0, len = b.size(); i < len; i++) {
            if (bOccurrences.remove(b.elementData[i])) {
                c.add(b.elementData[i]);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return c;
    }

    /**
     * Returns a new DoubleList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains the absolute difference
     * in occurrences between this list and the array.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array that are not in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list1 = DoubleList.of(1.0, 1.0, 2.0, 3.0);
     * double[] array = new double[] {2.0, 3.0, 3.0, 4.0};
     * DoubleList result = list1.symmetricDifference(array);
     * // result will contain: [1.0, 1.0, 3.0, 4.0]
     * // Elements explanation:
     * // - 1.0 appears twice in list1 and zero times in array, so both occurrences remain
     * // - 2.0 appears once in each source, so it's removed from the result
     * // - 3.0 appears once in list1 and twice in array, so one occurrence remains
     * // - 4.0 appears only in array, so it remains in the result
     * }</pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new DoubleList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(DoubleList)
     * @see #difference(double[])
     * @see #intersection(double[])
     * @see N#symmetricDifference(double[], double[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public DoubleList symmetricDifference(final double[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Counts the number of occurrences of the specified value in this list.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param valueToFind the value to count occurrences of
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final double valueToFind) {
        if (size == 0) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0; i < size; i++) {
            if (Double.compare(elementData[i], valueToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or {@code N.INDEX_NOT_FOUND} (-1) if this list does not contain the element. The search starts from index 0.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element, or {@code N.INDEX_NOT_FOUND} (-1) if not found
     */
    public int indexOf(final double valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * starting the search at the specified index, or {@code N.INDEX_NOT_FOUND} (-1) if the element is not found.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start the search from (inclusive)
     * @return the index of the first occurrence of the specified element at or after fromIndex,
     *         or {@code N.INDEX_NOT_FOUND} (-1) if not found
     */
    public int indexOf(final double valueToFind, final int fromIndex) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (N.equals(elementData[i], valueToFind)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. The search starts from the end of the list.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element, or -1 if not found
     */
    public int lastIndexOf(final double valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     * Elements are compared using {@code Double.compare} for accurate floating-point comparison.
     * 
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *        The search includes this index and proceeds towards index 0.
     * @return the index of the last occurrence of the specified element at or before startIndexFromBack,
     *         or -1 if not found
     */
    public int lastIndexOf(final double valueToFind, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
            if (N.equals(elementData[i], valueToFind)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the minimum element in this list wrapped in an OptionalDouble.
     * If the list is empty, returns an empty OptionalDouble.
     * For lists containing NaN values, the behavior follows the standard double comparison rules.
     *
     * @return an OptionalDouble containing the minimum element, or empty if the list is empty
     */
    public OptionalDouble min() {
        return size() == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list wrapped in an OptionalDouble.
     * If the range is empty (fromIndex == toIndex), returns an empty OptionalDouble.
     * For ranges containing NaN values, the behavior follows the standard double comparison rules.
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return an OptionalDouble containing the minimum element in the range, or empty if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public OptionalDouble min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list wrapped in an OptionalDouble.
     * If the list is empty, returns an empty OptionalDouble.
     * For lists containing NaN values, the behavior follows the standard double comparison rules.
     *
     * @return an OptionalDouble containing the maximum element, or empty if the list is empty
     */
    public OptionalDouble max() {
        return size() == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list wrapped in an OptionalDouble.
     * If the range is empty (fromIndex == toIndex), returns an empty OptionalDouble.
     * For ranges containing NaN values, the behavior follows the standard double comparison rules.
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return an OptionalDouble containing the maximum element in the range, or empty if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public OptionalDouble max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * @return an OptionalDouble containing the median value if the list is non-empty, or an empty OptionalDouble if the list is empty
     */
    public OptionalDouble median() {
        return size() == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.median(elementData, 0, size));
    }

    /**
     * Returns the median value of elements within the specified range of this list.
     * 
     * <p>The median is computed for elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * For ranges with an odd number of elements, this returns the exact middle element when sorted.
     * For ranges with an even number of elements, this returns the lower of the two middle elements.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to calculate median for
     * @param toIndex the ending index (exclusive) of the range to calculate median for
     * @return an OptionalDouble containing the median value if the range is non-empty, or an empty OptionalDouble if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalDouble median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element in this list, in the order elements
     * occur in the list, until all elements have been processed or the action throws
     * an exception.
     * 
     * <p>This method iterates through all elements from index 0 to size-1.</p>
     *
     * @param action the action to be performed for each element. Must not be {@code null}.
     */
    public void forEach(final DoubleConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list,
     * in the order elements occur in the list (or reverse order if fromIndex &gt; toIndex),
     * until all elements have been processed or the action throws an exception.
     * 
     * <p>If {@code fromIndex <= toIndex}, iteration proceeds from fromIndex (inclusive)
     * to toIndex (exclusive). If {@code fromIndex > toIndex}, iteration proceeds from
     * fromIndex (inclusive) down to toIndex (exclusive) in reverse order.</p>
     * 
     * <p>Special case: if {@code toIndex == -1} and {@code fromIndex > toIndex},
     * iteration starts from fromIndex down to 0 (inclusive).</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be processed
     * @param toIndex the index of the last element (exclusive) to be processed,
     *                or -1 for reverse iteration to the beginning
     * @param action the action to be performed for each element. Must not be {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || (fromIndex > toIndex && toIndex != -1)})
     */
    public void forEach(final int fromIndex, final int toIndex, final DoubleConsumer action) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex), size);

        if (size > 0) {
            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(elementData[i]);
                }
            } else {
                for (int i = N.min(size - 1, fromIndex); i > toIndex; i--) {
                    action.accept(elementData[i]);
                }
            }
        }
    }

    /**
     * Returns an {@code OptionalDouble} containing the first element of this list,
     * or an empty {@code OptionalDouble} if this list is empty.
     * 
     * <p>This method provides a null-safe way to access the first element without
     * risking an exception when the list is empty.</p>
     *
     * @return an {@code OptionalDouble} containing the first element if present,
     *         otherwise an empty {@code OptionalDouble}
     */
    public OptionalDouble first() {
        return size() == 0 ? OptionalDouble.empty() : OptionalDouble.of(elementData[0]);
    }

    /**
     * Returns an {@code OptionalDouble} containing the last element of this list,
     * or an empty {@code OptionalDouble} if this list is empty.
     * 
     * <p>This method provides a null-safe way to access the last element without
     * risking an exception when the list is empty.</p>
     *
     * @return an {@code OptionalDouble} containing the last element if present,
     *         otherwise an empty {@code OptionalDouble}
     */
    public OptionalDouble last() {
        return size() == 0 ? OptionalDouble.empty() : OptionalDouble.of(elementData[size() - 1]);
    }

    /**
     * Returns a new {@code DoubleList} containing only the distinct elements from the
     * specified range of this list, preserving their original order of first occurrence.
     * 
     * <p>This method creates a new list containing each unique element from the specified
     * range exactly once, in the order they first appear. Duplicate values are removed.</p>
     * 
     * <p>For example, if the range contains [1.0, 2.0, 1.0, 3.0, 2.0], the returned
     * list will contain [1.0, 2.0, 3.0].</p>
     *
     * @param fromIndex the index of the first element (inclusive) to process
     * @param toIndex the index of the last element (exclusive) to process
     * @return a new {@code DoubleList} containing the distinct elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public DoubleList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains any duplicate elements.
     * 
     * <p>This method returns {@code true} if any value appears more than once
     * in the list, {@code false} if all elements are unique.</p>
     * 
     * <p>The comparison uses {@code ==} for primitive doubles, which means that
     * {@code NaN} values are considered equal to themselves for this purpose.</p>
     *
     * @return {@code true} if this list contains at least one duplicate element,
     *         {@code false} if all elements are unique
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * 
     * <p>Returns {@code true} if the list is empty, contains only one element,
     * or all elements are arranged in non-decreasing order (each element is less
     * than or equal to the next element).</p>
     * 
     * <p>This method considers {@code NaN} values according to their natural
     * ordering as defined by {@code Double.compare()}.</p>
     *
     * @return {@code true} if this list is sorted in ascending order,
     *         {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts the elements of this list into ascending order.
     * 
     * <p>This method modifies the list in-place, arranging all elements according
     * to their natural ordering. The sort is stable: equal elements will not be
     * reordered as a result of the sort.</p>
     * 
     * <p>The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy,
     * Jon Bentley, and Joshua Bloch. This algorithm offers O(n log(n)) performance
     * on many data sets.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no sorting is performed.</p>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list into ascending order using a parallel sort algorithm.
     * 
     * <p>This method modifies the list in-place, arranging all elements according
     * to their natural ordering. The parallel sort algorithm divides the array into
     * sub-arrays that are sorted in parallel and then merged.</p>
     * 
     * <p>This method is beneficial for large lists on multi-core systems. For small
     * lists, the overhead of parallelization may make it slower than {@link #sort()}.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no sorting is performed.</p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list into descending order.
     * 
     * <p>This method first sorts the list in ascending order using {@link #sort()},
     * then reverses the entire list to achieve descending order. This approach
     * maintains stability for equal elements.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no sorting is performed.</p>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value in this sorted list using the binary search algorithm.
     * 
     * <p>The list must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined. If the list contains multiple
     * elements with the specified value, there is no guarantee which one will be found.</p>
     * 
     * <p>This method runs in O(log n) time for a list of size n.</p>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key, if it is contained in the list;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is
     *         defined as the point at which the key would be inserted into the list:
     *         the index of the first element greater than the key, or {@code size()}
     *         if all elements in the list are less than the specified key.
     */
    public int binarySearch(final double valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified value within the specified range of this sorted list
     * using the binary search algorithm.
     * 
     * <p>The range must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined. If the range contains multiple
     * elements with the specified value, there is no guarantee which one will be found.</p>
     * 
     * <p>This method runs in O(log(toIndex - fromIndex)) time.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param valueToFind the value to search for
     * @return the index of the search key, if it is contained in the specified range;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is
     *         defined as the point at which the key would be inserted into the range:
     *         the index of the first element greater than the key, or {@code toIndex}
     *         if all elements in the range are less than the specified key.
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final double valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * 
     * <p>This method modifies the list in-place. After calling this method,
     * the first element becomes the last, the second element becomes the
     * second-to-last, and so on.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no reversal is performed.</p>
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * 
     * <p>This method modifies the list in-place, reversing only the elements
     * between {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * Elements outside this range are not affected.</p>
     * 
     * <p>If the range contains fewer than 2 elements, no reversal is performed.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be reversed
     * @param toIndex the index of the last element (exclusive) to be reversed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public void reverse(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            N.reverse(elementData, fromIndex, toIndex);
        }
    }

    /**
     * Rotates all elements in this list by the specified distance.
     * After calling rotate(distance), the element at index i will be moved to
     * index (i + distance) % size. 
     * 
     * <p>Positive values of distance rotate elements towards higher indices (right rotation),
     * while negative values rotate towards lower indices (left rotation).
     * The list is modified in place.</p>
     *
     * @param distance the distance to rotate the list. Positive values rotate right,
     *                 negative values rotate left
     * @see N#rotate(int[], int)
     */
    @Override
    public void rotate(final int distance) {
        if (size > 1) {
            N.rotate(elementData, 0, size, distance);
        }
    }

    /**
     * Randomly permutes the elements in this list using a default source of randomness.
     * 
     * <p>All permutations occur with approximately equal likelihood. This method
     * runs in linear time.</p>
     * 
     * <p>This implementation traverses the list backwards, from the last element
     * up to the second, repeatedly swapping a randomly selected element into the
     * current position.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no shuffling is performed.</p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly permutes the elements in this list using the specified source of randomness.
     * 
     * <p>All permutations occur with equal likelihood assuming that the source of
     * randomness is fair. This method runs in linear time.</p>
     * 
     * <p>This implementation traverses the list backwards, from the last element
     * up to the second, repeatedly swapping a randomly selected element into the
     * current position using the provided {@code Random} instance.</p>
     * 
     * <p>If the list contains fewer than 2 elements, no shuffling is performed.</p>
     *
     * @param rnd the source of randomness to use to shuffle the list. Must not be {@code null}.
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * 
     * <p>After calling this method, the element previously at position {@code i}
     * will be at position {@code j}, and vice versa.</p>
     *
     * @param i the index of one element to be swapped
     * @param j the index of the other element to be swapped
     * @throws IndexOutOfBoundsException if either {@code i} or {@code j}
     *         is out of range ({@code i < 0 || i >= size() || j < 0 || j >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a shallow copy of this list.
     * 
     * <p>The returned list will have the same size and contain the same elements
     * in the same order as this list. The new list will have its own backing array,
     * so changes to the returned list will not affect this list, and vice versa.</p>
     *
     * @return a new {@code DoubleList} containing all elements of this list
     */
    @Override
    public DoubleList copy() {
        return new DoubleList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a shallow copy of the portion of this list between the specified
     * {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * 
     * <p>The returned list will contain {@code toIndex - fromIndex} elements.
     * The new list will have its own backing array, so changes to the returned
     * list will not affect this list, and vice versa.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be copied
     * @param toIndex the index of the last element (exclusive) to be copied
     * @return a new {@code DoubleList} containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public DoubleList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new DoubleList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a shallow copy of the portion of this list between the specified
     * {@code fromIndex} and {@code toIndex} with the specified step.
     * 
     * <p>The returned list will contain elements at indices {@code fromIndex},
     * {@code fromIndex + step}, {@code fromIndex + 2*step}, ..., up to but not
     * including {@code toIndex}. If {@code step} is negative and {@code fromIndex > toIndex},
     * elements are copied in reverse order.</p>
     * 
     * <p>For example, copying elements from index 0 to 10 with step 2 will return
     * elements at indices 0, 2, 4, 6, 8.</p>
     *
     * @param fromIndex the index of the first element to be copied
     * @param toIndex the index boundary (exclusive) for copying
     * @param step the increment between successive elements to be copied.
     *             Must not be zero.
     * @return a new {@code DoubleList} containing the selected elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if {@code step} is zero
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public DoubleList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new DoubleList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns a list of {@code DoubleList} instances, each containing a consecutive
     * subsequence of elements from the specified range of this list.
     * 
     * <p>This method divides the specified range into chunks of the specified size
     * (except possibly the last chunk, which may be smaller). Each chunk is returned
     * as a separate {@code DoubleList}.</p>
     * 
     * <p>For example, splitting a range containing [1, 2, 3, 4, 5, 6, 7] with
     * chunk size 3 returns [[1, 2, 3], [4, 5, 6], [7]].</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index of the last element (exclusive) to be included
     * @param chunkSize the desired size of each subsequence. Must be positive.
     * @return a list of {@code DoubleList} instances, each containing a chunk of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize <= 0}
     */
    @Override
    public List<DoubleList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<double[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<DoubleList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this list to be the list's current size.
     * 
     * <p>This method minimizes the storage of a {@code DoubleList} instance.
     * If the backing array has excess capacity, a new array of the exact size
     * is allocated and the elements are copied into it.</p>
     * 
     * <p>An application can use this operation to minimize the storage of a
     * {@code DoubleList} instance after it has finished adding elements.</p>
     *
     * @return this {@code DoubleList} instance (for method chaining)
     */
    @Override
    public DoubleList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Removes all elements from this list. The list will be empty after this call returns.
     * The capacity of the list is not changed.
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, 0);
        }

        size = 0;
    }

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a {@code List<Double>} containing all elements of this list.
     * 
     * <p>This method creates a new {@code ArrayList<Double>} and copies all
     * primitive double values from this list, boxing them into {@code Double}
     * objects. The returned list is independent of this list.</p>
     * 
     * <p>This is equivalent to calling {@code boxed(0, size())}.</p>
     *
     * @return a new {@code List<Double>} containing all elements of this list
     */
    @Override
    public List<Double> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a {@code List<Double>} containing the elements in the specified
     * range of this list.
     * 
     * <p>This method creates a new {@code ArrayList<Double>} and copies the
     * primitive double values from the specified range, boxing them into
     * {@code Double} objects. The returned list is independent of this list.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index of the last element (exclusive) to be included
     * @return a new {@code List<Double>} containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public List<Double> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Double> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new double array containing all elements of this list
     */
    @Override
    public double[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of the collection to be returned
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index of the last element (exclusive) to be included
     * @param supplier a function that creates a new collection instance with the
     *                 specified initial capacity
     * @return a collection containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Double>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final C c = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            c.add(elementData[i]);
        }

        return c;
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index of the last element (exclusive) to be included
     * @param supplier a function that creates a new {@code Multiset} instance with the
     *                 specified initial capacity
     * @return a {@code Multiset} containing the specified range of elements with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Double> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Double>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Double> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * 
     * <p>The returned iterator is fail-fast: if the list is structurally modified
     * at any time after the iterator is created, the iterator may throw a
     * {@code ConcurrentModificationException}. This behavior is not guaranteed
     * and should not be relied upon for correctness.</p>
     *
     * @return a {@code DoubleIterator} over the elements in this list
     */
    @Override
    public DoubleIterator iterator() {
        if (isEmpty()) {
            return DoubleIterator.EMPTY;
        }

        return DoubleIterator.of(elementData, 0, size);
    }

    /**
     * Returns a {@code DoubleStream} with this list as its source.
     * 
     * <p>This method creates a stream that will iterate over all elements in
     * this list in the order they appear.</p>
     *
     * @return a sequential {@code DoubleStream} over the elements in this list
     */
    public DoubleStream stream() {
        return DoubleStream.of(elementData, 0, size());
    }

    /**
     * Returns a {@code DoubleStream} with the specified range of this
     * list as its source.
     * 
     * <p>This method creates a stream that will iterate over the elements from
     * {@code fromIndex} (inclusive) to {@code toIndex} (exclusive) in the order
     * they appear in the list.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index of the last element (exclusive) to be included
     * @return a sequential {@code DoubleStream} over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public DoubleStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return DoubleStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * 
     * <p>This method provides direct access to the first element without creating
     * an {@code Optional} wrapper, but will throw an exception if the list is empty.</p>
     *
     * @return the first double value in the list
     * @throws NoSuchElementException if this list is empty
     */
    public double getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * 
     * <p>This method provides direct access to the last element without creating
     * an {@code Optional} wrapper, but will throw an exception if the list is empty.</p>
     *
     * @return the last double value in the list
     * @throws NoSuchElementException if this list is empty
     */
    public double getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * 
     * <p>Shifts the element currently at position 0 (if any) and any subsequent
     * elements to the right (adds one to their indices). This operation has
     * O(n) time complexity where n is the size of the list.</p>
     *
     * @param e the element to add at the beginning of the list
     */
    public void addFirst(final double e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This is equivalent to calling {@code add(e)} and has amortized
     * constant time complexity.</p>
     *
     * @param e the element to add at the end of the list
     */
    public void addLast(final double e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * 
     * <p>Shifts any subsequent elements to the left (subtracts one from their
     * indices). This operation has O(n) time complexity where n is the size
     * of the list.</p>
     *
     * @return the first double value that was removed from the list
     * @throws NoSuchElementException if this list is empty
     */
    public double removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * 
     * <p>This operation has O(1) time complexity.</p>
     *
     * @return the last double value that was removed from the list
     * @throws NoSuchElementException if this list is empty
     */
    public double removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns the hash code value for this list.
     * 
     * <p>The hash code is calculated based on the elements in the list and their
     * order. Two lists with the same elements in the same order will have the
     * same hash code.</p>
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality.
     * 
     * <p>Returns {@code true} if and only if the specified object is also a
     * {@code DoubleList}, both lists have the same size, and all corresponding
     * pairs of elements in the two lists are equal. Two double values are
     * considered equal if they have the same bit pattern (which means that
     * {@code NaN} equals {@code NaN} and {@code -0.0} does not equal {@code 0.0}
     * for this method).</p>
     *
     * @param obj the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list,
     *         {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DoubleList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * 
     * <p>The string representation consists of the list's elements in order,
     * enclosed in square brackets ("[]"). Adjacent elements are separated by
     * the characters ", " (comma and space). Elements are converted to strings
     * using {@code String.valueOf(double)}.</p>
     * 
     * <p>If the list is empty, returns "[]".</p>
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return size == 0 ? Strings.STR_FOR_EMPTY_ARRAY : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity < 0 || minCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new double[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}
