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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.ShortStream;

/**
 * A high-performance, resizable array implementation for primitive short values that provides
 * specialized operations optimized for 16-bit integer data types. This class extends {@link PrimitiveList}
 * to offer memory-efficient storage and operations that avoid the boxing overhead associated with
 * {@code List<Short>}, making it ideal for applications requiring intensive short integer array
 * manipulation with optimal performance characteristics.
 *
 * <p>ShortList is specifically designed for scenarios involving large collections of small integer
 * values such as audio sample data, compact integer storage, memory-constrained applications,
 * embedded systems programming, and performance-critical applications requiring 16-bit integer
 * precision. The implementation uses a compact short array as the underlying storage mechanism,
 * providing direct primitive access without wrapper object allocation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct short primitive storage without Short wrapper allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact short array storage with minimal memory overhead</li>
 *   <li><b>16-bit Integer Range:</b> Full support for short integer range (-32,768 to 32,767)</li>
 *   <li><b>High Performance:</b> Optimized algorithms for short-specific operations</li>
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
 *   <li><b>Audio Processing:</b> 16-bit audio samples, digital signal processing, sound data</li>
 *   <li><b>Memory-Constrained Systems:</b> Embedded systems, IoT devices, mobile applications</li>
 *   <li><b>Image Processing:</b> Pixel values, grayscale data, compressed image formats</li>
 *   <li><b>Network Protocols:</b> Port numbers, packet headers, protocol fields</li>
 *   <li><b>Data Compression:</b> Compact integer storage, delta encoding, difference arrays</li>
 *   <li><b>Gaming Applications:</b> Tile IDs, sprite indices, game state data</li>
 *   <li><b>Sensor Data:</b> Accelerometer readings, temperature values, measurement data</li>
 *   <li><b>Database Systems:</b> Compact integer columns, index storage, enumeration values</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and initializing short lists
 * ShortList audioSamples = ShortList.of((short) 1024, (short) -512, (short) 2048);
 * ShortList range = ShortList.range((short) 1, (short) 100);   // [1, 2, 3, ..., 99]
 * ShortList sequence = ShortList.range((short) 0, (short) 50, (short) 5);   // [0, 5, 10, ..., 45]
 * ShortList sensorData = new ShortList(1000);   // Pre-sized for performance
 *
 * // Basic operations
 * audioSamples.add((short) 3072);   // Add audio sample
 * short firstSample = audioSamples.get(0);   // Access by index: 1024
 * audioSamples.set(1, (short) -1024);   // Modify existing sample
 *
 * // Mathematical operations for 16-bit data
 * OptionalShort min = audioSamples.min();   // Find minimum sample
 * OptionalShort max = audioSamples.max();   // Find maximum sample
 * OptionalShort median = audioSamples.median();   // Calculate median sample
 *
 * // Set operations for data analysis
 * ShortList set1 = ShortList.of((short) 100, (short) 200, (short) 300);
 * ShortList set2 = ShortList.of((short) 200, (short) 300, (short) 400);
 * ShortList intersection = set1.intersection(set2);   // [200, 300]
 * ShortList difference = set1.difference(set2);   // [100]
 *
 * // High-performance sorting and searching
 * audioSamples.sort();   // Sort samples
 * audioSamples.parallelSort();   // Parallel sort for large datasets
 * int index = audioSamples.binarySearch((short) 1024);   // Fast lookup
 *
 * // Type conversions for different precision needs
 * IntList intValues = audioSamples.toIntList();   // Convert to int (no precision loss)
 * LongList longValues = audioSamples.toLongList();   // Convert to long (no precision loss)
 * FloatList floatValues = audioSamples.toFloatList();   // Convert to float
 * short[] primitiveArray = audioSamples.toArray();   // To primitive array
 * List<Short> boxedList = audioSamples.boxed();   // To boxed collection
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Element Access:</b> O(1) for get/set operations by index</li>
 *   <li><b>Insertion:</b> O(1) amortized for append, O(n) for middle insertion</li>
 *   <li><b>Deletion:</b> O(1) for last element, O(n) for arbitrary position</li>
 *   <li><b>Search:</b> O(n) for contains/indexOf, O(log n) for binary search on sorted data</li>
 *   <li><b>Sorting:</b> O(n log n) using optimized primitive sorting algorithms</li>
 *   <li><b>Set Operations:</b> O(n) to O(n²) depending on algorithm selection and data size</li>
 *   <li><b>Mathematical Operations:</b> O(n) for statistical calculations</li>
 * </ul>
 *
 * <p><b>Memory Efficiency:</b>
 * <ul>
 *   <li><b>Storage:</b> 2 bytes per element (16 bits) with no object overhead</li>
 *   <li><b>vs List&lt;Short&gt;:</b> ~8x less memory usage (no Short wrapper objects)</li>
 *   <li><b>vs IntList:</b> 50% less memory usage for values within short range</li>
 *   <li><b>Capacity Management:</b> 1.75x growth factor balances memory and performance</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} (typically Integer.MAX_VALUE - 8)</li>
 * </ul>
 *
 * <p><b>Short-Specific Operations:</b>
 * <ul>
 *   <li><b>Range Generation:</b> {@code range()}, {@code rangeClosed()} for arithmetic sequences</li>
 *   <li><b>Mathematical Functions:</b> {@code min()}, {@code max()}, {@code median()}</li>
 *   <li><b>Type Conversions:</b> {@code toIntList()}, {@code toLongList()}, {@code toFloatList()}, {@code toDoubleList()}</li>
 *   <li><b>Random Generation:</b> {@code random()} methods for test data and simulations</li>
 *   <li><b>Bulk Updates:</b> {@code replaceAll()}, {@code replaceIf()} for value transformations</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li><b>{@code of(short...)}:</b> Create from varargs array</li>
 *   <li><b>{@code copyOf(short[])}:</b> Create defensive copy of array</li>
 *   <li><b>{@code range(short, short)}:</b> Create arithmetic sequence [start, end)</li>
 *   <li><b>{@code rangeClosed(short, short)}:</b> Create arithmetic sequence [start, end]</li>
 *   <li><b>{@code repeat(short, int)}:</b> Create with repeated values</li>
 *   <li><b>{@code random(int)}:</b> Create with random short values</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toArray()}:</b> Convert to primitive short array</li>
 *   <li><b>{@code toIntList()}:</b> Convert to IntList with promoted values</li>
 *   <li><b>{@code toLongList()}:</b> Convert to LongList with promoted values</li>
 *   <li><b>{@code toFloatList()}:</b> Convert to FloatList with promoted values</li>
 *   <li><b>{@code toDoubleList()}:</b> Convert to DoubleList with promoted values</li>
 *   <li><b>{@code boxed()}:</b> Convert to {@code List<Short>}</li>
 *   <li><b>{@code stream()}:</b> Convert to ShortStream for functional processing</li>
 * </ul>
 *
 * <p><b>Deque-like Operations:</b>
 * <ul>
 *   <li><b>{@code addFirst(short)}:</b> Insert at beginning (O(n) operation)</li>
 *   <li><b>{@code addLast(short)}:</b> Insert at end (O(1) amortized)</li>
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
 *   <li><b>Efficient Format:</b> Optimized serialization of short arrays</li>
 *   <li><b>Cross-Platform:</b> Platform-independent serialized format</li>
 * </ul>
 *
 * <p><b>Integration with Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Collection Compatibility:</b> Seamless conversion to standard collections</li>
 *   <li><b>Utility Integration:</b> Works with Collections utility methods via boxed()</li>
 *   <li><b>Stream API:</b> Full integration with ShortStream for functional processing</li>
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
 *   <li><b>vs List&lt;Short&gt;:</b> 8x less memory, significantly faster operations</li>
 *   <li><b>vs short[]:</b> Dynamic sizing, rich API, set operations, statistical functions</li>
 *   <li><b>vs IntList:</b> 50% less memory for values within short range</li>
 *   <li><b>vs ArrayList&lt;Short&gt;:</b> No boxing overhead, primitive-specific methods</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code ShortList} when working with values in range [-32,768, 32,767]</li>
 *   <li>Specify initial capacity for known data sizes to avoid resizing</li>
 *   <li>Use bulk operations ({@code addAll}, {@code removeAll}) instead of loops</li>
 *   <li>Convert to larger primitive types when values exceed short range</li>
 *   <li>Leverage sorting for improved binary search performance</li>
 *   <li>Consider memory savings when storing large collections of small integers</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Pre-size lists with known capacity using constructor or {@code ensureCapacity()}</li>
 *   <li>Use {@code addLast()} instead of {@code addFirst()} for better performance</li>
 *   <li>Sort data before using {@code binarySearch()} for O(log n) lookups</li>
 *   <li>Use {@code stream()} API for complex transformations and filtering</li>
 *   <li>Consider {@code parallelSort()} for large datasets</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Audio Samples:</b> {@code ShortList samples = new ShortList(sampleRate);}</li>
 *   <li><b>Port Numbers:</b> {@code ShortList ports = ShortList.range((short) 8000, (short) 9000);}</li>
 *   <li><b>Sensor Data:</b> {@code ShortList readings = ShortList.random((short) -1000, (short) 1000, count);}</li>
 *   <li><b>Compact Storage:</b> {@code ShortList ids = data.stream().mapToShort(...).collect(...);}</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link PrimitiveList}:</b> Abstract base class for all primitive list types</li>
 *   <li><b>{@link IntList}:</b> 32-bit integer primitive list for larger values</li>
 *   <li><b>{@link LongList}:</b> 64-bit integer primitive list for large values</li>
 *   <li><b>{@link ByteList}:</b> 8-bit integer primitive list for smaller values</li>
 *   <li><b>{@link ShortIterator}:</b> Specialized iterator for short primitives</li>
 *   <li><b>{@link ShortStream}:</b> Functional processing of short sequences</li>
 * </ul>
 *
 * <p><b>Example: Audio Sample Processing</b>
 * <pre>{@code
 * // Process 16-bit audio samples
 * ShortList leftChannel = new ShortList(44100);   // 1 second at 44.1kHz
 * ShortList rightChannel = new ShortList(44100);
 *
 * // Read audio samples (simulated)
 * for (int i = 0; i < 44100; i++) {
 *     leftChannel.add((short) (Math.sin(2 * Math.PI * 440 * i / 44100.0) * 32767));
 *     rightChannel.add((short) (Math.sin(2 * Math.PI * 880 * i / 44100.0) * 32767));
 * }
 *
 * // Audio processing
 * OptionalShort maxAmplitude = leftChannel.max();   // Find peak amplitude
 * OptionalShort minAmplitude = leftChannel.min();   // Find minimum amplitude
 *
 * // Apply gain (volume adjustment)
 * double gain = 0.5;
 * leftChannel.replaceAll(sample -> (short) (sample * gain));
 *
 * // Mix channels (mono conversion)
 * ShortList monoChannel = new ShortList(leftChannel.size());
 * for (int i = 0; i < leftChannel.size(); i++) {
 *     short mixed = (short) ((leftChannel.get(i) + rightChannel.get(i)) / 2);
 *     monoChannel.add(mixed);
 * }
 *
 * // Convert to float for DSP processing
 * FloatList floatSamples = monoChannel.stream()
 *     .mapToFloat(sample -> sample / 32768.0f)
 *     .collect(FloatList::new, FloatList::add, FloatList::addAll);
 * }</pre>
 *
 * @see PrimitiveList
 * @see ShortIterator
 * @see ShortStream
 * @see IntList
 * @see ByteList
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see java.util.List
 * @see java.util.RandomAccess
 * @see java.io.Serializable
 */
