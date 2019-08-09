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
     * @param map the map
     */
    @SuppressWarnings("unchecked")
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map) {
        super(map);
        this.biMap = (BiMap<K, V>) map;
    }

    /**
     * Empty.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the immutable bi map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @param k3 the k 3
     * @param v3 the v 3
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @param k3 the k 3
     * @param v3 the v 3
     * @param k4 the k 4
     * @param v4 the v 4
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @param k3 the k 3
     * @param v3 the v 3
     * @param k4 the k 4
     * @param v4 the v 4
     * @param k5 the k 5
     * @param v5 the v 5
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @param k3 the k 3
     * @param v3 the v 3
     * @param k4 the k 4
     * @param v4 the v 4
     * @param k5 the k 5
     * @param v5 the v 5
     * @param k6 the k 6
     * @param v6 the v 6
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k> the generic type
     * @param <v> the generic type
     * @param k1 the k 1
     * @param v1 the v 1
     * @param k2 the k 2
     * @param v2 the v 2
     * @param k3 the k 3
     * @param v3 the v 3
     * @param k4 the k 4
     * @param v4 the v 4
     * @param k5 the k 5
     * @param v5 the v 5
     * @param k6 the k 6
     * @param v6 the v 6
     * @param k7 the k 7
     * @param v7 the v 7
     * @return the immutable bi map
     */
    public static <K, V, k extends K, v extends V> ImmutableBiMap<K, V> of(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<K, V>(biMap);
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the elements in this <code>map</code> are shared by the returned ImmutableBiMap.
     * @return the immutable bi map
     */
    public static <K, V> ImmutableBiMap<K, V> of(final BiMap<? extends K, ? extends V> map) {
        if (map == null) {
            return empty();
        }

        return new ImmutableBiMap<>(map);
    }

    /**
     * Copy of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map
     * @return the immutable bi map
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(final BiMap<? extends K, ? extends V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return new ImmutableBiMap<>(map.copy());
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map
     * @return the immutable map
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> of(final Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * Copy of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map
     * @return the immutable map
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the by value.
     *
     * @param value the value
     * @return the by value
     */
    public K getByValue(Object value) {
        return biMap.getByValue(value);
    }
}
