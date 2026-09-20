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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An abstract, package-private base class for read-only {@link Map} implementations.
 * All mutating operations ({@code put}, {@code remove}, {@code clear}, etc.) throw
 * {@link UnsupportedOperationException}. Concrete factories that defensively copy their input produce
 * immutable, thread-safe instances. Factories that wrap an externally owned backing map produce a read-only
 * view instead; external changes are reflected by the view and require the backing map's synchronization.
 *
 * <p>This class provides the shared infrastructure used by concrete subclasses such as
 * {@link ImmutableMap}, {@link ImmutableSortedMap}, {@link ImmutableNavigableMap} and
 * {@link ImmutableBiMap}. It is not intended to be instantiated directly; use the factory
 * methods on those concrete subclasses (e.g. {@link ImmutableMap#of(Object, Object)} or
 * {@link ImmutableMap#copyOf(Map)}) instead.</p>
 *
 * <p>The iteration order of entries is determined by the underlying map: for example,
 * a {@link java.util.LinkedHashMap} or {@link java.util.SortedMap} preserves its insertion
 * or sorted order, while a {@link java.util.HashMap} provides no specific order.</p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Map
 * @see Immutable
 * @see ImmutableMap
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings("java:S2160")
abstract class AbstractImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable {

    /**
     * The read-only view through which all lookups and collection views ({@code keySet()},
     * {@code values()}, {@code entrySet()}) are served, as well as {@code equals}/{@code hashCode}/
     * {@code toString} - {@link Collections#unmodifiableMap(Map)} forwards all three to the map it wraps,
     * so this single field is enough to compare equal to the mappings held. It is the supplied backing map
     * itself only when that map was already declared unmodifiable at construction time.
     */
    final Map<K, V> map;

    /**
     * Whether this instance exclusively owns {@link #map}'s backing map, i.e. no other reference through
     * which the mappings could still change escapes to a caller. Only an owning instance is a stable
     * value; a non-owning one is a read-only <i>view</i> whose contents can change underneath it.
     *
     * <p>This models the backing map's <i>stability</i>, not the factory that produced this instance:
     * {@code copyOf} may safely return an owning instance unchanged, derived views such as
     * {@link ImmutableSortedMap#subMap(Object, Object)} inherit their parent's ownership, and anything
     * built over caller-supplied storage ({@code wrap} or {@link ImmutableMap#builder(Map)}) is never owning.
     * A consumed no-argument {@link ImmutableMap#builder()} transfers its private storage to its result. It is deliberately
     * conservative - a false {@code false} only costs a redundant copy, whereas a false {@code true} would
     * hand out a value that can change.</p>
     */
    final boolean ownsBacking;

    /**
     * Constructs a non-owning read-only map over the given backing map, always wrapping it in an
     * unmodifiable view.
     *
     * @param map the backing map holding the mappings of this map
     * @throws NullPointerException if {@code map} is {@code null}
     */
    AbstractImmutableMap(final Map<? extends K, ? extends V> map) throws NullPointerException {
        // A class name is not a reliable immutability contract. Always create an unmodifiable
        // view here so that entrySet() entries and all collection views are read-only.
        this(map, false, false);
    }

    /**
     * Constructs a read-only map over the given backing map.
     *
     * <p>There is deliberately no two-argument {@code (Map, boolean)} form here or on {@link ImmutableMap}.
     * {@link ImmutableSortedMap}, {@link ImmutableNavigableMap} and {@link ImmutableBiMap} each declare one
     * whose flag is {@code ownsBacking}, so a two-argument {@code super(...)} call from any of them would
     * once have bound to a {@code (Map, boolean isUnmodifiable)} overload up here and silently skipped the
     * unmodifiable wrapper while dropping the ownership flag. Subclasses must pass all three arguments.</p>
     *
     * @param map the backing map holding the mappings of this map
     * @param isUnmodifiable {@code true} if {@code map} is already unmodifiable and therefore does not
     *        need to be wrapped in an additional unmodifiable view
     * @param ownsBacking {@code true} only if no other modifiable reference to {@code map} survives this
     *        call; see {@link #ownsBacking}
     * @throws NullPointerException if {@code map} is {@code null} and {@code isUnmodifiable} is false
     */
    AbstractImmutableMap(final Map<? extends K, ? extends V> map, final boolean isUnmodifiable, final boolean ownsBacking) throws NullPointerException {
        // to create immutable keySet(), values(), entrySet()
        this.map = isUnmodifiable ? (Map<K, V>) map : Collections.unmodifiableMap(map);
        this.ownsBacking = ownsBacking;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code defaultValue} if this map
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
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains no mapping for the key
     * @return the value to which the specified key is mapped, or {@code defaultValue} if this
     *         map contains no mapping for the key
     * @throws NullPointerException if {@code key} is {@code null} and the backing map rejects null-key queries
     * @throws ClassCastException if {@code key} has a type that the backing map cannot query or compare
     * @see java.util.Map#getOrDefault(Object, Object)
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) throws NullPointerException, ClassCastException {
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param k ignored.
     * @param v ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V put(final K k, final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param o ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V remove(final Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param map ignored.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final void putAll(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V putIfAbsent(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final boolean remove(final Object key, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param oldValue ignored.
     * @param newValue ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final boolean replace(final K key, final V oldValue, final V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param value ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V replace(final K key, final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param mappingFunction ignored, and may be {@code null}.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param remappingFunction ignored, and may be {@code null}.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param remappingFunction ignored, and may be {@code null}.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param key ignored.
     * @param value ignored.
     * @param remappingFunction ignored, and may be {@code null}.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @param function ignored, and may be {@code null}.
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
     */
    @Deprecated
    @Override
    public final void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) throws UnsupportedOperationException {
        // Without this override the inherited Map.replaceAll default iterates entrySet()/setValue(); on an EMPTY
        // immutable map it would silently no-op instead of throwing, inconsistent with every other mutator here
        // (and with ImmutableList.replaceAll). Block it unconditionally.
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by this immutable map.
     * Attempting to call this method will always throw an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException always.
     * @deprecated this immutable map does not support modification operations.
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
     * More formally, returns {@code true} if and only if this map contains a mapping for a key {@code k}
     * such that {@code (key == null ? k == null : key.equals(k))}.
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
     * @throws NullPointerException if {@code key} is {@code null} and the backing map rejects null-key queries
     * @throws ClassCastException if {@code key} has a type that the backing map cannot query or compare
     * @see java.util.Map#containsKey(Object)
     */
    @Override
    public boolean containsKey(final Object key) throws NullPointerException, ClassCastException {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     * More formally, returns {@code true} if and only if this map contains at least one mapping
     * to a value {@code v} such that {@code (value == null ? v == null : value.equals(v))}.
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
     * @throws NullPointerException if {@code value} is {@code null} and the backing map does not permit
     *         {@code null} values (optional)
     * @throws ClassCastException if {@code value} has a type that the backing map cannot compare (optional)
     * @see java.util.Map#containsValue(Object)
     */
    @Override
    public boolean containsValue(final Object value) throws NullPointerException, ClassCastException {
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
     * @throws NullPointerException if {@code key} is {@code null} and the backing map rejects null-key queries
     * @throws ClassCastException if {@code key} has a type that the backing map cannot query or compare
     * @see java.util.Map#get(Object)
     */
    @Override
    public V get(final Object key) throws NullPointerException, ClassCastException {
        return map.get(key);
    }

    /**
     * Returns an unmodifiable {@link Set} view of the keys contained in this map.
     * Changes made directly to an externally owned backing map are reflected in the returned view.
     * Attempts to modify the returned set will result in an {@link UnsupportedOperationException}.
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
     * Returns an unmodifiable {@link Collection} view of the values contained in this map.
     * Changes made directly to an externally owned backing map are reflected in the returned view.
     * Attempts to modify the returned collection will result in an {@link UnsupportedOperationException}.
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
     * Returns an unmodifiable {@link Set} view of the mappings contained in this map.
     * Changes made directly to an externally owned backing map are reflected in the returned view.
     * Each element in the returned set is an immutable {@link Map.Entry}.
     * Attempts to modify the returned set or its entries will result in an {@link UnsupportedOperationException}.
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
     * If the map contains more than {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
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
     * Performs the given action for each entry in this map until all entries have been processed or the
     * action throws an exception. Delegates to the backing map so that its optimized traversal is used
     * instead of the {@link Map#forEach(BiConsumer)} default's entry-set iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * map.forEach((k, v) -> System.out.println(k + "=" + v));   // prints a=1 then b=2
     * }</pre>
     *
     * @param action the action to be performed for each entry
     * @throws NullPointerException if {@code action} is {@code null}
     * @see java.util.Map#forEach(BiConsumer)
     */
    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) throws NullPointerException {
        Objects.requireNonNull(action);

        map.forEach(action);
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a map and the two maps represent the same mappings.
     * More formally, two maps {@code m1} and {@code m2} are equal if {@code m1.entrySet().equals(m2.entrySet())}.
     * This ensures that the {@code equals} method works properly across different implementations of the {@link Map} interface.
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

        if (obj instanceof AbstractImmutableMap im) {
            return map.equals(im.map);
        }

        return map.equals(obj);
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
        return map.hashCode();
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
     * <p>Direct self-references are rendered with the standard collection/map marker; indirect cycles are not detected.</p>
     *
     * @return a string representation of this map.
     */
    @Override
    public String toString() {
        // Format through this wrapper so direct self-references use the standard marker.
        return super.toString();
    }
}
