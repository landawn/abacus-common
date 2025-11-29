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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.landawn.abacus.annotation.Beta;

/**
 * An immutable, thread-safe implementation of the Map interface.
 * Once created, the contents of an ImmutableMap cannot be modified.
 * All mutating operations (put, remove, clear, etc.) will throw UnsupportedOperationException.
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 * <li>{@link #empty()} - returns an empty immutable map</li>
 * <li>{@link #of(Object, Object)} - creates maps with specific key-value pairs</li>
 * <li>{@link #copyOf(Map)} - creates a defensive copy from another map</li>
 * <li>{@link #wrap(Map)} - wraps an existing map (changes to the underlying map will be reflected)</li>
 * <li>{@link #builder()} - provides a builder for constructing immutable maps</li>
 * </ul>
 * 
 * <p>The implementation preserves the iteration order of entries when created from a LinkedHashMap
 * or SortedMap, otherwise no specific iteration order is guaranteed.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an immutable map with specific entries
 * ImmutableMap<String, Integer> map = ImmutableMap.of(
 *     "one", 1,
 *     "two", 2,
 *     "three", 3
 * );
 * 
 * // Create from a builder
 * ImmutableMap<String, String> built = ImmutableMap.<String, String>builder()
 *     .put("key1", "value1")
 *     .put("key2", "value2")
 *     .build();
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Map
 * @see Immutable
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
public class ImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable {

    @SuppressWarnings("rawtypes")
    private static final ImmutableMap EMPTY = new ImmutableMap(N.emptyMap(), false);

    private final Map<K, V> map;

    private final Map<K, V> valueMap;

    ImmutableMap(final Map<? extends K, ? extends V> map) {
        this(map, ClassUtil.isPossibleImmutable(map.getClass())); // to create immutable keySet(), values(), entrySet()
    }

    ImmutableMap(final Map<? extends K, ? extends V> map, final boolean isUnmodifiable) {
        this.valueMap = (Map<K, V>) map;
        this.map = isUnmodifiable ? valueMap : Collections.unmodifiableMap(valueMap); // to create immutable keySet(), values(), entrySet()
    }

    /**
     * Returns an empty ImmutableMap. This method always returns the same cached instance,
     * making it memory efficient for representing empty maps.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> emptyMap = ImmutableMap.empty();
     * System.out.println(emptyMap.size());  // 0
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return an empty ImmutableMap instance
     */
    public static <K, V> ImmutableMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableMap containing a single key-value mapping.
     * The returned map is immutable and will have a size of 1.
     * The iteration order is guaranteed to match the order of insertion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> singleEntry = ImmutableMap.of("count", 42);
     * System.out.println(singleEntry.get("count"));  // 42
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param k1 the first key
     * @param v1 the first value
     * @return an ImmutableMap containing only the specified key-value pair
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1), false);
    }

    /**
     * Returns an ImmutableMap containing exactly two key-value mappings.
     * The returned map is immutable and will have a size of 2.
     * The iteration order is guaranteed to match the order of insertion.
     * If the same key is provided twice, an IllegalArgumentException may be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, String> map = ImmutableMap.of(
     *     "first", "John",
     *     "last", "Doe"
     * );
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2), false);
    }

    /**
     * Returns an ImmutableMap containing exactly three key-value mappings.
     * The returned map is immutable and will have a size of 3.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> scores = ImmutableMap.of(
     *     "Alice", 95,
     *     "Bob", 87,
     *     "Charlie", 92
     * );
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3), false);
    }

    /**
     * Returns an ImmutableMap containing exactly four key-value mappings.
     * The returned map is immutable and will have a size of 4.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4), false);
    }

    /**
     * Returns an ImmutableMap containing exactly five key-value mappings.
     * The returned map is immutable and will have a size of 5.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5), false);
    }

    /**
     * Returns an ImmutableMap containing exactly six key-value mappings.
     * The returned map is immutable and will have a size of 6.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @param k6 the sixth key
     * @param v6 the sixth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6), false);
    }

    /**
     * Returns an ImmutableMap containing exactly seven key-value mappings.
     * The returned map is immutable and will have a size of 7.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @param k6 the sixth key
     * @param v6 the sixth value
     * @param k7 the seventh key
     * @param v7 the seventh value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7), false);
    }

    /**
     * Returns an ImmutableMap containing exactly eight key-value mappings.
     * The returned map is immutable and will have a size of 8.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @param k6 the sixth key
     * @param v6 the sixth value
     * @param k7 the seventh key
     * @param v7 the seventh value
     * @param k8 the eighth key
     * @param v8 the eighth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8), false);
    }

    /**
     * Returns an ImmutableMap containing exactly nine key-value mappings.
     * The returned map is immutable and will have a size of 9.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @param k6 the sixth key
     * @param v6 the sixth value
     * @param k7 the seventh key
     * @param v7 the seventh value
     * @param k8 the eighth key
     * @param v8 the eighth value
     * @param k9 the ninth key
     * @param v9 the ninth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9), false);
    }

    /**
     * Returns an ImmutableMap containing exactly ten key-value mappings.
     * The returned map is immutable and will have a size of 10.
     * The iteration order is guaranteed to match the order of insertion.
     * If duplicate keys are provided, an IllegalArgumentException may be thrown.
     * Unlike some map implementations, this method supports {@code null} keys and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of(
     *     "zero", 0, "one", 1, "two", 2, "three", 3, "four", 4,
     *     "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @param k4 the fourth key
     * @param v4 the fourth value
     * @param k5 the fifth key
     * @param v5 the fifth value
     * @param k6 the sixth key
     * @param v6 the sixth value
     * @param k7 the seventh key
     * @param v7 the seventh value
     * @param k8 the eighth key
     * @param v8 the eighth value
     * @param k9 the ninth key
     * @param v9 the ninth value
     * @param k10 the tenth key
     * @param v10 the tenth value
     * @return an ImmutableMap containing the specified key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        // return new ImmutableMap<>(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10), true); // Doesn't support null key/value
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10), false);
    }

    /**
     * Returns an ImmutableMap containing all mappings from the provided map.
     * If the provided map is already an ImmutableMap, it is returned directly without copying.
     * If the map is {@code null} or empty, an empty ImmutableMap is returned.
     * Otherwise, a new ImmutableMap is created with a defensive copy of the map's entries.
     * The iteration order is preserved if the source map is a LinkedHashMap or SortedMap.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> mutable = new HashMap<>();
     * mutable.put("a", 1);
     * mutable.put("b", 2);
     * ImmutableMap<String, Integer> immutable = ImmutableMap.copyOf(mutable);
     * mutable.put("c", 3);                    // Does not affect immutable
     * System.out.println(immutable.size());   // Still 2
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose mappings are to be placed in the ImmutableMap
     * @return an ImmutableMap containing all mappings from the source map, or the same instance if already an ImmutableMap
     */
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableMap) {
            return (ImmutableMap<K, V>) map;
        } else if (N.isEmpty(map)) {
            return empty();
        } else {
            return new ImmutableMap<>(map instanceof LinkedHashMap || map instanceof SortedMap ? N.newLinkedHashMap(map) : N.newHashMap(map), false);
        }
    }

    /**
     * Wraps the provided map into an ImmutableMap without copying the entries.
     * If the provided map is already an ImmutableMap, it is returned directly.
     * If the map is {@code null}, an empty ImmutableMap is returned.
     *
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the
     * underlying Map will be reflected in the returned ImmutableMap, which
     * violates the immutability contract. For a {@code true} immutable copy, use
     * {@link #copyOf(Map)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> mutable = new HashMap<>();
     * mutable.put("a", 1);
     * ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(mutable);
     * mutable.put("b", 2);                  // This change IS visible in wrapped!
     * System.out.println(wrapped.size());   // 2
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map to be wrapped into an ImmutableMap
     * @return an ImmutableMap view of the provided map, or the same instance if already an ImmutableMap
     */
    @Beta
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableMap) {
            return (ImmutableMap<K, V>) map;
        } else if (map == null) {
            return empty();
        } else {
            return new ImmutableMap<>(map);
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or the defaultValue if this map
     * contains no mapping for the key. This method distinguishes between a key that is mapped
     * to {@code null} and a key that is not present in the map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.getOrDefault("a", 0));    // 1
     * System.out.println(map.getOrDefault("c", 0));    // 0
     * System.out.println(map.getOrDefault("c", 99));   // 99
     * }</pre>
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if the map contains no mapping for the key
     * @return the value to which the specified key is mapped, or defaultValue if no mapping exists
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        final V val = get(key);

        return val == null && !containsKey(key) ? defaultValue : val;
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param k ignored
     * @param v ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V put(final K k, final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param o ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V remove(final Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param map ignored
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final void putAll(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param value ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V putIfAbsent(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param value ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean remove(final Object key, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param oldValue ignored
     * @param newValue ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean replace(final K key, final V oldValue, final V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param value ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V replace(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param mappingFunction ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param remappingFunction ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param remappingFunction ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored
     * @param value ignored
     * @param remappingFunction ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableMap does not support modification operations
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * This method has the same behavior as checking if size() == 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> empty = ImmutableMap.empty();
     * ImmutableMap<String, Integer> nonEmpty = ImmutableMap.of("a", 1);
     * System.out.println(empty.isEmpty());      // true
     * System.out.println(nonEmpty.isEmpty());   // false
     * }</pre>
     *
     * @return {@code true} if this map contains no key-value mappings, {@code false} otherwise
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * More formally, returns {@code true} if and only if this map contains a mapping for a key k
     * such that (key==null ? k==null : key.equals(k)).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.containsKey("a"));    // true
     * System.out.println(map.containsKey("c"));    // false
     * System.out.println(map.containsKey(null));   // false (unless null key was added)
     * }</pre>
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @see java.util.Map#containsKey(Object)
     */
    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     * More formally, returns {@code true} if and only if this map contains at least one mapping
     * to a value v such that (value==null ? v==null : value.equals(v)).
     * This operation requires linear time in the size of the map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);
     * System.out.println(map.containsValue(1));   // true
     * System.out.println(map.containsValue(3));   // false
     * }</pre>
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     * @see java.util.Map#containsValue(Object)
     */
    @Override
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key. A return value of {@code null} does not necessarily indicate that the
     * map contains no mapping for the key; it's also possible that the map explicitly maps
     * the key to {@code null}. The containsKey operation may be used to distinguish these two cases.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.get("a"));   // 1
     * System.out.println(map.get("c"));   // null
     * }</pre>
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if no mapping exists
     * @see java.util.Map#get(Object)
     */
    @Override
    public V get(final Object key) {
        return map.get(key);
    }

    /**
     * Returns an unmodifiable Set view of the keys contained in this map.
     * The set is backed by the map, so it reflects the current state of the map.
     * Attempts to modify the returned set will result in an UnsupportedOperationException.
     * The iteration order of the set matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Set<String> keys = map.keySet();
     * System.out.println(keys);  // [a, b]
     * // keys.add("c");  // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an unmodifiable set view of the keys contained in this map
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns an unmodifiable Collection view of the values contained in this map.
     * The collection is backed by the map, so it reflects the current state of the map.
     * Attempts to modify the returned collection will result in an UnsupportedOperationException.
     * The iteration order of the collection matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Collection<Integer> values = map.values();
     * System.out.println(values);  // [1, 2]
     * // values.remove(1);  // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an unmodifiable collection view of the values contained in this map
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns an unmodifiable Set view of the mappings contained in this map.
     * The set is backed by the map, so it reflects the current state of the map.
     * Each element in the returned set is an immutable Map.Entry.
     * Attempts to modify the returned set or its entries will result in an UnsupportedOperationException.
     * The iteration order of the set matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Set<Map.Entry<String, Integer>> entries = map.entrySet();
     * for (Map.Entry<String, Integer> entry : entries) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @return an unmodifiable set view of the mappings contained in this map
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns the number of key-value mappings in this map.
     * If the map contains more than Integer.MAX_VALUE elements, returns Integer.MAX_VALUE.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
     * System.out.println(map.size());  // 3
     * }</pre>
     *
     * @return the number of key-value mappings in this map
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a map and the two maps represent the same mappings.
     * More formally, two maps m1 and m2 are equal if m1.entrySet().equals(m2.entrySet()).
     * This ensures that the equals method works properly across different implementations of the Map interface.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map3 = ImmutableMap.of("a", 1, "c", 3);
     * System.out.println(map1.equals(map2));   // true
     * System.out.println(map1.equals(map3));   // false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     * @see java.util.Map#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof ImmutableMap im && valueMap.equals(im.valueMap);
    }

    /**
     * Returns the hash code value for this map.
     * The hash code is computed as the sum of the hash codes of each entry in the map's entry set.
     * This ensures that two maps that are equal (according to the equals method) will have the same hash code.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map1.hashCode() == map2.hashCode());  // true
     * }</pre>
     *
     * @return the hash code value for this map
     * @see java.util.Map#hashCode()
     */
    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    /**
     * Returns a string representation of this map.
     * The string representation consists of a list of key-value mappings in the order returned by the map's entry set,
     * enclosed in braces ("{}"). Each key-value mapping is represented as the key followed by an equals sign ("=")
     * followed by the value. Adjacent mappings are separated by the characters ", " (comma and space).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map);  // {a=1, b=2}
     * }</pre>
     *
     * @return a string representation of this map
     */
    @Override
    public String toString() {
        return valueMap.toString();
    }

    /**
     * Creates a new Builder for constructing an ImmutableMap.
     * The builder allows adding key-value pairs one by one and then creating an immutable map.
     * This is useful when the number of entries is not known at compile time.
     * The builder uses a HashMap internally for efficient entry addition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .put("three", 3)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of keys to be maintained by the map
     * @param <V> the type of mapped values
     * @return a new Builder instance for creating an ImmutableMap
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new Builder for constructing an ImmutableMap using the provided map as storage.
     * The builder will add entries to the provided map and then create an immutable view of it.
     * This allows reusing an existing map instance as the backing storage.
     * Note that the provided map should not be modified outside the builder after this call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> backing = new LinkedHashMap<>();
     * ImmutableMap<String, Integer> map = ImmutableMap.builder(backing)
     *     .put("one", 1)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of keys to be maintained by the map
     * @param <V> the type of mapped values
     * @param backedMap the map to be used as the backing storage for the Builder
     * @return a new Builder instance that will use the provided map
     * @throws IllegalArgumentException if backedMap is null
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> backedMap) throws IllegalArgumentException {
        N.checkArgNotNull(backedMap);

        return new Builder<>(backedMap);
    }

    /**
     * A builder for creating ImmutableMap instances.
     * The builder pattern allows for flexible construction of immutable maps,
     * especially useful when entries are added conditionally or in loops.
     * 
     * <p>The builder is not thread-safe and should not be used concurrently
     * from multiple threads without external synchronization.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .putAll(otherMap)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of keys in the map being built
     * @param <V> the type of values in the map being built
     */
    public static final class Builder<K, V> {
        private final Map<K, V> map;

        Builder() {
            map = new HashMap<>();
        }

        Builder(final Map<K, V> backedMap) {
            map = backedMap;
        }

        /**
         * Associates the specified value with the specified key in the map being built.
         * If the map previously contained a mapping for the key, the old value is replaced.
         * Null keys and values are permitted.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.put("key", "value")
         *        .put("another", "data");
         * }</pre>
         *
         * @param key the key with which the specified value is to be associated
         * @param value the value to be associated with the specified key
         * @return this builder instance for method chaining
         */
        public Builder<K, V> put(final K key, final V value) {
            map.put(key, value);

            return this;
        }

        /**
         * Copies all of the mappings from the specified map to the map being built.
         * The effect of this call is equivalent to calling put(k, v) on this builder
         * for each mapping from key k to value v in the specified map.
         * The behavior is undefined if the specified map is modified while this operation is in progress.
         * If the map is {@code null} or empty, no entries are added.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> existing = Map.of("a", 1, "b", 2);
         * builder.putAll(existing)
         *        .put("c", 3);
         * }</pre>
         *
         * @param m the map whose mappings are to be added, may be {@code null} or empty
         * @return this builder instance for method chaining
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                map.putAll(m);
            }

            return this;
        }

        /**
         * Builds and returns an ImmutableMap containing all entries added to this builder.
         * After calling this method, the builder should not be used further as the created
         * ImmutableMap may be backed by the builder's internal storage.
         *
         * <p>The returned map is immutable and will throw UnsupportedOperationException
         * for any modification attempts. The iteration order depends on the type of map
         * used internally by the builder.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableMap<String, Integer> map = builder.build();
         * // builder should not be used after this point
         * }</pre>
         *
         * @return a new ImmutableMap containing all added entries
         */
        public ImmutableMap<K, V> build() {
            return ImmutableMap.wrap(map);
        }
    }
}
