/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Internal;

/**
 * A bidirectional map that preserves the uniqueness of both keys and values, enabling efficient
 * forward and reverse lookups. This final class maintains two underlying maps to provide efficient
 * access in both directions while enforcing bijective constraints that ensure each key maps to
 * exactly one value and each value maps to exactly one key. Operation complexity follows that of
 * the supplied backing maps; the default {@link HashMap} backing provides constant-time average lookup.
 *
 * <p>BiMap extends the traditional Map interface with additional operations for value-based
 * lookups and inverse mapping functionality. The bijective constraint means that both keys
 * and values must be unique across the entire map, making BiMap ideal for scenarios requiring
 * two-way associations such as identifier mappings, code-name relationships, and reversible
 * transformations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Bidirectional Access:</b> Lookup by key or value using separate forward and reverse maps</li>
 *   <li><b>Bijective Constraint:</b> Enforces uniqueness of both keys and values</li>
 *   <li><b>Inverse View:</b> Provides reversed BiMap with swapped keys and values</li>
 *   <li><b>Flexible Construction:</b> Multiple constructors and factory methods for different use cases</li>
 *   <li><b>Force Operations:</b> Override uniqueness constraints when necessary</li>
 *   <li><b>Map Compatibility:</b> Implements {@link Map}; collection views are deliberately read-only</li>
 *   <li><b>Immutable Views:</b> Key, value, and entry sets as immutable collections</li>
 *   <li><b>Builder Pattern:</b> Fluent construction with validation and error handling</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Identifier Mapping:</b> Database ID to entity name associations</li>
 *   <li><b>Code Translation:</b> Error codes to human-readable messages</li>
 *   <li><b>Protocol Mapping:</b> Network protocol constants and string representations</li>
 *   <li><b>Language Translation:</b> Bidirectional language code mappings</li>
 *   <li><b>Enum Mapping:</b> Enum values to external representations</li>
 *   <li><b>Configuration Management:</b> Property keys to values with reverse lookup needs</li>
 *   <li><b>Data Transformation:</b> Reversible data format conversions</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic BiMap creation and operations
 * BiMap<String, Integer> userIdMap = BiMap.of(
 *     "alice", 1001,
 *     "bob", 1002,
 *     "charlie", 1003
 * );
 *
 * // Forward lookup (key to value)
 * Integer aliceId = userIdMap.get("alice");   // returns 1001
 *
 * // Reverse lookup (value to key)
 * String userName = userIdMap.getByValue(1002);   // returns "bob"
 *
 * // Inverse view with swapped keys and values
 * BiMap<Integer, String> idUserMap = userIdMap.inverse();
 * String user = idUserMap.get(1003);   // returns "charlie"
 *
 * // Bijective constraint enforcement
 * // userIdMap.put("david", 1001);   // Would throw IllegalArgumentException: value 1001 already exists
 * userIdMap.forcePut("david", 1001);   // forces mapping, removes "alice" -> 1001
 * userIdMap.forcePut("eve", 1002);     // forces mapping, removes conflicting entries
 *
 * // Builder pattern for complex construction
 * BiMap<String, String> countryMap = BiMap.<String, String>builder()
 *     .put("US", "United States")
 *     .put("UK", "United Kingdom")
 *     .put("DE", "Germany")
 *     .build();
 *
 * // Custom underlying map types
 * BiMap<String, Integer> linkedMap = new BiMap<>(
 *     LinkedHashMap::new,
 *     LinkedHashMap::new
 * );
 * }</pre>
 *
 * <p><b>Bijective Constraint Details:</b>
 * <ul>
 *   <li><b>Key Uniqueness:</b> Standard Map behavior - each key maps to at most one value</li>
 *   <li><b>Value Uniqueness:</b> BiMap constraint - each value maps to at most one key</li>
 *   <li><b>Constraint Violation:</b> {@code put()} throws {@link IllegalArgumentException} when the value is already mapped to a different key</li>
 *   <li><b>Force Operations:</b> {@code forcePut()} silently removes any conflicting mapping for the value</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #of(Object, Object)} - Single key-value pair</li>
 *   <li>{@link #of(Object, Object, Object, Object)} - Two key-value pairs (additional arity-overloads accept up to 10 pairs)</li>
 *   <li>{@link #copyOf(Map)} - Create from existing Map with validation</li>
 *   <li>{@link #builder()} - Start builder pattern construction</li>
 *   <li>{@link #builder(Map)} - Builder initialized with existing Map</li>
 * </ul>
 *
 * <p><b>Constructor Options:</b>
 * <ul>
 *   <li>{@link #BiMap()} - Default HashMap-backed BiMap</li>
 *   <li>{@link #BiMap(int)} - Specify initial capacity</li>
 *   <li>{@link #BiMap(int, float)} - Specify capacity and load factor</li>
 *   <li>{@link #BiMap(Class, Class)} - Custom map implementation types</li>
 *   <li>{@link #BiMap(Supplier, Supplier)} - Custom map suppliers for flexibility</li>
 * </ul>
 *
 * <p><b>Bidirectional Operations:</b>
 * <ul>
 *   <li><b>Forward Lookup:</b> {@code get(key)} - Standard Map operation</li>
 *   <li><b>Reverse Lookup:</b> {@code getByValue(value)} - Value-to-key lookup</li>
 *   <li><b>Safe Reverse Lookup:</b> {@code getByValueOrDefault(value, defaultValue)}</li>
 *   <li><b>Reverse Removal:</b> {@code removeByValue(value)} - Remove by value</li>
 *   <li><b>Inverse View:</b> {@code inverse()} - Swapped key-value BiMap</li>
 * </ul>
 *
 * <p><b>Advanced Operations:</b>
 * <ul>
 *   <li>{@link #forcePut(Object, Object)} - Override bijective constraint</li>
 *   <li>{@link #copy()} - Create independent copy with same data</li>
 *   <li>{@link #inverse()} - Get inverse view (cached for efficiency)</li>
 * </ul>
 *
 * <p><b>Collection Views:</b>
 * All three collection views are live views backed by the BiMap that cannot be modified through the view,
 * so bijective integrity is maintained. All three iterate the forward (key-to-value) backing map, so their
 * iteration orders correspond entry for entry, as for any other {@link Map}:
 * <ul>
 *   <li>{@code keySet()} - Returns {@link ImmutableSet} of keys</li>
 *   <li>{@code values()} - Returns {@link ImmutableSet} of values (a {@code Set}, not a {@code Collection},
 *       because values are unique)</li>
 *   <li>{@code entrySet()} - Returns {@link ImmutableSet} of entries. The set is live, but the entries it
 *       yields are {@link ImmutableEntry} snapshots: {@code setValue} throws
 *       {@link UnsupportedOperationException}, and an entry obtained before a change keeps its old value.</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Forward lookup: delegates to the key-to-value backing map</li>
 *   <li>Reverse lookup: delegates to the value-to-key backing map</li>
 *   <li>Put operations: follow the complexity of both backing maps</li>
 *   <li>Space complexity: O(n), with each mapping represented in both backing maps</li>
 *   <li>Inverse view creation: O(1) - cached after first access</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * BiMap instances are <b>not thread-safe</b>:
 * <ul>
 *   <li>Concurrent modifications require external synchronization</li>
 *   <li>Multiple readers can access safely if no writers are present</li>
 *   <li>Inverse views share underlying data — synchronize on the original BiMap for both sides</li>
 *   <li>Do not wrap a BiMap with {@code Collections.synchronizedMap}: that only covers the
 *       {@link Map} surface and cannot keep the dual key/value maps and inverse view consistent</li>
 * </ul>
 *
 * <p><b>Backing-map equality:</b>
 * Both backing maps must use an equality that is <i>consistent with {@code equals}</i>, the same requirement
 * {@link java.util.SortedMap} states for itself. A {@code BiMap} answers key questions from the forward map and
 * value questions ({@link #containsValue}, {@link #getByValue}, {@link #removeByValue}, {@link #values()}'s
 * {@code contains}, and {@code put}'s uniqueness check) from the reverse map, so a backing map whose equivalence
 * is coarser or finer than {@code equals} - a {@link java.util.TreeMap} with a case-insensitive or otherwise
 * non-{@code equals}-consistent {@link Comparator}, or an {@link java.util.IdentityHashMap} - makes this
 * {@code BiMap} violate the {@link Map} contract. For example, a case-insensitively ordered reverse map answers
 * {@code containsValue("abc")} with {@code true} for a stored value of {@code "ABC"}, although no value in the
 * map {@code equals} {@code "abc"}, while {@link #containsEntry(Object, Object)} and {@link #entrySet()}'s
 * {@code contains} - which compare with {@code equals} - answer {@code false}. Such a configuration is outside
 * this class's contract.
 *
 * <p><b>Null Handling:</b>
 * <ul>
 *   <li>A BiMap never <i>stores</i> a {@code null} key or value: every insertion path
 *       ({@link #put}, {@link #forcePut}, {@link #putIfAbsent}, {@link #putAll}, {@link #forcePutAll} and
 *       the {@link Map} default remapping methods) rejects one with {@code IllegalArgumentException}.</li>
 *   <li>The lookup and removal methods ({@link #get}, {@link #getByValue}, {@link #containsKey},
 *       {@link #containsValue}, {@link #remove}, {@link #removeByValue}) simply pass {@code null} to the
 *       backing maps: with the default {@link HashMap} backing they return {@code null}/{@code false},
 *       while a backing map that rejects {@code null} keys (such as {@link java.util.TreeMap}) throws
 *       {@link NullPointerException}.</li>
 *   <li>{@link Map#merge(Object, Object, java.util.function.BiFunction)} throws
 *       {@link NullPointerException} for a {@code null} value, as its own contract requires.</li>
 *   <li>A BiMap's values are the keys of its reverse map, so a stored value must not be mutated in a way
 *       that changes its equality, {@code hashCode} or ordering while it is in the map, exactly as for a
 *       {@link HashMap} key, and a supplied backing map must not drop entries of its own accord (a
 *       {@code LinkedHashMap} that overrides {@code removeEldestEntry}, for example). Such an entry
 *       becomes unreachable through the reverse map, so {@link #getByValue} can no longer find it and the
 *       forward and inverse views can report different sizes. When the old reverse lookup misses,
 *       overwriting a surviving forward key with {@link #put} or {@link #forcePut} attempts to drop its
 *       stale reverse entry and install the replacement. This is best-effort recovery, not support for
 *       mutable map keys or self-evicting backing maps. A bare {@link #remove} cannot find an unreachable
 *       reverse entry by value; {@link #clear}, if both backing maps accept it, empties both directions.</li>
 * </ul>
 *
 * <p><b>Error Conditions:</b>
 * <ul>
 *   <li><b>Duplicate Values:</b> {@code put()} throws {@link IllegalArgumentException} if the value is already mapped to a different key (use {@link #forcePut} to override)</li>
 *   <li><b>Builder Validation:</b> Builder throws {@code IllegalArgumentException} for duplicates</li>
 *   <li><b>Null Arguments:</b> Factory methods validate {@code non-null} arguments</li>
 *   <li><b>Backing-map write failures:</b> a mutator writes the forward and reverse maps in sequence. If a
 *       user-supplied backing map rejects a write - a comparator-backed {@link java.util.TreeMap} that
 *       cannot order a value, for example - {@link #put} and {@link #forcePut} attempt to restore every
 *       affected mapping, including a write that changes its mapping before throwing. If the map was
 *       consistent before the call and the restoring operations succeed, the original key and value
 *       objects remain present when the exception propagates. The undo re-inserts
 *       rather than rewinds, so an insertion-ordered backing map can iterate the restored entries in a
 *       different order than before; and a backing map that rejects the restoring writes as well leaves the
 *       undo incomplete - each such failure is reported on the propagating exception through
 *       {@link Throwable#addSuppressed} - so even {@link #put} and {@link #forcePut} can then leave
 *       {@code size()} and {@code inverse().size()} disagreeing. {@link #remove}, {@link #removeByValue},
 *       {@link #clear} and {@link #replaceAll} do not undo at all, so the two backing maps can be left out
 *       of step and {@code size()} and {@code inverse().size()} can disagree from then on. Re-binding the
 *       affected key to its value with {@link #forcePut} can repair an orphaned binding; a plain
 *       {@link #put} may reject it as a duplicate value. A successful {@link #clear} empties both directions.
 *       Recovery cannot guarantee consistency if a backing map keeps rejecting operations, mutates
 *       unrelated entries, or otherwise violates its {@link Map} contract.</li>
 * </ul>
 *
 * <p><b>Value uniqueness applies to every insertion, including the inherited {@link Map} defaults:</b>
 * {@link Map#replace(Object, Object)}, {@link Map#replace(Object, Object, Object)},
 * {@link Map#computeIfAbsent(Object, java.util.function.Function)},
 * {@link Map#computeIfPresent(Object, java.util.function.BiFunction)},
 * {@link Map#compute(Object, java.util.function.BiFunction)} and
 * {@link Map#merge(Object, Object, java.util.function.BiFunction)} all store through {@link #put}, so each
 * of them throws {@link IllegalArgumentException} when the value it would store is already bound to a
 * different key. This is permitted by the {@code Map} contract - each of those methods declares
 * {@code IllegalArgumentException} "if some property of the specified key or value prevents it from being
 * stored" - and value uniqueness is exactly such a property. Use {@link #forcePut} to displace the
 * conflicting entry instead.
 *
 * <p><b>Inverse View Behavior:</b>
 * <ul>
 *   <li>Inverse view is a live view - reflects changes in original BiMap</li>
 *   <li>Modifications to inverse view affect the original BiMap</li>
 *   <li>Inverse of inverse returns the original BiMap (not a new instance)</li>
 *   <li>Inverse view is cached for performance - created only once</li>
 * </ul>
 *
 * <p><b>Builder Pattern Features:</b>
 * <ul>
 *   <li>{@code put(key, value)} - Add entry with duplicate validation</li>
 *   <li>{@code forcePut(key, value)} - Add entry overriding conflicts</li>
 *   <li>{@code putAll(map)} - Bulk addition with validation</li>
 *   <li>{@code build()} - Return the constructed (mutable) BiMap</li>
 * </ul>
 *
 * <p><b>Integration Points:</b>
 * <ul>
 *   <li><b>{@link Map}:</b> Standard lookup and mutation operations, with read-only collection views</li>
 *   <li><b>{@link ImmutableSet}:</b> Immutable collection views</li>
 *   <li><b>{@link HashMap}:</b> Default underlying implementation</li>
 *   <li><b>Collections Framework:</b> Standard iteration and stream support</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate underlying map types based on ordering requirements</li>
 *   <li>Consider using Builder pattern for fluent construction with validation</li>
 *   <li>Cache inverted views when frequent reverse lookups are needed</li>
 *   <li>Use {@code forcePut()} when conflict resolution behavior is clear</li>
 *   <li>Prefer independent copies via {@code copy()} for sharing between components</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li>BiMap maintains two complete maps - consider memory implications</li>
 *   <li>Inverse views share underlying data and require only constant additional object overhead</li>
 *   <li>Use {@code clear()} to release all mappings and enable garbage collection</li>
 *   <li>Consider capacity and load factor for large datasets</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs Two separate Maps:</b> Automatic consistency and inverted view convenience</li>
 *   <li><b>vs Google Guava BiMap:</b> Similar API with builder pattern and force operations</li>
 *   <li><b>vs Apache Commons BidiMap:</b> Type-safe generics and modern Java features</li>
 * </ul>
 *
 * <p><b>{@code Map} vs. {@code BiMap} vs. {@code Multimap} vs. {@code Multiset}:</b> these model different
 * key/value relationships — pick by how many (and what kind of) values a key holds:</p>
 * <table border="1">
 *   <caption>Choosing between Map, BiMap, Multimap, and Multiset</caption>
 *   <tr>
 *     <th>Type</th>
 *     <th>Models</th>
 *     <th>Example contents</th>
 *     <th>Use when</th>
 *   </tr>
 *   <tr>
 *     <td>{@link java.util.Map} (helpers in {@link Maps})</td>
 *     <td>one key &rarr; one value</td>
 *     <td>{@code {a=1, b=2}}</td>
 *     <td>each key has exactly one value</td>
 *   </tr>
 *   <tr>
 *     <td>{@code BiMap}</td>
 *     <td>one key &harr; one value (both sides unique; invertible)</td>
 *     <td>{@code {a=1, b=2}} with inverse {@code {1=a, 2=b}}</td>
 *     <td>you must look up by value as well as by key, and values are unique</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Multimap} ({@link ListMultimap} / {@link SetMultimap})</td>
 *     <td>one key &rarr; many values</td>
 *     <td>{@code {a=[1, 2], b=[3]}}</td>
 *     <td>a key may hold several values (avoids hand-rolling {@code Map<K, List<V>>})</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Multiset}</td>
 *     <td>element &rarr; occurrence count</td>
 *     <td>{@code {a x 2, b x 1}}</td>
 *     <td>counting occurrences / frequencies (a {@code Map<E, Integer>} done right)</td>
 *   </tr>
 * </table>
 *
 * @param <K> the type of keys maintained by this BiMap
 * @param <V> the type of mapped values
 *
 * @see Map
 * @see ImmutableSet
 * @see HashMap
 * @see Builder
 * @see java.util.Collections#synchronizedMap(Map)
 */
