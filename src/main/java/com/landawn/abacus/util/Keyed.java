/*
 * Copyright (C) 2017 HaiYang Li
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
 * An immutable container that pairs a key with a value, where equality and hashing
 * are based solely on the key. This design provides significant performance improvements
 * in collections by avoiding expensive value comparisons.
 * 
 * <p>It's designed for performance improvement by only hash/compare {@code key} in 
 * {@code hashCode/equals} method. This makes it ideal for scenarios where you need
 * to store key-value pairs in sets or as map keys, but only want to consider the
 * key for uniqueness.</p>
 * 
 * <p>This class is sealed and only permits {@link IndexedKeyed} as a subclass.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Keyed<String, User> userKeyed = Keyed.of("userId123", new User("John", "Doe"));
 * Set<Keyed<String, User>> userSet = new HashSet<>();
 * userSet.add(userKeyed);
 * 
 * // Only the key is used for equality
 * Keyed<String, User> sameKey = Keyed.of("userId123", new User("Jane", "Smith"));
 * System.out.println(userKeyed.equals(sameKey)); // true
 * }</pre>
 *
 * @param <K> the type of the key
 * @param <T> the type of the value
 * @see IndexedKeyed
 * @see Wrapper
 * @author HaiYang Li
 * @since 1.0
 */
@com.landawn.abacus.annotation.Immutable
public sealed class Keyed<K, T> implements Immutable permits IndexedKeyed {

    /**
     * The key used for hashing and equality comparisons.
     */
    protected final K key;

    /**
     * The value associated with the key.
     */
    protected final T val;

    Keyed(final K key, final T val) {
        this.key = key;
        this.val = val;
    }

    /**
     * Creates a new {@code Keyed} instance with the specified key and value.
     * 
     * <p>This factory method provides a convenient way to create instances without
     * directly calling the constructor. The resulting object is immutable.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Keyed<Integer, String> keyed = Keyed.of(42, "The Answer");
     * }</pre>
     * 
     * @param <K> the type of the key
     * @param <T> the type of the value
     * @param key the key used for hashing and equality comparisons
     * @param val the value associated with the key
     * @return a new {@code Keyed} instance
     */
    public static <K, T> Keyed<K, T> of(final K key, final T val) {
        return new Keyed<>(key, val);
    }

    /**
     * Returns the key of this key-value pair.
     * 
     * @return the key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the value of this key-value pair.
     * 
     * @return the value
     */
    public T val() {
        return val;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed
     * based solely on the key, ignoring the value. This ensures consistent
     * hashing behavior even if the value is mutable.
     * 
     * @return the hash code of the key
     */
    @Override
    public int hashCode() {
        return N.hashCode(key);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code Keyed} objects are considered equal if they have the same key,
     * regardless of their values. This allows for efficient lookups in collections
     * where only the key matters for uniqueness.
     * 
     * <p>Note: The comparison only considers objects of the exact same class,
     * not subclasses.</p>
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if this object has the same key as the obj argument;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && this.getClass().equals(obj.getClass())) {
            return N.equals(((Keyed<K, T>) obj).key, key);
        }

        return false;
    }

    /**
     * Returns a string representation of this {@code Keyed}.
     * The string representation includes both the key and value in a
     * readable format.
     * 
     * <p>For example, a {@code Keyed} with key "abc" and value 123
     * would return the string {@code "{key=abc, val=123}"}.</p>
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{key=" + N.toString(key) + ", val=" + val + "}";
    }
}