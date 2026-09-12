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
 * An immutable implementation of the {@link SortedMap} interface.
 * This class extends {@link ImmutableMap} and maintains its entries in sorted order
 * according to the natural ordering of its keys or by a {@link Comparator} provided at map creation time.
 *
 * <p>The contents of an {@code ImmutableSortedMap} cannot be modified through its API.
 * All mutating operations ({@code put}, {@code remove}, {@code clear}, etc.) throw {@link UnsupportedOperationException}.</p>
 *
 * <p>An instance created by {@link #wrap(SortedMap)} reflects external changes to its backing map;
 * use {@link #copyOf(Map)} when an independent immutable value is required - it copies a wrapped view
 * rather than returning it unchanged.</p>
 *
 * <p><b>Note:</b> the {@code of(...)} factories accept any key type and throw {@link ClassCastException}
 * at run time if the keys are not mutually comparable, matching {@link #copyOf(Map)}. They deliberately
 * carry no {@code Comparable} bound, and one must not be added: a bound would make them inapplicable to a
 * non-comparable key, so the call would quietly resolve to the inherited
 * {@link ImmutableMap#of(Object, Object)} and hand back an unsorted map instead of failing.</p>
 *
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 * <li>{@link #empty()} - returns an empty immutable sorted map</li>
 * <li>{@link #of(Object, Object)} - creates maps with specific key-value pairs</li>
 * <li>{@link #copyOf(Map)} - creates a defensive copy from another map</li>
 * <li>{@link #wrap(SortedMap)} - wraps an existing sorted map (changes to the underlying map will be reflected)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
 *     "apple", 3,
 *     "banana", 2,
 *     "cherry", 5
 * );
 *
 * // Map is automatically sorted by keys: {apple=3, banana=2, cherry=5}
 * ImmutableSortedMap<String, Integer> subMap = map.subMap("banana", "cherry");
 * System.out.println(subMap);   // prints {banana=2}
 * }</pre>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see ImmutableMap
 * @see SortedMap
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
public class ImmutableSortedMap<K, V> extends ImmutableMap<K, V> implements SortedMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedMap EMPTY = new ImmutableSortedMap(N.emptySortedMap(), true);

    private final SortedMap<K, V> sortedMap;

    /**
     * Constructs a non-owning {@code ImmutableSortedMap} backed by the given sorted map.
     * The provided map is retained as live backing storage; external changes to it are
     * reflected in this read-only view. Use {@link #copyOf(Map)} for independent storage.
     *
     * @param sortedMap the sorted map to back this immutable map
     * @throws NullPointerException if {@code sortedMap} is {@code null}
     */
    ImmutableSortedMap(final SortedMap<? extends K, ? extends V> sortedMap) throws NullPointerException {
        this(sortedMap, false);
    }

    /**
     * Constructs an {@code ImmutableSortedMap} backed by the given sorted map.
     *
     * @param sortedMap the sorted map to back this immutable map
     * @param ownsBacking {@code true} only if no other modifiable reference to {@code sortedMap} survives
     *        this call; see {@link AbstractImmutableMap#ownsBacking}
     * @throws NullPointerException if {@code sortedMap} is {@code null}
     */
    ImmutableSortedMap(final SortedMap<? extends K, ? extends V> sortedMap, final boolean ownsBacking) throws NullPointerException {
        super(sortedMap, false, ownsBacking);
        this.sortedMap = (SortedMap<K, V>) sortedMap;
    }

    /**
     * Returns an empty ImmutableSortedMap. This method always returns the same cached instance,
     * making it memory efficient for representing empty sorted maps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> emptyMap = ImmutableSortedMap.empty();
     * System.out.println(emptyMap.size());   // prints 0
     * }</pre>
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
     * The key must implement {@link Comparable} to determine its natural ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> singleEntry = ImmutableSortedMap.of("count", 42);
     * System.out.println(singleEntry.firstKey());   // prints "count"
     * }</pre>
     *
     * @param <K> the type of the key in the ImmutableSortedMap; the key must be Comparable
     * @param <V> the type of the value in the ImmutableSortedMap
     * @param k1 the key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the key
     * @return an ImmutableSortedMap containing the provided key-value pair
     * @throws NullPointerException if {@code k1} is {@code null}
     * @throws ClassCastException if {@code k1} cannot be compared with itself in natural order
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     2, "two",
     *     1, "one"
     * );
     * System.out.println(map);   // prints {1=one, 2=two}
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     3, "three", 1, "one", 2, "two"
     * );
     * System.out.println(map);   // prints {1=one, 2=two, 3=three}
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3)
            throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     4, "four", 1, "one", 3, "three", 2, "two"
     * );
     * System.out.println(map);   // prints {1=one, 2=two, 3=three, 4=four}
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4)
            throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "banana", 2, "apple", 1, "cherry", 3, "date", 4, "elderberry", 5
     * );
     * System.out.println(map.firstKey());   // prints "apple"
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     3, "three", 1, "one", 4, "four", 2, "two", 6, "six", 5, "five"
     * );
     * System.out.println(map.lastKey());   // prints 6
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableSortedMap
     * @param v6 the value to be associated with the sixth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "g", 7, "a", 1, "f", 6, "b", 2, "e", 5, "c", 3, "d", 4
     * );
     * System.out.println(map);   // prints {a=1, b=2, c=3, d=4, e=5, f=6, g=7}
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableSortedMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableSortedMap
     * @param v7 the value to be associated with the seventh key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     8, "eight", 3, "three", 5, "five", 1, "one", 7, "seven", 2, "two", 6, "six", 4, "four"
     * );
     * System.out.println(map.firstKey());   // prints 1
     * System.out.println(map.lastKey());    // prints 8
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableSortedMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableSortedMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableSortedMap
     * @param v8 the value to be associated with the eighth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     1, "one", 2, "two", 3, "three", 4, "four", 5, "five",
     *     6, "six", 7, "seven", 8, "eight", 9, "nine"
     * );
     * System.out.println(map.size());   // prints 9
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableSortedMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableSortedMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableSortedMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableSortedMap
     * @param v9 the value to be associated with the ninth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9)
            throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the provided key-value pairs.
     * The keys must implement {@link Comparable} to determine their natural ordering.
     * If duplicate keys are provided, the last value for a key wins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     1, "one", 2, "two", 3, "three", 4, "four", 5, "five",
     *     6, "six", 7, "seven", 8, "eight", 9, "nine", 10, "ten"
     * );
     * System.out.println(map.lastKey());   // prints 10
     * }</pre>
     *
     * @param <K> the type of the keys in the ImmutableSortedMap; keys must be mutually comparable
     * @param <V> the type of the values in the ImmutableSortedMap
     * @param k1 the first key to be included in the ImmutableSortedMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableSortedMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableSortedMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableSortedMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableSortedMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableSortedMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableSortedMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableSortedMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableSortedMap
     * @param v9 the value to be associated with the ninth key
     * @param k10 the tenth key to be included in the ImmutableSortedMap
     * @param v10 the value to be associated with the tenth key
     * @return an ImmutableSortedMap containing the provided key-value pairs
     * @throws NullPointerException if any key is {@code null}
     * @throws ClassCastException if the keys are not mutually comparable
     */
    public static <K, V> ImmutableSortedMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10)
            throws NullPointerException, ClassCastException {
        final SortedMap<K, V> map = new TreeMap<>();

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

        return new ImmutableSortedMap<>(map, true);
    }

    /**
     * Returns an ImmutableSortedMap containing the same mappings as the provided Map.
     * If the provided Map is {@code null}, or is empty and not a {@link SortedMap}, an empty
     * ImmutableSortedMap is returned.
     * Otherwise, a new ImmutableSortedMap is created with the elements of the provided Map.
     *
     * <p>The copy is skipped only when {@code map} is an {@code ImmutableSortedMap} that already owns its
     * backing storage - that is, one produced by {@code of(...)}, {@code copyOf(...)} or {@link #empty()}.
     * An {@code ImmutableSortedMap} produced by {@link #wrap(SortedMap)} is a live view over storage its
     * creator may still modify, so it is copied like any other map.</p>
     *
     * <p><b>Note:</b> "independent" means independent of further <i>modification</i>, not of the source's
     * <i>memory</i>. This holds for every derived view of an owning map, not just a range:
     * {@code subMap}/{@code headMap}/{@code tailMap}/{@code reversed}, and {@code descendingMap} on the
     * navigable subtype, all own their backing storage too, so they are returned unchanged - and, like every
     * {@code SortedMap} sub-view, each keeps the whole parent map reachable. Wrap the view in a fresh map
     * ({@code copyOf(new TreeMap<>(view))}) when a small view of a large map must stop retaining it.</p>
     *
     * <p>If the source is a {@link SortedMap}, the returned map uses the same {@link Comparator}
     * (or natural ordering) as the source, even when the source is empty. Empty non-sorted sources
     * return the shared empty map. Otherwise, the entries are inserted into a new
     * {@link TreeMap} using the natural ordering of the keys.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> mutable = new HashMap<>();
     * mutable.put("b", 2);
     * mutable.put("a", 1);
     * ImmutableSortedMap<String, Integer> immutable = ImmutableSortedMap.copyOf(mutable);
     * mutable.put("c", 3);             // does not affect immutable
     * System.out.println(immutable);   // prints {a=1, b=2}
     * }</pre>
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param map the Map whose mappings are to be placed in the {@code ImmutableSortedMap}
     * @return an {@code ImmutableSortedMap} containing the same mappings as the provided map, or the same
     *         instance if it is already an {@code ImmutableSortedMap} that owns its backing storage. The
     *         comparator of a {@code SortedMap} source is retained even when the source is empty; a
     *         {@code null} or empty non-sorted source returns the shared empty instance.
     * @throws NullPointerException if the map contains a {@code null} key and natural ordering is used
     * @throws ClassCastException if the keys are not mutually comparable (when the source map is not a {@code SortedMap})
     * @see #wrap(SortedMap)
     */
    public static <K, V> ImmutableSortedMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws NullPointerException, ClassCastException {
        if (map instanceof ImmutableSortedMap && ((ImmutableSortedMap<K, V>) map).ownsBacking) {
            return (ImmutableSortedMap<K, V>) map;
        } else if (map instanceof SortedMap sortedMap) {
            return new ImmutableSortedMap<>(new TreeMap<>(sortedMap), true);
        } else if (N.isEmpty(map)) {
            return empty();
        } else {
            return new ImmutableSortedMap<>(new TreeMap<>(map), true);
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
     * immutability contract. Use {@link #copyOf(Map)} for a truly independent immutable copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortedMap<Integer, String> mutable = new TreeMap<>();
     * ImmutableSortedMap<Integer, String> wrapped = ImmutableSortedMap.wrap(mutable);
     * mutable.put(1, "one");   // this change is visible in wrapped!
     * }</pre>
     *
     * @param <K> the type of keys in the SortedMap
     * @param <V> the type of values in the SortedMap
     * @param sortedMap the SortedMap to be wrapped into an {@code ImmutableSortedMap}
     * @return an {@code ImmutableSortedMap} backed by the provided SortedMap, or the same instance if it is already an {@code ImmutableSortedMap}, or an empty instance if {@code sortedMap} is {@code null}
     * @see #copyOf(Map)
     */
    @Beta
    public static <K, V> ImmutableSortedMap<K, V> wrap(final SortedMap<? extends K, ? extends V> sortedMap) {
        if (sortedMap instanceof ImmutableSortedMap) {
            return (ImmutableSortedMap<K, V>) sortedMap;
        } else if (sortedMap == null) {
            return empty();
        } else {
            return new ImmutableSortedMap<>(sortedMap, false);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     * Use {@link #wrap(SortedMap)} for SortedMap or {@link ImmutableMap#wrap(Map)} for regular Maps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * ImmutableSortedMap.wrap(map);   // throws UnsupportedOperationException
     * }</pre>
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
     * This method is deprecated and will always throw an {@link UnsupportedOperationException}.
     *
     * <p>{@code ImmutableMap.builder()} is a static method and is therefore reachable through this
     * subclass's name, where {@code ImmutableSortedMap.builder().build()} would silently produce an
     * <i>unsorted</i> {@link ImmutableMap} in insertion order. This overload hides it so the mistake fails
     * loudly. Build the entries into a {@link java.util.TreeMap} and pass it to {@link #copyOf(Map)}, or
     * use one of the {@code of(...)} factories.</p>
     *
     * <p>The return type must stay {@code ImmutableMap.Builder} to legally hide the superclass method;
     * it never returns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap.builder();   // throws UnsupportedOperationException
     *
     * // instead:
     * SortedMap<String, Integer> m = new TreeMap<>();
     * m.put("b", 2);
     * m.put("a", 1);
     * ImmutableSortedMap<String, Integer> sorted = ImmutableSortedMap.copyOf(m);
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated use {@link #copyOf(Map)} over a {@link java.util.TreeMap}, or an {@code of(...)} factory.
     */
    @Deprecated
    public static <K, V> ImmutableMap.Builder<K, V> builder() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is deprecated and will always throw an {@link UnsupportedOperationException}.
     *
     * <p>See {@link #builder()}: the inherited {@code ImmutableMap.builder(Map)} would silently produce an
     * unsorted {@link ImmutableMap}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap.builder(new TreeMap<String, Integer>());   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param backedMap ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated use {@link #copyOf(Map)} over a {@link java.util.TreeMap}, or an {@code of(...)} factory.
     */
    @Deprecated
    public static <K, V> ImmutableMap.Builder<K, V> builder(final Map<K, V> backedMap) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the comparator used to order the keys in this map, or {@code null} if
     * this map uses the natural ordering of its keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> naturalOrder = ImmutableSortedMap.of("a", 1, "b", 2);
     * System.out.println(naturalOrder.comparator());   // prints null
     *
     * Comparator<String> reverseOrder = Comparator.reverseOrder();
     * SortedMap<String, Integer> customMap = new TreeMap<>(reverseOrder);
     * customMap.put("a", 1);
     * ImmutableSortedMap<String, Integer> customOrder = ImmutableSortedMap.wrap(customMap);
     * System.out.println(customOrder.comparator() == reverseOrder);   // prints true
     * }</pre>
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
     * so it is also immutable; any attempt to modify it throws {@link UnsupportedOperationException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     1, "one", 2, "two", 3, "three", 4, "four", 5, "five"
     * );
     * ImmutableSortedMap<Integer, String> subMap = map.subMap(2, 4);
     * System.out.println(subMap);   // prints {2=two, 3=three}
     * }</pre>
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey}, inclusive, to {@code toKey}, exclusive
     * @throws NullPointerException if an endpoint is null and the backing map rejects null endpoints
     * @throws ClassCastException if the endpoints cannot be compared with each other, or an endpoint cannot be compared with a backing range bound
     *         using the configured comparator or natural ordering
     * @throws IllegalArgumentException if {@code fromKey} is greater than {@code toKey}; or if this map itself has a restricted range, and {@code
     *         fromKey} or {@code toKey} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedMap<K, V> subMap(final K fromKey, final K toKey) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range view is a window onto this map's own backing storage, so it is exactly as stable
        // as this map is: an owning parent yields an owning view, a wrap()-backed parent a live one.
        return new ImmutableSortedMap<>(sortedMap.subMap(fromKey, toKey), ownsBacking);
    }

    /**
     * Returns a view of the portion of this map whose keys are strictly less than {@code toKey}.
     * The returned map is backed by this map, so it remains immutable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "a", 1, "b", 2, "c", 3, "d", 4
     * );
     * ImmutableSortedMap<String, Integer> headMap = map.headMap("c");
     * System.out.println(headMap);   // prints {a=1, b=2}
     * }</pre>
     *
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are strictly less than {@code toKey}
     * @throws NullPointerException if {@code toKey} is null and the backing map rejects null endpoints
     * @throws ClassCastException if the endpoint is incompatible with the ordering, or cannot be compared with a backing range bound
     * @throws IllegalArgumentException if this map itself has a restricted range, and {@code toKey} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedMap<K, V> headMap(final K toKey) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range view is a window onto this map's own backing storage, so it is exactly as stable
        // as this map is: an owning parent yields an owning view, a wrap()-backed parent a live one.
        return new ImmutableSortedMap<>(sortedMap.headMap(toKey), ownsBacking);
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than or equal to {@code fromKey}.
     * The returned map is backed by this map, so it remains immutable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     10, "ten", 20, "twenty", 30, "thirty", 40, "forty"
     * );
     * ImmutableSortedMap<Integer, String> tailMap = map.tailMap(25);
     * System.out.println(tailMap);   // prints {30=thirty, 40=forty}
     * }</pre>
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are greater than or equal to {@code fromKey}
     * @throws NullPointerException if {@code fromKey} is null and the backing map rejects null endpoints
     * @throws ClassCastException if the endpoint is incompatible with the ordering, or cannot be compared with a backing range bound
     * @throws IllegalArgumentException if this map itself has a restricted range, and {@code fromKey} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedMap<K, V> tailMap(final K fromKey) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range view is a window onto this map's own backing storage, so it is exactly as stable
        // as this map is: an owning parent yields an owning view, a wrap()-backed parent a live one.
        return new ImmutableSortedMap<>(sortedMap.tailMap(fromKey), ownsBacking);
    }

    /**
     * Returns an immutable view of this map with its mappings in reverse key order.
     * The returned map is backed by this map, so it remains immutable, and it has an ordering equivalent to
     * {@link java.util.Collections#reverseOrder(java.util.Comparator) Collections.reverseOrder(comparator())}.
     *
     * <p>This narrows the {@link java.util.SortedMap#reversed()} default, which would otherwise hand back a
     * plain JDK view that is neither an {@code ImmutableSortedMap} nor an {@link Immutable}, unlike the
     * reversed views of {@link ImmutableList}, {@link ImmutableNavigableSet} and {@link ImmutableNavigableMap}.</p>
     *
     * <p>A fresh view is returned on every call, so {@code map.reversed().reversed()} is {@code equals} to
     * {@code map} but is not the same instance - the rule {@link ImmutableNavigableMap#descendingMap()}
     * already follows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(1, "one", 2, "two", 3, "three");
     * ImmutableSortedMap<Integer, String> reversed = map.reversed();
     * System.out.println(reversed);   // prints {3=three, 2=two, 1=one}
     * }</pre>
     *
     * @return an immutable view of this map with its mappings in reverse key order
     */
    @Override
    public ImmutableSortedMap<K, V> reversed() {
        // A range/derived view is a window onto this map's own backing storage, so it is exactly as stable
        // as this map is: an owning parent yields an owning view, a wrap()-backed parent a live one.
        return new ImmutableSortedMap<>(sortedMap.reversed(), ownsBacking);
    }

    /**
     * Returns the first (lowest) key currently in this map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of(
     *     "banana", 2, "apple", 1, "cherry", 3
     * );
     * System.out.println(map.firstKey());   // prints "apple"
     * }</pre>
     *
     * @return the first (lowest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K firstKey() throws NoSuchElementException {
        return sortedMap.firstKey();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    // Overrides SortedMap.pollFirstEntry(), whose default returns null on an EMPTY map instead of
    // throwing - the same silent no-op that AbstractImmutableMap.replaceAll() is overridden to block.
    // ImmutableNavigableMap already blocks it unconditionally; this brings ImmutableSortedMap in line.
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollFirstEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    // See pollFirstEntry(): the inherited SortedMap default returns null on an empty map.
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public Map.Entry<K, V> pollLastEntry() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a map and the two maps represent
     * the same mappings, as defined by the {@link java.util.Map#equals(Object)} contract.
     *
     * @param obj the object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }

    /**
     * Returns the hash code value for this map.
     * The hash code is defined as the sum of the hash codes of each entry in the map,
     * as defined by the {@link java.util.Map#hashCode()} contract.
     *
     * @return the hash code value for this map
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns the last (highest) key currently in this map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedMap<Integer, String> map = ImmutableSortedMap.of(
     *     3, "three", 1, "one", 4, "four", 1, "uno", 5, "five"
     * );
     * System.out.println(map.lastKey());   // prints 5
     * }</pre>
     *
     * @return the last (highest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K lastKey() throws NoSuchElementException {
        return sortedMap.lastKey();
    }
}
