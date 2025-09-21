/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * Similar to {@link Map}, but in which each key may be associated with <i>multiple</i> values.
 *
 * <ul>
 * <li>a ->1, 2
 * <li>b -> 3
 * </ul>
 *
 * @param <K> the key type
 * @param <E>
 * @param <V> the value type
 * @see N#newMultimap(Supplier, Supplier)
 * @see N#newListMultimap()
 * @see N#newListMultimap(Class, Class)
 * @see N#newListMultimap(Supplier, Supplier)
 * @see N#newSetMultimap()
 * @see N#newSetMultimap(Class, Class)
 * @see N#newSetMultimap(Supplier, Supplier)
 *
 */
public sealed class Multimap<K, E, V extends Collection<E>> implements Iterable<Map.Entry<K, V>> permits ListMultimap, SetMultimap {

    final Supplier<? extends Map<K, V>> mapSupplier;

    final Supplier<? extends V> valueSupplier;

    final Map<K, V> backingMap;

    /**
     * Constructs a Multimap with the default initial capacity.
     *
     * This constructor initializes a Multimap with a HashMap for the backing map and an ArrayList for the value collection.
     */
    Multimap() {
        this(HashMap.class, ArrayList.class);
    }

    /**
     * Constructs a Multimap with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the Multimap.
     */
    Multimap(final int initialCapacity) {
        this(N.newHashMap(initialCapacity), (Supplier<V>) Suppliers.ofList());
    }

    /**
     * Constructs a Multimap with the specified map type and collection type.
     *
     * This constructor initializes a Multimap with a map of the specified type for the backing map
     * and a collection of the specified type for the value collection.
     *
     * @param mapType The class of the map to be used as the backing map.
     * @param valueType The class of the collection to be used as the value collection.
     */
    @SuppressWarnings("rawtypes")
    Multimap(final Class<? extends Map> mapType, final Class<? extends Collection> valueType) {
        this(Suppliers.ofMap(mapType), valueType2Supplier(valueType));
    }

    /**
     * Constructs a Multimap with the specified map supplier and value supplier.
     *
     * This constructor initializes a Multimap with a map provided by the specified supplier for the backing map
     * and a collection provided by the specified supplier for the value collection.
     *
     * @param mapSupplier The supplier of the map to be used as the backing map.
     * @param valueSupplier The supplier of the collection to be used as the value collection.
     */
    Multimap(final Supplier<? extends Map<K, V>> mapSupplier, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = mapSupplier;
        this.valueSupplier = valueSupplier;
        backingMap = mapSupplier.get();
    }

    /**
     * Constructs a Multimap with the specified map and value supplier.
     *
     * This constructor initializes a Multimap with a map provided as an argument for the backing map
     * and a collection provided by the specified supplier for the value collection.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueSupplier The supplier of the collection to be used as the value collection.
     */
    @Internal
    Multimap(final Map<K, V> valueMap, final Supplier<? extends V> valueSupplier) {
        mapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.valueSupplier = valueSupplier;
        backingMap = valueMap;
    }

    /**
     * Converts the provided class type into a Supplier.
     *
     * This method takes a class type that extends Collection and returns a Supplier of that type.
     * It is used to dynamically create instances of the specified collection type.
     *
     * @param valueType The class type that extends Collection.
     * @return A Supplier of the specified collection type.
     */
    @SuppressWarnings("rawtypes")
    static Supplier valueType2Supplier(final Class<? extends Collection> valueType) {
        return Suppliers.ofCollection(valueType);
    }

    /**
     * Returns the first value associated with the specified key in this Multimap.
     * This method is useful when you expect a key to have at least one value and want to retrieve
     * just the first one from the collection of values.
     *
     * <p>If the key is not present in the Multimap or has an empty collection of values,
     * this method returns {@code null}.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("key1", 10);
     * multimap.put("key1", 20);
     * Integer first = multimap.getFirst("key1"); // Returns 10
     * Integer missing = multimap.getFirst("key2"); // Returns null
     * }</pre>
     *
     * @param key the key whose associated first value is to be returned
     * @return the first value associated with the specified key, or {@code null} if the key
     *         has no associated values or is not present in the Multimap
     * @see #getFirstOrDefault(Object, Object)
     * @see #get(Object)
     */
    public E getFirst(final K key) {
        final V values = backingMap.get(key);
        return N.isEmpty(values) ? null : N.firstOrNullIfEmpty(values);
    }

    /**
     * Returns the first value associated with the specified key, or a default value if not found.
     * This method provides a null-safe way to retrieve the first value for a key, returning
     * the specified default value instead of null when the key is absent or has no values.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("exists", 42);
     * Integer value1 = multimap.getFirstOrDefault("exists", -1);   // Returns 42
     * Integer value2 = multimap.getFirstOrDefault("missing", -1); // Returns -1
     * }</pre>
     *
     * @param key the key whose associated first value is to be returned
     * @param defaultValue the default value to return if no value is associated with the key
     * @return the first value associated with the specified key, or the default value if the key
     *         has no associated values or is not present in the Multimap
     * @see #getFirst(Object)
     */
    public E getFirstOrDefault(final K key, final E defaultValue) {
        final V values = backingMap.get(key);
        return N.isEmpty(values) ? defaultValue : N.firstOrDefaultIfEmpty(values, defaultValue);
    }

    /**
     * Returns the value collection associated with the specified key in the Multimap.
     * If the key is not present in the Multimap, this method returns {@code null}.
     *
     * <br />
     * The returned collection is backed by the Multimap, so changes to the returned collection are reflected in the Multimap.
     * Usually, the returned collection should not be modified outside this Multimap directly, because it may cause unexpected behavior.
     *
     * @param key The key whose associated value collection is to be returned.
     * @return The value collection associated with the specified key, or {@code null} if the key is not present in the Multimap.
     */
    public V get(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.get(key);
    }

    /**
     * Returns the value collection associated with the specified key in the Multimap, or the provided default value if no value is found.
     *
     * <br />
     * The returned collection is backed by the Multimap, so changes to the returned collection are reflected in the Multimap.
     * Usually, the returned collection should not be modified outside this Multimap directly, because it may cause unexpected behavior.
     *
     * @param key The key whose associated value collection is to be returned.
     * @param defaultValue The default value to return if no value is associated with the key.
     * @return The value collection associated with the specified key, or the default value if the key is not present in the Multimap.
     */
    public V getOrDefault(final Object key, final V defaultValue) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V value = backingMap.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    /**
     * Associates the specified value with the specified key in this Multimap.
     * Unlike a regular Map, this method adds the value to the collection of values
     * associated with the key rather than replacing any existing value.
     *
     * <p>If the key is not already present in the Multimap, a new collection is created
     * for the key using the configured value supplier, and the value is added to it.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.put("fruits", "apple");
     * multimap.put("fruits", "banana");  // "fruits" now maps to ["apple", "banana"]
     * multimap.put("vegetables", "carrot"); // New key added
     * }</pre>
     *
     * <p><b>Thread Safety:</b> This operation is not thread-safe. External synchronization
     * is required for concurrent access.</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @return {@code true} if the value was successfully added to the collection,
     *         {@code false} if the collection does not permit duplicates and already contains the value
     * @throws NullPointerException if the key is null (depending on the backing map implementation)
     * @see #putIfAbsent(Object, Object)
     * @see #putMany(Object, Collection)
     */
    public boolean put(final K key, final E e) {
        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            backingMap.put(key, val);
        }

