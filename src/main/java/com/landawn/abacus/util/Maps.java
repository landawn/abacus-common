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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Factory;
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
 * <br />
 * Present -> key is found in the specified map with {@code non-null} value.
 * <br />
 * Absent -> key is not found in the specified map or found with {@code null} value.
 * <br />
 * <br />
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. for example, if
 * user tries to reverse a {@code null} or empty String. the input String will be
 * returned. But exception will be thrown if try to add element to a {@code null} Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Strings
 */
public final class Maps {

    private static final Object NONE = ClassUtil.createNullMask();

    private Maps() {
        // Utility class.
    }

    /**
     * Returns an immutable/unmodifiable key set of the specified {@code map}.
     * An empty immutable/unmodifiable set is returned if the specified {@code map} is {@code null} or empty.
     *
     * @param <K>
     * @param map
     * @return
     * @deprecated replaced by {@code N.nullToEmpty(map).keySet()}
     * @see N#nullToEmpty(Map)
     */
    @Deprecated
    @Beta
    public static <K> ImmutableSet<K> keys(final Map<? extends K, ?> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : ImmutableSet.wrap(map.keySet());
    }

    /**
     * Returns an immutable/unmodifiable value collection of the specified {@code map}.
     * An empty immutable/unmodifiable collection is returned if the specified {@code map} is {@code null} or empty.
     *
     * @param <V>
     * @param map
     * @return
     * @deprecated replaced by {@code N.nullToEmpty(map).values()}
     * @see N#nullToEmpty(Map)
     */
    @Deprecated
    @Beta
    public static <V> ImmutableCollection<V> values(final Map<?, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : ImmutableCollection.wrap(map.values());
    }

