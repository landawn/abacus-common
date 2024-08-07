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

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
@SuppressWarnings("java:S2160")
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @return
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K extends Comparable<? super K>, V> ImmutableNavigableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableNavigableMap<>(map);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
     * 
     *
     * @param <K> 
     * @param <V> 
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
     * @param k8 
     * @param v8 
     * @return 
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
     * 
     *
     * @param <K> 
     * @param <V> 
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
     * @param k8 
     * @param v8 
     * @param k9 
     * @param v9 
     * @return 
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
     * 
     *
     * @param <K> 
     * @param <V> 
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
     * @param k8 
     * @param v8 
     * @param k9 
     * @param v9 
     * @param k10 
     * @param v10 
     * @return 
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap
     * @return
     */
    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (N.isEmpty(sortedMap)) {
            return empty();
        }

        return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param navigableMap
     * @return an {@code ImmutableNavigableMap} backed by the specified {@code navigableMap}
     */
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
     *
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
    public ImmutableEntry<K, V> lowerEntry(K key) {
        return ImmutableEntry.copyOf(navigableMap.lowerEntry(key));
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
    public ImmutableEntry<K, V> floorEntry(K key) {
        return ImmutableEntry.copyOf(navigableMap.floorEntry(key));
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
    public ImmutableEntry<K, V> ceilingEntry(K key) {
        return ImmutableEntry.copyOf(navigableMap.ceilingEntry(key));
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
    public ImmutableEntry<K, V> higherEntry(K key) {
        return ImmutableEntry.copyOf(navigableMap.higherEntry(key));
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

    /**
     *
     *
     * @return
     */
    @Override
    public ImmutableEntry<K, V> firstEntry() {
        return ImmutableEntry.copyOf(navigableMap.firstEntry());
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @return
     */
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
    public ImmutableNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return wrap(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    /**
     *
     * @param toKey
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return wrap(navigableMap.headMap(toKey, inclusive));
    }

    /**
     *
     * @param fromKey
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return wrap(navigableMap.tailMap(fromKey, inclusive));
    }
}
