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
 *
 * @param <K> the key type
 * @param <E>
 * @see N#newListMultimap()
 * @see N#newListMultimap(Class, Class)
 * @see N#newListMultimap(Supplier, Supplier)
 */
public final class ListMultimap<K, E> extends Multimap<K, E, List<E>> {
    /**
     * Constructs a new instance of Multiset with the default initial capacity.
     *
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
     * @param <K> the type of the key
     * @param <E> the type of the value
     * @param k1 the key of the key-value pair
     * @param v1 the value of the key-value pair
     * @return a new instance of ListMultimap with the specified key-value pair
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1) {
        final ListMultimap<K, E> map = new ListMultimap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new instance of ListMultimap with two key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @return a new instance of ListMultimap with the specified key-value pairs
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
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param map the map containing the key-value pairs to be added to the new ListMultimap
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

    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @return
    //     */
    //    @Beta
    //    public static <K, E> ListMultimap<K, E> copyOf(final Map<? extends K, ? extends Collection<? extends E>> map) {
    //        final ListMultimap<K, E> multimap = new ListMultimap<>(Maps.newTargetMap(map), ArrayList.class);
    //
    //        if (N.notEmpty(map)) {
    //            for (Map.Entry<? extends K, ? extends Collection<? extends E>> entry : map.entrySet()) {
    //                multimap.putAll(entry.getKey(), entry.getValue());
    //            }
    //        }
    //
    //        return multimap;
    //    }

