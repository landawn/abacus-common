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
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

/**
 * A high-performance, resizable array implementation for primitive long values that provides
 * specialized operations optimized for 64-bit integer data types. This class extends {@link PrimitiveList}
 * to offer memory-efficient storage and operations that avoid the boxing overhead associated with
 * {@code List<Long>}, making it ideal for applications requiring intensive long integer array
 * manipulation with optimal performance characteristics.
 *
 * <p>LongList is specifically designed for scenarios involving large collections of long
 * values such as high-precision numerical computations, timestamp management, unique identifier
 * storage, large-scale data processing, and performance-critical applications requiring 64-bit
 * integer precision. The implementation uses a compact long array as the underlying storage
 * mechanism, providing direct primitive access without wrapper object allocation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct long primitive storage without Long wrapper allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact long array storage with minimal memory overhead</li>
 *   <li><b>64-bit Precision:</b> Full support for long integer range (-2^63 to 2^63-1)</li>
 *   <li><b>High Performance:</b> Optimized algorithms for long-specific operations</li>
 *   <li><b>Rich Mathematical API:</b> Statistical operations like min, max, median</li>
 *   <li><b>Set Operations:</b> Efficient intersection, union, and difference operations</li>
 *   <li><b>Range Generation:</b> Built-in support for arithmetic progressions and sequences</li>
 *   <li><b>Random Access:</b> O(1) element access and modification by index</li>
 *   <li><b>Dynamic Sizing:</b> Automatic capacity management with intelligent growth</li>
 *   <li><b>Type Conversions:</b> Seamless conversion to other numeric primitive lists</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Timestamp Management:</b> Storing millisecond timestamps, epoch times, duration values</li>
 *   <li><b>Unique Identifiers:</b> Database IDs, user IDs, transaction IDs, and other large identifiers</li>
 *   <li><b>High-Precision Computing:</b> Mathematical calculations requiring 64-bit integer precision</li>
 *   <li><b>Financial Systems:</b> Currency amounts in smallest units (e.g., cents, satoshis)</li>
 *   <li><b>Big Data Processing:</b> Large-scale data analysis with 64-bit counters and measurements</li>
 *   <li><b>Cryptography:</b> Large integer operations, hash values, and cryptographic keys</li>
 *   <li><b>Scientific Computing:</b> Large numerical datasets, simulation data, measurement values</li>
 *   <li><b>Performance Monitoring:</b> Nanosecond timestamps, memory usage, performance counters</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and initializing long lists
 * LongList timestamps = LongList.of(1640995200000L, 1640995260000L, 1640995320000L);
 * LongList range = LongList.range(1L, 1000000L);   // [1, 2, 3, ..., 999999]
 * LongList sequence = LongList.range(0L, 100L, 5L);   // [0, 5, 10, 15, ..., 95]
 * LongList userIds = new LongList(10000);   // Pre-sized for performance
 *
 * // Basic operations
 * timestamps.add(System.currentTimeMillis());   // Add current timestamp
 * long firstTime = timestamps.get(0);   // Access by index
 * timestamps.set(1, System.nanoTime());   // Modify with nanosecond precision
 *
 * // Mathematical operations for large numbers
 * OptionalLong min = timestamps.min();   // Find earliest timestamp
 * OptionalLong max = timestamps.max();   // Find latest timestamp
 * OptionalLong median = timestamps.median();   // Calculate median timestamp
 *
 * // Set operations for data analysis
 * LongList set1 = LongList.of(100L, 200L, 300L, 400L);
 * LongList set2 = LongList.of(300L, 400L, 500L, 600L);
 * LongList intersection = set1.intersection(set2);   // [300, 400]
 * LongList difference = set1.difference(set2);   // [100, 200]
 *
 * // High-performance sorting and searching
 * timestamps.sort();   // Sort chronologically
 * timestamps.parallelSort();   // Parallel sort for large datasets
 * int index = timestamps.binarySearch(1640995200000L);   // Fast lookup
 *
 * // Type conversions for different precision needs
 * DoubleList doubleValues = timestamps.toDoubleList();   // Convert to double precision
 * FloatList floatValues = timestamps.toFloatList();   // Convert to float (with precision loss)
 * long[] primitiveArray = timestamps.toArray();   // To primitive array
 * List<Long> boxedList = timestamps.boxed();   // To boxed collection
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
 *   <li><b>vs List&lt;Long&gt;:</b> ~3x less memory usage (no Long wrapper objects)</li>
 *   <li><b>Capacity Management:</b> 1.75x growth factor balances memory and performance</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} (typically Integer.MAX_VALUE - 8)</li>
 * </ul>
 *
 * <p><b>Long-Specific Operations:</b>
 * <ul>
 *   <li><b>Range Generation:</b> {@code range()}, {@code rangeClosed()} for arithmetic sequences</li>
 *   <li><b>Mathematical Functions:</b> {@code min()}, {@code max()}, {@code median()}</li>
 *   <li><b>Type Conversions:</b> {@code toFloatList()}, {@code toDoubleList()}</li>
 *   <li><b>Random Generation:</b> {@code random()} methods for test data and simulations</li>
 *   <li><b>Parallel Operations:</b> {@code parallelSort()} for large dataset optimization</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li><b>{@code of(long...)}:</b> Create from varargs array</li>
 *   <li><b>{@code copyOf(long[])}:</b> Create defensive copy of array</li>
 *   <li><b>{@code range(long, long)}:</b> Create arithmetic sequence [start, end)</li>
 *   <li><b>{@code rangeClosed(long, long)}:</b> Create arithmetic sequence [start, end]</li>
 *   <li><b>{@code repeat(long, int)}:</b> Create with repeated values</li>
 *   <li><b>{@code random(int)}:</b> Create with random long values</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toArray()}:</b> Convert to primitive long array</li>
 *   <li><b>{@code toFloatList()}:</b> Convert to FloatList (potential precision loss)</li>
 *   <li><b>{@code toDoubleList()}:</b> Convert to DoubleList with floating-point precision</li>
 *   <li><b>{@code boxed()}:</b> Convert to {@code List<Long>}</li>
 *   <li><b>{@code stream()}:</b> Convert to LongStream for functional processing</li>
 * </ul>
 *
 * <p><b>Deque-like Operations:</b>
 * <ul>
 *   <li><b>{@code addFirst(long)}:</b> Insert at beginning (O(n) operation)</li>
 *   <li><b>{@code addLast(long)}:</b> Insert at end (O(1) amortized)</li>
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
 *   <li><b>Efficient Format:</b> Optimized serialization of long arrays</li>
 *   <li><b>Cross-Platform:</b> Platform-independent serialized format</li>
 * </ul>
 *
 * <p><b>Integration with Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Collection Compatibility:</b> Seamless conversion to standard collections</li>
 *   <li><b>Utility Integration:</b> Works with Collections utility methods via boxed()</li>
 *   <li><b>Stream API:</b> Full integration with LongStream for functional processing</li>
 * </ul>
 *
 * <p><b>Mathematical and Statistical Operations:</b>
 * <ul>
 *   <li><b>Aggregation:</b> Sum, min, max operations via stream API</li>
 *   <li><b>Central Tendency:</b> Median calculation with efficient sorting</li>
 *   <li><b>Occurrence Counting:</b> {@code occurrencesOf()} for frequency analysis</li>
 *   <li><b>Duplicate Detection:</b> {@code hasDuplicates()}, {@code removeDuplicates()}</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs List&lt;Long&gt;:</b> 3x less memory, significantly faster operations</li>
 *   <li><b>vs long[]:</b> Dynamic sizing, rich API, set operations, statistical functions</li>
 *   <li><b>vs ArrayList&lt;Long&gt;:</b> No boxing overhead, primitive-specific methods</li>
 *   <li><b>vs IntList:</b> Double the range, suitable for large identifiers and timestamps</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code LongList} when working primarily with long primitives</li>
 *   <li>Specify initial capacity for known data sizes to avoid resizing</li>
 *   <li>Use bulk operations ({@code addAll}, {@code removeAll}) instead of loops</li>
 *   <li>Convert to boxed collections only when required for API compatibility</li>
 *   <li>Leverage parallel sorting for large datasets (>10,000 elements)</li>
 *   <li>Use set operations instead of manual intersection/difference calculations</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Pre-size lists with known capacity using constructor or {@code ensureCapacity()}</li>
 *   <li>Use {@code addLast()} instead of {@code addFirst()} for better performance</li>
 *   <li>Sort data before using {@code binarySearch()} for O(log n) lookups</li>
 *   <li>Use {@code parallelSort()} for large datasets to leverage multi-core processors</li>
 *   <li>Consider {@code stream()} API for complex transformations and filtering</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Timestamp Collections:</b> {@code LongList timestamps = new LongList(expectedEvents);}</li>
 *   <li><b>ID Management:</b> {@code LongList userIds = LongList.range(1L, maxUsers + 1L);}</li>
 *   <li><b>Financial Data:</b> {@code LongList amounts = prices.stream().mapToLong(p -> p * 100).collect(...);}</li>
 *   <li><b>Performance Monitoring:</b> {@code LongList timings = LongList.of(System.nanoTime());}</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link PrimitiveList}:</b> Abstract base class for all primitive list types</li>
 *   <li><b>{@link IntList}:</b> Similar implementation for int primitives</li>
 *   <li><b>{@link DoubleList}:</b> Similar implementation for double primitives</li>
 *   <li><b>{@link LongIterator}:</b> Specialized iterator for long primitives</li>
 *   <li><b>{@link LongStream}:</b> Functional processing of long sequences</li>
 * </ul>
 *
 * <p><b>Example: Timestamp Analysis</b>
 * <pre>{@code
 * // Collect and analyze system timestamps
 * LongList systemEvents = new LongList(1000);
 *
 * // Record events with nanosecond precision
 * systemEvents.add(System.nanoTime());
 * // ... record more events
 *
 * // Analyze timing data
 * systemEvents.sort();   // Sort chronologically
 * OptionalLong firstEvent = systemEvents.first();   // Earliest event
 * OptionalLong lastEvent = systemEvents.last();   // Latest event
 * OptionalLong medianTime = systemEvents.median();   // Median timestamp
 *
 * // Calculate duration and intervals
 * long totalDuration = lastEvent.orElse(0L) - firstEvent.orElse(0L);
 * LongStream intervals = systemEvents.stream()
 *     .skip(1)
 *     .map(current -> current - systemEvents.get(systemEvents.indexOf(current) - 1));
 *
 * // Performance analysis
 * double avgInterval = intervals.average().orElse(0.0);
 * long maxInterval = intervals.max().orElse(0L);
 * }</pre>
 *
 * @see PrimitiveList
 * @see LongIterator
 * @see LongStream
 * @see IntList
 * @see DoubleList
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see java.util.List
 * @see java.util.RandomAccess
 * @see java.io.Serializable
 */
