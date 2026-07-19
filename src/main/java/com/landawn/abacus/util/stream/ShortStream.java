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

import java.nio.ShortBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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
import com.landawn.abacus.util.Fn.FS;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortTriPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * A specialized stream implementation for processing sequences of primitive {@code short} values
 * (range {@code [-32768, 32767]}) with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive short-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for short data types.
 *
 * <p>ShortStream represents a sequence of short elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with short values,
 * avoiding boxing/unboxing overhead and offering short-specific utility methods for numerical computations.
 *
 * <p><b>Note on overflow:</b> Arithmetic operations on {@code short} values are subject to overflow
 * since the valid range is {@code [-32768, 32767]}. Operations such as {@code scan}, {@code reduce},
 * and {@code map} that perform arithmetic should account for this. The {@code sum()} method returns
 * an {@code int} to avoid overflow for streams of moderate size.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for short operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for short primitives, avoiding boxing overhead</li>
 *   <li><b>Numerical Operations:</b> Specialized methods for mathematical computations and statistical analysis</li>
 *   <li><b>Range Generation:</b> Built-in support for generating short ranges and sequences</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large short datasets</li>
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
 *   <li><b>Mathematical Computations:</b> Numerical analysis, statistical calculations, aggregations</li>
 *   <li><b>Data Processing:</b> Processing large datasets of short numbers</li>
 *   <li><b>Range Operations:</b> Generating sequences, loops, and iterative computations</li>
 *   <li><b>Array Processing:</b> Efficient processing of short arrays and collections</li>
 *   <li><b>Index Operations:</b> Working with array indices and position-based computations</li>
 *   <li><b>Compact Data:</b> Processing datasets requiring 16-bit integer precision</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic short stream operations
 * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
 *     .filter(s -> s > 2)          // keeps values > 2
 *     .map(s -> (short) (s * 2))   // maps each value by doubling
 *     .sum();                      // returns 24
 *
 * // Range-based operations
 * ShortStream.range((short) 1, (short) 101)  // numbers are 1 to 100
 *     .filter(s -> s % 2 == 0)               // keeps even numbers only
 *     .limit(10)                             // keeps first 10 even numbers
 *     .toArray();                            // [(short) 2, (short) 4, ...]
 *
 * // Statistical operations
 * ShortSummaryStatistics stats = ShortStream.of(scores)
 *     .filter(score -> score >= 0)   // filters valid scores
 *     .summaryStatistics();          // gets min, max, avg, count
 *
 * // Parallel processing for large datasets
 * int result = ShortStream.range((short) 1, (short) 10000)
 *     .parallel()              // uses parallel processing
 *     .filter(this::isPrime)   // filters prime numbers
 *     .mapToInt(s -> s * s)    // maps each prime to its square
 *     .sum();                  // sums the squares
 *
 * // Integration with other stream types
 * DoubleStream percentages = ShortStream.of(data)
 *     .asIntStream()                   // maps to int (widening)
 *     .mapToDouble(s -> s / 100.0)     // maps to percentages
 *     .filter(d -> d > 0.5);           // keeps values > 50%
 * }</pre>
 *
 * <p><b>Short-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(ShortPredicate)} - Filter shorts based on conditions</li>
 *   <li>{@code map(ShortUnaryOperator)} - Transform short values</li>
 *   <li>{@code reduce(ShortBinaryOperator)} - Reduce to single short value</li>
 *   <li>{@code sum()}, {@code average()}, {@code min()}, {@code max()} - Mathematical aggregations</li>
 *   <li>{@code range()}, {@code rangeClosed()} - Generate short sequences</li>
 *   <li>{@code mapToInt()}, {@code asIntStream()} - Convert to other primitive streams</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Short&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use ShortStream instead of {@code Stream<Short>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 elements)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * @see StreamBase
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see FloatStream
 * @see Stream
 * @see ShortIterator
 * @see ShortList
 * @see ShortSummaryStatistics
 * @see OptionalShort
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@LazyEvaluation
public abstract class ShortStream extends StreamBase<Short, short[], ShortPredicate, ShortConsumer, OptionalShort, IndexedShort, ShortIterator, ShortStream> {

    static final Random RAND = new SecureRandom();

    ShortStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, null, closeHandlers);
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
     * ⚠️ In a parallel stream, elements after the first unmatched element (the first element for which
     * the predicate returns {@code false}) may still be processed and included in the result if they
     * individually satisfy the predicate.<br>
     * There is no guarantee of encounter-order prefix semantics in parallel streams.
     *
     * <p>Parallel-stream behavior of these related short-circuiting operations:</p>
     * <pre>
     * ┌─────────────────┬─────────────────────────┬────────────────────────────────────────────────────────────────┐
     * │     Method      │        Boundary         │                            Warning                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ takeWhile       │ first unmatched         │ elements after it may still be processed and included if they  │
     * │                 │ (predicate false)       │ individually satisfy the predicate                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ dropWhile (+    │ first unmatched         │ elements after it may still be processed and dropped if they   │
     * │ onDrop)         │ (predicate false)       │ individually satisfy the predicate                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ skipUntil       │ first matched           │ elements after it may still be processed and skipped if they   │
     * │                 │ (predicate true)        │ do not satisfy the predicate                                   │
     * └─────────────────┴─────────────────────────┴────────────────────────────────────────────────────────────────┘
     * </pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *       .takeWhile(x -> x < 4)
     *       .toArray();   // returns [(short)1, (short)2, (short)3]
     *
     * // Empty stream returns empty
     * ShortStream.empty().takeWhile(x -> x < 4).toArray();   // returns []
     *
     * // No elements match predicate
     * ShortStream.of((short)5, (short)6, (short)7).takeWhile(x -> x < 3).toArray();   // returns []
     *
     * // All elements match predicate
     * ShortStream.of((short)1, (short)2).takeWhile(x -> x < 10).toArray();   // returns [(short)1, (short)2]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop taking elements
     * @return a new {@code ShortStream} consisting of elements from this stream until an element is encountered that doesn't match the predicate
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract ShortStream takeWhile(final ShortPredicate predicate);

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
     * ⚠️ In a parallel stream, elements after the first unmatched element (the first element for which
     * the predicate returns {@code false}) may still be processed and dropped if they individually
     * satisfy the predicate.<br>
     * In sequential streams the behavior is well-defined and deterministic; in parallel streams there
     * is no guarantee of encounter-order prefix/suffix semantics.
     *
     * <p>Parallel-stream behavior of these related short-circuiting operations:</p>
     * <pre>
     * ┌─────────────────┬─────────────────────────┬────────────────────────────────────────────────────────────────┐
     * │     Method      │        Boundary         │                            Warning                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ takeWhile       │ first unmatched         │ elements after it may still be processed and included if they  │
     * │                 │ (predicate false)       │ individually satisfy the predicate                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ dropWhile (+    │ first unmatched         │ elements after it may still be processed and dropped if they   │
     * │ onDrop)         │ (predicate false)       │ individually satisfy the predicate                             │
     * ├─────────────────┼─────────────────────────┼────────────────────────────────────────────────────────────────┤
     * │ skipUntil       │ first matched           │ elements after it may still be processed and skipped if they   │
     * │                 │ (predicate true)        │ do not satisfy the predicate                                   │
     * └─────────────────┴─────────────────────────┴────────────────────────────────────────────────────────────────┘
     * </pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 2, (short) 1)
     *       .dropWhile(x -> x < 4)
     *       .toArray();   // returns [(short) 4, (short) 5, (short) 2, (short) 1]
     *
     * // Empty stream returns empty
     * ShortStream.empty().dropWhile(x -> x < 4).toArray();   // returns []
     *
     * // All elements match predicate - all elements dropped
     * ShortStream.of((short)1, (short)2, (short)3).dropWhile(x -> x < 10).toArray();   // returns []
     *
     * // No elements match predicate - none dropped
     * ShortStream.of((short)5, (short)6).dropWhile(x -> x < 3).toArray();   // returns [(short)5, (short)6]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop dropping elements
     * @return a new stream consisting of the remaining elements of this stream after dropping elements
     *         while the given predicate returns {@code true}
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#dropWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract ShortStream dropWhile(final ShortPredicate predicate);

    /**
     * Returns a ShortStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each short element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
     *       .map(x -> (short) (x * 2))
     *       .toArray();   // returns [(short) 2, (short) 4, (short) 6, (short) 8]
     *
     * // Empty stream returns empty
     * ShortStream.empty().map(x -> (short) (x * 2)).toArray();   // returns []
     *
     * // Overflow example: (short)(30000 * 2) overflows short range
     * ShortStream.of((short) 3, (short) 30000).map(x -> (short) (x * 2)).toArray();   // returns [(short) 6, (short) -5536]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function that transforms each element from short to short
     * @return a new ShortStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream map(ShortUnaryOperator mapper);

    /**
     * Returns an IntStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each short element to an int value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert shorts to ints
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
     *       .mapToInt(s -> (int) s)
     *       .toArray();   // returns [1, 2, 3, 4]
     *
     * // Scale short values to int (avoiding overflow)
     * ShortStream.of((short) 100, (short) 200, (short) 300)
     *       .mapToInt(s -> s * 100)
     *       .toArray();   // returns [10000, 20000, 30000]
     *
     * // Empty stream returns empty int array
     * ShortStream.empty().mapToInt(s -> (int) s).toArray();   // returns []
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function that transforms each element from short to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(ShortUnaryOperator)
     * @see #mapToObj(ShortFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(ShortToIntFunction mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of applying the given function to the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert shorts to Strings
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .mapToObj(s -> "Value: " + s)
     *       .toList();   // returns ["Value: 1", "Value: 2", "Value: 3"]
     *
     * // Convert shorts to custom objects
     * ShortStream.of((short) 100, (short) 200, (short) 300)
     *       .mapToObj(s -> new Point(s, s))
     *       .toList();   // returns list of Point objects
     *
     * // Empty stream returns empty list
     * ShortStream.empty().mapToObj(s -> "x" + s).toList();   // returns []
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(ShortFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each element into a range
     * ShortStream.of((short) 2, (short) 3, (short) 4)
     *       .flatMap(s -> ShortStream.range((short) 0, s))
     *       .toArray();   // returns [(short) 0, (short) 1, (short) 0, (short) 1, (short) 2, (short) 0, (short) 1, (short) 2, (short) 3]
     *
     * // Duplicate each element
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .flatMap(s -> ShortStream.of(s, s))
     *       .toArray();   // returns [(short) 1, (short) 1, (short) 2, (short) 2, (short) 3, (short) 3]
     *
     * // Null mapper result produces empty stream for that element
     * ShortStream.of((short) 1, (short) 2).flatMap(s -> s > 1 ? ShortStream.of(s) : null).toArray();   // returns [(short) 2]
     *
     * // Empty stream returns empty
     * ShortStream.empty().flatMap(s -> ShortStream.of(s)).toArray();   // returns []
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function that transforms each element from short to ShortStream
     * @return a new {@link ShortStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#flatMap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream flatMap(ShortFunction<? extends ShortStream> mapper);

    // public abstract ShortStream flatmap(ShortFunction<ShortIterator> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(ShortFunction) flatMap} (uppercase 'M') - transforms each element into a {@link ShortStream}.</li>
     *   <li>{@link #flatmap(ShortFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Short}.</li>
     *   <li>{@link #flatMapArray(ShortFunction) flatMapArray} - transforms each element into a {@code short[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code ShortStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Short} entry is replaced with the primitive default value
     * {@code (short) 0} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 1, (short) 2)
     *            .flatmap(x -> Arrays.asList(x, (short) (x * 10)))
     *            .toArray();   // [1, 10, 2, 20]
     *
     * // null elements in a returned collection become (short) 0
     * ShortStream.of((short) 1)
     *            .flatmap(x -> Arrays.asList((Short) null, (short) 7))
     *            .toArray();   // [0, 7]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function that transforms each element from short to {@code Collection<Short>}
     * @return a new {@code ShortStream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ShortFunction)
     * @see #flatMapArray(ShortFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to ShortStream, flatmap maps to Collection<Short>, flatMapArray maps to short[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream flatmap(ShortFunction<? extends Collection<Short>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of the array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each element into an array
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .flatMapArray(s -> new short[]{s, (short) (s * 10)})
     *       .toArray();   // returns [(short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30]
     *
     * // Generate array based on element value
     * ShortStream.of((short) 2, (short) 3)
     *       .flatMapArray(s -> new short[s])
     *       .count();   // returns 5 (array of 2 zeros + array of 3 zeros)
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function that transforms each element from short to short[]
     * @return a new {@code ShortStream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ShortFunction)
     * @see #flatMapToInt(ShortFunction)
     * @see #flatMapToObj(ShortFunction)
     */
    // @ai-ignore flatMapArray/flatMap naming - intentional: flatMap maps to ShortStream, flatMapArray maps to short[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream flatMapArray(ShortFunction<short[]> mapper); //NOSONAR

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each short to a range of ints
     * ShortStream.of((short) 2, (short) 3)
     *       .flatMapToInt(s -> IntStream.range(0, s))
     *       .toArray();   // returns [0, 1, 0, 1, 2]
     *
     * // Convert and duplicate each short
     * ShortStream.of((short) 10, (short) 20)
     *       .flatMapToInt(s -> IntStream.of(s, s * 2))
     *       .toArray();   // returns [10, 20, 20, 40]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     *               which produces an IntStream of new values
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(ShortFunction<? extends IntStream> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each short to multiple strings
     * ShortStream.of((short) 1, (short) 2)
     *       .flatMapToObj(s -> Stream.of("A" + s, "B" + s))
     *       .toList();   // returns ["A1", "B1", "A2", "B2"]
     *
     * // Generate objects based on short value
     * ShortStream.of((short) 2, (short) 3)
     *       .flatMapToObj(s -> Stream.generate(() -> new Point(s, s)).limit(s))
     *       .toList();   // returns list with 2 Point(2,2) and 3 Point(3,3)
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     *               which produces a Stream of new values
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(ShortFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped collection produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each short to a list of strings
     * ShortStream.of((short) 1, (short) 2)
     *       .flatmapToObj(s -> List.of("Item" + s, "Value" + s))
     *       .toList();   // returns ["Item1", "Value1", "Item2", "Value2"]
     *
     * // Map to collections of varying sizes
     * ShortStream.of((short) 2, (short) 3)
     *       .flatmapToObj(s -> Collections.nCopies(s, "X"))
     *       .toList();   // returns ["X", "X", "X", "X", "X"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     *               which produces a Collection of new values
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(ShortFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped array produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Note:</b> This method is marked as {@code @Beta} because it provides a specialized flattening operation
     * for array-returning mappers. The API may evolve based on usage patterns and feedback. Consider using
     * {@link #flatMapToObj(ShortFunction)} with {@code Stream.of(array)} for similar functionality in production code.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each short to an array of strings
     * ShortStream.of((short) 1, (short) 2)
     *       .flatMapArrayToObj(s -> new String[]{"A" + s, "B" + s})
     *       .toList();   // returns ["A1", "B1", "A2", "B2"]
     *
     * // Create arrays of varying lengths
     * ShortStream.of((short) 2, (short) 3)
     *       .flatMapArrayToObj(s -> new Integer[s])
     *       .toList();   // returns list with 2 nulls followed by 3 nulls
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     *               which produces an array of new values
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(ShortFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream.
     * The function returns an {@code OptionalShort} for each element, and only elements that have a value present are included in the returned stream.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.
     *
     * <p><b>Note:</b> This method is marked as {@code @Beta} because it combines filtering and mapping in a single operation,
     * which is a convenience method borrowed from StreamEx. The API design may be refined based on usage patterns.
     * For production code, consider using {@code filter(predicate).map(mapper)} for more explicit operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and transform in one operation
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
     *       .mapPartial(s -> s % 2 == 0 ? OptionalShort.of((short) (s * 10)) : OptionalShort.empty())
     *       .toArray();   // returns [(short) 20, (short) 40]
     *
     * // Safe division returning only valid results
     * ShortStream.of((short) 10, (short) 0, (short) 5)
     *       .mapPartial(s -> s != 0 ? OptionalShort.of((short) (100 / s)) : OptionalShort.empty())
     *       .toArray();   // returns [(short) 10, (short) 20]
     * }</pre>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new stream containing only the mapped values that were present
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream mapPartial(ShortFunction<OptionalShort> mapper);

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
     * // Group consecutive numbers and map to range size
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11, (short) 20)
     *       .rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (last - first + 1))
     *       .toArray();   // returns [(short) 2, (short) 1, (short) 2, (short) 1]
     *
     * // Map each range to sum of first and last element
     * ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7)
     *       .rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (first + last))
     *       .toArray();   // returns [(short) 3, (short) 11, (short) 14]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param sameRange a predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element. Must be {@code non-null}.
     * @param mapper a function that maps a range (defined by its first and last element) to an output element
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if the sameRange predicate or mapper function is null
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream rangeMap(final ShortBiPredicate sameRange, final ShortBinaryOperator mapper);

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
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11)
     *       .rangeMapToObj((first, next) -> next - first == 1,
     *                      (first, last) -> first + "-" + last)
     *       .toList();   // returns ["1-2", "3-3", "10-11"]
     *
     * // Create custom range objects
     * ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6)
     *       .rangeMapToObj((first, next) -> next - first == 1,
     *                      (first, last) -> new Range(first, last))
     *       .toList();   // returns list of Range objects
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <T> the element type of the new stream
     * @param sameRange a predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element. Must be {@code non-null}.
     * @param mapper a function that maps a range (defined by its first and last element) to an output object of type T
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if the sameRange predicate or mapper function is null
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract <T> Stream<T> rangeMapToObj(final ShortBiPredicate sameRange, final ShortBiFunction<? extends T> mapper);

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
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11, (short) 20)
     *       .collapse((last, next) -> next - last == 1)
     *       .forEach(System.out::println);
     * // Output: [1, 2, 3], [10, 11], [20]
     *
     * // Group elements within a threshold
     * ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7, (short) 10)
     *       .collapse((last, next) -> next - last < 3)
     *       .map(ShortList::size)
     *       .toList();   // returns [2, 3, 1]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @return a stream of lists, each containing a sequence of consecutive elements that are collapsible with each other
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<ShortList> collapse(final ShortBiPredicate collapsible);

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
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11, (short) 20)
     *       .collapse((last, next) -> next - last == 1, (a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 6, (short) 21, (short) 20]
     *
     * // Get max of consecutive groups
     * ShortStream.of((short) 1, (short) 5, (short) 6, (short) 7, (short) 10)
     *       .collapse((last, next) -> next - last < 3, (a, b) -> (short) Math.max(a, b))
     *       .toArray();   // returns [(short) 1, (short) 7, (short) 10]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
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
    public abstract ShortStream collapse(final ShortBiPredicate collapsible, final ShortBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When the predicate returns {@code true} for the first element, last element, and next element of the current group, the next element is merged into the group.
     * The collapsible predicate takes three elements: the first and last elements of the current group, and the next element in the stream.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse based on range from first to next element
     * ShortStream.of((short) 1, (short) 2, (short) 4, (short) 7, (short) 8, (short) 9)
     *       .collapse((first, last, next) -> next - first < 5, (a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 7, (short) 24]
     *
     * // Complex grouping logic
     * ShortStream.of((short) 1, (short) 3, (short) 5, (short) 10, (short) 11)
     *       .collapse((first, last, next) -> (next - first) <= 10 && (next - last) <= 3,
     *                 (a, b) -> (short) (a + b))
     *       .toArray();   // collects groups with flexible criteria
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param collapsible a predicate that determines if the next element from this stream should be collapsed with the first and last elements of current group
     *          The collapsible predicate takes three elements: the first and last elements of current group, and the next element to check.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(TriPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream collapse(final ShortTriPredicate collapsible, final ShortBinaryOperator mergeFunction);

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
     * // Running sum (prefix sum)
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
     *       .scan((a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 1, (short) 3, (short) 6, (short) 10]
     *
     * // Running product
     * ShortStream.of((short) 2, (short) 3, (short) 4)
     *       .scan((a, b) -> (short) (a * b))
     *       .toArray();   // returns [(short) 2, (short) 6, (short) 24]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param accumulator a {@code ShortBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ShortStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream scan(final ShortBinaryOperator accumulator);

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
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .scan((short) 10, (a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 11, (short) 13, (short) 16]
     *
     * // Running product with initial multiplier
     * ShortStream.of((short) 2, (short) 3, (short) 4)
     *       .scan((short) 10, (a, b) -> (short) (a * b))
     *       .toArray();   // returns [(short) 20, (short) 60, (short) 240]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     *        It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * @param accumulator a {@code ShortBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ShortStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream scan(final short init, final ShortBinaryOperator accumulator);

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
     * // Running sum with initial value included
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .scan((short) 10, true, (a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 10, (short) 11, (short) 13, (short) 16]
     *
     * // Running sum with initial value NOT included
     * ShortStream.of((short) 1, (short) 2, (short) 3)
     *       .scan((short) 10, false, (a, b) -> (short) (a + b))
     *       .toArray();   // returns [(short) 11, (short) 13, (short) 16]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     * @param initIncluded a boolean value that determines if the initial value should be included as the first element in the returned stream.
     * @param accumulator a {@code ShortBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ShortStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream scan(final short init, final boolean initIncluded, final ShortBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)3, (short)4)
     *     .prepend((short)1, (short)2)
     *     .toArray();   // returns [(short)1, (short)2, (short)3, (short)4]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param a the elements to prepend to this stream
     * @return a new stream with the specified elements prepended
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream prepend(final short... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2)
     *     .append((short)3, (short)4)
     *     .toArray();   // returns [(short)1, (short)2, (short)3, (short)4]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param a the elements to append to this stream
     * @return a new stream with the specified elements appended
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream append(final short... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With empty stream
     * ShortStream.empty()
     *     .appendIfEmpty((short)1, (short)2)
     *     .toArray();   // returns [(short)1, (short)2]
     *
     * // With non-empty stream
     * ShortStream.of((short)10, (short)20)
     *     .appendIfEmpty((short)1, (short)2)
     *     .toArray();   // returns [(short)10, (short)20]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param a the elements to append if this stream is empty
     * @return this stream if not empty, otherwise a new stream containing the specified elements
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream appendIfEmpty(final short... a);

    /**
     * Returns a {@code ShortStream} consisting of the top n elements of this stream, according to the natural order of the elements.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9)
     *       .top(3)
     *       .toArray();   // returns the 3 largest elements: [9, 8, 5] (order not guaranteed)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param n the number of top elements to return
     * @return a new stream containing the top n elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream top(int n);

    /**
     * Returns a {@code ShortStream} consisting of the top n elements of this stream compared by the provided Comparator.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 elements using custom comparator (reverse order)
     * ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9)
     *       .top(3, Comparator.reverseOrder())
     *       .toArray();   // returns the 3 smallest elements: [1, 2, 5] (order not guaranteed)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param n the number of top elements to return
     * @param comparator a comparator to compare elements
     * @return a new stream containing the top n elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream top(final int n, Comparator<? super Short> comparator);

    /**
     * Returns a {@code ShortList} containing the elements of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list = ShortStream.of((short)1, (short)2, (short)3).toShortList();
     *
     * // Empty stream returns empty ShortList
     * ShortList emptyList = ShortStream.empty().toShortList();   // returns ShortList with size 0
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a {@code ShortList} containing the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract ShortList toShortList();

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates (according to {@link Object#equals(Object)}), an {@code IllegalStateException} is thrown when the collection operation is performed.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map short values to their string representation
     * Map<Short, String> map = ShortStream.of((short)1, (short)2, (short)3)
     *                                     .toMap(n -> n, n -> "Value: " + n);
     * // Result: {1="Value: 1", 2="Value: 2", 3="Value: 3"}
     *
     * // Map indices to values
     * Map<Long, Short> indexMap = ShortStream.of((short)10, (short)20, (short)30)
     *                                        .indexed()
     *                                        .toMap(IndexedShort::index, IndexedShort::value);
     * // Result: {0=10, 1=20, 2=30}
     *
     * // This will throw IllegalStateException due to duplicate keys
     * try {
     *     ShortStream.of((short)1, (short)2, (short)1)
     *                .toMap(n -> n % 2, n -> n);   // throws on duplicate key: 1
     * } catch (IllegalStateException e) {
     *     // Handle duplicate key
     * }
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Map} whose keys and values are the result of applying mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are found
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.ShortFunction<? extends K, E> keyMapper,
            Throwables.ShortFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates (according to {@link Object#equals(Object)}), an {@code IllegalStateException} is thrown when the collection operation is performed.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap to preserve insertion order
     * LinkedHashMap<Short, String> linkedMap = ShortStream.of((short)3, (short)1, (short)2)
     *                                                     .toMap(n -> n,
     *                                                            n -> "Value: " + n,
     *                                                            LinkedHashMap::new);
     * // Result: {3="Value: 3", 1="Value: 1", 2="Value: 2"} (insertion order preserved)
     *
     * // Create a TreeMap for sorted keys
     * TreeMap<Short, Integer> treeMap = ShortStream.of((short)5, (short)2, (short)8)
     *                                              .toMap(n -> n,
     *                                                     n -> (int)(n * 2),
     *                                                     TreeMap::new);
     * // Result: {2=4, 5=10, 8=16} (sorted by key)
     *
     * // Create a ConcurrentHashMap for thread-safe operations
     * ConcurrentHashMap<Short, Short> concurrentMap =
     *     ShortStream.of((short)1, (short)2, (short)3)
     *                .toMap(n -> n, n -> (short)(n * n), ConcurrentHashMap::new);
     * // Result: {1=1, 2=4, 3=9}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the result of applying mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are found
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.ShortFunction<? extends K, E> keyMapper,
            Throwables.ShortFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates (according to {@link Object#equals(Object)}), the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum values for duplicate keys
     * Map<String, Integer> sumMap = ShortStream.of((short)1, (short)2, (short)3, (short)4)
     *                                          .toMap(n -> n % 2 == 0 ? "even" : "odd",
     *                                                 n -> (int)n,
     *                                                 (v1, v2) -> v1 + v2);
     * // Result: {odd=4, even=6}  (1+3=4, 2+4=6)
     *
     * // Keep the first value when encountering duplicate keys
     * Map<Integer, Short> firstMap = ShortStream.of((short)10, (short)15, (short)20, (short)25)
     *                                           .toMap(n -> n / 10,
     *                                                  n -> n,
     *                                                  (v1, v2) -> v1);
     * // Result: {1=10, 2=20}  (first value wins: 10 over 15, 20 over 25)
     *
     * // Keep the maximum value for duplicate keys
     * Map<Integer, Short> maxMap = ShortStream.of((short)5, (short)2, (short)8, (short)3)
     *                                         .toMap(n -> n % 3,
     *                                                n -> n,
     *                                                (v1, v2) -> v1 > v2 ? v1 : v2);
     * // Result: {0=3, 2=8}  (max of values with same key)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a {@code Map} whose keys and values are the result of applying mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.ShortFunction<? extends K, E> keyMapper,
            Throwables.ShortFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a {@code Map} whose keys and values are the result of applying the provided mapping functions to the input elements.
     * If the mapped keys contain duplicates (according to {@link Object#equals(Object)}), the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap with insertion order and sum duplicate values
     * LinkedHashMap<String, Integer> linkedSumMap =
     *     ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *                .toMap(n -> n <= 3 ? "low" : "high",
     *                       n -> (int)n,
     *                       (v1, v2) -> v1 + v2,
     *                       LinkedHashMap::new);
     * // Result: {low=6, high=9} (insertion order preserved, 1+2+3=6, 4+5=9)
     *
     * // Create a TreeMap with sorted keys and keep maximum value
     * TreeMap<Integer, Short> sortedMaxMap =
     *     ShortStream.of((short)15, (short)8, (short)22, (short)19)
     *                .toMap(n -> n / 10,
     *                       n -> n,
     *                       (v1, v2) -> v1 > v2 ? v1 : v2,
     *                       TreeMap::new);
     * // Result: {0=8, 1=19, 2=22} (sorted by key, max values)
     *
     * // Create a ConcurrentHashMap with thread-safe merge operations
     * ConcurrentHashMap<String, Integer> concurrentMap =
     *     ShortStream.of((short)5, (short)10, (short)15, (short)20)
     *                .toMap(n -> n % 2 == 0 ? "even" : "odd",
     *                       n -> (int)n,
     *                       Integer::sum,
     *                       ConcurrentHashMap::new);
     * // Result: {odd=20, even=30} (5+15=20, 10+20=30)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the result of applying mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.ShortFunction<? extends K, E> keyMapper,
            Throwables.ShortFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function and collects the elements in each group using the specified downstream collector.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by parity and collect to list
     * Map<String, List<Short>> parityGroups =
     *     ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *                .groupTo(n -> n % 2 == 0 ? "even" : "odd",
     *                         Collectors.toList());
     * // Result: {odd=[1, 3, 5], even=[2, 4]}
     *
     * // Group by range and count elements
     * Map<String, Long> rangeCount =
     *     ShortStream.of((short)5, (short)15, (short)25, (short)35, (short)10)
     *                .groupTo(n -> n < 20 ? "low" : "high",
     *                         Collectors.counting());
     * // Result: {low=3, high=2}
     *
     * // Group by sign and calculate sum
     * Map<String, Integer> signSum =
     *     ShortStream.of((short)-5, (short)10, (short)-3, (short)7, (short)-2)
     *                .groupTo(n -> n < 0 ? "negative" : "positive",
     *                         Collectors.summingInt(Short::intValue));
     * // Result: {negative=-10, positive=17}
     *
     * // Group by range and join as string
     * Map<Integer, String> rangeStrings =
     *     ShortStream.of((short)5, (short)12, (short)8, (short)15, (short)3)
     *                .groupTo(n -> n / 10,
     *                         Collectors.mapping(String::valueOf, Collectors.joining(", ")));
     * // Result: {0="5, 8, 3", 1="12, 15"}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Map} containing the results of the group-and-reduce operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.ShortFunction<? extends K, E> keyMapper,
            final Collector<? super Short, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function and collects the elements in each group using the specified downstream collector.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group into LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, List<Short>> orderedGroups =
     *     ShortStream.of((short)3, (short)1, (short)4, (short)2, (short)5)
     *                .groupTo(n -> n % 2 == 0 ? "even" : "odd",
     *                         Collectors.toList(),
     *                         LinkedHashMap::new);
     * // Result: {odd=[3, 1, 5], even=[4, 2]} (insertion order preserved)
     *
     * // Group into TreeMap for sorted keys with counting
     * TreeMap<Integer, Long> sortedCount =
     *     ShortStream.of((short)15, (short)5, (short)25, (short)12, (short)8)
     *                .groupTo(n -> n / 10,
     *                         Collectors.counting(),
     *                         TreeMap::new);
     * // Result: {0=2, 1=2, 2=1} (sorted by key)
     *
     * // Group into ConcurrentHashMap with averaging
     * ConcurrentHashMap<String, Double> concurrentAvg =
     *     ShortStream.of((short)10, (short)20, (short)15, (short)25, (short)30)
     *                .groupTo(n -> n < 20 ? "low" : "high",
     *                         Collectors.averagingDouble(Short::doubleValue),
     *                         ConcurrentHashMap::new);
     * // Result: {low=12.5, high=25.0}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} containing the results of the group-and-reduce operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.ShortFunction<? extends K, E> keyMapper,
            final Collector<? super Short, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided identity value and
     * accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current accumulated value (starting from
     * {@code identity}) and the current stream element.
     *
     * <p>If the stream is empty, {@code identity} is returned. The {@code identity} value must be
     * an identity for the accumulator function, i.e., for all {@code t},
     * {@code accumulator.apply(identity, t) == t}.
     *
     * <p><b>Note:</b> Arithmetic on {@code short} is subject to overflow when the result exceeds the
     * range {@code [-32768, 32767]}. The caller is responsible for choosing an appropriate
     * {@code identity} value and accumulator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short sum = ShortStream.of((short)1, (short)2, (short)3).reduce((short)0, (a, b) -> (short)(a + b));   // returns 6
     *
     * // Empty stream returns identity
     * ShortStream.empty().reduce((short)0, (a, b) -> (short)(a + b));   // returns 0
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param identity the identity value for the accumulator function, and the value returned when the stream is empty
     * @param accumulator the function for combining the current accumulated value and the current stream element
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract short reduce(short identity, ShortBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value and the current stream element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Finding maximum value
     * OptionalShort max = ShortStream.of((short)5, (short)2, (short)8, (short)1)
     *                                .reduce((a, b) -> a > b ? a : b);   // returns OptionalShort[8]
     *
     * // Computing sum without identity
     * OptionalShort sum = ShortStream.of((short)1, (short)2, (short)3)
     *                                .reduce((a, b) -> (short)(a + b));   // returns OptionalShort[6]
     *
     * // Empty stream case
     * OptionalShort empty = ShortStream.empty()
     *                                  .reduce((a, b) -> (short)(a + b));   // returns OptionalShort.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param accumulator the function for combining the current reduced value and the current stream element
     * @return an OptionalShort describing the result of the reduction. If the stream is empty, an empty {@code OptionalShort} is returned.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalShort reduce(ShortBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a Collector.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into a custom container
     * List<Short> list = ShortStream.of((short)1, (short)2, (short)3)
     *                               .collect(ArrayList::new,
     *                                        (list, value) -> list.add(value),
     *                                        List::addAll);
     * // Result: [1, 2, 3]
     *
     * // Collect into StringBuilder
     * String result = ShortStream.of((short)10, (short)20, (short)30)
     *                            .collect(StringBuilder::new,
     *                                     (sb, value) -> sb.append(value).append(","),
     *                                     StringBuilder::append)
     *                            .toString();
     * // Result: "10,20,30,"
     *
     * // Collect statistics into custom object
     * class Stats {
     *     int sum = 0;
     *     int count = 0;
     *     void add(short value) { sum += value; count++; }
     *     void merge(Stats other) { sum += other.sum; count += other.count; }
     * }
     * Stats stats = ShortStream.of((short)5, (short)10, (short)15)
     *                          .collect(Stats::new,
     *                                   (s, value) -> s.add(value),
     *                                   (s1, s2) -> s1.merge(s2));
     * // Result: stats with sum=30, count=3
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <R> The type of the result
     * @param supplier a function that creates a new result container. For a parallel execution, this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a result
     * @param combiner an associative, non-interfering, stateless function for combining two values, which must be compatible with the accumulator function.
     *                It is unnecessary to specify {@code combiner} if {@code R} is a {@code Map/Collection/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     * @see BiConsumers#ofAddAll()
     * @see BiConsumers#ofPutAll()
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a Collector.
     *
     * <br />
     * Only call this method when the returned type {@code R} is one of these types: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjShortConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into ArrayList (no combiner needed)
     * List<Short> list = ShortStream.of((short)1, (short)2, (short)3, (short)4)
     *                               .collect(ArrayList::new,
     *                                        (list, value) -> list.add(value));
     * // Result: [1, 2, 3, 4]
     *
     * // Collect into HashSet
     * Set<Short> set = ShortStream.of((short)1, (short)2, (short)2, (short)3)
     *                             .collect(HashSet::new,
     *                                      (set, value) -> set.add(value));
     * // Result: [1, 2, 3]
     *
     * // Collect into StringBuilder
     * StringBuilder sb = ShortStream.of((short)5, (short)10, (short)15)
     *                               .collect(StringBuilder::new,
     *                                        (builder, value) -> builder.append(value).append(" "));
     * // Result: "5 10 15 "
     *
     * // Collect into ShortList (specialized collection)
     * ShortList shortList = ShortStream.of((short)100, (short)200, (short)300)
     *                                  .collect(ShortList::new,
     *                                           ShortList::add);
     * // Result: ShortList[100, 200, 300]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <R> The type of the result. It must be {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @param supplier a function that creates a new result container. For a parallel execution, this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a result.
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @see #collect(Supplier, ObjShortConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator);

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
     * ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *     .forEach(System.out::println);
     * // prints: 1, 2, 3, 4, 5 (each on a new line)
     *
     * // Empty stream - no action performed
     * ShortStream.empty().forEach(System.out::println);   // prints nothing
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the action
     * @param action a non-interfering action to perform on the elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws E;

    /**
     * Performs an action for each element of this stream, passing the element's index as well.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)100, (short)200, (short)300)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 100
     * // Element at index 1: 200
     * // Element at index 2: 300
     *
     * // Empty stream - no action performed
     * ShortStream.empty().forEachIndexed((index, value) -> System.out.println(index));   // prints nothing
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the action
     * @param action a non-interfering action to perform on the elements, taking both index and element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntShortConsumer<E> action) throws E;

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
     * boolean hasLarge = ShortStream.of((short)5, (short)12, (short)8)
     *                               .anyMatch(n -> n > 10);   // returns true
     *
     * // Check if any element is negative
     * boolean hasNegative = ShortStream.of((short)1, (short)2, (short)3)
     *                                  .anyMatch(n -> n < 0);   // returns false
     *
     * // Check if any element is even
     * boolean hasEven = ShortStream.of((short)1, (short)3, (short)5, (short)6)
     *                              .anyMatch(n -> n % 2 == 0);   // returns true (short-circuits at 6)
     *
     * // Empty stream always returns false
     * boolean empty = ShortStream.empty()
     *                            .anyMatch(n -> n > 0);   // returns false
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if any elements of the stream match the provided predicate, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean anyMatch(final Throwables.ShortPredicate<E> predicate) throws E;

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
     * boolean allPositive = ShortStream.of((short)1, (short)2, (short)3)
     *                                  .allMatch(n -> n > 0);   // returns true
     *
     * // Check if all elements are even
     * boolean allEven = ShortStream.of((short)2, (short)4, (short)5, (short)6)
     *                              .allMatch(n -> n % 2 == 0);   // returns false (short-circuits at 5)
     *
     * // Check if all elements are within range
     * boolean allInRange = ShortStream.of((short)10, (short)15, (short)20)
     *                                 .allMatch(n -> n >= 10 && n <= 20);   // returns true
     *
     * // Empty stream always returns true
     * boolean empty = ShortStream.empty()
     *                            .allMatch(n -> n > 100);   // returns true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either all elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean allMatch(final Throwables.ShortPredicate<E> predicate) throws E;

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
     * boolean noNegative = ShortStream.of((short)1, (short)2, (short)3)
     *                                 .noneMatch(n -> n < 0);   // returns true
     *
     * // Check if no elements are greater than 100
     * boolean noneAbove100 = ShortStream.of((short)10, (short)20, (short)150)
     *                                   .noneMatch(n -> n > 100);   // returns false (short-circuits at 150)
     *
     * // Check if no elements are even
     * boolean noEven = ShortStream.of((short)1, (short)3, (short)5, (short)7)
     *                             .noneMatch(n -> n % 2 == 0);   // returns true
     *
     * // Empty stream always returns true
     * boolean empty = ShortStream.empty()
     *                            .noneMatch(n -> n > 0);   // returns true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either no elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean noneMatch(final Throwables.ShortPredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalShort}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()}: it always returns the first element in
     * encounter order, including for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Note:</b> Consider using {@link #first()}, which is the primary method in this library; this
     * method exists for API compatibility with the standard Java Stream API naming conventions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalShort first = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
     *                                 .findFirst();   // returns OptionalShort[1]
     *
     * // Empty stream returns empty optional
     * ShortStream.empty().findFirst();   // returns OptionalShort.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an {@code OptionalShort} containing the first element of the stream, or an empty {@code OptionalShort} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.ShortPredicate)
     * @see #findAny(Throwables.ShortPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalShort findFirst() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalShort}.
     * This is a terminal operation that may short-circuit on any element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Note:</b> Consider using {@link #first()}, which is the primary method in this library; this
     * method exists for API compatibility with the standard Java Stream API naming conventions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalShort any = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
     *                              .findAny();   // returns OptionalShort[1] (in sequential stream)
     *
     * // Empty stream returns empty optional
     * ShortStream.empty().findAny();   // returns OptionalShort.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an {@code OptionalShort} containing the first element of the stream, or an empty {@code OptionalShort} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.ShortPredicate)
     * @see #findAny(Throwables.ShortPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalShort findAny() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns an {@code OptionalShort} describing the first element of this stream that matches the given predicate,
     * or an empty {@code OptionalShort} if no such element is found.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first element greater than 10
     * OptionalShort first = ShortStream.of((short)5, (short)12, (short)8, (short)15)
     *                                  .findFirst(n -> n > 10);   // returns OptionalShort[12]
     *
     * // Find first even number
     * OptionalShort firstEven = ShortStream.of((short)1, (short)3, (short)4, (short)6)
     *                                      .findFirst(n -> n % 2 == 0);   // returns OptionalShort[4]
     *
     * // No matching element
     * OptionalShort notFound = ShortStream.of((short)1, (short)2, (short)3)
     *                                     .findFirst(n -> n > 100);   // returns OptionalShort.empty()
     *
     * // Safe retrieval with default value
     * short value = ShortStream.of((short)5, (short)10, (short)15)
     *                          .findFirst(n -> n > 7)
     *                          .orElse((short)0);   // returns 10
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalShort} describing the first element that matches the predicate, or an empty {@code OptionalShort} if no such element is found
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalShort findFirst(final Throwables.ShortPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalShort} describing some element of this stream that matches the given predicate,
     * or an empty {@code OptionalShort} if no such element is found.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is free to select any element in the stream that matches the predicate.
     * This is to allow for maximal performance in parallel operations; the cost is that multiple invocations on the same source may not return the same result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any element greater than 10
     * OptionalShort any = ShortStream.of((short)5, (short)12, (short)8, (short)15)
     *                                .findAny(n -> n > 10);   // returns OptionalShort[12] or OptionalShort[15]
     *
     * // Find any even number
     * OptionalShort anyEven = ShortStream.of((short)1, (short)3, (short)4, (short)6)
     *                                    .findAny(n -> n % 2 == 0);   // returns OptionalShort[4] or OptionalShort[6]
     *
     * // No matching element
     * OptionalShort notFound = ShortStream.of((short)1, (short)2, (short)3)
     *                                     .findAny(n -> n > 100);   // returns OptionalShort.empty()
     *
     * // Useful in parallel streams for performance
     * OptionalShort parallel = ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *                                     .parallel()
     *                                     .findAny(n -> n > 2);   // returns any matching element efficiently
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalShort} describing some element that matches the predicate, or an empty {@code OptionalShort} if no such element is found
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalShort findAny(final Throwables.ShortPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalShort} describing the last element of this stream that matches the given predicate,
     * or an empty {@code OptionalShort} if no such element is found.
     *
     * <p>This is a terminal operation.
     *
     * <p>Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Note:</b> This method is marked as {@code @Beta} because it requires traversing the entire stream
     * to find the last matching element, which may have performance implications for large streams.
     * Consider using {@code reversed().findFirst(predicate)} for better performance when applicable.
     * The API may be refined based on usage patterns and performance feedback.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last element greater than 10
     * OptionalShort last = ShortStream.of((short)5, (short)12, (short)8, (short)15)
     *                                 .findLast(n -> n > 10);   // returns OptionalShort[15]
     *
     * // Find last even number
     * OptionalShort lastEven = ShortStream.of((short)1, (short)4, (short)3, (short)6, (short)5)
     *                                     .findLast(n -> n % 2 == 0);   // returns OptionalShort[6]
     *
     * // No matching element
     * OptionalShort notFound = ShortStream.of((short)1, (short)2, (short)3)
     *                                     .findLast(n -> n > 100);   // returns OptionalShort.empty()
     *
     * // Better performance with reversed stream
     * OptionalShort efficient = ShortStream.of((short)1, (short)2, (short)3, (short)4, (short)5)
     *                                      .reversed()
     *                                      .findFirst(n -> n > 2);   // returns OptionalShort[5]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param <E> the type of exception thrown by the predicate
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalShort} describing the last element that matches the predicate, or an empty {@code OptionalShort} if no such element is found
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalShort findLast(final Throwables.ShortPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalShort} describing the minimum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9).min();                   // returns OptionalShort[1]
     *
     * ShortStream.empty().min();                                                                     // returns OptionalShort.empty()
     *
     * // Safe retrieval with default value
     * short minValue = ShortStream.of((short) 10, (short) 20, (short) 30).min().orElse((short) 0);   // returns 10
     *
     * // Single element stream
     * ShortStream.of((short) 42).min();                                                              // returns OptionalShort[42]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an {@code OptionalShort} containing the minimum element of this stream, or an empty {@code OptionalShort} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalShort min();

    /**
     * Returns an {@code OptionalShort} describing the maximum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9).max();                   // returns OptionalShort[9]
     *
     * ShortStream.empty().max();                                                                     // returns OptionalShort.empty()
     *
     * // Safe retrieval with default value
     * short maxValue = ShortStream.of((short) 10, (short) 20, (short) 30).max().orElse((short) 0);   // returns 30
     *
     * // Single element stream
     * ShortStream.of((short) 42).max();                                                              // returns OptionalShort[42]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an {@code OptionalShort} containing the maximum element of this stream, or an empty {@code OptionalShort} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalShort max();

    /**
     * Returns the <i>k-th</i> largest element in the stream using natural (numeric) ordering.
     * If the stream is empty or the count of elements is less than {@code k}, an empty {@code OptionalShort} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * ShortStream.of((short) 10, (short) 30, (short) 20, (short) 50, (short) 40).kthLargest(2);   // returns OptionalShort[40]
     *
     * // Find the largest element (same as max)
     * ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1).kthLargest(1);   // returns OptionalShort[8]
     *
     * // When k exceeds stream size
     * ShortStream.of((short) 1, (short) 2, (short) 3).kthLargest(5);   // returns OptionalShort.empty()
     *
     * // kthLargest(1) on single element
     * ShortStream.of((short) 42).kthLargest(1);   // returns OptionalShort[42]
     *
     * // Non-positive k throws IllegalArgumentException
     * ShortStream.of((short) 1, (short) 2).kthLargest(0);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param k the position (1-based) of the largest element to retrieve; must be positive
     * @return an {@code OptionalShort} containing the k-th largest element, or an empty {@code OptionalShort} if the stream is empty or contains fewer than {@code k} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalShort kthLargest(int k);

    /**
     * Returns the sum of elements in this stream as an {@code int}.
     * This is a special case of a reduction.
     *
     * <p>This is a terminal operation.
     *
     * <p>Each {@code short} element is widened and accumulated in a {@code long} before the
     * total is returned as an {@code int}, avoiding overflow during accumulation. Note that the
     * individual {@code short} values are in the range {@code [-32768, 32767]}, so only very
     * large streams can overflow the {@code int} result; if the accumulated total is outside the
     * {@code int} range, an {@code ArithmeticException} is thrown. Returns {@code 0} if the stream is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int total = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sum();   // returns 15
     *
     * ShortStream.empty().sum();                                                                 // returns 0
     *
     * // Sum of short array
     * short[] data = {100, 200, 300, 400};
     * int sum = ShortStream.of(data).sum();   // returns 1000
     *
     * // Overflow note: sum returns int to avoid short overflow
     * ShortStream.range((short) 1, (short) 1001).sum();   // returns 500500 (int, no overflow)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return the sum of elements in this stream as an {@code int}, or {@code 0} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws ArithmeticException if the accumulated total is outside the {@code int} range
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
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).average();   // returns OptionalDouble[3.0]
     *
     * ShortStream.of((short) 100, (short) 200, (short) 300).average();                   // returns OptionalDouble[200.0]
     *
     * ShortStream.empty().average();                                                     // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double avg = ShortStream.of((short) 1000, (short) 500).average().orElse(0.0);      // returns 750.0
     *
     * // Single element stream
     * ShortStream.of((short) 42).average();                                              // returns OptionalDouble[42.0]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an OptionalDouble containing the average of the elements of this stream,
     *         or an empty optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble average();

    /**
     * Returns statistics about the elements of this stream.
     * The statistics include count, sum, min, max, and average.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = ShortStream.of((short) 10, (short) 20, (short) 30, (short) 40, (short) 50).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // prints Count: 5
     * System.out.println("Sum: " + stats.getSum());           // prints Sum: 150
     * System.out.println("Min: " + stats.getMin());           // prints Min: 10
     * System.out.println("Max: " + stats.getMax());           // prints Max: 50
     * System.out.println("Average: " + stats.getAverage());   // prints Average: 30.0
     *
     * // Empty stream statistics
     * ShortSummaryStatistics emptyStats = ShortStream.empty().summaryStatistics();
     * emptyStats.getCount();   // returns 0
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return a {@code ShortSummaryStatistics} describing various summary data about the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract ShortSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of ShortSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result =
     *     ShortStream.of((short) 10, (short) 20, (short) 30, (short) 40, (short) 50).summaryStatisticsAndPercentiles();
     * ShortSummaryStatistics stats = result.left();
     * Optional<Map<Percentage, Short>> percentiles = result.right();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a {@code Pair} containing summary statistics and a map of percentile values
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream, selecting elements based on the provided selector function.
     * The selector function determines which element to select when both streams have elements available.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)3, (short)5)
     *     .mergeWith(ShortStream.of((short)2, (short)4, (short)6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Merging with empty stream returns original
     * ShortStream.of((short)1, (short)2)
     *     .mergeWith(ShortStream.empty(), (a, b) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [1, 2]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param b the stream to merge with
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return the merged stream
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ShortStream mergeWith(final ShortStream b, final ShortBiFunction<MergeResult> nextSelector);

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
     * ShortStream.of((short)1, (short)2, (short)3)
     *            .zipWith(ShortStream.of((short)10, (short)20, (short)30, (short)40),
     *                     (a, b) -> (short)(a + b))
     *            .toArray();   // returns [(short)11, (short)22, (short)33]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param b the ShortStream to be combined with the current ShortStream. Must be {@code non-null}.
     * @param zipFunction a ShortBinaryOperator that determines the combination of elements in the combined ShortStream. Must be {@code non-null}.
     * @return a new ShortStream that is the result of combining the current ShortStream with the given ShortStream
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(ShortStream, short, short, ShortBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream zipWith(ShortStream b, ShortBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2, (short)3)
     *            .zipWith(ShortStream.of((short)10, (short)20), ShortStream.of((short)100, (short)200),
     *                     (a, b, c) -> (short)(a + b + c))
     *            .toArray();   // returns [(short)111, (short)222]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param b the second ShortStream to be combined with the current ShortStream. Will be closed along with this ShortStream.
     * @param c the third ShortStream to be combined with the current ShortStream. Will be closed along with this ShortStream.
     * @param zipFunction a ShortTernaryOperator that determines the combination of elements in the combined ShortStream. Must be {@code non-null}.
     * @return a new ShortStream that is the result of combining the current ShortStream with the given ShortStreams
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(ShortStream, ShortStream, short, short, short, ShortTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream zipWith(ShortStream b, ShortStream c, ShortTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2, (short)3)
     *            .zipWith(ShortStream.of((short)10), (short)0, (short)0, (a, b) -> (short)(a + b))
     *            .toArray();   // returns [(short)11, (short)2, (short)3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param b the ShortStream to be combined with the current ShortStream. Will be closed along with this ShortStream.
     * @param valueForNoneA the default value to use for the current ShortStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given ShortStream when it runs out of elements
     * @param zipFunction a ShortBinaryOperator that determines the combination of elements in the combined ShortStream. Must be {@code non-null}.
     * @return a new ShortStream that is the result of combining the current ShortStream with the given ShortStream
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream zipWith(ShortStream b, short valueForNoneA, short valueForNoneB, ShortBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2, (short)3)
     *            .zipWith(ShortStream.of((short)10), ShortStream.of((short)100),
     *                     (short)0, (short)0, (short)0,
     *                     (a, b, c) -> (short)(a + b + c))
     *            .toArray();   // returns [(short)111, (short)2, (short)3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @param b the second ShortStream to be combined with the current ShortStream. Will be closed along with this ShortStream.
     * @param c the third ShortStream to be combined with the current ShortStream. Will be closed along with this ShortStream.
     * @param valueForNoneA the default value to use for the current ShortStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second ShortStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third ShortStream when it runs out of elements
     * @param zipFunction a ShortTernaryOperator that determines the combination of elements in the combined ShortStream. Must be {@code non-null}.
     * @return a new ShortStream that is the result of combining the current ShortStream with the given ShortStreams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ShortStream zipWith(ShortStream b, ShortStream c, short valueForNoneA, short valueForNoneB, short valueForNoneC,
            ShortTernaryOperator zipFunction);

    /**
     * Returns an {@code IntStream} consisting of the elements of this stream, each widened to
     * {@code int} via sign extension. Negative {@code short} values (in the range
     * {@code [-32768, -1]}) will produce the corresponding negative {@code int} values.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream.of((short)1, (short)2, (short)3).asIntStream().toArray();   // returns [1, 2, 3]
     *
     * // Negative values are sign-extended
     * ShortStream.of((short)-1, (short)0, (short)32767).asIntStream().toArray();   // returns [-1, 0, 32767]
     *
     * // Empty stream returns empty IntStream
     * ShortStream.empty().asIntStream().toArray();   // returns []
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return an {@code IntStream} consisting of the elements of this stream, widened to int
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream asIntStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Short.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert primitive short stream to Stream<Short>
     * Stream<Short> boxed = ShortStream.of((short)1, (short)2, (short)3)
     *                                  .boxed();
     * // Result: Stream<Short> containing [1, 2, 3]
     *
     * // Collect to List<Short>
     * List<Short> list = ShortStream.of((short)10, (short)20, (short)30)
     *                               .boxed()
     *                               .collect(Collectors.toList());
     * // Result: [10, 20, 30]
     *
     * // Use Stream operations requiring Object types
     * Map<Short, String> map = ShortStream.of((short)1, (short)2, (short)3)
     *                                     .boxed()
     *                                     .collect(Collectors.toMap(
     *                                         n -> n,
     *                                         n -> "Value: " + n));
     * // Result: {1="Value: 1", 2="Value: 2", 3="Value: 3"}
     *
     * // Chain with other Stream operations
     * Optional<Short> max = ShortStream.of((short)5, (short)2, (short)8, (short)1)
     *                                  .boxed()
     *                                  .max(Short::compare);
     * // Result: Optional[8]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Meaning when Yes</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Does not require all elements to be buffered before producing results.</td></tr>
     * </table>
     *
     * @return a Stream consisting of the elements of this stream, each boxed to a Short
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Short> boxed();

    abstract ShortIteratorEx iteratorEx();

    // private static final ShortStream EMPTY_STREAM = new ArrayShortStream(N.EMPTY_SHORT_ARRAY, true, null);

    /**
     * Returns an empty sequential ShortStream with no elements.
     *
     * <p>This is a static factory method that creates a stream with zero elements. The returned stream
     * is useful as a base case in stream operations, conditional stream creation, or when no data is available.
     * All terminal operations on an empty stream will return empty results or default values (e.g., count returns 0, sum returns 0).
     *
     * <p>The empty stream has no performance overhead and can be safely used as a placeholder or default return value.
     * It supports all stream operations, but intermediate operations will simply return another empty stream,
     * and terminal operations will produce empty or default results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use as default return value
     * public ShortStream getNumbers(boolean hasData) {
     *     return hasData ? ShortStream.of(data) : ShortStream.empty();
     * }
     *
     * // Terminal operations on empty stream
     * ShortStream.empty().count();     // returns 0
     * ShortStream.empty().sum();       // returns 0
     * ShortStream.empty().min();       // returns OptionalShort.empty()
     * ShortStream.empty().max();       // returns OptionalShort.empty()
     * ShortStream.empty().average();   // returns OptionalDouble.empty()
     *
     * // Intermediate operations return empty stream
     * ShortStream.empty()
     *     .filter(x -> x > 10)
     *     .map(x -> (short) (x * 2))
     *     .toArray();   // returns empty array: []
     *
     * // Combining with non-empty streams
     * ShortStream result = condition
     *     ? ShortStream.of((short) 1, (short) 2, (short) 3)
     *     : ShortStream.empty();
     *
     * // Use in conditional concatenation
     * ShortStream combined = ShortStream.concat(
     *     primaryData.length > 0 ? ShortStream.of(primaryData) : ShortStream.empty(),
     *     fallbackData.length > 0 ? ShortStream.of(fallbackData) : ShortStream.empty()
     * );
     *
     * // Predicate operations always return true/false for empty streams
     * ShortStream.empty().allMatch(x -> x > 0);    // returns true (vacuously true)
     * ShortStream.empty().anyMatch(x -> x > 0);    // returns false
     * ShortStream.empty().noneMatch(x -> x > 0);   // returns true
     * }</pre>
     *
     * @return an empty sequential ShortStream with no elements
     * @see #of(short...)
     * @see #ofNullable(Short)
     */
    public static ShortStream empty() {
        return new ArrayShortStream(N.EMPTY_SHORT_ARRAY, true, null);
    }

    /**
     * Returns a {@code ShortStream} that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Defer expensive stream creation
     * ShortStream stream = ShortStream.defer(() -> {
     *     System.out.println("Stream created!");   // prints when stream is consumed or closed
     *     return ShortStream.range((short) 1, (short) 10);
     * });
     * // Stream not yet created here unless closed
     * stream.forEach(System.out::println);   // "Stream created!" is printed, then numbers 1-9
     *
     * // Conditional stream creation
     * ShortStream conditional = ShortStream.defer(() -> {
     *     if (someCondition()) {
     *         return ShortStream.of((short) 1, (short) 2, (short) 3);
     *     } else {
     *         return ShortStream.empty();
     *     }
     * });
     * }</pre>
     *
     * <p><b>Implementation Note:</b> it's equivalent to {@code Stream.just(supplier).flatMapToShort(it -> it.get())}.
     *
     * @param supplier the supplier that provides the ShortStream
     * @return a new ShortStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static ShortStream defer(final Supplier<ShortStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<ShortStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToShort(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Returns a stream containing a single element if the provided value is {@code non-null}, otherwise returns an empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With non-null value
     * ShortStream.ofNullable((short) 42)
     *     .forEach(System.out::println);   // prints: 42
     *
     * // With null value
     * ShortStream.ofNullable(null)
     *     .forEach(System.out::println);   // prints nothing (empty stream)
     *
     * // Useful for optional values
     * Short value = getValue();                             // returns null possibly
     * long count = ShortStream.ofNullable(value).count();   // 0 if null, 1 if non-null
     *
     * // Non-null Short boxing
     * ShortStream.ofNullable(Short.valueOf((short) 5))
     *     .toArray();   // returns [5]
     * }</pre>
     *
     * @param e the element to create a stream from, may be null
     * @return a stream containing the element if {@code non-null}, otherwise an empty stream
     */
    public static ShortStream ofNullable(final Short e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Returns a stream whose elements are the specified values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from varargs
     * ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
     *     .sum();   // returns 15
     *
     * // Create stream from array
     * short[] numbers = {10, 20, 30};
     * ShortStream.of(numbers)
     *     .average();   // returns OptionalDouble[20.0]
     *
     * // Empty array returns empty stream
     * ShortStream.of()
     *     .count();   // returns 0
     *
     * // Single element stream
     * ShortStream.of((short) 42).findFirst();   // returns OptionalShort[42]
     * }</pre>
     *
     * @param a the elements of the new stream
     * @return a new stream
     */
    public static ShortStream of(final short... a) {
        return N.isEmpty(a) ? empty() : new ArrayShortStream(a);
    }

    /**
     * Returns a stream whose elements are the specified values from the array within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] numbers = {10, 20, 30, 40, 50};
     *
     * // Get elements from index 1 to 3 (exclusive)
     * ShortStream.of(numbers, 1, 3)
     *     .toArray();   // returns [20, 30]
     *
     * // Get first three elements
     * ShortStream.of(numbers, 0, 3)
     *     .sum();   // returns 60
     *
     * // Get last two elements
     * ShortStream.of(numbers, 3, 5)
     *     .toArray();   // returns [40, 50]
     *
     * // Empty range returns empty stream
     * ShortStream.of(numbers, 2, 2).count();   // returns 0
     *
     * // Throws IndexOutOfBoundsException for invalid range
     * ShortStream.of(numbers, -1, 3);   // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param a the array containing the elements
     * @param fromIndex the starting index, inclusive
     * @param toIndex the ending index, exclusive
     * @return a ShortStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     */
    public static ShortStream of(final short[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayShortStream(a, fromIndex, toIndex);
    }

    /**
     * Returns a stream whose elements are the unboxed values from the specified Short array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from boxed Short array
     * Short[] boxed = {Short.valueOf((short) 1), Short.valueOf((short) 2), Short.valueOf((short) 3)};
     * ShortStream.of(boxed)
     *     .sum();   // returns 6
     *
     * // Empty array returns empty stream
     * ShortStream.of(new Short[0]).count();   // returns 0
     *
     * // Null elements in the array are unboxed to (short) 0
     * Short[] withNull = {Short.valueOf((short) 10), null, Short.valueOf((short) 20)};
     * ShortStream.of(withNull).sum();   // returns 30
     * }</pre>
     *
     * @param a the array of Short objects ({@code null} elements are unboxed to {@code (short) 0})
     * @return a new stream
     */
    public static ShortStream of(final Short[] a) {
        return Stream.of(a).mapToShort(FS.unbox());
    }

    /**
     * Returns a stream whose elements are the unboxed values from the specified Short array within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Short[] boxed = {Short.valueOf((short) 10), Short.valueOf((short) 20),
     *                  Short.valueOf((short) 30), Short.valueOf((short) 40)};
     *
     * // Get elements from index 1 to 3
     * ShortStream.of(boxed, 1, 3)
     *     .toArray();   // returns [20, 30]
     *
     * // Get first two elements
     * ShortStream.of(boxed, 0, 2)
     *     .average();   // returns OptionalDouble[15.0]
     *
     * // Throws IndexOutOfBoundsException for invalid range
     * ShortStream.of(boxed, -1, 2);   // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param a the array of Short objects ({@code null} elements in the range are unboxed to {@code (short) 0})
     * @param fromIndex the starting index, inclusive
     * @param toIndex the ending index, exclusive
     * @return a new stream
     * @throws IndexOutOfBoundsException if fromIndex is negative, toIndex is less than fromIndex, or toIndex is greater than the array length
     */
    public static ShortStream of(final Short[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToShort(FS.unbox());
    }

    /**
     * Returns a stream whose elements are the unboxed values from the specified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from List
     * List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
     * ShortStream.of(list)
     *     .sum();   // returns 6
     *
     * // Create stream from Set
     * Set<Short> set = new HashSet<>(Arrays.asList((short) 10, (short) 20, (short) 30));
     * ShortStream.of(set)
     *     .average();   // returns OptionalDouble[20.0]
     *
     * // Null collection returns empty stream
     * ShortStream.of((Collection<Short>) null).count();   // returns 0
     * }</pre>
     *
     * @param c the collection of Short objects ({@code null} elements are unboxed to {@code (short) 0})
     * @return a new stream
     */
    public static ShortStream of(final Collection<Short> c) {
        return Stream.of(c).mapToShort(FS.unbox());
    }

    /**
     * Returns a stream whose elements are provided by the specified iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from iterator
     * ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
     * ShortIterator iterator = list.iterator();
     * ShortStream.of(iterator)
     *     .sum();   // returns 6
     *
     * // Null iterator returns empty stream
     * ShortStream.of((ShortIterator) null)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param iterator the iterator providing the elements
     * @return a new stream
     */
    public static ShortStream of(final ShortIterator iterator) {
        return iterator == null ? empty() : new IteratorShortStream(iterator);
    }

    /**
     * Returns a stream whose elements are the values from the specified {@link ShortBuffer},
     * reading from the buffer's current {@link ShortBuffer#position() position} (inclusive)
     * to its {@link ShortBuffer#limit() limit} (exclusive). Returns an empty stream if
     * {@code buf} is {@code null}.
     *
     * <p>The buffer's position is <b>not</b> advanced by stream consumption — the stream
     * reads via absolute indexed {@code get(int)} access, so the buffer remains usable
     * afterwards.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create buffer and stream from it
     * ShortBuffer buffer = ShortBuffer.wrap(new short[]{(short) 1, (short) 2, (short) 3});
     * ShortStream.of(buffer)
     *     .sum();   // returns 6
     *
     * // Stream respects buffer position
     * buffer.position(1);   // leaves out first element
     * ShortStream.of(buffer)
     *     .toArray();   // returns [2, 3]
     *
     * // Null buffer returns empty stream
     * ShortStream.of((ShortBuffer) null)
     *     .count();   // returns 0
     *
     * // Empty buffer returns empty stream
     * ShortStream.of(ShortBuffer.allocate(0)).count();   // returns 0
     * }</pre>
     *
     * @param buf the ShortBuffer providing the elements (may be {@code null})
     * @return a ShortStream over elements in {@code buf} from {@code position} (inclusive)
     *         to {@code limit} (exclusive), or an empty stream if {@code buf} is {@code null}
     */
    public static ShortStream of(final ShortBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToShort(buf::get);
    }

    private static final Function<short[], ShortStream> flatMapper = ShortStream::of;

    private static final Function<short[][], ShortStream> flattMapper = ShortStream::flatten;

    /**
     * Returns a stream whose elements are all the elements of the first array,
     * followed by all the elements of the second array, and so on.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten 2D array
     * short[][] matrix = {{1, 2, 3}, {4, 5}, {6, 7, 8, 9}};
     * ShortStream.flatten(matrix)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Sum all elements
     * ShortStream.flatten(matrix)
     *     .sum();   // returns 45
     *
     * // Empty or null arrays
     * ShortStream.flatten(new short[0][])
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @return a new stream containing all elements from the input arrays
     */
    public static ShortStream flatten(final short[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToShort(flatMapper);
    }

    /**
     * Returns a stream whose elements are from a two-dimensional array,
     * read either horizontally (row by row) or vertically (column by column).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[][] matrix = {
     *     {1, 2, 3},
     *     {4, 5, 6},
     *     {7, 8, 9}
     * };
     *
     * // Read horizontally (row by row)
     * ShortStream.flatten(matrix, false)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Read vertically (column by column)
     * ShortStream.flatten(matrix, true)
     *     .toArray();   // returns [1, 4, 7, 2, 5, 8, 3, 6, 9]
     *
     * // Jagged array (different row lengths) read vertically
     * short[][] jagged = {{1, 2, 3}, {4, 5}, {6}};
     * ShortStream.flatten(jagged, true)
     *     .toArray();   // returns [1, 4, 6, 2, 5, 3]
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param vertically if {@code true}, reads elements column by column; if {@code false}, reads row by row
     * @return a new stream containing all elements from the input array
     */
    public static ShortStream flatten(final short[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToShort(flatMapper);
        }

        long n = 0;

        for (final short[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final ShortIterator iter = new ShortIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public short nextShort() {
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
     * Returns a stream whose elements are from a two-dimensional array,
     * read either horizontally (row by row) or vertically (column by column).
     * If arrays have different lengths, missing elements are filled with the specified value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Jagged array with alignment value
     * short[][] jagged = {{1, 2, 3}, {4, 5}, {6}};
     *
     * // Read horizontally with 0 padding
     * ShortStream.flatten(jagged, (short) 0, false)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 0, 6, 0, 0]
     *
     * // Read vertically with -1 padding
     * ShortStream.flatten(jagged, (short) -1, true)
     *     .toArray();   // returns [1, 4, 6, 2, 5, -1, 3, -1, -1]
     *
     * // Use alignment to create rectangular matrix
     * short[][] uneven = {{10, 20}, {30, 40, 50, 60}};
     * ShortStream.flatten(uneven, (short) 99, false)
     *     .toArray();   // returns [10, 20, 99, 99, 30, 40, 50, 60]
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param valueForAlignment the element to append so each row/column has the same number of elements
     * @param vertically if {@code true}, reads elements column by column; if {@code false}, reads row by row
     * @return a new stream containing all elements from the input array
     */
    public static ShortStream flatten(final short[][] a, final short valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final short[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        ShortIterator iter = null;

        if (vertically) {
            iter = new ShortIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public short nextShort() {
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
            iter = new ShortIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public short nextShort() {
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
     * Flattens a three-dimensional short array into a ShortStream.
     * Each element in the resulting stream is a short value from the innermost arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten 3D array
     * short[][][] cube = {
     *     {{1, 2}, {3, 4}},
     *     {{5, 6}, {7, 8}}
     * };
     * ShortStream.flatten(cube)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * // Sum all elements in 3D array
     * ShortStream.flatten(cube)
     *     .sum();   // returns 36
     * }</pre>
     *
     * @param a the three-dimensional short array to flatten
     * @return a ShortStream containing all short values from the input array
     */
    public static ShortStream flatten(final short[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToShort(flattMapper);
    }

    /**
     * Returns a ShortStream from startInclusive (inclusive) to endExclusive (exclusive) by an incremental step of 1.
     * If startInclusive is greater than or equal to endExclusive, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate range from 1 to 10 (exclusive)
     * ShortStream.range((short) 1, (short) 10)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Sum of numbers 1 to 100
     * ShortStream.range((short) 1, (short) 101)
     *     .sum();   // returns 5050
     *
     * // Empty range (start >= end)
     * ShortStream.range((short) 10, (short) 5)
     *     .count();   // returns 0
     *
     * // Single element not included (start == end)
     * ShortStream.range((short) 5, (short) 5).count();   // returns 0
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @return a sequential ShortStream for the range of short elements
     */
    public static ShortStream range(final short startInclusive, final short endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
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

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += (short) n;
            }

            @Override
            public long count() {
                final int ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ShortStream from startInclusive (inclusive) to endExclusive (exclusive) by the specified incremental step.
     * An empty stream is returned if startInclusive equals endExclusive, or if the step direction doesn't match the range direction (e.g., a positive step with startInclusive greater than endExclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Even numbers from 0 to 10
     * ShortStream.range((short) 0, (short) 10, (short) 2)
     *     .toArray();   // returns [0, 2, 4, 6, 8]
     *
     * // Countdown from 10 to 1 (exclusive)
     * ShortStream.range((short) 10, (short) 0, (short) -2)
     *     .toArray();   // returns [10, 8, 6, 4, 2]
     *
     * // Step of 5
     * ShortStream.range((short) 0, (short) 20, (short) 5)
     *     .toArray();   // returns [0, 5, 10, 15]
     *
     * // Zero step throws IllegalArgumentException
     * ShortStream.range((short) 1, (short) 10, (short) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @param by the amount to increment by at each step (can be negative)
     * @return a sequential ShortStream for the range of short elements
     * @throws IllegalArgumentException if by is zero
     */
    public static ShortStream range(final short startInclusive, final short endExclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final short result = next;
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

                cnt -= (int) n;
                next += (short) (n * by);
            }

            @Override
            public long count() {
                final int ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ShortStream from startInclusive (inclusive) to endInclusive (inclusive) by an incremental step of 1.
     * If startInclusive is greater than endInclusive, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate range from 1 to 10 (inclusive)
     * ShortStream.rangeClosed((short) 1, (short) 10)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     *
     * // Sum of numbers 1 to 100 (inclusive)
     * ShortStream.rangeClosed((short) 1, (short) 100)
     *     .sum();   // returns 5050
     *
     * // Single element range
     * ShortStream.rangeClosed((short) 5, (short) 5)
     *     .toArray();   // returns [5]
     *
     * // Empty range (start > end)
     * ShortStream.rangeClosed((short) 10, (short) 5).count();   // returns 0
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @return a sequential ShortStream for the range of short elements
     */
    public static ShortStream rangeClosed(final short startInclusive, final short endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
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

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += (short) n;
            }

            @Override
            public long count() {
                final int ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ShortStream from startInclusive (inclusive) to endInclusive (inclusive) by the specified incremental step.
     * If startInclusive equals endInclusive, a stream containing only that value is returned.
     * If the step direction doesn't match the range direction, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Even numbers from 0 to 10 (inclusive)
     * ShortStream.rangeClosed((short) 0, (short) 10, (short) 2)
     *     .toArray();   // returns [0, 2, 4, 6, 8, 10]
     *
     * // Countdown from 10 to 0 (inclusive)
     * ShortStream.rangeClosed((short) 10, (short) 0, (short) -2)
     *     .toArray();   // returns [10, 8, 6, 4, 2, 0]
     *
     * // Step of 5 from 0 to 20 (inclusive)
     * ShortStream.rangeClosed((short) 0, (short) 20, (short) 5)
     *     .toArray();   // returns [0, 5, 10, 15, 20]
     *
     * // Zero step throws IllegalArgumentException
     * ShortStream.rangeClosed((short) 1, (short) 10, (short) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @param by the amount to increment by at each step (can be negative)
     * @return a sequential ShortStream for the range of short elements
     * @throws IllegalArgumentException if by is zero
     */
    public static ShortStream rangeClosed(final short startInclusive, final short endInclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = (endInclusive - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final short result = next;
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

                cnt -= (int) n;
                next += (short) (n * by);
            }

            @Override
            public long count() {
                final int ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a sequential ShortStream with the specified element repeated {@code n} times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Repeat element 5 times
     * ShortStream.repeat((short) 42, 5)
     *     .toArray();   // returns [42, 42, 42, 42, 42]
     *
     * // Create array filled with zeros
     * ShortStream.repeat((short) 0, 10)
     *     .toArray();   // returns [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
     *
     * // Use in computation
     * ShortStream.repeat((short) 3, 100)
     *     .sum();   // returns 300
     *
     * // Zero count returns empty stream
     * ShortStream.repeat((short) 5, 0).count();   // returns 0
     *
     * // Negative count throws IllegalArgumentException
     * ShortStream.repeat((short) 5, -1);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param element the element to be repeated
     * @param n the number of times to repeat the element
     * @return a ShortStream consisting of n copies of the specified element
     * @throws IllegalArgumentException if n is negative
     */
    public static ShortStream repeat(final short element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final short[] result = new short[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns an infinite sequential unordered stream where each element is generated randomly.
     * The random values are uniformly distributed across the full range of short values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random shorts
     * ShortStream.random()
     *     .limit(10)
     *     .toArray();   // returns 10 random shorts
     *
     * // Generate random shorts in a range
     * ShortStream.random()
     *     .map(s -> (short) Math.abs(s % 100))  // 0-99
     *     .limit(5)
     *     .toArray();   // returns 5 random shorts between 0 and 99
     *
     * // Generate until condition met
     * OptionalShort first = ShortStream.random()
     *     .filter(s -> s > 30000)
     *     .findFirst();   // returns first random short > 30000
     * }</pre>
     *
     * @return an infinite ShortStream of random short values
     */
    public static ShortStream random() {
        final int bound = Short.MAX_VALUE - Short.MIN_VALUE + 1;

        return generate(() -> (short) (RAND.nextInt(bound) + Short.MIN_VALUE));
    }

    /**
     * Creates a ShortStream that iterates using the given hasNext and next suppliers.
     * The stream terminates when hasNext returns {@code false}.
     * Once the {@code hasNext} supplier returns {@code false}, the stream remains exhausted and
     * that supplier is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate with external counter
     * AtomicInteger counter = new AtomicInteger(0);
     * ShortStream.iterate(() -> counter.get() < 5,
     *                     () -> (short) counter.getAndIncrement())
     *     .toArray();   // returns [0, 1, 2, 3, 4]
     *
     * // Iterate from external source
     * Iterator<Short> source = Arrays.asList((short)10, (short)20, (short)30).iterator();
     * ShortStream.iterate(source::hasNext,
     *                     () -> source.next())
     *     .sum();   // returns 60
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a ShortSupplier that provides the next short in the iteration
     * @return a ShortStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or next is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static ShortStream iterate(final BooleanSupplier hasNext, final ShortSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorShortStream(new ShortIteratorEx() {
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal && hasMore) {
                    hasNextVal = hasNext.getAsBoolean();

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public short nextShort() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsShort();
            }
        });
    }

    /**
     * Creates a ShortStream that starts with an initial value and iterates by applying a function to generate subsequent values.
     * The stream continues as long as the hasNext supplier returns {@code true}.
     * After the {@code hasNext} supplier first returns {@code false}, the stream remains exhausted
     * and that supplier is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate sequence with external condition
     * AtomicBoolean condition = new AtomicBoolean(true);
     * ShortStream.iterate((short) 1, condition::get, s -> (short) (s * 2))
     *     .limit(5)
     *     .toArray();   // returns [1, 2, 4, 8, 16]
     *
     * // Arithmetic sequence with limit
     * AtomicInteger count = new AtomicInteger(0);
     * ShortStream.iterate((short) 1, () -> count.getAndIncrement() < 10, s -> (short) (s + 1))
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a ShortStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, BooleanSupplier, java.util.function.UnaryOperator)
     */
    public static ShortStream iterate(final short init, final BooleanSupplier hasNext, final ShortUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short cur = 0;
            private boolean isFirst = true;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal && hasMore) {
                    hasNextVal = hasNext.getAsBoolean();

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public short nextShort() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsShort(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates a ShortStream that starts with an initial value and iterates by applying a function to generate subsequent values.
     * The stream continues as long as the generated values satisfy the given predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Powers of 2 less than 1000
     * ShortStream.iterate((short) 1, s -> s < 1000, s -> (short) (s * 2))
     *     .toArray();   // returns [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]
     *
     * // Countdown while positive
     * ShortStream.iterate((short) 10, s -> s > 0, s -> (short) (s - 1))
     *     .toArray();   // returns [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
     *
     * // Sequence with condition
     * ShortStream.iterate((short) 0, s -> s < 20, s -> (short) (s + 3))
     *     .toArray();   // returns [0, 3, 6, 9, 12, 15, 18]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext predicate to determine if the stream should continue; tested on init for the first element and on subsequent generated values
     * @param f a function to apply to the previous element to generate the next element
     * @return a ShortStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, java.util.function.Predicate, java.util.function.UnaryOperator)
     */
    public static ShortStream iterate(final short init, final ShortPredicate hasNext, final ShortUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsShort(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public short nextShort() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return cur;
            }
        });
    }

    /**
     * Creates an infinite ShortStream that starts with an initial value and iterates by applying a function to generate subsequent values.
     * This is an infinite stream that will continue generating values indefinitely.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Infinite sequence of powers of 2
     * ShortStream.iterate((short) 1, s -> (short) (s * 2))
     *     .limit(10)
     *     .toArray();   // returns [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]
     *
     * // Infinite sequence incrementing by 5
     * ShortStream.iterate((short) 0, s -> (short) (s + 5))
     *     .limit(5)
     *     .toArray();   // returns [0, 5, 10, 15, 20]
     *
     * // Natural numbers starting from 1
     * ShortStream.iterate((short) 1, s -> (short) (s + 1))
     *     .limit(20)
     *     .sum();   // returns 210 (sum of first 20 natural numbers)
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an infinite ShortStream of elements generated by the iteration
     * @throws IllegalArgumentException if f is null
     * @see Stream#iterate(Object, java.util.function.UnaryOperator)
     */
    public static ShortStream iterate(final short init, final ShortUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsShort(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Generates an infinite sequential unordered stream where each element is generated by the provided ShortSupplier.
     * This is an infinite stream that will continue generating values indefinitely.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate constant values
     * ShortStream.generate(() -> (short) 42)
     *     .limit(5)
     *     .toArray();   // returns [42, 42, 42, 42, 42]
     *
     * // Generate from counter
     * AtomicInteger counter = new AtomicInteger();
     * ShortStream.generate(() -> (short) counter.getAndIncrement())
     *     .limit(10)
     *     .toArray();   // returns [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Generate random values
     * Random random = new Random();
     * ShortStream.generate(() -> (short) random.nextInt(100))
     *     .limit(5)
     *     .toArray();   // returns 5 random shorts between 0-99
     * }</pre>
     *
     * @param s the ShortSupplier that provides the elements of the stream
     * @return an infinite ShortStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static ShortStream generate(final ShortSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                return s.getAsShort();
            }
        });
    }

    /**
     * Concatenates multiple short arrays into a single ShortStream.
     * The elements are concatenated in the order they appear in the input arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array1 = {1, 2, 3};
     * short[] array2 = {4, 5};
     * short[] array3 = {6, 7, 8, 9};
     *
     * ShortStream.concat(array1, array2, array3)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     *
     * // Concatenate and sum
     * ShortStream.concat(array1, array2)
     *     .sum();   // returns 15
     *
     * // Empty varargs returns empty stream
     * ShortStream.concat(new short[0][0]).count();   // returns 0
     * }</pre>
     *
     * @param a the arrays of shorts to concatenate
     * @return a ShortStream containing all the shorts from the input arrays
     * @see Stream#concat(Object[][])
     */
    public static ShortStream concat(final short[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple ShortIterators into a single ShortStream.
     * The elements are concatenated in the order they appear in the input iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short) 1, (short) 2);
     * ShortList list2 = ShortList.of((short) 3, (short) 4);
     *
     * ShortStream.concat(list1.iterator(), list2.iterator())
     *     .toArray();   // returns [1, 2, 3, 4]
     * }</pre>
     *
     * @param a the ShortIterators to concatenate
     * @return a ShortStream containing all the shorts from the input iterators in order
     * @see Stream#concat(Iterator[])
     */
    public static ShortStream concat(final ShortIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple ShortStreams into a single ShortStream.
     * The elements are concatenated in the order they appear in the input streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream stream1 = ShortStream.of((short) 1, (short) 2);
     * ShortStream stream2 = ShortStream.of((short) 3, (short) 4);
     * ShortStream stream3 = ShortStream.of((short) 5);
     *
     * ShortStream.concat(stream1, stream2, stream3)
     *     .toArray();   // returns [1, 2, 3, 4, 5]
     *
     * // Concatenate ranges
     * ShortStream.concat(
     *     ShortStream.range((short) 1, (short) 4),
     *     ShortStream.range((short) 10, (short) 13)
     * ).toArray();   // returns [1, 2, 3, 10, 11, 12]
     * }</pre>
     *
     * @param a the ShortStreams to concatenate
     * @return a ShortStream containing all the shorts from the input streams in order
     * @see Stream#concat(Stream[])
     */
    public static ShortStream concat(final ShortStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of short arrays into a single ShortStream.
     * The elements are concatenated in the order they appear in the input arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<short[]> arrays = Arrays.asList(
     *     new short[]{1, 2, 3},
     *     new short[]{4, 5},
     *     new short[]{6, 7, 8}
     * );
     *
     * ShortStream.concat(arrays)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * ShortStream.concat(arrays)
     *     .sum();   // returns 36
     * }</pre>
     *
     * @param c the list of short arrays to concatenate
     * @return a ShortStream containing all the shorts from the input arrays
     * @see Stream#concat(Object[][])
     */
    @Beta
    public static ShortStream concat(final List<short[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new ShortIteratorEx() {
            private final Iterator<short[]> iter = c.iterator();
            private short[] cur;
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
            public short nextShort() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of ShortStreams into a single ShortStream.
     * The elements are concatenated in the order they appear in the input streams.
     * The resulting stream will automatically close all input streams when it is closed.
     * The collection's membership and encounter order are snapshotted when this method is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ShortStream> streams = Arrays.asList(
     *     ShortStream.of((short) 1, (short) 2),
     *     ShortStream.range((short) 10, (short) 13),
     *     ShortStream.of((short) 20)
     * );
     *
     * ShortStream.concat(streams)
     *     .toArray();   // returns [1, 2, 10, 11, 12, 20]
     *
     * // Concatenate and process
     * ShortStream.concat(streams)
     *     .filter(s -> s > 5)
     *     .toArray();   // returns [10, 11, 12, 20]
     * }</pre>
     *
     * @param streams the collection of ShortStreams to concatenate; {@code null} elements are treated as empty streams
     * @return a ShortStream containing all the shorts from the input streams
     * @see Stream#concat(Collection)
     */
    public static ShortStream concat(final Collection<? extends ShortStream> streams) {
        if (N.isEmpty(streams)) {
            return empty();
        }

        final List<? extends ShortStream> sources = new ArrayList<>(streams);

        return new IteratorShortStream(new ShortIteratorEx() { //NOSONAR
            private final Iterator<? extends ShortStream> iterators = sources.iterator();
            private ShortStream cur;
            private ShortIterator iter;

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
            public short nextShort() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextShort();
            }
        }).onClose(newCloseHandler(sources));
    }

    /**
     * Concatenates a collection of ShortIterators into a single ShortStream.
     * The elements are concatenated in the order they appear in the input iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
     * ShortList list2 = ShortList.of((short) 4, (short) 5);
     * ShortList list3 = ShortList.of((short) 6, (short) 7);
     *
     * List<ShortIterator> iterators = Arrays.asList(
     *     list1.iterator(),
     *     list2.iterator(),
     *     list3.iterator()
     * );
     *
     * ShortStream.concatIterators(iterators)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7]
     * }</pre>
     *
     * @param shortIterators the collection of ShortIterators to concatenate
     * @return a ShortStream containing all the shorts from the input iterators
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static ShortStream concatIterators(final Collection<? extends ShortIterator> shortIterators) {
        if (N.isEmpty(shortIterators)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final Iterator<? extends ShortIterator> iter = shortIterators.iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
            }
        });
    }

    /**
     * Zips two short arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both arrays pairwise.
     * The stream ends when the shorter array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a = {1, 2, 3};
     * short[] b = {10, 20, 30};
     * ShortStream.zip(a, b, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first short array; {@code null} is treated as empty
     * @param b the second short array; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from both arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], BiFunction)
     */
    public static ShortStream zip(final short[] a, final short[] b, final ShortBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsShort(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three short arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three arrays together.
     * The stream ends when the shortest array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a = {1, 2, 3};
     * short[] b = {10, 20, 30};
     * short[] c = {100, 200, 300};
     * ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 222, 333]
     * }</pre>
     *
     * @param a the first short array; {@code null} is treated as empty
     * @param b the second short array; {@code null} is treated as empty
     * @param c the third short array; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from all three arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], TriFunction)
     */
    public static ShortStream zip(final short[] a, final short[] b, final short[] c, final ShortTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsShort(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two short iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both iterators pairwise.
     * The stream ends when either iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
     * ShortIterator b = ShortIterator.of((short) 10, (short) 20, (short) 30);
     * ShortStream.zip(a, b, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first ShortIterator; {@code null} is treated as empty
     * @param b the second ShortIterator; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from both iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortBinaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            private final ShortIterator iterA = a == null ? ShortIterator.empty() : a;
            private final ShortIterator iterB = b == null ? ShortIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public short nextShort() {
                return zipFunction.applyAsShort(iterA.nextShort(), iterB.nextShort());
            }
        });
    }

    /**
     * Zips three short iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three iterators together.
     * The stream ends when any iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
     * ShortIterator b = ShortIterator.of((short) 10, (short) 20, (short) 30);
     * ShortIterator c = ShortIterator.of((short) 100, (short) 200, (short) 300);
     * ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 222, 333]
     * }</pre>
     *
     * @param a the first ShortIterator; {@code null} is treated as empty
     * @param b the second ShortIterator; {@code null} is treated as empty
     * @param c the third ShortIterator; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from all three iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, TriFunction)
     */
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortTernaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            private final ShortIterator iterA = a == null ? ShortIterator.empty() : a;
            private final ShortIterator iterB = b == null ? ShortIterator.empty() : b;
            private final ShortIterator iterC = c == null ? ShortIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public short nextShort() {
                return zipFunction.applyAsShort(iterA.nextShort(), iterB.nextShort(), iterC.nextShort());
            }
        });
    }

    /**
     * Zips two short streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both streams pairwise.
     * The stream ends when either stream runs out of values.
     * The resulting stream will automatically close both input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
     * ShortStream b = ShortStream.of((short) 10, (short) 20, (short) 30);
     * ShortStream.zip(a, b, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first ShortStream; {@code null} is treated as empty
     * @param b the second ShortStream; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from both streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, BiFunction)
     */
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three short streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three streams together.
     * The stream ends when any stream runs out of values.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
     * ShortStream b = ShortStream.of((short) 10, (short) 20, (short) 30);
     * ShortStream c = ShortStream.of((short) 100, (short) 200, (short) 300);
     * ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 222, 333]
     * }</pre>
     *
     * @param a the first ShortStream; {@code null} is treated as empty
     * @param b the second ShortStream; {@code null} is treated as empty
     * @param c the third ShortStream; {@code null} is treated as empty
     * @param zipFunction the function to combine elements from all three streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, TriFunction)
     */
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortStream c, final ShortTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple short streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all streams together.
     * The stream ends when any stream runs out of values.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream s1 = ShortStream.of((short) 1, (short) 2);
     * ShortStream s2 = ShortStream.of((short) 10, (short) 20);
     * ShortStream s3 = ShortStream.of((short) 100, (short) 200);
     * ShortStream.zip(Arrays.asList(s1, s2, s3), shorts -> (short) (shorts[0] + shorts[1] + shorts[2]))
     *         .toShortList();   // returns [111, 222]
     * }</pre>
     *
     * @param streams the collection of ShortStreams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param zipFunction the function to combine elements from all the streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Collection, Function)
     */
    public static ShortStream zip(final Collection<? extends ShortStream> streams, final ShortNFunction<Short> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToShort(ToShortFunction.UNBOX);
    }

    /**
     * Zips two short arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both arrays pairwise.
     * The stream ends when both arrays run out of values.
     * If one array runs out of values before the other, the specified default values are used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a = {1, 2, 3};
     * short[] b = {10, 20};
     * ShortStream.zip(a, b, (short) 0, (short) 0, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 3]
     * }</pre>
     *
     * @param a the first short array; {@code null} is treated as empty
     * @param b the second short array; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param zipFunction the function to combine elements from both arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static ShortStream zip(final short[] a, final short[] b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private short ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsShort(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three short arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three arrays together.
     * The stream ends when all arrays run out of values.
     * If any array runs out of values before the others, the specified default values are used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a = {1, 2, 3};
     * short[] b = {10, 20};
     * short[] c = {100};
     * ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 22, 3]
     * }</pre>
     *
     * @param a the first short array; {@code null} is treated as empty
     * @param b the second short array; {@code null} is treated as empty
     * @param c the third short array; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param valueForNoneC the default value to use if the third array is shorter
     * @param zipFunction the function to combine elements from all three arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], Object, Object, Object, TriFunction)
     */
    public static ShortStream zip(final short[] a, final short[] b, final short[] c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private short ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsShort(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two short iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both iterators pairwise.
     * The stream ends when both iterators run out of values.
     * If one iterator runs out of values before the other, the specified default values are used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
     * ShortIterator b = ShortIterator.of((short) 10, (short) 20);
     * ShortStream.zip(a, b, (short) 0, (short) 0, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 3]
     * }</pre>
     *
     * @param a the first ShortIterator; {@code null} is treated as empty
     * @param b the second ShortIterator; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param zipFunction the function to combine elements from both iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            private final ShortIterator iterA = a == null ? ShortIterator.empty() : a;
            private final ShortIterator iterB = b == null ? ShortIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public short nextShort() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsShort(iterA.nextShort(), iterB.hasNext() ? iterB.nextShort() : valueForNoneB);
                } else {
                    return zipFunction.applyAsShort(valueForNoneA, iterB.nextShort());
                }
            }
        });
    }

    /**
     * Zips three short iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three iterators together.
     * The stream ends when all iterators run out of values.
     * If any iterator runs out of values before the others, the specified default values are used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
     * ShortIterator b = ShortIterator.of((short) 10, (short) 20);
     * ShortIterator c = ShortIterator.of((short) 100);
     * ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 22, 3]
     * }</pre>
     *
     * @param a the first ShortIterator; {@code null} is treated as empty
     * @param b the second ShortIterator; {@code null} is treated as empty
     * @param c the third ShortIterator; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param valueForNoneC the default value to use if the third iterator is shorter
     * @param zipFunction the function to combine elements from all three iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)
     */
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            private final ShortIterator iterA = a == null ? ShortIterator.empty() : a;
            private final ShortIterator iterB = b == null ? ShortIterator.empty() : b;
            private final ShortIterator iterC = c == null ? ShortIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public short nextShort() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsShort(iterA.nextShort(), iterB.hasNext() ? iterB.nextShort() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextShort() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsShort(valueForNoneA, iterB.nextShort(), iterC.hasNext() ? iterC.nextShort() : valueForNoneC);
                } else {
                    return zipFunction.applyAsShort(valueForNoneA, valueForNoneB, iterC.nextShort());
                }
            }
        });
    }

    /**
     * Zips two short streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both streams pairwise.
     * The stream ends when both streams run out of values.
     * If one stream runs out of values before the other, the specified default values are used.
     * The resulting stream will automatically close both input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
     * ShortStream b = ShortStream.of((short) 10, (short) 20);
     * ShortStream.zip(a, b, (short) 0, (short) 0, (x, y) -> (short) (x + y))
     *         .toShortList();   // returns [11, 22, 3]
     * }</pre>
     *
     * @param a the first ShortStream; {@code null} is treated as empty
     * @param b the second ShortStream; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param zipFunction the function to combine elements from both streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Object, Object, BiFunction)
     */
    public static ShortStream zip(final ShortStream a, final ShortStream b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three short streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three streams together.
     * The stream ends when all streams run out of values.
     * If any stream runs out of values before the others, the specified default values are used.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
     * ShortStream b = ShortStream.of((short) 10, (short) 20);
     * ShortStream c = ShortStream.of((short) 100);
     * ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z))
     *         .toShortList();   // returns [111, 22, 3]
     * }</pre>
     *
     * @param a the first ShortStream; {@code null} is treated as empty
     * @param b the second ShortStream; {@code null} is treated as empty
     * @param c the third ShortStream; {@code null} is treated as empty
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param valueForNoneC the default value to use if the third stream is shorter
     * @param zipFunction the function to combine elements from all three streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, Object, Object, Object, TriFunction)
     */
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple short streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all streams together.
     * The stream ends when all streams run out of values.
     * If any stream runs out of values before the others, the corresponding default value from valuesForNone is used.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream s1 = ShortStream.of((short) 1, (short) 2, (short) 3);
     * ShortStream s2 = ShortStream.of((short) 10, (short) 20);
     * ShortStream s3 = ShortStream.of((short) 100);
     * short[] defaults = {0, 0, 0};
     * ShortStream.zip(Arrays.asList(s1, s2, s3), defaults, shorts -> (short) (shorts[0] + shorts[1] + shorts[2]))
     *         .toShortList();   // returns [111, 22, 3]
     * }</pre>
     *
     * @param streams the collection of ShortStreams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param valuesForNone the array of default values to use for streams that run out of values. Must be non-null
     * @param zipFunction the function to combine elements from all the streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Collection, List, Function)
     */
    public static ShortStream zip(final Collection<? extends ShortStream> streams, final short[] valuesForNone, final ShortNFunction<Short> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToShort(ToShortFunction.UNBOX);
    }

    /**
     * Merges two short arrays into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge sorted arrays
     * short[] a = {1, 3, 5, 7};
     * short[] b = {2, 4, 6, 8};
     * ShortStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8]
     *
     * // Merge with custom logic
     * short[] odds = {1, 3, 5};
     * short[] evens = {2, 4, 6};
     * ShortStream.merge(odds, evens, (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [1, 3, 5, 2, 4, 6]
     * }</pre>
     *
     * @param a the first short array
     * @param b the second short array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the two input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static ShortStream merge(final short[] a, final short[] b, final ShortBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public short nextShort() {
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
     * Merges three short arrays into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted arrays
     * short[] a = {1, 4, 7};
     * short[] b = {2, 5, 8};
     * short[] c = {3, 6, 9};
     * ShortStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first short array
     * @param b the second short array
     * @param c the third short array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the three input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static ShortStream merge(final short[] a, final short[] b, final short[] c, final ShortBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), ShortStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two ShortIterators into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short) 1, (short) 3, (short) 5);
     * ShortList list2 = ShortList.of((short) 2, (short) 4, (short) 6);
     * ShortStream.merge(list1.iterator(), list2.iterator(),
     *                   (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param a the first ShortIterator
     * @param b the second ShortIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the two input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static ShortStream merge(final ShortIterator a, final ShortIterator b, final ShortBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return new IteratorShortStream(new ShortIteratorEx() {
            private final ShortIterator iterA = a == null ? ShortIterator.empty() : a;
            private final ShortIterator iterB = b == null ? ShortIterator.empty() : b;
            private short nextA = 0;
            private short nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public short nextShort() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextShort())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextShort()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextShort()), (nextB = iterB.nextShort())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextShort();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextShort();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three ShortIterators into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list1 = ShortList.of((short) 1, (short) 4);
     * ShortList list2 = ShortList.of((short) 2, (short) 5);
     * ShortList list3 = ShortList.of((short) 3, (short) 6);
     * ShortStream.merge(list1.iterator(), list2.iterator(), list3.iterator(),
     *                   (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param a the first ShortIterator
     * @param b the second ShortIterator
     * @param c the third ShortIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the three input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static ShortStream merge(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two ShortStreams into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two streams.
     * The resulting stream will automatically close both input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream stream1 = ShortStream.of((short) 1, (short) 3, (short) 5);
     * ShortStream stream2 = ShortStream.of((short) 2, (short) 4, (short) 6);
     * ShortStream.merge(stream1, stream2,
     *                   (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6]
     *
     * // Merge sorted streams
     * ShortStream sorted1 = ShortStream.range((short) 0, (short) 10).filter(s -> s % 2 == 0);
     * ShortStream sorted2 = ShortStream.range((short) 0, (short) 10).filter(s -> s % 2 == 1);
     * ShortStream.merge(sorted1, sorted2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first ShortStream
     * @param b the second ShortStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the two input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static ShortStream merge(final ShortStream a, final ShortStream b, final ShortBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three ShortStreams into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the streams.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortStream stream1 = ShortStream.of((short) 1, (short) 4, (short) 7);
     * ShortStream stream2 = ShortStream.of((short) 2, (short) 5, (short) 8);
     * ShortStream stream3 = ShortStream.of((short) 3, (short) 6, (short) 9);
     * ShortStream.merge(stream1, stream2, stream3,
     *                   (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first ShortStream
     * @param b the second ShortStream
     * @param c the third ShortStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the three input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static ShortStream merge(final ShortStream a, final ShortStream b, final ShortStream c, final ShortBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of ShortStreams into a single ShortStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the streams.
     * The resulting stream will automatically close all input streams when it is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ShortStream> streams = Arrays.asList(
     *     ShortStream.of((short) 1, (short) 5, (short) 9),
     *     ShortStream.of((short) 2, (short) 6, (short) 10),
     *     ShortStream.of((short) 3, (short) 7, (short) 11),
     *     ShortStream.of((short) 4, (short) 8, (short) 12)
     * );
     * ShortStream.merge(streams,
     *                   (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
     * }</pre>
     *
     * @param streams the collection of ShortStreams to merge; a {@code null} collection and {@code null} elements are treated as empty
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ShortStream containing the merged elements from the input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Collection, BiFunction)
     */
    public static ShortStream merge(final Collection<? extends ShortStream> streams, final ShortBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            final ShortStream stream = streams.iterator().next();
            return stream == null ? empty() : stream;
        } else if (streams.size() == 2) {
            final Iterator<? extends ShortStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends ShortStream> iter = streams.iterator();
        ShortStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * An abstract extension class for ShortStream that allows for custom implementations
     * and extensions of the base ShortStream functionality.
     *
     * <p>This class serves as a base for creating specialized short stream implementations
     * while maintaining the core stream behavior and characteristics such as sorting state
     * and close handlers.
     */
    public abstract static class ShortStreamEx extends ShortStream {
        private ShortStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
        }
    }
}
