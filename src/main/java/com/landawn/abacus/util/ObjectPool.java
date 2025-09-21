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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A thread-safe, fixed-size map implementation optimized for high-frequency reads and infrequent writes.
 * This map uses open addressing with separate chaining for collision resolution.
 * 
 * <p>ObjectPool is designed for scenarios where:</p>
 * <ul>
 *   <li>The number of keys is limited and known in advance</li>
 *   <li>Read operations vastly outnumber write operations</li>
 *   <li>Thread safety is required for all operations</li>
 *   <li>Performance is critical for get operations</li>
 * </ul>
 * 
 * <p>The map has a fixed capacity and uses a hash table with power-of-two sizing
 * for efficient bitwise operations. When the size exceeds capacity, a warning
 * is logged (once) but the map continues to function.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a pool for caching database connections by connection string
 * ObjectPool<String, Connection> connectionPool = new ObjectPool<>(16);
 * 
 * // Store a connection
 * connectionPool.put("jdbc:mysql://localhost/test", connection);
 * 
 * // Retrieve a connection
 * Connection conn = connectionPool.get("jdbc:mysql://localhost/test");
 * }</pre>
 * 
 * <p><b>Note:</b> This implementation does not support null keys or values.
 * The keySet(), values(), and entrySet() methods return unmodifiable views
 * that are computed on each call.</p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 1.0
 * @see AbstractMap
 * @see Map
 */
