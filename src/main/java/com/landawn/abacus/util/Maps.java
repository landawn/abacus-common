/*
 * Copyright (C) 2019 HaiYang Li
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.IntFunctions;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Strings
 */
public final class Maps {

    private Maps() {
        // Utility class.
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     */
    public static <T, K> Map<K, T> create(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper) {
        N.checkArgNotNull(keyMapper);

        if (N.isEmpty(c)) {
            return new HashMap<>();
        }

        final Map<K, T> result = N.newHashMap(c.size());

        for (T e : c) {
            result.put(keyMapper.apply(e), e);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     */
    public static <T, K, V> Map<K, V> create(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (N.isEmpty(c)) {
            return new HashMap<>();
        }

        final Map<K, V> result = N.newHashMap(c.size());

        for (T e : c) {
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> M create(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final IntFunction<? extends M> mapSupplier) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mapSupplier);

        if (N.isEmpty(c)) {
            return mapSupplier.apply(0);
        }

        final M result = mapSupplier.apply(c.size());

        for (T e : c) {
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> M create(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mergeFunction);
        N.checkArgNotNull(mapSupplier);

        if (N.isEmpty(c)) {
            return mapSupplier.apply(0);
        }

        final M result = mapSupplier.apply(c.size());
        K key = null;

        for (T e : c) {
            key = keyMapper.apply(e);

            final V oldValue = result.get(key);

            if (oldValue == null && !result.containsKey(key)) {
                result.put(key, valueExtractor.apply(e));
            } else {
                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param iter
     * @param keyMapper
     * @return
     */
    public static <T, K> Map<K, T> create(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper) {
        N.checkArgNotNull(keyMapper);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, T> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), e);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @return
     */
    public static <T, K, V> Map<K, V> create(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> M create(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final Supplier<? extends M> mapSupplier) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mapSupplier);

        if (iter == null) {
            return mapSupplier.get();
        }

        final M result = mapSupplier.get();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> M create(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapSupplier) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mergeFunction);
        N.checkArgNotNull(mapSupplier);

        if (iter == null) {
            return mapSupplier.get();
        }

        final M result = mapSupplier.get();
        T e = null;
        K key = null;

        while (iter.hasNext()) {
            e = iter.next();
            key = keyMapper.apply(e);

            final V oldValue = result.get(key);

            if (oldValue == null && !result.containsKey(key)) {
                result.put(key, valueExtractor.apply(e));
            } else {
                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
            }
        }

        return result;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param <V2>
     * @param map
     * @param valueMapper
     * @return
     */
    public static <K, V, V2> Map<K, V2> create(final Map<? extends K, ? extends V> map, final Function<? super V, V2> valueMapper) {
        N.checkArgNotNull(valueMapper);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V2> result = Maps.newTargetMap(map);

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
        }

        return result;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param <V2>
     * @param <M>
     * @param map
     * @param valueMapper
     * @param mapSupplier
     * @return
     */
    public static <K, V, V2, M extends Map<K, V2>> M create(final Map<? extends K, ? extends V> map, final Function<? super V, V2> valueMapper,
            final IntFunction<? extends M> mapSupplier) {
        N.checkArgNotNull(valueMapper);
        N.checkArgNotNull(mapSupplier);

        if (map == null) {
            return mapSupplier.apply(0);
        }

        final M result = mapSupplier.apply(map.size());

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>)} instead
     */
    @Deprecated
    public static <T, K> Map<K, T> newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper) {
        return create(c, keyMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>)} instead
     */
    @Deprecated
    public static <T, K, V> Map<K, V> newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) {
        return create(c, keyMapper, valueExtractor);
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>,IntFunction<? extends M>)} instead
     */
    @Deprecated
    public static <T, K, V, M extends Map<K, V>> M newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final IntFunction<? extends M> mapSupplier) {
        return create(c, keyMapper, valueExtractor, mapSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>,BinaryOperator<V>,IntFunction<? extends M>)} instead
     */
    @Deprecated
    public static <T, K, V, M extends Map<K, V>> M newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        return create(c, keyMapper, valueExtractor, mergeFunction, mapSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param iter
     * @param keyMapper
     * @return
     * @deprecated Use {@link #create(Iterator<? extends T>,Throwables.Function<? super T, K, E>)} instead
     */
    @Deprecated
    public static <T, K> Map<K, T> newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper) {
        return create(iter, keyMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @deprecated Use {@link #create(Iterator<? extends T>,Throwables.Function<? super T, K, E>,Function<? super T, ? extends V>)} instead
     */
    @Deprecated
    public static <T, K, V> Map<K, V> newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) {
        return create(iter, keyMapper, valueExtractor);
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     * @deprecated Use {@link #create(Iterator<? extends T>,Throwables.Function<? super T, K, E>,Function<? super T, ? extends V>,Supplier<? extends M>)} instead
     */
    @Deprecated
    public static <T, K, V, M extends Map<K, V>> M newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final Supplier<? extends M> mapSupplier) {
        return create(iter, keyMapper, valueExtractor, mapSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     * @deprecated Use {@link #create(Iterator<? extends T>,Throwables.Function<? super T, K, E>,Function<? super T, ? extends V>,BinaryOperator<V>,Supplier<? extends M>)} instead
     */
    @Deprecated
    public static <T, K, V, M extends Map<K, V>> M newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapSupplier) {
        return create(iter, keyMapper, valueExtractor, mergeFunction, mapSupplier);
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param key
     * @param value
     * @return
     * @deprecated replaced by {@link N#newEntry(Object, Object)}
     */
    @Deprecated
    public static <K, V> Map.Entry<K, V> newEntry(final K key, final V value) {
        return N.newEntry(key, value);
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param key
     * @param value
     * @return
     * @deprecated replaced by {@link N#newImmutableEntry(Object, Object)}
     */
    @Deprecated
    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(final K key, final V value) {
        return N.newImmutableEntry(key, value);
    }

    /**
     * New target map.
     *
     * @param m
     * @return
     */
    @SuppressWarnings("rawtypes")
    static Map newTargetMap(Map<?, ?> m) {
        return newTargetMap(m, m == null ? 0 : m.size());
    }

    /**
     * New target map.
     *
     * @param m
     * @param size
     * @return
     */
    @SuppressWarnings("rawtypes")
    static Map newTargetMap(Map<?, ?> m, int size) {
        if (m == null) {
            return size == 0 ? new HashMap<>() : new HashMap<>(size);
        }

        if (m instanceof SortedMap) {
            return new TreeMap<>(((SortedMap) m).comparator());
        }

        return N.newMap(m.getClass(), size);
    }

    /**
     * New ordering map.
     *
     * @param m
     * @return
     */
    @SuppressWarnings("rawtypes")
    static Map newOrderingMap(Map<?, ?> m) {
        if (m == null) {
            return new HashMap<>();
        }

        if (m instanceof SortedMap) {
            return new LinkedHashMap<>();
        }

        return N.newMap(m.getClass(), m.size());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param keys
     * @param values
     * @return
     */
    public static <K, V> Map<K, V> zip(final Collection<? extends K> keys, final Collection<? extends V> values) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys.size(), values.size());
        final Map<K, V> result = N.newHashMap(minLen);

        for (int i = 0; i < minLen; i++) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keys
     * @param values
     * @param mapSupplier
     * @return
     */
    public static <K, V, M extends Map<K, V>> Map<K, V> zip(final Collection<? extends K> keys, final Collection<? extends V> values,
            final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys.size(), values.size());
        final Map<K, V> result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keys
     * @param values
     * @param mergeFunction
     * @param mapSupplier
     * @return
     */
    public static <K, V, M extends Map<K, V>> Map<K, V> zip(final Collection<? extends K> keys, final Collection<? extends V> values,
            final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys.size(), values.size());
        final Map<K, V> result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @return
     */
    public static <K, V> Nullable<V> get(final Map<K, ? extends V> map, final K key) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return Nullable.of(val);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping for the key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    public static <K, V> V getOrDefault(final Map<K, ? extends V> map, final K key, final V defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return val;
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the value to which the specified key is mapped if it's not {@code null},
     * or {@code defaultForNull} if this map contains no mapping for the key or it's {@code null}.
     *
     * @param <K>
     * @param <V>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K, V> V getOrDefaultIfNull(final Map<K, ? extends V> map, final K key, final V defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final V val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else {
            return val;
        }
    }

    /**
     * Returns an empty {@code OptionalBoolean} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalBoolean getBoolean(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalBoolean.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalBoolean.empty();
        } else if (val instanceof Boolean) {
            return OptionalBoolean.of((Boolean) val);
        } else {
            return OptionalBoolean.of(Strings.parseBoolean(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code boolean} or a boolean converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> boolean getBoolean(final Map<? super K, ?> map, final K key, final boolean defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else {
            return Strings.parseBoolean(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalChar} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalChar getChar(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalChar.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalChar.empty();
        } else if (val instanceof Character) {
            return OptionalChar.of(((Character) val));
        } else {
            return OptionalChar.of(Strings.parseChar(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code char} or a char converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> char getChar(final Map<? super K, ?> map, final K key, final char defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Character) {
            return (Character) val;
        } else {
            return Strings.parseChar(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalByte} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalByte getByte(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalByte.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalByte.empty();
        } else if (val instanceof Number) {
            return OptionalByte.of(((Number) val).byteValue());
        } else {
            return OptionalByte.of(Numbers.toByte(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code byte} or a byte converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> byte getByte(final Map<? super K, ?> map, final K key, final byte defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).byteValue();
        } else {
            return Numbers.toByte(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalShort} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalShort getShort(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalShort.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalShort.empty();
        } else if (val instanceof Number) {
            return OptionalShort.of(((Number) val).shortValue());
        } else {
            return OptionalShort.of(Numbers.toShort(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code short} or a short converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> short getShort(final Map<? super K, ?> map, final K key, final short defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else {
            return Numbers.toShort(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalInt} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalInt getInt(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalInt.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalInt.empty();
        } else if (val instanceof Number) {
            return OptionalInt.of(((Number) val).intValue());
        } else {
            return OptionalInt.of(Numbers.toInt(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code integer} or an integer converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> int getInt(final Map<? super K, ?> map, final K key, final int defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).intValue();
        } else {
            return Numbers.toInt(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalLong} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalLong getLong(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalLong.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalLong.empty();
        } else if (val instanceof Number) {
            return OptionalLong.of(((Number) val).longValue());
        } else {
            return OptionalLong.of(Numbers.toLong(N.toString(val)));
        }
    }

    /**
     * Returns the mapped {@code long} or a long converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> long getLong(final Map<? super K, ?> map, final K key, final long defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else {
            return Numbers.toLong(N.toString(val));
        }
    }

    /**
     * Returns an empty {@code OptionalFloat} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalFloat getFloat(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalFloat.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalFloat.empty();
        } else {
            return OptionalFloat.of(Numbers.toFloat(val));
        }
    }

    /**
     * Returns the mapped {@code float} or a float converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> float getFloat(final Map<? super K, ?> map, final K key, final float defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else {
            return Numbers.toFloat(val);
        }
    }

    /**
     * Returns an empty {@code OptionalDouble} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> OptionalDouble getDouble(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalDouble.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(Numbers.toDouble(val));
        }
    }

    /**
     * Returns the mapped {@code double} or a double converted from String.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> double getDouble(final Map<? super K, ?> map, final K key, final double defaultForNull) {
        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else {
            return Numbers.toDouble(val);
        }
    }

    /**
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @return
     */
    public static <K> Optional<String> getString(final Map<? super K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (val instanceof String) {
            return Optional.of((String) val);
        } else {
            return Optional.of(N.stringOf(val));
        }
    }

    /**
     * Returns the mapped {@code String} or a {@code String} converted from {@code N.toString(value)}.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K> String getString(final Map<? super K, ?> map, final K key, final String defaultForNull) {
        N.checkArgNotNull(defaultForNull, "defaultForNull");

        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (val instanceof String) {
            return (String) val;
        } else {
            return N.stringOf(val);
        }
    }

    /**
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * <br />
     * Node: To follow one of general design rules in {@code Abacus}, if there is a conversion behind when the source value is not assignable to the target type, put the {@code targetType} to last parameter of the method.
     * Otherwise, put the {@code targetTpye} to the first parameter of the method.
     *
     * @param <K>
     * @param <T>
     * @param map
     * @param key
     * @param targetType
     * @return
     */
    public static <K, T> Optional<T> get(final Map<? super K, ?> map, final K key, final Class<? extends T> targetType) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.isAssignableFrom(val.getClass())) {
            return Optional.of((T) val);
        } else {
            return Optional.of(N.convert(val, targetType));
        }
    }

    /**
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the value is {@code null}.
     *
     * <br />
     * Node: To follow one of general design rules in {@code Abacus}, if there is a conversion behind when the source value is not assignable to the target type, put the {@code targetType} to last parameter of the method.
     * Otherwise, put the {@code targetTpye} to the first parameter of the method.
     *
     * @param <K>
     * @param <T>
     * @param map
     * @param key
     * @param targetType
     * @return
     */
    public static <K, T> Optional<T> get(final Map<? super K, ?> map, final K key, final Type<? extends T> targetType) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.clazz().isAssignableFrom(val.getClass())) {
            return Optional.of((T) val);
        } else {
            return Optional.of(N.convert(val, targetType));
        }
    }

    /**
     * Returns the mapped {@code T} or a {@code T} converted from {@code N.valueOf((Class<T>) defaultForNull.getClass(), N.stringOf(val))}.
     * {@code defaultForNull} is returned if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     *
     * @param <K>
     * @param <T>
     * @param map
     * @param key
     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
     * @return
     */
    public static <K, T> T get(final Map<? super K, ?> map, final K key, final T defaultForNull) {
        N.checkArgNotNull(defaultForNull, "defaultForNull");

        if (N.isEmpty(map)) {
            return defaultForNull;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultForNull;
        } else if (defaultForNull.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultForNull.getClass());
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * an empty immutable {@code List} if this map contains no mapping for the key.
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @param key
     * @return
     */
    public static <K, E, V extends List<E>> List<E> getOrEmptyList(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.<E> emptyList();
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return val;
        } else {
            return N.emptyList();
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * an empty immutable {@code Set} if this map contains no mapping for the key.
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param map
     * @param key
     * @return
     */
    public static <K, E, V extends Set<E>> Set<E> getOrEmptySet(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.<E> emptySet();
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return val;
        } else {
            return N.emptySet();
        }
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists in the specified {@code map} contains, or the new put {@code List} if it's absent.
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @param key
     * @return
     */
    public static <K, E> List<E> getAndPutListIfAbsent(final Map<K, List<E>> map, final K key) {
        List<E> v = map.get(key);

        if (v == null) {
            v = new ArrayList<>();
            v = map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists in the specified {@code map} contains, or the new put {@code Set} if it's absent.
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @param key
     * @return
     */
    public static <K, E> Set<E> getAndPutSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = N.newHashSet();
            v = map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists in the specified {@code map} contains, or the new put {@code Set} if it's absent.
     *
     * @param <K> the key type
     * @param <E>
     * @param map
     * @param key
     * @return
     */
    public static <K, E> Set<E> getAndPutLinkedHashSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = N.newLinkedHashSet();
            v = map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists in the specified {@code map} contains, or the new put {@code Map} if it's absent.
     *
     * @param <K> the key type
     * @param <KK>
     * @param <VV>
     * @param map
     * @param key
     * @return
     */
    public static <K, KK, VV> Map<KK, VV> getAndPutMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new HashMap<>();
            v = map.put(key, v);
        }

        return v;
    }

    /**
     * Returns a list of values of the keys which exist in the specified <code>Map</code>.
     * If the key dosn't exist in the <code>Map</code>, No value will be added into the returned list.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param keys
     * @return
     */
    public static <K, V> List<V> getIfPresentForEach(final Map<K, ? extends V> map, final Collection<?> keys) {
        if (N.isEmpty(map) || N.isEmpty(keys)) {
            return new ArrayList<>(0);
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (Object key : keys) {
            val = map.get(key);

            if (val != null || map.containsKey(key)) {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Gets the or default for each.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param keys
     * @param defaultValue
     * @return
     */
    public static <K, V> List<V> getOrDefaultForEach(final Map<K, V> map, final Collection<?> keys, final V defaultValue) {
        if (N.isEmpty(keys)) {
            return new ArrayList<>(0);
        } else if (N.isEmpty(map)) {
            return N.repeat(defaultValue, keys.size());
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (Object key : keys) {
            val = map.get(key);

            if (val != null || map.containsKey(key)) {
                result.add(val);
            } else {
                result.add(defaultValue);
            }
        }

        return result;
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param map
     * @param keys
     * @param defaultValue
     * @return
     */
    public static <K, V> List<V> getOrDefaultIfNullForEach(final Map<K, V> map, final Collection<?> keys, final V defaultValue) {
        if (N.isEmpty(keys)) {
            return new ArrayList<>(0);
        } else if (N.isEmpty(map)) {
            return N.repeat(defaultValue, keys.size());
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (Object key : keys) {
            val = map.get(key);

            if (val == null) {
                result.add(defaultValue);
            } else {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Recursively get the values from the specified {@code map} by {@code path}. For example:
     * <pre>
     * <code>
        Map map = N.asMap("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));
    
        map = N.asMap("key1", N.asList("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));
    
        map = N.asMap("key1", N.asSet("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));
    
        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", "val2")));
        assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));
    
        map = N.asMap("key1", N.asSet(N.asList(N.asSet("val1"))));
        assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));
    
        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", "val22"))));
        assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));
    
        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));
    
        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));
     * </code>
     * </pre>
     *
     * @param <T>
     * @param map
     * @param path
     * @return {@code null} if there is no value found by the specified path.
     */
    public static <T> T getByPath(final Map<?, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, N.NULL_MASK);

        if (val == N.NULL_MASK) {
            return null;
        }

        return (T) val;
    }

    /**
     *
     * @param <T>
     * @param map
     * @param path
     * @param targetType
     * @return {@code null} if there is no value found by the specified path.
     */
    public static <T> T getByPath(final Map<?, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, N.NULL_MASK);

        if (val == N.NULL_MASK) {
            return null;
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return N.convert(val, targetType);
        }
    }

    /**
     *
     * @param <T>
     * @param map
     * @param path
     * @param defaultValue
     * @return {@code defaultValue} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    @SuppressWarnings("rawtypes")
    public static <T> T getByPathOrDefault(final Map<?, ?> map, final String path, final T defaultValue) {
        N.checkArgNotNull(defaultValue, "defaultValue");

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Class<?> targetType = defaultValue == null || defaultValue == N.NULL_MASK ? null : defaultValue.getClass();

        final String[] keys = Strings.split(path, '.');
        Map intermediateMap = map;
        Collection intermediateColl = null;
        String key = null;

        for (int i = 0, len = keys.length; i < len; i++) {
            key = keys[i];

            if (N.isEmpty(intermediateMap)) {
                return defaultValue;
            }

            if (key.charAt(key.length() - 1) == ']') {
                final int[] indexes = Strings.substringsBetween(key, "[", "]").stream().mapToInt(Numbers::toInt).toArray();
                final int idx = key.indexOf('[');
                intermediateColl = (Collection) intermediateMap.get(key.substring(0, idx));

                for (int j = 0, idxLen = indexes.length; j < idxLen; j++) {
                    if (N.isEmpty(intermediateColl) || intermediateColl.size() <= indexes[j]) {
                        return defaultValue;
                    } else {
                        if (j == idxLen - 1) {
                            if (i == len - 1) {
                                final Object ret = N.getElement(intermediateColl, indexes[j]);

                                if (ret == null || targetType == null || targetType.isAssignableFrom(ret.getClass())) {
                                    return (T) ret;
                                } else {
                                    return (T) N.convert(ret, targetType);
                                }
                            } else {
                                intermediateMap = (Map) N.getElement(intermediateColl, indexes[j]);
                            }
                        } else {
                            intermediateColl = (Collection) N.getElement(intermediateColl, indexes[j]);
                        }
                    }
                }
            } else {
                if (i == len - 1) {
                    final Object ret = intermediateMap.getOrDefault(key, defaultValue);

                    if (ret == null || targetType == null || targetType.isAssignableFrom(ret.getClass())) {
                        return (T) ret;
                    } else {
                        return (T) N.convert(ret, targetType);
                    }
                } else {
                    intermediateMap = (Map) intermediateMap.get(key);
                }
            }
        }

        return defaultValue;
    }

    /**
     *
     * @param <T>
     * @param map
     * @param path
     * @return an empty {@code Nullable} if there is no value found by the specified path.
     */
    public static <T> Nullable<T> getByPathIfPresent(final Map<?, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, N.NULL_MASK);

        if (val == N.NULL_MASK) {
            return Nullable.<T> empty();
        }

        return Nullable.of((T) val);
    }

    /**
     *
     * @param <T>
     * @param map
     * @param path
     * @param targetType
     * @return an empty {@code Nullable} if there is no value found by the specified path.
     */
    public static <T> Nullable<T> getByPathIfPresent(final Map<?, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, N.NULL_MASK);

        if (val == N.NULL_MASK) {
            return Nullable.<T> empty();
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return Nullable.of((T) val);
        } else {
            return Nullable.of(N.convert(val, targetType));
        }
    }

    /**
     * Check if the specified <code>Map</code> contains the specified <code>Entry</code>.
     *
     * @param map
     * @param entry
     * @return
     */
    public static boolean contains(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        return contains(map, entry.getKey(), entry.getValue());
    }

    /**
     *
     * @param map
     * @param key
     * @param value
     * @return
     */
    public static boolean contains(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object val = map.get(key);

        return val == null ? value == null && map.containsKey(key) : N.equals(val, value);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param map2
     * @return
     */
    public static <K, V> Map<K, V> intersection(final Map<K, V> map, final Map<? extends K, ? extends V> map2) {
        if (N.isEmpty(map) || N.isEmpty(map2)) {
            return new LinkedHashMap<>();
        }

        final Map<K, V> result = map instanceof IdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();
        Object val = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            val = map2.get(entry.getKey());

            if ((val != null && N.equals(val, entry.getValue())) || (val == null && entry.getValue() == null && map.containsKey(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param map2
     * @return
     */
    public static <K, V> Map<K, Pair<V, Nullable<V>>> difference(final Map<K, V> map, final Map<K, V> map2) {
        if (N.isEmpty(map)) {
            return new LinkedHashMap<>();
        }

        final Map<K, Pair<V, Nullable<V>>> result = map instanceof IdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();

        if (N.isEmpty(map2)) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.<V> empty()));
            }
        } else {
            V val = null;

            for (Map.Entry<K, V> entry : map.entrySet()) {
                val = map2.get(entry.getKey());

                if (val == null && !map2.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.<V> empty()));
                } else if (!N.equals(val, entry.getValue())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.of(val)));
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param map2
     * @return
     */
    public static <K, V> Map<K, Pair<Nullable<V>, Nullable<V>>> symmetricDifference(final Map<K, V> map, final Map<K, V> map2) {
        final boolean isIdentityHashMap = (N.notEmpty(map) && map instanceof IdentityHashMap) || (N.notEmpty(map2) && map2 instanceof IdentityHashMap);

        final Map<K, Pair<Nullable<V>, Nullable<V>>> result = isIdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();

        if (N.notEmpty(map)) {
            if (N.isEmpty(map2)) {
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.of(entry.getValue()), Nullable.<V> empty()));
                }
            } else {
                K key = null;
                V val2 = null;

                for (Map.Entry<K, V> entry : map.entrySet()) {
                    key = entry.getKey();
                    val2 = map2.get(key);

                    if (val2 == null && !map2.containsKey(key)) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.<V> empty()));
                    } else if (!N.equals(val2, entry.getValue())) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.of(val2)));
                    }
                }
            }
        }

        if (N.notEmpty(map2)) {
            if (N.isEmpty(map)) {
                for (Map.Entry<K, V> entry : map2.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.<V> empty(), Nullable.of(entry.getValue())));
                }
            } else {
                for (Map.Entry<K, V> entry : map2.entrySet()) {
                    if (!map.containsKey(entry.getKey())) {
                        result.put(entry.getKey(), Pair.of(Nullable.<V> empty(), Nullable.of(entry.getValue())));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Put if absent.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param value
     * @return
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, K key, final V value) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, value);
        }

        return v;
    }

    /**
     * Put if absent.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param supplier
     * @return
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, K key, final Supplier<V> supplier) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, supplier.get());
        }

        return v;
    }

    /**
     * Removes the specified entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param entry
     * @return
     */
    public static <K, V> boolean remove(final Map<K, V> map, Map.Entry<?, ?> entry) {
        return remove(map, entry.getKey(), entry.getValue());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param value
     * @return
     */
    public static <K, V> boolean remove(final Map<K, V> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object curValue = map.get(key);

        if (!N.equals(curValue, value) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        map.remove(key);
        return true;
    }

    /**
     * Removes the keys.
     *
     * @param map
     * @param keysToRemove
     * @return <code>true</code> if any key/value was removed, otherwise <code>false</code>.
     */
    public static boolean removeKeys(final Map<?, ?> map, final Collection<?> keysToRemove) {
        if (N.isEmpty(map) || N.isEmpty(keysToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (Object key : keysToRemove) {
            map.remove(key);
        }

        return map.size() < originalSize;
    }

    /**
     * The the entries from the specified <code>Map</code>.
     *
     * @param map
     * @param entriesToRemove
     * @return <code>true</code> if any key/value was removed, otherwise <code>false</code>.
     */
    public static boolean removeEntries(final Map<?, ?> map, final Map<?, ?> entriesToRemove) {
        if (N.isEmpty(map) || N.isEmpty(entriesToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (Map.Entry<?, ?> entry : entriesToRemove.entrySet()) {
            if (N.equals(map.get(entry.getKey()), entry.getValue())) {
                map.remove(entry.getKey());
            }
        }

        return map.size() < originalSize;
    }

    /**
     * Removes entries from the specified {@code map} by the the specified {@code filter}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param filter
     * @return {@code true} if there are one or more than one entries removed from the specified map.
     * @throws E the e
     */
    public static <K, V, E extends Exception> boolean removeIf(final Map<K, V> map, final Throwables.Predicate<? super Map.Entry<K, V>, E> filter) throws E {
        List<K> keysToRemove = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry)) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified {@code map} by the the specified {@code filter}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param filter
     * @return {@code true} if there are one or more than one entries removed from the specified map.
     * @throws E the e
     */
    public static <K, V, E extends Exception> boolean removeIfKey(final Map<K, V> map, final Throwables.Predicate<? super K, E> filter) throws E {
        List<K> keysToRemove = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified {@code map} by the the specified {@code filter}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param filter
     * @return {@code true} if there are one or more than one entries removed from the specified map.
     * @throws E the e
     */
    public static <K, V, E extends Exception> boolean removeIfValue(final Map<K, V> map, final Throwables.Predicate<? super V, E> filter) throws E {
        List<K> keysToRemove = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     */
    public static <K, V> boolean replace(final Map<K, V> map, final K key, final V oldValue, final V newValue) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object curValue = map.get(key);

        if (!N.equals(curValue, oldValue) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        map.put(key, newValue);
        return true;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param newValue
     * @return
     */
    public static <K, V> V replace(final Map<K, V> map, final K key, final V newValue) {
        if (N.isEmpty(map)) {
            return null;
        }

        V curValue = null;

        if (((curValue = map.get(key)) != null) || map.containsKey(key)) {
            curValue = map.put(key, newValue);
        }

        return curValue;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param function
     * @throws E the e
     */
    public static <K, V, E extends Exception> void replaceAll(final Map<K, V> map, final Throwables.BiFunction<? super K, ? super V, ? extends V, E> function)
            throws E {
        N.checkArgNotNull(function);

        if (N.isEmpty(map)) {
            return;
        }

        K k = null;
        V v = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }

            // ise thrown from function is not a cme.
            v = function.apply(k, v);

            try {
                entry.setValue(v);
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
        }
    }

    // Replaced with N.forEach(Map....)

    //    public static <K, V, E extends Exception> void forEach(final Map<K, V> map, final Throwables.Consumer<? super Map.Entry<K, V>, E> action) throws E {
    //        N.checkArgNotNull(action);
    //
    //        if (N.isEmpty(map)) {
    //            return;
    //        }
    //
    //        for (Map.Entry<K, V> entry : map.entrySet()) {
    //            action.accept(entry);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <E>
    //     * @param map
    //     * @param action
    //     * @throws E the e
    //     */
    //    public static <K, V, E extends Exception> void forEach(final Map<K, V> map, final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
    //        N.checkArgNotNull(action);
    //
    //        if (N.isEmpty(map)) {
    //            return;
    //        }
    //
    //        for (Map.Entry<K, V> entry : map.entrySet()) {
    //            action.accept(entry.getKey(), entry.getValue());
    //        }
    //    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param predicate
     * @return
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> filter(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filter by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param predicate
     * @return
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> filterByKey(final Map<K, V> map, final Throwables.Predicate<? super K, E> predicate) throws E {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filter by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param map
     * @param predicate
     * @return
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> filterByValue(final Map<K, V> map, final Throwables.Predicate<? super V, E> predicate) throws E {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @see Multimap#invertFrom(Map, Supplier)
     * @see ListMultimap#invertFrom(Map)
     * @see ListMultimap#invertFrom(Map)
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param mergeOp
     * @return
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map, final BinaryOperator<K> mergeOp) {
        N.checkArgNotNull(mergeOp, "mergeOp");

        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);
        K oldVal = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            oldVal = result.get(entry.getValue());

            if (oldVal != null || result.containsKey(entry.getValue())) {
                result.put(entry.getValue(), mergeOp.apply(oldVal, entry.getKey()));
            } else {
                result.put(entry.getValue(), entry.getKey());
            }
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @see Multimap#flatInvertFrom(Map, Supplier)
     * @see ListMultimap#flatInvertFrom(Map)
     * @see SetMultimap#flatInvertFrom(Map)
     */
    public static <K, V> Map<V, List<K>> flatInvert(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, List<K>> result = newOrderingMap(map);

        for (Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            final Collection<? extends V> c = entry.getValue();

            if (N.notEmpty(c)) {
                for (V v : c) {
                    List<K> list = result.get(v);

                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(v, list);
                    }

                    list.add(entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     * {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}].
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V> List<Map<K, V>> flatToMap(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new ArrayList<>();
        }

        int maxValueSize = 0;

        for (Collection<? extends V> v : map.values()) {
            maxValueSize = N.max(maxValueSize, N.size(v));
        }

        final List<Map<K, V>> result = new ArrayList<>(maxValueSize);

        for (int i = 0; i < maxValueSize; i++) {
            result.add(newTargetMap(map));
        }

        K key = null;
        Iterator<? extends V> iter = null;

        for (Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            if (N.isEmpty(entry.getValue())) {
                continue;
            }

            key = entry.getKey();
            iter = entry.getValue().iterator();

            for (int i = 0; iter.hasNext(); i++) {
                result.get(i).put(key, iter.next());
            }
        }

        return result;
    }

    /**
     *
     * @param map
     * @return
     */
    public static Map<String, Object> flatten(Map<String, Object> map) {
        return flatten(map, Suppliers.<String, Object> ofMap());
    }

    /**
     *
     * @param <M>
     * @param map
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M flatten(Map<String, Object> map, Supplier<? extends M> mapSupplier) {
        return flatten(map, ".", mapSupplier);
    }

    /**
     *
     * @param <M>
     * @param map
     * @param delimiter
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M flatten(Map<String, Object> map, String delimiter, Supplier<? extends M> mapSupplier) {
        final M result = mapSupplier.get();

        flatten(map, null, delimiter, result);

        return result;
    }

    /**
     *
     * @param map
     * @param prefix
     * @param delimiter
     * @param output
     */
    private static void flatten(Map<String, Object> map, String prefix, String delimiter, Map<String, Object> output) {
        if (N.isEmpty(map)) {
            return;
        }

        if (Strings.isEmpty(prefix)) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), entry.getKey(), delimiter, output);
                } else {
                    output.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), prefix + delimiter + entry.getKey(), delimiter, output);
                } else {
                    output.put(prefix + delimiter + entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     *
     * @param map
     * @return
     */
    public static Map<String, Object> unflatten(Map<String, Object> map) {
        return unflatten(map, Suppliers.<String, Object> ofMap());
    }

    /**
     *
     * @param <M>
     * @param map
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M unflatten(Map<String, Object> map, Supplier<? extends M> mapSupplier) {
        return unflatten(map, ".", mapSupplier);
    }

    /**
     *
     * @param <M>
     * @param map
     * @param delimiter
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M unflatten(Map<String, Object> map, String delimiter, Supplier<? extends M> mapSupplier) {
        final M result = mapSupplier.get();
        final Splitter keySplitter = Splitter.with(delimiter);

        if (N.notEmpty(map)) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().indexOf(delimiter) >= 0) {
                    final String[] keys = keySplitter.splitToArray(entry.getKey());
                    Map<String, Object> lastMap = result;

                    for (int i = 0, to = keys.length - 1; i < to; i++) {
                        Map<String, Object> tmp = (Map<String, Object>) lastMap.get(keys[i]);

                        if (tmp == null) {
                            tmp = mapSupplier.get();
                            lastMap.put(keys[i], tmp);
                        }

                        lastMap = tmp;
                    }

                    lastMap.put(keys[keys.length - 1], entry.getValue());
                } else {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    //    /**
    //     * Map type 2 supplier.
    //     *
    //     * @param mapType
    //     * @return
    //     */
    //    @SuppressWarnings("rawtypes")
    //    static Supplier mapType2Supplier(final Class<? extends Map> mapType) {
    //        return Suppliers.ofMap(mapType);
    //    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param function
     */
    static <K, V> void replaceAll(Map<K, V> map, BiFunction<? super K, ? super V, ? extends V> function) {
        N.checkArgNotNull(function);

        try {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            }
        } catch (IllegalStateException ise) {
            throw new ConcurrentModificationException(ise);
        }
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param value
     * @param remappingFunction
     */
    public static <K, V> void merge(Map<K, V> map, K key, V value, BinaryOperator<V> remappingFunction) {
        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param m
     * @param targetClass
     * @return
     */
    public static <T> T map2Bean(final Map<String, Object> m, final Class<? extends T> targetClass) {
        return map2Bean(m, false, true, targetClass);
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param m
     * @param ignoreNullProperty
     * @param ignoreUnmatchedProperty
     * @param targetClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T map2Bean(final Map<String, Object> m, final boolean ignoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetClass) {
        checkBeanClass(targetClass);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;

        String propName = null;
        Object propValue = null;

        for (Map.Entry<String, Object> entry : m.entrySet()) {
            propName = entry.getKey();
            propValue = entry.getValue();

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, ignoreUnmatchedProperty);
            } else {
                if (propValue != null && N.typeOf(propValue.getClass()).isMap() && propInfo.type.isBean()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, ignoreNullProperty, ignoreUnmatchedProperty, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param m
     * @param selectPropNames
     * @param targetClass
     * @return
     */
    public static <T> T map2Bean(final Map<String, Object> m, final Collection<String> selectPropNames, final Class<? extends T> targetClass) {
        checkBeanClass(targetClass);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;
        Object propValue = null;

        for (String propName : selectPropNames) {
            propValue = m.get(propName);

            if (propValue == null && !m.containsKey(propName)) {
                throw new IllegalArgumentException("Property name: " + propName + " is not found in map with key set: " + m.keySet());
            }

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, false);
            } else {
                if (propValue != null && N.typeOf(propValue.getClass()).isMap() && propInfo.type.isBean()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param mList
     * @param targetClass
     * @return
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final Class<? extends T> targetClass) {
        return map2Bean(mList, false, true, targetClass);
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param mList
     * @param igoreNullProperty
     * @param ignoreUnmatchedProperty
     * @param targetClass
     * @return
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final boolean igoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetClass) {
        checkBeanClass(targetClass);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, igoreNullProperty, ignoreUnmatchedProperty, targetClass));
        }

        return beanList;
    }

    /**
     * Map to bean.
     *
     * @param <T>
     * @param mList
     * @param selectPropNames
     * @param targetClass
     * @return
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final Collection<String> selectPropNames,
            final Class<? extends T> targetClass) {
        checkBeanClass(targetClass);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, selectPropNames, targetClass));
        }

        return beanList;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @return
     */
    public static Map<String, Object> bean2Map(final Object bean) {
        return bean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, null, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static Map<String, Object> bean2Map(final Object bean, final Collection<String> selectPropNames) {
        return bean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) {
        final M resultMap = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2Map(resultMap, bean, selectPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean) {
        return bean2Map(resultMap, bean, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean, final Collection<String> selectPropNames) {
        return bean2Map(resultMap, bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean, final Collection<String> selectPropNames,
            NamingPolicy keyNamingPolicy) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2Map(resultMap, bean, true, null, keyNamingPolicy);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                propValue = propInfo.getPropValue(bean);

                if (isLowerCamelCaseOrNoChange) {
                    resultMap.put(propName, propValue);
                } else {
                    resultMap.put(keyNamingPolicy.convert(propName), propValue);
                }
            }
        }

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty) {
        return bean2Map(bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M resultMap = mapSupplier.apply(initCapacity);

        bean2Map(resultMap, bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty) {
        return bean2Map(resultMap, bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames) {
        return bean2Map(resultMap, bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M bean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames, NamingPolicy keyNamingPolicy) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if (isLowerCamelCaseOrNoChange) {
                resultMap.put(propName, propValue);
            } else {
                resultMap.put(keyNamingPolicy.convert(propName), propValue);
            }
        }

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @return
     */
    public static Map<String, Object> deepBean2Map(final Object bean) {
        return deepBean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, null, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final Collection<String> selectPropNames) {
        return deepBean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M resultMap = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBean2Map(resultMap, bean, selectPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean) {
        return deepBean2Map(resultMap, bean, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean, final Collection<String> selectPropNames) {
        return deepBean2Map(resultMap, bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            deepBean2Map(resultMap, bean, true, null, keyNamingPolicy);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        resultMap.put(propName, propValue);
                    } else {
                        resultMap.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        resultMap.put(propName, deepBean2Map(propValue, true, null, keyNamingPolicy));
                    } else {
                        resultMap.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, true, null, keyNamingPolicy));
                    }
                }
            }
        }

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty) {
        return deepBean2Map(bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M resultMap = mapSupplier.apply(initCapacity);

        deepBean2Map(resultMap, bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty) {
        return deepBean2Map(resultMap, bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames) {
        return deepBean2Map(resultMap, bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames, final NamingPolicy keyNamingPolicy) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isLowerCamelCaseOrNoChange) {
                    resultMap.put(propName, propValue);
                } else {
                    resultMap.put(keyNamingPolicy.convert(propName), propValue);
                }
            } else {
                if (isLowerCamelCaseOrNoChange) {
                    resultMap.put(propName, deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                } else {
                    resultMap.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                }
            }
        }

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean) {
        return bean2FlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, null, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final Collection<String> selectPropNames) {
        return bean2FlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M resultMap = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2FlatMap(resultMap, bean, selectPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean) {
        return bean2FlatMap(resultMap, bean, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean, final Collection<String> selectPropNames) {
        return bean2FlatMap(resultMap, bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean, final Collection<String> selectPropNames,
            NamingPolicy keyNamingPolicy) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2FlatMap(resultMap, bean, true, null, keyNamingPolicy);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        resultMap.put(propName, propValue);
                    } else {
                        resultMap.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    bean2FlatMap(resultMap, propValue, true, null, keyNamingPolicy, isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName));
                }
            }
        }

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty) {
        return bean2FlatMap(bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Bean to map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M resultMap = mapSupplier.apply(initCapacity);

        bean2FlatMap(resultMap, bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy);

        return resultMap;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean, final boolean ignoreNullProperty) {
        return bean2FlatMap(resultMap, bean, ignoreNullProperty, null);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames) {
        return bean2FlatMap(resultMap, bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param resultMap
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final M resultMap, final Object bean, final boolean ignoreNullProperty,
            final Set<String> ignoredPropNames, final NamingPolicy keyNamingPolicy) {
        return bean2FlatMap(resultMap, bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null);
    }

    static <T extends Map<String, Object>> T bean2FlatMap(final T resultMap, final Object bean, final boolean ignoreNullProperty,
            final Collection<String> ignoredPropNames, final NamingPolicy keyNamingPolicy, final String parentPropName) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final boolean isNullParentPropName = (parentPropName == null);
        final Class<?> beanClass = bean.getClass();

        String propName = null;
        Object propValue = null;

        for (PropInfo propInfo : ParserUtil.getBeanInfo(beanClass).propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isNullParentPropName) {
                    if (isLowerCamelCaseOrNoChange) {
                        resultMap.put(propName, propValue);
                    } else {
                        resultMap.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        resultMap.put(parentPropName + WD.PERIOD + propName, propValue);
                    } else {
                        resultMap.put(parentPropName + WD.PERIOD + keyNamingPolicy.convert(propName), propValue);
                    }
                }
            } else {
                if (isNullParentPropName) {
                    bean2FlatMap(resultMap, propValue, ignoreNullProperty, null, keyNamingPolicy,
                            isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName));
                } else {
                    bean2FlatMap(resultMap, propValue, ignoreNullProperty, null, keyNamingPolicy,
                            parentPropName + WD.PERIOD + (isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName)));
                }
            }
        }

        return resultMap;
    }

    /**
     * Check bean class.
     *
     * @param <T>
     * @param cls
     */
    private static <T> void checkBeanClass(final Class<T> cls) {
        if (!ClassUtil.isBeanClass(cls)) {
            throw new IllegalArgumentException("No property getter/setter method is found in the specified class: " + ClassUtil.getCanonicalClassName(cls));
        }
    }

    //    @SuppressWarnings("deprecation")
    //    @Beta
    //    public static <T> Map<String, Object> record2Map(final T record) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        return record2Map(new LinkedHashMap<>(ClassUtil.getRecordInfo(recordClass).fieldNames().size()), record);
    //    }
    //
    //    @Beta
    //    public static <T, M extends Map<String, Object>> M record2Map(final T record, final Supplier<? extends M> mapSupplier) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        return record2Map(mapSupplier.get(), record);
    //    }
    //
    //    @SuppressWarnings("deprecation")
    //    @Beta
    //    public static <T, M extends Map<String, Object>> M record2Map(final T record, final IntFunction<? extends M> mapSupplier) {
    //        if (record == null) {
    //            return null;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        return record2Map(mapSupplier.apply(ClassUtil.getRecordInfo(recordClass).fieldNames().size()), record);
    //    }
    //
    //    @Beta
    //    static <M extends Map<String, Object>> M record2Map(final M resultMap, final Object record) {
    //        if (record == null) {
    //            return resultMap;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        @SuppressWarnings("deprecation")
    //        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(recordClass);
    //
    //        try {
    //            for (Tuple5<String, Field, Method, Type<Object>, Integer> tp : recordInfo.fieldMap().values()) {
    //                resultMap.put(tp._1, tp._3.invoke(record));
    //            }
    //        } catch (IllegalAccessException | InvocationTargetException e) {
    //            // Should never happen.
    //            throw ExceptionUtil.toRuntimeException(e);
    //        }
    //
    //        return resultMap;
    //    }
    //
    //    @Beta
    //    public static <T> T map2Record(final Map<String, Object> map, final Class<T> recordClass) {
    //        @SuppressWarnings("deprecation")
    //        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(recordClass);
    //
    //        if (map == null) {
    //            return null;
    //        }
    //
    //        final Object[] args = new Object[recordInfo.fieldNames().size()];
    //        Object val = null;
    //        int idx = 0;
    //
    //        for (String fieldName : recordInfo.fieldNames()) {
    //            val = map.get(fieldName);
    //
    //            // TODO, should be ignored?
    //            //    if (val == null && !map.containsKey(tp._1)) {
    //            //        throw new IllegalArgumentException("No value found for field: " + tp._1 + " from the input map");
    //            //    }
    //
    //            args[idx++] = val;
    //        }
    //
    //        return (T) recordInfo.creator().apply(args);
    //    }

    public static class MapGetter<K, V> {

        private final Map<K, V> map;
        private final boolean defaultForPrimitive;

        MapGetter(final Map<K, V> map, final boolean defaultForPrimitive) {
            this.map = map;
            this.defaultForPrimitive = defaultForPrimitive;
        }

        /**
         *
         *
         * @param <K>
         * @param <V>
         * @param map
         * @return
         */
        public static <K, V> MapGetter<K, V> of(final Map<K, V> map) {
            return of(map, false);
        }

        /**
         *
         *
         * @param <K>
         * @param <V>
         * @param map
         * @param defaultForPrimitive
         * @return
         */
        public static <K, V> MapGetter<K, V> of(final Map<K, V> map, final boolean defaultForPrimitive) {
            return new MapGetter<>(map, defaultForPrimitive);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Boolean getBoolean(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return Boolean.FALSE;
            } else if (value instanceof Boolean) {
                return (Boolean) value;
            }

            return Strings.parseBoolean(N.toString(value));
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Character getChar(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0;
            } else if (value instanceof Character) {
                return (Character) value;
            }

            return Strings.parseChar(N.toString(value));
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Byte getByte(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0;
            } else if (value instanceof Byte) {
                return (Byte) value;
            }

            return Numbers.toByte(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Short getShort(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0;
            } else if (value instanceof Short) {
                return (Short) value;
            }

            return Numbers.toShort(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Integer getInt(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0;
            } else if (value instanceof Integer) {
                return (Integer) value;
            }

            return Numbers.toInt(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Long getLong(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0L;
            } else if (value instanceof Long) {
                return (Long) value;
            }

            return Numbers.toLong(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Float getFloat(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0F;
            } else if (value instanceof Float) {
                return (Float) value;
            }

            return Numbers.toFloat(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Double getDouble(Object key) {
            Object value = map.get(key);

            if (value == null && defaultForPrimitive) {
                return 0d;
            } else if (value instanceof Double) {
                return (Double) value;
            }

            return Numbers.toDouble(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public BigInteger getBigInteger(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof BigInteger) {
                return (BigInteger) value;
            }

            return N.convert(value, BigInteger.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public BigDecimal getBigDecimal(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof BigDecimal) {
                return (BigDecimal) value;
            }

            return N.convert(value, BigDecimal.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public String getString(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof String) {
                return (String) value;
            }

            return N.stringOf(value);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Calendar getCalendar(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof Calendar) {
                return (Calendar) value;
            }

            return N.convert(value, Calendar.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Date getJUDate(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof Date) {
                return (Date) value;
            }

            return N.convert(value, Date.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public java.sql.Date getDate(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof java.sql.Date) {
                return (java.sql.Date) value;
            }

            return N.convert(value, java.sql.Date.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public java.sql.Time getTime(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof java.sql.Time) {
                return (java.sql.Time) value;
            }

            return N.convert(value, java.sql.Time.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public java.sql.Timestamp getTimestamp(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof java.sql.Timestamp) {
                return (java.sql.Timestamp) value;
            }

            return N.convert(value, java.sql.Timestamp.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public LocalDate getLocalDate(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof LocalDate) {
                return (LocalDate) value;
            }

            return N.convert(value, LocalDate.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public LocalTime getLocalTime(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof LocalTime) {
                return (LocalTime) value;
            }

            return N.convert(value, LocalTime.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public LocalDateTime getLocalDateTime(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            return N.convert(value, LocalDateTime.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public ZonedDateTime getZonedDateTime(Object key) {
            Object value = map.get(key);

            if (value == null || value instanceof ZonedDateTime) {
                return (ZonedDateTime) value;
            }

            return N.convert(value, ZonedDateTime.class);
        }

        /**
         *
         *
         * @param key
         * @return
         */
        public Object getObject(Object key) {
            return map.get(key);
        }

        /**
         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
         *
         * <br />
         * Node: To follow one of general design rules in {@code Abacus}, if there is a conversion behind when the source value is not assignable to the target type, put the {@code targetType} to last parameter of the method.
         * Otherwise, put the {@code targetTpye} to the first parameter of the method.
         *
         * @param <T>
         * @param key
         * @param targetType
         * @return
         */
        public <T> T get(Object key, Class<? extends T> targetType) {
            final V val = map.get(key);

            if (val == null || targetType.isAssignableFrom(val.getClass())) {
                return (T) val;
            }

            return N.convert(val, targetType);
        }

        /**
         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
         *
         * <br />
         * Node: To follow one of general design rules in {@code Abacus}, if there is a conversion behind when the source value is not assignable to the target type, put the {@code targetType} to last parameter of the method.
         * Otherwise, put the {@code targetTpye} to the first parameter of the method.
         *
         * @param <T>
         * @param key
         * @param targetType
         * @return
         */
        public <T> T get(Object key, Type<? extends T> targetType) {
            final V val = map.get(key);

            if (val == null || targetType.clazz().isAssignableFrom(val.getClass())) {
                return (T) val;
            }

            return N.convert(val, targetType);
        }
    }
}
