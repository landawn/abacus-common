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
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
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
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 *
 * <br />
 * Present -> key is found in the specified map with {@code non-null} value.
 * <br />
 * Absent -> key is not found in the specified map or found with {@code null} value.
 * <br />
 * <br />
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. For example, if
 * user tries to reverse a {@code null} or empty String. The input String will be
 * returned. But exception will be thrown if try to add an element to a {@code null} Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Strings
 */
public final class Maps {

    private static final Object NONE = ClassUtil.createNullMask();

    private Maps() {
        // Utility class.
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on)
     * 
     * <p>Example:
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
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final Map<K, V> result = N.newHashMap(minLen);

        for (int i = 0; i < minLen; i++) {
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
     * @param key The key of the new entry.
     * @param value The value of the new entry.
     * @return A new Entry with the provided key and value.
     * @deprecated replaced by {@link N#newEntry(Object, Object)}
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
     * @param key The key of the new entry.
     * @param value The value of the new entry.
     * @return A new ImmutableEntry with the provided key and value.
     * @deprecated replaced by {@link N#newImmutableEntry(Object, Object)}
     */
    @Deprecated
    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(final K key, final V value) {
        return N.newImmutableEntry(key, value);
    }

    private static final Set<Class<?>> UNABLE_CREATED_MAP_CLASSES = N.newConcurrentHashSet();

    /**
     * New target map.
     *
     * @param m
     * @return
     */
    @SuppressWarnings("rawtypes")
    static Map newTargetMap(final Map<?, ?> m) {
        return newTargetMap(m, m == null ? 0 : m.size());
    }

    /**
     * New target map.
     *
     * @param m
     * @param size
     * @return
     */
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

    /**
     * New ordering map.
     *
     * @param m
     * @return
     */
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
     * Returns the key set of the specified map if it is not null or empty. Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids null checks and provides a guaranteed non-null Set result.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * Set<String> keys = Maps.keys(map);
     * // keys contains: ["one", "two"]
     * 
     * Map<String, Integer> emptyMap = null;
     * Set<String> emptyKeys = Maps.keys(emptyMap);
     * // emptyKeys is an empty immutable set
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map whose keys are to be returned, may be null
     * @return the key set of the map if non-empty, otherwise an empty immutable set
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <K> Set<K> keys(final Map<? extends K, ?> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : (Set<K>) map.keySet();
    }

    /**
     * Returns the collection of values from the specified map if it is not null or empty. 
     * Otherwise, an empty immutable list is returned.
     * This is a convenience method that avoids null checks and provides a guaranteed non-null Collection result.
     *
     * <p>Example:
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
     * @param <V> the type of values in the map
     * @param map the map whose values are to be returned, may be null
     * @return the collection of values from the map if non-empty, otherwise an empty immutable list
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <V> Collection<V> values(final Map<?, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableList.empty() : (Collection<V>) map.values();
    }

    /**
     * Returns the entry set of the specified map if it is not null or empty. 
     * Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids null checks and provides a guaranteed non-null Set result.
     *
     * <p>Example:
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
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose entry set is to be returned, may be null
     * @return the entry set of the map if non-empty, otherwise an empty immutable set
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
     * <p>Example:
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "c");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
     * // result: {a=1, b=2, c=3} (maintains insertion order)
     * }</pre>
     *
     * @param <K> The type of keys in the resulting Map
     * @param <V> The type of values in the resulting Map
     * @param <M> The type of the resulting Map
     * @param keys An Iterable of keys for the resulting Map
     * @param values An Iterable of values for the resulting Map
     * @param mapSupplier A function that generates a new Map instance based on expected size
     * @return A Map where each key from the keys Iterable is associated with the corresponding value from the values Iterable
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables with a merge function to handle duplicate keys.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * If duplicate keys are encountered, the merge function is used to resolve the conflict.
     *
     * <p>Example:
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "a");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * Map<String, Integer> result = Maps.zip(keys, values, 
     *     (v1, v2) -> v1 + v2, HashMap::new);
     * // result: {a=4, b=2} (values for duplicate key "a" are summed: 1 + 3 = 4)
     * }</pre>
     *
     * @param <K> The type of keys in the resulting Map
     * @param <V> The type of values in the resulting Map
     * @param <M> The type of the resulting Map
     * @param keys An Iterable of keys for the resulting Map
     * @param values An Iterable of values for the resulting Map
     * @param mergeFunction A function used to resolve conflicts when duplicate keys are encountered
     * @param mapSupplier A function that generates a new Map instance based on expected size
     * @return A Map where each key from the keys Iterable is associated with the corresponding value from the values Iterable
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables with default values for missing elements.
     * The resulting Map will have the size of the longer Iterable.
     * If one Iterable is shorter, the default value is used for the missing elements.
     *
     * <p>Example:
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b");
     * List<Integer> values = Arrays.asList(1, 2, 3, 4);
     * Map<String, Integer> result = Maps.zip(keys, values, "default", 0);
     * // result: {a=1, b=2, default=3}
     * // Note: only one default key is used for remaining values
     * }</pre>
     *
     * @param <K> The type of keys in the resulting Map
     * @param <V> The type of values in the resulting Map
     * @param keys An Iterable of keys for the resulting Map
     * @param values An Iterable of values for the resulting Map
     * @param defaultForKey The default key to use when keys Iterable is shorter than values
     * @param defaultForValue The default value to use when values Iterable is shorter than keys
     * @return A Map where each key is associated with the corresponding value, using defaults for missing elements
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K defaultForKey, final V defaultForValue) {
        return zip(keys, values, defaultForKey, defaultForValue, Fn.selectFirst(), IntFunctions.ofMap());
    }

    /**
     * Creates a Map by zipping together two Iterables with default values and custom merge function.
     * The resulting Map will have entries for all elements from both Iterables.
     * If one Iterable is shorter, default values are used. Duplicate keys are handled by the merge function.
     *
     * <p>Example:
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * Map<String, Integer> result = Maps.zip(keys, values, "sum", 10,
     *     (v1, v2) -> v1 + v2, HashMap::new);
     * // result: {a=1, b=2, sum=3}
     * }</pre>
     *
     * @param <K> The type of keys in the resulting Map
     * @param <V> The type of values in the resulting Map
     * @param <M> The type of the resulting Map
     * @param keys An Iterable of keys for the resulting Map
     * @param values An Iterable of values for the resulting Map
     * @param defaultForKey The default key to use when keys Iterable is shorter than values
     * @param defaultForValue The default value to use when values Iterable is shorter than keys
     * @param mergeFunction A function used to resolve conflicts when duplicate keys are encountered
     * @param mapSupplier A function that generates a new Map instance
     * @return A Map where each key is associated with the corresponding value
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
     * Returns a {@code Nullable} with the value to which the specified key is mapped,
     * or an empty {@code Nullable} if the map is empty or contains no mapping for the key.
     * This method properly handles null values in the map.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     * 
     * Nullable<String> result1 = Maps.get(map, "key1");  // Nullable.of("value1")
     * Nullable<String> result2 = Maps.get(map, "key2");  // Nullable.of(null)
     * Nullable<String> result3 = Maps.get(map, "key3");  // Nullable.empty()
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return a {@code Nullable<V>} with the value mapped by the specified key, or an empty {@code Nullable<V>} if the map is empty or contains no value for the key
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     */
    public static <K, V> Nullable<V> get(final Map<K, ? extends V> map, final K key) {
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

    //    /**
    //     * Returns the value to which the specified key is mapped if the value not {@code null},
    //     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsent(Map, Object, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getOrDefaultIfNull(final Map<K, ? extends V> map, final K key, final V defaultForNull) throws IllegalArgumentException {
    //        return getOrDefaultIfAbsent(map, key, defaultForNull);
    //    }

    /**
     * Returns a {@code Nullable} with the value from a nested map structure.
     * First retrieves the inner map using the outer key, then retrieves the value using the inner key.
     * Returns an empty {@code Nullable} if either map is empty or the keys are not found.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Map<String, Integer>> nestedMap = new HashMap<>();
     * Map<String, Integer> innerMap = new HashMap<>();
     * innerMap.put("inner1", 100);
     * nestedMap.put("outer1", innerMap);
     * 
     * Nullable<Integer> result1 = Maps.get(nestedMap, "outer1", "inner1");  // Nullable.of(100)
     * Nullable<Integer> result2 = Maps.get(nestedMap, "outer1", "inner2");  // Nullable.empty()
     * Nullable<Integer> result3 = Maps.get(nestedMap, "outer2", "inner1");  // Nullable.empty()
     * }</pre>
     *
     * @param <K> the type of the outer map's keys
     * @param <K2> the type of the inner map's keys
     * @param <V2> the type of the inner map's values
     * @param map the outer map containing inner maps
     * @param key the key for the outer map
     * @param k2 the key for the inner map
     * @return a {@code Nullable<V2>} with the value if found, or empty if not found
     * @see #getOrDefaultIfAbsent(Map, Object, Object, Object)
     */
    public static <K, K2, V2> Nullable<V2> get(final Map<K, ? extends Map<K2, V2>> map, final K key, final K2 k2) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<K2, V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null || m2.containsKey(k2)) {
                return Nullable.of(v2);
            }
        }

        return Nullable.empty();
    }

    /**
     * Returns the value to which the specified key is mapped, or defaultValue if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is null.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     * 
     * String result1 = Maps.getOrDefaultIfAbsent(map, "key1", "default");  // "value1"
     * String result2 = Maps.getOrDefaultIfAbsent(map, "key2", "default");  // "default"
     * String result3 = Maps.getOrDefaultIfAbsent(map, "key3", "default");  // "default"
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default value to return if the key is absent (must not be null)
     * @return the value mapped by the key, or defaultValue if the key is absent
     * @throws IllegalArgumentException if defaultValue is null
     * @see #get(Map, Object)
     * @see #getNonNull(Map, Object, Object)
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

    //    /**
    //     * Returns the value to which the specified key is mapped if the value not {@code null},
    //     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsent(Map, Object, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getOrDefaultIfNull(final Map<K, ? extends V> map, final K key, final V defaultForNull) throws IllegalArgumentException {
    //        return getOrDefaultIfAbsent(map, key, defaultForNull);
    //    }

    /**
     * Returns the value from a nested map structure, or defaultValue if not found.
     * First retrieves the inner map using the outer key, then retrieves the value using the inner key.
     * Returns defaultValue if either map is empty, keys are not found, or the value is null.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Map<String, Integer>> nestedMap = new HashMap<>();
     * Map<String, Integer> innerMap = new HashMap<>();
     * innerMap.put("count", 5);
     * nestedMap.put("data", innerMap);
     * 
     * Integer result1 = Maps.getOrDefaultIfAbsent(nestedMap, "data", "count", 0);    // 5
     * Integer result2 = Maps.getOrDefaultIfAbsent(nestedMap, "data", "missing", 0);  // 0
     * Integer result3 = Maps.getOrDefaultIfAbsent(nestedMap, "missing", "count", 0); // 0
     * }</pre>
     *
     * @param <K> the type of the outer map's keys
     * @param <K2> the type of the inner map's keys
     * @param <V2> the type of the inner map's values
     * @param map the outer map containing inner maps
     * @param key the key for the outer map
     * @param k2 the key for the inner map
     * @param defaultValue the default value to return if not found (must not be null)
     * @return the value if found, or defaultValue if not found
     * @throws IllegalArgumentException if defaultValue is null
     * @see #get(Map, Object, Object)
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

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code List} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptyListIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E, V extends List<E>> List<E> getOrEmptyListIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptyListIfAbsent(map, key);
    //    }

    /**
     * Returns the List value to which the specified key is mapped, or an empty immutable List if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is null.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * map.put("fruits", Arrays.asList("apple", "banana"));
     * map.put("empty", null);
     * 
     * List<String> result1 = Maps.getOrEmptyListIfAbsent(map, "fruits");   // ["apple", "banana"]
     * List<String> result2 = Maps.getOrEmptyListIfAbsent(map, "empty");    // []
     * List<String> result3 = Maps.getOrEmptyListIfAbsent(map, "missing");  // []
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <E> the type of elements in the list
     * @param <V> the type of list values maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the List value mapped by the key, or an empty immutable List if the key is absent
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

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code Set} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptySetIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E, V extends Set<E>> Set<E> getOrEmptySetIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptySetIfAbsent(map, key);
    //    }

    /**
     * Returns the Set value to which the specified key is mapped, or an empty immutable Set if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is null.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Set<Integer>> map = new HashMap<>();
     * map.put("primes", new HashSet<>(Arrays.asList(2, 3, 5, 7)));
     * map.put("empty", null);
     * 
     * Set<Integer> result1 = Maps.getOrEmptySetIfAbsent(map, "primes");   // {2, 3, 5, 7}
     * Set<Integer> result2 = Maps.getOrEmptySetIfAbsent(map, "empty");    // {}
     * Set<Integer> result3 = Maps.getOrEmptySetIfAbsent(map, "missing");  // {}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <E> the type of elements in the set
     * @param <V> the type of set values maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the Set value mapped by the key, or an empty immutable Set if the key is absent
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

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code Map} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <KK> the key type of value map
    //     * @param <VV> the value type of value map
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptyMapIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV, V extends Map<KK, VV>> Map<KK, VV> getOrEmptyMapIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptyMapIfAbsent(map, key);
    //    }

    /**
     * Returns the Map value to which the specified key is mapped, or an empty immutable Map if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is null.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     * Map<String, Integer> innerMap = new HashMap<>();
     * innerMap.put("a", 1);
     * innerMap.put("b", 2);
     * map.put("data", innerMap);
     * map.put("empty", null);
     * 
     * Map<String, Integer> result1 = Maps.getOrEmptyMapIfAbsent(map, "data");    // {a=1, b=2}
     * Map<String, Integer> result2 = Maps.getOrEmptyMapIfAbsent(map, "empty");   // {}
     * Map<String, Integer> result3 = Maps.getOrEmptyMapIfAbsent(map, "missing"); // {}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the outer map
     * @param <KK> the type of keys maintained by the inner map
     * @param <VV> the type of values maintained by the inner map
     * @param <V> the type of map values maintained by the outer map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the Map value mapped by the key, or an empty immutable Map if the key is absent
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
     * Returns an OptionalBoolean containing the boolean value mapped to the specified key.
     * If the map is empty, the key is not found, or the mapped value is null, returns an empty OptionalBoolean.
     * Non-Boolean values are converted to boolean using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("flag1", true);
     * map.put("flag2", "false");
     * map.put("flag3", null);
     * 
     * OptionalBoolean result1 = Maps.getBoolean(map, "flag1");  // OptionalBoolean.of(true)
     * OptionalBoolean result2 = Maps.getBoolean(map, "flag2");  // OptionalBoolean.of(false)
     * OptionalBoolean result3 = Maps.getBoolean(map, "flag3");  // OptionalBoolean.empty()
     * OptionalBoolean result4 = Maps.getBoolean(map, "flag4");  // OptionalBoolean.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an OptionalBoolean containing the boolean value, or empty if not found
     */
    public static <K> OptionalBoolean getBoolean(final Map<? super K, ?> map, final K key) {
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
     * Returns the boolean value mapped to the specified key, or defaultForNull if not found.
     * If the map is empty, the key is not found, or the mapped value is null, returns defaultForNull.
     * Non-Boolean values are converted to boolean using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("enabled", true);
     * map.put("disabled", "false");
     * 
     * boolean result1 = Maps.getBoolean(map, "enabled", false);   // true
     * boolean result2 = Maps.getBoolean(map, "disabled", true);   // false
     * boolean result3 = Maps.getBoolean(map, "missing", true);    // true (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the key is not found or value is null
     * @return the boolean value mapped to the key, or defaultForNull if not found
     */
    public static <K> boolean getBoolean(final Map<? super K, ?> map, final K key, final boolean defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else {
            return Strings.parseBoolean(N.toString(val));
        }
    }

    /**
     * Returns an OptionalChar containing the character value mapped to the specified key.
     * If the map is empty, the key is not found, or the mapped value is null, returns an empty OptionalChar.
     * Non-Character values are converted to char using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("letter", 'A');
     * map.put("digit", "5");
     * map.put("empty", null);
     * 
     * OptionalChar result1 = Maps.getChar(map, "letter");  // OptionalChar.of('A')
     * OptionalChar result2 = Maps.getChar(map, "digit");   // OptionalChar.of('5')
     * OptionalChar result3 = Maps.getChar(map, "empty");   // OptionalChar.empty()
     * OptionalChar result4 = Maps.getChar(map, "missing"); // OptionalChar.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an OptionalChar containing the character value, or empty if not found
     */
    public static <K> OptionalChar getChar(final Map<? super K, ?> map, final K key) {
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
     * Returns the character value mapped to the specified key, or defaultForNull if not found.
     * If the map is empty, the key is not found, or the mapped value is null, returns defaultForNull.
     * Non-Character values are converted to char using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("grade", 'A');
     * map.put("initial", "J");
     * 
     * char result1 = Maps.getChar(map, "grade", 'F');    // 'A'
     * char result2 = Maps.getChar(map, "initial", 'X');  // 'J'
     * char result3 = Maps.getChar(map, "missing", 'N');  // 'N' (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the key is not found or value is null
     * @return the character value mapped to the key, or defaultForNull if not found
     */
    public static <K> char getChar(final Map<? super K, ?> map, final K key, final char defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Character) {
            return (Character) val;
        } else {
            return Strings.parseChar(N.toString(val));
        }
    }

    /**
     * Returns an OptionalByte containing the byte value mapped to the specified key.
     * If the map is empty, the key is not found, or the mapped value is null, returns an empty OptionalByte.
     * Non-Number values are converted to byte using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("small", (byte) 10);
     * map.put("medium", 127);
     * map.put("text", "25");
     * 
     * OptionalByte result1 = Maps.getByte(map, "small");   // OptionalByte.of(10)
     * OptionalByte result2 = Maps.getByte(map, "medium");  // OptionalByte.of(127)
     * OptionalByte result3 = Maps.getByte(map, "text");    // OptionalByte.of(25)
     * OptionalByte result4 = Maps.getByte(map, "missing"); // OptionalByte.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an OptionalByte containing the byte value, or empty if not found
     */
    public static <K> OptionalByte getByte(final Map<? super K, ?> map, final K key) {
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
     * Returns the byte value mapped to the specified key, or defaultForNull if not found.
     * If the map is empty, the key is not found, or the mapped value is null, returns defaultForNull.
     * Non-Number values are converted to byte using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("id", (byte) 5);
     * map.put("count", "10");
     * 
     * byte result1 = Maps.getByte(map, "id", (byte) 0);      // 5
     * byte result2 = Maps.getByte(map, "count", (byte) 0);   // 10
     * byte result3 = Maps.getByte(map, "missing", (byte) -1); // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the key is not found or value is null
     * @return the byte value mapped to the key, or defaultForNull if not found
     */
    public static <K> byte getByte(final Map<? super K, ?> map, final K key, final byte defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).byteValue();
        } else {
            return Numbers.toByte(N.toString(val));
        }
    }

    /**
     * Returns an OptionalShort containing the short value mapped to the specified key.
     * If the map is empty, the key is not found, or the mapped value is null, returns an empty OptionalShort.
     * Non-Number values are converted to short using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("year", (short) 2023);
     * map.put("count", 1000);
     * map.put("text", "500");
     * 
     * OptionalShort result1 = Maps.getShort(map, "year");    // OptionalShort.of(2023)
     * OptionalShort result2 = Maps.getShort(map, "count");   // OptionalShort.of(1000)
     * OptionalShort result3 = Maps.getShort(map, "text");    // OptionalShort.of(500)
     * OptionalShort result4 = Maps.getShort(map, "missing"); // OptionalShort.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an OptionalShort containing the short value, or empty if not found
     */
    public static <K> OptionalShort getShort(final Map<? super K, ?> map, final K key) {
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
     * Returns the short value mapped to the specified key, or defaultForNull if not found.
     * If the map is empty, the key is not found, or the mapped value is null, returns defaultForNull.
     * Non-Number values are converted to short using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("port", (short) 8080);
     * map.put("timeout", "3000");
     * 
     * short result1 = Maps.getShort(map, "port", (short) 80);      // 8080
     * short result2 = Maps.getShort(map, "timeout", (short) 0);    // 3000
     * short result3 = Maps.getShort(map, "missing", (short) -1);   // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the key is not found or value is null
     * @return the short value mapped to the key, or defaultForNull if not found
     */
    public static <K> short getShort(final Map<? super K, ?> map, final K key, final short defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else {
            return Numbers.toShort(N.toString(val));
        }
    }

    /**
     * Returns an OptionalInt containing the integer value mapped to the specified key.
     * If the map is empty, the key is not found, or the mapped value is null, returns an empty OptionalInt.
     * Non-Number values are converted to int using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42);
     * map.put("total", "100");
     * map.put("null", null);
     * 
     * OptionalInt result1 = Maps.getInt(map, "count");   // OptionalInt.of(42)
     * OptionalInt result2 = Maps.getInt(map, "total");   // OptionalInt.of(100)
     * OptionalInt result3 = Maps.getInt(map, "null");    // OptionalInt.empty()
     * OptionalInt result4 = Maps.getInt(map, "missing"); // OptionalInt.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an OptionalInt containing the integer value, or empty if not found
     */
    public static <K> OptionalInt getInt(final Map<? super K, ?> map, final K key) {
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
     * Returns the integer value mapped to the specified key, or defaultForNull if not found.
     * If the map is empty, the key is not found, or the mapped value is null, returns defaultForNull.
     * Non-Number values are converted to int using standard conversion rules.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("age", 25);
     * map.put("score", "98");
     * 
     * int result1 = Maps.getInt(map, "age", 0);      // 25
     * int result2 = Maps.getInt(map, "score", 0);    // 98
     * int result3 = Maps.getInt(map, "missing", -1); // -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the key is not found or value is null
     * @return the integer value mapped to the key, or defaultForNull if not found
     */
    public static <K> int getInt(final Map<? super K, ?> map, final K key, final int defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     * 
     * OptionalLong count = Maps.getLong(map, "count");
     * // count.isPresent() = true, count.getAsLong() = 42
     * 
     * OptionalLong score = Maps.getLong(map, "score");
     * // score.isPresent() = true, score.getAsLong() = 100 (converted from String)
     * 
     * OptionalLong missing = Maps.getLong(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @return An {@code OptionalLong} containing the value if present, otherwise empty
     */
    public static <K> OptionalLong getLong(final Map<? super K, ?> map, final K key) {
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Long/Number type, underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     * 
     * long count = Maps.getLong(map, "count", -1L);
     * // count = 42
     * 
     * long score = Maps.getLong(map, "score", -1L);
     * // score = 100 (converted from String)
     * 
     * long missing = Maps.getLong(map, "missing", -1L);
     * // missing = -1
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param defaultForNull The default value to return if the value is null or not found
     * @return The long value associated with the key, or defaultForNull if not found
     */
    public static <K> long getLong(final Map<? super K, ?> map, final K key, final long defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     * 
     * OptionalFloat price = Maps.getFloat(map, "price");
     * // price.isPresent() = true, price.getAsFloat() = 19.99
     * 
     * OptionalFloat discount = Maps.getFloat(map, "discount");
     * // discount.isPresent() = true, discount.getAsFloat() = 0.15 (converted from String)
     * 
     * OptionalFloat missing = Maps.getFloat(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @return An {@code OptionalFloat} containing the value if present, otherwise empty
     */
    public static <K> OptionalFloat getFloat(final Map<? super K, ?> map, final K key) {
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Float/Number type, underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     * 
     * float price = Maps.getFloat(map, "price", 0.0f);
     * // price = 19.99
     * 
     * float discount = Maps.getFloat(map, "discount", 0.0f);
     * // discount = 0.15 (converted from String)
     * 
     * float missing = Maps.getFloat(map, "missing", 0.0f);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param defaultForNull The default value to return if the value is null or not found
     * @return The float value associated with the key, or defaultForNull if not found
     */
    public static <K> float getFloat(final Map<? super K, ?> map, final K key, final float defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else {
            return Numbers.toFloat(val);
        }
    }

    /**
     * Returns an empty {@code OptionalDouble} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalDouble} with the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     * 
     * OptionalDouble temp = Maps.getDouble(map, "temperature");
     * // temp.isPresent() = true, temp.getAsDouble() = 98.6
     * 
     * OptionalDouble pi = Maps.getDouble(map, "pi");
     * // pi.isPresent() = true, pi.getAsDouble() = 3.14159 (converted from String)
     * 
     * OptionalDouble missing = Maps.getDouble(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @return An {@code OptionalDouble} containing the value if present, otherwise empty
     */
    public static <K> OptionalDouble getDouble(final Map<? super K, ?> map, final K key) {
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     * 
     * double temp = Maps.getDouble(map, "temperature", 0.0);
     * // temp = 98.6
     * 
     * double pi = Maps.getDouble(map, "pi", 0.0);
     * // pi = 3.14159 (converted from String)
     * 
     * double missing = Maps.getDouble(map, "missing", 0.0);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param defaultForNull The default value to return if the value is null or not found
     * @return The double value associated with the key, or defaultForNull if not found
     */
    public static <K> double getDouble(final Map<? super K, ?> map, final K key, final double defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else {
            return Numbers.toDouble(val);
        }
    }

    /**
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<String>} with the value mapped by the specified {@code key}.
     * If the mapped value is not String type, underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     * 
     * Optional<String> name = Maps.getString(map, "name");
     * // name.isPresent() = true, name.get() = "John"
     * 
     * Optional<String> age = Maps.getString(map, "age");
     * // age.isPresent() = true, age.get() = "25" (converted from Integer)
     * 
     * Optional<String> missing = Maps.getString(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @return An {@code Optional<String>} with the value mapped by the specified key, or an empty {@code Optional<String>} if the map is empty, contains no value for the key, or the value is null
     */
    public static <K> Optional<String> getString(final Map<? super K, ?> map, final K key) {
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
     * Returns the value to which the specified key is mapped if the value is not {@code null},
     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of String type, underlying conversion will be executed by {@code N.stringOf(value)}.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     * 
     * String name = Maps.getString(map, "name", "Unknown");
     * // name = "John"
     * 
     * String age = Maps.getString(map, "age", "Unknown");
     * // age = "25" (converted from Integer)
     * 
     * String missing = Maps.getString(map, "missing", "Unknown");
     * // missing = "Unknown"
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param defaultForNull The default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null
     * @return The value mapped by the specified key, or {@code defaultForNull} if the map is empty, contains no value for the key, or the value is null
     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
     */
    public static <K> String getString(final Map<? super K, ?> map, final K key, final String defaultForNull) throws IllegalArgumentException {
        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("date", "2023-12-25");
     * map.put("count", "100");
     * 
     * Optional<LocalDate> date = Maps.getNonNull(map, "date", LocalDate.class);
     * // date.isPresent() = true, date.get() = LocalDate.of(2023, 12, 25)
     * 
     * Optional<Integer> count = Maps.getNonNull(map, "count", Integer.class);
     * // count.isPresent() = true, count.get() = 100
     * 
     * Optional<BigDecimal> missing = Maps.getNonNull(map, "missing", BigDecimal.class);
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param <T> The type of the value
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param targetType The target type to which the value should be converted
     * @return An {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getNonNull(final Map<? super K, ?> map, final K key, final Class<? extends T> targetType) {
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("items", Arrays.asList("A", "B", "C"));
     * 
     * Optional<List<String>> items = Maps.getNonNull(map, "items", new TypeReference<List<String>>() {});
     * // items.isPresent() = true, items.get() = ["A", "B", "C"]
     * 
     * Optional<Set<Integer>> missing = Maps.getNonNull(map, "missing", new TypeReference<Set<Integer>>() {});
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param <T> The type of the value
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param targetType The target type to which the value should be converted
     * @return An {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getNonNull(final Map<? super K, ?> map, final K key, final Type<? extends T> targetType) {
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
     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of type {@code T}, an underlying conversion will be executed.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", "100");
     * map.put("active", "true");
     * 
     * Integer count = Maps.getNonNull(map, "count", 0);
     * // count = 100 (converted from String)
     * 
     * Boolean active = Maps.getNonNull(map, "active", false);
     * // active = true (converted from String)
     * 
     * Double missing = Maps.getNonNull(map, "missing", 0.0);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> The type of keys maintained by the map
     * @param <T> The type of the value
     * @param map The map from which to retrieve the value
     * @param key The key whose associated value is to be returned
     * @param defaultForNull The default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null
     * @return The value to which the specified key is mapped, or {@code defaultForNull} if the map is empty, contains no value for the key, or the value is null
     * @throws IllegalArgumentException if {@code defaultForNull} is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> T getNonNull(final Map<? super K, ?> map, final K key, final T defaultForNull) throws IllegalArgumentException {
        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (defaultForNull.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultForNull.getClass());
        }
    }

    //    /**
    //     * Returns an empty {@code Optional<V>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
    //     * Otherwise, returns an {@code Optional<V>} with the value mapped by the specified {@code key}.
    //     *
    //     * <br />
    //     * Present -> key is found in the specified map with {@code non-null} value.
    //     *
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated inconsistent with {@link #getNonNull(Map, Object, Object)}
    //     */
    //    @Deprecated
    //    public static <K, V> u.Optional<V> getNonNull(final Map<K, ? extends V> map, final K key) {
    //        if (N.isEmpty(map)) {
    //            return u.Optional.empty();
    //        }
    //
    //        final V val = map.get(key);
    //
    //        if (val == null) {
    //            return u.Optional.empty();
    //        } else {
    //            return u.Optional.of(val);
    //        }
    //    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new value got from {@code defaultValueSupplier} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @param defaultValueSupplier
    //     * @return
    //     * @deprecated Use {@link #getAndPutIfAbsent(Map, Object, Supplier)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getAndPutIfNull(final Map<K, V> map, K key, Supplier<? extends V> defaultValueSupplier) {
    //        V val = map.get(key);
    //
    //        if (val == null) {
    //            val = defaultValueSupplier.get(); // Objects.requireNonNull(defaultValueSupplier.get());
    //            val = map.put(key, val);
    //        }
    //
    //        return val;
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new value obtained from {@code defaultValueSupplier} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * 
     * List<String> list1 = Maps.getAndPutIfAbsent(map, "key1", () -> new ArrayList<>());
     * list1.add("value1");
     * // map now contains: {"key1"=["value1"]}
     * 
     * List<String> list2 = Maps.getAndPutIfAbsent(map, "key1", () -> new ArrayList<>());
     * // list2 is the same instance as list1, supplier not called
     * 
     * map.put("key2", null);
     * List<String> list3 = Maps.getAndPutIfAbsent(map, "key2", () -> new ArrayList<>());
     * // New list created because value was null
     * }</pre>
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @param defaultValueSupplier The supplier to provide a default value if the key is absent, which may be null
     * @return The value associated with the specified key, or a new value from {@code defaultValueSupplier} if the key is absent
     */
    public static <K, V> V getAndPutIfAbsent(final Map<K, V> map, final K key, final Supplier<? extends V> defaultValueSupplier) {
        V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            val = defaultValueSupplier.get(); // Objects.requireNonNull(defaultValueSupplier.get());
            map.put(key, val);
        }

        return val;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code List} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutListIfAbsent(Map<K, List<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> List<E> getAndPutListIfNull(final Map<K, List<E>> map, final K key) {
    //        return getAndPutListIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code List} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * 
     * List<Integer> list1 = Maps.getAndPutListIfAbsent(map, "numbers");
     * list1.add(1);
     * list1.add(2);
     * // map now contains: {"numbers"=[1, 2]}
     * 
     * List<Integer> list2 = Maps.getAndPutListIfAbsent(map, "numbers");
     * // list2 is the same instance as list1
     * // list2 contains [1, 2]
     * }</pre>
     *
     * @param <K> The key type
     * @param <E> The element type of the list
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @return The value associated with the specified key, or a new {@code List} if the key is absent
     */
    public static <K, E> List<E> getAndPutListIfAbsent(final Map<K, List<E>> map, final K key) {
        List<E> v = map.get(key);

        if (v == null) {
            v = new ArrayList<>();
            map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code Set} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutSetIfAbsent(Map<K, Set<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> Set<E> getAndPutSetIfNull(final Map<K, Set<E>> map, final K key) {
    //        return getAndPutSetIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Set} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     * 
     * Set<String> set1 = Maps.getAndPutSetIfAbsent(map, "tags");
     * set1.add("java");
     * set1.add("spring");
     * // map now contains: {"tags"=["java", "spring"]}
     * 
     * Set<String> set2 = Maps.getAndPutSetIfAbsent(map, "tags");
     * // set2 is the same instance as set1
     * }</pre>
     *
     * @param <K> The key type
     * @param <E> The element type of the set
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @return The value associated with the specified key, or a new {@code Set} if the key is absent
     */
    public static <K, E> Set<E> getAndPutSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new HashSet<>();
            map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code LinkedHashSet} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutLinkedHashSetIfAbsent(Map<K, Set<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> Set<E> getAndPutLinkedHashSetIfNull(final Map<K, Set<E>> map, final K key) {
    //        return getAndPutLinkedHashSetIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashSet} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     * 
     * Set<String> set = Maps.getAndPutLinkedHashSetIfAbsent(map, "orderedTags");
     * set.add("first");
     * set.add("second");
     * set.add("third");
     * // map now contains: {"orderedTags"=["first", "second", "third"]} (order preserved)
     * }</pre>
     *
     * @param <K> The key type
     * @param <E> The element type of the set
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @return The value associated with the specified key, or a new {@code LinkedHashSet} if the key is absent
     */
    public static <K, E> Set<E> getAndPutLinkedHashSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new LinkedHashSet<>();
            map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code Map} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <KK>
    //     * @param <VV>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutMapIfAbsent(Map<K, Map<KK, VV>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV> Map<KK, VV> getAndPutMapIfNull(final Map<K, Map<KK, VV>> map, final K key) {
    //        return getAndPutMapIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Map} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     * 
     * Map<String, Integer> innerMap = Maps.getAndPutMapIfAbsent(map, "scores");
     * innerMap.put("math", 95);
     * innerMap.put("english", 88);
     * // map now contains: {"scores"={"math"=95, "english"=88}}
     * 
     * Map<String, Integer> sameMap = Maps.getAndPutMapIfAbsent(map, "scores");
     * // sameMap is the same instance as innerMap
     * }</pre>
     *
     * @param <K> The key type
     * @param <KK> The key type of the value map
     * @param <VV> The value type of the value map
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @return The value associated with the specified key, or a new {@code Map} if the key is absent
     */
    public static <K, KK, VV> Map<KK, VV> getAndPutMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new HashMap<>();
            map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code LinkedHashMap} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <KK>
    //     * @param <VV>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutLinkedHashMapIfAbsent(Map<K, Map<KK, VV>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV> Map<KK, VV> getAndPutLinkedHashMapIfNull(final Map<K, Map<KK, VV>> map, final K key) {
    //        return getAndPutLinkedHashMapIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashMap} and returns it.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Map<String, String>> map = new HashMap<>();
     * 
     * Map<String, String> innerMap = Maps.getAndPutLinkedHashMapIfAbsent(map, "config");
     * innerMap.put("first", "1");
     * innerMap.put("second", "2");
     * innerMap.put("third", "3");
     * // map now contains: {"config"={"first"="1", "second"="2", "third"="3"}} (order preserved)
     * }</pre>
     *
     * @param <K> The key type
     * @param <KK> The key type of the value map
     * @param <VV> The value type of the value map
     * @param map The map to check and possibly update
     * @param key The key to check for, which may be null
     * @return The value associated with the specified key, or a new {@code LinkedHashMap} if the key is absent
     */
    public static <K, KK, VV> Map<KK, VV> getAndPutLinkedHashMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new LinkedHashMap<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns a list of values of the keys which exist in the specified {@code Map}.
     * If the key doesn't exist in the {@code Map} or associated value is {@code null}, no value will be added into the returned list.
     * 
     * <p>Present means key is found in the specified map with {@code non-null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     * 
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getIfPresentForEach(map, keys);
     * // values = [1, 2] (null value for "c" and missing "d" are not included)
     * }</pre>
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param map The map to check for keys
     * @param keys The collection of keys to check in the map
     * @return A list of values corresponding to the keys found in the map
     */
    public static <K, V> List<V> getIfPresentForEach(final Map<K, ? extends V> map, final Collection<?> keys) throws IllegalArgumentException {
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

    //    /**
    //     *
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param keys
    //     * @param defaultForNull
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsentForEach(Map, Collection, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> List<V> getOrDefaultIfNullForEach(final Map<K, V> map, final Collection<?> keys, final V defaultForNull) {
    //        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR
    //
    //        if (N.isEmpty(keys)) {
    //            return new ArrayList<>();
    //        } else if (N.isEmpty(map)) {
    //            return N.repeat(defaultForNull, keys.size());
    //        }
    //
    //        final List<V> result = new ArrayList<>(keys.size());
    //        V val = null;
    //
    //        for (Object key : keys) {
    //            val = map.get(key);
    //
    //            if (val == null) {
    //                result.add(defaultForNull);
    //            } else {
    //                result.add(val);
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     * Returns a list of values mapped by the keys which exist in the specified {@code Map}, or default value if the key doesn't exist in the {@code Map} or associated value is {@code null}.
     * 
     * <p>Absent means key is not found in the specified map or found with {@code null} value.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     * 
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getOrDefaultIfAbsentForEach(map, keys, -1);
     * // values = [1, 2, -1, -1] ("c" has null value, "d" is missing)
     * }</pre>
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param map The map to check for keys
     * @param keys The collection of keys to check in the map
     * @param defaultValue The default value to use when key is absent
     * @return A list of values corresponding to the keys, using defaultValue when absent
     * @throws IllegalArgumentException if keys is null
     */
    public static <K, V> List<V> getOrDefaultIfAbsentForEach(final Map<K, V> map, final Collection<?> keys, final V defaultValue)
            throws IllegalArgumentException {
        // N.checkArgNotNull(defaultValue, "defaultValue"); // NOSONAR

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
     * Retrieves a value from a nested map structure using a dot-separated path. For example:
     * <pre>
     * <code>
     Map map = N.asMap("key1", "val1");
     assertEquals("val1", Maps.getByPath(map, "key1"));
    
     map = N.asMap("key1", N.asList("val1"));
     assertEquals("val1", Maps.getByPath(map, "key1[0]"));
    
     map = N.asMap("key1", N.asSet("val1"));
     assertEquals("val1", Maps.getByPath(map, "key1[0]"));
    
     map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", "val2")));
     assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));
    
     map = N.asMap("key1", N.asSet(N.asList(N.asSet("val1"))));
     assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));
    
     map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", "val22"))));
     assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));
    
     map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
     assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));
    
     map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
     assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));
     * </code>
     * </pre>
     *
     * @param <T> The type of the value to be returned
     * @param map The map to retrieve the value from
     * @param path The dot-separated path with optional array indices
     * @return {@code null} if there is no value found by the specified path.
     */
    @MayReturnNull
    public static <T> T getByPath(final Map<String, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return null;
        }

        return (T) val;
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path.
     * The value is converted to the specified target type if necessary.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25"));
     * 
     * Integer age = Maps.getByPath(map, "user.age", Integer.class);
     * // age = 25 (converted from String to Integer)
     * 
     * LocalDate missing = Maps.getByPath(map, "user.birthdate", LocalDate.class);
     * // missing = null
     * }</pre>
     *
     * @param <T> The type of the value to be returned
     * @param map The map to retrieve the value from
     * @param path The dot-separated path with optional array indices
     * @param targetType The target type to convert the value to
     * @return {@code null} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    @MayReturnNull
    public static <T> T getByPath(final Map<String, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, NONE);

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
     * Retrieves a value from a nested map structure using a dot-separated path.
     * If the path does not exist in the map, the provided default value is returned.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("config", N.asMap("timeout", 30));
     * 
     * Integer timeout = Maps.getByPath(map, "config.timeout", 60);
     * // timeout = 30
     * 
     * Integer retries = Maps.getByPath(map, "config.retries", 3);
     * // retries = 3 (default value)
     * }</pre>
     *
     * @param <T> The type of the value to be returned
     * @param map The map to retrieve the value from
     * @param path The dot-separated path with optional array indices
     * @param defaultValue The default value to return if path not found
     * @return {@code defaultValue} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> T getByPath(final Map<String, ?> map, final String path, final T defaultValue) {
        // N.checkArgNotNull(defaultValue, "defaultValue");

        final Object val = getByPathOrDefault(map, path, defaultValue);

        if (val == null || defaultValue.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultValue.getClass());
        }
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path. If the value exists, it is returned wrapped in a {@code Nullable} object.
     * 
     * <p>Example:
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
     * @param <T> The type of the value to be returned
     * @param map The map to retrieve the value from
     * @param path The dot-separated path with optional array indices
     * @return An empty {@code Nullable} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<String, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return Nullable.empty();
        }

        return Nullable.of((T) val);
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path. If the value exists, it is returned wrapped in a {@code Nullable} object.
     * The value is converted to the specified target type if necessary.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("settings", N.asMap("maxConnections", "100"));
     * 
     * Nullable<Integer> maxConn = Maps.getByPathIfExists(map, "settings.maxConnections", Integer.class);
     * // maxConn.isPresent() = true, maxConn.get() = 100 (converted from String)
     * 
     * Nullable<Boolean> debug = Maps.getByPathIfExists(map, "settings.debug", Boolean.class);
     * // debug.isPresent() = false
     * }</pre>
     *
     * @param <T> The type of the value to be returned
     * @param map The map to retrieve the value from
     * @param path The dot-separated path with optional array indices
     * @param targetType The target type to convert the value to
     * @return An empty {@code Nullable} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<String, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, NONE);

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
    private static Object getByPathOrDefault(final Map<String, ?> map, final String path, final Object defaultValue) {
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
     * Checks if the specified map contains the specified entry.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 1);
     * Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("a", 2);
     * 
     * boolean contains1 = Maps.contains(map, entry1);
     * // contains1 = true
     * 
     * boolean contains2 = Maps.contains(map, entry2);
     * // contains2 = false (value doesn't match)
     * }</pre>
     *
     * @param map The map to check, which may be null
     * @param entry The entry to check for, which may be null
     * @return {@code true} if the map contains the specified entry, {@code false} otherwise
     */
    public static boolean contains(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        return contains(map, entry.getKey(), entry.getValue());
    }

