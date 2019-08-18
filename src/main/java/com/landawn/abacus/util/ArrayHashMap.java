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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * It's designed to supported primitive/object array key.
 * The elements in the array must not be modified after the array is put into the map as key.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public class ArrayHashMap<K, V> implements Map<K, V> {

    /** The map. */
    private final Map<Wrapper<K>, V> map;

    /**
     * Instantiates a new array hash map.
     */
    public ArrayHashMap() {
        map = new HashMap<>();
    }

    /**
     * Instantiates a new array hash map.
     *
     * @param initialCapacity
     */
    public ArrayHashMap(final int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     * Instantiates a new array hash map.
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

        // map = N.newInstance(mapType);

        try {
            map = Modifier.isAbstract(mapType.getModifiers()) ? N.newInstance(mapType) : mapType.newInstance();
        } catch (InstantiationException e) {
            throw N.toRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Instantiates a new array hash map.
     *
     * @param m
     */
    public ArrayHashMap(final Map<? extends K, ? extends V> m) {
        if (N.isNullOrEmpty(m)) {
            map = new HashMap<>();
        } else {
            map = new HashMap<>(N.initHashCapacity(m.size()));
        }

        putAll(m);
    }

    /**
     * Gets the.
     *
     * @param key
     * @return
     */
    @Override
    public V get(Object key) {
        return map.get(Wrapper.of(key));
    }

    /**
     * Put.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V put(K key, V value) {
        return map.put(Wrapper.of(key), value);
    }

    /**
     * Put all.
     *
     * @param m
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (N.isNullOrEmpty(m)) {
            return;
        }

        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the.
     *
     * @param key
     * @return
     */
    @Override
    public V remove(Object key) {
        return map.remove(Wrapper.of(key));
    }

    /**
     * Contains key.
     *
     * @param key
     * @return true, if successful
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(Wrapper.of(key));
    }

    /**
     * Contains value.
     *
     * @param value
     * @return true, if successful
     */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * Key set.
     *
     * @return
     */
    @Override
    public Set<K> keySet() {
        return new ArrayHashSet<>(map.keySet());
    }

    /**
     * Values.
     *
     * @return
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Entry set.
     *
     * @return
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return new ArrayEntrySet<>(map.entrySet());
    }

    /**
     * Size.
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
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ArrayHashMap && ((ArrayHashMap<K, V>) obj).map.equals(map));
    }

    /**
     * To string.
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
        ArrayEntrySet(Set<Map.Entry<Wrapper<K>, V>> set) {
            this.set = set;
        }

        /**
         * Adds the.
         *
         * @param e
         * @return true, if successful
         */
        @Override
        public boolean add(java.util.Map.Entry<K, V> e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return true, if successful
         */
        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        /**
         * Removes the.
         *
         * @param o
         * @return true, if successful
         */
        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        /**
         * Contains all.
         *
         * @param c
         * @return true, if successful
         */
        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return true, if successful
         */
        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         * Retain all.
         *
         * @param c
         * @return true, if successful
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        /**
         * Contains.
         *
         * @param o
         * @return true, if successful
         */
        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;

                return set.contains(N.newEntry(Wrapper.of(entry.getKey()), entry.getValue()));
            }

            return false;
        }

        /**
         * Iterator.
         *
         * @return
         */
        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {
            return new ArrayEntryIterator<>(set.iterator());
        }

        /**
         * To array.
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

            for (Map.Entry<Wrapper<K>, V> e : set) {
                result[i++] = new ArrayEntry<>(e);
            }

            return result;
        }

        /**
         * To array.
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

            for (Map.Entry<Wrapper<K>, V> e : set) {
                result[i++] = new ArrayEntry<>(e);
            }

            return a;
        }

        /**
         * Size.
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
         * Hash code.
         *
         * @return
         */
        @Override
        public int hashCode() {
            return set.hashCode();
        }

        /**
         * Equals.
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ArrayEntrySet && ((ArrayEntrySet<K, V>) obj).set.equals(set));
        }

        /**
         * To string.
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
    static class ArrayEntryIterator<K, V> implements Iterator<java.util.Map.Entry<K, V>> {

        /** The it. */
        private final Iterator<Map.Entry<Wrapper<K>, V>> it;

        /**
         * Instantiates a new array entry iterator.
         *
         * @param it
         */
        ArrayEntryIterator(Iterator<Map.Entry<Wrapper<K>, V>> it) {
            this.it = it;
        }

        /**
         * Checks for next.
         *
         * @return true, if successful
         */
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        /**
         * Next.
         *
         * @return
         */
        @Override
        public java.util.Map.Entry<K, V> next() {
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
        ArrayEntry(Map.Entry<Wrapper<K>, V> entry) {
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
        public V setValue(V value) {
            return entry.setValue(value);
        }

        /**
         * Hash code.
         *
         * @return
         */
        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        /**
         * Equals.
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ArrayEntry && ((ArrayEntry<K, V>) obj).entry.equals(entry));
        }

        /**
         * To string.
         *
         * @return
         */
        @Override
        public String toString() {
            return entry.toString();
        }
    }
}
