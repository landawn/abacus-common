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
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;

/**
 * The Class ListMultimap.
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

    /**
     * Instantiates a new list multimap.
     */
    ListMultimap() {
        this(HashMap.class, ArrayList.class);
    }

    /**
     * Instantiates a new list multimap.
     *
     * @param initialCapacity
     */
    ListMultimap(int initialCapacity) {
        this(new HashMap<K, List<E>>(initialCapacity), ArrayList.class);
    }

    /**
     * Instantiates a new list multimap.
     *
     * @param mapType
     * @param valueType
     */
    @SuppressWarnings("rawtypes")
    ListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        super(mapType, valueType);
    }

    /**
     * Instantiates a new list multimap.
     *
     * @param mapSupplier
     * @param valueSupplier
     */
    ListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier, final Supplier<? extends List<E>> valueSupplier) {
        super(mapSupplier, valueSupplier);
    }

    /**
     * Instantiates a new list multimap.
     *
     * @param valueMap
     * @param valueType
     */
    @Internal
    @SuppressWarnings("rawtypes")
    ListMultimap(final Map<K, List<E>> valueMap, final Class<? extends List> valueType) {
        super(valueMap, valueType2Supplier(valueType));
    }

    /**
     * Instantiates a new list multimap.
     *
     * @param valueMap
     * @param valueSupplier
     */
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
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @return
     */
    public static <K, E> ListMultimap<K, E> from(final Map<? extends K, ? extends E> map) {
        final ListMultimap<K, E> multimap = new ListMultimap<>(Maps.newTargetMap(map), ArrayList.class);

        if (N.notNullOrEmpty(map)) {
            multimap.putAll(map);
        }

        return multimap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @return
     */
    public static <K, E> ListMultimap<K, E> fromm(final Map<? extends K, ? extends Collection<? extends E>> map) {
        final ListMultimap<K, E> multimap = new ListMultimap<>(Maps.newTargetMap(map), ArrayList.class);

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
     * @param <X>
     * @param c
     * @param keyMapper
     * @return
     * @throws X the x
     */
    public static <T, K, X extends Exception> ListMultimap<K, T> from(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends K, X> keyMapper) throws X {
        N.checkArgNotNull(keyMapper);

        final ListMultimap<K, T> multimap = N.newListMultimap(N.initHashCapacity(N.size(c)));

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
     * @param <X>
     * @param <X2>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @throws X the x
     * @throws X2 the x2
     */
    public static <T, K, E, X extends Exception, X2 extends Exception> ListMultimap<K, E> from(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends K, X> keyMapper, final Throwables.Function<? super T, ? extends E, X2> valueExtractor) throws X, X2 {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        final ListMultimap<K, E> multimap = N.newListMultimap(N.initHashCapacity(N.size(c)));

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
     * @param map
     * @return
     * @see Multimap#invertFrom(Map, com.landawn.abacus.util.function.Supplier)
     */
    public static <K, E> ListMultimap<E, K> invertFrom(final Map<K, E> map) {
        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map), ArrayList.class);

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
     * @param map
     * @return
     * @see Multimap#flatInvertFrom(Map, com.landawn.abacus.util.function.Supplier)
     */
    public static <K, E> ListMultimap<E, K> flatInvertFrom(final Map<K, ? extends Collection<? extends E>> map) {
        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map), ArrayList.class);

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
     * @param map
     * @return
     */
    public static <K, E, V extends Collection<E>> ListMultimap<E, K> invertFrom(final Multimap<K, E, V> map) {
        final ListMultimap<E, K> multimap = new ListMultimap<>(Maps.newOrderingMap(map.valueMap), ArrayList.class);

        if (N.notNullOrEmpty(map)) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                final V c = entry.getValue();

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
     * @param a
     * @param b
     * @return
     */
    public static <K, E> ListMultimap<K, E> concat(final Map<? extends K, ? extends E> a, final Map<? extends K, ? extends E> b) {
        if (a == null) {
            return b == null ? N.<K, E> newListMultimap() : from(b);
        } else {
            final ListMultimap<K, E> res = from(a);
            res.putAll(b);
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
                return c == null ? N.<K, E> newListMultimap() : from(c);
            } else {
                final ListMultimap<K, E> res = from(b);
                res.putAll(c);
                return res;
            }
        } else {
            final ListMultimap<K, E> res = from(a);
            res.putAll(b);
            res.putAll(c);
            return res;
        }
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrap(final Map<K, V> map) {
        N.checkArgNotNull(map);
        N.checkArgument(N.anyNull(map.values()), "The specified map contains null value: %s", map);

        Class<? extends List> valueType = ArrayList.class;

        for (V v : map.values()) {
            if (v != null) {
                valueType = v.getClass();
                break;
            }
        }

        return new ListMultimap<>((Map<K, List<E>>) map, valueType);
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
    @SuppressWarnings("rawtypes")
    public static <K, E, V extends List<E>> ListMultimap<K, E> wrapp(final Map<K, V> map, final Supplier<? extends V> valueSupplier) {
        N.checkArgNotNull(map, "map");
        N.checkArgNotNull(valueSupplier, "valueSupplier");

        return new ListMultimap<K, E>((Map) map, valueSupplier);
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
    @Deprecated
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M from(final Map<? extends K, ? extends E> map,
            final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M fromm(final Map<? extends K, ? extends Collection<? extends E>> map,
            final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <T, K, V extends Collection<T>, M extends Multimap<K, T, V>, X extends Exception> M from(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends K, X> keyMapper, final IntFunction<? extends M> multimapSupplier) throws X {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <T, K, E, V extends Collection<E>, M extends Multimap<K, E, V>, X extends Exception, X2 extends Exception> M from(
            final Collection<? extends T> c, final Throwables.Function<? super T, ? extends K, X> keyMapper,
            final Throwables.Function<? super T, ? extends E, X2> valueExtractor, final IntFunction<? extends M> multimapSupplier) throws X, X2 {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<K>, M extends Multimap<E, K, V>> M invertFrom(final Map<K, E> map,
            final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
     */
    @Deprecated
    public static <K, E, V extends Collection<K>, M extends Multimap<E, K, V>> M flatInvertFrom(final Map<K, ? extends Collection<? extends E>> map,
            final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<E>, VV extends Collection<K>, M extends Multimap<E, K, VV>> M invertFrom(final Multimap<K, E, V> multimap,
            final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M concat(final Map<? extends K, ? extends E> a,
            final Map<? extends K, ? extends E> b, final IntFunction<? extends M> multimapSupplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M concat(final Map<? extends K, ? extends E> a,
            final Map<? extends K, ? extends E> b, final Map<? extends K, ? extends E> c, final IntFunction<? extends M> multimapSupplier)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
    @Deprecated
    public static <K, E, V extends Collection<E>> Multimap<K, E, V> wrap(final Map<K, V> map, final Supplier<? extends V> valueSupplier)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Filter by key.
     *
     * @param <X>
     * @param filter
     * @return
     * @throws X the x
     */
    @Override
    public <X extends Exception> ListMultimap<K, E> filterByKey(Throwables.Predicate<? super K, X> filter) throws X {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, List<E>> entry : valueMap.entrySet()) {
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
    @Override
    public <X extends Exception> ListMultimap<K, E> filterByValue(Throwables.Predicate<? super List<E>, X> filter) throws X {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, List<E>> entry : valueMap.entrySet()) {
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
    @Override
    public <X extends Exception> ListMultimap<K, E> filter(Throwables.BiPredicate<? super K, ? super List<E>, X> filter) throws X {
        final ListMultimap<K, E> result = new ListMultimap<>(mapSupplier, valueSupplier);

        for (Map.Entry<K, List<E>> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                result.valueMap.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     *
     * @return
     */
    @Override
    public ListMultimap<K, E> copy() {
        final ListMultimap<K, E> copy = new ListMultimap<>(mapSupplier, valueSupplier);

        copy.putAll(this);

        return copy;
    }

    /**
     * To immutable map.
     *
     * @return
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap() {
        final Map<K, ImmutableList<E>> map = Maps.newOrderingMap(valueMap);

        for (Map.Entry<K, List<E>> entry : valueMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.of(map);
    }

    /**
     * To immutable map.
     *
     * @param mapSupplier
     * @return
     */
    public ImmutableMap<K, ImmutableList<E>> toImmutableMap(final IntFunction<? extends Map<K, ImmutableList<E>>> mapSupplier) {
        final Map<K, ImmutableList<E>> map = mapSupplier.apply(valueMap.size());

        for (Map.Entry<K, List<E>> entry : valueMap.entrySet()) {
            map.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }

        return ImmutableMap.of(map);
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
    //        if (N.notNullOrEmpty(valueMap)) {
    //            for (Map.Entry<K, ? extends List<? extends E>> entry : valueMap.entrySet()) {
    //                final List<? extends E> c = entry.getValue();
    //
    //                if (N.notNullOrEmpty(c)) {
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
