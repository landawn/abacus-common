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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Builder.ComparisonBuilder;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * A comprehensive factory utility class providing static methods for creating and combining
 * {@link Comparator} instances with sophisticated null handling, type-specific optimizations,
 * and complex comparison scenarios. This final class serves as a central hub for all comparison
 * operations, offering pre-built comparators for common data types and flexible builder patterns
 * for custom comparison logic.
 *
 * <p>Comparators addresses the limitations of the standard Java {@link Comparator} interface by
 * providing consistent null handling semantics, specialized comparators for primitive arrays and
 * collections, and advanced features like case-insensitive string comparison, length-based
 * comparisons, and bean property comparisons. All comparators follow predictable null handling
 * rules and provide both natural and reversed ordering variants.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Comprehensive Null Handling:</b> Consistent null-first and null-last semantics across all comparators</li>
 *   <li><b>Type-Specific Optimizations:</b> Specialized comparators for primitives, arrays, and collections</li>
 *   <li><b>Functional Integration:</b> Key extractor support with lambda expressions and method references</li>
 *   <li><b>Flexible Ordering:</b> Natural and reversed ordering variants for all comparison types</li>
 *   <li><b>Advanced String Handling:</b> Case-insensitive and length-based string comparisons</li>
 *   <li><b>Collection Support:</b> Size-based and element-wise comparisons for collections and maps</li>
 *   <li><b>Bean Integration:</b> Property-based comparison for Java beans and POJOs</li>
 *   <li><b>Optional Handling:</b> Specialized comparators for Optional values with empty-first/last semantics</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Final Class &amp; Design:</b>
 * <ul>
 *   <li>This is a <b>final class</b> that cannot be extended for API stability</li>
 *   <li>All methods are static - no instance creation needed or allowed</li>
 *   <li>All comparators are thread-safe and can be safely shared between threads</li>
 *   <li>Null handling behavior is consistent and well-defined across all methods</li>
 * </ul>
 *
 * <p><b>Null Handling Semantics:</b>
 * <ul>
 *   <li><b>nullsFirst():</b> Treats null as the minimum value (null &lt; any non-null value)</li>
 *   <li><b>nullsLast():</b> Treats null as the maximum value (null &gt; any non-null value)</li>
 *   <li><b>Default Behavior:</b> Most comparators use nullsFirst semantics by default</li>
 *   <li><b>Consistency:</b> Both nulls compare as equal (null == null returns 0)</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Sorting Collections:</b> Providing comparators for Collections.sort() and Stream.sorted()</li>
 *   <li><b>Data Processing:</b> Complex sorting logic for business objects and data structures</li>
 *   <li><b>Search Operations:</b> Binary search with consistent ordering in sorted collections</li>
 *   <li><b>Priority Queues:</b> Custom ordering for priority-based data structures</li>
 *   <li><b>Database Ordering:</b> Mimicking SQL ORDER BY behavior in application code</li>
 *   <li><b>Report Generation:</b> Sorting data for presentation and reporting purposes</li>
 *   <li><b>Algorithm Implementation:</b> Providing ordering for sorting algorithms and tree structures</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic null-safe natural ordering
 * List<String> names = Arrays.asList("Alice", null, "Bob", "Charlie");
 * names.sort(Comparators.nullsFirst());
 * // Result: [null, "Alice", "Bob", "Charlie"]
 *
 * // Key extractor with null handling
 * List<Person> people = Arrays.asList(
 *     new Person("Alice", 30),
 *     new Person(null, 25),
 *     new Person("Bob", 35)
 * );
 * people.sort(Comparators.nullsFirstBy(Person::getName));
 *
 * // Primitive-optimized comparisons
 * List<Person> byAge = people.stream()
 *     .sorted(Comparators.comparingInt(Person::getAge))
 *     .collect(Collectors.toList());
 *
 * // Case-insensitive string comparison
 * List<String> words = Arrays.asList("apple", "BANANA", "Cherry");
 * words.sort(Comparators.comparingIgnoreCase());
 * // Result: ["apple", "BANANA", "Cherry"]
 *
 * // Collection size comparison
 * List<List<String>> lists = Arrays.asList(
 *     Arrays.asList("a", "b"),
 *     Arrays.asList("x"),
 *     Arrays.asList("p", "q", "r")
 * );
 * lists.sort(Comparators.comparingBySize());
 * // Sorted by: [1], [2], [3] elements
 *
 * // Map entry comparisons
 * Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87, "Charlie", 92);
 * List<Map.Entry<String, Integer>> sortedEntries = scores.entrySet().stream()
 *     .sorted(Comparators.comparingByValue())
 *     .collect(Collectors.toList());
 *
 * // Array comparisons with element-wise logic
 * List<int[]> arrays = Arrays.asList(
 *     new int[]{1, 2, 3},
 *     new int[]{1, 2},
 *     new int[]{1, 2, 4}
 * );
 * arrays.sort(Comparators.INT_ARRAY_COMPARATOR);
 *
 * // Reversed ordering variants
 * people.sort(Comparators.reversedComparingInt(Person::getAge));
 *
 * // Optional value handling
 * List<Optional<String>> optionals = Arrays.asList(
 *     Optional.of("Alice"),
 *     Optional.empty(),
 *     Optional.of("Bob")
 * );
 * optionals.sort(Comparators.emptiesFirst());
 * // Result: [empty, "Alice", "Bob"]
 * }</pre>
 *
 * <p><b>Predefined Comparators:</b>
 * <ul>
 *   <li><b>Array Comparators:</b> Element-wise comparison for all primitive array types</li>
 *   <li><b>Collection Comparators:</b> Size-based and element-wise comparison for collections</li>
 *   <li><b>String Comparators:</b> Case-insensitive and length-based string comparison</li>
 *   <li><b>Natural Order:</b> Null-safe natural ordering for Comparable types</li>
 *   <li><b>Reversed Order:</b> Inverted natural ordering with consistent null handling</li>
 * </ul>
 *
 * <p><b>Factory Method Categories:</b>
 * <ul>
 *   <li><b>Basic Ordering:</b> {@code naturalOrder()}, {@code reverseOrder()}, {@code nullsFirst()}, {@code nullsLast()}</li>
 *   <li><b>Key Extraction:</b> {@code comparingBy()}, {@code comparingInt()}, {@code comparingLong()}, etc.</li>
 *   <li><b>String Comparison:</b> {@code comparingIgnoreCase()}, {@code comparingByLength()}</li>
 *   <li><b>Collection Comparison:</b> {@code comparingBySize()}, {@code comparingCollection()}</li>
 *   <li><b>Array Comparison:</b> {@code comparingArray()}, {@code comparingByArrayLength()}</li>
 *   <li><b>Map Comparison:</b> {@code comparingByKey()}, {@code comparingByValue()}, {@code comparingMapByKey()}</li>
 *   <li><b>Optional Handling:</b> {@code emptiesFirst()}, {@code emptiesLast()}</li>
 * </ul>
 *
 * <p><b>Key Extractor Patterns:</b>
 * All key extractor methods follow consistent naming and behavior patterns:
 * <ul>
 *   <li><b>Primitive Extractors:</b> {@code comparingInt()}, {@code comparingLong()}, {@code comparingDouble()}, etc.</li>
 *   <li><b>Object Extractors:</b> {@code comparingBy()} for Comparable objects</li>
 *   <li><b>Null-Safe Variants:</b> {@code comparingByIfNotNullOrElseNullsFirst/Last()}</li>
 *   <li><b>Reversed Variants:</b> {@code reversedComparingInt()}, {@code reversedComparingBy()}, etc.</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Primitive comparators: O(1) time with no boxing overhead</li>
 *   <li>Array comparators: O(min(m,n)) where m,n are array lengths</li>
 *   <li>Collection comparators: O(min(m,n)) for element-wise, O(1) for size-based</li>
 *   <li>String comparators: O(min(m,n)) for content comparison, O(1) for length</li>
 *   <li>Memory usage: No additional allocations for most operations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * All comparators created by this class are <b>thread-safe</b>:
 * <ul>
 *   <li>Comparator instances are stateless and immutable</li>
 *   <li>Can be safely shared between multiple threads</li>
 *   <li>No synchronization required for concurrent use</li>
 *   <li>Ideal for static final fields and shared constants</li>
 * </ul>
 *
 * <p><b>Array Comparison Details:</b>
 * <ul>
 *   <li><b>Element-wise:</b> Compares arrays lexicographically element by element</li>
 *   <li><b>Length Handling:</b> Shorter arrays are considered "less than" longer arrays when all compared elements are equal</li>
 *   <li><b>Null Arrays:</b> Handled according to the chosen null semantics</li>
 *   <li><b>Type Safety:</b> Separate comparators for each primitive array type</li>
 * </ul>
 *
 * <p><b>Collection Comparison Strategies:</b>
 * <ul>
 *   <li><b>Size-based:</b> Compare collections by their size only</li>
 *   <li><b>Element-wise:</b> Lexicographic comparison of collection elements</li>
 *   <li><b>Iterator-based:</b> Works with any Iterable, including custom implementations</li>
 *   <li><b>Type Flexibility:</b> Generic typing supports various collection types</li>
 * </ul>
 *
 * <p><b>Integration with Java Streams:</b>
 * <ul>
 *   <li>Perfect integration with {@code Stream.sorted()} operations</li>
 *   <li>Chainable with {@code Comparator.thenComparing()} for multi-level sorting</li>
 *   <li>Compatible with parallel streams for high-performance sorting</li>
 *   <li>Works seamlessly with collectors and terminal operations</li>
 * </ul>
 *
 * <p><b>Advanced Features:</b>
 * <ul>
 *   <li><b>Conditional Null Handling:</b> Different null strategies based on context</li>
 *   <li><b>Multi-level Sorting:</b> Chain comparators for complex sort orders</li>
 *   <li><b>Type-specific Optimization:</b> Avoid boxing for primitive comparisons</li>
 *   <li><b>Functional Composition:</b> Combine with method references and lambdas</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use primitive-specific methods (comparingInt vs comparingBy) for better performance</li>
 *   <li>Cache frequently used comparators in static final fields</li>
 *   <li>Choose appropriate null handling strategy based on domain requirements</li>
 *   <li>Use method references with key extractors for readable and efficient code</li>
 *   <li>Combine comparators with thenComparing() for multi-level sorting</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link IllegalArgumentException} for null key extractors</li>
 *   <li>Handles null inputs gracefully according to configured null semantics</li>
 *   <li>Preserves natural ordering contracts for Comparable types</li>
 *   <li>Provides consistent behavior across all comparison operations</li>
 * </ul>
 *
 * <p><b>Comparison with JDK Comparator:</b>
 * <ul>
 *   <li><b>Enhanced Null Handling:</b> More sophisticated and consistent null strategies</li>
 *   <li><b>Additional Types:</b> Built-in support for arrays, collections, and complex types</li>
 *   <li><b>Performance Optimizations:</b> Primitive-specific comparators avoid boxing</li>
 *   <li><b>Convenience Methods:</b> More factory methods for common comparison scenarios</li>
 * </ul>
 *
 * <p><b>Constants:</b>
 * <ul>
 *   <li>{@code NATURAL_ORDER} - Default natural ordering with nulls first</li>
 *   <li>{@code REVERSED_ORDER} - Reversed natural ordering with nulls last</li>
 *   <li>{@code *_ARRAY_COMPARATOR} - Predefined comparators for each primitive array type</li>
 *   <li>{@code COLLECTION_COMPARATOR} - Element-wise collection comparison</li>
 * </ul>
 *
 * <p><b>Integration Points:</b>
 * <ul>
 *   <li><b>{@link ComparisonBuilder}:</b> Fluent chained comparisons</li>
 *   <li><b>{@link Comparator}:</b> Full compatibility with standard Java Comparator interface</li>
 *   <li><b>{@link Collections}:</b> Works with Collections.sort() and related methods</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Seamless integration with stream sorting operations</li>
 * </ul>
 *
 * <p><b>Memory Efficiency:</b>
 * <ul>
 *   <li>Reuse predefined static comparators when possible</li>
 *   <li>Primitive comparators avoid autoboxing overhead</li>
 *   <li>Lambda-based implementations minimize object allocation</li>
 *   <li>Consider caching for frequently used custom comparators</li>
 * </ul>
 *
 * @see Comparator
 * @see Comparable
 * @see Collections#sort(java.util.List, Comparator)
 * @see java.util.stream.Stream#sorted(Comparator)
 * @see ComparisonBuilder
 */
