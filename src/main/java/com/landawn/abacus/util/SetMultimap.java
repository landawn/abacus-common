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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * A specialized {@link Multimap} implementation that uses {@link Set} collections to store unique values
 * for each key, ensuring that duplicate values are automatically eliminated. This final class extends the base
 * Multimap to provide set-specific functionality optimized for scenarios where value uniqueness per key
 * is essential and ordering may not be important.
 *
 * <p>SetMultimap is ideal for use cases where you need to associate multiple unique values with each key
 * without worrying about duplicates. The underlying set structure provides efficient membership testing
 * and automatic deduplication while maintaining the multimap's key-to-many-values semantics.</p>
 *
 * <p><b>Key Characteristics:</b>
 * <ul>
 *   <li><b>Unique Values:</b> Automatically prevents duplicate values for the same key using Set semantics</li>
 *   <li><b>No Guaranteed Order:</b> Value ordering depends on the Set implementation (HashSet vs TreeSet)</li>
 *   <li><b>Set Semantics:</b> Each key maps to a {@link Set} of values with efficient contains() operations</li>
 *   <li><b>Efficient Membership:</b> O(1) average time for checking if a key-value pair exists</li>
 *   <li><b>Memory Efficient:</b> Only creates sets when values are actually added to keys</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Design Decisions:</b>
 * <ul>
 *   <li>This is a <b>final class</b> that cannot be extended for API stability</li>
 *   <li>Extends {@link Multimap} to inherit common multimap operations</li>
 *   <li>Uses {@link HashSet} as the default set implementation for optimal performance</li>
 *   <li>Thread safety depends on the backing Map and Set implementations chosen</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Relationship Modeling:</b> Many-to-many relationships where duplicates are meaningless</li>
 *   <li><b>Tag Systems:</b> Associating unique tags or categories with entities</li>
 *   <li><b>Permission Management:</b> Mapping users to unique sets of permissions or roles</li>
 *   <li><b>Data Deduplication:</b> Automatically eliminating duplicate associations</li>
 *   <li><b>Graph Adjacency:</b> Representing graph adjacency lists with unique neighbors</li>
 *   <li><b>Index Structures:</b> Building indexes where uniqueness is required</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic operations with automatic deduplication
 * SetMultimap<String, String> userRoles = N.newSetMultimap();
 * userRoles.put("alice", "admin");
 * userRoles.put("alice", "user");
 * userRoles.put("alice", "admin");   // duplicate ignored
 * Set<String> aliceRoles = userRoles.get("alice");   // ["admin", "user"]
 *
 * // Creating from collections with grouping
 * List<String> words = Arrays.asList("apple", "apricot", "banana", "blueberry");
 * SetMultimap<Integer, String> byLength =
 *     SetMultimap.fromCollection(words, String::length);
 * // Result: {5: ["apple"], 7: ["apricot", "banana"], 9: ["blueberry"]}
 *
 * // Tag system example
 * SetMultimap<String, String> articleTags = N.newSetMultimap();
 * articleTags.put("article1", "java");
 * articleTags.put("article1", "programming");
 * articleTags.put("article1", "tutorial");
 * articleTags.put("article2", "java");
 * articleTags.put("article2", "advanced");
 *
 * // Finding articles with specific tags
 * SetMultimap<String, String> inverted = articleTags.invert();
 * Set<String> javaArticles = inverted.get("java");   // ["article1", "article2"]
 *
 * // Filtering and transformations (stream-based)
 * SetMultimap<String, String> filtered = N.newSetMultimap();
 * articleTags.stream()
 *     .filter(e -> e.getValue().size() > 2)
 *     .forEach(e -> filtered.putValues(e.getKey(), e.getValue()));
 *
 * SetMultimap<String, String> javaOnly = N.newSetMultimap();
 * articleTags.stream()
 *     .filter(e -> e.getValue().contains("java"))
 *     .forEach(e -> javaOnly.putValues(e.getKey(), e.getValue()));
 *
 * // Conversion to immutable structures
 * ImmutableMap<String, ImmutableSet<String>> immutable = articleTags.toImmutableMap();
 * }</pre>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #of(Object, Object)} - Single key-value pair</li>
 *   <li>{@link #fromMap(Map)} - From existing Map</li>
 *   <li>{@link #fromCollection(Collection, Function)} - Grouping by key extractor</li>
 *   <li>{@link #fromCollection(Collection, Function, Function)} - Key and value extractors</li>
 *   <li>{@link #merge(Map, Map)} - Concatenating multiple maps</li>
 *   <li>{@link #wrap(Map)} - Wrapping existing Map&lt;K, Set&lt;E&gt;&gt;</li>
 *   <li>{@link N#newSetMultimap()} - Empty instance with default backing</li>
 * </ul>
 *
 * <p><b>Set-Specific Operations:</b>
 * <ul>
 *   <li>{@link #invert()} - Invert keys and values while maintaining uniqueness</li>
 *   <li>{@link #toImmutableMap()} - Convert to immutable representation</li>
 *   <li>{@link #copy()} - Create a deep copy of the SetMultimap</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Key lookup: O(1) average time with HashMap backing</li>
 *   <li>Value membership test: O(1) average time with HashSet</li>
 *   <li>Value addition: O(1) average time, with automatic deduplication</li>
 *   <li>Iteration: O(n) where n is total number of unique values</li>
 *   <li>Memory usage: O(k + v) where k is keys and v is total unique values</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * SetMultimap is <b>not thread-safe</b> by default. For concurrent access:
 * <ul>
 *   <li>Use {@code N.newSetMultimap(ConcurrentHashMap::new, ConcurrentHashMap::newKeySet)} for concurrent maps</li>
 *   <li>Wrap individual sets with {@code Collections.synchronizedSet()}</li>
 *   <li>Use external synchronization for write operations</li>
 *   <li>Consider using concurrent set implementations for value collections</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs {@link ListMultimap}:</b> Unique values and no ordering vs. duplicates and insertion order</li>
 *   <li><b>vs {@code Map<K, Set<E>>}:</b> Automatic set creation and multimap-specific operations</li>
 *   <li><b>vs Google Guava SetMultimap:</b> Similar API with additional utility methods</li>
 *   <li><b>vs {@link Multiset}:</b> Key-value structure vs. counting structure</li>
 * </ul>
 *
 * <p><b>Set Implementation Choices:</b>
 * <ul>
 *   <li><b>HashSet (default):</b> Fast operations, no ordering guarantees</li>
 *   <li><b>TreeSet:</b> Sorted values, O(log n) operations</li>
 *   <li><b>LinkedHashSet:</b> Insertion-order preservation with fast operations</li>
 *   <li><b>ConcurrentHashMap.newKeySet():</b> Thread-safe set implementation</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate backing implementations based on ordering and concurrency needs</li>
 *   <li>Consider using {@link #toImmutableMap()} for read-only snapshots</li>
 *   <li>Stream-based filtering creates new instances - chain operations efficiently</li>
 *   <li>Use {@link #wrap(Map)} when you already have a suitable Map structure</li>
 *   <li>Choose TreeSet backing for naturally ordered values</li>
 * </ul>
 *
 * <p><b>Memory Considerations:</b>
 * <ul>
 *   <li>Empty keys don't consume memory for value collections</li>
 *   <li>HashSet backing provides good performance for most use cases</li>
 *   <li>Use TreeSet only when ordering is required, as it has higher memory overhead</li>
 *   <li>Consider using primitive collections for numeric values to reduce boxing overhead</li>
 * </ul>
 *
 * <p><b>Uniqueness Guarantees:</b>
 * <ul>
 *   <li>Duplicate values for the same key are automatically ignored</li>
 *   <li>Equality is determined by the value's {@code equals()} method</li>
 *   <li>Null values are supported if the underlying Set implementation allows them</li>
 *   <li>Uniqueness is maintained across all multimap operations</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this SetMultimap
 * @param <E> the type of values maintained in the sets associated with each key
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
     * SetMultimap<String, Integer> map = SetMultimap.of("key1", 100);
     * // map contains: {"key1" -> [100]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
     * // map contains: {"a" -> [1], "b" -> [2]}
     *
     * SetMultimap<String, Integer> map2 = SetMultimap.of("a", 1, "a", 2);
     * // map2 contains: {"a" -> [1, 2]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
     * // map contains: {"a" -> [1], "b" -> [2], "c" -> [3]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4);
     * // map contains: {"a" -> [1], "b" -> [2], "c" -> [3], "d" -> [4]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
     * // map contains: {"a" -> [1], "b" -> [2], "c" -> [3], "d" -> [4], "e" -> [5]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
     * // map contains: {"a" -> [1], "b" -> [2], "c" -> [3], "d" -> [4], "e" -> [5], "f" -> [6]}
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
     * SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
     * // map contains: {"a" -> [1], "b" -> [2], "c" -> [3], "d" -> [4], "e" -> [5], "f" -> [6], "g" -> [7]}
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
     * SetMultimap<String, Integer> multimap = SetMultimap.fromMap(map);
     * // multimap contains: {"a" -> [1], "b" -> [2]}
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
     * SetMultimap<Character, String> grouped = SetMultimap.fromCollection(words, s -> s.charAt(0));
     * // grouped contains: {'a' -> ["apple", "ant"], 'b' -> ["banana", "bear"]}
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
     * SetMultimap<Character, Integer> multimap =
     *     SetMultimap.fromCollection(words, s -> s.charAt(0), String::length);
     * // multimap contains: {'a' -> [5, 3], 'b' -> [6]}
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
     * SetMultimap<String, Integer> concatenated = SetMultimap.merge(map1, map2);
     * // concatenated contains: {"a" -> [1, 4], "b" -> [2], "c" -> [3]}
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
     * Map<String, Integer> map3 = Map.of("c", 3);
     * SetMultimap<String, Integer> concatenated = SetMultimap.merge(map1, map2, map3);
     * // concatenated contains: {"a" -> [1], "b" -> [2], "c" -> [3]}
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
     * SetMultimap<String, Integer> concatenated = SetMultimap.merge(maps);
     * // concatenated contains: {"a" -> [1, 4], "b" -> [2], "c" -> [3]}
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
     * <p><strong>Important:</strong> The provided map must not contain {@code null} values.
     * All values must be {@code non-null} Set instances. This constraint is validated at wrap time.
     *
     * <p>This method is useful when you want to treat an existing Map&lt;K, Set&lt;E&gt;&gt; as a SetMultimap
     * without creating a copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<Integer>> map = new HashMap<>();
     * map.put("a", new HashSet<>(List.of(1, 2)));
     * SetMultimap<String, Integer> multimap = SetMultimap.wrap(map);
     * multimap.put("a", 3);   // modifies the original map
     * }</pre>
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the set
     * @param <V> the type of the set extending Set&lt;E&gt;
     * @param map The map to be wrapped into a SetMultimap; must not be {@code null} and must not contain {@code null} values
     * @return a SetMultimap instance backed by the provided map
     * @throws IllegalArgumentException if the provided map is {@code null} or contains {@code null} values
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends Set<E>> SetMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(map.values().stream().allMatch(Objects::nonNull), "The specified map contains null value: %s", map);

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
     * SetMultimap<String, Integer> multimap =
     *     SetMultimap.wrap(map, TreeSet::new);
     * multimap.put("a", 3);
     * multimap.put("a", 1);   // Creates TreeSet, values will be sorted
     * }</pre>
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the set
     * @param <V> the type of the set extending Set&lt;E&gt;
     * @param map The map to be wrapped into a SetMultimap
     * @param valueSupplier The supplier that provides the set to be used as the value collection
     * @return a SetMultimap instance backed by the provided map
     * @throws IllegalArgumentException if the provided map or valueSupplier is null
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
     * // inverted: {1 -> ["a", "b"], 2 -> ["a"]}
     * }</pre>
     *
     * @return a new SetMultimap where each original value is mapped to the set of keys that contained it
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
     * Creates a deep copy of this SetMultimap. The returned multimap contains all key-value pairs from the original,
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
     * copy.put("c", 3);   // Does not modify original
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
     * // immutable.get("a") returns ImmutableSet[1, 2]
     * }</pre>
     *
     * @return an ImmutableMap where each key from this multimap is associated with an ImmutableSet
     *         containing all values that were associated with that key
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
     * // Keys will be in sorted order: "a", "b"
     * }</pre>
     *
     * @param mapSupplier a function that creates a new map instance given an initial capacity;
     *                    the function receives the size of this multimap as its argument
     * @return an ImmutableMap where each key from this multimap is associated with an ImmutableSet
     *         containing all values that were associated with that key
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
