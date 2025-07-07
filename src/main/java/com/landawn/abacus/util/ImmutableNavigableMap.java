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

import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import com.landawn.abacus.annotation.Beta;

/**
 * An immutable, thread-safe implementation of the NavigableMap interface.
 * This class extends ImmutableSortedMap and provides additional navigation methods
 * for accessing entries based on their ordering.
 * 
 * <p>A NavigableMap extends SortedMap with navigation methods returning the closest
 * matches for given search targets. Methods like {@link #lowerEntry}, {@link #floorEntry},
 * {@link #ceilingEntry}, and {@link #higherEntry} return Map.Entry objects associated
 * with keys respectively less than, less than or equal, greater than or equal, and
 * greater than a given key, returning null if there is no such key.</p>
 * 
 * <p>All mutating operations will throw UnsupportedOperationException. The map maintains
 * elements in sorted order according to their natural ordering or by a Comparator provided
 * at creation time.</p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
 *     1, "one", 2, "two", 3, "three", 4, "four"
 * );
 * 
 * System.out.println(map.floorEntry(3));    // 3=three
 * System.out.println(map.higherKey(2));     // 3
 * System.out.println(map.descendingMap());  // {4=four, 3=three, 2=two, 1=one}
 * }</pre>
 * </p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see ImmutableSortedMap
 * @see NavigableMap
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
     * Returns an empty ImmutableNavigableMap. This method always returns the same cached instance,
     * making it memory efficient for representing empty navigable maps.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<String, Integer> emptyMap = ImmutableNavigableMap.empty();
     * System.out.println(emptyMap.size()); // 0
     * }</pre>
     * </p>
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
     * The key must implement Comparable to determine its natural ordering.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.of("count", 42);
     * System.out.println(map.firstKey()); // "count"
     * }</pre>
     * </p>
     *
     * @param <K> the type of the key in the ImmutableNavigableMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     2, "two", 1, "one"
     * );
     * System.out.println(map); // {1=one, 2=two}
     * }</pre>
     * </p>
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k4 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v4 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k5 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v5 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k6 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v6 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k7 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v7 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k8 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v8 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k9 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v9 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableNavigableMap, must extend Comparable
     * @param <V> the type of the values in the ImmutableNavigableMap
     * @param k1 to k10 the keys to be included in the ImmutableNavigableMap
     * @param v1 to v10 the values to be associated with the keys
     * @return an ImmutableNavigableMap containing the provided key-value pairs
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
     * Returns an ImmutableNavigableMap containing the same mappings as the provided Map.
     * If the provided Map is already an instance of ImmutableNavigableMap, it is directly returned.
     * If the provided Map is {@code null} or empty, an empty ImmutableNavigableMap is returned.
     * Otherwise, a new ImmutableNavigableMap is created with the elements of the provided Map.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("b", 2);
     * map.put("a", 1);
     * ImmutableNavigableMap<String, Integer> immutable = ImmutableNavigableMap.copyOf(map);
     * System.out.println(immutable); // {a=1, b=2}
     * }</pre>
     * </p>
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param map the Map whose mappings are to be placed in the ImmutableNavigableMap
     * @return an ImmutableNavigableMap containing the same mappings as the provided Map
     */
    public static <K, V> ImmutableNavigableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        if (map instanceof ImmutableNavigableMap) {
            return (ImmutableNavigableMap<K, V>) map;
        } else if (N.isEmpty(map)) {
            return empty();
        } else if (map instanceof SortedMap sortedMap) {
            return new ImmutableNavigableMap<>(new TreeMap<>(sortedMap));
        } else {
            return new ImmutableNavigableMap<>(new TreeMap<>(map));
        }
    }

    /**
     * Returns an ImmutableNavigableMap that is backed by the provided NavigableMap. 
     * Changes to the specified NavigableMap will be reflected in the ImmutableNavigableMap.
     * If the provided NavigableMap is already an instance of ImmutableNavigableMap, it is directly returned.
     * If the NavigableMap is {@code null}, an empty ImmutableNavigableMap is returned.
     * Otherwise, returns a new ImmutableNavigableMap backed by the provided NavigableMap.
     * 
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the underlying
     * NavigableMap will be visible through the returned ImmutableNavigableMap, which violates the
     * immutability contract. Use {@link #copyOf(Map)} for a true immutable copy.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * NavigableMap<Integer, String> mutable = new TreeMap<>();
     * ImmutableNavigableMap<Integer, String> wrapped = ImmutableNavigableMap.wrap(mutable);
     * mutable.put(1, "one"); // This change is visible in wrapped!
     * }</pre>
     * </p>
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
     * Use {@link #wrap(NavigableMap)} for NavigableMap or {@link ImmutableSortedMap#wrap(SortedMap)} 
     * for regular SortedMaps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param sortedMap ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <K, V> ImmutableSortedMap<K, V> wrap(final SortedMap<? extends K, ? extends V> sortedMap) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a key-value mapping associated with the greatest key strictly less than the given key,
     * or {@code null} if there is no such key. The returned entry is immutable and does not support
     * the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.lowerEntry(3));  // 1=one
     * System.out.println(map.lowerEntry(4));  // 3=three
     * System.out.println(map.lowerEntry(1));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return an entry with the greatest key less than {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public ImmutableEntry<K, V> lowerEntry(final K key) {
        final Entry<K, V> lowerEntry = navigableMap.lowerEntry(key);

        return lowerEntry == null ? null : ImmutableEntry.copyOf(lowerEntry);
    }

    /**
     * Returns the greatest key strictly less than the given key, or {@code null} if there is no such key.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.lowerKey(3));  // 1
     * System.out.println(map.lowerKey(4));  // 3
     * System.out.println(map.lowerKey(1));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return the greatest key less than {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public K lowerKey(final K key) {
        return navigableMap.lowerKey(key);
    }

    /**
     * Returns a key-value mapping associated with the greatest key less than or equal to the given key,
     * or {@code null} if there is no such key. The returned entry is immutable and does not support
     * the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.floorEntry(3));  // 3=three
     * System.out.println(map.floorEntry(4));  // 3=three
     * System.out.println(map.floorEntry(0));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return an entry with the greatest key less than or equal to {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public ImmutableEntry<K, V> floorEntry(final K key) {
        final Entry<K, V> floorEntry = navigableMap.floorEntry(key);

        return floorEntry == null ? null : ImmutableEntry.copyOf(floorEntry);
    }

    /**
     * Returns the greatest key less than or equal to the given key, or {@code null} if there is no such key.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.floorKey(3));  // 3
     * System.out.println(map.floorKey(4));  // 3
     * System.out.println(map.floorKey(0));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return the greatest key less than or equal to {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public K floorKey(final K key) {
        return navigableMap.floorKey(key);
    }

    /**
     * Returns a key-value mapping associated with the least key greater than or equal to the given key,
     * or {@code null} if there is no such key. The returned entry is immutable and does not support
     * the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.ceilingEntry(3));  // 3=three
     * System.out.println(map.ceilingEntry(4));  // 5=five
     * System.out.println(map.ceilingEntry(6));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return an entry with the least key greater than or equal to {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public ImmutableEntry<K, V> ceilingEntry(final K key) {
        final Entry<K, V> ceilingEntry = navigableMap.ceilingEntry(key);

        return ceilingEntry == null ? null : ImmutableEntry.copyOf(ceilingEntry);
    }

    /**
     * Returns the least key greater than or equal to the given key, or {@code null} if there is no such key.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.ceilingKey(3));  // 3
     * System.out.println(map.ceilingKey(4));  // 5
     * System.out.println(map.ceilingKey(6));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return the least key greater than or equal to {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public K ceilingKey(final K key) {
        return navigableMap.ceilingKey(key);
    }

    /**
     * Returns a key-value mapping associated with the least key strictly greater than the given key,
     * or {@code null} if there is no such key. The returned entry is immutable and does not support
     * the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.higherEntry(3));  // 5=five
     * System.out.println(map.higherEntry(4));  // 5=five
     * System.out.println(map.higherEntry(5));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return an entry with the least key greater than {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public ImmutableEntry<K, V> higherEntry(final K key) {
        final Entry<K, V> higherEntry = navigableMap.higherEntry(key);

        return higherEntry == null ? null : ImmutableEntry.copyOf(higherEntry);
    }

    /**
     * Returns the least key strictly greater than the given key, or {@code null} if there is no such key.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * System.out.println(map.higherKey(3));  // 5
     * System.out.println(map.higherKey(4));  // 5
     * System.out.println(map.higherKey(5));  // null
     * }</pre>
     * </p>
     *
     * @param key the key
     * @return the least key greater than {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     *         or its comparator does not permit null keys
     */
    @Override
    public K higherKey(final K key) {
        return navigableMap.higherKey(key);
    }

    /**
     * Returns a key-value mapping associated with the least key in this map, or {@code null} if the map is empty.
     * The returned entry is immutable and does not support the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     3, "three", 1, "one", 2, "two"
     * );
     * System.out.println(map.firstEntry()); // 1=one
     * }</pre>
     * </p>
     *
     * @return an entry with the least key, or {@code null} if this map is empty
     */
    @Override
    public ImmutableEntry<K, V> firstEntry() {
        final Entry<K, V> firstEntry = navigableMap.firstEntry();

        return firstEntry == null ? null : ImmutableEntry.copyOf(firstEntry);
    }

    /**
     * Returns a key-value mapping associated with the greatest key in this map, or {@code null} if the map is empty.
     * The returned entry is immutable and does not support the {@code setValue} operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     3, "three", 1, "one", 2, "two"
     * );
     * System.out.println(map.lastEntry()); // 3=three
     * }</pre>
     * </p>
     *
     * @return an entry with the greatest key, or {@code null} if this map is empty
     */
    @Override
    public ImmutableEntry<K, V> lastEntry() {
        final Entry<K, V> lastEntry = navigableMap.lastEntry();

        return lastEntry == null ? null : ImmutableEntry.copyOf(lastEntry);
    }

    /**
     * This operation is not supported by ImmutableNavigableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableNavigableMap does not support modification operations
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollFirstEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableNavigableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableNavigableMap does not support modification operations
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollLastEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a reverse order view of the mappings contained in this map.
     * The descending map is backed by this map, so it remains immutable.
     * The returned map has an ordering equivalent to Collections.reverseOrder(comparator()).
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 2, "two", 3, "three"
     * );
     * ImmutableNavigableMap<Integer, String> descending = map.descendingMap();
     * System.out.println(descending); // {3=three, 2=two, 1=one}
     * }</pre>
     * </p>
     *
     * @return a reverse order view of this map
     */
    @Override
    public ImmutableNavigableMap<K, V> descendingMap() {
        return wrap(navigableMap.descendingMap());
    }

    /**
     * Returns an immutable navigable set view of the keys contained in this map.
     * The set's iterator returns the keys in ascending order.
     * The set is backed by the map, so changes to the map are reflected in the set.
     * However, since the map is immutable, the set is effectively immutable as well.
     *
     * <p>The returned set supports all of the navigable set operations, such as
     * {@code lower}, {@code floor}, {@code ceiling}, {@code higher}, etc., allowing
     * navigation operations on the keys without requiring individual map lookups.</p>
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * ImmutableNavigableSet<Integer> keySet = map.navigableKeySet();
     * System.out.println(keySet);                 // [1, 3, 5]
     * System.out.println(keySet.lower(3));        // 1
     * System.out.println(keySet.ceiling(2));      // 3
     * System.out.println(keySet.descendingSet()); // [5, 3, 1]
     * }</pre>
     * </p>
     *
     * @return an immutable navigable set view of the keys in this map
     */
    @Override
    public ImmutableNavigableSet<K> navigableKeySet() {
        return ImmutableNavigableSet.wrap(navigableMap.navigableKeySet());
    }

    /**
     * Returns an immutable navigable set view of the keys contained in this map in descending order.
     * The set's iterator returns the keys in descending order.
     * The set is backed by the map, so changes to the map are reflected in the set.
     * However, since the map is immutable, the set is effectively immutable as well.
     *
     * <p>The returned set supports all of the navigable set operations, providing
     * descending order navigation of the keys without requiring individual map lookups.</p>
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five"
     * );
     * ImmutableNavigableSet<Integer> descKeys = map.descendingKeySet();
     * System.out.println(descKeys);          // [5, 3, 1]
     * System.out.println(descKeys.first());  // 5
     * System.out.println(descKeys.last());   // 1
     * }</pre>
     * </p>
     *
     * @return an immutable navigable set view of the keys in this map in descending order
     */
    @Override
    public ImmutableNavigableSet<K> descendingKeySet() {
        return ImmutableNavigableSet.wrap(navigableMap.descendingKeySet());
    }

    /**
     * Returns a view of the portion of this map whose keys range from {@code fromKey} to
     * {@code toKey}. If {@code fromKey} and {@code toKey} are equal, the returned map is empty
     * unless {@code fromInclusive} and {@code toInclusive} are both true. The returned map is
     * backed by this map, so changes in the returned map are reflected in this map, and vice-versa.
     * The returned map supports all optional map operations that this map supports.
     * 
     * <p>The returned map will throw an {@code IllegalArgumentException} if the starting key is greater
     * than the ending key considering the order of this map's comparator.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five", 7, "seven", 9, "nine"
     * );
     * 
     * // inclusive-inclusive sub-map: [3, 7]
     * ImmutableNavigableMap<Integer, String> subMap1 = map.subMap(3, true, 7, true);
     * System.out.println(subMap1); // {3=three, 5=five, 7=seven}
     * 
     * // exclusive-exclusive sub-map: (3, 7)
     * ImmutableNavigableMap<Integer, String> subMap2 = map.subMap(3, false, 7, false);
     * System.out.println(subMap2); // {5=five}
     * }</pre>
     * </p>
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param fromInclusive {@code true} if the low endpoint is to be included in the returned view
     * @param toKey high endpoint of the keys in the returned map
     * @param toInclusive {@code true} if the high endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys range from {@code fromKey} to {@code toKey}
     * @throws ClassCastException if {@code fromKey} and {@code toKey} cannot be compared to one another
     *         using this map's comparator (or, if the map has no comparator, using natural ordering)
     *         permit null keys
     * @throws IllegalArgumentException if {@code fromKey} is greater than {@code toKey}
     */
    @Override
    public ImmutableNavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        return wrap(navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    /**
     * Returns a view of the portion of this map whose keys are less than (or equal to,
     * if {@code inclusive} is true) {@code toKey}. The returned map is backed by this map,
     * so changes in the returned map are reflected in this map, and vice-versa.
     * The returned map supports all optional map operations that this map supports.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five", 7, "seven"
     * );
     * 
     * // all entries with keys less than or equal to 5
     * ImmutableNavigableMap<Integer, String> headMap1 = map.headMap(5, true);
     * System.out.println(headMap1); // {1=one, 3=three, 5=five}
     * 
     * // all entries with keys less than 5
     * ImmutableNavigableMap<Integer, String> headMap2 = map.headMap(5, false);
     * System.out.println(headMap2); // {1=one, 3=three}
     * }</pre>
     * </p>
     *
     * @param toKey high endpoint of the keys in the returned map
     * @param inclusive {@code true} if the high endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys are less than (or equal to,
     *         if {@code inclusive} is true) {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible with this map's comparator
     *         (or, if the map has no comparator, if {@code toKey} does not implement {@link Comparable})
     */
    @Override
    public ImmutableNavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        return wrap(navigableMap.headMap(toKey, inclusive));
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than (or equal to,
     * if {@code inclusive} is true) {@code fromKey}. The returned map is backed by this map,
     * so changes in the returned map are reflected in this map, and vice-versa.
     * The returned map supports all optional map operations that this map supports.
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(
     *     1, "one", 3, "three", 5, "five", 7, "seven"
     * );
     *
     * // all entries with keys greater than or equal to 3
     * ImmutableNavigableMap<Integer, String> tailMap1 = map.tailMap(3, true);
     * System.out.println(tailMap1); // {3=three, 5=five, 7=seven}
     *
     * // all entries with keys greater than 3
     * ImmutableNavigableMap<Integer, String> tailMap2 = map.tailMap(3, false);
     * System.out.println(tailMap2); // {5=five, 7=seven}
     * }</pre>
     * </p>
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param inclusive {@code true} if the low endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys are greater than (or equal to,
     *         if {@code inclusive} is true) {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible with this map's comparator
     *         (or, if the map has no comparator, if {@code fromKey} does not implement {@link Comparable})
     */
    @Override
    public ImmutableNavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        return wrap(navigableMap.tailMap(fromKey, inclusive));
    }
}