public final class Comparators {

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_FIRST_COMPARATOR = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_FIRST_REVERSED_ORDER = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : b.compareTo(a));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_LAST_COMPARATOR = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : a.compareTo(b));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_LAST_REVERSED_ORDER = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : b.compareTo(a));

    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_ORDER = NULL_FIRST_COMPARATOR;

    // Reversed comparison order (b.compareTo(a)) with nulls treated as greater (nulls last)
    @SuppressWarnings("rawtypes")
    static final Comparator REVERSED_ORDER = NULL_LAST_REVERSED_ORDER;

    static final Comparator<String> COMPARING_IGNORE_CASE = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));

    static final Comparator<CharSequence> COMPARING_BY_LENGTH = Comparator.comparingInt(a -> a == null ? 0 : a.length());

    static final Comparator<Object> COMPARING_BY_ARRAY_LENGTH = Comparator.comparingInt(a -> a == null ? 0 : Array.getLength(a));

    @SuppressWarnings("rawtypes")
    static final Comparator<Collection> COMPARING_BY_SIZE = Comparator.comparingInt(a -> a == null ? 0 : a.size());

    @SuppressWarnings("rawtypes")
    static final Comparator<Map> COMPARING_BY_MAP_SIZE = Comparator.comparingInt(a -> a == null ? 0 : a.size());

    /**
     * A comparator for boolean arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * In element comparison, {@code false} is considered less than {@code true}.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<boolean[]> BOOLEAN_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for char arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Characters are compared by their numeric values.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<char[]> CHAR_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for byte arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Bytes are compared as signed values.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<byte[]> BYTE_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for short arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Shorts are compared as signed values.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for int arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Integers are compared as signed values.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<int[]> INT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for long arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Longs are compared as signed values.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<long[]> LONG_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for float arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Floats are compared using {@link Float#compare(float, float)} to handle NaN and -0.0f correctly.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<float[]> FLOAT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = Float.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for double arrays that compares elements lexicographically.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Doubles are compared using {@link Double#compare(double, double)} to handle NaN and -0.0 correctly.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<double[]> DOUBLE_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = Double.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for Object arrays that compares elements lexicographically using natural ordering.
     * The comparison is performed element by element until a difference is found.
     * If all compared elements are equal, the shorter array is considered less than the longer array.
     * Elements are compared using their natural ordering with {@code null} considered as the minimum value.
     * Null arrays are handled with {@code null} considered as the minimum value.
     */
    public static final Comparator<Object[]> OBJECT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = NATURAL_ORDER.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    /**
     * A comparator for Collections that compares elements lexicographically using natural ordering.
     * The comparison is performed by iterating through elements until a difference is found.
     * If all compared elements are equal, the smaller collection is considered less than the larger collection.
     * Elements are compared using their natural ordering with {@code null} considered as the minimum value.
     * Empty collections are considered less than non-empty collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Comparator<Collection> COLLECTION_COMPARATOR = (a, b) -> {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? 0 : -1;
        } else if (N.isEmpty(b)) {
            return 1;
        }

        final Iterator<Object> iterA = a.iterator();
        final Iterator<Object> iterB = b.iterator();

        final int lenA = N.size(a);
        final int lenB = N.size(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = NATURAL_ORDER.compare(iterA.next(), iterB.next());

            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(lenA, lenB);
    };

    private Comparators() {
        // Utility class.
    }

    /**
     * Returns a comparator that compares {@link Comparable} objects in their natural order.
     * Null values are considered less than {@code non-null} values (nulls first).
     * This method is equivalent to {@link #nullsFirst()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("banana", null, "apple", "cherry");
     * list.sort(Comparators.naturalOrder());
     * // Result: [null, "apple", "banana", "cherry"]
     * }</pre>
     *
     * @param <T> the type of the objects being compared, must extend Comparable
     * @return a comparator that imposes the natural ordering with nulls first
     * @see #nullsFirst()
     * @see #reverseOrder()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable> Comparator<T> naturalOrder() {
        return NATURAL_ORDER;
    }

    /**
     * Returns a comparator that compares {@link Comparable} objects in their natural order
     * with {@code null} values considered less than {@code non-null} values.
     * This method is equivalent to {@link #naturalOrder()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list = Arrays.asList(3, null, 1, 4, null, 2);
     * list.sort(Comparators.nullsFirst());
     * // Result: [null, null, 1, 2, 3, 4]
     * }</pre>
     *
     * @param <T> the type of the objects being compared, must extend Comparable
     * @return a comparator that considers {@code null} less than {@code non-null} values, comparing {@code non-null} values in natural order
     * @see #naturalOrder()
     * @see #nullsLast()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> nullsFirst() {
        return (Comparator<T>) NULL_FIRST_COMPARATOR;
    }

    /**
     * Returns a comparator that considers {@code null} values to be less than {@code non-null} values.
     * When both values are {@code non-null}, the specified comparator is used for comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
     * List<String> list = Arrays.asList("long", null, "a", "medium");
     * list.sort(Comparators.nullsFirst(lengthComparator));
     * // Result: [null, "a", "long", "medium"]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param cmp the comparator to use for {@code non-null} values, may be null
     * @return a comparator that considers {@code null} less than {@code non-null} values, comparing {@code non-null} values using the specified comparator
     */
    public static <T> Comparator<T> nullsFirst(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_FIRST_COMPARATOR) { // NOSONAR
            return (Comparator<T>) NULL_FIRST_COMPARATOR;
        }

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(a, b));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key using the provided function.
     * The extracted keys are compared using natural ordering with {@code null} keys considered less than {@code non-null} keys.
     * This is a convenience method that combines key extraction with null-safe natural ordering comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     Integer age;
     * }
     * List<Person> people = ...;
     * people.sort(Comparators.nullsFirstBy(person -> person.age));
     * // Sorts by age with null ages first
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract the comparable key from objects
     * @return a comparator that compares by extracted keys with nulls first
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> nullsFirstBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NULL_FIRST_COMPARATOR.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    @SuppressWarnings("rawtypes")
    private static final Comparator NULLS_FIRST_OR_ELSE_EQUAL = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : 0);

    /**
     * Returns a comparator that considers {@code null} values to be less than {@code non-null} values,
     * but treats all {@code non-null} values as equal. This is useful when you only want to
     * separate {@code null} from {@code non-null} values without ordering the {@code non-null} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("b", null, "a", null, "c");
     * list.sort(Comparators.nullsFirstOrElseEqual());
     * // Result: [null, null, "b", "a", "c"] (non-null values remain in original order)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @return a comparator that puts nulls first and treats all {@code non-null} values as equal
     * @see #nullsFirst()
     * @see #nullsLastOrElseEqual()
     */
    @Beta
    public static <T> Comparator<T> nullsFirstOrElseEqual() {
        return NULLS_FIRST_OR_ELSE_EQUAL;
    }

    /**
     * Returns a comparator that compares {@link Comparable} objects in their natural order
     * with {@code null} values considered greater than {@code non-null} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("banana", null, "apple", "cherry");
     * list.sort(Comparators.nullsLast());
     * // Result: ["apple", "banana", "cherry", null]
     * }</pre>
     *
     * @param <T> the type of the objects being compared, must extend Comparable
     * @return a comparator that considers {@code null} greater than {@code non-null} values, comparing {@code non-null} values in natural order
     * @see #nullsFirst()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> nullsLast() {
        return (Comparator<T>) NULL_LAST_COMPARATOR;
    }

    /**
     * Returns a comparator that considers {@code null} values to be greater than {@code non-null} values.
     * When both values are {@code non-null}, the specified comparator is used for comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> reverseComparator = Comparator.reverseOrder();
     * List<String> list = Arrays.asList("b", null, "a", "c");
     * list.sort(Comparators.nullsLast(reverseComparator));
     * // Result: ["c", "b", "a", null]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param cmp the comparator to use for {@code non-null} values, may be null
     * @return a comparator that considers {@code null} greater than {@code non-null} values, comparing {@code non-null} values using the specified comparator
     */
    public static <T> Comparator<T> nullsLast(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_LAST_COMPARATOR) { // NOSONAR
            return (Comparator<T>) NULL_LAST_COMPARATOR;
        }

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(a, b));
    }

    @SuppressWarnings("rawtypes")
    private static final Comparator NULLS_LAST_OR_ELSE_EQUAL = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : 0);

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key using the provided function.
     * The extracted keys are compared using natural ordering with {@code null} keys considered greater than {@code non-null} keys.
     * This is a convenience method that combines key extraction with null-safe natural ordering comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Product {
     *     String name;
     *     Double price;
     * }
     * List<Product> products = ...;
     * products.sort(Comparators.nullsLastBy(product -> product.price));
     * // Sorts by price with null prices last
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract the comparable key from objects
     * @return a comparator that compares by extracted keys with nulls last
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> nullsLastBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NULL_LAST_COMPARATOR.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Returns a comparator that considers {@code null} values to be greater than {@code non-null} values,
     * but treats all {@code non-null} values as equal. This is useful when you only want to
     * separate {@code null} from {@code non-null} values without ordering the {@code non-null} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("b", null, "a", null, "c");
     * list.sort(Comparators.nullsLastOrElseEqual());
     * // Result: ["b", "a", "c", null, null] (non-null values remain in original order)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @return a comparator that puts nulls last and treats all {@code non-null} values as equal
     * @see #nullsLast()
     * @see #nullsFirstOrElseEqual()
     */
    @Beta
    public static <T> Comparator<T> nullsLastOrElseEqual() {
        return NULLS_LAST_OR_ELSE_EQUAL;
    }

    /**
     * Returns a comparator for {@link u.Optional} that considers empty optionals to be less than
     * present optionals. When both optionals are present, their values are compared using
     * their natural ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Optional<String>> list = Arrays.asList(
     *     Optional.of("b"), Optional.empty(), Optional.of("a")
     * );
     * list.sort(Comparators.emptiesFirst());
     * // Result: [Optional.empty(), Optional.of("a"), Optional.of("b")]
     * }</pre>
     *
     * @param <T> the type of the optional value, must extend Comparable
     * @return a comparator that treats empty optionals as less than present optionals
     * @see #emptiesLast()
     * @see #emptiesFirst(Comparator)
     */
    public static <T extends Comparable<? super T>> Comparator<u.Optional<T>> emptiesFirst() {
        return emptiesFirst(naturalOrder());
    }

    /**
     * Returns a comparator for {@link u.Optional} that considers empty optionals to be less than
     * present optionals. When both optionals are present, their values are compared using
     * the specified comparator. Null optionals are treated the same as empty optionals.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
     * List<Optional<String>> list = Arrays.asList(
     *     Optional.of("long"), Optional.empty(), Optional.of("a")
     * );
     * list.sort(Comparators.emptiesFirst(lengthComparator));
     * // Result: [Optional.empty(), Optional.of("a"), Optional.of("long")]
     * }</pre>
     *
     * @param <T> the type of the optional value
     * @param cmp the comparator to use for comparing present values
     * @return a comparator that treats empty optionals as less than present optionals
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T> Comparator<u.Optional<T>> emptiesFirst(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        // return Comparators.<u.Optional<T>, T> comparingBy(o -> o.orElse(null), Comparator.nullsFirst(cmp));

        return (a, b) -> a == null || a.isEmpty() ? (b == null || b.isEmpty() ? 0 : -1) : (b == null || b.isEmpty() ? 1 : cmp.compare(a.get(), b.get()));
    }

    /**
     * Returns a comparator for {@link u.Optional} that considers empty optionals to be greater than
     * present optionals. When both optionals are present, their values are compared using
     * their natural ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Optional<Integer>> list = Arrays.asList(
     *     Optional.of(2), Optional.empty(), Optional.of(1)
     * );
     * list.sort(Comparators.emptiesLast());
     * // Result: [Optional.of(1), Optional.of(2), Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of the optional value, must extend Comparable
     * @return a comparator that treats empty optionals as greater than present optionals
     * @see #emptiesFirst()
     * @see #emptiesLast(Comparator)
     */
    public static <T extends Comparable<? super T>> Comparator<u.Optional<T>> emptiesLast() {
        return emptiesLast(naturalOrder());
    }

    /**
     * Returns a comparator for {@link u.Optional} that considers empty optionals to be greater than
     * present optionals. When both optionals are present, their values are compared using
     * the specified comparator. Null optionals are treated the same as empty optionals.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> reverseComparator = Comparator.reverseOrder();
     * List<Optional<String>> list = Arrays.asList(
     *     Optional.of("a"), Optional.empty(), Optional.of("b")
     * );
     * list.sort(Comparators.emptiesLast(reverseComparator));
     * // Result: [Optional.of("b"), Optional.of("a"), Optional.empty()]
     * }</pre>
     *
     * @param <T> the type of the optional value
     * @param cmp the comparator to use for comparing present values
     * @return a comparator that treats empty optionals as greater than present optionals
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T> Comparator<u.Optional<T>> emptiesLast(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        // return Comparators.<u.Optional<T>, T> comparingBy(o -> o.orElse(null), Comparator.nullsLast(cmp));

        return (a, b) -> a == null || a.isEmpty() ? (b == null || b.isEmpty() ? 0 : 1) : (b == null || b.isEmpty() ? -1 : cmp.compare(a.get(), b.get()));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key using the provided function.
     * The extracted keys are compared using natural ordering. This method treats {@code null} keys as the minimum value
     * (equivalent to using nullsFirst for the key comparison).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     * }
     * List<Person> people = ...;
     * people.sort(Comparators.comparingBy(person -> person.name));
     * // Sorts people by name in natural order
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract the comparable key from objects
     * @return a comparator that compares by extracted keys using natural ordering
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #nullsFirstBy(Function)
     * @see #nullsLastBy(Function)
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> comparingBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NATURAL_ORDER.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key using the provided function,
     * but only compares {@code non-null} objects. If either object being compared is {@code null}, it is treated as less than
     * any {@code non-null} object. The extracted keys are compared using natural ordering with nulls first.
     *
     * <p>This method is useful when you want to handle {@code null} objects specially while still comparing
     * their extracted keys with null-safe natural ordering.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 30),
     *     null,
     *     new Person("Bob", null),
     *     new Person("Charlie", 25)
     * );
     * people.sort(Comparators.comparingByIfNotNullOrElseNullsFirst(p -> p.age));
     * // Result: [null, Person("Bob", null), Person("Charlie", 25), Person("Alice", 30)]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract the comparable key from objects
     * @return a comparator that handles {@code null} objects and {@code null} keys appropriately
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #comparingBy(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    @Beta
    public static <T> Comparator<T> comparingByIfNotNullOrElseNullsFirst(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_FIRST_COMPARATOR;

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key using the provided function,
     * but only compares {@code non-null} objects. If either object being compared is {@code null}, it is treated as greater than
     * any {@code non-null} object. The extracted keys are compared using natural ordering with nulls last.
     *
     * <p>This method is useful when you want to handle {@code null} objects specially while still comparing
     * their extracted keys with null-safe natural ordering.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 30),
     *     null,
     *     new Person("Bob", null),
     *     new Person("Charlie", 25)
     * );
     * people.sort(Comparators.comparingByIfNotNullOrElseNullsLast(p -> p.age));
     * // Result: [Person("Charlie", 25), Person("Alice", 30), Person("Bob", null), null]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract the comparable key from objects
     * @return a comparator that handles {@code null} objects and {@code null} keys appropriately
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #comparingBy(Function)
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     */
    @Beta
    public static <T> Comparator<T> comparingByIfNotNullOrElseNullsLast(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_LAST_COMPARATOR;

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares objects by extracting a key using the provided function
     * and comparing the keys with the specified comparator. This provides full control over
     * both the key extraction and the comparison logic.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     Address address;
     * }
     * class Address {
     *     String city;
     * }
     * Comparator<String> cityComparator = String.CASE_INSENSITIVE_ORDER;
     * List<Person> people = ...;
     * people.sort(Comparators.comparingBy(p -> p.address.city, cityComparator));
     * // Sorts people by city name case-insensitively
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param <U> the type of the keys extracted for comparison
     * @param keyExtractor the function to extract keys from objects
     * @param keyComparator the comparator to use for comparing extracted keys
     * @return a comparator that compares objects by their extracted keys
     * @throws IllegalArgumentException if keyExtractor or keyComparator is null
     * @see #comparingByIfNotNullOrElseNullsFirst(Function, Comparator)
     * @see #comparingByIfNotNullOrElseNullsLast(Function, Comparator)
     */
    public static <T, U> Comparator<T> comparingBy(final Function<? super T, ? extends U> keyExtractor, final Comparator<? super U> keyComparator)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Returns a comparator that compares objects by extracting a key using the provided function
     * and comparing the keys with the specified comparator, but only for {@code non-null} objects.
     * If either object being compared is {@code null}, it is treated as less than any {@code non-null} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<LocalDate> dateComparator = Comparator.naturalOrder();
     * List<Event> events = Arrays.asList(
     *     new Event("A", LocalDate.of(2023, 1, 15)),
     *     null,
     *     new Event("B", LocalDate.of(2023, 1, 10))
     * );
     * events.sort(Comparators.comparingByIfNotNullOrElseNullsFirst(
     *     e -> e.date, dateComparator));
     * // Result: [null, Event("B", 2023-01-10), Event("A", 2023-01-15)]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param <U> the type of the keys extracted for comparison
     * @param keyExtractor the function to extract keys from objects
     * @param keyComparator the comparator to use for comparing extracted keys
     * @return a comparator that handles {@code null} objects appropriately
     * @throws IllegalArgumentException if keyExtractor or keyComparator is null
     */
    public static <T, U> Comparator<T> comparingByIfNotNullOrElseNullsFirst(final Function<? super T, ? extends U> keyExtractor,
            final Comparator<? super U> keyComparator) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares objects by extracting a key using the provided function
     * and comparing the keys with the specified comparator, but only for {@code non-null} objects.
     * If either object being compared is {@code null}, it is treated as greater than any {@code non-null} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<BigDecimal> priceComparator = Comparator.reverseOrder();
     * List<Product> products = Arrays.asList(
     *     new Product("A", new BigDecimal("19.99")),
     *     null,
     *     new Product("B", new BigDecimal("29.99"))
     * );
     * products.sort(Comparators.comparingByIfNotNullOrElseNullsLast(
     *     p -> p.price, priceComparator));
     * // Result: [Product("B", 29.99), Product("A", 19.99), null]
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param <U> the type of the keys extracted for comparison
     * @param keyExtractor the function to extract keys from objects
     * @param keyComparator the comparator to use for comparing extracted keys
     * @return a comparator that handles {@code null} objects appropriately
     * @throws IllegalArgumentException if keyExtractor or keyComparator is null
     */
    public static <T, U> Comparator<T> comparingByIfNotNullOrElseNullsLast(final Function<? super T, ? extends U> keyExtractor,
            final Comparator<? super U> keyComparator) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares objects by extracting a boolean value using the provided function.
     * Boolean values are compared with {@code false} considered less than {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Task {
     *     String name;
     *     boolean completed;
     * }
     * List<Task> tasks = ...;
     * tasks.sort(Comparators.comparingBoolean(task -> task.completed));
     * // Result: incomplete tasks first, then completed tasks
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract boolean values from objects
     * @return a comparator that compares by extracted boolean values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingBoolean(final ToBooleanFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Boolean.compare(keyExtractor.applyAsBoolean(a), keyExtractor.applyAsBoolean(b));
    }

    /**
     * Returns a comparator that compares objects by extracting a char value using the provided function.
     * Characters are compared by their numeric (Unicode code point) values.
     * This method delegates to {@link Comparator#comparingInt(ToIntFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Grade {
     *     String student;
     *     char letter;  // 'A', 'B', 'C', etc.
     * }
     * List<Grade> grades = ...;
     * grades.sort(Comparators.comparingChar(grade -> grade.letter));
     * // Result: grades sorted by letter (A, B, C, ...)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract char values from objects
     * @return a comparator that compares by extracted char values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingChar(final ToCharFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsChar);
    }

    /**
     * Returns a comparator that compares objects by extracting a byte value using the provided function.
     * Byte values are compared as signed values.
     * This method delegates to {@link Comparator#comparingInt(ToIntFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Packet {
     *     byte priority;
     *     byte[] data;
     * }
     * List<Packet> packets = ...;
     * packets.sort(Comparators.comparingByte(packet -> packet.priority));
     * // Result: packets sorted by priority (lowest to highest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract byte values from objects
     * @return a comparator that compares by extracted byte values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingByte(final ToByteFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsByte);
    }

    /**
     * Returns a comparator that compares objects by extracting a short value using the provided function.
     * Short values are compared as signed values.
     * This method delegates to {@link Comparator#comparingInt(ToIntFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Product {
     *     String name;
     *     short quantity;
     * }
     * List<Product> inventory = ...;
     * inventory.sort(Comparators.comparingShort(product -> product.quantity));
     * // Result: products sorted by quantity (lowest to highest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract short values from objects
     * @return a comparator that compares by extracted short values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingShort(final ToShortFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsShort);
    }

    /**
     * Returns a comparator that compares objects by extracting an int value using the provided function.
     * Integer values are compared as signed values.
     * This method delegates to {@link Comparator#comparingInt(ToIntFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     * }
     * List<Person> people = ...;
     * people.sort(Comparators.comparingInt(person -> person.age));
     * // Result: people sorted by age (youngest to oldest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract int values from objects
     * @return a comparator that compares by extracted int values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingInt(final ToIntFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor);
    }

    /**
     * Returns a comparator that compares objects by extracting a long value using the provided function.
     * Long values are compared as signed values.
     * This method delegates to {@link Comparator#comparingLong(ToLongFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class File {
     *     String name;
     *     long size;
     * }
     * List<File> files = ...;
     * files.sort(Comparators.comparingLong(file -> file.size));
     * // Result: files sorted by size (smallest to largest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract long values from objects
     * @return a comparator that compares by extracted long values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingLong(final ToLongFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingLong(keyExtractor);
    }

    /**
     * Returns a comparator that compares objects by extracting a float value using the provided function.
     * Float values are compared using {@link Float#compare(float, float)} to properly handle NaN and -0.0f.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Measurement {
     *     String name;
     *     float value;
     * }
     * List<Measurement> measurements = ...;
     * measurements.sort(Comparators.comparingFloat(m -> m.value));
     * // Result: measurements sorted by value (lowest to highest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract float values from objects
     * @return a comparator that compares by extracted float values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingFloat(final ToFloatFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Float.compare(keyExtractor.applyAsFloat(a), keyExtractor.applyAsFloat(b));
    }

    /**
     * Returns a comparator that compares objects by extracting a double value using the provided function.
     * Double values are compared using {@link Double#compare(double, double)} to properly handle NaN and -0.0.
     * This method delegates to {@link Comparator#comparingDouble(ToDoubleFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Score {
     *     String player;
     *     double points;
     * }
     * List<Score> scores = ...;
     * scores.sort(Comparators.comparingDouble(score -> score.points));
     * // Result: scores sorted by points (lowest to highest)
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract double values from objects
     * @return a comparator that compares by extracted double values
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingDouble(final ToDoubleFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingDouble(keyExtractor);
    }

    /**
     * Returns a comparator that compares strings ignoring case differences.
     * Null values are considered less than {@code non-null} values. This comparator
     * uses {@link String#compareToIgnoreCase(String)} for the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("john", "Alice", null, "BOB");
     * names.sort(Comparators.comparingIgnoreCase());
     * // Result: [null, "Alice", "BOB", "john"]
     * }</pre>
     *
     * @return a case-insensitive string comparator with nulls first
     */
    public static Comparator<String> comparingIgnoreCase() {
        return COMPARING_IGNORE_CASE;
    }

    /**
     * Returns a comparator that compares objects by extracting a String value using the provided function
     * and comparing them ignoring case differences. Null values are considered less than {@code non-null} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User {
     *     String username;
     *     String email;
     * }
     * List<User> users = ...;
     * users.sort(Comparators.comparingIgnoreCase(user -> user.email));
     * // Result: users sorted by email case-insensitively
     * }</pre>
     *
     * @param <T> the type of the objects being compared
     * @param keyExtractor the function to extract String values from objects
     * @return a comparator that performs case-insensitive comparison on extracted strings
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> comparingIgnoreCase(final Function<? super T, String> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> COMPARING_IGNORE_CASE.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Returns a comparator for {@link Map.Entry} objects that compares entries by their keys
     * using the keys' natural ordering. Keys must implement {@link Comparable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map.Entry<String, Integer>> entries = map.entrySet().stream()
     *     .collect(Collectors.toList());
     * entries.sort(Comparators.comparingByKey());
     * // Result: entries sorted by key in natural order
     * }</pre>
     *
     * @param <K> the key type, must extend Comparable
     * @param <V> the value type
     * @return a comparator that compares map entries by their keys
     */
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return (a, b) -> NATURAL_ORDER.compare(a.getKey(), b.getKey());
    }

    /**
     * Returns a comparator for {@link Map.Entry} objects that compares entries by their values
     * using the values' natural ordering. Values must implement {@link Comparable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores = ...;
     * List<Map.Entry<String, Integer>> sortedScores = scores.entrySet().stream()
     *     .sorted(Comparators.comparingByValue())
     *     .collect(Collectors.toList());
     * // Result: entries sorted by score value (lowest to highest)
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type, must extend Comparable
     * @return a comparator that compares map entries by their values
     */
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (a, b) -> NATURAL_ORDER.compare(a.getValue(), b.getValue());
    }

    /**
     * Returns a comparator for {@link Map.Entry} objects that compares entries by their keys
     * using the specified comparator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
     * List<Map.Entry<String, Integer>> entries = ...;
     * entries.sort(Comparators.comparingByKey(lengthComparator));
     * // Result: entries sorted by key length
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp the comparator to use for comparing keys
     * @return a comparator that compares map entries by their keys
     * @throws IllegalArgumentException if cmp is null
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> cmp.compare(a.getKey(), b.getKey());
    }

    /**
     * Returns a comparator for {@link Map.Entry} objects that compares entries by their values
     * using the specified comparator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Integer> reverseOrder = Comparator.reverseOrder();
     * Map<String, Integer> scores = ...;
     * List<Map.Entry<String, Integer>> topScores = scores.entrySet().stream()
     *     .sorted(Comparators.comparingByValue(reverseOrder))
     *     .collect(Collectors.toList());
     * // Result: entries sorted by score value (highest to lowest)
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp the comparator to use for comparing values
     * @return a comparator that compares map entries by their values
     * @throws IllegalArgumentException if cmp is null
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> cmp.compare(a.getValue(), b.getValue());
    }

    /**
     * Returns a comparator that compares {@link CharSequence} objects by their length.
     * Null values are treated as having length 0. This comparator can be used with
     * String, StringBuilder, StringBuffer, and other CharSequence implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Arrays.asList("short", "a", "medium", "very long string");
     * words.sort(Comparators.comparingByLength());
     * // Result: ["a", "short", "medium", "very long string"]
     * }</pre>
     *
     * @param <T> the type of CharSequence being compared
     * @return a comparator that compares CharSequences by length
     */
    public static <T extends CharSequence> Comparator<T> comparingByLength() {
        return (Comparator<T>) COMPARING_BY_LENGTH;
    }

    /**
     * Returns a comparator that compares arrays by their length.
     * This works with any array type (primitive or object arrays).
     * Null arrays are treated as having length 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<int[]> arrays = Arrays.asList(
     *     new int[] {1, 2, 3},
     *     new int[] {1},
     *     new int[] {1, 2, 3, 4, 5}
     * );
     * arrays.sort(Comparators.comparingByArrayLength());
     * // Result: arrays sorted by length: [1], [1,2,3], [1,2,3,4,5]
     * }</pre>
     *
     * @param <T> the type of array being compared
     * @return a comparator that compares arrays by length
     */
    public static <T> Comparator<T> comparingByArrayLength() {
        return (Comparator<T>) COMPARING_BY_ARRAY_LENGTH;
    }

    /**
     * Returns a comparator that compares {@link Collection} objects by their size.
     * Null collections are treated as having size 0. This works with List, Set,
     * and other Collection implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<String>> lists = Arrays.asList(
     *     Arrays.asList("a", "b", "c"),
     *     Arrays.asList("x"),
     *     Arrays.asList("m", "n")
     * );
     * lists.sort(Comparators.comparingBySize());
     * // Result: lists sorted by size: ["x"], ["m","n"], ["a","b","c"]
     * }</pre>
     *
     * @param <T> the type of Collection being compared
     * @return a comparator that compares Collections by size
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Comparator<T> comparingBySize() {
        return (Comparator<T>) COMPARING_BY_SIZE;
    }

    /**
     * Returns a comparator that compares {@link Map} objects by their size.
     * Null maps are treated as having size 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Integer>> maps = Arrays.asList(
     *     Map.of("a", 1, "b", 2, "c", 3),
     *     Map.of("x", 1),
     *     Map.of("m", 1, "n", 2)
     * );
     * maps.sort(Comparators.comparingByMapSize());
     * // Result: maps sorted by size (1, 2, 3 entries)
     * }</pre>
     *
     * @param <T> the type of Map being compared
     * @return a comparator that compares Maps by size
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Comparator<T> comparingByMapSize() {
        return (Comparator<T>) COMPARING_BY_MAP_SIZE;
    }

    /**
     * Returns a comparator that compares {@code Object[]} arrays using the specified comparator
     * for element-wise comparison. The arrays are compared lexicographically, with shorter
     * arrays considered less than longer arrays when all compared elements are equal.
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>Empty arrays are considered less than non-empty arrays</li>
     *   <li>Elements are compared in order using the provided comparator</li>
     *   <li>The first non-equal comparison determines the result</li>
     *   <li>If all compared elements are equal, the shorter array is considered less</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Object[]> cmp = Comparators.comparingObjArray(String.CASE_INSENSITIVE_ORDER);
     * Object[] arr1 = {"apple", "banana"};
     * Object[] arr2 = {"APPLE", "CHERRY"};
     * int result = cmp.compare(arr1, arr2); // returns negative (banana < cherry)
     * }</pre>
     *
     * @param cmp the comparator to use for comparing array elements
     * @return a comparator that performs lexicographic comparison of Object arrays
     * @throws IllegalArgumentException if cmp is null
     */
    @SuppressWarnings("rawtypes")
    public static Comparator<Object[]> comparingObjArray(final Comparator<?> cmp) {
        return comparingArray((Comparator) cmp);
    }

    /**
     * Returns a comparator that compares arrays of {@link Comparable} elements using their
     * natural ordering. The arrays are compared lexicographically, with shorter arrays
     * considered less than longer arrays when all compared elements are equal.
     *
     * <p>This method is equivalent to calling {@code comparingArray(Comparator.naturalOrder())}
     * but is type-safe for arrays of Comparable elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr1 = {"apple", "banana"};
     * String[] arr2 = {"apple", "cherry"};
     * Comparator<String[]> cmp = Comparators.comparingArray();
     * int result = cmp.compare(arr1, arr2); // returns negative (banana < cherry)
     * }</pre>
     *
     * @param <T> the type of Comparable elements in the arrays
     * @return a comparator that performs lexicographic comparison using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T[]> comparingArray() {
        return comparingArray(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares arrays of elements using the specified comparator
     * for element-wise comparison. The arrays are compared lexicographically.
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>If both arrays are empty or {@code null}, they are considered equal</li>
     *   <li>An empty/null array is considered less than a non-empty array</li>
     *   <li>Elements are compared in order until a difference is found</li>
     *   <li>If all compared elements are equal, the shorter array is considered less</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Integer[]> cmp = Comparators.comparingArray(Integer::compare);
     * Integer[] arr1 = {1, 2, 3};
     * Integer[] arr2 = {1, 2, 3, 4};
     * int result = cmp.compare(arr1, arr2); // returns negative (arr1 is shorter)
     * }</pre>
     *
     * @param <T> the type of elements in the arrays
     * @param cmp the comparator to use for comparing array elements
     * @return a comparator that performs lexicographic comparison of arrays
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T> Comparator<T[]> comparingArray(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final int lenA = N.len(a);
            final int lenB = N.len(b);
            int result = 0;

            for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
                result = cmp.compare(a[i], b[i]);

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(lenA, lenB);
        };
    }

    /**
     * Returns a comparator that compares {@link Collection} objects containing {@link Comparable}
     * elements using their natural ordering. The collections are compared lexicographically
     * by iterating through their elements in order.
     *
     * <p>This method is particularly useful for comparing Lists, Sets, or other Collections
     * where element order matters. For Sets, the iteration order depends on the Set implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list1 = Arrays.asList(1, 2, 3);
     * List<Integer> list2 = Arrays.asList(1, 2, 4);
     * Comparator<List<Integer>> cmp = Comparators.comparingCollection();
     * int result = cmp.compare(list1, list2); // returns negative (3 < 4)
     * }</pre>
     *
     * @param <C> the type of Collection containing Comparable elements
     * @return a comparator that performs lexicographic comparison using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Collection<? extends Comparable>> Comparator<C> comparingCollection() {
        return comparingCollection(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares {@link Collection} objects using the specified
     * comparator for element-wise comparison. The collections are compared lexicographically
     * by iterating through their elements in order.
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>Empty collections are considered less than non-empty collections</li>
     *   <li>Elements are compared in iteration order until a difference is found</li>
     *   <li>If all compared elements are equal, the smaller collection is considered less</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<List<String>> cmp = Comparators.comparingCollection(String.CASE_INSENSITIVE_ORDER);
     * List<String> list1 = Arrays.asList("apple", "BANANA");
     * List<String> list2 = Arrays.asList("APPLE", "banana", "cherry");
     * int result = cmp.compare(list1, list2); // returns negative (smaller size)
     * }</pre>
     *
     * @param <T> the type of elements in the collections
     * @param <C> the type of Collection
     * @param cmp the comparator to use for comparing collection elements
     * @return a comparator that performs lexicographic comparison of collections
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T, C extends Collection<T>> Comparator<C> comparingCollection(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<T> iterA = a.iterator();
            final Iterator<T> iterB = b.iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     * Returns a comparator that compares {@link Iterable} objects containing {@link Comparable}
     * elements using their natural ordering. The iterables are compared lexicographically
     * by iterating through their elements.
     *
     * <p>This method works with any Iterable implementation, including custom iterables.
     * The comparison continues until one iterable is exhausted or a difference is found.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterable<String> iter1 = Arrays.asList("apple", "banana");
     * Iterable<String> iter2 = Arrays.asList("apple", "cherry");
     * Comparator<Iterable<String>> cmp = Comparators.comparingIterable();
     * int result = cmp.compare(iter1, iter2); // returns negative (banana < cherry)
     * }</pre>
     *
     * @param <C> the type of Iterable containing Comparable elements
     * @return a comparator that performs lexicographic comparison using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Iterable<? extends Comparable>> Comparator<C> comparingIterable() {
        return comparingIterable(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares {@link Iterable} objects using the specified
     * comparator for element-wise comparison. The iterables are compared lexicographically
     * by iterating through their elements until a difference is found or one is exhausted.
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>Empty iterables are considered less than non-empty iterables</li>
     *   <li>Elements are compared in iteration order</li>
     *   <li>If one iterable is exhausted first, it is considered less</li>
     *   <li>If both are exhausted simultaneously with all elements equal, they are equal</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Iterable<Person>> cmp = Comparators.comparingIterable(
     *     Comparator.comparing(Person::getAge)
     * );
     * Iterable<Person> team1 = getTeam1();
     * Iterable<Person> team2 = getTeam2();
     * int result = cmp.compare(team1, team2);
     * }</pre>
     *
     * @param <T> the type of elements in the iterables
     * @param <C> the type of Iterable
     * @param cmp the comparator to use for comparing iterable elements
     * @return a comparator that performs lexicographic comparison of iterables
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T, C extends Iterable<T>> Comparator<C> comparingIterable(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            final Iterator<T> iterA = N.iterate(a);
            final Iterator<T> iterB = N.iterate(b);

            if (N.isEmpty(iterA)) {
                return N.isEmpty(iterB) ? 0 : -1;
            } else if (N.isEmpty(iterB)) {
                return 1;
            }

            int result = 0;

            while (iterA.hasNext() && iterB.hasNext()) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return iterA.hasNext() ? 1 : (iterB.hasNext() ? -1 : 0);
        };
    }

    /**
     * Returns a comparator that compares {@link Iterator} objects containing {@link Comparable}
     * elements using their natural ordering. The iterators are compared lexicographically
     * by consuming elements from both iterators until a difference is found or one is exhausted.
     *
     * <p><strong>Warning:</strong> This comparator consumes elements from the iterators during
     * comparison. The iterators cannot be reused after comparison.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(1, 2, 4).iterator();
     * Comparator<Iterator<Integer>> cmp = Comparators.comparingIterator();
     * int result = cmp.compare(iter1, iter2); // returns negative (3 < 4)
     * // Note: iter1 and iter2 are now exhausted
     * }</pre>
     *
     * @param <C> the type of Iterator containing Comparable elements
     * @return a comparator that performs lexicographic comparison using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Iterator<? extends Comparable>> Comparator<C> comparingIterator() {
        return comparingIterator(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares {@link Iterator} objects using the specified
     * comparator for element-wise comparison. The iterators are compared lexicographically
     * by consuming elements from both iterators.
     *
     * <p><strong>Warning:</strong> This comparator consumes elements from the iterators during
     * comparison. The iterators cannot be reused after comparison. Consider using
     * {@link #comparingIterable(Comparator)} if you need to preserve the original data.</p>
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>Empty iterators are considered less than non-empty iterators</li>
     *   <li>Elements are consumed and compared until a difference is found</li>
     *   <li>If one iterator is exhausted first, it is considered less</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = getDataStream1();
     * Iterator<String> iter2 = getDataStream2();
     * Comparator<Iterator<String>> cmp = Comparators.comparingIterator(String::compareToIgnoreCase);
     * int result = cmp.compare(iter1, iter2);
     * // Both iterators are now partially or fully consumed
     * }</pre>
     *
     * @param <T> the type of elements in the iterators
     * @param <C> the type of Iterator
     * @param cmp the comparator to use for comparing iterator elements
     * @return a comparator that performs lexicographic comparison of iterators
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T, C extends Iterator<T>> Comparator<C> comparingIterator(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            int result = 0;

            while (a.hasNext() && b.hasNext()) {
                result = cmp.compare(a.next(), b.next());

                if (result != 0) {
                    return result;
                }
            }

            return a.hasNext() ? 1 : (b.hasNext() ? -1 : 0);
        };
    }

    /**
     * Returns a comparator that compares {@link Map} objects by their keys using the natural
     * ordering of the keys. The maps are compared by iterating through their key sets in the
     * order returned by the map's key set iterator.
     *
     * <p><strong>Note:</strong> The comparison order depends on the Map implementation. For predictable results,
     * use sorted maps (e.g., TreeMap) or maps with consistent iteration order (e.g., LinkedHashMap).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = new TreeMap<>();
     * map1.put("apple", 1);
     * map1.put("banana", 2);
     * 
     * Map<String, Integer> map2 = new TreeMap<>();
     * map2.put("apple", 1);
     * map2.put("cherry", 3);
     * 
     * Comparator<Map<String, Integer>> cmp = Comparators.comparingMapByKey();
     * int result = cmp.compare(map1, map2); // returns negative (banana < cherry)
     * }</pre>
     *
     * @param <M> the type of Map with Comparable keys
     * @return a comparator that compares maps by their keys using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <M extends Map<? extends Comparable, ?>> Comparator<M> comparingMapByKey() {
        return comparingMapByKey(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares {@link Map} objects by their keys using the specified
     * comparator. The maps are compared by iterating through their key sets in the order
     * returned by the map's key set iterator.
     *
     * <p>The comparison algorithm:</p>
     * <ol>
     *   <li>Empty maps are considered less than non-empty maps</li>
     *   <li>Keys are compared in iteration order using the provided comparator</li>
     *   <li>If all compared keys are equal, the smaller map is considered less</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Map<String, Integer>> cmp = Comparators.comparingMapByKey(
     *     String.CASE_INSENSITIVE_ORDER
     * );
     * Map<String, Integer> map1 = Map.of("apple", 1, "BANANA", 2);
     * Map<String, Integer> map2 = Map.of("APPLE", 1, "banana", 2, "cherry", 3);
     * int result = cmp.compare(map1, map2); // returns negative (smaller size)
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <M> the type of Map
     * @param cmp the comparator to use for comparing map keys
     * @return a comparator that compares maps by their keys
     * @throws IllegalArgumentException if cmp is null
     */
    public static <K, M extends Map<K, ?>> Comparator<M> comparingMapByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<K> iterA = a.keySet().iterator();
            final Iterator<K> iterB = b.keySet().iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     * Returns a comparator that compares {@link Map} objects by their values using the natural
     * ordering of the values. The maps are compared by iterating through their value collections
     * in the order returned by the map's values iterator.
     *
     * <p><strong>Note:</strong> The comparison order depends on the Map implementation and may not be predictable
     * for hash-based maps. This comparator is most useful when the iteration order is meaningful.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores1 = new LinkedHashMap<>();
     * scores1.put("Alice", 85);
     * scores1.put("Bob", 92);
     * 
     * Map<String, Integer> scores2 = new LinkedHashMap<>();
     * scores2.put("Carol", 85);
     * scores2.put("Dave", 88);
     * 
     * Comparator<Map<String, Integer>> cmp = Comparators.comparingMapByValue();
     * int result = cmp.compare(scores1, scores2); // returns positive (92 > 88)
     * }</pre>
     *
     * @param <M> the type of Map with Comparable values
     * @return a comparator that compares maps by their values using natural ordering
     */
    @SuppressWarnings("rawtypes")
    public static <M extends Map<?, ? extends Comparable>> Comparator<M> comparingMapByValue() {
        return comparingMapByValue(NATURAL_ORDER);
    }

    /**
     * Returns a comparator that compares {@link Map} objects by their values using the specified
     * comparator. The maps are compared by iterating through their value collections in the
     * order returned by the map's values iterator.
     *
     * <p>This comparator is useful for comparing maps based on their value content rather than
     * their keys. The iteration order of values depends on the Map implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Map<String, Person>> cmp = Comparators.comparingMapByValue(
     *     Comparator.comparing(Person::getAge).thenComparing(Person::getName)
     * );
     * Map<String, Person> team1 = getTeam1Roster();
     * Map<String, Person> team2 = getTeam2Roster();
     * int result = cmp.compare(team1, team2);
     * }</pre>
     *
     * @param <V> the type of values in the maps
     * @param <M> the type of Map
     * @param cmp the comparator to use for comparing map values
     * @return a comparator that compares maps by their values
     * @throws IllegalArgumentException if cmp is null
     */
    public static <V, M extends Map<?, V>> Comparator<M> comparingMapByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<V> iterA = a.values().iterator();
            final Iterator<V> iterB = b.values().iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     * Returns a comparator that compares Java beans by extracting and comparing the specified
     * properties using reflection. Properties are compared in the order they appear in the
     * collection, using their natural ordering.
     *
     * <p><strong>Performance Warning:</strong> This method uses reflection to access properties,
     * which can significantly impact performance. For better performance, use explicit comparators
     * with getter methods or {@link ComparisonBuilder}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> propsToCompare = Arrays.asList("age", "name", "salary");
     * Comparator<Employee> cmp = Comparators.comparingBeanByProps(propsToCompare);
     * List<Employee> employees = getEmployees();
     * employees.sort(cmp);
     * // Employees sorted by age, then name, then salary
     * }</pre>
     *
     * @param <T> the type of beans to compare
     * @param propNamesToCompare collection of property names to compare in order
     * @return a comparator that compares beans by the specified properties
     * @throws IllegalArgumentException if propNamesToCompare is {@code null} or contains invalid property names
     * @deprecated call {@code getPropValue} by reflection apis during comparing or sorting may have huge impact to performance. Use {@link ComparisonBuilder} instead.
     * @see Builder#compare(Object, Object, Comparator)
     * @see ComparisonBuilder
     */
    @Deprecated
    public static <T> Comparator<T> comparingBeanByProps(final Collection<String> propNamesToCompare) throws IllegalArgumentException {
        N.checkArgNotNull(propNamesToCompare, cs.propNamesToCompare);

        return (a, b) -> Beans.compareByProps(a, b, propNamesToCompare);
    }

    /**
     * Returns a comparator that imposes the reverse of the natural ordering on a collection
     * of {@link Comparable} objects. Unlike {@code Collections.reverseOrder()}, this comparator
     * handles {@code null} values by treating them as greater than {@code non-null} values (nulls last).
     *
     * <p>The returned comparator does NOT throw {@link NullPointerException} when comparing
     * {@code null} values. Instead, {@code null} is considered greater than any {@code non-null} value, and when both
     * values are {@code null}, they are considered equal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(3, 1, null, 4, 1, 5, 9);
     * numbers.sort(Comparators.reverseOrder());
     * // Result: [9, 5, 4, 3, 1, 1, null]
     *
     * List<String> words = Arrays.asList("apple", null, "zebra", "banana");
     * words.sort(Comparators.reverseOrder());
     * // Result: ["zebra", "banana", "apple", null]
     * }</pre>
     *
     * @param <T> the type of Comparable objects to compare
     * @return a comparator that imposes the reverse natural ordering with nulls last
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> reverseOrder() {
        return REVERSED_ORDER;
    }

    /**
     * Returns a comparator that imposes the reverse ordering of the specified comparator.
     * If the specified comparator is {@code null} or the natural order comparator, returns the
     * reverse natural order comparator. If the specified comparator is already the reverse
     * natural order comparator, returns the natural order comparator.
     *
     * <p>This method provides optimization for common cases:</p>
     * <ul>
     *   <li>reverseOrder(null) returns reverse natural order</li>
     *   <li>reverseOrder(naturalOrder) returns reverse natural order</li>
     *   <li>reverseOrder(reverseOrder) returns natural order</li>
     *   <li>reverseOrder(customComparator) returns reversed custom comparator</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<Person> byAge = Comparator.comparing(Person::getAge);
     * Comparator<Person> byAgeReversed = Comparators.reverseOrder(byAge);
     * 
     * List<Person> people = getPeople();
     * people.sort(byAgeReversed); // Sorts from oldest to youngest
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param cmp the comparator to reverse, or {@code null} for natural order
     * @return a comparator that imposes the reverse ordering of cmp
     */
    public static <T> Comparator<T> reverseOrder(final Comparator<T> cmp) {
        if (cmp == null || cmp == NATURAL_ORDER) { // NOSONAR
            return REVERSED_ORDER;
        } else if (cmp == REVERSED_ORDER) { // NOSONAR
            return NATURAL_ORDER;
        }

        return Collections.reverseOrder(cmp);
    }

    /**
     * Returns a comparator that compares objects by extracting a boolean key and comparing
     * in reverse order. In the reversed boolean ordering, {@code true} is considered less
     * than {@code false}.
     *
     * <p>This is useful when you want to sort items with a boolean property where true
     * values should appear first (since normal boolean ordering places {@code false} before true).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Task> tasks = getTasks();
     * // Sort tasks with completed tasks first
     * tasks.sort(Comparators.reversedComparingBoolean(Task::isCompleted));
     * 
     * // Sort products with in-stock items first
     * products.sort(Comparators.reversedComparingBoolean(Product::isInStock));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract boolean keys from objects
     * @return a comparator that compares by extracted boolean values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingBoolean(final ToBooleanFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Boolean.compare(keyExtractor.applyAsBoolean(b), keyExtractor.applyAsBoolean(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a char key and comparing
     * in reverse order. Characters are compared by their numeric values in descending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Student> students = getStudents();
     * // Sort students by grade in reverse order (Z to A)
     * students.sort(Comparators.reversedComparingChar(Student::getGrade));
     * 
     * // Sort items by priority code in reverse order
     * items.sort(Comparators.reversedComparingChar(Item::getPriorityCode));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract char keys from objects
     * @return a comparator that compares by extracted char values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingChar(final ToCharFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Character.compare(keyExtractor.applyAsChar(b), keyExtractor.applyAsChar(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a byte key and comparing
     * in reverse order. Bytes are compared numerically in descending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<DataPacket> packets = getPackets();
     * // Sort packets by priority byte in reverse order (highest priority first)
     * packets.sort(Comparators.reversedComparingByte(DataPacket::getPriority));
     * 
     * // Sort by compression level in reverse order
     * files.sort(Comparators.reversedComparingByte(File::getCompressionLevel));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract byte keys from objects
     * @return a comparator that compares by extracted byte values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingByte(final ToByteFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Byte.compare(keyExtractor.applyAsByte(b), keyExtractor.applyAsByte(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a short key and comparing
     * in reverse order. Shorts are compared numerically in descending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Product> products = getProducts();
     * // Sort products by quantity in reverse order (highest quantity first)
     * products.sort(Comparators.reversedComparingShort(Product::getQuantity));
     * 
     * // Sort by year in reverse order (newest first)
     * records.sort(Comparators.reversedComparingShort(Record::getYear));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract short keys from objects
     * @return a comparator that compares by extracted short values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingShort(final ToShortFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Short.compare(keyExtractor.applyAsShort(b), keyExtractor.applyAsShort(a));
    }

    /**
     * Returns a comparator that compares objects by extracting an int key and comparing
     * in reverse order. Integers are compared numerically in descending order.
     *
     * <p>This is one of the most commonly used reversed comparators, useful for sorting
     * by numeric properties in descending order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Employee> employees = getEmployees();
     * // Sort employees by salary in descending order
     * employees.sort(Comparators.reversedComparingInt(Employee::getSalary));
     * 
     * // Sort posts by view count (most viewed first)
     * posts.sort(Comparators.reversedComparingInt(Post::getViewCount));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract int keys from objects
     * @return a comparator that compares by extracted int values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingInt(final ToIntFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Integer.compare(keyExtractor.applyAsInt(b), keyExtractor.applyAsInt(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a long key and comparing
     * in reverse order. Longs are compared numerically in descending order.
     *
     * <p>Commonly used for sorting by timestamps, IDs, or large numeric values in
     * descending order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Transaction> transactions = getTransactions();
     * // Sort transactions by timestamp (newest first)
     * transactions.sort(Comparators.reversedComparingLong(Transaction::getTimestamp));
     * 
     * // Sort files by size (largest first)
     * files.sort(Comparators.reversedComparingLong(File::getSize));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract long keys from objects
     * @return a comparator that compares by extracted long values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingLong(final ToLongFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Long.compare(keyExtractor.applyAsLong(b), keyExtractor.applyAsLong(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a float key and comparing
     * in reverse order. Floats are compared numerically in descending order, with proper
     * handling of NaN values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Product> products = getProducts();
     * // Sort products by rating (highest rated first)
     * products.sort(Comparators.reversedComparingFloat(Product::getRating));
     * 
     * // Sort by discount percentage (highest discount first)
     * items.sort(Comparators.reversedComparingFloat(Item::getDiscountPercent));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract float keys from objects
     * @return a comparator that compares by extracted float values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingFloat(final ToFloatFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Float.compare(keyExtractor.applyAsFloat(b), keyExtractor.applyAsFloat(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a double key and comparing
     * in reverse order. Doubles are compared numerically in descending order, with proper
     * handling of NaN values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Student> students = getStudents();
     * // Sort students by GPA (highest first)
     * students.sort(Comparators.reversedComparingDouble(Student::getGpa));
     * 
     * // Sort locations by distance (farthest first)
     * locations.sort(Comparators.reversedComparingDouble(Location::getDistance));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract double keys from objects
     * @return a comparator that compares by extracted double values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <T> Comparator<T> reversedComparingDouble(final ToDoubleFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Double.compare(keyExtractor.applyAsDouble(b), keyExtractor.applyAsDouble(a));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key
     * and comparing in reverse order. The extracted keys must implement Comparable and
     * are compared using their natural ordering in reverse.
     *
     * <p>This is a general-purpose method for reversing any Comparable key extraction.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = getPeople();
     * // Sort people by name in reverse alphabetical order (Z to A)
     * people.sort(Comparators.reversedComparingBy(Person::getName));
     * 
     * // Sort events by date in reverse chronological order (newest first)
     * events.sort(Comparators.reversedComparingBy(Event::getDate));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract Comparable keys from objects
     * @return a comparator that compares by extracted Comparable values in reverse order
     * @throws IllegalArgumentException if keyExtractor is null
     * @see #reversedComparingByIfNotNullOrElseNullsFirst(Function)
     * @see #reversedComparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> reversedComparingBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> REVERSED_ORDER.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key
     * and comparing in reverse order, with special {@code null} handling. If either object being
     * compared is {@code null}, it is treated as the minimum value (nulls first). The extracted
     * keys are compared using reverse natural ordering.
     *
     * <p>This method is useful when you need reverse ordering but want to ensure that
     * {@code null} objects appear at the beginning of the sorted collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Employee> employees = getEmployees(); // May contain null entries
     * // Sort by salary in descending order, with null employees first
     * employees.sort(Comparators.reversedComparingByIfNotNullOrElseNullsFirst(
     *     emp -> emp.getSalary()
     * ));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract Comparable keys from objects
     * @return a comparator with reverse ordering and nulls-first behavior
     * @throws IllegalArgumentException if keyExtractor is null
     */
    @Beta
    public static <T> Comparator<T> reversedComparingByIfNotNullOrElseNullsFirst(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_FIRST_REVERSED_ORDER;

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares objects by extracting a {@link Comparable} key
     * and comparing in reverse order, with special {@code null} handling. If either object being
     * compared is {@code null}, it is treated as the maximum value (nulls last). The extracted
     * keys are compared using reverse natural ordering.
     *
     * <p>This method is useful when you need reverse ordering but want to ensure that
     * {@code null} objects appear at the end of the sorted collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Product> products = getProducts(); // May contain null entries
     * // Sort by price in descending order, with null products last
     * products.sort(Comparators.reversedComparingByIfNotNullOrElseNullsLast(
     *     prod -> prod.getPrice()
     * ));
     * }</pre>
     *
     * @param <T> the type of objects to compare
     * @param keyExtractor function to extract Comparable keys from objects
     * @return a comparator with reverse ordering and nulls-last behavior
     * @throws IllegalArgumentException if keyExtractor is null
     */
    @Beta
    public static <T> Comparator<T> reversedComparingByIfNotNullOrElseNullsLast(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_LAST_REVERSED_ORDER;

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Returns a comparator that compares {@link Map.Entry} objects by their keys in
     * reverse natural ordering. The keys must implement {@link Comparable}.
     *
     * <p>This comparator is useful for sorting map entries by key in descending order,
     * such as when processing map entries in reverse alphabetical or reverse numeric order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87, "Carol", 92);
     * List<Map.Entry<String, Integer>> entries = new ArrayList<>(scores.entrySet());
     * entries.sort(Comparators.reversedComparingByKey());
     * // Result: entries sorted by name in reverse order: Carol, Bob, Alice
     * }</pre>
     *
     * @param <K> the key type (must be Comparable)
     * @param <V> the value type
     * @return a comparator that compares entries by key in reverse natural order
     */
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> reversedComparingByKey() {
        return (a, b) -> REVERSED_ORDER.compare(a.getKey(), b.getKey());
    }

    /**
     * Returns a comparator that compares {@link Map.Entry} objects by their values in
     * reverse natural ordering. The values must implement {@link Comparable}.
     *
     * <p>This comparator is useful for sorting map entries by value in descending order,
     * such as creating a ranking from highest to lowest values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87, "Carol", 92);
     * List<Map.Entry<String, Integer>> entries = new ArrayList<>(scores.entrySet());
     * entries.sort(Comparators.reversedComparingByValue());
     * // Result: entries sorted by score descending: Alice(95), Carol(92), Bob(87)
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type (must be Comparable)
     * @return a comparator that compares entries by value in reverse natural order
     */
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> reversedComparingByValue() {
        return (a, b) -> REVERSED_ORDER.compare(a.getValue(), b.getValue());
    }

    /**
     * Returns a comparator that compares {@link Map.Entry} objects by their keys in
     * reverse order using the specified comparator. This allows custom comparison
     * logic for the keys while maintaining reverse ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort entries by key length in reverse order (longest first)
     * Comparator<Map.Entry<String, Integer>> cmp = Comparators.reversedComparingByKey(
     *     Comparator.comparingInt(String::length)
     * );
     * 
     * Map<String, Integer> data = Map.of("a", 1, "abc", 2, "ab", 3);
     * List<Map.Entry<String, Integer>> entries = new ArrayList<>(data.entrySet());
     * entries.sort(cmp);
     * // Result: "abc"=2, "ab"=3, "a"=1
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp the comparator to use for comparing keys (will be reversed)
     * @return a comparator that compares entries by key using reversed cmp
     * @throws IllegalArgumentException if cmp is null
     */
    @Beta
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        final Comparator<? super K> reversedOrder = reverseOrder(cmp);

        return (a, b) -> reversedOrder.compare(a.getKey(), b.getKey());
    }

    /**
     * Returns a comparator that compares {@link Map.Entry} objects by their values in
     * reverse order using the specified comparator. This allows custom comparison
     * logic for the values while maintaining reverse ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort entries by person age in reverse order (oldest first)
     * Comparator<Map.Entry<String, Person>> cmp = Comparators.reversedComparingByValue(
     *     Comparator.comparingInt(Person::getAge)
     * );
     * 
     * Map<String, Person> people = getPeopleMap();
     * List<Map.Entry<String, Person>> entries = new ArrayList<>(people.entrySet());
     * entries.sort(cmp);
     * // Result: entries sorted by age descending
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp the comparator to use for comparing values (will be reversed)
     * @return a comparator that compares entries by value using reversed cmp
     * @throws IllegalArgumentException if cmp is null
     */
    @Beta
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        final Comparator<? super V> reversedOrder = reverseOrder(cmp);

        return (a, b) -> reversedOrder.compare(a.getValue(), b.getValue());
    }
}
