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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
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
public class Multimap<K, E, V extends Collection<E>> {

    /** The map supplier. */
    final Supplier<? extends Map<K, V>> mapSupplier;

    /** The value supplier. */
    final Supplier<? extends V> valueSupplier;

    /** The value map. */
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
        this(new HashMap<K, V>(initialCapacity), (Supplier<V>) Suppliers.ofList());
    }

    /**
     * Instantiates a new multimap.
     *
     * @param mapType
     * @param valueType
     */
    @SuppressWarnings("rawtypes")
    Multimap(final Class<? extends Map> mapType, final Class<? extends Collection> valueType) {
        this(Maps.mapType2Supplier(mapType), valueType2Supplier(valueType));
    }

    /**
     * Instantiates a new multimap.
     *
     * @param mapSupplier
     * @param valueSupplier
     */
    Multimap(final Supplier<? extends Map<K, V>> mapSupplier, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = mapSupplier;
        this.valueSupplier = valueSupplier;
        this.valueMap = mapSupplier.get();
    }

    /**
     * Instantiates a new multimap.
     *
     * @param valueMap
     * @param valueSupplier
     */
    @Internal
    Multimap(final Map<K, V> valueMap, final Supplier<? extends V> valueSupplier) {
        this.mapSupplier = Maps.mapType2Supplier(valueMap.getClass());
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
        if (Modifier.isAbstract(valueType.getModifiers())) {
            if (List.class.isAssignableFrom(valueType)) {
                return Suppliers.ofList();
            } else if (Set.class.isAssignableFrom(valueType)) {
                return Suppliers.ofSet();
            } else if (Queue.class.isAssignableFrom(valueType)) {
                return Suppliers.ofArrayDeque();
            } else {
                throw new IllegalArgumentException("Unsupported collection type: " + valueType.getCanonicalName());
            }
        } else {
            return new Supplier<Collection>() {
                @Override
                public Collection get() {
                    return N.newInstance(valueType);
                }
            };
        }
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param map
     * @param multimapSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M from(final Map<? extends K, ? extends E> map,
            final IntFunction<? extends M> multimapSupplier) {
        final M multimap = multimapSupplier.apply(map == null ? 0 : map.size());

        if (N.notNullOrEmpty(map)) {
            multimap.putAll(map);
        }

        return multimap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param map
     * @param multimapSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M fromm(final Map<? extends K, ? extends Collection<? extends E>> map,
            final IntFunction<? extends M> multimapSupplier) {
        final M multimap = multimapSupplier.apply(map == null ? 0 : map.size());

        if (N.notNullOrEmpty(map)) {
            for (Map.Entry<? extends K, ? extends Collection<? extends E>> entry : map.entrySet()) {
                multimap.putAll(entry.getKey(), entry.getValue());
            }
        }

        return multimap;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <X>
     * @param c
     * @param keyMapper
     * @param multimapSupplier
     * @return
     * @throws X the x
     */
    public static <T, K, V extends Collection<T>, M extends Multimap<K, T, V>, X extends Exception> M from(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends K, X> keyMapper, final IntFunction<? extends M> multimapSupplier) throws X {
        final M multimap = multimapSupplier.apply(c == null ? 0 : c.size());

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                multimap.put(keyMapper.apply(e), e);
            }
        }

        return multimap;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param <X>
     * @param <X2>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param multimapSupplier
     * @return
     * @throws X the x
     * @throws X2 the x2
     */
    public static <T, K, E, V extends Collection<E>, M extends Multimap<K, E, V>, X extends Exception, X2 extends Exception> M from(
            final Collection<? extends T> c, final Throwables.Function<? super T, ? extends K, X> keyMapper,
            final Throwables.Function<? super T, ? extends E, X2> valueExtractor, final IntFunction<? extends M> multimapSupplier) throws X, X2 {
        final M multimap = multimapSupplier.apply(c == null ? 0 : c.size());

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                multimap.put(keyMapper.apply(e), valueExtractor.apply(e));
            }
        }

        return multimap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param map
     * @param multimapSupplier
     * @return
     * @see ListMultimap#invertFrom(Map)
     * @see SetMultimap#invertFrom(Map)
     */
    public static <K, E, V extends Collection<K>, M extends Multimap<E, K, V>> M invertFrom(final Map<K, E> map,
            final IntFunction<? extends M> multimapSupplier) {
        final M multimap = multimapSupplier.apply(map == null ? 0 : map.size());

        if (N.notNullOrEmpty(map)) {
            for (Map.Entry<K, E> entry : map.entrySet()) {
                multimap.put(entry.getValue(), entry.getKey());
            }
        }

        return multimap;
    }

    /**
     * Flat invert from.
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param map
     * @param multimapSupplier
     * @return
     * @see ListMultimap#flatInvertFrom(Map)
     * @see SetMultimap#flatInvertFrom(Map)
     */
    public static <K, E, V extends Collection<K>, M extends Multimap<E, K, V>> M flatInvertFrom(final Map<K, ? extends Collection<? extends E>> map,
            final IntFunction<? extends M> multimapSupplier) {
        final M multimap = multimapSupplier.apply(map == null ? 0 : map.size());

        if (N.notNullOrEmpty(map)) {
            for (Map.Entry<K, ? extends Collection<? extends E>> entry : map.entrySet()) {
                final Collection<? extends E> c = entry.getValue();

                if (N.notNullOrEmpty(c)) {
                    for (E e : c) {
                        multimap.put(e, entry.getKey());
                    }
                }
            }
        }

        return multimap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <VV>
     * @param <M>
     * @param multimap
     * @param multimapSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>, VV extends Collection<K>, M extends Multimap<E, K, VV>> M invertFrom(final Multimap<K, E, V> multimap,
            final IntFunction<? extends M> multimapSupplier) {
        final M res = multimapSupplier.apply(multimap == null ? 0 : multimap.size());

        if (N.notNullOrEmpty(multimap)) {
            for (Map.Entry<K, V> entry : multimap.entrySet()) {
                final V c = entry.getValue();

                if (N.notNullOrEmpty(c)) {
                    for (E e : c) {
                        res.put(e, entry.getKey());
                    }
                }
            }
        }

        return res;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param a
     * @param b
     * @param multimapSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M concat(final Map<? extends K, ? extends E> a,
            final Map<? extends K, ? extends E> b, final IntFunction<? extends M> multimapSupplier) {
        final M res = multimapSupplier.apply((a == null ? 0 : a.size()) + (b == null ? 0 : b.size()));

        res.putAll(a);
        res.putAll(b);

        return res;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param a
     * @param b
     * @param c
     * @param multimapSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M concat(final Map<? extends K, ? extends E> a,
            final Map<? extends K, ? extends E> b, final Map<? extends K, ? extends E> c, final IntFunction<? extends M> multimapSupplier) {
        final M res = multimapSupplier.apply((a == null ? 0 : a.size()) + (b == null ? 0 : b.size()) + (c == null ? 0 : c.size()));

        res.putAll(a);
        res.putAll(b);
        res.putAll(c);

        return res;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @param valueSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>> Multimap<K, E, V> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier) {
        N.checkArgNotNull(map, "map");
        N.checkArgNotNull(valueSupplier, "valueSupplier");

        return new Multimap<>(map, valueSupplier);
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
     * @return true, if successful
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
     * If the specified value is not already associated with the specified key 
     * associates it with the given key and returns {@code true}, else returns {@code false}.
     *
     * @param key
     * @param e
     * @return true, if successful
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
     * @return true, if successful
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
     * @return true, if successful
     */
    public boolean putAll(final K key, final Collection<? extends E> c) {
        if (N.isNullOrEmpty(c)) {
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
     * @return true, if successful
     */
    public boolean putAllIfKeyAbsent(final K key, final Collection<? extends E> c) {
        if (N.isNullOrEmpty(c)) {
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
     * @return true, if successful
     */
    public boolean putAll(final Map<? extends K, ? extends E> m) {
        if (N.isNullOrEmpty(m)) {
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
     *
     * @param m
     * @return true, if successful
     */
    public boolean putAll(final Multimap<? extends K, ? extends E, ? extends Collection<? extends E>> m) {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        boolean result = false;
        K key = null;
        V val = null;

        for (Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            if (N.isNullOrEmpty(e.getValue())) {
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
     * @return true, if successful
     */
    public boolean remove(final Object key, final Object e) {
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
     * @return true, if successful
     */
    public boolean removeAll(final Object key, final Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        boolean result = false;
        final V val = valueMap.get(key);

        if (N.notNullOrEmpty(val)) {
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
     * listMultimap.removeAll(N.asMap("a", 2)); // -> {a=[1, 2], b=[2]}
     * 
     * SetMultimap<String, Integer> setMultimap = SetMultimap.of("a", 1, "b", 2, "a", 2); // -> {a=[1, 2, 2], b=[2]}
     * setMultimap.removeAll(N.asMap("a", 2)); // -> {a=[1], b=[2]}
     * </code>
     * </pre>
     *
     * @param m
     * @return true, if successful
     */
    public boolean removeAll(final Map<? extends K, ? extends E> m) {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        boolean result = false;
        Object key = null;
        V val = null;

        for (Map.Entry<?, ?> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (N.notNullOrEmpty(val)) {
                if (result == false) {
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
     * @param m
     * @return true, if successful
     */
    public boolean removeAll(final Multimap<?, ?, ?> m) {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        boolean result = false;
        Object key = null;
        V val = null;

        for (Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            key = e.getKey();
            val = valueMap.get(key);

            if (N.notNullOrEmpty(val) && N.notNullOrEmpty(e.getValue())) {
                if (result == false) {
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
     */
    public <X extends Exception> boolean removeIf(E value, Throwables.Predicate<? super K, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (K key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        boolean modified = false;

        for (K k : removingKeys) {
            if (remove(k, value)) {
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
     */
    public <X extends Exception> boolean removeIf(E value, Throwables.BiPredicate<? super K, ? super V, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        boolean modified = false;

        for (K k : removingKeys) {
            if (remove(k, value)) {
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
     */
    public <X extends Exception> boolean removeAllIf(Collection<?> values, Throwables.Predicate<? super K, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (K key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        boolean modified = false;

        for (K k : removingKeys) {
            if (removeAll(k, values)) {
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
     */
    public <X extends Exception> boolean removeAllIf(Collection<?> values, Throwables.BiPredicate<? super K, ? super V, X> predicate) throws X {
        Set<K> removingKeys = null;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        boolean modified = false;

        for (K k : removingKeys) {
            if (removeAll(k, values)) {
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

        if (N.isNullOrEmpty(removingKeys)) {
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

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        for (K k : removingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Replaces one of the specified <code>oldValue</code> with the specified <code>newValue</code>.
     * <code>False</code> is returned if no <code>oldValue</code> is found.
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replace(final K key, final Object oldValue, final E newValue) {
        final V val = valueMap.get(key);

        if (val == null) {
            return false;
        }

        return replace(val, oldValue, newValue);
    }

    /**
     *
     * @param val
     * @param oldValue
     * @param newValue
     * @return true, if successful
     */
    private boolean replace(final V val, final Object oldValue, final E newValue) {
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
     * Replaces all of the specified <code>oldValue</code> with the specified <code>newValue</code>.
     * <code>False</code> is returned if no <code>oldValue</code> is found.
     *
     * @param key
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     */
    public boolean replaceAll(final K key, final Collection<?> oldValues, final E newValue) {
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
     * Replace the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceIf(Throwables.Predicate<? super K, X> predicate, Object oldValue, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                modified = modified | replace(entry.getValue(), oldValue, newValue);
            }
        }

        return modified;
    }

    /**
     * Replace the specified value (one occurrence) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @param oldValue
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceIf(Throwables.BiPredicate<? super K, ? super V, X> predicate, Object oldValue, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                modified = modified | replace(entry.getValue(), oldValue, newValue);
            }
        }

        return modified;
    }

    /**
     * Replace the specified value (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceAllIf(Throwables.Predicate<? super K, X> predicate, Collection<?> oldValues, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                if (entry.getValue().removeAll(oldValues)) {
                    entry.getValue().add(newValue);
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Replace the specified value (all occurrences) from the value set associated with keys which satisfy the specified <code>predicate</code>.
     *
     * @param <X>
     * @param predicate
     * @param oldValues
     * @param newValue
     * @return <code>true</code> if this Multimap is modified by this operation, otherwise <code>false</code>.
     * @throws X the x
     */
    public <X extends Exception> boolean replaceAllIf(Throwables.BiPredicate<? super K, ? super V, X> predicate, Collection<?> oldValues, E newValue) throws X {
        boolean modified = false;

        for (Map.Entry<K, V> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                if (entry.getValue().removeAll(oldValues)) {
                    entry.getValue().add(newValue);
                    modified = true;
                }
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

            if (N.isNullOrEmpty(newVal)) {
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

        if (N.notNullOrEmpty(keyToRemove)) {
            for (K key : keyToRemove) {
                valueMap.remove(key);
            }
        }
    }

    /**
     *
     * @param key
     * @param e
     * @return true, if successful
     */
    public boolean contains(final Object key, final Object e) {
        final V val = valueMap.get(key);

        return val == null ? false : val.contains(e);
    }

    /**
     *
     * @param key
     * @return true, if successful
     */
    public boolean containsKey(final Object key) {
        return valueMap.containsKey(key);
    }

    /**
     *
     * @param e
     * @return true, if successful
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
     * @return true, if successful
     */
    public boolean containsAll(final Object key, final Collection<?> c) {
        final V val = valueMap.get(key);

        return val == null ? false : (N.isNullOrEmpty(c) ? true : val.containsAll(c));
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
    public <X extends Exception> void flatForEachValue(Throwables.Consumer<? super E, X> action) throws X {
        N.checkArgNotNull(action);

        for (V v : valueMap.values()) {
            for (E e : v) {
                action.accept(e);
            }
        }
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     * 
     * <pre>
     * final V oldValue = get(key);
     * 
     * if (N.notNullOrEmpty(oldValue)) {
     *     return oldValue;
     * }
     * 
     * final V newValue = mappingFunction.apply(key);
     * 
     * if (N.notNullOrEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * }
     * 
     * return newValue;
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

        if (N.notNullOrEmpty(oldValue)) {
            return oldValue;
        }

        final V newValue = mappingFunction.apply(key);

        if (N.notNullOrEmpty(newValue)) {
            valueMap.put(key, newValue);
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     * 
     * <pre>
     * final V oldValue = get(key);
     * 
     * if (N.isNullOrEmpty(oldValue)) {
     *     return oldValue;
     * }
     * 
     * final V newValue = remappingFunction.apply(key, oldValue);
     * 
     * if (N.notNullOrEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     valueMap.remove(key);
     * }
     * 
     * return newValue;
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

        if (N.isNullOrEmpty(oldValue)) {
            return oldValue;
        }

        final V newValue = remappingFunction.apply(key, oldValue);

        if (N.notNullOrEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else {
            valueMap.remove(key);
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     * 
     * <pre>
     * final V oldValue = get(key);
     * final V newValue = remappingFunction.apply(key, oldValue);
     * 
     * if (N.notNullOrEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     if (oldValue != null) {
     *         valueMap.remove(key);
     *     }
     * }
     * 
     * return newValue;
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

        if (N.notNullOrEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else {
            if (oldValue != null) {
                valueMap.remove(key);
            }
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     * 
     * <pre>
     * final V oldValue = get(key);
     * final V newValue = oldValue == null ? value : remappingFunction.apply(oldValue, value);
     * 
     * if (N.notNullOrEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     if (oldValue != null) {
     *         valueMap.remove(key);
     *     }
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

        if (N.notNullOrEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else {
            if (oldValue != null) {
                valueMap.remove(key);
            }
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this Multimap:
     * 
     * <pre>
     * final V oldValue = get(key);
     * 
     * if (N.isNullOrEmpty(oldValue)) {
     *     put(key, e);
     *     return get(key);
     * }
     * 
     * final V newValue = remappingFunction.apply(oldValue, e);
     * 
     * if (N.notNullOrEmpty(newValue)) {
     *     valueMap.put(key, newValue);
     * } else {
     *     if (oldValue != null) {
     *         valueMap.remove(key);
     *     }
     * }
     * 
     * return newValue;
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

        if (N.isNullOrEmpty(oldValue)) {
            put(key, e);
            return get(key);
        }

        final V newValue = remappingFunction.apply(oldValue, e);

        if (N.notNullOrEmpty(newValue)) {
            valueMap.put(key, newValue);
        } else {
            if (oldValue != null) {
                valueMap.remove(key);
            }
        }

        return newValue;
    }

    /**
     *
     * @return
     */
    public Multimap<K, E, V> copy() {
        final Multimap<K, E, V> copy = new Multimap<>(mapSupplier, valueSupplier);

        copy.putAll(this);

        return copy;
    }

    /**
     *
     * @return
     */
    public Set<K> keySet() {
        return valueMap.keySet();
    }

    /**
     *
     * @return
     */
    public Collection<V> values() {
        return valueMap.values();
    }

    /**
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
     * @return
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return valueMap.entrySet();
    }

    /**
     *
     * @return
     */
    public Map<K, V> toMap() {
        final Map<K, V> result = Maps.newOrderingMap(valueMap);

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
    public Map<K, V> unwrap() {
        return valueMap;
    }

    /**
     *
     * @return
     */
    public Stream<Map.Entry<K, V>> stream() {
        return Stream.of(valueMap.entrySet());
    }

    /**
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
     * @param <X>
     * @param action
     * @throws X the x
     */
    public <X extends Exception> void accept(Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws X {
        action.accept(this);
    }

    /**
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
     * @return true, if successful
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multimap && valueMap.equals(((Multimap<K, E, V>) obj).valueMap));
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return valueMap.toString();
    }
}
