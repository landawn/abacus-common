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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * It's designed to supported primitive/object array key.
 * The elements in the array must not be modified after the array is put into the map as key.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public sealed class ArrayHashMap<K, V> implements Map<K, V> permits LinkedArrayHashMap {

    private final Map<Wrapper<K>, V> map;

    /**
     *
     */
    public ArrayHashMap() {
        map = new HashMap<>();
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public ArrayHashMap(final int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     *
     *
     * @param mapType
     */
    @SuppressWarnings("rawtypes")
    public ArrayHashMap(final Class<? extends Map> mapType) {
        //  StackOverflowError

        /*
        ......
        at java.lang.Class.getDeclaredConstructor(Class.java:2066)
        at com.landawn.abacus.util.N.getDeclaredConstructor(N.java:1554)
        at com.landawn.abacus.util.N.newInstance(N.java:3180)
        at com.landawn.abacus.util.ArrayHashMap.<init>(ArrayHashMap.java:45)
        at com.landawn.abacus.util.N.getDeclaredConstructor(N.java:1564)
        at com.landawn.abacus.util.N.newInstance(N.java:3180)
        at com.landawn.abacus.util.ArrayHashMap.<init>(ArrayHashMap.java:45)
        ......
        */

        map = N.newMap(mapType);
    }

    /**
     *
     *
     * @param m
     */
    public ArrayHashMap(final Map<? extends K, ? extends V> m) {
        if (N.isEmpty(m)) {
            map = new HashMap<>();
        } else {
            map = N.newHashMap(m.size());
        }

        putAll(m); // NOSONAR
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V get(final Object key) {
        return map.get(Wrapper.of(key));
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V put(final K key, final V value) {
        return map.put(Wrapper.of(key), value);
    }

    /**
     *
     * @param m
     */
    @Override
    public final void putAll(final Map<? extends K, ? extends V> m) {
        if (N.isEmpty(m)) {
            return;
        }

        for (final Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V remove(final Object key) {
        return map.remove(Wrapper.of(key));
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(Wrapper.of(key));
    }

    /**
     *
     * @param value
     * @return
     */
    @Override
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Set<K> keySet() {
        return new ArrayHashSet<>(map.keySet());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new ArrayEntrySet<>(map.entrySet());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof ArrayHashMap && ((ArrayHashMap<K, V>) obj).map.equals(map));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * The Class ArrayEntrySet.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    static class ArrayEntrySet<K, V> implements Set<Map.Entry<K, V>> {

        /** The set. */
        private final Set<Map.Entry<Wrapper<K>, V>> set;

        /**
         * Instantiates a new array entry set.
         *
         * @param set
         */
        ArrayEntrySet(final Set<Map.Entry<Wrapper<K>, V>> set) {
            this.set = set;
        }

        /**
         *
         * @param e
         * @return
         */
        @Override
        public boolean add(final Map.Entry<K, V> e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        @Override
        public boolean addAll(final Collection<? extends Map.Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param c
         * @return
         */
        @Override
        public boolean containsAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        @Override
        public boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param c
         * @return
         */
        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            if (valueToFind instanceof Map.Entry) {
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) valueToFind;

                return set.contains(N.newEntry(Wrapper.of(entry.getKey()), entry.getValue()));
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new ArrayEntryIterator<>(set.iterator());
        }

        /**
         *
         * @return
         */
        @Override
        public Object[] toArray() {
            final int size = size();

            if (size == 0) {
                return N.EMPTY_OBJECT_ARRAY;
            }

            final Object[] result = new Object[size];
            int i = 0;

            for (final Map.Entry<Wrapper<K>, V> e : set) {
                result[i++] = new ArrayEntry<>(e);
            }

            return result;
        }

        /**
         *
         * @param <T>
         * @param a
         * @return
         */
        @Override
        public <T> T[] toArray(T[] a) {
            final int size = size();

            if (a.length < size) {
                a = N.newArray(a.getClass().getComponentType(), size);
            }

            final Object[] result = a;
            int i = 0;

            for (final Map.Entry<Wrapper<K>, V> e : set) {
                result[i++] = new ArrayEntry<>(e);
            }

            return a;
        }

        /**
         *
         * @return
         */
        @Override
        public int size() {
            return set.size();
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        /**
         * Clear.
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return set.hashCode();
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof ArrayEntrySet && ((ArrayEntrySet<K, V>) obj).set.equals(set));
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return set.toString();
        }
    }

    /**
     * The Class ArrayEntryIterator.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    static class ArrayEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        /** The it. */
        private final Iterator<Map.Entry<Wrapper<K>, V>> it;

        /**
         * Instantiates a new array entry iterator.
         *
         * @param it
         */
        ArrayEntryIterator(final Iterator<Map.Entry<Wrapper<K>, V>> it) {
            this.it = it;
        }

        /**
         * Checks for next.
         *
         * @return
         */
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        /**
         *
         * @return
         */
        @Override
        public Map.Entry<K, V> next() {
            return new ArrayEntry<>(it.next());
        }

        /**
         * Removes the.
         */
        @Override
        public void remove() {
            it.remove();
        }
    }

    /**
     * The Class ArrayEntry.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    static class ArrayEntry<K, V> implements Map.Entry<K, V> {

        /** The entry. */
        private final Map.Entry<Wrapper<K>, V> entry;

        /**
         * Instantiates a new array entry.
         *
         * @param entry
         */
        ArrayEntry(final Map.Entry<Wrapper<K>, V> entry) {
            this.entry = entry;
        }

        /**
         * Gets the key.
         *
         * @return
         */
        @Override
        public K getKey() {
            return entry.getKey().value();
        }

        /**
         * Gets the value.
         *
         * @return
         */
        @Override
        public V getValue() {
            return entry.getValue();
        }

        /**
         * Sets the value.
         *
         * @param value
         * @return
         */
        @Override
        public V setValue(final V value) {
            return entry.setValue(value);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof ArrayEntry && ((ArrayEntry<K, V>) obj).entry.equals(entry));
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return entry.toString();
        }
    }
}
