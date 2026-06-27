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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * A {@link Multimap} implementation that uses {@link Set} as the value collection type, ensuring
 * that duplicate values for the same key are automatically eliminated.
 *
 * <p>The default backing map is {@link HashMap} and the default value collection is {@link HashSet},
 * giving O(1) average-time key lookup and value membership tests with no guaranteed ordering.
 * Alternative backing implementations (e.g., {@link java.util.TreeMap}/{@link java.util.TreeSet}
 * for sorted order, {@link java.util.LinkedHashMap}/{@link java.util.LinkedHashSet} for
 * insertion order) can be specified via the appropriate constructor or factory method.
 *
 * <p>This class is <b>not thread-safe</b>. For concurrent access, supply a thread-safe backing
 * map and set via {@link N#newSetMultimap(Supplier, Supplier)}, or apply external synchronization.
 *
 * <p><b>Typical use cases:</b>
 * <ul>
 *   <li>Many-to-many relationships where duplicate associations are meaningless</li>
 *   <li>Tag/permission systems associating entities with unique sets of labels</li>
 *   <li>Graph adjacency lists with unique neighbor sets</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * SetMultimap<String, String> userRoles = N.newSetMultimap();
 * userRoles.put("alice", "admin");
 * userRoles.put("alice", "user");
 * userRoles.put("alice", "admin");
 * Set<String> roles = userRoles.get("alice");   // returns {"admin", "user"}
 *
 * // Build from a collection
 * List<String> words = Arrays.asList("apple", "apricot", "banana");
 * SetMultimap<Character, String> byFirstChar =
 *     SetMultimap.fromCollection(words, s -> s.charAt(0));
 * // {'a' -> {"apple", "apricot"}, 'b' -> {"banana"}}
 *
 * // Invert mapping
 * SetMultimap<String, String> inverted = userRoles.invert();
 * }</pre>
 *
 * <p><b>Factory methods:</b>
 * <ul>
 *   <li>{@link #of(Object, Object)} - single key-value pair</li>
 *   <li>{@link #fromMap(Map)} - from an existing {@link Map}</li>
 *   <li>{@link #fromCollection(Collection, Function)} - group by key extractor</li>
 *   <li>{@link #fromCollection(Collection, Function, Function)} - key and value extractors</li>
 *   <li>{@link #merge(Map, Map)} / {@link #merge(Map, Map, Map)} - merge maps</li>
 *   <li>{@link #wrap(Map)} - wrap an existing {@code Map<K, Set<E>>} without copying</li>
 *   <li>{@link N#newSetMultimap()} - empty instance with default backing</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this multimap
 * @param <E> the type of values held in the sets associated with each key
 * @see Multimap
 * @see ListMultimap
 * @see N#newSetMultimap()
 * @see N#newSetMultimap(Class, Class)
 * @see N#newSetMultimap(Supplier, Supplier)
 */
public final class SetMultimap<K, E> extends Multimap<K, E, Set<E>> {
    /**
     * Constructs a new instance of SetMultimap with the default initial capacity.
     */
    SetMultimap() {
        this(HashMap.class, HashSet.class);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the SetMultimap.
     */
    SetMultimap(final int initialCapacity) {
        this(N.newHashMap(initialCapacity), HashSet.class);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map type and set type.
     *
     * @param mapType The class of the map to be used as the backing map.
     * @param valueType The class of the set to be used as the value collection.
     */
    @SuppressWarnings("rawtypes")
    SetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        super(mapType, valueType);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map supplier and set supplier.
     *
     * @param mapSupplier The supplier that provides the map to be used as the backing map.
     * @param valueSupplier The supplier that provides the set to be used as the value collection.
     */
    SetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier, final Supplier<? extends Set<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map and set type.
     * This constructor allows the user to specify a custom map and set type for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueType The class of the set to be used as the value collection.
     */
    @Internal
    @SuppressWarnings("rawtypes")
    SetMultimap(final Map<K, Set<E>> valueMap, final Class<? extends Set> valueType) {
        super(valueMap, valueTypeToSupplier(valueType));
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map and set supplier.
     * This constructor allows the user to specify a custom map and set supplier for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueSupplier The supplier that provides the set to be used as the value collection.
     */
    @Internal
    SetMultimap(final Map<K, Set<E>> valueMap, final Supplier<? extends Set<E>> valueSupplier) {
        super(valueMap, valueSupplier);
    }

    /**
     * Creates a new instance of SetMultimap with one key-value pair.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with a single entry.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("key1", 100);
     * mm.get("key1");                                    // returns [100]
     * mm.get("absent");                                  // returns null (absent key)
     *
     * SetMultimap<String, String> nullVal = SetMultimap.of("k", null);
     * nullVal.get("k");                                  // returns [null] (null value stored)
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <E> the type of the value
     * @param k1 the key of the key-value pair
     * @param v1 the value of the key-value pair
     * @return a new instance of SetMultimap with the specified key-value pair
     * @see #of(Object, Object, Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1) {
        final SetMultimap<K, E> map = new SetMultimap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with two key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with two entries.
     * If both keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2);
     * mm.get("a");                                       // returns [1]
     * mm.get("b");                                       // returns [2]
     * mm.get("x");                                       // returns null (absent key)
     *
     * SetMultimap<String, Integer> same = SetMultimap.of("a", 1, "a", 2);
     * same.get("a");                                     // returns [1, 2] (same key merged)
     *
     * SetMultimap<String, Integer> dup = SetMultimap.of("a", 1, "a", 1);
     * dup.get("a");                                      // returns [1] (duplicate removed)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2) {
        final SetMultimap<K, E> map = new SetMultimap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with three key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with three entries.
     * If multiple keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3);
     * mm.size();                                         // returns 3 (number of keys)
     * mm.get("a");                                       // returns [1]
     * mm.get("z");                                       // returns null (absent key)
     *
     * SetMultimap<String, Integer> dup = SetMultimap.of("a", 1, "a", 1, "a", 2);
     * dup.get("a");                                      // returns [1, 2] (duplicate removed)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3) {
        final SetMultimap<K, E> map = new SetMultimap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with four key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with four entries.
     * If multiple keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4);
     * mm.size();                                         // returns 4 (number of keys)
     * mm.get("d");                                       // returns [4]
     * mm.get("z");                                       // returns null (absent key)
     *
     * SetMultimap<String, Integer> dup = SetMultimap.of("a", 1, "a", 2, "a", 1, "b", 5);
     * dup.get("a");                                      // returns [1, 2] (duplicate removed)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @param k4 the fourth key of the key-value pairs
     * @param v4 the fourth value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4) {
        final SetMultimap<K, E> map = new SetMultimap<>(4);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with five key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with five entries.
     * If multiple keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
     * mm.size();                                         // returns 5 (number of keys)
     * mm.get("e");                                       // returns [5]
     * mm.get("z");                                       // returns null (absent key)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @param k4 the fourth key of the key-value pairs
     * @param v4 the fourth value of the key-value pairs
     * @param k5 the fifth key of the key-value pairs
     * @param v5 the fifth value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5) {
        final SetMultimap<K, E> map = new SetMultimap<>(5);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with six key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with six entries.
     * If multiple keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
     * mm.size();                                         // returns 6 (number of keys)
     * mm.get("f");                                       // returns [6]
     * mm.get("z");                                       // returns null (absent key)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @param k4 the fourth key of the key-value pairs
     * @param v4 the fourth value of the key-value pairs
     * @param k5 the fifth key of the key-value pairs
     * @param v5 the fifth value of the key-value pairs
     * @param k6 the sixth key of the key-value pairs
     * @param v6 the sixth value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5, final K k6, final E v6) {
        final SetMultimap<K, E> map = new SetMultimap<>(6);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with seven key-value pairs.
     *
     * <p>This is a convenient factory method for creating a SetMultimap with seven entries.
     * If multiple keys are the same, only unique values will be added to the set associated with that key.
     * Duplicate values will be ignored due to Set semantics.
     * The returned SetMultimap is mutable and backed by a HashMap and HashSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
     * mm.size();                                         // returns 7 (number of keys)
     * mm.get("g");                                       // returns [7]
     * mm.get("z");                                       // returns null (absent key)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @param k4 the fourth key of the key-value pairs
     * @param v4 the fourth value of the key-value pairs
     * @param k5 the fifth key of the key-value pairs
     * @param v5 the fifth value of the key-value pairs
     * @param k6 the sixth key of the key-value pairs
     * @param v6 the sixth value of the key-value pairs
     * @param k7 the seventh key of the key-value pairs
     * @param v7 the seventh value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #fromMap(Map)
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5, final K k6, final E v6, final K k7, final E v7) {
        final SetMultimap<K, E> map = new SetMultimap<>(7);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap from a regular map by converting each key-value pair
     * into a key with a single-element set containing the value.
     *
     * <p>The returned SetMultimap uses the same map type as the input map for its backing storage,
     * and uses {@link HashSet} for value collections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * SetMultimap<String, Integer> mm = SetMultimap.fromMap(map);
     * mm.get("a");                                       // returns [1]
     * mm.get("b");                                       // returns [2]
     * mm.get("x");                                       // returns null (absent key)
     *
     * SetMultimap<String, Integer> empty = SetMultimap.fromMap(null);
     * empty.isEmpty();                                   // returns true (null map -> empty)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param map The map containing the key-value pairs to be added to the new SetMultimap, may be {@code null} or empty
     * @return a new instance of SetMultimap with the key-value pairs from the specified map
     */
    public static <K, E> SetMultimap<K, E> fromMap(final Map<? extends K, ? extends E> map) {
        //noinspection rawtypes
        final SetMultimap<K, E> multimap = new SetMultimap<>(Maps.newTargetMap(map), HashSet.class);

        if (N.notEmpty(map)) {
            multimap.putAll(map);
        }

        return multimap;
    }

    /**
     * Creates a new instance of SetMultimap from a collection by grouping elements by keys extracted using the provided function.
     * Each element is mapped to a key, and all elements with the same key are collected into a set.
     *
     * <p>This method is useful for grouping objects by a specific property or attribute.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = List.of("apple", "ant", "banana", "bear");
     * SetMultimap<Character, String> mm = SetMultimap.fromCollection(words, s -> s.charAt(0));
     * mm.get('a');                                       // returns ["apple", "ant"]
     * mm.get('b');                                       // returns ["banana", "bear"]
     * mm.get('z');                                       // returns null (absent key)
     *
     * SetMultimap<Character, String> empty = SetMultimap.fromCollection(null, s -> s.charAt(0));
     * empty.isEmpty();                                   // returns true (null collection -> empty)
     *
     * SetMultimap.fromCollection(words, null);           // throws IllegalArgumentException (null keyExtractor)
     * }</pre>
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the SetMultimap
     * @param c the collection of elements to be added to the SetMultimap, may be {@code null} or empty
     * @param keyExtractor the function to extract keys from elements; must not be null
     * @return a new instance of SetMultimap with keys extracted from elements and values being the elements themselves
     * @throws IllegalArgumentException if the keyExtractor is null
     */
    public static <T, K> SetMultimap<K, T> fromCollection(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        final SetMultimap<K, T> multimap = N.newSetMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyExtractor.apply(e), e);
            }
        }

        return multimap;
    }

    /**
     * Creates a new instance of SetMultimap from a collection by extracting both keys and values using the provided functions.
     * Elements with the same key will have their extracted values collected into a set, with duplicates automatically eliminated.
     *
     * <p>This method is useful for creating a multimap from a collection of objects by transforming them into
     * different key-value pairs. The keyExtractor determines how elements are grouped, while the valueExtractor
     * transforms elements into the values that will be stored in the multimap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = List.of("apple", "ant", "banana");
     * SetMultimap<Character, Integer> mm =
     *     SetMultimap.fromCollection(words, s -> s.charAt(0), String::length);
     * mm.get('a');                                       // returns [3, 5] (apple=5, ant=3)
     * mm.get('b');                                       // returns [6] (banana=6)
     * mm.get('z');                                       // returns null (absent key)
     *
     * // duplicate extracted values under the same key are deduplicated
     * SetMultimap<Integer, Integer> dup =
     *     SetMultimap.fromCollection(List.of("xy", "zw"), String::length, String::length);
     * dup.get(2);                                        // returns [2] (both length 2, deduped)
     *
     * SetMultimap.fromCollection(words, null, String::length); // throws IllegalArgumentException (null keyExtractor)
     * }</pre>
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the SetMultimap
     * @param <E> the type of the values in the SetMultimap
     * @param c the collection of elements to be transformed, may be {@code null} or empty
     * @param keyExtractor the function to extract keys from elements; must not be null
     * @param valueExtractor the function to extract values from elements; must not be null
     * @return a new instance of SetMultimap with extracted keys and values from the specified collection
     * @throws IllegalArgumentException if keyExtractor or valueExtractor is null
     */
    public static <T, K, E> SetMultimap<K, E> fromCollection(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
            final Function<? super T, ? extends E> valueExtractor) {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(valueExtractor);

        final SetMultimap<K, E> multimap = N.newSetMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyExtractor.apply(e), valueExtractor.apply(e));
            }
        }

        return multimap;
    }

    /**
     * Creates a new instance of SetMultimap by merging the key-value pairs from two specified maps.
     *
     * <p>If the same key appears in both maps, both values will be added to the resulting set for that key.
     * Since this is a SetMultimap, duplicate values for the same key will be automatically eliminated.
     * Null maps are treated as empty maps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
     * Map<String, Integer> map2 = Map.of("c", 3, "a", 4);
     * SetMultimap<String, Integer> mm = SetMultimap.merge(map1, map2);
     * mm.get("a");                                       // returns [1, 4] (shared key merged)
     * mm.get("b");                                       // returns [2]
     * mm.get("c");                                       // returns [3]
     *
     * SetMultimap<String, Integer> nulls = SetMultimap.merge((Map<String, Integer>) null, (Map<String, Integer>) null);
     * nulls.isEmpty();                                   // returns true (both null -> empty)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new SetMultimap, may be {@code null}
     * @param b the second map containing the key-value pairs to be added to the new SetMultimap, may be {@code null}
     * @return a new instance of SetMultimap with the key-value pairs from the specified maps
     */
    public static <K, E> SetMultimap<K, E> merge(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.newSetMultimap() : fromMap(b);
        } else {
            final SetMultimap<K, E> res = fromMap(a);
            res.putAll(b);
            return res;
        }
    }

    /**
     * Creates a new instance of SetMultimap by merging the key-value pairs from three specified maps.
     *
     * <p>If the same key appears in multiple maps, all unique values will be added to the resulting set for that key.
     * Since this is a SetMultimap, duplicate values for the same key will be automatically eliminated.
     * Null maps are treated as empty maps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1);
     * Map<String, Integer> map2 = Map.of("b", 2);
     * Map<String, Integer> map3 = Map.of("a", 9);
     * SetMultimap<String, Integer> mm = SetMultimap.merge(map1, map2, map3);
     * mm.get("a");                                       // returns [1, 9] (shared key merged)
     * mm.get("b");                                       // returns [2]
     * mm.get("c");                                       // returns null (absent key)
     *
     * SetMultimap<String, Integer> empty = SetMultimap.merge(
     *         (Map<String, Integer>) null, (Map<String, Integer>) null, (Map<String, Integer>) null);
     * empty.isEmpty();                                   // returns true (all null -> empty)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new SetMultimap, may be {@code null}
     * @param b the second map containing the key-value pairs to be added to the new SetMultimap, may be {@code null}
     * @param c the third map containing the key-value pairs to be added to the new SetMultimap, may be {@code null}
     * @return a new instance of SetMultimap with the key-value pairs from the specified maps
     */
    public static <K, E> SetMultimap<K, E> merge(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b,
            final Map<? extends K, ? extends E> c) {
        if (a == null) {
            if (b == null) {
                return c == null ? N.newSetMultimap() : fromMap(c);
            } else {
                final SetMultimap<K, E> res = fromMap(b);
                res.putAll(c);
                return res;
            }
        } else {
            final SetMultimap<K, E> res = fromMap(a);
            res.putAll(b);
            res.putAll(c);
            return res;
        }
    }

    /**
     * Creates a new instance of SetMultimap by merging the key-value pairs from a collection of maps.
     *
     * <p>If the same key appears in multiple maps within the collection, all unique values will be added to the
     * resulting set for that key. Since this is a SetMultimap, duplicate values for the same key will be
     * automatically eliminated. If the collection is {@code null} or empty, an empty SetMultimap is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Integer>> maps = List.of(
     *     Map.of("a", 1, "b", 2),
     *     Map.of("c", 3),
     *     Map.of("a", 4)
     * );
     * SetMultimap<String, Integer> mm = SetMultimap.merge(maps);
     * mm.get("a");                                       // returns [1, 4] (shared key merged)
     * mm.get("b");                                       // returns [2]
     * mm.get("c");                                       // returns [3]
     *
     * SetMultimap<String, Integer> empty = SetMultimap.merge((List<Map<String, Integer>>) null);
     * empty.isEmpty();                                   // returns true (null collection -> empty)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param c the collection of maps containing the key-value pairs to be added to the new SetMultimap, may be {@code null} or empty
     * @return a new instance of SetMultimap with the key-value pairs from the specified collection of maps
     */
    public static <K, E> SetMultimap<K, E> merge(final Collection<? extends Map<? extends K, ? extends E>> c) {
        if (N.isEmpty(c)) {
            return N.newSetMultimap();
        }

        final Iterator<? extends Map<? extends K, ? extends E>> iter = c.iterator();
        final SetMultimap<K, E> res = fromMap(iter.next());

        while (iter.hasNext()) {
            res.putAll(iter.next());
        }

        return res;
    }

    /**
     * Wraps an existing map into a SetMultimap without copying its contents. Changes to the wrapped map
     * will be reflected in the SetMultimap and vice versa, as they share the same underlying storage.
     *
     * <p><strong>Important:</strong> The provided map must not contain {@code null} or empty values.
     * Every value must be a {@code non-null}, non-empty {@link Set} instance. This constraint is
     * validated at wrap time.
     *
     * <p>This method is useful when you want to treat an existing {@code Map<K, Set<E>>} as a SetMultimap
     * without creating a copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<Integer>> map = new HashMap<>();
     * map.put("a", new HashSet<>(List.of(1, 2)));
     * SetMultimap<String, Integer> mm = SetMultimap.wrap(map);
     * mm.get("a");                                       // returns [1, 2]
     * mm.put("a", 3);                                    // returns true; also mutates the wrapped map
     * map.get("a");                                      // returns [1, 2, 3] (backing map changed)
     *
     * SetMultimap.wrap((Map<String, Set<Integer>>) null); // throws IllegalArgumentException (null map)
     *
     * Map<String, Set<Integer>> bad = new HashMap<>();
     * bad.put("x", new HashSet<>());                     // empty value not allowed
     * SetMultimap.wrap(bad);                             // throws IllegalArgumentException (empty value)
     * }</pre>
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the set
     * @param map the map to be wrapped into a SetMultimap; must not be {@code null} and must not contain {@code null} or empty values
     * @return a SetMultimap instance backed by the provided map
     * @throws IllegalArgumentException if the provided map is {@code null} or contains a {@code null} or empty value
     * @see #wrap(Map, Supplier)
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E> SetMultimap<K, E> wrap(final Map<K, ? extends Set<E>> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(map.values().stream().noneMatch(v -> v == null || v.isEmpty()), "The specified map contains null or empty value: %s", map);

        final Class<? extends Set> valueType = map.isEmpty() ? HashSet.class : map.values().iterator().next().getClass();

        return new SetMultimap<>((Map<K, Set<E>>) map, valueType);
    }

    /**
     * Wraps an existing map into a SetMultimap with a custom set supplier. Changes to the wrapped map
     * will be reflected in the SetMultimap and vice versa, as they share the same underlying storage.
     *
     * <p>The provided value supplier will be used to create new sets when new keys are added to the multimap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, TreeSet<Integer>> map = new HashMap<>();
     * SetMultimap<String, Integer> mm = SetMultimap.wrap(map, TreeSet::new);
     * mm.put("a", 3);                                    // returns true; creates a TreeSet for key "a"
     * mm.put("a", 1);                                    // returns true; added to the same TreeSet
     * mm.get("a");                                       // returns [1, 3] (TreeSet keeps values sorted)
     * mm.get("a").iterator().next();                     // returns 1 (smallest, sorted order)
     *
     * SetMultimap.wrap((Map<String, TreeSet<Integer>>) null, TreeSet::new); // throws IllegalArgumentException (null map)
     * }</pre>
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the set
     * @param <V> the type of the value set, which must extend {@code Set<E>}
     * @param map the map to be wrapped into a SetMultimap; must not be {@code null}
     * @param valueSupplier the supplier that provides the set to be used as the value collection; must not be {@code null}
     * @return a SetMultimap instance backed by the provided map
     * @throws IllegalArgumentException if the provided map or {@code valueSupplier} is {@code null}
     * @see #wrap(Map)
     */
    @Beta
    public static <K, E, V extends Set<E>> SetMultimap<K, E> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(valueSupplier, cs.valueSupplier);

        return new SetMultimap<>((Map<K, Set<E>>) map, valueSupplier);
    }

    /**
     * Creates a new SetMultimap with inverted key-value relationships. Each value in the original multimap
     * becomes a key in the result, and each original key becomes a value associated with that new key.
     *
     * <p>This operation is useful for creating reverse mappings or index structures. For example, if you have
     * a mapping of users to roles, invert() will give you a mapping of roles to users.
     *
     * <p>This operation creates a new multimap and does not modify the original. The new multimap will
     * preserve the ordering characteristics of the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "a", 2, "b", 1);
     * // original: {"a" -> [1, 2], "b" -> [1]}
     * SetMultimap<Integer, String> inverted = original.invert();
     * inverted.get(1);                                   // returns ["a", "b"]
     * inverted.get(2);                                   // returns ["a"]
     * inverted.get(9);                                   // returns null (absent key)
     *
     * SetMultimap<String, Integer> empty = SetMultimap.fromMap(null);
     * empty.invert().isEmpty();                          // returns true (empty -> empty)
     * }</pre>
     *
     * @return a new SetMultimap where each original value is mapped to the set of keys that contained it;
     *         an empty SetMultimap if this multimap is empty
     * @see #copy()
     */
    public SetMultimap<E, K> invert() {
        final SetMultimap<K, E> multimap = this;
        //noinspection rawtypes
        final SetMultimap<E, K> result = new SetMultimap<>(Maps.newOrderingMap(backingMap), valueSupplier);

        if (N.notEmpty(multimap)) {
            for (final Map.Entry<K, Set<E>> entry : multimap.backingMap.entrySet()) {
                final Set<E> c = entry.getValue();

                if (N.notEmpty(c)) {
                    for (final E e : c) {
                        result.put(e, entry.getKey());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Creates a copy of this SetMultimap. The returned multimap contains all key-value pairs from the original,
     * but uses new set instances for value collections.
     *
     * <p>The new SetMultimap uses the same map and set suppliers as the original, ensuring consistent behavior
     * and characteristics (ordering, implementation type, etc.).
     *
     * <p>Modifications to the returned multimap will not affect the original and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "b", 2);
     * SetMultimap<String, Integer> copy = original.copy();
     * copy.get("a");                                     // returns [1]
     * copy.put("c", 3);                                  // returns true; mutates copy only
     * copy.get("c");                                     // returns [3]
     * original.get("c");                                 // returns null (original unaffected)
     * }</pre>
     *
     * @return a new SetMultimap containing all the key-value pairs of this SetMultimap
     * @see #putValues(Multimap)
     */
    @Override
    public SetMultimap<K, E> copy() {
        final SetMultimap<K, E> copy = new SetMultimap<>(mapSupplier, valueSupplier);

        copy.putValues(this);

        return copy;
    }

    //    /**
    //     * Creates a new SetMultimap containing only the entries that satisfy the given predicate.
    //     * The predicate is applied to each key and its complete set of values.
    //     *
    //     * <p>The new SetMultimap shares the same set instances as the original for matching entries
    //     * (shallow copy of matching entries), and uses the same map and set suppliers.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
    //     * SetMultimap<String, Integer> filtered = map.filter((k, v) -> v.size() > 1);
    //     * // filtered contains: {"a" -> [1, 2]}
    //     * }</pre>
    //     *
    //     * @param filter the predicate to test each key-value pair; must not be {@code null}.
    //     *               The predicate receives the key and the complete set of values associated with that key.
    //     *               If it returns {@code true}, the entire entry is included in the result.
    //     * @return a new SetMultimap containing only the entries that match the filter
    //     */
    //    @Override
    //    public SetMultimap<K, E> filter(final BiPredicate<? super K, ? super Set<E>> filter) {
    //        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);
    //
    //        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
    //            if (filter.test(entry.getKey(), entry.getValue())) {
    //                result.putValues(entry.getKey(), entry.getValue());
    //            }
    //        }
    //
    //        return result;
    //    }

    //    /**
    //     * Creates a new SetMultimap containing only the entries whose keys satisfy the given predicate.
    //     *
    //     * <p>The new SetMultimap shares the same set instances as the original for matching entries
    //     * (shallow copy of matching entries), and uses the same map and set suppliers.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
    //     * SetMultimap<String, Integer> filtered = map.filterByKey(k -> k.equals("a") || k.equals("b"));
    //     * // filtered contains: {"a" -> [1], "b" -> [2]}
    //     * }</pre>
    //     *
    //     * @param keyPredicate the predicate to test each key; must not be {@code null}.
    //     *               If it returns {@code true} for a key, all values associated with that key are included in the result.
    //     * @return a new SetMultimap containing only the entries whose keys match the filter
    //     */
    //    @Override
    //    public SetMultimap<K, E> filterByKey(final Predicate<? super K> keyPredicate) {
    //        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);
    //
    //        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
    //            if (keyPredicate.test(entry.getKey())) {
    //                result.putValues(entry.getKey(), entry.getValue());
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * Creates a new SetMultimap containing only the entries whose value sets satisfy the given predicate.
    //     * The predicate is applied to each complete set of values associated with a key.
    //     *
    //     * <p>The new SetMultimap shares the same set instances as the original for matching entries
    //     * (shallow copy of matching entries), and uses the same map and set suppliers.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
    //     * SetMultimap<String, Integer> filtered = map.filterByValues(v -> !v.isEmpty() && v.size() > 1);
    //     * // filtered contains: {"a" -> [1, 2]}
    //     * }</pre>
    //     *
    //     * @param filter the predicate to test each value set; must not be {@code null}.
    //     *               The predicate receives the complete set of values for each key.
    //     *               If it returns {@code true}, the entire entry is included in the result.
    //     * @return a new SetMultimap containing only the entries whose value sets match the filter
    //     */
    //    @Override
    //    public SetMultimap<K, E> filterByValues(final Predicate<? super Set<E>> filter) {
    //        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);
    //
    //        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
    //            if (filter.test(entry.getValue())) {
    //                result.putValues(entry.getKey(), entry.getValue());
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     * Converts this SetMultimap into an immutable map where each key is associated with an immutable set of values.
     *
     * <p>This method creates a new map using the same type as the backing map, converts each value set
     * to an {@link ImmutableSet}, and wraps the result in an {@link ImmutableMap}.
     *
     * <p>The returned map and its value sets are completely immutable and cannot be modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
     * ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap();
     * immutable.get("a");                                // returns [1, 2]
     * immutable.get("b");                                // returns [3]
     * immutable.get("z");                                // returns null (absent key)
     * immutable.put("c", null);                          // throws UnsupportedOperationException (immutable)
     * }</pre>
     *
     * @return an {@link ImmutableMap} where each key from this multimap is associated with an
     *         {@link ImmutableSet} containing all values that were associated with that key;
     *         an empty map if this multimap is empty
     * @see #toImmutableMap(IntFunction)
     */
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap() {
        final Map<K, ImmutableSet<E>> map = Maps.newTargetMap(backingMap);

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    /**
     * Converts this SetMultimap into an immutable map using a custom map supplier.
     * Each key is associated with an immutable set of values.
     *
     * <p>The provided map supplier allows you to control the type of the underlying map in the returned ImmutableMap
     * (e.g., use a TreeMap for sorted keys, or a LinkedHashMap for insertion-order).
     *
     * <p>The returned map and its value sets are completely immutable and cannot be modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> map = SetMultimap.of("b", 2, "a", 1);
     * ImmutableMap<String, ImmutableSet<Integer>> sorted =
     *     map.toImmutableMap(size -> new TreeMap<>());
     * sorted.keySet();                                   // returns ["a", "b"] (sorted by TreeMap)
     * sorted.get("a");                                   // returns [1]
     * sorted.get("z");                                   // returns null (absent key)
     * }</pre>
     *
     * @param mapSupplier a function that creates a new map instance given an initial capacity;
     *                    the function receives the number of keys in this multimap as its argument; must not be {@code null}
     * @return an {@link ImmutableMap} where each key from this multimap is associated with an
     *         {@link ImmutableSet} containing all values that were associated with that key
     * @throws NullPointerException if {@code mapSupplier} is {@code null}
     * @see #toImmutableMap()
     */
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableSet<E>>> mapSupplier) {
        final Map<K, ImmutableSet<E>> map = mapSupplier.apply(backingMap.size());

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    //    public SetMultimap<E, K> invert() {
}
