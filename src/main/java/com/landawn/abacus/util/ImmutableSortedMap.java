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

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.landawn.abacus.annotation.Beta;

/**
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("java:S2160")
public class ImmutableSortedMap<K, V> extends ImmutableMap<K, V> implements SortedMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedMap EMPTY = new ImmutableSortedMap(N.emptySortedMap());

    private final SortedMap<K, V> sortedMap;

    /**
     * Constructs an ImmutableSortedMap from the provided SortedMap.
     *
     * @param sortedMap the SortedMap to be used as the base for the ImmutableSortedMap
     */
    ImmutableSortedMap(final SortedMap<? extends K, ? extends V> sortedMap) {
        super(sortedMap);
        this.sortedMap = (SortedMap<K, V>) sortedMap;
    }

    /**
     * Returns an empty ImmutableSortedMap.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @return an empty ImmutableSortedMap
     */
    public static <K, V> ImmutableSortedMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pair.
     *
     * @param <K> the type of the key in the ImmutableSortedMap
     * @param <V> the type of the value in the ImmutableSortedMap
     * @param k1 the key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the key
     * @return an ImmutableSortedMap containing the provided key-value pair
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k4 the keys to be included in the ImmutableSortedMap
     * @param v1 to v4 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
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
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k5 the keys to be included in the ImmutableSortedMap
     * @param v1 to v5 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
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
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k6 the keys to be included in the ImmutableSortedMap
     * @param v1 to v6 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
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
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k7 the keys to be included in the ImmutableSortedMap
     * @param v1 to v7 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
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
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k8 the keys to be included in the ImmutableSortedMap
     * @param v1 to v8 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k9 the keys to be included in the ImmutableSortedMap
     * @param v1 to v9 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 to k10 the keys to be included in the ImmutableSortedMap
     * @param v1 to v10 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9,
            final K k10, final V v10) {
        final SortedMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);
        map.put(k10, v10);

        return new ImmutableSortedMap<>(map);
    }

    /**
     * Returns an ImmutableSortedMap containing the same mappings as the provided SortedMap.
     * If the provided SortedMap is already an instance of ImmutableSortedMap, it is directly returned.
     * If the provided SortedMap is {@code null} or empty, an empty ImmutableSortedMap is returned.
     * Otherwise, a new ImmutableSortedMap is created with the elements of the provided SortedMap.
     *
     * @param <K> the type of keys in the SortedMap
     * @param <V> the type of values in the SortedMap
     * @param sortedMap the SortedMap whose mappings are to be placed in the ImmutableSortedMap
     * @return an ImmutableSortedMap containing the same mappings as the provided SortedMap
     */
    public static <K, V> ImmutableSortedMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (sortedMap instanceof ImmutableSortedMap) {
            return (ImmutableSortedMap<K, V>) sortedMap;
        } else if (N.isEmpty(sortedMap)) {
            return empty();
        } else {
            return new ImmutableSortedMap<>(new TreeMap<>(sortedMap));
        }
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
     * Wraps the provided SortedMap into an ImmutableSortedMap. Changes to the specified SortedMap will be reflected in the ImmutableSortedMap.
     * If the provided SortedMap is already an instance of ImmutableSortedMap, it is directly returned.
     * If the SortedMap is {@code null}, an empty ImmutableSortedMap is returned.
     * Otherwise, returns a new ImmutableSortedMap backed by the provided SortedMap.
     *
     * @param <K> the type of keys in the SortedMap
     * @param <V> the type of values in the SortedMap
     * @param sortedMap the SortedMap to be wrapped into an ImmutableSortedMap
     * @return an ImmutableSortedMap backed by the provided SortedMap
     */
    @Beta
    public static <K, V> ImmutableSortedMap<K, V> wrap(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (sortedMap instanceof ImmutableSortedMap) {
            return (ImmutableSortedMap<K, V>) sortedMap;
        } else if (sortedMap == null) {
            return empty();
        } else {
            return new ImmutableSortedMap<>(sortedMap);
        }
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
    public ImmutableSortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return wrap(sortedMap.subMap(fromKey, toKey));
    }

    /**
     *
     * @param toKey
     * @return
     */
    @Override
    public ImmutableSortedMap<K, V> headMap(final K toKey) {
        return wrap(sortedMap.headMap(toKey));
    }

    /**
     *
     * @param fromKey
     * @return
     */
    @Override
    public ImmutableSortedMap<K, V> tailMap(final K fromKey) {
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
