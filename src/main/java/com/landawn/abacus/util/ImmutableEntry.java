/*
 * Copyright (C) 2017 HaiYang Li
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

import java.io.Serial;
import java.util.AbstractMap;
import java.util.Map;

/**
 * An immutable implementation of {@link Map.Entry} that represents a key-value pair
 * that cannot be modified after creation.
 * 
 * <p>This class extends {@link AbstractMap.SimpleImmutableEntry} and provides additional
 * factory methods for creating immutable entries. Once created, neither the key nor the
 * value can be changed. Attempts to call {@link #setValue(Object)} will throw
 * {@link UnsupportedOperationException}.
 * 
 * <p>ImmutableEntry is useful when you need to:
 * <ul>
 *   <li>Return entries from methods without worrying about external modification</li>
 *   <li>Store entries in collections while ensuring they remain unchanged</li>
 *   <li>Create temporary key-value pairs for processing</li>
 * </ul>
 * 
 * <p>This class is serializable if both the key and value are serializable.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an immutable entry
 * ImmutableEntry<String, Integer> entry = ImmutableEntry.of("count", 42);
 * 
 * // Use in a collection
 * List<ImmutableEntry<String, Integer>> entries = Arrays.asList(
 *     ImmutableEntry.of("one", 1),
 *     ImmutableEntry.of("two", 2),
 *     ImmutableEntry.of("three", 3)
 * );
 * 
 * // Copy from existing entry
 * Map.Entry<String, Integer> mutable = new AbstractMap.SimpleEntry<>("key", 10);
 * ImmutableEntry<String, Integer> immutable = ImmutableEntry.copyOf(mutable);
 * }</pre>
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 * @see Map.Entry
 * @see AbstractMap.SimpleImmutableEntry
 * @see Immutable
 */
@com.landawn.abacus.annotation.Immutable
public final class ImmutableEntry<K, V> extends AbstractMap.SimpleImmutableEntry<K, V> implements Immutable {

    @Serial
    private static final long serialVersionUID = -7667037689002186862L;

    ImmutableEntry(final K key, final V value) {
        super(key, value);
    }

    ImmutableEntry(final Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }

    /**
     * Creates an ImmutableEntry with the specified key and value.
     * Both the key and value may be null.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableEntry<String, Integer> entry = ImmutableEntry.of("age", 25);
     * System.out.println(entry.getKey());   // prints: age
     * System.out.println(entry.getValue()); // prints: 25
     * 
     * // Null values are allowed
     * ImmutableEntry<String, String> nullEntry = ImmutableEntry.of("missing", null);
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param key the key for this entry, may be null
     * @param value the value for this entry, may be null
     * @return a new ImmutableEntry containing the specified key-value pair
     */
    public static <K, V> ImmutableEntry<K, V> of(final K key, final V value) {
        return new ImmutableEntry<>(key, value);
    }

    /**
     * Creates an ImmutableEntry by copying the key and value from an existing Map.Entry.
     * This method creates a defensive copy, so changes to the original entry after
     * copying will not affect the created ImmutableEntry.
     * 
     * <p>If the provided entry contains null key or value, the ImmutableEntry will
     * also contain null for those fields.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("count", 100);
     * 
     * Map.Entry<String, Integer> mutableEntry = map.entrySet().iterator().next();
     * ImmutableEntry<String, Integer> immutableCopy = ImmutableEntry.copyOf(mutableEntry);
     * 
     * // Original entry can be modified (if supported), but copy remains unchanged
     * map.put("count", 200);
     * System.out.println(immutableCopy.getValue()); // still prints: 100
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param entry the entry to copy, must not be null
     * @return a new ImmutableEntry with the same key and value as the provided entry
     */
    public static <K, V> ImmutableEntry<K, V> copyOf(final Map.Entry<? extends K, ? extends V> entry) {
        return new ImmutableEntry<>(entry.getKey(), entry.getValue());
    }

    /**
     * This operation is not supported by ImmutableEntry.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     * 
     * <p>The immutability of this entry ensures that once created, the key-value
     * pair remains constant throughout its lifetime.
     *
     * @param v the new value to be stored (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this entry is immutable
     * @deprecated ImmutableEntry does not support value modification
     */
    @Deprecated
    @Override
    public V setValue(final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}