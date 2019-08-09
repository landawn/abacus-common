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

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableNavigableMap.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.4
 */
public class ImmutableNavigableMap<K, V> extends ImmutableSortedMap<K, V> implements NavigableMap<K, V> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableMap EMPTY = new ImmutableNavigableMap(N.emptyNavigableMap());

    /** The navigable map. */
    private final NavigableMap<K, V> navigableMap;

    /**
     * Instantiates a new immutable navigable map.
     *
     * @param navigableMap the navigable map
     */
    ImmutableNavigableMap(NavigableMap<? extends K, ? extends V> navigableMap) {
        super(navigableMap);
        this.navigableMap = (NavigableMap<K, V>) navigableMap;
    }

    /**
     * Empty.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the immutable navigable map
     */
    public static <K, V> ImmutableNavigableMap<K, V> empty() {
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
     * @return the immutable navigable map
     */
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);

        return new ImmutableNavigableMap<>(map);
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
     * @return the immutable navigable map
     */
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2,
            final v v2) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableNavigableMap<>(map);
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
     * @return the immutable navigable map
     */
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableNavigableMap<>(map);
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
     * @return the immutable navigable map
     */
    public static <K extends Comparable<? super K>, V, k extends K, v extends V> ImmutableNavigableMap<K, V> of(final k k1, final v v1, final k k2, final v v2,
            final k k3, final v v3, final k k4, final v v4) {
        final NavigableMap<K, V> map = N.newTreeMap();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableNavigableMap<>(map);
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
     * @return the immutable navigable map
     */
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
     * @return the immutable navigable map
     */
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
     * @return the immutable navigable map
     */
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
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param navigableMap the elements in this <code>map</code> are shared by the returned ImmutableNavigableMap.
     * @return the immutable navigable map
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
     * Copy of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap the sorted map
     * @return the immutable navigable map
     */
    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (N.isNullOrEmpty(sortedMap)) {
            return empty();
        }

        return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
    }

    /**
     * Of.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap the sorted map
     * @return the immutable sorted map
     */
    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> of(final SortedMap<? extends K, ? extends V> sortedMap) {
        throw new UnsupportedOperationException();
    }

    /**
     * Lower entry.
     *
     * @param key the key
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return navigableMap.lowerEntry(key);
    }

    /**
     * Lower key.
     *
     * @param key the key
     * @return the k
     */
    @Override
    public K lowerKey(K key) {
        return navigableMap.lowerKey(key);
    }

    /**
     * Floor entry.
     *
     * @param key the key
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return navigableMap.floorEntry(key);
    }

    /**
     * Floor key.
     *
     * @param key the key
     * @return the k
     */
    @Override
    public K floorKey(K key) {
        return navigableMap.floorKey(key);
    }

    /**
     * Ceiling entry.
     *
     * @param key the key
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return navigableMap.ceilingEntry(key);
    }

    /**
     * Ceiling key.
     *
     * @param key the key
     * @return the k
     */
    @Override
    public K ceilingKey(K key) {
        return navigableMap.ceilingKey(key);
    }

    /**
     * Higher entry.
     *
     * @param key the key
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return navigableMap.higherEntry(key);
    }

    /**
     * Higher key.
     *
     * @param key the key
     * @return the k
     */
    @Override
    public K higherKey(K key) {
        return navigableMap.higherKey(key);
    }

    /**
     * First entry.
     *
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> firstEntry() {
        return navigableMap.firstEntry();
    }

    /**
     * Last entry.
     *
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> lastEntry() {
        return navigableMap.lastEntry();
    }

    /**
     * Poll first entry.
     *
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return navigableMap.pollFirstEntry();
    }

    /**
     * Poll last entry.
     *
     * @return the map. entry
     */
    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return navigableMap.pollLastEntry();
    }

    /**
     * Descending map.
     *
     * @return the navigable map
     */
    @Override
    public NavigableMap<K, V> descendingMap() {
        return of(navigableMap.descendingMap());
    }

    /**
     * Navigable key set.
     *
     * @return the navigable set
     */
    @Override
    public NavigableSet<K> navigableKeySet() {
        return ImmutableNavigableSet.of(navigableMap.navigableKeySet());
    }

    /**
     * Descending key set.
     *
     * @return the navigable set
     */
    @Override
    public NavigableSet<K> descendingKeySet() {
        return ImmutableNavigableSet.of(navigableMap.descendingKeySet());
    }

    /**
     * Sub map.
     *
     * @param fromKey the from key
     * @param fromInclusive the from inclusive
     * @param toKey the to key
     * @param toInclusive the to inclusive
     * @return the navigable map
     */
    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return of(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    /**
     * Head map.
     *
     * @param toKey the to key
     * @param inclusive the inclusive
     * @return the navigable map
     */
    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return of(navigableMap.headMap(toKey, inclusive));
    }

    /**
     * Tail map.
     *
     * @param fromKey the from key
     * @param inclusive the inclusive
     * @return the navigable map
     */
    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return of(navigableMap.tailMap(fromKey, inclusive));
    }
}