    /**
     * Returns an immutable/unmodifiable entry set of the specified {@code map}.
     * An empty immutable/unmodifiable entry set is returned if the specified {@code map} is {@code null} or empty.
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     * @deprecated replaced by {@code N.nullToEmpty(map).entrySet()}
     * @see N#nullToEmpty(Map)
     */
    @Deprecated
    @Beta
    @SuppressWarnings({ "rawtypes" })
    public static <K, V> ImmutableSet<Map.Entry<K, V>> entrySet(final Map<? extends K, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : ImmutableSet.wrap((Set) map.entrySet());
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param c
    //     * @param keyExtractor
    //     * @return
    //     */
    //    public static <T, K> Map<K, T> create(Iterable<? extends T> c, final Function<? super T, ? extends K> keyExtractor) {
    //        N.checkArgNotNull(keyExtractor);
    //
    //        final Map<K, T> result = N.newHashMap(c instanceof Collection ? ((Collection<T>) c).size() : 0);
    //
    //        for (T e : c) {
    //            result.put(keyExtractor.apply(e), e);
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     */
    //    public static <T, K, V> Map<K, V> create(Iterable<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //
    //        final Map<K, V> result = N.newHashMap(c instanceof Collection ? ((Collection<T>) c).size() : 0);
    //
    //        for (T e : c) {
    //            result.put(keyExtractor.apply(e), valueExtractor.apply(e));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     */
    //    public static <T, K, V, M extends Map<K, V>> M create(Iterable<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final IntFunction<? extends M> mapSupplier) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //        N.checkArgNotNull(mapSupplier);
    //
    //        final M result = mapSupplier.apply(c instanceof Collection ? ((Collection<T>) c).size() : 0);
    //
    //        for (T e : c) {
    //            result.put(keyExtractor.apply(e), valueExtractor.apply(e));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K>
    //     * @param <V>
    //     * @param <M>
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mergeFunction
    //     * @param mapSupplier
    //     * @return
    //     */
    //    public static <T, K, V, M extends Map<K, V>> M create(Iterable<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //        N.checkArgNotNull(mergeFunction);
    //        N.checkArgNotNull(mapSupplier);
    //
    //        final M result = mapSupplier.apply(c instanceof Collection ? ((Collection<T>) c).size() : 0);
    //        K key = null;
    //
    //        for (T e : c) {
    //            key = keyExtractor.apply(e);
    //
    //            final V oldValue = result.get(key);
    //
    //            if (oldValue == null && !result.containsKey(key)) {
    //                result.put(key, valueExtractor.apply(e));
    //            } else {
    //                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param iter
    //     * @param keyExtractor
    //     * @return
    //     */
    //    public static <T, K> Map<K, T> create(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor) {
    //        N.checkArgNotNull(keyExtractor);
    //
    //        if (iter == null) {
    //            return new HashMap<>();
    //        }
    //
    //        final Map<K, T> result = new HashMap<>();
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //            result.put(keyExtractor.apply(e), e);
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     */
    //    public static <T, K, V> Map<K, V> create(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //
    //        if (iter == null) {
    //            return new HashMap<>();
    //        }
    //
    //        final Map<K, V> result = new HashMap<>();
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //            result.put(keyExtractor.apply(e), valueExtractor.apply(e));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     */
    //    public static <T, K, V, M extends Map<K, V>> M create(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final Supplier<? extends M> mapSupplier) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //        N.checkArgNotNull(mapSupplier);
    //
    //        if (iter == null) {
    //            return mapSupplier.get();
    //        }
    //
    //        final M result = mapSupplier.get();
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //            result.put(keyExtractor.apply(e), valueExtractor.apply(e));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <K>
    //     * @param <V>
    //     * @param <M>
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mergeFunction
    //     * @param mapSupplier
    //     * @return
    //     */
    //    public static <T, K, V, M extends Map<K, V>> M create(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapSupplier) {
    //        N.checkArgNotNull(keyExtractor);
    //        N.checkArgNotNull(valueExtractor);
    //        N.checkArgNotNull(mergeFunction);
    //        N.checkArgNotNull(mapSupplier);
    //
    //        if (iter == null) {
    //            return mapSupplier.get();
    //        }
    //
    //        final M result = mapSupplier.get();
    //        T e = null;
    //        K key = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //            key = keyExtractor.apply(e);
    //
    //            final V oldValue = result.get(key);
    //
    //            if (oldValue == null && !result.containsKey(key)) {
    //                result.put(key, valueExtractor.apply(e));
    //            } else {
    //                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param <V2>
    //     * @param map
    //     * @param valueExtractor
    //     * @return
    //     */
    //    public static <K, V, V2> Map<K, V2> create(final Map<? extends K, ? extends V> map, final Function<? super V, V2> valueExtractor) {
    //        N.checkArgNotNull(valueExtractor);
    //
    //        if (map == null) {
    //            return new HashMap<>();
    //        }
    //
    //        final Map<K, V2> result = Maps.newTargetMap(map);
    //
    //        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
    //            result.put(entry.getKey(), valueExtractor.apply(entry.getValue()));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param <V2>
    //     * @param <M>
    //     * @param map
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     */
    //    public static <K, V, V2, M extends Map<K, V2>> M create(final Map<? extends K, ? extends V> map, final Function<? super V, V2> valueExtractor,
    //            final IntFunction<? extends M> mapSupplier) {
    //        N.checkArgNotNull(valueExtractor);
    //        N.checkArgNotNull(mapSupplier);
    //
    //        if (map == null) {
    //            return mapSupplier.apply(0);
    //        }
    //
    //        final M result = mapSupplier.apply(map.size());
    //
    //        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
    //            result.put(entry.getKey(), valueExtractor.apply(entry.getValue()));
    //        }
    //
    //        return result;
    //    }

    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param c
    //     * @param keyExtractor
    //     * @return
    //     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K> Map<K, T> newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor) {
    //        return create(c, keyExtractor);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V> Map<K, V> newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor) {
    //        return create(c, keyExtractor, valueExtractor);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>,IntFunction<? extends M>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V, M extends Map<K, V>> M newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final IntFunction<? extends M> mapSupplier) {
    //        return create(c, keyExtractor, valueExtractor, mapSupplier);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K>
    //     * @param <V>
    //     * @param <M>
    //     * @param c
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mergeFunction
    //     * @param mapSupplier
    //     * @return
    //     * @deprecated Use {@link #create(Collection<? extends T>,Function<? super T, ? extends K>,Function<? super T, ? extends V>,BinaryOperator<V>,IntFunction<? extends M>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V, M extends Map<K, V>> M newMap(Collection<? extends T> c, final Function<? super T, ? extends K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
    //        return create(c, keyExtractor, valueExtractor, mergeFunction, mapSupplier);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param iter
    //     * @param keyExtractor
    //     * @return
    //     * @deprecated Use {@link #create(Iterator<? extends T>,Function<? super T, K, E>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K> Map<K, T> newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor) {
    //        return create(iter, keyExtractor);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @deprecated Use {@link #create(Iterator<? extends T>,Function<? super T, K, E>,Function<? super T, ? extends V>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V> Map<K, V> newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor) {
    //        return create(iter, keyExtractor, valueExtractor);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @deprecated Use {@link #create(Iterator<? extends T>,Function<? super T, K, E>,Function<? super T, ? extends V>,Supplier<? extends M>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V, M extends Map<K, V>> M newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final Supplier<? extends M> mapSupplier) {
    //        return create(iter, keyExtractor, valueExtractor, mapSupplier);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <K>
    //     * @param <V>
    //     * @param <M>
    //     * @param iter
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mergeFunction
    //     * @param mapSupplier
    //     * @return
    //     * @deprecated Use {@link #create(Iterator<? extends T>,Function<? super T, K, E>,Function<? super T, ? extends V>,BinaryOperator<V>,Supplier<? extends M>)} instead
    //     */
    //    @Deprecated
    //    public static <T, K, V, M extends Map<K, V>> M newMap(final Iterator<? extends T> iter, final Function<? super T, K> keyExtractor,
    //            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapSupplier) {
    //        return create(iter, keyExtractor, valueExtractor, mergeFunction, mapSupplier);
    //    }

    /**
     * Creates a new entry (key-value pair) with the provided key and value.
     *
     * <p>This method generates a new entry using the provided key and value.
     * The created entry is mutable, meaning that its key and value can be changed after creation.
     *
     * @param key The key of the new entry.
     * @param value The value of the new entry.
     * @return A new Entry with the provided key and value.
     * @deprecated replaced by {@link N#newEntry(Object, Object)}
     */
    @Deprecated
    public static <K, V> Map.Entry<K, V> newEntry(final K key, final V value) {
        return N.newEntry(key, value);
    }

    /**
     * Creates a new immutable entry with the provided key and value.
     *
     * <p>This method generates a new immutable entry (key-value pair) using the provided key and value.
     * The created entry is immutable, meaning that its key and value cannot be changed after creation.
     *
     * @param key The key of the new entry.
     * @param value The value of the new entry.
     * @return A new ImmutableEntry with the provided key and value.
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
    static Map newTargetMap(final Map<?, ?> m) {
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
    static Map newTargetMap(final Map<?, ?> m, final int size) {
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
    static Map newOrderingMap(final Map<?, ?> m) {
        if (m == null) {
            return new HashMap<>();
        }

        if (m instanceof SortedMap) {
            return new LinkedHashMap<>();
        }

        return N.newMap(m.getClass(), m.size());
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     *
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return new HashMap<>();
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final Map<K, V> result = N.newHashMap(minLen);

        for (int i = 0; i < minLen; i++) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     *
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param <M> The type of the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the smaller Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     * If there are duplicate keys, the merge function is used to resolve the conflict.
     *
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param <M> The type of the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @param mergeFunction A function used to resolve conflicts if there are duplicate keys.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int minLen = N.min(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(minLen);

        for (int i = 0; i < minLen; i++) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the longer Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     * If a key or value is not present in the Iterable, the corresponding default value is used.
     *
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param <M> The type of the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @param defaultForKey The default key to be used if the keys Iterable is shorter than the values Iterable.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K defaultForKey, final V defaultForValue) {
        return zip(keys, values, defaultForKey, defaultForValue, Fn.selectFirst(), Factory.ofMap());
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * The Iterables should be of the same length. If they are not, the resulting Map will have the size of the longer Iterable.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     * If there are duplicate keys, the merge function is used to resolve the conflict.
     * If a key or value is not present in the Iterable, the corresponding default value is used.
     *
     * @param <K> The type of keys in the resulting Map.
     * @param <V> The type of values in the resulting Map.
     * @param <M> The type of the resulting Map.
     * @param keys An Iterable of keys for the resulting Map.
     * @param values An Iterable of values for the resulting Map.
     * @param defaultForKey The default key to be used if the keys Iterable is shorter than the values Iterable.
     * @param defaultForValue The default value to be used if the values Iterable is shorter than the keys Iterable.
     * @param mergeFunction A function used to resolve conflicts if there are duplicate keys.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A Map where each key from the <i>keys</i> Iterable is associated with the corresponding value from the <i>values</i> Iterable.
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K defaultForKey,
            final V defaultForValue, final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return mapSupplier.apply(0);
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int maxLen = N.max(keys instanceof Collection ? ((Collection<K>) keys).size() : 0,
                values instanceof Collection ? ((Collection<V>) values).size() : 0);
        final M result = mapSupplier.apply(maxLen);

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        while (keyIter.hasNext()) {
            result.merge(keyIter.next(), defaultForValue, mergeFunction);
        }

        while (valueIter.hasNext()) {
            result.merge(defaultForKey, valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Returns a {@code Nullable} with the value to which the specified key is mapped,
     * or an empty {@code Nullable} if the specified map is empty or contains no mapping for the key.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return a {@code Nullable<V>} with the value mapped by the specified key, or an empty {@code Nullable<V>} if the map is empty or contains no value for the key
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
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

    //    /**
    //     * Returns the value to which the specified key is mapped if the value not {@code null},
    //     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsent(Map, Object, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getOrDefaultIfNull(final Map<K, ? extends V> map, final K key, final V defaultForNull) throws IllegalArgumentException {
    //        return getOrDefaultIfAbsent(map, key, defaultForNull);
    //    }

    /**
     * Returns a {@code Nullable} with the value to which the specified {@code key/k2} is mapped,
     * or an empty {@code Nullable} if the specified map is empty or contains no mapping for the key.
     *
     * @param <K> the type of the outer map's keys
     * @param <K2> the type of the inner map's keys
     * @param <V2> the type of the inner map's values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated map is to be returned
     * @param k2 the key whose associated value in the inner map is to be returned
     * @return a {@code Nullable<V2>} with the value mapped by the specified key and k2, or an empty {@code Nullable<V2>} if the map is empty or contains no value for the key
     * @see #getOrDefaultIfAbsent(Map, Object, Object, Object)
     */
    public static <K, K2, V2> Nullable<V2> get(final Map<K, ? extends Map<K2, V2>> map, final K key, final K2 k2) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<K2, V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null || m2.containsKey(k2)) {
                return Nullable.of(v2);
            }
        }

        return Nullable.empty();
    }

    /**
     * Returns the value to which the specified key is mapped if the value is not {@code null},
     * or {@code defaultValue} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default value to return if the map is empty, contains no value for the key, or the value is null
     * @return the value mapped by the specified key, or {@code defaultValue} if the map is empty, contains no value for the key, or the value is null
     * @throws IllegalArgumentException if specified {@code defaultValue} is null
     * @see #get(Map, Object)
     * @see #getNonNull(Map, Object, Object)
     */
    public static <K, V> V getOrDefaultIfAbsent(final Map<K, ? extends V> map, final K key, final V defaultValue) {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            return defaultValue;
        } else {
            return val;
        }
    }

    //    /**
    //     * Returns the value to which the specified key is mapped if the value not {@code null},
    //     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @param defaultForNull to return if the specified {@code map} doesn't contain the specified {@code key} or the mapped value is {@code null}.
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsent(Map, Object, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getOrDefaultIfNull(final Map<K, ? extends V> map, final K key, final V defaultForNull) throws IllegalArgumentException {
    //        return getOrDefaultIfAbsent(map, key, defaultForNull);
    //    }

    /**
     * Returns the value to which the specified key and k2 are mapped if the value is not {@code null},
     * or {@code defaultValue} if the specified map is empty or contains no value for the key and k2 or the mapping value is {@code null}.
     *
     * @param <K> the type of the outer map's keys
     * @param <K2> the type of the inner map's keys
     * @param <V2> the type of the inner map's values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated map is to be returned
     * @param k2 the key whose associated value in the inner map is to be returned
     * @param defaultValue the default value to return if the map is empty, contains no value for the key and k2, or the value is null
     * @return the value mapped by the specified key and k2, or {@code defaultValue} if the map is empty, contains no value for the key and k2, or the value is null
     * @throws IllegalArgumentException if specified {@code defaultValue} is null
     * @see #get(Map, Object, Object)
     */
    public static <K, K2, V2> V2 getOrDefaultIfAbsent(final Map<K, ? extends Map<K2, V2>> map, final K key, final K2 k2, final V2 defaultValue) {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Map<K2, V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null) {
                return v2;
            }
        }

        return defaultValue;
    }

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code List} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptyListIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E, V extends List<E>> List<E> getOrEmptyListIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptyListIfAbsent(map, key);
    //    }

    /**
     * Returns the value to which the specified key is mapped, or
     * an empty immutable/unmodifiable {@code List} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the type of keys maintained by the map
     * @param <E> the type of elements in the list
     * @param <V> the type of list values maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the value mapped by the specified key, or an empty immutable/unmodifiable {@code List} if the map is empty, contains no value for the key, or the value is null
     * @see N#emptyList()
     */
    public static <K, E, V extends List<E>> List<E> getOrEmptyListIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.<E> emptyList();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.emptyList();
        }

        return val;
    }

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code Set} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptySetIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E, V extends Set<E>> Set<E> getOrEmptySetIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptySetIfAbsent(map, key);
    //    }

    /**
     * Returns the value to which the specified key is mapped, or
     * an empty immutable/unmodifiable {@code Set} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the type of keys maintained by the map
     * @param <E> the type of elements in the set
     * @param <V> the type of set values maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the value mapped by the specified key, or an empty immutable/unmodifiable {@code Set} if the map is empty, contains no value for the key, or the value is null
     * @see N#emptySet()
     */
    public static <K, E, V extends Set<E>> Set<E> getOrEmptySetIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.<E> emptySet();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.emptySet();
        }

        return val;
    }

    //    /**
    //     * Returns the value to which the specified key is mapped, or
    //     * an empty immutable/unmodifiable {@code Map} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
    //     *
    //     * @param <K> the key type
    //     * @param <KK> the key type of value map
    //     * @param <VV> the value type of value map
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getOrEmptyMapIfAbsent(Map<K, V>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV, V extends Map<KK, VV>> Map<KK, VV> getOrEmptyMapIfNull(final Map<K, V> map, final K key) {
    //        return getOrEmptyMapIfAbsent(map, key);
    //    }

    /**
     * Returns the value to which the specified key is mapped, or
     * an empty immutable/unmodifiable {@code Map} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the type of keys maintained by the outer map
     * @param <KK> the type of keys maintained by the inner map
     * @param <VV> the type of values maintained by the inner map
     * @param <V> the type of map values maintained by the outer map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return the value mapped by the specified key, or an empty immutable/unmodifiable {@code Map} if the map is empty, contains no value for the key, or the value is null
     * @see N#emptyMap()
     */
    public static <K, KK, VV, V extends Map<KK, VV>> Map<KK, VV> getOrEmptyMapIfAbsent(final Map<K, V> map, final K key) {
        if (N.isEmpty(map)) {
            return N.<KK, VV> emptyMap();
        }

        final V val = map.get(key);

        if (val == null) {
            return N.<KK, VV> emptyMap();
        }

        return val;
    }

    /**
     * Returns an empty {@code OptionalBoolean} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalBoolean} with the value mapped by the specified {@code key}.
     * If the mapped value is not Boolean type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Boolean type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalChar} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalChar} with the value mapped by the specified {@code key}.
     * If the mapped value is not Character type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Character type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalByte} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalByte} with the value mapped by the specified {@code key}.
     * If the mapped value is not Byte/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Byte/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalShort} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalShort} with the value mapped by the specified {@code key}.
     * If the mapped value is not Short/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Short/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalInt} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalInt} with the value mapped by the specified {@code key}.
     * If the mapped value is not Integer/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Integer/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalLong} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalLong} with the value mapped by the specified {@code key}.
     * If the mapped value is not Long/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Long/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalFloat} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalFloat} with the value mapped by the specified {@code key}.
     * If the mapped value is not Float/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Float/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code OptionalDouble} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code OptionalDouble} with the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underline conversion will be executed.
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
     * Returns the specified {@code defaultForNull} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns the value mapped by the specified {@code key}.
     * If the mapped value is not Double/Number type, underline conversion will be executed.
     *
     * @param <K>
     * @param map
     * @param key
     * @param defaultForNull
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
     * Returns an empty {@code Optional<String>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<String>} with the value mapped by the specified {@code key}.
     * If the mapped value is not String type, underline conversion will be executed.
     *
     * @param <K> the type of keys maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return an {@code Optional<String>} with the value mapped by the specified key, or an empty {@code Optional<String>} if the map is empty, contains no value for the key, or the value is null
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
     * Returns the value to which the specified key is mapped if the value is not {@code null},
     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of String type, underline conversion will be executed by {@code N.stringOf(value)}.
     *
     * @param <K> the type of keys maintained by the map
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null
     * @return the value mapped by the specified key, or {@code defaultForNull} if the map is empty, contains no value for the key, or the value is null
     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
     */
    public static <K> String getString(final Map<? super K, ?> map, final K key, final String defaultForNull) throws IllegalArgumentException {
        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR

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
     * Returns an empty {@code Optional<T>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<T>} with the value mapped by the specified {@code key}.
     * If the mapped value is not {@code T} type, underline conversion will be executed by {@code N.convert(val, targetType)}.
     *
     * @param <K> the type of keys maintained by the map
     * @param <T> the type of the value
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param targetType the target type to which the value should be converted
     * @return an {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getNonNull(final Map<? super K, ?> map, final K key, final Class<? extends T> targetType) {
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
     * Returns an empty {@code Optional<T>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
     * Otherwise returns an {@code Optional<T>} with the value mapped by the specified {@code key}.
     * If the mapped value is not {@code T} type, underline conversion will be executed by {@code N.convert(val, targetType)}.
     *
     * @param <K> the type of keys maintained by the map
     * @param <T> the type of the value
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param targetType the target type to which the value should be converted
     * @return an {@code Optional<T>} with the value mapped by the specified key, or an empty {@code Optional<T>} if the map is empty, contains no value for the key, or the value is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getNonNull(final Map<? super K, ?> map, final K key, final Type<? extends T> targetType) {
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
     * Returns the value to which the specified {@code key} is mapped if the value is not {@code null},
     * or {@code defaultForNull} if the specified map is empty or contains no value for the key or the mapping value is {@code null}.
     * If the mapped value is not of type {@code T}, an underlying conversion will be executed.
     *
     * @param <K> the type of keys maintained by the map
     * @param <T> the type of the value
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @param defaultForNull the default value to return if the map is empty, contains no value for the key, or the value is {@code null}, must not be null
     * @return the value to which the specified key is mapped, or {@code defaultForNull} if the map is empty, contains no value for the key, or the value is null
     * @throws IllegalArgumentException if {@code defaultForNull} is null
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> T getNonNull(final Map<? super K, ?> map, final K key, final T defaultForNull) throws IllegalArgumentException {
        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR

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

    //    /**
    //     * Returns an empty {@code Optional<V>} if the specified {@code map} is empty, or no value found by the specified {@code key}, or the mapping value is {@code null}.
    //     * Otherwise returns an {@code Optional<V>} with the value mapped by the specified {@code key}.
    //     *
    //     * <br />
    //     * Present -> key is found in the specified map with {@code non-null} value.
    //     *
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated inconsistent with {@link #getNonNull(Map, Object, Object)
    //     */
    //    @Deprecated
    //    public static <K, V> u.Optional<V> getNonNull(final Map<K, ? extends V> map, final K key) {
    //        if (N.isEmpty(map)) {
    //            return u.Optional.empty();
    //        }
    //
    //        final V val = map.get(key);
    //
    //        if (val == null) {
    //            return u.Optional.empty();
    //        } else {
    //            return u.Optional.of(val);
    //        }
    //    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new value got from {@code defaultValueSupplier} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @param defaultValueSupplier
    //     * @return
    //     * @deprecated Use {@link #getAndPutIfAbsent(Map, Object, Supplier)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V getAndPutIfNull(final Map<K, V> map, final K key, Supplier<? extends V> defaultValueSupplier) {
    //        V val = map.get(key);
    //
    //        if (val == null) {
    //            val = defaultValueSupplier.get(); // Objects.requireNonNull(defaultValueSupplier.get());
    //            val = map.put(key, val);
    //        }
    //
    //        return val;
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new value obtained from {@code defaultValueSupplier} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @param defaultValueSupplier the supplier to provide a default value if the key is absent, may be null
     * @return the value associated with the specified key, or a new value from {@code defaultValueSupplier} if the key is absent
     */
    public static <K, V> V getAndPutIfAbsent(final Map<K, V> map, final K key, final Supplier<? extends V> defaultValueSupplier) {
        V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            val = defaultValueSupplier.get(); // Objects.requireNonNull(defaultValueSupplier.get());
            val = map.put(key, val);
        }

        return val;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code List} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutListIfAbsent(Map<K, List<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> List<E> getAndPutListIfNull(final Map<K, List<E>> map, final K key) {
    //        return getAndPutListIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code List} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <E> the element type of the list
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @return the value associated with the specified key, or a new {@code List} if the key is absent
     */
    public static <K, E> List<E> getAndPutListIfAbsent(final Map<K, List<E>> map, final K key) {
        List<E> v = map.get(key);

        if (v == null) {
            v = new ArrayList<>();
            v = map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code Set} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutSetIfAbsent(Map<K, Set<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> Set<E> getAndPutSetIfNull(final Map<K, Set<E>> map, final K key) {
    //        return getAndPutSetIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Set} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <E> the element type of the set
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @return the value associated with the specified key, or a new {@code Set} if the key is absent
     */
    public static <K, E> Set<E> getAndPutSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new HashSet<>();
            v = map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code LinkedHashSet} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutLinkedHashSetIfAbsent(Map<K, Set<E>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, E> Set<E> getAndPutLinkedHashSetIfNull(final Map<K, Set<E>> map, final K key) {
    //        return getAndPutLinkedHashSetIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashSet} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <E> the element type of the set
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @return the value associated with the specified key, or a new {@code LinkedHashSet} if the key is absent
     */
    public static <K, E> Set<E> getAndPutLinkedHashSetIfAbsent(final Map<K, Set<E>> map, final K key) {
        Set<E> v = map.get(key);

        if (v == null) {
            v = new LinkedHashSet<>();
            v = map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code Map} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <KK>
    //     * @param <VV>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutMapIfAbsent(Map<K, Map<KK, VV>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV> Map<KK, VV> getAndPutMapIfNull(final Map<K, Map<KK, VV>> map, final K key) {
    //        return getAndPutMapIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Map} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <KK> the key type of the value map
     * @param <VV> the value type of the value map
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @return the value associated with the specified key, or a new {@code Map} if the key is absent
     */
    public static <K, KK, VV> Map<KK, VV> getAndPutMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new HashMap<>();
            v = map.put(key, v);
        }

        return v;
    }

    //    /**
    //     * Returns the value associated with the specified {@code key} if it exists and not {@code null} in the specified {@code map}, Otherwise puts a new {@code LinkedHashMap} and returns it.
    //     *
    //     * @param <K> the key type
    //     * @param <KK>
    //     * @param <VV>
    //     * @param map
    //     * @param key
    //     * @return
    //     * @deprecated Use {@link #getAndPutLinkedHashMapIfAbsent(Map<K, Map<KK, VV>>,K)} instead
    //     */
    //    @Deprecated
    //    public static <K, KK, VV> Map<KK, VV> getAndPutLinkedHashMapIfNull(final Map<K, Map<KK, VV>> map, final K key) {
    //        return getAndPutLinkedHashMapIfAbsent(map, key);
    //    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashMap} and returns it.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <KK> the key type of the value map
     * @param <VV> the value type of the value map
     * @param map the map to check and possibly update
     * @param key the key to check for, may be null
     * @return the value associated with the specified key, or a new {@code LinkedHashMap} if the key is absent
     */
    public static <K, KK, VV> Map<KK, VV> getAndPutLinkedHashMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) {
        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new LinkedHashMap<>();
            v = map.put(key, v);
        }

        return v;
    }

    /**
     * Returns a list of values of the keys which exist in the specified {@code Map}.
     * If the key doesn't exist in the {@code Map} or associated value is {@code null}, no value will be added into the returned list.
     *
     * <br />
     * Present -> key is found in the specified map with {@code non-null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map to check for keys
     * @param keys the collection of keys to check in the map
     * @return a list of values corresponding to the keys found in the map
     */
    public static <K, V> List<V> getIfPresentForEach(final Map<K, ? extends V> map, final Collection<?> keys) throws IllegalArgumentException {
        if (N.isEmpty(map) || N.isEmpty(keys)) {
            return new ArrayList<>();
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
            val = map.get(key);

            if (val != null) {
                result.add(val);
            }
        }

        return result;
    }

    //    /**
    //     *
    //     *
    //     * @param <K>
    //     * @param <V>
    //     * @param map
    //     * @param keys
    //     * @param defaultForNull
    //     * @return
    //     * @throws IllegalArgumentException if the specified {@code defaultForNull} is {@code null}
    //     * @deprecated Use {@link #getOrDefaultIfAbsentForEach(Map, Collection, Object)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> List<V> getOrDefaultIfNullForEach(final Map<K, V> map, final Collection<?> keys, final V defaultForNull) {
    //        N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR
    //
    //        if (N.isEmpty(keys)) {
    //            return new ArrayList<>();
    //        } else if (N.isEmpty(map)) {
    //            return N.repeat(defaultForNull, keys.size());
    //        }
    //
    //        final List<V> result = new ArrayList<>(keys.size());
    //        V val = null;
    //
    //        for (Object key : keys) {
    //            val = map.get(key);
    //
    //            if (val == null) {
    //                result.add(defaultForNull);
    //            } else {
    //                result.add(val);
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     * Gets the or default for each.
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param keys
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> List<V> getOrDefaultIfAbsentForEach(final Map<K, V> map, final Collection<?> keys, final V defaultValue)
            throws IllegalArgumentException {
        // N.checkArgNotNull(defaultValue, "defaultValue"); // NOSONAR

        if (N.isEmpty(keys)) {
            return new ArrayList<>();
        } else if (N.isEmpty(map)) {
            return N.repeat(defaultValue, keys.size());
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
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
     * Retrieves a value from a nested map structure using a dot-separated path. For example:
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
    @MayReturnNull
    public static <T> T getByPath(final Map<?, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return null;
        }

        return (T) val;
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path.
     *
     * @param <T>
     * @param map
     * @param path
     * @param targetType
     * @return {@code null} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    @MayReturnNull
    public static <T> T getByPath(final Map<?, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return null;
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return N.convert(val, targetType);
        }
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path.
     * If the path does not exist in the map, the provided default value is returned.
     *
     * @param <T>
     * @param map
     * @param path
     * @param defaultValue
     * @return {@code defaultValue} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> T getByPath(final Map<?, ?> map, final String path, final T defaultValue) {
        // N.checkArgNotNull(defaultValue, "defaultValue");

        final Object val = getByPathOrDefault(map, path, defaultValue);

        if (val == null || defaultValue.getClass().isAssignableFrom(val.getClass())) {
            return (T) val;
        } else {
            return (T) N.convert(val, defaultValue.getClass());
        }
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path. If the value exists, it is returned wrapped in a {@code Nullable} object.
     *
     * @param <T>
     * @param map
     * @param path
     * @return an empty {@code Nullable} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<?, ?> map, final String path) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return Nullable.<T> empty();
        }

        return Nullable.of((T) val);
    }

    /**
     * Retrieves a value from a nested map structure using a dot-separated path. If the value exists, it is returned wrapped in a {@code Nullable} object.
     *
     * @param <T>
     * @param map
     * @param path
     * @param targetType
     * @return an empty {@code Nullable} if there is no value found by the specified path.
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<?, ?> map, final String path, final Class<? extends T> targetType) {
        final Object val = getByPathOrDefault(map, path, NONE);

        if (val == NONE) {
            return Nullable.<T> empty();
        }

        if (val == null || targetType.isAssignableFrom(val.getClass())) {
            return Nullable.of((T) val);
        } else {
            return Nullable.of(N.convert(val, targetType));
        }
    }

    @SuppressWarnings("rawtypes")
    private static Object getByPathOrDefault(final Map<?, ?> map, final String path, final Object defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Class<?> targetType = defaultValue == null || defaultValue == NONE ? null : defaultValue.getClass();

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
                                    return ret;
                                } else {
                                    return N.convert(ret, targetType);
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
                        return ret;
                    } else {
                        return N.convert(ret, targetType);
                    }
                } else {
                    intermediateMap = (Map) intermediateMap.get(key);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Checks if the specified map contains the specified entry.
     *
     * @param map the map to check, may be null
     * @param entry the entry to check for, may be null
     * @return {@code true} if the map contains the specified entry, {@code false} otherwise
     */
    public static boolean contains(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        return contains(map, entry.getKey(), entry.getValue());
    }

    /**
     * Checks if the specified map contains the specified key-value pair.
     *
     * @param map the map to be checked
     * @param key the key whose presence in the map is to be tested
     * @param value the value whose presence in the map is to be tested
     * @return {@code true} if the map contains the specified key-value pair, {@code false} otherwise
     */
    public static boolean contains(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object val = map.get(key);

        return val == null ? value == null && map.containsKey(key) : N.equals(val, value);
    }

    /**
     * Returns the intersection of two maps.
     * The intersection contains the entries that are present in both input maps.
     * The returned map's keys and values are those of the first input map.
     *
     * @param <K> The type of keys maintained by the returned map
     * @param <V> The type of mapped values in the returned map
     * @param map The first input map
     * @param map2 The second input map
     * @return A new map which is the intersection of the input maps
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see #commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <K, V> Map<K, V> intersection(final Map<K, V> map, final Map<?, ?> map2) {
        if (N.isEmpty(map) || N.isEmpty(map2)) {
            return new LinkedHashMap<>();
        }

        final Map<K, V> result = map instanceof IdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();
        Object val = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            val = map2.get(entry.getKey());

            if ((val != null && N.equals(val, entry.getValue())) || (val == null && entry.getValue() == null && map.containsKey(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Calculates the difference between two maps.
     * The difference is defined as a map where each entry's key exists in the first map,
     * and the entry's value is a pair consisting of the value from the first map and the value from the second map.
     * If a key exists in the first map but not in the second, the value from the second map in the pair is an empty {@code Nullable}.
     *
     * @param <K> The type of keys in the maps.
     * @param <V> The type of values in the maps.
     * @param map The first map to compare.
     * @param map2 The second map to compare.
     * @return A map representing the difference between the two input maps.
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Difference.MapDifference#of(Map, Map)
     */
    public static <K, V> Map<K, Pair<V, Nullable<V>>> difference(final Map<K, V> map, final Map<K, V> map2) {
        if (N.isEmpty(map)) {
            return new LinkedHashMap<>();
        }

        final Map<K, Pair<V, Nullable<V>>> result = map instanceof IdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();

        if (N.isEmpty(map2)) {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.<V> empty()));
            }
        } else {
            V val = null;

            for (final Map.Entry<K, V> entry : map.entrySet()) {
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
     * Calculates the symmetric difference between two maps.
     * The symmetric difference is defined as a map where each entry's key exists in either the first map or the second map, but not in both. Or the key exists in both but has different values.
     * The entry's value is a pair consisting of the value from the first map and the value from the second map.
     * If a key exists in the first map but not in the second, the value from the second map in the pair is an empty {@code Nullable}.
     * If a key exists in the second map but not in the first, the value from the first map in the pair is an empty {@code Nullable}.
     *
     * @param <K> The type of keys in the maps.
     * @param <V> The type of values in the maps.
     * @param map The first map to compare.
     * @param map2 The second map to compare.
     * @return A map representing the symmetric difference between the two input maps.
     * @see #symmetricDifference(int[], int[])
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see #difference(Collection, Collection)
     * @see Difference.MapDifference#of(Map, Map)
     * @see Difference#of(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     */
    public static <K, V> Map<K, Pair<Nullable<V>, Nullable<V>>> symmetricDifference(final Map<K, V> map, final Map<K, V> map2) {
        final boolean isIdentityHashMap = (N.notEmpty(map) && map instanceof IdentityHashMap) || (N.notEmpty(map2) && map2 instanceof IdentityHashMap);

        final Map<K, Pair<Nullable<V>, Nullable<V>>> result = isIdentityHashMap ? new IdentityHashMap<>() : new LinkedHashMap<>();

        if (N.notEmpty(map)) {
            if (N.isEmpty(map2)) {
                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.of(entry.getValue()), Nullable.<V> empty()));
                }
            } else {
                K key = null;
                V val2 = null;

                for (final Map.Entry<K, V> entry : map.entrySet()) {
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
                for (final Map.Entry<K, V> entry : map2.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.<V> empty(), Nullable.of(entry.getValue())));
                }
            } else {
                for (final Map.Entry<K, V> entry : map2.entrySet()) {
                    if (!map.containsKey(entry.getKey())) {
                        result.put(entry.getKey(), Pair.of(Nullable.<V> empty(), Nullable.of(entry.getValue())));
                    }
                }
            }
        }

        return result;
    }

    //    /**
    //     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @param value
    //     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
    //     * @see Map#putIfAbsent(Object, Object)
    //     * @deprecated Use {@link #putIfAbsent(Map<K, V>,K,V)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V putIfAbsentOrNull(final Map<K, V> map, K key, final V value) {
    //        return putIfAbsent(map, key, value);
    //    }
    //
    //    /**
    //     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param key
    //     * @param supplier
    //     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
    //     * @see Map#putIfAbsent(Object, Object)
    //     * @deprecated Use {@link #putIfAbsent(Map<K, V>,K,Supplier<V>)} instead
    //     */
    //    @Deprecated
    //    public static <K, V> V putIfAbsentOrNull(final Map<K, V> map, K key, final Supplier<V> supplier) {
    //        return putIfAbsent(map, key, supplier);
    //    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param value
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     * @see Map#putIfAbsent(Object, Object)
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final V value) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, value);
        }

        return v;
    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     *
     * <br />
     * Absent -> key is not found in the specified map or found with {@code null} value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @param key
     * @param supplier
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     * @see Map#putIfAbsent(Object, Object)
     */
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final Supplier<V> supplier) {
        V v = map.get(key);

        if (v == null) {
            v = map.put(key, supplier.get());
        }

        return v;
    }

    /**
     * Removes the specified entry from the map.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which the entry is to be removed
     * @param entry the entry to be removed from the map
     * @return {@code true} if the entry was removed, {@code false} otherwise
     * @see Map#remove(Object, Object)
     */
    public static <K, V> boolean remove(final Map<K, V> map, final Map.Entry<?, ?> entry) {
        return remove(map, entry.getKey(), entry.getValue());
    }

    /**
     * Removes the specified key-value pair from the map.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which the entry is to be removed
     * @param key the key whose associated value is to be removed
     * @param value the value to be removed
     * @return {@code true} if the entry was removed, {@code false} otherwise
     * @see Map#remove(Object, Object)
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
     * Removes the specified keys from the map.
     *
     * @param map the map from which the keys are to be removed
     * @param keysToRemove the collection of keys to be removed from the map
     * @return {@code true} if any keys were removed, {@code false} otherwise
     */
    public static boolean removeKeys(final Map<?, ?> map, final Collection<?> keysToRemove) {
        if (N.isEmpty(map) || N.isEmpty(keysToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (final Object key : keysToRemove) {
            map.remove(key);
        }

        return map.size() < originalSize;
    }

    /**
     * Removes the specified entries from the map.
     *
     * @param map the map from which the entries are to be removed
     * @param entriesToRemove the map containing the entries to be removed
     * @return {@code true} if any entries were removed, {@code false} otherwise
     */
    public static boolean removeEntries(final Map<?, ?> map, final Map<?, ?> entriesToRemove) {
        if (N.isEmpty(map) || N.isEmpty(entriesToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (final Map.Entry<?, ?> entry : entriesToRemove.entrySet()) {
            if (N.equals(map.get(entry.getKey()), entry.getValue())) {
                map.remove(entry.getKey());
            }
        }

        return map.size() < originalSize;
    }

    /**
     * Removes entries from the specified map that match the given filter.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which entries to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry)) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given filter.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which entries to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final BiPredicate<? super K, ? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given key filter.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which keys to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
     */
    public static <K, V> boolean removeIfKey(final Map<K, V> map, final Predicate<? super K> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given value filter.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which entries are to be removed
     * @param filter the predicate used to determine which values to remove
     * @return {@code true} if one or more entries were removed, {@code false} otherwise
     * @throws IllegalArgumentException if the filter is null
     */
    public static <K, V> boolean removeIfValue(final Map<K, V> map, final Predicate<? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); // NOSONAR

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(7);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entry is to be replaced
     * @param key the key with which the specified value is associated
     * @param oldValue the expected current value associated with the specified key
     * @param newValue the new value to be associated with the specified key
     * @return {@code true} if the value was replaced, {@code false} otherwise
     * @see Map#replace(Object, Object, Object)
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
     * Replaces the entry for the specified key with the new value.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entry is to be replaced
     * @param key the key with which the specified value is associated
     * @param newValue the new value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     */
    @MayReturnNull
    public static <K, V> V replace(final Map<K, V> map, final K key, final V newValue) throws IllegalArgumentException {
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
     * Replaces each entry's value with the result of applying the given function to that entry.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map in which the entries are to be replaced
     * @param function the function to apply to each entry to compute a new value
     * @throws IllegalArgumentException if the function is null
     */
    public static <K, V> void replaceAll(final Map<K, V> map, final BiFunction<? super K, ? super V, ? extends V> function) throws IllegalArgumentException {
        N.checkArgNotNull(function);

        if (N.isEmpty(map)) {
            return;
        }

        try {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            }
        } catch (final IllegalStateException ise) {
            // this usually means the entry is no longer in the map.
            throw new ConcurrentModificationException(ise);
        }
    }

    // Replaced with N.forEach(Map....)

    //    public static <K, V> void forEach(final Map<K, V> map, final Consumer<? super Map.Entry<K, V>, E> action)  {
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
    //     * @param map
    //     * @param action
    //     */
    //    public static <K, V> void forEach(final Map<K, V> map, final BiConsumer<? super K, ? super V, E> action)  {
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
     * Filters the entries of the specified map based on the given predicate.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the entries
     * @return a new map containing only the entries that match the predicate
     * @throws IllegalArgumentException if the predicate is null
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given predicate.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the entries
     * @return a new map containing only the entries that match the predicate
     * @throws IllegalArgumentException if the predicate is
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given key predicate.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the keys
     * @return a new map containing only the entries with keys that match the predicate
     * @throws IllegalArgumentException if the predicate is null
     */
    public static <K, V> Map<K, V> filterByKey(final Map<K, V> map, final Predicate<? super K> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given value predicate.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map to be filtered
     * @param predicate the predicate used to filter the values
     * @return a new map containing only the entries with values that match the predicate
     * @throws IllegalArgumentException if the predicate is null
     */
    public static <K, V> Map<K, V> filterByValue(final Map<K, V> map, final Predicate<? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); // NOSONAR

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, 0);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * Note: This method does not check for duplicate values in the input map. If there are duplicate values,
     * some information may be lost in the inversion process as each value in the resulting map must be unique.
     *
     * @param <K> The key type of the input map and the value type of the resulting map.
     * @param <V> The value type of the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @return A new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * If there are duplicate values in the input map, the merging operation specified by mergeOp is applied.
     *
     * @param <K> The key type of the input map and the value type of the resulting map.
     * @param <V> The value type of the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @param mergeOp The merging operation to be applied if there are duplicate values in the input map.
     * @return A new map which is the inverted version of the input map.
     * @throws IllegalArgumentException if mergeOp is {@code null}.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map, final BiFunction<? super K, ? super K, ? extends K> mergeOp) throws IllegalArgumentException {
        N.checkArgNotNull(mergeOp, cs.mergeOp);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);
        K oldVal = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
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
     * Inverts the given map by mapping each value in the Collection to the corresponding key.
     * The resulting map's keys are the values in the Collection of the input map and its values are Lists of the corresponding keys from the input map.
     *
     * @param <K> The key type of the input map and the element type of the List values in the resulting map.
     * @param <V> The element type of the Collection values in the input map and the key type of the resulting map.
     * @param map The map to be inverted.
     * @return A new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, List<K>> flatInvert(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, List<K>> result = newOrderingMap(map);

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            final Collection<? extends V> c = entry.getValue();

            if (N.notEmpty(c)) {
                for (final V v : c) {
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
     * Transforms a map of collections into a list of maps.
     * Each resulting map is a "flat" representation of the original map's entries, where each key in the original map
     * is associated with one element from its corresponding collection.
     * The transformation is done in a way that the first map in the resulting list contains the first elements of all collections,
     * the second map contains the second elements, and so on.
     * If the collections in the original map are of different sizes, the resulting list's size is equal to the size of the largest collection.
     *
     * @implSpec {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}].
     *
     * @param <K> The type of keys in the input map and the resulting maps.
     * @param <V> The type of values in the collections of the input map and the values in the resulting maps.
     * @param map The input map, where each key is associated with a collection of values.
     * @return A list of maps, where each map represents a "flat" version of the original map's entries.
     */
    public static <K, V> List<Map<K, V>> flatToMap(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new ArrayList<>();
        }

        int maxValueSize = 0;

        for (final Collection<? extends V> v : map.values()) {
            maxValueSize = N.max(maxValueSize, N.size(v));
        }

        final List<Map<K, V>> result = new ArrayList<>(maxValueSize);

        for (int i = 0; i < maxValueSize; i++) {
            result.add(newTargetMap(map));
        }

        K key = null;
        Iterator<? extends V> iter = null;

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
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
     * Flattens the given map.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     *
     * @param map The map to be flattened.
     * @return A new map which is the flattened version of the input map.
     */
    public static Map<String, Object> flatten(final Map<String, Object> map) {
        return flatten(map, Suppliers.<String, Object> ofMap());
    }

    /**
     * Flattens the given map using a provided map supplier.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends Map with String keys and Object values.
     * @param map The map to be flattened.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the flattened version of the input map.
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final Supplier<? extends M> mapSupplier) {
        return flatten(map, ".", mapSupplier);
    }

    /**
     * Flattens the given map using a provided map supplier and a delimiter.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a provided delimiter.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends Map with String keys and Object values.
     * @param map The map to be flattened.
     * @param delimiter The delimiter to be used when concatenating keys.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the flattened version of the input map.
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final String delimiter, final Supplier<? extends M> mapSupplier) {
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
    private static void flatten(final Map<String, Object> map, final String prefix, final String delimiter, final Map<String, Object> output) {
        if (N.isEmpty(map)) {
            return;
        }

        if (Strings.isEmpty(prefix)) {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), entry.getKey(), delimiter, output);
                } else {
                    output.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    flatten((Map<String, Object>) entry.getValue(), prefix + delimiter + entry.getKey(), delimiter, output);
                } else {
                    output.put(prefix + delimiter + entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Unflattens the given map.
     * This method takes a flattened map where keys are concatenated with a dot and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * @param map The flattened map to be unflattened.
     * @return A new map which is the unflattened version of the input map.
     */
    public static Map<String, Object> unflatten(final Map<String, Object> map) {
        return unflatten(map, Suppliers.<String, Object> ofMap());
    }

    /**
     * Unflattens the given map using a provided map supplier.
     * This method takes a flattened map where keys are concatenated with a delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends Map with String keys and Object values.
     * @param map The flattened map to be unflattened.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the unflattened version of the input map.
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final Supplier<? extends M> mapSupplier) {
        return unflatten(map, ".", mapSupplier);
    }

    /**
     * Unflattens the given map using a provided map supplier and a delimiter.
     * This method takes a flattened map where keys are concatenated with a specified delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     *
     * @param <M> The type of the map to be returned. It extends Map with String keys and Object values.
     * @param map The flattened map to be unflattened.
     * @param delimiter The delimiter that was used in the flattening process to concatenate keys.
     * @param mapSupplier A supplier function that provides a new instance of the map to be returned.
     * @return A new map which is the unflattened version of the input map.
     * @throws IllegalArgumentException if the delimiter is not found in the map's keys.
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final String delimiter, final Supplier<? extends M> mapSupplier)
            throws IllegalArgumentException {
        final M result = mapSupplier.get();
        final Splitter keySplitter = Splitter.with(delimiter);

        if (N.notEmpty(map)) {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
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

    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param map
    //     * @param function
    //     */
    //    static <K, V> void replaceAll(Map<K, V> map, BiFunction<? super K, ? super V, ? extends V> function) {
    //        N.checkArgNotNull(function);
    //
    //        try {
    //            for (Map.Entry<K, V> entry : map.entrySet()) {
    //                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
    //            }
    //        } catch (IllegalStateException ise) {
    //            throw new ConcurrentModificationException(ise);
    //        }
    //    }

    /**
     * Merges the given value with the existing value (if any) in the map for the given key.
     * The merging operation is performed using the provided remapping function.
     *
     * @param <K> The type of keys in the map.
     * @param <V> The type of values in the map.
     * @param map The map where the merging operation will be performed.
     * @param key The key whose value is to be merged with the given value.
     * @param value The value to be merged with the existing value for the given key in the map.
     * @param remappingFunction The function to be used for merging the existing and the given values.
     * @throws IllegalArgumentException if the map or remappingFunction is {@code null}.
     */
    public static <K, V> void merge(final Map<K, V> map, final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction, cs.remappingFunction);

        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    /**
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * The targetType parameter specifies the type of the bean object to be returned.
     *
     * @param <T> The type of the bean object to be returned.
     * @param m The map to be converted into a bean object.
     * @param targetType The type of the bean object to be returned.
     * @return A bean object of the specified type with its properties set to the values from the map.
     * @see #map2Bean(Map, Collection, Class)
     */
    public static <T> T map2Bean(final Map<String, Object> m, final Class<? extends T> targetType) {
        return map2Bean(m, false, true, targetType);
    }

    /**
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * The targetType parameter specifies the type of the bean object to be returned.
     *
     * @param <T>
     * @param m
     * @param ignoreNullProperty
     * @param ignoreUnmatchedProperty
     * @param targetType
     * @return A bean object of the specified type with its properties set to the values from the map.
     * @see #map2Bean(Map, Collection, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T map2Bean(final Map<String, Object> m, final boolean ignoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;

        String propName = null;
        Object propValue = null;

        for (final Map.Entry<String, Object> entry : m.entrySet()) {
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
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values, and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     *
     * @param <T> The type of the bean object to be returned.
     * @param m The map to be converted into a bean object.
     * @param selectPropNames A collection of property names to be included in the resulting bean objects.
     * @param targetType The type of the bean object to be returned.
     * @return A bean object of the specified type with its properties set to the values from the map.
     */
    @MayReturnNull
    public static <T> T map2Bean(final Map<String, Object> m, final Collection<String> selectPropNames, final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        PropInfo propInfo = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
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
     * Converts a collection of maps into a list of bean objects of the specified type.
     * Each map in the collection represents a bean object where the map's keys are the property names and the values are the corresponding property values.
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     * @see #map2Bean(Collection, Collection, Class)
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final Class<? extends T> targetType) {
        return map2Bean(mList, false, true, targetType);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type.
     * Each map in the collection represents a bean object where the map's keys are the property names and the values are the corresponding property values.
     * The resulting list contains bean objects of the specified type with their properties set to the values from the corresponding map.
     * The igoreNullProperty parameter allows the user to specify whether {@code null} properties should be ignored.
     * The ignoreUnmatchedProperty parameter allows the user to specify whether unmatched properties should be ignored.
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param igoreNullProperty A boolean that determines whether {@code null} properties should be ignored.
     * @param ignoreUnmatchedProperty A boolean that determines whether unmatched properties should be ignored.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final boolean igoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, igoreNullProperty, ignoreUnmatchedProperty, targetType));
        }

        return beanList;
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type.
     * This method takes a collection of maps where each map represents a bean object. The keys in the map are the property names and the values are the corresponding property values.
     *
     * @param <T> The type of the bean objects to be returned.
     * @param mList The collection of maps to be converted into bean objects.
     * @param selectPropNames A collection of property names to be included in the resulting bean objects. If this is empty, all properties are included.
     * @param targetType The type of the bean objects to be returned.
     * @return A list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<Map<String, Object>> mList, final Collection<String> selectPropNames,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, selectPropNames, targetType));
        }

        return beanList;
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     *
     * @param bean The bean object to be converted into a map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean) {
        return bean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map using the provided map supplier.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The map supplier function determines the type of the map to be returned.
     *
     * @param <M> The type of the resulting Map.
     * @param bean The bean object to be converted into a map.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     *
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames A collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @return A map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final Collection<String> selectPropNames) {
        return bean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * The map supplier function determines the type of the map to be returned.
     *
     * @param <M> The type of the resulting Map.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames A collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @param mapSupplier A function that generates a new Map instance.
     * @return A map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames The collection of property names to be included in the map.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param mapSupplier The supplier function to create a new instance of the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param output
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final M output) {
        bean2Map(bean, null, output);
    }

    /**
     * Bean to map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param output
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param selectPropNames The set of property names to be included during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                propValue = propInfo.getPropValue(bean);

                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            }
        }
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty) {
        return bean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The map is created by the provided <i>mapSupplier</i>.
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param mapSupplier A function that returns a new map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The keys of the map are formatted according to the provided <i>keyNamingPolicy</i>.
     *
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param keyNamingPolicy The policy used to name the keys in the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames The set of property names to be ignored during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param mapSupplier The supplier function to create a new instance of the map.
     * @return A map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * The result is stored in the provided output map.
     *
     * @param <M> The type of the output map.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param output The map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * @param <M> The type of the output map.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param output The map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * @param <M> The type of the map to be filled.
     * @param bean The bean object to be converted into a map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames The set of property names to be ignored during the conversion.
     * @param keyNamingPolicy The naming policy to be used for the keys in the map.
     * @param output The map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            NamingPolicy keyNamingPolicy, final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if (isLowerCamelCaseOrNoChange) {
                output.put(propName, propValue);
            } else {
                output.put(keyNamingPolicy.convert(propName), propValue);
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param bean The bean to be converted into a Map.
     * @return A Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean) {
        return deepBean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param mapSupplier
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param bean
     * @param selectPropNames
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final Collection<String> selectPropNames) {
        return deepBean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param mapSupplier
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M> The type of the Map to which the bean will be converted.
     * @param bean The bean to be converted into a Map.
     * @param selectPropNames A collection of property names to be included during the conversion process.
     * @param keyNamingPolicy The naming policy to be used for the keys in the resulting Map.
     * @param mapSupplier A supplier function to create the Map instance into which the bean properties will be put.
     * @return A Map representation of the provided bean.
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param output
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final M output) {
        deepBean2Map(bean, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param output
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @param output
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            deepBean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, deepBean2Map(propValue, true, null, keyNamingPolicy));
                    } else {
                        output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, true, null, keyNamingPolicy));
                    }
                }
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty) {
        return deepBean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param mapSupplier
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param output
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        deepBean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param output
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion process can be customized by specifying properties to ignore and whether to ignore {@code null} properties.
     *
     * @param <M> The type of the Map to which the bean will be converted.
     * @param bean The bean to be converted into a Map.
     * @param ignoreNullProperty If {@code true}, properties of the bean with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames A set of property names to be ignored during the conversion process.
     * @param output The Map instance into which the bean properties will be put.
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : beanInfo.propInfoList) {
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
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            } else {
                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                } else {
                    output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param bean The bean object to be converted into a flat map.
     * @return A map representing the bean object. Each key-value pair in the map corresponds to a property of the bean.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean) {
        return bean2FlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param mapSupplier
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param bean
     * @param selectPropNames
     * @return
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final Collection<String> selectPropNames) {
        return bean2FlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param mapSupplier
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and a specified naming policy.
     * This method takes a bean object and transforms it into a map where the keys are the property names of the bean and the values are the corresponding property values.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}.
     *
     * @implSpec
     * <code>
     *  Maps.bean2FlatMap(new User("John", new Address("New York"))) ==> {"name"="John", address.city="New York"};
     *  <br />
     *  // with below bean classes
     *  <br />
     *  public static class User {
     *     private String name;
     *     private Address address
     *  }
     *  <br/>
     *  public static class Address {
     *      private String city;
     *  }
     * </code>
     *
     * @param <M> The type of the map to be returned.
     * @param bean The bean object to be converted into a flat map.
     * @param selectPropNames A collection of property names to be included in the resulting map. If this is empty, all properties are included.
     * @param keyNamingPolicy The naming policy for the keys in the resulting map.
     * @param mapSupplier A function that generates a new map instance. The function argument is the initial map capacity.
     * @return A map representing the bean object. Each key-value pair in the map corresponds to a property of the bean.
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? ClassUtil.getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2FlatMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final M output) {
        bean2FlatMap(bean, null, output);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param selectPropNames
     * @param keyNamingPolicy
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2FlatMap(bean, true, null, keyNamingPolicy, output);
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    bean2FlatMap(propValue, true, null, keyNamingPolicy, isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param bean
     * @param ignoreNullProperty
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty) {
        return bean2FlatMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param mapSupplier
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param mapSupplier
     * @return
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = ClassUtil.getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation.
     * Values from nested beans are set to resulting map with property names concatenated with a dot, e.g. {@code "address.city"}
     *
     * @param <M>
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @param keyNamingPolicy
     * @param output
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Collection<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final String parentPropName, final M output) {
        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final boolean isNullParentPropName = (parentPropName == null);
        final Class<?> beanClass = bean.getClass();

        String propName = null;
        Object propValue = null;

        for (final PropInfo propInfo : ParserUtil.getBeanInfo(beanClass).propInfoList) {
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
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(parentPropName + WD.PERIOD + propName, propValue);
                    } else {
                        output.put(parentPropName + WD.PERIOD + keyNamingPolicy.convert(propName), propValue);
                    }
                }
            } else {
                if (isNullParentPropName) {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                } else {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            parentPropName + WD.PERIOD + (isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName)), output);
                }
            }
        }
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
    //    static <M extends Map<String, Object>> M record2Map(final Object record, final M output) {
    //        if (record == null) {
    //            return output;
    //        }
    //
    //        final Class<?> recordClass = record.getClass();
    //
    //        @SuppressWarnings("deprecation")
    //        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(recordClass);
    //
    //        try {
    //            for (Tuple5<String, Field, Method, Type<Object>, Integer> tp : recordInfo.fieldMap().values()) {
    //                output.put(tp._1, tp._3.invoke(record));
    //            }
    //        } catch (IllegalAccessException | InvocationTargetException e) {
    //            // Should never happen.
    //            throw N.toRuntimeException(e);
    //        }
    //
    //        return output;
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

    //    public static class MapGetter<K, V> {
    //
    //        private final Map<K, V> map;
    //        private final boolean defaultForPrimitiveOrBoxedType;
    //
    //        MapGetter(final Map<K, V> map, final boolean defaultForPrimitiveOrBoxedType) {
    //            this.map = map;
    //            this.defaultForPrimitiveOrBoxedType = defaultForPrimitiveOrBoxedType;
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param <K>
    //         * @param <V>
    //         * @param map
    //         * @return
    //         */
    //        public static <K, V> MapGetter<K, V> of(final Map<K, V> map) {
    //            return of(map, false);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param <K>
    //         * @param <V>
    //         * @param map
    //         * @param defaultForPrimitiveOrBoxedType
    //         * @return
    //         */
    //        public static <K, V> MapGetter<K, V> of(final Map<K, V> map, final boolean defaultForPrimitiveOrBoxedType) {
    //            return new MapGetter<>(map, defaultForPrimitiveOrBoxedType);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Boolean getBoolean(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return Boolean.FALSE;
    //            } else if (value instanceof Boolean) {
    //                return (Boolean) value;
    //            }
    //
    //            return Strings.parseBoolean(N.toString(value));
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Character getChar(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Character) {
    //                return (Character) value;
    //            }
    //
    //            return Strings.parseChar(N.toString(value));
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Byte getByte(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Byte) {
    //                return (Byte) value;
    //            }
    //
    //            return Numbers.toByte(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Short getShort(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Short) {
    //                return (Short) value;
    //            }
    //
    //            return Numbers.toShort(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Integer getInt(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0;
    //            } else if (value instanceof Integer) {
    //                return (Integer) value;
    //            }
    //
    //            return Numbers.toInt(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Long getLong(final K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0L;
    //            } else if (value instanceof Long) {
    //                return (Long) value;
    //            }
    //
    //            return Numbers.toLong(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Float getFloat(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0F;
    //            } else if (value instanceof Float) {
    //                return (Float) value;
    //            }
    //
    //            return Numbers.toFloat(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Double getDouble(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null && defaultForPrimitiveOrBoxedType) {
    //                return 0d;
    //            } else if (value instanceof Double) {
    //                return (Double) value;
    //            }
    //
    //            return Numbers.toDouble(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public BigInteger getBigInteger(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof BigInteger) {
    //                return (BigInteger) value;
    //            }
    //
    //            return N.convert(value, BigInteger.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public BigDecimal getBigDecimal(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof BigDecimal) {
    //                return (BigDecimal) value;
    //            }
    //
    //            return N.convert(value, BigDecimal.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public String getString(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof String) {
    //                return (String) value;
    //            }
    //
    //            return N.stringOf(value);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Calendar getCalendar(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof Calendar) {
    //                return (Calendar) value;
    //            }
    //
    //            return N.convert(value, Calendar.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public Date getJUDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof Date) {
    //                return (Date) value;
    //            }
    //
    //            return N.convert(value, Date.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Date getDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Date) {
    //                return (java.sql.Date) value;
    //            }
    //
    //            return N.convert(value, java.sql.Date.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Time getTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Time) {
    //                return (java.sql.Time) value;
    //            }
    //
    //            return N.convert(value, java.sql.Time.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public java.sql.Timestamp getTimestamp(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof java.sql.Timestamp) {
    //                return (java.sql.Timestamp) value;
    //            }
    //
    //            return N.convert(value, java.sql.Timestamp.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalDate getLocalDate(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalDate) {
    //                return (LocalDate) value;
    //            }
    //
    //            return N.convert(value, LocalDate.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalTime getLocalTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalTime) {
    //                return (LocalTime) value;
    //            }
    //
    //            return N.convert(value, LocalTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public LocalDateTime getLocalDateTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof LocalDateTime) {
    //                return (LocalDateTime) value;
    //            }
    //
    //            return N.convert(value, LocalDateTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public ZonedDateTime getZonedDateTime(K key) {
    //            Object value = map.get(key);
    //
    //            if (value == null || value instanceof ZonedDateTime) {
    //                return (ZonedDateTime) value;
    //            }
    //
    //            return N.convert(value, ZonedDateTime.class);
    //        }
    //
    //        /**
    //         *
    //         *
    //         * @param key
    //         * @return
    //         */
    //        public V getObject(K key) {
    //            return map.get(key);
    //        }
    //
    //        /**
    //         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
    //         *
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param targetType
    //         * @return
    //         */
    //        public <T> T get(final Object key, final Class<? extends T> targetType) {
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return (T) (defaultForPrimitiveOrBoxedType && ClassUtil.isPrimitiveWrapper(targetType) ? N.defaultValueOf(ClassUtil.unwrap(targetType))
    //                        : N.defaultValueOf(targetType));
    //            }
    //
    //            if (targetType.isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return N.convert(val, targetType);
    //        }
    //
    //        /**
    //         * Returns {@code null} if no value found by the specified {@code key}, or the value is {@code null}.
    //         *
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param targetType
    //         * @return
    //         */
    //        public <T> T get(final Object key, final Type<? extends T> targetType) {
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return (T) (defaultForPrimitiveOrBoxedType && targetType.isPrimitiveWrapper() ? N.defaultValueOf(ClassUtil.unwrap(targetType.clazz()))
    //                        : targetType.defaultValue());
    //            }
    //
    //            if (targetType.clazz().isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return N.convert(val, targetType);
    //        }
    //
    //        /**
    //         *
    //         * @param <T>
    //         * @param key
    //         * @param defaultForNull
    //         * @return
    //         * @throws IllegalArgumentException
    //         */
    //        public <T> T get(final Object key, final T defaultForNull) throws IllegalArgumentException {
    //            N.checkArgNotNull(defaultForNull, "defaultForNull"); // NOSONAR
    //
    //            final V val = map.get(key);
    //
    //            if (val == null) {
    //                return defaultForNull;
    //            }
    //
    //            if (defaultForNull.getClass().isAssignableFrom(val.getClass())) {
    //                return (T) val;
    //            }
    //
    //            return (T) N.convert(val, defaultForNull.getClass());
    //        }
    //    }

    /**
     * Replaces the keys in the specified map using the provided key converter function.
     * <p>
     * This method iterates over the keys in the map and applies the key converter function to each key.
     * If the converted key is different from the original key, the entry is moved to the new key.
     * </p>
     *
     * @param <K> the type of keys in the map
     * @param map the map whose keys are to be replaced
     * @param keyConverter the function to apply to each key
     */
    @Beta
    public static <K> void replaceKeys(final Map<K, ?> map, final Function<? super K, ? extends K> keyConverter) {
        if (N.isEmpty(map)) {
            return;
        }

        final Map<K, Object> mapToUse = (Map<K, Object>) map;
        final List<K> keys = new ArrayList<>(mapToUse.keySet());
        K newKey = null;

        for (final K key : keys) {
            newKey = keyConverter.apply(key);

            if (!N.equals(key, newKey)) {
                mapToUse.put(newKey, mapToUse.remove(key));
            }
        }
    }

    /**
     * Replaces the keys in the specified map using the provided key converter function and merges values if necessary.
     * <p>
     * This method iterates over the keys in the map and applies the key converter function to each key.
     * If the converted key is different from the original key, the entry is moved to the new key.
     * If there is a conflict (i.e., the new key already exists in the map), the merger function is used to resolve the conflict.
     * </p>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map whose keys are to be replaced
     * @param keyConverter the function to apply to each key
     * @param merger the function to merge values in case of key conflicts
     */
    @Beta
    public static <K, V> void replaceKeys(final Map<K, V> map, final Function<? super K, ? extends K> keyConverter,
            final BiFunction<? super V, ? super V, ? extends V> merger) {
        if (N.isEmpty(map)) {
            return;
        }

        final Map<K, V> mapToUse = map;
        final List<K> keys = new ArrayList<>(mapToUse.keySet());
        K newKey = null;

        for (final K key : keys) {
            newKey = keyConverter.apply(key);

            if (!N.equals(key, newKey)) {
                mapToUse.merge(newKey, mapToUse.remove(key), merger);
            }
        }
    }
}
