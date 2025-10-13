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
 * This class provides a type-safe wrapper around a Map with additional convenience methods
 * for retrieving values with type conversion and default values.
 * 
 * <p>Unlike {@link java.util.Properties}, this class is generic and can store any type of objects,
 * not just strings. It also provides methods to retrieve values converted to specific types.</p>
 * 
 * <p>All arguments to all task methods must be {@code non-null}.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Properties<String, Object> props = new Properties<>();
 * props.set("timeout", 30);
 * props.set("url", "https://example.com");
 * 
 * // Type-safe retrieval
 * int timeout = props.get("timeout", Integer.class);
 * String url = props.get("url", String.class);
 * 
 * // With default values
 * boolean debug = props.getOrDefault("debug", false, Boolean.class);
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map. It must not be {@code null}.
 * @param <V> the type of mapped values
 * @see Map
 * @see java.util.Properties
 */
public class Properties<K, V> implements Map<K, V> {

    protected volatile Map<K, V> values;

    /**
     * Constructs an empty Properties instance with a LinkedHashMap as the underlying map.
     * The LinkedHashMap preserves the insertion order of properties.
     */
    public Properties() {
        this(new LinkedHashMap<>());
    }

    Properties(final Map<? extends K, ? extends V> valueMap) {
        values = (Map<K, V>) valueMap;
    }