public final class ShortList extends PrimitiveList<Short, short[], ShortList> {

    @Serial
    private static final long serialVersionUID = 25682021483156507L;

    static final Random RAND = new SecureRandom();

    /**
     * The array buffer into which the elements of the ShortList are stored.
     */
    private short[] elementData = N.EMPTY_SHORT_ARRAY;

    /**
     * The size of the ShortList (the number of elements it contains).
     */
    private int size = 0;

    /**
     * Constructs an empty ShortList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     */
    public ShortList() {
    }

    /**
     * Constructs an empty ShortList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public ShortList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_SHORT_ARRAY : new short[initialCapacity];
    }

    /**
     * Constructs a ShortList using the specified array as the backing array for this list without copying.
     * The list will have the same length as the array. Modifications to the list will affect the original array.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     */
    public ShortList(final short[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a ShortList using the specified array as the backing array for this list without copying.
     * Only the first {@code size} elements of the array will be considered part of the list.
     * Modifications to the list will affect the original array.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     * @param size the number of elements in the list. Must be between 0 and a.length (inclusive).
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public ShortList(final short[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new ShortList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new ShortList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static ShortList of(final short... a) {
        return new ShortList(N.nullToEmpty(a));
    }

    /**
     * Creates a new ShortList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * @param a the array of short values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new ShortList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static ShortList of(final short[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new ShortList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new ShortList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(short...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new ShortList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static ShortList copyOf(final short[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new ShortList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new ShortList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static ShortList copyOf(final short[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a ShortList containing a sequence of short values from startInclusive (inclusive) to
     * endExclusive (exclusive), incrementing by 1. If startInclusive &gt;= endExclusive, an empty list is returned.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new ShortList containing the sequence of values
     */
    public static ShortList range(final short startInclusive, final short endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a ShortList containing a sequence of short values from startInclusive (inclusive) to
     * endExclusive (exclusive), incrementing by the specified step value. The step can be positive
     * or negative. If the step would not reach endExclusive from startInclusive, an empty list is returned.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new ShortList containing the sequence of values
     * @throws IllegalArgumentException if by is zero
     */
    public static ShortList range(final short startInclusive, final short endExclusive, final short by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a ShortList containing a sequence of short values from startInclusive to endInclusive
     * (both inclusive), incrementing by 1. If startInclusive &gt; endInclusive, an empty list is returned.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new ShortList containing the sequence of values including both endpoints
     */
    public static ShortList rangeClosed(final short startInclusive, final short endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a ShortList containing a sequence of short values from startInclusive to endInclusive
     * (both inclusive), incrementing by the specified step value. The step can be positive or negative.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new ShortList containing the sequence of values including both endpoints
     * @throws IllegalArgumentException if by is zero
     */
    public static ShortList rangeClosed(final short startInclusive, final short endInclusive, final short by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a ShortList containing the specified element repeated the given number of times.
     *
     * @param element the short value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new ShortList containing the element repeated len times
     * @throws IllegalArgumentException if len is negative
     */
    public static ShortList repeat(final short element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a ShortList containing the specified number of random short values. The values are
     * uniformly distributed across the entire range of short values (Short.MIN_VALUE to Short.MAX_VALUE).
     *
     * @param len the number of random elements to generate. Must be non-negative.
     * @return a new ShortList containing len random short values
     * @throws IllegalArgumentException if len is negative
     */
    public static ShortList random(final int len) {
        final int bound = Short.MAX_VALUE - Short.MIN_VALUE + 1;
        final short[] a = new short[len];

        for (int i = 0; i < len; i++) {
            a[i] = (short) (RAND.nextInt(bound) + Short.MIN_VALUE);
        }

        return of(a);
    }

    /**
     * Returns the underlying short array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.</p>
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal short array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public short[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public short get(final int index) {
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
    public short set(final int index, final short e) {
        rangeCheck(index);

        final short oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list. The list will automatically grow if necessary.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the element to be appended to this list
     */
    public void add(final short e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element currently
     * at that position (if any) and any subsequent elements to the right (adds one to their indices).
     * The list will automatically grow if necessary.
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final short e) {
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
     * Appends all elements from the specified ShortList to the end of this list, in the order they are
     * stored in the specified list. The behavior of this operation is undefined if the specified list
     * is modified during the operation.
     *
     * @param c the ShortList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean addAll(final ShortList c) {
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
     * Inserts all elements from the specified ShortList into this list, starting at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices). The new elements will appear in this list in the order they are stored
     * in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the ShortList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final ShortList c) {
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
     * Appends all elements from the specified array to the end of this list, in the order they appear
     * in the array.
     *
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (returns {@code false} only if the array is empty)
     */
    @Override
    public boolean addAll(final short[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list, starting at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices). The new elements will appear in this list in the order they appear
     * in the array.
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (returns {@code false} only if the array is empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final short[] a) {
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
     * If the list does not contain the element, it is unchanged. More formally, removes the element
     * with the lowest index i such that elementData[i] == e.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final short e) {
        for (int i = 0; i < size; i++) {
            if (elementData[i] == e) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list. The list is compacted after removal
     * so that there are no gaps between elements.
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list was modified (i.e., at least one occurrence was removed)
     */
    public boolean removeAllOccurrences(final short e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (short) 0);

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
     * Removes from this list all of its elements that are contained in the specified ShortList.
     * Each occurrence of an element in the specified list will remove at most one occurrence from this list.
     *
     * @param c the ShortList containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final ShortList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * Each occurrence of an element in the array will remove at most one occurrence from this list.
     *
     * @param a the array containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final short[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * Elements are tested in order from first to last, and those for which the predicate
     * returns {@code true} are removed. The order of the remaining elements is preserved.
     *
     * <p>This is a functional programming approach to element removal, allowing complex
     * removal logic to be expressed concisely using lambda expressions or method references.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list = ShortList.of((short)1, (short)-2, (short)3, (short)-4, (short)5);
     * list.removeIf(s -> s < 0);   // Removes negative values
     * // list now contains: [1, 3, 5]
     * }</pre>
     *
     * @param p the predicate which returns {@code true} for elements to be removed. Must not be {@code null}.
     * @return {@code true} if any elements were removed; {@code false} if the list was unchanged
     */
    public boolean removeIf(final ShortPredicate p) {
        final ShortList tmp = new ShortList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (short) 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes all duplicate elements from this list, keeping only the first occurrence of each value.
     * The relative order of unique elements is preserved. If the list is already sorted, this operation
     * is optimized to run in linear time.
     *
     * @return {@code true} if any duplicates were removed, {@code false} if all elements were already unique
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
            final Set<Short> set = N.newLinkedHashSet(size);
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
            N.fill(elementData, idx + 1, size, (short) 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified ShortList.
     * In other words, removes from this list all of its elements that are not contained in the
     * specified list. Each occurrence is considered independently.
     *
     * @param c the ShortList containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final ShortList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all of its elements that are not contained in the
     * specified array. Each occurrence is considered independently.
     *
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final short[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(ShortList.of(a));
    }

    /**
     * Performs a batch removal operation based on the specified collection and complement flag.
     *
     * @param c the collection of elements to check against
     * @param complement if {@code true}, retain elements in c; if {@code false}, remove elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final ShortList c, final boolean complement) {
        final short[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Short> set = c.toSet();

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
            N.fill(elementData, w, size, (short) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list. Shifts any subsequent elements
     * to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public short delete(final int index) {
        rangeCheck(index);

        final short oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list. The indices array may contain
     * duplicates and does not need to be sorted. Elements are removed efficiently in a single pass.
     *
     * @param indices the indices of elements to be removed. Can be empty, {@code null}, or contain duplicates.
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final short[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (short) 0);
        }

        // size = tmp.length; // incorrect. the array returned N.deleteAllByIndices(elementData, indices) contains empty elements after size.
        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes from this list all elements whose index is between fromIndex (inclusive) and toIndex (exclusive).
     * Shifts any subsequent elements to the left (reduces their index). If fromIndex equals toIndex,
     * no elements are removed.
     *
     * @param fromIndex the index of the first element to be removed (inclusive)
     * @param toIndex the index after the last element to be removed (exclusive)
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
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

        N.fill(elementData, newSize, size, (short) 0);

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
     * ShortList list = ShortList.of((short) 0, (short) 1, (Short) 2, (Short) 3, (Short) 4, (Short) 5);
     * list.moveRange(1, 3, 3);   // Moves elements [1, 2] to position starting at index 3
     * // Result: [0, 3, 4, 1, 2, 5]
     * }</pre>
     * 
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionAfterMove the index where the first element of the range
     *        should be positioned after the move; must be &gt;= 0 and &lt;= {@code size() - lengthOfRange}
     * @throws IndexOutOfBoundsException if any index is out of bounds or if newPositionAfterMove
     *         would cause elements to be moved outside the list bounds
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionAfterMove);
    }

    /**
     * Replaces a range of elements in this list with the elements from the specified ShortList.
     * The range to be replaced is [fromIndex, toIndex). If the replacement list has a different
     * size than the range being replaced, the list will be resized accordingly.
     *
     * @param fromIndex the starting index (inclusive) of the range to be replaced
     * @param toIndex the ending index (exclusive) of the range to be replaced
     * @param replacement the ShortList whose elements will replace the specified range. Can be empty.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final ShortList replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        // Use long arithmetic to prevent integer overflow
        final long newSizeLong = (long) size - (long) (toIndex - fromIndex) + replacement.size();

        if (newSizeLong < 0 || newSizeLong > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        final int newSize = (int) newSizeLong;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.size() && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.size(), size - toIndex);
        }

        N.copy(replacement.elementData, 0, elementData, fromIndex, replacement.size());

        if (newSize < size) {
            N.fill(elementData, newSize, size, (short) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with the elements from the specified array.
     * The range to be replaced is [fromIndex, toIndex). If the replacement array has a different
     * size than the range being replaced, the list will be resized accordingly.
     *
     * @param fromIndex the starting index (inclusive) of the range to be replaced
     * @param toIndex the ending index (exclusive) of the range to be replaced
     * @param replacement the array whose elements will replace the specified range. Can be empty or {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final short[] replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        // Use long arithmetic to prevent integer overflow
        final long newSizeLong = (long) size - (long) (toIndex - fromIndex) + replacement.length;

        if (newSizeLong < 0 || newSizeLong > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        final int newSize = (int) newSizeLong;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.length && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.length, size - toIndex);
        }

        N.copy(replacement, 0, elementData, fromIndex, replacement.length);

        if (newSize < size) {
            N.fill(elementData, newSize, size, (short) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value in this list with the new value.
     * Uses == for comparison.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal
     * @return the number of elements replaced
     */
    public int replaceAll(final short oldVal, final short newVal) {
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
     * Replaces each element of this list with the result of applying the specified operator to that element.
     * The operator is applied to each element in order from first to last.
     *
     * @param operator the operator to apply to each element
     */
    public void replaceAll(final ShortUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsShort(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * Elements are tested in order from first to last.
     *
     * @param predicate the predicate to test each element
     * @param newValue the value to replace matching elements with
     * @return {@code true} if any elements were replaced
     */
    public boolean replaceIf(final ShortPredicate predicate, final short newValue) {
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
     * Replaces all elements in this list with the specified value. The size of the list is unchanged.
     *
     * @param val the value to be stored in all elements of the list
     */
    public void fill(final short val) {
        fill(0, size(), val);
    }

    /**
     * Replaces all elements in the specified range with the specified value. The range to be filled
     * extends from index fromIndex (inclusive) to index toIndex (exclusive).
     *
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value
     * @param val the value to be stored in all elements of the specified range
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
     */
    public void fill(final int fromIndex, final int toIndex, final short val) throws IndexOutOfBoundsException {
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
    public boolean contains(final short valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified ShortList.
     * The operation returns as soon as any match is found.
     *
     * @param c the ShortList to be checked for containment in this list
     * @return {@code true} if this list contains any element from the specified list
     */
    @Override
    public boolean containsAny(final ShortList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified array.
     * The operation returns as soon as any match is found.
     *
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains any element from the specified array
     */
    @Override
    public boolean containsAny(final short[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified ShortList.
     * Each element's occurrences are counted independently - if an element appears twice
     * in the specified list, this list must contain at least two occurrences of that element.
     *
     * @param c the ShortList to be checked for containment in this list
     * @return {@code true} if this list contains all elements of the specified list
     */
    @Override
    public boolean containsAll(final ShortList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Short> set = this.toSet();

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
     * Returns {@code true} if this list contains all of the elements in the specified array.
     * Each element's occurrences are counted independently - if an element appears twice
     * in the specified array, this list must contain at least two occurrences of that element.
     *
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains all elements of the specified array
     */
    @Override
    public boolean containsAll(final short[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list has no elements in common with the specified ShortList.
     * Two lists are disjoint if they share no common elements.
     *
     * @param c the ShortList to be checked for disjointness with this list
     * @return {@code true} if this list has no elements in common with the specified list
     */
    @Override
    public boolean disjoint(final ShortList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Short> set = this.toSet();

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
     * Returns {@code true} if this list has no elements in common with the specified array.
     * This list and the array are disjoint if they share no common elements.
     *
     * @param b the array to be checked for disjointness with this list
     * @return {@code true} if this list has no elements in common with the specified array
     */
    @Override
    public boolean disjoint(final short[] b) {
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
     * ShortList list1 = ShortList.of((short)0, (short)1, (short)2, (short)2, (short)3);
     * ShortList list2 = ShortList.of((short)1, (short)2, (short)2, (short)4);
     * ShortList result = list1.intersection(list2);   // result will be [(short)1, (short)2, (short)2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     *
     * ShortList list3 = ShortList.of((short)5, (short)5, (short)6);
     * ShortList list4 = ShortList.of((short)5, (short)7);
     * ShortList result2 = list3.intersection(list4);   // result will be [(short)5]
     * // One occurrence of '5' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list
     * @return a new ShortList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(short[])
     * @see #difference(ShortList)
     * @see #symmetricDifference(ShortList)
     * @see N#intersection(short[], short[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public ShortList intersection(final ShortList b) {
        if (N.isEmpty(b)) {
            return new ShortList();
        }

        final Multiset<Short> bOccurrences = b.toMultiset();

        final ShortList result = new ShortList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                result.add(elementData[i]);
            }
        }

        return result;
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both sources. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short)0, (short)1, (short)2, (short)2, (short)3);
     * short[] array = new short[] {(short)1, (short)2, (short)2, (short)4};
     * ShortList result = list1.intersection(array);   // result will be [(short)1, (short)2, (short)2]
     * // One occurrence of '1' (minimum count in both sources) and two occurrences of '2'
     *
     * ShortList list2 = ShortList.of((short)5, (short)5, (short)6);
     * short[] array2 = new short[] {(short)5, (short)7};
     * ShortList result2 = list2.intersection(array2);   // result will be [(short)5]
     * // One occurrence of '5' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list
     * @return a new ShortList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(ShortList)
     * @see #difference(short[])
     * @see #symmetricDifference(short[])
     * @see N#intersection(short[], short[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public ShortList intersection(final short[] b) {
        if (N.isEmpty(b)) {
            return new ShortList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list containing the elements that are in this list but not in the specified list.
     * If an element appears multiple times, the difference considers the count of occurrences -
     * only the excess occurrences remain in the result. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * ShortList list2 = ShortList.of((short)1, (short)4);
     * ShortList result = list1.difference(list2);   // result will be [(short)1, (short)2, (short)3]
     * // One '1' remains because list1 has two occurrences and list2 has one
     *
     * ShortList list3 = ShortList.of((short)5, (short)6);
     * ShortList list4 = ShortList.of((short)5, (short)5, (short)6);
     * ShortList result2 = list3.difference(list4);   // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list
     * @return a new ShortList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(short[])
     * @see #symmetricDifference(ShortList)
     * @see #intersection(ShortList)
     * @see N#difference(short[], short[])
     * @see N#difference(int[], int[])
     */
    @Override
    public ShortList difference(final ShortList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Short> bOccurrences = b.toMultiset();

        final ShortList result = new ShortList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                result.add(elementData[i]);
            }
        }

        return result;
    }

    /**
     * Returns a new list containing the elements that are in this list but not in the specified array.
     * If an element appears multiple times, the difference considers the count of occurrences -
     * only the excess occurrences remain in the result. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * short[] array = new short[] {(short)1, (short)4};
     * ShortList result = list1.difference(array);   // result will be [(short)1, (short)2, (short)3]
     * // One '1' remains because list1 has two occurrences and array has one
     *
     * ShortList list2 = ShortList.of((short)5, (short)6);
     * short[] array2 = new short[] {(short)5, (short)5, (short)6};
     * ShortList result2 = list2.difference(array2);   // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param b the array to compare against this list
     * @return a new ShortList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(ShortList)
     * @see #symmetricDifference(short[])
     * @see #intersection(short[])
     * @see N#difference(short[], short[])
     * @see N#difference(int[], int[])
     */
    @Override
    public ShortList difference(final short[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new ShortList containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * ShortList list2 = ShortList.of((short)1, (short)2, (short)2, (short)4);
     * ShortList result = list1.symmetricDifference(list2);
     * // result will contain: [(short)1, (short)3, (short)2, (short)4]
     * // Elements explanation:
     * // - (short)1 appears twice in list1 and once in list2, so one occurrence remains
     * // - (short)3 appears only in list1, so it remains
     * // - (short)2 appears once in list1 and twice in list2, so one occurrence remains
     * // - (short)4 appears only in list2, so it remains
     * }</pre>
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new ShortList containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(short[])
     * @see #difference(ShortList)
     * @see #intersection(ShortList)
     * @see N#symmetricDifference(short[], short[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public ShortList symmetricDifference(final ShortList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Short> bOccurrences = b.toMultiset();
        final ShortList result = new ShortList(N.max(9, Math.abs(size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                result.add(elementData[i]);
            }
        }

        for (int i = 0, len = b.size(); i < len; i++) {
            if (bOccurrences.remove(b.elementData[i])) {
                result.add(b.elementData[i]);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return result;
    }

    /**
     * Returns a new ShortList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * short[] array = new short[] {(short)1, (short)2, (short)2, (short)4};
     * ShortList result = list1.symmetricDifference(array);
     * // result will contain: [(short)1, (short)3, (short)2, (short)4]
     * // Elements explanation:
     * // - (short)1 appears twice in list1 and once in array, so one occurrence remains
     * // - (short)3 appears only in list1, so it remains
     * // - (short)2 appears once in list1 and twice in array, so one occurrence remains
     * // - (short)4 appears only in array, so it remains
     * }</pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new ShortList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(ShortList)
     * @see #difference(short[])
     * @see #intersection(short[])
     * @see N#symmetricDifference(short[], short[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public ShortList symmetricDifference(final short[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Returns the number of times the specified value appears in this list.
     *
     * @param valueToFind the value to count occurrences of
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final short valueToFind) {
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
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. More formally, returns the lowest
     * index i such that elementData[i] == valueToFind, or -1 if there is no such index.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int indexOf(final short valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * searching forwards from the specified index, or -1 if the element is not found.
     * More formally, returns the lowest index i &gt;= fromIndex such that elementData[i] == valueToFind,
     * or -1 if there is no such index.
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start searching from (inclusive)
     * @return the index of the first occurrence of the element at position &gt;= fromIndex,
     *         or -1 if the element is not found
     */
    public int indexOf(final short valueToFind, final int fromIndex) {
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
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. More formally, returns the highest
     * index i such that elementData[i] == valueToFind, or -1 if there is no such index.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int lastIndexOf(final short valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     * More formally, returns the highest index i &lt;= startIndexFromBack such that
     * elementData[i] == valueToFind, or -1 if there is no such index.
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *        Can be size() to search the entire list.
     * @return the index of the last occurrence of the element at position &lt;= startIndexFromBack,
     *         or -1 if the element is not found
     */
    public int lastIndexOf(final short valueToFind, final int startIndexFromBack) {
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
     * Returns the minimum element in this list. If the list is empty, an empty OptionalShort is returned.
     *
     * @return an OptionalShort containing the minimum element, or an empty OptionalShort if this list is empty
     */
    public OptionalShort min() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list. If the range is empty
     * (fromIndex == toIndex), an empty OptionalShort is returned.
     *
     * @param fromIndex the index of the first element (inclusive) to examine
     * @param toIndex the index after the last element (exclusive) to examine
     * @return an OptionalShort containing the minimum element in the specified range,
     *         or an empty OptionalShort if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
     */
    public OptionalShort min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list. If the list is empty, an empty OptionalShort is returned.
     *
     * @return an OptionalShort containing the maximum element, or an empty OptionalShort if this list is empty
     */
    public OptionalShort max() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list. If the range is empty
     * (fromIndex == toIndex), an empty OptionalShort is returned.
     *
     * @param fromIndex the index of the first element (inclusive) to examine
     * @param toIndex the index after the last element (exclusive) to examine
     * @return an OptionalShort containing the maximum element in the specified range,
     *         or an empty OptionalShort if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex &lt; 0 || toIndex &gt; size() || fromIndex &gt; toIndex)
     */
    public OptionalShort max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     * 
     * @return an OptionalShort containing the median value if the list is non-empty, or an empty OptionalShort if the list is empty
     */
    public OptionalShort median() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.median(elementData, 0, size));
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
     * @return an OptionalShort containing the median value if the range is non-empty, or an empty OptionalShort if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalShort median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element in this list.
     * 
     * <p>The action is performed on each element in order, from the first element
     * to the last element.</p>
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final ShortConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element within the specified range of this list.
     *
     * <p>This method supports both forward and backward iteration based on the relative values of
     * fromIndex and toIndex:</p>
     * <ul>
     *   <li>If {@code fromIndex <= toIndex}: iterates forward from fromIndex (inclusive) to toIndex (exclusive)</li>
     *   <li>If {@code fromIndex > toIndex}: iterates backward from fromIndex (inclusive) to toIndex (exclusive)</li>
     *   <li>If {@code toIndex == -1}: treated as backward iteration from fromIndex to the beginning of the list</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list = ShortList.of((short)10, (short)20, (short)30, (short)40, (short)50);
     * list.forEach(0, 3, action);    // Forward: processes indices 0,1,2
     * list.forEach(3, 0, action);    // Backward: processes indices 3,2,1
     * list.forEach(4, -1, action);   // Backward: processes indices 4,3,2,1,0
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or -1 for backward iteration to the start
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public void forEach(final int fromIndex, final int toIndex, final ShortConsumer action) throws IndexOutOfBoundsException {
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
     * Returns the first element in this list wrapped in an OptionalShort.
     * 
     * @return an OptionalShort containing the first element, or empty if the list is empty
     */
    public OptionalShort first() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[0]);
    }

    /**
     * Returns the last element in this list wrapped in an OptionalShort.
     * 
     * @return an OptionalShort containing the last element, or empty if the list is empty
     */
    public OptionalShort last() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[size() - 1]);
    }

    /**
     * Returns a new ShortList containing only the distinct elements from the specified range.
     * 
     * <p>Duplicate elements are removed, keeping only the first occurrence of each value.
     * The order of the remaining elements is preserved.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range
     * @param toIndex the ending index (exclusive) of the range
     * @return a new ShortList containing the distinct elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public ShortList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains duplicate elements.
     *
     * @return {@code true} if the list contains at least one duplicate element, {@code false} otherwise
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * 
     * @return {@code true} if the list is sorted in ascending order or is empty, {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts all elements in this list in ascending order.
     * 
     * <p>This method modifies the list in place. After sorting, the elements will be
     * arranged from smallest to largest value.</p>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in ascending order using a parallel sort algorithm.
     * 
     * <p>This method uses a parallel sorting algorithm which may be more efficient
     * for large lists on multi-core systems. The list is modified in place.</p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in descending order.
     * 
     * <p>This method first sorts the list in ascending order, then reverses it to achieve
     * descending order. The list is modified in place.</p>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value using binary search algorithm.
     * 
     * <p>The list must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined.</p>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key if found; otherwise, (-(insertion point) - 1).
     *         The insertion point is the point at which the key would be inserted into the list.
     */
    public int binarySearch(final short valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified value in the given range using binary search algorithm.
     * 
     * <p>The specified range of the list must be sorted in ascending order prior to making
     * this call. If it is not sorted, the results are undefined.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @param valueToFind the value to search for
     * @return the index of the search key if found within the range; otherwise, (-(insertion point) - 1).
     *         The insertion point is the point at which the key would be inserted into the list.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public int binarySearch(final int fromIndex, final int toIndex, final short valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * 
     * <p>After calling this method, the first element becomes the last,
     * the second becomes the second-to-last, and so on.</p>
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
     * <p>Elements from fromIndex (inclusive) to toIndex (exclusive) are reversed.
     * Elements outside this range are not affected.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse
     * @param toIndex the ending index (exclusive) of the range to reverse
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
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
     * 
     * <p>After shuffling, each permutation of the list elements is equally likely.
     * This method uses the default source of randomness.</p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles the elements in this list using the specified random number generator.
     * 
     * <p>After shuffling, each permutation of the list elements is equally likely,
     * assuming the provided Random instance produces uniformly distributed values.</p>
     *
     * @param rnd the random number generator to use for shuffling
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
     * <p>If the specified positions are the same, the list is not modified.</p>
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either index is out of range (i &lt; 0 || i &gt;= size() || j &lt; 0 || j &gt;= size())
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Creates and returns a new ShortList containing all elements of this list.
     * 
     * <p>The returned list is a shallow copy; it contains the same element values
     * but is a distinct list instance.</p>
     * 
     * @return a new ShortList containing all elements of this list
     */
    @Override
    public ShortList copy() {
        return new ShortList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Creates and returns a new ShortList containing elements from the specified range.
     * 
     * <p>The returned list contains elements from fromIndex (inclusive) to toIndex (exclusive)
     * of this list. The returned list is independent of this list.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @return a new ShortList containing the specified range of elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public ShortList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Creates and returns a new ShortList containing elements from the specified range with the given step.
     * 
     * <p>The returned list contains elements starting at fromIndex, then fromIndex + step,
     * fromIndex + 2*step, and so on, up to but not including toIndex. If step is negative,
     * the elements are selected in reverse order.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @param step the step size between selected elements. Must not be zero.
     * @return a new ShortList containing the selected elements
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if step is zero
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public ShortList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into consecutive subsequences of the specified size.
     * 
     * <p>Returns a List of ShortList instances, where each inner list (except possibly the last)
     * has the specified size. The last list may have fewer elements if the range size is not
     * evenly divisible by the specified size.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to split
     * @param toIndex the ending index (exclusive) of the range to split
     * @param size the desired size of each subsequence. Must be positive.
     * @return a List of ShortList instances containing the split subsequences
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     * @throws IllegalArgumentException if size &lt;= 0
     */
    @Override
    public List<ShortList> split(final int fromIndex, final int toIndex, final int size) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<short[]> list = N.split(elementData, fromIndex, toIndex, size);
        @SuppressWarnings("rawtypes")
        final List<ShortList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this list to its current size.
     * 
     * <p>If the capacity of this list is larger than its current size, the capacity
     * is reduced to match the size. This operation minimizes the memory footprint
     * of the list.</p>
     *
     * @return this list instance (for method chaining)
     */
    @Override
    public ShortList trimToSize() {
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
            N.fill(elementData, 0, size, (short) 0);
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
     * Returns a List containing all elements of this list as boxed Short objects.
     * 
     * <p>This method converts each primitive short value to its corresponding
     * Short wrapper object.</p>
     * 
     * @return a new List&lt;Short&gt; containing all elements as boxed values
     */
    @Override
    public List<Short> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing elements from the specified range as boxed Short objects.
     * 
     * <p>This method converts each primitive short value in the range to its corresponding
     * Short wrapper object.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to box
     * @param toIndex the ending index (exclusive) of the range to box
     * @return a new List&lt;Short&gt; containing the specified range of elements as boxed values
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public List<Short> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Short> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new short array containing all elements of this list
     */
    @Override
    public short[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this ShortList to an IntList.
     * 
     * <p>Each short value is widened to an int value. The resulting IntList
     * contains the same number of elements as this list.</p>
     *
     * @return a new IntList containing all elements of this list widened to int values
     */
    public IntList toIntList() {
        final int[] a = new int[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return IntList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of the collection
     * @param fromIndex the starting index (inclusive) of the range to add
     * @param toIndex the ending index (exclusive) of the range to add
     * @param supplier a function that creates the collection with the specified initial capacity
     * @return the collection with the added elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public <C extends Collection<Short>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final C collection = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            collection.add(elementData[i]);
        }

        return collection;
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     *
     * @param fromIndex the starting index (inclusive) of the range
     * @param toIndex the ending index (exclusive) of the range
     * @param supplier a function that creates the Multiset with the specified initial capacity
     * @return a Multiset containing the elements from the specified range
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public Multiset<Short> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Short>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Short> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over the elements in this list.
     * 
     * <p>The iterator returns elements in order from first to last. The iterator
     * does not support element removal.</p>
     * 
     * @return a ShortIterator over the elements in this list
     */
    @Override
    public ShortIterator iterator() {
        if (isEmpty()) {
            return ShortIterator.EMPTY;
        }

        return ShortIterator.of(elementData, 0, size);
    }

    /**
     * Returns a ShortStream with this list as its source.
     * 
     * <p>The stream provides access to all elements in the list in order.</p>
     * 
     * @return a sequential ShortStream over the elements in this list
     */
    public ShortStream stream() {
        return ShortStream.of(elementData, 0, size());
    }

    /**
     * Returns a ShortStream for the specified range of this list.
     * 
     * <p>The stream provides access to elements from fromIndex (inclusive) to
     * toIndex (exclusive) in order.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range
     * @param toIndex the ending index (exclusive) of the range
     * @return a sequential ShortStream over the specified range of elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public ShortStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return ShortStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * 
     * <p>This method provides direct access to the first element without wrapping
     * in an Optional.</p>
     *
     * @return the first short value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public short getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * 
     * <p>This method provides direct access to the last element without wrapping
     * in an Optional.</p>
     *
     * @return the last short value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public short getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * 
     * <p>Shifts the element currently at position 0 and any subsequent elements
     * to the right (adds one to their indices).</p>
     *
     * @param e the element to add at the beginning of the list
     */
    public void addFirst(final short e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This method is equivalent to add(e).</p>
     *
     * @param e the element to add at the end of the list
     */
    public void addLast(final short e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * 
     * <p>Shifts any subsequent elements to the left (subtracts one from their indices).</p>
     *
     * @return the first short value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public short removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last short value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public short removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns the hash code value for this list.
     * 
     * <p>The hash code is calculated based on the elements in the list and their order.
     * Two lists with the same elements in the same order will have the same hash code.</p>
     * 
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares this list with the specified object for equality.
     * 
     * <p>Returns {@code true} if and only if the specified object is also a ShortList,
     * both lists have the same size, and all corresponding pairs of elements
     * are equal.</p>
     *
     * @param obj the object to compare with this list for equality
     * @return {@code true} if the specified object is equal to this list, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ShortList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * 
     * <p>The string representation consists of the list's elements, enclosed in
     * square brackets ("[]"). Adjacent elements are separated by comma and space ", ".
     * Elements are converted to strings by String.valueOf(short).</p>
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
            elementData = new short[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}
