/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Haiyang Li
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
 * @since 0.8
 */
public sealed class Multimap<K, E, V extends Collection<E>> implements Iterable<Map.Entry<K, V>> permits ListMultimap, SetMultimap {

    final Supplier<? extends Map<K, V>> mapSupplier;

    final Supplier<? extends V> valueSupplier;

    final Map<K, V> backingMap;

    /**
     * Returns a <code>Multimap<K, E, List<E>></code>.
     */
    Multimap() {
        this(HashMap.class, ArrayList.class);
    }

    /**
     * Returns a <code>Multimap<K, E, List<E>></code>.
     *
     * @param initialCapacity
     */
    Multimap(final int initialCapacity) {
        this(N.<K, V> newHashMap(initialCapacity), (Supplier<V>) Suppliers.ofList());
    }

    @SuppressWarnings("rawtypes")
    Multimap(final Class<? extends Map> mapType, final Class<? extends Collection> valueType) {
        this(Suppliers.ofMap(mapType), valueType2Supplier(valueType));
    }

    Multimap(final Supplier<? extends Map<K, V>> mapSupplier, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = mapSupplier;
        this.valueSupplier = valueSupplier;
        backingMap = mapSupplier.get();
    }

    @Internal
    Multimap(final Map<K, V> valueMap, final Supplier<? extends V> valueSupplier) {
        mapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.valueSupplier = valueSupplier;
        backingMap = valueMap;
    }

    /**
     * Value type 2 supplier.
     *
     * @param valueType
     * @return
     */
    @SuppressWarnings("rawtypes")
    static Supplier valueType2Supplier(final Class<? extends Collection> valueType) {
        return Suppliers.ofCollection(valueType);
    }

    /**
     * Return the first value for the given key, or {@code null} if no value is found
     * @param key the key
     */
    public E getFirst(final K key) {
        final V values = backingMap.get(key);
        return N.isEmpty(values) ? null : N.firstOrNullIfEmpty(values);
    }

    /**
     * Return the first value for the given key, or {@code defaultValue} if no value is found
     * @param key the key
     * @param defaultValue the default value to return if no value is found
     */
    public E getFirstOrDefault(final K key, final E defaultValue) {
        final V values = backingMap.get(key);
        return N.isEmpty(values) ? defaultValue : N.firstOrNullIfEmpty(values);
    }

    /**
     *
     * @param key
     * @return
     */
    public V get(final Object key) {
        return backingMap.get(key);
    }

    /**
     * Gets the or default.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public V getOrDefault(final Object key, final V defaultValue) {
        final V value = backingMap.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    /**
     *
     * @param key
     * @param e
     * @return
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
     *
     * @param m
     * @return
     */
    public boolean put(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
        K key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                backingMap.put(key, val);
            }

