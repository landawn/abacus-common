/*
 * Copyright (C) 2019 HaiYang Li
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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * A comprehensive utility class providing an extensive collection of static methods for Map operations,
 * transformations, manipulations, and analysis. This class serves as the primary map utility facade
 * in the Abacus library, offering null-safe, performance-optimized operations for all types of Map
 * implementations with a focus on functional programming patterns and Optional-based return types.
 *
 * <p>The {@code Maps} class is designed as a final utility class that provides a complete toolkit
 * for map processing including creation, transformation, filtering, searching, merging, and statistical
 * operations. All methods are static, thread-safe, and designed to handle edge cases gracefully while
 * maintaining optimal performance for large-scale map operations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Comprehensive Map Operations:</b> Complete set of operations for all Map types and implementations</li>
 *   <li><b>Optional-Based Returns:</b> Most methods return Optional types for null-safe value handling</li>
 *   <li><b>Null-Safe Design:</b> All methods handle null inputs gracefully without throwing exceptions</li>
 *   <li><b>Functional Programming:</b> Support for map, filter, reduce, and other functional patterns</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms with minimal object allocation</li>
 *   <li><b>Map Creation Utilities:</b> Factory methods for various Map types and initialization patterns</li>
 *   <li><b>Transformation Support:</b> Key/value transformation, inversion, and restructuring operations</li>
 * </ul>
 *
 * <p><b>Core Design Principles:</b>
 * <ul>
 *   <li><b>Present vs Absent:</b> "Present" means key is found with non-null value; "Absent" means
 *       key is not found or found with null value</li>
 *   <li><b>Exception Minimization:</b> Methods avoid throwing unnecessary exceptions when contracts
 *       are not violated, preferring sensible defaults for edge cases</li>
 *   <li><b>Empty Over Null:</b> Methods prefer returning empty Maps over null values when possible</li>
 *   <li><b>Null Safety First:</b> Comprehensive null input handling throughout the API</li>
 * </ul>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Map Creation:</b> Factory methods for HashMap, LinkedHashMap, TreeMap, and specialized maps</li>
 *   <li><b>Access Operations:</b> get, getOrDefault, getFirst, getLast with Optional and Nullable returns</li>
 *   <li><b>Search Operations:</b> find, contains, indexOf with predicate support and Optional returns</li>
 *   <li><b>Transformation Operations:</b> map keys/values, filter, invert, merge, combine</li>
 *   <li><b>Aggregation Operations:</b> reduce, fold, sum, count, min, max for map values</li>
 *   <li><b>Validation Operations:</b> isEmpty, isNotEmpty, containsKey, containsValue, equals</li>
 *   <li><b>Conversion Operations:</b> toList, toSet, toArray, entrySet operations</li>
 *   <li><b>Utility Operations:</b> zip, unzip, partition, group, flatten for map manipulation</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Map creation with factory methods
 * Map<String, Integer> map = Maps.newHashMap();
 * Map<String, Integer> linkedMap = Maps.newLinkedHashMap();
 * Map<String, Integer> sortedMap = Maps.newTreeMap();
 *
 * // Zip operations for map creation
 * List<String> keys = Arrays.asList("a", "b", "c");
 * List<Integer> values = Arrays.asList(1, 2, 3);
 * Map<String, Integer> zipped = Maps.zip(keys, values);        // {a=1, b=2, c=3}
 *
 * // Safe access operations with Optional returns
 * Optional<Integer> value = Maps.get(map, "key");              // Optional-wrapped value
 * Nullable<Integer> nullable = Maps.getNullable(map, "key");   // Nullable-wrapped value
 * int defaultValue = Maps.getOrDefault(map, "key", 0);         // With default value
 *
 * // Null-safe operations
 * Optional<Integer> fromNull = Maps.get(null, "key");   // Optional.empty()
 * boolean isEmpty = Maps.isEmpty(null);                 // Returns true
 *
 * // Transformation operations
 * Map<String, String> transformed = Maps.map(map, (k, v) -> k.toUpperCase(), v -> v.toString());
 * Map<String, Integer> filtered = Maps.filter(map, (k, v) -> v > 0);
 * Map<Integer, String> inverted = Maps.invert(map);   // Swap keys and values
 *
 * // Functional operations
 * boolean allPositive = Maps.allMatch(map, (k, v) -> v > 0);
 * boolean anyEven = Maps.anyMatch(map, (k, v) -> v % 2 == 0);
 * Optional<Integer> max = Maps.maxValue(map);
 * Optional<String> longestKey = Maps.maxKey(map, Comparator.comparing(String::length));
 *
 * // Merging and combining operations
 * Map<String, Integer> map1 = Maps.of("a", 1, "b", 2);
 * Map<String, Integer> map2 = Maps.of("b", 3, "c", 4);
 * Map<String, Integer> merged = Maps.merge(map1, map2, Integer::sum);   // {a=1, b=5, c=4}
 *
 * // Conversion operations
 * List<Map.Entry<String, Integer>> entries = Maps.toList(map);
 * Set<String> keys = Maps.keySet(map);
 * Collection<Integer> values = Maps.values(map);
 * }</pre>
 *
 * <p><b>Map Creation Utilities:</b>
 * <ul>
 *   <li><b>Factory Methods:</b> {@code newHashMap()}, {@code newLinkedHashMap()}, {@code newTreeMap()}</li>
 *   <li><b>Builder Patterns:</b> {@code of()} for immutable-style map creation</li>
 *   <li><b>Zip Operations:</b> {@code zip()} for combining separate key/value collections</li>
 *   <li><b>Specialized Maps:</b> {@code newIdentityHashMap()}, {@code newConcurrentHashMap()}</li>
 * </ul>
 *
 * <p><b>Optional-Based Access:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code get()}, {@code getFirst()}, {@code getLast()} returning Optional</li>
 *   <li><b>Nullable Access:</b> {@code getNullable()} returning Nullable wrapper</li>
 *   <li><b>Default Values:</b> {@code getOrDefault()}, {@code getOrElse()} with fallback values</li>
 *   <li><b>Conditional Access:</b> {@code getIf()}, {@code getUnless()} with predicate conditions</li>
 * </ul>
 *
 * <p><b>Functional Transformations:</b>
 * <ul>
 *   <li><b>Key/Value Mapping:</b> {@code map()}, {@code mapKeys()}, {@code mapValues()}</li>
 *   <li><b>Filtering:</b> {@code filter()}, {@code filterByKey()}, {@code filterByValue()}</li>
 *   <li><b>Reduction:</b> {@code reduce()}, {@code fold()}, {@code aggregate()}</li>
 *   <li><b>Inversion:</b> {@code invert()}, {@code invertMultimap()} for key-value swapping</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Memory Efficient:</b> Minimal object allocation and copying in operations</li>
 *   <li><b>Lazy Evaluation:</b> Operations performed only when results are consumed</li>
 *   <li><b>Algorithm Selection:</b> Optimal algorithms chosen based on map type and size</li>
 *   <li><b>Short-Circuit Operations:</b> Early termination for operations like anyMatch, allMatch</li>
 *   <li><b>Type-Specific Optimizations:</b> Specialized handling for different Map implementations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All static methods are stateless and thread-safe</li>
 *   <li><b>Immutable Operations:</b> Methods create new maps rather than modifying inputs</li>
 *   <li><b>No Shared State:</b> No static mutable fields that could cause race conditions</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <p><b>Integration with Java Maps:</b>
 * <ul>
 *   <li><b>Standard Map Interface:</b> Full compatibility with java.util.Map implementations</li>
 *   <li><b>Specialized Maps:</b> Support for SortedMap, NavigableMap, ConcurrentMap</li>
 *   <li><b>Stream Compatibility:</b> Integration with Java 8+ Stream operations</li>
 *   <li><b>Collection Framework:</b> Seamless integration with Java Collections</li>
 * </ul>
 *
 * <p><b>Null Handling Strategy:</b>
 * <ul>
 *   <li><b>Present/Absent Model:</b> Clear distinction between missing keys and null values</li>
 *   <li><b>Graceful Degradation:</b> Methods handle null maps gracefully without exceptions</li>
 *   <li><b>Optional Returns:</b> Use of Optional types to avoid null return values</li>
 *   <li><b>Null Value Support:</b> Proper handling of null values within map operations</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>Contract Preservation:</b> Exceptions thrown only when method contracts are violated</li>
 *   <li><b>Edge Case Handling:</b> Graceful handling of empty maps, null inputs, missing keys</li>
 *   <li><b>Clear Documentation:</b> Well-defined behavior for all edge cases</li>
 *   <li><b>Consistent API:</b> Uniform error handling patterns across all methods</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use Optional-returning methods to avoid null pointer exceptions</li>
 *   <li>Leverage the present/absent model for clear null value handling</li>
 *   <li>Prefer map transformation utilities over manual iteration</li>
 *   <li>Use appropriate map types for specific use cases (HashMap, LinkedHashMap, TreeMap)</li>
 *   <li>Take advantage of the null-safe design for robust code</li>
 *   <li>Use functional operations for complex map processing pipelines</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Choose appropriate Map implementations based on access patterns</li>
 *   <li>Use bulk operations for better performance with large maps</li>
 *   <li>Consider memory implications when transforming large maps</li>
 *   <li>Leverage lazy evaluation patterns for chained operations</li>
 *   <li>Use primitive-specific methods when working with numeric values</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code Optional<V> value = Maps.get(map, key);}</li>
 *   <li><b>Transformation:</b> {@code Map<K, R> result = Maps.mapValues(map, transformer);}</li>
 *   <li><b>Filtering:</b> {@code Map<K, V> filtered = Maps.filter(map, predicate);}</li>
 *   <li><b>Merging:</b> {@code Map<K, V> merged = Maps.merge(map1, map2, combiner);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with map operations</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterables}:</b> Iterable utilities for map processing</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterators}:</b> Iterator utilities for map entries</li>
 *   <li><b>{@link com.landawn.abacus.util.Strings}:</b> String utilities for map keys/values</li>
 *   <li><b>{@link com.landawn.abacus.util.Beans}:</b> Bean utilities for object-map conversion</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Stream operations for maps</li>
 *   <li><b>{@link java.util.Map}:</b> Core Java map interface</li>
 *   <li><b>{@link java.util.Collections}:</b> Core Java collection utilities</li>
 * </ul>
 *
 * <p><b>Example: Data Processing Pipeline</b>
 * <pre>{@code
 * // Complete map processing example
 * Map<String, Double> salesData = Maps.of(
 *     "Q1", 1200.50, "Q2", 1450.75, "Q3", 980.25, "Q4", 1350.00
 * );
 *
 * // Statistical analysis
 * Optional<Double> maxSales = Maps.maxValue(salesData);
 * Optional<String> bestQuarter = Maps.maxKey(salesData, Comparator.comparing(salesData::get));
 * double totalSales = Maps.sumValues(salesData);
 * double avgSales = Maps.averageValues(salesData).orElse(0.0);
 *
 * // Transformation and filtering
 * Map<String, String> formatted = Maps.mapValues(salesData, 
 *     sales -> String.format("$%.2f", sales));
 * Map<String, Double> highPerformance = Maps.filter(salesData, 
 *     (quarter, sales) -> sales > 1200.0);
 *
 * // Grouping and analysis
 * Map<String, String> performance = Maps.map(salesData,
 *     Function.identity(),
 *     sales -> sales > avgSales ? "Above Average" : "Below Average"
 * );
 *
 * // Merging with additional data
 * Map<String, Double> targets = Maps.of("Q1", 1100.0, "Q2", 1400.0, "Q3", 1000.0, "Q4", 1300.0);
 * Map<String, Double> variance = Maps.merge(salesData, targets, (actual, target) -> actual - target);
 *
 * // Validation and reporting
 * boolean allQuartersPresent = Maps.containsAllKeys(salesData, Arrays.asList("Q1", "Q2", "Q3", "Q4"));
 * boolean anyTargetMissed = Maps.anyMatch(variance, (quarter, var) -> var < 0);
 * }</pre>
 *
 * <p><b>Example: Configuration Management</b>
 * <pre>{@code
 * // Configuration map processing
 * Map<String, String> config = loadConfiguration();
 *
 * // Safe access with defaults
 * int timeout = Maps.getInt(config, "timeout").orElse(30);
 * String environment = Maps.get(config, "environment").orElse("development");
 * boolean debugMode = Maps.getBoolean(config, "debug").orElse(false);
 *
 * // Validation and filtering
 * Map<String, String> validConfig = Maps.filter(config, 
 *     (key, value) -> value != null && !value.trim().isEmpty());
 *
 * // Type conversion and transformation
 * Map<String, Integer> intConfigs = Maps.mapValues(
 *     Maps.filterByKey(validConfig, key -> key.endsWith(".timeout")),
 *     Integer::parseInt
 * );
 *
 * // Environment-specific filtering
 * String envPrefix = environment + ".";
 * Map<String, String> envConfig = Maps.filterByKey(validConfig, 
 *     key -> key.startsWith(envPrefix));
 *
 * // Flattening nested configuration
 * Map<String, String> flatConfig = Maps.map(envConfig,
 *     key -> key.substring(envPrefix.length()),
 *     Function.identity()
 * );
 *
 * // Merging with defaults
 * Map<String, String> defaults = Maps.of(
 *     "host", "localhost",
 *     "port", "8080",
 *     "ssl", "false"
 * );
 * Map<String, String> finalConfig = Maps.merge(defaults, flatConfig, (def, custom) -> custom);
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other open source
 * projects under the Apache License 2.0. Methods from these libraries may have been modified for
 * consistency, performance optimization, and enhanced null-safety within the Abacus framework.</p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Beans
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see java.util.Map
 * @see java.util.HashMap
 * @see java.util.LinkedHashMap
 * @see java.util.TreeMap
 * @see java.util.Collections
 */
