/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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

package com.landawn.abacus.util.stream;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.Difference;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Iterables;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;

/**
 * A specialized stream implementation for processing sequences of Map.Entry elements with functional-style operations.
 * This final class extends {@link StreamBase} and provides comprehensive entry-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for key-value pair data types.
 *
 * <p>EntryStream represents a sequence of Map.Entry elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient and convenient alternative to generic {@link Stream} when working specifically with
 * key-value pairs, offering entry-specific utility methods for map-like computations and transformations.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for Map.Entry operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for key-value pair processing with specialized operations</li>
 *   <li><b>Map Integration:</b> Seamless conversion to/from Map instances and map-like operations</li>
 *   <li><b>Key/Value Operations:</b> Dedicated methods for key and value transformations</li>
 *   <li><b>Grouping Support:</b> Advanced grouping and aggregation capabilities</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large entry datasets</li>
 *   <li><b>Resource Management:</b> Automatic resource cleanup and proper stream lifecycle management</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Stream Lifecycle:</b>
 * <ul>
 *   <li>A stream can only be consumed <b>ONCE</b> - after a terminal operation, the stream is closed</li>
 *   <li>Attempting to reuse a closed stream throws {@code IllegalStateException}</li>
 *   <li>Streams are automatically closed after terminal operations complete</li>
 *   <li>Use try-with-resources for streams created from I/O sources</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Map Processing:</b> Transforming, filtering, and aggregating map data</li>
 *   <li><b>Data Analysis:</b> Processing key-value datasets and performing statistical operations</li>
 *   <li><b>Configuration Management:</b> Processing configuration entries and settings</li>
 *   <li><b>Data Transformation:</b> Converting between different map formats and structures</li>
 *   <li><b>Database Results:</b> Processing query results with key-value relationships</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic entry stream operations
 * Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87, "Charlie", 92);
 * EntryStream.of(scores)
 *     .filterByValue(score -> score > 90)   // keeps high scores
 *     .mapValue(score -> score + 5)         // adds bonus points
 *     .toMap();                             // {Alice=100, Charlie=97}
 *
 * // Key and value transformations
 * EntryStream.of(userMap)
 *     .<String>mapKey(key -> key.toLowerCase())      // maps keys to lowercase
 *     .filterByKey(key -> key.startsWith("admin"))   // filters admin users
 *     .mapValue(User::getRole)                       // maps each user to its role
 *     .toMap();
 *
 * // Grouping and aggregation
 * EntryStream.of(salesData)
 *     .groupBy(Map.Entry::getValue, Map.Entry::getKey)  // maps each sales amount to its keys
 *     .mapValue(List::size)                             // maps each group to its size
 *     .sortedByValue(Integer::compareTo)                // sorts by count
 *     .toMap();
 *
 * // Complex transformations with parallel processing
 * Map<String, Double> averages = EntryStream.of(measurements)
 *     .parallel()                                 // uses parallel processing
 *     .filterByKey(key -> key.contains("temp"))   // keeps temperature measurements
 *     .mapValue(values -> values.stream()
 *         .mapToDouble(Double::doubleValue)
 *         .average().orElse(0.0))            // maps each group to its average
 *     .filterByValue(avg -> avg > 20.0)      // filters valid temperatures
 *     .toMap();
 *
 * // Integration with other stream types
 * Stream<String> reports = EntryStream.of(data)
 *     .entries()
 *     .map(entry -> entry.getKey() + ": " + entry.getValue()) // maps each entry to a string
 *     .filter(report -> report.length() > 10);                // filters by length
 * }</pre>
 *
 * <p><b>Entry-Specific Operations:</b>
 * <ul>
 *   <li>{@code filterByKey()}, {@code filterByValue()} - Filter by key or value conditions</li>
 *   <li>{@code mapKey()}, {@code mapValue()} - Transform keys or values independently</li>
 *   <li>{@code flatMapKey()}, {@code flatMapValue()} - Flatten key or value transformations</li>
 *   <li>{@code groupBy()} - Group entries by key, value, or custom criteria</li>
 *   <li>{@code sortedByKey(Comparator)}, {@code sortedByValue(Comparator)} - Sort by key or value comparators</li>
 *   <li>{@code distinctByKey()}, {@code distinctByValue()} - Remove duplicates by key or value</li>
 *   <li>{@code toMap()}, {@code toMultimap()} - Convert to map collections</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use EntryStream instead of {@code Stream<Map.Entry>} for better performance and convenience</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 entries)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * <p><b>{@code EntryStream} vs. {@link Stream} &mdash; how they differ:</b>
 * An {@code EntryStream<K, V>} is essentially a {@code Stream<Map.Entry<K, V>>} enriched with a
 * key/value-aware API. Both live in the same package and share the same execution model &mdash; lazy,
 * single-use, auto-closeable, and runnable either sequentially or in {@code parallel()}. They differ in
 * element shape and the operations they expose:
 *
 * <table border="1">
 *   <caption>{@code EntryStream} compared with {@code Stream}</caption>
 *   <tr><th>Aspect</th><th>{@code EntryStream<K, V>}</th><th>{@code Stream<T>}</th></tr>
 *   <tr>
 *     <td>Element shape</td>
 *     <td>A stream of key-value pairs ({@code Map.Entry<K, V>}); two type parameters.</td>
 *     <td>A stream of arbitrary single elements {@code T}; one type parameter.</td>
 *   </tr>
 *   <tr>
 *     <td>Key/value-aware operations</td>
 *     <td>Dedicated operations act on one side of the entry without destructuring it:
 *         {@code mapKey}/{@code mapValue}, {@code filterByKey}/{@code filterByValue},
 *         {@code flatMapKey}/{@code flatMapValue}, {@code sortedByKey}/{@code sortedByValue},
 *         {@code distinctByKey}/{@code distinctByValue}, plus {@link #keys()} and {@link #values()}.</td>
 *     <td>No entry-aware operations; you destructure each entry by hand, e.g.
 *         {@code .map(e -> f(e.getKey(), e.getValue()))}.</td>
 *   </tr>
 *   <tr>
 *     <td>Map integration</td>
 *     <td>Starts directly from a map ({@link #of(Map)}); terminals such as {@code toMap()} and
 *         {@code toMultimap()} rebuild a map without extra key/value extractors.</td>
 *     <td>Build a map with {@code Collectors.toMap(keyMapper, valueMapper)}, supplying the extractors
 *         explicitly.</td>
 *   </tr>
 * </table>
 *
 * <p><b>Prefer {@code EntryStream} when:</b>
 * <ul>
 *   <li>You are transforming a {@code Map} (or any sequence of key-value pairs) and want to act on keys
 *       and values independently &mdash; rename/normalize keys, transform or aggregate values, filter by
 *       key or by value, group, or sort by key or value &mdash; without {@code getKey()}/{@code getValue()}
 *       boilerplate on every element.</li>
 *   <li>The result is itself a map: {@link #of(Map)} &hellip; {@code toMap()} reads cleanly end to end.</li>
 * </ul>
 *
 * <p><b>Prefer {@link Stream} when:</b>
 * <ul>
 *   <li>The elements are not key-value pairs (general single-element processing).</li>
 *   <li>You need a primitive specialization ({@link IntStream} / {@code LongStream} / {@code DoubleStream}),
 *       which process primitives rather than object entries.</li>
 *   <li>A single, ad-hoc entry transformation is simpler written inline than by adopting the entry-aware API.</li>
 * </ul>
 *
 * <p>The two interoperate freely: turn a {@link Stream} into an {@code EntryStream} with
 * {@code Stream.mapToEntry(keyMapper, valueMapper)} (or {@code groupByToEntry} / {@code flatMapToEntry}),
 * and turn an {@code EntryStream} back into a {@link Stream} with {@link #entries()}, {@link #keys()},
 * or {@link #values()}.
 *
 * <br />
 * Note: This class includes codes copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License, version 2.0.
 * <br />
 * <br />
 *
 * @param <K> the type of the keys in the entries of the stream
 * @param <V> the type of the values in the entries of the stream
 *
 * @see StreamBase
 * @see Stream
 * @see Map.Entry
 * @see ObjIterator
 * @see Optional
 * @see Indexed
 * @see java.util.stream.Stream
 * @see java.util.Map
 * @see java.util.stream.Collectors
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.Comparators
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@com.landawn.abacus.annotation.Immutable
@LazyEvaluation
public final class EntryStream<K, V> extends
        StreamBase<Map.Entry<K, V>, Object[], Predicate<? super Map.Entry<K, V>>, Consumer<? super Map.Entry<K, V>>, Optional<Map.Entry<K, V>>, Indexed<Map.Entry<K, V>>, ObjIterator<Map.Entry<K, V>>, EntryStream<K, V>> {

    private static final Function<Map<Object, Object>, Stream<Map.Entry<Object, Object>>> mapper_func = Stream::of;

    /**
     * Sentinel entry used in mapKeyPartial and mapValuePartial methods to mark entries to be filtered out.
     * Uses unique objects as key and value to ensure this sentinel cannot be accidentally created by user code.
     */
    @SuppressWarnings("rawtypes")
    private static final Map.Entry NONE_ENTRY_SENTINEL = new SimpleImmutableEntry<>(new Object(), new Object());

    final Map<K, V> _map; //NOSONAR
    final Stream<Map.Entry<K, V>> _stream; //NOSONAR

    EntryStream(final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        this(null, s);
    }

    @SuppressWarnings("rawtypes")
    EntryStream(final Map<? extends K, ? extends V> m, final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        super(s.isSorted(), (Comparator<Map.Entry<K, V>>) s.comparator(), s.closeHandlers());
        _map = (Map) m;
        _stream = (Stream<Map.Entry<K, V>>) s;
    }

    /**
     * Returns a stream consisting of the keys from the entries in this EntryStream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = EntryStream.of("a", 1, "b", 2, "c", 3)
     *                                 .keys()
     *                                 .toList();   // returns ["a", "b", "c"]
     * }</pre>
     *
     * @return a new Stream consisting of the keys from the entries in this EntryStream
     * @see #values()
     * @see Stream#map(Function)
     * @see Fn#key()
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<K> keys() {
        _stream.assertNotClosed();

        // It won't be parallel stream if m != null.
        if (_map != null) {
            return Stream.of(_map.keySet()).onClose(newCloseHandler(_stream));
        }

        final Function<Map.Entry<K, V>, K> func = Fn.key();

        if (isParallel()) {
            return _stream.psp(ss -> ss.map(func));
        } else {
            return _stream.map(func);
        }
    }

    /**
     * Returns a stream consisting of the values from the entries in this EntryStream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> values = EntryStream.of("a", 1, "b", 2, "c", 3)
     *                                    .values()
     *                                    .toList();   // returns [1, 2, 3]
     * }</pre>
     *
     * @return a new Stream consisting of the values from the entries in this EntryStream
     * @see #keys()
     * @see Stream#map(Function)
     * @see Fn#value()
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<V> values() {
        _stream.assertNotClosed();

        // It won't be parallel stream if m != null.
        if (_map != null) {
            return Stream.of(_map.values()).onClose(newCloseHandler(_stream));
        }

        final Function<Map.Entry<K, V>, V> func = Fn.value();

        if (isParallel()) {
            return _stream.psp(ss -> ss.map(func));
        } else {
            return _stream.map(func);
        }
    }

    /**
     * Returns a stream consisting of the entries (Map.Entry objects) in this EntryStream.
     * This is an intermediate operation that essentially returns the underlying stream of entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * List<Map.Entry<String, Integer>> entries = EntryStream.of(map)
     *                                                         .entries()
     *                                                         .toList();
     * // returns list of Map.Entry objects
     * }</pre>
     *
     * @return a new Stream consisting of the entries in this EntryStream
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<Map.Entry<K, V>> entries() {
        _stream.assertNotClosed();

        return _stream;
    }

    /**
     * Returns a new EntryStream with inverted key-value pairs.
     * Each entry (k, v) in this stream is transformed to (v, k) in the returned stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<Integer, String> inverted = EntryStream.of(map)
     *                                             .invert()
     *                                             .toMap();   // returns {1="a", 2="b", 3="c"}
     * }</pre>
     *
     * @return a new EntryStream with inverted key-value pairs
     * @see #map(Function)
     * @see #map(BiFunction)
     * @see Stream#map(Function)
     * @see Fn#invert()
     */
    @SequentialOnly
    @IntermediateOp
    public EntryStream<V, K> invert() {
        final Function<Map.Entry<K, V>, Map.Entry<V, K>> mapper = Fn.invert();

        if (isParallel()) {
            return of(_stream.<Stream<Map.Entry<V, K>>> psp(ss -> ss.map(mapper)));
        } else {
            return map(mapper);
        }
    }

    /**
     * Returns a stream consisting of the elements of this stream whose keys are instances of the given class.
     * This is an intermediate operation that filters entries based on the runtime type of their keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Object, String> map = new HashMap<>();
     * map.put("a", "string key");
     * map.put(1, "integer key");
     * map.put(2.5, "double key");
     *
     * Map<Integer, String> integerKeys = EntryStream.of(map)
     *                                                .selectByKey(Integer.class)
     *                                                .toMap();   // returns {1="integer key"}
     * }</pre>
     *
     * @param <KK> the type of keys to select
     * @param clazz the class to filter the keys by
     * @return a new EntryStream with keys filtered by the specified class
     * @see #selectByValue(Class)
     * @see #filterByKey(Predicate)
     */
    @SequentialOnly
    @IntermediateOp
    public <KK> EntryStream<KK, V> selectByKey(final Class<KK> clazz) {
        if (isParallel()) {
            //noinspection resource
            return (EntryStream<KK, V>) sequential().filterByKey(Fn.instanceOf(clazz))
                    .parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return (EntryStream<KK, V>) filterByKey(Fn.instanceOf(clazz));
        }
    }

    /**
     * Returns a stream consisting of the elements of this stream whose values are instances of the given class.
     * This is an intermediate operation that filters entries based on the runtime type of their values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("a", "string value");
     * map.put("b", 42);
     * map.put("c", 3.14);
     *
     * Map<String, Integer> integerValues = EntryStream.of(map)
     *                                                  .selectByValue(Integer.class)
     *                                                  .toMap();   // returns {"b"=42}
     * }</pre>
     *
     * @param <VV> the type of values to select
     * @param clazz the class to filter the values by
     * @return a new EntryStream with values filtered by the specified class
     * @see #selectByKey(Class)
     * @see #filterByValue(Predicate)
     */
    @SequentialOnly
    @IntermediateOp
    public <VV> EntryStream<K, VV> selectByValue(final Class<VV> clazz) {
        if (isParallel()) {
            //noinspection resource
            return (EntryStream<K, VV>) sequential().filterByValue(Fn.instanceOf(clazz))
                    .parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return (EntryStream<K, VV>) filterByValue(Fn.instanceOf(clazz));
        }
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> filtered = EntryStream.of(map)
     *                                             .filter(e -> e.getValue() > 1)
     *                                             .toMap();   // returns {"b"=2, "c"=3}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry to determine if it should be included
     * @return a new EntryStream consisting of entries that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @Override
    public EntryStream<K, V> filter(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(_stream.filter(predicate));
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given bi-predicate.
     * This is an intermediate operation that allows filtering based on both key and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("apple", 5, "banana", 3, "cherry", 7);
     * Map<String, Integer> filtered = EntryStream.of(map)
     *                                             .filter((k, v) -> k.length() > 5 && v > 4)
     *                                             .toMap();   // returns {"cherry"=7}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless bi-predicate to apply to each key-value pair
     * @return a new EntryStream consisting of entries that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> filter(final BiPredicate<? super K, ? super V> predicate) {

        return of(_stream.filter(Fn.Entries.p(predicate)));
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     * Additionally, performs the specified action on each element that doesn't match the predicate.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
     * Map<String, Integer> filtered = EntryStream.of(map)
     *                                             .filter(e -> e.getValue() > 1, dropped::add)
     *                                             .toMap();   // returns {"b"=2, "c"=3}
     * // dropped contains entry "a"=1
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry
     * @param onDrop the action to perform on entries that don't match the predicate
     * @return a new EntryStream consisting of entries that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @Override
    public EntryStream<K, V> filter(final Predicate<? super Map.Entry<K, V>> predicate, final Consumer<? super Map.Entry<K, V>> onDrop) {
        return of(_stream.filter(predicate, onDrop));
    }

    /**
     * Returns a stream consisting of the elements of this stream whose keys match the given predicate.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("apple", 1, "banana", 2, "apricot", 3);
     * Map<String, Integer> filtered = EntryStream.of(map)
     *                                             .filterByKey(k -> k.startsWith("a"))
     *                                             .toMap();   // returns {"apple"=1, "apricot"=3}
     * }</pre>
     *
     * <p><b>Note:</b> By design there is no {@code (keyPredicate, onDrop)} overload; to capture dropped entries,
     * use the entry-level {@link #filter(Predicate, Consumer)} with a key-based predicate.
     *
     * @param keyPredicate a non-interfering, stateless predicate to apply to each key
     * @return a new EntryStream consisting of entries whose keys match the given predicate
     * @see #filterByValue(Predicate)
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> filterByKey(final Predicate<? super K> keyPredicate) {
        final Predicate<Map.Entry<K, V>> predicate = Fn.testByKey(keyPredicate);

        return of(_stream.filter(predicate));
    }

    /**
     * Returns a stream consisting of the elements of this stream whose values match the given predicate.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3, "d", 2);
     * Map<String, Integer> filtered = EntryStream.of(map)
     *                                             .filterByValue(v -> v == 2)
     *                                             .toMap();   // returns {"b"=2, "d"=2}
     * }</pre>
     *
     * <p><b>Note:</b> By design there is no {@code (valuePredicate, onDrop)} overload; to capture dropped entries,
     * use the entry-level {@link #filter(Predicate, Consumer)} with a value-based predicate.
     *
     * @param valuePredicate a non-interfering, stateless predicate to apply to each value
     * @return a new EntryStream consisting of entries whose values match the given predicate
     * @see #filterByKey(Predicate)
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> filterByValue(final Predicate<? super V> valuePredicate) {
        final Predicate<Map.Entry<K, V>> predicate = Fn.testByValue(valuePredicate);

        return of(_stream.filter(predicate));
    }

    /**
     * Returns a stream consisting of the longest prefix of elements from this stream
     * that satisfy the given predicate. As soon as an element fails the predicate test,
     * no further elements are processed for inclusion. This is an intermediate operation.
     *
     * <p>The returned stream stops once it encounters an element for which the predicate returns {@code false}.
     * For sorted data, this acts as a logical "take while condition is true" operation.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * In parallel Streams, elements beyond the first non-matching element might still be evaluated
     * and may appear in the result if they individually satisfy the predicate.
     * In parallel streams, there is no guarantee of encounter-order prefix semantics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assuming entries are ordered by key
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "b"); map.put(3, "c"); map.put(4, "d");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .takeWhile(e -> e.getKey() < 3)
     *                                           .toMap();   // returns {1="a", 2="b"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry
     * @return a new EntryStream consisting of elements taken while the predicate returns true
     * @see Stream#takeWhile(Predicate)
     */
    @Override
    public EntryStream<K, V> takeWhile(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(_stream.takeWhile(predicate));
    }

    /**
     * Returns a stream consisting of the longest prefix of elements from this stream
     * that satisfy the given predicate. As soon as an element fails the predicate test,
     * no further elements are processed for inclusion. This is an intermediate operation.
     *
     * <p>The returned stream stops once it encounters an element for which the predicate returns {@code false}.
     * For sorted data, this acts as a logical "take while condition is true" operation.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * In parallel Streams, elements beyond the first non-matching element might still be evaluated
     * and may appear in the result if they individually satisfy the predicate.
     * In parallel streams, there is no guarantee of encounter-order prefix semantics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assuming entries are ordered by key
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "bb"); map.put(3, "ccc"); map.put(4, "dddd");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .takeWhile((k, v) -> v.length() < 3)
     *                                           .toMap();   // returns {1="a", 2="bb"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless bi-predicate to apply to each key-value pair
     * @return a new EntryStream consisting of elements taken while the predicate returns true
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> takeWhile(final BiPredicate<? super K, ? super V> predicate) {
        return of(_stream.takeWhile(Fn.Entries.p(predicate)));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements from the start while the given predicate evaluates to
     * {@code true}. Once an element fails the predicate test, all subsequent elements
     * are included without further filtering. This is an intermediate operation.
     *
     * <p>The resulting stream begins with the first element for which the predicate
     * returns {@code false}, effectively performing a "drop while condition is true"
     * operation that preserves encounter order in sequential streams.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * In sequential streams, behavior is well-defined and deterministic.
     * However, in parallel, elements beyond the first non-matching element
     * may still be evaluated, and those satisfying the predicate may be dropped.
     * In parallel streams, there is no guarantee of encounter-order prefix/suffix semantics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assuming entries are ordered by key
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "b"); map.put(3, "c"); map.put(4, "d");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .dropWhile(e -> e.getKey() < 3)
     *                                           .toMap();   // returns {3="c", 4="d"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry
     * @return a new EntryStream consisting of the remaining elements after dropping
     * @see Stream#dropWhile(Predicate)
     */
    @Override
    public EntryStream<K, V> dropWhile(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(_stream.dropWhile(predicate));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements from the start while the given predicate evaluates to
     * {@code true}. Once an element fails the predicate test, all subsequent elements
     * are included without further filtering. This is an intermediate operation.
     *
     * <p>The resulting stream begins with the first element for which the predicate
     * returns {@code false}, effectively performing a "drop while condition is true"
     * operation that preserves encounter order in sequential streams.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * In sequential streams, behavior is well-defined and deterministic.
     * However, in parallel, elements beyond the first non-matching element
     * may still be evaluated, and those satisfying the predicate may be dropped.
     * In parallel streams, there is no guarantee of encounter-order prefix/suffix semantics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assuming entries are ordered by key
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "bb"); map.put(3, "ccc"); map.put(4, "dddd");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .dropWhile((k, v) -> v.length() < 3)
     *                                           .toMap();   // returns {3="ccc", 4="dddd"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless bi-predicate to apply to each key-value pair
     * @return a new EntryStream consisting of the remaining elements after dropping
     * @see Stream#dropWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> dropWhile(final BiPredicate<? super K, ? super V> predicate) {
        return of(_stream.dropWhile(Fn.Entries.p(predicate)));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements from the start while the given predicate evaluates to
     * {@code true}. Once an element fails the predicate test, all subsequent elements
     * are included without further filtering. Additionally, the specified action is performed
     * on each dropped element. This is an intermediate operation.
     *
     * <p>The resulting stream begins with the first element for which the predicate
     * returns {@code false}, effectively performing a "drop while condition is true"
     * operation that preserves encounter order in sequential streams.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * In sequential streams, behavior is well-defined and deterministic.
     * However, in parallel, elements beyond the first non-matching element
     * may still be evaluated, and those satisfying the predicate may be dropped.
     * In parallel streams, there is no guarantee of encounter-order prefix/suffix semantics.
     *
     * <p>In parallel streams, the action may be performed concurrently for multiple dropped elements.
     * The implementation should ensure the action is thread-safe when used with parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "b"); map.put(3, "c"); map.put(4, "d");
     * List<Map.Entry<Integer, String>> dropped = new ArrayList<>();
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .dropWhile(e -> e.getKey() < 3, dropped::add)
     *                                           .toMap();   // returns {3="c", 4="d"}
     * // dropped contains entries {1="a", 2="b"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry
     * @param onDrop the action to perform on each dropped entry
     * @return a new EntryStream consisting of the remaining elements after dropping
     * @see Stream#dropWhile(Predicate)
     */
    @Override
    public EntryStream<K, V> dropWhile(final Predicate<? super Map.Entry<K, V>> predicate, final Consumer<? super Map.Entry<K, V>> onDrop) {
        return of(_stream.dropWhile(predicate, onDrop));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements until the given predicate evaluates to {@code true}. Once an
     * element satisfies the predicate, that element and all subsequent elements are
     * included without further predicate checks. This is an intermediate operation.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * For sequential or ordered streams, the operation is deterministic — elements
     * are skipped until the first element for which the predicate returns {@code true},
     * and the rest are passed through unchanged.
     *
     * <p>For parallel unordered streams, elements beyond the first matching element
     * may still be evaluated, and elements that do not satisfy the predicate may also
     * be skipped. This can produce results that appear unintuitive when the stream does
     * not define an encounter order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "b"); map.put(3, "c"); map.put(4, "d");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .skipUntil(e -> e.getKey() >= 3)
     *                                           .toMap();   // returns {3="c", 4="d"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate to apply to each entry
     * @return a new EntryStream consisting of the remaining elements after skipping
     */
    @Override
    public EntryStream<K, V> skipUntil(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(_stream.skipUntil(predicate));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements until the given predicate evaluates to {@code true}. Once an
     * element satisfies the predicate, that element and all subsequent elements are
     * included without further predicate checks. This is an intermediate operation.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * For sequential or ordered streams, the operation is deterministic — elements
     * are skipped until the first element for which the predicate returns {@code true},
     * and the rest are passed through unchanged.
     *
     * <p>For parallel unordered streams, elements beyond the first matching element
     * may still be evaluated, and elements that do not satisfy the predicate may also
     * be skipped. This can produce results that appear unintuitive when the stream does
     * not define an encounter order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Integer, String> map = new LinkedHashMap<>();
     * map.put(1, "a"); map.put(2, "bb"); map.put(3, "ccc"); map.put(4, "dddd");
     *
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .skipUntil((k, v) -> v.length() >= 3)
     *                                           .toMap();   // returns {3="ccc", 4="dddd"}
     * }</pre>
     *
     * @param predicate a non-interfering, stateless bi-predicate to apply to each key-value pair
     * @return a new EntryStream consisting of the remaining elements after skipping
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> skipUntil(final BiPredicate<? super K, ? super V> predicate) {
        return of(_stream.skipUntil(Fn.Entries.p(predicate)));
    }

    /**
     * Returns a new EntryStream with entries transformed using the given mapper function.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * Map<String, String> result = EntryStream.of(map)
     *                                           .map(e -> new SimpleImmutableEntry<>(
     *                                               e.getKey().toUpperCase(),
     *                                               String.valueOf(e.getValue())))
     *                                           .toMap();   // returns {"A"="1", "B"="2"}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless function that transforms each entry
     * @return a new EntryStream with transformed entries
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> map(final Function<? super Map.Entry<K, V>, ? extends Map.Entry<? extends KK, ? extends VV>> mapper) {
        return _stream.mapToEntry(mapper);
    }

    /**
     * Returns a new EntryStream with entries transformed using the given key and value mapper functions.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * Map<String, String> result = EntryStream.of(map)
     *                                           .map(
     *                                               e -> e.getKey().toUpperCase(),
     *                                               e -> "Value: " + e.getValue()
     *                                           )
     *                                           .toMap();   // returns {"A"="Value: 1", "B"="Value: 2"}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param keyMapper a non-interfering, stateless function to transform each entry's key
     * @param valueMapper a non-interfering, stateless function to transform each entry's value
     * @return a new EntryStream with transformed entries
     * @see #map(Function)
     * @see #map(BiFunction, BiFunction)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> map(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return _stream.mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns a new EntryStream with entries transformed using the given bi-function mapper.
     * The bi-function receives the key and value of each entry as separate parameters.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * Map<String, String> result = EntryStream.of(map)
     *                                           .map((k, v) -> new SimpleImmutableEntry<>(
     *                                               k.toUpperCase(),
     *                                               k + ":" + v))
     *                                           .toMap();   // returns {"A"="a:1", "B"="b:2"}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless bi-function that transforms each key-value pair into a new entry
     * @return a new EntryStream with transformed entries
     * @see #map(Function)
     * @see #map(BiFunction, BiFunction)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> map(final BiFunction<? super K, ? super V, ? extends Map.Entry<? extends KK, ? extends VV>> mapper) {
        return map(Fn.Entries.f(mapper));
    }

    /**
     * Returns a new EntryStream with entries transformed using the given key and value mapper bi-functions.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("hello", 5, "world", 10);
     * Map<Integer, String> result = EntryStream.of(map)
     *                                           .map(
     *                                               (k, v) -> k.length() + v,
     *                                               (k, v) -> k + " has value " + v
     *                                           )
     *                                           .toMap();   // returns {10="hello has value 5", 15="world has value 10"}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param keyMapper a non-interfering, stateless bi-function to transform each key-value pair into a new key
     * @param valueMapper a non-interfering, stateless bi-function to transform each key-value pair into a new value
     * @return a new EntryStream with transformed entries
     * @see #map(Function)
     * @see #map(BiFunction)
     * @see #map(Function, Function)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> map(final BiFunction<? super K, ? super V, ? extends KK> keyMapper,
            final BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, VV>> mapper = t -> new SimpleImmutableEntry<>(keyMapper.apply(t.getKey(), t.getValue()),
                valueMapper.apply(t.getKey(), t.getValue()));

        return map(mapper);
    }

    /**
     * Returns a new EntryStream by applying a one-to-many transformation to each entry.
     * The mapper function receives each entry and a consumer that accepts multiple output entries.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 2, "b", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapMulti((entry, consumer) -> {
     *                                               for (int i = 0; i < entry.getValue(); i++) {
     *                                                   consumer.accept(new SimpleImmutableEntry<>(
     *                                                       entry.getKey() + i, i
     *                                                   ));
     *                                               }
     *                                           })
     *                                           .toMap();   // returns {"a0"=0, "a1"=1, "b0"=0, "b1"=1, "b2"=2}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless bi-consumer that receives each entry and a consumer for output entries
     * @return a new EntryStream with mapped entries
     * @see Stream#mapMulti(BiConsumer)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> mapMulti(final BiConsumer<? super Map.Entry<K, V>, ? super Consumer<Map.Entry<KK, VV>>> mapper) {
        return _stream.mapMulti(mapper).mapToEntry(Fn.identity());
    }

    /**
     * Returns a new EntryStream by applying a transformation that may produce optional entries.
     * Only entries that are present in the returned Optional are included in the result stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapPartial(e -> e.getValue() > 1
     *                                               ? Optional.of(new SimpleImmutableEntry<>(
     *                                                   e.getKey().toUpperCase(),
     *                                                   e.getValue() * 10))
     *                                               : Optional.empty())
     *                                           .toMap();   // returns {"B"=20, "C"=30}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless function that transforms each entry to an optional entry
     * @return a new EntryStream with transformed entries, excluding empty optionals
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> mapPartial(final Function<? super Map.Entry<K, V>, Optional<Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return _stream.mapPartial(mapper).mapToEntry(Fn.identity());
    }

    /**
     * Returns a new EntryStream by applying a bi-function transformation that may produce optional entries.
     * Only entries that are present in the returned Optional are included in the result stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapPartial((k, v) -> v > 1
     *                                               ? Optional.of(new SimpleImmutableEntry<>(
     *                                                   k.toUpperCase(), v * 10))
     *                                               : Optional.empty())
     *                                           .toMap();   // returns {"B"=20, "C"=30}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless bi-function that transforms each key-value pair to an optional entry
     * @return a new EntryStream with transformed entries, excluding empty optionals
     * @see #mapPartial(Function)
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> mapPartial(final BiFunction<? super K, ? super V, Optional<Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return mapPartial(Fn.Entries.f(mapper));
    }

    /**
     * Returns a new EntryStream with transformed keys while keeping the values unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("hello", 1, "world", 2);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapKey(String::toUpperCase)
     *                                           .toMap();   // returns {"HELLO"=1, "WORLD"=2}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param keyMapper a non-interfering, stateless function to transform each key
     * @return a new EntryStream with transformed keys
     * @see #mapKey(BiFunction)
     * @see #mapValue(Function)
     * @see #map(Function)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> mapKey(final Function<? super K, ? extends KK> keyMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = Fn.mapKey(keyMapper);

        return map(mapper);
    }

    /**
     * Returns a new EntryStream with keys transformed using a bi-function that receives both key and value.
     * The values remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapKey((k, v) -> k + v)
     *                                           .toMap();   // returns {"a1"=1, "b2"=2, "c3"=3}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param keyMapper a non-interfering, stateless bi-function to transform each key using both key and value
     * @return a new EntryStream with transformed keys
     * @see #mapKey(Function)
     * @see #mapValue(BiFunction)
     * @see #map(Function)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> mapKey(final BiFunction<? super K, ? super V, ? extends KK> keyMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = entry -> new SimpleImmutableEntry<>(keyMapper.apply(entry.getKey(), entry.getValue()),
                entry.getValue());

        return map(mapper);
    }

    /**
     * Returns a new EntryStream with transformed values while keeping the keys unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapValue(v -> v * v)
     *                                           .toMap();   // returns {"a"=1, "b"=4, "c"=9}
     * }</pre>
     *
     * @param <VV> the type of values in the resulting entries
     * @param valueMapper a non-interfering, stateless function to transform each value
     * @return a new EntryStream with transformed values
     * @see #mapValue(BiFunction)
     * @see #mapKey(Function)
     * @see #map(Function)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> mapValue(final Function<? super V, ? extends VV> valueMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = Fn.mapValue(valueMapper);

        return map(mapper);
    }

    /**
     * Returns a new EntryStream with values transformed using a bi-function that receives both key and value.
     * The keys remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, String> result = EntryStream.of(map)
     *                                           .mapValue((k, v) -> k + " has value " + v)
     *                                           .toMap();   // returns {"a"="a has value 1", "b"="b has value 2", "c"="c has value 3"}
     * }</pre>
     *
     * @param <VV> the type of values in the resulting entries
     * @param valueMapper a non-interfering, stateless bi-function to transform each value using both key and value
     * @return a new EntryStream with transformed values
     * @see #mapValue(Function)
     * @see #mapKey(BiFunction)
     * @see #map(Function)
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> mapValue(final BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = entry -> new SimpleImmutableEntry<>(entry.getKey(),
                valueMapper.apply(entry.getKey(), entry.getValue()));

        return map(mapper);
    }

    /**
     * Returns a new EntryStream with keys transformed using a function that may produce optional results.
     * Only entries where the key transformation produces a present Optional are included.
     * The values remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("1", 10, "two", 20, "3", 30);
     * Map<Integer, Integer> result = EntryStream.of(map)
     *                                            .mapKeyPartial(k -> {
     *                                                try {
     *                                                    return Optional.of(Integer.parseInt(k));
     *                                                } catch (NumberFormatException e) {
     *                                                    return Optional.empty();
     *                                                }
     *                                            })
     *                                            .toMap();   // returns {1=10, 3=30}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param keyMapper a non-interfering, stateless function to transform each key to an optional key
     * @return a new EntryStream with transformed keys, excluding entries with empty optional keys
     * @see #mapKeyPartial(BiFunction)
     * @see #mapValuePartial(Function)
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> mapKeyPartial(final Function<? super K, Optional<KK>> keyMapper) { //NOSONAR
        @SuppressWarnings("unchecked")
        final Map.Entry<KK, V> noneEntry = NONE_ENTRY_SENTINEL;

        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = entry -> {
            final Optional<KK> op = keyMapper.apply(entry.getKey());
            return op.isPresent() ? new SimpleImmutableEntry<>(op.get(), entry.getValue()) : noneEntry;
        };

        if (this.isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(it -> it != noneEntry));
        } else {
            //noinspection resource
            return map(mapper).filter(it -> it != noneEntry);
        }
    }

    /**
     * Returns a new EntryStream with keys transformed using a bi-function that may produce optional results.
     * Only entries where the key transformation produces a present Optional are included.
     * The values remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .mapKeyPartial((k, v) -> v > 1
     *                                               ? Optional.of(k.toUpperCase())
     *                                               : Optional.empty())
     *                                           .toMap();   // returns {"B"=2, "C"=3}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param keyMapper a non-interfering, stateless bi-function to transform each key-value pair to an optional key
     * @return a new EntryStream with transformed keys, excluding entries with empty optional keys
     * @see #mapKeyPartial(Function)
     * @see #mapValuePartial(BiFunction)
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> mapKeyPartial(final BiFunction<? super K, ? super V, Optional<KK>> keyMapper) { //NOSONAR
        @SuppressWarnings("unchecked")
        final Map.Entry<KK, V> noneEntry = NONE_ENTRY_SENTINEL;

        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = entry -> {
            final Optional<KK> op = keyMapper.apply(entry.getKey(), entry.getValue());
            return op.isPresent() ? new SimpleImmutableEntry<>(op.get(), entry.getValue()) : noneEntry;
        };

        if (this.isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(it -> it != noneEntry));
        } else {
            //noinspection resource
            return map(mapper).filter(it -> it != noneEntry);
        }
    }

    /**
     * Returns a new EntryStream with values transformed using a function that may produce optional results.
     * Only entries where the value transformation produces a present Optional are included.
     * The keys remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = Map.of("a", "1", "b", "two", "c", "3");
     * Map<String, Integer> result = EntryStream.of(map)
     *                                            .mapValuePartial(v -> {
     *                                                try {
     *                                                    return Optional.of(Integer.parseInt(v));
     *                                                } catch (NumberFormatException e) {
     *                                                    return Optional.empty();
     *                                                }
     *                                            })
     *                                            .toMap();   // returns {"a"=1, "c"=3}
     * }</pre>
     *
     * @param <VV> the type of values in the resulting entries
     * @param valueMapper a non-interfering, stateless function to transform each value to an optional value
     * @return a new EntryStream with transformed values, excluding entries with empty optional values
     * @see #mapValuePartial(BiFunction)
     * @see #mapKeyPartial(Function)
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> mapValuePartial(final Function<? super V, Optional<VV>> valueMapper) { //NOSONAR
        @SuppressWarnings("unchecked")
        final Map.Entry<K, VV> noneEntry = NONE_ENTRY_SENTINEL;

        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = entry -> {
            final Optional<VV> op = valueMapper.apply(entry.getValue());
            return op.isPresent() ? new SimpleImmutableEntry<>(entry.getKey(), op.get()) : noneEntry;
        };

        if (this.isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(it -> it != noneEntry));
        } else {
            //noinspection resource
            return map(mapper).filter(it -> it != noneEntry);
        }
    }

    /**
     * Returns a new EntryStream with values transformed using a bi-function that may produce optional results.
     * Only entries where the value transformation produces a present Optional are included.
     * The keys remain unchanged.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, String> result = EntryStream.of(map)
     *                                           .mapValuePartial((k, v) -> k.equals("b")
     *                                               ? Optional.empty()
     *                                               : Optional.of(k + ":" + v))
     *                                           .toMap();   // returns {"a"="a:1", "c"="c:3"}
     * }</pre>
     *
     * @param <VV> the type of values in the resulting entries
     * @param valueMapper a non-interfering, stateless bi-function to transform each key-value pair to an optional value
     * @return a new EntryStream with transformed values, excluding entries with empty optional values
     * @see #mapValuePartial(Function)
     * @see #mapKeyPartial(BiFunction)
     * @see Stream#mapPartial(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> mapValuePartial(final BiFunction<? super K, ? super V, Optional<VV>> valueMapper) { //NOSONAR
        @SuppressWarnings("unchecked")
        final Map.Entry<K, VV> noneEntry = NONE_ENTRY_SENTINEL;

        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = entry -> {
            final Optional<VV> op = valueMapper.apply(entry.getKey(), entry.getValue());
            return op.isPresent() ? new SimpleImmutableEntry<>(entry.getKey(), op.get()) : noneEntry;
        };

        if (this.isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(it -> it != noneEntry));
        } else {
            //noinspection resource
            return map(mapper).filter(it -> it != noneEntry);
        }
    }

    /**
     * Returns a new EntryStream by applying a one-to-many transformation where each entry is mapped to an EntryStream.
     * The resulting streams are concatenated into a single stream.
     * This is an intermediate operation.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(Function) flatMap} (this method) - transforms entries into {@link com.landawn.abacus.util.stream.EntryStream EntryStream}.</li>
     *   <li>{@link #flatmap(Function) flatmap} (lowercase 'm') - transforms entries into {@link java.util.Map Map}.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 2, "b", 1);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .flatMap(e -> {
     *                                               Map<String, Integer> expanded = new HashMap<>();
     *                                               for (int i = 0; i < e.getValue(); i++) {
     *                                                   expanded.put(e.getKey() + i, i);
     *                                               }
     *                                               return EntryStream.of(expanded);
     *                                           })
     *                                           .toMap();   // returns {"a0"=0, "a1"=1, "b0"=0}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless function that transforms each entry to an EntryStream
     * @return a new EntryStream with flat-mapped entries
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> flatMap(final Function<? super Map.Entry<K, V>, ? extends EntryStream<? extends KK, ? extends VV>> mapper) { //NOSONAR
        return _stream.flattMapToEntry(mapper);
    }

    /**
     * Returns a new EntryStream by applying a one-to-many transformation where each key-value pair is mapped to an EntryStream.
     * The resulting streams are concatenated into a single stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 2, "b", 1);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .flatMap((k, v) -> {
     *                                               return EntryStream.of(k + "1", v, k + "2", v * 2);
     *                                           })
     *                                           .toMap();   // returns {"a1"=2, "a2"=4, "b1"=1, "b2"=2}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless bi-function that transforms each key-value pair to an EntryStream
     * @return a new EntryStream with flat-mapped entries
     * @see #flatMap(Function)
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> flatMap(final BiFunction<? super K, ? super V, ? extends EntryStream<? extends KK, ? extends VV>> mapper) { //NOSONAR
        return flatMap(Fn.Entries.f(mapper));
    }

    /**
     * Returns a new EntryStream by applying a one-to-many transformation where each entry is mapped to a Map.
     * The resulting maps are merged into a single stream of entries.
     * This is an intermediate operation.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(Function) flatMap} (uppercase 'M') - transforms entries into {@link com.landawn.abacus.util.stream.EntryStream EntryStream}.</li>
     *   <li>{@link #flatmap(Function) flatmap} (this method) - transforms entries into {@link java.util.Map Map}.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("group1", 2, "group2", 1);
     * Map<String, Integer> result = EntryStream.of(map)
     *                                           .flatmap(e -> {
     *                                               Map<String, Integer> expanded = new HashMap<>();
     *                                               for (int i = 1; i <= e.getValue(); i++) {
     *                                                   expanded.put(e.getKey() + "_item" + i, i);
     *                                               }
     *                                               return expanded;
     *                                           })
     *                                           .toMap();   // returns {"group1_item1"=1, "group1_item2"=2, "group2_item1"=1}
     * }</pre>
     *
     * @param <KK> the type of keys in the resulting entries
     * @param <VV> the type of values in the resulting entries
     * @param mapper a non-interfering, stateless function that transforms each entry to a Map
     * @return a new EntryStream with flat-mapped entries
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    // @ai-ignore flatmap/flatMap/flattMap naming - intentional: flatMap maps to EntryStream via Stream, flatmap maps to Map, flattMap maps to Abacus Stream of entries. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> flatmap(final Function<? super Map.Entry<K, V>, ? extends Map<? extends KK, ? extends VV>> mapper) { //NOSONAR
        return _stream.flatmapToEntry(mapper);
    }

    /**
     * Applies a mapping operation on the stream with multiple output elements for each input element.
     * It takes a BiFunction as an argument which is applied to each element in the stream.
     * The result is a new EntryStream consisting of all output elements produced by the BiFunction for each input element.
     *
     * <p>This is an intermediate operation that allows one-to-many transformations where each key-value pair
     * can be transformed into a Map of zero or more new entries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("group1", 2, "group2", 1)
     *     .flatmap((k, v) -> {
     *         Map<String, Integer> expanded = new HashMap<>();
     *         for (int i = 1; i <= v; i++) {
     *             expanded.put(k + "_item" + i, i);
     *         }
     *         return expanded;
     *     })
     *     .toMap();
     * // Result: {group1_item1=1, group1_item2=2, group2_item1=1}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param <VV> the type of the values in the resulting entries
     * @param mapper a non-interfering, stateless BiFunction that transforms each key-value pair into a Map
     * @return a new EntryStream with transformed entries
     * @see #flatmap(Function)
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> flatmap(final BiFunction<? super K, ? super V, ? extends Map<? extends KK, ? extends VV>> mapper) { //NOSONAR
        return flatmap(Fn.Entries.f(mapper));
    }

    /**
     * Applies a mapping operation on the stream with multiple output elements for each input element.
     * It takes a Function as an argument which is applied to each entry in the stream.
     * The result is a new {@link EntryStream} consisting of all output entries produced by the Function for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many transformations where each entry
     * can be transformed into an Abacus {@link Stream} of zero or more new entries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", "1,2", "b", "3,4")
     *     .flattMap(e -> Stream.of(e.getValue().split(","))
     *         .map(v -> new SimpleImmutableEntry<>(e.getKey() + v, Integer.parseInt(v))))
     *     .toMap();
     * // Result: {a1=1, a2=2, b3=3, b4=4}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param <VV> the type of the values in the resulting entries
     * @param mapper a non-interfering, stateless Function that transforms each entry
     * @return a new {@code EntryStream} with the flattened contents of the mapped Abacus streams
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    // @ai-ignore flatmap/flatMap/flattMap naming - intentional: flatMap maps to EntryStream via Stream, flatmap maps to Map, flattMap maps to Abacus Stream of entries. Do not suggest renaming.
    @Beta
    @ParallelSupported
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> flattMap(
            final Function<? super Map.Entry<K, V>, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return _stream.flatMapToEntry(mapper);
    }

    /**
     * Applies a mapping operation on the stream with multiple output elements for each input element.
     * It takes a BiFunction as an argument which is applied to each key-value pair in the stream.
     * The result is a new {@link EntryStream} consisting of all output entries produced by the BiFunction for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many transformations where each key-value pair
     * can be transformed into an Abacus {@link Stream} of zero or more new entries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("prefix", "a,b", "suffix", "c,d")
     *     .flattMap((k, v) -> Stream.of(v.split(","))
     *         .map(s -> new SimpleImmutableEntry<>(k + "-" + s, s.length())))
     *     .toMap();
     * // Result: {prefix-a=1, prefix-b=1, suffix-c=1, suffix-d=1}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param <VV> the type of the values in the resulting entries
     * @param mapper a non-interfering, stateless BiFunction that transforms each key-value pair into a Stream of new entries
     * @return a new {@code EntryStream} with the flattened contents of the mapped Abacus streams
     * @see #flattMap(Function)
     * @see Stream#flatMap(Function)
     * @see Stream#flatmap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    @Beta
    public <KK, VV> EntryStream<KK, VV> flattMap(
            final BiFunction<? super K, ? super V, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return flattMap(Fn.Entries.f(mapper));
    }

    /**
     * Applies a mapping operation on the keys in this EntryStream with multiple output elements for each input key.
     * It takes a Function as an argument which is applied to each key in the stream.
     * The result is a new EntryStream consisting of all output keys produced by the Function for each input key.
     *
     * <p>This is an intermediate operation that allows one-to-many key transformations while preserving the original values.
     * Each key can be transformed into zero or more new keys, all paired with the same original value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("word", 1, "hello", 2)
     *     .flatMapKey(k -> Stream.of(k.toUpperCase(), k.toLowerCase()))
     *     .toMap();
     * // Result: {WORD=1, word=1, HELLO=2, hello=2}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param keyMapper a non-interfering, stateless function to apply to each key to transform it into a Stream of new keys
     * @return a new EntryStream with transformed keys
     * @see #flatMapKey(BiFunction)
     * @see #flatMapValue(Function)
     * @see #flatmapKey(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> flatMapKey(final Function<? super K, ? extends Stream<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> secondMapper = e -> {
            final Stream<? extends KK> keys = keyMapper.apply(e.getKey());
            return keys == null ? Stream.empty() : keys.map(kk -> new SimpleImmutableEntry<>(kk, e.getValue()));
        };

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the keys in this EntryStream with multiple output elements for each input key.
     * It takes a BiFunction as an argument which is applied to each key-value pair in the stream.
     * The result is a new EntryStream consisting of all output keys produced by the BiFunction for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many key transformations based on both key and value.
     * Each entry can be transformed into zero or more new keys, all paired with the same original value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2)
     *     .flatMapKey((k, v) -> Stream.of(k + v, k + "-" + v))
     *     .toMap();
     * // Result: {a1=1, a-1=1, b2=2, b-2=2}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param keyMapper a non-interfering, stateless bi-function to apply to each key-value pair to transform it into a Stream of new keys
     * @return a new EntryStream with transformed keys
     * @see #flatMapKey(Function)
     * @see Stream#flatMap(Function)
     * @see #flatMapValue(BiFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> flatMapKey(final BiFunction<? super K, ? super V, ? extends Stream<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> secondMapper = e -> {
            final Stream<? extends KK> keys = keyMapper.apply(e.getKey(), e.getValue());
            return keys == null ? Stream.empty() : keys.map(kk -> new SimpleImmutableEntry<>(kk, e.getValue()));
        };

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the keys in this EntryStream with multiple output elements for each input key.
     * It takes a Function as an argument which is applied to each key in the stream.
     * The result is a new EntryStream consisting of all output keys produced by the Function for each input key.
     *
     * <p>This is an intermediate operation that allows one-to-many key transformations while preserving the original values.
     * Each key can be transformed into a collection of new keys, all paired with the same original value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("word", 1, "hello", 2)
     *     .flatmapKey(k -> Arrays.asList(k.toUpperCase(), k.toLowerCase()))
     *     .toMap();
     * // Result: {WORD=1, word=1, HELLO=2, hello=2}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param keyMapper a non-interfering, stateless function to apply to each key to transform it into a Collection of new keys
     * @return a new EntryStream with transformed keys
     * @see Stream#flatMap(Function)
     * @see #flatMapKey(Function)
     * @see #flatmapValue(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> flatmapKey(final Function<? super K, ? extends Collection<? extends KK>> keyMapper) { //NOSONAR
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> secondMapper = e -> Stream.of(keyMapper.apply(e.getKey()))
                .map(kk -> new SimpleImmutableEntry<>(kk, e.getValue()));

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the keys in this EntryStream with multiple output elements for each input key.
     * It takes a BiFunction as an argument which is applied to each key-value pair in the stream.
     * The result is a new EntryStream consisting of all output keys produced by the BiFunction for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many key transformations based on both key and value.
     * Each entry can be transformed into a collection of new keys, all paired with the same original value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2)
     *     .flatmapKey((k, v) -> Arrays.asList(k + v, k + "-" + v))
     *     .toMap();
     * // Result: {a1=1, a-1=1, b2=2, b-2=2}
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting entries
     * @param keyMapper a non-interfering, stateless bi-function to apply to each key-value pair to transform it into a Collection of new keys
     * @return a new EntryStream with transformed keys
     * @see #flatMapKey(Function)
     * @see #flatMapKey(BiFunction)
     * @see #flatmapValue(BiFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public <KK> EntryStream<KK, V> flatmapKey(final BiFunction<? super K, ? super V, ? extends Collection<? extends KK>> keyMapper) { //NOSONAR
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> secondMapper = e -> Stream.of(keyMapper.apply(e.getKey(), e.getValue()))
                .map(kk -> new SimpleImmutableEntry<>(kk, e.getValue()));

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the values in this EntryStream with multiple output elements for each input value.
     * It takes a Function as an argument which is applied to each value in the stream.
     * The result is a new EntryStream consisting of all output values produced by the Function for each input value.
     *
     * <p>This is an intermediate operation that allows one-to-many value transformations while preserving the original keys.
     * Each value can be transformed into zero or more new values, all paired with the same original key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", "1,2", "b", "3,4")
     *     .flatMapValue(v -> Stream.of(v.split(",")))
     *     .toMultimap();
     * // Result: {a=[1, 2], b=[3, 4]}
     * }</pre>
     *
     * @param <VV> the type of the values in the resulting entries
     * @param valueMapper a non-interfering, stateless function to apply to each value to transform it into a Stream of new values
     * @return a new EntryStream with transformed values
     * @see Stream#flatMap(Function)
     * @see #flatMapKey(Function)
     * @see #flatmapValue(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> flatMapValue(final Function<? super V, ? extends Stream<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> secondMapper = e -> {
            final Stream<? extends VV> values = valueMapper.apply(e.getValue());
            return values == null ? Stream.empty() : values.map(vv -> new SimpleImmutableEntry<>(e.getKey(), vv));
        };

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the values in this EntryStream with multiple output elements for each input value.
     * It takes a BiFunction as an argument which is applied to each key-value pair in the stream.
     * The result is a new EntryStream consisting of all output values produced by the BiFunction for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many value transformations based on both key and value.
     * Each entry can be transformed into zero or more new values, all paired with the same original key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("prefix", "a,b", "suffix", "c,d")
     *     .flatMapValue((k, v) -> Stream.of(v.split(",")).map(s -> k + "-" + s))
     *     .toMultimap();
     * // Result: {prefix=[prefix-a, prefix-b], suffix=[suffix-c, suffix-d]}
     * }</pre>
     *
     * @param <VV> the type of the values in the resulting entries
     * @param valueMapper a non-interfering, stateless bi-function to apply to each key-value pair to transform it into a Stream of new values
     * @return a new EntryStream with transformed values
     * @see #flatMapValue(Function)
     * @see Stream#flatMap(Function)
     * @see #flatMapKey(BiFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> flatMapValue(final BiFunction<? super K, ? super V, ? extends Stream<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> secondMapper = e -> {
            final Stream<? extends VV> values = valueMapper.apply(e.getKey(), e.getValue());
            return values == null ? Stream.empty() : values.map(vv -> new SimpleImmutableEntry<>(e.getKey(), vv));
        };

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the values in this EntryStream with multiple output elements for each input value.
     * It takes a Function as an argument which is applied to each value in the stream.
     * The result is a new EntryStream consisting of all output values produced by the Function for each input value.
     *
     * <p>This is an intermediate operation that allows one-to-many value transformations while preserving the original keys.
     * Each value can be transformed into a collection of new values, all paired with the same original key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", "1,2", "b", "3,4")
     *     .flatmapValue(v -> Arrays.asList(v.split(",")))
     *     .toMultimap();
     * // Result: {a=[1, 2], b=[3, 4]}
     * }</pre>
     *
     * @param <VV> the type of the values in the resulting entries
     * @param valueMapper a non-interfering, stateless function to apply to each value to transform it into a Collection of new values
     * @return a new EntryStream with transformed values
     * @see Stream#flatMap(Function)
     * @see #flatMapValue(Function)
     * @see #flatmapKey(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> flatmapValue(final Function<? super V, ? extends Collection<? extends VV>> valueMapper) { //NOSONAR
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> secondMapper = e -> Stream.of(valueMapper.apply(e.getValue()))
                .map(vv -> new SimpleImmutableEntry<>(e.getKey(), vv));

        return flattMap(secondMapper);
    }

    /**
     * Applies a mapping operation on the values in this EntryStream with multiple output elements for each input value.
     * It takes a BiFunction as an argument which is applied to each key-value pair in the stream.
     * The result is a new EntryStream consisting of all output values produced by the BiFunction for each input entry.
     *
     * <p>This is an intermediate operation that allows one-to-many value transformations based on both key and value.
     * Each entry can be transformed into a collection of new values, all paired with the same original key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("prefix", "a,b", "suffix", "c,d")
     *     .flatmapValue((k, v) -> Arrays.stream(v.split(",")).map(s -> k + "-" + s).collect(Collectors.toList()))
     *     .toMultimap();
     * // Result: {prefix=[prefix-a, prefix-b], suffix=[suffix-c, suffix-d]}
     * }</pre>
     *
     * @param <VV> the type of the values in the resulting entries
     * @param valueMapper a non-interfering, stateless bi-function to apply to each key-value pair to transform it into a Collection of new values
     * @return a new EntryStream with transformed values
     * @see #flatMapValue(Function)
     * @see #flatMapValue(BiFunction)
     * @see #flatmapKey(BiFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public <VV> EntryStream<K, VV> flatmapValue(final BiFunction<? super K, ? super V, ? extends Collection<? extends VV>> valueMapper) { //NOSONAR
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> secondMapper = e -> Stream.of(valueMapper.apply(e.getKey(), e.getValue()))
                .map(vv -> new SimpleImmutableEntry<>(e.getKey(), vv));

        return flattMap(secondMapper);
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with a list of values that had that key.
     *
     * <p>This is an intermediate operation that triggers a terminal operation internally to collect
     * all elements before returning a new EntryStream.
     * This operation requires all elements to be processed sequentially.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy()
     *     .toMap();
     * // Result: {a=[1, 3], b=[2, 4]}
     * }</pre>
     *
     * <p><b>Note:</b> By design, the no-argument {@code groupBy()} overloads (regrouping by the stream's
     * existing key) are {@code @SequentialOnly}, while the key-mapper and {@code Collector} overloads are
     * {@code @ParallelSupported}.
     *
     * @return a new EntryStream with keys and their associated list of values
     * @see Stream#groupBy(Function, Function)
     * @see #groupBy(BinaryOperator)
     * @see #groupBy(Collector)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, List<V>> groupBy() {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(_stream.sequential()
                    .groupBy(keyMapper, valueMapper)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.groupBy(keyMapper, valueMapper));
        }
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with a list of values that had that key.
     *
     * <p>This is an intermediate operation that triggers a terminal operation internally to collect
     * all elements before returning a new EntryStream.
     * This operation requires all elements to be processed sequentially.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy(HashMap::new)
     *     .toMap();
     * // Result: {a=[1, 3], b=[2, 4]} (backed by HashMap)
     * }</pre>
     *
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream with keys and their associated list of values
     * @see Stream#groupBy(Function, Function, Supplier)
     * @see #groupBy()
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, List<V>> groupBy(final Supplier<? extends Map<K, List<V>>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(_stream.sequential()
                    .groupBy(keyMapper, valueMapper, mapFactory)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.groupBy(keyMapper, valueMapper, mapFactory));
        }
    }

    /**
     * Groups the elements of the stream by applying a key mapping function and a value mapping function to each element.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is a list of elements that are mapped to the corresponding key by the value mapping function.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7)
     *     .groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue())
     *     .toMap();
     * // Result: {a=[5, 7], b=[6]}
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <VV> the type of the value in the resulting Map.Entry.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param valueMapper the function to be applied to each element in the stream to determine its value in the group
     * @return a new EntryStream consisting of entries where the key is the group identifier, and the value is a list of elements that mapped to the corresponding key.
     * @see Stream#groupBy(Function, Function)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, VV> EntryStream<KK, List<VV>> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {

        return of(_stream.groupBy(keyMapper, valueMapper));
    }

    /**
     * Groups the elements of the stream by applying a key mapping function and a value mapping function to each element.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is a list of elements that are mapped to the corresponding key by the value mapping function.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7)
     *     .groupBy(
     *         e -> e.getKey().substring(0, 1),
     *         e -> e.getValue(),
     *         TreeMap::new
     *     )
     *     .toMap();
     * // Result: {a=[5, 7], b=[6]} (backed by TreeMap)
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <VV> the type of the value in the resulting Map.Entry.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param valueMapper the function to be applied to each element in the stream to determine its value in the group
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream consisting of entries where the key is the group identifier, and the value is a list of elements that mapped to the corresponding key.
     * @see Stream#groupBy(Function, Function, Supplier)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, VV> EntryStream<KK, List<VV>> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<? extends Map<KK, List<VV>>> mapFactory) {

        return of(_stream.groupBy(keyMapper, valueMapper, mapFactory));
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with the result of the downstream collector applied to all entries with that key.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy(Collectors.summingInt(e -> e.getValue()))
     *     .toMap();
     * // Result: {a=4, b=6}
     * }</pre>
     *
     * @param <D> the type of the result of the downstream collector
     * @param downstream the collector to use for grouping the entries
     * @return a new EntryStream with keys and their associated collected results
     * @see Stream#groupBy(Function, Collector)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <D> EntryStream<K, D> groupBy(final Collector<? super Map.Entry<K, V>, ?, D> downstream) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();

        return of(_stream.groupBy(keyMapper, downstream));
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with the result of the downstream collector applied to all entries with that key.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy(
     *         Collectors.summingInt(e -> e.getValue()),
     *         TreeMap::new
     *     )
     *     .toMap();
     * // Result: {a=4, b=6} (backed by TreeMap)
     * }</pre>
     *
     * @param <D> the type of the result of the downstream collector
     * @param downstream the collector to use for grouping the entries
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream with keys and their associated collected results
     * @see Stream#groupBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <D> EntryStream<K, D> groupBy(final Collector<? super Map.Entry<K, V>, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();

        return of(_stream.groupBy(keyMapper, downstream, mapFactory));
    }

    /**
     * Groups the elements of the stream by applying a key mapping function to each element.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is the result of the downstream collector.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7)
     *     .groupBy(
     *         e -> e.getKey().substring(0, 1),
     *         Collectors.summingInt(e -> e.getValue())
     *     )
     *     .toMap();
     * // Result: {a=12, b=6}
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <D> the type of the result of the downstream collector.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param downstream the collector to use for grouping the entries
     * @return a new EntryStream consisting of entries where the key is the group identifier and the value is the result of the downstream collector.
     * @see Stream#groupBy(Function, Collector)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, D> EntryStream<KK, D> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, ?, D> downstream) {

        return of(_stream.groupBy(keyMapper, downstream));
    }

    /**
     * Groups the elements of the stream by applying a key mapping function to each element.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is the result of the downstream collector.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7)
     *     .groupBy(
     *         e -> e.getKey().substring(0, 1),
     *         Collectors.summingInt(e -> e.getValue()),
     *         TreeMap::new
     *     )
     *     .toMap();
     * // Result: {a=12, b=6} (backed by TreeMap)
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <D> the type of the result of the downstream collector.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param downstream the collector to use for grouping the entries
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream consisting of entries where the key is the group identifier and the value is the result of the downstream collector.
     * @see Stream#groupBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, D> EntryStream<KK, D> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, ?, D> downstream, final Supplier<? extends Map<KK, D>> mapFactory) {

        return of(_stream.groupBy(keyMapper, downstream, mapFactory));
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with a single value,
     * which is the result of applying the merge function to all values associated with that key.
     *
     * <p>This is an intermediate operation that triggers a terminal operation internally to collect
     * all elements before returning a new EntryStream.
     * This operation requires all elements to be processed sequentially.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy(Integer::sum)
     *     .toMap();
     * // Result: {a=4, b=6}
     * }</pre>
     *
     * @param mergeFunction the function to merge values associated with the same key
     * @return a new EntryStream with keys and their associated merged values
     * @see Stream#groupBy(Function, Function, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> groupBy(final BinaryOperator<V> mergeFunction) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(_stream.sequential()
                    .groupBy(keyMapper, valueMapper, mergeFunction)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.groupBy(keyMapper, valueMapper, mergeFunction));
        }
    }

    /**
     * Groups the entries in this EntryStream by their keys.
     * The result is a new EntryStream where each key is associated with a single value,
     * which is the result of applying the merge function to all values associated with that key.
     *
     * <p>This is an intermediate operation that triggers a terminal operation internally to collect
     * all elements before returning a new EntryStream.
     * This operation requires all elements to be processed sequentially.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupBy(Integer::sum, TreeMap::new)
     *     .toMap();
     * // Result: {a=4, b=6} (backed by TreeMap)
     * }</pre>
     *
     * @param mergeFunction the function to merge values associated with the same key
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream with keys and their associated merged values
     * @see Stream#groupBy(Function, Function, BinaryOperator, Supplier)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> groupBy(final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(_stream.sequential()
                    .groupBy(keyMapper, valueMapper, mergeFunction, mapFactory)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.groupBy(keyMapper, valueMapper, mergeFunction, mapFactory));
        }
    }

    /**
     * Groups the elements of the stream by applying a key mapping function and a value mapping function to each element,
     * and then merges the values associated with the same key using the provided merge function.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is the result of merging the values that mapped to the corresponding key.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7, "apple", 3)
     *     .groupBy(
     *         e -> e.getKey().substring(0, 1),
     *         e -> e.getValue(),
     *         Integer::sum
     *     )
     *     .toMap();
     * // Result: {a=15, b=6}
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <VV> the type of the value in the resulting Map.Entry.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param valueMapper the function to be applied to each element in the stream to determine its value in the group
     * @param mergeFunction the function to merge values associated with the same key
     * @return a new EntryStream consisting of entries where the key is the group identifier and the value is the result of merging the values that mapped to the corresponding key.
     * @see Stream#groupBy(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, VV> EntryStream<KK, VV> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction) {
        return of(_stream.groupBy(keyMapper, valueMapper, mergeFunction));
    }

    /**
     * Groups the elements of the stream by applying a key mapping function and a value mapping function to each element,
     * and then merges the values associated with the same key using the provided merge function.
     * The result is an EntryStream where each entry's key is the group identifier (determined by the key mapping function),
     * and the value is the result of merging the values that mapped to the corresponding key.
     *
     * <p>This is an intermediate operation that can be executed in parallel. It triggers a terminal operation internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("apple", 5, "banana", 6, "apricot", 7, "apple", 3)
     *     .groupBy(
     *         e -> e.getKey().substring(0, 1),
     *         e -> e.getValue(),
     *         Integer::sum,
     *         TreeMap::new
     *     )
     *     .toMap();
     * // Result: {a=15, b=6} (backed by TreeMap)
     * }</pre>
     *
     * @param <KK> the type of the key in the resulting Map.Entry.
     * @param <VV> the type of the value in the resulting Map.Entry.
     * @param keyMapper the function to be applied to each element in the stream to determine the group it belongs to
     * @param valueMapper the function to be applied to each element in the stream to determine its value in the group
     * @param mergeFunction the function to merge values associated with the same key
     * @param mapFactory the supplier providing a new empty Map into which the results will be inserted
     * @return a new EntryStream consisting of entries where the key is the group identifier and the value is the result of merging the values that mapped to the corresponding key.
     * @see Stream#groupBy(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public <KK, VV> EntryStream<KK, VV> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction,
            final Supplier<? extends Map<KK, VV>> mapFactory) {

        return of(_stream.groupBy(keyMapper, valueMapper, mergeFunction, mapFactory));
    }

    /**
     * Collapses the entries in this EntryStream based on the given key predicate.
     * The entries are grouped into lists of values where the corresponding keys are collapsible according to the predicate.
     *
     * <p>This is an intermediate operation that processes elements sequentially.
     * Adjacent elements are collapsed together when their keys satisfy the given predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a1", 1, "a2", 2, "b1", 3, "b2", 4, "c1", 5)
     *     .collapseByKey((k1, k2) -> k1.charAt(0) == k2.charAt(0))
     *     .toList();
     * // Result: [[1, 2], [3, 4], [5]]
     * }</pre>
     *
     * <p><b>Note:</b> By design there is no dedicated {@code BinaryOperator} / {@code (init, BiFunction)} merge
     * overload; perform merge-style collapsing via the {@code (collapsible, mapper, Collector)} overload with
     * {@code Collectors.reducing(...)}.
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group;
     *        the first parameter is the key of the last (not the first) element of the current group, and the second parameter is the key of the next element to check
     * @return a new Stream with lists of values whose keys are collapsible
     * @see #collapseByKey(BiPredicate, Function, Collector)
     * @see Stream#collapse(BiPredicate, Collector)
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<List<V>> collapseByKey(final BiPredicate<? super K, ? super K> collapsible) {
        return collapseByKey(collapsible, Fn.value(), Collectors.toList());
    }

    /**
     * Collapses the entries in this EntryStream based on the given key predicate.
     * The entries are mapped using the provided mapper function and collected using the specified collector.
     *
     * <p>This is an intermediate operation that processes elements sequentially.
     * Adjacent elements are collapsed together when their keys satisfy the given predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a1", 1, "a2", 2, "b1", 3, "b2", 4, "c1", 5)
     *     .collapseByKey(
     *         (k1, k2) -> k1.charAt(0) == k2.charAt(0),
     *         Map.Entry::getValue,
     *         Collectors.summingInt(Integer::intValue)
     *     )
     *     .toList();
     * // Result: [3, 7, 5]
     * }</pre>
     *
     * @param <U> the type of the mapped elements
     * @param <R> the type of the result
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group;
     *        the first parameter is the key of the last (not the first) element of the current group, and the second parameter is the key of the next element to check
     * @param mapper a non-interfering, stateless function that maps each entry to a value
     * @param collector the collector to collect the mapped values
     * @return a new Stream with the collapsed and collected results
     * @see Stream#collapse(BiPredicate, Collector)
     */
    @SequentialOnly
    @IntermediateOp
    public <U, R> Stream<R> collapseByKey(final BiPredicate<? super K, ? super K> collapsible, final Function<? super Map.Entry<K, V>, U> mapper,
            final Collector<? super U, ?, R> collector) {
        final BiPredicate<? super Map.Entry<K, V>, ? super Map.Entry<K, V>> collapsible2 = (t, u) -> collapsible.test(t.getKey(), u.getKey());

        return _stream.collapse(collapsible2, Collectors.mapping(mapper, collector));
    }

    /**
     * Collapses the entries in this EntryStream based on the given value predicate.
     * The entries are grouped into lists of keys where the corresponding values are collapsible according to the predicate.
     *
     * <p>This is an intermediate operation that processes elements sequentially.
     * Adjacent elements are collapsed together when their values satisfy the given predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2, "e", 3)
     *     .collapseByValue((v1, v2) -> v1.equals(v2))
     *     .toList();
     * // Result: [[a, b], [c, d], [e]]
     * }</pre>
     *
     * <p><b>Note:</b> By design there is no dedicated {@code BinaryOperator} / {@code (init, BiFunction)} merge
     * overload; perform merge-style collapsing via the {@code (collapsible, mapper, Collector)} overload with
     * {@code Collectors.reducing(...)}.
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group;
     *        the first parameter is the value of the last (not the first) element of the current group, and the second parameter is the value of the next element to check
     * @return a new Stream with lists of keys whose values are collapsible
     * @see #collapseByValue(BiPredicate, Function, Collector)
     * @see Stream#collapse(BiPredicate, Collector)
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<List<K>> collapseByValue(final BiPredicate<? super V, ? super V> collapsible) {
        return collapseByValue(collapsible, Fn.key(), Collectors.toList());
    }

    /**
     * Collapses the entries in this EntryStream based on the given value predicate.
     * The entries are mapped using the provided mapper function and collected using the specified collector.
     *
     * <p>This is an intermediate operation that processes elements sequentially.
     * Adjacent elements are collapsed together when their values satisfy the given predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2, "e", 3)
     *     .collapseByValue(
     *         (v1, v2) -> v1.equals(v2),
     *         Map.Entry::getKey,
     *         Collectors.joining("-")
     *     )
     *     .toList();
     * // Result: ["a-b", "c-d", "e"]
     * }</pre>
     *
     * @param <U> the type of the mapped elements
     * @param <R> the type of the result
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group;
     *        the first parameter is the value of the last (not the first) element of the current group, and the second parameter is the value of the next element to check
     * @param mapper a non-interfering, stateless function that maps each entry to a value
     * @param collector the collector to collect the mapped values
     * @return a new Stream with the collapsed and collected results
     * @see Stream#collapse(BiPredicate, Collector)
     */
    @SequentialOnly
    @IntermediateOp
    public <U, R> Stream<R> collapseByValue(final BiPredicate<? super V, ? super V> collapsible, final Function<? super Map.Entry<K, V>, U> mapper,
            final Collector<? super U, ?, R> collector) {
        final BiPredicate<? super Map.Entry<K, V>, ? super Map.Entry<K, V>> collapsible2 = (t, u) -> collapsible.test(t.getValue(), u.getValue());

        return _stream.collapse(collapsible2, Collectors.mapping(mapper, collector));
    }

    /**
     * Splits the stream into chunks of the specified size.
     * Each chunk is collected into a List of Map.Entry objects.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * The last chunk may contain fewer elements if the stream size is not evenly divisible by the chunk size.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)
     *     .split(2)
     *     .toList();
     * // Result: [[a=1, b=2], [c=3, d=4], [e=5]]
     * }</pre>
     *
     * @param chunkSize the size of each chunk
     * @return a new Stream consisting of Lists of Map.Entry objects, each representing a chunk of the original Stream.
     * @see Stream#split(int)
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<List<Map.Entry<K, V>>> split(final int chunkSize) {
        return _stream.split(chunkSize);
    }

    /**
     * Splits the stream into chunks of the specified size.
     * Each chunk is collected into a collection of Map.Entry objects specified by the collectionSupplier.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * The last chunk may contain fewer elements if the stream size is not evenly divisible by the chunk size.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)
     *     .split(2, LinkedList::new)
     *     .toList();
     * // Result: [[a=1, b=2], [c=3, d=4], [e=5]] (each chunk is a LinkedList)
     * }</pre>
     *
     * @param <C> the type of the collection to store the chunks.
     * @param chunkSize the size of each chunk
     * @param collectionSupplier a function which returns a new, empty collection of the appropriate type
     * @return a new Stream consisting of collections of Map.Entry objects, each representing a chunk of the original Stream.
     * @see Stream#split(int, IntFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public <C extends Collection<Map.Entry<K, V>>> Stream<C> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        return _stream.split(chunkSize, collectionSupplier);
    }

    /**
     * Creates a sliding window over the elements of the Stream, where each window is a List of Map.Entry objects.
     * The window moves over the elements of the Stream according to the specified window size.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * Each window contains exactly windowSize elements, except possibly the last few windows
     * which may contain fewer elements if there are not enough remaining elements in the stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *     .sliding(3)
     *     .toList();
     * // Result: [[a=1, b=2, c=3], [b=2, c=3, d=4]]
     * }</pre>
     *
     * @param windowSize the size of the window to be used for sliding over the Stream elements
     * @return a new Stream where each element is a List of Map.Entry objects from the original Stream, representing a window.
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<List<Map.Entry<K, V>>> sliding(final int windowSize) {
        return _stream.sliding(windowSize);
    }

    /**
     * Creates a sliding window over the elements of the Stream, where each window is a collection of Map.Entry objects specified by the collectionSupplier.
     * The window moves over the elements of the Stream according to the specified window size.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * Each window contains exactly windowSize elements, except possibly the last few windows
     * which may contain fewer elements if there are not enough remaining elements in the stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *     .sliding(3, LinkedList::new)
     *     .toList();
     * // Result: [[a=1, b=2, c=3], [b=2, c=3, d=4]] (each window is a LinkedList)
     * }</pre>
     *
     * @param <C> the type of the collection to store the windows.
     * @param windowSize the size of the window to be used for sliding over the Stream elements
     * @param collectionSupplier a function which returns a new, empty collection of the appropriate type
     * @return a new Stream where each element is a collection of Map.Entry objects from the original Stream, representing a window.
     */
    @SequentialOnly
    @IntermediateOp
    public <C extends Collection<Map.Entry<K, V>>> Stream<C> sliding(final int windowSize, final IntFunction<? extends C> collectionSupplier) {
        return _stream.sliding(windowSize, collectionSupplier);
    }

    /**
     * Creates a sliding window over the elements of the Stream, where each window is a List of Map.Entry objects.
     * The window moves over the elements of the Stream according to the specified window size and increment.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * Each window contains exactly windowSize elements, except possibly the last few windows
     * which may contain fewer elements if there are not enough remaining elements in the stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)
     *     .sliding(3, 2)
     *     .toList();
     * // Result: [[a=1, b=2, c=3], [c=3, d=4, e=5]]
     * }</pre>
     *
     * @param windowSize the size of the window to be used for sliding over the Stream elements
     * @param increment the number of elements to move the window forward after each step
     * @return a new Stream where each element is a List of Map.Entry objects from the original Stream, representing a window.
     */
    @SequentialOnly
    @IntermediateOp
    public Stream<List<Map.Entry<K, V>>> sliding(final int windowSize, final int increment) {
        return _stream.sliding(windowSize, increment);
    }

    /**
     * Creates a sliding window over the elements of the Stream, where each window is a collection of Map.Entry objects specified by the collectionSupplier.
     * The window moves over the elements of the Stream according to the specified window size and increment.
     *
     * <p>This is an intermediate operation and can only be processed sequentially.
     * Each window contains exactly windowSize elements, except possibly the last few windows
     * which may contain fewer elements if there are not enough remaining elements in the stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)
     *     .sliding(3, 2, LinkedList::new)
     *     .toList();
     * // Result: [[a=1, b=2, c=3], [c=3, d=4, e=5]] (each window is a LinkedList)
     * }</pre>
     *
     * @param <C> the type of the collection to store the windows.
     * @param windowSize the size of the window to be used for sliding over the Stream elements
     * @param increment the number of elements to move the window forward after each step
     * @param collectionSupplier a function which returns a new, empty collection of the appropriate type
     * @return a new Stream where each element is a collection of Map.Entry objects from the original Stream, representing a window.
     */
    @SequentialOnly
    @IntermediateOp
    public <C extends Collection<Map.Entry<K, V>>> Stream<C> sliding(final int windowSize, final int increment,
            final IntFunction<? extends C> collectionSupplier) {
        return _stream.sliding(windowSize, increment, collectionSupplier);
    }

    /**
     * Returns an EntryStream consisting of the elements that are present in both this stream and the specified collection.
     * For entries that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     * The order of elements in the resulting stream is determined by their order in this stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Collection<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("c", 3),
     *     new SimpleImmutableEntry<>("d", 4)
     * );
     *
     * EntryStream<String, Integer> result = EntryStream.of(map).intersection(entries);
     * // result contains entries: ("b", 2), ("c", 3)
     * }</pre>
     *
     * @param c the collection to find common elements with this stream
     * @return a new EntryStream containing entries present in both this stream and the specified collection,
     *         considering the minimum number of occurrences in either source
     * @see Stream#intersection(Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     * @see #difference(Collection)
     * @see #symmetricDifference(Collection)
     */
    @Override
    public EntryStream<K, V> intersection(final Collection<?> c) {
        return of(_stream.intersection(c));
    }

    /**
     * Returns an EntryStream consisting of the elements that are present in both this stream and the specified map.
     * For entries that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     * The order of elements in the resulting stream is determined by their order in this stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Map.of("b", 2, "c", 3, "d", 4);
     *
     * EntryStream<String, Integer> result = EntryStream.of(map1).intersection(map2);
     * // result contains entries: ("b", 2), ("c", 3)
     * }</pre>
     *
     * @param map the map to find common elements with this stream
     * @return a new EntryStream containing entries present in both this stream and the specified map,
     *         considering the minimum number of occurrences in either source
     * @see Stream#intersection(Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     * @see #difference(Collection)
     * @see #symmetricDifference(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    public EntryStream<K, V> intersection(final Map<? extends K, ? extends V> map) {
        return intersection(N.isEmpty(map) ? N.emptyList() : map.entrySet());
    }

    /**
     * Returns an EntryStream consisting of the elements of this stream that are not present in the specified collection,
     * considering the number of occurrences of each element.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     * The order of elements in the resulting stream is determined by their order in this stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Collection<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("d", 4)
     * );
     *
     * EntryStream<String, Integer> result = EntryStream.of(map).difference(entries);
     * // result contains entries: ("a", 1), ("c", 3)
     * }</pre>
     *
     * @param c the collection to compare against this stream
     * @return a new EntryStream containing the elements that are present in this stream but not in the specified collection,
     *         considering the number of occurrences
     * @see Stream#difference(Collection)
     * @see N#difference(Collection, Collection)
     * @see #intersection(Collection)
     * @see #symmetricDifference(Collection)
     */
    @Override
    public EntryStream<K, V> difference(final Collection<?> c) {
        return of(_stream.difference(c));
    }

    /**
     * Returns an EntryStream consisting of the elements of this stream that are not present in the specified map,
     * considering the number of occurrences of each element.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     * The order of elements in the resulting stream is determined by their order in this stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Map.of("b", 2, "d", 4);
     *
     * EntryStream<String, Integer> result = EntryStream.of(map1).difference(map2);
     * // result contains entries: ("a", 1), ("c", 3)
     * }</pre>
     *
     * @param map the map to compare against this stream
     * @return a new EntryStream containing the elements that are present in this stream but not in the specified map,
     *         considering the number of occurrences
     * @see Stream#difference(Collection)
     * @see N#difference(Collection, Collection)
     * @see #intersection(Collection)
     * @see #symmetricDifference(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    public EntryStream<K, V> difference(final Map<?, ?> map) {
        return difference(N.isEmpty(map) ? N.emptyList() : map.entrySet());
    }

    /**
     * Returns an EntryStream consisting of elements that are present in either this stream or the specified collection,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this stream appearing first,
     * followed by elements from the specified collection.
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
     * Collection<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("c", 3),
     *     new SimpleImmutableEntry<>("d", 4)
     * );
     *
     * EntryStream<String, Integer> result = EntryStream.of(map).symmetricDifference(entries);
     * // result contains entries: ("a", 1), ("d", 4)
     * }</pre>
     *
     * @param c the collection to compare with this stream for symmetric difference
     * @return a new EntryStream containing entries that are present in either this stream or the collection,
     *         but not in both, considering the number of occurrences
     * @see #intersection(Collection)
     * @see #difference(Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     */
    @Override
    public EntryStream<K, V> symmetricDifference(final Collection<? extends Map.Entry<K, V>> c) {
        return of(_stream.symmetricDifference(c));
    }

    /**
     * Returns an EntryStream consisting of elements that are present in either this stream or the specified map,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this stream appearing first,
     * followed by elements from the specified map.
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Map.of("b", 2, "c", 3, "d", 4);
     *
     * EntryStream<String, Integer> result = EntryStream.of(map1).symmetricDifference(map2);
     * // result contains entries: ("a", 1), ("d", 4)
     * }</pre>
     *
     * @param map the map to compare with this stream for symmetric difference
     * @return a new EntryStream containing entries that are present in either this stream or the map,
     *         but not in both, considering the number of occurrences
     * @see #intersection(Collection)
     * @see #difference(Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     */
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    public EntryStream<K, V> symmetricDifference(final Map<? extends K, ? extends V> map) {
        return symmetricDifference(N.isEmpty(map) ? N.emptyList() : (Collection) map.entrySet());
    }

    /**
     * This method always throws {@link UnsupportedOperationException} because sorting
     * {@code Map.Entry} objects by natural order is not well-defined.
     * Use {@link #sorted(Comparator)} with an explicit comparator instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
     * stream.sorted();  // throws UnsupportedOperationException
     *
     * // Use sorted(Comparator) instead:
     * EntryStream<String, Integer> sorted = EntryStream.of("a", 1, "b", 2)
     *     .sorted(Map.Entry.comparingByValue());   // sorts entries by value
     * }</pre>
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #sorted(Comparator)} instead.
     */
    @Deprecated
    @Override
    public EntryStream<K, V> sorted() throws UnsupportedOperationException {
        //    final Comparator<Map.Entry<K, V>> cmp = Comparators.<K, V> comparingByKey((Comparator) Comparators.naturalOrder())
        //            .thenComparing(Comparators.comparingByValue((Comparator) Comparators.naturalOrder()));
        //
        //    return of(_stream.sorted(cmp));

        throw new UnsupportedOperationException("Use sorted(Comparator) instead.");
    }

    /**
     * Returns a sorted EntryStream based on the specified comparator.
     * The entries are sorted according to the order induced by the specified comparator.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by value in descending order
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .sorted(Map.Entry.<String, Integer>comparingByValue().reversed());
     * }</pre>
     *
     * @param comparator the comparator to compare the entries
     * @return a new EntryStream with entries sorted by the specified comparator
     * @see #reverseSorted(Comparator)
     * @see Comparators#comparingByKey()
     * @see Comparators#comparingByKey(Comparator)
     * @see Comparators#comparingByValue()
     * @see Comparators#comparingByValue(Comparator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sorted(final Comparator<? super Map.Entry<K, V>> comparator) {
        return of(_stream.sorted(comparator));
    }

    /**
     * Returns a sorted EntryStream based on the specified key comparator.
     * The entries are sorted according to the order induced by the specified key comparator.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by key in case-insensitive order
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .sortedByKey(String.CASE_INSENSITIVE_ORDER);
     * }</pre>
     *
     * @param keyComparator the comparator to compare the keys
     * @return a new EntryStream with entries sorted by the specified key comparator
     * @see #sortedByValue(Comparator)
     * @see #sorted(Comparator)
     * @see Stream#sorted(Comparator)
     * @see Comparators#comparingByKey(Comparator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sortedByKey(final Comparator<? super K> keyComparator) {
        final Comparator<Map.Entry<K, V>> comparator = Comparators.comparingByKey(keyComparator);

        return of(_stream.sorted(comparator));
    }

    /**
     * Returns a sorted EntryStream based on the specified value comparator.
     * The entries are sorted according to the order induced by the specified value comparator.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by value in descending order
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .sortedByValue(Comparator.reverseOrder());
     * }</pre>
     *
     * @param valueComparator the comparator to compare the values
     * @return a new EntryStream with entries sorted by the specified value comparator
     * @see #sortedByKey(Comparator)
     * @see #sorted(Comparator)
     * @see Stream#sorted(Comparator)
     * @see Comparators#comparingByValue(Comparator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sortedByValue(final Comparator<? super V> valueComparator) {
        final Comparator<Map.Entry<K, V>> comparator = Comparators.comparingByValue(valueComparator);

        return of(_stream.sorted(comparator));
    }

    /**
     * Returns a sorted EntryStream based on the specified extractor function.
     * The entries are sorted according to the natural ordering of the extracted values.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by the length of the key string
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .sortedBy(entry -> entry.getKey().length());
     * }</pre>
     *
     * @param sortKeyExtractor the function to extract the comparable value for comparison
     * @return a new EntryStream with entries sorted by the extracted values
     * @see #sorted(Comparator)
     * @see #sortedByInt(ToIntFunction)
     * @see Stream#sortedBy(Function)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public EntryStream<K, V> sortedBy(final Function<? super Map.Entry<K, V>, ? extends Comparable> sortKeyExtractor) {
        return of(_stream.sortedBy(sortKeyExtractor));
    }

    /**
     * Returns a sorted EntryStream based on the specified extractor function.
     * The entries are sorted according to the integer values extracted from the entries.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by the sum of key and value
     * EntryStream<Integer, Integer> sorted = EntryStream.of(map)
     *     .sortedByInt(entry -> entry.getKey() + entry.getValue());
     * }</pre>
     *
     * @param sortKeyExtractor the function to extract the integer value for comparison
     * @return a new EntryStream with entries sorted by the extracted integer values
     * @see #sortedBy(Function)
     * @see #sortedByLong(ToLongFunction)
     * @see #sorted(Comparator)
     * @see Stream#sortedByInt(ToIntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sortedByInt(final ToIntFunction<? super Map.Entry<K, V>> sortKeyExtractor) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingInt(sortKeyExtractor);

        return sorted(comparator);
    }

    /**
     * Returns a sorted EntryStream based on the specified extractor function.
     * The entries are sorted according to the long values extracted from the entries.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by a timestamp extracted from the value
     * EntryStream<String, Event> sorted = EntryStream.of(eventMap)
     *     .sortedByLong(entry -> entry.getValue().getTimestamp());
     * }</pre>
     *
     * @param sortKeyExtractor the function to extract the long value for comparison
     * @return a new EntryStream with entries sorted by the extracted long values
     * @see #sortedByInt(ToIntFunction)
     * @see #sortedByDouble(ToDoubleFunction)
     * @see #sorted(Comparator)
     * @see Stream#sortedByLong(ToLongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sortedByLong(final ToLongFunction<? super Map.Entry<K, V>> sortKeyExtractor) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingLong(sortKeyExtractor);

        return sorted(comparator);
    }

    /**
     * Returns a sorted EntryStream based on the specified extractor function.
     * The entries are sorted according to the double values extracted from the entries.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by a score calculated from key and value
     * EntryStream<String, Double> sorted = EntryStream.of(scoreMap)
     *     .sortedByDouble(entry -> entry.getValue() * 0.8);
     * }</pre>
     *
     * @param sortKeyExtractor the function to extract the double value for comparison
     * @return a new EntryStream with entries sorted by the extracted double values
     * @see #sortedByLong(ToLongFunction)
     * @see #sortedByInt(ToIntFunction)
     * @see #sorted(Comparator)
     * @see Stream#sortedByDouble(ToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> sortedByDouble(final ToDoubleFunction<? super Map.Entry<K, V>> sortKeyExtractor) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingDouble(sortKeyExtractor);

        return sorted(comparator);
    }

    /**
     * This method always throws {@link UnsupportedOperationException} because reverse-sorting
     * {@code Map.Entry} objects by natural order is not well-defined.
     * Use {@link #reverseSorted(Comparator)} with an explicit comparator instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
     * stream.reverseSorted();  // throws UnsupportedOperationException
     *
     * // Use reverseSorted(Comparator) instead:
     * EntryStream<String, Integer> sorted = EntryStream.of("a", 1, "b", 2)
     *     .reverseSorted(Map.Entry.comparingByValue());   // sorts entries by value (descending)
     * }</pre>
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #reverseSorted(Comparator)} instead.
     */
    @Deprecated
    @Override
    public EntryStream<K, V> reverseSorted() throws UnsupportedOperationException {
        //        @SuppressWarnings("rawtypes")
        //        final Comparator<Map.Entry<K, V>> cmp = Comparators.reverseOrder(Comparators.<K, V> comparingByKey((Comparator) Comparators.naturalOrder())
        //                .thenComparing(Comparators.comparingByValue((Comparator) Comparators.naturalOrder())));
        //
        //        return of(_stream.sorted(cmp));
        throw new UnsupportedOperationException("Use reverseSorted(Comparator) instead.");
    }

    /**
     * Returns a reverse sorted EntryStream based on the specified comparator.
     * The entries are sorted according to the reverse order induced by the specified comparator.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by key length in descending order
     * Comparator<Map.Entry<String, Integer>> cmp =
     *     Comparator.comparing(e -> e.getKey().length());
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .reverseSorted(cmp);
     * }</pre>
     *
     * @param comparator the comparator to compare the entries
     * @return a new EntryStream with entries reverse sorted by the specified comparator
     * @see #sorted(Comparator)
     * @see Comparators#comparingByKey()
     * @see Comparators#comparingByKey(Comparator)
     * @see Comparators#comparingByValue()
     * @see Comparators#comparingByValue(Comparator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> reverseSorted(final Comparator<? super Map.Entry<K, V>> comparator) {
        return of(_stream.reverseSorted(comparator));
    }

    /**
     * Returns a reverse sorted EntryStream based on the specified key extractor function.
     * The entries are sorted according to the reverse order induced by the extracted values.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect and sort all elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort by value in descending order
     * EntryStream<String, Integer> sorted = EntryStream.of(map)
     *     .reverseSortedBy(Map.Entry::getValue);
     * }</pre>
     *
     * @param sortKeyExtractor the function to extract the comparable value for comparison
     * @return a new EntryStream with entries reverse sorted by the extracted values
     * @see #reverseSorted(Comparator)
     * @see #sortedBy(Function)
     * @see Stream#reverseSortedBy(Function)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super Map.Entry<K, V>, ? extends Comparable> sortKeyExtractor) {
        return of(_stream.reverseSortedBy(sortKeyExtractor));
    }

    /**
     * Returns a stream of indexed entries from this EntryStream.
     * Each element in the stream is an Indexed object containing the index and the entry.
     *
     * <p>This is an intermediate operation that preserves the order of elements.
     * The index starts from 0 and increments by 1 for each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .indexed()
     *     .forEach(indexed -> System.out.println(
     *         "Entry at index " + indexed.index() + ": " +
     *         indexed.value().getKey() + "=" + indexed.value().getValue()
     *     ));
     * }</pre>
     *
     * @return a Stream of Indexed objects containing the index and the entry
     * @see Stream#indexed()
     */
    @Override
    public Stream<Indexed<Map.Entry<K, V>>> indexed() {
        return _stream.indexed();
    }

    /**
     * Returns a stream consisting of the distinct elements of this EntryStream.
     * This is a stateful intermediate operation.
     *
     * <p>Entries are compared for equality using their {@code equals} method.
     * For {@code Map.Entry}, this means entries are equal if both their keys and values are equal.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Remove duplicate entries
     * List<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("a", 1),
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("a", 1)
     * );
     * EntryStream.of(entries)
     *     .distinct()
     *     .toList();   // returns entries for ("a", 1), ("b", 2)
     * }</pre>
     *
     * @return a new EntryStream with distinct elements
     * @see Stream#distinct()
     */
    @Override
    public EntryStream<K, V> distinct() {
        return of(_stream.distinct());
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream.
     * The merge function is used to resolve conflicts between duplicate elements.
     *
     * <p>When duplicate entries are encountered, the merge function is applied to determine
     * which entry to keep. This allows for custom handling of duplicates beyond simple removal.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep the entry with larger value when duplicates are found
     * EntryStream.of(entries)
     *     .distinct((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2)
     *     .toList();
     * }</pre>
     *
     * @param mergeFunction the function to merge duplicate elements
     * @return a new EntryStream with distinct elements
     * @see Stream#distinct(BinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> distinct(final BinaryOperator<Map.Entry<K, V>> mergeFunction) {
        return of(_stream.distinct(mergeFunction));
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream based on the keys.
     * The order of the elements is preserved.
     *
     * <p>When multiple entries have the same key, only the first occurrence is kept.
     * This is useful for removing entries with duplicate keys while preserving the first value
     * associated with each key.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep only first occurrence of each key
     * List<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("a", 1),
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("a", 3)
     * );
     * EntryStream.of(entries)
     *     .distinctByKey()
     *     .toList();   // returns entries for ("a", 1), ("b", 2)
     * }</pre>
     *
     * @return a new EntryStream with distinct elements based on the keys
     * @see Stream#distinctBy(Function)
     */
    @SequentialOnly
    @IntermediateOp
    public EntryStream<K, V> distinctByKey() {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();

        if (isParallel()) {
            return of(_stream.sequential()
                    .distinctBy(keyMapper)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.distinctBy(keyMapper));
        }
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream based on the values.
     * The order of the elements is preserved.
     *
     * <p>When multiple entries have the same value, only the first occurrence is kept.
     * This is useful for removing entries with duplicate values while preserving the first key
     * associated with each value.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep only first occurrence of each value
     * List<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     new SimpleImmutableEntry<>("a", 1),
     *     new SimpleImmutableEntry<>("b", 2),
     *     new SimpleImmutableEntry<>("c", 1)
     * );
     * EntryStream.of(entries)
     *     .distinctByValue()
     *     .toList();   // returns entries for ("a", 1), ("b", 2)
     * }</pre>
     *
     * @return a new EntryStream with distinct elements based on the values
     * @see Stream#distinctBy(Function)
     */
    @SequentialOnly
    @IntermediateOp
    public EntryStream<K, V> distinctByValue() {
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(_stream.sequential()
                    .distinctBy(valueMapper)
                    .parallel(_stream.maxThreadNum(), _stream.splitor(), _stream.asyncExecutor(), _stream.cancelUncompletedThreads()));
        } else {
            return of(_stream.distinctBy(valueMapper));
        }
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream based on the keys extracted by the specified key extractor function.
     * The order of the elements is preserved.
     *
     * <p>When multiple entries produce the same key from the extractor function, only the first
     * occurrence is kept. This allows for custom definitions of uniqueness.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep entries with distinct key lengths
     * EntryStream<String, Integer> stream = EntryStream.of(map);
     * stream.distinctBy(e -> e.getKey().length())
     *       .toList();   // keeps first entry for each unique key length
     * }</pre>
     *
     * @param keyMapper the function to extract the key for comparison
     * @return a new EntryStream with distinct elements based on the extracted keys
     * @see Stream#distinctBy(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> distinctBy(final Function<? super Map.Entry<K, V>, ?> keyMapper) {
        return of(_stream.distinctBy(keyMapper));
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream based on the keys extracted by the specified key extractor function.
     * The merge function is used to resolve conflicts between duplicate elements.
     *
     * <p>When multiple entries produce the same key from the extractor function, the merge function
     * determines which entry to keep. This allows for both custom uniqueness definitions and custom
     * duplicate handling.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep entries with distinct keys, preferring higher values
     * EntryStream<String, Integer> stream = EntryStream.of(map);
     * stream.distinctBy(Map.Entry::getKey,
     *                   (e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2)
     *       .toList();
     * }</pre>
     *
     * @param keyMapper the function to extract the key for comparison
     * @param mergeFunction the function to merge duplicate elements
     * @return a new EntryStream with distinct elements based on the extracted keys
     * @see Stream#distinctBy(Function, BinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    public EntryStream<K, V> distinctBy(final Function<? super Map.Entry<K, V>, ?> keyMapper, final BinaryOperator<Map.Entry<K, V>> mergeFunction) {
        return of(_stream.distinctBy(keyMapper, mergeFunction));
    }

    /**
     * Returns a new EntryStream with the elements rotated by the specified distance.
     * Positive values rotate the elements to the right, while negative values rotate them to the left.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect all elements before rotating them.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Rotate entries to the right by 2 positions
     * List<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     entry("a", 1), entry("b", 2), entry("c", 3), entry("d", 4)
     * );
     * EntryStream.of(entries)
     *     .rotated(2)
     *     .toList();   // returns [("c", 3), ("d", 4), ("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param distance the distance to rotate the elements
     * @return a new EntryStream with the elements rotated by the specified distance
     * @see Stream#rotated(int)
     */
    @Override
    public EntryStream<K, V> rotated(final int distance) {
        return of(_stream.rotated(distance));
    }

    /**
     * Returns a new EntryStream with the elements shuffled randomly.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect all elements before shuffling them.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Shuffle entries randomly
     * EntryStream.of(map)
     *     .shuffled()
     *     .toList();   // returns entries in random order
     * }</pre>
     *
     * @return a new EntryStream with the elements shuffled
     * @see Stream#shuffled()
     */
    @Override
    public EntryStream<K, V> shuffled() {
        return of(_stream.shuffled());
    }

    /**
     * Returns a new EntryStream with the elements shuffled using the specified random.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect all elements before shuffling them.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Shuffle entries with a specific seed for reproducible results
     * Random rnd = new Random(42);
     * EntryStream.of(map)
     *     .shuffled(rnd)
     *     .toList();   // returns entries in a deterministic random order
     * }</pre>
     *
     * @param rnd the random to shuffle the elements
     * @return a new EntryStream with the elements shuffled using the specified random
     * @see Stream#shuffled(Random)
     */
    @Override
    public EntryStream<K, V> shuffled(final Random rnd) {
        return of(_stream.shuffled(rnd));
    }

    /**
     * Returns a new EntryStream with the elements in reverse order.
     *
     * <p>This is a stateful intermediate operation that triggers a terminal operation internally
     * to collect all elements before reversing them.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reverse the order of entries
     * List<Map.Entry<String, Integer>> entries = Arrays.asList(
     *     entry("a", 1), entry("b", 2), entry("c", 3)
     * );
     * EntryStream.of(entries)
     *     .reversed()
     *     .toList();   // returns [("c", 3), ("b", 2), ("a", 1)]
     * }</pre>
     *
     * @return a new EntryStream with the elements reversed
     * @see Stream#reversed()
     */
    @Override
    public EntryStream<K, V> reversed() {
        return of(_stream.reversed());
    }

    /**
     * Returns a new EntryStream with the elements cycled.
     * The stream will repeat its elements indefinitely.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     * All elements are stored in memory after the first iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an infinite stream of cycling entries
     * EntryStream.of(smallMap)
     *     .cycled()
     *     .limit(10)
     *     .toList();   // returns 10 entries, cycling through the original entries
     * }</pre>
     *
     * @return a new EntryStream with the elements cycled
     * @see Stream#cycled()
     */
    @Override
    public EntryStream<K, V> cycled() {
        return of(_stream.cycled());
    }

    /**
     * Returns a new EntryStream with the elements cycled a specified number of times.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     * All elements are stored in memory after the first iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Repeat entries 3 times
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * EntryStream.of(map)
     *     .cycled(3)
     *     .toList();   // returns [("a", 1), ("b", 2), ("a", 1), ("b", 2), ("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param rounds the number of rounds to cycle the elements
     * @return a new EntryStream with the elements cycled the specified number of times
     * @throws IllegalArgumentException if rounds is negative
     * @see Stream#cycled(long)
     */
    @Override
    public EntryStream<K, V> cycled(final long rounds) {
        return of(_stream.cycled(rounds));
    }

    /**
     * Prepends the specified map to the current EntryStream.
     * If the map is empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> prefix = new LinkedHashMap<>();
     * prefix.put("x", 10); prefix.put("y", 20);
     * EntryStream.of("a", 1, "b", 2)
     *     .prepend(prefix)
     *     .toList();   // returns [("x", 10), ("y", 20), ("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param <M> the type of the map to prepend
     * @param map the map to prepend to the stream
     * @return a new EntryStream with the elements of the specified map prepended
     * @see Stream#prepend(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    public <M extends Map<? extends K, ? extends V>> EntryStream<K, V> prepend(final M map) {
        if (N.isEmpty(map)) {
            return of(_stream);
        }

        final Set<Map.Entry<K, V>> set = (Set) map.entrySet();

        return of(_stream.prepend(set));
    }

    /**
     * Prepends the specified stream to the current EntryStream.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> prefix = EntryStream.of("x", 10, "y", 20);
     * EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
     * main.prepend(prefix)
     *     .toList();   // returns [("x", 10), ("y", 20), ("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param stream the stream to prepend to the current stream
     * @return a new EntryStream with the elements of the specified stream prepended
     * @see BaseStream#prepend(BaseStream)
     */
    @Override
    public EntryStream<K, V> prepend(final EntryStream<K, V> stream) {
        return of(_stream.prepend(stream._stream));
    }

    /**
     * Prepends the specified optional entry to the current EntryStream.
     * If the optional entry is empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Map.Entry<String, Integer>> optional = Optional.of(entry("x", 10));
     * EntryStream.of("a", 1, "b", 2)
     *     .prepend(optional)
     *     .toList();   // returns [("x", 10), ("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param op the optional entry to prepend to the stream
     * @return a new EntryStream with the optional entry prepended
     * @see #prepend(Map)
     */
    @Override
    public EntryStream<K, V> prepend(final Optional<Map.Entry<K, V>> op) {
        if (op.isEmpty()) {
            return this;
        }

        return of(_stream.prepend(op));
    }

    /**
     * Appends the specified map to the current EntryStream.
     * If the map is empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> suffix = new LinkedHashMap<>();
     * suffix.put("x", 10); suffix.put("y", 20);
     * EntryStream.of("a", 1, "b", 2)
     *     .append(suffix)
     *     .toList();   // returns [("a", 1), ("b", 2), ("x", 10), ("y", 20)]
     * }</pre>
     *
     * @param <M> the type of the map to append
     * @param map the map to append to the stream
     * @return a new EntryStream with the elements of the specified map appended
     * @see Stream#append(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    public <M extends Map<? extends K, ? extends V>> EntryStream<K, V> append(final M map) {
        if (N.isEmpty(map)) {
            return of(_stream);
        }

        final Set<Map.Entry<K, V>> set = (Set) map.entrySet();

        return of(_stream.append(set));
    }

    /**
     * Appends the specified stream to the current EntryStream.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
     * EntryStream<String, Integer> suffix = EntryStream.of("x", 10, "y", 20);
     * main.append(suffix)
     *     .toList();   // returns [("a", 1), ("b", 2), ("x", 10), ("y", 20)]
     * }</pre>
     *
     * @param stream the stream to append to the current stream
     * @return a new EntryStream with the elements of the specified stream appended
     * @see BaseStream#append(BaseStream)
     */
    @Override
    public EntryStream<K, V> append(final EntryStream<K, V> stream) {
        return of(_stream.append(stream._stream));
    }

    /**
     * Appends the specified optional entry to the current EntryStream.
     * If the optional entry is empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Map.Entry<String, Integer>> optional = Optional.of(entry("x", 10));
     * EntryStream.of("a", 1, "b", 2)
     *     .append(optional)
     *     .toList();   // returns [("a", 1), ("b", 2), ("x", 10)]
     * }</pre>
     *
     * @param op the optional entry to append to the stream
     * @return a new EntryStream with the optional entry appended
     * @see #append(Map)
     */
    @Override
    public EntryStream<K, V> append(final Optional<Map.Entry<K, V>> op) {
        if (op.isEmpty()) {
            return this;
        }

        return of(_stream.append(op));
    }

    /**
     * Appends the specified map to the current EntryStream if the stream is empty.
     * If the map is empty or the stream is not empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> defaults = Map.of("default", 0);
     *
     * // Empty stream gets defaults appended
     * EntryStream.<String, Integer>empty()
     *     .appendIfEmpty(defaults)
     *     .toList();   // returns [("default", 0)]
     *
     * // Non-empty stream remains unchanged
     * EntryStream.of("a", 1)
     *     .appendIfEmpty(defaults)
     *     .toList();   // returns [("a", 1)]
     * }</pre>
     *
     * @param <M> the type of the map to append
     * @param map the map to append to the stream if the stream is empty
     * @return a new EntryStream with the elements of the specified map appended if the stream is empty
     * @see Stream#appendIfEmpty(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    public <M extends Map<? extends K, ? extends V>> EntryStream<K, V> appendIfEmpty(final M map) {
        if (N.isEmpty(map)) {
            return of(_stream);
        }

        final Set<Map.Entry<K, V>> set = (Set) map.entrySet();

        return of(_stream.appendIfEmpty(set));
    }

    /**
     * Appends the specified EntryStream to the current EntryStream if the stream is empty.
     * If the stream is not empty, the original stream is returned.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     * The supplier is only invoked if the stream is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Supplier<EntryStream<String, Integer>> defaultSupplier =
     *     () -> EntryStream.of("default", 0);
     *
     * // Empty stream gets defaults appended
     * EntryStream.<String, Integer>empty()
     *     .appendIfEmpty(defaultSupplier)
     *     .toList();   // returns [("default", 0)]
     *
     * // Non-empty stream remains unchanged
     * EntryStream.of("a", 1)
     *     .appendIfEmpty(defaultSupplier)
     *     .toList();   // returns [("a", 1)]
     * }</pre>
     *
     * @param supplier the supplier providing the EntryStream to append if the current stream is empty
     * @return a new EntryStream with the elements of the specified EntryStream appended if the current stream is empty
     * @see Stream#appendIfEmpty(Supplier)
     */
    @Override
    public EntryStream<K, V> appendIfEmpty(final Supplier<? extends EntryStream<K, V>> supplier) {
        return EntryStream.of(_stream.appendIfEmpty(() -> supplier.get()._stream));
    }

    /**
     * Executes the specified action if the stream is empty.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     * The action is executed during terminal operation execution, not when this method is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .filter(e -> e.getValue() > 100)
     *     .ifEmpty(() -> System.out.println("No entries with value > 100"))
     *     .toList();
     * }</pre>
     *
     * @param action the action to execute if the stream is empty
     * @return a new EntryStream with the action registered to execute if the stream is empty
     * @throws IllegalStateException if the stream has already been operated upon or closed
     * @see Stream#ifEmpty(Runnable)
     */
    @Override
    public EntryStream<K, V> ifEmpty(final Runnable action) throws IllegalStateException {
        return EntryStream.of(_stream.ifEmpty(action));
    }

    /**
     * Skips the first <i>n</i> elements of this EntryStream.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *     .skip(2)
     *     .toList();   // returns [("c", 3), ("d", 4)]
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new EntryStream with the first <i>n</i> elements skipped
     * @throws IllegalArgumentException if n is negative
     * @see Stream#skip(long)
     */
    @SequentialOnly
    @IntermediateOp
    @Override
    public EntryStream<K, V> skip(final long n) {
        return of(_stream.skip(n));
    }

    /**
     * Skips the first <i>n</i> elements of this EntryStream and applies the specified consumer to each skipped element.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map.Entry<String, Integer>> skipped = new ArrayList<>();
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *     .skip(2, skipped::add)
     *     .toList();   // returns [("c", 3), ("d", 4)]
     * // skipped contains [("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param n the number of elements to skip
     * @param onSkip the consumer to apply to each skipped element
     * @return a new EntryStream with the first <i>n</i> elements skipped
     * @throws IllegalArgumentException if n is negative
     * @see #skip(long)
     */
    @Override
    public EntryStream<K, V> skip(final long n, final Consumer<? super Map.Entry<K, V>> onSkip) {
        return of(_stream.skip(n, onSkip));
    }

    /**
     * Limits the number of elements in this EntryStream to the specified maximum size.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *     .limit(2)
     *     .toList();   // returns [("a", 1), ("b", 2)]
     * }</pre>
     *
     * @param maxSize the maximum number of elements to include in the stream
     * @return a new EntryStream with at most <i>maxSize</i> elements
     * @throws IllegalArgumentException if maxSize is negative
     * @see Stream#limit(long)
     */
    @SequentialOnly
    @IntermediateOp
    @Override
    public EntryStream<K, V> limit(final long maxSize) {
        return of(_stream.limit(maxSize));
    }

    /**
     * Returns a new EntryStream with entries selected at regular intervals.
     * The step parameter determines the interval between entries in the resulting stream.
     * For example, a step of 2 will include every second entry from the original stream.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)
     *     .step(2)
     *     .toList();   // returns [("a", 1), ("c", 3), ("e", 5)]
     * }</pre>
     *
     * @param step the interval between elements to include in the resulting stream
     * @return a new EntryStream consisting of every 'step'th entry of this stream.
     * @throws IllegalArgumentException if step is not positive
     * @see Stream#step(long)
     */
    @Override
    public EntryStream<K, V> step(final long step) {
        return of(_stream.step(step));
    }

    /**
     * Limits the rate at which elements are processed in this EntryStream.
     * The rateLimiter parameter controls the rate of processing.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter rateLimiter = RateLimiter.create(2.0);   // 2 permits per second
     * EntryStream.of(map)
     *     .rateLimited(rateLimiter)
     *     .forEach((k, v) -> processEntry(k, v));   // processes at most 2 entries per second
     * }</pre>
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every entry?</th><th>Drops entries?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each entry except the first (constant spacing between entries)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a stream with a fixed gap between entries</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest entry in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded entries)</td>
     *     <td>Collapse rapid bursts into a single trailing entry</td>
     *   </tr>
     * </table>
     *
     * @param rateLimiter the RateLimiter to control the rate of processing
     * @return a new EntryStream with rate-limited processing
     * @see #delay(Duration)
     * @see #debounce(Duration)
     * @see Stream#rateLimited(RateLimiter)
     */
    @Override
    public EntryStream<K, V> rateLimited(final RateLimiter rateLimiter) {
        return of(_stream.rateLimited(rateLimiter));
    }

    /**
     * Delays each element in this EntryStream by the given duration except for the first element.
     *
     * <p>This method only runs sequentially, even in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .delay(Duration.ofSeconds(1))
     *     .forEach((k, v) -> System.out.println(k + "=" + v));
     * // Prints "a=1" immediately, then "b=2" after 1 second, "c=3" after 2 seconds
     * }</pre>
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every entry?</th><th>Drops entries?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each entry except the first (constant spacing between entries)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a stream with a fixed gap between entries</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest entry in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded entries)</td>
     *     <td>Collapse rapid bursts into a single trailing entry</td>
     *   </tr>
     * </table>
     *
     * @param duration the duration to delay each element
     * @return a new EntryStream with delayed processing
     * @see #rateLimited(RateLimiter)
     * @see #debounce(Duration)
     * @see Stream#delay(Duration)
     */
    @Override
    public EntryStream<K, V> delay(final Duration duration) {
        return of(_stream.delay(duration));
    }

    /**
     * Returns a new {@code EntryStream} that performs pull-based trailing debouncing: it keeps the latest
     * pulled entry and emits it when a later upstream pull shows that no newer entry arrived for at least
     * the specified {@code duration}, or when the upstream is exhausted.
     *
     * <p>When entries are pulled rapidly &mdash; a "burst" in which each entry arrives less than
     * {@code duration} after the previous one &mdash; all but the most recent entry of the burst are
     * discarded; the most recent entry survives. It is emitted when a later pull observes a gap of at least
     * {@code duration}, or when the upstream is exhausted. The final entry of the stream is always emitted
     * without waiting for an additional timer. Arrival time is measured with {@link System#currentTimeMillis()}
     * at the moment each entry is pulled, so this operator is only meaningful for streams whose upstream
     * produces entries over time; for a stream that produces all of its entries immediately, only the single
     * last entry is emitted.
     *
     * <p>This differs from {@link #rateLimited(RateLimiter)}, which spreads permits evenly over time. It is
     * also not a scheduler-based debounce: it does not start a background timer, and pending entries are
     * emitted only while downstream iteration pulls from this stream.
     *
     * <p>The debounce step is evaluated sequentially. If this method is invoked on a parallel stream, the
     * returned stream may switch back to parallel mode for downstream operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse rapid bursts: keep the latest entry and emit it when iteration observes a 300ms quiet gap.
     * EntryStream.of(updates)
     *     .debounce(Duration.ofMillis(300))
     *     .forEach((key, value) -> cache.put(key, value));
     * }</pre>
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every entry?</th><th>Drops entries?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each entry except the first (constant spacing between entries)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a stream with a fixed gap between entries</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest entry in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded entries)</td>
     *     <td>Collapse rapid bursts into a single trailing entry</td>
     *   </tr>
     * </table>
     *
     * @param duration the quiet period used to decide whether a pending entry has been superseded.
     *                 Must not be {@code null} and must have a positive millisecond value.
     * @return a new {@code EntryStream} that emits the most recent entry of each burst when a later pull
     *         observes a quiet gap, and always emits the final pending entry
     * @throws IllegalArgumentException if {@code duration} is {@code null} or has a non-positive
     *         millisecond value
     * @see #rateLimited(RateLimiter)
     * @see #delay(Duration)
     * @see Stream#debounce(Duration)
     */
    @Override
    public EntryStream<K, V> debounce(Duration duration) {
        return of(_stream.debounce(duration));
    }

    /**
     * Performs the provided action on the entries pulled by downstream/terminal operation.
     * Mostly, it's used for debugging.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .peek(e -> System.out.println("Processing: " + e))
     *     .filter(e -> e.getValue() > 0)
     *     .toList();
     * }</pre>
     *
     * @param action the action to be performed on the entries pulled by downstream/terminal operation
     * @return a new EntryStream consisting of the elements of this stream with the provided action applied to each element
     * @see #onEach(BiConsumer)
     * @see #onEach(Consumer)
     * @see Stream#onEach(Consumer)
     */
    @Override
    public EntryStream<K, V> peek(final Consumer<? super Map.Entry<K, V>> action) {
        return of(_stream.peek(action));
    }

    /**
     * Performs the provided action on the entries pulled by downstream/terminal operation.
     * Mostly, it's used for debugging.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .peek((k, v) -> System.out.println("Processing: " + k + "=" + v))
     *     .filter(e -> e.getValue() > 0)
     *     .toList();
     * }</pre>
     *
     * @param action the action to be performed on the entries pulled by downstream/terminal operation
     * @return a new EntryStream consisting of the elements of this stream with the provided action applied to each element
     * @see #onEach(Consumer)
     * @see #onEach(BiConsumer)
     * @see Stream#onEach(Consumer)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> peek(final BiConsumer<? super K, ? super V> action) {
        return of(_stream.peek(Fn.Entries.c(action)));
    }

    /**
     * Performs the provided action on the entries pulled by downstream/terminal operation.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .onEach(e -> System.out.println("Processing: " + e))
     *     .filter(e -> e.getValue() > 0)
     *     .toList();
     * }</pre>
     *
     * @param action the action to be performed on the entries pulled by downstream/terminal operation
     * @return a new EntryStream consisting of the elements of this stream with the provided action applied to each element
     * @see #peek(BiConsumer)
     * @see #peek(Consumer)
     * @see Stream#peek(Consumer)
     */
    @Override
    public EntryStream<K, V> onEach(final Consumer<? super Map.Entry<K, V>> action) {
        return of(_stream.onEach(action));
    }

    /**
     * Performs the provided action on the entries pulled by downstream/terminal operation.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .onEach((k, v) -> System.out.println("Processing: " + k + "=" + v))
     *     .filter(e -> e.getValue() > 0)
     *     .toList();
     * }</pre>
     *
     * @param action the action to be performed on the entries pulled by downstream/terminal operation
     * @return a new EntryStream consisting of the elements of this stream with the provided action applied to each element
     * @see #peek(Consumer)
     * @see #peek(BiConsumer)
     * @see Stream#peek(Consumer)
     */
    @ParallelSupported
    @IntermediateOp
    public EntryStream<K, V> onEach(final BiConsumer<? super K, ? super V> action) {
        return of(_stream.onEach(Fn.Entries.c(action)));
    }

    /**
     * Performs the provided action on each entry of the EntryStream.
     * This is a terminal operation that consumes all entries in the stream.
     *
     * <p>The action is performed on Map.Entry objects. For actions on separate key-value pairs,
     * use {@link #forEach(Throwables.BiConsumer)}.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .forEach(entry -> System.out.println(entry.getKey() + " = " + entry.getValue()));
     *
     * // With exception handling
     * EntryStream.of(map)
     *     .forEach(entry -> processEntry(entry));   // invokes processEntry, which may throw a checked exception
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the action
     * @param action the action to be performed for each entry
     * @throws E if the action throws an exception
     * @see #forEach(Throwables.BiConsumer)
     * @see Stream#forEach(Throwables.Consumer)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> void forEach(final Throwables.Consumer<? super Map.Entry<K, V>, E> action) throws E {
        _stream.forEach(action);
    }

    /**
     * Performs the provided action on each key-value pair of the EntryStream.
     * This is a terminal operation that consumes all entries in the stream.
     *
     * <p>Unlike {@link #forEach(Throwables.Consumer)}, this method provides the key and value
     * as separate parameters to the action, which can be more convenient.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .forEach((key, value) -> System.out.println(key + " = " + value));
     *
     * // Process key-value pairs
     * EntryStream.of(userScores)
     *     .forEach((userId, score) -> updateUserScore(userId, score));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the action
     * @param action the action to be performed for each key-value pair
     * @throws E if the action throws an exception
     * @see #forEach(Throwables.Consumer)
     * @see Stream#forEach(Throwables.Consumer)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> void forEach(final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
        _stream.forEach(Fn.Entries.ec(action));
    }

    /**
     * Performs the provided action on each entry of the EntryStream along with its index.
     * This is a terminal operation that consumes all entries in the stream.
     *
     * <p>The index starts from 0 and increments for each entry processed. This is useful
     * when you need to track the position of entries during processing.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .forEachIndexed((index, entry) ->
     *         System.out.println(index + ": " + entry.getKey() + " = " + entry.getValue()));
     * // Output:
     * // 0: a = 1
     * // 1: b = 2
     * // 2: c = 3
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the action
     * @param action the action to be performed for each entry with its index
     * @throws E if the action throws an exception
     * @see Stream#forEachIndexed(Throwables.IntObjConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> void forEachIndexed(final Throwables.IntObjConsumer<? super Map.Entry<K, V>, E> action) throws E {
        _stream.forEachIndexed(action);
    }

    /**
     * Returns an Optional containing the minimum entry of this EntryStream according to the provided comparator.
     * This is a terminal operation.
     *
     * <p>The comparator should compare Map.Entry objects. For comparing by key or value specifically,
     * use {@link #minByKey(Comparator)} or {@link #minByValue(Comparator)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with minimum key length
     * Optional<Map.Entry<String, Integer>> min = EntryStream.of(map)
     *     .min((e1, e2) -> Integer.compare(e1.getKey().length(), e2.getKey().length()));
     *
     * // Using a custom comparator for complex comparison
     * Optional<Map.Entry<String, Person>> youngest = EntryStream.of(personMap)
     *     .min((e1, e2) -> Integer.compare(e1.getValue().getAge(), e2.getValue().getAge()));
     * }</pre>
     *
     * @param comparator the comparator to compare the entries
     * @return an {@code Optional} containing the minimum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #minByKey(Comparator)
     * @see #minByValue(Comparator)
     * @see #minBy(Function)
     * @see Stream#min(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> min(final Comparator<? super Map.Entry<K, V>> comparator) {
        return _stream.min(comparator);
    }

    /**
     * Returns an Optional containing the minimum entry of this EntryStream according to the provided key comparator.
     * This is a terminal operation.
     *
     * <p>This method compares entries based solely on their keys, ignoring the values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with minimum key (natural ordering)
     * Optional<Map.Entry<String, Integer>> min = EntryStream.of(map)
     *     .minByKey(Comparator.naturalOrder());
     *
     * // Find entry with minimum key by length
     * Optional<Map.Entry<String, Integer>> shortest = EntryStream.of(map)
     *     .minByKey(Comparator.comparingInt(String::length));
     * }</pre>
     *
     * @param keyComparator the comparator to compare the keys
     * @return an {@code Optional} containing the minimum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #min(Comparator)
     * @see #minByValue(Comparator)
     * @see Stream#min(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> minByKey(final Comparator<? super K> keyComparator) {
        return _stream.min(Comparators.comparingBy(Fn.key(), keyComparator));
    }

    /**
     * Returns an Optional containing the minimum entry of this EntryStream according to the provided value comparator.
     * This is a terminal operation.
     *
     * <p>This method compares entries based solely on their values, ignoring the keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with minimum value
     * Optional<Map.Entry<String, Integer>> min = EntryStream.of(scores)
     *     .minByValue(Comparator.naturalOrder());
     *
     * // Find person entry with minimum age
     * Optional<Map.Entry<String, Person>> youngest = EntryStream.of(persons)
     *     .minByValue(Comparator.comparingInt(Person::getAge));
     * }</pre>
     *
     * @param valueComparator the comparator to compare the values
     * @return an {@code Optional} containing the minimum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #min(Comparator)
     * @see #minByKey(Comparator)
     * @see Stream#min(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> minByValue(final Comparator<? super V> valueComparator) {
        return _stream.min(Comparators.comparingBy(Fn.value(), valueComparator));
    }

    /**
     * Returns an Optional containing the minimum entry of this EntryStream based on the natural ordering
     * of the keys extracted by the key mapper.
     * This is a terminal operation.
     *
     * <p>Null keys are considered bigger than non-null keys in the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with minimum total (key length + value)
     * Optional<Map.Entry<String, Integer>> min = EntryStream.of(map)
     *     .minBy(entry -> entry.getKey().length() + entry.getValue());
     *
     * // Find entry with earliest date
     * Optional<Map.Entry<String, Event>> earliest = EntryStream.of(events)
     *     .minBy(entry -> entry.getValue().getDate());
     * }</pre>
     *
     * @param keyMapper the function to extract the comparable key for comparison
     * @return an {@code Optional} containing the minimum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #min(Comparator)
     * @see Stream#minBy(Function)
     */
    @ParallelSupported
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<Map.Entry<K, V>> minBy(final Function<? super Map.Entry<K, V>, ? extends Comparable> keyMapper) {
        return _stream.minBy(keyMapper);
    }

    /**
     * Returns an Optional containing the maximum entry of this EntryStream according to the provided comparator.
     * This is a terminal operation.
     *
     * <p>The comparator should compare Map.Entry objects. For comparing by key or value specifically,
     * use {@link #maxByKey(Comparator)} or {@link #maxByValue(Comparator)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with maximum key length
     * Optional<Map.Entry<String, Integer>> max = EntryStream.of(map)
     *     .max((e1, e2) -> Integer.compare(e1.getKey().length(), e2.getKey().length()));
     *
     * // Using a custom comparator for complex comparison
     * Optional<Map.Entry<String, Person>> oldest = EntryStream.of(personMap)
     *     .max((e1, e2) -> Integer.compare(e1.getValue().getAge(), e2.getValue().getAge()));
     * }</pre>
     *
     * @param comparator the comparator to compare the entries
     * @return an {@code Optional} containing the maximum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #maxByKey(Comparator)
     * @see #maxByValue(Comparator)
     * @see #maxBy(Function)
     * @see Stream#max(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> max(final Comparator<? super Map.Entry<K, V>> comparator) {
        return _stream.max(comparator);
    }

    /**
     * Returns an Optional containing the maximum entry of this EntryStream according to the provided key comparator.
     * This is a terminal operation.
     *
     * <p>This method compares entries based solely on their keys, ignoring the values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with maximum key (natural ordering)
     * Optional<Map.Entry<String, Integer>> max = EntryStream.of(map)
     *     .maxByKey(Comparator.naturalOrder());
     *
     * // Find entry with longest key
     * Optional<Map.Entry<String, Integer>> longest = EntryStream.of(map)
     *     .maxByKey(Comparator.comparingInt(String::length));
     * }</pre>
     *
     * @param keyComparator the comparator to compare the keys
     * @return an {@code Optional} containing the maximum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #max(Comparator)
     * @see #maxByValue(Comparator)
     * @see Stream#max(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> maxByKey(final Comparator<? super K> keyComparator) {
        return _stream.max(Comparators.comparingBy(Fn.key(), keyComparator));
    }

    /**
     * Returns an Optional containing the maximum entry of this EntryStream according to the provided value comparator.
     * This is a terminal operation.
     *
     * <p>This method compares entries based solely on their values, ignoring the keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with maximum value
     * Optional<Map.Entry<String, Integer>> max = EntryStream.of(scores)
     *     .maxByValue(Comparator.naturalOrder());
     *
     * // Find person entry with maximum age
     * Optional<Map.Entry<String, Person>> oldest = EntryStream.of(persons)
     *     .maxByValue(Comparator.comparingInt(Person::getAge));
     * }</pre>
     *
     * @param valueComparator the comparator to compare the values
     * @return an {@code Optional} containing the maximum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #max(Comparator)
     * @see #maxByKey(Comparator)
     * @see Stream#max(Comparator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> maxByValue(final Comparator<? super V> valueComparator) {
        return _stream.max(Comparators.comparingBy(Fn.value(), valueComparator));
    }

    /**
     * Returns an Optional containing the maximum entry of this EntryStream based on the natural ordering
     * of the keys extracted by the key mapper.
     * This is a terminal operation.
     *
     * <p>Null keys are considered smaller than non-null keys in the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find entry with maximum total (key length + value)
     * Optional<Map.Entry<String, Integer>> max = EntryStream.of(map)
     *     .maxBy(entry -> entry.getKey().length() + entry.getValue());
     *
     * // Find entry with latest date
     * Optional<Map.Entry<String, Event>> latest = EntryStream.of(events)
     *     .maxBy(entry -> entry.getValue().getDate());
     * }</pre>
     *
     * @param keyMapper the function to extract the comparable key for comparison
     * @return an {@code Optional} containing the maximum entry of this EntryStream, or an empty {@code Optional} if the stream is empty
     * @see #max(Comparator)
     * @see Stream#maxBy(Function)
     */
    @ParallelSupported
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<Map.Entry<K, V>> maxBy(final Function<? super Map.Entry<K, V>, ? extends Comparable> keyMapper) {
        return _stream.maxBy(keyMapper);
    }

    /**
     * Checks if any elements of this EntryStream match the provided predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>The predicate operates on Map.Entry objects. For predicates on separate key-value pairs,
     * use {@link #anyMatch(Throwables.BiPredicate)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any entry has a null value
     * boolean hasNullValue = EntryStream.of(map)
     *     .anyMatch(entry -> entry.getValue() == null);
     *
     * // Check if any key starts with "test"
     * boolean hasTestKey = EntryStream.of(map)
     *     .anyMatch(entry -> entry.getKey().startsWith("test"));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if any elements match the predicate, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #anyMatch(Throwables.BiPredicate)
     * @see #allMatch(Throwables.Predicate)
     * @see #noneMatch(Throwables.Predicate)
     * @see Stream#anyMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean anyMatch(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.anyMatch(predicate);
    }

    /**
     * Checks if any elements of this EntryStream match the provided bi-predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Unlike {@link #anyMatch(Throwables.Predicate)}, this method provides the key and value
     * as separate parameters to the predicate, which can be more convenient.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any score is above 90
     * boolean hasHighScore = EntryStream.of(studentScores)
     *     .anyMatch((student, score) -> score > 90);
     *
     * // Check if any key equals its value
     * boolean hasEqualKeyValue = EntryStream.of(map)
     *     .anyMatch((key, value) -> key.equals(value));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return {@code true} if any elements match the predicate, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #anyMatch(Throwables.Predicate)
     * @see Stream#anyMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean anyMatch(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.anyMatch(Fn.Entries.ep(predicate));
    }

    /**
     * Checks if all elements of this EntryStream match the provided predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Returns {@code true} if all elements match the predicate or if the stream is empty.
     * The predicate operates on Map.Entry objects. For predicates on separate key-value pairs,
     * use {@link #allMatch(Throwables.BiPredicate)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all values are positive
     * boolean allPositive = EntryStream.of(scores)
     *     .allMatch(entry -> entry.getValue() > 0);
     *
     * // Check if all keys are non-empty strings
     * boolean allNonEmpty = EntryStream.of(map)
     *     .allMatch(entry -> !entry.getKey().isEmpty());
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if all elements match the predicate or this EntryStream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #allMatch(Throwables.BiPredicate)
     * @see #anyMatch(Throwables.Predicate)
     * @see #noneMatch(Throwables.Predicate)
     * @see Stream#allMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean allMatch(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.allMatch(predicate);
    }

    /**
     * Checks if all elements of this EntryStream match the provided bi-predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Returns {@code true} if all elements match the predicate or if the stream is empty.
     * Unlike {@link #allMatch(Throwables.Predicate)}, this method provides the key and value
     * as separate parameters to the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all scores are passing (>= 60)
     * boolean allPassing = EntryStream.of(studentScores)
     *     .allMatch((student, score) -> score >= 60);
     *
     * // Check if all keys are shorter than their values
     * boolean keyShorterThanValue = EntryStream.of(stringMap)
     *     .allMatch((key, value) -> key.length() < value.length());
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return {@code true} if all elements match the predicate or this EntryStream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #allMatch(Throwables.Predicate)
     * @see Stream#allMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean allMatch(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.allMatch(Fn.Entries.ep(predicate));
    }

    /**
     * Checks if no elements of this EntryStream match the provided predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Returns {@code true} if no elements match the predicate or if the stream is empty.
     * The predicate operates on Map.Entry objects. For predicates on separate key-value pairs,
     * use {@link #noneMatch(Throwables.BiPredicate)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if no values are negative
     * boolean noNegatives = EntryStream.of(scores)
     *     .noneMatch(entry -> entry.getValue() < 0);
     *
     * // Check if no keys contain spaces
     * boolean noSpaces = EntryStream.of(map)
     *     .noneMatch(entry -> entry.getKey().contains(" "));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if no elements match the predicate or this EntryStream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #noneMatch(Throwables.BiPredicate)
     * @see #anyMatch(Throwables.Predicate)
     * @see #allMatch(Throwables.Predicate)
     * @see Stream#noneMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean noneMatch(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.noneMatch(predicate);
    }

    /**
     * Checks if no elements of this EntryStream match the provided bi-predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Returns {@code true} if no elements match the predicate or if the stream is empty.
     * Unlike {@link #noneMatch(Throwables.Predicate)}, this method provides the key and value
     * as separate parameters to the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if no scores are failing (< 60)
     * boolean noFailures = EntryStream.of(studentScores)
     *     .noneMatch((student, score) -> score < 60);
     *
     * // Check if no key equals its value
     * boolean noEqualKeyValue = EntryStream.of(map)
     *     .noneMatch((key, value) -> key.equals(value));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return {@code true} if no elements match the predicate or this EntryStream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     * @see #noneMatch(Throwables.Predicate)
     * @see Stream#noneMatch(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean noneMatch(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.noneMatch(Fn.Entries.ep(predicate));
    }

    /**
     * Checks if the specified number of elements in this EntryStream match the provided predicate.
     * This is a short-circuiting terminal operation that stops as soon as the number of matching
     * elements exceeds {@code atMost}.
     *
     * <p>The operation returns {@code true} if and only if
     * {@code atLeast <= stream.filter(predicate).count() <= atMost}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if between 2 and 5 entries have values over 100
     * boolean inRange = EntryStream.of(scores)
     *     .hasMatchCountBetween(2, 5, entry -> entry.getValue() > 100);
     *
     * // Check if exactly 3 entries have keys starting with "test"
     * boolean exactlyThree = EntryStream.of(map)
     *     .hasMatchCountBetween(3, 3, entry -> entry.getKey().startsWith("test"));
     * }</pre>
     *
     * @param <E> the type of the exception that can be thrown when processing elements
     * @param atLeast the minimum number of elements that need to match the predicate
     * @param atMost the maximum number of elements that need to match the predicate
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return {@code true} if the number of elements matching the predicate is within the specified range (inclusive), {@code false} otherwise
     * @throws IllegalArgumentException if {@code atLeast} or {@code atMost} is negative, or if {@code atLeast > atMost}
     * @throws E if any element processing results in an exception
     * @see #hasMatchCountBetween(long, long, Throwables.BiPredicate)
     * @see Stream#hasMatchCountBetween(long, long, Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean hasMatchCountBetween(final long atLeast, final long atMost,
            final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.hasMatchCountBetween(atLeast, atMost, predicate);
    }

    /**
     * Checks if the specified number of elements in this EntryStream match the provided bi-predicate.
     * This is a short-circuiting terminal operation that stops as soon as the number of matching
     * elements exceeds {@code atMost}.
     *
     * <p>The operation returns {@code true} if and only if
     * {@code atLeast <= stream.filter(predicate).count() <= atMost}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if between 1 and 3 students have scores over 90
     * boolean inRange = EntryStream.of(studentScores)
     *     .hasMatchCountBetween(1, 3, (student, score) -> score > 90);
     *
     * // Check if at least 5 entries have matching key-value lengths
     * boolean atLeastFive = EntryStream.of(stringMap)
     *     .hasMatchCountBetween(5, Long.MAX_VALUE, (key, value) -> key.length() == value.length());
     * }</pre>
     *
     * @param <E> the type of the exception that can be thrown when processing elements
     * @param atLeast the minimum number of elements that need to match the predicate
     * @param atMost the maximum number of elements that need to match the predicate
     * @param predicate a non-interfering, stateless predicate to apply to each key-value pair
     * @return {@code true} if the number of elements matching the predicate is within the specified range (inclusive), {@code false} otherwise
     * @throws IllegalArgumentException if {@code atLeast} or {@code atMost} is negative, or if {@code atLeast > atMost}
     * @throws E if any element processing results in an exception
     * @see #hasMatchCountBetween(long, long, Throwables.Predicate)
     * @see Stream#hasMatchCountBetween(long, long, Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> boolean hasMatchCountBetween(final long atLeast, final long atMost,
            final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.hasMatchCountBetween(atLeast, atMost, Fn.Entries.ep(predicate));
    }

    /**
     * Returns the first entry in the stream, if present, otherwise returns an empty {@code Optional}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} (and is equivalent to {@link #findAny()}): it
     * always returns the first entry in encounter order, including for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Map.Entry<String, Integer>> found = EntryStream.of("a", 1, "b", 2, "c", 3)
     *                               .findFirst();   // returns Optional.of(entry("a", 1)) (always the first entry)
     * }</pre>
     *
     * @return an {@code Optional} containing the first entry of the stream, or an empty {@code Optional} if the stream is empty
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.Predicate)
     * @see #findAny(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> findFirst() {
        return first();
    }

    /**
     * Returns the first entry in the stream, if present, otherwise returns an empty {@code Optional}.
     * This is a terminal operation.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Map.Entry<String, Integer>> any = EntryStream.of("a", 1, "b", 2, "c", 3)
     *                               .findAny();   // returns Optional.of(entry("a", 1))
     * }</pre>
     *
     * @return an {@code Optional} containing the first entry of the stream, or an empty {@code Optional} if the stream is empty
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.Predicate)
     * @see #findAny(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> findAny() {
        return first();
    }

    /**
     * Finds the first entry in this EntryStream that matches the provided predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>The predicate operates on Map.Entry objects. For predicates on separate key-value pairs,
     * use {@link #findFirst(Throwables.BiPredicate)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first entry with value over 100
     * Optional<Map.Entry<String, Integer>> found = EntryStream.of(scores)
     *     .findFirst(entry -> entry.getValue() > 100);
     *
     * // Find first entry with key containing "admin"
     * Optional<Map.Entry<String, User>> admin = EntryStream.of(users)
     *     .findFirst(entry -> entry.getKey().contains("admin"));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code Optional} containing the first entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findFirst(Throwables.BiPredicate)
     * @see #findAny(Throwables.Predicate)
     * @see #findLast(Throwables.Predicate)
     * @see Stream#findFirst(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findFirst(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.findFirst(predicate);
    }

    /**
     * Finds the first entry in this EntryStream that matches the provided bi-predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>Unlike {@link #findFirst(Throwables.Predicate)}, this method provides the key and value
     * as separate parameters to the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first student with passing score
     * Optional<Map.Entry<String, Integer>> passing = EntryStream.of(studentScores)
     *     .findFirst((student, score) -> score >= 60);
     *
     * // Find first entry where key length equals value
     * Optional<Map.Entry<String, Integer>> found = EntryStream.of(map)
     *     .findFirst((key, value) -> key.length() == value);
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return an {@code Optional} containing the first entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findFirst(Throwables.Predicate)
     * @see Stream#findFirst(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findFirst(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.findFirst(Fn.Entries.ep(predicate));
    }

    /**
     * Finds any entry in this EntryStream that matches the provided predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>In sequential streams, this method behaves like {@link #findFirst(Throwables.Predicate)}.
     * In parallel streams, it may return any matching element, which can be more efficient.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any entry with null value
     * Optional<Map.Entry<String, Object>> nullEntry = EntryStream.of(map)
     *     .parallel()
     *     .findAny(entry -> entry.getValue() == null);
     *
     * // Find any large score
     * Optional<Map.Entry<String, Integer>> highScore = EntryStream.of(scores)
     *     .parallel()
     *     .findAny(entry -> entry.getValue() > 95);
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code Optional} containing any entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findAny(Throwables.BiPredicate)
     * @see #findFirst(Throwables.Predicate)
     * @see Stream#findAny(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findAny(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.findAny(predicate);
    }

    /**
     * Finds any entry in this EntryStream that matches the provided bi-predicate.
     * This is a short-circuiting terminal operation.
     *
     * <p>In sequential streams, this method behaves like {@link #findFirst(Throwables.BiPredicate)}.
     * In parallel streams, it may return any matching element, which can be more efficient.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any student with perfect score
     * Optional<Map.Entry<String, Integer>> perfect = EntryStream.of(studentScores)
     *     .parallel()
     *     .findAny((student, score) -> score == 100);
     *
     * // Find any entry with equal key and value
     * Optional<Map.Entry<String, String>> equal = EntryStream.of(stringMap)
     *     .parallel()
     *     .findAny((key, value) -> key.equals(value));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return an {@code Optional} containing any entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findAny(Throwables.Predicate)
     * @see Stream#findAny(Throwables.Predicate)
     */
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findAny(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.findAny(Fn.Entries.ep(predicate));
    }

    /**
     * Finds the last entry in this EntryStream that matches the provided predicate.
     * This is a terminal operation that may need to process the entire stream.
     *
     * <p>Note: This operation loads all matching elements into memory to find the last one.
     * Consider using {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last entry with value over 90
     * Optional<Map.Entry<String, Integer>> lastHigh = EntryStream.of(scores)
     *     .findLast(entry -> entry.getValue() > 90);
     *
     * // Find last entry with key starting with "z"
     * Optional<Map.Entry<String, Object>> lastZ = EntryStream.of(map)
     *     .findLast(entry -> entry.getKey().startsWith("z"));
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code Optional} containing the last entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findLast(Throwables.BiPredicate)
     * @see #findFirst(Throwables.Predicate)
     * @see Stream#findLast(Throwables.Predicate)
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findLast(final Throwables.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return _stream.findLast(predicate);
    }

    /**
     * Finds the last entry in this EntryStream that matches the provided bi-predicate.
     * This is a terminal operation that may need to process the entire stream.
     *
     * <p>Note: This operation loads all matching elements into memory to find the last one.
     * Consider using {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last student with failing score
     * Optional<Map.Entry<String, Integer>> lastFailing = EntryStream.of(studentScores)
     *     .findLast((student, score) -> score < 60);
     *
     * // Find last entry where value is longer than key
     * Optional<Map.Entry<String, String>> lastLonger = EntryStream.of(stringMap)
     *     .findLast((key, value) -> value.length() > key.length());
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate a non-interfering, stateless bi-predicate to apply to key-value pairs of this stream
     * @return an {@code Optional} containing the last entry that matches the predicate, or an empty {@code Optional} if no match is found
     * @throws E if the predicate throws an exception
     * @see #findLast(Throwables.Predicate)
     * @see Stream#findLast(Throwables.Predicate)
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public <E extends Exception> Optional<Map.Entry<K, V>> findLast(final Throwables.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return _stream.findLast(Fn.Entries.ep(predicate));
    }

    /**
     * Returns an Optional containing the first entry of this EntryStream.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the first entry from a map
     * Optional<Map.Entry<String, Integer>> first = EntryStream.of(map).first();
     *
     * // Get the first entry after filtering
     * Optional<Map.Entry<String, Integer>> firstHigh = EntryStream.of(scores)
     *     .filter((k, v) -> v > 80)
     *     .first();
     * }</pre>
     *
     * @return an {@code Optional} containing the first entry, or an empty {@code Optional} if the stream is empty
     * @see #last()
     * @see Stream#first()
     */
    @Override
    public Optional<Map.Entry<K, V>> first() {
        return _stream.first();
    }

    /**
     * Returns an Optional containing the last entry of this EntryStream.
     * This is a terminal operation that may need to process the entire stream.
     *
     * <p>Note: This operation loads all elements into memory to find the last one.
     * Consider restructuring your stream operations if this becomes a performance issue.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the last entry from a map
     * Optional<Map.Entry<String, Integer>> last = EntryStream.of(map).last();
     *
     * // Get the last entry after sorting
     * Optional<Map.Entry<String, Integer>> highest = EntryStream.of(scores)
     *     .sortedByValue(Comparator.naturalOrder())
     *     .last();
     * }</pre>
     *
     * @return an {@code Optional} containing the last entry, or an empty {@code Optional} if the stream is empty
     * @see #first()
     * @see Stream#last()
     */
    @Override
    public Optional<Map.Entry<K, V>> last() {
        return _stream.last();
    }

    /**
     * Returns the entry at the specified position in this stream.
     * This is a terminal operation.
     *
     * <p>The position is zero-based, meaning the first entry in the stream has a position of 0,
     * the second has a position of 1, and so on.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the third entry (index 2)
     * Optional<Map.Entry<String, Integer>> third = EntryStream.of(map)
     *     .elementAt(2);
     *
     * // Get the 10th highest score
     * Optional<Map.Entry<String, Integer>> tenth = EntryStream.of(scores)
     *     .sortedByValue(Comparator.reverseOrder())
     *     .elementAt(9);
     * }</pre>
     *
     * @param position the position of the entry to return (zero-based)
     * @return an {@code Optional} containing the entry at the specified position if it exists, otherwise an empty {@code Optional}
     * @see Stream#elementAt(long)
     */
    @Override
    public Optional<Map.Entry<K, V>> elementAt(final long position) {
        return _stream.elementAt(position);
    }

    /**
     * Returns an Optional containing the only entry of this EntryStream if it contains exactly one entry.
     * This is a terminal operation.
     *
     * <p>If the stream is empty, an empty {@code Optional} is returned. If the stream contains more than one entry,
     * a {@link TooManyElementsException} is thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the only entry from a single-entry map
     * Optional<Map.Entry<String, Integer>> only = EntryStream.of(singletonMap).onlyOne();
     *
     * // Get the only matching entry
     * Optional<Map.Entry<String, Integer>> onlyHigh = EntryStream.of(scores)
     *     .filter((k, v) -> v == 100)
     *     .onlyOne();   // throws if multiple entries have value 100
     * }</pre>
     *
     * @return an {@code Optional} containing the only entry of this EntryStream if it is not empty, otherwise an empty {@code Optional}
     * @throws TooManyElementsException if the EntryStream contains more than one entry
     * @see Stream#onlyOne()
     */
    @Override
    public Optional<Map.Entry<K, V>> onlyOne() throws TooManyElementsException {
        return _stream.onlyOne();
    }

    /**
     * This method always throws {@link UnsupportedOperationException} because computing
     * percentiles on {@code Map.Entry} objects without an explicit comparator is not
     * well-defined. Use {@link #percentiles(Comparator)} with an appropriate comparator instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores = N.newLinkedHashMap();
     * scores.put("Alice", 95); scores.put("Bob", 87);
     * // EntryStream.of(scores).percentiles(); // throws UnsupportedOperationException
     *
     * // Use percentiles(Comparator) instead:
     * Optional<Map<Percentage, Map.Entry<String, Integer>>> result =
     *     EntryStream.of(scores).percentiles(Map.Entry.comparingByValue()); // returns Optional with percentile map
     *
     * EntryStream.empty().percentiles(); // throws UnsupportedOperationException
     * }</pre>
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #percentiles(Comparator)} instead.
     * @see #percentiles(Comparator)
     */
    @Deprecated
    @Override
    public Optional<Map<Percentage, Map.Entry<K, V>>> percentiles() throws UnsupportedOperationException {
        //    final Comparator<Map.Entry<Comparable, Comparable>> cmp = Comparators.<Comparable, Comparable> comparingByKey()
        //            .thenComparing(Comparators.comparingByValue());
        //
        //    return percentiles((Comparator) cmp);

        throw new UnsupportedOperationException("Use percentiles(Comparator) instead.");
    }

    /**
     * Calculates the percentiles of the elements in the stream according to the provided comparator.
     * All elements will be loaded into memory and sorted if not yet.
     * The returned map contains the percentile values as keys and the corresponding elements as values.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get percentiles of entries sorted by value
     * Optional<Map<Percentage, Map.Entry<String, Integer>>> percentiles =
     *     EntryStream.of(scores)
     *         .percentiles(Map.Entry.comparingByValue());
     *
     * // Get percentiles of entries sorted by key length
     * Optional<Map<Percentage, Map.Entry<String, Object>>> keyLengthPercentiles =
     *     EntryStream.of(map)
     *         .percentiles((e1, e2) -> Integer.compare(e1.getKey().length(), e2.getKey().length()));
     * }</pre>
     *
     * @param comparator the comparator to determine the order of the entries
     * @return an {@code Optional} containing a map of percentiles to entries, or an empty {@code Optional} if the stream is empty
     * @see N#percentilesOfSorted(int[])
     * @see Comparators#comparingByKey()
     * @see Comparators#comparingByKey(Comparator)
     * @see Comparators#comparingByValue()
     * @see Comparators#comparingByValue(Comparator)
     */
    @SequentialOnly
    @TerminalOp
    public Optional<Map<Percentage, Map.Entry<K, V>>> percentiles(final Comparator<? super Map.Entry<K, V>> comparator) {
        return _stream.percentiles(comparator);
    }

    /**
     * Returns the count of entries in this EntryStream.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count total entries
     * long total = EntryStream.of(map).count();
     *
     * // Count entries matching criteria
     * long highScores = EntryStream.of(scores)
     *     .filter((student, score) -> score > 90)
     *     .count();
     * }</pre>
     *
     * @return the number of entries in this EntryStream
     * @see Stream#count()
     */
    @Override
    public long count() {
        return _stream.count();
    }

    /**
     * Returns an iterator over the entries in this EntryStream.
     *
     * <p><b>Warning:</b> This method should be used with caution. The stream must be properly closed
     * after iteration to avoid resource leaks. Consider using terminal operations like
     * {@link #forEach(Throwables.Consumer)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Manual iteration (remember to close!)
     * try (EntryStream<String, Integer> stream = EntryStream.of(map)) {
     *     ObjIterator<Map.Entry<String, Integer>> iter = stream.iterator();
     *     while (iter.hasNext()) {
     *         Map.Entry<String, Integer> entry = iter.next();
     *         // Process entry
     *     }
     * }
     * }</pre>
     *
     * @return an iterator over the entries in this EntryStream
     * @see #biIterator()
     * @see Stream#iterator()
     */
    @Override
    public ObjIterator<Map.Entry<K, V>> iterator() {
        return _stream.iteratorEx();
    }

    /**
     * Returns a BiIterator over the key-value pairs in this EntryStream.
     * This provides a convenient way to iterate over keys and values separately.
     *
     * <p><b>Warning:</b> This method should be used with caution. The stream must be properly closed
     * after iteration to avoid resource leaks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate over key-value pairs using forEachRemaining
     * try (EntryStream<String, Integer> stream = EntryStream.of(map)) {
     *     BiIterator<String, Integer> iter = stream.biIterator();
     *     iter.forEachRemaining((key, value) -> {
     *         System.out.println(key + " = " + value);
     *     });
     * }
     *
     * // Or iterate using hasNext/next with Pair
     * try (EntryStream<String, Integer> stream = EntryStream.of(map)) {
     *     BiIterator<String, Integer> iter = stream.biIterator();
     *     while (iter.hasNext()) {
     *         Pair<String, Integer> pair = iter.next();
     *         // Process pair.left() and pair.right()
     *     }
     * }
     * }</pre>
     *
     * @return a BiIterator over the key-value pairs in this EntryStream
     * @see #iterator()
     */
    @SequentialOnly
    public BiIterator<K, V> biIterator() {
        final ObjIterator<Map.Entry<K, V>> iter = _stream.iteratorEx();

        final BooleanSupplier hasNext = iter::hasNext;

        final Consumer<Pair<K, V>> output = t -> {
            final Map.Entry<K, V> entry = iter.next();
            t.set(entry.getKey(), entry.getValue());
        };

        return BiIterator.generate(hasNext, output);
    }

    @Override
    protected Object[] toArray(final boolean closeStream) {
        return _stream.toArray(closeStream);
    }

    /**
     * Returns a list containing all entries of this stream.
     * This is a terminal operation.
     *
     * <p>The returned list is mutable and contains {@code Map.Entry} objects.
     * Structural modifications to the list do not affect the original map. If this stream was
     * created from a map entry view, the entries themselves may remain live views of that map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map.Entry<String, Integer>> entries = EntryStream.of(map).toList();
     *
     * // Get sorted list of entries
     * List<Map.Entry<String, Integer>> sorted = EntryStream.of(scores)
     *     .sortedByValue(Comparator.reverseOrder())
     *     .toList();
     * }</pre>
     *
     * @return a List containing the entries of this stream
     * @see #toSet()
     * @see #toMap()
     * @see Stream#toList()
     */
    @Override
    public List<Map.Entry<K, V>> toList() {
        return _stream.toList();
    }

    /**
     * Returns a set containing all unique entries of this stream.
     * This is a terminal operation.
     *
     * <p>The returned set is mutable. Duplicate entries (based on equals/hashCode) are removed.
     * Note that the uniqueness is based on the entire Map.Entry, not just the key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Map.Entry<String, Integer>> uniqueEntries = EntryStream.of(map).toSet();
     *
     * // Get unique high-scoring entries
     * Set<Map.Entry<String, Integer>> highScorers = EntryStream.of(scores)
     *     .filter((student, score) -> score > 90)
     *     .toSet();
     * }</pre>
     *
     * @return a Set containing the unique entries of this stream
     * @see #toList()
     * @see Stream#toSet()
     */
    @Override
    public Set<Map.Entry<K, V>> toSet() {
        return _stream.toSet();
    }

    /**
     * Returns a collection containing all entries of this stream.
     * This is a terminal operation.
     *
     * <p>The type of collection is determined by the provided supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to LinkedHashSet to preserve order
     * LinkedHashSet<Map.Entry<String, Integer>> orderedSet =
     *     EntryStream.of(map).toCollection(LinkedHashSet::new);
     *
     * // Collect to TreeSet with custom comparator
     * TreeSet<Map.Entry<String, Integer>> sorted =
     *     EntryStream.of(scores).toCollection(
     *         () -> new TreeSet<>(Map.Entry.comparingByValue())
     *     );
     * }</pre>
     *
     * @param <C> the type of the collection
     * @param supplier the supplier providing the collection
     * @return a collection containing the entries of this stream
     * @see Stream#toCollection(Supplier)
     */
    @Override
    public <C extends Collection<Map.Entry<K, V>>> C toCollection(final Supplier<? extends C> supplier) {
        return _stream.toCollection(supplier);
    }

    /**
     * Returns a {@code Multiset} containing all the {@code Map.Entry} elements from this stream.
     * A multiset is a collection that allows duplicate elements and keeps track of their frequencies.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream with duplicate entries
     * Multiset<Map.Entry<String, Integer>> multiset = EntryStream.of("a", 1, "b", 2, "a", 1)
     *     .toMultiset();
     * // The multiset contains entries with their occurrence counts
     * }</pre>
     *
     * @return a {@code Multiset} containing all entries from this stream
     * @see Stream#toMultiset()
     * @see #toMultiset(Supplier)
     */
    @Override
    public Multiset<Map.Entry<K, V>> toMultiset() {
        return _stream.toMultiset();
    }

    /**
     * Returns a {@code Multiset} containing all the {@code Map.Entry} elements from this stream,
     * using the provided supplier to create the multiset.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Supplier<Multiset<Map.Entry<String, Integer>>> supplier = () -> new Multiset<>();
     * Multiset<Map.Entry<String, Integer>> multiset = EntryStream.of(map)
     *     .toMultiset(supplier);
     * }</pre>
     *
     * @param supplier a function which returns a new, empty {@code Multiset} into which the
     *                 results will be inserted
     * @return a {@code Multiset} containing all entries from this stream
     * @see Stream#toMultiset(Supplier)
     * @see #toMultiset()
     */
    @Override
    public Multiset<Map.Entry<K, V>> toMultiset(final Supplier<? extends Multiset<Map.Entry<K, V>>> supplier) {
        return _stream.toMultiset(supplier);
    }

    /**
     * Returns a {@code Map} containing the key-value pairs from this stream.
     * If duplicate keys are encountered, an {@code IllegalStateException} is thrown.
     * Use {@link #toMap(BinaryOperator)} with a merge function to handle duplicates.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toMap();
     * // Result: {"a"=1, "b"=2, "c"=3}
     * }</pre>
     *
     * @return a {@code Map} containing all key-value pairs from this stream
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Stream#toMap(Throwables.Function, Throwables.Function)
     * @see #toMap(BinaryOperator)
     * @see #toMap(Supplier)
     * @see #toMap(BinaryOperator, Supplier)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public Map<K, V> toMap() throws IllegalStateException {
        if (isParallel()) {
            return _stream.sequential().toMap(Fn.key(), Fn.value());
        } else {
            return _stream.toMap(Fn.key(), Fn.value());
        }
    }

    /**
     * Returns a {@code Map} containing the key-value pairs from this stream, using the provided
     * merge function to resolve collisions between values associated with the same key.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum values for duplicate keys
     * Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3)
     *     .toMap(Integer::sum);
     * // Result: {"a"=4, "b"=2}
     *
     * // Keep the last value for duplicate keys
     * Map<String, String> result2 = EntryStream.of("a", "first", "a", "second")
     *     .toMap((v1, v2) -> v2);
     * // Result: {"a"="second"}
     * }</pre>
     *
     * @param mergeFunction a function used to resolve collisions between values associated
     *                      with the same key, as supplied to {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code Map} containing all key-value pairs from this stream
     * @see Stream#toMap(Throwables.Function, Throwables.Function, BinaryOperator)
     * @see #toMap()
     * @see #toMap(Supplier)
     * @see #toMap(BinaryOperator, Supplier)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public Map<K, V> toMap(final BinaryOperator<V> mergeFunction) {
        if (isParallel()) {
            return _stream.sequential().toMap(Fn.key(), Fn.value(), mergeFunction);
        } else {
            return _stream.toMap(Fn.key(), Fn.value(), mergeFunction);
        }
    }

    /**
     * Returns a {@code Map} containing the key-value pairs from this stream, using the provided
     * supplier to create the map. If duplicate keys are encountered, an {@code IllegalStateException} is thrown.
     * Use {@link #toMap(BinaryOperator, Supplier)} with a merge function to handle duplicates.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMap to maintain sorted order
     * Map<String, Integer> result = EntryStream.of("b", 2, "a", 1, "c", 3)
     *     .toMap(TreeMap::new);
     * // Result: TreeMap with entries in sorted order: {"a"=1, "b"=2, "c"=3}
     *
     * // Create a LinkedHashMap to maintain insertion order
     * Map<String, Integer> result2 = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toMap(LinkedHashMap::new);
     * }</pre>
     *
     * @param <M> the type of the resulting {@code Map}
     * @param mapFactory a function which returns a new, empty {@code Map} into which the
     *                   results will be inserted
     * @return a {@code Map} containing all key-value pairs from this stream
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Stream#toMap(Throwables.Function, Throwables.Function, Supplier)
     * @see #toMap()
     * @see #toMap(BinaryOperator)
     * @see #toMap(BinaryOperator, Supplier)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public <M extends Map<K, V>> M toMap(final Supplier<? extends M> mapFactory) throws IllegalStateException {
        if (isParallel()) {
            return _stream.sequential().toMap(Fn.key(), Fn.value(), mapFactory);
        } else {
            return _stream.toMap(Fn.key(), Fn.value(), mapFactory);
        }
    }

    /**
     * Returns a {@code Map} containing the key-value pairs from this stream, using the provided
     * merge function to resolve collisions and the provided supplier to create the map.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMap with custom merge logic
     * Map<String, Integer> result = EntryStream.of("b", 2, "a", 1, "b", 3, "c", 4)
     *     .toMap(Integer::sum, TreeMap::new);
     * // Result: TreeMap {"a"=1, "b"=5, "c"=4}
     *
     * // Create a ConcurrentHashMap keeping the maximum value for duplicates
     * Map<String, Integer> result2 = EntryStream.of("a", 5, "b", 3, "a", 2)
     *     .toMap(Integer::max, ConcurrentHashMap::new);
     * // Result: ConcurrentHashMap {"a"=5, "b"=3}
     * }</pre>
     *
     * @param <M> the type of the resulting {@code Map}
     * @param mergeFunction a function used to resolve collisions between values associated
     *                      with the same key
     * @param mapFactory a function which returns a new, empty {@code Map} into which the
     *                   results will be inserted
     * @return a {@code Map} containing all key-value pairs from this stream
     * @see Stream#toMap(Throwables.Function, Throwables.Function, BinaryOperator, Supplier)
     * @see #toMap()
     * @see #toMap(BinaryOperator)
     * @see #toMap(Supplier)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public <M extends Map<K, V>> M toMap(final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return _stream.sequential().toMap(Fn.key(), Fn.value(), mergeFunction, mapFactory);
        } else {
            return _stream.toMap(Fn.key(), Fn.value(), mergeFunction, mapFactory);
        }
    }

    /**
     * Collects the entries into a {@code Map} and then applies the provided function to that map.
     * This is useful for performing operations on the collected map in a single pipeline step.
     * If duplicate keys are encountered during collection, an {@code IllegalStateException} is thrown.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the size of the resulting map
     * int size = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toMapThenApply(Map::size);
     * // Result: 3
     *
     * // Get a specific value from the map
     * Integer value = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toMapThenApply(map -> map.get("b"));
     * // Result: 2
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown by the function
     * @param func the function to apply to the resulting map
     * @return the result of applying the function to the collected map
     * @throws IllegalStateException if duplicate keys are encountered during map collection
     * @throws E if the function throws an exception
     * @see #toMap()
     * @see #toMapThenAccept(Throwables.Consumer)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public <R, E extends Exception> R toMapThenApply(final Throwables.Function<? super Map<K, V>, ? extends R, E> func) throws IllegalStateException, E {
        return func.apply(toMap());
    }

    /**
     * Collects the entries into a {@code Map} and then passes that map to the provided consumer.
     * This is useful for performing side effects on the collected map in a single pipeline step.
     * If duplicate keys are encountered during collection, an {@code IllegalStateException} is thrown.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Print the resulting map
     * EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toMapThenAccept(System.out::println);
     * // Prints: {a=1, b=2, c=3}
     *
     * // Store the map in an external variable
     * Map<String, Integer> external = new HashMap<>();
     * EntryStream.of("a", 1, "b", 2)
     *     .toMapThenAccept(external::putAll);
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the consumer
     * @param consumer the consumer to accept the resulting map
     * @throws IllegalStateException if duplicate keys are encountered during map collection
     * @throws E if the consumer throws an exception
     * @see #toMap()
     * @see #toMapThenApply(Throwables.Function)
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public <E extends Exception> void toMapThenAccept(final Throwables.Consumer<? super Map<K, V>, E> consumer) throws IllegalStateException, E {
        consumer.accept(toMap());
    }

    /**
     * Returns an {@code ImmutableMap} containing the key-value pairs from this stream.
     * The returned map is immutable and cannot be modified after creation.
     * If duplicate keys are encountered, an {@code IllegalStateException} is thrown.
     * Use {@link #toImmutableMap(BinaryOperator)} with a merge function to handle duplicates.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .toImmutableMap();
     * // Result: ImmutableMap {"a"=1, "b"=2, "c"=3}
     *
     * // Attempting to modify throws UnsupportedOperationException
     * // result.put("d", 4);   // Throws exception
     * }</pre>
     *
     * @return an {@code ImmutableMap} containing all key-value pairs from this stream
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Stream#toImmutableMap(Throwables.Function, Throwables.Function)
     * @see #toImmutableMap(BinaryOperator)
     * @see #toMap()
     * @see ImmutableMap
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public ImmutableMap<K, V> toImmutableMap() throws IllegalStateException {
        return ImmutableMap.wrap(toMap());
    }

    /**
     * Returns an {@code ImmutableMap} containing the key-value pairs from this stream,
     * using the provided merge function to resolve collisions between values associated
     * with the same key.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create immutable map with sum merge for duplicates
     * ImmutableMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3)
     *     .toImmutableMap(Integer::sum);
     * // Result: ImmutableMap {"a"=4, "b"=2}
     *
     * // Create immutable map keeping first value for duplicates
     * ImmutableMap<String, String> result2 = EntryStream.of("a", "first", "a", "second")
     *     .toImmutableMap((v1, v2) -> v1);
     * // Result: ImmutableMap {"a"="first"}
     * }</pre>
     *
     * @param mergeFunction a function used to resolve collisions between values associated
     *                      with the same key
     * @return an {@code ImmutableMap} containing all key-value pairs from this stream
     * @see Stream#toImmutableMap(Throwables.Function, Throwables.Function, BinaryOperator)
     * @see #toImmutableMap()
     * @see #toMap(BinaryOperator)
     * @see ImmutableMap
     * @see Fn#throwingMerger()
     * @see Fn#replacingMerger()
     * @see Fn#ignoringMerger()
     */
    @SequentialOnly
    @TerminalOp
    public ImmutableMap<K, V> toImmutableMap(final BinaryOperator<V> mergeFunction) {
        return ImmutableMap.wrap(toMap(mergeFunction));
    }

    /**
     * Returns a {@code ListMultimap} containing the key-value pairs from this stream.
     * A multimap is like a map but allows multiple values per key, stored in lists.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create multimap from entries with duplicate keys
     * ListMultimap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3)
     *     .toMultimap();
     * // Result: {"a"=[1, 3], "b"=[2]}
     *
     * // All values for a key are stored in a list
     * List<Integer> aValues = result.get("a");   // returns [1, 3]
     * }</pre>
     *
     * <p><b>Note:</b> If this stream is parallel, this operation internally switches to sequential
     * processing, so any upstream parallelism is silently lost for this terminal step.
     *
     * @return a {@code ListMultimap} containing all key-value pairs from this stream
     * @see Stream#toMultimap(Throwables.Function, Throwables.Function)
     * @see #toMultimap(Supplier)
     * @see #groupTo()
     * @see ListMultimap
     */
    @SequentialOnly
    @TerminalOp
    public ListMultimap<K, V> toMultimap() {
        if (isParallel()) {
            return _stream.sequential().toMultimap(Fn.key(), Fn.value());
        } else {
            return _stream.toMultimap(Fn.key(), Fn.value());
        }
    }

    /**
     * Returns a {@code Multimap} containing the key-value pairs from this stream,
     * using the provided supplier to create the multimap.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a SetMultimap to avoid duplicate values per key
     * SetMultimap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 1, "a", 3)
     *     .toMultimap(N::newSetMultimap);
     * // Result: {"a"={1, 3}, "b"={2}} (duplicate "a"=1 is removed)
     *
     * // Create a custom multimap implementation
     * Multimap<String, Integer, TreeSet<Integer>> sorted =
     *     EntryStream.of("a", 3, "a", 1, "a", 2)
     *     .toMultimap(() -> N.newMultimap(TreeMap::new, TreeSet::new));
     * // Result: {"a"={1, 2, 3}} with sorted keys and values
     * }</pre>
     *
     * <p><b>Note:</b> If this stream is parallel, this operation internally switches to sequential
     * processing, so any upstream parallelism is silently lost for this terminal step.
     *
     * @param <C> the type of collection used to store values in the multimap
     * @param <M> the type of the resulting multimap
     * @param mapFactory a function which returns a new, empty {@code Multimap} into which
     *                   the results will be inserted
     * @return a {@code Multimap} containing all key-value pairs from this stream
     * @see Stream#toMultimap(Throwables.Function, Throwables.Function, Supplier)
     * @see #toMultimap()
     * @see #groupTo(Supplier)
     * @see Multimap
     */
    @SequentialOnly
    @TerminalOp
    public <C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return _stream.sequential().toMultimap(Fn.key(), Fn.value(), mapFactory);
        } else {
            return _stream.toMultimap(Fn.key(), Fn.value(), mapFactory);
        }
    }

    /**
     * Groups the values by their keys into a {@code Map} where each key is associated
     * with a {@code List} of values. This is similar to {@link #toMultimap()} but returns
     * a standard {@code Map<K, List<V>>} instead of a {@code Multimap}.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group values by key
     * Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4)
     *     .groupTo();
     * // Result: {"a"=[1, 3], "b"=[2, 4]}
     *
     * // Group and process employee data
     * List<Employee> employees = getEmployees();
     * Map<String, List<Employee>> byDept = EntryStream.of(employees, Employee::getDepartment)
     *     .groupTo();
     * }</pre>
     *
     * <p><b>Note:</b> If this stream is parallel, this operation internally switches to sequential
     * processing, so any upstream parallelism is silently lost for this terminal step.
     *
     * @return a {@code Map} where each key maps to a {@code List} of all values associated
     *         with that key in this stream
     * @see Stream#groupTo(Throwables.Function, Throwables.Function)
     * @see #groupTo(Supplier)
     * @see #toMultimap()
     * @see java.util.stream.Collectors#groupingBy(Function)
     */
    @SequentialOnly
    @TerminalOp
    public Map<K, List<V>> groupTo() {
        if (isParallel()) {
            return _stream.sequential().groupTo(Fn.key(), Fn.value());
        } else {
            return _stream.groupTo(Fn.key(), Fn.value());
        }
    }

    /**
     * Groups the values by their keys into a {@code Map} where each key is associated
     * with a {@code List} of values, using the provided supplier to create the map.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group into a TreeMap for sorted keys
     * Map<String, List<Integer>> result = EntryStream.of("b", 2, "a", 1, "b", 4, "a", 3)
     *     .groupTo(TreeMap::new);
     * // Result: TreeMap {"a"=[1, 3], "b"=[2, 4]}
     *
     * // Group into a LinkedHashMap to preserve insertion order
     * Map<String, List<String>> result2 = EntryStream.of("x", "X1", "y", "Y1", "x", "X2")
     *     .groupTo(LinkedHashMap::new);
     * // Result maintains order of first occurrence of each key
     * }</pre>
     *
     * <p><b>Note:</b> If this stream is parallel, this operation internally switches to sequential
     * processing, so any upstream parallelism is silently lost for this terminal step.
     *
     * @param <M> the type of the resulting {@code Map}
     * @param mapFactory a function which returns a new, empty {@code Map} into which the
     *                   grouped results will be inserted
     * @return a {@code Map} where each key maps to a {@code List} of all values associated
     *         with that key in this stream
     * @see Stream#groupTo(Throwables.Function, Throwables.Function, Supplier)
     * @see #groupTo()
     * @see #toMultimap(Supplier)
     * @see java.util.stream.Collectors#groupingBy(Function, Supplier, Collector)
     */
    @SequentialOnly
    @TerminalOp
    public <M extends Map<K, List<V>>> M groupTo(final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return _stream.sequential().groupTo(Fn.key(), Fn.value(), mapFactory);
        } else {
            return _stream.groupTo(Fn.key(), Fn.value(), mapFactory);
        }
    }

    /**
     * Groups the values by their keys into a {@code Map} and then applies the provided
     * function to that map. This is useful for performing operations on the grouped
     * data in a single pipeline step.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count the number of unique keys after grouping
     * int uniqueKeys = EntryStream.of("a", 1, "b", 2, "a", 3)
     *     .groupToThenApply(Map::size);
     * // Result: 2
     *
     * // Get the maximum list size after grouping
     * int maxGroupSize = EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4)
     *     .groupToThenApply(map -> map.values().stream()
     *         .mapToInt(List::size)
     *         .max()
     *         .orElse(0));
     * // Result: 3 (for key "a" which has 3 values)
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown by the function
     * @param func the function to apply to the grouped map
     * @return the result of applying the function to the grouped map
     * @throws E if the function throws an exception
     * @see #groupTo()
     * @see #groupToThenAccept(Throwables.Consumer)
     */
    @SequentialOnly
    @TerminalOp
    public <R, E extends Exception> R groupToThenApply(final Throwables.Function<? super Map<K, List<V>>, ? extends R, E> func) throws E {
        return func.apply(groupTo());
    }

    /**
     * Groups the values by their keys into a {@code Map} and then passes that map
     * to the provided consumer. This is useful for performing side effects on the
     * grouped data in a single pipeline step.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Print grouped data
     * EntryStream.of("a", 1, "b", 2, "a", 3)
     *     .groupToThenAccept(System.out::println);
     * // Prints: {a=[1, 3], b=[2]}
     *
     * // Process each group
     * EntryStream.of("dept1", emp1, "dept2", emp2, "dept1", emp3)
     *     .groupToThenAccept(map -> {
     *         map.forEach((dept, employees) ->
     *             System.out.println(dept + " has " + employees.size() + " employees"));
     *     });
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the consumer
     * @param consumer the consumer to accept the grouped map
     * @throws E if the consumer throws an exception
     * @see #groupTo()
     * @see #groupToThenApply(Throwables.Function)
     */
    @SequentialOnly
    @TerminalOp
    public <E extends Exception> void groupToThenAccept(final Throwables.Consumer<? super Map<K, List<V>>, E> consumer) throws E {
        consumer.accept(groupTo());
    }

    /**
     * Performs a reduction on the entries of this stream, using the provided identity value
     * and accumulation function, and returns the reduced value.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the entry with maximum value
     * Map.Entry<String, Integer> maxEntry = EntryStream.of("a", 1, "b", 3, "c", 2)
     *     .reduce(new SimpleEntry<>("", Integer.MIN_VALUE),
     *             (e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2);
     * // Result: Entry("b", 3)
     *
     * // Concatenate all entries into a single entry
     * Map.Entry<String, String> combined = EntryStream.of("a", "1", "b", "2", "c", "3")
     *     .reduce(new SimpleEntry<>("", ""),
     *             (acc, e) -> new SimpleEntry<>(acc.getKey() + e.getKey(),
     *                                          acc.getValue() + e.getValue()));
     * // Result: Entry("abc", "123")
     * }</pre>
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @see Stream#reduce(Object, BinaryOperator)
     * @see #reduce(BinaryOperator)
     */
    @SequentialOnly
    @TerminalOp
    public Map.Entry<K, V> reduce(final Map.Entry<K, V> identity, final BinaryOperator<Map.Entry<K, V>> accumulator) {
        return _stream.reduce(identity, accumulator);
    }

    /**
     * Performs a reduction on the entries of this stream, using an associative accumulation
     * function, and returns an {@code Optional} describing the reduced value, if any.
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the entry with minimum key (lexicographically)
     * Optional<Map.Entry<String, Integer>> minEntry =
     *     EntryStream.of("c", 3, "a", 1, "b", 2)
     *         .reduce((e1, e2) -> e1.getKey().compareTo(e2.getKey()) < 0 ? e1 : e2);
     * // Result: Optional[Entry("a", 1)]
     *
     * // Combine all entries by summing values with same keys
     * Optional<Map.Entry<String, Integer>> summed =
     *     EntryStream.of("total", 10, "total", 20, "total", 30)
     *         .reduce((e1, e2) -> new SimpleEntry<>(e1.getKey(),
     *                                               e1.getValue() + e2.getValue()));
     * // Result: Optional[Entry("total", 60)]
     * }</pre>
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code Optional} describing the result of the reduction, or empty if the stream is empty
     * @see Stream#reduce(BinaryOperator)
     * @see #reduce(Map.Entry, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public Optional<Map.Entry<K, V>> reduce(final BinaryOperator<Map.Entry<K, V>> accumulator) {
        return _stream.reduce(accumulator);
    }

    /**
     * Performs a mutable reduction operation on the entries of this stream using a
     * {@code Collector}. This is equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (Map.Entry<K,V> entry : this stream)
     *         accumulator.accept(result, entry);
     *     return result;
     * }</pre>
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect entries into a list
     * List<Map.Entry<String, Integer>> list = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collect(ArrayList::new, List::add, List::addAll);
     *
     * // Build a custom string representation
     * String result = EntryStream.of("x", 10, "y", 20)
     *     .collect(StringBuilder::new,
     *              (sb, e) -> sb.append(e.getKey()).append("=").append(e.getValue()).append("; "),
     *              StringBuilder::append)
     *     .toString();
     * // Result: "x=10; y=20; "
     * }</pre>
     *
     * @param <R> the type of the result
     * @param supplier a function that creates a new result container
     * @param accumulator an associative, non-interfering, stateless function for incorporating
     *                    an additional element into a result
     * @param combiner an associative, non-interfering, stateless function for combining two
     *                 values, which must be compatible with the accumulator function
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     * @see #collect(Supplier, BiConsumer)
     * @see #collect(Collector)
     */
    @ParallelSupported
    @TerminalOp
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super Map.Entry<K, V>> accumulator, final BiConsumer<R, R> combiner) {
        return _stream.collect(supplier, accumulator, combiner);
    }

    /**
     * Performs a mutable reduction operation on the entries of this stream. This is a
     * simplified form of {@link #collect(Supplier, BiConsumer, BiConsumer)} for cases
     * where the result container type naturally handles combining (such as
     * {@code List}, {@code Set}, {@code Map}, etc.).
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p>Only call this method when the returned type {@code R} is a mutable container type like:
     * {@code Collection}, {@code Map}, {@code StringBuilder}, {@code Multiset}, {@code Multimap},
     * or primitive list types. Otherwise, use {@link #collect(Supplier, BiConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect entries into a list
     * List<Map.Entry<String, Integer>> list = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collect(ArrayList::new, List::add);
     *
     * // Collect into a custom map (different from toMap)
     * Map<String, Integer> map = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()));
     *
     * // Build a string representation
     * StringBuilder sb = EntryStream.of("x", 10, "y", 20)
     *     .collect(StringBuilder::new,
     *              (builder, e) -> builder.append(e.getKey()).append(":").append(e.getValue()).append(","));
     * // Result: "x:10,y:20,"
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator an associative, non-interfering, stateless function that adds an
     *                    entry into the result container
     * @return the result container
     * @see #collect(Supplier, BiConsumer, BiConsumer)
     * @see #collect(Collector)
     * @see Stream#collect(Supplier, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super Map.Entry<K, V>> accumulator) {
        return _stream.collect(supplier, accumulator);
    }

    /**
     * Performs a mutable reduction operation on the entries of this stream using a
     * {@code Collector}. A {@code Collector} encapsulates the functions used as arguments
     * to {@link #collect(Supplier, BiConsumer, BiConsumer)}, allowing for reuse of
     * collection strategies and composition of collect operations.
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a map (similar to toMap but with more control)
     * Map<String, Integer> map = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
     *
     * // Group entries by value ranges
     * Map<String, List<Map.Entry<String, Integer>>> grouped =
     *     EntryStream.of("a", 1, "b", 5, "c", 2, "d", 8)
     *         .collect(Collectors.groupingBy(e -> e.getValue() < 5 ? "small" : "large"));
     *
     * // Partition entries by a condition
     * Map<Boolean, List<Map.Entry<String, Integer>>> partitioned =
     *     EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4)
     *         .collect(Collectors.partitioningBy(e -> e.getValue() % 2 == 0));
     * }</pre>
     *
     * @param <R> the type of the result
     * @param collector the {@code Collector} describing the reduction
     * @return the result of the reduction
     * @see Stream#collect(Collector)
     * @see #collect(Supplier, BiConsumer, BiConsumer)
     * @see java.util.stream.Collectors
     */
    @ParallelSupported
    @TerminalOp
    public <R> R collect(final Collector<? super Map.Entry<K, V>, ?, R> collector) {
        return _stream.collect(collector);
    }

    /**
     * Performs a mutable reduction operation using a {@code Collector} and then applies
     * the provided function to the result. This allows for transforming the collected
     * result in a single pipeline step.
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to map and get its size
     * int mapSize = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collectThenApply(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
     *                       Map::size);
     * // Result: 3
     *
     * // Collect entries and format as string
     * String formatted = EntryStream.of("x", 10, "y", 20)
     *     .collectThenApply(Collectors.toList(),
     *                       list -> "Total entries: " + list.size());
     * // Result: "Total entries: 2"
     *
     * // Group by value and get the largest group
     * List<Map.Entry<String, Integer>> largestGroup =
     *     EntryStream.of("a", 1, "b", 1, "c", 2, "d", 1)
     *         .collectThenApply(
     *             Collectors.groupingBy(Map.Entry::getValue),
     *             groups -> groups.values().stream()
     *                 .max(Comparator.comparing(List::size))
     *                 .orElse(Collections.emptyList())
     *         );
     * // Result: [Entry("a", 1), Entry("b", 1), Entry("d", 1)]
     * }</pre>
     *
     * @param <R> the type of the intermediate result from the collector
     * @param <RR> the type of the final result after applying the function
     * @param <E> the type of exception that may be thrown by the function
     * @param downstream the {@code Collector} to perform the mutable reduction
     * @param func the function to apply to the result of the collection
     * @return the result of applying the function to the collected data
     * @throws E if the function throws an exception
     * @see Stream#collectThenApply(Collector, Throwables.Function)
     * @see #collect(Collector)
     * @see #collectThenAccept(Collector, Throwables.Consumer)
     */
    @ParallelSupported
    @TerminalOp
    public <R, RR, E extends Exception> RR collectThenApply(final Collector<? super Map.Entry<K, V>, ?, R> downstream,
            final Throwables.Function<? super R, ? extends RR, E> func) throws E {
        return _stream.collectThenApply(downstream, func);
    }

    /**
     * Performs a mutable reduction operation using a {@code Collector} and then passes
     * the result to the provided consumer. This allows for performing side effects on
     * the collected result in a single pipeline step.
     *
     * <p>This is a terminal operation that supports parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect and print the result
     * EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .collectThenAccept(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
     *                        System.out::println);
     * // Prints: {a=1, b=2, c=3}
     *
     * // Group by value and process each group
     * EntryStream.of("a", 1, "b", 2, "c", 1, "d", 2)
     *     .collectThenAccept(
     *         Collectors.groupingBy(Map.Entry::getValue),
     *         groups -> groups.forEach((value, entries) ->
     *             System.out.println("Value " + value + " has " + entries.size() + " entries"))
     *     );
     * // Prints:
     * // Value 1 has 2 entries
     * // Value 2 has 2 entries
     * }</pre>
     *
     * @param <R> the type of the result from the collector
     * @param <E> the type of exception that may be thrown by the consumer
     * @param downstream the {@code Collector} to perform the mutable reduction
     * @param consumer the consumer to accept the result of the collection
     * @throws E if the consumer throws an exception
     * @see Stream#collectThenAccept(Collector, Throwables.Consumer)
     * @see #collect(Collector)
     * @see #collectThenApply(Collector, Throwables.Function)
     */
    @ParallelSupported
    @TerminalOp
    public <R, E extends Exception> void collectThenAccept(final Collector<? super Map.Entry<K, V>, ?, R> downstream,
            final Throwables.Consumer<? super R, E> consumer) throws E {
        _stream.collectThenAccept(downstream, consumer);
    }

    /**
     * Joins the entries of this stream into a string using the specified delimiter.
     * Each entry is formatted as "key=value".
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .join(", ");
     * // Result: "a=1, b=2, c=3"
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each entry
     * @return a string representation of the entries joined by the delimiter
     * @see Stream#join(CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence)
     * @see #join(CharSequence, CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence, CharSequence)
     */
    @Override
    public String join(final CharSequence delimiter) {
        return join(delimiter, "", "");
    }

    /**
     * Joins the entries of this stream into a string using the specified delimiter,
     * prefix, and suffix. Each entry is formatted as "key=value".
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .join(", ", "{", "}");
     * // Result: "{a=1, b=2, c=3}"
     *
     * String result2 = EntryStream.of("x", 10, "y", 20)
     *     .join(" | ", "[", "]");
     * // Result: "[x=10 | y=20]"
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each entry
     * @param prefix the sequence of characters to be used at the beginning
     * @param suffix the sequence of characters to be used at the end
     * @return a string representation of the entries joined by the delimiter and
     *         enclosed with prefix and suffix
     * @see Stream#join(CharSequence, CharSequence, CharSequence)
     * @see #join(CharSequence)
     * @see #join(CharSequence, CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence, CharSequence)
     */
    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        return join(delimiter, "=", prefix, suffix);
    }

    /**
     * Joins the entries of this stream into a string using the specified delimiter
     * and key-value delimiter. This allows customization of how keys and values are
     * separated within each entry.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .join(", ", ":");
     * // Result: "a:1, b:2, c:3"
     *
     * String result2 = EntryStream.of("name", "John", "age", "30")
     *     .join("; ", " -> ");
     * // Result: "name -> John; age -> 30"
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each entry
     * @param keyValueDelimiter the delimiter to be used between key and value in each entry
     * @return a string representation of the entries
     * @see #join(CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence, CharSequence)
     */
    @SequentialOnly
    @TerminalOp
    public String join(final CharSequence delimiter, final CharSequence keyValueDelimiter) {
        return join(delimiter, keyValueDelimiter, "", "");
    }

    /**
     * Joins the entries of this stream into a string using the specified delimiter,
     * key-value delimiter, prefix, and suffix. This provides full control over the
     * formatting of the output string.
     *
     * <p>This is a terminal operation that must be executed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = EntryStream.of("a", 1, "b", 2, "c", 3)
     *     .join(", ", ":", "{", "}");
     * // Result: "{a:1, b:2, c:3}"
     *
     * String json = EntryStream.of("name", "John", "age", 30, "city", "NYC")
     *     .join(", ", ": ", "{", "}")
     *     .replaceAll("(\\w+)", "\"$1\"");
     * // Result after replacement: {"name": "John", "age": "30", "city": "NYC"}
     *
     * String xml = EntryStream.of("id", "123", "name", "Product")
     *     .join(" ", "='", "<item ", "' />");
     * // Result: <item id='123 name='Product' />
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each entry
     * @param keyValueDelimiter the delimiter to be used between key and value in each entry
     * @param prefix the sequence of characters to be used at the beginning
     * @param suffix the sequence of characters to be used at the end
     * @return a string representation of the entries with custom formatting
     * @throws IllegalStateException if the stream has already been operated upon or closed
     * @see #join(CharSequence)
     * @see #join(CharSequence, CharSequence)
     * @see #join(CharSequence, CharSequence, CharSequence)
     * @see Joiner
     */
    @SequentialOnly
    @TerminalOp
    public String join(final CharSequence delimiter, final CharSequence keyValueDelimiter, final CharSequence prefix, final CharSequence suffix)
            throws IllegalStateException {
        _stream.assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Joiner joiner = Joiner.with(delimiter, keyValueDelimiter, prefix, suffix).reuseBuffer();
            final Iterator<Map.Entry<K, V>> iter = _stream.iteratorEx();

            while (iter.hasNext()) {
                joiner.appendEntry(iter.next());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    /**
     * Joins the entries of this stream using the provided {@code Joiner}.
     * The {@code Joiner} specifies the delimiter, prefix, suffix, and other formatting options.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Joiner joiner = Joiner.with(", ", "=", "{", "}");
     *
     * EntryStream.of("a", 1, "b", null, "c", 3)
     *     .joinTo(joiner);
     *
     * String result = joiner.toString();
     * // Result: "{a=1, b=null, c=3}" (a null value is rendered as "null", not skipped)
     * }</pre>
     *
     * @param joiner the {@code Joiner} to use for formatting the entries
     * @return the same {@code Joiner} instance after all entries have been appended
     * @throws IllegalStateException if the stream has already been operated upon or closed
     * @throws IllegalArgumentException if the joiner is null
     * @see Stream#joinTo(Joiner)
     * @see #join(CharSequence, CharSequence, CharSequence, CharSequence)
     * @see Joiner
     */
    @TerminalOp
    @Override
    public Joiner joinTo(final Joiner joiner) throws IllegalStateException, IllegalArgumentException {
        _stream.assertNotClosed();
        checkArgNotNull(joiner, cs.joiner);

        try {
            final Iterator<Map.Entry<K, V>> iter = _stream.iteratorEx();

            while (iter.hasNext()) {
                joiner.appendEntry(iter.next());
            }

        } finally {
            close();
        }

        return joiner;
    }

    /**
     * Transforms the current EntryStream into another EntryStream by applying the provided function.
     * The function takes a Stream as input and returns a new Stream.
     * The returned Stream is then wrapped into an EntryStream of this class.
     * This is equivalent to {@link #transformB(Function, boolean)} with {@code deferred = false}.
     *
     * <p>This method is marked as {@code @Beta} to indicate it's still in experimental phase.
     * It only runs sequentially, even in parallel streams, as noted by the {@code @SequentialOnly} annotation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Immediate transformation - executed right away
     * EntryStream<String, Integer> input = EntryStream.of("a", 5, "bb", 15, "ccc", 8);
     * EntryStream<String, Integer> result = input
     *     .<String, Integer>transformB(s -> s
     *         .filter(e -> e.getValue() > 10)
     *         .map(e -> new SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 2))
     *     );
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting EntryStream
     * @param <VV> the type of the values in the resulting EntryStream
     * @param transfer the function to be applied on the current stream to produce a new stream.
     * @return a new EntryStream transformed by the provided function.
     * @throws IllegalArgumentException if transfer is null
     * @see #transformB(Function, boolean)
     * @see Stream#transform(Function)
     * @see Stream#transformB(Function)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> transformB(
            final Function<? super Stream<Map.Entry<K, V>>, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> transfer) {
        return transformB(transfer, false);
    }

    /**
     * Transforms the current EntryStream into another EntryStream by applying the provided function.
     * The function takes a Stream as input and returns a new Stream.
     * The returned Stream is then wrapped into an EntryStream of this class.
     * The transformation can be deferred, which means it will be performed when the stream is consumed.
     *
     * <p>This method is marked as {@code @Beta} to indicate it's still in experimental phase.
     * It only runs sequentially, even in parallel streams, as noted by the {@code @SequentialOnly} annotation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deferred transformation - executed only when terminal operation is called
     * EntryStream<String, Integer> input = EntryStream.of("a", 5, "bb", 15, "ccc", 8);
     * EntryStream<String, Integer> result = input
     *     .<String, Integer>transformB(s -> s
     *         .filter(e -> e.getValue() > 10)
     *         .map(e -> new SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 2)),
     *         true  // uses deferred execution
     *     );
     * }</pre>
     *
     * @param <KK> the type of the keys in the resulting EntryStream
     * @param <VV> the type of the values in the resulting EntryStream
     * @param transfer the function to be applied on the current stream to produce a new stream.
     * @param deferred if {@code true}, the transformation is deferred until the EntryStream is consumed.
     * @return a new EntryStream transformed by the provided function.
     * @throws IllegalArgumentException if transfer is null
     * @see Stream#transform(Function)
     * @see Stream#transformB(Function)
     * @see Stream#transformB(Function, boolean)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public <KK, VV> EntryStream<KK, VV> transformB(
            final Function<? super Stream<Map.Entry<K, V>>, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> transfer,
            final boolean deferred) {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        if (deferred) {
            final Supplier<EntryStream<KK, VV>> delayInitializer = () -> EntryStream.of(transfer.apply(_stream));
            return EntryStream.defer(delayInitializer);
        } else {
            return of(transfer.apply(_stream));
        }
    }

    //    @SequentialOnly
    //    @IntermediateOp
    //    @Beta
    //    public <KK, VV> EntryStream<KK, VV> transform(Function<? super EntryStream<K, V>, EntryStream<KK, VV>> transfer) {
    //        return transfer.apply(this);
    //    }

    /**
     * Checks if the current stream is parallel.
     *
     * <p>This method delegates to the underlying stream implementation to determine
     * whether the stream is configured for parallel execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> stream = EntryStream.of(map);
     * boolean isParallel = stream.isParallel();   // false by default
     *
     * EntryStream<String, Integer> parallelStream = stream.parallel();
     * boolean isParallel2 = parallelStream.isParallel();   // true
     * }</pre>
     *
     * @return {@code true} if the stream is parallel, {@code false} otherwise
     * @see Stream#isParallel()
     */
    @Override
    public boolean isParallel() {
        return _stream.isParallel();
    }

    /**
     * Returns an equivalent stream that is sequential.
     *
     * <p>If the stream is already sequential, this method returns the current stream.
     * If the stream is parallel, it returns a new sequential EntryStream with the same elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> parallelStream = EntryStream.of(map).parallel();
     * EntryStream<String, Integer> sequentialStream = parallelStream.sequential();
     * boolean isSequential = !sequentialStream.isParallel();   // true
     * }</pre>
     *
     * @return a sequential stream
     * @see Stream#sequential()
     */
    @Override
    public EntryStream<K, V> sequential() {
        return _stream.isParallel() ? of(_stream.sequential()) : this;
    }

    @Override
    protected EntryStream<K, V> parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor,
            final boolean cancelUncompletedThreads) {
        return of(_stream.parallel(maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads));
    }

    @Override
    protected int maxThreadNum() {
        return _stream.maxThreadNum();
    }

    @Override
    protected Splitor splitor() {
        return _stream.splitor();
    }

    @Override
    protected AsyncExecutor asyncExecutor() {
        return _stream.asyncExecutor();
    }

    @Override
    protected boolean cancelUncompletedThreads() {
        return _stream.cancelUncompletedThreads();
    }

    @Override
    protected boolean isSorted() {
        return _stream.isSorted();
    }

    @Override
    protected Comparator<? super Map.Entry<K, V>> comparator() {
        return _stream.comparator();
    }

    @Override
    protected Deque<LocalRunnable> closeHandlers() {
        return _stream.closeHandlers();
    }

    @Override
    protected boolean isEmpty() {
        return _stream.isEmpty();
    }

    /**
     * Returns a new EntryStream with inverted key-value pairs, where each entry is represented
     * as a {@link DisposableEntry}. The elements can only be retrieved one by one, cannot be
     * modified or saved. The returned stream does not support operations that require two or more
     * elements at the same time (e.g., sort, distinct, pairMap, slidingMap, sliding, split,
     * toList, toSet, etc.).
     *
     * <p>This method is deprecated and marked as {@code @Beta} for experimental features.
     * It can only be used in sequential streams (not parallel).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original: {("a", 1), ("b", 2), ("c", 3)}
     * // Inverted: {(1, "a"), (2, "b"), (3, "c")}
     * EntryStream.of(map)
     *     .invertToDisposableEntry()
     *     .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
     * }</pre>
     *
     * @return a new EntryStream with inverted keys and values as DisposableEntry
     * @throws IllegalStateException if the stream is parallel
     * @deprecated Use {@link #invert()} instead, which returns a standard (non-disposable) inverted EntryStream.
     * @see #invert()
     */
    @SequentialOnly
    @Deprecated
    @Beta
    public EntryStream<V, K> invertToDisposableEntry() {
        checkState(!_stream.isParallel(), "invertToDisposableEntry cannot be applied to parallel stream");

        final Function<Map.Entry<K, V>, DisposableEntry<V, K>> mapper = new Function<>() {
            private final ReusableEntry<V, K> entry = new ReusableEntry<>();

            @Override
            public DisposableEntry<V, K> apply(final Map.Entry<K, V> e) {
                entry.set(e.getValue(), e.getKey());

                return entry;
            }
        };

        return map(mapper);
    }

    /**
     * Registers a close handler to be invoked when the stream is closed.
     * This method can be called multiple times to register multiple handlers.
     * Handlers are invoked in the order they were added, first-added, first-invoked.
     *
     * <p>Close handlers are useful for cleaning up resources that were allocated for stream processing.
     * The handlers will be invoked when the stream is closed, either explicitly by calling {@link #close()}
     * or implicitly when a terminal operation completes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream.of(map)
     *     .onClose(() -> System.out.println("Stream processing completed"))
     *     .onClose(() -> releaseResources())
     *     .filter(e -> e.getValue() > 10)
     *     .toMap();
     * // All close handlers are invoked after toMap() completes
     * }</pre>
     *
     * @param closeHandler a Runnable whose run method will be invoked when the stream is closed.
     * @return an EntryStream with the close handler registered. This may be the same EntryStream instance.
     */
    @Override
    public EntryStream<K, V> onClose(final Runnable closeHandler) {
        // return of(_stream.onClose(closeHandler));

        final Stream<Entry<K, V>> s = _stream.onClose(closeHandler);
        if (s == _stream) {
            // If the stream is unchanged, return this instance
            return this;
        } else {
            // If the stream has changed, create a new EntryStream with the updated stream
            return new EntryStream<>(s);
        }
    }

    /**
     * Closes the EntryStream.
     *
     * <p>This method releases any resources associated with the stream. If the stream is already closed,
     * then invoking this method has no effect. All registered close handlers (added via {@link #onClose(Runnable)})
     * are invoked when this method is called.
     *
     * <p>Streams are automatically closed after a terminal operation completes, so explicit closing
     * is typically not necessary. However, if a stream may be abandoned before a terminal operation,
     * it should be closed explicitly.
     *
     * <p>Close handlers are typically used to release external resources like file handles, database
     * connections, or network sockets that were used during sequence processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> stream = EntryStream.of(map);
     * try {
     *     // Use the stream
     *     stream.filter(e -> e.getValue() > 10).toMap();
     * } finally {
     *     stream.close();   // closes and releases resources
     * }
     * }</pre>
     *
     */
    @Override
    public synchronized void close() {
        _stream.close();
    }

    @SuppressWarnings({ "rawtypes" })
    static <K, V> Function<Map<K, V>, Stream<Map.Entry<K, V>>> mapFunc() {
        return (Function) mapper_func;
    }

    //    @SuppressWarnings("rawtypes")
    //    private static final EntryStream EMPTY_STREAM = new EntryStream(Stream.<Map.Entry> empty());

    /**
     * Returns an empty EntryStream.
     *
     * <p>This is a static factory method that creates an EntryStream with no elements.
     * It's useful as a starting point for building streams or as a return value for methods
     * that might return no results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> empty = EntryStream.empty();
     * long count = empty.count();   // 0
     *
     * // Using empty stream as default
     * EntryStream<String, Integer> stream = map.isEmpty()
     *     ? EntryStream.empty()
     *     : EntryStream.of(map);
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @return an empty EntryStream
     * @see Stream#empty()
     */
    public static <K, V> EntryStream<K, V> empty() {
        return new EntryStream<>(Stream.<Map.Entry<K, V>> empty());
    }

    /**
     * Returns an EntryStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Defer expensive map creation
     * EntryStream<String, Integer> deferred = EntryStream.defer(() ->
     *     EntryStream.of(expensiveMapComputation())
     * );
     *
     * // The expensive computation is not executed until the stream is consumed or closed
     * if (someCondition) {
     *     Map<String, Integer> result = deferred.toMap();   // processes the computation here
     * }
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param supplier the supplier that provides the EntryStream
     * @return an EntryStream that is lazily populated by the provided supplier
     * @throws IllegalArgumentException if {@code supplier} is null
     * @see Stream#defer(Supplier)
     */
    public static <K, V> EntryStream<K, V> defer(final Supplier<? extends EntryStream<? extends K, ? extends V>> supplier) { // NOSONAR
        N.checkArgNotNull(supplier, cs.supplier);

        final Stream<Map.Entry<? extends K, ? extends V>> s = Stream.defer(() -> supplier.get().entries());
        return new EntryStream<>(s);
    }

    /**
     * Returns an EntryStream containing the provided entry if it is not {@code null}, otherwise returns an empty EntryStream.
     *
     * <p>This method is useful for safely creating streams from {@code nullable} entries, avoiding NullPointerException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map.Entry<String, Integer> entry = map.entrySet().stream().findFirst().orElse(null);
     * EntryStream<String, Integer> stream = EntryStream.ofNullable(entry);
     * // If entry is null, stream will be empty
     *
     * // Safe chaining with nullable entries
     * Map.Entry<String, Integer> nullableEntry = getNullableEntry();
     * int sum = EntryStream.ofNullable(nullableEntry)
     *     .values().mapToInt(Integer::intValue)
     *     .sum();   // 0 if entry was null
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param entry the Map.Entry to be included in the EntryStream if it is not null
     * @return an EntryStream containing the provided entry if it is not {@code null}, otherwise an empty EntryStream
     * @see Stream#ofNullable(Object)
     */
    public static <K, V> EntryStream<K, V> ofNullable(final Map.Entry<K, V> entry) {
        if (entry == null) {
            return EntryStream.empty();
        }

        return of(Stream.of(entry));
    }

    /**
     * Returns an EntryStream containing a single entry with the provided key and value.
     *
     * <p>This is a convenience method for creating a stream with exactly one key-value pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> single = EntryStream.of("count", 42);
     * Map<String, Integer> map = single.toMap();   // {"count": 42}
     *
     * // Process a single entry
     * EntryStream.of("name", "John")
     *     .mapValue(String::toUpperCase)
     *     .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
     * // Prints: name: JOHN
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key to be included in the EntryStream
     * @param v1 the value to be included in the EntryStream
     * @return an EntryStream containing a single entry with the provided key and value
     * @see #of(Map)
     * @see #ofNullable(Map.Entry)
     * @see #concat(Map[])
     * @see #zip(Object[], Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1)));
    }

    /**
     * Returns an EntryStream containing two entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly two key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
     * Map<String, Integer> map = stream.toMap();   // {"a": 1, "b": 2}
     *
     * // Filter and process two entries
     * EntryStream.of("score", 85, "grade", 90)
     *     .filter(e -> e.getValue() > 80)
     *     .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
     * // Prints: score: 85, grade: 90
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @return an EntryStream containing two entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    /**
     * Returns an EntryStream containing three entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly three key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Double> grades = EntryStream.of(
     *     "Math", 95.5,
     *     "English", 87.0,
     *     "Science", 92.3
     * );
     * double average = grades.values().mapToDouble(Double::doubleValue).average().orElse(0.0);
     * // average = 91.6
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @param k3 the key of the third entry
     * @param v3 the value of the third entry
     * @return an EntryStream containing three entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3)));
    }

    /**
     * Returns an EntryStream containing four entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly four key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, String> config = EntryStream.of(
     *     "host", "localhost",
     *     "port", "8080",
     *     "username", "admin",
     *     "timeout", "30"
     * );
     * Map<String, String> configMap = config.toMap();
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @param k3 the key of the third entry
     * @param v3 the value of the third entry
     * @param k4 the key of the fourth entry
     * @param v4 the value of the fourth entry
     * @return an EntryStream containing four entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4)));
    }

    /**
     * Returns an EntryStream containing five entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly five key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<Integer, String> rankNames = EntryStream.of(
     *     1, "Gold",
     *     2, "Silver",
     *     3, "Bronze",
     *     4, "Participant",
     *     5, "Participant"
     * );
     * List<String> top3 = rankNames
     *     .filterByKey(rank -> rank <= 3)
     *     .values()
     *     .toList();   // ["Gold", "Silver", "Bronze"]
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @param k3 the key of the third entry
     * @param v3 the value of the third entry
     * @param k4 the key of the fourth entry
     * @param v4 the value of the fourth entry
     * @param k5 the key of the fifth entry
     * @param v5 the value of the fifth entry
     * @return an EntryStream containing five entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5)));
    }

    /**
     * Returns an EntryStream containing six entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly six key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, Integer> dayNumbers = EntryStream.of(
     *     "Monday", 1,
     *     "Tuesday", 2,
     *     "Wednesday", 3,
     *     "Thursday", 4,
     *     "Friday", 5,
     *     "Saturday", 6
     * );
     * Map<Integer, String> numberToDay = dayNumbers
     *     .invert()
     *     .toMap();   // {1: "Monday", 2: "Tuesday", ...}
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @param k3 the key of the third entry
     * @param v3 the value of the third entry
     * @param k4 the key of the fourth entry
     * @param v4 the value of the fourth entry
     * @param k5 the key of the fifth entry
     * @param v5 the value of the fifth entry
     * @param k6 the key of the sixth entry
     * @param v6 the value of the sixth entry
     * @return an EntryStream containing six entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5), new SimpleImmutableEntry<>(k6, v6)));
    }

    /**
     * Returns an EntryStream containing seven entries with the provided keys and values.
     *
     * <p>This is a convenience method for creating a stream with exactly seven key-value pairs.
     * The entries appear in the stream in the order they are specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntryStream<String, String> weekColors = EntryStream.of(
     *     "Monday", "Blue",
     *     "Tuesday", "Red",
     *     "Wednesday", "Green",
     *     "Thursday", "Orange",
     *     "Friday", "Yellow",
     *     "Saturday", "Purple",
     *     "Sunday", "Pink"
     * );
     * Set<String> weekendColors = weekColors
     *     .filterByKey(day -> day.startsWith("S"))
     *     .values()
     *     .toSet();   // {"Purple", "Pink"}
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param k1 the key of the first entry
     * @param v1 the value of the first entry
     * @param k2 the key of the second entry
     * @param v2 the value of the second entry
     * @param k3 the key of the third entry
     * @param v3 the value of the third entry
     * @param k4 the key of the fourth entry
     * @param v4 the value of the fourth entry
     * @param k5 the key of the fifth entry
     * @param v5 the value of the fifth entry
     * @param k6 the key of the sixth entry
     * @param v6 the value of the sixth entry
     * @param k7 the key of the seventh entry
     * @param v7 the value of the seventh entry
     * @return an EntryStream containing seven entries with the provided keys and values
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5), new SimpleImmutableEntry<>(k6, v6),
                new SimpleImmutableEntry<>(k7, v7)));
    }

    static <K, V> EntryStream<K, V> of(final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        return new EntryStream<>(s);
    }

    /**
     * Returns an EntryStream containing the entries from the given map.
     *
     * <p>This method creates a stream of all key-value pairs in the map. The order of entries
     * in the stream depends on the map implementation (e.g., HashMap has no guaranteed order,
     * LinkedHashMap preserves insertion order, TreeMap uses natural ordering).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> scores = new HashMap<>();
     * scores.put("Alice", 95);
     * scores.put("Bob", 87);
     * scores.put("Charlie", 92);
     *
     * EntryStream<String, Integer> stream = EntryStream.of(scores);
     * double average = stream.values().mapToInt(Integer::intValue).average().orElse(0.0);
     * // average = 91.33...
     *
     * // Filter and transform map entries
     * Map<String, String> highScorers = EntryStream.of(scores)
     *     .filter(e -> e.getValue() > 90)
     *     .mapValue(v -> "Score: " + v)
     *     .toMap();   // {"Alice": "Score: 95", "Charlie": "Score: 92"}
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param map the entries to be included in the EntryStream
     * @return an EntryStream containing the entries from the given map
     * @see Stream#of(Object[])
     */
    public static <K, V> EntryStream<K, V> of(final Map<? extends K, ? extends V> map) {
        return new EntryStream<>(map, Stream.of(map));
    }

    /**
     * Returns an EntryStream containing the entries from the given iterator.
     *
     * <p>This method creates a stream that lazily consumes entries from the iterator.
     * The iterator is consumed as the stream operations are executed. Once the stream
     * is consumed, the iterator will be exhausted and cannot be reused.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
     * EntryStream<String, Integer> stream = EntryStream.of(iterator);
     *
     * // Process entries from iterator
     * List<String> keys = stream
     *     .filterByValue(v -> v > 50)
     *     .keys()
     *     .toList();
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param iterator the iterator providing the entries to be included in the EntryStream
     * @return an EntryStream containing the entries from the given iterator
     */
    public static <K, V> EntryStream<K, V> of(final Iterator<? extends Map.Entry<? extends K, ? extends V>> iterator) {
        return new EntryStream<>(Stream.of(iterator));
    }

    /**
     * Returns an EntryStream containing the entries from the given iterable.
     *
     * <p>This method creates a stream of all entries in the iterable. The iterable can be
     * any collection of Map.Entry objects, such as the entrySet() of a map or a custom
     * collection of entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
     * EntryStream<String, Integer> stream = EntryStream.of(entrySet);
     *
     * // Custom list of entries
     * List<Map.Entry<String, Double>> entries = Arrays.asList(
     *     new SimpleEntry<>("pi", 3.14),
     *     new SimpleEntry<>("e", 2.718),
     *     new SimpleEntry<>("phi", 1.618)
     * );
     * Map<String, Double> constants = EntryStream.of(entries).toMap();
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param entries the iterable providing the entries to be included in the EntryStream
     * @return an EntryStream containing the entries from the given iterable
     */
    public static <K, V> EntryStream<K, V> of(final Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
        return new EntryStream<>(Stream.of(entries));
    }

    //    @SafeVarargs
    //    public static <K, V> EntryStream<K, V> of(final Map.Entry<K, V>... entries) {
    //        return new EntryStream<K, V>(Stream.of(entries));
    //    }

    /**
     * Returns an EntryStream containing the entries from the given Multimap.
     *
     * <p>A Multimap is a map where each key can be associated with multiple values.
     * This method creates a stream where each entry consists of a key and its associated
     * collection of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> multimap = N.newListMultimap();
     * multimap.put("even", 2);
     * multimap.put("even", 4);
     * multimap.put("odd", 1);
     * multimap.put("odd", 3);
     *
     * EntryStream<String, Collection<Integer>> stream = EntryStream.of(multimap);
     * stream.forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
     * // Prints:
     * // even: [2, 4]
     * // odd: [1, 3]
     * }</pre>
     *
     * @param <K> the type of keys in the EntryStream
     * @param <E> the type of elements in the value collections
     * @param <V> the type of value collections (must extend Collection&lt;E&gt;)
     * @param multimap the Multimap providing the entries to be included in the EntryStream
     * @return an EntryStream containing the entries from the given Multimap
     */
    public static <K, E, V extends Collection<E>> EntryStream<K, V> of(final Multimap<? extends K, E, ? extends V> multimap) {
        return multimap == null ? EntryStream.empty() : (EntryStream<K, V>) multimap.stream().mapToEntry(Fn.identity());
    }

    /**
     * Returns an EntryStream from the provided array and key mapper function.
     *
     * <p>Each element in the array becomes a value in the resulting entries, with keys
     * generated by applying the keyMapper function to each element. The values in the
     * entries are the original array elements themselves.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * EntryStream<Integer, String> stream = EntryStream.of(names, String::length);
     * Map<Integer, String> nameLengths = stream.toMap();
     * // Result: {5="Alice", 3="Bob", 7="Charlie"} (keys are string lengths)
     *
     * // Using first character as key
     * EntryStream<Character, String> firstCharStream = EntryStream.of(names,
     *     name -> name.charAt(0));
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param <K> the type of keys in the EntryStream
     * @param a the array to be converted into an EntryStream
     * @param keyMapper the function to map array elements to keys
     * @return an EntryStream containing the entries from the provided array
     */
    public static <T, K> EntryStream<K, T> of(final T[] a, final Function<? super T, ? extends K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(a).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns an EntryStream from the provided array and key and value mapper functions.
     *
     * <p>Each element in the array is transformed into an entry by applying the keyMapper
     * to generate the key and the valueMapper to generate the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] words = {"hello", "world", "java"};
     * EntryStream<String, Integer> stream = EntryStream.of(words,
     *     String::toUpperCase,  // maps the key
     *     String::length        // maps the value
     * );
     * Map<String, Integer> wordLengths = stream.toMap();
     * // Result: {"HELLO": 5, "WORLD": 5, "JAVA": 4}
     *
     * // Transform numbers to different representations
     * Integer[] numbers = {1, 2, 3, 4, 5};
     * Map<String, String> representations = EntryStream.of(numbers,
     *     n -> "Decimal: " + n,
     *     n -> "Binary: " + Integer.toBinaryString(n)
     * ).toMap();
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param a the array to be converted into an EntryStream
     * @param keyMapper the function to map array elements to keys
     * @param valueMapper the function to map array elements to values
     * @return an EntryStream containing the entries from the provided array
     */
    public static <T, K, V> EntryStream<K, V> of(final T[] a, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        return Stream.of(a).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns an EntryStream from the provided collection and key mapper function.
     *
     * <p>Each element in the collection becomes a value in the resulting entries, with keys
     * generated by applying the keyMapper function to each element. The values in the
     * entries are the original collection elements themselves.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 25),
     *     new Person("Bob", 30),
     *     new Person("Charlie", 25)
     * );
     * EntryStream<Integer, Person> byAge = EntryStream.of(people, Person::getAge);
     * // Group people by age
     * Map<Integer, List<Person>> ageGroups = byAge.groupTo(LinkedHashMap::new);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <K> the type of keys in the EntryStream
     * @param c the iterable to be converted into an EntryStream
     * @param keyMapper the function to map collection elements to keys
     * @return an EntryStream containing the entries from the provided collection
     */
    public static <T, K> EntryStream<K, T> of(final Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(c).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns an EntryStream from the provided collection and key and value mapper functions.
     *
     * <p>Each element in the collection is transformed into an entry by applying both the keyMapper
     * and valueMapper functions to it. This allows for flexible conversion of collections into
     * EntryStreams with customized keys and values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 25),
     *     new Person("Bob", 30),
     *     new Person("Charlie", 25)
     * );
     *
     * // Create EntryStream with name as key and age as value
     * Map<String, Integer> nameToAge = EntryStream.of(
     *     people,
     *     Person::getName,
     *     Person::getAge
     * ).toMap();
     * // Result: {Alice=25, Bob=30, Charlie=25}
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <K> the type of keys in the EntryStream
     * @param <V> the type of values in the EntryStream
     * @param c the iterable to be converted into an EntryStream
     * @param keyMapper the function to map collection elements to keys
     * @param valueMapper the function to map collection elements to values
     * @return an EntryStream containing the entries from the provided collection
     */
    public static <T, K, V> EntryStream<K, V> of(final Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        return Stream.of(c).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns an EntryStream from the provided iterator and key mapper function.
     * Each element from the iterator is transformed into a Map.Entry where the key is determined by
     * the keyMapper function and the value is the element itself.
     *
     * <p>This method is useful when you have a collection of values and want to create entries
     * where each value is associated with a computed key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * EntryStream<Integer, String> stream = EntryStream.of(names.iterator(), String::length);
     * Map<Integer, String> result = stream.toMap();
     * // Result: {5=Alice, 3=Bob, 7=Charlie}
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param <K> the type of keys in the resulting EntryStream
     * @param iter the iterator providing the elements to be converted into entries
     * @param keyMapper the function that extracts/computes a key from each element
     * @return a new EntryStream containing entries created from the iterator elements
     * @see #of(Iterator, Function, Function)
     * @see Stream#mapToEntry(Function, Function)
     */
    public static <T, K> EntryStream<K, T> of(final Iterator<? extends T> iter, final Function<? super T, ? extends K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(iter).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Returns an EntryStream from the provided iterator with custom key and value mapper functions.
     * Each element from the iterator is transformed into a Map.Entry where both the key and value
     * are computed using the provided mapper functions.
     *
     * <p>This method provides maximum flexibility when creating entries from an iterator,
     * allowing you to transform the original elements into both keys and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 25),
     *     new Person("Bob", 30)
     * );
     * EntryStream<String, Integer> stream = EntryStream.of(
     *     people.iterator(),
     *     Person::getName,
     *     Person::getAge
     * );
     * Map<String, Integer> result = stream.toMap();
     * // Result: {Alice=25, Bob=30}
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param <K> the type of keys in the resulting EntryStream
     * @param <V> the type of values in the resulting EntryStream
     * @param iter the iterator providing the source elements
     * @param keyMapper the function that extracts/computes a key from each element
     * @param valueMapper the function that extracts/computes a value from each element
     * @return a new EntryStream containing entries created from the iterator elements
     * @see #of(Iterator, Function)
     * @see Stream#mapToEntry(Function, Function)
     */
    public static <T, K, V> EntryStream<K, V> of(final Iterator<? extends T> iter, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) throws IllegalArgumentException {
        return Stream.of(iter).mapToEntry(keyMapper, valueMapper);
    }

    /**
     * Concatenates multiple maps into a single EntryStream.
     * All entries from all provided maps are combined into a single stream in the order
     * they appear in each map.
     *
     * <p>If the same key appears in multiple maps, it will appear multiple times in the
     * resulting stream. This method does not perform any deduplication.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
     * Map<String, Integer> map2 = Map.of("c", 3, "d", 4);
     * Map<String, Integer> map3 = Map.of("e", 5, "a", 6);
     *
     * EntryStream<String, Integer> stream = EntryStream.concat(map1, map2, map3);
     * List<Map.Entry<String, Integer>> entries = stream.toList();
     * // Result contains all entries: [a=1, b=2, c=3, d=4, e=5, a=6]
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param maps the maps to be concatenated into a single stream
     * @return an EntryStream containing all entries from all provided maps
     * @see #concat(Collection)
     * @see #merge(Map, Map, BiFunction)
     * @see #of(Map)
     * @see #zip(Object[], Object[])
     */
    @SafeVarargs
    public static <K, V> EntryStream<K, V> concat(final Map<? extends K, ? extends V>... maps) {
        if (N.isEmpty(maps)) {
            return EntryStream.empty();
        }

        return Stream.of(maps).flatmapToEntry(Fn.identity());
    }

    /**
     * Concatenates multiple maps from a collection into a single EntryStream.
     * All entries from all maps in the collection are combined into a single stream
     * in the order they appear in each map.
     *
     * <p>If the same key appears in multiple maps, it will appear multiple times in the
     * resulting stream. This method does not perform any deduplication.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Integer>> maps = Arrays.asList(
     *     Map.of("a", 1, "b", 2),
     *     Map.of("c", 3, "d", 4),
     *     Map.of("e", 5, "a", 6)
     * );
     *
     * EntryStream<String, Integer> stream = EntryStream.concat(maps);
     * Map<String, List<Integer>> grouped = stream.groupTo();
     * // Result: {a=[1, 6], b=[2], c=[3], d=[4], e=[5]}
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param maps the collection of maps to be concatenated
     * @return an EntryStream containing all entries from all maps in the collection
     * @see #concat(Map[])
     * @see Stream#concat(Collection)
     * @see Stream#flatmapToEntry(Function)
     */
    public static <K, V> EntryStream<K, V> concat(final Collection<? extends Map<? extends K, ? extends V>> maps) {
        if (N.isEmpty(maps)) {
            return EntryStream.empty();
        }

        return Stream.of(maps).flatmapToEntry(Fn.<Map<? extends K, ? extends V>> identity());
    }

    /**
     * Merges two maps into a single EntryStream based on the provided nextSelector function.
     * The nextSelector function determines the order in which entries from the two maps
     * appear in the resulting stream.
     *
     * <p>The merge operation is particularly useful when you need to combine sorted maps
     * while maintaining a specific ordering, or when implementing custom merge strategies
     * for entries with duplicate keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Integer, String> map1 = new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e"));
     * Map<Integer, String> map2 = new TreeMap<>(Map.of(2, "b", 4, "d", 6, "f"));
     *
     * // Merge by key order
     * EntryStream<Integer, String> merged = EntryStream.merge(map1, map2,
     *     (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND
     * );
     * List<Map.Entry<Integer, String>> result = merged.toList();
     * // Result: [1=a, 2=b, 3=c, 4=d, 5=e, 6=f]
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param a the first map to be merged
     * @param b the second map to be merged
     * @param nextSelector a function that determines which entry should be selected next from the two maps
     * @return an EntryStream containing the merged entries from the two maps
     * @throws IllegalArgumentException if nextSelector is null
     * @see #merge(Map, Map, Map, BiFunction)
     * @see #merge(Collection, BiFunction)
     * @see #concat(Map[])
     * @see #zip(Object[], Object[])
     * @see MergeResult
     */
    public static <K, V> EntryStream<K, V> merge(final Map<? extends K, ? extends V> a, final Map<? extends K, ? extends V> b,
            final BiFunction<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        } else {
            @SuppressWarnings("rawtypes")
            final Set<Map.Entry<K, V>> entrySetA = (Set) a.entrySet();
            @SuppressWarnings("rawtypes")
            final Set<Map.Entry<K, V>> entrySetB = (Set) b.entrySet();
            return Stream.merge(entrySetA, entrySetB, nextSelector).mapToEntry(Fn.identity());
        }
    }

    /**
     * Merges three maps into a single EntryStream based on the provided nextSelector function.
     * The nextSelector function determines the order in which entries from the maps
     * appear in the resulting stream by comparing pairs of entries.
     *
     * <p>This method extends the two-map merge functionality to handle three maps,
     * internally merging them in pairs while maintaining the ordering defined by
     * the nextSelector function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use ordered maps (e.g. LinkedHashMap or sorted maps) so that
     * // the merge inputs have a defined iteration order.
     * Map<String, Integer> scores1 = new LinkedHashMap<>();
     * scores1.put("Bob", 90); scores1.put("Alice", 85);
     * Map<String, Integer> scores2 = new LinkedHashMap<>();
     * scores2.put("David", 92); scores2.put("Charlie", 88);
     * Map<String, Integer> scores3 = new LinkedHashMap<>();
     * scores3.put("Frank", 89); scores3.put("Eve", 87);
     *
     * // Pick the entry with the higher score at each step.
     * // Note: like all merge-style algorithms this only yields a globally
     * // sorted result when each input is already sorted by the same criterion.
     * EntryStream<String, Integer> topScores = EntryStream.merge(scores1, scores2, scores3,
     *     (e1, e2) -> e1.getValue() > e2.getValue() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND
     * );
     * List<String> result = topScores.keys().toList();
     * // Each input is pre-sorted by score (desc), so the merged result is too:
     * // [David, Bob, Frank, Charlie, Eve, Alice]
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param a the first map to be merged
     * @param b the second map to be merged
     * @param c the third map to be merged
     * @param nextSelector a function that determines which entry should be selected next
     * @return an EntryStream containing the merged entries from the three maps
     * @throws IllegalArgumentException if nextSelector is null
     * @see #merge(Map, Map, BiFunction)
     * @see #merge(Collection, BiFunction)
     * @see MergeResult
     * @see N#merge(Iterable, Iterable, BiFunction)
     */
    public static <K, V> EntryStream<K, V> merge(final Map<? extends K, ? extends V> a, final Map<? extends K, ? extends V> b,
            final Map<? extends K, ? extends V> c, final BiFunction<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, MergeResult> nextSelector)
            throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(a)) {
            return merge(b, c, nextSelector);
        } else if (N.isEmpty(b)) {
            return merge(a, c, nextSelector);
        } else if (N.isEmpty(c)) {
            return merge(a, b, nextSelector);
        } else {
            @SuppressWarnings("rawtypes")
            final Set<Map.Entry<K, V>> entrySetA = (Set) a.entrySet();
            @SuppressWarnings("rawtypes")
            final Set<Map.Entry<K, V>> entrySetB = (Set) b.entrySet();
            @SuppressWarnings("rawtypes")
            final Set<Map.Entry<K, V>> entrySetC = (Set) c.entrySet();
            return Stream.merge(entrySetA, entrySetB, entrySetC, nextSelector).mapToEntry(Fn.identity());
        }
    }

    /**
     * Merges a collection of maps into a single EntryStream based on the provided nextSelector function.
     * The nextSelector function determines the order in which entries from different maps
     * appear in the resulting stream.
     *
     * <p>This method is useful when you have multiple maps that need to be merged with
     * a custom ordering strategy, such as merging sorted maps while maintaining order
     * or implementing priority-based merge strategies.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Integer>> inventories = Arrays.asList(
     *     Map.of("apple", 10, "banana", 5),
     *     Map.of("apple", 15, "orange", 8),
     *     Map.of("banana", 3, "orange", 12)
     * );
     *
     * // Merge maps prioritizing higher quantities
     * EntryStream<String, Integer> merged = EntryStream.merge(inventories,
     *     (e1, e2) -> {
     *         int cmp = e1.getKey().compareTo(e2.getKey());
     *         if (cmp != 0) return cmp < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
     *         return e1.getValue() >= e2.getValue() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
     *     }
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the maps
     * @param <V> the type of values in the maps
     * @param maps the collection of maps to be merged
     * @param nextSelector a function that determines which entry should be selected next
     * @return an EntryStream containing the merged entries from all maps
     * @throws IllegalArgumentException if nextSelector is null
     * @see #merge(Map, Map, BiFunction)
     * @see #merge(Map, Map, Map, BiFunction)
     * @see Stream#mergeIterables(Collection, BiFunction)
     * @see MergeResult
     */
    public static <K, V> EntryStream<K, V> merge(final Collection<? extends Map<? extends K, ? extends V>> maps,
            final BiFunction<? super Map.Entry<? extends K, ? extends V>, ? super Map.Entry<? extends K, ? extends V>, MergeResult> nextSelector)
            throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(maps)) {
            return EntryStream.empty();
        }

        final List<Set<? extends Map.Entry<? extends K, ? extends V>>> entryIteratorList = new ArrayList<>(maps.size());

        for (final Map<? extends K, ? extends V> map : maps) {
            if (N.notEmpty(map)) {
                entryIteratorList.add(map.entrySet());
            }
        }

        //noinspection resource
        return Stream.mergeIterables(entryIteratorList, nextSelector).mapToEntry(Fn.identity());
    }

    /**
     * Zips two arrays of keys and values into an EntryStream.
     * Each key at index i is paired with the value at index i to create an entry.
     * The operation continues until one of the arrays runs out of elements.
     *
     * <p>This method is useful for creating key-value pairs from parallel arrays
     * where the relationship between keys and values is defined by their position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35, 40};
     *
     * EntryStream<String, Integer> stream = EntryStream.zip(names, ages);
     * Map<String, Integer> result = stream.toMap();
     * // Result: {Alice=25, Bob=30, Charlie=35}
     * // Note: The value 40 is ignored since there's no corresponding name
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the array of keys
     * @param values the array of values
     * @return an EntryStream containing entries created from the corresponding elements of the arrays
     * @see #zip(Object[], Object[], Object, Object)
     * @see #zip(Iterable, Iterable)
     * @see #of(Object[], Function)
     * @see #concat(Map[])
     * @see #merge(Map, Map, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final K[] keys, final V[] values) {
        if (N.isEmpty(keys) || N.isEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, zipFunction).mapToEntry(mapper);
    }

    /**
     * Zips two arrays of keys and values into an EntryStream with default values.
     * Each key at index i is paired with the value at index i to create an entry.
     * If one array is shorter, the provided default values are used for the remaining elements.
     *
     * <p>This method ensures that all elements from both arrays are used, filling in
     * with default values when one array is longer than the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob"};
     * Integer[] ages = {25, 30, 35};
     *
     * EntryStream<String, Integer> stream = EntryStream.zip(names, ages, "Unknown", -1);
     * List<Map.Entry<String, Integer>> result = stream.toList();
     * // Result: [Alice=25, Bob=30, Unknown=35]
     *
     * String[] cities = {"New York", "London", "Paris"};
     * Integer[] populations = {8000000};
     *
     * EntryStream<String, Integer> stream2 = EntryStream.zip(cities, populations, null, 0);
     * Map<String, Integer> result2 = stream2.toMap();
     * // Result: {New York=8000000, London=0, Paris=0}
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the array of keys
     * @param values the array of values
     * @param valueForNoneKey the default key to use when the keys array is shorter (i.e., the values array is longer)
     * @param valueForNoneValue the default value to use when the values array is shorter (i.e., the keys array is longer)
     * @return an EntryStream containing entries created from the arrays with defaults for missing elements
     * @see #zip(Object[], Object[])
     * @see #zip(Iterable, Iterable, Object, Object)
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     * @see N#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final K[] keys, final V[] values, final K valueForNoneKey, final V valueForNoneValue) {
        if (N.isEmpty(keys) && N.isEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, valueForNoneKey, valueForNoneValue, zipFunction).mapToEntry(mapper);
    }

    /**
     * Zips two iterables of keys and values into an EntryStream.
     * Each key from the keys iterable is paired with the corresponding value from the values iterable
     * to create an entry. The operation continues until one of the iterables runs out of elements.
     *
     * <p>This method is useful for creating key-value pairs from separate collections
     * where the relationship is defined by iteration order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> userIds = Arrays.asList("user1", "user2", "user3");
     * Set<String> userNames = Set.of("Alice", "Bob", "Charlie");
     *
     * EntryStream<String, String> stream = EntryStream.zip(userIds, userNames);
     * Map<String, String> userMap = stream.toMap();
     * // Result depends on Set iteration order, but could be:
     * // {user1=Alice, user2=Bob, user3=Charlie}
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the iterable providing keys
     * @param values the iterable providing values
     * @return an EntryStream containing entries created from the corresponding elements
     * @see #zip(Iterable, Iterable, Object, Object)
     * @see #zip(Object[], Object[])
     * @see Stream#zip(Iterable, Iterable, BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values) {
        if (keys == null || values == null) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, zipFunction).mapToEntry(mapper);
    }

    /**
     * Zips two iterables of keys and values into an EntryStream with default values.
     * Each key is paired with the corresponding value to create an entry.
     * If one iterable is shorter, the provided default values are used for the remaining elements.
     *
     * <p>This method ensures that all elements from both iterables are used, filling in
     * with default values when one iterable has fewer elements than the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> products = Arrays.asList("Laptop", "Mouse", "Keyboard", "Monitor");
     * List<Double> prices = Arrays.asList(999.99, 29.99);
     *
     * EntryStream<String, Double> stream = EntryStream.zip(
     *     products, prices, "Unknown Product", 0.0
     * );
     * Map<String, Double> priceMap = stream.toMap();
     * // Result: {Laptop=999.99, Mouse=29.99, Keyboard=0.0, Monitor=0.0}
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the iterable providing keys
     * @param values the iterable providing values
     * @param valueForNoneKey the default key to use when the keys iterable is shorter (i.e., values has more elements)
     * @param valueForNoneValue the default value to use when the values iterable is shorter (i.e., keys has more elements)
     * @return an EntryStream containing entries with defaults for missing elements
     * @see #zip(Iterable, Iterable)
     * @see #zip(Object[], Object[], Object, Object)
     * @see Stream#zip(Iterable, Iterable, Object, Object, BiFunction)
     * @see N#zip(Iterable, Iterable, Object, Object, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values, final K valueForNoneKey,
            final V valueForNoneValue) {
        // Only empty when BOTH sides are null: with default values, a null side is treated as empty
        // and filled with the default, matching the array sibling and the underlying zip.
        if (keys == null && values == null) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, valueForNoneKey, valueForNoneValue, zipFunction).mapToEntry(mapper);
    }

    /**
     * Zips two iterators of keys and values into an EntryStream.
     * Each key from the keys iterator is paired with the corresponding value from the values iterator
     * to create an entry. The operation continues until one of the iterators runs out of elements.
     *
     * <p>This method consumes elements from both iterators. Once consumed, the iterators
     * cannot be reset or reused.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> ids = Arrays.asList(1, 2, 3).iterator();
     * Iterator<String> names = Arrays.asList("One", "Two", "Three", "Four").iterator();
     *
     * EntryStream<Integer, String> stream = EntryStream.zip(ids, names);
     * Map<Integer, String> result = stream.toMap();
     * // Result: {1=One, 2=Two, 3=Three}
     * // Note: "Four" is not included as there's no corresponding id
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the iterator providing keys
     * @param values the iterator providing values
     * @return an EntryStream containing entries created from the corresponding elements
     * @see #zip(Iterator, Iterator, Object, Object)
     * @see #zip(Iterable, Iterable)
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final Iterator<? extends K> keys, final Iterator<? extends V> values) {
        if (keys == null || values == null) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, zipFunction).mapToEntry(mapper);
    }

    /**
     * Zips two iterators of keys and values into an EntryStream with default values.
     * Each key is paired with the corresponding value to create an entry.
     * If one iterator is exhausted before the other, the provided default values are used.
     *
     * <p>This method ensures that all elements from both iterators are used, filling in
     * with default values when one iterator has fewer elements than the other.
     * The iterators are consumed by this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> categories = Arrays.asList("Electronics", "Books", "Clothing").iterator();
     * Iterator<Integer> counts = Arrays.asList(100, 200, 300, 400, 500).iterator();
     *
     * EntryStream<String, Integer> stream = EntryStream.zip(
     *     categories, counts, "Other", 0
     * );
     * List<Map.Entry<String, Integer>> result = stream.toList();
     * // Result: [Electronics=100, Books=200, Clothing=300, Other=400, Other=500]
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keys the iterator providing keys
     * @param values the iterator providing values
     * @param valueForNoneKey the default key to use when the keys iterator is shorter (i.e., values has more elements)
     * @param valueForNoneValue the default value to use when the values iterator is shorter (i.e., keys has more elements)
     * @return an EntryStream containing entries with defaults for missing elements
     * @see #zip(Iterator, Iterator)
     * @see #zip(Iterable, Iterable, Object, Object)
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static <K, V> EntryStream<K, V> zip(final Iterator<? extends K> keys, final Iterator<? extends V> values, final K valueForNoneKey,
            final V valueForNoneValue) {
        // Only empty when BOTH sides are null: with default values, a null side is treated as empty
        // and filled with the default, matching the array sibling and the underlying zip.
        if (keys == null && values == null) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        //noinspection resource
        return Stream.zip(keys, values, valueForNoneKey, valueForNoneValue, zipFunction).mapToEntry(mapper);
    }

    static class ReusableEntry<K, V> extends DisposableEntry<K, V> {
        private K key = null;
        private V value = null;
        private boolean flag = false; //check if it's used/read.

        /**
         * Returns the key corresponding to this entry.
         * This method resets the internal flag to indicate the entry has been accessed.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry = new ReusableEntry<>();
         * entry.set("key1", 100);
         * String k = entry.getKey(); // returns "key1"
         *
         * ReusableEntry<String, Integer> entry2 = new ReusableEntry<>();
         * entry2.set(null, 42);
         * String k2 = entry2.getKey(); // returns null
         * }</pre>
         *
         * @return the key corresponding to this entry
         */
        @Override
        public K getKey() {
            flag = false;
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         * This method resets the internal flag to indicate the entry has been accessed.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry = new ReusableEntry<>();
         * entry.set("key1", 100);
         * Integer v = entry.getValue(); // returns 100
         *
         * ReusableEntry<String, Integer> entry2 = new ReusableEntry<>();
         * entry2.set("key2", null);
         * Integer v2 = entry2.getValue(); // returns null
         * }</pre>
         *
         * @return the value corresponding to this entry
         */
        @Override
        public V getValue() {
            flag = false;
            return value;
        }

        /**
         * Sets the key and value for this reusable entry.
         * This method can only be called once per entry usage cycle.
         * If called when the entry has already been set (flag is true), an IllegalStateException is thrown.
         *
         * <p>This is primarily used internally for performance optimization to avoid creating
         * new entry objects for each iteration.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry = new ReusableEntry<>();
         * entry.set("key1", 100);
         * String k = entry.getKey();   // returns "key1", resets flag
         * entry.set("key2", 200);      // sets the new pair (valid since the entry was read)
         * // entry.set("key3", 300);   // Would throw IllegalStateException if previous set wasn't read
         * }</pre>
         *
         * @param key the key to set
         * @param value the value to set
         * @throws IllegalStateException if the entry has already been set without being read
         */
        public void set(final K key, final V value) {
            if (flag) {
                throw new IllegalStateException("Entry has already been set; call get() to retrieve the value before setting again");
            }

            this.key = key;
            this.value = value;
            flag = true;
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns {@code true} if the given object is also a Map.Entry and the two entries represent the same mapping.
         * Two entries e1 and e2 represent the same mapping if the keys are equal and the values are equal.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry1 = new ReusableEntry<>();
         * entry1.set("a", 1);
         * ReusableEntry<String, Integer> entry2 = new ReusableEntry<>();
         * entry2.set("a", 1);
         * entry1.equals(entry2); // returns true
         *
         * ReusableEntry<String, Integer> entry3 = new ReusableEntry<>();
         * entry3.set("b", 2);
         * entry1.equals(entry3); // returns false
         *
         * entry1.equals(null); // returns false
         * }</pre>
         *
         * @param obj the object to be compared for equality with this entry
         * @return {@code true} if the specified object is equal to this entry, {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Entry<?, ?> another) {
                return N.equals(key, another.getKey()) && N.equals(value, another.getValue());
            }

            return false;
        }

        /**
         * Returns the hash code value for this map entry.
         * The hash code of a map entry {@code e} is defined to be:
         * {@code (e.getKey() == null ? 0 : e.getKey().hashCode()) ^ (e.getValue() == null ? 0 : e.getValue().hashCode())}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry = new ReusableEntry<>();
         * entry.set("a", 1);
         * int h = entry.hashCode(); // returns "a".hashCode() ^ Integer.valueOf(1).hashCode()
         *
         * ReusableEntry<String, Integer> entry2 = new ReusableEntry<>();
         * entry2.set(null, null);
         * int h2 = entry2.hashCode(); // returns 0 ^ 0 = 0
         * }</pre>
         *
         * @return the hash code value for this map entry
         */
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        /**
         * Returns a string representation of this entry in the form "key=value".
         * This method resets the internal flag to indicate the entry has been accessed.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ReusableEntry<String, Integer> entry = new ReusableEntry<>();
         * entry.set("name", 42);
         * String s = entry.toString(); // returns "name=42"
         *
         * ReusableEntry<String, Integer> entry2 = new ReusableEntry<>();
         * entry2.set(null, null);
         * String s2 = entry2.toString(); // returns "null=null"
         * }</pre>
         *
         * @return a string representation of this entry
         */
        @Override
        public String toString() {
            flag = false;
            return key + "=" + value;
        }
    }
}