public final class LongList extends PrimitiveList<Long, long[], LongList> {

    @Serial
    private static final long serialVersionUID = -7764836427712181163L;

    static final Random RAND = new SecureRandom();

    /**
     * The array buffer into which the elements of the LongList are stored.
     */
    private long[] elementData = N.EMPTY_LONG_ARRAY;

    /**
     * The size of the LongList (the number of elements it contains).
     */
    private int size = 0;

    /**
     * Constructs an empty LongList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     */
    public LongList() {
    }

    /**
     * Constructs an empty LongList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public LongList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_LONG_ARRAY : new long[initialCapacity];
    }

    /**
     * Constructs a LongList containing the elements of the specified array.
     * The list will use the provided array as its internal storage without copying,
     * making this operation O(1) in time complexity.
     * 
     * <p>Changes to the provided array after construction will be reflected in this list
     * and vice versa, as they share the same underlying array.
     *
     * @param a the array whose elements are to be placed into this list. Must not be {@code null}.
     */
    public LongList(final long[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a LongList using the specified array as the internal storage with the specified size.
     * The list will use the provided array directly without copying.
     * 
     * <p>This constructor allows creating a list that uses only a portion of the provided array.
     * The size parameter must not exceed the array length.
     * 
     * <p>Changes to the provided array after construction will be reflected in this list
     * and vice versa, as they share the same underlying array.
     *
     * @param a the array to be used as the internal storage for this list. Must not be {@code null}.
     * @param size the number of elements in the list, must be between 0 and a.length (inclusive)
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public LongList(final long[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new LongList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new LongList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static LongList of(final long... a) {
        return new LongList(N.nullToEmpty(a));
    }

    /**
     * Creates a new LongList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * @param a the array of long values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new LongList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static LongList of(final long[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new LongList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new LongList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(long...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new LongList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static LongList copyOf(final long[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new LongList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new LongList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static LongList copyOf(final long[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a LongList containing a sequence of long values in the specified range.
     * 
     * <p>The sequence starts at startInclusive and increments by 1 until reaching
     * endExclusive. If startInclusive equals endExclusive, an empty list is returned.
     * If startInclusive is greater than endExclusive, the sequence decrements by 1.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList.range(1, 5);   // returns [1, 2, 3, 4]
     * LongList.range(5, 1);   // returns [5, 4, 3, 2]
     * LongList.range(3, 3);   // returns []
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new LongList containing the sequence of values
     */
    public static LongList range(final long startInclusive, final long endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a LongList containing a sequence of long values in the specified range with the given step.
     * 
     * <p>The sequence starts at startInclusive and increments by the step value until the value
     * would exceed endExclusive (or fall below it if step is negative).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList.range(0, 10, 2);    // returns [0, 2, 4, 6, 8]
     * LongList.range(10, 0, -2);   // returns [10, 8, 6, 4, 2]
     * LongList.range(1, 10, 3);    // returns [1, 4, 7]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new LongList containing the sequence of values
     * @throws IllegalArgumentException if by is zero
     */
    public static LongList range(final long startInclusive, final long endExclusive, final long by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a LongList containing a sequence of long values in the specified closed range.
     * 
     * <p>The sequence starts at startInclusive and increments by 1 until reaching
     * endInclusive. Unlike {@link #range(long, long)}, the end value is included.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList.rangeClosed(1, 4);   // returns [1, 2, 3, 4]
     * LongList.rangeClosed(4, 1);   // returns [4, 3, 2, 1]
     * LongList.rangeClosed(3, 3);   // returns [3]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new LongList containing the sequence of values
     */
    public static LongList rangeClosed(final long startInclusive, final long endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a LongList containing a sequence of long values in the specified closed range with the given step.
     * 
     * <p>The sequence starts at startInclusive and increments by the step value until the value
     * would exceed endInclusive (or fall below it if step is negative). The end value is included
     * if it's exactly reachable by the step increments.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList.rangeClosed(0, 10, 2);    // returns [0, 2, 4, 6, 8, 10]
     * LongList.rangeClosed(10, 0, -2);   // returns [10, 8, 6, 4, 2, 0]
     * LongList.rangeClosed(1, 10, 3);    // returns [1, 4, 7, 10]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new LongList containing the sequence of values
     * @throws IllegalArgumentException if by is zero
     */
    public static LongList rangeClosed(final long startInclusive, final long endInclusive, final long by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a LongList containing the specified element repeated the given number of times.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList.repeat(5, 3);   // returns [5, 5, 5]
     * LongList.repeat(0, 0);   // returns []
     * LongList.repeat(7, 1);   // returns [7]
     * }</pre>
     *
     * @param element the long value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new LongList containing the repeated elements
     * @throws IllegalArgumentException if len is negative
     */
    public static LongList repeat(final long element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a LongList filled with random long values.
     * 
     * <p>The random values are generated using a secure random number generator
     * and can be any valid long value (positive or negative).
     *
     * @param len the number of random elements to generate. Must be non-negative.
     * @return a new LongList containing random long values
     * @throws IllegalArgumentException if len is negative
     */
    public static LongList random(final int len) {
        final long[] a = new long[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextLong();
        }

        return of(a);
    }

    /**
     * Returns the underlying long array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.</p>
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal long array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public long[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public long get(final int index) {
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
    public long set(final int index, final long e) {
        rangeCheck(index);

        final long oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>The list will automatically grow if necessary to accommodate the new element.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the long value to be appended to this list
     */
    public void add(final long e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>The list will automatically grow if necessary to accommodate the new element.
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the long value to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final long e) {
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
     * Appends all elements from the specified LongList to the end of this list.
     * 
     * <p>The elements are appended in the order they appear in the specified list.
     * This list will grow as necessary to accommodate all new elements.
     *
     * @param c the LongList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     *         (returns {@code false} if the specified list is empty)
     */
    @Override
    public boolean addAll(final LongList c) {
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
     * Inserts all elements from the specified LongList into this list at the specified position.
     * 
     * <p>Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices). The new elements will appear
     * in this list in the order they appear in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the LongList containing elements to be inserted into this list
     * @return {@code true} if this list changed as a result of the call
     *         (returns {@code false} if the specified list is empty)
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final LongList c) {
        rangeCheckForAdd(index);

        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew);   // Increments modCount

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
     * 
     * <p>The elements are appended in the order they appear in the array.
     * This list will grow as necessary to accommodate all new elements.
     *
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     *         (returns {@code false} if the specified array is {@code null} or empty)
     */
    @Override
    public boolean addAll(final long[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list at the specified position.
     * 
     * <p>Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices). The new elements will appear
     * in this list in the order they appear in the array.
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be inserted into this list
     * @return {@code true} if this list changed as a result of the call
     *         (returns {@code false} if the specified array is {@code null} or empty)
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final long[] a) {
        rangeCheckForAdd(index);

        if (N.isEmpty(a)) {
            return false;
        }

        final int numNew = a.length;

        ensureCapacity(size + numNew);   // Increments modCount

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
     *
     * <p>If this list contains multiple occurrences of the specified element, only the first
     * occurrence is removed. The list is shifted to close the gap left by the removed element.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final long e) {
        for (int i = 0; i < size; i++) {
            if (elementData[i] == e) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list.
     * 
     * <p>This method removes every element in the list that equals the specified value,
     * not just the first occurrence. The remaining elements are shifted to close any gaps.
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list was modified (at least one element was removed)
     */
    public boolean removeAllOccurrences(final long e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
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
     * Removes from this list all of its elements that are contained in the specified LongList.
     * 
     * <p>This method compares elements by value. For each element in the specified list,
     * it removes one matching occurrence from this list (if present). If the specified list
     * contains duplicates, multiple occurrences may be removed from this list.
     *
     * @param c the LongList containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final LongList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * 
     * <p>This method compares elements by value. For each element in the specified array,
     * it removes one matching occurrence from this list (if present). If the specified array
     * contains duplicates, multiple occurrences may be removed from this list.
     *
     * @param a the array containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final long[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * 
     * <p>Each element in the list is tested with the predicate, and elements for which
     * the predicate returns {@code true} are removed. The order of remaining elements
     * is preserved.
     *
     * @param p the predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed from this list
     */
    public boolean removeIf(final LongPredicate p) {
        final LongList tmp = new LongList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes all duplicate elements from this list, keeping only the first occurrence of each value.
     * 
     * <p>This method preserves the order of elements. If the list is already sorted,
     * the operation is optimized to run in O(n) time. Otherwise, it uses a LinkedHashSet
     * internally to track seen elements, resulting in O(n) time complexity with O(n) space.
     *
     * @return {@code true} if any duplicates were removed from this list
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
                if (elementData[i] != elementData[idx]) {
                    elementData[++idx] = elementData[i];
                }
            }

        } else {
            final Set<Long> set = N.newLinkedHashSet(size);
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
     * Retains only the elements in this list that are contained in the specified LongList.
     * 
     * <p>In other words, removes from this list all of its elements that are not contained
     * in the specified list. This method preserves the order of retained elements.
     *
     * @param c the LongList containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final LongList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * 
     * <p>In other words, removes from this list all of its elements that are not contained
     * in the specified array. This method preserves the order of retained elements.
     *
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final long[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(LongList.of(a));
    }

    /**
     * Performs a batch removal operation based on the specified collection and complement flag.
     *
     * @param c the collection of elements to check against
     * @param complement if {@code true}, retain elements in c; if {@code false}, remove elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final LongList c, final boolean complement) {
        final long[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Long> set = c.toSet();

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
     * 
     * <p>Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public long delete(final int index) {
        rangeCheck(index);

        final long oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list.
     * 
     * <p>The indices array may contain duplicate values and does not need to be sorted.
     * Invalid indices are ignored. The remaining elements maintain their relative order.
     *
     * @param indices the indices of elements to be removed
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final long[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, 0L);
        }

        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes all elements in the specified range from this list.
     * 
     * <p>Removes elements starting at fromIndex (inclusive) up to toIndex (exclusive).
     * Shifts any subsequent elements to the left to close the gap.
     *
     * @param fromIndex the index of the first element to be removed (inclusive)
     * @param toIndex the index after the last element to be removed (exclusive)
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
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
     * LongList list = LongList.of(0, 1, 2, 3, 4, 5);
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
     * Replaces a range of elements in this list with the elements from the specified LongList.
     * 
     * <p>The elements from fromIndex (inclusive) to toIndex (exclusive) are removed and
     * replaced with all elements from the replacement list. The size of this list may change
     * if the replacement contains a different number of elements than the replaced range.
     *
     * @param fromIndex the index of the first element to replace (inclusive)
     * @param toIndex the index after the last element to replace (exclusive)
     * @param replacement the LongList whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final LongList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0L);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with the elements from the specified array.
     * 
     * <p>The elements from fromIndex (inclusive) to toIndex (exclusive) are removed and
     * replaced with all elements from the replacement array. The size of this list may change
     * if the replacement contains a different number of elements than the replaced range.
     *
     * @param fromIndex the index of the first element to replace (inclusive)
     * @param toIndex the index after the last element to replace (exclusive)
     * @param replacement the array whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final long[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0L);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value with a new value in this list.
     * 
     * <p>This method scans through the entire list and replaces every element that
     * equals oldVal with newVal.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal with
     * @return the number of elements that were replaced
     */
    public int replaceAll(final long oldVal, final long newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (elementData[i] == oldVal) {
                elementData[i] = newVal;

                result++;
            }
        }

        return result;
    }

    /**
     * Replaces each element of this list with the result of applying the given operator to that element.
     * 
     * <p>This method applies the provided unary operator to each element in the list,
     * replacing the element with the result of the operation.
     *
     * @param operator the operator to apply to each element
     */
    public void replaceAll(final LongUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsLong(elementData[i]);
        }
    }

    /**
     * Replaces all elements in this list that satisfy the given predicate with the specified value.
     * 
     * <p>Each element is tested with the predicate, and elements for which the predicate
     * returns {@code true} are replaced with newValue.
     *
     * @param predicate the predicate to test each element
     * @param newValue the value to replace matching elements with
     * @return {@code true} if any elements were replaced
     */
    public boolean replaceIf(final LongPredicate predicate, final long newValue) {
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
     * Fills the entire list with the specified value.
     * 
     * <p>After this operation, every element in the list will have the same value.
     * The size of the list remains unchanged.
     *
     * @param val the value to fill the list with
     */
    public void fill(final long val) {
        fill(0, size(), val);
    }

    /**
     * Fills the specified range of this list with the specified value.
     * 
     * <p>Elements from fromIndex (inclusive) to toIndex (exclusive) are set to the given value.
     * Other elements in the list remain unchanged.
     *
     * @param fromIndex the index of the first element to fill (inclusive)
     * @param toIndex the index after the last element to fill (exclusive)
     * @param val the value to fill the range with
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    public void fill(final int fromIndex, final int toIndex, final long val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code e == valueToFind}.
     *
     * <p>This method performs a linear search through the list.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element, {@code false} otherwise
     */
    public boolean contains(final long valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Tests if this list contains any of the elements in the specified LongList.
     * 
     * <p>Returns {@code true} if this list contains at least one element that is also
     * present in the specified list.
     *
     * @param c the LongList to check for common elements
     * @return {@code true} if this list contains any element from the specified list
     */
    @Override
    public boolean containsAny(final LongList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Tests if this list contains any of the elements in the specified array.
     * 
     * <p>Returns {@code true} if this list contains at least one element that is also
     * present in the specified array.
     *
     * @param a the array to check for common elements
     * @return {@code true} if this list contains any element from the specified array
     */
    @Override
    public boolean containsAny(final long[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Tests if this list contains all elements in the specified LongList.
     *
     * <p>Returns {@code true} only if every distinct element in the specified list is also
     * present in this list. The frequency of elements is not considered; only presence is checked.</p>
     *
     * @param c the LongList to check for containment
     * @return {@code true} if this list contains all distinct elements from the specified list
     */
    @Override
    public boolean containsAll(final LongList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Long> set = this.toSet();

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
     * Tests if this list contains all elements in the specified array.
     *
     * <p>Returns {@code true} only if every distinct element in the specified array is also
     * present in this list. The frequency of elements is not considered; only presence is checked.</p>
     *
     * @param a the array to check for containment
     * @return {@code true} if this list contains all distinct elements from the specified array
     */
    @Override
    public boolean containsAll(final long[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Tests if this list and the specified LongList have no elements in common.
     * 
     * <p>Two lists are disjoint if they share no common elements. Empty lists are
     * disjoint with all lists (including other empty lists).
     *
     * @param c the LongList to check for common elements
     * @return {@code true} if this list and the specified list have no elements in common
     */
    @Override
    public boolean disjoint(final LongList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Long> set = this.toSet();

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
     * Tests if this list and the specified array have no elements in common.
     * 
     * <p>This list and the array are disjoint if they share no common elements.
     * Empty lists are disjoint with all arrays (including empty arrays).
     *
     * @param b the array to check for common elements
     * @return {@code true} if this list and the specified array have no elements in common
     */
    @Override
    public boolean disjoint(final long[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(0L, 1L, 1L, 2L, 3L);
     * LongList list2 = LongList.of(1L, 2L, 2L, 4L);
     * LongList result = list1.intersection(list2);   // result will be [1L, 2L]
     * // One occurrence of '1L' (minimum count in both lists) and one occurrence of '2L'
     *
     * LongList list3 = LongList.of(5L, 5L, 6L);
     * LongList list4 = LongList.of(5L, 7L);
     * LongList result2 = list3.intersection(list4);   // result will be [5L]
     * // One occurrence of '5L' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list
     * @return a new LongList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(long[])
     * @see #difference(LongList)
     * @see #symmetricDifference(LongList)
     * @see N#intersection(long[], long[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public LongList intersection(final LongList b) {
        if (N.isEmpty(b)) {
            return new LongList();
        }

        final Multiset<Long> bOccurrences = b.toMultiset();

        final LongList c = new LongList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(0L, 1L, 1L, 2L, 3L);
     * long[] array = new long[] {1L, 2L, 2L, 4L};
     * LongList result = list1.intersection(array);   // result will be [1L, 2L]
     * // One occurrence of '1L' (minimum count in both sources) and one occurrence of '2L'
     *
     * LongList list2 = LongList.of(5L, 5L, 6L);
     * long[] array2 = new long[] {5L, 7L};
     * LongList result2 = list2.intersection(array2);   // result will be [5L]
     * // One occurrence of '5L' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list
     * @return a new LongList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(LongList)
     * @see #difference(long[])
     * @see #symmetricDifference(long[])
     * @see N#intersection(long[], long[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public LongList intersection(final long[] b) {
        if (N.isEmpty(b)) {
            return new LongList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(1L, 1L, 2L, 3L);
     * LongList list2 = LongList.of(1L, 4L);
     * LongList result = list1.difference(list2);   // result will be [1L, 2L, 3L]
     * // One '1L' remains because list1 has two occurrences and list2 has one
     *
     * LongList list3 = LongList.of(5L, 6L);
     * LongList list4 = LongList.of(5L, 5L, 6L);
     * LongList result2 = list3.difference(list4);   // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list
     * @return a new LongList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(long[])
     * @see #symmetricDifference(LongList)
     * @see #intersection(LongList)
     * @see N#difference(long[], long[])
     * @see N#difference(int[], int[])
     */
    @Override
    public LongList difference(final LongList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Long> bOccurrences = b.toMultiset();

        final LongList c = new LongList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list with the elements in this list but not in the specified array {@code b},
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(1L, 1L, 2L, 3L);
     * long[] array = new long[] {1L, 4L};
     * LongList result = list1.difference(array);   // result will be [1L, 2L, 3L]
     * // One '1L' remains because list1 has two occurrences and array has one
     *
     * LongList list2 = LongList.of(5L, 6L);
     * long[] array2 = new long[] {5L, 5L, 6L};
     * LongList result2 = list2.difference(array2);   // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param b the array to compare against this list
     * @return a new LongList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(LongList)
     * @see #symmetricDifference(long[])
     * @see #intersection(long[])
     * @see N#difference(long[], long[])
     * @see N#difference(int[], int[])
     */
    @Override
    public LongList difference(final long[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new LongList containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both lists.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(1L, 1L, 2L, 3L);
     * LongList list2 = LongList.of(1L, 2L, 2L, 4L);
     * LongList result = list1.symmetricDifference(list2);
     * // result will contain: [1L, 3L, 2L, 4L]
     * // Elements explanation:
     * // - 1L appears twice in list1 and once in list2, so one occurrence remains
     * // - 3L appears only in list1, so it remains
     * // - 2L appears once in list1 and twice in list2, so one occurrence remains
     * // - 4L appears only in list2, so it remains
     * }</pre>
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new LongList containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(long[])
     * @see #difference(LongList)
     * @see #intersection(LongList)
     * @see N#symmetricDifference(long[], long[])
     * @see N#symmetricDifference(Collection, Collection)
     */
    @Override
    public LongList symmetricDifference(final LongList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Long> bOccurrences = b.toMultiset();
        final LongList c = new LongList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new LongList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list1 = LongList.of(1L, 1L, 2L, 3L);
     * long[] array = new long[] {1L, 2L, 2L, 4L};
     * LongList result = list1.symmetricDifference(array);
     * // result will contain: [1L, 3L, 2L, 4L]
     * // Elements explanation:
     * // - 1L appears twice in list1 and once in array, so one occurrence remains
     * // - 3L appears only in list1, so it remains
     * // - 2L appears once in list1 and twice in array, so one occurrence remains
     * // - 4L appears only in array, so it remains
     * }</pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new LongList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(LongList)
     * @see #difference(long[])
     * @see #intersection(long[])
     * @see N#symmetricDifference(long[], long[])
     * @see N#symmetricDifference(Collection, Collection)
     */
    @Override
    public LongList symmetricDifference(final long[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Counts the number of occurrences of the specified value in this list.
     * 
     * <p>This method performs a linear scan through the list, counting elements
     * that are equal to the specified value.
     *
     * @param valueToFind the value to count occurrences of
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final long valueToFind) {
        if (size == 0) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Returns the index of the first occurrence of the specified value in this list,
     * or {@code N.INDEX_NOT_FOUND} (-1) if this list does not contain the value.
     * <p>
     * The search starts from the beginning of the list (index 0).
     * </p>
     *
     * @param valueToFind the long value to search for
     * @return the index of the first occurrence of the specified value in this list,
     *         or {@code N.INDEX_NOT_FOUND} (-1) if the value is not found
     */
    public int indexOf(final long valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified value in this list,
     * starting the search at the specified index, or {@code N.INDEX_NOT_FOUND} (-1) 
     * if the value is not found.
     * <p>
     * The search starts at the specified {@code fromIndex} and proceeds to the end of the list.
     * If {@code fromIndex} is negative, the search starts from index 0.
     * If {@code fromIndex} is greater than or equal to the list size, {@code N.INDEX_NOT_FOUND} is returned.
     * </p>
     *
     * @param valueToFind the long value to search for
     * @param fromIndex the index to start the search from (inclusive)
     * @return the index of the first occurrence of the specified value in this list starting
     *         from {@code fromIndex}, or {@code N.INDEX_NOT_FOUND} (-1) if the value is not found
     */
    public int indexOf(final long valueToFind, final int fromIndex) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index of the last occurrence of the specified value in this list,
     * or {@code N.INDEX_NOT_FOUND} (-1) if this list does not contain the value.
     * <p>
     * The search starts from the end of the list and proceeds backwards to the beginning.
     * </p>
     *
     * @param valueToFind the long value to search for
     * @return the index of the last occurrence of the specified value in this list,
     *         or {@code N.INDEX_NOT_FOUND} (-1) if the value is not found
     */
    public int lastIndexOf(final long valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified value in this list,
     * searching backwards from the specified index, or {@code N.INDEX_NOT_FOUND} (-1)
     * if the value is not found.
     * <p>
     * The search starts at the specified {@code startIndexFromBack} (inclusive) and proceeds
     * backwards to the beginning of the list (index 0). If {@code startIndexFromBack} is
     * greater than or equal to the list size, the search starts from the last element.
     * If {@code startIndexFromBack} is negative or the list is empty, {@code N.INDEX_NOT_FOUND} is returned.
     * </p>
     *
     * @param valueToFind the long value to search for
     * @param startIndexFromBack the index to start the backwards search from (inclusive)
     * @return the index of the last occurrence of the specified value, searching backwards
     *         from {@code startIndexFromBack}, or {@code N.INDEX_NOT_FOUND} (-1) if the value is not found
     */
    public int lastIndexOf(final long valueToFind, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns an {@code OptionalLong} containing the minimum element of this list,
     * or an empty {@code OptionalLong} if this list is empty.
     * <p>
     * This method iterates through all elements to find the minimum value.
     * </p>
     *
     * @return an {@code OptionalLong} containing the minimum element of this list,
     *         or an empty {@code OptionalLong} if this list is empty
     */
    public OptionalLong min() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.min(elementData, 0, size));
    }

    /**
     * Returns an {@code OptionalLong} containing the minimum element in the specified range
     * of this list, or an empty {@code OptionalLong} if the range is empty.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * This method iterates through the specified range to find the minimum value.
     * </p>
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return an {@code OptionalLong} containing the minimum element in the specified range,
     *         or an empty {@code OptionalLong} if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    public OptionalLong min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns an {@code OptionalLong} containing the maximum element of this list,
     * or an empty {@code OptionalLong} if this list is empty.
     * <p>
     * This method iterates through all elements to find the maximum value.
     * </p>
     *
     * @return an {@code OptionalLong} containing the maximum element of this list,
     *         or an empty {@code OptionalLong} if this list is empty
     */
    public OptionalLong max() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.max(elementData, 0, size));
    }

    /**
     * Returns an {@code OptionalLong} containing the maximum element in the specified range
     * of this list, or an empty {@code OptionalLong} if the range is empty.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * This method iterates through the specified range to find the maximum value.
     * </p>
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return an {@code OptionalLong} containing the maximum element in the specified range,
     *         or an empty {@code OptionalLong} if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    public OptionalLong max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * @return an OptionalLong containing the median value if the list is non-empty, or an empty OptionalLong if the list is empty
     */
    public OptionalLong median() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.median(elementData, 0, size));
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
     * @return an OptionalLong containing the median value if the range is non-empty, or an empty OptionalLong if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalLong median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element of this list.
     * <p>
     * The action is performed on each element in the order of iteration, from the first
     * element (index 0) to the last element.
     * </p>
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final LongConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * If {@code fromIndex} is less than {@code toIndex}, the action is performed on elements
     * in ascending order. If {@code fromIndex} is greater than {@code toIndex}, the action
     * is performed on elements in descending order. If {@code fromIndex} equals {@code toIndex},
     * no action is performed.
     * </p>
     * <p>
     * Special case: if {@code toIndex} is -1 and {@code fromIndex} is greater than -1,
     * the iteration starts from {@code fromIndex} and goes backwards to index 0.
     * </p>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or -1 for reverse iteration to index 0
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public void forEach(final int fromIndex, final int toIndex, final LongConsumer action) throws IndexOutOfBoundsException {
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
     * Returns an {@code OptionalLong} containing the first element of this list,
     * or an empty {@code OptionalLong} if this list is empty.
     *
     * @return an {@code OptionalLong} containing the first element of this list,
     *         or an empty {@code OptionalLong} if this list is empty
     */
    public OptionalLong first() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(elementData[0]);
    }

    /**
     * Returns an {@code OptionalLong} containing the last element of this list,
     * or an empty {@code OptionalLong} if this list is empty.
     *
     * @return an {@code OptionalLong} containing the last element of this list,
     *         or an empty {@code OptionalLong} if this list is empty
     */
    public OptionalLong last() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(elementData[size() - 1]);
    }

    /**
     * Returns a new {@code LongList} containing only the distinct elements from the
     * specified range of this list, in the order they first appear.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * Duplicate elements are identified using long equality (==).
     * The returned list contains each distinct element only once, preserving the order
     * of first occurrence.
     * </p>
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return a new {@code LongList} containing only the distinct elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public LongList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Returns {@code true} if this list contains duplicate elements.
     * <p>
     * This method checks if any element appears more than once in the list.
     * Elements are compared using long equality (==).
     * </p>
     *
     * @return {@code true} if this list contains at least one duplicate element,
     *         {@code false} otherwise
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Returns {@code true} if the elements in this list are sorted in ascending order.
     * <p>
     * An empty list or a list with a single element is considered sorted.
     * </p>
     *
     * @return {@code true} if this list is sorted in ascending order, {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts the elements of this list in ascending order.
     * <p>
     * This method modifies the list in place. After this method returns,
     * the elements will be arranged in ascending numerical order.
     * </p>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list in ascending order using a parallel sort algorithm.
     * <p>
     * This method modifies the list in place. The parallel sort algorithm may provide
     * better performance than sequential sort for large lists on multi-core systems.
     * After this method returns, the elements will be arranged in ascending numerical order.
     * </p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list in descending order.
     * <p>
     * This method first sorts the list in ascending order, then reverses it.
     * After this method returns, the elements will be arranged in descending numerical order.
     * </p>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value using the binary search algorithm.
     * <p>
     * The list must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined. If the list contains multiple
     * elements equal to the specified value, there is no guarantee which one will be found.
     * </p>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the list;
     *         otherwise, {@code (-insertion point - 1)}. The insertion point is defined
     *         as the point at which the key would be inserted into the list: the index
     *         of the first element greater than the key, or {@code size()} if all elements
     *         in the list are less than the specified key
     */
    public int binarySearch(final long valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified value in the specified range using the binary search algorithm.
     * <p>
     * The range must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined. If the range contains multiple
     * elements equal to the specified value, there is no guarantee which one will be found.
     * </p>
     *
     * @param fromIndex the starting index of the range to search (inclusive)
     * @param toIndex the ending index of the range to search (exclusive)
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the specified range;
     *         otherwise, {@code (-insertion point - 1)}. The insertion point is defined
     *         as the point at which the key would be inserted into the range: the index
     *         of the first element in the range greater than the key, or {@code toIndex}
     *         if all elements in the range are less than the specified key
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final long valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * <p>
     * This method modifies the list in place. After this method returns,
     * the first element becomes the last, the second element becomes the second to last,
     * and so on.
     * </p>
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * This method modifies the list in place. After this method returns, the elements
     * in the specified range will be in reverse order.
     * </p>
     *
     * @param fromIndex the starting index of the range to reverse (inclusive)
     * @param toIndex the ending index of the range to reverse (exclusive)
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
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
     * Randomly shuffles the elements in this list.
     * <p>
     * This method uses a default source of randomness to shuffle the elements.
     * After this method returns, the elements will be in random order.
     * Each permutation of the list elements is equally likely.
     * </p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles the elements in this list using the specified source of randomness.
     * <p>
     * After this method returns, the elements will be in random order as determined
     * by the provided {@code Random} instance. Each permutation of the list elements
     * is equally likely, assuming the provided source of randomness is fair.
     * </p>
     *
     * @param rnd the source of randomness to use for shuffling
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * <p>
     * After this method returns, the element previously at position {@code i}
     * will be at position {@code j}, and vice versa.
     * </p>
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
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
     * Returns a copy of this list.
     * <p>
     * The returned list is a new instance containing the same elements as this list
     * in the same order. Changes to the returned list will not affect this list,
     * and vice versa.
     * </p>
     *
     * @return a new {@code LongList} containing a copy of all elements from this list
     */
    @Override
    public LongList copy() {
        return new LongList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a copy of the specified range of this list.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * The returned list is a new instance containing copies of the elements in the
     * specified range. Changes to the returned list will not affect this list, and vice versa.
     * </p>
     *
     * @param fromIndex the starting index of the range to copy (inclusive)
     * @param toIndex the ending index of the range to copy (exclusive)
     * @return a new {@code LongList} containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public LongList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new LongList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a copy of the specified range of this list with the specified step.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * Elements are copied at intervals specified by {@code step}. For example, with
     * {@code step = 2}, every second element in the range will be included in the result.
     * </p>
     * <p>
     * If {@code fromIndex} is greater than {@code toIndex}, elements are copied in
     * reverse order. The step value must be positive.
     * </p>
     *
     * @param fromIndex the starting index of the range to copy (inclusive)
     * @param toIndex the ending index of the range to copy (exclusive)
     * @param step the step size for selecting elements (must be positive)
     * @return a new {@code LongList} containing elements from the specified range at the specified intervals
     * @throws IndexOutOfBoundsException if the range is out of bounds
     * @throws IllegalArgumentException if {@code step} is not positive
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public LongList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new LongList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns a list of {@code LongList} instances, each containing a consecutive subsequence
     * of elements from the specified range of this list.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * Each subsequence (except possibly the last) will have {@code chunkSize} elements.
     * The last subsequence may have fewer elements if the range size is not evenly divisible
     * by {@code chunkSize}.
     * </p>
     * <p>
     * For example, splitting the range [1, 2, 3, 4, 5, 6, 7] with {@code chunkSize = 3}
     * results in [[1, 2, 3], [4, 5, 6], [7]].
     * </p>
     *
     * @param fromIndex the starting index of the range to split (inclusive)
     * @param toIndex the ending index of the range to split (exclusive)
     * @param chunkSize the desired size of each subsequence (must be positive)
     * @return a list of {@code LongList} instances, each containing a subsequence of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize} is not positive
     */
    @Override
    public List<LongList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<long[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<LongList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this list to its current size.
     * <p>
     * If the capacity of this list is larger than its current size, this method
     * reduces the capacity to match the size, minimizing memory usage.
     * This operation does not change the elements or the size of the list.
     * </p>
     *
     * @return this list instance (for method chaining)
     */
    @Override
    public LongList trimToSize() {
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
     * Returns a {@code List<Long>} containing all elements of this list.
     * <p>
     * Each primitive long value is boxed into a {@code Long} object.
     * The returned list is a new instance, and changes to it will not affect this list.
     * </p>
     *
     * @return a new {@code List<Long>} containing all elements from this list as boxed values
     */
    @Override
    public List<Long> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a {@code List<Long>} containing the elements in the specified range of this list.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * Each primitive long value in the range is boxed into a {@code Long} object.
     * The returned list is a new instance, and changes to it will not affect this list.
     * </p>
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return a new {@code List<Long>} containing the elements in the specified range as boxed values
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public List<Long> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Long> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new long array containing all elements of this list
     */
    @Override
    public long[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Returns a new {@code FloatList} containing all elements of this list converted to float values.
     * <p>
     * Each long value is converted to a float value. Note that this conversion may lose
     * precision for large long values that cannot be exactly represented as float.
     * </p>
     *
     * @return a new {@code FloatList} containing all elements from this list as float values
     */
    public FloatList toFloatList() {
        final float[] a = new float[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return FloatList.of(a);
    }

    /**
     * Returns a new {@code DoubleList} containing all elements of this list converted to double values.
     * <p>
     * Each long value is converted to a double value. Note that this conversion may lose
     * precision for very large long values that cannot be exactly represented as double.
     * </p>
     *
     * @return a new {@code DoubleList} containing all elements from this list as double values
     */
    public DoubleList toDoubleList() {
        final double[] a = new double[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return DoubleList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of the collection to return
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @param supplier a function which produces a new collection of the desired type,
     *                given the size of the range
     * @return a collection containing the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Long>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @param supplier a function which produces a new {@code Multiset} instance,
     *                given the size of the range
     * @return a {@code Multiset} containing the elements in the specified range with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Long> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Long>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Long> multiset = supplier.apply(toIndex - fromIndex);

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
     * @return a {@code LongIterator} over the elements in this list
     */
    @Override
    public LongIterator iterator() {
        if (isEmpty()) {
            return LongIterator.EMPTY;
        }

        return LongIterator.of(elementData, 0, size);
    }

    /**
     * Returns a {@code LongStream} with this list as its source.
     * <p>
     * The stream processes all elements of this list in order.
     * </p>
     *
     * @return a sequential {@code LongStream} over the elements in this list
     */
    public LongStream stream() {
        return LongStream.of(elementData, 0, size());
    }

    /**
     * Returns a {@code LongStream} with the specified range of this list as its source.
     * <p>
     * The range is defined by {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     * The stream processes elements in the specified range in order.
     * </p>
     *
     * @param fromIndex the starting index of the range (inclusive)
     * @param toIndex the ending index of the range (exclusive)
     * @return a sequential {@code LongStream} over the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     *         or {@code fromIndex > toIndex}
     */
    public LongStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return LongStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * <p>
     * This method provides direct access to the first element without using Optional.
     * </p>
     *
     * @return the first long value in this list
     * @throws NoSuchElementException if this list is empty
     */
    public long getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * <p>
     * This method provides direct access to the last element without using Optional.
     * </p>
     *
     * @return the last long value in this list
     * @throws NoSuchElementException if this list is empty
     */
    public long getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * <p>
     * Shifts all existing elements to the right (adds one to their indices).
     * This operation has O(n) time complexity where n is the size of the list.
     * </p>
     *
     * @param e the element to add at the beginning of this list
     */
    public void addFirst(final long e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * <p>
     * This operation has O(1) amortized time complexity.
     * </p>
     *
     * @param e the element to add at the end of this list
     */
    public void addLast(final long e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * <p>
     * Shifts all remaining elements to the left (subtracts one from their indices).
     * This operation has O(n) time complexity where n is the size of the list.
     * </p>
     *
     * @return the first element that was removed from this list
     * @throws NoSuchElementException if this list is empty
     */
    public long removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * <p>
     * This operation has O(1) time complexity.
     * </p>
     *
     * @return the last element that was removed from this list
     * @throws NoSuchElementException if this list is empty
     */
    public long removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns the hash code value for this list.
     * <p>
     * The hash code is computed based on the elements in the list and their order.
     * Two lists with the same elements in the same order will have the same hash code.
     * </p>
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares this list with the specified object for equality.
     * <p>
     * Returns {@code true} if and only if the specified object is also a {@code LongList},
     * both lists have the same size, and all corresponding pairs of elements in the two
     * lists are equal. Two long values are considered equal if they have the same value
     * (using ==).
     * </p>
     *
     * @param obj the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LongList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * <p>
     * The string representation consists of the list's elements in order,
     * enclosed in square brackets ("[]"). Adjacent elements are separated by
     * the characters ", " (comma and space). If the list is empty, returns "[]".
     * </p>
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
            elementData = new long[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}
