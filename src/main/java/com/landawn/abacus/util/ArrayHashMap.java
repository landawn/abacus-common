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

/**
 * It's designed to supported primitive/object array key.
 * The elements in the array must not be modified after the array is put into the map as key.
 *
 * @since 0.8
 *
 * @author Haiyang Li
 */
public class ArrayHashMap<K, V> implements Map<K, V> {
    private final Map<Wrapper<K>, V> map;

    public ArrayHashMap() {
        map = new HashMap<>();
    }

    public ArrayHashMap(final int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

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

    public ArrayHashMap(final Map<? extends K, ? extends V> m) {
        if (N.isNullOrEmpty(m)) {
            map = new HashMap<>();
        } else {
            map = new HashMap<>(N.initHashCapacity(m.size()));
        }

        putAll(m);
    }

    @Override
    public V get(Object key) {
        return map.get(Wrapper.of(key));
    }

    @Override
    public V put(K key, V value) {
        return map.put(Wrapper.of(key), value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (N.isNullOrEmpty(m)) {
            return;
        }

        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        return map.remove(Wrapper.of(key));
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(Wrapper.of(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Set<K> keySet() {
        return new ArrayHashSet<>(map.keySet());
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return new ArrayEntrySet<>(map.entrySet());
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ArrayHashMap && ((ArrayHashMap<K, V>) obj).map.equals(map));
    }

    @Override
    public String toString() {
        return map.toString();
    }

    static class ArrayEntrySet<K, V> implements Set<Map.Entry<K, V>> {
        private final Set<Map.Entry<Wrapper<K>, V>> set;

        ArrayEntrySet(Set<Map.Entry<Wrapper<K>, V>> set) {
            this.set = set;
        }

        @Override
        public boolean add(java.util.Map.Entry<K, V> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;

                return set.contains(N.newEntry(Wrapper.of(entry.getKey()), entry.getValue()));
            }

            return false;
        }

        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {
            return new ArrayEntryIterator<>(set.iterator());
        }

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

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ArrayEntrySet && ((ArrayEntrySet<K, V>) obj).set.equals(set));
        }

        @Override
        public String toString() {
            return set.toString();
        }
    }

    static class ArrayEntryIterator<K, V> implements Iterator<java.util.Map.Entry<K, V>> {
        private final Iterator<Map.Entry<Wrapper<K>, V>> it;

        ArrayEntryIterator(Iterator<Map.Entry<Wrapper<K>, V>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public java.util.Map.Entry<K, V> next() {
            return new ArrayEntry<>(it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    static class ArrayEntry<K, V> implements Map.Entry<K, V> {
        private final Map.Entry<Wrapper<K>, V> entry;

        ArrayEntry(Map.Entry<Wrapper<K>, V> entry) {
            this.entry = entry;
        }

        @Override
        public K getKey() {
            return entry.getKey().value();
        }

        @Override
        public V getValue() {
            return entry.getValue();
        }

        @Override
        public V setValue(V value) {
            return entry.setValue(value);
        }

        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ArrayEntry && ((ArrayEntry<K, V>) obj).entry.equals(entry));
        }

        @Override
        public String toString() {
            return entry.toString();
        }
    }
}
