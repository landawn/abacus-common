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
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import com.landawn.abacus.annotation.Beta;

/**
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("java:S2160")
public class ImmutableNavigableMap<K, V> extends ImmutableSortedMap<K, V> implements NavigableMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableMap EMPTY = new ImmutableNavigableMap(N.emptyNavigableMap());

    private final NavigableMap<K, V> navigableMap;

    /**
     * Constructs an ImmutableNavigableMap from the provided NavigableMap.
     *
     * @param navigableMap the NavigableMap to be used as the base for the ImmutableNavigableMap
     */
    ImmutableNavigableMap(final NavigableMap<? extends K, ? extends V> navigableMap) {
        super(navigableMap);
        this.navigableMap = (NavigableMap<K, V>) navigableMap;
    }

    /**
     * Returns an empty ImmutableNavigableMap.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @return an empty ImmutableNavigableMap
     */
    public static <K, V> ImmutableNavigableMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pair.
     *
     * @param <K> the type of the key in the ImmutableNavigableMap
     * @param <V> the type of the value in the ImmutableNavigableMap
     * @param k1 the key to be included in the ImmutableNavigableMap
     * @param v1 the value to be associated with the key
     * @return an ImmutableNavigableMap containing the provided key-value pair
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 the first key to be included in the ImmutableNavigableMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableNavigableMap
     * @param v2 the value to be associated with the second key
     * @return an ImmutableNavigableMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 the first key to be included in the ImmutableNavigableMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableNavigableMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableNavigableMap
     * @param v3 the value to be associated with the third key
     * @return an ImmutableNavigableMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k4 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v4 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k5 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v5 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k6 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v6 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k7 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v7 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k8 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v8 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k9 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v9 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the provided key-value pairs.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k10 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v10 the values to be associated with the keys
     * @return an ImmutableSortedMap containing the provided key-value pairs
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
            final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9,
            final K k10, final V v10) {
        final NavigableMap<K, V> map = N.newTreeMap();

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

        return new ImmutableNavigableMap<>(map);
    }

    /**
     * Returns an ImmutableNavigableMap containing the same mappings as the provided SortedMap.
     * If the provided SortedMap is already an instance of ImmutableNavigableMap, it is directly returned.
     * If the provided SortedMap is {@code null} or empty, an empty ImmutableNavigableMap is returned.
     * Otherwise, a new ImmutableNavigableMap is created with the elements of the provided SortedMap.
     *
     * @param <K> the type of keys in the SortedMap
     * @param <V> the type of values in the SortedMap
     * @param sortedMap the SortedMap whose mappings are to be placed in the ImmutableNavigableMap
     * @return an ImmutableNavigableMap containing the same mappings as the provided SortedMap
     */
    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (sortedMap instanceof ImmutableNavigableMap) {
            return (ImmutableNavigableMap<K, V>) sortedMap;
        } else if (N.isEmpty(sortedMap)) {
            return empty();
        } else {
            return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
        }
    }

    /**
     * Returns an ImmutableNavigableMap that is backed by the provided NavigableMap. Changes to the specified NavigableMap will be reflected in the ImmutableNavigableMap.
     * If the provided NavigableMap is already an instance of ImmutableNavigableMap, it is directly returned.
     * If the NavigableMap is {@code null}, an empty ImmutableNavigableMap is returned.
     * Otherwise, returns a new ImmutableNavigableMap backed by the provided NavigableMap.
     *
     * @param <K> the type of keys in the NavigableMap
     * @param <V> the type of values in the NavigableMap
     * @param navigableMap the NavigableMap to be used as the base for the ImmutableNavigableMap
     * @return an ImmutableNavigableMap that is backed by the provided NavigableMap
     */
    @Beta
    public static <K, V> ImmutableNavigableMap<K, V> wrap(final NavigableMap<? extends K, ? extends V> navigableMap) {
        if (navigableMap instanceof ImmutableNavigableMap) {
            return (ImmutableNavigableMap<K, V>) navigableMap;
        } else if (navigableMap == null) {
            return empty();
        } else {
            return new ImmutableNavigableMap<>(navigableMap);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> wrap(final SortedMap<? extends K, ? extends V> sortedMap) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public ImmutableEntry<K, V> lowerEntry(final K key) {
        return ImmutableEntry.copyOf(navigableMap.lowerEntry(key));
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K lowerKey(final K key) {
        return navigableMap.lowerKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public ImmutableEntry<K, V> floorEntry(final K key) {
        return ImmutableEntry.copyOf(navigableMap.floorEntry(key));
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K floorKey(final K key) {
        return navigableMap.floorKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public ImmutableEntry<K, V> ceilingEntry(final K key) {
        return ImmutableEntry.copyOf(navigableMap.ceilingEntry(key));
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K ceilingKey(final K key) {
        return navigableMap.ceilingKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public ImmutableEntry<K, V> higherEntry(final K key) {
        return ImmutableEntry.copyOf(navigableMap.higherEntry(key));
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K higherKey(final K key) {
        return navigableMap.higherKey(key);
    }

    @Override
    public ImmutableEntry<K, V> firstEntry() {
        return ImmutableEntry.copyOf(navigableMap.firstEntry());
    }

    @Override
    public ImmutableEntry<K, V> lastEntry() {
        return ImmutableEntry.copyOf(navigableMap.lastEntry());
    }

    /**
     * Poll first entry.
     *
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollFirstEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Poll last entry.
     *
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollLastEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableNavigableMap<K, V> descendingMap() {
        return wrap(navigableMap.descendingMap());
    }

    /**
     * Navigable key set.
     *
     * @return
     */
    @Override
    public ImmutableNavigableSet<K> navigableKeySet() {
        return ImmutableNavigableSet.wrap(navigableMap.navigableKeySet());
    }

    /**
     * Descending key set.
     *
     * @return
     */
    @Override
    public ImmutableNavigableSet<K> descendingKeySet() {
        return ImmutableNavigableSet.wrap(navigableMap.descendingKeySet());
    }

    /**
     *
     * @param fromKey
     * @param fromInclusive
     * @param toKey
     * @param toInclusive
     * @return
     */
    @Override
    public ImmutableNavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        return wrap(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    /**
     *
     * @param toKey
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        return wrap(navigableMap.headMap(toKey, inclusive));
    }

    /**
     *
     * @param fromKey
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        return wrap(navigableMap.tailMap(fromKey, inclusive));
    }
}
