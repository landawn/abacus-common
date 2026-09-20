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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * A collection that maps keys to multiple values, similar to {@link Map} but allowing each key
 * to be associated with a collection of values rather than a single value. This sealed class
 * provides a comprehensive implementation for managing key-to-collection mappings with efficient
 * operations for adding, removing, and querying multiple values per key.
 *
 * <p>Unlike a standard Map where each key maps to exactly one value, a Multimap allows:</p>
 * <ul>
 *   <li>Multiple values per key: {@code key1 -> [value1, value2, value3]}</li>
 *   <li>Absent keys return {@code null} from {@link #get(Object)}</li>
 *   <li>Different collection types for values (List, Set, etc.)</li>
 * </ul>
 *
 * <p><b>Key lifecycle:</b> this class's own mutators do not retain empty value collections - a key whose
 * last value is removed is removed with it, so {@link #get(Object)} returns {@code null} for it. That
 * invariant holds only for mutations made <i>through</i> this class. Value collections are handed out live
 * ({@link #get(Object)}, {@link #getOrDefault(Object, Collection)}, {@link #valueCollections()},
 * {@link #iterator()}, {@link #stream()}), and a {@code wrap} factory keeps using a backing map the caller
 * may still hold, so a caller can leave behind a key mapped to an empty collection. {@link #get(Object)}
 * documents exactly what such a key does to the other operations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Flexible Value Storage:</b> Supports any Collection type for storing values</li>
 *   <li><b>Type Safety:</b> Strongly typed with separate key and element type parameters</li>
 *   <li><b>Efficient Operations:</b> Optimized for common multimap operations like put, get, remove</li>
 *   <li><b>Collection Integration:</b> Seamless integration with Java Collections Framework</li>
 *   <li><b>Stream Support:</b> Full support for stream operations and functional programming</li>
 *   <li><b>Customizable Backing:</b> Configurable backing Map and Collection implementations</li>
 *   <li><b>Thread Safety:</b> Not thread-safe; external synchronization is required when an instance is shared and mutated</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Sealed Class:</b>
 * <ul>
 *   <li>This is a <b>sealed class</b> that only permits {@link ListMultimap} and {@link SetMultimap}</li>
 *   <li>Use factory methods in {@link N} class to create instances</li>
 *   <li>Cannot be extended by classes outside this package</li>
 *   <li>Provides controlled inheritance for type safety and API consistency</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Grouping Data:</b> Grouping objects by categories, types, or attributes</li>
 *   <li><b>Index Structures:</b> Creating indexes where keys map to multiple related items</li>
 *   <li><b>Graph Representations:</b> Representing adjacency lists in graphs</li>
 *   <li><b>Configuration Management:</b> Managing configuration entries with multiple values</li>
 *   <li><b>Data Processing:</b> Collecting and organizing related data items</li>
 *   <li><b>Caching:</b> Caching multiple results per key</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating different types of multimaps
 * ListMultimap<String, Integer> listMultimap = N.newListMultimap();
 * SetMultimap<String, String> setMultimap = N.newSetMultimap();
 *
 * // Basic operations
 * listMultimap.put("scores", 85);
 * listMultimap.put("scores", 92);
 * listMultimap.put("scores", 78);
 * List<Integer> scores = listMultimap.get("scores");   // returns [85, 92, 78]
 *
 * // Bulk operations
 * listMultimap.putValues("grades", Arrays.asList(90, 85, 88));
 * listMultimap.removeValues("scores", Arrays.asList(78, 92));
 *
 * // Functional operations
 * listMultimap.stream()
 *     .filter(entry -> entry.getValue().size() > 1)
 *     .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
 *
 * // Conversion operations
 * Map<String, List<Integer>> map = listMultimap.toMap();
 * Multiset<String> keyFrequency = listMultimap.toMultiset();
 *
 * // Statistical analysis
 * int totalValues = listMultimap.totalValueCount();
 * boolean hasData = !listMultimap.isEmpty();
 * }</pre>
 *
 * <p><b>Specialized Implementations:</b>
 * <ul>
 *   <li><b>{@link ListMultimap}:</b> Values stored in List collections, preserving order and allowing duplicates</li>
 *   <li><b>{@link SetMultimap}:</b> Values stored in Set collections, ensuring uniqueness per key</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * Use the {@link N} utility class for creating Multimap instances:
 * <ul>
 *   <li>{@code N.newListMultimap()} - Creates ListMultimap with HashMap+ArrayList</li>
 *   <li>{@code N.newSetMultimap()} - Creates SetMultimap with HashMap+HashSet</li>
 *   <li>{@code N.newListMultimap(Class, Class)} - Custom backing types</li>
 *   <li>{@code N.newMultimap(Supplier, Supplier)} - Custom suppliers</li>
 * </ul>
 *
 * <p><b>Collection Types:</b>
 * The value collection type V determines behavior:
 * <ul>
 *   <li><b>List:</b> Preserves insertion order, allows duplicates</li>
 *   <li><b>Set:</b> No duplicates, no guaranteed order (unless specialized Set)</li>
 *   <li><b>Queue/Deque:</b> Specialized ordering for queue-based operations</li>
 *   <li><b>SortedSet:</b> Maintains sorted order of values</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Backed by Map for key storage - performance depends on Map implementation</li>
 *   <li>Value collection performance depends on Collection implementation</li>
 *   <li>HashMap backing: O(1) average time for key operations</li>
 *   <li>TreeMap backing: O(log n) operations but sorted key iteration</li>
 *   <li>Memory usage: O(k + v) where k is keys and v is total values</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * This class is not thread-safe, regardless of the backing implementations. A concurrent map and
 * thread-safe value collections do not make compound operations atomic: for example, two writers
 * can both observe an absent key, create separate value collections, and overwrite one another.
 * If any thread mutates an instance, synchronize all accesses externally.
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs Map&lt;K,Collection&lt;E&gt;&gt;:</b> Automatic collection creation, specialized operations</li>
 *   <li><b>vs Google Guava Multimap:</b> Similar API with additional utility methods</li>
 *   <li><b>vs Apache Commons MultiValuedMap:</b> Different API design, comparable functionality</li>
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
 *     <td>{@link BiMap}</td>
 *     <td>one key &harr; one value (both sides unique; invertible)</td>
 *     <td>{@code {a=1, b=2}} with inverse {@code {1=a, 2=b}}</td>
 *     <td>you must look up by value as well as by key, and values are unique</td>
 *   </tr>
 *   <tr>
 *     <td>{@code Multimap} ({@link ListMultimap} / {@link SetMultimap})</td>
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
 * <p><b>Attribution:</b>
 * This class may include code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @param <K> the type of keys maintained by this multimap
 * @param <E> the type of individual elements stored in the value collections
 * @param <V> the type of collection used to store values (must extend Collection&lt;E&gt;)
 *
 * @see N#newMultimap(Supplier, Supplier)
 * @see N#newListMultimap()
 * @see N#newListMultimap(Class, Class)
 * @see N#newListMultimap(Supplier, Supplier)
 * @see N#newSetMultimap()
 * @see N#newSetMultimap(Class, Class)
 * @see N#newSetMultimap(Supplier, Supplier)
 * @see ListMultimap
 * @see SetMultimap
 * @see Map
 * @see Collection
 * @see Stream
 * @see EntryStream
 * @see Multiset
 */
public sealed class Multimap<K, E, V extends Collection<E>> implements Iterable<Map.Entry<K, V>> permits ListMultimap, SetMultimap {

    /** Creates the backing map; also reused by copy-producing operations such as {@link #copy()} and {@link #toMultiset()}. */
    final Supplier<? extends Map<K, V>> mapSupplier;

    /** Creates a fresh, empty value collection each time a key gains its first value. */
    final Supplier<? extends V> valueSupplier;

    /** The map that actually stores the key-to-value-collection mappings. Never {@code null}. */
    final Map<K, V> backingMap;

    private transient Collection<E> values;

    private transient Set<Map.Entry<K, V>> entries;

    /**
     * Constructs a Multimap with a {@link HashMap} backing map and {@link java.util.ArrayList} value
     * collections.
     *
     * <p>Because the value collections are {@code ArrayList}s, {@code V} must be a type an {@code ArrayList}
     * can be assigned to - {@code List<E>} or {@code Collection<E>}. Both permitted subclasses declare their
     * own no-arg constructor with the right value type, so this one is only reached by instantiating the base
     * class directly; prefer {@link N#newMultimap(Supplier, Supplier)}, which makes the value type explicit.</p>
     */
    Multimap() {
        this(HashMap.class, ArrayList.class);
    }

    // There is deliberately no Multimap(int) constructor. One existed, nothing in the tree called it, and it
    // forced `(Supplier<V>) Suppliers.ofList()` regardless of V - so any subclass that had ever picked it up
    // (SetMultimap, say) would have been handed ArrayList values and failed with a ClassCastException on the
    // first get(). The no-arg constructor above carries the same ArrayList assumption but is documented, is
    // actually used, and is not a sizing overload anyone would reach for by analogy.

    /**
     * Constructs a Multimap with the specified map type and collection type.
     *
     * <p>This constructor initializes a Multimap with a map of the specified type for the backing map
     * and a collection of the specified type for the value collection.</p>
     *
     * @param mapType the class of the map to be used as the backing map
     * @param valueType the class of the collection to be used as the value collection
     */
    @SuppressWarnings("rawtypes")
    Multimap(final Class<? extends Map> mapType, final Class<? extends Collection> valueType) {
        this(Suppliers.ofMap(mapType), valueTypeToSupplier(valueType));
    }

    /**
     * Constructs a Multimap with the specified map supplier and value supplier.
     *
     * <p>This constructor initializes a Multimap with a map provided by the specified supplier for the backing map
     * and a collection provided by the specified supplier for the value collection.</p>
     *
     * @param mapSupplier the supplier of the map to be used as the backing map; invoked once during
     *                    construction, and again by {@link #copy()}, so it must return a new empty map
     *                    on every call
     * @param valueSupplier the supplier that creates a new value collection for each key; it must return a
     *                      new empty collection on every call
     * @throws IllegalArgumentException if either supplier is {@code null}, or if {@code mapSupplier}
     *         returns {@code null} or a non-empty map
     */
    Multimap(final Supplier<? extends Map<K, V>> mapSupplier, final Supplier<? extends V> valueSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);
        N.checkArgNotNull(valueSupplier, cs.valueSupplier);

        final Map<K, V> suppliedMap = N.checkArgNotNull(mapSupplier.get(), "mapSupplier.get()");

        if (!suppliedMap.isEmpty()) {
            throw new IllegalArgumentException("The supplied map must be empty");
        }

        this.mapSupplier = mapSupplier;
        this.valueSupplier = valueSupplier;
        backingMap = suppliedMap;
    }

    /**
     * Constructs a Multimap with the specified map and value supplier.
     *
     * <p>This constructor initializes a Multimap with the provided map as the backing map
     * and a collection provided by the specified supplier for the value collection.</p>
     *
     * @param valueMap the map to be used directly as the backing map (not copied)
     * @param valueSupplier the supplier that creates a new value collection for each key
     * @implNote Copy-producing operations create a compatible empty map from {@code valueMap};
     *           if its runtime wrapper type cannot be instantiated, they fall back to a HashMap.
     */
    @Internal
    @SuppressWarnings("unchecked")
    Multimap(final Map<K, V> valueMap, final Supplier<? extends V> valueSupplier) {
        mapSupplier = () -> Maps.newTargetMap(valueMap);
        this.valueSupplier = valueSupplier;
        backingMap = valueMap;
    }

    /**
     * Converts the provided collection class type into a {@link Supplier} that creates instances of that type.
     *
     * <p>This method takes a class type that extends {@link Collection} and returns a {@code Supplier}
     * that dynamically creates new instances of the specified collection type on each call.</p>
     *
     * @param valueType the class type that extends {@link Collection}
     * @return a {@code Supplier} that creates new instances of the specified collection type
     */
    @SuppressWarnings("rawtypes")
    static Supplier valueTypeToSupplier(final Class<? extends Collection> valueType) {
        return Suppliers.ofCollection(valueType);
    }

    /**
     * Returns the collection of values associated with the specified key.
     *
     * <p><b>IMPORTANT - Null vs Empty Behavior:</b></p>
     * <ul>
     *   <li>If the key has never been added: returns {@code null}</li>
     *   <li>If all values for the key have been removed: returns {@code null} (key is removed from map)</li>
     *   <li>If the key exists with values: returns its stored collection</li>
     * </ul>
     *
     * <p><b>This is DIFFERENT from Guava's Multimap</b> which returns an empty collection
     * for absent keys. This implementation returns {@code null} for absent keys.</p>
     *
     * <p><b>Key Lifecycle:</b></p>
     * <ul>
     *   <li>After {@code put(key, value)}: key exists, {@code get(key)} returns collection containing the value</li>
     *   <li>After removing ALL values: key is automatically removed, {@code get(key)} returns {@code null}</li>
     *   <li>After {@code removeAll(key)}: key is removed, {@code get(key)} returns {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> map = N.newListMultimap();
     * map.put("a", 1);
     * map.put("a", 2);
     *
     * List<Integer> values = map.get("a");
     * // values = [1, 2] - not null
     *
     * map.removeEntry("a", 1);
     * map.removeEntry("a", 2);
     * // All values removed - key is removed from map
     *
     * List<Integer> empty = map.get("a");
     * // empty = null - NOT empty collection!
     *
     * List<Integer> never = map.get("nonexistent");
     * // never = null
     * }</pre>
     *
     * <br />
     * The returned collection is backed by the Multimap, so changes to the returned collection are reflected in the Multimap.
     *
     * <p><b>Mutating the returned collection bypasses this class's key lifecycle.</b> The mapping is not
     * removed when the collection becomes empty, so the key stays present with an empty collection: after
     * {@code get(key).clear()}, {@code containsKey(key)} still returns {@code true} and {@code get(key)}
     * returns an empty collection rather than {@code null}. {@link #keyCount()} then counts a key that
     * contributes nothing to {@link #totalValueCount()}, and {@link #toMultiset()} omits it (a zero count
     * is not representable in a {@link Multiset}). {@link #copy()} does reproduce such a key faithfully.
     * The same applies to collections obtained from {@link #getOrDefault(Object, Collection)},
     * {@link #valueCollections()}, {@link #iterator()}, {@link #stream()} and to an externally retained
     * backing map passed to a {@code wrap} factory. Prefer this class's own mutators - {@link #put(Object, Object)},
     * {@link #removeEntry(Object, Object)}, {@link #removeAll(Object)} - which maintain the invariant.</p>
     *
     * @param key the key whose associated values are to be returned
     * @return the stored collection of values for the key, or {@code null} if the key is not present;
     *         its supported mutations depend on the configured or wrapped collection
     * @see #containsKey(Object)
     * @see #getOrDefault(Object, Collection)
     */
    public V get(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.get(key);
    }

    /**
     * Returns the value collection associated with the specified key in the Multimap, or the provided default value if no value is found.
     *
     * <p>When the key is present, the returned collection is backed by the Multimap, so changes to the returned collection are reflected in the Multimap.
     * Usually, the returned collection should not be modified outside this Multimap directly, because it may cause unexpected behavior.
     * When the key is absent, the provided default value is returned as-is (it is not stored in the Multimap).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("key1", 1);
     * List<Integer> defaultList = new ArrayList<>();
     * List<Integer> values = multimap.getOrDefault("key2", defaultList);
     * // values == defaultList since "key2" doesn't exist
     * }</pre>
     *
     * @param key the key whose associated value collection is to be returned
     * @param defaultValue the default value to return if no value is associated with the key
     * @return the value collection associated with the specified key, or the default value if the key is not present in the Multimap
     * @see #get(Object)
     */
    public V getOrDefault(final Object key, final V defaultValue) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V value = backingMap.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    /**
     * Associates the specified value with the specified key in this Multimap.
     * Unlike a regular Map, this method adds the value to the collection of values
     * associated with the key rather than replacing any existing value.
     *
     * <p>If the key is not already present in the Multimap, a new collection is created
     * for the key using the configured value supplier, and the value is added to it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.put("fruits", "apple");
     * multimap.put("fruits", "banana");       // "fruits" now maps to ["apple", "banana"]
     * multimap.put("vegetables", "carrot");   // adds new key "vegetables" -> ["carrot"]
     * }</pre>
     *
     * <p><b>Thread Safety:</b> This operation is not thread-safe. External synchronization
     * is required for concurrent access.</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @return {@code true} if the value was successfully added to the collection,
     *         {@code false} if the collection does not permit duplicates and already contains the value
     * @see #putIfValueAbsent(Object, Object)
     * @see #putValues(Object, Collection)
     */
    public boolean put(final K key, final E e) {
        return addOne(key, e);
    }

    /**
     * Adds one element under {@code key}, creating the value collection if the key is absent, and returns
     * whether the collection accepted it. Shared by {@link #put(Object, Object)},
     * {@link #putAll(Map)} and {@link #putIfValueAbsent(Object, Object)}.
     *
     * <p>The mapping is retained only after a successful add, and an already-present but <i>empty</i>
     * collection that refuses the add is dropped, so this class's mutators never leave an empty value
     * collection behind. (An empty collection can only be reached through a live collection handed out by
     * {@link #get(Object)} or through an externally retained wrapped backing map.)</p>
     *
     * @param key the key to add under
     * @param e the element to add
     * @return {@code true} if the value collection accepted {@code e}
     */
    private boolean addOne(final K key, final E e) {
        return addOne(key, backingMap.get(key), e);
    }

    /**
     * {@link #addOne(Object, Object)} for a caller that has already looked {@code key} up, so the lookup is
     * not repeated.
     *
     * @param key the key to add under
     * @param val the value collection currently mapped to {@code key}, or {@code null} if the key is absent
     * @param e the element to add
     * @return {@code true} if the value collection accepted {@code e}
     */
    private boolean addOne(final K key, final V val, final E e) {
        if (val == null) {
            final V newVal = valueSupplier.get();

            if (newVal.add(e)) {
                backingMap.put(key, newVal);
                return true;
            }

            return false;
        }

        final boolean added = val.add(e);

        if (!added && val.isEmpty()) {
            backingMap.remove(key);
        }

        return added;
    }

    /**
     * Adds every element of {@code c} under {@code key}, creating the value collection if the key is absent,
     * and returns whether the collection accepted any of them. Shared by
     * {@link #putValues(Object, Collection)}, {@link #putValues(Map)} and {@link #putValues(Multimap)}.
     *
     * <p>Callers must have established that {@code c} is non-empty. Key lifecycle is maintained exactly as in
     * {@link #addOne(Object, Object)}.</p>
     *
     * @param key the key to add under
     * @param c the non-empty collection of elements to add
     * @return {@code true} if the value collection accepted at least one element of {@code c}
     */
    private boolean addMany(final K key, final Collection<? extends E> c) {
        final V val = backingMap.get(key);

        if (val == null) {
            final V newVal = valueSupplier.get();

            if (newVal.addAll(c)) {
                backingMap.put(key, newVal);
                return true;
            }

            return false;
        }

        final boolean added = val.addAll(c);

        if (!added && val.isEmpty()) {
            backingMap.remove(key);
        }

        return added;
    }

    /**
     * Associates all the specified keys and values from the provided map to this Multimap.
     *
     * <p>This method iterates over the provided map and for each entry, it associates the key with the value in this Multimap.
     * If the Multimap previously contained mappings for a key, the new value is added to the collection of values associated with this key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("a", 1);
     * Map<String, Integer> map = Map.of("a", 3, "b", 2);
     * multimap.putAll(map);
     * // multimap now contains: {a=[1, 3], b=[2]}
     * }</pre>
     *
     * @param m the map whose keys and values are to be added to this Multimap
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise
     * @see #put(Object, Object)
     * @see #putValues(Object, Collection)
     */
    public boolean putAll(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            wasModified |= addOne(e.getKey(), e.getValue());
        }

        return wasModified;
    }

    /**
     * Associates the specified value with the specified key only if the value is not already present.
     * This method provides a way to add a value to a key's collection while avoiding duplicates.
     *
     * <p>The method behaves as follows:</p>
     * <ul>
     *   <li>If the key is not present, creates a new collection and adds the value</li>
     *   <li>If the key is present but the value is not in its collection, adds the value</li>
     *   <li>If the key is present and the value already exists in its collection, does nothing</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SetMultimap<String, Integer> multimap = N.newSetMultimap();
     * multimap.putIfValueAbsent("numbers", 1);   // returns true, adds 1
     * multimap.putIfValueAbsent("numbers", 1);   // returns false, 1 already exists
     * multimap.putIfValueAbsent("numbers", 2);   // returns true, adds 2
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key if not already present
     * @return {@code true} if the value was added (either to a new or existing collection),
     *         {@code false} if the value already exists in the key's collection
     * @see #putIfKeyAbsent(Object, Object)
     * @see #put(Object, Object)
     */
    public boolean putIfValueAbsent(final K key, final E e) {
        final V val = backingMap.get(key);

        if (val != null && val.contains(e)) {
            return false;
        }

        // Pass `val` through rather than calling addOne(key, e): the lookup has already been paid for here,
        // and repeating it would cost an extra O(log n) probe on a sorted backing map.
        return addOne(key, val, e);
    }

    /**
     * Associates the specified value with the specified key only if the key is not already present.
     * This method is useful when you want to ensure a key is only initialized once, regardless
     * of how many values might be added to it later.
     *
     * <p>Unlike {@link #putIfValueAbsent(Object, Object)}, this method only checks for key presence,
     * not whether the specific value already exists in the collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putIfKeyAbsent("new-key", "value1");   // returns true, creates new key
     * multimap.putIfKeyAbsent("new-key", "value2");   // returns false, key exists
     * multimap.put("new-key", "value2");              // returns true; can still add more values
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @return {@code true} if the key was not present and a new mapping was created,
     *         {@code false} if the key was already present (value is not added)
     * @see #putIfValueAbsent(Object, Object)
     * @see #put(Object, Object)
     */
    public boolean putIfKeyAbsent(final K key, final E e) {
        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            if (val.add(e)) {
                backingMap.put(key, val);
                return true;
            }
            return false;
        }

        return false;
    }

    /**
     * Associates all values from the provided collection with the specified key in this Multimap.
     * This is a bulk operation that adds multiple values to a key at once.
     *
     * <p>If the key already exists, the values are added to its existing collection.
     * If the key doesn't exist, a new collection is created and populated with the values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("evens", 2);
     * multimap.putValues("evens", Arrays.asList(4, 6, 8));   // "evens" -> [2, 4, 6, 8]
     * multimap.putValues("odds", Arrays.asList(1, 3, 5));    // "odds" -> [1, 3, 5]
     * }</pre>
     *
     * <p><b>Note:</b> The behavior depends on the underlying collection type. For ListMultimap,
     * duplicates are allowed. For SetMultimap, duplicate values are automatically filtered out.</p>
     *
     * @param key the key with which the specified values are to be associated
     * @param c the collection of values to be associated with the specified key
     * @return {@code true} if any values were added to the Multimap,
     *         {@code false} if the collection was empty or no values were added
     * @see #put(Object, Object)
     * @see #putValuesIfKeyAbsent(Object, Collection)
     * @see Collection#addAll(Collection)
     */
    public boolean putValues(final K key, final Collection<? extends E> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return addMany(key, c);
    }

    /**
     * Associates all the specified values from the provided collection with the specified key in this Multimap if the key is not already present.
     * If the key is already present, this method does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValuesIfKeyAbsent("key1", Arrays.asList(1, 2, 3));   // returns true
     * multimap.putValuesIfKeyAbsent("key1", Arrays.asList(4, 5));      // returns false, key exists
     * // multimap contains: {key1=[1, 2, 3]}
     * }</pre>
     *
     * @param key the key with which the specified values are to be associated
     * @param c the collection of values to be associated with the specified key
     * @return {@code true} if the key was not already present and the association was successfully added, {@code false} if the key is already present or the specified value collection is empty
     * @see #putIfKeyAbsent(Object, Object)
     * @see #putValues(Object, Collection)
     */
    public boolean putValuesIfKeyAbsent(final K key, final Collection<? extends E> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        V val = backingMap.get(key);

        if (val == null) {
            val = valueSupplier.get();
            final boolean added = val.addAll(c);

            if (added) {
                backingMap.put(key, val);
            }

            return added;
        }

        return false;
    }

    /**
     * Adds all key-to-collection mappings from the specified map to this Multimap.
     *
     * <p>For each entry in the provided map, all values in the entry's collection are associated
     * with the corresponding key in this Multimap. If a key already exists, the new values are
     * appended to its existing collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * Map<String, List<Integer>> map = Map.of(
     *     "key1", Arrays.asList(1, 2),
     *     "key2", Arrays.asList(3, 4));
     * multimap.putValues(map);
     * // multimap now contains: {key1=[1, 2], key2=[3, 4]}
     * }</pre>
     *
     * <p>An entry of {@code m} whose value collection is {@code null} or empty is skipped: it does not
     * create a key here. (This is why {@link #copy()} does not go through this method - a copy must
     * reproduce a key that is mapped to an empty collection.)</p>
     *
     * @param m the map whose keys and collections of values are to be added to this Multimap; entries with
     *          a {@code null} or empty value collection are skipped
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise
     * @see #putAll(Map)
     * @see #putValues(Object, Collection)
     */
    public boolean putValues(final Map<? extends K, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;

        for (final Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.entrySet()) {
            if (N.isEmpty(e.getValue())) {
                continue;
            }

            wasModified |= addMany(e.getKey(), e.getValue());
        }

        return wasModified;
    }

    /**
     * Adds all key-to-collection mappings from the specified Multimap to this Multimap.
     *
     * <p>For each key in the provided Multimap, all of its associated values are appended to
     * this Multimap's collection for that key. If a key already exists in this Multimap,
     * the new values are added to the existing collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap1 = N.newListMultimap();
     * multimap1.putValues("key1", Arrays.asList(1, 2));
     *
     * ListMultimap<String, Integer> multimap2 = N.newListMultimap();
     * multimap2.putValues("key1", Arrays.asList(3, 4));
     * multimap2.putValues("key2", Arrays.asList(5, 6));
     *
     * multimap1.putValues(multimap2);
     * // multimap1 now contains: {key1=[1, 2, 3, 4], key2=[5, 6]}
     * }</pre>
     *
     * <p>A key of {@code m} whose value collection is empty is skipped: it does not create a key here.
     * (This is why {@link #copy()} does not go through this method - a copy must reproduce a key that is
     * mapped to an empty collection.)</p>
     *
     * @param m the Multimap whose keys and collections of values are to be added to this Multimap; keys
     *          mapped to an empty collection are skipped
     * @return {@code true} if the operation modifies the Multimap, {@code false} otherwise
     * @see #putAll(Map)
     * @see #putValues(Map)
     */
    public boolean putValues(final Multimap<? extends K, ? extends E, ? extends Collection<? extends E>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;

        // Iterate the source's entries rather than keySet() + get(key): the latter costs a second lookup
        // per key, which is O(log n) rather than free for a sorted backing map.
        for (final Map.Entry<? extends K, ? extends Collection<? extends E>> e : m.backingMap.entrySet()) {
            if (N.isEmpty(e.getValue())) {
                continue;
            }

            wasModified |= addMany(e.getKey(), e.getValue());
        }

        return wasModified;
    }

    /**
     * Removes a single occurrence of the specified value from the collection associated with the given key.
     * This method is useful for removing specific key-value pairs from the Multimap.
     *
     * <p>If the removal leaves the key with an empty collection, the key itself is removed
     * from the Multimap to maintain consistency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 1);
     * multimap.put("numbers", 2);
     * multimap.put("numbers", 1);
     *
     * multimap.removeEntry("numbers", 1);   // Removes first occurrence of 1
     * // "numbers" now maps to [2, 1]
     *
     * multimap.removeEntry("numbers", 3);   // returns false, 3 not found
     * }</pre>
     *
     * @param key the key whose associated collection is to be modified
     * @param e the element to be removed from the collection
     * @return {@code true} if the element was found and removed,
     *         {@code false} if the key was not found or the element was not in the collection
     * @see #removeAll(Object)
     * @see #removeValues(Object, Collection)
     */
    public boolean removeEntry(final Object key, final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        if (val != null && val.remove(e)) {
            if (val.isEmpty()) {
                //noinspection SuspiciousMethodCalls
                backingMap.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes a single occurrence of each specified key-value pair from this Multimap.
     *
     * <p>This method iterates over the provided map and for each entry, it attempts to remove a single occurrence of the key-value pair from this Multimap.
     * If the key-value pair is successfully removed and the collection of values associated with the key becomes empty as a result, the key is also removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("key1", Arrays.asList(1, 2, 3, 2));
     * multimap.putValues("key2", Arrays.asList(4, 5));
     *
     * Map<String, Integer> toRemove = Map.of("key1", 2, "key2", 4);
     * multimap.removeEntries(toRemove);
     * // multimap now contains: {key1=[1, 3, 2], key2=[5]}
     * }</pre>
     *
     * @param m the map whose key-value pairs are to be removed from this Multimap
     * @return {@code true} if at least one key-value pair was successfully removed from the Multimap, {@code false} otherwise
     * @see #removeEntry(Object, Object)
     * @see #removeValues(Object, Collection)
     */
    public boolean removeEntries(final Map<? extends K, ? extends E> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        boolean wasModified = false;
        Object key = null;
        V val = null;

        for (final Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            key = e.getKey();
            //noinspection SuspiciousMethodCalls
            val = backingMap.get(key);

            if (N.notEmpty(val)) {
                wasModified |= val.remove(e.getValue());

                if (val.isEmpty()) {
                    //noinspection SuspiciousMethodCalls
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes all values associated with the specified key from this Multimap.
     * This is equivalent to removing the entire mapping for the key.
     *
     * <p>After this operation, the key will no longer exist in the Multimap,
     * and any subsequent {@link #get(Object)} calls with this key will return {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putValues("colors", Arrays.asList("red", "blue", "green"));
     *
     * Collection<String> removed = multimap.removeAll("colors");
     * // removed contains ["red", "blue", "green"]
     * // multimap.containsKey("colors") returns false
     * }</pre>
     *
     * <p><b>Note:</b> The returned collection is the actual collection that was stored
     * in the Multimap, not a copy. The mapping is removed, but other references to that collection
     * still observe changes to it.</p>
     *
     * @param key the key whose entire mapping is to be removed
     * @return the collection of values that were associated with the key,
     *         or {@code null} if the key was not present in the Multimap
     * @see #removeEntry(Object, Object)
     * @see #clear()
     */
    public V removeAll(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.remove(key);
    }

    /**
     * Removes all occurrences of the specified elements from the collection of values associated with the specified key in this Multimap.
     *
     * <p>This method retrieves the collection of values associated with the specified key and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("numbers", Arrays.asList(1, 2, 3, 2, 4, 2));
     *
     * multimap.removeValues("numbers", Arrays.asList(2, 3));
     * // multimap now contains: {numbers=[1, 4]}
     * }</pre>
     *
     * <p>Membership is determined by {@code valuesToRemove.contains(value)} before any values are removed.
     * The argument may therefore be a live value collection or a sublist backed by this Multimap;
     * its comparator or identity-based membership rules are preserved. A stored value that
     * {@code valuesToRemove} is not allowed to be asked about - a {@code null} in a null-hostile collection such
     * as {@code List.of(...)}, or a foreign type in a {@link java.util.TreeSet} - counts as not a member rather
     * than propagating the {@link NullPointerException} or {@link ClassCastException} that
     * {@link Collection#contains(Object)} is permitted to throw for such an element.</p>
     *
     * <p>This is the rule the bulk removals share: {@link #removeValues(Map)}, {@link #removeValues(Multimap)}
     * and both {@code removeValuesIf} overloads also decide membership with the supplied collection's
     * {@code contains}. The single-value methods - {@link #removeEntry(Object, Object)},
     * {@link #removeEntries(Map)}, {@code removeEntriesIf}, {@code replaceEntry} and the query
     * {@link #containsEntry(Object, Object)} - use the value collection's own equivalence instead, so the two
     * families can disagree: for a value set keyed by a comparator inconsistent with {@code equals},
     * {@code containsEntry(key, value)} can report {@code true} for a value that
     * {@code removeValues(key, List.of(value))} does not remove.</p>
     *
     * @param key the key whose associated collection of values is to be processed
     * @param valuesToRemove the collection of elements to be removed from the collection of values associated with the specified key
     * @return {@code true} if at least one element was successfully removed from the collection of values associated with the specified key, {@code false} otherwise
     * @implNote {@code valuesToRemove.contains} is invoked once per value currently stored under the key, so the
     *           cost scales with the number of stored values rather than with the number of values being removed.
     * @see #removeEntry(Object, Object)
     * @see #removeValues(Map)
     * @see #removeValues(Multimap)
     */
    public boolean removeValues(final Object key, final Collection<?> valuesToRemove) {
        if (N.isEmpty(valuesToRemove)) {
            return false;
        }

        boolean wasModified = false;
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        if (N.notEmpty(val)) {
            // Ask the ARGUMENT what it contains, then remove those very objects by identity - the same staging
            // removeValues(Map) and removeValuesFromKeys use. For a Set value collection, Collection.removeAll
            // would instead pick between the argument's and the target's equivalence according to their relative
            // sizes (AbstractSet.removeAll's heuristic), answering the same removal question two different ways.
            // A List value collection already consulted the argument, but ArrayList.removeAll compacts in place
            // while an argument that is a sublist view of that same list keeps reading it, so values the argument
            // never matched were silently dropped.
            final Set<E> matchingValues = new IdentityHashSet<>();

            for (final E value : val) {
                if (containsSafely(valuesToRemove, value)) {
                    matchingValues.add(value);
                }
            }

            wasModified = !matchingValues.isEmpty() && val.removeIf(matchingValues::contains);

            if (val.isEmpty()) {
                //noinspection SuspiciousMethodCalls
                backingMap.remove(key);
            }
        }

        return wasModified;
    }

    /**
     * Answers {@code c.contains(value)}, treating the {@link NullPointerException} and {@link ClassCastException}
     * that {@link Collection#contains(Object)} is permitted to throw for an element the collection cannot hold as
     * a negative answer. {@code AbstractSet.removeAll} suppressed the same two exceptions whenever it chose to
     * iterate the argument, so answering rather than throwing is what callers of a bulk removal saw.
     *
     * @param c the collection whose membership decides the answer
     * @param value the value to test for membership
     * @return {@code true} if {@code c} reports {@code value} as a member
     */
    private static boolean containsSafely(final Collection<?> c, final Object value) {
        try {
            return c.contains(value);
        } catch (final NullPointerException | ClassCastException e) {
            return false;
        }
    }

    /**
     * Removes all occurrences of the specified elements from their corresponding key collections in this Multimap.
     *
     * <p>For each entry in the provided map, all occurrences of the entry's values are removed from
     * the collection associated with the entry's key in this Multimap. Keys whose collections become
     * empty after removal are automatically removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> listMultimap = ListMultimap.of("a", 1, "b", 2, "a", 2, "a", 2);
     * listMultimap.removeValues(N.asMap("a", N.asList(2)));
     * // listMultimap now contains: {"a": [1], "b": [2]}
     * }</pre>
     *
     * <p>{@code m} may be this multimap's own backing map, or a view of it - such as the map handed to a
     * {@code wrap} factory and still held by the caller. Removing every value of every key that way empties
     * this multimap rather than throwing {@link java.util.ConcurrentModificationException}.</p>
     * <p>Removal collections may also be live value collections or sublists shared across keys. Membership
     * is evaluated with each supplied collection's {@code contains} method before any target collection changes.</p>
     *
     * @param m the map whose keys and collections of elements are to be removed from this Multimap
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the specified keys, {@code false} otherwise
     * @implNote Keys and matching target objects are staged before mutation. Identity sets record the objects
     *           selected by each removal collection's membership rules, preserving comparator and identity semantics
     *           even when removal collections are backed by a target collection that changes earlier in the operation.
     * @see #removeValues(Object, Collection)
     * @see #removeValues(Multimap)
     */
    public boolean removeValues(final Map<?, ? extends Collection<?>> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        // A removal collection may be a live bucket or subList shared by several keys. Stage every
        // membership decision before changing a bucket, retaining each collection's contains semantics.
        final List<Object> keys = new ArrayList<>(m.size());
        final List<Set<E>> removals = new ArrayList<>(m.size());

        for (final Map.Entry<?, ? extends Collection<?>> e : m.entrySet()) {
            keys.add(e.getKey());
            final Set<E> matchingValues = new IdentityHashSet<>();
            final Collection<?> elements = e.getValue();
            final V values = backingMap.get(e.getKey());

            if (N.notEmpty(values) && N.notEmpty(elements)) {
                for (final E value : values) {
                    // containsSafely, not contains - see removeValues(Object, Collection).
                    if (containsSafely(elements, value)) {
                        matchingValues.add(value);
                    }
                }
            }

            removals.add(matchingValues);
        }

        boolean wasModified = false;
        Object key = null;
        V val = null;

        for (int i = 0, size = keys.size(); i < size; i++) {
            key = keys.get(i);
            final Set<E> matchingValues = removals.get(i);
            //noinspection SuspiciousMethodCalls
            val = backingMap.get(key);

            if (val != null && !matchingValues.isEmpty()) {
                wasModified |= val.removeIf(matchingValues::contains);

                if (val.isEmpty()) {
                    //noinspection SuspiciousMethodCalls
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified elements from their corresponding key collections in this Multimap.
     *
     * <p>For each key in the provided Multimap, all occurrences of its associated values are removed from
     * the collection mapped to the same key in this Multimap. Keys whose collections become empty after
     * removal are automatically removed from this Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap1 = N.newListMultimap();
     * multimap1.putValues("key1", Arrays.asList(1, 2, 3, 2));
     * multimap1.putValues("key2", Arrays.asList(4, 5));
     *
     * ListMultimap<String, Integer> multimap2 = N.newListMultimap();
     * multimap2.putValues("key1", Arrays.asList(2, 3));
     *
     * multimap1.removeValues(multimap2);
     * // multimap1 now contains: {key1=[1], key2=[4, 5]}
     * }</pre>
     *
     * <p>{@code m} may be a distinct multimap wrapping this multimap's backing map; that case empties this
     * multimap rather than throwing {@link java.util.ConcurrentModificationException}. Passing this very
     * instance is also accepted: every value is removed, which empties this multimap except for a key already
     * mapped to an empty value collection - such a key has nothing to remove, so it is kept and does not
     * contribute to the returned {@code boolean}. For that self case the value collections are dropped rather
     * than emptied, so a caller still holding one of them - through the map handed to a {@code wrap} factory, say
     * - sees its contents unchanged.</p>
     *
     * @param m the Multimap whose keys and collections of elements are to be removed from this Multimap
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the specified keys, {@code false} otherwise
     * @implNote Delegates to {@link #removeValues(Map)}, which stages keys and matching target objects before
     *           any mutation. This also supports live removal collections and sublists shared across keys.
     *           Passing this very instance is handled directly instead, in a single pass over the entry set, in
     *           time proportional to the number of keys.
     * @see #removeValues(Object, Collection)
     * @see #removeValues(Map)
     */
    public boolean removeValues(final Multimap<?, ?, ?> m) {
        if (N.isEmpty(m)) {
            return false;
        }

        if (m == this) {
            // Self-removal is answered per key, not per value: every stored value matches, so routing this case
            // through removeValues(Map) would consult the value collection's own contains once per stored value -
            // quadratic in the values held by a key. Drop each non-empty value collection instead, without
            // emptying it, so a fixed-size or immutable collection from a wrap factory is never asked to remove
            // anything. clear() is not the shortcut either: it would report true even when nothing was removed,
            // and would drop a key whose value collection is already empty - such a key has nothing to remove, so
            // it is kept and does not set the flag, which is the answer removeValues(Map) gives for it.
            boolean wasModified = false;

            for (final Iterator<Map.Entry<K, V>> it = backingMap.entrySet().iterator(); it.hasNext();) {
                if (!it.next().getValue().isEmpty()) {
                    it.remove();
                    wasModified = true;
                }
            }

            return wasModified;
        }

        return removeValues(m.backingMap);
    }

    /**
     * Removes a single occurrence of the specified value from all collections where the key satisfies the given predicate.
     * This method allows conditional removal based on key properties.
     *
     * <p>The method works in two phases:</p>
     * <ol>
     *   <li>Identifies all keys that satisfy the predicate</li>
     *   <li>For each matching key, removes one occurrence of the specified value from its collection</li>
     * </ol>
     *
     * <p>Keys with empty collections after removal are automatically removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("active", Arrays.asList(1, 2, 1, 3));
     * multimap.putValues("inactive", Arrays.asList(1, 2, 3));
     * multimap.putValues("archived", Arrays.asList(1, 2));
     *
     * // Remove value 1 from keys that don't equal "archived"
     * multimap.removeEntriesIf(key -> !key.equals("archived"), 1);
     * // "active" -> [2, 1, 3], "inactive" -> [2, 3], "archived" -> [1, 2]
     * }</pre>
     *
     * @param keyPredicate the predicate that determines which keys to process
     * @param value the value to be removed from matching collections
     * @return {@code true} if at least one value was removed from any collection,
     *         {@code false} if no values were removed (either no keys matched or value not found)
     * @throws IllegalArgumentException if {@code keyPredicate} is {@code null}.
     * @see #removeEntry(Object, Object)
     * @see #removeValuesIf(Predicate, Collection)
     */
    public boolean removeEntriesIf(final Predicate<? super K> keyPredicate, final E value) throws IllegalArgumentException {
        N.checkArgNotNull(keyPredicate, cs.keyPredicate);

        // Stage in a List, never a Set: the backing map may key by identity (IdentityHashMap) or by a
        // comparator finer than equals, in which case a HashSet would collapse two distinct backing-map
        // keys into one and leave the second one untouched. Keys from a single map iteration are already
        // unique, so a List needs no de-duplication.
        List<K> matchingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (keyPredicate.test(key)) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(key);
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : matchingKeys) {
            wasModified |= removeEntry(k, value);
        }

        return wasModified;
    }

    /**
     * Removes a single occurrence of the specified value from the collections of values associated with keys that satisfy the specified entry predicate.
     *
     * <p>This method iterates over the key-value pairs in the Multimap and applies the predicate to each pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove a single occurrence of the specified value from it.
     * If the value is successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2, 1));
     * multimap.putValues("large", Arrays.asList(1, 2, 3, 4, 5));
     *
     * // Remove value 1 from collections with more than 3 elements
     * multimap.removeEntriesIf((key, values) -> values.size() > 3, 1);
     * // multimap now contains: {small=[1, 2, 1], large=[2, 3, 4, 5]}
     * }</pre>
     *
     * @param entryPredicate the predicate to be applied to each key and its value collection in the Multimap
     * @param value the value to be removed from the collections of values associated with the keys that satisfy the predicate
     * @return {@code true} if at least one value was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise
     * @throws IllegalArgumentException if {@code entryPredicate} is {@code null}.
     * @see #removeEntriesIf(Predicate, Object)
     * @see #removeEntry(Object, Object)
     * @see Collection#remove(Object)
     */
    public boolean removeEntriesIf(final BiPredicate<? super K, ? super V> entryPredicate, final E value) throws IllegalArgumentException {
        N.checkArgNotNull(entryPredicate, cs.entryPredicate);

        // See the sibling keyPredicate overload: a List, not a Set, so backing maps whose key equivalence
        // is not equals/hashCode (IdentityHashMap, comparator-based TreeMap) do not lose a matching key.
        List<K> matchingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (entryPredicate.test(entry.getKey(), entry.getValue())) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        boolean wasModified = false;

        for (final K k : matchingKeys) {
            wasModified |= removeEntry(k, value);
        }

        return wasModified;
    }

    /**
     * Removes all occurrences of the specified values from collections where the key satisfies the given predicate.
     * This method enables bulk conditional removal based on key properties.
     *
     * <p>For each key that satisfies the predicate, all occurrences of all specified values
     * are removed from its collection. This is more efficient than repeatedly calling
     * {@link #removeEntry(Object, Object)} for each key/value pair.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putValues("fruits", Arrays.asList("apple", "banana", "apple", "orange"));
     * multimap.putValues("vegetables", Arrays.asList("carrot", "apple", "tomato"));
     * multimap.putValues("berries", Arrays.asList("strawberry", "apple"));
     *
     * // Remove "apple" and "orange" from keys starting with "f"
     * Set<String> toRemove = Set.of("apple", "orange");
     * multimap.removeValuesIf(key -> key.startsWith("f"), toRemove);
     * // "fruits" -> ["banana"], "vegetables" unchanged, "berries" unchanged
     * }</pre>
     *
     * <p><b>Note:</b> This method mutates this Multimap in place. If all values are removed
     * from a key's collection, the key is removed from the Multimap.</p>
     *
     * <p>Membership is determined by {@code valuesToRemove.contains(value)} before any values are removed.
     * The argument may therefore be a live value collection or a sublist backed by this Multimap;
     * its comparator or identity-based membership rules are preserved.</p>
     *
     * @param keyPredicate the predicate that determines which keys to process
     * @param valuesToRemove the collection of values to remove from matching collections
     * @return {@code true} if any values were removed from any collection,
     *         {@code false} if no changes were made (no keys matched, values not found, or values collection empty)
     * @throws IllegalArgumentException if {@code keyPredicate} is {@code null}.
     * @see #removeValues(Object, Collection)
     * @see #removeKeysIf(Predicate)
     */
    public boolean removeValuesIf(final Predicate<? super K> keyPredicate, final Collection<?> valuesToRemove) throws IllegalArgumentException {
        N.checkArgNotNull(keyPredicate, cs.keyPredicate);

        if (N.isEmpty(valuesToRemove)) {
            return false;
        }

        // Stage in a List, never a Set: the backing map may key by identity (IdentityHashMap) or by a
        // comparator finer than equals, in which case a HashSet would collapse two distinct backing-map
        // keys into one and leave the second one untouched. Keys from a single map iteration are already
        // unique, so a List needs no de-duplication.
        List<K> matchingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (keyPredicate.test(key)) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(key);
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        return removeValuesFromKeys(matchingKeys, valuesToRemove);
    }

    /**
     * Removes all occurrences of the specified elements from the collections of values associated with keys that satisfy the specified predicate.
     *
     * <p>This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to remove all occurrences of the elements from it.
     * If the elements are successfully removed and the collection becomes empty as a result, the key is also removed from the Multimap.</p>
     *
     * <p>As with {@link #removeValuesIf(Predicate, Collection)}, membership is evaluated before mutation
     * using the argument collection's {@code contains} method, so live backed views are supported.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2, 3));
     * multimap.putValues("large", Arrays.asList(1, 2, 3, 4, 5, 6));
     *
     * // Remove 1 and 2 from collections with more than 4 elements
     * multimap.removeValuesIf((key, values) -> values.size() > 4, Arrays.asList(1, 2));
     * // multimap now contains: {small=[1, 2, 3], large=[3, 4, 5, 6]}
     * }</pre>
     *
     * @param entryPredicate the predicate to be applied to each key and its value collection in the Multimap
     * @param valuesToRemove the collection of elements to be removed from the collections of values associated with the keys that satisfy the predicate
     * @return {@code true} if at least one element was successfully removed from the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise
     * @throws IllegalArgumentException if {@code entryPredicate} is {@code null}.
     * @see #removeValuesIf(Predicate, Collection)
     * @see #removeValues(Object, Collection)
     * @see Collection#removeAll(Collection)
     */
    public boolean removeValuesIf(final BiPredicate<? super K, ? super V> entryPredicate, final Collection<?> valuesToRemove) throws IllegalArgumentException {
        N.checkArgNotNull(entryPredicate, cs.entryPredicate);

        if (N.isEmpty(valuesToRemove)) {
            return false;
        }

        // See the sibling keyPredicate overload: a List, not a Set, so backing maps whose key equivalence
        // is not equals/hashCode (IdentityHashMap, comparator-based TreeMap) do not lose a matching key.
        List<K> matchingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (entryPredicate.test(entry.getKey(), entry.getValue())) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        return removeValuesFromKeys(matchingKeys, valuesToRemove);
    }

    private boolean removeValuesFromKeys(final List<K> keys, final Collection<?> valuesToRemove) {
        final List<Set<E>> removals = new ArrayList<>(keys.size());

        // The argument may be a live value collection or subList. Evaluate its membership
        // before any mutation, preserving its comparator/identity contains semantics.
        for (final K key : keys) {
            final Set<E> matchingValues = new IdentityHashSet<>();
            final V values = backingMap.get(key);

            if (values != null) {
                for (final E value : values) {
                    // containsSafely, not contains: a stored null probed against a null-hostile removal
                    // collection must count as "not a member", the same rule removeValues(Object, Collection)
                    // applies and this class's javadoc states is shared by every bulk removal form.
                    if (containsSafely(valuesToRemove, value)) {
                        matchingValues.add(value);
                    }
                }
            }

            removals.add(matchingValues);
        }

        boolean wasModified = false;

        for (int i = 0; i < keys.size(); i++) {
            final K key = keys.get(i);
            final V values = backingMap.get(key);

            // The match set must be non-empty too: Collection.removeIf on an unmodifiable value collection
            // throws UnsupportedOperationException unconditionally, without first checking whether anything
            // would actually be removed. Both sibling removals guard the same way (see removeValues(Object,
            // Collection) and removeValues(Map)), so a no-op bulk removal must not throw where they do not.
            if (N.notEmpty(values) && !removals.get(i).isEmpty()) {
                // Identity records exactly which target objects matched the original argument;
                // equals-based removal could also remove equal objects that did not match it.
                wasModified |= values.removeIf(removals.get(i)::contains);

                if (values.isEmpty()) {
                    backingMap.remove(key);
                }
            }
        }

        return wasModified;
    }

    /**
     * Removes all entries (keys and their associated value collections) where the key satisfies the given predicate.
     * This is a bulk removal operation for entire mappings based on key criteria.
     *
     * <p>This method is useful for filtering out entire key-value mappings based on key properties,
     * such as removing expired entries, inactive items, or entries matching certain patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("user1", Arrays.asList(1, 2, 3));
     * multimap.putValues("admin1", Arrays.asList(4, 5));
     * multimap.putValues("user2", Arrays.asList(6, 7));
     * multimap.putValues("guest1", Arrays.asList(8));
     *
     * // Remove all non-admin entries
     * multimap.removeKeysIf(key -> !key.startsWith("admin"));
     * // Only "admin1" -> [4, 5] remains
     *
     * // Remove entries with numeric suffix greater than 5
     * multimap.removeKeysIf(key -> {
     *     String suffix = key.substring(key.length() - 1);
     *     return Integer.parseInt(suffix) > 5;
     * });
     * }</pre>
     *
     * @param keyPredicate the predicate that determines which entries to remove
     * @return {@code true} if any entries were removed from the Multimap,
     *         {@code false} if no entries matched the predicate
     * @throws IllegalArgumentException if {@code keyPredicate} is {@code null}.
     * @see #removeAll(Object)
     * @see #removeKeysIf(BiPredicate)
     */
    public boolean removeKeysIf(final Predicate<? super K> keyPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(keyPredicate, cs.keyPredicate);

        // Stage in a List, never a Set: the backing map may key by identity (IdentityHashMap) or by a
        // comparator finer than equals, in which case a HashSet would collapse two distinct backing-map
        // keys into one and leave the second one untouched. Keys from a single map iteration are already
        // unique, so a List needs no de-duplication.
        List<K> matchingKeys = null;

        for (final K key : backingMap.keySet()) {
            if (keyPredicate.test(key)) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(key);
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        for (final K k : matchingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Removes all entries where both the key and its value collection satisfy the given predicate.
     * This method provides fine-grained control by considering both key and value collection properties.
     *
     * <p>Unlike {@link #removeKeysIf(Predicate)}, this method passes both the key and its entire
     * value collection to the predicate, allowing decisions based on collection properties
     * such as size, specific contents, or other aggregate conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2));
     * multimap.putValues("medium", Arrays.asList(1, 2, 3, 4));
     * multimap.putValues("large", Arrays.asList(1, 2, 3, 4, 5, 6));
     *
     * // Remove entries with more than 3 values
     * multimap.removeKeysIf((key, values) -> values.size() > 3);
     * // Only "small" -> [1, 2] remains
     *
     * // Remove entries where key length equals collection size
     * multimap.removeKeysIf((key, values) -> key.length() == values.size());
     *
     * // Remove entries containing a specific value
     * multimap.removeKeysIf((key, values) -> values.contains(42));
     * }</pre>
     *
     * @param entryPredicate the bi-predicate that accepts a key and its value collection
     * @return {@code true} if any entries were removed, {@code false} if no entries matched
     * @throws IllegalArgumentException if {@code entryPredicate} is {@code null}.
     * @see #removeKeysIf(Predicate)
     */
    public boolean removeKeysIf(final BiPredicate<? super K, ? super V> entryPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(entryPredicate, cs.entryPredicate);

        // See the sibling keyPredicate overload: a List, not a Set, so backing maps whose key equivalence
        // is not equals/hashCode (IdentityHashMap, comparator-based TreeMap) do not lose a matching key.
        List<K> matchingKeys = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (entryPredicate.test(entry.getKey(), entry.getValue())) {
                if (matchingKeys == null) {
                    matchingKeys = new ArrayList<>();
                }

                matchingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(matchingKeys)) {
            return false;
        }

        for (final K k : matchingKeys) {
            removeAll(k);
        }

        return true;
    }

    /**
     * Replaces the first occurrence of an old value with a new value in the collection associated with the given key.
     * This method is useful for updating specific values while maintaining collection order and other elements.
     *
     * <p>For List-based multimaps, the replacement maintains the position of the replaced element.
     * For Set-based multimaps, the old value is removed and the new value is added (position not guaranteed).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putValues("colors", Arrays.asList("red", "blue", "red", "green"));
     *
     * // Replace first "red" with "orange"
     * multimap.replaceEntry("colors", "red", "orange");
     * // "colors" -> ["orange", "blue", "red", "green"]
     *
     * // Try to replace non-existent value
     * boolean replaced = multimap.replaceEntry("colors", "yellow", "purple");
     * // replaced is false, collection unchanged
     *
     * // SetMultimap example
     * SetMultimap<String, Integer> setMap = N.newSetMultimap();
     * setMap.putValues("nums", Arrays.asList(1, 2, 3));
     * setMap.replaceEntry("nums", 2, 20);   // Removes 2, adds 20
     * }</pre>
     *
     * @param key the key whose value collection should be modified
     * @param oldValue the value to be replaced (can be null)
     * @param newValue the value to replace with (can be null)
     * @return {@code true} if the old value was found and replaced,
     *         {@code false} if the key doesn't exist or old value not found
     * @throws IllegalStateException if a non-List value collection rejects the new value after the
     *         old value was removed (e.g. a Set-based multimap where {@code newValue} already exists);
     *         not thrown for List-based value collections
     * @see #replaceValues(Object, Collection)
     * @see #replaceEntriesIf(Predicate, Object, Object)
     */
    public boolean replaceEntry(final K key, final E oldValue, final E newValue) throws IllegalStateException {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        return replaceEntry(key, val, oldValue, newValue);
    }

    private boolean replaceEntry(final K key, final V val, final E oldValue, final E newValue) {
        if (val instanceof List) {
            final List<E> list = (List<E>) val;

            if (list instanceof RandomAccess) {
                for (int i = 0, len = list.size(); i < len; i++) {
                    if (N.equals(oldValue, list.get(i))) {
                        list.set(i, newValue);
                        return true;
                    }
                }
            } else {
                // One ListIterator pass, not indexOf() followed by set(index, ..), which would walk a
                // linked list twice.
                final ListIterator<E> iter = list.listIterator();

                while (iter.hasNext()) {
                    if (N.equals(oldValue, iter.next())) {
                        iter.set(newValue);
                        return true;
                    }
                }
            }
        } else {
            if (val.remove(oldValue)) {
                boolean added = false;
                RuntimeException addFailure = null;

                try {
                    added = val.add(newValue);
                } catch (final RuntimeException ex) {
                    addFailure = ex;
                }

                if (!added) {
                    final IllegalStateException failure = new IllegalStateException(
                            "Failed to add the new value: " + newValue + " for key: " + key + " for replacement", addFailure);

                    // Restore the previous value so either a false return (for example a duplicate in
                    // a Set) or an exception (for example null in a TreeSet) does not silently drop data.
                    try {
                        if (!val.add(oldValue)) {
                            failure.addSuppressed(new IllegalStateException("Failed to restore the old value: " + oldValue + " for key: " + key));
                        }
                    } catch (final RuntimeException restoreFailure) {
                        failure.addSuppressed(restoreFailure);
                    }

                    if (val.isEmpty()) {
                        backingMap.remove(key);
                    }

                    throw failure;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Replaces a live value collection while preserving its identity. If the configured collection
     * rejects the replacement, restoration of the previous contents is attempted before an exception is reported.
     *
     * <p>{@code replacement} must be non-empty. Every caller reaches this method only after its own
     * {@code N.isEmpty} guard has routed the empty case to outright removal of the mapping, because an empty
     * replacement here would clear {@code target} and leave the key mapped to an empty collection - the one
     * state this class's mutators are required never to produce.</p>
     *
     * @param key the key whose value collection is being replaced, used only for the failure messages
     * @param target the live value collection to refill in place
     * @param replacement the new contents; must not be empty
     * @throws IllegalStateException if {@code target} rejects the replacement
     */
    private void replaceCollectionContents(final K key, final V target, final Collection<? extends E> replacement) throws IllegalStateException {
        final List<E> previous = new ArrayList<>(target);
        RuntimeException updateFailure = null;
        boolean modified = false;

        try {
            target.clear();
            modified = target.addAll(replacement);
        } catch (final RuntimeException ex) {
            updateFailure = ex;
        }

        // No `|| replacement.isEmpty()` here: treating an empty replacement as success would return with
        // `target` cleared and the key still mapped to it. The callers guarantee a non-empty replacement.
        if (updateFailure == null && modified) {
            return;
        }

        final IllegalStateException failure = new IllegalStateException("Failed to replace values for key: " + key, updateFailure);

        try {
            target.clear();

            if (!previous.isEmpty() && !target.addAll(previous)) {
                failure.addSuppressed(new IllegalStateException("Failed to restore the previous values for key: " + key));
            }
        } catch (final RuntimeException restoreFailure) {
            failure.addSuppressed(restoreFailure);
        }

        if (target.isEmpty()) {
            backingMap.remove(key);
        }

        throw failure;
    }

    /**
     * Replaces one occurrence of an old value with a new value in all collections where the key matches the predicate.
     * This allows targeted replacement across multiple keys based on key properties.
     *
     * <p>For each key that satisfies the predicate, this method will replace only the FIRST
     * occurrence of the old value in that key's collection. Different keys may have the
     * replacement happen at different positions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("user_123", Arrays.asList(99, 85, 99, 92));
     * multimap.putValues("user_456", Arrays.asList(78, 99, 88));
     * multimap.putValues("admin_001", Arrays.asList(99, 99, 95));
     *
     * // Replace first occurrence of 99 with 100 for all user keys
     * multimap.replaceEntriesIf(key -> key.startsWith("user_"), 99, 100);
     * // "user_123" -> [100, 85, 99, 92]
     * // "user_456" -> [78, 100, 88]
     * // "admin_001" -> [99, 99, 95] (unchanged)
     *
     * // Update default timeout value for test environments
     * multimap.replaceEntriesIf(key -> key.contains("test"), 30, 60);
     * }</pre>
     *
     * <p>This method is not atomic: keys are processed in the backing map's iteration order and each is
     * updated in place, so if a later key fails the updates already made to earlier keys remain.</p>
     *
     * @param keyPredicate the predicate to test each key
     * @param oldValue the value to be replaced in matching collections
     * @param newValue the replacement value
     * @return {@code true} if at least one replacement was made,
     *         {@code false} if no keys matched or old value was not found
     * @throws IllegalArgumentException if {@code keyPredicate} is {@code null}.
     * @throws IllegalStateException if a Set-based value collection rejects the new value (e.g. the new value already exists in that collection)
     * @see #replaceEntriesIf(BiPredicate, Object, Object)
     * @see #replaceEntry(Object, Object, Object)
     * @see #replaceValuesIf(Predicate, Collection)
     */
    public boolean replaceEntriesIf(final Predicate<? super K> keyPredicate, final E oldValue, final E newValue)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(keyPredicate, cs.keyPredicate);

        boolean wasModified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (keyPredicate.test(entry.getKey())) {
                wasModified |= replaceEntry(entry.getKey(), entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return wasModified;
    }

    /**
     * Replaces a single occurrence of the specified old value with the new value for keys that satisfy the specified predicate in this Multimap.
     *
     * <p>This method iterates over the keys in the Multimap and applies the predicate to each key-value pair. If the predicate returns {@code true},
     * it retrieves the collection of values associated with the key and attempts to replace a single occurrence of the old value with the new value.
     * If the old value is successfully replaced, the method returns {@code true}. If the old value is not found in the collection of values associated with the key,
     * no change is made for that key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2, 3));
     * multimap.putValues("large", Arrays.asList(1, 2, 3, 4, 5));
     *
     * // Replace 1 with 100 in collections with more than 3 elements
     * multimap.replaceEntriesIf((key, values) -> values.size() > 3, 1, 100);
     * // multimap now contains: {small=[1, 2, 3], large=[100, 2, 3, 4, 5]}
     * }</pre>
     *
     * <p>This method is not atomic: keys are processed in the backing map's iteration order and each is
     * updated in place, so if a later key fails the updates already made to earlier keys remain.</p>
     *
     * @param entryPredicate the predicate to be applied to each key and its value collection in the Multimap
     * @param oldValue the old value to be replaced in the collections of values associated with the keys that satisfy the predicate
     * @param newValue the new value to replace the old value in the collections of values associated with the keys that satisfy the predicate
     * @return {@code true} if at least one old value was successfully replaced in the collections of values associated with the keys that satisfy the predicate, {@code false} otherwise
     * @throws IllegalArgumentException if {@code entryPredicate} is {@code null}.
     * @throws IllegalStateException if a Set-based value collection rejects the new value (e.g. the new value already exists in that collection)
     * @see #replaceEntriesIf(Predicate, Object, Object)
     * @see #replaceEntry(Object, Object, Object)
     */
    public boolean replaceEntriesIf(final BiPredicate<? super K, ? super V> entryPredicate, final E oldValue, final E newValue)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(entryPredicate, cs.entryPredicate);

        boolean wasModified = false;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            if (entryPredicate.test(entry.getKey(), entry.getValue())) {
                wasModified |= replaceEntry(entry.getKey(), entry.getValue(), oldValue, newValue); //NOSONAR
            }
        }

        return wasModified;
    }

    /**
     * Replaces all values associated with the specified key with a collection of new values.
     * This is a bulk replacement operation that clears the existing collection and adds new values.
     *
     * <p>If {@code newValues} is {@code null} or empty, the specified key is removed from this Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("scores", Arrays.asList(85, 92, 78, 88, 91));
     *
     * // Replace all scores with a single average
     * multimap.replaceValues("scores", Arrays.asList(87));
     * // "scores" -> [87]
     *
     * // Consolidate multiple statuses to a single status
     * ListMultimap<String, String> statusMap = N.newListMultimap();
     * statusMap.putValues("status", Arrays.asList("pending", "processing", "pending"));
     * statusMap.replaceValues("status", Arrays.asList("completed"));
     * // "status" -> ["completed"]
     *
     * // No effect on non-existent key
     * boolean replaced = statusMap.replaceValues("missing", Arrays.asList("completed"));
     * // replaced is false
     * }</pre>
     *
     * @param key the key whose entire value collection should be replaced
     * @param newValues the new values that will replace all existing values; if {@code null} or
     *                  empty, the key is removed from this Multimap
     * @return {@code true} if the key existed (values were replaced or the key was removed),
     *         {@code false} if the key was not present in the Multimap
     * @throws IllegalStateException if the configured value collection rejects the replacement;
     *         the previous contents are restored when the collection permits it
     * @see #replaceEntry(Object, Object, Object)
     * @see #put(Object, Object)
     */
    public boolean replaceValues(final K key, final Collection<? extends E> newValues) throws IllegalStateException {
        final V val = backingMap.get(key);

        if (val == null) {
            return false;
        }

        if (N.isEmpty(newValues)) {
            backingMap.remove(key);

            return true;
        }

        final List<E> copiedValues = new ArrayList<>(newValues);

        replaceCollectionContents(key, val, copiedValues);

        return true;
    }

    /**
     * Replaces the entire value collection for keys that satisfy the specified predicate.
     *
     * <p>If {@code newValues} is {@code null} or empty, matching keys are removed from this Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("key1", Arrays.asList(1, 2, 3));
     * multimap.putValues("key2", Arrays.asList(4, 5));
     *
     * // Replace all values for keys that start with "key"
     * multimap.replaceValuesIf(key -> key.startsWith("key"), Arrays.asList(100, 200));
     * // multimap now contains: {key1=[100, 200], key2=[100, 200]}
     * }</pre>
     *
     * <p>This method is not atomic: keys are processed in the backing map's iteration order and each is
     * updated in place, so if a later key fails the updates already made to earlier keys remain.</p>
     *
     * @param keyPredicate the predicate to be applied to each key in the Multimap
     * @param newValues the replacement values; if {@code null} or empty, matching keys are removed
     * @return {@code true} if at least one key was updated or removed, {@code false} otherwise
     * @throws IllegalArgumentException if {@code keyPredicate} is {@code null}.
     * @throws IllegalStateException if a configured value collection rejects the replacement;
     *         that key's previous contents are restored when the collection permits it
     */
    public boolean replaceValuesIf(final Predicate<? super K> keyPredicate, final Collection<? extends E> newValues)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(keyPredicate, cs.keyPredicate);

        boolean wasModified = false;

        if (N.isEmpty(newValues)) {
            final List<K> keys = new ArrayList<>(backingMap.keySet());

            for (K key : keys) {
                if (keyPredicate.test(key)) {
                    backingMap.remove(key);
                    wasModified = true;
                }
            }
        } else {
            // Defensive copy like replaceValues(K, Collection): newValues may be (or view) one of the
            // live value collections, which val.clear() would empty before the addAll.
            final List<E> copiedValues = new ArrayList<>(newValues);
            V val = null;

            for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
                if (keyPredicate.test(entry.getKey())) {
                    val = entry.getValue();
                    replaceCollectionContents(entry.getKey(), val, copiedValues);

                    wasModified = true;
                }
            }
        }

        return wasModified;
    }

    /**
     * Replaces the entire value collection for keys whose key-value collections satisfy the specified predicate.
     *
     * <p>If {@code newValues} is {@code null} or empty, matching keys are removed from this Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2, 3));
     * multimap.putValues("large", Arrays.asList(1, 2, 3, 4, 5));
     *
     * // Replace values for collections with more than 3 elements
     * multimap.replaceValuesIf((key, values) -> values.size() > 3, Arrays.asList(100));
     * // multimap now contains: {small=[1, 2, 3], large=[100]}
     * }</pre>
     *
     * <p>This method is not atomic: keys are processed in the backing map's iteration order and each is
     * updated in place, so if a later key fails the updates already made to earlier keys remain.</p>
     *
     * @param entryPredicate the predicate to be applied to each key and its value collection in the Multimap
     * @param newValues the replacement values; if {@code null} or empty, matching keys are removed
     * @return {@code true} if at least one key was updated or removed, {@code false} otherwise
     * @throws IllegalArgumentException if {@code entryPredicate} is {@code null}.
     * @throws IllegalStateException if a configured value collection rejects the replacement;
     *         that key's previous contents are restored when the collection permits it
     */
    public boolean replaceValuesIf(final BiPredicate<? super K, ? super V> entryPredicate, final Collection<? extends E> newValues)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(entryPredicate, cs.entryPredicate);

        boolean wasModified = false;

        if (N.isEmpty(newValues)) {
            final List<K> keysToRemove = new ArrayList<>();

            for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
                if (entryPredicate.test(entry.getKey(), entry.getValue())) {
                    keysToRemove.add(entry.getKey());
                }
            }

            if (!keysToRemove.isEmpty()) {
                for (K key : keysToRemove) {
                    backingMap.remove(key);
                }

                wasModified = true;
            }
        } else {
            // Defensive copy like replaceValues(K, Collection): newValues may be (or view) one of the
            // live value collections, which val.clear() would empty before the addAll.
            final List<E> copiedValues = new ArrayList<>(newValues);
            V val = null;

            for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
                val = entry.getValue();

                if (entryPredicate.test(entry.getKey(), val)) {
                    replaceCollectionContents(entry.getKey(), val, copiedValues);

                    wasModified = true;
                }
            }
        }

        return wasModified;
    }

    /**
     * Replaces all values associated with each key in this Multimap according to the provided function.
     *
     * <p>This method iterates over the keys in the Multimap and applies the function to each key-value pair.
     * The function should return a new collection of values that will replace the old collection associated with the key.
     * If the function returns {@code null} or an empty collection, the key is removed from the Multimap.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("numbers", Arrays.asList(1, 2, 3));
     * multimap.putValues("values", Arrays.asList(4, 5));
     *
     * // Double all values
     * multimap.replaceAll((key, values) -> {
     *     List<Integer> doubled = new ArrayList<>();
     *     for (Integer v : values) {
     *         doubled.add(v * 2);
     *     }
     *     return doubled;
     * });
     * // multimap now contains: {numbers=[2, 4, 6], values=[8, 10]}
     * }</pre>
     *
     * <p>If the function returns the same non-empty collection instance that was passed in, that
     * entry is left unchanged. If the function empties the collection in place and returns it, the
     * empty-result rule above applies and the key is removed.</p>
     *
     * <p><b>The collection instance a function returns is not stored.</b> Its <i>contents</i> are copied
     * into the collection already held for that key, which keeps its identity - so a caller still holding a
     * collection from {@link #get(Object)} sees the new values, and returning a different collection
     * implementation does not change the type this Multimap stores.</p>
     *
     * @param function the function that transforms each key's value collection; must not be {@code null}.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     * @throws IllegalStateException if the returned non-empty collection of values cannot be added
     *         back into the existing value collection for a key
     * @see #compute(Object, BiFunction)
     * @see #replaceValuesIf(BiPredicate, Collection)
     */
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(function, cs.function);

        List<K> keyToRemove = null;
        V value = null;
        V newValue = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            value = entry.getValue();

            newValue = function.apply(entry.getKey(), value);

            // Emptiness first: the documented "null or empty result removes the mapping" must also apply
            // when the function emptied and returned the SAME live collection instance.
            if (N.isEmpty(newValue)) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else if (newValue == value) {
                // continue.
            } else {
                final List<E> copiedValues = new ArrayList<>(newValue);
                replaceCollectionContents(entry.getKey(), value, copiedValues);
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (final K key : keyToRemove) {
                backingMap.remove(key);
            }
        }
    }

    /**
     * Computes and associates a value collection for the specified key if it's not already present.
     * This method implements lazy initialization for Multimap entries, computing values only when needed.
     *
     * <p>If the key already has an associated value collection, that existing collection is returned
     * without calling the mapping function. This makes the method safe to call repeatedly.</p>
     *
     * <p>If the mapping function returns {@code null} or an empty collection, no entry is created,
     * and the method returns {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     *
     * // Initialize default permissions for new users
     * multimap.computeIfAbsent("user123",
     *     key -> new ArrayList<>(Arrays.asList("read", "write")));
     *
     * // Won't overwrite existing values
     * multimap.put("admin", "super");
     * multimap.computeIfAbsent("admin",
     *     key -> Arrays.asList("basic"));   // returns existing ["super"]
     *
     * // Compute based on key properties
     * multimap.computeIfAbsent("guest_temp", key -> {
     *     if (key.startsWith("guest_")) {
     *         return Arrays.asList("read-only");
     *     }
     *     return Arrays.asList("read", "write");
     * });
     * }</pre>
     *
     * <p><b>The collection instance the function returns is not stored.</b> Its <i>contents</i> are copied
     * into a fresh collection obtained from this Multimap's value supplier, which is what gets stored and
     * returned - so the returned collection is never the one the function produced, and mutating the
     * function's collection afterwards does not affect this Multimap. This matches
     * {@link #compute(Object, BiFunction)} and {@link #replaceAll(BiFunction)}, and it is what keeps the
     * value-collection type this Multimap was configured with.</p>
     *
     * <p><b>Thread Safety:</b> This operation is not atomic. External synchronization is required
     * for concurrent access.</p>
     *
     * @param key the key for which to compute an initial value collection
     * @param mappingFunction the function to compute the initial collection, called only if key is absent
     * @return the existing collection if key was present, or the newly created collection holding the
     *         computed contents, or {@code null} if the mapping function returned null/empty
     * @throws IllegalArgumentException if {@code mappingFunction} is {@code null}.
     * @see #computeIfPresent(Object, BiFunction)
     * @see #compute(Object, BiFunction)
     */
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(mappingFunction, cs.mappingFunction);

        final V oldValue = get(key);

        if (oldValue != null) {
            return oldValue;
        }

        final V newValue = mappingFunction.apply(key);

        if (N.notEmpty(newValue)) {
            putValues(key, newValue);
        }

        return get(key);
    }

    /**
     * Updates the value collection for the specified key using a remapping function, but only if the key is present.
     * This method allows conditional transformation of existing collections without affecting absent keys.
     *
     * <p>If the key is not present, this method does nothing and returns {@code null}.</p>
     *
     * <p>If the remapping function returns {@code null} or an empty collection, the entire mapping
     * (key and its collection) is removed from the Multimap.</p>
     *
     * <p>The remapping function receives both the key and its current collection, allowing
     * transformations based on both key properties and current values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("scores", Arrays.asList(85, 92, 78, 95, 88));
     * multimap.putValues("grades", Arrays.asList(75, 82));
     *
     * // Transform scores above threshold to 100
     * multimap.computeIfPresent("scores", (key, values) -> {
     *     return values.stream()
     *         .map(score -> score >= 90 ? 100 : score)
     *         .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
     * });
     * // "scores" -> [85, 100, 78, 100, 88]
     *
     * // Remove collections with fewer than 3 elements
     * multimap.computeIfPresent("grades", (key, values) -> {
     *     return values.size() >= 3 ? values : null;
     * });
     * // "grades" is removed from multimap
     *
     * // No effect on absent keys
     * multimap.computeIfPresent("absent", (key, values) -> new ArrayList<>());
     * // No change
     * }</pre>
     *
     * @param key the key whose value collection should be remapped
     * @param remappingFunction the function that computes a new collection based on key and current collection
     * @return the updated collection, or {@code null} if the key was not present or the function returned null/empty
     * @throws IllegalArgumentException if {@code remappingFunction} is {@code null}.
     * @throws IllegalStateException if the configured value collection rejects the computed replacement
     * @see #computeIfAbsent(Object, Function)
     * @see #compute(Object, BiFunction)
     */
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(remappingFunction, cs.remappingFunction);

        final V oldValue = get(key);

        if (oldValue == null) {
            //noinspection ConstantValue
            return oldValue;
        }

        V ret = null;
        final V newValue = remappingFunction.apply(key, oldValue);

        // Emptiness first: the documented "null or empty result removes the mapping" must also apply
        // when the function emptied and returned the SAME live collection instance.
        if (N.isEmpty(newValue)) {
            backingMap.remove(key);
        } else if (newValue == oldValue) {
            ret = oldValue;
        } else {
            final List<E> copiedValues = new ArrayList<>(newValue);
            replaceCollectionContents(key, oldValue, copiedValues);
            ret = oldValue;
        }

        return ret;
    }

    /**
     * Computes the value for the specified key using the given remapping function.
     *
     * <p>This method first retrieves the current value associated with the specified key. Then, it applies the remapping function to the key and its current associated value,
     * and updates the key's associated value in the Multimap with the result. If the remapping function returns {@code null} or an empty collection,
     * the key is removed from the Multimap.</p>
     *
     * <p>If the key is not already associated with a value in the Multimap, this method associates the key with the new value returned by the remapping function.
     * In that case, if the remapping function returns {@code null} or an empty collection, no entry is created.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("numbers", Arrays.asList(1, 2, 3));
     *
     * // Add 10 to all values or create new list with 10
     * multimap.compute("numbers", (key, values) -> {
     *     if (values == null) {
     *         return Arrays.asList(10);
     *     }
     *     List<Integer> result = new ArrayList<>();
     *     for (Integer v : values) {
     *         result.add(v + 10);
     *     }
     *     return result;
     * });
     * // multimap now contains: {numbers=[11, 12, 13]}
     * }</pre>
     *
     * <p>When the key was already present, the collection instance the function returns is not stored: its
     * contents are copied into the collection already held for that key, which keeps its identity, and that
     * same instance is returned. Only for a previously absent key is a new collection created.</p>
     *
     * @param key the key whose associated value is to be computed
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or {@code null} if the remapping function returns {@code null} or an empty collection
     * @throws IllegalArgumentException if {@code remappingFunction} is {@code null}.
     * @throws IllegalStateException if the configured value collection rejects the computed replacement
     */
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(remappingFunction, cs.remappingFunction);

        V ret = null;
        final V oldValue = get(key);
        final V newValue = remappingFunction.apply(key, oldValue);

        // Emptiness first: the documented "null or empty result removes the mapping" must also apply
        // when the function emptied and returned the SAME live collection instance.
        if (N.isEmpty(newValue)) {
            if (oldValue != null) {
                backingMap.remove(key);
            }
        } else if (newValue == oldValue) {
            ret = oldValue;
        } else if (oldValue == null) {
            putValues(key, newValue);
            ret = get(key);
        } else {
            final List<E> copiedValues = new ArrayList<>(newValue);
            replaceCollectionContents(key, oldValue, copiedValues);
            ret = oldValue;
        }

        return ret;
    }

    /**
     * Merges a collection of elements with the existing values for a key using a custom merging function.
     * This method provides flexible control over how new elements are combined with existing ones.
     *
     * <p>The merge operation works as follows:</p>
     * <ul>
     *   <li>If the key doesn't exist, the elements are added directly and the resulting collection is returned</li>
     *   <li>If the key exists, the remapping function receives the current collection and the new elements,
     *       allowing you to define custom merge logic (e.g., union, intersection, custom filtering)</li>
     *   <li>If the function returns {@code null} or an empty collection, the key is removed from the Multimap</li>
     *   <li>If the function returns the same non-empty collection instance (oldValue == newValue),
     *       it is retained, including any changes the function made to it</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("numbers", Arrays.asList(1, 2, 3));
     *
     * // Union: add all new elements to existing ones
     * multimap.merge("numbers", Arrays.asList(4, 5), (oldVals, newVals) -> {
     *     List<Integer> result = new ArrayList<>(oldVals);
     *     result.addAll(newVals);
     *     return result;
     * });
     * // "numbers" -> [1, 2, 3, 4, 5]
     *
     * // Custom logic: only keep values > 10 (key already present, so the function runs)
     * multimap.merge("numbers", Arrays.asList(15, 8, 22), (oldVals, newVals) -> {
     *     List<Integer> combined = new ArrayList<>(oldVals);
     *     combined.addAll(newVals);
     *     return combined.stream().filter(v -> v > 10).collect(java.util.stream.Collectors.toList());
     * });
     *
     * // Remove key if the function returns an empty collection
     * multimap.merge("numbers", Arrays.asList(1, 2), (old, neu) -> Collections.emptyList());
     * // "numbers" key is removed
     * }</pre>
     *
     * @param <C> the type of the collection containing elements to merge
     * @param key the key whose value should be merged with the elements
     * @param elements the collection of elements to merge with existing values; must not be {@code null}
     *                 (an empty collection is allowed), mirroring {@link java.util.Map#merge} where the
     *                 supplied merge value may not be {@code null}
     * @param remappingFunction the function that defines how to merge old and new collections
     * @return the updated collection associated with the key, or {@code null} if the key was removed
     *         by the remapping function, or if the key was absent and {@code elements} is empty (nothing is stored)
     * @throws IllegalArgumentException if {@code elements} is {@code null}, or if {@code remappingFunction} is
     *         {@code null}.
     * @throws IllegalStateException if the configured value collection rejects the merged replacement
     * @see #merge(Object, Object, BiFunction)
     * @see #compute(Object, BiFunction)
     */
    public <C extends Collection<? extends E>> V merge(final K key, final C elements, final BiFunction<? super V, ? super C, ? extends V> remappingFunction)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(elements, cs.elements);
        N.checkArgNotNull(remappingFunction, cs.remappingFunction);

        final V oldValue = get(key);

        if (oldValue == null) {
            putValues(key, elements);
            return get(key);
        }

        V ret = null;
        final V newValue = remappingFunction.apply(oldValue, elements);

        // Emptiness first: the documented "null or empty result removes the mapping" must also apply
        // when the function emptied and returned the SAME live collection instance.
        if (N.isEmpty(newValue)) {
            backingMap.remove(key);
        } else if (newValue == oldValue) {
            ret = oldValue;
        } else {
            final List<E> copiedValues = new ArrayList<>(newValue);
            replaceCollectionContents(key, oldValue, copiedValues);
            ret = oldValue;
        }

        return ret;
    }

    /**
     * Merges a single element with the existing values for a key using a custom merging function.
     * This is the single-element variant of {@link #merge(Object, Collection, BiFunction)}.
     *
     * <p>The merge operation works as follows:</p>
     * <ul>
     *   <li>If the key doesn't exist, the element is added directly and the resulting collection is returned</li>
     *   <li>If the key exists, the remapping function receives the current collection and the new element,
     *       allowing custom logic to determine the final collection state</li>
     *   <li>If the function returns {@code null} or an empty collection, the key is removed</li>
     *   <li>If the function returns the same non-empty collection instance, it is retained,
     *       including any changes the function made to it</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("scores", Arrays.asList(85, 92, 78));
     *
     * // Add element if it improves the average
     * multimap.merge("scores", 95, (oldVals, newVal) -> {
     *     double currentAvg = oldVals.stream().mapToInt(Integer::intValue).average().orElse(0);
     *     if (newVal > currentAvg) {
     *         List<Integer> updated = new ArrayList<>(oldVals);
     *         updated.add(newVal);
     *         return updated;
     *     }
     *     return oldVals;
     * });
     *
     * // Replace all existing values if the new value meets a condition
     * multimap.merge("scores", 100, (oldVals, newVal) -> {
     *     if (newVal > 50) {
     *         return Arrays.asList(newVal);
     *     }
     *     return oldVals;
     * });
     * }</pre>
     *
     * @param key the key whose value should be merged with the element
     * @param e the element to merge with existing values; must not be {@code null}, mirroring
     *          {@link java.util.Map#merge} where the supplied merge value may not be {@code null}. Note
     *          this is stricter than {@link #put(Object, Object)}, which does store a {@code null} element
     *          when the value collection permits one.
     * @param remappingFunction the function that defines how to merge the collection with the new element
     * @return the updated collection associated with the key, or {@code null} if the key was removed
     * @throws IllegalArgumentException if {@code e} is {@code null}, or if {@code remappingFunction} is {@code null}.
     * @throws IllegalStateException if the configured value collection rejects the merged replacement
     * @see #merge(Object, Collection, BiFunction)
     * @see #compute(Object, BiFunction)
     */
    public V merge(final K key, final E e, final BiFunction<? super V, ? super E, ? extends V> remappingFunction)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(e, cs.element);
        N.checkArgNotNull(remappingFunction, cs.remappingFunction);

        final V oldValue = get(key);

        if (oldValue == null) {
            put(key, e);
            return get(key);
        }

        V ret = null;
        final V newValue = remappingFunction.apply(oldValue, e);

        // Emptiness first: the documented "null or empty result removes the mapping" must also apply
        // when the function emptied and returned the SAME live collection instance.
        if (N.isEmpty(newValue)) {
            backingMap.remove(key);
        } else if (newValue == oldValue) {
            ret = oldValue;
        } else {
            final List<E> copiedValues = new ArrayList<>(newValue);
            replaceCollectionContents(key, oldValue, copiedValues);
            ret = oldValue;
        }

        return ret;
    }

    /**
     * Creates an inverted Multimap where keys and values are swapped.
     * This operation transforms a Multimap&lt;K, E, V&gt; into a Multimap&lt;E, K, VV&gt;.
     *
     * <p>The verb <i>invert</i> denotes a copy-producing transformation: the returned multimap is
     * independent of this one. In contrast, {@link BiMap#inverse()} returns a live view backed by
     * the same mappings.</p>
     *
     * <p>In the resulting Multimap:</p>
     * <ul>
     *   <li>Each value from the original collections becomes a key</li>
     *   <li>Each original key becomes a value in the new collections</li>
     *   <li>If multiple keys had the same value, they all appear in that value's collection</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> original = N.newListMultimap();
     * original.putValues("group1", Arrays.asList(1, 2, 3));
     * original.putValues("group2", Arrays.asList(2, 4));
     * original.put("group3", 1);
     *
     * // Invert: numbers become keys, groups become values
     * ListMultimap<Integer, String> inverted = original.invert(N::newListMultimap);
     * // Result (order within each value list follows the source's HashMap key
     * // iteration order, which is not guaranteed):
     * // 1 -> ["group1", "group3"]  (or ["group3", "group1"])
     * // 2 -> ["group1", "group2"]
     * // 3 -> ["group1"]
     * // 4 -> ["group2"]
     *
     * // Different Multimap types
     * SetMultimap<Integer, String> invertedSet = original.invert(N::newSetMultimap);
     * }</pre>
     *
     * <p><b>Performance:</b> This operation has O(n) complexity where n is the total number
     * of key-value pairs across all collections.</p>
     *
     * @param <VV> the collection type for values in the inverted Multimap
     * @param <M> the specific Multimap type to create
     * @param multimapSupplier factory function that creates the target Multimap type,
     *                         receives this Multimap's total value count as a capacity hint,
     *                         saturated at {@link Integer#MAX_VALUE}
     * @return a new inverted Multimap with keys and values swapped
     * @throws IllegalArgumentException if {@code multimapSupplier} is {@code null}.
     * @see #copy()
     */
    public <VV extends Collection<K>, M extends Multimap<E, K, VV>> M invert(final IntFunction<? extends M> multimapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(multimapSupplier, cs.multimapSupplier);

        final M res = multimapSupplier.apply(saturatedTotalValueCount());

        if (!backingMap.isEmpty()) {
            for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
                final V values = entry.getValue();

                if (N.notEmpty(values)) {
                    for (final E element : values) {
                        res.put(element, entry.getKey());
                    }
                }
            }
        }

        return res;
    }

    /**
     * Creates a shallow copy of this Multimap with the same structure and contents.
     * The new Multimap has independent map and collection structure; keys and individual elements are shared.
     *
     * <p>This is a shallow copy, meaning:</p>
     * <ul>
     *   <li>The Multimap structure is duplicated</li>
     *   <li>New collections are created for each key</li>
     *   <li>The individual elements themselves are not cloned</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> original = N.newListMultimap();
     * original.putValues("colors", Arrays.asList("red", "blue", "green"));
     *
     * ListMultimap<String, String> copy = original.copy();
     * copy.put("colors", "yellow");   // only affects the copy
     *
     * original.keyCount();            // still 1 key
     * copy.keyCount();                // still 1 key, but different collections
     * }</pre>
     *
     * <p><b>Note:</b> the returned instance is a base {@code Multimap} created from this Multimap's
     * map and value suppliers; subclasses override this method to preserve their concrete type.</p>
     *
     * <p>Every key of this Multimap appears in the copy, including a key whose value collection is
     * empty. Normal mutators never leave such a key behind, but one that was emptied through a live
     * collection from {@link #get(Object)} (or through an externally supplied backing map) is copied
     * faithfully rather than dropped, so {@code copy().equals(this)} always holds.</p>
     *
     * @return a new Multimap containing the same key-value mappings as this one
     * @throws IllegalArgumentException if the map supplier returns a {@code null} or non-empty map, or the copy would share the backing map or a live
     *         value collection with this Multimap
     * @throws NullPointerException if the value supplier returns {@code null} while copying a stored entry, or a stored value collection is {@code null}
     * @throws UnsupportedOperationException if a supplied map or value collection does not support the mutations needed to copy entries
     * @see #invert(IntFunction)
     */
    public Multimap<K, E, V> copy() throws IllegalArgumentException, NullPointerException, UnsupportedOperationException {
        final Multimap<K, E, V> copy = new Multimap<>(mapSupplier, valueSupplier);

        copyInto(copy);

        return copy;
    }

    /**
     * Verifies that {@code target} - freshly built from this Multimap's suppliers - shares no mutable state
     * with this Multimap, and fails fast if it does.
     *
     * <p>The constructor can only check that a supplied map is empty <i>at that moment</i>. A supplier that
     * hands out one shared instance therefore passes construction whenever the original is still empty, and
     * {@link #copy()} would then return an alias: writes to the "copy" would be visible through the original.
     * A shared value-collection supplier is worse still - {@link #copyInto(Multimap)} would ask for a fresh
     * collection, be handed the one already installed under the source key, and append it to itself.</p>
     *
     * @param target the copy under construction, built from this Multimap's suppliers
     * @throws IllegalArgumentException if {@code target} shares this Multimap's backing map or would reuse
     *         one of its live value collections
     */
    void checkCopyIsIndependent(final Multimap<K, E, V> target) throws IllegalArgumentException {
        if (target.backingMap == backingMap) {
            throw new IllegalArgumentException("The map supplier returned this Multimap's own backing map; it must return a new empty map on every call");
        }

        if (!backingMap.isEmpty()) {
            final V probe = target.valueSupplier.get();

            for (final V existing : backingMap.values()) {
                if (probe == existing) {
                    throw new IllegalArgumentException(
                            "The value supplier returned a value collection already held by this Multimap; it must return a new empty collection on every call");
                }
            }
        }
    }

    /**
     * Copies every mapping of this Multimap into {@code target}, giving each key a fresh value collection
     * obtained from {@code target}'s value supplier.
     *
     * <p>This is deliberately not {@code target.putValues(this)}: {@code putValues} skips a source entry
     * whose value collection is empty, which would make {@code copy()} silently drop such a key and stop
     * being {@code equals} to its source. A copy must reproduce what is actually stored.</p>
     *
     * <p>Each key is <i>replaced</i> in {@code target}, not merged into it, so this is only meaningful for
     * a freshly created target - which is the sole use, {@link #copy()} and the subclass overrides of it.</p>
     *
     * @param target the (normally empty) Multimap to copy this Multimap's mappings into
     */
    void copyInto(final Multimap<K, E, V> target) {
        checkCopyIsIndependent(target);

        V val = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            val = target.valueSupplier.get();
            val.addAll(entry.getValue());

            target.backingMap.put(entry.getKey(), val);
        }
    }

    /**
     * Tests whether this Multimap contains the specified key-value pair.
     * This method checks for exact key-value associations rather than just key or value presence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 1);
     * multimap.put("numbers", 2);
     * multimap.put("letters", null);
     *
     * multimap.containsEntry("numbers", 1);      // returns true
     * multimap.containsEntry("numbers", 3);      // returns false
     * multimap.containsEntry("missing", 1);      // returns false
     * multimap.containsEntry("letters", null);   // returns true (null values supported)
     * }</pre>
     *
     * @param key the key to check for
     * @param e the value to check for in the key's collection
     * @return {@code true} if the key exists and its collection contains the specified value,
     *         {@code false} otherwise
     * @see #containsKey(Object)
     * @see #containsValue(Object)
     */
    public boolean containsEntry(final Object key, final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V val = backingMap.get(key);

        //noinspection SuspiciousMethodCalls
        return val != null && val.contains(e);
    }

    /**
     * Tests whether this Multimap contains the specified key.
     *
     * <p>Note: This Multimap automatically removes a key when its value collection becomes
     * empty, so a key is only present if it has at least one associated value (when the
     * Multimap's own mutation methods are used).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("numbers", 42);
     *
     * multimap.containsKey("numbers");   // returns true
     * multimap.containsKey("missing");   // returns false
     * multimap.containsKey(null);        // result depends on backing map (usually false)
     * }</pre>
     *
     * @param key the key to check for
     * @return {@code true} if this Multimap contains the specified key, {@code false} otherwise
     * @see #containsValue(Object)
     * @see #containsEntry(Object, Object)
     */
    public boolean containsKey(final Object key) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(key);
    }

    /**
     * Tests whether this Multimap contains the specified value in any of its collections.
     * This method searches through all value collections to find the specified element.
     *
     * <p><b>Performance:</b> This operation is O(n) where n is the total number of values
     * across all collections, as it must check each collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("group1", 10);
     * multimap.put("group2", 20);
     * multimap.put("group1", null);
     *
     * multimap.containsValue(10);     // returns true
     * multimap.containsValue(30);     // returns false
     * multimap.containsValue(null);   // returns true (null values supported)
     * }</pre>
     *
     * @param e the value to search for across all collections
     * @return {@code true} if any collection in this Multimap contains the specified value,
     *         {@code false} if not found or Multimap is empty
     * @see #containsKey(Object)
     * @see #containsEntry(Object, Object)
     */
    public boolean containsValue(final Object e) {
        final Collection<V> values = backingMap.values();

        for (final V val : values) {
            //noinspection SuspiciousMethodCalls
            if (val.contains(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs the given action for each individual key-element pair in this Multimap.
     * This method "flattens" the Multimap structure, treating each element separately rather than as collections.
     *
     * <p>Unlike iterating over {@link #stream()} or {@link #iterator()} (which operate on key-collection pairs),
     * this method operates on key-element pairs, iterating through every individual element.</p>
     *
     * <p>The name is deliberately not {@code forEach}: the inherited {@link Iterable#forEach(java.util.function.Consumer)}
     * visits key-<i>collection</i> pairs, so two same-named overloads would have differed only by the arity
     * of the caller's lambda while iterating at different granularities.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("group1", Arrays.asList(1, 2, 3));
     * multimap.putValues("group2", Arrays.asList(4, 5));
     *
     * // Process each key-element pair individually
     * multimap.forEachKeyValue((key, element) -> {
     *     System.out.println(key + " contains " + element);
     * });
     * // Output (key order follows the backing HashMap's iteration order, which is
     * // not guaranteed; elements within a key follow its collection's order):
     * // group1 contains 1
     * // group1 contains 2
     * // group1 contains 3
     * // group2 contains 4
     * // group2 contains 5
     *
     * // Collect all values with their keys
     * Map<String, List<Integer>> collected = new HashMap<>();
     * multimap.forEachKeyValue((key, element) ->
     *     collected.computeIfAbsent(key, k -> new ArrayList<>()).add(element));
     * }</pre>
     *
     * <p><b>Performance:</b> This method processes every individual element, so the time complexity
     * is O(n) where n is the total number of elements across all collections.</p>
     *
     * @param action the action to be performed for each key-element pair
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @see #allValues()
     * @see #entryStream()
     */
    @Beta
    public void forEachKeyValue(final BiConsumer<? super K, ? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        K key = null;

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            key = entry.getKey();

            for (final E e : entry.getValue()) {
                action.accept(key, e);
            }
        }
    }

    /**
     * Returns a Set view of the keys contained in this Multimap.
     * The returned set is backed by the Multimap, so changes to the Multimap are reflected in the set.
     *
     * <p><b>Live View:</b> The returned set is the backing map's key set. Structural changes to the
     * Multimap (adding/removing keys) are visible in this set. Removing a key through this set also
     * removes the corresponding mapping from the Multimap; however, the set does not support adding
     * keys (just as a {@link Map#keySet()} does not).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("key1", 1);
     * multimap.put("key2", 2);
     *
     * Set<String> keys = multimap.keySet();
     * System.out.println(keys.size());             // prints 2
     * System.out.println(keys.contains("key1"));   // prints true
     *
     * // Live view - changes reflect
     * multimap.put("key3", 3);
     * System.out.println(keys.size());   // prints 3
     *
     * // Iteration
     * for (String key : keys) {
     *     Collection<?> values = multimap.get(key);
     *     System.out.println(key + " has " + values.size() + " values");
     * }
     * }</pre>
     *
     * @return a Set view of the keys contained in this Multimap
     * @see #allValues()
     * @see #totalValueCount()
     */
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    /**
     * Returns a Collection view of all value collections in this Multimap.
     * Each element in the returned collection is itself a collection of values for a key.
     *
     * <p><b>Live View:</b> The returned collection is the backing map's own {@code values()} view, so it
     * reflects subsequent changes to this Multimap. Its supported removals are those of the backing map,
     * and mutating an inner collection bypasses this
     * class's key lifecycle:</p>
     * <ul>
     *   <li>{@code add} and {@code addAll} throw {@link UnsupportedOperationException}, as for any
     *       {@link Map#values()} view.</li>
     *   <li>{@code remove(collection)} removes <b>one arbitrary</b> mapping whose value collection is
     *       {@code equals} to the argument - not necessarily the one you meant, when several keys hold
     *       equal collections.</li>
     *   <li>{@code clear} removes every mapping, and {@code Iterator.remove} removes the mapping just
     *       returned.</li>
     *   <li>Mutating one of the <i>inner</i> collections it yields is the same as mutating the collection
     *       from {@link #get(Object)} - see that method for what it implies.</li>
     * </ul>
     * <p>Prefer this class's own mutators, which maintain the key lifecycle. Note that {@link #allValues()},
     * by contrast, is an unmodifiable view.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("evens", Arrays.asList(2, 4, 6));
     * multimap.putValues("odds", Arrays.asList(1, 3, 5));
     *
     * Collection<List<Integer>> valueCollections = multimap.valueCollections();
     * for (List<Integer> collection : valueCollections) {
     *     System.out.println("Collection size: " + collection.size());
     * }
     * }</pre>
     *
     * <p><b>Note:</b> If you need all individual elements rather than collections,
     * use {@link #allValues()} instead.</p>
     *
     * @return a Collection view of the value collections contained in this Multimap
     * @see #keySet()
     * @see #allValues()
     */
    public Collection<V> valueCollections() {
        return backingMap.values();
    }

    /**
     * Returns a read-only view collection containing the <i>value</i> from each key-element pair contained in
     * this multimap, without collapsing duplicates. When the total fits in an {@code int},
     * {@code allValues().size() == totalValueCount()}; for a larger total, the collection view's
     * {@code size()} saturates at {@link Integer#MAX_VALUE} as required by {@link Collection#size()}.
     *
     * <p>The returned collection is backed by this multimap and reflects subsequent changes.
     * It is unmodifiable; use {@code put/remove} operations on the multimap to mutate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("evens", Arrays.asList(2, 4, 6));
     * multimap.putValues("odds", Arrays.asList(1, 3, 5));
     *
     * Collection<Integer> allValues = multimap.allValues();
     * System.out.println(allValues);                    // prints e.g. [1, 3, 5, 2, 4, 6] (key order not guaranteed)
     * System.out.println(allValues.size());             // prints 6
     * System.out.println(multimap.totalValueCount());   // prints 6
     * }</pre>
     *
     * @return an unmodifiable collection view of all individual values in this Multimap
     * @see #flatValues(IntFunction)
     * @see #totalValueCount()
     * @see #stream()
     */
    public Collection<E> allValues() {
        Collection<E> result = values;

        return (result == null) ? values = ImmutableCollection.wrap(createValues()) : result;
    }

    /**
     * Returns all individual values from all collections in this Multimap in a collection of a specific type.
     * This is the customizable variant of {@link #allValues()} that allows choosing the collection type.
     *
     * <p>The supplier function receives the total count of values as a capacity hint,
     * which can improve performance by pre-sizing the collection appropriately. The hint
     * is saturated at {@link Integer#MAX_VALUE} if the exact total is larger.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("evens", Arrays.asList(2, 4, 6));
     * multimap.putValues("odds", Arrays.asList(1, 3, 5));
     *
     * // Get as LinkedHashSet to preserve order and remove duplicates. `size` is an expected element
     * // count, so convert it to a capacity with N.newLinkedHashSet rather than passing it straight
     * // to the LinkedHashSet capacity constructor.
     * Set<Integer> uniqueOrdered = multimap.flatValues(N::newLinkedHashSet);
     *
     * // Get as TreeSet for sorted unique values
     * TreeSet<Integer> sorted = multimap.flatValues(size -> new TreeSet<>());
     *
     * // Get as ArrayList with pre-sized capacity
     * List<Integer> allValues = multimap.flatValues(ArrayList::new);
     *
     * // Get as concurrent collection
     * ConcurrentLinkedQueue<Integer> concurrent = multimap.flatValues(size -> new ConcurrentLinkedQueue<>());
     * }</pre>
     *
     * @param <C> the type of collection to return
     * @param supplier function that creates the collection, receives total value count as parameter
     * @return a new collection of the specified type containing all values from this Multimap
     * @throws IllegalArgumentException if {@code supplier} is {@code null}.
     * @see #allValues()
     */
    @Beta
    public <C extends Collection<E>> C flatValues(final IntFunction<C> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final C result = supplier.apply(saturatedTotalValueCount());

        for (final V v : backingMap.values()) {
            result.addAll(v);
        }

        return result;
    }

    /**
     * Returns an {@link EntryStream} of individual key-element pairs in this Multimap.
     *
     * <p>This is a flattened view: for each key, every element in its value collection becomes
     * a separate entry in the returned stream. The iteration order follows the backing map's
     * entry order and the iteration order of each value collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("a", Arrays.asList(1, 2));
     * multimap.putValues("b", Arrays.asList(3));
     *
     * multimap.entryStream().forEach((key, value) -> {
     *     System.out.println(key + ":" + value);
     * });
     * // Possible output (key order follows the backing map and is not guaranteed):
     * // a:1
     * // a:2
     * // b:3
     * }</pre>
     *
     * @return an EntryStream of flattened key-element pairs
     * @see #stream()
     * @see #forEachKeyValue(BiConsumer)
     */
    public EntryStream<K, E> entryStream() {
        return stream().mapToEntry(Fn.identity()).flatmapValue(Fn.identity());
    }

    /**
     * Returns a Stream of key-collection entries for functional-style operations on this Multimap.
     * This enables powerful stream-based transformations and queries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("small", Arrays.asList(1, 2));
     * multimap.putValues("medium", Arrays.asList(3, 4, 5));
     * multimap.putValues("large", Arrays.asList(6, 7, 8, 9));
     *
     * // Find keys with more than 2 values
     * List<String> bigKeys = multimap.stream()
     *     .filter(e -> e.getValue().size() > 2)
     *     .map(Map.Entry::getKey)
     *     .toList();
     * // Returns ["medium", "large"] (element order follows the backing HashMap, not guaranteed)
     *
     * // Calculate total of all values
     * int total = multimap.stream()
     *     .flatMap(e -> Stream.of(e.getValue()))
     *     .mapToInt(Integer::intValue)
     *     .sum();
     *
     * // Group by collection size
     * Map<Integer, List<String>> bySize = multimap.stream()
     *     .collect(Collectors.groupingBy(
     *         e -> e.getValue().size(),
     *         Collectors.mapping(Map.Entry::getKey, Collectors.toList())
     *     ));
     * }</pre>
     *
     * <p>The streamed entries are the same live, {@code setValue}-free entries {@link #iterator()} yields -
     * see that method.</p>
     *
     * @return a Stream of key-collection entries from this Multimap
     * @see #entryStream()
     * @see #iterator()
     */
    public Stream<Map.Entry<K, V>> stream() {
        return Stream.of(entrySetView());
    }

    /**
     * Returns an iterator over the key-collection entries in this Multimap.
     * This iterator allows traversing all mappings in the Multimap.
     *
     * <p>Each element in the iteration is a Map.Entry where:</p>
     * <ul>
     *   <li>The key is a key from the Multimap</li>
     *   <li>The value is the entire collection of values associated with that key</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("group1", Arrays.asList(1, 2, 3));
     * multimap.putValues("group2", Arrays.asList(4, 5));
     *
     * for (Map.Entry<String, List<Integer>> entry : multimap) {
     *     System.out.println(entry.getKey() + " -> " + entry.getValue());
     * }
     * // Output (entry order follows the backing HashMap's iteration order,
     * // which is not guaranteed):
     * // group1 -> [1, 2, 3]
     * // group2 -> [4, 5]
     * }</pre>
     *
     * <p>The returned iterator walks the backing map's own entry set, so it is a live view and inherits that
     * map's concurrent-modification behaviour: with a fail-fast backing map (the default {@link HashMap}, or
     * {@link java.util.LinkedHashMap}/{@link java.util.TreeMap}) adding or removing a key during iteration
     * may raise {@link java.util.ConcurrentModificationException}, whereas a weakly consistent one such as
     * {@link java.util.concurrent.ConcurrentHashMap} does not. {@link Iterator#remove()} delegates to the backing iterator
     * and, when supported, removes the whole mapping, as {@link #removeAll(Object)} would.</p>
     *
     * <p><b>{@link Map.Entry#setValue} is not supported and throws {@link UnsupportedOperationException}.</b>
     * It would install a caller-supplied collection directly into the backing map, bypassing both the value
     * supplier and the key lifecycle: an unmodifiable or otherwise foreign collection installed that way makes
     * {@link #put(Object, Object)}, {@link #putValues(Object, Collection)}, {@link #removeEntry(Object, Object)}
     * and {@link #replaceValues(Object, Collection)} fail for that key until the whole mapping is dropped with
     * {@link #removeAll(Object)}. Use {@link #replaceValues(Object, Collection)} to swap a key's contents.
     * Mutating the value collection an entry hands back is permitted when that collection supports the operation,
     * and bypasses the key lifecycle - see {@link #get(Object)} for what that implies.</p>
     *
     * @return an iterator over the key-collection entries in this Multimap
     * @see #stream()
     * @see #entryStream()
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return entrySetView().iterator();
    }

    /**
     * Returns the live entry view that {@link #iterator()} and {@link #stream()} traverse: the backing map's
     * entries, each wrapped so that {@link Map.Entry#setValue} is refused. Everything else - {@code size},
     * {@code contains}, {@code remove}, {@code clear} and {@code Iterator.remove} - delegates straight to the
     * backing map's own entry set, so the view stays as cheap and as live as that one.
     *
     * @return the cached entry view; never {@code null}
     */
    Set<Map.Entry<K, V>> entrySetView() {
        final Set<Map.Entry<K, V>> result = entries;

        return (result == null) ? entries = new EntrySetView() : result;
    }

    /**
     * The live view over the backing map's entries whose {@link Map.Entry#setValue} is refused.
     * See {@link Multimap#iterator()} for why {@code setValue} is withdrawn while {@code Iterator.remove}
     * is kept.
     */
    final class EntrySetView extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            final Iterator<Map.Entry<K, V>> backingEntryIter = backingMap.entrySet().iterator();

            // A plain Iterator, not ObjIterator: ObjIterator extends ImmutableIterator, whose remove() is
            // deprecated because it never removes anything. This iterator's remove() does remove - it is
            // only setValue that is withdrawn - so it must not claim that type.
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return backingEntryIter.hasNext();
                }

                @Override
                public Map.Entry<K, V> next() {
                    // ImmutableEntry copies the key and the value REFERENCE, so the value collection it hands
                    // back is still the live one this Multimap stores - only setValue is withdrawn. A present
                    // key's collection instance is never swapped by this class, so the copy cannot go stale.
                    return ImmutableEntry.copyOf(backingEntryIter.next());
                }

                @Override
                public void remove() {
                    // Delegated deliberately: iterator() documents that remove() drops the whole mapping.
                    backingEntryIter.remove();
                }
            };
        }

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean contains(final Object o) {
            // AbstractCollection.contains would scan linearly; the backing entry set answers directly.
            return backingMap.entrySet().contains(o);
        }

        @Override
        public boolean remove(final Object o) {
            return backingMap.entrySet().remove(o);
        }

        @Override
        public void clear() {
            backingMap.clear();
        }
    }

    /**
     * Converts this Multimap to a Multiset where each key's count equals the size of its value collection.
     * This is useful for analyzing key frequencies or distribution of values across keys.
     *
     * <p>The conversion works as follows:</p>
     * <ul>
     *   <li>Each key from the Multimap becomes an element in the Multiset</li>
     *   <li>The count for each key equals the number of values associated with that key</li>
     *   <li>Under this class's normal mutation invariant, every key has at least one value and therefore a positive count.
     *       If a live value collection or wrapped backing map was externally emptied, that zero-count key is omitted from the Multiset.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> multimap = N.newListMultimap();
     * multimap.putValues("user1", Arrays.asList("action1", "action2", "action3"));
     * multimap.putValues("user2", Arrays.asList("action1"));
     * multimap.putValues("user3", Arrays.asList("action1", "action2"));
     *
     * Multiset<String> activityCount = multimap.toMultiset();
     * // Result: {"user1"=3, "user2"=1, "user3"=2}
     *
     * // Find most active user
     * String mostActive = activityCount.entrySet().stream()
     *     .max(Comparator.comparingInt(Multiset.Entry::count))
     *     .map(Multiset.Entry::element)
     *     .orElse(null);
     * // Returns "user1"
     * }</pre>
     *
     * @return a new Multiset where each key's count equals its value collection size
     * @see #toMap()
     * @see Multiset
     */
    public Multiset<K> toMultiset() {
        // Use the map supplier, not backingMap.getClass(): a Class round-trip cannot carry a custom
        // comparator, so a TreeMap-backed multimap with non-Comparable keys would throw CCE.
        final Multiset<K> multiset = new Multiset<>(mapSupplier);

        for (final Map.Entry<K, V> entry : backingMap.entrySet()) {
            multiset.setCount(entry.getKey(), entry.getValue().size());
        }

        return multiset;
    }

    /**
     * Converts this Multimap to a standard Map with independent collection copies.
     * Each value collection is copied; the keys and individual elements themselves are shared.
     *
     * <p>The returned Map:</p>
     * <ul>
     *   <li>Uses a compatible independent map (common concrete types and sorted-map comparators are preserved;
     *       uninstantiable runtime wrapper types fall back to HashMap)</li>
     *   <li>Contains copies of all value collections (not references to original collections)</li>
     *   <li>Has independent map and collection structure; mutations to shared keys or element objects remain visible through both</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("group1", Arrays.asList(1, 2, 3));
     * multimap.putValues("group2", Arrays.asList(4, 5));
     *
     * Map<String, List<Integer>> map = multimap.toMap();
     * map.get("group1").add(99);     // only affects the map, not the multimap
     *
     * multimap.put("group1", 100);   // only affects the multimap, not the map
     * }</pre>
     *
     * @return a new independent Map containing all key-collection pairs from this Multimap
     * @see #toMap(IntFunction)
     * @see #toMultiset()
     */
    public Map<K, V> toMap() {
        final Map<K, V> result = Maps.newTargetMap(backingMap);

        V val = null;

        for (final Map.Entry<K, V> e : backingMap.entrySet()) {
            val = valueSupplier.get();
            val.addAll(e.getValue());

            result.put(e.getKey(), val);
        }

        return result;
    }

    /**
     * Converts this Multimap to a Map of a specific type with independent collection copies.
     * This method provides control over the type of Map to create.
     *
     * <p>The supplier function receives the size of this Multimap as a hint for initial capacity,
     * which can improve performance by reducing rehashing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("a", Arrays.asList(1, 2));
     * multimap.putValues("b", Arrays.asList(3, 4));
     *
     * // Convert to TreeMap for sorted keys
     * TreeMap<String, List<Integer>> treeMap = multimap.toMap(size -> new TreeMap<>());
     *
     * // Convert to LinkedHashMap for insertion order. `size` is an expected key count, so convert it to
     * // a capacity with N.newLinkedHashMap rather than passing it straight to the capacity constructor.
     * LinkedHashMap<String, List<Integer>> linkedMap = multimap.toMap(N::newLinkedHashMap);
     *
     * // Convert to concurrent map
     * ConcurrentHashMap<String, List<Integer>> concurrentMap =
     *     multimap.toMap(ConcurrentHashMap::new);
     * }</pre>
     *
     * @param <M> the specific Map type to create
     * @param supplier function that creates a new Map instance, receives Multimap size as parameter
     * @return a new Map of the specified type containing all key-collection pairs
     * @throws IllegalArgumentException if {@code supplier} is {@code null}.
     * @throws NullPointerException if this Multimap is nonempty and {@code supplier} returns {@code null}, or a stored key is {@code null} and the supplied map rejects null-key insertion
     * @throws ClassCastException if a stored key cannot be compared or inserted into the supplied map
     * @throws UnsupportedOperationException if entries are copied and the supplied map does not support insertion
     * @see #toMap()
     */
    public <M extends Map<K, V>> M toMap(final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, NullPointerException, ClassCastException, UnsupportedOperationException {
        N.checkArgNotNull(supplier, cs.supplier);

        final M result = supplier.apply(keyCount());

        // result.putAll(backingMap);

        V val = null;

        for (final Map.Entry<K, V> e : backingMap.entrySet()) {
            val = valueSupplier.get();
            val.addAll(e.getValue());

            result.put(e.getKey(), val);
        }

        return result;
    }

    /**
     * Removes all key-value pairs from this Multimap, leaving it empty.
     * After this call, {@link #isEmpty()} will return {@code true} and {@link #totalValueCount()} will return 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("key1", Arrays.asList(1, 2, 3));
     * multimap.putValues("key2", Arrays.asList(4, 5));
     *
     * multimap.clear();
     * // multimap is now empty
     * // keyCount() returns 0
     * // isEmpty() returns true
     * }</pre>
     *
     */
    public void clear() {
        backingMap.clear();
    }

    /**
     * Returns the number of distinct keys in this Multimap.
     *
     * <p>This method is deprecated. Use {@link #keyCount()} for clarity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("a", 1);
     * int keys = multimap.size();       // deprecated; prefer multimap.keyCount()
     * }</pre>
     *
     * @return the number of distinct keys in this Multimap
     * @deprecated use {@code keyCount()} instead.
     * @see #keyCount()
     */
    @Deprecated
    public int size() {
        return backingMap.size();
    }

    /**
     * Returns the number of distinct keys in this Multimap.
     *
     * <p>This counts keys only, not the total number of individual values.
     * To count all values across all collections, use {@link #totalValueCount()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("key1", Arrays.asList(1, 2, 3));
     * multimap.putValues("key2", Arrays.asList(4, 5));
     *
     * int keyCount = multimap.keyCount();            // returns 2
     * int valueCount = multimap.totalValueCount();   // returns 5
     * }</pre>
     *
     * @return the number of distinct keys in this Multimap
     * @see #keySet()
     * @see #totalValueCount()
     * @see #isEmpty()
     */
    public int keyCount() {
        return backingMap.size();
    }

    /**
     * Returns the total count of all individual values across all value collections in this Multimap.
     * This is the sum of the sizes of every value collection (the number of key-element pairs).
     *
     * <p>Unlike {@link #keyCount()} which counts distinct keys, this method counts every individual
     * value. For example, if you have 3 keys with 3, 2, and 4 values respectively, this method
     * returns 9 (3+2+4), while {@link #keyCount()} returns 3.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("group1", Arrays.asList(1, 2, 3));
     * multimap.putValues("group2", Arrays.asList(4, 5));
     * multimap.putValues("group3", Arrays.asList(6, 7, 8, 9));
     *
     * int totalValues = multimap.totalValueCount();   // returns 9
     * int keyCount = multimap.keyCount();             // returns 3
     * }</pre>
     *
     * @return the total count of all individual values in all value collections
     * @throws ArithmeticException if the total count exceeds {@link Integer#MAX_VALUE}
     * @see #keyCount()
     * @see #isEmpty()
     */
    public int totalValueCount() throws ArithmeticException {
        return Numbers.toIntExact(totalValueCountAsLong());
    }

    private long totalValueCountAsLong() {
        long count = 0;

        for (final V v : backingMap.values()) {
            count += v.size();
        }

        return count;
    }

    private int saturatedTotalValueCount() {
        return (int) Math.min(totalValueCountAsLong(), Integer.MAX_VALUE);
    }

    /**
     * Checks if this Multimap contains no keys.
     * Returns {@code true} if there are no keys in the Multimap, {@code false} otherwise.
     *
     * <p>This method checks whether the Multimap has any keys. Mutations through this class normally
     * remove a key when its value collection becomes empty. However, callers can mutate a live value
     * collection returned by {@link #get(Object)} or an externally supplied backing map, so a retained
     * empty collection can make this method return {@code false} while {@link #totalValueCount()} returns 0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     *
     * // Initially empty
     * multimap.isEmpty();   // returns true
     *
     * // After adding values
     * multimap.put("key1", 1);
     * multimap.isEmpty();   // returns false
     *
     * // After removing all keys
     * multimap.removeAll("key1");
     * multimap.isEmpty();   // returns true
     *
     * // Conditional processing
     * if (!multimap.isEmpty()) {
     *     multimap.forEachKeyValue((key, value) -> {
     *         System.out.println(key + ": " + value);
     *     });
     * }
     *
     * // With acceptIfNotEmpty
     * multimap.acceptIfNotEmpty(mm -> {
     *     mm.forEachKeyValue((k, v) -> System.out.println(k + ": " + v));
     * }).orElse(() -> {
     *     System.out.println("No data available");
     * });
     * }</pre>
     *
     * @return {@code true} if the Multimap contains no keys, {@code false} otherwise
     * @see #totalValueCount()
     * @see #acceptIfNotEmpty(Throwables.Consumer)
     */
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Applies a function to this Multimap and returns the result.
     * This enables functional-style transformations and method chaining.
     *
     * <p>This method is useful for:</p>
     * <ul>
     *   <li>Transforming the Multimap into another data structure</li>
     *   <li>Extracting computed values from the Multimap</li>
     *   <li>Integrating with functional pipelines</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("a", Arrays.asList(1, 2, 3));
     * multimap.putValues("b", Arrays.asList(4, 5));
     *
     * // Transform to summary statistics
     * String summary = multimap.apply(mm -> {
     *     int totalKeys = mm.keyCount();
     *     int totalValues = mm.totalValueCount();
     *     return String.format("Keys: %d, Values: %d", totalKeys, totalValues);
     * });
     * // Returns "Keys: 2, Values: 5"
     *
     * // Convert to another representation
     * Map<String, List<Integer>> data = multimap.apply(mm -> mm.toMap());
     *
     * // Chain with other operations
     * int maxCollectionSize = multimap.apply(mm ->
     *     mm.stream()
     *       .map(e -> e.getValue().size())
     *       .max(Integer::compare)
     *       .orElse(0)
     * );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <X> the type of exception that may be thrown
     * @param func the function to apply to this Multimap
     * @return the result of applying the function
     * @throws IllegalArgumentException if {@code func} is {@code null}.
     * @throws X if the function throws an exception
     * @see #applyIfNotEmpty(Throwables.Function)
     * @see #accept(Throwables.Consumer)
     */
    public <R, X extends Exception> R apply(final Throwables.Function<? super Multimap<K, E, V>, R, X> func) throws IllegalArgumentException, X {
        N.checkArgNotNull(func, cs.func);

        return func.apply(this);
    }

    /**
     * Applies a function to this Multimap only if it's not empty, returning the result wrapped in an Optional.
     * This provides safe handling of potentially empty Multimaps in functional pipelines.
     *
     * <p>If the Multimap is empty, returns {@link Optional#empty()} without calling the function.
     * If the Multimap is not empty, applies the function and wraps the result in an Optional.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     *
     * // Empty multimap - returns Optional.empty()
     * Optional<Integer> result1 = multimap.applyIfNotEmpty(mm ->
     *     mm.allValues().stream().mapToInt(Integer::intValue).sum()
     * );
     * // result1 is empty
     *
     * // Non-empty multimap - applies function
     * multimap.putValues("nums", Arrays.asList(1, 2, 3));
     * Optional<Integer> result2 = multimap.applyIfNotEmpty(mm ->
     *     mm.allValues().stream().mapToInt(Integer::intValue).sum()
     * );
     * // result2 contains 6
     *
     * // Chain with Optional operations
     * String message = multimap.applyIfNotEmpty(mm ->
     *     "Total: " + mm.totalValueCount()
     * ).orElse("No data");
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <X> the type of exception that may be thrown
     * @param func the function to apply if the Multimap is not empty
     * @return an Optional containing the result if this Multimap is not empty, otherwise an empty
     *         Optional.
     * @throws IllegalArgumentException if {@code func} is {@code null}.
     * @throws NullPointerException if the function returns {@code null}
     * @throws X if the function throws an exception
     * @see #apply(Throwables.Function)
     * @see #acceptIfNotEmpty(Throwables.Consumer)
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Multimap<K, E, V>, R, X> func)
            throws IllegalArgumentException, X {
        N.checkArgNotNull(func, cs.func);

        return isEmpty() ? Optional.empty() : Optional.of(func.apply(this));
    }

    /**
     * Performs an action on this Multimap, useful for side effects and method chaining.
     * This method enables functional-style operations that don't produce a return value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.putValues("scores", Arrays.asList(85, 92, 78));
     *
     * // Perform logging
     * multimap.accept(mm -> {
     *     System.out.println("Key count: " + mm.keyCount());
     *     System.out.println("Total values: " + mm.totalValueCount());
     * });
     *
     * // Perform validation
     * multimap.accept(mm -> {
     *     if (mm.totalValueCount() > 1000) {
     *         throw new IllegalStateException("Too many values");
     *     }
     * });
     *
     * // Method chaining with side effects
     * multimap.accept(mm -> System.out.println("Processing " + mm.keyCount() + " keys"));
     * Map<String, List<Integer>> snapshot = multimap.apply(mm -> mm.toMap());
     * }</pre>
     *
     * @param <X> the type of exception that may be thrown
     * @param action the consumer action to perform on this Multimap
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws X if the action throws an exception
     * @see #acceptIfNotEmpty(Throwables.Consumer)
     * @see #apply(Throwables.Function)
     */
    public <X extends Exception> void accept(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws IllegalArgumentException, X {
        N.checkArgNotNull(action, cs.action);

        action.accept(this);
    }

    /**
     * Performs an action on this Multimap only if it's not empty, with support for else clause.
     * This enables conditional processing with fallback behavior for empty Multimaps.
     *
     * <p>The action is only executed if {@link #isEmpty()} returns {@code false}, i.e. this Multimap
     * has at least one key. Returns an {@link OrElse} instance that allows chaining an alternative
     * action for the empty case.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     *
     * // Simple conditional action
     * multimap.acceptIfNotEmpty(mm -> {
     *     System.out.println("Processing " + mm.keyCount() + " keys");
     *     // Process the multimap
     * });
     *
     * // With else clause
     * multimap.acceptIfNotEmpty(mm -> {
     *     mm.forEachKeyValue((k, v) -> System.out.println(k + ": " + v));
     * }).orElse(() -> {
     *     System.out.println("No data to process");
     * });
     *
     * // More complex example
     * multimap.putValues("data", Arrays.asList(1, 2, 3));
     * multimap.acceptIfNotEmpty(mm -> {
     *     mm.forEachKeyValue((key, value) -> {
     *         System.out.println(key + ": " + value);
     *     });
     * }).orElse(() -> {
     *     System.out.println("Using default data");
     * });
     * }</pre>
     *
     * @param <X> the type of exception that may be thrown
     * @param action the consumer action to perform if the Multimap is not empty
     * @return an OrElse instance for chaining an alternative action for the empty case
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws X if the action throws an exception
     * @see #accept(Throwables.Consumer)
     * @see #applyIfNotEmpty(Throwables.Function)
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Multimap<K, E, V>, X> action) throws IllegalArgumentException, X {
        N.checkArgNotNull(action, cs.action);

        return If.is(!isEmpty()).then(this, action);
    }

    /**
     * Returns the hash code value for this Multimap.
     * The hash code is computed from the backing map's hash code, which in turn is based
     * on the hash codes of all key-value collection pairs.
     *
     * <p>Two Multimaps that are equal according to the {@link #equals(Object)} method have the same hash code -
     * which is what hash-based collections like HashMap and HashSet rely on - provided both backing maps decide
     * key equality with {@code equals} and each value collection's {@code hashCode()} is consistent with its own
     * {@code equals()} ({@link List#hashCode()} and {@link Set#hashCode()} specify such a hash code, while
     * {@link Collection#hashCode()} stipulates nothing). Deciding equality some other way breaks the guarantee
     * even when {@link Map#hashCode()} is implemented exactly as specified, because that hash code is built from
     * {@code key.hashCode()}: two multimaps backed by {@link java.util.TreeMap}s that share one comparator
     * inconsistent with {@code equals} can compare equal in both directions and still report different hash
     * codes - under {@code String.CASE_INSENSITIVE_ORDER}, {@code A=[1]} and {@code a=[1]} hash to 97 and 65 -
     * and so can an {@link java.util.IdentityHashMap}-backed multimap compared with a {@link HashMap}-backed
     * one.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> m1 = N.newListMultimap();
     * m1.putValues("a", Arrays.asList(1, 2));
     * ListMultimap<String, Integer> m2 = N.newListMultimap();
     * m2.putValues("a", Arrays.asList(1, 2));
     * boolean sameHash = m1.hashCode() == m2.hashCode();   // true (both are HashMap-backed, so equal implies the same hash)
     *
     * m2.put("a", 3);
     * boolean changedHash = m1.hashCode() != m2.hashCode();   // true for these contents
     *
     * int emptyHash = N.newListMultimap().hashCode();   // 0 (the empty backing map's hash code)
     * }</pre>
     *
     * @return the hash code value for this Multimap
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    /**
     * Compares the specified object with this Multimap for equality.
     * Returns {@code true} if the specified object is also a Multimap and both have
     * identical key-value collection mappings.
     *
     * <p>Two Multimaps are considered equal if:</p>
     * <ul>
     *   <li>They have the same set of keys</li>
     *   <li>For each key, the associated value collections are equal</li>
     *   <li>The equality check uses the backing map's equals method</li>
     * </ul>
     *
     * <p><b>Equality is delegated to the backing maps, so it is only well defined between multimaps whose
     * backing maps share the same key equivalence.</b> Two multimaps built on maps that judge keys
     * differently - a {@link HashMap} versus an {@link java.util.IdentityHashMap}, or versus a
     * {@link java.util.TreeMap} whose comparator is inconsistent with {@code equals} - can compare unequal
     * in one direction and equal in the other, and can report different hash codes while comparing equal.
     * This is inherited from {@link Map#equals(Object)}, which probes the other map with its own lookup
     * rules. Compare only multimaps with compatible backing maps.</p>
     *
     * <p><b>Note:</b> Equality is delegated to the backing maps, so the value-collection types
     * must also match: a {@code List} value collection never equals a {@code Set} value collection.
     * Consequently a nonempty {@link ListMultimap} and a nonempty {@link SetMultimap} are never equal even when they
     * hold the same logical key-to-elements mappings. Empty instances compare equal. The contents and ordering of each value
     * collection also matter (e.g., {@code [1, 2, 2]} is not equal to {@code [1, 2]}, and
     * {@code [1, 2]} is not equal to {@code [2, 1]}, for lists).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> m1 = N.newListMultimap();
     * m1.putValues("a", Arrays.asList(1, 2));
     * ListMultimap<String, Integer> m2 = N.newListMultimap();
     * m2.putValues("a", Arrays.asList(1, 2));
     * m1.equals(m2);   // returns true (same mappings)
     * m1.equals(m1);   // returns true (same instance)
     *
     * m2.put("a", 3);
     * m1.equals(m2);   // returns false ("a" maps to [1, 2] vs [1, 2, 3])
     *
     * ListMultimap<String, Integer> m3 = N.newListMultimap();
     * m3.putValues("a", Arrays.asList(2, 1));
     * m1.equals(m3);                 // returns false (list order differs: [1, 2] vs [2, 1])
     *
     * m1.equals("not a multimap");   // returns false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this Multimap
     * @return {@code true} if the specified object is equal to this Multimap, {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multimap && backingMap.equals(((Multimap<?, ?, ?>) obj).backingMap));
    }

    /**
     * Returns a string representation of this Multimap in standard Map format.
     * The string format matches that of a standard Java Map, showing all key-collection pairs.
     *
     * <p>The string representation consists of:</p>
     * <ul>
     *   <li>Key-value mappings enclosed in braces: {@code "{}"}</li>
     *   <li>Adjacent mappings separated by comma and space: {@code ", "}</li>
     *   <li>Each mapping rendered as: {@code key=collection}</li>
     *   <li>The order depends on the backing map type (HashMap, LinkedHashMap, etc.)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("key1", 1);
     * multimap.put("key1", 2);
     * multimap.toString();              // returns "{key1=[1, 2]}"
     *
     * N.newListMultimap().toString();   // returns "{}" (empty multimap)
     * }</pre>
     *
     * <p><b>Note:</b> This method is primarily intended for debugging and logging.
     * The exact format may vary depending on the backing map and collection implementations
     * (for example, key order is not guaranteed for a HashMap-backed multimap).</p>
     *
     * @return a string representation of this Multimap
     */
    @Override
    public String toString() {
        return backingMap.toString();
    }

    /**
     * Creates the live, flattened view of all individual values that {@link #allValues()} caches and returns.
     *
     * @return a new unmodifiable-in-practice view collection over every key-element pair's element
     */
    Collection<E> createValues() {
        return new Values();
    }

    /**
     * The live, flattened view over every element of every value collection in the enclosing Multimap.
     * It is created once by {@code createValues()} and exposed (wrapped as immutable) by
     * {@link Multimap#allValues()}; it holds no data of its own and reflects subsequent changes to the
     * Multimap.
     */
    final class Values extends AbstractCollection<E> {
        @Override
        public Iterator<E> iterator() {
            return valueIterator();
        }

        @Override
        public Spliterator<E> spliterator() {
            return valueSpliterator();
        }

        @Override
        public int size() {
            return Multimap.this.saturatedTotalValueCount();
        }

        @Override
        public boolean contains(Object obj) {
            return Multimap.this.containsValue(obj);
        }
    }

    /**
     * Returns an iterator over every element of every value collection, concatenated in the backing
     * map's entry order and, within each entry, in that value collection's own iteration order.
     *
     * @return an iterator over all individual values in this Multimap
     */
    Iterator<E> valueIterator() {
        return Iterators.concatIterables(Multimap.this.backingMap.values());
    }

    /**
     * Returns a spliterator over every element of every value collection, sized with the exact total
     * value count - which may exceed {@link Integer#MAX_VALUE}, and so may exceed what
     * {@link #allValues()}{@code .size()} can report.
     *
     * <p>No characteristics are requested; {@link Spliterators#spliterator(Iterator, long, int)} nonetheless
     * reports {@link Spliterator#SIZED} and {@link Spliterator#SUBSIZED}, which it adds unconditionally.</p>
     *
     * @return a spliterator over all individual values in this Multimap
     */
    Spliterator<E> valueSpliterator() {
        return Spliterators.spliterator(valueIterator(), totalValueCountAsLong(), 0);
    }
}