    /**
     * Creates a new Properties instance from the specified map.
     * The returned Properties object contains a copy of the entries from the input map.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 30);
     * Properties<String, Object> props = Properties.create(map);
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("name", "John");
     * Object name = props.get("name"); // Returns "John"
     * }</pre>
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
     * This method helps avoid {@code NullPointerException} for primitive types if the target property is {@code null} or not set.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("age", "25");
     * props.put("active", true);
     * 
     * int age = props.get("age", Integer.class); // Returns 25
     * boolean active = props.get("active", Boolean.class); // Returns true
     * double salary = props.get("salary", Double.class); // Returns 0.0 (default for double)
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("host", "localhost");
     * 
     * String host = props.getOrDefault("host", "0.0.0.0"); // Returns "localhost"
     * String port = props.getOrDefault("port", "8080"); // Returns "8080"
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("timeout", "30");
     * 
     * int timeout = props.getOrDefault("timeout", 60, Integer.class); // Returns 30
     * boolean debug = props.getOrDefault("debug", false, Boolean.class); // Returns false
     * }</pre>
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
     * This method is a fluent alternative to {@link #put(Object, Object)} that returns the Properties instance
     * for method chaining.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>()
     *     .set("host", "localhost")
     *     .set("port", 8080)
     *     .set("debug", true);
     * }</pre>
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
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Integer> props = new Properties<>();
     * props.put("count", 10);
     * Integer old = props.put("count", 20); // Returns 10
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props1 = new Properties<>();
     * props1.put("a", 1);
     * 
     * Map<String, Object> map = new HashMap<>();
     * map.put("b", 2);
     * map.put("c", 3);
     * 
     * props1.putAll(map); // props1 now contains: a=1, b=2, c=3
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("name", "John");
     * 
     * String v1 = props.putIfAbsent("name", "Jane"); // Returns "John", doesn't change value
     * String v2 = props.putIfAbsent("age", "30"); // Returns null, adds age=30
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     */
    @Override
    public V putIfAbsent(final K key, final V value) {
        V v = get(key);

        if (v == null) {
            put(key, value);
        }

        return v;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("temp", "value");
     * Object removed = props.remove("temp"); // Returns "value"
     * }</pre>
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or {@code null} if there was no mapping for key
     */
    @Override
    public V remove(final Object key) {
        return values.remove(key);
    }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("status", "active");
     * 
     * boolean removed1 = props.remove("status", "inactive"); // Returns false
     * boolean removed2 = props.remove("status", "active"); // Returns true
     * }</pre>
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
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
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Integer> props = new Properties<>();
     * props.put("version", 1);
     * 
     * Integer old = props.replace("version", 2); // Returns 1
     * Integer none = props.replace("missing", 3); // Returns null, no change
     * }</pre>
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key
     */
    @Override
    public V replace(final K key, final V value) {
        V curValue = null;
        if (((curValue = get(key)) != null) || containsKey(key)) {
            curValue = put(key, value);
        }
        return curValue;
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("status", "draft");
     * 
     * boolean replaced1 = props.replace("status", "published", "approved"); // Returns false
     * boolean replaced2 = props.replace("status", "draft", "published"); // Returns true
     * }</pre>
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
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
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("name", "John");
     * 
     * boolean hasName = props.containsKey("name"); // Returns true
     * boolean hasAge = props.containsKey("age"); // Returns false
     * }</pre>
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(final Object key) {
        return values.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("host", "localhost");
     * props.put("backup", "localhost");
     * 
     * boolean hasLocalhost = props.containsValue("localhost"); // Returns true
     * }</pre>
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(final Object value) {
        return values.containsValue(value);
    }

    /**
     * Returns a Set view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("name", "John");
     * props.put("age", 30);
     * Set<String> keys = props.keySet(); // Returns ["name", "age"]
     * }</pre>
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        return values.keySet();
    }

    /**
     * Returns a Collection view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are reflected in the collection, and vice-versa.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Integer> props = new Properties<>();
     * props.put("a", 1);
     * props.put("b", 2);
     * Collection<Integer> vals = props.values(); // Returns [1, 2]
     * }</pre>
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<V> values() {
        return values.values();
    }

    /**
     * Returns a Set view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("x", 10);
     * props.put("y", 20);
     * for (Map.Entry<String, Object> entry : props.entrySet()) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return values.entrySet();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * boolean empty1 = props.isEmpty(); // Returns true
     * 
     * props.put("key", "value");
     * boolean empty2 = props.isEmpty(); // Returns false
     * }</pre>
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("a", 1);
     * props.put("b", 2);
     * int count = props.size(); // Returns 2
     * }</pre>
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return values.size();
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("a", 1);
     * props.clear();
     * boolean empty = props.isEmpty(); // Returns true
     * }</pre>
     */
    @Override
    public void clear() {
        values.clear();
    }

    /**
     * Creates a shallow copy of this Properties instance.
     * The keys and values themselves are not cloned.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> original = new Properties<>();
     * original.put("key", "value");
     * 
     * Properties<String, Object> copy = original.copy();
     * copy.put("key2", "value2"); // Doesn't affect original
     * }</pre>
     *
     * @return a shallow copy of this Properties instance
     */
    public Properties<K, V> copy() {
        final Properties<K, V> copy = new Properties<>();

        copy.values.putAll(values);

        return copy;
    }

    /**
     * Returns a hash code value for this Properties object.
     * The hash code is computed based on the underlying map of key-value mappings.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("key", "value");
     * int hash = props.hashCode();
     * }</pre>
     *
     * @return a hash code value for this Properties object
     */
    @Override
    public int hashCode() {
        return 31 + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Compares the specified object with this Properties for equality.
     * Returns {@code true} if the given object is also a Properties instance
     * and the two Properties represent the same mappings.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props1 = new Properties<>();
     * props1.put("key", "value");
     *
     * Properties<String, Object> props2 = new Properties<>();
     * props2.put("key", "value");
     *
     * boolean same = props1.equals(props2); // Returns true
     * }</pre>
     *
     * @param obj object to be compared for equality with this Properties
     * @return {@code true} if the specified object is equal to this Properties
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Properties && N.equals(((Properties<K, V>) obj).values, values));
    }

    /**
     * Returns a string representation of this Properties object.
     * The string representation consists of the string representation of the underlying map.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("name", "John");
     * props.put("age", 30);
     * String str = props.toString(); // Returns "{name=John, age=30}"
     * }</pre>
     *
     * @return a string representation of this Properties object
     */
    @Override
    public String toString() {
        return values.toString();
    }

    /**
     * Resets the internal map with new values.
     * This is an internal method used for auto-refresh functionality.
     *
     * @param newValues the new map to use as the internal storage
     */
    void reset(final Map<K, V> newValues) {
        values = newValues;
    }
}