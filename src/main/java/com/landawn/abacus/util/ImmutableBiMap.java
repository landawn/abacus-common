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

import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * An immutable, thread-safe implementation of a bidirectional map (BiMap).
 * A BiMap is a map that preserves the uniqueness of both its keys and values,
 * allowing efficient lookups in both directions (key-to-value and value-to-key).
 * 
 * <p>Once created, the contents of an ImmutableBiMap cannot be modified.
 * All mutating operations inherited from Map will throw {@link UnsupportedOperationException}.
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 *   <li>{@link #empty()} - returns an empty ImmutableBiMap</li>
 *   <li>{@link #of(Object, Object)} - creates an ImmutableBiMap with the specified key-value pairs</li>
 *   <li>{@link #copyOf(BiMap)} - creates a defensive copy from another BiMap</li>
 *   <li>{@link #wrap(BiMap)} - wraps an existing BiMap (changes to the underlying map will be reflected)</li>
 * </ul>
 * 
 * <p>Unlike regular maps, BiMaps enforce that values are unique. If duplicate values
 * are provided during construction, an exception may be thrown.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
 *     "one", 1,
 *     "two", 2,
 *     "three", 3
 * );
 * 
 * // Forward lookup
 * Integer value = biMap.get("two"); // returns 2
 * 
 * // Reverse lookup
 * String key = biMap.getByValue(2); // returns "two"
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see BiMap
 * @see ImmutableMap
 */
@SuppressWarnings("java:S2160")
public final class ImmutableBiMap<K, V> extends ImmutableMap<K, V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>());

    private final BiMap<K, V> biMap;

    /**
     * Constructs an ImmutableBiMap from the provided BiMap.
     * This constructor is marked as @Internal and should not be used directly.
     * Use the static factory methods instead.
     *
     * @param map the BiMap whose mappings are to be placed in this ImmutableBiMap
     */
    @Internal
    @SuppressWarnings("unchecked")
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map) {
        super(map);
        biMap = (BiMap<K, V>) map;
    }

    /**
     * Returns an empty ImmutableBiMap. This method always returns the same cached instance,
     * making it memory efficient for representing empty bidirectional maps.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
     * System.out.println(empty.size()); // prints: 0
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableBiMap
     * @param <V> the type of the values in the ImmutableBiMap
     * @return an empty ImmutableBiMap instance
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableBiMap containing a single key-value mapping.
     * The returned BiMap is immutable and will have a size of 1.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> single = ImmutableBiMap.of("one", 1);
     * System.out.println(single.get("one")); // prints: 1
     * System.out.println(single.getByValue(1)); // prints: "one"
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param k1 the key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the key
     * @return an ImmutableBiMap containing the provided key-value pair
     * @throws IllegalArgumentException if the key or value is null
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly two key-value mappings.
     * Both keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
     *     "first", 1,
     *     "second", 2
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly three key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly four key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly five key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly six key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly seven key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly eight key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly nine key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableBiMap
     * @param v9 the value to be associated with the ninth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly ten key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableBiMap
     * @param v9 the value to be associated with the ninth key
     * @param k10 the tenth key to be included in the ImmutableBiMap
     * @param v10 the value to be associated with the tenth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the same mappings as the provided BiMap.
     * If the provided BiMap is {@code null} or empty, an empty ImmutableBiMap is returned.
     * Otherwise, a new ImmutableBiMap is created with a defensive copy of the BiMap's entries.
     * 
     * <p>This method creates a defensive copy, so subsequent modifications to the original
     * BiMap will not affect the returned ImmutableBiMap.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> mutable = new BiMap<>();
     * mutable.put("one", 1);
     * mutable.put("two", 2);
     * 
     * ImmutableBiMap<String, Integer> immutable = ImmutableBiMap.copyOf(mutable);
     * mutable.put("three", 3); // Does not affect 'immutable'
     * }</pre>
     *
     * @param <K> the type of keys in the BiMap
     * @param <V> the type of values in the BiMap
     * @param map the BiMap whose mappings are to be placed in the ImmutableBiMap
     * @return an ImmutableBiMap containing the same mappings as the provided BiMap
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(final BiMap<? extends K, ? extends V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return new ImmutableBiMap<>(map.copy());
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     * Use {@link #copyOf(BiMap)} instead for BiMaps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map to copy
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #copyOf(BiMap)} instead
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Wraps the provided BiMap into an ImmutableBiMap without copying the entries.
     * The returned ImmutableBiMap is backed by the provided BiMap, so changes to the original
     * BiMap will be reflected in the ImmutableBiMap. However, the ImmutableBiMap itself cannot be modified.
     * 
     * <p>If the provided BiMap is {@code null}, an empty ImmutableBiMap is returned.
     * 
     * <p><b>Warning:</b> Use this method with caution as the immutability guarantee depends on not modifying
     * the original BiMap after wrapping. This method is marked as @Beta.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> mutable = new BiMap<>();
     * mutable.put("one", 1);
     * 
     * ImmutableBiMap<String, Integer> wrapped = ImmutableBiMap.wrap(mutable);
     * mutable.put("two", 2); // This WILL affect 'wrapped'!
     * }</pre>
     *
     * @param <K> the type of keys in the BiMap
     * @param <V> the type of values in the BiMap
     * @param map the BiMap to be wrapped into an ImmutableBiMap
     * @return an ImmutableBiMap backed by the provided BiMap
     */
    @Beta
    public static <K, V> ImmutableBiMap<K, V> wrap(final BiMap<? extends K, ? extends V> map) {
        if (map == null) {
            return empty();
        }

        return new ImmutableBiMap<>(map);
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     * Use {@link #wrap(BiMap)} instead for BiMaps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map to wrap
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #wrap(BiMap)} instead
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the key to which the specified value is mapped in this BiMap,
     * or {@code null} if this BiMap contains no mapping for the value.
     * 
     * <p>This is the reverse lookup operation that makes BiMap bidirectional.
     * The lookup is typically as efficient as the forward lookup (get by key).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
     *     "one", 1,
     *     "two", 2
     * );
     * 
     * String key = biMap.getByValue(2); // returns "two"
     * String notFound = biMap.getByValue(3); // returns null
     * }</pre>
     *
     * @param value the value whose associated key is to be returned
     * @return the key to which the specified value is mapped, or {@code null} if this map contains no mapping for the value
     */
    public K getByValue(final Object value) {
        return biMap.getByValue(value);
    }
}
