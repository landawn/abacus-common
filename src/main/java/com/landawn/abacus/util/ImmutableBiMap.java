/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Map;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
@SuppressWarnings("java:S2160")
public final class ImmutableBiMap<K, V> extends ImmutableMap<K, V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>());

    private final BiMap<K, V> biMap;

    @SuppressWarnings("unchecked")
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map) {
        super(map);
        this.biMap = (BiMap<K, V>) map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @param k7
     * @param v7
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(final BiMap<? extends K, ? extends V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return new ImmutableBiMap<>(map.copy());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return an {@code ImmutableBiMap} backed by the specified {@code map}
     * @deprecated the ImmutableBiMap may be modified through the specified {@code map}
     */
    @Deprecated
    public static <K, V> ImmutableBiMap<K, V> wrap(final BiMap<? extends K, ? extends V> map) {
        if (map == null) {
            return empty();
        }

        return new ImmutableBiMap<>(map);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the by value.
     *
     * @param value
     * @return
     */
    public K getByValue(Object value) {
        return biMap.getByValue(value);
    }
}
