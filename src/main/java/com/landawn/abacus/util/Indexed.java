/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

/**
 * An immutable container that pairs a value of type {@code T} with a long index position,
 * providing a type-safe way to associate data with positional information. This class is
 * particularly useful in scenarios where tracking both a value and its position is essential,
 * such as during stream operations, collection processing, and data transformation pipelines.
 *
 * <p>The {@code Indexed} class extends {@link AbstractIndexed}, which provides the core index
 * storage and access functionality. This design allows the class to focus on value management
 * while inheriting index-related operations from its parent class.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Generic type parameter {@code T} ensures compile-time type checking for values</li>
 *   <li><b>Immutable Design:</b> Both index and value are final and set only during construction</li>
 *   <li><b>Long Index Support:</b> Uses long for index to support very large collections beyond Integer.MAX_VALUE</li>
 *   <li><b>Null Value Support:</b> Values can be null, allowing representation of absent or optional data</li>
 *   <li><b>Equality Semantics:</b> Two Indexed instances are equal if they have the same index and equal values</li>
 *   <li><b>Hash Code Contract:</b> Hash code is computed from both index and value for proper collection usage</li>
 *   <li><b>String Representation:</b> Clear format {@code [index]=value} for debugging and logging</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Immutability:</b> Once created, an Indexed instance cannot be modified, ensuring thread safety</li>
 *   <li><b>Simplicity:</b> Focused on a single purpose - pairing values with indices</li>
 *   <li><b>Type Safety:</b> Leverages generics to prevent type-related runtime errors</li>
 *   <li><b>Integration:</b> Works seamlessly with Java's collections framework and functional APIs</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Stream Processing:</b> Maintaining element positions during stream transformations</li>
 *   <li><b>Parallel Processing:</b> Tracking original indices when processing collections in parallel</li>
 *   <li><b>Sorting with Index:</b> Preserving original positions when sorting or filtering data</li>
 *   <li><b>Pagination:</b> Associating values with their position in paginated result sets</li>
 *   <li><b>Data Mapping:</b> Creating index-to-value mappings without using heavyweight Map structures</li>
 *   <li><b>Result Tracking:</b> Recording the position where specific values or conditions occurred</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic creation and access
 * Indexed<String> indexed = Indexed.of("Hello", 5);
 * String value = indexed.value();  // "Hello"
 * long index = indexed.index();  // 5
 *
 * // Stream processing with indices
 * List<String> items = Arrays.asList("a", "b", "c");
 * List<Indexed<String>> indexedItems = IntStream.range(0, items.size())
 *     .mapToObj(i -> Indexed.of(items.get(i), i))
 *     .collect(Collectors.toList());
 *
 * // Filtering while preserving indices
 * List<Indexed<String>> filtered = indexedItems.stream()
 *     .filter(idx -> idx.value().length() > 1)
 *     .collect(Collectors.toList());
 * // Each element knows its original position
 *
 * // Sorting with original index tracking
 * List<Indexed<Integer>> numbers = Arrays.asList(
 *     Indexed.of(30, 0),
 *     Indexed.of(10, 1),
 *     Indexed.of(20, 2)
 * );
 * numbers.sort(Comparator.comparing(Indexed::value));
 * // Can still access original positions via index()
 *
 * // Working with large indices
 * Indexed<String> largeIndex = Indexed.of("data", 5_000_000_000L);
 * // Supports indices beyond Integer.MAX_VALUE
 * }</pre>
 *
 * <p><b>Primitive Specializations:</b>
 * <p>For primitive types, specialized versions are available to avoid boxing overhead:
 * <ul>
 *   <li>{@link IndexedBoolean} - for boolean values</li>
 *   <li>{@link IndexedByte} - for byte values</li>
 *   <li>{@link IndexedChar} - for char values</li>
 *   <li>{@link IndexedShort} - for short values</li>
 *   <li>{@link IndexedInt} - for int values</li>
 *   <li>{@link IndexedLong} - for long values</li>
 *   <li>{@link IndexedFloat} - for float values</li>
 *   <li>{@link IndexedDouble} - for double values</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) - Simple object allocation with two field assignments</li>
 *   <li><b>Memory Overhead:</b> Minimal - One long (8 bytes) + one object reference + object header</li>
 *   <li><b>Access Cost:</b> O(1) - Direct field access for both index and value</li>
 *   <li><b>Equality Check:</b> O(1) - Compares index (primitive) and value (using equals)</li>
 *   <li><b>Hash Code:</b> O(1) - Simple computation based on index and value hash</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable State:</b> Both index and value are final and cannot be changed after construction</li>
 *   <li><b>Safe Publication:</b> Can be safely shared between threads without synchronization</li>
 *   <li><b>No Synchronization:</b> No locks or synchronization needed due to immutability</li>
 *   <li><b>Concurrent Access:</b> Multiple threads can safely read from the same instance</li>
 *   <li><b>Value Mutability:</b> If the contained value is mutable, proper synchronization is required for the value itself</li>
 * </ul>
 *
 * <p><b>Equality and Hash Code:</b>
 * <ul>
 *   <li>Two Indexed instances are equal if they have the same index and equal values</li>
 *   <li>Null values are supported and two null values are considered equal</li>
 *   <li>Hash code is computed as: {@code index * 31 + (value == null ? 0 : value.hashCode())}</li>
 *   <li>The implementation satisfies the hash code contract for use in collections</li>
 * </ul>
 *
 * <p><b>String Representation:</b>
 * <p>The {@link #toString()} method returns a string in the format {@code [index]=value},
 * making it easy to identify both the position and content at a glance. For example:
 * <pre>{@code
 * Indexed.of("hello", 5).toString()  // returns "[5]=hello"
 * Indexed.of(null, 0).toString()     // returns "[0]=null"
 * }</pre>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use factory methods {@link #of(Object, int)} or {@link #of(Object, long)} instead of constructor</li>
 *   <li>Choose int overload when indices are guaranteed to be within int range for clarity</li>
 *   <li>Use long overload when working with very large collections or unbounded sequences</li>
 *   <li>Consider primitive specializations (IndexedInt, IndexedLong, etc.) for performance-critical code</li>
 *   <li>Remember that while Indexed itself is immutable, contained values may be mutable</li>
 *   <li>Use in streams with {@code mapToObj} or similar operations to maintain index information</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs. Map.Entry:</b> Indexed uses long index vs Map.Entry's key-value semantics</li>
 *   <li><b>vs. Pair:</b> Indexed is specialized for index-value pairs with specific semantics</li>
 *   <li><b>vs. Tuple2:</b> Indexed provides semantic meaning (index + value) vs generic tuple</li>
 *   <li><b>vs. Array/List:</b> Indexed is immutable and can exist independently of collections</li>
 * </ul>
 *
 * <p><b>Integration Examples:</b>
 * <pre>{@code
 * // Collecting indexed values into a map
 * Map<Long, String> indexToValue = Stream.of(
 *     Indexed.of("a", 0L),
 *     Indexed.of("b", 1L),
 *     Indexed.of("c", 2L)
 * ).collect(Collectors.toMap(Indexed::index, Indexed::value));
 *
 * // Finding first occurrence matching a predicate
 * Optional<Indexed<String>> firstMatch = indexedList.stream()
 *     .filter(idx -> idx.value().startsWith("test"))
 *     .findFirst();
 * // firstMatch contains both the value and its original position
 *
 * // Grouping by value while preserving indices
 * Map<String, List<Long>> valueToIndices = indexedList.stream()
 *     .collect(Collectors.groupingBy(
 *         Indexed::value,
 *         Collectors.mapping(Indexed::index, Collectors.toList())
 *     ));
 * }</pre>
 *
 * @param <T> the type of the value being indexed, can be any type including null
 * @see AbstractIndexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class Indexed<T> extends AbstractIndexed {

    private final T value;

    /**
     * Constructs an Indexed instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(Object, int)} or
     * {@link #of(Object, long)} factory methods for creating instances.
     *
     * <p>The constructor initializes the index through the superclass {@link AbstractIndexed}
     * and stores the value in this class. Both the index and value become immutable after
     * construction.</p>
     *
     * @param index the index position (non-negative long value)
     * @param value the value to be associated with the index (can be null)
     */
    Indexed(final long index, final T value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new Indexed instance with the specified value and index.
     *
     * <p>This is a static factory method that provides a convenient and type-safe way to create
     * an Indexed instance. The method allows for type inference, making the code more concise.
     * The value can be {@code null}, allowing representation of optional or absent data at a
     * specific position. The returned instance is immutable.</p>
     *
     * <p>This overload accepts an int index, which is suitable for most use cases where indices
     * are within the range of Integer.MAX_VALUE. For larger indices, use {@link #of(Object, long)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic usage
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     *
     * // With null value
     * Indexed<String> nullValue = Indexed.of(null, 0);
     *
     * // Using in stream operations
     * List<String> items = Arrays.asList("a", "b", "c");
     * List<Indexed<String>> withIndices = IntStream.range(0, items.size())
     *     .mapToObj(i -> Indexed.of(items.get(i), i))
     *     .collect(Collectors.toList());
     *
     * // With complex types
     * Indexed<List<Integer>> listIndexed = Indexed.of(Arrays.asList(1, 2, 3), 10);
     * }</pre>
     *
     * @param <T> the type of the value to be indexed
     * @param value the value to be associated with the index (may be {@code null})
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable Indexed instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static <T> Indexed<T> of(final T value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Creates a new Indexed instance with the specified value and index.
     *
     * <p>This is a static factory method that provides a convenient and type-safe way to create
     * an Indexed instance with a long index. This overload accepts a long index for cases where
     * the index might exceed Integer.MAX_VALUE, such as when working with very large datasets,
     * unbounded streams, or distributed systems where indices can grow beyond the int range.</p>
     *
     * <p>The method allows for type inference, making the code more concise. The value can be
     * {@code null}, allowing representation of optional or absent data at a specific position.
     * The returned instance is immutable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic usage with long index
     * Indexed<String> indexed = Indexed.of("Hello", 5_000_000_000L);
     *
     * // Working with large datasets
     * Indexed<byte[]> dataChunk = Indexed.of(dataBytes, 9_999_999_999L);
     *
     * // Using with LongStream
     * List<Indexed<Long>> squares = LongStream.range(0, 10)
     *     .mapToObj(i -> Indexed.of(i * i, i))
     *     .collect(Collectors.toList());
     *
     * // Combining with int values (automatic widening)
     * int position = 100;
     * Indexed<String> item = Indexed.of("item", position);  // int automatically converts to long
     *
     * // Representing positions in distributed systems
     * long globalOffset = 10_000_000_000L;
     * Indexed<Record> record = Indexed.of(recordData, globalOffset);
     * }</pre>
     *
     * @param <T> the type of the value to be indexed
     * @param value the value to be associated with the index (may be {@code null})
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable Indexed instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static <T> Indexed<T> of(final T value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Returns the value stored in this Indexed instance.
     *
     * <p>This method provides direct access to the value component of the index-value pair.
     * The returned value may be {@code null} if the Indexed instance was created with a null value.
     * This is useful for extracting the actual data while the index can be accessed separately
     * through the {@link #index()} method inherited from {@link AbstractIndexed}.</p>
     *
     * <p>Since Indexed is immutable, this method always returns the same value that was provided
     * during construction. However, if the value itself is a mutable object (such as a List or array),
     * modifications to that object will be reflected in subsequent calls to this method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic value retrieval
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     * String value = indexed.value();   // "Hello"
     * long index = indexed.index();     // 5
     *
     * // Handling null values
     * Indexed<String> nullIndexed = Indexed.of(null, 0);
     * String nullValue = nullIndexed.value();  // null
     *
     * // Using in stream operations
     * List<Indexed<String>> indexedItems = Arrays.asList(
     *     Indexed.of("apple", 0),
     *     Indexed.of("banana", 1),
     *     Indexed.of("cherry", 2)
     * );
     * List<String> values = indexedItems.stream()
     *     .map(Indexed::value)
     *     .collect(Collectors.toList());
     * // values: ["apple", "banana", "cherry"]
     *
     * // Filtering based on value
     * List<Indexed<Integer>> numbers = Arrays.asList(
     *     Indexed.of(10, 0),
     *     Indexed.of(25, 1),
     *     Indexed.of(30, 2)
     * );
     * List<Integer> indicesOfLargeNumbers = numbers.stream()
     *     .filter(idx -> idx.value() > 20)
     *     .map(Indexed::index)
     *     .collect(Collectors.toList());
     * // indicesOfLargeNumbers: [1, 2]
     * }</pre>
     *
     * @return the value associated with this index, may be {@code null}
     */
    public T value() {
        return value;
    }

    /**
     * Returns the hash code of this Indexed instance.
     *
     * <p>The hash code is computed based on both the index and the value using the formula:
     * {@code (int) (index * 31 + (value == null ? 0 : value.hashCode()))}. This ensures
     * that Indexed instances with the same index and value will have the same hash code,
     * making them suitable for use in hash-based collections like HashMap and HashSet.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> idx1 = Indexed.of("Hello", 5);
     * Indexed<String> idx2 = Indexed.of("Hello", 5);
     * Indexed<String> idx3 = Indexed.of("World", 5);
     *
     * idx1.hashCode() == idx2.hashCode();   // true (same index and value)
     * idx1.hashCode() == idx3.hashCode();   // likely false (different values)
     *
     * // Using in HashSet
     * Set<Indexed<String>> set = new HashSet<>();
     * set.add(Indexed.of("A", 0));
     * set.add(Indexed.of("A", 0));   // Duplicate, won't be added
     * set.size();                    // 1
     * }</pre>
     *
     * @return the hash code value for this Indexed instance
     */
    @Override
    public int hashCode() {
        return (int) (index * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     * Compares this Indexed instance to the specified object for equality.
     * Returns {@code true} if and only if the specified object is also an Indexed instance,
     * and both instances have the same index and equal values.
     *
     * <p>Two Indexed instances are equal if they have the same index (primitive long comparison)
     * and equal values. Values are compared using {@link N#equals(Object, Object)}, which properly
     * handles {@code null} values - two {@code null} values are considered equal to each other.</p>
     *
     * <p>This implementation follows the general contract of {@link Object#equals(Object)}:
     * <ul>
     *   <li>Reflexive: {@code x.equals(x)} returns {@code true}</li>
     *   <li>Symmetric: {@code x.equals(y)} returns {@code true} if and only if {@code y.equals(x)} returns {@code true}</li>
     *   <li>Transitive: If {@code x.equals(y)} and {@code y.equals(z)}, then {@code x.equals(z)}</li>
     *   <li>Consistent: Multiple invocations return the same result (assuming no modification)</li>
     *   <li>Null comparison: {@code x.equals(null)} returns {@code false}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic equality
     * Indexed<String> idx1 = Indexed.of("Hello", 5);
     * Indexed<String> idx2 = Indexed.of("Hello", 5);
     * Indexed<String> idx3 = Indexed.of("World", 5);
     * Indexed<String> idx4 = Indexed.of("Hello", 6);
     *
     * idx1.equals(idx2);   // true (same index and value)
     * idx1.equals(idx3);   // false (different values)
     * idx1.equals(idx4);   // false (different indices)
     *
     * // Null value handling
     * Indexed<String> nullIdx1 = Indexed.of(null, 0);
     * Indexed<String> nullIdx2 = Indexed.of(null, 0);
     * Indexed<String> nullIdx3 = Indexed.of(null, 1);
     *
     * nullIdx1.equals(nullIdx2);   // true (both have null values and same index)
     * nullIdx1.equals(nullIdx3);   // false (different indices)
     *
     * // Different types
     * Indexed<String> strIdx = Indexed.of("Hello", 5);
     * String str = "Hello";
     * strIdx.equals(str);  // false (different types)
     *
     * // Using in collections
     * List<Indexed<String>> list = new ArrayList<>();
     * Indexed<String> item = Indexed.of("test", 0);
     * list.add(item);
     * list.contains(Indexed.of("test", 0));   // true
     * list.contains(Indexed.of("test", 1));   // false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this Indexed instance
     * @return {@code true} if the specified object is equal to this Indexed instance, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Indexed<?> other) {

            return index == other.index && N.equals(value, other.value);
        }

        return false;
    }

    /**
     * Returns a string representation of this Indexed instance.
     *
     * <p>The string representation is formatted as {@code [index]=value}, where the index
     * is enclosed in square brackets followed by an equals sign and the string representation
     * of the value. This format clearly shows both the position and the associated data,
     * making it useful for debugging and logging purposes.</p>
     *
     * <p>The value is converted to a string using {@link N#toString(Object)}, which properly
     * handles {@code null} values by converting them to the string {@code "null"}. Complex
     * objects will use their {@code toString()} implementation.</p>
     *
     * <p><b>Format:</b> {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic string representation
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     * System.out.println(indexed);  // prints: [5]=Hello
     *
     * // With null value
     * Indexed<String> nullIndexed = Indexed.of(null, 0);
     * System.out.println(nullIndexed);  // prints: [0]=null
     *
     * // With different types
     * Indexed<Integer> intIndexed = Indexed.of(42, 10);
     * System.out.println(intIndexed);  // prints: [10]=42
     *
     * Indexed<List<String>> listIndexed = Indexed.of(Arrays.asList("a", "b"), 3);
     * System.out.println(listIndexed);  // prints: [3]=[a, b]
     *
     * // Using in logging
     * List<Indexed<String>> items = Arrays.asList(
     *     Indexed.of("apple", 0),
     *     Indexed.of("banana", 1),
     *     Indexed.of("cherry", 2)
     * );
     * items.forEach(System.out::println);
     * // Output:
     * // [0]=apple
     * // [1]=banana
     * // [2]=cherry
     *
     * // Large index values
     * Indexed<String> largeIdx = Indexed.of("data", 5_000_000_000L);
     * System.out.println(largeIdx);  // prints: [5000000000]=data
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
