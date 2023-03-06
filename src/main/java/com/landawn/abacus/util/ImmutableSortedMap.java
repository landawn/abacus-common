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

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
@SuppressWarnings("java:S2160")
public class ImmutableSortedMap<K, V> extends ImmutableMap<K, V> implements SortedMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedMap EMPTY = new ImmutableSortedMap(N.emptySortedMap());

    private final SortedMap<K, V> sortedMap;

    ImmutableSortedMap(SortedMap<? extends K, ? extends V> sortedMap) {
        super(sortedMap);
        this.sortedMap = (SortedMap<K, V>) sortedMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> ImmutableSortedMap<K, V> empty() {
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return new ImmutableSortedMap<>(map);
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
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);

        return new ImmutableSortedMap<>(map);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap
     * @return
     */
    public static <K, V> ImmutableSortedMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (N.isNullOrEmpty(sortedMap)) {
            return empty();
        }

        return new ImmutableSortedMap<>(new TreeMap<>(sortedMap));
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
     * @param sortedMap
     * @return an {@code ImmutableSortedMap} backed by the specified {@code sortedMap}
     * @deprecated the ImmutableSortedMap may be modified through the specified {@code sortedMap}
     */
    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> wrap(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (sortedMap == null) {
            return empty();
        } else if (sortedMap instanceof ImmutableSortedMap) {
            return (ImmutableSortedMap<K, V>) sortedMap;
        }

        return new ImmutableSortedMap<>(sortedMap);
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

    @Override
    public Comparator<? super K> comparator() {
        return sortedMap.comparator();
    }

    /**
     *
     * @param fromKey
     * @param toKey
     * @return
     */
    @Override
    public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
        return wrap(sortedMap.subMap(fromKey, toKey));
    }

    /**
     *
     * @param toKey
     * @return
     */
    @Override
    public ImmutableSortedMap<K, V> headMap(K toKey) {
        return wrap(sortedMap.headMap(toKey));
    }

    /**
     *
     * @param fromKey
     * @return
     */
    @Override
    public ImmutableSortedMap<K, V> tailMap(K fromKey) {
        return wrap(sortedMap.tailMap(fromKey));
    }

    @Override
    public K firstKey() {
        return sortedMap.firstKey();
    }

    @Override
    public K lastKey() {
        return sortedMap.lastKey();
    }
}
