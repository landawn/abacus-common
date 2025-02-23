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
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("java:S2160")
public final class ImmutableBiMap<K, V> extends ImmutableMap<K, V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>());

    private final BiMap<K, V> biMap;

    /**
     * Constructs an ImmutableBiMap from the provided BiMap.
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
     * Returns an empty ImmutableBiMap.
     *
     * @param <K> the type of the keys in the ImmutableBiMap
     * @param <V> the type of the values in the ImmutableBiMap
     * @return an empty ImmutableBiMap
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pair.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param k1 the key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the key
     * @return an ImmutableBiMap containing the provided key-value pair
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
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
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k4 the keys to be included in the ImmutableBiMap
     * @param v1 to v4 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k5 the keys to be included in the ImmutableBiMap
     * @param v1 to v5 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k6 the keys to be included in the ImmutableBiMap
     * @param v1 to v6 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k7 the keys to be included in the ImmutableBiMap
     * @param v1 to v7 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k8 the keys to be included in the ImmutableBiMap
     * @param v1 to v8 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k9 the keys to be included in the ImmutableBiMap
     * @param v1 to v9 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 to k10 the keys to be included in the ImmutableBiMap
     * @param v1 to v10 the values to be included in the ImmutableBiMap
     * @return an ImmutableBiMap containing the provided key-value pairs
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing the same mappings as the provided BiMap.
     * If the provided BiMap is {@code null} or empty, an empty ImmutableBiMap is returned.
     * Otherwise, a new ImmutableBiMap is created with the elements of the provided BiMap.
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Wraps the provided BiMap into an ImmutableBiMap. Changes to the specified map will be reflected in the ImmutableBiMap.
     * If the provided BiMap is {@code null}, an empty ImmutableBiMap is returned.
     * Otherwise, returns an ImmutableBiMap backed by the provided BiMap.
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the key to which the specified value is mapped in the BiMap.
     *
     * @param value the value whose associated key is to be returned
     * @return the key to which the specified value is mapped, or {@code null} if this map contains no mapping for the value
     */
    public K getByValue(final Object value) {
        return biMap.getByValue(value);
    }
}
