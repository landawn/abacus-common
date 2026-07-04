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

import java.math.BigInteger;
import java.nio.LongBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.FL;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongMapMultiConsumer;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.function.LongTriPredicate;
import com.landawn.abacus.util.function.ToLongFunction;

/**
 * A specialized stream implementation for processing sequences of long values with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive long-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for long data types.
 *
 * <p>LongStream represents a sequence of long elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with long values,
 * avoiding boxing/unboxing overhead and offering long-specific utility methods for numerical computations.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for long operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for long primitives, avoiding boxing overhead</li>
 *   <li><b>Numerical Operations:</b> Specialized methods for mathematical computations and statistical analysis</li>
 *   <li><b>Range Generation:</b> Built-in support for generating long ranges and sequences</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large long datasets</li>
 *   <li><b>Resource Management:</b> Automatic resource cleanup and proper stream lifecycle management</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Stream Lifecycle:</b>
 * <ul>
 *   <li>A stream can only be consumed <b>ONCE</b> - after a terminal operation, the stream is closed</li>
 *   <li>Attempting to reuse a closed stream throws {@code IllegalStateException}</li>
 *   <li>Streams are automatically closed after terminal operations complete</li>
 *   <li>Use try-with-resources for streams created from I/O sources</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Mathematical Computations:</b> Numerical analysis, statistical calculations, aggregations</li>
 *   <li><b>Data Processing:</b> Processing large datasets of long numbers</li>
 *   <li><b>Range Operations:</b> Generating sequences, loops, and iterative computations</li>
 *   <li><b>Array Processing:</b> Efficient processing of long arrays and collections</li>
 *   <li><b>Index Operations:</b> Working with array indices and position-based computations</li>
 *   <li><b>Big Data:</b> Processing large numerical datasets requiring 64-bit precision</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic long stream operations
 * LongStream.of(1L, 2L, 3L, 4L, 5L)
 *     .filter(l -> l > 2)   // keeps values > 2
 *     .map(l -> l * 2)      // maps each value to double its value
 *     .sum();               // returns 24
 *
 * // Range-based operations for large sequences
 * LongStream.range(1L, 1_000_001L)   // contains numbers 1 to 1,000,000
 *     .filter(l -> l % 2 == 0)       // keeps even numbers only
 *     .limit(10)                     // keeps first 10 even numbers
 *     .toArray();                    // [2L, 4L, 6L, 8L, 10L, 12L, 14L, 16L, 18L, 20L]
 *
 * // Statistical operations
 * LongSummaryStatistics stats = LongStream.of(timestamps)
 *     .filter(time -> time > 0)   // filters valid timestamps
 *     .summaryStatistics();       // gets min, max, avg, count
 *
 * // Parallel processing for large datasets
 * long result = LongStream.range(1L, 10_000_000L)
 *     .parallel()              // switches to parallel processing
 *     .filter(this::isPrime)   // filters prime numbers
 *     .map(l -> l * l)         // maps each prime to its square
 *     .sum();                  // returns the sum of squares
 *
 * // Integration with other stream types
 * DoubleStream percentages = LongStream.of(counts)
 *     .mapToDouble(l -> l / 100.0)     // maps to percentages
 *     .filter(d -> d > 0.5);           // keeps values > 50%
 * }</pre>
 *
 * <p><b>Long-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(LongPredicate)} - Filter longs based on conditions</li>
 *   <li>{@code map(LongUnaryOperator)} - Transform long values</li>
 *   <li>{@code reduce(LongBinaryOperator)} - Reduce to single long value</li>
 *   <li>{@code sum()}, {@code average()}, {@code min()}, {@code max()} - Mathematical aggregations</li>
 *   <li>{@code range()}, {@code rangeClosed()} - Generate long sequences</li>
 *   <li>{@code mapToInt()}, {@code mapToDouble()}, {@code mapToObj()} - Convert to other stream types</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Long&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use LongStream instead of {@code Stream<Long>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 elements)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * <p><b>abacus {@code LongStream} vs. {@link java.util.stream.LongStream JDK LongStream} &mdash; how they differ:</b>
 * This {@code LongStream} mirrors the JDK's {@link java.util.stream.LongStream} (the same lazy, single-use,
 * primitive-{@code long} pipeline) but adds a much larger operation set and a few behavioral differences,
 * paralleling the relationship between the abacus and JDK object {@link Stream}s:
 *
 * <table border="1">
 *   <caption>abacus {@code LongStream} compared with {@code java.util.stream.LongStream}</caption>
 *   <tr><th>Aspect</th><th>abacus {@code LongStream}</th><th>JDK {@code java.util.stream.LongStream}</th></tr>
 *   <tr>
 *     <td>Operation set</td>
 *     <td>Much larger &mdash; e.g. {@code takeWhile}/{@code dropWhile} (independent of the JDK version),
 *         {@code collapse}, {@code scan}, {@code step}, {@code zipWith}/{@code mergeWith}, plus convenient
 *         terminals and {@link LongList} conversions.</td>
 *     <td>The standard, smaller set defined by {@code java.util.stream}.</td>
 *   </tr>
 *   <tr>
 *     <td>Companion primitive streams</td>
 *     <td>One of a full family: {@link ByteStream}, {@link CharStream}, {@link ShortStream}, {@link IntStream},
 *         {@code LongStream}, {@link FloatStream}, {@link DoubleStream}.</td>
 *     <td>Only {@code IntStream}, {@code LongStream}, {@code DoubleStream} exist.</td>
 *   </tr>
 *   <tr>
 *     <td>Resource lifecycle</td>
 *     <td>Closed automatically when a terminal operation completes.</td>
 *     <td>A terminal operation does not close the stream.</td>
 *   </tr>
 *   <tr>
 *     <td>Parallelism</td>
 *     <td>{@code parallel()} plus overloads taking an explicit thread count and a
 *         {@link java.util.concurrent.Executor}.</td>
 *     <td>{@code parallel()} on the shared common ForkJoinPool.</td>
 *   </tr>
 * </table>
 *
 * <p>Beyond those high-level differences, a few <i>same-named</i> operations behave differently from the JDK
 * &mdash; worth knowing when porting code; each point where the {@code JDK} differs is flagged with {@code &#9888;&#65039;}:</p>
 * <table border="1">
 *   <caption>How selected {@code LongStream} operations differ from JDK</caption>
 *   <thead>
 *     <tr><th>Operation</th><th>Behavior difference</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code min}/{@code max}/{@code reduce}/{@code findFirst}/{@code findAny}/{@code average}</td>
 *       <td><b><i>abacus</i></b>: returns {@code u.OptionalLong} (and {@code u.OptionalDouble} for {@code average()}) &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns {@code java.util.OptionalLong}/{@code OptionalDouble}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code count()}</td>
 *       <td><b><i>abacus</i></b>: always traverses the pipeline, so an upstream {@code peek}/{@code filter} still runs &middot; &#9888;&#65039; <b><i>JDK</i></b> (9+): may return the count without traversal when the element count is already known</td>
 *     </tr>
 *     <tr>
 *       <td>{@code peek}/{@code onEach}</td>
 *       <td><b><i>abacus</i></b>: always invokes the action for every element pulled &middot; &#9888;&#65039; <b><i>JDK</i></b>: the action "may not be invoked" for elements it optimizes away (e.g. under a short-circuiting {@code count()})</td>
 *     </tr>
 *     <tr>
 *       <td>{@code iterator()}</td>
 *       <td><b><i>abacus</i></b>: returns an extended {@code LongIterator} (and is deprecated) that does not auto-close the stream &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns a {@code java.util.PrimitiveIterator.OfLong}</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><b>Prefer the abacus {@code LongStream} when:</b> you want the expanded operation set, the additional
 * primitive stream types, automatic closing of I/O-backed pipelines, or tight interop with the abacus
 * {@link Stream} / {@link LongList} ecosystem.
 *
 * <p><b>Prefer the JDK {@link java.util.stream.LongStream} when:</b> you want to avoid an extra dependency,
 * or the surrounding code is standardized on {@code java.util.stream} and must exchange JDK stream types
 * across its API boundaries.
 *
 * <p>The two convert directly: {@link #from(java.util.stream.LongStream)} wraps a JDK {@code LongStream} and
 * {@link #toJdkStream()} returns one, while {@link #boxed()} bridges to the object stream and
 * {@link #asDoubleStream()} bridges to the wider primitive stream (use {@link #asFloatStream()} only
 * with care — it is a lossy narrowing conversion and is deprecated).
 *
 * @see StreamBase
 * @see IntStream
 * @see DoubleStream
 * @see FloatStream
 * @see Stream
 * @see LongIterator
 * @see LongList
 * @see LongSummaryStatistics
 * @see java.util.stream.LongStream
 * @see OptionalLong
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@com.landawn.abacus.annotation.Immutable
@LazyEvaluation
public abstract class LongStream extends StreamBase<Long, long[], LongPredicate, LongConsumer, OptionalLong, IndexedLong, LongIterator, LongStream> {

    static final Random RAND = new SecureRandom();

    LongStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     * This is an intermediate operation that filters elements based on the provided condition.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The predicate should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *       .filter(x -> x > 3L)
     *       .toArray();   // returns [4L, 5L]
     *
     * // Filter to keep even numbers
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *       .filter(x -> x % 2 == 0)
     *       .toArray();   // returns [2L, 4L]
     *
     * // Filter with no matches returns empty stream
     * LongStream.of(1L, 2L, 3L)
     *       .filter(x -> x > 100L)
     *       .count();   // returns 0
     *
     * // Filter on empty stream returns empty stream
     * LongStream.empty()
     *       .filter(x -> x > 0)
     *       .count();   // returns 0
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine if it should be included
     * @return a new stream consisting of the elements that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract LongStream filter(final LongPredicate predicate);

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
     * LongStream.of(1L, 2L, 3L, 4L, 5L, 2L, 1L)
     *       .takeWhile(x -> x < 4L)
     *       .toArray();   // returns [1L, 2L, 3L]
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop taking elements
     * @return a new stream consisting of elements from this stream until an element is encountered that doesn't match the predicate
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract LongStream takeWhile(final LongPredicate predicate);

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
     * LongStream.of(1L, 2L, 3L, 4L, 5L, 2L, 1L)
     *       .dropWhile(x -> x < 4L)
     *       .toArray();   // returns [4L, 5L, 2L, 1L]
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop dropping elements
     * @return a new stream consisting of the remaining elements of this stream after dropping elements
     *         while the given predicate returns {@code true}
     * @see Stream#dropWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract LongStream dropWhile(final LongPredicate predicate);

    /**
     * Returns a LongStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each long element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .map(x -> x * 2)
     *       .toArray();   // returns [2L, 4L, 6L, 8L]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to long
     * @return a new LongStream consisting of the results of applying the mapper function to each element
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream map(LongUnaryOperator mapper);

    /**
     * Returns an IntStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each long element to an int value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to ints (with potential truncation)
     * LongStream.of(100L, 200L, 300L)
     *       .mapToInt(l -> (int) l)
     *       .toArray();   // returns [100, 200, 300]
     *
     * // Extract lower 32 bits from longs
     * LongStream.of(0x123456789ABCDEFL)
     *       .mapToInt(l -> (int) (l & 0xFFFFFFFFL))
     *       .toArray();   // returns [0x89ABCDEF]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(LongUnaryOperator)
     * @see #mapToObj(LongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(LongToIntFunction mapper);

    /**
     * Returns a FloatStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each long element to a float value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to floats
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .mapToFloat(l -> (float) l)
     *       .toArray();   // returns [1.0f, 2.0f, 3.0f, 4.0f]
     *
     * // Calculate percentages
     * LongStream.of(25L, 50L, 75L, 100L)
     *       .mapToFloat(l -> l / 100.0f)
     *       .toArray();   // returns [0.25f, 0.5f, 0.75f, 1.0f]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to float
     * @return a new FloatStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(LongUnaryOperator)
     * @see #mapToDouble(LongToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream mapToFloat(LongToFloatFunction mapper);

    /**
     * Returns a DoubleStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each long element to a double value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to doubles
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .mapToDouble(l -> (double) l)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     *
     * // Calculate square roots
     * LongStream.of(1L, 4L, 9L, 16L)
     *       .mapToDouble(Math::sqrt)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to double
     * @return a new DoubleStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(LongUnaryOperator)
     * @see #mapToFloat(LongToFloatFunction)
     * @see java.util.stream.LongStream#mapToDouble(LongToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapToDouble(LongToDoubleFunction mapper);

    /**
     * Returns an object-valued Stream consisting of the results of applying the
     * given function to the elements of this stream. This is an intermediate operation
     * that transforms each long element to an object of type T using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to strings
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .mapToObj(String::valueOf)
     *       .toList();   // returns ["1", "2", "3", "4"]
     *
     * // Convert timestamps to Date objects
     * LongStream.of(1609459200000L, 1612137600000L)
     *       .mapToObj(Date::new)
     *       .toList();   // returns Date objects
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from long to T
     * @return a new Stream of objects resulting from applying the mapper function to each element of this stream
     * @see #map(LongUnaryOperator)
     * @see java.util.stream.LongStream#mapToObj(LongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(LongFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each mapped stream is
     * {@link BaseStream#close() closed} after its contents have been placed
     * into this stream. (If a mapped stream is {@code null} an empty stream
     * is used, instead.)
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each number to a range
     * LongStream.of(1L, 2L, 3L)
     *       .flatMap(n -> LongStream.range(0, n))
     *       .toLongList();   // returns [0, 0, 1, 0, 1, 2]
     *
     * // Duplicate each element
     * LongStream.of(1L, 2L, 3L)
     *       .flatMap(n -> LongStream.of(n, n))
     *       .toLongList();   // returns [1, 1, 2, 2, 3, 3]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to LongStream
     * @return a new {@link LongStream} consisting of the flattened contents of the mapped streams
     * @see Stream#flatMap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatMap(LongFunction<? extends LongStream> mapper);

    // public abstract LongStream flatmap(LongFunction<LongIterator> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(LongFunction) flatMap} (uppercase 'M') - transforms each element into a {@link LongStream}.</li>
     *   <li>{@link #flatmap(LongFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Long}.</li>
     *   <li>{@link #flatMapArray(LongFunction) flatMapArray} - transforms each element into a {@code long[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code LongStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Long} entry is replaced with the primitive default value
     * {@code 0L} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L)
     *           .flatmap(x -> Arrays.asList(x, x * 10L))
     *           .toArray();   // [1, 10, 2, 20]
     *
     * // null elements in a returned collection become 0L
     * LongStream.of(1L)
     *           .flatmap(x -> Arrays.asList((Long) null, 7L))
     *           .toArray();   // [0, 7]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to {@code Collection<Long>}
     * @return a new {@code LongStream} consisting of the flattened contents of the collections produced by the mapper
     * @see #flatMap(LongFunction)
     * @see #flatMapArray(LongFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to LongStream, flatmap maps to Collection<Long>, flatMapArray maps to long[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatmap(LongFunction<? extends Collection<Long>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of the array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each number to array of multiples
     * LongStream.of(2L, 3L)
     *       .flatMapArray(n -> new long[] {n, n * 10, n * 100})
     *       .toLongList();   // returns [2, 20, 200, 3, 30, 300]
     *
     * // Generate sequences based on each number
     * LongStream.of(1L, 2L, 3L)
     *       .flatMapArray(n -> new long[] {n, n + 10})
     *       .toLongList();   // returns [1, 11, 2, 12, 3, 13]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to long[]
     * @return a new {@code LongStream} consisting of the flattened contents of the arrays produced by the mapper
     * @see #flatMap(LongFunction)
     * @see #flattMap(LongFunction)
     * @see #flatMapToInt(LongFunction)
     * @see #flatMapToObj(LongFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to LongStream, flatMapArray maps to long[], flattMap maps to JDK java.util.stream.LongStream. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatMapArray(LongFunction<long[]> mapper); //NOSONAR

    /**
     * This method is deprecated and not supported.
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped collection produced by applying
     * the provided mapping function to each element.
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a Collection
     *               (parameter is not used as this method always throws)
     * @return the new stream (this method never returns as it always throws)
     * @throws UnsupportedOperationException always thrown as this method is not supported
     * @deprecated This method is not supported. Use {@link #flatmapToObj(LongFunction)} instead
     * @see #flatmapToObj(LongFunction)
     */
    @Deprecated
    @ParallelSupported
    @IntermediateOp
    LongStream flattmap(@SuppressWarnings("unused") final LongFunction<? extends Collection<Long>> mapper) throws UnsupportedOperationException { // NOSONAR
        throw new UnsupportedOperationException("Method 'flattmap' is deprecated and unsupported; use 'flatmapToObj' or 'flatMap' instead");
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped java.util.stream.LongStream
     * produced by applying the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert custom stream to JDK stream within flatMap
     * LongStream.of(1L, 3L, 5L)
     *       .flattMap(n -> java.util.stream.LongStream.range(n, n + 2))
     *       .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     *
     * // Integrate with JDK stream operations
     * LongStream.of(2L, 4L)
     *       .flattMap(n -> java.util.stream.LongStream.of(n, n * 2, n * 3))
     *       .toArray();   // returns [2L, 4L, 6L, 4L, 8L, 12L]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to java.util.stream.LongStream
     * @return a new {@code LongStream} consisting of the flattened contents of the mapped JDK {@code LongStream} instances
     * @see #flatMap(LongFunction)
     * @see #flatMapArray(LongFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to LongStream, flatMapArray maps to long[], flattMap maps to JDK java.util.stream.LongStream. Do not suggest renaming.
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flattMap(LongFunction<? extends java.util.stream.LongStream> mapper); //NOSONAR

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the provided
     * mapping function to each element. Each mapped stream is
     * {@link BaseStream#close() closed} after its contents have been placed
     * into this stream. (If a mapped stream is {@code null} an empty stream
     * is used, instead.)
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Extract int digits from long numbers
     * LongStream.of(123L, 456L)
     *       .flatMapToInt(n -> IntStream.of(
     *           (int)(n / 100), (int)((n / 10) % 10), (int)(n % 10)))
     *       .toArray();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Convert longs to int ranges
     * LongStream.of(2L, 3L)
     *       .flatMapToInt(n -> IntStream.range(0, (int)n))
     *       .toArray();   // returns [0, 1, 0, 1, 2]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to IntStream
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(LongFunction<? extends IntStream> mapper);

    /**
     * Returns a {@code FloatStream} consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the provided
     * mapping function to each element. Each mapped stream is
     * {@link BaseStream#close() closed} after its contents have been placed
     * into this stream. (If a mapped stream is {@code null} an empty stream
     * is used, instead.)
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate fractional values from longs
     * LongStream.of(1L, 2L)
     *       .flatMapToFloat(n -> FloatStream.of(n * 0.1f, n * 0.5f, n * 1.0f))
     *       .toArray();   // returns [0.1f, 0.5f, 1.0f, 0.2f, 1.0f, 2.0f]
     *
     * // Convert timestamps to normalized time ranges
     * LongStream.of(100L, 200L)
     *       .flatMapToFloat(n -> FloatStream.of(n / 100.0f, n / 50.0f))
     *       .toArray();   // returns [1.0f, 2.0f, 2.0f, 4.0f]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to FloatStream
     * @return a new {@link FloatStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatMapToFloat(LongFunction<? extends FloatStream> mapper);

    /**
     * Returns a {@code DoubleStream} consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the provided
     * mapping function to each element. Each mapped stream is
     * {@link BaseStream#close() closed} after its contents have been placed
     * into this stream. (If a mapped stream is {@code null} an empty stream
     * is used, instead.)
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate precision values from longs
     * LongStream.of(1L, 2L)
     *       .flatMapToDouble(n -> DoubleStream.of(n, n + 0.5, n + 1.0))
     *       .toArray();   // returns [1.0, 1.5, 2.0, 2.0, 2.5, 3.0]
     *
     * // Create mathematical series
     * LongStream.of(2L, 3L)
     *       .flatMapToDouble(n -> DoubleStream.of(Math.sqrt(n), Math.pow(n, 2)))
     *       .toArray();   // returns [1.414..., 4.0, 1.732..., 9.0]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from long to DoubleStream
     * @return a new {@link DoubleStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatMapToDouble(LongFunction<? extends DoubleStream> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream produced by applying the provided mapping function to each element.
     * Each mapped stream is closed after its contents have been placed into this stream.
     * (If a mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert numbers to strings with variations
     * LongStream.of(1L, 2L)
     *       .flatMapToObj(n -> Stream.of("Number: " + n, "Value: " + n))
     *       .toList();   // returns ["Number: 1", "Value: 1", "Number: 2", "Value: 2"]
     *
     * // Generate object sequences from numbers
     * LongStream.of(2L, 3L)
     *       .flatMapToObj(n -> Stream.generate(() -> "Item-" + n).limit(n))
     *       .toList();   // returns ["Item-2", "Item-2", "Item-3", "Item-3", "Item-3"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from long to Stream of T
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     * @see #flatMap(LongFunction)
     * @see #flatmapToObj(LongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(LongFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of a collection produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten collections generated from longs
     * LongStream.of(2L, 3L)
     *       .flatmapToObj(n -> Arrays.asList("A" + n, "B" + n))
     *       .toList();   // returns ["A2", "B2", "A3", "B3"]
     *
     * // Generate dynamic lists from numbers
     * LongStream.of(1L, 2L)
     *       .flatmapToObj(n -> Collections.nCopies((int)n, "X"))
     *       .toList();   // returns ["X", "X", "X"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from long to Collection of T
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     * @see #flatMapToObj(LongFunction)
     * @see #flatMapArrayToObj(LongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(LongFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of an array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to string arrays
     * LongStream.of(1L, 2L)
     *       .flatMapArrayToObj(n -> new String[]{"#" + n, "*" + n})
     *       .toList();   // returns ["#1", "*1", "#2", "*2"]
     *
     * // Generate object arrays dynamically
     * LongStream.of(10L, 20L)
     *       .flatMapArrayToObj(n -> new Integer[]{(int)n, (int)(n * 2)})
     *       .toList();   // returns [10, 20, 20, 40]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from long to T[]
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     * @see #flatMapToObj(LongFunction)
     * @see #flatmapToObj(LongFunction)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(LongFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given multi-mapping function to each element.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map each element to multiple values
     * LongStream.of(1L, 2L)
     *       .mapMulti((n, consumer) -> {
     *           consumer.accept(n);
     *           consumer.accept(n * 10);
     *           consumer.accept(n * 100);
     *       })
     *       .toArray();   // returns [1L, 10L, 100L, 2L, 20L, 200L]
     *
     * // Conditionally emit multiple values
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .mapMulti((n, consumer) -> {
     *           if (n % 2 == 0) {
     *               consumer.accept(n);
     *               consumer.accept(n / 2);
     *           }
     *       })
     *       .toArray();   // returns [2L, 1L, 4L, 2L]
     * }</pre>
     *
     * @param mapper a non-interfering, stateless function that generates replacement elements
     * @return a new {@link LongStream} consisting of the elements generated by applying the mapper to each element of this stream
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapMulti(LongMapMultiConsumer mapper);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream, where the function returns an {@code OptionalLong}.
     * Only the present values are included in the returned stream.
     * This is an intermediate operation.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and transform in one operation
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *       .mapPartial(n -> n % 2 == 0 ? OptionalLong.of(n * 10) : OptionalLong.empty())
     *       .toArray();   // returns [20L, 40L]
     *
     * // Safe division with partial mapping
     * LongStream.of(10L, 0L, 20L, 0L, 30L)
     *       .mapPartial(n -> n != 0 ? OptionalLong.of(100 / n) : OptionalLong.empty())
     *       .toArray();   // returns [10L, 5L, 3L]
     * }</pre>
     *
     * @param mapper a non-interfering, stateless function to apply to each element which returns an {@code OptionalLong}
     * @return a new {@link LongStream} consisting of the present values from the non-empty {@code OptionalLong} results produced by the mapper
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapPartial(LongFunction<OptionalLong> mapper);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream, where the function returns a {@code java.util.OptionalLong}.
     * Only the present values are included in the returned stream.
     * This is an intermediate operation.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and transform using JDK OptionalLong
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *       .mapPartialJdk(n -> n > 2 ? java.util.OptionalLong.of(n * 2) : java.util.OptionalLong.empty())
     *       .toArray();   // returns [6L, 8L, 10L]
     *
     * // Integration with JDK APIs returning OptionalLong
     * LongStream.of(5L, -1L, 10L)
     *       .mapPartialJdk(n -> n > 0 ? java.util.OptionalLong.of(n) : java.util.OptionalLong.empty())
     *       .toArray();   // returns [5L, 10L]
     * }</pre>
     *
     * @param mapper a non-interfering, stateless function to apply to each element which returns a {@code java.util.OptionalLong}
     * @return a new {@link LongStream} consisting of the present values from the non-empty {@code java.util.OptionalLong} results produced by the mapper
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapPartialJdk(LongFunction<java.util.OptionalLong> mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate range sums for consecutive numbers
     * LongStream.of(1L, 2L, 3L, 10L, 11L, 20L)
     *       .rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last)
     *       .toArray();   // returns [3L, 6L, 21L, 40L] - (first+last) of ranges [1,2], [3,3], [10,11], [20,20]
     *
     * // Group by proximity threshold
     * LongStream.of(100L, 102L, 105L, 200L, 202L)
     *       .rangeMap((first, next) -> next - first < 5, (first, last) -> last - first)
     *       .toArray();   // returns [2L, 0L, 2L] - (last-first) of ranges [100,102], [105,105], [200,202]
     * }</pre>
     *
     * @param sameRange a predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element. Must be {@code non-null}.
     * @param mapper a function that maps a range (defined by its first and last element) to an output element
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream rangeMap(final LongBiPredicate sameRange, final LongBinaryOperator mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21] to objects
     * using the mapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create range descriptions
     * LongStream.of(1L, 2L, 3L, 10L, 11L, 20L)
     *       .rangeMapToObj((first, next) -> next - first <= 1,
     *                      (first, last) -> first + "-" + last)
     *       .toList();   // returns ["1-2", "3-3", "10-11", "20-20"]
     *
     * // Map ranges to custom objects
     * LongStream.of(100L, 101L, 200L, 201L, 202L)
     *       .rangeMapToObj((first, next) -> next - first == 1,
     *                      (first, last) -> new Pair<>(first, last))
     *       .toList();   // returns [Pair(100,101), Pair(200,201), Pair(202,202)]
     * }</pre>
     *
     * @param <T> the element type of the new stream
     * @param sameRange a predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element. Must be {@code non-null}.
     * @param mapper a function that maps a range (defined by its first and last element) to an output object of type T
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract <T> Stream<T> rangeMapToObj(final LongBiPredicate sameRange, final LongBiFunction<? extends T> mapper);

    /**
     * Collapses consecutive elements in the stream into groups based on a predicate.
     * Elements for which the predicate returns {@code true} when applied to adjacent elements are grouped together into lists.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * the resulting stream will contain [[1, 2], [5, 6, 7], [10]].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group consecutive numbers
     * LongStream.of(1L, 2L, 3L, 10L, 11L, 20L)
     *       .collapse((last, next) -> next - last == 1)
     *       .map(LongList::toArray)
     *       .toList();   // returns [[1, 2, 3], [10, 11], [20]]
     *
     * // Group by proximity threshold
     * LongStream.of(100L, 101L, 105L, 106L, 200L)
     *       .collapse((last, next) -> next - last < 3)
     *       .map(LongList::toArray)
     *       .toList();   // returns [[100, 101], [105, 106], [200]]
     * }</pre>
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @return a stream of lists, each containing a sequence of consecutive elements that are collapsible with each other
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<LongList> collapse(final LongBiPredicate collapsible);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When adjacent elements satisfy the collapsible predicate, they are merged using the provided merge function.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum consecutive numbers
     * LongStream.of(1L, 2L, 3L, 10L, 11L, 20L)
     *       .collapse((last, next) -> next - last == 1, Long::sum)
     *       .toArray();   // returns [6L, 21L, 20L] - sums of [1,2,3], [10,11], [20]
     *
     * // Keep maximum within groups
     * LongStream.of(5L, 3L, 8L, 100L, 99L, 200L)
     *       .collapse((last, next) -> Math.abs(next - last) < 10, Long::max)
     *       .toArray();   // returns [8L, 100L, 200L]
     * }</pre>
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream collapse(final LongBiPredicate collapsible, final LongBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * Elements for which the predicate returns {@code true} when applied with the first and last elements of the current group and the next element in the stream are merged together.
     * The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between the first element of the group and the next element is less than 5,
     * and the merge function sums the elements, the resulting stream will contain [8, 23].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse based on range from first to next element
     * LongStream.of(1L, 2L, 5L, 6L, 7L, 10L)
     *       .collapse((first, last, next) -> next - first < 5, Long::sum)
     *       .toArray();   // returns [8L, 23L] - sums of groups [1,2,5] and [6,7,10] where span from first element is &lt; 5
     *
     * // Advanced grouping with first/last tracking
     * LongStream.of(100L, 105L, 110L, 200L, 205L)
     *       .collapse((first, last, next) -> (next - first) <= 20, Long::sum)
     *       .toArray();   // returns [315L, 405L] - sums of groups [100,105,110] and [200,205] where span from first element is &lt;= 20
     * }</pre>
     *
     * @param collapsible a predicate that determines if the next element should be collapsed with the first and last elements of the group.
     *          The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream collapse(final LongTriPredicate collapsible, final LongBinaryOperator mergeFunction);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * For example, given a stream of numbers [1, 2, 3, 4], and an accumulator that performs addition,
     * the output would be a stream of numbers [1, 3, 6, 10].
     *
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate running sum (prefix sum)
     * LongStream.of(1L, 2L, 3L, 4L)
     *       .scan(Long::sum)
     *       .toArray();   // returns [1L, 3L, 6L, 10L]
     *
     * // Calculate running product
     * LongStream.of(2L, 3L, 4L)
     *       .scan((a, b) -> a * b)
     *       .toArray();   // returns [2L, 6L, 24L]
     *
     * // Running maximum
     * LongStream.of(5L, 2L, 8L, 1L, 9L)
     *       .scan(Long::max)
     *       .toArray();   // returns [5L, 5L, 8L, 8L, 9L]
     * }</pre>
     *
     * @param accumulator a {@code LongBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code LongStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream scan(final LongBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * For example, given a stream of numbers [1, 2, 3, 4], an initial value of 10, and an accumulator that performs addition,
     * the output would be a stream of numbers [11, 13, 16, 20].
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running sum with initial value
     * LongStream.of(1L, 2L, 3L)
     *       .scan(10L, Long::sum)
     *       .toArray();   // returns [11L, 13L, 16L]
     *
     * // Running product with multiplier
     * LongStream.of(2L, 3L, 4L)
     *       .scan(5L, (a, b) -> a * b)
     *       .toArray();   // returns [10L, 30L, 120L]
     * }</pre>
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     *        It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * @param accumulator a {@code LongBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code LongStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream scan(final long init, final LongBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Include initial value in output
     * LongStream.of(1L, 2L, 3L)
     *       .scan(0L, true, Long::sum)
     *       .toArray();   // returns [0L, 1L, 3L, 6L]
     *
     * // Exclude initial value (like scan(init, accumulator))
     * LongStream.of(1L, 2L, 3L)
     *       .scan(10L, false, Long::sum)
     *       .toArray();   // returns [11L, 13L, 16L]
     * }</pre>
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     * @param initIncluded a boolean value that determines if the initial value should be included as the first element in the returned stream.
     * @param accumulator a {@code LongBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code LongStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream scan(final long init, final boolean initIncluded, final LongBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Add header values to stream
     * LongStream.of(3L, 4L, 5L)
     *       .prepend(1L, 2L)
     *       .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Add default values at the beginning
     * LongStream.of(100L, 200L)
     *       .prepend(0L)
     *       .toArray();   // returns [0L, 100L, 200L]
     * }</pre>
     *
     * @param a the elements to prepend to this stream
     * @return a new stream with the specified elements prepended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream prepend(final long... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Add footer values to stream
     * LongStream.of(1L, 2L, 3L)
     *       .append(4L, 5L)
     *       .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Add terminator value
     * LongStream.of(100L, 200L)
     *       .append(-1L)
     *       .toArray();   // returns [100L, 200L, -1L]
     * }</pre>
     *
     * @param a the elements to append to this stream
     * @return a new stream with the specified elements appended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream append(final long... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Empty stream gets default values
     * LongStream.empty()
     *       .appendIfEmpty(1L, 2L, 3L)
     *       .toArray();   // returns [1L, 2L, 3L]
     *
     * // Non-empty stream unchanged
     * LongStream.of(100L, 200L)
     *       .appendIfEmpty(999L)
     *       .toArray();   // returns [100L, 200L]
     * }</pre>
     *
     * @param a the elements to append if this stream is empty
     * @return this stream if not empty, otherwise a new stream containing the specified elements
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream appendIfEmpty(final long... a);

    /**
     * Returns a LongStream consisting of the top n elements of this stream, according to the natural order of the elements.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 largest numbers
     * LongStream.of(1L, 5L, 3L, 9L, 2L, 8L)
     *       .top(3)
     *       .sorted()
     *       .toArray();   // returns [5L, 8L, 9L]
     *
     * // Request more than available
     * LongStream.of(10L, 20L)
     *       .top(5)
     *       .toArray();   // returns [10L, 20L] or [20L, 10L]
     * }</pre>
     *
     * @param n the number of elements to select
     * @return a new {@link LongStream} consisting of the top {@code n} elements; the order of the returned elements is not guaranteed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream top(int n);

    /**
     * Returns a LongStream consisting of the top n elements of this stream compared by the provided Comparator.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 smallest numbers (reverse order)
     * LongStream.of(1L, 5L, 3L, 9L, 2L, 8L)
     *       .top(3, Comparator.reverseOrder())
     *       .sorted()
     *       .toArray();   // returns [1L, 2L, 3L] (sorted ascending)
     *
     * // Custom comparator (e.g., by absolute value)
     * LongStream.of(-100L, 50L, -75L, 25L)
     *       .top(2, Comparator.comparingLong(Math::abs))
     *       .toArray();   // returns [-100L, -75L] or similar
     * }</pre>
     *
     * @param n the number of elements to select
     * @param comparator a non-interfering, stateless comparator to be used to compare stream elements
     * @return a new {@link LongStream} consisting of the top {@code n} elements as determined by the comparator; the order of the returned elements is not guaranteed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream top(final int n, Comparator<? super Long> comparator);

    /**
     * Returns a {@code LongList} containing the elements of this stream.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .filter(x -> x > 2)
     *     .toLongList();   // returns LongList [3, 4, 5]
     * }</pre>
     *
     * @return a {@code LongList} containing the elements of this stream
     */
    @SequentialOnly
    @TerminalOp
    public abstract LongList toLongList();

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *     .toMap(n -> "key" + n, n -> n * 10)
     *     // Result: {key1=10, key2=20, key3=30}
     *
     * LongStream.range(1, 4)
     *     .toMap(n -> n, n -> String.valueOf(n))
     *     // Result: {1="1", 2="2", 3="3"}
     * }</pre>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to get the keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to get the values
     * @return a {@code Map} whose keys and values are the result of applying the mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.LongFunction<? extends K, E> keyMapper,
            Throwables.LongFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(3L, 1L, 2L)
     *     .toMap(n -> n, n -> "value" + n, LinkedHashMap::new)
     *     // Result: LinkedHashMap {3="value3", 1="value1", 2="value2"} (insertion order preserved)
     *
     * LongStream.of(5L, 3L, 8L)
     *     .toMap(n -> n, n -> n * n, TreeMap::new)
     *     // Result: TreeMap {3=9, 5=25, 8=64} (sorted by keys)
     * }</pre>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to get the keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to get the values
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the result of applying the mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.LongFunction<? extends K, E> keyMapper,
            Throwables.LongFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates, the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 11L, 12L)
     *     .toMap(n -> n / 10, n -> n, Long::sum)
     *     // Result: {0=6, 1=23} (keys: 0 for 1,2,3 summed to 6; 1 for 11,12 summed to 23)
     *
     * LongStream.of(5L, 15L, 25L, 35L)
     *     .toMap(n -> n % 10, n -> n, Long::max)
     *     // Result: {5=35} (all map to key 5, max value is 35)
     * }</pre>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to get the keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to get the values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a {@code Map} whose keys and values are the result of applying the mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.LongFunction<? extends K, E> keyMapper,
            Throwables.LongFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates, the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(10L, 20L, 30L, 15L, 25L)
     *     .toMap(n -> n / 10, n -> n, Long::min, TreeMap::new)
     *     // Result: TreeMap {1=10, 2=20, 3=30} (sorted keys, min values for duplicates)
     *
     * LongStream.of(100L, 200L, 150L)
     *     .toMap(n -> n / 100, n -> String.valueOf(n), (a, b) -> a + "," + b, LinkedHashMap::new)
     *     // Result: LinkedHashMap {1="100,150", 2="200"} (concatenated values in insertion order)
     * }</pre>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to get the keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to get the values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the result of applying the mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.LongFunction<? extends K, E> keyMapper,
            Throwables.LongFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function, and performs a reduction operation on the values associated with each key.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 11L, 12L, 13L)
     *     .groupTo(n -> n / 10, Collectors.counting())
     *     // Result: {0=3, 1=3} (count of elements in each group)
     *
     * LongStream.of(5L, 10L, 15L, 20L, 25L)
     *     .groupTo(n -> n / 10, Collectors.summingLong(Long::longValue))
     *     // Result: {0=5, 1=25, 2=45} (sum of elements in each group)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Map} containing the results of the reduction operation on the values associated with each key
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function, and performs a reduction operation on the values associated with each key.
     * The resulting map is created by the provided factory function.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(8L, 3L, 5L, 18L, 13L, 15L)
     *     .groupTo(n -> n / 10, Collectors.toList(), TreeMap::new)
     *     // Result: TreeMap {0=[8, 3, 5], 1=[18, 13, 15]} (sorted keys, values in encounter order)
     *
     * LongStream.of(100L, 200L, 150L, 250L)
     *     .groupTo(n -> n / 100, Collectors.averagingLong(Long::longValue), LinkedHashMap::new)
     *     // Result: LinkedHashMap {1=125.0, 2=225.0} (insertion order preserved)
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} containing the results of the reduction operation on the values associated with each key
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided identity value and an associative accumulation function, and returns the reduced value.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long sum = LongStream.of(1L, 2L, 3L, 4L)
     *     .reduce(0L, Long::sum);
     *     // Result: 10
     *
     * long product = LongStream.of(2L, 3L, 4L)
     *     .reduce(1L, (a, b) -> a * b);
     *     // Result: 24
     *
     * long max = LongStream.of(5L, 2L, 8L, 1L)
     *     .reduce(Long.MIN_VALUE, Long::max);
     *     // Result: 8
     * }</pre>
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract long reduce(long identity, LongBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using an associative accumulation function, and returns an {@code OptionalLong} describing the reduced value, if any.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalLong result = LongStream.of(10L, 20L, 30L)
     *     .reduce(Long::sum);
     *     // Result: OptionalLong[60]
     *
     * OptionalLong empty = LongStream.empty()
     *     .reduce(Long::sum);
     *     // Result: OptionalLong.empty()
     *
     * OptionalLong min = LongStream.of(5L, 2L, 8L, 1L)
     *     .reduce(Long::min);
     *     // Result: OptionalLong[1]
     * }</pre>
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalLong} describing the result of the reduction
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalLong reduce(LongBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream.
     * A mutable reduction is one in which the reduced value is a mutable result container, such as an {@code ArrayList}.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect longs into a custom container
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .collect(
     *         () -> new long[1],                   // creates accumulator (Supplier)
     *         (arr, value) -> arr[0] += value,     // adds value to accumulator (ObjLongConsumer)
     *         (arr1, arr2) -> arr1[0] += arr2[0]   // adds one accumulator into the other (BiConsumer)
     *     );                                       // returns long[]{15}
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container. For a parallel execution, this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a result
     * @param combiner an associative, non-interfering, stateless function for combining two values, which must be compatible with the accumulator function.
     *                It is unnecessary to specify {@code combiner} if {@code R} is a {@code Map/Collection/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     * @see BiConsumers#ofAddAll()
     * @see BiConsumers#ofPutAll()
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream.
     * A mutable reduction is one in which the reduced value is a mutable result container, such as an {@code ArrayList}.
     * This is a terminal operation.
     *
     * <br />
     * Only call this method when the returned type {@code R} is one of these types: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjLongConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into a LongList (combiner auto-detected)
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .collect(LongList::new, LongList::add);   // returns LongList [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param <R> The type of the result. It must be {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @param supplier a function that creates a new result container. For a parallel execution, this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a result.
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @return the result of the reduction
     * @see #collect(Supplier, ObjLongConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator);

    /**
     * Performs an action for each element of this stream.
     * This is a convenience method that delegates to {@link #forEach(Throwables.LongConsumer)}.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .foreach(System.out::println);
     * // prints: 1, 2, 3, 4, 5 (each on a new line)
     * }</pre>
     *
     * @param action a non-interfering action to perform on the elements
     * @see #forEach(Throwables.LongConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public void foreach(final LongConsumer action) { // NOSONAR
        forEach(action::accept);
    }

    /**
     * Performs an action for each element of this stream.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .forEach(System.out::println);
     * // prints: 1, 2, 3, 4, 5 (each on a new line)
     * }</pre>
     *
     * @param <E> the type of exception thrown by the action
     * @param action a non-interfering action to perform on the elements
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws E;

    /**
     * Performs an action for each element of this stream, passing the element index as the first parameter to the action.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(100L, 200L, 300L)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 100
     * // Element at index 1: 200
     * // Element at index 2: 300
     * }</pre>
     *
     * @param <E> the type of exception thrown by the action
     * @param action a non-interfering action to perform on the elements, where the first parameter is the element index and the second is the element value
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntLongConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any element is greater than 100
     * boolean hasLarge = LongStream.of(10L, 50L, 150L)
     *     .anyMatch(n -> n > 100);   // returns true
     *
     * // Check if any element is negative
     * boolean hasNegative = LongStream.of(1L, 2L, 3L)
     *     .anyMatch(n -> n < 0);   // returns false
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if any elements of the stream match the provided predicate, otherwise {@code false}. Returns {@code false} if the stream is empty.
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean anyMatch(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all elements are positive
     * boolean allPositive = LongStream.of(1L, 2L, 3L, 4L)
     *     .allMatch(n -> n > 0);   // returns true
     *
     * // Check if all elements are greater than 10
     * boolean allLarge = LongStream.of(5L, 15L, 20L)
     *     .allMatch(n -> n > 10);   // returns false
     *
     * // Empty stream returns true
     * boolean empty = LongStream.empty()
     *     .allMatch(n -> n < 0);   // returns true
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either all elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean allMatch(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if no elements are negative
     * boolean noNegatives = LongStream.of(1L, 2L, 3L, 4L)
     *     .noneMatch(n -> n < 0);   // returns true
     *
     * // Check if no elements are greater than 100
     * boolean noLarge = LongStream.of(10L, 50L, 150L)
     *     .noneMatch(n -> n > 100);   // returns false
     *
     * // Empty stream returns true
     * boolean empty = LongStream.empty()
     *     .noneMatch(n -> n > 0);   // returns true
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either no elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean noneMatch(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalLong}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()}: it always returns the first element in
     * encounter order, including for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalLong first = LongStream.of(1L, 2L, 3L, 4L, 5L)
     *                                 .findFirst();   // returns OptionalLong.of(1)
     * }</pre>
     *
     * @return an {@code OptionalLong} containing the first element of the stream, or an empty {@code OptionalLong} if the stream is empty
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.LongPredicate)
     * @see #findAny(Throwables.LongPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalLong findFirst() {
        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalLong}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalLong any = LongStream.of(1L, 2L, 3L, 4L, 5L)
     *                              .findAny();   // returns OptionalLong.of(1)
     * }</pre>
     *
     * @return an {@code OptionalLong} containing the first element of the stream, or an empty {@code OptionalLong} if the stream is empty
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.LongPredicate)
     * @see #findAny(Throwables.LongPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalLong findAny() {
        return first();
    }

    /**
     * Returns an {@code OptionalLong} describing the first element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element exists.
     * This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first even number
     * OptionalLong firstEven = LongStream.of(1L, 3L, 4L, 5L, 6L)
     *     .findFirst(n -> n % 2 == 0);   // returns OptionalLong[4]
     *
     * // Find first number greater than 100
     * OptionalLong firstLarge = LongStream.of(10L, 20L, 150L, 200L)
     *     .findFirst(n -> n > 100);   // returns OptionalLong[150]
     *
     * // No matching element
     * OptionalLong notFound = LongStream.of(1L, 2L, 3L)
     *     .findFirst(n -> n > 10);   // returns OptionalLong.empty()
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalLong} describing the first element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element is found
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalLong findFirst(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalLong} describing some element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element exists.
     * This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is free to select any element in the stream that matches the predicate.
     * This is to allow for maximal performance in parallel operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any even number (may return different results in parallel streams)
     * OptionalLong anyEven = LongStream.of(1L, 3L, 4L, 5L, 6L)
     *     .findAny(n -> n % 2 == 0);   // returns OptionalLong[4] or OptionalLong[6]
     *
     * // Find any negative number
     * OptionalLong anyNegative = LongStream.of(10L, -5L, 20L, -3L)
     *     .findAny(n -> n < 0);   // returns OptionalLong[-5] or OptionalLong[-3]
     *
     * // No matching element
     * OptionalLong notFound = LongStream.of(1L, 2L, 3L)
     *     .findAny(n -> n > 10);   // returns OptionalLong.empty()
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalLong} describing some element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element is found
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalLong findAny(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalLong} describing the last element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element exists.
     * This is a terminal operation.
     *
     * Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last even number
     * OptionalLong lastEven = LongStream.of(1L, 4L, 3L, 6L, 5L)
     *     .findLast(n -> n % 2 == 0);   // returns OptionalLong[6]
     *
     * // Find last number less than 10
     * OptionalLong lastSmall = LongStream.of(5L, 15L, 8L, 20L, 3L)
     *     .findLast(n -> n < 10);   // returns OptionalLong[3]
     *
     * // No matching element
     * OptionalLong notFound = LongStream.of(1L, 2L, 3L)
     *     .findLast(n -> n > 10);   // returns OptionalLong.empty()
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalLong} describing the last element of this stream that matches the given predicate, or an empty {@code OptionalLong} if no such element is found
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalLong findLast(final Throwables.LongPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalLong} describing the minimum element of this stream, or an empty optional if this stream is empty.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(5L, 2L, 8L, 1L, 9L).min();   // returns OptionalLong[1L]
     *
     * LongStream.empty().min();   // returns OptionalLong.empty()
     *
     * // Safe retrieval with default value
     * long minValue = LongStream.of(10L, 20L, 30L).min().orElse(0L);   // returns 10L
     * }</pre>
     *
     * @return an {@code OptionalLong} containing the minimum element of this stream, or an empty {@code OptionalLong} if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalLong min();

    /**
     * Returns an {@code OptionalLong} describing the maximum element of this stream, or an empty optional if this stream is empty.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(5L, 2L, 8L, 1L, 9L).max();   // returns OptionalLong[9L]
     *
     * LongStream.empty().max();   // returns OptionalLong.empty()
     *
     * // Safe retrieval with default value
     * long maxValue = LongStream.of(10L, 20L, 30L).max().orElse(0L);   // returns 30L
     * }</pre>
     *
     * @return an {@code OptionalLong} containing the maximum element of this stream, or an empty {@code OptionalLong} if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalLong max();

    /**
     * Returns the <i>k-th</i> largest element in the stream.
     * If the stream is empty or the count of elements is less than k, an empty {@code OptionalLong} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * LongStream.of(10L, 30L, 20L, 50L, 40L).kthLargest(2);   // returns OptionalLong[40L]
     *
     * // Find the largest element (same as max)
     * LongStream.of(5L, 2L, 8L, 1L).kthLargest(1);   // returns OptionalLong[8L]
     *
     * // When k exceeds stream size
     * LongStream.of(1L, 2L, 3L).kthLargest(5);   // returns OptionalLong.empty()
     * }</pre>
     *
     * @param k the position (1-based) of the largest element to retrieve; must be positive
     * @return an {@code OptionalLong} containing the k-th largest element, or an empty {@code OptionalLong} if the stream is empty or the count of elements is less than k
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalLong kthLargest(int k);

    /**
     * Returns the sum of all elements in this stream. This is a terminal operation.
     *
     * <p>This is equivalent to:
     * <pre>{@code
     *     reduce(0L, Long::sum)
     * }</pre>
     *
     * <p>The sum is computed by adding all elements sequentially. For empty streams, returns 0.
     *
     * <p><b>Note on overflow:</b> The sum may overflow if the result exceeds {@code Long.MAX_VALUE}
     * or is less than {@code Long.MIN_VALUE}. For extremely large datasets or when overflow is a concern,
     * consider using {@link #mapToObj(LongFunction)} to convert to {@link java.math.BigInteger}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long total = LongStream.of(1L, 2L, 3L, 4L, 5L).sum();   // returns 15L
     *
     * // Handling potential overflow with BigInteger
     * BigInteger safeSum = LongStream.of(Long.MAX_VALUE, 1L)
     *     .mapToObj(BigInteger::valueOf)
     *     .reduce(BigInteger.ZERO, BigInteger::add);
     * }</pre>
     *
     * <p>Note: the sum is accumulated in a {@code long}. Unlike {@link IntStream#sum()} (which accumulates
     * in a wider type and throws {@link ArithmeticException} on overflow), if the true sum exceeds
     * {@link Long#MAX_VALUE} this silently wraps around.
     *
     * @return the sum of elements in this stream. Returns 0 if the stream is empty.
     * @see #average()
     * @see #reduce(long, LongBinaryOperator)
     */
    @SequentialOnly
    @TerminalOp
    public abstract long sum() throws ArithmeticException;

    /**
     * Returns an OptionalDouble describing the arithmetic mean of elements of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L).average();   // returns OptionalDouble[3.0]
     *
     * LongStream.of(10L, 20L, 30L).average();   // returns OptionalDouble[20.0]
     *
     * LongStream.empty().average();   // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double avg = LongStream.of(100L, 200L).average().orElse(0.0);   // returns 150.0
     * }</pre>
     *
     * <p>Note: the running total is accumulated in a {@code long} before the division, so for very large
     * magnitudes it can overflow and wrap (unlike {@link DoubleStream#average()}, which sums in floating
     * point); the returned mean is inaccurate in that case.
     *
     * @return an OptionalDouble containing the arithmetic mean of elements of this stream,
     *         or an empty optional if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble average();

    /**
     * Returns statistics about the elements of this stream.
     * The statistics include count, sum, min, max, and average.
     * This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSummaryStatistics stats = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // prints Count: 5
     * System.out.println("Sum: " + stats.getSum());           // prints Sum: 15
     * System.out.println("Min: " + stats.getMin());           // prints Min: 1
     * System.out.println("Max: " + stats.getMax());           // prints Max: 5
     * System.out.println("Average: " + stats.getAverage());   // prints Average: 3.0
     * }</pre>
     *
     * @return a {@code LongSummaryStatistics} describing various summary data about the elements of this stream
     */
    @SequentialOnly
    @TerminalOp
    public abstract LongSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of LongSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result =
     *     LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
     *         .summaryStatisticsAndPercentiles();
     *
     * LongSummaryStatistics stats = result.left();
     * stats.getCount();     // 10
     * stats.getSum();       // 55
     * stats.getAverage();   // 5.5
     *
     * Map<Percentage, Long> percentiles = result.right().get();
     * percentiles.get(Percentage._50);   // returns the median value
     * percentiles.get(Percentage._95);   // 95th percentile
     * }</pre>
     *
     * @return a {@code Pair} containing a {@code LongSummaryStatistics} describing various summary data about the elements of this stream,
     *         and an {@code Optional} containing a map of percentile values
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream, selecting elements based on the given selector function.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams maintaining sort order
     * LongStream stream1 = LongStream.of(1L, 3L, 5L, 7L);
     * LongStream stream2 = LongStream.of(2L, 4L, 6L, 8L);
     * stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L]
     *
     * // Merge with custom selection logic
     * LongStream.of(10L, 20L, 30L)
     *     .mergeWith(LongStream.of(15L, 25L, 35L),
     *         (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [10L, 15L, 20L, 25L, 30L, 35L]
     * }</pre>
     *
     * @param b the stream to merge with
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return the new merged stream
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream mergeWith(final LongStream b, final LongBiFunction<MergeResult> nextSelector);

    /**
     * Zips this stream with the given stream using the provided zip function.
     * The zip function takes elements from this stream and the given stream until either the current stream or the given stream runs out of elements.
     * The resulting stream will have the length of the shorter of the current stream and the given stream.
     * This is an intermediate operation.
     *
     * <p>This operation can be parallelized if the stream supports parallel processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *           .zipWith(LongStream.of(10L, 20L, 30L, 40L),
     *                    (a, b) -> a + b)
     *           .toArray();   // returns [11L, 22L, 33L]
     * }</pre>
     *
     * @param b the LongStream to be combined with the current LongStream. Must be {@code non-null}.
     * @param zipFunction a LongBinaryOperator that determines the combination of elements in the combined LongStream. Must be {@code non-null}.
     * @return a new LongStream that is the result of combining the current LongStream with the given LongStream
     * @see #zipWith(LongStream, long, long, LongBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream zipWith(LongStream b, LongBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *           .zipWith(LongStream.of(10L, 20L), LongStream.of(110L, 120L),
     *                    (a, b, c) -> a + b + c)
     *           .toArray();   // returns [121L, 142L]
     * }</pre>
     *
     * @param b the second LongStream to be combined with the current LongStream. Will be closed along with this LongStream.
     * @param c the third LongStream to be combined with the current LongStream. Will be closed along with this LongStream.
     * @param zipFunction a LongTernaryOperator that determines the combination of elements in the combined LongStream. Must be {@code non-null}.
     * @return a new LongStream that is the result of combining the current LongStream with the given LongStreams
     * @see #zipWith(LongStream, LongStream, long, long, long, LongTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream zipWith(LongStream b, LongStream c, LongTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *           .zipWith(LongStream.of(10L), 0L, 0L, (a, b) -> a + b)
     *           .toArray();   // returns [11L, 2L, 3L]
     * }</pre>
     *
     * @param b the LongStream to be combined with the current LongStream. Will be closed along with this LongStream.
     * @param valueForNoneA the default value to use for the current LongStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given LongStream when it runs out of elements
     * @param zipFunction a LongBinaryOperator that determines the combination of elements in the combined LongStream. Must be {@code non-null}.
     * @return a new LongStream that is the result of combining the current LongStream with the given LongStream
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream zipWith(LongStream b, long valueForNoneA, long valueForNoneB, LongBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *           .zipWith(LongStream.of(10L), LongStream.of(100L),
     *                    0L, 0L, 0L,
     *                    (a, b, c) -> a + b + c)
     *           .toArray();   // returns [111L, 2L, 3L]
     * }</pre>
     *
     * @param b the second LongStream to be combined with the current LongStream. Will be closed along with this LongStream.
     * @param c the third LongStream to be combined with the current LongStream. Will be closed along with this LongStream.
     * @param valueForNoneA the default value to use for the current LongStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second LongStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third LongStream when it runs out of elements
     * @param zipFunction a LongTernaryOperator that determines the combination of elements in the combined LongStream. Must be {@code non-null}.
     * @return a new LongStream that is the result of combining the current LongStream with the given LongStreams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream zipWith(LongStream b, LongStream c, long valueForNoneA, long valueForNoneB, long valueForNoneC, LongTernaryOperator zipFunction);

    /**
     * Converts this LongStream to a FloatStream by casting each long value to float.
     * This is an intermediate operation that performs a narrowing primitive conversion.
     *
     * <p><b>Precision Warning:</b> This conversion may lose precision because float uses only 24 bits
     * for the significand (mantissa), while long uses 64 bits. Long values outside the range
     * [-2^24, 2^24] may not be represented exactly as floats. For values requiring full precision,
     * consider using {@link #asDoubleStream()} instead, which can represent all long values without
     * loss of magnitude (though precision loss can still occur for values beyond 2^53).
     *
     * <p><b>Range Considerations:</b>
     * <ul>
     *   <li>Long values within [-16777216, 16777216] ([-2^24, 2^24]) can be represented exactly</li>
     *   <li>Larger long values will be rounded to the nearest representable float value</li>
     *   <li>Very large long values may result in float infinity</li>
     *   <li>The conversion is always safe (no overflow exceptions), but may lose precision</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic conversion - small values retain precision
     * LongStream.of(1L, 2L, 3L, 4L)
     *     .asFloatStream()
     *     .toArray();   // returns [1.0f, 2.0f, 3.0f, 4.0f]
     *
     * // Convert for percentage calculations
     * LongStream.of(25L, 50L, 75L, 100L)
     *     .asFloatStream()
     *     .map(f -> f / 100.0f)
     *     .toArray();   // returns [0.25f, 0.5f, 0.75f, 1.0f]
     *
     * // Precision loss with large values
     * long largeValue = 16777217L;  // 2^24 + 1
     * float converted = LongStream.of(largeValue)
     *     .asFloatStream()
     *     .findFirst()
     *     .orElse(0f);   // returns a value that may not equal exactly 16777217.0f due to rounding
     *
     * // Combining with float arithmetic
     * LongStream.of(10L, 20L, 30L)
     *     .asFloatStream()
     *     .map(f -> f * 1.5f)
     *     .toArray();   // returns [15.0f, 30.0f, 45.0f]
     * }</pre>
     *
     * <p><b>Comparison with alternatives:</b>
     * <pre>{@code
     * LongStream longs = LongStream.of(1L, 2L, 3L);
     *
     * // Using asFloatStream (direct conversion)
     * FloatStream floats1 = longs.asFloatStream();
     *
     * // Using mapToFloat (explicit mapping)
     * FloatStream floats2 = longs.mapToFloat(l -> (float) l);
     *
     * // Using asDoubleStream for better precision
     * DoubleStream doubles = longs.asDoubleStream();  // uses double for precision-critical operations
     * }</pre>
     *
     * @return a FloatStream representation of this LongStream with each long value cast to float
     * @see #asDoubleStream()
     * @see #mapToFloat(LongToFloatFunction)
     * @see #mapToDouble(LongToDoubleFunction)
     * @deprecated {@code long}-to-{@code float} is lossy — {@code float} has only a 24-bit mantissa, so {@code long}
     *             values larger than 2<sup>24</sup> in magnitude lose precision. Prefer {@link #asDoubleStream()} for a
     *             precision-preserving widening, or {@link #mapToFloat(LongToFloatFunction)} for an explicit narrowing cast.
     */
    @SequentialOnly
    @IntermediateOp
    @Deprecated
    public abstract FloatStream asFloatStream();

    /**
     * Converts this LongStream to a DoubleStream by casting each long value to double.
     * This is an intermediate operation.
     *
     * <p>This method performs a widening primitive conversion from long to double,
     * which is always safe and does not lose magnitude information, though it may
     * lose precision for very large long values (beyond 2^53).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert longs to doubles for mathematical operations
     * LongStream.of(1L, 2L, 3L, 4L)
     *     .asDoubleStream()
     *     .map(d -> d / 2.0)
     *     .toArray();   // returns [0.5, 1.0, 1.5, 2.0]
     *
     * // Calculate average as double
     * double avg = LongStream.of(10L, 20L, 30L)
     *     .asDoubleStream()
     *     .average()
     *     .orElse(0.0);   // returns 20.0
     * }</pre>
     *
     * @return a DoubleStream representation of this LongStream
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream asDoubleStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Long.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .boxed()
     *     .collect(Collectors.toList());   // returns List<Long> [1L, 2L, 3L, 4L, 5L]
     * }</pre>
     *
     * @return a Stream consisting of the elements of this stream, each boxed to a Long
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Long> boxed();

    /**
     * Converts this stream to a {@code java.util.stream.LongStream}.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .toJdkStream()
     *     .filter(x -> x > 2)
     *     .sum();   // returns 12L (3 + 4 + 5)
     * }</pre>
     *
     * @return a {@code java.util.stream.LongStream} consisting of the elements of this stream
     */
    @SequentialOnly
    @IntermediateOp
    public abstract java.util.stream.LongStream toJdkStream();

    /**
     * Transforms this stream using the provided transfer function.
     * This is an intermediate operation that allows integration with JDK stream operations.
     *
     * <p>This method converts the stream to a {@code java.util.stream.LongStream}, applies the transformation,
     * and converts back to an abacus-common {@code LongStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L)
     *     .transformB(s -> s.map(x -> x * 2).sorted())
     *     .toArray();   // returns [2L, 4L, 6L, 8L]
     * }</pre>
     *
     * @param transfer a function that transforms a {@code java.util.stream.LongStream} to another {@code java.util.stream.LongStream}
     * @return a new {@code LongStream} that is the result of applying the transfer function
     * @see #transformB(Function, boolean)
     * @see #toJdkStream()
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public LongStream transformB(final Function<? super java.util.stream.LongStream, ? extends java.util.stream.LongStream> transfer) {
        return transformB(transfer, false);
    }

    /**
     * Transforms this stream using the provided transfer function, with an option to defer the transformation.
     * This is an intermediate operation that allows integration with JDK stream operations.
     *
     * <p>When {@code deferred} is {@code true}, the transformation is not applied until the stream is consumed,
     * allowing for lazy initialization and dynamic stream generation based on runtime conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L, 4L)
     *     .transformB(s -> s.map(x -> x * 2).sorted(), true)
     *     .toArray();   // transforms only when toArray() is called
     * }</pre>
     *
     * @param transfer a function that transforms a {@code java.util.stream.LongStream} to another {@code java.util.stream.LongStream}
     * @param deferred if {@code true}, the transformation is deferred until the stream is consumed
     * @return a new {@code LongStream} that is the result of applying the transfer function
     * @throws IllegalArgumentException if the transfer function is null
     * @see #transformB(Function)
     * @see #defer(Supplier)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public LongStream transformB(final Function<? super java.util.stream.LongStream, ? extends java.util.stream.LongStream> transfer, final boolean deferred)
            throws IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        if (deferred) {
            final Supplier<LongStream> delayInitializer = () -> LongStream.from(transfer.apply(toJdkStream()));
            return LongStream.defer(delayInitializer);
        } else {
            return LongStream.from(transfer.apply(toJdkStream()));
        }
    }

    abstract LongIteratorEx iteratorEx();

    // private static final LongStream EMPTY_STREAM = new ArrayLongStream(N.EMPTY_LONG_ARRAY, true, null);

    /**
     * Returns an empty LongStream with no elements.
     * This is a static factory method that creates a stream containing zero elements.
     *
     * <p>An empty stream is useful as a base case in stream operations, conditional stream creation,
     * or when initializing stream variables that may be populated later.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic empty stream
     * LongStream.empty()
     *     .count();   // returns 0
     *
     * // Conditional stream creation
     * LongStream stream = condition ? LongStream.of(1L, 2L, 3L) : LongStream.empty();
     *
     * // Use as fallback in optional operations
     * OptionalLong result = LongStream.empty().findFirst();   // returns OptionalLong.empty()
     *
     * // Empty stream with terminal operations
     * long sum = LongStream.empty().sum();           // returns 0
     * OptionalLong max = LongStream.empty().max();   // returns OptionalLong.empty()
     *
     * // Combining with other streams
     * LongStream combined = LongStream.concat(
     *     LongStream.empty(),
     *     LongStream.of(1L, 2L, 3L)
     * );   // returns a stream with elements [1L, 2L, 3L]
     * }</pre>
     *
     * @return an empty LongStream with no elements
     */
    public static LongStream empty() {
        return new ArrayLongStream(N.EMPTY_LONG_ARRAY, true, null);
    }

    /**
     * Returns a LongStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once, when the returned stream is first traversed or closed.
     * Closing the returned stream before traversal may still invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Implementation Note:</b> it's equivalent to {@code Stream.just(supplier).flatMapToLong(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Lazy stream creation - supplier called when stream is consumed or closed
     * LongStream stream = LongStream.defer(() -> LongStream.of(1L, 2L, 3L));
     * stream.toArray();   // returns [1L, 2L, 3L]
     *
     * // Defer expensive computation until needed
     * LongStream expensiveStream = LongStream.defer(() -> {
     *     long[] data = computeExpensiveData();
     *     return LongStream.of(data);
     * });
     * // computeExpensiveData() is NOT called until the stream is consumed or closed
     * expensiveStream.count();   // invokes computeExpensiveData() now
     *
     * // Empty stream from defer
     * LongStream emptyDeferred = LongStream.defer(() -> LongStream.empty());
     * emptyDeferred.count();   // returns 0
     *
     * // Defer with conditional logic
     * boolean useLargeData = false;
     * LongStream conditional = LongStream.defer(() ->
     *     useLargeData ? LongStream.range(0L, 1000L) : LongStream.of(1L, 2L));
     * conditional.toArray();   // returns [1L, 2L]
     * }</pre>
     *
     * @param supplier the supplier that provides the LongStream
     * @return a new LongStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static LongStream defer(final Supplier<LongStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<LongStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToLong(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Creates a LongStream from a java.util.stream.LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert JDK stream to abacus stream
     * LongStream stream = LongStream.from(java.util.stream.LongStream.of(1L, 2L, 3L));
     * stream.toArray();   // returns [1L, 2L, 3L]
     *
     * // Convert with pipeline operations
     * long[] result = LongStream.from(
     *         java.util.stream.LongStream.of(10L, 20L, 30L).filter(n -> n > 15L)
     *     ).toArray();   // returns [20L, 30L]
     *
     * // Null input returns empty stream
     * LongStream empty = LongStream.from((java.util.stream.LongStream) null);
     * empty.count();   // returns 0
     *
     * // Convert range from JDK stream
     * long[] range = LongStream.from(java.util.stream.LongStream.rangeClosed(1L, 5L))
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     * }</pre>
     *
     * @param stream the java.util.stream.LongStream to convert
     * @return a new LongStream, or an empty stream if the input is null
     */
    public static LongStream from(final java.util.stream.LongStream stream) {
        if (stream == null) {
            return empty();
        }

        return of(new LongIteratorEx() {
            private PrimitiveIterator.OfLong iter = null;
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (exhausted) {
                    return false;
                }

                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (exhausted) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextLong();
            }

            @Override
            public long count() {
                if (exhausted) {
                    return 0;
                }

                if (iter == null) {
                    exhausted = true;
                    return stream.count();
                }

                return super.count();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0 || exhausted) {
                    return;
                }

                if (iter == null) {
                    iter = stream.skip(n).iterator();
                } else {
                    super.advance(n);
                }
            }

            @Override
            public long[] toArray() {
                if (exhausted) {
                    return new long[0];
                }

                if (iter == null) {
                    exhausted = true;
                    return stream.toArray();
                }

                return super.toArray();
            }
        }).transform(s -> stream.isParallel() ? s.parallel() : s.sequential()).onClose(stream::close);
    }

    /**
     * Returns a LongStream containing the specified element if it is non-null, otherwise returns an empty LongStream.
     * This is a static factory method that provides null-safe stream creation from nullable Long objects.
     *
     * <p>This method is particularly useful when working with nullable Long values (such as from database
     * queries, optional fields, or map lookups) and you want to seamlessly integrate them into stream
     * pipelines without explicit null checks. It acts as a bridge between nullable boxed Long values
     * and primitive long streams.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>If {@code e} is {@code null}: returns an empty LongStream (equivalent to {@link #empty()})</li>
     *   <li>If {@code e} is non-null: returns a LongStream containing the single unboxed long value</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic usage with non-null value
     * LongStream.ofNullable(42L)
     *     .toArray();   // returns [42L]
     *
     * // Null value produces empty stream
     * LongStream.ofNullable(null)
     *     .count();   // returns 0
     *
     * // Null-safe database value processing
     * Long dbValue = map.get("count");  // value is possibly null
     * long result = LongStream.ofNullable(dbValue)
     *     .findFirst()
     *     .orElse(0L);
     *
     * // Combining multiple nullable values
     * Long val1 = map.get("key1");   // value is possibly null
     * Long val2 = map.get("key2");   // value is possibly null
     * Long val3 = map.get("key3");   // value is possibly null
     *
     * long sum = LongStream.concat(
     *     LongStream.ofNullable(val1),
     *     LongStream.ofNullable(val2),
     *     LongStream.ofNullable(val3)
     * ).sum();   // returns the sum of non-null values
     *
     * // Conditional stream operations
     * Long threshold = config.getThreshold();  // value is possibly null
     * LongStream.ofNullable(threshold)
     *     .forEach(t -> applyThreshold(t));   // invokes applyThreshold only if threshold is not null
     *
     * // Flat-mapping nullable values
     * List<Long> nullableValues = Arrays.asList(1L, null, 3L, null, 5L);
     * long[] nonNullValues = nullableValues.stream()
     *     .flatMapToLong(LongStream::ofNullable)
     *     .toArray();   // returns [1L, 3L, 5L]
     * }</pre>
     *
     * <p><b>Comparison with alternatives:</b>
     * <pre>{@code
     * Long value = getValue();  // value is possibly null
     *
     * // Using ofNullable (concise)
     * LongStream stream1 = LongStream.ofNullable(value);
     *
     * // Manual null check (verbose)
     * LongStream stream2 = value != null ? LongStream.of(value) : LongStream.empty();
     *
     * // Using Optional (more verbose)
     * LongStream stream3 = Optional.ofNullable(value)
     *     .map(LongStream::of)
     *     .orElseGet(LongStream::empty);
     * }</pre>
     *
     * @param e the Long element to be wrapped in a LongStream; may be null
     * @return a LongStream containing the single element if non-null, otherwise an empty LongStream
     * @see #of(long...)
     * @see #empty()
     * @see Optional#ofNullable(Object)
     */
    public static LongStream ofNullable(final Long e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Returns a LongStream containing the specified array of long values.
     * This is a static factory method that creates a sequential stream from a varargs array or
     * an explicit array of primitive long values.
     *
     * <p>This method supports both varargs usage for convenience with literal values and
     * array usage for passing existing arrays. For empty or null arrays, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Varargs usage with literal values
     * LongStream.of(1L, 2L, 3L, 4L, 5L)
     *     .sum();   // returns 15L
     *
     * // From an existing array
     * long[] numbers = {10L, 20L, 30L, 40L};
     * long max = LongStream.of(numbers)
     *     .max()
     *     .orElse(0L);   // returns 40L
     *
     * // Empty array produces empty stream
     * long[] empty = new long[0];
     * LongStream.of(empty).count();   // returns 0
     *
     * // Single element
     * LongStream.of(42L)
     *     .findFirst();   // returns OptionalLong[42L]
     *
     * // Processing data
     * long[] timestamps = {1609459200000L, 1612137600000L, 1614556800000L};
     * LongStream.of(timestamps)
     *     .filter(t -> t > 1610000000000L)
     *     .count();   // returns the count of timestamps after a certain date
     * }</pre>
     *
     * @param a the array of long values; may be empty or used as varargs
     * @return a LongStream containing the elements of the array, or an empty stream if the array is null or empty
     * @see #of(long[], int, int)
     * @see #empty()
     */
    public static LongStream of(final long... a) {
        return N.isEmpty(a) ? empty() : new ArrayLongStream(a);
    }

    /**
     * Returns a LongStream containing elements from the specified array between the given indices.
     * This is a static factory method that creates a sequential stream from a subrange of a primitive long array.
     *
     * <p>This method provides a way to create streams from portions of arrays without copying the array data,
     * making it efficient for processing subarrays. The stream includes elements from fromIndex (inclusive)
     * to toIndex (exclusive), following standard Java array slicing conventions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process a subrange of an array
     * long[] numbers = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
     * LongStream.of(numbers, 2, 7)
     *     .toArray();   // returns [3L, 4L, 5L, 6L, 7L]
     *
     * // Sum elements in a specific range
     * long[] data = {10L, 20L, 30L, 40L, 50L};
     * long sum = LongStream.of(data, 1, 4)
     *     .sum();   // returns 90L (20 + 30 + 40)
     *
     * // Filter and process subarray
     * long[] values = {100L, 200L, 300L, 400L, 500L, 600L};
     * long max = LongStream.of(values, 2, 5)
     *     .filter(v -> v > 250)
     *     .max()
     *     .orElse(0L);   // returns 500L
     *
     * // Empty range produces empty stream
     * LongStream.of(numbers, 3, 3)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the array of long values
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a LongStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(long...)
     * @see #of(Long[], int, int)
     */
    public static LongStream of(final long[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayLongStream(a, fromIndex, toIndex);
    }

    /**
     * Returns a LongStream containing the unboxed values from the specified Long array.
     * This is a static factory method that creates a sequential stream from a boxed Long array by unboxing each element.
     *
     * <p>This method automatically handles the conversion from boxed Long objects to primitive long values,
     * providing a convenient way to create primitive streams from object arrays. Null elements in the array
     * are unboxed to the primitive default value {@code 0L} when the stream is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from boxed Long array
     * Long[] boxedArray = {1L, 2L, 3L, 4L, 5L};
     * LongStream.of(boxedArray)
     *     .sum();   // returns 15L
     *
     * // Process database results or API responses
     * Long[] timestamps = fetchTimestampsFromDB();
     * long maxTimestamp = LongStream.of(timestamps)
     *     .max()
     *     .orElse(0L);
     *
     * // Filter and transform boxed values
     * Long[] counts = {10L, 20L, 30L, 40L, 50L};
     * long[] doubled = LongStream.of(counts)
     *     .filter(c -> c > 20)
     *     .map(c -> c * 2)
     *     .toArray();   // returns [60L, 80L, 100L]
     *
     * // Empty array produces empty stream
     * Long[] empty = new Long[0];
     * LongStream.of(empty).count();   // returns 0
     * }</pre>
     *
     * @param a the array of Long values; {@code null} elements are unboxed to {@code 0L} during stream processing
     * @return a LongStream containing the unboxed elements of the array, or an empty stream if the array is null or empty
     * @see #of(long...)
     * @see #of(Long[], int, int)
     * @see #of(Collection)
     */
    public static LongStream of(final Long[] a) {
        return Stream.of(a).mapToLong(FL.unbox());
    }

    /**
     * Returns a LongStream containing unboxed values from the specified Long array between the given indices.
     * This is a static factory method that creates a sequential stream from a subrange of a boxed Long array
     * by unboxing each element in the specified range.
     *
     * <p>This method combines array slicing with unboxing, providing an efficient way to create primitive long streams
     * from portions of boxed Long arrays. The stream includes elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process a subrange of a boxed array
     * Long[] boxedNumbers = {10L, 20L, 30L, 40L, 50L, 60L, 70L};
     * LongStream.of(boxedNumbers, 2, 5)
     *     .toArray();   // returns [30L, 40L, 50L]
     *
     * // Sum elements in a specific range of boxed array
     * Long[] values = {100L, 200L, 300L, 400L, 500L};
     * long sum = LongStream.of(values, 1, 4)
     *     .sum();   // returns 900L (200 + 300 + 400)
     *
     * // Process API response subrange
     * Long[] apiResults = fetchLongArrayFromAPI();
     * double average = LongStream.of(apiResults, 0, Math.min(10, apiResults.length))
     *     .average()
     *     .orElse(0.0);   // returns the average of the first 10 elements
     *
     * // Empty range produces empty stream
     * LongStream.of(boxedNumbers, 3, 3)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the array of Long values; {@code null} elements in the range are unboxed to {@code 0L} during stream processing
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a LongStream containing the unboxed elements from fromIndex to toIndex
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of array bounds
     * @see #of(Long[])
     * @see #of(long[], int, int)
     */
    public static LongStream of(final Long[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToLong(FL.unbox());
    }

    /**
     * Returns a LongStream containing the unboxed values from the specified Collection of Long values.
     * This is a static factory method that creates a sequential stream from a Collection of boxed Long objects
     * by unboxing each element.
     *
     * <p>This method provides a convenient bridge between Java Collections framework and primitive long streams,
     * automatically handling the unboxing conversion. The iteration order of the resulting stream matches the
     * iteration order of the collection. Null elements in the collection are unboxed to the primitive default
     * value {@code 0L} when the stream is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from a List
     * List<Long> list = Arrays.asList(1L, 2L, 3L, 4L, 5L);
     * LongStream.of(list)
     *     .sum();   // returns 15L
     *
     * // Process Set elements
     * Set<Long> uniqueIds = new HashSet<>(Arrays.asList(100L, 200L, 300L, 200L));
     * long[] sortedIds = LongStream.of(uniqueIds)
     *     .sorted()
     *     .toArray();   // returns [100L, 200L, 300L]
     *
     * // Transform collection data
     * Collection<Long> timestamps = database.getTimestamps();
     * long maxTimestamp = LongStream.of(timestamps)
     *     .max()
     *     .orElse(0L);
     *
     * // Filter and count
     * List<Long> counts = Arrays.asList(10L, 20L, 30L, 40L, 50L);
     * long largeCount = LongStream.of(counts)
     *     .filter(c -> c > 25)
     *     .count();   // returns 3
     *
     * // Empty collection produces empty stream
     * Collection<Long> empty = Collections.emptyList();
     * LongStream.of(empty).count();   // returns 0
     * }</pre>
     *
     * @param c the collection of Long values; {@code null} elements are unboxed to {@code 0L} during stream processing
     * @return a LongStream containing the unboxed elements of the collection, or an empty stream if the collection is null or empty
     * @see #of(Long[])
     * @see #of(LongIterator)
     * @see Stream#of(Collection)
     */
    public static LongStream of(final Collection<Long> c) {
        return Stream.of(c).mapToLong(FL.unbox());
    }

    /**
     * Returns a LongStream containing elements from the specified LongIterator.
     * This is a static factory method that creates a sequential stream from a LongIterator,
     * allowing iteration-based data sources to be processed using stream operations.
     *
     * <p>This method provides a bridge between iterator-based APIs and stream-based processing.
     * The resulting stream will consume elements from the iterator as needed during terminal operations.
     * Once the stream is consumed, the iterator will be exhausted and cannot be reused.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from a custom iterator
     * LongIterator iterator = new LongIterator() {
     *     private long current = 0;
     *     public boolean hasNext() { return current < 5; }
     *     public long nextLong() { return current++; }
     * };
     * LongStream.of(iterator)
     *     .toArray();   // returns [0L, 1L, 2L, 3L, 4L]
     *
     * // Process data from an iterator-based source
     * LongIterator dataIterator = dataSource.longIterator();
     * long sum = LongStream.of(dataIterator)
     *     .filter(n -> n > 0)
     *     .sum();
     *
     * // Convert LongList iterator to stream
     * LongList list = LongList.of(10L, 20L, 30L, 40L);
     * long max = LongStream.of(list.iterator())
     *     .max()
     *     .orElse(0L);   // returns 40L
     *
     * // Null iterator produces empty stream
     * LongStream.of((LongIterator) null)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param iterator the LongIterator providing the elements; will be exhausted when the stream is consumed
     * @return a LongStream containing the elements from the iterator, or an empty stream if iterator is null
     * @see #of(long[])
     * @see #of(Collection)
     * @see LongIterator
     */
    public static LongStream of(final LongIterator iterator) {
        return iterator == null ? empty() : new IteratorLongStream(iterator);
    }

    //    /**
    //     * Returns a LongStream from the specified java.util.stream.LongStream.
    //     *
    //     * @param stream the java.util.stream.LongStream to convert
    //     * @return a LongStream containing the elements from the input stream
    //     * @deprecated Use {@link #from(java.util.stream.LongStream)} instead
    //     */
    //    @Deprecated
    //    public static LongStream of(final java.util.stream.LongStream stream) { // Should the name be from?
    //        return from(stream);
    //    }

    /**
     * Returns a LongStream containing elements from the specified LongBuffer.
     * Elements are read from the buffer's current position to its limit.
     * This is a static factory method that creates a sequential stream from a NIO LongBuffer.
     *
     * <p>This method provides integration between Java NIO buffers and stream processing, allowing
     * efficient processing of buffer data using stream operations. The buffer's position is not modified
     * by this operation, as the stream accesses elements by index using the get() method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from a LongBuffer
     * LongBuffer buffer = LongBuffer.allocate(5);
     * buffer.put(new long[]{1L, 2L, 3L, 4L, 5L});
     * buffer.flip();
     * LongStream.of(buffer)
     *     .sum();   // returns 15L
     *
     * // Process memory-mapped file data
     * LongBuffer mmapBuffer = mapFileToLongBuffer("data.bin");
     * long max = LongStream.of(mmapBuffer)
     *     .max()
     *     .orElse(0L);
     *
     * // Filter buffer contents
     * LongBuffer dataBuffer = LongBuffer.wrap(new long[]{10L, 20L, 30L, 40L, 50L});
     * long[] filtered = LongStream.of(dataBuffer)
     *     .filter(n -> n > 25)
     *     .toArray();   // returns [30L, 40L, 50L]
     *
     * // Process buffer slice
     * LongBuffer slice = buffer.slice();
     * long count = LongStream.of(slice).count();
     *
     * // Null buffer produces empty stream
     * LongStream.of((LongBuffer) null)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param buf the LongBuffer to read from; the buffer's position remains unchanged
     * @return a LongStream containing the elements from the buffer's current position to its limit,
     *         or an empty stream if buf is null
     * @see #of(long[])
     * @see java.nio.LongBuffer
     */
    public static LongStream of(final LongBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToLong(buf::get);
    }

    /**
     * Returns a LongStream containing the value from the specified OptionalLong if present.
     * This is a static factory method that creates a stream from an Abacus OptionalLong,
     * producing a single-element stream if the optional contains a value, or an empty stream otherwise.
     *
     * <p>This method provides a convenient way to convert optional values into streams, enabling
     * seamless integration of optional-based APIs with stream processing pipelines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert present optional to stream
     * OptionalLong optional = OptionalLong.of(42L);
     * LongStream.of(optional)
     *     .toArray();   // returns [42L]
     *
     * // Empty optional produces empty stream
     * OptionalLong empty = OptionalLong.empty();
     * LongStream.of(empty)
     *     .count();   // returns 0
     *
     * // Chain optional operations with streams
     * OptionalLong result = findValue();
     * long processed = LongStream.of(result)
     *     .map(n -> n * 2)
     *     .findFirst()
     *     .orElse(0L);
     *
     * // Combine multiple optionals
     * long sum = LongStream.concat(
     *     LongStream.of(OptionalLong.of(10L)),
     *     LongStream.of(OptionalLong.empty()),
     *     LongStream.of(OptionalLong.of(20L))
     * ).sum();   // returns 30L
     *
     * // Null optional produces empty stream
     * LongStream.of((OptionalLong) null)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param op the OptionalLong to convert; may be null or empty
     * @return a LongStream containing the value if present, otherwise an empty stream
     * @see #ofNullable(Long)
     * @see OptionalLong
     */
    public static LongStream of(final OptionalLong op) {
        return op == null || op.isEmpty() ? LongStream.empty() : LongStream.of(op.get());
    }

    /**
     * Returns a LongStream containing the value from the specified java.util.OptionalLong if present.
     * This is a static factory method that creates a stream from a JDK OptionalLong,
     * producing a single-element stream if the optional contains a value, or an empty stream otherwise.
     *
     * <p>This method provides interoperability with the standard Java OptionalLong from java.util package,
     * enabling seamless integration between JDK optional-based APIs and Abacus stream processing pipelines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert JDK optional to stream
     * java.util.OptionalLong optional = java.util.OptionalLong.of(100L);
     * LongStream.of(optional)
     *     .toArray();   // returns [100L]
     *
     * // Empty JDK optional produces empty stream
     * java.util.OptionalLong empty = java.util.OptionalLong.empty();
     * LongStream.of(empty)
     *     .count();   // returns 0
     *
     * // Integrate JDK stream results with Abacus streams
     * java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1L, 2L, 3L);
     * java.util.OptionalLong jdkMax = jdkStream.max();
     * long result = LongStream.of(jdkMax)
     *     .map(n -> n * 2)
     *     .findFirst()
     *     .orElse(0L);   // returns 6L
     *
     * // Process method results returning JDK OptionalLong
     * java.util.OptionalLong dbResult = repository.findMaxId();
     * long nextId = LongStream.of(dbResult)
     *     .map(id -> id + 1)
     *     .findFirst()
     *     .orElse(1L);
     *
     * // Null optional produces empty stream
     * LongStream.of((java.util.OptionalLong) null)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param op the java.util.OptionalLong to convert; may be null or empty
     * @return a LongStream containing the value if present, otherwise an empty stream
     * @see #of(OptionalLong)
     * @see #ofNullable(Long)
     * @see java.util.OptionalLong
     */
    public static LongStream of(final java.util.OptionalLong op) {
        return op == null || op.isEmpty() ? LongStream.empty() : LongStream.of(op.getAsLong());
    }

    private static final Function<long[], LongStream> flatMapper = LongStream::of;

    private static final Function<long[][], LongStream> flattMapper = LongStream::flatten;

    /**
     * Flattens a two-dimensional array of longs into a single LongStream.
     * Elements are concatenated in row-major order (horizontally), meaning all elements from the first row,
     * then all elements from the second row, and so on.
     *
     * <p>This is a static factory method that provides a convenient way to convert nested array structures
     * into a flat stream for processing. The method handles null or empty arrays gracefully by returning an empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten a 2D array horizontally (row-major order)
     * long[][] matrix = {
     *     {1L, 2L, 3L},
     *     {4L, 5L, 6L},
     *     {7L, 8L, 9L}
     * };
     * LongStream.flatten(matrix)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * // Sum all elements in a 2D array
     * long[][] data = {{10L, 20L}, {30L, 40L}, {50L, 60L}};
     * long sum = LongStream.flatten(data)
     *     .sum();   // returns 210L
     *
     * // Process jagged arrays (different row lengths)
     * long[][] jagged = {{1L, 2L}, {3L, 4L, 5L}, {6L}};
     * LongStream.flatten(jagged)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     *
     * // Filter flattened elements
     * long[][] values = {{1L, 2L, 3L}, {4L, 5L, 6L}};
     * long[] even = LongStream.flatten(values)
     *     .filter(n -> n % 2 == 0)
     *     .toArray();   // returns [2L, 4L, 6L]
     *
     * // Empty array produces empty stream
     * LongStream.flatten(new long[0][])
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the two-dimensional array to flatten; may be null, empty, or contain null rows
     * @return a LongStream containing all elements from the two-dimensional array in row-major order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(long[][], boolean)
     * @see #flatten(long[][][])
     */
    public static LongStream flatten(final long[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToLong(flatMapper);
    }

    /**
     * Flattens a two-dimensional array of longs into a single LongStream with control over traversal direction.
     * When {@code vertically} is {@code false}, elements are read in row-major order (horizontally across rows).
     * When {@code vertically} is {@code true}, elements are read in column-major order (vertically down columns).
     *
     * <p>This method provides flexibility in how 2D data structures are linearized, which is useful for
     * matrix operations, image processing, or any scenario where the traversal order matters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[][] matrix = {
     *     {1L, 2L, 3L},
     *     {4L, 5L, 6L},
     *     {7L, 8L, 9L}
     * };
     *
     * // Flatten horizontally (row by row)
     * LongStream.flatten(matrix, false)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * // Flatten vertically (column by column)
     * LongStream.flatten(matrix, true)
     *     .toArray();   // returns [1L, 4L, 7L, 2L, 5L, 8L, 3L, 6L, 9L]
     *
     * // Process jagged array vertically (handles different row lengths)
     * long[][] jagged = {
     *     {1L, 2L, 3L},
     *     {4L, 5L},
     *     {6L, 7L, 8L, 9L}
     * };
     * LongStream.flatten(jagged, true)
     *     .toArray();   // returns [1L, 4L, 6L, 2L, 5L, 7L, 3L, 8L, 9L]
     *
     * // Sum columns separately using vertical flattening
     * long[][] data = {{10L, 20L}, {30L, 40L}, {50L, 60L}};
     * // Column sums would be calculated by grouping vertically flattened elements
     * }</pre>
     *
     * @param a the two-dimensional array to flatten; may be null, empty, or contain null/jagged rows
     * @param vertically if {@code true}, elements are read column by column (column-major order);
     *                   if {@code false}, row by row (row-major order)
     * @return a LongStream containing all elements from the two-dimensional array in the specified traversal order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(long[][])
     * @see #flatten(long[][], long, boolean)
     */
    public static LongStream flatten(final long[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToLong(flatMapper);
        }

        long n = 0;

        for (final long[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final LongIterator iter = new LongIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public long nextLong() {
                if (cnt++ >= count) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (rowNum == rows) {
                    rowNum = 0;
                    colNum++;
                }

                while (a[rowNum] == null || colNum >= a[rowNum].length) {
                    if (rowNum < rows - 1) {
                        rowNum++;
                    } else {
                        rowNum = 0;
                        colNum++;
                    }
                }

                return a[rowNum++][colNum];
            }
        };

        return of(iter);
    }

    /**
     * Flattens a two-dimensional array of longs into a single LongStream with alignment padding.
     * When processing jagged arrays (arrays with rows of different lengths), shorter rows are padded
     * with the specified {@code valueForAlignment} to match the length of the longest row.
     *
     * <p>This method is particularly useful when working with irregular data structures that need to be
     * normalized or when processing matrix-like data where consistent dimensions are required. The alignment
     * ensures that all rows have the same effective length during flattening.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[][] jagged = {
     *     {1L, 2L, 3L},
     *     {4L, 5L},        // row is shorter
     *     {6L, 7L, 8L, 9L} // row is the longest
     * };
     *
     * // Flatten horizontally with alignment (pad with 0)
     * LongStream.flatten(jagged, 0L, false)
     *     .toArray();   // returns [1L, 2L, 3L, 0L, 4L, 5L, 0L, 0L, 6L, 7L, 8L, 9L]
     *                   // Each row padded to length 4 (longest row)
     *
     * // Flatten vertically with alignment (pad with -1)
     * LongStream.flatten(jagged, -1L, true)
     *     .toArray();   // returns [1L, 4L, 6L, 2L, 5L, 7L, 3L, -1L, 8L, -1L, -1L, 9L]
     *                   // Columns aligned with -1 for missing values
     *
     * // Normalize data tables with missing values
     * long[][] dataTable = {
     *     {100L, 200L, 300L},
     *     {400L},          // row is missing values
     *     {500L, 600L}     // row is missing one value
     * };
     * long[] normalized = LongStream.flatten(dataTable, 0L, false)
     *     .toArray();   // returns [100, 200, 300, 400, 0, 0, 500, 600, 0]
     *
     * // Sum columns with padding
     * long[][] matrix = {{1L, 2L}, {3L, 4L, 5L}, {6L}};
     * // When flattened vertically with 0 padding, columns can be summed consistently
     * }</pre>
     *
     * @param a the two-dimensional array to flatten; may be null, empty, or contain rows of different lengths
     * @param valueForAlignment the value to use for padding shorter rows to match the longest row length
     * @param vertically if {@code true}, elements are read column by column with vertical padding;
     *                   if {@code false}, row by row with horizontal padding
     * @return a LongStream containing all elements with alignment padding applied to shorter rows,
     *         or an empty stream if the array is null or empty
     * @see #flatten(long[][], boolean)
     * @see #flatten(long[][])
     */
    public static LongStream flatten(final long[][] a, final long valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final long[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = ((long) rows) * cols;
        LongIterator iter = null;

        if (vertically) {
            iter = new LongIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public long nextLong() {
                    if (cnt++ >= count) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    if (rowNum == rows) {
                        rowNum = 0;
                        colNum++;
                    }

                    if (a[rowNum] == null || colNum >= a[rowNum].length) {
                        rowNum++;
                        return valueForAlignment;
                    } else {
                        return a[rowNum++][colNum];
                    }
                }
            };

        } else {
            iter = new LongIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public long nextLong() {
                    if (cnt++ >= count) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    if (colNum >= cols) {
                        colNum = 0;
                        rowNum++;
                    }

                    if (a[rowNum] == null || colNum >= a[rowNum].length) {
                        colNum++;
                        return valueForAlignment;
                    } else {
                        return a[rowNum][colNum++];
                    }
                }
            };
        }

        return of(iter);
    }

    /**
     * Flattens a three-dimensional array of longs into a single LongStream.
     * The array is flattened in depth-first order, processing all elements of the innermost arrays first,
     * then moving through the middle dimension, and finally the outermost dimension.
     *
     * <p>This is a static factory method that provides a convenient way to convert 3D nested array structures
     * into a flat stream for processing. This is useful for volumetric data, 3D matrices, or nested data structures.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten a 3D array (2x2x2 cube)
     * long[][][] cube = {
     *     {{1L, 2L}, {3L, 4L}},
     *     {{5L, 6L}, {7L, 8L}}
     * };
     * LongStream.flatten(cube)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L]
     *
     * // Sum all elements in 3D array
     * long[][][] data = {
     *     {{1L, 2L}, {3L, 4L}},
     *     {{5L, 6L}, {7L, 8L}},
     *     {{9L, 10L}, {11L, 12L}}
     * };
     * long sum = LongStream.flatten(data)
     *     .sum();   // returns 78L
     *
     * // Filter elements from 3D structure
     * long[][][] values = {{{1L, 2L, 3L}}, {{4L, 5L, 6L}}};
     * long[] even = LongStream.flatten(values)
     *     .filter(n -> n % 2 == 0)
     *     .toArray();   // returns [2L, 4L, 6L]
     *
     * // Process 3D image/voxel data
     * long[][][] voxelData = loadVoxelData();  // 3D volumetric data
     * long maxIntensity = LongStream.flatten(voxelData)
     *     .max()
     *     .orElse(0L);
     *
     * // Empty array produces empty stream
     * LongStream.flatten(new long[0][][])
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the three-dimensional array to flatten; may be null, empty, or contain null elements at any level
     * @return a LongStream containing all elements from the three-dimensional array in depth-first order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(long[][])
     * @see #flatten(long[][], boolean)
     */
    public static LongStream flatten(final long[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToLong(flattMapper);
    }

    /**
     * Returns a LongStream of sequential-ordered values from startInclusive (inclusive) to endExclusive (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.range(1, 5).toArray();
     *     // Result: [1, 2, 3, 4]
     *
     * LongStream.range(0, 3)
     *     .map(n -> n * n)
     *     .toArray();
     *     // Result: [0, 1, 4]
     *
     * LongStream.range(5, 5).toArray();
     *     // Result: [] (empty stream)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a LongStream of sequential values
     */
    public static LongStream range(final long startInclusive, final long endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        } else if (endExclusive - startInclusive < 0) {
            final long m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();
            return concat(range(startInclusive, startInclusive + m), range(startInclusive + m, (startInclusive + m) + m),
                    range((startInclusive + m) + m, endExclusive));
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return next++;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= cnt) {
                    cnt = 0;
                    return;
                }

                cnt -= n;
                next += n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public long[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a LongStream of values from startInclusive (inclusive) to endExclusive (exclusive) with the specified step.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.range(0, 10, 2).toArray();
     *     // Result: [0, 2, 4, 6, 8] (even numbers)
     *
     * LongStream.range(10, 0, -2).toArray();
     *     // Result: [10, 8, 6, 4, 2] (descending)
     *
     * LongStream.range(1, 20, 5).toArray();
     *     // Result: [1, 6, 11, 16]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step size (must not be zero)
     * @return a LongStream of values with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static LongStream range(final long startInclusive, final long endExclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        if (startInclusive + by == endExclusive) {
            return of(startInclusive);
        }

        if ((by > 0 && endExclusive - startInclusive < 0) || (by < 0 && startInclusive - endExclusive < 0)) {
            long m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();

            if ((by > 0 && by > m) || (by < 0 && by < m)) {
                return concat(range(startInclusive, startInclusive + by, by), range(startInclusive + by, endExclusive, by));
            } else {
                m -= m % by; // round m toward zero to an exact multiple of by (m % by carries the sign of m), so the split points stay on the step grid.
                return concat(range(startInclusive, startInclusive + m, by), range(startInclusive + m, (startInclusive + m) + m, by),
                        range((startInclusive + m) + m, endExclusive, by));
            }
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final long result = next;
                next += by;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= cnt) {
                    cnt = 0;
                    return;
                }

                cnt -= n;
                next = Math.addExact(next, Math.multiplyExact(n, by));
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public long[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a LongStream of sequential-ordered values from startInclusive (inclusive) to endInclusive (inclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.rangeClosed(1L, 5L)
     *     .toArray();   // returns [1, 2, 3, 4, 5]
     *
     * LongStream.rangeClosed(10L, 15L)
     *     .sum();   // returns 75 (10 + 11 + 12 + 13 + 14 + 15)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a LongStream of sequential values
     */
    public static LongStream rangeClosed(final long startInclusive, final long endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        } else if (endInclusive - startInclusive + 1 <= 0) {
            final long m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();
            return concat(range(startInclusive, startInclusive + m), range(startInclusive + m, (startInclusive + m) + m),
                    rangeClosed((startInclusive + m) + m, endInclusive));
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return next++;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= cnt) {
                    cnt = 0;
                    return;
                }

                cnt -= n;
                next += n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public long[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a LongStream of values from startInclusive (inclusive) to endInclusive (inclusive) with the specified step.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream.rangeClosed(0L, 10L, 2L)
     *     .toArray();   // returns [0, 2, 4, 6, 8, 10]
     *
     * LongStream.rangeClosed(10L, 1L, -2L)
     *     .toArray();   // returns [10, 8, 6, 4, 2]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step size (must not be zero)
     * @return a LongStream of values with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static LongStream rangeClosed(final long startInclusive, final long endInclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        if ((by > 0 && endInclusive - startInclusive < 0) || (by < 0 && startInclusive - endInclusive < 0) || ((endInclusive - startInclusive) / by + 1 <= 0)) {
            long m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();

            if ((by > 0 && by > m) || (by < 0 && by < m)) {
                return concat(range(startInclusive, startInclusive + by, by), rangeClosed(startInclusive + by, endInclusive, by));
            } else {
                m -= m % by; // round m toward zero to an exact multiple of by (m % by carries the sign of m), so the split points stay on the step grid.
                return concat(range(startInclusive, startInclusive + m, by), range(startInclusive + m, (startInclusive + m) + m, by),
                        rangeClosed((startInclusive + m) + m, endInclusive, by));
            }
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = (endInclusive - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final long result = next;
                next += by;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= cnt) {
                    cnt = 0;
                    return;
                }

                cnt -= n;
                next = Math.addExact(next, Math.multiplyExact(n, by));
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public long[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a LongStream consisting of the specified element repeated n times.
     * This is a static factory method that creates a finite stream containing exactly n copies
     * of the given element.
     *
     * <p>This method is useful for initializing arrays or collections with a default value,
     * padding data structures, or generating test data. For n=0, an empty stream is returned.
     * For small values (n &lt; 10), the stream is optimized using an array-backed implementation.
     *
     * <p><b>Performance Note:</b> For small n values (less than 10), this method uses an
     * array-based implementation for better performance. For larger values, it uses a
     * lazy iterator-based approach to avoid excessive memory allocation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create array of repeated values
     * LongStream.repeat(5L, 3L)
     *     .toArray();   // returns [5L, 5L, 5L]
     *
     * // Calculate sum of repeated elements
     * LongStream.repeat(100L, 5L)
     *     .sum();   // returns 500L
     *
     * // Initialize with default value
     * long[] defaults = LongStream.repeat(0L, 10L).toArray();   // [0L, 0L, ..., 0L]
     *
     * // Combine with other operations
     * LongStream.repeat(2L, 4L)
     *     .map(x -> x * x)
     *     .toArray();   // returns [4L, 4L, 4L, 4L]
     *
     * // Empty stream when n is 0
     * LongStream.repeat(99L, 0L).count();   // returns 0
     *
     * // Padding data with repeated values
     * LongStream combined = LongStream.concat(
     *     LongStream.of(1L, 2L, 3L),
     *     LongStream.repeat(0L, 5L)
     * );   // returns [1L, 2L, 3L, 0L, 0L, 0L, 0L, 0L]
     * }</pre>
     *
     * @param element the element to repeat
     * @param n the number of times to repeat the element (must be non-negative)
     * @return a LongStream containing n copies of the element, or an empty stream if n is 0
     * @throws IllegalArgumentException if n is negative
     * @see #generate(LongSupplier)
     * @see #iterate(long, LongUnaryOperator)
     */
    public static LongStream repeat(final long element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return element;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cnt = n >= cnt ? 0 : cnt - n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public long[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final long[] result = new long[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns an infinite LongStream of pseudorandom long values generated by a {@link SecureRandom} instance.
     * This is a static factory method that creates an unbounded stream of random values across the full
     * range of long values (from {@code Long.MIN_VALUE} to {@code Long.MAX_VALUE}).
     *
     * <p>The stream is infinite by nature and will continue to generate random values indefinitely.
     * Always use a limiting operation such as {@link #limit(long)}, {@link #takeWhile(LongPredicate)},
     * or a short-circuiting terminal operation to prevent infinite loops.
     *
     * <p><b>Security Note:</b> This method uses {@link SecureRandom} for cryptographically strong
     * random number generation. While suitable for general purposes, be aware that SecureRandom
     * may be slower than {@link java.util.Random}. For non-security-critical applications requiring
     * better performance, consider using {@link #generate(LongSupplier)} with a custom Random instance.
     *
     * <p><b>Thread Safety:</b> The underlying SecureRandom instance is thread-safe, making this
     * stream safe for parallel operations. However, parallel streams may produce different sequences
     * than sequential streams due to concurrent access to the random generator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random long values
     * long[] randomLongs = LongStream.random()
     *     .limit(10)
     *     .toArray();
     *
     * // Generate random positive longs
     * long[] positiveLongs = LongStream.random()
     *     .map(Math::abs)
     *     .limit(5)
     *     .toArray();
     *
     * // Generate random longs in a specific range [0, 100)
     * long[] rangedRandoms = LongStream.random()
     *     .map(n -> Math.abs(n) % 100)
     *     .limit(20)
     *     .toArray();
     *
     * // Find first random even number
     * OptionalLong firstEven = LongStream.random()
     *     .filter(n -> n % 2 == 0)
     *     .findFirst();
     *
     * // Generate random IDs
     * List<Long> randomIds = LongStream.random()
     *     .map(Math::abs)
     *     .distinct()
     *     .limit(1000)
     *     .boxed()
     *     .collect(Collectors.toList());
     *
     * // Statistical analysis of random values
     * LongSummaryStatistics stats = LongStream.random()
     *     .limit(10000)
     *     .summaryStatistics();
     * }</pre>
     *
     * <p><b>Warning:</b> Never consume this stream without a limiting operation:
     * <pre>{@code
     * // DON'T DO THIS - will run forever!
     * // LongStream.random().forEach(System.out::println);
     *
     * // DO THIS instead:
     * LongStream.random().limit(100).forEach(System.out::println);
     * }</pre>
     *
     * @return an infinite LongStream of pseudorandom long values generated by SecureRandom
     * @see #generate(LongSupplier)
     * @see SecureRandom
     * @see java.util.Random#longs()
     */
    public static LongStream random() {
        return generate(RAND::nextLong);
    }

    /**
     * Generates an infinite LongStream that emits timestamp values at fixed intervals.
     * The stream starts immediately and continues indefinitely.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Emit timestamps every 100ms, take first 3
     * long[] timestamps = LongStream.interval(100)
     *     .limit(3)
     *     .toArray();   // returns 3 timestamps 100ms apart
     *
     * // Empty result when limited to 0
     * LongStream.interval(50).limit(0).count();   // returns 0
     * }</pre>
     *
     * @param intervalInMillis the time interval in milliseconds between emissions
     * @return an infinite LongStream that emits timestamp values at the specified intervals
     * @see #interval(long, long, TimeUnit)
     * @see N#sleepUninterruptibly(long)
     * @see System#currentTimeMillis()
     */
    @Beta
    public static LongStream interval(final long intervalInMillis) {
        return interval(0, intervalInMillis);
    }

    /**
     * Generates an infinite LongStream that emits timestamp values at fixed intervals after an initial delay.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Emit timestamps with 200ms delay then 100ms interval, take first 2
     * long[] timestamps = LongStream.interval(200, 100)
     *     .limit(2)
     *     .toArray();   // returns 2 timestamps
     *
     * // Zero delay starts immediately
     * long[] immediate = LongStream.interval(0, 50)
     *     .limit(3)
     *     .toArray();   // returns 3 timestamps 50ms apart
     * }</pre>
     *
     * @param delayInMillis the initial delay in milliseconds before the first emission
     * @param intervalInMillis the time interval in milliseconds between subsequent emissions
     * @return an infinite LongStream that emits timestamp values at the specified intervals after the delay
     * @see #interval(long, long, TimeUnit)
     * @see N#sleepUninterruptibly(long)
     * @see System#currentTimeMillis()
     */
    @Beta
    public static LongStream interval(final long delayInMillis, final long intervalInMillis) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Generates an infinite LongStream that emits timestamp values at fixed intervals with a custom time unit.
     * <p>
     * This method creates a time-based stream that emits values representing timestamps at regular intervals.
     * The stream starts after an initial delay and continues indefinitely, making it suitable for periodic
     * operations, scheduling, or time-based data generation.
     * </p>
     *
     * <p>Implementation details:</p>
     * <ul>
     *   <li>The stream emits timestamp values (in milliseconds since epoch) at each interval</li>
     *   <li>Uses {@link N#sleepUninterruptibly(long)} to ensure precise timing between emissions</li>
     *   <li>The iterator blocks the calling thread until the next scheduled emission time</li>
     *   <li>Each emitted value represents the scheduled time for that emission</li>
     * </ul>
     *
     * <p>Timing behavior:</p>
     * <ul>
     *   <li><strong>Initial delay:</strong> The first value is emitted after the specified delay</li>
     *   <li><strong>Subsequent intervals:</strong> Values are emitted at fixed intervals from the first emission</li>
     *   <li><strong>Drift compensation:</strong> Uses absolute timing to prevent cumulative drift</li>
     *   <li><strong>Thread blocking:</strong> The calling thread sleeps between emissions to maintain timing</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Emit every 5 seconds after 2 second delay
     * LongStream.interval(2, 5, TimeUnit.SECONDS)
     *     .limit(3)
     *     .forEach(timestamp -> System.out.println("Tick: " + timestamp));
     *
     * // Emit every 100 milliseconds immediately
     * LongStream.interval(0, 100, TimeUnit.MILLISECONDS)
     *     .limit(10)
     *     .map(t -> System.currentTimeMillis() - t) // maps to the actual delay
     *     .forEach(delay -> System.out.println("Delay: " + delay + "ms"));
     * }</pre>
     *
     * <p><strong>Thread safety:</strong> This stream is designed for single-threaded consumption.
     * Multiple threads consuming the same stream may lead to unpredictable timing behavior.</p>
     *
     * <p><strong>Resource management:</strong> Since this creates an infinite stream, always use operations
     * like {@link #limit(long)} or conditions that will terminate the stream to prevent infinite loops.</p>
     *
     * <p><strong>Performance considerations:</strong></p>
     * <ul>
     *   <li>Each emission blocks the consuming thread until the scheduled time</li>
     *   <li>Very short intervals may impact performance due to frequent sleeping/waking</li>
     *   <li>The stream maintains accurate timing even under system load</li>
     * </ul>
     *
     * @param delay the initial delay before the first emission
     * @param interval the time interval between subsequent emissions
     * @param unit the time unit for both delay and interval parameters
     * @return an infinite LongStream that emits timestamp values at the specified intervals
     * @throws NullPointerException if unit is null
     *
     * @see #interval(long)
     * @see #interval(long, long)
     * @see N#sleepUninterruptibly(long)
     * @see System#currentTimeMillis()
     */
    @Beta
    public static LongStream interval(final long delay, final long interval, final TimeUnit unit) {
        return of(new LongIteratorEx() {
            private final long intervalInMillis = unit.toMillis(interval);
            private long nextTime = System.currentTimeMillis() + unit.toMillis(delay);
            private long ret = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                final long now = System.currentTimeMillis();

                if (now < nextTime) {
                    N.sleepUninterruptibly(nextTime - now);
                }

                ret = nextTime;

                nextTime += intervalInMillis;

                return ret;
            }
        });
    }

    /**
     * Creates a stream that iterates using the given hasNext and next suppliers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read values from an external source with a condition
     * AtomicLong counter = new AtomicLong(0);
     * LongStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement())
     *     .toArray();   // returns [0L, 1L, 2L, 3L, 4L]
     *
     * // Empty stream when hasNext returns false immediately
     * LongStream.iterate(() -> false, () -> 42L)
     *     .toArray();   // returns []
     *
     * // Generate values with external termination
     * AtomicLong src = new AtomicLong(10L);
     * LongStream.iterate(() -> src.get() > 0, () -> src.getAndDecrement())
     *     .toArray();   // returns [10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L]
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a LongSupplier that provides the next long value in the iteration
     * @return a LongStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or next is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static LongStream iterate(final BooleanSupplier hasNext, final LongSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorLongStream(new LongIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsLong();
            }
        });
    }

    /**
     * Creates a stream that iterates from an initial value, applying a function to generate subsequent values,
     * and continues as long as a BooleanSupplier returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate values with external termination condition
     * AtomicLong counter = new AtomicLong(5);
     * LongStream.iterate(0L, () -> counter.getAndDecrement() > 0, n -> n + 1)
     *     .toArray();   // returns [0L, 1L, 2L, 3L, 4L] (5 iterations)
     *
     * // Empty stream when BooleanSupplier returns false immediately
     * LongStream.iterate(10L, () -> false, n -> n * 2)
     *     .toArray();   // returns []
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a LongStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, BooleanSupplier, java.util.function.UnaryOperator)
     */
    public static LongStream iterate(final long init, final BooleanSupplier hasNext, final LongUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long cur = 0;
            private boolean isFirst = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsLong(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates a finite stream that iterates from an initial value, applying a function to generate subsequent values,
     * and continues as long as a predicate is satisfied. This is a static factory method that produces a
     * conditionally-terminated sequential stream.
     *
     * <p>This method is useful for generating sequences where the termination condition depends on the
     * value itself, such as mathematical series, countdowns, or converging sequences. The predicate is
     * evaluated on each generated value before it is emitted, allowing for dynamic termination.
     *
     * <p><b>Iteration Behavior:</b>
     * <ul>
     *   <li>The initial value is tested with the predicate first</li>
     *   <li>If the predicate returns {@code true}, the value is emitted and the function is applied</li>
     *   <li>The process continues until the predicate returns {@code false}</li>
     *   <li>The value that fails the predicate is NOT included in the stream</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate powers of 2 less than 100
     * LongStream.iterate(1L, n -> n < 100, n -> n * 2)
     *     .toArray();   // returns [1L, 2L, 4L, 8L, 16L, 32L, 64L]
     *
     * // Countdown from 10 to 1
     * LongStream.iterate(10L, n -> n > 0, n -> n - 1)
     *     .forEach(System.out::println);   // prints 10, 9, 8, ..., 1
     *
     * // Generate sequence with custom termination
     * LongStream.iterate(100L, n -> n >= 10, n -> n / 2)
     *     .toArray();   // returns [100L, 50L, 25L, 12L]
     *
     * // Empty stream when initial value fails predicate
     * LongStream.iterate(100L, n -> n < 50, n -> n * 2)
     *     .count();   // returns 0
     *
     * // Collatz conjecture sequence
     * long start = 27L;
     * LongStream.iterate(start, n -> n != 1, n -> n % 2 == 0 ? n / 2 : 3 * n + 1)
     *     .forEach(System.out::println);   // prints Collatz sequence
     * }</pre>
     *
     * <p><b>Comparison with alternatives:</b>
     * <pre>{@code
     * // Using iterate with predicate (concise, declarative)
     * LongStream.iterate(1L, n -> n < 10, n -> n * 2).toArray();
     *
     * // Using infinite iterate + takeWhile (similar but different)
     * LongStream.iterate(1L, n -> n * 2).takeWhile(n -> n < 10).toArray();
     *
     * // Using iterate with BooleanSupplier (when you need external state)
     * AtomicLong counter = new AtomicLong(0);
     * LongStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement());
     * }</pre>
     *
     * @param init the initial seed value
     * @param hasNext a stateless predicate that tests each value to determine if the stream should continue;
     *                returns {@code true} to continue iteration, {@code false} to terminate
     * @param f a stateless function to apply to the previous element to generate the next element
     * @return a finite LongStream of elements generated by the iteration while the predicate holds
     * @throws IllegalArgumentException if hasNext or f is null
     * @see #iterate(long, LongUnaryOperator)
     * @see #iterate(BooleanSupplier, LongSupplier)
     * @see #takeWhile(LongPredicate)
     * @see java.util.stream.LongStream#iterate(long, java.util.function.LongPredicate, java.util.function.LongUnaryOperator)
     */
    public static LongStream iterate(final long init, final LongPredicate hasNext, final LongUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long cur = 0;
            private boolean isFirst = true;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal && hasMore) {
                    if (isFirst) {
                        isFirst = false;
                        hasNextVal = hasNext.test(cur = init);
                    } else {
                        hasNextVal = hasNext.test(cur = f.applyAsLong(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return cur;
            }
        });
    }

    /**
     * Creates an infinite stream that iterates from an initial value, applying a function to generate subsequent values.
     * This is a static factory method that produces an unbounded sequential stream where each element is
     * generated by applying the function to the previous element.
     *
     * <p>The stream is infinite by nature and will continue to generate values indefinitely unless
     * limited by operations such as {@link #limit(long)}, {@link #takeWhile(LongPredicate)}, or
     * short-circuiting terminal operations. Always ensure you have a termination condition to prevent
     * infinite loops.
     *
     * <p>This method is ideal for generating mathematical sequences, iterative calculations, or
     * any scenario where each value depends on the previous one according to a fixed rule.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate first 10 powers of 2
     * LongStream.iterate(1L, n -> n * 2)
     *     .limit(10)
     *     .toArray();   // returns [1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L]
     *
     * // Generate arithmetic sequence
     * LongStream.iterate(5L, n -> n + 3)
     *     .limit(5)
     *     .toArray();   // returns [5L, 8L, 11L, 14L, 17L]
     *
     * // Generate factorial sequence
     * long[] factorials = LongStream.iterate(1L, n -> n + 1)
     *     .limit(10)
     *     .map(n -> {
     *         long fact = 1;
     *         for (long i = 2; i <= n; i++) fact *= i;
     *         return fact;
     *     })
     *     .toArray();
     *
     * // Find first value in sequence satisfying a condition
     * OptionalLong firstOver1000 = LongStream.iterate(1L, n -> n * 3)
     *     .filter(n -> n > 1000)
     *     .findFirst();   // returns OptionalLong[2187L]
     *
     * // Generate with conditional termination using takeWhile
     * LongStream.iterate(2L, n -> n * 2)
     *     .takeWhile(n -> n < 1000)
     *     .toArray();   // returns [2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L]
     *
     * // Constant sequence (identity function)
     * LongStream.iterate(42L, n -> n)
     *     .limit(5)
     *     .toArray();   // returns [42L, 42L, 42L, 42L, 42L]
     * }</pre>
     *
     * <p><b>Warning:</b> Never consume this stream without a limiting operation:
     * <pre>{@code
     * // DON'T DO THIS - will run forever!
     * // LongStream.iterate(0L, n -> n + 1).forEach(System.out::println);
     *
     * // DO THIS instead:
     * LongStream.iterate(0L, n -> n + 1).limit(100).forEach(System.out::println);
     * }</pre>
     *
     * <p><b>Comparison with alternatives:</b>
     * <pre>{@code
     * // Infinite iterate (simple, state is in the value)
     * LongStream.iterate(1L, n -> n * 2).limit(10);
     *
     * // Iterate with predicate (terminates automatically)
     * LongStream.iterate(1L, n -> n < 1024, n -> n * 2);
     *
     * // Generate (when values don't depend on previous)
     * LongStream.generate(() -> random.nextLong()).limit(10);
     *
     * // Range (for simple sequences)
     * LongStream.range(0L, 10L);
     * }</pre>
     *
     * @param init the initial seed value
     * @param f a stateless function to apply to the previous element to generate the next element
     * @return an infinite LongStream of elements generated by repeatedly applying the function
     * @throws IllegalArgumentException if f is null
     * @see #iterate(long, LongPredicate, LongUnaryOperator)
     * @see #generate(LongSupplier)
     * @see #limit(long)
     * @see #takeWhile(LongPredicate)
     * @see java.util.stream.LongStream#iterate(long, java.util.function.LongUnaryOperator)
     */
    public static LongStream iterate(final long init, final LongUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsLong(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Generates an infinite LongStream using the provided LongSupplier.
     * This is a static factory method that produces an unbounded sequential stream where each element
     * is generated by invoking the supplier independently.
     *
     * <p>The supplier is invoked to generate each element of the stream on-demand. Unlike
     * {@link #iterate(long, LongUnaryOperator)}, where each value depends on the previous one,
     * {@code generate} produces values independently. This makes it suitable for random values,
     * constant values, or values that depend on external state.
     *
     * <p>The stream is infinite by nature and will continue to generate values indefinitely.
     * Always use a limiting operation such as {@link #limit(long)}, {@link #takeWhile(LongPredicate)},
     * or a short-circuiting terminal operation to prevent infinite loops.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random long values
     * Random random = new Random();
     * LongStream.generate(random::nextLong)
     *     .limit(10)
     *     .toArray();
     *
     * // Generate constant value stream
     * LongStream.generate(() -> 42L)
     *     .limit(5)
     *     .toArray();   // returns [42L, 42L, 42L, 42L, 42L]
     *
     * // Generate timestamps
     * LongStream.generate(System::currentTimeMillis)
     *     .limit(3)
     *     .forEach(System.out::println);   // prints current timestamps
     *
     * // Generate random positive longs in range
     * LongStream.generate(() -> Math.abs(random.nextLong()) % 100)
     *     .limit(20)
     *     .toArray();   // 20 random values between 0 and 99
     *
     * // Generate with external state (counter)
     * AtomicLong counter = new AtomicLong(0);
     * LongStream.generate(counter::getAndIncrement)
     *     .limit(10)
     *     .toArray();   // returns [0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * // Generate values from a collection cyclically
     * long[] values = {10L, 20L, 30L};
     * AtomicInteger index = new AtomicInteger(0);
     * LongStream.generate(() -> values[index.getAndIncrement() % values.length])
     *     .limit(8)
     *     .toArray();   // returns [10L, 20L, 30L, 10L, 20L, 30L, 10L, 20L]
     *
     * // Generate Gaussian distributed values
     * LongStream.generate(() -> (long) (random.nextGaussian() * 100))
     *     .limit(1000)
     *     .toArray();
     * }</pre>
     *
     * <p><b>Warning:</b> Never consume this stream without a limiting operation:
     * <pre>{@code
     * // DON'T DO THIS - will run forever!
     * // LongStream.generate(() -> 42L).forEach(System.out::println);
     *
     * // DO THIS instead:
     * LongStream.generate(() -> 42L).limit(100).forEach(System.out::println);
     * }</pre>
     *
     * <p><b>Comparison with alternatives:</b>
     * <pre>{@code
     * // Generate (for independent values or external state)
     * LongStream.generate(random::nextLong).limit(10);
     *
     * // Iterate (when each value depends on previous)
     * LongStream.iterate(1L, n -> n * 2).limit(10);
     *
     * // Random (built-in random generator)
     * LongStream.random().limit(10);
     *
     * // Repeat (for constant values)
     * LongStream.repeat(42L, 10);
     * }</pre>
     *
     * <p><b>Thread Safety:</b> If the supplier accesses mutable state, ensure proper synchronization
     * for thread safety, especially when using parallel streams.
     *
     * @param s the LongSupplier that provides the elements of the stream; invoked for each element
     * @return an infinite LongStream where each element is generated by invoking the supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see #iterate(long, LongUnaryOperator)
     * @see #random()
     * @see #repeat(long, long)
     * @see java.util.stream.LongStream#generate(java.util.function.LongSupplier)
     */
    public static LongStream generate(final LongSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                return s.getAsLong();
            }
        });
    }

    /**
     * Concatenates multiple arrays of longs into a single LongStream.
     * This is a static factory method that creates a sequential stream by concatenating
     * all provided arrays in the order they are specified.
     *
     * <p>The resulting stream contains all elements from the first array, followed by all elements
     * from the second array, and so on. Empty or null arrays are handled gracefully. If all arrays
     * are empty or the varargs parameter is empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple arrays
     * long[] arr1 = {1L, 2L, 3L};
     * long[] arr2 = {4L, 5L, 6L};
     * long[] arr3 = {7L, 8L, 9L};
     * LongStream.concat(arr1, arr2, arr3)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * // Concatenate with empty arrays
     * LongStream.concat(new long[]{1L, 2L}, new long[]{}, new long[]{3L})
     *     .toArray();   // returns [1L, 2L, 3L]
     *
     * // Combine header, body, and footer
     * long[] header = {0L};
     * long[] data = {10L, 20L, 30L};
     * long[] footer = {-1L};
     * LongStream.concat(header, data, footer)
     *     .sum();   // returns 59L
     *
     * // Single array
     * LongStream.concat(new long[]{1L, 2L, 3L})
     *     .count();   // returns 3
     *
     * // Concatenate generated arrays
     * long[] evens = {2L, 4L, 6L};
     * long[] odds = {1L, 3L, 5L};
     * LongStream.concat(evens, odds)
     *     .sorted()
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     * }</pre>
     *
     * @param a the arrays of longs to concatenate; may be empty or contain empty arrays
     * @return a LongStream containing all the longs from the input arrays in order, or an empty stream if no arrays provided
     * @see #concat(LongStream...)
     * @see #concat(List)
     */
    public static LongStream concat(final long[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple LongIterators into a single LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter1 = LongIterator.of(1L, 2L, 3L);
     * LongIterator iter2 = LongIterator.of(4L, 5L);
     * LongStream.concat(iter1, iter2)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Null iterator treated as empty
     * LongStream.concat((LongIterator) null, LongIterator.of(10L, 20L))
     *     .toArray();   // returns [10L, 20L]
     *
     * // Single iterator
     * LongStream.concat(LongIterator.of(7L))
     *     .toArray();   // returns [7L]
     * }</pre>
     *
     * @param a the LongIterators to concatenate
     * @return a LongStream containing all the longs from the input iterators in order
     * @see Stream#concat(Iterator[])
     */
    public static LongStream concat(final LongIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple LongStreams into a single LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream stream1 = LongStream.of(1L, 2L, 3L);
     * LongStream stream2 = LongStream.of(4L, 5L);
     * LongStream.concat(stream1, stream2)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Single stream
     * LongStream.concat(LongStream.of(10L, 20L))
     *     .toArray();   // returns [10L, 20L]
     *
     * // Empty stream in concatenation
     * LongStream.concat(LongStream.empty(), LongStream.of(5L))
     *     .toArray();   // returns [5L]
     * }</pre>
     *
     * @param a the LongStreams to concatenate
     * @return a LongStream containing all the longs from the input streams in order
     * @see Stream#concat(Stream[])
     */
    public static LongStream concat(final LongStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of long arrays into a single LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<long[]> arrays = Arrays.asList(new long[]{1L, 2L}, new long[]{3L, 4L, 5L});
     * LongStream.concat(arrays)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Empty list returns empty stream
     * LongStream.concat(Collections.emptyList())
     *     .count();   // returns 0
     *
     * // List with empty arrays
     * List<long[]> withEmpty = Arrays.asList(new long[]{1L}, new long[0], new long[]{2L});
     * LongStream.concat(withEmpty)
     *     .toArray();   // returns [1L, 2L]
     * }</pre>
     *
     * @param c the list of long arrays to concatenate
     * @return a LongStream containing all the longs from the input arrays in order
     * @see #concat(long[]...)
     */
    @Beta
    public static LongStream concat(final List<long[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new LongIteratorEx() {
            private final Iterator<long[]> iter = c.iterator();
            private long[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public long nextLong() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of LongStreams into a single LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<LongStream> streams = Arrays.asList(
     *     LongStream.of(1L, 2L),
     *     LongStream.of(3L, 4L),
     *     LongStream.of(5L)
     * );
     * LongStream.concat(streams)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L]
     *
     * // Empty collection returns empty stream
     * LongStream.concat(Collections.emptyList())
     *     .count();   // returns 0
     *
     * // Collection with empty stream
     * List<LongStream> withEmpty = Arrays.asList(LongStream.empty(), LongStream.of(10L));
     * LongStream.concat(withEmpty)
     *     .toArray();   // returns [10L]
     * }</pre>
     *
     * @param streams the collection of LongStreams to concatenate
     * @return a LongStream containing all the longs from the input streams in order
     * @see Stream#concat(Collection)
     */
    public static LongStream concat(final Collection<? extends LongStream> streams) {
        return N.isEmpty(streams) ? empty() : new IteratorLongStream(new LongIteratorEx() { //NOSONAR
            private final Iterator<? extends LongStream> iterators = streams.iterator();
            private LongStream cur;
            private LongIterator iter;

            @Override
            public boolean hasNext() {
                while ((iter == null || !iter.hasNext()) && iterators.hasNext()) {
                    if (cur != null) {
                        cur.close();
                    }

                    cur = iterators.next();
                    iter = cur == null ? null : cur.iteratorEx();
                }

                return iter != null && iter.hasNext();
            }

            @Override
            public long nextLong() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextLong();
            }
        }).onClose(newCloseHandler(streams));
    }

    /**
     * Concatenates a collection of LongIterators into a single LongStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<LongIterator> iters = Arrays.asList(
     *     LongIterator.of(1L, 2L),
     *     LongIterator.of(3L, 4L)
     * );
     * LongStream.concatIterators(iters)
     *     .toArray();   // returns [1L, 2L, 3L, 4L]
     *
     * // Empty collection returns empty stream
     * LongStream.concatIterators(Collections.emptyList())
     *     .count();   // returns 0
     *
     * // Collection with null iterator
     * List<LongIterator> withNull = Arrays.asList(null, LongIterator.of(5L));
     * LongStream.concatIterators(withNull)
     *     .toArray();   // returns [5L]
     * }</pre>
     *
     * @param longIterators the collection of LongIterators to concatenate
     * @return a LongStream containing all the longs from the input iterators in order
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static LongStream concatIterators(final Collection<? extends LongIterator> longIterators) {
        if (N.isEmpty(longIterators)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final Iterator<? extends LongIterator> iter = longIterators.iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
            }
        });
    }

    /**
     * Zips two long arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two long arrays using a zip function.
     *
     * <p>The operation stops when either array runs out of values. No default values are used
     * for the shorter array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a = {1L, 2L, 3L};
     * long[] b = {4L, 5L};
     * LongStream.zip(a, b, (x, y) -> x + y)
     *       .toLongList();   // returns [5L, 7L]
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final long[] a, final long[] b, final LongBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int idx = cursor++;
                return zipFunction.applyAsLong(a[idx], b[idx]);
            }
        });
    }

    /**
     * Zips three long arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three long arrays using a zip function.
     *
     * <p>The operation stops when any array runs out of values. No default values are used
     * for the shorter arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a = {1L, 2L, 3L};
     * long[] b = {4L, 5L};
     * long[] c = {7L, 8L};
     * LongStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *       .toLongList();   // returns [12L, 15L]
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param c the third long array
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final long[] a, final long[] b, final long[] c, final LongTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int idx = cursor++;
                return zipFunction.applyAsLong(a[idx], b[idx], c[idx]);
            }
        });
    }

    /**
     * Zips two long iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two long iterators using a zip function.
     *
     * <p>The operation stops when either iterator runs out of values. No default values are used
     * for the shorter iterator. Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 2L, 3L);
     * LongIterator b = LongIterator.of(4L, 5L);
     * LongStream.zip(a, b, (x, y) -> x * y)
     *       .toLongList();   // returns [4L, 10L]
     * }</pre>
     *
     * @param a the first long iterator. Can be {@code null} (treated as empty)
     * @param b the second long iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongBinaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            private final LongIterator iterA = a == null ? LongIterator.empty() : a;
            private final LongIterator iterB = b == null ? LongIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public long nextLong() {
                return zipFunction.applyAsLong(iterA.nextLong(), iterB.nextLong());
            }
        });
    }

    /**
     * Zips three long iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three long iterators using a zip function.
     *
     * <p>The operation stops when any iterator runs out of values. No default values are used
     * for the shorter iterators. Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 2L, 3L);
     * LongIterator b = LongIterator.of(4L, 5L);
     * LongIterator c = LongIterator.of(7L, 8L);
     * LongStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *       .toLongList();   // returns [12L, 15L]
     * }</pre>
     *
     * @param a the first long iterator. Can be {@code null} (treated as empty)
     * @param b the second long iterator. Can be {@code null} (treated as empty)
     * @param c the third long iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongIterator c, final LongTernaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            private final LongIterator iterA = a == null ? LongIterator.empty() : a;
            private final LongIterator iterB = b == null ? LongIterator.empty() : b;
            private final LongIterator iterC = c == null ? LongIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public long nextLong() {
                return zipFunction.applyAsLong(iterA.nextLong(), iterB.nextLong(), iterC.nextLong());
            }
        });
    }

    /**
     * Zips two long streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two long streams using a zip function.
     *
     * <p>The operation stops when either stream runs out of values. No default values are used
     * for the shorter stream. The resulting stream will automatically close both input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream a = LongStream.of(1L, 2L, 3L);
     * LongStream b = LongStream.of(10L, 20L);
     * LongStream.zip(a, b, (x, y) -> x + y)
     *       .toLongList();   // returns [11L, 22L]
     * }</pre>
     *
     * @param a the first LongStream
     * @param b the second LongStream
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three long streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three long streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream a = LongStream.of(1L, 2L, 3L);
     * LongStream b = LongStream.of(4L, 5L);
     * LongStream c = LongStream.of(7L, 8L);
     * LongStream.zip(a, b, c, (x, y, z) -> x * y * z)
     *       .toLongList();   // returns [28L, 80L]
     * }</pre>
     *
     * @param a the first LongStream
     * @param b the second LongStream
     * @param c the third LongStream
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongStream c, final LongTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple long streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from a collection of long streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The zip function receives an array containing one element from each stream.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<LongStream> streams = Arrays.asList(
     *     LongStream.of(1L, 2L, 3L),
     *     LongStream.of(4L, 5L),
     *     LongStream.of(7L, 8L)
     * );
     * LongStream.zip(streams, values -> LongStream.of(values).sum())
     *       .toLongList();   // returns [12L, 15L]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Long} which is then unboxed back to {@code long}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link LongBinaryOperator} /
     * {@link LongTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of long streams to zip
     * @param zipFunction the function to combine arrays of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is empty
     */
    public static LongStream zip(final Collection<? extends LongStream> streams, final LongNFunction<Long> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToLong(ToLongFunction.UNBOX);
    }

    /**
     * Zips two long arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two long arrays using a zip function.
     *
     * <p>The operation continues until both arrays are exhausted. When one array runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a = {1L, 2L, 3L};
     * long[] b = {4L, 5L};
     * LongStream.zip(a, b, 0L, 10L, (x, y) -> x + y)
     *       .toLongList();   // returns [5L, 7L, 13L]
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final long[] a, final long[] b, final long valueForNoneA, final long valueForNoneB, final LongBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private long ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsLong(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three long arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three long arrays using a zip function.
     *
     * <p>The operation continues until all arrays are exhausted. When one array runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a = {1L, 2L, 3L};
     * long[] b = {4L, 5L};
     * long[] c = {7L};
     * LongStream.zip(a, b, c, 0L, 10L, 20L, (x, y, z) -> x + y + z)
     *       .toLongList();   // returns [12L, 27L, 33L]
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param c the third long array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param valueForNoneC the default value to use when the third array runs out of values
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final long[] a, final long[] b, final long[] c, final long valueForNoneA, final long valueForNoneB, final long valueForNoneC,
            final LongTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private long ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsLong(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two long iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two long iterators using a zip function.
     *
     * <p>The operation continues until both iterators are exhausted. When one iterator runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that iterator.
     * Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 2L, 3L);
     * LongIterator b = LongIterator.of(4L, 5L);
     * LongStream.zip(a, b, 0L, 10L, (x, y) -> x + y)
     *       .toLongList();   // returns [5L, 7L, 13L]
     * }</pre>
     *
     * @param a the first long iterator, may be {@code null} (treated as empty)
     * @param b the second long iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final long valueForNoneA, final long valueForNoneB,
            final LongBinaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            private final LongIterator iterA = a == null ? LongIterator.empty() : a;
            private final LongIterator iterB = b == null ? LongIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public long nextLong() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsLong(iterA.nextLong(), iterB.hasNext() ? iterB.nextLong() : valueForNoneB);
                } else {
                    return zipFunction.applyAsLong(valueForNoneA, iterB.nextLong());
                }
            }
        });
    }

    /**
     * Zips three long iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three long iterators using a zip function.
     *
     * <p>The operation continues until all iterators are exhausted. When one iterator runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that iterator.
     * Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 2L, 3L);
     * LongIterator b = LongIterator.of(4L, 5L);
     * LongIterator c = LongIterator.of(7L);
     * LongStream.zip(a, b, c, 0L, 10L, 20L, (x, y, z) -> x + y + z)
     *       .toLongList();   // returns [12L, 27L, 33L]
     * }</pre>
     *
     * @param a the first long iterator, may be {@code null} (treated as empty)
     * @param b the second long iterator, may be {@code null} (treated as empty)
     * @param c the third long iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param valueForNoneC the default value to use when the third iterator runs out of values
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongIterator c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTernaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            private final LongIterator iterA = a == null ? LongIterator.empty() : a;
            private final LongIterator iterB = b == null ? LongIterator.empty() : b;
            private final LongIterator iterC = c == null ? LongIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public long nextLong() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsLong(iterA.nextLong(), iterB.hasNext() ? iterB.nextLong() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextLong() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsLong(valueForNoneA, iterB.nextLong(), iterC.hasNext() ? iterC.nextLong() : valueForNoneC);
                } else {
                    return zipFunction.applyAsLong(valueForNoneA, valueForNoneB, iterC.nextLong());
                }
            }
        });
    }

    /**
     * Zips two long streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two long streams using a zip function.
     *
     * <p>The operation continues until both streams are exhausted. When one stream runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that stream.
     * The returned stream will handle closing of the input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream a = LongStream.of(1L, 2L, 3L);
     * LongStream b = LongStream.of(4L, 5L);
     * LongStream.zip(a, b, 0L, 10L, (x, y) -> x + y)
     *       .toLongList();   // returns [5L, 7L, 13L]
     * }</pre>
     *
     * @param a the first long stream
     * @param b the second long stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close the input streams when closed
     */
    public static LongStream zip(final LongStream a, final LongStream b, final long valueForNoneA, final long valueForNoneB,
            final LongBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three long streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three long streams using a zip function.
     *
     * <p>The operation continues until all streams are exhausted. When one stream runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that stream.
     * The returned stream will handle closing of the input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream a = LongStream.of(1L, 2L, 3L);
     * LongStream b = LongStream.of(4L, 5L);
     * LongStream c = LongStream.of(7L);
     * LongStream.zip(a, b, c, 0L, 10L, 20L, (x, y, z) -> x + y + z)
     *       .toLongList();   // returns [12L, 27L, 33L]
     * }</pre>
     *
     * @param a the first long stream
     * @param b the second long stream
     * @param c the third long stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param valueForNoneC the default value to use when the third stream runs out of values
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close the input streams when closed
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple long streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from a collection of long streams using a zip function.
     *
     * <p>The operation continues until all streams are exhausted. When one stream runs out of values,
     * the corresponding default value from valuesForNone is used for that stream.
     * The zip function receives an array containing one element from each stream.
     * The returned stream will handle closing of all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<LongStream> streams = Arrays.asList(
     *     LongStream.of(1L, 2L, 3L),
     *     LongStream.of(4L, 5L),
     *     LongStream.of(7L)
     * );
     * long[] defaults = {0L, 10L, 20L};
     * LongStream.zip(streams, defaults, values -> LongStream.of(values).sum())
     *       .toLongList();   // returns [12L, 27L, 33L]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Long} which is then unboxed back to {@code long}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link LongBinaryOperator} /
     * {@link LongTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of long streams to zip
     * @param valuesForNone array of default values, must have same size as streams collection
     * @param zipFunction the function to combine arrays of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close all input streams when closed
     * @throws IllegalArgumentException if the size of valuesForNone doesn't match the size of streams collection
     */
    public static LongStream zip(final Collection<? extends LongStream> streams, final long[] valuesForNone, final LongNFunction<Long> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToLong(ToLongFunction.UNBOX);
    }

    /**
     * Merges two long arrays into a single LongStream based on the provided nextSelector function.
     * The nextSelector determines which element to take next from the two arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted arrays maintaining sort order
     * long[] a = {1L, 3L, 5L};
     * long[] b = {2L, 4L, 6L};
     * LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     *
     * // Always take from first array
     * LongStream.merge(new long[]{10L, 20L}, new long[]{15L, 25L},
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [10L, 20L, 15L, 25L]
     *
     * // Empty first array
     * LongStream.merge(new long[]{}, new long[]{5L, 10L},
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [5L, 10L]
     *
     * // Empty second array
     * LongStream.merge(new long[]{5L, 10L}, new long[]{},
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [5L, 10L]
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param nextSelector a function to determine which element should be selected next.
     *                     Returns MergeResult.TAKE_FIRST to select from the first array, otherwise from the second
     * @return a LongStream containing the merged elements from the two input arrays
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static LongStream merge(final long[] a, final long[] b, final LongBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public long nextLong() {
                if (cursorA < lenA) {
                    if ((cursorB >= lenB) || (nextSelector.apply(a[cursorA], b[cursorB]) == MergeResult.TAKE_FIRST)) {
                        return a[cursorA++];
                    } else {
                        return b[cursorB++];
                    }
                } else if (cursorB < lenB) {
                    return b[cursorB++];
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three long arrays into a single LongStream based on the provided nextSelector function.
     * The arrays are first merged pairwise, then the result is merged with the third array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a = {1L, 5L, 9L};
     * long[] b = {2L, 6L, 10L};
     * long[] c = {3L, 7L, 11L};
     * LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns sorted merge [1L, 2L, 3L, 5L, 6L, 7L, 9L, 10L, 11L]
     *
     * // One empty array
     * LongStream.merge(new long[]{1L}, new long[]{}, new long[]{2L},
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .count();   // returns 3
     * }</pre>
     *
     * @param a the first long array
     * @param b the second long array
     * @param c the third long array
     * @param nextSelector a function to determine which element should be selected next.
     *                     Returns MergeResult.TAKE_FIRST to select from the first array, otherwise from the second
     * @return a LongStream containing the merged elements from the three input arrays
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static LongStream merge(final long[] a, final long[] b, final long[] c, final LongBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), LongStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two LongIterators into a single LongStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 3L, 5L);
     * LongIterator b = LongIterator.of(2L, 4L, 6L);
     * LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     *
     * // Null iterator treated as empty
     * LongStream.merge(null, LongIterator.of(10L, 20L),
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [10L, 20L]
     * }</pre>
     *
     * @param a the first LongIterator
     * @param b the second LongIterator
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a LongStream containing the merged elements from the two input iterators
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static LongStream merge(final LongIterator a, final LongIterator b, final LongBiFunction<MergeResult> nextSelector) {
        return new IteratorLongStream(new LongIteratorEx() {
            private final LongIterator iterA = a == null ? LongIterator.empty() : a;
            private final LongIterator iterB = b == null ? LongIterator.empty() : b;
            private long nextA = 0;
            private long nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public long nextLong() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextLong())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextLong()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextLong()), (nextB = iterB.nextLong())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextLong();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextLong();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three LongIterators into a single LongStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator a = LongIterator.of(1L, 4L);
     * LongIterator b = LongIterator.of(2L, 5L);
     * LongIterator c = LongIterator.of(3L, 6L);
     * LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     * }</pre>
     *
     * @param a the first LongIterator
     * @param b the second LongIterator
     * @param c the third LongIterator
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a LongStream containing the merged elements from the three input iterators
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static LongStream merge(final LongIterator a, final LongIterator b, final LongIterator c, final LongBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two LongStreams into a single LongStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream s1 = LongStream.of(1L, 3L, 5L);
     * LongStream s2 = LongStream.of(2L, 4L, 6L);
     * LongStream.merge(s1, s2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     *
     * // Always prefer second stream
     * LongStream.merge(LongStream.of(10L), LongStream.of(20L),
     *         (x, y) -> MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [20L, 10L]
     * }</pre>
     *
     * @param a the first LongStream
     * @param b the second LongStream
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a LongStream containing the merged elements from the two input streams
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static LongStream merge(final LongStream a, final LongStream b, final LongBiFunction<MergeResult> nextSelector) {
        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three LongStreams into a single LongStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongStream s1 = LongStream.of(1L, 5L, 9L);
     * LongStream s2 = LongStream.of(2L, 6L, 10L);
     * LongStream s3 = LongStream.of(3L, 7L, 11L);
     * LongStream.merge(s1, s2, s3, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 5L, 6L, 7L, 9L, 10L, 11L]
     * }</pre>
     *
     * @param a the first LongStream
     * @param b the second LongStream
     * @param c the third LongStream
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a LongStream containing the merged elements from the three input streams
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static LongStream merge(final LongStream a, final LongStream b, final LongStream c, final LongBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of LongStream into a single LongStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the multiple streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<LongStream> streams = Arrays.asList(
     *     LongStream.of(1L, 4L, 7L),
     *     LongStream.of(2L, 5L, 8L),
     *     LongStream.of(3L, 6L, 9L)
     * );
     * LongStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * // Empty collection returns empty stream
     * LongStream.merge(Collections.emptyList(),
     *         (x, y) -> MergeResult.TAKE_FIRST)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param streams the collection of LongStream instances to merge
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a LongStream containing the merged elements from the input LongStreams
     * @see Stream#merge(Collection, BiFunction)
     */
    public static LongStream merge(final Collection<? extends LongStream> streams, final LongBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            return streams.iterator().next();
        } else if (streams.size() == 2) {
            final Iterator<? extends LongStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends LongStream> iter = streams.iterator();
        LongStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Extended LongStream class for providing additional functionality.
     * This is an abstract base class for extended long stream operations.
     */
    public abstract static class LongStreamEx extends LongStream {
        private LongStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
