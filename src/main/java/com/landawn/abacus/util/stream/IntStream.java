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

import java.nio.IntBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
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
import com.landawn.abacus.util.Fn.FI;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntMapMultiConsumer;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.IntTriPredicate;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ToIntFunction;

/**
 * A specialized stream implementation for processing sequences of integer values with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive integer-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for int data types.
 *
 * <p>IntStream represents a sequence of integer elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with integer values,
 * avoiding boxing/unboxing overhead and offering integer-specific utility methods for numerical computations.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for integer operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for int primitives, avoiding boxing overhead</li>
 *   <li><b>Numerical Operations:</b> Specialized methods for mathematical computations and statistical analysis</li>
 *   <li><b>Range Generation:</b> Built-in support for generating integer ranges and sequences</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large integer datasets</li>
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
 *   <li><b>Data Processing:</b> Processing large datasets of integer numbers</li>
 *   <li><b>Range Operations:</b> Generating sequences, loops, and iterative computations</li>
 *   <li><b>Array Processing:</b> Efficient processing of integer arrays and collections</li>
 *   <li><b>Index Operations:</b> Working with array indices and position-based computations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic integer stream operations
 * IntStream.of(1, 2, 3, 4, 5)
 *     .filter(i -> i > 2)   // keeps values > 2
 *     .map(i -> i * 2)      // doubles each value
 *     .sum();               // sum is 24
 *
 * // Range-based operations
 * IntStream.range(1, 101)        // creates numbers 1 to 100
 *     .filter(i -> i % 2 == 0)   // keeps even numbers only
 *     .limit(10)                 // keeps first 10 even numbers
 *     .toArray();                // [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
 *
 * // Statistical operations
 * IntSummaryStatistics stats = IntStream.of(scores)
 *     .filter(score -> score >= 0)   // filters valid scores
 *     .summaryStatistics();          // gets min, max, avg, count
 *
 * // Parallel processing for large datasets
 * int result = IntStream.range(1, 1_000_000)
 *     .parallel()              // uses parallel processing
 *     .filter(this::isPrime)   // filters prime numbers
 *     .map(i -> i * i)         // maps each prime to its square
 *     .sum();                  // sums the squares
 *
 * // Integration with other stream types
 * DoubleStream averages = IntStream.of(data)
 *     .mapToDouble(i -> i / 100.0)   // maps to percentages
 *     .filter(d -> d > 0.5);         // keeps values > 50%
 * }</pre>
 *
 * <p><b>Integer-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(IntPredicate)} - Filter integers based on conditions</li>
 *   <li>{@code map(IntUnaryOperator)} - Transform integer values</li>
 *   <li>{@code reduce(IntBinaryOperator)} - Reduce to single integer value</li>
 *   <li>{@code sum()}, {@code average()}, {@code min()}, {@code max()} - Mathematical aggregations</li>
 *   <li>{@code range()}, {@code rangeClosed()} - Generate integer sequences</li>
 *   <li>{@code mapToLong()}, {@code mapToDouble()}, {@code mapToObj()} - Convert to other stream types</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Integer&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use IntStream instead of {@code Stream<Integer>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 elements)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * <p><b>abacus {@code IntStream} vs. {@link java.util.stream.IntStream JDK IntStream} &mdash; how they differ:</b>
 * This {@code IntStream} mirrors the JDK's {@link java.util.stream.IntStream} (the same lazy, single-use,
 * primitive-{@code int} pipeline) but adds a much larger operation set and a few behavioral differences,
 * paralleling the relationship between the abacus and JDK object {@link Stream}s:
 *
 * <table border="1">
 *   <caption>abacus {@code IntStream} compared with {@code java.util.stream.IntStream}</caption>
 *   <tr><th>Aspect</th><th>abacus {@code IntStream}</th><th>JDK {@code java.util.stream.IntStream}</th></tr>
 *   <tr>
 *     <td>Operation set</td>
 *     <td>Much larger &mdash; e.g. {@code takeWhile}/{@code dropWhile} (independent of the JDK version),
 *         {@code collapse}, {@code scan}, {@code step}, {@code zipWith}/{@code mergeWith}, plus convenient
 *         terminals and {@link IntList} conversions.</td>
 *     <td>The standard, smaller set defined by {@code java.util.stream}.</td>
 *   </tr>
 *   <tr>
 *     <td>Companion primitive streams</td>
 *     <td>One of a full family: {@link ByteStream}, {@link CharStream}, {@link ShortStream}, {@code IntStream},
 *         {@link LongStream}, {@link FloatStream}, {@link DoubleStream}.</td>
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
 *   <caption>How selected {@code IntStream} operations differ from JDK</caption>
 *   <thead>
 *     <tr><th>Operation</th><th>Behavior difference</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code min}/{@code max}/{@code reduce}/{@code findFirst}/{@code findAny}/{@code average}</td>
 *       <td><b><i>abacus</i></b>: returns {@code u.OptionalInt} (and {@code u.OptionalDouble} for {@code average()}) &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns {@code java.util.OptionalInt}/{@code OptionalDouble}</td>
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
 *       <td><b><i>abacus</i></b>: returns an extended {@code IntIterator} (and is deprecated) that does not auto-close the stream &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns a {@code java.util.PrimitiveIterator.OfInt}</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><b>Prefer the abacus {@code IntStream} when:</b> you want the expanded operation set, the additional
 * primitive stream types, automatic closing of I/O-backed pipelines, or tight interop with the abacus
 * {@link Stream} / {@link IntList} ecosystem.
 *
 * <p><b>Prefer the JDK {@link java.util.stream.IntStream} when:</b> you want to avoid an extra dependency,
 * or the surrounding code is standardized on {@code java.util.stream} and must exchange JDK stream types
 * across its API boundaries.
 *
 * <p>The two convert directly: {@link #from(java.util.stream.IntStream)} wraps a JDK {@code IntStream} and
 * {@link #toJdkStream()} returns one, while {@link #boxed()}, {@link #asLongStream()} and
 * {@link #asDoubleStream()} bridge to the object and wider primitive streams.
 *
 * @see StreamBase
 * @see LongStream
 * @see DoubleStream
 * @see FloatStream
 * @see Stream
 * @see IntIterator
 * @see IntList
 * @see IntSummaryStatistics
 * @see java.util.stream.IntStream
 * @see OptionalInt
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@com.landawn.abacus.annotation.Immutable
@LazyEvaluation
public abstract class IntStream extends StreamBase<Integer, int[], IntPredicate, IntConsumer, OptionalInt, IndexedInt, IntIterator, IntStream> {

    static final Random RAND = new SecureRandom();

    IntStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * IntStream.of(1, 2, 3, 4, 5)
     *       .filter(x -> x > 3)
     *       .toArray();   // returns [4, 5]
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine if it should be included
     * @return a new stream consisting of the elements that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract IntStream filter(final IntPredicate predicate);

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
     * IntStream.of(1, 2, 3, 4, 5, 2, 1)
     *       .takeWhile(x -> x < 4)
     *       .toIntList();   // returns [1, 2, 3]
     *
     * // Take while condition is met
     * IntStream.of(2, 4, 6, 8, 3, 10)
     *       .takeWhile(x -> x % 2 == 0)
     *       .toIntList();   // returns [2, 4, 6, 8]
     * }</pre>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop taking elements
     * @return a new stream consisting of elements from this stream until an element is encountered that doesn't match the predicate
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract IntStream takeWhile(final IntPredicate predicate);

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
     * IntStream.of(1, 2, 3, 4, 5, 2, 1)
     *       .dropWhile(x -> x < 4)
     *       .toIntList();   // returns [4, 5, 2, 1]
     *
     * // Drop while condition is met
     * IntStream.of(2, 4, 6, 8, 3, 10)
     *       .dropWhile(x -> x % 2 == 0)
     *       .toIntList();   // returns [3, 10]
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
    public abstract IntStream dropWhile(final IntPredicate predicate);

    /**
     * Returns an IntStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each int element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3, 4)
     *       .map(x -> x * 2)
     *       .toArray();   // returns [2, 4, 6, 8]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream map(IntUnaryOperator mapper);

    /**
     * Returns a CharStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a char value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert ASCII codes to characters
     * IntStream.of(65, 66, 67, 68)
     *       .mapToChar(i -> (char) i)
     *       .toCharList();   // returns ['A', 'B', 'C', 'D']
     *
     * // Map integers to digit characters
     * IntStream.range(0, 5)
     *       .mapToChar(i -> (char) ('0' + i))
     *       .toCharList();   // returns ['0', '1', '2', '3', '4']
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to char
     * @return a new CharStream consisting of the results of applying the mapper function to each element
     * @see #map(IntUnaryOperator)
     * @see #mapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream mapToChar(IntToCharFunction mapper);

    /**
     * Returns a ByteStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a byte value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to bytes (with truncation)
     * IntStream.of(1, 2, 300, 4)
     *       .mapToByte(i -> (byte) i)
     *       .toByteList();   // returns [1, 2, 44, 4] (300 truncated to 44)
     *
     * // Extract low byte from integers
     * IntStream.of(0x1234, 0x5678)
     *       .mapToByte(i -> (byte) (i & 0xFF))
     *       .toByteList();   // returns [0x34, 0x78]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to byte
     * @return a new ByteStream consisting of the results of applying the mapper function to each element
     * @see #map(IntUnaryOperator)
     * @see #mapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream mapToByte(IntToByteFunction mapper);

    /**
     * Returns a ShortStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a short value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to shorts
     * IntStream.of(100, 200, 300)
     *       .mapToShort(i -> (short) i)
     *       .toShortList();   // returns [100, 200, 300]
     *
     * // Extract lower 16 bits from integers
     * IntStream.of(0x12345678, 0xABCDEF01)
     *       .mapToShort(i -> (short) (i & 0xFFFF))
     *       .toShortList();   // returns [0x5678, 0xEF01]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to short
     * @return a new ShortStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(IntUnaryOperator)
     * @see #mapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream mapToShort(IntToShortFunction mapper);

    /**
     * Returns a LongStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a long value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to longs
     * IntStream.of(1, 2, 3, 4)
     *       .mapToLong(i -> (long) i)
     *       .toArray();   // returns [1L, 2L, 3L, 4L]
     *
     * // Square integers to long to avoid overflow
     * IntStream.of(10000, 20000, 30000)
     *       .mapToLong(i -> (long) i * i)
     *       .toArray();   // returns [100000000L, 400000000L, 900000000L]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to long
     * @return a new LongStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(IntUnaryOperator)
     * @see #mapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapToLong(IntToLongFunction mapper);

    /**
     * Returns a FloatStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a float value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to floats
     * IntStream.of(1, 2, 3, 4)
     *       .mapToFloat(i -> (float) i)
     *       .toArray();   // returns [1.0f, 2.0f, 3.0f, 4.0f]
     *
     * // Divide integers to get percentages
     * IntStream.of(25, 50, 75, 100)
     *       .mapToFloat(i -> i / 100.0f)
     *       .toArray();   // returns [0.25f, 0.5f, 0.75f, 1.0f]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to float
     * @return a new FloatStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(IntUnaryOperator)
     * @see #mapToDouble(IntToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream mapToFloat(IntToFloatFunction mapper);

    /**
     * Returns a DoubleStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to a double value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to doubles
     * IntStream.of(1, 2, 3, 4)
     *       .mapToDouble(i -> (double) i)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     *
     * // Calculate square roots
     * IntStream.of(1, 4, 9, 16)
     *       .mapToDouble(Math::sqrt)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to double
     * @return a new DoubleStream consisting of the results of applying the mapper function to each element of this stream
     * @see #map(IntUnaryOperator)
     * @see #mapToFloat(IntToFloatFunction)
     * @see java.util.stream.IntStream#mapToDouble(IntToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapToDouble(IntToDoubleFunction mapper);

    /**
     * Returns an object-valued Stream consisting of the results of applying the
     * given function to the elements of this stream. This is an intermediate operation
     * that transforms each int element to an object of type T using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to strings
     * IntStream.of(1, 2, 3, 4)
     *       .mapToObj(String::valueOf)
     *       .toList();   // returns ["1", "2", "3", "4"]
     *
     * // Create objects from integers
     * IntStream.range(0, 3)
     *       .mapToObj(i -> new Point(i, i * 2))
     *       .toList();   // returns [Point(0,0), Point(1,2), Point(2,4)]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from int to T
     * @return a new Stream of objects resulting from applying the mapper function to each element of this stream
     * @see #map(IntUnaryOperator)
     * @see java.util.stream.IntStream#mapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(IntFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each mapped stream is
     * {@link BaseStream#close() closed} after its contents have been placed
     * into this stream. (If a mapped stream is {@code null} an empty stream
     * is used, instead.)
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(IntFunction) flatMap} (this method) - transforms elements into {@link com.landawn.abacus.util.stream.IntStream IntStream} (this library's stream).</li>
     *   <li>{@link #flatMapArray(IntFunction) flatMapArray} - transforms elements into an {@code int[]} array.</li>
     *   <li>{@link #flattMap(IntFunction) flattMap} (double 't', uppercase 'M') - transforms elements into a standard {@link java.util.stream.IntStream java.util.stream.IntStream} (JDK).</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each number to a range
     * IntStream.of(1, 2, 3)
     *       .flatMap(n -> IntStream.range(0, n))
     *       .toIntList();   // returns [0, 0, 1, 0, 1, 2]
     *
     * // Duplicate each element
     * IntStream.of(1, 2, 3)
     *       .flatMap(n -> IntStream.of(n, n))
     *       .toIntList();   // returns [1, 1, 2, 2, 3, 3]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to IntStream
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     * @see Stream#flatMap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMap(IntFunction<? extends IntStream> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(IntFunction) flatMap} (uppercase 'M') - transforms each element into an {@link IntStream}.</li>
     *   <li>{@link #flatmap(IntFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Integer}.</li>
     *   <li>{@link #flatMapArray(IntFunction) flatMapArray} - transforms each element into an {@code int[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code IntStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Integer} entry is replaced with the primitive default value
     * {@code 0} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2)
     *          .flatmap(i -> Arrays.asList(i, i * 10))
     *          .toArray();   // [1, 10, 2, 20]
     *
     * // null elements in a returned collection become 0
     * IntStream.of(1)
     *          .flatmap(i -> Arrays.asList((Integer) null, 7))
     *          .toArray();   // [0, 7]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to {@code Collection<Integer>}
     * @return a new {@code IntStream} consisting of the flattened contents of the collections produced by the mapper
     * @see #flatMap(IntFunction)
     * @see #flatMapArray(IntFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to IntStream, flatmap maps to Collection<Integer>, flatMapArray maps to int[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatmap(IntFunction<? extends Collection<Integer>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of the array produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(IntFunction) flatMap} (uppercase 'M') - transforms elements into {@link com.landawn.abacus.util.stream.IntStream IntStream} (this library's stream).</li>
     *   <li>{@link #flatMapArray(IntFunction) flatMapArray} (this method) - transforms elements into an {@code int[]} array.</li>
     *   <li>{@link #flattMap(IntFunction) flattMap} (double 't', uppercase 'M') - transforms elements into a standard {@link java.util.stream.IntStream java.util.stream.IntStream} (JDK).</li>
     * </ul>
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each number to array of multiples
     * IntStream.of(2, 3)
     *       .flatMapArray(n -> new int[] {n, n * 10, n * 100})
     *       .toIntList();   // returns [2, 20, 200, 3, 30, 300]
     *
     * // Generate sequences based on each number
     * IntStream.of(1, 2, 3)
     *       .flatMapArray(n -> new int[] {n, n + 10})
     *       .toIntList();   // returns [1, 11, 2, 12, 3, 13]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element from int to int[]
     * @return a new {@code IntStream} consisting of the flattened contents of the arrays produced by the mapper
     * @see #flatMap(IntFunction)
     * @see #flattMap(IntFunction)
     * @see #flatMapToLong(IntFunction)
     * @see #flatMapToObj(IntFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to IntStream, flatMapArray maps to int[], flattMap maps to JDK java.util.stream.IntStream. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapArray(IntFunction<int[]> mapper); //NOSONAR

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
     * @deprecated This method is not supported. Use {@link #flatmapToObj(IntFunction)} instead
     * @see #flatmapToObj(IntFunction)
     */
    @Deprecated
    @ParallelSupported
    @IntermediateOp
    IntStream flattmap(@SuppressWarnings("unused") final IntFunction<? extends Collection<Integer>> mapper) throws UnsupportedOperationException { // NOSONAR
        throw new UnsupportedOperationException("Method 'flattmap' is deprecated and unsupported; use 'flatmapToObj' or 'flatMap' instead");
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. The mapped streams are
     * standard JDK IntStreams.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(IntFunction) flatMap} (uppercase 'M') - transforms elements into {@link com.landawn.abacus.util.stream.IntStream IntStream} (this library's stream).</li>
     *   <li>{@link #flatMapArray(IntFunction) flatMapArray} - transforms elements into an {@code int[]} array.</li>
     *   <li>{@link #flattMap(IntFunction) flattMap} (this method) - transforms elements into a standard {@link java.util.stream.IntStream java.util.stream.IntStream} (JDK).</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Interoperate with JDK IntStream
     * IntStream.of(1, 2, 3)
     *       .flattMap(n -> java.util.stream.IntStream.range(0, n))
     *       .toIntList();   // returns [0, 0, 1, 0, 1, 2]
     *
     * // Use JDK stream operations
     * IntStream.of(5, 10)
     *       .flattMap(n -> java.util.stream.IntStream.of(n, n * 2))
     *       .toIntList();   // returns [5, 10, 10, 20]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a JDK IntStream
     * @return a new {@code IntStream} consisting of the flattened contents of the mapped JDK {@code IntStream} instances
     * @see #flatMap(IntFunction)
     * @see #flatMapArray(IntFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to IntStream, flatMapArray maps to int[], flattMap maps to JDK java.util.stream.IntStream. Do not suggest renaming.
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flattMap(IntFunction<? extends java.util.stream.IntStream> mapper); //NOSONAR

    /**
     * Alias for {@link #flattMap(IntFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3)
     *       .flatMapJdkStream(n -> java.util.stream.IntStream.range(0, n))
     *       .toIntList();   // returns [0, 0, 1, 0, 1, 2]
     *
     * IntStream.of(5, 10)
     *       .flatMapJdkStream(n -> java.util.stream.IntStream.of(n, n * 2))
     *       .toIntList();   // returns [5, 10, 10, 20]
     *
     * IntStream.empty()
     *       .flatMapJdkStream(n -> java.util.stream.IntStream.of(n))
     *       .toIntList();   // returns []
     * }</pre>
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped JDK streams
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public IntStream flatMapJdkStream(IntFunction<? extends java.util.stream.IntStream> mapper) {
        return flattMap(mapper);
    }

    /**
     * Returns a CharStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple chars
     * IntStream.of(65, 97)
     *       .flatMapToChar(i -> CharStream.of((char) i, (char) (i + 1)))
     *       .toCharList();   // returns ['A', 'B', 'a', 'b']
     *
     * // Generate char sequences
     * IntStream.of(3, 2)
     *       .flatMapToChar(n -> CharStream.repeat('*', n))
     *       .toCharList();   // returns ['*', '*', '*', '*', '*']
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a CharStream
     * @return a new {@link CharStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream flatMapToChar(IntFunction<? extends CharStream> mapper);

    /**
     * Returns a ByteStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple bytes
     * IntStream.of(1, 2)
     *       .flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)))
     *       .toByteList();   // returns [1, 10, 2, 20]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a ByteStream
     * @return a new {@link ByteStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream flatMapToByte(IntFunction<? extends ByteStream> mapper);

    /**
     * Returns a ShortStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple shorts
     * IntStream.of(100, 200)
     *       .flatMapToShort(i -> ShortStream.of((short) i, (short) (i + 1)))
     *       .toShortList();   // returns [100, 101, 200, 201]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a ShortStream
     * @return a new {@link ShortStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream flatMapToShort(IntFunction<? extends ShortStream> mapper);

    /**
     * Returns a LongStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple longs
     * IntStream.of(1, 2)
     *       .flatMapToLong(i -> LongStream.of(i, i * 100L))
     *       .toLongList();   // returns [1L, 100L, 2L, 200L]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a LongStream
     * @return a new {@link LongStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatMapToLong(IntFunction<? extends LongStream> mapper);

    /**
     * Returns a FloatStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple floats
     * IntStream.of(1, 2)
     *       .flatMapToFloat(i -> FloatStream.of(i, i + 0.5f))
     *       .toFloatList();   // returns [1.0f, 1.5f, 2.0f, 2.5f]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a FloatStream
     * @return a new {@link FloatStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatMapToFloat(IntFunction<? extends FloatStream> mapper);

    /**
     * Returns a DoubleStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to multiple doubles
     * IntStream.of(1, 2)
     *       .flatMapToDouble(i -> DoubleStream.of(i, i + 0.5))
     *       .toDoubleList();   // returns [1.0, 1.5, 2.0, 2.5]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a DoubleStream
     * @return a new {@link DoubleStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatMapToDouble(IntFunction<? extends DoubleStream> mapper);

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
     * // Convert each int to multiple strings
     * IntStream.of(1, 2)
     *       .flatMapToObj(i -> Stream.of(String.valueOf(i), "num" + i))
     *       .toList();   // returns ["1", "num1", "2", "num2"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to a Stream
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     * @see #flatMap(IntFunction)
     * @see #flatmapToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(IntFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of a collection produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each int to collection of strings
     * IntStream.of(1, 2)
     *       .flatmapToObj(i -> Arrays.asList(String.valueOf(i), "x" + i))
     *       .toList();   // returns ["1", "x1", "2", "x2"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to a Collection
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     * @see #flatMapToObj(IntFunction)
     * @see #flatMapArrayToObj(IntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(IntFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of an array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten arrays using array mapper
     * IntStream.of(1, 2)
     *       .flatMapArrayToObj(i -> new String[] {String.valueOf(i), "x" + i})
     *       .toList();   // returns ["1", "x1", "2", "x2"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to an array
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     * @see #flatMapToObj(IntFunction)
     * @see #flatmapToObj(IntFunction)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(IntFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given
     * multi-mapping function to each element. The multi-mapping function
     * can map each element to zero, one, or multiple output elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand elements with multiple values
     * IntStream.of(1, 2, 3)
     *       .mapMulti((value, consumer) -> {
     *           consumer.accept(value);
     *           consumer.accept(value * 10);
     *       })
     *       .toIntList();   // returns [1, 10, 2, 20, 3, 30]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param mapper a non-interfering, stateless function that generates zero or more
     *               output values for each input value
     * @return a new {@link IntStream} consisting of the elements generated by the mapper
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapMulti(IntMapMultiConsumer mapper);

    /**
     * Returns a stream consisting of the elements that have a non-empty result
     * when the given mapping function is applied to them. Elements that produce
     * an empty OptionalInt are excluded from the returned stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map only when condition is met
     * IntStream.of(1, 2, 3, 4, 5)
     *       .mapPartial(value -> value % 2 == 0 ? OptionalInt.of(value * 10) : OptionalInt.empty())
     *       .toIntList();   // returns [20, 40]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to an OptionalInt
     * @return a new {@link IntStream} consisting of the present values from the non-empty {@code OptionalInt} results produced by the mapper
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapPartial(IntFunction<OptionalInt> mapper);

    /**
     * Returns a stream consisting of the elements that have a non-empty result
     * when the given mapping function is applied to them. Elements that produce
     * an empty OptionalInt are excluded from the returned stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Conditional mapping with JDK style
     * IntStream.of(1, 2, 3, 4)
     *       .mapPartialJdk(value -> value > 2 ? java.util.OptionalInt.of(value * 2) : java.util.OptionalInt.empty())
     *       .toIntList();   // returns [6, 8]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a JDK OptionalInt
     * @return a new {@link IntStream} consisting of the present values from the non-empty {@code java.util.OptionalInt} results produced by the mapper
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapPartialJdk(IntFunction<java.util.OptionalInt> mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group consecutive numbers and sum each range
     * IntStream.of(1, 2, 3, 10, 11, 20, 21)
     *       .rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last)
     *       .toIntList();   // returns [3, 6, 21, 41] (1+2, 3+3, 10+11, 20+21)
     * }</pre>
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21].
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
    public abstract IntStream rangeMap(final IntBiPredicate sameRange, final IntBinaryOperator mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map ranges to string representations
     * IntStream.of(1, 2, 3, 10, 11, 20, 21)
     *       .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
     *       .toList();   // returns ["1-2", "3-3", "10-11", "20-21"]
     * }</pre>
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21] to objects
     * using the mapper function.
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
    public abstract <T> Stream<T> rangeMapToObj(final IntBiPredicate sameRange, final IntBiFunction<? extends T> mapper);

    /**
     * Collapses consecutive elements in the stream into groups based on a predicate.
     * Elements for which the predicate returns {@code true} when applied to adjacent elements are grouped together into lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse consecutive elements
     * IntStream.of(1, 1, 2, 2, 2, 3)
     *       .collapse((a, b) -> a == b)
     *       .map(IntList::toString)
     *       .toList();   // returns ["[1, 1]", "[2, 2, 2]", "[3]"]
     * }</pre>
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * the resulting stream will contain [[1, 2], [5, 6, 7], [10]].
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @return a stream of lists, each containing a sequence of consecutive elements that are collapsible with each other
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<IntList> collapse(final IntBiPredicate collapsible);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When adjacent elements satisfy the collapsible predicate, they are merged using the provided merge function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge consecutive elements by summing
     * IntStream.of(1, 2, 5, 6, 7, 10)
     *       .collapse((a, b) -> b - a < 3, (a, b) -> a + b)
     *       .toIntList();   // returns [3, 18, 10]
     *
     * // Collapse duplicates by keeping first
     * IntStream.of(1, 1, 2, 2, 2, 3)
     *       .collapse((a, b) -> a == b, (a, b) -> a)
     *       .toIntList();   // returns [1, 2, 3]
     *
     * // Single element stream
     * IntStream.of(42)
     *       .collapse((a, b) -> true, (a, b) -> a + b)
     *       .toIntList();   // returns [42]
     *
     * // Empty stream returns empty
     * IntStream.empty()
     *       .collapse((a, b) -> true, (a, b) -> a + b)
     *       .toIntList();   // returns []
     * }</pre>
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
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
    public abstract IntStream collapse(final IntBiPredicate collapsible, final IntBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * Elements for which the predicate returns {@code true} when applied with the first and last elements of the group are merged using the provided merge function.
     * The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse using tri-predicate (first, last, next) with sum merge
     * IntStream.of(1, 2, 5, 6, 7, 10)
     *       .collapse((first, last, next) -> next - first < 3, (a, b) -> a + b)
     *       .toIntList();   // returns [3, 18, 10]
     *
     * // Empty stream returns empty
     * IntStream.empty()
     *       .collapse((first, last, next) -> true, (a, b) -> a + b)
     *       .toIntList();   // returns []
     *
     * // Single element stream
     * IntStream.of(5)
     *       .collapse((first, last, next) -> true, (a, b) -> a + b)
     *       .toIntList();   // returns [5]
     * }</pre>
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
     *
     * @param collapsible a predicate that determines if the next element from this stream should be collapsed with the first and last elements of current group
     *          The collapsible predicate takes three elements: the first and last elements of current group, and the next element to check.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream collapse(final IntTriPredicate collapsible, final IntBinaryOperator mergeFunction);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * For example, given a stream of numbers [1, 2, 3, 4], and an accumulator that performs addition,
     * the output would be a stream of numbers [1, 3, 6, 10].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running sum
     * IntStream.of(1, 2, 3, 4)
     *       .scan((a, b) -> a + b)
     *       .toIntList();   // returns [1, 3, 6, 10]
     *
     * // Running product
     * IntStream.of(2, 3, 4)
     *       .scan((a, b) -> a * b)
     *       .toIntList();   // returns [2, 6, 24]
     * }</pre>
     *
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * @param accumulator an {@code IntBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code IntStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream scan(final IntBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * For example, given a stream of numbers [1, 2, 3, 4], an initial value of 10, and an accumulator that performs addition,
     * the output would be a stream of numbers [11, 13, 16, 20].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running sum with initial value
     * IntStream.of(1, 2, 3, 4)
     *       .scan(10, (a, b) -> a + b)
     *       .toIntList();   // returns [11, 13, 16, 20]
     *
     * // Running product with initial value
     * IntStream.of(2, 3, 4)
     *       .scan(1, (a, b) -> a * b)
     *       .toIntList();   // returns [2, 6, 24]
     * }</pre>
     *
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     *        It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * @param accumulator an {@code IntBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code IntStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream scan(final int init, final IntBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running sum with initial value included
     * IntStream.of(1, 2, 3)
     *       .scan(10, true, (a, b) -> a + b)
     *       .toIntList();   // returns [10, 11, 13, 16]
     *
     * // Running sum with initial value not included
     * IntStream.of(1, 2, 3)
     *       .scan(10, false, (a, b) -> a + b)
     *       .toIntList();   // returns [11, 13, 16]
     * }</pre>
     *
     * This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     * @param initIncluded a boolean value that determines if the initial value should be included as the first element in the returned stream.
     * @param accumulator an {@code IntBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code IntStream} consisting of the results of the scan operation on the elements of the original stream.
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream scan(final int init, final boolean initIncluded, final IntBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Add elements at the beginning
     * IntStream.of(3, 4, 5)
     *       .prepend(1, 2)
     *       .toIntList();   // returns [1, 2, 3, 4, 5]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param a the elements to prepend to this stream
     * @return a new stream with the specified elements prepended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream prepend(final int... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Add elements at the end
     * IntStream.of(1, 2, 3)
     *       .append(4, 5)
     *       .toIntList();   // returns [1, 2, 3, 4, 5]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param a the elements to append to this stream
     * @return a new stream with the specified elements appended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream append(final int... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Append only if stream is empty
     * IntStream.empty()
     *       .appendIfEmpty(42, 43)
     *       .toIntList();   // returns [42, 43]
     *
     * IntStream.of(1, 2)
     *       .appendIfEmpty(42, 43)
     *       .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param a the elements to append if this stream is empty
     * @return this stream if not empty, otherwise a new stream containing the specified elements
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream appendIfEmpty(final int... a);

    /**
     * Returns an IntStream consisting of the top n elements of this stream, according to the natural order of the elements.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 largest numbers
     * IntStream.of(5, 2, 8, 1, 9, 3, 7)
     *       .top(3)
     *       .sorted()
     *       .toIntList();   // returns [7, 8, 9] (order not guaranteed without sorted)
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param n the number of elements to return
     * @return a new {@link IntStream} consisting of the top {@code n} elements; the order of the returned elements is not guaranteed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream top(int n);

    /**
     * Returns an IntStream consisting of the top n elements of this stream compared by the provided Comparator.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 smallest numbers using custom comparator
     * IntStream.of(5, 2, 8, 1, 9, 3, 7)
     *       .top(3, Comparator.reverseOrder())
     *       .sorted()
     *       .toIntList();   // returns [1, 2, 3] (order not guaranteed without sorted)
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param n the number of elements to return
     * @param comparator a non-interfering, stateless Comparator to be used to compare stream elements
     * @return a new {@link IntStream} consisting of the top {@code n} elements as determined by the comparator; the order of the returned elements is not guaranteed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream top(final int n, Comparator<? super Integer> comparator);

    /**
     * Returns an IntList containing all the elements of this stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntStream.of(1, 2, 3, 4, 5)
     *       .filter(x -> x > 2)
     *       .toIntList();   // returns IntList [3, 4, 5]
     * }</pre>
     *
     * <p>This is a terminal operation.
     *
     * @return an IntList containing all stream elements
     */
    @SequentialOnly
    @TerminalOp
    public abstract IntList toIntList();

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * The keys are produced by applying the key mapper function and values by applying the value mapper function
     * to each element. If the mapped keys contain duplicates, an IllegalStateException is thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = IntStream.of(1, 2, 3)
     *       .toMap(i -> "key" + i, i -> i * 10);
     * // returns {"key1"=10, "key2"=20, "key3"=30}
     * }</pre>
     *
     * <p>This is a terminal operation.
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a non-interfering, stateless function to apply to each element to produce keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to produce values
     * @return a Map whose keys and values are the result of applying the mapper functions to the input elements
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * The keys are produced by applying the key mapper function and values by applying the value mapper function
     * to each element. The Map is created by the provided supplier function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, Integer> orderedMap = IntStream.of(3, 1, 4, 2)
     *     .toMap(i -> "key" + i,
     *            i -> i * 10,
     *            LinkedHashMap::new);   // returns LinkedHashMap preserving order
     *
     * // Create a TreeMap for sorted keys
     * TreeMap<Integer, String> sortedMap = IntStream.of(5, 2, 8, 1)
     *     .toMap(i -> i,
     *            i -> "value" + i,
     *            TreeMap::new);   // returns TreeMap with keys sorted
     * }</pre>
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a non-interfering, stateless function to apply to each element to produce keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to produce values
     * @param mapFactory a supplier which returns a new, empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying the mapper functions to the input elements
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * The keys are produced by applying the key mapper function and values by applying the value mapper function
     * to each element. If the mapped keys contain duplicates, the values are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Handle duplicate keys by summing values
     * Map<String, Integer> map = IntStream.of(1, 2, 3, 2, 1)
     *     .toMap(i -> "key" + (i % 2),
     *            i -> i,
     *            (v1, v2) -> v1 + v2);   // returns {"key1"=5, "key0"=4}
     *
     * // Keep the first value when keys collide
     * Map<String, Integer> firstWins = IntStream.of(10, 20, 30, 40)
     *     .toMap(i -> i % 2 == 0 ? "even" : "odd",
     *            i -> i,
     *            (v1, v2) -> v1);   // returns {"even"=10}
     *
     * // Keep the maximum value when keys collide
     * Map<String, Integer> maxValues = IntStream.of(5, 3, 8, 2)
     *     .toMap(i -> i % 2 == 0 ? "even" : "odd",
     *            i -> i,
     *            Math::max);   // returns {"odd"=5, "even"=8}
     * }</pre>
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a non-interfering, stateless function to apply to each element to produce keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a Map whose keys and values are the result of applying the mapper functions to the input elements
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * The keys are produced by applying the key mapper function and values by applying the value mapper function
     * to each element. If the mapped keys contain duplicates, the values are merged using the provided merging function.
     * The Map is created by the provided supplier function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap with insertion order, merging duplicates
     * LinkedHashMap<String, Integer> orderedMap = IntStream.of(3, 1, 4, 1, 5)
     *     .toMap(i -> "key" + i,
     *            i -> i,
     *            (v1, v2) -> v1 + v2,
     *            LinkedHashMap::new);   // returns LinkedHashMap with insertion order
     *
     * // Create a TreeMap with natural ordering
     * TreeMap<Integer, String> sortedMap = IntStream.of(5, 2, 8, 2, 1)
     *     .toMap(i -> i,
     *            i -> "value" + i,
     *            (v1, v2) -> v1,
     *            TreeMap::new);   // returns TreeMap sorted by keys
     * }</pre>
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a non-interfering, stateless function to apply to each element to produce keys
     * @param valueMapper a non-interfering, stateless function to apply to each element to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier which returns a new, empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying the mapper functions to the input elements
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function and performs a reduction
     * on the values associated with each key using the specified downstream Collector.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Long> grouped = IntStream.of(1, 2, 3, 4, 5, 6)
     *       .groupTo(i -> i % 2 == 0 ? "even" : "odd", Collectors.counting());
     * // returns {"odd"=3, "even"=3}
     * }</pre>
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Note:</b> Each {@code int} element is boxed to {@link Integer} before being passed to the
     * downstream collector, since {@link Collector} cannot be parameterized on a primitive type.
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <E> the type of exception thrown by the classifier
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @return a Map containing the results of the group-by operation
     * @throws E if the classifier throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<? super Integer, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function and performs a reduction
     * on the values associated with each key using the specified downstream Collector. The Map is created
     * by the provided supplier function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group with LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, List<Integer>> grouped = IntStream.of(1, 2, 3, 4, 5, 6)
     *     .groupTo(i -> i % 2 == 0 ? "even" : "odd",
     *              Collectors.toList(),
     *              LinkedHashMap::new);
     * // returns LinkedHashMap {"odd"=[1, 3, 5], "even"=[2, 4, 6]}
     *
     * // Group with TreeMap for sorted keys
     * TreeMap<Integer, Long> countByRange = IntStream.range(0, 20)
     *     .groupTo(i -> i / 5,
     *              Collectors.counting(),
     *              TreeMap::new);
     * // returns TreeMap {0=5, 1=5, 2=5, 3=5}
     * }</pre>
     *
     * <p><b>Note:</b> Each {@code int} element is boxed to {@link Integer} before being passed to the
     * downstream collector, since {@link Collector} cannot be parameterized on a primitive type.
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception thrown by the classifier
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @param mapFactory a supplier which returns a new, empty Map into which the results will be inserted
     * @return a Map containing the results of the group-by operation
     * @throws E if the classifier throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<? super Integer, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided identity value and an
     * associative accumulation function, and returns the reduced value.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum of all elements
     * int sum = IntStream.of(1, 2, 3, 4, 5)
     *     .reduce(0, (a, b) -> a + b);   // returns 15
     *
     * // Product of all elements
     * int product = IntStream.of(2, 3, 4)
     *     .reduce(1, (a, b) -> a * b);   // returns 24
     *
     * // Maximum value
     * int max = IntStream.of(5, 2, 8, 1)
     *     .reduce(Integer.MIN_VALUE, Math::max);   // returns 8
     *
     * // Empty stream returns identity
     * int result = IntStream.empty()
     *     .reduce(10, (a, b) -> a + b);   // returns 10
     * }</pre>
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract int reduce(int identity, IntBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using an associative accumulation
     * function, and returns an OptionalInt describing the reduced value, if any.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum of elements
     * OptionalInt sum = IntStream.of(1, 2, 3, 4, 5)
     *     .reduce((a, b) -> a + b);   // returns OptionalInt[15]
     *
     * // Maximum value using method reference
     * OptionalInt max = IntStream.of(5, 2, 8, 1)
     *     .reduce(Math::max);   // returns OptionalInt[8]
     *
     * // Empty stream returns empty Optional
     * OptionalInt empty = IntStream.empty()
     *     .reduce((a, b) -> a + b);   // returns OptionalInt.empty()
     *
     * // Safe value retrieval
     * int result = IntStream.of(10, 20, 30)
     *     .reduce((a, b) -> a + b)
     *     .orElse(0);   // returns 60
     * }</pre>
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an OptionalInt describing the result of the reduction, or an empty OptionalInt if the stream is empty
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalInt reduce(IntBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using explicit supplier,
     * accumulator, and combiner functions.
     * A mutable reduction collects input elements into a mutable result container, such as a
     * Collection or Map.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into a custom container
     * List<Integer> result = IntStream.of(1, 2, 3, 4, 5)
     *     .collect(ArrayList::new,
     *              (list, i) -> list.add(i * 2),
     *              ArrayList::addAll);   // returns [2, 4, 6, 8, 10]
     *
     * // Collect into a StringBuilder
     * String joined = IntStream.of(1, 2, 3)
     *     .collect(StringBuilder::new,
     *              (sb, i) -> sb.append(i).append(','),
     *              StringBuilder::append)
     *     .toString();   // returns "1,2,3,"
     * }</pre>
     *
     * @param <R> the type of the result
     * @param supplier a function that creates a new result container. For a parallel execution,
     *                 this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an
     *                    additional element into a result
     * @param combiner an associative, non-interfering, stateless function for combining two values,
     *                 which must be compatible with the accumulator function. It's unnecessary to specify
     *                 {@code combiner} if {@code R} is a {@code Map/Collection/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     * @see BiConsumers#ofAddAll()
     * @see BiConsumers#ofPutAll()
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using explicit supplier
     * and accumulator functions, without requiring a combiner.
     * A mutable reduction collects input elements into a mutable result container, such as a
     * Collection or Map.
     *
     * <p>This is a terminal operation.
     *
     * <p>Only call this method when the returned type {@code R} is one of these types:
     * {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjIntConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into IntList (no combiner needed)
     * IntList list = IntStream.of(1, 2, 3, 4, 5)
     *     .collect(IntList::new, IntList::add);   // returns IntList[1, 2, 3, 4, 5]
     *
     * // Collect into ArrayList (no combiner needed for Collection types)
     * List<Integer> boxedList = IntStream.of(10, 20, 30)
     *     .collect(ArrayList::new, ArrayList::add);   // returns [10, 20, 30]
     * }</pre>
     *
     * @param <R> the type of the result. It must be {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @param supplier a function that creates a new result container. For a parallel execution,
     *                 this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an
     *                    additional element into a result
     * @return the result of the reduction
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of:
     *         {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @see #collect(Supplier, ObjIntConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator);

    /**
     * Performs an action for each element of this stream.
     * This is a convenience method that delegates to {@link #forEach(Throwables.IntConsumer)}.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3, 4, 5)
     *     .foreach(System.out::println);
     * // prints: 1, 2, 3, 4, 5 (each on a new line)
     * }</pre>
     *
     * @param action a non-interfering action to perform on the elements
     * @see #forEach(Throwables.IntConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public void foreach(final IntConsumer action) { // NOSONAR
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
     * IntStream.of(1, 2, 3, 4, 5)
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
    public abstract <E extends Exception> void forEach(final Throwables.IntConsumer<E> action) throws E;

    /**
     * Performs an action for each element of this stream, providing the element index to the action.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(10, 20, 30)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 10
     * // Element at index 1: 20
     * // Element at index 2: 30
     * }</pre>
     *
     * @param <E> the type of exception thrown by the action
     * @param action a non-interfering action to perform on the elements. The first parameter is the
     *               element index and the second parameter is the element value.
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntIntConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code false} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any element is greater than 10
     * boolean hasLarge = IntStream.of(5, 8, 12, 3)
     *     .anyMatch(x -> x > 10);   // returns true
     *
     * // Check if any element is negative
     * boolean hasNegative = IntStream.of(1, 2, 3, 4)
     *     .anyMatch(x -> x < 0);   // returns false
     *
     * // Empty stream always returns false
     * boolean result = IntStream.empty()
     *     .anyMatch(x -> x > 0);   // returns false
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if any elements of the stream match the provided predicate,
     *         otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean anyMatch(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all elements are positive
     * boolean allPositive = IntStream.of(1, 2, 3, 4)
     *     .allMatch(x -> x > 0);   // returns true
     *
     * // Check if all elements are even
     * boolean allEven = IntStream.of(2, 4, 6, 7)
     *     .allMatch(x -> x % 2 == 0);   // returns false
     *
     * // Empty stream always returns true
     * boolean result = IntStream.empty()
     *     .allMatch(x -> x < 0);   // returns true
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if either all elements of the stream match the provided predicate or
     *         the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean allMatch(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if no elements are negative
     * boolean noNegatives = IntStream.of(1, 2, 3, 4)
     *     .noneMatch(x -> x < 0);   // returns true
     *
     * // Check if no elements are even
     * boolean noEvens = IntStream.of(1, 3, 5, 6)
     *     .noneMatch(x -> x % 2 == 0);   // returns false
     *
     * // Empty stream always returns true
     * boolean result = IntStream.empty()
     *     .noneMatch(x -> x > 0);   // returns true
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return {@code true} if either no elements of the stream match the provided predicate or
     *         the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean noneMatch(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty OptionalInt.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()}: it always returns the first element in
     * encounter order, including for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalInt first = IntStream.of(1, 2, 3, 4, 5)
     *                                 .findFirst();   // returns OptionalInt.of(1)
     * }</pre>
     *
     * @return an OptionalInt containing the first element of the stream, or an empty OptionalInt if the stream is empty
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.IntPredicate)
     * @see #findAny(Throwables.IntPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalInt findFirst() {
        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty OptionalInt.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalInt any = IntStream.of(1, 2, 3, 4, 5)
     *                              .findAny();   // returns OptionalInt.of(1)
     * }</pre>
     *
     * @return an OptionalInt containing the first element of the stream, or an empty OptionalInt if the stream is empty
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.IntPredicate)
     * @see #findAny(Throwables.IntPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalInt findAny() {
        return first();
    }

    /**
     * Returns an OptionalInt describing the first element of this stream that matches the given predicate,
     * or an empty OptionalInt if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first even number
     * OptionalInt firstEven = IntStream.of(1, 3, 4, 6, 8)
     *     .findFirst(x -> x % 2 == 0);   // returns OptionalInt[4]
     *
     * // Find first element greater than 100
     * OptionalInt firstLarge = IntStream.of(10, 20, 30)
     *     .findFirst(x -> x > 100);   // returns OptionalInt.empty()
     *
     * // Safe retrieval with default value
     * int value = IntStream.of(1, 2, 3, 4, 5)
     *     .findFirst(x -> x > 3)
     *     .orElse(-1);   // returns 4
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code OptionalInt} describing the first element that matches the predicate, or an empty {@code OptionalInt} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalInt findFirst(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalInt describing some element of this stream that matches the given predicate,
     * or an empty OptionalInt if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is free to select any element
     * in the stream. This is to allow for maximal performance in parallel operations; the cost is that
     * multiple invocations on the same source may not return the same result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any even number (sequential streams will find first)
     * OptionalInt anyEven = IntStream.of(1, 3, 4, 6, 8)
     *     .findAny(x -> x % 2 == 0);   // returns OptionalInt[4] in sequential
     *
     * // Find any negative number
     * OptionalInt anyNegative = IntStream.of(5, 10, -3, 15)
     *     .findAny(x -> x < 0);   // returns OptionalInt[-3]
     *
     * // Parallel streams may return different elements
     * OptionalInt anyLarge = IntStream.range(1, 1000)
     *     .parallel()
     *     .findAny(x -> x > 500);   // returns any element > 500
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code OptionalInt} describing any element that matches the predicate, or an empty {@code OptionalInt} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalInt findAny(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalInt describing the last element of this stream that matches the given predicate,
     * or an empty OptionalInt if no such element exists.
     *
     * <p>This is a terminal operation.
     *
     * <p>Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last even number
     * OptionalInt lastEven = IntStream.of(1, 4, 3, 6, 5, 8)
     *     .findLast(x -> x % 2 == 0);   // returns OptionalInt[8]
     *
     * // Find last element less than 5
     * OptionalInt lastSmall = IntStream.of(1, 7, 3, 9, 2)
     *     .findLast(x -> x < 5);   // returns OptionalInt[2]
     *
     * // More efficient alternative using reversed
     * OptionalInt result = IntStream.of(1, 2, 3, 4, 5)
     *     .reversed()
     *     .findFirst(x -> x % 2 == 0);   // returns OptionalInt[4]
     * }</pre>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate to apply to elements of this stream
     * @return an {@code OptionalInt} describing the last element that matches the predicate, or an empty {@code OptionalInt} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalInt findLast(final Throwables.IntPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalInt describing the minimum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(5, 2, 8, 1, 9).min();   // returns OptionalInt[1]
     *
     * IntStream.empty().min();   // returns OptionalInt.empty()
     *
     * // Safe retrieval with default value
     * int minValue = IntStream.of(10, 20, 30).min().orElse(0);   // returns 10
     * }</pre>
     *
     * @return an OptionalInt containing the minimum element of this stream,
     *         or an empty OptionalInt if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalInt min();

    /**
     * Returns an OptionalInt describing the maximum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(5, 2, 8, 1, 9).max();   // returns OptionalInt[9]
     *
     * IntStream.empty().max();   // returns OptionalInt.empty()
     *
     * // Safe retrieval with default value
     * int maxValue = IntStream.of(10, 20, 30).max().orElse(0);   // returns 30
     * }</pre>
     *
     * @return an OptionalInt containing the maximum element of this stream,
     *         or an empty OptionalInt if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalInt max();

    /**
     * Returns the <i>k-th</i> largest element in the stream.
     * If the stream is empty or the count of elements is less than k, an empty OptionalInt is returned.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * IntStream.of(10, 30, 20, 50, 40).kthLargest(2);   // returns OptionalInt[40]
     *
     * // Find the largest element (same as max)
     * IntStream.of(5, 2, 8, 1).kthLargest(1);   // returns OptionalInt[8]
     *
     * // When k exceeds stream size
     * IntStream.of(1, 2, 3).kthLargest(5);   // returns OptionalInt.empty()
     * }</pre>
     *
     * @param k the position (1-based) of the largest element to retrieve; must be positive
     * @return an OptionalInt containing the k-th largest element, or an empty OptionalInt if the stream is empty or the count of elements is less than k
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalInt kthLargest(int k);

    /**
     * Returns the sum of elements in this stream. This is a special case of a reduction.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Note on overflow:</b> The sum is accumulated in a {@code long}, and an {@code ArithmeticException}
     * is thrown if the result overflows the {@code int} range. For large datasets or when overflow is a concern,
     * consider using {@link #mapToLong(IntToLongFunction)} followed by {@link LongStream#sum()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int total = IntStream.of(1, 2, 3, 4, 5).sum();   // returns 15
     *
     * // Handling potential overflow with long
     * long safeSum = IntStream.of(Integer.MAX_VALUE, 1)
     *     .mapToLong(i -> (long) i)
     *     .sum();
     * }</pre>
     *
     * @return the sum of elements in this stream. Returns 0 if the stream is empty.
     * @see #average()
     * @see #reduce(int, IntBinaryOperator)
     */
    @SequentialOnly
    @TerminalOp
    public abstract int sum();

    /**
     * Returns an OptionalDouble describing the arithmetic mean of elements of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3, 4, 5).average();   // returns OptionalDouble[3.0]
     *
     * IntStream.of(10, 20, 30).average();   // returns OptionalDouble[20.0]
     *
     * IntStream.empty().average();   // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double avg = IntStream.of(100, 200).average().orElse(0.0);   // returns 150.0
     * }</pre>
     *
     * @return an OptionalDouble containing the arithmetic mean of elements of this stream,
     *         or an empty optional if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble average();

    /**
     * Returns an IntSummaryStatistics describing various summary data about the elements of this stream.
     * The statistics include count, sum, min, max, and average.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // prints Count: 5
     * System.out.println("Sum: " + stats.getSum());           // prints Sum: 15
     * System.out.println("Min: " + stats.getMin());           // prints Min: 1
     * System.out.println("Max: " + stats.getMax());           // prints Max: 5
     * System.out.println("Average: " + stats.getAverage());   // prints Average: 3.0
     * }</pre>
     *
     * @return an IntSummaryStatistics describing various summary data about the elements of this stream
     */
    @SequentialOnly
    @TerminalOp
    public abstract IntSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of IntSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics and percentiles for a dataset
     * Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result =
     *     IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     *         .summaryStatisticsAndPercentiles();
     *
     * IntSummaryStatistics stats = result.left();
     * System.out.println("Count: " + stats.getCount());       // 10
     * System.out.println("Sum: " + stats.getSum());           // 55
     * System.out.println("Min: " + stats.getMin());           // 1
     * System.out.println("Max: " + stats.getMax());           // 10
     * System.out.println("Average: " + stats.getAverage());   // 5.5
     *
     * // Access percentiles (25th, 50th, 75th)
     * result.right().ifPresent(percentiles -> {
     *     percentiles.forEach((pct, value) ->
     *         System.out.println(pct + ": " + value));
     * });
     *
     * // Empty stream returns empty percentiles
     * Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> empty =
     *     IntStream.empty().summaryStatisticsAndPercentiles();
     * // empty.right().isPresent() == false
     * }</pre>
     *
     * @return a Pair where the first element is IntSummaryStatistics and the second element is
     *         an Optional containing a map from each {@link Percentage} to its corresponding value,
     *         or an empty Optional if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream, selecting elements based on the provided selector function.
     * The selector function determines which element to take from the two streams at each step.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams in order
     * IntStream.of(1, 3, 5)
     *       .mergeWith(IntStream.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *       .toIntList();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Merge with TakeSecond preference
     * IntStream.of(10, 30)
     *       .mergeWith(IntStream.of(20, 40), (a, b) -> MergeResult.TAKE_SECOND)
     *       .toIntList();   // returns [20, 40, 10, 30] (remaining elements are appended)
     *
     * // Merge with empty stream
     * IntStream.of(1, 2, 3)
     *       .mergeWith(IntStream.empty(), (a, b) -> MergeResult.TAKE_FIRST)
     *       .toIntList();   // returns [1, 2, 3]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @param b the stream to merge with this stream
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned,
     *                     otherwise the second parameter is selected.
     * @return the merged stream
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream mergeWith(final IntStream b, final IntBiFunction<MergeResult> nextSelector);

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
     * IntStream.of(1, 2, 3)
     *          .zipWith(IntStream.of(10, 20, 30, 40),
     *                   (a, b) -> a + b)
     *          .toArray();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param b the IntStream to be combined with the current IntStream. Must be {@code non-null}.
     * @param zipFunction an IntBinaryOperator that determines the combination of elements in the combined IntStream. Must be {@code non-null}.
     * @return a new IntStream that is the result of combining the current IntStream with the given IntStream
     * @see #zipWith(IntStream, int, int, IntBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream zipWith(IntStream b, IntBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3)
     *          .zipWith(IntStream.of(10, 20), IntStream.of(110, 120),
     *                   (a, b, c) -> a + b + c)
     *          .toArray();   // returns [121, 142]
     * }</pre>
     *
     * @param b the second IntStream to be combined with the current IntStream. Will be closed along with this IntStream.
     * @param c the third IntStream to be combined with the current IntStream. Will be closed along with this IntStream.
     * @param zipFunction an IntTernaryOperator that determines the combination of elements in the combined IntStream. Must be {@code non-null}.
     * @return a new IntStream that is the result of combining the current IntStream with the given IntStreams
     * @see #zipWith(IntStream, IntStream, int, int, int, IntTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream zipWith(IntStream b, IntStream c, IntTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3)
     *          .zipWith(IntStream.of(10), 0, 0, (a, b) -> a + b)
     *          .toArray();   // returns [11, 2, 3]
     * }</pre>
     *
     * @param b the IntStream to be combined with the current IntStream. Will be closed along with this IntStream.
     * @param valueForNoneA the default value to use for the current IntStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given IntStream when it runs out of elements
     * @param zipFunction an IntBinaryOperator that determines the combination of elements in the combined IntStream. Must be {@code non-null}.
     * @return a new IntStream that is the result of combining the current IntStream with the given IntStream
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream zipWith(IntStream b, int valueForNoneA, int valueForNoneB, IntBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream.of(1, 2, 3)
     *          .zipWith(IntStream.of(10), IntStream.of(100),
     *                   0, 0, 0,
     *                   (a, b, c) -> a + b + c)
     *          .toArray();   // returns [111, 2, 3]
     * }</pre>
     *
     * @param b the second IntStream to be combined with the current IntStream. Will be closed along with this IntStream.
     * @param c the third IntStream to be combined with the current IntStream. Will be closed along with this IntStream.
     * @param valueForNoneA the default value to use for the current IntStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second IntStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third IntStream when it runs out of elements
     * @param zipFunction an IntTernaryOperator that determines the combination of elements in the combined IntStream. Must be {@code non-null}.
     * @return a new IntStream that is the result of combining the current IntStream with the given IntStreams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream zipWith(IntStream b, IntStream c, int valueForNoneA, int valueForNoneB, int valueForNoneC, IntTernaryOperator zipFunction);

    /**
     * Converts this IntStream to a LongStream by widening each int element to a long element.
     * This is an intermediate operation that transforms the stream type while preserving all values.
     *
     * <p>This operation performs a lossless conversion where each int value is widened to its
     * corresponding long value. This is useful when you need to perform operations that require
     * long precision or when combining with other long streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to long stream for larger range operations
     * LongStream longStream = IntStream.of(1, 2, 3, 4, 5)
     *     .asLongStream();
     * longStream.forEach(System.out::println);   // prints: 1, 2, 3, 4, 5
     *
     * // Avoid overflow in calculations
     * long sum = IntStream.of(1_000_000, 1_000_000)
     *     .asLongStream()
     *     .map(l -> l * l)      // maps each value to its square as long
     *     .sum();               // returns 2_000_000_000_000L
     *
     * // Combine with long stream
     * LongStream combined = IntStream.range(1, 5)
     *     .asLongStream()
     *     .append(100L, 200L, 300L);
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @return a LongStream containing the elements of this stream widened to long
     * @see #asFloatStream()
     * @see #asDoubleStream()
     * @see #mapToLong(IntToLongFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract LongStream asLongStream();

    /**
     * Converts this IntStream to a FloatStream by converting each int element to a float element.
     * This is an intermediate operation that transforms the stream type to floating-point representation.
     *
     * <p>This operation converts each int value to its float representation. For most int values,
     * this conversion is lossless, but very large int values may lose precision due to float's
     * limited precision (24 bits of mantissa).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to floats
     * FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5)
     *     .asFloatStream();
     * floatStream.forEach(System.out::println);   // prints: 1.0, 2.0, 3.0, 4.0, 5.0
     *
     * // Perform floating-point operations
     * double average = IntStream.of(10, 20, 30, 40)
     *     .asFloatStream()
     *     .map(f -> f / 100.0f)     // maps to percentages
     *     .average()
     *     .orElse(0.0);             // returns 0.25
     *
     * // Mix integer and float operations
     * FloatStream results = IntStream.range(1, 6)
     *     .asFloatStream()
     *     .map(f -> f * 1.5f);      // maps each value to itself times 1.5
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @return a FloatStream containing the elements of this stream converted to float
     * @see #asLongStream()
     * @see #asDoubleStream()
     * @see #mapToFloat(IntToFloatFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream asFloatStream();

    /**
     * Converts this IntStream to a DoubleStream by converting each int element to a double element.
     * This is an intermediate operation that transforms the stream type to double-precision floating-point.
     *
     * <p>This operation performs a lossless conversion where each int value is converted to its
     * exact double representation. All int values can be represented exactly as doubles since
     * double has 53 bits of precision (more than int's 32 bits).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert integers to doubles
     * DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5)
     *     .asDoubleStream();
     * doubleStream.forEach(System.out::println);   // prints: 1.0, 2.0, 3.0, 4.0, 5.0
     *
     * // Calculate precise averages
     * double average = IntStream.of(10, 20, 30, 40, 50)
     *     .asDoubleStream()
     *     .average()
     *     .orElse(0.0);         // returns 30.0
     *
     * // Perform mathematical operations with high precision
     * DoubleStream results = IntStream.range(1, 11)
     *     .asDoubleStream()
     *     .map(Math::sqrt)      // maps each value to its square root
     *     .map(d -> d * 2);     // maps each result to double
     *
     * // Useful for statistical calculations
     * double stdDev = IntStream.of(scores)
     *     .asDoubleStream()
     *     .map(d -> Math.pow(d - mean, 2))
     *     .average()
     *     .map(Math::sqrt)
     *     .orElse(0.0);
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @return a DoubleStream containing the elements of this stream converted to double
     * @see #asLongStream()
     * @see #asFloatStream()
     * @see #mapToDouble(IntToDoubleFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream asDoubleStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to an Integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Boxed stream of integers
     * IntStream.of(1, 2, 3)
     *       .boxed()
     *       .toList();   // returns [1, 2, 3]
     *
     * // Use boxed for generic operations
     * IntStream.of(5, 3, 1, 4, 2)
     *       .boxed()
     *       .sorted()
     *       .toList();   // returns [1, 2, 3, 4, 5]
     *
     * // Empty stream
     * IntStream.empty()
     *       .boxed()
     *       .toList();   // returns []
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * @return a Stream consisting of the elements of this stream, each boxed to an Integer
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Integer> boxed();

    /**
     * Converts this IntStream to a java.util.stream.IntStream.
     * This allows interoperability with the standard Java Stream API.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to JDK stream for interop
     * java.util.stream.IntStream jdkStream = IntStream.of(1, 2, 3).toJdkStream();
     * jdkStream.sum();   // returns 6
     *
     * // Empty stream conversion
     * java.util.stream.IntStream empty = IntStream.empty().toJdkStream();
     * empty.count();   // returns 0
     * }</pre>
     *
     * @return a java.util.stream.IntStream containing the elements of this stream
     */
    @SequentialOnly
    @IntermediateOp
    public abstract java.util.stream.IntStream toJdkStream();

    /**
     * Transforms this IntStream using the provided function that operates on java.util.stream.IntStream.
     * This method allows applying standard Java Stream API operations and converting back to IntStream.
     * The transformation is applied immediately (not deferred).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use JDK IntStream operations
     * IntStream.of(1, 2, 3, 4, 5)
     *     .transformB(s -> s.filter(i -> i % 2 == 0))
     *     .toArray();   // returns [2, 4]
     *
     * // Chain multiple JDK operations
     * IntStream.range(1, 10)
     *     .transformB(s -> s.map(i -> i * 2).limit(3))
     *     .toArray();   // returns [2, 4, 6]
     * }</pre>
     *
     * @param transfer the function to transform the java.util.stream.IntStream
     * @return a new IntStream resulting from the transformation
     * @throws IllegalArgumentException if transfer is null
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public IntStream transformB(final Function<? super java.util.stream.IntStream, ? extends java.util.stream.IntStream> transfer) {
        return transformB(transfer, false);
    }

    /**
     * Transforms this IntStream using the provided function that operates on java.util.stream.IntStream.
     * This method allows applying standard Java Stream API operations and converting back to IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Immediate transformation
     * IntStream.of(1, 2, 3, 4, 5)
     *     .transformB(s -> s.filter(i -> i > 2), false)
     *     .toArray();   // returns [3, 4, 5]
     *
     * // Deferred transformation - useful for lazy evaluation
     * IntStream.of(1, 2, 3, 4, 5)
     *     .transformB(s -> s.map(i -> i * 2), true)
     *     .limit(3)
     *     .toArray();   // returns [2, 4, 6]
     * }</pre>
     *
     * @param transfer the function to transform the java.util.stream.IntStream
     * @param deferred if {@code true}, the transformation is deferred until the stream is consumed;
     *                 if {@code false}, the transformation is applied immediately
     * @return a new IntStream resulting from the transformation
     * @throws IllegalArgumentException if transfer is null
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public IntStream transformB(final Function<? super java.util.stream.IntStream, ? extends java.util.stream.IntStream> transfer, final boolean deferred)
            throws IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        if (deferred) {
            final Supplier<IntStream> delayInitializer = () -> IntStream.from(transfer.apply(toJdkStream()));
            return IntStream.defer(delayInitializer);
        } else {
            return IntStream.from(transfer.apply(toJdkStream()));
        }
    }

    /**
     * Returns an extended IntIterator for this stream with additional functionality.
     * This is an internal method used by the stream framework to provide extended iteration capabilities.
     *
     * @return an IntIteratorEx providing extended iteration capabilities including advance() and count()
     */
    abstract IntIteratorEx iteratorEx();

    // private static final IntStream EMPTY_STREAM = new ArrayIntStream(N.EMPTY_INT_ARRAY, true, null);

    /**
     * Returns an empty IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an empty stream
     * IntStream empty = IntStream.empty();
     * long count = empty.count();   // returns 0
     *
     * // Useful as a default return value
     * public IntStream getNumbers(boolean condition) {
     *     return condition ? IntStream.of(1, 2, 3) : IntStream.empty();
     * }
     * }</pre>
     *
     * @return an empty IntStream
     */
    public static IntStream empty() {
        return new ArrayIntStream(N.EMPTY_INT_ARRAY, true, null);
    }

    /**
     * Returns an IntStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Implementation Note:</b> it's equivalent to {@code Stream.just(supplier).flatMapToInt(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Defer expensive stream creation
     * IntStream deferred = IntStream.defer(() -> {
     *     // This code runs when the stream is consumed or closed
     *     System.out.println("Creating stream...");
     *     return IntStream.range(1, 1000000);
     * });
     *
     * // Stream is created when consumed
     * int sum = deferred.limit(10).sum();   // prints "Creating stream..."
     *
     * // Useful for conditional stream generation
     * IntStream conditional = IntStream.defer(() ->
     *     someCondition() ? IntStream.of(1, 2, 3) : IntStream.range(1, 100)
     * );
     * }</pre>
     *
     * @param supplier the supplier that provides the IntStream
     * @return a new IntStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static IntStream defer(final Supplier<IntStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<IntStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToInt(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Creates an IntStream from a java.util.stream.IntStream.
     * This allows conversion from the standard Java Stream API to this IntStream implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert from JDK IntStream
     * java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
     * IntStream stream = IntStream.from(jdkStream);
     * stream.forEach(System.out::println);
     *
     * // Chain with other operations
     * IntStream converted = IntStream.from(java.util.stream.IntStream.range(1, 10))
     *     .filter(i -> i % 2 == 0);
     * }</pre>
     *
     * @param stream the java.util.stream.IntStream to convert
     * @return an IntStream containing the elements from the input stream, or empty stream if input is null
     */
    public static IntStream from(final java.util.stream.IntStream stream) {
        if (stream == null) {
            return empty();
        }

        return of(new IntIteratorEx() {
            private PrimitiveIterator.OfInt iter = null;
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
            public int nextInt() {
                if (exhausted) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextInt();
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
            public int[] toArray() {
                if (exhausted) {
                    return new int[0];
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
     * Creates an IntStream containing a single Integer element.
     * If the element is {@code null}, returns an empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With non-null value
     * IntStream.ofNullable(42).forEach(System.out::println);   // prints 42
     *
     * // With null value
     * IntStream.ofNullable(null).count();   // returns 0
     *
     * // Useful in optional chaining
     * Integer value = getValue();   // value is possibly null
     * IntStream.ofNullable(value).forEach(System.out::println);
     * }</pre>
     *
     * @param e the Integer element (nullable)
     * @return an IntStream containing the element, or empty stream if the element is null
     */
    public static IntStream ofNullable(final Integer e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Creates an IntStream from an array of int values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from varargs
     * IntStream.of(1, 2, 3, 4, 5).forEach(System.out::println);
     *
     * // Create stream from array
     * int[] numbers = {10, 20, 30, 40};
     * IntStream.of(numbers).sum();   // returns 100
     *
     * // Empty array
     * IntStream.of().count();   // returns 0
     * }</pre>
     *
     * @param a the array of int values
     * @return an IntStream containing the elements from the array
     */
    public static IntStream of(final int... a) {
        return N.isEmpty(a) ? empty() : new ArrayIntStream(a);
    }

    /**
     * Creates an IntStream from a portion of an int array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
     *
     * // Get elements from index 2 to 5 (exclusive)
     * IntStream.of(numbers, 2, 5).toArray();   // returns [3, 4, 5]
     *
     * // Get first 3 elements
     * IntStream.of(numbers, 0, 3).toArray();   // returns [1, 2, 3]
     *
     * // Get last 3 elements
     * IntStream.of(numbers, 7, 10).toArray();   // returns [8, 9, 10]
     * }</pre>
     *
     * @param a the array of int values
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an IntStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     */
    public static IntStream of(final int[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayIntStream(a, fromIndex, toIndex);
    }

    /**
     * Creates an IntStream from an array of Integer objects.
     * Each Integer is unboxed to its primitive int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from Integer array
     * Integer[] values = {1, 2, 3, 4, 5};
     * IntStream.of(values).sum();   // returns 15
     *
     * // Works with boxed integers
     * Integer[] boxed = {Integer.valueOf(10), Integer.valueOf(20)};
     * IntStream.of(boxed).toArray();   // returns [10, 20]
     * }</pre>
     *
     * @param a the array of Integer objects ({@code null} elements are unboxed to {@code 0})
     * @return an IntStream containing the unboxed elements from the array
     */
    public static IntStream of(final Integer[] a) {
        return Stream.of(a).mapToInt(FI.unbox());
    }

    /**
     * Creates an IntStream from a portion of an Integer array.
     * Each Integer is unboxed to its primitive int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] values = {10, 20, 30, 40, 50};
     *
     * // Get middle elements
     * IntStream.of(values, 1, 4).toArray();   // returns [20, 30, 40]
     *
     * // Get first two elements
     * IntStream.of(values, 0, 2).sum();   // returns 30
     * }</pre>
     *
     * @param a the array of Integer objects ({@code null} elements in the range are unboxed to {@code 0})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an IntStream containing the unboxed elements from the specified range of the array
     */
    public static IntStream of(final Integer[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToInt(FI.unbox());
    }

    /**
     * Creates an IntStream from a Collection of Integer objects.
     * Each Integer is unboxed to its primitive int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // From ArrayList
     * List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
     * IntStream.of(list).sum();   // returns 15
     *
     * // From Set
     * Set<Integer> set = new HashSet<>(Arrays.asList(10, 20, 30));
     * IntStream.of(set).forEach(System.out::println);
     * }</pre>
     *
     * @param c the Collection of Integer objects ({@code null} elements are unboxed to {@code 0})
     * @return an IntStream containing the unboxed elements from the collection
     */
    public static IntStream of(final Collection<Integer> c) {
        return Stream.of(c).mapToInt(FI.unbox());
    }

    /**
     * Creates an IntStream from an IntIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // From IntIterator
     * IntIterator iterator = IntIterator.of(1, 2, 3, 4, 5);
     * IntStream.of(iterator).forEach(System.out::println);
     *
     * // From IntList iterator
     * IntList intList = IntList.of(10, 20, 30);
     * IntStream.of(intList.iterator()).sum();   // returns 60
     * }</pre>
     *
     * @param iterator the IntIterator providing the elements
     * @return an IntStream containing the elements from the iterator, or empty stream if iterator is null
     */
    public static IntStream of(final IntIterator iterator) {
        return iterator == null ? empty() : new IteratorIntStream(iterator);
    }

    //    /**
    //     * Creates an IntStream from a java.util.stream.IntStream.
    //     * This method is deprecated, use {@link #from(java.util.stream.IntStream)} instead.
    //     *
    //     * @param stream the java.util.stream.IntStream to convert
    //     * @return an IntStream containing the elements from the input stream
    //     * @deprecated Use {@link #from(java.util.stream.IntStream)} instead
    //     */
    //    @Deprecated
    //    public static IntStream of(final java.util.stream.IntStream stream) { // Should the name be from?
    //        return from(stream);
    //    }

    /**
     * Creates an IntStream from an OptionalInt.
     * If the OptionalInt is empty or {@code null}, returns an empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // From non-empty OptionalInt
     * OptionalInt opt = OptionalInt.of(42);
     * IntStream.of(opt).forEach(System.out::println);   // prints 42
     *
     * // From empty OptionalInt
     * OptionalInt empty = OptionalInt.empty();
     * IntStream.of(empty).count();   // returns 0
     *
     * // Chain with stream operations
     * IntStream.of(OptionalInt.of(10))
     *     .map(i -> i * 2)
     *     .sum();   // returns 20
     * }</pre>
     *
     * @param op the OptionalInt containing the value
     * @return an IntStream containing the value if present, otherwise empty stream
     */
    public static IntStream of(final OptionalInt op) {
        return op == null || op.isEmpty() ? IntStream.empty() : IntStream.of(op.get());
    }

    /**
     * Creates an IntStream from a java.util.OptionalInt.
     * If the OptionalInt is empty or {@code null}, returns an empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // From JDK OptionalInt
     * java.util.OptionalInt jdkOpt = java.util.OptionalInt.of(100);
     * IntStream.of(jdkOpt).forEach(System.out::println);   // prints 100
     *
     * // From empty JDK OptionalInt
     * java.util.OptionalInt empty = java.util.OptionalInt.empty();
     * IntStream.of(empty).count();   // returns 0
     * }</pre>
     *
     * @param op the java.util.OptionalInt containing the value
     * @return an IntStream containing the value if present, otherwise empty stream
     */
    public static IntStream of(final java.util.OptionalInt op) {
        return op == null || op.isEmpty() ? IntStream.empty() : IntStream.of(op.getAsInt());
    }

    /**
     * Creates an IntStream from an {@link IntBuffer}, reading elements from the buffer's
     * current {@link IntBuffer#position() position} (inclusive) to its
     * {@link IntBuffer#limit() limit} (exclusive). Returns an empty stream if {@code buf}
     * is {@code null}.
     *
     * <p>The buffer's position is <b>not</b> advanced by stream consumption — the stream
     * reads via absolute indexed {@code get(int)} access, so the buffer remains usable
     * afterwards.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // From IntBuffer
     * IntBuffer buffer = IntBuffer.allocate(5);
     * buffer.put(new int[]{1, 2, 3, 4, 5});
     * buffer.flip();                // position is reset to 0
     * IntStream.of(buffer).sum();   // returns 15
     *
     * // With positioned buffer
     * IntBuffer positioned = IntBuffer.wrap(new int[]{10, 20, 30, 40, 50});
     * positioned.position(2);               // starts from index 2
     * IntStream.of(positioned).toArray();   // returns [30, 40, 50]
     * }</pre>
     *
     * @param buf the IntBuffer to read from (may be {@code null})
     * @return an IntStream over elements in {@code buf} from {@code position} (inclusive)
     *         to {@code limit} (exclusive), or an empty stream if {@code buf} is {@code null}
     */
    public static IntStream of(final IntBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return range(buf.position(), buf.limit()).map(buf::get);
    }

    /**
     * Creates an IntStream of Unicode code points from a CharSequence.
     * Surrogate pairs are properly handled and converted to their code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic ASCII string
     * IntStream.ofCodePoints("Hello").toArray();   // returns [72, 101, 108, 108, 111]
     *
     * // String with emoji (surrogate pairs)
     * IntStream.ofCodePoints("Hello 😀")
     *     .forEach(cp -> System.out.println(cp + " = " + Character.toString(cp)));
     *
     * // Count unique code points
     * long uniqueCount = IntStream.ofCodePoints("programming")
     *     .distinct()
     *     .count();   // returns 8
     * }</pre>
     *
     * @param str the CharSequence to convert to code points
     * @return an IntStream containing the Unicode code points from the CharSequence
     */
    public static IntStream ofCodePoints(final CharSequence str) {
        if (Strings.isEmpty(str)) {
            return empty();
        }

        return from(str.codePoints());
    }

    private static final Function<int[], IntStream> flatMapper = IntStream::of;

    private static final Function<int[][], IntStream> flattMapper = IntStream::flatten;

    /**
     * Splits the total size into chunks based on the specified maximum chunk count.
     * <br />
     * The size of the chunks is larger first.
     * <br />
     * The length of returned IntStream may be less than the specified {@code maxChunkCount} if the input {@code totalSize} is less than {@code maxChunkCount}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split 7 into 5 chunks with larger chunks first; mapper returns chunk size
     * IntStream.splitByChunkCount(7, 5, (fromIndex, toIndex) ->
     *     toIndex - fromIndex).toArray();   // returns [2, 2, 1, 1, 1]
     *
     * // Use the chunk boundaries to process an array
     * final int[] a = Array.rangeClosed(1, 7);
     * IntStream.splitByChunkCount(7, 5, (from, to) ->
     *     IntStream.of(a, from, to).sum()).toArray();   // returns [3, 7, 5, 6, 7] (chunk sums)
     * }</pre>
     *
     * @param totalSize the total size to be split. It could be the size of an array, list, etc.
     * @param maxChunkCount the maximum number of chunks to split into
     * @param mapper a function that maps a chunk defined by its from index (inclusive) and to index (exclusive) to an int value in the resulting stream
     * @return an IntStream of the mapped chunk values
     * @throws IllegalArgumentException if {@code totalSize} is negative or {@code maxChunkCount} is not positive.
     * @see #splitByChunkCount(int, int, boolean, IntBinaryOperator)
     */
    public static IntStream splitByChunkCount(final int totalSize, final int maxChunkCount, final IntBinaryOperator mapper) {
        return splitByChunkCount(totalSize, maxChunkCount, false, mapper);
    }

    /**
     * Splits the total size into chunks based on the specified maximum chunk count.
     * <br />
     * The size of the chunks can be either smaller or larger first based on the flag.
     * <br />
     * The length of returned IntStream may be less than the specified {@code maxChunkCount} if the input {@code totalSize} is less than {@code maxChunkCount}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Smaller chunks first: chunk sizes are [1, 1, 1, 2, 2]
     * IntStream.splitByChunkCount(7, 5, true, (fromIndex, toIndex) ->
     *     toIndex - fromIndex).toArray();   // returns [1, 1, 1, 2, 2]
     *
     * // Larger chunks first: chunk sizes are [2, 2, 1, 1, 1]
     * IntStream.splitByChunkCount(7, 5, false, (fromIndex, toIndex) ->
     *     toIndex - fromIndex).toArray();   // returns [2, 2, 1, 1, 1]
     * }</pre>
     *
     * @param totalSize the total size to be split. It could be the size of an array, list, etc.
     * @param maxChunkCount the maximum number of chunks to split into
     * @param sizeSmallerFirst if {@code true}, smaller chunks will be created first; otherwise, larger chunks will be created first
     * @param mapper a function that maps a chunk defined by its from index (inclusive) and to index (exclusive) to an int value in the resulting stream
     * @return an IntStream of the mapped chunk values
     * @throws IllegalArgumentException if {@code totalSize} is negative or {@code maxChunkCount} is not positive.
     * @see Stream#splitByChunkCount(int, int, boolean, IntBiFunction)
     */
    public static IntStream splitByChunkCount(final int totalSize, final int maxChunkCount, final boolean sizeSmallerFirst, final IntBinaryOperator mapper) {
        N.checkArgNotNegative(totalSize, cs.totalSize);
        N.checkArgPositive(maxChunkCount, cs.maxChunkCount);

        if (totalSize == 0) {
            return IntStream.empty();
        }

        final int count = Math.min(totalSize, maxChunkCount);
        final int biggerSize = totalSize % maxChunkCount == 0 ? totalSize / maxChunkCount : totalSize / maxChunkCount + 1;
        final int biggerCount = totalSize % maxChunkCount;
        final int smallerSize = Math.max(totalSize / maxChunkCount, 1);
        final int smallerCount = count - biggerCount;

        IntIterator iter = null;

        if (sizeSmallerFirst) {
            iter = new IntIteratorEx() {
                private int cnt = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < totalSize;
                }

                @Override
                public int nextInt() {
                    if (cursor >= totalSize) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return mapper.applyAsInt(cursor, cursor = (cnt++ < smallerCount ? cursor + smallerSize : cursor + biggerSize));
                }

                @Override
                public void advance(long n) {
                    if (n > 0) {
                        while (n-- > 0 && cursor < totalSize) {
                            cursor = cnt++ < smallerCount ? cursor + smallerSize : cursor + biggerSize;
                        }
                    }
                }

                @Override
                public long count() {
                    final long ret = count - cnt;
                    cnt = count;
                    cursor = totalSize;
                    return ret;
                }
            };
        } else {
            iter = new IntIteratorEx() {
                private int cnt = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < totalSize;
                }

                @Override
                public int nextInt() {
                    if (cursor >= totalSize) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return mapper.applyAsInt(cursor, cursor = (cnt++ < biggerCount ? cursor + biggerSize : cursor + smallerSize));
                }

                @Override
                public void advance(long n) {
                    if (n > 0) {
                        while (n-- > 0 && cursor < totalSize) {
                            cursor = cnt++ < biggerCount ? cursor + biggerSize : cursor + smallerSize;
                        }
                    }
                }

                @Override
                public long count() {
                    final long ret = count - cnt;
                    cnt = count;
                    cursor = totalSize;
                    return ret;
                }
            };
        }

        return IntStream.of(iter);
    }

    /**
     * Flattens a two-dimensional int array into a single IntStream.
     * Elements are read row by row (horizontally).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[][] matrix = {
     *     {1, 2, 3},
     *     {4, 5, 6},
     *     {7, 8, 9}
     * };
     * IntStream.flatten(matrix).toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Process all elements in a matrix
     * int sum = IntStream.flatten(matrix).sum();   // returns 45
     * }</pre>
     *
     * @param a the two-dimensional int array to flatten
     * @return an IntStream containing all elements from the array in row-major order
     */
    public static IntStream flatten(final int[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToInt(flatMapper);
    }

    /**
     * Flattens a two-dimensional int array into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[][] matrix = {
     *     {1, 2, 3},
     *     {4, 5, 6},
     *     {7, 8, 9}
     * };
     *
     * // Horizontal (row by row)
     * IntStream.flatten(matrix, false).toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Vertical (column by column)
     * IntStream.flatten(matrix, true).toArray();   // returns [1, 4, 7, 2, 5, 8, 3, 6, 9]
     * }</pre>
     *
     * @param a the two-dimensional int array to flatten
     * @param vertically if {@code true}, elements are read column by column; if {@code false}, row by row
     * @return an IntStream containing all elements from the array
     */
    public static IntStream flatten(final int[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToInt(flatMapper);
        }

        long n = 0;

        for (final int[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final IntIterator iter = new IntIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public int nextInt() {
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
     * Flattens a two-dimensional int array into a single IntStream with alignment.
     * If arrays have different lengths, shorter arrays are padded with the specified value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten horizontally with padding
     * int[][] jagged = { {1, 2}, {3}, {4, 5, 6} };
     * IntStream.flatten(jagged, 0, false).toArray();   // returns [1, 2, 0, 3, 0, 0, 4, 5, 6]
     *
     * // Flatten vertically with padding
     * int[][] matrix = { {1, 2, 3}, {4, 5}, {6} };
     * IntStream.flatten(matrix, 0, true).toArray();   // returns [1, 4, 6, 2, 5, 0, 3, 0, 0]
     *
     * // Align with custom padding value
     * int[][] data = { {1, 2}, {3} };
     * IntStream.flatten(data, -1, false).toArray();   // returns [1, 2, 3, -1]
     * }</pre>
     *
     * @param a the two-dimensional int array to flatten
     * @param valueForAlignment the element to append so each row/column has the same number of elements
     * @param vertically if {@code true}, elements are read column by column; if {@code false}, row by row
     * @return an IntStream containing all elements from the array with alignment padding
     */
    public static IntStream flatten(final int[][] a, final int valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final int[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        IntIterator iter = null;

        if (vertically) {
            iter = new IntIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public int nextInt() {
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
            iter = new IntIteratorEx() {

                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public int nextInt() {
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
     * Flattens a three-dimensional int array into a single IntStream.
     * Elements are read in the order: array[i][j][k] for all valid indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[][][] cube = {
     *     {{1, 2}, {3, 4}},
     *     {{5, 6}, {7, 8}}
     * };
     * IntStream.flatten(cube).toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * // Process all elements
     * int sum = IntStream.flatten(cube).sum();   // returns 36
     * }</pre>
     *
     * @param a the three-dimensional int array to flatten
     * @return an IntStream containing all elements from the array
     */
    public static IntStream flatten(final int[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToInt(flattMapper);
    }

    /**
     * Creates an IntStream of sequential integers from startInclusive (inclusive) to endExclusive (exclusive).
     * If startInclusive &gt;= endExclusive, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate numbers from 0 to 9
     * IntStream.range(0, 10).toArray();   // returns [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Use in loop-like operations
     * IntStream.range(1, 6).forEach(i -> System.out.println("Item " + i));
     *
     * // Empty stream when start >= end
     * IntStream.range(5, 5).count();    // returns 0
     * IntStream.range(10, 5).count();   // returns 0
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return an IntStream of sequential integers
     */
    public static IntStream range(final int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = (long) endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

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
                next = (int) (next + n);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public int[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an IntStream of integers from startInclusive (inclusive) to endExclusive (exclusive) with the specified step.
     * The step can be positive or negative but cannot be zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Even numbers from 0 to 10
     * IntStream.range(0, 10, 2).toArray();   // returns [0, 2, 4, 6, 8]
     *
     * // Counting down
     * IntStream.range(10, 0, -2).toArray();   // returns [10, 8, 6, 4, 2]
     *
     * // Every 5th number
     * IntStream.range(5, 25, 5).toArray();   // returns [5, 10, 15, 20]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value
     * @return an IntStream of integers with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static IntStream range(final int startInclusive, final int endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endExclusive == startInclusive) {
            return empty();
        }

        if ((endExclusive > startInclusive && by < 0) || (endExclusive < startInclusive && by > 0)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = ((long) endExclusive - startInclusive) / by + (((long) endExclusive - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int result = next;
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
                next = (int) (next + n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public int[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an IntStream of sequential integers from startInclusive (inclusive) to endInclusive (inclusive).
     * If startInclusive &gt; endInclusive, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate numbers from 1 to 10 (both inclusive)
     * IntStream.rangeClosed(1, 10).toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     *
     * // Single element
     * IntStream.rangeClosed(5, 5).toArray();   // returns [5]
     *
     * // Empty stream when start > end
     * IntStream.rangeClosed(10, 5).count();   // returns 0
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return an IntStream of sequential integers
     */
    public static IntStream rangeClosed(final int startInclusive, final int endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = (long) endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

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
                next = (int) (next + n);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public int[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an IntStream of integers from startInclusive (inclusive) to endInclusive (inclusive) with the specified step.
     * The step can be positive or negative but cannot be zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Even numbers from 0 to 10 (inclusive)
     * IntStream.rangeClosed(0, 10, 2).toArray();   // returns [0, 2, 4, 6, 8, 10]
     *
     * // Counting down (inclusive)
     * IntStream.rangeClosed(10, 0, -2).toArray();   // returns [10, 8, 6, 4, 2, 0]
     *
     * // Every 3rd number
     * IntStream.rangeClosed(3, 15, 3).toArray();   // returns [3, 6, 9, 12, 15]
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value
     * @return an IntStream of integers with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static IntStream rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        }

        if ((endInclusive > startInclusive && by < 0) || (endInclusive < startInclusive && by > 0)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = ((long) endInclusive - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int result = next;
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
                next = (int) (next + n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public int[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an IntStream that repeats the specified element for the given number of times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Repeat a value 5 times
     * IntStream.repeat(42, 5).toArray();   // returns [42, 42, 42, 42, 42]
     *
     * // Initialize an array with default value
     * int[] defaults = IntStream.repeat(0, 10).toArray();   // returns [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
     *
     * // Create a stream of same values for calculations
     * int sum = IntStream.repeat(10, 5).sum();   // returns 50
     * }</pre>
     *
     * @param element the int value to repeat
     * @param n the number of times to repeat the element
     * @return an IntStream containing n copies of the element
     * @throws IllegalArgumentException if n is negative
     */
    public static IntStream repeat(final int element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

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
            public int[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final int[] result = new int[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an infinite IntStream of random integers.
     * Values are generated using a {@link SecureRandom} instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random integers
     * IntStream.random().limit(10).toArray();
     *
     * // Generate random numbers and filter
     * IntStream.random()
     *     .limit(100)
     *     .filter(i -> i > 0)
     *     .limit(10)
     *     .forEach(System.out::println);
     * }</pre>
     *
     * @return an infinite IntStream of random integers
     */
    public static IntStream random() {
        return generate(RAND::nextInt);
    }

    /**
     * Creates an infinite IntStream of random integers within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate random integers between 1 and 100
     * IntStream.random(1, 101).limit(10).toArray();
     *
     * // Generate 5 random dice rolls
     * IntStream.random(1, 7).limit(5).forEach(System.out::println);
     *
     * // Random ages between 18 and 65
     * int[] ages = IntStream.random(18, 66).limit(100).toArray();
     * }</pre>
     *
     * @param startInclusive the lower bound (inclusive)
     * @param endExclusive the upper bound (exclusive)
     * @return an infinite IntStream of random integers in the range [startInclusive, endExclusive)
     * @throws IllegalArgumentException if startInclusive &gt;= endExclusive
     */
    public static IntStream random(final int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final long mod = (long) endExclusive - (long) startInclusive;

        if (mod < Integer.MAX_VALUE) {
            final int n = (int) mod;

            return generate(() -> RAND.nextInt(n) + startInclusive);
        } else {
            return generate(() -> (int) (Math.abs(RAND.nextLong() % mod) + startInclusive));
        }
    }

    /**
     * Creates an IntStream of indices from 0 (inclusive) to the specified length or size (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate indices for array iteration
     * int[] array = {10, 20, 30, 40, 50};
     * IntStream.ofIndices(array.length).forEach(i ->
     *     System.out.println("Index " + i + ": " + array[i]));
     *
     * // Process list by indices
     * List<String> list = Arrays.asList("a", "b", "c");
     * IntStream.ofIndices(list.size()).toArray();   // returns [0, 1, 2]
     * }</pre>
     *
     * @param lenOrSize the length or size of a collection, map, array, CharSequence, etc.
     * @return an IntStream of indices from 0 to lenOrSize (exclusive)
     * @throws IllegalArgumentException if lenOrSize is negative
     * @see N#len(CharSequence)
     * @see N#len(int[])
     * @see N#len(Object[])
     * @see N#size(Collection)
     * @see N#size(Map)
     * @see N#forEach(int, int, Throwables.IntConsumer)
     */
    @Beta
    public static IntStream ofIndices(final int lenOrSize) {
        N.checkArgNotNegative(lenOrSize, cs.lenOrSize);

        return range(0, lenOrSize);
    }

    /**
     * Creates an IntStream of indices from 0 to {@code lenOrSize} (exclusive) with the specified step when {@code step} is positive,
     * or from {@code lenOrSize} - 1 to 0 (inclusive) with the specified {@code step} when step is negative.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Forward iteration with step
     * IntStream.ofIndices(10, 2).toArray();   // returns [0, 2, 4, 6, 8]
     *
     * // Backward iteration
     * IntStream.ofIndices(5, -1).toArray();   // returns [4, 3, 2, 1, 0]
     *
     * // Process every 3rd element
     * int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9};
     * IntStream.ofIndices(array.length, 3)
     *     .forEach(i -> System.out.println(array[i]));   // prints 1, 4, 7
     * }</pre>
     *
     * @param lenOrSize the length or size of a collection, map, array, CharSequence, etc.
     * @param step the increment value for each iteration in the range. It can be positive or negative but not zero.
     * @return an IntStream of indices from 0 to lenOrSize (exclusive) with the specified step when it is positive,
     * or from lenOrSize - 1 to 0 (inclusive) with the specified step when it is negative.
     * @throws IllegalArgumentException if lenOrSize is negative, or if step is zero.
     * @see N#len(CharSequence)
     * @see N#len(int[])
     * @see N#len(Object[])
     * @see N#size(Collection)
     * @see N#size(Map)
     * @see N#forEach(int, int, int, Throwables.IntConsumer)
     */
    @Beta
    public static IntStream ofIndices(final int lenOrSize, final int step) {
        N.checkArgNotNegative(lenOrSize, cs.lenOrSize);
        N.checkArgument(step != 0, "The input parameter 'step' cannot be zero");

        if (step == 1) {
            return range(0, lenOrSize);
        } else if (step < 0) {
            return range(lenOrSize - 1, -1, step);
        } else {
            return range(0, lenOrSize, step);
        }
    }

    /**
     * Creates an IntStream of indices from the given source using the provided index function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all occurrences of a value in an array
     * int[] source = { 1, 2, 3, 1, 5, 1 };
     * IntStream.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).toArray();   // returns [0, 3, 5]
     *
     * // Find all indices of a character in a string
     * String str = "hello world";
     * IntStream.ofIndices(str, (s, fromIndex) -> s.indexOf('o', fromIndex)).toArray();   // returns [4, 7]
     *
     * // Find all occurrences in a list
     * List<String> list = Arrays.asList("a", "b", "a", "c", "a");
     * IntStream.ofIndices(list, (l, fromIndex) -> N.indexOf(l, "a", fromIndex)).toArray();   // returns [0, 2, 4]
     * }</pre>
     *
     * @param <AC> the type of the source. It should be {@code array}, {@code Collection} or {@code CharSequence}.
     * @param source the source from which indices are generated
     * @param indexFunc the function to generate indices from the source
     * @return an IntStream of indices
     * @throws IllegalArgumentException if indexFunc is null
     */
    @Beta
    public static <AC> IntStream ofIndices(final AC source, final ObjIntFunction<? super AC, Integer> indexFunc) {
        return ofIndices(source, 0, indexFunc);
    }

    /**
     * Creates an IntStream of indices from the given source starting from the specified index using the provided index function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all occurrences starting from a specific index
     * int[] source = { 1, 2, 3, 1, 5, 1 };
     * IntStream.ofIndices(source, 1, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).toArray();   // returns [3, 5]
     *
     * // Find indices starting after a position
     * String str = "hello world";
     * IntStream.ofIndices(str, 5, (s, fromIndex) -> s.indexOf('o', fromIndex)).toArray();   // returns [7]
     *
     * // Find occurrences in a list starting from index 2
     * List<String> list = Arrays.asList("a", "b", "a", "c", "a");
     * IntStream.ofIndices(list, 2, (l, fromIndex) -> N.indexOf(l, "a", fromIndex)).toArray();   // returns [2, 4]
     * }</pre>
     *
     * @param <AC> the type of the source. It should be {@code array}, {@code Collection} or {@code CharSequence}.
     * @param source the source from which indices are generated
     * @param fromIndex the starting index from which to generate indices
     * @param indexFunc the function to generate indices from the source
     * @return an IntStream of indices
     * @throws IllegalArgumentException if fromIndex is negative or if indexFunc is null
     */
    @Beta
    public static <AC> IntStream ofIndices(final AC source, final int fromIndex, final ObjIntFunction<? super AC, Integer> indexFunc) {
        return ofIndices(source, fromIndex, 1, indexFunc);
    }

    /**
     * Creates an IntStream of indices from the given source starting from the specified index,
     * advancing by the specified increment, using the provided index function.
     * A positive increment iterates forward; a negative increment iterates backward.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Forwards:
     * int[] source = { 1, 2, 3, 1, 5, 1};
     * IntStream.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).println();              // [0, 3, 5]
     * IntStream.ofIndices(source, 1, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).println();           // [3, 5]
     *
     * // Backwards
     * IntStream.ofIndices(source, 5, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).println();   // [5, 3, 0]
     * IntStream.ofIndices(source, 4, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).println();   // [3, 0]
     *
     * }</pre>
     *
     * @param <AC> the type of the source. It should be {@code array}, {@code Collection} or {@code CharSequence}.
     * @param source the source from which indices are generated
     * @param fromIndex the starting index from which to generate indices
     * @param increment the increment value for generating indices (can be positive or negative but not zero)
     * @param indexFunc the function to generate indices from the source
     * @return an IntStream of indices
     * @throws IllegalArgumentException if fromIndex is negative, if increment is zero, or if indexFunc is null
     */
    @Beta
    public static <AC> IntStream ofIndices(final AC source, final int fromIndex, final int increment, final ObjIntFunction<? super AC, Integer> indexFunc) {
        N.checkArgNotNegative(fromIndex, cs.fromIndex);
        N.checkArgument(increment != 0, "'increment' cannot be zero");
        N.checkArgNotNull(indexFunc, cs.indexFunc);

        if (source == null) {
            return IntStream.empty();
        }

        @SuppressWarnings("rawtypes")
        final int sourceLen = source.getClass().isArray() ? Array.getLength(source)
                : (source instanceof Collection ? ((Collection) source).size()
                        : (source instanceof CharSequence ? ((CharSequence) source).length() : Integer.MAX_VALUE));

        return ofIndices(source, fromIndex, increment, sourceLen, indexFunc);
    }

    /**
     * Creates an IntStream of indices from the given source using the provided index function.
     *
     * @param <AC> the type of the source. It should be {@code array}, {@code Collection} or {@code CharSequence}.
     * @param source the source from which indices are generated (returns empty stream if null)
     * @param fromIndex the starting index from which to generate indices
     * @param increment the increment value for generating indices (must not be zero)
     * @param sourceLen the length of the source
     * @param indexFunc the function to generate indices from the source
     * @return an IntStream of indices, or empty stream if source is null
     * @throws IllegalArgumentException if fromIndex is negative, if increment is zero, or if indexFunc is null
     */
    @Beta
    private static <AC> IntStream ofIndices(final AC source, final int fromIndex, final int increment, final int sourceLen,
            final ObjIntFunction<? super AC, Integer> indexFunc) throws IllegalArgumentException {
        N.checkArgNotNegative(fromIndex, cs.fromIndex);
        N.checkArgument(increment != 0, "'increment' cannot be zero");
        N.checkArgNotNull(indexFunc, cs.indexFunc);

        if (source == null) {
            return IntStream.empty();
        }

        final IntUnaryOperator f = idx -> ((increment > 0 && idx >= sourceLen - increment) || (increment < 0 && idx + increment < 0)) ? N.INDEX_NOT_FOUND
                : indexFunc.apply(source, idx + increment);

        return iterate(indexFunc.apply(source, fromIndex), com.landawn.abacus.util.function.IntPredicate.NOT_NEGATIVE, f);
    }

    /**
     * Creates a stream that iterates using the given <i>hasNext</i> and <i>next</i> suppliers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a stream with external iteration control
     * final AtomicInteger counter = new AtomicInteger(0);
     * IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement())
     *     .toArray();   // returns [0, 1, 2, 3, 4]
     *
     * // Read from iterator until empty
     * Iterator<Integer> iter = Arrays.asList(10, 20, 30).iterator();
     * IntStream.iterate(iter::hasNext, () -> iter.next())
     *     .sum();   // returns 60
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next an IntSupplier that provides the next int in the iteration
     * @return an IntStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>next</i> is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static IntStream iterate(final BooleanSupplier hasNext, final IntSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorIntStream(new IntIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public int nextInt() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsInt();
            }
        });
    }

    /**
     * Creates a stream that iterates from an initial value, applying a function to generate subsequent values,
     * and continues as long as the hasNext supplier returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate numbers with external control
     * final AtomicBoolean cont = new AtomicBoolean(true);
     * IntStream.iterate(0, () -> cont.get(), i -> i + 1)
     *     .limit(5)
     *     .toArray();   // returns [0, 1, 2, 3, 4]
     *
     * // Conditional generation based on external state
     * IntStream.iterate(1, () -> someCondition(), i -> i * 2)
     *     .limit(10)
     *     .toArray();
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return an IntStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, BooleanSupplier, java.util.function.UnaryOperator)
     */
    public static IntStream iterate(final int init, final BooleanSupplier hasNext, final IntUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int cur = 0;
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
            public int nextInt() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsInt(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates a stream that iterates from an initial value, applying a function to generate subsequent values,
     * and continues as long as a predicate is satisfied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate powers of 2 less than 100
     * IntStream.iterate(1, i -> i < 100, i -> i * 2)
     *     .toArray();   // returns [1, 2, 4, 8, 16, 32, 64]
     *
     * // Countdown
     * IntStream.iterate(10, i -> i >= 0, i -> i - 1)
     *     .toArray();   // returns [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0]
     *
     * // Generate multiples of 5 less than 50
     * IntStream.iterate(5, i -> i < 50, i -> i + 5)
     *     .toArray();   // returns [5, 10, 15, 20, 25, 30, 35, 40, 45]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a predicate that determines if the returned stream has next by testing hasNext.test(init) for first time and hasNext.test(f.apply(previous)) for remaining elements
     * @param f a function to apply to the previous element to generate the next element
     * @return an IntStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, java.util.function.Predicate, java.util.function.UnaryOperator)
     */
    public static IntStream iterate(final int init, final IntPredicate hasNext, final IntUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsInt(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public int nextInt() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return cur;
            }
        });
    }

    /**
     * Creates a stream that iterates from an initial value, applying a function to generate subsequent values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Infinite sequence of powers of 2
     * IntStream.iterate(1, i -> i * 2)
     *     .limit(10)
     *     .toArray();   // returns [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]
     *
     * // Natural numbers starting from 1
     * IntStream.iterate(1, i -> i + 1)
     *     .limit(5)
     *     .toArray();   // returns [1, 2, 3, 4, 5]
     *
     * // Countdown from 10
     * IntStream.iterate(10, i -> i - 1)
     *     .limit(5)
     *     .toArray();   // returns [10, 9, 8, 7, 6]
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an IntStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>f</i> is null
     * @see Stream#iterate(Object, java.util.function.UnaryOperator)
     */
    public static IntStream iterate(final int init, final IntUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsInt(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Generates an IntStream using the provided IntSupplier.
     * The supplier is used to generate each element of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate constant stream
     * IntStream.generate(() -> 42)
     *     .limit(5)
     *     .toArray();   // returns [42, 42, 42, 42, 42]
     *
     * // Generate random numbers
     * Random rand = new Random();
     * IntStream.generate(rand::nextInt)
     *     .limit(10)
     *     .toArray();
     *
     * // Generate from external source
     * final AtomicInteger counter = new AtomicInteger();
     * IntStream.generate(counter::incrementAndGet)
     *     .limit(5)
     *     .toArray();   // returns [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param s the IntSupplier that provides the elements of the stream
     * @return an IntStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static IntStream generate(final IntSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return s.getAsInt();
            }
        });
    }

    /**
     * Concatenates multiple arrays of ints into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple int arrays
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {4, 5};
     * int[] arr3 = {6, 7, 8};
     * IntStream.concat(arr1, arr2, arr3).toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * // Concatenate with empty arrays
     * IntStream.concat(new int[]{1, 2}, new int[]{}, new int[]{3}).toArray();   // returns [1, 2, 3]
     * }</pre>
     *
     * @param a the arrays of ints to concatenate
     * @return an IntStream containing all the ints from the input arrays
     * @see Stream#concat(Object[][])
     */
    public static IntStream concat(final int[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple IntIterators into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple IntIterators
     * IntIterator iter1 = IntIterator.of(1, 2, 3);
     * IntIterator iter2 = IntIterator.of(4, 5);
     * IntStream.concat(iter1, iter2).toArray();   // returns [1, 2, 3, 4, 5]
     *
     * // Concatenate from different sources
     * IntList list = IntList.of(1, 2);
     * IntIterator iter = IntIterator.of(3, 4);
     * IntStream.concat(list.iterator(), iter).sum();   // returns 10
     * }</pre>
     *
     * @param a the IntIterators to concatenate
     * @return an IntStream containing all the ints from the input IntIterators in order
     * @see Stream#concat(Iterator[])
     */
    public static IntStream concat(final IntIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple IntStreams into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple IntStreams
     * IntStream stream1 = IntStream.of(1, 2, 3);
     * IntStream stream2 = IntStream.of(4, 5);
     * IntStream stream3 = IntStream.of(6, 7);
     * IntStream.concat(stream1, stream2, stream3).toArray();   // returns [1, 2, 3, 4, 5, 6, 7]
     *
     * // Concatenate range streams
     * IntStream.concat(IntStream.range(1, 3), IntStream.range(10, 12)).toArray();   // returns [1, 2, 10, 11]
     * }</pre>
     *
     * @param a the IntStreams to concatenate
     * @return an IntStream containing all the ints from the input IntStreams in order
     * @see Stream#concat(Stream[])
     */
    public static IntStream concat(final IntStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of int arrays into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate a list of int arrays
     * List<int[]> arrays = Arrays.asList(
     *     new int[]{1, 2},
     *     new int[]{3, 4, 5},
     *     new int[]{6}
     * );
     * IntStream.concat(arrays).toArray();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Concatenate dynamically created arrays
     * List<int[]> chunks = new ArrayList<>();
     * chunks.add(new int[]{1, 2});
     * chunks.add(new int[]{3});
     * IntStream.concat(chunks).sum();   // returns 6
     * }</pre>
     *
     * @param c the list of int arrays to concatenate
     * @return an IntStream containing all the ints from the input list of int arrays
     * @see Stream#concat(Object[][])
     */
    @Beta
    public static IntStream concat(final List<int[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new IntIteratorEx() {
            private final Iterator<int[]> iter = c.iterator();
            private int[] cur;
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
            public int nextInt() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of IntStreams into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate a collection of IntStreams
     * List<IntStream> streams = Arrays.asList(
     *     IntStream.of(1, 2),
     *     IntStream.of(3, 4),
     *     IntStream.of(5)
     * );
     * IntStream.concat(streams).toArray();   // returns [1, 2, 3, 4, 5]
     *
     * // Concatenate dynamically generated streams
     * List<IntStream> rangeStreams = new ArrayList<>();
     * rangeStreams.add(IntStream.range(0, 3));
     * rangeStreams.add(IntStream.range(10, 13));
     * IntStream.concat(rangeStreams).toArray();   // returns [0, 1, 2, 10, 11, 12]
     * }</pre>
     *
     * @param streams the collection of IntStreams to concatenate
     * @return an IntStream containing all the ints from the input collection of IntStreams
     * @see Stream#concat(Collection)
     */
    public static IntStream concat(final Collection<? extends IntStream> streams) {
        return N.isEmpty(streams) ? empty() : new IteratorIntStream(new IntIteratorEx() { //NOSONAR
            private final Iterator<? extends IntStream> iterators = streams.iterator();
            private IntStream cur;
            private IntIterator iter;

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
            public int nextInt() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextInt();
            }
        }).onClose(newCloseHandler(streams));
    }

    /**
     * Concatenates a collection of IntIterators into a single IntStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate a collection of IntIterators
     * List<IntIterator> iterators = Arrays.asList(
     *     IntIterator.of(1, 2),
     *     IntIterator.of(3, 4, 5),
     *     IntIterator.of(6)
     * );
     * IntStream.concatIterators(iterators).toArray();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Concatenate from mixed sources
     * List<IntIterator> iters = new ArrayList<>();
     * iters.add(IntList.of(1, 2).iterator());
     * iters.add(IntIterator.of(3, 4));
     * IntStream.concatIterators(iters).sum();   // returns 10
     * }</pre>
     *
     * @param intIterators the collection of IntIterators to concatenate
     * @return an IntStream containing all the ints from the input collection of IntIterators
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static IntStream concatIterators(final Collection<? extends IntIterator> intIterators) {
        if (N.isEmpty(intIterators)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final Iterator<? extends IntIterator> iter = intIterators.iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextInt();
            }
        });
    }

    /**
     * Zips two int arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two int arrays using a zip function.
     *
     * <p>The operation stops when either array runs out of values. No default values are used
     * for the shorter array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a = {1, 2, 3};
     * int[] b = {4, 5};
     * IntStream.zip(a, b, (x, y) -> x + y)
     *       .toIntList();   // returns [5, 7]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(int[], int[], int, int, IntBinaryOperator)
     */
    public static IntStream zip(final int[] a, final int[] b, final IntBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsInt(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three int arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three int arrays using a zip function.
     *
     * <p>The operation stops when any array runs out of values. No default values are used
     * for the shorter arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a = {1, 2, 3};
     * int[] b = {4, 5};
     * int[] c = {7, 8};
     * IntStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *       .toIntList();   // returns [12, 15]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param c the third int array
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(int[], int[], int[], int, int, int, IntTernaryOperator)
     */
    public static IntStream zip(final int[] a, final int[] b, final int[] c, final IntTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsInt(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two int iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two int iterators using a zip function.
     *
     * <p>The operation stops when either iterator runs out of values. No default values are used
     * for the shorter iterator. Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator a = IntIterator.of(1, 2, 3);
     * IntIterator b = IntIterator.of(4, 5);
     * IntStream.zip(a, b, (x, y) -> x * y)
     *       .toIntList();   // returns [4, 10]
     * }</pre>
     *
     * @param a the first int iterator. Can be {@code null} (treated as empty)
     * @param b the second int iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntIterator, IntIterator, int, int, IntBinaryOperator)
     */
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntBinaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            private final IntIterator iterA = a == null ? IntIterator.empty() : a;
            private final IntIterator iterB = b == null ? IntIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public int nextInt() {
                return zipFunction.applyAsInt(iterA.nextInt(), iterB.nextInt());
            }
        });
    }

    /**
     * Zips three int iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three int iterators using a zip function.
     *
     * <p>The operation stops when any iterator runs out of values. No default values are used
     * for the shorter iterators. Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator a = IntIterator.of(1, 2, 3);
     * IntIterator b = IntIterator.of(4, 5);
     * IntIterator c = IntIterator.of(7, 8);
     * IntStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *       .toIntList();   // returns [12, 15]
     * }</pre>
     *
     * @param a the first int iterator. Can be {@code null} (treated as empty)
     * @param b the second int iterator. Can be {@code null} (treated as empty)
     * @param c the third int iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntIterator, IntIterator, IntIterator, int, int, int, IntTernaryOperator)
     */
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntIterator c, final IntTernaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            private final IntIterator iterA = a == null ? IntIterator.empty() : a;
            private final IntIterator iterB = b == null ? IntIterator.empty() : b;
            private final IntIterator iterC = c == null ? IntIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public int nextInt() {
                return zipFunction.applyAsInt(iterA.nextInt(), iterB.nextInt(), iterC.nextInt());
            }
        });
    }

    /**
     * Zips two int streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two int streams using a zip function.
     *
     * <p>The operation stops when either stream runs out of values. No default values are used
     * for the shorter stream. The resulting stream will automatically close both input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream a = IntStream.of(1, 2, 3);
     * IntStream b = IntStream.of(10, 20);
     * IntStream.zip(a, b, (x, y) -> x + y)
     *       .toIntList();   // returns [11, 22]
     * }</pre>
     *
     * @param a the first IntStream
     * @param b the second IntStream
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntStream, IntStream, int, int, IntBinaryOperator)
     */
    public static IntStream zip(final IntStream a, final IntStream b, final IntBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three int streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three int streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream a = IntStream.of(1, 2, 3);
     * IntStream b = IntStream.of(4, 5);
     * IntStream c = IntStream.of(7, 8);
     * IntStream.zip(a, b, c, (x, y, z) -> x * y * z)
     *       .toIntList();   // returns [28, 80]
     * }</pre>
     *
     * @param a the first IntStream
     * @param b the second IntStream
     * @param c the third IntStream
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntStream, IntStream, IntStream, int, int, int, IntTernaryOperator)
     */
    public static IntStream zip(final IntStream a, final IntStream b, final IntStream c, final IntTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple int streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from a collection of int streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The zip function receives an array containing one element from each stream.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<IntStream> streams = Arrays.asList(
     *     IntStream.of(1, 2, 3),
     *     IntStream.of(4, 5),
     *     IntStream.of(7, 8)
     * );
     * IntStream.zip(streams, values -> IntStream.of(values).sum())
     *       .toIntList();   // returns [12, 15]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Integer} which is then unboxed back to {@code int}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link IntBinaryOperator} /
     * {@link IntTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of int streams to zip
     * @param zipFunction the function to combine arrays of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is empty
     * @see #zip(Collection, int[], IntNFunction)
     */
    public static IntStream zip(final Collection<? extends IntStream> streams, final IntNFunction<Integer> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToInt(ToIntFunction.UNBOX);
    }

    /**
     * Zips two int arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two int arrays using a zip function.
     *
     * <p>The operation continues until both arrays are exhausted. When one array runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a = {1, 2, 3};
     * int[] b = {4, 5};
     * IntStream.zip(a, b, 0, 10, (x, y) -> x + y)
     *       .toIntList();   // returns [5, 7, 13]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(int[], int[], IntBinaryOperator)
     */
    public static IntStream zip(final int[] a, final int[] b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private int ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsInt(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three int arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three int arrays using a zip function.
     *
     * <p>The operation continues until all arrays are exhausted. When one array runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a = {1, 2, 3};
     * int[] b = {4, 5};
     * int[] c = {7};
     * IntStream.zip(a, b, c, 0, 10, 20, (x, y, z) -> x + y + z)
     *       .toIntList();   // returns [12, 27, 33]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param c the third int array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param valueForNoneC the default value to use when the third array runs out of values
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(int[], int[], int[], IntTernaryOperator)
     */
    public static IntStream zip(final int[] a, final int[] b, final int[] c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private int ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsInt(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two int iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two int iterators using a zip function.
     *
     * <p>The operation continues until both iterators are exhausted. When one iterator runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that iterator.
     * Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator a = IntIterator.of(1, 2, 3);
     * IntIterator b = IntIterator.of(4, 5);
     * IntStream.zip(a, b, 0, 10, (x, y) -> x + y)
     *       .toIntList();   // returns [5, 7, 13]
     * }</pre>
     *
     * @param a the first int iterator, may be {@code null} (treated as empty)
     * @param b the second int iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntIterator, IntIterator, IntBinaryOperator)
     */
    public static IntStream zip(final IntIterator a, final IntIterator b, final int valueForNoneA, final int valueForNoneB,
            final IntBinaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            private final IntIterator iterA = a == null ? IntIterator.empty() : a;
            private final IntIterator iterB = b == null ? IntIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public int nextInt() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsInt(iterA.nextInt(), iterB.hasNext() ? iterB.nextInt() : valueForNoneB);
                } else {
                    return zipFunction.applyAsInt(valueForNoneA, iterB.nextInt());
                }
            }
        });
    }

    /**
     * Zips three int iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three int iterators using a zip function.
     *
     * <p>The operation continues until all iterators are exhausted. When one iterator runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that iterator.
     * Null iterators are treated as empty iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator a = IntIterator.of(1, 2, 3);
     * IntIterator b = IntIterator.of(4, 5);
     * IntIterator c = IntIterator.of(7);
     * IntStream.zip(a, b, c, 0, 10, 20, (x, y, z) -> x + y + z)
     *       .toIntList();   // returns [12, 27, 33]
     * }</pre>
     *
     * @param a the first int iterator, may be {@code null} (treated as empty)
     * @param b the second int iterator, may be {@code null} (treated as empty)
     * @param c the third int iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param valueForNoneC the default value to use when the third iterator runs out of values
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see #zip(IntIterator, IntIterator, IntIterator, IntTernaryOperator)
     */
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntIterator c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTernaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            private final IntIterator iterA = a == null ? IntIterator.empty() : a;
            private final IntIterator iterB = b == null ? IntIterator.empty() : b;
            private final IntIterator iterC = c == null ? IntIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public int nextInt() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsInt(iterA.nextInt(), iterB.hasNext() ? iterB.nextInt() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextInt() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsInt(valueForNoneA, iterB.nextInt(), iterC.hasNext() ? iterC.nextInt() : valueForNoneC);
                } else {
                    return zipFunction.applyAsInt(valueForNoneA, valueForNoneB, iterC.nextInt());
                }
            }
        });
    }

    /**
     * Zips two int streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two int streams using a zip function.
     *
     * <p>The operation continues until both streams are exhausted. When one stream runs out of values,
     * the specified default value (valueForNoneA or valueForNoneB) is used for that stream.
     * The returned stream will handle closing of the input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream a = IntStream.of(1, 2, 3);
     * IntStream b = IntStream.of(4, 5);
     * IntStream.zip(a, b, 0, 10, (x, y) -> x + y)
     *       .toIntList();   // returns [5, 7, 13]
     * }</pre>
     *
     * @param a the first int stream
     * @param b the second int stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close the input streams when closed
     * @see #zip(IntStream, IntStream, IntBinaryOperator)
     */
    public static IntStream zip(final IntStream a, final IntStream b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three int streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three int streams using a zip function.
     *
     * <p>The operation continues until all streams are exhausted. When one stream runs out of values,
     * the specified default value (valueForNoneA, valueForNoneB, or valueForNoneC) is used for that stream.
     * The returned stream will handle closing of the input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream a = IntStream.of(1, 2, 3);
     * IntStream b = IntStream.of(4, 5);
     * IntStream c = IntStream.of(7);
     * IntStream.zip(a, b, c, 0, 10, 20, (x, y, z) -> x + y + z)
     *       .toIntList();   // returns [12, 27, 33]
     * }</pre>
     *
     * @param a the first int stream
     * @param b the second int stream
     * @param c the third int stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param valueForNoneC the default value to use when the third stream runs out of values
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close the input streams when closed
     * @see #zip(IntStream, IntStream, IntStream, IntTernaryOperator)
     */
    public static IntStream zip(final IntStream a, final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple int streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from a collection of int streams using a zip function.
     *
     * <p>The operation continues until all streams are exhausted. When one stream runs out of values,
     * the corresponding default value from valuesForNone is used for that stream.
     * The zip function receives an array containing one element from each stream.
     * The returned stream will handle closing of all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<IntStream> streams = Arrays.asList(
     *     IntStream.of(1, 2, 3),
     *     IntStream.of(4, 5),
     *     IntStream.of(7)
     * );
     * int[] defaults = {0, 10, 20};
     * IntStream.zip(streams, defaults, values -> IntStream.of(values).sum())
     *       .toIntList();   // returns [12, 27, 33]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Integer} which is then unboxed back to {@code int}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link IntBinaryOperator} /
     * {@link IntTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of int streams to zip
     * @param valuesForNone array of default values, must have same size as streams collection
     * @param zipFunction the function to combine arrays of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values that will close all input streams when closed
     * @throws IllegalArgumentException if the size of valuesForNone doesn't match the size of streams collection
     * @see #zip(Collection, IntNFunction)
     */
    public static IntStream zip(final Collection<? extends IntStream> streams, final int[] valuesForNone, final IntNFunction<Integer> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToInt(ToIntFunction.UNBOX);
    }

    /**
     * Merges two int arrays into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from the two arrays.
     *
     * <p>This method is useful for merging sorted arrays while maintaining order, or for implementing
     * custom merge strategies. The selector is called with the next available elements from both arrays.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted arrays
     * int[] array1 = {1, 3, 5};
     * int[] array2 = {2, 4, 6};
     * IntStream.merge(array1, array2,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Merge with empty array
     * IntStream.merge(new int[]{1, 2}, new int[]{},
     *     (a, b) -> MergeResult.TAKE_FIRST)
     *     .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param nextSelector a function that determines which element to select next. It receives the next elements
     *                     from both arrays and returns {@link MergeResult#TAKE_FIRST} to select from the first array
     *                     or {@link MergeResult#TAKE_SECOND} to select from the second array
     * @return a new IntStream containing the merged elements
     * @see #merge(int[], int[], int[], IntBiFunction)
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static IntStream merge(final int[] a, final int[] b, final IntBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public int nextInt() {
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
     * Merges three int arrays into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from pairs of elements.
     *
     * <p>This method first merges the first two arrays, then merges the result with the third array.
     * The selector is applied in a pairwise fashion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted arrays
     * int[] array1 = {1, 4, 7};
     * int[] array2 = {2, 5, 8};
     * int[] array3 = {3, 6, 9};
     * IntStream.merge(array1, array2, array3,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Merge with empty array
     * IntStream.merge(new int[]{1, 2}, new int[]{}, new int[]{3},
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3]
     * }</pre>
     *
     * @param a the first int array
     * @param b the second int array
     * @param c the third int array
     * @param nextSelector a function that determines which element to select next from pairs of elements
     * @return a new IntStream containing the merged elements
     * @see #merge(int[], int[], IntBiFunction)
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static IntStream merge(final int[] a, final int[] b, final int[] c, final IntBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), IntStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two IntIterators into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from the two iterators.
     *
     * <p>This method is useful for merging sorted iterators while maintaining order, or for implementing
     * custom merge strategies. The selector is called with the next available elements from both iterators.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted iterators
     * IntIterator iter1 = IntIterator.of(1, 3, 5);
     * IntIterator iter2 = IntIterator.of(2, 4, 6);
     * IntStream.merge(iter1, iter2,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Merge with null iterator (treated as empty)
     * IntStream.merge(IntIterator.of(1, 2), (IntIterator) null,
     *     (a, b) -> MergeResult.TAKE_FIRST)
     *     .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * @param a the first IntIterator (null is treated as an empty iterator)
     * @param b the second IntIterator (null is treated as an empty iterator)
     * @param nextSelector a function that determines which element to select next. It receives the next elements
     *                     from both iterators and returns {@link MergeResult#TAKE_FIRST} to select from the first iterator
     *                     or {@link MergeResult#TAKE_SECOND} to select from the second iterator
     * @return a new IntStream containing the merged elements
     * @see #merge(IntIterator, IntIterator, IntIterator, IntBiFunction)
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static IntStream merge(final IntIterator a, final IntIterator b, final IntBiFunction<MergeResult> nextSelector) {
        return new IteratorIntStream(new IntIteratorEx() {
            private final IntIterator iterA = a == null ? IntIterator.empty() : a;
            private final IntIterator iterB = b == null ? IntIterator.empty() : b;
            private int nextA = 0;
            private int nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public int nextInt() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextInt())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextInt()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextInt()), (nextB = iterB.nextInt())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextInt();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextInt();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three IntIterators into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from pairs of elements.
     *
     * <p>This method first merges the first two iterators, then merges the result with the third iterator.
     * The selector is applied in a pairwise fashion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted iterators
     * IntIterator iter1 = IntIterator.of(1, 4, 7);
     * IntIterator iter2 = IntIterator.of(2, 5, 8);
     * IntIterator iter3 = IntIterator.of(3, 6, 9);
     * IntStream.merge(iter1, iter2, iter3,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Merge with null iterator
     * IntStream.merge(IntIterator.of(1), IntIterator.of(2), (IntIterator) null,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * @param a the first IntIterator (null is treated as an empty iterator)
     * @param b the second IntIterator (null is treated as an empty iterator)
     * @param c the third IntIterator (null is treated as an empty iterator)
     * @param nextSelector a function that determines which element to select next from pairs of elements
     * @return a new IntStream containing the merged elements
     * @see #merge(IntIterator, IntIterator, IntBiFunction)
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static IntStream merge(final IntIterator a, final IntIterator b, final IntIterator c, final IntBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two IntStreams into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from the two streams.
     *
     * <p>Both input streams will be automatically closed when the returned stream is closed. This ensures
     * proper resource management when working with streams that hold resources.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams
     * IntStream stream1 = IntStream.of(1, 3, 5, 7);
     * IntStream stream2 = IntStream.of(2, 4, 6, 8);
     * IntStream.merge(stream1, stream2,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * // Merge with empty stream
     * IntStream.merge(IntStream.of(1, 2), IntStream.empty(),
     *     (a, b) -> MergeResult.TAKE_FIRST)
     *     .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * @param a the first IntStream
     * @param b the second IntStream
     * @param nextSelector a function that determines which element to select next. It receives the next elements
     *                     from both streams and returns {@link MergeResult#TAKE_FIRST} to select from the first stream
     *                     or {@link MergeResult#TAKE_SECOND} to select from the second stream
     * @return a new IntStream containing the merged elements
     * @see #merge(IntStream, IntStream, IntStream, IntBiFunction)
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static IntStream merge(final IntStream a, final IntStream b, final IntBiFunction<MergeResult> nextSelector) {
        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three IntStreams into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from pairs of elements.
     *
     * <p>All input streams will be automatically closed when the returned stream is closed. This ensures
     * proper resource management when working with streams that hold resources.</p>
     *
     * <p>This method first merges the first two streams, then merges the result with the third stream.
     * The selector is applied in a pairwise fashion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted streams
     * IntStream stream1 = IntStream.of(1, 4, 7);
     * IntStream stream2 = IntStream.of(2, 5, 8);
     * IntStream stream3 = IntStream.of(3, 6, 9);
     * IntStream.merge(stream1, stream2, stream3,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Merge with empty stream
     * IntStream.merge(IntStream.of(1), IntStream.of(2), IntStream.empty(),
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2]
     * }</pre>
     *
     * @param a the first IntStream
     * @param b the second IntStream
     * @param c the third IntStream
     * @param nextSelector a function that determines which element to select next from pairs of elements
     * @return a new IntStream containing the merged elements
     * @see #merge(IntStream, IntStream, IntBiFunction)
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static IntStream merge(final IntStream a, final IntStream b, final IntStream c, final IntBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges multiple IntStreams into a single IntStream by selecting elements based on the provided selector function.
     * The selector function determines which element to take next from pairs of elements.
     *
     * <p>All input streams will be automatically closed when the returned stream is closed. This ensures
     * proper resource management when working with streams that hold resources.</p>
     *
     * <p>The streams are merged pairwise: first two streams are merged, then the result is merged with the
     * third stream, and so on. The selector is applied at each merge step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge multiple sorted streams
     * List<IntStream> streams = Arrays.asList(
     *     IntStream.of(1, 5, 9),
     *     IntStream.of(2, 6),
     *     IntStream.of(3, 7),
     *     IntStream.of(4, 8)
     * );
     * IntStream.merge(streams,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toIntList();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Merge empty collection
     * IntStream.merge(Collections.emptyList(),
     *     (a, b) -> MergeResult.TAKE_FIRST)
     *     .toIntList();   // returns []
     * }</pre>
     *
     * @param streams the collection of IntStreams to merge
     * @param nextSelector a function that determines which element to select next from pairs of elements
     * @return a new IntStream containing the merged elements, or an empty stream if the collection is empty
     * @see #merge(IntStream, IntStream, IntBiFunction)
     * @see Stream#merge(Collection, BiFunction)
     */
    public static IntStream merge(final Collection<? extends IntStream> streams, final IntBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            return streams.iterator().next();
        } else if (streams.size() == 2) {
            final Iterator<? extends IntStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends IntStream> iter = streams.iterator();
        IntStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Abstract extension of IntStream that provides additional functionality.
     * This class is used internally by the stream framework.
     */
    public abstract static class IntStreamEx extends IntStream {
        private IntStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
