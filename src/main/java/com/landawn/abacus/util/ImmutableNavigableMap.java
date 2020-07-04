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
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
public class ImmutableNavigableMap<K, V> extends ImmutableSortedMap<K, V> implements NavigableMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableMap EMPTY = new ImmutableNavigableMap(N.emptyNavigableMap());

    private final NavigableMap<K, V> navigableMap;

    ImmutableNavigableMap(NavigableMap<? extends K, ? extends V> navigableMap) {
        super(navigableMap);
        this.navigableMap = (NavigableMap<K, V>) navigableMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> ImmutableNavigableMap<K, V> empty() {
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableNavigableMap<>(map);
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2,
            final V v2) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableNavigableMap<>(map);
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2,
            final K k3, final V v3) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableNavigableMap<>(map);
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2,
            final K k3, final V v3, final K k4, final V v4) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableNavigableMap<>(map);
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2,
            final K k3, final V v3, final K k4, final V v4, final K k5, final V v5) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return new ImmutableNavigableMap<>(map);
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2,
            final K k3, final V v3, final K k4, final V v4, final K k5, final V v5, final K k6, final V v6) {
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
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2,
            final K k3, final V v3, final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7) {
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param navigableMap the elements in this <code>map</code> are shared by the returned ImmutableNavigableMap.
     * @return
     */
    public static <K, V> ImmutableNavigableMap<K, V> of(final NavigableMap<? extends K, ? extends V> navigableMap) {
        if (navigableMap == null) {
            return empty();
        } else if (navigableMap instanceof ImmutableNavigableMap) {
            return (ImmutableNavigableMap<K, V>) navigableMap;
        }

        return new ImmutableNavigableMap<>(navigableMap);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap
     * @return
     */
    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (N.isNullOrEmpty(sortedMap)) {
            return empty();
        }

        return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> of(final SortedMap<? extends K, ? extends V> sortedMap) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return navigableMap.lowerEntry(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K lowerKey(K key) {
        return navigableMap.lowerKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return navigableMap.floorEntry(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K floorKey(K key) {
        return navigableMap.floorKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return navigableMap.ceilingEntry(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K ceilingKey(K key) {
        return navigableMap.ceilingKey(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return navigableMap.higherEntry(key);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public K higherKey(K key) {
        return navigableMap.higherKey(key);
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return navigableMap.firstEntry();
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return navigableMap.lastEntry();
    }

    /**
     * Poll first entry.
     *
     * @return
     */
    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return navigableMap.pollFirstEntry();
    }

    /**
     * Poll last entry.
     *
     * @return
     */
    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return navigableMap.pollLastEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return of(navigableMap.descendingMap());
    }

    /**
     * Navigable key set.
     *
     * @return
     */
    @Override
    public NavigableSet<K> navigableKeySet() {
        return ImmutableNavigableSet.of(navigableMap.navigableKeySet());
    }

    /**
     * Descending key set.
     *
     * @return
     */
    @Override
    public NavigableSet<K> descendingKeySet() {
        return ImmutableNavigableSet.of(navigableMap.descendingKeySet());
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
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return of(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    /**
     *
     * @param toKey
     * @param inclusive
     * @return
     */
    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return of(navigableMap.headMap(toKey, inclusive));
    }

    /**
     *
     * @param fromKey
     * @param inclusive
     * @return
     */
    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return of(navigableMap.tailMap(fromKey, inclusive));
    }
}
