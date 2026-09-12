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
 * An immutable pairing of a value with a {@code long} index position - the value's place in a sequence, carried
 * alongside the value itself. Useful wherever a transformation must survive without losing where each element came
 * from: filtering, sorting, or parallel processing that has to report original positions.
 *
 * <p>The index is stored as a {@code long} by {@link AbstractIndexed}, so it spans sequences longer than
 * {@link Integer#MAX_VALUE}; read it with {@link #index()} for an {@code int} (which throws
 * {@link ArithmeticException} on overflow) or {@link #longIndex()} for the full range. The factory methods reject a
 * negative index. The value may be {@code null}.
 *
 * <p>Two instances are equal when their indices are equal and their values are equal, {@code null} included;
 * {@link #hashCode()} mixes both halves of the index so a large index is not truncated away. {@link #toString()}
 * renders as {@code [index]=value}.
 *
 * <p>Only the pairing is immutable. A mutable value is not copied or frozen, so the usual caution applies: mutating
 * it after the {@code Indexed} is in a hash-based collection corrupts that collection.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Indexed<String> indexed = Indexed.of("Hello", 5);
 * indexed.value();       // "Hello"
 * indexed.index();       // 5
 * indexed.toString();    // "[5]=Hello"
 *
 * // Keep the original position through a filter
 * List<String> items = Arrays.asList("a", "bb", "c");
 * List<Indexed<String>> longOnes = IntStream.range(0, items.size())
 *         .mapToObj(i -> Indexed.of(items.get(i), i))
 *         .filter(idx -> idx.value().length() > 1)
 *         .collect(Collectors.toList());
 * // [ [1]=bb ] - the element still knows it was second
 *
 * // Sort by value, recover the original order afterwards
 * List<Indexed<Integer>> numbers = new ArrayList<>(List.of(
 *         Indexed.of(30, 0), Indexed.of(10, 1), Indexed.of(20, 2)));
 * numbers.sort(Comparator.comparing(Indexed::value));
 * // [ [1]=10, [2]=20, [0]=30 ]
 *
 * // Indices beyond int range
 * Indexed.of("data", 5_000_000_000L);
 * }</pre>
 *
 * <p>For primitive values, the specializations below avoid boxing: {@link IndexedBoolean}, {@link IndexedByte},
 * {@link IndexedChar}, {@link IndexedShort}, {@link IndexedInt}, {@link IndexedLong}, {@link IndexedFloat},
 * {@link IndexedDouble}.
 *
 * @param <T> the type of the value being indexed; the value itself may be {@code null}.
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
     * Constructs an {@code Indexed} instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(Object, int)} or
     * {@link #of(Object, long)} factory methods for creating instances, as those
     * validate that the index is non-negative.
     *
     * <p>The constructor delegates index storage to the superclass {@link AbstractIndexed}
     * and stores the value in this class. Both fields are {@code final} and cannot be changed
     * after construction.</p>
     *
     * @param index the index position; the factory methods enforce that this is non-negative.
     * @param value the value to be associated with the index; may be {@code null}.
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
     * @param <T> the type of the value to be indexed.
     * @param value the value to be associated with the index (may be {@code null}).
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE).
     * @return a new immutable Indexed instance containing the specified value and index.
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
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
     * // Passing a long literal to select this overload explicitly
     * long position = 100L;
     * Indexed<String> item = Indexed.of("item", position);   // resolves to of(T, long)
     *
     * // Representing positions in distributed systems
     * long globalOffset = 10_000_000_000L;
     * Indexed<Record> record = Indexed.of(recordData, globalOffset);
     * }</pre>
     *
     * @param <T> the type of the value to be indexed.
     * @param value the value to be associated with the index (may be {@code null}).
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE).
     * @return a new immutable Indexed instance containing the specified value and index.
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static <T> Indexed<T> of(final T value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Returns the value stored in this Indexed instance.
     *
     * <p>This method provides direct access to the value component of the index-value pair.
     * The returned value may be {@code null} if the Indexed instance was created with a {@code null} value.
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
     * String value = indexed.value();   // returns "Hello"
     * int index = indexed.index();      // returns 5
     *
     * // Handling null values
     * Indexed<String> nullIndexed = Indexed.of(null, 0);
     * String nullValue = nullIndexed.value();   // returns null
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
     * @return the value associated with this index, may be {@code null}.
     */
    public T value() {
        return value;
    }

    /**
     * Returns the hash code of this Indexed instance.
     *
     * <p>The hash code is computed from all bits of both the index and the value using the formula:
     * {@code 31 * hashLong(index) + (value == null ? 0 : value.hashCode())}. Equal instances
     * therefore have equal hash codes and the upper half of a {@code long} index is not discarded.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> idx1 = Indexed.of("Hello", 5);
     * Indexed<String> idx2 = Indexed.of("Hello", 5);
     * Indexed<String> idx3 = Indexed.of("World", 5);
     *
     * boolean sameHash = idx1.hashCode() == idx2.hashCode();        // true (same index and value)
     * boolean differentHash = idx1.hashCode() != idx3.hashCode();   // true for these values
     *
     * // Using in HashSet
     * Set<Indexed<String>> set = new HashSet<>();
     * set.add(Indexed.of("A", 0));
     * set.add(Indexed.of("A", 0));
     * int size = set.size();         // 1
     * }</pre>
     *
     * @return the hash code value for this Indexed instance.
     */
    @Override
    public int hashCode() {
        return 31 * hashLong(index) + N.hashCode(value);
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
     * idx1.equals(idx2);   // returns true (same index and value)
     * idx1.equals(idx3);   // returns false (different values)
     * idx1.equals(idx4);   // returns false (different indices)
     *
     * // Null value handling
     * Indexed<String> nullIdx1 = Indexed.of(null, 0);
     * Indexed<String> nullIdx2 = Indexed.of(null, 0);
     * Indexed<String> nullIdx3 = Indexed.of(null, 1);
     *
     * nullIdx1.equals(nullIdx2);   // returns true (both have null values and same index)
     * nullIdx1.equals(nullIdx3);   // returns false (different indices)
     *
     * // Different types
     * Indexed<String> strIdx = Indexed.of("Hello", 5);
     * String str = "Hello";
     * strIdx.equals(str);   // returns false (different types)
     *
     * // Using in collections
     * List<Indexed<String>> list = new ArrayList<>();
     * Indexed<String> item = Indexed.of("test", 0);
     * list.add(item);
     * list.contains(Indexed.of("test", 0));   // returns true
     * list.contains(Indexed.of("test", 1));   // returns false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this Indexed instance.
     * @return {@code true} if the specified object is equal to this Indexed instance, {@code false} otherwise.
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
     * System.out.println(indexed);   // prints [5]=Hello
     *
     * // With null value
     * Indexed<String> nullIndexed = Indexed.of(null, 0);
     * System.out.println(nullIndexed);   // prints [0]=null
     *
     * // With different types
     * Indexed<Integer> intIndexed = Indexed.of(42, 10);
     * System.out.println(intIndexed);   // prints [10]=42
     *
     * Indexed<List<String>> listIndexed = Indexed.of(Arrays.asList("a", "b"), 3);
     * System.out.println(listIndexed);   // prints [3]=[a, b]
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
     * System.out.println(largeIdx);   // prints [5000000000]=data
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}.
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
