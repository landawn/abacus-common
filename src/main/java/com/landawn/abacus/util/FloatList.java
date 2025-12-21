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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.stream.FloatStream;

/**
 * A high-performance, resizable array implementation for primitive float values that provides
 * specialized operations optimized for single-precision floating-point data types. This class extends
 * {@link PrimitiveList} to offer memory-efficient storage and operations that avoid the boxing overhead
 * associated with {@code List<Float>}, making it ideal for applications requiring intensive float array
 * manipulation with optimal performance characteristics.
 *
 * <p>FloatList is specifically designed for scenarios involving large collections of floating-point
 * values such as scientific computing, graphics programming, audio processing, machine learning,
 * signal processing, and performance-critical applications requiring single-precision arithmetic.
 * The implementation uses a compact float array as the underlying storage mechanism, providing
 * direct primitive access without wrapper object allocation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct float primitive storage without Float wrapper allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact float array storage with minimal memory overhead</li>
 *   <li><b>Single-Precision Arithmetic:</b> Full support for IEEE 754 single-precision operations</li>
 *   <li><b>High Performance:</b> Optimized algorithms for floating-point specific operations</li>
 *   <li><b>Rich Mathematical API:</b> Statistical operations like min, max, median, sum</li>
 *   <li><b>Set Operations:</b> Efficient intersection, union, and difference operations</li>
 *   <li><b>Range Generation:</b> Built-in support for arithmetic progressions and sequences</li>
 *   <li><b>Random Access:</b> O(1) element access and modification by index</li>
 *   <li><b>Dynamic Sizing:</b> Automatic capacity management with intelligent growth</li>
 *   <li><b>Type Conversions:</b> Seamless conversion to other numeric primitive lists</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Graphics Programming:</b> 3D coordinates, vertices, color values, texture coordinates</li>
 *   <li><b>Audio Processing:</b> Digital signal processing, audio samples, frequency analysis</li>
 *   <li><b>Scientific Computing:</b> Numerical simulations, experimental data, measurements</li>
 *   <li><b>Machine Learning:</b> Feature vectors, weights, neural network parameters</li>
 *   <li><b>Game Development:</b> Physics calculations, animation data, particle systems</li>
 *   <li><b>Image Processing:</b> Pixel values, filters, transformations, computer vision</li>
 *   <li><b>Financial Modeling:</b> Price data, returns, volatility calculations</li>
 *   <li><b>Signal Processing:</b> Digital filters, Fourier transforms, sensor data</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and initializing float lists
 * FloatList coordinates = FloatList.of(1.5f, 2.7f, 3.14f, 4.2f);
 * FloatList range = FloatList.range(0.0f, 10.0f, 0.5f);   // [0.0, 0.5, 1.0, ..., 9.5]
 * FloatList audioSamples = new FloatList(44100);   // Pre-sized for 1 second at 44.1kHz
 * FloatList random = FloatList.random(0.0f, 1.0f, 1000);   // 1000 random floats [0.0, 1.0)
 *
 * // Basic operations
 * coordinates.add(5.8f);   // Append float value
 * float x = coordinates.get(0);   // Access by index: 1.5
 * coordinates.set(1, 3.0f);   // Modify existing value
 *
 * // Mathematical operations for floating-point data
 * OptionalFloat min = coordinates.min();   // Find minimum value
 * OptionalFloat max = coordinates.max();   // Find maximum value
 * OptionalFloat median = coordinates.median();   // Calculate median value
 * double sum = coordinates.stream().sum();   // Calculate sum
 *
 * // Set operations for data analysis
 * FloatList set1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
 * FloatList set2 = FloatList.of(3.0f, 4.0f, 5.0f, 6.0f);
 * FloatList intersection = set1.intersection(set2);   // [3.0, 4.0]
 * FloatList difference = set1.difference(set2);   // [1.0, 2.0]
 *
 * // High-performance sorting and searching
 * coordinates.sort();   // Sort in ascending order
 * coordinates.parallelSort();   // Parallel sort for large datasets
 * int index = coordinates.binarySearch(3.14f);   // Fast lookup
 *
 * // Type conversions for different precision needs
 * DoubleList doubleValues = coordinates.toDoubleList();   // Convert to double precision
 * IntList roundedValues = coordinates.stream()             // Convert to rounded integers
 *     .mapToInt(f -> Math.round(f))
 *     .collect(IntList::new, IntList::add, IntList::addAll);
 * float[] primitiveArray = coordinates.toArray();   // To primitive array
 * List<Float> boxedList = coordinates.boxed();   // To boxed collection
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
 *   <li><b>Storage:</b> 4 bytes per element (32 bits) with no object overhead</li>
 *   <li><b>vs List&lt;Float&gt;:</b> ~4x less memory usage (no Float wrapper objects)</li>
 *   <li><b>Capacity Management:</b> 1.75x growth factor balances memory and performance</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} (typically Integer.MAX_VALUE - 8)</li>
 * </ul>
 *
 * <p><b>Floating-Point Considerations:</b>
 * <ul>
 *   <li><b>IEEE 754 Compliance:</b> Full support for single-precision floating-point standard</li>
 *   <li><b>Special Values:</b> Proper handling of NaN, positive/negative infinity</li>
 *   <li><b>Precision:</b> ~7 decimal digits of precision (24-bit mantissa)</li>
 *   <li><b>Range:</b> Approximately ±3.4 × 10^38 with subnormal support</li>
 *   <li><b>Comparison:</b> NaN-aware comparison operations</li>
 * </ul>
 *
 * <p><b>Float-Specific Operations:</b>
 * <ul>
 *   <li><b>Range Generation:</b> {@code range()}, {@code rangeClosed()} for arithmetic sequences</li>
 *   <li><b>Mathematical Functions:</b> {@code min()}, {@code max()}, {@code median()}</li>
 *   <li><b>Type Conversions:</b> {@code toDoubleList()} for increased precision</li>
 *   <li><b>Random Generation:</b> {@code random()} methods for simulations and testing</li>
 *   <li><b>Parallel Operations:</b> {@code parallelSort()} for large dataset optimization</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li><b>{@code of(float...)}:</b> Create from varargs array</li>
 *   <li><b>{@code copyOf(float[])}:</b> Create defensive copy of array</li>
 *   <li><b>{@code range(float, float, float)}:</b> Create arithmetic sequence with step</li>
 *   <li><b>{@code repeat(float, int)}:</b> Create with repeated values</li>
 *   <li><b>{@code random(int)}:</b> Create with random float values</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toArray()}:</b> Convert to primitive float array</li>
 *   <li><b>{@code toDoubleList()}:</b> Convert to DoubleList with increased precision</li>
 *   <li><b>{@code boxed()}:</b> Convert to {@code List<Float>}</li>
 *   <li><b>{@code stream()}:</b> Convert to FloatStream for functional processing</li>
 * </ul>
 *
 * <p><b>Deque-like Operations:</b>
 * <ul>
 *   <li><b>{@code addFirst(float)}:</b> Insert at beginning (O(n) operation)</li>
 *   <li><b>{@code addLast(float)}:</b> Insert at end (O(1) amortized)</li>
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
 *   <li><b>Efficient Format:</b> Optimized serialization of float arrays</li>
 *   <li><b>Cross-Platform:</b> Platform-independent serialized format</li>
 * </ul>
 *
 * <p><b>Integration with Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Collection Compatibility:</b> Seamless conversion to standard collections</li>
 *   <li><b>Utility Integration:</b> Works with Collections utility methods via boxed()</li>
 *   <li><b>Stream API:</b> Full integration with FloatStream for functional processing</li>
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
 *   <li><b>vs List&lt;Float&gt;:</b> 4x less memory, significantly faster operations</li>
 *   <li><b>vs float[]:</b> Dynamic sizing, rich API, set operations, statistical functions</li>
 *   <li><b>vs DoubleList:</b> Half the memory usage, lower precision, faster for simple operations</li>
 *   <li><b>vs ArrayList&lt;Float&gt;:</b> No boxing overhead, primitive-specific methods</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code FloatList} when single-precision is sufficient for your application</li>
 *   <li>Specify initial capacity for known data sizes to avoid resizing</li>
 *   <li>Use bulk operations ({@code addAll}, {@code removeAll}) instead of loops</li>
 *   <li>Convert to {@code DoubleList} when higher precision is required</li>
 *   <li>Leverage parallel sorting for large datasets (>10,000 elements)</li>
 *   <li>Be aware of floating-point precision limitations in comparisons</li>
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
 *   <li><b>3D Graphics:</b> {@code FloatList vertices = FloatList.of(x1, y1, z1, x2, y2, z2);}</li>
 *   <li><b>Audio Processing:</b> {@code FloatList samples = new FloatList(sampleRate);}</li>
 *   <li><b>Machine Learning:</b> {@code FloatList features = dataset.stream().mapToFloat(...).collect(...);}</li>
 *   <li><b>Scientific Data:</b> {@code FloatList measurements = FloatList.random(minVal, maxVal, count);}</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link PrimitiveList}:</b> Abstract base class for all primitive list types</li>
 *   <li><b>{@link DoubleList}:</b> Higher precision floating-point list implementation</li>
 *   <li><b>{@link IntList}:</b> Integer primitive list for discrete values</li>
 *   <li><b>{@link FloatIterator}:</b> Specialized iterator for float primitives</li>
 *   <li><b>{@link FloatStream}:</b> Functional processing of float sequences</li>
 * </ul>
 *
 * <p><b>Example: Graphics Programming</b>
 * <pre>{@code
 * // Define 3D vertex data for a triangle
 * FloatList vertices = new FloatList(9);   // 3 vertices × 3 coordinates
 *
 * // Add vertex coordinates (x, y, z)
 * vertices.addAll(new float[]{
 *     0.0f,  0.5f, 0.0f,   // Top vertex
 *    -0.5f, -0.5f, 0.0f,   // Bottom left
 *     0.5f, -0.5f, 0.0f    // Bottom right
 * });
 *
 * // Transform vertices (scale by 2.0)
 * vertices.replaceAll(coord -> coord * 2.0f);
 *
 * // Extract coordinates
 * float[] vertexArray = vertices.toArray();
 *
 * // Calculate bounding box
 * OptionalFloat minX = vertices.stream().skip(0).filter((i, v) -> i % 3 == 0).min();
 * OptionalFloat maxX = vertices.stream().skip(0).filter((i, v) -> i % 3 == 0).max();
 * OptionalFloat minY = vertices.stream().skip(1).filter((i, v) -> i % 3 == 1).min();
 * OptionalFloat maxY = vertices.stream().skip(1).filter((i, v) -> i % 3 == 1).max();
 * }</pre>
 *
 * @see PrimitiveList
 * @see FloatIterator
 * @see FloatStream
 * @see DoubleList
 * @see IntList
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see java.util.List
 * @see java.util.RandomAccess
 * @see java.io.Serializable
 */