    /**
     * Creates a new instance of ListMultimap from a given collection and a key mapper function.
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
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the ListMultimap
     * @param <E> the type of the values in the ListMultimap
     * @param c the collection of elements to be added to the ListMultimap
     * @param keyExtractor the function to generate keys for the ListMultimap
     * @param valueExtractor the function to extract values for the ListMultimap
     * @return a new instance of ListMultimap with keys and values from the specified collection
     * @throws IllegalArgumentException if the keyExtractor or valueExtractor is null
     */
    public static <T, K, E> ListMultimap<K, E> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
            final Function<? super T, ? extends E> valueExtractor) throws IllegalArgumentException {
        final ListMultimap<K, E> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyExtractor.apply(e), valueExtractor.apply(e));
            }
        }

        return multimap;
    }

    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @return
    //     * @see Multimap#invertFrom(Map, Supplier)
    //     */
    //    public static <K, E> ListMultimap<E, K> invertFrom(final Map<K, E> map) {
    //        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map), ArrayList.class);
    //
    //        if (N.notEmpty(map)) {
    //            for (Map.Entry<K, E> entry : map.entrySet()) {
    //                multimap.put(entry.getValue(), entry.getKey());
    //            }
    //        }
    //
    //        return multimap;
    //    }
    //
    //    /**
    //     * Flat invert from.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @return
    //     * @see Multimap#flatInvertFrom(Map, Supplier)
    //     */
    //    public static <K, E> ListMultimap<E, K> flatInvertFrom(final Map<K, ? extends Collection<? extends E>> map) {
    //        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map), ArrayList.class);
    //
    //        if (N.notEmpty(map)) {
    //            for (Map.Entry<K, ? extends Collection<? extends E>> entry : map.entrySet()) {
    //                final Collection<? extends E> c = entry.getValue();
    //
    //                if (N.notEmpty(c)) {
    //                    for (E e : c) {
    //                        multimap.put(e, entry.getKey());
    //                    }
    //                }
    //            }
    //        }
    //
    //        return multimap;
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param <V> the value type
    //     * @param map
    //     * @return
    //     */
    //    public static <K, E, V extends Collection<E>> ListMultimap<E, K> invertFrom(final Multimap<K, E, V> map) {
    //        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map.valueMap), ArrayList.class);
    //
    //        if (N.notEmpty(map)) {
    //            for (Map.Entry<K, V> entry : map.entrySet()) {
    //                final V c = entry.getValue();
    //
    //                if (N.notEmpty(c)) {
    //                    for (E e : c) {
    //                        multimap.put(e, entry.getKey());
    //                    }
    //                }
    //            }
    //        }
    //
    //        return multimap;
    //    }

    /**
     * Creates a new instance of ListMultimap by concatenating the key-value pairs from two specified maps.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new ListMultimap
     * @param b the second map containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified maps
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
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new ListMultimap
     * @param b the second map containing the key-value pairs to be added to the new ListMultimap
     * @param c the third map containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified maps
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
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param c the collection of maps containing the key-value pairs to be added to the new ListMultimap
     * @return a new instance of ListMultimap with the key-value pairs from the specified collection of maps
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
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map the map to be wrapped into a ListMultimap
     * @return a new instance of ListMultimap backed by the provided map.
     * @throws IllegalArgumentException if the provided map is null
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(map.values().stream().noneMatch(Fn.isEmptyC()), "The specified map contains null or empty value: %s", map);

        final Class<? extends List> valueType = map.isEmpty() ? ArrayList.class : map.values().iterator().next().getClass();

        return new ListMultimap<>((Map<K, List<E>>) map, valueType);
    }

    /**
     * Wraps the provided map into a ListMultimap with a custom list supplier. Changes to the specified map will be reflected in the ListMultimap and vice versa.
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map the map to be wrapped into a ListMultimap
     * @param valueSupplier the supplier that provides the list to be used as the value collection
     * @return a new instance of ListMultimap backed by the provided map.
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
     * Return the first value for the given key, or {@code null} if no value is found.
     *
     * @param key The key whose associated value is to be returned.
     * @return The first value associated with the specified key, or {@code null} if the key has no associated values.
     */
    @Override
    public E getFirst(final K key) {
        final List<E> values = backingMap.get(key);
        return N.isEmpty(values) ? null : values.get(0);
    }

    /**
     * Return the first value for the given key, or {@code defaultValue} if no value is found.
     *
     * @param key The key whose associated value is to be returned.
     * @param defaultValue The default value to return if no value is associated with the key.
     * @return The first value associated with the specified key, or the default value if the key has no associated values.
     */
    @Override
    public E getFirstOrDefault(final K key, final E defaultValue) {
        final List<E> values = backingMap.get(key);
        return N.isEmpty(values) ? defaultValue : values.get(0);
    }

    /**
     * Inverts the ListMultimap, swapping keys with values.
     *
     * @return a new instance of ListMultimap where the original keys are now values and the original values are now keys
     */
    public ListMultimap<E, K> inverse() {
        final ListMultimap<K, E> multimap = this;
        //noinspection rawtypes
        final ListMultimap<E, K> res = new ListMultimap<>(Maps.newOrderingMap(backingMap), valueSupplier);

        if (N.notEmpty(multimap)) {
            for (final Map.Entry<K, List<E>> entry : multimap.entrySet()) {
                final List<E> c = entry.getValue();

                if (N.notEmpty(c)) {
                    for (final E e : c) {
                        res.put(e, entry.getKey());
                    }
                }
            }
        }

        return res;
    }

    /**
     * Returns a new ListMultimap and copies all key-value pairs from this ListMultimap to the new one.
     * The new ListMultimap has the same structural and hash characteristics as this one.
     *
     * @return A new ListMultimap containing all the key-value pairs of this ListMultimap.
     * @see #putMany(Multimap)
     */
    @Override
    public ListMultimap<K, E> copy() {
        final ListMultimap<K, E> copy = new ListMultimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Filters the ListMultimap based on the provided key filter.
     *
     * This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap that satisfy the provided key filter.
     * The new ListMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap.
     * @return A new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided key filter.
     */
    @Override
    public ListMultimap<K, E> filterByKey(final Predicate<? super K> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the ListMultimap based on the provided value filter.
     *
     * This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap that satisfy the provided value filter.
     * The new ListMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each value in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap.
     * @return A new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided value filter.
     */
    @Override
    public ListMultimap<K, E> filterByValue(final Predicate<? super List<E>> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the ListMultimap based on the provided key-value pair filter.
     *
     * This method creates a new ListMultimap and adds all key-value pairs from the current ListMultimap that satisfy the provided key-value pair filter.
     * The new ListMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key-value pair in the ListMultimap. If the predicate returns {@code true}, the key-value pair is included in the new ListMultimap.
     * @return A new ListMultimap containing all the key-value pairs of the current ListMultimap that satisfy the provided key-value pair filter.
     */
    @Override
    public ListMultimap<K, E> filter(final BiPredicate<? super K, ? super List<E>> filter) {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Converts the current ListMultimap into an ImmutableMap.
     *
     * Each key-value pair in the ListMultimap is transformed into a key-ImmutableList pair in the ImmutableMap.
     * The ImmutableList contains all the values associated with the key in the ListMultimap.
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
     * Each key-value pair in the ListMultimap is transformed into a key-ImmutableList pair in the ImmutableMap.
     * The ImmutableList contains all the values associated with the key in the ListMultimap.
     *
     * @param mapSupplier The supplier function that provides a Map instance. The function takes an integer argument which is the initial size of the map.
     * @return an ImmutableMap where each key is associated with an ImmutableList of values from the original ListMultimap
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableList<E>>> mapSupplier) {
        final Map<K, ImmutableList<E>> map = mapSupplier.apply(backingMap.size());

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    // It won't work.
    //    /**
    //     * Returns a synchronized {@code ListMultimap} which shares the same internal {@code Map} with this {@code ListMultimap}.
    //     * That's to say the changes in one of the returned {@code ListMultimap} and this {@code ListMultimap} will impact another one.
    //     *
    //     * @see Collections#synchronizedMap(Map)
    //     */
    //    @Override
    //    public ListMultimap<K, E> synchronized() {
    //        return new ListMultimap<>(Collections.synchronizedMap(valueMap), concreteValueType);
    //    }

    //    public ListMultimap<E, K> inversed() {
    //        final ListMultimap<E, K> multimap = new ListMultimap<E, K>(valueMap.getClass(), concreteValueType);
    //
    //        if (N.notEmpty(valueMap)) {
    //            for (Map.Entry<K, ? extends List<? extends E>> entry : valueMap.entrySet()) {
    //                final List<? extends E> c = entry.getValue();
    //
    //                if (N.notEmpty(c)) {
    //                    for (E e : c) {
    //                        multimap.put(e, entry.getKey());
    //                    }
    //                }
    //            }
    //        }
    //
    //        return multimap;
    //    }
}
