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
import java.util.Set;
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
 * @see N#newSetMultimap()
 * @see N#newSetMultimap(Class, Class)
 * @see N#newSetMultimap(Supplier, Supplier)
 */
public final class SetMultimap<K, E> extends Multimap<K, E, Set<E>> {
    /**
     * Constructs a new instance of SetMultimap with the default initial capacity.
     *
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
     * Constructs a new instance of SetMultimap with the specified map type and list type.
     *
     * @param mapType The class of the map to be used as the backing map.
     * @param valueType The class of the list to be used as the value collection.
     */
    @SuppressWarnings("rawtypes")
    SetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        super(mapType, valueType);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map supplier and list supplier.
     *
     * @param mapSupplier The supplier that provides the map to be used as the backing map.
     * @param valueSupplier The supplier that provides the list to be used as the value collection.
     */
    SetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier, final Supplier<? extends Set<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map and list type.
     * This constructor allows the user to specify a custom map and list type for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueType The class of the list to be used as the value collection.
     */
    @Internal
    @SuppressWarnings("rawtypes")
    SetMultimap(final Map<K, Set<E>> valueMap, final Class<? extends Set> valueType) {
        super(valueMap, valueType2Supplier(valueType));
    }

    /**
     * Constructs a new instance of SetMultimap with the specified map and list supplier.
     * This constructor allows the user to specify a custom map and list supplier for the backing map and value collections respectively.
     *
     * @param valueMap The map to be used as the backing map.
     * @param valueSupplier The supplier that provides the list to be used as the value collection.
     */
    @Internal
    SetMultimap(final Map<K, Set<E>> valueMap, final Supplier<? extends Set<E>> valueSupplier) {
        super(valueMap, valueSupplier);
    }

