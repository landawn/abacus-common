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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
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
public sealed class Multimap<K, E, V extends Collection<E>> permits ListMultimap, SetMultimap {

    final Supplier<? extends Map<K, V>> mapSupplier;

    final Supplier<? extends V> valueSupplier;

    final Map<K, V> valueMap;

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
    Multimap(int initialCapacity) {
        this(N.<K, V> newHashMap(initialCapacity), (Supplier<V>) Suppliers.ofList());
    }

    @SuppressWarnings("rawtypes")
    Multimap(final Class<? extends Map> mapType, final Class<? extends Collection> valueType) {
        this(Suppliers.ofMap(mapType), valueType2Supplier(valueType));
    }

    Multimap(final Supplier<? extends Map<K, V>> mapSupplier, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = mapSupplier;
        this.valueSupplier = valueSupplier;
        this.valueMap = mapSupplier.get();
    }

    @Internal
    Multimap(final Map<K, V> valueMap, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.valueSupplier = valueSupplier;
        this.valueMap = valueMap;
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
     *
     * @param key
     * @return
     */
    public V get(final Object key) {
        return valueMap.get(key);
    }

    /**
     * Gets the or default.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public V getOrDefault(final Object key, V defaultValue) {
        final V value = valueMap.get(key);

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
        V val = valueMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            valueMap.put(key, val);
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

        for (Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                valueMap.put(key, val);
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
        V val = valueMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            valueMap.put(key, val);
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
        V val = valueMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            val.add(e);
            valueMap.put(key, val);
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

        V val = valueMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            valueMap.put(key, val);
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

        V val = valueMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            val.addAll(c);
            valueMap.put(key, val);
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

        for (Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                valueMap.put(key, val);
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

        for (Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            if (N.isEmpty(e.getValue())) {
                continue;
            }

            key = e.getKey();
            val = valueMap.get(key);

            if (val == null) {
                val = valueSupplier.get();
                valueMap.put(key, val);
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
        V val = valueMap.get(key);

        if (val != null && val.remove(e)) {
            if (val.isEmpty()) {
                valueMap.remove(key);
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

        for (Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (N.notEmpty(val)) {
                if (!result) {
                    result = val.remove(e.getValue());
                } else {
                    val.remove(e.getValue());
                }

                if (val.isEmpty()) {
                    valueMap.remove(key);
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
        return valueMap.remove(key);
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
        final V val = valueMap.get(key);

        if (N.notEmpty(val)) {
            result = val.removeAll(c);

            if (val.isEmpty()) {
                valueMap.remove(key);
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

        for (Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (N.notEmpty(val)) {
                if (!result) {
                    result = val.removeAll(e.getValue());
                } else {
                    val.removeAll(e.getValue());
                }

                if (val.isEmpty()) {
                    valueMap.remove(key);
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

        for (Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (N.notEmpty(val) && N.notEmpty(e.getValue())) {
                if (!result) {
                    result = val.removeAll(e.getValue());
                } else {
                    val.removeAll(e.getValue());
                }

                if (val.isEmpty()) {
                    valueMap.remove(key);
                }
            }
        }

        return result;
    }

    /**
     * Remove the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param value
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     * @see Collection#remove(Object)
     */
    public <X extends Exception> boolean removeOneIf(E value, Throwables.Predicate<? super K, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (K key : this.valueMap.keySet()) {
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

        for (K k : removingKeys) {
            if (removeOne(k, value)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param value
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     * @see Collection#remove(Object)
     */
    public <X extends Exception> boolean removeOneIf(E value, Throwables.BiPredicate<? super K, ? super V, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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

        for (K k : removingKeys) {
            if (removeOne(k, value)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified values (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param values
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     * @see Collection#removeAll(Collection)
     */
    public <X extends Exception> boolean removeManyIf(Collection<?> values, Throwables.Predicate<? super K, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (K key : this.valueMap.keySet()) {
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

        for (K k : removingKeys) {
            if (removeMany(k, values)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove the specified values (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param values
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     * @see Collection#removeAll(Collection)
     */
    public <X extends Exception> boolean removeManyIf(Collection<?> values, Throwables.BiPredicate<? super K, ? super V, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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

        for (K k : removingKeys) {
            if (removeMany(k, values)) {
                modified = true;
            }
        }

        return modified;
    }

    /**
     * Remove all the values associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean removeAllIf(Throwables.Predicate<? super K, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (K key : this.valueMap.keySet()) {
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

        for (K k : removingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Remove all the values associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean removeAllIf(Throwables.BiPredicate<? super K, ? super V, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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

        for (K k : removingKeys) {
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
        final V val = valueMap.get(key);

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

            return false;
        } else {
            if (val.remove(oldValue)) {
                val.add(newValue);
                return true;
            }

            return false;
        }
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
        final V val = valueMap.get(key);

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
        final V val = valueMap.get(key);

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
     * @param <X>
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceOneIf(Throwables.Predicate<? super K, X> predicate, E oldValue, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                modified = modified | replaceOne(entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return modified;
    }

    /**
     * Replace the specified {@code oldValue} (one occurrence) from the value set associated with keys which satisfy the specified {@code predicate} with the specified {@code newValue}.
     *
     * @param <X>
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceOneIf(Throwables.BiPredicate<? super K, ? super V, X> predicate, E oldValue, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                modified = modified | replaceOne(entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return modified;
    }

    /**
     * Replace all the specified {@code oldValue} (all occurrences) from the value set associated with keys which satisfy the specified {@code predicate} with single specified {@code newValue}.
     *
     * @param <X>
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceManyWithOneIf(Throwables.Predicate<? super K, X> predicate, Collection<? extends E> oldValues, E newValue)
            throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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
     * @param <X>
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceManyWithOneIf(Throwables.BiPredicate<? super K, ? super V, X> predicate, Collection<? extends E> oldValues,
            E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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
     * @param <X>
     * @param predicate
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceAllWithOneIf(Throwables.Predicate<? super K, X> predicate, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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
     * @param <X>
     * @param predicate
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceAllWithOneIf(Throwables.BiPredicate<? super K, ? super V, X> predicate, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
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
     * @param <X>
     * @param function
     * @throws X the x
     */
    public <X extends Exception> void replaceAll(Throwables.BiFunction<? super K, ? super V, ? extends V, X> function) throws X {
        List<K> keyToRemove = null;
        V newVal = null;

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            newVal = function.apply(entry.getKey(), entry.getValue());

            if (N.isEmpty(newVal)) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else {
                try {
                    entry.setValue(newVal);
                } catch (IllegalStateException ise) {
                    throw new ConcurrentModificationException(ise);
                }
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (K key : keyToRemove) {
                valueMap.remove(key);
            }
        }
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (N.notEmpty(oldValue)) {
     *     return oldValue;
     * }
     *
     * final V newValue = mappingFunction.apply(key);
     *
     * if (N.notEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param <X>
     * @param key
     * @param mappingFunction
     * @return
     * @throws X the x
     */
    public <X extends Exception> V computeIfAbsent(K key, Throwables.Function<? super K, ? extends V, X> mappingFunction) throws X {
        N.checkArgNotNull(mappingFunction);

        final V oldValue = get(key);

        if (N.notEmpty(oldValue)) {
            return oldValue;
        }

        final V newValue = mappingFunction.apply(key);

        if (N.notEmpty(newValue)) {
            valueMap.put(key, newValue);
        }

        return get(key);
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (N.isEmpty(oldValue)) {
     *     return oldValue;
     * }
     *
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     valueMap.remove(key);
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param <X>
     * @param key
     * @param remappingFunction
     * @return
     * @throws X the x
     */
    public <X extends Exception> V computeIfPresent(K key, Throwables.BiFunction<? super K, ? super V, ? extends V, X> remappingFunction) throws X {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = get(key);

        if (N.isEmpty(oldValue)) {
            return oldValue;
        }

        final V newValue = remappingFunction.apply(key, oldValue);

        if (N.notEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else {
            valueMap.remove(key);
        }

        return get(key);
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     * final V newValue = remappingFunction.apply(key, oldValue);
     *
     * if (N.notEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     if (oldValue != null) {
     *         valueMap.remove(key);
     *     }
     * }
     *
     * return get(key);
     * </pre>
     *
     * @param <X>
     * @param key
     * @param remappingFunction
     * @return
     * @throws X the x
     */
    public <X extends Exception> V compute(K key, Throwables.BiFunction<? super K, ? super V, ? extends V, X> remappingFunction) throws X {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = get(key);
        final V newValue = remappingFunction.apply(key, oldValue);

        if (N.notEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else if (oldValue != null) {
            valueMap.remove(key);
        }

        return get(key);
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     * final V newValue = oldValue == null ? value : remappingFunction.apply(oldValue, value);
     *
     * if (N.notEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else if (oldValue != null) {
     *     valueMap.remove(key);
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param <X>
     * @param key
     * @param value
     * @param remappingFunction
     * @return
     * @throws X the x
     */
    public <X extends Exception> V merge(K key, V value, Throwables.BiFunction<? super V, ? super V, ? extends V, X> remappingFunction) throws X {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(value);

        final V oldValue = get(key);
        final V newValue = oldValue == null ? value : remappingFunction.apply(oldValue, value);

        if (N.notEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else if (oldValue != null) {
            valueMap.remove(key);
        }

        return get(key);
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     *
     * <pre>
     * final V oldValue = get(key);
     *
     * if (N.isEmpty(oldValue)) {
     *     put(key, e);
     *     return get(key);
     * }
     *
     * final V newValue = remappingFunction.apply(oldValue, e);
     *
     * if (N.notEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else if (oldValue != null) {
     *     valueMap.remove(key);
     * }
     *
     * return  get(key);
     * </pre>
     *
     * @param <X>
     * @param key
     * @param e
     * @param remappingFunction
     * @return
     * @throws X the x
     */
    public <X extends Exception> V merge(K key, E e, Throwables.BiFunction<? super V, ? super E, ? extends V, X> remappingFunction) throws X {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(e);

        final V oldValue = get(key);

        if (N.isEmpty(oldValue)) {
            put(key, e);
            return get(key);
        }

        final V newValue = remappingFunction.apply(oldValue, e);

        if (N.notEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else if (oldValue != null) {
            valueMap.remove(key);
        }

        return get(key);
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
            for (Map.Entry<K, V> entry : multimap.entrySet()) {
                final V v = entry.getValue();

                if (N.notEmpty(v)) {
                    for (E e : v) {
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
        final V val = valueMap.get(key);

        return val == null ? false : val.contains(e);
    }

    /**
     *
     * @param key
     * @return
     */
    public boolean containsKey(final Object key) {
        return valueMap.containsKey(key);
    }

    /**
     *
     * @param e
     * @return
     */
    public boolean containsValue(final Object e) {
        Collection<V> values = values();

        for (V val : values) {
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
        final V val = valueMap.get(key);

        return val == null ? false : (N.isEmpty(c) ? true : val.containsAll(c));
    }

    /**
     * Filter by key.
     *
     * @param <X>
     * @param filter
     * @return
     * @throws X the x
     */
    public <X extends Exception> Multimap<K, E, V> filterByKey(Throwables.Predicate<? super K, X> filter) throws X {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.valueMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filter by value.
     *
     * @param <X>
     * @param filter
     * @return
     * @throws X the x
     */
    public <X extends Exception> Multimap<K, E, V> filterByValue(Throwables.Predicate<? super V, X> filter) throws X {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            if (filter.test(entry.getValue())) {
                result.valueMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <X>
     * @param filter
     * @return
     * @throws X the x
     */
    public <X extends Exception> Multimap<K, E, V> filter(Throwables.BiPredicate<? super K, ? super V, X> filter) throws X {
        final Multimap<K, E, V> result = new Multimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.valueMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    public <X extends Exception> void forEach(Throwables.BiConsumer<? super K, ? super V, X> action) throws X {
        N.checkArgNotNull(action);

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Flat for each.
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    public <X extends Exception> void flatForEach(Throwables.BiConsumer<? super K, ? super E, X> action) throws X {
        N.checkArgNotNull(action);

        K key = null;

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            key = entry.getKey();

            for (E e : entry.getValue()) {
                action.accept(key, e);
            }
        }
    }

    /**
     * For each key.
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    @Beta
    public <X extends Exception> void forEachKey(final Throwables.Consumer<? super K, X> action) throws X {
        N.checkArgNotNull(action);

        for (K k : valueMap.keySet()) {
            action.accept(k);
        }
    }

    /**
     * For each value.
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    @Beta
    public <X extends Exception> void forEachValue(final Throwables.Consumer<? super V, X> action) throws X {
        N.checkArgNotNull(action);

        for (V v : valueMap.values()) {
            action.accept(v);
        }
    }

    /**
     * Flat for each value.
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    @Beta
    public <X extends Exception> void flatForEachValue(Throwables.Consumer<? super E, X> action) throws X {
        N.checkArgNotNull(action);

        for (V v : valueMap.values()) {
            for (E e : v) {
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
        return valueMap.keySet();
    }

    /**
     *
     *
     * @return
     */
    public Collection<V> values() {
        return valueMap.values();
    }

    /**
     *
     *
     * @return
     */
    public List<E> flatValues() {
        final List<E> result = new ArrayList<>(totalCountOfValues());

        for (V v : valueMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     *
     * @param <R>
     * @param supplier
     * @return
     */
    public <R extends Collection<E>> R flatValues(final IntFunction<R> supplier) {
        final R result = supplier.apply(totalCountOfValues());

        for (V v : valueMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     *
     *
     * @return
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return valueMap.entrySet();
    }

    /**
     *
     *
     * @return
     */
    public Map<K, V> toMap() {
        final Map<K, V> result = Maps.newTargetMap(valueMap);

        result.putAll(valueMap);

        return result;
    }

    /**
     *
     * @param <M>
     * @param supplier
     * @return
     */
    public <M extends Map<K, V>> M toMap(final IntFunction<? extends M> supplier) {
        final M map = supplier.apply(size());
        map.putAll(valueMap);
        return map;
    }

    /**
     *
     *
     * @return
     */
    public Multiset<K> toMultiset() {
        final Multiset<K> multiset = new Multiset<>(valueMap.getClass());

        for (Map.Entry<K, V> entry : valueMap.entrySet()) {
            multiset.set(entry.getKey(), entry.getValue().size());
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

    /**
     * Returns a view of this multimap as a {@code Map} from each distinct key
     * to the nonempty collection of that key's associated values.
     *
     * <p>Changes to the returned map or the collections that serve as its values
     * will update the underlying multimap, and vice versa.
     *
     * @return
     */
    @Beta
    public Map<K, V> unwrap() {
        return valueMap;
    }

    /**
     *
     *
     * @return
     */
    public Stream<Map.Entry<K, V>> stream() {
        return Stream.of(valueMap.entrySet());
    }

    /**
     *
     *
     * @return
     */
    public EntryStream<K, V> entryStream() {
        return EntryStream.of(valueMap);
    }

    /**
     * Clear.
     */
    public void clear() {
        valueMap.clear();
    }

    /**
     *
     *
     * @return
     */
    public int size() {
        return valueMap.size();
    }

    /**
     * Returns the total count of all the elements in all values.
     *
     * @return
     */
    public int totalCountOfValues() {
        int count = 0;

        for (V v : valueMap.values()) {
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
        return valueMap.isEmpty();
    }

    /**
     *
     * @param <R>
     * @param <X>
     * @param func
     * @return
     * @throws X the x
     */
    public <R, X extends Exception> R apply(Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
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
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws X {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <X>
     * @param action
     * @throws X the x
     */
    public <X extends Exception> void accept(Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
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
    public <X extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        return If.is(size() > 0).then(this, action);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multimap && valueMap.equals(((Multimap<K, E, V>) obj).valueMap));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return valueMap.toString();
    }
}
