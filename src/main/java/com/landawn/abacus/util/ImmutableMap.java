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
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
public class ImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable {

    @SuppressWarnings("rawtypes")
    private static final ImmutableMap EMPTY = new ImmutableMap(N.emptyMap(), false);

    private final Map<K, V> map;

    /**
     * Constructs an ImmutableMap from the provided map.
     *
     * @param map the map whose mappings are to be placed in this ImmutableMap
     */
    ImmutableMap(final Map<? extends K, ? extends V> map) {
        this(map, ClassUtil.isPossibleImmutable(map.getClass())); // to create immutable keySet(), values(), entrySet()
    }

    /**
     * Constructs an ImmutableMap from the provided map.
     *
     * @param map the map whose mappings are to be placed in this ImmutableMap
     * @param isUnmodifiable a boolean value indicating if the provided map is unmodifiable
     */
    ImmutableMap(final Map<? extends K, ? extends V> map, final boolean isUnmodifiable) {
        this.map = isUnmodifiable ? (Map<K, V>) map : Collections.unmodifiableMap(map); // to create immutable keySet(), values(), entrySet()
    }

    /**
     * Returns an empty ImmutableMap.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return an empty ImmutableMap
     */
    public static <K, V> ImmutableMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pair.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param k1 the key to be included in the ImmutableMap
     * @param v1 the value of the key to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pair
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableMap
     * @param v1 the value of the first key to be included in the ImmutableMap
     * @param k2 the second key to be included in the ImmutableMap
     * @param v2 the value of the second key to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableMap
     * @param v1 the value of the first key to be included in the ImmutableMap
     * @param k2 the second key to be included in the ImmutableMap
     * @param v2 the value of the second key to be included in the ImmutableMap
     * @param k3 the third key to be included in the ImmutableMap
     * @param v3 the value of the third key to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k4 the keys to be included in the ImmutableMap
     * @param v1 to v4 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k5 the keys to be included in the ImmutableMap
     * @param v1 to v5 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k6 the keys to be included in the ImmutableMap
     * @param v1 to v6 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k7 the keys to be included in the ImmutableMap
     * @param v1 to v7 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k8 the keys to be included in the ImmutableMap
     * @param v1 to v8 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k9 the keys to be included in the ImmutableMap
     * @param v1 to v9 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9), false);
    }

    /**
     * Returns an ImmutableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k10 the keys to be included in the ImmutableMap
     * @param v1 to v10 the values to be included in the ImmutableMap
     * @return an ImmutableMap containing the provided key-value pairs
     */
    @SuppressWarnings("deprecation")
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        return new ImmutableMap<>(N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10), false);
    }

    /**
     * Returns an ImmutableMap containing the same mappings as the provided map.
     * If the provided map is already an instance of ImmutableMap, it is directly returned.
     * If the provided map is {@code null} or empty, an empty ImmutableMap is returned.
     * Otherwise, a new ImmutableMap is created with the elements of the provided map.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose mappings are to be placed in the ImmutableMap
     * @return an ImmutableMap containing the same mappings as the provided map
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
     * Wraps the provided map into an ImmutableMap. Changes to the specified map will be reflected in the ImmutableMap.
     * If the provided map is already an instance of ImmutableMap, it is directly returned.
     * If the provided map is {@code null}, an empty ImmutableMap is returned.
     * Otherwise, returns a new ImmutableMap backed by the provided map.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map to be wrapped into an ImmutableMap
     * @return an ImmutableMap backed by the provided map
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
     * Returns the value to which the specified key is mapped, or the defaultValue if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or the defaultValue if this map contains no mapping for the key
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        final V val = get(key);

        return val == null && !containsKey(key) ? defaultValue : val;
    }

    /**
     *
     * @param k
     * @param v
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V put(final K k, final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param o
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V remove(final Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param map
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final void putAll(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Put if absent.
     *
     * @param key
     * @param value
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V putIfAbsent(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param value
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean remove(final Object key, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean replace(final K key, final V oldValue, final V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param value
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V replace(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute if absent.
     *
     * @param key
     * @param mappingFunction
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute if present.
     *
     * @param key
     * @param remappingFunction
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param remappingFunction
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param value
     * @param remappingFunction
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
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
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
     * @see java.util.Map#get(Object)
     */
    @Override
    public V get(final Object key) {
        return map.get(key);
    }

    /**
     * Returns an unmodifiable Set view of the keys contained in this map.
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
     *
     * @return the number of key-value mappings in this map
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableMap.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return a new Builder instance
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableMap with the provided map as the backing map.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param backedMap the map to be used as the backing map for the Builder
     * @return a new Builder instance
     * @throws IllegalArgumentException if the provided map is null
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> backedMap) throws IllegalArgumentException {
        N.checkArgNotNull(backedMap);

        return new Builder<>(backedMap);
    }

    public static final class Builder<K, V> {
        private final Map<K, V> map;

        Builder() {
            map = new HashMap<>();
        }

        Builder(final Map<K, V> backedMap) {
            map = backedMap;
        }

        /**
         *
         * @param key
         * @param value
         * @return
         */
        public Builder<K, V> put(final K key, final V value) {
            map.put(key, value);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                map.putAll(m);
            }

            return this;
        }

        public ImmutableMap<K, V> build() {
            return ImmutableMap.wrap(map);
        }
    }
}
