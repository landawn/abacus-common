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
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E>
 * @see N#newSetMultimap()
 * @see N#newSetMultimap(Class, Class)
 * @see N#newSetMultimap(Supplier, Supplier)
 * @since 0.9
 */
public final class SetMultimap<K, E> extends Multimap<K, E, Set<E>> {

    SetMultimap() {
        this(HashMap.class, HashSet.class);
    }

    SetMultimap(int initialCapacity) {
        this(N.<K, Set<E>> newHashMap(initialCapacity), HashSet.class);
    }

    @SuppressWarnings("rawtypes")
    SetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        super(mapType, valueType);
    }

    SetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier, final Supplier<? extends Set<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    @Internal
    @SuppressWarnings("rawtypes")
    SetMultimap(final Map<K, Set<E>> valueMap, final Class<? extends Set> valueType) {
        super(valueMap, valueType2Supplier(valueType));
    }

    @Internal
    SetMultimap(final Map<K, Set<E>> valueMap, final Supplier<? extends Set<E>> valueSupplier) {
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
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1) {
        final SetMultimap<K, E> map = new SetMultimap<>(1);

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
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2) {
        final SetMultimap<K, E> map = new SetMultimap<>(2);

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
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3) {
        final SetMultimap<K, E> map = new SetMultimap<>(3);

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
    public static <K, E> SetMultimap<K, E> of(final K k1, final E v1, final K k2, final E v2, final K k3, final E v3, final K k4, final E v4) {
        final SetMultimap<K, E> map = new SetMultimap<>(4);

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
     * Create a new {@code SetMultimap} with the keys/values from the specified {@code map}.
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @return
     */
    public static <K, E> SetMultimap<K, E> create(final Map<? extends K, ? extends E> map) {
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
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K> SetMultimap<K, T> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);

        final SetMultimap<K, T> multimap = N.newSetMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (T e : c) {
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
    public static <T, K, E> SetMultimap<K, E> create(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends E> valueExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        final SetMultimap<K, E> multimap = N.newSetMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (T e : c) {
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
     *
     * @param <K> the key type
     * @param <E>
     * @param a
     * @param b
     * @return
     */
    public static <K, E> SetMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.<K, E> newSetMultimap() : create(b);
        } else {
            final SetMultimap<K, E> res = create(a);
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
    public static <K, E> SetMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b,
            final Map<? extends K, ? extends E> c) {
        if (a == null) {
            if (b == null) {
                return c == null ? N.<K, E> newSetMultimap() : create(c);
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
     *
     * @param <K>
     * @param <E>
     * @param c
     * @return
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
    public static <K, E, V extends Set<E>> SetMultimap<K, E> wrap(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);
        N.checkArgument(N.anyNull(map.values()), "The specified map contains null value: %s", map);

        Class<? extends Set> valueType = HashSet.class;

        for (V v : map.values()) {
            if (v != null) {
                valueType = v.getClass();
                break;
            }
        }

        return new SetMultimap<>((Map<K, Set<E>>) map, valueType);
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
    public static <K, E, V extends Set<E>> SetMultimap<K, E> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, "map");
        N.checkArgNotNull(valueSupplier, "valueSupplier");

        return new SetMultimap<>((Map) map, valueSupplier);
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SetMultimap<E, K> inverse() {
        final SetMultimap<K, E> multimap = this;
        final SetMultimap<E, K> res = new SetMultimap<>(Maps.newOrderingMap(backingMap), (Supplier) valueSupplier);

        if (N.notEmpty(multimap)) {
            for (Map.Entry<K, Set<E>> entry : multimap.entrySet()) {
                final Set<E> c = entry.getValue();

                if (N.notEmpty(c)) {
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
     *
     * @return
     */
    @Override
    public SetMultimap<K, E> copy() {
        final SetMultimap<K, E> copy = new SetMultimap<>(mapSupplier, valueSupplier);

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
    public SetMultimap<K, E> filterByKey(Predicate<? super K> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
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
    public SetMultimap<K, E> filterByValue(Predicate<? super Set<E>> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
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
    public SetMultimap<K, E> filter(BiPredicate<? super K, ? super Set<E>> filter) {
        final SetMultimap<K, E> result = new SetMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
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
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap() {
        final Map<K, ImmutableSet<E>> map = Maps.newTargetMap(backingMap);

        for (Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    /**
     * To immutable map.
     *
     * @param mapSupplier
     * @return
     */
    public ImmutableMap<K, ImmutableSet<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableSet<E>>> mapSupplier) {
        final Map<K, ImmutableSet<E>> map = mapSupplier.apply(backingMap.size());

        for (Map.Entry<K, Set<E>> entry : backingMap.entrySet()) {
            map.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }

        return ImmutableMap.wrap(map);
    }

    // It won't work.
    //    /**
    //     * Returns a synchronized {@code SetMultimap} which shares the same internal {@code Map} with this {@code SetMultimap}.
    //     * That's to say the changes in one of the returned {@code SetMultimap} and this {@code SetMultimap} will impact another one.
    //     *
    //     * @see Collections#synchronizedMap(Map)
    //     */
    //    @Override
    //    public SetMultimap<K, E> synchronizedd() {
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
