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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.landawn.abacus.util.function.Supplier;

public class LazyMap<K, V> implements Map<K, V> {
    protected final Supplier<? extends Map<K, V>> supplier;
    protected Map<K, V> map;
    protected boolean isInitialized = false;

    protected LazyMap(final Supplier<? extends Map<K, V>> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        this.supplier = supplier;
    }

    protected void init() {
        if (isInitialized == false) {
            isInitialized = true;
            map = Objects.requireNonNull(supplier.get());
        }
    }

    public static <K, V> LazyMap<K, V> of(final Supplier<? extends Map<K, V>> supplier) {
        return new LazyMap<K, V>(supplier);
    }

    @Override
    public boolean containsKey(Object key) {
        if (isInitialized == false) {
            init();
        }

        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (isInitialized == false) {
            init();
        }

        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (isInitialized == false) {
            init();
        }

        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (isInitialized == false) {
            init();
        }

        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if (isInitialized == false) {
            init();
        }

        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (isInitialized == false) {
            init();
        }

        map.putAll(m);
        ;
    }

    @Override
    public Set<K> keySet() {
        if (isInitialized == false) {
            init();
        }

        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        if (isInitialized == false) {
            init();
        }

        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (isInitialized == false) {
            init();
        }

        return map.entrySet();
    }

    @Override
    public int size() {
        if (isInitialized == false) {
            init();
        }

        return map.size();
    }

    @Override
    public boolean isEmpty() {
        if (isInitialized == false) {
            init();
        }

        return map.isEmpty();
    }

    @Override
    public void clear() {
        if (isInitialized == false) {
            init();
        }

        map.clear();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return obj instanceof LazyMap && map.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return map.toString();
    }
}
