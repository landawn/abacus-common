/*
 * Copyright (c) 2020, Haiyang Li.
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

import java.util.NavigableMap;
import java.util.NavigableSet;

import com.landawn.abacus.util.function.Supplier;

public class LazyNavigableMap<K, V> extends LazySortedMap<K, V> implements NavigableMap<K, V> {
    protected NavigableMap<K, V> navigableMap;

    protected LazyNavigableMap(final Supplier<? extends NavigableMap<K, V>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            navigableMap = (NavigableMap<K, V>) map;
        }
    }

    public static <K, V> LazyNavigableMap<K, V> of(final NavigableMapSupplier<K, V> supplier) {
        return new LazyNavigableMap<K, V>(supplier);
    }

    /**
     * 
     * @param <K>
     * @param <V>
     * @param supplier
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <K, V> LazySortedMap<K, V> of(final SortedMapSupplier<K, V> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.higherKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.firstEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.lastEntry();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.pollFirstEntry();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.pollLastEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.descendingMap();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.navigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.descendingKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.headMap(toKey, inclusive);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.tailMap(fromKey, inclusive);
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return navigableMap.toString();
    }

    public static interface NavigableMapSupplier<K, V> extends Supplier<NavigableMap<K, V>> {

        @Override
        NavigableMap<K, V> get();

        public static <K, V> NavigableMapSupplier<K, V> of(NavigableMapSupplier<K, V> supplier) {
            return supplier;
        }
    }
}
