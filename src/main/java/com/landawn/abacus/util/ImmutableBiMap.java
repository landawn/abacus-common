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
 *   <li>{@link #copyOf(BiMap)} – creates a defensive copy from another {@code BiMap}</li>
 *   <li>{@link #wrap(BiMap)} – wraps an existing {@code BiMap} (changes to the
 *       underlying map will be reflected in the view)</li>
 * </ul>
 *
 * <p>Unlike regular maps, {@code BiMap}s enforce that values are unique in
 * addition to keys. If duplicate values are provided when populating the
 * underlying {@code BiMap}, an exception may be thrown.</p>
 *
 * <p><b>Usage examples:</b></p>
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
@SuppressWarnings("java:S2160")
public final class ImmutableBiMap<K, V> extends ImmutableAbstractMap<K, V> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ImmutableBiMap EMPTY = new ImmutableBiMap(new BiMap<>());

    private final BiMap<K, V> biMap;

    private transient ImmutableBiMap<V, K> invertedView;

    /**
     * Constructs an {@code ImmutableBiMap} backed by the provided {@link BiMap}.
     * This constructor is marked as {@link Internal} and is not intended to be
     * used directly by library consumers. Prefer static factory methods such as
     * {@link #of(Object, Object)}, {@link #copyOf(BiMap)} or {@link #wrap(BiMap)}.
     *
     * @param map the {@code BiMap} whose mappings back this {@code ImmutableBiMap}
     */
    @Internal
    @SuppressWarnings("unchecked")
    ImmutableBiMap(final BiMap<? extends K, ? extends V> map) {
        super(map);
        biMap = (BiMap<K, V>) map;
    }

    /**
     * Returns a shared empty {@code ImmutableBiMap}. This method always returns the
     * same cached instance, making it memory-efficient for representing empty
     * bidirectional maps.
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> empty = ImmutableBiMap.empty();
     * System.out.println(empty.size());   // prints: 0
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
     * Returns an {@code ImmutableBiMap} containing a single key-value mapping.
     * The returned map is immutable and has a size of {@code 1}.
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> single = ImmutableBiMap.of("one", 1);
     * System.out.println(single.get("one"));      // prints: 1
     * System.out.println(single.getByValue(1));   // prints: "one"
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param k1 the key to be included in the map
     * @param v1 the value to be associated with {@code k1}
     * @return an {@code ImmutableBiMap} containing the provided key-value pair
     * @throws IllegalArgumentException if the underlying {@code BiMap} implementation
     *         does not accept the provided key or value (for example, due to
     *         nulls or duplicates)
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing exactly two key-value mappings.
     * All keys and values must be unique as required by {@link BiMap}.
     *
     * <p><b>Usage example:</b></p>
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
     * @throws IllegalArgumentException if the underlying {@code BiMap} implementation
     *         rejects the provided keys or values (for example, due to nulls or
     *         duplicates)
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an {@code ImmutableBiMap} containing exactly three key-value mappings.
     * All keys and values must be unique as required by {@link BiMap}.
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
     * @throws IllegalArgumentException if the underlying {@code BiMap} implementation
     *         rejects the provided keys or values (for example, due to nulls or
     *         duplicates)
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly four key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly five key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly six key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly seven key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly eight key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly nine key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableBiMap
     * @param v9 the value to be associated with the ninth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        return new ImmutableBiMap<>(biMap);
    }

    /**
     * Returns an ImmutableBiMap containing exactly ten key-value mappings.
     * All keys and values must be unique. If duplicate keys or values are provided,
     * an exception will be thrown.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param k1 the first key to be included in the ImmutableBiMap
     * @param v1 the value to be associated with the first key
     * @param k2 the second key to be included in the ImmutableBiMap
     * @param v2 the value to be associated with the second key
     * @param k3 the third key to be included in the ImmutableBiMap
     * @param v3 the value to be associated with the third key
     * @param k4 the fourth key to be included in the ImmutableBiMap
     * @param v4 the value to be associated with the fourth key
     * @param k5 the fifth key to be included in the ImmutableBiMap
     * @param v5 the value to be associated with the fifth key
     * @param k6 the sixth key to be included in the ImmutableBiMap
     * @param v6 the value to be associated with the sixth key
     * @param k7 the seventh key to be included in the ImmutableBiMap
     * @param v7 the value to be associated with the seventh key
     * @param k8 the eighth key to be included in the ImmutableBiMap
     * @param v8 the value to be associated with the eighth key
     * @param k9 the ninth key to be included in the ImmutableBiMap
     * @param v9 the value to be associated with the ninth key
     * @param k10 the tenth key to be included in the ImmutableBiMap
     * @param v10 the value to be associated with the tenth key
     * @return an ImmutableBiMap containing the provided key-value pairs
     * @throws IllegalArgumentException if any key or value is {@code null}, or if duplicate keys or values are provided
     */
    public static <K, V> ImmutableBiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        final BiMap<K, V> biMap = BiMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        return new ImmutableBiMap<>(biMap);
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
     * <p><b>Usage example:</b></p>
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
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(final BiMap<? extends K, ? extends V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return new ImmutableBiMap<>(map.copy());
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
     * <p><b>Warning:</b> the immutability guarantee of the returned instance relies
     * on external code not mutating the wrapped {@code BiMap}. This method is
     * therefore marked as {@link Beta}.</p>
     *
     * <p><b>Usage example:</b></p>
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
     * <p><b>Usage example:</b></p>
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
     * @param value the value whose associated key is to be returned; may be {@code null}
     * @return the key to which the specified value is mapped, or {@code null}
     *         if this map contains no mapping for the value
     */
    public K getByValue(final Object value) {
        return biMap.getByValue(value);
    }

    /**
     * Returns an inverted view of this {@code ImmutableBiMap}, in which each value
     * of this map becomes a key, and each key becomes the corresponding value.
     *
     * <p>The returned map is also an {@code ImmutableBiMap} and is backed by the
     * same underlying {@code BiMap} as this instance. Calling {@code inverted()}
     * multiple times returns the same cached instance.</p>
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2);
     * ImmutableBiMap<Integer, String> inverted = map.inverted();
     * String key = inverted.get(1);   // returns "one"
     * }</pre>
     *
     * @return an {@code ImmutableBiMap} view where keys and values are swapped
     */
    public ImmutableBiMap<V, K> inverted() {
        return invertedView == null ? (invertedView = ImmutableBiMap.wrap(biMap.inverted())) : invertedView;
    }

    /**
     * Creates a new {@link Builder} for constructing an {@code ImmutableBiMap}.
     *
     * <p>The builder allows adding key-value pairs incrementally and then producing
     * an immutable result via {@link Builder#build()}. This is useful when the
     * number of entries is not known at compile time or when entries are added
     * conditionally.</p>
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .put("three", 3)
     *     .build();
     * }</pre>
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
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> backing = new BiMap<>();
     * ImmutableBiMap<String, Integer> map = ImmutableBiMap.builder(backing)
     *     .put("one", 1)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of keys to be maintained by the map
     * @param <V> the type of mapped values
     * @param backedMap the {@code BiMap} to be used as backing storage for the builder
     * @return a new {@code Builder} instance that uses {@code backedMap} as storage
     * @throws IllegalArgumentException if {@code backedMap} is {@code null}
     */
    public static <K, V> Builder<K, V> builder(final BiMap<K, V> backedMap) throws IllegalArgumentException {
        N.checkArgNotNull(backedMap);

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

        Builder() {
            map = new BiMap<>();
        }

        Builder(final BiMap<K, V> backedMap) {
            map = backedMap;
        }

        /**
         * Associates the specified value with the specified key in the map being
         * built. If the map previously contained a mapping for the key, the old
         * value is replaced. The same {@code BiMap} constraints apply as usual
         * (for example, value uniqueness).
         *
         * <p><b>Usage example:</b></p>
         * <pre>{@code
         * builder.put("key", "value")
         *        .put("another", "data");
         * }</pre>
         *
         * @param key   the key with which the specified value is to be associated
         * @param value the value to be associated with the specified key
         * @return this builder instance, for method chaining
         */
        public Builder<K, V> put(final K key, final V value) {
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
         * <p><b>Usage example:</b></p>
         * <pre>{@code
         * Map<String, Integer> existing = Map.of("a", 1, "b", 2);
         * builder.putAll(existing)
         *        .put("c", 3);
         * }</pre>
         *
         * @param m the map whose mappings are to be added; may be {@code null} or empty
         * @return this builder instance, for method chaining
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                map.putAll(m);
            }

            return this;
        }

        /**
         * Builds and returns an {@link ImmutableBiMap} containing all entries
         * added to this builder.
         *
         * <p>After calling this method, the builder should generally not be used
         * further, as the returned {@code ImmutableBiMap} may be backed directly
         * by the builder's internal {@code BiMap} storage.</p>
         *
         * <p><b>Usage example:</b></p>
         * <pre>{@code
         * ImmutableBiMap<String, Integer> map = builder.build();
         * // The builder is not intended to be used after this point.
         * }</pre>
         *
         * @return a new {@code ImmutableBiMap} containing all entries added to this builder
         */
        public ImmutableBiMap<K, V> build() {
            return ImmutableBiMap.wrap(map);
        }
    }
}
