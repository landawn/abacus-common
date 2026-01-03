/*
 * Copyright (C) 2024 HaiYang Li
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
 * An immutable container that combines an index, a key, and a value. This class extends
 * {@link Keyed} to add index information while maintaining the performance benefits of
 * key-based hashing and comparison.
 * 
 * <p>It's designed for performance improvement by only hash/compare {@code key} and {@code index}
 * in {@code hashCode/equals} method, ignoring the value. This makes it ideal for use in
 * collections where you need to track both the position and a unique identifier for elements.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedKeyed<String, User> indexed = IndexedKeyed.of(0, "user123", new User("John"));
 * System.out.println(indexed.index());   // prints: 0
 * System.out.println(indexed.key());   // prints: "user123"
 * System.out.println(indexed.val());   // prints: User object
 * }</pre>
 *
 * @param <K> the type of the key
 * @param <T> the type of the value
 * @see Keyed
 * @see Wrapper
 */
@com.landawn.abacus.annotation.Immutable
public final class IndexedKeyed<K, T> extends Keyed<K, T> {

    private final int index;

    IndexedKeyed(final int index, final K key, final T val) {
        super(key, val);
        this.index = index;
    }

    /**
     * Creates a new {@code IndexedKeyed} instance with the specified index, key, and value.
     *
     * <p>This factory method provides a convenient way to create instances without directly
     * calling the constructor. The resulting object is immutable and encapsulates three pieces
     * of information: an index (position), a key (identifier), and a value (content).</p>
     *
     * <p>The key and index are used for hashing and equality comparisons, while the value is
     * stored but not considered in identity operations. This design is optimized for performance
     * in collections where elements are uniquely identified by their index and key combination.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an indexed keyed element for a user at position 5
     * IndexedKeyed<String, User> item = IndexedKeyed.of(5, "user-123", new User("John"));
     *
     * // Can also use null values
     * IndexedKeyed<String, String> nullValue = IndexedKeyed.of(0, "key", null);
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <T> the type of the value.
     * @param index the index position of this element (can be any integer including negative).
     * @param key the key used for hashing and equality comparisons (can be null).
     * @param val the value associated with the key (can be null).
     * @return a new immutable {@code IndexedKeyed} instance containing the provided index, key, and value.
     */
    public static <K, T> IndexedKeyed<K, T> of(final int index, final K key, final T val) {
        return new IndexedKeyed<>(index, key, val);
    }

    /**
     * Returns the index position associated with this object. The index represents
     * the sequential position or order of this element in a collection or sequence.
     *
     * <p>This index value is included in hash code computation and equality checks
     * alongside the key, making it part of the object's identity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedKeyed<String, String> item = IndexedKeyed.of(3, "key", "value");
     * int pos = item.index();   // returns 3
     * }</pre>
     *
     * @return the index value, which can be any integer including negative values.
     */
    public int index() {
        return index;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed using both
     * the index and the key, while intentionally ignoring the value. This design ensures
     * consistent hashing behavior even if the value is mutable or changes over time.
     *
     * <p>The hash code calculation uses the formula: {@code N.hashCode(index) * 31 + N.hashCode(key)},
     * where {@code N.hashCode()} is a null-safe hash code computation utility that handles both
     * primitive values and object references.</p>
     *
     * <p>This implementation guarantees that if two {@code IndexedKeyed} objects are equal
     * according to {@link #equals(Object)}, they will have the same hash code, fulfilling
     * the general contract of {@code hashCode}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedKeyed<String, String> item1 = IndexedKeyed.of(1, "key", "value1");
     * IndexedKeyed<String, String> item2 = IndexedKeyed.of(1, "key", "value2");
     * // Same hash code despite different values
     * assert item1.hashCode() == item2.hashCode();
     * }</pre>
     *
     * @return a hash code value for this object based on index and key only.
     */
    @Override
    public int hashCode() {
        return N.hashCode(index) * 31 + N.hashCode(key);
    }

    /**
     * Indicates whether some other object is "equal to" this one. Two {@code IndexedKeyed}
     * objects are considered equal if and only if they have the same index and the same key.
     * The value is intentionally excluded from the equality comparison.
     *
     * <p>This design allows for efficient lookups in collections where the combination of
     * index and key uniquely identifies an element. It also provides performance benefits
     * by avoiding potentially expensive value comparisons and ensures stable equality even
     * when values are mutable.</p>
     *
     * <p>The equality check follows this logic:</p>
     * <ol>
     *   <li>If comparing with itself (same reference), returns {@code true}</li>
     *   <li>If the other object is not an {@code IndexedKeyed} instance, returns {@code false}</li>
     *   <li>Otherwise, compares both index and key using null-safe equality</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedKeyed<String, String> item1 = IndexedKeyed.of(1, "key", "value1");
     * IndexedKeyed<String, String> item2 = IndexedKeyed.of(1, "key", "value2");
     * IndexedKeyed<String, String> item3 = IndexedKeyed.of(1, "other", "value1");
     *
     * item1.equals(item2);   // true - same index and key, different value
     * item1.equals(item3);   // false - same index but different key
     * }</pre>
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object has the same index and key as the obj argument;
     *         {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof IndexedKeyed another) {
            return N.equals(another.index, index) && N.equals(another.key, key);
        }

        return false;
    }

    /**
     * Returns a string representation of this {@code IndexedKeyed} object. The string
     * representation includes all three components (index, key, and value) in a readable
     * format, regardless of whether they are used in equality comparisons.
     *
     * <p>The format follows the pattern: {@code "{index=<index>, key=<key>, val=<value>}"}
     * where each component is converted to its string representation. Null values are
     * represented as the string "null".</p>
     *
     * <p>This method is useful for debugging, logging, and displaying the complete state
     * of an {@code IndexedKeyed} instance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedKeyed<String, Integer> item1 = IndexedKeyed.of(2, "abc", 42);
     * System.out.println(item1);   // Output: {index=2, key=abc, val=42}
     *
     * IndexedKeyed<String, String> item2 = IndexedKeyed.of(0, null, "test");
     * System.out.println(item2);   // Output: {index=0, key=null, val=test}
     * }</pre>
     *
     * @return a string representation of this object showing index, key, and value.
     */
    @Override
    public String toString() {
        return "{index=" + index + ", key=" + key + ", val=" + val + "}";
    }
}
