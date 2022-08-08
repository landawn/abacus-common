/*
 * Copyright (C) 2015 HaiYang Li
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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * It's is a multiple-thread safety map with fixed size. it's designed for frequent get and few add/remove operation
 * with a few limited keys.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
@Internal
@Beta
public final class ObjectPool<K, V> extends AbstractMap<K, V> {

    final int capacity;

    private final Entry<K, V>[] table;

    private final int indexMask;

    private int size = 0;

    private Set<K> keySet = null;

    private Collection<V> values = null;

    private Set<java.util.Map.Entry<K, V>> entrySet;

    @SuppressWarnings("unchecked")
    public ObjectPool(int capacity) {
        this.capacity = capacity;
        this.table = new Entry[capacity];
        this.indexMask = table.length - 1;
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V get(Object key) {
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
    public V put(K key, V value) {
        if ((key == null) || (value == null)) {
            throw new NullPointerException();
        }

        final int hash = hash(key);
        final int i = hash & indexMask;

        synchronized (table) {
            //            if (size() > capacity) {
            //                throw new IndexOutOfBoundsException("Object pool is full with capacity=" + capacity);
            //            }
            //
            for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
                if ((hash == entry.hash) && key.equals(entry.key)) {
                    V previousValue = entry.value;
                    entry.value = value;

                    return previousValue;
                }
            }

            Entry<K, V> entry = new Entry<>(hash, key, value, table[i]);
            table[i] = entry;

            keySet = null;
            values = null;
            entrySet = null;

            size++;

            return null;
        }
    }

    /**
     *
     * @param m
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            if (entry.getValue() != null) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V remove(Object key) {
        if (size == 0) {
            return null;
        }

        final int hash = hash(key);
        final int i = hash & indexMask;

        synchronized (table) {
            Entry<K, V> prev = table[i];
            Entry<K, V> e = prev;

            while (e != null) {
                Entry<K, V> next = e.next;

                if ((hash == e.hash) && key.equals(e.key)) {
                    if (prev == e) {
                        table[i] = next;
                    } else {
                        prev.next = next;
                    }

                    keySet = null;
                    values = null;
                    entrySet = null;

                    size--;

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
    public boolean containsKey(Object key) {
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
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }

        for (Entry<K, V> element : table) {
            for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                if ((entry.value != null) && value.equals(entry.value)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Set<K> keySet() {
        Set<K> tmp = keySet;

        if (tmp == null) {
            tmp = N.newHashSet(size);

            for (Entry<K, V> element : table) {
                for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                    if (entry.value != null) {
                        tmp.add(entry.key);
                    }
                }
            }

            tmp = Collections.unmodifiableSet(tmp);

            keySet = tmp;
        }

        return tmp;
    }

    @Override
    public Collection<V> values() {
        Collection<V> tmp = values;

        if (tmp == null) {
            tmp = N.newHashSet(size);

            V value = null;

            for (Entry<K, V> element : table) {
                for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                    value = entry.value;

                    if (value != null) {
                        tmp.add(value);
                    }
                }
            }

            tmp = Collections.unmodifiableCollection(tmp);

            values = tmp;
        }

        return tmp;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Set<java.util.Map.Entry<K, V>> tmp = entrySet;

        if (tmp == null) {
            tmp = N.newHashSet(size);

            for (Entry<K, V> element : table) {
                for (Entry<K, V> entry = element; entry != null; entry = entry.next) {
                    if (entry.value != null) {
                        tmp.add(entry);
                    }
                }
            }

            tmp = Collections.unmodifiableSet(tmp);

            entrySet = tmp;
        }

        return tmp;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        synchronized (table) {
            Arrays.fill(table, null);

            keySet = null;
            values = null;
            entrySet = null;

            size = 0;
        }
    }

    /**
     *
     * @param key
     * @return
     */
    static int hash(Object key) {
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
        int hash;

        /**
         * Creates new entry.
         *
         * @param h
         * @param k
         * @param v
         * @param n
         */
        Entry(int h, K k, V v, Entry<K, V> n) {
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
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;

            return oldValue;
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Map.Entry) {
                Map.Entry<K, V> other = (Map.Entry<K, V>) obj;

                return N.equals(getKey(), other.getKey()) && N.equals(getValue(), other.getValue());
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public final int hashCode() {
            return N.hashCode(getKey()) ^ N.hashCode(getValue());
        }

        /**
         *
         * @return
         */
        @Override
        public final String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