    /**
     * Checks if the specified map contains the specified key-value pair.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", null);
     * 
     * boolean contains1 = Maps.contains(map, "a", 1);
     * // contains1 = true
     * 
     * boolean contains2 = Maps.contains(map, "a", 2);
     * // contains2 = false
     * 
     * boolean contains3 = Maps.contains(map, "b", null);
     * // contains3 = true
     * 
     * boolean contains4 = Maps.contains(map, "c", null);
     * // contains4 = false (key not present)
     * }</pre>
     *
     * @param map The map to be checked
     * @param key The key whose presence in the map is to be tested
     * @param value The value whose presence in the map is to be tested
     * @return {@code true} if the map contains the specified key-value pair, {@code false} otherwise
     */
    public static boolean contains(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object val = map.get(key);

        return val == null ? value == null && map.containsKey(key) : N.equals(val, value);
    }

    /**
     * Returns a new map containing entries that are present in both input maps.
     * The intersection contains entries whose keys are present in both maps with equal values.
     * The returned map's key-value pairs are taken from the first input map.
     *
     * <p>Example:
     * <pre>
     * Map&lt;String, Integer&gt; map1 = new HashMap&lt;&gt;();
     * map1.put("a", 1);
     * map1.put("b", 2);
     * map1.put("c", 3);
     *
     * Map&lt;String, Integer&gt; map2 = new HashMap&lt;&gt;();
     * map2.put("b", 2);
     * map2.put("c", 4);
     * map2.put("d", 5);
     *
     * Map&lt;String, Integer&gt; result = Maps.intersection(map1, map2); // result will be {"b": 2}
     * // Only "b" is included because it has the same value in both maps
     *
     * Map&lt;String, String&gt; map3 = new HashMap&lt;&gt;();
     * map3.put("x", "foo");
     * map3.put("y", "bar");
     *
     * Map&lt;String, String&gt; map4 = new HashMap&lt;&gt;();
     * map4.put("x", "foo");
     * map4.put("z", "baz");
     *
     * Map&lt;String, String&gt; result2 = Maps.intersection(map3, map4); // result will be {"x": "foo"}
     * // Only "x" is included because it has the same value in both maps
     * </pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the first input map
     * @param map2 the second input map to find common entries with
     * @return a new map containing entries present in both maps with equal values
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

            if ((val != null && N.equals(val, entry.getValue())) || (val == null && entry.getValue() == null && map.containsKey(entry.getKey()))) {
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
     * <p>Example:
     * <pre>
     * Map&lt;String, Integer&gt; map1 = Maps.of("a", 1, "b", 2, "c", 3);
     * Map&lt;String, Integer&gt; map2 = Maps.of("a", 1, "b", 20, "d", 4);
     * 
     * Map&lt;String, Pair&lt;Integer, Nullable&lt;Integer&gt;&gt;&gt; diff = Maps.difference(map1, map2);
     * // diff contains:
     * // "b" -> Pair.of(2, Nullable.of(20))    // different values
     * // "c" -> Pair.of(3, Nullable.empty())   // key only in map1
     * </pre>
     *
     * <p>Note that this method only returns keys from the first map. Keys that exist only in the second map 
     * are not included in the result. If you need to identify keys that are unique to each map, 
     * use {@link #symmetricDifference(Map, Map)} instead.
     *
     * <p>If the first map is {@code null}, an empty map is returned. If the second map is {@code null},
     * all values from the first map will be paired with empty {@code Nullable} objects.
     *
     * @param <K> The type of keys in the maps
     * @param <V> The type of values in the maps
     * @param map The first map to compare
     * @param map2 The second map to compare
     * @return A map representing the difference between the two input maps
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
     * <li>If the key exists only in the second map, the pair contains an empty Nullable and the value from the second map</li>
     * <li>If the key exists in both maps with different values, the pair contains both values</li>
     * </ul>
     *
     * <p>Example:
     * <pre>
     * Map&lt;String, Integer&gt; map1 = Maps.of("a", 1, "b", 2, "c", 3);
     * Map&lt;String, Integer&gt; map2 = Maps.of("b", 2, "c", 4, "d", 5);
     * 
     * Map&lt;String, Pair&lt;Nullable&lt;Integer&gt;, Nullable&lt;Integer&gt;&gt;&gt; result = Maps.symmetricDifference(map1, map2);
     * // result contains:
     * // "a" -> Pair.of(Nullable.of(1), Nullable.empty())   // key only in map1
     * // "c" -> Pair.of(Nullable.of(3), Nullable.of(4))     // different values
     * // "d" -> Pair.of(Nullable.empty(), Nullable.of(5))   // key only in map2
     * // Note: "b" is not included because it has identical values in both maps
     * </pre>
     *
     * <p>If either input map is null, it is treated as an empty map.
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param map the first input map
     * @param map2 the second input map
     * @return a new map containing the symmetric difference between the two input maps
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

    //    /**
    //     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @param value
    //     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
    //     * @see Map#putIfAbsent(Object, Object)
    //     * @deprecated Use {@link #putIfAbsent(Map<K, V>,K,V)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V putIfAbsentOrNull(final Map<K, V> map, K key, final V value) {
    //        return putIfAbsent(map, key, value);
    //    }
    //
    //    /**
    //     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @param supplier
    //     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
    //     * @see Map#putIfAbsent(Object, Object)
    //     * @deprecated Use {@link #putIfAbsent(Map<K, V>,K,Supplier<V>)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V putIfAbsentOrNull(final Map<K, V> map, K key, final Supplier<V> supplier) {
    //        return putIfAbsent(map, key, supplier);
    //    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param value
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
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
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param supplier
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     * 
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("abc", 4);
     * 
     * boolean changed = Maps.putIf(target, source, key -> key.length() > 1);
     * // changed: true
     * // target: {a=1, abc=4}
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the target map to which entries will be added
     * @param sourceMap the source map from which entries will be taken
     * @param filter a predicate that filters keys to be added to the target map
     * @return {@code true} if any entries were added, {@code false} otherwise
     */
    @Beta
    public static <K, V> boolean putIf(final Map<K, V> map, final Map<? extends K, ? extends V> sourceMap, Predicate<? super K> filter) {
        if (N.isEmpty(sourceMap)) {
            return false;
        }

        boolean changed = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue());
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
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     * 
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("d", 10);
     * 
     * boolean changed = Maps.putIf(target, source, (key, value) -> value > 2);
     * // changed: true
     * // target: {a=1, c=3, d=10}
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the target map to which entries will be added
     * @param sourceMap the source map from which entries will be taken
     * @param filter a predicate that filters keys and values to be added to the target map
     * @return {@code true} if any entries were added, {@code false} otherwise
     */
    @Beta
    public static <K, V> boolean putIf(final Map<K, V> map, final Map<? extends K, ? extends V> sourceMap, BiPredicate<? super K, ? super V> filter) {
        if (N.isEmpty(sourceMap)) {
            return false;
        }

        boolean changed = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                map.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Removes the specified entry from the map.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which the entry is to be removed
     * @param entry the entry to be removed from the map
     * @return {@code true} if the entry was removed, {@code false} otherwise
     * @see Map#remove(Object, Object)
     */
    public static <K, V> boolean remove(final Map<K, V> map, final Map.Entry<?, ?> entry) {
        return remove(map, entry.getKey(), entry.getValue());
    }

    /**
     * Removes the specified key-value pair from the map.
     * This method removes an entry from the map only if the key is mapped to the specified value.
     * If the key is not present in the map or is mapped to a different value, the map remains unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * boolean removed1 = Maps.remove(map, "a", 1);  // true, entry removed
     * boolean removed2 = Maps.remove(map, "b", 3);  // false, value doesn't match
     * // map: {b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which the entry is to be removed
     * @param key the key whose associated value is to be removed
     * @param value the value to be removed
     * @return {@code true} if the entry was removed, {@code false} otherwise
     * @see Map#remove(Object, Object)
     */
    public static <K, V> boolean remove(final Map<K, V> map, final Object key, final Object value) {
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
     * Removes the specified keys from the map.
     * This method removes all entries from the map whose keys are contained in the provided collection.
     * If any of the keys in the collection are not present in the map, they are ignored.
     *
     * <p>Example:
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
     * @param map the map from which the keys are to be removed
     * @param keysToRemove the collection of keys to be removed from the map
     * @return {@code true} if any keys were removed, {@code false} otherwise
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
     * Removes the specified entries from the map.
     * This method removes all entries from the map that have matching key-value pairs in the entriesToRemove map.
     * An entry is removed only if both the key and value match exactly.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * Map<String, Integer> entriesToRemove = new HashMap<>();
     * entriesToRemove.put("a", 1);
     * entriesToRemove.put("b", 5);  // Different value
     * 
     * boolean changed = Maps.removeEntries(map, entriesToRemove);
     * // changed: true
     * // map: {b=2, c=3}  // Only "a"=1 was removed
     * }</pre>
     *
     * @param map the map from which the entries are to be removed
     * @param entriesToRemove the map containing the entries to be removed
     * @return {@code true} if any entries were removed, {@code false} otherwise
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
     * Removes entries from the specified map that match the given filter predicate.
     * This method iterates through all entries in the map and removes those that satisfy the filter condition.
     * The map is modified in place.
     *
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which entries to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which entries to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which keys to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which values to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
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
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * This method updates the value for a key only if the current value matches the oldValue parameter.
     * If the key is not present or the current value doesn't match oldValue, the map remains unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * boolean replaced1 = Maps.replace(map, "a", 1, 10);  // true
     * boolean replaced2 = Maps.replace(map, "b", 3, 20);  // false, old value doesn't match
     * // map: {a=10, b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entry is to be replaced
     * @param key the key with which the specified value is associated
     * @param oldValue the expected current value associated with the specified key
     * @param newValue the new value to be associated with the specified key
     * @return {@code true} if the value was replaced, {@code false} otherwise
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
     * Replaces the entry for the specified key with the new value if the key is present in the map.
     * This method updates the value for a key only if the key exists in the map.
     * If the key is not present, the map remains unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * 
     * Integer oldValue1 = Maps.replace(map, "a", 10);  // returns 1
     * Integer oldValue2 = Maps.replace(map, "c", 30);  // returns null
     * // map: {a=10, b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entry is to be replaced
     * @param key the key with which the specified value is associated
     * @param newValue the new value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
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
     * Replaces each entry's value with the result of applying the given function to that entry.
     * This method applies the provided function to each key-value pair in the map and updates the value with the function's result.
     * The function receives both the key and the current value as parameters.
     *
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entries are to be replaced
     * @param function the function to apply to each entry to compute a new value
     * @throws IllegalArgumentException if the function is null
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

    //    public static <K, V> void forEach(final Map<K, V> map, final Consumer<? super Map.Entry<K, V>, E> action)  {
    //        N.checkArgNotNull(action);
    //
    //        if (N.isEmpty(map)) {
    //            return;
    //        }
    //
    //        for (Map.Entry<K, V> entry : map.entrySet()) {
    //            action.accept(entry);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param action
    //     */
    //    public static <K, V> void forEach(final Map<K, V> map, final BiConsumer<? super K, ? super V, E> action)  {
    //        N.checkArgNotNull(action);
    //
    //        if (N.isEmpty(map)) {
    //            return;
    //        }
    //
    //        for (Map.Entry<K, V> entry : map.entrySet()) {
    //            action.accept(entry.getKey(), entry.getValue());
    //        }
    //    }

    /**
     * Filters the entries of the specified map based on the given predicate.
     * This method creates a new map containing only the entries that satisfy the predicate condition.
     * The predicate is tested against each Map.Entry in the original map.
     * The returned map is of the same type as the input map if possible.
     *
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the entries
     * @return a new map containing only the entries that match the predicate
     * @throws IllegalArgumentException if the predicate is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the entries
     * @return a new map containing only the entries that match the predicate
     * @throws IllegalArgumentException if the predicate is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the keys
     * @return a new map containing only the entries with keys that match the predicate
     * @throws IllegalArgumentException if the predicate is null
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
     * <p>Example:
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
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the values
     * @return a new map containing only the entries with values that match the predicate
     * @throws IllegalArgumentException if the predicate is null
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
     * @param <K> The key type of the input map and the value type of the resulting map.
     * @param <V> The value type of the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @return A new map which is the inverted version of the input map.
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
     * @param <K> The key type of the input map and the value type of the resulting map.
     * @param <V> The value type of the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @param mergeOp The merging operation to be applied if there are duplicate values in the input map.
     * @return A new map which is the inverted version of the input map.
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
     * @param <K> The key type of the input map and the element type of the List values in the resulting map.
     * @param <V> The element type of the Collection values in the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @return A new map which is the inverted version of the input map.
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
     * @implSpec {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}].
     *
     * @param <K> The type of keys in the input map and the resulting maps.
     * @param <V> The type of values in the collections of the input map and the values in the resulting maps.
     * @param map The input map, where each key is associated with a collection of values.
     * @return A list of maps, where each map represents a "flat" version of the original map's entries.
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
     * @param map The map to be flattened.
     * @return A new map which is the flattened version of the input map.
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
     * @param <M> The type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map The map to be flattened.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the flattened version of the input map.
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
     * @param <M> The type of the map to be returned. It extends The Map with String keys and Object values.
     * @param map The map to be flattened.
     * @param delimiter The delimiter to be used when concatenating keys.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the flattened version of the input map.
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final String delimiter, final Supplier<? extends M> mapSupplier) {
        final M result = mapSupplier.get();

        flatten(map, null, delimiter, result);

        return result;
    }

    /**
     *
     * @param map
     * @param prefix
     * @param delimiter
     * @param output
     */
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
     * @param map The flattened map to be unflattened.
     * @return A new map which is the unflattened version of the input map.
     */
    public static Map<String, Object> unflatten(final Map<String, Object> map) {
        return unflatten(map, Suppliers.ofMap());
    }

    /**
     * Unflattens the given map using a provided map supplier.
     * This method takes a flattened map where keys are concatenated with a delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends The Map with String keys and Object values.
     * @param map The flattened map to be unflattened.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the unflattened version of the input map.
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final Supplier<? extends M> mapSupplier) {
        return unflatten(map, ".", mapSupplier);
    }

    /**
     * Unflattens the given map using a provided map supplier and a delimiter.
     * This method takes a flattened map where keys are concatenated with a specified delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends The Map with String keys and Object values.
     * @param map The flattened map to be unflattened.
     * @param delimiter The delimiter that was used in the flattening process to concatenate keys.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the unflattened version of the input map.
     * @throws IllegalArgumentException if the delimiter is not found in the map's keys.
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
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, 
     * and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * Unmatched properties from the specified map are ignored by default.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with a User bean class
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     * userMap.put("email", "john@example.com");
     * 
     * User user = Maps.map2Bean(userMap, User.class);
     * // user.getName() returns "John"
     * // user.getAge() returns 25
     * // user.getEmail() returns "john@example.com"
     * }</pre>
     *
     * @param <T> The type of the bean object to be returned.
     * @param m The map to be converted into a bean object.
     * @param targetType The type of the bean object to be returned.
     * @return A bean object of the specified type with its properties set to the values from the map.
     * @see #map2Bean(Map, boolean, boolean, Class)
     * @see #map2Bean(Map, Collection, Class)
     */
    public static <T> T map2Bean(final Map<String, Object> m, final Class<? extends T> targetType) {
        return map2Bean(m, false, true, targetType);
    }

    /**
     * Converts a map into a bean object of the specified type with control over null and unmatched properties.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, 
     * and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * You can control whether null properties should be ignored and whether unmatched properties should cause an error.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with ignoring null properties
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", null);
     * userMap.put("unknownField", "value"); // This field doesn't exist in User class
     * 
     * // Ignore null properties and unmatched properties
     * User user = Maps.map2Bean(userMap, true, true, User.class);
     * // user.getName() returns "John"
     * // user.getAge() remains unchanged (not set to null)
     * // unknownField is ignored
     * 
     * // Don't ignore null, but throw exception for unmatched properties
     * User user2 = Maps.map2Bean(userMap, false, false, User.class);
     * // This would throw an exception due to "unknownField"
     * }</pre>
     *
     * @param <T> The type of the bean object to be returned.
     * @param m The map to be converted into a bean object.
     * @param ignoreNullProperty If {@code true}, null values in the map will not be set on the bean.
     * @param ignoreUnmatchedProperty If {@code true}, map entries with keys that don't match any bean property will be ignored; if {@code false}, an exception will be thrown.
     * @param targetType The type of the bean object to be returned.
     * @return A bean object of the specified type with its properties set to the values from the map, or null if the input map is null.
     * @see #map2Bean(Map, Collection, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T map2Bean(final Map<String, Object> m, final boolean ignoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;

        String propName = null;
        Object propValue = null;

        for (final Map.Entry<String, Object> entry : m.entrySet()) {
            propName = entry.getKey();
            propValue = entry.getValue();

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, ignoreUnmatchedProperty);
            } else {
                if (propValue != null && propInfo.type.isBean() && N.typeOf(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, ignoreNullProperty, ignoreUnmatchedProperty, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Converts a map into a bean object of the specified type, including only selected properties.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, 
     * and transforms it into a bean object of the specified type.
     * Only the properties specified in selectPropNames will be set on the bean.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     * userMap.put("email", "john@example.com");
     * userMap.put("password", "secret");
     * 
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * User user = Maps.map2Bean(userMap, selectedProps, User.class);
     * // user.getName() returns "John"
     * // user.getEmail() returns "john@example.com"
     * // user.getAge() and user.getPassword() remain unset
     * }</pre>
     *
     * @param <T> The type of the bean object to be returned.
     * @param m The map to be converted into a bean object.
     * @param selectPropNames A collection of property names to be included in the resulting bean objects.
     * @param targetType The type of the bean object to be returned.
     * @return A bean object of the specified type with its properties set to the values from the map, or null if the input map is null.
     */
    @MayReturnNull
    public static <T> T map2Bean(final Map<String, Object> m, final Collection<String> selectPropNames, final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
            propValue = m.get(propName);

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, false);
            } else {
                if (propValue != null && propInfo.type.isBean() && N.typeOf(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type.
     * Each map in the collection represents a bean object where the map's keys are the property names 
     * and the values are the corresponding property values.
     * Unmatched properties from the maps are ignored by default.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with a list of user maps
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * 
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", 25);
     * userMaps.add(user1);
     * 
     * Map<String, Object> user2 = new HashMap<>();
     * user2.put("name", "Jane");
     * user2.put("age", 30);
     * userMaps.add(user2);
     * 
     * List<User> users = Maps.map2Bean(userMaps, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(1).getName() returns "Jane"
     * }</pre>
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     * @see #map2Bean(Collection, Collection, Class)
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final Class<? extends T> targetType) {
        return map2Bean(mList, false, true, targetType);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type with control over null and unmatched properties.
     * Each map in the collection represents a bean object where the map's keys are the property names 
     * and the values are the corresponding property values.
     * The resulting list contains bean objects of the specified type with their properties set to the values from the corresponding map.
     * The ignoreNullProperty parameter allows the user to specify whether {@code null} properties should be ignored.
     * The ignoreUnmatchedProperty parameter allows the user to specify whether unmatched properties should be ignored.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with null handling
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * 
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", null);
     * user1.put("unknownField", "value");
     * userMaps.add(user1);
     * 
     * // Ignore null values and unmatched properties
     * List<User> users = Maps.map2Bean(userMaps, true, true, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(0).getAge() remains unchanged (not set to null)
     * // unknownField is ignored
     * }</pre>
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param ignoreNullProperty A boolean that determines whether {@code null} properties should be ignored.
     * @param ignoreUnmatchedProperty A boolean that determines whether unmatched properties should be ignored.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final boolean ignoreNullProperty,
            final boolean ignoreUnmatchedProperty, final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, ignoreNullProperty, ignoreUnmatchedProperty, targetType));
        }

