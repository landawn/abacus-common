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
 * @since 1.1.4
 * 
 * @author Haiyang Li
 */
public class ImmutableNavigableMap<K, V> extends ImmutableSortedMap<K, V> implements NavigableMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableMap EMPTY = new ImmutableNavigableMap(N.emptyNavigableMap());

    private final NavigableMap<K, V> navigableMap;

    ImmutableNavigableMap(NavigableMap<? extends K, ? extends V> navigableMap) {
        super(navigableMap);
        this.navigableMap = (NavigableMap<K, V>) navigableMap;
    }

    public static <K, V> ImmutableNavigableMap<K, V> empty() {
        return EMPTY;
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2,
            final v v2) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3, final k k4, final v v4) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3, final k k4, final v v4, final k k5, final v v5) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3, final k k4, final v v4, final k k5, final v v5, final k k6, final v v6) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return new ImmutableNavigableMap<>(map);
    }

    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3, final k k4, final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
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

    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (N.isNullOrEmpty(sortedMap)) {
            return empty();
        }

        return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
    }

    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> of(final SortedMap<? extends K, ? extends V> sortedMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return navigableMap.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        return navigableMap.lowerKey(key);
    }

    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return navigableMap.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        return navigableMap.floorKey(key);
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return navigableMap.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        return navigableMap.ceilingKey(key);
    }

    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return navigableMap.higherEntry(key);
    }

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

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return navigableMap.pollFirstEntry();
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return navigableMap.pollLastEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return of(navigableMap.descendingMap());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return ImmutableNavigableSet.of(navigableMap.navigableKeySet());
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return ImmutableNavigableSet.of(navigableMap.descendingKeySet());
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return of(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return of(navigableMap.headMap(toKey, inclusive));
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return of(navigableMap.tailMap(fromKey, inclusive));
    }
}
