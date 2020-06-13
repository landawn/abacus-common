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

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

import com.landawn.abacus.util.function.Supplier;

public class LazySortedMap<K, V> extends LazyMap<K, V> implements SortedMap<K, V> {
    protected SortedMap<K, V> sortedMap;

    protected LazySortedMap(final Supplier<? extends SortedMap<K, V>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            sortedMap = (SortedMap<K, V>) map;
        }
    }

    public static <K, V> LazySortedMap<K, V> of(final SortedMapSupplier<K, V> supplier) {
        return new LazySortedMap<K, V>(supplier);
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
    public static <K, V> LazyMap<K, V> of(final Supplier<? extends Map<K, V>> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Comparator<? super K> comparator() {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.comparator();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.subMap(fromKey, toKey);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.headMap(toKey);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.tailMap(fromKey);
    }

    @Override
    public K firstKey() {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.firstKey();

    }

    @Override
    public K lastKey() {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.lastKey();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return sortedMap.toString();
    }

    public static interface SortedMapSupplier<K, V> extends Supplier<SortedMap<K, V>> {

        @Override
        SortedMap<K, V> get();

        public static <K, V> SortedMapSupplier<K, V> of(SortedMapSupplier<K, V> supplier) {
            return supplier;
        }
    }
}