    /**
     * Creates a new instance of SetMultimap with one key-value pair.
     *
     * @param <K> the type of the key
     * @param <E> the type of the value
     * @param k1 the key of the key-value pair
     * @param v1 the value of the key-value pair
     * @return a new instance of SetMultimap with the specified key-value pair
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1) {
        final SetMultimap<K, E> map = new SetMultimap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new instance of SetMultimap with two key-value pairs.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
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
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param k1 the first key of the key-value pairs
     * @param v1 the first value of the key-value pairs
     * @param k2 the second key of the key-value pairs
     * @param v2 the second value of the key-value pairs
     * @param k3 the third key of the key-value pairs
     * @param v3 the third value of the key-value pairs
     * @return a new instance of SetMultimap with the specified key-value pairs
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
     */
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4, final K k5,
            final E v5, final K k6, final E v6, final K k7, final E v7) {
        final SetMultimap<K, E> map = new SetMultimap<>();

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
     * Creates a new instance of SetMultimap with the key-value pairs from the specified map.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param map the map containing the key-value pairs to be added to the new SetMultimap
     * @return a new instance of SetMultimap with the key-value pairs from the specified map
     */
    public static <K, E> SetMultimap<K, E> create(final Map<? extends K, ? extends E> map) {
        //noinspection rawtypes
        final SetMultimap<K, E> multimap = new SetMultimap<>(Maps.newTargetMap(map), HashSet.class);

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
    //    public static <K, E> SetMultimap<K, E> copyOf(final Map<? extends K, ? extends Collection<? extends E>> map) {
    //        final SetMultimap<K, E> multimap = new SetMultimap<>(Maps.newTargetMap(map), HashSet.class);
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
     * Creates a new instance of SetMultimap from a given collection and a key mapper function.
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the SetMultimap
     * @param c the collection of elements to be added to the SetMultimap
     * @param keyExtractor the function to generate keys for the SetMultimap
     * @return a new instance of SetMultimap with keys and values from the specified collection
     * @throws IllegalArgumentException if the keyExtractor is null
     */
    public static <T, K> SetMultimap<K, T> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor)
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
     * Creates a new instance of SetMultimap from a given collection, a key mapper function, and a value extractor function.
     *
     * @param <T> the type of the elements in the collection
     * @param <K> the type of the keys in the SetMultimap
     * @param <E> the type of the values in the SetMultimap
     * @param c the collection of elements to be added to the SetMultimap
     * @param keyExtractor the function to generate keys for the SetMultimap
     * @param valueExtractor the function to extract values for the SetMultimap
     * @return a new instance of SetMultimap with keys and values from the specified collection
     * @throws IllegalArgumentException if the keyExtractor or valueExtractor is null
     */
    public static <T, K, E> SetMultimap<K, E> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
            final Function<? super T, ? extends E> valueExtractor) throws IllegalArgumentException {
        final SetMultimap<K, E> multimap = N.newSetMultimap(N.size(c));

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
    //    public static <K, E> SetMultimap<E, K> invertFrom(final Map<K, E> map) {
    //        final SetMultimap<E, K> multimap = new SetMultimap<>(Maps.newOrderingMap(map), HashSet.class);
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
    //    public static <K, E> SetMultimap<E, K> flatInvertFrom(final Map<K, ? extends Collection<? extends E>> map) {
    //        final SetMultimap<E, K> multimap = new SetMultimap<>(Maps.newOrderingMap(map), HashSet.class);
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
    //    public static <K, E, V extends Collection<E>> SetMultimap<E, K> invertFrom(final Multimap<K, E, V> map) {
    //        final SetMultimap<E, K> multimap = new SetMultimap<>(Maps.newOrderingMap(map.valueMap), HashSet.class);
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
     * Creates a new instance of SetMultimap by concatenating the key-value pairs from two specified maps.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new SetMultimap
     * @param b the second map containing the key-value pairs to be added to the new SetMultimap
     * @return a new instance of SetMultimap with the key-value pairs from the specified maps
     */
    public static <K, E> SetMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.newSetMultimap() : create(b);
        } else {
            final SetMultimap<K, E> res = create(a);
            res.put(b);
            return res;
        }
    }

    /**
     * Creates a new instance of SetMultimap by concatenating the key-value pairs from three specified maps.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param a the first map containing the key-value pairs to be added to the new SetMultimap
     * @param b the second map containing the key-value pairs to be added to the new SetMultimap
     * @param c the third map containing the key-value pairs to be added to the new SetMultimap
     * @return a new instance of SetMultimap with the key-value pairs from the specified maps
     */
    public static <K, E> SetMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b,
            final Map<? extends K, ? extends E> c) {
        if (a == null) {
            if (b == null) {
                return c == null ? N.newSetMultimap() : create(c);
            } else {
                final SetMultimap<K, E> res = create(b);
                res.put(c);
                return res;
            }
        } else {
            final SetMultimap<K, E> res = create(a);
            res.put(b);
            res.put(c);
            return res;
        }
    }

    /**
     * Creates a new instance of SetMultimap by concatenating the key-value pairs from a collection of maps.
     *
     * @param <K> the type of the keys
     * @param <E> the type of the values
     * @param c the collection of maps containing the key-value pairs to be added to the new SetMultimap
     * @return a new instance of SetMultimap with the key-value pairs from the specified collection of maps
     */
    public static <K, E> SetMultimap<K, E> concat(final Collection<? extends Map<? extends K, ? extends E>> c) {
        if (N.isEmpty(c)) {
            return N.newSetMultimap();
        }

        final Iterator<? extends Map<? extends K, ? extends E>> iter = c.iterator();
        final SetMultimap<K, E> res = create(iter.next());

        while (iter.hasNext()) {
            res.put(iter.next());
        }

        return res;
    }

    /**
     * Wraps the provided map into a SetMultimap. Changes to the specified map will be reflected in the SetMultimap and vice versa.
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map the map to be wrapped into a SetMultimap
     * @return a new instance of SetMultimap backed by the provided map.
     * @throws IllegalArgumentException if the provided map is null
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends Set<E>> SetMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(map.values().stream().noneMatch(Fn.isEmptyC()), "The specified map contains null or empty value: %s", map);

        final Class<? extends Set> valueType = map.isEmpty() ? HashSet.class : map.values().iterator().next().getClass();

        return new SetMultimap<>((Map<K, Set<E>>) map, valueType);
    }

    /**
     * Wraps the provided map into a SetMultimap with a custom list supplier. Changes to the specified map will be reflected in the SetMultimap and vice versa.
     *
     * @param <K> the type of the keys in the map
     * @param <E> the type of the elements in the list
     * @param <V> the type of the list
     * @param map the map to be wrapped into a SetMultimap
     * @param valueSupplier the supplier that provides the list to be used as the value collection
     * @return a new instance of SetMultimap backed by the provided map.
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
     * Inverts the SetMultimap, swapping keys with values.
     *
     * @return a new instance of SetMultimap where the original keys are now values and the original values are now keys
     */
    public SetMultimap<E, K> inverse() {
        final SetMultimap<K, E> multimap = this;
        //noinspection rawtypes
        final SetMultimap<E, K> res = new SetMultimap<>(Maps.newOrderingMap(backingMap), valueSupplier);

        if (N.notEmpty(multimap)) {
            for (final Map.Entry<K, Set<E>> entry : multimap.entrySet()) {
                final Set<E> c = entry.getValue();

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
     * Returns a new SetMultimap and copies all key-value pairs from this SetMultimap to the new one.
     * The new SetMultimap has the same structural and hash characteristics as this one.
     *
     * @return A new SetMultimap containing all the key-value pairs of this SetMultimap.
     * @see #putMany(Multimap)
     */
    @Override
    public SetMultimap<K, E> copy() {
        final SetMultimap<K, E> copy = new SetMultimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Filters the SetMultimap based on the provided key filter.
     *
     * This method creates a new SetMultimap and adds all key-value pairs from the current SetMultimap that satisfy the provided key filter.
     * The new SetMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key in the SetMultimap. If the predicate returns {@code true}, the key-value pair is included in the new SetMultimap.
     * @return A new SetMultimap containing all the key-value pairs of the current SetMultimap that satisfy the provided key filter.
     */
    @Override
    public SetMultimap<K, E> filterByKey(final Predicate<? super K> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the SetMultimap based on the provided value filter.
     *
     * This method creates a new SetMultimap and adds all key-value pairs from the current SetMultimap that satisfy the provided value filter.
     * The new SetMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each value in the SetMultimap. If the predicate returns {@code true}, the key-value pair is included in the new SetMultimap.
     * @return A new SetMultimap containing all the key-value pairs of the current SetMultimap that satisfy the provided value filter.
     */
    @Override
    public SetMultimap<K, E> filterByValue(final Predicate<? super Set<E>> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the SetMultimap based on the provided key-value pair filter.
     *
     * This method creates a new SetMultimap and adds all key-value pairs from the current SetMultimap that satisfy the provided key-value pair filter.
     * The new SetMultimap has the same structural and hash characteristics as the current one.
     *
     * @param filter The predicate to be applied to each key-value pair in the SetMultimap. If the predicate returns {@code true}, the key-value pair is included in the new SetMultimap.
     * @return A new SetMultimap containing all the key-value pairs of the current SetMultimap that satisfy the provided key-value pair filter.
     */
    @Override
    public SetMultimap<K, E> filter(final BiPredicate<? super K, ? super Set<E>> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.backingMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Converts the current SetMultimap into an ImmutableMap.
     *
     * Each key-value pair in the SetMultimap is transformed into a key-ImmutableSet pair in the ImmutableMap.
     * The ImmutableSet contains all the values associated with the key in the SetMultimap.
     *
     * @return an ImmutableMap where each key is associated with an ImmutableSet of values from the original SetMultimap
     */
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap() {
        final Map<K, ImmutableSet<E>> map = Maps.newTargetMap(backingMap);

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    /**
     * Converts the current SetMultimap into an ImmutableMap using a provided map supplier.
     *
     * Each key-value pair in the SetMultimap is transformed into a key-ImmutableSet pair in the ImmutableMap.
     * The ImmutableSet contains all the values associated with the key in the SetMultimap.
     *
     * @param mapSupplier The supplier function that provides a Map instance. The function takes an integer argument which is the initial size of the map.
     * @return an ImmutableMap where each key is associated with an ImmutableSet of values from the original SetMultimap
     */
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableSet<E>>> mapSupplier) {
        final Map<K, ImmutableSet<E>> map = mapSupplier.apply(backingMap.size());

        for (final Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    // It won't work.
    //    /**
    //     * Returns a synchronized {@code SetMultimap} which shares the same internal {@code Map} with this {@code SetMultimap}.
    //     * That's to say, the changes in one of the returned {@code SetMultimap} and this {@code SetMultimap} will impact another one.
    //     *
    //     * @see Collections#synchronizedMap(Map)
    //     */
    //    @Override
    //    public SetMultimap<K, E> synchronized() {
    //        return new SetMultimap<>(Collections.synchronizedMap(valueMap), concreteValueType);
    //    }

    //    public SetMultimap<E, K> inversed() {
    //        final SetMultimap<E, K> multimap = new SetMultimap<E, K>(valueMap.getClass(), concreteValueType);
    //
    //        if (N.notEmpty(valueMap)) {
    //            for (Map.Entry<K, ? extends Set<? extends E>> entry : valueMap.entrySet()) {
    //                final Set<? extends E> c = entry.getValue();
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
