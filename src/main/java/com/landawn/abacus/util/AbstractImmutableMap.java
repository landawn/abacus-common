/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An immutable, thread-safe implementation of the Map interface.
 * Once created, the contents of an ImmutableMap cannot be modified.
 * All mutating operations (put, remove, clear, etc.) will throw UnsupportedOperationException.
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 * <li>{@link #of(Object, Object)} - creates maps with specific key-value pairs</li>
 * <li>{@link #copyOf(Map)} - creates a defensive copy from another map</li>
 * </ul>
 * 
 * <p>The implementation preserves the iteration order of entries when created from a LinkedHashMap
 * or SortedMap, otherwise no specific iteration order is guaranteed.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an immutable map with specific entries
 * ImmutableMap<String, Integer> map = ImmutableMap.of(
 *     "one", 1,
 *     "two", 2,
 *     "three", 3
 * );
 * 
 * // Create from a builder
 * ImmutableMap<String, String> built = ImmutableMap.<String, String>builder()
 *     .put("key1", "value1")
 *     .put("key2", "value2")
 *     .build();
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Map
 * @see Immutable
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
abstract class AbstractImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable {

    final Map<K, V> map;

    final Map<K, V> valueMap;

    AbstractImmutableMap(final Map<? extends K, ? extends V> map) {
        this(map, ClassUtil.isPossibleImmutable(map.getClass())); // to create immutable keySet(), values(), entrySet()
    }

    AbstractImmutableMap(final Map<? extends K, ? extends V> map, final boolean isUnmodifiable) {
        this.valueMap = (Map<K, V>) map;
        this.map = isUnmodifiable ? valueMap : Collections.unmodifiableMap(valueMap); // to create immutable keySet(), values(), entrySet()
    }

    /**
     * Returns the value to which the specified key is mapped, or the defaultValue if this map
     * contains no mapping for the key. This method distinguishes between a key that is mapped
     * to {@code null} and a key that is not present in the map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.getOrDefault("a", 0));    // 1
     * System.out.println(map.getOrDefault("c", 0));    // 0
     * System.out.println(map.getOrDefault("c", 99));   // 99
     * }</pre>
     *
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map contains no mapping for the key.
     * @return the value to which the specified key is mapped, or defaultValue if no mapping exists.
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        final V val = get(key);

        return val == null && !containsKey(key) ? defaultValue : val;
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param k ignored.
     * @param v ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V put(final K k, final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param o ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V remove(final Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param map ignored.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final void putAll(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V putIfAbsent(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final boolean remove(final Object key, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param oldValue ignored.
     * @param newValue ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final boolean replace(final K key, final V oldValue, final V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V replace(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param mappingFunction ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param remappingFunction ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param remappingFunction ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param key ignored.
     * @param value ignored.
     * @param remappingFunction ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableMap.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableMap does not support modification operations.
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * This method has the same behavior as checking if size() == 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> empty = ImmutableMap.empty();
     * ImmutableMap<String, Integer> nonEmpty = ImmutableMap.of("a", 1);
     * System.out.println(empty.isEmpty());      // true
     * System.out.println(nonEmpty.isEmpty());   // false
     * }</pre>
     *
     * @return {@code true} if this map contains no key-value mappings, {@code false} otherwise.
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * More formally, returns {@code true} if and only if this map contains a mapping for a key k
     * such that (key==null ? k==null : key.equals(k)).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.containsKey("a"));    // true
     * System.out.println(map.containsKey("c"));    // false
     * System.out.println(map.containsKey(null));   // false (unless null key was added)
     * }</pre>
     *
     * @param key the key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified key.
     * @see java.util.Map#containsKey(Object)
     */
    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     * More formally, returns {@code true} if and only if this map contains at least one mapping
     * to a value v such that (value==null ? v==null : value.equals(v)).
     * This operation requires linear time in the size of the map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);
     * System.out.println(map.containsValue(1));   // true
     * System.out.println(map.containsValue(3));   // false
     * }</pre>
     *
     * @param value the value whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the specified value.
     * @see java.util.Map#containsValue(Object)
     */
    @Override
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key. A return value of {@code null} does not necessarily indicate that the
     * map contains no mapping for the key; it's also possible that the map explicitly maps
     * the key to {@code null}. The containsKey operation may be used to distinguish these two cases.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map.get("a"));   // 1
     * System.out.println(map.get("c"));   // null
     * }</pre>
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null} if no mapping exists.
     * @see java.util.Map#get(Object)
     */
    @Override
    public V get(final Object key) {
        return map.get(key);
    }

    /**
     * Returns an unmodifiable Set view of the keys contained in this map.
     * The set is backed by the map, so it reflects the current state of the map.
     * Attempts to modify the returned set will result in an UnsupportedOperationException.
     * The iteration order of the set matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Set<String> keys = map.keySet();
     * System.out.println(keys);   // [a, b]
     * // keys.add("c");   // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an unmodifiable set view of the keys contained in this map.
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns an unmodifiable Collection view of the values contained in this map.
     * The collection is backed by the map, so it reflects the current state of the map.
     * Attempts to modify the returned collection will result in an UnsupportedOperationException.
     * The iteration order of the collection matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Collection<Integer> values = map.values();
     * System.out.println(values);   // [1, 2]
     * // values.remove(1);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an unmodifiable collection view of the values contained in this map.
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns an unmodifiable Set view of the mappings contained in this map.
     * The set is backed by the map, so it reflects the current state of the map.
     * Each element in the returned set is an immutable Map.Entry.
     * Attempts to modify the returned set or its entries will result in an UnsupportedOperationException.
     * The iteration order of the set matches the iteration order of the underlying map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * Set<Map.Entry<String, Integer>> entries = map.entrySet();
     * for (Map.Entry<String, Integer> entry : entries) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @return an unmodifiable set view of the mappings contained in this map.
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns the number of key-value mappings in this map.
     * If the map contains more than Integer.MAX_VALUE elements, returns Integer.MAX_VALUE.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
     * System.out.println(map.size());   // 3
     * }</pre>
     *
     * @return the number of key-value mappings in this map.
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a map and the two maps represent the same mappings.
     * More formally, two maps m1 and m2 are equal if m1.entrySet().equals(m2.entrySet()).
     * This ensures that the equals method works properly across different implementations of the Map interface.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map3 = ImmutableMap.of("a", 1, "c", 3);
     * System.out.println(map1.equals(map2));   // true
     * System.out.println(map1.equals(map3));   // false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this map.
     * @return {@code true} if the specified object is equal to this map.
     * @see java.util.Map#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof AbstractImmutableMap im && valueMap.equals(im.valueMap);
    }

    /**
     * Returns the hash code value for this map.
     * The hash code is computed as the sum of the hash codes of each entry in the map's entry set.
     * This ensures that two maps that are equal (according to the equals method) will have the same hash code.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
     * ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map1.hashCode() == map2.hashCode());   // true
     * }</pre>
     *
     * @return the hash code value for this map.
     * @see java.util.Map#hashCode()
     */
    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    /**
     * Returns a string representation of this map.
     * The string representation consists of a list of key-value mappings in the order returned by the map's entry set,
     * enclosed in braces ("{}"). Each key-value mapping is represented as the key followed by an equals sign ("=")
     * followed by the value. Adjacent mappings are separated by the characters ", " (comma and space).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * System.out.println(map);   // {a=1, b=2}
     * }</pre>
     *
     * @return a string representation of this map.
     */
    @Override
    public String toString() {
        return valueMap.toString();
    }
}
