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
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import com.landawn.abacus.annotation.Beta;

/**
 * An immutable, thread-safe implementation of the SortedMap interface.
 * This class extends ImmutableMap and maintains its entries in sorted order
 * according to the natural ordering of its keys or by a Comparator provided at map creation time.
 * 
 * <p>Once created, the contents of an ImmutableSortedMap cannot be modified.
 * All mutating operations (put, remove, clear, etc.) will throw UnsupportedOperationException.</p>
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 * <li>{@link #empty()} - returns an empty immutable sorted map</li>
 * <li>{@link #of(...)} - creates maps with specific key-value pairs</li>
 * <li>{@link #copyOf(SortedMap)} - creates a defensive copy from another sorted map</li>
 * <li>{@link #wrap(SortedMap)} - wraps an existing sorted map (changes to the underlying map will be reflected)</li>
 * </ul>
 * </p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
 *     "apple", 3,
 *     "banana", 2,
 *     "cherry", 5
 * );
 * 
 * // Map is automatically sorted by keys: {apple=3, banana=2, cherry=5}
 * ImmutableSortedMap<String, Integer> subMap = map.subMap("banana", "cherry");
 * System.out.println(subMap); // {banana=2}
 * }</pre>
 * </p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see ImmutableMap
 * @see SortedMap
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
     * Returns an empty ImmutableSortedMap. This method always returns the same cached instance,
     * making it memory efficient for representing empty sorted maps.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> emptyMap = ImmutableSortedMap.empty();
     * System.out.println(emptyMap.size()); // 0
     * }</pre>
     * </p>
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
     * The key must implement Comparable to determine its natural ordering.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> singleEntry = ImmutableSortedMap.of("count", 42);
     * System.out.println(singleEntry.firstKey()); // "count"
     * }</pre>
     * </p>
     *
     * @param <K> the type of the key in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     2, "two",
     *     1, "one"
     * );
     * System.out.println(map); // {1=one, 2=two}
     * }</pre>
     * </p>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * The keys must implement Comparable to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * @param <K> the type of the keys in the ImmutableSortedMap, must extend Comparable
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
     * Returns an ImmutableSortedMap containing the same mappings as the provided Map.
     * If the provided Map is already an instance of ImmutableSortedMap, it is directly returned.
     * If the provided Map is {@code null} or empty, an empty ImmutableSortedMap is returned.
     * Otherwise, a new ImmutableSortedMap is created with the elements of the provided Map.
     * 
     * <p>The returned map will maintain the same ordering as the source map.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> mutable = new HashMap<>();
     * mutable.put("b", 2);
     * mutable.put("a", 1);
     * ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.copyOf(mutable);
     * mutable.put("c", 3); // Does not affect immutable
     * System.out.println(immutable); // {a=1, b=2}
     * }</pre>
     * </p>
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param map the Map whose mappings are to be placed in the ImmutableSortedMap
     * @return an ImmutableSortedMap containing the same mappings as the provided Map
     */
    public static <K, V> ImmutableSortedMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        if (map instanceof ImmutableSortedMap) {
            return (ImmutableSortedMap<K, V>) map;
        } else if (N.isEmpty(map)) {
            return empty();
        } else if (map instanceof SortedMap sortedMap) {
            return new ImmutableSortedMap<>(new TreeMap<>(sortedMap));
        } else {
            return new ImmutableSortedMap<>(new TreeMap<>(map));
        }
    }

    /**
     * Wraps the provided SortedMap into an ImmutableSortedMap. Changes to the specified SortedMap 
     * will be reflected in the ImmutableSortedMap.
     * If the provided SortedMap is already an instance of ImmutableSortedMap, it is directly returned.
     * If the SortedMap is {@code null}, an empty ImmutableSortedMap is returned.
     * Otherwise, returns a new ImmutableSortedMap backed by the provided SortedMap.
     * 
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the underlying
     * SortedMap will be visible through the returned ImmutableSortedMap, which violates the
     * immutability contract. Use {@link #copyOf(SortedMap)} for a true immutable copy.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * SortedMap<Integer, String> mutable = new TreeMap<>();
     * ImmutableSortedMap<Integer, String> wrapped = ImmutableSortedMap.wrap(mutable);
     * mutable.put(1, "one"); // This change is visible in wrapped!
     * }</pre>
     * </p>
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
     * Use {@link #wrap(SortedMap)} for SortedMap or {@link ImmutableMap#wrap(Map)} for regular Maps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the comparator used to order the keys in this map, or {@code null} if
     * this map uses the natural ordering of its keys.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> naturalOrder = ImmutableSortedMap.of("a", 1, "b", 2);
     * System.out.println(naturalOrder.comparator()); // null
     * 
     * Comparator<String> reverseOrder = Comparator.reverseOrder();
     * SortedMap<String, Integer> customMap = new TreeMap<>(reverseOrder);
     * customMap.put("a", 1);
     * ImmutableSortedMap<String, Integer> customOrder = ImmutableSortedMap.wrap(customMap);
     * System.out.println(customOrder.comparator()); // ReverseComparator
     * }</pre>
     * </p>
     *
     * @return the comparator used to order the keys in this map, or {@code null}
     *         if this map uses the natural ordering of its keys
     */
    @Override
    public Comparator<? super K> comparator() {
        return sortedMap.comparator();
    }

    /**
     * Returns a view of the portion of this map whose keys range from {@code fromKey},
     * inclusive, to {@code toKey}, exclusive. The returned map is backed by this map,
     * so it remains immutable.
     * 
     * <p>The returned map will throw an {@code IllegalArgumentException} on an attempt to
     * insert a key outside its range.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     1, "one", 2, "two", 3, "three", 4, "four", 5, "five"
     * );
     * ImmutableSortedMap<Integer, String> subMap = map.subMap(2, 4);
     * System.out.println(subMap); // {2=two, 3=three}
     * }</pre>
     * </p>
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey}, inclusive, to {@code toKey}, exclusive
     * @throws ClassCastException if {@code fromKey} and {@code toKey}
     *         cannot be compared to one another using this map's comparator
     *         is null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException if {@code fromKey} is greater than
     *         {@code toKey}; or if this map itself has a restricted range,
     *         and {@code fromKey} or {@code toKey} lies outside the bounds of the range
     */
    @Override
    public ImmutableSortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return wrap(sortedMap.subMap(fromKey, toKey));
    }

    /**
     * Returns a view of the portion of this map whose keys are strictly less than {@code toKey}.
     * The returned map is backed by this map, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "a", 1, "b", 2, "c", 3, "d", 4
     * );
     * ImmutableSortedMap<String, Integer> headMap = map.headMap("c");
     * System.out.println(headMap); // {a=1, b=2}
     * }</pre>
     * </p>
     *
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are strictly less than {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible with
     *         this map's comparator
     *         natural ordering, or its comparator does not permit null keys
     * @throws IllegalArgumentException if this map itself has a restricted range,
     *         and {@code toKey} lies outside the bounds of the range
     */
    @Override
    public ImmutableSortedMap<K, V> headMap(final K toKey) {
        return wrap(sortedMap.headMap(toKey));
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than or equal to {@code fromKey}.
     * The returned map is backed by this map, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     10, "ten", 20, "twenty", 30, "thirty", 40, "forty"
     * );
     * ImmutableSortedMap<Integer, String> tailMap = map.tailMap(25);
     * System.out.println(tailMap); // {30=thirty, 40=forty}
     * }</pre>
     * </p>
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are greater than or equal to {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible with
     *         this map's comparator
     *         natural ordering, or its comparator does not permit null keys
     * @throws IllegalArgumentException if this map itself has a restricted range,
     *         and {@code fromKey} lies outside the bounds of the range
     */
    @Override
    public ImmutableSortedMap<K, V> tailMap(final K fromKey) {
        return wrap(sortedMap.tailMap(fromKey));
    }

    /**
     * Returns the first (lowest) key currently in this map.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "banana", 2, "apple", 1, "cherry", 3
     * );
     * System.out.println(map.firstKey()); // "apple"
     * }</pre>
     * </p>
     *
     * @return the first (lowest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K firstKey() {
        return sortedMap.firstKey();
    }

    /**
     * Returns the last (highest) key currently in this map.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     3, "three", 1, "one", 4, "four", 1, "uno", 5, "five"
     * );
     * System.out.println(map.lastKey()); // 5
     * }</pre>
     * </p>
     *
     * @return the last (highest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K lastKey() {
        return sortedMap.lastKey();
    }
}