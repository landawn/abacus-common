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

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.Fn.Suppliers;

/**
 * A BiMap (or "bidirectional map") is a map that preserves the uniqueness of its values as well as that of its keys.
 * This constraint enables BiMaps to support an "inverse view", which is another BiMap containing the same entries as this BiMap but with reversed keys and values.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public final class BiMap<K, V> implements Map<K, V> {
    /**
     * The maximum capacity, used if a higher value is implicitly specified by either of the constructors with
     * arguments. MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    final Supplier<? extends Map<K, V>> keyMapSupplier;

    final Supplier<? extends Map<V, K>> valueMapSupplier;

    final Map<K, V> keyMap;

    final Map<V, K> valueMap;

    private transient BiMap<V, K> inverse; //NOSONAR

    /**
     *
     */
    public BiMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public BiMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     *
     *
     * @param initialCapacity
     * @param loadFactor
     */
    @SuppressWarnings("deprecation")
    public BiMap(final int initialCapacity, final float loadFactor) {
        this(new HashMap<>(N.initHashCapacity(initialCapacity), loadFactor), new HashMap<>(N.initHashCapacity(initialCapacity), loadFactor));
    }

    /**
     *
     *
     * @param keyMapType
     * @param valueMapType
     */
    @SuppressWarnings("rawtypes")
    public BiMap(final Class<? extends Map> keyMapType, final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(keyMapType), Suppliers.ofMap(valueMapType));
    }

    /**
     *
     *
     * @param keyMapSupplier
     * @param valueMapSupplier
     */
    public BiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier) {
        this.keyMapSupplier = keyMapSupplier;
        this.valueMapSupplier = valueMapSupplier;
        keyMap = keyMapSupplier.get();
        valueMap = valueMapSupplier.get();
    }

    @Internal
    BiMap(final Map<K, V> keyMap, final Map<V, K> valueMap) {
        keyMapSupplier = Suppliers.ofMap(keyMap.getClass());
        valueMapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.keyMap = keyMap;
        this.valueMap = valueMap;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> map = new BiMap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> map = new BiMap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> map = new BiMap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final BiMap<K, V> map = new BiMap<>(4);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return map;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final BiMap<K, V> map = new BiMap<>(5);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return map;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
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
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6) {
        final BiMap<K, V> map = new BiMap<>(6);

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
     *
     * @param <K> the key type
     * @param <V> the value type
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
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7) {
        final BiMap<K, V> map = new BiMap<>(7);

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
     *
     * @param <K>
     * @param <V>
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
     * @param k8
     * @param v8
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) {
        final BiMap<K, V> map = new BiMap<>(8);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);

        return map;
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
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
     * @param k8
     * @param v8
     * @param k9
     * @param v9
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) {
        final BiMap<K, V> map = new BiMap<>(9);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);

        return map;
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
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
     * @param k8
     * @param v8
     * @param k9
     * @param v9
     * @param k10
     * @param v10
     * @return
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10) {
        final BiMap<K, V> map = new BiMap<>(10);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);
        map.put(k10, v10);

        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> BiMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        final BiMap<K, V> biMap = new BiMap<>(Maps.newTargetMap(map), Maps.newOrderingMap(map));

        biMap.putAll(map);

        return biMap;
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V get(final Object key) {
        return keyMap.get(key);
    }

    /**
     * Gets the by value.
     *
     * @param value
     * @return
     */
    public K getByValue(final Object value) {
        return valueMap.get(value);
    }

    /**
     *
     * @param value
     * @param defaultValue
     * @return
     */
    public K getByValueOrDefault(final Object value, final K defaultValue) {
        return valueMap.getOrDefault(value, defaultValue);
    }

    /**
     *
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if the given value is already bound to a
     *     different key in this bimap. The bimap will remain unmodified in this
     *     event. To avoid this exception, call {@link #forcePut} instead.
     */
    @Override
    public V put(final K key, final V value) {
        return put(key, value, false);
    }

    /**
     * An alternate form of {@code put} that silently removes any existing entry
     * with the value {@code value} before proceeding with the {@link #put}
     * operation. If the bimap previously contained the provided key-value
     * mapping, this method has no effect.
     *
     * <p>Note that a successful call to this method could cause the size of the
     * bimap to increase by one, stay the same, or even decrease by one.
     *
     * <p><b>Warning:</b> If an existing entry with this value is removed, the key
     * for that entry is discarded and not returned.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return
     *     be {@code null}, or {@code null} if there was no previous entry
     */
    public V forcePut(final K key, final V value) {
        return put(key, value, true);
    }

    /**
     *
     * @param key
     * @param value
     * @param isForce
     * @return
     */
    private V put(final K key, final V value, final boolean isForce) {
        if ((key == null) || (value == null)) {
            throw new IllegalArgumentException("key or value can't be null");
        } else if (!isForce && valueMap.containsKey(value)) {
            throw new IllegalArgumentException("Value already exists: " + value);
        }

        final V v = keyMap.remove(key);

        if (v != null) {
            valueMap.remove(v);
        }

        final K k = valueMap.remove(value);

        if (k != null) {
            keyMap.remove(k);
        }

        keyMap.put(key, value);
        valueMap.put(value, key);

        return v;
    }

    /**
     * <p><b>Warning:</b> the results of calling this method may vary depending on
     * the iteration order of {@code map}.
     *
     * @param m
     * @throws IllegalArgumentException if an attempt to {@code put} any
     *     entry fails. Note that some map entries may have been added to the
     *     bimap before the exception was thrown.
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V remove(final Object key) {
        final V value = keyMap.remove(key);

        if (value != null) {
            valueMap.remove(value);
        }

        return value;
    }

    /**
     * Removes the by value.
     *
     * @param value
     * @return
     */
    public K removeByValue(final Object value) {
        final K key = valueMap.remove(value);

        if (key != null) {
            keyMap.remove(key);
        }

        return key;
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public boolean containsKey(final Object key) {
        return keyMap.containsKey(key);
    }

    /**
     *
     * @param value
     * @return
     */
    @Override
    public boolean containsValue(final Object value) {
        return valueMap.containsKey(value);
    }

    /**
     * Returns an immutable key set.
     *
     * @return
     */
    @Override
    public ImmutableSet<K> keySet() {
        return ImmutableSet.wrap(keyMap.keySet());
    }

    /**
     * Returns an immutable value set.
     *
     * @return
     */
    @Override
    public ImmutableSet<V> values() {
        return ImmutableSet.wrap(valueMap.keySet());
    }

    /**
     * Returns an immutable Set of Immutable entry.
     *
     * @return
     */
    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return ImmutableSet.wrap(new AbstractSet<>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new ObjIterator<>() {
                    private final Iterator<Map.Entry<K, V>> keyValueEntryIter = keyMap.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return keyValueEntryIter.hasNext();
                    }

                    @Override
                    public ImmutableEntry<K, V> next() {
                        return ImmutableEntry.copyOf(keyValueEntryIter.next());
                    }
                };
            }

            @Override
            public int size() {
                return keyMap.size();
            }
        });
    }

    /**
     * Returns the inverse view of this BiMap, which maps each of this bimap's values to its associated key.
     * The two BiMaps are backed by the same data; any changes to one will appear in the other.
     *
     * @return
     */
    public BiMap<V, K> inversed() {
        return (inverse == null) ? inverse = new BiMap<>(valueMap, keyMap) : inverse;
    }

    /**
     *
     *
     * @return
     */
    public BiMap<K, V> copy() {
        final BiMap<K, V> copy = new BiMap<>(keyMapSupplier, valueMapSupplier);

        copy.putAll(keyMap);

        return copy;
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        keyMap.clear();
        valueMap.clear();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return keyMap.isEmpty();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return keyMap.size();
    }

    //    public Stream<Map.Entry<K, V>> stream() {
    //        return Stream.of(keyMap.entrySet());
    //    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof BiMap && keyMap.equals(((BiMap<K, V>) obj).keyMap));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return keyMap.toString();
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);

        return new Builder<>(map);
    }

    public static final class Builder<K, V> {
        private final BiMap<K, V> biMap;

        Builder() {
            biMap = new BiMap<>();
        }

        Builder(final Map<K, V> backedMap) {
            biMap = BiMap.copyOf(backedMap);
        }

        /**
         *
         *
         * @param key
         * @param value
         * @return
         */
        public Builder<K, V> put(final K key, final V value) {
            biMap.put(key, value);

            return this;
        }

        /**
         *
         *
         * @param key
         * @param value
         * @return
         */
        public Builder<K, V> forcePut(final K key, final V value) {
            biMap.forcePut(key, value);

            return this;
        }

        /**
         *
         *
         * @param m
         * @return
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                biMap.putAll(m);
            }

            return this;
        }

        /**
         *
         *
         * @return
         */
        public BiMap<K, V> build() {
            return biMap;
        }
    }
}
