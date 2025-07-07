/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * @param <K> the key type
 * @param <V> the value type
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
     * Constructs a BiMap with the default initial capacity.
     */
    public BiMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a BiMap with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the BiMap.
     */
    public BiMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a BiMap with the specified initial capacity and load factor.
     *
     * @param initialCapacity The initial capacity of the BiMap.
     * @param loadFactor The load factor for the BiMap.
     */
    @SuppressWarnings("deprecation")
    public BiMap(final int initialCapacity, final float loadFactor) {
        this(new HashMap<>(N.initHashCapacity(initialCapacity), loadFactor), new HashMap<>(N.initHashCapacity(initialCapacity), loadFactor));
    }

    /**
     * Constructs a BiMap with the specified types of maps for keys and values.
     * This constructor allows the user to specify the types of the underlying maps used to store keys and values.
     *
     * @param keyMapType The Class object representing the type of the Map to be used for storing keys.
     * @param valueMapType The Class object representing the type of the Map to be used for storing values.
     */
    @SuppressWarnings("rawtypes")
    public BiMap(final Class<? extends Map> keyMapType, final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(keyMapType), Suppliers.ofMap(valueMapType));
    }

    /**
     * Constructs a BiMap with the specified suppliers for key and value maps.
     * This constructor allows the user to specify the suppliers of the underlying maps used to store keys and values.
     *
     * @param keyMapSupplier The Supplier object providing the Map to be used for storing keys.
     * @param valueMapSupplier The Supplier object providing the Map to be used for storing values.
     */
    public BiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier) {
        this.keyMapSupplier = keyMapSupplier;
        this.valueMapSupplier = valueMapSupplier;
        keyMap = keyMapSupplier.get();
        valueMap = valueMapSupplier.get();
    }

    /**
     * Constructs a BiMap with the specified key and value maps.
     * This constructor allows the user to directly provide the underlying maps used to store keys and values.
     *
     * @param keyMap The Map to be used for storing keys.
     * @param valueMap The Map to be used for storing values.
     */
    @Internal
    BiMap(final Map<K, V> keyMap, final Map<V, K> valueMap) {
        keyMapSupplier = Suppliers.ofMap(keyMap.getClass());
        valueMapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.keyMap = keyMap;
        this.valueMap = valueMap;
    }

    /**
     * Constructs a BiMap with the specified key and value maps, and an inverse BiMap.
     * This constructor allows the user to provide the underlying maps used to store keys and values, as well as an inverse BiMap.
     *
     * @param keyMap The Map to be used for storing keys.
     * @param valueMap The Map to be used for storing values.
     * @param inverse The inverse BiMap containing the same entries as this BiMap but with reversed keys and values.
     */
    @Internal
    BiMap(final Map<K, V> keyMap, final Map<V, K> valueMap, BiMap<V, K> inverse) {
        keyMapSupplier = Suppliers.ofMap(keyMap.getClass());
        valueMapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.keyMap = keyMap;
        this.valueMap = valueMap;
        this.inverse = inverse;
    }

    /**
     * Creates a new BiMap with a single key-value pair.
     *
     * @param <K> The type of the key.
     * @param <V> The type of the value.
     * @param k1 The key to be inserted into the BiMap.
     * @param v1 The value to be associated with the key in the BiMap.
     * @return A BiMap containing the specified key-value pair.
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> map = new BiMap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new BiMap with two key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> map = new BiMap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Creates a new BiMap with three key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> map = new BiMap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @param k6 The sixth key to be inserted into the BiMap.
     * @param v6 The value to be associated with the sixth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @param k6 The sixth key to be inserted into the BiMap.
     * @param v6 The value to be associated with the sixth key in the BiMap.
     * @param k7 The seventh key to be inserted into the BiMap.
     * @param v7 The value to be associated with the seventh key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @param k6 The sixth key to be inserted into the BiMap.
     * @param v6 The value to be associated with the sixth key in the BiMap.
     * @param k7 The seventh key to be inserted into the BiMap.
     * @param v7 The value to be associated with the seventh key in the BiMap.
     * @param k8 The eighth key to be inserted into the BiMap.
     * @param v8 The value to be associated with the eighth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @param k6 The sixth key to be inserted into the BiMap.
     * @param v6 The value to be associated with the sixth key in the BiMap.
     * @param k7 The seventh key to be inserted into the BiMap.
     * @param v7 The value to be associated with the seventh key in the BiMap.
     * @param k8 The eighth key to be inserted into the BiMap.
     * @param v8 The value to be associated with the eighth key in the BiMap.
     * @param k9 The ninth key to be inserted into the BiMap.
     * @param v9 The value to be associated with the ninth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap with specified key-value pairs.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @param k3 The third key to be inserted into the BiMap.
     * @param v3 The value to be associated with the third key in the BiMap.
     * @param k4 The fourth key to be inserted into the BiMap.
     * @param v4 The value to be associated with the fourth key in the BiMap.
     * @param k5 The fifth key to be inserted into the BiMap.
     * @param v5 The value to be associated with the fifth key in the BiMap.
     * @param k6 The sixth key to be inserted into the BiMap.
     * @param v6 The value to be associated with the sixth key in the BiMap.
     * @param k7 The seventh key to be inserted into the BiMap.
     * @param v7 The value to be associated with the seventh key in the BiMap.
     * @param k8 The eighth key to be inserted into the BiMap.
     * @param v8 The value to be associated with the eighth key in the BiMap.
     * @param k9 The ninth key to be inserted into the BiMap.
     * @param v9 The value to be associated with the ninth key in the BiMap.
     * @param k10 The tenth key to be inserted into the BiMap.
     * @param v10 The value to be associated with the tenth key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
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
     * Creates a new BiMap that is a copy of the specified map.
     *
     * This method allows the user to create a BiMap that contains the same entries as the provided map.
     * The keys and values are copied from the provided map to the new BiMap.
     *
     * @param <K> The type of the keys in the map.
     * @param <V> The type of the values in the map.
     * @param map The map whose entries are to be placed into the new BiMap.
     * @return A new BiMap containing the same entries as the provided map.
     */
    public static <K, V> BiMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        //noinspection rawtypes
        final BiMap<K, V> biMap = new BiMap<>(Maps.newTargetMap(map), Maps.newOrderingMap(map));

        biMap.putAll(map);

        return biMap;
    }

    /**
     * Retrieves the value to which the specified key is mapped.
     *
     * @param key The key whose associated value is to be returned.
     * @return The value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     */
    @Override
    public V get(final Object key) {
        return keyMap.get(key);
    }

    /**
     * Retrieves the key to which the specified value is mapped.
     *
     * @param value The value whose associated key is to be returned.
     * @return The key to which the specified value is mapped, or {@code null} if this map contains no mapping for the value.
     */
    public K getByValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        return valueMap.get(value);
    }

    /**
     * Retrieves the key associated with the specified value, or the default key if this map contains no mapping for the value.
     *
     * @param value The value whose associated key is to be returned.
     * @param defaultValue The default key to be returned if the map contains no mapping for the value.
     * @return The key to which the specified value is mapped, or the default key if this map contains no mapping for the value.
     */
    public K getByValueOrDefault(final Object value, final K defaultValue) {
        //noinspection SuspiciousMethodCalls
        return valueMap.getOrDefault(value, defaultValue);
    }

    /**
     * Associates the specified value with the specified key in this BiMap.
     * If the BiMap previously contained a mapping for the key, the old value is replaced.
     *
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @throws IllegalArgumentException if the given value is already bound to a different key in this BiMap. The BiMap will remain unmodified in this action. To avoid this exception, call {@link #forcePut} instead.
     * @see #forcePut(Object, Object)
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
     * @return the previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @see #put(Object, Object)
     */
    public V forcePut(final K key, final V value) {
        return put(key, value, true);
    }

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
     * Inserts all entries from the specified map into this BiMap.
     *
     * This method allows the user to add multiple key-value pairs to the BiMap at once.
     * Each key-value pair in the provided map is inserted into this BiMap.
     * If a key in the provided map is already present in this BiMap, the associated value is replaced.
     *
     * <p><b>Warning:</b> the results of calling this method may vary depending on the iteration order of {@code map}.</p>
     *
     * @param m The map whose entries are to be added to this BiMap.
     * @throws IllegalArgumentException if an attempt to {@code put} any entry fails. Note that some map entries may have been added to the BiMap before the exception was thrown.
     * @see #put(Object, Object)
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes the mapping for a key from this BiMap if it is present.
     *
     * @param key The key whose mapping is to be removed from the BiMap.
     * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
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
     * Removes the mapping for a value from this BiMap if it is present.
     *
     * @param value The value whose mapping is to be removed from the BiMap.
     * @return The key associated with the value, or {@code null} if there was no mapping for the value.
     */
    public K removeByValue(final Object value) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final K key = valueMap.remove(value);

        if (key != null) {
            keyMap.remove(key);
        }

        return key;
    }

    /**
     * Checks if this BiMap contains a mapping for the specified key.
     *
     * @param key The key whose presence in this BiMap is to be tested.
     * @return {@code true} if this BiMap contains a mapping for the specified key, {@code false} otherwise.
     */
    @Override
    public boolean containsKey(final Object key) {
        return keyMap.containsKey(key);
    }

    /**
     * Checks if this BiMap contains a mapping for the specified value.
     *
     * @param value The value whose presence in this BiMap is to be tested.
     * @return {@code true} if this BiMap contains a mapping for the specified value, {@code false} otherwise.
     */
    @Override
    public boolean containsValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        return valueMap.containsKey(value);
    }

    /**
     * Returns an immutable set of keys contained in this BiMap.
     * The set is backed by the BiMap, so changes to the BiMap are reflected in the set.
     *
     * @return An immutable set of the keys contained in this BiMap.
     */
    @Override
    public ImmutableSet<K> keySet() {
        return ImmutableSet.wrap(keyMap.keySet());
    }

    /**
     * Returns an immutable set of values contained in this BiMap.
     * The set is backed by the BiMap, so changes to the BiMap are reflected in the set.
     *
     * @return An immutable set of the values contained in this BiMap.
     */
    @Override
    public ImmutableSet<V> values() {
        return ImmutableSet.wrap(valueMap.keySet());
    }

    /**
     * Returns an immutable set of the entries contained in this BiMap.
     * The set is backed by the BiMap, so changes to the BiMap are reflected in the set.
     *
     * @return An immutable set of the entries (key-value pairs) contained in this BiMap.
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
     * @return The inverse view of this BiMap.
     */
    public BiMap<V, K> inversed() {
        return (inverse == null) ? inverse = new BiMap<>(valueMap, keyMap, this) : inverse;
    }

    /**
     * Creates a new BiMap that is a copy of the current BiMap.
     * The keys and values are copied from the current BiMap to the new BiMap.
     *
     * @return A new BiMap containing the same entries as the current BiMap.
     */
    public BiMap<K, V> copy() {
        final BiMap<K, V> copy = new BiMap<>(keyMapSupplier, valueMapSupplier);

        copy.putAll(keyMap);

        return copy;
    }

    /**
     * Removes all the mappings from this BiMap.
     * The BiMap will be empty after this call returns.
     */
    @Override
    public void clear() {
        keyMap.clear();
        valueMap.clear();
    }

    /**
     * Checks if this BiMap is empty.
     *
     * @return {@code true} if this BiMap contains no entries, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return keyMap.isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this BiMap.
     *
     * @return The number of key-value mappings in this BiMap.
     */
    @Override
    public int size() {
        return keyMap.size();
    }

    //    public Stream<Map.Entry<K, V>> stream() {
    //        return Stream.of(keyMap.entrySet());
    //    }

    /**
     * Returns the hash code value for this BiMap. The hash code of a BiMap is defined to be the sum of the hash codes of each entry in the BiMap
     *
     * @return the hash code value for this BiMap.
     */
    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    /**
     * Compares the specified object with this BiMap for equality.
     * Returns {@code true} if the given object is also a BiMap and the two BiMaps represent the same mappings.
     *
     * @param obj the object to be compared for equality with this BiMap
     * @return {@code true} if the specified object is equal to this BiMap, {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof BiMap && keyMap.equals(((BiMap<K, V>) obj).keyMap));
    }

    /**
     * Returns a string representation of this BiMap.
     * The string representation consists of a list of key-value mappings in the BiMap, enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters"," (comma and space).
     * Each key-value mapping is rendered as the key followed by an equal sign ("=") followed by the associated value.
     *
     * @return a string representation of this BiMap.
     */
    @Override
    public String toString() {
        return keyMap.toString();
    }

    /**
     * Creates a new Builder for a BiMap.
     *
     * @param <K> The type of the keys in the BiMap.
     * @param <V> The type of the values in the BiMap.
     * @return A new Builder instance for a BiMap.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new Builder for a BiMap with the specified map as its initial data.
     *
     *
     * @param <K> The type of the keys in the BiMap.
     * @param <V> The type of the values in the BiMap.
     * @param map The map whose entries are to be placed into the new BiMap.
     * @return A new Builder instance for a BiMap with the specified map as its initial data.
     * @throws IllegalArgumentException if the specified map is {@code null}.
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map);

        return new Builder<>(map);
    }

    /**
     * This is a static inner class that provides a builder for the BiMap.
     * The Builder design pattern allows for the creation of complex objects step by step.
     * This Builder class provides methods to set the keys and values for the BiMap and build the BiMap when ready.
     *
     * @param <K> The type of the keys in the BiMap.
     * @param <V> The type of the values in the BiMap.
     */
    public static final class Builder<K, V> {
        private final BiMap<K, V> biMap;

        Builder() {
            biMap = new BiMap<>();
        }

        Builder(final Map<K, V> backedMap) {
            biMap = BiMap.copyOf(backedMap);
        }

        /**
         * Adds a key-value pair to the BiMap being built.
         *
         * This method allows the user to add a single key-value pair to the BiMap.
         * If the BiMap previously contained a mapping for the key, the old value is replaced.
         *
         * @param key The key with which the specified value is to be associated.
         * @param value The value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if the given value is already bound to a different key in this BiMap. The BiMap will remain unmodified in this event.
         * @see #forcePut(Object, Object)
         */
        public Builder<K, V> put(final K key, final V value) {
            biMap.put(key, value);

            return this;
        }

        /**
         * Associates the specified value with the specified key in this BiMap.
         * If the BiMap previously contained a mapping for the key or value, the old value or key is replaced.
         *
         * This method is an alternate form of put that silently removes any existing entry with the value before proceeding with the put operation.
         * If the bimap previously contained the provided key-value mapping, this method has no effect.
         *
         * Note that a successful call to this method could cause the size of the bimap to increase by one, stay the same, or even decrease by one.
         *
         * Warning: If an existing entry with this value is removed, the key for that entry is discarded and not returned.
         *
         * @param key The key with which the specified value is to be associated.
         * @param value The value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @see #put(Object, Object)
         */
        public Builder<K, V> forcePut(final K key, final V value) {
            biMap.forcePut(key, value);

            return this;
        }

        /**
         * Inserts all entries from the specified map into the BiMap being built.
         *
         * @param m The map whose entries are to be added to this BiMap.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if an attempt to {@code put} any entry fails. Note that some map entries may have been added to the BiMap before the exception was thrown.
         * @see #put(Object, Object)
         * @see #forcePut(Object, Object)
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                biMap.putAll(m);
            }

            return this;
        }

        /**
         * Returns the BiMap instance that has been built up by the builder's methods
         *
         * @return The constructed BiMap instance.
         */
        public BiMap<K, V> build() {
            return biMap;
        }
    }
}