public final class BiMap<K, V> implements Map<K, V> {
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /** Supplier used by {@link #copy()} and {@link #inverse()} to recreate an empty key-to-value backing map of the same kind. */
    final Supplier<? extends Map<K, V>> keyMapSupplier;

    /** Supplier used by {@link #copy()} and {@link #inverse()} to recreate an empty value-to-key backing map of the same kind. */
    final Supplier<? extends Map<V, K>> valueMapSupplier;

    /** The forward backing map holding the key-to-value mappings. */
    final Map<K, V> keyMap;

    /** The reverse backing map holding the value-to-key mappings; always the exact inverse of {@link #keyMap}. */
    final Map<V, K> valueMap;

    private transient BiMap<V, K> invertedView; //NOSONAR

    // The three collection views are stateless wrappers over the live backing maps, so - as in the JDK's
    // own map implementations - one instance each is created lazily and reused.
    private transient ImmutableSet<K> keySet; //NOSONAR

    private transient ImmutableSet<V> values; //NOSONAR

    private transient ImmutableSet<Map.Entry<K, V>> entrySet; //NOSONAR

    /**
     * Constructs a BiMap with the default initial capacity.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * }</pre>
     *
     */
    public BiMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a BiMap with the specified initial capacity.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(100);
     * }</pre>
     *
     * @param initialCapacity the initial capacity of the BiMap
     * @throws IllegalArgumentException if {@code initialCapacity} is negative.
     */
    public BiMap(final int initialCapacity) throws IllegalArgumentException {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a BiMap with the specified initial capacity and load factor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(100, 0.9f);
     * }</pre>
     *
     * @param initialCapacity the initial capacity of the BiMap
     * @param loadFactor the load factor of the BiMap
     * @throws IllegalArgumentException if {@code initialCapacity} is negative, or if {@code loadFactor} is not
     *         positive or is {@link Float#NaN}.
     */
    public BiMap(final int initialCapacity, final float loadFactor) throws IllegalArgumentException {
        this(new HashMap<>(initialCapacity, loadFactor), new HashMap<>(initialCapacity, loadFactor));
    }

    /**
     * Converts an expected entry count into an initial capacity for the {@code of(...)} factories.
     *
     * <p>{@link #BiMap(int)} takes a <i>capacity</i>, so handing it the number of key-value pairs made
     * {@code of(...)} resize its backing maps while it was still filling them for every arity above one.</p>
     *
     * @param pairCount the number of key-value pairs the factory will insert
     * @return the initial capacity that holds them without a resize
     */
    private static int capacityFor(final int pairCount) {
        return (int) (pairCount / DEFAULT_LOAD_FACTOR) + 1;
    }

    /**
     * Constructs a BiMap with the specified types of maps for keys and values.
     * This constructor allows the user to specify the types of the underlying maps used to store keys and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(LinkedHashMap.class, TreeMap.class);
     * // Uses LinkedHashMap for keys and TreeMap for values
     * }</pre>
     *
     * @param keyMapType the Class object representing the type of Map to be used for storing keys; must not be {@code null}
     * @param valueMapType the Class object representing the type of Map to be used for storing values; must not be {@code null}
     * @throws IllegalArgumentException if either map type is {@code null} or has no supported construction path, or the resulting suppliers return null, non-empty, or identical map instances
     */
    @SuppressWarnings("rawtypes")
    public BiMap(final Class<? extends Map> keyMapType, final Class<? extends Map> valueMapType) throws IllegalArgumentException {
        this(Suppliers.ofMap(keyMapType), Suppliers.ofMap(valueMapType));
    }

    /**
     * Constructs a BiMap with the specified suppliers for key and value maps.
     * This constructor allows the user to specify the suppliers of the underlying maps used to store keys and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, TreeMap::new);
     * // Uses custom suppliers for both maps
     * }</pre>
     *
     * @param keyMapSupplier the supplier of the empty map used for key-to-value mappings; must not be {@code null}
     * @param valueMapSupplier the supplier of the empty map used for value-to-key mappings; must not be {@code null}
     * @throws IllegalArgumentException if {@code keyMapSupplier} or {@code valueMapSupplier} is {@code null}, or if
     *         a map returned by either supplier is {@code null}, or if a returned map is nonempty or both suppliers
     *         return the same map instance.
     */
    public BiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapSupplier, cs.keyMapSupplier);
        N.checkArgNotNull(valueMapSupplier, cs.valueMapSupplier);

        final Map<K, V> suppliedKeyMap = N.checkArgNotNull(keyMapSupplier.get(), "keyMapSupplier.get()");
        final Map<V, K> suppliedValueMap = N.checkArgNotNull(valueMapSupplier.get(), "valueMapSupplier.get()");

        if (suppliedKeyMap == suppliedValueMap) {
            throw new IllegalArgumentException("The suppliers must return distinct map instances");
        }

        if (!suppliedKeyMap.isEmpty() || !suppliedValueMap.isEmpty()) {
            throw new IllegalArgumentException("The supplied maps must be empty");
        }

        this.keyMapSupplier = keyMapSupplier;
        this.valueMapSupplier = valueMapSupplier;
        keyMap = suppliedKeyMap;
        valueMap = suppliedValueMap;
    }

    /**
     * Constructs a BiMap with the specified key and value maps.
     * This constructor allows the user to directly provide the underlying maps used to store keys and values.
     *
     * @param keyMap the Map to be used for storing keys.
     * @param valueMap the Map to be used for storing values.
     */
    @Internal
    BiMap(final Map<K, V> keyMap, final Map<V, K> valueMap) {
        keyMapSupplier = Suppliers.ofMap(keyMap.getClass());
        valueMapSupplier = Suppliers.ofMap(valueMap.getClass());
        this.keyMap = keyMap;
        this.valueMap = valueMap;
    }

    /**
     * Constructs a BiMap with the specified backing maps and explicit suppliers.
     * Used when class-derived suppliers would lose state (e.g. a TreeMap's custom comparator).
     *
     * @param keyMapSupplier the Supplier used by copy()/inverse() to recreate the key map.
     * @param valueMapSupplier the Supplier used by copy()/inverse() to recreate the value map.
     * @param keyMap the Map to be used for storing keys.
     * @param valueMap the Map to be used for storing values.
     */
    @Internal
    BiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier, final Map<K, V> keyMap,
            final Map<V, K> valueMap) {
        this.keyMapSupplier = keyMapSupplier;
        this.valueMapSupplier = valueMapSupplier;
        this.keyMap = keyMap;
        this.valueMap = valueMap;
    }

    /**
     * Constructs a BiMap with the specified key and value maps, and an inverted BiMap.
     * This constructor allows the user to provide the underlying maps used to store keys and values, as well as an inverted BiMap.
     *
     * @param keyMap the Map to be used for storing keys.
     * @param valueMap the Map to be used for storing values.
     * @param inverted the inverted BiMap containing the same entries as this BiMap but with reversed keys and values.
     */
    @Internal
    BiMap(final Map<K, V> keyMap, final Map<V, K> valueMap, BiMap<V, K> inverted) {
        // Reuse the inverted BiMap's suppliers (swapped) instead of re-deriving them from the runtime map
        // classes: Suppliers.ofMap can't instantiate wrapper classes (e.g. Collections.synchronizedMap) and
        // would drop a custom Comparator from a supplier-built TreeMap.
        keyMapSupplier = inverted.valueMapSupplier;
        valueMapSupplier = inverted.keyMapSupplier;
        this.keyMap = keyMap;
        this.valueMap = valueMap;
        this.invertedView = inverted;
    }

    /**
     * Creates a new BiMap with a single key-value pair.
     * This method provides a convenient way to create a BiMap with one entry.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <V> the type of the value.
     * @param k1 the key to be inserted into the BiMap.
     * @param v1 the value to be associated with the key in the BiMap.
     * @return a BiMap containing the specified key-value pair.
     * @throws IllegalArgumentException if {@code k1} or {@code v1} is {@code null}.
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(1));

        map.put(k1, v1);

        return map;
    }

    /**
     * Creates a new BiMap with two key-value pairs.
     * This method provides a convenient way to create a BiMap with two entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(2));

        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Creates a new BiMap with three key-value pairs.
     * This method provides a convenient way to create a BiMap with three entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2, "three", 3);
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(3));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Creates a new BiMap with four key-value pairs.
     * This method provides a convenient way to create a BiMap with four entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4);
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4)
            throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(4));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return map;
    }

    /**
     * Creates a new BiMap with five key-value pairs.
     * This method provides a convenient way to create a BiMap with five entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5)
            throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(5));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);

        return map;
    }

    /**
     * Creates a new BiMap with six key-value pairs.
     * This method provides a convenient way to create a BiMap with six entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of(
     *     "one", 1, "two", 2, "three", 3,
     *     "four", 4, "five", 5, "six", 6
     * );
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @param k6 the sixth key to be inserted into the BiMap.
     * @param v6 the value to be associated with the sixth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(6));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);

        return map;
    }

    /**
     * Creates a new BiMap with seven key-value pairs.
     * This method provides a convenient way to create a BiMap with seven entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of(
     *     "one", 1, "two", 2, "three", 3,
     *     "four", 4, "five", 5, "six", 6,
     *     "seven", 7
     * );
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @param k6 the sixth key to be inserted into the BiMap.
     * @param v6 the value to be associated with the sixth key in the BiMap.
     * @param k7 the seventh key to be inserted into the BiMap.
     * @param v7 the value to be associated with the seventh key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(7));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);

        return map;
    }

    /**
     * Creates a new BiMap with eight key-value pairs.
     * This method provides a convenient way to create a BiMap with eight entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4,
     *     "five", 5, "six", 6, "seven", 7, "eight", 8
     * );
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @param k6 the sixth key to be inserted into the BiMap.
     * @param v6 the value to be associated with the sixth key in the BiMap.
     * @param k7 the seventh key to be inserted into the BiMap.
     * @param v7 the value to be associated with the seventh key in the BiMap.
     * @param k8 the eighth key to be inserted into the BiMap.
     * @param v8 the value to be associated with the eighth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(8));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);

        return map;
    }

    /**
     * Creates a new BiMap with nine key-value pairs.
     * This method provides a convenient way to create a BiMap with nine entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4,
     *     "five", 5, "six", 6, "seven", 7, "eight", 8,
     *     "nine", 9
     * );
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @param k6 the sixth key to be inserted into the BiMap.
     * @param v6 the value to be associated with the sixth key in the BiMap.
     * @param k7 the seventh key to be inserted into the BiMap.
     * @param v7 the value to be associated with the seventh key in the BiMap.
     * @param k8 the eighth key to be inserted into the BiMap.
     * @param v8 the value to be associated with the eighth key in the BiMap.
     * @param k9 the ninth key to be inserted into the BiMap.
     * @param v9 the value to be associated with the ninth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9) throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(9));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);

        return map;
    }

    /**
     * Creates a new BiMap with ten key-value pairs.
     * This method provides a convenient way to create a BiMap with ten entries.
     *
     * <p>A key repeated in the argument list keeps only its last value, so the returned BiMap can contain
     * fewer entries than pairs supplied. A <i>value</i> that is still bound when it is supplied again is
     * rejected with {@link IllegalArgumentException} if it is bound to a different key; repeating the same
     * key-value mapping is allowed. A value displaced earlier by a repeated key may be reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of(
     *     "one", 1, "two", 2, "three", 3, "four", 4, "five", 5,
     *     "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10
     * );
     * }</pre>
     *
     * @param <K> the type of the keys.
     * @param <V> the type of the values.
     * @param k1 the first key to be inserted into the BiMap.
     * @param v1 the value to be associated with the first key in the BiMap.
     * @param k2 the second key to be inserted into the BiMap.
     * @param v2 the value to be associated with the second key in the BiMap.
     * @param k3 the third key to be inserted into the BiMap.
     * @param v3 the value to be associated with the third key in the BiMap.
     * @param k4 the fourth key to be inserted into the BiMap.
     * @param v4 the value to be associated with the fourth key in the BiMap.
     * @param k5 the fifth key to be inserted into the BiMap.
     * @param v5 the value to be associated with the fifth key in the BiMap.
     * @param k6 the sixth key to be inserted into the BiMap.
     * @param v6 the value to be associated with the sixth key in the BiMap.
     * @param k7 the seventh key to be inserted into the BiMap.
     * @param v7 the value to be associated with the seventh key in the BiMap.
     * @param k8 the eighth key to be inserted into the BiMap.
     * @param v8 the value to be associated with the eighth key in the BiMap.
     * @param k9 the ninth key to be inserted into the BiMap.
     * @param v9 the value to be associated with the ninth key in the BiMap.
     * @param k10 the tenth key to be inserted into the BiMap.
     * @param v10 the value to be associated with the tenth key in the BiMap.
     * @return a BiMap containing the specified key-value pairs; a key repeated in the argument list keeps
     *         only its last value, so the result can contain fewer entries than pairs supplied.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if a value is duplicated (bound to
     *         more than one key).
     */
    public static <K, V> BiMap<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7, final K k8, final V v8, final K k9, final V v9, final K k10, final V v10)
            throws IllegalArgumentException {
        final BiMap<K, V> map = new BiMap<>(capacityFor(10));

        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);
        map.put(k10, v10);

        return map;
    }

    /**
     * Creates a new BiMap that is a copy of the specified map.
     * This method creates a BiMap containing the same key-value mappings as the provided map.
     * The underlying map implementation is determined based on the type of the input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * BiMap<String, Integer> biMap = BiMap.copyOf(map);
     * }</pre>
     *
     * <p>When {@code map} is itself a {@code BiMap}, this is equivalent to {@link #copy()} on it: the copy
     * is built from that BiMap's own map suppliers, so a source backed by identity- or comparator-keyed
     * maps is reproduced exactly rather than being rebuilt through {@link HashMap}.</p>
     *
     * <p><b>Iteration order is best-effort, not guaranteed.</b> For any other source this method mirrors the
     * source's runtime map class, so a {@link java.util.LinkedHashMap} or {@link java.util.SortedMap} source
     * does keep its order (a {@code SortedMap}'s comparator included). A source whose class cannot be
     * instantiated reflectively - {@code Collections.unmodifiableMap(aLinkedHashMap)}, for instance - falls
     * back to a {@link HashMap}, and its order is then lost. Use {@link #builder()} and insert the entries
     * yourself, or supply explicit map suppliers to {@link #BiMap(Supplier, Supplier)}, when a particular
     * iteration order must be guaranteed.</p>
     *
     * @param <K> the type of the keys in the map.
     * @param <V> the type of the values in the map.
     * @param map the map whose entries are to be placed into the new BiMap, must not be {@code null}.
     * @return a new BiMap containing the same entries as the provided map.
     * @throws IllegalArgumentException if {@code map}, any key, or any value is {@code null}; if a value is
     *         bound to more than one key; or if {@code map} is a {@code BiMap} whose map suppliers do not
     *         return a new, empty, distinct map on each call, as required by the delegated {@link #copy()} operation.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> BiMap<K, V> copyOf(final Map<? extends K, ? extends V> map) throws IllegalArgumentException {
        // Reject null up front, before allocating the
        // two backing maps and their suppliers.
        N.checkArgNotNull(map, cs.map);

        if (map instanceof BiMap) {
            // Mirroring the runtime class of a BiMap source would build a BiMap-backed BiMap (twice the
            // maps, every put validated twice) AND, worse, would rebuild it through the plain HashMap that
            // Suppliers.ofMap(BiMap.class) produces - silently discarding a source whose own backing maps
            // key by identity or by a comparator. Two identity-distinct but equal keys then collapse into
            // one, and two identity-distinct but equal values are rejected as a duplicate. The source's own
            // copy() already reproduces it faithfully through its own suppliers.
            return ((BiMap<K, V>) map).copy();
        }

        final Map<K, V> keyMap = Maps.newTargetMap(map);
        final Map<V, K> valueMap = Maps.newOrderingMap(map);

        // Preserve a SortedMap's comparator in the key-map supplier: deriving the supplier from
        // keyMap.getClass() builds natural-order TreeMaps, so copy()/inverse().copy() of a BiMap
        // copied from a comparator-backed TreeMap would throw CCE for non-Comparable keys.
        // Capture the comparator itself rather than `map`: the supplier outlives this call (it is held by
        // the BiMap, its inverse and every copy), and capturing `map` would pin the whole source map.
        final Comparator<?> sourceComparator = map instanceof SortedMap ? ((SortedMap<K, V>) map).comparator() : null;
        final Supplier<? extends Map<K, V>> keyMapSupplier = map instanceof SortedMap ? () -> new TreeMap(sourceComparator)
                : Suppliers.ofMap(keyMap.getClass());
        final Supplier<? extends Map<V, K>> valueMapSupplier = Suppliers.ofMap(valueMap.getClass());

        final BiMap<K, V> biMap = new BiMap<>(keyMapSupplier, valueMapSupplier, keyMap, valueMap);

        biMap.putAll(map);

        return biMap;
    }

    /**
     * Retrieves the value to which the specified key is mapped in this BiMap.
     * Returns {@code null} if this BiMap contains no mapping for the key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * Integer value = map.get("one");   // returns 1
     * }</pre>
     *
     * @param key the key whose associated value is to be returned.
     * @return The value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     */
    @Override
    public V get(final Object key) {
        return keyMap.get(key);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code defaultValue} if this BiMap
     * contains no mapping for the key.
     *
     * <p>Delegates to the forward backing map, so this is a single lookup. The inherited
     * {@link Map#getOrDefault(Object, Object)} default would follow a missed {@code get} with a second
     * {@code containsKey} probe, which a BiMap never needs: it stores no {@code null} value, so a
     * {@code null} from {@code get} already means "absent".</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.getOrDefault("one", 0);   // returns 1
     * map.getOrDefault("two", 0);   // returns 0
     * }</pre>
     *
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if this BiMap contains no mapping for the key.
     * @return the value to which the specified key is mapped, or {@code defaultValue} if there is no mapping for the key.
     * @see #getByValueOrDefault(Object, Object)
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return keyMap.getOrDefault(key, defaultValue);
    }

    /**
     * Retrieves the key to which the specified value is mapped in this BiMap.
     * This is the inverted lookup operation, returning the key associated with the given value.
     * Returns {@code null} if this BiMap contains no mapping for the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.getByValue(1);   // returns "one"
     * }</pre>
     *
     * @param value the value whose associated key is to be returned.
     * @return The key to which the specified value is mapped, or {@code null} if this map contains no mapping for the value.
     */
    public K getByValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        return valueMap.get(value);
    }

    /**
     * Retrieves the key associated with the specified value, or returns the default key if this BiMap contains no mapping for the value.
     * This method provides a safe way to perform inverted lookups with a fallback value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.getByValueOrDefault(2, "unknown");   // returns "unknown"
     * }</pre>
     *
     * @param value the value whose associated key is to be returned.
     * @param defaultValue the key to return if this map contains no mapping for the given value.
     * @return the key to which the specified value is mapped, or {@code defaultValue} if this map contains no mapping for the value.
     */
    public K getByValueOrDefault(final Object value, final K defaultValue) {
        //noinspection SuspiciousMethodCalls
        return valueMap.getOrDefault(value, defaultValue);
    }

    /**
     * Associates the specified value with the specified key in this BiMap.
     * If the BiMap previously contained a mapping for the key, the old value is replaced.
     *
     * <p><b>Bijective Constraint:</b> Both keys and values must be unique in a BiMap.
     * If the specified value is already bound to a different key, this method throws
     * {@code IllegalArgumentException}. Use {@link #forcePut} instead to automatically
     * remove the conflicting entry.
     *
     * <p><b>Behavior Differences:</b>
     * <ul>
     *   <li><b>{@code put()}:</b> Throws exception if value already exists (mapped to a different key)</li>
     *   <li><b>{@code forcePut()}:</b> Silently removes any existing entry with the same value</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * map.put("one", 1);        // adds mapping: "one" -> 1
     * map.put("one", 2);        // replaces value for "one": "one" -> 2
     *
     * // This throws IllegalArgumentException because 2 is already mapped to "one"
     * // map.put("two", 2);   // ERROR!
     *
     * // Use forcePut to override the conflict
     * map.forcePut("two", 2);   // removes ("one", 2), adds ("two", 2)
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @throws IllegalArgumentException if the key or value is {@code null}, or if the given value is already bound
     *         to a different key in this BiMap. The BiMap will remain unmodified in this event. To avoid this
     *         exception, call {@link #forcePut} instead.
     * @see #forcePut(Object, Object)
     */
    @Override
    public V put(final K key, final V value) throws IllegalArgumentException {
        return put(key, value, false);
    }

    /**
     * Inserts all entries from the specified map into this BiMap.
     * Each key-value pair in the provided map is inserted into this BiMap using {@link #put}.
     * If a key in the provided map is already present in this BiMap, the associated value is replaced.
     *
     * <p><b>Warning:</b> The results of calling this method may vary depending on the iteration order of {@code map}.
     * If the operation fails, some entries may have already been added to the BiMap before the exception was thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> biMap = new BiMap<>();
     * Map<String, Integer> map = Map.of("one", 1, "two", 2);
     * biMap.putAll(map);
     * }</pre>
     *
     * @param m the map whose entries are to be added to this BiMap, must not be {@code null}.
     * @throws NullPointerException if {@code m} is {@code null}.
     * @throws IllegalArgumentException if any key or value is {@code null}, or if an attempt to {@code put} any
     *         entry fails due to a duplicate value. Note that some map entries may have been added to the BiMap
     *         before the exception was thrown.
     * @see #put(Object, Object)
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) throws NullPointerException, IllegalArgumentException {
        N.requireNonNull(m, "m");

        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * An alternate form of {@code put} that silently removes any existing entry
     * with the value {@code value} before proceeding with the {@link #put}
     * operation. If the BiMap previously contained the provided key-value
     * mapping, this method has no effect.
     *
     * <p>This method ensures that the value is unique by removing any existing
     * entry with the same value, even if it's mapped to a different key.
     *
     * <p><b>Behavior Differences:</b>
     * <ul>
     *   <li><b>{@code put()}:</b> Throws exception if value already exists (mapped to a different key)</li>
     *   <li><b>{@code forcePut()}:</b> Silently removes any existing entry with the same value</li>
     * </ul>
     *
     * <p><b>Size Impact:</b> A successful call to this method could cause the size of the
     * BiMap to increase by one, stay the same, or even decrease by one, depending on whether
     * the operation removes existing entries:
     * <ul>
     *   <li><b>Increase by one:</b> New key, new value</li>
     *   <li><b>Stay the same:</b> Existing key, new value OR new key, existing value (removes one, adds one)</li>
     *   <li><b>Decrease by one:</b> Existing key, existing value mapped to different key (removes two, adds one)</li>
     * </ul>
     *
     * <p><b>Warning:</b> If an existing entry with this value is removed, the key
     * for that entry is discarded and not returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     *
     * // Using put() would throw IllegalArgumentException here
     * // map.put("three", 1);   // ERROR: value 1 already exists!
     *
     * // forcePut() removes conflicting entry and adds new one
     * map.forcePut("three", 1);   // removes ("one", 1), adds ("three", 1)
     * // Result: {"two"=2, "three"=1}
     *
     * // Force put with both key and value existing elsewhere
     * map.put("four", 4);
     * map.forcePut("three", 4);   // removes ("three", 1) and ("four", 4), adds ("three", 4)
     * // Result: {"two"=2, "three"=4}
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     * @return the previous value associated with the key, or {@code null} if there was no mapping for the key.
     * @throws IllegalArgumentException if the key or value is {@code null}.
     * @see #put(Object, Object)
     */
    public V forcePut(final K key, final V value) throws IllegalArgumentException {
        return put(key, value, true);
    }

    /**
     * @throws IllegalArgumentException if {@code key} or {@code value} is {@code null}, or {@code isForce} is false and the value is already bound to another key
     */
    private V put(final K key, final V value, final boolean isForce) throws IllegalArgumentException {
        if ((key == null) || (value == null)) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        final V oldValue = keyMap.get(key);
        final K keyForValue = valueMap.get(value);
        final K keyForOldValue = oldValue == null ? null : valueMap.get(oldValue);
        // No insertion path stores a null key or value, so valueMap never holds a null value: a non-null
        // keyForValue is exactly valueMap.containsKey(value), and saves two more lookups on this hot path.
        final boolean valueAlreadyBound = keyForValue != null;
        final boolean sameMapping = oldValue != null && valueAlreadyBound && keyForValue == keyForOldValue;
        // Maps such as TreeMap and HashMap retain the stored key object when an equivalent key is
        // updated. Keep that canonical key in the inverse map as well; otherwise the two directions
        // can expose different key objects after put(equivalentKey, newValue). A missed reverse lookup
        // (a value whose hashCode or ordering changed while it was stored, or a backing map that dropped
        // the entry of its own accord) must NOT inject a null into valueMap: that falsifies the invariant
        // stated above, which would silently disable the value-uniqueness check for that value.
        K canonicalKey = oldValue == null || keyForOldValue == null ? key : keyForOldValue;

        // Compare the inverse entries, rather than K.equals/V.equals, so both backing maps' own
        // equality semantics are honored (including comparator-based TreeMaps). Two equivalent
        // lookups in valueMap return the same stored key reference.
        if (!isForce && valueAlreadyBound && !sameMapping) {
            throw new IllegalArgumentException("Value already exists: " + value + " is already bound to key: " + keyForValue);
        }

        // No-op when the exact mapping already exists: documented for forcePut, and re-inserting
        // would needlessly move the entry to the end of LinkedHashMap-backed BiMaps. "Same" here
        // deliberately follows the backing maps' equality semantics.
        if (sameMapping) {
            return oldValue;
        }

        // Both backing maps are written below. If either rejects a write - a comparator-backed TreeMap
        // that cannot order the value, for example - undo whatever has already been written: a
        // half-committed mutation leaves this BiMap permanently non-bijective, and the entry displaced
        // by forcePut would otherwise be destroyed outright.
        // Capture the displaced objects before removal. Restoring the incoming (merely equivalent) value
        // would change the original mapping's object identity even when the rollback otherwise succeeds.
        final K displacedKey = keyForValue;
        final V displacedValue = displacedKey == null ? null : keyMap.get(displacedKey);
        V displacedReverseValue = displacedValue;

        if (displacedKey != null && (displacedValue == null || valueMap.get(displacedValue) != displacedKey)) {
            // An orphaned reverse binding may have no matching forward entry. Preserve what was actually
            // present rather than manufacturing a new forward mapping during rollback.
            displacedReverseValue = value;

            for (final Map.Entry<V, K> entry : valueMap.entrySet()) {
                if (entry.getValue() == displacedKey) {
                    displacedReverseValue = entry.getKey();
                    break;
                }
            }
        }

        boolean restoreOldReverse = false;
        boolean displacedReverseRemovalAttempted = false;
        boolean displacedForwardRemovalAttempted = false;
        boolean forwardWriteAttempted = false;
        boolean reverseWriteAttempted = false;

        try {
            if (oldValue != null) {
                // Mark a reachable binding before attempting removal: a custom map can throw after
                // changing it. Rollback checks the current mapping and skips a write if it is intact.
                restoreOldReverse = keyForOldValue != null;
                if (valueMap.remove(oldValue) != null) {
                    restoreOldReverse = true;
                } else {
                    // The old value can no longer be found by value, so its reverse entry would survive as
                    // a stale duplicate and desynchronize the two maps. Drop it by key instead - but match
                    // that key by identity against the object keyMap itself holds, because the forward map
                    // decides what "the same key" means and it need not be equals() (an IdentityHashMap
                    // keeps two equal keys apart; a comparator-keyed TreeMap merges two unequal ones). The
                    // incumbent is the forward key bound to this very value object; values are unique, so
                    // at most one entry matches. This scan runs only on that already inconsistent path,
                    // never on the normal one.
                    K incumbentKey = key;

                    for (final Map.Entry<K, V> entry : keyMap.entrySet()) {
                        if (entry.getValue() == oldValue) {
                            incumbentKey = entry.getKey();
                            break;
                        }
                    }

                    final K staleKey = incumbentKey;
                    // Only an entry that was really dropped may be restored below: re-inserting a reverse
                    // entry that was never there can evict a live one from a bounded backing map.
                    restoreOldReverse = valueMap.values().removeIf(storedKey -> storedKey == staleKey);
                    canonicalKey = staleKey;
                }
            }

            displacedReverseRemovalAttempted = displacedKey != null;
            valueMap.remove(value);

            if (displacedKey != null) {
                displacedForwardRemovalAttempted = true;
                keyMap.remove(displacedKey);
            }

            // put without a prior remove(key) so an existing key keeps its position in ordered backing
            // maps (LinkedHashMap re-insertion would move it to the end).
            forwardWriteAttempted = true;
            keyMap.put(key, value);
            reverseWriteAttempted = true;
            valueMap.put(value, canonicalKey);
        } catch (final RuntimeException | Error e) {
            // Each undo is guarded on its own: a backing map that rejects one restoring write must not
            // abandon the remaining ones, and the entry displaced by forcePut - restored last, because an
            // inconsistent map can bind `value` to `key` itself, in which case the forward undo above must
            // run first - is the one piece of state that is otherwise destroyed outright.
            if (reverseWriteAttempted) {
                // A put implementation may insert and then throw. Remove that new reverse binding before
                // restoring either original one; otherwise a failed write can leave a phantom value.
                restoreMapping(valueMap, value, null, e);
            }

            if (forwardWriteAttempted) {
                restoreMapping(keyMap, canonicalKey, oldValue, e);
            }

            if (restoreOldReverse) {
                restoreMapping(valueMap, oldValue, canonicalKey, e);
            }

            if (displacedForwardRemovalAttempted) {
                restoreMapping(keyMap, displacedKey, displacedValue, e);
            }

            if (displacedReverseRemovalAttempted) {
                // Its removal precedes the forward removal and can need undoing even if that removal
                // failed. Guard each direction separately so one failed restore cannot skip the other.
                restoreMapping(valueMap, displacedReverseValue, displacedKey, e);
            }

            throw e;
        }

        return oldValue;
    }

    /** Restores one original mapping (null means absent), without rewriting an already intact entry. */
    private static <K, V> void restoreMapping(final Map<K, V> map, final K key, final V originalValue, final Throwable thrown) {
        try {
            if (map.get(key) != originalValue) {
                if (originalValue == null) {
                    map.remove(key);
                } else {
                    map.put(key, originalValue);
                }
            }
        } catch (final RuntimeException | Error restoreFailed) {
            addSuppressedRestoreFailure(thrown, restoreFailed);
        }
    }

    /**
     * Attaches a failed undo to the exception that is about to propagate, so a failing restore never hides
     * why the mutation itself failed. A backing map that throws one cached exception instance would
     * otherwise make {@code addSuppressed} raise {@code IllegalArgumentException: Self-suppression not
     * permitted} - the very exception type {@link #put} documents for its own contract violations.
     *
     * @param thrown the exception the mutator is about to rethrow
     * @param restoreFailed the exception a restoring write threw
     */
    private static void addSuppressedRestoreFailure(final Throwable thrown, final Throwable restoreFailed) {
        if (restoreFailed != thrown) {
            thrown.addSuppressed(restoreFailed);
        }
    }

    /**
     * Inserts all entries from the specified map into this BiMap using {@link #forcePut} for each entry.
     * Unlike {@link #putAll(Map)}, value conflicts do not throw; any existing entry whose value collides
     * with an incoming value is silently removed before the incoming entry is inserted.
     *
     * <p><b>Warning:</b> The results of calling this method may vary depending on the iteration order of {@code m}.
     * If the operation fails (e.g. a {@code null} key or value), some entries may have already been added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> biMap = BiMap.of("one", 1);
     * Map<String, Integer> map = Map.of("two", 1, "three", 3);
     * biMap.forcePutAll(map);   // ("one", 1) replaced by ("two", 1); ("three", 3) added
     * }</pre>
     *
     * @param m the map whose entries are to be force-inserted into this BiMap, must not be {@code null}.
     * @throws IllegalArgumentException if {@code m} is {@code null}
     *         or if any key or value is {@code null}.
     * @see #forcePut(Object, Object)
     * @see #putAll(Map)
     */
    public void forcePutAll(final Map<? extends K, ? extends V> m) throws IllegalArgumentException {
        N.checkArgNotNull(m, cs.m);

        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            forcePut(e.getKey(), e.getValue());
        }
    }

    /**
     * Associates the specified value with the specified key only if the key is not already present.
     * If the key is already mapped, the existing mapping is left unchanged and its value is returned.
     *
     * <p>Unlike the inherited {@link Map#putIfAbsent(Object, Object)} default method, this override
     * applies the bijective constraint: if the key is absent but the value is already bound to a
     * different key, an {@link IllegalArgumentException} is thrown (use {@link #forcePut(Object, Object)}
     * to override such a conflict).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.putIfAbsent("one", 99);   // returns 1, mapping unchanged: "one" -> 1
     * map.putIfAbsent("two", 2);    // returns null, adds: "two" -> 2
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     * @return the value already associated with the key, or {@code null} if there was no mapping and the value was inserted.
     * @throws IllegalArgumentException if the key or value is {@code null}, or if the key is absent but the given
     *         value is already bound to a different key. To avoid this exception, call {@link #forcePut} instead.
     * @see #put(Object, Object)
     * @see #forcePut(Object, Object)
     */
    @Override
    public V putIfAbsent(final K key, final V value) throws IllegalArgumentException {
        if ((key == null) || (value == null)) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        final V curValue = keyMap.get(key);

        if (curValue != null) {
            return curValue;
        }

        put(key, value);

        return null;
    }

    /**
     * Removes the mapping for a key from this BiMap if it is present.
     * This operation also removes the inverted mapping from value to key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * Integer value = map.remove("one");   // returns 1, removes mapping
     * }</pre>
     *
     * @param key the key whose mapping is to be removed from the BiMap.
     * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
     */
    @Override
    public V remove(final Object key) {
        final V removedValue = keyMap.remove(key);

        if (removedValue != null) {
            valueMap.remove(removedValue);
        }

        return removedValue;
    }

    /**
     * Removes the mapping for a value from this BiMap if it is present.
     * This is the inverted removal operation, removing the entry by its value and returning the associated key.
     * This operation also removes the mapping from key to value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * String key = map.removeByValue(1);   // returns "one", removes mapping
     * }</pre>
     *
     * @param value the value whose mapping is to be removed from the BiMap.
     * @return The key associated with the value, or {@code null} if there was no mapping for the value.
     */
    public K removeByValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        final K removedKey = valueMap.remove(value);

        if (removedKey != null) {
            keyMap.remove(removedKey);
        }

        return removedKey;
    }

    /**
     * Checks if this BiMap contains a mapping for the specified key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * boolean exists = map.containsKey("one");   // returns true
     * }</pre>
     *
     * @param key the key whose presence in this BiMap is to be tested.
     * @return {@code true} if this BiMap contains a mapping for the specified key, {@code false} otherwise.
     */
    @Override
    public boolean containsKey(final Object key) {
        return keyMap.containsKey(key);
    }

    /**
     * Checks if this BiMap contains a mapping for the specified value.
     * In a BiMap, values are unique and can be used for lookups just like keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * boolean exists = map.containsValue(1);   // returns true
     * }</pre>
     *
     * @param value the value whose presence in this BiMap is to be tested.
     * @return {@code true} if this BiMap contains a mapping for the specified value, {@code false} otherwise.
     */
    @Override
    public boolean containsValue(final Object value) {
        //noinspection SuspiciousMethodCalls
        return valueMap.containsKey(value);
    }

    /**
     * Checks if this BiMap contains the exact key-value mapping specified, i.e. the key is present
     * and is mapped to a value equal to {@code value}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.containsEntry("one", 1);   // returns true
     * map.containsEntry("one", 2);   // returns false
     * map.containsEntry("two", 1);   // returns false
     * }</pre>
     *
     * @param key the key whose mapping is to be tested.
     * @param value the value expected to be associated with the key.
     * @return {@code true} if this BiMap maps {@code key} to a value equal to {@code value}, {@code false} otherwise.
     * @see #containsKey(Object)
     * @see #containsValue(Object)
     */
    public boolean containsEntry(final Object key, final Object value) {
        //noinspection SuspiciousMethodCalls
        final V curValue = keyMap.get(key);

        return curValue != null && curValue.equals(value);
    }

    /**
     * Returns an immutable set of keys contained in this BiMap.
     * The returned set is a view backed by the BiMap, so changes to the BiMap are reflected in the set,
     * but the set itself cannot be modified directly.
     *
     * <p>This view, {@link #values()} and {@link #entrySet()} all iterate the forward (key-to-value)
     * backing map, so their iteration orders correspond entry for entry.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * Set<String> keys = map.keySet();   // contains "one" and "two" (iteration order follows the backing map)
     * }</pre>
     *
     * @return An immutable set of the keys contained in this BiMap.
     */
    @Override
    public ImmutableSet<K> keySet() {
        ImmutableSet<K> result = keySet;

        if (result == null) {
            keySet = result = ImmutableSet.wrap(keyMap.keySet());
        }

        return result;
    }

    /**
     * Returns an immutable set of values contained in this BiMap.
     * Unlike a regular Map where values() returns a Collection, BiMap returns a Set because values are unique.
     * The returned set is a view backed by the BiMap, so changes to the BiMap are reflected in the set,
     * but the set itself cannot be modified directly.
     *
     * <p>The values are iterated in the order of the forward (key-to-value) backing map, so this view
     * corresponds entry for entry with {@link #keySet()} and {@link #entrySet()}, exactly as for any other
     * {@link Map}. It does <i>not</i> follow the reverse map's own order, which is unrelated: with ordered
     * backing maps the two can differ, and with differently-typed backing maps (say a
     * {@code LinkedHashMap} forward map and a {@code TreeMap} reverse map) they routinely do.</p>
     *
     * <p>{@code contains} is answered by the reverse map, so it stays a constant-time lookup.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("a", 3);                      // "a" keeps its position, its value becomes 3
     * System.out.println(map.keySet());     // prints [a, b]
     * System.out.println(map.values());     // prints [3, 2]  - aligned with keySet()
     * System.out.println(map.entrySet());   // prints [a=3, b=2]
     * }</pre>
     *
     * @return An immutable set of the values contained in this BiMap.
     */
    @Override
    public ImmutableSet<V> values() {
        ImmutableSet<V> result = values;

        if (result == null) {
            // Read the values off the FORWARD map so that keySet()/values()/entrySet() present the same
            // entries in the same order; valueMap.keySet() holds the same elements but in the reverse
            // map's own iteration order, which pairs values with the wrong keys positionally.
            values = result = ImmutableSet.wrap(new AbstractSet<>() {
                @Override
                public Iterator<V> iterator() {
                    return ObjIterator.of(keyMap.values().iterator());
                }

                @Override
                public Spliterator<V> spliterator() {
                    // Preserve the forward map's encounter order without claiming projected values are sorted.
                    return Spliterators.spliterator(this, Spliterator.DISTINCT | (keyMap.entrySet().spliterator().characteristics() & Spliterator.ORDERED));
                }

                @Override
                public int size() {
                    return keyMap.size();
                }

                @Override
                public boolean contains(final Object o) {
                    // Keep the O(1) membership test that valueMap.keySet() provided.
                    //noinspection SuspiciousMethodCalls
                    return valueMap.containsKey(o);
                }
            });
        }

        return result;
    }

    /**
     * Returns an immutable set of the entries contained in this BiMap.
     * Each entry is a key-value pair from the BiMap.
     * The returned set is a live view backed by the BiMap, so changes to the BiMap are reflected in the
     * set, but neither the set nor the entries it yields can be modified: the iterator returns
     * {@link ImmutableEntry} snapshots, whose {@code setValue} throws
     * {@link UnsupportedOperationException}. Iterating the set after the BiMap changes sees the new
     * contents; an entry object obtained before the change keeps the value it was created with.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * Set<Map.Entry<String, Integer>> entries = map.entrySet();
     * }</pre>
     *
     * @return An immutable set of the entries (key-value pairs) contained in this BiMap.
     */
    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        ImmutableSet<Map.Entry<K, V>> result = entrySet;

        if (result == null) {
            entrySet = result = ImmutableSet.wrap(new AbstractSet<>() {
                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new ObjIterator<>() {
                        private final Iterator<Map.Entry<K, V>> keyValueEntryIter = keyMap.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return keyValueEntryIter.hasNext();
                        }

                        /**
                         * {@inheritDoc}
                         * @throws NoSuchElementException if no entry remains in the backing-map iterator
                         */
                        @Override
                        public ImmutableEntry<K, V> next() throws NoSuchElementException {
                            return ImmutableEntry.copyOf(keyValueEntryIter.next());
                        }
                    };
                }

                @Override
                public Spliterator<Map.Entry<K, V>> spliterator() {
                    // Traverse this view so entries remain immutable snapshots, including in parallel streams.
                    return Spliterators.spliterator(this, Spliterator.DISTINCT | (keyMap.entrySet().spliterator().characteristics() & Spliterator.ORDERED));
                }

                @Override
                public int size() {
                    return keyMap.size();
                }

                @Override
                public boolean contains(final Object o) {
                    // AbstractCollection.contains would scan linearly; the forward map answers directly.
                    return o instanceof Map.Entry<?, ?> entry && containsEntry(entry.getKey(), entry.getValue());
                }
            });
        }

        return result;
    }

    /**
     * Performs the given action for each entry in this BiMap until all entries have been processed or the
     * action throws an exception.
     *
     * <p>Delegates to the forward backing map so that its optimized traversal is used. The inherited
     * {@link Map#forEach(BiConsumer)} default iterates {@link #entrySet()}, which materializes one
     * {@link ImmutableEntry} snapshot per entry purely to read the key and value back out of it.</p>
     *
     * <p>The action must not modify this BiMap; doing so has the same undefined effect as modifying the
     * backing map during any other iteration.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
     * map.put("one", 1);
     * map.put("two", 2);
     * map.forEach((k, v) -> System.out.println(k + "=" + v));   // prints one=1 then two=2
     * }</pre>
     *
     * @param action the action to be performed for each entry; must not be {@code null}
     * @throws NullPointerException if {@code action} is {@code null}
     * @see Map#forEach(BiConsumer)
     */
    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) throws NullPointerException {
        Objects.requireNonNull(action);

        keyMap.forEach(action);
    }

    /**
     * Replaces each entry's value with the result of applying the given function to that entry.
     * <p>
     * The default {@link Map#replaceAll} implementation updates values via {@code Entry.setValue},
     * but this BiMap's {@link #entrySet()} yields immutable entry snapshots. This override applies
     * updates through the backing maps directly so the bijective maps stay consistent.
     * </p>
     * <p>
     * All replacement values are computed and staged before this BiMap is modified: the function
     * is invoked exactly once per entry with its original key and value, each result must be
     * non-{@code null}, and no two results may be equal under the value map's own equivalence
     * (for example, a case-insensitive comparator). If function evaluation or staging fails, an
     * exception is thrown and this BiMap is left unchanged. Once staging succeeds, the existing
     * forward entries are updated in place and the reverse map is rebuilt from the staged inverse.
     * As with other mutating operations, an exception thrown by a backing map during this final
     * commit may leave the BiMap partially updated.
     * </p>
     * <p>
     * The function must not modify this BiMap while the replacement values are being computed.
     * </p>
     *
     * @param function the function to apply to each entry; must not be {@code null}
     * @throws NullPointerException if {@code function} is {@code null}, as {@link Map#replaceAll} specifies
     * @throws IllegalArgumentException if a replacement value is null or duplicated, the staging suppliers return null, non-empty, or identical maps, or the staging key map collapses keys that are distinct in the live map.
     */
    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) throws NullPointerException, IllegalArgumentException {
        N.requireNonNull(function, cs.function);

        if (keyMap.isEmpty()) {
            return;
        }

        // Constructing a temporary BiMap validates that both user-supplied factories still return
        // non-null, empty, distinct maps. It also lets all key/value constraints be exercised before
        // either live backing map is touched.
        final BiMap<K, V> staged = new BiMap<>(keyMapSupplier, valueMapSupplier);
        final int size = keyMap.size();
        final List<ImmutableEntry<K, V>> entries = new ArrayList<>(size);

        for (final Map.Entry<K, V> entry : keyMap.entrySet()) {
            entries.add(ImmutableEntry.copyOf(entry));
        }

        // Compute all replacement values before mutating this BiMap, so a failing function leaves
        // the map unchanged.
        final List<V> newValues = new ArrayList<>(size);

        for (ImmutableEntry<K, V> entry : entries) {
            final V newValue = function.apply(entry.getKey(), entry.getValue());

            if (newValue == null) {
                throw new IllegalArgumentException("function returned null for key: " + entry.getKey());
            }

            newValues.add(newValue);
        }

        // put() validates each result with both staged backing maps, including comparator- or
        // identity-based value equivalence. A complete staging pass also permits swaps and cycles.
        for (int i = 0; i < size; i++) {
            staged.put(entries.get(i).getKey(), newValues.get(i));

            if (staged.size() != i + 1) {
                throw new IllegalArgumentException("The key map supplier does not preserve the live key map's equivalence semantics");
            }
        }

        // Keys do not change, so update their values without clearing keyMap. This preserves
        // canonical key objects and avoids structural churn in insertion-ordered backing maps.
        for (int i = 0; i < size; i++) {
            keyMap.put(entries.get(i).getKey(), newValues.get(i));
        }

        valueMap.clear();
        valueMap.putAll(staged.valueMap);
    }

    /**
     * Returns the inverse view of this BiMap, which maps each of this BiMap's values to its associated key.
     * The two BiMaps are backed by the same underlying data; any changes to one will appear in the other.
     * This provides an efficient way to perform reverse lookups without creating a separate copy.
     *
     * <p>The noun <i>inverse</i> denotes this shared, bidirectional view. Copy-producing key/value
     * transformations are instead named <i>invert</i>; see {@link Maps#invert(Map)} and
     * {@link Multimap#invert(java.util.function.IntFunction)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * BiMap<Integer, String> inverse = map.inverse();
     * String key = inverse.get(1);   // returns "one"
     * }</pre>
     *
     * <p>The inverse view is cached, so repeated calls return the same instance. Calling
     * {@code inverse()} on the returned view returns this original BiMap (not a new instance).</p>
     *
     * @return the inverse view of this BiMap where keys and values are swapped.
     */
    public BiMap<V, K> inverse() {
        return (invertedView == null) ? invertedView = new BiMap<>(valueMap, keyMap, this) : invertedView;
    }

    /**
     * Creates a new BiMap that is a shallow copy of the current BiMap.
     * The new BiMap will contain the same key-value mappings as this BiMap,
     * but is independent - changes to one will not affect the other.
     * The underlying map implementations are created using the same suppliers as this BiMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> original = BiMap.of("one", 1);
     * BiMap<String, Integer> copy = original.copy();
     * }</pre>
     *
     * @return a new BiMap containing the same entries as the current BiMap.
     * @throws IllegalArgumentException if this BiMap's map suppliers do not return a new, empty, distinct
     *         map on each call - a supplier that hands out one shared instance, for example.
     */
    public BiMap<K, V> copy() throws IllegalArgumentException {
        final BiMap<K, V> copy = new BiMap<>(keyMapSupplier, valueMapSupplier);

        // The constructor can only check that the supplied maps are empty and differ from each other at
        // that moment. A supplier handing out one shared instance therefore passes construction whenever
        // this BiMap is still empty, and the "copy" would then be an alias: writes to it would show up
        // here. (Once this BiMap is non-empty, that constructor check already rejects it.)
        // Either new direction can alias either original direction when K and V have the same type.
        if (copy.keyMap == keyMap || copy.keyMap == valueMap || copy.valueMap == keyMap || copy.valueMap == valueMap) {
            throw new IllegalArgumentException("The map suppliers returned this BiMap's own backing maps; they must return new empty maps on every call");
        }

        copy.putAll(keyMap);

        return copy;
    }

    /**
     * Removes all the mappings from this BiMap.
     * Both the key-to-value and value-to-key mappings are removed.
     * The BiMap will be empty after this call returns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1);
     * map.clear();
     * map.isEmpty();   // returns true
     * }</pre>
     *
     */
    @Override
    public void clear() {
        keyMap.clear();
        valueMap.clear();
    }

    /**
     * Checks if this BiMap is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = new BiMap<>();
     * boolean empty = map.isEmpty();   // returns true
     * }</pre>
     *
     * @return {@code true} if this BiMap contains no key-value mappings, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return keyMap.isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this BiMap.
     * This is equivalent to the number of keys or values, as they are always equal in a BiMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);
     * int count = map.size();   // returns 2
     * }</pre>
     *
     * @return the number of key-value mappings in this BiMap.
     */
    @Override
    public int size() {
        return keyMap.size();
    }

    /**
     * Returns the hash code value for this BiMap.
     * The hash code of a BiMap is defined to be the sum of the hash codes of each entry in the BiMap,
     * consistent with the contract of {@link Map#hashCode()}.
     *
     * @return the hash code value for this BiMap.
     */
    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    /**
     * Compares the specified object with this BiMap for equality.
     * Returns {@code true} if the given object is also a {@link Map} and the two represent the same key-value mappings.
     * The comparison is delegated to the underlying key-to-value map's {@code equals} method,
     * so any {@code Map} (not just a BiMap) with the same mappings is considered equal.
     *
     * <p><b>Equality is delegated to the key-to-value backing map, so it is only well defined against maps
     * that share its key equivalence.</b> Compared with a map that judges keys differently - an
     * {@link java.util.IdentityHashMap}, or a {@link java.util.TreeMap} whose comparator is inconsistent
     * with {@code equals} - the comparison can succeed in one direction and fail in the other, and two
     * maps that compare equal can report different hash codes. This is inherited from
     * {@link Map#equals(Object)}, which probes the other map with its own lookup rules.</p>
     *
     * @param obj the object to be compared for equality with this BiMap.
     * @return {@code true} if the specified object is a Map equal to this BiMap, {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Map && keyMap.equals(obj));
    }

    /**
     * Returns a string representation of this BiMap.
     * The string representation consists of a list of key-value mappings in the BiMap, enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters ", " (comma and space).
     * Each key-value mapping is rendered as the key followed by an equal sign ("=") followed by the associated value.
     * The format is consistent with {@code Map.toString()}.
     *
     * <p><b>Usage Examples:</b> {@code {one=1, two=2}}</p>
     *
     * @return a string representation of this BiMap.
     */
    @Override
    public String toString() {
        return keyMap.toString();
    }

    /**
     * Creates a new Builder for constructing a BiMap.
     * The Builder pattern allows for fluent construction of BiMaps with multiple entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
     *     .put("one", 1)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of the keys in the BiMap.
     * @param <V> the type of the values in the BiMap.
     * @return a new Builder instance for a BiMap.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new Builder for a BiMap initialized with the specified map's entries.
     * This allows starting with an existing map and adding additional entries via the builder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> existing = Map.of("one", 1);
     * BiMap<String, Integer> map = BiMap.builder(existing)
     *     .put("two", 2)
     *     .build();
     * }</pre>
     *
     * @param <K> the type of the keys in the BiMap.
     * @param <V> the type of the values in the BiMap.
     * @param map the map whose entries are to be placed into the new BiMap, must not be {@code null}.
     * @return a new Builder instance for a BiMap with the specified map as its initial data.
     * @throws IllegalArgumentException if {@code map} is {@code null}, or if any key or value in {@code map} is
     *         {@code null}, or if {@code map} contains duplicate values.
     */
    public static <K, V> Builder<K, V> builder(final Map<K, V> map) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        return new Builder<>(map);
    }

    /**
     * This is a static inner class that provides a builder for the BiMap.
     * The Builder design pattern allows for the creation of complex objects step by step.
     * This Builder class provides methods to set the keys and values for the BiMap and build the BiMap when ready.
     *
     * @param <K> the type of the keys in the BiMap.
     * @param <V> the type of the values in the BiMap.
     */
    public static final class Builder<K, V> {
        private final BiMap<K, V> biMap;

        /**
         * Creates a Builder backed by a new, empty {@link HashMap}-based BiMap.
         */
        Builder() {
            biMap = new BiMap<>();
        }

        /**
         * Creates a Builder backed by a new BiMap pre-populated with the entries of {@code backedMap}.
         *
         * @param backedMap the map whose entries seed the BiMap being built; it is copied, not wrapped.
         * @throws NullPointerException if {@code backedMap} is {@code null}.
         * @throws IllegalArgumentException if any key or value in {@code backedMap} is {@code null}, or if
         *         {@code backedMap} contains a duplicated value (bound to more than one key).
         */
        Builder(final Map<K, V> backedMap) throws NullPointerException, IllegalArgumentException {
            biMap = BiMap.copyOf(backedMap);
        }

        /**
         * Adds a key-value pair to the BiMap being built.
         * If the BiMap previously contained a mapping for the key, the old value is replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .put("two", 2)
         *     .build();
         * }</pre>
         *
         * @param key the key with which the specified value is to be associated.
         * @param value the value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if the key or value is {@code null}, or if the given value is already
         *         bound to a different key in the BiMap being built. The BiMap being built will remain unmodified in
         *         this event.
         * @see #forcePut(Object, Object)
         */
        public Builder<K, V> put(final K key, final V value) throws IllegalArgumentException {
            biMap.put(key, value);

            return this;
        }

        /**
         * Associates the specified value with the specified key in the BiMap being built, forcefully removing any existing mapping with the same value.
         * If the BiMap being built previously contained a mapping for the key or value, the old value or key is replaced.
         *
         * <p>This method is an alternate form of put that silently removes any existing entry with the value before proceeding with the put operation.
         * If the BiMap being built previously contained the provided key-value mapping, this method has no effect.
         *
         * <p>Note that a successful call to this method could cause the size of the BiMap being built to increase by one, stay the same, or even decrease by one.
         *
         * <p>Warning: If an existing entry with this value is removed, the key for that entry is discarded and not returned.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .forcePut("two", 1) // removes ("one", 1), adds ("two", 1)
         *     .build();
         * }</pre>
         *
         * @param key the key with which the specified value is to be associated.
         * @param value the value to be associated with the specified key.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if the key or value is {@code null}.
         * @see #put(Object, Object)
         */
        public Builder<K, V> forcePut(final K key, final V value) throws IllegalArgumentException {
            biMap.forcePut(key, value);

            return this;
        }

        /**
         * Inserts all entries from the specified map into the BiMap being built.
         * Each entry in the provided map is added using {@link #put(Object, Object)}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> existing = Map.of("one", 1, "two", 2);
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .putAll(existing)
         *     .put("three", 3)
         *     .build();
         * }</pre>
         *
         * @param m the map whose entries are to be added to the BiMap being built; a {@code null} or empty map is silently ignored.
         * @return This Builder instance to allow for chaining of calls to builder methods.
         * @throws IllegalArgumentException if any key or value is {@code null}, or if an attempt to {@code put} any
         *         entry fails due to a duplicate value. Note that some map entries may have been added to the BiMap
         *         before the exception was thrown.
         * @see #put(Object, Object)
         * @see #forcePut(Object, Object)
         */
        public Builder<K, V> putAll(final Map<? extends K, ? extends V> m) throws IllegalArgumentException {
            if (N.notEmpty(m)) {
                biMap.putAll(m);
            }

            return this;
        }

        /**
         * Returns the BiMap instance that has been built up by the builder's methods.
         * This finalizes the construction and returns the completed BiMap.
         *
         * <p>The returned BiMap is the builder's own mutable instance, not a defensive copy: it can be
         * modified afterwards, and any further calls on this builder are reflected in it.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BiMap<String, Integer> map = BiMap.<String, Integer>builder()
         *     .put("one", 1)
         *     .put("two", 2)
         *     .build();
         * }</pre>
         *
         * @return The constructed BiMap instance.
         */
        public BiMap<K, V> build() {
            return biMap;
        }
    }
}