@Internal
@Beta
@SuppressWarnings("java:S2160")
public final class ObjectPool<K, V> extends AbstractMap<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);

    private final int capacity;

    private final Entry<K, V>[] table;

    private final int indexMask;

    private int _size = 0; //NOSONAR

    private boolean isWarningLoggedForCapacity;

    private transient Set<K> _keySet = null; //NOSONAR

    private transient Collection<V> _values = null; //NOSONAR

    private transient Set<Map.Entry<K, V>> _entrySet; //NOSONAR

    /**
     * Constructs an ObjectPool with the specified capacity.
     * The actual capacity will be the specified value, and the internal
     * hash table size will be adjusted for optimal performance.
     *
     * @param capacity the maximum expected number of entries
     * @throws IllegalArgumentException if capacity is not positive
     */
    @SuppressWarnings("unchecked")
    public ObjectPool(final int capacity) {
        N.checkArgPositive(capacity, cs.capacity);

        this.capacity = capacity;
        table = new Entry[capacity];
        indexMask = table.length - 1;
    }

    /**
     * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
     * This operation is thread-safe and optimized for performance.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjectPool<String, Integer> pool = new ObjectPool<>(10);
     * pool.put("one", 1);
     * Integer value = pool.get("one"); // Returns 1
     * Integer missing = pool.get("two"); // Returns null
     * }</pre>
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    @Override
    public V get(final Object key) {
        final int hash = hash(key);
        final int i = hash & indexMask;

        for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
            if ((hash == entry.hash) && key.equals(entry.key)) {
                return entry.value;
            }
        }

        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     * This operation is thread-safe.
     * 
     * <p>If the pool size exceeds its capacity, a warning is logged once,
     * but the operation completes successfully.</p>
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key
     * @throws IllegalArgumentException if the key or value is null
     */
    @Override
    public V put(final K key, final V value) {
        synchronized (table) {
            return putValue(key, value);
        }
    }

    /**
     * Internal method to put a value without additional synchronization.
     * This method handles the actual insertion logic including collision resolution.
     *
     * @param key the key to insert
     * @param value the value to associate with the key
     * @return the previous value associated with the key, or null
     * @throws IllegalArgumentException if key or value is null
     */
    private V putValue(final K key, final V value) {
        if ((key == null) || (value == null)) {
            throw new IllegalArgumentException();
        }

        final int hash = hash(key);
        final int i = hash & indexMask;

        //            if (size() > capacity) {
        //                throw new IndexOutOfBoundsException("Object pool is full of capacity=" + capacity);
        //            }
        //

        if (!isWarningLoggedForCapacity && size() > capacity) {
            final Set<Map.Entry<K, V>> entrySet = entrySet();

            logger.warn("Pool size={} is bigger than capacity={}, first entry={}, last entry={}", size(), capacity, N.firstElement(entrySet),
                    N.lastElement(entrySet));

            isWarningLoggedForCapacity = true;
        }

        for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
            if ((hash == entry.hash) && key.equals(entry.key)) {
                final V previousValue = entry.value;
                entry.value = value;

                return previousValue;
            }
        }

        final Entry<K, V> entry = new Entry<>(hash, key, value, table[i]);
        table[i] = entry;

        _keySet = null;
        _values = null;
        _entrySet = null;

        _size++;

        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     * This operation is thread-safe.
     * 
     * <p>Null values in the source map are silently ignored.</p>
     *
     * @param m mappings to be stored in this map
     * @throws IllegalArgumentException if any key in the map is null
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        synchronized (table) {
            for (final Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                if (entry.getValue() != null) {
                    putValue(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * This operation is thread-safe.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjectPool<String, Integer> pool = new ObjectPool<>(10);
     * pool.put("key", 100);
     * Integer removed = pool.remove("key"); // Returns 100
     * Integer notFound = pool.remove("key"); // Returns null
     * }</pre>
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @MayReturnNull
    @Override
    public V remove(final Object key) {
        if (_size == 0) {
            return null;
        }

        final int hash = hash(key);
        final int i = hash & indexMask;

        synchronized (table) {
            Entry<K, V> prev = table[i];
            Entry<K, V> e = prev;

            while (e != null) {
                final Entry<K, V> next = e.next;

                if ((hash == e.hash) && key.equals(e.key)) {
                    if (prev == e) { // NOSONAR
                        table[i] = next;
                    } else {
                        prev.next = next;
                    }

                    _keySet = null;
                    _values = null;
                    _entrySet = null;

                    _size--;

                    return e.value;
                }

                prev = e;
                e = next;
            }

            return null;
        }
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     * This method also verifies that the associated value is not null.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a non-null mapping for the specified key
     */
    @Override
    public boolean containsKey(final Object key) {
        final int hash = hash(key);
        final int i = hash & indexMask;

        for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
            if ((hash == entry.hash) && (entry.value != null) && key.equals(entry.key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this map maps one or more keys to the specified value.
     * This operation requires a full scan of the map and is not optimized for performance.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(final Object value) {
        if (value == null) {
            return false;
        }

        for (final Entry<K, V> element : table) {
            for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                if (value.equals(entry.value)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns an unmodifiable Set view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the set.
     * The set is computed on each call to this method.
     * 
     * <p>The returned set does not support modification operations.</p>
     *
     * @return an unmodifiable set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        Set<K> tmp = _keySet;

        if (tmp == null) {
            synchronized (table) {
                tmp = N.newHashSet(_size);

                for (final Entry<K, V> element : table) {
                    for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                        if (entry.value != null) {
                            tmp.add(entry.key);
                        }
                    }
                }

                tmp = Collections.unmodifiableSet(tmp);

                _keySet = tmp;
            }
        }

        return tmp;
    }

    /**
     * Returns an unmodifiable Collection view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are reflected in the collection.
     * The collection is computed on each call to this method.
     * 
     * <p>The returned collection does not support modification operations.</p>
     *
     * @return an unmodifiable collection view of the values contained in this map
     */
    @Override
    public Collection<V> values() {
        Collection<V> tmp = _values;

        if (tmp == null) {
            synchronized (table) {
                tmp = N.newHashSet(_size);

                V value = null;

                for (final Entry<K, V> element : table) {
                    for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                        value = entry.value;

                        if (value != null) {
                            tmp.add(value);
                        }
                    }
                }

                tmp = Collections.unmodifiableCollection(tmp);

                _values = tmp;
            }
        }

        return tmp;
    }

    /**
     * Returns an unmodifiable Set view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the set.
     * The set is computed on each call to this method.
     * 
     * <p>The returned set does not support modification operations.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjectPool<String, Integer> pool = new ObjectPool<>(10);
     * pool.put("one", 1);
     * pool.put("two", 2);
     * 
     * for (Map.Entry<String, Integer> entry : pool.entrySet()) {
     *     System.out.println(entry.getKey() + " = " + entry.getValue());
     * }
     * }</pre>
     *
     * @return an unmodifiable set view of the mappings contained in this map
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> tmp = _entrySet;

        if (tmp == null) {
            synchronized (table) {
                tmp = N.newHashSet(_size);

                for (final Entry<K, V> element : table) {
                    for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                        if (entry.value != null) {
                            tmp.add(entry);
                        }
                    }
                }

                tmp = Collections.unmodifiableSet(tmp);

                _entrySet = tmp;
            }
        }

        return tmp;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return _size;
    }

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return _size == 0;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     * This operation is thread-safe.
     */
    @Override
    public void clear() {
        synchronized (table) {
            Arrays.fill(table, null);

            _keySet = null;
            _values = null;
            _entrySet = null;

            _size = 0;
        }
    }

    /**
     * Computes the hash code for the given key.
     * This method uses the same hash spreading technique as HashMap
     * to ensure good distribution of hash values.
     *
     * @param key the key to hash
     * @return the hash code, or 0 if key is null
     */
    static int hash(final Object key) {
        int h;

        return (key == null) ? 0 : ((h = key.hashCode()) ^ (h >>> 16));
    }

    /**
     * A hash table entry (node) in the ObjectPool.
     * Each entry stores a key-value pair along with its hash code
     * and a reference to the next entry in the collision chain.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     */
    static class Entry<K, V> implements Map.Entry<K, V> {

        /** The key for this entry */
        final K key;

        /** The value for this entry */
        V value;

        /** Reference to the next entry in the collision chain */
        Entry<K, V> next;

        /** Cached hash code of the key */
        final int hash;

        /**
         * Creates a new entry with the specified values.
         *
         * @param h the hash code of the key
         * @param k the key
         * @param v the value
         * @param n the next entry in the chain
         */
        Entry(final int h, final K k, final V v, final Entry<K, V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        @Override
        public final K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        @Override
        public final V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified value.
         *
         * @param newValue new value to be stored in this entry
         * @return old value corresponding to the entry
         */
        @Override
        public final V setValue(final V newValue) {
            final V oldValue = value;
            value = newValue;

            return oldValue;
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns true if the given object is also a map entry and
         * the two entries represent the same mapping.
         *
         * @param obj object to be compared for equality with this map entry
         * @return {@code true} if the specified object is equal to this map entry
         */
        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Map.Entry) {
                final Map.Entry<K, V> other = (Map.Entry<K, V>) obj;

                return N.equals(getKey(), other.getKey()) && N.equals(getValue(), other.getValue());
            }

            return false;
        }

        /**
         * Returns the hash code value for this map entry.
         * The hash code is computed as the XOR of the hash codes
         * of the entry's key and value.
         *
         * @return the hash code value for this map entry
         */
        @Override
        public final int hashCode() {
            return N.hashCode(getKey()) ^ N.hashCode(getValue());
        }

        /**
         * Returns a String representation of this map entry.
         * The string representation consists of the key followed by
         * an equals sign ("=") followed by the associated value.
         *
         * @return a String representation of this map entry
         */
        @Override
        public final String toString() {
            return getKey() + "=" + getValue();
        }
    }
}