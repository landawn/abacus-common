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
 * <p>Example usage:</p>
 * <pre>{@code
 * IndexedKeyed<String, User> indexed = IndexedKeyed.of(0, "user123", new User("John"));
 * System.out.println(indexed.index()); // prints: 0
 * System.out.println(indexed.key());   // prints: "user123"
 * System.out.println(indexed.val());   // prints: User object
 * }</pre>
 *
 * @param <K> the type of the key
 * @param <T> the type of the value
 * @see Keyed
 * @see Wrapper
 * @author HaiYang Li
 * @since 1.0
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
     * calling the constructor. The resulting object is immutable.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IndexedKeyed<String, Integer> item = IndexedKeyed.of(5, "itemKey", 100);
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <T> the type of the value
     * @param index the index position of this element
     * @param key the key used for hashing and equality comparisons
     * @param val the value associated with the key
     * @return a new {@code IndexedKeyed} instance
     */
    public static <K, T> IndexedKeyed<K, T> of(final int index, final K key, final T val) {
        return new IndexedKeyed<>(index, key, val);
    }

    /**
     * Returns the index associated with this object.
     * 
     * @return the index value
     */
    public int index() {
        return index;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed
     * using both the index and the key, while ignoring the value. This ensures
     * consistent hashing behavior even if the value is mutable.
     * 
     * <p>The hash code is calculated as: {@code N.hashCode(index) * 31 + N.hashCode(key)}</p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return N.hashCode(index) * 31 + N.hashCode(key);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code IndexedKeyed} objects are considered equal if they have
     * the same index and the same key. The value is not considered in the
     * equality comparison.
     * 
     * <p>This design allows for efficient lookups in collections where the
     * combination of index and key uniquely identifies an element.</p>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && IndexedKeyed.class.equals(obj.getClass())) {
            @SuppressWarnings("rawtypes")
            final IndexedKeyed another = (IndexedKeyed) obj;

            return N.equals(another.index, index) && N.equals(another.key, key);
        }

        return false;
    }

    /**
     * Returns a string representation of this {@code IndexedKeyed}.
     * The string representation includes the index, key, and value in a
     * readable format.
     * 
     * <p>For example, an {@code IndexedKeyed} with index 2, key "abc", and value 42
     * would return the string {@code "{index=2, key=abc, val=42}"}.</p>
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{index=" + N.toString(index) + ", key=" + key + ", val=" + val + "}";
    }
}