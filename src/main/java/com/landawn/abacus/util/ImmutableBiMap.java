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

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableBiMap.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
public final class ImmutableBiMap<K, V> extends ImmutableMap<K, V> {

    /** The Constant EMPTY. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>());

    /** The bi map. */
    private final BiMap<K, V> biMap;

    /**
     * Instantiates a new immutable bi map.
     *
     * @param map
     */
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6) {
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
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the elements in this <code>map</code> are shared by the returned ImmutableBiMap.
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> of(final BiMap<? extends K, ? extends V> map) {
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
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> of(final Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
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