public final class FloatList extends PrimitiveList<Float, float[], FloatList> {

    @Serial
    private static final long serialVersionUID = 6459013170687883950L;

    static final Random RAND = new SecureRandom();

    /**
     * The array buffer into which the elements of the FloatList are stored.
     */
    private float[] elementData = N.EMPTY_FLOAT_ARRAY;

    /**
     * The size of the FloatList (the number of elements it contains).
     */
    private int size = 0;

    /**
     * Constructs an empty FloatList with an initial capacity of zero.
     * The internal array will be initialized with an empty array until elements are added.
     */
    public FloatList() {
    }

    /**
     * Constructs an empty FloatList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public FloatList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_FLOAT_ARRAY : new float[initialCapacity];
    }

    /**
     * Constructs a FloatList using the specified array as the backing array for this list.
     * The array is used directly without copying, so modifications to the list will affect
     * the original array and vice versa. The size of the list will be equal to the length
     * of the array.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     */
    public FloatList(final float[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a FloatList using the specified array as the backing array for this list,
     * with a specified size. The array is used directly without copying. Only the first
     * <i>size</i> elements of the array are considered part of the list.
     *
     * @param a the array to be used as the backing array for this list. Must not be {@code null}.
     * @param size the number of elements in the list. Must be between 0 and a.length (inclusive).
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public FloatList(final float[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new FloatList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new FloatList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static FloatList of(final float... a) {
        return new FloatList(N.nullToEmpty(a));
    }

    /**
     * Creates a new FloatList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * @param a the array of float values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new FloatList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static FloatList of(final float[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new FloatList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new FloatList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(float...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new FloatList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static FloatList copyOf(final float[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new FloatList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new FloatList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static FloatList copyOf(final float[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a new FloatList with the specified element repeated the given number of times.
     * For example, repeat(3.14f, 5) returns a list containing [3.14, 3.14, 3.14, 3.14, 3.14].
     *
     * @param element the float value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new FloatList containing the repeated elements
     * @throws IllegalArgumentException if len is negative
     */
    public static FloatList repeat(final float element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a new FloatList filled with random float values between 0.0 (inclusive) and 1.0 (exclusive).
     * The random values are generated using a secure random number generator.
     *
     * @param len the number of random float values to generate. Must be non-negative.
     * @return a new FloatList containing the specified number of random float values
     * @throws IllegalArgumentException if len is negative
     */
    public static FloatList random(final int len) {
        final float[] a = new float[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextFloat();
        }

        return of(a);
    }

    /**
     * Returns the underlying float array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.</p>
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal float array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public float[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public float get(final int index) {
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
    public float set(final int index, final float e) {
        rangeCheck(index);

        final float oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list. The list will be automatically
     * resized if necessary to accommodate the new element.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the element to be appended to this list
     */
    public void add(final float e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (adds one
     * to their indices). The list will be automatically resized if necessary.
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted. Must be between 0 and size (inclusive).
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final float e) {
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
     * Appends all elements from the specified FloatList to the end of this list, in the order
     * that they appear in the specified list. The behavior of this operation is undefined if
     * the specified list is modified during the operation.
     *
     * @param c the FloatList containing elements to be added to this list. Must not be {@code null}.
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty)
     */
    @Override
    public boolean addAll(final FloatList c) {
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
     * Inserts all elements from the specified FloatList into this list, starting at the specified
     * position. Shifts the element currently at that position (if any) and any subsequent elements
     * to the right (increases their indices). The new elements will appear in this list in the
     * order that they appear in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list. Must be between 0 and size (inclusive).
     * @param c the FloatList containing elements to be inserted into this list. Must not be {@code null}.
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final FloatList c) {
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
     * Appends all elements from the specified array to the end of this list, in array order.
     * The list will be automatically resized if necessary to accommodate the new elements.
     *
     * @param a the array containing elements to be added to this list. May be {@code null} or empty.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not {@code null} or empty)
     */
    @Override
    public boolean addAll(final float[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list, starting at the specified
     * position. Shifts the element currently at that position (if any) and any subsequent elements
     * to the right (increases their indices). The new elements will appear in this list in array order.
     *
     * @param index the index at which to insert the first element from the specified array. Must be between 0 and size (inclusive).
     * @param a the array containing elements to be inserted into this list. May be {@code null} or empty.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not {@code null} or empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size())
     */
    @Override
    public boolean addAll(final int index, final float[] a) {
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
     * If this list does not contain the element, it is unchanged. The comparison is done using
     * Float.compare() to handle NaN values correctly.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final float e) {
        for (int i = 0; i < size; i++) {
            if (N.equals(elementData[i], e)) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list. The list is compacted
     * after removal, and the size is adjusted accordingly. The comparison is done using
     * Float.compare() to handle NaN values correctly.
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list contained one or more occurrences of the specified element
     */
    public boolean removeAllOccurrences(final float e) {
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
     * Removes from this list all of its elements that are contained in the specified FloatList.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param c the FloatList containing elements to be removed from this list. Must not be {@code null}.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final FloatList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param a the array containing elements to be removed from this list. May be {@code null} or empty.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final float[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate. The elements are
     * tested in order, and those for which the predicate returns {@code true} are removed. The list
     * is compacted after removal.
     *
     * @param p the predicate which returns {@code true} for elements to be removed. Must not be {@code null}.
     * @return {@code true} if any elements were removed
     */
    public boolean removeIf(final FloatPredicate p) {
        final FloatList tmp = new FloatList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, 0f);
        size = tmp.size;

        return true;
    }

    /**
     * Removes duplicate elements from this list, keeping only the first occurrence of each value.
     * The order of elements is preserved. If the list is already sorted, the operation is optimized
     * to run in linear time. For unsorted lists, a LinkedHashSet is used internally to track
     * unique elements while preserving order.
     *
     * @return {@code true} if any duplicate elements were removed, {@code false} if all elements were already unique
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
            final Set<Float> set = N.newLinkedHashSet(size);
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
     * Retains only the elements in this list that are contained in the specified FloatList.
     * In other words, removes from this list all of its elements that are not contained in
     * the specified FloatList. The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param c the FloatList containing elements to be retained in this list. Must not be {@code null}.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final FloatList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all of its elements that are not contained in
     * the specified array. The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param a the array containing elements to be retained in this list. May be {@code null} or empty.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final float[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(FloatList.of(a));
    }

    /**
     * Performs a batch removal operation based on the specified collection and complement flag.
     *
     * @param c the collection of elements to check against
     * @param complement if {@code true}, retain elements in c; if {@code false}, remove elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final FloatList c, final boolean complement) {
        final float[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Float> set = c.toSet();

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
     * Removes the element at the specified position in this list. Shifts any subsequent elements
     * to the left (subtracts one from their indices). Returns the element that was removed from the list.
     *
     * @param index the index of the element to be removed. Must be between 0 (inclusive) and size (exclusive).
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public float delete(final int index) {
        rangeCheck(index);

        final float oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list. The indices array is processed
     * to remove elements efficiently, with remaining elements shifted to fill gaps. The indices
     * must be valid positions within the list.
     *
     * @param indices the array of indices at which elements should be removed. May be {@code null} or empty.
     *                Duplicate indices are handled correctly.
     * @throws IndexOutOfBoundsException if any index is out of range (index &lt; 0 || index &gt;= size())
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final float[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, 0f);
        }

        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes a range of elements from this list. The removal range is defined by fromIndex (inclusive)
     * and toIndex (exclusive). Shifts any subsequent elements to the left (subtracts toIndex-fromIndex
     * from their indices).
     *
     * @param fromIndex the index of the first element to be removed (inclusive). Must be non-negative.
     * @param toIndex the index after the last element to be removed (exclusive). Must be &gt;= fromIndex.
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
     * FloatList list = FloatList.of(0f, 1f, 2f, 3f, 4f, 5f);
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
     * Replaces a range of elements in this list with elements from the specified FloatList.
     * The range to be replaced is defined by fromIndex (inclusive) and toIndex (exclusive).
     * The size of the list may change if the replacement has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the starting index of the range to be replaced (inclusive). Must be non-negative.
     * @param toIndex the ending index of the range to be replaced (exclusive). Must be &gt;= fromIndex.
     * @param replacement the FloatList whose elements will replace the specified range. May be {@code null} or empty.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final FloatList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0F);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with elements from the specified array.
     * The range to be replaced is defined by fromIndex (inclusive) and toIndex (exclusive).
     * The size of the list may change if the replacement array has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the starting index of the range to be replaced (inclusive). Must be non-negative.
     * @param toIndex the ending index of the range to be replaced (exclusive). Must be &gt;= fromIndex.
     * @param replacement the array whose elements will replace the specified range. May be {@code null} or empty.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final float[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0F);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of a specified value with a new value throughout the entire list.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param oldVal the old value to be replaced
     * @param newVal the new value to replace oldVal
     * @return the number of elements that were replaced
     */
    public int replaceAll(final float oldVal, final float newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (Float.compare(elementData[i], oldVal) == 0) {
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
     * @param operator the operator to apply to each element. Must not be {@code null}.
     */
    public void replaceAll(final FloatUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsFloat(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * Elements are tested in order, and those for which the predicate returns {@code true} are replaced.
     *
     * @param predicate the predicate to test each element. Must not be {@code null}.
     * @param newValue the value to replace matching elements with
     * @return {@code true} if any elements were replaced, {@code false} otherwise
     */
    public boolean replaceIf(final FloatPredicate predicate, final float newValue) {
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
     * Replaces all elements in this list with the specified value. After this operation,
     * every element in the list will have the same value.
     *
     * @param val the value to be stored in all elements of the list
     */
    public void fill(final float val) {
        fill(0, size(), val);
    }

    /**
     * Replaces each element in the specified range of this list with the specified value.
     * The range is defined by fromIndex (inclusive) and toIndex (exclusive).
     *
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value. Must be non-negative.
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value. Must be &gt;= fromIndex.
     * @param val the value to be stored in the specified range of the list
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public void fill(final int fromIndex, final int toIndex, final float val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code Float.compare(e, valueToFind) == 0}.
     * This comparison method correctly handles NaN and signed zero values.
     *
     * <p>This method performs a linear search through the list.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element, {@code false} otherwise
     */
    public boolean contains(final float valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified FloatList.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param c the FloatList to be checked for containment in this list. Must not be {@code null}.
     * @return {@code true} if this list contains any element from the specified FloatList, {@code false} if this list
     *         is empty, c is empty, or no elements match
     */
    @Override
    public boolean containsAny(final FloatList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified array.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param a the array to be checked for containment in this list. May be {@code null} or empty.
     * @return {@code true} if this list contains any element from the specified array, {@code false} if this list
     *         is empty, the array is {@code null} or empty, or no elements match
     */
    @Override
    public boolean containsAny(final float[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified FloatList.
     * The frequency of elements is not considered; only presence is checked.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param c the FloatList to be checked for containment in this list. Must not be {@code null}.
     * @return {@code true} if this list contains all distinct elements from the specified FloatList,
     *         {@code false} otherwise. Returns {@code true} if c is empty.
     */
    @Override
    public boolean containsAll(final FloatList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Float> set = this.toSet();

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
     * The frequency of elements is not considered; only presence is checked.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param a the array to be checked for containment in this list. May be {@code null} or empty.
     * @return {@code true} if this list contains all distinct elements from the specified array,
     *         {@code false} otherwise. Returns {@code true} if the array is {@code null} or empty.
     */
    @Override
    public boolean containsAll(final float[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list has no elements in common with the specified FloatList.
     * Two lists are disjoint if they share no common elements.
     *
     * @param c the FloatList to be checked for disjointness with this list. Must not be {@code null}.
     * @return {@code true} if this list has no elements in common with the specified FloatList,
     *         {@code false} if they share at least one element. Returns {@code true} if either list is empty.
     */
    @Override
    public boolean disjoint(final FloatList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Float> set = this.toSet();

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
     * @param b the array to be checked for disjointness with this list. May be {@code null} or empty.
     * @return {@code true} if this list has no elements in common with the specified array,
     *         {@code false} if they share at least one element. Returns {@code true} if either this list
     *         or the array is empty or {@code null}.
     */
    @Override
    public boolean disjoint(final float[] b) {
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
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * FloatList list2 = FloatList.of(1.0f, 2.0f, 2.0f, 4.0f);
     * FloatList result = list1.intersection(list2);   // result will be [1.0f, 2.0f]
     * // One occurrence of '1.0f' (minimum count in both lists) and one occurrence of '2.0f'
     *
     * FloatList list3 = FloatList.of(5.0f, 5.0f, 6.0f);
     * FloatList list4 = FloatList.of(5.0f, 7.0f);
     * FloatList result2 = list3.intersection(list4);   // result will be [5.0f]
     * // One occurrence of '5.0f' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list
     * @return a new FloatList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(float[])
     * @see #difference(FloatList)
     * @see #symmetricDifference(FloatList)
     * @see N#intersection(float[], float[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public FloatList intersection(final FloatList b) {
        if (N.isEmpty(b)) {
            return new FloatList();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(9, size(), b.size()));

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
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * float[] array = new float[] {1.0f, 2.0f, 2.0f, 4.0f};
     * FloatList result = list1.intersection(array);   // result will be [1.0f, 2.0f]
     * // One occurrence of '1.0f' (minimum count in both sources) and one occurrence of '2.0f'
     *
     * FloatList list2 = FloatList.of(5.0f, 5.0f, 6.0f);
     * float[] array2 = new float[] {5.0f, 7.0f};
     * FloatList result2 = list2.intersection(array2);   // result will be [5.0f]
     * // One occurrence of '5.0f' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list
     * @return a new FloatList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(FloatList)
     * @see #difference(float[])
     * @see #symmetricDifference(float[])
     * @see N#intersection(float[], float[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public FloatList intersection(final float[] b) {
        if (N.isEmpty(b)) {
            return new FloatList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * FloatList list2 = FloatList.of(1.0f, 4.0f);
     * FloatList result = list1.difference(list2);   // result will be [1.0f, 2.0f, 3.0f]
     * // One '1.0f' remains because list1 has two occurrences and list2 has one
     *
     * FloatList list3 = FloatList.of(5.0f, 6.0f);
     * FloatList list4 = FloatList.of(5.0f, 5.0f, 6.0f);
     * FloatList result2 = list3.difference(list4);   // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list
     * @return a new FloatList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(float[])
     * @see #symmetricDifference(FloatList)
     * @see #intersection(FloatList)
     * @see N#difference(float[], float[])
     * @see N#difference(int[], int[])
     */
    @Override
    public FloatList difference(final FloatList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(size(), N.max(9, size() - b.size())));

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
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * float[] array = new float[] {1.0f, 4.0f};
     * FloatList result = list1.difference(array);   // result will be [1.0f, 2.0f, 3.0f]
     * // One '1.0f' remains because list1 has two occurrences and array has one
     *
     * FloatList list2 = FloatList.of(5.0f, 6.0f);
     * float[] array2 = new float[] {5.0f, 5.0f, 6.0f};
     * FloatList result2 = list2.difference(array2);   // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param b the array to compare against this list
     * @return a new FloatList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(FloatList)
     * @see #symmetricDifference(float[])
     * @see #intersection(float[])
     * @see N#difference(float[], float[])
     * @see N#difference(int[], int[])
     */
    @Override
    public FloatList difference(final float[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new FloatList containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both lists.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * FloatList list2 = FloatList.of(1.0f, 2.0f, 2.0f, 4.0f);
     * FloatList result = list1.symmetricDifference(list2);
     * // result will contain: [1.0f, 3.0f, 2.0f, 4.0f]
     * // Elements explanation:
     * // - 1.0f appears twice in list1 and once in list2, so one occurrence remains
     * // - 3.0f appears only in list1, so it remains
     * // - 2.0f appears once in list1 and twice in list2, so one occurrence remains
     * // - 4.0f appears only in list2, so it remains
     * }</pre>
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new FloatList containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(float[])
     * @see #difference(FloatList)
     * @see #intersection(FloatList)
     * @see N#symmetricDifference(float[], float[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public FloatList symmetricDifference(final FloatList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();
        final FloatList c = new FloatList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new FloatList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatList list1 = FloatList.of(1.0f, 1.0f, 2.0f, 3.0f);
     * float[] array = new float[] {1.0f, 2.0f, 2.0f, 4.0f};
     * FloatList result = list1.symmetricDifference(array);
     * // result will contain: [1.0f, 3.0f, 2.0f, 4.0f]
     * // Elements explanation:
     * // - 1.0f appears twice in list1 and once in array, so one occurrence remains
     * // - 3.0f appears only in list1, so it remains
     * // - 2.0f appears once in list1 and twice in array, so one occurrence remains
     * // - 4.0f appears only in array, so it remains
     * }</pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new FloatList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(FloatList)
     * @see #difference(float[])
     * @see #intersection(float[])
     * @see N#symmetricDifference(float[], float[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public FloatList symmetricDifference(final float[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Counts the number of occurrences of the specified value in this list.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param valueToFind the value whose occurrences are to be counted
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final float valueToFind) {
        if (size == 0) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0; i < size; i++) {
            if (Float.compare(elementData[i], valueToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or {@code N.INDEX_NOT_FOUND} (-1) if this list does not contain the element.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     *         or {@code N.INDEX_NOT_FOUND} (-1) if this list does not contain the element
     */
    public int indexOf(final float valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * searching forwards from the specified index, or {@code N.INDEX_NOT_FOUND} (-1) if the element is not found.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start searching from (inclusive). May be negative, in which case it is treated as 0.
     * @return the index of the first occurrence of the element at position &gt;= fromIndex,
     *         or {@code N.INDEX_NOT_FOUND} (-1) if the element is not found
     */
    public int indexOf(final float valueToFind, final int fromIndex) {
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
     * or -1 if this list does not contain the element. The comparison is done using
     * Float.compare() to handle NaN values correctly.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int lastIndexOf(final float valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     * The comparison is done using Float.compare() to handle NaN values correctly.
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *                          If &gt;= size, the search starts from the last element.
     * @return the index of the last occurrence of the element at position &lt;= startIndexFromBack,
     *         or -1 if the element is not found or startIndexFromBack is negative
     */
    public int lastIndexOf(final float valueToFind, final int startIndexFromBack) {
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
     * Returns the minimum element in this list wrapped in an OptionalFloat.
     * If the list is empty, returns an empty OptionalFloat. NaN values are handled
     * according to Float.compare() semantics.
     *
     * @return an OptionalFloat containing the minimum element, or an empty OptionalFloat if this list is empty
     */
    public OptionalFloat min() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list wrapped in an OptionalFloat.
     * If the range is empty (fromIndex == toIndex), returns an empty OptionalFloat.
     * NaN values are handled according to Float.compare() semantics.
     *
     * @param fromIndex the index of the first element in the range (inclusive). Must be non-negative.
     * @param toIndex the index after the last element in the range (exclusive). Must be &gt;= fromIndex.
     * @return an OptionalFloat containing the minimum element in the range, or an empty OptionalFloat if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public OptionalFloat min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum value in this list as an OptionalFloat.
     * 
     * @return an OptionalFloat containing the maximum value, or an empty OptionalFloat if this list is empty
     */
    public OptionalFloat max() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum value in the specified range of this list as an OptionalFloat.
     *
     * @param fromIndex the index of the first element (inclusive) to be included in the max calculation
     * @param toIndex the index of the last element (exclusive) to be included in the max calculation
     * @return an OptionalFloat containing the maximum value in the specified range, or an empty OptionalFloat if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalFloat max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * @return an OptionalFloat containing the median value if the list is non-empty, or an empty OptionalFloat if the list is empty
     */
    public OptionalFloat median() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, 0, size));
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
     * @return an OptionalFloat containing the median value if the range is non-empty, or an empty OptionalFloat if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalFloat median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element in this list in sequential order.
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final FloatConsumer action) {
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
     * FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
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
    public void forEach(final int fromIndex, final int toIndex, final FloatConsumer action) throws IndexOutOfBoundsException {
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
     * Returns the first element in this list as an OptionalFloat.
     *
     * @return an OptionalFloat containing the first element, or an empty OptionalFloat if this list is empty
     */
    public OptionalFloat first() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[0]);
    }

    /**
     * Returns the last element in this list as an OptionalFloat.
     *
     * @return an OptionalFloat containing the last element, or an empty OptionalFloat if this list is empty
     */
    public OptionalFloat last() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[size() - 1]);
    }

    /**
     * Returns a new FloatList containing only the distinct elements from the specified range of this list.
     * The order of elements is preserved, keeping the first occurrence of each distinct value.
     *
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index of the last element (exclusive) to include
     * @return a new FloatList containing the distinct elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public FloatList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Returns {@code true} if this list contains any duplicate elements.
     * Two elements are considered duplicates if they are equal according to {@code Float.compare()}.
     *
     * @return {@code true} if this list contains duplicate elements, {@code false} otherwise
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks if the elements in this list are sorted in ascending order.
     * NaN values are considered greater than all other values.
     *
     * @return {@code true} if this list is sorted in ascending order or is empty, {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts the elements in this list in ascending order.
     * This method modifies the list in place.
     * NaN values are sorted to the end of the list.
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements in this list in ascending order using a parallel sort algorithm.
     * This method modifies the list in place and may be faster than {@link #sort()} for large lists.
     * NaN values are sorted to the end of the list.
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements in this list in descending order.
     * This method first sorts the list in ascending order, then reverses it.
     * NaN values will appear at the beginning of the list after reverse sorting.
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value in this list using the binary search algorithm.
     * The list must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined.
     *
     * <p>If the list contains multiple elements equal to the specified value, there is no
     * guarantee which one will be found.</p>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key, if it is contained in the list;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined as
     *         the point at which the key would be inserted into the list: the index of the first
     *         element greater than the key, or {@code size()} if all elements in the list are
     *         less than the specified key
     */
    public int binarySearch(final float valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified value in the specified range of this list using the binary search algorithm.
     * The range must be sorted in ascending order prior to making this call.
     * If it is not sorted, the results are undefined.
     *
     * @param fromIndex the index of the first element (inclusive) to search
     * @param toIndex the index of the last element (exclusive) to search
     * @param valueToFind the value to search for
     * @return the index of the search key, if it is contained in the range;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined as
     *         the point at which the key would be inserted into the range: the index of the first
     *         element greater than the key, or {@code toIndex} if all elements in the range are
     *         less than the specified key
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final float valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * This method modifies the list in place.
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * This method modifies the list in place.
     *
     * @param fromIndex the index of the first element (inclusive) to reverse
     * @param toIndex the index of the last element (exclusive) to reverse
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
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
     * Randomly shuffles the elements in this list using a default source of randomness.
     * All permutations occur with approximately equal likelihood.
     * This method modifies the list in place.
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles the elements in this list using the specified source of randomness.
     * All permutations occur with equal likelihood assuming that the source of randomness is fair.
     * This method modifies the list in place.
     *
     * @param rnd the source of randomness to use to shuffle the list
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * After this method returns, the element previously at position {@code i}
     * will be at position {@code j}, and vice versa.
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either {@code i} or {@code j} is out of range
     *         ({@code i < 0 || i >= size() || j < 0 || j >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a shallow copy of this FloatList instance.
     * The elements themselves are copied.
     *
     * @return a new FloatList containing the same elements as this list
     */
    @Override
    public FloatList copy() {
        return new FloatList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a new FloatList containing a copy of the elements in the specified range of this list.
     *
     * @param fromIndex the index of the first element (inclusive) to copy
     * @param toIndex the index of the last element (exclusive) to copy
     * @return a new FloatList containing a copy of the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public FloatList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new FloatList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a new FloatList containing a copy of the elements in the specified range of this list,
     * selecting elements at the specified step interval.
     *
     * <p>Examples:</p>
     * <ul>
     * <li>If step is 1, all elements in the range are included</li>
     * <li>If step is 2, every other element is included</li>
     * <li>If step is negative and fromIndex &gt; toIndex, elements are selected in reverse order</li>
     * </ul>
     *
     * @param fromIndex the index of the first element (inclusive) to copy. Can be greater than toIndex for reverse iteration
     * @param toIndex the index of the last element (exclusive) to copy. If -1 and fromIndex &gt; toIndex, it's treated as 0
     * @param step the step size for selecting elements. Must not be zero
     * @return a new FloatList containing a copy of the selected elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if step is zero
     * @see N#copyOfRange(float[], int, int, int)
     */
    @Override
    public FloatList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new FloatList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into consecutive chunks of the specified size and returns them as a list of FloatLists.
     * The last chunk may be smaller than the specified size if the range doesn't divide evenly.
     *
     * @param fromIndex the index of the first element (inclusive) to include in the split
     * @param toIndex the index of the last element (exclusive) to include in the split
     * @param chunkSize the desired size of each chunk (must be positive)
     * @return a List of FloatLists, each containing a chunk of elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if chunkSize is not positive
     */
    @Override
    public List<FloatList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<float[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<FloatList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this FloatList instance to be the list's current size.
     * An application can use this operation to minimize the storage of a FloatList instance.
     *
     * @return this FloatList instance
     */
    @Override
    public FloatList trimToSize() {
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
     * Returns a List containing all elements in this FloatList, boxed as Float objects.
     *
     * @return a new List&lt;Float&gt; containing all elements from this list
     */
    @Override
    public List<Float> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing elements in the specified range of this FloatList, boxed as Float objects.
     *
     * @param fromIndex the index of the first element (inclusive) to box
     * @param toIndex the index of the last element (exclusive) to box
     * @return a new List&lt;Float&gt; containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public List<Float> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Float> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new float array containing all elements of this list
     */
    @Override
    public float[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this FloatList to a DoubleList.
     * Each float value is converted to a double value.
     *
     * @return a new DoubleList containing all elements from this list converted to double values
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
     * @param <C> the type of the Collection to return
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index of the last element (exclusive) to include
     * @param supplier a function that creates a new Collection instance with the specified initial capacity
     * @return a Collection containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Float>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index of the last element (exclusive) to include
     * @param supplier a function that creates a new Multiset instance with the specified initial capacity
     * @return a Multiset containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Float> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Float>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Float> multiset = supplier.apply(toIndex - fromIndex);

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
     * @return a FloatIterator over the elements in this list
     */
    @Override
    public FloatIterator iterator() {
        if (isEmpty()) {
            return FloatIterator.EMPTY;
        }

        return FloatIterator.of(elementData, 0, size);
    }

    /**
     * Returns a FloatStream with this list as its source.
     *
     * @return a FloatStream over the elements in this list
     */
    public FloatStream stream() {
        return FloatStream.of(elementData, 0, size());
    }

    /**
     * Returns a FloatStream with the specified range of this list as its source.
     *
     * @param fromIndex the index of the first element (inclusive) to include in the stream
     * @param toIndex the index of the last element (exclusive) to include in the stream
     * @return a FloatStream over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public FloatStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return FloatStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     *
     * @return the first float value in the list
     * @throws NoSuchElementException if this list is empty
     */
    public float getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last float value in the list
     * @throws NoSuchElementException if this list is empty
     */
    public float getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * Shifts any existing elements to the right (adds one to their indices).
     *
     * @param e the element to add at the beginning of this list
     */
    public void addFirst(final float e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e the element to add at the end of this list
     */
    public void addLast(final float e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @return the first float value that was removed from the list
     * @throws NoSuchElementException if this list is empty
     */
    public float removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last float value that was removed from the list
     * @throws NoSuchElementException if this list is empty
     */
    public float removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns a hash code value for this list.
     * The hash code is computed based on the elements in the list and their order.
     *
     * @return a hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns {@code true} if and only if the specified object is also a FloatList,
     * both lists have the same size, and all corresponding pairs of elements in
     * the two lists are equal.
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

        if (obj instanceof FloatList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * The string representation consists of the list's elements in order,
     * enclosed in square brackets ("[]"). Adjacent elements are separated by
     * the characters ", " (comma and space).
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
            elementData = new float[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}