        return val.add(e);
    }

    /**
     * Associates all the specified keys and values from the provided map to this Multimap.
     *
     * This method iterates over the provided map and for each entry, it associates the key with the value in this Multimap.
     * If the Multimap previously contained mappings for a key, the new value is added to the collection of values associated with this key.
     *
     * @param m The map whose keys and values are to be added to this Multimap.
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise.
     */
    public boolean put(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        K key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                backingMap.put(key, val);
            }

            wasModified |= val.add(e.getValue());
        }

        return wasModified;
    }

    /**
     * Associates the specified value with the specified key only if the value is not already present.
     * This method provides a way to add a value to a key's collection while avoiding duplicates.
     *
     * <p>The method behaves as follows:</p>
     * <ul>
     *   <li>If the key is not present, creates a new collection and adds the value</li>
     *   <li>If the key is present but the value is not in its collection, adds the value</li>
     *   <li>If the key is present and the value already exists in its collection, does nothing</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * SetMultimap<String, Integer> multimap = N.newSetMultimap();
     * multimap.putIfAbsent("numbers", 1); // Returns true, adds 1
     * multimap.putIfAbsent("numbers", 1); // Returns false, 1 already exists
     * multimap.putIfAbsent("numbers", 2); // Returns true, adds 2
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key if not already present
     * @return {@code true} if the value was added (either to a new or existing collection),
     *         {@code false} if the value already existed in the key's collection
     * @see #putIfKeyAbsent(Object, Object)
     * @see #put(Object, Object)
     */
    public boolean putIfAbsent(final K key, final E e) {
        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            backingMap.put(key, val);
        } else if (val.contains(e)) {
            return false;
        }

        return val.add(e);
    }

    /**
     * Associates the specified value with the specified key only if the key is not already present.
     * This method is useful when you want to ensure a key is only initialized once, regardless
     * of how many values might be added to it later.
     *
     * <p>Unlike {@link #putIfAbsent(Object, Object)}, this method only checks for key presence,
     * not whether the specific value already exists in the collection.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putIfKeyAbsent("new-key", "value1");    // Returns true, creates new key
     * multimap.putIfKeyAbsent("new-key", "value2");    // Returns false, key exists
     * multimap.put("new-key", "value2");              // Can still add more values
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @return {@code true} if the key was not present and a new mapping was created,
     *         {@code false} if the key was already present (value is not added)
     * @see #putIfAbsent(Object, Object)
     * @see #put(Object, Object)
     */
    public boolean putIfKeyAbsent(final K key, final E e) {
        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            val.add(e);
            backingMap.put(key, val);
            return true;
        }

        return false;
    }

    /**
     * Associates all values from the provided collection with the specified key in this Multimap.
     * This is a bulk operation that adds multiple values to a key at once.
     *
     * <p>If the key already exists, the values are added to its existing collection.
     * If the key doesn't exist, a new collection is created and populated with the values.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("evens", 2);
     * multimap.putMany("evens", Arrays.asList(4, 6, 8)); // "evens" -> [2, 4, 6, 8]
     * multimap.putMany("odds", Arrays.asList(1, 3, 5));  // "odds" -> [1, 3, 5]
     * }</pre>
     *
     * <p><b>Note:</b> The behavior depends on the underlying collection type. For ListMultimap,
     * duplicates are allowed. For SetMultimap, duplicate values are automatically filtered out.</p>
     *
     * @param key the key with which the specified values are to be associated
     * @param c the collection of values to be associated with the specified key
     * @return {@code true} if any values were added to the Multimap,
     *         {@code false} if the collection was empty or no values were added
     * @throws NullPointerException if the collection is null
     * @see #put(Object, Object)
     * @see #putManyIfKeyAbsent(Object, Collection)
     * @see Collection#addAll(Collection)
     */
    public boolean putMany(final K key, final Collection<? extends E> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            backingMap.put(key, val);
        }

        return val.addAll(c);
    }

    /**
     * Associates all the specified values from the provided collection with the specified key in this Multimap if the key is not already present.
     * If the key is already present, this method does nothing.
     *
     * @param key The key with which the specified values are to be associated.
     * @param c The collection of values to be associated with the specified key.
     * @return {@code true} if the key was not already present and the association was successfully added, {@code false} otherwise If the key is already present or the specified value collection is empty.
     * @see Collection#addAll(Collection)
     */
    public boolean putManyIfKeyAbsent(final K key, final Collection<? extends E> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            val.addAll(c);
            backingMap.put(key, val);
            return true;
        }

        return false;
    }

    /**
     * Associates all the specified keys and their corresponding collections of values from the provided map to this Multimap.
     *
     * This method iterates over the provided map and for each entry, it associates the key with the collection of values in this Multimap.
     * If the Multimap previously contained mappings for a key, the new values are added to the collection of values associated with this key.
     *
     * @param m The map whose keys and collections of values are to be added to this Multimap.
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise.
     * @see Collection#addAll(Collection)
     */
    public boolean putMany(final Map<? extends K, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        K key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            if (N.isEmpty(e.getValue())) {
                continue;
            }

            key = e.getKey();
            val = backingMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                backingMap.put(key, val);
            }

            wasModified |= val.addAll(e.getValue());
        }

        return wasModified;
    }

    /**
     * Associates all the specified keys and their corresponding collections of values from the provided Multimap to this Multimap.
     *
     * This method iterates over the provided Multimap and for each entry, it associates the key with the collection of values in this Multimap.
     * If this Multimap previously contained mappings for a key, the new values are added to the collection of values associated with this key.
     *
     * @param m The Multimap whose keys and collections of values are to be added to this Multimap.
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise.
     * @see Collection#addAll(Collection)
     */
    public boolean putMany(final Multimap<? extends K, ? extends E, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        K key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            if (N.isEmpty(e.getValue())) {
                continue;
            }

            key = e.getKey();
            val = backingMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                backingMap.put(key, val);
            }

            wasModified |= val.addAll(e.getValue());
        }

        return wasModified;
    }

    /**
     * Removes a single occurrence of the specified value from the collection associated with the given key.
     * This method is useful for removing specific key-value pairs from the Multimap.
     *
     * <p>If the removal leaves the key with an empty collection, the key itself is removed
     * from the Multimap to maintain consistency.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 1);
     * multimap.put("numbers", 2);
     * multimap.put("numbers", 1);
     * 
     * multimap.removeOne("numbers", 1);  // Removes first occurrence of 1
     * // "numbers" now maps to [2, 1]
     * 
     * multimap.removeOne("numbers", 3);  // Returns false, 3 not found
     * }</pre>
     *
     * @param key the key whose associated collection is to be modified
     * @param e the element to be removed from the collection
     * @return {@code true} if the element was found and removed,
     *         {@code false} if the key was not found or the element was not in the collection
     * @see #removeAll(Object)
     * @see #removeMany(Object, Collection)
     */
    public boolean removeOne(final Object key, final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        if (val != null && val.remove(e)) {
            if (val.isEmpty()) {
                //noinspection SuspiciousMethodCalls
                backingMap.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes a single occurrence of each specified key-value pair from this Multimap.
     *
     * This method iterates over the provided map and for each entry, it attempts to remove a single occurrence of the key-value pair from this Multimap.
     * If the key-value pair is successfully removed and the collection of values associated with the key becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param m The map whose key-value pairs are to be removed from this Multimap.
     * @return {@code true} if at least one key-value pair was successfully removed from the Multimap, {@code false} otherwise.
     */
    public boolean removeOne(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            //noinspection SuspiciousMethodCalls
            val = backingMap.get(key);

            if (N.notEmpty(val)) {
                wasModified |= val.remove(e.getValue());

                if (val.isEmpty()) {
                    //noinspection SuspiciousMethodCalls
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes all values associated with the specified key from this Multimap.
     * This is equivalent to removing the entire mapping for the key.
     *
     * <p>After this operation, the key will no longer exist in the Multimap,
     * and any subsequent {@link #get(Object)} calls with this key will return null.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putMany("colors", Arrays.asList("red", "blue", "green"));
     * 
     * Collection<String> removed = multimap.removeAll("colors");
     * // removed contains ["red", "blue", "green"]
     * // multimap.containsKey("colors") returns false
     * }</pre>
     *
     * <p><b>Note:</b> The returned collection is the actual collection that was stored
     * in the Multimap, not a copy. Modifying it after removal may lead to unexpected behavior.</p>
     *
     * @param key the key whose entire mapping is to be removed
     * @return the collection of values that were associated with the key,
     *         or {@code null} if the key was not present in the Multimap
     * @see #removeOne(Object, Object)
     * @see #clear()
     */
    public V removeAll(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.remove(key);
    }

    /**
     * Removes all occurrences of the specified elements from the collection of values associated with the specified key in this Multimap.
     *
     * This method retrieves the collection of values associated with the specified key and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param key The key whose associated collection of values is to be processed.
     * @param c The collection of elements to be removed from the collection of values associated with the specified key.
     * @return {@code true} if at least one element was successfully removed from the collection of values associated with the specified key, {@code false} otherwise.
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Object key, final Collection<?> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        boolean wasModified = false;
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        if (N.notEmpty(val)) {
            //noinspection SuspiciousMethodCalls
            wasModified = val.removeAll(c);

            if (val.isEmpty()) {
                //noinspection SuspiciousMethodCalls
                backingMap.remove(key);
            }
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified elements from the collections of values associated with their respective keys in this Multimap.
     *
     * This method iterates over the provided map and for each entry, it retrieves the collection of values associated with the key in this Multimap
     * and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * <pre>
     * <code>
     * ListMultimap<String, Integer> listMultimap = ListMultimap.of("a", 1, "b", 2, "a", 2, "a", 2); // -> {a=[1, 2, 2], b=[2]}
     * listMultimap.removeAll(N.asMap("a", N.asList(2))); // -> {a=[1], b=[2]}
     * </code>
     * </pre>
     *
     * @param m The map whose keys and collections of elements are to be removed from this Multimap.
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the specified keys, {@code false} otherwise.
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Map<?, ? extends Collection<?>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            //noinspection SuspiciousMethodCalls
            val = backingMap.get(key);

            if (N.notEmpty(val) && N.notEmpty(e.getValue())) {
                //noinspection SuspiciousMethodCalls
                wasModified |= val.removeAll(e.getValue());

                if (val.isEmpty()) {
                    //noinspection SuspiciousMethodCalls
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified elements from the collections of values associated with their respective keys in this Multimap.
     *
     * This method iterates over the provided Multimap and for each entry, it retrieves the collection of values associated with the key in this Multimap
     * and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param m The Multimap whose keys and collections of elements are to be removed from this Multimap.
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the specified keys, {@code false} otherwise.
     * @see #removeMany(Map)
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Multimap<?, ?, ?> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            //noinspection SuspiciousMethodCalls
            val = backingMap.get(key);

            if (N.notEmpty(val) && N.notEmpty(e.getValue())) {
                //noinspection SuspiciousMethodCalls
                wasModified |= val.removeAll(e.getValue());

                if (val.isEmpty()) {
                    //noinspection SuspiciousMethodCalls
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes a single occurrence of the specified value from all collections where the key satisfies the given predicate.
     * This method allows conditional removal based on key properties.
     *
     * <p>The method works in two phases:</p>
     * <ol>
     *   <li>Identifies all keys that satisfy the predicate</li>
     *   <li>For each matching key, removes one occurrence of the specified value from its collection</li>
     * </ol>
     *
     * <p>Keys with empty collections after removal are automatically removed from the Multimap.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("active", Arrays.asList(1, 2, 1, 3));
     * multimap.putMany("inactive", Arrays.asList(1, 2, 3));
     * multimap.putMany("archived", Arrays.asList(1, 2));
     * 
     * // Remove value 1 from keys that don't equal "archived"
     * multimap.removeOneIf(1, key -> !key.equals("archived"));
     * // "active" -> [2, 1, 3], "inactive" -> [2, 3], "archived" -> [1, 2]
     * }</pre>
     *
     * @param value the value to be removed from matching collections
     * @param predicate the predicate that determines which keys to process
     * @return {@code true} if at least one value was removed from any collection,
     *         {@code false} if no values were removed (either no keys matched or value not found)
     * @throws NullPointerException if the predicate is null
     * @see #removeOne(Object, Object)
     * @see #removeManyIf(Collection, Predicate)
     */
    public boolean removeOneIf(final E value, final Predicate<? super K> predicate) {
        Set<K> removingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : removingKeys) {
            wasModified |= removeOne(k, value);
        }

        return wasModified;
    }

    /**
     * Removes a single occurrence of the specified value from the collections of values associated with keys that satisfy the specified predicate.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove a single occurrence of the specified value from it.
     * If the value is successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param value The value to be removed from the collections of values associated with the keys that satisfy the predicate.
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @return {@code true} if at least one value was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @see Collection#remove(Object)
     */
    public boolean removeOneIf(final E value, final BiPredicate<? super K, ? super V> predicate) {
        Set<K> removingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : removingKeys) {
            wasModified |= removeOne(k, value);
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified values from collections where the key satisfies the given predicate.
     * This method enables bulk conditional removal based on key properties.
     *
     * <p>For each key that satisfies the predicate, all occurrences of all specified values
     * are removed from its collection. This is more efficient than calling removeOneIf multiple times.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putMany("fruits", Arrays.asList("apple", "banana", "apple", "orange"));
     * multimap.putMany("vegetables", Arrays.asList("carrot", "apple", "tomato"));
     * multimap.putMany("berries", Arrays.asList("strawberry", "apple"));
     * 
     * // Remove "apple" and "orange" from keys starting with "f"
     * Set<String> toRemove = Set.of("apple", "orange");
     * multimap.removeManyIf(toRemove, key -> key.startsWith("f"));
     * // "fruits" -> ["banana"], "vegetables" unchanged, "berries" unchanged
     * }</pre>
     *
     * <p><b>Note:</b> Empty collections are not added to the result. If all values are removed
     * from a key's collection, the key is removed from the Multimap.</p>
     *
     * @param values the collection of values to remove from matching collections
     * @param predicate the predicate that determines which keys to process
     * @return {@code true} if any values were removed from any collection,
     *         {@code false} if no changes were made (no keys matched, values not found, or values collection empty)
     * @throws NullPointerException if the predicate is null
     * @see #removeMany(Object, Collection)
     * @see #removeAllIf(Predicate)
     */
    public boolean removeManyIf(final Collection<?> values, final Predicate<? super K> predicate) {
        if (N.isEmpty(values)) {
            return false;
        }

        Set<K> removingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : removingKeys) {
            wasModified |= removeMany(k, values);
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified elements from the collections of values associated with keys that satisfy the specified predicate.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param values The collection of elements to be removed from the collections of values associated with the keys that satisfy the predicate.
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @see Collection#removeAll(Collection)
     */
    public boolean removeManyIf(final Collection<?> values, final BiPredicate<? super K, ? super V> predicate) {
        if (N.isEmpty(values)) {
            return false;
        }

        Set<K> removingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : removingKeys) {
            wasModified |= removeMany(k, values);
        }

        return wasModified;
    }

    /**
     * Removes all entries (keys and their associated value collections) where the key satisfies the given predicate.
     * This is a bulk removal operation for entire mappings based on key criteria.
     *
     * <p>This method is useful for filtering out entire key-value mappings based on key properties,
     * such as removing expired entries, inactive items, or entries matching certain patterns.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("user1", Arrays.asList(1, 2, 3));
     * multimap.putMany("admin1", Arrays.asList(4, 5));
     * multimap.putMany("user2", Arrays.asList(6, 7));
     * multimap.putMany("guest1", Arrays.asList(8));
     * 
     * // Remove all non-admin entries
     * multimap.removeAllIf(key -> !key.startsWith("admin"));
     * // Only "admin1" -> [4, 5] remains
     * 
     * // Remove entries with numeric suffix greater than 5
     * multimap.removeAllIf(key -> {
     *     String suffix = key.substring(key.length() - 1);
     *     return Integer.parseInt(suffix) > 5;
     * });
     * }</pre>
     *
     * @param predicate the predicate that determines which entries to remove
     * @return {@code true} if any entries were removed from the Multimap,
     *         {@code false} if no entries matched the predicate
     * @throws NullPointerException if the predicate is null
     * @see #removeAll(Object)
     * @see #filter(BiPredicate)
     */
    public boolean removeAllIf(final Predicate<? super K> predicate) {
        Set<K> removingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        for (final K k : removingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Removes all entries where both the key and its value collection satisfy the given predicate.
     * This method provides fine-grained control by considering both key and value collection properties.
     *
     * <p>Unlike {@link #removeAllIf(Predicate)}, this method passes both the key and its entire
     * value collection to the predicate, allowing decisions based on collection properties
     * such as size, specific contents, or other aggregate conditions.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("small", Arrays.asList(1, 2));
     * multimap.putMany("medium", Arrays.asList(1, 2, 3, 4));
     * multimap.putMany("large", Arrays.asList(1, 2, 3, 4, 5, 6));
     * 
     * // Remove entries with more than 3 values
     * multimap.removeAllIf((key, values) -> values.size() > 3);
     * // Only "small" -> [1, 2] remains
     * 
     * // Remove entries where key length equals collection size
     * multimap.removeAllIf((key, values) -> key.length() == values.size());
     * 
     * // Remove entries containing a specific value
     * multimap.removeAllIf((key, values) -> values.contains(42));
     * }</pre>
     *
     * @param predicate the bi-predicate that accepts a key and its value collection
     * @return {@code true} if any entries were removed, {@code false} if no entries matched
     * @throws NullPointerException if the predicate is null
     * @see #removeAllIf(Predicate)
     * @see #filter(BiPredicate)
     */
    public boolean removeAllIf(final BiPredicate<? super K, ? super V> predicate) {
        Set<K> removingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        for (final K k : removingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Replaces the first occurrence of an old value with a new value in the collection associated with the given key.
     * This method is useful for updating specific values while maintaining collection order and other elements.
     *
     * <p>For List-based multimaps, the replacement maintains the position of the replaced element.
     * For Set-based multimaps, the old value is removed and the new value is added (position not guaranteed).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putMany("colors", Arrays.asList("red", "blue", "red", "green"));
     * 
     * // Replace first "red" with "orange"
     * multimap.replaceOne("colors", "red", "orange");
     * // "colors" -> ["orange", "blue", "red", "green"]
     * 
     * // Try to replace non-existent value
     * boolean replaced = multimap.replaceOne("colors", "yellow", "purple");
     * // replaced is false, collection unchanged
     * 
     * // SetMultimap example
     * SetMultimap<String, Integer> setMap = N.newSetMultimap();
     * setMap.putMany("nums", Arrays.asList(1, 2, 3));
     * setMap.replaceOne("nums", 2, 20); // Removes 2, adds 20
     * }</pre>
     *
     * @param key the key whose value collection should be modified
     * @param oldValue the value to be replaced (can be null)
     * @param newValue the value to replace with (can be null)
     * @return {@code true} if the old value was found and replaced,
     *         {@code false} if the key doesn't exist or old value not found
     * @throws IllegalStateException if the collection rejects the new value
     *         (e.g., Set-based multimap where newValue already exists)
     * @see #replaceAllWithOne(Object, Object)
     * @see #replaceManyWithOne(Object, Collection, Object)
     */
    public boolean replaceOne(final K key, final E oldValue, final E newValue) throws IllegalStateException {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        return replaceOne(key, val, oldValue, newValue);
    }

    private boolean replaceOne(final K key, final V val, final E oldValue, final E newValue) {
        if (val instanceof List) {
            final List<E> list = (List<E>) val;

            if (list instanceof ArrayList) {
                if (oldValue == null) {
                    for (int i = 0, len = list.size(); i < len; i++) {
                        if (list.get(i) == null) {
                            list.set(i, newValue);
                            return true;
                        }
                    }
                } else {
                    for (int i = 0, len = list.size(); i < len; i++) {
                        if (oldValue.equals(list.get(i))) {
                            list.set(i, newValue);
                            return true;
                        }
                    }
                }
            } else {
                final ListIterator<E> iter = list.listIterator();

                if (oldValue == null) {
                    while (iter.hasNext()) {
                        if (iter.next() == null) {
                            iter.set(newValue);
                            return true;
                        }
                    }
                } else {
                    while (iter.hasNext()) {
                        if (oldValue.equals(iter.next())) {
                            iter.set(newValue);
                            return true;
                        }
                    }
                }
            }
        } else {
            if (val.remove(oldValue)) {
                addNewValueForReplacement(key, val, newValue);

                return true;
            }
        }

        return false;
    }

    private void addNewValueForReplacement(final K key, final V val, final E newValue) {
        if (!val.add(newValue)) {
            throw new IllegalStateException("Failed to add the new value: " + newValue + " for key: " + key + " for replacement");
        }
    }

    /**
     * Replaces all values associated with the specified key with a single new value.
     * This is a bulk replacement operation that clears the existing collection and adds one new value.
     *
     * <p>This method is atomic in the sense that the collection is cleared and the new value
     * is added in one operation. If adding the new value fails, the collection will be empty.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("scores", Arrays.asList(85, 92, 78, 88, 91));
     * 
     * // Replace all scores with a single average
     * multimap.replaceAllWithOne("scores", 87);
     * // "scores" -> [87]
     * 
     * // Consolidate multiple statuses to a single status
     * multimap.putMany("status", Arrays.asList("pending", "processing", "pending"));
     * multimap.replaceAllWithOne("status", "completed");
     * // "status" -> ["completed"]
     * 
     * // No effect on non-existent key
     * boolean replaced = multimap.replaceAllWithOne("missing", 100);
     * // replaced is false
     * }</pre>
     *
     * @param key the key whose entire value collection should be replaced
     * @param newValue the single value that will replace all existing values
     * @return {@code true} if the key existed and replacement was performed,
     *         {@code false} if the key was not present in the Multimap
     * @throws IllegalStateException if the new value cannot be added to the collection
     *         (this is rare but could happen with custom collection implementations)
     * @see #replaceOne(Object, Object, Object)
     * @see #put(Object, Object)
     */
    public boolean replaceAllWithOne(final K key, final E newValue) throws IllegalStateException {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        val.clear();

        addNewValueForReplacement(key, val, newValue);

        return true;
    }

    //    private boolean replaceAllWithOne(final V val, final E oldValue, final E newValue) {
    //        boolean wasModified = false;
    //
    //        if (val instanceof List) {
    //            final List<E> list = (List<E>) val;
    //
    //            if (list instanceof ArrayList) {
    //                if (oldValue == null) {
    //                    for (int i = 0, len = list.size(); i < len; i++) {
    //                        if (list.get(i) == null) {
    //                            list.set(i, newValue);
    //                            wasModified = true;
    //                        }
    //                    }
    //                } else {
    //                    for (int i = 0, len = list.size(); i < len; i++) {
    //                        if (oldValue.equals(list.get(i))) {
    //                            list.set(i, newValue);
    //                            wasModified = true;
    //                        }
    //                    }
    //                }
    //            } else {
    //                final ListIterator<E> iter = list.listIterator();
    //
    //                if (oldValue == null) {
    //                    while (iter.hasNext()) {
    //                        if (iter.next() == null) {
    //                            iter.set(newValue);
    //                            wasModified = true;
    //                        }
    //                    }
    //                } else {
    //                    while (iter.hasNext()) {
    //                        if (oldValue.equals(iter.next())) {
    //                            iter.set(newValue);
    //                            wasModified = true;
    //                        }
    //                    }
    //                }
    //            }
    //        } else {
    //            final Object[] tmp = val.toArray();
    //            wasModified = N.replaceAll(tmp, oldValue, newValue) > 0;
    //            val.clear();
    //            val.addAll(Arrays.asList((E[]) tmp));
    //        }
    //
    //        return wasModified;
    //    }

    /**
     * Replaces all occurrences of multiple old values with a single new value for the given key.
     * This is useful for consolidating or normalizing multiple values into one.
     *
     * <p>This method removes ALL occurrences of ALL specified old values and replaces them
     * with a SINGLE instance of the new value. This is different from one-to-one replacement.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putMany("status", Arrays.asList("pending", "waiting", "pending", "on-hold", "waiting"));
     * 
     * // Replace all "pending" and "waiting" with "in-progress"
     * Set<String> oldStatuses = Set.of("pending", "waiting", "on-hold");
     * multimap.replaceManyWithOne("status", oldStatuses, "in-progress");
     * // "status" -> ["in-progress"] (single value)
     * 
     * // Consolidate multiple error codes
     * multimap.putMany("errors", Arrays.asList("E001", "E002", "E001", "E003"));
     * multimap.replaceManyWithOne("errors", Arrays.asList("E001", "E002"), "E_GENERIC");
     * // "errors" -> ["E_GENERIC", "E003"]
     * }</pre>
     *
     * <p><b>Note:</b> The new value is added only once, regardless of how many old values were removed.</p>
     *
     * @param key the key whose value collection should be modified
     * @param oldValues the collection of values to be removed and replaced
     * @param newValue the single value that will replace all removed values
     * @return {@code true} if at least one old value was found and removed,
     *         {@code false} if the key doesn't exist or none of the old values were found
     * @throws IllegalStateException if the new value cannot be added to the collection
     * @see #replaceOne(Object, Object, Object)
     * @see #replaceAllWithOne(Object, Object)
     */
    public boolean replaceManyWithOne(final K key, final Collection<? extends E> oldValues, final E newValue) throws IllegalStateException {
        if (N.isEmpty(oldValues)) {
            return false;
        }

        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        if (val.removeAll(oldValues)) {
            addNewValueForReplacement(key, val, newValue);

            return true;
        }

        return false;
    }

    /**
     * Replaces one occurrence of an old value with a new value in all collections where the key matches the predicate.
     * This allows targeted replacement across multiple keys based on key properties.
     *
     * <p>For each key that satisfies the predicate, this method will replace only the FIRST
     * occurrence of the old value in that key's collection. Different keys may have the
     * replacement happen at different positions.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("user_123", Arrays.asList(99, 85, 99, 92));
     * multimap.putMany("user_456", Arrays.asList(78, 99, 88));
     * multimap.putMany("admin_001", Arrays.asList(99, 99, 95));
     * 
     * // Replace first occurrence of 99 with 100 for all user keys
     * multimap.replaceOneIf(key -> key.startsWith("user_"), 99, 100);
     * // "user_123" -> [100, 85, 99, 92]
     * // "user_456" -> [78, 100, 88]
     * // "admin_001" -> [99, 99, 95] (unchanged)
     * 
     * // Update default timeout value for test environments
     * multimap.replaceOneIf(key -> key.contains("test"), 30, 60);
     * }</pre>
     *
     * @param predicate the predicate to test each key
     * @param oldValue the value to be replaced in matching collections
     * @param newValue the replacement value
     * @return {@code true} if at least one replacement was made,
     *         {@code false} if no keys matched or old value was not found
     * @throws IllegalStateException if the new value cannot be added to any collection
     * @throws NullPointerException if the predicate is null
     * @see #replaceOne(Object, Object, Object)
     * @see #replaceAllWithOneIf(Predicate, Object)
     */
    public boolean replaceOneIf(final Predicate<? super K> predicate, final E oldValue, final E newValue) throws IllegalStateException {
        boolean wasModified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                wasModified |= replaceOne(entry.getKey(), entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return wasModified;
    }

    /**
     * Replaces a single occurrence of the specified old value with the new value for keys that satisfy the specified predicate in this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to replace a single occurrence of the old value with the new value.
     * If the old value is successfully replaced, the method returns {@code true}. If the old value is not found in the collection of values associated with the key, the method returns {@code false}.
     *
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @param oldValue The old value to be replaced in the collections of values associated with the keys that satisfy the predicate.
     * @param newValue The new value to replace the old value in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
     */
    public boolean replaceOneIf(final BiPredicate<? super K, ? super V> predicate, final E oldValue, final E newValue) throws IllegalStateException {
        boolean wasModified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                wasModified |= replaceOne(entry.getKey(), entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return wasModified;
    }

    /**
     * Replaces all occurrences of the specified old values with the new value for keys that satisfy the specified predicate in this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove all occurrences of the old values.
     * If the old values are successfully removed, the method adds the new value to the collection. If none of the old values are found in the collection of values associated with the key, the method does nothing for that key.
     *
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @param oldValues The collection of old values to be replaced in the collections of values associated with the keys that satisfy the predicate.
     * @param newValue The new value to replace all the old values in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
     */
    public boolean replaceManyWithOneIf(final Predicate<? super K> predicate, final Collection<? extends E> oldValues, final E newValue)
            throws IllegalStateException {
        boolean wasModified = false;
        V val = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            val = entry.getValue();

            if (predicate.test(entry.getKey()) && val.removeAll(oldValues)) {
                addNewValueForReplacement(entry.getKey(), val, newValue);

                wasModified = true;
            }
        }

        return wasModified;
    }

    /**
     * Replaces all occurrences of the specified old values with the new value for keys that satisfy the specified predicate in this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove all occurrences of the old values.
     * If the old values are successfully removed, the method adds the new value to the collection. If none of the old values are found in the collection of values associated with the key, the method does nothing for that key.
     *
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @param oldValues The collection of old values to be replaced in the collections of values associated with the keys that satisfy the predicate.
     * @param newValue The new value to replace all the old values in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
     */
    public boolean replaceManyWithOneIf(final BiPredicate<? super K, ? super V> predicate, final Collection<? extends E> oldValues, final E newValue)
            throws IllegalStateException {
        boolean wasModified = false;
        V val = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            val = entry.getValue();

            if (predicate.test(entry.getKey(), val) && val.removeAll(oldValues)) {
                addNewValueForReplacement(entry.getKey(), val, newValue);

                wasModified = true;
            }
        }

        return wasModified;
    }

    /**
     * Replaces all values associated with keys that satisfy the specified predicate in this Multimap with the new value.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and clears it.
     * Then, it adds the new value to the now empty collection. If the key is not present in the Multimap,
     * or the predicate returns {@code false} for a key, the method does nothing for that key.
     *
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @param newValue The new value to replace all the old values in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
     */
    public boolean replaceAllWithOneIf(final Predicate<? super K> predicate, final E newValue) throws IllegalStateException {
        boolean wasModified = false;
        V val = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                val = entry.getValue();
                val.clear();

                addNewValueForReplacement(entry.getKey(), val, newValue);

                wasModified = true;
            }
        }

        return wasModified;
    }

    /**
     * Replaces all values associated with keys that satisfy the specified predicate in this Multimap with the new value.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and clears it.
     * Then, it adds the new value to the now empty collection. If the key is not present in the Multimap,
     * or the predicate returns {@code false} for a key, the method does nothing for that key.
     *
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @param newValue The new value to replace all the old values in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throws IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
     */
    public boolean replaceAllWithOneIf(final BiPredicate<? super K, ? super V> predicate, final E newValue) throws IllegalStateException {
        boolean wasModified = false;
        V val = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            val = entry.getValue();

            if (predicate.test(entry.getKey(), val)) {
                val.clear();

                addNewValueForReplacement(entry.getKey(), val, newValue);

                wasModified = true;
            }
        }

        return wasModified;
    }

    /**
     * Replaces all values associated with each key in this Multimap according to the provided function.
     *
     * This method iterates over the keys in the Multimap and applies the function to each key-value pair.
     * The function should return a new collection of values that will replace the old collection associated with the key.
     * If the function returns {@code null} or an empty collection, the key is removed from the Multimap.
     *
     * @param function The function to be applied to each key-value pair in the Multimap. It should return a new collection of values.
     * @throws IllegalStateException if the new collection of values cannot be added to the Multimap.
     */
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) throws IllegalStateException {
        List<K> keyToRemove = null;
        V value = null;
        V newValue = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            value = entry.getValue();

            newValue = function.apply(entry.getKey(), value);

            if (newValue == value) {
                // continue.
            } else if (N.isEmpty(newValue)) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else {
                value.clear();

                if (!value.addAll(newValue)) {
                    throw new IllegalStateException("Failed to add the new value: " + newValue + " for key: " + entry.getKey() + " for replacement");
                }
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (final K key : keyToRemove) {
                backingMap.remove(key);
            }
        }
    }

    /**
     * Computes and associates a value collection for the specified key if it's not already present.
     * This method implements lazy initialization for Multimap entries, computing values only when needed.
     *
     * <p>If the key already has an associated value collection, that existing collection is returned
     * without calling the mapping function. This makes the method safe to call repeatedly.</p>
     *
     * <p>If the mapping function returns null or an empty collection, no entry is created,
     * and the method returns null.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * 
     * // Initialize default permissions for new users
     * multimap.computeIfAbsent("user123", 
     *     key -> new ArrayList<>(Arrays.asList("read", "write")));
     * 
     * // Won't overwrite existing values
     * multimap.put("admin", "super");
     * multimap.computeIfAbsent("admin", 
     *     key -> Arrays.asList("basic")); // Returns existing ["super"]
     * 
     * // Compute based on key properties
     * multimap.computeIfAbsent("guest_temp", key -> {
     *     if (key.startsWith("guest_")) {
     *         return Arrays.asList("read-only");
     *     }
     *     return Arrays.asList("read", "write");
     * });
     * }</pre>
     *
     * <p><b>Thread Safety:</b> This operation is not atomic. External synchronization is required
     * for concurrent access.</p>
     *
     * @param key the key for which to compute an initial value collection
     * @param mappingFunction the function to compute the initial collection, called only if key is absent
     * @return the existing collection if key was present, or the newly computed and stored collection,
     *         or null if the mapping function returned null/empty
     * @throws IllegalArgumentException if the mappingFunction is null
     * @see #computeIfPresent(Object, BiFunction)
     * @see #compute(Object, BiFunction)
     */
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(mappingFunction);

        final V oldValue = get(key);

        if (oldValue != null) {
            return oldValue;
        }

        final V newValue = mappingFunction.apply(key);

        if (N.notEmpty(newValue)) {
            putMany(key, newValue);
        }

        return get(key);
    }

    /**
     * Updates the value collection for the specified key using a remapping function, but only if the key is present.
     * This method allows conditional transformation of existing collections without affecting absent keys.
     *
     * <p>If the key is not present, this method does nothing and returns null.</p>
     *
     * <p>If the remapping function returns null or an empty collection, the entire mapping
     * (key and its collection) is removed from the Multimap.</p>
     *
     * <p>The remapping function receives both the key and its current collection, allowing
     * transformations based on both key properties and current values.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("scores", Arrays.asList(85, 92, 78, 95, 88));
     * multimap.putMany("grades", Arrays.asList(75, 82));
     * 
     * // Transform scores above threshold to 100
     * multimap.computeIfPresent("scores", (key, values) -> {
     *     return values.stream()
     *         .map(score -> score >= 90 ? 100 : score)
     *         .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
     * });
     * // "scores" -> [85, 100, 78, 100, 88]
     * 
     * // Remove collections with fewer than 3 elements
     * multimap.computeIfPresent("grades", (key, values) -> {
     *     return values.size() >= 3 ? values : null;
     * });
     * // "grades" is removed from multimap
     * 
     * // No effect on absent keys
     * multimap.computeIfPresent("absent", (key, values) -> new ArrayList<>());
     * // No change
     * }</pre>
     *
     * @param key the key whose value collection should be remapped
     * @param remappingFunction the function that computes a new collection based on key and current collection
     * @return the updated collection, or null if the key was not present or the function returned null/empty
     * @throws IllegalArgumentException if the remappingFunction is null
     * @see #computeIfAbsent(Object, Function)
     * @see #compute(Object, BiFunction)
     */
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = get(key);

        if (oldValue == null) {
            //noinspection ConstantValue
            return oldValue;
        }

        V ret = null;
        final V newValue = remappingFunction.apply(key, oldValue);

        if (newValue == oldValue) {
            // continue.
        } else if (N.notEmpty(newValue)) {
            oldValue.clear();
            oldValue.addAll(newValue);

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     * Computes the value for the specified key using the given remapping function.
     *
     * This method first retrieves the current value associated with the specified key. Then, it applies the remapping function to the key and its current associated value,
     * and updates the key's associated value in the Multimap with the result. If the remapping function returns {@code null} or an empty collection,
     * the key is removed from the Multimap.
     *
     * If the key is not already associated with a value in the Multimap, this method associates the key with the new value returned by the remapping function.
     *
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * V ret = null;
     * final V oldValue = get(key);
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue == null) {
     *          putMany(key, newValue);
     *          ret = get(key);
     *      } else {
     *          if (oldValue != newValue) {
     *              oldValue.clear();
     *              oldValue.addAll(newValue);
     *           }
     *
     *           ret = oldValue;
     *      }
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return ret;
     * </pre>
     *
     * @param key The key whose associated value is to be computed.
     * @param remappingFunction The function to compute a value.
     * @return The new value associated with the specified key, or {@code null} if the remapping function returns {@code null} or an empty collection.
     * @throws IllegalArgumentException if the remappingFunction is {@code null}.
     */
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        V ret = null;
        final V oldValue = get(key);
        final V newValue = remappingFunction.apply(key, oldValue);

        if (newValue == oldValue) {
            // continue.
        } else if (N.notEmpty(newValue)) {
            if (oldValue == null) {
                putMany(key, newValue);
                ret = get(key);
            } else {
                oldValue.clear();
                oldValue.addAll(newValue);

                ret = oldValue;
            }
        } else if (oldValue != null) {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     * Merges the given collection of elements with the current values associated with the specified key in this Multimap.
     *
     * This method first retrieves the current value associated with the specified key. If the key is not already associated with a value,
     * it associates the key with the given collection of elements.
     *
     * If the key is already associated with a value, it applies the remapping function to the current value and the given collection of elements,
     * and updates the key's associated value in the Multimap with the result. If the remapping function returns {@code null} or an empty collection,
     * the key is removed from the Multimap.
     *
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (oldValue == null) {
     *     putMany(key, elements);
     *     return get(key);
     * }
     *
     * V ret = null;
     * final V newValue = remappingFunction.apply(oldValue, elements);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue != newValue) {
     *          oldValue.clear();
     *          oldValue.addAll(newValue);
     *      }
     *
     *      ret = oldValue;
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return ret;
     * </pre>
     *
     * @param key The key whose associated value is to be computed.
     * @param elements The collection of elements to be merged with the current values associated with the key.
     * @param remappingFunction The function to compute a value.
     * @return The new value associated with the specified key, or {@code null} if the remapping function returns {@code null} or an empty collection.
     * @throws IllegalArgumentException if the remappingFunction is {@code null}.
     */
    public <C extends Collection<? extends E>> V merge(final K key, final C elements, final BiFunction<? super V, ? super C, ? extends V> remappingFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(elements);

        final V oldValue = get(key);

        if (oldValue == null) {
            putMany(key, elements);
            return get(key);
        }

        V ret = null;
        final V newValue = remappingFunction.apply(oldValue, elements);

        if (newValue == oldValue) {
            // continue.
        } else if (N.notEmpty(newValue)) {
            oldValue.clear();
            oldValue.addAll(newValue);

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     * Merges the given element with the current values associated with the specified key in this Multimap.
     *
     * This method first retrieves the current value associated with the specified key. If the key is not already associated with a value,
     * it associates the key with the given element.
     *
     * If the key is already associated with a value, it applies the remapping function to the current value and the given element,
     * and updates the key's associated value in the Multimap with the result. If the remapping function returns {@code null} or an empty collection,
     * the key is removed from the Multimap.
     *
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (oldValue == null) {
     *     put(key, e);
     *     return get(key);
     * }
     *
     * V ret = null;
     * final V newValue = remappingFunction.apply(oldValue, e);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue != newValue) {
     *          oldValue.clear();
     *          oldValue.addAll(newValue);
     *      }
     *
     *      ret = oldValue;
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return ret;
     * </pre>
     *
     * @param key The key whose associated value is to be computed.
     * @param e The element to be merged with the current values associated with the key.
     * @param remappingFunction The function to compute a value.
     * @return The new value associated with the specified key, or {@code null} if the remapping function returns {@code null} or an empty collection.
     * @throws IllegalArgumentException if the remappingFunction is {@code null}.
     */
    public V merge(final K key, final E e, final BiFunction<? super V, ? super E, ? extends V> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(e);

        final V oldValue = get(key);

        if (oldValue == null) {
            put(key, e);
            return get(key);
        }

        V ret = null;
        final V newValue = remappingFunction.apply(oldValue, e);

        if (newValue == oldValue) {
            // continue.
        } else if (N.notEmpty(newValue)) {
            oldValue.clear();
            oldValue.addAll(newValue);

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     * Creates an inverted Multimap where keys and values are swapped.
     * This operation transforms a Multimap&lt;K, E, V&gt; into a Multimap&lt;E, K, VV&gt;.
     *
     * <p>In the resulting Multimap:</p>
     * <ul>
     *   <li>Each value from the original collections becomes a key</li>
     *   <li>Each original key becomes a value in the new collections</li>
     *   <li>If multiple keys had the same value, they all appear in that value's collection</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> original = N.newListMultimap();
     * original.putMany("group1", Arrays.asList(1, 2, 3));
     * original.putMany("group2", Arrays.asList(2, 4));
     * original.put("group3", 1);
     * 
     * // Invert: numbers become keys, groups become values
     * ListMultimap<Integer, String> inverted = original.inverse(N::newListMultimap);
     * // Result:
     * // 1 -> ["group1", "group3"]
     * // 2 -> ["group1", "group2"]
     * // 3 -> ["group1"]
     * // 4 -> ["group2"]
     * 
     * // Different Multimap types
     * SetMultimap<Integer, String> invertedSet = original.inverse(N::newSetMultimap);
     * }</pre>
     *
     * <p><b>Performance:</b> This operation has O(n) complexity where n is the total number
     * of key-value pairs across all collections.</p>
     *
     * @param <VV> the collection type for values in the inverted Multimap
     * @param <M> the specific Multimap type to create
     * @param multimapSupplier factory function that creates the target Multimap type,
     *                         receives the size of this Multimap as a hint
     * @return a new inverted Multimap with keys and values swapped
     * @throws NullPointerException if multimapSupplier is null
     * @see #copy()
     */
    public <VV extends Collection<K>, M extends Multimap<E, K, VV>> M inverse(final IntFunction<? extends M> multimapSupplier) {
        final Multimap<K, E, V> multimap = this;
        final M res = multimapSupplier.apply(multimap.size());

        if (N.notEmpty(multimap)) {
            for (final Map.Entry<K, V> entry : multimap.entrySet()) {
                final V v = entry.getValue();

                if (N.notEmpty(v)) {
                    for (final E e : v) {
                        res.put(e, entry.getKey());
                    }
                }
            }
        }

        return res;
    }

    /**
     * Creates a shallow copy of this Multimap with the same structure and contents.
     * The new Multimap is independent - modifications to either won't affect the other.
     *
     * <p>This is a shallow copy, meaning:</p>
     * <ul>
     *   <li>The Multimap structure is duplicated</li>
     *   <li>New collections are created for each key</li>
     *   <li>The individual elements themselves are not cloned</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, String> original = N.newListMultimap();
     * original.putMany("colors", Arrays.asList("red", "blue", "green"));
     * 
     * ListMultimap<String, String> copy = original.copy();
     * copy.put("colors", "yellow");  // Only affects the copy
     * 
     * original.size(); // Still 1 key
     * copy.size();     // Still 1 key, but different collections
     * }</pre>
     *
     * @return a new Multimap containing the same key-value mappings as this one
     * @see #inverse(IntFunction)
     * @see #filter(BiPredicate)
     */
    public Multimap<K, E, V> copy() {
        final Multimap<K, E, V> copy = new Multimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Tests whether this Multimap contains the specified key-value pair.
     * This method checks for exact key-value associations rather than just key or value presence.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 1);
     * multimap.put("numbers", 2);
     * multimap.put("letters", null);
     * 
     * multimap.contains("numbers", 1);    // Returns true
     * multimap.contains("numbers", 3);    // Returns false
     * multimap.contains("missing", 1);    // Returns false
     * multimap.contains("letters", null); // Returns true (null values supported)
     * }</pre>
     *
     * @param key the key to check for
     * @param e the value to check for in the key's collection
     * @return {@code true} if the key exists and its collection contains the specified value,
     *         {@code false} otherwise
     * @see #containsKey(Object)
     * @see #containsValue(Object)
     * @see #containsAll(Object, Collection)
     */
    public boolean contains(final Object key, final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        return val != null && val.contains(e);
    }

    /**
     * Tests whether this Multimap contains the specified key.
     * Returns true even if the key is associated with an empty collection.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 42);
     * 
     * multimap.containsKey("numbers");  // Returns true
     * multimap.containsKey("missing");  // Returns false
     * multimap.containsKey(null);       // Depends on backing map (usually false)
     * }</pre>
     *
     * @param key the key to check for
     * @return {@code true} if this Multimap contains the specified key, {@code false} otherwise
     * @see #containsValue(Object)
     * @see #contains(Object, Object)
     */
    public boolean containsKey(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(key);
    }

    /**
     * Tests whether this Multimap contains the specified value in any of its collections.
     * This method searches through all value collections to find the specified element.
     *
     * <p><b>Performance:</b> This operation is O(n) where n is the total number of values
     * across all collections, as it must check each collection.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("group1", 10);
     * multimap.put("group2", 20);
     * multimap.put("group1", null);
     * 
     * multimap.containsValue(10);   // Returns true
     * multimap.containsValue(30);   // Returns false
     * multimap.containsValue(null); // Returns true (null values supported)
     * }</pre>
     *
     * @param e the value to search for across all collections
     * @return {@code true} if any collection in this Multimap contains the specified value,
     *         {@code false} if not found or Multimap is empty
     * @see #containsKey(Object)
     * @see #contains(Object, Object)
     */
    public boolean containsValue(final Object e) {
        final Collection<V> values = values();

        for (final V val : values) {
            //noinspection SuspiciousMethodCalls
            if (val.contains(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether the collection associated with the specified key contains all elements
     * from the given collection. This is equivalent to subset testing.
     *
     * <p>An empty collection is always considered to be contained by any collection,
     * so this method returns {@code true} for empty input collections if the key exists.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("numbers", Arrays.asList(1, 2, 3, 4, 5));
     * 
     * multimap.containsAll("numbers", Arrays.asList(1, 3, 5));     // Returns true
     * multimap.containsAll("numbers", Arrays.asList(1, 6));        // Returns false
     * multimap.containsAll("numbers", Collections.emptyList());    // Returns true
     * multimap.containsAll("missing", Arrays.asList(1, 2));       // Returns false
     * 
     * // Works with duplicates in the test collection
     * multimap.containsAll("numbers", Arrays.asList(1, 1, 2));    // Returns true
     * }</pre>
     *
     * @param key the key whose collection should be checked
     * @param c the collection of elements to test for containment
     * @return {@code true} if the key exists and its collection contains all elements from c,
     *         {@code true} if c is empty and key exists,
     *         {@code false} if key doesn't exist or any element from c is missing
     * @see Collection#containsAll(Collection)
     * @see #contains(Object, Object)
     */
    public boolean containsAll(final Object key, final Collection<?> c) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        return val != null && (N.isEmpty(c) || val.containsAll(c));
    }

    /**
     * Creates a new Multimap containing only entries where the key satisfies the given predicate.
     * This is a non-destructive filtering operation that preserves the original Multimap.
     *
     * <p>The filtering is based solely on keys - if a key passes the filter, its entire
     * value collection is included in the result.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("user123", Arrays.asList(1, 2, 3));
     * multimap.putMany("admin456", Arrays.asList(4, 5));
     * multimap.putMany("guest789", Arrays.asList(6));
     * 
     * // Filter for admin and user accounts only
     * Multimap<String, Integer, ?> filtered = multimap.filterByKey(
     *     key -> key.startsWith("admin") || key.startsWith("user"));
     * // Result contains "user123" and "admin456" with their complete collections
     * 
     * // Filter by key length
     * Multimap<String, Integer, ?> shortKeys = multimap.filterByKey(key -> key.length() < 8);
     * }</pre>
     *
     * @param filter the predicate to test each key
     * @return a new Multimap containing only entries whose keys satisfy the predicate
     * @throws NullPointerException if filter is null
     * @see #filterByValue(Predicate)
     * @see #filter(BiPredicate)
     */
    public Multimap<K, E, V> filterByKey(final Predicate<? super K> filter) {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Creates a new Multimap containing only entries where the value collection satisfies the given predicate.
     * This allows filtering based on collection properties like size, contents, or type.
     *
     * <p>The filtering is applied to entire value collections - if a collection passes the filter,
     * the entire key-collection mapping is included in the result.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("small", Arrays.asList(1, 2));
     * multimap.putMany("large", Arrays.asList(1, 2, 3, 4, 5));
     * multimap.putMany("empty", Collections.emptyList());
     * 
     * // Filter for collections with more than 2 elements
     * Multimap<String, Integer, ?> bigCollections = multimap.filterByValue(
     *     values -> values.size() > 2);
     * // Result contains only "large" key
     * 
     * // Filter for non-empty collections
     * Multimap<String, Integer, ?> nonEmpty = multimap.filterByValue(
     *     values -> !values.isEmpty());
     * 
     * // Filter by collection contents
     * Multimap<String, Integer, ?> containsOne = multimap.filterByValue(
     *     values -> values.contains(1));
     * }</pre>
     *
     * @param filter the predicate to test each value collection
     * @return a new Multimap containing only entries whose value collections satisfy the predicate
     * @throws NullPointerException if filter is null
     * @see #filterByKey(Predicate)
     * @see #filter(BiPredicate)
     */
    public Multimap<K, E, V> filterByValue(final Predicate<? super V> filter) {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (filter.test(entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Creates a new Multimap containing only entries where both the key and value collection
     * satisfy the given bi-predicate. This provides the most flexible filtering option.
     *
     * <p>The bi-predicate receives both the key and its entire value collection,
     * allowing complex filtering logic based on the relationship between keys and their values.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("scores_2023", Arrays.asList(85, 92, 78));
     * multimap.putMany("scores_2022", Arrays.asList(90, 88));
     * multimap.putMany("temp_data", Arrays.asList(1, 2, 3, 4, 5));
     * 
     * // Filter recent years with good average scores
     * Multimap<String, Integer, ?> goodRecentScores = multimap.filter(
     *     (key, values) -> key.contains("2023") && 
     *                      values.stream().mapToInt(Integer::intValue).average().orElse(0) > 80);
     * 
     * // Filter by key length and collection size relationship
     * Multimap<String, Integer, ?> balanced = multimap.filter(
     *     (key, values) -> key.length() > values.size());
     * 
     * // Complex business logic
     * Multimap<String, Integer, ?> filtered = multimap.filter(
     *     (key, values) -> !key.startsWith("temp_") && 
     *                      !values.isEmpty() && 
     *                      values.stream().allMatch(v -> v > 0));
     * }</pre>
     *
     * @param filter the bi-predicate to test each key-value collection pair
     * @return a new Multimap containing only entries that satisfy the bi-predicate
     * @throws NullPointerException if filter is null
     * @see #filterByKey(Predicate)
     * @see #filterByValue(Predicate)
     */
    public Multimap<K, E, V> filter(final BiPredicate<? super K, ? super V> filter) {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Performs the given action for each key-value pair in the Multimap.
     *
     * This method iterates over all key-value pairs in the Multimap and applies the provided BiConsumer action.
     * The action should be a function that accepts a key and a value, and performs some operation.
     * The action is performed in the order of the iteration if that order is specified.
     * <br />
     * Usually, the value collection should not be modified outside this Multimap directly by the specified action, because it may cause unexpected behavior.
     *
     * @param action The action to be performed for each key-value pair in the Multimap.
     * @throws IllegalArgumentException if the provided action is {@code null}.
     */
    public void forEach(final BiConsumer<? super K, ? super V> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Performs the given action for each individual key-element pair in this Multimap.
     * This method "flattens" the Multimap structure, treating each element separately rather than as collections.
     *
     * <p>Unlike {@link #forEach(BiConsumer)}, which operates on key-collection pairs,
     * this method operates on key-element pairs, iterating through every individual element.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("group1", Arrays.asList(1, 2, 3));
     * multimap.putMany("group2", Arrays.asList(4, 5));
     * 
     * // Process each key-element pair individually
     * multimap.flatForEach((key, element) -> {
     *     System.out.println(key + " contains " + element);
     * });
     * // Output:
     * // group1 contains 1
     * // group1 contains 2
     * // group1 contains 3
     * // group2 contains 4
     * // group2 contains 5
     * 
     * // Collect all values with their keys
     * Map<String, List<Integer>> collected = new HashMap<>();
     * multimap.flatForEach((key, element) -> 
     *     collected.computeIfAbsent(key, k -> new ArrayList<>()).add(element));
     * }</pre>
     *
     * <p><b>Performance:</b> This method processes every individual element, so the time complexity
     * is O(n) where n is the total number of elements across all collections.</p>
     *
     * @param action the action to be performed for each key-element pair
     * @throws IllegalArgumentException if action is null
     * @see #forEach(BiConsumer)
     * @see #flatForEachValue(Consumer)
     */
    @Beta
    public void flatForEach(final BiConsumer<? super K, ? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        K key = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            key = entry.getKey();

            for (final E e : entry.getValue()) {
                action.accept(key, e);
            }
        }
    }

    /**
     * Performs the given action for each key in this Multimap.
     * This method provides a convenient way to iterate over just the keys without accessing their values.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("user123", 1);
     * multimap.put("admin456", 2);
     * multimap.put("guest789", 3);
     * 
     * // Process each key
     * multimap.forEachKey(key -> {
     *     System.out.println("Processing key: " + key);
     *     if (key.startsWith("admin")) {
     *         // Special handling for admin keys
     *     }
     * });
     * 
     * // Collect key statistics
     * Map<String, Integer> keyLengths = new HashMap<>();
     * multimap.forEachKey(key -> keyLengths.put(key, key.length()));
     * }</pre>
     *
     * <p><b>Performance:</b> O(k) where k is the number of keys in the Multimap.</p>
     *
     * @param action the action to be performed for each key
     * @throws IllegalArgumentException if action is null
     * @see #forEach(BiConsumer)
     * @see #keySet()
     */
    @Beta
    public void forEachKey(final Consumer<? super K> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final K k : backingMap.keySet()) {
            action.accept(k);
        }
    }

    /**
     * Performs the given action for each value collection in this Multimap.
     * This method operates on entire collections rather than individual elements.
     *
     * <p><b>Warning:</b> The value collections should generally not be modified directly
     * through this method, as it may cause unexpected behavior. Use Multimap's own
     * modification methods instead.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("group1", Arrays.asList(1, 2, 3));
     * multimap.putMany("group2", Arrays.asList(4, 5));
     * multimap.putMany("group3", Arrays.asList());
     * 
     * // Analyze each collection
     * multimap.forEachValue(collection -> {
     *     System.out.println("Collection size: " + collection.size());
     *     if (!collection.isEmpty()) {
     *         System.out.println("First element: " + collection.iterator().next());
     *     }
     * });
     * 
     * // Count non-empty collections
     * AtomicInteger nonEmptyCount = new AtomicInteger(0);
     * multimap.forEachValue(collection -> {
     *     if (!collection.isEmpty()) {
     *         nonEmptyCount.incrementAndGet();
     *     }
     * });
     * }</pre>
     *
     * <p><b>Performance:</b> O(k) where k is the number of keys in the Multimap.</p>
     *
     * @param action the action to be performed for each value collection
     * @throws IllegalArgumentException if action is null
     * @see #forEach(BiConsumer)
     * @see #flatForEachValue(Consumer)
     * @see #values()
     */
    @Beta
    public void forEachValue(final Consumer<? super V> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final V v : backingMap.values()) {
            action.accept(v);
        }
    }

    /**
     * Performs the given action for each individual element across all collections in this Multimap.
     * This method "flattens" the structure, processing every element regardless of which key it belongs to.
     *
     * <p>This is useful when you need to process all values uniformly without caring about
     * their associated keys.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("evens", Arrays.asList(2, 4, 6));
     * multimap.putMany("odds", Arrays.asList(1, 3, 5));
     * multimap.putMany("primes", Arrays.asList(2, 3, 5, 7));
     * 
     * // Process all numbers regardless of category
     * List<Integer> allNumbers = new ArrayList<>();
     * multimap.flatForEachValue(allNumbers::add);
     * 
     * // Calculate statistics across all values
     * AtomicInteger sum = new AtomicInteger(0);
     * AtomicInteger count = new AtomicInteger(0);
     * multimap.flatForEachValue(value -> {
     *     sum.addAndGet(value);
     *     count.incrementAndGet();
     * });
     * double average = (double) sum.get() / count.get();
     * 
     * // Validation across all elements
     * multimap.flatForEachValue(value -> {
     *     if (value < 0) {
     *         throw new IllegalArgumentException("Negative value found: " + value);
     *     }
     * });
     * }</pre>
     *
     * <p><b>Performance:</b> O(n) where n is the total number of elements across all collections.</p>
     *
     * @param action the action to be performed for each element
     * @throws IllegalArgumentException if action is null
     * @see #flatForEach(BiConsumer)
     * @see #forEachValue(Consumer)
     * @see #flatValues()
     */
    @Beta
    public void flatForEachValue(final Consumer<? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final V v : backingMap.values()) {
            for (final E e : v) {
                action.accept(e);
            }
        }
    }

    /**
     * Returns a Set view of the keys contained in this Multimap.
     * The returned set is backed by the Multimap, so changes to the Multimap are reflected in the set.
     *
     * <p><b>Live View:</b> The returned set is a live view - structural changes to the Multimap
     * (like adding/removing keys) will be visible in this set, but modifying the set directly
     * is not supported.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("key1", 1);
     * multimap.put("key2", 2);
     * 
     * Set<String> keys = multimap.keySet();
     * System.out.println(keys.size()); // 2
     * System.out.println(keys.contains("key1")); // true
     * 
     * // Live view - changes reflect
     * multimap.put("key3", 3);
     * System.out.println(keys.size()); // 3
     * 
     * // Iteration
     * for (String key : keys) {
     *     Collection<?> values = multimap.get(key);
     *     System.out.println(key + " has " + values.size() + " values");
     * }
     * }</pre>
     *
     * @return a Set view of the keys contained in this Multimap
     * @see #values()
     * @see #entrySet()
     */
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    /**
     * Returns a Collection view of all value collections in this Multimap.
     * Each element in the returned collection is itself a collection of values for a key.
     *
     * <p><b>Live View:</b> The returned collection is backed by the Multimap and reflects changes,
     * but should not be modified directly.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("evens", Arrays.asList(2, 4, 6));
     * multimap.putMany("odds", Arrays.asList(1, 3, 5));
     * 
     * Collection<List<Integer>> valueCollections = multimap.values();
     * 
     * // Analyze each collection
     * for (List<Integer> collection : valueCollections) {
     *     System.out.println("Collection size: " + collection.size());
     *     System.out.println("Sum: " + collection.stream().mapToInt(Integer::intValue).sum());
     * }
     * 
     * // Count total collections
     * int totalCollections = valueCollections.size(); // 2
     * }</pre>
     *
     * <p><b>Note:</b> If you need all individual elements rather than collections,
     * use {@link #flatValues()} instead.</p>
     *
     * @return a Collection view of the value collections contained in this Multimap
     * @see #keySet()
     * @see #flatValues()
     * @see #forEachValue(Consumer)
     */
    public Collection<V> values() {
        return backingMap.values();
    }

    /**
     * Returns a Set view of the key-collection mappings contained in this Multimap.
     * Each entry represents a key and its associated collection of values.
     *
     * <p><b>Live View:</b> The returned set is backed by the Multimap and reflects changes.
     * The Entry objects themselves are also live - changes to the value collections
     * are visible through the Entry.getValue() method.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("group1", Arrays.asList(1, 2, 3));
     * multimap.putMany("group2", Arrays.asList(4, 5));
     * 
     * Set<Map.Entry<String, List<Integer>>> entries = multimap.entrySet();
     * 
     * // Iterate over all key-collection pairs
     * for (Map.Entry<String, List<Integer>> entry : entries) {
     *     String key = entry.getKey();
     *     List<Integer> values = entry.getValue();
     *     System.out.println(key + " contains " + values.size() + " values");
     * }
     * 
     * // Stream processing
     * Map<String, Integer> sizes = entries.stream()
     *     .collect(Collectors.toMap(
     *         Map.Entry::getKey,
     *         e -> e.getValue().size()
     *     ));
     * }</pre>
     *
     * @return a Set view of the key-collection mappings contained in this Multimap
     * @see #keySet()
     * @see #values()
     * @see #forEach(BiConsumer)
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }

    /**
     * Returns a List containing all individual values from all collections in this Multimap.
     * This method "flattens" the Multimap structure, combining all values into a single list.
     *
     * <p><b>Independent Copy:</b> The returned List is a new, independent copy.
     * Changes to the Multimap after calling this method will not affect the returned List.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putMany("group1", Arrays.asList(1, 2, 3));
     * multimap.putMany("group2", Arrays.asList(4, 5));
     * multimap.putMany("group3", Arrays.asList(6));
     * 
     * List<Integer> allValues = multimap.flatValues();
     * // allValues contains [1, 2, 3, 4, 5, 6] (order may vary by Multimap type)
     * 
     * // Process all values uniformly
     * int sum = allValues.stream().mapToInt(Integer::intValue).sum();
     * Collections.sort(allValues);
     * 
     * // Independent from Multimap
     * multimap.put("group4", 7);
     * // allValues still contains [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * <p><b>Performance:</b> Creates a new ArrayList with initial capacity equal to the total number of values.</p>
     *
     * @return a new List containing all individual values from this Multimap
     * @see #flatValues(IntFunction)
     * @see #values()
     * @see #flatForEachValue(Consumer)
     */
    public List<E> flatValues() {
        final List<E> result = new ArrayList<>(totalCountOfValues());

        for (final V v : backingMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     * Returns a collection of all values in the Multimap.
     *
     * This method retrieves all value collections present in the Multimap and flattens them into a single collection.
     * The type of the returned collection is determined by the provided supplier function.
     * The returned collection is a new collection and not backed by the Multimap, so changes to the Multimap are not reflected in the collection.
     *
     * @param <C> The type of the collection to be returned.
     * @param supplier A function that creates a new collection of the desired type. The function is provided with an integer argument, which is the total count of values in the Multimap.
     * @return A collection of all values contained in this Multimap.
     */
    public <C extends Collection<E>> C flatValues(final IntFunction<C> supplier) {
        final C result = supplier.apply(totalCountOfValues());

        for (final V v : backingMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     * Converts the Multimap to a Map.
     *
     * This method creates a new Map and copies all key-value pairs from the Multimap to the new Map.
     * The new Map has the same structural and hash characteristics as the Multimap.
     *
     * @return A Map containing all the key-value pairs of this Multimap.
     */
    public Map<K, V> toMap() {
        final Map<K, V> result = Maps.newTargetMap(backingMap);

        // result.putAll(backingMap);

        V val = null;

        for (final Map.Entry<K, V> e : backingMap.entrySet()) {
            val = valueSupplier.get();
            val.addAll(e.getValue());

            result.put(e.getKey(), val);
        }

        return result;
    }

    /**
     * Converts the Multimap to a Map of a specific type.
     *
     * This method creates a new Map of the type specified by the supplier function and copies all key-value pairs from the Multimap to the new Map.
     *
     * @param <M> The type of the Map to be returned.
     * @param supplier A function that creates a new Map of the desired type. The function is provided with an integer argument, which is the size of this Multimap.
     * @return A Map of the specified type containing all the key-value pairs of this Multimap.
     */
    public <M extends Map<K, V>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(size());

        // result.putAll(backingMap);

        V val = null;

        for (final Map.Entry<K, V> e : backingMap.entrySet()) {
            val = valueSupplier.get();
            val.addAll(e.getValue());

            result.put(e.getKey(), val);
        }

        return result;
    }

    /**
     * Converts the Multimap to a Multiset.
     *
     * This method creates a new Multiset and adds all keys from the Multimap to the new Multiset.
     * The count of each key in the Multiset is equal to the size of its corresponding value collection in the Multimap.
     *
     * @return A Multiset containing all the keys of this Multimap, with each key's count equal to the size of its corresponding value collection.
     */
    public Multiset<K> toMultiset() {
        final Multiset<K> multiset = new Multiset<>(backingMap.getClass());

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            multiset.setCount(entry.getKey(), entry.getValue().size());
        }

        return multiset;
    }

    // It won't work.
    //    /**
    //     * Returns a synchronized {@code Multimap} which shares the same internal {@code Map} with this {@code Multimap}.
    //     * That's to say the changes in one of the returned {@code Multimap} and this {@code Multimap} will impact another one.
    //     *
    //     * @see Collections#synchronizedMap(Map)
    //     */
    //    public Multimap<K, E, V> synchronized() {
    //        return new Multimap<>(Collections.synchronizedMap(valueMap), concreteValueType);
    //    }

    //    /**
    //     * Returns a view of this multimap as a {@code Map} from each distinct key
    //     * to the nonempty collection of that key's associated values.
    //     *
    //     * <p>Changes to the returned map or the collections that serve as its values
    //     * will update the underlying multimap, and vice versa.
    //     *
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @Beta
    //    public Map<K, V> unwrap() {
    //        return backingMap;
    //    }

    /**
     * Returns an iterator over the entries in the Multimap.
     * Each element in the iteration is a Map.Entry where the key is a key in the Multimap and the value is the corresponding collection of values.
     *
     * @return An Iterator over the entries in the Multimap.
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return backingMap.entrySet().iterator();
    }

    /**
     * Returns a Stream of the entries in the Multimap.
     * Each element in the Stream is a Map.Entry where the key is a key in the Multimap and the value is the corresponding collection of values.
     *
     * @return A Stream of the entries in the Multimap.
     */
    public Stream<Map.Entry<K, V>> stream() {
        return Stream.of(backingMap.entrySet());
    }

    /**
     * Returns an EntryStream of the entries in the Multimap.
     * Each element in the EntryStream is a Map.Entry where the key is a key in the Multimap and the value is the corresponding collection of values.
     *
     * @return An EntryStream of the entries in the Multimap.
     */
    public EntryStream<K, V> entryStream() {
        return EntryStream.of(backingMap);
    }

    /**
     * Removes all key-value pairs from the Multimap.
     */
    public void clear() {
        backingMap.clear();
    }

    /**
     * Returns the number of key-value pairs in the Multimap.
     *
     * This method counts the number of keys in the Multimap. Each key and its corresponding collection of values is considered a single entry.
     * Therefore, the size is equal to the number of distinct keys in the Multimap.
     *
     * @return The number of key-value pairs in the Multimap.
     */
    public int size() {
        return backingMap.size();
    }

    /**
     * Returns the total count of all the elements in all value collections in the Multimap.
     *
     * This method iterates over all value collections in the Multimap and sums up their sizes.
     * The size of a value collection is the number of elements it contains.
     * Therefore, the total count of values is the sum of the sizes of all value collections.
     *
     * @return The total count of all the elements in all value collections in the Multimap.
     */
    public int totalCountOfValues() {
        if (backingMap.isEmpty()) {
            return 0;
        }

        int count = 0;

        for (final V v : backingMap.values()) {
            count += v.size();
        }

        return count;
    }

    /**
     * Checks if the Multimap is empty.
     *
     * @return {@code true} if the Multimap contains no key-value pairs, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Applies the given function to this Multimap and returns the result.
     *
     * @param <R> The type of the result returned by the function.
     * @param <X> The type of the exception that can be thrown by the function.
     * @param func The function to be applied to this Multimap.
     * @return The result of applying the function to this Multimap.
     * @throws X if the function throws an exception of type X.
     */
    public <R, X extends Exception> R apply(final Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
        return func.apply(this);
    }

    /**
     * Applies the given function to this Multimap and returns the result if the Multimap is not empty, and wraps the returned result with an Optional.
     * If the Multimap is empty, the method returns an empty Optional.
     *
     * @param <R> The type of the result returned by the function.
     * @param <X> The type of the exception that can be thrown by the function.
     * @param func The function to be applied to this Multimap.
     * @return An Optional containing the result of applying the function to this Multimap, or an empty Optional if the Multimap is empty.
     * @throws X if the function throws an exception of type X.
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     * Applies the given action to this Multimap.
     *
     * @param <X> The type of the exception that can be thrown by the action.
     * @param action The consumer action to be applied to this Multimap.
     * @throws X if the action throws an exception of type X.
     */
    public <X extends Exception> void accept(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        action.accept(this);
    }

    /**
     * Applies the given action to this Multimap if it is not empty.
     *
     * @param <X> The type of the exception that can be thrown by the action.
     * @param action The consumer action to be applied to this Multimap.
     * @return An instance of OrElse which can be used to perform some other operation if the Multimap is empty.
     * @throws X if the action throws an exception of type X.
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        return If.is(size() > 0).then(this, action);
    }

    /**
     * Returns the hash code value for this Multimap.
     *
     * @return the hash code value for this Multimap.
     */
    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    /**
     * Checks if this Multimap is equal to the specified object.
     * The method returns {@code true} if the specified object is also a Multimap and has the same keys and values.
     *
     * @param obj The object to be compared with this Multimap for equality.
     * @return {@code true} if the specified object is equal to this Multimap, {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multimap && backingMap.equals(((Multimap<K, E, V>) obj).backingMap));
    }

    /**
     * Returns a string representation of this Multimap.
     *
     * The string representation consists of a list of key-value mappings in the Multimap, enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters ", " (comma and space).
     * Each key-value mapping is rendered as the key followed by an equal sign ("=") followed by the associated value.
     *
     * @return a string representation of this Multimap.
     */
    @Override
    public String toString() {
        return backingMap.toString();
    }
}
