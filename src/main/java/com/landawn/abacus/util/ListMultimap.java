/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E>
 * @see N#newListMultimap()
 * @see N#newListMultimap(Class, Class)
 * @see N#newListMultimap(Supplier, Supplier)
 * @since 0.9
 */
public final class ListMultimap<K, E> extends Multimap<K, E, List<E>> {

    ListMultimap() {
        this(HashMap.class, ArrayList.class);
    }

    ListMultimap(final int initialCapacity) {
        this(N.<K, List<E>> newHashMap(initialCapacity), ArrayList.class);
    }

    @SuppressWarnings("rawtypes")
    ListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        super(mapType, valueType);
    }

    ListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier, final Supplier<? extends List<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    @Internal
    @SuppressWarnings("rawtypes")
    ListMultimap(final Map<K, List<E>> valueMap, final Class<? extends List> valueType) {
        super(valueMap, valueType2Supplier(valueType));
    }

    @Internal
    ListMultimap(final Map<K, List<E>> valueMap, final Supplier<? extends List<E>> valueSupplier) {
        super(valueMap, valueSupplier);
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @return
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1) {
        final ListMultimap<K, E> map = new ListMultimap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2) {
        final ListMultimap<K, E> map = new ListMultimap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, E> ListMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3) {
        final ListMultimap<K, E> map = new ListMultimap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @return
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
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @return
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
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @return
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
     *
     * @param <K> the key type
     * @param <E>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @param k7
     * @param v7
     * @return
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
     * Create a new {@code ListMultimap} with the keys/values from the specified {@code map}.
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @return
     */
    public static <K, E> ListMultimap<K, E> create(final Map<? extends K, ? extends E> map) {
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
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K> ListMultimap<K, T> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);

        final ListMultimap<K, T> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyMapper.apply(e), e);
            }
        }

        return multimap;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <E>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, E> ListMultimap<K, E> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends E> valueExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        final ListMultimap<K, E> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (final T e : c) {
                multimap.put(keyMapper.apply(e), valueExtractor.apply(e));
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
     *
     * @param <K> the key type
     * @param <E>
     * @param a
     * @param b
     * @return
     */
    public static <K, E> ListMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.<K, E> newListMultimap() : create(b);
        } else {
            final ListMultimap<K, E> res = create(a);
            res.put(b);
            return res;
        }
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <K, E> ListMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b,
            final Map<? extends K, ? extends E> c) {
        if (a == null) {
            if (b == null) {
                return c == null ? N.<K, E> newListMultimap() : create(c);
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
     *
     * @param <K>
     * @param <E>
     * @param c
     * @return
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
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(N.anyNull(map.values()), "The specified map contains null value: %s", map);

        Class<? extends List> valueType = ArrayList.class;

        for (final V v : map.values()) {
            if (v != null) {
                valueType = v.getClass();
                break;
            }
        }

        return new ListMultimap<>((Map<K, List<E>>) map, valueType);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @param valueSupplier
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(valueSupplier, cs.valueSupplier);

        return new ListMultimap<>((Map) map, valueSupplier);
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ListMultimap<E, K> inverse() {
        final ListMultimap<K, E> multimap = this;
        final ListMultimap<E, K> res = new ListMultimap<>(Maps.newOrderingMap(backingMap), (Supplier) valueSupplier);

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
     *
     *
     * @return
     */
    @Override
    public ListMultimap<K, E> copy() {
        final ListMultimap<K, E> copy = new ListMultimap<>(mapSupplier, valueSupplier);

        copy.putMany(this);

        return copy;
    }

    /**
     * Filter by key.
     *
     * @param filter
     * @return
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
     * Filter by value.
     *
     * @param filter
     * @return
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
     *
     * @param filter
     * @return
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
     * To immutable map.
     *
     * @return
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap() {
        final Map<K, ImmutableList<E>> map = Maps.newTargetMap(backingMap);

        for (final Map.Entry<K, List<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    /**
     * To immutable map.
     *
     * @param mapSupplier
     * @return
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
    //    public ListMultimap<K, E> synchronizedd() {
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
