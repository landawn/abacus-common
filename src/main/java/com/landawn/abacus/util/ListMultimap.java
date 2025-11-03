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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * A specialized {@link Multimap} implementation that uses {@link List} collections to store multiple values
 * for each key, preserving insertion order and allowing duplicate values. This final class extends the base
 * Multimap to provide list-specific functionality optimized for scenarios where value ordering and duplicates
 * are important.
 *
 * <p>ListMultimap is ideal for use cases where you need to maintain the order in which values were added
 * to each key and allow the same value to be associated with a key multiple times. The underlying list
 * structure provides efficient random access and iteration while preserving temporal relationships.</p>
 *
 * <p><b>Key Characteristics:</b>
 * <ul>
 *   <li><b>Duplicate Values:</b> Allows the same value to be added multiple times for the same key</li>
 *   <li><b>Insertion Order:</b> Maintains the order in which values were added to each key</li>
 *   <li><b>List Semantics:</b> Each key maps to a {@link List} of values with index-based access</li>
 *   <li><b>Efficient Access:</b> O(1) access to first/last elements, O(n) for arbitrary positions</li>
 *   <li><b>Memory Efficient:</b> Only creates lists when values are actually added to keys</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Design Decisions:</b>
 * <ul>
 *   <li>This is a <b>final class</b> that cannot be extended for API stability</li>
 *   <li>Extends {@link Multimap} to inherit common multimap operations</li>
 *   <li>Uses {@link ArrayList} as the default list implementation for optimal performance</li>
 *   <li>Thread safety depends on the backing Map and List implementations chosen</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Event Processing:</b> Maintaining chronological order of events per entity</li>
 *   <li><b>Log Aggregation:</b> Collecting log entries grouped by source with temporal ordering</li>
 *   <li><b>Task Queuing:</b> Managing task lists per worker or category</li>
 *   <li><b>Data Collection:</b> Gathering survey responses or measurements over time</li>
 *   <li><b>Relationship Modeling:</b> Representing one-to-many relationships with order significance</li>
 *   <li><b>Configuration Management:</b> Storing ordered lists of configuration values per key</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic operations with duplicates and ordering
 * ListMultimap<String, Integer> scores = N.newListMultimap();
 * scores.put("Alice", 85);
 * scores.put("Alice", 92);
 * scores.put("Alice", 85);  // duplicate allowed
 * List<Integer> aliceScores = scores.get("Alice");  // [85, 92, 85]
 *
 * // Creating from collections with grouping
 * List<String> words = Arrays.asList("apple", "apricot", "banana", "blueberry");
 * ListMultimap<Character, String> byFirstLetter = 
 *     ListMultimap.create(words, word -> word.charAt(0));
 * // Result: {'a': ["apple", "apricot"], 'b': ["banana", "blueberry"]}
 *
 * // Maintaining chronological order
 * ListMultimap<String, LocalDateTime> userLogins = N.newListMultimap();
 * userLogins.put("john", LocalDateTime.now());
 * userLogins.put("jane", LocalDateTime.now().plusMinutes(5));
 * userLogins.put("john", LocalDateTime.now().plusMinutes(10));
 * // Each user's login times are preserved in chronological order
 *
 * // Filtering and transformations
 * ListMultimap<String, Integer> filtered = scores.filter((key, values) -> values.size() > 2);
 * ListMultimap<String, Integer> transformed = scores.filterByKey(key -> key.startsWith("A"));
 *
 * // Conversion to immutable structures
 * ImmutableMap<String, ImmutableList<Integer>> immutable = scores.toImmutableMap();
 * }</pre>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #of(Object, Object)} - Single key-value pair</li>
 *   <li>{@link #create(Map)} - From existing Map</li>
 *   <li>{@link #create(Collection, Function)} - Grouping by key extractor</li>
 *   <li>{@link #create(Collection, Function, Function)} - Key and value extractors</li>
 *   <li>{@link #concat(Map, Map)} - Concatenating multiple maps</li>
 *   <li>{@link #wrap(Map)} - Wrapping existing Map&lt;K, List&lt;E&gt;&gt;</li>
 *   <li>{@link N#newListMultimap()} - Empty instance with default backing</li>
 * </ul>
 *
 * <p><b>List-Specific Operations:</b>
 * <ul>
 *   <li>{@link #getFirst(Object)} - Get the first value for a key</li>
 *   <li>{@link #getFirstOrDefault(Object, Object)} - Get first value or default</li>
 *   <li>{@link #inverse()} - Invert keys and values while preserving order</li>
 *   <li>{@link #toImmutableMap()} - Convert to immutable representation</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Key lookup: O(1) average time with HashMap backing</li>
 *   <li>Value addition: O(1) amortized time for ArrayList</li>
 *   <li>First value access: O(1) constant time</li>
 *   <li>Iteration: O(n) where n is total number of values</li>
 *   <li>Memory usage: O(k + v) where k is keys and v is total values</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * ListMultimap is <b>not thread-safe</b> by default. For concurrent access:
 * <ul>
 *   <li>Use {@code N.newListMultimap(ConcurrentHashMap::new, ArrayList::new)} for concurrent maps</li>
 *   <li>Wrap individual lists with {@code Collections.synchronizedList()}</li>
 *   <li>Use external synchronization for write operations</li>
 *   <li>Consider using copy-on-write patterns for read-heavy scenarios</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs {@link SetMultimap}:</b> Allows duplicates and preserves order vs. unique values</li>
 *   <li><b>vs {@code Map<K, List<E>>}:</b> Automatic list creation and multimap-specific operations</li>
 *   <li><b>vs Google Guava ListMultimap:</b> Similar API with additional utility methods</li>
 *   <li><b>vs {@link Multiset}:</b> Key-value structure vs. counting structure</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate backing implementations based on access patterns</li>
 *   <li>Consider using {@link #toImmutableMap()} for read-only snapshots</li>
 *   <li>Filter operations create new instances - chain operations efficiently</li>
 *   <li>Use {@link #wrap(Map)} when you already have a suitable Map structure</li>
 *   <li>For large datasets, consider using {@code LinkedList} if insertion performance is critical</li>
 * </ul>
 *
 * <p><b>Memory Considerations:</b>
 * <ul>
 *   <li>Empty keys don't consume memory for value collections</li>
 *   <li>ArrayList backing provides good memory locality for small lists</li>
 *   <li>Use {@code trimToSize()} on value lists if they become large and stable</li>
 *   <li>Consider using primitive collections for numeric values to reduce boxing overhead</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this ListMultimap
 * @param <E> the type of values maintained in the lists associated with each key
 * @see N#newListMultimap()
 * @see N#newListMultimap(Class, Class)
 * @see N#newListMultimap(Supplier, Supplier)
 */
public final class ListMultimap<K, E> extends Multimap<K, E, List<E>> {
    /**
     * Constructs a new instance of ListMultimap with the default initial capacity.
     */
    ListMultimap() {
        this(HashMap.class, ArrayList.class);
    }

    /**
     * Constructs a new instance of ListMultimap with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the ListMultimap.
     */
    ListMultimap(final int initialCapacity) {
        this(N.newHashMap(initialCapacity), ArrayList.class);
    }

    /**
     * Constructs a new instance of ListMultimap with the specified map type and list type.
     *
     * @param mapType The class of the map to be used as the backing map.
     * @param valueType The class of the list to be used as the value collection.
     */
    @SuppressWarnings("rawtypes")
    ListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        super(mapType, valueType);
    }

    /**
     * Constructs a new instance of ListMultimap with the specified map supplier and list supplier.
     *
     * @param mapSupplier The supplier that provides the map to be used as the backing map.
     * @param valueSupplier The supplier that provides the list to be used as the value collection.
     */
    ListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier, final Supplier<? extends List<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    /**
     * Constructs a new instance of ListMultimap with the specified map and list type.
     * This constructor allows the user to specify a custom map and list type for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueType The class of the list to be used as the value collection.
     */
    @Internal
    @SuppressWarnings("rawtypes")
    ListMultimap(final Map<K, List<E>> valueMap, final Class<? extends List> valueType) {
        super(valueMap, valueType2Supplier(valueType));
    }

    /**
     * Constructs a new instance of ListMultimap with the specified map and list supplier.
     * This constructor allows the user to specify a custom map and list supplier for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueSupplier The supplier that provides the list to be used as the value collection.
     */
    @Internal
    ListMultimap(final Map<K, List<E>> valueMap, final Supplier<? extends List<E>> valueSupplier) {
        super(valueMap, valueSupplier);
    }

    /**
     * Creates a new instance of ListMultimap with one key-value pair.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with a single entry.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
     *
     * @param <K> the type of the key
     * @param <E> the type of the value
     * @param k1 the key of the key-value pair
     * @param v1 the value of the key-value pair
     * @return a new instance of ListMultimap with the specified key-value pair
     * @see #of(Object, Object, Object, Object)
     * @see #create(Map)
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1) {
        final ListMultimap<K, E> map = new ListMultimap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with two key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with two entries.
     * If both keys are the same, both values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @return a new instance of ListMultimap with the specified key-value pairs
     * @see #of(Object, Object)
     * @see #create(Map)
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2) {
        final ListMultimap<K, E> map = new ListMultimap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with three key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with three entries.
     * If multiple keys are the same, all corresponding values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @return a new instance of ListMultimap with the specified key-value pairs
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3) {
        final ListMultimap<K, E> map = new ListMultimap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with four key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with four entries.
     * If multiple keys are the same, all corresponding values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
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
     * @return a new instance of ListMultimap with the specified key-value pairs
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4) {
        final ListMultimap<K, E> map = new ListMultimap<>(4);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with five key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with five entries.
     * If multiple keys are the same, all corresponding values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
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
     * @return a new instance of ListMultimap with the specified key-value pairs
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5) {
        final ListMultimap<K, E> map = new ListMultimap<>(5);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with six key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with six entries.
     * If multiple keys are the same, all corresponding values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
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
     * @return a new instance of ListMultimap with the specified key-value pairs
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5, final K k6, final E v6) {
        final ListMultimap<K, E> map = new ListMultimap<>(6);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with seven key-value pairs.
     *
     * <p>This is a convenient factory method for creating a ListMultimap with seven entries.
     * If multiple keys are the same, all corresponding values will be added to the list associated with that key.
     * The returned ListMultimap is mutable and backed by a HashMap and ArrayList.
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
     * @return a new instance of ListMultimap with the specified key-value pairs
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5, final K k6, final E v6, final K k7, final E v7) {
        final ListMultimap<K, E> map = new ListMultimap<>(7);

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
     * Creates a new instance of ListMultimap with the key-value pairs from the specified map.
     *
     * <p>This method converts a regular Map into a ListMultimap where each key is associated with a list containing a single value.
     * The returned ListMultimap uses the same backing map implementation as the source map (e.g., if the source is a LinkedHashMap,
     * the returned ListMultimap will maintain insertion order).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * ListMultimap<String, Integer> multimap = ListMultimap.create(map);
     * // multimap now contains: {"one": [1], "two": [2]}
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param map The map containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified map
     */
    public static <K, E> ListMultimap<K, E> create(final Map<? extends K, ? extends E> map) {
        //noinspection rawtypes
        final ListMultimap<K, E> multimap = new ListMultimap<>(Maps.newTargetMap(map), ArrayList.class);

        if (N.notEmpty(map)) {
            multimap.put(map);
        }

        return multimap;
    }

    /**
     * Creates a new instance of ListMultimap from a given collection and a key mapper function.
     *
     * <p>This method groups elements from the collection by keys generated from the keyExtractor function.
     * Elements sharing the same key are grouped into a list associated with that key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Arrays.asList("apple", "banana", "apricot", "blueberry");
     * ListMultimap<Character, String> multimap = ListMultimap.create(words, s -> s.charAt(0));
     * // multimap contains: {'a': ["apple", "apricot"], 'b': ["banana", "blueberry"]}
     * }</pre>
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the ListMultimap
     * @param c the collection of elements to be added to the ListMultimap
     * @param keyExtractor the function to generate keys for the ListMultimap
     * @return a new instance of ListMultimap with keys and values from the specified collection
     * @throws IllegalArgumentException if the keyExtractor is null
     */
    public static <T, K> ListMultimap<K, T> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        final ListMultimap<K, T> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyExtractor.apply(e), e);
            }
        }

        return multimap;
    }

    /**
     * Creates a new instance of ListMultimap from a given collection, a key mapper function, and a value extractor function.
     *
     * <p>This method transforms elements from the collection into key-value pairs using the provided extractor functions.
     * Elements that map to the same key are grouped into a list associated with that key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String department;
     *     String name;
     *     Person(String dept, String name) { this.department = dept; this.name = name; }
     * }
     * List<Person> people = Arrays.asList(
     *     new Person("Engineering", "Alice"),
     *     new Person("Engineering", "Bob"),
     *     new Person("Sales", "Carol")
     * );
     * ListMultimap<String, String> multimap = ListMultimap.create(people, p -> p.department, p -> p.name);
     * // multimap contains: {"Engineering": ["Alice", "Bob"], "Sales": ["Carol"]}
     * }</pre>
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the ListMultimap
     * @param <E> the type of the values in the ListMultimap
     * @param c the collection of elements to be added to the ListMultimap
     * @param keyExtractor the function to generate keys for the ListMultimap
     * @param valueExtractor the function to extract values for the ListMultimap
     * @return a new instance of ListMultimap with keys and values from the specified collection
     * @throws IllegalArgumentException if the keyExtractor is {@code null} or if the valueExtractor is null
     */
    public static <T, K, E> ListMultimap<K, E> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
            final Function<? super T, ? extends E> valueExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(valueExtractor);

        final ListMultimap<K, E> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyExtractor.apply(e), valueExtractor.apply(e));
            }
        }

        return multimap;
    }

    /**
     * Creates a new instance of ListMultimap by concatenating the key-value pairs from two specified maps.
     *
     * <p>This method combines entries from two maps into a single ListMultimap. If the same key appears in both maps,
     * both values will be added to the list associated with that key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
     * Map<String, Integer> map2 = Map.of("b", 3, "c", 4);
     * ListMultimap<String, Integer> multimap = ListMultimap.concat(map1, map2);
     * // multimap contains: {"a": [1], "b": [2, 3], "c": [4]}
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new ListMultimap
     * @param b the second map containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified maps, or an empty ListMultimap if both maps are null
     */
    public static <K, E> ListMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.newListMultimap() : create(b);
        } else {
            final ListMultimap<K, E> res = create(a);
            res.put(b);
            return res;
        }
    }

    /**
     * Creates a new instance of ListMultimap by concatenating the key-value pairs from three specified maps.
     *
     * <p>This method combines entries from three maps into a single ListMultimap. If the same key appears in multiple maps,
     * all corresponding values will be added to the list associated with that key in the order they appear.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new ListMultimap
     * @param b the second map containing the key-value pairs to be added to the new ListMultimap
     * @param c the third map containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified maps, or an empty ListMultimap if all maps are null
     */
    public static <K, E> ListMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b,
            final Map<? extends K, ? extends E> c) {
        if (a == null) {
            if (b == null) {
                return c == null ? N.newListMultimap() : create(c);
            } else {
                final ListMultimap<K, E> res = create(b);
                res.put(c);
                return res;
            }
        } else {
            final ListMultimap<K, E> res = create(a);
            res.put(b);
            res.put(c);
            return res;
        }
    }

    /**
     * Creates a new instance of ListMultimap by concatenating the key-value pairs from a collection of maps.
     *
     * <p>This method combines entries from all maps in the collection into a single ListMultimap.
     * If the same key appears in multiple maps, all corresponding values will be added to the list
     * associated with that key in the order they are encountered.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param c the collection of maps containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified collection of maps, or an empty ListMultimap if the collection is {@code null} or empty
     */
    public static <K, E> ListMultimap<K, E> concat(final Collection<? extends Map<? extends K, ? extends E>> c) {
        if (N.isEmpty(c)) {
            return N.newListMultimap();
        }

        final Iterator<? extends Map<? extends K, ? extends E>> iter = c.iterator();
        final ListMultimap<K, E> res = create(iter.next());

        while (iter.hasNext()) {
            res.put(iter.next());
        }

        return res;
    }

    /**
     * Wraps the provided map into a ListMultimap. Changes to the specified map will be reflected in the ListMultimap and vice versa.
     *
     * <p>This method creates a view of the provided map as a ListMultimap. The map must contain {@code non-null}, non-empty list values.
     * Any modifications made to the returned ListMultimap will be reflected in the underlying map and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * map.put("a", new ArrayList<>(Arrays.asList(1, 2)));
     * map.put("b", new ArrayList<>(Arrays.asList(3, 4)));
     * ListMultimap<String, Integer> multimap = ListMultimap.wrap(map);
     * multimap.put("a", 5); // modifies the original map
     * }</pre>
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map The map to be wrapped into a ListMultimap
     * @return a new instance of ListMultimap backed by the provided map
     * @throws IllegalArgumentException if the provided map is {@code null} or contains {@code null} or empty list values
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(map.values().stream().noneMatch(v -> v == null || v.isEmpty()), "The specified map contains null or empty value: %s", map);

        final Class<? extends List> valueType = map.isEmpty() ? ArrayList.class : map.values().iterator().next().getClass();

        return new ListMultimap<>((Map<K, List<E>>) map, valueType);
    }

    /**
     * Wraps the provided map into a ListMultimap with a custom list supplier. Changes to the specified map will be reflected in the ListMultimap and vice versa.
     *
     * <p>This method creates a view of the provided map as a ListMultimap with a custom value supplier.
     * Any modifications made to the returned ListMultimap will be reflected in the underlying map and vice versa.
     * The value supplier is used to create new list instances when adding entries to new keys.
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map The map to be wrapped into a ListMultimap
     * @param valueSupplier The supplier that provides the list to be used as the value collection
     * @return a new instance of ListMultimap backed by the provided map
     * @throws IllegalArgumentException if the provided map or valueSupplier is null
     */
    @Beta
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(valueSupplier, cs.valueSupplier);

        return new ListMultimap<>((Map<K, List<E>>) map, valueSupplier);
    }

    /**
     * Returns the first value for the given key, or {@code null} if no value is found.
     *
     * <p>Since ListMultimap maintains values in a list, this method returns the first element
     * from the list associated with the specified key. If the key is not present or the list is empty,
     * {@code null} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
     * multimap.put("a", 2);
     * multimap.getFirst("a"); // returns 1
     * multimap.getFirst("b"); // returns null
     * }</pre>
     *
     * @param key the key whose associated value is to be returned
     * @return the first value associated with the specified key, or {@code null} if the key has no associated values
     */
    @Override
    public E getFirst(final K key) {
        final List<E> values = backingMap.get(key);
        return N.isEmpty(values) ? null : values.get(0);
    }

    /**
     * Returns the first value for the given key, or {@code defaultValue} if no value is found.
     *
     * <p>Since ListMultimap maintains values in a list, this method returns the first element
     * from the list associated with the specified key. If the key is not present or the list is empty,
     * the provided default value is returned instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
     * multimap.getFirstOrDefault("a", 99); // returns 1
     * multimap.getFirstOrDefault("b", 99); // returns 99
     * }</pre>
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default value to return if no value is associated with the key
     * @return the first value associated with the specified key, or the default value if the key has no associated values
     */
    @Override
    public E getFirstOrDefault(final K key, final E defaultValue) {
        final List<E> values = backingMap.get(key);
        return N.isEmpty(values) ? defaultValue : values.get(0);
    }

    /**
     * Inverts the ListMultimap, swapping keys with values.
     *
     * <p>This method creates a new ListMultimap where each value from the original multimap becomes a key,
     * and the associated original key becomes a value in the new multimap. If multiple keys in the original
     * multimap share the same value, that value will be associated with multiple keys in the inverted multimap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
     * ListMultimap<Integer, String> inverted = multimap.inverse();
     * // inverted contains: {1: ["a", "b"], 2: ["a"]}
     * }</pre>
     *
     * @return a new instance of ListMultimap where the original keys are now values and the original values are now keys
     */
    public ListMultimap<E, K> inverse() {
        final ListMultimap<K, E> multimap = this;
        //noinspection rawtypes
        final ListMultimap<E, K> result = new ListMultimap<>(Maps.newOrderingMap(backingMap), valueSupplier);

        if (N.notEmpty(multimap)) {
            for (final Map.Entry<K, List<E>> entry : multimap.entrySet()) {
                final List<E> c = entry.getValue();

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
     * Returns a new ListMultimap and copies all key-value pairs from this ListMultimap to the new one.
     *
     * <p>This method creates a deep copy of the ListMultimap, including all keys and their associated values.
     * The new ListMultimap uses the same map supplier and value supplier as the original, ensuring it has
     * the same structural and hash characteristics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> original = ListMultimap.of("a", 1, "b", 2);
     * ListMultimap<String, Integer> copy = original.copy();
     * copy.put("c", 3); // does not affect original
     * }</pre>
     *
     * @return a new ListMultimap containing all the key-value pairs of this ListMultimap
     * @see #putMany(Multimap)
     */
    @Override
    public ListMultimap<K, E> copy() {
        final ListMultimap<K, E> copy = new ListMultimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Filters the ListMultimap based on the provided key-value pair filter.
     *
     * <p>This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap
     * that satisfy the provided predicate. The predicate is applied to each key and its entire associated list.
     * The new ListMultimap has the same structural and hash characteristics as the current one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
     * ListMultimap<String, Integer> filtered = multimap.filter((k, v) -> v.size() > 1);
     * // filtered contains only: {"a": [1, 2]}
     * }</pre>
     *
     * @param filter the predicate to be applied to each key-value pair in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap
     * @return a new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided key-value pair filter
     */
    @Override
    public ListMultimap<K, E> filter(final BiPredicate<? super K, ? super List<E>> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.putMany(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the ListMultimap based on the provided key filter.
     *
     * <p>This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap
     * where the key satisfies the provided predicate. The new ListMultimap has the same structural and
     * hash characteristics as the current one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("apple", 1, "apricot", 2, "banana", 3);
     * ListMultimap<String, Integer> filtered = multimap.filterByKey(k -> k.startsWith("a"));
     * // filtered contains: {"apple": [1], "apricot": [2]}
     * }</pre>
     *
     * @param filter the predicate to be applied to each key in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap
     * @return a new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided key filter
     */
    @Override
    public ListMultimap<K, E> filterByKey(final Predicate<? super K> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.putMany(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the ListMultimap based on the provided value filter.
     *
     * <p>This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap
     * where the entire list of values associated with a key satisfies the provided predicate.
     * The new ListMultimap has the same structural and hash characteristics as the current one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
     * ListMultimap<String, Integer> filtered = multimap.filterByValue(list -> list.size() >= 2);
     * // filtered contains: {"a": [1, 2]}
     * }</pre>
     *
     * @param filter the predicate to be applied to each value list in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap
     * @return a new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided value filter
     */
    @Override
    public ListMultimap<K, E> filterByValue(final Predicate<? super List<E>> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getValue())) {
                result.putMany(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Converts the current ListMultimap into an ImmutableMap.
     *
     * <p>Each key-value pair in the ListMultimap is transformed into a key-ImmutableList pair in the ImmutableMap.
     * The ImmutableList contains all the values associated with the key in the ListMultimap.
     * The returned ImmutableMap cannot be modified after creation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
     * ImmutableMap<String, ImmutableList<Integer>> immutable = multimap.toImmutableMap();
     * // immutable contains: {"a": ImmutableList[1, 2], "b": ImmutableList[3]}
     * }</pre>
     *
     * @return an ImmutableMap where each key is associated with an ImmutableList of values from the original ListMultimap
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap() {
        final Map<K, ImmutableList<E>> map = Maps.newTargetMap(backingMap);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    /**
     * Converts the current ListMultimap into an ImmutableMap using a provided map supplier.
     *
     * <p>Each key-value pair in the ListMultimap is transformed into a key-ImmutableList pair in the ImmutableMap.
     * The ImmutableList contains all the values associated with the key in the ListMultimap.
     * This method allows control over the underlying map implementation used for the ImmutableMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
     * ImmutableMap<String, ImmutableList<Integer>> immutable =
     *     multimap.toImmutableMap(LinkedHashMap::new);
     * // uses LinkedHashMap to preserve insertion order
     * }</pre>
     *
     * @param mapSupplier The supplier function that provides a Map instance. The function takes an integer argument which is the initial size of the map
     * @return an ImmutableMap where each key is associated with an ImmutableList of values from the original ListMultimap
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableList<E>>> mapSupplier) {
        final Map<K, ImmutableList<E>> map = mapSupplier.apply(backingMap.size());

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    //    public ListMultimap<E, K> inversed() {
}
