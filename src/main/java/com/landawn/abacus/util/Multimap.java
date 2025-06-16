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
import com.landawn.abacus.util.Fn.Suppliers;
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
     * Return the first value for the given key, or {@code null} if no value is found.
     *
     * @param key The key whose associated value is to be returned.
     * @return The first value associated with the specified key, or {@code null} if the key has no associated values.
     */
    public E getFirst(final K key) {
        final V values = backingMap.get(key);
        return N.isEmpty(values) ? null : N.firstOrNullIfEmpty(values);
    }

    /**
     * Return the first value for the given key, or {@code defaultValue} if no value is found.
     *
     * @param key The key whose associated value is to be returned.
     * @param defaultValue The default value to return if no value is associated with the key.
     * @return The first value associated with the specified key, or the default value if the key has no associated values.
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
     *
     * If the Multimap previously contained mappings for the key, the new value is added to the collection of values associated with this key.
     *
     * @param key The key with which the specified value is to be associated.
     * @param e The value to be associated with the specified key.
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise.
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
     * Associates the specified value with the specified key in this Multimap if the key is not already associated with a value.
     *
     * This method checks if the Multimap contains the specified key. If it does not, it associates the key with the specified value.
     * If the key is already present, this method does nothing.
     *
     * @param key The key with which the specified value is to be associated.
     * @param e The value to be associated with the specified key.
     * @return {@code true} if the key was not already associated with a value and the association was successfully added, {@code false} otherwise.
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
     * Associates the specified value with the specified key in this Multimap if the key is not already present.
     *
     * This method checks if the Multimap contains the specified key. If it does not, it associates the key with the specified value.
     * If the key is already present, this method does nothing.
     *
     * @param key The key with which the specified value is to be associated.
     * @param e The value to be associated with the specified key.
     * @return {@code true} if the key was not already present and the association was successfully added, {@code false} otherwise.
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
     * Associates all the specified values from the provided collection with the specified key in this Multimap.
     * If the Multimap previously contained mappings for the key, the new values are added to the collection of values associated with this key.
     * If the specified value collection is empty, {@code false} is returned.
     *
     * @param key The key with which the specified values are to be associated.
     * @param c The collection of values to be associated with the specified key.
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise.
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
     * Removes a single occurrence of the specified element from the collection of values associated with the specified key in this Multimap.
     *
     * This method retrieves the collection of values associated with the specified key and attempts to remove the specified element from it.
     * If the element is successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param key The key whose associated collection of values is to be processed.
     * @param e The element to be removed from the collection of values associated with the specified key.
     * @return {@code true} if the element was successfully removed from the collection of values associated with the specified key, {@code false} otherwise.
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
     * The key is also removed from the Multimap.
     *
     * @param key The key whose associated collection of values is to be removed.
     * @return The collection of values that were associated with the specified key, or {@code null} if the key was not present in the Multimap.
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
     * Removes a single occurrence of the specified value from the collections of values associated with keys that satisfy the specified predicate.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove a single occurrence of the specified value from it.
     * If the value is successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param value The value to be removed from the collections of values associated with the keys that satisfy the predicate.
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @return {@code true} if at least one value was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @see Collection#remove(Object)
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
     * Removes all occurrences of the specified elements from the collections of values associated with keys that satisfy the specified predicate.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.
     *
     * @param values The collection of elements to be removed from the collections of values associated with the keys that satisfy the predicate.
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @see Collection#removeAll(Collection)
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
     * Removes all values associated with keys that satisfy the specified predicate from this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it removes all values associated with the key and the key itself from the Multimap.
     *
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @return {@code true} if at least one key-value pair was successfully removed from the Multimap, {@code false} otherwise.
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
     * Removes all values associated with keys that satisfy the specified predicate from this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it removes all values associated with the key and the key itself from the Multimap.
     *
     * @param predicate The predicate to be applied to each key-value pair in the Multimap.
     * @return {@code true} if at least one key-value pair was successfully removed from the Multimap, {@code false} otherwise.
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
     * Replaces a single occurrence of the specified old value with the new value for the given key in this Multimap.
     *
     * This method retrieves the collection of values associated with the specified key and attempts to replace a single occurrence of the old value with the new value.
     * If the old value is successfully replaced, the method returns {@code true}. If the old value is not found in the collection of values associated with the key, the method returns {@code false}.
     *
     * @param key The key whose associated collection of values is to be processed.
     * @param oldValue The old value to be replaced in the collection of values associated with the specified key.
     * @param newValue The new value to replace the old value in the collection of values associated with the specified key.
     * @return {@code true} if a value was successfully replaced in the collection of values associated with the specified key, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key.
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
     * Replaces all the values associated with the specified key in this Multimap with the new value.
     *
     * This method retrieves the collection of values associated with the specified key and clears it.
     * Then, it adds the new value to the now empty collection. If the key is not present in the Multimap,
     * this method does nothing and returns {@code false}.
     *
     * @param key The key whose associated collection of values is to be replaced.
     * @param newValue The new value to replace all the old values in the collection associated with the specified key.
     * @return {@code true} if the values were successfully replaced in the collection associated with the specified key, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key.
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
     * Replaces all occurrences of the specified old values with the new value for the given key in this Multimap.
     *
     * This method retrieves the collection of values associated with the specified key and attempts to remove all occurrences of the old values.
     * If the old values are successfully removed, the method adds the new value to the collection. If the key is not present in the Multimap,
     * or none of the old values are found in the collection of values associated with the key, the method returns {@code false}.
     *
     * @param key The key whose associated collection of values is to be processed.
     * @param oldValues The collection of old values to be replaced in the collection of values associated with the specified key.
     * @param newValue The new value to replace all the old values in the collection associated with the specified key.
     * @return {@code true} if at least one old value was successfully replaced in the collection of values associated with the specified key, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key.
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
     * Replaces a single occurrence of the specified old value with the new value for keys that satisfy the specified predicate in this Multimap.
     *
     * This method iterates over the keys in the Multimap and applies the predicate to each key. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to replace a single occurrence of the old value with the new value.
     * If the old value is successfully replaced, the method returns {@code true}. If the old value is not found in the collection of values associated with the key, the method returns {@code false}.
     *
     * @param predicate The predicate to be applied to each key in the Multimap.
     * @param oldValue The old value to be replaced in the collections of values associated with the keys that satisfy the predicate.
     * @param newValue The new value to replace the old value in the collections of values associated with the keys that satisfy the predicate.
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise.
     * @throw IllegalStateException if the new value cannot be added to the collection associated with the specified key that satisfy the predicate.
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
     * Computes the value for the specified key using the given mapping function if the key is not already associated with a value.
     *
     * This method first checks if the Multimap contains the specified key. If it does not, it applies the mapping function to the key,
     * and associates the key with the resulting value in the Multimap. If the mapping function returns {@code null} or an empty collection,
     * the key is not associated with any value in the Multimap.
     *
     * If the key is already associated with a value in the Multimap, this method returns the old value and does not modify the Multimap.
     * <br />
     * <br />
     *
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (oldValue != null)) {
     *     return oldValue;
     * }
     *
     * final V newValue = mappingFunction.apply(key);
     *
     * if (N.notEmpty(newValue)) {
     *      putMany(key, newValue);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param key The key whose associated value is to be computed.
     * @param mappingFunction The function to compute a value.
     * @return The new value associated with the specified key, or the old value if the key is already associated with a value.
     * @throws IllegalArgumentException if the mappingFunction is {@code null}.
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
     * Computes the value for the specified key using the given remapping function if the key is already associated with a value.
     *
     * This method first checks if the Multimap contains the specified key. If it does, it applies the remapping function to the key and its current associated value,
     * and updates the key's associated value in the Multimap with the result. If the remapping function returns {@code null} or an empty collection,
     * the key is removed from the Multimap.
     *
     * If the key is not already associated with a value in the Multimap, this method returns {@code null} and does not modify the Multimap.
     *
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (oldValue == null) {
     *     return null;
     * }
     *
     * V ret = null;
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue != newValue) {
     *          oldValue.clear();
     *          oldValue.addAll(newValue);
     *       }
     *
     *       ret = oldValue;
     * } else {
     *     backingMap.remove(key);
     * }
     *
     * return ret;
     * </pre>
     *
     * @param key The key whose associated value is to be computed.
     * @param remappingFunction The function to compute a value.
     * @return The new value associated with the specified key, or {@code null} if the key is not already associated with a value or the remapping function returns {@code null} or an empty collection.
     * @throws IllegalArgumentException if the remappingFunction is {@code null}.
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
     * Inverts the Multimap, swapping keys with values.
     *
     * This method creates a new Multimap where the keys of this Multimap become the values of the new Multimap, and the values of this Multimap become the keys of the new Multimap.
     * If multiple keys in this Multimap are associated with the same value, the resulting Multimap associates all such keys with that value.
     *
     * The type of the new Multimap is determined by the provided multimapSupplier function. This function should return a new Multimap of the desired type.
     * The function is provided with an integer argument, which is the size of this Multimap.
     *
     * @param <VV> The type of the collection of values in the new Multimap. The element type of this collection is the same as the key type of this Multimap.
     * @param <M> The type of the new Multimap. This is a Multimap where the key type is the value type of this Multimap, and the value type is a collection of the key type of this Multimap.
     * @param multimapSupplier A function that creates a new Multimap of the desired type. The function is provided with an integer argument, which is the size of this Multimap.
     * @return The new, inverted Multimap.
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
     * Returns a new Multimap and copies all key-value pairs from this Multimap to the new one.
     * The new Multimap has the same structural and hash characteristics as this one.
     *
     * @return A new Multimap containing all the key-value pairs of this Multimap.
     * @see #putMany(Multimap)
     */
    public Multimap<K, E, V> copy() {
        final Multimap<K, E, V> copy = new Multimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Checks if the Multimap contains the specified key-value pair.
     *
     * This method retrieves the collection of values associated with the specified key and checks if it contains the specified element.
     * If the key is not present in the Multimap, or the collection of values associated with the key does not contain the element, the method returns {@code false}.
     *
     * @param key The key to be checked.
     * @param e The value to be checked.
     * @return {@code true} if the Multimap contains the specified key-value pair, {@code false} otherwise.
     */
    public boolean contains(final Object key, final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        return val != null && val.contains(e);
    }

    /**
     * Checks if the Multimap contains the specified key.
     *
     * @param key The key to be checked.
     * @return {@code true} if the Multimap contains the specified key, {@code false} otherwise.
     */
    public boolean containsKey(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(key);
    }

    /**
     * Checks if the Multimap contains the specified value.
     *
     * This method iterates over all the collections of values in the Multimap and checks if any of them contains the specified value.
     * If at least one collection contains the value, the method returns {@code true}. If no collection contains the value, or if the Multimap is empty, the method returns {@code false}.
     *
     * @param e The value to be checked.
     * @return {@code true} if the Multimap contains the specified value, {@code false} otherwise.
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
     * Checks if the Multimap contains all elements of the specified collection for the given key.
     *
     * This method retrieves the collection of values associated with the specified key and checks if it contains all elements of the specified collection.
     * If the key is not present in the Multimap, or the collection of values associated with the key does not contain all elements of the specified collection, the method returns {@code false}.
     *
     * @param key The key whose associated collection of values is to be checked.
     * @param c The collection of elements to be checked.
     * @return {@code true} if the Multimap contains all elements of the specified collection for the given key, {@code false} otherwise.
     */
    public boolean containsAll(final Object key, final Collection<?> c) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        return val != null && (N.isEmpty(c) || val.containsAll(c));
    }

    /**
     * Filters the Multimap based on the provided key filter.
     *
     * This method creates a new Multimap and adds all key-value pairs from the current Multimap that satisfy the provided key filter.
     * The new Multimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key in the Multimap. If the predicate returns {@code true}, the key-value pair is included in the new Multimap.
     * @return A new Multimap containing all the key-value pairs of the current Multimap that satisfy the provided key filter.
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
     * Filters the Multimap based on the provided value filter.
     *
     * This method creates a new Multimap and adds all key-value pairs from the current Multimap that satisfy the provided value filter.
     * The new Multimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each value in the Multimap. If the predicate returns {@code true}, the key-value pair is included in the new Multimap.
     * @return A new Multimap containing all the key-value pairs of the current Multimap that satisfy the provided value filter.
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
     * Filters the Multimap based on the provided key-value pair filter.
     *
     * This method creates a new Multimap and adds all key-value pairs from the current Multimap that satisfy the provided key-value pair filter.
     * The new Multimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key-value pair in the Multimap. If the predicate returns {@code true}, the key-value pair is included in the new Multimap.
     * @return A new Multimap containing all the key-value pairs of the current Multimap that satisfy the provided key-value pair filter.
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
     * Performs the given action for each key-element pair in the Multimap.
     *
     * This method iterates over all key-value pairs in the Multimap, where each value is a collection of elements.
     * It then iterates over each element in the value collection, and applies the provided BiConsumer action to the key and the element.
     * The action should be a function that accepts a key and an element, and performs some operation.
     * The action is performed in the order of the iteration if that order is specified.
     *
     * @param action The action to be performed for each key-element pair in the Multimap.
     * @throws IllegalArgumentException if the provided action is {@code null}.
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
     * Performs the given action for each key in the Multimap.
     *
     * This method iterates over all keys in the Multimap and applies the provided Consumer action.
     * The action should be a function that accepts a key and performs some operation.
     * The action is performed in the order of the iteration, if that order is specified.
     *
     * @param action The action to be performed for each key in the Multimap.
     * @throws IllegalArgumentException if the provided action is {@code null}.
     */
    @Beta
    public void forEachKey(final Consumer<? super K> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final K k : backingMap.keySet()) {
            action.accept(k);
        }
    }

    /**
     * Performs the given action for each value collection in the Multimap.
     *
     * This method iterates over all value collections in the Multimap and applies the provided Consumer action.
     * The action should be a function that accepts a collection of values and performs some operation.
     * The action is performed in the order of the iteration if that order is specified.
     * <br />
     * Usually, the value collection should not be modified outside this Multimap directly by the specified action, because it may cause unexpected behavior.
     *
     * @param action The action to be performed for each value collection in the Multimap.
     * @throws IllegalArgumentException if the provided action is {@code null}.
     */
    @Beta
    public void forEachValue(final Consumer<? super V> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final V v : backingMap.values()) {
            action.accept(v);
        }
    }

    /**
     * Performs the given action for each element in the Multimap.
     *
     * This method iterates over all value collections in the Multimap, where each value is a collection of elements.
     * It then iterates over each element in the value collection, and applies the provided Consumer action to the element.
     * The action should be a function that accepts an element and performs some operation.
     * The action is performed in the order of the iteration if that order is specified.
     *
     * @param action The action to be performed for each element in the Multimap.
     * @throws IllegalArgumentException if the provided action is {@code null}.
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
     * Returns the set of keys in the Multimap.
     *
     * This method retrieves all keys present in the Multimap and returns them as a Set.
     * The returned Set is backed by the Multimap, so changes to the Multimap are reflected in the Set.
     * If the Multimap is modified while an iteration over the Set is in progress, the results of the iteration are undefined.
     *
     * @return A Set view of the keys contained in this Multimap.
     */
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    /**
     * Returns a collection of the values in the Multimap.
     *
     * This method retrieves all value collections present in the Multimap and returns them as a Collection.
     * The returned Collection is backed by the Multimap, so changes to the Multimap are reflected in the Collection.
     * If the Multimap is modified while an iteration over the Collection is in progress, the results of the iteration are undefined.
     *
     * @return A Collection view of the value collections contained in this Multimap.
     */
    public Collection<V> values() {
        return backingMap.values();
    }

    /**
     * Returns a set of the mappings contained in this Multimap.
     *
     * This method retrieves all key-value pairs present in the Multimap and returns them as a Set of Map.Entry.
     * The returned Set is backed by the Multimap, so changes to the Multimap are reflected in the Set.
     * If the Multimap is modified while an iteration over the Set is in progress, the results of the iteration are undefined.
     *
     * @return A Set view of the mappings contained in this Multimap.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }

    /**
     * Returns a list of all values in the Multimap.
     *
     * This method retrieves all value collections present in the Multimap and flattens them into a single List.
     * The returned List is a new list and not backed by the Multimap, so changes to the Multimap are not reflected in the List.
     *
     * @return A List of all values contained in this Multimap.
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