        return beanList;
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type, including only selected properties.
     * This method takes a collection of maps where each map represents a bean object. 
     * The keys in the map are the property names and the values are the corresponding property values.
     * Only the properties specified in selectPropNames will be set on the beans.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * 
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", 25);
     * user1.put("email", "john@example.com");
     * user1.put("password", "secret1");
     * userMaps.add(user1);
     * 
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * List<User> users = Maps.map2Bean(userMaps, selectedProps, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(0).getEmail() returns "john@example.com"
     * // age and password remain unset
     * }</pre>
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param selectPropNames A collection of property names to be included in the resulting bean objects. If this is empty, all properties are included.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final Collection<String> selectPropNames,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, selectPropNames, targetType));
        }

        return beanList;
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with a User bean
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     * 
     * Map<String, Object> userMap = Maps.bean2Map(user);
     * // userMap: {name=John, age=25, email=john@example.com}
     * }</pre>
     *
     * @param bean The bean object to be converted into a map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean) {
        return bean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map using the provided map supplier.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The map supplier function determines the type of the map to be returned.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with a custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * 
     * // Using TreeMap to get sorted properties
     * TreeMap<String, Object> sortedMap = Maps.bean2Map(user, TreeMap::new);
     * // sortedMap: {age=25, name=John} (sorted by key)
     * 
     * // Using HashMap for better performance
     * HashMap<String, Object> hashMap = Maps.bean2Map(user, HashMap::new);
     * // hashMap: {name=John, age=25} (order not guaranteed)
     * }</pre>
     *
     * @param <M> The type of the resulting Map.
     * @param bean The bean object to be converted into a map.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     * 
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * Map<String, Object> userMap = Maps.bean2Map(user, selectedProps);
     * // userMap: {name=John, email=john@example.com}
     * // age and password are not included
     * }</pre>
     *
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames A collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @return A map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final Collection<String> selectPropNames) {
        return bean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * The map supplier function determines the type of the map to be returned.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties and custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     * 
     * // Only include name and age, using TreeMap
     * Collection<String> selectedProps = Arrays.asList("name", "age");
     * TreeMap<String, Object> sortedMap = Maps.bean2Map(user, selectedProps, TreeMap::new);
     * // sortedMap: {age=25, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> The type of the resulting Map.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames A collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * 
     * // Convert to snake_case
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     * Map<String, Object> snakeMap = Maps.bean2Map(user, props, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, HashMap::new);
     * // snakeMap: {first_name=John, last_name=Doe}
     * 
     * // Convert to UPPER_CASE
     * Map<String, Object> upperMap = Maps.bean2Map(user, props, NamingPolicy.UPPER_CASE, HashMap::new);
     * // upperMap: {FIRSTNAME=John, LASTNAME=Doe}
     * }</pre>
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames The collection of property names to be included in the map.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param mapSupplier The supplier function to create a new instance of the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into the provided output map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * All properties of the bean are included in the map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with existing map
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * 
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     * 
     * Maps.bean2Map(user, existingMap);
     * // existingMap: {id=123, name=John, age=25}
     * }</pre>
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final M output) {
        bean2Map(bean, null, output);
    }

    /**
     * Converts a bean object into the provided output map, selecting only specified properties.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the selectPropNames collection are added to the map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with existing map and selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     * 
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     * 
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * Maps.bean2Map(user, selectedProps, existingMap);
     * // existingMap: {id=123, name=John, email=john@example.com}
     * // age is not included
     * }</pre>
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames A collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with naming policy and output map
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * 
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     * 
     * // Convert property names to snake_case
     * Maps.bean2Map(user, props, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames The set of property names to be included during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                propValue = propInfo.getPropValue(bean);

                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            }
        }
    }

    /**
     * Converts a bean object into a map with optional null property filtering.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with null property filtering
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * 
     * // Include null properties
     * Map<String, Object> mapWithNulls = Maps.bean2Map(user, false);
     * // mapWithNulls: {name=John, age=null, email=john@example.com}
     * 
     * // Ignore null properties
     * Map<String, Object> mapWithoutNulls = Maps.bean2Map(user, true);
     * // mapWithoutNulls: {name=John, email=john@example.com}
     * }</pre>
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty) {
        return bean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a map with optional null property filtering and property exclusion.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with null filtering and ignored properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     * 
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     * 
     * // Ignore null properties and password field
     * Map<String, Object> filteredMap = Maps.bean2Map(user, true, ignoredProps);
     * // filteredMap: {name=John, email=john@example.com}
     * // age (null) and password (ignored) are not included
     * }</pre>
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a map with optional null property filtering and property exclusion.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The map is created by the provided <i>mapSupplier</i>.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * 
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     * 
     * // Create TreeMap ignoring null properties
     * TreeMap<String, Object> sortedMap = Maps.bean2Map(user, true, ignoredProps, TreeMap::new);
     * // sortedMap: {email=john@example.com, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param mapSupplier A function that returns a new map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map with optional null property filtering, property exclusion, and key naming policy.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The keys of the map are formatted according to the provided <i>keyNamingPolicy</i>.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     * 
     * Set<String> ignoredProps = new HashSet<>();
     * 
     * // Convert to snake_case, ignoring null properties
     * Map<String, Object> snakeMap = Maps.bean2Map(user, true, ignoredProps, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // snakeMap: {first_name=John, last_name=Doe}
     * // age is not included because it's null
     * }</pre>
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param keyNamingPolicy The policy used to name the keys in the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Properties can be filtered based on null values, excluded by name, and keys can be transformed using a naming policy.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with all options
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     * user.setPassword("secret");
     * 
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     * 
     * // Create custom map with snake_case keys, ignoring nulls and password
     * TreeMap<String, Object> customMap = Maps.bean2Map(user, true, ignoredProps, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, TreeMap::new);
     * // customMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames The set of property names to be ignored during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param mapSupplier The supplier function to create a new instance of the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * The result is stored in the provided output map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with existing map and null filtering
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * 
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     * 
     * // Add properties to existing map, ignoring nulls
     * Maps.bean2Map(user, true, existingMap);
     * // existingMap: {id=123, name=John, email=john@example.com}
     * // age is not included because it's null
     * }</pre>
     *
     * @param <M> The type of the output map.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param output The map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with existing map, null filtering, and ignored properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     * 
     * Map<String, Object> existingMap = new HashMap<>();
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     * 
     * Maps.bean2Map(user, true, ignoredProps, existingMap);
     * // existingMap: {name=John, email=john@example.com}
     * // age (null) and password (ignored) are not included
     * }</pre>
     *
     * @param <M> The type of the output map.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param output The map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with all options and output map
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     * user.setPassword("secret");
     * 
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     * 
     * // Fill output map with snake_case keys, ignoring nulls and password
     * Maps.bean2Map(user, true, ignoredProps, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames The set of property names to be ignored during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            NamingPolicy keyNamingPolicy, final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if (isLowerCamelCaseOrNoChange) {
                output.put(propName, propValue);
            } else {
                output.put(keyNamingPolicy.convert(propName), propValue);
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The resulting map uses LinkedHashMap to preserve property order.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with nested beans
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * address.setZipCode("10001");
     * user.setAddress(address);
     * 
     * Map<String, Object> deepMap = Maps.deepBean2Map(user);
     * // deepMap: {
     * //   name=John, 
     * //   address={city=New York, zipCode=10001}
     * // }
     * // Note: address is converted to a Map, not kept as Address object
     * }</pre>
     *
     * @param bean The bean to be converted into a Map.
     * @return A Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean) {
        return deepBean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The map type is determined by the provided mapSupplier.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with custom map type
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     * 
     * // Using TreeMap for sorted keys
     * TreeMap<String, Object> sortedDeepMap = Maps.deepBean2Map(user, TreeMap::new);
     * // sortedDeepMap: {
     * //   address={city=New York}, 
     * //   name=John
     * // } (sorted by key)
     * }</pre>
     *
     * @param <M> The type of the Map to which the bean will be converted.
     * @param bean The bean to be converted into a Map.
     * @param mapSupplier A supplier function to create the Map instance.
     * @return A Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     * 
     * Collection<String> props = Arrays.asList("name", "address");
     * Map<String, Object> selectedDeepMap = Maps.deepBean2Map(user, props);
     * // selectedDeepMap: {
     * //   name=John, 
     * //   address={city=New York}
     * // }
     * // age is not included
     * }</pre>
     *
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @return A Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final Collection<String> selectPropNames) {
        return deepBean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, and the map type is determined by mapSupplier.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties and custom map
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * address.setZipCode("10001");
     * user.setAddress(address);
     * 
     * Collection<String> props = Arrays.asList("name", "address");
     * HashMap<String, Object> customDeepMap = Maps.deepBean2Map(user, props, HashMap::new);
     * // customDeepMap: {
     * //   name=John, 
     * //   address={city=New York, zipCode=10001}
     * // }
     * }</pre>
     *
     * @param <M> The type of the Map to which the bean will be converted.
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @param mapSupplier A supplier function to create the Map instance.
     * @return A Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The keys in the map are transformed according to the specified naming policy.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * Address address = new Address();
     * address.setStreetName("Main St");
     * user.setHomeAddress(address);
     * 
     * Collection<String> props = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeMap = Maps.deepBean2Map(user, props, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, HashMap::new);
     * // snakeMap: {
     * //   first_name=John, 
     * //   home_address={street_name=Main St}
     * // }
     * // Note: nested properties are also converted
     * }</pre>
     *
     * @param <M> The type of the Map to which the bean will be converted.
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @param keyNamingPolicy The naming policy to be used for the keys in the resulting Map.
     * @param mapSupplier A supplier function to create the Map instance into which the bean properties will be put.
     * @return A Map representation of the provided bean.
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * All properties are included and stored in the provided output map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with output map
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     * 
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     * 
     * Maps.deepBean2Map(user, existingMap);
     * // existingMap: {
     * //   id=123,
     * //   name=John, 
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> The type of the output map.
     * @param bean The bean to be converted into a Map.
     * @param output The map where the result should be stored.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final M output) {
        deepBean2Map(bean, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included and stored in the provided output map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with selected properties and output map
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     * 
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("name", "address");
     * 
     * Maps.deepBean2Map(user, props, outputMap);
     * // outputMap: {
     * //   name=John, 
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> The type of the output map.
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @param output The map where the result should be stored.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, keys are transformed according to the naming policy, and results are stored in the output map.
     *
     * <p>Example:
     * <pre>{@code
     * // Example with all options
     * User user = new User();
     * user.setFirstName("John");
     * Address address = new Address();
     * address.setStreetName("Main St");
     * user.setHomeAddress(address);
     * 
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("firstName", "homeAddress");
     * 
     * Maps.deepBean2Map(user, props, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {
     * //   first_name=John, 
     * //   home_address={street_name=Main St}
     * // }
     * }</pre>
     *
     * @param <M> The type of the output map.
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @param keyNamingPolicy The naming policy to be used for the keys in the resulting Map.
     * @param output The map where the result should be stored.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            deepBean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, deepBean2Map(propValue, true, null, keyNamingPolicy));
                    } else {
                        output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, true, null, keyNamingPolicy));
                    }
                }
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties with null values will be included in the resulting Map.
     * 
     * <p>Example:
     * <pre>{@code
     * // Given a User bean with nested Address
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Map<String, Object> result = deepBean2Map(user, false);
     * // result: {name=John, age=25, address={city=NYC, zipCode=10001}}
     * 
     * // With ignoreNullProperty=true
     * User userWithNull = new User("Jane", null, null);
     * Map<String, Object> filtered = deepBean2Map(userWithNull, true);
     * // filtered: {name=Jane} (null properties excluded)
     * }</pre>
     *
     * @param bean The bean object to be converted into a Map. Can be any Java object with getter/setter methods.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @return A Map representation of the bean where nested beans are recursively converted to Maps.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty) {
        return deepBean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     * 
     * <p>Example:
     * <pre>{@code
     * // Given a User bean with multiple properties
     * User user = new User("John", 25, "john@example.com", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("email", "age"));
     * Map<String, Object> result = deepBean2Map(user, false, ignored);
     * // result: {name=John, address={city=NYC}} (email and age excluded)
     * }</pre>
     *
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process. Can be null.
     * @return A Map representation of the bean with specified properties excluded.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The resulting Map type can be customized using the mapSupplier function.
     * 
     * <p>Example:
     * <pre>{@code
     * // Create a TreeMap instead of default LinkedHashMap
     * User user = new User("John", 25, new Address("NYC"));
     * TreeMap<String, Object> result = deepBean2Map(user, false, null, 
     *     size -> new TreeMap<>());
     * // result: TreeMap with {address={city=NYC}, age=25, name=John} (sorted keys)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A Map of the specified type containing the bean properties.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The keys in the resulting Map can be transformed according to the specified naming policy.
     * 
     * <p>Example:
     * <pre>{@code
     * // Given a bean with camelCase properties
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * 
     * Map<String, Object> snakeCase = deepBean2Map(user, false, null, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
     * // snakeCase: {first_name=John, last_name=Doe}
     * 
     * Map<String, Object> upperCase = deepBean2Map(user, false, null, 
     *     NamingPolicy.UPPER_CASE);
     * // upperCase: {FIRSTNAME=John, LASTNAME=Doe}
     * }</pre>
     *
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy The naming policy to apply to the keys in the resulting Map. If null, defaults to LOWER_CAMEL_CASE.
     * @return A Map representation of the bean with keys transformed according to the naming policy.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Provides full control over the conversion process including naming policy and Map type.
     * 
     * <p>Example:
     * <pre>{@code
     * // Custom conversion with all options
     * User user = new User("John", null, new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     * 
     * LinkedHashMap<String, Object> result = deepBean2Map(user, true, ignored,
     *     NamingPolicy.UPPER_CASE_WITH_UNDERSCORES, 
     *     size -> new LinkedHashMap<>(size));
     * // result: {NAME=John, ADDRESS={CITY=NYC}} (ordered, uppercase with underscores)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy The naming policy to apply to the keys in the resulting Map.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A Map of the specified type with full customization applied.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion is performed in-place into the provided output Map.
     * 
     * <p>Example:
     * <pre>{@code
     * // Reuse existing map
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("timestamp", System.currentTimeMillis());
     * 
     * User user = new User("John", 25);
     * deepBean2Map(user, false, existingMap);
     * // existingMap now contains: {timestamp=..., name=John, age=25}
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param output The Map instance into which the bean properties will be put. Existing entries are preserved.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        deepBean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     * 
     * <p>Example:
     * <pre>{@code
     * // Populate map with selective properties
     * Map<String, Object> output = new HashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     * 
     * User user = new User("John", "pass123", "123-45-6789");
     * deepBean2Map(user, false, ignored, output);
     * // output: {name=John} (sensitive fields excluded)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param output The Map instance into which the bean properties will be put.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion process can be customized by specifying properties to ignore, whether to ignore {@code null} properties, and the naming policy for keys.
     * 
     * <p>Example:
     * <pre>{@code
     * // Full control over in-place conversion
     * Map<String, Object> output = new TreeMap<>(); // Sorted map
     * Set<String> ignored = new HashSet<>(Arrays.asList("id"));
     * 
     * Product product = new Product("Widget", 29.99, new Category("Electronics"));
     * deepBean2Map(product, true, ignored, NamingPolicy.UPPER_CASE, output);
     * // output: {CATEGORY={NAME=Electronics}, NAME=Widget, PRICE=29.99} (sorted)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy The naming policy to apply to the keys in the output Map.
     * @param output The Map instance into which the bean properties will be put.
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            } else {
                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                } else {
                    output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * All properties from the bean are included in the result.
     * 
     * <p>Example:
     * <pre>{@code
     * // Given nested beans
     * User user = new User("John", new Address("NYC", "10001"));
     * Map<String, Object> flat = bean2FlatMap(user);
     * // flat: {name=John, address.city=NYC, address.zipCode=10001}
     * 
     * // Deep nesting
     * Company company = new Company("TechCorp", 
     *     new Address("NYC", new Location(40.7128, -74.0060)));
     * Map<String, Object> result = bean2FlatMap(company);
     * // result: {name=TechCorp, address.city=NYC, 
     * //          address.location.latitude=40.7128, 
     * //          address.location.longitude=-74.0060}
     * }</pre>
     *
     * @param bean The bean object to be converted into a flat map.
     * @return A map representing the bean object with nested properties flattened using dot notation.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean) {
        return bean2FlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * The type of Map returned can be customized using the mapSupplier.
     * 
     * <p>Example:
     * <pre>{@code
     * // Create a sorted flat map
     * User user = new User("John", new Address("NYC", "10001"));
     * TreeMap<String, Object> sortedFlat = bean2FlatMap(user, 
     *     size -> new TreeMap<>());
     * // sortedFlat: {address.city=NYC, address.zipCode=10001, name=John} (sorted)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A map of the specified type with nested properties flattened.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included in the result.
     * 
     * <p>Example:
     * <pre>{@code
     * // Select specific properties including nested ones
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Collection<String> select = Arrays.asList("name", "address");
     * Map<String, Object> result = bean2FlatMap(user, select);
     * // result: {name=John, address.city=NYC, address.zipCode=10001}
     * 
     * // Select only top-level properties
     * Collection<String> topLevel = Arrays.asList("name", "age");
     * Map<String, Object> flat = bean2FlatMap(user, topLevel);
     * // flat: {name=John, age=25} (address excluded)
     * }</pre>
     *
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the resulting map. Nested properties of selected beans are automatically included.
     * @return A map with only the selected properties flattened.
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final Collection<String> selectPropNames) {
        return bean2FlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties and custom Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines property selection with Map type customization.
     * 
     * <p>Example:
     * <pre>{@code
     * // Select properties and use custom map type
     * Employee emp = new Employee("John", "IT", new Manager("Jane"));
     * Collection<String> select = Arrays.asList("name", "manager");
     * 
     * LinkedHashMap<String, Object> result = bean2FlatMap(emp, select,
     *     size -> new LinkedHashMap<>(size));
     * // result: {name=John, manager.name=Jane} (ordered, dept excluded)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the resulting map.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A map of the specified type with selected properties flattened.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and a specified naming policy.
     * This method takes a bean object and transforms it into a map where the keys are the property names of the bean and the values are the corresponding property values.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * 
     * <p>Example:
     * <pre>{@code
     * // With naming policy transformation
     * User user = new User();
     * user.setFirstName("John");
     * user.setHomeAddress(new Address("NYC"));
     * 
     * Collection<String> select = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeCase = bean2FlatMap(user, select,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES,
     *     size -> new HashMap<>(size));
     * // snakeCase: {first_name=John, home_address.city=NYC}
     * }</pre>
     *
     * @implSpec
     * <code>
     *  Maps.bean2FlatMap(new User("John", new Address("New York"))) ==> {"name"="John", "address.city"="New York"};
     *  <br />
     *  // with the below bean classes
     *  <br />
     *  public static class User {
     *     private String name;
     *     private Address address;
     *  }
     *  <br/>
     *  public static class Address {
     *      private String city;
     *  }
     * </code>
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the resulting map. If this is empty, all properties are included.
     * @param keyNamingPolicy The naming policy for the keys in the resulting map.
     * @param mapSupplier A function that generates a new map instance. The function argument is the initial map capacity.
     * @return A map representing the bean object. Each key-value pair in the map corresponds to a property of the bean.
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2FlatMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * All properties from the bean are included in the output.
     * 
     * <p>Example:
     * <pre>{@code
     * // Populate existing map with flattened bean
     * Map<String, Object> output = new HashMap<>();
     * output.put("version", "1.0");
     * 
     * User user = new User("John", new Address("NYC"));
     * bean2FlatMap(user, output);
     * // output: {version=1.0, name=John, address.city=NYC}
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param output The Map instance into which the flattened bean properties will be put. Existing entries are preserved.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final M output) {
        bean2FlatMap(bean, null, output);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included.
     * 
     * <p>Example:
     * <pre>{@code
     * // Selective flattening into existing map
     * Map<String, Object> output = new LinkedHashMap<>();
     * Collection<String> select = Arrays.asList("name", "contact");
     * 
     * Customer customer = new Customer("John", "123-456", 
     *     new Contact("john@email.com", "555-1234"));
     * bean2FlatMap(customer, select, output);
     * // output: {name=John, contact.email=john@email.com, contact.phone=555-1234}
     * // (customerId excluded)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the output map.
     * @param output The Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation with full customization options and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides control over property selection and key naming policy.
     * 
     * <p>Example:
     * <pre>{@code
     * // Full customization of flattening process
     * Map<String, Object> output = new TreeMap<>();
     * Collection<String> select = Arrays.asList("productName", "category");
     * 
     * Product product = new Product("WidgetPro", 
     *     new Category("Electronics", "Gadgets"));
     * bean2FlatMap(product, select, NamingPolicy.UPPER_CASE, output);
     * // output: {CATEGORY.NAME=Electronics, CATEGORY.SUBCATEGORY=Gadgets, 
     * //          PRODUCTNAME=WidgetPro} (sorted, uppercase)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the output map.
     * @param keyNamingPolicy The naming policy to apply to the keys in the output map.
     * @param output The Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2FlatMap(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    bean2FlatMap(propValue, true, null, keyNamingPolicy, isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation with control over null property handling.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Properties with null values can be included or excluded based on the ignoreNullProperty parameter.
     * 
     * <p>Example:
     * <pre>{@code
     * // Include null properties
     * User user = new User("John", null, new Address("NYC", null));
     * Map<String, Object> withNulls = bean2FlatMap(user, false);
     * // withNulls: {name=John, age=null, address.city=NYC, address.zipCode=null}
     * 
     * // Exclude null properties
     * Map<String, Object> noNulls = bean2FlatMap(user, true);
     * // noNulls: {name=John, address.city=NYC}
     * }</pre>
     *
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @return A flat map representation of the bean with null handling as specified.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty) {
        return bean2FlatMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a flat map representation with control over null property handling and property exclusion.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines null value filtering with property name exclusion.
     * 
     * <p>Example:
     * <pre>{@code
     * // Filter nulls and exclude specific properties
     * User user = new User("John", null, "secret123", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("password"));
     * 
     * Map<String, Object> result = bean2FlatMap(user, true, ignored);
     * // result: {name=John, address.city=NYC} 
     * // (age is null so excluded, password is in ignored set)
     * }</pre>
     *
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames A set of property names to be excluded from the resulting map.
     * @return A flat map with specified filtering applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a flat map representation with control over null handling, property exclusion, and Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides flexibility in filtering and Map implementation.
     * 
     * <p>Example:
     * <pre>{@code
     * // Custom map with filtering
     * Employee emp = new Employee("John", null, "IT", new Office("Building A"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("department"));
     * 
     * TreeMap<String, Object> result = bean2FlatMap(emp, true, ignored,
     *     size -> new TreeMap<>());
     * // result: {name=John, office.building=Building A} 
     * // (sorted, salary null excluded, department ignored)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames A set of property names to be excluded from the resulting map.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A map of the specified type with filtering applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with control over null handling, property exclusion, and key naming policy.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides comprehensive control over the flattening process.
     * 
     * <p>Example:
     * <pre>{@code
     * // Transform to snake_case with filtering
     * UserProfile profile = new UserProfile();
     * profile.setFirstName("John");
     * profile.setLastLogin(null);
     * profile.setHomeAddress(new Address("NYC"));
     * 
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     * Map<String, Object> result = bean2FlatMap(profile, true, ignored,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
     * // result: {first_name=John, home_address.city=NYC}
     * // (last_login null excluded, internal_id ignored, snake_case keys)
     * }</pre>
     *
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames A set of property names to be excluded from the resulting map.
     * @param keyNamingPolicy The naming policy to apply to the keys in the resulting map.
     * @return A flat map with comprehensive customization applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with full control over all conversion aspects.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This is the most flexible variant offering complete customization.
     * 
     * <p>Example:
     * <pre>{@code
     * // Complete customization example
     * Order order = new Order("ORD-123", null, 
     *     new Customer("John", new Address("NYC", "10001")));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalNotes"));
     * 
     * LinkedHashMap<String, Object> result = bean2FlatMap(order, true, ignored,
     *     NamingPolicy.UPPER_CASE_WITH_UNDERSCORES,
     *     size -> new LinkedHashMap<>(size * 2)); // larger initial capacity
     * // result: {ORDER_ID=ORD-123, CUSTOMER.NAME=John, 
     * //          CUSTOMER.ADDRESS.CITY=NYC, CUSTOMER.ADDRESS.ZIP_CODE=10001}
     * // (amount null excluded, internal_notes ignored, ordered map)
     * }</pre>
     *
     * @param <M> The type of Map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames A set of property names to be excluded from the resulting map.
     * @param keyNamingPolicy The naming policy to apply to the keys in the resulting map.
     * @param mapSupplier A function that creates a new Map instance. The function argument is the initial capacity.
     * @return A fully customized flat map representation of the bean.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with null handling.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This is an in-place operation that modifies the provided output Map.
     * 
     * <p>Example:
     * <pre>{@code
     * // Populate existing map with null filtering
     * Map<String, Object> output = new HashMap<>();
     * output.put("timestamp", new Date());
     * 
     * User user = new User("John", null, new Address("NYC"));
     * bean2FlatMap(user, true, output);
     * // output: {timestamp=..., name=John, address.city=NYC}
     * // (age null is excluded)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the output map.
     * @param output The Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with filtering options.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines in-place operation with null handling and property exclusion.
     * 
     * <p>Example:
     * <pre>{@code
     * // In-place population with multiple filters
     * Map<String, Object> output = new LinkedHashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     * 
     * Account account = new Account("john123", "pass", null, "123-45-6789");
     * bean2FlatMap(account, true, ignored, output);
     * // output: {username=john123}
     * // (password and ssn ignored, balance null excluded)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames A set of property names to be excluded from the output map.
     * @param output The Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with full customization.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This method provides complete control over the in-place flattening operation.
     * 
     * <p>Example:
     * <pre>{@code
     * // Full control in-place flattening
     * Map<String, Object> output = new TreeMap<>(); // sorted output
     * Set<String> ignored = new HashSet<>(Arrays.asList("metadata"));
     * 
     * Document doc = new Document("Report", null, 
     *     new Author("John", new Department("Research")));
     * bean2FlatMap(doc, true, ignored, 
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES, output);
     * // output: {author.department.name=research, author.name=john, title=report}
     * // (sorted keys, snake_case, version null excluded, metadata ignored)
     * }</pre>
     *
     * @param <M> The type of Map to populate.
     * @param bean The bean object to be converted into a flat map.
     * @param ignoreNullProperty If {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames A set of property names to be excluded from the output map.
     * @param keyNamingPolicy The naming policy to apply to the keys in the output map.
     * @param output The Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Collection<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final String parentPropName, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final boolean isNullParentPropName = (parentPropName == null);
        final Class<?> beanClass = bean.getClass();

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : ParserUtil.getBeanInfo(beanClass).propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isNullParentPropName) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(parentPropName + WD.PERIOD + propName, propValue);
                    } else {
                        output.put(parentPropName + WD.PERIOD + keyNamingPolicy.convert(propName), propValue);
                    }
                }
            } else {
                if (isNullParentPropName) {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                } else {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            parentPropName + WD.PERIOD + (isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName)), output);
                }
            }
        }
    }

    /**
     * Check bean class.
     *
     * @param <T>
     * @param cls
     */
    private static <T> void checkBeanClass(final Class<T> cls) {
        if (!ClassUtil.isBeanClass(cls)) {
            throw new IllegalArgumentException("No property getter/setter method is found in the specified class: " + ClassUtil.getCanonicalClassName(cls));
        }
    }

    //    @SuppressWarnings("deprecation")
    //    @Beta
    //    public static <T> Map<String, Object> record2Map(final T record) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        return record2Map(new LinkedHashMap<>(ClassUtil.getRecordInfo(recordClass).fieldNames().size()), record);
    //    }
    //
    //    @Beta
    //    public static <T, M extends Map<String, Object>> M record2Map(final T record, final Supplier<? extends M> mapSupplier) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        return record2Map(mapSupplier.get(), record);
    //    }
    //
    //    @SuppressWarnings("deprecation")
    //    @Beta
    //    public static <T, M extends Map<String, Object>> M record2Map(final T record, final IntFunction<? extends M> mapSupplier) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        return record2Map(mapSupplier.apply(ClassUtil.getRecordInfo(recordClass).fieldNames().size()), record);
    //    }
    //
    //    @Beta
    //    static <M extends Map<String, Object>> M record2Map(final Object record, final M output) {
    //        if (record == null) {
    //            return output;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        @SuppressWarnings("deprecation")
    //        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(recordClass);
    //
    //        try {
    //            for (Tuple5<String, Field, Method, Type<Object>, Integer> tp : recordInfo.fieldMap().values()) {
    //                output.put(tp._1, tp._3.invoke(record));
    //            }
    //        } catch (IllegalAccessException | InvocationTargetException e) {
    //            // Should never happen.
    //            throw ExceptionUtil.toRuntimeException(e, true);
    //        }
    //
    //        return output;
    //    }
    //
    //    @Beta
    //    public static <T> T map2Record(final Map<String, Object> map, final Class<T> recordClass) {
    //        @SuppressWarnings("deprecation")
    //        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(recordClass);
    //
    //        if (map == null) {
    //            return null;
    //        }
    //
    //        final Object[] args = new Object[recordInfo.fieldNames().size()];
    //        Object val = null;
    //        int idx = 0;
    //
    //        for (String fieldName : recordInfo.fieldNames()) {
    //            val = map.get(fieldName);
    //
    //            // TODO, should be ignored?
    //            //    if (val == null && !map.containsKey(tp._1)) {
    //            //        throw new IllegalArgumentException("No value found for field: " + tp._1 + " from the input map");
    //            //    }
    //
    //            args[idx++] = val;
    //        }
    //
    //        return (T) recordInfo.creator().apply(args);
    //    }

    //    public static class MapGetter<K, V> {
    //
    //        private final Map<K, V> map;
    //        private final boolean defaultForPrimitiveOrBoxedType;
    //
    //        MapGetter(final Map<K, V> map, final boolean defaultForPrimitiveOrBoxedType) {
    //            this.map = map;
    //            this.defaultForPrimitiveOrBoxedType = defaultForPrimitiveOrBoxedType;
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param <K>
    //         * @param <V>
    //         * @param map
    //         * @return
    //         */
    //        public static <K, V> MapGetter<K, V> of(final Map<K, V> map) {
    //            return of(map, false);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param <K>
    //         * @param <V>
    //         * @param map
    //         * @param defaultForPrimitiveOrBoxedType
    //         * @return
    //         */
    //        public static <K, V> MapGetter<K, V> of(final Map<K, V> map, final boolean defaultForPrimitiveOrBoxedType) {
    //            return new MapGetter<>(map, defaultForPrimitiveOrBoxedType);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Boolean getBoolean(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return Boolean.FALSE;
    //            } else if (value instanceof Boolean) {
    //                return (Boolean) value;
    //            }
    //
    //            return Strings.parseBoolean(N.toString(value));
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Character getChar(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Character) {
    //                return (Character) value;
    //            }
    //
    //            return Strings.parseChar(N.toString(value));
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Byte getByte(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Byte) {
    //                return (Byte) value;
    //            }
    //
    //            return Numbers.toByte(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Short getShort(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Short) {
    //                return (Short) value;
    //            }
    //
    //            return Numbers.toShort(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Integer getInt(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Integer) {
    //                return (Integer) value;
    //            }
    //
    //            return Numbers.toInt(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Long getLong(final K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0L;
    //            } else if (value instanceof Long) {
    //                return (Long) value;
    //            }
    //
    //            return Numbers.toLong(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Float getFloat(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0F;
    //            } else if (value instanceof Float) {
    //                return (Float) value;
    //            }
    //
    //            return Numbers.toFloat(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Double getDouble(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0d;
    //            } else if (value instanceof Double) {
    //                return (Double) value;
    //            }
    //
    //            return Numbers.toDouble(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public BigInteger getBigInteger(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof BigInteger) {
    //                return (BigInteger) value;
    //            }
    //
    //            return N.convert(value, BigInteger.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public BigDecimal getBigDecimal(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof BigDecimal) {
    //                return (BigDecimal) value;
    //            }
    //
    //            return N.convert(value, BigDecimal.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public String getString(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof String) {
    //                return (String) value;
    //            }
    //
    //            return N.stringOf(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Calendar getCalendar(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof Calendar) {
    //                return (Calendar) value;
    //            }
    //
    //            return N.convert(value, Calendar.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Date getJUDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof Date) {
    //                return (Date) value;
    //            }
    //
    //            return N.convert(value, Date.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Date getDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Date) {
    //                return (java.sql.Date) value;
    //            }
    //
    //            return N.convert(value, java.sql.Date.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Time getTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Time) {
    //                return (java.sql.Time) value;
    //            }
    //
    //            return N.convert(value, java.sql.Time.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Timestamp getTimestamp(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Timestamp) {
    //                return (java.sql.Timestamp) value;
    //            }
    //
    //            return N.convert(value, java.sql.Timestamp.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalDate getLocalDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalDate) {
    //                return (LocalDate) value;
    //            }
    //
    //            return N.convert(value, LocalDate.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalTime getLocalTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalTime) {
    //                return (LocalTime) value;
    //            }
    //
    //            return N.convert(value, LocalTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalDateTime getLocalDateTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalDateTime) {
    //                return (LocalDateTime) value;
    //            }
    //
    //            return N.convert(value, LocalDateTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public ZonedDateTime getZonedDateTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof ZonedDateTime) {
    //                return (ZonedDateTime) value;
    //            }
    //
    //            return N.convert(value, ZonedDateTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public V getObject(K key) {
    //            return map.get(key);
    //        }
    //
    //        /**
    //         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
    //         *
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param targetType
    //         * @return
    //         */
    //        public <T> T get(final Object key, final Class<? extends T> targetType) {
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return (T) (defaultForPrimitiveOrBoxedType && ClassUtil.isPrimitiveWrapper(targetType) ? N.defaultValueOf(ClassUtil.unwrap(targetType))
    //                        : N.defaultValueOf(targetType));
    //            }
    //
    //            if (targetType.isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return N.convert(val, targetType);
    //        }
    //
    //        /**
    //         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
    //         *
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param targetType
    //         * @return
    //         */
    //        public <T> T get(final Object key, final Type<? extends T> targetType) {
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return (T) (defaultForPrimitiveOrBoxedType && targetType.isPrimitiveWrapper() ? N.defaultValueOf(ClassUtil.unwrap(targetType.clazz()))
    //                        : targetType.defaultValue());
    //            }
    //
    //            if (targetType.clazz().isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return N.convert(val, targetType);
    //        }
    //
    //        /**
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param defaultForNull
    //         * @return
    //         * @throws IllegalArgumentException
    //         */
    //        public <T> T get(final Object key, final T defaultForNull) throws IllegalArgumentException {
    //            N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR
    //
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return defaultForNull;
    //            }
    //
    //            if (defaultForNull.getClass().isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return (T) N.convert(val, defaultForNull.getClass());
    //        }
    //    }

    /**
     * Replaces the keys in the specified map using the provided key converter function.
     * This method iterates over the keys in the map and applies the key converter function to each key.
     * If the converted key is different from the original key, the entry is moved to the new key.
     * Note that if multiple original keys convert to the same new key, the last value will overwrite previous ones.
     * 
     * <p>Example:
     * <pre>{@code
     * // Convert keys to uppercase
     * Map<String, Integer> map = new HashMap<>();
     * map.put("name", 1);
     * map.put("age", 2);
     * map.put("city", 3);
     * 
     * replaceKeys(map, String::toUpperCase);
     * // map now contains: {NAME=1, AGE=2, CITY=3}
     * 
     * // Add prefix to keys
     * Map<String, String> data = new HashMap<>();
     * data.put("id", "123");
     * data.put("type", "user");
     * 
     * replaceKeys(data, key -> "prefix_" + key);
     * // data now contains: {prefix_id=123, prefix_type=user}
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param map the map whose keys are to be replaced. This map is modified in-place.
     * @param keyConverter the function to apply to each key. Must not return null.
     */
    @Beta
    public static <K> void replaceKeys(final Map<K, ?> map, final Function<? super K, ? extends K> keyConverter) {
        if (N.isEmpty(map)) {
            return;
        }

        final Map<K, Object> mapToUse = (Map<K, Object>) map;
        final List<K> keys = new ArrayList<>(mapToUse.keySet());
        K newKey = null;

        for (final K key : keys) {
            newKey = keyConverter.apply(key);

            if (!N.equals(key, newKey)) {
                mapToUse.put(newKey, mapToUse.remove(key));
            }
        }
    }

    /**
     * Replaces the keys in the specified map using the provided key converter function and merges values if necessary.
     * This method iterates over the keys in the map and applies the key converter function to each key.
     * If the converted key is different from the original key, the entry is moved to the new key.
     * If there is a conflict (i.e., the new key already exists in the map), the merger function is used to resolve the conflict.
     * 
     * <p>Example:
     * <pre>{@code
     * // Merge values when keys collide
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a1", 10);
     * map.put("a2", 20);
     * map.put("b1", 30);
     * 
     * replaceKeys(map, key -> key.substring(0, 1), Integer::sum);
     * // map now contains: {a=30, b=30} (a1 and a2 merged to 'a')
     * 
     * // Concatenate strings on collision
     * Map<String, String> data = new HashMap<>();
     * data.put("user_1", "John");
     * data.put("user_2", "Jane");
     * data.put("admin_1", "Bob");
     * 
     * replaceKeys(data, key -> key.split("_")[0], 
     *     (v1, v2) -> v1 + ", " + v2);
     * // data now contains: {user=John, Jane, admin=Bob}
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose keys are to be replaced. This map is modified in-place.
     * @param keyConverter the function to apply to each key. Must not return null.
     * @param merger the function to merge values in case of key conflicts. The first argument is the existing value, the second is the new value.
     */
    @Beta
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
