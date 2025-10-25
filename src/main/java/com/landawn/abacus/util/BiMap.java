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
     * This method provides a convenient way to create a BiMap with one entry.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * }</pre>
     *
     * @param <K> The type of the key.
     * @param <V> The type of the value.
     * @param k1 The key to be inserted into the BiMap.
     * @param v1 The value to be associated with the key in the BiMap.
     * @return A BiMap containing the specified key-value pair.
     * @throws IllegalArgumentException if any key or value is null
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1) {
        final BiMap<K, V> map = new BiMap<>(1);

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new BiMap with two key-value pairs.
     * This method provides a convenient way to create a BiMap with two entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * }</pre>
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param k1 The first key to be inserted into the BiMap.
     * @param v1 The value to be associated with the first key in the BiMap.
     * @param k2 The second key to be inserted into the BiMap.
     * @param v2 The value to be associated with the second key in the BiMap.
     * @return A BiMap containing the specified key-value pairs.
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final BiMap<K, V> map = new BiMap<>(2);

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Creates a new BiMap with three key-value pairs.
     * This method provides a convenient way to create a BiMap with three entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final BiMap<K, V> map = new BiMap<>(3);

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Creates a new BiMap with four key-value pairs.
     * This method provides a convenient way to create a BiMap with four entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with five key-value pairs.
     * This method provides a convenient way to create a BiMap with five entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with six key-value pairs.
     * This method provides a convenient way to create a BiMap with six entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with seven key-value pairs.
     * This method provides a convenient way to create a BiMap with seven entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with eight key-value pairs.
     * This method provides a convenient way to create a BiMap with eight entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with nine key-value pairs.
     * This method provides a convenient way to create a BiMap with nine entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * Creates a new BiMap with ten key-value pairs.
     * This method provides a convenient way to create a BiMap with ten entries.
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
     * @throws IllegalArgumentException if any key or value is null, or if any value is duplicated
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
     * This method creates a BiMap containing the same key-value mappings as the provided map.
     * The underlying map implementation is determined based on the type of the input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * BiMap<String, Integer> biMap = BiMap.copyOf(map);
     * }</pre>
     *
     * @param <K> The type of the keys in the map.
     * @param <V> The type of the values in the map.
     * @param map The map whose entries are to be placed into the new BiMap.
     * @return A new BiMap containing the same entries as the provided map.
     * @throws IllegalArgumentException if any key or value in the map is null, or if any value is duplicated
     */
    public static <K, V> BiMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        //noinspection rawtypes
        final BiMap<K, V> biMap = new BiMap<>(Maps.newTargetMap(map), Maps.newOrderingMap(map));

        biMap.putAll(map);

        return biMap;
    }

    /**
     * Retrieves the value to which the specified key is mapped in this BiMap.
     * Returns {@code null} if this BiMap contains no mapping for the key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * Integer value = map.get("one"); // returns 1
     * }</pre>
     *
     * @param key The key whose associated value is to be returned.
     * @return The value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     */
    @Override
    public V get(final Object key) {
        return keyMap.get(key);
    }

    /**
     * Retrieves the key to which the specified value is mapped in this BiMap.
     * This is the inverse lookup operation, returning the key associated with the given value.
     * Returns {@code null} if this BiMap contains no mapping for the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.getByValue(1); // returns "one"
     * }</pre>
     *
     * @param value The value whose associated key is to be returned.
     * @return The key to which the specified value is mapped, or {@code null} if this map contains no mapping for the value.
     */
    public K getByValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        return valueMap.get(value);
    }

    /**
     * Retrieves the key associated with the specified value, or returns the default key if this BiMap contains no mapping for the value.
     * This method provides a safe way to perform inverse lookups with a fallback value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.getByValueOrDefault(2, "unknown"); // returns "unknown"
     * }</pre>
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
     * Both keys and values must be unique in a BiMap; attempting to insert a value that already exists
     * will throw an exception unless {@link #forcePut} is used instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * map.put("one", 1); // adds mapping
     * map.put("one", 2); // replaces value for "one"
     * }</pre>
     *
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @throws IllegalArgumentException if the key or value is null, or if the given value is already bound to a different key in this BiMap. The BiMap will remain unmodified in this event. To avoid this exception, call {@link #forcePut} instead.
     * @see #forcePut(Object, Object)
     */
    @Override
    public V put(final K key, final V value) {
        return put(key, value, false);
    }

    /**
     * An alternate form of {@code put} that silently removes any existing entry
     * with the value {@code value} before proceeding with the {@link #put}
     * operation. If the BiMap previously contained the provided key-value
     * mapping, this method has no effect.
     *
     * <p>This method ensures that the value is unique by removing any existing
     * entry with the same value, even if it's mapped to a different key.
     *
     * <p>Note that a successful call to this method could cause the size of the
     * BiMap to increase by one, stay the same, or even decrease by one.
     *
     * <p><b>Warning:</b> If an existing entry with this value is removed, the key
     * for that entry is discarded and not returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.forcePut("two", 1); // removes ("one", 1), adds ("two", 1)
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @throws IllegalArgumentException if the key or value is null
     * @see #put(Object, Object)
     */
    public V forcePut(final K key, final V value) {
        return put(key, value, true);
    }

    private V put(final K key, final V value, final boolean isForce) {
        if ((key == null) || (value == null)) {
            throw new IllegalArgumentException("key or value can't be null");
        } else if (!isForce && valueMap.containsKey(value) && !key.equals(valueMap.get(value))) {
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
     * Each key-value pair in the provided map is inserted into this BiMap using {@link #put}.
     * If a key in the provided map is already present in this BiMap, the associated value is replaced.
     *
     * <p><b>Warning:</b> The results of calling this method may vary depending on the iteration order of {@code map}.
     * If the operation fails, some entries may have already been added to the BiMap before the exception was thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> biMap = new BiMap<>();
     * Map<String, Integer> map = Map.of("one", 1, "two", 2);
     * biMap.putAll(map);
     * }</pre>
     *
     * @param m The map whose entries are to be added to this BiMap.
     * @throws IllegalArgumentException if any key or value is null, or if an attempt to {@code put} any entry fails due to a duplicate value. Note that some map entries may have been added to the BiMap before the exception was thrown.
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
     * This operation also removes the inverse mapping from value to key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * Integer value = map.remove("one"); // returns 1, removes mapping
     * }</pre>
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
     * This is the inverse removal operation, removing the entry by its value and returning the associated key.
     * This operation also removes the mapping from key to value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.removeByValue(1); // returns "one", removes mapping
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * boolean exists = map.containsKey("one"); // returns true
     * }</pre>
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
     * In a BiMap, values are unique and can be used for lookups just like keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * boolean exists = map.containsValue(1); // returns true
     * }</pre>
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
     * The returned set is a view backed by the BiMap, so changes to the BiMap are reflected in the set,
     * but the set itself cannot be modified directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * Set<String> keys = map.keySet(); // returns ["one", "two"]
     * }</pre>
     *
     * @return An immutable set of the keys contained in this BiMap.
     */
    @Override
    public ImmutableSet<K> keySet() {
        return ImmutableSet.wrap(keyMap.keySet());
    }

    /**
     * Returns an immutable set of values contained in this BiMap.
     * Unlike a regular Map where values() returns a Collection, BiMap returns a Set because values are unique.
     * The returned set is a view backed by the BiMap, so changes to the BiMap are reflected in the set,
     * but the set itself cannot be modified directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * Set<Integer> values = map.values(); // returns [1, 2]
     * }</pre>
     *
     * @return An immutable set of the values contained in this BiMap.
     */
    @Override
    public ImmutableSet<V> values() {
        return ImmutableSet.wrap(valueMap.keySet());
    }

    /**
     * Returns an immutable set of the entries contained in this BiMap.
     * Each entry is a key-value pair from the BiMap.
     * The returned set is a view backed by the BiMap, so changes to the BiMap are reflected in the set,
     * but the set itself cannot be modified directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * Set<Map.Entry<String, Integer>> entries = map.entrySet();
     * }</pre>
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
     * Returns the inverse view of this BiMap, which maps each of this BiMap's values to its associated key.
     * The two BiMaps are backed by the same underlying data; any changes to one will appear in the other.
     * This provides an efficient way to perform reverse lookups without creating a separate copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * BiMap<Integer, String> inverse = map.inversed();
     * String key = inverse.get(1); // returns "one"
     * }</pre>
     *
     * @return The inverse view of this BiMap where keys and values are swapped.
     */
    public BiMap<V, K> inversed() {
        return (inverse == null) ? inverse = new BiMap<>(valueMap, keyMap, this) : inverse;
    }

    /**
     * Creates a new BiMap that is a shallow copy of the current BiMap.
     * The new BiMap will contain the same key-value mappings as this BiMap,
     * but is independent - changes to one will not affect the other.
     * The underlying map implementations are created using the same suppliers as this BiMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> original = BiMap.of("one", 1);
     * BiMap<String, Integer> copy = original.copy();
     * }</pre>
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
     * Both the key-to-value and value-to-key mappings are removed.
     * The BiMap will be empty after this call returns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.clear();
     * map.isEmpty(); // returns true
     * }</pre>
     */
    @Override
    public void clear() {
        keyMap.clear();
        valueMap.clear();
    }

    /**
     * Checks if this BiMap is empty.
     * Returns {@code true} if this BiMap contains no key-value mappings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * boolean empty = map.isEmpty(); // returns true
     * }</pre>
     *
     * @return {@code true} if this BiMap contains no entries, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return keyMap.isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this BiMap.
     * This is equivalent to the number of keys or values, as they are always equal in a BiMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * int count = map.size(); // returns 2
     * }</pre>
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
     * Returns the hash code value for this BiMap.
     * The hash code of a BiMap is defined to be the sum of the hash codes of each entry in the BiMap,
     * consistent with the contract of {@link Map#hashCode()}.
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
     * Two BiMaps are considered equal if they have the same key-value mappings.
     *
     * @param obj the object to be compared for equality with this BiMap
     * @return {@code true} if the specified object is equal to this BiMap, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof BiMap && keyMap.equals(((BiMap<?, ?>) obj).keyMap));
    }

    /**
     * Returns a string representation of this BiMap.
     * The string representation consists of a list of key-value mappings in the BiMap, enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters ", " (comma and space).
     * Each key-value mapping is rendered as the key followed by an equal sign ("=") followed by the associated value.
     * The format is consistent with {@link Map#toString()}.
     *
     * <p><b>Usage Examples:</b></p> {@code {one=1, two=2}}
     *
     * @return a string representation of this BiMap.
     */
    @Override
    public String toString() {
        return keyMap.toString();
    }

    /**
     * Creates a new Builder for constructing a BiMap.
     * The Builder pattern allows for fluent construction of BiMaps with multiple entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> The type of the keys in the BiMap.
     * @param <V> The type of the values in the BiMap.
     * @return A new Builder instance for a BiMap.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new Builder for a BiMap initialized with the specified map's entries.
     * This allows starting with an existing map and adding additional entries via the builder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> existing = Map.of("one", 1);
     * BiMap<String, Integer> map = BiMap.builder(existing)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> The type of the keys in the BiMap.
     * @param <V> The type of the values in the BiMap.
     * @param map The map whose entries are to be placed into the new BiMap.
     * @return A new Builder instance for a BiMap with the specified map as its initial data.
     * @throws IllegalArgumentException if the specified map is {@code null}, or if any key or value in the map is null, or if any value is duplicated
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
         * If the BiMap previously contained a mapping for the key, the old value is replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .put("two", 2)
         *     .build();
         * }</pre>
         *
         * @param key The key with which the specified value is to be associated.
         * @param value The value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if the key or value is null, or if the given value is already bound to a different key in this BiMap. The BiMap will remain unmodified in this event.
         * @see #forcePut(Object, Object)
         */
        public Builder<K, V> put(final K key, final V value) {
            biMap.put(key, value);

            return this;
        }

        /**
         * Associates the specified value with the specified key in this BiMap, forcefully removing any existing mapping with the same value.
         * If the BiMap previously contained a mapping for the key or value, the old value or key is replaced.
         *
         * <p>This method is an alternate form of put that silently removes any existing entry with the value before proceeding with the put operation.
         * If the BiMap previously contained the provided key-value mapping, this method has no effect.
         *
         * <p>Note that a successful call to this method could cause the size of the BiMap to increase by one, stay the same, or even decrease by one.
         *
         * <p>Warning: If an existing entry with this value is removed, the key for that entry is discarded and not returned.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .forcePut("two", 1) // removes ("one", 1), adds ("two", 1)
         *     .build();
         * }</pre>
         *
         * @param key The key with which the specified value is to be associated.
         * @param value The value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if the key or value is null
         * @see #put(Object, Object)
         */
        public Builder<K, V> forcePut(final K key, final V value) {
            biMap.forcePut(key, value);

            return this;
        }

        /**
         * Inserts all entries from the specified map into the BiMap being built.
         * Each entry in the provided map is added using {@link #put(Object, Object)}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> existing = Map.of("one", 1, "two", 2);
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .putAll(existing)
         *     .put("three", 3)
         *     .build();
         * }</pre>
         *
         * @param m The map whose entries are to be added to this BiMap.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if any key or value is null, or if an attempt to {@code put} any entry fails due to a duplicate value. Note that some map entries may have been added to the BiMap before the exception was thrown.
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
         * Returns the BiMap instance that has been built up by the builder's methods.
         * This finalizes the construction and returns the completed BiMap.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .put("two", 2)
         *     .build();
         * }</pre>
         *
         * @return The constructed BiMap instance.
         */
        public BiMap<K, V> build() {
            return biMap;
        }
    }
}
