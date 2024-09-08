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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public class Properties<K, V> implements Map<K, V> {

    protected final Map<K, V> values;

    /**
     *
     */
    public Properties() {
        this(new ConcurrentHashMap<>());
    }

    Properties(final ConcurrentHashMap<? extends K, ? extends V> valueMap) {
        values = (Map<K, V>) valueMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> Properties<K, V> create(final Map<? extends K, ? extends V> map) {
        return new Properties<>(new ConcurrentHashMap<>(map));
    }

    /**
     *
     * @param propName
     * @return
     */
    @Override
    public V get(final Object propName) {
        return values.get(propName);
    }

    /**
     * To avoid <code>NullPointerException</code> for primitive type if the target property is null or not set.
     *
     * @param <T>
     * @param propName
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Object propName, final Class<? extends T> targetType) {
        return N.convert(values.get(propName), targetType);
    }

    /**
     * Gets the or default.
     *
     * @param propName
     * @param defaultValue is returned if the specified {@code propName} is not contained in this Properties instance or it's
     *            null.
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(final Object propName, final V defaultValue) {
        final V result = values.get(propName);

        if (result == null) {
            return defaultValue;
        }

        return result;
    }

    /**
     * Gets the or default.
     *
     * @param <T>
     * @param propName
     * @param defaultValue is returned if the specified {@code propName} is not contained in this Properties instance or it's null.
     * @param targetType
     * @return
     */
    public <T> T getOrDefault(final Object propName, final T defaultValue, final Class<? extends T> targetType) {
        final Object result = values.get(propName);

        if (result == null) {
            return defaultValue;
        }

        return N.convert(result, targetType);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public Properties<K, V> set(final K propName, final V propValue) {
        put(propName, propValue);

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V put(final K key, final V value) {
        return values.put(key, value);
    }

    /**
     *
     * @param m
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        values.putAll(m);
    }

    /**
     * Put if absent.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V putIfAbsent(final K key, final V value) {
        V v = get(key);

        if (v == null) {
            v = put(key, value);
        }

        return v;
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public V remove(final Object key) {
        return values.remove(key);
    }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public boolean remove(final Object key, final Object value) {
        final Object curValue = get(key);

        if (!Objects.equals(curValue, value) || (curValue == null && !containsKey(key))) {
            return false;
        }

        remove(key);

        return true;
    }

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public V replace(final K key, final V value) {
        V curValue;
        if (((curValue = get(key)) != null) || containsKey(key)) {
            curValue = put(key, value);
        }
        return curValue;
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     */
    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        final Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || (curValue == null && !containsKey(key))) {
            return false;
        }
        put(key, newValue);
        return true;
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public boolean containsKey(final Object key) {
        return values.containsKey(key);
    }

    /**
     *
     * @param value
     * @return
     */
    @Override
    public boolean containsValue(final Object value) {
        return values.containsValue(value);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Set<K> keySet() {
        return values.keySet();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Collection<V> values() {
        return values.values();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return values.entrySet();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return values.size();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        values.clear();
    }

    /**
     *
     *
     * @return
     */
    public Properties<K, V> copy() {
        final Properties<K, V> copy = new Properties<>();

        copy.values.putAll(values);

        return copy;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return 31 + ((values == null) ? 0 : values.hashCode());
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Properties && N.equals(((Properties<K, V>) obj).values, values));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return values.toString();
    }
}