            result |= val.add(e.getValue());
        }

        return result;
    }

    /**
     * If the specified value is not already associated with the specified key
     * associates it with the given key and returns {@code true}, else returns {@code false}.
     *
     * @param key
     * @param e
     * @return
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
     * If the specified key is not already associated with any value (or is mapped
     * to {@code null}) associates it with the given value and returns {@code true}, else returns {@code false}.
     *
     * @param key
     * @param e
     * @return
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
     *
     * @param key
     * @param c
     * @return
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
     * If the specified key is not already associated with any value (or is mapped
     * to {@code null}) associates it with the given values in the specified {@code collection}
     * and returns {@code true}, else returns {@code false}.
     *
     * @param key
     * @param c
     * @return
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
     *
     * @param m
     * @return
     */
    public boolean putMany(final Map<? extends K, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
        K key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                backingMap.put(key, val);
            }

            result |= val.addAll(e.getValue());
        }

        return result;
    }

    /**
     *
     * @param m
     * @return
     * @see Collection#addAll(Collection)
     */
    public boolean putMany(final Multimap<? extends K, ? extends E, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
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

            result |= val.addAll(e.getValue());
        }

        return result;
    }

    /**
     *
     * @param key
     * @param e
     * @return
     */
    public boolean removeOne(final Object key, final Object e) {
        final V val = backingMap.get(key);

        if (val != null && val.remove(e)) {
            if (val.isEmpty()) {
                backingMap.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     *
     * @param m
     * @return
     */
    public boolean removeOne(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (N.notEmpty(val)) {
                if (!result) {
                    result = val.remove(e.getValue());
                } else {
                    val.remove(e.getValue());
                }

                if (val.isEmpty()) {
                    backingMap.remove(key);
                }
            }
        }

        return result;
    }

    /**
     * Removes the all.
     *
     * @param key
     * @return values associated with specified key.
     */
    public V removeAll(final Object key) {
        return backingMap.remove(key);
    }

    /**
     * Removes the all.
     *
     * @param key
     * @param c
     * @return
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Object key, final Collection<?> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        boolean result = false;
        final V val = backingMap.get(key);

        if (N.notEmpty(val)) {
            result = val.removeAll(c);

            if (val.isEmpty()) {
                backingMap.remove(key);
            }
        }

        return result;
    }

    /**
     * <pre>
     * <code>
     * ListMultimap<String, Integer> listMultimap = ListMultimap.of("a", 1, "b", 2, "a", 2, "a", 2); // -> {a=[1, 2, 2], b=[2]}
     * listMultimap.removeAll(N.asMap("a", N.asList(2))); // -> {a=[1], b=[2]}
     * </code>
     * </pre>
     *
     * @param m
     * @return
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Map<?, ? extends Collection<?>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (N.notEmpty(val)) {
                if (!result) {
                    result = val.removeAll(e.getValue());
                } else {
                    val.removeAll(e.getValue());
                }

                if (val.isEmpty()) {
                    backingMap.remove(key);
                }
            }
        }

        return result;
    }

    /**
     * Removes the all.
     *
     * @param m
     * @return
     * @see Collection#removeAll(Collection)
     */
    public boolean removeMany(final Multimap<?, ?, ?> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean result = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            val = backingMap.get(key);

            if (N.notEmpty(val) && N.notEmpty(e.getValue())) {
                if (!result) {
                    result = val.removeAll(e.getValue());
                } else {
                    val.removeAll(e.getValue());
                }

                if (val.isEmpty()) {
                    backingMap.remove(key);
                }
            }
        }

        return result;
    }

    /**
     * Remove the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param value
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
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

        boolean modified = false;

        for (final K k : removingKeys) {
            if (removeOne(k, value)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param value
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
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

        boolean modified = false;

        for (final K k : removingKeys) {
            if (removeOne(k, value)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified values (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param values
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @see Collection#removeAll(Collection)
     */
    public boolean removeManyIf(final Collection<?> values, final Predicate<? super K> predicate) {
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

        boolean modified = false;

        for (final K k : removingKeys) {
            if (removeMany(k, values)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified values (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param values
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @see Collection#removeAll(Collection)
     */
    public boolean removeManyIf(final Collection<?> values, final BiPredicate<? super K, ? super V> predicate) {
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

        boolean modified = false;

        for (final K k : removingKeys) {
            if (removeMany(k, values)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove all the values associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
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
     * Remove all the values associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
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
     * Replace the specified {@code oldValue} (one occurrence) with the specified {@code newValue}.
     * <code>False</code> is returned if no <code>oldValue</code> is found.
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceOne(final K key, final E oldValue, final E newValue) {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        return replaceOne(val, oldValue, newValue);
    }

    private boolean replaceOne(final V val, final E oldValue, final E newValue) {
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
                val.add(newValue);
                return true;
            }
        }
        return false;
    }

    /**
     * Replace all the values associated to specified {@code key} with the specified {@code newValue}.
     * <code>False</code> is returned if no value found by the specified {@code key}
     *
     * @param key
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceAllWithOne(final K key, final E newValue) {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        val.clear();

        val.add(newValue);

        return true;
    }

    //    private boolean replaceAllWithOne(final V val, final E oldValue, final E newValue) {
    //        boolean modified = false;
    //
    //        if (val instanceof List) {
    //            final List<E> list = (List<E>) val;
    //
    //            if (list instanceof ArrayList) {
    //                if (oldValue == null) {
    //                    for (int i = 0, len = list.size(); i < len; i++) {
    //                        if (list.get(i) == null) {
    //                            list.set(i, newValue);
    //                            modified = true;
    //                        }
    //                    }
    //                } else {
    //                    for (int i = 0, len = list.size(); i < len; i++) {
    //                        if (oldValue.equals(list.get(i))) {
    //                            list.set(i, newValue);
    //                            modified = true;
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
    //                            modified = true;
    //                        }
    //                    }
    //                } else {
    //                    while (iter.hasNext()) {
    //                        if (oldValue.equals(iter.next())) {
    //                            iter.set(newValue);
    //                            modified = true;
    //                        }
    //                    }
    //                }
    //            }
    //        } else {
    //            final Object[] tmp = val.toArray();
    //            modified = N.replaceAll(tmp, oldValue, newValue) > 0;
    //            val.clear();
    //            val.addAll(Arrays.asList((E[]) tmp));
    //        }
    //
    //        return modified;
    //    }

    /**
     * Replace all the specified {@code oldValues} (all occurrences) with single specified {@code newValue}.
     * <code>False</code> is returned if no <code>oldValue</code> is found.
     *
     * @param key
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceManyWithOne(final K key, final Collection<? extends E> oldValues, final E newValue) {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        if (val.removeAll(oldValues)) {
            val.add(newValue);
            return true;
        }

        return false;
    }

    /**
     * Replace the specified {@code oldValue} (one occurrence) from the value set associated with keys which satisfy the specified {@code predicate} with the specified {@code newValue}.
     *
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceOneIf(final Predicate<? super K> predicate, final E oldValue, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                modified = modified | replaceOne(entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return modified;
    }

    /**
     * Replace the specified {@code oldValue} (one occurrence) from the value set associated with keys which satisfy the specified {@code predicate} with the specified {@code newValue}.
     *
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceOneIf(final BiPredicate<? super K, ? super V> predicate, final E oldValue, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                modified = modified | replaceOne(entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return modified;
    }

    /**
     * Replace all the specified {@code oldValue} (all occurrences) from the value set associated with keys which satisfy the specified {@code predicate} with single specified {@code newValue}.
     *
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceManyWithOneIf(final Predicate<? super K> predicate, final Collection<? extends E> oldValues, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey()) && entry.getValue().removeAll(oldValues)) {
                entry.getValue().add(newValue);
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Replace all the specified {@code oldValue} (all occurrences) from the value set associated with keys which satisfy the specified {@code predicate} with single specified {@code newValue}.
     *
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceManyWithOneIf(final BiPredicate<? super K, ? super V> predicate, final Collection<? extends E> oldValues, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue()) && entry.getValue().removeAll(oldValues)) {
                entry.getValue().add(newValue);
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Replace all the values associated to specified {@code key} which satisfy the specified {@code predicate} with the specified {@code newValue}.
     *
     * @param predicate
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceAllWithOneIf(final Predicate<? super K> predicate, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                entry.getValue().clear();
                entry.getValue().add(newValue);

                modified = true;
            }
        }

        return modified;
    }

    /**
     * Replace all the values associated to specified {@code key} which satisfy the specified {@code predicate} with the specified {@code newValue}.
     *
     * @param predicate
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceAllWithOneIf(final BiPredicate<? super K, ? super V> predicate, final E newValue) {
        boolean modified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                entry.getValue().clear();
                entry.getValue().add(newValue);

                modified = true;
            }
        }

        return modified;
    }

    /**
     * The associated keys will be removed if null or empty values are returned by the specified <code>function</code>.
     *
     * @param function
     */
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        List<K> keyToRemove = null;
        V value = null;
        V newVal = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            value = entry.getValue();

            newVal = function.apply(entry.getKey(), value);

            if (N.isEmpty(newVal)) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else {
                value.clear();
                value.addAll(newVal);
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (final K key : keyToRemove) {
                backingMap.remove(key);
            }
        }
    }

    /**
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
     * @param key
     * @param mappingFunction
     * @return the new value after computation. It could be {@code null}
     * @throws IllegalArgumentException
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
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (oldValue == null) {
     *     return null;
     * }
     *
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue != newValue) {
     *          oldValue.clear();
     *          oldValue.addAll(newValue);
     *       }
     * } else {
     *     backingMap.remove(key);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param key
     * @param remappingFunction
     * @return the new value after computation. It could be {@code null}
     * @throws IllegalArgumentException
     */
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = get(key);

        if (oldValue == null) {
            return oldValue;
        }

        V ret = null;
        final V newValue = remappingFunction.apply(key, oldValue);

        if (N.notEmpty(newValue)) {
            if (oldValue != newValue) {
                oldValue.clear();
                oldValue.addAll(newValue);
            }

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue == null) {
     *          putMany(key, newValue);
     *      } else if (oldValue != newValue) {
     *          oldValue.clear();
     *          oldValue.addAll(newValue);
     *      }
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param key
     * @param remappingFunction
     * @return the new value after computation. It could be {@code null}
     * @throws IllegalArgumentException
     */
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        V ret = null;
        final V oldValue = get(key);
        final V newValue = remappingFunction.apply(key, oldValue);

        if (N.notEmpty(newValue)) {
            if (oldValue == null) {
                putMany(key, newValue);
                ret = get(key);
            } else {
                if (oldValue != newValue) {
                    oldValue.clear();
                    oldValue.addAll(newValue);
                }

                ret = oldValue;
            }
        } else if (oldValue != null) {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
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
     * final V newValue = remappingFunction.apply(oldValue, elements);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue == null) {
     *          putMany(key, newValue);
     *          return get(key);
     *      }
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param <C>
     * @param key
     * @param elements
     * @param remappingFunction
     * @return the new value after computation. It could be {@code null}
     * @throws IllegalArgumentException
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

        if (N.notEmpty(newValue)) {
            if (oldValue != newValue) {
                oldValue.clear();
                oldValue.addAll(newValue);
            }

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
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
     * final V newValue = remappingFunction.apply(oldValue, e);
     *
     * if (N.notEmpty(newValue)) {
     *      if (oldValue == null) {
     *          putMany(key, newValue);
     *          return get(key);
     *      }
     * } else if (oldValue != null) {
     *     backingMap.remove(key);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param key
     * @param e
     * @param remappingFunction
     * @return the new value after computation. It could be {@code null}
     * @throws IllegalArgumentException
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

        if (N.notEmpty(newValue)) {
            if (oldValue != newValue) {
                oldValue.clear();
                oldValue.addAll(newValue);
            }

            ret = oldValue;
        } else {
            backingMap.remove(key);
        }

        // return get(key);

        return ret;
    }

    /**
     *
     *
     * @param <VV>
     * @param <M>
     * @param multimapSupplier
     * @return
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
     *
     *
     * @return
     */
    public Multimap<K, E, V> copy() {
        final Multimap<K, E, V> copy = new Multimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     *
     * @param key
     * @param e
     * @return
     */
    public boolean contains(final Object key, final Object e) {
        final V val = backingMap.get(key);

        return val == null ? false : val.contains(e);
    }

    /**
     *
     * @param key
     * @return
     */
    public boolean containsKey(final Object key) {
        return backingMap.containsKey(key);
    }

    /**
     *
     * @param e
     * @return
     */
    public boolean containsValue(final Object e) {
        final Collection<V> values = values();

        for (final V val : values) {
            if (val.contains(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param key
     * @param c
     * @return
     */
    public boolean containsAll(final Object key, final Collection<?> c) {
        final V val = backingMap.get(key);

        return val == null ? false : (N.isEmpty(c) ? true : val.containsAll(c));
    }

    /**
     * Filter by key.
     *
     * @param filter
     * @return
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
     * Filter by value.
     *
     * @param filter
     * @return
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
     *
     * @param filter
     * @return
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
     *
     *
     * @param action
     * @throws IllegalArgumentException
     */
    public void forEach(final BiConsumer<? super K, ? super V> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Flat for each.
     *
     * @param action
     * @throws IllegalArgumentException
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
     * For each key.
     *
     * @param action
     * @throws IllegalArgumentException
     */
    @Beta
    public void forEachKey(final Consumer<? super K> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final K k : backingMap.keySet()) {
            action.accept(k);
        }
    }

    /**
     * For each value.
     *
     * @param action
     * @throws IllegalArgumentException
     */
    @Beta
    public void forEachValue(final Consumer<? super V> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final V v : backingMap.values()) {
            action.accept(v);
        }
    }

    /**
     * Flat for each value.
     *
     * @param action
     * @throws IllegalArgumentException
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
     *
     *
     * @return
     */
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    /**
     *
     *
     * @return
     */
    public Collection<V> values() {
        return backingMap.values();
    }

    /**
     *
     *
     * @return
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }

    /**
     *
     *
     * @return
     */
    public List<E> flatValues() {
        final List<E> result = new ArrayList<>(totalCountOfValues());

        for (final V v : backingMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param supplier
     * @return
     */
    public <C extends Collection<E>> C flatValues(final IntFunction<C> supplier) {
        final C result = supplier.apply(totalCountOfValues());

        for (final V v : backingMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     *
     *
     * @return
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
     *
     * @param <M>
     * @param supplier
     * @return
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
     *
     *
     * @return
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
    //    public Multimap<K, E, V> synchronizedd() {
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
     *
     *
     * @return
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return backingMap.entrySet().iterator();
    }

    /**
     *
     *
     * @return
     */
    public Stream<Map.Entry<K, V>> stream() {
        return Stream.of(backingMap.entrySet());
    }

    /**
     *
     *
     * @return
     */
    public EntryStream<K, V> entryStream() {
        return EntryStream.of(backingMap);
    }

    /**
     * Clear.
     */
    public void clear() {
        backingMap.clear();
    }

    /**
     *
     *
     * @return
     */
    public int size() {
        return backingMap.size();
    }

    /**
     * Returns the total count of all the elements in all values.
     *
     * @return
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
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     *
     * @param <R>
     * @param <X>
     * @param func
     * @return
     * @throws X the x
     */
    public <R, X extends Exception> R apply(final Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
        return func.apply(this);
    }

    /**
     *
     * @param <R>
     * @param <X>
     * @param func
     * @return
     * @throws X the x
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    public <X extends Exception> void accept(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        action.accept(this);
    }

    /**
     * Accept if not empty.
     *
     * @param <X>
     * @param action
     * @return
     * @throws X the x
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        return If.is(size() > 0).then(this, action);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multimap && backingMap.equals(((Multimap<K, E, V>) obj).backingMap));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return backingMap.toString();
    }
}
