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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * An immutable bidirectional map ({@link BiMap}) implementation.
 * A BiMap is a map that preserves the uniqueness of both its keys and values,
 * allowing efficient lookups in both directions (key-to-value and value-to-key).
 *
 * <p>Once created, the logical contents of an {@code ImmutableBiMap} cannot be
 * modified through its API. All mutating operations inherited from {@link Map}
 * will throw {@link UnsupportedOperationException}.</p>
 *
 * <p>This class provides several static factory methods for creating instances:</p>
 * <ul>
 *   <li>{@link #empty()} – returns an empty {@code ImmutableBiMap}</li>
 *   <li>{@link #of(Object, Object)} and its overloads – create small
 *       {@code ImmutableBiMap} instances with a fixed number of entries</li>
 *   <li>{@link #copyOf(BiMap)} / {@link #copyOf(Map)} – create a defensive copy from another
 *       {@code BiMap} or from any {@code Map} (including another {@code ImmutableBiMap})</li>
 *   <li>{@link #wrap(BiMap)} – wraps an existing {@code BiMap} (changes to the
 *       underlying map will be reflected in the view)</li>
 *   <li>{@link #builder()} – provides a builder for constructing maps incrementally</li>
 * </ul>
 *
 * <p>Unlike regular maps, {@code BiMap}s enforce that values are unique in
 * addition to keys. The {@code of(...)} factory methods reject {@code null} keys and
 * values and reject a value that is already bound to a different key, throwing
 * {@link IllegalArgumentException}. Because the values are unique, {@link #values()} returns an
 * {@link ImmutableSet} rather than a plain {@link java.util.Collection}.</p>
 *
 * <p>The {@code of(...)} factories preserve the order in which the entries were supplied:
 * {@link #keySet()}, {@link #values()} and {@link #entrySet()} all iterate in that order, matching
 * {@link ImmutableMap#of(Object, Object)}. The order of a {@link #copyOf(Map)} result instead follows
 * that method's own (best-effort) rule, and a {@link #wrap(BiMap)} view iterates in whatever order the
 * wrapped {@code BiMap}'s backing maps use.</p>
 *
 * <p>Instances created by {@link #wrap(BiMap)} or by a {@link Builder} over a caller-supplied
 * {@code BiMap} are read-only <i>views</i>: they reflect external changes to the backing map and have
 * that map's thread-safety characteristics. {@link #copyOf(Map)} always returns an independent value, so
 * it turns a view into a stable one.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
 *     "one", 1,
 *     "two", 2,
 *     "three", 3
 * );
 *
 * // Forward lookup
 * Integer value = biMap.get("two");   // returns 2
 *
 * // Reverse lookup
 * String key = biMap.getByValue(2);   // returns "two"
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see BiMap
 * @see ImmutableMap
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
public final class ImmutableBiMap<K, V> extends AbstractImmutableMap<K, V> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>(), true);

    /**
     * The backing {@link BiMap}, kept unwrapped so that the reverse lookups ({@link #getByValue(Object)},
     * {@link #inverse()}) and the {@code Set}-typed {@link #values()} can be served directly. All the
     * {@code Map} views come from the superclass's unmodifiable wrapper instead, so this reference never
     * escapes to a caller.
     */
    private final BiMap<K, V> biMap;

    /** Lazily created by {@link #inverse()}; {@code volatile} for safe publication under double-checked locking. */
    private volatile ImmutableBiMap<V, K> invertedView;

    /**
     * Constructs a non-owning {@code ImmutableBiMap} backed by the provided {@link BiMap}.
     * This constructor is marked as {@link Internal} and is not intended to be
     * used directly by library consumers. Prefer static factory methods such as
     * {@link #of(Object, Object)}, {@link #copyOf(Map)} or {@link #wrap(BiMap)}.
     *
     * @param map the {@code BiMap} whose mappings back this {@code ImmutableBiMap}
     * @throws NullPointerException if {@code map} is {@code null}
     */
    @Internal
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map) throws NullPointerException {
        this(map, false);
    }

    /**
     * Constructs an {@code ImmutableBiMap} backed by the given BiMap.
     *
     * @param map the BiMap to back this immutable map
     * @param ownsBacking {@code true} only if no other modifiable reference to {@code map} survives this
     *        call; see {@link AbstractImmutableMap#ownsBacking}.
     * @throws NullPointerException if {@code map} is {@code null}
     */
    @Internal
    @SuppressWarnings("unchecked")
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map, final boolean ownsBacking) throws NullPointerException {
        super(map, false, ownsBacking);
        biMap = (BiMap<K, V>) map;
    }

    /**
     * Returns a shared empty {@code ImmutableBiMap}. This method always returns the
     * same cached instance, making it memory-efficient for representing empty
     * bidirectional maps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
     * System.out.println(empty.size());   // prints 0
     * }</pre>
     *
     * @param <K> the type of keys in the returned map
     * @param <V> the type of values in the returned map
     * @return a shared empty {@code ImmutableBiMap} instance
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * Creates the empty, insertion-ordered {@link BiMap} that the {@code of(...)} factories fill.
     *
     * <p>{@link BiMap#of(Object, Object)} would be the obvious delegate, but it builds {@link java.util.HashMap}-
     * backed storage, so the resulting map iterated in hash order while the sibling
     * {@link ImmutableMap#of(Object, Object)} guarantees insertion order. Both backing maps have to be
     * insertion-ordered for {@link #keySet()}, {@link #values()} and {@link #entrySet()} to line up with the
     * order the caller supplied.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param pairCount the number of key-value pairs the caller is about to insert
     * @return a new, empty, insertion-ordered {@code BiMap}
     */
    private static <K, V> BiMap<K, V> orderedBiMap(final int pairCount) {
        return new BiMap<>(() -> N.newLinkedHashMap(pairCount), () -> N.newLinkedHashMap(pairCount));
    }

    /**
     * Returns an {@code ImmutableBiMap} containing a single key-value mapping.
     * The returned map is immutable and has a size of {@code 1}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> single = ImmutableBiMap.of("one", 1);
     * System.out.println(single.get("one"));      // prints 1
     * System.out.println(single.getByValue(1));   // prints "one"
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param k1 the key to be included in the map
     * @param v1 the value to be associated with {@code k1}
     * @return an {@code ImmutableBiMap} containing the provided key-value pair
     * @throws IllegalArgumentException if {@code k1} or {@code v1} is {@code null}.
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(1);
        biMap.put(k1, v1);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to two key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
     *     "first", 1,
     *     "second", 2
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(2);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to three key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(3);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to four key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4)
            throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(4);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to five key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4, "five", 5
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(5);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to six key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @param k6 the sixth key
     * @param v6 the value associated with {@code k6}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(6);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        biMap.put(k6, v6);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to seven key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @param k6 the sixth key
     * @param v6 the value associated with {@code k6}
     * @param k7 the seventh key
     * @param v7 the value associated with {@code k7}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(7);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        biMap.put(k6, v6);
        biMap.put(k7, v7);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to eight key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4,
     *     "five", 5, "six", 6, "seven", 7, "eight", 8
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @param k6 the sixth key
     * @param v6 the value associated with {@code k6}
     * @param k7 the seventh key
     * @param v7 the value associated with {@code k7}
     * @param k8 the eighth key
     * @param v8 the value associated with {@code k8}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(8);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        biMap.put(k6, v6);
        biMap.put(k7, v7);
        biMap.put(k8, v8);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to nine key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4,
     *     "five", 5, "six", 6, "seven", 7, "eight", 8, "nine", 9
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @param k6 the sixth key
     * @param v6 the value associated with {@code k6}
     * @param k7 the seventh key
     * @param v7 the value associated with {@code k7}
     * @param k8 the eighth key
     * @param v8 the value associated with {@code k8}
     * @param k9 the ninth key
     * @param v9 the value associated with {@code k9}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(9);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        biMap.put(k6, v6);
        biMap.put(k7, v7);
        biMap.put(k8, v8);
        biMap.put(k9, v9);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing up to ten key-value mappings.
     * Values must be unique, as required by {@link BiMap}. Keys need not be: a repeated key is not
     * an error, the last value supplied for it wins, and the resulting map is correspondingly smaller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4, "five", 5,
     *     "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10
     * );
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key
     * @param v1 the value associated with {@code k1}
     * @param k2 the second key
     * @param v2 the value associated with {@code k2}
     * @param k3 the third key
     * @param v3 the value associated with {@code k3}
     * @param k4 the fourth key
     * @param v4 the value associated with {@code k4}
     * @param k5 the fifth key
     * @param v5 the value associated with {@code k5}
     * @param k6 the sixth key
     * @param v6 the value associated with {@code k6}
     * @param k7 the seventh key
     * @param v7 the value associated with {@code k7}
     * @param k8 the eighth key
     * @param v8 the value associated with {@code k8}
     * @param k9 the ninth key
     * @param v9 the value associated with {@code k9}
     * @param k10 the tenth key
     * @param v10 the value associated with {@code k10}
     * @return an {@code ImmutableBiMap} containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10)
            throws IllegalArgumentException {
        final BiMap<K, V> biMap = orderedBiMap(10);
        biMap.put(k1, v1);
        biMap.put(k2, v2);
        biMap.put(k3, v3);
        biMap.put(k4, v4);
        biMap.put(k5, v5);
        biMap.put(k6, v6);
        biMap.put(k7, v7);
        biMap.put(k8, v8);
        biMap.put(k9, v9);
        biMap.put(k10, v10);
        return new ImmutableBiMap<>(biMap, true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing the same mappings as the
     * provided {@link BiMap}.
     *
     * <p>If the provided {@code BiMap} is {@code null} or empty, this method
     * returns {@link #empty()}. Otherwise, it creates a new {@code ImmutableBiMap}
     * backed by a defensive copy of the entries, so subsequent modifications to
     * the original {@code BiMap} do not affect the returned instance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> mutable = new BiMap<>();
     * mutable.put("one", 1);
     * mutable.put("two", 2);
     *
     * ImmutableBiMap<String, Integer> immutable = ImmutableBiMap.copyOf(mutable);
     * mutable.put("three", 3);   // does not affect 'immutable'
     * }</pre>
     *
     * @param <K> the type of keys in the {@code BiMap}
     * @param <V> the type of values in the {@code BiMap}
     * @param map the {@code BiMap} whose mappings are to be copied; may be {@code null}
     * @return an {@code ImmutableBiMap} containing the same mappings as {@code map},
     *         or {@link #empty()} if {@code map} is {@code null} or empty
     * @throws IllegalArgumentException if {@code map}'s own map suppliers do not return a new, empty,
     *         distinct map on each call, because the defensive copy is made by {@link BiMap#copy()}
     * @see #copyOf(Map)
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(final BiMap<? extends K, ? extends V> map) throws IllegalArgumentException {
        if (N.isEmpty(map)) {
            return empty();
        }

        // BiMap.copy() preserves the source's key/value map suppliers, which BiMap.copyOf(Map) cannot see.
        return new ImmutableBiMap<>(map.copy(), true);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing the same mappings as the provided {@link Map}.
     *
     * <p>If the provided map is {@code null} or empty, this method returns {@link #empty()}. Otherwise it
     * creates a new {@code ImmutableBiMap} backed by a defensive copy of the entries, so subsequent
     * modifications to the source do not affect the returned instance. This is the overload to use to take
     * an independent snapshot of an existing {@code ImmutableBiMap} - including one created by
     * {@link #wrap(BiMap)}, whose contents can still change underneath it.</p>
     *
     * <p>The copy is skipped only when {@code map} is an {@code ImmutableBiMap} that already owns its
     * backing storage - one produced by {@code of(...)}, {@code copyOf(...)}, {@link #empty()} or the no-arg
     * {@link #builder()}, or the {@link #inverse()} of such a map. An {@code ImmutableBiMap} produced by
     * {@link #wrap(BiMap)} or by a {@link Builder} over a caller-supplied {@code BiMap} is a live view over
     * storage its creator may still modify, so it is copied like any other map.</p>
     *
     * <p>The map's values must be unique, as required by {@link BiMap}; the entries are inserted in the
     * source's iteration order, so which entry is reported as the offender for a duplicate value depends on
     * that order.</p>
     *
     * <p><b>The result's own iteration order is best-effort, not guaranteed.</b> An {@code ImmutableBiMap}
     * source is reproduced exactly, through its own backing-map suppliers. Any other source is copied by
     * {@link BiMap#copyOf(Map)}, which mirrors the source's runtime map class where it can - so a
     * {@link java.util.LinkedHashMap} or {@link java.util.SortedMap} source does keep its order - and falls
     * back to a {@link java.util.HashMap} where it cannot, as for
     * {@code Collections.unmodifiableMap(aLinkedHashMap)}. This differs from {@link ImmutableMap#copyOf(Map)},
     * which always preserves the source's entry order. Build the entries into a {@code BiMap} whose backing
     * maps you chose and call {@link #wrap(BiMap)} (or {@link #copyOf(BiMap)}) when a particular order
     * matters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> mutable = new BiMap<>();
     * mutable.put("one", 1);
     *
     * ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(mutable);
     * ImmutableBiMap<String, Integer> snapshot = ImmutableBiMap.copyOf(view);
     *
     * mutable.put("two", 2);   // visible through view, but NOT through snapshot
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose mappings are to be copied; may be {@code null}
     * @return the same instance if {@code map} is already an {@code ImmutableBiMap} that owns its backing
     *         storage; {@link #empty()} if {@code map} is {@code null} or empty; otherwise a new
     *         {@code ImmutableBiMap} containing a defensive copy of the mappings
     * @throws IllegalArgumentException if any key or value in {@code map} is {@code null}, or if a value is
     *         bound to more than one key, or - when {@code map} is a {@code BiMap}-backed source that has to
     *         be copied - if that source's map suppliers do not return a new, empty, distinct map on each
     *         call, because the copy is then made by {@link BiMap#copy()}.
     * @see #copyOf(BiMap)
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws IllegalArgumentException {
        if (map instanceof ImmutableBiMap && ((ImmutableBiMap<K, V>) map).ownsBacking) {
            return (ImmutableBiMap<K, V>) map;
        } else if (N.isEmpty(map)) {
            return empty();
        } else if (map instanceof ImmutableBiMap) {
            // Copy through the source's own BiMap so that its key/value map suppliers - and therefore its
            // ordering and its key/value comparison rules - are reproduced. Rebuilding through BiMap.copyOf
            // would route an identity- or comparator-keyed source through a plain HashMap instead.
            return new ImmutableBiMap<>(((ImmutableBiMap<K, V>) map).biMap.copy(), true);
        } else {
            return new ImmutableBiMap<>(BiMap.copyOf(map), true);
        }
    }

    /**
     * Wraps the provided {@link BiMap} in an {@code ImmutableBiMap} without copying
     * its entries.
     *
     * <p>The returned {@code ImmutableBiMap} is backed directly by the given
     * {@code BiMap}: subsequent modifications to the original {@code BiMap} are
     * visible through this view. However, the {@code ImmutableBiMap} itself cannot
     * be modified via its API (its mutating methods throw
     * {@link UnsupportedOperationException}).</p>
     *
     * <p>If the provided {@code BiMap} is {@code null}, this method returns
     * {@link #empty()}.</p>
     *
     * <p>The returned object is a read-only view, not a deeply immutable snapshot. Use
     * {@link #copyOf(BiMap)} when subsequent changes to the source must not be visible.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> mutable = new BiMap<>();
     * mutable.put("one", 1);
     *
     * ImmutableBiMap<String, Integer> wrapped = ImmutableBiMap.wrap(mutable);
     * mutable.put("two", 2);   // this WILL be visible through 'wrapped'
     * }</pre>
     *
     * @param <K> the type of keys in the {@code BiMap}
     * @param <V> the type of values in the {@code BiMap}
     * @param map the {@code BiMap} to wrap; may be {@code null}
     * @return an {@code ImmutableBiMap} view backed by {@code map},
     *         or {@link #empty()} if {@code map} is {@code null}
     */
    @Beta
    public static <K, V> ImmutableBiMap<K, V> wrap(final BiMap<? extends K, ? extends V> map) {
        if (map == null) {
            return empty();
        }

        return new ImmutableBiMap<>(map);
    }

    /**
     * Returns the key to which the specified value is mapped in this {@code BiMap},
     * or {@code null} if this map contains no mapping for the value.
     *
     * <p>This is the reverse-lookup operation that makes {@code BiMap} bidirectional.
     * In a typical implementation, its performance is similar to that of
     * {@link #get(Object)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
     *     "one", 1,
     *     "two", 2
     * );
     *
     * String key = biMap.getByValue(2);        // returns "two"
     * String notFound = biMap.getByValue(3);   // returns null
     * }</pre>
     *
     * @param value the value whose associated key is to be returned
     * @return the key to which the specified value is mapped, or {@code null}
     *         if this map contains no mapping for the value
     * @throws NullPointerException if {@code value} is {@code null} and the backing {@code BiMap}'s
     *         value-to-key map does not permit {@code null} keys (optional). The {@code of(...)} factories
     *         and {@link #empty()} always build one that permits it; {@link #copyOf(Map)} instead derives
     *         the value map from the source, so a {@link java.util.Hashtable} or
     *         {@link java.util.concurrent.ConcurrentHashMap} source - or a {@code BiMap} or
     *         {@code ImmutableBiMap} source whose own value map rejects {@code null} keys - produces one
     *         that does not
     * @throws ClassCastException if {@code value} has a type that the reverse backing map cannot query or compare
     */
    public K getByValue(final Object value) throws NullPointerException, ClassCastException {
        return biMap.getByValue(value);
    }

    /**
     * Returns an immutable {@link java.util.Set Set} view of the keys contained in this map.
     *
     * <p>Changes made directly to an externally owned backing {@code BiMap} are reflected in the returned
     * view. Attempts to modify it throw {@link UnsupportedOperationException}. The iteration order matches
     * that of {@link #values()} and {@link #entrySet()}, entry for entry.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
     * ImmutableSet<String> keys = biMap.keySet();
     * System.out.println(keys.contains("one"));   // prints true
     * }</pre>
     *
     * @return an immutable set view of the keys contained in this map
     */
    @Override
    public ImmutableSet<K> keySet() {
        // The superclass would hand back Collections.unmodifiableMap(biMap).keySet(), re-wrapping a view that
        // BiMap already publishes as an ImmutableSet. Returning the BiMap's own view drops that redundant
        // layer and narrows the static type to match values().
        return biMap.keySet();
    }

    /**
     * Returns an immutable {@link java.util.Set Set} view of the mappings contained in this map.
     *
     * <p>Changes made directly to an externally owned backing {@code BiMap} are reflected in the returned
     * view, but neither the set nor the entries it yields can be modified: the iterator returns
     * {@link ImmutableEntry} snapshots, whose {@code setValue} throws
     * {@link UnsupportedOperationException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
     * for (Map.Entry<String, Integer> entry : biMap.entrySet()) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @return an immutable set view of the mappings contained in this map
     */
    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        // As in keySet(): BiMap.entrySet() is already an ImmutableSet of immutable entries, so the
        // superclass's UnmodifiableEntrySet layer would only add a second wrapper around each entry.
        return biMap.entrySet();
    }

    /**
     * Returns an immutable {@link java.util.Set Set} view of the values contained in this map.
     *
     * <p>Unlike a general {@link Map}, a {@link BiMap} keeps its values unique, so the value view is a
     * {@code Set} rather than a plain {@code Collection}: it compares equal to any other {@code Set} holding
     * the same values. Membership-test cost depends on the backing map implementation. Changes made directly
     * to an externally owned backing {@code BiMap} are reflected in the returned view.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
     * ImmutableSet<Integer> values = biMap.values();
     * System.out.println(values.contains(2));                     // prints true
     * System.out.println(values.equals(Set.of(1, 2)));            // prints true
     * }</pre>
     *
     * @return an immutable set view of the values contained in this map
     */
    @Override
    public ImmutableSet<V> values() {
        // AbstractImmutableMap.values() would return Collections.unmodifiableMap(biMap).values(), an
        // UnmodifiableCollection: not a Set, and (like every JDK map value view) equal only by identity.
        // BiMap.values() is already an ImmutableSet, so it can be handed out directly.
        return biMap.values();
    }

    /**
     * Returns an inverse view of this {@code ImmutableBiMap}, in which each value
     * of this map becomes a key, and each key becomes the corresponding value.
     *
     * <p>The noun <i>inverse</i> denotes this shared, bidirectional view. Copy-producing key/value
     * transformations are instead named <i>invert</i>; see {@link Maps#invert(Map)} and
     * {@link Multimap#invert(java.util.function.IntFunction)}.</p>
     *
     * <p>The returned map is also an {@code ImmutableBiMap} and is backed by the
     * inverse view of the same underlying {@code BiMap} as this instance, so it
     * shares the same storage and requires no copying. Calling {@code inverse()}
     * multiple times returns the same cached instance, and calling {@code inverse()} on
     * that result returns this instance. {@link #empty()} is its own inverse.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2);
     * ImmutableBiMap<Integer, String> inverse = map.inverse();
     * String key = inverse.get(1);   // returns "one"
     * }</pre>
     *
     * @return an {@code ImmutableBiMap} view where keys and values are swapped
     */
    public ImmutableBiMap<V, K> inverse() {
        if (this == EMPTY) {
            // The shared empty instance can never gain an entry, so it is its own inverse. Special-cased only
            // for EMPTY: an empty instance obtained from wrap(...) is a view whose backing BiMap can still
            // grow, and returning it as its own inverse would then be wrong.
            return empty();
        }

        ImmutableBiMap<V, K> result = invertedView;

        if (result == null) {
            synchronized (this) {
                result = invertedView;

                if (result == null) {
                    // The inverse is a window onto this map's own backing storage, so it is exactly as stable
                    // as this map is: an owning source yields an owning inverse, a wrap()-backed one a live view.
                    result = new ImmutableBiMap<>(biMap.inverse(), ownsBacking);
                    result.invertedView = this;
                    invertedView = result;
                }
            }
        }

        return result;
    }

    /**
     * Creates a new {@link Builder} for constructing an {@code ImmutableBiMap}.
     *
     * <p>The builder allows adding key-value pairs incrementally and then producing
     * an immutable result via {@link Builder#build()}. This is useful when the
     * number of entries is not known at compile time or when entries are added
     * conditionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .put("three", 3)
     *     .build();
     * }</pre>
     *
     * <p>The builder uses its own private storage, so the map returned by {@link Builder#build()} is an
     * independent, stable value once the builder has been consumed.</p>
     *
     * @param <K> the type of keys to be maintained by the map
     * @param <V> the type of mapped values
     * @return a new {@code Builder} instance for creating an {@code ImmutableBiMap}
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new {@link Builder} for constructing an {@code ImmutableBiMap}
     * using the provided {@link BiMap} as backing storage.
     *
     * <p>The builder will add entries to the provided {@code BiMap} and then
     * create an immutable view of it when {@link Builder#build()} is called.
     * The given {@code BiMap} should not be modified outside the builder after
     * this method is invoked.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> backing = new BiMap<>();
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.builder(backing)
     *     .put("one", 1)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * <p><b>Warning:</b> the caller keeps a reference to {@code backedMap}, so the map returned by
     * {@link Builder#build()} is a live view over storage the caller can still modify. Use the no-arg
     * {@link #builder()} when an independent value is wanted.</p>
     *
     * @param <K> the type of keys to be maintained by the map
     * @param <V> the type of mapped values
     * @param backedMap the {@code BiMap} to be used as backing storage for the builder
     * @return a new {@code Builder} instance that uses {@code backedMap} as storage
     * @throws IllegalArgumentException if {@code backedMap} is {@code null}.
     */
    public static <K, V> Builder<K, V> builder(final BiMap<K, V> backedMap) throws IllegalArgumentException {
        N.checkArgNotNull(backedMap, cs.backedMap);

        return new Builder<>(backedMap);
    }

    /**
     * A builder for creating {@link ImmutableBiMap} instances.
     *
     * <p>The builder pattern allows for flexible construction of immutable
     * bidirectional maps, especially useful when entries are added conditionally
     * or in loops.</p>
     *
     * <p>The builder is not thread-safe and must not be used concurrently from
     * multiple threads without external synchronization.</p>
     *
     * @param <K> the type of keys in the map being built
     * @param <V> the type of values in the map being built
     */
    public static final class Builder<K, V> {
        private final BiMap<K, V> map;

        /** Whether {@link #map} is the builder's own storage, unreachable to any caller. */
        private final boolean ownsStorage;

        /** Set by {@link #build()}; further entry additions are rejected from then on. */
        private boolean built;

        /**
         * Creates a builder that accumulates entries into a newly allocated insertion-ordered {@link BiMap}.
         */
        Builder() {
            // Same LinkedHashMap backing as of(...): a HashMap-backed BiMap would iterate in hash order
            // while ImmutableMap.builder() and ImmutableBiMap.of preserve encounter order.
            map = orderedBiMap(16);
            ownsStorage = true;
        }

        /**
         * Creates a builder that accumulates entries into the given {@link BiMap}.
         * The map is used as-is (not copied), so entries already present in it are
         * part of the built result.
         *
         * @param backedMap the {@code BiMap} used as backing storage for this builder
         */
        Builder(final BiMap<K, V> backedMap) {
            map = backedMap;
            ownsStorage = false;
        }

        /**
         * @throws IllegalStateException if this builder has already been consumed by {@code build()}
         */
        private void assertNotBuilt() throws IllegalStateException {
            if (built) {
                throw new IllegalStateException("This builder has already been consumed by build() and cannot be modified");
            }
        }

        /**
         * Associates the specified value with the specified key in the map being
         * built. If the map previously contained a mapping for the key, the old
         * value is replaced. The same {@code BiMap} constraints apply as usual
         * (for example, value uniqueness).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.put("key", "value")
         *        .put("another", "data");
         * }</pre>
         *
         * @param key   the key with which the specified value is to be associated
         * @param value the value to be associated with the specified key
         * @return this builder instance, for method chaining
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         * @throws IllegalArgumentException if {@code key} or {@code value} is {@code null}, or if {@code value} is
         *         already bound to a different key in the map being built.
         */
        public Builder<K, V> put(final K key, final V value) throws IllegalStateException, IllegalArgumentException {
            assertNotBuilt();

            map.put(key, value);

            return this;
        }

        /**
         * Copies all mappings from the specified map into the map being built.
         * The effect of this call is equivalent to calling {@link #put(Object, Object)}
         * for each mapping from key {@code k} to value {@code v} in the specified map.
         *
         * <p>If the provided map is {@code null} or empty, this method has no effect.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> existing = Map.of("a", 1, "b", 2);
         * builder.putAll(existing)
         *        .put("c", 3);
         * }</pre>
         *
         * @param m the map whose mappings are to be added; may be {@code null} or empty
         * @return this builder instance, for method chaining
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         * @throws IllegalArgumentException if any key or value in {@code m} is {@code null}, or if a value is
         *         already bound to a different key in the map being built. Entries added before the failing one
         *         remain in the map being built.
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) throws IllegalStateException, IllegalArgumentException {
            assertNotBuilt();

            if (N.notEmpty(m)) {
                map.putAll(m);
            }

            return this;
        }

        /**
         * Builds and returns an {@link ImmutableBiMap} containing all entries added to this builder.
         * The returned map is backed by the builder's storage rather than by a copy, so this method
         * consumes the builder: any subsequent {@code put}/{@code putAll} call throws
         * {@link IllegalStateException}. {@code build()} itself may be called more than once and returns
         * an equal map each time.
         *
         * <p>The returned map is immutable and will throw {@link UnsupportedOperationException}
         * for any modification attempts. When the builder was created by {@link ImmutableBiMap#builder()}
         * its storage is private and the result is an independent value; when it was created by
         * {@link ImmutableBiMap#builder(BiMap)} the caller can still modify the {@code BiMap} it supplied,
         * so the result stays a live view.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableBiMap<String, Integer> map = builder.build();
         * // builder.put("more", 1);   // throws IllegalStateException
         * }</pre>
         *
         * @return a new {@code ImmutableBiMap} containing all entries added to this builder
         */
        public ImmutableBiMap<K, V> build() {
            built = true;

            return new ImmutableBiMap<>(map, ownsStorage);
        }
    }
}
