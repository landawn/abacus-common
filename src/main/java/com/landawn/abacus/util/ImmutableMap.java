/*
 * Copyright (C) 2016 HaiYang Li
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
public class ImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable {

    @SuppressWarnings("rawtypes")
    private static final ImmutableMap EMPTY = new ImmutableMap(Collections.emptyMap());

    private final Map<K, V> map;

    ImmutableMap(final Map<? extends K, ? extends V> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> ImmutableMap<K, V> empty() {
        return EMPTY;
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1) {
        return new ImmutableMap<>(Collections.singletonMap(k1, v1));
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2);
        return new ImmutableMap<>(map);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @param k3 
     * @param v3 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2, k3, v3);
        return new ImmutableMap<>(map);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @param k3 
     * @param v3 
     * @param k4 
     * @param v4 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4);
        return new ImmutableMap<>(map);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @param k3 
     * @param v3 
     * @param k4 
     * @param v4 
     * @param k5 
     * @param v5 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        return new ImmutableMap<>(map);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @param k3 
     * @param v3 
     * @param k4 
     * @param v4 
     * @param k5 
     * @param v5 
     * @param k6 
     * @param v6 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        return new ImmutableMap<>(map);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 
     * @param v1 
     * @param k2 
     * @param v2 
     * @param k3 
     * @param v3 
     * @param k4 
     * @param v4 
     * @param k5 
     * @param v5 
     * @param k6 
     * @param v6 
     * @param k7 
     * @param v7 
     * @return 
     */
    public static <K, V> ImmutableMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final Map<K, V> map = N.asLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        return new ImmutableMap<>(map);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableMap<K, V> copyOf(final Map<? extends K, ? extends V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        final Map<K, V> tmp = map instanceof IdentityHashMap ? new IdentityHashMap<>(map)
                : ((map instanceof LinkedHashMap || map instanceof SortedMap) ? new LinkedHashMap<>(map) : new HashMap<>(map));

        return new ImmutableMap<>(tmp);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return an {@code ImmutableMap} backed by the specified {@code map}
     * @deprecated the ImmutableMap may be modified through the specified {@code map}
     */
    @Deprecated
    public static <K, V> ImmutableMap<K, V> wrap(final Map<? extends K, ? extends V> map) {
        if (map == null) {
            return empty();
        } else if (map instanceof ImmutableMap) {
            return (ImmutableMap<K, V>) map;
        }

        return new ImmutableMap<>(map);
    }

    /**
     * Gets the or default.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        final V val = get(key);

        return val == null && !containsKey(key) ? defaultValue : val;
    }

    /**
     * 
     *
     * @param k 
     * @param v 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V put(K k, V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param o 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V remove(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param map 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final void putAll(Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Put if absent.
     *
     * @param key 
     * @param value 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V putIfAbsent(K key, V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param key 
     * @param value 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean remove(Object key, Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param key 
     * @param oldValue 
     * @param newValue 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean replace(K key, V oldValue, V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param key 
     * @param value 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V replace(K key, V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute if absent.
     *
     * @param key 
     * @param mappingFunction 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute if present.
     *
     * @param key 
     * @param remappingFunction 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param key 
     * @param remappingFunction 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param key 
     * @param value 
     * @param remappingFunction 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
     *
     * @param key
     * @return
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     *
     * @param value
     * @return
     */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V get(Object key) {
        return map.get(key);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
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
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
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
     * 
     *
     * @param <K> 
     * @param <V> 
     * @return 
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * 
     *
     * @param <K> 
     * @param <V> 
     * @param backedMap 
     * @return 
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> backedMap) {
        N.checkArgNotNull(backedMap);

        return new Builder<>(backedMap);
    }

    public static final class Builder<K, V> {
        private final Map<K, V> map;

        Builder() {
            map = new HashMap<>();
        }

        Builder(final Map<K, V> backedMap) {
            map = backedMap;
        }

        /**
         * 
         *
         * @param key 
         * @param value 
         * @return 
         */
        public Builder<K, V> put(final K key, final V value) {
            map.put(key, value);

            return this;
        }

        /**
         * 
         *
         * @param m 
         * @return 
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notNullOrEmpty(m)) {
                map.putAll(m);
            }

            return this;
        }

        /**
         * 
         *
         * @return 
         */
        public ImmutableMap<K, V> build() {
            return ImmutableMap.wrap(map);
        }
    }
}
