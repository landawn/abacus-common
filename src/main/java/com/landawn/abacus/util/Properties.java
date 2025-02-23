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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A generic Properties class that implements the Map interface.
 *
 * <p>All arguments to all task methods must be {@code non-null}.</p>
 *
 * @param <K> the type of keys maintained by this map. It must not be {@code null}.
 * @param <V> the type of mapped values
 * @see Map
 */
public class Properties<K, V> implements Map<K, V> {

    protected volatile Map<K, V> values;

    /**
     * Constructs an empty Properties instance with a ConcurrentHashMap as the underlying map.
     */
    public Properties() {
        this(new LinkedHashMap<>());
    }

    Properties(final Map<? extends K, ? extends V> valueMap) {
        values = (Map<K, V>) valueMap;
    }

    /**
     * Creates a new Properties instance from the specified map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map from which to create the Properties instance
     * @return a new Properties instance containing the entries from the specified map
     */
    public static <K, V> Properties<K, V> create(final Map<? extends K, ? extends V> map) {
        return new Properties<>(new LinkedHashMap<>(map));
    }

    /**
     * Retrieves the value associated with the specified property name.
     *
     * @param propName the name of the property whose associated value is to be returned
     * @return the value associated with the specified property name, or {@code null} if the property is not found
     */
    @Override
    public V get(final Object propName) {
        return values.get(propName);
    }

    /**
     * Retrieves the value associated with the specified property name and converts it to the specified target type.
     * To avoid {@code NullPointerException} for primitive type if the target property is {@code null} or not set.
     *
     * @param <T> the type to which the value should be converted
     * @param propName the name of the property whose associated value is to be returned
     * @param targetType the class of the type to which the value should be converted
     * @return the value associated with the specified property name, converted to the specified target type, or default value of {@code targetType} if the property is not found or its value is {@code null}
     */
    public <T> T get(final Object propName, final Class<? extends T> targetType) {
        //noinspection SuspiciousMethodCalls
        return N.convert(values.get(propName), targetType);
    }

    /**
     * Retrieves the value associated with the specified property name or returns the default value if the property is not found.
     *
     * @param propName the name of the property whose associated value is to be returned
     * @param defaultValue the value to be returned if the specified property name is not found or its value is {@code null}
     * @return the value associated with the specified property name, or {@code defaultValue} if the property is not found or its value is {@code null}
     */
    @Override
    public V getOrDefault(final Object propName, final V defaultValue) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V result = values.get(propName);

        if (result == null) {
            return defaultValue;
        }

        return result;
    }

    /**
     * Retrieves the value associated with the specified property name or returns the default value if the property is not found.
     * Converts the value to the specified target type.
     *
     * @param <T> the type to which the value should be converted
     * @param propName the name of the property whose associated value is to be returned
     * @param defaultValue the value to be returned if the specified property name is not found or its value is {@code null}
     * @param targetType the class of the type to which the value should be converted
     * @return the value associated with the specified property name, converted to the specified target type, or {@code defaultValue} if the property is not found or its value is {@code null}
     */
    public <T> T getOrDefault(final Object propName, final T defaultValue, final Class<? extends T> targetType) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final Object result = values.get(propName);

        if (result == null) {
            return defaultValue;
        }

        return N.convert(result, targetType);
    }

    /**
     * Sets the specified property name to the specified property value.
     *
     * @param propName the name of the property to be set
     * @param propValue the value to be set for the specified property name
     * @return the current Properties instance with the updated property
     */
    public Properties<K, V> set(final K propName, final V propValue) {
        put(propName, propValue);

        return this;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     */
    @Override
    public V put(final K key, final V value) {
        return values.put(key, value);
    }

    /**
     * Copies all the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for any of the keys currently in the specified map.
     *
     * @param m the mappings to be stored in this map
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        values.putAll(m);
    }

    /**
     * Associates the specified value with the specified key in this map if the key is not already associated with a value.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
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

    @Override
    public Set<K> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<V> values() {
        return values.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return values.entrySet();
    }

    /**
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

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

    public Properties<K, V> copy() {
        final Properties<K, V> copy = new Properties<>();

        copy.values.putAll(values);

        return copy;
    }

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

    @Override
    public String toString() {
        return values.toString();
    }

    void reset(final Map<K, V> newValues) {
        values = newValues;
    }
}
