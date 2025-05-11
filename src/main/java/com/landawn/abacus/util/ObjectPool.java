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
 * It's a multiple-thread safety map with fixed size. It's designed for frequent get and few add/remove operations
 * with a few limited keys.
 *
 * @param <K> the key type
 * @param <V> the value type
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
     *
     * @param capacity
     */
    @SuppressWarnings("unchecked")
    public ObjectPool(final int capacity) {
        this.capacity = capacity;
        table = new Entry[capacity];
        indexMask = table.length - 1;
    }

    /**
     *
     * @param key
     * @return
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
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V put(final K key, final V value) {
        synchronized (table) {
            return putValue(key, value);
        }
    }

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
     *
     * @param m
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
     *
     * @param key
     * @return
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
     *
     * @param key
     * @return
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
     *
     * @param value
     * @return
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

    @Override
    public int size() {
        return _size;
    }

    /**
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    @Override
    public boolean isEmpty() {
        return _size == 0;
    }

    /**
     * Clear.
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
     *
     * @param key
     * @return
     */
    static int hash(final Object key) {
        int h;

        return (key == null) ? 0 : ((h = key.hashCode()) ^ (h >>> 16));
    }

    /**
     * The Class Entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    static class Entry<K, V> implements Map.Entry<K, V> {

        /** The key. */
        final K key;

        /** The value. */
        V value;

        /** The next. */
        Entry<K, V> next;

        /** The hash. */
        final int hash;

        /**
         * Creates new entry.
         *
         * @param h
         * @param k
         * @param v
         * @param n
         */
        Entry(final int h, final K k, final V v, final Entry<K, V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }

        /**
         * Gets the key.
         *
         * @return
         */
        @Override
        public final K getKey() {
            return key;
        }

        /**
         * Gets the value.
         *
         * @return
         */
        @Override
        public final V getValue() {
            return value;
        }

        /**
         * Sets the value.
         *
         * @param newValue
         * @return
         */
        @Override
        public final V setValue(final V newValue) {
            final V oldValue = value;
            value = newValue;

            return oldValue;
        }

        /**
         *
         * @param obj
         * @return
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

        @Override
        public final int hashCode() {
            return N.hashCode(getKey()) ^ N.hashCode(getValue());
        }

        @Override
        public final String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
