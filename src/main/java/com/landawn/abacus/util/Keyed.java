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

import com.landawn.abacus.annotation.MayReturnNull;

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
 * <p><b>Usage Examples:</b></p>
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

    /**
     * Constructs a new {@code Keyed} instance with the specified key and value.
     *
     * <p>This constructor is package-private and used internally by the factory method
     * {@link #of(Object, Object)} and subclasses.</p>
     *
     * @param key the key used for hashing and equality comparisons (can be {@code null})
     * @param val the value associated with the key (can be {@code null})
     */
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
     * <p>Both {@code key} and {@code val} can be {@code null}. However, be aware that
     * a {@code null} key will result in a hash code of 0 and may affect collection behavior.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed<Integer, String> keyed = Keyed.of(42, "The Answer");
     * Keyed<String, User> withNull = Keyed.of("id", null); // value can be null
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <T> the type of the value
     * @param key the key used for hashing and equality comparisons (can be {@code null})
     * @param val the value associated with the key (can be {@code null})
     * @return a new {@code Keyed} instance containing the specified key-value pair
     */
    public static <K, T> Keyed<K, T> of(final K key, final T val) {
        return new Keyed<>(key, val);
    }

    /**
     * Returns the key of this key-value pair.
     *
     * <p>The key is used for hashing and equality comparisons in collections.
     * This value is immutable once the {@code Keyed} instance is created.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed<String, Integer> keyed = Keyed.of("myKey", 42);
     * String key = keyed.key(); // returns "myKey"
     *
     * Keyed<String, Integer> nullKey = Keyed.of(null, 100);
     * String nullKeyValue = nullKey.key(); // returns null
     * }</pre>
     *
     * @return the key (can be {@code null})
     */
    @MayReturnNull
    public K key() {
        return key;
    }

    /**
     * Returns the value of this key-value pair.
     *
     * <p>The value is not used in hashing or equality comparisons, allowing for
     * efficient lookups based solely on the key. This value is immutable once
     * the {@code Keyed} instance is created.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed<String, Integer> keyed = Keyed.of("myKey", 42);
     * Integer value = keyed.val(); // returns 42
     *
     * Keyed<String, Integer> nullVal = Keyed.of("key", null);
     * Integer nullValue = nullVal.val(); // returns null
     * }</pre>
     *
     * @return the value (can be {@code null})
     */
    @MayReturnNull
    public T val() {
        return val;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed
     * based solely on the key, ignoring the value. This ensures consistent
     * hashing behavior even if the value is mutable.
     *
     * <p>This method uses {@link N#hashCode(Object)} to compute the hash code,
     * which properly handles {@code null} keys by returning 0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed<String, Integer> k1 = Keyed.of("key", 100);
     * Keyed<String, Integer> k2 = Keyed.of("key", 200);
     * assert k1.hashCode() == k2.hashCode(); // true, only key matters
     * }</pre>
     *
     * @return the hash code of the key, or 0 if the key is {@code null}
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
     * <p>The equality check follows these rules:</p>
     * <ul>
     *   <li>Returns {@code true} if {@code obj} is the same instance as this object</li>
     *   <li>Returns {@code false} if {@code obj} is {@code null}</li>
     *   <li>Returns {@code false} if {@code obj} is not of the exact same class (subclasses are not considered equal)</li>
     *   <li>Otherwise, compares keys using {@link N#equals(Object, Object)}, which handles {@code null} keys properly</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed<String, Integer> k1 = Keyed.of("key", 100);
     * Keyed<String, Integer> k2 = Keyed.of("key", 200);
     * Keyed<String, Integer> k3 = Keyed.of("other", 100);
     *
     * k1.equals(k2); // true - same key, different values
     * k1.equals(k3); // false - different keys
     * }</pre>
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
     * <p>Both the key and value are converted to strings using {@link N#toString(Object)},
     * which properly handles {@code null} values by converting them to the string {@code "null"}.</p>
     *
     * <p>The format is: {@code "{key=<key>, val=<value>}"}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Keyed.of("abc", 123).toString();        // returns "{key=abc, val=123}"
     * Keyed.of(null, "value").toString();     // returns "{key=null, val=value}"
     * Keyed.of("key", null).toString();       // returns "{key=key, val=null}"
     * }</pre>
     *
     * @return a string representation of this object in the format "{key=&lt;key&gt;, val=&lt;value&gt;}"
     */
    @Override
    public String toString() {
        return "{key=" + N.toString(key) + ", val=" + N.toString(val) + "}";
    }
}
