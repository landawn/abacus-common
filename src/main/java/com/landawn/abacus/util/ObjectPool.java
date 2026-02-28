/*
 * Copyright (C) 2015 HaiYang Li
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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A thread-safe {@link Map} implementation backed by a {@link ConcurrentHashMap},
 * providing lock-free concurrent reads and thread-safe writes.
 *
 * <p>ObjectPool is designed as a lightweight caching map for scenarios where:</p>
 * <ul>
 *   <li>Read operations vastly outnumber write operations</li>
 *   <li>Thread safety is required without external synchronization</li>
 *   <li>The approximate number of entries is known in advance (for sizing the initial table)</li>
 * </ul>
 *
 * <p><b>Null handling:</b> Neither {@code null} keys nor {@code null} values are permitted.
 * Write operations ({@link #put}, {@link #putAll}) throw {@link NullPointerException} for
 * {@code null} keys or values, consistent with the underlying {@link ConcurrentHashMap}.
 * Read and query operations are null-safe: {@link #get(Object)} and {@link #remove(Object)}
 * return {@code null}, while {@link #containsKey(Object)} and {@link #containsValue(Object)}
 * return {@code false} for {@code null} arguments instead of throwing.</p>
 *
 * <p><b>View collections:</b> The {@link #keySet()}, {@link #values()}, and {@link #entrySet()}
 * methods return live views backed by the underlying map. Changes to the map are reflected
 * in the views, and vice versa where supported.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a pool for caching resolved types by name
 * ObjectPool<String, Type<?>> typeCache = new ObjectPool<>(128);
 *
 * // Cache a resolved type
 * typeCache.put("int", IntType.INSTANCE);
 *
 * // Look up a cached type
 * Type<?> type = typeCache.get("int");
 *
 * // Use computeIfAbsent for atomic cache-on-miss
 * Type<?> resolved = typeCache.computeIfAbsent("long", name -> resolveType(name));
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see ConcurrentHashMap
 * @see Map
 */
@Internal
@Beta
@SuppressWarnings("java:S2160")
public final class ObjectPool<K, V> extends AbstractMap<K, V> {
    private final ConcurrentHashMap<K, V> map;

    /**
     * Constructs an ObjectPool with the specified initial capacity.
     * The {@code capacity} parameter is used as the initial capacity hint
     * for the underlying {@link ConcurrentHashMap}, helping to avoid rehashing
     * when the approximate number of entries is known in advance.
     *
     * @param capacity the initial capacity hint; used to size the underlying hash table
     */
    public ObjectPool(final int capacity) {
        this.map = new ConcurrentHashMap<>(capacity);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * <p>This operation is lock-free and safe for concurrent use.
     * Unlike {@link ConcurrentHashMap#get(Object)}, this method returns
     * {@code null} for a {@code null} key instead of throwing
     * {@link NullPointerException}.</p>
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped,
     *         or {@code null} if the key is {@code null} or no mapping exists
     */
    @MayReturnNull
    @Override
    public V get(final Object key) {
        if (key == null) {
            return null;
        }

        return map.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key},
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is {@code null}
     */
    @MayReturnNull
    @Override
    public V put(final K key, final V value) {
        return map.put(key, value);
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is {@code null},
     *         or if any key or value in the specified map is {@code null}
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * <p>Unlike {@link ConcurrentHashMap#remove(Object)}, this method returns
     * {@code null} for a {@code null} key instead of throwing
     * {@link NullPointerException}.</p>
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key},
     *         or {@code null} if the key is {@code null} or no mapping existed
     */
    @MayReturnNull
    @Override
    public V remove(final Object key) {
        if (key == null) {
            return null;
        }

        return map.remove(key);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * <p>Unlike {@link ConcurrentHashMap#containsKey(Object)}, this method
     * returns {@code false} for a {@code null} key instead of throwing
     * {@link NullPointerException}.</p>
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key;
     *         {@code false} if the key is {@code null} or not present
     */
    @Override
    public boolean containsKey(final Object key) {
        if (key == null) {
            return false;
        }

        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * <p>Unlike {@link ConcurrentHashMap#containsValue(Object)}, this method
     * returns {@code false} for a {@code null} value instead of throwing
     * {@link NullPointerException}.</p>
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value;
     *         {@code false} if the value is {@code null} or not present
     */
    @Override
    public boolean containsValue(final Object value) {
        if (value == null) {
            return false;
        }

        return map.containsValue(value);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice versa.
     *
     * @return a set view of the keys contained in this map
     * @see ConcurrentHashMap#keySet()
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice versa.
     *
     * @return a collection view of the values contained in this map
     * @see ConcurrentHashMap#values()
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice versa. Entries support
     * {@link Map.Entry#setValue(Object)} which writes through to this map.
     *
     * @return a set view of the mappings contained in this map
     * @see ConcurrentHashMap#entrySet()
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * Returns the hash code value for this map, computed as the sum of the
     * hash codes of each entry in the map's {@link #entrySet()} view.
     *
     * @return the hash code value for this map
     */
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a {@link Map} and the two maps
     * represent the same mappings, as defined by the {@link Map#equals(Object)} contract.
     *
     * <p>If {@code other} is also an {@code ObjectPool}, the comparison is performed
     * directly between the backing {@link ConcurrentHashMap} instances for efficiency.</p>
     *
     * @param other object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof ObjectPool op) {
            return map.equals(op.map);
        }

        return map.equals(other);
    }

    /**
     * Returns a string representation of this map. The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's {@link #entrySet()} view's iterator, enclosed in braces ({@code "{}"}).
     *
     * @return a string representation of this map
     */
    @Override
    public String toString() {
        return map.toString();
    }
}
