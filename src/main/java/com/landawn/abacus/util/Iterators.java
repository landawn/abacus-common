/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.Nullable;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * A comprehensive utility class providing an extensive collection of static methods for Iterator operations,
 * transformations, aggregations, and manipulations. This class serves as the primary iterator utility facade
 * in the Abacus library, offering performance-optimized, iterator-focused operations with null-safety and
 * functional programming patterns as core design principles.
 *
 * <p>The {@code Iterators} class is designed as a final utility class that provides a complete toolkit
 * for iterator processing including filtering, mapping, reducing, searching, sorting, and parallel
 * operations. Unlike collection-based utilities, this class focuses specifically on Iterator patterns
 * for memory-efficient, lazy evaluation of large datasets.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Iterator-Centric Design:</b> Optimized specifically for Iterator patterns and lazy evaluation</li>
 *   <li><b>Memory Efficient:</b> Minimal memory footprint with streaming operations. The exceptions are
 *       {@code distinct}/{@code distinctBy}, which retain every key seen, and the two {@code cycle(Iterable...)}
 *       methods, which snapshot a source that is not a {@link Collection}; each says so on its own javadoc</li>
 *   <li><b>Parallel Processing:</b> The {@code forEach} family can read from several iterators and invoke the
 *       element consumer concurrently, configured through {@link IterateOptions}. The worker pool is created
 *       and shut down internally; no method in this class accepts an {@link java.util.concurrent.Executor}.
 *       To run on the library's shared executor, or one you supply, use
 *       {@link N#forEachInParallel(Iterator, Throwables.Consumer, int)} instead</li>
 *   <li><b>Null-Safe Operations:</b> Graceful handling of {@code null} inputs and empty iterators</li>
 *   <li><b>Functional Programming:</b> Comprehensive support for map, filter, reduce, and functional patterns</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms with minimal object allocation</li>
 *   <li><b>Interoperability:</b> Every adapter returns a plain {@link java.util.Iterator} ({@link ObjIterator} or a
 *       primitive {@code XxxIterator}), so results can be handed straight to
 *       {@link com.landawn.abacus.util.stream.Stream#of(Iterator)}, {@link Seq#of(Iterator)} or any API that
 *       accepts an {@code Iterator}. This class itself neither accepts nor returns a
 *       {@link java.util.stream.Stream}</li>
 * </ul>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Access Operations:</b> {@code elementAt} with safe index handling</li>
 *   <li><b>Search Operations:</b> {@code indexOf}, {@code frequency}, {@code count} with predicate support</li>
 *   <li><b>Transformation Operations:</b> {@code map}, {@code flatMap}, {@code flatmap}, {@code filter}, {@code skipNulls}, {@code distinct}, {@code distinctBy}</li>
 *   <li><b>Slicing Operations:</b> {@code skip}, {@code limit}, {@code skipAndLimit}, {@code takeWhile}, {@code takeWhileInclusive}, {@code dropWhile}, {@code skipUntil}</li>
 *   <li><b>Repetition Operations:</b> {@code repeat}, {@code repeatElements}, {@code cycle}, {@code cycleToSize}</li>
 *   <li><b>Parallel Operations:</b> {@code forEach} with multi-threaded reading/processing support</li>
 *   <li><b>Combination Operations:</b> {@code concat}, {@code merge}, {@code mergeSorted}, {@code zip}, {@code unzip} for iterator composition</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Iterator First:</b> Methods are designed to work with Iterator types as primary input,
 *       promoting memory-efficient streaming operations over collection materialization</li>
 *   <li><b>Lazy Evaluation:</b> Operations are performed lazily when possible, allowing for efficient
 *       processing of large datasets without excessive memory consumption. Note that the {@code Iterable}-accepting
 *       overloads call {@link Iterable#iterator()} <i>eagerly</i>, when the factory method is invoked, rather than
 *       on the first {@code hasNext()}/{@code next()}; the elements themselves are still pulled on demand. The
 *       multi-source forms {@code concat(Iterable...)}, {@link #concatIterables(Collection)} and
 *       {@code concat(Map...)} are the exception: each source's iterator is obtained only once the previous
 *       source has been exhausted. {@link #repeatElements(Iterable, long)} goes the other way and additionally
 *       calls {@code hasNext()} on the iterator at construction to detect an empty source, so a source backed by
 *       I/O performs one read before the caller asks for anything; {@link #cycle(Iterable)} probes the same way,
 *       and so does {@link #cycle(Iterable, long)} for a source that is not a {@link Collection}</li>
 *   <li><b>Consuming Operations:</b> Methods do not call {@link Iterator#remove()}, but they advance
 *       and therefore consume the supplied iterators</li>
 *   <li><b>Exception Avoidance:</b> Methods avoid throwing unnecessary exceptions when contracts
 *       are not violated, preferring empty results over exceptions for edge cases</li>
 *   <li><b>Nullable Returns:</b> {@link #elementAt(Iterator, long)} returns a {@link Nullable} rather than
 *       throwing on an out-of-bounds index; it is the only method here that returns {@code Nullable}</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic iterator access operations
 * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
 * Nullable<String> element = Iterators.elementAt(iter, 2);     // Nullable[C]
 *
 * // Search operations
 * Iterator<String> letters = Arrays.asList("A", "B", "C", "B", "D").iterator();
 * long index = Iterators.indexOf(letters, "B");                // returns 1
 *
 * // Counting operations
 * Iterator<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5).iterator();
 * long evenCount = Iterators.count(numbers, n -> n % 2 == 0);  // returns 2
 *
 * // Transformation operations
 * Iterator<String> words = Arrays.asList("hello", "world", "java").iterator();
 * ObjIterator<Integer> lengths = Iterators.map(words, String::length);              // [5, 5, 4]
 * ObjIterator<String> filtered = Iterators.filter(
 *     Arrays.asList("hello", "world", "java").iterator(), s -> s.length() > 4);     // [hello, world]
 *
 * // Combination operations
 * Iterator<Integer> a = Arrays.asList(1, 3, 5).iterator();
 * Iterator<Integer> b = Arrays.asList(2, 4, 6).iterator();
 * ObjIterator<Integer> merged = Iterators.mergeSorted(a, b);   // 1, 2, 3, 4, 5, 6
 *
 * // Parallel forEach processing of a collection of iterators
 * List<Iterator<String>> data = getDataIterators();
 * Iterators.forEach(data, IterateOptions.builder().readThreads(2).processThreads(4).queueSize(100).build(),
 *     item -> processItem(item));
 * }</pre>
 *
 * <p><b>Iterator Access Patterns:</b>
 * <ul>
 *   <li><b>Index-Based:</b> {@code elementAt()} with bounds checking and {@code Nullable} returns</li>
 *   <li><b>Position Finding:</b> {@code indexOf()} with optional start index</li>
 *   <li><b>Counting:</b> {@code count()}, {@code frequency()} with optional predicate support</li>
 *   <li><b>Comparison:</b> {@code equalsInOrder()} for ordered element equality</li>
 * </ul>
 *
 * <p><b>Functional Transformations:</b>
 * <ul>
 *   <li><b>Mapping:</b> {@code map()}, {@code flatMap()}, {@code flatmap()}</li>
 *   <li><b>Filtering:</b> {@code filter()}, {@code skipNulls()}, {@code distinct()}, {@code distinctBy()}, {@code limit()}</li>
 *   <li><b>Slicing:</b> {@code skip()}, {@code skipAndLimit()}, {@code takeWhile()}, {@code takeWhileInclusive()}, {@code dropWhile()}, {@code skipUntil()}</li>
 *   <li><b>Composition:</b> {@code concat()}, {@code merge()}, {@code mergeSorted()}, {@code zip()}</li>
 * </ul>
 *
 * <p><b>Parallel Processing Support:</b> configured entirely through {@link IterateOptions}.
 * <ul>
 *   <li><b>{@code readThreads}:</b> reads the supplied iterators concurrently. Only the
 *       {@code Collection<Iterator>} overloads honour it. Single-iterator overloads ignore it; processing workers
 *       serialize source reads when {@code processThreads > 0}. With more than one reader the iterators interleave nondeterministically, so
 *       {@code offset}/{@code count} then select an unstable subset</li>
 *   <li><b>{@code processThreads}:</b> invokes the element consumer concurrently on a pool of named daemon
 *       threads created and shut down by the call. The order of consumer invocations is not guaranteed</li>
 *   <li><b>{@code queueSize}:</b> bounds the hand-off buffer between the reader threads and the consumer. It has
 *       no effect unless {@code readThreads > 0}; {@code 0} lets the implementation pick a size</li>
 *   <li><b>Exception Handling:</b> the first failure cancels the remaining work; later failures are attached to it
 *       with {@link Throwable#addSuppressed(Throwable)}. The first failure is then rethrown <i>as it is</i>, so
 *       the declared {@code throws E}/{@code throws E2} holds whatever {@code processThreads} is set to -
 *       a tuning change never moves an exception out of the {@code catch} clause that was matching it</li>
 * </ul>
 *
 * <p><b>Thread Safety and Resource Ownership:</b>
 * <ul>
 *   <li><b>No Shared State:</b> The utility methods retain no caller data between invocations and the class has
 *       no mutable static fields</li>
 *   <li><b>Single-use Results:</b> Adapter methods return new iterators that consume their input iterators;
 *       they are not thread-safe and should be used by one thread at a time</li>
 *   <li><b>Coordinated Parallel Consumption:</b> The parallel {@code forEach} overloads are the exception - they
 *       coordinate access to their own combined iterator internally</li>
 *   <li><b>Resource Ownership:</b> Plain iterators have no close contract; callers remain responsible
 *       for closing any stream, reader, or other resource from which an iterator was obtained. Note that
 *       {@code forEach} reads source iterators on pool threads when either reading or processing is parallel,
 *       so a source bound to the calling thread requires {@code readThreads == 0} and {@code processThreads == 0}</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code Nullable<T> result = Iterators.elementAt(iterator, index);}</li>
 *   <li><b>Parallel Processing:</b> {@code Iterators.forEach(iterators, IterateOptions.builder().offset(o).count(c).readThreads(r).processThreads(p).queueSize(q).build(), processor);}</li>
 *   <li><b>Functional Pipeline:</b> {@code ObjIterator<R> result = Iterators.map(Iterators.filter(iter, pred), mapper);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.util.Iterables}:</b> Iterable-focused utility operations</li>
 *   <li><b>{@link com.landawn.abacus.util.ObjIterator}:</b> Enhanced iterator implementations</li>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with collection operations</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Stream-based processing utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Enumerations}:</b> Enumeration utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Array}:</b> Array manipulation utilities</li>
 *   <li><b>{@link java.util.Iterator}:</b> Core Java iterator interface</li>
 *   <li><b>{@link java.util.stream.Stream}:</b> Java 8+ Stream API</li>
 * </ul>
 *
 * <p><b>Usage Examples: Large Dataset Processing</b></p>
 * <pre>{@code
 * // Processing large datasets with memory efficiency
 * Iterator<String> largeDataset = getMillionRecordIterator();
 *
 * // Memory-efficient streaming transformation (lazy)
 * ObjIterator<ProcessedRecord> processed = Iterators.map(largeDataset, this::transform);
 *
 * // Filter without loading all data into memory (lazy)
 * ObjIterator<ProcessedRecord> filtered = Iterators.filter(processed, ProcessedRecord::isValid);
 *
 * // Count matching records by consuming the iterator
 * long validCount = Iterators.count(filtered, r -> r.getScore() > 0);
 * }</pre>
 *
 * <p><b>Usage Examples: Data Pipeline with Functional Operations</b></p>
 * <pre>{@code
 * // Building a data processing pipeline
 * Iterator<RawData> source = dataSource.iterator();
 *
 * // Multi-stage transformation pipeline (all stages are lazy)
 * ObjIterator<String> stage1 = Iterators.map(source, this::extractText);
 * ObjIterator<String> stage2 = Iterators.filter(stage1, text -> !text.isEmpty());
 * ObjIterator<String> stage3 = Iterators.map(stage2, String::trim);
 * ObjIterator<String> stage4 = Iterators.distinct(stage3);
 *
 * // Consume the pipeline with a single terminal operation (an iterator pipeline can only be consumed once)
 * Iterators.forEach(stage4, this::process);
 *
 * // Alternatively (instead of forEach): long totalCount = Iterators.count(stage4);
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other open source
 * projects under the Apache License 2.0. Methods from these libraries may have been modified for
 * consistency, performance optimization, and enhanced iterator-specific functionality within the
 * Abacus framework.</p>
 *
 * <p><b>{@code Iterators} vs. related APIs:</b> {@code Iterators} is primarily a factory/adapter toolkit for lazy
 * {@link java.util.Iterator}s; it also hosts a small set of Guava-style <i>eager</i> iterator utilities
 * ({@code elementAt}, {@code frequency}, {@code count}, {@code indexOf}, {@code equalsInOrder}, {@code advance})
 * and the (eager) parallel {@code forEach} family. For other eager results or a chainable pipeline, pick a sibling instead.</p>
 * <table border="1">
 *   <caption>When to use Iterators versus Iterables, N, and Stream</caption>
 *   <tr>
 *     <th>API</th>
 *     <th>Focus</th>
 *     <th>Evaluation</th>
 *     <th>Use when</th>
 *   </tr>
 *   <tr>
 *     <td>{@code Iterators}</td>
 *     <td>factory &amp; adapter methods returning {@link java.util.Iterator}/{@link ObjIterator}</td>
 *     <td>lazy — elements are pulled on demand (except the Guava-style eager helpers and {@code forEach} noted above)</td>
 *     <td>composing iteration: {@code concat}, {@code merge}, {@code skip}, {@code limit}, {@code filter}, {@code map}, {@code cycle}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Iterables}</td>
 *     <td>aggregate operations over an {@link Iterable}/{@code Collection}</td>
 *     <td>eager — returns values/collections</td>
 *     <td>computing a result ({@code min}/{@code max}/{@code sum}, {@code indexOf}, set operations) over an existing collection</td>
 *   </tr>
 *   <tr>
 *     <td>{@link N}</td>
 *     <td>general array/collection utilities</td>
 *     <td>eager</td>
 *     <td>broadly-applicable, one-shot operations</td>
 *   </tr>
 *   <tr>
 *     <td>{@link com.landawn.abacus.util.stream.Stream Stream} / {@link Seq}</td>
 *     <td>chainable functional pipeline</td>
 *     <td>lazy</td>
 *     <td>multi-step transformations ending in a terminal collect/reduce</td>
 *   </tr>
 * </table>
 *
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.util.Enumerations
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.Numbers
 * @see com.landawn.abacus.util.u.Nullable
 * @see java.util.Iterator
 * @see java.util.stream.Stream
 * @see java.lang.Iterable
 */
public final class Iterators {

    private static final Logger logger = LoggerFactory.getLogger(Iterators.class);

    /**
     * How long the parallel {@code forEach} waits for its workers to stop after the calling thread has been
     * interrupted, before it closes the source and rethrows. Bounded so that a consumer which ignores
     * interruption cannot pin the caller here indefinitely.
     */
    private static final long CANCELLATION_TIMEOUT_IN_MILLIS = 1000;

    private Iterators() {
        // Utility class.
    }

    /**
     * Retrieves the element at the specified position in the given iterator.
     * The method will advance the iterator to the specified index and return the element at that position wrapped in a {@code Nullable}.
     * If the index is out of bounds (greater than or equal to the number of elements in the iterator), a {@code Nullable.empty()} is returned.
     *
     * <p><b>Note:</b> this is the lenient counterpart of {@link N#getElement(Iterator, long)}, which performs the same
     * positional access but throws {@code IndexOutOfBoundsException} when the index is out of bounds.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
     * Nullable<String> result = Iterators.elementAt(iter, 2);
     * // result.get() => "C"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();
     * Nullable<Integer> result2 = Iterators.elementAt(iter2, 10);
     * // result2.isPresent() => false
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator from which to retrieve the element, or {@code null} to return {@code Nullable.empty()}.
     * @param index the position in the iterator of the element to be returned. Indexing starts from 0.
     * @return a {@code Nullable} containing the element at the specified position in the iterator, or {@code Nullable.empty()} if the index is out of bounds.
     * @throws IllegalArgumentException if {@code index} is negative.
     * @see N#getElement(Iterator, long)
     */
    public static <T> Nullable<T> elementAt(final Iterator<? extends T> iter, long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        if (iter == null) {
            return Nullable.empty();
        }

        while (iter.hasNext()) {
            if (index == 0) {
                return Nullable.of(iter.next());
            } else {
                iter.next();
                index--;
            }
        }

        return Nullable.empty();
    }

    /**
     * Counts the occurrences of a specific value in the given iterator.
     *
     * <p><b>Note:</b> {@code frequency()} counts occurrences of a specific <b>value</b> using equality comparison,
     * while {@link #count(Iterator, Predicate)} counts elements matching a <b>predicate</b>.
     * Use {@code frequency()} when you know the exact value to search for;
     * use {@code count()} when you need a custom matching condition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "A", "C", "A").iterator();
     * long count = Iterators.frequency(iter, "A");
     * // count => 3
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, null, 2, null, 3).iterator();
     * long nullCount = Iterators.frequency(iter2, null);
     * // nullCount => 2
     * }</pre>
     *
     * <p><b>Comparison:</b> elements are compared with {@link java.util.Objects#equals(Object, Object)}, so array
     * elements are matched by <i>identity</i>, not by content:
     * {@code Iterators.frequency(ObjIterator.of(new int[][] { { 1, 2 } }), new int[] { 1, 2 })} returns {@code 0},
     * while passing the <i>same</i> {@code int[]} reference as both the element and {@code valueToFind} returns
     * {@code 1}. Use {@link N#deepEquals(Object, Object)} through {@link #count(Iterator, Predicate)} when
     * array contents should match.</p>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code 0}.
     * @param valueToFind the value to count occurrences of, or {@code null} to count {@code null} occurrences.
     * @return the number of occurrences of the value in the iterator, or {@code 0} if {@code iter} is {@code null}.
     * @see N#frequency(Iterator, Object)
     * @see #count(Iterator, Predicate)
     */
    public static long frequency(final Iterator<?> iter, final Object valueToFind) {
        if (iter == null) {
            return 0;
        }

        long occurrences = 0;

        if (valueToFind == null) {
            while (iter.hasNext()) {
                if (iter.next() == null) {
                    occurrences++;
                }
            }
        } else {
            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    occurrences++;
                }
            }
        }

        return occurrences;
    }

    /**
     * Counts the number of elements in the given iterator.
     *
     * <p><b>Note:</b> {@code count()} counts elements (optionally matching a <b>predicate</b>),
     * while {@link #frequency(Iterator, Object)} counts occurrences of a specific <b>value</b> using equality comparison.
     * Use {@code count()} when counting all elements or when you need flexible matching logic;
     * use {@code frequency()} when you know the exact value to search for.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
     * long count = Iterators.count(iter);
     * // count => 4
     *
     * Iterator<Integer> emptyIter = Collections.emptyIterator();
     * long emptyCount = Iterators.count(emptyIter);
     * // emptyCount => 0
     * }</pre>
     *
     * @param iter the iterator to be counted, or {@code null} to return {@code 0}.
     * @return the number of elements in the iterator, or {@code 0} if {@code iter} is {@code null}.
     * @see N#count(Iterator)
     * @see #count(Iterator, Predicate)
     * @see #frequency(Iterator, Object)
     */
    public static long count(final Iterator<?> iter) {
        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            iter.next();
            res++;
        }

        return res;
    }

    /**
     * Counts the number of elements in the given iterator that match the provided predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6).iterator();
     * long evenCount = Iterators.count(iter, n -> n % 2 == 0);
     * // evenCount => 3
     *
     * Iterator<String> iter2 = Arrays.asList("apple", "apricot", "banana", "avocado").iterator();
     * long aCount = Iterators.count(iter2, s -> s.startsWith("a"));
     * // aCount => 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator to be searched, or {@code null} to return {@code 0}.
     * @param predicate the predicate to apply to each element in the iterator.
     * @return the number of elements in the iterator that match the provided predicate, or {@code 0} if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#count(Iterator, Predicate)
     */
    public static <T> long count(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            if (predicate.test(iter.next())) {
                res++;
            }
        }

        return res;
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator.
     * This method starts searching from the beginning of the iterator.
     *
     * <p><b>Note on return conventions:</b> this method returns a {@code long} with {@code -1} as the not-found sentinel
     * ({@code long} because an iterator may yield more than {@code Integer.MAX_VALUE} elements);
     * {@link Iterables#indexOf(Collection, Object)} returns an {@code OptionalInt} that is empty when not found;
     * {@link N#indexOf(Iterator, Object)} returns an {@code int} with the same {@code -1} sentinel.</p>
     *
     * <p><b>Comparison:</b> elements are compared with {@link java.util.Objects#equals(Object, Object)}, so array
     * elements are matched by <i>identity</i>, not by content.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "B", "D").iterator();
     * long index = Iterators.indexOf(iter, "B");
     * // index => 1
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * long notFound = Iterators.indexOf(iter2, "Z");
     * // notFound => -1
     * }</pre>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code -1}.
     * @param valueToFind the value to find in the iterator, or {@code null} to find {@code null} values.
     * @return the index of the first occurrence of the specified value in the iterator, or {@code -1} if the value is not found or {@code iter} is {@code null}.
     * @see Iterables#indexOf(Collection, Object)
     * @see N#indexOf(Iterator, Object)
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind) {
        return indexOf(iter, valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator,
     * starting the search from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "B", "D").iterator();
     * long index = Iterators.indexOf(iter, "B", 2);
     * // index => 3 (finds second occurrence of "B")
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * long notFound = Iterators.indexOf(iter2, "A", 1);
     * // notFound => -1 (skips first element)
     * }</pre>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code -1}.
     * @param valueToFind the value to find in the iterator, or {@code null} to find {@code null} values.
     * @param fromIndex the index to start the search from; a negative value is treated as {@code 0}.
     * @return the index of the first occurrence of the specified value in the iterator, or {@code -1} if the value is not found or {@code iter} is {@code null}.
     * @see Iterables#indexOf(Collection, Object)
     * @see N#indexOf(Iterator, Object, int)
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind, final long fromIndex) {
        if (iter == null) {
            return N.INDEX_NOT_FOUND;
        }

        final long startIndex = N.max(fromIndex, 0);
        long index = 0;

        if (startIndex > 0) {
            while (index < startIndex && iter.hasNext()) {
                iter.next();
                index++;
            }
        }

        while (iter.hasNext()) {
            if (N.equals(iter.next(), valueToFind)) {
                return index;
            }

            index++;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Determines whether two iterators contain equal elements in the same order. More specifically,
     * this method returns {@code true} if {@code iterator1} and {@code iterator2} contain the same
     * number of elements and every element of {@code iterator1} is equal to the corresponding element
     * of {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have been advanced some
     * number of elements forward.
     *
     * <p><b>Comparison:</b> corresponding elements are compared with
     * {@link java.util.Objects#equals(Object, Object)}, so array elements are compared by <i>identity</i>, not by
     * content: two iterators each yielding an equal-but-distinct {@code int[]} are <b>not</b> equal in order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = Arrays.asList("A", "B", "C").iterator();
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * boolean equal = Iterators.equalsInOrder(iter1, iter2);
     * // equal => true
     *
     * Iterator<Integer> iter3 = Arrays.asList(1, 2, 3).iterator();
     * Iterator<Integer> iter4 = Arrays.asList(1, 2, 4).iterator();
     * boolean notEqual = Iterators.equalsInOrder(iter3, iter4);
     * // notEqual => false
     * }</pre>
     *
     * @param iterator1 the first iterator to compare, or {@code null} which is treated as empty.
     * @param iterator2 the second iterator to compare, or {@code null} which is treated as empty.
     * @return {@code true} if the iterators contain equal elements in the same order, {@code false} otherwise. Two {@code null} or empty iterators are considered equal.
     */
    public static boolean equalsInOrder(final Iterator<?> iterator1, final Iterator<?> iterator2) {
        final boolean isIterator1Empty = N.isEmpty(iterator1);
        final boolean isIterator2Empty = N.isEmpty(iterator2);

        if (isIterator1Empty && isIterator2Empty) {
            return true;
        }

        if (isIterator1Empty || isIterator2Empty) {
            return false;
        }

        while (iterator1.hasNext()) {
            if (!iterator2.hasNext() || !N.equals(iterator1.next(), iterator2.next())) {
                return false;
            }
        }

        return !iterator2.hasNext();
    }

    /**
     * Creates an iterator that returns the same element a specified number of times.
     * This method is useful for generating a sequence of identical elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.repeat("Hello", 3);
     * // Yields: "Hello", "Hello", "Hello"
     *
     * ObjIterator<Integer> numbers = Iterators.repeat(5, 0);
     * // numbers.hasNext() => false (empty iterator)
     * }</pre>
     *
     * @param <T> the type of the element to repeat.
     * @param e the element to repeat (can be {@code null}).
     * @param n the number of times to repeat the element. Must be non-negative.
     * @return an {@code ObjIterator} that returns the element {@code n} times, or an empty iterator if {@code n} is {@code 0}.
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #repeat(Object, long)
     * @see #cycle(Object...)
     */
    public static <T> ObjIterator<T> repeat(final T e, final int n) throws IllegalArgumentException {
        // Behaviourally identical to the long overload - kept as its own public method because it has its own
        // JVM descriptor (Object, int) that compiled callers are already bound to, but with no duplicated body.
        return repeat(e, (long) n);
    }

    /**
     * Creates an iterator that returns the same element a specified number of times (long version).
     * This method is similar to {@link #repeat(Object, int)} but supports a larger number of repetitions using {@code long}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.repeat("Hello", 1000000L);
     * // Yields "Hello" one million times
     *
     * ObjIterator<Integer> iter2 = Iterators.repeat(42, 5L);
     * // Yields: 42, 42, 42, 42, 42
     * }</pre>
     *
     * @param <T> the type of the element to repeat.
     * @param e the element to repeat (can be {@code null}).
     * @param n the number of times to repeat the element. Must be non-negative.
     * @return an {@code ObjIterator} that returns the element {@code n} times, or an empty iterator if {@code n} is {@code 0}.
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #repeat(Object, int)
     * @see #cycle(Object...)
     */
    public static <T> ObjIterator<T> repeat(final T e, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (cnt <= 0) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     * Repeats each element in the specified iterable {@code n} times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.repeatElements(list, 2);
     * // Yields: "A", "A", "B", "B", "C", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, 2);
     * ObjIterator<Integer> iter2 = Iterators.repeatElements(numbers, 3);
     * // Yields: 1, 1, 1, 2, 2, 2
     * }</pre>
     *
     * <p><b>Note:</b> {@code c.iterator()} is obtained eagerly, when this method is called, and {@code hasNext()} is
     * called on it once to detect an empty source - so a source backed by I/O performs one read before the caller
     * pulls anything.</p>
     *
     * @param <T> the type of elements in the iterable.
     * @param c the iterable whose elements are to be repeated, or {@code null}/empty to return an empty iterator.
     * @param n the number of times each element is to be repeated. Must be non-negative.
     * @return an {@code ObjIterator} over the elements in the iterable, each repeated {@code n} times, or an empty iterator if {@code c} is {@code null}/empty or {@code n} is {@code 0}.
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #cycle(Object...)
     * @see #cycle(Iterable)
     * @see #repeatElementsToSize(Collection, long)
     * @see N#repeatElements(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElements(final Iterable<? extends T> c, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0 || N.isEmptyCollection(c)) {
            return ObjIterator.empty();
        }

        final Iterator<? extends T> iter = c.iterator();

        if (!iter.hasNext()) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private T next = null;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt > 0 || iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cnt <= 0) {
                    next = iter.next();
                    cnt = n;
                }

                cnt--;

                return next;
            }
        };
    }

    /**
     * Repeats each element in the specified Collection a calculated number of times until the specified total size is reached.
     * Elements are repeated in order, with some elements potentially repeated more times than others to reach exactly the target size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.repeatElementsToSize(list, 7);
     * // Yields: "A", "A", "A", "B", "B", "C", "C"
     * // Each element repeated at least twice, with "A" repeated three times
     *
     * List<Integer> numbers = Arrays.asList(1, 2);
     * ObjIterator<Integer> iter2 = Iterators.repeatElementsToSize(numbers, 5);
     * // Yields: 1, 1, 1, 2, 2
     * }</pre>
     *
     * <p><b>Live view:</b> {@code c.size()} is read when this method is called, to work out how many times each
     * element must be repeated, but the elements themselves are pulled lazily from
     * {@link Collection#iterator() c.iterator()}, which is obtained on the first call to {@code next()}. A
     * modification made <i>before</i> that first {@code next()} raises nothing - the iterator is created after
     * it - but the per-element repeat counts were already fixed from the original {@code c.size()}: a source
     * that has grown is still truncated to {@code size}, and one that has shrunk produces fewer than
     * {@code size} elements, with {@code hasNext()} simply reporting {@code false}. If {@code c} has been
     * emptied, the first {@code next()} throws {@link NoSuchElementException} even though {@code hasNext()}
     * reported {@code true}. A structural modification made <i>after</i> the first {@code next()} is governed
     * by that collection's own iterator contract: a fail-fast collection raises
     * {@link java.util.ConcurrentModificationException}.</p>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection whose elements are to be repeated. Must not be empty or {@code null} if {@code size > 0}.
     * @param size the total number of elements the resulting iterator should produce. Must be non-negative.
     * @return an {@code ObjIterator} that repeats each element until {@code size} elements have been produced.
     * @throws IllegalArgumentException if {@code size} is negative, or if {@code c} is empty or {@code null} when
     *         {@code size > 0}.
     * @see #repeatElements(Iterable, long)
     * @see #cycleToSize(Collection, long)
     * @see N#repeatElementsToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElementsToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgNotNegative(size, cs.size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection cannot be empty or null when size > 0");

        if (size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final long n = size / c.size();
            private long mod = size % c.size();

            private Iterator<? extends T> iter = null;
            private T next = null;
            private long cnt = mod-- > 0 ? n + 1 : n;

            // The per-element repeat counts above are fixed from c.size() when this iterator is constructed, but
            // c.iterator() is only taken on the first next(). A source that GREW in between therefore offers more
            // elements than those counts were divided among, and nothing else here bounds the total - the
            // requested size is the one guarantee this method makes, so it is tracked explicitly. (A source that
            // SHRANK is caught separately, by nextElement().)
            private long remaining = size;

            @Override
            public boolean hasNext() {
                return remaining > 0 && (cnt > 0 || ((n > 0 || mod > 0) && (iter != null && iter.hasNext())));
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = c.iterator();
                    next = nextElement();
                } else if (cnt <= 0) {
                    next = nextElement();
                    cnt = mod-- > 0 ? n + 1 : n;
                }

                cnt--;
                remaining--;

                return next;
            }

            /**
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            private T nextElement() throws NoSuchElementException {
                if (!iter.hasNext()) {
                    // The source shrank after this iterator was created, so the per-element repeat counts
                    // computed from its original size can no longer add up to the requested total.
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the provided elements.
     * However, if the provided elements are empty, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.cycle("A", "B", "C");
     * // Yields: "A", "B", "C", "A", "B", "C", "A", "B", "C", ... (infinitely)
     *
     * ObjIterator<Integer> numbers = Iterators.cycle(1, 2);
     * // Yields: 1, 2, 1, 2, 1, 2, ... (infinitely)
     * }</pre>
     *
     * <p><b>Live view vs. snapshot:</b> unlike {@link #cycle(Iterable)} over a {@link Collection}, the varargs array
     * is <b>snapshotted</b> - it is copied when this method is called, so later writes to the caller's array are not
     * observed. The element references themselves are shared, so mutating an element object still is.</p>
     *
     * @param <T> the type of elements in the array.
     * @param elements the array whose elements are to be cycled over.
     * @return an infinite iterator cycling over the elements of the array, or an empty iterator if {@code elements} is {@code null} or empty.
     * @see #repeat(Object, int)
     * @see #repeat(Object, long)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> cycle(final T... elements) {
        if (N.isEmpty(elements)) {
            return ObjIterator.empty();
        }

        final T[] a = elements.clone();
        final int len = a.length;

        return new ObjIterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @SuppressFBWarnings("IT_NO_SUCH_ELEMENT")
            @Override
            public T next() { // NOSONAR
                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the elements of the provided iterable.
     * However, if the provided elements are empty, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.cycle(list);
     * // Yields: "A", "B", "C", "A", "B", "C", ... (infinitely)
     *
     * Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
     * ObjIterator<Integer> numbers = Iterators.cycle(set);
     * // Yields: 1, 2, 3, 1, 2, 3, ... (infinitely, in set iteration order)
     * }</pre>
     *
     * <p><b>Live view vs. snapshot:</b> a {@link Collection} is cycled <i>in place</i> - the returned iterator
     * simply calls {@link Collection#iterator()} again once the current one is exhausted. It therefore uses no
     * extra memory and observes later changes to the source, exactly like {@link #cycleToSize(Collection, long)}.
     * Structural modification of the source is governed by that collection's own iterator contract: a fail-fast
     * collection raises {@link java.util.ConcurrentModificationException}, while one whose iterator does not fail
     * fast simply picks up the change on the next round - and, if the source has been emptied, ends the cycle by
     * reporting {@code hasNext() == false} rather than looping forever. Any other {@code Iterable} cannot be assumed to
     * be re-iterable, so the first round is read through to the source and <b>snapshotted</b>: every element is
     * retained for the lifetime of the returned iterator and later changes to the source are not visible.
     * Do not call this on a non-{@code Collection} {@code Iterable} of unbounded size.</p>
     *
     * <p><b>Note:</b> {@code iterable.iterator()} is obtained eagerly, when this method is called, in order to
     * detect an empty source.</p>
     *
     * @param <T> the type of elements in the iterable.
     * @param iterable the iterable whose elements are to be cycled over, or {@code null} to return an empty iterator.
     * @return an infinite iterator cycling over the elements of the iterable, or an empty iterator if {@code iterable} is {@code null} or empty.
     * @see #cycle(Object...)
     * @see #cycle(Iterable, long)
     * @see #cycleToSize(Collection, long)
     * @see #repeatElements(Iterable, long)
     * @see N#cycle(Collection, int)
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable) {
        if (N.isEmptyCollection(iterable)) {
            return ObjIterator.empty();
        }

        if (iterable instanceof Collection) {
            // A Collection can be re-iterated, so cycle it in place rather than copying it into a private
            // array: O(1) memory, and consistent with cycleToSize(Collection, long), which does the same.
            final Collection<? extends T> c = (Collection<? extends T>) iterable;
            final Iterator<? extends T> firstRound = c.iterator();

            if (!firstRound.hasNext()) {
                // size() said non-empty but the iterator is empty: a concurrent collection can be drained
                // between the two calls. Report empty rather than an iterator whose next() always throws.
                return ObjIterator.empty();
            }

            return new ObjIterator<>() {
                private Iterator<? extends T> iter = firstRound;

                @Override
                public boolean hasNext() {
                    if (iter.hasNext()) {
                        return true;
                    }

                    iter = c.iterator();

                    // A fresh iterator with no elements means the source has been emptied since this iterator was
                    // created. Report that honestly instead of promising an element next() could not supply: a
                    // constant true here made while (it.hasNext()) it.next() throw NoSuchElementException.
                    return iter.hasNext();
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if no next element is available from the source iteration.
                 */
                @Override
                public T next() throws NoSuchElementException {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return iter.next();
                }
            };
        }

        final Iterator<? extends T> iter = iterable.iterator();

        if (!iter.hasNext()) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private List<T> list = new ArrayList<>();
            private T[] a;
            private int len;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                if (a == null) {
                    if (iter.hasNext()) {
                        final T e = iter.next();
                        list.add(e);
                        return e;
                    } else {
                        a = list.toArray((T[]) new Object[list.size()]);
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an iterator that cycles over the elements of the provided iterable for a specified number of rounds.
     * If the provided iterable is empty, an empty iterator will be returned.
     * If the number of rounds is zero, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.cycle(list, 2);
     * // Yields: "A", "B", "C", "A", "B", "C"
     *
     * Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
     * ObjIterator<Integer> numbers = Iterators.cycle(set, 3);
     * // Yields: 1, 2, 1, 2, 1, 2
     * }</pre>
     *
     * <p><b>Live view vs. snapshot:</b> a {@link Collection} is cycled <i>in place</i> - the returned iterator
     * simply calls {@link Collection#iterator()} again at the start of every round. It therefore uses no extra
     * memory and observes later changes to the source. Structural modification of the source is governed by that
     * collection's own iterator contract: a fail-fast collection raises
     * {@link java.util.ConcurrentModificationException}, while one whose iterator does not fail fast picks up the
     * change on the next round - and, if the source has been emptied, ends the iteration early instead of walking
     * the remaining rounds. Any other {@code Iterable} cannot be assumed to be re-iterable, so the first round is
     * read through to the source and <b>snapshotted</b>: every element is retained for the lifetime of the
     * returned iterator and later changes to the source are not visible.</p>
     *
     * <p><b>Note:</b> {@code iterable.iterator()} is obtained eagerly, when this method is called, in order to
     * detect an empty source.</p>
     *
     * @param <T> the type of elements in the iterable.
     * @param iterable the iterable whose elements are to be cycled over, or {@code null} to return an empty iterator.
     * @param rounds the number of times to cycle over the iterable's elements. Must be non-negative.
     * @return an {@code ObjIterator} cycling over the elements of the iterable for the specified number of rounds, or an empty iterator if {@code iterable} is {@code null}/empty or {@code rounds} is {@code 0}.
     * @throws IllegalArgumentException if {@code rounds} is negative.
     * @see #cycle(Object...)
     * @see #cycle(Iterable)
     * @see #cycleToSize(Collection, long)
     * @see N#cycle(Collection, int)
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable, final long rounds) throws IllegalArgumentException {
        N.checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0 || N.isEmptyCollection(iterable)) {
            return ObjIterator.empty();
        }

        if (iterable instanceof Collection) {
            // A Collection can be re-iterated, so cycle it in place rather than copying it into a private
            // array: O(1) memory, and consistent with cycleToSize(Collection, long), which does the same.
            final Collection<? extends T> c = (Collection<? extends T>) iterable;

            if (rounds == 1) {
                return ObjIterator.of(c.iterator());
            }

            return new ObjIterator<>() {
                private Iterator<? extends T> iter = c.iterator();
                private long round = 1;

                // hasNext() has to stay a pure query. Without this latch, every call made while the source is
                // momentarily empty would consume one of the requested rounds, so the number of elements this
                // iterator yields would depend on how often hasNext() was called.
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    if (done) {
                        return false;
                    }

                    if (iter.hasNext()) {
                        return true;
                    }

                    if (round >= rounds) {
                        done = true;
                        return false;
                    }

                    round++;
                    iter = c.iterator();

                    if (!iter.hasNext()) {
                        // A fresh iterator with no elements means the source has been emptied since this
                        // iterator was created. No later round can produce anything either, so end the
                        // iteration here, as documented, rather than spinning through the remaining rounds.
                        done = true;
                        return false;
                    }

                    return true;
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if no next element is available from the source iteration.
                 */
                @Override
                public T next() throws NoSuchElementException {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return iter.next();
                }
            };
        }

        final Iterator<? extends T> iter = iterable.iterator();

        if (!iter.hasNext()) {
            return ObjIterator.empty();
        } else if (rounds == 1) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private List<T> list = new ArrayList<>();
            private T[] a;
            private int len;
            private long m = 1;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return m < rounds || (m == rounds && cursor < len);
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (a == null) {
                    if (iter.hasNext()) {
                        final T e = iter.next();
                        list.add(e);
                        return e;
                    } else {
                        m++;
                        a = list.toArray((T[]) new Object[list.size()]);
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    m++;
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Repeats the entire specified Collection cyclically until the specified total size is reached.
     * The collection is repeated as a whole, cycling through it multiple times if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B");
     * ObjIterator<String> iter = Iterators.cycleToSize(list, 5);
     * // Yields: "A", "B", "A", "B", "A"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3);
     * ObjIterator<Integer> iter2 = Iterators.cycleToSize(numbers, 7);
     * // Yields: 1, 2, 3, 1, 2, 3, 1
     * }</pre>
     *
     * <p><b>Live view:</b> the collection is cycled <i>in place</i> - {@link Collection#iterator()} is called again
     * at the start of every round - so no copy is made and later changes to the source are observed. Structural
     * modification of the source is governed by that collection's own iterator contract: a fail-fast collection
     * raises {@link java.util.ConcurrentModificationException}. If the source is emptied and its iterator does not
     * fail fast, the next {@code next()} throws {@link NoSuchElementException} even though {@code hasNext()}
     * reported {@code true}, because the requested {@code size} can no longer be produced.</p>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection to be repeated. Must not be empty or {@code null} if {@code size > 0}.
     * @param size the total number of elements the resulting iterator should produce. Must be non-negative.
     * @return an {@code ObjIterator} that cycles through the collection until {@code size} elements have been produced.
     * @throws IllegalArgumentException if {@code size} is negative, or if {@code c} is empty or {@code null} when
     *         {@code size > 0}.
     * @see #cycle(Iterable)
     * @see #cycle(Iterable, long)
     * @see #repeatElementsToSize(Collection, long)
     * @see N#cycleToSize(Collection, int)
     */
    public static <T> ObjIterator<T> cycleToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgNotNegative(size, cs.size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection cannot be empty or null when size > 0");

        if (size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private long cnt = size;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null || !iter.hasNext()) {
                    iter = c.iterator();

                    if (!iter.hasNext()) {
                        // The source was emptied after this iterator was created - nothing left to cycle.
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }
                }

                cnt--;

                return iter.next();
            }
        };
    }

    /**
     * Concatenates multiple boolean arrays into a single BooleanIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] a1 = {true, false};
     * boolean[] a2 = {true};
     * BooleanIterator iter = Iterators.concat(a1, a2);
     * // iter.nextBoolean() => true
     * // iter.nextBoolean() => false
     * // iter.nextBoolean() => true
     * }</pre>
     *
     * @param a the boolean arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a BooleanIterator that will iterate over the elements of each provided boolean array in order, or {@code BooleanIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static BooleanIterator concat(final boolean[]... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<boolean[]> iter = Arrays.asList(a.clone()).iterator();
            private boolean[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public boolean nextBoolean() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple char arrays into a single CharIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a1 = {'a', 'b'};
     * char[] a2 = {'c'};
     * CharIterator iter = Iterators.concat(a1, a2);
     * // iter.nextChar() => 'a'
     * // iter.nextChar() => 'b'
     * // iter.nextChar() => 'c'
     * }</pre>
     *
     * @param a the char arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a CharIterator that will iterate over the elements of each provided char array in order, or {@code CharIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static CharIterator concat(final char[]... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<char[]> iter = Arrays.asList(a.clone()).iterator();
            private char[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public char nextChar() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple byte arrays into a single ByteIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a1 = {1, 2};
     * byte[] a2 = {3};
     * ByteIterator iter = Iterators.concat(a1, a2);
     * // iter.nextByte() => 1
     * // iter.nextByte() => 2
     * // iter.nextByte() => 3
     * }</pre>
     *
     * @param a the byte arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a ByteIterator that will iterate over the elements of each provided byte array in order, or {@code ByteIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static ByteIterator concat(final byte[]... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<byte[]> iter = Arrays.asList(a.clone()).iterator();
            private byte[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple short arrays into a single ShortIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a1 = {10, 20};
     * short[] a2 = {30};
     * ShortIterator iter = Iterators.concat(a1, a2);
     * // iter.nextShort() => 10
     * // iter.nextShort() => 20
     * // iter.nextShort() => 30
     * }</pre>
     *
     * @param a the short arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a ShortIterator that will iterate over the elements of each provided short array in order, or {@code ShortIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static ShortIterator concat(final short[]... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<short[]> iter = Arrays.asList(a.clone()).iterator();
            private short[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public short nextShort() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple int arrays into a single IntIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a1 = {1, 2};
     * int[] a2 = {3, 4};
     * IntIterator iter = Iterators.concat(a1, a2);
     * // iter.nextInt() => 1
     * // iter.nextInt() => 2
     * // iter.nextInt() => 3
     * // iter.nextInt() => 4
     * }</pre>
     *
     * @param a the int arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return an IntIterator that will iterate over the elements of each provided int array in order, or {@code IntIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static IntIterator concat(final int[]... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<int[]> iter = Arrays.asList(a.clone()).iterator();
            private int[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public int nextInt() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple long arrays into a single LongIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a1 = {1L, 2L};
     * long[] a2 = {3L};
     * LongIterator iter = Iterators.concat(a1, a2);
     * // iter.nextLong() => 1L
     * // iter.nextLong() => 2L
     * // iter.nextLong() => 3L
     * }</pre>
     *
     * @param a the long arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a LongIterator that will iterate over the elements of each provided long array in order, or {@code LongIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static LongIterator concat(final long[]... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<long[]> iter = Arrays.asList(a.clone()).iterator();
            private long[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public long nextLong() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple float arrays into a single FloatIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a1 = {1.1f};
     * float[] a2 = {2.2f};
     * FloatIterator iter = Iterators.concat(a1, a2);
     * // iter.nextFloat() => 1.1f
     * // iter.nextFloat() => 2.2f
     * }</pre>
     *
     * @param a the float arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a FloatIterator that will iterate over the elements of each provided float array in order, or {@code FloatIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static FloatIterator concat(final float[]... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<float[]> iter = Arrays.asList(a.clone()).iterator();
            private float[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public float nextFloat() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple double arrays into a single DoubleIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a1 = {1.1};
     * double[] a2 = {2.2, 3.3};
     * DoubleIterator iter = Iterators.concat(a1, a2);
     * // iter.nextDouble() => 1.1
     * // iter.nextDouble() => 2.2
     * // iter.nextDouble() => 3.3
     * }</pre>
     *
     * @param a the double arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect; the supplied arrays themselves are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return a DoubleIterator that will iterate over the elements of each provided double array in order, or {@code DoubleIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static DoubleIterator concat(final double[]... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<double[]> iter = Arrays.asList(a.clone()).iterator();
            private double[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((cur == null || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public double nextDouble() throws NoSuchElementException {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple BooleanIterators into a single BooleanIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter1 = BooleanIterator.of(true, false);
     * BooleanIterator iter2 = BooleanIterator.of(true);
     * BooleanIterator result = Iterators.concat(iter1, iter2);
     * // result.nextBoolean() => true
     * // result.nextBoolean() => false
     * // result.nextBoolean() => true
     * }</pre>
     *
     * @param a the BooleanIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a BooleanIterator that will iterate over the elements of each provided BooleanIterator in order, or {@code BooleanIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static BooleanIterator concat(final BooleanIterator... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<BooleanIterator> iter = Arrays.asList(a.clone()).iterator();
            private BooleanIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public boolean nextBoolean() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextBoolean();
            }
        };
    }

    /**
     * Concatenates multiple CharIterators into a single CharIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b');
     * CharIterator iter2 = CharIterator.of('c');
     * CharIterator result = Iterators.concat(iter1, iter2);
     * // result.nextChar() => 'a'
     * // result.nextChar() => 'b'
     * // result.nextChar() => 'c'
     * }</pre>
     *
     * @param a the CharIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a CharIterator that will iterate over the elements of each provided CharIterator in order, or {@code CharIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static CharIterator concat(final CharIterator... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<CharIterator> iter = Arrays.asList(a.clone()).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public char nextChar() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
            }
        };
    }

    /**
     * Concatenates multiple ByteIterators into a single ByteIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
     * ByteIterator iter2 = ByteIterator.of((byte) 3);
     * ByteIterator result = Iterators.concat(iter1, iter2);
     * // result.nextByte() => 1
     * // result.nextByte() => 2
     * // result.nextByte() => 3
     * }</pre>
     *
     * @param a the ByteIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a ByteIterator that will iterate over the elements of each provided ByteIterator in order, or {@code ByteIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static ByteIterator concat(final ByteIterator... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<ByteIterator> iter = Arrays.asList(a.clone()).iterator();
            private ByteIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
            }
        };
    }

    /**
     * Concatenates multiple ShortIterators into a single ShortIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter1 = ShortIterator.of((short) 1, (short) 2);
     * ShortIterator iter2 = ShortIterator.of((short) 3);
     * ShortIterator result = Iterators.concat(iter1, iter2);
     * // result.nextShort() => 1
     * // result.nextShort() => 2
     * // result.nextShort() => 3
     * }</pre>
     *
     * @param a the ShortIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a ShortIterator that will iterate over the elements of each provided ShortIterator in order, or {@code ShortIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static ShortIterator concat(final ShortIterator... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<ShortIterator> iter = Arrays.asList(a.clone()).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public short nextShort() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
            }
        };
    }

    /**
     * Concatenates multiple IntIterators into a single IntIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter1 = IntIterator.of(1, 2);
     * IntIterator iter2 = IntIterator.of(3);
     * IntIterator result = Iterators.concat(iter1, iter2);
     * // result.nextInt() => 1
     * // result.nextInt() => 2
     * // result.nextInt() => 3
     * }</pre>
     *
     * @param a the IntIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return an IntIterator that will iterate over the elements of each provided IntIterator in order, or {@code IntIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static IntIterator concat(final IntIterator... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<IntIterator> iter = Arrays.asList(a.clone()).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public int nextInt() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextInt();
            }
        };
    }

    /**
     * Concatenates multiple LongIterators into a single LongIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter1 = LongIterator.of(1L, 2L);
     * LongIterator iter2 = LongIterator.of(3L);
     * LongIterator result = Iterators.concat(iter1, iter2);
     * // result.nextLong() => 1L
     * // result.nextLong() => 2L
     * // result.nextLong() => 3L
     * }</pre>
     *
     * @param a the LongIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a LongIterator that will iterate over the elements of each provided LongIterator in order, or {@code LongIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static LongIterator concat(final LongIterator... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<LongIterator> iter = Arrays.asList(a.clone()).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public long nextLong() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
            }
        };
    }

    /**
     * Concatenates multiple FloatIterators into a single FloatIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter1 = FloatIterator.of(1.1f, 2.2f);
     * FloatIterator iter2 = FloatIterator.of(3.3f);
     * FloatIterator result = Iterators.concat(iter1, iter2);
     * // result.nextFloat() => 1.1f
     * // result.nextFloat() => 2.2f
     * // result.nextFloat() => 3.3f
     * }</pre>
     *
     * @param a the FloatIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a FloatIterator that will iterate over the elements of each provided FloatIterator in order, or {@code FloatIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static FloatIterator concat(final FloatIterator... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<FloatIterator> iter = Arrays.asList(a.clone()).iterator();
            private FloatIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public float nextFloat() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextFloat();
            }
        };
    }

    /**
     * Concatenates multiple DoubleIterators into a single DoubleIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2);
     * DoubleIterator iter2 = DoubleIterator.of(3.3);
     * DoubleIterator result = Iterators.concat(iter1, iter2);
     * // result.nextDouble() => 1.1
     * // result.nextDouble() => 2.2
     * // result.nextDouble() => 3.3
     * }</pre>
     *
     * @param a the DoubleIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a DoubleIterator that will iterate over the elements of each provided DoubleIterator in order, or {@code DoubleIterator.EMPTY} if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static DoubleIterator concat(final DoubleIterator... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<DoubleIterator> iter = Arrays.asList(a.clone()).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public double nextDouble() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextDouble();
            }
        };
    }

    /**
     * Concatenates multiple arrays into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] a1 = {"a", "b"};
     * String[] a2 = {"c"};
     * ObjIterator<String> iter = Iterators.concat(a1, a2);
     * // iter.next() => "a"
     * // iter.next() => "b"
     * // iter.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the arrays.
     * @param a the arrays to be concatenated. {@code null} or empty arrays within {@code a} are skipped. The supplied
     *          arrays are <b>not</b> copied and are read lazily, so writing into one is visible to the returned iterator.
     * @return an ObjIterator that will iterate over the elements of each provided array in order, or an empty iterator if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (final T[] e : a) {
            if (N.notEmpty(e)) {
                list.add(ObjIterator.of(e));
            }
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
     * Iterator<String> iter2 = Arrays.asList("c").iterator();
     * ObjIterator<String> result = Iterators.concat(iter1, iter2);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the Iterators.
     * @param a the Iterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return an ObjIterator that will iterate over the elements of each provided Iterator in order, or an empty iterator if {@code a} is {@code null} or empty.
     * @see N#concat(Iterator...)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(Arrays.asList(a.clone()));
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("A", "B");
     * List<String> list2 = Arrays.asList("C", "D");
     * Set<String> set = new HashSet<>(Arrays.asList("E", "F"));
     * ObjIterator<String> iter = Iterators.concat(list1, list2, set);
     * // Yields: "A", "B", "C", "D", "E", "F"
     *
     * List<Integer> nums1 = Arrays.asList(1, 2);
     * List<Integer> nums2 = Arrays.asList(3, 4);
     * ObjIterator<Integer> numbers = Iterators.concat(nums1, nums2);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the Iterable objects.
     * @param a the Iterable objects to be concatenated. {@code null} Iterable elements within {@code a} are treated as empty. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return an ObjIterator that will lazily obtain and iterate over each provided Iterable in order, or an empty iterator if {@code a} is {@code null} or empty.
     * @see N#concat(Iterable...)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        return concatIterables(Arrays.asList(a.clone()));
    }

    /**
     * Concatenates multiple Maps into a single ObjIterator of Map.Entry.
     * The entries retain each map's write-through behavior, so all maps must have the declared
     * key and value types. Copy entries into wider-typed entries when widening is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = N.asMap("a", 1);
     * Map<String, Integer> map2 = N.asMap("b", 2);
     * ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(map1, map2);
     * // Iterates over entries from map1 then map2
     * }</pre>
     *
     * @param <K> the type of keys in the Maps.
     * @param <V> the type of values in the Maps.
     * @param a the Maps to be concatenated. {@code null} Maps in the array are skipped; an empty one contributes
     *          nothing. Each map's entry set is iterated lazily, when the previous map has been exhausted, so a map
     *          modified before its own turn contributes its current entries rather than raising
     *          {@link java.util.ConcurrentModificationException}.
     * @return an ObjIterator of Map.Entry that will iterate over the entries of each provided Map in order, or an empty iterator if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static <K, V> ObjIterator<Map.Entry<K, V>> concat(final Map<K, V>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterable<Map.Entry<K, V>>> list = new ArrayList<>(a.length);

        for (final Map<K, V> e : a) {
            if (e != null) {
                // Collect the entry *sets*, not their iterators: concatIterables calls iterator() only when the
                // previous source runs out, matching concat(Iterable...). Grabbing every entrySet().iterator() up
                // front used to make a later put() on any of these maps throw ConcurrentModificationException on
                // the first next(), even for a map whose turn had not come yet.
                list.add(e.entrySet());
            }
        }

        return concatIterables(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("a", "b").iterator(),
     *     Arrays.asList("c").iterator()
     * );
     * ObjIterator<String> result = Iterators.concat(iterators);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * <p><b>Note:</b> {@code c.iterator()} is obtained eagerly, when this method is called, so structurally modifying
     * {@code c} afterwards is governed by that collection's own iterator contract - a fail-fast collection raises
     * {@link java.util.ConcurrentModificationException}. The iterators <i>inside</i> {@code c} are consumed lazily.</p>
     *
     * @param <T> the type of elements in the Iterators.
     * @param c the collection of iterators to be concatenated, or {@code null}/empty to return an empty iterator.
     *          {@code null} or exhausted iterators within {@code c} are skipped.
     * @return an ObjIterator that will iterate over the elements of each provided Iterator in order, or an empty iterator if {@code c} is {@code null} or empty.
     * @see #concat(Iterator...)
     * @see #concatIterables(Collection)
     */
    public static <T> ObjIterator<T> concat(final Collection<? extends Iterator<? extends T>> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        // How many sources there are is decided by iterating c, never by Collection.isEmpty() - the same rule
        // merge(Collection, ..) follows. A collection whose isEmpty() disagrees with its iterator used to make
        // this method drop every source silently.
        final Iterator<? extends Iterator<? extends T>> outer = c.iterator();

        if (!outer.hasNext()) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterator<? extends T>> iter = outer;
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<String>> iterables = Arrays.asList(
     *     Arrays.asList("a", "b"),
     *     Arrays.asList("c")
     * );
     * ObjIterator<String> result = Iterators.concatIterables(iterables);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * <p><b>Note:</b> {@code c.iterator()} is obtained eagerly, when this method is called, so structurally modifying
     * {@code c} afterwards is governed by that collection's own iterator contract - a fail-fast collection raises
     * {@link java.util.ConcurrentModificationException}. Each {@code Iterable} <i>inside</i> {@code c} is iterated
     * lazily, only once the previous one has been exhausted.</p>
     *
     * @param <T> the type of elements in the Iterable objects.
     * @param c the collection of Iterable objects to be concatenated, or {@code null}/empty to return an empty iterator.
     *          {@code null} or empty {@code Iterable}s within {@code c} are skipped.
     * @return an ObjIterator that will iterate over the elements of each provided Iterable, or an empty iterator if {@code c} is {@code null} or empty.
     * @see N#concat(Iterable...)
     * @see N#iterateEach(Collection)
     */
    public static <T> ObjIterator<T> concatIterables(final Collection<? extends Iterable<? extends T>> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        // See concat(Collection): the source count comes from the iterator, not from Collection.isEmpty().
        final Iterator<? extends Iterable<? extends T>> outer = c.iterator();

        if (!outer.hasNext()) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterable<? extends T>> iter = outer;
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = N.iterate(iter.next());
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple BiIterators into a single BiIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter1 = BiIterator.of(N.asMap("a", 1));
     * BiIterator<String, Integer> iter2 = BiIterator.of(N.asMap("b", 2));
     * BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);
     * // result.next() => (a, 1)
     * // result.next() => (b, 2)
     * }</pre>
     *
     * @param <A> the type of the first element in the BiIterator.
     * @param <B> the type of the second element in the BiIterator.
     * @param a the BiIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a BiIterator that will iterate over the elements of each provided BiIterator in order, or an empty BiIterator if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static <A, B> BiIterator<A, B> concat(final BiIterator<A, B>... a) {
        if (N.isEmpty(a)) {
            return BiIterator.empty();
        }

        return new BiIterator<>() {
            private final Iterator<BiIterator<A, B>> iter = Arrays.asList(a.clone()).iterator();
            private BiIterator<A, B> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws NoSuchElementException if no next element is available from the source iteration.
             * @throws E if {@code action} throws while consuming an element.
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            // Performs the given action for each remaining pair of elements from this concatenated iterator.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                final Throwables.BiConsumer<? super A, ? super B, RuntimeException> actionE = Fnn.from(action);

                while (hasNext()) {
                    cur.foreachRemaining(actionE);
                }
            }

            // Performs the given action for each remaining pair of elements from this concatenated iterator.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while consuming an element.
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            // Returns an iterator that applies the given mapping function to each remaining pair of elements.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no next element is available from the source iteration.
                     */
                    @Override
                    public R next() throws NoSuchElementException {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Concatenates multiple TriIterators into a single TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<Integer, Integer, Integer> iter1 = TriIterator.generate(0, 1, (i, t) -> t.set(1, 2, 3));
     * TriIterator<Integer, Integer, Integer> iter2 = TriIterator.generate(0, 1, (i, t) -> t.set(4, 5, 6));
     * TriIterator<Integer, Integer, Integer> result = Iterators.concat(iter1, iter2);
     * // result.next() => (1, 2, 3)
     * // result.next() => (4, 5, 6)
     * }</pre>
     *
     * @param <A> the type of the first element in the TriIterator.
     * @param <B> the type of the second element in the TriIterator.
     * @param <C> the type of the third element in the TriIterator.
     * @param a the TriIterators to be concatenated. {@code null} or exhausted iterators within {@code a} are skipped. The varargs array is copied, so replacing one of its elements afterwards has no effect on the returned iterator.
     * @return a TriIterator that will iterate over the elements of each provided TriIterator in order, or an empty TriIterator if {@code a} is {@code null} or empty.
     */
    @SafeVarargs
    public static <A, B, C> TriIterator<A, B, C> concat(final TriIterator<A, B, C>... a) {
        if (N.isEmpty(a)) {
            return TriIterator.empty();
        }

        return new TriIterator<>() {
            private final Iterator<TriIterator<A, B, C>> iter = Arrays.asList(a.clone()).iterator();
            private TriIterator<A, B, C> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public Triple<A, B, C> next() throws NoSuchElementException {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws NoSuchElementException if no next element is available from the source iteration.
             * @throws E if {@code action} throws while consuming an element.
             */
            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            // Performs the given action for each remaining triple of elements from this concatenated iterator.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            // Performs the given action for each remaining triple of elements from this concatenated iterator.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while consuming an element.
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            // Returns an iterator that applies the given mapping function to each remaining triple of elements.
            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no next element is available from the source iteration.
                     */
                    @Override
                    public R next() throws NoSuchElementException {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Merges two iterators into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();
     * ObjIterator<Integer> result = Iterators.merge(iter1, iter2,
     *     (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // result => [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param <T> the type of elements in the iterators.
     * @param a the first iterator to be merged, or {@code null} which is treated as an empty iterator.
     * @param b the second iterator to be merged, or {@code null} which is treated as an empty iterator.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The element from {@code a} (the first parameter) is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the element from {@code b} (the second parameter) is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     * @see #merge(Collection, BiFunction)
     * @see #mergeSorted(Iterator, Iterator, Comparator)
     * @see N#merge(Iterable, Iterable, BiFunction)
     * @see Maps#merge(Map, Object, Object, BiFunction)
     */
    public static <T> ObjIterator<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return new ObjIterator<>() {
            private final Iterator<? extends T> iterA = a == null ? ObjIterator.<T> empty() : a;
            private final Iterator<? extends T> iterB = b == null ? ObjIterator.<T> empty() : b;
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (iterA.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (iterA.hasNext()) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.next();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.next();
                } else {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        };
    }

    /**
     * Merges multiple iterators into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iters = Arrays.asList(
     *     Arrays.asList(1, 3, 5).iterator(),
     *     Arrays.asList(2, 4, 6).iterator()
     * );
     * ObjIterator<Integer> merged = Iterators.merge(iters, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6 (sorted merge)
     * }</pre>
     *
     * <p><b>What {@code nextSelector} is handed for more than two iterators:</b> the merge is built by folding
     * {@link #merge(Iterator, Iterator, BiFunction)} from the left, so for three or more iterators the <i>first</i>
     * argument is the next element of the already-merged prefix rather than of any one fixed iterator, while the
     * second is the next element of the iterator being folded in. For example, merging {@code [1, 4]},
     * {@code [2, 5]} and {@code [3, 6]} with a min-first selector calls it with
     * {@code (1,2) (1,3) (4,2) (2,3) (4,5) (4,3) (4,6) (5,6)}. Selectors that only compare their two arguments -
     * such as {@link MergeResult#minFirst(Comparator)} - are unaffected; selectors that depend on <i>which</i>
     * iterator an element came from are not meaningful here. The fold also makes this
     * {@code O(n * c.size())} comparisons rather than {@code O(n log c.size())}.</p>
     *
     * <p>The fold is <i>executed</i> iteratively rather than as nested iterators, so {@code c} may hold any number
     * of iterators without exhausting the call stack. The cost per element is unchanged in kind: it stays
     * proportional to {@code c.size()}, as the comparison count above already was.</p>
     *
     * <p><b>How many sources there are is decided by iterating {@code c}, never by {@link Collection#size()} or
     * {@link Collection#isEmpty()}</b>, so a collection whose {@code size()} disagrees with what its iterator
     * yields - any weakly consistent or concurrently modified one - still merges exactly the iterators it hands
     * out. The source references are collected eagerly, when this method is called; the elements themselves are
     * still pulled on demand, because no source is asked for {@code hasNext()}/{@code next()} until the returned
     * iterator is read.</p>
     *
     * @param <T> the type of elements in the iterators.
     * @param c the collection of iterators to be merged, or {@code null}/empty which results in an empty iterator.
     *          {@code null} iterators within {@code c} are treated as empty. Each element must be a <i>distinct</i>
     *          iterator: every position of the fold advances its sources independently, so listing the same
     *          {@code Iterator} instance twice gives an unspecified result.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in the order determined by {@code nextSelector}, or an empty iterator if {@code c} is {@code null} or empty.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     * @see N#merge(Collection, BiFunction)
     */
    public static <T> ObjIterator<T> merge(final Collection<? extends Iterator<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (c == null) {
            return ObjIterator.empty();
        }

        // The collection's ITERATOR, not its size()/isEmpty(), decides how many sources there are. A collection
        // whose size() disagrees with what its iterator yields - a concurrently modified or loosely implemented
        // one - used to lose sources silently: size() 0 dropped every source (via isEmpty()), size() 1 or 2 kept
        // only that many and dropped the rest, and an over-reported size() ran the iterator off its end with a
        // NoSuchElementException. Walking it directly also means the 0-, 1- and 2-source shapes allocate no list
        // at all, and that no capacity is ever taken from a size() that could be bogus.
        final Iterator<? extends Iterator<? extends T>> iter = c.iterator();

        if (!iter.hasNext()) {
            return ObjIterator.empty();
        }

        // merge(Iterator, Iterator, ..) maps a null iterator to an empty one; do the same up front so that
        // neither the pair form nor the fold below has to special-case it.
        final Iterator<? extends T> first = nullToEmptyIterator(iter.next());

        if (!iter.hasNext()) {
            return ObjIterator.of(first);
        }

        final Iterator<? extends T> second = nullToEmptyIterator(iter.next());

        if (!iter.hasNext()) {
            return merge(first, second, nextSelector);
        }

        final List<Iterator<? extends T>> sourceList = new ArrayList<>();
        sourceList.add(first);
        sourceList.add(second);

        while (iter.hasNext()) {
            sourceList.add(nullToEmptyIterator(iter.next()));
        }

        @SuppressWarnings("unchecked")
        final Iterator<? extends T>[] sources = sourceList.toArray(new Iterator[0]);

        return mergeLeftFold(sources, nextSelector);
    }

    private static <T> Iterator<? extends T> nullToEmptyIterator(final Iterator<? extends T> iter) {
        return iter == null ? ObjIterator.<T> empty() : iter;
    }

    /**
     * Executes the left fold of {@link #merge(Iterator, Iterator, BiFunction)} over {@code sources} - which must
     * hold at least three iterators - without nesting one merge iterator inside the next.
     *
     * <p>Nesting made every {@code hasNext()}/{@code next()} recurse once per source, which overflowed the call
     * stack at a few thousand sources. This keeps the same fold, and therefore the exact sequence of
     * {@code nextSelector} calls documented on {@link #merge(Collection, BiFunction)}, by holding the state of
     * each conceptual merge node in parallel arrays: node {@code j} merges the result of nodes {@code 0..j-1}
     * (or {@code sources[0]} when {@code j == 0}) with {@code sources[j + 1]}.</p>
     *
     * <p>Each element is produced in two passes. Which nodes have to pull a value from their left side is decided
     * by node state alone - {@code !hasNextA[j] && leftHasNext[j]} - so the first pass can walk <i>down</i> from
     * the outermost node to the lowest node that must pull, consuming nothing; the second pass then walks back
     * <i>up</i>, handing each node the value the node below it produced. Both passes are plain loops.</p>
     *
     * @param <T> the type of elements in the iterators.
     * @param sources the non-{@code null}, pairwise distinct iterators to fold, in order; at least three.
     * @param nextSelector the non-{@code null} selector applied at every node.
     * @return an {@code ObjIterator} yielding exactly what the nested fold yielded - same elements, same
     *         {@code nextSelector} calls in the same order - for any independently advanced sources. The one
     *         input the two disagree on is the same {@code Iterator} instance listed more than once, where the
     *         nested form tended to throw {@link NoSuchElementException}; both results are unspecified, and
     *         {@link #merge(Collection, BiFunction)} documents the requirement.
     */
    private static <T> ObjIterator<T> mergeLeftFold(final Iterator<? extends T>[] sources, final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        final int nodeCount = sources.length - 1;
        final int top = nodeCount - 1;

        return new ObjIterator<>() {
            private final Object[] nextA = new Object[nodeCount];
            private final Object[] nextB = new Object[nodeCount];
            private final boolean[] hasNextA = new boolean[nodeCount];
            private final boolean[] hasNextB = new boolean[nodeCount];

            /** Scratch, refreshed by {@code refresh()}: does node {@code j}'s left side have a next element? */
            private final boolean[] leftHasNext = new boolean[nodeCount];

            /** Scratch, refreshed by {@code refresh()}: does node {@code j} itself have a next element? */
            private final boolean[] nodeHasNext = new boolean[nodeCount];

            @Override
            public boolean hasNext() {
                // Short-circuiting equivalent of refresh()'s nodeHasNext[top]. Expanding
                //   nodeHasNext[j] = hasNextA[j] || hasNextB[j] || nodeHasNext[j - 1] || sources[j + 1].hasNext()
                // down to node 0 leaves a plain OR over every node, so the first true answer wins and the nodes
                // below it are never examined - the same early exit the nested merge iterators had. next() still
                // needs the full bottom-up pass, because it reads leftHasNext[] for the descent.
                for (int j = top; j >= 0; j--) {
                    if (hasNextA[j] || hasNextB[j] || sources[j + 1].hasNext()) {
                        return true;
                    }
                }

                return sources[0].hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                refresh();

                if (!nodeHasNext[top]) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                int j = top;

                while (j > 0 && needsValueFromLeft(j)) {
                    j--;
                }

                T value = null;

                for (int k = j; k <= top; k++) {
                    value = produce(k, value, k > j);
                }

                return value;
            }

            /**
             * Recomputes both scratch arrays bottom-up. A node's left side is the node below it, so one upward
             * pass answers every "does this node have a next element?" question without recursing.
             */
            private void refresh() {
                for (int j = 0; j < nodeCount; j++) {
                    leftHasNext[j] = j == 0 ? sources[0].hasNext() : nodeHasNext[j - 1];
                    nodeHasNext[j] = hasNextA[j] || hasNextB[j] || leftHasNext[j] || sources[j + 1].hasNext();
                }
            }

            /**
             * Mirrors the branch structure of {@code merge(A, B).next()}: the buffered-left branch answers from
             * {@code nextA} without touching the left side, and every other branch pulls from the left exactly
             * when the left side has an element.
             */
            private boolean needsValueFromLeft(final int k) {
                return !hasNextA[k] && leftHasNext[k];
            }

            /**
             * The element node {@code k} takes from its left side: the one node {@code k - 1} just produced, or -
             * only for node 0, whose left side is the leaf {@code sources[0]} - one pulled from that leaf. The
             * descent in {@code next()} stops at a node that does not pull from its left, so every node above the
             * stopping point is handed a value; this guard makes that invariant fail loudly rather than silently
             * consuming from the wrong source if the descent is ever changed.
             * @throws IllegalStateException if a non-leaf merge node requires a left value but none was supplied.
             * @throws NoSuchElementException if the leftmost source iterator is exhausted when its next value is requested.
             */
            private T leftElement(final int k, final T leftValue, final boolean leftValueSupplied) throws IllegalStateException, NoSuchElementException {
                if (leftValueSupplied) {
                    return leftValue;
                }

                if (k != 0) {
                    throw new IllegalStateException("No left element was produced for merge node " + k);
                }

                return sources[0].next();
            }

            /**
             * Produces node {@code k}'s next element, branch for branch as {@code merge(A, B).next()} does.
             * {@code leftValue} is the element node {@code k - 1} just produced; it is supplied precisely when
             * the descent decided this node pulls from its left side, so it is never dropped. Node 0's left side
             * is the leaf {@code sources[0]}, which it pulls itself.
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @SuppressWarnings("unchecked")
            private T produce(final int k, final T leftValue, final boolean leftValueSupplied) throws NoSuchElementException {
                final Iterator<? extends T> iterB = sources[k + 1];

                if (hasNextA[k]) {
                    if (iterB.hasNext()) {
                        final T b = iterB.next();
                        nextB[k] = b;

                        if (nextSelector.apply((T) nextA[k], b) == MergeResult.TAKE_FIRST) {
                            hasNextA[k] = false;
                            hasNextB[k] = true;

                            return (T) nextA[k];
                        }

                        return b;
                    }

                    hasNextA[k] = false;

                    return (T) nextA[k];
                } else if (hasNextB[k]) {
                    if (leftHasNext[k]) {
                        final T a = leftElement(k, leftValue, leftValueSupplied);
                        nextA[k] = a;

                        if (nextSelector.apply(a, (T) nextB[k]) == MergeResult.TAKE_FIRST) {
                            return a;
                        }

                        hasNextA[k] = true;
                        hasNextB[k] = false;

                        return (T) nextB[k];
                    }

                    hasNextB[k] = false;

                    return (T) nextB[k];
                } else if (leftHasNext[k]) {
                    if (iterB.hasNext()) {
                        final T a = leftElement(k, leftValue, leftValueSupplied);
                        final T b = iterB.next();
                        nextA[k] = a;
                        nextB[k] = b;

                        if (nextSelector.apply(a, b) == MergeResult.TAKE_FIRST) {
                            hasNextB[k] = true;

                            return a;
                        }

                        hasNextA[k] = true;

                        return b;
                    }

                    return leftElement(k, leftValue, leftValueSupplied);
                } else if (iterB.hasNext()) {
                    return iterB.next();
                }

                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }
        };
    }

    /**
     * Merges two {@code Iterable} objects into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list1 = Arrays.asList(1, 3, 5);
     * List<Integer> list2 = Arrays.asList(2, 4, 6);
     * ObjIterator<Integer> merged = Iterators.merge(list1, list2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6 (sorted merge)
     * }</pre>
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param a the first {@code Iterable} object to be merged, or {@code null} which is treated as empty.
     * @param b the second {@code Iterable} object to be merged, or {@code null} which is treated as empty.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The element from {@code a} (the first parameter) is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the element from {@code b} (the second parameter) is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     * @see #merge(Iterator, Iterator, BiFunction)
     * @see #mergeIterables(Collection, BiFunction)
     */
    public static <T> ObjIterator<T> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        final Iterator<? extends T> iterA = N.iterate(a);
        final Iterator<? extends T> iterB = N.iterate(b);

        return merge(iterA, iterB, nextSelector);

    }

    /**
     * Merges multiple {@code Iterable} objects into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<Integer>> lists = Arrays.asList(
     *     Arrays.asList(1, 4, 7),
     *     Arrays.asList(2, 5, 8),
     *     Arrays.asList(3, 6, 9)
     * );
     * ObjIterator<Integer> merged = Iterators.mergeIterables(lists, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6, 7, 8, 9 (sorted merge)
     * }</pre>
     *
     * <p>For three or more inputs the merge is a left fold of pairwise merges; see
     * {@link #merge(Collection, BiFunction)} for what {@code nextSelector} is handed and what it costs.</p>
     *
     * <p>As in {@link #merge(Collection, BiFunction)}, how many sources there are is decided by iterating
     * {@code iterables}, never by {@link Collection#size()} or {@link Collection#isEmpty()}.</p>
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param iterables the collection of {@code Iterable} objects to be merged, or {@code null}/empty which results in an empty iterator.
     *                  {@code null} {@code Iterable}s within {@code iterables} are treated as empty.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in the order determined by {@code nextSelector}, or an empty iterator if {@code iterables} is {@code null} or empty.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     * @see #merge(Collection, BiFunction)
     */
    public static <T> ObjIterator<T> mergeIterables(final Collection<? extends Iterable<? extends T>> iterables,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (iterables == null) {
            return ObjIterator.empty();
        }

        // Walk the iterator, never size()/isEmpty() - see merge(Collection, ..) for why. Dispatched here rather
        // than by handing a materialised list to merge(Collection, ..), which would scan it a second time.
        // N.iterate maps a null Iterable to an empty iterator.
        final Iterator<? extends Iterable<? extends T>> iter = iterables.iterator();

        if (!iter.hasNext()) {
            return ObjIterator.empty();
        }

        final Iterator<? extends T> first = N.iterate(iter.next());

        if (!iter.hasNext()) {
            return ObjIterator.of(first);
        }

        final Iterator<? extends T> second = N.iterate(iter.next());

        if (!iter.hasNext()) {
            return merge(first, second, nextSelector);
        }

        final List<Iterator<? extends T>> sourceList = new ArrayList<>();
        sourceList.add(first);
        sourceList.add(second);

        while (iter.hasNext()) {
            sourceList.add(N.iterate(iter.next()));
        }

        // Straight to the fold: N.iterate never returns null, so there is nothing left for merge(Collection, ..)
        // to normalise, and routing through it would only re-scan the list just built.
        @SuppressWarnings("unchecked")
        final Iterator<? extends T>[] sources = sourceList.toArray(new Iterator[0]);

        return mergeLeftFold(sources, nextSelector);
    }

    /**
     * Merges two sorted Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in a sorted order.
     * The elements in the Iterators should implement the {@code Comparable} interface.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();
     * ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);
     * // result => [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * <p><b>{@code null} elements:</b> the natural ordering used here is {@link Comparators#naturalOrder()}, which is
     * null-friendly and sorts {@code null} <i>first</i>; it does not throw {@link NullPointerException} the way
     * {@link Comparator#naturalOrder()} would. Both inputs must therefore be sorted nulls-first as well. Pass an
     * explicit comparator to {@link #mergeSorted(Iterator, Iterator, Comparator)} for any other {@code null} policy.</p>
     *
     * @param <T> the type of elements in the Iterators, which should implement the {@code Comparable} interface.
     * @param sortedA the first Iterator to be merged. It should be in non-descending order, with {@code null}s first.
     * @param sortedB the second Iterator to be merged. It should be in non-descending order, with {@code null}s first.
     * @return an ObjIterator that will iterate over the elements of the provided Iterators in a sorted order.
     * @see #mergeSorted(Iterator, Iterator, Comparator)
     * @see Comparators#naturalOrder()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in a sorted order.
     * The order of elements in the resulting iterator is determined by the provided {@code Comparator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(5, 3, 1).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(6, 4, 2).iterator();
     * ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2, Comparator.reverseOrder());
     * // result => [6, 5, 4, 3, 2, 1]
     * }</pre>
     *
     * @param <T> the type of elements in the iterators.
     * @param sortedA the first iterator to be merged. It should already be sorted according to {@code cmp}.
     * @param sortedB the second iterator to be merged. It should already be sorted according to {@code cmp}.
     * @param cmp the {@code Comparator} to determine the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in a sorted order.
     * @throws IllegalArgumentException if {@code cmp} is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmp, cs.cmp);

        return merge(sortedA, sortedB, MergeResult.minFirst(cmp));
    }

    /**
     * Merges two sorted Iterable objects into a single ObjIterator, which will iterate over the elements of each Iterable in a sorted order.
     * The elements in the Iterable objects should implement the {@code Comparable} interface.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> listA = Arrays.asList(1, 3, 5, 7);
     * List<Integer> listB = Arrays.asList(2, 4, 6, 8);
     * ObjIterator<Integer> merged = Iterators.mergeSorted(listA, listB);
     * // Iterates through: 1, 2, 3, 4, 5, 6, 7, 8
     * }</pre>
     *
     * <p><b>{@code null} elements:</b> the natural ordering used here is {@link Comparators#naturalOrder()}, which is
     * null-friendly and sorts {@code null} <i>first</i>; it does not throw {@link NullPointerException} the way
     * {@link Comparator#naturalOrder()} would. Both inputs must therefore be sorted nulls-first as well. Pass an
     * explicit comparator to {@link #mergeSorted(Iterable, Iterable, Comparator)} for any other {@code null} policy.</p>
     *
     * @param <T> the type of elements in the Iterable objects, which should implement the {@code Comparable} interface.
     * @param sortedA the first Iterable object to be merged. It should be in non-descending order, with {@code null}s first.
     * @param sortedB the second Iterable object to be merged. It should be in non-descending order, with {@code null}s first.
     * @return an ObjIterator that will iterate over the elements of the provided Iterable objects in a sorted order.
     * @see #mergeSorted(Iterable, Iterable, Comparator)
     * @see Comparators#naturalOrder()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in the order determined by the provided {@code Comparator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> listA = Arrays.asList("apple", "cherry", "grape");
     * List<String> listB = Arrays.asList("banana", "date", "fig");
     * ObjIterator<String> merged = Iterators.mergeSorted(listA, listB, Comparator.naturalOrder());
     * // Iterates through: "apple", "banana", "cherry", "date", "fig", "grape"
     * }</pre>
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param sortedA the first {@code Iterable} object to be merged. It should already be sorted according to {@code cmp}.
     * @param sortedB the second {@code Iterable} object to be merged. It should already be sorted according to {@code cmp}.
     * @param cmp the {@code Comparator} to determine the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in a sorted order.
     * @throws IllegalArgumentException if {@code cmp} is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmp, cs.cmp);

        final Iterator<? extends T> iterA = N.iterate(sortedA);
        final Iterator<? extends T> iterB = N.iterate(sortedB);

        return mergeSorted(iterA, iterB, cmp);
    }

    /**
     * Zips two iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in parallel.
     * The resulting elements are determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a", "b").iterator();
     * Iterator<Integer> b = Arrays.asList(1, 2).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "b2"
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator.
     * @param <B> the type of elements in the second iterator.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param b the second iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param zipFunction a {@code BiFunction} that takes an element from each iterator and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}. The resulting iterator stops as soon as either input iterator is exhausted.
     * Calling {@code next()} after exhaustion throws {@link NoSuchElementException} without advancing either input iterator.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, Object, Object, BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     * @see Maps#zip(Iterable, Iterable)
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        return new ObjIterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public R next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.next(), iterB.next());
            }
        };
    }

    /**
     * Zips two {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in parallel.
     * The resulting elements are determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> a = Arrays.asList("a", "b");
     * List<Integer> b = Arrays.asList(1, 2);
     * ObjIterator<String> result = Iterators.zip(a, b, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "b2"
     * }</pre>
     *
     * @param <A> the type of elements in the first {@code Iterable}.
     * @param <B> the type of elements in the second {@code Iterable}.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first {@code Iterable} to be zipped, or {@code null} which is treated as empty.
     * @param b the second {@code Iterable} to be zipped, or {@code null} which is treated as empty.
     * @param zipFunction a {@code BiFunction} that takes an element from each {@code Iterable} and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}. The resulting iterator stops as soon as either input is exhausted.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, BiFunction)
     * @see #zip(Iterable, Iterable, Object, Object, BiFunction)
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final Iterator<? extends A> iterA = N.iterate(a);
        final Iterator<? extends B> iterB = N.iterate(b);

        return zip(iterA, iterB, zipFunction);
    }

    /**
     * Zips three iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in parallel.
     * The resulting elements are determined by the provided {@code TriFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a").iterator();
     * Iterator<Integer> b = Arrays.asList(1).iterator();
     * Iterator<Boolean> c = Arrays.asList(true).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, c, (s, i, bool) -> s + i + bool);
     * // result.next() => "a1true"
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator.
     * @param <B> the type of elements in the second iterator.
     * @param <C> the type of elements in the third iterator.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param b the second iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param c the third iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param zipFunction a {@code TriFunction} that takes an element from each iterator and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}. The resulting iterator stops as soon as any input iterator is exhausted.
     * Calling {@code next()} after exhaustion throws {@link NoSuchElementException} without advancing any input iterator.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        return new ObjIterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<? extends C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public R next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        };
    }

    /**
     * Zips three {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in parallel.
     * The resulting elements are determined by the provided {@code TriFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob");
     * List<Integer> ages = Arrays.asList(30, 25);
     * List<String> cities = Arrays.asList("NYC", "LA");
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Bob,25,LA"
     * }</pre>
     *
     * @param <A> the type of elements in the first {@code Iterable}.
     * @param <B> the type of elements in the second {@code Iterable}.
     * @param <C> the type of elements in the third {@code Iterable}.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first {@code Iterable} to be zipped, or {@code null} which is treated as empty.
     * @param b the second {@code Iterable} to be zipped, or {@code null} which is treated as empty.
     * @param c the third {@code Iterable} to be zipped, or {@code null} which is treated as empty.
     * @param zipFunction a {@code TriFunction} that takes an element from each {@code Iterable} and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}. The resulting iterator stops as soon as any input is exhausted.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, Iterator, TriFunction)
     * @see #zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final Iterator<? extends A> iterA = N.iterate(a);
        final Iterator<? extends B> iterB = N.iterate(b);
        final Iterator<? extends C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, zipFunction);
    }

    /**
     * Zips two Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction {@code zipFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a").iterator();
     * Iterator<Integer> b = Arrays.asList(1, 2).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, "empty", 0, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "empty2"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterator.
     * @param <B> the type of elements in the second Iterator.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param b the second Iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param valueForNoneA the default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterator is exhausted.
     * @param zipFunction a BiFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by {@code zipFunction}. The resulting iterator continues until both input iterators are exhausted, substituting the corresponding default value for an exhausted iterator.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, BiFunction)
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        return new ObjIterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public R next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB);
                } else {
                    return zipFunction.apply(valueForNoneA, iterB.next());
                }
            }
        };
    }

    /**
     * Zips two Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction {@code zipFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> a = Arrays.asList("a");
     * List<Integer> b = Arrays.asList(1, 2);
     * ObjIterator<String> result = Iterators.zip(a, b, "empty", 0, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "empty2"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterable.
     * @param <B> the type of elements in the second Iterable.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterable to be zipped, or {@code null} which is treated as empty.
     * @param b the second Iterable to be zipped, or {@code null} which is treated as empty.
     * @param valueForNoneA the default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterable is exhausted.
     * @param zipFunction a BiFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by {@code zipFunction}. The resulting iterator continues until both inputs are exhausted, substituting the corresponding default value for an exhausted input.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterable, Iterable, BiFunction)
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final Iterator<? extends A> iterA = N.iterate(a);
        final Iterator<? extends B> iterB = N.iterate(b);

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips three Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction {@code zipFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> names = Arrays.asList("Alice").iterator();
     * Iterator<Integer> ages = Arrays.asList(30, 25).iterator();
     * Iterator<String> cities = Arrays.asList("NYC").iterator();
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, "Unknown", 0, "N/A",
     *     (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Unknown,25,N/A"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterator.
     * @param <B> the type of elements in the second Iterator.
     * @param <C> the type of elements in the third Iterator.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param b the second Iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param c the third Iterator to be zipped, or {@code null} which is treated as an empty iterator.
     * @param valueForNoneA the default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterator is exhausted.
     * @param valueForNoneC the default value to be used when the third Iterator is exhausted.
     * @param zipFunction a TriFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by {@code zipFunction}. The resulting iterator continues until all input iterators are exhausted, substituting the corresponding default value for an exhausted iterator.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterator, Iterator, Iterator, TriFunction)
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        return new ObjIterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<? extends C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public R next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.apply(valueForNoneA, iterB.next(), iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else {
                    return zipFunction.apply(valueForNoneA, valueForNoneB, iterC.next());
                }
            }
        };
    }

    /**
     * Zips three Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction <i>zipFunction</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice");
     * List<Integer> ages = Arrays.asList(30, 25);
     * List<String> cities = Arrays.asList("NYC");
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, "Unknown", 0, "N/A",
     *     (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Unknown,25,N/A"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterable.
     * @param <B> the type of elements in the second Iterable.
     * @param <C> the type of elements in the third Iterable.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterable to be zipped, or {@code null} which is treated as empty.
     * @param b the second Iterable to be zipped, or {@code null} which is treated as empty.
     * @param c the third Iterable to be zipped, or {@code null} which is treated as empty.
     * @param valueForNoneA the default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterable is exhausted.
     * @param valueForNoneC the default value to be used when the third Iterable is exhausted.
     * @param zipFunction a TriFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by <i>zipFunction</i>. The resulting iterator continues until all inputs are exhausted, substituting the corresponding default value for an exhausted input.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     * @see #zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final Iterator<? extends A> iterA = N.iterate(a);
        final Iterator<? extends B> iterB = N.iterate(b);
        final Iterator<? extends C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Unzips an Iterator into a BiIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Note:</b> this method simply delegates to {@link BiIterator#unzip(Iterator, BiConsumer)}, which may be called directly instead.
     * Its three-way counterpart {@code unzip3} is deprecated in favor of {@link TriIterator#unzip(Iterator, BiConsumer)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a:1", "b:2").iterator();
     * BiIterator<String, Integer> result = Iterators.unzip(iter, (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * // result.next() => (a, 1)
     * // result.next() => (b, 2)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterator.
     * @param <A> the type of the first element in the resulting BiIterator.
     * @param <B> the type of the second element in the resulting BiIterator.
     * @param iter the original Iterator to be unzipped, or {@code null} to return an empty result.
     * @param unzip a BiConsumer that takes an element from the original Iterator and a Pair to be filled with the resulting elements for the BiIterator.
     * @return a BiIterator that will iterate over the elements created by <i>unzip</i>, or an empty one if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code unzip} is {@code null}.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     * @see N#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzip)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzip, cs.unzip);

        return BiIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a BiIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Note:</b> this method is equivalent to {@link BiIterator#unzip(Iterable, BiConsumer)}, which may be called directly instead.
     * Its three-way counterpart {@code unzip3} is deprecated in favor of {@link TriIterator#unzip(Iterable, BiConsumer)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a:1", "b:2");
     * BiIterator<String, Integer> result = Iterators.unzip(list, (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * // result.next() => (a, 1)
     * // result.next() => (b, 2)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param <A> the type of the first element in the resulting BiIterator.
     * @param <B> the type of the second element in the resulting BiIterator.
     * @param c the original Iterable to be unzipped, or {@code null} to return an empty result.
     * @param unzip a BiConsumer that takes an element from the original Iterable and a Pair to be filled with the resulting elements for the BiIterator.
     * @return a BiIterator that will iterate over the elements created by <i>unzip</i>, or an empty one if {@code c} is {@code null}.
     * @throws IllegalArgumentException if {@code unzip} is {@code null}.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     * @see N#unzip(Iterable, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> c, final BiConsumer<? super T, Pair<A, B>> unzip)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzip, cs.unzip);

        return BiIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * Unzips an Iterator into a TriIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a:1:x", "b:2:y").iterator();
     * TriIterator<String, Integer, String> result = Iterators.unzip3(iter, (s, triple) -> {
     *     String[] parts = s.split(":");
     *     triple.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
     * });
     * // Iterates through (a, 1, x), (b, 2, y)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterator.
     * @param <A> the type of the first element in the resulting TriIterator.
     * @param <B> the type of the second element in the resulting TriIterator.
     * @param <C> the type of the third element in the resulting TriIterator.
     * @param iter the original Iterator to be unzipped, or {@code null} to return an empty result.
     * @param unzip a BiConsumer that takes an element from the original Iterator and a Triple to be filled with the resulting elements for the TriIterator.
     * @return a TriIterator that will iterate over the elements created by <i>unzip</i>, or an empty one if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code unzip} is {@code null}.
     * @deprecated replaced by {@link TriIterator#unzip(Iterator, BiConsumer)}
     * @see TriIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzipToLists(Supplier)
     * @see TriIterator#unzipToSets(Supplier)
     */
    @Deprecated
    public static <T, A, B, C> TriIterator<A, B, C> unzip3(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzip)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzip, cs.unzip);

        return TriIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a TriIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a:1:x", "b:2:y");
     * TriIterator<String, Integer, String> result = Iterators.unzip3(list, (s, triple) -> {
     *     String[] parts = s.split(":");
     *     triple.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
     * });
     * // result.next() => (a, 1, x)
     * // result.next() => (b, 2, y)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param <A> the type of the first element in the resulting TriIterator.
     * @param <B> the type of the second element in the resulting {@code TriIterator}.
     * @param <C> the type of the third element in the resulting {@code TriIterator}.
     * @param c the original {@code Iterable} to be unzipped, or {@code null} to return an empty result.
     * @param unzip a {@code BiConsumer} that takes an element from the original {@code Iterable} and a {@code Triple} to be filled with the resulting elements for the {@code TriIterator}.
     * @return a {@code TriIterator} that will iterate over the elements created by {@code unzip}, or an empty one if {@code c} is {@code null}.
     * @throws IllegalArgumentException if {@code unzip} is {@code null}.
     * @deprecated replaced by {@link TriIterator#unzip(Iterable, BiConsumer)}
     * @see TriIterator#unzip(Iterable, BiConsumer)
     * @see TriIterator#unzipToLists(Supplier)
     * @see TriIterator#unzipToSets(Supplier)
     */
    @Deprecated
    public static <T, A, B, C> TriIterator<A, B, C> unzip3(final Iterable<? extends T> c, final BiConsumer<? super T, Triple<A, B, C>> unzip)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzip, cs.unzip);

        return TriIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * <p><b>Note:</b> this is the eager form; {@link #skip(Iterator, long)} is the lazy equivalent that wraps
     * the iterator instead of consuming it immediately.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * long advanced = Iterators.advance(iter, 3);
     * // advanced => 3
     * // iter.next() => 4 (iterator is now at position 3)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B").iterator();
     * long advanced2 = Iterators.advance(iter2, 10);
     * // advanced2 => 2 (only 2 elements were available)
     * // iter2.hasNext() => false
     * }</pre>
     *
     * @param iterator the iterator to be advanced, or {@code null} to return {@code 0}.
     * @param numberToAdvance the number of elements to advance the iterator.
     * @return the actual number of elements the iterator was advanced, or {@code 0} if {@code iterator} is {@code null}.
     * @throws IllegalArgumentException if {@code numberToAdvance} is negative.
     * @see #skip(Iterator, long)
     */
    public static long advance(final Iterator<?> iterator, final long numberToAdvance) throws IllegalArgumentException {
        N.checkArgNotNegative(numberToAdvance, cs.numberToAdvance);

        if (iterator == null) {
            return 0;
        }

        long i;

        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }

        return i;
    }

    /**
     * Skips the first {@code n} elements of the provided iterator and returns a new {@code ObjIterator} starting from the (n+1)th element.
     * If {@code n} is greater than the size of the iterator, an empty {@code ObjIterator} will be returned.
     *
     * <p>This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * <p><b>Note:</b> {@link #advance(Iterator, long)} is the eager equivalent; it consumes the iterator
     * immediately and returns the number of elements actually advanced.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.skip(iter, 2);
     * // Yields: 3, 4, 5 (skips first 2 elements)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * ObjIterator<String> result2 = Iterators.skip(iter2, 10);
     * // result2.hasNext() => false (skipped all elements)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be skipped, or {@code null} to return an empty iterator.
     * @param n the number of elements to skip from the beginning of the iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator starting from the (n+1)th element, or an empty iterator if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #advance(Iterator, long)
     */
    public static <T> ObjIterator<T> skip(final Iterator<? extends T> iter, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (iter == null) {
            return ObjIterator.empty();
        } else if (n <= 0) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private boolean skipped = false;
            private long remaining = n;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            private void skip() {
                while (remaining > 0 && iter.hasNext()) {
                    iter.next();
                    remaining--;
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that is limited to the specified count of elements from the original iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.limit(iter, 3);
     * // Yields: 1, 2, 3 (limits to first 3 elements)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B").iterator();
     * ObjIterator<String> result2 = Iterators.limit(iter2, 10);
     * // Yields: "A", "B" (count exceeds available elements)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be limited, or {@code null} to return an empty iterator.
     * @param count the maximum number of elements to be iterated over from the original iterator.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements of the original iterator, or an empty iterator if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code count} is negative.
     */
    public static <T> ObjIterator<T> limit(final Iterator<? extends T> iter, final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (iter == null || count == 0) {
            return ObjIterator.empty();
        } else if (count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = iter.next();
                cnt--;
                return result;
            }
        };
    }

    /**
     * Returns a new ObjIterator that starts from the specified offset and is limited to the specified count of elements from the original Iterator.
     * This method combines both {@link #skip(Iterator, long)} and {@link #limit(Iterator, long)} operations in a single call.
     * A zero {@code count} returns an empty iterator without inspecting or consuming the source,
     * regardless of {@code offset}.
     *
     * <p>This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 1, 3);
     * // Yields: 2, 3, 4 (skips first element, then takes next 3)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C", "D", "E", "F").iterator();
     * ObjIterator<String> result2 = Iterators.skipAndLimit(iter2, 2, 2);
     * // Yields: "C", "D"
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator to be skipped and limited, or {@code null} to return an empty iterator.
     * @param offset the number of elements to skip from the beginning. Must be non-negative.
     * @param count the maximum number of elements to return after skipping. Must be non-negative.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements starting from the (offset+1)th element, or an empty iterator if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     * @see N#slice(Iterator, int, int)
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterator<? extends T> iter, final long offset, final long count) throws IllegalArgumentException {
        checkOffsetCount(offset, count);

        if (iter == null || count == 0) {
            return ObjIterator.empty();
        }

        if (offset == 0 && count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        } else if (offset == 0) {
            return limit(iter, count);
        } else if (count == Long.MAX_VALUE) {
            return skip(iter, offset);
        }

        return new ObjIterator<>() {
            private long cnt = count;
            private boolean skipped = false;
            private long remainingToSkip = offset;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return cnt > 0 && iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = iter.next();
                cnt--;
                return result;
            }

            private void skip() {
                while (remainingToSkip > 0 && iter.hasNext()) {
                    iter.next();
                    remainingToSkip--;
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that starts from the specified offset and is limited to the specified count of elements from the original {@code Iterable}.
     * For a non-null {@code iterable}, its iterator is obtained when this method is called;
     * a zero {@code count} does not traverse that iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> result = Iterators.skipAndLimit(list, 2, 3);
     * // Yields: 3, 4, 5 (skips 2, takes next 3)
     *
     * List<String> words = Arrays.asList("A", "B", "C", "D");
     * ObjIterator<String> result2 = Iterators.skipAndLimit(words, 1, 2);
     * // Yields: "B", "C"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param iterable the original {@code Iterable} to be skipped and limited, or {@code null} to return an empty iterator.
     * @param offset the number of elements to skip from the beginning of the {@code Iterable}.
     * @param count the maximum number of elements to be iterated over from the {@code Iterable} after skipping.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements of the original {@code Iterable} starting from the (offset+1)th element.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterable<? extends T> iterable, final long offset, final long count) throws IllegalArgumentException {
        checkOffsetCount(offset, count);

        return iterable == null ? ObjIterator.empty() : skipAndLimit(iterable.iterator(), offset, count);
    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed from the specified Iterable.
     * All {@code null} elements will be filtered out, returning only {@code non-null} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", null, "B", null, "C");
     * ObjIterator<String> iter = Iterators.skipNulls(list);
     * // Yields: "A", "B", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, null, 2, 3, null);
     * ObjIterator<Integer> iter2 = Iterators.skipNulls(numbers);
     * // Yields: 1, 2, 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterable.
     * @param c the iterable whose {@code null} elements should be skipped, or {@code null} to return an empty iterator.
     * @return an {@code ObjIterator} that iterates over only the {@code non-null} elements, or an empty iterator if {@code c} is {@code null}.
     */
    public static <T> ObjIterator<T> skipNulls(final Iterable<? extends T> c) {
        return filter(c, Fn.notNull());
    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed from the specified Iterator.
     * All {@code null} elements will be filtered out, returning only {@code non-null} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", null, "B", null, "C").iterator();
     * ObjIterator<String> result = Iterators.skipNulls(iter);
     * // Yields: "A", "B", "C"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, null, 2, 3, null).iterator();
     * ObjIterator<Integer> result2 = Iterators.skipNulls(iter2);
     * // Yields: 1, 2, 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator whose {@code null} elements should be skipped, or {@code null} to return an empty iterator.
     * @return an {@code ObjIterator} that iterates over only the {@code non-null} elements, or an empty iterator if {@code iter} is {@code null}.
     */
    public static <T> ObjIterator<T> skipNulls(final Iterator<? extends T> iter) {
        return filter(iter, Fn.notNull());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "A", "C", "B", "D");
     * ObjIterator<String> distinct = Iterators.distinct(list);
     * // Yields: "A", "B", "C", "D"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 1, 4);
     * ObjIterator<Integer> distinct2 = Iterators.distinct(numbers);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * <p><b>Memory:</b> every distinct element seen so far is retained in an internal {@link java.util.HashSet}
     * for the lifetime of the returned iterator, so memory grows with the number of distinct elements. Do not
     * run this to completion over an unbounded source.</p>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param c the original Iterable to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @return a new ObjIterator that will iterate over the distinct elements of the original Iterable, or an empty iterator if {@code c} is {@code null}.
     */
    public static <T> ObjIterator<T> distinct(final Iterable<? extends T> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return distinct(c.iterator());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "A", "C", "B", "D").iterator();
     * ObjIterator<String> distinct = Iterators.distinct(iter);
     * // Yields: "A", "B", "C", "D"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, 2, 2, 3, 1, 4).iterator();
     * ObjIterator<Integer> distinct2 = Iterators.distinct(iter2);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * <p><b>Memory:</b> every distinct element seen so far is retained in an internal {@link java.util.HashSet}
     * for the lifetime of the returned iterator, so memory grows with the number of distinct elements. Do not
     * run this to completion over an unbounded source.</p>
     *
     * @param <T> the type of elements in the original Iterator.
     * @param iter the original Iterator to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @return a new ObjIterator that will iterate over the distinct elements of the original Iterator, or an empty iterator if {@code iter} is {@code null}.
     */
    public static <T> ObjIterator<T> distinct(final Iterator<? extends T> iter) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<T> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        final T e = iter.next();

                        if (set.add(e)) {
                            next = e;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = NONE;
                return result;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} with distinct elements from the original {@code Iterable} based on a key derived from each element.
     * The key for each element is determined by the provided {@code Function}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Arrays.asList("apple", "apricot", "banana", "avocado");
     * ObjIterator<String> distinct = Iterators.distinctBy(words, s -> s.charAt(0));
     * // Yields: "apple", "banana" (distinct by first character)
     *
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 30),
     *     new Person("Bob", 25),
     *     new Person("Charlie", 30)
     * );
     * ObjIterator<Person> distinctByAge = Iterators.distinctBy(people, Person::getAge);
     * // Yields: Person("Alice", 30), Person("Bob", 25)
     * }</pre>
     *
     * <p><b>Memory:</b> every distinct key seen so far is retained in an internal {@link java.util.HashSet}
     * for the lifetime of the returned iterator, so memory grows with the number of distinct keys. Do not run
     * this to completion over an unbounded source.</p>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @param keyExtractor a {@code Function} that takes an element from the {@code Iterable} and returns a key. Elements with the same key are considered duplicates.
     * @return an {@code ObjIterator} that will iterate over the distinct elements of the original {@code Iterable} based on the keys derived from {@code keyExtractor}.
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}.
     */
    public static <T> ObjIterator<T> distinctBy(final Iterable<? extends T> c, final Function<? super T, ?> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        if (c == null) {
            return ObjIterator.empty();
        }

        return distinctBy(c.iterator(), keyExtractor);
    }

    /**
     * Returns an {@code ObjIterator} with distinct elements from the original iterator based on a key derived from each element.
     * The key for each element is determined by the provided {@code Function}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> words = Arrays.asList("apple", "apricot", "banana", "avocado").iterator();
     * ObjIterator<String> distinct = Iterators.distinctBy(words, s -> s.charAt(0));
     * // Yields: "apple", "banana" (distinct by first character)
     *
     * Iterator<String> names = Arrays.asList("Alice", "Bobby", "Bob", "Eve").iterator();
     * ObjIterator<String> distinctByLength = Iterators.distinctBy(names, String::length);
     * // Yields: "Alice", "Bob" (distinct by length)
     * }</pre>
     *
     * <p><b>Memory:</b> every distinct key seen so far is retained in an internal {@link java.util.HashSet}
     * for the lifetime of the returned iterator, so memory grows with the number of distinct keys. Do not run
     * this to completion over an unbounded source.</p>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @param keyExtractor a {@code Function} that takes an element from the iterator and returns a key. Elements with the same key are considered duplicates.
     * @return an {@code ObjIterator} that will iterate over the distinct elements of the original iterator based on the keys derived from {@code keyExtractor}.
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}.
     */
    public static <T> ObjIterator<T> distinctBy(final Iterator<? extends T> iter, final Function<? super T, ?> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<Object> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        final T e = iter.next();

                        if (set.add(keyExtractor.apply(e))) {
                            next = e;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = NONE;
                return result;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that only includes elements from the original {@code Iterable} that satisfy the provided {@code Predicate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> evens = Iterators.filter(numbers, n -> n % 2 == 0);
     * // Yields: 2, 4, 6
     *
     * List<String> words = Arrays.asList("apple", "banana", "apricot", "cherry");
     * ObjIterator<String> aWords = Iterators.filter(words, s -> s.startsWith("a"));
     * // Yields: "apple", "apricot"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be filtered, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. Only elements that return {@code true} are included in the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} that satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#filter(Iterable, Predicate)
     * @see Maps#filter(Map, BiPredicate)
     */
    public static <T> ObjIterator<T> filter(final Iterable<? extends T> c, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return filter(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that only includes elements from the original iterator that satisfy the provided {@code Predicate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6).iterator();
     * ObjIterator<Integer> evens = Iterators.filter(iter, n -> n % 2 == 0);
     * // Yields: 2, 4, 6
     *
     * Iterator<String> words = Arrays.asList("hello", "world", "hi", "java").iterator();
     * ObjIterator<String> longWords = Iterators.filter(words, s -> s.length() > 3);
     * // Yields: "hello", "world", "java"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be filtered, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. Only elements that return {@code true} are included in the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator that satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#filter(Iterator, Predicate)
     * @see Maps#filter(Map, BiPredicate)
     */
    public static <T> ObjIterator<T> filter(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        final T e = iter.next();

                        if (predicate.test(e)) {
                            next = e;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = NONE;
                return result;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration stops when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.takeWhile(numbers, n -> n < 4);
     * // Yields: 1, 2, 3 (stops at 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> shortWords = Iterators.takeWhile(words, s -> s.startsWith("a"));
     * // Yields: "a", "ab", "abc" (stops at "b")
     * }</pre>
     *
     * <p><b>The stopping element is consumed and discarded:</b> the element that first fails {@code predicate} has
     * already been pulled from the source in order to test it, and it is not emitted, so it is lost to anyone who
     * keeps using the source afterwards. Use {@link #takeWhileInclusive(Iterable, Predicate)} to emit it instead.</p>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration continues as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #takeWhileInclusive(Iterable, Predicate)
     */
    public static <T> ObjIterator<T> takeWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhile(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration stops when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.takeWhile(iter, n -> n < 4);
     * // Yields: 1, 2, 3 (stops at 4)
     *
     * Iterator<String> words = Arrays.asList("short", "text", "verylongword", "a").iterator();
     * ObjIterator<String> shortWords = Iterators.takeWhile(words, s -> s.length() < 10);
     * // Yields: "short", "text" (stops at "verylongword")
     * }</pre>
     *
     * <p><b>The stopping element is consumed and discarded:</b> the element that first fails {@code predicate} has
     * already been pulled from {@code iter} in order to test it, and it is not emitted - so continuing to read
     * {@code iter} afterwards resumes <i>after</i> that element. Use
     * {@link #takeWhileInclusive(Iterator, Predicate)} to emit it instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> src = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> taken = Iterators.takeWhile(src, n -> n < 3);
     * // taken yields 1, 2 - and 3 has been consumed from src, which now continues at 4
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration continues as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator as long as they satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #takeWhileInclusive(Iterator, Predicate)
     */
    public static <T> ObjIterator<T> takeWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    final T e = iter.next();

                    if (predicate.test(e)) {
                        next = e;
                    } else {
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = NONE;
                return result;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration stops after the first element that does not satisfy the {@code Predicate} is encountered, but includes that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.takeWhileInclusive(numbers, n -> n < 4);
     * // Yields: 1, 2, 3, 4 (includes 4, then stops)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> result2 = Iterators.takeWhileInclusive(words, s -> s.startsWith("a"));
     * // Yields: "a", "ab", "abc", "b" (includes "b", then stops)
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration continues as long as the {@code Predicate} returns {@code true}, including the first element that returns {@code false}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} as long as they satisfy the provided {@code Predicate}, including the first element that does not satisfy the {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterable<? extends T> c, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhileInclusive(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration stops after the first element that does not satisfy the {@code Predicate} is encountered, but includes that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.takeWhileInclusive(iter, n -> n < 4);
     * // Yields: 1, 2, 3, 4 (includes 4, then stops)
     *
     * Iterator<String> words = Arrays.asList("cat", "car", "dog", "cap").iterator();
     * ObjIterator<String> result2 = Iterators.takeWhileInclusive(words, s -> s.startsWith("ca"));
     * // Yields: "cat", "car", "dog" (includes "dog", then stops)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration continues as long as the {@code Predicate} returns {@code true}, including the first element that returns {@code false}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator as long as they satisfy the provided {@code Predicate}, including the first element that does not satisfy the {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterator<? extends T> iter, final Predicate<? super T> predicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    final T e = iter.next();

                    next = e;
                    // The first non-matching element is still emitted - it is simply the last one.
                    hasMore = predicate.test(e);
                }

                return next != NONE;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = NONE;
                return result;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that skips elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration begins when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.dropWhile(numbers, n -> n < 4);
     * // Yields: 4, 5, 2, 1 (drops 1, 2, 3, starts from 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> result2 = Iterators.dropWhile(words, s -> s.length() < 3);
     * // Yields: "abc", "b", "c" (drops "a", "ab", starts from "abc")
     * }</pre>
     *
     * <p>This is equivalent to {@code skipUntil(c, predicate.negate())}.</p>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration skips elements as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} starting from the first element that does not satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #skipUntil(Iterable, Predicate)
     */
    public static <T> ObjIterator<T> dropWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return dropWhile(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that skips elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration begins when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.dropWhile(iter, n -> n < 4);
     * // Yields: 4, 5, 2, 1 (drops 1, 2, 3, starts from 4)
     *
     * Iterator<String> words = Arrays.asList("short", "tiny", "verylongword", "a").iterator();
     * ObjIterator<String> result2 = Iterators.dropWhile(words, s -> s.length() < 10);
     * // Yields: "verylongword", "a" (drops "short", "tiny", starts from "verylongword")
     * }</pre>
     *
     * <p>This is equivalent to {@code skipUntil(iter, predicate.negate())}.</p>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration skips elements as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator starting from the first element that does not satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #skipUntil(Iterator, Predicate)
     */
    public static <T> ObjIterator<T> dropWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;
            private boolean hasDropped = false;

            @Override
            public boolean hasNext() {
                if (!hasDropped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            next = NONE;
                        } else {
                            hasDropped = true;
                            break;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Skips elements in the provided {@code Iterable} until the provided {@code Predicate} returns {@code true}.
     * This method can be used to ignore elements in an {@code Iterable} until a certain condition is met.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.skipUntil(numbers, n -> n >= 4);
     * // Yields: 4, 5, 2, 1 (skips until finding n >= 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "abcd", "b");
     * ObjIterator<String> result2 = Iterators.skipUntil(words, s -> s.length() >= 4);
     * // Yields: "abcd", "b" (skips until finding length >= 4)
     * }</pre>
     *
     * <p>This is equivalent to {@code dropWhile(c, predicate.negate())}.</p>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests elements from the original {@code Iterable}.
     * @return an {@code ObjIterator} that will iterate over the remaining elements starting with the first element for which the {@code Predicate} returns {@code true} (that element is included).
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #dropWhile(Iterable, Predicate)
     */
    public static <T> ObjIterator<T> skipUntil(final Iterable<? extends T> c, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return skipUntil(c.iterator(), predicate);
    }

    /**
     * Skips elements in the provided iterator until the provided {@code Predicate} returns {@code true}.
     * This method can be used to ignore elements in an iterator until a certain condition is met.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.skipUntil(iter, n -> n >= 4);
     * // Yields: 4, 5, 2, 1 (skips until finding n >= 4)
     *
     * Iterator<String> words = Arrays.asList("cat", "dog", "elephant", "ant").iterator();
     * ObjIterator<String> result2 = Iterators.skipUntil(words, s -> s.length() > 5);
     * // Yields: "elephant", "ant" (skips until finding length > 5)
     * }</pre>
     *
     * <p>This is equivalent to {@code dropWhile(iter, predicate.negate())}.</p>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests elements from the original iterator.
     * @return an {@code ObjIterator} that will iterate over the remaining elements starting with the first element for which the {@code Predicate} returns {@code true} (that element is included).
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #dropWhile(Iterator, Predicate)
     */
    public static <T> ObjIterator<T> skipUntil(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_SENTINEL; //NOSONAR
            private T next = NONE;
            private boolean hasSkipped = false;

            @Override
            public boolean hasNext() {
                if (!hasSkipped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            hasSkipped = true;
                            break;
                        } else {
                            next = NONE;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} using the provided {@code Function} and returns an {@code ObjIterator} with the transformed elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjIterator<String> uppercase = Iterators.map(list, String::toUpperCase);
     * // Yields: "A", "B", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4);
     * ObjIterator<Integer> doubled = Iterators.map(numbers, n -> n * 2);
     * // Yields: 2, 4, 6, 8
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns a transformed element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> map(final Iterable<? extends T> c, final Function<? super T, ? extends U> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        if (c == null) {
            return ObjIterator.empty();
        }

        return map(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator using the provided {@code Function} and returns an {@code ObjIterator} with the transformed elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b").iterator();
     * ObjIterator<String> result = Iterators.map(iter, String::toUpperCase);
     * // result.next() => "A"
     * // result.next() => "B"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns a transformed element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> map(final Iterator<? extends T> iter, final Function<? super T, ? extends U> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public U next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.apply(iter.next());
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} into {@code Iterable}s using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for {@code flatMap} variants in {@code Iterators} and {@code N}:</p>
     * <ul>
     *   <li>{@link #flatMap(Iterable, Function) flatMap} (uppercase 'M') - transforms elements into an {@link java.lang.Iterable Iterable}.</li>
     *   <li>{@link #flatmap(Iterable, Function) flatmap} (lowercase 'm') - transforms elements into an array.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a,b", "c,d");
     * ObjIterator<String> result = Iterators.flatMap(list, s -> Arrays.asList(s.split(",")));
     * // Yields: "a", "b", "c", "d"
     *
     * List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
     * ObjIterator<Integer> flat = Iterators.flatMap(nested, x -> x);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns an {@code Iterable} of
     *               transformed elements; a {@code null} or empty result contributes nothing and is skipped.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterable<? extends T> c, final Function<? super T, ? extends Iterable<? extends U>> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        if (c == null) {
            return ObjIterator.empty();
        }

        return flatMap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator into {@code Iterable}s using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a,b", "c").iterator();
     * ObjIterator<String> result = Iterators.flatMap(iter, s -> Arrays.asList(s.split(",")));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns an {@code Iterable} of
     *               transformed elements; a {@code null} or empty result contributes nothing and is skipped.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterator<? extends T> iter, final Function<? super T, ? extends Iterable<? extends U>> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends U> cur = null;

            @Override
            public boolean hasNext() {
                if (cur == null || !cur.hasNext()) {
                    while (iter.hasNext()) {
                        final Iterable<? extends U> mapped = mapper.apply(iter.next());
                        cur = mapped == null ? null : mapped.iterator();

                        if (cur != null && cur.hasNext()) {
                            break;
                        }
                    }

                    if (cur != null && !cur.hasNext()) {
                        // Exhausted: drop the last mapped iterator so that an exhausted iterator which is still
                        // referenced does not pin it, matching flatmap(Iterator, Function) below.
                        cur = null;
                    }
                }

                return cur != null && cur.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public U next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} into arrays using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for {@code flatMap} variants in {@code Iterators} and {@code N}:</p>
     * <ul>
     *   <li>{@link #flatMap(Iterable, Function) flatMap} (uppercase 'M') - transforms elements into an {@link java.lang.Iterable Iterable}.</li>
     *   <li>{@link #flatmap(Iterable, Function) flatmap} (lowercase 'm') - transforms elements into an array.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a,b", "c,d");
     * ObjIterator<String> result = Iterators.flatmap(list, s -> s.split(","));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * // result.next() => "d"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns an array of
     *               transformed elements; a {@code null} or empty result contributes nothing and is skipped.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatmap(final Iterable<? extends T> c, final Function<? super T, ? extends U[]> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper); //NOSONAR

        if (c == null) {
            return ObjIterator.empty();
        }

        return flatmap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator into arrays using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a,b", "c").iterator();
     * ObjIterator<String> result = Iterators.flatmap(iter, s -> s.split(","));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns an array of
     *               transformed elements; a {@code null} or empty result contributes nothing and is skipped.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatmap(final Iterator<? extends T> iter, final Function<? super T, ? extends U[]> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper); //NOSONAR

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private U[] a = null;
            private int len = 0;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (cursor >= len) {
                    while (iter.hasNext()) {
                        a = mapper.apply(iter.next());
                        len = N.len(a);
                        cursor = 0;

                        if (len > 0) {
                            break;
                        }
                    }

                    if (cursor >= len) {
                        // Exhausted: drop the last mapped array so that an exhausted iterator which is still
                        // referenced does not pin it.
                        a = null;
                        len = 0;
                    }
                }

                return cursor < len;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element is available from the source iteration.
             */
            @Override
            public U next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Performs an action for each element of the given iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b").iterator();
     * List<String> list = new ArrayList<>();
     * Iterators.forEach(iter, list::add);
     * // list => ["a", "b"]
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the original iterator to be processed; {@code null} is treated as empty
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @throws IllegalArgumentException if {@code elementConsumer} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> elementConsumer)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iter, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given iterator and executes a final action upon completion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
     * List<String> list = new ArrayList<>();
     * Iterators.forEach(iter, list::add, () -> System.out.println("Done: " + list.size()));
     * // Adds all elements to list, then prints "Done: 3"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the original iterator to be processed; {@code null} is treated as empty, and
     *        {@code onComplete} still runs.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterator have been processed.
     * @throws IllegalArgumentException if any of {@code elementConsumer}, {@code onComplete} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEach(iter, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8).iterator();
     * Iterators.forEach(iter, 2, 3, i -> System.out.println(i));
     * // prints: 3, 4, 5 (skips first 2, processes next 3)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the original iterator to be processed; {@code null} is treated as empty.
     * @param offset the starting point in the iterator from where elements will be processed. Must be non-negative.
     * @param count the maximum number of elements to be processed from the iterator. Must be non-negative.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code elementConsumer}
     *         is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iter, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D", "E").iterator();
     * AtomicInteger processed = new AtomicInteger();
     * Iterators.forEach(iter, 1, 3,
     *     s -> processed.incrementAndGet(),
     *     () -> System.out.println("Processed: " + processed.get())
     * );
     * // Processes "B", "C", "D", then prints "Processed: 3"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the original iterator to be processed; {@code null} is treated as empty, and
     *        {@code onComplete} still runs.
     * @param offset the starting point in the iterator from where elements will be processed. Must be non-negative.
     * @param count the maximum number of elements to be processed from the iterator. Must be non-negative.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterator have been processed.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if any of
     *         {@code elementConsumer}, {@code onComplete} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEach(iter, offset, count, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each selected element of the given iterator, using the slicing and processing
     * configuration carried by the supplied {@link IterateOptions}.
     *
     * <p>The iterator is consumed by this terminal operation. The effective input is first sliced by
     * {@code offset} and {@code count}, then each selected element is passed to {@code elementConsumer}.
     * The {@code readThreads} and {@code queueSize} options are ignored for this overload: a single iterator has
     * only one source and there is no reader hand-off to buffer. With {@code processThreads == 0}, the calling
     * thread reads it; otherwise the processing workers serialize calls to its {@code hasNext()} and {@code next()}.</p>
     *
     * <p>This is also the only way to configure {@code processThreads}: the positional overloads cover slicing
     * alone ({@link #forEach(Iterator, long, long, Throwables.Consumer)}), so each tuning knob is named on the
     * {@code options} object rather than identified by its position in a run of numbers.</p>
     *
     * <p>When {@code processThreads > 0}, this method creates a new dedicated thread pool for this call and shuts it down
     * before returning. Element processing may then happen concurrently and the order of {@code elementConsumer} calls is
     * not guaranteed. To process an iterator on the library's shared executor (or a caller-supplied {@code Executor}), use
     * {@link N#forEachInParallel(Iterator, Throwables.Consumer, int)} or its {@code Executor}-accepting overload instead.</p>
     *
     * <p><b>Exception propagation is the same in both processing modes.</b> A checked exception from
     * {@code elementConsumer} propagates as {@code E}, a {@code RuntimeException} is rethrown as-is and an
     * {@code Error} as {@code Error} - whether the consumer ran on the calling thread
     * ({@code processThreads == 0}) or on a worker ({@code processThreads > 0}). Changing {@code processThreads}
     * is a tuning decision and therefore never changes which {@code catch} clause matches. When more than one
     * worker fails, the first failure is thrown and the rest are attached to it with
     * {@link Throwable#addSuppressed(Throwable)}.</p>
     *
     * <p>The one exception to that symmetry is <i>cancellation</i>: if the calling thread is interrupted while it
     * waits for the workers, this method publishes the cancellation, interrupts them, waits up to one second for
     * them to stop and then throws the {@link InterruptedException} wrapped in a {@code RuntimeException}. The
     * wait is bounded so that a consumer which ignores interruption cannot pin the caller indefinitely.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8).iterator();
     * List<Integer> result = new ArrayList<>();
     * Iterators.forEach(iter, IterateOptions.builder().offset(2).count(3).build(), result::add);
     * // result => [3, 4, 5] (skips first 2, processes next 3)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the iterator to consume; {@code null} is treated as empty.
     * @param options the slicing and processing configuration; {@code null} is treated as the default
     *        {@link IterateOptions} (no slicing, caller-thread processing). The {@code readThreads} and
     *        {@code queueSize} values are ignored.
     * @param elementConsumer the action to perform for each selected element.
     * @throws IllegalArgumentException if {@code elementConsumer} is {@code null}. Negative settings are rejected
     *         earlier, by {@code IterateOptions.builder()...build()}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @see #forEach(Iterator, IterateOptions, Throwables.Consumer, Throwables.Runnable)
     * @see IterateOptions
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final IterateOptions options,
            final Throwables.Consumer<? super T, E> elementConsumer) throws IllegalArgumentException, E, UncheckedInterruptedException {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iter, options, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each selected element of the given iterator, using the slicing and processing
     * configuration carried by the supplied {@link IterateOptions}, then runs a completion action if processing succeeds.
     *
     * <p>The iterator is consumed by this terminal operation. The effective input is first sliced by
     * {@code offset} and {@code count}, then each selected element is passed to {@code elementConsumer}.
     * The {@code readThreads} and {@code queueSize} options are ignored for this overload: a single iterator has
     * only one source and there is no reader hand-off to buffer. With {@code processThreads == 0}, the calling
     * thread reads it; otherwise the processing workers serialize calls to its {@code hasNext()} and {@code next()}.</p>
     *
     * <p>{@code onComplete} is invoked at most once, after all selected elements have been processed successfully.
     * If {@code elementConsumer} throws, {@code onComplete} is not invoked.</p>
     *
     * <p>This is also the only way to configure {@code processThreads}: the positional overloads cover slicing
     * alone ({@link #forEach(Iterator, long, long, Throwables.Consumer, Throwables.Runnable)}), so each tuning knob is
     * named on the {@code options} object rather than identified by its position in a run of numbers.</p>
     *
     * <p>When {@code processThreads > 0}, this method creates a new dedicated thread pool for this call and shuts it down
     * before returning. Element processing may then happen concurrently and the order of {@code elementConsumer} calls is
     * not guaranteed. To process an iterator on the library's shared executor (or a caller-supplied {@code Executor}), use
     * {@link N#forEachInParallel(Iterator, Throwables.Consumer, int)} or its {@code Executor}-accepting overload instead.</p>
     *
     * <p><b>Exception propagation is the same in both processing modes.</b> Checked exceptions from
     * {@code elementConsumer} and {@code onComplete} propagate as {@code E} and {@code E2}, a
     * {@code RuntimeException} is rethrown as-is and an {@code Error} as {@code Error} - whether the consumer ran
     * on the calling thread ({@code processThreads == 0}) or on a worker ({@code processThreads > 0}). Changing
     * {@code processThreads} is a tuning decision and therefore never changes which {@code catch} clause matches.
     * When more than one worker fails, the first failure is thrown and the rest are attached to it with
     * {@link Throwable#addSuppressed(Throwable)}. {@code onComplete} always runs on the calling thread.</p>
     *
     * <p>The one exception to that symmetry is <i>cancellation</i>: if the calling thread is interrupted while it
     * waits for the workers, this method publishes the cancellation, interrupts them, waits up to one second for
     * them to stop and then throws the {@link InterruptedException} wrapped in a {@code RuntimeException}. The
     * wait is bounded so that a consumer which ignores interruption cannot pin the caller indefinitely.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = IntStream.range(0, 100).iterator();
     * AtomicInteger sum = new AtomicInteger();
     * Iterators.forEach(iter, IterateOptions.builder().processThreads(4).build(),
     *     i -> sum.addAndGet(i),
     *     () -> System.out.println("Total: " + sum.get()));
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the iterator to consume; {@code null} is treated as empty, and {@code onComplete} still runs.
     * @param options the slicing and processing configuration; {@code null} is treated as the default
     *        {@link IterateOptions} (no slicing, caller-thread processing). The {@code readThreads} and
     *        {@code queueSize} values are ignored.
     * @param elementConsumer the action to perform for each selected element.
     * @param onComplete the action invoked after all selected elements have been processed; must not be {@code null}.
     * @throws IllegalArgumentException if any of {@code elementConsumer}, {@code onComplete} is {@code null}.
     *         Negative settings are rejected earlier, by {@code IterateOptions.builder()...build()}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     * @see #forEach(Iterator, IterateOptions, Throwables.Consumer)
     * @see IterateOptions
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final IterateOptions options,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, E, UncheckedInterruptedException, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final IterateOptions opts = options == null ? IterateOptions.DEFAULT : options;

        doForEach(iter, opts.offset(), opts.count(), opts.processThreads(), opts.queueSize(), elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     Arrays.asList(1, 2).iterator(),
     *     Arrays.asList(3, 4).iterator()
     * );
     * List<Integer> result = new ArrayList<>();
     * Iterators.forEach(iterators, result::add);
     * // result => [1, 2, 3, 4]
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed; {@code null}/empty means no elements
     *        are processed, and a {@code null} element inside is skipped.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws IllegalArgumentException if {@code elementConsumer} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iterators, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("A", "B").iterator(),
     *     Arrays.asList("C", "D").iterator()
     * );
     * List<String> result = new ArrayList<>();
     * Iterators.forEach(iterators, result::add, () -> System.out.println("Total: " + result.size()));
     * // Adds all elements, then prints "Total: 4"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed; {@code null}/empty means no elements
     *        are processed and {@code onComplete} still runs, and a {@code null} element inside is skipped.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterators have been processed.
     * @throws IllegalArgumentException if any of {@code elementConsumer}, {@code onComplete} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEach(iterators, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     *
     * <p>The two leading {@code long} arguments are {@code offset}/{@code count} (slicing). To configure reading or
     * processing threads as well, use the {@link IterateOptions} builder overload
     * {@link #forEach(Collection, IterateOptions, Throwables.Consumer)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     Arrays.asList(1, 2, 3).iterator(),
     *     Arrays.asList(4, 5, 6).iterator()
     * );
     * List<Integer> result = new ArrayList<>();
     * Iterators.forEach(iterators, 2, 3, result::add);
     * // result => [3, 4, 5] (skips first 2 elements, processes next 3)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed; {@code null}/empty means no elements
     *        are processed, and a {@code null} element inside is skipped.
     * @param offset the starting point in the iterators from where elements will be processed. Must be non-negative.
     * @param count the maximum number of elements to be processed from the iterators. Must be non-negative.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code elementConsumer}
     *         is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iterators, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * <p>The two leading {@code long} arguments are {@code offset}/{@code count} (slicing). To configure reading or
     * processing threads as well, use the {@link IterateOptions} builder overload
     * {@link #forEach(Collection, IterateOptions, Throwables.Consumer, Throwables.Runnable)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("A", "B", "C").iterator(),
     *     Arrays.asList("D", "E", "F").iterator()
     * );
     * List<String> result = new ArrayList<>();
     * Iterators.forEach(iterators, 1, 3, result::add, () -> System.out.println("Done"));
     * // result => ["B", "C", "D"], then prints "Done"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed; {@code null}/empty means no elements
     *        are processed and {@code onComplete} still runs, and a {@code null} element inside is skipped.
     * @param offset the starting point in the iterators from where elements will be processed. Must be non-negative.
     * @param count the maximum number of elements to be processed from the iterators. Must be non-negative.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterators have been processed.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if any of
     *         {@code elementConsumer}, {@code onComplete} is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEach(iterators, offset, count, 0, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each selected element of the given collection of iterators, using the slicing,
     * reading and processing configuration carried by the supplied {@link IterateOptions}.
     *
     * <p>Each iterator is consumed by this terminal operation. The combined iterator stream is first sliced by
     * {@code offset} and {@code count}, then each selected element is passed
     * to {@code elementConsumer}. A {@code null} or empty {@code iterators} collection has no elements to process,
     * and a {@code null} element inside {@code iterators} is skipped, whatever {@code readThreads} is set to.</p>
     *
     * <p>This is also the only way to configure {@code readThreads}/{@code processThreads}/{@code queueSize}: the
     * positional overloads cover slicing alone ({@link #forEach(Collection, long, long, Throwables.Consumer)}), so each
     * tuning knob is named on the {@code options} object rather than identified by its position in a run of numbers.</p>
     *
     * <p>When {@code readThreads > 0}, iterator reading may happen concurrently. When {@code processThreads > 0},
     * this method creates a new dedicated thread pool for this call and shuts it down before returning.
     * Element processing may then happen concurrently and the order of {@code elementConsumer} calls is not guaranteed.
     * To process a single iterator on the library's shared executor (or a caller-supplied {@code Executor}), use
     * {@link N#forEachInParallel(Iterator, Throwables.Consumer, int)} or its {@code Executor}-accepting overload instead.</p>
     *
     * <p><b>{@code offset}/{@code count} are not a stable selection once {@code readThreads > 0}.</b> They count
     * elements in the order the readers happen to deliver them, so with more than one iterator being read
     * concurrently a given offset selects a different subset from run to run - the count is honoured, the
     * <i>identity</i> of the selected elements is not. Slice with {@code readThreads == 0} whenever which
     * elements are selected matters.</p>
     *
     * <p><b>Exception propagation is the same in both processing modes.</b> A checked exception from
     * {@code elementConsumer} propagates as {@code E}, a {@code RuntimeException} is rethrown as-is and an
     * {@code Error} as {@code Error} - whether the consumer ran on the calling thread
     * ({@code processThreads == 0}) or on a worker ({@code processThreads > 0}). Changing {@code processThreads}
     * is a tuning decision and therefore never changes which {@code catch} clause matches. When more than one
     * worker fails, the first failure is thrown and the rest are attached to it with
     * {@link Throwable#addSuppressed(Throwable)}.</p>
     *
     * <p>The one exception to that symmetry is <i>cancellation</i>: if the calling thread is interrupted while it
     * waits for the workers, this method publishes the cancellation, interrupts them, waits up to one second for
     * them to stop and then throws the {@link InterruptedException} wrapped in a {@code RuntimeException}. The
     * wait is bounded so that a consumer which ignores interruption cannot pin the caller indefinitely.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     IntStream.range(0, 1000).iterator(),
     *     IntStream.range(1000, 2000).iterator()
     * );
     * AtomicInteger sum = new AtomicInteger();
     * Iterators.forEach(iterators,
     *     IterateOptions.builder().offset(100).count(500).readThreads(2).processThreads(4).queueSize(50).build(),
     *     i -> sum.addAndGet(i));
     * // Skips first 100 elements, processes next 500 with 2 read threads and 4 process threads
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the collection of iterators to consume; {@code null} or empty means no elements are processed.
     * @param options the slicing, reading and processing configuration; {@code null} is treated as the default
     *        {@link IterateOptions} (no slicing, caller-thread reading and processing).
     * @param elementConsumer the action to perform for each selected element.
     * @throws IllegalArgumentException if {@code elementConsumer} is {@code null}. Negative settings are rejected
     *         earlier, by {@code IterateOptions.builder()...build()}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @see #forEach(Collection, IterateOptions, Throwables.Consumer, Throwables.Runnable)
     * @see IterateOptions
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final IterateOptions options,
            final Throwables.Consumer<? super T, E> elementConsumer) throws IllegalArgumentException, E, UncheckedInterruptedException {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);

        forEach(iterators, options, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each selected element of the given collection of iterators, using the slicing,
     * reading and processing configuration carried by the supplied {@link IterateOptions}, then runs a completion
     * action if processing succeeds.
     *
     * <p>Each iterator is consumed by this terminal operation. The combined iterator stream is first sliced by
     * {@code offset} and {@code count}, then each selected element is passed
     * to {@code elementConsumer}. A {@code null} or empty {@code iterators} collection has no elements to process,
     * but {@code onComplete} is still invoked. A {@code null} element inside {@code iterators} is skipped, whatever
     * {@code readThreads} is set to.</p>
     *
     * <p>{@code onComplete} is invoked at most once, after all selected elements have been processed successfully.
     * If {@code elementConsumer} throws, {@code onComplete} is not invoked.</p>
     *
     * <p>This is also the only way to configure {@code readThreads}/{@code processThreads}/{@code queueSize}: the
     * positional overloads cover slicing alone ({@link #forEach(Collection, long, long, Throwables.Consumer, Throwables.Runnable)}),
     * so each tuning knob is named on the {@code options} object rather than identified by its position in a run of numbers.</p>
     *
     * <p>When {@code readThreads > 0}, iterator reading may happen concurrently. When {@code processThreads > 0},
     * this method creates a new dedicated thread pool for this call and shuts it down before returning.
     * Element processing may then happen concurrently and the order of {@code elementConsumer} calls is not guaranteed.
     * To process a single iterator on the library's shared executor (or a caller-supplied {@code Executor}), use
     * {@link N#forEachInParallel(Iterator, Throwables.Consumer, int)} or its {@code Executor}-accepting overload instead.</p>
     *
     * <p><b>{@code offset}/{@code count} are not a stable selection once {@code readThreads > 0}.</b> They count
     * elements in the order the readers happen to deliver them, so with more than one iterator being read
     * concurrently a given offset selects a different subset from run to run - the count is honoured, the
     * <i>identity</i> of the selected elements is not. Slice with {@code readThreads == 0} whenever which
     * elements are selected matters.</p>
     *
     * <p><b>Exception propagation is the same in both processing modes.</b> Checked exceptions from
     * {@code elementConsumer} and {@code onComplete} propagate as {@code E} and {@code E2}, a
     * {@code RuntimeException} is rethrown as-is and an {@code Error} as {@code Error} - whether the consumer ran
     * on the calling thread ({@code processThreads == 0}) or on a worker ({@code processThreads > 0}). Changing
     * {@code processThreads} is a tuning decision and therefore never changes which {@code catch} clause matches.
     * When more than one worker fails, the first failure is thrown and the rest are attached to it with
     * {@link Throwable#addSuppressed(Throwable)}. {@code onComplete} always runs on the calling thread.</p>
     *
     * <p>The one exception to that symmetry is <i>cancellation</i>: if the calling thread is interrupted while it
     * waits for the workers, this method publishes the cancellation, interrupts them, waits up to one second for
     * them to stop and then throws the {@link InterruptedException} wrapped in a {@code RuntimeException}. The
     * wait is bounded so that a consumer which ignores interruption cannot pin the caller indefinitely.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (java.util.stream.Stream<String> lines1 = Files.lines(Paths.get("file1.txt"));
     *         java.util.stream.Stream<String> lines2 = Files.lines(Paths.get("file2.txt"))) {
     *     List<Iterator<String>> iterators = Arrays.asList(lines1.iterator(), lines2.iterator());
     *     AtomicInteger lineCount = new AtomicInteger();
     *     Iterators.forEach(iterators,
     *         IterateOptions.builder().readThreads(2).processThreads(4).queueSize(100).build(),
     *         line -> lineCount.incrementAndGet(),
     *         () -> System.out.println("Total lines: " + lineCount.get()));
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the collection of iterators to consume; {@code null} or empty means no elements are processed.
     * @param options the slicing, reading and processing configuration; {@code null} is treated as the default
     *        {@link IterateOptions} (no slicing, caller-thread reading and processing).
     * @param elementConsumer the action to perform for each selected element.
     * @param onComplete the action invoked after all selected elements have been processed; must not be {@code null}.
     * @throws IllegalArgumentException if any of {@code elementConsumer}, {@code onComplete} is {@code null}.
     *         Negative settings are rejected earlier, by {@code IterateOptions.builder()...build()}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     * @see #forEach(Collection, IterateOptions, Throwables.Consumer)
     * @see IterateOptions
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final IterateOptions options, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, E, UncheckedInterruptedException, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final IterateOptions opts = options == null ? IterateOptions.DEFAULT : options;

        doForEach(iterators, opts.offset(), opts.count(), opts.readThreads(), opts.processThreads(), opts.queueSize(), elementConsumer, onComplete);
    }

    /**
     * Validates that the offset and count parameters are non-negative (long version).
     * This is a package-private utility method used internally for parameter validation.
     *
     * @param offset the offset value to check.
     * @param count the count value to check.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    static void checkOffsetCount(final long offset, final long count) throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);
    }

    /**
     * Returns {@code iterators} with every {@code null} element removed, or {@code iterators} itself when it has
     * none (the common case, which then costs one scan and no allocation).
     *
     * @param <T> the element type.
     * @param iterators the iterators to normalize; must not be {@code null}.
     * @return a collection with no {@code null} elements.
     */
    private static <T> Collection<? extends Iterator<? extends T>> withoutNulls(final Collection<? extends Iterator<? extends T>> iterators) {
        boolean hasNull = false;

        for (final Iterator<? extends T> e : iterators) {
            if (e == null) {
                hasNull = true;
                break;
            }
        }

        if (!hasNull) {
            return iterators;
        }

        final List<Iterator<? extends T>> result = new ArrayList<>(iterators.size());

        for (final Iterator<? extends T> e : iterators) {
            if (e != null) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Records {@code e} as the failure of a parallel {@code forEach}, keeping the first failure as the primary one
     * and attaching every later failure to it with {@link Throwable#addSuppressed(Throwable)}.
     *
     * @param errorHolder where the primary failure is published; also the monitor that serialises the update.
     * @param e the failure to record.
     */
    private static void recordFailure(final AtomicReference<Throwable> errorHolder, final Throwable e) {
        synchronized (errorHolder) {
            final Throwable primary = errorHolder.get();

            if (primary == null) {
                errorHolder.set(e);
            } else if (primary != e) {
                // A consumer is allowed to throw a cached exception instance, and Throwable rejects
                // suppressing an exception onto itself.
                primary.addSuppressed(e);
            }
        }
    }

    /**
     * Returns the thread factory for a parallel {@code forEach} worker pool.
     *
     * <p>The threads are <b>daemon</b> threads: the caller may abandon them after
     * {@link #CANCELLATION_TIMEOUT_IN_MILLIS} when it is interrupted and a consumer ignores interruption, and
     * non-daemon threads would then keep the JVM alive indefinitely. They are also named, because the default
     * {@code Executors} names ({@code pool-7-thread-2}) cannot be attributed to a caller in a thread dump.</p>
     *
     * @param callId a discriminator that distinguishes concurrent {@code forEach} calls in a thread dump. Derived
     *        from an object created for the call rather than from a counter, so that this class keeps its
     *        documented "no mutable static fields" property.
     * @return a thread factory producing named daemon threads.
     */
    private static ThreadFactory newForEachThreadFactory(final int callId) {
        final String namePrefix = "Iterators-forEach-" + Integer.toHexString(callId) + "-";
        final AtomicInteger threadCount = new AtomicInteger();

        return r -> {
            final Thread t = new Thread(r, namePrefix + threadCount.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /**
     * Throws {@code t} exactly as it is, while telling the compiler it is an {@code X}.
     *
     * <p>Used to carry a worker thread's failure back out of the parallel {@code forEach} under the {@code throws E}
     * / {@code throws E2} the public methods already declare. The alternative - wrapping the checked exception in a
     * {@code RuntimeException} - makes the declared exception type a lie on the parallel path, so a
     * {@code catch (SomeCheckedException e)} that the compiler still requires stops matching.</p>
     *
     * <p>The declared {@code RuntimeException} return exists only so that call sites can write
     * {@code throw sneakyThrow(t);} and have the compiler see the statement as terminating; this method never
     * returns normally.</p>
     *
     * @param <X> the type the caller pretends {@code t} has; erased, so nothing is checked at run time.
     * @param t the throwable to rethrow; must not be {@code null}.
     * @return never returns.
     * @throws X always - {@code t} itself.
     */
    @SuppressWarnings("unchecked")
    private static <X extends Throwable> RuntimeException sneakyThrow(final Throwable t) throws X {
        throw (X) t;
    }

    /**
     * Shared implementation for the single-iterator {@code forEach} overloads. Kept private so that the public
     * entry points do not have to route through one another.
     *
     * @param <T> the element type.
     * @param <E> the exception type the element consumer may throw.
     * @param <E2> the exception type the completion action may throw.
     * @param iter the iterator to read from; {@code null} is treated as empty.
     * @param offset the number of elements to skip.
     * @param count the maximum number of elements to process after the offset.
     * @param processThreads the number of worker threads; 0 processes on the calling thread.
     * @param queueSize the size of the buffer between reading and processing; unused here, because a single
     *        iterator is read directly by the calling thread or by serialized processing workers.
     * @param elementConsumer the action to run for each selected element.
     * @param onComplete the action to run once every element has been processed.
     * @throws IllegalArgumentException if any numeric argument is negative, or either action is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    private static <T, E extends Exception, E2 extends Exception> void doForEach(final Iterator<? extends T> iter, final long offset, final long count,
            final int processThreads, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, E, UncheckedInterruptedException, E2 {
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);
        N.checkArgument(processThreads >= 0 && queueSize >= 0, "'processThreads'=%s and 'queueSize'=%s cannot be negative", processThreads, queueSize);

        if (iter == null) {
            onComplete.run();
            return;
        }

        if (processThreads == 0) {
            // Fast path for "walk this iterator on this thread", which is what every overload without
            // IterateOptions asks for. Delegating to doForEach(Collection, ..) would wrap the source in a
            // concat() view and a skipAndLimit() view - two extra virtual calls plus a few allocations per
            // call - to drive machinery that a single-threaded walk never uses.
            long idx = 0;

            while (idx++ < offset && iter.hasNext()) {
                iter.next();
            }

            long remaining = count;

            while (remaining-- > 0 && iter.hasNext()) {
                elementConsumer.accept(iter.next());
            }

            onComplete.run();

            return;
        }

        doForEach(Array.asList(iter), offset, count, 0, processThreads, queueSize, elementConsumer, onComplete);
    }

    /**
     * Shared implementation for the collection-of-iterators {@code forEach} overloads. Kept private so that the
     * public entry points do not have to route through one another.
     *
     * @param <T> the element type.
     * @param <E> the exception type the element consumer may throw.
     * @param <E2> the exception type the completion action may throw.
     * @param iterators the iterators to read from; {@code null} or empty processes nothing. {@code null} elements
     *        are skipped.
     * @param offset the number of elements to skip across the combined sequence.
     * @param count the maximum number of elements to process after the offset.
     * @param readThreads the number of reader threads; 0 reads directly on the calling thread or processing workers.
     * @param processThreads the number of worker threads; 0 processes on the calling thread.
     * @param queueSize the size of the buffer between reading and processing; only consulted when
     *        {@code readThreads > 0}, and 0 lets the implementation pick a size.
     * @param elementConsumer the action to run for each selected element.
     * @param onComplete the action to run once every element has been processed.
     * @throws IllegalArgumentException if any numeric argument is negative, or either action is {@code null}.
     * @throws E if {@code elementConsumer} throws while processing a selected element; worker-thread failures propagate unchanged.
     * @throws UncheckedInterruptedException if an interruption propagates while the calling thread awaits asynchronously read elements or parallel processing; its interrupt status is restored.
     * @throws E2 if {@code onComplete} throws after iteration completes successfully.
     */
    private static <T, E extends Exception, E2 extends Exception> void doForEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreads, final int processThreads, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, UncheckedInterruptedException, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);
        N.checkArgument(readThreads >= 0 && processThreads >= 0 && queueSize >= 0,
                "'readThreads'=%s, 'processThreads'=%s and 'queueSize'=%s cannot be negative", readThreads, processThreads, queueSize);
        N.checkArgNotNull(elementConsumer, cs.elementConsumer);
        N.checkArgNotNull(onComplete, cs.onComplete);

        // Stream.parallelConcatIterators dereferences every element and so throws NPE on a null one, while
        // concat(Collection) skips it. Normalise up front so that the reading mode - a pure tuning choice -
        // cannot decide whether a null iterator is an error or a no-op.
        final Collection<? extends Iterator<? extends T>> iters = iterators == null ? null : withoutNulls(iterators);

        // Emptiness is decided by the iterator, not by Collection.isEmpty() - see concat(Collection). Only
        // iters.size() below is still a size() read, and it feeds a debug log line.
        if (iters == null || !iters.iterator().hasNext()) {
            // onComplete is documented to run after all elements have been processed - vacuously
            // true here; a collection of empty iterators runs it too, so the empty collection must.
            onComplete.run();

            return;
        }

        final long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("Start processing: sizeOfIterators={}, offset={}, count={}, readThreads={}, processThreads={}, queueSize={}", iters.size(), offset,
                    count, readThreads, processThreads, queueSize);
        }

        // Only concurrent reading needs a Stream; it owns the reader threads and is closed in the finally
        // below. Sequential reading is a plain concatenate-then-slice, and doing it on the iterators directly
        // avoids building a Stream pipeline whose derived stages each carry a parent-link close handler -
        // which made Stream.iterator() log "Remember to close .. because it has close handlers" on every
        // call, even though this method closes everything it opens.
        @SuppressWarnings("resource")
        Stream<T> stream = null;

        try {
            final Iterator<? extends T> iteratorII;

            // Only readThreads starts dedicated readers. queueSize sizes the hand-off buffer between the
            // readers and the consumer, so on its own it has nothing to buffer: honouring it here used to
            // start a reader thread behind the caller's back, which breaks sources bound to the calling thread.
            if (readThreads > 0) {
                stream = queueSize == 0 ? Stream.parallelConcatIterators(iters, readThreads) : Stream.parallelConcatIterators(iters, readThreads, queueSize);

                // Not stream.iterator(): that logs "Remember to close .. because it has close handlers" on every
                // call, telling the caller to close a Stream they never see and which the finally below always
                // closes. iteratorWithoutCloseWarning is the same iterator without the warning.
                iteratorII = Stream.iteratorWithoutCloseWarning(stream.skip(offset).limit(count));
            } else {
                iteratorII = skipAndLimit(concat(iters), offset, count);
            }

            if (processThreads == 0) {
                while (iteratorII.hasNext()) {
                    elementConsumer.accept(iteratorII.next());
                }

                onComplete.run();
            } else {
                final CountDownLatch countDownLatch = new CountDownLatch(processThreads);
                // AtomicReference, not Holder: every worker polls this on each iteration without
                // synchronizing, and Holder's field is not volatile - so a worker could miss a sibling's
                // failure entirely and keep consuming elements. recordFailure below still
                // serializes "first failure wins, the rest are suppressed onto it".
                final AtomicReference<Throwable> errorHolder = new AtomicReference<>();
                final ExecutorService executorService = Executors.newFixedThreadPool(processThreads,
                        newForEachThreadFactory(System.identityHashCode(errorHolder)));

                try {
                    // If execute() fails part-way - a rejected task, or OutOfMemoryError while creating a native
                    // thread for a very large processThreads - the latch was sized for processThreads and can
                    // never reach zero, so countDownLatch.await() below would block forever. Join on the pool
                    // instead: publishing the failure first makes any worker that did start stop at its next
                    // element, and waiting for them before unwinding keeps the finally block from closing the
                    // stream while a worker is still reading it.
                    try {
                        for (int i = 0; i < processThreads; i++) {
                            executorService.execute(() -> {
                                T element = null;
                                try {
                                    while (errorHolder.get() == null) {
                                        synchronized (iteratorII) {
                                            if (errorHolder.get() != null) {
                                                break;
                                            }

                                            if (iteratorII.hasNext()) {
                                                element = iteratorII.next();
                                            } else {
                                                break;
                                            }
                                        }

                                        elementConsumer.accept(element);
                                    }
                                } catch (final Throwable e) {
                                    recordFailure(errorHolder, e);
                                } finally {
                                    countDownLatch.countDown();
                                }
                            });
                        }
                    } catch (final Throwable e) {
                        recordFailure(errorHolder, e);

                        executorService.shutdown();

                        try {
                            executorService.awaitTermination(CANCELLATION_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
                        } catch (final InterruptedException e2) {
                            Thread.currentThread().interrupt();
                        }

                        throw sneakyThrow(errorHolder.get());
                    }

                    try {
                        countDownLatch.await();
                    } catch (final InterruptedException e) {
                        // Publish cancellation before interrupting the workers. This stops workers
                        // that finish (or ignore) their current consumer invocation from taking
                        // another element, while shutdownNow wakes interruptible consumers.
                        synchronized (errorHolder) {
                            final Throwable priorFailure = errorHolder.get();
                            errorHolder.set(e);

                            if (priorFailure != null && priorFailure != e) {
                                e.addSuppressed(priorFailure);
                            }
                        }

                        executorService.shutdownNow();

                        try {
                            // Give the workers a bounded moment to notice the cancellation: the finally block
                            // below closes the stream they read from, and a worker still inside
                            // iteratorII.next() would then be reading a closed source. Bounded, because a
                            // consumer that ignores interruption must not be able to pin the caller here.
                            executorService.awaitTermination(CANCELLATION_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
                        } catch (final InterruptedException e2) { // NOSONAR - re-asserted below by toRuntimeException(e, true)
                            Thread.currentThread().interrupt();
                        }

                        throw ExceptionUtil.toRuntimeException(e, true);
                    }

                    final Throwable failure = errorHolder.get();

                    if (failure != null) {
                        // Rethrow the worker's exception unchanged, so that the declared `throws E` holds on this
                        // path exactly as it does on the caller-thread path above. Wrapping it in a
                        // RuntimeException used to make `catch (SomeCheckedException e)` - which the compiler
                        // still demands, because E is inferred from the consumer - silently stop matching as soon
                        // as a caller set processThreads > 0. Errors and RuntimeExceptions pass through unchanged
                        // either way.
                        throw sneakyThrow(failure);
                    }

                    // Runs on the calling thread, so it can simply throw E2 - no capture-and-wrap needed.
                    onComplete.run();
                } finally {
                    executorService.shutdown();
                }
            }
        } finally {
            if (stream != null) {
                stream.close();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Finished processing. Elapsed time: {} ms", System.currentTimeMillis() - startTime);
            }
        }
    }

    /**
     * Immutable options for the {@link Iterators#forEach(Iterator, IterateOptions, Throwables.Consumer)}
     * and {@link Iterators#forEach(Collection, IterateOptions, Throwables.Consumer)} overloads.
     *
     * <p>This is the required way to configure multi-threaded {@code forEach} calls, and the recommended way to
     * configure sliced ones. The positional overloads such as
     * {@link Iterators#forEach(Collection, long, long, Throwables.Consumer)} cover {@code offset}/{@code count} only;
     * the thread and queue settings live here, named rather than identified by position.</p>
     *
     * <p>{@code offset} and {@code count} slice the combined element stream before processing.
     * {@code readThreads} reads the supplied iterators concurrently; it applies only to the
     * collection-of-iterators overloads and is ignored by the single-iterator overloads.
     * {@code processThreads} controls concurrent calls to the element consumer. With no dedicated readers,
     * processing workers also serialize source reads; both thread counts must be zero to keep reads on the calling thread.
     * {@code queueSize} sizes the hand-off buffer between the reader threads and the consumer, so it has no
     * effect unless {@code readThreads > 0}; {@code 0} asks the implementation to choose a size.</p>
     *
     * <p><b>Thread budget:</b> {@code processThreads} maps one-to-one onto platform threads created for the call
     * (daemon threads, named {@code Iterators-forEach-*}) and shut down before it returns, so keep it in the order
     * of the available cores rather than the number of elements. {@code readThreads} is handed to
     * {@link com.landawn.abacus.util.stream.Stream#parallelConcatIterators(Collection, int)}, which reads on the
     * library's shared pool. Those pool threads are daemon threads, so they never hold the JVM open; they do stay
     * alive for their keep-alive time (180 s) after the call, ready for the next caller. If the shared pool is
     * saturated the read runs on a private pool instead, which this call shuts down before it returns.</p>
     *
     * <p>All values default to "no slicing, caller-thread reading and processing": {@code offset = 0},
     * {@code count = Long.MAX_VALUE}, {@code readThreads = 0}, {@code processThreads = 0} and {@code queueSize = 0}.
     * None of them may be negative - {@code IterateOptions.builder()...build()} rejects a negative value rather than
     * deferring the failure to the {@code forEach} call.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> first = Arrays.asList("a", "b", "c");
     * List<String> second = Arrays.asList("d", "e", "f");
     * List<Iterator<String>> iterators = Arrays.asList(first.iterator(), second.iterator());
     *
     * Iterators.forEach(iterators,
     *     Iterators.IterateOptions.builder().offset(1).count(4).readThreads(2).processThreads(4).queueSize(50).build(),
     *     item -> System.out.println(item));
     * }</pre>
     *
     * @see Iterators#forEach(Iterator, IterateOptions, Throwables.Consumer)
     * @see Iterators#forEach(Iterator, IterateOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Collection, IterateOptions, Throwables.Consumer)
     * @see Iterators#forEach(Collection, IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    @Builder
    @Value
    @Accessors(fluent = true)
    public static final class IterateOptions {

        /**
         * The default configuration: no slicing, caller-thread reading and processing - that is,
         * {@code offset = 0}, {@code count = Long.MAX_VALUE} and {@code readThreads = processThreads = queueSize = 0}.
         *
         * <p>This is the instance a {@code forEach} overload uses when it is passed a {@code null} {@code options},
         * and it is equal to {@code IterateOptions.builder().build()}. Being immutable, it is safe to share.</p>
         */
        public static final IterateOptions DEFAULT = IterateOptions.builder().build();

        @Builder.Default
        private long offset = 0;

        @Builder.Default
        private long count = Long.MAX_VALUE;

        @Builder.Default
        private int readThreads = 0;

        @Builder.Default
        private int processThreads = 0;

        @Builder.Default
        private int queueSize = 0;

        /**
         * Creates a validated instance. Declared explicitly so that {@code IterateOptions.builder()...build()}, which
         * Lombok routes through this constructor, rejects a negative setting at the point where it was supplied
         * instead of at the eventual {@code forEach} call.
         *
         * @param offset the number of elements to skip; must not be negative.
         * @param count the maximum number of elements to process after the offset; must not be negative.
         * @param readThreads the number of reader threads; must not be negative.
         * @param processThreads the number of element-consumer threads; must not be negative.
         * @param queueSize the size of the reader/consumer hand-off buffer; must not be negative.
         * @throws IllegalArgumentException if any argument is negative.
         */
        IterateOptions(final long offset, final long count, final int readThreads, final int processThreads, final int queueSize)
                throws IllegalArgumentException {
            N.checkArgNotNegative(offset, cs.offset);
            N.checkArgNotNegative(count, cs.count);
            N.checkArgNotNegative(readThreads, cs.readThreads);
            N.checkArgNotNegative(processThreads, cs.processThreads);
            N.checkArgNotNegative(queueSize, cs.queueSize);

            this.offset = offset;
            this.count = count;
            this.readThreads = readThreads;
            this.processThreads = processThreads;
            this.queueSize = queueSize;
        }
    }
}