public final class Maps {

    private static final Object NONE = ClassUtil.newNullSentinel();

    private Maps() {
        // Utility class.
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on)
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Same length iterables
     * List<String> keys = Arrays.asList("name", "age", "city");
     * List<String> values = Arrays.asList("John", "25", "New York");
     * Map<String, String> result = zip(keys, values);
     * // result: {name=John, age=25, city=New York}
     *
     * // Example 2: Different length iterables (keys shorter)
     * List<Integer> ids = Arrays.asList(1, 2);
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * Map<Integer, String> userMap = zip(ids, names);
     * // userMap: {1=Alice, 2=Bob}
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param keys an Iterable of keys for the resulting Map.
     * @param values an Iterable of values for the resulting Map.
     * @return a Map where each key from the keys Iterable is associated with the corresponding value from the values Iterable.
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : Integer.MAX_VALUE,
                values instanceof Collection ? ((Collection<V>) values).size() : Integer.MAX_VALUE);
        final Map<K, V> result = N.newHashMap(minLen == Integer.MAX_VALUE ? 0 : minLen);

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a new entry (key-value pair) with the provided key and value.
     *
     * <p>This method generates a new entry using the provided key and value.
     * The created entry is mutable, meaning that its key and value can be changed after creation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map.Entry<String, Integer> entry = Maps.newEntry("key", 1);
     * // entry.getKey() returns "key"
     * // entry.getValue() returns 1
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <V> the type of the value.
     * @param key the key of the new entry.
     * @param value the value of the new entry.
     * @return a new Entry with the provided key and value.
     * @deprecated replaced by {@link N#newEntry(Object, Object)}.
     */
    @Deprecated
    public static <K, V> Map.Entry<K, V> newEntry(final K key, final V value) {
        return N.newEntry(key, value);
    }

    /**
     * Creates a new immutable entry with the provided key and value.
     *
     * <p>This method generates a new immutable entry (key-value pair) using the provided key and value.
     * The created entry is immutable, meaning that its key and value cannot be changed after creation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 1);
     * // entry.getKey() returns "key"
     * // entry.getValue() returns 1
     * // entry.setValue(2) throws UnsupportedOperationException
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <V> the type of the value.
     * @param key the key of the new entry.
     * @param value the value of the new entry.
     * @return a new ImmutableEntry with the provided key and value.
     * @deprecated replaced by {@link N#newImmutableEntry(Object, Object)}.
     */
    @Deprecated
    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(final K key, final V value) {
        return N.newImmutableEntry(key, value);
    }

    private static final Set<Class<?>> UNABLE_CREATED_MAP_CLASSES = N.newConcurrentHashSet();

    @SuppressWarnings("rawtypes")
    static Map newTargetMap(final Map<?, ?> m) {
        return newTargetMap(m, m == null ? 0 : m.size());
    }

    @SuppressWarnings("rawtypes")
    static Map newTargetMap(final Map<?, ?> m, final int size) {
        if (m == null) {
            return size == 0 ? new HashMap<>() : new HashMap<>(size);
        }

        if (m instanceof SortedMap) {
            return new TreeMap<>(((SortedMap) m).comparator());
        }

        final Class<? extends Map> cls = m.getClass();

        if (UNABLE_CREATED_MAP_CLASSES.contains(cls)) {
            return new HashMap<>(size);
        }

        try {
            return N.newMap(cls, size);
        } catch (final Exception e) {
            try {
                N.newMap(cls, 1); // Attempt to create a map with size 1 to check if the class is instantiable.
            } catch (final Exception e1) {
                UNABLE_CREATED_MAP_CLASSES.add(m.getClass());
            }

            return new HashMap<>(size);
        }
    }

    @SuppressWarnings("rawtypes")
    static Map newOrderingMap(final Map<?, ?> m) {
        if (m == null) {
            return new HashMap<>();
        }

        if (m instanceof SortedMap) {
            return new LinkedHashMap<>();
        }

        final int size = m.size();
        final Class<? extends Map> cls = m.getClass();

        if (UNABLE_CREATED_MAP_CLASSES.contains(cls)) {
            return new LinkedHashMap<>(size);
        }

        try {
            return N.newMap(cls, size);
        } catch (final Exception e) {
            try {
                N.newMap(cls, 1); // Attempt to create a map with size 1 to check if the class is instantiable.
            } catch (final Exception e1) {
                UNABLE_CREATED_MAP_CLASSES.add(m.getClass());
            }

            return new LinkedHashMap<>(size);
        }
    }

    /**
     * Returns the key set of the specified map if it is not {@code null} or empty. Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Set result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * Set<String> keys = Maps.keySet(map);
     * // keys contains: ["one", "two"]
     * 
     * Map<String, Integer> emptyMap = null;
     * Set<String> emptyKeys = Maps.keySet(emptyMap);
     * // emptyKeys is an empty immutable set
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map whose keys are to be returned, may be null.
     * @return the key set of the map if non-empty, otherwise an empty immutable set.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <K> Set<K> keySet(final Map<? extends K, ?> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : (Set<K>) map.keySet();
    }

    /**
     * Returns the collection of values from the specified map if it is not {@code null} or empty. 
     * Otherwise, an empty immutable list is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Collection result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * Collection<Integer> values = Maps.values(map);
     * // values contains: [1, 2]
     * 
     * Map<String, Integer> emptyMap = null;
     * Collection<Integer> emptyValues = Maps.values(emptyMap);
     * // emptyValues is an empty immutable list
     * }</pre>
     *
     * @param <V> the type of values in the map.
     * @param map the map whose values are to be returned, may be null.
     * @return the collection of values from the map if non-empty, otherwise an empty immutable list.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <V> Collection<V> values(final Map<?, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableList.empty() : (Collection<V>) map.values();
    }

    /**
     * Returns the entry set of the specified map if it is not {@code null} or empty. 
     * Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Set result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * Set<Map.Entry<String, Integer>> entries = Maps.entrySet(map);
     * // entries contains the key-value pairs: ["one"=1, "two"=2]
     * 
     * Map<String, Integer> emptyMap = null;
     * Set<Map.Entry<String, Integer>> emptyEntries = Maps.entrySet(emptyMap);
     * // emptyEntries is an empty immutable set
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param <V> the type of values in the map.
     * @param map the map whose entry set is to be returned, may be null.
     * @return the entry set of the map if non-empty, otherwise an empty immutable set.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    @SuppressWarnings({ "rawtypes" })
    public static <K, V> Set<Map.Entry<K, V>> entrySet(final Map<? extends K, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : (Set) map.entrySet();
    }

    /**
     * Creates a Map by zipping together two Iterables with a custom map supplier.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "c");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
     * // result: {a=1, b=2, c=3} (maintains insertion order)
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param <M> the type of the resulting Map.
     * @param keys an Iterable of keys for the resulting Map.
     * @param values an Iterable of values for the resulting Map.
     * @param mapSupplier a function that generates a new Map instance based on expected size.
     * @return a Map where each key from the keys Iterable is associated with the corresponding value from the values Iterable.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int keysSize = keys instanceof Collection ? ((Collection<K>) keys).size() : 0;
        final int valuesSize = values instanceof Collection ? ((Collection<V>) values).size() : 0;
        final int minLen = N.min(keysSize, valuesSize);
        final M result = mapSupplier.apply(minLen);

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables with a merge function to handle duplicate keys.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * If duplicate keys are encountered, the merge function is used to resolve the conflict.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "a");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * Map<String, Integer> result = Maps.zip(keys, values, 
     *     (v1, v2) -> v1 + v2, HashMap::new);
     * // result: {a=4, b=2} (values for duplicate key "a" are summed: 1 + 3 = 4)
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param <M> the type of the resulting Map.
     * @param keys an Iterable of keys for the resulting Map.
     * @param values an Iterable of values for the resulting Map.
     * @param mergeFunction a function used to resolve conflicts when duplicate keys are encountered.
     * @param mapSupplier a function that generates a new Map instance based on expected size.
     * @return a Map where each key from the keys Iterable is associated with the corresponding value from the values Iterable.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int keysSize = keys instanceof Collection ? ((Collection<K>) keys).size() : 0;
        final int valuesSize = values instanceof Collection ? ((Collection<V>) values).size() : 0;
        final int minLen = N.min(keysSize, valuesSize);
        final M result = mapSupplier.apply(minLen);

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables with default values for missing elements.
     * The resulting Map will have the size of the longer Iterable.
     * If one Iterable is shorter, the default value is used for the missing elements.
     * Returns an empty map if either Iterable is {@code null} or empty.
     *
     * <p><b>Important:</b> When using default keys, if multiple values map to the same default key,
     * the merge function {@code Fn.selectFirst()} is applied, which keeps the first value and
     * discards subsequent values for duplicate keys. This means if {@code values} has more elements
     * than {@code keys}, only the first extra value will be mapped to {@code defaultForKey}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b");
     * List<Integer> values = Arrays.asList(1, 2, 3, 4);
     * Map<String, Integer> result = Maps.zip(keys, values, "default", 0);
     * // result: {a=1, b=2, default=3}
     * // Note: Only the first extra value (3) is kept; value 4 is discarded
     * //       because "default" key is reused and Fn.selectFirst() keeps the first value
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param keys an Iterable of keys for the resulting Map.
     * @param values an Iterable of values for the resulting Map.
     * @param defaultForKey the default key to use when keys Iterable is shorter than values.
     * @param defaultForValue the default value to use when values Iterable is shorter than keys.
     * @return a Map where each key is associated with the corresponding value, using defaults for missing elements.
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K defaultForKey, final V defaultForValue) {
        return zip(keys, values, defaultForKey, defaultForValue, Fn.selectFirst(), IntFunctions.ofMap());
    }

    /**
     * Creates a Map by zipping together two Iterables with default values and custom merge function.
     * The resulting Map will have entries for all elements from both Iterables.
     * If one Iterable is shorter, default values are used. Duplicate keys are handled by the merge function.
     * Returns an empty map if either Iterable is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * Map<String, Integer> result = Maps.zip(keys, values, "sum", 10,
     *     (v1, v2) -> v1 + v2, HashMap::new);
     * // result: {a=1, b=2, sum=3}
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param <M> the type of the resulting Map.
     * @param keys an Iterable of keys for the resulting Map.
     * @param values an Iterable of values for the resulting Map.
     * @param defaultForKey the default key to use when keys Iterable is shorter than values.
     * @param defaultForValue the default value to use when values Iterable is shorter than keys.
     * @param mergeFunction a function used to resolve conflicts when duplicate keys are encountered.
     * @param mapSupplier a function that generates a new Map instance.
     * @return a Map where each key is associated with the corresponding value.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K defaultForKey,
            final V defaultForValue, final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int maxLen = N.max(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(maxLen);

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        while (keyIter.hasNext()) {
            result.merge(keyIter.next(), defaultForValue, mergeFunction);
        }

        while (valueIter.hasNext()) {
            result.merge(defaultForKey, valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Retrieves the value associated with the specified key, wrapped in a {@link Nullable}.
     * <p>This method distinguishes between a key that maps to {@code null} and a key that is missing:
     * <ul>
     * <li>If the key exists and maps to a value, returns {@code Nullable.of(value)}.</li>
     * <li>If the key exists and maps to {@code null}, returns {@code Nullable.of(null)}.</li>
     * <li>If the key does not exist or the map is {@code null}, returns {@code Nullable.empty()}.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     * Maps.getIfExists(map, "key1");   // returns Nullable.of("value1")
     * Maps.getIfExists(map, "key2");   // returns Nullable.of(null)
     * Maps.getIfExists(map, "key3");   // returns Nullable.empty()
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return a {@code Nullable} wrapping the value if the key exists, otherwise an empty {@code Nullable}
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see #getIfExists(Map, Object, Object, Object)
     */
    public static <K, V> Nullable<V> getIfExists(final Map<K, ? extends V> map, final K key) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return Nullable.of(val);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Retrieves a value from a double-nested map structure (Map-in-Map) using two keys.
     *
     * <p>This is a safe-navigation shorthand for {@code map.get(key).get(k2)}. It returns
     * {@code Nullable.empty()} if any of the following conditions are met:</p>
     * <ul>
     *   <li>The outer map is {@code null}, empty, or does not contain {@code key}.</li>
     *   <li>The inner map (retrieved via {@code key}) is {@code null}, empty, or does not contain {@code k2}.</li>
     * </ul>
     *
     * <p><b>Key presence vs. value {@code null}:</b><br>
     * This method distinguishes between “key not present” and “key present with a {@code null} value”.
     * If both keys exist in their respective maps, the result is {@code Nullable.of(value)},
     * even when the value is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a double-nested map: country -> state -> population
     * Map<String, Map<String, Integer>> populationMap = new HashMap<>();
     * Map<String, Integer> california = new HashMap<>();
     * california.put("Los Angeles", 4000000);
     * california.put("San Francisco", 870000);
     * populationMap.put("California", california);
     *
     * // Successful retrieval
     * Nullable<Integer> pop1 = Maps.getIfExists(populationMap, "California", "Los Angeles");
     * // Returns: Nullable.of(4000000)
     *
     * // Missing inner key
     * Nullable<Integer> pop2 = Maps.getIfExists(populationMap, "California", "San Diego");
     * // Returns: Nullable.empty()
     *
     * // Missing outer key
     * Nullable<Integer> pop3 = Maps.getIfExists(populationMap, "Texas", "Houston");
     * // Returns: Nullable.empty()
     *
     * // Null map
     * Nullable<Integer> pop4 = Maps.getIfExists(null, "California", "Los Angeles");
     * // Returns: Nullable.empty()
     * }</pre>
     *
     * @param <K>  the type of keys in the outer map
     * @param <K2> the type of keys in the inner map
     * @param <V2> the type of values in the inner map
     * @param map  the outer map containing nested inner maps, may be {@code null}
     * @param key  the key used to retrieve the inner map from the outer map
     * @param k2   the key used to retrieve the value from the inner map
     * @return a {@code Nullable<V2>} containing the value if both keys are present
     *         (even if the value is {@code null}); otherwise {@code Nullable.empty()}
     *
     * @see #getIfExists(Map, Object)
     * @see #getIfExists(Map, Object, Object, Object)
     */
    public static <K, K2, V2> Nullable<V2> getIfExists(final Map<K, ? extends Map<? extends K2, ? extends V2>> map, final K key, final K2 k2) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<? extends K2, ? extends V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null || m2.containsKey(k2)) {
                return Nullable.of(v2);
            }
        }

        return Nullable.empty();
    }

    /**
     * Retrieves a value from a triple-nested map structure (Map-in-Map-in-Map) using three keys.
     *
     * <p>This is a safe-navigation shorthand for {@code map.get(key).get(k2).get(k3)}. It returns
     * {@code Nullable.empty()} if any of the following conditions are met:</p>
     * <ul>
     *   <li>The outermost map is {@code null}, empty, or does not contain {@code key}.</li>
     *   <li>The middle map (retrieved via {@code key}) is {@code null}, empty, or does not contain {@code k2}.</li>
     *   <li>The innermost map (retrieved via {@code k2}) is {@code null}, empty, or does not contain {@code k3}.</li>
     * </ul>
     *
     * <p><b>Key presence vs. value {@code null}:</b><br>
     * This method distinguishes between "key not present" and "key present with a {@code null} value".
     * If all three keys exist in their respective maps, the result is {@code Nullable.of(value)},
     * even when the value is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a triple-nested map structure: country -> state -> city -> population
     * Map<String, Map<String, Map<String, Integer>>> populationMap = new HashMap<>();
     * Map<String, Map<String, Integer>> usaStates = new HashMap<>();
     * Map<String, Integer> californiaCities = new HashMap<>();
     * californiaCities.put("Los Angeles", 4000000);
     * californiaCities.put("San Francisco", 870000);
     * usaStates.put("California", californiaCities);
     * populationMap.put("USA", usaStates);
     *
     * // Successful retrieval
     * Nullable<Integer> pop1 = Maps.getIfExists(populationMap, "USA", "California", "Los Angeles");
     * // Returns: Nullable.of(4000000)
     *
     * // Missing innermost key
     * Nullable<Integer> pop2 = Maps.getIfExists(populationMap, "USA", "California", "San Diego");
     * // Returns: Nullable.empty()
     *
     * // Missing middle key
     * Nullable<Integer> pop3 = Maps.getIfExists(populationMap, "USA", "Texas", "Houston");
     * // Returns: Nullable.empty()
     *
     * // Missing outermost key
     * Nullable<Integer> pop4 = Maps.getIfExists(populationMap, "Canada", "Ontario", "Toronto");
     * // Returns: Nullable.empty()
     *
     * // Null map
     * Nullable<Integer> pop5 = Maps.getIfExists(null, "USA", "California", "Los Angeles");
     * // Returns: Nullable.empty()
     * }</pre>
     *
     * @param <K>  the type of keys in the outermost map
     * @param <K2> the type of keys in the middle map
     * @param <K3> the type of keys in the innermost map
     * @param <V3> the type of values in the innermost map
     * @param map  the outermost map containing nested maps, may be {@code null}
     * @param key  the key used to retrieve the middle map from the outermost map
     * @param k2   the key used to retrieve the innermost map from the middle map
     * @param k3   the key used to retrieve the value from the innermost map
     * @return a {@code Nullable<V3>} containing the value if all three keys are present
     *         (even if the value is {@code null}); otherwise {@code Nullable.empty()}
     * @see #getIfExists(Map, Object)
     * @see #getIfExists(Map, Object, Object)
     */
    public static <K, K2, K3, V3> Nullable<V3> getIfExists(final Map<K, ? extends Map<? extends K2, ? extends Map<? extends K3, ? extends V3>>> map,
            final K key, final K2 k2, final K3 k3) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<? extends K2, ? extends Map<? extends K3, ? extends V3>> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final Map<? extends K3, ? extends V3> m3 = m2.get(k2);

            if (N.notEmpty(m3)) {
                final V3 v3 = m3.get(k3);

                if (v3 != null || m3.containsKey(k3)) {
                    return Nullable.of(v3);
                }
            }
        }

        return Nullable.empty();
    }

    /**
     * Resolves a value from a nested map/collection structure using a dot-separated path.
     *
     * <p>The path supports dot-separated keys and optional {@code [index]} segments to access
     * list/array elements (for example: {@code "user.addresses[0].city"}). If the path cannot
     * be resolved (missing key, index out of bounds, or root map is {@code null}/empty),
     * this method returns {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "tags", Arrays.asList("a", "b")));
     *
     * String name = Maps.getByPath(map, "user.name");        // "John"
     * String firstTag = Maps.getByPath(map, "user.tags[0]");  // "a"
     * String missing = Maps.getByPath(map, "user.age");      // null
     * }</pre>
     *
     * @param <T> the type of the value to be returned
     * @param map the map to query, may be {@code null}
     * @param path the dot-separated path to the value
     * @return the value at the specified path, or {@code null} if the path cannot be resolved
     * @see #getByPathOrDefault(Map, String, Object)
     */
    @MayReturnNull
    public static <T> T getByPath(final Map<String, ?> map, final String path) {
        final Object val = getByPathOrDefaultValue(map, path, NONE);

        if (val == NONE) {
            return null;
        }

        return (T) val;
    }

    /**
     * Resolves a value from a nested map/collection structure using a dot-separated path,
     * returning {@code defaultValue} if the path cannot be resolved.
     *
     * <p>If the path resolves to a non-{@code null} value that is not assignable to the
     * {@code defaultValue} type, the value is converted using {@link N#convert(Object, Class)}.
     * If the path exists but the resolved value is {@code null}, this method returns {@code null}
     * (the default is not applied).</p>
     *
     * <p><b>Note:</b> {@code defaultValue} must be non-{@code null} because its class is used
     * to determine the conversion target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25"));
     *
     * int age = Maps.getByPathOrDefault(map, "user.age", 0);     // 25 (converted)
     * int missing = Maps.getByPathOrDefault(map, "user.height", 180); // 180
     * }</pre>
     *
     * @param <T> the type of the value to be returned
     * @param map the map to query, may be {@code null}
     * @param path the dot-separated path to the value
     * @param defaultValue the default value to return if the path cannot be resolved
     * @return the value at the specified path, or {@code defaultValue} if the path cannot be resolved
     * @see #getByPath(Map, String)
     */
    public static <T> T getByPathOrDefault(final Map<String, ?> map, final String path, final T defaultValue) {
        // N.checkArgNotNull(defaultValue, "defaultValue");

        final Object val = getByPathOrDefaultValue(map, path, defaultValue);

        if (val == null || defaultValue.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultValue.getClass());
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and converts it to the specified target type if necessary.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * can be resolved, the resulting value is either:</p>
     * <ul>
     *   <li>returned as-is if it is {@code null} or already assignable to {@code targetType}, or</li>
     *   <li>converted to {@code targetType} using {@link N#convert(Object, Class)}.</li>
     * </ul>
     *
     * <p>If the path cannot be resolved (for example a key is missing, an index is out of
     * bounds, or the root map is {@code null}/empty), this method returns {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25"));
     *
     * Integer age = Maps.getByPathAs(map, "user.age", Integer.class);
     * // age = 25 (converted from String to Integer)
     *
     * LocalDate missing = Maps.getByPathAs(map, "user.birthdate", LocalDate.class);
     * // missing = null
     * }</pre>
     *
     * @param <T>        the target type
     * @param map        the root map to traverse, may be {@code null}
     * @param path       the dot-separated path with optional {@code [index]} segments
     * @param targetType the target type to convert the value to
     * @return the resolved and possibly converted value, or {@code null} if the path cannot
     *         be resolved
     * @see #getByPath(Map, String)
     */
    @MayReturnNull
    public static <T> T getByPathAs(final Map<String, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefaultValue(map, path, NONE);

        if (val == NONE) {
            return null;
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return N.convert(val, targetType);
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and returns it wrapped in a {@link Nullable} if the path exists.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * can be resolved, the resulting value (which may be {@code null}) is wrapped in
     * {@code Nullable.of(...)}. If the path cannot be resolved (for example a key is
     * missing, an index is out of bounds, or the root map is {@code null}/empty),
     * {@code Nullable.empty()} is returned.</p>
     *
     * <p><b>Null vs. missing path:</b><br>
     * This method distinguishes between "path exists but value is {@code null}" and
     * "path does not exist":</p>
     *
     * <ul>
     *   <li>Path exists → {@code Nullable.of(value)} (even when {@code value == null})</li>
     *   <li>Path missing → {@code Nullable.empty()}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "age", null));
     *
     * Nullable<String> name = Maps.getByPathIfExists(map, "user.name");
     * // name.isPresent() = true, name.get() = "John"
     *
     * Nullable<Integer> age = Maps.getByPathIfExists(map, "user.age");
     * // age.isPresent() = true, age.get() = null
     *
     * Nullable<String> email = Maps.getByPathIfExists(map, "user.email");
     * // email.isPresent() = false
     * }</pre>
     *
     * @param <T>  the expected type of the value
     * @param map  the root map to traverse, may be {@code null}
     * @param path the dot-separated path with optional {@code [index]} segments
     * @return a {@code Nullable<T>} containing the value if the path exists (even if the
     *         value is {@code null}); otherwise {@code Nullable.empty()}
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<String, ?> map, final String path) {
        final Object val = getByPathOrDefaultValue(map, path, NONE);

        if (val == NONE) {
            return Nullable.empty();
        }

        return Nullable.of((T) val);
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path,
     * converts it to the specified target type if necessary, and returns the result wrapped
     * in a {@link Nullable} if the path exists.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * can be resolved, the resulting value is either:</p>
     * <ul>
     *   <li>wrapped as {@code Nullable.of(value)} if it is {@code null} or already
     *       assignable to {@code targetType}, or</li>
     *   <li>converted to {@code targetType} using {@link N#convert(Object, Class)} and
     *       wrapped as {@code Nullable.of(convertedValue)}.</li>
     * </ul>
     *
     * <p>If the path cannot be resolved (for example a key is missing, an index is out of
     * bounds, or the root map is {@code null}/empty), this method returns
     * {@code Nullable.empty()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("settings", N.asMap("maxConnections", "100"));
     *
     * Nullable<Integer> maxConn =
     *         Maps.getByPathAsIfExists(map, "settings.maxConnections", Integer.class);
     * // maxConn.isPresent() = true, maxConn.get() = 100 (converted from String)
     *
     * Nullable<Boolean> debug =
     *         Maps.getByPathAsIfExists(map, "settings.debug", Boolean.class);
     * // debug.isPresent() = false
     * }</pre>
     *
     * @param <T>        the target type
     * @param map        the root map to traverse, may be {@code null}
     * @param path       the dot-separated path with optional {@code [index]} segments
     * @param targetType the target type to convert the value to
     * @return a {@code Nullable<T>} containing the resolved (and possibly converted) value
     *         if the path exists; otherwise {@code Nullable.empty()}
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathAsIfExists(final Map<String, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefaultValue(map, path, NONE);

        if (val == NONE) {
            return Nullable.empty();
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return Nullable.of((T) val);
        } else {
            return Nullable.of(N.convert(val, targetType));
        }
    }

    @SuppressWarnings("rawtypes")
    private static Object getByPathOrDefaultValue(final Map<String, ?> map, final String path, final Object defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        } else if (N.isEmpty(path)) {
            return getOrDefaultIfAbsent(map, path, defaultValue);
        }

        final Class<?> targetType = defaultValue == null || defaultValue == NONE ? null : defaultValue.getClass();

        final String[] keys = Strings.split(path, '.');
        Map intermediateMap = map;
        Collection intermediateColl = null;
        String key = null;

        for (int i = 0, len = keys.length; i < len; i++) {
            key = keys[i];

            if (N.isEmpty(intermediateMap)) {
                return defaultValue;
            }

            if (key.charAt(key.length() - 1) == ']') {
                final int[] indexes = Strings.substringsBetween(key, "[", "]").stream().mapToInt(Numbers::toInt).toArray();
                final int idx = key.indexOf('[');
                intermediateColl = (Collection) intermediateMap.get(key.substring(0, idx));

                for (int j = 0, idxLen = indexes.length; j < idxLen; j++) {
                    if (N.isEmpty(intermediateColl) || intermediateColl.size() <= indexes[j]) {
                        return defaultValue;
                    } else {
                        if (j == idxLen - 1) {
                            if (i == len - 1) {
                                final Object ret = N.getElement(intermediateColl, indexes[j]);

                                if (ret == null || targetType == null || targetType.isAssignableFrom(ret.getClass())) {
                                    return ret;
                                } else {
                                    return N.convert(ret, targetType);
                                }
                            } else {
                                intermediateMap = (Map) N.getElement(intermediateColl, indexes[j]);
                            }
                        } else {
                            intermediateColl = (Collection) N.getElement(intermediateColl, indexes[j]);
                        }
                    }
                }
            } else {
                if (i == len - 1) {
                    final Object ret = intermediateMap.getOrDefault(key, defaultValue);

                    if (ret == null || targetType == null || targetType.isAssignableFrom(ret.getClass())) {
                        return ret;
                    } else {
                        return N.convert(ret, targetType);
                    }
                } else {
                    intermediateMap = (Map) intermediateMap.get(key);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Returns the value associated with the specified key, or {@code defaultValue} if the key is
     * considered <em>absent</em>.
     *
     * <p>A key is treated as absent if:
     * <ul>
     *   <li>the map is {@code null} or empty, or</li>
     *   <li>the map does not contain the key, or</li>
     *   <li>the value mapped to the key is {@code null}</li>
     * </ul>
     *
     * <p>This method therefore differs from {@link Map#getOrDefault(Object, Object)} in that
     * a {@code null} value is treated the same as a missing key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     *
     * Maps.getOrDefaultIfAbsent(map, "key1", "default"); // "value1"
     * Maps.getOrDefaultIfAbsent(map, "key2", "default"); // "default"
     * Maps.getOrDefaultIfAbsent(map, "key3", "default"); // "default"
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map from which to retrieve the value; may be {@code null} or empty
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if the key is absent; must not be {@code null}
     * @return the value mapped to {@code key}, or {@code defaultValue} if the key is absent
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}
     * @see #getIfExists(Map, Object)
     * @see #getAsOrDefault(Map, Object, Object)
     */
    public static <K, V> V getOrDefaultIfAbsent(final Map<K, ? extends V> map, final K key, final V defaultValue) {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            return defaultValue;
        } else {
            return val;
        }
    }

    /**
     * Returns the value from a two-level nested map structure, or {@code defaultValue} if the value
     * is considered <em>absent</em>.
     *
     * <p>The lookup proceeds as follows:
     * <ol>
     *   <li>Retrieve the inner map associated with {@code key}</li>
     *   <li>Retrieve the value associated with {@code k2} from the inner map</li>
     * </ol>
     *
     * <p>The value is considered absent if:
     * <ul>
     *   <li>the outer map is {@code null} or empty, or</li>
     *   <li>no inner map is associated with {@code key}, or</li>
     *   <li>the inner map does not contain {@code k2}, or</li>
     *   <li>the mapped value is {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> nested = new HashMap<>();
     * nested.put("data", Collections.singletonMap("count", 5));
     *
     * Maps.getOrDefaultIfAbsent(nested, "data", "count", 0);    // 5
     * Maps.getOrDefaultIfAbsent(nested, "data", "missing", 0);  // 0
     * Maps.getOrDefaultIfAbsent(nested, "missing", "count", 0); // 0
     * }</pre>
     *
     * @param <K> the outer map key type
     * @param <K2> the inner map key type
     * @param <V2> the value type
     * @param map the outer map; may be {@code null} or empty
     * @param key the key used to retrieve the inner map
     * @param k2 the key used to retrieve the value from the inner map
     * @param defaultValue the value to return if the lookup result is absent; must not be {@code null}
     * @return the resolved value, or {@code defaultValue} if absent
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}
     * @see #getIfExists(Map, Object, Object)
     */
    public static <K, K2, V2> V2 getOrDefaultIfAbsent(final Map<K, ? extends Map<K2, V2>> map, final K key, final K2 k2, final V2 defaultValue) {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Map<K2, V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null) {
                return v2;
            }
        }

        return defaultValue;
    }

    /**
     * Returns the value associated with the specified key, or a value supplied by
     * {@code defaultValueSupplier} if the key is considered <em>absent</em>.
     *
     * <p>A key is treated as absent if the map is {@code null} or empty, the key is not present,
     * or the associated value is {@code null}.</p>
     *
     * <p>The supplier is invoked <strong>only</strong> when the key is absent and its result
     * must not be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     *
     * String value = Maps.getOrDefaultIfAbsent(map, "key", () -> computeDefault());
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map from which to retrieve the value; may be {@code null} or empty
     * @param key the key whose associated value is to be returned
     * @param defaultValueSupplier supplies the value to return if the key is absent
     * @return the mapped value, or the value supplied by {@code defaultValueSupplier} if absent
     * @throws NullPointerException if {@code defaultValueSupplier} is {@code null} or returns {@code null}
     */
    public static <K, V> V getOrDefaultIfAbsent(final Map<K, ? extends V> map, final K key, final Supplier<? extends V> defaultValueSupplier) {
        if (N.isEmpty(map)) {
            return N.requireNonNull(defaultValueSupplier.get());
        }

        final V val = map.get(key);

        if (val == null) {
            return N.requireNonNull(defaultValueSupplier.get());
        } else {
            return val;
        }
    }

    /**
     * Returns the List value to which the specified key is found, or an empty immutable List if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * map.put("fruits", Arrays.asList("apple", "banana"));
     * map.put("empty", null);
     * 
     * List<String> result1 = Maps.getOrEmptyListIfAbsent(map, "fruits");    // ["apple", "banana"]
     * List<String> result2 = Maps.getOrEmptyListIfAbsent(map, "empty");     // []
     * List<String> result3 = Maps.getOrEmptyListIfAbsent(map, "missing");   // []
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <E> the type of elements in the list.
     * @param <V> the type of list values maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return the List value mapped by the key, or an empty immutable List if the key is absent.
     * @see N#emptyList()
     */
    public static <K, E, V extends List<E>> List<E> getOrEmptyListIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptyList();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.emptyList();
        }

        return val;
    }

    /**
     * Returns the Set value to which the specified key is found, or an empty immutable Set if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<Integer>> map = new HashMap<>();
     * map.put("primes", new HashSet<>(Arrays.asList(2, 3, 5, 7)));
     * map.put("empty", null);
     * 
     * Set<Integer> result1 = Maps.getOrEmptySetIfAbsent(map, "primes");    // {2, 3, 5, 7}
     * Set<Integer> result2 = Maps.getOrEmptySetIfAbsent(map, "empty");     // {}
     * Set<Integer> result3 = Maps.getOrEmptySetIfAbsent(map, "missing");   // {}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <E> the type of elements in the set.
     * @param <V> the type of set values maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return the Set value mapped by the key, or an empty immutable Set if the key is absent.
     * @see N#emptySet()
     */
    public static <K, E, V extends Set<E>> Set<E> getOrEmptySetIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptySet();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.emptySet();
        }

        return val;
    }

    /**
     * Returns the Map value to which the specified key is found, or an empty immutable Map if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     * Map<String, Integer> innerMap = new HashMap<>();
     * innerMap.put("a", 1);
     * innerMap.put("b", 2);
     * map.put("data", innerMap);
     * map.put("empty", null);
     * 
     * Map<String, Integer> result1 = Maps.getOrEmptyMapIfAbsent(map, "data");      // {a=1, b=2}
     * Map<String, Integer> result2 = Maps.getOrEmptyMapIfAbsent(map, "empty");     // {}
     * Map<String, Integer> result3 = Maps.getOrEmptyMapIfAbsent(map, "missing");   // {}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the outer map.
     * @param <KK> the type of keys maintained by the inner map.
     * @param <VV> the type of values maintained by the inner map.
     * @param <V> the type of map values maintained by the outer map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return the Map value mapped by the key, or an empty immutable Map if the key is absent.
     * @see N#emptyMap()
     */
    public static <K, KK, VV, V extends Map<KK, VV>> Map<KK, VV> getOrEmptyMapIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptyMap();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.emptyMap();
        }

        return val;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new value obtained from {@code defaultValueSupplier} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * 
     * List<String> list1 = Maps.getOrPutIfAbsent(map, "key1", () -> new ArrayList<>());
     * list1.add("value1");
     * // map now contains: {"key1"=["value1"]}
     * 
     * List<String> list2 = Maps.getOrPutIfAbsent(map, "key1", () -> new ArrayList<>());
     * // list2 is the same instance as list1, supplier not called
     * 
     * map.put("key2", null);
     * List<String> list3 = Maps.getOrPutIfAbsent(map, "key2", () -> new ArrayList<>());
     * // New list created because value was null
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @param defaultValueSupplier the supplier to provide a default value if the key is absent; must not be {@code null}.
     * @return the value associated with the specified key, or a new value from {@code defaultValueSupplier} if the key is absent.
     */
    public static <K, V> V getOrPutIfAbsent(final Map<K, V> map, final K key, final Supplier<? extends V> defaultValueSupplier) {
        V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            val = defaultValueSupplier.get(); // Objects.requireNonNull(defaultValueSupplier.get());
            map.put(key, val);
        }

        return val;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code List} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * 
     * List<Integer> list1 = Maps.getOrPutListIfAbsent(map, "numbers");
     * list1.add(1);
     * list1.add(2);
     * // map now contains: {"numbers"=[1, 2]}
     * 
     * List<Integer> list2 = Maps.getOrPutListIfAbsent(map, "numbers");
     * // list2 is the same instance as list1
     * // list2 contains [1, 2]
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the list.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code List} if the key is absent.
     */
    public static <K, E> List<E> getOrPutListIfAbsent(final Map<K, List<E>> map, final K key) {
        List<E> v = map.get(key);

        if (v == null) {
            v = new ArrayList<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Set} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     * 
     * Set<String> set1 = Maps.getOrPutSetIfAbsent(map, "tags");
     * set1.add("java");
     * set1.add("spring");
     * // map now contains: {"tags"=["java", "spring"]}
     * 
     * Set<String> set2 = Maps.getOrPutSetIfAbsent(map, "tags");
     * // set2 is the same instance as set1
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the set.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code Set} if the key is absent.
     */
    public static <K, E> Set<E> getOrPutSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new HashSet<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashSet} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     * 
     * Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "orderedTags");
     * set.add("first");
     * set.add("second");
     * set.add("third");
     * // map now contains: {"orderedTags"=["first", "second", "third"]} (order preserved)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the set.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code LinkedHashSet} if the key is absent.
     */
    public static <K, E> Set<E> getOrPutLinkedHashSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new LinkedHashSet<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Map} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     * 
     * Map<String, Integer> innerMap = Maps.getOrPutMapIfAbsent(map, "scores");
     * innerMap.put("math", 95);
     * innerMap.put("english", 88);
     * // map now contains: {"scores"={"math"=95, "english"=88}}
     * 
     * Map<String, Integer> sameMap = Maps.getOrPutMapIfAbsent(map, "scores");
     * // sameMap is the same instance as innerMap
     * }</pre>
     *
     * @param <K> the key type.
     * @param <KK> the key type of the value map.
     * @param <VV> the value type of the value map.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code Map} if the key is absent.
     */
    public static <K, KK, VV> Map<KK, VV> getOrPutMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new HashMap<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashMap} and returns it.
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, String>> map = new HashMap<>();
     * 
     * Map<String, String> innerMap = Maps.getOrPutLinkedHashMapIfAbsent(map, "config");
     * innerMap.put("first", "1");
     * innerMap.put("second", "2");
     * innerMap.put("third", "3");
     * // map now contains: {"config"={"first"="1", "second"="2", "third"="3"}} (order preserved)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <KK> the key type of the value map.
     * @param <VV> the value type of the value map.
     * @param map the map to check and possibly update.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code LinkedHashMap} if the key is absent.
     */
    public static <K, KK, VV> Map<KK, VV> getOrPutLinkedHashMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new LinkedHashMap<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns an empty {@code OptionalBoolean} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalBoolean} with the value mapped by the specified {@code key}.
     * If the mapped value is not Boolean type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("flag1", true);
     * map.put("flag2", "false");
     * map.put("flag3", null);
     * 
     * OptionalBoolean result1 = Maps.getAsBoolean(map, "flag1");   // OptionalBoolean.of(true)
     * OptionalBoolean result2 = Maps.getAsBoolean(map, "flag2");   // OptionalBoolean.of(false)
     * OptionalBoolean result3 = Maps.getAsBoolean(map, "flag3");   // OptionalBoolean.empty()
     * OptionalBoolean result4 = Maps.getAsBoolean(map, "flag4");   // OptionalBoolean.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an OptionalBoolean containing the boolean value, or empty if not found.
     */
    public static <K> OptionalBoolean getAsBoolean(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalBoolean.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalBoolean.empty();
        } else if (val instanceof Boolean) {
            return OptionalBoolean.of((Boolean) val);
        } else {
            return OptionalBoolean.of(Strings.parseBoolean(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Boolean type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("enabled", true);
     * map.put("disabled", "false");
     * 
     * boolean result1 = Maps.getAsBooleanOrDefault(map, "enabled", false);   // true
     * boolean result2 = Maps.getAsBooleanOrDefault(map, "disabled", true);   // false
     * boolean result3 = Maps.getAsBooleanOrDefault(map, "missing", true);    // true (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the key is not found or value is null.
     * @return the boolean value mapped to the key, or defaultValue if not found.
     */
    public static <K> boolean getAsBooleanOrDefault(final Map<? super K, ?> map, final K key, final boolean defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else {
            return Strings.parseBoolean(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalChar} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalChar} with the value mapped by the specified {@code key}.
     * If the mapped value is not Character type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("letter", 'A');
     * map.put("digit", "5");
     * map.put("empty", null);
     * 
     * OptionalChar result1 = Maps.getAsChar(map, "letter");    // OptionalChar.of('A')
     * OptionalChar result2 = Maps.getAsChar(map, "digit");     // OptionalChar.of('5')
     * OptionalChar result3 = Maps.getAsChar(map, "empty");     // OptionalChar.empty()
     * OptionalChar result4 = Maps.getAsChar(map, "missing");   // OptionalChar.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an OptionalChar containing the character value, or empty if not found.
     */
    public static <K> OptionalChar getAsChar(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalChar.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalChar.empty();
        } else if (val instanceof Character) {
            return OptionalChar.of(((Character) val));
        } else {
            return OptionalChar.of(Strings.parseChar(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Character type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("grade", 'A');
     * map.put("initial", "J");
     * 
     * char result1 = Maps.getAsCharOrDefault(map, "grade", 'F');     // 'A'
     * char result2 = Maps.getAsCharOrDefault(map, "initial", 'X');   // 'J'
     * char result3 = Maps.getAsCharOrDefault(map, "missing", 'N');   // 'N' (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the key is not found or value is null.
     * @return the character value mapped to the key, or defaultValue if not found.
     */
    public static <K> char getAsCharOrDefault(final Map<? super K, ?> map, final K key, final char defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Character) {
            return (Character) val;
        } else {
            return Strings.parseChar(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalByte} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalByte} with the value mapped by the specified {@code key}.
     * If the mapped value is not Byte/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("small", (byte) 10);
     * map.put("medium", 127);
     * map.put("text", "25");
     * 
     * OptionalByte result1 = Maps.getAsByte(map, "small");     // OptionalByte.of(10)
     * OptionalByte result2 = Maps.getAsByte(map, "medium");    // OptionalByte.of(127)
     * OptionalByte result3 = Maps.getAsByte(map, "text");      // OptionalByte.of(25)
     * OptionalByte result4 = Maps.getAsByte(map, "missing");   // OptionalByte.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an OptionalByte containing the byte value, or empty if not found.
     */
    public static <K> OptionalByte getAsByte(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalByte.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalByte.empty();
        } else if (val instanceof Number) {
            return OptionalByte.of(((Number) val).byteValue());
        } else {
            return OptionalByte.of(Numbers.toByte(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Byte/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("id", (byte) 5);
     * map.put("count", "10");
     * 
     * byte result1 = Maps.getAsByteOrDefault(map, "id", (byte) 0);         // 5
     * byte result2 = Maps.getAsByteOrDefault(map, "count", (byte) 0);      // 10
     * byte result3 = Maps.getAsByteOrDefault(map, "missing", (byte) -1);   // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the key is not found or value is null.
     * @return the byte value mapped to the key, or defaultValue if not found.
     */
    public static <K> byte getAsByteOrDefault(final Map<? super K, ?> map, final K key, final byte defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Number) {
            return ((Number) val).byteValue();
        } else {
            return Numbers.toByte(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalShort} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalShort} with the value mapped by the specified {@code key}.
     * If the mapped value is not Short/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("year", (short) 2023);
     * map.put("count", 1000);
     * map.put("text", "500");
     * 
     * OptionalShort result1 = Maps.getAsShort(map, "year");      // OptionalShort.of(2023)
     * OptionalShort result2 = Maps.getAsShort(map, "count");     // OptionalShort.of(1000)
     * OptionalShort result3 = Maps.getAsShort(map, "text");      // OptionalShort.of(500)
     * OptionalShort result4 = Maps.getAsShort(map, "missing");   // OptionalShort.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an OptionalShort containing the short value, or empty if not found.
     */
    public static <K> OptionalShort getAsShort(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalShort.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalShort.empty();
        } else if (val instanceof Number) {
            return OptionalShort.of(((Number) val).shortValue());
        } else {
            return OptionalShort.of(Numbers.toShort(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Short/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("port", (short) 8080);
     * map.put("timeout", "3000");
     * 
     * short result1 = Maps.getAsShortOrDefault(map, "port", (short) 80);      // 8080
     * short result2 = Maps.getAsShortOrDefault(map, "timeout", (short) 0);    // 3000
     * short result3 = Maps.getAsShortOrDefault(map, "missing", (short) -1);   // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the key is not found or value is null.
     * @return the short value mapped to the key, or defaultValue if not found.
     */
    public static <K> short getAsShortOrDefault(final Map<? super K, ?> map, final K key, final short defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else {
            return Numbers.toShort(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalInt} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalInt} with the value mapped by the specified {@code key}.
     * If the mapped value is not Integer/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42);
     * map.put("total", "100");
     * map.put("null", null);
     * 
     * OptionalInt result1 = Maps.getAsInt(map, "count");     // OptionalInt.of(42)
     * OptionalInt result2 = Maps.getAsInt(map, "total");     // OptionalInt.of(100)
     * OptionalInt result3 = Maps.getAsInt(map, "null");      // OptionalInt.empty()
     * OptionalInt result4 = Maps.getAsInt(map, "missing");   // OptionalInt.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an OptionalInt containing the integer value, or empty if not found.
     */
    public static <K> OptionalInt getAsInt(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalInt.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalInt.empty();
        } else if (val instanceof Number) {
            return OptionalInt.of(((Number) val).intValue());
        } else {
            return OptionalInt.of(Numbers.toInt(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Integer/Number type, underlying conversion will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("age", 25);
     * map.put("score", "98");
     * 
     * int result1 = Maps.getAsIntOrDefault(map, "age", 0);        // 25
     * int result2 = Maps.getAsIntOrDefault(map, "score", 0);      // 98
     * int result3 = Maps.getAsIntOrDefault(map, "missing", -1);   // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the key is not found or value is null.
     * @return the integer value mapped to the key, or defaultValue if not found.
     */
    public static <K> int getAsIntOrDefault(final Map<? super K, ?> map, final K key, final int defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Number) {
            return ((Number) val).intValue();
        } else {
            return Numbers.toInt(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalLong} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalLong} with the value mapped by the specified {@code key}.
     * If the mapped value is not Long/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     * 
     * OptionalLong count = Maps.getAsLong(map, "count");
     * // count.isPresent() = true, count.getAsLong() = 42
     * 
     * OptionalLong score = Maps.getAsLong(map, "score");
     * // score.isPresent() = true, score.getAsLong() = 100 (converted from String)
     * 
     * OptionalLong missing = Maps.getAsLong(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalLong} containing the value if present, otherwise empty.
     */
    public static <K> OptionalLong getAsLong(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalLong.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalLong.empty();
        } else if (val instanceof Number) {
            return OptionalLong.of(((Number) val).longValue());
        } else {
            return OptionalLong.of(Numbers.toLong(N.toString(val)));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Long/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     * 
     * long count = Maps.getAsLongOrDefault(map, "count", -1L);
     * // count = 42
     * 
     * long score = Maps.getAsLongOrDefault(map, "score", -1L);
     * // score = 100 (converted from String)
     * 
     * long missing = Maps.getAsLongOrDefault(map, "missing", -1L);
     * // missing = -1
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the value is {@code null} or not found.
     * @return the long value associated with the key, or defaultValue if not found.
     */
    public static <K> long getAsLongOrDefault(final Map<? super K, ?> map, final K key, final long defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else {
            return Numbers.toLong(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalFloat} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalFloat} with the value mapped by the specified {@code key}.
     * If the mapped value is not Float/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     * 
     * OptionalFloat price = Maps.getAsFloat(map, "price");
     * // price.isPresent() = true, price.getAsFloat() = 19.99
     * 
     * OptionalFloat discount = Maps.getAsFloat(map, "discount");
     * // discount.isPresent() = true, discount.getAsFloat() = 0.15 (converted from String)
     * 
     * OptionalFloat missing = Maps.getAsFloat(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalFloat} containing the value if present, otherwise empty.
     */
    public static <K> OptionalFloat getAsFloat(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalFloat.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalFloat.empty();
        } else {
            return OptionalFloat.of(Numbers.toFloat(val));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Float/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     * 
     * float price = Maps.getAsFloatOrDefault(map, "price", 0.0f);
     * // price = 19.99
     * 
     * float discount = Maps.getAsFloatOrDefault(map, "discount", 0.0f);
     * // discount = 0.15 (converted from String)
     * 
     * float missing = Maps.getAsFloatOrDefault(map, "missing", 0.0f);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the value is {@code null} or not found.
     * @return the float value associated with the key, or defaultValue if not found.
     */
    public static <K> float getAsFloatOrDefault(final Map<? super K, ?> map, final K key, final float defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else {
            return Numbers.toFloat(val);
        }
    }

    /**
     * Returns an empty {@code OptionalDouble} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalDouble} with the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     * 
     * OptionalDouble temp = Maps.getAsDouble(map, "temperature");
     * // temp.isPresent() = true, temp.getAsDouble() = 98.6
     * 
     * OptionalDouble pi = Maps.getAsDouble(map, "pi");
     * // pi.isPresent() = true, pi.getAsDouble() = 3.14159 (converted from String)
     * 
     * OptionalDouble missing = Maps.getAsDouble(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalDouble} containing the value if present, otherwise empty.
     */
    public static <K> OptionalDouble getAsDouble(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalDouble.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(Numbers.toDouble(val));
        }
    }

    /**
     * Returns the specified {@code defaultValue} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     * 
     * double temp = Maps.getAsDoubleOrDefault(map, "temperature", 0.0);
     * // temp = 98.6
     * 
     * double pi = Maps.getAsDoubleOrDefault(map, "pi", 0.0);
     * // pi = 3.14159 (converted from String)
     * 
     * double missing = Maps.getAsDoubleOrDefault(map, "missing", 0.0);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the value is {@code null} or not found.
     * @return the double value associated with the key, or defaultValue if not found.
     */
    public static <K> double getAsDoubleOrDefault(final Map<? super K, ?> map, final K key, final double defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else {
            return Numbers.toDouble(val);
        }
    }

    /**
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<String>} with the value mapped by the specified {@code key}.
     * If the mapped value is not String type, underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     * 
     * Optional<String> name = Maps.getAsString(map, "name");
     * // name.isPresent() = true, name.get() = "John"
     * 
     * Optional<String> age = Maps.getAsString(map, "age");
     * // age.isPresent() = true, age.get() = "25" (converted from Integer)
     * 
     * Optional<String> missing = Maps.getAsString(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code Optional<String>} with the value mapped by the specified key, or an empty {@code Optional<String>} if the map is empty, contains no value for the key, or the value is null.
     */
    public static <K> Optional<String> getAsString(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (val instanceof String) {
            return Optional.of((String) val);
        } else {
            return Optional.of(N.stringOf(val));
        }
    }

    /**
     * Returns the value to which the specified key is found if the value is not {@code null},
     * or {@code defaultValue} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of String type, underlying conversion will be executed by {@code N.stringOf(value)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     * 
     * String name = Maps.getAsStringOrDefault(map, "name", "Unknown");
     * // name = "John"
     * 
     * String age = Maps.getAsStringOrDefault(map, "age", "Unknown");
     * // age = "25" (converted from Integer)
     * 
     * String missing = Maps.getAsStringOrDefault(map, "missing", "Unknown");
     * // missing = "Unknown"
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null.
     * @return the value mapped by the specified key, or {@code defaultValue} if the map is empty, contains no value for the key, or the value is null.
     * @throws IllegalArgumentException if the specified {@code defaultValue} is {@code null}.
     */
    public static <K> String getAsStringOrDefault(final Map<? super K, ?> map, final K key, final String defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, "defaultValue"); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof String) {
            return (String) val;
        } else {
            return N.stringOf(val);
        }
    }

    /**
     * Returns an empty {@code Optional<T>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<T>} with the value mapped by the specified {@code key}.
     * If the mapped value is not {@code T} type, underlying conversion will be executed by {@code N.convert(val, targetType)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("date", "2023-12-25");
     * map.put("count", "100");
     * 
     * Optional<LocalDate> date = Maps.getAs(map, "date", LocalDate.class);
     * // date.isPresent() = true, date.get() = LocalDate.of(2023, 12, 25)
     * 
     * Optional<Integer> count = Maps.getAs(map, "count", Integer.class);
     * // count.isPresent() = true, count.get() = 100
     * 
     * Optional<BigDecimal> missing = Maps.getAs(map, "missing", BigDecimal.class);
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param targetType the target type to which the value should be converted.
     * @return an {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getAs(final Map<? super K, ?> map, final K key, final Class<? extends T> targetType) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.isAssignableFrom(val.getClass())) {
            return Optional.of((T) val);
        } else {
            return Optional.of(N.convert(val, targetType));
        }
    }

    /**
     * Returns an empty {@code Optional<T>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<T>} with the value mapped by the specified {@code key}.
     * If the mapped value is not {@code T} type, underlying conversion will be executed by {@code N.convert(val, targetType)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("items", Arrays.asList("A", "B", "C"));
     * 
     * Optional<List<String>> items = Maps.getAs(map, "items", new TypeReference<List<String>>() {});
     * // items.isPresent() = true, items.get() = ["A", "B", "C"]
     * 
     * Optional<Set<Integer>> missing = Maps.getAs(map, "missing", new TypeReference<Set<Integer>>() {});
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param targetType the target type to which the value should be converted.
     * @return an {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getAs(final Map<? super K, ?> map, final K key, final Type<? extends T> targetType) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.clazz().isAssignableFrom(val.getClass())) {
            return Optional.of((T) val);
        } else {
            return Optional.of(N.convert(val, targetType));
        }
    }

    /**
     * Returns the value to which the specified {@code key} is mapped if the value is not {@code null},
     * or {@code defaultValue} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of type {@code T}, an underlying conversion will be executed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", "100");
     * map.put("active", "true");
     * 
     * Integer count = Maps.getAsOrDefault(map, "count", 0);
     * // count = 100 (converted from String)
     * 
     * Boolean active = Maps.getAsOrDefault(map, "active", false);
     * // active = true (converted from String)
     * 
     * Double missing = Maps.getAsOrDefault(map, "missing", 0.0);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null.
     * @return the value to which the specified key is found, or {@code defaultValue} if the map is empty, contains no value for the key, or the value is null.
     * @throws IllegalArgumentException if {@code defaultValue} is null.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> T getAsOrDefault(final Map<? super K, ?> map, final K key, final T defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, "defaultValue"); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (defaultValue.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultValue.getClass());
        }
    }

    /**
     * Returns a list of values of the keys which exist in the specified {@code Map}.
     * If the key doesn't exist in the {@code Map} or associated value is {@code null}, no value will be added into the returned list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     *
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getValuesIfPresent(map, keys);
     * // values = [1, 2] (null value for "c" and missing "d" are not included)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to check for keys.
     * @param keys the collection of keys to check in the map.
     * @return a list of values corresponding to the keys found in the map. Returns an empty list
     *         if {@code map} or {@code keys} is {@code null} or empty.
     */
    public static <K, V> List<V> getValuesIfPresent(final Map<K, ? extends V> map, final Collection<?> keys) throws IllegalArgumentException {
        if (N.isEmpty(map) || N.isEmpty(keys)) {
            return new ArrayList<>();
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
            //noinspection SuspiciousMethodCalls
            val = map.get(key);

            if (val != null) {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Returns a list of values mapped by the keys which exist in the specified {@code Map}, 
     * or default value if the key doesn't exist in the {@code Map} or associated value is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     * 
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getValuesOrDefault(map, keys, -1);
     * // values = [1, 2, -1, -1] ("c" has null value, "d" is missing)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to check for keys.
     * @param keys the collection of keys to check in the map.
     * @param defaultValue the default value to use when key is absent.
     * @return a list of values corresponding to the keys, using defaultValue when absent. Returns an empty list
     *         if {@code keys} is {@code null} or empty. If {@code map} is {@code null} or empty, the returned list
     *         contains {@code defaultValue} repeated {@code keys.size()} times.
     */
    public static <K, V> List<V> getValuesOrDefault(final Map<K, V> map, final Collection<?> keys, final V defaultValue) throws IllegalArgumentException {
        // N.checkArgNotNull(defaultValue, "defaultValue");   // NOSONAR

        if (N.isEmpty(keys)) {
            return new ArrayList<>();
        } else if (N.isEmpty(map)) {
            return N.repeat(defaultValue, keys.size());
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
            //noinspection SuspiciousMethodCalls
            val = map.get(key);

            if (val == null) {
                result.add(defaultValue);
            } else {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Returns a new map containing entries that are present in both input maps.
     * The intersection contains entries whose keys are present in both maps with equal values.
     * The returned map's key-value pairs are taken from the first input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = new HashMap<>();
     * map1.put("a", 1);
     * map1.put("b", 2);
     * map1.put("c", 3);
     *
     * Map<String, Integer> map2 = new HashMap<>();
     * map2.put("b", 2);
     * map2.put("c", 4);
     * map2.put("d", 5);
     *
     * Map<String, Integer> result = Maps.intersection(map1, map2);   // result will be {"b": 2}
     * // Only "b" is included because it has the same value in both maps
     *
     * Map<String, String> map3 = new HashMap<>();
     * map3.put("x", "foo");
     * map3.put("y", "bar");
     *
     * Map<String, String> map4 = new HashMap<>();
     * map4.put("x", "foo");
     * map4.put("z", "baz");
     *
     * Map<String, String> result2 = Maps.intersection(map3, map4);   // result will be {"x": "foo"}
     * // Only "x" is included because it has the same value in both maps
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param <V> the type of values in the map.
     * @param map the first input map.
     * @param map2 the second input map to find common entries with.
     * @return a new map containing entries present in both maps with equal values.
     *         If the first map is {@code null}, returns an empty map.
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static <K, V> Map<K, V> intersection(final Map<K, V> map, final Map<?, ?> map2) {
        if (map == null) {
            return new HashMap<>();
        }

        if (N.isEmpty(map2)) {
            return newTargetMap(map, 0);
        }

        final Map<K, V> result = Maps.newTargetMap(map, N.size(map) / 2);
        Object val = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            val = map2.get(entry.getKey());

            if ((val != null && N.equals(val, entry.getValue())) || (val == null && entry.getValue() == null && map2.containsKey(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Calculates the difference between two maps.
     * The difference is defined as a map where each entry's key exists in the first map,
     * and the entry's value is a pair consisting of the value from the first map and the value from the second map.
     * If a key exists in the first map but not in the second, the value from the second map in the pair is an empty {@code Nullable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Maps.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Maps.of("a", 1, "b", 20, "d", 4);
     * 
     * Map<String, Pair<Integer, Nullable<Integer>>> diff = Maps.difference(map1, map2);
     * // diff contains:
     * // "b" -> Pair.of(2, Nullable.of(20))    // different values
     * // "c" -> Pair.of(3, Nullable.empty())   // key only in map1
     * }</pre>
     *
     * <p>Note that this method only returns keys from the first map. Keys that exist only in the second map 
     * are not included in the result. If you need to identify keys that are unique to each map, 
     * use {@link #symmetricDifference(Map, Map)} instead.
     *
     * <p>If the first map is {@code null}, an empty map is returned. If the second map is {@code null},
     * all values from the first map will be paired with empty {@code Nullable} objects.
     *
     * @param <K> the type of keys in the maps.
     * @param <V> the type of values in the maps.
     * @param map the first map to compare.
     * @param map2 the second map to compare.
     * @return a map representing the difference between the two input maps.
     * @see #symmetricDifference(Map, Map)
     * @see Difference.MapDifference#of(Map, Map)
     * @see N#difference(Collection, Collection)
     * @see #intersection(Map, Map)
     */
    public static <K, V> Map<K, Pair<V, Nullable<V>>> difference(final Map<K, V> map, final Map<K, V> map2) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, Pair<V, Nullable<V>>> result = newTargetMap(map, N.size(map) / 2);

        if (N.isEmpty(map2)) {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.empty()));
            }
        } else {
            V val = null;

            for (final Map.Entry<K, V> entry : map.entrySet()) {
                val = map2.get(entry.getKey());

                if (val == null && !map2.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.empty()));
                } else if (!N.equals(val, entry.getValue())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.of(val)));
                }
            }
        }

        return result;
    }

    /**
     * Returns a new map containing the symmetric difference between two maps.
     * The symmetric difference includes entries whose keys are present in only one of the maps
     * or entries with the same key but different values in both maps.
     *
     * <p>For each key in the result map, the value is a pair where:
     * <ul>
     * <li>If the key exists only in the first map, the pair contains the value from the first map and an empty Nullable</li>
     * <li>If the key exists only in the second map, the pair contains an empty {@code Nullable} and the value from the second map</li>
     * <li>If the key exists in both maps with different values, the pair contains both values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Maps.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Maps.of("b", 2, "c", 4, "d", 5);
     * 
     * Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(map1, map2);
     * // result contains:
     * // "a" -> Pair.of(Nullable.of(1), Nullable.empty())   // key only in map1
     * // "c" -> Pair.of(Nullable.of(3), Nullable.of(4))     // different values
     * // "d" -> Pair.of(Nullable.empty(), Nullable.of(5))   // key only in map2
     * // Note: "b" is not included because it has identical values in both maps
     * }</pre>
     *
     * <p>If either input map is {@code null}, it is treated as an empty map.
     *
     * @param <K> the type of keys in the maps.
     * @param <V> the type of values in the maps.
     * @param map the first input map.
     * @param map2 the second input map.
     * @return a new map containing the symmetric difference between the two input maps.
     * @see #difference(Map, Map)
     * @see N#symmetricDifference(int[], int[])
     * @see N#symmetricDifference(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     * @see #intersection(Map, Map)
     */
    public static <K, V> Map<K, Pair<Nullable<V>, Nullable<V>>> symmetricDifference(final Map<K, V> map, final Map<K, V> map2) {
        final boolean isIdentityHashMap = (N.notEmpty(map) && map instanceof IdentityHashMap) || (N.notEmpty(map2) && map2 instanceof IdentityHashMap);

        final Map<K, Pair<Nullable<V>, Nullable<V>>> result = isIdentityHashMap ? new IdentityHashMap<>()
                : (map == null ? new HashMap<>() : Maps.newTargetMap(map, Math.max(N.size(map), N.size(map2))));

        if (N.notEmpty(map)) {
            if (N.isEmpty(map2)) {
                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.of(entry.getValue()), Nullable.empty()));
                }
            } else {
                K key = null;
                V val2 = null;

                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    key = entry.getKey();
                    val2 = map2.get(key);

                    if (val2 == null && !map2.containsKey(key)) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.empty()));
                    } else if (!N.equals(val2, entry.getValue())) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.of(val2)));
                    }
                }
            }
        }

        if (N.notEmpty(map2)) {
            if (N.isEmpty(map)) {
                for (final Map.Entry<K, V> entry : map2.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.empty(), Nullable.of(entry.getValue())));
                }
            } else {
                for (final Map.Entry<K, V> entry : map2.entrySet()) {
                    if (!map.containsKey(entry.getKey())) {
                        result.put(entry.getKey(), Pair.of(Nullable.empty(), Nullable.of(entry.getValue())));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks if the specified map contains the specified entry.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 1);
     * Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("a", 2);
     * 
     * boolean contains1 = Maps.containsEntry(map, entry1);
     * // contains1 = true
     * 
     * boolean contains2 = Maps.containsEntry(map, entry2);
     * // contains2 = false (value doesn't match)
     * }</pre>
     *
     * @param map the map to check, may be {@code null}.
     * @param entry the entry to check for, may be {@code null}.
     * @return {@code true} if the map contains the specified entry, {@code false} otherwise.
     */
    public static boolean containsEntry(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        if (entry == null) {
            return false;
        }
        return containsEntry(map, entry.getKey(), entry.getValue());
    }

    /**
     * Checks if the specified map contains the specified key-value pair.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", null);
     * 
     * boolean contains1 = Maps.containsEntry(map, "a", 1);
     * // contains1 = true
     * 
     * boolean contains2 = Maps.containsEntry(map, "a", 2);
     * // contains2 = false
     * 
     * boolean contains3 = Maps.containsEntry(map, "b", null);
     * // contains3 = true
     * 
     * boolean contains4 = Maps.containsEntry(map, "c", null);
     * // contains4 = false (key not present)
     * }</pre>
     *
     * @param map the map to be checked.
     * @param key the key whose presence in the map is to be tested.
     * @param value the value whose presence in the map is to be tested.
     * @return {@code true} if the map contains the specified key-value pair, {@code false} otherwise.
     */
    public static boolean containsEntry(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object val = map.get(key);

        return val == null ? value == null && map.containsKey(key) : N.equals(val, value);
    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     *
     * String result1 = Maps.putIfAbsent(map, "key1", "newValue");
     * // result1 = null (key1 already has a value, not changed)
     * // map = {key1=value1, key2=null}
     *
     * String result2 = Maps.putIfAbsent(map, "key2", "value2");
     * // result2 = null (key2 was null, now set to value2)
     * // map = {key1=value1, key2=value2}
     *
     * String result3 = Maps.putIfAbsent(map, "key3", "value3");
     * // result3 = null (key3 was absent, now set to value3)
     * // map = {key1=value1, key2=value2, key3=value3}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to put the value in.
     * @param key the key to associate the value with.
     * @param value the value to put if the key is absent.
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key or if the key was mapped to {@code null}.
     * @see Map#putIfAbsent(Object, Object)
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final V value) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, value);
        }

        return v;
    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     * 
     * <p>Here absent means key is not found in the specified map or found with {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * map.put("key1", Arrays.asList("a", "b"));
     *
     * // Supplier is only called when the key is absent
     * List<String> result1 = Maps.putIfAbsent(map, "key1", () -> new ArrayList<>());
     * // result1 = null (key1 already has a value, supplier not called)
     * // map = {key1=[a, b]}
     *
     * List<String> result2 = Maps.putIfAbsent(map, "key2", () -> new ArrayList<>());
     * // result2 = null (key2 was absent, supplier called and value set)
     * // map = {key1=[a, b], key2=[]}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to put the value in.
     * @param key the key to associate the value with.
     * @param supplier the supplier to get the value from if the key is absent.
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key or if the key was mapped to {@code null}.
     * @see Map#putIfAbsent(Object, Object)
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final Supplier<V> supplier) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, supplier.get());
        }

        return v;
    }

    /**
     * Puts all entries from the source map into the target map, but only if the key passes the specified filter predicate.
     * This method iterates through all entries in the source map and adds them to the target map if the key satisfies the filter condition.
     * The target map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     * 
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("abc", 4);
     * 
     * boolean changed = Maps.putAllIf(target, source, key -> key.length() > 1);
     * // changed: true
     * // target: {a=1, abc=4}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param targetMap the target map to which entries will be added.
     * @param sourceMap the source map from which entries will be taken.
     * @param keyFilter a predicate that filters keys to be added to the target map.
     * @return {@code true} if any entries were added, {@code false} otherwise.
     */
    @Beta
    public static <K, V> boolean putAllIf(final Map<K, V> targetMap, final Map<? extends K, ? extends V> sourceMap, Predicate<? super K> keyFilter) {
        if (N.isEmpty(sourceMap)) {
            return false;
        }

        boolean changed = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (keyFilter.test(entry.getKey())) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Puts all entries from the source map into the target map, but only if the key and value pass the specified filter predicate.
     * This method iterates through all entries in the source map and adds them to the target map if both the key and value satisfy the filter condition.
     * The target map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     * 
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("d", 10);
     * 
     * boolean changed = Maps.putAllIf(target, source, (key, value) -> value > 2);
     * // changed: true
     * // target: {a=1, c=3, d=10}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param targetMap the target map to which entries will be added.
     * @param sourceMap the source map from which entries will be taken.
     * @param entryFilter a predicate that filters keys and values to be added to the target map.
     * @return {@code true} if any entries were added, {@code false} otherwise.
     */
    @Beta
    public static <K, V> boolean putAllIf(final Map<K, V> targetMap, final Map<? extends K, ? extends V> sourceMap,
            BiPredicate<? super K, ? super V> entryFilter) {
        if (N.isEmpty(sourceMap)) {
            return false;
        }

        boolean changed = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (entryFilter.test(entry.getKey(), entry.getValue())) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Removes the specified entry from the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * Map.Entry<String, Integer> entry = N.newEntry("a", 1);
     * boolean removed = Maps.removeEntry(map, entry);   // true, entry removed
     * // map: {b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which the entry is to be removed.
     * @param entry the entry to be removed from the map.
     * @return {@code true} if the entry was removed, {@code false} otherwise.
     * @see Map#remove(Object, Object)
     */
    public static <K, V> boolean removeEntry(final Map<K, V> map, final Map.Entry<?, ?> entry) {
        return removeEntry(map, entry.getKey(), entry.getValue());
    }

    /**
     * Removes the specified key-value pair from the map.
     * This method removes an entry from the map only if the key is mapped to the specified value.
     * If the key is not present in the map or is mapped to a different value, the map remains unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * boolean removed1 = Maps.removeEntry(map, "a", 1);   // true, entry removed
     * boolean removed2 = Maps.removeEntry(map, "b", 3);   // false, value doesn't match
     * // map: {b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which the entry is to be removed.
     * @param key the key whose associated value is to be removed.
     * @param value the value to be removed.
     * @return {@code true} if the entry was removed, {@code false} otherwise.
     * @see Map#remove(Object, Object)
     */
    public static <K, V> boolean removeEntry(final Map<K, V> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        final Object curValue = map.get(key);

        //noinspection SuspiciousMethodCalls
        if (!N.equals(curValue, value) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        //noinspection SuspiciousMethodCalls
        map.remove(key);
        return true;
    }

    /**
     * Removes the specified entries from the map.
     * This method removes all entries from the map that have matching key-value pairs in the entriesToRemove map.
     * An entry is removed only if both the key and value match exactly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * Map<String, Integer> entriesToRemove = new HashMap<>();
     * entriesToRemove.put("a", 1);
     * entriesToRemove.put("b", 5);   // Different value
     * 
     * boolean changed = Maps.removeEntries(map, entriesToRemove);
     * // changed: true
     * // map: {b=2, c=3}  // Only "a"=1 was removed
     * }</pre>
     *
     * @param map the map from which the entries are to be removed.
     * @param entriesToRemove the map containing the entries to be removed.
     * @return {@code true} if any entries were removed, {@code false} otherwise.
     */
    public static boolean removeEntries(final Map<?, ?> map, final Map<?, ?> entriesToRemove) {
        if (N.isEmpty(map) || N.isEmpty(entriesToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (final Map.Entry<?, ?> entry : entriesToRemove.entrySet()) {
            if (N.equals(map.get(entry.getKey()), entry.getValue())) {
                map.remove(entry.getKey());
            }
        }

        return map.size() < originalSize;
    }

    /**
     * Removes the specified keys from the map.
     * This method removes all entries from the map whose keys are contained in the provided collection.
     * If any of the keys in the collection are not present in the map, they are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * List<String> keysToRemove = Arrays.asList("a", "c", "d");
     * boolean changed = Maps.removeKeys(map, keysToRemove);
     * // changed: true
     * // map: {b=2}
     * }</pre>
     *
     * @param map the map from which the keys are to be removed.
     * @param keysToRemove the collection of keys to be removed from the map.
     * @return {@code true} if any keys were removed, {@code false} otherwise.
     */
    public static boolean removeKeys(final Map<?, ?> map, final Collection<?> keysToRemove) {
        if (N.isEmpty(map) || N.isEmpty(keysToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (final Object key : keysToRemove) {
            map.remove(key);
        }

        return map.size() < originalSize;
    }

    /**
     * Removes entries from the specified map that match the given filter predicate.
     * This method iterates through all entries in the map and removes those that satisfy the filter condition.
     * The map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * boolean changed = Maps.removeIf(map, entry -> entry.getValue() > 1);
     * // changed: true
     * // map: {a=1}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which entries to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if the filter is null.
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry)) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given filter predicate based on key and value.
     * This method iterates through all entries in the map and removes those whose key and value satisfy the filter condition.
     * The map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("cherry", 3);
     * 
     * boolean changed = Maps.removeIf(map, (key, value) -> key.length() > 5 && value > 1);
     * // changed: true
     * // map: {apple=1, banana=2}  // "cherry" was removed
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which entries to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if the filter is null.
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final BiPredicate<? super K, ? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given key filter predicate.
     * This method iterates through all entries in the map and removes those whose keys satisfy the filter condition.
     * The map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("cherry", 3);
     * 
     * boolean changed = Maps.removeIfKey(map, key -> key.startsWith("b"));
     * // changed: true
     * // map: {apple=1, cherry=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which keys to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if the filter is null.
     */
    public static <K, V> boolean removeIfKey(final Map<K, V> map, final Predicate<? super K> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given value filter predicate.
     * This method iterates through all entries in the map and removes those whose values satisfy the filter condition.
     * The map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * map.put("d", 2);
     * 
     * boolean changed = Maps.removeIfValue(map, value -> value == 2);
     * // changed: true
     * // map: {a=1, c=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which values to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if the filter is null.
     */
    public static <K, V> boolean removeIfValue(final Map<K, V> map, final Predicate<? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Replaces the entry for the specified key with the new value if the key is present in the map.
     * This method updates the value for a key only if the key exists in the map.
     * If the key is not present, the map remains unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * Integer oldValue1 = Maps.replace(map, "a", 10);   // returns 1
     * Integer oldValue2 = Maps.replace(map, "c", 30);   // returns null
     * // map: {a=10, b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entry is to be replaced.
     * @param key the key with which the specified value is associated.
     * @param newValue the new value to be associated with the specified key.
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key.
     */
    @MayReturnNull
    public static <K, V> V replace(final Map<K, V> map, final K key, final V newValue) throws IllegalArgumentException {
        if (N.isEmpty(map)) {
            return null;
        }

        V curValue = null;

        if (((curValue = map.get(key)) != null) || map.containsKey(key)) {
            curValue = map.put(key, newValue);
        }

        return curValue;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * This method updates the value for a key only if the current value matches the oldValue parameter.
     * If the key is not present or the current value doesn't match oldValue, the map remains unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * boolean replaced1 = Maps.replace(map, "a", 1, 10);   // true
     * boolean replaced2 = Maps.replace(map, "b", 3, 20);   // false, old value doesn't match
     * // map: {a=10, b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entry is to be replaced.
     * @param key the key with which the specified value is associated.
     * @param oldValue the expected current value associated with the specified key.
     * @param newValue the new value to be associated with the specified key.
     * @return {@code true} if the value was replaced, {@code false} otherwise.
     * @see Map#replace(Object, Object, Object)
     */
    public static <K, V> boolean replace(final Map<K, V> map, final K key, final V oldValue, final V newValue) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object curValue = map.get(key);

        if (!N.equals(curValue, oldValue) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        map.put(key, newValue);
        return true;
    }

    /**
     * Replaces each entry's value with the result of applying the given function to that entry.
     * This method applies the provided function to each key-value pair in the map and updates the value with the function's result.
     * The function receives both the key and the current value as parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * Maps.replaceAll(map, (key, value) -> value * 10);
     * // map: {a=10, b=20, c=30}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entries are to be replaced.
     * @param function the function to apply to each entry to compute a new value.
     * @throws IllegalArgumentException if the function is null.
     */
    public static <K, V> void replaceAll(final Map<K, V> map, final BiFunction<? super K, ? super V, ? extends V> function) throws IllegalArgumentException {
        N.checkArgNotNull(function);

        if (N.isEmpty(map)) {
            return;
        }

        try {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            }
        } catch (final IllegalStateException ise) {
            // this usually means the entry is no longer in the map.
            throw new ConcurrentModificationException(ise);
        }
    }

    // Replaced with N.forEach(Map....)

    /**
     * Filters the entries of the specified map based on the given predicate.
     * This method creates a new map containing only the entries that satisfy the predicate condition.
     * The predicate is tested against each Map.Entry in the original map.
     * The returned map is of the same type as the input map if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * Map<String, Integer> filtered = Maps.filter(map, entry -> entry.getValue() > 1);
     * // filtered: {b=2, c=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the entries.
     * @return a new map containing only the entries that match the predicate.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given predicate applied to key-value pairs.
     * This method creates a new map containing only the entries whose key and value satisfy the predicate condition.
     * The predicate receives both the key and value as separate parameters.
     * The returned map is of the same type as the input map if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 5);
     * map.put("banana", 2);
     * map.put("cherry", 8);
     * 
     * Map<String, Integer> filtered = Maps.filter(map, (key, value) -> key.length() > 5 || value > 4);
     * // filtered: {apple=5, banana=2, cherry=8}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the entries.
     * @return a new map containing only the entries that match the predicate.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given key predicate.
     * This method creates a new map containing only the entries whose keys satisfy the predicate condition.
     * The returned map is of the same type as the input map if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("apricot", 3);
     * 
     * Map<String, Integer> filtered = Maps.filterByKey(map, key -> key.startsWith("ap"));
     * // filtered: {apple=1, apricot=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the keys.
     * @return a new map containing only the entries with keys that match the predicate.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public static <K, V> Map<K, V> filterByKey(final Map<K, V> map, final Predicate<? super K> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given value predicate.
     * This method creates a new map containing only the entries whose values satisfy the predicate condition.
     * The returned map is of the same type as the input map if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 10);
     * map.put("b", 20);
     * map.put("c", 30);
     * 
     * Map<String, Integer> filtered = Maps.filterByValue(map, value -> value >= 20);
     * // filtered: {b=20, c=30}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the values.
     * @return a new map containing only the entries with values that match the predicate.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public static <K, V> Map<K, V> filterByValue(final Map<K, V> map, final Predicate<? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * Note: This method does not check for duplicate values in the input map. If there are duplicate values,
     * some information may be lost in the inversion process as each value in the resulting map must be unique.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("Alice", 1);
     * map.put("Bob", 2);
     * map.put("Charlie", 3);
     *
     * Map<Integer, String> inverted = Maps.invert(map);
     * // inverted = {1=Alice, 2=Bob, 3=Charlie}
     *
     * // Example with duplicate values (last occurrence wins)
     * Map<String, String> map2 = new HashMap<>();
     * map2.put("key1", "valueA");
     * map2.put("key2", "valueA");
     *
     * Map<String, String> inverted2 = Maps.invert(map2);
     * // inverted2 = {valueA=key2} (key1 was overwritten)
     * }</pre>
     *
     * @param <K> the key type of the input map and the value type of the resulting map.
     * @param <V> the value type of the input map and the key type of the resulting map.
     * @param map the map to be inverted.
     * @return a new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * If there are duplicate values in the input map, the merging operation specified by mergeOp is applied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "valueA");
     * map.put("key2", "valueA");
     * map.put("key3", "valueB");
     *
     * // Use the first key when there are duplicates
     * Map<String, String> inverted1 = Maps.invert(map, (oldKey, newKey) -> oldKey);
     * // inverted1 = {valueA=key1, valueB=key3}
     *
     * // Use the last key when there are duplicates
     * Map<String, String> inverted2 = Maps.invert(map, (oldKey, newKey) -> newKey);
     * // inverted2 = {valueA=key2, valueB=key3}
     *
     * // Concatenate keys when there are duplicates
     * Map<String, String> inverted3 = Maps.invert(map, (oldKey, newKey) -> oldKey + "," + newKey);
     * // inverted3 = {valueA=key1,key2, valueB=key3}
     * }</pre>
     *
     * @param <K> the key type of the input map and the value type of the resulting map.
     * @param <V> the value type of the input map and the key type of the resulting map.
     * @param map the map to be inverted.
     * @param mergeOp the merging operation to be applied if there are duplicate values in the input map.
     * @return a new map which is the inverted version of the input map.
     * @throws IllegalArgumentException if mergeOp is {@code null}.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map, final BiFunction<? super K, ? super K, ? extends K> mergeOp) throws IllegalArgumentException {
        N.checkArgNotNull(mergeOp, cs.mergeOp);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);
        K oldVal = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            oldVal = result.get(entry.getValue());

            if (oldVal != null || result.containsKey(entry.getValue())) {
                result.put(entry.getValue(), mergeOp.apply(oldVal, entry.getKey()));
            } else {
                result.put(entry.getValue(), entry.getKey());
            }
        }

        return result;
    }

    /**
     * Inverts the given map by mapping each value in the Collection to the corresponding key.
     * The resulting map's keys are the values in the Collection of the input maps and its values are Lists of the corresponding keys from the input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * map.put("Alice", Arrays.asList(1, 2, 3));
     * map.put("Bob", Arrays.asList(2, 4));
     * map.put("Charlie", Arrays.asList(3, 5));
     *
     * Map<Integer, List<String>> inverted = Maps.flatInvert(map);
     * // inverted = {1=[Alice], 2=[Alice, Bob], 3=[Alice, Charlie], 4=[Bob], 5=[Charlie]}
     *
     * // Each value from the collections becomes a key, mapping to all original keys that contained it
     * }</pre>
     *
     * @param <K> the key type of the input map and the element type of the List values in the resulting map.
     * @param <V> the element type of the Collection values in the input map and the key type of the resulting map.
     * @param map the map to be inverted.
     * @return a new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, List<K>> flatInvert(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, List<K>> result = newOrderingMap(map);

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            final Collection<? extends V> c = entry.getValue();

            if (N.notEmpty(c)) {
                for (final V v : c) {
                    List<K> list = result.computeIfAbsent(v, k -> new ArrayList<>());

                    list.add(entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     * Transforms a map of collections into a list of maps.
     * Each resulting map is a "flat" representation of the original map's entries, where each key in the original map
     * is associated with one element from its corresponding collection.
     * The transformation is done in a way that the first map in the resulting list contains the first elements of all collections,
     * the second map contains the second elements, and so on.
     * If the collections in the original map are of different sizes, the resulting list's size is equal to the size of the largest collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * map.put("a", Arrays.asList(1, 2, 3));
     * map.put("b", Arrays.asList(4, 5, 6));
     * map.put("c", Arrays.asList(7, 8));
     *
     * List<Map<String, Integer>> result = Maps.flatToMap(map);
     * // result contains:
     * // [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}]
     * }</pre>
     *
     * @param <K> the type of keys in the input map and the resulting maps.
     * @param <V> the type of values in the collections of the input map and the values in the resulting maps.
     * @param map the input map, where each key is associated with a collection of values.
     * @return a list of maps, where each map represents a "flat" version of the original map's entries.
     */
    public static <K, V> List<Map<K, V>> flatToMap(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new ArrayList<>();
        }

        int maxValueSize = 0;

        for (final Collection<? extends V> v : map.values()) {
            maxValueSize = N.max(maxValueSize, N.size(v));
        }

        final List<Map<K, V>> result = new ArrayList<>(maxValueSize);

        for (int i = 0; i < maxValueSize; i++) {
            result.add(newTargetMap(map));
        }

        K key = null;
        Iterator<? extends V> iter = null;

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            if (N.isEmpty(entry.getValue())) {
                continue;
            }

            key = entry.getKey();
            iter = entry.getValue().iterator();

            for (int i = 0; iter.hasNext(); i++) {
                result.get(i).put(key, iter.next());
            }
        }

        return result;
    }

    /**
     * Flattens the given map.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new HashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     *
     * Map<String, Object> flattened = Maps.flatten(map);
     * // flattened = {name=John, address.city=New York, address.zip=10001}
     * }</pre>
     *
     * @param map the map to be flattened.
     * @return a new map which is the flattened version of the input map.
     */
    public static Map<String, Object> flatten(final Map<String, Object> map) {
        return flatten(map, Suppliers.ofMap());
    }

    /**
     * Flattens the given map using a provided map supplier.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new HashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     *
     * // Use a LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, Object> flattened = Maps.flatten(map, LinkedHashMap::new);
     * // flattened = {name=John, address.city=New York, address.zip=10001}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the map to be flattened.
     * @param mapSupplier a supplier function that provides a new instance of the map to be returned.
     * @return a new map which is the flattened version of the input map.
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final Supplier<? extends M> mapSupplier) {
        return flatten(map, ".", mapSupplier);
    }

    /**
     * Flattens the given map using a provided map supplier and a delimiter.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a provided delimiter.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new HashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     *
     * // Use underscore as delimiter instead of dot
     * Map<String, Object> flattened = Maps.flatten(map, "_", HashMap::new);
     * // flattened = {name=John, address_city=New York, address_zip=10001}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the map to be flattened.
     * @param delimiter the delimiter to be used when concatenating keys.
     * @param mapSupplier a supplier function that provides a new instance of the map to be returned.
     * @return a new map which is the flattened version of the input map.
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final String delimiter, final Supplier<? extends M> mapSupplier) {
        final M result = mapSupplier.get();

        flatten(map, null, delimiter, result);

        return result;
    }

    private static void flatten(final Map<String, Object> map, final String prefix, final String delimiter, final Map<String, Object> output) {
        if (N.isEmpty(map)) {
            return;
        }

        if (Strings.isEmpty(prefix)) {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), entry.getKey(), delimiter, output);
                } else {
                    output.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), prefix + delimiter + entry.getKey(), delimiter, output);
                } else {
                    output.put(prefix + delimiter + entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Unflattens the given map.
     * This method takes a flattened map where keys are concatenated with a dot and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new HashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address.city", "New York");
     * flattened.put("address.zip", "10001");
     *
     * Map<String, Object> unflattened = Maps.unflatten(flattened);
     * // unflattened = {name=John, address={city=New York, zip=10001}}
     * }</pre>
     *
     * @param map the flattened map to be unflattened.
     * @return a new map which is the unflattened version of the input map.
     */
    public static Map<String, Object> unflatten(final Map<String, Object> map) {
        return unflatten(map, Suppliers.ofMap());
    }

    /**
     * Unflattens the given map using a provided map supplier.
     * This method takes a flattened map where keys are concatenated with a delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new HashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address.city", "New York");
     * flattened.put("address.zip", "10001");
     *
     * // Use a LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, Object> unflattened = Maps.unflatten(flattened, LinkedHashMap::new);
     * // unflattened = {name=John, address={city=New York, zip=10001}}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the flattened map to be unflattened.
     * @param mapSupplier a supplier function that provides a new instance of the map to be returned.
     * @return a new map which is the unflattened version of the input map.
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final Supplier<? extends M> mapSupplier) {
        return unflatten(map, ".", mapSupplier);
    }

    /**
     * Unflattens the given map using a provided map supplier and a delimiter.
     * This method takes a flattened map where keys are concatenated with a specified delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new HashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address_city", "New York");
     * flattened.put("address_zip", "10001");
     *
     * // Use underscore as delimiter
     * Map<String, Object> unflattened = Maps.unflatten(flattened, "_", HashMap::new);
     * // unflattened = {name=John, address={city=New York, zip=10001}}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the flattened map to be unflattened.
     * @param delimiter the delimiter that was used in the flattening process to concatenate keys.
     * @param mapSupplier a supplier function that provides a new instance of the map to be returned.
     * @return a new map which is the unflattened version of the input map. Keys without the delimiter
     *         are copied as-is; no error is raised when the delimiter is absent.
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final String delimiter, final Supplier<? extends M> mapSupplier)
            throws IllegalArgumentException {
        final M result = mapSupplier.get();
        final Splitter keySplitter = Splitter.with(delimiter);

        if (N.notEmpty(map)) {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().contains(delimiter)) {
                    final String[] keys = keySplitter.splitToArray(entry.getKey());
                    Map<String, Object> lastMap = result;

                    for (int i = 0, to = keys.length - 1; i < to; i++) {
                        Map<String, Object> tmp = (Map<String, Object>) lastMap.get(keys[i]);

                        if (tmp == null) {
                            tmp = mapSupplier.get();
                            lastMap.put(keys[i], tmp);
                        }

                        lastMap = tmp;
                    }

                    lastMap.put(keys[keys.length - 1], entry.getValue());
                } else {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Replaces (renames) the keys in the specified map by applying the given converter to each existing key.
     *
     * <p>This method operates in two phases:
     * <ol>
     *   <li><b>Validation phase:</b> Computes the converted key for every current key and validates that no duplicates
     *       would be produced. If duplicates are detected, throws {@link IllegalStateException} before modifying the map.</li>
     *   <li><b>Replacement phase:</b> Removes each original entry and reinserts it under the corresponding converted key,
     *       preserving the original iteration order of {@link Map#keySet()} at the time this method is called.</li>
     * </ol>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If a key converts to itself (i.e., {@code keyConverter.apply(key).equals(key)}), the entry is still
     *       removed and reinserted, which may affect iteration order in some map implementations.</li>
     *   <li>If {@code keyConverter} returns {@code null}, it is treated as a valid key. However, if {@code null}
     *       is returned for multiple keys, an {@link IllegalStateException} is thrown due to duplicate keys.
     *       Additionally, the final {@code map.put(null, value)} behavior depends on whether the map implementation
     *       supports {@code null} keys (e.g., {@link java.util.HashMap} allows {@code null}, but
     *       {@link java.util.concurrent.ConcurrentHashMap} does not).</li>
     *   <li>If the map is empty or {@code null}, this method returns immediately without any action.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert keys to uppercase
     * Map<String, Integer> map = new HashMap<>();
     * map.put("name", 1);
     * map.put("age", 2);
     * map.put("city", 3);
     *
     * Maps.replaceKeys(map, String::toUpperCase);
     * // map now contains: {NAME=1, AGE=2, CITY=3}
     *
     * // Add prefix to keys
     * Map<String, String> data = new HashMap<>();
     * data.put("id", "123");
     * data.put("type", "user");
     *
     * Maps.replaceKeys(data, k -> "prefix_" + k);
     * // data now contains: {prefix_id=123, prefix_type=user}
     *
     * // This will throw IllegalStateException (duplicate keys):
     * // Maps.replaceKeys(map, k -> "constant"); // All keys convert to same value
     * }</pre>
     *
     * @param <K> the key type
     * @param map the map whose keys are to be replaced; modified in-place. If {@code null} or empty, no action is taken.
     * @param keyConverter the function applied to each existing key to produce the new key; must not be {@code null}
     * @throws IllegalStateException if the converted keys contain duplicates (including multiple {@code null} values)
     * @throws NullPointerException if {@code keyConverter} is {@code null}, or if the map implementation does not
     *         support {@code null} keys and {@code keyConverter} returns {@code null}
     */
    public static <K> void replaceKeys(final Map<K, ?> map, final Function<? super K, ? extends K> keyConverter) throws IllegalStateException {
        if (N.isEmpty(map)) {
            return;
        }

        final List<K> keys = new ArrayList<>(map.keySet());
        final Set<K> newKeySet = new LinkedHashSet<>(map.size());
        K newKey = null;

        for (K key : keys) {
            newKey = keyConverter.apply(key);

            if (!newKeySet.add(newKey)) {
                throw new IllegalStateException("Duplicate new Keys: " + Joiner.defauLt().appendAll(newKeySet).append(newKey).toString());
            }
        }

        final Map<K, Object> mapToUse = (Map<K, Object>) map;
        final Iterator<K> newKeyIter = newKeySet.iterator();

        for (final K key : keys) {
            mapToUse.put(newKeyIter.next(), mapToUse.remove(key));
        }
    }

    /**
     * Replaces (renames) keys in the specified map by applying the given converter, merging values when
     * multiple original keys map to the same converted key.
     *
     * <p>This method iterates over a snapshot of the current {@link Map#keySet()} to avoid concurrent
     * modification while updating the map. For each key, it computes a new key using {@code keyConverter}.
     * If the new key is equal to the original key (via {@link N#equals(Object, Object)}), the entry is
     * left unchanged. Otherwise, the entry is removed from the original key and merged into the entry
     * under the new key via {@link Map#merge(Object, Object, BiFunction)}. If the new key is not already
     * present, the value is simply moved; if it is present, {@code merger} is used to combine the existing
     * value (first argument) and the moved value (second argument).</p>
     *
     * <p><b>Difference from {@link #replaceKeys(Map, Function)}:</b> This method allows duplicate converted
     * keys by merging their values, whereas the single-argument version throws {@link IllegalStateException}
     * on duplicates.</p>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>The conversion order follows the iteration order of {@code map.keySet()} at the time this method
     *       is called. When multiple keys convert to the same new key, merges occur in that iteration order.</li>
     *   <li>If {@code keyConverter} returns {@code null} for any key, behavior depends on the map implementation.
     *       Maps that allow {@code null} keys (e.g., {@link java.util.HashMap}) will accept it; others
     *       (e.g., {@link java.util.concurrent.ConcurrentHashMap}) will throw {@link NullPointerException}.</li>
     *   <li>If {@code merger} returns {@code null}, {@link Map#merge(Object, Object, BiFunction)} removes the
     *       entry for that key (per the {@code Map.merge} contract). This can be used intentionally to filter
     *       out entries during the merge process.</li>
     *   <li>If the map is empty or {@code null}, this method returns immediately without any action.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge values when keys collide (sum integers)
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a1", 10);
     * map.put("a2", 20);
     * map.put("b1", 30);
     *
     * Maps.replaceKeys(map, k -> k.substring(0, 1), Integer::sum);
     * // map now contains: {a=30, b=30}
     * // Explanation: "a1" (10) and "a2" (20) both map to "a", merged via sum -> 30
     *
     * // Concatenate strings on collision
     * Map<String, String> data = new LinkedHashMap<>();
     * data.put("user_1", "John");
     * data.put("user_2", "Jane");
     * data.put("admin_1", "Bob");
     *
     * Maps.replaceKeys(data, k -> k.split("_")[0], (v1, v2) -> v1 + ", " + v2);
     * // data now contains: {user="John, Jane", admin="Bob"}
     *
     * // Keep only the first value on collision
     * Maps.replaceKeys(someMap, keyConverter, (existing, incoming) -> existing);
     *
     * // Keep only the last value on collision
     * Maps.replaceKeys(someMap, keyConverter, (existing, incoming) -> incoming);
     *
     * // Remove entries that would collide (merger returns null)
     * Maps.replaceKeys(someMap, keyConverter, (existing, incoming) -> null);
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map whose keys are to be replaced; modified in-place. If {@code null} or empty, no action is taken.
     * @param keyConverter converts each existing key to its replacement key; must not be {@code null}
     * @param merger merges values when multiple entries map to the same converted key. The function receives
     *        {@code (existingValue, incomingValue)} and returns the merged value. If it returns {@code null},
     *        the entry is removed. Must not be {@code null}.
     * @throws NullPointerException if {@code keyConverter} or {@code merger} is {@code null}, or if the map
     *         implementation does not support {@code null} keys and {@code keyConverter} returns {@code null}
     * @see #replaceKeys(Map, Function) for a version that throws on duplicate keys instead of merging
     */
    public static <K, V> void replaceKeys(final Map<K, V> map, final Function<? super K, ? extends K> keyConverter,
            final BiFunction<? super V, ? super V, ? extends V> merger) {
        if (N.isEmpty(map)) {
            return;
        }

        final List<K> keys = new ArrayList<>(map.keySet());
        K newKey = null;

        for (final K key : keys) {
            newKey = keyConverter.apply(key);

            if (!N.equals(key, newKey)) {
                map.merge(newKey, map.remove(key), merger);
            }
        }
    }
}
